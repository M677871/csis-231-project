package com.csis231.api.quiz.dto;

import java.util.List;

public record QuizToTakeDto(
        Long quizId,
        String quizTitle,
        String quizDescription,
        List<QuestionDto> questions
) {
    public record QuestionDto(
            Long questionId,
            String questionText,
            List<AnswerDto> answers
    ) {
        public record AnswerDto(
                Long answerId,
                String answerText
                // NO isCorrect here
        ) {}
    }
}

