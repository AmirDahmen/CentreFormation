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
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration pour initialiser les données de base au démarrage
 * 
 * COMPORTEMENT:
 * - Profil "dev" : Crée les données de base SI elles n'existent pas (idempotent)
 * - Profil "reset" : Force la réinitialisation complète (ddl-auto=create-drop)
 * 
 * NOUVEAU MODÈLE DE DONNÉES:
 * SessionPédagogique (année, semestre)
 *   └── Cours (Java, Python, Web...)
 *         └── Groupes (Java-G1, Java-G2...)
 *               └── Étudiants (via Inscription validée)
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
    private final SalleRepository salleRepository;
    private final SpecialiteRepository specialiteRepository;
    private final GroupeRepository groupeRepository;
    private final SeanceCoursRepository seanceCoursRepository;
    private final SessionPedagogiqueRepository sessionPedagogiqueRepository;

    @Bean
    @Profile({"dev", "reset"})
    public CommandLineRunner initData() {
        return args -> {
            log.info("========================================");
            log.info("   INITIALISATION DES DONNÉES");
            log.info("========================================");
            
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

            // ========== SYNCHRONISATION DES LIAISONS UTILISATEURS/ENTITÉS ==========
            synchronizeUserLinks();

            // ========== DONNÉES DE TEST POUR LES RAPPORTS PDF ==========
            initTestData();

            log.info("========================================");
            log.info("   INITIALISATION TERMINÉE");
            log.info("========================================");
            log.info("Comptes disponibles:");
            log.info("  - Admin: admin / admin123");
            log.info("  - Les formateurs/étudiants créés ont le mot de passe: prenom123");
            log.info("========================================");
        };
    }

    /**
     * Synchronise les liaisons entre utilisateurs et entités (formateurs/étudiants)
     * Cette méthode permet de lier des entités existantes à leurs comptes utilisateurs par email
     */
    private void synchronizeUserLinks() {
        log.info("Synchronisation des liaisons utilisateurs/entités...");
        
        // Lier les formateurs aux utilisateurs par email
        formateurRepository.findAll().forEach(formateur -> {
            if (formateur.getUtilisateur() == null) {
                utilisateurRepository.findByEmail(formateur.getEmail()).ifPresent(utilisateur -> {
                    formateur.setUtilisateur(utilisateur);
                    formateurRepository.save(formateur);
                    log.info("Formateur {} lié à l'utilisateur {}", formateur.getEmail(), utilisateur.getUsername());
                });
            }
        });
        
        // Lier les étudiants aux utilisateurs par email
        etudiantRepository.findAll().forEach(etudiant -> {
            if (etudiant.getUtilisateur() == null) {
                utilisateurRepository.findByEmail(etudiant.getEmail()).ifPresent(utilisateur -> {
                    etudiant.setUtilisateur(utilisateur);
                    etudiantRepository.save(etudiant);
                    log.info("Etudiant {} lié à l'utilisateur {}", etudiant.getEmail(), utilisateur.getUsername());
                });
            }
        });
        
        log.info("Synchronisation terminée.");
    }

    private void initTestData() {
        // Vérifier si des données existent déjà
        if (etudiantRepository.count() > 0) {
            log.info("Données de test déjà présentes, skip...");
            // Initialiser les salles si elles n'existent pas
            initSalles();
            initSpecialites();
            return;
        }

        log.info("Création des données de test pour les rapports PDF...");
        
        // Initialiser les salles
        initSalles();
        
        // Initialiser les spécialités
        Specialite specInfo = initSpecialites();

        // Récupérer le rôle FORMATEUR
        Role roleFormateur = roleRepository.findByNom("FORMATEUR")
                .orElseGet(() -> roleRepository.save(new Role("FORMATEUR")));
        Role roleEtudiant = roleRepository.findByNom("ETUDIANT")
                .orElseGet(() -> roleRepository.save(new Role("ETUDIANT")));

        // Créer un formateur de test AVEC son compte utilisateur
        Utilisateur userFormateur1 = Utilisateur.builder()
                .username("jean.dupont")
                .email("jean.dupont@centreformation.com")
                .password(passwordEncoder.encode("jean123"))
                .actif(true)
                .roles(Set.of(roleFormateur))
                .build();
        userFormateur1 = utilisateurRepository.save(userFormateur1);
        
        Formateur formateur1 = Formateur.builder()
                .nom("Dupont")
                .prenom("Jean")
                .email("jean.dupont@centreformation.com")
                .telephone("0601020304")
                .specialite(specInfo)
                .utilisateur(userFormateur1)
                .build();
        formateur1 = formateurRepository.save(formateur1);
        log.info("Formateur Jean Dupont créé (username: jean.dupont, password: jean123)");

        Utilisateur userFormateur2 = Utilisateur.builder()
                .username("marie.martin")
                .email("marie.martin@centreformation.com")
                .password(passwordEncoder.encode("marie123"))
                .actif(true)
                .roles(Set.of(roleFormateur))
                .build();
        userFormateur2 = utilisateurRepository.save(userFormateur2);
        
        Formateur formateur2 = Formateur.builder()
                .nom("Martin")
                .prenom("Marie")
                .email("marie.martin@centreformation.com")
                .telephone("0605060708")
                .specialite(specInfo)
                .utilisateur(userFormateur2)
                .build();
        formateur2 = formateurRepository.save(formateur2);
        log.info("Formateur Marie Martin créé (username: marie.martin, password: marie123)");

        // ========== NOUVEAU MODÈLE: Session → Cours → Groupes ==========
        
        // Créer une session pédagogique
        SessionPedagogique session2025 = SessionPedagogique.builder()
                .nom("Session Automne 2025")
                .anneeScolaire("2025-2026")
                .semestre("S1")
                .dateDebut(LocalDate.of(2025, 9, 1))
                .dateFin(LocalDate.of(2026, 1, 31))
                .actif(true)
                .build();
        session2025 = sessionPedagogiqueRepository.save(session2025);
        log.info("Session pédagogique créée: {}", session2025.getDisplayName());

        // Créer des cours DANS la session
        Cours coursJava = Cours.builder()
                .code("JAVA101")
                .titre("Programmation Java")
                .description("Introduction à la programmation Java")
                .nombreHeures(40)
                .formateur(formateur1)
                .sessionPedagogique(session2025)
                .build();
        coursJava = coursRepository.save(coursJava);

        Cours coursPython = Cours.builder()
                .code("PY101")
                .titre("Python pour débutants")
                .description("Apprendre Python de zéro")
                .nombreHeures(30)
                .formateur(formateur1)
                .sessionPedagogique(session2025)
                .build();
        coursPython = coursRepository.save(coursPython);

        Cours coursWeb = Cours.builder()
                .code("WEB101")
                .titre("Développement Web")
                .description("HTML, CSS et JavaScript")
                .nombreHeures(50)
                .formateur(formateur2)
                .sessionPedagogique(session2025)
                .build();
        coursWeb = coursRepository.save(coursWeb);
        
        log.info("3 cours créés dans la session {}", session2025.getDisplayName());

        // Créer des groupes DANS les cours
        Groupe groupeJavaG1 = Groupe.builder()
                .code("JAVA-G1")
                .nom("Groupe 1")
                .cours(coursJava)
                .specialite(specInfo)
                .capacite(25)
                .build();
        groupeJavaG1 = groupeRepository.save(groupeJavaG1);

        Groupe groupeJavaG2 = Groupe.builder()
                .code("JAVA-G2")
                .nom("Groupe 2")
                .cours(coursJava)
                .specialite(specInfo)
                .capacite(25)
                .build();
        groupeJavaG2 = groupeRepository.save(groupeJavaG2);

        Groupe groupePythonG1 = Groupe.builder()
                .code("PY-G1")
                .nom("Groupe 1")
                .cours(coursPython)
                .specialite(specInfo)
                .capacite(25)
                .build();
        groupePythonG1 = groupeRepository.save(groupePythonG1);

        Groupe groupeWebG1 = Groupe.builder()
                .code("WEB-G1")
                .nom("Groupe 1")
                .cours(coursWeb)
                .specialite(specInfo)
                .capacite(30)
                .build();
        groupeWebG1 = groupeRepository.save(groupeWebG1);
        
        log.info("4 groupes créés (2 pour Java, 1 pour Python, 1 pour Web)");

        // Créer des étudiants AVEC leurs comptes utilisateurs
        Utilisateur userEtud1 = createUserForEtudiant("alice.bernard", "alice.bernard@email.com", "alice123", roleEtudiant);
        Etudiant etudiant1 = Etudiant.builder()
                .matricule("ETU2024001")
                .nom("Bernard")
                .prenom("Alice")
                .email("alice.bernard@email.com")
                .dateInscription(LocalDate.now().minusMonths(6))
                .specialite(specInfo)
                .utilisateur(userEtud1)
                .build();
        etudiant1 = etudiantRepository.save(etudiant1);

        Utilisateur userEtud2 = createUserForEtudiant("thomas.petit", "thomas.petit@email.com", "thomas123", roleEtudiant);
        Etudiant etudiant2 = Etudiant.builder()
                .matricule("ETU2024002")
                .nom("Petit")
                .prenom("Thomas")
                .email("thomas.petit@email.com")
                .dateInscription(LocalDate.now().minusMonths(5))
                .specialite(specInfo)
                .utilisateur(userEtud2)
                .build();
        etudiant2 = etudiantRepository.save(etudiant2);

        Utilisateur userEtud3 = createUserForEtudiant("sophie.durand", "sophie.durand@email.com", "sophie123", roleEtudiant);
        Etudiant etudiant3 = Etudiant.builder()
                .matricule("ETU2024003")
                .nom("Durand")
                .prenom("Sophie")
                .email("sophie.durand@email.com")
                .dateInscription(LocalDate.now().minusMonths(4))
                .specialite(specInfo)
                .utilisateur(userEtud3)
                .build();
        etudiant3 = etudiantRepository.save(etudiant3);

        Utilisateur userEtud4 = createUserForEtudiant("lucas.moreau", "lucas.moreau@email.com", "lucas123", roleEtudiant);
        Etudiant etudiant4 = Etudiant.builder()
                .matricule("ETU2024004")
                .nom("Moreau")
                .prenom("Lucas")
                .email("lucas.moreau@email.com")
                .dateInscription(LocalDate.now().minusMonths(3))
                .specialite(specInfo)
                .utilisateur(userEtud4)
                .build();
        etudiant4 = etudiantRepository.save(etudiant4);

        Utilisateur userEtud5 = createUserForEtudiant("emma.leroy", "emma.leroy@email.com", "emma123", roleEtudiant);
        Etudiant etudiant5 = Etudiant.builder()
                .matricule("ETU2024005")
                .nom("Leroy")
                .prenom("Emma")
                .email("emma.leroy@email.com")
                .dateInscription(LocalDate.now().minusMonths(2))
                .specialite(specInfo)
                .utilisateur(userEtud5)
                .build();
        etudiant5 = etudiantRepository.save(etudiant5);

        log.info("5 étudiants créés avec leurs comptes utilisateurs (mot de passe: prenom123)");

        // Créer des inscriptions (mélange de VALIDEE et EN_ATTENTE pour les tests)
        // NOUVEAU: On inscrit à un cours, et si VALIDEE, on assigne à un groupe DU cours
        createInscription(etudiant1, coursJava, Inscription.StatutInscription.VALIDEE, groupeJavaG1);
        createInscription(etudiant1, coursPython, Inscription.StatutInscription.VALIDEE, groupePythonG1);
        createInscription(etudiant2, coursJava, Inscription.StatutInscription.VALIDEE, groupeJavaG1);
        createInscription(etudiant2, coursWeb, Inscription.StatutInscription.EN_ATTENTE, null);
        createInscription(etudiant3, coursJava, Inscription.StatutInscription.VALIDEE, groupeJavaG2);
        createInscription(etudiant3, coursPython, Inscription.StatutInscription.VALIDEE, groupePythonG1);
        createInscription(etudiant3, coursWeb, Inscription.StatutInscription.EN_ATTENTE, null);
        createInscription(etudiant4, coursWeb, Inscription.StatutInscription.EN_ATTENTE, null);
        createInscription(etudiant5, coursPython, Inscription.StatutInscription.VALIDEE, groupePythonG1);
        createInscription(etudiant5, coursWeb, Inscription.StatutInscription.VALIDEE, groupeWebG1);

        // Créer des notes (une seule note par étudiant/cours à cause de la contrainte d'unicité)
        createNote(etudiant1, coursJava, 15.75, "Moyenne des contrôles");
        createNote(etudiant1, coursPython, 14.0, "TP Pratique");
        createNote(etudiant2, coursJava, 12.75, "Moyenne des contrôles");
        createNote(etudiant2, coursWeb, 17.5, "Projet");
        createNote(etudiant3, coursJava, 17.5, "Moyenne des contrôles");
        createNote(etudiant3, coursPython, 16.5, "TP Pratique");
        createNote(etudiant3, coursWeb, 15.0, "Projet");
        createNote(etudiant4, coursWeb, 8.5, "Projet");
        createNote(etudiant5, coursPython, 11.0, "TP Pratique");
        createNote(etudiant5, coursWeb, 13.5, "Projet");

        // Créer des séances de cours (emploi du temps)
        Salle salleA101 = salleRepository.findByCode("A101").orElse(null);
        Salle salleB101 = salleRepository.findByCode("B101").orElse(null);
        
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1); // Lundi de cette semaine
        
        // Séances pour Jean Dupont (formateur1) - cours Java et Python
        createSeance(coursJava, formateur1, groupeJavaG1, salleB101, monday, LocalTime.of(8, 30), LocalTime.of(10, 30), "Introduction Java");
        createSeance(coursJava, formateur1, groupeJavaG2, salleB101, monday.plusDays(2), LocalTime.of(8, 30), LocalTime.of(10, 30), "Les classes Java");
        createSeance(coursPython, formateur1, groupePythonG1, salleB101, monday.plusDays(1), LocalTime.of(14, 0), LocalTime.of(16, 0), "Variables Python");
        createSeance(coursPython, formateur1, groupePythonG1, salleB101, monday.plusDays(3), LocalTime.of(10, 30), LocalTime.of(12, 30), "Fonctions Python");
        
        // Séances pour Marie Martin (formateur2) - cours Web
        createSeance(coursWeb, formateur2, groupeWebG1, salleA101, monday.plusDays(1), LocalTime.of(8, 30), LocalTime.of(10, 30), "HTML5 Bases");
        createSeance(coursWeb, formateur2, groupeWebG1, salleA101, monday.plusDays(4), LocalTime.of(14, 0), LocalTime.of(17, 0), "CSS3 et Flexbox");
        
        // Séances semaine prochaine
        createSeance(coursJava, formateur1, groupeJavaG1, salleB101, monday.plusDays(7), LocalTime.of(8, 30), LocalTime.of(10, 30), "Héritage Java");
        createSeance(coursWeb, formateur2, groupeWebG1, salleA101, monday.plusDays(8), LocalTime.of(8, 30), LocalTime.of(10, 30), "JavaScript Intro");

        log.info("Données de test créées: {} étudiants, {} formateurs, {} cours, {} inscriptions, {} notes, {} séances",
                etudiantRepository.count(),
                formateurRepository.count(),
                coursRepository.count(),
                inscriptionRepository.count(),
                noteRepository.count(),
                seanceCoursRepository.count());
    }
    
    private void createSeance(Cours cours, Formateur formateur, Groupe groupe, Salle salle, 
                              LocalDate date, LocalTime heureDebut, LocalTime heureFin, String contenu) {
        if (groupe == null || cours == null) return;
        
        SeanceCours seance = SeanceCours.builder()
                .cours(cours)
                .formateur(formateur)
                .groupe(groupe)
                .salle(salle)
                .date(date)
                .heureDebut(heureDebut)
                .heureFin(heureFin)
                .contenu(contenu)
                .build();
        seanceCoursRepository.save(seance);
    }

    private void createInscription(Etudiant etudiant, Cours cours, Inscription.StatutInscription statut, Groupe groupe) {
        Inscription inscription = Inscription.builder()
                .etudiant(etudiant)
                .cours(cours)
                .groupe(groupe)
                .dateInscription(LocalDateTime.now().minusDays((long)(Math.random() * 60)))
                .dateValidation(statut == Inscription.StatutInscription.VALIDEE ? LocalDateTime.now().minusDays(10) : null)
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
    
    /**
     * Initialise les spécialités de base
     * @return la spécialité Informatique (utilisée pour les données de test)
     */
    private Specialite initSpecialites() {
        if (specialiteRepository.count() > 0) {
            log.info("Spécialités déjà présentes, skip...");
            return specialiteRepository.findAll().stream().findFirst().orElse(null);
        }
        
        log.info("Création des spécialités...");
        
        // Créer les spécialités
        Specialite specInfo = Specialite.builder()
                .nom("Informatique")
                .description("Développement logiciel et systèmes d'information")
                .build();
        specInfo = specialiteRepository.save(specInfo);
        
        Specialite specGestion = Specialite.builder()
                .nom("Gestion")
                .description("Management et administration des entreprises")
                .build();
        specialiteRepository.save(specGestion);
        
        Specialite specReseau = Specialite.builder()
                .nom("Réseaux et Télécommunications")
                .description("Administration réseau et sécurité")
                .build();
        specialiteRepository.save(specReseau);
        
        // Note: Les groupes sont maintenant créés APRÈS les cours car ils dépendent des cours
        // Voir initTestData() pour la création des groupes
        
        log.info("{} spécialités créées", specialiteRepository.count());
        
        return specInfo;
    }
    
    private void initSalles() {
        if (salleRepository.count() > 0) {
            log.info("Salles déjà présentes, skip...");
            return;
        }
        
        log.info("Création des salles de test...");
        
        // Salles de cours classiques
        salleRepository.save(Salle.builder()
                .code("A101")
                .nom("Salle de cours A101")
                .capacite(30)
                .type("COURS")
                .batiment("Bâtiment A")
                .etage(1)
                .actif(true)
                .hasProjecteur(true)
                .hasTableauBlanc(true)
                .build());
        
        salleRepository.save(Salle.builder()
                .code("A102")
                .nom("Salle de cours A102")
                .capacite(25)
                .type("COURS")
                .batiment("Bâtiment A")
                .etage(1)
                .actif(true)
                .hasProjecteur(true)
                .hasTableauBlanc(true)
                .build());
        
        salleRepository.save(Salle.builder()
                .code("A201")
                .nom("Grande salle A201")
                .capacite(50)
                .type("AMPHI")
                .batiment("Bâtiment A")
                .etage(2)
                .actif(true)
                .hasProjecteur(true)
                .hasTableauBlanc(false)
                .build());
        
        // Salles informatiques
        salleRepository.save(Salle.builder()
                .code("B101")
                .nom("Labo Informatique 1")
                .capacite(20)
                .type("INFORMATIQUE")
                .batiment("Bâtiment B")
                .etage(1)
                .actif(true)
                .hasProjecteur(true)
                .hasOrdinateurs(true)
                .hasTableauBlanc(true)
                .build());
        
        salleRepository.save(Salle.builder()
                .code("B102")
                .nom("Labo Informatique 2")
                .capacite(18)
                .type("INFORMATIQUE")
                .batiment("Bâtiment B")
                .etage(1)
                .actif(true)
                .hasProjecteur(true)
                .hasOrdinateurs(true)
                .hasTableauBlanc(true)
                .build());
        
        salleRepository.save(Salle.builder()
                .code("B201")
                .nom("Labo Réseau")
                .capacite(15)
                .type("INFORMATIQUE")
                .batiment("Bâtiment B")
                .etage(2)
                .actif(true)
                .hasProjecteur(true)
                .hasOrdinateurs(true)
                .hasTableauBlanc(true)
                .build());
        
        // Salles de TP
        salleRepository.save(Salle.builder()
                .code("C101")
                .nom("Salle TP Électronique")
                .capacite(24)
                .type("TP")
                .batiment("Bâtiment C")
                .etage(1)
                .actif(true)
                .hasProjecteur(false)
                .hasTableauBlanc(true)
                .build());
        
        salleRepository.save(Salle.builder()
                .code("C102")
                .nom("Salle TP Mécanique")
                .capacite(20)
                .type("TP")
                .batiment("Bâtiment C")
                .etage(1)
                .actif(true)
                .hasProjecteur(false)
                .hasTableauBlanc(true)
                .build());
        
        log.info("{} salles créées avec succès", salleRepository.count());
    }
    
    /**
     * Crée un compte utilisateur pour un étudiant
     */
    private Utilisateur createUserForEtudiant(String username, String email, String password, Role role) {
        Utilisateur user = Utilisateur.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .actif(true)
                .roles(Set.of(role))
                .build();
        return utilisateurRepository.save(user);
    }
}
