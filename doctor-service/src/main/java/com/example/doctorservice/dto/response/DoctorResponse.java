package com.example.doctorservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO trả về thông tin bác sĩ cho client.
 * Hỗ trợ cả chế độ xem tóm tắt (id, name, specialization) và chế độ xem chi tiết đầy đủ.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DoctorResponse {

    private Long id;
    private String name;
    private String specialization;
    private Integer experienceYears;
    private String email;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
