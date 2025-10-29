package com.csis231.api.course.dto;

public record CourseSummaryDto(
        Long id,
        String courseName,
        String description,
        String instructorName,
        boolean published
) {}
