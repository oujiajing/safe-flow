package com.pingan.banzu.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record NotificationQuery(
    String groupType,
    String moduleKey,
    String category,
    Boolean unread,
    String actionStatus,
    String handlingStatus,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateStart,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEnd,
    Integer page,
    Integer pageSize) {}
