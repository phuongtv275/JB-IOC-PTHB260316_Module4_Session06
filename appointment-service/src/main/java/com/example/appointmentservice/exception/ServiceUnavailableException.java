package com.example.appointmentservice.exception;

/**
 * Ngoại lệ ném ra khi không thể kết nối tới microservice khác (HTTP 503 Service Unavailable).
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
