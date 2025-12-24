package com.centreformation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Column(nullable = false)
    private String password;

    @Email(message = "L'email doit être valide")
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "utilisateurs_roles",
        joinColumns = @JoinColumn(name = "utilisateur_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @OneToOne(mappedBy = "utilisateur", cascade = CascadeType.ALL)
    private Etudiant etudiantLie;

    @OneToOne(mappedBy = "utilisateur", cascade = CascadeType.ALL)
    private Formateur formateurLie;
}
