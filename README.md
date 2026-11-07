# Nền tảng quản lý Freelance / Marketplace việc làm Part-time cho Sinh viên

- **Môn học**: Chuyên đề tốt nghiệp 2 (Track Công nghệ phần mềm - SE)
- **Giảng viên hướng dẫn**: ThS. Hà Đồng Hưng
- **Nhóm thực hiện (Nhóm 5)**:
  - **Bùi Đăng Khoa** (`2374802013428`) — Backend 1 (Spring Security, JWT, Auth, Profile, Review, Notification)
  - **Đoàn Xuân Tiến** (`2374802010493`) — Backend 2 (Database, Jobs, Applications, Full-text Search, Admin)
  - **Viên Thành Đạt** (`2374802010107`) — Frontend (ReactJS 18, Tailwind CSS 3.4, Axios Client, Admin Dashboard & UI 3 trạng thái)

---

## 1. Kiến trúc & Cấu trúc thư mục dự án (Sprint 1 – Sprint 5)

```text
cdtn2-Nhom5/
├── backend/                  # Spring Boot 3.4 (Java 17) + Spring Security 6 + JPA + MySQL 8.0
│   ├── Dockerfile            # Multi-stage build (Maven JDK 17 -> Eclipse Temurin JRE 17)
│   ├── src/main/java/com/marketplace/
│   │   ├── controllers/      # AuthController, ProfileController, CatalogController, JobController, ApplicationController, ReviewController, NotificationController, AdminController
│   │   ├── services/         # AuthService, ProfileService, CatalogService, JobService, ApplicationService, ReviewService, NotificationService, AdminService
│   │   ├── repositories/     # 8 Spring Data JPA Repositories tương ứng 8 bảng
│   │   ├── entities/         # User, Profile, Category, Location, Job, JobApplication, Review, Notification + Enums
│   │   ├── dtos/             # ApiResponse, PageResponse, Request/Response DTOs
│   │   ├── security/         # SecurityConfig, JwtAuthenticationFilter, JwtProvider, UserPrincipal
│   │   └── exceptions/       # AppException, GlobalExceptionHandler (@RestControllerAdvice)
│   └── src/main/resources/
│       ├── schema.sql        # Chuẩn hóa 8 bảng MySQL 8.0 (InnoDB, utf8mb4, ON DELETE RESTRICT, FULLTEXT)
│       ├── data.sql          # Dữ liệu mẫu: 8 ngành nghề, 10 địa điểm, 6 tài khoản, 4 việc làm, 1 đơn ứng tuyển
│       └── application.properties
├── frontend/                 # ReactJS 18 (Vite) + React Router v6 + Tailwind CSS 3.4 + Lucide Icons + Axios 1.6
│   ├── Dockerfile            # Multi-stage build (Node 20 -> Nginx 1.27 Alpine)
│   ├── nginx.conf            # Cấu hình SPA fallback và reverse-proxy /api/v1 sang backend
│   └── src/
│       ├── services/         # axiosClient.js, authService.js, profileService.js, jobService.js, reviewService.js, notificationService.js, adminService.js
│       ├── contexts/         # AuthContext.jsx
│       ├── components/       # Navbar.jsx, NotificationBell.jsx (polling 30s), JobCard.jsx, FilterBar.jsx, ReviewModal.jsx
│       └── pages/            # RegisterPage.jsx, LoginPage.jsx, ProfilePage.jsx, JobListPage.jsx, JobDetailPage.jsx, JobFormPage.jsx, EmployerJobsPage.jsx, JobApplicantsPage.jsx, MyApplicationsPage.jsx, AdminDashboardPage.jsx
├── database/                 # schema.sql + seed.sql (8 bảng chuẩn MySQL 8.0)
├── docs/                     # Tài liệu thiết kế, Postman Collection (Sprint 2 -> 5), Benchmark NFR-01
└── docker-compose.yml        # Đóng gói 3 dịch vụ: mysql (8.0), backend (8080), frontend (5173 -> 80)
```

---

## 2. Tài khoản Demo kèm Dữ liệu mẫu (`database/seed.sql` & `backend/src/main/resources/data.sql`)

Tất cả tài khoản mẫu đều được băm mật khẩu bằng **BCrypt** với mật khẩu chung là: **`Abc@12345`**

