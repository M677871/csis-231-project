package com.csis231.api.quizAnswer;

import com.csis231.api.quizQuestion.QuizQuestion;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quiz_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAnswer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;
}
