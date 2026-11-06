-- ==============================================================================
-- SCHEMA-H2.SQL - KHỞI TẠO 8 BẢNG TƯƠNG THÍCH H2 (MODE=MySQL) CHO TEST & OFFLINE
-- ==============================================================================

CREATE TABLE IF NOT EXISTS tbl_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS tbl_locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    city VARCHAR(80) NOT NULL,
    district VARCHAR(80) NOT NULL,
    CONSTRAINT uk_city_district UNIQUE (city, district)
);

CREATE TABLE IF NOT EXISTS tbl_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
);

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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES tbl_users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_profiles_location FOREIGN KEY (location_id) REFERENCES tbl_locations (id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS tbl_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employer_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    salary_min DECIMAL(12,0) NOT NULL,
    salary_max DECIMAL(12,0) NOT NULL,
    salary_unit VARCHAR(20) NOT NULL DEFAULT 'HOUR',
    work_schedule VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    slots INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_jobs_employer FOREIGN KEY (employer_id) REFERENCES tbl_users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_jobs_category FOREIGN KEY (category_id) REFERENCES tbl_categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_jobs_location FOREIGN KEY (location_id) REFERENCES tbl_locations (id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS tbl_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    cover_letter TEXT DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_job_student UNIQUE (job_id, student_id),
    CONSTRAINT fk_app_job FOREIGN KEY (job_id) REFERENCES tbl_jobs (id) ON DELETE RESTRICT,
    CONSTRAINT fk_app_student FOREIGN KEY (student_id) REFERENCES tbl_users (id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS tbl_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    reviewee_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    comment VARCHAR(1000) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_review_pair UNIQUE (job_id, reviewer_id, reviewee_id),
    CONSTRAINT fk_reviews_job FOREIGN KEY (job_id) REFERENCES tbl_jobs (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES tbl_users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_reviewee FOREIGN KEY (reviewee_id) REFERENCES tbl_users (id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS tbl_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    application_id BIGINT DEFAULT NULL,
    type VARCHAR(40) NOT NULL,
    message VARCHAR(300) NOT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES tbl_users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_notifications_app FOREIGN KEY (application_id) REFERENCES tbl_applications (id) ON DELETE RESTRICT
);

