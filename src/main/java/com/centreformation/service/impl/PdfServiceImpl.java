package com.centreformation.service.impl;

import com.centreformation.entity.*;
import com.centreformation.service.PdfService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Implémentation du service de génération de rapports PDF
 * Utilise OpenPDF (fork de iText)
 */
@Service
public class PdfServiceImpl implements PdfService {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfServiceImpl.class);
    
    // Couleurs du thème
    private static final Color PRIMARY_COLOR = new Color(52, 73, 94);      // Bleu foncé
    private static final Color SECONDARY_COLOR = new Color(46, 204, 113);  // Vert
    private static final Color HEADER_BG = new Color(236, 240, 241);       // Gris clair
    private static final Color TABLE_BORDER = new Color(189, 195, 199);    // Gris
    
    // Polices
    private Font titleFont;
    private Font subtitleFont;
    private Font headerFont;
    private Font normalFont;
    private Font boldFont;
    private Font smallFont;
    
    public PdfServiceImpl() {
        initFonts();
    }
    
    private void initFonts() {
        titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
        subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY_COLOR);
        headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
    }
    
    @Override
    public byte[] generateBulletinNotes(Etudiant etudiant, List<Note> notes, Double moyenne) {
        logger.info("Génération du bulletin de notes pour l'étudiant: {}", etudiant.getMatricule());
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();
            
            // En-tête
            addHeader(document, "BULLETIN DE NOTES");
            
            // Informations étudiant
            addStudentInfo(document, etudiant);
            
            // Tableau des notes
            if (!notes.isEmpty()) {
                document.add(new Paragraph(" "));
                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{3, 2, 2, 2});
                
                // En-têtes
                addTableHeader(table, "Cours", "Code", "Note", "Appréciation");
                
                // Données
                for (Note note : notes) {
                    table.addCell(createCell(note.getCours().getTitre(), normalFont, Element.ALIGN_LEFT));
                    table.addCell(createCell(note.getCours().getCode(), normalFont, Element.ALIGN_CENTER));
                    table.addCell(createCell(String.format("%.2f/20", note.getValeur()), 
                            note.getValeur() >= 10 ? boldFont : normalFont, Element.ALIGN_CENTER));
                    table.addCell(createCell(getAppreciation(note.getValeur()), normalFont, Element.ALIGN_CENTER));
                }
                
                document.add(table);
            } else {
                document.add(new Paragraph("Aucune note enregistrée.", normalFont));
            }
            
            // Moyenne générale
            document.add(new Paragraph(" "));
            PdfPTable moyenneTable = new PdfPTable(2);
            moyenneTable.setWidthPercentage(50);
            moyenneTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            PdfPCell labelCell = new PdfPCell(new Phrase("Moyenne Générale:", boldFont));
            labelCell.setBorder(Rectangle.BOX);
            labelCell.setBackgroundColor(HEADER_BG);
            labelCell.setPadding(8);
            moyenneTable.addCell(labelCell);
            
            String moyenneStr = moyenne != null ? String.format("%.2f/20", moyenne) : "N/A";
            PdfPCell valueCell = new PdfPCell(new Phrase(moyenneStr, titleFont));
            valueCell.setBorder(Rectangle.BOX);
            valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            valueCell.setPadding(8);
            valueCell.setBackgroundColor(moyenne != null && moyenne >= 10 ? 
                    new Color(212, 239, 223) : new Color(253, 237, 236));
            moyenneTable.addCell(valueCell);
            
            document.add(moyenneTable);
            
            // Pied de page
            addFooter(document);
            
            document.close();
            logger.info("Bulletin de notes généré avec succès");
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du bulletin PDF", e);
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }
    
    @Override
    public byte[] generateListeEtudiantsCours(Cours cours, List<Inscription> inscriptions) {
        logger.info("Génération de la liste des étudiants pour le cours: {}", cours.getCode());
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();
            
            // En-tête
            addHeader(document, "LISTE DES ÉTUDIANTS INSCRITS");
            
            // Informations cours
            addCourseInfo(document, cours);
            
            // Tableau des étudiants
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Étudiants inscrits: " + inscriptions.size(), subtitleFont));
            document.add(new Paragraph(" "));
            
            if (!inscriptions.isEmpty()) {
                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1, 2, 2, 3, 2});
                
                // En-têtes
                addTableHeader(table, "N°", "Matricule", "Nom", "Email", "Date inscription");
                
                // Données
                int i = 1;
                for (Inscription insc : inscriptions) {
                    Etudiant etu = insc.getEtudiant();
                    table.addCell(createCell(String.valueOf(i++), normalFont, Element.ALIGN_CENTER));
                    table.addCell(createCell(etu.getMatricule(), normalFont, Element.ALIGN_CENTER));
                    table.addCell(createCell(etu.getNom() + " " + etu.getPrenom(), normalFont, Element.ALIGN_LEFT));
                    table.addCell(createCell(etu.getEmail(), normalFont, Element.ALIGN_LEFT));
                    table.addCell(createCell(formatDate(insc.getDateInscription()), normalFont, Element.ALIGN_CENTER));
                }
                
                document.add(table);
            } else {
                document.add(new Paragraph("Aucun étudiant inscrit à ce cours.", normalFont));
            }
            
            // Pied de page
            addFooter(document);
            
            document.close();
            logger.info("Liste des étudiants générée avec succès");
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération de la liste PDF", e);
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }
    
    @Override
    public byte[] generateReleveNotesCours(Cours cours, List<Note> notes, Double moyenne, Double tauxReussite) {
        logger.info("Génération du relevé de notes pour le cours: {}", cours.getCode());
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();
            
            // En-tête
            addHeader(document, "RELEVÉ DE NOTES");
            
            // Informations cours
            addCourseInfo(document, cours);
            
            // Statistiques
            document.add(new Paragraph(" "));
            PdfPTable statsTable = new PdfPTable(3);
            statsTable.setWidthPercentage(100);
            
            addStatsCell(statsTable, "Nombre d'étudiants", String.valueOf(notes.size()));
            addStatsCell(statsTable, "Moyenne du cours", moyenne != null ? String.format("%.2f/20", moyenne) : "N/A");
            addStatsCell(statsTable, "Taux de réussite", tauxReussite != null ? String.format("%.1f%%", tauxReussite) : "N/A");
            
            document.add(statsTable);
            
            // Tableau des notes
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Détail des notes:", subtitleFont));
            document.add(new Paragraph(" "));
            
            if (!notes.isEmpty()) {
                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1, 2, 3, 2, 2});
                
                // En-têtes
                addTableHeader(table, "N°", "Matricule", "Nom Prénom", "Note", "Résultat");
                
                // Données triées par note décroissante
                notes.stream()
                    .sorted((n1, n2) -> Double.compare(n2.getValeur(), n1.getValeur()))
                    .forEach(note -> {
                        Etudiant etu = note.getEtudiant();
                        table.addCell(createCell(String.valueOf(table.getRows().size()), normalFont, Element.ALIGN_CENTER));
                        table.addCell(createCell(etu.getMatricule(), normalFont, Element.ALIGN_CENTER));
                        table.addCell(createCell(etu.getNom() + " " + etu.getPrenom(), normalFont, Element.ALIGN_LEFT));
                        table.addCell(createCell(String.format("%.2f", note.getValeur()), 
                                note.getValeur() >= 10 ? boldFont : normalFont, Element.ALIGN_CENTER));
                        
                        String resultat = note.getValeur() >= 10 ? "Validé" : "Non validé";
                        Color bgColor = note.getValeur() >= 10 ? new Color(212, 239, 223) : new Color(253, 237, 236);
                        PdfPCell cell = createCell(resultat, boldFont, Element.ALIGN_CENTER);
                        cell.setBackgroundColor(bgColor);
                        table.addCell(cell);
                    });
                
                document.add(table);
            } else {
                document.add(new Paragraph("Aucune note enregistrée pour ce cours.", normalFont));
            }
            
            // Pied de page
            addFooter(document);
            
            document.close();
            logger.info("Relevé de notes généré avec succès");
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du relevé PDF", e);
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }
    
    @Override
    public byte[] generateRapportStatistiques(Map<String, Object> stats) {
        logger.info("Génération du rapport statistique global");
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();
            
            // En-tête
            addHeader(document, "RAPPORT STATISTIQUE");
            
            document.add(new Paragraph("Vue d'ensemble du centre de formation", subtitleFont));
            document.add(new Paragraph(" "));
            
            // Statistiques générales
            PdfPTable statsTable = new PdfPTable(2);
            statsTable.setWidthPercentage(100);
            statsTable.setWidths(new float[]{3, 2});
            
            // En-têtes
            PdfPCell labelHeader = new PdfPCell(new Phrase("Indicateur", headerFont));
            labelHeader.setBackgroundColor(PRIMARY_COLOR);
            labelHeader.setPadding(10);
            statsTable.addCell(labelHeader);
            
            PdfPCell valueHeader = new PdfPCell(new Phrase("Valeur", headerFont));
            valueHeader.setBackgroundColor(PRIMARY_COLOR);
            valueHeader.setPadding(10);
            valueHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            statsTable.addCell(valueHeader);
            
            // Données
            addStatRow(statsTable, "Nombre total d'étudiants", stats.getOrDefault("totalEtudiants", 0));
            addStatRow(statsTable, "Nombre total de formateurs", stats.getOrDefault("totalFormateurs", 0));
            addStatRow(statsTable, "Nombre total de cours", stats.getOrDefault("totalCours", 0));
            addStatRow(statsTable, "Inscriptions actives", stats.getOrDefault("inscriptionsActives", 0));
            addStatRow(statsTable, "Notes enregistrées", stats.getOrDefault("totalNotes", 0));
            addStatRow(statsTable, "Moyenne générale", 
                    stats.get("moyenneGenerale") != null ? 
                    String.format("%.2f/20", stats.get("moyenneGenerale")) : "N/A");
            addStatRow(statsTable, "Taux de réussite global", 
                    stats.get("tauxReussiteGlobal") != null ? 
                    String.format("%.1f%%", stats.get("tauxReussiteGlobal")) : "N/A");
            
            document.add(statsTable);
            
            // Cours populaires
            if (stats.containsKey("coursPopulaires")) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Cours les plus suivis:", subtitleFont));
                document.add(new Paragraph(" "));
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> coursPopulaires = (List<Map<String, Object>>) stats.get("coursPopulaires");
                
                if (coursPopulaires != null && !coursPopulaires.isEmpty()) {
                    PdfPTable coursTable = new PdfPTable(3);
                    coursTable.setWidthPercentage(100);
                    coursTable.setWidths(new float[]{1, 4, 2});
                    
                    addTableHeader(coursTable, "Rang", "Cours", "Inscriptions");
                    
                    int rang = 1;
                    for (Map<String, Object> cp : coursPopulaires) {
                        coursTable.addCell(createCell(String.valueOf(rang++), normalFont, Element.ALIGN_CENTER));
                        coursTable.addCell(createCell(String.valueOf(cp.get("titre")), normalFont, Element.ALIGN_LEFT));
                        coursTable.addCell(createCell(String.valueOf(cp.get("nombreInscrits")), boldFont, Element.ALIGN_CENTER));
                    }
                    
                    document.add(coursTable);
                }
            }
            
            // Pied de page
            addFooter(document);
            
            document.close();
            logger.info("Rapport statistique généré avec succès");
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du rapport PDF", e);
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }
    
    @Override
    public byte[] generateAttestationInscription(Etudiant etudiant, List<Inscription> inscriptions) {
        logger.info("Génération de l'attestation d'inscription pour: {}", etudiant.getMatricule());
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 80, 50);
            PdfWriter.getInstance(document, baos);
            document.open();
            
            // En-tête
            addHeader(document, "ATTESTATION D'INSCRIPTION");
            
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            
            // Corps de l'attestation
            Paragraph intro = new Paragraph();
            intro.add(new Chunk("Je soussigné, le Directeur du Centre de Formation, atteste que :", normalFont));
            intro.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(intro);
            
            document.add(new Paragraph(" "));
            
            // Informations étudiant encadrées
            PdfPTable infoTable = new PdfPTable(1);
            infoTable.setWidthPercentage(80);
            infoTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            StringBuilder infoText = new StringBuilder();
            infoText.append("M./Mme ").append(etudiant.getNom().toUpperCase())
                    .append(" ").append(etudiant.getPrenom()).append("\n");
            infoText.append("Matricule: ").append(etudiant.getMatricule()).append("\n");
            infoText.append("Email: ").append(etudiant.getEmail());
            
            PdfPCell infoCell = new PdfPCell(new Phrase(infoText.toString(), boldFont));
            infoCell.setPadding(15);
            infoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            infoCell.setBackgroundColor(HEADER_BG);
            infoTable.addCell(infoCell);
            
            document.add(infoTable);
            document.add(new Paragraph(" "));
            
            // Texte
            Paragraph body = new Paragraph();
            body.add(new Chunk("est régulièrement inscrit(e) dans notre établissement pour l'année académique en cours.", normalFont));
            body.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(body);
            
            document.add(new Paragraph(" "));
            
            // Liste des cours
            if (!inscriptions.isEmpty()) {
                document.add(new Paragraph("Cours suivis:", subtitleFont));
                document.add(new Paragraph(" "));
                
                PdfPTable coursTable = new PdfPTable(3);
                coursTable.setWidthPercentage(100);
                coursTable.setWidths(new float[]{1, 3, 3});
                
                addTableHeader(coursTable, "N°", "Code", "Intitulé du cours");
                
                int i = 1;
                for (Inscription insc : inscriptions) {
                    coursTable.addCell(createCell(String.valueOf(i++), normalFont, Element.ALIGN_CENTER));
                    coursTable.addCell(createCell(insc.getCours().getCode(), normalFont, Element.ALIGN_CENTER));
                    coursTable.addCell(createCell(insc.getCours().getTitre(), normalFont, Element.ALIGN_LEFT));
                }
                
                document.add(coursTable);
            }
            
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            
            // Formule de conclusion
            Paragraph conclusion = new Paragraph();
            conclusion.add(new Chunk("Cette attestation est délivrée pour servir et valoir ce que de droit.", normalFont));
            conclusion.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(conclusion);
            
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            
            // Signature
            PdfPTable signatureTable = new PdfPTable(1);
            signatureTable.setWidthPercentage(40);
            signatureTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            String dateStr = "Fait à Tunis, le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, normalFont));
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            signatureTable.addCell(dateCell);
            
            PdfPCell sigCell = new PdfPCell(new Phrase("\n\n\nLe Directeur", boldFont));
            sigCell.setBorder(Rectangle.NO_BORDER);
            sigCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            signatureTable.addCell(sigCell);
            
            document.add(signatureTable);
            
            // Pied de page
            addFooter(document);
            
            document.close();
            logger.info("Attestation d'inscription générée avec succès");
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération de l'attestation PDF", e);
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }
    
    // ========== Méthodes utilitaires ==========
    
    private void addHeader(Document document, String title) throws DocumentException {
        // Titre du centre
        Paragraph centerName = new Paragraph("CENTRE DE FORMATION", subtitleFont);
        centerName.setAlignment(Element.ALIGN_CENTER);
        document.add(centerName);
        
        Paragraph centerSubtitle = new Paragraph("Institut International de Technologie", normalFont);
        centerSubtitle.setAlignment(Element.ALIGN_CENTER);
        document.add(centerSubtitle);
        
        // Ligne de séparation
        document.add(new Paragraph(" "));
        LineSeparator line = new LineSeparator();
        line.setLineColor(PRIMARY_COLOR);
        line.setLineWidth(2f);
        document.add(new Chunk(line));
        document.add(new Paragraph(" "));
        
        // Titre du document
        Paragraph docTitle = new Paragraph(title, titleFont);
        docTitle.setAlignment(Element.ALIGN_CENTER);
        docTitle.setSpacingAfter(20);
        document.add(docTitle);
    }
    
    private void addStudentInfo(Document document, Etudiant etudiant) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});
        
        addInfoRow(table, "Matricule", etudiant.getMatricule());
        addInfoRow(table, "Nom", etudiant.getNom());
        addInfoRow(table, "Prénom", etudiant.getPrenom());
        addInfoRow(table, "Email", etudiant.getEmail());
        if (etudiant.getDateInscription() != null) {
            addInfoRow(table, "Date d'inscription", formatDate(etudiant.getDateInscription()));
        }
        if (etudiant.getSpecialite() != null) {
            addInfoRow(table, "Spécialité", etudiant.getSpecialite().getNom());
        }
        
        document.add(table);
    }
    
    private void addCourseInfo(Document document, Cours cours) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2});
        
        addInfoRow(table, "Code", cours.getCode());
        addInfoRow(table, "Titre", cours.getTitre());
        if (cours.getDescription() != null) {
            addInfoRow(table, "Description", cours.getDescription());
        }
        if (cours.getFormateur() != null) {
            addInfoRow(table, "Formateur", cours.getFormateur().getNom() + " " + cours.getFormateur().getPrenom());
        }
        
        document.add(table);
    }
    
    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label + ":", boldFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(HEADER_BG);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A", normalFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }
    
    private void addTableHeader(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }
    
    private PdfPCell createCell(String content, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(content != null ? content : "", font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(TABLE_BORDER);
        return cell;
    }
    
    private void addStatsCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(15);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(HEADER_BG);
        
        Paragraph p = new Paragraph();
        p.add(new Chunk(value + "\n", titleFont));
        p.add(new Chunk(label, smallFont));
        p.setAlignment(Element.ALIGN_CENTER);
        
        cell.addElement(p);
        table.addCell(cell);
    }
    
    private void addStatRow(PdfPTable table, String label, Object value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, normalFont));
        labelCell.setPadding(8);
        labelCell.setBorderColor(TABLE_BORDER);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(String.valueOf(value), boldFont));
        valueCell.setPadding(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setBorderColor(TABLE_BORDER);
        table.addCell(valueCell);
    }
    
    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        LineSeparator line = new LineSeparator();
        line.setLineColor(Color.LIGHT_GRAY);
        document.add(new Chunk(line));
        
        String footerText = "Document généré le " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) +
                " - Centre de Formation - Tous droits réservés";
        Paragraph footer = new Paragraph(footerText, smallFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }
    
    private String formatDate(LocalDate date) {
        if (date == null) return "N/A";
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
    
    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
    
    private String getAppreciation(Double note) {
        if (note == null) return "N/A";
        if (note >= 16) return "Très bien";
        if (note >= 14) return "Bien";
        if (note >= 12) return "Assez bien";
        if (note >= 10) return "Passable";
        if (note >= 8) return "Insuffisant";
        return "Très insuffisant";
    }
}
