-- ========================================================
-- Dữ liệu mẫu kiểm thử và demo Sprint 2 (Buổi 7 - Đánh giá giữa kỳ lần 2)
-- Đề tài: Nền tảng marketplace việc làm part-time cho sinh viên
-- Nhóm: Viên Thành Đạt · Đoàn Xuân Tiến · Bùi Đăng Khoa
-- Phụ trách module Job & Application: Đoàn Xuân Tiến - 2374802010493
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
CREATE TABLE IF NOT EXISTS `tbl_users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `password_hash` VARCHAR(100) NOT NULL,
    `role` ENUM('STUDENT','EMPLOYER','ADMIN') NOT NULL,
    `status` ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `tbl_users` (`id`, `email`, `password_hash`, `role`, `status`, `created_at`) VALUES
(1, 'admin@freelance.vlu.edu.vn', '$2a$10$wN9aZk1cE1F8l7K.0a6rUuJ6QoO7h3mI9c3bZ1a2c3d4e5f6g7h8i', 'ADMIN', 'ACTIVE', NOW()),
(2, 'cafegocpho@gmail.com', '$2a$10$wN9aZk1cE1F8l7K.0a6rUuJ6QoO7h3mI9c3bZ1a2c3d4e5f6g7h8i', 'EMPLOYER', 'ACTIVE', NOW()),
(3, 'thecoffeelab@gmail.com', '$2a$10$wN9aZk1cE1F8l7K.0a6rUuJ6QoO7h3mI9c3bZ1a2c3d4e5f6g7h8i', 'EMPLOYER', 'ACTIVE', NOW()),
(4, 'events.saigon@gmail.com', '$2a$10$wN9aZk1cE1F8l7K.0a6rUuJ6QoO7h3mI9c3bZ1a2c3d4e5f6g7h8i', 'EMPLOYER', 'ACTIVE', NOW()),
(5, 'tien.sv@student.vlu.edu.vn', '$2a$10$wN9aZk1cE1F8l7K.0a6rUuJ6QoO7h3mI9c3bZ1a2c3d4e5f6g7h8i', 'STUDENT', 'ACTIVE', NOW()),
(6, 'dat.sv@student.vlu.edu.vn', '$2a$10$wN9aZk1cE1F8l7K.0a6rUuJ6QoO7h3mI9c3bZ1a2c3d4e5f6g7h8i', 'STUDENT', 'ACTIVE', NOW()),
(7, 'khoa.sv@student.vlu.edu.vn', '$2a$10$wN9aZk1cE1F8l7K.0a6rUuJ6QoO7h3mI9c3bZ1a2c3d4e5f6g7h8i', 'STUDENT', 'ACTIVE', NOW());

-- 4. Bảng tin tuyển dụng (tbl_jobs - FR-07 đến 10, 17, 18, 23)
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

-- 5. Bảng hồ sơ ứng tuyển (tbl_applications - FR-12 đến 14, 19, 21, 22)
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
