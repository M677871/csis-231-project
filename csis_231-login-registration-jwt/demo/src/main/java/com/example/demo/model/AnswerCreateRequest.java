package com.example.demo.model;

/**
 * Represents a single answer option when creating questions.
 */
public class AnswerCreateRequest {
    private String answerText;
    private boolean correct;

    public AnswerCreateRequest() {}
    public AnswerCreateRequest(String answerText, boolean correct) {
        this.answerText = answerText;
        this.correct = correct;
    }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
}
