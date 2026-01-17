package com.centreformation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "formateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Formateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Column(nullable = false)
    private String prenom;

    @Email(message = "L'email doit être valide")
    @NotBlank(message = "L'email est obligatoire")
    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 20)
    private String telephone;

    @ManyToOne
    @JoinColumn(name = "specialite_id")
    @JsonIgnoreProperties({"groupes", "etudiants", "formateurs"})
    private Specialite specialite;

    @OneToMany(mappedBy = "formateur", cascade = CascadeType.ALL)
    @Builder.Default
    @JsonIgnore
    private Set<Cours> cours = new HashSet<>();

    @OneToMany(mappedBy = "formateur", cascade = CascadeType.ALL)
    @Builder.Default
    @JsonIgnore
    private Set<SeanceCours> seances = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "utilisateur_id", unique = true)
    @JsonIgnore
    private Utilisateur utilisateur;
}
