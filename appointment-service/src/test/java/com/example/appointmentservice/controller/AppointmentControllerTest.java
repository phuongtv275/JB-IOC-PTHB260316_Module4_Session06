package com.example.appointmentservice.controller;

import com.example.appointmentservice.dto.request.AppointmentRequest;
import com.example.appointmentservice.dto.response.AppointmentResponse;
import com.example.appointmentservice.dto.response.PageResponse;
import com.example.appointmentservice.exception.GlobalExceptionHandler;
import com.example.appointmentservice.exception.ResourceNotFoundException;
import com.example.appointmentservice.filter.CorrelationIdFilter;
import com.example.appointmentservice.service.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class})
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppointmentService appointmentService;

    @Test
    @DisplayName("POST /api/v1/appointments - Tạo lịch hẹn thành công trả về 201 Created và correlationId")
    void createAppointment_Success() throws Exception {
        LocalDateTime appointmentDate = LocalDateTime.now().plusDays(3);
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(2L)
                .appointmentDate(appointmentDate)
                .reason("Tái khám định kỳ tim mạch")
                .status("PENDING")
                .build();

        AppointmentResponse response = AppointmentResponse.builder()
                .id(1L)
                .patientId(1L)
                .doctorId(2L)
                .appointmentDate(appointmentDate)
                .reason("Tái khám định kỳ tim mạch")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(appointmentService.createAppointment(any(AppointmentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "cid-appointment-test-01")
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", "cid-appointment-test-01"))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Tạo lịch hẹn khám bệnh thành công")))
                .andExpect(jsonPath("$.correlationId", is("cid-appointment-test-01")))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.patientId", is(1)))
                .andExpect(jsonPath("$.data.doctorId", is(2)))
                .andExpect(jsonPath("$.data.status", is("PENDING")));
    }

    @Test
    @DisplayName("POST /api/v1/appointment - Hỗ trợ cả đường dẫn số ít /api/v1/appointment")
    void createAppointment_SingularEndpoint_Success() throws Exception {
        LocalDateTime appointmentDate = LocalDateTime.now().plusDays(3);
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(2L)
                .appointmentDate(appointmentDate)
                .reason("Tái khám định kỳ")
                .build();

        AppointmentResponse response = AppointmentResponse.builder()
                .id(2L)
                .patientId(1L)
                .doctorId(2L)
                .appointmentDate(appointmentDate)
                .reason("Tái khám định kỳ")
                .status("PENDING")
                .build();

        when(appointmentService.createAppointment(any(AppointmentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/appointment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(2)));
    }

    @Test
    @DisplayName("POST /api/v1/appointments - Dữ liệu không hợp lệ (ngày quá khứ, ID null) trả về 400 Bad Request")
    void createAppointment_InvalidData_Returns400() throws Exception {
        AppointmentRequest invalid = AppointmentRequest.builder()
                .patientId(null)
                .doctorId(-1L)
                .appointmentDate(LocalDateTime.now().minusDays(1)) // Trong quá khứ
                .reason("") // Trống
                .status("INVALID_STATUS")
                .build();

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors.patientId", notNullValue()))
                .andExpect(jsonPath("$.errors.doctorId", notNullValue()))
                .andExpect(jsonPath("$.errors.appointmentDate", notNullValue()))
                .andExpect(jsonPath("$.errors.reason", notNullValue()))
                .andExpect(jsonPath("$.errors.status", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/appointments - Bệnh nhân không tồn tại trả về 404 Not Found")
    void createAppointment_PatientNotFound_Returns404() throws Exception {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(999L)
                .doctorId(1L)
                .appointmentDate(LocalDateTime.now().plusDays(1))
                .reason("Khám tổng quát")
                .build();

        when(appointmentService.createAppointment(any(AppointmentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy bệnh nhân với ID: 999 trong hệ thống"));

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Không tìm thấy bệnh nhân với ID: 999")));
    }

    @Test
    @DisplayName("GET /api/v1/appointments - Lấy danh sách lịch hẹn phân trang trả về 200 OK")
    void getAppointments_Success() throws Exception {
        AppointmentResponse item = AppointmentResponse.builder()
                .id(1L)
                .patientId(1L)
                .doctorId(2L)
                .appointmentDate(LocalDateTime.now().plusDays(2))
                .reason("Khám răng")
                .status("CONFIRMED")
                .build();

        PageResponse<AppointmentResponse> pageResponse = PageResponse.<AppointmentResponse>builder()
                .items(List.of(item))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        when(appointmentService.getAppointments(0, 10, "id", "desc", null, null, null))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/appointments?page=0&size=10&sortBy=id&sortDir=desc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items.length()", is(1)))
                .andExpect(jsonPath("$.data.items[0].id", is(1)))
                .andExpect(jsonPath("$.data.items[0].status", is("CONFIRMED")));
    }
}
