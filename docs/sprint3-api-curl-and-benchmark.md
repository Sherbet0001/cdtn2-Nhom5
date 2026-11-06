# Tài liệu Kiểm thử API (cURL) & Báo cáo Hiệu năng Sprint 3

Phạm vi Sprint 3: **FR-07 đến FR-10, FR-12, FR-17 đến FR-22** và **NFR-01** (Phản hồi `< 2 giây` với `1.000` tin tuyển dụng).

---

## 1. Ví dụ lệnh cURL kiểm thử nhanh

### 1.1. Danh mục công khai (`GET /categories`, `GET /locations`)
```bash
curl -X GET "http://localhost:8080/api/v1/categories"
curl -X GET "http://localhost:8080/api/v1/locations"
```

### 1.2. Tìm kiếm, lọc và phân trang việc làm công khai (`FR-07`, `FR-08`, `FR-09`)
```bash
# Tìm kiếm theo từ khóa + lọc ngành nghề + địa điểm + mức lương tối thiểu kèm đơn vị tính
curl -X GET "http://localhost:8080/api/v1/jobs?keyword=ReactJS&categoryId=6&locationId=5&minSalary=30000&salaryUnit=HOUR&page=0&size=10&sort=createdAt,desc"
```

### 1.3. Xem chi tiết tin tuyển dụng (`FR-10`)
```bash
curl -X GET "http://localhost:8080/api/v1/jobs/1"
```

### 1.4. Nhà tuyển dụng đăng tin mới (`FR-17`)
```bash
curl -X POST "http://localhost:8080/api/v1/jobs" \
  -H "Authorization: Bearer <EMPLOYER_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Nhân viên Pha chế & Phục vụ Ca Tối",
    "description": "Pha chế đồ uống theo công thức, phục vụ khách hàng tại quán cà phê acoustic.",
    "categoryId": 1,
    "locationId": 3,
    "salaryMin": 28000,
    "salaryMax": 35000,
    "salaryUnit": "HOUR",
    "workSchedule": "Ca tối 18:00 - 22:30",
    "startDate": "2026-05-01",
    "endDate": "2026-08-31",
    "slots": 2
  }'
```

### 1.5. Nhà tuyển dụng xem danh sách tin đã đăng (`FR-18`) và cập nhật tin (`FR-19`)
```bash
# Danh sách tin của NTD
curl -X GET "http://localhost:8080/api/v1/jobs/mine?page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer <EMPLOYER_JWT_TOKEN>"

# Cập nhật tin khi chưa COMPLETED/CLOSED
curl -X PUT "http://localhost:8080/api/v1/jobs/1" \
  -H "Authorization: Bearer <EMPLOYER_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Nhân viên Pha chế & Phục vụ Ca Tối (Đã cập nhật)",
    "description": "Cập nhật mức lương và tăng chỉ tiêu tuyển dụng.",
    "categoryId": 1,
    "locationId": 3,
    "salaryMin": 30000,
    "salaryMax": 38000,
    "salaryUnit": "HOUR",
    "workSchedule": "Ca tối 18:00 - 22:30",
    "startDate": "2026-05-01",
    "endDate": "2026-08-31",
    "slots": 3
  }'
```

### 1.6. Sinh viên ứng tuyển việc làm (`FR-12`)
```bash
curl -X POST "http://localhost:8080/api/v1/jobs/2/applications" \
  -H "Authorization: Bearer <STUDENT_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "coverLetter": "Em là sinh viên năm 3 CNTT ĐH Văn Lang, đã có CV và kinh nghiệm ReactJS."
  }'
```

### 1.7. Nhà tuyển dụng xem danh sách ứng viên (`FR-20`) và Duyệt / Từ chối (`FR-21`, `FR-22`)
```bash
# Xem danh sách ứng viên kèm nội dung CV
curl -X GET "http://localhost:8080/api/v1/jobs/1/applications?page=0&size=10" \
  -H "Authorization: Bearer <EMPLOYER_JWT_TOKEN>"

# Chấp nhận (ACCEPTED) hoặc Từ chối (REJECTED) hồ sơ
curl -X PATCH "http://localhost:8080/api/v1/applications/1/status" \
  -H "Authorization: Bearer <EMPLOYER_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "ACCEPTED"
  }'
```

---

## 2. Kết quả đo hiệu năng NFR-01 (1.000 tin tuyển dụng)

- **Kịch bản kiểm thử**: `JobAndApplicationIntegrationTests.benchmarkGetJobs_NFR01_with1000SeededJobs_respondsUnderTwoSeconds`
- **Số lượng bản ghi trong `tbl_jobs`**: `1.004` tin tuyển dụng ở trạng thái `OPEN`.
- **Truy vấn thực thi**: `GET /api/v1/jobs?keyword=Benchmark&categoryId=1&locationId=1&minSalary=25000&salaryUnit=HOUR&page=0&size=10`
- **Chỉ tiêu yêu cầu (NFR-01)**: `< 2.000 ms` (`2 giây`).
- **Kết quả thực tế**: `~35 - 80 ms` (nhanh hơn ngưỡng 2 giây khoảng 25 lần).

