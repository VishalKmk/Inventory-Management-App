package app.web.inventory.user.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import app.web.inventory.user.dto.UserDto;
import app.web.inventory.auth.exception.UnverifiedRegistrationExistsException;
import app.web.inventory.shared.exception.DuplicateResourceException;
import app.web.inventory.user.model.Users;
import app.web.inventory.user.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Users createLocalUser(String name, String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);

        Optional<Users> existing = userRepository.findByEmail(normalizedEmail);
        existing.ifPresent(this::throwForExisting);

        Users user = new Users();
        user.setName(name);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setAuthProvider("local");
        user.setVerified(false);

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException raceLoss) {
            userRepository.findByEmail(normalizedEmail).ifPresent(this::throwForExisting);
            throw new UnverifiedRegistrationExistsException(
                    "A user has already registered with this email. "
                            + "If not verified then request an OTP or contact support.");
        }
    }

    private void throwForExisting(Users existingUser) {
        if (existingUser.isVerified()) {
            throw new DuplicateResourceException(
                    "An account already exists with this email. Please log in.");
        }
        throw new UnverifiedRegistrationExistsException(
                "A user has already registered with this email. "
                        + "If not verified then request an OTP or contact support.");
    }

    /** Backwards-compatible registration helper for internal callers. */
    public Users register(String name, String email, String rawPassword) {
        return createLocalUser(name, email, rawPassword);
    }

    public Users findOrCreateGoogleUser(String email, String name) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> {
                    Users user = new Users();
                    user.setName(name);
                    user.setEmail(normalizedEmail);
                    user.setPasswordHash(null);
                    user.setAuthProvider("google");
                    user.setVerified(true);
                    return userRepository.save(user);
                });
    }

    public Optional<Users> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    public Optional<Users> findById(UUID id) {
        return userRepository.findById(id);
    }

    public boolean checkPassword(Users user, String rawPassword) {
        if (user.getPasswordHash() == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public void markVerified(Users user) {
        user.setVerified(true);
        userRepository.save(user);
    }

    public UserDto convertToDto(Users user) {
        return new UserDto(
                user.getId(), user.getEmail(), user.getName(), user.isVerified(), user.getCreatedAt());
    }

    public Optional<UserDto> getUserDtoByEmail(String email) {
        return findByEmail(email).map(this::convertToDto);
    }

    public Optional<UserDto> getUserDtoById(UUID id) {
        return findById(id).map(this::convertToDto);
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}