package com.pingan.banzu.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record MonitorCenterQuery(
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateStart,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEnd,
    Long orgId) {}
