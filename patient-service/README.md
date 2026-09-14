# Patient Service (Dịch vụ quản lý bệnh nhân)

`patient-service` là một microservice trong hệ thống y tế/quản lý bệnh viện, chịu trách nhiệm quản lý hồ sơ thông tin bệnh nhân, tiền sử bệnh lý và thông tin liên lạc.

---

## 1. Thông tin tổng quan

- **Cổng dịch vụ (Port):** `8081`
- **Tên đăng ký Eureka (Service ID):** `PATIENT-SERVICE`
- **Cơ sở dữ liệu:** PostgreSQL (`patient_db`)
- **Kiến trúc:** Spring Web MVC, Clean Code, tuân thủ nguyên lý SOLID
- **Mapping DTO:** MapStruct 1.6.3 (Compile-time code generation)
- **Tracing:** SLF4J MDC + Correlation ID (`X-Correlation-Id`)

---

## 2. Cấu trúc thư mục

```text
patient-service/
├── src/main/java/com/example/patientservice/
│   ├── controller/
│   │   └── PatientController.java          # REST API endpoints
│   ├── dto/
│   │   ├── request/
│   │   │   └── PatientRequest.java         # DTO đầu vào kèm Jakarta Validation
│   │   └── response/
│   │       ├── ApiResponse.java            # Envelope chuẩn hóa dữ liệu trả về
│   │       ├── PageResponse.java           # DTO phân trang
│   │       └── PatientResponse.java        # DTO thông tin bệnh nhân
│   ├── entity/
│   │   └── Patient.java                    # JPA Entity tương ứng bảng patients
│   ├── exception/
│   │   ├── BadRequestException.java        # HTTP 400
│   │   ├── DuplicateResourceException.java # HTTP 409
│   │   ├── ResourceNotFoundException.java  # HTTP 404
│   │   └── GlobalExceptionHandler.java     # @RestControllerAdvice
│   ├── filter/
│   │   └── CorrelationIdFilter.java        # Bắt/sinh X-Correlation-Id đưa vào MDC
│   ├── mapper/
│   │   └── PatientMapper.java              # MapStruct interface
│   ├── repository/
│   │   └── PatientRepository.java          # Spring Data JPA repository
│   └── service/
│       ├── PatientService.java             # Interface nghiệp vụ
│       └── impl/
│           └── PatientServiceImpl.java     # Triển khai nghiệp vụ & logging
└── src/main/resources/
    └── application.yaml                    # Cấu hình cổng, Eureka, Datasource, Logging
```

---

## 3. Cấu trúc thực thể `Patient`

| Thuộc tính | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Primary Key, Identity | Mã định danh duy nhất của bệnh nhân |
| `fullName` | `String` | Not Blank, Max 150 | Họ và tên bệnh nhân |
| `dateOfBirth` | `LocalDate` | Not Null, Quá khứ | Ngày sinh |
| `gender` | `String` | Not Blank (Nam/Nữ/Khác) | Giới tính |
| `phoneNumber` | `String` | Not Blank, Unique, Định dạng VN | Số điện thoại liên hệ |
| `address` | `String` | Not Blank, Max 255 | Địa chỉ thường trú |
| `medicalHistory`| `String` | Kiểu `TEXT` | Tiền sử bệnh lý (dị ứng thuốc, bệnh nền) |
| `createdAt` | `LocalDateTime` | Not Null, Không cập nhật | Thời điểm tạo bản ghi |
| `updatedAt` | `LocalDateTime` | Cập nhật tự động | Thời điểm sửa đổi gần nhất |

---

## 4. Danh sách API Endpoints

### 4.1. Thêm mới bệnh nhân
- **Phương thức:** `POST`
- **Đường dẫn:** `/api/v1/patients`
- **Headers:** `Content-Type: application/json`, `X-Correlation-Id: <uuid>` (tùy chọn)
- **Request Body mẫu:**
```json
{
  "fullName": "Nguyễn Văn An",
  "dateOfBirth": "1992-08-15",
  "gender": "Nam",
  "phoneNumber": "0981234567",
  "address": "Số 10 Tràng Thi, Hoàn Kiếm, Hà Nội",
  "medicalHistory": "Dị ứng Penicillin, có bệnh nền huyết áp nhẹ"
}
```
- **Response (HTTP 201 Created):**
```json
{
  "success": true,
  "message": "Thêm mới bệnh nhân thành công",
  "data": {
    "id": 1,
    "fullName": "Nguyễn Văn An",
    "dateOfBirth": "1992-08-15",
    "gender": "Nam",
    "phoneNumber": "0981234567",
    "address": "Số 10 Tràng Thi, Hoàn Kiếm, Hà Nội",
    "medicalHistory": "Dị ứng Penicillin, có bệnh nền huyết áp nhẹ",
    "createdAt": "2026-09-14T10:51:01.800175",
    "updatedAt": "2026-09-14T10:51:01.800205"
  },
  "correlationId": "custom-cid-12345",
  "timestamp": "2026-09-14T10:51:01.842361"
}
```

### 4.2. Lấy danh sách bệnh nhân (Phân trang & Sắp xếp)
- **Phương thức:** `GET`
- **Đường dẫn:** `/api/v1/patients`
- **Query Parameters:**
  - `page`: Chỉ số trang (bắt đầu từ `0`, mặc định `0`)
  - `size`: Số phần tử trên trang (mặc định `10`, tối đa `100`)
  - `sortBy`: Cột sắp xếp (`id`, `fullName`, `dateOfBirth`, `createdAt`..., mặc định `id`)
  - `sortDir`: Chiều sắp xếp (`asc` hoặc `desc`, mặc định `desc`)
- **Response (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy danh sách bệnh nhân thành công",
  "data": {
    "items": [...],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 2,
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

### 4.3. Lấy chi tiết bệnh nhân theo ID
- **Phương thức:** `GET`
- **Đường dẫn:** `/api/v1/patients/{id}`
- **Response (HTTP 200 OK):** Trả về thông tin chi tiết bệnh nhân.
- **Response (HTTP 404 Not Found):** Nếu không tìm thấy ID.

---

## 5. Hướng dẫn khởi chạy & Kiểm thử

### Yêu cầu môi trường
- JDK 21
- PostgreSQL (Cơ sở dữ liệu `patient_db`)
- Eureka Server chạy tại `http://localhost:8761/eureka/`

### Lệnh khởi chạy
```bash
cd patient-service
./gradlew bootRun
```

### Chạy kiểm thử tự động
```bash
./gradlew test
```
