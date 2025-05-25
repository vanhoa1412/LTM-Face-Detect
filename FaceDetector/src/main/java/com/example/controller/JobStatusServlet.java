package com.example.controller;

import com.example.dao.ImageJobDAO;
import com.example.model.ImageJob;
import com.example.model.ProcessedImage;
import com.example.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Collections; // Import Collections
import java.util.List;

@WebServlet("/jobStatus")
public class JobStatusServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private ImageJobDAO imageJobDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        imageJobDAO = new ImageJobDAO();
        System.out.println("JobStatusServlet initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = null;

        if (session != null && session.getAttribute("loggedInUser") != null) {
            currentUser = (User) session.getAttribute("loggedInUser");
        } else {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if ("true".equals(request.getParameter("uploadSubmitted"))) {
            request.setAttribute("successMessage", "Files submitted successfully! Processing has started. Results will appear below.");
        }
        if ("true".equals(request.getParameter("hasWarnings"))) { // Lấy thông báo warning từ redirect
             request.setAttribute("warningMessage", "Some files were skipped or encountered issues during upload. See server logs for details.");
        }


        String jobIdParam = request.getParameter("jobId");

        if (jobIdParam != null && !jobIdParam.trim().isEmpty()) {
            try {
                int jobId = Integer.parseInt(jobIdParam.trim());
                ImageJob job = imageJobDAO.getJobById(jobId);

                if (job != null && job.getUserId() == currentUser.getUserId()) {
                    List<ProcessedImage> images = imageJobDAO.getImagesByJob(jobId);
                    request.setAttribute("job", job);
                    request.setAttribute("images", images != null ? images : Collections.emptyList());
                    System.out.println("JobStatusServlet: Displaying details for job " + jobId + " for user " + currentUser.getUsername() + ". Images found: " + (images != null ? images.size() : 0));
                } else {
                    System.out.println("JobStatusServlet: Job " + jobId + " not found or user " + currentUser.getUsername() + " lacks permission.");
                    request.setAttribute("errorMessage", "Job not found or you do not have permission to view this job.");
                }
            } catch (NumberFormatException e) {
                System.err.println("JobStatusServlet: Invalid Job ID format received: '" + jobIdParam + "'");
                request.setAttribute("errorMessage", "Invalid Job ID format. Please use a numeric ID.");
            }
        } else {
            List<ImageJob> jobs = imageJobDAO.getJobsByUser(currentUser.getUserId());
            request.setAttribute("jobs", jobs != null ? jobs : Collections.emptyList());
            System.out.println("JobStatusServlet: Displaying all jobs for user " + currentUser.getUsername() + ". Jobs found: " + (jobs != null ? jobs.size() : 0));
        }

        request.getRequestDispatcher("/WEB-INF/jsp/jobStatus.jsp").forward(request, response);
    }
}