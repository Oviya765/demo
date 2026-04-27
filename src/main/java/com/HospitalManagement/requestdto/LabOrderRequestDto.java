package com.HospitalManagement.requestdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record LabOrderRequestDto(
        Long encounterId,
        @NotNull Long patientId,
        @NotNull Long orderedById,
        @NotBlank String testsJson,
        String sampleId,
        LocalDateTime collectedAt,
        @NotBlank String status,
        String resultUri
) {
}
