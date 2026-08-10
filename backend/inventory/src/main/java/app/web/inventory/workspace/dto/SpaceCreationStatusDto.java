package app.web.inventory.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceCreationStatusDto {
    private long currentSpaces;
    private int maxSpaces;
    private int remainingSlots;
    private boolean canCreateMore;
}