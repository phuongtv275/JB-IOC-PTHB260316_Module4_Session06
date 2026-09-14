package com.example.appointmentservice.mapper;

import com.example.appointmentservice.dto.request.AppointmentRequest;
import com.example.appointmentservice.dto.response.AppointmentResponse;
import com.example.appointmentservice.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper chuyển đổi giữa Entity Appointment và các DTO sử dụng MapStruct.
 * Tự động sinh mã nguồn ở compile-time, an toàn kiểu dữ liệu và hiệu năng tối ưu.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentMapper {

    /**
     * Ánh xạ từ AppointmentRequest sang Appointment entity.
     * Bỏ qua id, createdAt, updatedAt do JPA và Database tự sinh.
     *
     * @param request DTO tạo lịch khám.
     * @return Entity Appointment.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Appointment toEntity(AppointmentRequest request);

    /**
     * Ánh xạ từ Appointment entity sang AppointmentResponse DTO.
     *
     * @param entity Entity Appointment từ CSDL.
     * @return DTO AppointmentResponse.
     */
    AppointmentResponse toResponse(Appointment entity);
}
