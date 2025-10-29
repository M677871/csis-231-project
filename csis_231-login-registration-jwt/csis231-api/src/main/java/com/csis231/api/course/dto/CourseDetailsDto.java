package com.csis231.api.course.dto;

import java.time.Instant;
import java.util.List;

public record CourseDetailsDto(
        Long id,
        String courseName,
        String description,
        String instructorName,
        boolean published,
        Instant createdAt,
        List<String> categories,
        List<MaterialDto> materials,
        List<QuizSummaryDto> quizzes
) {}
