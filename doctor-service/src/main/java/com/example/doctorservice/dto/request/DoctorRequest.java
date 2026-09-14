package com.example.doctorservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO nhận dữ liệu khi thêm mới hoặc cập nhật thông tin bác sĩ.
 * Sử dụng Jakarta Validation để kiểm tra tính toàn vẹn dữ liệu đầu vào.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorRequest {

    @NotBlank(message = "Tên bác sĩ không được để trống")
    @Size(min = 2, max = 150, message = "Tên bác sĩ phải từ 2 đến 150 ký tự")
    private String name;

    @NotBlank(message = "Chuyên khoa không được để trống")
    @Size(min = 2, max = 100, message = "Chuyên khoa phải từ 2 đến 100 ký tự")
    private String specialization;

    @NotNull(message = "Số năm kinh nghiệm không được để trống")
    @Min(value = 0, message = "Số năm kinh nghiệm không được là số âm")
    @Max(value = 70, message = "Số năm kinh nghiệm không hợp lệ (tối đa 70 năm)")
    private Integer experienceYears;

    @NotBlank(message = "Email công việc không được để trống")
    @Email(message = "Email không đúng định dạng hợp lệ")
    @Size(max = 100, message = "Email không vượt quá 100 ký tự")
    private String email;

    @NotNull(message = "Trạng thái làm việc không được để trống (true: Đang làm việc, false: Nghỉ phép)")
    private Boolean status;
}
