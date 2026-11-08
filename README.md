# PartJob – Nền tảng việc làm part-time cho sinh viên (CDTN2 – Nhóm 5)

Chuyên đề tốt nghiệp 2 (ĐH Văn Lang, CNPM). Nhóm: Viên Thành Đạt, Đoàn Xuân Tiến, Bùi Đăng Khoa. GVHD: ThS. Nguyễn Văn Trung.

Tiến độ và quyết định đến hết buổi 5: xem [`docs/CHECKPOINT_Buoi05.md`](docs/CHECKPOINT_Buoi05.md).

## Công nghệ
Spring Boot 3.2 (Java 17), Spring Security + JWT, Spring Data JPA, MySQL 8, springdoc-openapi; React 18 (Vite), React Router, Axios và Tailwind CSS.

## Trạng thái Sprint buổi 6
| FR | Nội dung | Endpoint |
|---|---|---|
| 01 | Đăng ký STUDENT / EMPLOYER | `POST /api/v1/auth/register` |
| 02 | Đăng nhập, trả JWT | `POST /api/v1/auth/login` |
| 03 | Đăng xuất | `POST /api/v1/auth/logout` (204) |
| 04 | Phân quyền theo vai trò | JWT Filter + `/admin/**` chỉ ADMIN; ADMIN nạp sẵn |
| 05, 16 | Xem / cập nhật hồ sơ (SV và NTD) | `GET`, `PUT /api/v1/profiles/me` |
| 11 | Cập nhật CV và giới thiệu bản thân | `PUT /api/v1/profiles/me` (`cvContent`, `bio`) |

Bám API contract buổi 5: base URL `/api/v1`, lỗi JSON thống nhất, mã 400/401/403/409. Giao diện đăng ký, đăng nhập và hồ sơ dùng API backend; Vite proxy `/api` tới `localhost:8080`.

## Luồng website tuyển dụng
- Khách và sinh viên xem danh sách tin đang tuyển tại `/jobs` (8 tin mỗi trang), tìm theo từ khóa, khu vực và hình thức làm việc; mở từng tin để xem mô tả, yêu cầu, quyền lợi, lương và hạn tuyển.
- Nhà tuyển dụng đăng nhập sẽ vào khu vực quản lý tin tại `/employer/jobs`, có thể tạo/sửa tin, đóng tin đã tuyển đủ hoặc mở lại tin.
- API công khai: `GET /api/v1/jobs`, `GET /api/v1/jobs/{id}`. API nhà tuyển dụng: `GET /api/v1/jobs/mine`, `POST /api/v1/jobs`, `PUT /api/v1/jobs/{id}`, `PATCH /api/v1/jobs/{id}/status`.
- Tin đóng hoặc quá hạn không xuất hiện trong tìm kiếm công khai; chỉ chủ tin được sửa hoặc đổi trạng thái.

## Luồng ứng tuyển và phỏng vấn
- Sinh viên cập nhật CV trong hồ sơ, mở chi tiết việc làm và gửi CV kèm lời nhắn ứng tuyển. Mỗi sinh viên chỉ ứng tuyển một lần cho mỗi tin.
- Hồ sơ sinh viên hỗ trợ nhập nội dung CV hoặc tải tệp PDF/DOC/DOCX tối đa 5 MB; tệp được lưu cùng hồ sơ và chụp bản sao khi ứng tuyển để nhà tuyển dụng tải xuống.
- Nhà tuyển dụng xem toàn bộ hồ sơ của mình trong tab **Đơn ứng tuyển**, có thể lọc theo trạng thái, tin tuyển dụng hoặc tìm ứng viên; cũng có thể xem theo từng tin tại mục **Ứng viên**. Nhà tuyển dụng đọc CV được lưu tại thời điểm nộp, rồi mời phỏng vấn với thời gian/địa điểm hoặc từ chối hồ sơ.
- Sinh viên theo dõi kết quả tại **Hồ sơ ứng tuyển** và nhận thông báo trong **Thông báo**; thông báo có thể đánh dấu đã đọc.
- API: `PUT /api/v1/profiles/me/cv-file` (multipart field `file`), `GET /api/v1/profiles/me/cv-file`, `POST /api/v1/jobs/{id}/applications`, `GET /api/v1/applications/mine`, `GET /api/v1/applications/employer/mine`, `GET /api/v1/jobs/{id}/applications`, `GET /api/v1/applications/{id}/cv-file`, `PATCH /api/v1/applications/{id}/status`, `GET /api/v1/notifications/mine`, `PATCH /api/v1/notifications/{id}/read`.
- Chỉ sinh viên đăng nhập được ứng tuyển; chỉ nhà tuyển dụng sở hữu tin được xem và xử lý hồ sơ; quyết định xử lý sẽ được lưu và thông báo cho sinh viên.

