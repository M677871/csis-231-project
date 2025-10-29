package com.csis231.api.course;

import com.csis231.api.category.Category;
import com.csis231.api.course.dto.CourseDetailsDto;
import com.csis231.api.course.dto.CourseSummaryDto;
import com.csis231.api.course.dto.CreateCourseRequest;
import com.csis231.api.course.dto.EnrollResponseDto;
import com.csis231.api.course.dto.MaterialDto;
import com.csis231.api.course.dto.QuizSummaryDto;
import com.csis231.api.courseEnrollement.CourseEnrollment;
import com.csis231.api.courseEnrollement.CourseEnrollmentRepository;
import com.csis231.api.courseMaterial.CourseMaterial;
import com.csis231.api.courseMaterial.CourseMaterialRepository;
import com.csis231.api.quiz.Quiz;
import com.csis231.api.quiz.QuizRepository;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepo;
    private final CourseMaterialRepository materialRepo;
    private final QuizRepository quizRepo;
    private final CourseEnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;

    // list courses that are published (public catalog)
    public List<CourseSummaryDto> listPublishedCourses() {
        return courseRepo.findByPublishedTrue()
                .stream()
                .map(c -> new CourseSummaryDto(
                        c.getId(),
                        c.getCourseName(),
                        c.getDescription(),
                        c.getInstructor().getFirstName() + " " + c.getInstructor().getLastName(),
                        Boolean.TRUE.equals(c.getPublished())
                ))
                .toList();
    }

    // get full detail for a specific course
    public CourseDetailsDto getCourseDetails(Long courseId) {
        Course c = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // materials
        List<CourseMaterial> mats = materialRepo.findByCourse_Id(courseId);
        var materialDtos = mats.stream()
                .map(m -> new MaterialDto(
                        m.getId(),
                        m.getTitle(),
                        m.getMaterialType(),
                        m.getResourcePath()
                ))
                .toList();

        // quizzes
        List<Quiz> quizzes = quizRepo.findByCourse_Id(courseId);
        var quizDtos = quizzes.stream()
                .map(q -> new QuizSummaryDto(
                        q.getId(),
                        q.getTitle(),
                        q.getDescription()
                ))
                .toList();

        // categories
        List<String> catNames = new ArrayList<>();
        List<Category> cats = c.getCategories();
        if (cats != null) {
            catNames = cats.stream()
                    .map(Category::getName)
                    .collect(Collectors.toList());
        }

        return new CourseDetailsDto(
                c.getId(),
                c.getCourseName(),
                c.getDescription(),
                c.getInstructor().getFirstName() + " " + c.getInstructor().getLastName(),
                Boolean.TRUE.equals(c.getPublished()),
                c.getCreatedAt(),
                catNames,
                materialDtos,
                quizDtos
        );
    }

    // enroll a STUDENT into a course
    @Transactional
    public EnrollResponseDto enrollStudentInCourse(Long userId, Long courseId) {
        User student = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (student.getRole() != User.Role.STUDENT) {
            throw new RuntimeException("Only students can enroll");
        }

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        boolean exists = enrollmentRepo.existsByStudentAndCourse(student, course);
        if (exists) {
            return new EnrollResponseDto(
                    courseId,
                    student.getId(),
                    "ACTIVE",
                    "Already enrolled"
            );
        }

        CourseEnrollment ce = CourseEnrollment.builder()
                .student(student)
                .course(course)
                .status("ACTIVE")
                .build();

        enrollmentRepo.save(ce);

        return new EnrollResponseDto(
                courseId,
                student.getId(),
                ce.getStatus(),
                "Enrollment successful"
        );
    }

    // create a course as INSTRUCTOR
    @Transactional
    public CourseSummaryDto createCourseAsInstructor(Long instructorUserId, CreateCourseRequest req) {
        User instructor = userRepo.findById(instructorUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (instructor.getRole() != User.Role.INSTRUCTOR) {
            throw new RuntimeException("Only instructors can create courses");
        }

        Course course = Course.builder()
                .courseName(req.courseName())
                .description(req.description())
                .instructor(instructor)
                .published(req.published() != null ? req.published() : Boolean.FALSE)
                .categories(null) // hook up categories later if you want
                .build();

        courseRepo.save(course);

        return new CourseSummaryDto(
                course.getId(),
                course.getCourseName(),
                course.getDescription(),
                instructor.getFirstName() + " " + instructor.getLastName(),
                Boolean.TRUE.equals(course.getPublished())
        );
    }
}
