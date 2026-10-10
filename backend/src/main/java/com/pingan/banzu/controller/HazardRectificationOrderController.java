package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.AttachmentResponse;
import com.pingan.banzu.dto.HazardRectificationOrderActionRequest;
import com.pingan.banzu.dto.HazardRectificationOrderBatchDeleteRequest;
import com.pingan.banzu.dto.HazardRectificationOrderCreateRequest;
import com.pingan.banzu.dto.HazardRectificationOrderDetailResponse;
import com.pingan.banzu.dto.HazardRectificationOrderListItem;
import com.pingan.banzu.dto.HazardRectificationOrderQuery;
import com.pingan.banzu.dto.RectificationAgentReviewRequest;
import com.pingan.banzu.dto.RectificationAgentSummaryRequest;
import com.pingan.banzu.dto.RectificationAgentSummaryConfirmRequest;
import com.pingan.banzu.dto.RectificationAgentSummaryEditRequest;
import com.pingan.banzu.service.FileStorageService;
import com.pingan.banzu.service.HazardRectificationOrderService;
import com.pingan.banzu.service.RectificationAgentAssistService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;

@RestController
@RequestMapping("/api/pingan/hazard-rectification/orders")
public class HazardRectificationOrderController {

  private static final String BIZ_TYPE = "HAZARD_RECTIFICATION_ORDER";
  private static final String MODULE_FOLDER = "hazard-rectification/order";

  private final FileStorageService fileStorageService;
  private final HazardRectificationOrderService orderService;
  private final RectificationAgentAssistService agentAssistService;

  public HazardRectificationOrderController(
      FileStorageService fileStorageService, HazardRectificationOrderService orderService,
      RectificationAgentAssistService agentAssistService) {
    this.fileStorageService = fileStorageService;
    this.orderService = orderService;
    this.agentAssistService = agentAssistService;
  }

  @GetMapping
  public ApiResponse<PageResult<HazardRectificationOrderListItem>> list(
      @ModelAttribute HazardRectificationOrderQuery query) {
    return ApiResponse.ok(orderService.list(query));
  }

  @GetMapping("/{id}")
  public ApiResponse<HazardRectificationOrderDetailResponse> detail(@PathVariable Long id) {
    return ApiResponse.ok(orderService.detail(id));
  }

  @PostMapping
  public ApiResponse<HazardRectificationOrderDetailResponse> create(
      @RequestBody HazardRectificationOrderCreateRequest request) {
    return ApiResponse.ok(orderService.createManual(request));
  }

  @PostMapping("/batch-delete")
  public ApiResponse<Void> batchDelete(
      @Valid @RequestBody HazardRectificationOrderBatchDeleteRequest request) {
    orderService.batchDelete(request);
    return ApiResponse.ok(null);
  }

  @PostMapping("/{id}/actions")
  public ApiResponse<HazardRectificationOrderDetailResponse> action(
      @PathVariable Long id, @RequestBody HazardRectificationOrderActionRequest request) {
    HazardRectificationOrderDetailResponse result = orderService.action(id, request);
    if ("CLOSED".equals(result.status())) {
      CurrentUser actor = CurrentUserContext.require();
      CompletableFuture.runAsync(() -> {
        try {
          CurrentUserContext.set(actor);
          List<String> measures = result.items().stream()
              .map(com.pingan.banzu.dto.HazardRectificationOrderItemResponse::defaultFollowUpPlan)
              .filter(value -> value != null && !value.isBlank()).distinct().toList();
          List<String> acceptanceCriteria = result.items().stream()
              .flatMap(item -> sourceList(item.sourceSnapshot(), "acceptanceCriteria").stream())
              .distinct().toList();
          List<String> hazardDescriptions = result.items().stream()
              .map(com.pingan.banzu.dto.HazardRectificationOrderItemResponse::hazardDescription)
              .filter(value -> value != null && !value.isBlank()).toList();
          agentAssistService.summary(Long.valueOf(result.id()), new RectificationAgentSummaryRequest(
              result.version(), String.join("；", hazardDescriptions), result.rectificationDescription(), result.acceptanceResult(),
              result.acceptanceRemark(), measures, acceptanceCriteria,
              List.of("order closed at " + result.closedAt()),
              result.rectificationAfterPhoto() == null ? List.of() : List.of(result.rectificationAfterPhoto())));
        } catch (RuntimeException ignored) {
          // Summary generation is best-effort and never blocks the authoritative close action.
        } finally {
          CurrentUserContext.clear();
        }
      });
    }
    return ApiResponse.ok(result);
  }

  private static List<String> sourceList(java.util.Map<String, Object> snapshot, String key) {
    if (snapshot == null) return List.of();
    Object value = snapshot.get(key);
    if (value instanceof List<?> values) {
      return values.stream().filter(String.class::isInstance).map(String.class::cast)
          .map(String::trim).filter(text -> !text.isBlank()).toList();
    }
    if (value instanceof String text && !text.isBlank()) {
      return java.util.Arrays.stream(text.split("[；;\\n]"))
          .map(String::trim).filter(item -> !item.isBlank()).toList();
    }
    return List.of();
  }

  @PostMapping("/{id}/agent-review")
  public ApiResponse<com.fasterxml.jackson.databind.JsonNode> agentReview(
      @PathVariable Long id, @RequestBody RectificationAgentReviewRequest request) {
    HazardRectificationOrderDetailResponse detail = orderService.detail(id);
    if ("CLOSED".equals(detail.status()) || "CANCELLED".equals(detail.status())) {
      throw new com.pingan.banzu.common.BusinessException("当前工单状态不支持整改复核");
    }
    return ApiResponse.ok(agentAssistService.review(id, request));
  }

  @PostMapping("/{id}/agent-summary")
  public ApiResponse<com.fasterxml.jackson.databind.JsonNode> agentSummary(
      @PathVariable Long id, @RequestBody RectificationAgentSummaryRequest request) {
    HazardRectificationOrderDetailResponse detail = orderService.detail(id);
    if (!"CLOSED".equals(detail.status())) {
      throw new com.pingan.banzu.common.BusinessException("仅已关闭工单可生成整改说明总结");
    }
    return ApiResponse.ok(agentAssistService.summary(id, request));
  }

  @GetMapping("/{id}/agent-summary")
  public ApiResponse<com.pingan.banzu.domain.HazardAgentRectificationSummary> agentSummaryLatest(@PathVariable Long id) {
    return ApiResponse.ok(agentAssistService.latestSummary(id));
  }

  @PostMapping("/{id}/agent-summary/confirm")
  public ApiResponse<com.pingan.banzu.domain.HazardAgentRectificationSummary> agentSummaryConfirm(
      @PathVariable Long id, @RequestBody RectificationAgentSummaryConfirmRequest request) {
    return ApiResponse.ok(agentAssistService.confirmSummary(id, request));
  }

  @PostMapping("/{id}/agent-summary/edit")
  public ApiResponse<com.pingan.banzu.domain.HazardAgentRectificationSummary> agentSummaryEdit(
      @PathVariable Long id, @RequestBody RectificationAgentSummaryEditRequest request) {
    return ApiResponse.ok(agentAssistService.editSummary(id, request));
  }

  @PostMapping("/{id}/attachments")
  public ApiResponse<AttachmentResponse> uploadAttachment(
      @PathVariable Long id,
      @RequestParam String fileKind,
      @RequestParam MultipartFile file) {
    orderService.assertCanUploadAttachment(id, fileKind);
    return ApiResponse.ok(
        fileStorageService.saveBusinessAttachment(BIZ_TYPE, id, MODULE_FOLDER, fileKind, file));
  }
}
