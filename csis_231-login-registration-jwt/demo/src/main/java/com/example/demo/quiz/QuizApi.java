package com.example.demo.quiz;

import com.example.demo.common.ApiClient;
import com.example.demo.common.ApiResponse;
import com.example.demo.model.*;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

/**
 * HTTP wrapper for quiz endpoints.
 */
public class QuizApi {
    private final ApiClient client = new ApiClient();

    public QuizDetailDto getQuiz(Long quizId) {
        ApiResponse<QuizDetailDto> resp = client.get("/api/quizzes/" + quizId, new TypeReference<QuizDetailDto>() {});
        return resp.getBody();
    }

    public QuizSummaryDto createQuiz(QuizCreateRequest req) {
        ApiResponse<QuizSummaryDto> resp = client.post("/api/quizzes", req, new TypeReference<QuizSummaryDto>() {});
        return resp.getBody();
    }

    public void addQuestions(Long quizId, List<QuizQuestionRequest> questions) {
        client.post("/api/quizzes/" + quizId + "/questions", questions);
    }

    public QuizSubmissionResponse submit(Long quizId, QuizSubmissionRequest req) {
        ApiResponse<QuizSubmissionResponse> resp = client.post("/api/quizzes/" + quizId + "/submit", req, new TypeReference<QuizSubmissionResponse>() {});
        return resp.getBody();
    }

    public QuizResultDto[] results(Long quizId) {
        ApiResponse<QuizResultDto[]> resp = client.get("/api/quizzes/" + quizId + "/results", new TypeReference<QuizResultDto[]>() {});
        return resp.getBody();
    }

    public void deleteQuiz(Long quizId) {
        client.delete("/api/quizzes/" + quizId);
    }
}
