package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.config.SafeGuardAgentProperties;
import com.pingan.banzu.domain.HazardAgentDecision;
import com.pingan.banzu.domain.HazardAgentRun;
import com.pingan.banzu.domain.HazardRectificationOrder;
import com.pingan.banzu.dto.QuickShotAgentRectificationPreviewResponse;
import com.pingan.banzu.dto.ThreeCheckRecordDetailResponse;
import com.pingan.banzu.mapper.HazardAgentDecisionMapper;
import com.pingan.banzu.mapper.HazardAgentRunMapper;
import com.pingan.banzu.mapper.HazardRectificationOrderMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Produces a deterministic, read-only order projection from a persisted review draft.
 * Business creation remains exclusively in the quick-shot APPROVE transaction.
 */
@Service
public class QuickShotAgentRectificationPreviewService {
  private static final String MODULE_KEY = "quick-shot";
  private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

  private final ThreeCheckRecordService recordService;
  private final HazardAgentRunMapper runMapper;
  private final HazardAgentDecisionMapper decisionMapper;
  private final HazardRectificationOrderMapper orderMapper;
  private final ObjectMapper mapper;
  private final SafeGuardAgentProperties properties;
  private final QuickShotAgentRunService runService;

  public QuickShotAgentRectificationPreviewService(
      ThreeCheckRecordService recordService,
      HazardAgentRunMapper runMapper,
      HazardAgentDecisionMapper decisionMapper,
      HazardRectificationOrderMapper orderMapper,
      ObjectMapper mapper,
      SafeGuardAgentProperties properties,
      QuickShotAgentRunService runService) {
    this.recordService = recordService;
    this.runMapper = runMapper;
    this.decisionMapper = decisionMapper;
    this.orderMapper = orderMapper;
    this.mapper = mapper;
    this.properties = properties;
    this.runService = runService;
  }

  public QuickShotAgentRectificationPreviewResponse preview(Long recordId, String runId) {
    if (!properties.rectificationPreviewEnabled()) {
      throw new BusinessException("Agent 整改预览未启用");
    }
    ThreeCheckRecordDetailResponse record = recordService.detail(MODULE_KEY, recordId);
    HazardAgentRun run = requireRun(recordId, runId);
    runService.assertCurrent(recordId, runId);
    if (!"REVIEW_DRAFTED".equals(run.status)) {
      throw new BusinessException("Agent 审核草稿尚未保存，不能生成整改预览");
    }
    if (record.version() != run.sourceRecordVersion) {
      throw new BusinessException("来源记录已更新，请重新运行 AI 研判");
    }
    List<HazardAgentDecision> decisions = latestDecisions(run.runId).stream()
        .filter(value -> !"REJECTED".equals(value.decision))
        .toList();
    if (decisions.isEmpty()) {
      throw new BusinessException("请至少保留一条人工确认的隐患候选");
    }
    Map<String, JsonNode> assessments = assessments(run.assessmentJson);
    List<QuickShotAgentRectificationPreviewResponse.Item> items = new ArrayList<>();
    List<String> missing = new ArrayList<>();
    int decisionVersion = 0;
    for (HazardAgentDecision decision : decisions) {
      decisionVersion = Math.max(decisionVersion, decision.decisionVersion == null ? 0 : decision.decisionVersion);
      JsonNode assessment = assessments.get(decision.candidateId);
      String type = first(decision.editedHazardType, modelField(decision, "hazardType"));
      String description = first(decision.editedDescription, modelField(decision, "description"));
      String risk = first(decision.editedRiskLevel, assessment == null ? null : assessment.path("riskLevel").path("value").asText(null));
      List<String> measures = strings(decision.editedMeasuresJson);
      if (measures.isEmpty() && assessment != null) measures = strings(assessment.path("rectificationMeasures"));
      List<String> acceptance = assessment == null ? List.of() : strings(assessment.path("acceptanceCriteria"));
      if (blank(type)) missing.add(decision.candidateId + ":隐患类型");
      if (blank(description)) missing.add(decision.candidateId + ":隐患描述");
      if (blank(risk)) missing.add(decision.candidateId + ":风险等级");
      if (measures.isEmpty()) missing.add(decision.candidateId + ":整改措施");
      items.add(new QuickShotAgentRectificationPreviewResponse.Item(
          decision.candidateId, strings(modelNode(decision).path("sourceAttachmentIds")), type, description,
          risk, measures, acceptance, assessment == null ? List.of() : assessment.path("legalEvidence")));
    }
    String existingOrderId = existingOrderId(recordId);
    String digest = digest(recordId, run, decisionVersion, items);
    return new QuickShotAgentRectificationPreviewResponse(
        run.runId, run.assessmentId, String.valueOf(recordId), record.version(), decisionVersion, digest,
        List.copyOf(items), List.copyOf(missing), existingOrderId,
        missing.isEmpty());
  }

