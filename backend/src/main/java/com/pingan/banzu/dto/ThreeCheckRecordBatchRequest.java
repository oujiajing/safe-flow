package com.pingan.banzu.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ThreeCheckRecordBatchRequest(String action, @NotEmpty List<Long> ids, String reason) {}
