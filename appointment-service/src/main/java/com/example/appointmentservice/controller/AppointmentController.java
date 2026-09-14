package com.example.appointmentservice.controller;

import com.example.appointmentservice.dto.request.AppointmentRequest;
import com.example.appointmentservice.dto.response.ApiResponse;
import com.example.appointmentservice.dto.response.AppointmentResponse;
import com.example.appointmentservice.dto.response.PageResponse;
import com.example.appointmentservice.filter.CorrelationIdFilter;
import com.example.appointmentservice.service.AppointmentService;
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
 * Controller xử lý các yêu cầu liên quan đến đặt và quản lý lịch khám bệnh.
 * Hỗ trợ cả 2 định dạng đường dẫn URL: /api/v1/appointments và /api/v1/appointment.
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1/appointments", "/api/v1/appointment"})
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    private String getCorrelationId() {
        return MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
    }

    /**
     * API tạo mới lịch hẹn khám bệnh.
     * Xác thực sự tồn tại của patientId và doctorId thông qua RestTemplate (@LoadBalanced).
     *
     * @param request DTO dữ liệu lịch hẹn gửi từ client đã qua validation (@Valid).
     * @return Thông tin lịch hẹn vừa tạo và HTTP 201 Created.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody AppointmentRequest request) {

        log.info("[APPOINTMENT-CONTROLLER] Nhận request POST tạo lịch hẹn: patientId={}, doctorId={}, date={}",
                request.getPatientId(), request.getDoctorId(), request.getAppointmentDate());

        AppointmentResponse responseData = appointmentService.createAppointment(request);

        ApiResponse<AppointmentResponse> response = ApiResponse.success(
                responseData,
                "Tạo lịch hẹn khám bệnh thành công",
                getCorrelationId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * API lấy danh sách lịch hẹn khám có hỗ trợ phân trang, sắp xếp và lọc.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> getAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String status
    ) {
        log.info("[APPOINTMENT-CONTROLLER] Nhận request GET danh sách lịch hẹn: page={}, size={}, sortBy={}, sortDir={}",
                page, size, sortBy, sortDir);

        PageResponse<AppointmentResponse> responseData = appointmentService.getAppointments(
                page, size, sortBy, sortDir, patientId, doctorId, status
        );

        ApiResponse<PageResponse<AppointmentResponse>> response = ApiResponse.success(
                responseData,
                "Lấy danh sách lịch hẹn thành công",
                getCorrelationId()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * API lấy chi tiết một lịch hẹn theo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointmentById(@PathVariable Long id) {
        log.info("[APPOINTMENT-CONTROLLER] Nhận request GET chi tiết lịch hẹn: id={}", id);

        AppointmentResponse responseData = appointmentService.getAppointmentById(id);

        ApiResponse<AppointmentResponse> response = ApiResponse.success(
                responseData,
                "Lấy thông tin lịch hẹn thành công",
                getCorrelationId()
        );

        return ResponseEntity.ok(response);
    }
}
