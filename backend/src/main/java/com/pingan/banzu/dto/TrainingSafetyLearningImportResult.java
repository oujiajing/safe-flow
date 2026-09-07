package com.pingan.banzu.dto;

import java.util.List;

public record TrainingSafetyLearningImportResult(int successRows, List<String> errors) {}
