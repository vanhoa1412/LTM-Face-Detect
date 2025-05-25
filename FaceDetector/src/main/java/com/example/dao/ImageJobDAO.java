package com.example.dao;

import com.example.model.ImageJob;
import com.example.model.ProcessedImage;
import com.example.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ImageJobDAO {

    public int createJob(ImageJob job) {
        if (job == null) return -1;
        String sql = "INSERT INTO image_jobs (user_id, status, upload_timestamp) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, job.getUserId());
            pstmt.setString(2, job.getStatus() != null ? job.getStatus() : "PENDING");
            pstmt.setTimestamp(3, job.getUploadTimestamp() != null ? job.getUploadTimestamp() : new Timestamp(System.currentTimeMillis()));
            if (pstmt.executeUpdate() > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) { System.err.println("SQL Error in createJob: " + e.getMessage()); }
        return -1;
    }

    public int addProcessedImage(ProcessedImage image) {
        if (image == null) return -1;
        String sql = "INSERT INTO processed_images (job_id, original_filename, original_filepath) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, image.getJobId());
            pstmt.setString(2, image.getOriginalFilename());
            pstmt.setString(3, image.getOriginalFilepath());
            if (pstmt.executeUpdate() > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) { System.err.println("SQL Error in addProcessedImage for job " + image.getJobId() + ": " + e.getMessage());}
        return -1;
    }

    public boolean updateJobStatus(int jobId, String status) {
        if (status == null || status.trim().isEmpty()) return false;
        String sql = ("COMPLETED".equalsIgnoreCase(status.trim()) || "FAILED".equalsIgnoreCase(status.trim())) ?
                     "UPDATE image_jobs SET status = ?, completion_timestamp = CURRENT_TIMESTAMP WHERE job_id = ?" :
                     "UPDATE image_jobs SET status = ? WHERE job_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.trim());
            pstmt.setInt(2, jobId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("SQL Error in updateJobStatus for job " + jobId + " to " + status + ": " + e.getMessage());}
        return false;
    }

    public boolean updateJobStatusIfPending(int jobId, String newStatus) {
        if (newStatus == null || newStatus.trim().isEmpty()) return false;
        String sql = "UPDATE image_jobs SET status = ? WHERE job_id = ? AND status = 'PENDING'";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.trim());
            pstmt.setInt(2, jobId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("SQL Error in updateJobStatusIfPending for job " + jobId + ": " + e.getMessage());}
        return false;
    }

    public boolean updateFaceCropPath(int imageId, String faceCropFilepath) {
        // Cho phép faceCropFilepath là null nếu không có khuôn mặt
        String sql = "UPDATE processed_images SET face_crop_filepath = ? WHERE image_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (faceCropFilepath != null && !faceCropFilepath.trim().isEmpty()) {
                pstmt.setString(1, faceCropFilepath.trim());
            } else {
                pstmt.setNull(1, Types.VARCHAR);
            }
            pstmt.setInt(2, imageId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("SQL Error in updateFaceCropPath for image " + imageId + ": " + e.getMessage());}
        return false;
    }

    public boolean updateDetectionDetails(int imageId, String details) {
        String sql = "UPDATE processed_images SET detection_details = ? WHERE image_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, details);
            pstmt.setInt(2, imageId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("SQL Error in updateDetectionDetails for image " + imageId + ": " + e.getMessage());}
        return false;
    }

    public ImageJob getJobById(int jobId) {
        String sql = "SELECT job_id, user_id, status, upload_timestamp, completion_timestamp FROM image_jobs WHERE job_id = ?";
        ImageJob job = null;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, jobId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    job = new ImageJob();
                    job.setJobId(rs.getInt("job_id"));
                    job.setUserId(rs.getInt("user_id"));
                    job.setStatus(rs.getString("status"));
                    job.setUploadTimestamp(rs.getTimestamp("upload_timestamp"));
                    job.setCompletionTimestamp(rs.getTimestamp("completion_timestamp"));
                }
            }
        } catch (SQLException e) { System.err.println("SQL Error in getJobById for job " + jobId + ": " + e.getMessage());}
        return job;
    }

    public List<ImageJob> getJobsByUser(int userId) {
        List<ImageJob> jobs = new ArrayList<>();
        String sql = "SELECT job_id, user_id, status, upload_timestamp, completion_timestamp FROM image_jobs WHERE user_id = ? ORDER BY upload_timestamp DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ImageJob job = new ImageJob();
                    // ... (set properties cho job) ...
                    job.setJobId(rs.getInt("job_id"));
                    job.setUserId(rs.getInt("user_id"));
                    job.setStatus(rs.getString("status"));
                    job.setUploadTimestamp(rs.getTimestamp("upload_timestamp"));
                    job.setCompletionTimestamp(rs.getTimestamp("completion_timestamp"));
                    jobs.add(job);
                }
            }
        } catch (SQLException e) { System.err.println("SQL Error in getJobsByUser for user " + userId + ": " + e.getMessage());}
        return jobs;
    }

    public ProcessedImage getProcessedImageById(int imageId) {
        String sql = "SELECT image_id, job_id, original_filename, original_filepath, face_crop_filepath, detection_details " +
                     "FROM processed_images WHERE image_id = ?";
        ProcessedImage image = null;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, imageId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    image = new ProcessedImage();
                    image.setImageId(rs.getInt("image_id"));
                    image.setJobId(rs.getInt("job_id"));
                    image.setOriginalFilename(rs.getString("original_filename"));
                    image.setOriginalFilepath(rs.getString("original_filepath"));
                    image.setFaceCropFilepath(rs.getString("face_crop_filepath"));
                    image.setDetectionDetails(rs.getString("detection_details"));
                }
            }
        } catch (SQLException e) { System.err.println("SQL Error in getProcessedImageById for image " + imageId + ": " + e.getMessage());}
        return image;
    }

    public List<ProcessedImage> getImagesByJob(int jobId) {
        List<ProcessedImage> images = new ArrayList<>();
        String sql = "SELECT image_id, job_id, original_filename, original_filepath, face_crop_filepath, detection_details " +
                     "FROM processed_images WHERE job_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, jobId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ProcessedImage img = new ProcessedImage();
                    // ... (set properties cho img) ...
                    img.setImageId(rs.getInt("image_id"));
                    img.setJobId(rs.getInt("job_id"));
                    img.setOriginalFilename(rs.getString("original_filename"));
                    img.setOriginalFilepath(rs.getString("original_filepath"));
                    img.setFaceCropFilepath(rs.getString("face_crop_filepath"));
                    img.setDetectionDetails(rs.getString("detection_details"));
                    images.add(img);
                }
            }
        } catch (SQLException e) { System.err.println("SQL Error in getImagesByJob for job " + jobId + ": " + e.getMessage());}
        return images;
    }

    public boolean areAllFacesProcessedForJob(int jobId) {
        String sql = "SELECT COUNT(*) AS pending_detection FROM processed_images WHERE job_id = ? AND detection_details IS NULL";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, jobId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("pending_detection") == 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in areAllFacesProcessedForJob for job " + jobId + ": " + e.getMessage());
        }
        return false;
    }
}