package com.example.doctorservice.service;

import com.example.doctorservice.dto.request.DoctorRequest;
import com.example.doctorservice.dto.response.DoctorResponse;
import com.example.doctorservice.dto.response.PageResponse;
import com.example.doctorservice.entity.Doctor;
import com.example.doctorservice.exception.BadRequestException;
import com.example.doctorservice.exception.DuplicateResourceException;
import com.example.doctorservice.exception.ResourceNotFoundException;
import com.example.doctorservice.mapper.DoctorMapper;
import com.example.doctorservice.repository.DoctorRepository;
import com.example.doctorservice.service.impl.DoctorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorMapper doctorMapper;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    private Doctor sampleDoctor;
    private DoctorResponse sampleSummaryResponse;
    private DoctorResponse sampleDetailResponse;
    private DoctorRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleDoctor = Doctor.builder()
                .id(1L)
                .name("BS. Nguyễn Văn Hùng")
                .specialization("Nội khoa")
                .experienceYears(12)
                .email("hung.nguyen@hospital.vn")
                .status(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleSummaryResponse = DoctorResponse.builder()
                .id(1L)
                .name("BS. Nguyễn Văn Hùng")
                .specialization("Nội khoa")
                .build();

        sampleDetailResponse = DoctorResponse.builder()
                .id(1L)
                .name("BS. Nguyễn Văn Hùng")
                .specialization("Nội khoa")
                .experienceYears(12)
                .email("hung.nguyen@hospital.vn")
                .status(true)
                .createdAt(sampleDoctor.getCreatedAt())
                .updatedAt(sampleDoctor.getUpdatedAt())
                .build();

        sampleRequest = DoctorRequest.builder()
                .name("BS. Nguyễn Văn Hùng")
                .specialization("Nội khoa")
                .experienceYears(12)
                .email("hung.nguyen@hospital.vn")
                .status(true)
                .build();
    }

    @Test
    @DisplayName("Lấy danh sách bác sĩ tóm tắt (id, name, specialization) mặc định thành công")
    void getDoctors_SummaryDefault_Success() {
        Page<Doctor> mockPage = new PageImpl<>(List.of(sampleDoctor));
        when(doctorRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(doctorMapper.toSummaryResponse(sampleDoctor)).thenReturn(sampleSummaryResponse);

        PageResponse<DoctorResponse> result = doctorService.getDoctors(
                0, 10, "id", "asc", null, null, false
        );

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(1L, result.getItems().get(0).getId());
        assertEquals("BS. Nguyễn Văn Hùng", result.getItems().get(0).getName());
        assertEquals("Nội khoa", result.getItems().get(0).getSpecialization());
        assertNull(result.getItems().get(0).getExperienceYears()); // Không có trong summary

        verify(doctorMapper).toSummaryResponse(sampleDoctor);
        verify(doctorMapper, never()).toDetailResponse(any());
    }

    @Test
    @DisplayName("Lấy danh sách bác sĩ chi tiết khi detail=true thành công")
    void getDoctors_DetailTrue_Success() {
        Page<Doctor> mockPage = new PageImpl<>(List.of(sampleDoctor));
        when(doctorRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(doctorMapper.toDetailResponse(sampleDoctor)).thenReturn(sampleDetailResponse);

        PageResponse<DoctorResponse> result = doctorService.getDoctors(
                0, 10, "id", "asc", null, null, true
        );

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(12, result.getItems().get(0).getExperienceYears());
        assertEquals("hung.nguyen@hospital.vn", result.getItems().get(0).getEmail());

        verify(doctorMapper).toDetailResponse(sampleDoctor);
    }

    @Test
    @DisplayName("Ném ngoại lệ BadRequestException khi truyền sortBy không hợp lệ")
    void getDoctors_InvalidSortBy_ThrowsBadRequest() {
        assertThrows(BadRequestException.class, () ->
                doctorService.getDoctors(0, 10, "invalid_col", "asc", null, null, false)
        );
    }

    @Test
    @DisplayName("Tìm bác sĩ theo ID thành công trả về đầy đủ chi tiết")
    void getDoctorById_Success() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(sampleDoctor));
        when(doctorMapper.toDetailResponse(sampleDoctor)).thenReturn(sampleDetailResponse);

        DoctorResponse result = doctorService.getDoctorById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("BS. Nguyễn Văn Hùng", result.getName());
    }

    @Test
    @DisplayName("Tìm bác sĩ theo ID không tồn tại ném ngoại lệ ResourceNotFoundException")
    void getDoctorById_NotFound_ThrowsException() {
        when(doctorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> doctorService.getDoctorById(999L));
    }

    @Test
    @DisplayName("Thêm mới bác sĩ thành công khi email chưa tồn tại")
    void createDoctor_Success() {
        when(doctorRepository.existsByEmail("hung.nguyen@hospital.vn")).thenReturn(false);
        when(doctorMapper.toEntity(sampleRequest)).thenReturn(sampleDoctor);
        when(doctorRepository.save(sampleDoctor)).thenReturn(sampleDoctor);
        when(doctorMapper.toDetailResponse(sampleDoctor)).thenReturn(sampleDetailResponse);

        DoctorResponse result = doctorService.createDoctor(sampleRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(doctorRepository).save(sampleDoctor);
    }

    @Test
    @DisplayName("Ném ngoại lệ DuplicateResourceException khi email đã tồn tại")
    void createDoctor_DuplicateEmail_ThrowsException() {
        when(doctorRepository.existsByEmail("hung.nguyen@hospital.vn")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> doctorService.createDoctor(sampleRequest));
        verify(doctorRepository, never()).save(any());
    }
}
