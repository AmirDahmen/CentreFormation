package com.centreformation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "salles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Salle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 20)
    private String code; // Ex: A101, B203, LAB1
    
    @Column(length = 100)
    private String nom; // Ex: Salle de cours 1, Laboratoire Informatique
    
    @Column
    private Integer capacite; // Nombre de places
    
    @Column(length = 50)
    private String type; // COURS, LABO, AMPHI, TP
    
    @Column(length = 50)
    private String batiment; // Bâtiment A, B, etc.
    
    @Column
    private Integer etage;
    
    @Column(nullable = false)
    private Boolean actif = true;
    
    // Équipements disponibles
    @Column
    private Boolean hasProjecteur = false;
    
    @Column
    private Boolean hasOrdinateurs = false;
    
    @Column
    private Boolean hasTableauBlanc = true;
    
    @Override
    public String toString() {
        return code + (nom != null ? " - " + nom : "");
    }
}
