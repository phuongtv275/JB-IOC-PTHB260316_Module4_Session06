# Medical Gateway (API Gateway trung tâm hệ thống y tế)

`medical-gateway` đóng vai trò là điểm đón tiếp duy nhất (Single Entry Point) cho toàn bộ các yêu cầu từ phía Client (Web, Mobile, Postman, bên thứ 3) gửi vào hệ thống microservices y tế. Dự án được xây dựng trên nền tảng **Spring Cloud Gateway** với kiến trúc bất đồng bộ (Reactive WebFlux / Netty).

---

## 1. Thông tin tổng quan

- **Cổng dịch vụ (Port):** `8080`
- **Tên đăng ký Eureka (Service ID):** `medical-gateway`
- **Nền tảng:** Spring Cloud Gateway, Spring Boot Reactive WebFlux
- **Định tuyến động:** Tích hợp Netflix Eureka Discovery Client (`lb://<SERVICE-NAME>`)
- **Bộ lọc toàn cục (Global Filter):** `LoggingGlobalFilter` ghi log toàn trình (Remote IP, Method, Path, Correlation ID, Status, Duration)

---

## 2. Cấu trúc thư mục

```text
medical-gateway/
├── src/main/java/com/example/medicalgateway/
│   ├── filter/
│   │   └── LoggingGlobalFilter.java        # GlobalFilter ghi log request và quản lý Correlation ID
│   └── MedicalGatewayApplication.java      # Main Application
└── src/main/resources/
    └── application.yaml                    # Cấu hình cổng 8080, Eureka và các quy tắc định tuyến (routes)
```

---

## 3. Cấu hình định tuyến (Routing Rules)

Gateway nhận các request tại cổng `8080` và tự động cân bằng tải, chuyển tiếp đến các service tương ứng trong mạng Eureka:

| Tuyến đường (Route ID) | Mẫu đường dẫn (Path Predicate) | Đích chuyển tiếp (Target URI) | Dịch vụ tiếp nhận |
| :--- | :--- | :--- | :--- |
| `patient-service` | `/api/v1/patients/**`, `/api/v1/patients` | `lb://PATIENT-SERVICE` | `patient-service` (Port 8081) |
| `doctor-service` | `/api/v1/doctors/**`, `/api/v1/doctors` | `lb://DOCTOR-SERVICE` | `doctor-service` (Port 8082) |
| `appointment-service`| `/api/v1/appointments/**`, `/api/v1/appointments`<br>`/api/v1/appointment/**`, `/api/v1/appointment` | `lb://APPOINTMENT-SERVICE` | `appointment-service` (Port 8083) |

---

## 4. Cơ chế Global Filter ghi log (`LoggingGlobalFilter`)

Theo tiêu chuẩn giám sát hệ thống phân tán, `LoggingGlobalFilter` triển khai `GlobalFilter` và `Ordered` với độ ưu tiên cao nhất (`Ordered.HIGHEST_PRECEDENCE`):

### 4.1. Thông tin ghi nhận khi Request đến (`[GATEWAY-REQUEST]`):
- **Địa chỉ IP máy khách (Remote Address):** Hỗ trợ trích xuất thông minh từ header `X-Forwarded-For` (qua proxy, load balancer), `X-Real-IP`, hoặc fallback về remote socket address trực tiếp.
- **Phương thức HTTP (HTTP Method):** `GET`, `POST`, `PUT`, `DELETE`...
- **Đường dẫn truy cập (Path):** Đường dẫn URI kèm chuỗi truy vấn (query params nếu có).
- **Correlation ID:** Tự động phát hiện header `X-Correlation-Id` hoặc `X-Request-Id` từ client gửi lên; nếu không có sẽ tự động sinh mới một mã `UUID` chuẩn.

### 4.2. Lan truyền Correlation ID (Distributed Tracing):
- Tự động gắn header `X-Correlation-Id` vào request gửi đến downstream microservices (`patient-service`, `doctor-service`, `appointment-service`).
- Tự động gắn header `X-Correlation-Id` vào response trả về cho client.

### 4.3. Thông tin ghi nhận khi Request hoàn tất (`[GATEWAY-RESPONSE]`):
- **Mã phản hồi HTTP (Status Code):** `200`, `201`, `400`, `404`, `503`...
- **Thời gian xử lý toàn trình (Duration):** Đo lường chính xác tổng thời gian từ lúc nhận request đến khi phản hồi (ms).

### Mẫu log thực tế:
```text
2026-09-14 11:45:00.123 [reactor-http-epoll-2] INFO  c.e.m.f.LoggingGlobalFilter - [GATEWAY-REQUEST] [Correlation Id:9b2c34a1-0612-4211-9a74-d4f1837890f5] Client IP: 127.0.0.1 | Method: POST | Path: /api/v1/appointments
2026-09-14 11:45:00.285 [reactor-http-epoll-2] INFO  c.e.m.f.LoggingGlobalFilter - [GATEWAY-RESPONSE] [Correlation ID:9b2c34a1-0612-4211-9a74-d4f1837890f5] Client IP: 127.0.0.1 | Method: POST | Path: /api/v1/appointments | Status: 201 | Duration: 162ms | Signal: onComplete
```

---

## 5. Hướng dẫn khởi chạy & Kiểm thử

### Yêu cầu môi trường
- Java Development Kit (JDK) 21 trở lên
- Discovery Server chạy sẵn tại `http://localhost:8761/eureka/`

### Lệnh khởi chạy Gateway
```bash
cd medical-gateway
./gradlew bootRun
```

### Chạy kiểm thử tự động
```bash
cd medical-gateway
./gradlew test
```
*(Bao gồm các test case kiểm tra cơ chế trích xuất IP, tạo lập/lan truyền Correlation ID và chuỗi reactive filter).*
