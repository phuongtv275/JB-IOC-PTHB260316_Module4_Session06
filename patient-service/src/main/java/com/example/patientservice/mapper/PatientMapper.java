package com.example.patientservice.mapper;

import com.example.patientservice.dto.request.PatientRequest;
import com.example.patientservice.dto.response.PatientResponse;
import com.example.patientservice.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper chuyển đổi dữ liệu giữa Entity Patient và các DTO sử dụng MapStruct.
 * Tự động sinh mã nguồn ở compile-time, đảm bảo hiệu năng cao và type-safe.
 * componentModel = "spring" giúp MapStruct mapper được quản lý như một Spring Bean (@Component).
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PatientMapper {

    /**
     * Ánh xạ từ PatientRequest sang thực thể Patient.
     * Bỏ qua id, createdAt, updatedAt vì các trường này do JPA và database tự động quản lý.
     *
     * @param request DTO dữ liệu đầu vào.
     * @return Thực thể Patient.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Patient toEntity(PatientRequest request);

    /**
     * Ánh xạ từ thực thể Patient sang PatientResponse DTO trả về cho client.
     *
     * @param entity Thực thể Patient lấy từ CSDL.
     * @return DTO PatientResponse an toàn.
     */
    PatientResponse toResponse(Patient entity);
}
