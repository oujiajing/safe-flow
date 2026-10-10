package com.pingan.banzu.service;

import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.system.security.SystemPermissionService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class HazardPermissionPolicy {

  private static final String LEGACY_HAZARD_VIEW = "PINGAN_HAZARD_VIEW";
  private static final String LEGACY_HAZARD_REPORT = "PINGAN_HAZARD_REPORT";
  private static final String LEGACY_HAZARD_RECTIFICATION = "PINGAN_HAZARD_RECTIFICATION";
  private static final String LEGACY_HAZARD_ACCEPT = "PINGAN_HAZARD_ACCEPT";
  private static final String LEGACY_HAZARD_CLOSE = "PINGAN_HAZARD_CLOSE";

  private static final Map<String, String> MODULE_PREFIXES =
      Map.of(
          "safety-check", "PINGAN_HAZARD_SAFETY_CHECK",
          "hazard-rectification", "PINGAN_HAZARD_RECTIFICATION",
          "quick-shot", "PINGAN_HAZARD_QUICK_SHOT",
          "curtain-wall-penalty", "PINGAN_HAZARD_CURTAIN_WALL_PENALTY",
          "curtain-wall-routine-check", "PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK");

  private final SystemPermissionService permissionService;

  public HazardPermissionPolicy(SystemPermissionService permissionService) {
    this.permissionService = permissionService;
  }

  public void assertCanView(String moduleKey) {
    permissionService.assertHasAnyPermission(permission(moduleKey, "VIEW"), LEGACY_HAZARD_VIEW);
  }

  public void assertCanCreateOrUpdateRecord(String moduleKey) {
    if ("quick-shot".equals(moduleKey)) {
      permissionService.assertHasAnyPermission(
          permission(moduleKey, "REPORT"), LEGACY_HAZARD_REPORT);
      return;
    }
    permissionService.assertHasPermission(permission(moduleKey, "CREATE"));
  }

  public void assertCanSubmitRecord(String moduleKey) {
    if ("quick-shot".equals(moduleKey) || "hazard-rectification".equals(moduleKey)) {
      permissionService.assertHasAnyPermission(
          permission(moduleKey, "REPORT"), LEGACY_HAZARD_REPORT);
      return;
    }
    permissionService.assertHasPermission(permission(moduleKey, "CREATE"));
  }

  public void assertCanDeleteRecord(String moduleKey) {
    permissionService.assertHasPermission(permission(moduleKey, "DELETE"));
  }

  public void assertCanVoidRecord(String moduleKey) {
    permissionService.assertHasAnyPermission(permission(moduleKey, "VOID"), LEGACY_HAZARD_CLOSE);
  }

  public void assertCanManageRecordAttachment(String moduleKey) {
    if ("quick-shot".equals(moduleKey)) {
      permissionService.assertHasAnyPermission(
          permission(moduleKey, "REPORT"), LEGACY_HAZARD_REPORT);
      return;
    }
    permissionService.assertHasPermission(permission(moduleKey, "CREATE"));
  }

  public void assertCanReviewQuickShot() {
    permissionService.assertHasAnyPermission(
        permission("quick-shot", "REVIEW"), LEGACY_HAZARD_ACCEPT);
  }

  /** Agent runs are initiated by the reporter, but never transition the business workflow. */
  public void assertCanStartQuickShotAgentRun() {
    permissionService.assertHasAnyPermission(
        permission("quick-shot", "REPORT"), LEGACY_HAZARD_REPORT);
  }

  public void assertCanRectifyOrder() {
    permissionService.assertHasAnyPermission(
        permission("hazard-rectification", "RECTIFY"), LEGACY_HAZARD_RECTIFICATION);
  }

  public void assertCanAcceptOrder() {
    permissionService.assertHasAnyPermission(
        permission("hazard-rectification", "ACCEPT"), LEGACY_HAZARD_ACCEPT);
  }

  public void assertCanCreateOrder() {
    permissionService.assertHasAnyPermission(
        permission("hazard-rectification", "CREATE"), LEGACY_HAZARD_REPORT);
  }

  public void assertCanDeleteOrder() {
    permissionService.assertHasAnyPermission(
        permission("hazard-rectification", "DELETE"), LEGACY_HAZARD_RECTIFICATION);
  }

  public void assertCanVoidOrder() {
    permissionService.assertHasAnyPermission(
        permission("hazard-rectification", "VOID"), LEGACY_HAZARD_CLOSE);
  }

  public boolean canCreateOrUpdateRecord(String moduleKey) {
    return "quick-shot".equals(moduleKey)
        ? permissionService.hasAnyPermission(permission(moduleKey, "REPORT"), LEGACY_HAZARD_REPORT)
        : permissionService.hasPermission(permission(moduleKey, "CREATE"));
  }

  public boolean canSubmitRecord(String moduleKey) {
    return "quick-shot".equals(moduleKey) || "hazard-rectification".equals(moduleKey)
        ? permissionService.hasAnyPermission(permission(moduleKey, "REPORT"), LEGACY_HAZARD_REPORT)
        : permissionService.hasPermission(permission(moduleKey, "CREATE"));
  }

  public boolean canDeleteRecord(String moduleKey) {
    return permissionService.hasPermission(permission(moduleKey, "DELETE"));
  }

  public boolean canVoidRecord(String moduleKey) {
    return permissionService.hasAnyPermission(permission(moduleKey, "VOID"), LEGACY_HAZARD_CLOSE);
  }

  public boolean canReviewQuickShot() {
    return permissionService.hasAnyPermission(
        permission("quick-shot", "REVIEW"), LEGACY_HAZARD_ACCEPT);
  }

  public boolean hasQuickShotPrivilegedView() {
    return permissionService.hasAnyPermission(
        permission("quick-shot", "REVIEW"),
        permission("quick-shot", "DELETE"),
        permission("quick-shot", "VOID"),
        permission("quick-shot", "EXPORT"),
        LEGACY_HAZARD_ACCEPT,
        LEGACY_HAZARD_CLOSE);
  }

  private String permission(String moduleKey, String action) {
    String prefix = MODULE_PREFIXES.get(moduleKey);
    if (prefix == null) {
      throw new BusinessException("隐患排查模块不支持：" + moduleKey);
    }
    return prefix + "_" + action;
  }
}
