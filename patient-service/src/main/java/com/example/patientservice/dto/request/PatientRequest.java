package com.example.patientservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO nhận dữ liệu khi thêm mới hoặc cập nhật thông tin bệnh nhân.
 * Chứa các validation annotations để đảm bảo tính toàn vẹn dữ liệu đầu vào.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRequest {

    @NotBlank(message = "Họ và tên bệnh nhân không được để trống")
    @Size(min = 2, max = 150, message = "Họ và tên bệnh nhân phải từ 2 đến 150 ký tự")
    private String fullName;

    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Giới tính không được để trống")
    @Pattern(regexp = "^(?i)(Nam|Nữ|Khác|Male|Female|Other)$", message = "Giới tính hợp lệ: Nam, Nữ, Khác (hoặc Male, Female, Other)")
    private String gender;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
            message = "Số điện thoại không đúng định dạng hợp lệ của Việt Nam (VD: 0987654321)")
    private String phoneNumber;

    @NotBlank(message = "Địa chỉ thường trú không được để trống")
    @Size(max = 255, message = "Địa chỉ không vượt quá 255 ký tự")
    private String address;

    @Size(max = 2000, message = "Tiền sử bệnh lý không vượt quá 2000 ký tự")
    private String medicalHistory;
}
