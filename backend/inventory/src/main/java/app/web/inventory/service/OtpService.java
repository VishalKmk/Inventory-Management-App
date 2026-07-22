package app.web.inventory.service;

import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import app.web.inventory.model.Otp;
import app.web.inventory.repository.OtpRepository;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class OtpService {
    private final OtpRepository otpRepository;
    private final int ttlMinutes;
    private final SecureRandom rnd = new SecureRandom();

    public OtpService(OtpRepository otpRepository, @Value("${app.otp.ttl-minutes:10}") int ttlMinutes) {
        this.otpRepository = otpRepository;
        this.ttlMinutes = ttlMinutes;
    }

    public Otp createOtpFor(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }

        // Delete any existing OTPs for this email before creating new one
        otpRepository.deleteByEmail(email);

        String code = String.format("%06d", rnd.nextInt(1_000_000));
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setCode(code);
        otp.setExpiresAt(Instant.now().plusSeconds(ttlMinutes * 60L));
        otpRepository.save(otp);
        return otp;
    }

    public boolean verify(String email, String code) {
        if (email == null || code == null) {
            return false;
        }

        var maybe = otpRepository.findTopByEmailOrderByExpiresAtDesc(email);
        if (maybe.isEmpty())
            return false;

        var otp = maybe.get();

        // Always delete expired OTPs immediately
        if (otp.getExpiresAt().isBefore(Instant.now())) {
            otpRepository.delete(otp);
            return false;
        }

        boolean isValid = otp.getCode().equals(code);

        otpRepository.delete(otp);

        return isValid;
    }
}