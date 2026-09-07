package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.RiskFourColorMapRequest;
import com.pingan.banzu.dto.RiskFourColorMapResponse;
import com.pingan.banzu.service.RiskFourColorMapService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pingan/risk-four-color-maps")
public class RiskFourColorMapController {

  private final RiskFourColorMapService service;

  public RiskFourColorMapController(RiskFourColorMapService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResult<RiskFourColorMapResponse>> list(
      @RequestParam(required = false) Long companyId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return ApiResponse.ok(service.list(companyId, keyword, page, pageSize));
  }

  @PostMapping
  public ApiResponse<RiskFourColorMapResponse> create(@RequestBody RiskFourColorMapRequest request) {
    return ApiResponse.ok(service.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<RiskFourColorMapResponse> update(
      @PathVariable Long id, @RequestBody RiskFourColorMapRequest request) {
    return ApiResponse.ok(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok(null);
  }

  @PostMapping("/{id}/background")
  public ApiResponse<RiskFourColorMapResponse> uploadBackground(
      @PathVariable Long id, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.uploadBackground(id, file));
  }
}
