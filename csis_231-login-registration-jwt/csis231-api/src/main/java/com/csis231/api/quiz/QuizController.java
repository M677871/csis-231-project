package com.csis231.api.quiz;

import com.csis231.api.common.ResourceNotFoundException;
import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for quiz authoring and delivery.
 */
@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {
    private final QuizService quizService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<QuizSummaryDto> create(@Valid @RequestBody QuizCreateRequest request,
                                                 Authentication authentication) {
        User actor = resolveUser(authentication);
        Quiz quiz = quizService.createQuiz(request, actor);
        QuizSummaryDto dto = QuizMapper.toSummaryDto(quiz, 0);
        return ResponseEntity.status(201).body(dto);
    }

    @PostMapping("/{quizId}/questions")
    public ResponseEntity<Void> addQuestions(@PathVariable Long quizId,
                                             @Valid @RequestBody List<QuizQuestionRequest> questions,
                                             Authentication authentication) {
        User actor = resolveUser(authentication);
        quizService.addQuestions(quizId, questions, actor);
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/{quizId}")
    public QuizDetailDto get(@PathVariable Long quizId, Authentication authentication) {
        User viewer = resolveUser(authentication);
        return quizService.getQuizDetail(quizId, viewer);
    }

    @PostMapping("/{quizId}/submit")
    public QuizSubmissionResponse submit(@PathVariable Long quizId,
                                         @Valid @RequestBody QuizSubmissionRequest request,
                                         Authentication authentication) {
        User actor = resolveUser(authentication);
        return quizService.submitQuiz(quizId, request, actor);
    }

    @GetMapping("/{quizId}/results")
    public List<QuizResultDto> results(@PathVariable Long quizId, Authentication authentication) {
        User actor = resolveUser(authentication);
        return quizService.resultsForQuiz(quizId, actor);
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }
}
