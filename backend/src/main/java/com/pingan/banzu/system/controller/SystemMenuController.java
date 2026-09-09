package com.pingan.banzu.system.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.system.dto.SystemMenuRequest;
import com.pingan.banzu.system.dto.SystemMenuResponse;
import com.pingan.banzu.system.service.SystemMenuService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/menus")
public class SystemMenuController {

  private final SystemMenuService menuService;

  public SystemMenuController(SystemMenuService menuService) {
    this.menuService = menuService;
  }

  @GetMapping("/tree")
  public ApiResponse<List<SystemMenuResponse>> tree() {
    return ApiResponse.ok(menuService.tree());
  }

  @GetMapping
  public ApiResponse<List<SystemMenuResponse>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status) {
    return ApiResponse.ok(menuService.list(keyword, status));
  }

  @PostMapping
  public ApiResponse<SystemMenuResponse> create(@Valid @RequestBody SystemMenuRequest request) {
    return ApiResponse.ok(menuService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<SystemMenuResponse> update(@PathVariable Long id, @Valid @RequestBody SystemMenuRequest request) {
    return ApiResponse.ok(menuService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    menuService.delete(id);
    return ApiResponse.ok();
  }
}
