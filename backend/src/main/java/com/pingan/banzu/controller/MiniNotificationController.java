package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.NotificationListItem;
import com.pingan.banzu.dto.NotificationQuery;
import com.pingan.banzu.dto.NotificationRouteResolution;
import com.pingan.banzu.dto.NotificationUnreadCounts;
import com.pingan.banzu.service.NotificationService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini/notifications")
public class MiniNotificationController {

  private final NotificationService service;

  public MiniNotificationController(NotificationService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResult<NotificationListItem>> list(@ModelAttribute NotificationQuery query) {
    return ApiResponse.ok(service.list(query));
  }

  @GetMapping("/unread-counts")
  public ApiResponse<NotificationUnreadCounts> unreadCounts() {
    return ApiResponse.ok(service.unreadCounts());
  }

  @GetMapping("/{id}")
  public ApiResponse<NotificationListItem> detail(@PathVariable Long id) {
    return ApiResponse.ok(service.detail(id));
  }

  @PostMapping("/{id}/read")
  public ApiResponse<Void> read(@PathVariable Long id) {
    service.markRead(id);
    return ApiResponse.ok(null);
  }

  @PostMapping("/{id}/unread")
  public ApiResponse<Void> unread(@PathVariable Long id) {
    service.markUnread(id);
    return ApiResponse.ok(null);
  }

  @PostMapping("/{id}/resolve-action")
  public ApiResponse<NotificationRouteResolution> resolveAction(@PathVariable Long id) {
    return ApiResponse.ok(service.resolveAction(id));
  }

  @PostMapping("/read-all")
  public ApiResponse<Integer> readAll(
      @RequestParam(required = false) String groupType,
      @RequestParam(required = false) String moduleKey,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateStart,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEnd) {
    return ApiResponse.ok(service.markAllRead(groupType, moduleKey, dateStart, dateEnd));
  }
}
