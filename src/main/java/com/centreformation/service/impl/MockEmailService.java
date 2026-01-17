package com.centreformation.service.impl;

import com.centreformation.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Implementation du service d'email pour le développement.
 * - Si spring.mail.host est configuré → envoie de vrais emails
 * - Sinon → simule l'envoi en loggant dans la console
 * Activé uniquement avec le profil "dev".
 */
@Service
@Profile("dev")
@Slf4j
public class MockEmailService implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.host:}")
    private String mailHost;
    
    private boolean isMailConfigured() {
        return mailHost != null && !mailHost.isEmpty() && mailSender != null;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        if (isMailConfigured()) {
            // Envoyer un vrai email
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                log.info("✅ Email réel envoyé à: {}", to);
            } catch (Exception e) {
                log.error("❌ Erreur envoi email à {}: {}", to, e.getMessage());
            }
        } else {
            // Mode mock - afficher dans les logs
            log.info("========== MOCK EMAIL ==========");
            log.info("To: {}", to);
            log.info("Subject: {}", subject);
            log.info("Body:\n{}", body);
            log.info("================================");
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        // En mode dev, on envoie en texte simple
        sendEmail(to, subject, htmlBody.replaceAll("<[^>]*>", ""));
    }

    @Override
    public void notifyStudentInscription(String studentEmail, String studentName, String courseName, String formateurName) {
        String subject = "Confirmation d'inscription - " + courseName;
        String body = String.format("""
            Bonjour %s,
            
            Nous avons le plaisir de vous confirmer votre inscription au cours "%s".
            
            Détails du cours:
            - Cours: %s
            - Formateur: %s
            
            Vous pouvez consulter les détails du cours depuis votre espace personnel.
            
            Cordialement,
            L'équipe du Centre de Formation
            """, studentName, courseName, courseName, formateurName);
        
        sendEmail(studentEmail, subject, body);
    }

    @Override
    public void notifyStudentDesinscription(String studentEmail, String studentName, String courseName) {
        String subject = "Annulation d'inscription - " + courseName;
        String body = String.format("""
            Bonjour %s,
            
            Nous vous confirmons l'annulation de votre inscription au cours "%s".
            
            Si vous avez des questions, n'hésitez pas à contacter l'administration.
            
            Cordialement,
            L'équipe du Centre de Formation
            """, studentName, courseName);
        
        sendEmail(studentEmail, subject, body);
    }

    @Override
    public void notifyFormateurNewInscription(String formateurEmail, String formateurName, String studentName, String courseName) {
        String subject = "Nouvelle inscription - " + courseName;
        String body = String.format("""
            Bonjour %s,
            
            Un nouvel étudiant s'est inscrit à votre cours "%s".
            
            Étudiant: %s
            
            Vous pouvez consulter la liste des inscrits depuis votre espace formateur.
            
            Cordialement,
            L'équipe du Centre de Formation
            """, formateurName, courseName, studentName);
        
        sendEmail(formateurEmail, subject, body);
    }

    @Override
    public void notifyFormateurDesinscription(String formateurEmail, String formateurName, String studentName, String courseName) {
        String subject = "Désinscription - " + courseName;
        String body = String.format("""
            Bonjour %s,
            
            L'étudiant %s s'est désinscrit de votre cours "%s".
            
            Cordialement,
            L'équipe du Centre de Formation
            """, formateurName, studentName, courseName);
        
        sendEmail(formateurEmail, subject, body);
    }

    @Override
    public void notifyStudentNewNote(String studentEmail, String studentName, String courseName, Double note, String commentaire) {
        String subject = "Nouvelle note - " + courseName;
        String body = String.format("""
            Bonjour %s,
            
            Une nouvelle note a été saisie pour le cours "%s".
            
            Note: %.2f/20
            %s
            
            Vous pouvez consulter vos notes depuis votre espace personnel.
            
            Cordialement,
            L'équipe du Centre de Formation
            """, studentName, courseName, note, 
            commentaire != null && !commentaire.isEmpty() ? "Commentaire: " + commentaire : "");
        
        sendEmail(studentEmail, subject, body);
    }
}
