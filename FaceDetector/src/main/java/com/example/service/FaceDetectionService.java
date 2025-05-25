package com.example.service;

// Import các lớp cần thiết của OpenCV
import org.opencv.core.Mat;          // Lớp ma trận cơ bản của OpenCV, dùng để lưu trữ và xử lý ảnh
import org.opencv.core.MatOfRect;   // Lớp lưu trữ một tập hợp các hình chữ nhật (Rect), kết quả của face detection
import org.opencv.core.Rect;        // Lớp biểu diễn một hình chữ nhật (x, y, width, height)
import org.opencv.core.Size;        // Lớp biểu diễn kích thước (width, height)
import org.opencv.imgcodecs.Imgcodecs; // Lớp chứa các hàm đọc và ghi ảnh
import org.opencv.imgproc.Imgproc;   // Lớp chứa các hàm xử lý ảnh (ví dụ: chuyển đổi không gian màu, cân bằng histogram)
import org.opencv.objdetect.CascadeClassifier; // Lớp chính để phát hiện đối tượng (khuôn mặt)

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream; // Import này có thể không cần nếu dùng Files.copy
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FaceDetectionService {

    private CascadeClassifier faceCascade;
    private boolean isInitialized = false;

 
    public FaceDetectionService(String cascadeFileName) throws IOException {
        // Load OpenCV native library (đảm bảo đã được load trước đó, ví dụ trong AppContextListener)
        // Nếu chưa load, dòng này sẽ gây lỗi. Thông thường, load một lần khi ứng dụng khởi động.
        // static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); } // Không nên đặt ở đây, nên ở AppContextListener

        String resourcePath = "cascades/" + cascadeFileName;
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if (inputStream == null) {
            System.err.println("FaceDetectionService FATAL ERROR: Cascade file '" + resourcePath + "' not found in classpath resources.");
            throw new IOException("Cascade classifier file not found in resources: " + resourcePath);
        }


        File tempCascadeFile;
        try {
            tempCascadeFile = File.createTempFile("cv_cascade_", ".xml");
            tempCascadeFile.deleteOnExit(); // Xóa file tạm khi JVM tắt
            try (OutputStream outputStream = new FileOutputStream(tempCascadeFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("FaceDetectionService INFO: Cascade file copied to temporary location: " + tempCascadeFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("FaceDetectionService FATAL ERROR: Could not create temporary file for cascade classifier.");
            throw new IOException("Failed to create temporary cascade file", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) { /* ignore */ }
            }
        }

        this.faceCascade = new CascadeClassifier(tempCascadeFile.getAbsolutePath());
        if (this.faceCascade.empty()) {
            // Xóa file tạm nếu classifier không load được để tránh lỗi lần sau
            if (tempCascadeFile != null && tempCascadeFile.exists()) {
                // tempCascadeFile.delete(); // Không cần thiết vì có deleteOnExit()
            }
            System.err.println("FaceDetectionService FATAL ERROR: Failed to load OpenCV cascade classifier from: " + tempCascadeFile.getAbsolutePath());
            throw new IOException("Failed to load OpenCV cascade classifier. Path: " + tempCascadeFile.getAbsolutePath());
        }

        isInitialized = true;
        System.out.println("FaceDetectionService INFO: Cascade classifier '" + cascadeFileName + "' loaded successfully.");
    }

    /**
     * Phát hiện khuôn mặt trong ảnh và cắt khuôn mặt đầu tiên tìm thấy.
     *
     * @param originalImagePath Đường dẫn tuyệt đối đến file ảnh gốc.
     * @param outputDirectory   Thư mục để lưu ảnh khuôn mặt đã cắt.
     * @param originalFilename  Tên file gốc (dùng để tạo tên file output).
     * @return Một mảng Object:
     *         [0] (String) là đường dẫn đến file ảnh khuôn mặt đã cắt (có thể null nếu không tìm thấy khuôn mặt hoặc lỗi).
     *         [1] (String) là thông tin chi tiết về quá trình phát hiện.
     * @throws IOException Nếu có lỗi nghiêm trọng trong quá trình đọc/ghi file.
     */
    public Object[] detectAndCropFirstFace(String originalImagePath, String outputDirectory, String originalFilename) throws IOException {
        if (!isInitialized || faceCascade.empty()) {
            System.err.println("FaceDetectionService ERROR: Service not properly initialized or cascade classifier is empty.");
            return new Object[]{null, "Error: Face detection service not initialized."};
        }

        Mat image = Imgcodecs.imread(originalImagePath); // Đọc ảnh gốc
        if (image.empty()) {
            System.err.println("FaceDetectionService ERROR: Could not read image file from path: " + originalImagePath);
            return new Object[]{null, "Error: Could not read the image file."};
        }

        Mat grayImage = new Mat();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY); // Chuyển sang ảnh xám
        Imgproc.equalizeHist(grayImage, grayImage); // Cân bằng histogram để cải thiện độ tương phản

        MatOfRect facesDetected = new MatOfRect();
        // Các tham số của detectMultiScale có thể cần được tinh chỉnh để có kết quả tốt nhất:
        // scaleFactor: Mức độ giảm kích thước ảnh ở mỗi lần quét (nên > 1.0, ví dụ 1.1 hoặc 1.05).
        // minNeighbors: Số lượng "hàng xóm" mà mỗi hình chữ nhật ứng viên phải có để được coi là khuôn mặt.
        //               Giá trị cao hơn làm giảm số lượng phát hiện sai nhưng có thể bỏ lỡ một số khuôn mặt.
        // flags: Không dùng trong các phiên bản OpenCV cũ, đặt là 0.
        // minSize: Kích thước tối thiểu của khuôn mặt cần phát hiện.
        // maxSize: Kích thước tối đa của khuôn mặt cần phát hiện (để trống nếu không giới hạn).
        faceCascade.detectMultiScale(grayImage, facesDetected, 1.1, 5, 0, new Size(30, 30), new Size());

        Rect[] facesArray = facesDetected.toArray();
        String detectionDetails;
        String croppedFaceFilepath = null;

        if (facesArray.length > 0) {
            detectionDetails = facesArray.length + " face(s) detected.";
            System.out.println("FaceDetectionService INFO: " + detectionDetails + " in '" + originalFilename + "'. Cropping the first one.");

            // Lấy khuôn mặt đầu tiên (lớn nhất hoặc xuất hiện đầu tiên tùy thuộc vào cách sắp xếp của OpenCV)
            Rect faceRect = facesArray[0];

            // (Tùy chọn) Mở rộng vùng cắt một chút để bao gồm thêm ngữ cảnh xung quanh khuôn mặt
            int paddingHorizontal = (int) (faceRect.width * 0.20); // 20% padding chiều rộng
            int paddingVertical = (int) (faceRect.height * 0.30); // 30% padding chiều cao

            faceRect.x = Math.max(0, faceRect.x - paddingHorizontal);
            faceRect.y = Math.max(0, faceRect.y - paddingVertical);
            // Đảm bảo vùng cắt không vượt ra ngoài kích thước ảnh gốc
            faceRect.width = Math.min(image.cols() - faceRect.x, faceRect.width + 2 * paddingHorizontal);
            faceRect.height = Math.min(image.rows() - faceRect.y, faceRect.height + 2 * paddingVertical);

            // Cắt vùng khuôn mặt từ ảnh gốc
            Mat croppedImage = new Mat(image, faceRect);

            // Tạo tên file và đường dẫn lưu ảnh đã cắt
            String fileExtension = ".png"; // Luôn lưu ảnh cắt dưới dạng PNG để đảm bảo chất lượng
            String cropFilename = "face_crop_" + UUID.randomUUID().toString() + fileExtension;
            Path cropFileDestPath = Paths.get(outputDirectory, cropFilename);

            // Tạo thư mục output nếu chưa có
            Files.createDirectories(cropFileDestPath.getParent());

            // Lưu ảnh đã cắt
            boolean saved = Imgcodecs.imwrite(cropFileDestPath.toString(), croppedImage);
            if (saved) {
                croppedFaceFilepath = cropFileDestPath.toAbsolutePath().toString();
                System.out.println("FaceDetectionService INFO: Cropped face saved to: " + croppedFaceFilepath);
            } else {
                System.err.println("FaceDetectionService ERROR: Failed to save cropped face image for '" + originalFilename + "'.");
                detectionDetails += " (Error saving cropped image)";
            }
            // Giải phóng Mat
            croppedImage.release();

        } else {
            detectionDetails = "No faces detected.";
            System.out.println("FaceDetectionService INFO: " + detectionDetails + " in '" + originalFilename + "'.");
        }

        // Giải phóng Mat
        image.release();
        grayImage.release();
        facesDetected.release();

        return new Object[]{croppedFaceFilepath, detectionDetails};
    }
}