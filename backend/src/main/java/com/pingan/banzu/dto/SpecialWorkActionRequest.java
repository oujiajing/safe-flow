package com.pingan.banzu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record SpecialWorkActionRequest(
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime implementationStartTime,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime implementationEndTime,
    String safetyDisclosurePerson,
    String guardian,
    String disclosureReceiver,
    String completionAcceptor,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime completionAcceptanceTime) {}
