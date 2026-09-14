package com.example.patientservice.controller;

import com.example.patientservice.dto.request.PatientRequest;
import com.example.patientservice.dto.response.ApiResponse;
import com.example.patientservice.dto.response.PageResponse;
import com.example.patientservice.dto.response.PatientResponse;
import com.example.patientservice.filter.CorrelationIdFilter;
import com.example.patientservice.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller tiếp nhận và xử lý các yêu cầu HTTP liên quan đến tài nguyên Bệnh nhân (Patients).
 * Tuân thủ chuẩn RESTful API và cấu trúc Spring Web MVC.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    private String getCorrelationId() {
        return MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
    }

    /**
     * API thêm mới một bệnh nhân vào hệ thống.
     * 
     * @param request DTO chứa thông tin bệnh nhân đã qua validate đầu vào (@Valid).
     * @return ResponseEntity chứa ApiResponse với thông tin bệnh nhân vừa tạo và HTTP 201 Created.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(@Valid @RequestBody PatientRequest request) {
        log.info("[PATIENT-CONTROLLER] Nhận request POST /api/v1/patients: fullName='{}'", request.getFullName());

        PatientResponse responseData = patientService.createPatient(request);

        ApiResponse<PatientResponse> response = ApiResponse.success(
                responseData,
                "Thêm mới bệnh nhân thành công",
                getCorrelationId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * API lấy danh sách bệnh nhân có hỗ trợ phân trang và sắp xếp.
     * 
     * @param page    Số trang (mặc định: 0).
     * @param size    Số phần tử trên 1 trang (mặc định: 10).
     * @param sortBy  Trường sắp xếp (mặc định: id).
     * @param sortDir Hướng sắp xếp asc/desc (mặc định: desc).
     * @return Danh sách bệnh nhân phân trang bọc trong ApiResponse.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PatientResponse>>> getAllPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        log.info("[PATIENT-CONTROLLER] Nhận request GET /api/v1/patients: page={}, size={}, sortBy={}, sortDir={}",
                page, size, sortBy, sortDir);

        PageResponse<PatientResponse> responseData = patientService.getAllPatients(page, size, sortBy, sortDir);

        ApiResponse<PageResponse<PatientResponse>> response = ApiResponse.success(
                responseData,
                "Lấy danh sách bệnh nhân thành công",
                getCorrelationId()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * API lấy thông tin chi tiết một bệnh nhân theo ID.
     * 
     * @param id ID của bệnh nhân cần tìm kiếm.
     * @return Chi tiết bệnh nhân bọc trong ApiResponse.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(@PathVariable Long id) {
        log.info("[PATIENT-CONTROLLER] Nhận request GET /api/v1/patients/{}", id);

        PatientResponse responseData = patientService.getPatientById(id);

        ApiResponse<PatientResponse> response = ApiResponse.success(
                responseData,
                "Lấy thông tin bệnh nhân thành công",
                getCorrelationId()
        );

        return ResponseEntity.ok(response);
    }
}
