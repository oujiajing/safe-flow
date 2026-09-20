package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.ThreeCheckBizType;
import com.pingan.banzu.config.SafeGuardAgentProperties;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.domain.HazardAgentDecision;
import com.pingan.banzu.domain.HazardAgentRun;
import com.pingan.banzu.domain.HazardAgentRunAttachment;
import com.pingan.banzu.dto.QuickShotAgentCandidateDecisionRequest;
import com.pingan.banzu.dto.QuickShotAgentReviewDraftRequest;
import com.pingan.banzu.dto.QuickShotAgentRunRequest;
import com.pingan.banzu.dto.QuickShotAgentRunResponse;
import com.pingan.banzu.dto.ThreeCheckRecordDetailResponse;
import com.pingan.banzu.mapper.BizAttachmentMapper;
import com.pingan.banzu.mapper.HazardAgentDecisionMapper;
import com.pingan.banzu.mapper.HazardAgentRunAttachmentMapper;
import com.pingan.banzu.mapper.HazardAgentRunMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Durable, read-only orchestration for the quick-shot Agent pilot. */
@Service
public class QuickShotAgentRunService {
  private static final String MODULE_KEY = "quick-shot";
  private static final String WORKFLOW_VERSION = "quick-shot-agent-v1";
  private static final Set<String> DECISIONS = Set.of("ACCEPTED", "REJECTED", "EDITED");
  private final ThreeCheckRecordService recordService;
  private final HazardPermissionPolicy permissionPolicy;
  private final BizAttachmentMapper attachmentMapper;
  private final HazardAgentRunMapper runMapper;
  private final HazardAgentRunAttachmentMapper runAttachmentMapper;
  private final HazardAgentDecisionMapper decisionMapper;
  private final AttachmentContentReader contentReader;
  private final SafeGuardAgentProperties properties;
  private final ObjectMapper mapper;
  private final AuditLogService auditLogService;
  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  public QuickShotAgentRunService(
      ThreeCheckRecordService recordService, HazardPermissionPolicy permissionPolicy,
      BizAttachmentMapper attachmentMapper, HazardAgentRunMapper runMapper,
      HazardAgentRunAttachmentMapper runAttachmentMapper, HazardAgentDecisionMapper decisionMapper,
      AttachmentContentReader contentReader, SafeGuardAgentProperties properties, ObjectMapper mapper,
      AuditLogService auditLogService) {
    this.recordService = recordService;
    this.permissionPolicy = permissionPolicy;
    this.attachmentMapper = attachmentMapper;
    this.runMapper = runMapper;
    this.runAttachmentMapper = runAttachmentMapper;
    this.decisionMapper = decisionMapper;
    this.contentReader = contentReader;
    this.properties = properties;
    this.mapper = mapper;
    this.auditLogService = auditLogService;
  }

