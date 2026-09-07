package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.security.SystemDataScopeService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class SystemDataScopeTest {

  @Autowired private SystemDataScopeService dataScopeService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void clearUser() {
    CurrentUserContext.clear();
  }

  @Test
  void adminCanAccessAllOrganizations() {
    CurrentUserContext.set(new CurrentUser(1L, "admin", "系统管理员", 1L, "/1/", List.of("ADMIN")));

    assertThat(dataScopeService.canAccessOrg(4L)).isTrue();
    assertThat(dataScopeService.canAccessOrg(8L)).isTrue();
    assertThat(dataScopeService.accessibleOrgIds()).contains(1L, 4L, 8L, 32L);
  }

  @Test
  void orgAndChildrenCanAccessOwnOrgAndDescendantsButNotSibling() {
    CurrentUserContext.set(new CurrentUser(3L, "MQ_SAFE", "幕墙安全员", 4L, "/1/2/3/4/", List.of("SAFETY_OFFICER")));

    assertThat(dataScopeService.canAccessOrg(4L)).isTrue();
    assertThat(dataScopeService.canAccessOrg(8L)).isFalse();
    assertThat(dataScopeService.accessibleOrgIds()).contains(4L).doesNotContain(8L);
    assertThatThrownBy(() -> dataScopeService.assertCanAccessOrg(8L))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("无权访问");
  }

  @Test
  void selfScopeAllowsOnlyCurrentUsersOrganization() {
    jdbcTemplate.update(
        "insert into sys_role (id, role_code, role_name, data_scope) values (9001, 'SELF_ONLY_TEST', '仅本人测试', 'SELF')");
    jdbcTemplate.update(
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (9001, 'self_scope_user', '{noop}123456', '仅本人用户', 4, 'ACTIVE', 0)");
    jdbcTemplate.update("insert into sys_user_role (user_id, role_id) values (9001, 9001)");

    CurrentUserContext.set(
        new CurrentUser(9001L, "self_scope_user", "仅本人用户", 4L, "/1/2/3/4/", List.of("SELF_ONLY_TEST")));

    assertThat(dataScopeService.canAccessOrg(4L)).isTrue();
    assertThat(dataScopeService.canAccessOrg(8L)).isFalse();
    assertThat(dataScopeService.accessibleOrgIds()).containsExactly(4L);
  }

  @Test
  void applyOrgScopeAddsAccessibleOrgCondition() {
    CurrentUserContext.set(new CurrentUser(3L, "MQ_SAFE", "幕墙安全员", 4L, "/1/2/3/4/", List.of("SAFETY_OFFICER")));

    QueryWrapper<Object> wrapper = new QueryWrapper<>();
    dataScopeService.applyOrgScope(wrapper, "org_id");

    assertThat(wrapper.getSqlSegment()).contains("org_id");
  }

  @Test
  void threeCheckRolePermissionSeedsExistWithCurtainWallScopedAndGroupGlobalLeaders() {
    assertThat(roleSeedFor("CURTAIN_WALL_LEADER"))
        .containsEntry("ROLE_NAME", "幕墙领导")
        .containsEntry("DATA_SCOPE", "ORG_AND_CHILDREN");
    assertThat(roleSeedFor("GROUP_LEADER"))
        .containsEntry("ROLE_NAME", "集团领导")
        .containsEntry("DATA_SCOPE", "ALL");
    assertThat(hiddenPermissionCodesUnderPinganPermissionTree())
        .contains(
            "PINGAN_TEAM_DISPATCH_ENTRY",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_ENTRY",
            "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_ENTRY",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_ENTRY",
            "PINGAN_KEY_SITES_ENTRY")
        .doesNotContain(
            "PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW",
            "PINGAN_GROUP_THREE_CHECK_VIEW");

    assertRoleMatrix(
        "COMPANY_LEADER",
        "ORG_AND_CHILDREN",
        List.of(
            "PINGAN_MONITOR_CENTER_ENTRY",
            "PINGAN_MONITOR_CENTER_VIEW",
            "PINGAN_THREE_CHECK_ENTRY",
            "PINGAN_TEAM_DISPATCH_VIEW",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
            "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VIEW",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW",
            "PINGAN_KEY_SITES_VIEW",
            "PINGAN_HAZARD_ENTRY",
            "PINGAN_HAZARD_VIEW",
            "PINGAN_RISK_ENTRY",
            "PINGAN_RISK_VIEW",
            "PINGAN_SPECIAL_WORK_ENTRY",
            "PINGAN_SPECIAL_WORK_VIEW",
            "PINGAN_TRAINING_ENTRY",
            "PINGAN_TRAINING_VIEW",
            "PINGAN_POINTS_ENTRY",
            "PINGAN_POINTS_VIEW",
            "PINGAN_LEDGER_ENTRY",
            "PINGAN_LEDGER_VIEW"),
        leaderExcludedMutationCodes());
    assertRoleMatrix(
        "ENTERPRISE_LEADER",
        "ORG_AND_CHILDREN",
        List.of(
            "PINGAN_MONITOR_CENTER_ENTRY",
            "PINGAN_MONITOR_CENTER_VIEW",
            "PINGAN_THREE_CHECK_ENTRY",
            "PINGAN_TEAM_DISPATCH_VIEW",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
            "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VIEW",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW",
            "PINGAN_KEY_SITES_VIEW",
            "PINGAN_HAZARD_ENTRY",
            "PINGAN_HAZARD_VIEW",
            "PINGAN_RISK_ENTRY",
            "PINGAN_RISK_VIEW",
            "PINGAN_SPECIAL_WORK_ENTRY",
            "PINGAN_SPECIAL_WORK_VIEW",
            "PINGAN_TRAINING_ENTRY",
            "PINGAN_TRAINING_VIEW",
            "PINGAN_POINTS_ENTRY",
            "PINGAN_POINTS_VIEW",
            "PINGAN_LEDGER_ENTRY",
            "PINGAN_LEDGER_VIEW"),
        leaderExcludedMutationCodes());
    assertRoleMatrix(
        "SAFETY_OFFICER",
        "ORG_AND_CHILDREN",
        List.of(
            "PINGAN_MONITOR_CENTER_ENTRY",
            "PINGAN_MONITOR_CENTER_VIEW",
            "PINGAN_THREE_CHECK_ENTRY",
            "PINGAN_TEAM_DISPATCH_VIEW",
            "PINGAN_TEAM_DISPATCH_REMIND",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_REMIND",
            "PINGAN_KEY_SITES_VIEW",
            "PINGAN_KEY_SITES_REMIND",
            "PINGAN_HAZARD_ENTRY",
            "PINGAN_HAZARD_VIEW",
            "PINGAN_HAZARD_RECTIFICATION",
            "PINGAN_HAZARD_ACCEPT",
            "PINGAN_HAZARD_CLOSE",
            "PINGAN_RISK_ENTRY",
            "PINGAN_RISK_VIEW",
            "PINGAN_SPECIAL_WORK_ENTRY",
            "PINGAN_SPECIAL_WORK_REVIEW",
            "PINGAN_TRAINING_ENTRY",
            "PINGAN_TRAINING_MANAGE"),
        List.of(
            "PINGAN_TEAM_DISPATCH_CREATE",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_CREATE",
            "PINGAN_KEY_SITES_CREATE"));
    assertRoleMatrix(
        "DEPARTMENT_MANAGER",
        "ORG_AND_CHILDREN",
        List.of(
            "PINGAN_MONITOR_CENTER_ENTRY",
            "PINGAN_MONITOR_CENTER_VIEW",
            "PINGAN_THREE_CHECK_ENTRY",
            "PINGAN_TEAM_DISPATCH_VIEW",
            "PINGAN_TEAM_DISPATCH_CREATE",
            "PINGAN_TEAM_DISPATCH_UPDATE",
            "PINGAN_TEAM_DISPATCH_SUBMIT",
            "PINGAN_TEAM_DISPATCH_VOID",
            "PINGAN_TEAM_DISPATCH_DELETE",
            "PINGAN_TEAM_DISPATCH_REMIND",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_REMIND",
            "PINGAN_KEY_SITES_VIEW",
            "PINGAN_KEY_SITES_REMIND",
            "PINGAN_HAZARD_ENTRY",
            "PINGAN_HAZARD_VIEW",
            "PINGAN_RISK_ENTRY",
            "PINGAN_RISK_VIEW",
            "PINGAN_SPECIAL_WORK_ENTRY",
            "PINGAN_SPECIAL_WORK_VIEW",
            "PINGAN_TRAINING_ENTRY",
            "PINGAN_TRAINING_VIEW",
            "PINGAN_POINTS_ENTRY",
            "PINGAN_POINTS_VIEW",
            "PINGAN_LEDGER_ENTRY",
            "PINGAN_LEDGER_VIEW",
            "PINGAN_DATABASE_ENTRY",
            "PINGAN_DATABASE_VIEW"),
        List.of(
            "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_UPDATE",
            "PINGAN_HAZARD_RECTIFICATION",
            "PINGAN_DATABASE_MANAGE"));
    assertRoleMatrix(
        "WORKSHOP_DIRECTOR",
        "ORG_AND_CHILDREN",
        List.of(
            "PINGAN_MONITOR_CENTER_ENTRY",
            "PINGAN_MONITOR_CENTER_VIEW",
            "PINGAN_THREE_CHECK_ENTRY",
            "PINGAN_TEAM_DISPATCH_VIEW",
            "PINGAN_TEAM_DISPATCH_CREATE",
            "PINGAN_TEAM_DISPATCH_UPDATE",
            "PINGAN_TEAM_DISPATCH_SUBMIT",
            "PINGAN_TEAM_DISPATCH_VOID",
            "PINGAN_TEAM_DISPATCH_DELETE",
            "PINGAN_TEAM_DISPATCH_REMIND",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VOID",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW",
            "PINGAN_KEY_SITES_VIEW",
            "PINGAN_HAZARD_ENTRY",
            "PINGAN_HAZARD_VIEW",
            "PINGAN_HAZARD_RECTIFICATION",
            "PINGAN_RISK_ENTRY",
            "PINGAN_RISK_VIEW"),
        List.of(
            "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_UPDATE",
            "PINGAN_DATABASE_MANAGE"));
    assertRoleMatrix(
        "TEAM_LEADER",
        "ORG_AND_CHILDREN",
        List.of(
            "PINGAN_THREE_CHECK_ENTRY",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_UPDATE",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VOID",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_CREATE",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_UPDATE",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_SUBMIT",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VOID",
            "PINGAN_KEY_SITES_VIEW",
            "PINGAN_KEY_SITES_CREATE",
            "PINGAN_KEY_SITES_UPDATE",
            "PINGAN_KEY_SITES_SUBMIT",
            "PINGAN_KEY_SITES_VOID",
            "PINGAN_HAZARD_ENTRY",
            "PINGAN_HAZARD_VIEW",
            "PINGAN_HAZARD_RECTIFICATION"),
        List.of("PINGAN_TEAM_DISPATCH_CREATE", "PINGAN_ONE_SHIFT_THREE_CHECKS_DELETE", "PINGAN_DATABASE_MANAGE"));
    assertRoleMatrix(
        "TEAM_MEMBER",
        "SELF",
        List.of(
            "PINGAN_THREE_CHECK_ENTRY",
            "PINGAN_TEAM_DISPATCH_VIEW",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW",
            "PINGAN_KEY_SITES_VIEW",
            "PINGAN_HAZARD_ENTRY",
            "PINGAN_HAZARD_VIEW",
            "PINGAN_HAZARD_REPORT",
            "PINGAN_RISK_ENTRY",
            "PINGAN_RISK_VIEW",
            "PINGAN_RISK_CONFIRM",
            "PINGAN_TRAINING_ENTRY",
            "PINGAN_TRAINING_VIEW",
            "PINGAN_TRAINING_STUDY_EXAM"),
        List.of(
            "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND",
            "PINGAN_TEAM_DISPATCH_CREATE",
            "PINGAN_HAZARD_RECTIFICATION",
            "PINGAN_DATABASE_MANAGE"));
    assertRoleMatrix(
        "CURTAIN_WALL_LEADER",
        "ORG_AND_CHILDREN",
        List.of(
            "PINGAN_MONITOR_CENTER_ENTRY",
            "PINGAN_MONITOR_CENTER_VIEW",
            "PINGAN_THREE_CHECK_ENTRY",
            "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VIEW",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
            "PINGAN_HAZARD_ENTRY",
            "PINGAN_HAZARD_VIEW",
            "PINGAN_RISK_ENTRY",
            "PINGAN_RISK_VIEW"),
        List.of(
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW",
            "PINGAN_KEY_SITES_VIEW",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE",
            "PINGAN_TEAM_DISPATCH_CREATE",
            "PINGAN_HAZARD_RECTIFICATION",
            "PINGAN_DATABASE_MANAGE"));
    assertRoleMatrix(
        "GROUP_LEADER",
        "ALL",
        List.of(
            "PINGAN_MONITOR_CENTER_ENTRY",
            "PINGAN_MONITOR_CENTER_VIEW",
            "PINGAN_MONITOR_CENTER_GLOBAL_VIEW",
            "PINGAN_THREE_CHECK_ENTRY",
            "PINGAN_TEAM_DISPATCH_VIEW",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
            "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VIEW",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW",
            "PINGAN_KEY_SITES_VIEW",
            "PINGAN_HAZARD_ENTRY",
            "PINGAN_HAZARD_VIEW",
            "PINGAN_RISK_ENTRY",
            "PINGAN_RISK_VIEW",
            "PINGAN_SPECIAL_WORK_ENTRY",
            "PINGAN_SPECIAL_WORK_VIEW",
            "PINGAN_TRAINING_ENTRY",
            "PINGAN_TRAINING_VIEW",
            "PINGAN_POINTS_ENTRY",
            "PINGAN_POINTS_VIEW",
            "PINGAN_LEDGER_ENTRY",
            "PINGAN_LEDGER_VIEW",
            "PINGAN_DATABASE_ENTRY",
            "PINGAN_DATABASE_VIEW"),
        List.of(
            "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE",
            "PINGAN_TEAM_DISPATCH_CREATE",
            "PINGAN_HAZARD_RECTIFICATION",
            "PINGAN_DATABASE_MANAGE"));
  }

  private void assertRoleMatrix(
      String roleCode, String dataScope, List<String> expectedPermissionCodes, List<String> excludedPermissionCodes) {
    assertThat(roleSeedFor(roleCode)).containsEntry("DATA_SCOPE", dataScope);
    assertThat(permissionCodesForRole(roleCode))
        .containsAll(expectedPermissionCodes)
        .doesNotContainAnyElementsOf(excludedPermissionCodes);
  }

  private List<String> leaderExcludedMutationCodes() {
    return List.of(
        "PINGAN_TEAM_DISPATCH_CREATE",
        "PINGAN_TEAM_DISPATCH_UPDATE",
        "PINGAN_TEAM_DISPATCH_SUBMIT",
        "PINGAN_TEAM_DISPATCH_VOID",
        "PINGAN_TEAM_DISPATCH_DELETE",
        "PINGAN_TEAM_DISPATCH_REMIND",
        "PINGAN_TEAM_DISPATCH_EXPORT",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_UPDATE",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VOID",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_DELETE",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_EXPORT",
        "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_CREATE",
        "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_UPDATE",
        "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_SUBMIT",
        "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VOID",
        "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_DELETE",
        "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_REMIND",
        "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_EXPORT",
        "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_CREATE",
        "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_UPDATE",
        "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_SUBMIT",
        "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VOID",
        "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_DELETE",
        "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_REMIND",
        "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_EXPORT",
        "PINGAN_KEY_SITES_CREATE",
        "PINGAN_KEY_SITES_UPDATE",
        "PINGAN_KEY_SITES_SUBMIT",
        "PINGAN_KEY_SITES_VOID",
        "PINGAN_KEY_SITES_DELETE",
        "PINGAN_KEY_SITES_REMIND",
        "PINGAN_KEY_SITES_EXPORT",
        "PINGAN_HAZARD_REPORT",
        "PINGAN_HAZARD_RECTIFICATION",
        "PINGAN_HAZARD_ACCEPT",
        "PINGAN_HAZARD_CLOSE",
        "PINGAN_RISK_MANAGE",
        "PINGAN_RISK_CONFIRM",
        "PINGAN_SPECIAL_WORK_APPLY",
        "PINGAN_SPECIAL_WORK_APPROVE",
        "PINGAN_SPECIAL_WORK_REVIEW",
        "PINGAN_TRAINING_STUDY_EXAM",
        "PINGAN_TRAINING_MANAGE",
        "PINGAN_POINTS_MANAGE",
        "PINGAN_LEDGER_MANAGE",
        "PINGAN_DATABASE_MANAGE");
  }

  private Map<String, Object> roleSeedFor(String roleCode) {
    return jdbcTemplate
        .queryForList("select role_name, data_scope from sys_role where role_code = ?", roleCode)
        .stream()
        .findFirst()
        .orElse(Map.of());
  }

  private Set<String> hiddenPermissionCodesUnderPinganPermissionTree() {
    return Set.copyOf(
        jdbcTemplate.queryForList(
            """
            with recursive pingan_tree(id, parent_id, permission_code, visible, status, deleted) as (
              select id, parent_id, permission_code, visible, status, deleted
              from sys_menu
              where menu_code = 'PINGAN_PERMISSION_GROUP'
              union all
              select child.id, child.parent_id, child.permission_code, child.visible, child.status, child.deleted
              from sys_menu child
              join pingan_tree p on child.parent_id = p.id
            )
            select permission_code
            from pingan_tree
            where visible = 0
              and status = 'ACTIVE'
              and deleted = 0
            """,
            String.class));
  }

  private Set<String> permissionCodesForRole(String roleCode) {
    return Set.copyOf(
        jdbcTemplate.queryForList(
            """
            select m.permission_code
            from sys_role r
            join sys_role_menu rm on rm.role_id = r.id
            join sys_menu m on m.id = rm.menu_id
            where r.role_code = ?
              and m.permission_code is not null
              and m.status = 'ACTIVE'
              and m.deleted = 0
            """,
            String.class,
            roleCode));
  }
}
