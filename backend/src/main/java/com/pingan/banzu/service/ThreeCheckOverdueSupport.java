package com.pingan.banzu.service;

import com.pingan.banzu.common.ThreeCheckStatus;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

public final class ThreeCheckOverdueSupport {

  public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
  private static final Set<String> OVERDUE_STATUSES =
      Set.of(ThreeCheckStatus.DRAFT, ThreeCheckStatus.WITHDRAWN);

  private ThreeCheckOverdueSupport() {}

  public static LocalDate today() {
    return LocalDate.now(BUSINESS_ZONE);
  }

  public static boolean isOverdue(LocalDate businessDate, String status) {
    return isOverdue(businessDate, status, today());
  }

  static boolean isOverdue(LocalDate businessDate, String status, LocalDate today) {
    return isPastBusinessDate(businessDate, today) && OVERDUE_STATUSES.contains(status);
  }

  public static boolean isPastBusinessDate(LocalDate businessDate) {
    return isPastBusinessDate(businessDate, today());
  }

  static boolean isPastBusinessDate(LocalDate businessDate, LocalDate today) {
    return businessDate != null && today != null && businessDate.isBefore(today);
  }
}
