package com.pingan.banzu.dto;

import java.time.LocalDate;

public record MineRecordListItem(
    Long id,
    String type,
    String moduleKey,
    Long targetId,
    Long relatedId,
    String title,
    String subtitle,
    String status,
    String statusLabel,
    LocalDate businessDate,
    Integer pointsDelta) {}
