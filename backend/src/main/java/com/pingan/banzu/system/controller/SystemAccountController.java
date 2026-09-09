package com.pingan.banzu.system.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.system.dto.SystemAccountQuery;
import com.pingan.banzu.system.dto.SystemAccountRequest;
import com.pingan.banzu.system.dto.SystemAccountResponse;
import com.pingan.banzu.system.dto.SystemAccountRoleRequest;
import com.pingan.banzu.system.dto.SystemResetPasswordRequest;
import com.pingan.banzu.system.service.SystemAccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/accounts")
public class SystemAccountController {

  private final SystemAccountService accountService;

  public SystemAccountController(SystemAccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping
  public ApiResponse<PageResult<SystemAccountResponse>> list(@ModelAttribute SystemAccountQuery query) {
    return ApiResponse.ok(accountService.list(query));
  }

  @PostMapping
  public ApiResponse<SystemAccountResponse> create(@Valid @RequestBody SystemAccountRequest request) {
    return ApiResponse.ok(accountService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<SystemAccountResponse> update(
      @PathVariable Long id, @RequestBody SystemAccountRequest request) {
    return ApiResponse.ok(accountService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    accountService.delete(id);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/freeze")
  public ApiResponse<SystemAccountResponse> freeze(@PathVariable Long id) {
    return ApiResponse.ok(accountService.freeze(id));
  }

  @PostMapping("/{id}/unfreeze")
  public ApiResponse<SystemAccountResponse> unfreeze(@PathVariable Long id) {
    return ApiResponse.ok(accountService.unfreeze(id));
  }

  @PostMapping("/{id}/reset-password")
  public ApiResponse<SystemAccountResponse> resetPassword(
      @PathVariable Long id, @Valid @RequestBody SystemResetPasswordRequest request) {
    return ApiResponse.ok(accountService.resetPassword(id, request.password()));
  }

  @PutMapping("/{id}/roles")
  public ApiResponse<SystemAccountResponse> updateRoles(
      @PathVariable Long id, @RequestBody SystemAccountRoleRequest request) {
    return ApiResponse.ok(accountService.updateRoles(id, request.roleIds()));
  }
}
