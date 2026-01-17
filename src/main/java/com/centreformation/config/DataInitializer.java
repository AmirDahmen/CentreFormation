package com.centreformation.config;

import com.centreformation.entity.*;
import com.centreformation.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Configuration pour initialiser les données de base au démarrage
 * Active uniquement en profil "dev"
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final EtudiantRepository etudiantRepository;
    private final FormateurRepository formateurRepository;
    private final CoursRepository coursRepository;
    private final InscriptionRepository inscriptionRepository;
    private final NoteRepository noteRepository;

    @Bean
    @Profile("dev")
    public CommandLineRunner initData() {
        return args -> {
            log.info("Initialisation des données de base...");
            
            // Créer les rôles s'ils n'existent pas
            Role roleAdmin = createRoleIfNotExists("ADMIN");
            Role roleFormateur = createRoleIfNotExists("FORMATEUR");
            Role roleEtudiant = createRoleIfNotExists("ETUDIANT");

            // Créer un utilisateur admin par défaut s'il n'existe pas
            if (!utilisateurRepository.existsByUsername("admin")) {
                Utilisateur admin = Utilisateur.builder()
                        .username("admin")
                        .email("admin@centreformation.com")
                        .password(passwordEncoder.encode("admin123"))
                        .actif(true)
                        .roles(Set.of(roleAdmin))
                        .build();
                utilisateurRepository.save(admin);
                log.info("Utilisateur admin créé avec succès (username: admin, password: admin123)");
            }

            // Créer un utilisateur formateur par défaut s'il n'existe pas
            if (!utilisateurRepository.existsByUsername("formateur")) {
                Utilisateur formateur = Utilisateur.builder()
                        .username("formateur")
                        .email("formateur@centreformation.com")
                        .password(passwordEncoder.encode("form123"))
                        .actif(true)
                        .roles(Set.of(roleFormateur))
                        .build();
                utilisateurRepository.save(formateur);
                log.info("Utilisateur formateur créé avec succès (username: formateur, password: form123)");
            }

            // Créer un utilisateur étudiant par défaut s'il n'existe pas
            if (!utilisateurRepository.existsByUsername("etudiant")) {
                Utilisateur etudiant = Utilisateur.builder()
                        .username("etudiant")
                        .email("etudiant@centreformation.com")
                        .password(passwordEncoder.encode("etud123"))
                        .actif(true)
                        .roles(Set.of(roleEtudiant))
                        .build();
                utilisateurRepository.save(etudiant);
                log.info("Utilisateur étudiant créé avec succès (username: etudiant, password: etud123)");
            }

            // ========== DONNÉES DE TEST POUR LES RAPPORTS PDF ==========
            initTestData();

            log.info("Initialisation des données terminée.");
        };
    }

    private void initTestData() {
        // Vérifier si des données existent déjà
        if (etudiantRepository.count() > 0) {
            log.info("Données de test déjà présentes, skip...");
            return;
        }

        log.info("Création des données de test pour les rapports PDF...");

        // Créer un formateur de test
        Formateur formateur1 = Formateur.builder()
                .nom("Dupont")
                .prenom("Jean")
                .email("jean.dupont@centreformation.com")
                .telephone("0601020304")
                .build();
        formateur1 = formateurRepository.save(formateur1);

        Formateur formateur2 = Formateur.builder()
                .nom("Martin")
                .prenom("Marie")
                .email("marie.martin@centreformation.com")
                .telephone("0605060708")
                .build();
        formateur2 = formateurRepository.save(formateur2);

        // Créer des cours
        Cours coursJava = Cours.builder()
                .code("JAVA101")
                .titre("Programmation Java")
                .description("Introduction à la programmation Java")
                .nombreHeures(40)
                .formateur(formateur1)
                .build();
        coursJava = coursRepository.save(coursJava);

        Cours coursPython = Cours.builder()
                .code("PY101")
                .titre("Python pour débutants")
                .description("Apprendre Python de zéro")
                .nombreHeures(30)
                .formateur(formateur1)
                .build();
        coursPython = coursRepository.save(coursPython);

        Cours coursWeb = Cours.builder()
                .code("WEB101")
                .titre("Développement Web")
                .description("HTML, CSS et JavaScript")
                .nombreHeures(50)
                .formateur(formateur2)
                .build();
        coursWeb = coursRepository.save(coursWeb);

        // Créer des étudiants
        Etudiant etudiant1 = Etudiant.builder()
                .matricule("ETU2024001")
                .nom("Bernard")
                .prenom("Alice")
                .email("alice.bernard@email.com")
                .dateInscription(LocalDate.now().minusMonths(6))
                .build();
        etudiant1 = etudiantRepository.save(etudiant1);

        Etudiant etudiant2 = Etudiant.builder()
                .matricule("ETU2024002")
                .nom("Petit")
                .prenom("Thomas")
                .email("thomas.petit@email.com")
                .dateInscription(LocalDate.now().minusMonths(5))
                .build();
        etudiant2 = etudiantRepository.save(etudiant2);

        Etudiant etudiant3 = Etudiant.builder()
                .matricule("ETU2024003")
                .nom("Durand")
                .prenom("Sophie")
                .email("sophie.durand@email.com")
                .dateInscription(LocalDate.now().minusMonths(4))
                .build();
        etudiant3 = etudiantRepository.save(etudiant3);

        Etudiant etudiant4 = Etudiant.builder()
                .matricule("ETU2024004")
                .nom("Moreau")
                .prenom("Lucas")
                .email("lucas.moreau@email.com")
                .dateInscription(LocalDate.now().minusMonths(3))
                .build();
        etudiant4 = etudiantRepository.save(etudiant4);

        Etudiant etudiant5 = Etudiant.builder()
                .matricule("ETU2024005")
                .nom("Leroy")
                .prenom("Emma")
                .email("emma.leroy@email.com")
                .dateInscription(LocalDate.now().minusMonths(2))
                .build();
        etudiant5 = etudiantRepository.save(etudiant5);

        // Créer des inscriptions
        createInscription(etudiant1, coursJava, Inscription.StatutInscription.ACTIVE);
        createInscription(etudiant1, coursPython, Inscription.StatutInscription.ACTIVE);
        createInscription(etudiant2, coursJava, Inscription.StatutInscription.ACTIVE);
        createInscription(etudiant2, coursWeb, Inscription.StatutInscription.ACTIVE);
        createInscription(etudiant3, coursJava, Inscription.StatutInscription.ACTIVE);
        createInscription(etudiant3, coursPython, Inscription.StatutInscription.ACTIVE);
        createInscription(etudiant3, coursWeb, Inscription.StatutInscription.ACTIVE);
        createInscription(etudiant4, coursWeb, Inscription.StatutInscription.ACTIVE);
        createInscription(etudiant5, coursPython, Inscription.StatutInscription.ACTIVE);
        createInscription(etudiant5, coursWeb, Inscription.StatutInscription.ACTIVE);

        // Créer des notes
        createNote(etudiant1, coursJava, 15.5, "Contrôle continu");
        createNote(etudiant1, coursJava, 16.0, "Examen final");
        createNote(etudiant1, coursPython, 14.0, "TP Pratique");
        createNote(etudiant2, coursJava, 12.5, "Contrôle continu");
        createNote(etudiant2, coursJava, 13.0, "Examen final");
        createNote(etudiant2, coursWeb, 17.5, "Projet");
        createNote(etudiant3, coursJava, 18.0, "Contrôle continu");
        createNote(etudiant3, coursJava, 17.0, "Examen final");
        createNote(etudiant3, coursPython, 16.5, "TP Pratique");
        createNote(etudiant3, coursWeb, 15.0, "Projet");
        createNote(etudiant4, coursWeb, 8.5, "Projet");
        createNote(etudiant5, coursPython, 11.0, "TP Pratique");
        createNote(etudiant5, coursWeb, 13.5, "Projet");

        log.info("Données de test créées: {} étudiants, {} formateurs, {} cours, {} inscriptions, {} notes",
                etudiantRepository.count(),
                formateurRepository.count(),
                coursRepository.count(),
                inscriptionRepository.count(),
                noteRepository.count());
    }

    private void createInscription(Etudiant etudiant, Cours cours, Inscription.StatutInscription statut) {
        Inscription inscription = Inscription.builder()
                .etudiant(etudiant)
                .cours(cours)
                .dateInscription(LocalDateTime.now().minusDays((long)(Math.random() * 60)))
                .statut(statut)
                .build();
        inscriptionRepository.save(inscription);
    }

    private void createNote(Etudiant etudiant, Cours cours, double valeur, String commentaire) {
        Note note = Note.builder()
                .etudiant(etudiant)
                .cours(cours)
                .valeur(valeur)
                .commentaire(commentaire)
                .dateSaisie(LocalDateTime.now().minusDays((long)(Math.random() * 30)))
                .build();
        noteRepository.save(note);
    }

    private Role createRoleIfNotExists(String roleName) {
        return roleRepository.findByNom(roleName)
                .orElseGet(() -> {
                    Role role = new Role(roleName);
                    roleRepository.save(role);
                    log.info("Rôle {} créé", roleName);
                    return role;
                });
    }
}
