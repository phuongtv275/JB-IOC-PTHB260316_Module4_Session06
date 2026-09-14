package com.example.appointmentservice.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO nhận dữ liệu khi tạo mới lịch khám.
 * Có validate đầy đủ dữ liệu đầu vào.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequest {

    @NotNull(message = "ID bệnh nhân (patientId) không được để trống")
    @Positive(message = "ID bệnh nhân (patientId) phải là số nguyên dương")
    private Long patientId;

    @NotNull(message = "ID bác sĩ (doctorId) không được để trống")
    @Positive(message = "ID bác sĩ (doctorId) phải là số nguyên dương")
    private Long doctorId;

    @NotNull(message = "Ngày giờ hẹn khám không được để trống")
    @Future(message = "Ngày giờ hẹn khám phải ở thời điểm tương lai")
    private LocalDateTime appointmentDate;

    @NotBlank(message = "Lý do khám bệnh không được để trống")
    @Size(max = 500, message = "Lý do khám bệnh tối đa 500 ký tự")
    private String reason;

    @Pattern(regexp = "^(PENDING|CONFIRMED|CANCELLED)$", message = "Trạng thái lịch hẹn hợp lệ: PENDING, CONFIRMED, CANCELLED")
    private String status;
}
