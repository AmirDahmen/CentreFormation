package com.centreformation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "specialites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Specialite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom de la spécialité est obligatoire")
    @Column(nullable = false, unique = true)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "specialite", cascade = CascadeType.ALL)
    @Builder.Default
    @JsonIgnore
    private Set<Groupe> groupes = new HashSet<>();

    @OneToMany(mappedBy = "specialite")
    @Builder.Default
    @JsonIgnore
    private Set<Etudiant> etudiants = new HashSet<>();

    @OneToMany(mappedBy = "specialite")
    @Builder.Default
    @JsonIgnore
    private Set<Formateur> formateurs = new HashSet<>();
}
