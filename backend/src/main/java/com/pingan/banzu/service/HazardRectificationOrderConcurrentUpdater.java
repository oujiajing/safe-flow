package com.pingan.banzu.service;

import com.pingan.banzu.common.ConflictException;
import com.pingan.banzu.domain.HazardRectificationOrder;
import com.pingan.banzu.mapper.HazardRectificationOrderMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
class HazardRectificationOrderConcurrentUpdater {

  private static final String CONFLICT_MESSAGE = "工单已被其他端更新，请刷新后重试";

  private final HazardRectificationOrderMapper orderMapper;

  HazardRectificationOrderConcurrentUpdater(HazardRectificationOrderMapper orderMapper) {
    this.orderMapper = orderMapper;
  }

  void update(HazardRectificationOrder order, String expectedStatus) {
    int expectedVersion = currentVersion(order);
    requireExpectedStatus(order, expectedStatus);
    order.version = expectedVersion + 1;
    order.updatedAt = LocalDateTime.now();
    if (orderMapper.updateByIdAndVersionAndStatus(order, expectedVersion, expectedStatus) != 1) {
      throw new ConflictException(CONFLICT_MESSAGE);
    }
  }

  void softDelete(HazardRectificationOrder order, Long updatedBy) {
    int expectedVersion = currentVersion(order);
    String expectedStatus = order.status;
    LocalDateTime updatedAt = LocalDateTime.now();
    if (orderMapper.softDeleteByIdAndVersionAndStatus(
            order.id, expectedVersion, expectedStatus, updatedBy, updatedAt)
        != 1) {
      throw new ConflictException(CONFLICT_MESSAGE);
    }
    order.deleted = 1;
    order.version = expectedVersion + 1;
    order.updatedBy = updatedBy;
    order.updatedAt = updatedAt;
  }

  private int currentVersion(HazardRectificationOrder order) {
    if (order == null || order.id == null || order.version == null) {
      throw new ConflictException(CONFLICT_MESSAGE);
    }
    return order.version;
  }

  private void requireExpectedStatus(HazardRectificationOrder order, String expectedStatus) {
    if (expectedStatus == null || order.status == null) {
      throw new ConflictException(CONFLICT_MESSAGE);
    }
  }
}
