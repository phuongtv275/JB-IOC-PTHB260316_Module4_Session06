# Doctor Service (Dịch vụ quản lý thông tin bác sĩ)

`doctor-service` là một microservice trong hệ thống y tế, chịu trách nhiệm quản lý hồ sơ thông tin bác sĩ, chuyên khoa, số năm kinh nghiệm, email công việc và trạng thái làm việc.

---

## 1. Thông tin tổng quan

- **Cổng dịch vụ (Port):** `8082`
- **Tên đăng ký Eureka (Service ID):** `DOCTOR-SERVICE`
- **Cơ sở dữ liệu:** PostgreSQL (`doctor_db`)
- **Kiến trúc:** Spring Web MVC, Clean Code, tuân thủ nguyên lý SOLID
- **Mapping DTO:** MapStruct 1.6.3 (Compile-time code generation)
- **Tracing:** SLF4J MDC + Correlation ID (`X-Correlation-Id`)
- **Dữ liệu mẫu:** Tự động khởi tạo dữ liệu ban đầu qua `DataInitializer`

---

## 2. Cấu trúc thư mục

```text
doctor-service/
├── src/main/java/com/example/doctorservice/
│   ├── config/
│   │   └── DataInitializer.java            # Khởi tạo 5 bác sĩ mẫu khi DB trống
│   ├── controller/
│   │   └── DoctorController.java           # REST API endpoints cho Doctor
│   ├── dto/
│   │   ├── request/
│   │   │   └── DoctorRequest.java          # DTO đầu vào kèm Jakarta Validation
│   │   └── response/
│   │       ├── ApiResponse.java            # Envelope chuẩn hóa dữ liệu trả về
│   │       ├── PageResponse.java           # DTO chuẩn phân trang
│   │       └── DoctorResponse.java         # DTO thông tin bác sĩ (tóm tắt / chi tiết)
│   ├── entity/
│   │   └── Doctor.java                     # JPA Entity tương ứng bảng doctors
│   ├── exception/
│   │   ├── BadRequestException.java        # HTTP 400
│   │   ├── DuplicateResourceException.java # HTTP 409
│   │   ├── ResourceNotFoundException.java  # HTTP 404
│   │   └── GlobalExceptionHandler.java     # @RestControllerAdvice xử lý ngoại lệ tập trung
│   ├── filter/
│   │   └── CorrelationIdFilter.java        # Bắt/sinh X-Correlation-Id đưa vào MDC
│   ├── mapper/
│   │   └── DoctorMapper.java               # MapStruct interface
│   ├── repository/
│   │   └── DoctorRepository.java           # Spring Data JPA repository + JpaSpecificationExecutor
│   └── service/
│       ├── DoctorService.java              # Interface nghiệp vụ bác sĩ
│       └── impl/
│           └── DoctorServiceImpl.java      # Triển khai nghiệp vụ, phân trang & logging
└── src/main/resources/
    └── application.yaml                    # Cấu hình cổng, Eureka, Datasource, Logging
```

---

## 3. Cấu trúc thực thể `Doctor`

| Thuộc tính | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Primary Key, Identity | Mã định danh duy nhất của bác sĩ |
| `name` | `String` | Not Blank, Max 150 | Họ và tên bác sĩ |
| `specialization` | `String` | Not Blank, Max 100 | Chuyên khoa (Nội khoa, Ngoại khoa, Nhi khoa,...) |
| `experienceYears` | `Integer` | Not Null, Min 0 | Số năm kinh nghiệm công tác |
| `email` | `String` | Not Blank, Email, Unique | Email công việc |
| `status` | `Boolean` | Not Null | Trạng thái (`true`: Đang làm việc, `false`: Nghỉ phép) |
| `createdAt` | `LocalDateTime` | Not Null, Không cập nhật | Thời điểm tạo bản ghi |
| `updatedAt` | `LocalDateTime` | Cập nhật tự động | Thời điểm cập nhật bản ghi gần nhất |

---

## 4. Danh sách API Endpoints

