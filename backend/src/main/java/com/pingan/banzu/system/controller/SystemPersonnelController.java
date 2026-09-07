package com.pingan.banzu.system.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.system.dto.SystemPersonnelQuery;
import com.pingan.banzu.system.dto.SystemPersonnelRequest;
import com.pingan.banzu.system.dto.SystemPersonnelResponse;
import com.pingan.banzu.system.service.SystemPersonnelService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/personnel")
public class SystemPersonnelController {
  private final SystemPersonnelService service;

  public SystemPersonnelController(SystemPersonnelService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResult<SystemPersonnelResponse>> list(@ModelAttribute SystemPersonnelQuery query) {
    return ApiResponse.ok(service.list(query));
  }

  @PostMapping
  public ApiResponse<SystemPersonnelResponse> create(@Valid @RequestBody SystemPersonnelRequest request) {
    return ApiResponse.ok(service.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<SystemPersonnelResponse> update(@PathVariable Long id, @Valid @RequestBody SystemPersonnelRequest request) {
    return ApiResponse.ok(service.update(id, request));
  }

  @PatchMapping("/{id}/status")
  public ApiResponse<SystemPersonnelResponse> status(@PathVariable Long id, @RequestBody Map<String, String> request) {
    return ApiResponse.ok(service.status(id, request.get("status")));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
