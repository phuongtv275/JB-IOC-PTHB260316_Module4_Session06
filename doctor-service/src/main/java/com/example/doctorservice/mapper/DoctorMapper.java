package com.example.doctorservice.mapper;

import com.example.doctorservice.dto.request.DoctorRequest;
import com.example.doctorservice.dto.response.DoctorResponse;
import com.example.doctorservice.entity.Doctor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper chuyển đổi giữa Entity Doctor và các DTO sử dụng MapStruct.
 * Tự động sinh mã nguồn ở compile-time, an toàn kiểu dữ liệu và tối ưu hiệu năng.
 * componentModel = "spring" giúp MapStruct mapper được quản lý như một Spring Bean (@Component).
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DoctorMapper {

    /**
     * Chuyển đổi Doctor sang DTO tóm tắt chỉ bao gồm: id, name, specialization.
     * Bỏ qua các trường chi tiết (kinh nghiệm, email, trạng thái, timestamps).
     *
     * @param entity Thực thể Doctor từ CSDL.
     * @return DTO DoctorResponse tóm tắt.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "specialization", source = "specialization")
    @Mapping(target = "experienceYears", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DoctorResponse toSummaryResponse(Doctor entity);

    /**
     * Chuyển đổi Doctor sang DTO chi tiết đầy đủ mọi thuộc tính.
     *
     * @param entity Thực thể Doctor từ CSDL.
     * @return DTO DoctorResponse đầy đủ.
     */
    DoctorResponse toDetailResponse(Doctor entity);

    /**
     * Chuyển đổi DoctorRequest sang Entity Doctor.
     * Bỏ qua id, createdAt, updatedAt do CSDL và JPA tự động quản lý.
     *
     * @param request DTO gửi từ client.
     * @return Entity Doctor.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Doctor toEntity(DoctorRequest request);
}
