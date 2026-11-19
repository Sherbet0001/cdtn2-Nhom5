# BÁO CÁO KẾT QUẢ KIỂM THỬ VÀ TINH CHỈNH SẢN PHẨM – BUỔI 8
**Đề tài:** Nền tảng quản lý freelance / marketplace việc làm part-time cho sinh viên  
**Nhóm:** Viên Thành Đạt · Đoàn Xuân Tiến · Bùi Đăng Khoa  
**Sinh viên thực hiện:** **Đoàn Xuân Tiến – 2374802010493 (Phụ trách: Module Tin tuyển dụng & Ứng tuyển)**  
**Giảng viên giảng dạy:** ThS. Nguyễn Văn Trung / GVHD: ThS. Hà Đồng Hưng  
**Mốc thực hiện:** Buổi 8 – Sprint 3: Kiểm thử và tinh chỉnh sản phẩm

---

## 1. Mục tiêu và phạm vi kiểm thử
- Kiểm thử đơn vị (Unit Test) cho các phương thức xử lý nghiệp vụ của `JobService` và `ApplicationService`.
- Kiểm thử tích hợp (Integration Test) luồng API từ `Controller` qua `MockMvc`.
- Kiểm thử bao phủ cả **"Happy Path"** và **"Unhappy Path"** (dữ liệu sai/thiếu, vi phạm ràng buộc nghiệp vụ, phân quyền trái phép) theo đúng yêu cầu tại Slide 5 & Slide 7 Buổi 8.
- Xây dựng bảng theo dõi lỗi (Bug Log), phân loại mức độ nghiêm trọng và ghi nhận danh sách tinh chỉnh / hạn chế đã biết.

---

## 2. Bảng tổng hợp kết quả kiểm thử (Test Cases Execution Matrix)

| Mã TC | Phân hệ / FR | Mô tả kịch bản kiểm thử | Dữ liệu đầu vào / Điều kiện | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
| :---: | :---: | :--- | :--- | :--- | :--- | :---: |
| **TC-01** | **Job / FR-17** | Đăng tin tuyển dụng hợp lệ | Tiêu đề, mô tả, lương min < max, ngày bắt đầu < kết thúc, slots >= 1 | Trả về HTTP 201 Created, `status = OPEN` | Trả về 201, `jobId=10`, `status="OPEN"` | **PASSED** |
| **TC-02** | **Job / FR-17** | Đăng tin với `salaryMin > salaryMax` | `salaryMin=40.000`, `salaryMax=30.000` | Ném `IllegalArgumentException`, thông báo lỗi rõ ràng | Bắt lỗi đúng: "Lương tối thiểu không được lớn hơn lương tối đa" | **PASSED** |
| **TC-03** | **Job / FR-17** | Đăng tin với `startDate > endDate` | `startDate=2026-12-31`, `endDate=2026-11-01` | Ném `IllegalArgumentException` | Bắt lỗi đúng: "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc" | **PASSED** |
| **TC-04** | **Job / FR-10** | Xem chi tiết tin tuyển dụng tồn tại | `jobId = 1` | Trả về thông tin chi tiết kèm tên danh mục & địa điểm | Trả về `JobResponse` đầy đủ thông tin | **PASSED** |
| **TC-05** | **Job / FR-10** | Xem chi tiết tin không tồn tại | `jobId = 999` (không có trong DB) | Ném `ResourceNotFoundException` (HTTP 404) | Bắt lỗi 404 "Không tìm thấy tin tuyển dụng" | **PASSED** |
| **TC-06** | **Job / FR-18** | Sửa tin đã ở trạng thái `COMPLETED` | Tin `#1` có `status = COMPLETED` | Chặn sửa, ném `BusinessRuleException` (HTTP 409) | Trả về 409: "Không thể chỉnh sửa tin đã hoàn thành" | **PASSED** |
| **TC-07** | **Job / FR-18** | NTD sửa tin của người khác | `employerId = 99L` (không phải chủ tin) | Chặn sửa, ném `ForbiddenException` (HTTP 403) | Bắt lỗi 403: "Bạn không có quyền chỉnh sửa tin này" | **PASSED** |
| **TC-08** | **Job / FR-23** | Chuyển đổi trạng thái tin hợp lệ | `OPEN` → `FILLED` → `COMPLETED` | Cập nhật trạng thái thành công từng bước | Chuyển trạng thái chính xác theo máy trạng thái | **PASSED** |
| **TC-09** | **Job / FR-23** | Cố ý chuyển ngược từ `COMPLETED` | Tin `#1` đang `COMPLETED` cố chuyển về `OPEN` | Ném `BusinessRuleException` (HTTP 409) | Bắt lỗi 409: "Tin đã hoàn thành, không thể thay đổi nữa" | **PASSED** |
| **TC-10** | **App / FR-12** | Sinh viên nộp hồ sơ vào tin `OPEN` | `jobId=1` (đang OPEN), `studentId=5` | Trả về HTTP 201, trạng thái hồ sơ `PENDING` | Tạo thành công hồ sơ với `status="PENDING"` | **PASSED** |
| **TC-11** | **App / FR-12** | Ứng tuyển vào tin đã đóng (`CLOSED`) | `jobId=2` có `status = CLOSED` *(Kịch bản Slide 5)* | Từ chối nộp hồ sơ, ném lỗi HTTP 409 | Bắt lỗi 409: "Tin tuyển dụng hiện không mở nhận hồ sơ" | **PASSED** |
| **TC-12** | **App / FR-12** | Nộp hồ sơ lặp lại vào cùng 1 tin | Sinh viên `#5` nộp lại vào tin `#1` đã nộp | Chặn nộp lặp, ném `BusinessRuleException` (409) | Bắt lỗi 409: "Bạn đã ứng tuyển công việc này rồi" | **PASSED** |
| **TC-13** | **App / FR-21** | NTD duyệt hồ sơ (`ACCEPTED`) | Hồ sơ `#10` đang `PENDING`, tin còn slot | Cập nhật hồ sơ thành `ACCEPTED` (HTTP 200) | Cập nhật thành công sang `ACCEPTED` | **PASSED** |
| **TC-14** | **App / FR-21** | Duyệt hồ sơ khi đã đủ số lượng slots | Số lượng đã duyệt = `slots` tuyển | Chặn duyệt vượt chỉ tiêu, ném lỗi HTTP 409 | Bắt lỗi 409: "Tin tuyển dụng đã nhận đủ số lượng sinh viên" | **PASSED** |
| **TC-15** | **App / FR-21, 22** | Thay đổi lại hồ sơ đã chốt kết quả | Hồ sơ đã là `ACCEPTED` cố chuyển sang `REJECTED` | Chặn thay đổi quyết định cuối cùng (409) | Bắt lỗi 409: "Hồ sơ đã được xử lý, không thể thay đổi lại" | **PASSED** |
| **TC-16** | **App / FR-21, 22** | NTD khác duyệt hồ sơ không phải của mình | User không sở hữu tin tuyển dụng | Chặn truy cập, ném `ForbiddenException` (HTTP 403) | Bắt lỗi 403 Forbidden | **PASSED** |
| **TC-17** | **Job / MockMvc** | `GET /api/v1/jobs?keyword=phục vụ` | Gọi endpoint tìm kiếm qua HTTP | Trả về mã 200 OK và danh sách tin JSON | Khớp status 200 và cấu trúc phân trang | **PASSED** |
| **TC-18** | **Job / MockMvc** | `POST /api/v1/jobs` qua HTTP | Body JSON hợp lệ kèm header `X-User-Id: 2` | Trả về HTTP 201 Created và `{ "jobId": ..., "status": "OPEN" }` | Khớp status 201 và JSON response | **PASSED** |

