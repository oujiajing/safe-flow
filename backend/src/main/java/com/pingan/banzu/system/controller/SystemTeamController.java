package com.pingan.banzu.system.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.system.dto.SystemTeamQuery;
import com.pingan.banzu.system.dto.SystemTeamRequest;
import com.pingan.banzu.system.dto.SystemTeamResponse;
import com.pingan.banzu.system.service.SystemTeamService;
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
@RequestMapping("/api/system/teams")
public class SystemTeamController {
  private final SystemTeamService service;

  public SystemTeamController(SystemTeamService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResult<SystemTeamResponse>> list(@ModelAttribute SystemTeamQuery query) {
    return ApiResponse.ok(service.list(query));
  }

  @PostMapping
  public ApiResponse<SystemTeamResponse> create(@Valid @RequestBody SystemTeamRequest request) {
    return ApiResponse.ok(service.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<SystemTeamResponse> update(@PathVariable Long id, @Valid @RequestBody SystemTeamRequest request) {
    return ApiResponse.ok(service.update(id, request));
  }

  @PatchMapping("/{id}/status")
  public ApiResponse<SystemTeamResponse> status(@PathVariable Long id, @RequestBody Map<String, String> request) {
    return ApiResponse.ok(service.status(id, request.get("status")));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
