package com.centreformation.service.impl;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Etudiant;
import com.centreformation.entity.Formateur;
import com.centreformation.entity.Groupe;
import com.centreformation.entity.Inscription;
import com.centreformation.repository.CoursRepository;
import com.centreformation.repository.EtudiantRepository;
import com.centreformation.repository.GroupeRepository;
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
    private final GroupeRepository groupeRepository;
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
        // Utiliser la requête avec fetch pour éviter lazy loading
        return inscriptionRepository.findByCoursIdWithDetails(coursId);
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
        
        // Créer l'inscription en statut EN_ATTENTE (sera validée par l'admin)
        Inscription inscription = Inscription.builder()
                .etudiant(etudiant)
                .cours(cours)
                .dateInscription(LocalDateTime.now())
                .statut(Inscription.StatutInscription.EN_ATTENTE)
                .build();
        
        Inscription savedInscription = inscriptionRepository.save(inscription);
        
        // Envoyer notification à l'étudiant (demande en attente)
        sendInscriptionDemandeNotification(etudiant, cours);
        
        return savedInscription;
    }
    
    @Override
    public Inscription valider(Long inscriptionId, Long groupeId) {
        Inscription inscription = findById(inscriptionId);
        
        // Vérifier que l'inscription est en attente
        if (inscription.getStatut() != Inscription.StatutInscription.EN_ATTENTE) {
            throw new RuntimeException("Seules les inscriptions en attente peuvent être validées");
        }
        
        Groupe groupe = groupeRepository.findById(groupeId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé avec l'ID: " + groupeId));
        
        Cours cours = inscription.getCours();
        
        // Vérifier que le groupe appartient bien au cours de l'inscription
        if (groupe.getCours() == null || !groupe.getCours().getId().equals(cours.getId())) {
            throw new RuntimeException("Le groupe sélectionné n'appartient pas au cours " + cours.getTitre());
        }
        
        // Vérifier la capacité du groupe
        long inscriptionsActuelles = countInscriptionsValideesForGroupe(groupeId);
        if (groupe.getCapacite() != null && inscriptionsActuelles >= groupe.getCapacite()) {
            throw new RuntimeException("Le groupe a atteint sa capacité maximale (" + groupe.getCapacite() + " places)");
        }
        
        // Valider l'inscription
        inscription.setStatut(Inscription.StatutInscription.VALIDEE);
        inscription.setGroupe(groupe);
        inscription.setDateValidation(LocalDateTime.now());
        
        Inscription savedInscription = inscriptionRepository.save(inscription);
        
        // Envoyer les notifications de validation
        sendInscriptionValideeNotifications(inscription.getEtudiant(), inscription.getCours(), groupe);
        
        return savedInscription;
    }
    
    @Override
    public Inscription refuser(Long inscriptionId) {
        Inscription inscription = findById(inscriptionId);
        
        // Vérifier que l'inscription est en attente
        if (inscription.getStatut() != Inscription.StatutInscription.EN_ATTENTE) {
            throw new RuntimeException("Seules les inscriptions en attente peuvent être refusées");
        }
        
        inscription.setStatut(Inscription.StatutInscription.REFUSEE);
        inscription.setDateValidation(LocalDateTime.now());
        
        Inscription savedInscription = inscriptionRepository.save(inscription);
        
        // Envoyer notification de refus
        sendInscriptionRefuseeNotification(inscription.getEtudiant(), inscription.getCours());
        
        return savedInscription;
    }
    
    /**
     * Notification quand l'étudiant fait une demande d'inscription
     */
    private void sendInscriptionDemandeNotification(Etudiant etudiant, Cours cours) {
        try {
            String studentName = etudiant.getPrenom() + " " + etudiant.getNom();
            
            // Notifier l'étudiant que sa demande est en attente
            if (etudiant.getEmail() != null && !etudiant.getEmail().isEmpty()) {
                emailService.sendEmail(
                    etudiant.getEmail(),
                    "Demande d'inscription en attente - " + cours.getTitre(),
                    "Bonjour " + studentName + ",\n\n" +
                    "Votre demande d'inscription au cours \"" + cours.getTitre() + "\" a bien été enregistrée.\n" +
                    "Elle est actuellement en attente de validation par l'administration.\n" +
                    "Vous recevrez une notification dès que votre inscription sera traitée.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe du Centre de Formation"
                );
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification de demande d'inscription: {}", e.getMessage());
        }
    }
    
    /**
     * Notifications quand l'admin valide l'inscription
     */
    private void sendInscriptionValideeNotifications(Etudiant etudiant, Cours cours, Groupe groupe) {
        try {
            String studentName = etudiant.getPrenom() + " " + etudiant.getNom();
            Formateur formateur = cours.getFormateur();
            String formateurName = formateur != null ? formateur.getPrenom() + " " + formateur.getNom() : "Non assigné";
            
            // Notifier l'étudiant
            if (etudiant.getEmail() != null && !etudiant.getEmail().isEmpty()) {
                emailService.sendEmail(
                    etudiant.getEmail(),
                    "Inscription validée - " + cours.getTitre(),
                    "Bonjour " + studentName + ",\n\n" +
                    "Bonne nouvelle ! Votre inscription au cours \"" + cours.getTitre() + "\" a été validée.\n" +
                    "Vous êtes assigné(e) au groupe : " + groupe.getNom() + "\n" +
                    "Formateur : " + formateurName + "\n\n" +
                    "Vous pouvez dès maintenant consulter votre emploi du temps.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe du Centre de Formation"
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
            log.error("Erreur lors de l'envoi des notifications de validation: {}", e.getMessage());
        }
    }
    
    /**
     * Notification quand l'admin refuse l'inscription
     */
    private void sendInscriptionRefuseeNotification(Etudiant etudiant, Cours cours) {
        try {
            String studentName = etudiant.getPrenom() + " " + etudiant.getNom();
            
            if (etudiant.getEmail() != null && !etudiant.getEmail().isEmpty()) {
                emailService.sendEmail(
                    etudiant.getEmail(),
                    "Inscription refusée - " + cours.getTitre(),
                    "Bonjour " + studentName + ",\n\n" +
                    "Nous avons le regret de vous informer que votre demande d'inscription au cours \"" + cours.getTitre() + "\" n'a pas pu être acceptée.\n" +
                    "Pour plus d'informations, veuillez contacter l'administration.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe du Centre de Formation"
                );
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification de refus: {}", e.getMessage());
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
        if (inscription.getGroupe() != null) {
            existing.setGroupe(inscription.getGroupe());
        }
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
    
    @Override
    public long countInscriptionsValideesForGroupe(Long groupeId) {
        return inscriptionRepository.countByGroupeIdAndStatutValidee(groupeId);
    }
}
