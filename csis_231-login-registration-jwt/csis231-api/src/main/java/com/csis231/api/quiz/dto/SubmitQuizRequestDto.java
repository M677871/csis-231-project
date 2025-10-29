package com.csis231.api.quiz.dto;

import java.util.List;

public record SubmitQuizRequestDto(
        List<SubmittedAnswerDto> answers
) {}
