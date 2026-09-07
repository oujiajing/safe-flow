package com.pingan.banzu.dto;

public record MiniSafetyLearningCheckInResponse(
    Long learningContentId,
    String checkInStatus,
    Integer pointsAwarded,
    ThreeCheckRecordDetailResponse pointsRecord) {}
