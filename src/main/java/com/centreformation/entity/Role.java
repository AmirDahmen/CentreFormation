package com.centreformation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nom;

    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    private Set<Utilisateur> utilisateurs = new HashSet<>();

    public Role(String nom) {
        this.nom = nom;
    }
}
