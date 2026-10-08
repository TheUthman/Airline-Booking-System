package com.airline.authservice.repository;

import com.airline.authservice.entity.RefreshToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserId(Long userId);

    /** Revoke all non-revoked tokens for a user (called before issuing a new token). */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.id = :userId AND t.revoked = false")
    int revokeAllActiveForUser(@Param("userId") Long userId);

    /** Delete tokens that are either revoked or past their expiry date. Returns deleted count. */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken t WHERE t.revoked = true OR t.expiresAt < :now")
    int deleteExpiredOrRevoked(@Param("now") LocalDateTime now);
}
