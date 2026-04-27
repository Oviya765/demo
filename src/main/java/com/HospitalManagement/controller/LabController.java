package com.HospitalManagement.controller;

import com.HospitalManagement.requestdto.LabOrderRequestDto;
import com.HospitalManagement.requestdto.LabResultRequestDto;
import com.HospitalManagement.responsedto.LabOrderResponseDto;
import com.HospitalManagement.responsedto.LabResultResponseDto;
import com.HospitalManagement.service.LabService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lab")
public class LabController {

    private final LabService labService;

    public LabController(LabService labService) {
        this.labService = labService;
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyAuthority('LAB_TECHNICIAN', 'CLINICIAN', 'ADMIN')")
    public List<LabOrderResponseDto> getAllOrders() {
        return labService.getAllOrders();
    }

    @GetMapping("/orders/{labOrderId}")
    @PreAuthorize("hasAnyAuthority('LAB_TECHNICIAN', 'CLINICIAN', 'ADMIN')")
    public LabOrderResponseDto getOrderById(@PathVariable Long labOrderId) {
        return labService.getOrderById(labOrderId);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CLINICIAN', 'LAB_TECHNICIAN', 'ADMIN')")
    public LabOrderResponseDto createOrder(@Valid @RequestBody LabOrderRequestDto requestDto) {
        return labService.createOrder(requestDto);
    }

    @PutMapping("/orders/{labOrderId}")
    @PreAuthorize("hasAnyAuthority('CLINICIAN', 'LAB_TECHNICIAN', 'ADMIN')")
    public LabOrderResponseDto updateOrder(
            @PathVariable Long labOrderId,
            @Valid @RequestBody LabOrderRequestDto requestDto
    ) {
        return labService.updateOrder(labOrderId, requestDto);
    }

    @DeleteMapping("/orders/{labOrderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('LAB_TECHNICIAN', 'ADMIN')")
    public void deleteOrder(@PathVariable Long labOrderId) {
        labService.deleteOrder(labOrderId);
    }

    @GetMapping("/results")
    @PreAuthorize("hasAnyAuthority('LAB_TECHNICIAN', 'CLINICIAN', 'ADMIN')")
    public List<LabResultResponseDto> getAllResults() {
        return labService.getAllResults();
    }

    @GetMapping("/results/{resultId}")
    @PreAuthorize("hasAnyAuthority('LAB_TECHNICIAN', 'CLINICIAN', 'ADMIN')")
    public LabResultResponseDto getResultById(@PathVariable Long resultId) {
        return labService.getResultById(resultId);
    }

    @PostMapping("/results")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('LAB_TECHNICIAN', 'ADMIN')")
    public LabResultResponseDto createResult(@Valid @RequestBody LabResultRequestDto requestDto) {
        return labService.createResult(requestDto);
    }

    @PutMapping("/results/{resultId}")
    @PreAuthorize("hasAnyAuthority('LAB_TECHNICIAN', 'ADMIN')")
    public LabResultResponseDto updateResult(
            @PathVariable Long resultId,
            @Valid @RequestBody LabResultRequestDto requestDto
    ) {
        return labService.updateResult(resultId, requestDto);
    }

    @DeleteMapping("/results/{resultId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('LAB_TECHNICIAN', 'ADMIN')")
    public void deleteResult(@PathVariable Long resultId) {
        labService.deleteResult(resultId);
    }
}
