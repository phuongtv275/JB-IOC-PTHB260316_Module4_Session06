package com.example.doctorservice.config;

import com.example.doctorservice.entity.Doctor;
import com.example.doctorservice.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Khởi tạo dữ liệu mẫu cho bảng doctors khi ứng dụng khởi động lần đầu (nếu CSDL trống).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DoctorRepository doctorRepository;

    @Override
    public void run(String... args) {
        if (doctorRepository.count() == 0) {
            log.info("[DATA-INIT] Chưa có dữ liệu bác sĩ trong CSDL, tiến hành nạp dữ liệu mẫu ban đầu...");

            List<Doctor> sampleDoctors = List.of(
                    Doctor.builder()
                            .name("BS. Nguyễn Văn Hùng")
                            .specialization("Nội khoa")
                            .experienceYears(12)
                            .email("hung.nguyen@hospital.vn")
                            .status(true)
                            .build(),
                    Doctor.builder()
                            .name("BS. Trần Mai Anh")
                            .specialization("Nhi khoa")
                            .experienceYears(8)
                            .email("maianh.tran@hospital.vn")
                            .status(true)
                            .build(),
                    Doctor.builder()
                            .name("BS. Lê Quốc Tuấn")
                            .specialization("Ngoại khoa")
                            .experienceYears(15)
                            .email("tuan.le@hospital.vn")
                            .status(true)
                            .build(),
                    Doctor.builder()
                            .name("BS. Phạm Minh Đức")
                            .specialization("Tim mạch")
                            .experienceYears(20)
                            .email("duc.pham@hospital.vn")
                            .status(false)
                            .build()
            );

            doctorRepository.saveAll(sampleDoctors);
            log.info("[DATA-INIT] Đã nạp thành công {} bác sĩ mẫu vào CSDL!", sampleDoctors.size());
        }
    }
}