  public QuickShotAgentRunResponse create(Long recordId, QuickShotAgentRunRequest request) {
    requireEnabled();
    permissionPolicy.assertCanStartQuickShotAgentRun();
    CurrentUser actor = CurrentUserContext.require();
    ThreeCheckRecordDetailResponse record = recordService.detail(MODULE_KEY, recordId);
    if (!properties.isCompanyAllowed(record.companyId())) throw new BusinessException("当前企业未加入 Agent 试点灰度");
    List<BizAttachment> images = images(recordId);
    if (images.isEmpty()) throw new BusinessException("随手拍记录没有可分析的图片");
    String input = inputText(record, request == null ? null : request.extraDescription());
    List<ImageInput> inputs = snapshotImages(images);
    String inputHash = sha256(input);
    String attachmentHashSet = sha256(inputs.stream().map(ImageInput::sha256).sorted().collect(Collectors.joining(",")));
    HazardAgentRun run = newRun(record, actor, input, inputHash, attachmentHashSet);
    runMapper.insert(run);
    inputs.forEach(inputImage -> insertAttachment(run.runId, inputImage));
    try {
      JsonNode visual = invokeHostedVisual(record, actor, input, inputs, run.traceId);
      ArrayNode candidates = visual.path("candidates").isArray()
          ? (ArrayNode) visual.path("candidates") : mapper.createArrayNode();
      for (ImageInput image : inputs) markAttachmentAnalyzed(run.runId, image.attachment.id);
      ObjectNode output = mapper.createObjectNode();
      output.set("hazardCandidates", candidates);
      output.put("partial", false);
      run.analysisId = visual.path("analysisId").asText(visual.path("runId").asText());
      run.model = visual.path("model").asText("");
      run.modelOutputJson = mapper.writeValueAsString(output);
      run.status = candidates.isEmpty() ? "NO_CANDIDATE" : "CANDIDATES_READY";
      run.completedAt = LocalDateTime.now();
      run.updatedAt = run.completedAt;
      runMapper.updateById(run);
      audit(run, "CREATE_RUN", "创建随手拍 AI 视觉研判");
      return response(run);
    } catch (Exception exception) {
      fail(run, "VISUAL_ANALYSIS_FAILED", safeMessage(exception));
      throw exception instanceof BusinessException business ? business : new BusinessException("Agent 视觉研判请求失败：" + safeMessage(exception));
    }
  }

  public QuickShotAgentRunResponse detail(Long recordId, String runId) {
    recordService.detail(MODULE_KEY, recordId);
    HazardAgentRun run = requireRun(recordId, runId);
    return response(run);
  }

  /** Returns the most recent durable run for restoring the detail workbench after navigation. */
  public QuickShotAgentRunResponse latest(Long recordId) {
    recordService.detail(MODULE_KEY, recordId);
    HazardAgentRun run = runMapper.selectOne(new QueryWrapper<HazardAgentRun>()
        .eq("source_module_key", MODULE_KEY).eq("source_record_id", recordId).eq("deleted", 0)
        .orderByDesc("created_at").last("limit 1"));
    if (run == null) return null;
    if (isStale(run, recordId)) markStale(run);
    return response(run);
  }

  @Transactional
  public QuickShotAgentRunResponse decide(Long recordId, String runId, QuickShotAgentCandidateDecisionRequest request) {
    permissionPolicy.assertCanReviewQuickShot();
    recordService.detail(MODULE_KEY, recordId);
    HazardAgentRun run = requireRun(recordId, runId);
    if (isStale(run, recordId)) markStale(run);
    if ("STALE".equals(run.status)) throw new BusinessException("记录或图片已变更，旧研判结果不可编辑");
    if (request == null || request.decisions() == null || request.decisions().isEmpty()) throw new BusinessException("请至少提交一条候选决定");
    Map<String, JsonNode> candidates = candidates(run);
    CurrentUser actor = CurrentUserContext.require();
    for (QuickShotAgentCandidateDecisionRequest.Item item : request.decisions()) {
      if (item == null || item.candidateId() == null || !candidates.containsKey(item.candidateId())) throw new BusinessException("候选不存在");
      saveDecision(run.runId, item.candidateId(), normalizeDecision(item.decision()), candidates.get(item.candidateId()),
          item.editedHazardType(), item.editedDescription(), null, null, item.reviewerNote(), actor.userId());
    }
    run.status = "CANDIDATES_CONFIRMED";
    run.updatedAt = LocalDateTime.now();
    runMapper.updateById(run);
    audit(run, "CANDIDATE_DECISION", "保存 AI 候选人工决定");
    return response(run);
  }

