package com.example.appointmentservice.service;

import com.example.appointmentservice.dto.request.AppointmentRequest;
import com.example.appointmentservice.dto.response.AppointmentResponse;
import com.example.appointmentservice.dto.response.PageResponse;
import com.example.appointmentservice.entity.Appointment;
import com.example.appointmentservice.exception.BadRequestException;
import com.example.appointmentservice.exception.ResourceNotFoundException;
import com.example.appointmentservice.exception.ServiceUnavailableException;
import com.example.appointmentservice.mapper.AppointmentMapper;
import com.example.appointmentservice.repository.AppointmentRepository;
import com.example.appointmentservice.service.impl.AppointmentServiceImpl;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private AppointmentRequest sampleRequest;
    private Appointment sampleEntity;
    private AppointmentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(2);

        sampleRequest = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(2L)
                .appointmentDate(appointmentTime)
                .reason("Khám đau nửa đầu mãn tính")
                .status("PENDING")
                .build();

        sampleEntity = Appointment.builder()
                .id(10L)
                .patientId(1L)
                .doctorId(2L)
                .appointmentDate(appointmentTime)
                .reason("Khám đau nửa đầu mãn tính")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleResponse = AppointmentResponse.builder()
                .id(10L)
                .patientId(1L)
                .doctorId(2L)
                .appointmentDate(appointmentTime)
                .reason("Khám đau nửa đầu mãn tính")
                .status("PENDING")
                .createdAt(sampleEntity.getCreatedAt())
                .updatedAt(sampleEntity.getUpdatedAt())
                .build();
    }

    @Test
    @DisplayName("Tạo lịch khám thành công khi bệnh nhân và bác sĩ hợp lệ")
    void createAppointment_Success() {
        when(restTemplate.getForEntity("http://patient-service/api/v1/patients/{id}", String.class, 1L))
                .thenReturn(ResponseEntity.ok("OK"));
        when(restTemplate.getForEntity("http://doctor-service/api/v1/doctors/{id}", String.class, 2L))
                .thenReturn(ResponseEntity.ok("OK"));
        when(appointmentMapper.toEntity(sampleRequest)).thenReturn(sampleEntity);
        when(appointmentRepository.save(sampleEntity)).thenReturn(sampleEntity);
        when(appointmentMapper.toResponse(sampleEntity)).thenReturn(sampleResponse);

        AppointmentResponse result = appointmentService.createAppointment(sampleRequest);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1L, result.getPatientId());
        assertEquals(2L, result.getDoctorId());
        assertEquals("PENDING", result.getStatus());

        verify(appointmentRepository).save(sampleEntity);
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi bệnh nhân không tồn tại trên Patient-Service")
    void createAppointment_PatientNotFound_ThrowsException() {
        when(restTemplate.getForEntity("http://patient-service/api/v1/patients/{id}", String.class, 1L))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThrows(ResourceNotFoundException.class, () -> appointmentService.createAppointment(sampleRequest));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi bác sĩ không tồn tại trên Doctor-Service")
    void createAppointment_DoctorNotFound_ThrowsException() {
        when(restTemplate.getForEntity("http://patient-service/api/v1/patients/{id}", String.class, 1L))
                .thenReturn(ResponseEntity.ok("OK"));
        when(restTemplate.getForEntity("http://doctor-service/api/v1/doctors/{id}", String.class, 2L))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThrows(ResourceNotFoundException.class, () -> appointmentService.createAppointment(sampleRequest));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ServiceUnavailableException khi Doctor-Service bị sập (lỗi kết nối/sự cố server)")
    void createAppointment_DoctorServiceDown_ThrowsServiceUnavailableException() {
        when(restTemplate.getForEntity("http://patient-service/api/v1/patients/{id}", String.class, 1L))
                .thenReturn(ResponseEntity.ok("OK"));
        when(restTemplate.getForEntity("http://doctor-service/api/v1/doctors/{id}", String.class, 2L))
                .thenThrow(new ResourceAccessException("I/O error on GET request: Connection refused"));

        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> appointmentService.createAppointment(sampleRequest)
        );

        assertEquals("Hệ thống quản lý bác sĩ hiện không khả dụng. Vui lòng đặt lịch sau!", exception.getMessage());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lấy danh sách lịch hẹn phân trang thành công")
    void getAppointments_Success() {
        Page<Appointment> mockPage = new PageImpl<>(List.of(sampleEntity));
        when(appointmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(appointmentMapper.toResponse(sampleEntity)).thenReturn(sampleResponse);

        PageResponse<AppointmentResponse> result = appointmentService.getAppointments(
                0, 10, "id", "desc", null, null, null
        );

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(10L, result.getItems().get(0).getId());
    }

    @Test
    @DisplayName("Ném BadRequestException khi thuộc tính sortBy không hợp lệ")
    void getAppointments_InvalidSortBy_ThrowsException() {
        assertThrows(BadRequestException.class, () ->
                appointmentService.getAppointments(0, 10, "unknown_column", "desc", null, null, null)
        );
    }

    @Test
    @DisplayName("Lấy thông tin chi tiết lịch hẹn theo ID thành công")
    void getAppointmentById_Success() {
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(sampleEntity));
        when(appointmentMapper.toResponse(sampleEntity)).thenReturn(sampleResponse);

        AppointmentResponse result = appointmentService.getAppointmentById(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
    }
}
