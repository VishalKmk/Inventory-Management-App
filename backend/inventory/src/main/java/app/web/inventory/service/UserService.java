package app.web.inventory.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
            throw new RuntimeException("User already exists with email: " + email);
        }

        Users user = new Users();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword)); // ✅ hash password
        user.setVerified(false);

        return userRepository.save(user);
    }

    public Optional<Users> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<Users> findById(UUID id) {
        return userRepository.findById(id);
    }

    public boolean checkPassword(Users user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public Users markVerified(Users user) {
        user.setVerified(true);
        return userRepository.save(user); // return updated user
    }
}
