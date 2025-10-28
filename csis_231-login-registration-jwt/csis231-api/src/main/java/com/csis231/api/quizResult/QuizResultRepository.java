package com.csis231.api.quizResult;

import com.csis231.api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    List<QuizResult> findByStudent(User student);
    List<QuizResult> findByQuiz_Id(Long quizId);
}
