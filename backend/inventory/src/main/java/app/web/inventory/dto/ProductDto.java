package app.web.inventory.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private UUID id;
    private UUID spaceId;
    private String name;
    private Double price;
    private Integer currentStock;
    private Integer minimumQuantity;
    private Integer maximumQuantity;
}