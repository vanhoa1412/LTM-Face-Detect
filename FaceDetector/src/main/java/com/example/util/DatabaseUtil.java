package com.example.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtil {
    // !!! THAY ĐỔI CÁC THÔNG TIN NÀY CHO PHÙ HỢP VỚI CSDL CỦA BẠN !!!
    private static final String DB_URL = "jdbc:mysql://localhost:3306/face_detector_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root"; // Thay bằng user CSDL của bạn
    private static final String DB_PASSWORD = "1234"; // Thay bằng password CSDL của bạn
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";

    static {
        try {
            Class.forName(DB_DRIVER);
            System.out.println("INFO: JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("FATAL ERROR: JDBC driver not found: " + DB_DRIVER);
            throw new RuntimeException("Failed to load JDBC driver", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}