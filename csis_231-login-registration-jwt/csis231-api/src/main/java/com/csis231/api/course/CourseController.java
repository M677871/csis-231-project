package com.csis231.api.course;

import com.csis231.api.course.dto.CourseDetailsDto;
import com.csis231.api.course.dto.CourseSummaryDto;
import com.csis231.api.course.dto.CreateCourseRequest;
import com.csis231.api.course.dto.EnrollResponseDto;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final UserRepository userRepo;

    // Public: list of published courses
    @GetMapping
    public ResponseEntity<List<CourseSummaryDto>> listCourses() {
        return ResponseEntity.ok(courseService.listPublishedCourses());
    }

    // Public: course details page
    @GetMapping("/{id}")
    public ResponseEntity<CourseDetailsDto> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseDetails(id));
    }

    // Student: enroll in course
    @PostMapping("/{id}/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollResponseDto> enroll(@PathVariable Long id) {
        User current = getCurrentUser();
        EnrollResponseDto dto = courseService.enrollStudentInCourse(current.getId(), id);
        return ResponseEntity.ok(dto);
    }

    // Instructor: create a course
    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<CourseSummaryDto> createCourse(@RequestBody CreateCourseRequest req) {
        User current = getCurrentUser();
        CourseSummaryDto created = courseService.createCourseAsInstructor(current.getId(), req);
        return ResponseEntity.ok(created);
    }

    // helper to resolve the currently authenticated User from JWT
    private User getCurrentUser() {
        String principalName = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepo.findByUsername(principalName)
                .orElseGet(() -> userRepo.findByEmail(principalName)
                        .orElseThrow(() -> new RuntimeException("User not found")));
    }
}
