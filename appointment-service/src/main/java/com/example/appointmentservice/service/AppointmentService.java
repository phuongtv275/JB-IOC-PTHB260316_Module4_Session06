package com.example.appointmentservice.service;

import com.example.appointmentservice.dto.request.AppointmentRequest;
import com.example.appointmentservice.dto.response.AppointmentResponse;
import com.example.appointmentservice.dto.response.PageResponse;

/**
 * Interface định nghĩa các nghiệp vụ quản lý lịch khám bệnh (DIP & OCP).
 */
public interface AppointmentService {

    /**
     * Tạo mới một lịch hẹn khám bệnh.
     * Xác thực sự tồn tại của bệnh nhân và bác sĩ qua RestTemplate trước khi lưu.
     *
     * @param request DTO thông tin lịch khám cần tạo.
     * @return DTO thông tin lịch khám đã được lưu thành công.
     */
    AppointmentResponse createAppointment(AppointmentRequest request);

    /**
     * Lấy danh sách lịch khám có phân trang, sắp xếp và lọc đa tiêu chí.
     *
     * @param page      Chỉ số trang (0-indexed).
     * @param size      Số lượng phần tử trên 1 trang.
     * @param sortBy    Trường sắp xếp.
     * @param sortDir   Hướng sắp xếp (asc hoặc desc).
     * @param patientId Lọc theo ID bệnh nhân (tùy chọn).
     * @param doctorId  Lọc theo ID bác sĩ (tùy chọn).
     * @param status    Lọc theo trạng thái (tùy chọn: PENDING, CONFIRMED, CANCELLED).
     * @return PageResponse bọc danh sách lịch hẹn và metadata phân trang.
     */
    PageResponse<AppointmentResponse> getAppointments(int page, int size, String sortBy, String sortDir,
                                                      Long patientId, Long doctorId, String status);

    /**
     * Lấy thông tin chi tiết một lịch hẹn theo ID.
     *
     * @param id Khóa chính định danh lịch hẹn.
     * @return DTO chi tiết lịch hẹn.
     */
    AppointmentResponse getAppointmentById(Long id);
}
