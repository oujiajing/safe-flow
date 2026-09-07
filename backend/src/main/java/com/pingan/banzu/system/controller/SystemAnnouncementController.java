package com.pingan.banzu.system.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.AnnouncementRequest;
import com.pingan.banzu.dto.AnnouncementResponse;
import com.pingan.banzu.dto.NotificationDeliveryResponse;
import com.pingan.banzu.system.service.SystemAnnouncementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemAnnouncementController {

  private final SystemAnnouncementService service;

  public SystemAnnouncementController(SystemAnnouncementService service) {
    this.service = service;
  }

  @GetMapping("/announcements")
  public ApiResponse<PageResult<AnnouncementResponse>> list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return ApiResponse.ok(service.list(status, page, pageSize));
  }

  @GetMapping("/announcements/{id}")
  public ApiResponse<AnnouncementResponse> get(@PathVariable Long id) {
    return ApiResponse.ok(service.get(id));
  }

  @PostMapping("/announcements")
  public ApiResponse<AnnouncementResponse> create(@RequestBody AnnouncementRequest request) {
    return ApiResponse.ok(service.create(request));
  }

  @PutMapping("/announcements/{id}")
  public ApiResponse<AnnouncementResponse> update(
      @PathVariable Long id, @RequestBody AnnouncementRequest request) {
    return ApiResponse.ok(service.update(id, request));
  }

  @PostMapping("/announcements/{id}/publish")
  public ApiResponse<AnnouncementResponse> publish(@PathVariable Long id) {
    return ApiResponse.ok(service.publish(id));
  }

  @PostMapping("/announcements/{id}/withdraw")
  public ApiResponse<AnnouncementResponse> withdraw(@PathVariable Long id) {
    return ApiResponse.ok(service.withdraw(id));
  }

  @GetMapping("/notification-deliveries")
  public ApiResponse<PageResult<NotificationDeliveryResponse>> deliveries(
      @RequestParam Long announcementId,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return ApiResponse.ok(service.deliveries(announcementId, page, pageSize));
  }
}

