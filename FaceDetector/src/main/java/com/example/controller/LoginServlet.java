package com.example.controller;

import com.example.dao.UserDAO;
import com.example.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        userDAO = new UserDAO();
        System.out.println("LoginServlet initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("true".equals(request.getParameter("registrationSuccess"))) {
            request.setAttribute("successMessage", "Registration successful! Please log in.");
        }
        if ("true".equals(request.getParameter("logoutSuccess"))) {
            request.setAttribute("successMessage", "You have been logged out.");
        }
        request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        String errorMessage = null;
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            errorMessage = "Username and password are required.";
        }

        User user = null;
        if (errorMessage == null) {
            user = userDAO.validateUser(username.trim(), password); // Mật khẩu thô
            if (user == null) {
                errorMessage = "Invalid username or password.";
            }
        }

        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage);
            request.setAttribute("username", username);
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
            return;
        }

        HttpSession session = request.getSession();
        session.setAttribute("loggedInUser", user);
        System.out.println("User logged in: " + user.getUsername());
        response.sendRedirect(request.getContextPath() + "/upload");
    }
}