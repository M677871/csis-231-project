package com.csis231.api.enrollment;

import com.csis231.api.common.BadRequestException;
import com.csis231.api.common.ConflictException;
import com.csis231.api.common.ResourceNotFoundException;
import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.course.Course;
import com.csis231.api.course.CourseRepository;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling enrollment flows.
 */
@Service
@RequiredArgsConstructor
public class EnrollmentService {
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional
    public CourseEnrollment enroll(User actor, EnrollmentRequest req) {
        if (req == null || req.courseId() == null) {
            throw new BadRequestException("courseId is required");
        }
        if (actor == null) throw new UnauthorizedException("Authentication required");

        Long targetStudentId = req.studentUserId() != null ? req.studentUserId() : actor.getId();
        User targetStudent = userRepository.findById(targetStudentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + targetStudentId));

        // only the student himself or an admin can enroll this student
        boolean isSelf = actor.getId().equals(targetStudentId);
        if (actor.getRole() == User.Role.INSTRUCTOR) {
            throw new UnauthorizedException("Instructors cannot enroll students");
        }
        if (!isSelf && actor.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("You cannot enroll another student");
        }
        if (targetStudent.getRole() != User.Role.STUDENT && actor.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("Only students can be enrolled");
        }

        Course course = courseRepository.findById(req.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + req.courseId()));
        if (Boolean.FALSE.equals(course.getPublished())) {
            throw new UnauthorizedException("Cannot enroll in an unpublished course");
        }

        if (enrollmentRepository.existsByStudent_IdAndCourse_Id(targetStudentId, req.courseId())) {
            throw new ConflictException("Already enrolled in this course");
        }

        CourseEnrollment enrollment = CourseEnrollment.builder()
                .course(course)
                .student(targetStudent)
                .status(CourseEnrollment.EnrollmentStatus.ENROLLED)
                .build();
        return enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<CourseEnrollment> findByStudent(Long studentId) {
        return enrollmentRepository.findByStudent_Id(studentId);
    }

    @Transactional(readOnly = true)
    public List<CourseEnrollment> findByCourse(Long courseId, User actor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        if (actor == null) throw new UnauthorizedException("Authentication required");
        if (actor.getRole() == User.Role.ADMIN) {
            return enrollmentRepository.findByCourse_Id(courseId);
        }
        if (actor.getRole() == User.Role.INSTRUCTOR) {
            if (course.getInstructor() != null && course.getInstructor().getId() != null
                    && course.getInstructor().getId().equals(actor.getId())) {
                return enrollmentRepository.findByCourse_Id(courseId);
            }
            throw new UnauthorizedException("You can only view enrollments for your courses");
        }
        throw new UnauthorizedException("Only instructors or admins can view course enrollments");
    }

    @Transactional(readOnly = true)
    public long countForCourse(Long courseId) {
        return enrollmentRepository.countByCourse_Id(courseId);
    }

    @Transactional(readOnly = true)
    public boolean isStudentEnrolled(Long studentId, Long courseId) {
        return enrollmentRepository.existsByStudent_IdAndCourse_Id(studentId, courseId);
    }
}
