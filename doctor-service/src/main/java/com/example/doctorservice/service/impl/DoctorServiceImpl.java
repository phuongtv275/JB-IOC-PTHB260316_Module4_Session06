package com.example.doctorservice.service.impl;

import com.example.doctorservice.dto.request.DoctorRequest;
import com.example.doctorservice.dto.response.DoctorResponse;
import com.example.doctorservice.dto.response.PageResponse;
import com.example.doctorservice.entity.Doctor;
import com.example.doctorservice.exception.BadRequestException;
import com.example.doctorservice.exception.DuplicateResourceException;
import com.example.doctorservice.exception.ResourceNotFoundException;
import com.example.doctorservice.mapper.DoctorMapper;
import com.example.doctorservice.repository.DoctorRepository;
import com.example.doctorservice.service.DoctorService;
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
 * Lớp triển khai nghiệp vụ cho DoctorService.
 * Tuân thủ Single Responsibility Principle (SRP) và Open-Closed Principle (OCP).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorMapper doctorMapper;

    /**
     * Danh sách các trường cho phép sắp xếp nhằm đảm bảo an toàn truy vấn và tránh lỗi InvalidPropertyReferenceException.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "specialization", "experienceYears", "email", "status", "createdAt"
    );

    /**
     * Lấy danh sách bác sĩ với các tính năng phân trang, sắp xếp và lọc động.
     * Mặc định trả về DTO tóm tắt chỉ gồm (id, name, specialization) theo yêu cầu.
     * Nếu detail = true sẽ trả về toàn bộ thuộc tính chi tiết.
     * 
     * Logic nghiệp vụ:
     * 1. Validate tham số phân trang (page >= 0, 1 <= size <= 100).
     * 2. Validate trường sortBy nằm trong danh sách an toàn.
     * 3. Xây dựng JPA Specification động để lọc theo chuyên khoa (specialization) hoặc trạng thái (status) nếu có.
     * 4. Thực thi truy vấn phân trang qua Spring Data JPA.
     * 5. Ánh xạ từng phần tử sang DTO tương ứng (summary hoặc detail) và bọc vào PageResponse.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> getDoctors(int page, int size, String sortBy, String sortDir,
                                                   String specialization, Boolean status, boolean detail) {

        log.info("[DOCTOR-SERVICE] Truy vấn danh sách bác sĩ: page={}, size={}, sortBy={}, sortDir={}, specialization='{}', status={}, detail={}",
                page, size, sortBy, sortDir, specialization, status, detail);

        // 1. Kiểm tra tham số phân trang
        if (page < 0) {
            throw new BadRequestException("Chỉ số trang (page) không được nhỏ hơn 0");
        }
        if (size <= 0 || size > 100) {
            throw new BadRequestException("Kích thước trang (size) phải từ 1 đến 100");
        }

        // 2. Kiểm tra thuộc tính sắp xếp
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException(
                    String.format("Trường sắp xếp '%s' không hợp lệ. Các trường hợp lệ: %s", sortBy, ALLOWED_SORT_FIELDS)
            );
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // 3. Xây dựng JPA Specification lọc động
        Specification<Doctor> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(specialization)) {
                predicates.add(cb.equal(cb.lower(root.get("specialization")), specialization.trim().toLowerCase()));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 4. Truy vấn database
        Page<Doctor> doctorPage = doctorRepository.findAll(spec, pageable);
        log.info("[DOCTOR-SERVICE] Lấy danh sách thành công! Tổng số bác sĩ: {}, Số trang: {}",
                doctorPage.getTotalElements(), doctorPage.getTotalPages());

        // 5. Ánh xạ dữ liệu: nếu detail=false -> toSummaryResponse (id, name, specialization)
        Page<DoctorResponse> responsePage = doctorPage.map(doctor ->
                detail ? doctorMapper.toDetailResponse(doctor) : doctorMapper.toSummaryResponse(doctor)
        );

        return PageResponse.from(responsePage);
    }

    /**
     * Lấy chi tiết thông tin một bác sĩ theo ID.
     */
    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(Long id) {
        log.info("[DOCTOR-SERVICE] Tìm kiếm bác sĩ theo ID: {}", id);

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[DOCTOR-SERVICE] Không tìm thấy bác sĩ với ID: {}", id);
                    return new ResourceNotFoundException("Bác sĩ", "id", id);
                });

        return doctorMapper.toDetailResponse(doctor);
    }

    /**
     * Thêm mới bác sĩ vào hệ thống.
     * 
     * Logic nghiệp vụ:
     * 1. Kiểm tra tính duy nhất của email công việc.
     * 2. Ánh xạ DTO sang Entity.
     * 3. Lưu xuống cơ sở dữ liệu.
     * 4. Trả về DTO chi tiết của bác sĩ vừa tạo.
     */
    @Override
    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request) {
        log.info("[DOCTOR-SERVICE] Bắt đầu thêm mới bác sĩ: name='{}', email='{}'",
                request.getName(), request.getEmail());

        if (doctorRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            log.warn("[DOCTOR-SERVICE] Email '{}' đã tồn tại trong hệ thống", request.getEmail());
            throw new DuplicateResourceException(
                    String.format("Bác sĩ với email '%s' đã tồn tại trong hệ thống", request.getEmail())
            );
        }

        Doctor doctor = doctorMapper.toEntity(request);
        Doctor savedDoctor = doctorRepository.save(doctor);
        log.info("[DOCTOR-SERVICE] Thêm mới bác sĩ thành công! ID được cấp: {}", savedDoctor.getId());

        return doctorMapper.toDetailResponse(savedDoctor);
    }
}
