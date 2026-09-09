package com.pingan.banzu.dto;

import java.util.List;

public record OrgNodeResponse(
    String key,
    Long id,
    String title,
    String orgType,
    String companyType,
    List<OrgNodeResponse> children) {}
