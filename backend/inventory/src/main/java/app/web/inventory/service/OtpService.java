package app.web.inventory.service;

import java.time.Instant;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import app.web.inventory.model.Otp;
import app.web.inventory.repository.OtpRepository;

@Service
public class OtpService {
    private final OtpRepository otpRepository;
    private final int ttlMinutes;
    private final Random rnd = new Random();

    public OtpService(OtpRepository otpRepository, @Value("${app.otp.ttl-minutes:10}") int ttlMinutes) {
        this.otpRepository = otpRepository;
        this.ttlMinutes = ttlMinutes;
    }

    public Otp createOtpFor(String email) {
        String code = String.format("%06d", rnd.nextInt(1_000_000));
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setCode(code);
        otp.setExpiresAt(Instant.now().plusSeconds(ttlMinutes * 60L));
        otpRepository.save(otp);
        return otp;
    }

    public boolean verify(String email, String code) {
        var maybe = otpRepository.findTopByEmailOrderByExpiresAtDesc(email);
        if (maybe.isEmpty())
            return false;
        var otp = maybe.get();
        if (otp.getExpiresAt().isBefore(Instant.now()))
            return false;
        return otp.getCode().equals(code);
    }
}
