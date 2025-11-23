package com.example.demo.course;

import com.example.demo.common.ApiClient;
import com.example.demo.common.ApiResponse;
import com.example.demo.common.PageResponse;
import com.example.demo.model.*;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP wrapper for course-related endpoints.
 */
public class CourseApi {
    private final ApiClient client = new ApiClient();

    public PageResponse<CourseDto> listPublished(int page, int size, Long categoryId, String search) {
        StringBuilder path = new StringBuilder("/api/courses?page=" + Math.max(page, 0) + "&size=" + Math.max(size, 1));
        if (categoryId != null) path.append("&categoryId=").append(categoryId);
        if (search != null && !search.isBlank()) path.append("&search=").append(search.trim());
        ApiResponse<PageResponse<CourseDto>> resp = client.get(path.toString(), new TypeReference<PageResponse<CourseDto>>() {});
        return resp.getBody();
    }

    public CourseDetailDto get(Long id) {
        ApiResponse<CourseDetailDto> resp = client.get("/api/courses/" + id, new TypeReference<CourseDetailDto>() {});
        return resp.getBody();
    }

    public CourseDto create(CourseRequest req) {
        ApiResponse<CourseDto> resp = client.post("/api/courses", req, new TypeReference<CourseDto>() {});
        return resp.getBody();
    }

    public CourseDto update(Long id, CourseRequest req) {
        ApiResponse<CourseDto> resp = client.put("/api/courses/" + id, req, new TypeReference<CourseDto>() {});
        return resp.getBody();
    }

    public void delete(Long id) {
        client.delete("/api/courses/" + id);
    }

    public CourseMaterialDto addMaterial(Long courseId, CourseMaterialRequest req) {
        ApiResponse<CourseMaterialDto> resp = client.post("/api/courses/" + courseId + "/materials", req, new TypeReference<CourseMaterialDto>() {});
        return resp.getBody();
    }

    public void deleteMaterial(Long materialId) {
        client.delete("/api/materials/" + materialId);
    }

    public CourseDetailDto getWithViewer(Long id) {
        return get(id);
    }

    public CourseDto[] listInstructorCourses(Long userId) {
        ApiResponse<CourseDto[]> resp = client.get("/api/instructors/" + userId + "/courses", new TypeReference<CourseDto[]>() {});
        return resp.getBody();
    }

    public EnrollmentResponse[] listCourseEnrollments(Long courseId) {
        ApiResponse<EnrollmentResponse[]> resp = client.get("/api/courses/" + courseId + "/enrollments", new TypeReference<EnrollmentResponse[]>() {});
        return resp.getBody();
    }
}
