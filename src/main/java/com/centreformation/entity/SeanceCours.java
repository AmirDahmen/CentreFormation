package com.centreformation.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "seances_cours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeanceCours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La date est obligatoire")
    @Column(nullable = false)
    private LocalDate date;

    @NotNull(message = "L'heure de début est obligatoire")
    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @NotNull(message = "L'heure de fin est obligatoire")
    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    // Relation avec l'entité Salle (optionnelle pour compatibilité)
    @ManyToOne
    @JoinColumn(name = "salle_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Salle salle;
    
    // Ancien champ texte pour compatibilité (sera migré)
    @Column(name = "salle_texte", length = 100)
    private String salleTexte;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    @ManyToOne
    @JoinColumn(name = "cours_id", nullable = false)
    @JsonIgnoreProperties({"inscriptions", "notes", "groupes", "seances"})
    private Cours cours;

    @ManyToOne
    @JoinColumn(name = "groupe_id")
    @JsonIgnoreProperties({"etudiants", "cours", "seances", "sessionPedagogique", "specialite"})
    private Groupe groupe;

    @ManyToOne
    @JoinColumn(name = "formateur_id")
    @JsonIgnoreProperties({"cours", "seances", "utilisateur", "specialite"})
    private Formateur formateur;
}
