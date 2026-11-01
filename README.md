# Nền tảng quản lý Freelance / Marketplace việc làm Part-time cho Sinh viên

- **Môn học**: Chuyên đề tốt nghiệp 2 (Track Công nghệ phần mềm - SE)
- **Giảng viên hướng dẫn**: ThS. Hà Đồng Hưng
- **Nhóm thực hiện (Nhóm 5)**:
  - **Bùi Đăng Khoa** (`2374802013428`) — Backend 1 (Spring Security, JWT, Auth, Profile, Review, Notification)
  - **Đoàn Xuân Tiến** (`2374802010493`) — Backend 2 (Database, Jobs, Applications, Full-text Search)
  - **Viên Thành Đạt** (`2374802010107`) — Frontend (ReactJS, Tailwind CSS, Axios Client, Tích hợp UI 3 trạng thái)

---

## 1. Cấu trúc thư mục dự án (Buổi 06 - Sprint 1)

```text
cdtn2-Nhom5/
├── backend/                  # Spring Boot 3.x (Java 17) + Spring Security + JPA + MySQL 8.0
│   ├── src/main/java/com/marketplace/
│   │   ├── controllers/      # AuthController, ProfileController, CatalogController
│   │   ├── services/         # AuthService, ProfileService, CatalogService
│   │   ├── repositories/     # UserRepository, ProfileRepository, CategoryRepository, LocationRepository
│   │   ├── entities/         # User, Profile, Category, Location, UserRole, UserStatus
│   │   ├── dtos/             # ApiResponse, RegisterRequest, LoginRequest, AuthResponse, ProfileResponse...
│   │   ├── security/         # SecurityConfig, JwtAuthenticationFilter, JwtProvider, UserPrincipal
│   │   └── exceptions/       # AppException, GlobalExceptionHandler (@RestControllerAdvice)
│   └── src/main/resources/
│       ├── schema.sql        # Chuẩn hóa 8 bảng MySQL 8.0 (InnoDB, utf8mb4)
│       ├── data.sql          # Dữ liệu mẫu: 8 ngành nghề, 10 địa điểm, 6 tài khoản kiểm thử
│       └── application.properties
└── frontend/                 # ReactJS 18 (Vite) + Tailwind CSS + Axios Interceptor
    └── src/
        ├── services/         # axiosClient.js, authService.js, profileService.js
        ├── contexts/         # AuthContext.jsx
        ├── components/       # Navbar.jsx
        └── pages/            # RegisterPage.jsx (/register), LoginPage.jsx (/login), ProfilePage.jsx (/profile)
```

---

## 2. Tài khoản mẫu kiểm thử (Seed Data trong `data.sql`)

Tất cả tài khoản mẫu đều được băm mật khẩu bằng **BCrypt** với mật khẩu chung là: `Abc@12345`

| Vai trò (Role) | Email đăng nhập | Họ và tên | Mật khẩu |
| :--- | :--- | :--- | :--- |
| **STUDENT** | `sv1@vanlanguni.vn` | Lê Hoàng Nam | `Abc@12345` |
| **STUDENT** | `sv2@vanlanguni.vn` | Phạm Minh Châu | `Abc@12345` |
| **STUDENT** | `sv3@vanlanguni.vn` | Võ Quốc Bảo | `Abc@12345` |
| **EMPLOYER** | `hr@cafegocpho.vn` | Nguyễn Minh Hoàng (Cafe Góc Phố) | `Abc@12345` |
| **EMPLOYER** | `tuyendung@eduviet.vn` | Trần Thu Hà (Trung tâm EduViet) | `Abc@12345` |
| **ADMIN** | `admin@vlu.edu.vn` | Quản trị viên Hệ thống | `Abc@12345` |

---

## 3. Hướng dẫn khởi chạy & Kiểm thử End-to-End

### Bước 1: Khởi chạy Backend (Spring Boot)
- **Chạy với MySQL 8.0** (mặc định tại `localhost:3306/parttime_db`):
  ```bash
  cd backend
  ./mvnw spring-boot:run
  ```
- **Chạy chế độ độc lập Offline (Plan B khi máy chưa bật dịch vụ MySQL)**:
  ```bash
  cd backend
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
  ```
- Tài liệu Swagger UI: `http://localhost:8080/swagger-ui.html`

### Bước 2: Khởi chạy Frontend (ReactJS + Vite)
```bash
cd frontend
npm install
npm run dev
```
- Giao diện Web chạy tại: `http://localhost:5173`

### Bước 3: Chạy kiểm thử tự động (Unit & Integration Tests)
```bash
cd backend
./mvnw test
```
