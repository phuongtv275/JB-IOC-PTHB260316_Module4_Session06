# Medical Discovery Server (Máy chủ đăng ký & định vị dịch vụ Eureka)

`medical-discovery-server` là trung tâm đăng ký và khám phá dịch vụ (Service Registry & Discovery) của toàn bộ kiến trúc microservices y tế, được xây dựng dựa trên **Spring Cloud Netflix Eureka Server**.

---

## 1. Thông tin tổng quan

- **Cổng dịch vụ (Port):** `8761`
- **Tên ứng dụng:** `medical-discovery-server`
- **Dashboard Web UI:** `http://localhost:8761`
- **Nền tảng:** Spring Cloud Netflix Eureka Server (Spring Boot 3.5.x)
- **Cấu hình Client:** Tự tắt đăng ký chính nó (`register-with-eureka: false`, `fetch-registry: false`) để đóng vai trò máy chủ trung tâm độc lập.

---

## 2. Vai trò trong kiến trúc Microservices

1. **Đăng ký dịch vụ (Service Registration):**
   - Khi các dịch vụ con (`patient-service`, `doctor-service`, `appointment-service`, `medical-gateway`) khởi động, chúng tự động gửi thông tin định danh (Service ID, IP, Port, Health status) lên Eureka Server.
2. **Khám phá dịch vụ (Service Discovery):**
   - API Gateway (`medical-gateway`) sử dụng Eureka để phân giải các định tuyến `lb://PATIENT-SERVICE`, `lb://DOCTOR-SERVICE`, `lb://APPOINTMENT-SERVICE`.
   - `appointment-service` sử dụng `RestTemplate` kèm `@LoadBalanced` để tự động khám phá và cân bằng tải khi gọi `http://patient-service/...` và `http://doctor-service/...`.
3. **Giám sát trạng thái (Heartbeat & Health Check):**
   - Định kỳ nhận heartbeat từ các service con. Nếu một instance bị dừng hoặc sập, Eureka sẽ cập nhật trạng thái và loại bỏ khỏi bảng điều hướng sau một khoảng thời gian chờ.

---

## 3. Cấu trúc thư mục

```text
medical-discovery-server/
├── src/main/java/com/example/medicaldiscoveryserver/
│   └── MedicalDiscoveryServerApplication.java  # Đánh dấu @EnableEurekaServer
└── src/main/resources/
    └── application.yaml                        # Cấu hình cổng 8761 và tắt tự đăng ký
```

---

## 4. Cấu hình chi tiết (`application.yaml`)

```yaml
spring:
  application:
    name: medical-discovery-server

server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

---

## 5. Hướng dẫn khởi chạy & Kiểm tra

### Yêu cầu môi trường
- Java Development Kit (JDK) 21 trở lên

### Lệnh khởi chạy
Discovery Server là dịch vụ **cần được khởi động đầu tiên** trong toàn bộ hệ thống:

```bash
cd medical-discovery-server
./gradlew bootRun
```

### Kiểm tra Dashboard
Sau khi khởi động thành công, mở trình duyệt và truy cập:
`http://localhost:8761`

Bảng danh sách **Instances currently registered with Eureka** sẽ hiển thị các dịch vụ đăng ký thành công:
- `MEDICAL-GATEWAY` (Port 8080)
- `PATIENT-SERVICE` (Port 8081)
- `DOCTOR-SERVICE` (Port 8082)
- `APPOINTMENT-SERVICE` (Port 8083)
