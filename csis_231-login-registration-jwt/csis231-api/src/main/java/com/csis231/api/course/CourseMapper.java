package com.csis231.api.course;

import com.csis231.api.coursematerial.CourseMaterialDto;
import com.csis231.api.quiz.QuizSummaryDto;
import com.csis231.api.user.User;

import java.util.List;

/**
 * Simple mapper utilities for converting {@link Course} entities to DTOs.
 */
public final class CourseMapper {
    private CourseMapper() {}

    public static CourseDto toDto(Course course) {
        if (course == null) return null;
        User instructor = course.getInstructor();
        return new CourseDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                instructor != null ? instructor.getId() : null,
                instructor != null ? instructor.getUsername() : null,
                course.getCategory() != null ? course.getCategory().getId() : null,
                course.getPublished(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }

    public static CourseDetailDto toDetailDto(Course course,
                                              List<CourseMaterialDto> materials,
                                              List<QuizSummaryDto> quizzes) {
        CourseDto base = toDto(course);
        return new CourseDetailDto(
                base.id(),
                base.title(),
                base.description(),
                base.instructorUserId(),
                base.instructorName(),
                base.categoryId(),
                base.published(),
                base.createdAt(),
                base.updatedAt(),
                materials,
                quizzes
        );
    }
}
