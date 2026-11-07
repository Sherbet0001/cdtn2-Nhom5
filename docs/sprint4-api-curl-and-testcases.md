# Tài liệu API cURL & Bảng Test Case Sprint 4 (`FR-23, FR-15, FR-24, FR-13, FR-06`)

## 1. Ví dụ lệnh cURL cho các API Sprint 4

### 1.1. Đổi trạng thái tin tuyển dụng (`PATCH /api/v1/jobs/{id}/status` – `FR-23`)
```bash
# Bước 1: Chuyển từ OPEN sang FILLED
curl -X PATCH "http://localhost:8080/api/v1/jobs/1/status" \
  -H "Authorization: Bearer <EMPLOYER_JWT>" \
  -H "Content-Type: application/json" \
  -d '{"status": "FILLED"}'

# Bước 2: Chuyển từ FILLED sang COMPLETED (Tự động tạo thông báo JOB_COMPLETED cho các SV đã ACCEPTED)
curl -X PATCH "http://localhost:8080/api/v1/jobs/1/status" \
  -H "Authorization: Bearer <EMPLOYER_JWT>" \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}'
```

### 1.2. Đánh giá hai chiều (`POST /api/v1/jobs/{jobId}/reviews` & `GET /api/v1/users/{userId}/reviews` – `FR-15, FR-24`)
```bash
# NTD đánh giá Sinh viên đã ACCEPTED (FR-24)
curl -X POST "http://localhost:8080/api/v1/jobs/1/reviews" \
  -H "Authorization: Bearer <EMPLOYER_JWT>" \
  -H "Content-Type: application/json" \
  -d '{"revieweeId": 4, "rating": 5, "comment": "Làm việc đúng giờ, có trách nhiệm cao."}'

# Sinh viên đã ACCEPTED đánh giá NTD chủ tin (FR-15)
curl -X POST "http://localhost:8080/api/v1/jobs/1/reviews" \
  -H "Authorization: Bearer <STUDENT_JWT>" \
  -H "Content-Type: application/json" \
  -d '{"revieweeId": 2, "rating": 5, "comment": "Môi trường thân thiện, trả lương đúng hạn."}'

# Xem danh sách đánh giá VISIBLE và điểm trung bình của người dùng
curl -X GET "http://localhost:8080/api/v1/users/4/reviews" \
  -H "Authorization: Bearer <JWT>"
```

### 1.3. Theo dõi & Lịch sử hồ sơ ứng tuyển (`GET /api/v1/applications/mine` & `GET /api/v1/applications/{id}` – `FR-13, FR-14`)
```bash
# Lấy danh sách hồ sơ đã nộp của SV (có lọc theo status)
curl -X GET "http://localhost:8080/api/v1/applications/mine?status=ACCEPTED" \
  -H "Authorization: Bearer <STUDENT_JWT>"

# Xem chi tiết 1 hồ sơ ứng tuyển (SV chủ hồ sơ hoặc NTD chủ tin)
curl -X GET "http://localhost:8080/api/v1/applications/1" \
  -H "Authorization: Bearer <STUDENT_JWT>"
```

### 1.4. Hệ thống thông báo (`GET/PATCH /api/v1/notifications/*` – `FR-06`)
```bash
# Đếm số thông báo chưa đọc (Frontend polling mỗi 30 giây)
curl -X GET "http://localhost:8080/api/v1/notifications/unread-count" \
  -H "Authorization: Bearer <JWT>"

# Lấy danh sách thông báo (unreadOnly=true/false)
curl -X GET "http://localhost:8080/api/v1/notifications?unreadOnly=true" \
  -H "Authorization: Bearer <JWT>"

# Đánh dấu 1 thông báo đã đọc
curl -X PATCH "http://localhost:8080/api/v1/notifications/1/read" \
  -H "Authorization: Bearer <JWT>"

# Đánh dấu tất cả thông báo đã đọc
curl -X PATCH "http://localhost:8080/api/v1/notifications/read-all" \
  -H "Authorization: Bearer <JWT>"
```

