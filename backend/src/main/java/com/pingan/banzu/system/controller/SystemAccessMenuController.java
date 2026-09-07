package com.pingan.banzu.system.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.system.service.SystemMenuService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menu")
public class SystemAccessMenuController {

  private final SystemMenuService menuService;

  public SystemAccessMenuController(SystemMenuService menuService) {
    this.menuService = menuService;
  }

  @GetMapping("/all")
  public ApiResponse<List<Map<String, Object>>> all() {
    return ApiResponse.ok(menuService.vbenRouteTree());
  }
}
