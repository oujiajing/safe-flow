package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.AttachmentResponse;
import com.pingan.banzu.dto.HazardRectificationOrderDetailResponse;
import com.pingan.banzu.dto.QuickShotWorkflowActionRequest;
import com.pingan.banzu.dto.ThreeCheckRecordBatchRequest;
import com.pingan.banzu.dto.ThreeCheckRecordBatchResponse;
import com.pingan.banzu.dto.ThreeCheckRecordDetailResponse;
import com.pingan.banzu.dto.ThreeCheckRecordListItem;
import com.pingan.banzu.dto.ThreeCheckRecordQuery;
import com.pingan.banzu.dto.ThreeCheckRecordStatisticsResponse;
import com.pingan.banzu.dto.ThreeCheckRecordUpsertRequest;
import com.pingan.banzu.dto.ThreeCheckRecordWorkflowResponse;
import com.pingan.banzu.dto.WithdrawRequest;
import com.pingan.banzu.service.FileStorageService;
import com.pingan.banzu.service.ThreeCheckRecordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pingan/three-checks/{moduleKey}/records")
public class ThreeCheckRecordController {

  private final FileStorageService fileStorageService;
  private final ThreeCheckRecordService recordService;

  public ThreeCheckRecordController(
      FileStorageService fileStorageService, ThreeCheckRecordService recordService) {
    this.fileStorageService = fileStorageService;
    this.recordService = recordService;
  }

  @GetMapping
  public ApiResponse<PageResult<ThreeCheckRecordListItem>> list(
      @PathVariable String moduleKey, @ModelAttribute ThreeCheckRecordQuery query) {
    return ApiResponse.ok(recordService.list(moduleKey, query));
  }

  @GetMapping("/statistics")
  public ApiResponse<ThreeCheckRecordStatisticsResponse> statistics(
      @PathVariable String moduleKey, @ModelAttribute ThreeCheckRecordQuery query) {
    return ApiResponse.ok(recordService.statistics(moduleKey, query));
  }

  @GetMapping("/{id}")
  public ApiResponse<ThreeCheckRecordDetailResponse> detail(
      @PathVariable String moduleKey, @PathVariable Long id) {
    return ApiResponse.ok(recordService.detail(moduleKey, id));
  }

  @GetMapping("/{id}/workflow")
  public ApiResponse<ThreeCheckRecordWorkflowResponse> workflow(
      @PathVariable String moduleKey, @PathVariable Long id) {
    return ApiResponse.ok(recordService.workflow(moduleKey, id));
  }

  @PostMapping
  public ApiResponse<ThreeCheckRecordDetailResponse> create(
      @PathVariable String moduleKey, @Valid @RequestBody ThreeCheckRecordUpsertRequest request) {
    return ApiResponse.ok(recordService.create(moduleKey, request));
  }

  @PutMapping("/{id}")
  public ApiResponse<ThreeCheckRecordDetailResponse> update(
      @PathVariable String moduleKey,
      @PathVariable Long id,
      @Valid @RequestBody ThreeCheckRecordUpsertRequest request) {
    return ApiResponse.ok(recordService.update(moduleKey, id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable String moduleKey, @PathVariable Long id) {
    recordService.delete(moduleKey, id);
    return ApiResponse.ok();
  }

  @PostMapping("/batch")
  public ApiResponse<ThreeCheckRecordBatchResponse> batch(
      @PathVariable String moduleKey,
      @Valid @RequestBody ThreeCheckRecordBatchRequest request) {
    return ApiResponse.ok(recordService.batch(moduleKey, request));
  }

  @PostMapping("/{id}/submit")
  public ApiResponse<ThreeCheckRecordDetailResponse> submit(
      @PathVariable String moduleKey, @PathVariable Long id) {
    return ApiResponse.ok(recordService.submit(moduleKey, id));
  }

  @PostMapping("/{id}/withdraw")
  public ApiResponse<ThreeCheckRecordDetailResponse> withdraw(
      @PathVariable String moduleKey,
      @PathVariable Long id,
      @RequestBody(required = false) WithdrawRequest request) {
    return ApiResponse.ok(recordService.withdraw(moduleKey, id, request));
  }

  @PostMapping("/{id}/remind")
  public ApiResponse<ThreeCheckRecordDetailResponse> remind(
      @PathVariable String moduleKey, @PathVariable Long id) {
    return ApiResponse.ok(recordService.remind(moduleKey, id));
  }

  @PostMapping("/{id}/rectification-order")
  public ApiResponse<HazardRectificationOrderDetailResponse> openRectificationOrder(
      @PathVariable String moduleKey, @PathVariable Long id) {
    return ApiResponse.ok(recordService.openRectificationOrder(moduleKey, id));
  }

  @PostMapping("/{id}/workflow-actions")
  public ApiResponse<ThreeCheckRecordDetailResponse> workflowAction(
      @PathVariable String moduleKey,
      @PathVariable Long id,
      @RequestBody QuickShotWorkflowActionRequest request) {
    return ApiResponse.ok(recordService.quickShotWorkflowAction(moduleKey, id, request));
  }

  @PostMapping("/{id}/attachments")
  public ApiResponse<AttachmentResponse> uploadAttachment(
      @PathVariable String moduleKey,
      @PathVariable Long id,
      @RequestParam String fileKind,
      @RequestParam MultipartFile file) {
    return ApiResponse.ok(fileStorageService.saveThreeCheckRecordAttachment(moduleKey, id, fileKind, file));
  }

  @DeleteMapping("/{id}/attachments/{attachmentId}")
  public ApiResponse<Void> deleteAttachment(
      @PathVariable String moduleKey,
      @PathVariable Long id,
      @PathVariable Long attachmentId) {
    fileStorageService.deleteThreeCheckRecordAttachment(moduleKey, id, attachmentId);
    return ApiResponse.ok();
  }
}
