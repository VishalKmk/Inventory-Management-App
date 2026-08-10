package app.web.inventory.audit.dto;

import app.web.inventory.shared.dto.PaginationDto;
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