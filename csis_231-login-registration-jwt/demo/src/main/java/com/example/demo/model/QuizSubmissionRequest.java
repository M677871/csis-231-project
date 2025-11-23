package com.example.demo.model;

import java.util.List;

/**
 * Request body for submitting quiz answers.
 */
public class QuizSubmissionRequest {
    private List<QuizSubmissionAnswer> answers;

    public QuizSubmissionRequest() {}
    public QuizSubmissionRequest(List<QuizSubmissionAnswer> answers) {
        this.answers = answers;
    }

    public List<QuizSubmissionAnswer> getAnswers() { return answers; }
    public void setAnswers(List<QuizSubmissionAnswer> answers) { this.answers = answers; }
}
