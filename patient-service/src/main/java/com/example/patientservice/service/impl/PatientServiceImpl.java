package com.example.patientservice.service.impl;

import com.example.patientservice.dto.request.PatientRequest;
import com.example.patientservice.dto.response.PageResponse;
import com.example.patientservice.dto.response.PatientResponse;
import com.example.patientservice.entity.Patient;
import com.example.patientservice.exception.BadRequestException;
import com.example.patientservice.exception.DuplicateResourceException;
import com.example.patientservice.exception.ResourceNotFoundException;
import com.example.patientservice.mapper.PatientMapper;
import com.example.patientservice.repository.PatientRepository;
import com.example.patientservice.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Lớp triển khai nghiệp vụ cho PatientService.
 * Tuân thủ nguyên lý Single Responsibility Principle (SRP) và Open-Closed Principle (OCP).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    /**
     * Danh sách các trường cho phép sắp xếp nhằm ngăn chặn lỗi InvalidPropertyReferenceException.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "fullName", "dateOfBirth", "gender", "phoneNumber", "address", "createdAt"
    );

    /**
     * Thêm mới bệnh nhân vào cơ sở dữ liệu.
     * 
     * Logic nghiệp vụ:
     * 1. Kiểm tra tính duy nhất của số điện thoại (tránh việc 1 số điện thoại được đăng ký nhiều lần).
     * 2. Chuyển đổi DTO sang Entity bằng PatientMapper.
     * 3. Lưu entity xuống CSDL qua PatientRepository.
     * 4. Chuyển đổi Entity đã lưu (đã có ID sinh tự động và timestamps) sang DTO phản hồi.
     * 5. Ghi log đầy đủ để phục vụ việc giám sát và debug.
     */
    @Override
    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        log.info("[PATIENT-SERVICE] Bắt đầu thêm mới bệnh nhân: fullName='{}', phone='{}'",
                request.getFullName(), request.getPhoneNumber());

        // 1. Kiểm tra nghiệp vụ: số điện thoại không được trùng lặp
        if (patientRepository.existsByPhoneNumber(request.getPhoneNumber().trim())) {
            log.warn("[PATIENT-SERVICE] Thêm mới thất bại: Số điện thoại '{}' đã tồn tại trong hệ thống",
                    request.getPhoneNumber());
            throw new DuplicateResourceException(
                    String.format("Bệnh nhân với số điện thoại '%s' đã tồn tại trong hệ thống", request.getPhoneNumber())
            );
        }

        // 2. Chuyển đổi DTO -> Entity
        Patient patient = patientMapper.toEntity(request);
        log.debug("[PATIENT-SERVICE] Entity sau khi ánh xạ từ request: {}", patient);

        // 3. Lưu vào cơ sở dữ liệu
        Patient savedPatient = patientRepository.save(patient);
        log.info("[PATIENT-SERVICE] Lưu bệnh nhân thành công! ID được cấp: {}", savedPatient.getId());

        // 4. Ánh xạ sang DTO trả về cho client
        return patientMapper.toResponse(savedPatient);
    }

    /**
     * Lấy danh sách bệnh nhân có phân trang và sắp xếp linh hoạt.
     * 
     * Logic nghiệp vụ:
     * 1. Kiểm tra tính hợp lệ của tham số phân trang (page >= 0, 1 <= size <= 100).
     * 2. Kiểm tra thuộc tính sortBy có nằm trong danh sách các trường được phép hay không.
     * 3. Xây dựng đối tượng Pageable với chiều sắp xếp ASC hoặc DESC.
     * 4. Truy vấn database và chuyển đổi từng phần tử Page<Patient> sang Page<PatientResponse>.
     * 5. Đóng gói kết quả dưới dạng PageResponse chuẩn.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<PatientResponse> getAllPatients(int page, int size, String sortBy, String sortDir) {
        log.info("[PATIENT-SERVICE] Lấy danh sách bệnh nhân: page={}, size={}, sortBy={}, sortDir={}",
                page, size, sortBy, sortDir);

        // Kiểm tra hợp lệ cho pagination
        if (page < 0) {
            throw new BadRequestException("Chỉ số trang (page) không được nhỏ hơn 0");
        }
        if (size <= 0 || size > 100) {
            throw new BadRequestException("Kích thước trang (size) phải từ 1 đến 100");
        }

        // Validate thuộc tính sắp xếp
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException(
                    String.format("Trường sắp xếp '%s' không hợp lệ. Các trường hợp lệ: %s", sortBy, ALLOWED_SORT_FIELDS)
            );
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Patient> patientPage = patientRepository.findAll(pageable);
        log.info("[PATIENT-SERVICE] Truy vấn danh sách thành công. Tổng số bản ghi: {}, Tổng số trang: {}",
                patientPage.getTotalElements(), patientPage.getTotalPages());

        Page<PatientResponse> responsePage = patientPage.map(patientMapper::toResponse);
        return PageResponse.from(responsePage);
    }

    /**
     * Lấy thông tin chi tiết của một bệnh nhân theo ID.
     * 
     * Logic nghiệp vụ:
     * 1. Tìm kiếm bệnh nhân theo ID trong cơ sở dữ liệu.
     * 2. Nếu không tìm thấy, ném ngoại lệ ResourceNotFoundException (sẽ được GlobalExceptionHandler chuyển thành 404).
     * 3. Nếu tìm thấy, ánh xạ sang DTO phản hồi.
     */
    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        log.info("[PATIENT-SERVICE] Tìm kiếm thông tin bệnh nhân theo ID: {}", id);

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[PATIENT-SERVICE] Không tìm thấy bệnh nhân với ID: {}", id);
                    return new ResourceNotFoundException("Bệnh nhân", "id", id);
                });

        return patientMapper.toResponse(patient);
    }
}
