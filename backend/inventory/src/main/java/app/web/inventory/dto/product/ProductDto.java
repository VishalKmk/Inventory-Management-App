package app.web.inventory.dto.product;

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
    private String sku;
    private String category;
    private String imageUrl;
    private Double price;
    private Integer currentStock;
    private Integer minimumQuantity;
    private Integer maximumQuantity;
}
