package app.web.inventory.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityTrendsDto {
    private Map<String, Long> dailyActivity;
    private Map<String, Long> operationBreakdown;
    private Long totalActivities;
    private String period;
}