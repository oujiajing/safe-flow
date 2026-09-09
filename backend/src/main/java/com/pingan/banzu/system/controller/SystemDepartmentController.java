package com.pingan.banzu.system.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.system.dto.SystemDepartmentQuery;
import com.pingan.banzu.system.dto.SystemDepartmentRequest;
import com.pingan.banzu.system.dto.SystemDepartmentResponse;
import com.pingan.banzu.system.service.SystemDepartmentService;
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
@RequestMapping("/api/system/departments")
public class SystemDepartmentController {
  private final SystemDepartmentService service;

  public SystemDepartmentController(SystemDepartmentService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResult<SystemDepartmentResponse>> list(@ModelAttribute SystemDepartmentQuery query) {
    return ApiResponse.ok(service.list(query));
  }

  @PostMapping
  public ApiResponse<SystemDepartmentResponse> create(@Valid @RequestBody SystemDepartmentRequest request) {
    return ApiResponse.ok(service.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<SystemDepartmentResponse> update(@PathVariable Long id, @Valid @RequestBody SystemDepartmentRequest request) {
    return ApiResponse.ok(service.update(id, request));
  }

  @PatchMapping("/{id}/status")
  public ApiResponse<SystemDepartmentResponse> status(@PathVariable Long id, @RequestBody Map<String, String> request) {
    return ApiResponse.ok(service.status(id, request.get("status")));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
