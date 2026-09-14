package com.example.patientservice.exception;

import com.example.patientservice.dto.response.ApiResponse;
import com.example.patientservice.filter.CorrelationIdFilter;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * Bộ xử lý ngoại lệ tập trung toàn ứng dụng (Global Exception Handler).
 * Bắt các lỗi xảy ra trong tầng Controller / Service và chuyển thành chuẩn phản hồi ApiResponse.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String getCorrelationId() {
        String id = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        return id != null ? id : "N/A";
    }

    /**
     * Xử lý lỗi validation DTO (@Valid, @NotNull, @NotBlank, etc.)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        String correlationId = getCorrelationId();
        log.warn("[VALIDATION-ERROR] [cid:{}] Validation failed for fields: {}", correlationId, fieldErrors);

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Dữ liệu gửi lên không hợp lệ, vui lòng kiểm tra lại")
                .errors(fieldErrors)
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý lỗi vi phạm ràng buộc Constraint (@Validated ở Controller param)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        String correlationId = getCorrelationId();
        log.warn("[CONSTRAINT-VIOLATION] [cid:{}] {}", correlationId, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                "Vi phạm ràng buộc dữ liệu: " + ex.getMessage(),
                null,
                correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý lỗi không tìm thấy tài nguyên (HTTP 404)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        String correlationId = getCorrelationId();
        log.warn("[NOT-FOUND] [cid:{}] {}", correlationId, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), null, correlationId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Xử lý lỗi xung đột dữ liệu duy nhất (HTTP 409)
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateResourceException(DuplicateResourceException ex) {
        String correlationId = getCorrelationId();
        log.warn("[DUPLICATE-DATA] [cid:{}] {}", correlationId, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), null, correlationId);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Xử lý lỗi nghiệp vụ chung (HTTP 400)
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequestException(BadRequestException ex) {
        String correlationId = getCorrelationId();
        log.warn("[BAD-REQUEST] [cid:{}] {}", correlationId, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), null, correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý lỗi sai định dạng JSON hoặc sai kiểu dữ liệu truyền vào
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String correlationId = getCorrelationId();
        log.warn("[MALFORMED-JSON] [cid:{}] Malformed JSON request: {}", correlationId, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                "Định dạng JSON gửi lên không hợp lệ hoặc sai kiểu dữ liệu (VD: định dạng ngày sinh YYYY-MM-DD)",
                null,
                correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý lỗi sai kiểu tham số URL (VD: id truyền chữ cái thay vì số)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String correlationId = getCorrelationId();
        log.warn("[TYPE-MISMATCH] [cid:{}] Parameter '{}' has invalid value: '{}'",
                correlationId, ex.getName(), ex.getValue());

        String message = String.format("Tham số '%s' nhận giá trị không hợp lệ: '%s'", ex.getName(), ex.getValue());
        ApiResponse<Object> response = ApiResponse.error(message, null, correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý lỗi IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String correlationId = getCorrelationId();
        log.warn("[ILLEGAL-ARG] [cid:{}] {}", correlationId, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), null, correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý tất cả các ngoại lệ chưa được khai báo khác (HTTP 500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex) {
        String correlationId = getCorrelationId();
        log.error("[SYSTEM-ERROR] [cid:{}] Unhandled exception caught:", correlationId, ex);

        ApiResponse<Object> response = ApiResponse.error(
                "Đã có lỗi xảy ra từ phía hệ thống, vui lòng thử lại sau!",
                null,
                correlationId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
