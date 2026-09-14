package com.example.doctorservice.controller;

import com.example.doctorservice.dto.request.DoctorRequest;
import com.example.doctorservice.dto.response.ApiResponse;
import com.example.doctorservice.dto.response.DoctorResponse;
import com.example.doctorservice.dto.response.PageResponse;
import com.example.doctorservice.filter.CorrelationIdFilter;
import com.example.doctorservice.service.DoctorService;
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
 * REST Controller tiếp nhận và xử lý các yêu cầu HTTP liên quan đến tài nguyên Bác sĩ (Doctors).
 * Tuân thủ cấu trúc Spring Web MVC và chuẩn RESTful API.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    private String getCorrelationId() {
        return MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
    }

    /**
     * API GET /api/v1/doctors: Lấy danh sách bác sĩ có hỗ trợ phân trang, sắp xếp và lọc.
     * Mặc định trả về danh sách bác sĩ với các trường tóm tắt: id, name, specialization.
     * Có thể truyền detail=true để lấy thêm các trường chi tiết.
     *
     * @param page           Số trang (mặc định: 0).
     * @param size           Số phần tử trên 1 trang (mặc định: 10).
     * @param sortBy         Trường sắp xếp (mặc định: id).
     * @param sortDir        Hướng sắp xếp asc/desc (mặc định: asc).
     * @param specialization Lọc theo chuyên khoa (tùy chọn).
     * @param status         Lọc theo trạng thái làm việc (tùy chọn).
     * @param detail         Cờ hiển thị chi tiết (mặc định: false - chỉ trả về id, name, specialization).
     * @return Danh sách bác sĩ phân trang được bọc trong ApiResponse chuẩn.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DoctorResponse>>> getDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) Boolean status,
            @RequestParam(defaultValue = "false") boolean detail
    ) {
        log.info("[DOCTOR-CONTROLLER] Nhận request GET /api/v1/doctors: page={}, size={}, sortBy={}, sortDir={}, specialization='{}', status={}, detail={}",
                page, size, sortBy, sortDir, specialization, status, detail);

        PageResponse<DoctorResponse> responseData = doctorService.getDoctors(
                page, size, sortBy, sortDir, specialization, status, detail
        );

        ApiResponse<PageResponse<DoctorResponse>> response = ApiResponse.success(
                responseData,
                "Lấy danh sách bác sĩ thành công",
                getCorrelationId()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * API GET /api/v1/doctors/{id}: Lấy thông tin chi tiết của một bác sĩ theo ID.
     *
     * @param id Khóa chính định danh bác sĩ.
     * @return Thông tin chi tiết bác sĩ bọc trong ApiResponse.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(@PathVariable Long id) {
        log.info("[DOCTOR-CONTROLLER] Nhận request GET /api/v1/doctors/{}", id);

        DoctorResponse responseData = doctorService.getDoctorById(id);

        ApiResponse<DoctorResponse> response = ApiResponse.success(
                responseData,
                "Lấy thông tin bác sĩ thành công",
                getCorrelationId()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * API POST /api/v1/doctors: Thêm mới một bác sĩ vào hệ thống.
     *
     * @param request DTO chứa thông tin bác sĩ có kiểm tra hợp lệ (@Valid).
     * @return Thông tin bác sĩ vừa tạo với mã HTTP 201 Created.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<DoctorResponse>> createDoctor(@Valid @RequestBody DoctorRequest request) {
        log.info("[DOCTOR-CONTROLLER] Nhận request POST /api/v1/doctors: name='{}', email='{}'",
                request.getName(), request.getEmail());

        DoctorResponse responseData = doctorService.createDoctor(request);

        ApiResponse<DoctorResponse> response = ApiResponse.success(
                responseData,
                "Thêm mới bác sĩ thành công",
                getCorrelationId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
