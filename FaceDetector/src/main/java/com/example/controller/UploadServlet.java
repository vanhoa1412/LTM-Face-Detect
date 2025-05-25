package com.example.controller;

import com.example.dao.ImageJobDAO;
import com.example.model.ImageJob;
import com.example.model.ProcessedImage;
import com.example.model.User;
import com.example.service.FaceDetectionService; // Import service mới
import com.example.service.ImageProcessingTask;
import com.example.service.ThumbnailQueueManager; // Vẫn dùng tên này cho queue manager

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

@WebServlet("/upload")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,
    maxFileSize = 1024 * 1024 * 10,
    maxRequestSize = 1024 * 1024 * 50
)
public class UploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private ImageJobDAO imageJobDAO;
    private FaceDetectionService faceDetectionService; // Sử dụng FaceDetectionService
    private static final String CASCADE_FILE_NAME = "haarcascade_frontalface_alt.xml"; // Tên file cascade

    @Override
    public void init() throws ServletException {
        super.init();
        imageJobDAO = new ImageJobDAO();
        try {
            faceDetectionService = new FaceDetectionService();
            System.out.println("UploadServlet initialized with FaceDetectionService (using hardcoded cascade path).");
        } catch (IOException e) {
            System.err.println("FATAL: Could not initialize FaceDetectionService in UploadServlet (hardcoded path): " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Failed to initialize FaceDetectionService. Check hardcoded cascade path and OpenCV setup.", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInUser") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/jsp/upload.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = null;

        if (session != null && session.getAttribute("loggedInUser") != null) {
            currentUser = (User) session.getAttribute("loggedInUser");
        } else {
            System.out.println("UploadServlet: User not logged in during POST. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        ServletContext context = getServletContext();
        String uploadDirectory = (String) context.getAttribute("uploadDirectory");
        String outputDirectory = (String) context.getAttribute("outputDirectory"); // Thư mục lưu ảnh đã xử lý (khuôn mặt cắt)

        if (uploadDirectory == null || outputDirectory == null) {
            System.err.println("UploadServlet CRITICAL ERROR: 'uploadDirectory' or 'outputDirectory' is not configured in ServletContext.");
            request.setAttribute("errorMessage", "Server configuration error: Storage directories are not properly set up. Please contact the administrator.");
            request.getRequestDispatcher("/WEB-INF/jsp/upload.jsp").forward(request, response);
            return;
        }
        if (faceDetectionService == null) { // Kiểm tra nếu service không khởi tạo được
             System.err.println("UploadServlet CRITICAL ERROR: FaceDetectionService is not initialized. Cannot process uploads.");
             request.setAttribute("errorMessage", "Face detection service is unavailable. Please contact the administrator.");
             request.getRequestDispatcher("/WEB-INF/jsp/upload.jsp").forward(request, response);
             return;
        }


        Collection<Part> fileParts;
        try {
            fileParts = request.getParts().stream()
                               .filter(part -> "files".equals(part.getName()) && part.getSubmittedFileName() != null && !part.getSubmittedFileName().trim().isEmpty())
                               .collect(Collectors.toList());
        } catch (Exception e) { // Bắt cả IOException và ServletException
            System.err.println("UploadServlet: Error retrieving file parts from request: " + e.getMessage());
            request.setAttribute("errorMessage", "Error processing the uploaded files. Please ensure they are valid images and try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/upload.jsp").forward(request, response);
            return;
        }

        if (fileParts.isEmpty()) {
             request.setAttribute("errorMessage", "No files selected. Please choose at least one image file to upload.");
             request.getRequestDispatcher("/WEB-INF/jsp/upload.jsp").forward(request, response);
             return;
        }

        // Tạo Job mới
        ImageJob newJob = new ImageJob(currentUser.getUserId(), "PENDING");
        int jobId = imageJobDAO.createJob(newJob);

        if (jobId == -1) {
            System.err.println("UploadServlet: Database error - Failed to create a new job entry for user ID: " + currentUser.getUserId());
            request.setAttribute("errorMessage", "Failed to start the processing job due to a database error. Please try again later.");
            request.getRequestDispatcher("/WEB-INF/jsp/upload.jsp").forward(request, response);
            return;
        }
        System.out.println("UploadServlet: Created new job with ID: " + jobId + " for user: " + currentUser.getUsername());

        boolean fileProcessingQueued = false;
        int validFilesCount = 0;
        StringBuilder uploadWarnings = new StringBuilder();

        for (Part filePart : fileParts) {
            String submittedFileName = filePart.getSubmittedFileName();
            String contentType = filePart.getContentType();

            if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/gif"))) {
                String warning = "Skipped: '" + submittedFileName + "' (Unsupported type: " + contentType + "). Only JPG, PNG, GIF are processed.";
                System.out.println("UploadServlet: " + warning);
                if(uploadWarnings.length() > 0) uploadWarnings.append("<br/>");
                uploadWarnings.append(warning);
                continue;
            }

            validFilesCount++;
            String extension = "";
            int i = submittedFileName.lastIndexOf('.');
            if (i > 0 && i < submittedFileName.length() - 1) {
                extension = submittedFileName.substring(i).toLowerCase();
            }
            String uniqueServerFileName = UUID.randomUUID().toString() + extension;
            Path uploadedFilePath = Paths.get(uploadDirectory, uniqueServerFileName);

            try (InputStream fileContent = filePart.getInputStream()) {
                Files.copy(fileContent, uploadedFilePath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("UploadServlet: File '" + submittedFileName + "' saved as '" + uploadedFilePath.toAbsolutePath() + "' for job " + jobId);

                ProcessedImage pImage = new ProcessedImage(jobId, submittedFileName, uploadedFilePath.toAbsolutePath().toString());
                int imageId = imageJobDAO.addProcessedImage(pImage);

                if (imageId != -1) {
                    pImage.setImageId(imageId);
                    // Tạo task với FaceDetectionService
                    ImageProcessingTask task = new ImageProcessingTask(
                        pImage, jobId, imageJobDAO, faceDetectionService, outputDirectory
                    );
                    ThumbnailQueueManager.getInstance().submitTask(task);
                    fileProcessingQueued = true;
                } else {
                    System.err.println("UploadServlet: Database error - Failed to save image metadata for: '" + submittedFileName + "' in job " + jobId);
                    Files.deleteIfExists(uploadedFilePath); // Dọn dẹp file đã upload nếu lỗi DB
                    if(uploadWarnings.length() > 0) uploadWarnings.append("<br/>");
                    uploadWarnings.append("Error saving metadata for '"+submittedFileName+"'. This file was not processed.");
                }

            } catch (Exception e) {
                System.err.println("UploadServlet: Exception while handling file '" + submittedFileName + "' for job " + jobId + ": " + e.getMessage());
                e.printStackTrace(); // In stack trace để debug
                if(uploadWarnings.length() > 0) uploadWarnings.append("<br/>");
                uploadWarnings.append("A server error occurred while processing '"+submittedFileName+"'.");
            }
        }

        if (uploadWarnings.length() > 0) {
            // Lưu ý: session.setAttribute dùng cho redirect, request.setAttribute dùng cho forward
            // Vì ta redirect, nên dùng session cho warning (hoặc flash attribute nếu dùng framework)
            // Tạm thời vẫn dùng request.setAttribute, sẽ chỉ hiển thị nếu có lỗi và forward lại
            request.setAttribute("warningMessage", uploadWarnings.toString());
        }

        if (!fileProcessingQueued && validFilesCount > 0) {
            imageJobDAO.updateJobStatus(jobId, "FAILED");
            System.err.println("UploadServlet: Job " + jobId + " marked FAILED - valid files were present but none could be queued for processing.");
            request.setAttribute("errorMessage", "Although valid image files were found, none could be queued for processing. Please check server logs or contact support.");
            request.getRequestDispatcher("/WEB-INF/jsp/upload.jsp").forward(request, response);
        } else if (!fileProcessingQueued && validFilesCount == 0) {
             imageJobDAO.updateJobStatus(jobId, "FAILED"); // Hoặc xóa job này nếu không có file nào
             System.out.println("UploadServlet: Job " + jobId + " marked FAILED - no valid image files were selected by the user.");
             if (uploadWarnings.length() == 0) { // Chỉ thêm lỗi nếu chưa có warning về file type
                request.setAttribute("errorMessage", "No valid image files (JPG, PNG, GIF) were selected for upload.");
             }
             request.getRequestDispatcher("/WEB-INF/jsp/upload.jsp").forward(request, response);
        } else if (fileProcessingQueued) { // Chỉ cập nhật PROCESSING nếu có file được queue
            imageJobDAO.updateJobStatusIfPending(jobId, "PROCESSING");
            System.out.println("UploadServlet: Job " + jobId + " submitted with " + validFilesCount + " valid files. Redirecting to job status.");
            response.sendRedirect(request.getContextPath() + "/jobStatus?jobId=" + jobId + "&uploadSubmitted=true" + (uploadWarnings.length() > 0 ? "&hasWarnings=true" : ""));
        } else { // Trường hợp không lường trước
             imageJobDAO.updateJobStatus(jobId, "FAILED");
             request.setAttribute("errorMessage", "An unexpected issue occurred during upload. Job has been marked as failed.");
             request.getRequestDispatcher("/WEB-INF/jsp/upload.jsp").forward(request, response);
        }
    }
}