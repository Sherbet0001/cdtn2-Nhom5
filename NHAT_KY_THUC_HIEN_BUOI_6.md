# NHẬT KÝ THỰC HIỆN ĐỀ TÀI – BUỔI 6 (SPRINT 1)
**Đề tài:** Nền tảng quản lý freelance / marketplace việc làm part-time cho sinh viên  
**Nhóm sinh viên:**
- Viên Thành Đạt – 2374802010107 (Frontend ReactJS)
- **Đoàn Xuân Tiến – 2374802010493 (Backend: Module Tin tuyển dụng & Ứng tuyển)**
- Bùi Đăng Khoa – 2374802013428 (Backend: Module Xác thực, Đánh giá, Quản trị)  
**GVHD:** ThS. Hà Đồng Hưng  
**Mốc đánh giá:** Sprint 1 – Xây dựng sản phẩm

---

## 1. Mục tiêu và phạm vi công việc trong buổi 6 (Sprint 1)
Theo yêu cầu phân công của đề cương và mục tiêu Sprint 1:
- Hoàn thành khung ứng dụng Spring Boot 3.2 cho phân hệ Tin tuyển dụng (`Job_Module_SourceCode`).
- Triển khai toàn bộ các endpoint CRUD và tìm kiếm tin tuyển dụng (FR-07 đến FR-10, FR-17, FR-18, FR-23) theo đúng **API Contract** đã duyệt ở Buổi 5.
- Thực hiện quản lý mã nguồn bằng Git theo quy chuẩn: làm việc trên nhánh riêng `feature/job-post`, commit thường xuyên với commit message rõ ràng.
- Chuẩn bị dữ liệu mẫu (`seed_data_buoi6.sql`) và tài liệu Swagger UI để phục vụ demo tiến độ cuối buổi trước GVHD.

---

## 2. Kết quả đạt được (Deliverables)

### 2.1 Các API và nghiệp vụ đã hoàn thành
| Method | Đường dẫn API | Chức năng & FR | Kiểm tra nghiệp vụ / Ràng buộc |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/jobs` | **FR-07, 08, 09**: Tìm kiếm & lọc tin | Hỗ trợ tìm từ khóa FULLTEXT (`MATCH ... AGAINST`), lọc `categoryId`, `locationId`, `minSalary`, phân trang và sắp xếp `createdAt,desc`. |
| **GET** | `/api/v1/jobs/{id}` | **FR-10**: Chi tiết việc làm | Trả về đầy đủ thông tin tin tuyển dụng, trả HTTP 404 nếu không tìm thấy. |
| **POST** | `/api/v1/jobs` | **FR-17**: Đăng tin mới (NTD) | Validation dữ liệu đầu vào: `salaryMin <= salaryMax`, `startDate <= endDate`, `slots >= 1`. Trạng thái mặc định `OPEN`. Trả HTTP 201. |
| **GET** | `/api/v1/jobs/mine` | **FR-18**: Danh sách tin của tôi | Lấy toàn bộ tin của NTD hiện tại kèm phân trang. |
| **PUT** | `/api/v1/jobs/{id}` | **FR-18**: Sửa nội dung tin | Chỉ chủ tin mới được sửa (HTTP 403 nếu sai quyền), chặn sửa tin đã `COMPLETED` (HTTP 409). |
| **PATCH** | `/api/v1/jobs/{id}/status` | **FR-23**: Đổi trạng thái tin | Máy trạng thái: `OPEN` → `FILLED` → `COMPLETED`; cho phép đóng tin `CLOSED`; cấm quay ngược từ `COMPLETED` (HTTP 409). |

### 2.2 Hạ tầng và kiến trúc phân lớp
- **Entity**: `Job.java` ánh xạ bảng `tbl_jobs` chuẩn JPA/Hibernate.
- **Repository**: `JobRepository.java` với query native hỗ trợ tìm kiếm toàn văn và `countQuery` tối ưu cho phân trang.
- **DTOs**: `JobCreateRequest`, `JobUpdateRequest`, `JobStatusUpdateRequest`, `JobResponse`.
- **Xử lý ngoại lệ tập trung**: `GlobalExceptionHandler.java` chuẩn hóa định dạng lỗi JSON theo đúng đặc tả API contract Buổi 5 (`timestamp`, `status`, `error`, `message`, `path`).
- **Tài liệu trực quan**: Tích hợp SpringDoc OpenAPI 3, truy cập trực tiếp tại: `http://localhost:8080/swagger-ui.html`.

### 2.3 Quản lý phiên bản Git
- Đã khởi tạo Git repository cục bộ.
- Đã tạo nhánh làm việc độc lập: `feature/job-post`.
- Đã tạo `.gitignore` chuẩn cho Java Maven, tránh commit file rác (`target/`, IDE settings).
- Đã commit mã nguồn hoàn chỉnh với thông điệp: `feat(job): implement core CRUD, search, status transitions and swagger docs`.

### 2.4 Dữ liệu mẫu (Seed Data)
- File `src/main/resources/seed_data_buoi6.sql` gồm:
  - 8 ngành nghề mẫu (`tbl_categories`)
  - 10 quận/huyện TP.HCM (`tbl_locations`)
  - 1 tài khoản Admin, 3 NTD, 3 Sinh viên mẫu (`tbl_users`)
  - 5 tin tuyển dụng mẫu đa dạng trạng thái (`OPEN`, `COMPLETED`) để kiểm thử tìm kiếm và lọc.

---

## 3. Các vướng mắc kỹ thuật đã gặp và cách giải quyết

1. **Vấn đề phân trang với Native Query FULLTEXT trên MySQL:**
   - *Mô tả:* Khi sử dụng `MATCH(title, description) AGAINST(...)` trong query native kết hợp với `Pageable`, Spring Data JPA không tự sinh được count query chính xác, dẫn đến lỗi runtime khi tính tổng số trang.
   - *Cách giải quyết:* Khai báo tường minh thuộc tính `countQuery` trong `@Query`, xử lý trường hợp tham số `keyword` bị rỗng/null bằng điều kiện `(:keyword IS NULL OR :keyword = '' OR MATCH(...) AGAINST(...))`.

2. **Ràng buộc nghiệp vụ trạng thái công việc (FR-23):**
   - *Mô tả:* Tránh việc NTD tùy tiện sửa nội dung khi công việc đã xong hoặc chuyển ngược trạng thái từ `COMPLETED` về các trạng thái trước.
   - *Cách giải quyết:* Xây dựng lớp ngoại lệ `BusinessRuleException` ánh xạ HTTP 409 (Conflict) và viết hàm kiểm tra luồng chuyển trạng thái hợp lệ ngay tại tầng `JobService`.

---

## 4. Kế hoạch cho buổi tiếp theo (Sprint 2 - Buổi 7)
- Hoàn thiện module **Ứng tuyển (`ApplicationController`, `ApplicationService`, `ApplicationRepository`)** (FR-12 đến FR-14, FR-19, FR-21, FR-22).
- Tích hợp gọi thông báo (`NotificationService`) khi có sinh viên ứng tuyển hoặc NTD duyệt hồ sơ.
- Phối hợp với Đạt (Frontend) kết nối API tin tuyển dụng vào giao diện ReactJS.
- Chuẩn bị đầy đủ báo cáo và sản phẩm để đánh giá giữa kỳ đợt 2 (Rubric 2 - 30%).

