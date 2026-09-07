package com.pingan.banzu.dto;

import java.util.List;

public record ThreeCheckFlowResponse(
    String rootDispatchRecordId,
    String rootDispatchModuleKey,
    String businessDate,
    String company,
    String department,
    String team,
    List<ThreeCheckFlowStageItem> stages) {}
