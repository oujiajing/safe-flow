package com.pingan.banzu.dto;

import java.util.List;

public record ThreeCheckRecordDocumentFlowResponse(
    ThreeCheckRecordDetailResponse record,
    List<ThreeCheckRecordDocumentFlowStatusLog> statusLogs,
    HazardRectificationOrderDetailResponse linkedRectificationOrder) {}
