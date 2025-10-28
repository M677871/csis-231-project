package com.csis231.api.quizAnswer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    List<QuizAnswer> findByQuestion_Id(Long questionId);
}
