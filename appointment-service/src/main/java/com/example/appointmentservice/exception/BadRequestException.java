package com.example.appointmentservice.exception;

/**
 * Ngoại lệ ném ra khi yêu cầu từ client không hợp lệ về mặt logic nghiệp vụ (HTTP 400 Bad Request).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
