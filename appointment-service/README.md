# Appointment Service (Dịch vụ đặt và quản lý lịch khám)

`appointment-service` là một microservice trong hệ thống y tế, chịu trách nhiệm quản lý quy trình tạo mới, tìm kiếm và tra cứu lịch hẹn khám bệnh giữa bệnh nhân và bác sĩ.

---

## 1. Thông tin tổng quan

- **Cổng dịch vụ (Port):** `8083`
- **Tên đăng ký Eureka (Service ID):** `APPOINTMENT-SERVICE`
- **Cơ sở dữ liệu:** PostgreSQL (`appointment_db`)
- **Kiến trúc:** Spring Web MVC, Clean Code, tuân thủ nguyên lý SOLID
- **Mapping DTO:** MapStruct 1.6.3 (Compile-time code generation)
- **Giao tiếp liên dịch vụ:** Spring `RestTemplate` kết hợp `@LoadBalanced` để phân giải Eureka Service ID
- **Khả năng chịu lỗi (Resilience & Fault Tolerance):** Bọc khối `try-catch` quanh các lời gọi API liên dịch vụ, chuyển đổi lỗi sập hệ thống thành định dạng phản hồi chuẩn `ApiResponseError` (HTTP 503 Service Unavailable)
- **Tracing:** SLF4J MDC + Correlation ID (`X-Correlation-Id`)

---

## 2. Cấu trúc thư mục

```text
appointment-service/
├── src/main/java/com/example/appointmentservice/
│   ├── config/
│   │   └── RestTemplateConfig.java         # Khai báo RestTemplate với annotation @LoadBalanced
│   ├── controller/
│   │   └── AppointmentController.java      # REST API endpoints (/api/v1/appointments và /api/v1/appointment)
│   ├── dto/
│   │   ├── request/
│   │   │   └── AppointmentRequest.java     # DTO tạo lịch hẹn kèm Jakarta Validation
│   │   └── response/
│   │       ├── ApiResponse.java            # Envelope dữ liệu thành công chuẩn hóa
│   │       ├── ApiResponseError.java       # Envelope báo lỗi chuẩn theo yêu cầu dự án
│   │       ├── PageResponse.java           # DTO chuẩn phân trang
│   │       └── AppointmentResponse.java    # DTO thông tin lịch hẹn
│   ├── entity/
│   │   └── Appointment.java                # JPA Entity tương ứng bảng appointments
│   ├── exception/
│   │   ├── BadRequestException.java        # HTTP 400
│   │   ├── ResourceNotFoundException.java  # HTTP 404
│   │   ├── ServiceUnavailableException.java# HTTP 503
│   │   └── GlobalExceptionHandler.java     # @RestControllerAdvice xử lý ngoại lệ tập trung
│   ├── filter/
│   │   └── CorrelationIdFilter.java        # Bắt/sinh X-Correlation-Id đưa vào MDC
│   ├── mapper/
│   │   └── AppointmentMapper.java          # MapStruct interface
│   ├── repository/
│   │   └── AppointmentRepository.java      # Spring Data JPA repository + JpaSpecificationExecutor
│   └── service/
│       ├── AppointmentService.java         # Interface nghiệp vụ lịch hẹn
│       └── impl/
│           └── AppointmentServiceImpl.java # Triển khai nghiệp vụ, gọi liên dịch vụ & xử lý lỗi
└── src/main/resources/
    └── application.yaml                    # Cấu hình cổng, Eureka, Datasource, Logging
```

---

## 3. Cấu trúc thực thể `Appointment`

Theo yêu cầu đặc thù của kiến trúc Microservices, thực thể `Appointment` chỉ lưu trữ khóa ngoại định danh `patientId` và `doctorId` thay vì quan hệ trực tiếp JPA `@ManyToOne` giữa các cơ sở dữ liệu tách biệt:

| Thuộc tính | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Primary Key, Identity | Mã định danh duy nhất của lịch hẹn |
| `patientId` | `Long` | Not Null | ID của bệnh nhân (tham chiếu tới `patient-service`) |
| `doctorId` | `Long` | Not Null | ID của bác sĩ (tham chiếu tới `doctor-service`) |
| `appointmentDate`| `LocalDateTime` | Not Null, Tương lai | Ngày và giờ hẹn khám |
| `reason` | `String` | Max 500 | Lý do khám bệnh / Triệu chứng ban đầu |
| `status` | `String` | Not Blank, Max 20 | Trạng thái lịch hẹn (`PENDING`, `CONFIRMED`, `CANCELLED`) |
| `createdAt` | `LocalDateTime` | Not Null, Không cập nhật | Thời điểm tạo lịch hẹn |
| `updatedAt` | `LocalDateTime` | Cập nhật tự động | Thời điểm cập nhật lịch hẹn gần nhất |

---

## 4. Cơ chế giao tiếp liên dịch vụ & Khả năng chịu lỗi (Resilience)

### 4.1. Kiểm tra sự tồn tại của Bệnh nhân và Bác sĩ
Khi client gửi yêu cầu tạo mới lịch hẹn (`POST /api/v1/appointments`), `AppointmentServiceImpl` thực hiện:
1. Gửi request `GET http://patient-service/api/v1/patients/{id}` thông qua `RestTemplate` (@LoadBalanced) để xác thực bệnh nhân có tồn tại hay không.
2. Gửi request `GET http://doctor-service/api/v1/doctors/{id}` thông qua `RestTemplate` (@LoadBalanced) để xác thực bác sĩ có tồn tại hay không.

