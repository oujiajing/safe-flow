package com.pingan.banzu.dto;

import java.util.List;

public record ThreeCheckRecordWorkflowResponse(
    List<ThreeCheckWorkflowItem> documentFlow, List<ThreeCheckWorkflowItem> changeHistory) {}
