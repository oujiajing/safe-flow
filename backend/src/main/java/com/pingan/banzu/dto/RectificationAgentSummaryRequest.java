package com.pingan.banzu.dto;

import java.util.List;

public record RectificationAgentSummaryRequest(int orderVersion, String hazardDescription,
    String rectificationDescription, String acceptanceResult, String acceptanceRemark,
    List<String> measures, List<String> acceptanceCriteria, List<String> flowFacts, List<String> citations) {}
