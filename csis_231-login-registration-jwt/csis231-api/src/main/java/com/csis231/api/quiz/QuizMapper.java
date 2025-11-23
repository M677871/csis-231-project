package com.csis231.api.quiz;

import java.util.List;

/**
 * Mapper utilities for quiz-related DTOs.
 */
public final class QuizMapper {
    private QuizMapper() {}

    public static QuizSummaryDto toSummaryDto(Quiz quiz) {
        return toSummaryDto(quiz, 0);
    }

    public static QuizSummaryDto toSummaryDto(Quiz quiz, int questionCount) {
        if (quiz == null) return null;
        return new QuizSummaryDto(
                quiz.getId(),
                quiz.getCourse() != null ? quiz.getCourse().getId() : null,
                quiz.getName(),
                quiz.getDescription(),
                questionCount,
                quiz.getCreatedAt()
        );
    }

    public static QuizDetailDto toDetailDto(Quiz quiz, List<QuizQuestionDto> questions) {
        if (quiz == null) return null;
        return new QuizDetailDto(
                quiz.getId(),
                quiz.getCourse() != null ? quiz.getCourse().getId() : null,
                quiz.getName(),
                quiz.getDescription(),
                questions
        );
    }

    public static QuizResultDto toResultDto(QuizResult result) {
        if (result == null) return null;
        return new QuizResultDto(
                result.getId(),
                result.getQuiz() != null ? result.getQuiz().getId() : null,
                result.getStudent() != null ? result.getStudent().getId() : null,
                result.getScore(),
                result.getTotalQuestions(),
                result.getCompletedAt()
        );
    }
}
