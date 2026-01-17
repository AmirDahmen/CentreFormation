package com.centreformation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le code du cours est obligatoire")
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @NotBlank(message = "Le titre est obligatoire")
    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "nombre_heures")
    private Integer nombreHeures;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formateur_id", referencedColumnName = "id", 
                foreignKey = @ForeignKey(name = "fk_cours_formateur"))
    @JsonIgnoreProperties({"cours", "seances", "utilisateur", "specialite"})
    private Formateur formateur;

    @OneToMany(mappedBy = "cours", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private Set<Inscription> inscriptions = new HashSet<>();

    @OneToMany(mappedBy = "cours", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private Set<Note> notes = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "cours_groupes",
        joinColumns = @JoinColumn(name = "cours_id"),
        inverseJoinColumns = @JoinColumn(name = "groupe_id")
    )
    @Builder.Default
    @JsonIgnore
    private Set<Groupe> groupes = new HashSet<>();

    @OneToMany(mappedBy = "cours", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private Set<SeanceCours> seances = new HashSet<>();
}
