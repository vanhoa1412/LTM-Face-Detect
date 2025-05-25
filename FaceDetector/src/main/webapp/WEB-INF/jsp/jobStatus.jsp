<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:useBean id="loggedInUser" scope="session" type="com.example.model.User"/>

<%-- Redirect to login if user is not in session --%>
<c:if test="${empty loggedInUser.username}">
    <c:redirect url="/login" />
</c:if>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Job Status - Face Detector App</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/job-status-style.css">
    <%-- Auto-refresh if viewing a specific job that is still PENDING or PROCESSING --%>
    <c:if test="${not empty job && (job.status == 'PENDING' || job.status == 'PROCESSING')}">
        <meta http-equiv="refresh" content="10;url=${pageContext.request.contextPath}/jobStatus?jobId=${job.jobId}">
    </c:if>
</head>
<body>
    <div class="container app-container">
        <header class="app-header">
            <h1>Job Status</h1>
             <nav>
                <span>User: <c:out value="${loggedInUser.username}"/></span>
                <a href="${pageContext.request.contextPath}/upload" class="nav-link">Upload More Images</a>
                <a href="${pageContext.request.contextPath}/logout" class="nav-link btn btn-logout">Logout</a>
            </nav>
        </header>

        <main>
            <c:if test="${not empty successMessage}">
                <p class="message success"><c:out value="${successMessage}"/></p>
            </c:if>
             <c:if test="${not empty warningMessage}"> <%-- Hiển thị warning từ redirect --%>
                <div class="message warning">
                    <strong>Upload Warnings:</strong><br>
                    ${warningMessage}
                </div>
            </c:if>
            <c:if test="${not empty errorMessage}">
                <p class="message error"><c:out value="${errorMessage}"/></p>
            </c:if>

            <c:choose>
                <c:when test="${not empty job}"> <%-- Displaying details for a specific job --%>
                    <h2>Job ID: <c:out value="${job.jobId}"/></h2>
                    <div class="job-details">
                        <p><strong>Status:</strong> <span class="status status-${job.status.toLowerCase()}"><c:out value="${job.status}"/></span></p>
                        <p><strong>Uploaded:</strong> <fmt:formatDate value="${job.uploadTimestamp}" pattern="yyyy-MM-dd HH:mm:ss z"/> </p>
                        <c:if test="${not empty job.completionTimestamp}">
                            <p><strong>Finished:</strong> <fmt:formatDate value="${job.completionTimestamp}" pattern="yyyy-MM-dd HH:mm:ss z"/></p>
                        </c:if>
                    </div>

                    <h3>Images Processed in this Job (${images.size()}):</h3>
                    <c:if test="${empty images && job.status != 'PENDING'}">
                        <p>No images were processed for this job, or an error occurred during processing.</p>
                    </c:if>
                     <c:if test="${job.status == 'PENDING'}">
                        <p>This job is waiting to be processed. Results will appear shortly.</p>
                    </c:if>

                    <div class="image-gallery job-status-gallery">
                        <c:forEach var="img" items="${images}">
                            <div class="image-item-detailed">
                                <p class="filename" title="${img.originalFilename}"><c:out value="${img.originalFilename}"/></p>
                                <p class="detection-details">
                                    <strong>Detection Result:</strong>
                                    <c:choose>
                                        <c:when test="${empty img.detectionDetails && (job.status == 'PROCESSING' || job.status == 'PENDING')}">
                                            <span class="status-processing">Processing...</span>
                                        </c:when>
                                        <c:when test="${not empty img.detectionDetails}">
                                            <span class="<c:if test='${img.detectionDetails.toLowerCase().contains(\"no faces\")}'>status-info</c:if><c:if test='${img.detectionDetails.toLowerCase().contains(\"error\")}'>status-failed</c:if><c:if test='${img.detectionDetails.toLowerCase().contains(\"detected\")}'>status-completed</c:if>">
                                                <c:out value="${img.detectionDetails}"/>
                                            </span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="status-info">Awaiting processing</span>
                                        </c:otherwise>
                                    </c:choose>
                                </p>
                                <div class="image-comparison">
                                    <%-- Original Image Box --%>
                                    <div class="image-box original-box">
                                        <span class="box-label">Original Image</span>
                                        <a href="${pageContext.request.contextPath}/imageDisplay?imageId=${img.imageId}&type=original" target="_blank" title="View Full Original Image">
                                            <img src="${pageContext.request.contextPath}/imageDisplay?imageId=${img.imageId}&type=original"
                                                 alt="Original: ${img.originalFilename}" class="comparison-preview">
                                        </a>
                                    </div>
                                    <%-- Cropped Face Image Box --%>
                                    <div class="image-box facecrop-box"> <%-- Đổi tên class nếu cần cho CSS --%>
                                         <span class="box-label">Detected Face</span>
                                        <div class="preview-content">
                                            <c:choose>
                                                <c:when test="${not empty img.faceCropFilepath}">
                                                    <a href="${pageContext.request.contextPath}/imageDisplay?imageId=${img.imageId}&type=facecrop" target="_blank" title="View Cropped Face">
                                                        <img src="${pageContext.request.contextPath}/imageDisplay?imageId=${img.imageId}&type=facecrop"
                                                             alt="Detected face from ${img.originalFilename}" class="comparison-preview">
                                                    </a>
                                                </c:when>
                                                <c:when test="${job.status == 'PROCESSING' || job.status == 'PENDING' || empty img.detectionDetails}">
                                                    <div class="comparison-placeholder status-processing">Awaiting / Processing...</div>
                                                </c:when>
                                                <c:when test="${img.detectionDetails.toLowerCase().contains('no faces detected')}">
                                                    <div class="comparison-placeholder status-info">No Face Detected</div>
                                                </c:when>
                                                <c:otherwise> <%-- Thường là lỗi --%>
                                                    <div class="comparison-placeholder status-failed">Unavailable</div>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                    <p class="back-link"><a href="${pageContext.request.contextPath}/jobStatus">View All Jobs</a></p>
                </c:when>

                <c:otherwise> <%-- Displaying list of all jobs for the user --%>
                   <h2>Your Image Processing Jobs</h2>
                    <c:if test="${empty jobs}">
                        <p>You have no image processing jobs yet.</p>
                        <p><a href="${pageContext.request.contextPath}/upload" class="btn btn-primary">Upload Images Now</a></p>
                    </c:if>
                    <c:if test="${not empty jobs}">
                        <table class="job-table">
                            <thead>
                                <tr>
                                    <th>Job ID</th>
                                    <th>Status</th>
                                    <th>Uploaded At</th>
                                    <th>Finished At</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="j" items="${jobs}">
                                    <tr>
                                        <td><c:out value="${j.jobId}"/></td>
                                        <td><span class="status status-${j.status.toLowerCase()}"><c:out value="${j.status}"/></span></td>
                                        <td><fmt:formatDate value="${j.uploadTimestamp}" pattern="yyyy-MM-dd HH:mm"/></td>
                                        <td>
                                            <c:if test="${not empty j.completionTimestamp}">
                                                <fmt:formatDate value="${j.completionTimestamp}" pattern="yyyy-MM-dd HH:mm"/>
                                            </c:if>
                                            <c:if test="${empty j.completionTimestamp && (j.status == 'COMPLETED' || j.status == 'FAILED')}">
                                                <span>-</span>
                                            </c:if>
                                             <c:if test="${j.status == 'PENDING' || j.status == 'PROCESSING'}">
                                                <span>In Progress</span>
                                            </c:if>
                                        </td>
                                        <td><a href="${pageContext.request.contextPath}/jobStatus?jobId=${j.jobId}" class="btn btn-view">View Details</a></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </main>
         <footer>
            <p>© <%= new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) %> Face Detector App</p>
        </footer>
    </div>
</body>
</html>