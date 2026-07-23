package app.web.inventory.dto.space;

import java.util.UUID;
import app.web.inventory.model.enums.SpaceRole;
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
