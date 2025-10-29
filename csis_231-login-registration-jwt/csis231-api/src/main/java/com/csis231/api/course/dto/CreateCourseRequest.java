package com.csis231.api.course.dto;

import java.util.List;

public record CreateCourseRequest(
        String courseName,
        String description,
        Boolean published,
        List<Long> categoryIds
) {}
