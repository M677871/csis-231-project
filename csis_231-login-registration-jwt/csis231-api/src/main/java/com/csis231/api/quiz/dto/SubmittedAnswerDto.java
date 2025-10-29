package com.csis231.api.quiz.dto;

public record SubmittedAnswerDto(
        Long questionId,
        Long chosenAnswerId
) {}
