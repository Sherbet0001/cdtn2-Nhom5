# NHẬT KÝ THỰC HIỆN ĐỀ TÀI – BUỔI 7 (SPRINT 2 - MỐC GIỮA KỲ 2)
**Đề tài:** Nền tảng quản lý freelance / marketplace việc làm part-time cho sinh viên  
**Nhóm sinh viên:**
- Viên Thành Đạt – 2374802010107 (Frontend ReactJS)
- **Đoàn Xuân Tiến – 2374802010493 (Backend: Module Tin tuyển dụng & Ứng tuyển)**
- Bùi Đăng Khoa – 2374802013428 (Backend: Module Xác thực, Đánh giá, Quản trị)  
**GVHD:** ThS. Hà Đồng Hưng / ThS. Nguyễn Văn Trung  
**Mốc đánh giá:** Giữa kỳ lần 2 (Rubric 2 - 30%)

---

## 1. Mục tiêu buổi 7 (Sprint 2)
- Hoàn thiện trọn vẹn luồng nghiệp vụ chính đầu-cuối: **Đăng tin (FR-17) → Tìm kiếm / Lọc (FR-07 đến FR-10) → Ứng tuyển (FR-12) → Duyệt / Từ chối hồ sơ (FR-19, FR-21, FR-22) → Đổi trạng thái tin sang Hoàn thành (FR-23)**.
- Đảm bảo tính nhất quán dữ liệu giữa Frontend và Backend (đồng bộ các trạng thái: `OPEN`, `FILLED`, `COMPLETED`, `CLOSED` của tin và `PENDING`, `ACCEPTED`, `REJECTED` của hồ sơ).
- Rà soát quyết định **Go/No-go**: Dự án đảm bảo tiến độ "Go", giữ nguyên toàn bộ các mục Must-have mà không cần cắt giảm.

---

## 2. Kết quả xây dựng chi tiết theo phân công (Đoàn Xuân Tiến)

### 2.1 Module Tin tuyển dụng (`JobController` – `JobService` – `JobRepository` / `tbl_jobs`)
| Mã FR | Endpoint & Method | Chức năng nghiệp vụ | Hiện trạng kiểm thử |
| :--- | :--- | :--- | :--- |
| **FR-07** | `GET /api/v1/jobs` | Danh sách việc part-time trạng thái `OPEN`, phân trang `created_at,desc`. | Đã kiểm thử thành công trên Swagger & UI. |
| **FR-08** | `GET /api/v1/jobs?keyword=...` | Tìm kiếm từ khóa FULLTEXT (`MATCH ... AGAINST`), kết hợp Plan B (LIKE fallback) cho từ khóa ngắn. | Đã kiểm thử với từ khóa `phục vụ`, `gia sư`, `thu ngân`. |
| **FR-09** | `GET /api/v1/jobs?categoryId=...&minSalary=...&salaryUnit=HOUR` | Lọc kết hợp ngành nghề, quận/huyện, lương tối thiểu kèm đơn vị. | Đã kiểm thử đúng quy tắc (nếu thiếu `salaryUnit` sẽ bỏ qua `minSalary`). |
| **FR-10** | `GET /api/v1/jobs/{id}` | Chi tiết việc làm, nạp tên ngành nghề và địa điểm chuẩn xác. | Đã kiểm thử thành công (trả 404 nếu không tìm thấy). |
| **FR-17** | `POST /api/v1/jobs` | NTD tạo tin mới. Validate: `salaryMin <= salaryMax`, `startDate <= endDate`, `slots >= 1`. | Đã kiểm thử thành công (HTTP 201 Created). |
| **FR-18** | `GET /api/v1/jobs/mine`<br>`PUT /api/v1/jobs/{id}` | NTD quản lý và sửa tin của mình. Chặn sửa tin đã `COMPLETED` (HTTP 409). | Đã kiểm thử kiểm tra quyền sở hữu NTD (HTTP 403). |
| **FR-23** | `PATCH /api/v1/jobs/{id}/status` | Đổi trạng thái `OPEN` → `FILLED` → `COMPLETED` / `CLOSED`. Cấm quay ngược từ `COMPLETED`. | Đã kiểm thử máy trạng thái và bắt lỗi 409 Conflict. |

### 2.2 Module Ứng tuyển (`ApplicationController` – `ApplicationService` – `ApplicationRepository` / `tbl_applications`)
| Mã FR | Endpoint & Method | Chức năng nghiệp vụ | Hiện trạng kiểm thử |
| :--- | :--- | :--- | :--- |
| **FR-12** | `POST /api/v1/jobs/{jobId}/applications` | Sinh viên nộp hồ sơ vào tin `OPEN`. Chặn nộp lặp (HTTP 409). | Đã kiểm thử thành công (HTTP 201, trạng thái ban đầu `PENDING`). |
| **FR-13, 14**| `GET /api/v1/applications/mine` | Sinh viên theo dõi lịch sử và trạng thái hồ sơ ứng tuyển của mình. | Đã kiểm thử thành công. |
| **FR-19** | `GET /api/v1/jobs/{jobId}/applications` | NTD xem danh sách sinh viên ứng tuyển vào tin của mình. | Đã kiểm thử thành công. |
| **FR-21, 22**| `PATCH /api/v1/applications/{id}/status` | NTD duyệt (`ACCEPTED`) hoặc từ chối (`REJECTED`) hồ sơ. Chặn duyệt quá `slots` tuyển. | Đã kiểm thử quyết định cuối cùng không được đổi lại. |

---

## 3. Đánh giá Checkpoint Go/No-go
- **Trạng thái:** **"GO"** (Nhóm đang đi đúng tiến độ, không cần cắt giảm phạm vi Must-have).
- Toàn bộ luồng đăng tin → tìm kiếm → ứng tuyển → duyệt hồ sơ đã chạy thông suốt từ CSDL MySQL đến API và giao diện Web trực quan.

---

## 4. Kế hoạch buổi tiếp theo (Sprint 3 - Buổi 8)
- Phối hợp với Khoa tích hợp NotificationService khi hồ sơ được duyệt và khi tin hoàn thành.
- Mở chức năng đánh giá hai chiều (FR-15, FR-24) sau khi tin sang `COMPLETED`.
- Kiểm thử tích hợp tổng thể toàn bộ hệ thống giữa 3 thành viên.
