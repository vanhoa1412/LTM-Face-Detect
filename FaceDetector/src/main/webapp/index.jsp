<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // System.out.println("TRACE: Accessing /index.jsp - Redirecting to /login...");
    response.sendRedirect(request.getContextPath() + "/login");
%>