package app.web.inventory.controller;

import app.web.inventory.dto.api.ApiResponse;
import app.web.inventory.dto.user.UserResponseDto;
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
    private final app.web.inventory.service.SpaceService spaceService;

    public UserController(UserService userService, app.web.inventory.service.SpaceService spaceService) {
        this.userService = userService;
        this.spaceService = spaceService;
    }

    /**
     * Get current logged-in user
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Unauthorized"));
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
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("User not found"));
        }

        Users user = userOpt.get();
        UserResponseDto userDto = new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isVerified(),
                user.getCreatedAt());

        return ResponseEntity.ok(ApiResponse.success(userDto));
    }

    /**
     * Get pending invites for current user
     * GET /api/users/me/invites
     */
    @GetMapping("/me/invites")
    public ResponseEntity<ApiResponse<java.util.List<app.web.inventory.dto.space.SpaceInviteDto>>> getMyInvites() {
        java.util.UUID currentUserId = app.web.inventory.config.SecurityUtil.getCurrentUserId();
        java.util.List<app.web.inventory.dto.space.SpaceInviteDto> invites = spaceService
                .getPendingInvites(currentUserId);

        return ResponseEntity.ok(ApiResponse.success(invites));
    }
}