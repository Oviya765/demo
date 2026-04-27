package com.HospitalManagement.responsedto;

import java.time.LocalDateTime;
import java.util.List;

public record LabOrderResponseDto(
        Long labOrderId,
        Long encounterId,
        Long patientId,
        String patientName,
        Long orderedById,
        String orderedByName,
        String testsJson,
        String sampleId,
        LocalDateTime collectedAt,
        String status,
        String resultUri,
        List<LabResultSummaryDto> results
) {
}
