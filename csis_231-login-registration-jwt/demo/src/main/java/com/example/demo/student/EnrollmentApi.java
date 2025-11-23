package com.example.demo.student;

import com.example.demo.common.ApiClient;
import com.example.demo.common.ApiResponse;
import com.example.demo.model.EnrollmentRequest;
import com.example.demo.model.EnrollmentResponse;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Client for enrollment endpoints.
 */
public class EnrollmentApi {
    private final ApiClient client = new ApiClient();

    public EnrollmentResponse enroll(EnrollmentRequest req) {
        ApiResponse<EnrollmentResponse> resp = client.post("/api/enrollments/enroll", req, new TypeReference<EnrollmentResponse>() {});
        return resp.getBody();
    }

    public EnrollmentResponse[] listByStudent(Long userId) {
        ApiResponse<EnrollmentResponse[]> resp = client.get("/api/students/" + userId + "/enrollments", new TypeReference<EnrollmentResponse[]>() {});
        return resp.getBody();
    }
}