> **Tổng kết kết quả:** **18 / 18 Test Cases PASSED (100%)**.

---

## 3. Bảng theo dõi lỗi phát hiện & Tinh chỉnh (Bug Log & Refactoring)

| Mã lỗi | Mức độ | Mô tả lỗi phát hiện trong quá trình kiểm thử | Nguyên nhân gốc rễ | Giải pháp đã tinh chỉnh / sửa lỗi |
| :---: | :---: | :--- | :--- | :--- |
| **BUG-01** | **Cao (Critical)** | Tìm kiếm từ khóa tiếng Việt ngắn (< 3 ký tự như "bếp", "pha", "xe") bằng `MATCH ... AGAINST` không trả về kết quả | Mặc định cấu hình `innodb_ft_min_token_size=3` của MySQL bỏ qua từ khóa ngắn hơn 3 ký tự | **Áp dụng Plan B buổi 4:** Tinh chỉnh câu query trong `JobRepository` kết hợp `MATCH ... AGAINST` với `title LIKE %keyword%` fallback. |
| **BUG-02** | **Nghiêm trọng (High)** | Phân trang bị văng Exception khi gọi native query với `Pageable` | Thiếu câu truy vấn đếm `countQuery` trong `@Query` native | Bổ sung tường minh `countQuery` đồng bộ điều kiện lọc trong `JobRepository.java`. |
| **BUG-03** | **Trung bình (Medium)** | NTD có thể vô tình bấm Duyệt (`ACCEPTED`) vượt quá số lượng cần tuyển (`slots`) | Thiếu điều kiện đếm số lượng hồ sơ đã duyệt trước khi cập nhật | Bổ sung hàm `countByJobIdAndStatus(jobId, "ACCEPTED")` trong `ApplicationService`, nếu đã đủ thì trả lỗi HTTP 409. |
| **BUG-04** | **Thấp (Minor)** | `JobResponse` trả về `category` và `location` bị `null` | Hàm chuyển đổi DTO chỉ sao chép thuộc tính `Job` mà chưa join lấy tên bảng liên quan | Tích hợp `CategoryRepository` và `LocationRepository` vào `JobService` để nạp tên ngành nghề và định dạng địa điểm. |

---

## 4. Danh sách hạn chế đã biết (Known Limitations)
1. **Tìm kiếm tiếng Việt có dấu phức tạp:** Với các từ khóa có ký tự đặc biệt hoặc từ đồng nghĩa, hệ thống hiện dùng toán tử so khớp LIKE và FULLTEXT Boolean Mode cơ bản, chưa hỗ trợ tách từ (tokenization) chuyên sâu tiếng Việt.
2. **Xác thực phiên làm việc trong môi trường test độc lập:** Do phân hệ Xác thực (JWT) do bạn Khoa phụ trách đang được tích hợp song song, các test case hiện hỗ trợ cơ chế giả lập danh tính người dùng (`X-User-Id` / Mock Security) để đảm bảo kiểm thử nghiệp vụ độc lập không bị nghẽn.