  private HazardAgentRun requireRun(Long recordId, String runId) {
    HazardAgentRun run = runMapper.selectOne(new QueryWrapper<HazardAgentRun>()
        .eq("run_id", runId).eq("source_module_key", MODULE_KEY).eq("source_record_id", recordId).eq("deleted", 0));
    if (run == null) throw new BusinessException("Agent 研判记录不存在");
    return run;
  }

  private List<HazardAgentDecision> latestDecisions(String runId) {
    return decisionMapper.selectList(new QueryWrapper<HazardAgentDecision>()
            .eq("run_id", runId).orderByAsc("candidate_id").orderByDesc("decision_version"))
        .stream().collect(Collectors.toMap(value -> value.candidateId, value -> value,
            (first, ignored) -> first, LinkedHashMap::new)).values().stream()
        .sorted(Comparator.comparing(value -> value.candidateId)).toList();
  }

  private Map<String, JsonNode> assessments(String json) {
    Map<String, JsonNode> result = new LinkedHashMap<>();
    try {
      for (JsonNode item : mapper.readTree(json == null ? "{}" : json).path("assessments")) {
        result.put(item.path("candidateId").asText(), item.path("assessment"));
      }
    } catch (Exception ignored) {
      // Invalid assessment snapshots are handled as missing business fields, never trusted.
    }
    return result;
  }

  private String existingOrderId(Long recordId) {
    HazardRectificationOrder order = orderMapper.selectOne(new QueryWrapper<HazardRectificationOrder>()
        .eq("source_type", "QUICK_SHOT").eq("source_record_id", recordId).eq("deleted", 0)
        .ne("status", "CANCELLED").last("limit 1"));
    return order == null ? null : String.valueOf(order.id);
  }

  private String digest(Long recordId, HazardAgentRun run, int version,
      List<QuickShotAgentRectificationPreviewResponse.Item> items) {
    try {
      String value = mapper.writeValueAsString(Map.of("recordId", recordId, "recordVersion", run.sourceRecordVersion,
          "runId", run.runId, "assessmentId", Objects.toString(run.assessmentId, ""), "decisionSetVersion", version,
          "items", items));
      return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new BusinessException("无法生成整改预览摘要");
    }
  }

  private JsonNode modelNode(HazardAgentDecision decision) {
    try { return mapper.readTree(decision.modelOutputJson == null ? "{}" : decision.modelOutputJson); }
    catch (Exception ignored) { return mapper.createObjectNode(); }
  }

  private String modelField(HazardAgentDecision decision, String field) { return modelNode(decision).path(field).asText(null); }
  private List<String> strings(String json) { try { return strings(mapper.readTree(json == null ? "[]" : json)); } catch (Exception ignored) { return List.of(); } }
  private List<String> strings(JsonNode node) { if (!node.isArray()) return List.of(); List<String> result = new ArrayList<>(); node.forEach(value -> { String text = value.asText("").trim(); if (!text.isEmpty()) result.add(text); }); return List.copyOf(result); }
  private String first(String preferred, String fallback) { return blank(preferred) ? fallback : preferred.trim(); }
  private boolean blank(String value) { return value == null || value.isBlank(); }
}
