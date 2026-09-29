package com.pingan.banzu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.config.SafeGuardAgentProperties;
import com.pingan.banzu.dto.RectificationAgentReviewRequest;
import com.pingan.banzu.dto.RectificationAgentSummaryRequest;
import com.pingan.banzu.dto.RectificationAgentSummaryConfirmRequest;
import com.pingan.banzu.domain.HazardAgentRectificationSummary;
import com.pingan.banzu.mapper.HazardAgentRectificationSummaryMapper;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class RectificationAgentAssistService {
  private final SafeGuardAgentProperties properties;
  private final ObjectMapper mapper;
  private final HazardAgentRectificationSummaryMapper summaryMapper;
  private final AuditLogService auditLogService;
  private final HttpClient client = HttpClient.newHttpClient();

  public RectificationAgentAssistService(SafeGuardAgentProperties properties, ObjectMapper mapper,
      HazardAgentRectificationSummaryMapper summaryMapper, AuditLogService auditLogService) {
    this.properties = properties; this.mapper = mapper; this.summaryMapper = summaryMapper; this.auditLogService = auditLogService;
  }

  public JsonNode review(Long orderId, RectificationAgentReviewRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("orderId", String.valueOf(orderId)); body.put("orderVersion", request.orderVersion());
    body.put("criteria", (request.criteria() == null ? java.util.List.<String>of() : request.criteria()).stream().map(text -> Map.of("text", text)).toList());
    body.put("beforeEvidence", request.beforeEvidence()); body.put("afterEvidence", request.afterEvidence());
    body.put("beforeImageData", request.beforeImageData()); body.put("afterImageData", request.afterImageData());
    body.put("rectificationDescription", request.rectificationDescription()); body.put("executionContext", context(orderId));
    return post("/agent/v1/hosted/rectification-reviews", body);
  }

  public JsonNode summary(Long orderId, RectificationAgentSummaryRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("orderId", String.valueOf(orderId)); body.put("orderVersion", request.orderVersion());
    body.put("hazardDescription", request.hazardDescription()); body.put("rectificationDescription", request.rectificationDescription());
    body.put("acceptanceResult", request.acceptanceResult()); body.put("acceptanceRemark", request.acceptanceRemark());
    body.put("measures", request.measures()); body.put("acceptanceCriteria", request.acceptanceCriteria());
    body.put("flowFacts", request.flowFacts()); body.put("citations", request.citations()); body.put("executionContext", context(orderId));
    JsonNode result = post("/agent/v1/hosted/rectification-summaries", body);
    int version = saveDraft(orderId, request.orderVersion(), result);
    if (result.isObject()) ((com.fasterxml.jackson.databind.node.ObjectNode) result).put("summaryVersion", version);
    return result;
  }

  public HazardAgentRectificationSummary latestSummary(Long orderId) {
    return summaryMapper.selectOne(new QueryWrapper<HazardAgentRectificationSummary>().eq("order_id", orderId).orderByDesc("summary_version").last("limit 1"));
  }

  public HazardAgentRectificationSummary confirmSummary(Long orderId, RectificationAgentSummaryConfirmRequest request) {
    HazardAgentRectificationSummary value = summaryMapper.selectOne(new QueryWrapper<HazardAgentRectificationSummary>().eq("order_id", orderId).eq("summary_version", request.summaryVersion()).last("limit 1"));
    if (value == null) throw new BusinessException("总结版本不存在");
    if (!"DRAFT".equals(value.status) && !"REVISED".equals(value.status)) throw new BusinessException("当前总结版本不可确认");
    if (value.orderVersion == null || value.orderVersion != request.orderVersion()) throw new BusinessException("工单已更新，请重新生成总结");
    if (request.confirmedJson() == null || request.confirmedJson().isBlank()) throw new BusinessException("确认稿不能为空");
    value.confirmedJson = request.confirmedJson(); value.status = "CONFIRMED"; value.confirmedBy = CurrentUserContext.require().userId(); value.confirmedAt = java.time.LocalDateTime.now(); value.updatedAt = value.confirmedAt;
    summaryMapper.updateById(value);
    auditLogService.record(SystemModule.SECURITY, "HAZARD_AGENT_RECTIFICATION_SUMMARY", value.id, "CONFIRM", "确认整改说明总结版本 " + value.summaryVersion);
    return value;
  }

  public HazardAgentRectificationSummary editSummary(Long orderId, com.pingan.banzu.dto.RectificationAgentSummaryEditRequest request) {
    HazardAgentRectificationSummary value = summaryMapper.selectOne(new QueryWrapper<HazardAgentRectificationSummary>()
        .eq("order_id", orderId).eq("summary_version", request.summaryVersion()).last("limit 1"));
    if (value == null) throw new BusinessException("总结版本不存在");
    if (!"DRAFT".equals(value.status) && !"REVISED".equals(value.status)) throw new BusinessException("已冻结的总结不可编辑");
    if (value.orderVersion == null || value.orderVersion != request.orderVersion()) throw new BusinessException("工单已更新，请重新生成总结");
    if (request.editedJson() == null || request.editedJson().isBlank()) throw new BusinessException("修订稿不能为空");
    String previous = value.confirmedJson != null ? value.confirmedJson : value.generatedJson;
    value.confirmedJson = request.editedJson();
    value.status = "REVISED";
    value.updatedAt = java.time.LocalDateTime.now();
    summaryMapper.updateById(value);
    String reason = request.editReason() == null ? "" : request.editReason();
    auditLogService.record(SystemModule.SECURITY, "HAZARD_AGENT_RECTIFICATION_SUMMARY", value.id, "EDIT",
        "修订整改说明总结版本 " + value.summaryVersion + "; previousLength=" + (previous == null ? 0 : previous.length())
            + "; currentLength=" + request.editedJson().length() + "; reason=" + reason);
    return value;
  }

  private int saveDraft(Long orderId, int orderVersion, JsonNode result) {
    HazardAgentRectificationSummary value = new HazardAgentRectificationSummary();
    value.orderId = orderId; value.orderVersion = orderVersion; value.summaryVersion = nextVersion(orderId); value.status = "DRAFT";
    value.sourceDigest = result.path("sourceDigest").asText(""); value.generatedJson = result.toString(); value.createdBy = CurrentUserContext.require().userId();
    value.createdAt = java.time.LocalDateTime.now(); value.updatedAt = value.createdAt; summaryMapper.insert(value);
    auditLogService.record(SystemModule.SECURITY, "HAZARD_AGENT_RECTIFICATION_SUMMARY", value.id, "DRAFT", "生成整改说明总结草稿版本 " + value.summaryVersion);
    return value.summaryVersion;
  }

  private int nextVersion(Long orderId) { HazardAgentRectificationSummary latest = latestSummary(orderId); return latest == null || latest.summaryVersion == null ? 1 : latest.summaryVersion + 1; }

  private JsonNode post(String path, Map<String, Object> body) {
    if (!properties.enabled()) throw new BusinessException("SafeGuard Agent 未启用");
    if (properties.serviceToken() == null || properties.serviceToken().isBlank()) throw new BusinessException("未配置 SAFEGUARD_SERVICE_TOKEN");
    try {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(properties.baseUrl() + path)).timeout(Duration.ofMillis(properties.requestTimeoutMs()))
          .header("Accept", "application/json").header("Content-Type", "application/json").header("X-Safeguard-Service-Token", properties.serviceToken())
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) throw new BusinessException("Agent 服务失败（HTTP " + response.statusCode() + "）");
      return mapper.readTree(response.body());
    } catch (BusinessException exception) { throw exception; }
    catch (Exception exception) { throw new BusinessException("Agent 复核/总结服务不可用"); }
  }

  private Map<String, Object> context(Long orderId) {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("actorUserId", CurrentUserContext.require().userId());
    context.put("enterpriseId", null); context.put("projectId", null); context.put("teamId", null);
    context.put("sourceHazardId", "hazard-rectification:" + orderId);
    context.put("traceId", "safe-flow-rectification-" + orderId);
    return context;
  }
}
