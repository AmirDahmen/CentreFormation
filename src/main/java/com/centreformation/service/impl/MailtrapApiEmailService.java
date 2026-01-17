package com.centreformation.service.impl;

import com.centreformation.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation du service d'email utilisant l'API Mailtrap.
 * Activé avec le profil "mailtrap".
 * 
 * Configuration requise dans application.properties:
 * mailtrap.api.token=votre_token_api
 * mailtrap.api.from.email=hello@demomailtrap.co
 * mailtrap.api.from.name=Centre Formation
 */
@Service
@Profile("mailtrap")
@Slf4j
public class MailtrapApiEmailService implements EmailService {

    @Value("${mailtrap.api.token}")
    private String apiToken;
    
    @Value("${mailtrap.api.from.email:hello@demomailtrap.co}")
    private String fromEmail;
    
    @Value("${mailtrap.api.from.name:Centre Formation}")
    private String fromName;
    
    private static final String MAILTRAP_API_URL = "https://send.api.mailtrap.io/api/send";
    
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("from", Map.of("email", fromEmail, "name", fromName));
            payload.put("to", List.of(Map.of("email", to)));
            payload.put("subject", subject);
            payload.put("text", body);
            payload.put("category", "Centre Formation");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                MAILTRAP_API_URL, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Email envoyé via Mailtrap API à: {}", to);
            } else {
                log.error("❌ Erreur Mailtrap API: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Erreur envoi email via Mailtrap API: {}", e.getMessage());
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("from", Map.of("email", fromEmail, "name", fromName));
            payload.put("to", List.of(Map.of("email", to)));
            payload.put("subject", subject);
            payload.put("html", htmlBody);
            payload.put("category", "Centre Formation");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            restTemplate.postForEntity(MAILTRAP_API_URL, request, String.class);
            log.info("✅ Email HTML envoyé via Mailtrap API à: {}", to);
        } catch (Exception e) {
            log.error("❌ Erreur envoi email HTML via Mailtrap API: {}", e.getMessage());
        }
    }

    @Override
    public void notifyStudentInscription(String studentEmail, String studentName, String courseName, String formateurName) {
        String subject = "✅ Confirmation d'inscription - " + courseName;
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
    public void notifyFormateurNewInscription(String formateurEmail, String formateurName, String studentName, String courseName) {
        String subject = "📝 Nouvelle inscription - " + courseName;
        String body = String.format("""
            Bonjour %s,
            
            Un nouvel étudiant s'est inscrit à votre cours "%s".
            
            Étudiant: %s
            
            Cordialement,
            L'équipe du Centre de Formation
            """, formateurName, courseName, studentName);
        
        sendEmail(formateurEmail, subject, body);
    }

    @Override
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
    public void notifyStudentNewNote(String studentEmail, String studentName, String courseName, Double note, String commentaire) {
        String subject = "📊 Nouvelle note - " + courseName;
        String body = String.format("""
            Bonjour %s,
            
            Une nouvelle note a été saisie pour le cours "%s".
            
            Note: %.2f/20
            %s
            
            Consultez vos notes depuis votre espace personnel.
            
            Cordialement,
            L'équipe du Centre de Formation
            """, studentName, courseName, note,
            commentaire != null && !commentaire.isEmpty() ? "Commentaire: " + commentaire : "");
        
        sendEmail(studentEmail, subject, body);
    }
}
