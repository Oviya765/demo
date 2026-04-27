package com.HospitalManagement.requestdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record LabResultRequestDto(
        @NotNull Long labOrderId,
        @NotBlank String testCode,
        @NotBlank String value,
        String units,
        String referenceRangeJson,
        String flag,
        LocalDateTime reportedAt,
        @NotNull Long reportedById
) {
}
