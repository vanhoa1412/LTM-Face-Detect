package com.example.service;

import java.io.IOException;

import com.example.dao.ImageJobDAO;
import com.example.model.ImageJob;
import com.example.model.ProcessedImage;

public class ImageProcessingTask implements Runnable {
    private final ProcessedImage imageInfo;
    private final int jobId;
    private final ImageJobDAO imageJobDAO;
    private final FaceDetectionService faceDetectionService; // Sử dụng service này
    private final String faceCropOutputDirectory;    // Thư mục lưu ảnh khuôn mặt đã cắt

    public ImageProcessingTask(ProcessedImage imageInfo, int jobId,
                               ImageJobDAO imageJobDAO, FaceDetectionService faceDetectionService,
                               String faceCropOutputDirectory) {

        if (imageInfo == null || imageJobDAO == null || faceDetectionService == null || faceCropOutputDirectory == null) {
            throw new IllegalArgumentException("All arguments for ImageProcessingTask (Face Detection) must be non-null.");
        }
        this.imageInfo = imageInfo;
        this.jobId = jobId;
        this.imageJobDAO = imageJobDAO;
        this.faceDetectionService = faceDetectionService;
        this.faceCropOutputDirectory = faceCropOutputDirectory;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        System.out.println("[" + threadName + "] Starting face detection processing for imageId: " + imageInfo.getImageId() +
                           ", original file: '" + imageInfo.getOriginalFilename() + "' in JobId: " + jobId);

        String faceCropFilepath = null; // Đường dẫn file ảnh khuôn mặt đã cắt
        String detectionDetails = "Processing error occurred before detection."; // Thông tin phát hiện

        try {
            // 1. Cập nhật trạng thái job sang PROCESSING nếu nó đang PENDING
            imageJobDAO.updateJobStatusIfPending(this.jobId, "PROCESSING");

            // 2. Gọi FaceDetectionService để phát hiện và cắt khuôn mặt
            Object[] detectionResult = faceDetectionService.detectAndCropFirstFace(
                imageInfo.getOriginalFilepath(),
                faceCropOutputDirectory,
                imageInfo.getOriginalFilename()
            );

            faceCropFilepath = (String) detectionResult[0]; // Có thể là null
            detectionDetails = (String) detectionResult[1];

            // 3. Cập nhật thông tin vào CSDL
            // Cập nhật đường dẫn ảnh khuôn mặt đã cắt (nếu có)
            imageJobDAO.updateFaceCropPath(imageInfo.getImageId(), faceCropFilepath);
            // Cập nhật chi tiết phát hiện
            imageJobDAO.updateDetectionDetails(imageInfo.getImageId(), detectionDetails);

            System.out.println("[" + threadName + "] Face detection details updated for imageId: " + imageInfo.getImageId() +
                               " - Details: '" + detectionDetails +
                               (faceCropFilepath != null ? "', Cropped to: '" + faceCropFilepath + "'" : "'"));

            // 4. Kiểm tra xem tất cả ảnh trong job này đã được xử lý (có detection_details) chưa
            if (imageJobDAO.areAllFacesProcessedForJob(this.jobId)) {
                ImageJob currentJob = imageJobDAO.getJobById(this.jobId);
                // Chỉ cập nhật COMPLETED nếu trạng thái hiện tại không phải là FAILED
                // (để tránh ghi đè nếu một task khác trong job đã đánh dấu FAILED)
                if (currentJob != null && !"FAILED".equals(currentJob.getStatus())) {
                    imageJobDAO.updateJobStatus(this.jobId, "COMPLETED");
                    System.out.println("[" + threadName + "] All images processed (face detection) for JobId: " + this.jobId + ". Marked as COMPLETED.");
                }
            }

        } catch (IOException ioe) { // Lỗi từ FaceDetectionService (đọc/ghi file, load cascade)
            System.err.println("[" + threadName + "] IO ERROR during face detection for imageId: " + imageInfo.getImageId() +
                               " ('" + imageInfo.getOriginalFilename() + "') in JobId: " + jobId + ". Error: " + ioe.getMessage());
            // ioe.printStackTrace(); // Bỏ comment để debug sâu hơn
            detectionDetails = "Error during processing: " + ioe.getMessage().substring(0, Math.min(ioe.getMessage().length(), 150));
            imageJobDAO.updateDetectionDetails(imageInfo.getImageId(), detectionDetails);
            handleProcessingError(threadName, "File I/O or face detection service error");
        }
        catch (UnsatisfiedLinkError ule) { // Lỗi nghiêm trọng nếu OpenCV native library không load được lúc runtime
            System.err.println("[" + threadName + "] UNSATISFIED LINK ERROR for imageId: " + imageInfo.getImageId() + ". OpenCV NATIVE LIBRARY NOT LOADED. Error: " + ule.getMessage());
            // ule.printStackTrace();
            detectionDetails = "Critical Error: OpenCV native library not available.";
            imageJobDAO.updateDetectionDetails(imageInfo.getImageId(), detectionDetails);
            handleProcessingError(threadName, "OpenCV native library link error");
        }
        catch (Exception e) { // Bắt các lỗi không mong muốn khác
            System.err.println("[" + threadName + "] UNEXPECTED ERROR during face detection for imageId: " + imageInfo.getImageId() +
                               " ('" + imageInfo.getOriginalFilename() + "') in JobId: " + jobId + ". Error: " + e.getMessage());
            e.printStackTrace();
            detectionDetails = "Unexpected internal error: " + e.getMessage().substring(0, Math.min(e.getMessage().length(), 150));
            imageJobDAO.updateDetectionDetails(imageInfo.getImageId(), detectionDetails);
            handleProcessingError(threadName, "Unexpected internal error during face detection");
        } finally {
            System.out.println("[" + threadName + "] Finished face detection processing for imageId: " + imageInfo.getImageId() +
                               " in JobId: " + jobId);
        }
    }

    // Hàm helper để xử lý lỗi và đánh dấu job FAILED
    private void handleProcessingError(String threadName, String reason) {
        ImageJob currentJob = imageJobDAO.getJobById(this.jobId);
        // Chỉ đánh dấu FAILED một lần và nếu job chưa COMPLETED
        if (currentJob != null && !"COMPLETED".equals(currentJob.getStatus()) && !"FAILED".equals(currentJob.getStatus())) {
            imageJobDAO.updateJobStatus(this.jobId, "FAILED");
            System.err.println("[" + threadName + "] JobId: " + this.jobId + " marked as FAILED. Reason: " + reason);
        } else if (currentJob != null) {
            System.err.println("[" + threadName + "] JobId: " + this.jobId + " already " + currentJob.getStatus() +
                               ". Skipping FAILED status update for current error (" + reason + ")");
        } else {
             System.err.println("[" + threadName + "] Could not retrieve current status for JobId: " + this.jobId +
                               ". Cannot determine if FAILED status update is needed for error (" + reason + ")");
        }
    }
}