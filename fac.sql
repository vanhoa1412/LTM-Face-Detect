CREATE DATABASE IF NOT EXISTS face_detector_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE face_detector_db;
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL -- Sẽ lưu mật khẩu thô
);

CREATE TABLE IF NOT EXISTS image_jobs (
    job_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    upload_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completion_timestamp TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS processed_images (
    image_id INT AUTO_INCREMENT PRIMARY KEY,
    job_id INT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    original_filepath VARCHAR(512) NOT NULL,
    face_crop_filepath VARCHAR(512) NULL,    -- Lưu đường dẫn ảnh khuôn mặt đã cắt
    detection_details VARCHAR(255) NULL, -- Lưu thông tin phát hiện (ví dụ: "1 face detected")
    FOREIGN KEY (job_id) REFERENCES image_jobs(job_id) ON DELETE CASCADE
);
