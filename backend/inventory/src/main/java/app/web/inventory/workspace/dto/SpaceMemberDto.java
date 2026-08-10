package app.web.inventory.workspace.dto;

import java.time.Instant;
import java.util.UUID;
import app.web.inventory.workspace.model.enums.SpaceRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceMemberDto {
    private UUID id; // Membership ID
    private UUID userId; // User ID
    private String userName; // User Name
    private String email; // User Email
    private SpaceRole role;
    private Instant joinedAt;
}
