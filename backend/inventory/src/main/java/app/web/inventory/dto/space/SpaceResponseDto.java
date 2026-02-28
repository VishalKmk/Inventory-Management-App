package app.web.inventory.dto.space;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SpaceResponseDto {
    private UUID id;
    private String name;
    private UUID ownerId;
    private String ownerName;
    private Long productCount;
    private Instant createdAt;
    private Instant updatedAt;
}