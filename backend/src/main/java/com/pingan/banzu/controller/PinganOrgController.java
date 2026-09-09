package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.dto.OrgNodeResponse;
import com.pingan.banzu.service.OrgService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pingan/org")
public class PinganOrgController {

  private final OrgService orgService;

  public PinganOrgController(OrgService orgService) {
    this.orgService = orgService;
  }

  @GetMapping("/tree")
  public ApiResponse<List<OrgNodeResponse>> tree() {
    return ApiResponse.ok(orgService.tree());
  }

  @GetMapping("/company-tree")
  public ApiResponse<List<OrgNodeResponse>> companyTree() {
    return ApiResponse.ok(orgService.companyTree());
  }
}
