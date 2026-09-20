package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.config.SafeGuardAgentProperties;
import com.pingan.banzu.domain.HazardAgentRectificationApply;
import com.pingan.banzu.domain.HazardRectificationOrder;
import com.pingan.banzu.domain.ThreeCheckRecord;
import com.pingan.banzu.dto.QuickShotAgentConfirmation;
import com.pingan.banzu.dto.QuickShotAgentRectificationPreviewResponse;
import com.pingan.banzu.mapper.HazardAgentRectificationApplyMapper;
import com.pingan.banzu.mapper.HazardRectificationOrderMapper;
import com.pingan.banzu.security.CurrentUser;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;

/** Guarded audit/idemponent layer for the existing quick-shot APPROVE transaction. */
@Service
public class QuickShotAgentRectificationApplyService {
  private final SafeGuardAgentProperties properties;
  private final HazardPermissionPolicy permissionPolicy;
  private final QuickShotAgentRectificationPreviewService previewService;
  private final HazardAgentRectificationApplyMapper applyMapper;
  private final HazardRectificationOrderMapper orderMapper;
  private final ObjectMapper mapper;

  public QuickShotAgentRectificationApplyService(SafeGuardAgentProperties properties, HazardPermissionPolicy permissionPolicy,
      @Lazy QuickShotAgentRectificationPreviewService previewService, HazardAgentRectificationApplyMapper applyMapper,
      HazardRectificationOrderMapper orderMapper, ObjectMapper mapper) {
    this.properties = properties; this.permissionPolicy = permissionPolicy; this.previewService = previewService;
    this.applyMapper = applyMapper; this.orderMapper = orderMapper; this.mapper = mapper;
  }

  public ApplyContext begin(ThreeCheckRecord record, QuickShotAgentConfirmation confirmation, String idempotencyKey, CurrentUser actor) {
    if (!properties.rectificationApplyEnabled()) throw new BusinessException("Agent 整改应用未启用");
    if (confirmation == null || !Boolean.TRUE.equals(confirmation.confirmation())) throw new BusinessException("必须明确确认后才能生成整改草稿");
    if (idempotencyKey == null || idempotencyKey.isBlank()) throw new BusinessException("缺少 Idempotency-Key");
    permissionPolicy.assertCanReviewQuickShot(); permissionPolicy.assertCanCreateOrder();
    QuickShotAgentRectificationPreviewResponse preview = previewService.preview(record.id, confirmation.runId());
    if (!safe(confirmation.assessmentId()).equals(safe(preview.assessmentId())) || confirmation.decisionSetVersion() == null
        || confirmation.decisionSetVersion() != preview.decisionSetVersion() || !safe(confirmation.previewDigest()).equals(safe(preview.previewDigest()))) {
      throw new BusinessException("AI 整改预览已失效，请重新获取预览");
    }
    if (!preview.missingFields().isEmpty()) throw new BusinessException("整改预览缺少必填字段：" + String.join("、", preview.missingFields()));
    HazardAgentRectificationApply existing = applyMapper.selectOne(new QueryWrapper<HazardAgentRectificationApply>().eq("run_id", confirmation.runId()).last("limit 1"));
    if (existing != null) {
      if (!safe(existing.idempotencyKey).equals(idempotencyKey)) throw new BusinessException("该 Agent 草稿已在其他请求中应用");
      return new ApplyContext(existing, preview, true);
    }
    HazardAgentRectificationApply value = new HazardAgentRectificationApply(); value.runId = confirmation.runId(); value.assessmentId = confirmation.assessmentId();
    value.sourceModuleKey = "quick-shot"; value.sourceRecordId = record.id; value.sourceRecordVersion = record.version;
    value.decisionSetVersion = confirmation.decisionSetVersion(); value.previewDigest = confirmation.previewDigest(); value.idempotencyKey = idempotencyKey.trim();
    value.status = preview.existingOrderId() == null ? "APPLYING" : "REUSED_EXISTING";
    value.orderId = preview.existingOrderId() == null ? null : Long.valueOf(preview.existingOrderId());
    value.actorUserId = actor.userId(); value.createdAt = LocalDateTime.now(); value.updatedAt = value.createdAt;
    value.appliedAt = preview.existingOrderId() == null ? null : value.createdAt;
    try { value.snapshotJson = mapper.writeValueAsString(preview); } catch (Exception e) { throw new BusinessException("无法保存整改应用快照"); }
    applyMapper.insert(value); return new ApplyContext(value, preview, preview.existingOrderId() != null);
  }

  public void complete(ApplyContext context, Long recordId) {
    if (context.reused) return;
    HazardRectificationOrder order = orderMapper.selectOne(new QueryWrapper<HazardRectificationOrder>().eq("source_type", "QUICK_SHOT").eq("source_record_id", recordId).eq("deleted", 0).last("limit 1"));
    if (order == null) throw new BusinessException("整改草稿未创建");
    context.apply.orderId = order.id; context.apply.status = "APPLIED"; context.apply.appliedAt = LocalDateTime.now(); context.apply.updatedAt = context.apply.appliedAt; applyMapper.updateById(context.apply);
  }
  public HazardAgentRectificationApply find(Long recordId, String runId, String idempotencyKey) {
    HazardAgentRectificationApply value = applyMapper.selectOne(new QueryWrapper<HazardAgentRectificationApply>()
        .eq("source_record_id", recordId).eq("run_id", runId).last("limit 1"));
    if (value != null && idempotencyKey != null && !idempotencyKey.isBlank() && !safe(value.idempotencyKey).equals(idempotencyKey.trim())) {
      throw new BusinessException("Idempotency-Key 与已有应用记录不一致");
    }
    return value;
  }
  private String safe(String value) { return value == null ? "" : value; }
  public record ApplyContext(
      HazardAgentRectificationApply apply,
      QuickShotAgentRectificationPreviewResponse preview,
      boolean reused) {}
}
