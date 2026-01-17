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
    private String nom;

    @Column(unique = true, length = 20)
    private String code;

    @ManyToOne
    @JoinColumn(name = "session_pedagogique_id")
    @JsonIgnoreProperties({"groupes"})
    private SessionPedagogique sessionPedagogique;

    @ManyToOne
    @JoinColumn(name = "specialite_id")
    @JsonIgnoreProperties({"groupes", "etudiants", "formateurs"})
    private Specialite specialite;

    @ManyToMany(mappedBy = "groupes")
    @Builder.Default
    @JsonIgnore
    private Set<Etudiant> etudiants = new HashSet<>();

    @ManyToMany(mappedBy = "groupes")
    @Builder.Default
    @JsonIgnore
    private Set<Cours> cours = new HashSet<>();

    @OneToMany(mappedBy = "groupe", cascade = CascadeType.ALL)
    @Builder.Default
    @JsonIgnore
    private Set<SeanceCours> seances = new HashSet<>();
}
