package com.pingan.banzu.system.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.system.dto.SystemCheckItemLibraryRequest;
import com.pingan.banzu.system.dto.SystemCheckItemLibraryResponse;
import com.pingan.banzu.system.dto.SystemTeamCheckTemplateQuery;
import com.pingan.banzu.system.dto.SystemTeamCheckTemplateRequest;
import com.pingan.banzu.system.dto.SystemTeamCheckTemplateResponse;
import com.pingan.banzu.system.service.SystemTeamCheckTemplateService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/team-check-item-templates")
public class SystemTeamCheckTemplateController {

  private final SystemTeamCheckTemplateService service;

  public SystemTeamCheckTemplateController(SystemTeamCheckTemplateService service) {
    this.service = service;
  }

  @GetMapping("/library")
  public ApiResponse<PageResult<SystemCheckItemLibraryResponse>> listLibrary(
      @RequestParam(required = false) String stage,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return ApiResponse.ok(service.listLibrary(stage, status, keyword, page, pageSize));
  }

  @PostMapping("/library")
  public ApiResponse<SystemCheckItemLibraryResponse> createLibrary(
      @Valid @RequestBody SystemCheckItemLibraryRequest request) {
    return ApiResponse.ok(service.createLibrary(request));
  }

  @PutMapping("/library/{id}")
  public ApiResponse<SystemCheckItemLibraryResponse> updateLibrary(
      @PathVariable Long id, @Valid @RequestBody SystemCheckItemLibraryRequest request) {
    return ApiResponse.ok(service.updateLibrary(id, request));
  }

  @PatchMapping("/library/{id}/status")
  public ApiResponse<SystemCheckItemLibraryResponse> libraryStatus(
      @PathVariable Long id, @RequestBody Map<String, String> request) {
    return ApiResponse.ok(service.libraryStatus(id, request.get("status")));
  }

  @DeleteMapping("/library/{id}")
  public ApiResponse<Void> deleteLibrary(@PathVariable Long id) {
    service.deleteLibrary(id);
    return ApiResponse.ok();
  }

  @GetMapping("/templates")
  public ApiResponse<PageResult<SystemTeamCheckTemplateResponse>> list(@ModelAttribute SystemTeamCheckTemplateQuery query) {
    return ApiResponse.ok(service.list(query));
  }

  @PostMapping("/templates")
  public ApiResponse<SystemTeamCheckTemplateResponse> create(
      @Valid @RequestBody SystemTeamCheckTemplateRequest request) {
    return ApiResponse.ok(service.create(request));
  }

  @GetMapping("/templates/{id}")
  public ApiResponse<SystemTeamCheckTemplateResponse> detail(@PathVariable Long id) {
    return ApiResponse.ok(service.detail(id));
  }

  @PutMapping("/templates/{id}")
  public ApiResponse<SystemTeamCheckTemplateResponse> update(
      @PathVariable Long id, @Valid @RequestBody SystemTeamCheckTemplateRequest request) {
    return ApiResponse.ok(service.update(id, request));
  }

  @PatchMapping("/templates/{id}/status")
  public ApiResponse<SystemTeamCheckTemplateResponse> status(
      @PathVariable Long id, @RequestBody Map<String, String> request) {
    return ApiResponse.ok(service.status(id, request.get("status")));
  }

  @DeleteMapping("/templates/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  @PostMapping("/templates/{id}/copy")
  public ApiResponse<SystemTeamCheckTemplateResponse> copy(
      @PathVariable Long id, @Valid @RequestBody SystemTeamCheckTemplateRequest request) {
    return ApiResponse.ok(service.copy(id, request));
  }

  @GetMapping("/resolve")
  public ApiResponse<SystemTeamCheckTemplateResponse> resolve(
      @RequestParam Long companyOrgId,
      @RequestParam(required = false) Long departmentOrgId,
      @RequestParam(required = false) Long teamOrgId,
      @RequestParam String stage) {
    return ApiResponse.ok(service.resolve(companyOrgId, departmentOrgId, teamOrgId, stage));
  }
}
