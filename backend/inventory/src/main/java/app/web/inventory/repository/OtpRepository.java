package app.web.inventory.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import app.web.inventory.model.Otp;

// OtpRepository.java
public interface OtpRepository extends JpaRepository<Otp, UUID> {
    Optional<Otp> findTopByEmailOrderByExpiresAtDesc(String email);

    void deleteByEmail(String email);
}
