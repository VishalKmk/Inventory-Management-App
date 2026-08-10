package app.web.inventory.product.dto;

import java.util.List;

import app.web.inventory.shared.dto.PaginationDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductListDto {
    private List<ProductDto> data;
    private PaginationDto pagination;
}
