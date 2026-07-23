package app.web.inventory.dto.space;

import java.util.UUID;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class SpaceDto {
    private UUID id;
    private String name;
    private UUID ownerId;
    private String ownerName;
    private long productCount;

    private String currentUserRole;

    public SpaceDto(UUID id, String name, UUID ownerId, String ownerName, long productCount, String currentUserRole) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.productCount = productCount;
        this.currentUserRole = currentUserRole;
    }

    public SpaceDto(UUID id, String name, UUID ownerId, String ownerName, long productCount) {
        this(id, name, ownerId, ownerName, productCount, "OWNER");
    }

    public SpaceDto(UUID id, String name, UUID ownerId, String ownerName) {
        this(id, name, ownerId, ownerName, 0L, "OWNER");
    }
}