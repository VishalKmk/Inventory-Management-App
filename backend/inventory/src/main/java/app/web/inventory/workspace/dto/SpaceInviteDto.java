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
public class SpaceInviteDto {
    private UUID spaceId;
    private String spaceName;
    private String spaceOwnerName;
    private SpaceRole role;
    private Instant invitedAt;
    private UUID invitedBy; // ID of the user who sent the invite
}
