package com.csis231.api.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    List<QuizResult> findTop5ByStudent_IdOrderByCompletedAtDesc(Long studentId);
    List<QuizResult> findByQuiz_Id(Long quizId);
    List<QuizResult> findByQuiz_Course_Id(Long courseId);

    java.util.Optional<QuizResult> findTop1ByQuiz_IdAndStudent_IdOrderByCompletedAtDesc(Long quizId, Long studentId);
}
