package com.centreformation.service.impl;

import com.centreformation.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Implementation réelle du service d'email pour la production.
 * Utilise JavaMailSender pour envoyer de vrais emails.
 * Activé uniquement avec le profil "prod".
 */
@Service
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@centreformation.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            log.info("Email envoyé à: {}", to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email à {}: {}", to, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("noreply@centreformation.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            mailSender.send(message);
            log.info("Email HTML envoyé à: {}", to);
        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email HTML à {}: {}", to, e.getMessage());
        }
    }

    @Override
    @Async
    public void notifyStudentInscription(String studentEmail, String studentName, String courseName, String formateurName) {
        String subject = "✅ Confirmation d'inscription - " + courseName;
        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background: #f9f9f9; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    .details { background: white; padding: 15px; border-radius: 5px; margin: 15px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎓 Centre de Formation</h1>
                    </div>
                    <div class="content">
                        <h2>Bonjour %s,</h2>
                        <p>Nous avons le plaisir de vous confirmer votre inscription au cours.</p>
                        <div class="details">
                            <p><strong>📚 Cours:</strong> %s</p>
                            <p><strong>👨‍🏫 Formateur:</strong> %s</p>
                        </div>
                        <p>Vous pouvez consulter les détails depuis votre espace personnel.</p>
                    </div>
                    <div class="footer">
                        <p>Centre de Formation - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """, studentName, courseName, formateurName);
        
        sendHtmlEmail(studentEmail, subject, htmlBody);
    }

    @Override
    @Async
    public void notifyStudentDesinscription(String studentEmail, String studentName, String courseName) {
        String subject = "❌ Annulation d'inscription - " + courseName;
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
    @Async
    public void notifyFormateurNewInscription(String formateurEmail, String formateurName, String studentName, String courseName) {
        String subject = "📝 Nouvelle inscription - " + courseName;
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
    @Async
    public void notifyFormateurDesinscription(String formateurEmail, String formateurName, String studentName, String courseName) {
        String subject = "🚫 Désinscription - " + courseName;
        String body = String.format("""
            Bonjour %s,
            
            L'étudiant %s s'est désinscrit de votre cours "%s".
            
            Cordialement,
            L'équipe du Centre de Formation
            """, formateurName, studentName, courseName);
        
        sendEmail(formateurEmail, subject, body);
    }

    @Override
    @Async
    public void notifyStudentNewNote(String studentEmail, String studentName, String courseName, Double note, String commentaire) {
        String subject = "📊 Nouvelle note - " + courseName;
        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #2196F3; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background: #f9f9f9; }
                    .note { font-size: 48px; text-align: center; color: %s; font-weight: bold; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📊 Nouvelle Note</h1>
                    </div>
                    <div class="content">
                        <h2>Bonjour %s,</h2>
                        <p>Une nouvelle note a été saisie pour le cours <strong>%s</strong>.</p>
                        <div class="note">%.2f/20</div>
                        %s
                        <p>Consultez vos notes depuis votre espace personnel.</p>
                    </div>
                    <div class="footer">
                        <p>Centre de Formation - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            note >= 10 ? "#4CAF50" : "#f44336",
            studentName, 
            courseName, 
            note,
            commentaire != null && !commentaire.isEmpty() ? "<p><em>Commentaire: " + commentaire + "</em></p>" : "");
        
        sendHtmlEmail(studentEmail, subject, htmlBody);
    }
}
