package com.example.doctorservice.repository;

import com.example.doctorservice.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository thao tác cơ sở dữ liệu với thực thể Doctor.
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long>, JpaSpecificationExecutor<Doctor> {

    /**
     * Kiểm tra email bác sĩ đã tồn tại trong hệ thống hay chưa.
     *
     * @param email Email công việc cần kiểm tra.
     * @return true nếu email đã tồn tại, ngược lại false.
     */
    boolean existsByEmail(String email);

    /**
     * Tìm kiếm bác sĩ theo email.
     *
     * @param email Email công việc.
     * @return Optional chứa Doctor nếu tìm thấy.
     */
    Optional<Doctor> findByEmail(String email);

    /**
     * Lọc danh sách bác sĩ theo chuyên khoa (không phân biệt hoa thường) có phân trang.
     *
     * @param specialization Tên chuyên khoa.
     * @param pageable       Thông tin phân trang.
     * @return Page chứa các bác sĩ thuộc chuyên khoa.
     */
    Page<Doctor> findBySpecializationIgnoreCase(String specialization, Pageable pageable);

    /**
     * Lọc danh sách bác sĩ theo trạng thái làm việc có phân trang.
     *
     * @param status   Trạng thái (true: Đang làm việc, false: Nghỉ phép).
     * @param pageable Thông tin phân trang.
     * @return Page chứa danh sách bác sĩ theo trạng thái.
     */
    Page<Doctor> findByStatus(Boolean status, Pageable pageable);
}
