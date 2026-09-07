package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(
    properties = {
      "pingan.security.max-failed-login-attempts=2",
      "pingan.security.lock-minutes=30",
      "pingan.security.two-factor-enabled=true",
      "pingan.security.test-verification-code=246810"
    })
class SystemSecurityApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void demoAccountSeedsContainSixRolesAccountsAndFixedOrgMap() throws Exception {
    Map<String, String> roleScopes =
        Map.of(
            "COMPANY_LEADER",
            "ORG_AND_CHILDREN",
            "ENTERPRISE_LEADER",
            "ORG_AND_CHILDREN",
            "DEPARTMENT_MANAGER",
            "ORG_AND_CHILDREN",
            "WORKSHOP_DIRECTOR",
            "ORG_AND_CHILDREN",
            "TEAM_LEADER",
            "ORG_AND_CHILDREN",
            "TEAM_MEMBER",
            "SELF");
    for (Map.Entry<String, String> entry : roleScopes.entrySet()) {
      assertThat(
              jdbcTemplate.queryForObject(
                  "select data_scope from sys_role where role_code = ? and deleted = 0",
                  String.class,
                  entry.getKey()))
          .isEqualTo(entry.getValue());
    }

    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from sys_org where org_code in ('SYS_DEMO_DEPT', 'SYS_DEMO_TEAM')",
                Integer.class))
        .isZero();

    for (String username :
        List.of(
            "company_leader",
            "enterprise_leader",
            "department_manager",
            "workshop_director",
            "team_leader",
            "team_member")) {
      assertThat(login(username, "SAFE_TEST_PASSWORD")).isNotBlank();
    }

    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    JsonNode teamMember =
        getJson("/api/system/accounts?keyword=team_member", adminToken).path("data").path("items").get(0);
    assertThat(ids(teamMember.path("roleIds")))
        .contains(
            jdbcTemplate.queryForObject(
                "select id from sys_role where role_code = 'TEAM_MEMBER'", Long.class));
    assertThat(textsByField(teamMember.path("roles"), "roleCode")).contains("TEAM_MEMBER");
  }

  @Test
  void oneShiftThreeCheckRolePermissionSeedsFollowBusinessMatrix() throws Exception {
    Map<String, List<String>> expectedCodes =
        Map.of(
            "company_leader",
                List.of(
                    "PINGAN_MONITOR_CENTER_VIEW",
                    "PINGAN_ORGANIZATION_VIEW",
                    "PINGAN_RISK_VIEW",
                    "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
                    "PINGAN_HAZARD_VIEW"),
            "enterprise_leader",
                List.of(
                    "PINGAN_MONITOR_CENTER_VIEW",
                    "PINGAN_ORGANIZATION_VIEW",
                    "PINGAN_RISK_VIEW",
                    "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
                    "PINGAN_HAZARD_VIEW"),
            "department_manager",
                List.of(
                    "PINGAN_ORGANIZATION_VIEW",
                    "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
                    "PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND",
                    "PINGAN_SPECIAL_WORK_APPROVE"),
            "workshop_director",
                List.of(
                    "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
                    "PINGAN_TEAM_DISPATCH_CREATE",
                    "PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND",
                    "PINGAN_ONE_SHIFT_THREE_CHECKS_VOID",
                    "PINGAN_HAZARD_RECTIFICATION"),
            "team_leader",
                List.of(
                    "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
                    "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE",
                    "PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT",
                    "PINGAN_ONE_SHIFT_THREE_CHECKS_VOID",
                    "PINGAN_HAZARD_RECTIFICATION"),
            "team_member",
                List.of(
                    "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
                    "PINGAN_RISK_CONFIRM",
                    "PINGAN_HAZARD_REPORT",
                    "PINGAN_TRAINING_STUDY_EXAM"));

    for (Map.Entry<String, List<String>> entry : expectedCodes.entrySet()) {
      JsonNode codes = getJson("/api/auth/codes", login(entry.getKey(), "SAFE_TEST_PASSWORD")).path("data");
      assertThat(texts(codes)).containsAll(entry.getValue());
    }

    JsonNode companyLeaderCodes = getJson("/api/auth/codes", login("company_leader", "SAFE_TEST_PASSWORD")).path("data");
    assertThat(texts(companyLeaderCodes))
        .doesNotContain("PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT", "PINGAN_TEAM_DISPATCH_CREATE");

    JsonNode teamMemberCodes = getJson("/api/auth/codes", login("team_member", "SAFE_TEST_PASSWORD")).path("data");
    assertThat(texts(teamMemberCodes))
        .doesNotContain(
            "PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND",
            "PINGAN_TEAM_DISPATCH_CREATE");

    assertThat(
            jdbcTemplate.queryForObject(
                "select role_name from sys_role where role_code = 'ENTERPRISE_LEADER'",
                String.class))
        .isEqualTo("企业领导");
  }

  @Test
  void nonAdminUsersCannotSeeOrOpenSystemManagement() throws Exception {
    String companyToken = login("company_leader", "SAFE_TEST_PASSWORD");
    JsonNode companyLeaderCodes = getJson("/api/auth/codes", companyToken).path("data");
    assertThat(texts(companyLeaderCodes)).noneMatch(code -> code.startsWith("SYSTEM_"));

    JsonNode companyLeaderRoutes = getJson("/api/menu/all", companyToken).path("data");
    assertThat(findRoute(companyLeaderRoutes, "SystemManagement").isMissingNode()).isTrue();

    String memberToken = login("team_member", "SAFE_TEST_PASSWORD");
    JsonNode memberCodes = getJson("/api/auth/codes", memberToken).path("data");
    assertThat(texts(memberCodes)).noneMatch(code -> code.startsWith("SYSTEM_"));

    mockMvc
        .perform(get("/api/system/accounts?pageSize=50").header("Authorization", "Bearer " + companyToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(get("/api/system/accounts?pageSize=50").header("Authorization", "Bearer " + memberToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void organizationViewersCanReadOrganizationStructureSystemEndpointsOnly() throws Exception {
    String departmentManagerToken = login("department_manager", "SAFE_TEST_PASSWORD");
    JsonNode codes = getJson("/api/auth/codes", departmentManagerToken).path("data");
    assertThat(texts(codes)).contains("PINGAN_ORGANIZATION_VIEW");

    getJson("/api/system/companies?pageSize=50", departmentManagerToken);
    getJson("/api/system/departments?pageSize=50", departmentManagerToken);
    getJson("/api/system/teams?pageSize=50", departmentManagerToken);
    getJson("/api/system/personnel?pageSize=50", departmentManagerToken);

    mockMvc
        .perform(get("/api/system/accounts?pageSize=50").header("Authorization", "Bearer " + departmentManagerToken))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/system/roles?pageSize=50").header("Authorization", "Bearer " + departmentManagerToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void enterpriseLeaderCanLoadOrganizationArchitecturePageData() throws Exception {
    String enterpriseLeaderToken = login("enterprise_leader", "SAFE_TEST_PASSWORD");
    JsonNode codes = getJson("/api/auth/codes", enterpriseLeaderToken).path("data");
    assertThat(texts(codes)).contains("PINGAN_ORGANIZATION_VIEW");

    getJson("/api/pingan/org/company-tree", enterpriseLeaderToken);
    getJson("/api/pingan/org/tree", enterpriseLeaderToken);
    getJson("/api/system/companies?page=1&pageSize=1&status=all", enterpriseLeaderToken);
    getJson("/api/system/departments?page=1&pageSize=1&status=all", enterpriseLeaderToken);
    getJson("/api/system/teams?page=1&pageSize=1&status=all", enterpriseLeaderToken);
    getJson("/api/system/personnel?page=1&pageSize=1&status=ACTIVE", enterpriseLeaderToken);
    getJson("/api/pingan/content-profiles/by-org/1", enterpriseLeaderToken);
    getJson(
        "/api/pingan/three-checks/team-dispatch/records?page=1&pageSize=1",
        enterpriseLeaderToken);
    getJson(
        "/api/pingan/three-checks/curtain-wall-team-dispatch/records?page=1&pageSize=1",
        enterpriseLeaderToken);
    getJson(
        "/api/pingan/three-checks/pre-shift-meeting/records?page=1&pageSize=1",
        enterpriseLeaderToken);
    getJson(
        "/api/pingan/three-checks/pre-shift-inspection/records?page=1&pageSize=1",
        enterpriseLeaderToken);
    getJson(
        "/api/pingan/three-checks/mid-shift-inspection/records?page=1&pageSize=1",
        enterpriseLeaderToken);
    getJson(
        "/api/pingan/three-checks/post-shift-inspection/records?page=1&pageSize=1",
        enterpriseLeaderToken);
    getJson(
        "/api/pingan/three-checks/hazard-rectification/records?page=1&pageSize=1&status=PENDING_RECTIFICATION",
        enterpriseLeaderToken);
  }

  @Test
  void accountLifecycleSupportsFreezeLoginBlockUnfreezeResetPasswordAndRoles() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");

    JsonNode created =
        postJson(
                "/api/system/accounts",
                adminToken,
                Map.of(
                    "username",
                    "security_api_user",
                    "password",
                    "InitSAFE_TEST_PASSWORD",
                    "realName",
                    "权限测试用户",
                    "mobile",
                    "13900000001",
                    "orgId",
                    4,
                    "roleIds",
                    List.of(3)))
            .path("data");

    long accountId = created.path("id").asLong();
    assertThat(created.path("username").asText()).isEqualTo("security_api_user");
    assertThat(ids(created.path("roleIds"))).containsExactly(3L);
    assertThat(created.path("roles").get(0).path("roleCode").asText()).isEqualTo("TEAM_LEADER");

    JsonNode list = getJson("/api/system/accounts?keyword=security_api_user", adminToken).path("data");
    assertThat(list.path("total").asInt()).isGreaterThanOrEqualTo(1);

    JsonNode updated =
        putJson(
                "/api/system/accounts/" + accountId,
                adminToken,
                Map.of("realName", "权限测试用户已更新", "mobile", "13900000002", "orgId", 4, "status", "ACTIVE"))
            .path("data");
    assertThat(updated.path("realName").asText()).isEqualTo("权限测试用户已更新");

    postJson("/api/system/accounts/" + accountId + "/freeze", adminToken, Map.of());
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", "security_api_user", "password", "InitSAFE_TEST_PASSWORD"))))
        .andExpect(status().isBadRequest());

    postJson("/api/system/accounts/" + accountId + "/unfreeze", adminToken, Map.of());
    String userToken = login("security_api_user", "InitSAFE_TEST_PASSWORD");
    assertThat(userToken).isNotBlank();

    JsonNode reset =
        postJson(
                "/api/system/accounts/" + accountId + "/reset-password",
                adminToken,
                Map.of("password", "ChangedSAFE_TEST_PASSWORD"))
            .path("data");
    assertThat(reset.path("id").asLong()).isEqualTo(accountId);
    assertThat(login("security_api_user", "ChangedSAFE_TEST_PASSWORD")).isNotBlank();

    putJson("/api/system/accounts/" + accountId + "/roles", adminToken, Map.of("roleIds", List.of(2)));
    Integer roleCount =
        jdbcTemplate.queryForObject(
            "select count(*) from sys_user_role where user_id = ? and role_id = 2", Integer.class, accountId);
    assertThat(roleCount).isEqualTo(1);

    deleteJson("/api/system/accounts/" + accountId, adminToken);
    Integer deleted =
        jdbcTemplate.queryForObject("select deleted from sys_user where id = ?", Integer.class, accountId);
    assertThat(deleted).isEqualTo(1);
  }

  @Test
  void roleMenuGrantChangesAuthCodesAndMenuTreeComesFromDatabase() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    String roleCode = "SECURITY_API_ROLE_" + System.nanoTime();
    JsonNode role =
        postJson(
                "/api/system/roles",
                adminToken,
                Map.of("roleCode", roleCode, "roleName", "权限接口测试角色", "dataScope", "SELF"))
            .path("data");
    long roleId = role.path("id").asLong();

    JsonNode menu =
        postJson(
                "/api/system/menus",
                adminToken,
                Map.of(
                    "parentId",
                    1000,
                    "menuCode",
                    "SECURITY_API_MENU_" + System.nanoTime(),
                    "title",
                    "权限接口测试菜单",
                    "routePath",
                    "/system/security-api-test",
                    "component",
                    "/system-management/security-api-test/index.vue",
                    "icon",
                    "lucide:key-round",
                    "permissionCode",
                    "SECURITY_API_TEST_CODE",
                    "sortOrder",
                    99,
                    "visible",
                    true,
                    "status",
                    "ACTIVE"))
            .path("data");
    long menuId = menu.path("id").asLong();

    putJson("/api/system/roles/" + roleId + "/menus", adminToken, Map.of("menuIds", List.of(menuId)));
    JsonNode grantedMenuIds = getJson("/api/system/roles/" + roleId + "/menus", adminToken).path("data");
    assertThat(ids(grantedMenuIds)).contains(menuId);

    JsonNode account =
        postJson(
                "/api/system/accounts",
                adminToken,
                Map.of(
                    "username",
                    "security_codes_user",
                    "password",
                    "InitSAFE_TEST_PASSWORD",
                    "realName",
                    "权限码用户",
                    "orgId",
                    4,
                    "roleIds",
                    List.of(roleId)))
            .path("data");

    String userToken = login("security_codes_user", "InitSAFE_TEST_PASSWORD");
    JsonNode codes = getJson("/api/auth/codes", userToken).path("data");
    assertThat(texts(codes)).contains("SECURITY_API_TEST_CODE");

    JsonNode tree = getJson("/api/system/menus/tree", adminToken).path("data");
    assertThat(flattenTitles(tree)).contains("系统管理", "权限接口测试菜单");

    deleteJson("/api/system/accounts/" + account.path("id").asLong(), adminToken);
    deleteJson("/api/system/menus/" + menuId, adminToken);
    deleteJson("/api/system/roles/" + roleId, adminToken);
  }

  @Test
  void authCodesIncludeAncestorModuleEntriesForGrantedChildPermissions() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    String roleCode = "SECURITY_CHILD_ONLY_ROLE_" + System.nanoTime();
    JsonNode role =
        postJson(
                "/api/system/roles",
                adminToken,
                Map.of("roleCode", roleCode, "roleName", "子权限入口推导测试角色", "dataScope", "SELF"))
            .path("data");
    long roleId = role.path("id").asLong();
    Long viewMenuId =
        jdbcTemplate.queryForObject(
            "select id from sys_menu where permission_code = 'PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW'",
            Long.class);

    putJson("/api/system/roles/" + roleId + "/menus", adminToken, Map.of("menuIds", List.of(viewMenuId)));

    JsonNode account =
        postJson(
                "/api/system/accounts",
                adminToken,
                Map.of(
                    "username",
                    "security_child_only_user",
                    "password",
                    "InitSAFE_TEST_PASSWORD",
                    "realName",
                    "子权限入口推导用户",
                    "orgId",
                    4,
                    "roleIds",
                    List.of(roleId)))
            .path("data");

    JsonNode codes = getJson("/api/auth/codes", login("security_child_only_user", "InitSAFE_TEST_PASSWORD")).path("data");
    assertThat(texts(codes))
        .contains(
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_ENTRY",
            "PINGAN_THREE_CHECK_ENTRY")
        .doesNotContain("PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT");

    deleteJson("/api/system/accounts/" + account.path("id").asLong(), adminToken);
    deleteJson("/api/system/roles/" + roleId, adminToken);
  }

  @Test
  void roleMenuParentGrantExpandsToExecutableChildPermissions() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    String suffix = String.valueOf(System.nanoTime());
    JsonNode role =
        postJson(
                "/api/system/roles",
                adminToken,
                Map.of(
                    "roleCode",
                    "SECURITY_PARENT_GRANT_ROLE_" + suffix,
                    "roleName",
                    "父级授权执行测试角色",
                    "dataScope",
                    "SELF"))
            .path("data");
    long roleId = role.path("id").asLong();
    Long oneShiftModuleMenuId =
        jdbcTemplate.queryForObject(
            "select id from sys_menu where menu_code = 'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE'",
            Long.class);

    putJson(
        "/api/system/roles/" + roleId + "/menus",
        adminToken,
        Map.of("menuIds", List.of(oneShiftModuleMenuId)));

    JsonNode account =
        postJson(
                "/api/system/accounts",
                adminToken,
                Map.of(
                    "username",
                    "security_parent_grant_user_" + suffix,
                    "password",
                    "InitSAFE_TEST_PASSWORD",
                    "realName",
                    "父级授权执行用户",
                    "orgId",
                    1011001,
                    "roleIds",
                    List.of(roleId)))
            .path("data");

    String token = login("security_parent_grant_user_" + suffix, "InitSAFE_TEST_PASSWORD");
    JsonNode codes = getJson("/api/auth/codes", token).path("data");
    assertThat(texts(codes))
        .contains(
            "PINGAN_ONE_SHIFT_THREE_CHECKS_ENTRY",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT");

    postJson(
        "/api/pingan/three-checks/pre-shift-inspection/records",
        token,
        Map.of(
            "companyId",
            4,
            "departmentId",
            101109,
            "teamId",
            1011001,
            "ownerUserId",
            account.path("id").asLong(),
            "businessDate",
            "2036-12-20",
            "payload",
            Map.of("workContent", "父级授权新增班前检查", "statusLabel", "待检查")));

    deleteJson("/api/system/accounts/" + account.path("id").asLong(), adminToken);
    deleteJson("/api/system/roles/" + roleId, adminToken);
  }

  @Test
  void pinganBusinessPermissionTreeGroupsModuleEntryViewAndActionCodes() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");

    JsonNode tree = getJson("/api/system/menus/tree", adminToken).path("data");
    JsonNode pinganPermissionRoot = findNodeByTitle(tree, "平安班组业务权限");

    assertThat(pinganPermissionRoot.isMissingNode()).isFalse();
    assertThat(directChildTitles(pinganPermissionRoot.path("children")))
        .containsExactly(
            "监控中心",
            "组织架构",
            "风险管控",
            "一班三查",
            "隐患排查",
            "特殊作业",
            "宣教培训",
            "安全积分",
            "安全台账",
            "消息预警",
            "数据库");
    assertThat(flattenTitles(pinganPermissionRoot.path("children")))
        .doesNotContain("安全报表查看", "安全检查与集团监管");

    assertModulePermissionCodes(
        pinganPermissionRoot,
        "组织架构",
        List.of("PINGAN_ORGANIZATION_ENTRY", "PINGAN_ORGANIZATION_VIEW"));
    assertModulePermissionCodes(
        pinganPermissionRoot,
        "监控中心",
        List.of(
            "PINGAN_MONITOR_CENTER_ENTRY",
            "PINGAN_MONITOR_CENTER_VIEW",
            "PINGAN_MONITOR_CENTER_GLOBAL_VIEW"));
    assertModulePermissionCodes(
        pinganPermissionRoot,
        "一班三查",
        List.of(
            "PINGAN_THREE_CHECK_ENTRY",
            "PINGAN_TEAM_DISPATCH_ENTRY",
            "PINGAN_TEAM_DISPATCH_VIEW",
            "PINGAN_TEAM_DISPATCH_CREATE",
            "PINGAN_TEAM_DISPATCH_UPDATE",
            "PINGAN_TEAM_DISPATCH_SUBMIT",
            "PINGAN_TEAM_DISPATCH_VOID",
            "PINGAN_TEAM_DISPATCH_DELETE",
            "PINGAN_TEAM_DISPATCH_REMIND",
            "PINGAN_TEAM_DISPATCH_EXPORT",
            "PINGAN_PRE_SHIFT_MEETING_ENTRY",
            "PINGAN_PRE_SHIFT_MEETING_VIEW",
            "PINGAN_PRE_SHIFT_MEETING_CREATE",
            "PINGAN_PRE_SHIFT_MEETING_UPDATE",
            "PINGAN_PRE_SHIFT_MEETING_SUBMIT",
            "PINGAN_PRE_SHIFT_MEETING_VOID",
            "PINGAN_PRE_SHIFT_MEETING_DELETE",
            "PINGAN_PRE_SHIFT_MEETING_REMIND",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_ENTRY",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_UPDATE",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_VOID",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_DELETE",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_EXPORT",
            "PINGAN_ONE_SHIFT_THREE_CHECKS_RECTIFICATION_ORDER_CREATE",
            "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_ENTRY",
            "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VIEW",
            "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_CREATE",
            "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_UPDATE",
            "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_SUBMIT",
            "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VOID",
            "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_DELETE",
            "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_REMIND",
            "PINGAN_CURTAIN_WALL_TEAM_DISPATCH_EXPORT",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_ENTRY",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_CREATE",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_UPDATE",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_SUBMIT",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VOID",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_DELETE",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_REMIND",
            "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_EXPORT",
            "PINGAN_KEY_SITES_ENTRY",
            "PINGAN_KEY_SITES_VIEW",
            "PINGAN_KEY_SITES_CREATE",
            "PINGAN_KEY_SITES_UPDATE",
            "PINGAN_KEY_SITES_SUBMIT",
            "PINGAN_KEY_SITES_VOID",
            "PINGAN_KEY_SITES_DELETE",
            "PINGAN_KEY_SITES_REMIND",
            "PINGAN_KEY_SITES_EXPORT"));
    JsonNode threeCheckModule = findNodeByTitle(pinganPermissionRoot.path("children"), "一班三查");
    assertThat(directChildTitles(threeCheckModule.path("children")))
        .containsExactly("班组派班", "班前会", "三查", "幕墙班组派班", "班前安全活动", "重点场所");
    assertThat(permissionCodesInSubtree(threeCheckModule))
        .doesNotContain(
            "PINGAN_THREE_CHECK_VIEW",
            "PINGAN_TEAM_DISPATCH_MANAGE",
            "PINGAN_THREE_CHECK_EXECUTE",
            "PINGAN_THREE_CHECK_UPDATE",
            "PINGAN_THREE_CHECK_WITHDRAW",
            "PINGAN_THREE_CHECK_REMIND",
            "PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW",
            "PINGAN_GROUP_THREE_CHECK_VIEW",
            "PINGAN_KEY_SITE_INSPECTION");
    assertModulePermissionCodes(
        pinganPermissionRoot,
        "隐患排查",
        List.of(
            "PINGAN_HAZARD_ENTRY",
            "PINGAN_HAZARD_SAFETY_CHECK_ENTRY",
            "PINGAN_HAZARD_SAFETY_CHECK_VIEW",
            "PINGAN_HAZARD_SAFETY_CHECK_CREATE",
            "PINGAN_HAZARD_SAFETY_CHECK_DELETE",
            "PINGAN_HAZARD_SAFETY_CHECK_VOID",
            "PINGAN_HAZARD_SAFETY_CHECK_EXPORT",
            "PINGAN_HAZARD_RECTIFICATION_ENTRY",
            "PINGAN_HAZARD_RECTIFICATION_VIEW",
            "PINGAN_HAZARD_RECTIFICATION_CREATE",
            "PINGAN_HAZARD_RECTIFICATION_REPORT",
            "PINGAN_HAZARD_RECTIFICATION_RECTIFY",
            "PINGAN_HAZARD_RECTIFICATION_ACCEPT",
            "PINGAN_HAZARD_RECTIFICATION_DELETE",
            "PINGAN_HAZARD_RECTIFICATION_VOID",
            "PINGAN_HAZARD_RECTIFICATION_EXPORT",
            "PINGAN_HAZARD_QUICK_SHOT_ENTRY",
            "PINGAN_HAZARD_QUICK_SHOT_VIEW",
            "PINGAN_HAZARD_QUICK_SHOT_REPORT",
            "PINGAN_HAZARD_QUICK_SHOT_REVIEW",
            "PINGAN_HAZARD_QUICK_SHOT_DELETE",
            "PINGAN_HAZARD_QUICK_SHOT_VOID",
            "PINGAN_HAZARD_QUICK_SHOT_EXPORT",
            "PINGAN_HAZARD_CURTAIN_WALL_PENALTY_ENTRY",
            "PINGAN_HAZARD_CURTAIN_WALL_PENALTY_VIEW",
            "PINGAN_HAZARD_CURTAIN_WALL_PENALTY_CREATE",
            "PINGAN_HAZARD_CURTAIN_WALL_PENALTY_DELETE",
            "PINGAN_HAZARD_CURTAIN_WALL_PENALTY_VOID",
            "PINGAN_HAZARD_CURTAIN_WALL_PENALTY_EXPORT",
            "PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_ENTRY",
            "PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_VIEW",
            "PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_CREATE",
            "PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_DELETE",
            "PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_VOID",
            "PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_EXPORT"));
    JsonNode hazardModule = findNodeByTitle(pinganPermissionRoot.path("children"), "隐患排查");
    assertThat(directChildTitles(hazardModule.path("children")))
        .containsExactly("安全检查", "隐患整改", "随手拍", "幕墙处罚管理", "幕墙日周月检");
    assertThat(permissionCodesInSubtree(hazardModule))
        .doesNotContain(
            "PINGAN_HAZARD_VIEW",
            "PINGAN_HAZARD_REPORT",
            "PINGAN_HAZARD_RECTIFICATION",
            "PINGAN_HAZARD_ACCEPT",
            "PINGAN_HAZARD_CLOSE");
    assertModulePermissionCodes(
        pinganPermissionRoot,
        "风险管控",
        List.of("PINGAN_RISK_ENTRY", "PINGAN_RISK_VIEW", "PINGAN_RISK_MANAGE", "PINGAN_RISK_CONFIRM"));
    assertModulePermissionCodes(
        pinganPermissionRoot,
        "特殊作业",
        List.of(
            "PINGAN_SPECIAL_WORK_ENTRY",
            "PINGAN_SPECIAL_WORK_VIEW",
            "PINGAN_SPECIAL_WORK_APPLY",
            "PINGAN_SPECIAL_WORK_APPROVE",
            "PINGAN_SPECIAL_WORK_REVIEW"));
    assertModulePermissionCodes(
        pinganPermissionRoot,
        "宣教培训",
        List.of(
            "PINGAN_TRAINING_ENTRY",
            "PINGAN_TRAINING_EXAM_TASKS_ENTRY",
            "PINGAN_TRAINING_EXAM_TASKS_VIEW",
            "PINGAN_TRAINING_EXAM_TASKS_CREATE",
            "PINGAN_TRAINING_EXAM_TASKS_DELETE",
            "PINGAN_TRAINING_EXAM_TASKS_DOWNLOAD",
            "PINGAN_TRAINING_EXAM_RESULTS_ENTRY",
            "PINGAN_TRAINING_EXAM_RESULTS_VIEW",
            "PINGAN_TRAINING_EXAM_RESULTS_CREATE",
            "PINGAN_TRAINING_EXAM_RESULTS_DELETE",
            "PINGAN_TRAINING_SAFETY_LEARNING_ENTRY",
            "PINGAN_TRAINING_SAFETY_LEARNING_VIEW",
            "PINGAN_TRAINING_SAFETY_LEARNING_CREATE",
            "PINGAN_TRAINING_SAFETY_LEARNING_DELETE",
            "PINGAN_TRAINING_SAFETY_LEARNING_VOID",
            "PINGAN_TRAINING_SAFETY_LEARNING_DOWNLOAD"));
    JsonNode trainingModule = findNodeByTitle(pinganPermissionRoot.path("children"), "宣教培训");
    assertThat(directChildTitles(trainingModule.path("children")))
        .containsExactly("考试任务", "考试成绩", "安全学习");
    assertThat(permissionCodesInSubtree(trainingModule))
        .doesNotContain("PINGAN_TRAINING_VIEW", "PINGAN_TRAINING_STUDY_EXAM", "PINGAN_TRAINING_MANAGE");
    assertModulePermissionCodes(
        pinganPermissionRoot,
        "安全积分",
        List.of(
            "PINGAN_POINTS_ENTRY",
            "PINGAN_POINTS_FLOW_ENTRY",
            "PINGAN_POINTS_FLOW_VIEW",
            "PINGAN_POINTS_FLOW_CREATE",
            "PINGAN_POINTS_FLOW_DELETE",
            "PINGAN_POINTS_RANKING_ENTRY",
            "PINGAN_POINTS_RANKING_VIEW"));
    JsonNode pointsModule = findNodeByTitle(pinganPermissionRoot.path("children"), "安全积分");
    assertThat(directChildTitles(pointsModule.path("children"))).containsExactly("积分流水", "积分榜单");
    assertThat(permissionCodesInSubtree(pointsModule)).doesNotContain("PINGAN_POINTS_VIEW", "PINGAN_POINTS_MANAGE");
    assertModulePermissionCodes(
        pinganPermissionRoot,
        "安全台账",
        List.of("PINGAN_LEDGER_ENTRY", "PINGAN_LEDGER_VIEW", "PINGAN_LEDGER_MANAGE"));
    assertModulePermissionCodes(
        pinganPermissionRoot,
        "数据库",
        List.of("PINGAN_DATABASE_ENTRY", "PINGAN_DATABASE_VIEW", "PINGAN_DATABASE_MANAGE"));
  }

  @Test
  void menuAllReturnsSystemManagementRoutesForVbenBackendAccessMode() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");

    JsonNode routes = getJson("/api/menu/all", adminToken).path("data");
    JsonNode systemRoute =
        findRoute(routes, "SystemManagement");

    assertThat(systemRoute.path("path").asText()).isEqualTo("/system");
    assertThat(systemRoute.path("component").asText()).isEqualTo("BasicLayout");
    assertThat(systemRoute.path("redirect").asText()).isEqualTo("/system/account-management");
    assertThat(systemRoute.path("meta").path("title").asText()).isEqualTo("系统管理");

    JsonNode accountRoute = findRoute(systemRoute.path("children"), "SystemAccountManagement");
    assertThat(accountRoute.path("path").asText()).isEqualTo("/system/account-management");
    assertThat(accountRoute.path("component").asText())
        .isEqualTo("/system-management/account-management/index.vue");

    assertThat(routeNames(systemRoute.path("children")))
        .containsExactly(
            "SystemAccountManagement",
            "SystemRolePermission",
            "SystemCompanyManagement",
            "SystemDepartmentManagement",
            "SystemTeamManagement",
            "SystemPersonnelManagement",
            "SystemMenuManagement",
            "SystemContentProfile",
            "SystemTeamCheckItemTemplate");
  }

  @Test
  void menuAllExcludesHiddenPermissionNodes() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");

    JsonNode routes = getJson("/api/menu/all", adminToken).path("data");

    assertThat(flattenTitles(routes)).doesNotContain("平安班组业务权限");
    assertThat(flattenPermissionCodes(routes)).doesNotContain("PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT");
    assertThat(findRoute(routes, "PinganThreeCheckModule").isMissingNode()).isTrue();
  }

  @Test
  void roleMenuGrantRejectsInvalidMenuIdsBeforeReplacingExistingGrants() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    String suffix = String.valueOf(System.nanoTime());
    String roleCode = "INVALID_MENU_GRANT_ROLE_" + suffix;
    JsonNode role =
        postJson(
                "/api/system/roles",
                adminToken,
                Map.of("roleCode", roleCode, "roleName", "无效授权测试角色", "dataScope", "SELF"))
            .path("data");
    long roleId = role.path("id").asLong();
    long validMenuId =
        jdbcTemplate.queryForObject(
            "select id from sys_menu where menu_code = 'PINGAN_THREE_CHECK_MODULE'",
            Long.class);
    long missingMenuId =
        jdbcTemplate.queryForObject("select coalesce(max(id), 0) + 999999 from sys_menu", Long.class);
    JsonNode inactiveMenu = createTestMenu(adminToken, suffix + "_INACTIVE", "INACTIVE");
    JsonNode deletedMenu = createTestMenu(adminToken, suffix + "_DELETED", "ACTIVE");
    long inactiveMenuId = inactiveMenu.path("id").asLong();
    long deletedMenuId = deletedMenu.path("id").asLong();

    try {
      putJson("/api/system/roles/" + roleId + "/menus", adminToken, Map.of("menuIds", List.of(validMenuId)));
      List<Long> expectedGrantedMenuIds = ids(getJson("/api/system/roles/" + roleId + "/menus", adminToken).path("data"));
      assertThat(expectedGrantedMenuIds).contains(validMenuId);
      deleteJson("/api/system/menus/" + deletedMenuId, adminToken);

      assertInvalidMenuGrantRejectedPreservesExistingGrants(
          adminToken, roleId, validMenuId, missingMenuId, expectedGrantedMenuIds);
      assertInvalidMenuGrantRejectedPreservesExistingGrants(
          adminToken, roleId, validMenuId, inactiveMenuId, expectedGrantedMenuIds);
      assertInvalidMenuGrantRejectedPreservesExistingGrants(
          adminToken, roleId, validMenuId, deletedMenuId, expectedGrantedMenuIds);
    } finally {
      deleteJson("/api/system/menus/" + inactiveMenuId, adminToken);
      deleteJson("/api/system/roles/" + roleId, adminToken);
    }
  }

  @Test
  void roleAndMenuListsSupportKeywordAndStatusFilters() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    String suffix = String.valueOf(System.nanoTime());
    JsonNode role =
        postJson(
                "/api/system/roles",
                adminToken,
                Map.of("roleCode", "FILTER_ROLE_" + suffix, "roleName", "筛选角色" + suffix, "dataScope", "SELF"))
            .path("data");
    JsonNode menu =
        postJson(
                "/api/system/menus",
                adminToken,
                Map.of(
                    "parentId",
                    1000,
                    "menuCode",
                    "FILTER_MENU_" + suffix,
                    "title",
                    "筛选菜单" + suffix,
                    "routePath",
                    "/system/filter-menu-" + suffix,
                    "component",
                    "/system-management/filter-menu/index.vue",
                    "icon",
                    "lucide:search",
                    "permissionCode",
                    "FILTER_MENU_CODE_" + suffix,
                    "sortOrder",
                    88,
                    "visible",
                    true,
                    "status",
                    "INACTIVE"))
            .path("data");

    JsonNode roles = getJson("/api/system/roles?keyword=" + suffix, adminToken).path("data");
    assertThat(textsByField(roles, "roleCode")).contains("FILTER_ROLE_" + suffix);
    assertThat(textsByField(roles, "roleCode")).allMatch(code -> code.contains(suffix));

    JsonNode inactiveMenus = getJson("/api/system/menus?keyword=" + suffix + "&status=INACTIVE", adminToken).path("data");
    assertThat(textsByField(inactiveMenus, "menuCode")).contains("FILTER_MENU_" + suffix);
    assertThat(textsByField(inactiveMenus, "status")).containsOnly("INACTIVE");

    JsonNode activeMenus = getJson("/api/system/menus?keyword=" + suffix + "&status=ACTIVE", adminToken).path("data");
    assertThat(activeMenus).isEmpty();

    deleteJson("/api/system/menus/" + menu.path("id").asLong(), adminToken);
    deleteJson("/api/system/roles/" + role.path("id").asLong(), adminToken);
  }

  @Test
  void failedLoginLockTwoFactorAndLoginAuditAreEnforced() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    String username = "security_lock_user_" + System.nanoTime();
    postJson(
        "/api/system/accounts",
        adminToken,
        Map.of(
            "username",
            username,
            "password",
            "InitSAFE_TEST_PASSWORD",
            "realName",
            "登录锁定用户",
            "orgId",
            4,
            "roleIds",
            List.of(3)));

    JsonNode missingCode =
        objectMapper.readTree(
            mockMvc
                .perform(
                    post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "SAFE_TEST_PASSWORD"))))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    assertThat(missingCode.path("message").asText()).contains("验证码");

    failedLogin(username, "WrongSAFE_TEST_PASSWORD").andExpect(status().isBadRequest());
    JsonNode locked =
        objectMapper.readTree(
            failedLogin(username, "WrongSAFE_TEST_PASSWORD")
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    assertThat(locked.path("message").asText()).contains("锁定");

    JsonNode stillLocked =
        objectMapper.readTree(
            mockMvc
                .perform(
                    post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of(
                                    "username",
                                    username,
                                    "password",
                                    "InitSAFE_TEST_PASSWORD",
                                    "verificationCode",
                                    "246810"))))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    assertThat(stillLocked.path("message").asText()).contains("锁定");

    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from sys_login_attempt where username = ? and success = 0 and locked_until is not null",
                Integer.class,
                username))
        .isGreaterThanOrEqualTo(1);
    assertThat(login("admin", "SAFE_TEST_PASSWORD")).isNotBlank();
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from sys_access_log where module = 'SECURITY' and action = 'LOGIN' and username = 'admin'",
                Integer.class))
        .isGreaterThanOrEqualTo(1);
  }

  private String login(String username, String password) throws Exception {
    JsonNode response =
        postJson(
            "/api/auth/login",
            null,
            Map.of("username", username, "password", password, "verificationCode", "246810"));
    return response.path("data").path("accessToken").asText();
  }

  private org.springframework.test.web.servlet.ResultActions failedLogin(String username, String password)
      throws Exception {
    return mockMvc.perform(
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                objectMapper.writeValueAsString(
                    Map.of("username", username, "password", password, "verificationCode", "246810"))));
  }

  private JsonNode getJson(String url, String token) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(get(url).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }

  private JsonNode postJson(String url, String token, Object body) throws Exception {
    var request =
        post(url).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return objectMapper.readTree(
        mockMvc
            .perform(request)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }

  private JsonNode putJson(String url, String token, Object body) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(
                put(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }

  private void deleteJson(String url, String token) throws Exception {
    mockMvc.perform(delete(url).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
  }

  private JsonNode createTestMenu(String adminToken, String suffix, String status) throws Exception {
    return postJson(
            "/api/system/menus",
            adminToken,
            Map.of(
                "parentId",
                1000,
                "menuCode",
                "INVALID_GRANT_MENU_" + suffix,
                "title",
                "无效授权菜单" + suffix,
                "routePath",
                "/system/invalid-grant-menu-" + suffix,
                "component",
                "/system-management/invalid-grant-menu/index.vue",
                "icon",
                "lucide:ban",
                "permissionCode",
                "INVALID_GRANT_CODE_" + suffix,
                "sortOrder",
                77,
                "visible",
                true,
                "status",
                status))
        .path("data");
  }

  private void assertInvalidMenuGrantRejectedPreservesExistingGrants(
      String adminToken, long roleId, long validMenuId, long invalidMenuId, List<Long> expectedGrantedMenuIds)
      throws Exception {
    mockMvc
        .perform(
            put("/api/system/roles/" + roleId + "/menus")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("menuIds", List.of(validMenuId, invalidMenuId)))))
        .andExpect(status().isBadRequest())
        .andExpect(
            result ->
                assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .contains("菜单授权包含无效菜单"));

    JsonNode grantedMenuIds = getJson("/api/system/roles/" + roleId + "/menus", adminToken).path("data");
    assertThat(ids(grantedMenuIds)).containsExactlyElementsOf(expectedGrantedMenuIds);
  }

  private List<Long> ids(JsonNode nodes) {
    List<Long> values = new ArrayList<>();
    for (JsonNode node : nodes) {
      values.add(node.asLong());
    }
    return values;
  }

  private List<String> texts(JsonNode nodes) {
    List<String> values = new ArrayList<>();
    for (JsonNode node : nodes) {
      values.add(node.asText());
    }
    return values;
  }

  private List<String> textsByField(JsonNode nodes, String field) {
    List<String> values = new ArrayList<>();
    for (JsonNode node : nodes) {
      values.add(node.path(field).asText());
    }
    return values;
  }

  private List<String> flattenTitles(JsonNode nodes) {
    List<String> values = new ArrayList<>();
    for (JsonNode node : nodes) {
      values.add(node.path("title").asText());
      values.addAll(flattenTitles(node.path("children")));
    }
    return values;
  }

  private List<String> directChildTitles(JsonNode nodes) {
    List<String> values = new ArrayList<>();
    for (JsonNode node : nodes) {
      values.add(node.path("title").asText());
    }
    return values;
  }

  private List<String> flattenPermissionCodes(JsonNode nodes) {
    List<String> values = new ArrayList<>();
    for (JsonNode node : nodes) {
      if (!node.path("permissionCode").asText().isBlank()) {
        values.add(node.path("permissionCode").asText());
      }
      values.addAll(flattenPermissionCodes(node.path("children")));
    }
    return values;
  }

  private void assertModulePermissionCodes(JsonNode pinganPermissionRoot, String moduleTitle, List<String> expectedCodes) {
    JsonNode module = findNodeByTitle(pinganPermissionRoot.path("children"), moduleTitle);
    assertThat(module.isMissingNode()).isFalse();
    assertThat(permissionCodesInSubtree(module)).containsAll(expectedCodes);
  }

  private List<String> permissionCodesInSubtree(JsonNode node) {
    List<String> values = new ArrayList<>();
    if (!node.path("permissionCode").asText().isBlank()) {
      values.add(node.path("permissionCode").asText());
    }
    values.addAll(flattenPermissionCodes(node.path("children")));
    return values;
  }

  private JsonNode findNodeByTitle(JsonNode nodes, String title) {
    for (JsonNode node : nodes) {
      if (title.equals(node.path("title").asText())) {
        return node;
      }
      JsonNode child = findNodeByTitle(node.path("children"), title);
      if (!child.isMissingNode()) {
        return child;
      }
    }
    return objectMapper.missingNode();
  }

  private JsonNode findRoute(JsonNode nodes, String name) {
    for (JsonNode node : nodes) {
      if (name.equals(node.path("name").asText())) {
        return node;
      }
      JsonNode child = findRoute(node.path("children"), name);
      if (!child.isMissingNode()) {
        return child;
      }
    }
    return objectMapper.missingNode();
  }

  private List<String> routeNames(JsonNode nodes) {
    List<String> values = new ArrayList<>();
    for (JsonNode node : nodes) {
      values.add(node.path("name").asText());
    }
    return values;
  }
}

