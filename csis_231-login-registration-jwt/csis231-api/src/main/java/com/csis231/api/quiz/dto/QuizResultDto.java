package com.csis231.api.quiz.dto;

public record QuizResultDto(
        Long quizId,
        Long studentUserId,
        double score,
        String message
) {}
