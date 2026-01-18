package com.centreformation.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inscriptions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"etudiant_id", "cours_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_inscription", nullable = false)
    @Builder.Default
    private LocalDateTime dateInscription = LocalDateTime.now();

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutInscription statut = StatutInscription.EN_ATTENTE;

    @ManyToOne
    @JoinColumn(name = "etudiant_id", nullable = false)
    @JsonIgnoreProperties({"inscriptions", "notes", "groupes", "utilisateur", "hibernateLazyInitializer", "handler"})
    private Etudiant etudiant;

    @ManyToOne
    @JoinColumn(name = "cours_id", nullable = false)
    @JsonIgnoreProperties({"inscriptions", "notes", "groupes", "seances", "hibernateLazyInitializer", "handler"})
    private Cours cours;

    // Groupe assigné par l'admin lors de la validation
    @ManyToOne
    @JoinColumn(name = "groupe_id")
    @JsonIgnoreProperties({"etudiants", "cours", "seances", "hibernateLazyInitializer", "handler"})
    private Groupe groupe;

    /**
     * Statuts possibles pour une inscription:
     * - EN_ATTENTE: L'étudiant a demandé l'inscription, en attente de validation admin
     * - VALIDEE: L'admin a validé et assigné un groupe
     * - REFUSEE: L'admin a refusé l'inscription
     * - ANNULEE: L'inscription a été annulée (par étudiant ou admin)
     */
    public enum StatutInscription {
        EN_ATTENTE,
        VALIDEE,
        REFUSEE,
        ANNULEE
    }
}
