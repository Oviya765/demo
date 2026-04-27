package com.HospitalManagement.responsedto;

import java.time.LocalDateTime;

public record LabResultSummaryDto(
        Long resultId,
        String testCode,
        String value,
        String units,
        String flag,
        LocalDateTime reportedAt
) {
}
