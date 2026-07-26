package app.web.inventory.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import app.web.inventory.dto.user.UserDto;
import app.web.inventory.exception.DuplicateResourceException;
import app.web.inventory.model.Users;
import app.web.inventory.repository.UserRepository;

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

    public Users register(String name, String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateResourceException("User already exists with email: " + normalizedEmail);
        }

        Users user = new Users();
        user.setName(name);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setAuthProvider("local");
        user.setVerified(false);

        return userRepository.save(user);
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

    @SuppressWarnings("null")
    public Optional<Users> findById(UUID id) {
        return userRepository.findById(id);
    }

    public boolean checkPassword(Users user, String rawPassword) {
        if (user.getPasswordHash() == null) {
            return false; // Google-only account has no local password to check
        }
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public Users markVerified(Users user) {
        user.setVerified(true);
        return userRepository.save(user);
    }

    // Convert Users entity to UserDto
    public UserDto convertToDto(Users user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isVerified(),
                user.getCreatedAt());
    }

    // Get user as DTO by email
    public Optional<UserDto> getUserDtoByEmail(String email) {
        return findByEmail(email).map(this::convertToDto);
    }

    // Get user as DTO by ID
    public Optional<UserDto> getUserDtoById(UUID id) {
        return findById(id).map(this::convertToDto);
    }

    // Normalize email by trimming and converting to lowercase
    public static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}