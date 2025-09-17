package app.web.inventory.dto;

import java.util.UUID;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpaceDto {
    private UUID id;
    private String name;
    private UUID ownerId;
    private String ownerName;
    private long productCount;

    // Add this constructor to SpaceDto.java
    public SpaceDto(UUID id, String name, UUID ownerId, String ownerName) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.productCount = 0L;
    }
}