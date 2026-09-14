package com.example.patientservice.repository;

import com.example.patientservice.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository thao tác cơ sở dữ liệu với thực thể Patient.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * Kiểm tra số điện thoại đã tồn tại trong hệ thống hay chưa.
     *
     * @param phoneNumber Số điện thoại cần kiểm tra.
     * @return true nếu số điện thoại đã tồn tại, ngược lại false.
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Tìm kiếm bệnh nhân theo số điện thoại.
     *
     * @param phoneNumber Số điện thoại cần tìm.
     * @return Optional chứa thông tin bệnh nhân nếu tìm thấy.
     */
    Optional<Patient> findByPhoneNumber(String phoneNumber);
}
