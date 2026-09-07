package com.pingan.banzu.dto;

import java.time.LocalDate;

public record SafetyLedgerDocumentQuery(
    String ledgerKey,
    Long companyId,
    String keyword,
    LocalDate dateStart,
    LocalDate dateEnd,
    LocalDate uploadedStart,
    LocalDate uploadedEnd,
    Integer page,
    Integer pageSize) {}
