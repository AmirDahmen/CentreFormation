package com.centreformation.service;

/**
 * Interface pour le service d'envoi d'emails
 */
public interface EmailService {
    
    /**
     * Envoie un email simple
     */
    void sendEmail(String to, String subject, String body);
    
    /**
     * Envoie un email HTML
     */
    void sendHtmlEmail(String to, String subject, String htmlBody);
    
    /**
     * Notifie un étudiant de son inscription à un cours
     */
    void notifyStudentInscription(String studentEmail, String studentName, String courseName, String formateurName);
    
    /**
     * Notifie un étudiant de l'annulation de son inscription
     */
    void notifyStudentDesinscription(String studentEmail, String studentName, String courseName);
    
    /**
     * Notifie un formateur qu'un étudiant s'est inscrit à son cours
     */
    void notifyFormateurNewInscription(String formateurEmail, String formateurName, String studentName, String courseName);
    
    /**
     * Notifie un formateur qu'un étudiant s'est désinscrit de son cours
     */
    void notifyFormateurDesinscription(String formateurEmail, String formateurName, String studentName, String courseName);
    
    /**
     * Notifie un étudiant de sa nouvelle note
     */
    void notifyStudentNewNote(String studentEmail, String studentName, String courseName, Double note, String commentaire);
}
