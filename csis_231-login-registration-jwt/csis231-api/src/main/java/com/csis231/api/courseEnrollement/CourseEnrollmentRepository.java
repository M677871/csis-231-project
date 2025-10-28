package com.csis231.api.courseEnrollement;

import com.csis231.api.course.Course;
import com.csis231.api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {
    boolean existsByStudentAndCourse(User student, Course course);
    List<CourseEnrollment> findByStudent(User student);
}
