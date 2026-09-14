package com.example.appointmentservice.exception;

/**
 * Ngoại lệ ném ra khi không tìm thấy tài nguyên trong hệ thống (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("Không tìm thấy %s với %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
