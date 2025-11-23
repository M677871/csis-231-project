package com.csis231.api.enrollment;

import java.time.Instant;

/**
 * Response DTO representing a course enrollment.
 */
public record EnrollmentResponse(
        Long id,
        Long studentUserId,
        Long courseId,
        String courseTitle,
        CourseEnrollment.EnrollmentStatus status,
        Instant enrolledAt
) {}
