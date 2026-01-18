package com.centreformation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sessions_pedagogiques")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionPedagogique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String nom; // Ex: "Session Automne 2025"

    @NotBlank(message = "L'année scolaire est obligatoire")
    @Column(name = "annee_scolaire", nullable = false, length = 20)
    private String anneeScolaire; // Ex: "2025-2026"

    @Column(length = 10)
    private String semestre; // Ex: "S1", "S2"

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "actif")
    @Builder.Default
    private Boolean actif = true;

    // Une session contient plusieurs cours
    @OneToMany(mappedBy = "sessionPedagogique", cascade = CascadeType.ALL)
    @Builder.Default
    @JsonIgnore
    private Set<Cours> cours = new HashSet<>();
    
    // Méthode utilitaire pour obtenir un nom d'affichage
    public String getDisplayName() {
        if (nom != null && !nom.isEmpty()) {
            return nom;
        }
        return anneeScolaire + (semestre != null ? " - " + semestre : "");
    }
}
