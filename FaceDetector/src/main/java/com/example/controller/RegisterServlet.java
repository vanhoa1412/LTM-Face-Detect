package com.example.controller;

import com.example.dao.UserDAO;
import com.example.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        userDAO = new UserDAO();
        System.out.println("RegisterServlet initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        String errorMessage = null;

        if (username == null || username.trim().isEmpty()) {
            errorMessage = "Username is required.";
        } else if (password == null || password.isEmpty()) {
            errorMessage = "Password is required.";
        } else if (username.trim().length() < 3) {
            errorMessage = "Username must be at least 3 characters long.";
        } else if (password.length() < 4) {
            errorMessage = "Password must be at least 4 characters long.";
        } else if (userDAO.getUserByUsername(username.trim()) != null) {
            errorMessage = "Username '" + username.trim() + "' already exists.";
        }

        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage);
            request.setAttribute("username", username);
            request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
            return;
        }

        User newUser = new User(username.trim(), password); // Mật khẩu thô

        if (userDAO.addUser(newUser)) {
            System.out.println("User registered successfully: " + username.trim());
            response.sendRedirect(request.getContextPath() + "/login?registrationSuccess=true");
        } else {
            System.err.println("Registration failed for user: " + username.trim());
            request.setAttribute("errorMessage", "Registration failed. Please try again.");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
        }
    }
}