package com.centreformation.service;

import com.centreformation.entity.*;

import java.util.List;
import java.util.Map;

/**
 * Service de génération de rapports PDF
 */
public interface PdfService {
    
    /**
     * Génère le bulletin de notes d'un étudiant
     * @param etudiant L'étudiant
     * @param notes Liste des notes
     * @param moyenne Moyenne générale
     * @return Le contenu PDF en bytes
     */
    byte[] generateBulletinNotes(Etudiant etudiant, List<Note> notes, Double moyenne);
    
    /**
     * Génère la liste des étudiants d'un cours
     * @param cours Le cours
     * @param inscriptions Liste des inscriptions
     * @return Le contenu PDF en bytes
     */
    byte[] generateListeEtudiantsCours(Cours cours, List<Inscription> inscriptions);
    
    /**
     * Génère le relevé de notes d'un cours
     * @param cours Le cours
     * @param notes Liste des notes
     * @param moyenne Moyenne du cours
     * @param tauxReussite Taux de réussite
     * @return Le contenu PDF en bytes
     */
    byte[] generateReleveNotesCours(Cours cours, List<Note> notes, Double moyenne, Double tauxReussite);
    
    /**
     * Génère un rapport statistique global
     * @param stats Map contenant les statistiques
     * @return Le contenu PDF en bytes
     */
    byte[] generateRapportStatistiques(Map<String, Object> stats);
    
    /**
     * Génère l'attestation d'inscription d'un étudiant
     * @param etudiant L'étudiant
     * @param inscriptions Liste des inscriptions actives
     * @return Le contenu PDF en bytes
     */
    byte[] generateAttestationInscription(Etudiant etudiant, List<Inscription> inscriptions);
}