### 4.1. Lấy danh sách bác sĩ (Phân trang, Lọc & Tóm tắt)
- **Phương thức:** `GET`
- **Đường dẫn:** `/api/v1/doctors`
- **Mô tả:** Theo yêu cầu bài toán, API mặc định trả về danh sách bác sĩ với các thuộc tính: `id`, `name`, `specialization`. Nếu truyền tham số `detail=true`, API sẽ trả về thông tin đầy đủ gồm cả kinh nghiệm, email, trạng thái làm việc và thời gian tạo.
- **Query Parameters:**
  - `page`: Chỉ số trang (bắt đầu từ `0`, mặc định: `0`)
  - `size`: Kích thước trang (mặc định: `10`, tối đa `100`)
  - `sortBy`: Trường sắp xếp (`id`, `name`, `specialization`, `experienceYears`, `createdAt`..., mặc định `id`)
  - `sortDir`: Chiều sắp xếp (`asc` hoặc `desc`, mặc định `asc`)
  - `specialization`: Lọc chính xác theo chuyên khoa (tùy chọn)
  - `status`: Lọc theo trạng thái làm việc (tùy chọn: `true` / `false`)
  - `detail`: Xem chi tiết (`true`) hoặc xem tóm tắt (`false`, mặc định)
- **Response mặc định (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy danh sách bác sĩ thành công",
  "data": {
    "items": [
      {
        "id": 1,
        "name": "BS. Nguyễn Văn Hùng",
        "specialization": "Nội khoa"
      },
      {
        "id": 2,
        "name": "BS. Trần Thị Mai",
        "specialization": "Ngoại khoa"
      },
      {
        "id": 3,
        "name": "BS. Lê Hoàng Long",
        "specialization": "Nhi khoa"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 5,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false,
    "first": true,
    "last": true
  },
  "correlationId": "58c3dbcb-b7b5-4b10-85f2-ecbfbbdcece2",
  "timestamp": "2026-09-14T11:00:00"
}
```

### 4.2. Lấy thông tin chi tiết bác sĩ theo ID
- **Phương thức:** `GET`
- **Đường dẫn:** `/api/v1/doctors/{id}`
- **Response (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Lấy thông tin bác sĩ thành công",
  "data": {
    "id": 1,
    "name": "BS. Nguyễn Văn Hùng",
    "specialization": "Nội khoa",
    "experienceYears": 15,
    "email": "hung.nguyen@hospital.com",
    "status": true,
    "createdAt": "2026-09-14T11:00:00",
    "updatedAt": "2026-09-14T11:00:00"
  },
  "correlationId": "...",
  "timestamp": "..."
}
```
- **Response lỗi (HTTP 404 Not Found):** Trả về khi không tìm thấy bác sĩ theo ID cung cấp.

### 4.3. Thêm mới bác sĩ
- **Phương thức:** `POST`
- **Đường dẫn:** `/api/v1/doctors`
- **Request Body mẫu:**
```json
{
  "name": "BS. Đỗ Minh Quân",
  "specialization": "Tai Mũi Họng",
  "experienceYears": 8,
  "email": "quan.do@hospital.com",
  "status": true
}
```
- **Response (HTTP 201 Created):**
```json
{
  "success": true,
  "message": "Thêm mới bác sĩ thành công",
  "data": {
    "id": 6,
    "name": "BS. Đỗ Minh Quân",
    "specialization": "Tai Mũi Họng",
    "experienceYears": 8,
    "email": "quan.do@hospital.com",
    "status": true,
    "createdAt": "2026-09-14T11:05:00",
    "updatedAt": "2026-09-14T11:05:00"
  },
  "correlationId": "...",
  "timestamp": "..."
}
```

---

## 5. Hướng dẫn khởi chạy & Kiểm thử

### Yêu cầu môi trường
- Java Development Kit (JDK) 21 trở lên
- PostgreSQL (Cơ sở dữ liệu `doctor_db` trên port `5432`)
- Discovery Server chạy sẵn tại `http://localhost:8761/eureka/`

### Lệnh khởi chạy ứng dụng
```bash
cd doctor-service
./gradlew bootRun
```

### Chạy kiểm thử tự động
```bash
cd doctor-service
./gradlew test
```
*(Bao gồm bộ 13 test case kiểm thử toàn diện Repository, MapStruct Mapper, Service Logic và REST Controller).*
