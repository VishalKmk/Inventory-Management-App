package app.web.inventory.dto.auth;

public class OtpVerifyResponseDto {
    private final boolean verified;
    private final int attemptsRemaining;
    private final boolean locked;

    public OtpVerifyResponseDto(boolean verified, int attemptsRemaining, boolean locked) {
        this.verified = verified;
        this.attemptsRemaining = attemptsRemaining;
        this.locked = locked;
    }

    public boolean isVerified() {
        return verified;
    }

    public int getAttemptsRemaining() {
        return attemptsRemaining;
    }

    public boolean isLocked() {
        return locked;
    }
}