package com.pingan.banzu.system.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.system.service.SystemOptionService;
import com.pingan.banzu.system.service.SystemOptionService.OptionItem;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/options")
public class SystemOptionController {
  private final SystemOptionService service;

  public SystemOptionController(SystemOptionService service) {
    this.service = service;
  }

  @GetMapping("/companies")
  public ApiResponse<List<OptionItem>> companies() {
    return ApiResponse.ok(service.companies());
  }

  @GetMapping("/departments")
  public ApiResponse<List<OptionItem>> departments(@RequestParam(required = false) Long companyOrgId) {
    return ApiResponse.ok(service.departments(companyOrgId));
  }

  @GetMapping("/teams")
  public ApiResponse<List<OptionItem>> teams(@RequestParam(required = false) Long parentOrgId) {
    return ApiResponse.ok(service.teams(parentOrgId));
  }
}
