package app.web.inventory.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import app.web.inventory.config.SecurityUtil;
import app.web.inventory.model.Spaces;
import app.web.inventory.service.SpaceService;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    /**
     * Create a new space
     */
    @PostMapping
    public ResponseEntity<?> createSpace(@RequestBody Map<String, String> body) {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            String name = body.get("name");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Space name is required"));
            }

            Spaces space = spaceService.createSpace(currentUserId, name);

            return ResponseEntity.status(201).body(Map.of(
                    "success", true,
                    "message", "Space created successfully",
                    "data", Map.of(
                            "id", space.getId(),
                            "name", space.getName(),
                            "ownerId", space.getOwner().getId(),
                            "createdAt", space.getCreatedAt(),
                            "remainingSlots", spaceService.getRemainingSpaceSlots(currentUserId))));

        } catch (IllegalStateException ex) {
            // Space limit reached
            return ResponseEntity.status(400)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Get all spaces for current user
     */
    @GetMapping
    public ResponseEntity<?> getSpaces() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            var spaces = spaceService.getSpacesWithProductCount(currentUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", spaces));

        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Get a specific space by ID
     */
    @GetMapping("/{spaceId}")
    public ResponseEntity<?> getSpaceById(@PathVariable UUID spaceId) {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            var spaceOpt = spaceService.getSpaceByIdAndOwner(spaceId, currentUserId);

            if (spaceOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Space not found"));
            }

            Spaces space = spaceOpt.get();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "id", space.getId(),
                            "name", space.getName(),
                            "ownerId", space.getOwner().getId(),
                            "ownerName", space.getOwner().getName(),
                            "createdAt", space.getCreatedAt(),
                            "updatedAt", space.getUpdatedAt())));

        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Get space creation status
     */
    @GetMapping("/creation-status")
    public ResponseEntity<?> getCreationStatus() {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            long currentCount = spaceService.getSpacesByOwner(currentUserId).size();
            int remaining = spaceService.getRemainingSpaceSlots(currentUserId);
            boolean canCreate = spaceService.canCreateMoreSpaces(currentUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "currentSpaces", currentCount,
                            "maxSpaces", 10,
                            "remainingSlots", remaining,
                            "canCreateMore", canCreate)));

        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Update space name
     */
    @PutMapping("/{spaceId}")
    public ResponseEntity<?> updateSpaceById(@PathVariable UUID spaceId, @RequestBody Map<String, String> body) {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            String newName = body.get("name");

            if (newName == null || newName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Space name is required"));
            }

            Spaces updatedSpace = spaceService.updateSpace(spaceId, currentUserId, newName);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Space updated successfully",
                    "data", Map.of(
                            "id", updatedSpace.getId(),
                            "name", updatedSpace.getName(),
                            "ownerId", updatedSpace.getOwner().getId(),
                            "updatedAt", updatedSpace.getUpdatedAt())));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Delete a space
     */
    @DeleteMapping("/{spaceId}")
    public ResponseEntity<?> deleteSpace(@PathVariable UUID spaceId) {
        try {
            UUID currentUserId = SecurityUtil.getCurrentUserId();
            spaceService.deleteSpace(spaceId, currentUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Space deleted successfully"));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }
}