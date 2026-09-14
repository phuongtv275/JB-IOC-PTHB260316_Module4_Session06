package com.example.doctorservice.controller;

import com.example.doctorservice.dto.request.DoctorRequest;
import com.example.doctorservice.dto.response.DoctorResponse;
import com.example.doctorservice.dto.response.PageResponse;
import com.example.doctorservice.exception.GlobalExceptionHandler;
import com.example.doctorservice.exception.ResourceNotFoundException;
import com.example.doctorservice.filter.CorrelationIdFilter;
import com.example.doctorservice.service.DoctorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorController.class)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class})
class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DoctorService doctorService;

    @Test
    @DisplayName("GET /api/v1/doctors - Trả về danh sách bác sĩ với các thuộc tính id, name, specialization")
    void getDoctors_Success_ReturnsSummaryList() throws Exception {
        DoctorResponse doc1 = DoctorResponse.builder()
                .id(1L)
                .name("BS. Nguyễn Văn Hùng")
                .specialization("Nội khoa")
                .build();

        DoctorResponse doc2 = DoctorResponse.builder()
                .id(2L)
                .name("BS. Trần Mai Anh")
                .specialization("Nhi khoa")
                .build();

        PageResponse<DoctorResponse> pageResponse = PageResponse.<DoctorResponse>builder()
                .items(List.of(doc1, doc2))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(2)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        when(doctorService.getDoctors(0, 10, "id", "asc", null, null, false))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/doctors")
                        .header("X-Correlation-Id", "cid-doc-test-123"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "cid-doc-test-123"))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Lấy danh sách bác sĩ thành công")))
                .andExpect(jsonPath("$.correlationId", is("cid-doc-test-123")))
                .andExpect(jsonPath("$.data.items.length()", is(2)))
                .andExpect(jsonPath("$.data.items[0].id", is(1)))
                .andExpect(jsonPath("$.data.items[0].name", is("BS. Nguyễn Văn Hùng")))
                .andExpect(jsonPath("$.data.items[0].specialization", is("Nội khoa")))
                .andExpect(jsonPath("$.data.items[0].email").doesNotExist()) // Tóm tắt, không lộ email
                .andExpect(jsonPath("$.data.items[1].id", is(2)))
                .andExpect(jsonPath("$.data.items[1].name", is("BS. Trần Mai Anh")))
                .andExpect(jsonPath("$.data.items[1].specialization", is("Nhi khoa")));
    }

    @Test
    @DisplayName("GET /api/v1/doctors/{id} - Lấy chi tiết thông tin bác sĩ theo ID thành công")
    void getDoctorById_Success() throws Exception {
        DoctorResponse detail = DoctorResponse.builder()
                .id(1L)
                .name("BS. Nguyễn Văn Hùng")
                .specialization("Nội khoa")
                .experienceYears(12)
                .email("hung.nguyen@hospital.vn")
                .status(true)
                .build();

        when(doctorService.getDoctorById(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/doctors/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.name", is("BS. Nguyễn Văn Hùng")))
                .andExpect(jsonPath("$.data.specialization", is("Nội khoa")))
                .andExpect(jsonPath("$.data.experienceYears", is(12)))
                .andExpect(jsonPath("$.data.email", is("hung.nguyen@hospital.vn")))
                .andExpect(jsonPath("$.data.status", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/doctors/{id} - Không tìm thấy bác sĩ trả về 404")
    void getDoctorById_NotFound() throws Exception {
        when(doctorService.getDoctorById(999L)).thenThrow(new ResourceNotFoundException("Bác sĩ", "id", 999L));

        mockMvc.perform(get("/api/v1/doctors/999"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Không tìm thấy Bác sĩ với id: '999'")));
    }

    @Test
    @DisplayName("POST /api/v1/doctors - Thêm mới bác sĩ thành công trả về 201 Created")
    void createDoctor_Success() throws Exception {
        DoctorRequest request = DoctorRequest.builder()
                .name("BS. Lê Hoàng C")
                .specialization("Ngoại khoa")
                .experienceYears(15)
                .email("hoangc@hospital.vn")
                .status(true)
                .build();

        DoctorResponse createdResponse = DoctorResponse.builder()
                .id(3L)
                .name("BS. Lê Hoàng C")
                .specialization("Ngoại khoa")
                .experienceYears(15)
                .email("hoangc@hospital.vn")
                .status(true)
                .build();

        when(doctorService.createDoctor(any(DoctorRequest.class))).thenReturn(createdResponse);

        mockMvc.perform(post("/api/v1/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(3)))
                .andExpect(jsonPath("$.data.name", is("BS. Lê Hoàng C")));
    }

    @Test
    @DisplayName("POST /api/v1/doctors - Validate dữ liệu lỗi trả về 400 Bad Request kèm chi tiết vi phạm")
    void createDoctor_InvalidData_Returns400() throws Exception {
        DoctorRequest invalid = DoctorRequest.builder()
                .name("") // trống
                .specialization("") // trống
                .experienceYears(-1) // âm
                .email("sai-email") // không đúng format email
                .status(null) // null
                .build();

        mockMvc.perform(post("/api/v1/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors.name", notNullValue()))
                .andExpect(jsonPath("$.errors.specialization", notNullValue()))
                .andExpect(jsonPath("$.errors.experienceYears", notNullValue()))
                .andExpect(jsonPath("$.errors.email", notNullValue()))
                .andExpect(jsonPath("$.errors.status", notNullValue()));
    }
}
