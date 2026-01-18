package com.centreformation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "groupes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Groupe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du groupe est obligatoire")
    @Column(nullable = false)
    private String nom; // Ex: "Groupe 1", "Groupe A"

    @Column(unique = true, length = 20)
    private String code; // Ex: "JAVA-G1", "PY-G2"

    // Capacité maximale du groupe (nombre de places)
    @Column(name = "capacite")
    @Builder.Default
    private Integer capacite = 30;

    // Un groupe appartient à UN cours (pas ManyToMany)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cours_id")
    @JsonIgnoreProperties({"groupes", "inscriptions", "notes", "seances", "hibernateLazyInitializer", "handler"})
    private Cours cours;

    @ManyToOne
    @JoinColumn(name = "specialite_id")
    @JsonIgnoreProperties({"groupes", "etudiants", "formateurs", "hibernateLazyInitializer", "handler"})
    private Specialite specialite;

    // Les étudiants du groupe (via inscription validée)
    @ManyToMany(mappedBy = "groupes")
    @Builder.Default
    @JsonIgnore
    private Set<Etudiant> etudiants = new HashSet<>();

    // Les séances de ce groupe
    @OneToMany(mappedBy = "groupe", cascade = CascadeType.ALL)
    @Builder.Default
    @JsonIgnore
    private Set<SeanceCours> seances = new HashSet<>();
    
    // Les inscriptions validées pour ce groupe
    @OneToMany(mappedBy = "groupe")
    @Builder.Default
    @JsonIgnore
    private Set<Inscription> inscriptions = new HashSet<>();
    
    // Méthode utilitaire pour compter les places restantes
    public int getPlacesRestantes() {
        if (capacite == null) return Integer.MAX_VALUE;
        long inscriptionsValidees = inscriptions.stream()
            .filter(i -> i.getStatut() == Inscription.StatutInscription.VALIDEE)
            .count();
        return Math.max(0, capacite - (int) inscriptionsValidees);
    }
    
    // Nom complet incluant le cours
    public String getNomComplet() {
        if (cours != null) {
            return cours.getTitre() + " - " + nom;
        }
        return nom;
    }
}
