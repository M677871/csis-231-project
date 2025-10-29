package com.csis231.api.quiz;

import com.csis231.api.quiz.dto.QuizResultDto;
import com.csis231.api.quiz.dto.QuizToTakeDto;
import com.csis231.api.quiz.dto.SubmitQuizRequestDto;
import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/quizzes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final UserRepository userRepo;

    @GetMapping("/{quizId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<QuizToTakeDto> getQuiz(@PathVariable Long quizId) {
        User current = getCurrentUser();
        QuizToTakeDto dto = quizService.getQuizForStudent(quizId, current.getId());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{quizId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<QuizResultDto> submitQuiz(
            @PathVariable Long quizId,
            @RequestBody SubmitQuizRequestDto submission
    ) {
        User current = getCurrentUser();
        QuizResultDto dto = quizService.submitQuiz(quizId, current.getId(), submission);
        return ResponseEntity.ok(dto);
    }

    private User getCurrentUser() {
        String principalName = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepo.findByUsername(principalName)
                .orElseGet(() -> userRepo.findByEmail(principalName)
                        .orElseThrow(() -> new RuntimeException("User not found")));
    }

}
