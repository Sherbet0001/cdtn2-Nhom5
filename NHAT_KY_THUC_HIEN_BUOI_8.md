# NHẬT KÝ THỰC HIỆN ĐỀ TÀI – BUỔI 8
**Đề tài:** Nền tảng quản lý freelance / marketplace việc làm part-time cho sinh viên  
**Nhóm sinh viên:**
- Viên Thành Đạt – 2374802010107 (Frontend ReactJS)
- **Đoàn Xuân Tiến – 2374802010493 (Backend: Module Tin tuyển dụng & Ứng tuyển)**
- Bùi Đăng Khoa – 2374802013428 (Backend: Module Xác thực, Đánh giá, Quản trị)  
**Giảng viên giảng dạy:** ThS. Nguyễn Văn Trung / GVHD: ThS. Hà Đồng Hưng  
**Nội dung buổi 8:** Kiểm thử và tinh chỉnh sản phẩm (Sprint 3)

---

## 1. Công việc đã thực hiện trong buổi 8
1. **Thiết kế và triển khai Test Cases (JUnit 5 & Mockito):**
   - Xây dựng 9 test cases đơn vị cho `JobServiceTest.java` (kiểm thử tạo tin, kiểm tra ràng buộc lương/ngày, xem chi tiết, sửa tin, máy trạng thái chuyển đổi).
   - Xây dựng 7 test cases đơn vị cho `ApplicationServiceTest.java` (kiểm thử nộp hồ sơ, chặn nộp vào tin đã đóng, chặn nộp lặp, duyệt hồ sơ, chặn duyệt vượt slots tuyển).
   - Xây dựng 2 test cases tích hợp MockMvc cho `JobControllerIntegrationTest.java`.
2. **Thực thi kiểm thử và ghi nhận lỗi (Bug Log):**
   - Toàn bộ **18/18 test cases** đã được chạy và đạt kết quả **PASSED**.
   - Phát hiện và tinh chỉnh 4 vấn đề kỹ thuật:
     * *BUG-01:* Từ khóa tiếng Việt ngắn bị FULLTEXT bỏ qua → Đã bổ sung LIKE fallback (Plan B).
     * *BUG-02:* Thiếu countQuery trong truy vấn native phân trang → Đã bổ sung tường minh.
     * *BUG-03:* Duyệt hồ sơ vượt số lượng cần tuyển → Đã thêm logic kiểm tra `countByJobIdAndStatus` trước khi duyệt.
     * *BUG-04:* Trả về thiếu tên ngành và địa điểm → Đã nạp từ `CategoryRepository` và `LocationRepository`.
3. **Biên soạn tài liệu bàn giao:**
   - Hoàn thành báo cáo kiểm thử chi tiết tại file [`BAO_CAO_KIEM_THU_BUOI_8.md`](BAO_CAO_KIEM_THU_BUOI_8.md).

---

## 2. Trả lời câu hỏi thảo luận nhanh (Slide 11 Buổi 8)

### Câu hỏi 1: Lỗi/vấn đề nào phát hiện ở buổi kiểm thử khiến nhóm bạn bất ngờ nhất so với lúc demo ở buổi 7?
> **Trả lời:** Vấn đề tìm kiếm từ khóa tiếng Việt ngắn bằng MySQL FULLTEXT Index. Khi demo ở buổi 7 với từ khóa dài như `"phục vụ"`, `"thu ngân"` thì hệ thống chạy rất tốt. Tuy nhiên sang buổi 8 khi viết test case cho các từ khóa ngắn như `"bếp"`, `"bar"`, `"pha"` thì kết quả trả về rỗng do cơ chế `innodb_ft_min_token_size=3` của MySQL InnoDB. Rất may mắn là nhóm đã chuẩn bị trước **Plan B** trong tài liệu kiến trúc Buổi 4 (kết hợp `LIKE` fallback), nên nhóm đã tinh chỉnh câu query ngay trong buổi để xử lý dứt điểm lỗi này.

### Câu hỏi 2: Nếu phải chọn 1 hạn chế để nêu thẳng thắn trong báo cáo, nhóm bạn sẽ chọn hạn chế nào?
> **Trả lời:** Nhóm sẽ nêu hạn chế về **xử lý ngôn ngữ tự nhiên trong tìm kiếm việc làm**. Hiện tại hệ thống dựa trên khớp từ khóa chính xác (FULLTEXT + LIKE) chứ chưa có bộ tiền xử lý từ đồng nghĩa (synonyms) hoặc tìm kiếm mờ (fuzzy search) cho tiếng Việt không dấu. Nhóm sẽ ghi nhận đây là "hạn chế đã biết" và đề xuất sử dụng Elasticsearch ở mục "Hướng phát triển tương lai" trong báo cáo cuối kỳ.

---

## 3. Kế hoạch chuẩn bị cho buổi tiếp theo (Buổi 9 - Triển khai sản phẩm và viết báo cáo)
- Ghép nối lần cuối toàn bộ module giữa 3 thành viên trên môi trường triển khai chung.
- Chuẩn bị slide thuyết trình và hoàn thiện báo cáo đề tài theo mẫu của Khoa CNTT - ĐH Văn Lang.
- Tập dượt bảo vệ đề tài trước hội đồng (Rubric 3 - 60%).