  public QuickShotAgentRunResponse assess(Long recordId, String runId) {
    requireEnabled();
    if (!properties.assessmentEnabled()) throw new BusinessException("Agent 法规评估试点未启用");
    permissionPolicy.assertCanReviewQuickShot();
    recordService.detail(MODULE_KEY, recordId);
    HazardAgentRun run = requireRun(recordId, runId);
    if (isStale(run, recordId)) markStale(run);
    if ("STALE".equals(run.status)) throw new BusinessException("记录或图片已变更，旧研判结果不可用于评估");
    List<HazardAgentDecision> accepted = latestDecisions(run.runId).stream().filter(d -> !"REJECTED".equals(d.decision)).toList();
    if (accepted.isEmpty()) throw new BusinessException("请先采纳或编辑至少一个候选");
    run.status = "RETRIEVING_EVIDENCE";
    run.updatedAt = LocalDateTime.now();
    runMapper.updateById(run);
    try {
      JsonNode result = invokeHostedAssessment(run, accepted);
      ArrayNode assessments = mapper.createArrayNode();
      for (JsonNode item : result.path("assessments")) {
        ObjectNode presentation = mapper.createObjectNode();
        presentation.put("candidateId", item.path("candidateId").asText());
        presentation.set("assessment", item);
        assessments.add(presentation);
      }
      ObjectNode aggregate = mapper.createObjectNode();
      aggregate.set("assessments", assessments);
      boolean knowledgeUsed = false;
      for (JsonNode node : assessments) {
        if (node.path("assessment").path("legalEvidence").isArray()
            && !node.path("assessment").path("legalEvidence").isEmpty()) {
          knowledgeUsed = true;
          break;
        }
      }
      aggregate.put("knowledgeUsed", knowledgeUsed);
      run.assessmentId = result.path("assessmentId").asText("");
      run.assessmentJson = mapper.writeValueAsString(aggregate);
      run.knowledgeStatus = knowledgeUsed ? "USED" : "NO_RELEVANT_EVIDENCE";
      run.status = knowledgeUsed ? "ASSESSED" : "NO_RELEVANT_EVIDENCE";
      run.completedAt = LocalDateTime.now();
      run.updatedAt = run.completedAt;
      runMapper.updateById(run);
      audit(run, "ASSESS", "生成只读法规证据评估");
      return response(run);
    } catch (Exception exception) {
      fail(run, "ASSESSMENT_FAILED", safeMessage(exception));
      throw exception instanceof BusinessException business ? business : new BusinessException("Agent 法规评估请求失败：" + safeMessage(exception));
    }
  }

  @Transactional
  public QuickShotAgentRunResponse saveReviewDraft(Long recordId, String runId, QuickShotAgentReviewDraftRequest request) {
    if (!properties.reviewDraftEnabled()) throw new BusinessException("Agent 审核草稿试点未启用");
    permissionPolicy.assertCanReviewQuickShot();
    recordService.detail(MODULE_KEY, recordId);
    HazardAgentRun run = requireRun(recordId, runId);
    if (isStale(run, recordId)) markStale(run);
    if ("STALE".equals(run.status)) throw new BusinessException("旧研判结果不可保存审核草稿");
    CurrentUser actor = CurrentUserContext.require();
    Map<String, JsonNode> candidates = candidates(run);
    if (request != null && request.items() != null) for (QuickShotAgentReviewDraftRequest.Item item : request.items()) {
      if (item == null || item.candidateId() == null || !candidates.containsKey(item.candidateId())) throw new BusinessException("候选不存在");
      saveDecision(run.runId, item.candidateId(), normalizeDecision(item.decision()), candidates.get(item.candidateId()),
          item.editedHazardType(), item.editedDescription(), item.editedRiskLevel(), write(item.editedMeasures()),
          item.reviewerNote() == null ? request.reviewerNote() : item.reviewerNote(), actor.userId());
    }
    run.status = "REVIEW_DRAFTED";
    run.updatedAt = LocalDateTime.now();
    runMapper.updateById(run);
    audit(run, "REVIEW_DRAFT", "保存 AI 辅助审核草稿；未改变随手拍业务状态");
    return response(run);
  }

  private List<BizAttachment> images(Long recordId) {
    return attachmentMapper.selectList(new QueryWrapper<BizAttachment>().eq("biz_type", ThreeCheckBizType.HAZARD_QUICK_SHOT)
        .eq("biz_id", recordId).eq("file_kind", "IMAGE").eq("deleted", 0).orderByAsc("uploaded_at"));
  }

