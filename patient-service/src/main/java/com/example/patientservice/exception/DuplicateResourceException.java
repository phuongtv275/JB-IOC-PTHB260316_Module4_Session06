package com.example.patientservice.exception;

/**
 * Ngoại lệ ném ra khi vi phạm ràng buộc dữ liệu duy nhất (HTTP 409 Conflict).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
