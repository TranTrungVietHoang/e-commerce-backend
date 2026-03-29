package com.ecommerce.repository;

import com.ecommerce.entity.User;
import com.ecommerce.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByUserEmailAndTokenAndType(String email, String token, String type);

    @Modifying
    @Query("DELETE FROM VerificationToken v WHERE v.user = :user AND v.type = :type")
    void deleteByUserAndType(User user, String type);
}
