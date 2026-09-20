package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.config.SafeGuardAgentProperties;
import com.pingan.banzu.dto.QuickShotAgentAssistResponse;
import com.pingan.banzu.dto.QuickShotAgentCandidateDecisionRequest;
import com.pingan.banzu.dto.QuickShotAgentReviewDraftRequest;
import com.pingan.banzu.dto.QuickShotAgentRunRequest;
import com.pingan.banzu.dto.QuickShotAgentRunResponse;
import com.pingan.banzu.dto.QuickShotAgentRectificationPreviewResponse;
import com.pingan.banzu.dto.QuickShotAgentRectificationApplyResponse;
import com.pingan.banzu.service.QuickShotAgentAssistService;
import com.pingan.banzu.service.QuickShotAgentRunService;
import com.pingan.banzu.service.QuickShotAgentRectificationPreviewService;
import com.pingan.banzu.service.QuickShotAgentRectificationApplyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pingan/three-checks/quick-shot/records")
@ConditionalOnProperty(prefix = "pingan.safeguard-agent", name = "enabled", havingValue = "true")
public class QuickShotAgentAssistController {
  private final QuickShotAgentAssistService service;
  private final QuickShotAgentRunService runService;
  private final QuickShotAgentRectificationPreviewService previewService;
  private final QuickShotAgentRectificationApplyService applyService;

  public QuickShotAgentAssistController(QuickShotAgentAssistService service, QuickShotAgentRunService runService,
      QuickShotAgentRectificationPreviewService previewService, QuickShotAgentRectificationApplyService applyService) {
    this.service = service; this.runService = runService; this.previewService = previewService; this.applyService = applyService;
  }

  @PostMapping("/{id}/agent-assist")
  public ApiResponse<QuickShotAgentAssistResponse> analyze(
      @PathVariable Long id, @RequestBody(required = false) AgentAssistRequest request) {
    return ApiResponse.ok(service.analyze(id, request == null ? null : request.extraDescription()));
  }

  @PostMapping("/{id}/agent-runs")
  public ApiResponse<QuickShotAgentRunResponse> createRun(
      @PathVariable Long id, @RequestBody(required = false) QuickShotAgentRunRequest request) {
    return ApiResponse.ok(runService.create(id, request));
  }

  @GetMapping("/{id}/agent-runs")
  public ApiResponse<QuickShotAgentRunResponse> latestRun(@PathVariable Long id) {
    return ApiResponse.ok(runService.latest(id));
  }

  @GetMapping("/{id}/agent-runs/{runId}")
  public ApiResponse<QuickShotAgentRunResponse> runDetail(
      @PathVariable Long id, @PathVariable String runId) {
    return ApiResponse.ok(runService.detail(id, runId));
  }

  @PostMapping("/{id}/agent-runs/{runId}/candidate-decisions")
  public ApiResponse<QuickShotAgentRunResponse> decide(
      @PathVariable Long id, @PathVariable String runId,
      @RequestBody QuickShotAgentCandidateDecisionRequest request) {
    return ApiResponse.ok(runService.decide(id, runId, request));
  }

  @PostMapping("/{id}/agent-runs/{runId}/assess")
  public ApiResponse<QuickShotAgentRunResponse> assess(
      @PathVariable Long id, @PathVariable String runId) {
    return ApiResponse.ok(runService.assess(id, runId));
  }

  @PutMapping("/{id}/agent-runs/{runId}/review-draft")
  public ApiResponse<QuickShotAgentRunResponse> reviewDraft(
      @PathVariable Long id, @PathVariable String runId,
      @RequestBody QuickShotAgentReviewDraftRequest request) {
    return ApiResponse.ok(runService.saveReviewDraft(id, runId, request));
  }

  @GetMapping("/{id}/agent-runs/{runId}/rectification-preview")
  public ApiResponse<QuickShotAgentRectificationPreviewResponse> rectificationPreview(
      @PathVariable Long id, @PathVariable String runId) {
    return ApiResponse.ok(previewService.preview(id, runId));
  }

  @GetMapping("/{id}/agent-runs/{runId}/rectification-apply")
  public ApiResponse<QuickShotAgentRectificationApplyResponse> rectificationApply(
      @PathVariable Long id, @PathVariable String runId,
      @org.springframework.web.bind.annotation.RequestHeader(value = "Idempotency-Key", required = false) String key) {
    var value = applyService.find(id, runId, key);
    return ApiResponse.ok(value == null ? null : new QuickShotAgentRectificationApplyResponse(value.runId, value.status,
        value.orderId == null ? null : String.valueOf(value.orderId), value.errorCode, value.errorMessage, value.appliedAt));
  }

  public record AgentAssistRequest(String extraDescription) {}
}
