package com.centreformation.entity;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutInscription statut = StatutInscription.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @ManyToOne
    @JoinColumn(name = "cours_id", nullable = false)
    private Cours cours;

    public enum StatutInscription {
        ACTIVE,
        ANNULEE,
        TERMINEE
    }
}
