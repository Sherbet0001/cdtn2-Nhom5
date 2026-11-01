-- ==============================================================================
-- DATA.SQL - DỮ LIỆU MẪU CHUẨN CHO SPRINT 1 (DANH MỤC, ĐỊA ĐIỂM, TÀI KHOẢN TEST)
-- Mật khẩu mặc định của tất cả tài khoản mẫu: Abc@12345 (BCrypt Hash)
-- ==============================================================================

-- 1. Danh mục ngành nghề (8 ngành nghề phổ biến cho sinh viên)
INSERT IGNORE INTO tbl_categories (id, name) VALUES
(1, 'Phục vụ & F&B'),
(2, 'Gia sư & Trợ giảng'),
(3, 'Bán hàng & Thu ngân'),
(4, 'Sự kiện & PG/PB'),
(5, 'Thiết kế & Sáng tạo nội dung'),
(6, 'Công nghệ thông tin & Lập trình'),
(7, 'Hành chính & Nhập liệu'),
(8, 'Giao hàng & Kho vận');

-- 2. Danh mục địa điểm làm việc (10 quận/huyện tại TP. Hồ Chí Minh)
INSERT IGNORE INTO tbl_locations (id, city, district) VALUES
(1, 'TP. Hồ Chí Minh', 'Quận 1'),
(2, 'TP. Hồ Chí Minh', 'Quận 3'),
(3, 'TP. Hồ Chí Minh', 'Quận 7'),
(4, 'TP. Hồ Chí Minh', 'Quận 10'),
(5, 'TP. Hồ Chí Minh', 'Quận Bình Thạnh'),
(6, 'TP. Hồ Chí Minh', 'Quận Gò Vấp'),
(7, 'TP. Hồ Chí Minh', 'Quận Phú Nhuận'),
(8, 'TP. Hồ Chí Minh', 'Quận Tân Bình'),
(9, 'TP. Hồ Chí Minh', 'TP. Thủ Đức'),
(10, 'TP. Hồ Chí Minh', 'Quận 5');

-- 3. Tài khoản kiểm thử (1 ADMIN, 2 EMPLOYER, 3 STUDENT)
-- BCrypt hash dưới đây tương ứng với mật khẩu: Abc@12345
INSERT IGNORE INTO tbl_users (id, email, password_hash, role, status, created_at) VALUES
(1, 'admin@vlu.edu.vn', '$2a$10$LSU3FwHUjHQHf0nIYmoDL.X4UkTxE/Sua4irDX22k4bqfWCQxdj7a', 'ADMIN', 'ACTIVE', '2026-10-01 08:00:00'),
(2, 'hr@cafegocpho.vn', '$2a$10$LSU3FwHUjHQHf0nIYmoDL.X4UkTxE/Sua4irDX22k4bqfWCQxdj7a', 'EMPLOYER', 'ACTIVE', '2026-10-02 09:00:00'),
(3, 'tuyendung@eduviet.vn', '$2a$10$LSU3FwHUjHQHf0nIYmoDL.X4UkTxE/Sua4irDX22k4bqfWCQxdj7a', 'EMPLOYER', 'ACTIVE', '2026-10-03 10:00:00'),
(4, 'sv1@vanlanguni.vn', '$2a$10$LSU3FwHUjHQHf0nIYmoDL.X4UkTxE/Sua4irDX22k4bqfWCQxdj7a', 'STUDENT', 'ACTIVE', '2026-10-04 11:00:00'),
(5, 'sv2@vanlanguni.vn', '$2a$10$LSU3FwHUjHQHf0nIYmoDL.X4UkTxE/Sua4irDX22k4bqfWCQxdj7a', 'STUDENT', 'ACTIVE', '2026-10-05 14:00:00'),
(6, 'sv3@vanlanguni.vn', '$2a$10$LSU3FwHUjHQHf0nIYmoDL.X4UkTxE/Sua4irDX22k4bqfWCQxdj7a', 'STUDENT', 'ACTIVE', '2026-10-06 15:30:00');

-- 4. Hồ sơ cá nhân tương ứng 1-1 với tbl_users
INSERT IGNORE INTO tbl_profiles (id, user_id, full_name, phone, location_id, bio, school, major, cv_content, company_name, updated_at) VALUES
(1, 1, 'Quản trị viên Hệ thống', '0901000111', 1, 'Quản trị viên nền tảng việc làm part-time sinh viên Văn Lang', NULL, NULL, NULL, 'VLU Career Center', '2026-10-01 08:00:00'),
(2, 2, 'Nguyễn Minh Hoàng', '0902111222', 3, 'Chuỗi cà phê acoustic và không gian học tập dành cho sinh viên tại Quận 7 và Bình Thạnh.', NULL, NULL, NULL, 'Cafe Góc Phố', '2026-10-02 09:00:00'),
(3, 3, 'Trần Thu Hà', '0903222333', 5, 'Trung tâm Anh ngữ và Gia sư kỹ năng mềm cho học sinh - sinh viên tại TP.HCM.', NULL, NULL, NULL, 'Trung tâm Giáo dục EduViet', '2026-10-03 10:00:00'),
(4, 4, 'Lê Hoàng Nam', '0914333444', 6, 'Sinh viên năm 3 ngành Công nghệ phần mềm, cẩn thận, trách nhiệm và có khả năng làm việc nhóm tốt.', 'Đại học Văn Lang', 'Công nghệ phần mềm', 'Kỹ năng: Java, Spring Boot, MySQL, Git. Kinh nghiệm: Từng làm trợ giảng lập trình cơ bản 1 học kỳ. Thời gian rảnh: Ca tối Thứ 2 - Thứ 6 và cả ngày cuối tuần.', NULL, '2026-10-04 11:00:00'),
(5, 5, 'Phạm Minh Châu', '0915444555', 5, 'Sinh viên năng động, giao tiếp tốt, mong muốn tìm công việc part-time lĩnh vực công nghệ hoặc phục vụ.', 'Đại học Văn Lang', 'Công nghệ thông tin', 'Kỹ năng: SQL, RESTful API, Giao tiếp tiếng Anh cơ bản. Kinh nghiệm: Phục vụ sự kiện tại trường Đại học Văn Lang. Thời gian rảnh: Chiều Thứ 3, Thứ 5, Thứ 7, CN.', NULL, '2026-10-05 14:00:00'),
(6, 6, 'Võ Quốc Bảo', '0916555666', 1, 'Sinh viên yêu thích Frontend & UI/UX, từng tham gia hỗ trợ truyền thông sự kiện khoa CNTT.', 'Đại học Văn Lang', 'Công nghệ phần mềm', 'Kỹ năng: ReactJS, Tailwind CSS, Figma, Chụp ảnh sự kiện. Kinh nghiệm: Cộng tác viên thiết kế ấn phẩm CLB. Thời gian rảnh: Các buổi tối trong tuần.', NULL, '2026-10-06 15:30:00');
