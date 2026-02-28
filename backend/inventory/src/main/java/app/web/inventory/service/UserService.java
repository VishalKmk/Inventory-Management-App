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
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("User already exists with email: " + email);
        }

        Users user = new Users();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setVerified(false);

        return userRepository.save(user);
    }

    public Optional<Users> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @SuppressWarnings("null")
    public Optional<Users> findById(UUID id) {
        return userRepository.findById(id);
    }

    public boolean checkPassword(Users user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public Users markVerified(Users user) {
        user.setVerified(true);
        return userRepository.save(user);
    }

    /**
     * Convert Users entity to UserDto
     */
    public UserDto convertToDto(Users user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isVerified(),
                user.getCreatedAt());
    }

    /**
     * Get user as DTO by email
     */
    public Optional<UserDto> getUserDtoByEmail(String email) {
        return findByEmail(email).map(this::convertToDto);
    }

    /**
     * Get user as DTO by ID
     */
    public Optional<UserDto> getUserDtoById(UUID id) {
        return findById(id).map(this::convertToDto);
    }
}