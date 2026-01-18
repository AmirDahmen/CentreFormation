package com.centreformation.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"etudiant_id", "cours_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Note finale validée (après double saisie)
    @DecimalMin(value = "0.0", message = "La note doit être supérieure ou égale à 0")
    @DecimalMax(value = "20.0", message = "La note doit être inférieure ou égale à 20")
    @Column(nullable = true)
    private Double valeur;

    // Première saisie
    @Column(name = "saisie1_valeur")
    private Double saisie1Valeur;
    
    @Column(name = "saisie1_date")
    private LocalDateTime saisie1Date;
    
    @Column(name = "saisie1_formateur_id")
    private Long saisie1FormateurId;

    // Deuxième saisie
    @Column(name = "saisie2_valeur")
    private Double saisie2Valeur;
    
    @Column(name = "saisie2_date")
    private LocalDateTime saisie2Date;
    
    @Column(name = "saisie2_formateur_id")
    private Long saisie2FormateurId;

    // Statut de la double saisie
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_saisie")
    @Builder.Default
    private StatutSaisie statutSaisie = StatutSaisie.EN_ATTENTE_SAISIE1;

    @Column(name = "date_saisie")
    @Builder.Default
    private LocalDateTime dateSaisie = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @ManyToOne
    @JoinColumn(name = "etudiant_id", nullable = false)
    @JsonIgnoreProperties({"inscriptions", "notes", "groupes", "utilisateur", "hibernateLazyInitializer", "handler"})
    private Etudiant etudiant;

    @ManyToOne
    @JoinColumn(name = "cours_id", nullable = false)
    @JsonIgnoreProperties({"inscriptions", "notes", "groupes", "seances", "hibernateLazyInitializer", "handler"})
    private Cours cours;

    @ManyToOne
    @JoinColumn(name = "formateur_id")
    @JsonIgnoreProperties({"cours", "seances", "utilisateur", "hibernateLazyInitializer", "handler"})
    private Formateur formateur;

    /**
     * Statuts possibles pour la double saisie
     */
    public enum StatutSaisie {
        EN_ATTENTE_SAISIE1,    // Aucune saisie effectuée
        EN_ATTENTE_SAISIE2,    // Première saisie faite, en attente de la seconde
        CONFLIT,               // Les deux saisies ne correspondent pas
        VALIDEE                // Les deux saisies correspondent, note validée
    }

    /**
     * Vérifie si la double saisie est complète et valide
     */
    public boolean isDoubleSaisieComplete() {
        return statutSaisie == StatutSaisie.VALIDEE;
    }

    /**
     * Vérifie si les deux saisies correspondent
     */
    public boolean saisiesesCorrespondent() {
        if (saisie1Valeur == null || saisie2Valeur == null) {
            return false;
        }
        // Tolérance de 0.01 pour les erreurs de précision
        return Math.abs(saisie1Valeur - saisie2Valeur) < 0.01;
    }
}
