package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.system.dto.SystemContentProfileResponse;
import com.pingan.banzu.system.service.SystemContentProfileService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pingan/content-profiles")
public class PinganContentProfileController {

  private final SystemContentProfileService service;

  public PinganContentProfileController(SystemContentProfileService service) {
    this.service = service;
  }

  @GetMapping("/by-org/{orgId}")
  public ApiResponse<SystemContentProfileResponse> byOrg(@PathVariable Long orgId) {
    return ApiResponse.ok(service.activeByOrg(orgId));
  }

  @GetMapping("/screen-videos")
  public ApiResponse<List<SystemContentProfileResponse>> screenVideos(@RequestParam(required = false) Long orgId) {
    return ApiResponse.ok(service.activeScreenVideos(orgId));
  }
}
