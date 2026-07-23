package app.web.inventory.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionsDto {
    private String[] entityTypes;
    private String[] operations;
    private String[] sortByOptions;
    private String[] sortDirections;
}