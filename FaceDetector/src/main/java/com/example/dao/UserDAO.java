package com.example.dao;

import com.example.model.User;
import com.example.util.DatabaseUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    public User getUserByUsername(String username) {
        String sql = "SELECT user_id, username, password_hash FROM users WHERE username = ?";
        User user = null;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setPasswordHash(rs.getString("password_hash"));
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getUserByUsername for '" + username + "': " + e.getMessage());
        }
        return user;
    }

    public boolean addUser(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().trim().isEmpty() ||
            user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            System.err.println("UserDAO.addUser: User data invalid.");
            return false;
        }
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername().trim());
            pstmt.setString(2, user.getPasswordHash()); // Mật khẩu thô
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SQL Error in addUser for '" + user.getUsername() + "': " + e.getMessage());
        }
        return false;
    }

    public User validateUser(String username, String rawPassword) {
        if (username == null || username.trim().isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            return null;
        }
        User user = getUserByUsername(username.trim());
        if (user != null && user.getPasswordHash().equals(rawPassword)) { // So sánh thô
            return user;
        }
        return null;
    }
}