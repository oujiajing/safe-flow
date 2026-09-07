package com.pingan.banzu.system.dto;

import java.util.List;

public record SystemImportResult(
    Long jobId, String module, String status, Integer totalRows, Integer successRows, Integer failureRows, List<String> errors) {}
