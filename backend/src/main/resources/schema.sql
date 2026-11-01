-- ==============================================================================
-- SCHEMA.SQL - NỀN TẢNG VIỆC LÀM PART-TIME CHO SINH VIÊN (CHUẨN HÓA 8 BẢNG)
-- Database: MySQL 8.0 (InnoDB, utf8mb4_unicode_ci)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS tbl_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uk_categories_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tbl_locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    city VARCHAR(80) NOT NULL,
    district VARCHAR(80) NOT NULL,
    CONSTRAINT uk_city_district UNIQUE (city, district)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tbl_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role ENUM('STUDENT', 'EMPLOYER', 'ADMIN') NOT NULL,
    status ENUM('ACTIVE', 'LOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tbl_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    location_id BIGINT DEFAULT NULL,
    bio TEXT DEFAULT NULL,
    school VARCHAR(100) DEFAULT NULL,
    major VARCHAR(100) DEFAULT NULL,
    cv_content TEXT DEFAULT NULL,
    company_name VARCHAR(150) DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES tbl_users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_profiles_location FOREIGN KEY (location_id) REFERENCES tbl_locations (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tbl_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employer_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    salary_min DECIMAL(12,0) NOT NULL,
    salary_max DECIMAL(12,0) NOT NULL,
    salary_unit ENUM('HOUR', 'DAY', 'MONTH', 'TASK') NOT NULL DEFAULT 'HOUR',
    work_schedule VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    slots INT NOT NULL DEFAULT 1,
    status ENUM('OPEN', 'FILLED', 'COMPLETED', 'CLOSED', 'HIDDEN') NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_jobs_employer FOREIGN KEY (employer_id) REFERENCES tbl_users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_jobs_category FOREIGN KEY (category_id) REFERENCES tbl_categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_jobs_location FOREIGN KEY (location_id) REFERENCES tbl_locations (id) ON DELETE RESTRICT,
    INDEX idx_jobs_filter (status, category_id, location_id, created_at),
    INDEX idx_jobs_employer (employer_id, status),
    FULLTEXT INDEX idx_jobs_fulltext (title, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tbl_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    cover_letter TEXT DEFAULT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_job_student UNIQUE (job_id, student_id),
    CONSTRAINT fk_app_job FOREIGN KEY (job_id) REFERENCES tbl_jobs (id) ON DELETE RESTRICT,
    CONSTRAINT fk_app_student FOREIGN KEY (student_id) REFERENCES tbl_users (id) ON DELETE RESTRICT,
    INDEX idx_app_job_status (job_id, status),
    INDEX idx_app_student_history (student_id, applied_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tbl_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    reviewee_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    comment VARCHAR(1000) DEFAULT NULL,
    status ENUM('VISIBLE', 'HIDDEN') NOT NULL DEFAULT 'VISIBLE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_review_pair UNIQUE (job_id, reviewer_id, reviewee_id),
    CONSTRAINT fk_reviews_job FOREIGN KEY (job_id) REFERENCES tbl_jobs (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES tbl_users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_reviewee FOREIGN KEY (reviewee_id) REFERENCES tbl_users (id) ON DELETE RESTRICT,
    INDEX idx_reviews_reviewee (reviewee_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tbl_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    application_id BIGINT DEFAULT NULL,
    type ENUM('APPLICATION_NEW', 'APPLICATION_ACCEPTED', 'APPLICATION_REJECTED', 'JOB_COMPLETED') NOT NULL,
    message VARCHAR(300) NOT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES tbl_users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_notifications_app FOREIGN KEY (application_id) REFERENCES tbl_applications (id) ON DELETE RESTRICT,
    INDEX idx_notifications_user_read (user_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
