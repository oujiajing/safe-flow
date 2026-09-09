package com.pingan.banzu.dto;

import java.math.BigDecimal;

public record TrainingExamResultRequest(
    Long taskId, Long examPersonUserId, BigDecimal score, String status) {}
