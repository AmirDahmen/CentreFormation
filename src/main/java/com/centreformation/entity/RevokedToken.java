package com.centreformation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entité pour stocker les tokens révoqués (blacklist)
 * Permet d'invalider les access tokens avant leur expiration naturelle
 */
@Entity
@Table(name = "revoked_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false)
    private Instant revokedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(length = 100)
    private String reason;
}
