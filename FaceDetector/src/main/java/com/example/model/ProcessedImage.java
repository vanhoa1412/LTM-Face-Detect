package com.example.model;

import java.io.Serializable;

public class ProcessedImage implements Serializable {
    private static final long serialVersionUID = 3L; // UID có thể thay đổi nếu cấu trúc thay đổi

    private int imageId;
    private int jobId;
    private String originalFilename;
    private String originalFilepath;
    private String faceCropFilepath;    // Đường dẫn đến ảnh khuôn mặt đã cắt
    private String detectionDetails; // Ví dụ: "1 face detected", "No faces found"

    public ProcessedImage() {
    }

    public ProcessedImage(int jobId, String originalFilename, String originalFilepath) {
        this.jobId = jobId;
        this.originalFilename = originalFilename;
        this.originalFilepath = originalFilepath;
    }

    // Getters and Setters
    public int getImageId() { return imageId; }
    public void setImageId(int imageId) { this.imageId = imageId; }

    public int getJobId() { return jobId; }
    public void setJobId(int jobId) { this.jobId = jobId; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getOriginalFilepath() { return originalFilepath; }
    public void setOriginalFilepath(String originalFilepath) { this.originalFilepath = originalFilepath; }

    public String getFaceCropFilepath() { return faceCropFilepath; }
    public void setFaceCropFilepath(String faceCropFilepath) { this.faceCropFilepath = faceCropFilepath; }

    public String getDetectionDetails() { return detectionDetails; }
    public void setDetectionDetails(String detectionDetails) { this.detectionDetails = detectionDetails; }
}