<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - Face Detector App</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/style.css"> <%-- File CSS chung --%>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/login-style.css"> <%-- File CSS riêng cho login --%>
</head>
<body class="login-page-body-specific"> <%-- Thêm class này --%>
    <div class="container auth-container"> <%-- Giữ lại các class chung nếu style.css vẫn định nghĩa chúng --%>
        <h2>User Login</h2>

        <c:if test="${not empty successMessage}">
            <p class="message success"><c:out value="${successMessage}"/></p>
        </c:if>
        <c:if test="${not empty errorMessage}">
            <p class="message error"><c:out value="${errorMessage}"/></p>
        </c:if>

        <form action="${pageContext.request.contextPath}/login" method="post">
            <div class="form-group">
                <label for="username">Username:</label>
                <input type="text" id="username" name="username" value="<c:out value='${username}'/>" required autofocus>
            </div>
            <div class="form-group">
                <label for="password">Password:</label>
                <input type="password" id="password" name="password" required>
            </div>
            <div class="form-group">
                <button type="submit" class="btn btn-primary">Login</button>
            </div>
        </form>
        <p class="auth-switch">Don't have an account? <a href="${pageContext.request.contextPath}/register">Register here</a></p>
    </div>
</body>
</html>