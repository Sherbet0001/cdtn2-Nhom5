# Tài liệu Kiểm thử API (cURL) & Ghi chú Kỹ thuật — Sprint 2 (Auth & Profile)

## 1. Ví dụ lệnh `curl` kiểm thử các API Sprint 2 (`FR-01, 02, 03, 04, 05, 11, 16`)

### 1.1. Đăng ký tài khoản Sinh viên mới (`POST /api/v1/auth/register` — `201 Created`)
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Trần Minh Khoa",
    "email": "sv.moi@vanlanguni.vn",
    "password": "Abc@12345",
    "role": "STUDENT"
  }'
```

### 1.2. Đăng nhập nhận JWT Bearer Token 2 giờ (`POST /api/v1/auth/login` — `200 OK`)
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "sv1@vanlanguni.vn",
    "password": "Abc@12345"
  }'
```

### 1.3. Xem hồ sơ cá nhân đang đăng nhập (`GET /api/v1/profiles/me` — `200 OK`)
```bash
curl -X GET http://localhost:8080/api/v1/profiles/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

### 1.4. Sinh viên cập nhật CV & học vấn (`PUT /api/v1/profiles/me` — `FR-11`)
```bash
curl -X PUT http://localhost:8080/api/v1/profiles/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <STUDENT_ACCESS_TOKEN>" \
  -d '{
    "fullName": "Lê Hoàng Nam",
    "phone": "0914333444",
    "locationId": 5,
    "bio": "Sinh viên năm 3 ngành Công nghệ phần mềm",
    "school": "Đại học Văn Lang",
    "major": "Công nghệ phần mềm",
    "cvContent": "Kỹ năng: Java, Spring Boot, ReactJS, MySQL. Rảnh các ca tối trong tuần."
  }'
```

### 1.5. Nhà tuyển dụng cập nhật thông tin đơn vị (`PUT /api/v1/profiles/me` — `FR-16`)
```bash
curl -X PUT http://localhost:8080/api/v1/profiles/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <EMPLOYER_ACCESS_TOKEN>" \
  -d '{
    "fullName": "Nguyễn Minh Hoàng",
    "phone": "0902111222",
    "locationId": 3,
    "bio": "Chuỗi cà phê acoustic dành cho sinh viên",
    "companyName": "Cafe Góc Phố"
  }'
```

### 1.6. Nhà tuyển dụng / Admin xem hồ sơ ứng viên (`GET /api/v1/profiles/{userId}`)
- NTD `hr@cafegocpho.vn` (`userId=2`) xem hồ sơ SV `sv1@vanlanguni.vn` (`userId=4`, đã nộp đơn vào tin `job_id=1` của NTD) -> **`200 OK`**:
```bash
curl -X GET http://localhost:8080/api/v1/profiles/4 \
  -H "Authorization: Bearer <EMPLOYER_ACCESS_TOKEN>"
```
- NTD `hr@cafegocpho.vn` (`userId=2`) xem hồ sơ SV `sv2@vanlanguni.vn` (`userId=5`, chưa ứng tuyển vào tin của NTD) -> **`403 Forbidden`**:
```bash
curl -X GET http://localhost:8080/api/v1/profiles/5 \
  -H "Authorization: Bearer <EMPLOYER_ACCESS_TOKEN>"
```

### 1.7. Đăng xuất (`POST /api/v1/auth/logout` — `204 No Content`)
```bash
curl -i -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

## 2. Ghi chú Giả định & Bảo mật đã áp dụng trong Sprint 2

1. **Lưu trữ JWT tại `localStorage` & Biện pháp hạn chế XSS**:
   - Phía Frontend (`ReactJS`) lưu `accessToken` trong `localStorage` (`vlu_parttime_token`) để tự động đính kèm qua Axios Request Interceptor.
   - **Hạn chế rủi ro XSS**:
     - Token có thời hạn ngắn cố định **2 giờ (`7200` giây)**.
     - Toàn bộ dữ liệu văn bản người dùng nhập (`bio`, `cvContent`, `fullName`, `companyName`) được render qua cơ chế tự động escape JSX của React (tuyệt đối không sử dụng `dangerouslySetInnerHTML`).
     - Khi nhận phản hồi `401 Unauthorized` (token hết hạn/không hợp lệ) hoặc khi người dùng bấm **Đăng xuất**, `AuthContext` lập tức xóa sạch `vlu_parttime_token` và `vlu_parttime_user` khỏi `localStorage`.
2. **Kiểm tra quyền sở hữu hai tầng trên `GET /api/v1/profiles/{userId}`**:
   - **Tầng 1 (Spring Security RBAC)**: Chỉ cho phép `ROLE_EMPLOYER` và `ROLE_ADMIN` gọi endpoint (`ROLE_STUDENT` bị chặn ngay với `403 Forbidden`).
   - **Tầng 2 (Service Ownership Check)**: Nếu người gọi là `EMPLOYER`, `ProfileService` truy vấn bảng `tbl_applications` kết nối `tbl_jobs` để xác nhận sinh viên `userId` đã từng ứng tuyển vào ít nhất 1 công việc do chính `EMPLOYER` đó đăng tải; nếu không thỏa mãn sẽ trả về `403 Forbidden`.
3. **Phạm vi chưa thực hiện (Thuộc các Sprint tiếp theo)**:
   - Module Quản lý Tin tuyển dụng & Tìm kiếm/Lọc việc làm (`JobController` – `JobService`) sẽ thực hiện ở Sprint 3.
   - Module Ứng tuyển, Đánh giá hai chiều, Thông báo và Quản trị Admin (`Application`, `Review`, `Notification`, `Admin`) sẽ thực hiện ở các Sprint kế tiếp theo đúng lộ trình.

