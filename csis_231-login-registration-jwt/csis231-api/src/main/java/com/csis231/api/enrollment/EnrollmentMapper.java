package com.csis231.api.enrollment;

/**
 * Utility class converting enrollment entities to DTOs.
 */
public final class EnrollmentMapper {
    private EnrollmentMapper() {}

    public static EnrollmentResponse toDto(CourseEnrollment enrollment) {
        if (enrollment == null) return null;
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent() != null ? enrollment.getStudent().getId() : null,
                enrollment.getCourse() != null ? enrollment.getCourse().getId() : null,
                enrollment.getCourse() != null ? enrollment.getCourse().getTitle() : null,
                enrollment.getStatus(),
                enrollment.getEnrolledAt()
        );
    }
}
