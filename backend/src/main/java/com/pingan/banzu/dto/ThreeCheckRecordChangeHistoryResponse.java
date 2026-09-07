package com.pingan.banzu.dto;

import java.util.List;

public record ThreeCheckRecordChangeHistoryResponse(
    ThreeCheckRecordDetailResponse record, List<ThreeCheckRecordChangeHistoryItem> items) {}
