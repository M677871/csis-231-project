package com.csis231.api.course.dto;

public record MaterialDto(
        Long id,
        String title,
        String materialType,
        String resourcePath
) {}
