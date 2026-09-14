package com.example.appointmentservice.client;

import com.example.appointmentservice.exception.ResourceNotFoundException;
import com.example.appointmentservice.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Client giao tiếp liên service (Inter-service communication) sử dụng RestTemplate (@LoadBalanced).
 * Tách biệt logic gọi HTTP sang các microservice khác nhằm tuân thủ Single Responsibility Principle (SRP).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalServiceClient {

    private final RestTemplate restTemplate;

    private static final String PATIENT_SERVICE_URL = "http://patient-service/api/v1/patients/{id}";
    private static final String DOCTOR_SERVICE_URL = "http://doctor-service/api/v1/doctors/{id}";

    /**
     * Gọi tới Patient-Service qua Eureka Service Name để xác thực sự tồn tại của bệnh nhân.
     *
     * @param patientId ID của bệnh nhân.
     * @throws ResourceNotFoundException    nếu bệnh nhân không tồn tại (HTTP 404).
     * @throws ServiceUnavailableException nếu Patient-Service không phản hồi hoặc Eureka chưa phân giải được.
     */
    public void validatePatientExists(Long patientId) {
        log.info("[REST-CLIENT] Xác thực bệnh nhân ID: {} qua Eureka Service 'patient-service'", patientId);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(PATIENT_SERVICE_URL, String.class, patientId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ResourceNotFoundException(String.format("Không tìm thấy bệnh nhân với ID: %d trong hệ thống", patientId));
            }
            log.info("[REST-CLIENT] Xác thực thành công: Bệnh nhân ID: {} tồn tại trong hệ thống", patientId);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("[REST-CLIENT] Bệnh nhân ID: {} không tồn tại trên patient-service (404 Not Found)", patientId);
            throw new ResourceNotFoundException(String.format("Không tìm thấy bệnh nhân với ID: %d trong hệ thống", patientId));
        } catch (HttpClientErrorException ex) {
            log.warn("[REST-CLIENT] Lỗi client khi gọi patient-service: {}", ex.getMessage());
            throw new ResourceNotFoundException(String.format("Không tìm thấy bệnh nhân với ID: %d trong hệ thống", patientId));
        } catch (ResourceAccessException ex) {
            log.error("[REST-CLIENT] Không thể kết nối tới 'patient-service' qua Eureka: {}", ex.getMessage());
            throw new ServiceUnavailableException("Dịch vụ quản lý bệnh nhân (patient-service) hiện không khả dụng, vui lòng thử lại sau!");
        } catch (RestClientException ex) {
            log.error("[REST-CLIENT] Lỗi khi giao tiếp với 'patient-service': {}", ex.getMessage());
            throw new ServiceUnavailableException("Lỗi kết nối tới patient-service: " + ex.getMessage());
        }
    }

    /**
     * Gọi tới Doctor-Service qua Eureka Service Name để xác thực sự tồn tại của bác sĩ.
     *
     * @param doctorId ID của bác sĩ.
     * @throws ResourceNotFoundException    nếu bác sĩ không tồn tại (HTTP 404).
     * @throws ServiceUnavailableException nếu Doctor-Service không phản hồi hoặc Eureka chưa phân giải được.
     */
    public void validateDoctorExists(Long doctorId) {
        log.info("[REST-CLIENT] Xác thực bác sĩ ID: {} qua Eureka Service 'doctor-service'", doctorId);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(DOCTOR_SERVICE_URL, String.class, doctorId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ResourceNotFoundException(String.format("Không tìm thấy bác sĩ với ID: %d trong hệ thống", doctorId));
            }
            log.info("[REST-CLIENT] Xác thực thành công: Bác sĩ ID: {} tồn tại trong hệ thống", doctorId);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("[REST-CLIENT] Bác sĩ ID: {} không tồn tại trên doctor-service (404 Not Found)", doctorId);
            throw new ResourceNotFoundException(String.format("Không tìm thấy bác sĩ với ID: %d trong hệ thống", doctorId));
        } catch (HttpClientErrorException ex) {
            log.warn("[REST-CLIENT] Lỗi client khi gọi doctor-service: {}", ex.getMessage());
            throw new ResourceNotFoundException(String.format("Không tìm thấy bác sĩ với ID: %d trong hệ thống", doctorId));
        } catch (ResourceAccessException ex) {
            log.error("[REST-CLIENT] Không thể kết nối tới 'doctor-service' qua Eureka: {}", ex.getMessage());
            throw new ServiceUnavailableException("Dịch vụ quản lý bác sĩ (doctor-service) hiện không khả dụng, vui lòng thử lại sau!");
        } catch (RestClientException ex) {
            log.error("[REST-CLIENT] Lỗi khi giao tiếp với 'doctor-service': {}", ex.getMessage());
            throw new ServiceUnavailableException("Lỗi kết nối tới doctor-service: " + ex.getMessage());
        }
    }
}
