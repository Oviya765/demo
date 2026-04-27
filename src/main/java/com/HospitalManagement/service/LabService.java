package com.HospitalManagement.service;

import com.HospitalManagement.entity.Encounter;
import com.HospitalManagement.entity.LabOrder;
import com.HospitalManagement.entity.LabResult;
import com.HospitalManagement.entity.Patient;
import com.HospitalManagement.entity.User;
import com.HospitalManagement.repository.EncounterRepository;
import com.HospitalManagement.repository.LabOrderRepository;
import com.HospitalManagement.repository.LabResultRepository;
import com.HospitalManagement.repository.PatientRepository;
import com.HospitalManagement.repository.UserRepository;
import com.HospitalManagement.requestdto.LabOrderRequestDto;
import com.HospitalManagement.requestdto.LabResultRequestDto;
import com.HospitalManagement.responsedto.LabOrderResponseDto;
import com.HospitalManagement.responsedto.LabResultResponseDto;
import com.HospitalManagement.responsedto.LabResultSummaryDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class LabService {

    private static final String STATUS_ORDERED = "ORDERED";
    private static final String STATUS_COLLECTED = "COLLECTED";
    private static final String STATUS_RESULTS_REPORTED = "RESULTS_REPORTED";
    private static final String STATUS_CRITICAL_REPORTED = "CRITICAL_REPORTED";

    private final LabOrderRepository labOrderRepository;
    private final LabResultRepository labResultRepository;
    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public LabService(
            LabOrderRepository labOrderRepository,
            LabResultRepository labResultRepository,
            EncounterRepository encounterRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.labOrderRepository = labOrderRepository;
        this.labResultRepository = labResultRepository;
        this.encounterRepository = encounterRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<LabOrderResponseDto> getAllOrders() {
        return labOrderRepository.findAll()
                .stream()
                .map(this::toOrderResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public LabOrderResponseDto getOrderById(Long labOrderId) {
        return toOrderResponseDto(findLabOrder(labOrderId));
    }

    public LabOrderResponseDto createOrder(LabOrderRequestDto requestDto) {
        LabOrder labOrder = new LabOrder();
        mapOrderRequestToEntity(requestDto, labOrder);
        LabOrder savedOrder = labOrderRepository.save(labOrder);
        if (savedOrder.getResultUri() == null || savedOrder.getResultUri().isBlank()) {
            savedOrder.setResultUri(buildResultUri(savedOrder.getLabOrderId()));
            savedOrder = labOrderRepository.save(savedOrder);
        }
        return toOrderResponseDto(savedOrder);
    }

    public LabOrderResponseDto updateOrder(Long labOrderId, LabOrderRequestDto requestDto) {
        LabOrder labOrder = findLabOrder(labOrderId);
        mapOrderRequestToEntity(requestDto, labOrder);
        return toOrderResponseDto(labOrderRepository.save(labOrder));
    }

    public void deleteOrder(Long labOrderId) {
        LabOrder labOrder = findLabOrder(labOrderId);
        labResultRepository.findAllByLabOrderLabOrderId(labOrderId)
                .forEach(labResultRepository::delete);
        labOrderRepository.delete(labOrder);
    }

    @Transactional(readOnly = true)
    public List<LabResultResponseDto> getAllResults() {
        return labResultRepository.findAll()
                .stream()
                .map(this::toResultResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public LabResultResponseDto getResultById(Long resultId) {
        return toResultResponseDto(findLabResult(resultId));
    }

    public LabResultResponseDto createResult(LabResultRequestDto requestDto) {
        LabResult labResult = new LabResult();
        mapResultRequestToEntity(requestDto, labResult);
        LabResult savedResult = labResultRepository.save(labResult);
        updateLabOrderStatus(savedResult.getLabOrder());
        return toResultResponseDto(savedResult);
    }

    public LabResultResponseDto updateResult(Long resultId, LabResultRequestDto requestDto) {
        LabResult labResult = findLabResult(resultId);
        mapResultRequestToEntity(requestDto, labResult);
        LabResult savedResult = labResultRepository.save(labResult);
        updateLabOrderStatus(savedResult.getLabOrder());
        return toResultResponseDto(savedResult);
    }

    public void deleteResult(Long resultId) {
        LabResult labResult = findLabResult(resultId);
        LabOrder order = labResult.getLabOrder();
        labResultRepository.delete(labResult);
        updateLabOrderStatus(order);
    }

    private void mapOrderRequestToEntity(LabOrderRequestDto requestDto, LabOrder labOrder) {
        labOrder.setEncounter(requestDto.encounterId() != null ? findEncounter(requestDto.encounterId()) : null);
        labOrder.setPatient(findPatient(requestDto.patientId()));
        labOrder.setOrderedBy(findUser(requestDto.orderedById()));
        labOrder.setTestsJson(validateJson(requestDto.testsJson(), "testsJson"));
        labOrder.setSampleId(requestDto.sampleId() == null || requestDto.sampleId().isBlank()
                ? generateSampleId()
                : requestDto.sampleId());
        labOrder.setCollectedAt(requestDto.collectedAt());
        labOrder.setStatus(resolveOrderStatus(requestDto.status(), requestDto.collectedAt()));
        labOrder.setResultUri((requestDto.resultUri() == null || requestDto.resultUri().isBlank())
                ? buildResultUri(labOrder.getLabOrderId())
                : requestDto.resultUri());
    }

    private void mapResultRequestToEntity(LabResultRequestDto requestDto, LabResult labResult) {
        LabOrder labOrder = findLabOrder(requestDto.labOrderId());
        String normalizedReferenceRange = normalizeOptionalJson(requestDto.referenceRangeJson(), "referenceRangeJson");
        String computedFlag = determineFlag(requestDto.value(), normalizedReferenceRange, requestDto.flag());

        labResult.setLabOrder(labOrder);
        labResult.setTestCode(requestDto.testCode());
        labResult.setValue(requestDto.value());
        labResult.setUnits(requestDto.units());
        labResult.setReferenceRangeJson(normalizedReferenceRange);
        labResult.setFlag(computedFlag);
        labResult.setReportedAt(requestDto.reportedAt() != null ? requestDto.reportedAt() : LocalDateTime.now());
        labResult.setReportedBy(findUser(requestDto.reportedById()));

        if (labOrder.getResultUri() == null || labOrder.getResultUri().isBlank()) {
            labOrder.setResultUri(buildResultUri(labOrder.getLabOrderId()));
        }
    }

    private String resolveOrderStatus(String requestedStatus, LocalDateTime collectedAt) {
        if (requestedStatus == null || requestedStatus.isBlank()) {
            return collectedAt != null ? STATUS_COLLECTED : STATUS_ORDERED;
        }
        return requestedStatus;
    }

    private void updateLabOrderStatus(LabOrder labOrder) {
        List<LabResult> results = labResultRepository.findAllByLabOrderLabOrderId(labOrder.getLabOrderId());
        if (results.isEmpty()) {
            if (labOrder.getCollectedAt() != null) {
                labOrder.setStatus(STATUS_COLLECTED);
            } else if (labOrder.getStatus() == null || labOrder.getStatus().isBlank()) {
                labOrder.setStatus(STATUS_ORDERED);
            }
            labOrderRepository.save(labOrder);
            return;
        }

        boolean hasCritical = results.stream()
                .anyMatch(result -> STATUS_CRITICAL_REPORTED.equalsIgnoreCase(result.getFlag())
                        || "CRITICAL".equalsIgnoreCase(result.getFlag()));

        labOrder.setStatus(hasCritical ? STATUS_CRITICAL_REPORTED : STATUS_RESULTS_REPORTED);
        if (labOrder.getResultUri() == null || labOrder.getResultUri().isBlank()) {
            labOrder.setResultUri(buildResultUri(labOrder.getLabOrderId()));
        }
        labOrderRepository.save(labOrder);
    }

    private String determineFlag(String value, String referenceRangeJson, String requestedFlag) {
        if (referenceRangeJson == null || referenceRangeJson.isBlank()) {
            return fallbackFlag(requestedFlag);
        }

        try {
            Map<String, Object> range = objectMapper.readValue(referenceRangeJson, new TypeReference<>() { });
            Double numericValue = parseDouble(value);
            if (numericValue == null) {
                return fallbackFlag(requestedFlag);
            }

            Double criticalMin = parseDouble(range.get("criticalMin"));
            Double criticalMax = parseDouble(range.get("criticalMax"));
            Double min = parseDouble(range.get("min"));
            Double max = parseDouble(range.get("max"));

            if (criticalMin != null && numericValue < criticalMin) {
                return "CRITICAL";
            }
            if (criticalMax != null && numericValue > criticalMax) {
                return "CRITICAL";
            }
            if (min != null && numericValue < min) {
                return "LOW";
            }
            if (max != null && numericValue > max) {
                return "HIGH";
            }
            return "NORMAL";
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid referenceRangeJson");
        }
    }

    private String fallbackFlag(String requestedFlag) {
        return (requestedFlag == null || requestedFlag.isBlank()) ? "NORMAL" : requestedFlag.toUpperCase();
    }

    private String validateJson(String json, String fieldName) {
        try {
            objectMapper.readTree(json);
            return json;
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + fieldName);
        }
    }

    private String normalizeOptionalJson(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return validateJson(json, fieldName);
    }

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String generateSampleId() {
        return "SMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String buildResultUri(Long labOrderId) {
        if (labOrderId == null) {
            return null;
        }
        return "/api/v1/lab/orders/" + labOrderId + "/results";
    }

    private LabOrder findLabOrder(Long labOrderId) {
        return labOrderRepository.findById(labOrderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Lab order not found with id: " + labOrderId));
    }

    private LabResult findLabResult(Long resultId) {
        return labResultRepository.findById(resultId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Lab result not found with id: " + resultId));
    }

    private Encounter findEncounter(Long encounterId) {
        return encounterRepository.findById(encounterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Encounter not found with id: " + encounterId));
    }

    private Patient findPatient(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Patient not found with id: " + patientId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + userId));
    }

    private LabOrderResponseDto toOrderResponseDto(LabOrder labOrder) {
        User orderedBy = labOrder.getOrderedBy();
        List<LabResultSummaryDto> results = labResultRepository.findAllByLabOrderLabOrderId(labOrder.getLabOrderId())
                .stream()
                .map(result -> new LabResultSummaryDto(
                        result.getResultId(),
                        result.getTestCode(),
                        result.getValue(),
                        result.getUnits(),
                        result.getFlag(),
                        result.getReportedAt()
                ))
                .toList();

        return new LabOrderResponseDto(
                labOrder.getLabOrderId(),
                labOrder.getEncounter() != null ? labOrder.getEncounter().getEncounterId() : null,
                labOrder.getPatient().getPatientId(),
                labOrder.getPatient().getName(),
                orderedBy != null ? orderedBy.getUserId() : null,
                orderedBy != null ? orderedBy.getName() : null,
                labOrder.getTestsJson(),
                labOrder.getSampleId(),
                labOrder.getCollectedAt(),
                labOrder.getStatus(),
                labOrder.getResultUri(),
                results
        );
    }

    private LabResultResponseDto toResultResponseDto(LabResult labResult) {
        User reportedBy = labResult.getReportedBy();
        return new LabResultResponseDto(
                labResult.getResultId(),
                labResult.getLabOrder().getLabOrderId(),
                labResult.getTestCode(),
                labResult.getValue(),
                labResult.getUnits(),
                labResult.getReferenceRangeJson(),
                labResult.getFlag(),
                labResult.getReportedAt(),
                reportedBy != null ? reportedBy.getUserId() : null,
                reportedBy != null ? reportedBy.getName() : null
        );
    }
}
