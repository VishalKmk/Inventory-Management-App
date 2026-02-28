package app.web.inventory.dto.product;

import java.util.List;

import app.web.inventory.dto.pagination.PaginationDto;
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
