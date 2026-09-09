package com.pingan.banzu.common;

import java.util.Set;

public final class HazardRectificationOrderStatus {
  public static final String PENDING_ASSIGN = "PENDING_ASSIGN";
  public static final String PENDING_RECTIFY = "PENDING_RECTIFY";
  public static final String RECTIFIED = "RECTIFIED";
  public static final String PENDING_ACCEPTANCE = "PENDING_ACCEPTANCE";
  public static final String CLOSED = "CLOSED";
  public static final String CANCELLED = "CANCELLED";

  public static final Set<String> ALL =
      Set.of(PENDING_ASSIGN, PENDING_RECTIFY, RECTIFIED, PENDING_ACCEPTANCE, CLOSED, CANCELLED);

  private HazardRectificationOrderStatus() {}

  public static String label(String status) {
    return switch (status) {
      case PENDING_ASSIGN -> "待派发";
      case PENDING_RECTIFY -> "待整改";
      case RECTIFIED -> "已整改";
      case PENDING_ACCEPTANCE -> "待验收";
      case CLOSED -> "已关闭";
      case CANCELLED -> "已作废";
      default -> status;
    };
  }
}
