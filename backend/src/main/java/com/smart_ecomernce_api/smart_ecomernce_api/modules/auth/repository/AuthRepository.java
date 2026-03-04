package com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<Auth, Long> {

        Optional<Auth> findByRefreshToken(String refreshToken);


        /**
         * Find the most recent active session for a user (ordered by last activity).
         */
        @Query("SELECT a FROM Auth a WHERE a.user.id = :userId AND a.isActive = true ORDER BY a.lastActivityAt DESC LIMIT 1")
        Optional<Auth> findTopByUserIdAndIsActiveTrueOrderByLastActivityAtDesc(@Param("userId") Long userId);

        @Modifying
        @Query("UPDATE Auth a SET a.isActive = false, a.loggedOutAt = :logoutTime " +
                        "WHERE a.user.id = :userId AND a.isActive = true")
        int invalidateAllUserSessions(@Param("userId") Long userId,
                        @Param("logoutTime") LocalDateTime logoutTime);

        @Modifying
        @Query("UPDATE Auth a SET a.isActive = false " +
                        "WHERE a.isActive = true AND a.expiresAt <= :now")
        int invalidateExpiredSessions(@Param("now") LocalDateTime now);
}
