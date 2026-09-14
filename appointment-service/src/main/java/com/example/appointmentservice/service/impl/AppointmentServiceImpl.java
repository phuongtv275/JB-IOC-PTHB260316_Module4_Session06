package com.example.appointmentservice.service.impl;

import com.example.appointmentservice.client.MedicalServiceClient;
import com.example.appointmentservice.dto.request.AppointmentRequest;
import com.example.appointmentservice.dto.response.AppointmentResponse;
import com.example.appointmentservice.dto.response.PageResponse;
import com.example.appointmentservice.entity.Appointment;
import com.example.appointmentservice.exception.BadRequestException;
import com.example.appointmentservice.exception.ResourceNotFoundException;
import com.example.appointmentservice.mapper.AppointmentMapper;
import com.example.appointmentservice.repository.AppointmentRepository;
import com.example.appointmentservice.service.AppointmentService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lớp triển khai nghiệp vụ cho AppointmentService.
 * Tuân thủ Single Responsibility Principle (SRP) và Open-Closed Principle (OCP).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final MedicalServiceClient medicalServiceClient;

    /**
     * Danh sách các trường cho phép sắp xếp nhằm đảm bảo an toàn truy vấn.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "patientId", "doctorId", "appointmentDate", "status", "createdAt"
    );

    /**
     * Tạo mới lịch khám bệnh.
     * 
     * Logic nghiệp vụ:
     * 1. Gọi RestTemplate (@LoadBalanced) tới 'patient-service' qua Eureka để kiểm tra sự tồn tại của patientId.
     * 2. Gọi RestTemplate (@LoadBalanced) tới 'doctor-service' qua Eureka để kiểm tra sự tồn tại của doctorId.
     * 3. Nếu một trong hai bên không tồn tại, ném ngoại lệ ResourceNotFoundException (trả về HTTP 404).
     * 4. Chuyển đổi DTO sang Entity qua MapStruct.
     * 5. Đặt trạng thái mặc định là "PENDING" nếu client chưa chỉ định.
     * 6. Lưu vào cơ sở dữ liệu và chuyển đổi sang DTO phản hồi.
     * 7. Ghi log đầy đủ thông tin để phục vụ tracing và debug.
     */
    @Override
    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        log.info("[APPOINTMENT-SERVICE] Bắt đầu xử lý tạo lịch khám: patientId={}, doctorId={}, date={}",
                request.getPatientId(), request.getDoctorId(), request.getAppointmentDate());

        // 1. Kiểm tra sự tồn tại của Bệnh nhân trên Patient-Service
        medicalServiceClient.validatePatientExists(request.getPatientId());

        // 2. Kiểm tra sự tồn tại của Bác sĩ trên Doctor-Service
        medicalServiceClient.validateDoctorExists(request.getDoctorId());

        // 3. Ánh xạ DTO sang Entity
        Appointment appointment = appointmentMapper.toEntity(request);

        // Thiết lập trạng thái mặc định nếu không truyền
        if (!StringUtils.hasText(appointment.getStatus())) {
            appointment.setStatus("PENDING");
        } else {
            appointment.setStatus(appointment.getStatus().trim().toUpperCase());
        }

        // 4. Lưu vào CSDL
        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("[APPOINTMENT-SERVICE] Tạo lịch khám thành công! ID cấp phát: {}, Trạng thái: {}",
                savedAppointment.getId(), savedAppointment.getStatus());

        // 5. Trả về DTO
        return appointmentMapper.toResponse(savedAppointment);
    }

    /**
     * Lấy danh sách lịch khám có phân trang, sắp xếp và lọc động.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> getAppointments(int page, int size, String sortBy, String sortDir,
                                                             Long patientId, Long doctorId, String status) {

        log.info("[APPOINTMENT-SERVICE] Lấy danh sách lịch hẹn: page={}, size={}, sortBy={}, sortDir={}, patientId={}, doctorId={}, status={}",
                page, size, sortBy, sortDir, patientId, doctorId, status);

        if (page < 0) {
            throw new BadRequestException("Chỉ số trang (page) không được nhỏ hơn 0");
        }
        if (size <= 0 || size > 100) {
            throw new BadRequestException("Kích thước trang (size) phải từ 1 đến 100");
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException(
                    String.format("Trường sắp xếp '%s' không hợp lệ. Các trường hợp lệ: %s", sortBy, ALLOWED_SORT_FIELDS)
            );
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Xây dựng JPA Specification lọc động
        Specification<Appointment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (patientId != null) {
                predicates.add(cb.equal(root.get("patientId"), patientId));
            }
            if (doctorId != null) {
                predicates.add(cb.equal(root.get("doctorId"), doctorId));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Appointment> appointmentPage = appointmentRepository.findAll(spec, pageable);
        log.info("[APPOINTMENT-SERVICE] Truy vấn thành công! Tổng số bản ghi: {}, Số trang: {}",
                appointmentPage.getTotalElements(), appointmentPage.getTotalPages());

        Page<AppointmentResponse> responsePage = appointmentPage.map(appointmentMapper::toResponse);
        return PageResponse.from(responsePage);
    }

    /**
     * Lấy thông tin chi tiết một lịch hẹn theo ID.
     */
    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        log.info("[APPOINTMENT-SERVICE] Tìm kiếm lịch hẹn theo ID: {}", id);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[APPOINTMENT-SERVICE] Không tìm thấy lịch hẹn với ID: {}", id);
                    return new ResourceNotFoundException("Lịch hẹn", "id", id);
                });

        return appointmentMapper.toResponse(appointment);
    }
}
