package com.csis231.api.course.dto;

public record EnrollResponseDto(
        Long courseId,
        Long studentUserId,
        String status,
        String message
) {}