## Chạy thử
1. Cài JDK 17+, Maven 3.9+, MySQL 8 (tạo sẵn hoặc để app tự tạo database `partjob_db`).
2. Đặt biến môi trường nếu cần (mặc định: user `root`, mật khẩu rỗng):
   `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (≥ 32 ký tự), `ADMIN_EMAIL`, `ADMIN_PASSWORD`.
3. Chạy:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
4. Swagger UI: http://localhost:8080/api/v1/swagger-ui.html (nút **Authorize** để dán JWT).
5. Tài khoản ADMIN nạp sẵn lần chạy đầu: `admin@partjob.local` / `Admin@12345` (đổi bằng biến môi trường khi chạy thật). Một số địa điểm mẫu (id 1–8) cũng được nạp sẵn.

### Chạy giao diện web
```bash
cd frontend
npm ci
npm run dev
```
Mở URL Vite hiển thị trong terminal (mặc định `http://localhost:5173`). Đăng ký tài khoản Sinh viên hoặc Nhà tuyển dụng, đăng nhập, rồi cập nhật hồ sơ/CV.

### Luồng demo bằng curl
```bash
curl -X POST localhost:8080/api/v1/auth/register -H "Content-Type: application/json" \
  -d '{"fullName":"Bùi Đăng Khoa","email":"khoa@student.vlu.edu.vn","password":"Abc@12345","role":"STUDENT"}'

curl -X POST localhost:8080/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"email":"khoa@student.vlu.edu.vn","password":"Abc@12345"}'      # lấy accessToken

curl localhost:8080/api/v1/profiles/me -H "Authorization: Bearer <accessToken>"

curl -X PUT localhost:8080/api/v1/profiles/me -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Bùi Đăng Khoa","phone":"0901234567","locationId":1,"bio":"Sinh viên năm 3","school":"ĐH Văn Lang","major":"Công nghệ phần mềm","cvContent":"Kỹ năng: giao tiếp, Excel"}'

curl -X POST localhost:8080/api/v1/auth/logout -H "Authorization: Bearer <accessToken>"   # 204
```

## Kiểm thử và build giao diện
```bash
cd backend
mvn test
```
Bộ test `AuthProfileFlowTest` chạy trên H2 (profile `test`), phủ luồng đăng ký → đăng nhập → hồ sơ/CV → đăng xuất và các lỗi 400/401/403/409.

```bash
cd frontend
npm ci
npm run build
```

## Quy ước làm việc với Git (theo yêu cầu buổi 6)
- Mỗi thành viên một nhánh `feature/<ten-chuc-nang>-<ten>`, gộp vào `main` bằng pull request.
- Commit nhỏ, thông điệp rõ ràng, ví dụ: `feat(auth): dang ky va dang nhap JWT (FR-01, FR-02)`.
- Tối thiểu 2 commit trong buổi; không commit `target/`, `.env`, mật khẩu thật.

## Quy ước nghiệp vụ đã áp dụng
- Mật khẩu băm BCrypt; JWT hết hạn sau 2 giờ; chỉ ADMIN do nhóm nạp sẵn, không cho tự đăng ký.
- Email được chuẩn hóa chữ thường; tài khoản `LOCKED` bị từ chối đăng nhập (403) và mọi token cũ ngừng hiệu lực.
- `PUT /profiles/me`: `school`, `major`, `cvContent` chỉ ghi với STUDENT; `companyName` chỉ ghi với EMPLOYER (trường không thuộc vai trò bị bỏ qua).
