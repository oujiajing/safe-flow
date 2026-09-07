package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.dto.ThreeCheckRecordChangeHistoryResponse;
import com.pingan.banzu.dto.ThreeCheckRecordDocumentFlowResponse;
import com.pingan.banzu.service.ThreeCheckRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pingan/hazard-inspection/{moduleKey}/records")
public class HazardInspectionDocumentFlowController {

  private final ThreeCheckRecordService recordService;

  public HazardInspectionDocumentFlowController(ThreeCheckRecordService recordService) {
    this.recordService = recordService;
  }

  @GetMapping("/{id}/document-flow")
  public ApiResponse<ThreeCheckRecordDocumentFlowResponse> documentFlow(
      @PathVariable String moduleKey, @PathVariable Long id) {
    return ApiResponse.ok(recordService.documentFlow(moduleKey, id));
  }

  @GetMapping("/{id}/change-history")
  public ApiResponse<ThreeCheckRecordChangeHistoryResponse> changeHistory(
      @PathVariable String moduleKey, @PathVariable Long id) {
    return ApiResponse.ok(recordService.changeHistory(moduleKey, id));
  }
}
