package com.example.controller;

import com.example.dao.ImageJobDAO;
import com.example.model.ImageJob;
import com.example.model.ProcessedImage;
import com.example.model.User;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

@WebServlet("/imageDisplay")
public class ImageDisplayServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private ImageJobDAO imageJobDAO;

    @Override
    public void init() throws ServletException {
         super.init();
         imageJobDAO = new ImageJobDAO();
         System.out.println("ImageDisplayServlet initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = null;

        if (session != null && session.getAttribute("loggedInUser") != null) {
            currentUser = (User) session.getAttribute("loggedInUser");
        } else {
            System.out.println("ImageDisplayServlet: Access denied (not logged in). Sending 401.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Login required to view images.");
            return;
        }

        String imageIdParam = request.getParameter("imageId");
        String typeParam = request.getParameter("type"); // "original" hoặc "facecrop"

        if (imageIdParam == null || imageIdParam.trim().isEmpty() ||
            typeParam == null || typeParam.trim().isEmpty() ||
            !(typeParam.equalsIgnoreCase("original") || typeParam.equalsIgnoreCase("facecrop"))) {
            System.err.println("ImageDisplayServlet: Bad request params. imageId='" + imageIdParam + "', type='" + typeParam + "'");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Required parameters: 'imageId' (integer) and 'type' ('original' or 'facecrop').");
            return;
        }

        ProcessedImage pImage = null;
        int imageId;
        try {
            imageId = Integer.parseInt(imageIdParam.trim());
            pImage = imageJobDAO.getProcessedImageById(imageId);
        } catch (NumberFormatException e) {
            System.err.println("ImageDisplayServlet: Invalid imageId format: '" + imageIdParam + "'.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image ID format.");
            return;
        }

        if (pImage == null) {
            System.out.println("ImageDisplayServlet: Image record not found for ID: " + imageId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image record not found.");
            return;
        }

        ImageJob job = imageJobDAO.getJobById(pImage.getJobId());
        if (job == null || job.getUserId() != currentUser.getUserId()) {
             System.out.println("ImageDisplayServlet: Permission denied for user '" + currentUser.getUsername() + "' on image " + pImage.getImageId());
             response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to view this image.");
             return;
        }

        String imagePathString;
        if ("facecrop".equalsIgnoreCase(typeParam.trim())) {
            imagePathString = pImage.getFaceCropFilepath();
            if (imagePathString == null || imagePathString.trim().isEmpty()) {
                 System.out.println("ImageDisplayServlet: Cropped face image path not available for image ID: " + pImage.getImageId());
                 // Trả về lỗi 404 hoặc một ảnh placeholder nếu muốn
                 response.sendError(HttpServletResponse.SC_NOT_FOUND, "Cropped face image is not available for this item (no face detected or processing error).");
                 return;
            }
        } else { // "original"
            imagePathString = pImage.getOriginalFilepath();
        }

        if (imagePathString == null || imagePathString.trim().isEmpty()) {
            System.err.println("ImageDisplayServlet: File path for type '" + typeParam + "' is missing for image ID: " + pImage.getImageId());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Image file path missing in record for type '"+typeParam+"'.");
            return;
        }

        File imageFile = new File(imagePathString.trim());

        if (!imageFile.exists() || !imageFile.isFile()) {
            System.err.println("ImageDisplayServlet: File not found on server. Path: '" + imagePathString + "' for type '" + typeParam + "'");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested image file ('" + typeParam + "') does not exist on server.");
            return;
        }

        // Path Traversal Prevention
        ServletContext servletContext = getServletContext();
        String configuredUploadDir = (String) servletContext.getAttribute("uploadDirectory");
        String configuredOutputDir = (String) servletContext.getAttribute("outputDirectory"); // Thư mục chứa ảnh đã xử lý

        if (configuredUploadDir == null || configuredOutputDir == null) {
            System.err.println("ImageDisplayServlet: CRITICAL! Storage directory paths not in ServletContext.");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server storage configuration error.");
            return;
        }

        try {
            Path requestedCanonicalPath = imageFile.getCanonicalFile().toPath();
            Path uploadCanonicalPath = Paths.get(configuredUploadDir).toFile().getCanonicalFile().toPath();
            Path outputCanonicalPath = Paths.get(configuredOutputDir).toFile().getCanonicalFile().toPath();

            if (!requestedCanonicalPath.startsWith(uploadCanonicalPath) && !requestedCanonicalPath.startsWith(outputCanonicalPath)) {
                System.err.println("ImageDisplayServlet: PATH TRAVERSAL! Req: '" + requestedCanonicalPath + "'. Allowed: '" + uploadCanonicalPath + "' OR '" + outputCanonicalPath + "'.");
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access forbidden.");
                return;
            }
        } catch (IOException | InvalidPathException | NullPointerException e) {
            System.err.println("ImageDisplayServlet: Path validation error for '" + imagePathString + "': " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error validating image path.");
            return;
        }

        String mimeType = servletContext.getMimeType(imageFile.getAbsolutePath());
        if (mimeType == null) mimeType = "application/octet-stream";
        response.setContentType(mimeType);
        response.setContentLengthLong(imageFile.length());

        System.out.println("ImageDisplayServlet: Serving " + typeParam + " image: '" + imageFile.getAbsolutePath() + "'");

        try (FileInputStream fis = new FileInputStream(imageFile);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (IOException e) {
            if (e.getClass().getName().contains("ClientAbortException")) {
                 System.out.println("ImageDisplayServlet: Client aborted for: '" + imageFile.getAbsolutePath() + "'.");
            } else {
                 System.err.println("ImageDisplayServlet: IOException streaming '" + imageFile.getAbsolutePath() + "': " + e.getMessage());
            }
        }
    }
}