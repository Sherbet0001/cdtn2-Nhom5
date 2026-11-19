-- ========================================================
-- Dữ liệu mẫu kiểm thử và demo Sprint 2 / Sprint 3
-- Đề tài: Nền tảng marketplace việc làm part-time cho sinh viên
-- Nhóm: Viên Thành Đạt · Đoàn Xuân Tiến · Bùi Đăng Khoa
-- ========================================================

CREATE DATABASE IF NOT EXISTS `freelance_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `freelance_db`;

-- 1. Bảng danh mục ngành nghề (tbl_categories - FR-09, 28)
CREATE TABLE IF NOT EXISTS `tbl_categories` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `tbl_categories` (`id`, `name`) VALUES
(1, 'Phục vụ - Nhà hàng'),
(2, 'Bán hàng - Thu ngân'),
(3, 'Gia sư - Trợ giảng'),
(4, 'Tổ chức sự kiện'),
(5, 'Giao hàng - Kho vận'),
(6, 'Lập trình viên thực tập'),
(7, 'Thiết kế đồ họa'),
(8, 'Dịch thuật - Viết lách');

-- 2. Bảng địa điểm làm việc (tbl_locations - FR-09, 29)
CREATE TABLE IF NOT EXISTS `tbl_locations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `city` VARCHAR(80) NOT NULL,
    `district` VARCHAR(80) NOT NULL,
    UNIQUE KEY `uq_location` (`city`, `district`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `tbl_locations` (`id`, `city`, `district`) VALUES
(1, 'TP. Hồ Chí Minh', 'Quận 1'),
(2, 'TP. Hồ Chí Minh', 'Quận 3'),
(3, 'TP. Hồ Chí Minh', 'Quận 7'),
(4, 'TP. Hồ Chí Minh', 'Quận Bình Thạnh'),
(5, 'TP. Hồ Chí Minh', 'Quận Gò Vấp'),
(6, 'TP. Hồ Chí Minh', 'TP. Thủ Đức'),
(7, 'TP. Hồ Chí Minh', 'Quận 10'),
(8, 'TP. Hồ Chí Minh', 'Quận Phú Nhuận'),
(9, 'TP. Hồ Chí Minh', 'Quận Tân Bình'),
(10, 'TP. Hồ Chí Minh', 'Quận 5');

-- 3. Bảng tài khoản người dùng mẫu (tbl_users - FR-01, 02, 04, 26)
-- Mật khẩu mặc định cho tất cả tài khoản mẫu là: Abc@12345
CREATE TABLE IF NOT EXISTS `tbl_users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `password_hash` VARCHAR(100) NOT NULL,
    `role` ENUM('STUDENT','EMPLOYER','ADMIN') NOT NULL,
    `status` ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `tbl_users` (`id`, `email`, `password_hash`, `role`, `status`, `created_at`) VALUES
(1, 'admin@freelance.vlu.edu.vn', 'Abc@12345', 'ADMIN', 'ACTIVE', NOW()),
(2, 'cafegocpho@gmail.com', 'Abc@12345', 'EMPLOYER', 'ACTIVE', NOW()),
(3, 'thecoffeelab@gmail.com', 'Abc@12345', 'EMPLOYER', 'ACTIVE', NOW()),
(4, 'events.saigon@gmail.com', 'Abc@12345', 'EMPLOYER', 'ACTIVE', NOW()),
(5, 'tien.sv@student.vlu.edu.vn', 'Abc@12345', 'STUDENT', 'ACTIVE', NOW()),
(6, 'dat.sv@student.vlu.edu.vn', 'Abc@12345', 'STUDENT', 'ACTIVE', NOW()),
(7, 'khoa.sv@student.vlu.edu.vn', 'Abc@12345', 'STUDENT', 'ACTIVE', NOW());

-- 4. Bảng hồ sơ người dùng (tbl_profiles - FR-05, 11, 16, 20)
CREATE TABLE IF NOT EXISTS `tbl_profiles` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `full_name` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20),
    `location_id` BIGINT,
    `bio` TEXT,
    `school` VARCHAR(100),
    `major` VARCHAR(100),
    `cv_content` TEXT,
    `company_name` VARCHAR(150),
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `tbl_profiles` (`id`, `user_id`, `full_name`, `phone`, `location_id`, `bio`, `school`, `major`, `cv_content`, `company_name`) VALUES
(1, 1, 'Quản trị viên hệ thống', '0901234567', 1, 'Ban quản trị nền tảng VLU Freelance', NULL, NULL, NULL, NULL),
(2, 2, 'Cafe Góc Phố', '0912345678', 3, 'Hệ thống quán cà phê sinh viên khu vực Quận 7', NULL, NULL, NULL, 'Công ty TNHH Cafe Góc Phố'),
(3, 3, 'The Coffee Lab', '0923456789', 5, 'Chuỗi đồ uống mang đi', NULL, NULL, NULL, 'Hộ kinh doanh The Coffee Lab'),
(4, 4, 'Saigon Events Group', '0934567890', 1, 'Tổ chức sự kiện âm nhạc, hội thảo', NULL, NULL, NULL, 'Saigon Events Group'),
(5, 5, 'Đoàn Xuân Tiến', '0945678901', 3, 'Sinh viên năm 4 CNTT ĐH Văn Lang', 'Đại học Văn Lang', 'Công nghệ thông tin', 'Kinh nghiệm phục vụ bàn, pha chế cơ bản, nhiệt tình', NULL),
(6, 6, 'Viên Thành Đạt', '0956789012', 4, 'Sinh viên năm 4 khoa CNTT', 'Đại học Văn Lang', 'Công nghệ phần mềm', 'Kinh nghiệm thu ngân và giao hàng part-time', NULL),
(7, 7, 'Bùi Đăng Khoa', '0967890123', 2, 'Sinh viên năm 4 khoa CNTT', 'Đại học Văn Lang', 'Kỹ thuật phần mềm', 'Kinh nghiệm gia sư Toán và Anh văn', NULL);

-- 5. Bảng tin tuyển dụng (tbl_jobs - FR-07 đến 10, 17, 18, 23)
CREATE TABLE IF NOT EXISTS `tbl_jobs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `employer_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `location_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT NOT NULL,
    `salary_min` DECIMAL(12,0) NOT NULL,
    `salary_max` DECIMAL(12,0) NOT NULL,
    `salary_unit` ENUM('HOUR','DAY','MONTH','TASK') NOT NULL,
    `work_schedule` VARCHAR(200) NOT NULL,
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `slots` INT NOT NULL,
    `status` ENUM('OPEN','FILLED','COMPLETED','CLOSED','HIDDEN') NOT NULL DEFAULT 'OPEN',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FULLTEXT KEY `ft_job_search` (`title`, `description`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `tbl_jobs` (`id`, `employer_id`, `category_id`, `location_id`, `title`, `description`, `salary_min`, `salary_max`, `salary_unit`, `work_schedule`, `start_date`, `end_date`, `slots`, `status`, `created_at`) VALUES
(1, 2, 1, 3, 'Nhân viên phục vụ quán cà phê cuối tuần', 'Bưng bê, phục vụ nước, order món cơ bản. Được đào tạo kỹ năng ban đầu.', 28000, 32000, 'HOUR', 'Thứ 7, CN (18:00 - 22:30)', '2026-11-01', '2026-12-31', 3, 'OPEN', NOW()),
(2, 2, 2, 4, 'Nhân viên thu ngân bán thời gian ca sáng', 'Thu ngân tại quầy bánh ngọt, kiểm soát hóa đơn và tính tiền cho khách qua POS.', 30000, 35000, 'HOUR', 'Thứ 2 đến Thứ 6 (07:00 - 12:00)', '2026-11-05', '2026-12-31', 2, 'OPEN', NOW()),
(3, 3, 3, 5, 'Gia sư dạy kèm Toán lớp 9 cho học sinh cấp 2', 'Ôn thi vào lớp 10, yêu cầu sinh viên kiên nhẫn, có phương pháp dạy dễ hiểu.', 150000, 200000, 'HOUR', 'Tối thứ 3, 5 (19:00 - 21:00)', '2026-11-10', '2026-12-31', 1, 'OPEN', NOW()),
(4, 4, 4, 1, 'Hỗ trợ sự kiện âm nhạc cuối tuần tại Quận 1', 'Đón khách, soát vé check-in, hướng dẫn vị trí ngồi và hỗ trợ ban tổ chức sân khấu.', 300000, 450000, 'DAY', 'Chủ nhật (13:00 - 22:00)', '2026-11-15', '2026-11-15', 5, 'OPEN', NOW()),
(5, 2, 1, 3, 'Phụ bếp và dọn dẹp quán nước', 'Hỗ trợ sơ chế nguyên liệu trà sữa, dọn dẹp quầy kệ cuối ca.', 25000, 28000, 'HOUR', 'Ca tối thứ 6, 7', '2026-10-01', '2026-10-25', 2, 'COMPLETED', NOW());

-- 6. Bảng hồ sơ ứng tuyển (tbl_applications - FR-12 đến 14, 19, 21, 22)
CREATE TABLE IF NOT EXISTS `tbl_applications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `job_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `cover_letter` TEXT,
    `status` ENUM('PENDING','ACCEPTED','REJECTED') NOT NULL DEFAULT 'PENDING',
    `applied_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uq_job_student` (`job_id`, `student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `tbl_applications` (`id`, `job_id`, `student_id`, `cover_letter`, `status`, `applied_at`) VALUES
(1, 1, 5, 'Em rảnh tối thứ 7 và chủ nhật, đã có kinh nghiệm phục vụ quán nước.', 'PENDING', NOW()),
(2, 1, 6, 'Em là sinh viên năm 2 ĐH Văn Lang, mong muốn tìm việc làm thêm cuối tuần.', 'ACCEPTED', NOW()),
(3, 2, 5, 'Em thành thạo sử dụng máy tính và tính tiền cẩn thận.', 'PENDING', NOW());
