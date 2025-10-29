package com.csis231.api.quiz.dto;

import java.util.List;

public record QuestionDto(
        Long questionId,
        String questionText,
        List<AnswerOptionDto> options
) {}