  private List<ImageInput> snapshotImages(List<BizAttachment> images) {
    List<ImageInput> result = new ArrayList<>();
    int index = 0;
    for (BizAttachment attachment : images) {
      byte[] bytes = contentReader.read(attachment);
      if (bytes.length == 0 || bytes.length > properties.maxImageBytes()) throw new BusinessException("图片大小不符合 Agent 试点限制");
      result.add(new ImageInput(attachment, bytes, sha256(bytes), index++));
    }
    return result;
  }

  private HazardAgentRun newRun(ThreeCheckRecordDetailResponse record, CurrentUser actor, String input, String inputHash, String attachmentHashSet) {
    HazardAgentRun run = new HazardAgentRun();
    run.runId = UUID.randomUUID().toString(); run.sourceModuleKey = MODULE_KEY; run.sourceRecordId = Long.valueOf(record.id());
    run.sourceRecordVersion = record.version(); run.actorUserId = actor.userId(); run.companyId = record.companyId(); run.departmentId = record.departmentId(); run.teamId = record.teamId();
    run.inputText = input; run.inputHash = inputHash; run.attachmentHashSet = attachmentHashSet; run.workflowVersion = WORKFLOW_VERSION;
    run.status = "VISUAL_ANALYZING"; run.traceId = "safe-flow-agent-" + UUID.randomUUID(); run.startedAt = LocalDateTime.now(); run.createdAt = run.startedAt; run.updatedAt = run.startedAt; run.deleted = 0;
    return run;
  }