### 4.2. Xử lý sự cố khi Downstream Service bị sập (Chaos Engineering / Resilience Handling)
Đoạn gọi API liên dịch vụ được bọc cẩn thận bằng khối `try-catch`:
- Nếu service trả về `404 Not Found`: Ném `ResourceNotFoundException` báo rõ bệnh nhân hoặc bác sĩ không tồn tại trong hệ thống.
- **Nếu `doctor-service` bị sập do sự cố server (crash, mất mạng, timed out, 500 error):** Khối `catch (Exception ex)` sẽ bắt ngoại lệ và ném `ServiceUnavailableException`. `GlobalExceptionHandler` bắt ngoại lệ này và trả về định dạng `ApiResponseError` với HTTP 503 Service Unavailable:

```json
{
  "timestamp": "2026-09-14T11:45:10",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Hệ thống quản lý bác sĩ hiện không khả dụng. Vui lòng đặt lịch sau!"
}
```

---

## 5. Danh sách API Endpoints

### 5.1. Đặt lịch khám bệnh mới
- **Phương thức:** `POST`
- **Đường dẫn:** `/api/v1/appointments` (hỗ trợ đồng thời `/api/v1/appointment`)
- **Headers:** `Content-Type: application/json`, `X-Correlation-Id: <uuid>` (tùy chọn)
- **Request Body mẫu:**
```json
{
  "patientId": 1,
  "doctorId": 1,
  "appointmentDate": "2026-09-20T09:30:00",
  "reason": "Khám định kỳ tổng quát và kiểm tra huyết áp"
}
```
- **Response thành công (HTTP 201 Created):**
```json
{
  "success": true,
  "message": "Tạo lịch hẹn khám bệnh thành công",
  "data": {
    "id": 1,
    "patientId": 1,
    "doctorId": 1,
    "appointmentDate": "2026-09-20T09:30:00",
    "reason": "Khám định kỳ tổng quát và kiểm tra huyết áp",
    "status": "PENDING",
    "createdAt": "2026-09-14T11:30:00",
    "updatedAt": "2026-09-14T11:30:00"
  },
  "correlationId": "custom-uuid-12345",
  "timestamp": "2026-09-14T11:30:00"
}
```

- **Response khi Bác sĩ không tồn tại (HTTP 404 Not Found):**
```json
{
  "success": false,
  "message": "Không tìm thấy bác sĩ với ID: 999 trong hệ thống",
  "correlationId": "...",
  "timestamp": "..."
}
```

- **Response khi Doctor-Service bị sập (HTTP 503 Service Unavailable):**
```json
{
  "timestamp": "2026-09-14T11:45:10",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Hệ thống quản lý bác sĩ hiện không khả dụng. Vui lòng đặt lịch sau!"
}
```

### 5.2. Lấy danh sách lịch hẹn (Phân trang & Lọc)
- **Phương thức:** `GET`
- **Đường dẫn:** `/api/v1/appointments` (hoặc `/api/v1/appointment`)
- **Query Parameters:**
  - `page`: Chỉ số trang (bắt đầu từ `0`, mặc định `0`)
  - `size`: Kích thước trang (mặc định `10`, tối đa `100`)
  - `sortBy`: Cột sắp xếp (`id`, `patientId`, `doctorId`, `appointmentDate`, `status`, `createdAt`, mặc định `id`)
  - `sortDir`: Chiều sắp xếp (`asc` hoặc `desc`, mặc định `desc`)
  - `patientId`: Lọc theo ID bệnh nhân (tùy chọn)
  - `doctorId`: Lọc theo ID bác sĩ (tùy chọn)
  - `status`: Lọc theo trạng thái (`PENDING`, `CONFIRMED`, `CANCELLED`)
- **Response (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy danh sách lịch hẹn thành công",
  "data": {
    "items": [...],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false,
    "first": true,
    "last": true
  },
  "correlationId": "...",
  "timestamp": "..."
}
```

### 5.3. Lấy chi tiết lịch hẹn theo ID
- **Phương thức:** `GET`
- **Đường dẫn:** `/api/v1/appointments/{id}`
- **Response (HTTP 200 OK):** Trả về thông tin chi tiết lịch hẹn.

---

## 6. Hướng dẫn khởi chạy & Kiểm thử

### Yêu cầu môi trường
- Java Development Kit (JDK) 21
- PostgreSQL (Cơ sở dữ liệu `appointment_db` trên port `5432`)
- Discovery Server chạy sẵn tại `http://localhost:8761/eureka/`
- `patient-service` và `doctor-service` đã khởi chạy và đăng ký thành công trên Eureka

### Lệnh khởi chạy ứng dụng
```bash
cd appointment-service
./gradlew bootRun
```

### Chạy kiểm thử tự động
```bash
cd appointment-service
./gradlew test
```
*(Bao gồm bộ 13 test case kiểm thử toàn diện kịch bản tích hợp thành công, bệnh nhân không tồn tại, bác sĩ không tồn tại, và mô phỏng sự cố server sập).*
