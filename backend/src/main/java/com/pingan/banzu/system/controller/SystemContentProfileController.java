package com.pingan.banzu.system.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.system.dto.SystemContentProfileQuery;
import com.pingan.banzu.system.dto.SystemContentProfileRequest;
import com.pingan.banzu.system.dto.SystemContentProfileResponse;
import com.pingan.banzu.system.service.SystemContentProfileService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/system/content-profiles")
public class SystemContentProfileController {

  private final SystemContentProfileService service;

  public SystemContentProfileController(SystemContentProfileService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResult<SystemContentProfileResponse>> list(@ModelAttribute SystemContentProfileQuery query) {
    return ApiResponse.ok(service.list(query));
  }

  @PostMapping
  public ApiResponse<SystemContentProfileResponse> create(@Valid @RequestBody SystemContentProfileRequest request) {
    return ApiResponse.ok(service.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<SystemContentProfileResponse> update(
      @PathVariable Long id, @Valid @RequestBody SystemContentProfileRequest request) {
    return ApiResponse.ok(service.update(id, request));
  }

  @PatchMapping("/{id}/status")
  public ApiResponse<SystemContentProfileResponse> status(@PathVariable Long id, @RequestBody Map<String, String> request) {
    return ApiResponse.ok(service.status(id, request.get("status")));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/image")
  public ApiResponse<SystemContentProfileResponse> uploadImage(
      @PathVariable Long id, @RequestPart("file") MultipartFile file) {
    return ApiResponse.ok(service.uploadImage(id, file));
  }

  @DeleteMapping("/{id}/image")
  public ApiResponse<SystemContentProfileResponse> removeImage(@PathVariable Long id) {
    return ApiResponse.ok(service.removeImage(id));
  }

  @PostMapping("/{id}/video")
  public ApiResponse<SystemContentProfileResponse> uploadVideo(
      @PathVariable Long id, @RequestPart("file") MultipartFile file) {
    return ApiResponse.ok(service.uploadVideo(id, file));
  }

  @DeleteMapping("/{id}/video")
  public ApiResponse<SystemContentProfileResponse> removeVideo(@PathVariable Long id) {
    return ApiResponse.ok(service.removeVideo(id));
  }
}
