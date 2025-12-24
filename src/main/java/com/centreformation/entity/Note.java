package com.centreformation.entity;

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

    @DecimalMin(value = "0.0", message = "La note doit être supérieure ou égale à 0")
    @DecimalMax(value = "20.0", message = "La note doit être inférieure ou égale à 20")
    @Column(nullable = false)
    private Double valeur;

    @Column(name = "date_saisie")
    @Builder.Default
    private LocalDateTime dateSaisie = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @ManyToOne
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @ManyToOne
    @JoinColumn(name = "cours_id", nullable = false)
    private Cours cours;

    @ManyToOne
    @JoinColumn(name = "formateur_id")
    private Formateur formateur;
}
