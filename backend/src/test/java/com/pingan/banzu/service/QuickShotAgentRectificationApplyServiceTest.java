package com.pingan.banzu.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.config.SafeGuardAgentProperties;
import com.pingan.banzu.domain.HazardAgentRectificationApply;
import com.pingan.banzu.domain.ThreeCheckRecord;
import com.pingan.banzu.dto.QuickShotAgentConfirmation;
import com.pingan.banzu.dto.QuickShotAgentRectificationPreviewResponse;
import com.pingan.banzu.mapper.HazardAgentRectificationApplyMapper;
import com.pingan.banzu.mapper.HazardRectificationOrderMapper;
import com.pingan.banzu.security.CurrentUser;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuickShotAgentRectificationApplyServiceTest {
  @Test
  void reusesSameKeyAndRejectsDifferentKey() {
    HazardAgentRectificationApplyMapper applies = mock(HazardAgentRectificationApplyMapper.class);
    HazardAgentRectificationApply existing = new HazardAgentRectificationApply();
    existing.runId = "run-1"; existing.idempotencyKey = "key-1"; existing.status = "APPLIED";
    when(applies.selectOne(any())).thenReturn(existing);
    QuickShotAgentRectificationApplyService service = service(applies, preview("digest-1", 2));
    ThreeCheckRecord record = record();
    var confirmation = new QuickShotAgentConfirmation("run-1", "assessment-1", 2, "digest-1", true);
    var result = service.begin(record, confirmation, "key-1", user());
    org.junit.jupiter.api.Assertions.assertTrue(result.reused());
    assertThrows(BusinessException.class, () -> service.begin(record, confirmation, "key-2", user()));
  }

  @Test
  void rejectsChangedPreviewDigestBeforeWriting() {
    HazardAgentRectificationApplyMapper applies = mock(HazardAgentRectificationApplyMapper.class);
    when(applies.selectOne(any())).thenReturn(null);
    QuickShotAgentRectificationApplyService service = service(applies, preview("digest-1", 2));
    assertThrows(BusinessException.class, () -> service.begin(record(),
        new QuickShotAgentConfirmation("run-1", "assessment-1", 2, "stale", true), "key", user()));
  }

  @Test
  void reusesExistingSourceOrderWithoutReplayingApprove() {
    HazardAgentRectificationApplyMapper applies = mock(HazardAgentRectificationApplyMapper.class);
    when(applies.selectOne(any())).thenReturn(null);
    QuickShotAgentRectificationPreviewResponse preview = new QuickShotAgentRectificationPreviewResponse(
        "run-1", "assessment-1", "9", 3, 2, "digest-1", List.of(), List.of(), "88", false);
    var result = service(applies, preview).begin(record(),
        new QuickShotAgentConfirmation("run-1", "assessment-1", 2, "digest-1", true), "key", user());
    org.junit.jupiter.api.Assertions.assertTrue(result.reused());
    org.junit.jupiter.api.Assertions.assertEquals("REUSED_EXISTING", result.apply().status);
    org.junit.jupiter.api.Assertions.assertEquals(88L, result.apply().orderId);
  }

  private QuickShotAgentRectificationApplyService service(HazardAgentRectificationApplyMapper applies,
      QuickShotAgentRectificationPreviewResponse preview) {
    SafeGuardAgentProperties properties = new SafeGuardAgentProperties(true, "http://x", "token", 1, 1, true, true, "", false, true, true);
    HazardPermissionPolicy permissions = mock(HazardPermissionPolicy.class);
    doNothing().when(permissions).assertCanReviewQuickShot(); doNothing().when(permissions).assertCanCreateOrder();
    QuickShotAgentRectificationPreviewService previews = mock(QuickShotAgentRectificationPreviewService.class);
    when(previews.preview(9L, "run-1")).thenReturn(preview);
    return new QuickShotAgentRectificationApplyService(properties, permissions, previews, applies,
        mock(HazardRectificationOrderMapper.class), new com.fasterxml.jackson.databind.ObjectMapper());
  }

  private QuickShotAgentRectificationPreviewResponse preview(String digest, int version) {
    return new QuickShotAgentRectificationPreviewResponse("run-1", "assessment-1", "9", 3, version, digest,
        List.of(), List.of(), null, true);
  }
  private ThreeCheckRecord record() { ThreeCheckRecord value = new ThreeCheckRecord(); value.id = 9L; value.version = 3; return value; }
  private CurrentUser user() { return new CurrentUser(1L, "u", "u", null, null, List.of()); }
}
