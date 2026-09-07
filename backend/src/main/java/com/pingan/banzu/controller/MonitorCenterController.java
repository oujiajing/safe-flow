package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse;
import com.pingan.banzu.dto.MonitorCenterQuery;
import com.pingan.banzu.service.MonitorCenterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pingan/monitor-center")
public class MonitorCenterController {

  private final MonitorCenterService service;

  public MonitorCenterController(MonitorCenterService service) {
    this.service = service;
  }

  @GetMapping("/overview")
  public ApiResponse<MonitorCenterOverviewResponse> overview(@ModelAttribute MonitorCenterQuery query) {
    return ApiResponse.ok(service.overview(query));
  }
}
