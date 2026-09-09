package com.pingan.banzu.service;

import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.system.security.SystemPermissionService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ThreeCheckPermissionPolicy {

  private static final Map<String, String> MODULE_PREFIXES =
      Map.of(
          "team-dispatch", "PINGAN_TEAM_DISPATCH",
          "curtain-wall-team-dispatch", "PINGAN_CURTAIN_WALL_TEAM_DISPATCH",
          "pre-shift-meeting", "PINGAN_PRE_SHIFT_MEETING",
          "pre-shift-inspection", "PINGAN_ONE_SHIFT_THREE_CHECKS",
          "mid-shift-inspection", "PINGAN_ONE_SHIFT_THREE_CHECKS",
          "post-shift-inspection", "PINGAN_ONE_SHIFT_THREE_CHECKS",
          "pre-shift-safety-activity", "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY",
          "key-sites", "PINGAN_KEY_SITES");

  private final SystemPermissionService permissionService;

  public ThreeCheckPermissionPolicy(SystemPermissionService permissionService) {
    this.permissionService = permissionService;
  }

  public void assertCanView(String moduleKey) {
    permissionService.assertHasPermission(permission(moduleKey, "VIEW"));
  }

  public void assertCanCreate(String moduleKey) {
    permissionService.assertHasPermission(permission(moduleKey, "CREATE"));
  }

  public void assertCanUpdate(String moduleKey) {
    permissionService.assertHasPermission(permission(moduleKey, "UPDATE"));
  }

  public void assertCanSubmit(String moduleKey) {
    permissionService.assertHasPermission(permission(moduleKey, "SUBMIT"));
  }

  public void assertCanDelete(String moduleKey) {
    permissionService.assertHasPermission(permission(moduleKey, "DELETE"));
  }

  public void assertCanVoid(String moduleKey) {
    permissionService.assertHasPermission(permission(moduleKey, "VOID"));
  }

  public void assertCanRemind(String moduleKey) {
    permissionService.assertHasPermission(permission(moduleKey, "REMIND"));
  }

  public boolean canSubmit(String moduleKey) {
    return permissionService.hasPermission(permission(moduleKey, "SUBMIT"));
  }

  public boolean canVoid(String moduleKey) {
    return permissionService.hasPermission(permission(moduleKey, "VOID"));
  }

  public boolean canRemind(String moduleKey) {
    return permissionService.hasPermission(permission(moduleKey, "REMIND"));
  }

  public boolean canDelete(String moduleKey) {
    return permissionService.hasPermission(permission(moduleKey, "DELETE"));
  }

  public void assertCanCreateRectificationOrder() {
    permissionService.assertHasPermission(
        "PINGAN_ONE_SHIFT_THREE_CHECKS_RECTIFICATION_ORDER_CREATE");
  }

  public boolean canCreateRectificationOrder() {
    return permissionService.hasPermission(
        "PINGAN_ONE_SHIFT_THREE_CHECKS_RECTIFICATION_ORDER_CREATE");
  }

  private String permission(String moduleKey, String action) {
    String prefix = MODULE_PREFIXES.get(moduleKey);
    if (prefix == null) {
      throw new BusinessException("一班三查模块不支持：" + moduleKey);
    }
    return prefix + "_" + action;
  }
}
