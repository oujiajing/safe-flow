package com.pingan.banzu.system.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.system.dto.SystemCompanyQuery;
import com.pingan.banzu.system.dto.SystemCompanyRequest;
import com.pingan.banzu.system.dto.SystemCompanyResponse;
import com.pingan.banzu.system.service.SystemCompanyService;
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
@RequestMapping("/api/system/companies")
public class SystemCompanyController {
  private final SystemCompanyService service;

  public SystemCompanyController(SystemCompanyService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResult<SystemCompanyResponse>> list(@ModelAttribute SystemCompanyQuery query) {
    return ApiResponse.ok(service.list(query));
  }

  @PostMapping
  public ApiResponse<SystemCompanyResponse> create(@Valid @RequestBody SystemCompanyRequest request) {
    return ApiResponse.ok(service.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<SystemCompanyResponse> update(@PathVariable Long id, @Valid @RequestBody SystemCompanyRequest request) {
    return ApiResponse.ok(service.update(id, request));
  }

  @PatchMapping("/{id}/status")
  public ApiResponse<SystemCompanyResponse> status(@PathVariable Long id, @RequestBody Map<String, String> request) {
    return ApiResponse.ok(service.status(id, request.get("status")));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }
}
