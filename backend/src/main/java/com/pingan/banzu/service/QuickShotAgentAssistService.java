package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.ThreeCheckBizType;
import com.pingan.banzu.config.SafeGuardAgentProperties;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.dto.QuickShotAgentAssistResponse;
import com.pingan.banzu.dto.ThreeCheckRecordDetailResponse;
import com.pingan.banzu.mapper.BizAttachmentMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class QuickShotAgentAssistService {
  private static final String MODULE_KEY = "quick-shot";
  private final ThreeCheckRecordService recordService;
  private final BizAttachmentMapper attachmentMapper;
  private final AttachmentContentReader contentReader;
  private final SafeGuardAgentProperties properties;
  private final ObjectMapper mapper;
  private final HttpClient httpClient;
  private final AuditLogService auditLogService;

  public QuickShotAgentAssistService(
      ThreeCheckRecordService recordService,
      BizAttachmentMapper attachmentMapper,
      AttachmentContentReader contentReader,
      SafeGuardAgentProperties properties,
      ObjectMapper mapper,
      AuditLogService auditLogService) {
    this.recordService = recordService;
    this.attachmentMapper = attachmentMapper;
    this.contentReader = contentReader;
    this.properties = properties;
    this.mapper = mapper;
    this.auditLogService = auditLogService;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
  }

  public QuickShotAgentAssistResponse analyze(Long recordId, String extraDescription) {
    if (!properties.enabled()) {
      throw new BusinessException("SafeGuard Agent 试点未启用");
    }
    if (recordId == null) {
      throw new BusinessException("随手拍记录不存在");
    }
    CurrentUser actor = CurrentUserContext.require();
    ThreeCheckRecordDetailResponse record = recordService.detail(MODULE_KEY, recordId);
    if (record.attachments() == null || record.attachments().isEmpty()) {
      throw new BusinessException("随手拍记录没有可分析的附件");
    }
    BizAttachment attachment = attachmentMapper.selectOne(new QueryWrapper<BizAttachment>()
        .eq("biz_type", ThreeCheckBizType.HAZARD_QUICK_SHOT)
        .eq("biz_id", recordId)
        .eq("file_kind", "IMAGE")
        .eq("deleted", 0)
        .orderByAsc("uploaded_at")
        .last("limit 1"));
    if (attachment == null) {
      throw new BusinessException("随手拍记录没有可分析的图片");
    }
    byte[] bytes = contentReader.read(attachment);
    if (bytes.length == 0 || bytes.length > properties.maxImageBytes()) {
      throw new BusinessException("图片大小不符合 Agent 试点限制");
    }
    String description = record.payload() == null ? "" : String.valueOf(
        record.payload().getOrDefault("hazardDescription", ""));
    if (extraDescription != null && !extraDescription.isBlank()) {
      description = description.isBlank() ? extraDescription.trim()
          : description + "\n补充说明：" + extraDescription.trim();
    }
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("actorUserId", actor.userId());
    context.put("enterpriseId", record.companyId());
    context.put("projectId", record.departmentId());
    context.put("teamId", record.teamId());
    context.put("sourceHazardId", MODULE_KEY + ":" + record.id());
    context.put("traceId", "safe-flow-agent-" + UUID.randomUUID());
    Map<String, Object> requestBody = new LinkedHashMap<>();
    requestBody.put("description", description);
    requestBody.put("imageBase64", Base64.getEncoder().encodeToString(bytes));
    requestBody.put("executionContext", context);
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(properties.baseUrl() + "/agent/visual-hazard-analysis"))
          .timeout(Duration.ofMillis(properties.requestTimeoutMs()))
          .header("Accept", "application/json")
          .header("Content-Type", "application/json")
          .header("X-Safeguard-Service-Token", requiredServiceToken())
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessException("Agent 视觉研判失败（HTTP " + response.statusCode() + "）");
      }
      JsonNode root = mapper.readTree(response.body());
      if (root.has("code") && !root.path("code").asText().equals("0")) {
        throw new BusinessException("Agent 视觉研判不可用：" + root.path("message").asText("未知错误"));
      }
      List<QuickShotAgentAssistResponse.Candidate> candidates = root.path("hazardCandidates").isArray()
          ? mapper.readerForListOf(QuickShotAgentAssistResponse.Candidate.class)
              .readValue(root.path("hazardCandidates")) : List.of();
      QuickShotAgentAssistResponse result = new QuickShotAgentAssistResponse(
          record.id(), record.version(), root.path("analysisId").asText(),
          root.path("scene").asText("UNKNOWN"), root.path("model").asText(""),
          root.hasNonNull("analyzedAt") ? Instant.parse(root.path("analyzedAt").asText()) : null,
          candidates);
      auditLogService.record(
          SystemModule.SECURITY,
          "QUICK_SHOT_AGENT_ASSIST",
          recordId,
          "ANALYZE",
          "随手拍 AI 辅助研判 " + result.analysisId());
      return result;
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException("Agent 视觉研判请求失败：" + exception.getMessage());
    }
  }

  private String requiredServiceToken() {
    if (properties.serviceToken() == null || properties.serviceToken().isBlank()) {
      throw new BusinessException("未配置 SAFEGUARD_SERVICE_TOKEN");
    }
    return properties.serviceToken();
  }
}