  private JsonNode invokeHostedVisual(ThreeCheckRecordDetailResponse record, CurrentUser actor, String input, List<ImageInput> inputs, String traceId) throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("sourceRecordId", record.id()); body.put("sourceRecordVersion", record.version()); body.put("userProvidedContext", input);
    body.put("images", inputs.stream().map(image -> Map.of("attachmentId", String.valueOf(image.attachment.id), "imageBase64", Base64.getEncoder().encodeToString(image.bytes))).toList());
    body.put("executionContext", executionContext(record, actor, traceId));
    return post("/agent/v1/hosted/visual-analysis", body);
  }

  private JsonNode invokeHostedAssessment(HazardAgentRun run, List<HazardAgentDecision> decisions) throws Exception {
    Map<String, Object> body = new LinkedHashMap<>(); body.put("visualRunId", run.analysisId);
    body.put("candidates", decisions.stream().map(decision -> Map.of("candidateId", decision.candidateId, "editedHazardType", decision.editedHazardType == null ? "" : decision.editedHazardType, "editedDescription", effectiveDescription(decision))).toList());
    Map<String, Object> context = new LinkedHashMap<>(); context.put("actorUserId", run.actorUserId); context.put("enterpriseId", run.companyId); context.put("projectId", run.departmentId); context.put("teamId", run.teamId); context.put("sourceHazardId", MODULE_KEY + ":" + run.sourceRecordId); context.put("traceId", run.traceId);
    body.put("executionContext", context);
    return post("/agent/v1/hosted/hazard-assessments", body);
  }

  private JsonNode post(String path, Map<String, Object> body) throws Exception {
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(properties.baseUrl() + path)).timeout(Duration.ofMillis(properties.requestTimeoutMs()))
        .header("Accept", "application/json").header("Content-Type", "application/json").header("X-Safeguard-Service-Token", requiredToken())
        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) throw new BusinessException("Agent 服务失败（HTTP " + response.statusCode() + "）");
    JsonNode root = mapper.readTree(response.body());
    if (root.has("code") && !"0".equals(root.path("code").asText())) throw new BusinessException("Agent 服务不可用：" + root.path("message").asText("未知错误"));
    return root.has("data") && root.path("data").isObject() ? root.path("data") : root;
  }

  private Map<String, Object> executionContext(ThreeCheckRecordDetailResponse record, CurrentUser actor, String traceId) {
    Map<String, Object> result = new LinkedHashMap<>(); result.put("actorUserId", actor.userId()); result.put("enterpriseId", record.companyId()); result.put("projectId", record.departmentId()); result.put("teamId", record.teamId()); result.put("sourceHazardId", MODULE_KEY + ":" + record.id()); result.put("traceId", traceId); return result;
  }

  private void insertAttachment(String runId, ImageInput input) { HazardAgentRunAttachment item = new HazardAgentRunAttachment(); item.runId = runId; item.attachmentId = input.attachment.id; item.attachmentSha256 = input.sha256; item.mimeType = input.attachment.contentType; item.fileSize = input.attachment.fileSize; item.imageIndex = input.index; item.createdAt = LocalDateTime.now(); runAttachmentMapper.insert(item); }
  private void markAttachmentAnalyzed(String runId, Long attachmentId) { HazardAgentRunAttachment item = runAttachmentMapper.selectOne(new QueryWrapper<HazardAgentRunAttachment>().eq("run_id", runId).eq("attachment_id", attachmentId)); if (item != null) { item.analyzedAt = LocalDateTime.now(); runAttachmentMapper.updateById(item); } }
  private HazardAgentRun requireRun(Long recordId, String runId) { HazardAgentRun run = runMapper.selectOne(new QueryWrapper<HazardAgentRun>().eq("run_id", runId).eq("source_record_id", recordId).eq("source_module_key", MODULE_KEY).eq("deleted", 0)); if (run == null) throw new BusinessException("Agent 研判记录不存在"); return run; }
  private Map<String, JsonNode> candidates(HazardAgentRun run) { try { Map<String, JsonNode> result = new LinkedHashMap<>(); for (JsonNode item : mapper.readTree(run.modelOutputJson == null ? "{}" : run.modelOutputJson).path("hazardCandidates")) result.put(item.path("candidateId").asText(), item); return result; } catch (Exception exception) { throw new BusinessException("Agent 候选结果损坏"); } }
  private List<HazardAgentDecision> latestDecisions(String runId) { return decisionMapper.selectList(new QueryWrapper<HazardAgentDecision>().eq("run_id", runId).orderByAsc("candidate_id").orderByDesc("decision_version")).stream().collect(Collectors.toMap(d -> d.candidateId, d -> d, (first, ignored) -> first, LinkedHashMap::new)).values().stream().toList(); }
  private void saveDecision(String runId, String candidateId, String decision, JsonNode candidate, String type, String description, String risk, String measures, String note, Long userId) { int version = decisionMapper.selectList(new QueryWrapper<HazardAgentDecision>().eq("run_id", runId).eq("candidate_id", candidateId)).stream().map(d -> d.decisionVersion).filter(java.util.Objects::nonNull).max(Comparator.naturalOrder()).orElse(0) + 1; HazardAgentDecision value = new HazardAgentDecision(); value.runId = runId; value.candidateId = candidateId; value.decision = decision; value.modelOutputJson = write(candidate); value.editedHazardType = blankToNull(type); value.editedDescription = blankToNull(description); value.editedRiskLevel = blankToNull(risk); value.editedMeasuresJson = measures; value.reviewerNote = blankToNull(note); value.decidedBy = userId; value.decidedAt = LocalDateTime.now(); value.decisionVersion = version; value.createdAt = value.decidedAt; decisionMapper.insert(value); }
  private boolean isStale(HazardAgentRun run, Long recordId) { ThreeCheckRecordDetailResponse record = recordService.detail(MODULE_KEY, recordId); if (record.version() != run.sourceRecordVersion) return true; String current = sha256(snapshotImages(images(recordId)).stream().map(ImageInput::sha256).sorted().collect(Collectors.joining(","))); return !current.equals(run.attachmentHashSet); }
  private void markStale(HazardAgentRun run) { run.status = "STALE"; run.updatedAt = LocalDateTime.now(); runMapper.updateById(run); audit(run, "STALE", "源记录版本或图片已变更"); }
  private String effectiveDescription(HazardAgentDecision decision) { if (decision.editedDescription != null && !decision.editedDescription.isBlank()) return decision.editedDescription; try { return mapper.readTree(decision.modelOutputJson).path("description").asText(); } catch (Exception exception) { throw new BusinessException("候选描述不可用"); } }
  private QuickShotAgentRunResponse response(HazardAgentRun run) { List<QuickShotAgentRunResponse.Attachment> attachments = runAttachmentMapper.selectList(new QueryWrapper<HazardAgentRunAttachment>().eq("run_id", run.runId).orderByAsc("image_index")).stream().map(a -> new QuickShotAgentRunResponse.Attachment(String.valueOf(a.attachmentId), a.attachmentSha256, a.mimeType, a.fileSize, a.imageIndex)).toList(); List<QuickShotAgentRunResponse.Decision> decisions = latestDecisions(run.runId).stream().map(d -> new QuickShotAgentRunResponse.Decision(d.candidateId, d.decision, d.editedHazardType, d.editedDescription, d.editedRiskLevel, tree(d.editedMeasuresJson), d.reviewerNote, d.decisionVersion, d.decidedAt)).toList(); return new QuickShotAgentRunResponse(run.runId, String.valueOf(run.sourceRecordId), run.sourceRecordVersion, run.status, run.knowledgeStatus, run.analysisId, run.assessmentId, run.model, run.inputText, tree(run.modelOutputJson), tree(run.assessmentJson), attachments, decisions, run.errorCode, run.errorMessage, run.startedAt, run.completedAt); }
  private void fail(HazardAgentRun run, String code, String message) { run.status = "FAILED"; run.errorCode = code; run.errorMessage = message; run.completedAt = LocalDateTime.now(); run.updatedAt = run.completedAt; runMapper.updateById(run); audit(run, "FAILED", code); }
  private void audit(HazardAgentRun run, String action, String message) { auditLogService.record(SystemModule.SECURITY, "QUICK_SHOT_AGENT_RUN", run.sourceRecordId, action, message + " runId=" + run.runId); }
  private void requireEnabled() { if (!properties.enabled()) throw new BusinessException("SafeGuard Agent 试点未启用"); if (properties.writebackEnabled()) throw new BusinessException("Agent 写回开关必须保持关闭"); }
  private String requiredToken() { if (properties.serviceToken() == null || properties.serviceToken().isBlank()) throw new BusinessException("未配置 SAFEGUARD_SERVICE_TOKEN"); return properties.serviceToken(); }
  private String inputText(ThreeCheckRecordDetailResponse record, String extra) { String value = record.payload() == null ? "" : String.valueOf(record.payload().getOrDefault("hazardDescription", "")); return extra == null || extra.isBlank() ? value : (value.isBlank() ? extra.trim() : value + "\n补充说明：" + extra.trim()); }
  private String normalizeDecision(String value) { String decision = value == null ? "" : value.trim().toUpperCase(); if (!DECISIONS.contains(decision)) throw new BusinessException("候选决定不合法"); return decision; }
  private String sha256(String value) { return sha256(value.getBytes(StandardCharsets.UTF_8)); }
  private String sha256(byte[] value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); } catch (Exception exception) { throw new IllegalStateException("SHA-256 不可用", exception); } }
  private JsonNode tree(String json) { try { return json == null || json.isBlank() ? null : mapper.readTree(json); } catch (Exception exception) { return null; } }
  private String write(Object value) { try { return value == null ? null : mapper.writeValueAsString(value); } catch (Exception exception) { throw new BusinessException("无法保存 Agent 结果"); } }
  private String safeMessage(Exception exception) { return exception.getMessage() == null ? "未知错误" : exception.getMessage().replaceAll("[\\r\\n]", " "); }
  private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
  private record ImageInput(BizAttachment attachment, byte[] bytes, String sha256, int index) {}
}
