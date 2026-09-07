package com.pingan.banzu.system.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.system.dto.SystemRoleMenuRequest;
import com.pingan.banzu.system.dto.SystemRoleRequest;
import com.pingan.banzu.system.dto.SystemRoleResponse;
import com.pingan.banzu.system.service.SystemRoleService;
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
@RequestMapping("/api/system/roles")
public class SystemRoleController {

  private final SystemRoleService roleService;

  public SystemRoleController(SystemRoleService roleService) {
    this.roleService = roleService;
  }

  @GetMapping
  public ApiResponse<List<SystemRoleResponse>> list(@RequestParam(required = false) String keyword) {
    return ApiResponse.ok(roleService.list(keyword));
  }

  @PostMapping
  public ApiResponse<SystemRoleResponse> create(@Valid @RequestBody SystemRoleRequest request) {
    return ApiResponse.ok(roleService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<SystemRoleResponse> update(@PathVariable Long id, @Valid @RequestBody SystemRoleRequest request) {
    return ApiResponse.ok(roleService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    roleService.delete(id);
    return ApiResponse.ok();
  }

  @GetMapping("/{id}/menus")
  public ApiResponse<List<Long>> menus(@PathVariable Long id) {
    return ApiResponse.ok(roleService.menuIds(id));
  }

  @PutMapping("/{id}/menus")
  public ApiResponse<List<Long>> updateMenus(@PathVariable Long id, @RequestBody SystemRoleMenuRequest request) {
    return ApiResponse.ok(roleService.updateMenus(id, request.menuIds()));
  }
}
