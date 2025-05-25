package com.example.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class ImageJob implements Serializable {
    private static final long serialVersionUID = 1L;
    private int jobId;
    private int userId;
    private String status;
    private Timestamp uploadTimestamp;
    private Timestamp completionTimestamp;

    public ImageJob() {}

    public ImageJob(int userId, String status) {
        this.userId = userId;
        this.status = status;
        this.uploadTimestamp = new Timestamp(System.currentTimeMillis());
    }
    // Getters and Setters
    public int getJobId() { return jobId; }
    public void setJobId(int jobId) { this.jobId = jobId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getUploadTimestamp() { return uploadTimestamp; }
    public void setUploadTimestamp(Timestamp uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; }
    public Timestamp getCompletionTimestamp() { return completionTimestamp; }
    public void setCompletionTimestamp(Timestamp completionTimestamp) { this.completionTimestamp = completionTimestamp; }
}