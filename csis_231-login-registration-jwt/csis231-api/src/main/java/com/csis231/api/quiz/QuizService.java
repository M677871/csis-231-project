package com.csis231.api.quiz;

import com.csis231.api.quiz.dto.*;
import com.csis231.api.quizAnswer.QuizAnswer;
import com.csis231.api.quizAnswer.QuizAnswerRepository;
import com.csis231.api.quizQuestion.QuizQuestion;
import com.csis231.api.quizQuestion.QuizQuestionRepository;
import com.csis231.api.quizResult.QuizResult;
import com.csis231.api.quizResult.QuizResultRepository;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepo;
    private final QuizQuestionRepository questionRepo;
    private final QuizAnswerRepository answerRepo;
    private final QuizResultRepository resultRepo;
    private final UserRepository userRepo;

    public QuizToTakeDto getQuizForStudent(Long quizId, Long userId) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (u.getRole() != User.Role.STUDENT) {
            throw new RuntimeException("Only students can take quizzes");
        }

        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        List<QuizQuestion> questions = questionRepo.findByQuiz_Id(quizId);

        List<QuestionDto> dtoQuestions = new ArrayList<>();
        for (QuizQuestion q : questions) {
            List<QuizAnswer> answers = answerRepo.findByQuestion_Id(q.getId());

            List<AnswerOptionDto> dtoOptions = answers.stream()
                    .map(a -> new AnswerOptionDto(
                            a.getId(),
                            a.getAnswerText()
                    ))
                    .toList();

            dtoQuestions.add(new QuestionDto(
                    q.getId(),
                    q.getQuestionText(),
                    dtoOptions
            ));
        }

        return new QuizToTakeDto(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                dtoQuestions
        );
    }

    @Transactional
    public QuizResultDto submitQuiz(Long quizId, Long userId, SubmitQuizRequestDto submission) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (u.getRole() != User.Role.STUDENT) {
            throw new RuntimeException("Only students can submit quizzes");
        }

        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        // Map questionId -> correctAnswerId
        Map<Long, Long> correctAnswerByQuestion = new HashMap<>();
        List<QuizQuestion> quizQuestions = questionRepo.findByQuiz_Id(quizId);

        for (QuizQuestion q : quizQuestions) {
            List<QuizAnswer> answers = answerRepo.findByQuestion_Id(q.getId());
            answers.stream()
                    .filter(QuizAnswer::getIsCorrect)
                    .findFirst()
                    .ifPresent(correct -> {
                        correctAnswerByQuestion.put(q.getId(), correct.getId());
                    });
        }

        int total = quizQuestions.size();
        int correctCount = 0;

        for (SubmittedAnswerDto ans : submission.answers()) {
            Long qId = ans.questionId();
            Long chosen = ans.chosenAnswerId();
            Long correct = correctAnswerByQuestion.get(qId);
            if (correct != null && correct.equals(chosen)) {
                correctCount++;
            }
        }

        double score = (total == 0) ? 0.0 : (100.0 * correctCount / total);

        QuizResult result = QuizResult.builder()
                .quiz(quiz)
                .student(u)
                .score(score)
                .build();

        resultRepo.save(result);

        return new QuizResultDto(
                quiz.getId(),
                u.getId(),
                score,
                "Quiz submitted"
        );
    }
}
