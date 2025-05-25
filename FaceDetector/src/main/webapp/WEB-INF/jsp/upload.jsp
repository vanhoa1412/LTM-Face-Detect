<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<jsp:useBean id="loggedInUser" scope="session" type="com.example.model.User" />

<%-- Redirect to login if user is not in session --%>
<c:if test="${empty loggedInUser.username}">
    <c:redirect url="/login" />
</c:if>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Upload Images - Face Detector App</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <div class="container app-container">
        <header class="app-header">
            <h1>Face Detector App</h1>
            <nav>
                <span>Welcome, <c:out value="${loggedInUser.username}"/>!</span>
                <a href="${pageContext.request.contextPath}/jobStatus" class="nav-link">View My Jobs</a>
                <a href="${pageContext.request.contextPath}/logout" class="nav-link btn btn-logout">Logout</a>
            </nav>
        </header>

        <main>
            <h2>Upload Images for Face Detection</h2>
            <p>Select multiple image files (JPG, PNG, GIF). The system will attempt to detect and crop the first face found in each image.</p>

            <c:if test="${not empty successMessage}">
                <p class="message success"><c:out value="${successMessage}"/></p>
            </c:if>
            <c:if test="${not empty errorMessage}">
                <p class="message error"><c:out value="${errorMessage}"/></p>
            </c:if>
            <c:if test="${not empty warningMessage}">
                <div class="message warning">
                    <strong>Warnings during last upload:</strong><br>
                    ${warningMessage} <%-- Cho phép HTML nếu warningMessage có <br/> --%>
                </div>
            </c:if>

            <form action="${pageContext.request.contextPath}/upload" method="post" enctype="multipart/form-data" class="upload-form">
                <div class="form-group">
                    <label for="files">Choose Images:</label>
                    <input type="file" id="files" name="files" multiple required accept="image/jpeg,image/png,image/gif">
                    <small>Max file size: 10MB each. Max total request: 50MB.</small>
                </div>
                <div class="form-group">
                    <button type="submit" class="btn btn-primary btn-upload">Upload and Detect Faces</button>
                </div>
            </form>
        </main>
         <footer>
            <p>© <%= new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) %> Face Detector App</p>
        </footer>
    </div>
</body>
</html>