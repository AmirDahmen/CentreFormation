package com.centreformation.repository;

import com.centreformation.entity.RefreshToken;
import com.centreformation.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.utilisateur = :utilisateur")
    void deleteByUtilisateur(Utilisateur utilisateur);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.utilisateur = :utilisateur")
    void revokeAllByUtilisateur(Utilisateur utilisateur);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < CURRENT_TIMESTAMP")
    void deleteExpiredTokens();
}
