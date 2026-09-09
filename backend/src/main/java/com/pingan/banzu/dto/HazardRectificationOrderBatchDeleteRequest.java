package com.pingan.banzu.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record HazardRectificationOrderBatchDeleteRequest(
    @NotEmpty(message = "请选择要删除的隐患整改工单") List<Long> ids) {}
