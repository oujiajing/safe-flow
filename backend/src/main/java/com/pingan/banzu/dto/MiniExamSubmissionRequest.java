package com.pingan.banzu.dto;

import java.util.List;

public record MiniExamSubmissionRequest(
    String requestId, List<MiniExamAnswerRequest> answers, Integer currentQuestionIndex,
    Integer remainingSeconds) {}
