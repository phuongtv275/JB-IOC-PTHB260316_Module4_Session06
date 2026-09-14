package com.example.doctorservice.service;

import com.example.doctorservice.dto.request.DoctorRequest;
import com.example.doctorservice.dto.response.PageResponse;
import com.example.doctorservice.dto.response.DoctorResponse;

/**
 * Interface định nghĩa các nghiệp vụ quản lý bác sĩ (DIP & OCP).
 */
public interface DoctorService {

    /**
     * Lấy danh sách bác sĩ có hỗ trợ phân trang, sắp xếp và lọc theo tiêu chí.
     * Mặc định trả về danh sách tóm tắt (id, name, specialization).
     *
     * @param page           Chỉ số trang (0-indexed).
     * @param size           Số lượng phần tử trên 1 trang.
     * @param sortBy         Trường sắp xếp (ví dụ: id, name, specialization, experienceYears).
     * @param sortDir        Hướng sắp xếp (asc hoặc desc).
     * @param specialization Tên chuyên khoa cần lọc (tùy chọn, null nếu không lọc).
     * @param status         Trạng thái làm việc cần lọc (tùy chọn, null nếu không lọc).
     * @param detail         Nếu true trả về thông tin đầy đủ, false trả về tóm tắt (id, name, specialization).
     * @return PageResponse chứa danh sách bác sĩ và thông tin phân trang.
     */
    PageResponse<DoctorResponse> getDoctors(int page, int size, String sortBy, String sortDir,
                                            String specialization, Boolean status, boolean detail);

    /**
     * Lấy thông tin chi tiết một bác sĩ theo ID.
     *
     * @param id ID của bác sĩ.
     * @return DTO DoctorResponse chứa đầy đủ chi tiết thông tin bác sĩ.
     */
    DoctorResponse getDoctorById(Long id);

    /**
     * Thêm mới bác sĩ vào hệ thống.
     *
     * @param request DTO dữ liệu tạo bác sĩ.
     * @return DTO DoctorResponse của bác sĩ vừa tạo.
     */
    DoctorResponse createDoctor(DoctorRequest request);
}
