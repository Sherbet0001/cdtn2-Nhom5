# Tài liệu cURL & Rà soát tổng thể Sprint 5 (FR-14, FR-25 → FR-31)

## 1. Lệnh cURL kiểm thử nhanh các Endpoint Sprint 5

### 1.1. Đăng nhập tài khoản Admin (`admin@vlu.edu.vn`)
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@vlu.edu.vn","password":"Abc@12345"}'
```

### 1.2. Xem danh sách người dùng & Khóa/Mở khóa tài khoản (`FR-25`, `FR-26`)
```bash
# Lọc danh sách người dùng theo vai trò STUDENT
curl -X GET "http://localhost:8080/api/v1/admin/users?role=STUDENT&page=0&size=10" \
  -H "Authorization: Bearer <ADMIN_JWT>"

# Khóa tài khoản sinh viên #6 (sv3@vanlanguni.vn)
curl -X PATCH http://localhost:8080/api/v1/admin/users/6/status \
  -H "Authorization: Bearer <ADMIN_JWT>" \
  -H "Content-Type: application/json" \
  -d '{"status":"LOCKED"}'
```

### 1.3. Quản lý toàn bộ tin đăng & Ẩn/Khôi phục tin vi phạm (`FR-27`)
```bash
# Xem tất cả tin đăng ở mọi trạng thái
curl -X GET "http://localhost:8080/api/v1/admin/jobs?page=0&size=10" \
  -H "Authorization: Bearer <ADMIN_JWT>"

# Ẩn tin tuyển dụng đang OPEN hoặc FILLED sang HIDDEN
curl -X PATCH http://localhost:8080/api/v1/admin/jobs/1/status \
  -H "Authorization: Bearer <ADMIN_JWT>" \
  -H "Content-Type: application/json" \
  -d '{"status":"HIDDEN"}'

# Khôi phục tin đang HIDDEN (tự động về OPEN nếu acceptedCount < slots, ngược lại về FILLED)
curl -X PATCH http://localhost:8080/api/v1/admin/jobs/1/status \
  -H "Authorization: Bearer <ADMIN_JWT>" \
  -H "Content-Type: application/json" \
  -d '{"status":"RESTORE"}'
```

### 1.4. CRUD Danh mục Ngành nghề & Địa điểm (`FR-28`, `FR-29`)
```bash
# Thêm ngành nghề mới
curl -X POST http://localhost:8080/api/v1/admin/categories \
  -H "Authorization: Bearer <ADMIN_JWT>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Trợ lý Nghiên cứu Khoa học"}'

# Thử xóa ngành nghề #1 đang có tin tuyển dụng sử dụng -> Trả về 409 Conflict (RESOURCE_IN_USE)
curl -i -X DELETE http://localhost:8080/api/v1/admin/categories/1 \
  -H "Authorization: Bearer <ADMIN_JWT>"
```

### 1.5. Kiểm duyệt đánh giá & Xem thống kê tổng quan (`FR-30`, `FR-31`)
```bash
# Ẩn đánh giá không phù hợp sang HIDDEN
curl -X PATCH http://localhost:8080/api/v1/admin/reviews/1/status \
  -H "Authorization: Bearer <ADMIN_JWT>" \
  -H "Content-Type: application/json" \
  -d '{"status":"HIDDEN"}'

# Xem số liệu thống kê tổng quan hệ thống
curl -X GET http://localhost:8080/api/v1/admin/stats \
  -H "Authorization: Bearer <ADMIN_JWT>"
```