| Vai trò (Role) | Email đăng nhập | Họ và tên / Đơn vị | Mật khẩu | Ghi chú dữ liệu mẫu |
| :--- | :--- | :--- | :--- | :--- |
| **STUDENT (Demo chính)** | `sv1@vanlanguni.vn` | Lê Hoàng Nam | `Abc@12345` | Đã có CV đầy đủ và 1 đơn ứng tuyển `PENDING` vào Tin #1 |
| **STUDENT** | `sv2@vanlanguni.vn` | Phạm Minh Châu | `Abc@12345` | Đã có CV đầy đủ, sẵn sàng ứng tuyển mới |
| **STUDENT** | `sv3@vanlanguni.vn` | Võ Quốc Bảo | `Abc@12345` | Đã có CV đầy đủ, sẵn sàng kiểm thử khóa/mở khóa tài khoản |
| **EMPLOYER (Demo chính)** | `hr@cafegocpho.vn` | Nguyễn Minh Hoàng (Cafe Góc Phố) | `Abc@12345` | Sở hữu Tin #1 (`slots=2`) và Tin #3 (`slots=1`) |
| **EMPLOYER** | `tuyendung@eduviet.vn` | Trần Thu Hà (Trung tâm EduViet) | `Abc@12345` | Sở hữu Tin #2 (`slots=3`) và Tin #4 (`slots=2`) |
| **ADMIN (Demo chính)** | `admin@vlu.edu.vn` | Quản trị viên Hệ thống | `Abc@12345` | Toàn quyền truy cập `/admin` (`FR-25` → `FR-31`) |

---

## 3. Hướng dẫn Cài đặt & Khởi chạy

### Cách 1: Chạy trọn bộ bằng Docker Compose (Khuyên dùng khi đóng gói / nghiệm thu)
Tại thư mục gốc `cdtn2-Nhom5`:
```bash
docker compose up -d --build
```
- **Giao diện Frontend (Nginx)**: `http://localhost:5173`
- **Backend API**: `http://localhost:8080/api/v1`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **MySQL 8.0**: cổng `3306` (tự động nạp `database/schema.sql` và `database/seed.sql`).

### Cách 2: Chạy trực tiếp trên máy phát triển (Local Development)

#### Bước 1: Khởi chạy Backend (Spring Boot)
- **Chạy với MySQL 8.0** (mặc định tại `localhost:3306/parttime_db`):
  ```bash
  cd backend
  ./mvnw spring-boot:run
  ```
- **Chạy chế độ độc lập Offline (Plan B với H2 MySQL-mode khi chưa bật MySQL)**:
  ```bash
  cd backend
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
  ```

#### Bước 2: Khởi chạy Frontend (ReactJS + Vite)
```bash
cd frontend
npm install
npm run dev
```
- Giao diện Web chạy tại: `http://localhost:5173`

#### Bước 3: Chạy bộ kiểm thử tự động (36 bài kiểm thử Unit & Integration)
```bash
cd backend
./mvnw test
```

---

## 4. Tài liệu Postman Collection
- `docs/postman-sprint5-admin-full.json`: Collection tổng hợp toàn bộ API từ Sprint 1 đến Sprint 5 (Auth, Profile, Jobs, Applications, Reviews, Notifications, Admin Users/Jobs/Catalogs/Reviews/Stats).
- `docs/postman-sprint2-auth-profile.json`: Collection chi tiết Sprint 2.
- `docs/postman-sprint3-jobs-applications.json`: Collection chi tiết Sprint 3.

---

## 5. Danh sách Hạn chế kỹ thuật đã biết (Known Limitations)

1. **Lưu trữ JWT trong `localStorage`**:
   - Hiện tại Frontend lưu `accessToken` trong `localStorage` (`vlu_parttime_token`) để gắn vào header `Authorization: Bearer <JWT>` qua Axios Interceptor. Cách tiếp cận này đơn giản cho kiến trúc SPA stateless nhưng có rủi ro nếu ứng dụng bị tấn công XSS. Hướng nâng cấp trong tương lai là chuyển sang `HttpOnly, Secure, SameSite=Strict Cookie` kết hợp CSRF token.
2. **Tìm kiếm toàn văn (`MySQL FULLTEXT`) với từ tiếng Việt ngắn**:
   - InnoDB FULLTEXT mặc định của MySQL 8.0 có cấu hình `innodb_ft_min_token_size = 3`, dẫn đến các từ tiếng Việt đơn âm tiết dưới 3 ký tự (như `"ca"`, `"in"`, `"vẽ"`) không được lập chỉ mục trong chế độ `NATURAL LANGUAGE MODE`. Hệ thống đã giảm thiểu bằng cơ chế tự động fallback sang `JPA Specification LIKE` khi từ khóa ngắn `< 3` ký tự hoặc khi chạy trên H2; hướng tối ưu triệt để trên MySQL sản xuất là bật `WITH PARSER ngram` (`ngram_token_size = 2`).
3. **Kênh thông báo chỉ hoạt động nội bộ (In-App Polling)**:
   - Hệ thống thông báo (`tbl_notifications` + chuông thông báo polling mỗi 30 giây trên thanh điều hướng) chỉ gửi thông báo bên trong ứng dụng Web, **chưa tích hợp dịch vụ gửi Email (SMTP/SendGrid) hoặc WebSocket/Push Notification thời gian thực**.
4. **Hồ sơ CV dạng văn bản (`cv_content` TEXT)**:
   - Để giữ kiến trúc gọn nhẹ trong phạm vi môn học, CV của sinh viên được lưu trực tiếp dưới dạng văn bản có cấu trúc trong cột `cv_content` thay vì tải tệp PDF/DOCX lên dịch vụ lưu trữ đám mây (S3/Cloudinary).
