package app.web.inventory.workspace.dto;

import java.util.UUID;
import app.web.inventory.workspace.model.enums.SpaceRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteMemberRequest {
    private String email; // Optional: Invite by email
    private UUID userId; // Optional: Invite by ID
    private SpaceRole role = SpaceRole.MEMBER; // Default role
}
