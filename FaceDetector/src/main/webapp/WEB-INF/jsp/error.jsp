<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error - Face Detector App</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/error-page-style.css">
</head>
<body>
    <div class="container error-container">
        <h2>Oops! Something Went Wrong</h2>
        <p>We apologize for the inconvenience. An unexpected error has occurred.</p>

        <c:set var="statusCode" value="${pageContext.errorData.statusCode}"/>
        <c:set var="errMessage" value="${pageContext.errorData.throwable.message}"/> <%-- Đổi tên biến để tránh trùng với requestScope.errorMessage --%>
        <c:set var="requestUri" value="${pageContext.errorData.requestURI}"/>

        <div class="error-details">
            <c:if test="${not empty statusCode and statusCode ne 0}">
                <p><strong>Status Code:</strong> <c:out value="${statusCode}"/></p>
            </c:if>

            <c:choose>
                <c:when test="${statusCode == 404}">
                    <p>The page you requested (<c:out value="${requestUri}"/>) could not be found.</p>
                </c:when>
                <c:when test="${statusCode == 403}">
                    <p>You do not have permission to access the requested resource (<c:out value="${requestUri}"/>).</p>
                </c:when>
                 <c:when test="${statusCode == 401}">
                    <p>You are not authorized. Please <a href="${pageContext.request.contextPath}/login">login</a>.</p>
                </c:when>
                <c:when test="${not empty errMessage}">
                    <p><strong>Error:</strong> <c:out value="${errMessage}"/></p>
                </c:when>
                <c:otherwise>
                     <p>An unspecified error occurred.</p>
                </c:otherwise>
            </c:choose>
            <c:if test="${not empty requestScope.errorMessage}"> <%-- Hiển thị errorMessage từ request nếu có --%>
                <p><strong>Additional Info:</strong> <c:out value="${requestScope.errorMessage}"/></p>
            </c:if>
        </div>

        <%--
        // Developer Debug Information (Comment out or remove for production)
        <c:if test="${pageContext.exception != null}">
            <h3>Developer Debug Information:</h3>
            <p><strong>Exception Type:</strong> ${pageContext.errorData.throwable.class.name}</p>
            <h4>Stack Trace:</h4>
            <pre><%@ page import="java.io.PrintWriter, java.io.StringWriter" %><%
                if (pageContext.getException() != null) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    pageContext.getException().printStackTrace(pw);
                    // Escape HTML for safe rendering
                    String stackTrace = sw.toString();
                    stackTrace = stackTrace.replace("&", "&").replace("<", "<").replace(">", ">");
                    out.print(stackTrace);
                }
            %></pre>
        </c:if>
        --%>

        <p class="home-link">
            <a href="${pageContext.request.contextPath}/" class="btn">Go to Homepage</a>
            <c:if test="${not empty loggedInUser.username}">
                 <a href="${pageContext.request.contextPath}/upload" class="btn">Go to Upload Page</a>
            </c:if>
        </p>
    </div>
</body>
</html>