package com.centreformation.service.impl;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Etudiant;
import com.centreformation.entity.Formateur;
import com.centreformation.entity.Inscription;
import com.centreformation.repository.CoursRepository;
import com.centreformation.repository.EtudiantRepository;
import com.centreformation.repository.InscriptionRepository;
import com.centreformation.service.EmailService;
import com.centreformation.service.InscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InscriptionServiceImpl implements InscriptionService {

    private final InscriptionRepository inscriptionRepository;
    private final EtudiantRepository etudiantRepository;
    private final CoursRepository coursRepository;
    private final EmailService emailService;

    @Override
    public List<Inscription> findAll() {
        return inscriptionRepository.findAll();
    }

    @Override
    public Page<Inscription> findAll(Pageable pageable) {
        return inscriptionRepository.findAll(pageable);
    }

    @Override
    public Inscription findById(Long id) {
        return inscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée avec l'ID: " + id));
    }

    @Override
    public List<Inscription> findByEtudiant(Long etudiantId) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        return inscriptionRepository.findByEtudiant(etudiant);
    }

    @Override
    public List<Inscription> findByCours(Long coursId) {
        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        return inscriptionRepository.findByCours(cours);
    }

    @Override
    public Page<Inscription> search(Long etudiantId, Long coursId,
                                    Inscription.StatutInscription statut, Pageable pageable) {
        return inscriptionRepository.searchInscriptions(etudiantId, coursId, statut, pageable);
    }

    @Override
    public Page<Inscription> findByStatut(Inscription.StatutInscription statut, Pageable pageable) {
        return inscriptionRepository.findByStatut(statut, pageable);
    }

    @Override
    public Inscription inscrire(Long etudiantId, Long coursId) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        
        // Vérifier si l'inscription existe déjà
        if (inscriptionRepository.existsByEtudiantAndCours(etudiant, cours)) {
            throw new RuntimeException("L'étudiant est déjà inscrit à ce cours");
        }
        
        Inscription inscription = Inscription.builder()
                .etudiant(etudiant)
                .cours(cours)
                .dateInscription(LocalDateTime.now())
                .statut(Inscription.StatutInscription.ACTIVE)
                .build();
        
        Inscription savedInscription = inscriptionRepository.save(inscription);
        
        // Envoyer les notifications par email
        sendInscriptionNotifications(etudiant, cours);
        
        return savedInscription;
    }
    
    /**
     * Envoie les notifications d'inscription à l'étudiant et au formateur
     */
    private void sendInscriptionNotifications(Etudiant etudiant, Cours cours) {
        try {
            String studentName = etudiant.getPrenom() + " " + etudiant.getNom();
            Formateur formateur = cours.getFormateur();
            String formateurName = formateur != null ? formateur.getPrenom() + " " + formateur.getNom() : "Non assigné";
            
            // Notifier l'étudiant
            if (etudiant.getEmail() != null && !etudiant.getEmail().isEmpty()) {
                emailService.notifyStudentInscription(
                    etudiant.getEmail(),
                    studentName,
                    cours.getTitre(),
                    formateurName
                );
            }
            
            // Notifier le formateur
            if (formateur != null && formateur.getEmail() != null && !formateur.getEmail().isEmpty()) {
                emailService.notifyFormateurNewInscription(
                    formateur.getEmail(),
                    formateurName,
                    studentName,
                    cours.getTitre()
                );
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications d'inscription: {}", e.getMessage());
        }
    }

    @Override
    public Inscription save(Inscription inscription) {
        // Vérifier si l'inscription existe déjà
        if (inscription.getId() == null && 
            inscriptionRepository.existsByEtudiantAndCours(inscription.getEtudiant(), inscription.getCours())) {
            throw new RuntimeException("L'étudiant est déjà inscrit à ce cours");
        }
        
        return inscriptionRepository.save(inscription);
    }

    @Override
    public Inscription update(Long id, Inscription inscription) {
        Inscription existing = findById(id);
        existing.setStatut(inscription.getStatut());
        existing.setDateInscription(inscription.getDateInscription());
        return inscriptionRepository.save(existing);
    }

    @Override
    public void annuler(Long inscriptionId) {
        Inscription inscription = findById(inscriptionId);
        inscription.setStatut(Inscription.StatutInscription.ANNULEE);
        inscriptionRepository.save(inscription);
        
        // Envoyer les notifications de désinscription
        sendDesinscriptionNotifications(inscription.getEtudiant(), inscription.getCours());
    }
    
    /**
     * Envoie les notifications de désinscription à l'étudiant et au formateur
     */
    private void sendDesinscriptionNotifications(Etudiant etudiant, Cours cours) {
        try {
            String studentName = etudiant.getPrenom() + " " + etudiant.getNom();
            Formateur formateur = cours.getFormateur();
            
            // Notifier l'étudiant
            if (etudiant.getEmail() != null && !etudiant.getEmail().isEmpty()) {
                emailService.notifyStudentDesinscription(
                    etudiant.getEmail(),
                    studentName,
                    cours.getTitre()
                );
            }
            
            // Notifier le formateur
            if (formateur != null && formateur.getEmail() != null && !formateur.getEmail().isEmpty()) {
                String formateurName = formateur.getPrenom() + " " + formateur.getNom();
                emailService.notifyFormateurDesinscription(
                    formateur.getEmail(),
                    formateurName,
                    studentName,
                    cours.getTitre()
                );
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications de désinscription: {}", e.getMessage());
        }
    }

    @Override
    public void deleteById(Long id) {
        if (!inscriptionRepository.existsById(id)) {
            throw new RuntimeException("Inscription non trouvée avec l'ID: " + id);
        }
        inscriptionRepository.deleteById(id);
    }

    @Override
    public long count() {
        return inscriptionRepository.count();
    }

    @Override
    public long countByStatut(Inscription.StatutInscription statut) {
        return inscriptionRepository.findByStatut(statut).size();
    }
}
