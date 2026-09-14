package com.example.patientservice.service;

import com.example.patientservice.dto.request.PatientRequest;
import com.example.patientservice.dto.response.PageResponse;
import com.example.patientservice.dto.response.PatientResponse;

/**
 * Interface định nghĩa các nghiệp vụ quản lý bệnh nhân (Interface Segregation & DIP).
 */
public interface PatientService {

    /**
     * Thêm mới một bệnh nhân vào hệ thống.
     *
     * @param request DTO thông tin bệnh nhân gửi từ client.
     * @return DTO thông tin bệnh nhân đã được lưu thành công.
     */
    PatientResponse createPatient(PatientRequest request);

    /**
     * Lấy danh sách bệnh nhân có hỗ trợ phân trang và sắp xếp.
     *
     * @param page    Chỉ số trang (bắt đầu từ 0).
     * @param size    Số lượng phần tử trên mỗi trang.
     * @param sortBy  Trường cần sắp xếp (ví dụ: id, fullName, createdAt).
     * @param sortDir Hướng sắp xếp (asc hoặc desc).
     * @return Đối tượng PageResponse chứa danh sách bệnh nhân và metadata phân trang.
     */
    PageResponse<PatientResponse> getAllPatients(int page, int size, String sortBy, String sortDir);

    /**
     * Lấy chi tiết thông tin một bệnh nhân theo ID.
     *
     * @param id Khóa chính định danh bệnh nhân.
     * @return DTO thông tin chi tiết bệnh nhân.
     */
    PatientResponse getPatientById(Long id);
}
