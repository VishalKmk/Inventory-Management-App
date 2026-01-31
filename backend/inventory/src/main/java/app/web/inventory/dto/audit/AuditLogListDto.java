package app.web.inventory.dto.audit;

import app.web.inventory.dto.pagination.PaginationDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogListDto {
    private List<AuditLogDto> data;
    private PaginationDto pagination;
}