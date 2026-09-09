package com.pingan.banzu.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.domain.SysUser;
import com.pingan.banzu.dto.UserOptionResponse;
import com.pingan.banzu.mapper.SysUserMapper;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.security.SystemDataScopeService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pingan/users")
public class PinganUserController {

  private final SysUserMapper userMapper;
  private final SystemDataScopeService dataScopeService;

  public PinganUserController(SysUserMapper userMapper, SystemDataScopeService dataScopeService) {
    this.userMapper = userMapper;
    this.dataScopeService = dataScopeService;
  }

  @GetMapping
  public ApiResponse<List<UserOptionResponse>> list() {
    CurrentUserContext.require();
    List<Long> accessibleOrgIds = dataScopeService.accessibleOrgIds();
    List<UserOptionResponse> users =
        userMapper
            .selectList(
                new QueryWrapper<SysUser>()
                    .eq("status", "ACTIVE")
                    .eq("deleted", 0)
                    .in(accessibleOrgIds != null && !accessibleOrgIds.isEmpty(), "org_id", accessibleOrgIds)
                    .eq(accessibleOrgIds == null || accessibleOrgIds.isEmpty(), "org_id", -1L)
                    .orderByAsc("id"))
            .stream()
            .map(user -> new UserOptionResponse(user.id, user.username, user.realName, user.orgId))
            .toList();
    return ApiResponse.ok(users);
  }
}
