package com.centreformation.service.impl;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Formateur;
import com.centreformation.entity.Role;
import com.centreformation.entity.Specialite;
import com.centreformation.entity.Utilisateur;
import com.centreformation.repository.CoursRepository;
import com.centreformation.repository.FormateurRepository;
import com.centreformation.repository.RoleRepository;
import com.centreformation.repository.SpecialiteRepository;
import com.centreformation.repository.UtilisateurRepository;
import com.centreformation.service.FormateurService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FormateurServiceImpl implements FormateurService {

    private final FormateurRepository formateurRepository;
    private final CoursRepository coursRepository;
    private final SpecialiteRepository specialiteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<Formateur> findAll() {
        return formateurRepository.findAll();
    }

    @Override
    public Page<Formateur> findAll(Pageable pageable) {
        return formateurRepository.findAll(pageable);
    }

    @Override
    public Formateur findById(Long id) {
        return formateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formateur non trouvé avec l'ID: " + id));
    }

    @Override
    public Formateur findByEmail(String email) {
        return formateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Formateur non trouvé avec l'email: " + email));
    }

    @Override
    public Page<Formateur> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll(pageable);
        }
        return formateurRepository.searchFormateurs(keyword, pageable);
    }

    @Override
    public Page<Formateur> findBySpecialite(Long specialiteId, Pageable pageable) {
        return formateurRepository.findBySpecialiteId(specialiteId, pageable);
    }

    @Override
    public List<Cours> findCoursByFormateur(Long formateurId) {
        Formateur formateur = findById(formateurId);
        return coursRepository.findByFormateur(formateur);
    }

    @Override
    public Formateur save(Formateur formateur) {
        // Vérification unicité email
        if (formateur.getId() == null && formateurRepository.existsByEmail(formateur.getEmail())) {
            throw new RuntimeException("Un formateur avec cet email existe déjà");
        }
        
        // Si pas d'utilisateur lié, créer automatiquement un compte
        if (formateur.getUtilisateur() == null) {
            Utilisateur utilisateur = createUtilisateurForFormateur(formateur);
            formateur.setUtilisateur(utilisateur);
        }
        
        return formateurRepository.save(formateur);
    }
    
    /**
     * Crée automatiquement un compte utilisateur pour un formateur
     */
    private Utilisateur createUtilisateurForFormateur(Formateur formateur) {
        // Vérifier si un utilisateur avec cet email existe déjà
        if (utilisateurRepository.existsByEmail(formateur.getEmail())) {
            // Lier au compte existant
            Utilisateur existing = utilisateurRepository.findByEmail(formateur.getEmail()).orElse(null);
            if (existing != null) {
                log.info("Liaison du formateur {} à l'utilisateur existant {}", formateur.getEmail(), existing.getUsername());
                return existing;
            }
        }
        
        // Générer un username unique basé sur prénom.nom
        String baseUsername = (formateur.getPrenom().toLowerCase() + "." + formateur.getNom().toLowerCase())
                .replaceAll("[^a-z0-9.]", "");
        String username = baseUsername;
        int counter = 1;
        while (utilisateurRepository.existsByUsername(username)) {
            username = baseUsername + counter++;
        }
        
        // Créer le rôle FORMATEUR s'il n'existe pas
        Role roleFormateur = roleRepository.findByNom("FORMATEUR")
                .orElseGet(() -> roleRepository.save(new Role("FORMATEUR")));
        
        Set<Role> roles = new HashSet<>();
        roles.add(roleFormateur);
        
        // Mot de passe par défaut: prenom123 (le formateur devra le changer)
        String defaultPassword = formateur.getPrenom().toLowerCase() + "123";
        
        Utilisateur utilisateur = Utilisateur.builder()
                .username(username)
                .email(formateur.getEmail())
                .password(passwordEncoder.encode(defaultPassword))
                .actif(true)
                .roles(roles)
                .build();
        
        utilisateur = utilisateurRepository.save(utilisateur);
        log.info("Compte utilisateur créé automatiquement pour le formateur: {} (username: {}, mot de passe par défaut: {})", 
                formateur.getEmail(), username, defaultPassword);
        
        return utilisateur;
    }

    @Override
    public Formateur update(Long id, Formateur formateur) {
        Formateur existing = findById(id);
        
        // Vérification unicité email (sauf pour le formateur actuel)
        if (!existing.getEmail().equals(formateur.getEmail()) && 
            formateurRepository.existsByEmail(formateur.getEmail())) {
            throw new RuntimeException("Un formateur avec cet email existe déjà");
        }
        
        existing.setNom(formateur.getNom());
        existing.setPrenom(formateur.getPrenom());
        existing.setEmail(formateur.getEmail());
        existing.setTelephone(formateur.getTelephone());
        
        // Récupérer la spécialité si l'ID est fourni
        if (formateur.getSpecialite() != null && formateur.getSpecialite().getId() != null) {
            Specialite specialite = specialiteRepository.findById(formateur.getSpecialite().getId())
                .orElseThrow(() -> new RuntimeException("Spécialité non trouvée"));
            existing.setSpecialite(specialite);
        } else {
            existing.setSpecialite(null);
        }
        
        return formateurRepository.save(existing);
    }

    @Override
    public void deleteById(Long id) {
        if (!formateurRepository.existsById(id)) {
            throw new RuntimeException("Formateur non trouvé avec l'ID: " + id);
        }
        formateurRepository.deleteById(id);
    }

    @Override
    public long count() {
        return formateurRepository.count();
    }
}
