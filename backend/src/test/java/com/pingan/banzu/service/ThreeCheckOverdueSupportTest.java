package com.pingan.banzu.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pingan.banzu.common.ThreeCheckStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ThreeCheckOverdueSupportTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 7, 24);

  @Test
  void onlyPastDraftAndWithdrawnRecordsAreOverdue() {
    assertThat(
            ThreeCheckOverdueSupport.isOverdue(
                TODAY.minusDays(1), ThreeCheckStatus.DRAFT, TODAY))
        .isTrue();
    assertThat(
            ThreeCheckOverdueSupport.isOverdue(
                TODAY.minusDays(1), ThreeCheckStatus.WITHDRAWN, TODAY))
        .isTrue();
    assertThat(ThreeCheckOverdueSupport.isOverdue(TODAY, ThreeCheckStatus.DRAFT, TODAY))
        .isFalse();
    assertThat(
            ThreeCheckOverdueSupport.isOverdue(
                TODAY.minusDays(1), ThreeCheckStatus.OPENED, TODAY))
        .isFalse();
    assertThat(
            ThreeCheckOverdueSupport.isOverdue(
                TODAY.minusDays(1), ThreeCheckStatus.ARCHIVED, TODAY))
        .isFalse();
  }

  @Test
  void identifiesPastBusinessDatesIndependentlyFromWorkflowStatus() {
    assertThat(ThreeCheckOverdueSupport.isPastBusinessDate(TODAY.minusDays(1), TODAY)).isTrue();
    assertThat(ThreeCheckOverdueSupport.isPastBusinessDate(TODAY, TODAY)).isFalse();
    assertThat(ThreeCheckOverdueSupport.isPastBusinessDate(TODAY.plusDays(1), TODAY)).isFalse();
  }
}
