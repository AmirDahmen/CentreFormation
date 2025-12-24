package com.centreformation.entity;

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
    private SessionPedagogique sessionPedagogique;

    @ManyToOne
    @JoinColumn(name = "specialite_id")
    private Specialite specialite;

    @ManyToMany(mappedBy = "groupes")
    @Builder.Default
    private Set<Etudiant> etudiants = new HashSet<>();

    @ManyToMany(mappedBy = "groupes")
    @Builder.Default
    private Set<Cours> cours = new HashSet<>();

    @OneToMany(mappedBy = "groupe", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<SeanceCours> seances = new HashSet<>();
}
