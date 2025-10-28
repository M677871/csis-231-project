package com.csis231.api.quizResult;

import com.csis231.api.quiz.Quiz;
import com.csis231.api.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "quiz_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student; // role == STUDENT

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private Instant completedAt;

    @PrePersist
    public void onCreate() {
        completedAt = Instant.now();
    }
}
