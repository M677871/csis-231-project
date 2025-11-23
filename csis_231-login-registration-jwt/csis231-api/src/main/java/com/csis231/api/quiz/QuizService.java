package com.csis231.api.quiz;

import com.csis231.api.common.BadRequestException;
import com.csis231.api.common.ResourceNotFoundException;
import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.course.Course;
import com.csis231.api.course.CourseRepository;
import com.csis231.api.enrollment.EnrollmentService;
import com.csis231.api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service encapsulating quiz authoring, delivery and grading.
 */
@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final QuizResultRepository resultRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentService enrollmentService;

    @Transactional
    public Quiz createQuiz(QuizCreateRequest req, User actor) {
        requireInstructorOrAdmin(actor);
        Course course = courseRepository.findById(req.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + req.courseId()));
        ensureCourseOwnership(actor, course);

        Quiz quiz = Quiz.builder()
                .course(course)
                .name(req.name())
                .description(req.description())
                .build();
        return quizRepository.save(quiz);
    }

    @Transactional
    public List<QuizQuestion> addQuestions(Long quizId, List<QuizQuestionRequest> requests, User actor) {
        if (quizId == null) throw new BadRequestException("quizId required");
        if (requests == null || requests.isEmpty()) throw new BadRequestException("Questions are required");
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
        ensureCourseOwnership(actor, quiz.getCourse());

        List<QuizQuestion> createdQuestions = new ArrayList<>();
        for (QuizQuestionRequest request : requests) {
            boolean hasCorrect = request.answers().stream().anyMatch(AnswerCreateRequest::correct);
            if (!hasCorrect) {
                throw new BadRequestException("Each question must have at least one correct answer");
            }
            QuizQuestion question = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionText(request.questionText())
                    .build();
            QuizQuestion saved = questionRepository.save(question);
            createdQuestions.add(saved);

            for (AnswerCreateRequest answerReq : request.answers()) {
                AnswerOption option = AnswerOption.builder()
                        .question(saved)
                        .answerText(answerReq.answerText())
                        .correct(answerReq.correct())
                        .build();
                answerOptionRepository.save(option);
            }
        }
        return createdQuestions;
    }

    @Transactional(readOnly = true)
    public QuizDetailDto getQuizDetail(Long quizId, User viewer) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
        enforceQuizAccessForView(quiz, viewer);

        List<QuizQuestion> questions = questionRepository.findByQuiz_Id(quizId);
        List<Long> questionIds = questions.stream().map(QuizQuestion::getId).toList();
        Map<Long, List<AnswerOption>> optionsByQuestion = answerOptionRepository.findByQuestion_IdIn(questionIds).stream()
                .collect(Collectors.groupingBy(opt -> opt.getQuestion().getId()));

        List<QuizQuestionDto> questionDtos = questions.stream().map(q -> {
            List<AnswerOptionDto> options = optionsByQuestion.getOrDefault(q.getId(), Collections.emptyList())
                    .stream()
                    .map(opt -> new AnswerOptionDto(opt.getId(), opt.getAnswerText()))
                    .toList();
            return new QuizQuestionDto(q.getId(), q.getQuestionText(), options);
        }).toList();

        return QuizMapper.toDetailDto(quiz, questionDtos);
    }

    @Transactional
    public QuizSubmissionResponse submitQuiz(Long quizId, QuizSubmissionRequest request, User actor) {
        if (actor == null) throw new UnauthorizedException("Authentication required");
        if (actor.getRole() != User.Role.STUDENT && actor.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("Only students can submit quizzes");
        }
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new BadRequestException("Answers are required");
        }
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));

        if (actor.getRole() == User.Role.STUDENT) {
            boolean enrolled = enrollmentService.isStudentEnrolled(actor.getId(), quiz.getCourse().getId());
            if (!enrolled) throw new UnauthorizedException("Enroll in the course before taking the quiz");
        }

        List<QuizQuestion> questions = questionRepository.findByQuiz_Id(quizId);
        Map<Long, QuizQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));
        List<Long> questionIds = questions.stream().map(QuizQuestion::getId).toList();
        Map<Long, List<AnswerOption>> optionsByQuestion = answerOptionRepository.findByQuestion_IdIn(questionIds).stream()
                .collect(Collectors.groupingBy(opt -> opt.getQuestion().getId()));

        int totalQuestions = questions.size();
        int score = 0;

        for (QuizSubmissionAnswer answer : request.answers()) {
            QuizQuestion question = questionMap.get(answer.questionId());
            if (question == null) {
                throw new BadRequestException("Answer references invalid question: " + answer.questionId());
            }
            List<AnswerOption> options = optionsByQuestion.getOrDefault(question.getId(), Collections.emptyList());
            Optional<AnswerOption> chosen = options.stream()
                    .filter(opt -> Objects.equals(opt.getId(), answer.answerId()))
                    .findFirst();
            if (chosen.isEmpty()) {
                throw new BadRequestException("Invalid answer option for question: " + question.getId());
            }
            if (Boolean.TRUE.equals(chosen.get().getCorrect())) {
                score++;
            }
        }

        QuizResult result = QuizResult.builder()
                .quiz(quiz)
                .student(actor)
                .score(score)
                .totalQuestions(totalQuestions)
                .build();
        QuizResult saved = resultRepository.save(result);

        double percentage = totalQuestions == 0 ? 0 : (score * 100.0 / totalQuestions);
        return new QuizSubmissionResponse(
                quiz.getId(),
                actor.getId(),
                score,
                totalQuestions,
                percentage,
                saved.getCompletedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<QuizResultDto> resultsForQuiz(Long quizId, User actor) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
        ensureCourseOwnership(actor, quiz.getCourse());
        return resultRepository.findByQuiz_Id(quizId).stream()
                .map(QuizMapper::toResultDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuizResultDto> latestResultsForStudent(Long studentId) {
        return resultRepository.findTop5ByStudent_IdOrderByCompletedAtDesc(studentId).stream()
                .map(QuizMapper::toResultDto)
                .collect(Collectors.toList());
    }

    private static void requireInstructorOrAdmin(User actor) {
        if (actor == null) throw new UnauthorizedException("Authentication required");
        if (actor.getRole() == User.Role.ADMIN || actor.getRole() == User.Role.INSTRUCTOR) return;
        throw new UnauthorizedException("Only instructors or admins can manage quizzes");
    }

    private static void ensureCourseOwnership(User actor, Course course) {
        requireInstructorOrAdmin(actor);
        if (actor.getRole() == User.Role.ADMIN) return;
        if (course == null || course.getInstructor() == null || !Objects.equals(course.getInstructor().getId(), actor.getId())) {
            throw new UnauthorizedException("You can only manage quizzes for your own courses");
        }
    }

    private void enforceQuizAccessForView(Quiz quiz, User viewer) {
        if (viewer == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (viewer.getRole() == User.Role.ADMIN) return;
        if (viewer.getRole() == User.Role.INSTRUCTOR) {
            ensureCourseOwnership(viewer, quiz.getCourse());
            return;
        }
        if (viewer.getRole() == User.Role.STUDENT) {
            boolean enrolled = enrollmentService.isStudentEnrolled(viewer.getId(), quiz.getCourse().getId());
            if (!enrolled) {
                throw new UnauthorizedException("Enroll in the course to access this quiz");
            }
            return;
        }
        throw new UnauthorizedException("Not authorized");
    }
}
