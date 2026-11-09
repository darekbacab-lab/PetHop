package com.pethop.security;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    @Modifying @Query("update PasswordResetToken t set t.usedAt=:now where t.user.id=:userId and t.usedAt is null")
    void invalidateForUser(@Param("userId") UUID userId, @Param("now") Instant now);
    @Query("select t from PasswordResetToken t join fetch t.user where t.tokenHash=:hash and t.usedAt is null and t.expiresAt>:now")
    Optional<PasswordResetToken> findActive(@Param("hash") String hash, @Param("now") Instant now);
}
