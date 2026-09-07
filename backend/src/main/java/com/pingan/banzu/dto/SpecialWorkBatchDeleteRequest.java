package com.pingan.banzu.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SpecialWorkBatchDeleteRequest(@NotEmpty(message = "请选择要删除的记录") List<Long> ids) {}
