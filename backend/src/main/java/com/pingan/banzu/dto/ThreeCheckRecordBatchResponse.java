package com.pingan.banzu.dto;

import java.util.List;

public record ThreeCheckRecordBatchResponse(
    int total, int successCount, int failureCount, List<Result> results) {

  public record Result(String id, boolean success, String message) {}
}
