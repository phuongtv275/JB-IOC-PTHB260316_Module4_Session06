package com.example.patientservice.service;

import com.example.patientservice.dto.request.PatientRequest;
import com.example.patientservice.dto.response.PageResponse;
import com.example.patientservice.dto.response.PatientResponse;
import com.example.patientservice.entity.Patient;
import com.example.patientservice.exception.BadRequestException;
import com.example.patientservice.exception.DuplicateResourceException;
import com.example.patientservice.exception.ResourceNotFoundException;
import com.example.patientservice.mapper.PatientMapper;
import com.example.patientservice.repository.PatientRepository;
import com.example.patientservice.service.impl.PatientServiceImpl;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientMapper patientMapper;

    @InjectMocks
    private PatientServiceImpl patientService;

    private PatientRequest sampleRequest;
    private Patient sampleEntity;
    private PatientResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleRequest = PatientRequest.builder()
                .fullName("Nguyễn Văn A")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("Nam")
                .phoneNumber("0987654321")
                .address("123 Đường Cầu Giấy, Hà Nội")
                .medicalHistory("Dị ứng Penicillin, tiền sử viêm dạ dày")
                .build();

        sampleEntity = Patient.builder()
                .id(1L)
                .fullName("Nguyễn Văn A")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("Nam")
                .phoneNumber("0987654321")
                .address("123 Đường Cầu Giấy, Hà Nội")
                .medicalHistory("Dị ứng Penicillin, tiền sử viêm dạ dày")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleResponse = PatientResponse.builder()
                .id(1L)
                .fullName("Nguyễn Văn A")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("Nam")
                .phoneNumber("0987654321")
                .address("123 Đường Cầu Giấy, Hà Nội")
                .medicalHistory("Dị ứng Penicillin, tiền sử viêm dạ dày")
                .createdAt(sampleEntity.getCreatedAt())
                .updatedAt(sampleEntity.getUpdatedAt())
                .build();
    }

    @Test
    @DisplayName("Tạo mới bệnh nhân thành công khi thông tin hợp lệ")
    void createPatient_Success() {
        when(patientRepository.existsByPhoneNumber("0987654321")).thenReturn(false);
        when(patientMapper.toEntity(sampleRequest)).thenReturn(sampleEntity);
        when(patientRepository.save(sampleEntity)).thenReturn(sampleEntity);
        when(patientMapper.toResponse(sampleEntity)).thenReturn(sampleResponse);

        PatientResponse result = patientService.createPatient(sampleRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Nguyễn Văn A", result.getFullName());
        assertEquals("0987654321", result.getPhoneNumber());

        verify(patientRepository).existsByPhoneNumber("0987654321");
        verify(patientRepository).save(sampleEntity);
    }

    @Test
    @DisplayName("Ném ngoại lệ DuplicateResourceException khi số điện thoại đã tồn tại")
    void createPatient_DuplicatePhone_ThrowsException() {
        when(patientRepository.existsByPhoneNumber("0987654321")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> patientService.createPatient(sampleRequest));

        verify(patientRepository).existsByPhoneNumber("0987654321");
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lấy danh sách bệnh nhân phân trang thành công")
    void getAllPatients_Success() {
        Page<Patient> mockPage = new PageImpl<>(List.of(sampleEntity));
        when(patientRepository.findAll(any(Pageable.class))).thenReturn(mockPage);
        when(patientMapper.toResponse(sampleEntity)).thenReturn(sampleResponse);

        PageResponse<PatientResponse> result = patientService.getAllPatients(0, 10, "id", "desc");

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getPageSize());
    }

    @Test
    @DisplayName("Ném ngoại lệ BadRequestException khi trường sortBy không hợp lệ")
    void getAllPatients_InvalidSortBy_ThrowsException() {
        assertThrows(BadRequestException.class, () ->
                patientService.getAllPatients(0, 10, "malicious_column", "desc")
        );
    }

    @Test
    @DisplayName("Lấy thông tin bệnh nhân theo ID thành công")
    void getPatientById_Success() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(sampleEntity));
        when(patientMapper.toResponse(sampleEntity)).thenReturn(sampleResponse);

        PatientResponse result = patientService.getPatientById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Nguyễn Văn A", result.getFullName());
    }

    @Test
    @DisplayName("Ném ngoại lệ ResourceNotFoundException khi không tìm thấy ID")
    void getPatientById_NotFound_ThrowsException() {
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> patientService.getPatientById(999L));
    }
}
