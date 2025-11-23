package com.csis231.api.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link CourseEnrollment} entities.
 */
@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {
    boolean existsByStudent_IdAndCourse_Id(Long studentId, Long courseId);
    List<CourseEnrollment> findByStudent_Id(Long studentId);
    List<CourseEnrollment> findByCourse_Id(Long courseId);
    long countByCourse_Id(Long courseId);
    Optional<CourseEnrollment> findByStudent_IdAndCourse_Id(Long studentId, Long courseId);
}
