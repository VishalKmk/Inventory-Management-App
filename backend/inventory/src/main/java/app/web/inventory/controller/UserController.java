package app.web.inventory.controller;

import app.web.inventory.model.Users;
import app.web.inventory.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Get current logged-in user (from Authentication)
    @GetMapping("/me")
    public ResponseEntity<?> getUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        String email;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Map<?, ?> map) {
            email = (String) map.get("email");
        } else {
            email = authentication.getName();
        }

        Optional<Users> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
        }

        Users user = userOpt.get();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "verified", user.isVerified(),
                        "createdAt", user.getCreatedAt())));
    }

}
