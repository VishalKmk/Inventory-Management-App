package app.web.inventory.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private UUID id;
    private UUID spaceId;
    private String spaceName;
    private String name;
    private Double price;
    private Integer currentStock;
    private Integer minimumQuantity;
    private Integer maximumQuantity;
    private Boolean isLowStock;
    private Instant createdAt;
    private Instant updatedAt;
}