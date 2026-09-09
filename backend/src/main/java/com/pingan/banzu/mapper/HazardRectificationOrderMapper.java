package com.pingan.banzu.mapper;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pingan.banzu.domain.HazardRectificationOrder;
import java.time.LocalDateTime;

public interface HazardRectificationOrderMapper extends BaseMapper<HazardRectificationOrder> {

  default int updateByIdAndVersionAndStatus(
      HazardRectificationOrder order, int expectedVersion, String expectedStatus) {
    return update(
        order,
        new UpdateWrapper<HazardRectificationOrder>()
            .eq("id", order.id)
            .eq("version", expectedVersion)
            .eq("status", expectedStatus)
            .eq("deleted", 0));
  }

  default int softDeleteByIdAndVersionAndStatus(
      Long id,
      int expectedVersion,
      String expectedStatus,
      Long updatedBy,
      LocalDateTime updatedAt) {
    return update(
        null,
        new UpdateWrapper<HazardRectificationOrder>()
            .eq("id", id)
            .eq("version", expectedVersion)
            .eq("status", expectedStatus)
            .eq("deleted", 0)
            .set("deleted", 1)
            .set("version", expectedVersion + 1)
            .set("updated_by", updatedBy)
            .set("updated_at", updatedAt));
  }
}
