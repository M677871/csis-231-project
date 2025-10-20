package com.csis231.api.auth.Otp;

import com.csis231.api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {


    Optional<OtpCode> findTopByUserIdAndPurposeOrderByIdDesc(Long userId, String purpose);

    // Used by the invalidation step (if you don’t already have one)
    @Query("select c from OtpCode c where c.user.id = :userId and c.purpose = :purpose and c.consumedAt is null and c.expiresAt > :now")
    List<OtpCode> findActiveByUserId(@Param("purpose") String purpose,
                                     @Param("userId") Long userId,
                                     @Param("now") Instant now);
}
