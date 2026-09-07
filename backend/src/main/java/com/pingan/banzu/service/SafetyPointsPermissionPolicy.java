package com.pingan.banzu.service;

import com.pingan.banzu.system.security.SystemPermissionService;
import org.springframework.stereotype.Service;

@Service
public class SafetyPointsPermissionPolicy {

  private static final String LEGACY_POINTS_VIEW = "PINGAN_POINTS_VIEW";
  private static final String LEGACY_POINTS_MANAGE = "PINGAN_POINTS_MANAGE";

  private final SystemPermissionService permissionService;

  public SafetyPointsPermissionPolicy(SystemPermissionService permissionService) {
    this.permissionService = permissionService;
  }

  public void assertCanViewFlow() {
    permissionService.assertHasAnyPermission("PINGAN_POINTS_FLOW_VIEW", LEGACY_POINTS_VIEW, LEGACY_POINTS_MANAGE);
  }

  public void assertCanCreateFlow() {
    permissionService.assertHasAnyPermission("PINGAN_POINTS_FLOW_CREATE", LEGACY_POINTS_MANAGE);
  }

  public void assertCanDeleteFlow() {
    permissionService.assertHasAnyPermission("PINGAN_POINTS_FLOW_DELETE", LEGACY_POINTS_MANAGE);
  }

  public void assertCanViewRanking() {
    permissionService.assertHasAnyPermission("PINGAN_POINTS_RANKING_VIEW", LEGACY_POINTS_VIEW, LEGACY_POINTS_MANAGE);
  }
}
