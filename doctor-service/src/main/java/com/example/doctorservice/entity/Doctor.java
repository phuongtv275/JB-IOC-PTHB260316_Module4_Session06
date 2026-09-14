package com.example.doctorservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Thực thể lưu trữ thông tin bác sĩ trong hệ thống.
 * Bao gồm các thông tin cá nhân, chuyên khoa, kinh nghiệm và trạng thái làm việc.
 */
@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "specialization", nullable = false, length = 100)
    private String specialization;

    @Column(name = "experience_years", nullable = false)
    private Integer experienceYears;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Tự động gán thời điểm tạo và sửa đổi khi persist entity lần đầu vào CSDL.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = true; // Mặc định là đang làm việc
        }
    }

    /**
     * Tự động cập nhật thời gian sửa đổi trước mỗi lần update entity.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
