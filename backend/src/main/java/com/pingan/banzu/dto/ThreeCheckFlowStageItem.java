package com.pingan.banzu.dto;

public record ThreeCheckFlowStageItem(
    String stageKey,
    String stageLabel,
    String moduleKey,
    ThreeCheckRecordListItem record) {}
