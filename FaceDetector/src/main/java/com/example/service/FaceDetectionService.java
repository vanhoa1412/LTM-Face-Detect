package com.example.service;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File; // Import File
import java.io.IOException; // Import IOException
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class FaceDetectionService {

    private CascadeClassifier faceCascade;
    private boolean isInitialized = false;

    // !!! THAY ĐỔI ĐƯỜNG DẪN NÀY CHO PHÙ HỢP VỚI MÁY TÍNH CỦA BẠN !!!
    // Ví dụ:
    // private static final String CASCADE_FILE_ABSOLUTE_PATH = "C:/OpenCV_Cascades/haarcascade_frontalface_alt.xml";
    // private static final String CASCADE_FILE_ABSOLUTE_PATH = "/opt/opencv_cascades/haarcascade_frontalface_alt.xml";
    private static final String CASCADE_FILE_ABSOLUTE_PATH = "C:/Users/My computer/eclipseWeb/hehe/src/main/resources/cascades/haarcascade_frontalface_alt.xml";

    /**
     * Constructor: Tải Cascade Classifier XML file từ một đường dẫn tuyệt đối được hardcode.
     *
     * @throws IOException Nếu không tìm thấy hoặc không load được file cascade.
     */
    public FaceDetectionService() throws IOException {
        // Load OpenCV native library - Đảm bảo đã được load một lần ở AppContextListener
        // static { try { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); } catch (UnsatisfiedLinkError e) { ... } }

        if (CASCADE_FILE_ABSOLUTE_PATH.equals("ĐƯỜNG_DẪN_TUYỆT_ĐỐI_ĐẾN_FILE_XML_CỦA_BẠN")) {
            String errorMsg = "FATAL: CASCADE_FILE_ABSOLUTE_PATH has not been set in FaceDetectionService.java. " +
                              "Please update this path to the location of your haarcascade XML file.";
            System.err.println(errorMsg);
            throw new IOException(errorMsg);
        }

        File cascadeFile = new File(CASCADE_FILE_ABSOLUTE_PATH);
        if (!cascadeFile.exists() || !cascadeFile.isFile()) {
            String errorMessage = "Cascade file not found at hardcoded absolute path: " + CASCADE_FILE_ABSOLUTE_PATH;
            System.err.println("FaceDetectionService FATAL ERROR: " + errorMessage);
            throw new IOException(errorMessage);
        }

        this.faceCascade = new CascadeClassifier(CASCADE_FILE_ABSOLUTE_PATH);
        if (this.faceCascade.empty()) {
            String errorMessage = "Failed to load OpenCV cascade classifier from hardcoded absolute path: " + CASCADE_FILE_ABSOLUTE_PATH +
                                  ". Ensure OpenCV native libraries are loaded correctly and the cascade file is valid and accessible.";
            System.err.println("FaceDetectionService FATAL ERROR: " + errorMessage);
            throw new IOException(errorMessage);
        }

        isInitialized = true;
        System.out.println("FaceDetectionService INFO: Cascade classifier loaded successfully from hardcoded path: " + CASCADE_FILE_ABSOLUTE_PATH);
    }

    /**
     * Phát hiện khuôn mặt trong ảnh và cắt khuôn mặt đầu tiên tìm thấy.
     * (Giữ nguyên logic của phương thức này như đã cung cấp ở phản hồi trước)
     *
     * @param originalImagePath Đường dẫn đến ảnh gốc.
     * @param outputDirectory Thư mục lưu ảnh khuôn mặt đã cắt.
     * @param originalFilename Tên file gốc.
     * @return Một mảng Object: [0] là đường dẫn đến file ảnh khuôn mặt đã cắt (String, có thể null),
     *                        [1] là thông tin phát hiện (String).
     * @throws IOException Nếu có lỗi đọc/ghi file.
     */
    public Object[] detectAndCropFirstFace(String originalImagePath, String outputDirectory, String originalFilename) throws IOException {
        if (!isInitialized || faceCascade.empty()) {
            System.err.println("FaceDetectionService ERROR: Service not properly initialized or cascade classifier is empty.");
            return new Object[]{null, "Error: Face detection service not initialized."};
        }

        Mat image = Imgcodecs.imread(originalImagePath);
        if (image.empty()) {
            System.err.println("FaceDetectionService ERROR: Could not read image file from path: " + originalImagePath);
            return new Object[]{null, "Error: Could not read the image file."};
        }

        Mat grayImage = new Mat();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayImage, grayImage);

        MatOfRect facesDetected = new MatOfRect();
        faceCascade.detectMultiScale(grayImage, facesDetected, 1.1, 5, 0, new Size(30, 30), new Size());

        Rect[] facesArray = facesDetected.toArray();
        String detectionDetails;
        String croppedFaceFilepath = null;

        if (facesArray.length > 0) {
            detectionDetails = facesArray.length + " face(s) detected.";
            System.out.println("FaceDetectionService INFO: " + detectionDetails + " in '" + originalFilename + "'. Cropping the first one.");

            Rect faceRect = facesArray[0];
            int paddingHorizontal = (int) (faceRect.width * 0.20);
            int paddingVertical = (int) (faceRect.height * 0.30);

            faceRect.x = Math.max(0, faceRect.x - paddingHorizontal);
            faceRect.y = Math.max(0, faceRect.y - paddingVertical);
            faceRect.width = Math.min(image.cols() - faceRect.x, faceRect.width + 2 * paddingHorizontal);
            faceRect.height = Math.min(image.rows() - faceRect.y, faceRect.height + 2 * paddingVertical);

            Mat croppedImage = new Mat(image, faceRect);
            String fileExtension = ".png";
            String cropFilename = "face_crop_" + UUID.randomUUID().toString() + fileExtension;
            Path cropFileDestPath = Paths.get(outputDirectory, cropFilename);

            Files.createDirectories(cropFileDestPath.getParent());
            boolean saved = Imgcodecs.imwrite(cropFileDestPath.toString(), croppedImage);

            if (saved) {
                croppedFaceFilepath = cropFileDestPath.toAbsolutePath().toString();
                System.out.println("FaceDetectionService INFO: Cropped face saved to: " + croppedFaceFilepath);
            } else {
                System.err.println("FaceDetectionService ERROR: Failed to save cropped face image for '" + originalFilename + "'.");
                detectionDetails += " (Error saving cropped image)";
            }
            croppedImage.release();
        } else {
            detectionDetails = "No faces detected.";
            System.out.println("FaceDetectionService INFO: " + detectionDetails + " in '" + originalFilename + "'.");
        }

        image.release();
        grayImage.release();
        facesDetected.release();

        return new Object[]{croppedFaceFilepath, detectionDetails};
    }
}