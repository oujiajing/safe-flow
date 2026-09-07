package com.pingan.banzu.dto;

import java.util.List;

public record RiskControlImportResult(
    RiskControlLibraryResponse library, int successRows, List<String> errors) {}
