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
import com.pingan.banzu.service.FileStorageService;
import com.pingan.banzu.service.HazardRectificationOrderService;
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

@RestController
@RequestMapping("/api/pingan/hazard-rectification/orders")
public class HazardRectificationOrderController {

  private static final String BIZ_TYPE = "HAZARD_RECTIFICATION_ORDER";
  private static final String MODULE_FOLDER = "hazard-rectification/order";

  private final FileStorageService fileStorageService;
  private final HazardRectificationOrderService orderService;

  public HazardRectificationOrderController(
      FileStorageService fileStorageService, HazardRectificationOrderService orderService) {
    this.fileStorageService = fileStorageService;
    this.orderService = orderService;
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
    return ApiResponse.ok(orderService.action(id, request));
  }

  @PostMapping("/{id}/attachments")
  public ApiResponse<AttachmentResponse> uploadAttachment(
      @PathVariable Long id,
      @RequestParam String fileKind,
      @RequestParam MultipartFile file) {
    orderService.assertCanUploadAttachment(id);
    return ApiResponse.ok(
        fileStorageService.saveBusinessAttachment(BIZ_TYPE, id, MODULE_FOLDER, fileKind, file));
  }
}
