package com.pingan.banzu.dto;

import java.util.List;

public record TrainingExamPdfPreviewResponse(
    String previewId,
    String filename,
    int pageCount,
    int characterCount,
    List<TrainingExamPdfQuestionDraft> questions,
    List<String> warnings,
    List<String> errors) {}
