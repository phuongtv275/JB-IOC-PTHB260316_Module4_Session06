package com.example.patientservice.controller;

import com.example.patientservice.dto.request.PatientRequest;
import com.example.patientservice.dto.response.PageResponse;
import com.example.patientservice.dto.response.PatientResponse;
import com.example.patientservice.exception.GlobalExceptionHandler;
import com.example.patientservice.exception.ResourceNotFoundException;
import com.example.patientservice.filter.CorrelationIdFilter;
import com.example.patientservice.service.PatientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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

@WebMvcTest(PatientController.class)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class})
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PatientService patientService;

    @Test
    @DisplayName("POST /api/v1/patients - Thành công trả về 201 Created và correlationId")
    void createPatient_Success_Returns201() throws Exception {
        PatientRequest request = PatientRequest.builder()
                .fullName("Trần Thị B")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .gender("Nữ")
                .phoneNumber("0912345678")
                .address("456 Đường Nguyễn Trãi, Thanh Xuân, Hà Nội")
                .medicalHistory("Không có bệnh nền")
                .build();

        PatientResponse response = PatientResponse.builder()
                .id(1L)
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .medicalHistory(request.getMedicalHistory())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(patientService.createPatient(any(PatientRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "test-corr-id-12345")
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", "test-corr-id-12345"))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Thêm mới bệnh nhân thành công")))
                .andExpect(jsonPath("$.correlationId", is("test-corr-id-12345")))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.fullName", is("Trần Thị B")))
                .andExpect(jsonPath("$.data.phoneNumber", is("0912345678")));
    }

    @Test
    @DisplayName("POST /api/v1/patients - Dữ liệu không hợp lệ trả về 400 Bad Request kèm chi tiết lỗi")
    void createPatient_InvalidPayload_Returns400() throws Exception {
        PatientRequest invalidRequest = PatientRequest.builder()
                .fullName("") // trống
                .dateOfBirth(LocalDate.now().plusDays(1)) // tương lai
                .gender("") // trống
                .phoneNumber("12345") // sai định dạng số điện thoại VN
                .address("") // trống
                .build();

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors.fullName", notNullValue()))
                .andExpect(jsonPath("$.errors.dateOfBirth", notNullValue()))
                .andExpect(jsonPath("$.errors.gender", notNullValue()))
                .andExpect(jsonPath("$.errors.phoneNumber", notNullValue()))
                .andExpect(jsonPath("$.errors.address", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/patients - Lấy danh sách phân trang thành công trả về 200 OK")
    void getAllPatients_Success_Returns200() throws Exception {
        PatientResponse p1 = PatientResponse.builder()
                .id(1L)
                .fullName("Trần Thị B")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .gender("Nữ")
                .phoneNumber("0912345678")
                .address("Hà Nội")
                .build();

        PageResponse<PatientResponse> pageResponse = PageResponse.<PatientResponse>builder()
                .items(List.of(p1))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        when(patientService.getAllPatients(0, 10, "id", "desc")).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/patients?page=0&size=10&sortBy=id&sortDir=desc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items.length()", is(1)))
                .andExpect(jsonPath("$.data.items[0].fullName", is("Trần Thị B")))
                .andExpect(jsonPath("$.data.totalElements", is(1)));
    }

    @Test
    @DisplayName("GET /api/v1/patients/{id} - Tìm kiếm bệnh nhân theo ID không tồn tại trả về 404")
    void getPatientById_NotFound_Returns404() throws Exception {
        when(patientService.getPatientById(999L)).thenThrow(new ResourceNotFoundException("Bệnh nhân", "id", 999L));

        mockMvc.perform(get("/api/v1/patients/999"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Không tìm thấy Bệnh nhân với id: '999'")));
    }
}
