package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.dto.ThreeCheckFlowResponse;
import com.pingan.banzu.service.ThreeCheckRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pingan/three-checks/flows")
public class ThreeCheckFlowController {

  private final ThreeCheckRecordService recordService;

  public ThreeCheckFlowController(ThreeCheckRecordService recordService) {
    this.recordService = recordService;
  }

  @GetMapping("/{rootDispatchRecordId}")
  public ApiResponse<ThreeCheckFlowResponse> flow(@PathVariable Long rootDispatchRecordId) {
    return ApiResponse.ok(recordService.flow(rootDispatchRecordId));
  }
}
