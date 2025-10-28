package com.csis231.api.quizQuestion;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByQuiz_Id(Long quizId);
}
