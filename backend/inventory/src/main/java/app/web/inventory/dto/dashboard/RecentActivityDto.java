package app.web.inventory.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDto {
    private List<ActivityItem> activities;
    private Integer totalCount;
    private Boolean hasActivity;
    private String message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private UUID id;
        private String type;
        private String entityType;
        private UUID entityId;
        private LocalDateTime timestamp;
        private String ipAddress;
        private Map<String, Object> details;
        private String description;
    }
}