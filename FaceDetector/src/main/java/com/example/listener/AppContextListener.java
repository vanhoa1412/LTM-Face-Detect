package com.example.listener;

import com.example.service.ThumbnailQueueManager; 

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@WebListener
public class AppContextListener implements ServletContextListener {

    // KHỐI STATIC ĐỂ LOAD OPENCV NATIVE LIBRARY
	static {
	    try {
	        // THAY ĐỔI ĐƯỜNG DẪN NÀY CHO CHÍNH XÁC
	        String pathToNativeLib = "C:/Users/My computer/eclipseWeb/FaceDetector/opencv_java4110.dll";
	        System.out.println("Attempting to load OpenCV native library from absolute path: " + pathToNativeLib);
	        System.load(pathToNativeLib);
	        System.out.println("OpenCV Native Library loaded successfully. Version: " + org.opencv.core.Core.VERSION);
	    } catch (UnsatisfiedLinkError e) {
	        System.err.println("FATAL ERROR: OpenCV native library failed to load from absolute path.");
	        System.err.println(e.getMessage());
	        // throw new RuntimeException("Failed to load OpenCV native library", e);
	    }
	}


    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("========================================================");
        System.out.println("AppContextListener: Application Context Initializing...");
        System.out.println("========================================================");

        // Khởi tạo Queue Manager (tên lớp ThumbnailQueueManager có thể cần đổi nếu bạn muốn)
        try {
            ThumbnailQueueManager.getInstance();
            System.out.println("AppContextListener INFO: ThumbnailQueueManager (Processing Queue) initialized.");
        } catch (Exception e) {
            System.err.println("AppContextListener ERROR: Failed to initialize ThumbnailQueueManager: " + e.getMessage());
            e.printStackTrace();
        }


        ServletContext context = sce.getServletContext();
        String appName = context.getServletContextName();
        System.out.println("AppContextListener INFO: Initializing directories for application: " + (appName != null ? appName : "DefaultWebApp"));

        String uploadDirParam = context.getInitParameter("uploadDirectory");
        String outputDirParam = context.getInitParameter("outputDirectory"); // Đã đổi tên key trong web.xml

        if (uploadDirParam == null || uploadDirParam.trim().isEmpty()) {
            System.err.println("AppContextListener CRITICAL: 'uploadDirectory' context-param is not set or empty in web.xml. Uploads will likely fail.");
        } else {
            setupDirectory(Paths.get(uploadDirParam.trim()), "Upload Storage", context, "uploadDirectory");
        }

        if (outputDirParam == null || outputDirParam.trim().isEmpty()) {
            System.err.println("AppContextListener CRITICAL: 'outputDirectory' (for processed images) context-param is not set or empty in web.xml. Processing output will likely fail.");
        } else {
            setupDirectory(Paths.get(outputDirParam.trim()), "Processed Image Output", context, "outputDirectory");
        }
        System.out.println("========================================================");
        System.out.println("AppContextListener: Application Context Initialization Finished.");
        System.out.println("========================================================");
    }

    private void setupDirectory(Path dirPath, String dirPurpose, ServletContext context, String contextAttributeName) {
        try {
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath); // Tạo thư mục và các thư mục cha nếu cần
                System.out.println("AppContextListener INFO: " + dirPurpose + " directory created successfully at: " + dirPath.toAbsolutePath());
            } else if (!Files.isDirectory(dirPath)) {
                System.err.println("AppContextListener ERROR: Configured " + dirPurpose + " path exists but IS NOT A DIRECTORY: " + dirPath.toAbsolutePath());
                return; // Không set attribute nếu đường dẫn không phải là thư mục
            } else {
                 System.out.println("AppContextListener INFO: " + dirPurpose + " directory already exists at: " + dirPath.toAbsolutePath());
            }

            // Kiểm tra quyền ghi (đơn giản)
            if (Files.isWritable(dirPath)) {
                 System.out.println("AppContextListener INFO: Application has write permissions for " + dirPurpose + " directory: " + dirPath.toAbsolutePath());
            } else {
                 System.err.println("AppContextListener CRITICAL WARNING: Application may NOT have write permissions for " + dirPurpose + " directory: " + dirPath.toAbsolutePath() + ". Operations requiring write access will fail.");
            }

            context.setAttribute(contextAttributeName, dirPath.toAbsolutePath().toString());
            System.out.println("AppContextListener INFO: " + dirPurpose + " directory path set in ServletContext ('" + contextAttributeName + "'): " + dirPath.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("AppContextListener ERROR: IOException while creating or accessing " + dirPurpose + " directory at: '" + dirPath + "'. Error: " + e.getMessage());
            // e.printStackTrace(); // Bỏ comment để debug sâu hơn
        } catch (SecurityException se) {
            System.err.println("AppContextListener SECURITY ERROR: Permission denied while trying to access or create " + dirPurpose + " directory at: '" + dirPath + "'. Error: " + se.getMessage());
            // se.printStackTrace();
        } catch (Exception ex) {
            System.err.println("AppContextListener UNEXPECTED ERROR during setup for " + dirPurpose + " directory at: '" + dirPath + "'. Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("========================================================");
        System.out.println("AppContextListener: Application Context Destroying...");
        System.out.println("========================================================");
        try {
            ThumbnailQueueManager.getInstance().shutdown();
            System.out.println("AppContextListener INFO: ThumbnailQueueManager (Processing Queue) has been shut down.");
        } catch (Exception e) {
            System.err.println("AppContextListener ERROR: Failed to properly shut down ThumbnailQueueManager: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("========================================================");
        System.out.println("AppContextListener: Application Context Destruction Finished.");
        System.out.println("========================================================");
    }
}