package com.pingan.banzu.dto;

import java.util.List;

public record MiniExamProgressRequest(
    List<MiniExamAnswerRequest> answers, Integer currentQuestionIndex, Integer remainingSeconds) {}
