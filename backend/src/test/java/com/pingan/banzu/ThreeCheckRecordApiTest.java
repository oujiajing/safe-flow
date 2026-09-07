package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class ThreeCheckRecordApiTest {

  private static final long SOURCE_COMPANY_ID = 4L;
  private static final long SOURCE_DEPARTMENT_ID = 101109L;
  private static final long SOURCE_TEAM_ID = 1011001L;
  private static final String SOURCE_COMPANY_NAME = "广晟源成";
  private static final List<String> MODULE_KEYS =
      List.of(
          "team-dispatch",
          "curtain-wall-team-dispatch",
          "pre-shift-meeting",
          "pre-shift-safety-activity",
          "pre-shift-inspection",
          "mid-shift-inspection",
          "post-shift-inspection",
          "key-sites");
  private static final List<HazardModuleCase> HAZARD_MODULE_CASES =
      List.of(
          new HazardModuleCase(
              "safety-check", "HAZARD_SAFETY_CHECK", "待检查", "已检查", null),
          new HazardModuleCase(
              "hazard-rectification", "HAZARD_RECTIFICATION", "待整改", "已整改", null),
          new HazardModuleCase(
              "curtain-wall-penalty",
              "HAZARD_CURTAIN_WALL_PENALTY",
              "待批准",
              "已生效（未读）",
              "已生效已读"),
          new HazardModuleCase(
              "curtain-wall-routine-check",
              "HAZARD_CURTAIN_WALL_ROUTINE_CHECK",
              "待检查",
              "已检查",
              null));

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  private record HazardModuleCase(
      String moduleKey,
      String bizType,
      String draftLabel,
      String openedLabel,
      String archivedLabel) {}

  @Test
  void listsSeedRecordsForEveryGenericOneShiftThreeCheckModule() throws Exception {
    String token = login("admin", "123456");

    for (String moduleKey : MODULE_KEYS) {
      JsonNode list =
          getJson(
                  "/api/pingan/three-checks/"
                      + moduleKey
                      + "/records?company="
                      + SOURCE_COMPANY_NAME
                      + "&status=all",
                  token)
              .path("data");

      assertThat(list.path("total").asInt()).as(moduleKey).isGreaterThanOrEqualTo(2);
      JsonNode row = list.path("items").get(0);
      assertThat(row.path("moduleKey").asText()).isEqualTo(moduleKey);
      assertThat(row.path("company").asText()).isEqualTo(SOURCE_COMPANY_NAME);
      assertThat(row.path("payload").isObject()).isTrue();
    }
  }

  @Test
  void filtersGenericRecordListByExactOrganizationIds() throws Exception {
    String token = login("admin", "123456");

    JsonNode byCompany =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records?companyId="
                    + SOURCE_COMPANY_ID,
                token)
            .path("data");
    assertThat(byCompany.path("total").asInt()).isGreaterThanOrEqualTo(2);
    for (JsonNode row : byCompany.path("items")) {
      assertThat(row.path("company").asText()).isEqualTo(SOURCE_COMPANY_NAME);
    }

    JsonNode byBranch =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records?organizationId=3",
                token)
            .path("data");
    assertThat(byBranch.path("total").asInt()).isGreaterThanOrEqualTo(2);
    for (JsonNode row : byBranch.path("items")) {
      assertThat(row.path("company").asText()).isEqualTo(SOURCE_COMPANY_NAME);
    }

    JsonNode byCompanyOrganization =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records?organizationId="
                    + SOURCE_COMPANY_ID,
                token)
            .path("data");
    assertThat(byCompanyOrganization.path("total").asInt()).isGreaterThanOrEqualTo(2);

    JsonNode byDepartment =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records?companyId="
                    + SOURCE_COMPANY_ID
                    + "&departmentId="
                    + SOURCE_DEPARTMENT_ID,
                token)
            .path("data");
    assertThat(byDepartment.path("total").asInt()).isGreaterThanOrEqualTo(1);
    for (JsonNode row : byDepartment.path("items")) {
      assertThat(row.path("department").asText()).isEqualTo("幕墙组装");
    }

    JsonNode byTeam =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records?companyId="
                    + SOURCE_COMPANY_ID
                    + "&departmentId="
                    + SOURCE_DEPARTMENT_ID
                    + "&teamId="
                    + SOURCE_TEAM_ID,
                token)
            .path("data");
    assertThat(byTeam.path("total").asInt()).isGreaterThanOrEqualTo(1);
    for (JsonNode row : byTeam.path("items")) {
      assertThat(row.path("team").asText()).isEqualTo("幕墙组装1班");
    }

    JsonNode mismatch =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records?companyId=8&departmentId="
                    + SOURCE_DEPARTMENT_ID,
                token)
            .path("data");
    assertThat(mismatch.path("total").asInt()).isZero();
  }

  @Test
  void paginatesAndSummarizesGenericRecordsWithEnterpriseFilters() throws Exception {
    String token = login("admin", "123456");
    List<String> createdIds = new ArrayList<>();
    jdbcTemplate.update(
        "delete from three_check_record where module_key = 'pre-shift-inspection' and payload_json like '%enterprise-query-test%'");

    for (int day = 1; day <= 32; day++) {
      boolean opened = day % 2 == 0;
      JsonNode created =
          postJson(
                  "/api/pingan/three-checks/pre-shift-inspection/records",
                  token,
                  Map.of(
                      "companyId", SOURCE_COMPANY_ID,
                      "departmentId", SOURCE_DEPARTMENT_ID,
                      "teamId", SOURCE_TEAM_ID,
                      "ownerUserId", 2,
                      "businessDate", "2026-07-" + "%02d".formatted(((day - 1) % 28) + 1),
                      "status", opened ? "已检查" : "待检查",
                      "payload",
                          Map.of(
                              "owner", "湖贝班长",
                              "statusLabel", opened ? "已检查" : "待检查",
                              "batch", "enterprise-query-test")))
              .path("data");
      createdIds.add(created.path("id").asText());
    }
    jdbcTemplate.update(
        "update three_check_record set image_check_status = '现场照片', video_check_status = '视频已传' where id in (?, ?)",
        Long.parseLong(createdIds.get(0)),
        Long.parseLong(createdIds.get(1)));

    JsonNode page =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records"
                    + "?dateStart=2026-07-01&dateEnd=2026-07-31&page=2&pageSize=10&status=all",
                token)
            .path("data");
    assertThat(page.path("total").asInt()).isEqualTo(32);
    assertThat(page.path("items")).hasSize(10);
    for (JsonNode row : page.path("items")) {
      assertThat(row.path("company").asText()).isEqualTo(SOURCE_COMPANY_NAME);
      assertThat(row.path("date").asText()).startsWith("2026-07-");
    }

    JsonNode statistics =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/statistics"
                    + "?dateStart=2026-07-01&dateEnd=2026-07-31&status=all",
                token)
            .path("data");
    assertThat(statistics.path("total").asInt()).isEqualTo(32);
    assertThat(statistics.path("statusCounts").path("OPENED").asInt()).isEqualTo(16);
    assertThat(statistics.path("statusCounts").path("DRAFT").asInt()).isEqualTo(16);
    assertThat(statistics.path("attachmentCounts").path("imageUploaded").asInt()).isEqualTo(2);
    assertThat(statistics.path("attachmentCounts").path("videoUploaded").asInt()).isEqualTo(2);
    assertThat(statistics.path("dateCounts")).isNotEmpty();
    assertThat(statistics.path("companyCounts").get(0).path("organizationId").asLong())
        .isEqualTo(SOURCE_COMPANY_ID);
  }

  @Test
  void selfScopeGenericRecordQueriesOnlyReturnCurrentOwnerRows() throws Exception {
    jdbcTemplate.update(
        "insert into sys_role (id, role_code, role_name, data_scope) values (9101, 'SELF_THREE_CHECK_TEST', '一班三查本人测试', 'SELF')");
    jdbcTemplate.update(
        "insert into sys_role_menu (role_id, menu_id) select 9101, id from sys_menu where permission_code = 'PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW'");
    jdbcTemplate.update(
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (9101, 'self_three_check', '{noop}123456', '本人范围用户', ?, 'ACTIVE', 0)",
        SOURCE_TEAM_ID);
    jdbcTemplate.update("insert into sys_user_role (user_id, role_id) values (9101, 9101)");
    insertRecordForEnterpriseQuery("TCR-SELF-OWNED", 9101L, "2026-07-01");
    insertRecordForEnterpriseQuery("TCR-SELF-OTHER", 2L, "2026-07-02");

    String token = login("self_three_check", "123456");
    JsonNode list =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records"
                    + "?dateStart=2026-07-01&dateEnd=2026-07-31&status=all",
                token)
            .path("data");

    assertThat(list.path("total").asInt()).isEqualTo(1);
    assertThat(list.path("items").get(0).path("owner").asText()).isEqualTo("本人范围用户");
  }

  @Test
  void teamMemberGenericRecordQueriesReturnSameTeamRows() throws Exception {
    long memberUserId = 9124L;
    String username = "team_member_three_check";
    cleanupThreeCheckAccessFixture(memberUserId, username, "TCR-TM-");
    createThreeCheckRoleUser(
        memberUserId,
        username,
        "组员班组可见用户",
        SOURCE_TEAM_ID,
        "TEAM_MEMBER");
    createTeamMember(SOURCE_TEAM_ID, memberUserId, username, "组员班组可见用户");
    insertRecordForEnterpriseQuery(
        "TCR-TM-SAME-TEAM", "pre-shift-inspection", 2L, SOURCE_TEAM_ID, "2036-08-21");
    insertRecordForEnterpriseQuery(
        "TCR-TM-OWNED", "pre-shift-inspection", memberUserId, 1011002L, "2036-08-22");
    insertRecordForEnterpriseQuery(
        "TCR-TM-HIDDEN", "pre-shift-inspection", 2L, 1011002L, "2036-08-23");

    String token = login(username, "123456");
    JsonNode list =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records"
                    + "?dateStart=2036-08-21&dateEnd=2036-08-23&status=all",
                token)
            .path("data");

    assertThat(list.path("total").asInt()).isEqualTo(2);
    assertThat(recordNosIn(list.path("items")))
        .containsExactlyInAnyOrder("TCR-TM-SAME-TEAM", "TCR-TM-OWNED");
  }

  @Test
  void teamMemberQuickShotReporterOnlySeesOwnQuickShotRows() throws Exception {
    long memberUserId = 9125L;
    String username = "team_member_quick_shot_reporter";
    cleanupThreeCheckAccessFixture(memberUserId, username, "TCR-QS-MEMBER-");
    createPermissionFixtureUser(
        memberUserId,
        9125L,
        username,
        "组员随手拍上报用户",
        "TEAM_MEMBER_QUICK_SHOT_REPORTER",
        "PINGAN_HAZARD_QUICK_SHOT_VIEW",
        "PINGAN_HAZARD_QUICK_SHOT_REPORT");
    jdbcTemplate.update(
        """
        insert into sys_user_role (user_id, role_id)
        select ?, id
        from sys_role
        where role_code = 'TEAM_MEMBER'
          and not exists (
            select 1 from sys_user_role existing
            where existing.user_id = ? and existing.role_id = sys_role.id
          )
        """,
        memberUserId,
        memberUserId);
    createTeamMember(SOURCE_TEAM_ID, memberUserId, username, "组员随手拍上报用户");
    insertRecordForEnterpriseQuery(
        "TCR-QS-MEMBER-SAME-TEAM", "quick-shot", 2L, SOURCE_TEAM_ID, "2036-09-21");
    insertRecordForEnterpriseQuery(
        "TCR-QS-MEMBER-OWN", "quick-shot", memberUserId, SOURCE_TEAM_ID, "2036-09-22");

    String token = login(username, "123456");
    JsonNode list =
        getJson(
                "/api/pingan/three-checks/quick-shot/records"
                    + "?dateStart=2036-09-21&dateEnd=2036-09-22&status=all",
                token)
            .path("data");

    assertThat(recordNosIn(list.path("items"))).containsExactly("TCR-QS-MEMBER-OWN");
  }

  @Test
  void teamLeaderGenericRecordQueriesReturnTeamAndOwnedRowsOnly() throws Exception {
    long leaderUserId = 9102L;
    cleanupThreeCheckAccessFixture(leaderUserId, "team_leader_three_check", "TCR-TL-");
    createThreeCheckRoleUser(
        leaderUserId,
        "team_leader_three_check",
        "班长可见性用户",
        SOURCE_DEPARTMENT_ID,
        "TEAM_LEADER");
    createTeamLeaderMember(SOURCE_TEAM_ID, leaderUserId, "team_leader_three_check", "班长可见性用户");
    insertRecordForEnterpriseQuery(
        "TCR-TL-TEAM", "pre-shift-inspection", 2L, SOURCE_TEAM_ID, "2036-07-11");
    insertRecordForEnterpriseQuery(
        "TCR-TL-OWNED", "pre-shift-inspection", leaderUserId, 1011002L, "2036-07-12");
    insertRecordForEnterpriseQuery(
        "TCR-TL-HIDDEN", "pre-shift-inspection", 2L, 1011002L, "2036-07-13");

    String token = login("team_leader_three_check", "123456");
    JsonNode list =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records"
                    + "?dateStart=2036-07-11&dateEnd=2036-07-13&status=all",
                token)
            .path("data");
    JsonNode statistics =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/statistics"
                    + "?dateStart=2036-07-11&dateEnd=2036-07-13&status=all",
                token)
            .path("data");

    assertThat(list.path("total").asInt()).isEqualTo(2);
    assertThat(recordNosIn(list.path("items")))
        .containsExactlyInAnyOrder("TCR-TL-TEAM", "TCR-TL-OWNED");
    assertThat(statistics.path("total").asInt()).isEqualTo(2);
  }

  @Test
  void curtainWallLeaderGenericRecordQueriesReturnOnlyCurtainWallRowsInsideScope()
      throws Exception {
    long leaderUserId = 9103L;
    cleanupThreeCheckAccessFixture(leaderUserId, "curtain_wall_three_check", "TCR-CW-");
    createThreeCheckRoleUser(
        leaderUserId,
        "curtain_wall_three_check",
        "幕墙领导可见性用户",
        SOURCE_COMPANY_ID,
        "CURTAIN_WALL_LEADER");
    insertRecordForEnterpriseQuery(
        "TCR-CW-VISIBLE", "curtain-wall-routine-check", 2L, SOURCE_TEAM_ID, "2036-07-21");
    insertRecordForEnterpriseQuery(
        "TCR-CW-GENERIC", "pre-shift-inspection", 2L, SOURCE_TEAM_ID, "2036-07-22");
    insertRecordForEnterpriseQuery(
        "TCR-CW-OUTSIDE", "curtain-wall-routine-check", 4L, 10L, "2036-07-23");

    String token = login("curtain_wall_three_check", "123456");
    JsonNode curtainWallList =
        getJson(
                "/api/pingan/three-checks/curtain-wall-routine-check/records"
                    + "?dateStart=2036-07-21&dateEnd=2036-07-23&status=all",
                token)
            .path("data");
    JsonNode curtainWallStatistics =
        getJson(
                "/api/pingan/three-checks/curtain-wall-routine-check/records/statistics"
                    + "?dateStart=2036-07-21&dateEnd=2036-07-23&status=all",
                token)
            .path("data");
    JsonNode genericList =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records"
                    + "?dateStart=2036-07-21&dateEnd=2036-07-23&status=all",
                token)
            .path("data");

    assertThat(curtainWallList.path("total").asInt()).isEqualTo(1);
    assertThat(recordNosIn(curtainWallList.path("items"))).containsExactly("TCR-CW-VISIBLE");
    assertThat(curtainWallStatistics.path("total").asInt()).isEqualTo(1);
    assertThat(genericList.path("total").asInt()).isZero();
  }

  @Test
  void curtainWallLeaderCanViewGenericChildrenOnlyFromCurtainWallDispatchRoot()
      throws Exception {
    long leaderUserId = 9131L;
    cleanupThreeCheckAccessFixture(leaderUserId, "curtain_wall_child_three_check", "TCR-CWC-");
    createThreeCheckRoleUser(
        leaderUserId,
        "curtain_wall_child_three_check",
        "幕墙子记录可见性用户",
        SOURCE_COMPANY_ID,
        "CURTAIN_WALL_LEADER");
    long curtainWallRootId =
        insertRecordForEnterpriseQueryAndReturnId(
            "TCR-CWC-CURTAIN-ROOT",
            "curtain-wall-team-dispatch",
            2L,
            SOURCE_TEAM_ID,
            "2036-07-24",
            "OPENED");
    long curtainWallChildId =
        insertRecordForEnterpriseQueryAndReturnId(
            "TCR-CWC-CURTAIN-CHILD",
            "pre-shift-inspection",
            2L,
            SOURCE_TEAM_ID,
            "2036-07-24",
            "DRAFT");
    long ordinaryRootId =
        insertRecordForEnterpriseQueryAndReturnId(
            "TCR-CWC-ORDINARY-ROOT",
            "team-dispatch",
            2L,
            SOURCE_TEAM_ID,
            "2036-07-25",
            "OPENED");
    long ordinaryChildId =
        insertRecordForEnterpriseQueryAndReturnId(
            "TCR-CWC-ORDINARY-CHILD",
            "pre-shift-inspection",
            2L,
            SOURCE_TEAM_ID,
            "2036-07-25",
            "DRAFT");
    setRootDispatchRecordId(curtainWallChildId, curtainWallRootId);
    setRootDispatchRecordId(ordinaryChildId, ordinaryRootId);

    String token = login("curtain_wall_child_three_check", "123456");
    JsonNode genericList =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records"
                    + "?dateStart=2036-07-24&dateEnd=2036-07-25&status=all",
                token)
            .path("data");
    JsonNode curtainWallChildDetail =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/" + curtainWallChildId,
                token)
            .path("data");

    assertThat(genericList.path("total").asInt()).isEqualTo(1);
    assertThat(recordNosIn(genericList.path("items"))).containsExactly("TCR-CWC-CURTAIN-CHILD");
    assertThat(curtainWallChildDetail.path("recordNo").asText())
        .isEqualTo("TCR-CWC-CURTAIN-CHILD");
    mockMvc
        .perform(
            get("/api/pingan/three-checks/pre-shift-inspection/records/" + ordinaryChildId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void teamLeaderCannotDetailOrMutateOwnedRecordOutsideOrgScope() throws Exception {
    long leaderUserId = 9104L;
    long roleId = 9104L;
    cleanupThreeCheckAccessFixture(leaderUserId, "team_leader_cross_scope", "TCR-TL-OUTSIDE-");
    jdbcTemplate.update(
        "delete from sys_role_menu where role_id = ?",
        roleId);
    jdbcTemplate.update("delete from sys_role where id = ?", roleId);
    jdbcTemplate.update(
        "insert into sys_role (id, role_code, role_name, data_scope) values (?, 'TEAM_LEADER_CROSS_SCOPE_TEST', '跨范围班长测试角色', 'ORG_AND_CHILDREN')",
        roleId);
    jdbcTemplate.update(
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (?, 'team_leader_cross_scope', '{noop}123456', '跨范围班长用户', ?, 'ACTIVE', 0)",
        leaderUserId,
        SOURCE_DEPARTMENT_ID);
    jdbcTemplate.update(
        "insert into sys_user_role (user_id, role_id) values (?, ?)",
        leaderUserId,
        roleId);
    grantThreeCheckPermissions(
        leaderUserId,
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VOID",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND");
    long draftId =
        insertRecordForEnterpriseQueryAndReturnId(
            "TCR-TL-OUTSIDE-DRAFT", "pre-shift-inspection", leaderUserId, 10L, "2026-07-31", "DRAFT");
    long openedId =
        insertRecordForEnterpriseQueryAndReturnId(
            "TCR-TL-OUTSIDE-OPENED", "pre-shift-inspection", leaderUserId, 10L, "2026-08-01", "OPENED");

    String token = login("team_leader_cross_scope", "123456");

    mockMvc
        .perform(get("/api/pingan/three-checks/pre-shift-inspection/records/" + draftId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
    assertForbiddenMutationLeavesRecordUnchanged(
        token,
        "/api/pingan/three-checks/pre-shift-inspection/records/" + draftId + "/submit",
        Map.of(),
        draftId,
        "DRAFT",
        0);
    assertForbiddenMutationLeavesRecordUnchanged(
        token,
        "/api/pingan/three-checks/pre-shift-inspection/records/" + openedId + "/withdraw",
        Map.of("reason", "跨范围撤回"),
        openedId,
        "OPENED",
        0);
    assertForbiddenMutationLeavesRecordUnchanged(
        token,
        "/api/pingan/three-checks/pre-shift-inspection/records/" + openedId + "/remind",
        Map.of(),
        openedId,
        "OPENED",
        0);
  }

  @Test
  void broaderRoleCombinedWithTeamLeaderSkipsTeamLeaderNarrowing() throws Exception {
    long userId = 9105L;
    cleanupThreeCheckAccessFixture(userId, "team_group_three_check", "TCR-TG-");
    createThreeCheckRoleUser(
        userId,
        "team_group_three_check",
        "班长集团复合用户",
        SOURCE_COMPANY_ID,
        "TEAM_LEADER",
        "GROUP_LEADER");
    insertRecordForEnterpriseQuery(
        "TCR-TG-FIRST", "pre-shift-inspection", 2L, SOURCE_TEAM_ID, "2026-08-11");
    insertRecordForEnterpriseQuery(
        "TCR-TG-SECOND", "pre-shift-inspection", 2L, 1011002L, "2026-08-12");

    String token = login("team_group_three_check", "123456");
    JsonNode list =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records"
                    + "?dateStart=2026-08-11&dateEnd=2026-08-12&status=all",
                token)
            .path("data");

    assertThat(recordNosIn(list.path("items")))
        .containsExactlyInAnyOrder("TCR-TG-FIRST", "TCR-TG-SECOND");
  }

  @Test
  void broaderRoleCombinedWithCurtainWallLeaderSkipsCurtainWallNarrowing() throws Exception {
    long userId = 9106L;
    cleanupThreeCheckAccessFixture(userId, "curtain_group_three_check", "TCR-CG-");
    createThreeCheckRoleUser(
        userId,
        "curtain_group_three_check",
        "幕墙集团复合用户",
        SOURCE_COMPANY_ID,
        "CURTAIN_WALL_LEADER",
        "GROUP_LEADER");
    insertRecordForEnterpriseQuery(
        "TCR-CG-GENERIC", "pre-shift-inspection", 2L, SOURCE_TEAM_ID, "2026-08-21");

    String token = login("curtain_group_three_check", "123456");
    JsonNode list =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records"
                    + "?dateStart=2026-08-21&dateEnd=2026-08-21&status=all",
                token)
            .path("data");

    assertThat(recordNosIn(list.path("items"))).containsExactly("TCR-CG-GENERIC");
  }

  @Test
  void selfScopedTeamLeaderQueriesRemainOwnerOnly() throws Exception {
    long userId = 9107L;
    cleanupThreeCheckAccessFixture(userId, "self_team_leader_three_check", "TCR-STL-");
    String originalTeamLeaderScope =
        jdbcTemplate.queryForObject(
            "select data_scope from sys_role where role_code = 'TEAM_LEADER'",
            String.class);
    try {
      jdbcTemplate.update("update sys_role set data_scope = 'SELF' where role_code = 'TEAM_LEADER'");
      createThreeCheckRoleUser(
          userId,
          "self_team_leader_three_check",
          "本人班长复合用户",
          SOURCE_TEAM_ID,
          "TEAM_LEADER");
      insertRecordForEnterpriseQuery(
          "TCR-STL-OWNED", "pre-shift-inspection", userId, SOURCE_TEAM_ID, "2026-08-31");
      insertRecordForEnterpriseQuery(
          "TCR-STL-TEAM", "pre-shift-inspection", 2L, SOURCE_TEAM_ID, "2026-09-01");

      String token = login("self_team_leader_three_check", "123456");
      JsonNode list =
          getJson(
                  "/api/pingan/three-checks/pre-shift-inspection/records"
                      + "?dateStart=2026-08-31&dateEnd=2026-09-01&status=all",
                  token)
              .path("data");

      assertThat(recordNosIn(list.path("items"))).containsExactly("TCR-STL-OWNED");
    } finally {
      jdbcTemplate.update(
          "update sys_role set data_scope = ? where role_code = 'TEAM_LEADER'",
          originalTeamLeaderScope);
    }
  }

  @Test
  void selfScopedCurtainWallLeaderQueriesRemainOwnerOnly() throws Exception {
    long userId = 9108L;
    cleanupThreeCheckAccessFixture(userId, "self_curtain_leader_three_check", "TCR-SCW-");
    String originalCurtainWallLeaderScope =
        jdbcTemplate.queryForObject(
            "select data_scope from sys_role where role_code = 'CURTAIN_WALL_LEADER'",
            String.class);
    try {
      jdbcTemplate.update(
          "update sys_role set data_scope = 'SELF' where role_code = 'CURTAIN_WALL_LEADER'");
      createThreeCheckRoleUser(
          userId,
          "self_curtain_leader_three_check",
          "本人幕墙领导用户",
          SOURCE_TEAM_ID,
          "CURTAIN_WALL_LEADER");
      long ownedGenericRecordId =
          insertRecordForEnterpriseQueryAndReturnId(
              "TCR-SCW-OWNED",
              "pre-shift-inspection",
              userId,
              SOURCE_TEAM_ID,
              "2026-09-11",
              "DRAFT");
      insertRecordForEnterpriseQuery(
          "TCR-SCW-OTHER",
          "pre-shift-inspection",
          2L,
          SOURCE_TEAM_ID,
          "2026-09-12");

      String token = login("self_curtain_leader_three_check", "123456");
      JsonNode list =
          getJson(
                  "/api/pingan/three-checks/pre-shift-inspection/records"
                      + "?dateStart=2026-09-11&dateEnd=2026-09-12&status=all",
                  token)
              .path("data");
      JsonNode detail =
          getJson(
                  "/api/pingan/three-checks/pre-shift-inspection/records/" + ownedGenericRecordId,
                  token)
              .path("data");

      assertThat(recordNosIn(list.path("items"))).containsExactly("TCR-SCW-OWNED");
      assertThat(detail.path("recordNo").asText()).isEqualTo("TCR-SCW-OWNED");
    } finally {
      jdbcTemplate.update(
          "update sys_role set data_scope = ? where role_code = 'CURTAIN_WALL_LEADER'",
          originalCurtainWallLeaderScope);
    }
  }

  @Test
  void departmentManagerCanManageDispatchButCannotCreateInspectionTask() throws Exception {
    long userId = 9110L;
    String username = "department_manager_dispatch_only";
    cleanupThreeCheckAccessFixture(userId, username, "TCR-DM-");
    createThreeCheckRoleUser(
        userId, username, "部门派班经理", SOURCE_DEPARTMENT_ID, "DEPARTMENT_MANAGER");

    String token = login(username, "123456");
    JsonNode dispatch =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2036-10-01",
                    "status", "未生效",
                    "payload", Map.of("teamTask", "部门经理派班", "dispatchStatus", "未生效")))
            .path("data");
    assertThat(dispatch.path("moduleKey").asText()).isEqualTo("team-dispatch");

    mockMvc
        .perform(
            post("/api/pingan/three-checks/pre-shift-inspection/records")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "companyId", SOURCE_COMPANY_ID,
                            "departmentId", SOURCE_DEPARTMENT_ID,
                            "teamId", SOURCE_TEAM_ID,
                            "ownerUserId", 2,
                            "businessDate", "2036-10-02",
                            "status", "待检查",
                            "payload", Map.of("owner", "部门经理不应执行普通三查")))))
        .andExpect(status().isForbidden());
  }

  @Test
  void createsUpdatesSubmitsWithdrawsRemindsAndUploadsGenericRecord() throws Exception {
    String token = login("admin", "123456");
    String today = futureBusinessDate();

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", today,
                    "payload", preShiftInspectionPayload("未上传")))
            .path("data");

    String id = created.path("id").asText();
    assertThat(created.path("status").asText()).isEqualTo("DRAFT");
    assertThat(created.path("payload").path("statusLabel").asText()).isEqualTo("待检查");

    JsonNode updated =
        putJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/" + id,
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", today,
                    "payload", preShiftInspectionPayload("现场照片"),
                    "version", 0))
            .path("data");
    assertThat(updated.path("payload").path("imageCheck").asText()).isEqualTo("现场照片");
    assertThat(updated.path("version").asInt()).isEqualTo(1);

    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/" + id + "/submit",
                token,
                Map.of())
            .path("data");
    assertThat(submitted.path("status").asText()).isEqualTo("OPENED");
    assertThat(submitted.path("statusLabel").asText()).isEqualTo("已检查");
    assertThat(submitted.path("payload").path("statusLabel").asText()).isEqualTo("已检查");
    assertThat(submitted.path("canRemind").asBoolean()).isTrue();

    Integer beforeRemindCount =
        jdbcTemplate.queryForObject(
            "select count(*) from biz_remind_record where biz_type = 'THREE_CHECK_PRE_SHIFT_INSPECTION' and biz_id = ?",
            Integer.class,
            Long.parseLong(id));
    JsonNode reminded =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/" + id + "/remind",
                token,
                Map.of())
            .path("data");
    assertThat(reminded.path("reminderCount").asInt()).isEqualTo(1);
    Integer afterRemindCount =
        jdbcTemplate.queryForObject(
            "select count(*) from biz_remind_record where biz_type = 'THREE_CHECK_PRE_SHIFT_INSPECTION' and biz_id = ?",
            Integer.class,
            Long.parseLong(id));
    assertThat(afterRemindCount).isEqualTo(beforeRemindCount + 1);

    MockMultipartFile image =
        new MockMultipartFile(
            "file",
            "inspection.jpg",
            "image/jpeg",
            tinyJpeg());
    JsonNode uploadResponse =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        multipart(
                                "/api/pingan/three-checks/pre-shift-inspection/records/"
                                    + id
                                    + "/attachments")
                            .file(image)
                            .param("fileKind", "IMAGE")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");
    assertThat(uploadResponse.path("url").asText()).startsWith("/api/attachments/");

    JsonNode detail =
        getJson("/api/pingan/three-checks/pre-shift-inspection/records/" + id, token)
            .path("data");
    assertThat(detail.path("imageCheck").asText()).isEqualTo("现场照片");
    assertThat(detail.path("attachments")).hasSize(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from biz_attachment where biz_type = 'THREE_CHECK_PRE_SHIFT_INSPECTION' and biz_id = ? and file_kind = 'IMAGE'",
                Integer.class,
                Long.parseLong(id)))
        .isEqualTo(1);

    JsonNode withdrawn =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/" + id + "/withdraw",
                token,
                Map.of("reason", "检查项需要补充"))
            .path("data");
    assertThat(withdrawn.path("status").asText()).isEqualTo("WITHDRAWN");
    assertThat(withdrawn.path("statusLabel").asText()).isEqualTo("待检查");
    assertThat(withdrawn.path("payload").path("statusLabel").asText()).isEqualTo("待检查");
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from biz_status_log where biz_type = 'THREE_CHECK_PRE_SHIFT_INSPECTION' and biz_id = ? and action = 'WITHDRAW'",
                Integer.class,
                Long.parseLong(id)))
        .isEqualTo(1);
  }

  @Test
  void teamLeaderSubmitAssignsOneShiftInspectionOwnerToSubmitter() throws Exception {
    long memberUserId = 903_221L;
    String memberUsername = "submit_owner_member";
    cleanupThreeCheckAccessFixture(memberUserId, memberUsername, "TCR-TL-SUBMIT-OWNER-");
    createThreeCheckRoleUser(memberUserId, memberUsername, "提交前组员", SOURCE_TEAM_ID);
    createTeamMember(SOURCE_TEAM_ID, memberUserId, memberUsername, "提交前组员");
    String token = login("HB_MONITOR", "123456");

    try {
      for (String moduleKey :
          List.of(
              "pre-shift-meeting",
              "pre-shift-inspection",
              "mid-shift-inspection",
              "post-shift-inspection")) {
        long id =
            insertSubmitReadyOneShiftRecord(
                "TCR-TL-SUBMIT-OWNER-" + moduleKey,
                moduleKey,
                memberUserId,
                "2036-10-"
                    + switch (moduleKey) {
                      case "pre-shift-meeting" -> "01";
                      case "pre-shift-inspection" -> "02";
                      case "mid-shift-inspection" -> "03";
                      default -> "04";
                    });

        JsonNode submitted =
            postJson(
                    "/api/pingan/three-checks/" + moduleKey + "/records/" + id + "/submit",
                    token,
                    Map.of())
                .path("data");

        assertThat(submitted.path("ownerUserId").asLong()).isEqualTo(2L);
        assertThat(submitted.path("owner").asText()).isEqualTo("湖贝班长");
        assertThat(submitted.path("payload").path("owner").asText()).isEqualTo("湖贝班长");
        if (!"pre-shift-meeting".equals(moduleKey)) {
          assertThat(submitted.path("payload").path("responsiblePerson").asText())
              .isEqualTo("湖贝班长");
        }

        JsonNode list =
            getJson("/api/pingan/three-checks/" + moduleKey + "/records?status=all", token)
                .path("data")
                .path("items");
        JsonNode row = firstItemById(list, String.valueOf(id));
        assertThat(row.path("owner").asText()).isEqualTo("湖贝班长");
        assertThat(row.path("payload").path("owner").asText()).isEqualTo("湖贝班长");
      }
    } finally {
      cleanupThreeCheckAccessFixture(memberUserId, memberUsername, "TCR-TL-SUBMIT-OWNER-");
    }
  }

  @Test
  void deletesAttachmentFromThreeCheckRecordDetail() throws Exception {
    String token = login("admin", "123456");
    JsonNode created =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-05-18",
                    "payload",
                    Map.of("statusLabel", "待检查", "checkItems", List.of())))
            .path("data");
    String id = created.path("id").asText();
    MockMultipartFile image =
        new MockMultipartFile(
            "file",
            "inspection-delete.jpg",
            "image/jpeg",
            tinyJpeg());
    JsonNode attachment =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        multipart(
                                "/api/pingan/three-checks/pre-shift-inspection/records/"
                                    + id
                                    + "/attachments")
                            .file(image)
                            .param("fileKind", "IMAGE")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");

    mockMvc
        .perform(
            delete(
                    "/api/pingan/three-checks/pre-shift-inspection/records/"
                        + id
                        + "/attachments/"
                        + attachment.path("id").asText())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    JsonNode detail =
        getJson("/api/pingan/three-checks/pre-shift-inspection/records/" + id, token)
            .path("data");
    assertThat(detail.path("imageCheck").asText()).isEqualTo("未上传");
    assertThat(detail.path("attachments")).isEmpty();
    assertThat(
            jdbcTemplate.queryForObject(
                "select deleted from biz_attachment where id = ?",
                Integer.class,
                Long.parseLong(attachment.path("id").asText())))
        .isEqualTo(1);
  }

  @Test
  void createsUpdatesAndFiltersPointsFlowRecords() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/points-flow/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-05-22",
                    "payload",
                        Map.of(
                            "createdAt", "2026-05-22 17:45:21",
                            "user", "湖贝班长",
                            "pointsReason", "安全学习奖励",
                            "pointsChange", "加分",
                            "pointsQuantity", 5)))
            .path("data");

    String id = created.path("id").asText();
    assertThat(created.path("recordNo").asText()).startsWith("PF-");
    assertThat(created.path("moduleKey").asText()).isEqualTo("points-flow");
    assertThat(created.path("status").asText()).isEqualTo("OPENED");
    assertThat(created.path("statusLabel").asText()).isEqualTo("加分");
    assertThat(created.path("payload").path("createdAt").asText()).isEqualTo("2026-05-22 17:45:21");
    assertThat(created.path("payload").path("pointsChange").asText()).isEqualTo("加分");
    assertThat(created.path("payload").path("pointsQuantity").asInt()).isEqualTo(5);

    JsonNode updated =
        putJson(
                "/api/pingan/three-checks/points-flow/records/" + id,
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-05-22",
                    "payload",
                        Map.of(
                            "createdAt", "2026-05-22 17:45:21",
                            "user", "湖贝班长",
                            "pointsReason", "自动售货机兑换",
                            "pointsChange", "兑换",
                            "pointsQuantity", 3,
                            "vendingMachine", "一号机",
                            "goods", "矿泉水"),
                    "version", 0))
            .path("data");

    assertThat(updated.path("recordNo").asText()).isEqualTo(created.path("recordNo").asText());
    assertThat(updated.path("payload").path("createdAt").asText()).isEqualTo("2026-05-22 17:45:21");
    assertThat(updated.path("payload").path("pointsChange").asText()).isEqualTo("兑换");
    assertThat(updated.path("payload").path("vendingMachine").asText()).isEqualTo("一号机");
    assertThat(updated.path("payload").path("goods").asText()).isEqualTo("矿泉水");

    JsonNode filtered =
        getJson(
                "/api/pingan/three-checks/points-flow/records"
                    + "?dateStart=2026-05-22&dateEnd=2026-05-22&pointsReason=售货机",
                token)
            .path("data");

    assertThat(filtered.path("total").asInt()).isGreaterThanOrEqualTo(1);
    List<String> ids = idsIn(filtered.path("items"));
    assertThat(ids).contains(id);
    for (JsonNode row : filtered.path("items")) {
      assertThat(row.path("payload").path("pointsReason").asText()).contains("售货机");
    }
  }

  @Test
  void pointsFlowRecordListRequiresPointsViewPermissions() throws Exception {
    long threeCheckOnlyUserId = 9521L;
    long pointsViewerUserId = 9522L;
    String recordNo = "PF-RBAC-VIEW-20261106";
    jdbcTemplate.update("delete from three_check_record where record_no = ?", recordNo);
    long recordId =
        insertRecordForEnterpriseQueryAndReturnId(
            recordNo,
            "points-flow",
            pointsViewerUserId,
            SOURCE_TEAM_ID,
            "2026-11-06",
            "OPENED");
    createPermissionFixtureUser(
        threeCheckOnlyUserId,
        9521L,
        "points_flow_three_check_only",
        "积分流水三查查看用户",
        "POINTS_FLOW_THREE_CHECK_ONLY",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW");
    createPermissionFixtureUser(
        pointsViewerUserId,
        9522L,
        "points_flow_viewer",
        "积分流水查看用户",
        "POINTS_FLOW_VIEWER",
        "PINGAN_POINTS_FLOW_VIEW");

    String threeCheckOnlyToken = login("points_flow_three_check_only", "123456");
    mockMvc
        .perform(
            get("/api/pingan/three-checks/points-flow/records?status=all")
                .header("Authorization", "Bearer " + threeCheckOnlyToken))
        .andExpect(status().isForbidden());

    String pointsViewerToken = login("points_flow_viewer", "123456");
    JsonNode list =
        getJson(
                "/api/pingan/three-checks/points-flow/records?status=all&dateStart=2026-11-06&dateEnd=2026-11-06",
                pointsViewerToken)
            .path("data");
    assertThat(list.path("total").asInt()).isEqualTo(1);
    assertThat(list.path("items").get(0).path("id").asLong()).isEqualTo(recordId);
  }

  @Test
  void oneShiftThreeCheckSubmodulePermissionsAreIsolated() throws Exception {
    long teamDispatchUserId = 9523L;
    long oneShiftUserId = 9524L;
    long safetyActivityUserId = 9525L;
    long meetingUserId = 9526L;
    createPermissionFixtureUser(
        teamDispatchUserId,
        9523L,
        "team_dispatch_creator",
        "班组派班新增用户",
        "TEAM_DISPATCH_CREATOR",
        "PINGAN_TEAM_DISPATCH_VIEW",
        "PINGAN_TEAM_DISPATCH_CREATE");
    createPermissionFixtureUser(
        oneShiftUserId,
        9524L,
        "one_shift_creator",
        "一班三查新增用户",
        "ONE_SHIFT_CREATOR",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE");
    createPermissionFixtureUser(
        safetyActivityUserId,
        9525L,
        "pre_shift_safety_activity_creator",
        "班前安全活动新增用户",
        "PRE_SHIFT_SAFETY_ACTIVITY_CREATOR",
        "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW",
        "PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_CREATE");
    createPermissionFixtureUser(
        meetingUserId,
        9526L,
        "pre_shift_meeting_creator",
        "班前会新增用户",
        "PRE_SHIFT_MEETING_CREATOR",
        "PINGAN_PRE_SHIFT_MEETING_VIEW",
        "PINGAN_PRE_SHIFT_MEETING_CREATE");

    String teamDispatchToken = login("team_dispatch_creator", "123456");
    JsonNode teamDispatch =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records",
                teamDispatchToken,
                genericThreeCheckPayload(teamDispatchUserId, "2026-11-20", "班组派班新增"))
            .path("data");
    assertThat(teamDispatch.path("moduleKey").asText()).isEqualTo("team-dispatch");
    mockMvc
        .perform(
            post("/api/pingan/three-checks/curtain-wall-team-dispatch/records")
                .header("Authorization", "Bearer " + teamDispatchToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        genericThreeCheckPayload(teamDispatchUserId, "2026-11-21", "幕墙派班越权"))))
        .andExpect(status().isForbidden());

    String oneShiftToken = login("one_shift_creator", "123456");
    mockMvc
        .perform(
            post("/api/pingan/three-checks/pre-shift-meeting/records")
                .header("Authorization", "Bearer " + oneShiftToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        genericThreeCheckPayload(
                            oneShiftUserId, "2026-11-22", "班前会权限已独立"))))
        .andExpect(status().isForbidden());
    JsonNode inspection =
        postJson(
                "/api/pingan/three-checks/mid-shift-inspection/records",
                oneShiftToken,
                genericThreeCheckPayload(oneShiftUserId, "2026-11-23", "班中检查并入一班三查"))
            .path("data");
    assertThat(inspection.path("moduleKey").asText()).isEqualTo("mid-shift-inspection");
    mockMvc
        .perform(
            post("/api/pingan/three-checks/pre-shift-safety-activity/records")
                .header("Authorization", "Bearer " + oneShiftToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        genericThreeCheckPayload(oneShiftUserId, "2026-11-24", "安全活动越权"))))
        .andExpect(status().isForbidden());

    String meetingToken = login("pre_shift_meeting_creator", "123456");
    JsonNode meeting =
        postJson(
                "/api/pingan/three-checks/pre-shift-meeting/records",
                meetingToken,
                genericThreeCheckPayload(meetingUserId, "2026-11-22", "班前会独立新增"))
            .path("data");
    assertThat(meeting.path("moduleKey").asText()).isEqualTo("pre-shift-meeting");

    String safetyActivityToken = login("pre_shift_safety_activity_creator", "123456");
    JsonNode activity =
        postJson(
                "/api/pingan/three-checks/pre-shift-safety-activity/records",
                safetyActivityToken,
                genericThreeCheckPayload(safetyActivityUserId, "2026-11-25", "班前安全活动"))
            .path("data");
    assertThat(activity.path("moduleKey").asText()).isEqualTo("pre-shift-safety-activity");
    mockMvc
        .perform(
            post("/api/pingan/three-checks/key-sites/records")
                .header("Authorization", "Bearer " + safetyActivityToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        genericThreeCheckPayload(safetyActivityUserId, "2026-11-26", "重点场所越权"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void oneShiftThreeCheckSubmitRequiresSubmoduleSubmitPermission() throws Exception {
    long createOnlyUserId = 9526L;
    long submitUserId = 9527L;
    createPermissionFixtureUser(
        createOnlyUserId,
        9526L,
        "one_shift_create_only",
        "一班三查仅新增用户",
        "ONE_SHIFT_CREATE_ONLY",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE");
    createPermissionFixtureUser(
        submitUserId,
        9527L,
        "one_shift_submitter",
        "一班三查提交用户",
        "ONE_SHIFT_SUBMITTER",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT");

    String createOnlyToken = login("one_shift_create_only", "123456");
    JsonNode createOnlyDraft =
        postJson(
                "/api/pingan/three-checks/mid-shift-inspection/records",
                createOnlyToken,
                inspectionThreeCheckPayload(createOnlyUserId, "2026-11-27", "仅新增不能提交"))
            .path("data");
    mockMvc
        .perform(
            post(
                    "/api/pingan/three-checks/mid-shift-inspection/records/"
                        + createOnlyDraft.path("id").asText()
                        + "/submit")
                .header("Authorization", "Bearer " + createOnlyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());

    JsonNode submitDraft =
        postJson(
                "/api/pingan/three-checks/mid-shift-inspection/records",
                login("admin", "123456"),
                inspectionThreeCheckPayload(submitUserId, "2026-11-28", "提交权限可提交"))
            .path("data");
    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/mid-shift-inspection/records/"
                    + submitDraft.path("id").asText()
                    + "/submit",
                login("one_shift_submitter", "123456"),
                Map.of())
            .path("data");

    assertThat(submitted.path("status").asText()).isEqualTo("OPENED");
    assertThat(submitted.path("statusLabel").asText()).isEqualTo("已检查");
  }

  @Test
  void aggregatesSafetyPointsRankingFromPointsFlowRecords() throws Exception {
    String token = login("admin", "123456");

    createPointsFlowRecord(token, "2026-06-01", SOURCE_TEAM_ID, "榜单甲", "学习-应急管理", "加分", 20);
    createPointsFlowRecord(token, "2026-06-02", SOURCE_TEAM_ID, "榜单甲", "考试不通过", "扣分", 4);
    createPointsFlowRecord(token, "2026-06-03", 1011002L, "榜单乙", "学习-安全课程", "加分", 12);
    createPointsFlowRecord(token, "2026-06-03", 1011002L, "榜单乙", "积分兑换", "兑换", 2);
    createPointsFlowRecord(token, "2026-06-04", 1011003L, "榜单丙", "学习-我要安全", "加分", 8);

    JsonNode individual =
        getJson(
                "/api/pingan/safety-points/ranking/individual"
                    + "?dateStart=2026-06-01&dateEnd=2026-06-03&keyword=榜单&page=1&pageSize=10",
                token)
            .path("data");

    assertThat(individual.path("total").asInt()).isEqualTo(2);
    JsonNode firstIndividual = individual.path("items").get(0);
    JsonNode secondIndividual = individual.path("items").get(1);
    assertThat(firstIndividual.path("rank").asInt()).isEqualTo(1);
    assertThat(firstIndividual.path("userName").asText()).isEqualTo("榜单甲");
    assertThat(firstIndividual.path("currentScore").asInt()).isEqualTo(16);
    assertThat(firstIndividual.path("addScore").asInt()).isEqualTo(20);
    assertThat(firstIndividual.path("deductScore").asInt()).isEqualTo(4);
    assertThat(firstIndividual.path("redeemScore").asInt()).isEqualTo(0);
    assertThat(secondIndividual.path("rank").asInt()).isEqualTo(2);
    assertThat(secondIndividual.path("userName").asText()).isEqualTo("榜单乙");
    assertThat(secondIndividual.path("currentScore").asInt()).isEqualTo(10);

    JsonNode team =
        getJson(
                "/api/pingan/safety-points/ranking/team"
                    + "?dateStart=2026-06-01&dateEnd=2026-06-03&keyword=榜单&rankBy=TEAM&page=1&pageSize=10",
                token)
            .path("data");

    assertThat(team.path("total").asInt()).isEqualTo(2);
    assertThat(team.path("items").get(0).path("rankName").asText()).isEqualTo("幕墙组装1班");
    assertThat(team.path("items").get(0).path("currentScore").asInt()).isEqualTo(16);
    assertThat(team.path("items").get(1).path("rankName").asText()).isEqualTo("幕墙组装2班");
    assertThat(team.path("items").get(1).path("currentScore").asInt()).isEqualTo(10);

    JsonNode teamByDepartment =
        getJson(
                "/api/pingan/safety-points/ranking/team"
                    + "?dateStart=2026-06-01&dateEnd=2026-06-03&keyword=榜单&rankBy=DEPARTMENT&page=1&pageSize=10",
                token)
            .path("data");

    assertThat(teamByDepartment.path("total").asInt()).isEqualTo(1);
    assertThat(teamByDepartment.path("items").get(0).path("rankName").asText()).isEqualTo("幕墙组装");
    assertThat(teamByDepartment.path("items").get(0).path("currentScore").asInt()).isEqualTo(26);

    JsonNode overview =
        getJson(
                "/api/pingan/safety-points/ranking/overview"
                    + "?dateStart=2026-06-01&dateEnd=2026-06-03&keyword=榜单",
                token)
            .path("data");

    assertThat(overview.path("currentTotalScore").asInt()).isEqualTo(26);
    assertThat(overview.path("participantCount").asInt()).isEqualTo(2);
    assertThat(overview.path("monthlyAddScore").asInt()).isEqualTo(32);
    assertThat(overview.path("monthlyRedeemScore").asInt()).isEqualTo(2);

    JsonNode charts =
        getJson(
                "/api/pingan/safety-points/ranking/charts"
                    + "?dateStart=2026-06-01&dateEnd=2026-06-03&keyword=榜单",
                token)
            .path("data");

    assertThat(charts.path("trend").get(0).path("date").asText()).isEqualTo("2026-06-01");
    assertThat(charts.path("trend").get(0).path("score").asInt()).isEqualTo(20);
    assertThat(charts.path("trend").get(1).path("score").asInt()).isEqualTo(16);
    assertThat(charts.path("trend").get(2).path("score").asInt()).isEqualTo(26);
    assertThat(charts.path("sources").get(0).path("name").asText()).isEqualTo("学习-应急管理");
    assertThat(charts.path("teamTop5").get(0).path("name").asText()).isEqualTo("幕墙组装1班");

    JsonNode departmentCharts =
        getJson(
                "/api/pingan/safety-points/ranking/charts"
                    + "?dateStart=2026-06-01&dateEnd=2026-06-03&keyword=榜单&rankBy=DEPARTMENT",
                token)
            .path("data");
    assertThat(departmentCharts.path("teamTop5").get(0).path("name").asText()).isEqualTo("幕墙组装");
    assertThat(departmentCharts.path("teamTop5").get(0).path("score").asInt()).isEqualTo(26);

    JsonNode companyCharts =
        getJson(
                "/api/pingan/safety-points/ranking/charts"
                    + "?dateStart=2026-06-01&dateEnd=2026-06-03&keyword=榜单&rankBy=COMPANY",
                token)
            .path("data");
    assertThat(companyCharts.path("teamTop5").get(0).path("name").asText()).isEqualTo("广晟源成");
    assertThat(companyCharts.path("teamTop5").get(0).path("score").asInt()).isEqualTo(26);
  }

  @Test
  void safetyPointsRankingEndpointsAllowPointsViewWithoutManagePermission() throws Exception {
    String adminToken = login("admin", "123456");
    createPointsFlowRecord(adminToken, "2026-06-08", SOURCE_TEAM_ID, "查看权限用户", "安全学习", "加分", 6);
    createPermissionFixtureUser(
        9523L,
        9523L,
        "points_ranking_viewer",
        "积分榜单查看用户",
        "POINTS_RANKING_VIEWER",
        "PINGAN_POINTS_RANKING_VIEW");
    jdbcTemplate.update(
        "update sys_role set data_scope = 'ORG_AND_CHILDREN' where role_code = 'POINTS_RANKING_VIEWER'");

    String viewerToken = login("points_ranking_viewer", "123456");
    String query = "?dateStart=2026-06-08&dateEnd=2026-06-08&keyword=查看权限";

    mockMvc
        .perform(
            get("/api/pingan/three-checks/points-flow/records?status=all")
                .header("Authorization", "Bearer " + viewerToken))
        .andExpect(status().isForbidden());

    assertThat(getJson("/api/pingan/safety-points/ranking/overview" + query, viewerToken).path("data").path("participantCount").asInt())
        .isEqualTo(1);
    assertThat(getJson("/api/pingan/safety-points/ranking/charts" + query, viewerToken).path("data").path("trend"))
        .hasSize(1);
    assertThat(getJson("/api/pingan/safety-points/ranking/individual" + query, viewerToken).path("data").path("total").asInt())
        .isEqualTo(1);
    assertThat(getJson("/api/pingan/safety-points/ranking/team" + query + "&rankBy=TEAM", viewerToken).path("data").path("total").asInt())
        .isEqualTo(1);
  }

  @Test
  void scopesSafetyPointsRankingChartsBySelectedTeamRankDimension() throws Exception {
    String token = login("admin", "123456");

    createPointsFlowRecord(token, "2026-06-10", SOURCE_TEAM_ID, "图表维度甲", "学习-应急管理", "加分", 30);
    createPointsFlowRecord(token, "2026-06-11", 1011002L, "图表维度乙", "学习-安全课程", "加分", 5);
    createPointsFlowRecord(token, "2026-06-10", 1011003L, "图表维度丙", "学习-我要安全", "加分", 20);

    JsonNode teamCharts =
        getJson(
                "/api/pingan/safety-points/ranking/charts"
                    + "?dateStart=2026-06-10&dateEnd=2026-06-11&keyword=图表维度&rankBy=TEAM",
                token)
            .path("data");
    assertThat(teamCharts.path("teamTop5").get(0).path("name").asText()).isEqualTo("幕墙组装1班");
    assertThat(teamCharts.path("trend").size()).isEqualTo(1);
    assertThat(teamCharts.path("trend").get(0).path("score").asInt()).isEqualTo(30);
    assertThat(teamCharts.path("sources").get(0).path("name").asText()).isEqualTo("学习-应急管理");

    JsonNode departmentCharts =
        getJson(
                "/api/pingan/safety-points/ranking/charts"
                    + "?dateStart=2026-06-10&dateEnd=2026-06-11&keyword=图表维度&rankBy=DEPARTMENT",
                token)
            .path("data");
    assertThat(departmentCharts.path("teamTop5").get(0).path("name").asText()).isEqualTo("幕墙组装");
    assertThat(departmentCharts.path("trend").size()).isEqualTo(2);
    assertThat(departmentCharts.path("trend").get(0).path("score").asInt()).isEqualTo(30);
    assertThat(departmentCharts.path("trend").get(1).path("score").asInt()).isEqualTo(35);
    assertThat(namesIn(departmentCharts.path("sources"))).containsExactly("学习-应急管理", "学习-安全课程");

    JsonNode companyCharts =
        getJson(
                "/api/pingan/safety-points/ranking/charts"
                    + "?dateStart=2026-06-10&dateEnd=2026-06-11&keyword=图表维度&rankBy=COMPANY",
                token)
            .path("data");
    assertThat(companyCharts.path("teamTop5").get(0).path("name").asText()).isEqualTo("广晟源成");
    assertThat(companyCharts.path("trend").size()).isEqualTo(2);
    assertThat(companyCharts.path("trend").get(0).path("score").asInt()).isEqualTo(50);
    assertThat(companyCharts.path("trend").get(1).path("score").asInt()).isEqualTo(55);
    assertThat(namesIn(companyCharts.path("sources")))
        .containsExactly("学习-应急管理", "学习-我要安全", "学习-安全课程");
  }

  @Test
  void deletesGenericRecordWithSoftDeleteAndKeepsAuditData() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/safety-check/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-05-20",
                    "payload",
                    Map.of("statusLabel", "待检查", "hazardDescription", "delete hazard")))
            .path("data");
    String id = created.path("id").asText();

    MockMultipartFile image =
        new MockMultipartFile(
            "file",
            "delete-hazard.jpg",
            "image/jpeg",
            tinyJpeg());
    mockMvc
        .perform(
            multipart("/api/pingan/three-checks/safety-check/records/" + id + "/attachments")
                .file(image)
                .param("fileKind", "IMAGE")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    deleteJson("/api/pingan/three-checks/safety-check/records/" + id, token);

    assertThat(
            jdbcTemplate.queryForObject(
                "select deleted from three_check_record where id = ?",
                Integer.class,
                Long.parseLong(id)))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from biz_attachment where biz_type = 'HAZARD_SAFETY_CHECK' and biz_id = ? and deleted = 0",
                Integer.class,
                Long.parseLong(id)))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from biz_status_log where biz_type = 'HAZARD_SAFETY_CHECK' and biz_id = ? and action = 'DELETE'",
                Integer.class,
                Long.parseLong(id)))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from biz_change_history where biz_type = 'HAZARD_SAFETY_CHECK' and biz_id = ? and action = 'DELETE'",
                Integer.class,
                Long.parseLong(id)))
        .isEqualTo(1);

    JsonNode list =
        getJson(
                "/api/pingan/three-checks/safety-check/records"
                    + "?dateStart=2026-05-20&dateEnd=2026-05-20&status=all",
                token)
            .path("data");
    assertThat(idsIn(list.path("items"))).doesNotContain(id);
    mockMvc
        .perform(
            get("/api/pingan/three-checks/safety-check/records/" + id)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void derivesAndFiltersOverdueWithoutChangingOriginalStatusAcrossPcAndMiniProgram()
      throws Exception {
    String token = login("admin", "123456");
    String yesterday = LocalDate.now(com.pingan.banzu.service.ThreeCheckOverdueSupport.BUSINESS_ZONE)
        .minusDays(1)
        .toString();
    String today = LocalDate.now(com.pingan.banzu.service.ThreeCheckOverdueSupport.BUSINESS_ZONE)
        .toString();
    String prefix = "TCR-OVERDUE-" + System.nanoTime();
    long draftId =
        insertRecordForEnterpriseQueryAndReturnId(
            prefix + "-DRAFT", "pre-shift-inspection", 2L, SOURCE_TEAM_ID, yesterday, "DRAFT");
    long withdrawnId =
        insertRecordForEnterpriseQueryAndReturnId(
            prefix + "-WITHDRAWN",
            "pre-shift-inspection",
            2L,
            SOURCE_TEAM_ID,
            yesterday,
            "WITHDRAWN");
    long openedId =
        insertRecordForEnterpriseQueryAndReturnId(
        prefix + "-OPENED", "pre-shift-inspection", 2L, SOURCE_TEAM_ID, yesterday, "OPENED");
    long todayId =
        insertRecordForEnterpriseQueryAndReturnId(
        prefix + "-TODAY", "pre-shift-inspection", 2L, SOURCE_TEAM_ID, today, "DRAFT");

    try {
      JsonNode pcList =
          getJson(
                  "/api/pingan/three-checks/pre-shift-inspection/records?overdue=true&pageSize=100",
                  token)
              .path("data")
              .path("items");
      JsonNode miniList =
          getJson(
                  "/api/mini/pingan/three-checks/pre-shift-inspection/records?overdue=true&pageSize=100",
                  token)
              .path("data")
              .path("items");

      JsonNode pcDraft = findRecordById(pcList, draftId);
      JsonNode pcWithdrawn = findRecordById(pcList, withdrawnId);
      JsonNode miniDraft = findRecordById(miniList, draftId);
      assertThat(pcDraft.path("overdue").asBoolean()).isTrue();
      assertThat(pcDraft.path("status").asText()).isEqualTo("DRAFT");
      assertThat(pcDraft.path("canSubmit").asBoolean()).isFalse();
      assertThat(pcWithdrawn.path("overdue").asBoolean()).isTrue();
      assertThat(pcWithdrawn.path("status").asText()).isEqualTo("WITHDRAWN");
      assertThat(pcWithdrawn.path("canSubmit").asBoolean()).isFalse();
      assertThat(miniDraft.path("overdue").asBoolean()).isTrue();
      assertThat(miniDraft.path("status").asText()).isEqualTo("DRAFT");
      assertThat(miniDraft.path("canSubmit").asBoolean()).isFalse();

      JsonNode detail =
          getJson(
                  "/api/pingan/three-checks/pre-shift-inspection/records/" + draftId,
                  token)
              .path("data");
      assertThat(detail.path("overdue").asBoolean()).isTrue();
      assertThat(detail.path("status").asText()).isEqualTo("DRAFT");
      assertThat(detail.path("canSubmit").asBoolean()).isFalse();

      JsonNode openedDetail =
          getJson(
                  "/api/pingan/three-checks/pre-shift-inspection/records/" + openedId,
                  token)
              .path("data");
      assertThat(openedDetail.path("canWithdraw").asBoolean()).isFalse();

      JsonNode todayDetail =
          getJson(
                  "/api/pingan/three-checks/pre-shift-inspection/records/" + todayId,
                  token)
              .path("data");
      assertThat(todayDetail.path("canSubmit").asBoolean()).isTrue();

      mockMvc
          .perform(
              post("/api/pingan/three-checks/pre-shift-inspection/records/" + draftId + "/submit")
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
      mockMvc
          .perform(
              post("/api/mini/pingan/three-checks/pre-shift-inspection/records/" + draftId + "/submit")
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
      mockMvc
          .perform(
              post("/api/pingan/three-checks/pre-shift-inspection/records/" + openedId + "/withdraw")
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"reason\":\"历史记录不可撤回\"}"))
          .andExpect(status().isBadRequest());
      mockMvc
          .perform(
              put("/api/pingan/three-checks/pre-shift-inspection/records/" + draftId)
                  .header("Authorization", "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "companyId", SOURCE_COMPANY_ID,
                              "departmentId", SOURCE_DEPARTMENT_ID,
                              "teamId", SOURCE_TEAM_ID,
                              "ownerUserId", 2,
                              "businessDate", today,
                              "version", 0,
                              "payload", Map.of("statusLabel", "待检查")))))
          .andExpect(status().isBadRequest());
    } finally {
      jdbcTemplate.update("delete from three_check_record where record_no like ?", prefix + "%");
    }
  }

  @Test
  void createsSafetyCheckWithoutTeamFromMiniProgram() throws Exception {
    String token = login("admin", "123456");
    Map<String, Object> payload = new HashMap<>();
    payload.put("companyId", SOURCE_COMPANY_ID);
    payload.put("departmentId", SOURCE_DEPARTMENT_ID);
    payload.put("ownerUserId", 2);
    payload.put("businessDate", "2026-05-21");
    payload.put("sourceRecordId", "mini-safety-check-no-team");
    payload.put("clientRequestId", "mini-safety-check-no-team-client");
    payload.put(
        "payload",
        Map.of(
            "inspectionUnit", "集团",
            "inspectedUnit", "广晟源成",
            "inspectionType", "综合检查",
            "statusLabel", "待检查"));

    JsonNode created =
        postJson("/api/mini/pingan/three-checks/safety-check/records", token, payload)
            .path("data");

    assertThat(created.path("moduleKey").asText()).isEqualTo("safety-check");
    assertThat(created.path("teamId").isNull()).isTrue();
    assertThat(created.path("team").asText()).isEmpty();
  }

  @Test
  void submitsSafetyCheckWithoutTeamAndCreatesRectificationOrder() throws Exception {
    String token = login("admin", "123456");
    Map<String, Object> payload = new HashMap<>();
    payload.put("companyId", SOURCE_COMPANY_ID);
    payload.put("departmentId", SOURCE_DEPARTMENT_ID);
    payload.put("ownerUserId", 2);
    payload.put("businessDate", "2026-05-22");
    payload.put("sourceRecordId", "mini-safety-check-submit-no-team");
    payload.put("clientRequestId", "mini-safety-check-submit-no-team-client");
    payload.put(
        "payload",
        Map.of(
            "inspectionUnit", "集团",
            "inspectedUnit", "广晟源成",
            "inspectionType", "综合检查",
            "checkItems",
                List.of(
                    Map.of(
                        "checkItem", "检查通道",
                        "checkResult", "有隐患",
                        "hazardDescription", "通道堆放材料",
                        "rectificationMeasures", "立即清理")),
            "statusLabel", "待检查"));
    JsonNode created =
        postJson("/api/mini/pingan/three-checks/safety-check/records", token, payload)
            .path("data");

    JsonNode submitted =
        postJson(
                "/api/mini/pingan/three-checks/safety-check/records/"
                    + created.path("id").asText()
                    + "/submit",
                token,
                Map.of())
            .path("data");

    assertThat(submitted.path("teamId").isNull()).isTrue();
    assertThat(submitted.path("status").asText()).isEqualTo("OPENED");
    assertThat(submitted.path("statusLabel").asText()).isEqualTo("已检查");
    Integer orderCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from hazard_rectification_order
            where source_type = 'SAFETY_INSPECTION'
              and source_record_id = ?
              and team_id is null
              and deleted = 0
            """,
            Integer.class,
            Long.parseLong(created.path("id").asText()));
    assertThat(orderCount).isEqualTo(1);

    JsonNode pendingList =
        getJson(
                "/api/mini/pingan/three-checks/safety-check/records?status=OPENED&dateStart=2026-05-22&dateEnd=2026-05-22",
                token)
            .path("data");
    assertThat(idsIn(pendingList.path("items"))).contains(created.path("id").asText());
  }

  @Test
  void rejectsOrdinaryThreeCheckRecordWithoutTeam() throws Exception {
    String token = login("admin", "123456");
    Map<String, Object> payload = new HashMap<>();
    payload.put("companyId", SOURCE_COMPANY_ID);
    payload.put("departmentId", SOURCE_DEPARTMENT_ID);
    payload.put("ownerUserId", 2);
    payload.put("businessDate", "2026-05-21");
    payload.put("payload", Map.of("statusLabel", "待检查", "workContent", "班前检查"));

    mockMvc
        .perform(
            post("/api/pingan/three-checks/pre-shift-inspection/records")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void batchProcessesGenericRecordsWithPartialSuccess() throws Exception {
    String token = login("admin", "123456");
    JsonNode draft =
        createGenericRecord(
            token,
            "pre-shift-inspection",
            "待检查",
            Map.of("statusLabel", "待检查", "inspectionContent", "batch draft"),
            futureBusinessDate());
    JsonNode opened =
        createGenericRecord(
            token,
            "pre-shift-inspection",
            "已检查",
            Map.of("statusLabel", "已检查", "inspectionContent", "batch opened"));
    String draftId = draft.path("id").asText();
    String openedId = opened.path("id").asText();

    JsonNode submitBatch =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/batch",
                token,
                Map.of("action", "SUBMIT", "ids", List.of(draftId, openedId)))
            .path("data");

    assertThat(submitBatch.path("total").asInt()).isEqualTo(2);
    assertThat(submitBatch.path("successCount").asInt()).isEqualTo(1);
    assertThat(submitBatch.path("failureCount").asInt()).isEqualTo(1);
    assertThat(submitBatch.path("results").get(0).path("success").asBoolean()).isTrue();
    assertThat(submitBatch.path("results").get(1).path("success").asBoolean()).isFalse();
    assertThat(
            jdbcTemplate.queryForObject(
                "select status from three_check_record where id = ?",
                String.class,
                Long.parseLong(draftId)))
        .isEqualTo("OPENED");

    JsonNode deleteBatch =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/batch",
                token,
                Map.of("action", "DELETE", "ids", List.of(draftId, "999999999")))
            .path("data");

    assertThat(deleteBatch.path("successCount").asInt()).isEqualTo(1);
    assertThat(deleteBatch.path("failureCount").asInt()).isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select deleted from three_check_record where id = ?",
                Integer.class,
                Long.parseLong(draftId)))
        .isEqualTo(1);
  }

  @Test
  void exposesGenericRecordWorkflowFromRealStatusLogs() throws Exception {
    String token = login("admin", "123456");
    String today = futureBusinessDate();

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", today,
                    "payload", preShiftInspectionPayload("未上传")))
            .path("data");
    String id = created.path("id").asText();

    putJson(
        "/api/pingan/three-checks/pre-shift-inspection/records/" + id,
        token,
        Map.of(
            "companyId", SOURCE_COMPANY_ID,
            "departmentId", SOURCE_DEPARTMENT_ID,
            "teamId", SOURCE_TEAM_ID,
            "ownerUserId", 2,
            "businessDate", today,
            "payload", preShiftInspectionPayload("现场照片"),
            "version", 0));
    postJson(
        "/api/pingan/three-checks/pre-shift-inspection/records/" + id + "/submit",
        token,
        Map.of());
    postJson(
        "/api/pingan/three-checks/pre-shift-inspection/records/" + id + "/remind",
        token,
        Map.of());
    postJson(
        "/api/pingan/three-checks/pre-shift-inspection/records/" + id + "/withdraw",
        token,
        Map.of("reason", "检查项需要补充"));

    JsonNode workflow =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/" + id + "/workflow",
                token)
            .path("data");

    JsonNode documentFlow = workflow.path("documentFlow");
    JsonNode changeHistory = workflow.path("changeHistory");
    assertThat(idsIn(documentFlow).size()).isGreaterThanOrEqualTo(5);
    assertThat(actionsIn(documentFlow))
        .containsSubsequence("CREATE", "UPDATE", "SUBMIT", "REMIND", "WITHDRAW");
    assertThat(actionsIn(changeHistory).get(0)).isEqualTo("WITHDRAW");
    assertThat(documentFlow.get(0).path("actionLabel").asText()).isEqualTo("创建记录");
    assertThat(documentFlow.get(0).path("toStatusLabel").asText()).isEqualTo("待检查");
    assertThat(changeHistory.get(0).path("remark").asText()).isEqualTo("检查项需要补充");
    assertThat(changeHistory.get(0).path("operatorName").asText()).isNotBlank();
    assertThat(changeHistory.get(0).path("occurredAt").asText()).isNotBlank();
  }

  @Test
  void linksFullChainByRootDispatchRecordIdInsteadOfOnlyTeamAndDate() throws Exception {
    String token = login("admin", "123456");

    JsonNode firstDispatch =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-08-01",
                    "payload",
                        Map.of(
                            "teamTask", "上午吊装派班",
                            "dispatchTime", "2026-08-01 08:00",
                            "statusLabel", "已生效")))
            .path("data");
    JsonNode secondDispatch =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-08-01",
                    "payload",
                        Map.of(
                            "teamTask", "下午焊接派班",
                            "dispatchTime", "2026-08-01 14:00",
                            "statusLabel", "已生效")))
            .path("data");
    String firstRootId = firstDispatch.path("id").asText();
    String secondRootId = secondDispatch.path("id").asText();
    assertThat(firstDispatch.path("rootDispatchRecordId").asText()).isEqualTo(firstRootId);
    assertThat(secondDispatch.path("rootDispatchRecordId").asText()).isEqualTo(secondRootId);

    JsonNode firstMeeting =
        postJson(
                "/api/pingan/three-checks/pre-shift-meeting/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-08-01",
                    "rootDispatchRecordId", Long.parseLong(firstRootId),
                    "payload",
                        Map.of(
                            "meetingContent", "上午派班班前会",
                            "attendeesText", "湖贝班长",
                            "statusLabel", "待开会议")))
            .path("data");
    JsonNode secondMeeting =
        postJson(
                "/api/pingan/three-checks/pre-shift-meeting/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-08-01",
                    "rootDispatchRecordId", Long.parseLong(secondRootId),
                    "payload",
                        Map.of(
                            "meetingContent", "下午派班班前会",
                            "attendeesText", "湖贝班长",
                            "statusLabel", "待开会议")))
            .path("data");
    assertThat(firstMeeting.path("rootDispatchRecordId").asText()).isEqualTo(firstRootId);
    assertThat(secondMeeting.path("rootDispatchRecordId").asText()).isEqualTo(secondRootId);

    JsonNode flow =
        getJson("/api/pingan/three-checks/flows/" + firstRootId, token).path("data");

    assertThat(flow.path("rootDispatchRecordId").asText()).isEqualTo(firstRootId);
    assertThat(flow.path("stages")).hasSize(5);
    assertThat(flow.path("stages").get(0).path("stageKey").asText()).isEqualTo("teamDispatch");
    assertThat(flow.path("stages").get(0).path("record").path("id").asText()).isEqualTo(firstRootId);
    assertThat(flow.path("stages").get(1).path("stageKey").asText()).isEqualTo("preShiftMeeting");
    assertThat(flow.path("stages").get(1).path("record").path("id").asText())
        .isEqualTo(firstMeeting.path("id").asText());
    assertThat(flow.path("stages").get(1).path("record").path("payload").path("meetingContent").asText())
        .isEqualTo("上午派班班前会");
    List<String> stageRecordIds = new ArrayList<>();
    for (JsonNode stage : flow.path("stages")) {
      JsonNode record = stage.path("record");
      if (!record.isMissingNode() && !record.isNull()) {
        stageRecordIds.add(record.path("id").asText());
      }
    }
    assertThat(stageRecordIds).doesNotContain(secondMeeting.path("id").asText());
  }

  @Test
  void autoCreatesFullChainChildrenOnlyWhenDispatchIsSubmittedAndKeepsThemIdempotent()
      throws Exception {
    String token = login("admin", "123456");
    List<String> childModules =
        List.of(
            "pre-shift-meeting",
            "pre-shift-inspection",
            "mid-shift-inspection",
            "post-shift-inspection");

    JsonNode draftDispatch =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-09-01",
                    "payload",
                        Map.of(
                            "teamTask", "自动链路派班",
                            "dispatchTime", "2026-09-01 08:00",
                            "statusLabel", "未生效")))
            .path("data");
    long rootId = draftDispatch.path("id").asLong();
    long taskId = draftDispatch.path("taskId").asLong();

    assertThat(childCount(rootId)).isZero();

    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records/" + rootId + "/submit",
                token,
                Map.of())
            .path("data");
    assertThat(submitted.path("status").asText()).isEqualTo("OPENED");
    assertThat(childCount(rootId)).isEqualTo(4);

    for (String moduleKey : childModules) {
      JsonNode child = childByModule(rootId, moduleKey);
      assertThat(child.path("status").asText()).isEqualTo("DRAFT");
      assertThat(child.path("task_id").asLong()).isEqualTo(taskId);
      assertThat(child.path("company_id").asLong()).isEqualTo(SOURCE_COMPANY_ID);
      assertThat(child.path("department_id").asLong()).isEqualTo(SOURCE_DEPARTMENT_ID);
      assertThat(child.path("team_id").asLong()).isEqualTo(SOURCE_TEAM_ID);
      assertThat(child.path("owner_user_id").asLong()).isEqualTo(2L);
      assertThat(child.path("business_date").asText()).isEqualTo("2026-09-01");
      assertThat(child.path("source_record_id").asText())
          .isEqualTo("auto-dispatch-" + rootId + "-" + moduleKey);
      assertThat(child.path("client_request_id").asText())
          .isEqualTo("auto-dispatch-" + rootId + "-" + moduleKey);
      assertThat(child.path("payload_json").asText()).doesNotContain("rootDispatchPaused");
    }

    postJson(
        "/api/pingan/three-checks/team-dispatch/" + "records/" + rootId + "/withdraw",
        token,
        Map.of("reason", "派班计划调整"));
    assertThat(childCount(rootId)).isEqualTo(4);
    for (String moduleKey : childModules) {
      JsonNode child = childByModule(rootId, moduleKey);
      assertThat(child.path("status").asText()).isEqualTo("DRAFT");
      assertThat(child.path("payload_json").asText()).contains("\"rootDispatchPaused\":true");
      assertThat(child.path("payload_json").asText()).contains("派班计划调整");
      JsonNode childDetail =
          getJson(
                  "/api/pingan/three-checks/" + moduleKey + "/records/" + child.path("id").asText(),
                  token)
              .path("data");
      assertThat(childDetail.path("status").asText()).isEqualTo("DRAFT");
      assertThat(childDetail.path("statusLabel").asText()).isEqualTo("派班已撤回/暂停");
      assertThat(
              jdbcTemplate.queryForObject(
                  """
                  select count(*)
                  from biz_status_log
                  where biz_id = ?
                    and action = 'ROOT_DISPATCH_WITHDRAWN'
                  """,
                  Integer.class,
                  child.path("id").asLong()))
          .isEqualTo(1);
    }

    postJson(
        "/api/pingan/three-checks/team-dispatch/records/" + rootId + "/submit",
        token,
        Map.of());
    assertThat(childCount(rootId)).isEqualTo(4);
    for (String moduleKey : childModules) {
      JsonNode child = childByModule(rootId, moduleKey);
      assertThat(child.path("payload_json").asText()).doesNotContain("rootDispatchPaused");
      JsonNode childDetail =
          getJson(
                  "/api/pingan/three-checks/" + moduleKey + "/records/" + child.path("id").asText(),
                  token)
              .path("data");
      assertThat(childDetail.path("statusLabel").asText())
          .isEqualTo("pre-shift-meeting".equals(moduleKey) ? "待开会议" : "待检查");
    }
  }

  @Test
  void autoCreatesInspectionChildrenWithTeamCheckItemSnapshots() throws Exception {
    String token = login("admin", "123456");
    deleteExistingTeamCheckTemplate("PRE_SHIFT_INSPECTION");

    JsonNode libraryItem =
        postJson(
                "/api/system/team-check-item-templates/library",
                token,
                Map.of(
                    "riskType", "机械伤害",
                    "checkItem", "检查机械设备是否处于良好状态",
                    "applicableStage", "PRE_SHIFT_INSPECTION",
                    "defaultCheckResult", "无隐患",
                    "requireImage", true,
                    "requireVideo", false,
                    "sortOrder", 1,
                    "status", "ACTIVE"))
            .path("data");
    postJson(
        "/api/system/team-check-item-templates/templates",
        token,
        Map.of(
            "name", "幕墙组装1班班前检查模板",
            "companyOrgId", SOURCE_COMPANY_ID,
            "departmentOrgId", SOURCE_DEPARTMENT_ID,
            "teamOrgId", SOURCE_TEAM_ID,
            "inspectionStage", "PRE_SHIFT_INSPECTION",
            "status", "ACTIVE",
            "items",
                List.of(
                    Map.of(
                        "libraryItemId", libraryItem.path("id").asLong(),
                        "riskType", "机械伤害",
                        "checkItem", "检查机械设备是否处于良好状态",
                        "defaultCheckResult", "无隐患",
                        "requireImage", true,
                        "requireVideo", false,
                        "sortOrder", 1))));

    JsonNode dispatch =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-09-06",
                    "status", "已生效",
                    "payload", Map.of("teamTask", "检查项快照派班", "statusLabel", "已生效")))
            .path("data");

    JsonNode child = childByModule(dispatch.path("id").asLong(), "pre-shift-inspection");
    JsonNode payload = objectMapper.readTree(child.path("payload_json").asText());
    assertThat(payload.path("checkItems")).hasSize(1);
    assertThat(payload.path("checkItems").get(0).path("riskType").asText()).isEqualTo("机械伤害");
    assertThat(payload.path("checkItems").get(0).path("checkItem").asText())
        .isEqualTo("检查机械设备是否处于良好状态");
    assertThat(payload.path("checkItems").get(0).path("checkResult").asText()).isEmpty();
    assertThat(payload.path("checkItems").get(0).path("defaultCheckResult").asText())
        .isEqualTo("无隐患");
    assertThat(payload.path("checkItemTemplateSource").path("scope").asText()).isEqualTo("TEAM");
    assertThat(payload.path("checkItemTemplateSnapshotAt").asText()).isNotBlank();
  }

  @Test
  void autoCreatesPreShiftMeetingSafetyConfirmItemsAndRequiresConfirmation()
      throws Exception {
    String token = login("admin", "123456");
    deleteExistingTeamCheckTemplate("PRE_SHIFT_MEETING_CONFIRMATION");

    JsonNode libraryItem =
        postJson(
                "/api/system/team-check-item-templates/library",
                token,
                Map.of(
                    "riskType", "机械伤害",
                    "checkItem", "确认排水构筑物设计与防护能力",
                    "applicableStage", "PRE_SHIFT_MEETING_CONFIRMATION",
                    "defaultCheckResult", "无隐患",
                    "sortOrder", 1,
                    "status", "ACTIVE"))
            .path("data");
    postJson(
        "/api/system/team-check-item-templates/templates",
        token,
        Map.of(
            "name", "幕墙组装1班班前会安全确认模板",
            "companyOrgId", SOURCE_COMPANY_ID,
            "departmentOrgId", SOURCE_DEPARTMENT_ID,
            "teamOrgId", SOURCE_TEAM_ID,
            "inspectionStage", "PRE_SHIFT_MEETING_CONFIRMATION",
            "status", "ACTIVE",
            "items",
                List.of(
                    Map.of(
                        "libraryItemId", libraryItem.path("id").asLong(),
                        "riskType", "机械伤害",
                        "checkItem", "确认排水构筑物设计与防护能力",
                        "defaultCheckResult", "无隐患",
                        "sortOrder", 1))));

    JsonNode dispatch =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-09-12",
                    "status", "已生效",
                    "payload", Map.of("teamTask", "安全确认派班", "statusLabel", "已生效")))
            .path("data");

    JsonNode meeting = childByModule(dispatch.path("id").asLong(), "pre-shift-meeting");
    JsonNode payload = objectMapper.readTree(meeting.path("payload_json").asText());
    assertThat(payload.path("safetyConfirmItems")).hasSize(1);
    assertThat(payload.path("safetyConfirmItems").get(0).path("riskType").asText()).isEqualTo("机械伤害");
    assertThat(payload.path("safetyConfirmItems").get(0).path("safetyItem").asText())
        .isEqualTo("确认排水构筑物设计与防护能力");
    assertThat(payload.path("safetyConfirmItems").get(0).path("confirmStatus").asText()).isEmpty();
    assertThat(payload.path("safetyConfirmTemplateSource").path("scope").asText()).isEqualTo("TEAM");
    assertThat(payload.path("safetyConfirmTemplateSnapshotAt").asText()).isNotBlank();

    mockMvc
        .perform(
            post(
                    "/api/pingan/three-checks/pre-shift-meeting/records/"
                        + meeting.path("id").asText()
                        + "/submit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());

    payload.path("safetyConfirmItems").get(0);
    JsonNode confirmed =
        putJson(
                "/api/pingan/three-checks/pre-shift-meeting/records/"
                    + meeting.path("id").asText(),
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-09-12",
                    "payload",
                        Map.of(
                            "statusLabel",
                            "待开会议",
                            "meetingContent",
                            "",
                            "attendeesText",
                            "",
                            "safetyConfirmItems",
                            List.of(
                                Map.of(
                                    "riskType",
                                    "机械伤害",
                                    "safetyItem",
                                    "确认排水构筑物设计与防护能力",
                                    "confirmStatus",
                                    "已确认")),
                            "safetyConfirmTemplateSource",
                            payload.path("safetyConfirmTemplateSource"),
                            "safetyConfirmTemplateSnapshotAt",
                            payload.path("safetyConfirmTemplateSnapshotAt").asText()),
                    "version", 0))
            .path("data");
    assertThat(confirmed.path("payload").path("safetyConfirmItems").get(0).path("confirmStatus").asText())
        .isEqualTo("已确认");

    uploadThreeCheckRecordAttachment("pre-shift-meeting", meeting.path("id").asText(), token, "IMAGE", "meeting.jpg", "image/jpeg");
    uploadThreeCheckRecordAttachment("pre-shift-meeting", meeting.path("id").asText(), token, "VIDEO", "meeting.mp4", "video/mp4");
    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/pre-shift-meeting/records/"
                    + meeting.path("id").asText()
                    + "/submit",
                token,
                Map.of())
            .path("data");
    assertThat(submitted.path("status").asText()).isEqualTo("OPENED");
  }

  @Test
  void preShiftMeetingRequiresAtLeastOneMediaCheckBeforeSubmit() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/pre-shift-meeting/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-09-18",
                    "payload",
                        Map.of(
                            "meetingContent",
                            "班前会打卡校验",
                            "attendees",
                            List.of("湖贝班长"),
                            "attendeesText",
                            "湖贝班长",
                            "statusLabel",
                            "待开会议")))
            .path("data");
    String id = created.path("id").asText();

    mockMvc
        .perform(
            post("/api/pingan/three-checks/pre-shift-meeting/records/" + id + "/submit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            multipart("/api/pingan/three-checks/pre-shift-meeting/records/" + id + "/attachments")
                .file(
                    new MockMultipartFile(
                        "file",
                        "meeting.jpg",
                        "image/jpeg",
                        tinyJpeg()))
                .param("fileKind", "IMAGE")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/pre-shift-meeting/records/" + id + "/submit",
                token,
                Map.of())
            .path("data");
    assertThat(submitted.path("status").asText()).isEqualTo("OPENED");
  }

  @Test
  void submitInspectionRecordBackfillsTeamCheckItemsButRequiresManualResults() throws Exception {
    String token = login("admin", "123456");
    deleteExistingTeamCheckTemplate("POST_SHIFT_INSPECTION");

    JsonNode libraryItem =
        postJson(
                "/api/system/team-check-item-templates/library",
                token,
                Map.of(
                    "riskType", "",
                    "checkItem", "清点工具材料并确认交班状态",
                    "applicableStage", "POST_SHIFT_INSPECTION",
                    "defaultCheckResult", "无隐患",
                    "sortOrder", 1,
                    "status", "ACTIVE"))
            .path("data");
    postJson(
        "/api/system/team-check-item-templates/templates",
        token,
        Map.of(
            "name", "幕墙组装1班班后检查模板",
            "companyOrgId", SOURCE_COMPANY_ID,
            "departmentOrgId", SOURCE_DEPARTMENT_ID,
            "teamOrgId", SOURCE_TEAM_ID,
            "inspectionStage", "POST_SHIFT_INSPECTION",
            "status", "ACTIVE",
            "items",
                List.of(
                    Map.of(
                        "libraryItemId", libraryItem.path("id").asLong(),
                        "riskType", "",
                        "checkItem", "清点工具材料并确认交班状态",
                        "defaultCheckResult", "无隐患",
                        "sortOrder", 1))));

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/post-shift-inspection/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-09-07",
                    "payload", Map.of("statusLabel", "待检查")))
            .path("data");

    mockMvc
        .perform(
            post(
                    "/api/pingan/three-checks/post-shift-inspection/records/"
                        + created.path("id").asText()
                        + "/submit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void submitInspectionRecordRejectsRequiredCheckItemFieldViolations() throws Exception {
    String token = login("admin", "123456");

    assertInspectionSubmitRejected(
        token,
        "pre-shift-inspection",
        "2026-09-08",
        Map.of(
            "riskType",
            "机械伤害",
            "checkItem",
            "检查机械设备是否处于良好状态",
            "checkResult",
            "",
            "defaultCheckResult",
            "无隐患"));
    assertInspectionSubmitRejected(
        token,
        "pre-shift-inspection",
        "2026-09-09",
        Map.of("riskType", "", "checkItem", "检查机械设备是否处于良好状态", "checkResult", "无隐患"));
    assertInspectionSubmitRejected(
        token,
        "pre-shift-inspection",
        "2026-09-10",
        Map.of("riskType", "机械伤害", "checkItem", "", "checkResult", "无隐患"));
    assertInspectionSubmitRejected(
        token,
        "pre-shift-inspection",
        "2026-09-11",
        Map.of("riskType", "机械伤害", "checkItem", "检查机械设备是否处于良好状态", "checkResult", "不适用"));
  }

  @Test
  void submitInspectionRecordCreatesOneRectificationOrderForMultipleHazardItems()
      throws Exception {
    String token = login("admin", "123456");
    List<Map<String, Object>> checkItems =
        List.of(
            Map.of(
                "lineId",
                "line-safe",
                "riskType",
                "机械伤害",
                "checkItem",
                "检查机械设备是否处于良好状态",
                "checkResult",
                "无隐患"),
            Map.of(
                "lineId",
                "line-hazard-1",
                "libraryItemId",
                "1001",
                "riskType",
                "高处坠落",
                "checkItem",
                "检查临边防护是否牢固",
                "checkResult",
                "有隐患",
                "hazardDescription",
                "临边防护缺少挡脚板",
                "beforePhoto",
                "/uploads/hazard-before-1.jpg",
                "defaultFollowUpPlan",
                "补齐挡脚板"),
            Map.of(
                "lineId",
                "line-hazard-2",
                "libraryItemId",
                "1002",
                "riskType",
                "物体打击",
                "checkItem",
                "检查材料堆放是否稳固",
                "checkResult",
                "有隐患",
                "hazardDescription",
                "材料堆放超高且未固定",
                "beforePhoto",
                "/uploads/hazard-before-2.jpg"));

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/mid-shift-inspection/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-09-12",
                    "payload",
                    Map.of("statusLabel", "待检查", "checkItems", checkItems)))
            .path("data");

    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/mid-shift-inspection/records/"
                    + created.path("id").asText()
                    + "/submit",
                token,
                Map.of())
            .path("data");

    JsonNode orders =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceType=THREE_CHECK&sourceRecordId="
                    + submitted.path("id").asText(),
                token)
            .path("data");
    assertThat(orders.path("total").asInt()).isEqualTo(1);
    JsonNode order = orders.path("items").get(0);
    assertThat(order.path("sourceModuleKey").asText()).isEqualTo("mid-shift-inspection");
    assertThat(order.path("sourceRecordId").asText()).isEqualTo(submitted.path("id").asText());
    assertThat(order.path("status").asText()).isEqualTo("PENDING_ASSIGN");
    assertThat(order.path("statusLabel").asText()).isEqualTo("待派发");
    assertThat(order.path("hazardCount").asInt()).isEqualTo(2);

    JsonNode orderDetail =
        getJson(
                "/api/pingan/hazard-rectification/orders/" + order.path("id").asText(),
                token)
            .path("data");
    assertThat(orderDetail.path("items")).hasSize(2);
    assertThat(orderDetail.path("items").get(0).path("sourceLineId").asText()).isEqualTo("line-hazard-1");
    assertThat(orderDetail.path("items").get(1).path("sourceLineId").asText()).isEqualTo("line-hazard-2");

    JsonNode reloaded =
        getJson(
                "/api/pingan/three-checks/mid-shift-inspection/records/"
                    + submitted.path("id").asText(),
                token)
            .path("data");
    JsonNode payloadItems = reloaded.path("payload").path("checkItems");
    assertThat(payloadItems.get(0).hasNonNull("rectificationOrderId")).isFalse();
    assertThat(payloadItems.get(1).path("rectificationOrderNo").asText())
        .isEqualTo(order.path("orderNo").asText());
    assertThat(payloadItems.get(2).path("rectificationOrderNo").asText())
        .isEqualTo(order.path("orderNo").asText());
    assertThat(payloadItems.get(1).path("rectificationStatus").asText()).isEqualTo("PENDING_ASSIGN");
    assertThat(payloadItems.get(2).path("rectificationStatus").asText()).isEqualTo("PENDING_ASSIGN");

    mockMvc
        .perform(
            post(
                    "/api/pingan/three-checks/mid-shift-inspection/records/"
                        + submitted.path("id").asText()
                        + "/submit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
    JsonNode afterRepeatSubmit =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceType=THREE_CHECK&sourceRecordId="
                    + submitted.path("id").asText(),
                token)
            .path("data");
    assertThat(afterRepeatSubmit.path("total").asInt()).isEqualTo(1);
  }

  @Test
  void submitInspectionRecordCopiesRectificationDescriptionToOrderItemHazardDescription()
      throws Exception {
    String token = login("admin", "123456");
    JsonNode created =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-10-11",
                    "payload",
                    Map.of(
                        "statusLabel",
                        "待检查",
                        "checkItems",
                        List.of(
                            Map.of(
                                "lineId",
                                "line-rectification-description",
                                "riskType",
                                "高处坠落",
                                "checkItem",
                                "检查临边防护是否牢固",
                                "checkResult",
                                "有隐患",
                                "rectificationDescription",
                                "临边防护栏杆松动")))))
            .path("data");

    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/"
                    + created.path("id").asText()
                    + "/submit",
                token,
                Map.of())
            .path("data");

    JsonNode orders =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceType=THREE_CHECK&sourceRecordId="
                    + submitted.path("id").asText(),
                token)
            .path("data");
    JsonNode order = orders.path("items").get(0);
    JsonNode orderDetail =
        getJson(
                "/api/pingan/hazard-rectification/orders/" + order.path("id").asText(),
                token)
            .path("data");

    assertThat(orderDetail.path("items").get(0).path("hazardDescription").asText())
        .isEqualTo("临边防护栏杆松动");
  }

  @Test
  void miniCanOpenRectificationOrderForSubmittedThreeCheckInspection() throws Exception {
    String token = login("admin", "123456");
    JsonNode created =
        createGenericRecord(
            token,
            "pre-shift-inspection",
            "待检查",
            Map.of(
                "statusLabel",
                "待检查",
                "checkItems",
                List.of(
                    Map.of(
                        "lineId",
                        "mini-open-order-line-1",
                        "riskType",
                        "机械伤害",
                        "checkItem",
                        "设备防护罩检查",
                        "checkResult",
                        "有隐患",
                        "hazardDescription",
                        "防护罩缺失"))),
            futureBusinessDate());
    String id = created.path("id").asText();
    postJson("/api/mini/pingan/three-checks/pre-shift-inspection/records/" + id + "/submit", token, Map.of());

    JsonNode opened =
        postJson(
                "/api/mini/pingan/three-checks/pre-shift-inspection/records/"
                    + id
                    + "/rectification-order",
                token,
                Map.of())
            .path("data");

    assertThat(opened.path("sourceType").asText()).isEqualTo("THREE_CHECK");
    assertThat(opened.path("sourceModuleKey").asText()).isEqualTo("pre-shift-inspection");
    assertThat(opened.path("sourceRecordId").asText()).isEqualTo(id);
    assertThat(opened.path("statusLabel").asText()).isEqualTo("待派发");
    assertThat(opened.path("items")).hasSize(1);
  }

  @Test
  void threeCheckRectificationOrderPermissionIsSharedByPcAndMiniAndRequiresHazardItem()
      throws Exception {
    long viewOnlyUserId = 990301L;
    long enabledUserId = 990302L;
    createPermissionFixtureUser(
        viewOnlyUserId,
        990301L,
        "three_check_rectification_view_only",
        "三查开单只读用户",
        "THREE_CHECK_RECTIFICATION_VIEW_ONLY",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW");
    createPermissionFixtureUser(
        enabledUserId,
        990302L,
        "three_check_rectification_enabled",
        "三查开单授权用户",
        "THREE_CHECK_RECTIFICATION_ENABLED",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_RECTIFICATION_ORDER_CREATE");

    String adminToken = login("admin", "123456");
    JsonNode viewOnlyRecord =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records",
                adminToken,
                inspectionThreeCheckPayloadWithResult(
                    viewOnlyUserId, "2036-10-02", "只读用户有隐患记录", "有隐患"))
            .path("data");
    String viewOnlyId = viewOnlyRecord.path("id").asText();
    String viewOnlyToken = login("three_check_rectification_view_only", "123456");
    assertThat(
            getJson(
                    "/api/pingan/three-checks/pre-shift-inspection/records/" + viewOnlyId,
                    viewOnlyToken)
                .path("data")
                .path("canCreateRectificationOrder")
                .asBoolean())
        .isFalse();
    mockMvc
        .perform(
            post(
                    "/api/pingan/three-checks/pre-shift-inspection/records/"
                        + viewOnlyId
                        + "/rectification-order")
                .header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post(
                    "/api/mini/pingan/three-checks/pre-shift-inspection/records/"
                        + viewOnlyId
                        + "/rectification-order")
                .header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isForbidden());

    JsonNode enabledRecord =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records",
                adminToken,
                inspectionThreeCheckPayloadWithResult(
                    enabledUserId, "2036-10-03", "授权用户有隐患记录", "有隐患"))
            .path("data");
    String enabledId = enabledRecord.path("id").asText();
    String enabledToken = login("three_check_rectification_enabled", "123456");
    assertThat(
            getJson(
                    "/api/pingan/three-checks/pre-shift-inspection/records/" + enabledId,
                    enabledToken)
                .path("data")
                .path("canCreateRectificationOrder")
                .asBoolean())
        .isTrue();

    JsonNode pcOpened =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/"
                    + enabledId
                    + "/rectification-order",
                enabledToken,
                Map.of())
            .path("data");
    JsonNode miniOpened =
        postJson(
                "/api/mini/pingan/three-checks/pre-shift-inspection/records/"
                    + enabledId
                    + "/rectification-order",
                enabledToken,
                Map.of())
            .path("data");
    assertThat(miniOpened.path("id").asText()).isEqualTo(pcOpened.path("id").asText());
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from hazard_rectification_order where source_type = 'THREE_CHECK' and source_module_key = 'pre-shift-inspection' and source_record_id = ? and deleted = 0",
                Integer.class,
                Long.parseLong(enabledId)))
        .isEqualTo(1);

    JsonNode noHazardRecord =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records",
                adminToken,
                inspectionThreeCheckPayloadWithResult(
                    enabledUserId, "2036-10-04", "授权用户无隐患记录", "无隐患"))
            .path("data");
    String noHazardId = noHazardRecord.path("id").asText();
    assertThat(
            getJson(
                    "/api/pingan/three-checks/pre-shift-inspection/records/" + noHazardId,
                    enabledToken)
                .path("data")
                .path("canCreateRectificationOrder")
                .asBoolean())
        .isFalse();
    mockMvc
        .perform(
            post(
                    "/api/pingan/three-checks/pre-shift-inspection/records/"
                        + noHazardId
                        + "/rectification-order")
                .header("Authorization", "Bearer " + enabledToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rectificationOrderActionsRejectAcceptanceAndCloseBackfillThreeCheckItems()
      throws Exception {
    String adminToken = login("admin", "123456");
    String safetyToken = login("MQ_SAFE", "123456");
    String teamLeaderToken = login("HB_MONITOR", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records",
                adminToken,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-09-13",
                    "payload",
                    Map.of(
                        "statusLabel",
                        "待检查",
                        "checkItems",
                        List.of(
                            Map.of(
                                "lineId",
                                "line-action-hazard",
                                "riskType",
                                "机械伤害",
                                "checkItem",
                                "检查设备防护罩是否齐全",
                                "checkResult",
                                "有隐患",
                                "hazardDescription",
                                "设备防护罩缺失")))))
            .path("data");
    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/"
                    + created.path("id").asText()
                    + "/submit",
                adminToken,
                Map.of())
            .path("data");
    JsonNode order =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceRecordId="
                    + submitted.path("id").asText(),
                adminToken)
            .path("data")
            .path("items")
            .get(0);
    String orderId = order.path("id").asText();

    mockMvc
        .perform(
            post("/api/pingan/hazard-rectification/orders/" + orderId + "/actions")
                .header("Authorization", "Bearer " + safetyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "action",
                            "ISSUE_RECTIFICATION",
                            "version",
                            order.path("version").asInt(),
                            "payload",
                            Map.of(
                                "rectificationResponsibleUserId",
                                2,
                                "acceptanceUserId",
                                2,
                                "rectificationDeadline",
                                "2026-09-20 18:00:00",
                                "rectificationRequirement",
                                "立即整改")))))
        .andExpect(status().isBadRequest());

    JsonNode pendingRectify =
        hazardOrderAction(
            safetyToken,
            orderId,
            "ISSUE_RECTIFICATION",
            Map.of(
                "rectificationResponsibleUserId",
                2,
                "rectificationDepartmentId",
                SOURCE_DEPARTMENT_ID,
                "rectificationDeadline",
                "2026-09-20 18:00:00",
                "rectificationRequirement",
                "补齐防护罩并拍照留存"));
    assertThat(pendingRectify.path("status").asText()).isEqualTo("PENDING_RECTIFY");
    assertThat(pendingRectify.path("statusLabel").asText()).isEqualTo("待整改");
    assertThat(pendingRectify.path("rectificationDepartmentId").asLong()).isEqualTo(SOURCE_DEPARTMENT_ID);
    assertThat(
            firstLogByAction(pendingRectify.path("flowLogs"), "ISSUE_RECTIFICATION")
                .path("payload")
                .path("rectificationRequirement")
                .asText())
        .isEqualTo("补齐防护罩并拍照留存");

    JsonNode rectified =
        hazardOrderAction(
            teamLeaderToken,
            orderId,
            "MARK_RECTIFIED",
            Map.of(
                "rectificationDescription",
                "已补齐防护罩",
                "afterPhoto",
                "/uploads/rectified-after.jpg"));
    assertThat(rectified.path("status").asText()).isEqualTo("RECTIFIED");
    assertThat(rectified.path("rectificationAfterPhoto").asText())
        .isEqualTo("/uploads/rectified-after.jpg");
    JsonNode rectifiedLog = firstLogByAction(rectified.path("flowLogs"), "MARK_RECTIFIED");
    assertThat(rectifiedLog.path("payload").path("rectificationDescription").asText())
        .isEqualTo("已补齐防护罩");
    assertThat(rectifiedLog.path("payload").path("afterPhoto").asText())
        .isEqualTo("/uploads/rectified-after.jpg");

    JsonNode pendingAcceptance =
        hazardOrderAction(
            teamLeaderToken,
            orderId,
            "REQUEST_ACCEPTANCE",
            Map.of("acceptanceDepartmentId", SOURCE_DEPARTMENT_ID, "acceptanceRemark", "申请验收"));
    assertThat(pendingAcceptance.path("status").asText()).isEqualTo("PENDING_ACCEPTANCE");
    assertThat(pendingAcceptance.path("acceptanceDepartmentId").asLong()).isEqualTo(SOURCE_DEPARTMENT_ID);

    mockMvc
        .perform(
            post("/api/pingan/hazard-rectification/orders/" + orderId + "/actions")
                .header("Authorization", "Bearer " + teamLeaderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "action",
                            "ACCEPT",
                            "version",
                            pendingAcceptance.path("version").asInt(),
                            "payload",
                            Map.of("acceptanceRemark", "责任人不能自验")))))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/pingan/hazard-rectification/orders/" + orderId + "/actions")
                .header("Authorization", "Bearer " + teamLeaderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "action",
                            "REJECT_ACCEPTANCE",
                            "version",
                            pendingAcceptance.path("version").asInt(),
                            "payload",
                            Map.of("acceptanceUserId", 1, "rejectReason", "整改权限不能验收驳回")))))
        .andExpect(status().isForbidden());

    long acceptRoleId = 9902L;
    jdbcTemplate.update("delete from sys_user_role where role_id = ?", acceptRoleId);
    jdbcTemplate.update("delete from sys_role_menu where role_id = ?", acceptRoleId);
    jdbcTemplate.update("delete from sys_role where id = ?", acceptRoleId);
    jdbcTemplate.update(
        "insert into sys_role (id, role_code, role_name, data_scope) values (?, 'HAZARD_ACCEPT_FIXTURE', '隐患验收测试角色', 'ORG_AND_CHILDREN')",
        acceptRoleId);
    jdbcTemplate.update(
        "insert into sys_user_role (user_id, role_id) values (2, ?)",
        acceptRoleId);
    jdbcTemplate.update(
        """
        insert into sys_role_menu (role_id, menu_id)
        select ?, id
        from sys_menu
        where permission_code = 'PINGAN_HAZARD_RECTIFICATION_ACCEPT'
        """,
        acceptRoleId);
    String acceptanceToken = login("HB_MONITOR", "123456");
    mockMvc
        .perform(
            post("/api/pingan/hazard-rectification/orders/" + orderId + "/actions")
                .header("Authorization", "Bearer " + acceptanceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "action",
                            "ACCEPT",
                            "version",
                            pendingAcceptance.path("version").asInt(),
                            "payload",
                            Map.of("acceptanceRemark", "责任人不能自验")))))
        .andExpect(status().isBadRequest());
    jdbcTemplate.update("delete from sys_user_role where role_id = ?", acceptRoleId);
    jdbcTemplate.update("delete from sys_role_menu where role_id = ?", acceptRoleId);
    jdbcTemplate.update("delete from sys_role where id = ?", acceptRoleId);

    JsonNode rejected =
        hazardOrderAction(
            adminToken,
            orderId,
            "REJECT_ACCEPTANCE",
            Map.of("acceptanceUserId", 1, "rejectReason", "整改照片不清晰"));
    assertThat(rejected.path("status").asText()).isEqualTo("PENDING_RECTIFY");
    assertThat(rejected.path("acceptanceUserId").asLong()).isEqualTo(1);
    assertThat(rejected.path("flowLogs").toString()).contains("验收驳回", "整改照片不清晰");

    hazardOrderAction(
        teamLeaderToken,
        orderId,
        "MARK_RECTIFIED",
        Map.of("rectificationDescription", "已重新拍照上传", "afterPhoto", "/uploads/after-clear.jpg"));
    hazardOrderAction(
        teamLeaderToken,
        orderId,
        "REQUEST_ACCEPTANCE",
        Map.of("acceptanceUserId", 1, "acceptanceRemark", "重新申请验收"));
    JsonNode closed =
        hazardOrderAction(
            adminToken,
            orderId,
            "ACCEPT",
            Map.of("acceptanceUserId", 1, "acceptanceRemark", "验收通过"));
    assertThat(closed.path("status").asText()).isEqualTo("CLOSED");
    assertThat(closed.path("acceptanceUserId").asLong()).isEqualTo(1);
    assertThat(closed.path("closedAt").asText()).isNotBlank();
    assertThat(closed.path("items").get(0).path("rectificationStatus").asText()).isEqualTo("CLOSED");

    JsonNode reloaded =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/"
                    + submitted.path("id").asText(),
                adminToken)
            .path("data");
    JsonNode item = reloaded.path("payload").path("checkItems").get(0);
    assertThat(item.path("rectificationStatus").asText()).isEqualTo("CLOSED");
    assertThat(item.path("rectificationClosedAt").asText()).isNotBlank();
    assertThat(item.path("lastRectificationAction").asText()).isEqualTo("验收通过");
  }

  @Test
  void cancelsPendingHazardRectificationOrderAndBackfillsSourceItems() throws Exception {
    String token = login("admin", "123456");
    JsonNode created =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-09-14",
                    "payload",
                    Map.of(
                        "statusLabel",
                        "待检查",
                        "checkItems",
                        List.of(
                            Map.of(
                                "lineId",
                                "line-cancel-hazard",
                                "riskType",
                                "现场环境",
                                "checkItem",
                                "检查安全出口是否畅通",
                                "checkResult",
                                "有隐患",
                                "hazardDescription",
                                "安全出口临时堆放材料")))))
            .path("data");
    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/"
                    + created.path("id").asText()
                    + "/submit",
                token,
                Map.of())
            .path("data");
    JsonNode order =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceRecordId="
                    + submitted.path("id").asText(),
                token)
            .path("data")
            .path("items")
            .get(0);
    String orderId = order.path("id").asText();

    JsonNode cancelled =
        hazardOrderAction(token, orderId, "CANCEL", Map.of("cancelReason", "现场复核无隐患，作废处理"));

    assertThat(cancelled.path("status").asText()).isEqualTo("CANCELLED");
    assertThat(cancelled.path("statusLabel").asText()).isEqualTo("已作废");
    assertThat(cancelled.path("closedAt").isNull()).isTrue();
    assertThat(cancelled.path("items").get(0).path("rectificationStatus").asText()).isEqualTo("CANCELLED");
    assertThat(cancelled.path("items").get(0).path("closedAt").isNull()).isTrue();
    assertThat(cancelled.path("flowLogs").toString()).contains("作废", "现场复核无隐患，作废处理");

    JsonNode reloaded =
        getJson(
                "/api/pingan/three-checks/pre-shift-inspection/records/"
                    + submitted.path("id").asText(),
                token)
            .path("data");
    JsonNode sourceItem = reloaded.path("payload").path("checkItems").get(0);
    assertThat(sourceItem.path("rectificationStatus").asText()).isEqualTo("CANCELLED");
    assertThat(sourceItem.path("rectificationStatusLabel").asText()).isEqualTo("已作废");
    assertThat(sourceItem.path("rectificationClosedAt").asText()).isBlank();
    assertThat(sourceItem.path("lastRectificationAction").asText()).isEqualTo("作废");
    assertThat(sourceItem.path("lastRectificationRemark").asText()).isEqualTo("现场复核无隐患，作废处理");

    mockMvc
        .perform(
            post("/api/pingan/hazard-rectification/orders/" + orderId + "/actions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "action",
                            "ACCEPT",
                            "version",
                            cancelled.path("version").asInt(),
                            "payload",
                            Map.of("acceptanceUserId", 1, "acceptanceRemark", "不能关闭作废工单")))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void closedHazardRectificationOrderCannotBeCancelled() throws Exception {
    String adminToken = login("admin", "123456");
    String safetyToken = login("MQ_SAFE", "123456");
    String teamLeaderToken = login("HB_MONITOR", "123456");
    JsonNode created =
        postJson(
                "/api/pingan/hazard-rectification/orders",
                adminToken,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "businessDate",
                    "2026-09-15",
                    "items",
                    List.of(
                        Map.of(
                            "riskType",
                            "设备设施",
                            "checkItem",
                            "消防栓检查",
                            "hazardDescription",
                            "消防栓箱门损坏"))))
            .path("data");
    String orderId = created.path("id").asText();
    hazardOrderAction(
        safetyToken,
        orderId,
        "ISSUE_RECTIFICATION",
        Map.of(
            "rectificationResponsibleUserId",
            2,
            "acceptanceUserId",
            1,
            "rectificationDeadline",
            "2026-09-18 18:00:00",
            "rectificationRequirement",
            "修复消防栓箱门"));
    hazardOrderAction(
        teamLeaderToken,
        orderId,
        "MARK_RECTIFIED",
        Map.of("rectificationDescription", "箱门已修复", "afterPhoto", "/uploads/fire-hydrant-after.jpg"));
    hazardOrderAction(
        teamLeaderToken,
        orderId,
        "REQUEST_ACCEPTANCE",
        Map.of("acceptanceUserId", 1, "acceptanceRemark", "申请验收"));
    JsonNode closed =
        hazardOrderAction(
            adminToken,
            orderId,
            "ACCEPT",
            Map.of("acceptanceUserId", 1, "acceptanceRemark", "验收通过"));
    assertThat(closed.path("status").asText()).isEqualTo("CLOSED");

    mockMvc
        .perform(
            post("/api/pingan/hazard-rectification/orders/" + orderId + "/actions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "action",
                            "CANCEL",
                            "version",
                            closed.path("version").asInt(),
                            "payload",
                            Map.of("cancelReason", "关闭后不能作废")))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void singleAccountCanOperateAcceptanceActionsWithSelectedAcceptanceUser()
      throws Exception {
    String adminToken = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/mid-shift-inspection/records",
                adminToken,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-09-14",
                    "payload",
                    Map.of(
                        "statusLabel",
                        "待检查",
                        "checkItems",
                        List.of(
                            Map.of(
                                "lineId",
                                "line-single-account",
                                "riskType",
                                "高处坠落",
                                "checkItem",
                                "检查安全绳是否固定",
                                "checkResult",
                                "有隐患",
                                "hazardDescription",
                                "安全绳固定点松动")))))
            .path("data");
    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/mid-shift-inspection/records/"
                    + created.path("id").asText()
                    + "/submit",
                adminToken,
                Map.of())
            .path("data");
    String orderId =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceRecordId="
                    + submitted.path("id").asText(),
                adminToken)
            .path("data")
            .path("items")
            .get(0)
            .path("id")
            .asText();

    hazardOrderAction(
        adminToken,
        orderId,
        "ISSUE_RECTIFICATION",
        Map.of(
            "rectificationResponsibleUserId",
            2,
            "acceptanceUserId",
            3,
            "rectificationDeadline",
            "2026-09-20 18:30",
            "rectificationRequirement",
            "加固安全绳固定点"));
    hazardOrderAction(
        adminToken,
        orderId,
        "MARK_RECTIFIED",
        Map.of(
            "rectificationDescription",
            "已完成加固",
            "afterPhoto",
            "data:image/png;base64,ZmFrZQ=="));
    hazardOrderAction(
        adminToken,
        orderId,
        "REQUEST_ACCEPTANCE",
        Map.of("acceptanceUserId", 3, "acceptanceRemark", "单账号提交验收"));

    JsonNode closed =
        hazardOrderAction(
            adminToken,
            orderId,
            "ACCEPT",
            Map.of("acceptanceUserId", 3, "acceptanceRemark", "单账号代验收通过"));

    assertThat(closed.path("status").asText()).isEqualTo("CLOSED");
    assertThat(closed.path("acceptanceUserId").asLong()).isEqualTo(3);
    assertThat(closed.path("rectificationDeadline").asText()).isEqualTo("2026-09-20 18:30:00");
    assertThat(closed.path("rectificationAfterPhoto").asText())
        .startsWith("data:image/png;base64,");
  }

  @Test
  void uploadsRectificationAfterPhotoThroughOrderAttachmentEndpoint()
      throws Exception {
    String adminToken = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/mid-shift-inspection/records",
                adminToken,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-09-15",
                    "payload",
                    Map.of(
                        "statusLabel",
                        "待检查",
                        "checkItems",
                        List.of(
                            Map.of(
                                "lineId",
                                "line-order-attachment",
                                "riskType",
                                "机械伤害",
                                "checkItem",
                                "检查防护罩是否牢固",
                                "checkResult",
                                "有隐患",
                                "hazardDescription",
                                "防护罩松动")))))
            .path("data");
    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/mid-shift-inspection/records/"
                    + created.path("id").asText()
                    + "/submit",
                adminToken,
                Map.of())
            .path("data");
    String orderId =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceRecordId="
                    + submitted.path("id").asText(),
                adminToken)
            .path("data")
            .path("items")
            .get(0)
            .path("id")
            .asText();
    hazardOrderAction(
        adminToken,
        orderId,
        "ISSUE_RECTIFICATION",
        Map.of(
            "rectificationResponsibleUserId",
            2,
            "acceptanceUserId",
            3,
            "rectificationDeadline",
            "2026-09-20 18:30",
            "rectificationRequirement",
            "重新固定防护罩"));

    MockMultipartFile image =
        new MockMultipartFile(
            "file",
            "after.jpg",
            "image/jpeg",
            tinyJpeg());
    JsonNode uploadResponse =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        multipart(
                                "/api/pingan/hazard-rectification/orders/"
                                    + orderId
                                    + "/attachments")
                            .file(image)
                            .param("fileKind", "RECTIFICATION_AFTER_PHOTO")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");

    assertThat(uploadResponse.path("fileKind").asText()).isEqualTo("RECTIFICATION_AFTER_PHOTO");
    assertThat(uploadResponse.path("url").asText()).startsWith("/api/attachments/");

    JsonNode rectified =
        hazardOrderAction(
            adminToken,
            orderId,
            "MARK_RECTIFIED",
            Map.of(
                "rectificationDescription",
                "已重新固定",
                "afterPhoto",
                uploadResponse.path("url").asText()));
    assertThat(rectified.path("rectificationAfterPhoto").asText())
        .isEqualTo(uploadResponse.path("url").asText());
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from biz_attachment where biz_type = 'HAZARD_RECTIFICATION_ORDER' and biz_id = ? and file_kind = 'RECTIFICATION_AFTER_PHOTO'",
                Integer.class,
                Long.parseLong(orderId)))
        .isEqualTo(1);
  }

  @Test
  void manuallyCreatesHazardRectificationOrderWithMultipleItems() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/hazard-rectification/orders",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "businessDate",
                    "2026-10-01",
                    "items",
                    List.of(
                        Map.of(
                            "riskType",
                            "高处坠落",
                            "checkItem",
                            "临边防护",
                            "hazardDescription",
                            "防护栏杆缺失"),
                        Map.of(
                            "riskType",
                            "机械伤害",
                            "checkItem",
                            "设备防护罩",
                            "hazardDescription",
                            "防护罩松动"))))
            .path("data");

    assertThat(created.path("sourceType").asText()).isEqualTo("MANUAL");
    assertThat(created.path("sourceModuleKey").asText()).isEqualTo("manual");
    assertThat(created.path("sourceRecordId").isMissingNode() || created.path("sourceRecordId").isNull()).isTrue();
    assertThat(created.path("status").asText()).isEqualTo("PENDING_ASSIGN");
    assertThat(created.path("hazardCount").asInt()).isEqualTo(2);
    assertThat(created.path("items")).hasSize(2);
    assertThat(created.path("flowLogs").toString()).contains("手工创建");
  }

  @Test
  void batchDeletesHazardRectificationOrders() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/hazard-rectification/orders",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "businessDate",
                    "2026-10-06",
                    "items",
                    List.of(
                        Map.of(
                            "riskType",
                            "高处坠落",
                            "checkItem",
                            "临边防护",
                            "hazardDescription",
                            "防护栏杆缺失"))))
            .path("data");
    long orderId = created.path("id").asLong();

    postJson(
        "/api/pingan/hazard-rectification/orders/batch-delete",
        token,
        Map.of("ids", List.of(orderId)));

    Integer deleted =
        jdbcTemplate.queryForObject(
            "select deleted from hazard_rectification_order where id = ?", Integer.class, orderId);
    assertThat(deleted).isEqualTo(1);
  }

  @Test
  void submitSafetyCheckCanCreateHazardRectificationOrder() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/safety-check/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-10-02",
                        "payload",
                        Map.of(
                            "statusLabel",
                            "待检查",
                            "checkItems",
                            List.of(
                            Map.of(
                                "lineId",
                                "safety-hazard-1",
                                "riskType",
                                "触电",
                                "checkItem",
                                "临时用电",
                                "checkResult",
                                "有隐患",
                                "hazardDescription",
                                "配电箱未上锁"),
                            Map.of(
                                "lineId",
                                "safety-hazard-2",
                                "riskType",
                                "物体打击",
                                "checkItem",
                                "材料堆放",
                                "checkResult",
                                "有隐患",
                                "hazardDescription",
                                "材料堆放超高")))))
            .path("data");

    postJson(
        "/api/pingan/three-checks/safety-check/records/"
            + created.path("id").asText()
            + "/submit",
        token,
        Map.of());

    JsonNode orders =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceType=SAFETY_INSPECTION&sourceRecordId="
                    + created.path("id").asText(),
                token)
            .path("data");
    assertThat(orders.path("total").asInt()).isEqualTo(1);
    JsonNode order = orders.path("items").get(0);
    assertThat(order.path("sourceType").asText()).isEqualTo("SAFETY_INSPECTION");
    assertThat(order.path("sourceModuleKey").asText()).isEqualTo("safety-check");
    assertThat(order.path("hazardCount").asInt()).isEqualTo(2);
    JsonNode detail =
        getJson(
                "/api/pingan/hazard-rectification/orders/"
                    + order.path("id").asText(),
                token)
            .path("data");
    assertThat(detail.path("items")).hasSize(2);
    assertThat(detail.path("flowLogs").toString()).contains("安全检查发现隐患自动生成工单");
  }

  @Test
  void submitSafetyCheckHazardLinesCanCreateHazardRectificationOrder() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/safety-check/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-10-10",
                    "payload",
                    Map.of(
                        "statusLabel",
                        "待检查",
                        "hazardLines",
                        List.of(
                            Map.of(
                                "sequence",
                                "1",
                                "checkResult",
                                "有隐患",
                                "aiEnabled",
                                "是",
                                "hazardLibrary",
                                "临时用电",
                                "checkItem",
                                "配电箱检查",
                                "hazardDescription",
                                "配电箱未上锁",
                                "rectificationMeasures",
                                "整改上锁",
                                "rectificationResponsiblePerson",
                                "张三",
                                "rectificationDeadline",
                                "2026-10-12",
                                "status",
                                "待整改")))))
            .path("data");
    String recordId = created.path("id").asText();

    postJson(
        "/api/pingan/three-checks/safety-check/records/" + recordId + "/submit",
        token,
        Map.of());

    JsonNode orders =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceType=SAFETY_INSPECTION&sourceRecordId="
                    + recordId,
                token)
            .path("data");
    assertThat(orders.path("total").asInt()).isEqualTo(1);
    JsonNode order = orders.path("items").get(0);
    assertThat(order.path("sourceType").asText()).isEqualTo("SAFETY_INSPECTION");
    assertThat(order.path("sourceModuleKey").asText()).isEqualTo("safety-check");
    assertThat(order.path("hazardCount").asInt()).isEqualTo(1);

    JsonNode detail =
        getJson("/api/pingan/hazard-rectification/orders/" + order.path("id").asText(), token)
            .path("data");
    assertThat(detail.path("items")).hasSize(1);
    JsonNode item = detail.path("items").get(0);
    assertThat(item.path("hazardDescription").asText()).isEqualTo("配电箱未上锁");
    assertThat(item.path("aiEnabled").asText()).isEqualTo("是");
    assertThat(item.path("sourceSnapshot").path("checkResult").asText()).isEqualTo("有隐患");

    JsonNode sourceRecord =
        getJson("/api/pingan/three-checks/safety-check/records/" + recordId, token).path("data");
    JsonNode hazardLine = sourceRecord.path("payload").path("hazardLines").get(0);
    assertThat(hazardLine.path("rectificationOrderId").asText())
        .isEqualTo(order.path("id").asText());
    assertThat(hazardLine.path("rectificationOrderNo").asText())
        .isEqualTo(order.path("orderNo").asText());
    assertThat(sourceRecord.path("payload").has("checkItems")).isFalse();

    hazardOrderAction(
        token,
        order.path("id").asText(),
        "ISSUE_RECTIFICATION",
        Map.of(
            "rectificationResponsibleUserId",
            2,
            "rectificationDepartmentId",
            SOURCE_DEPARTMENT_ID,
            "rectificationDeadline",
            "2026-10-12 18:00:00",
            "rectificationRequirement",
            "整改上锁"));
    JsonNode issuedRecord =
        getJson("/api/pingan/three-checks/safety-check/records/" + recordId, token).path("data");
    JsonNode issuedHazardLine = issuedRecord.path("payload").path("hazardLines").get(0);
    assertThat(issuedHazardLine.path("rectificationStatus").asText()).isEqualTo("PENDING_RECTIFY");
    assertThat(issuedHazardLine.path("rectificationStatusLabel").asText()).isEqualTo("待整改");
    assertThat(issuedHazardLine.path("lastRectificationAction").asText()).isEqualTo("下发整改");
  }

  @Test
  void submitSafetyCheckCopiesBeforeRectificationPhotoToOrderItem() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/safety-check/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-10-11",
                    "payload",
                    Map.of(
                        "statusLabel",
                        "待检查",
                        "hazardLines",
                        List.of(
                            Map.of(
                                "sequence",
                                "1",
                                "checkResult",
                                "有隐患",
                                "checkItem",
                                "配电箱检查",
                                "beforeRectificationPhoto",
                                "/uploads/safety-before-photo.jpg",
                                "hazardDescription",
                                "配电箱未上锁")))))
            .path("data");
    String recordId = created.path("id").asText();

    postJson(
        "/api/pingan/three-checks/safety-check/records/" + recordId + "/submit",
        token,
        Map.of());

    JsonNode orders =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceType=SAFETY_INSPECTION&sourceRecordId="
                    + recordId,
                token)
            .path("data");
    JsonNode detail =
        getJson(
                "/api/pingan/hazard-rectification/orders/"
                    + orders.path("items").get(0).path("id").asText(),
                token)
            .path("data");

    assertThat(detail.path("items").get(0).path("beforePhoto").asText())
        .isEqualTo("/uploads/safety-before-photo.jpg");

    jdbcTemplate.update(
        "update hazard_rectification_order_item set before_photo = null where order_id = ?",
        orders.path("items").get(0).path("id").asLong());
    JsonNode fallbackDetail =
        getJson(
                "/api/pingan/hazard-rectification/orders/"
                    + orders.path("items").get(0).path("id").asText(),
                token)
            .path("data");
    assertThat(fallbackDetail.path("items").get(0).path("beforePhoto").asText())
        .isEqualTo("/uploads/safety-before-photo.jpg");
  }

  @Test
  void autoCreatesFullChainChildrenWhenDispatchIsCreatedAsEffective()
      throws Exception {
    String token = login("admin", "123456");

    JsonNode effectiveDispatch =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-09-03",
                    "status", "已生效",
                    "payload",
                        Map.of(
                            "teamTask", "新增即生效派班",
                            "dispatchStatus", "已生效",
                            "statusLabel", "已生效")))
            .path("data");

    long rootId = effectiveDispatch.path("id").asLong();
    assertThat(effectiveDispatch.path("status").asText()).isEqualTo("OPENED");
    assertThat(childCount(rootId)).isEqualTo(4);
    assertThat(childByModule(rootId, "pre-shift-meeting").path("status").asText()).isEqualTo("DRAFT");
    assertThat(childByModule(rootId, "pre-shift-inspection").path("source_record_id").asText())
        .isEqualTo("auto-dispatch-" + rootId + "-pre-shift-inspection");
  }

  @Test
  void effectiveDispatchGeneratedChildrenAreAssignedToTeamLeader()
      throws Exception {
    long leaderUserId = 9109L;
    String leaderUsername = "dispatch_child_team_leader";
    cleanupThreeCheckAccessFixture(leaderUserId, leaderUsername, "TCR-DCL-");
    String originalLeaderUsername =
        jdbcTemplate.queryForObject(
            "select leader_username from sys_team_profile where org_id = ? and deleted = 0",
            String.class,
            SOURCE_TEAM_ID);
    try {
      createThreeCheckRoleUser(
          leaderUserId, leaderUsername, "派班子任务班长", SOURCE_TEAM_ID, "TEAM_LEADER");
      createTeamLeaderMember(SOURCE_TEAM_ID, leaderUserId, leaderUsername, "派班子任务班长");
      jdbcTemplate.update(
          "update sys_team_profile set leader_username = ? where org_id = ? and deleted = 0",
          leaderUsername,
          SOURCE_TEAM_ID);

      String token = login("admin", "123456");
      JsonNode effectiveDispatch =
          postJson(
                  "/api/pingan/three-checks/team-dispatch/records",
                  token,
                  Map.of(
                      "companyId", SOURCE_COMPANY_ID,
                      "departmentId", SOURCE_DEPARTMENT_ID,
                      "teamId", SOURCE_TEAM_ID,
                      "ownerUserId", 2,
                      "businessDate", "2036-09-03",
                      "status", "已生效",
                      "payload",
                          Map.of(
                              "teamTask", "班长负责子任务派班",
                              "dispatchStatus", "已生效",
                              "statusLabel", "已生效")))
              .path("data");

      long rootId = effectiveDispatch.path("id").asLong();
      assertThat(childCount(rootId)).isEqualTo(4);
      assertThat(childByModule(rootId, "pre-shift-meeting").path("owner_user_id").asLong())
          .isEqualTo(leaderUserId);
      assertThat(childByModule(rootId, "pre-shift-inspection").path("owner_user_id").asLong())
          .isEqualTo(leaderUserId);
      assertThat(childByModule(rootId, "mid-shift-inspection").path("owner_user_id").asLong())
          .isEqualTo(leaderUserId);
      assertThat(childByModule(rootId, "post-shift-inspection").path("owner_user_id").asLong())
          .isEqualTo(leaderUserId);
    } finally {
      jdbcTemplate.update(
          "update sys_team_profile set leader_username = ? where org_id = ? and deleted = 0",
          originalLeaderUsername,
          SOURCE_TEAM_ID);
    }
  }

  @Test
  void autoCreatesFullChainChildrenWhenDispatchIsUpdatedAsEffective()
      throws Exception {
    String token = login("admin", "123456");

    JsonNode draftDispatch =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-09-04",
                    "status", "未生效",
                    "payload", Map.of("teamTask", "编辑生效派班", "dispatchStatus", "未生效")))
            .path("data");

    long rootId = draftDispatch.path("id").asLong();
    assertThat(childCount(rootId)).isZero();

    JsonNode updated =
        putJson(
                "/api/pingan/three-checks/team-dispatch/records/" + rootId,
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-09-04",
                    "status", "已生效",
                    "version", draftDispatch.path("version").asLong(),
                    "payload", Map.of("teamTask", "编辑生效派班", "dispatchStatus", "已生效")))
            .path("data");

    assertThat(updated.path("status").asText()).isEqualTo("OPENED");
    assertThat(childCount(rootId)).isEqualTo(4);
  }

  @Test
  void autoCreatedChildrenAreScopedToTheirOwnRootDispatchWhenSameTeamHasTwoDispatches()
      throws Exception {
    String token = login("admin", "123456");

    long firstRootId =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-09-02",
                    "payload", Map.of("teamTask", "上午派班", "statusLabel", "未生效")))
            .path("data")
            .path("id")
            .asLong();
    long secondRootId =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-09-02",
                    "payload", Map.of("teamTask", "下午派班", "statusLabel", "未生效")))
            .path("data")
            .path("id")
            .asLong();

    postJson(
        "/api/pingan/three-checks/team-dispatch/records/" + firstRootId + "/submit",
        token,
        Map.of());
    postJson(
        "/api/pingan/three-checks/team-dispatch/records/" + secondRootId + "/submit",
        token,
        Map.of());

    assertThat(childCount(firstRootId)).isEqualTo(4);
    assertThat(childCount(secondRootId)).isEqualTo(4);
    assertThat(idsIn(getJson("/api/pingan/three-checks/flows/" + firstRootId, token).path("data").path("stages")))
        .doesNotContain(String.valueOf(secondRootId));
  }

  @Test
  void connectsHazardInspectionModulesToGenericRecordsWithRequestedStatusesAndAttachments()
      throws Exception {
    String token = login("admin", "123456");

    for (HazardModuleCase module : HAZARD_MODULE_CASES) {
      JsonNode draft =
          createGenericRecord(
              token,
              module.moduleKey(),
              module.draftLabel(),
              Map.of(
                  "statusLabel", module.draftLabel(),
                  "hazardDescription", module.moduleKey() + " draft"));
      assertThat(draft.path("moduleKey").asText()).isEqualTo(module.moduleKey());
      assertThat(draft.path("status").asText()).isEqualTo("DRAFT");
      assertThat(draft.path("statusLabel").asText()).isEqualTo(module.draftLabel());

      String id = draft.path("id").asText();
      JsonNode submitted =
          postJson(
                  "/api/pingan/three-checks/" + module.moduleKey() + "/records/" + id + "/submit",
                  token,
                  Map.of())
              .path("data");
      assertThat(submitted.path("status").asText()).isEqualTo("OPENED");
      assertThat(submitted.path("statusLabel").asText()).isEqualTo(module.openedLabel());

      MockMultipartFile image =
          new MockMultipartFile(
              "file",
              module.moduleKey() + ".jpg",
              "image/jpeg",
              tinyJpeg());
      JsonNode uploadResponse =
          objectMapper
              .readTree(
                  mockMvc
                      .perform(
                          multipart(
                                  "/api/pingan/three-checks/"
                                      + module.moduleKey()
                                      + "/records/"
                                      + id
                                      + "/attachments")
                              .file(image)
                              .param("fileKind", "IMAGE")
                              .header("Authorization", "Bearer " + token))
                      .andExpect(status().isOk())
                      .andReturn()
                      .getResponse()
                      .getContentAsString(StandardCharsets.UTF_8))
              .path("data");
    assertThat(uploadResponse.path("url").asText()).startsWith("/api/attachments/");
      assertThat(
              jdbcTemplate.queryForObject(
                  "select count(*) from biz_attachment where biz_type = ? and biz_id = ? and file_kind = 'IMAGE'",
                  Integer.class,
                  module.bizType(),
                  Long.parseLong(id)))
          .isEqualTo(1);

      if (module.archivedLabel() != null) {
        JsonNode archived =
            createGenericRecord(
                token,
                module.moduleKey(),
                module.archivedLabel(),
                Map.of("statusLabel", module.archivedLabel()));
        assertThat(archived.path("status").asText()).isEqualTo("ARCHIVED");
        assertThat(archived.path("statusLabel").asText()).isEqualTo(module.archivedLabel());
      }
    }
  }

  @Test
  void returnsHazardInspectionDocumentFlow() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        createGenericRecord(
            token,
            "safety-check",
            "待检查",
            Map.of(
                "statusLabel",
                "待检查",
                "hazardDescription",
                "document flow hazard",
                "checkItems",
                List.of(
                    Map.of(
                        "lineId",
                        "safety-document-flow-hazard",
                        "checkResult",
                        "有隐患",
                        "checkItem",
                        "配电箱检查",
                        "hazardDescription",
                        "document flow hazard"))));
    String id = created.path("id").asText();

    postJson(
        "/api/pingan/three-checks/safety-check/records/" + id + "/submit",
        token,
        Map.of());
    JsonNode linkedFlow =
        getJson(
                "/api/pingan/hazard-inspection/safety-check/records/" + id + "/document-flow",
                token)
            .path("data");
    JsonNode linkedOrder = linkedFlow.path("linkedRectificationOrder");
    assertThat(linkedOrder.path("sourceType").asText()).isEqualTo("SAFETY_INSPECTION");
    assertThat(linkedOrder.path("sourceModuleKey").asText()).isEqualTo("safety-check");
    assertThat(linkedOrder.path("statusLabel").asText()).isEqualTo("待派发");
    hazardOrderAction(
        token,
        linkedOrder.path("id").asText(),
        "ISSUE_RECTIFICATION",
        Map.of(
            "rectificationResponsibleUserId",
            3,
            "rectificationRequirement",
            "按要求整改",
            "rectificationDeadline",
            "2026-05-25 18:00:00",
            "acceptanceUserId",
            4));
    JsonNode reminded =
        postJson(
                "/api/pingan/three-checks/safety-check/records/" + id + "/remind",
                token,
                Map.of())
            .path("data");
    putJson(
        "/api/pingan/three-checks/safety-check/records/" + id,
        token,
        Map.of(
            "companyId",
            SOURCE_COMPANY_ID,
            "departmentId",
            SOURCE_DEPARTMENT_ID,
            "teamId",
            SOURCE_TEAM_ID,
            "ownerUserId",
            2,
            "businessDate",
            "2026-05-19",
            "status",
            "已检查",
            "payload",
            Map.of("statusLabel", "已检查", "hazardDescription", "document flow updated"),
            "version",
            reminded.path("version").asInt()));

    JsonNode flow =
        getJson(
                "/api/pingan/hazard-inspection/safety-check/records/" + id + "/document-flow",
                token)
            .path("data");

    assertThat(flow.path("record").path("id").asText()).isEqualTo(id);
    assertThat(flow.path("record").path("moduleKey").asText()).isEqualTo("safety-check");
    JsonNode statusLogs = flow.path("statusLogs");
    assertThat(idsIn(statusLogs).isEmpty()).isFalse();
    assertThat(actionsIn(statusLogs)).contains("CREATE", "SUBMIT", "REMIND", "UPDATE");
    assertThat(statusLogs.get(0).path("operatorName").asText()).isEqualTo("系统管理员");
    assertThat(statusLogs.get(0).path("toStatusLabel").asText()).isEqualTo("待检查");
    JsonNode submitLog =
        firstLogByAction(statusLogs, "SUBMIT");
    assertThat(submitLog.path("fromStatusLabel").asText()).isEqualTo("待检查");
    assertThat(submitLog.path("toStatusLabel").asText()).isEqualTo("已检查");
    JsonNode history =
        getJson(
                "/api/pingan/hazard-inspection/safety-check/records/" + id + "/change-history",
                token)
            .path("data");
    assertThat(history.path("items").toString()).doesNotContain("下发整改");
  }

  @Test
  void legacyQuickShotWithoutUnifiedOrderRejectsRectificationActionsButKeepsDocumentFlowReadable()
      throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        createGenericRecord(
            token,
            "quick-shot",
            "已审核",
            Map.of(
                "statusLabel",
                "已审核",
                "status",
                "已审核",
                "reviewRemark",
                "历史审核记录",
                "hazardDescription",
                "quick shot workflow hazard"));

    String id = created.path("id").asText();
    assertThat(created.path("status").asText()).isEqualTo("REVIEWED");
    assertThat(created.path("statusLabel").asText()).isEqualTo("已审核");
    assertThat(created.path("payload").hasNonNull("rectificationOrderId")).isFalse();

    String response =
        mockMvc
            .perform(
                post("/api/pingan/three-checks/quick-shot/records/" + id + "/workflow-actions")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "action",
                                "ISSUE_RECTIFICATION",
                                "payload",
                                Map.of(
                                    "rectificationResponsiblePerson",
                                    "湖贝班长",
                                    "rectificationDeadline",
                                    "2026-05-30"),
                                "version",
                                created.path("version").asInt()))))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    assertThat(response).contains("统一隐患整改工单");

    JsonNode flow =
        getJson(
                "/api/pingan/hazard-inspection/quick-shot/records/" + id + "/document-flow",
                token)
            .path("data");
    assertThat(actionsIn(flow.path("statusLogs"))).containsExactly("CREATE");
    assertThat(firstLogByAction(flow.path("statusLogs"), "CREATE").path("payload").path("hazardDescription").asText())
        .isEqualTo("quick shot workflow hazard");
    assertThat(firstLogByAction(flow.path("statusLogs"), "CREATE").path("payload").path("reviewRemark").asText())
        .isEqualTo("历史审核记录");
    assertThat(flow.path("linkedRectificationOrder").isNull()).isTrue();
  }

  @Test
  void approvedQuickShotCreatesUnifiedHazardRectificationOrderButRejectedDoesNot()
      throws Exception {
    String token = login("admin", "123456");
    JsonNode rejectedCreated =
        postJson(
                "/api/pingan/three-checks/quick-shot/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-10-03",
                    "payload",
                    Map.of("hazardDescription", "quick shot rejected hazard")))
            .path("data");
    postJson(
        "/api/pingan/three-checks/quick-shot/records/"
            + rejectedCreated.path("id").asText()
            + "/workflow-actions",
        token,
        Map.of(
            "action",
            "REJECT",
            "payload",
            Map.of("rejectReason", "现场复核隐患不成立"),
            "version",
            rejectedCreated.path("version").asInt()));
    JsonNode rejectedOrders =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceType=QUICK_SHOT&sourceRecordId="
                    + rejectedCreated.path("id").asText(),
                token)
            .path("data");
    assertThat(rejectedOrders.path("total").asInt()).isEqualTo(0);

    JsonNode approvedCreated =
        postJson(
                "/api/pingan/three-checks/quick-shot/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-10-04",
                    "payload",
                    Map.of(
                        "riskType",
                        "现场环境",
                        "aiEnabled",
                        "是",
                        "hazardDescription",
                        "随手拍发现通道堆物",
                        "hazardImage",
                        "/uploads/quick-shot-before.mp4",
                        "uploadTime",
                        "2026-10-04 09:35")))
            .path("data");
    JsonNode approved =
        quickShotWorkflowAction(
            token,
            approvedCreated.path("id").asText(),
            "APPROVE",
            approvedCreated.path("version").asInt());
    assertThat(approved.path("status").asText()).isEqualTo("REVIEWED");

    JsonNode orders =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceType=QUICK_SHOT&sourceRecordId="
                    + approvedCreated.path("id").asText(),
                token)
            .path("data");
    assertThat(orders.path("total").asInt()).isEqualTo(1);
    JsonNode order = orders.path("items").get(0);
    assertThat(order.path("sourceType").asText()).isEqualTo("QUICK_SHOT");
    assertThat(order.path("sourceModuleKey").asText()).isEqualTo("quick-shot");
    assertThat(order.path("hazardCount").asInt()).isEqualTo(1);
    JsonNode detail =
        getJson(
                "/api/pingan/hazard-rectification/orders/"
                    + order.path("id").asText(),
                token)
            .path("data");
    assertThat(detail.path("items")).hasSize(1);
    assertThat(detail.path("items").get(0).path("hazardDescription").asText())
        .isEqualTo("随手拍发现通道堆物");
    assertThat(detail.path("items").get(0).path("beforeVideo").asText())
        .isEqualTo("/uploads/quick-shot-before.mp4");
    assertThat(detail.path("items").get(0).path("sourceSnapshot").path("aiEnabled").asText())
        .isEqualTo("是");
    assertThat(detail.path("items").get(0).path("sourceSnapshot").path("uploadTime").asText())
        .isEqualTo("2026-10-04 09:35");
    assertThat(detail.path("flowLogs").toString()).contains("随手拍审核通过自动生成工单");

    JsonNode pendingRectify =
        hazardOrderAction(
            token,
            order.path("id").asText(),
            "ISSUE_RECTIFICATION",
            Map.of(
                "rectificationResponsibleUserId",
                2,
                "rectificationDepartmentId",
                SOURCE_DEPARTMENT_ID,
                "acceptanceUserId",
                1,
                "rectificationRequirement",
                "清理通道堆物",
                "rectificationDeadline",
                "2026-10-08 18:00:00"));
    assertThat(pendingRectify.path("status").asText()).isEqualTo("PENDING_RECTIFY");

    JsonNode flow =
        getJson(
                "/api/pingan/hazard-inspection/quick-shot/records/"
                    + approvedCreated.path("id").asText()
                    + "/document-flow",
                token)
            .path("data");
    assertThat(actionsIn(flow.path("statusLogs"))).contains("CREATE", "APPROVE");
    JsonNode linkedOrder = flow.path("linkedRectificationOrder");
    assertThat(linkedOrder.path("id").asText()).isEqualTo(order.path("id").asText());
    assertThat(linkedOrder.path("orderNo").asText()).isEqualTo(order.path("orderNo").asText());
    assertThat(linkedOrder.path("status").asText()).isEqualTo("PENDING_RECTIFY");
    assertThat(actionsIn(linkedOrder.path("flowLogs"))).contains("CREATE", "ISSUE_RECTIFICATION");
    assertThat(
            firstLogByAction(linkedOrder.path("flowLogs"), "ISSUE_RECTIFICATION")
                .path("payload")
                .path("rectificationRequirement")
                .asText())
        .isEqualTo("清理通道堆物");
  }

  @Test
  void approvedQuickShotCopiesUploadedAttachmentUrlToOrderItemMedia() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/quick-shot/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-10-12",
                    "payload",
                    Map.of(
                        "hazardDescription",
                        "随手拍发现通道堆物",
                        "hazardImage",
                        "quick-shot-before.jpg",
                        "uploadTime",
                        "2026-10-12 09:35")))
            .path("data");
    String recordId = created.path("id").asText();
    JsonNode uploadResponse =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        multipart(
                                "/api/pingan/three-checks/quick-shot/records/"
                                    + recordId
                                    + "/attachments")
                            .file(
                                new MockMultipartFile(
                                    "file",
                                    "quick-shot-before.jpg",
                                    "image/jpeg",
                                    tinyJpeg()))
                            .param("fileKind", "IMAGE")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");

    JsonNode approved =
        quickShotWorkflowAction(token, recordId, "APPROVE", created.path("version").asInt() + 1);
    assertThat(approved.path("status").asText()).isEqualTo("REVIEWED");

    JsonNode orders =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceType=QUICK_SHOT&sourceRecordId="
                    + recordId,
                token)
            .path("data");
    JsonNode detail =
        getJson(
                "/api/pingan/hazard-rectification/orders/"
                    + orders.path("items").get(0).path("id").asText(),
                token)
            .path("data");

    assertThat(detail.path("items").get(0).path("beforePhoto").asText())
        .isEqualTo(uploadResponse.path("url").asText());

    jdbcTemplate.update(
        "update hazard_rectification_order_item set before_photo = null where order_id = ?",
        orders.path("items").get(0).path("id").asLong());
    JsonNode fallbackDetail =
        getJson(
                "/api/pingan/hazard-rectification/orders/"
                    + orders.path("items").get(0).path("id").asText(),
                token)
            .path("data");
    assertThat(fallbackDetail.path("items").get(0).path("beforePhoto").asText())
        .isEqualTo(uploadResponse.path("url").asText());
  }

  @Test
  void withdrawApprovedQuickShotReturnsToPendingReviewAndCancelsLinkedOrder()
      throws Exception {
    String token = login("admin", "123456");
    JsonNode created =
        postJson(
                "/api/pingan/three-checks/quick-shot/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-10-05",
                    "payload",
                    Map.of(
                        "hazardDescription",
                        "随手拍发现临边堆物",
                        "hazardImage",
                        "/uploads/quick-shot-withdraw.jpg",
                        "uploadTime",
                        "2026-10-05 09:35")))
            .path("data");

    JsonNode approved =
        quickShotWorkflowAction(
            token,
            created.path("id").asText(),
            "APPROVE",
            created.path("version").asInt());
    assertThat(approved.path("status").asText()).isEqualTo("REVIEWED");
    assertThat(approved.path("canWithdraw").asBoolean()).isTrue();
    assertThat(approved.path("canRemind").asBoolean()).isTrue();
    JsonNode list =
        getJson("/api/pingan/three-checks/quick-shot/records?status=all", token).path("data");
    JsonNode listed = objectMapper.createObjectNode();
    for (JsonNode item : list.path("items")) {
      if (created.path("id").asText().equals(item.path("id").asText())) {
        listed = item;
      }
    }
    assertThat(listed.path("canWithdraw").asBoolean()).isTrue();
    assertThat(listed.path("canRemind").asBoolean()).isTrue();
    JsonNode reminded =
        postJson(
                "/api/pingan/three-checks/quick-shot/records/"
                    + created.path("id").asText()
                    + "/remind",
                token,
                Map.of())
            .path("data");
    assertThat(reminded.path("status").asText()).isEqualTo("REVIEWED");
    assertThat(reminded.path("reminderCount").asInt()).isEqualTo(1);

    JsonNode orders =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceType=QUICK_SHOT&sourceRecordId="
                    + created.path("id").asText(),
                token)
            .path("data");
    JsonNode order = orders.path("items").get(0);

    JsonNode withdrawn =
        postJson(
                "/api/pingan/three-checks/quick-shot/records/"
                    + created.path("id").asText()
                    + "/withdraw",
                token,
                Map.of("reason", "审核通过后撤回重审"))
            .path("data");
    assertThat(withdrawn.path("status").asText()).isEqualTo("PENDING_REVIEW");
    assertThat(withdrawn.path("statusLabel").asText()).isEqualTo("待审核");
    assertThat(withdrawn.path("canWithdraw").asBoolean()).isFalse();
    assertThat(withdrawn.path("payload").path("rectificationStatus").asText()).isEqualTo("CANCELLED");
    assertThat(withdrawn.path("payload").path("rectificationStatusLabel").asText()).isEqualTo("已作废");

    JsonNode cancelledOrder =
        getJson("/api/pingan/hazard-rectification/orders/" + order.path("id").asText(), token)
            .path("data");
    assertThat(cancelledOrder.path("status").asText()).isEqualTo("CANCELLED");
    assertThat(cancelledOrder.path("statusLabel").asText()).isEqualTo("已作废");
    assertThat(cancelledOrder.path("flowLogs").toString()).contains("随手拍撤回自动作废");
  }

  @Test
  void quickShotWithdrawRequiresHazardCloseInsteadOfThreeCheckWithdraw()
      throws Exception {
    long withdrawOnlyUserId = 9513L;
    createPermissionFixtureUser(
        withdrawOnlyUserId,
        9513L,
        "quick_shot_withdraw_only",
        "随手拍撤回权限用户",
        "QUICK_SHOT_WITHDRAW_ONLY",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VOID");

    String adminToken = login("admin", "123456");
    JsonNode created =
        postJson(
                "/api/pingan/three-checks/quick-shot/records",
                adminToken,
                quickShotPayload(withdrawOnlyUserId, "2026-11-13"))
            .path("data");
    JsonNode approved =
        quickShotWorkflowAction(
            adminToken,
            created.path("id").asText(),
            "APPROVE",
            created.path("version").asInt());

    String withdrawOnlyToken = login("quick_shot_withdraw_only", "123456");
    mockMvc
        .perform(
            post(
                    "/api/pingan/three-checks/quick-shot/records/"
                        + approved.path("id").asText()
                        + "/withdraw")
                .header("Authorization", "Bearer " + withdrawOnlyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", "缺少隐患关闭权限"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void miniProgramQuickShotWithdrawRequiresHazardClose()
      throws Exception {
    long withdrawOnlyUserId = 9514L;
    createPermissionFixtureUser(
        withdrawOnlyUserId,
        9514L,
        "mini_quick_shot_withdraw_only",
        "小程序随手拍撤回权限用户",
        "MINI_QUICK_SHOT_WITHDRAW_ONLY",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VOID");

    String adminToken = login("admin", "123456");
    JsonNode created =
        postJson(
                "/api/mini/pingan/three-checks/quick-shot/records",
                adminToken,
                miniQuickShotPayload(
                    withdrawOnlyUserId,
                    "2026-11-14",
                    "wx-quick-shot-withdraw-rbac",
                    "wx-quick-shot-withdraw-rbac-req"))
            .path("data");
    JsonNode approved =
        quickShotWorkflowAction(
            adminToken,
            created.path("id").asText(),
            "APPROVE",
            created.path("version").asInt());

    String withdrawOnlyToken = login("mini_quick_shot_withdraw_only", "123456");
    mockMvc
        .perform(
            post(
                    "/api/mini/pingan/three-checks/quick-shot/records/"
                        + approved.path("id").asText()
                        + "/withdraw")
                .header("Authorization", "Bearer " + withdrawOnlyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", "缺少隐患关闭权限"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void migratedQuickShotRejectsLegacyRectificationActions() throws Exception {
    String token = login("admin", "123456");
    JsonNode created =
        postJson(
                "/api/pingan/three-checks/quick-shot/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-10-05",
                    "payload",
                    Map.of(
                        "riskType",
                        "现场环境",
                        "hazardDescription",
                        "随手拍发现安全出口堵塞")))
            .path("data");

    JsonNode approved =
        quickShotWorkflowAction(token, created.path("id").asText(), "APPROVE", created.path("version").asInt());
    assertThat(approved.path("payload").path("rectificationOrderId").asText()).isNotBlank();

    String response =
        mockMvc
            .perform(
                post(
                        "/api/pingan/three-checks/quick-shot/records/"
                            + created.path("id").asText()
                            + "/workflow-actions")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "action",
                                "ISSUE_RECTIFICATION",
                                "payload",
                                Map.of(
                                    "rectificationResponsiblePerson",
                                    "湖贝班长",
                                    "rectificationDeadline",
                                    "2026-10-08",
                                    "rectificationRequirement",
                                    "清理堵塞物"),
                                "version",
                                approved.path("version").asInt()))))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    assertThat(response).contains("统一隐患整改工单");
  }

  @Test
  void miniProgramQuickShotWorkflowActionsSyncToPcDocumentFlowWithSnapshots() throws Exception {
    String token = login("admin", "123456");
    JsonNode created =
        postJson(
                "/api/mini/pingan/three-checks/quick-shot/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-05-20",
                    "payload", Map.of("hazardDescription", "mini quick shot hazard"),
                    "sourceRecordId", "wx-quick-shot-flow-001",
                    "clientRequestId", "wx-quick-shot-flow-req-001"))
            .path("data");
    String id = created.path("id").asText();

    JsonNode approved =
        postJson(
                "/api/mini/pingan/three-checks/quick-shot/records/" + id + "/workflow-actions",
                token,
                Map.of(
                    "action",
                    "APPROVE",
                    "version",
                    created.path("version").asInt(),
                    "payload",
                    Map.of("reviewRemark", "小程序审核确认")))
            .path("data");

    assertThat(created.path("sourceChannel").asText()).isEqualTo("WECHAT_MINI_PROGRAM");
    assertThat(approved.path("statusLabel").asText()).isEqualTo("已审核");
    assertThat(approved.path("payload").path("rectificationOrderId").asText()).isNotBlank();

    String legacyActionResponse =
        mockMvc
            .perform(
                post("/api/mini/pingan/three-checks/quick-shot/records/" + id + "/workflow-actions")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "action",
                                "ISSUE_RECTIFICATION",
                                "version",
                                approved.path("version").asInt(),
                                "payload",
                                Map.of(
                                    "rectificationResponsiblePerson",
                                    "小程序整改人",
                                    "rectificationDeadline",
                                    "2026-05-31")))))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    assertThat(legacyActionResponse).contains("统一隐患整改工单");

    JsonNode flow =
        getJson(
                "/api/pingan/hazard-inspection/quick-shot/records/" + id + "/document-flow",
                token)
            .path("data");
    assertThat(actionsIn(flow.path("statusLogs"))).contains("CREATE", "APPROVE");
    assertThat(actionsIn(flow.path("statusLogs"))).doesNotContain("ISSUE_RECTIFICATION");
    assertThat(firstLogByAction(flow.path("statusLogs"), "CREATE").path("payload").path("hazardDescription").asText())
        .isEqualTo("mini quick shot hazard");
    assertThat(firstLogByAction(flow.path("statusLogs"), "APPROVE").path("payload").path("reviewRemark").asText())
        .isEqualTo("小程序审核确认");
  }

  @Test
  void quickShotCreationRequiresHazardReportInsteadOfThreeCheckExecute() throws Exception {
    long executeOnlyUserId = 9501L;
    long reportUserId = 9502L;
    createPermissionFixtureUser(
        executeOnlyUserId,
        9501L,
        "quick_shot_execute_only",
        "随手拍执行权限用户",
        "QUICK_SHOT_EXECUTE_ONLY",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE");
    createPermissionFixtureUser(
        reportUserId,
        9502L,
        "quick_shot_reporter",
        "随手拍上报用户",
        "QUICK_SHOT_REPORTER",
        "PINGAN_HAZARD_QUICK_SHOT_VIEW",
        "PINGAN_HAZARD_QUICK_SHOT_REPORT");

    String executeOnlyToken = login("quick_shot_execute_only", "123456");
    mockMvc
        .perform(
            post("/api/pingan/three-checks/quick-shot/records")
                .header("Authorization", "Bearer " + executeOnlyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(quickShotPayload(executeOnlyUserId, "2026-11-01"))))
        .andExpect(status().isForbidden());

    String reportToken = login("quick_shot_reporter", "123456");
    JsonNode created =
        postJson(
                "/api/pingan/three-checks/quick-shot/records",
                reportToken,
                quickShotPayload(reportUserId, "2026-11-02"))
            .path("data");

    assertThat(created.path("moduleKey").asText()).isEqualTo("quick-shot");
    assertThat(created.path("owner").asText()).isEqualTo("随手拍上报用户");
  }

  @Test
  void hazardRectificationSubmitRequiresReportPermissionInsteadOfCreate() throws Exception {
    long createOnlyUserId = 9515L;
    long reportUserId = 9516L;
    createPermissionFixtureUser(
        createOnlyUserId,
        9515L,
        "hazard_rectification_create_only",
        "隐患整改新增权限用户",
        "HAZARD_RECTIFICATION_CREATE_ONLY",
        "PINGAN_HAZARD_RECTIFICATION_VIEW",
        "PINGAN_HAZARD_RECTIFICATION_CREATE");
    createPermissionFixtureUser(
        reportUserId,
        9516L,
        "hazard_rectification_reporter",
        "隐患整改上报用户",
        "HAZARD_RECTIFICATION_REPORTER",
        "PINGAN_HAZARD_RECTIFICATION_VIEW",
        "PINGAN_HAZARD_RECTIFICATION_REPORT");

    String createOnlyToken = login("hazard_rectification_create_only", "123456");
    JsonNode createOnlyDraft =
        postJson(
                "/api/pingan/three-checks/hazard-rectification/records",
                createOnlyToken,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    createOnlyUserId,
                    "businessDate",
                    "2026-11-15",
                    "payload",
                    Map.of("hazardDescription", "create only rectification hazard")))
            .path("data");
    mockMvc
        .perform(
            post(
                    "/api/pingan/three-checks/hazard-rectification/records/"
                        + createOnlyDraft.path("id").asText()
                        + "/submit")
                .header("Authorization", "Bearer " + createOnlyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());

    JsonNode reportDraft =
        postJson(
                "/api/pingan/three-checks/hazard-rectification/records",
                login("admin", "123456"),
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    reportUserId,
                    "businessDate",
                    "2026-11-16",
                    "payload",
                    Map.of("hazardDescription", "report rectification hazard")))
            .path("data");
    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/hazard-rectification/records/"
                    + reportDraft.path("id").asText()
                    + "/submit",
                login("hazard_rectification_reporter", "123456"),
                Map.of())
            .path("data");

    assertThat(submitted.path("status").asText()).isEqualTo("OPENED");
    assertThat(submitted.path("statusLabel").asText()).isEqualTo("已整改");
  }

  @Test
  void miniProgramQuickShotCreationRequiresHazardReport() throws Exception {
    long executeOnlyUserId = 9503L;
    long reportUserId = 9504L;
    createPermissionFixtureUser(
        executeOnlyUserId,
        9503L,
        "mini_quick_shot_execute_only",
        "小程序随手拍执行权限用户",
        "MINI_QUICK_SHOT_EXECUTE_ONLY",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE");
    createPermissionFixtureUser(
        reportUserId,
        9504L,
        "mini_quick_shot_reporter",
        "小程序随手拍上报用户",
        "MINI_QUICK_SHOT_REPORTER",
        "PINGAN_HAZARD_QUICK_SHOT_VIEW",
        "PINGAN_HAZARD_QUICK_SHOT_REPORT");

    String executeOnlyToken = login("mini_quick_shot_execute_only", "123456");
    mockMvc
        .perform(
            post("/api/mini/pingan/three-checks/quick-shot/records")
                .header("Authorization", "Bearer " + executeOnlyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        miniQuickShotPayload(
                            executeOnlyUserId,
                            "2026-11-03",
                            "wx-quick-shot-rbac-denied",
                            "wx-quick-shot-rbac-denied-req"))))
        .andExpect(status().isForbidden());

    String reportToken = login("mini_quick_shot_reporter", "123456");
    JsonNode created =
        postJson(
                "/api/mini/pingan/three-checks/quick-shot/records",
                reportToken,
                miniQuickShotPayload(
                    reportUserId,
                    "2026-11-04",
                    "wx-quick-shot-rbac-allowed",
                    "wx-quick-shot-rbac-allowed-req"))
            .path("data");

    assertThat(created.path("moduleKey").asText()).isEqualTo("quick-shot");
    assertThat(created.path("sourceChannel").asText()).isEqualTo("WECHAT_MINI_PROGRAM");
    assertThat(created.path("owner").asText()).isEqualTo("小程序随手拍上报用户");
  }

  @Test
  void quickShotWorkflowActionsUseHazardPermissionsInsteadOfThreeCheckExecute()
      throws Exception {
    long executeOnlyUserId = 9505L;
    long rectificationUserId = 9506L;
    createPermissionFixtureUser(
        executeOnlyUserId,
        9505L,
        "quick_shot_workflow_execute_only",
        "随手拍流程执行权限用户",
        "QUICK_SHOT_WORKFLOW_EXECUTE_ONLY",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT");
    createPermissionFixtureUser(
        rectificationUserId,
        9506L,
        "quick_shot_rectification_user",
        "随手拍整改用户",
        "QUICK_SHOT_RECTIFICATION_USER",
        "PINGAN_HAZARD_QUICK_SHOT_VIEW",
        "PINGAN_HAZARD_RECTIFICATION_RECTIFY");

    String adminToken = login("admin", "123456");
    JsonNode executeOnlyRecord =
        postJson(
                "/api/pingan/three-checks/quick-shot/records",
                adminToken,
                quickShotPayload(executeOnlyUserId, "2026-11-05"))
            .path("data");
    String executeOnlyToken = login("quick_shot_workflow_execute_only", "123456");
    mockMvc
        .perform(
            post(
                    "/api/pingan/three-checks/quick-shot/records/"
                        + executeOnlyRecord.path("id").asText()
                        + "/workflow-actions")
                .header("Authorization", "Bearer " + executeOnlyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "action",
                            "MARK_RECTIFIED",
                            "payload",
                            Map.of("rectificationDescription", "已完成整改"),
                            "version",
                            executeOnlyRecord.path("version").asInt()))))
        .andExpect(status().isForbidden());

    JsonNode rectificationRecord =
        postJson(
                "/api/pingan/three-checks/quick-shot/records",
                adminToken,
                quickShotPayload(rectificationUserId, "2026-11-06"))
            .path("data");
    String rectificationToken = login("quick_shot_rectification_user", "123456");
    String response =
        mockMvc
            .perform(
                post(
                        "/api/pingan/three-checks/quick-shot/records/"
                            + rectificationRecord.path("id").asText()
                            + "/workflow-actions")
                    .header("Authorization", "Bearer " + rectificationToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "action",
                                "MARK_RECTIFIED",
                                "payload",
                                Map.of("rectificationDescription", "已完成整改"),
                                "version",
                                rectificationRecord.path("version").asInt()))))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    assertThat(response).contains("统一隐患整改工单");
  }

  @Test
  void quickShotAttachmentsRequireHazardReportInsteadOfThreeCheckExecute()
      throws Exception {
    long executeOnlyUserId = 9507L;
    long reportUserId = 9508L;
    createPermissionFixtureUser(
        executeOnlyUserId,
        9507L,
        "quick_shot_attachment_execute_only",
        "随手拍附件执行权限用户",
        "QUICK_SHOT_ATTACHMENT_EXECUTE_ONLY",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_UPDATE");
    createPermissionFixtureUser(
        reportUserId,
        9508L,
        "quick_shot_attachment_reporter",
        "随手拍附件上报用户",
        "QUICK_SHOT_ATTACHMENT_REPORTER",
        "PINGAN_HAZARD_QUICK_SHOT_VIEW",
        "PINGAN_HAZARD_QUICK_SHOT_REPORT");

    String adminToken = login("admin", "123456");
    JsonNode executeOnlyRecord =
        postJson(
                "/api/pingan/three-checks/quick-shot/records",
                adminToken,
                quickShotPayload(executeOnlyUserId, "2026-11-07"))
            .path("data");
    String executeOnlyToken = login("quick_shot_attachment_execute_only", "123456");
    mockMvc
        .perform(
            multipart(
                    "/api/pingan/three-checks/quick-shot/records/"
                        + executeOnlyRecord.path("id").asText()
                        + "/attachments")
                .file(
                    new MockMultipartFile(
                        "file",
                        "quick-shot-execute-only.jpg",
                        "image/jpeg",
                        tinyJpeg()))
                .param("fileKind", "IMAGE")
                .header("Authorization", "Bearer " + executeOnlyToken))
        .andExpect(status().isForbidden());
    assertThat(countAttachmentsByOriginalName("quick-shot-execute-only.jpg")).isZero();

    String reportToken = login("quick_shot_attachment_reporter", "123456");
    JsonNode reportRecord =
        postJson(
                "/api/pingan/three-checks/quick-shot/records",
                reportToken,
                quickShotPayload(reportUserId, "2026-11-08"))
            .path("data");
    String uploadResponse =
        mockMvc
            .perform(
                multipart(
                        "/api/pingan/three-checks/quick-shot/records/"
                            + reportRecord.path("id").asText()
                            + "/attachments")
                    .file(
                        new MockMultipartFile(
                            "file",
                            "quick-shot-report.jpg",
                            "image/jpeg",
                            tinyJpeg()))
                    .param("fileKind", "IMAGE")
                    .header("Authorization", "Bearer " + reportToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode attachment = objectMapper.readTree(uploadResponse).path("data");

    deleteJson(
        "/api/pingan/three-checks/quick-shot/records/"
            + reportRecord.path("id").asText()
            + "/attachments/"
            + attachment.path("id").asText(),
        reportToken);
  }

  @Test
  void quickShotAttachmentsRequireRecordOwnershipWithinHazardReportPermission()
      throws Exception {
    long ownerUserId = 9515L;
    long otherReportUserId = 9516L;
    createPermissionFixtureUser(
        ownerUserId,
        9515L,
        "quick_shot_attachment_owner",
        "随手拍附件本人",
        "QUICK_SHOT_ATTACHMENT_OWNER",
        "PINGAN_HAZARD_QUICK_SHOT_VIEW",
        "PINGAN_HAZARD_QUICK_SHOT_REPORT");
    createPermissionFixtureUser(
        otherReportUserId,
        9516L,
        "quick_shot_attachment_other",
        "随手拍附件他人",
        "QUICK_SHOT_ATTACHMENT_OTHER",
        "PINGAN_HAZARD_QUICK_SHOT_VIEW",
        "PINGAN_HAZARD_QUICK_SHOT_REPORT");

    String ownerToken = login("quick_shot_attachment_owner", "123456");
    JsonNode ownerRecord =
        postJson(
                "/api/pingan/three-checks/quick-shot/records",
                ownerToken,
                quickShotPayload(ownerUserId, "2026-11-15"))
            .path("data");

    String otherToken = login("quick_shot_attachment_other", "123456");
    mockMvc
        .perform(
            multipart(
                    "/api/pingan/three-checks/quick-shot/records/"
                        + ownerRecord.path("id").asText()
                        + "/attachments")
                .file(
                    new MockMultipartFile(
                        "file",
                        "quick-shot-other-report.jpg",
                        "image/jpeg",
                        tinyJpeg()))
                .param("fileKind", "IMAGE")
                .header("Authorization", "Bearer " + otherToken))
        .andExpect(status().isForbidden());
    assertThat(countAttachmentsByOriginalName("quick-shot-other-report.jpg")).isZero();

    String ownerUploadResponse =
        mockMvc
            .perform(
                multipart(
                        "/api/pingan/three-checks/quick-shot/records/"
                            + ownerRecord.path("id").asText()
                            + "/attachments")
                    .file(
                        new MockMultipartFile(
                            "file",
                            "quick-shot-owner-report.jpg",
                            "image/jpeg",
                            tinyJpeg()))
                    .param("fileKind", "IMAGE")
                    .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode attachment = objectMapper.readTree(ownerUploadResponse).path("data");

    mockMvc
        .perform(
            delete(
                    "/api/pingan/three-checks/quick-shot/records/"
                        + ownerRecord.path("id").asText()
                        + "/attachments/"
                        + attachment.path("id").asText())
                .header("Authorization", "Bearer " + otherToken))
        .andExpect(status().isForbidden());
    deleteJson(
        "/api/pingan/three-checks/quick-shot/records/"
            + ownerRecord.path("id").asText()
            + "/attachments/"
            + attachment.path("id").asText(),
        ownerToken);
  }

  @Test
  void quickShotAttachmentRejectsHazardReporterWithDataScopeButNotRecordOwner()
      throws Exception {
    long ownerUserId = 9531L;
    long scopedReportUserId = 9532L;
    createPermissionFixtureUser(
        ownerUserId,
        9531L,
        "quick_shot_attachment_scope_owner",
        "随手拍附件范围本人",
        "QUICK_SHOT_ATTACHMENT_SCOPE_OWNER",
        "PINGAN_HAZARD_QUICK_SHOT_VIEW",
        "PINGAN_HAZARD_QUICK_SHOT_REPORT");
    createPermissionFixtureUser(
        scopedReportUserId,
        9532L,
        "quick_shot_attachment_scope_other",
        "随手拍附件范围他人",
        "QUICK_SHOT_ATTACHMENT_SCOPE_OTHER",
        "PINGAN_HAZARD_QUICK_SHOT_VIEW",
        "PINGAN_HAZARD_QUICK_SHOT_REPORT");
    jdbcTemplate.update(
        "update sys_role set data_scope = 'ORG_AND_CHILDREN' where role_code = ?",
        "QUICK_SHOT_ATTACHMENT_SCOPE_OTHER");

    String ownerToken = login("quick_shot_attachment_scope_owner", "123456");
    JsonNode ownerRecord =
        postJson(
                "/api/pingan/three-checks/quick-shot/records",
                ownerToken,
                quickShotPayload(ownerUserId, "2026-11-17"))
            .path("data");

    String scopedReportToken = login("quick_shot_attachment_scope_other", "123456");
    mockMvc
        .perform(
            multipart(
                    "/api/pingan/three-checks/quick-shot/records/"
                        + ownerRecord.path("id").asText()
                        + "/attachments")
                .file(
                    new MockMultipartFile(
                        "file",
                        "quick-shot-scoped-report.jpg",
                        "image/jpeg",
                        tinyJpeg()))
                .param("fileKind", "IMAGE")
                .header("Authorization", "Bearer " + scopedReportToken))
        .andExpect(status().isForbidden());
    assertThat(countAttachmentsByOriginalName("quick-shot-scoped-report.jpg")).isZero();

    String ownerUploadResponse =
        mockMvc
            .perform(
                multipart(
                        "/api/pingan/three-checks/quick-shot/records/"
                            + ownerRecord.path("id").asText()
                            + "/attachments")
                    .file(
                        new MockMultipartFile(
                            "file",
                            "quick-shot-scope-owner.jpg",
                            "image/jpeg",
                            tinyJpeg()))
                    .param("fileKind", "IMAGE")
                    .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode attachment = objectMapper.readTree(ownerUploadResponse).path("data");

    deleteJson(
        "/api/pingan/three-checks/quick-shot/records/"
            + ownerRecord.path("id").asText()
            + "/attachments/"
            + attachment.path("id").asText(),
        ownerToken);
  }

  @Test
  void quickShotAttachmentCreatorCanManageOwnCreatedRecord()
      throws Exception {
    long creatorUserId = 9517L;
    createPermissionFixtureUser(
        creatorUserId,
        9517L,
        "quick_shot_attachment_creator",
        "随手拍附件创建人",
        "QUICK_SHOT_ATTACHMENT_CREATOR",
        "PINGAN_HAZARD_QUICK_SHOT_VIEW",
        "PINGAN_HAZARD_QUICK_SHOT_REPORT");
    jdbcTemplate.update(
        """
        insert into three_check_record (
          module_key, record_no, company_id, department_id, team_id, owner_user_id,
          business_date, status, payload_json, image_check_status, video_check_status,
          reminder_count, version, created_by, updated_by, source_channel,
          client_request_id, last_synced_at, created_at, updated_at, deleted
        ) values (
          'quick-shot', 'QS-CREATOR-ATTACHMENT-RBAC', ?, ?, ?, 9518,
          cast('2026-11-16' as date), 'PENDING_REVIEW',
          '{"statusLabel":"待审核","hazardDescription":"creator quick shot"}',
          '未上传', '未上传', 0, 0, ?, ?, 'PC', 'creator-attachment-rbac',
          current_timestamp, current_timestamp, current_timestamp, 0
        )
        """,
        SOURCE_COMPANY_ID,
        SOURCE_DEPARTMENT_ID,
        SOURCE_TEAM_ID,
        creatorUserId,
        creatorUserId);
    Long recordId =
        jdbcTemplate.queryForObject(
            "select id from three_check_record where record_no = 'QS-CREATOR-ATTACHMENT-RBAC'",
            Long.class);

    String creatorToken = login("quick_shot_attachment_creator", "123456");
    String uploadResponse =
        mockMvc
            .perform(
                multipart(
                        "/api/pingan/three-checks/quick-shot/records/"
                            + recordId
                            + "/attachments")
                    .file(
                        new MockMultipartFile(
                            "file",
                            "quick-shot-creator-report.jpg",
                            "image/jpeg",
                            tinyJpeg()))
                    .param("fileKind", "IMAGE")
                    .header("Authorization", "Bearer " + creatorToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode attachment = objectMapper.readTree(uploadResponse).path("data");

    deleteJson(
        "/api/pingan/three-checks/quick-shot/records/"
            + recordId
            + "/attachments/"
            + attachment.path("id").asText(),
        creatorToken);
  }

  @Test
  void hazardInspectionListDetailAndMiniListRequireHazardViewPermissions() throws Exception {
    long threeCheckOnlyUserId = 9511L;
    long hazardViewerUserId = 9512L;
    String recordNo = "TCR-HAZARD-VIEW-RBAC";
    jdbcTemplate.update("delete from three_check_record where record_no = ?", recordNo);
    long recordId =
        insertRecordForEnterpriseQueryAndReturnId(
            recordNo,
            "safety-check",
            hazardViewerUserId,
            SOURCE_TEAM_ID,
            "2026-11-05",
            "OPENED");
    createPermissionFixtureUser(
        threeCheckOnlyUserId,
        9511L,
        "hazard_list_three_check_only",
        "隐患列表三查查看用户",
        "HAZARD_LIST_THREE_CHECK_ONLY",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW");
    createPermissionFixtureUser(
        hazardViewerUserId,
        9512L,
        "hazard_list_viewer",
        "隐患列表查看用户",
        "HAZARD_LIST_VIEWER",
        "PINGAN_HAZARD_ENTRY",
        "PINGAN_HAZARD_SAFETY_CHECK_VIEW");

    String threeCheckOnlyToken = login("hazard_list_three_check_only", "123456");
    mockMvc
        .perform(
            get("/api/pingan/three-checks/safety-check/records?status=all")
                .header("Authorization", "Bearer " + threeCheckOnlyToken))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            get("/api/pingan/three-checks/safety-check/records/" + recordId)
                .header("Authorization", "Bearer " + threeCheckOnlyToken))
        .andExpect(status().isForbidden());

    String hazardViewerToken = login("hazard_list_viewer", "123456");
    JsonNode list =
        getJson(
                "/api/pingan/three-checks/safety-check/records?status=all&dateStart=2026-11-05&dateEnd=2026-11-05",
                hazardViewerToken)
            .path("data");
    assertThat(list.path("total").asInt()).isEqualTo(1);
    assertThat(list.path("items").get(0).path("id").asLong()).isEqualTo(recordId);

    JsonNode detail =
        getJson("/api/pingan/three-checks/safety-check/records/" + recordId, hazardViewerToken)
            .path("data");
    assertThat(detail.path("id").asLong()).isEqualTo(recordId);

    JsonNode miniList =
        getJson(
                "/api/mini/pingan/three-checks/safety-check/records?status=all&dateStart=2026-11-05&dateEnd=2026-11-05",
                hazardViewerToken)
            .path("data");
    assertThat(miniList.path("total").asInt()).isEqualTo(1);
    assertThat(miniList.path("items").get(0).path("id").asLong()).isEqualTo(recordId);
  }

  @Test
  void hazardInspectionWorkflowRequiresHazardViewPermissions() throws Exception {
    long threeCheckOnlyUserId = 9513L;
    long hazardViewerUserId = 9514L;
    String recordNo = "TCR-HAZARD-WORKFLOW-RBAC";
    jdbcTemplate.update(
        "delete from biz_status_log where biz_id in (select id from three_check_record where record_no = ?)",
        recordNo);
    jdbcTemplate.update("delete from three_check_record where record_no = ?", recordNo);
    long recordId =
        insertRecordForEnterpriseQueryAndReturnId(
            recordNo,
            "quick-shot",
            hazardViewerUserId,
            SOURCE_TEAM_ID,
            "2026-11-07",
            "PENDING_REVIEW");
    createPermissionFixtureUser(
        threeCheckOnlyUserId,
        9513L,
        "hazard_workflow_three_check_only",
        "隐患流程三查查看用户",
        "HAZARD_WORKFLOW_THREE_CHECK_ONLY",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW");
    createPermissionFixtureUser(
        hazardViewerUserId,
        9514L,
        "hazard_workflow_viewer",
        "隐患流程查看用户",
        "HAZARD_WORKFLOW_VIEWER",
        "PINGAN_HAZARD_ENTRY",
        "PINGAN_HAZARD_QUICK_SHOT_VIEW");

    String threeCheckOnlyToken = login("hazard_workflow_three_check_only", "123456");
    mockMvc
        .perform(
            get("/api/pingan/three-checks/quick-shot/records/" + recordId + "/workflow")
                .header("Authorization", "Bearer " + threeCheckOnlyToken))
        .andExpect(status().isForbidden());

    JsonNode workflow =
        getJson(
                "/api/pingan/three-checks/quick-shot/records/" + recordId + "/workflow",
                login("hazard_workflow_viewer", "123456"))
            .path("data");
    assertThat(workflow.path("documentFlow")).isNotNull();
    assertThat(workflow.path("changeHistory")).isNotNull();
  }

  @Test
  void rejectsQuickShotHazardWhenReviewFindsItInvalidAndStoresReviewPayload() throws Exception {
    String token = login("admin", "123456");
    JsonNode created =
        postJson(
                "/api/pingan/three-checks/quick-shot/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2026-05-19",
                    "payload",
                    Map.of("hazardDescription", "not a hazard after review")))
            .path("data");

    JsonNode rejected =
        postJson(
                "/api/pingan/three-checks/quick-shot/records/"
                    + created.path("id").asText()
                    + "/workflow-actions",
                token,
                Map.of(
                    "action",
                    "REJECT",
                    "version",
                    created.path("version").asInt(),
                    "payload",
                    Map.of(
                        "rejectReason", "现场复核隐患不成立",
                        "reviewRemark", "部门、班组和描述已复核")))
            .path("data");

    assertThat(rejected.path("status").asText()).isEqualTo("REJECTED");
    assertThat(rejected.path("statusLabel").asText()).isEqualTo("已驳回");
    assertThat(rejected.path("payload").path("rejectReason").asText()).isEqualTo("现场复核隐患不成立");
    assertThat(rejected.path("payload").path("reviewRemark").asText()).isEqualTo("部门、班组和描述已复核");
    assertThat(rejected.path("payload").path("rejectedBy").asText()).isEqualTo("系统管理员");
    assertThat(rejected.path("payload").path("rejectedAt").asText()).isNotBlank();
  }

  @Test
  void rejectsInvalidAndUnauthorizedQuickShotWorkflowActions() throws Exception {
    String adminToken = login("admin", "123456");
    JsonNode created =
        createGenericRecord(
            adminToken,
            "quick-shot",
            "待审核",
            Map.of("statusLabel", "待审核", "hazardDescription", "quick shot invalid action"));
    String id = created.path("id").asText();

    mockMvc
        .perform(
            post("/api/pingan/three-checks/quick-shot/records/" + id + "/workflow-actions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("action", "ISSUE_RECTIFICATION", "version", 0))))
        .andExpect(status().isBadRequest());

    String teamLeaderToken = login("HB_MONITOR", "123456");
    mockMvc
        .perform(
            post("/api/pingan/three-checks/quick-shot/records/" + id + "/workflow-actions")
                .header("Authorization", "Bearer " + teamLeaderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(Map.of("action", "APPROVE", "version", 0))))
        .andExpect(status().isForbidden());
  }

  @Test
  void mapsLegacyQuickShotStatusesToSixStepLabelsWithoutMigratingRows() throws Exception {
    String token = login("admin", "123456");
    jdbcTemplate.update(
        """
        insert into three_check_record (
          module_key, record_no, company_id, department_id, team_id, owner_user_id,
          business_date, status, payload_json, image_check_status, video_check_status,
          reminder_count, version, created_by, updated_by, source_channel,
          client_request_id, last_synced_at, created_at, updated_at, deleted
        ) values (
          'quick-shot', 'QS-LEGACY-ARCHIVED', ?, ?, ?, 2, cast('2026-05-19' as date), 'ARCHIVED',
          '{"statusLabel":"不通过","hazardDescription":"legacy quick shot"}',
          '未上传', '未上传', 0, 0, 1, 1, 'PC', 'legacy-quick-shot',
          current_timestamp, current_timestamp, current_timestamp, 0
        )
        """,
        SOURCE_COMPANY_ID,
        SOURCE_DEPARTMENT_ID,
        SOURCE_TEAM_ID);

    JsonNode list =
        getJson("/api/pingan/three-checks/quick-shot/records?status=all", token).path("data");
    JsonNode legacy = objectMapper.createObjectNode();
    for (JsonNode item : list.path("items")) {
      if ("QS-LEGACY-ARCHIVED".equals(item.path("recordNo").asText())) {
        legacy = item;
      }
    }
    assertThat(legacy.path("statusLabel").asText()).isEqualTo("已验收");

    JsonNode detail =
        getJson(
                "/api/pingan/three-checks/quick-shot/records/"
                    + jdbcTemplate.queryForObject(
                        "select id from three_check_record where record_no = 'QS-LEGACY-ARCHIVED'",
                        Long.class),
                token)
            .path("data");
    assertThat(detail.path("status").asText()).isEqualTo("ARCHIVED");
    assertThat(detail.path("statusLabel").asText()).isEqualTo("已验收");
    assertThat(detail.path("payload").path("statusLabel").asText()).isEqualTo("已验收");
  }

  @Test
  void rejectsNonHazardModuleDocumentFlowPrefix() throws Exception {
    String token = login("admin", "123456");
    JsonNode created =
        createGenericRecord(
            token,
            "pre-shift-inspection",
            "待检查",
            Map.of("statusLabel", "待检查", "inspectionContent", "generic module"));

    JsonNode response =
        objectMapper.readTree(
            mockMvc
                .perform(
                    get(
                            "/api/pingan/hazard-inspection/pre-shift-inspection/records/"
                                + created.path("id").asText()
                                + "/document-flow")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

    assertThat(response.path("message").asText()).isEqualTo("隐患排查模块不支持");
  }

  @Test
  void keepsDocumentFlowDataScope() throws Exception {
    jdbcTemplate.update(
        "insert into sys_role (id, role_code, role_name, data_scope) values (9201, 'SELF_HAZARD_FLOW_TEST', '隐患排查单据流本人测试', 'SELF')");
    jdbcTemplate.update(
        "insert into sys_role_menu (role_id, menu_id) select 9201, id from sys_menu where permission_code = 'PINGAN_HAZARD_SAFETY_CHECK_VIEW'");
    jdbcTemplate.update(
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (9201, 'self_hazard_flow', '{noop}123456', '单据流本人范围用户', ?, 'ACTIVE', 0)",
        SOURCE_TEAM_ID);
    jdbcTemplate.update("insert into sys_user_role (user_id, role_id) values (9201, 9201)");

    String adminToken = login("admin", "123456");
    JsonNode created =
        createGenericRecord(
            adminToken,
            "safety-check",
            "待检查",
            Map.of("statusLabel", "待检查", "hazardDescription", "other owner hazard"));
    String selfToken = login("self_hazard_flow", "123456");

    mockMvc
        .perform(
            get(
                    "/api/pingan/hazard-inspection/safety-check/records/"
                        + created.path("id").asText()
                        + "/document-flow")
                .header("Authorization", "Bearer " + selfToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void returnsHazardInspectionChangeHistoryWithFieldAndAttachmentChanges() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        createGenericRecord(
            token,
            "safety-check",
            "待检查",
            Map.of("statusLabel", "待检查", "hazardDescription", "old hazard"));
    String id = created.path("id").asText();

    putJson(
        "/api/pingan/three-checks/safety-check/records/" + id,
        token,
        Map.of(
            "companyId",
            SOURCE_COMPANY_ID,
            "departmentId",
            SOURCE_DEPARTMENT_ID,
            "teamId",
            SOURCE_TEAM_ID,
            "ownerUserId",
            2,
            "businessDate",
            "2026-05-19",
            "status",
            "待检查",
            "payload",
            Map.of(
                "statusLabel",
                "待检查",
                "hazardDescription",
                "updated hazard",
                "reporter",
                "张三"),
            "version",
            0));

    MockMultipartFile image =
        new MockMultipartFile(
            "file",
            "hazard-change.jpg",
            "image/jpeg",
            tinyJpeg());
    mockMvc
        .perform(
            multipart("/api/pingan/three-checks/safety-check/records/" + id + "/attachments")
                .file(image)
                .param("fileKind", "IMAGE")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    JsonNode history =
        getJson(
                "/api/pingan/hazard-inspection/safety-check/records/" + id + "/change-history",
                token)
            .path("data");

    assertThat(history.path("record").path("id").asText()).isEqualTo(id);
    JsonNode items = history.path("items");
    assertThat(actionsIn(items)).contains("CREATE", "UPDATE", "ATTACHMENT");
    JsonNode hazardChange = firstHistoryByField(items, "hazardDescription");
    assertThat(hazardChange.path("fieldLabel").asText()).isEqualTo("隐患描述");
    assertThat(hazardChange.path("beforeValue").asText()).isEqualTo("old hazard");
    assertThat(hazardChange.path("afterValue").asText()).isEqualTo("updated hazard");
    assertThat(hazardChange.path("operatorName").asText()).isEqualTo("系统管理员");
    assertThat(hazardChange.path("createdAt").asText())
        .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    JsonNode reporterChange = firstHistoryByField(items, "reporter");
    assertThat(reporterChange.path("fieldLabel").asText()).isEqualTo("上报人");
    assertThat(reporterChange.path("afterValue").asText()).isEqualTo("张三");
    JsonNode attachmentChange = firstLogByAction(items, "ATTACHMENT");
    assertThat(attachmentChange.path("fieldLabel").asText()).isEqualTo("附件");
    assertThat(attachmentChange.path("afterValue").asText()).contains("hazard-change.jpg");
    assertThat(attachmentChange.path("valueType").asText()).isEqualTo("IMAGE");
  }

  @Test
  void rejectsNonHazardModuleChangeHistoryPrefix() throws Exception {
    String token = login("admin", "123456");
    JsonNode created =
        createGenericRecord(
            token,
            "pre-shift-inspection",
            "待检查",
            Map.of("statusLabel", "待检查", "inspectionContent", "generic module"));

    JsonNode response =
        objectMapper.readTree(
            mockMvc
                .perform(
                    get(
                            "/api/pingan/hazard-inspection/pre-shift-inspection/records/"
                                + created.path("id").asText()
                                + "/change-history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

    assertThat(response.path("message").asText()).isEqualTo("隐患排查模块不支持");
  }

  @Test
  void keepsChangeHistoryDataScope() throws Exception {
    jdbcTemplate.update(
        "insert into sys_role (id, role_code, role_name, data_scope) values (9401, 'SELF_HAZARD_HISTORY_TEST', '隐患排查变更历史本人测试', 'SELF')");
    jdbcTemplate.update(
        "insert into sys_role_menu (role_id, menu_id) select 9401, id from sys_menu where permission_code = 'PINGAN_HAZARD_SAFETY_CHECK_VIEW'");
    jdbcTemplate.update(
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (9401, 'self_hazard_history', '{noop}123456', '变更历史本人范围用户', ?, 'ACTIVE', 0)",
        SOURCE_TEAM_ID);
    jdbcTemplate.update("insert into sys_user_role (user_id, role_id) values (9401, 9401)");

    String adminToken = login("admin", "123456");
    JsonNode created =
        createGenericRecord(
            adminToken,
            "safety-check",
            "待检查",
            Map.of("statusLabel", "待检查", "hazardDescription", "other owner hazard"));
    String selfToken = login("self_hazard_history", "123456");

    mockMvc
        .perform(
            get(
                    "/api/pingan/hazard-inspection/safety-check/records/"
                        + created.path("id").asText()
                        + "/change-history")
                .header("Authorization", "Bearer " + selfToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void withdrawsTeamDispatchBackToInactiveAndKeepsDetailEditableIds() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-05-18",
                    "payload",
                        Map.of(
                            "teamTask", "吊装作业派班",
                            "dispatchStatus", "未生效",
                            "statusLabel", "未生效")))
            .path("data");
    String id = created.path("id").asText();

    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records/" + id + "/submit",
                token,
                Map.of())
            .path("data");
    assertThat(submitted.path("status").asText()).isEqualTo("OPENED");
    assertThat(submitted.path("statusLabel").asText()).isEqualTo("已生效");
    assertThat(submitted.path("payload").path("statusLabel").asText()).isEqualTo("已生效");
    assertThat(submitted.path("payload").path("dispatchStatus").asText()).isEqualTo("已生效");
    assertThat(submitted.path("canSubmit").asBoolean()).isFalse();
    assertThat(submitted.path("canWithdraw").asBoolean()).isTrue();

    JsonNode withdrawn =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records/" + id + "/withdraw",
                token,
                Map.of("reason", "派班内容需要调整"))
            .path("data");
    assertThat(withdrawn.path("status").asText()).isEqualTo("WITHDRAWN");
    assertThat(withdrawn.path("statusLabel").asText()).isEqualTo("未生效");
    assertThat(withdrawn.path("payload").path("statusLabel").asText()).isEqualTo("未生效");
    assertThat(withdrawn.path("payload").path("dispatchStatus").asText()).isEqualTo("未生效");
    assertThat(withdrawn.path("canSubmit").asBoolean()).isTrue();
    assertThat(withdrawn.path("canWithdraw").asBoolean()).isFalse();
    assertThat(withdrawn.path("companyId").asLong()).isEqualTo(SOURCE_COMPANY_ID);
    assertThat(withdrawn.path("departmentId").asLong()).isEqualTo(SOURCE_DEPARTMENT_ID);
    assertThat(withdrawn.path("teamId").asLong()).isEqualTo(SOURCE_TEAM_ID);
    assertThat(withdrawn.path("ownerUserId").asLong()).isEqualTo(2L);
  }

  @Test
  void curtainWallTeamDispatchSubmitAndWithdrawSyncDispatchStatusAndControls() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/curtain-wall-team-dispatch/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-05-18",
                    "payload",
                        Map.of(
                            "teamTask", "幕墙吊装作业派班",
                            "dispatchStatus", "未生效",
                            "statusLabel", "未生效")))
            .path("data");
    assertThat(created.path("statusLabel").asText()).isEqualTo("未生效");
    assertThat(created.path("payload").path("dispatchStatus").asText()).isEqualTo("未生效");
    assertThat(created.path("canSubmit").asBoolean()).isTrue();
    assertThat(created.path("canWithdraw").asBoolean()).isFalse();

    String id = created.path("id").asText();
    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/curtain-wall-team-dispatch/records/" + id + "/submit",
                token,
                Map.of())
            .path("data");
    assertThat(submitted.path("statusLabel").asText()).isEqualTo("已生效");
    assertThat(submitted.path("payload").path("dispatchStatus").asText()).isEqualTo("已生效");
    assertThat(submitted.path("canSubmit").asBoolean()).isFalse();
    assertThat(submitted.path("canWithdraw").asBoolean()).isTrue();

    JsonNode withdrawn =
        postJson(
                "/api/pingan/three-checks/curtain-wall-team-dispatch/records/" + id + "/withdraw",
                token,
                Map.of("reason", "幕墙派班撤回"))
            .path("data");
    assertThat(withdrawn.path("statusLabel").asText()).isEqualTo("未生效");
    assertThat(withdrawn.path("payload").path("dispatchStatus").asText()).isEqualTo("未生效");
    assertThat(withdrawn.path("canSubmit").asBoolean()).isTrue();
    assertThat(withdrawn.path("canWithdraw").asBoolean()).isFalse();
  }

  @Test
  void dispatchInactiveStatusFilterIncludesWithdrawnRecords() throws Exception {
    String token = login("admin", "123456");

    JsonNode teamDispatch =
        postJson(
                "/api/pingan/three-checks/team-dispatch/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-05-19",
                    "payload",
                        Map.of(
                            "teamTask", "未生效筛选测试",
                            "dispatchStatus", "未生效",
                            "statusLabel", "未生效")))
            .path("data");
    String teamDispatchId = teamDispatch.path("id").asText();
    postJson(
            "/api/pingan/three-checks/team-dispatch/records/" + teamDispatchId + "/submit",
            token,
            Map.of());
    postJson(
            "/api/pingan/three-checks/team-dispatch/records/" + teamDispatchId + "/withdraw",
            token,
            Map.of("reason", "测试未生效筛选"))
        .path("data");

    JsonNode curtainWallDispatch =
        postJson(
                "/api/pingan/three-checks/curtain-wall-team-dispatch/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-05-19",
                    "payload",
                        Map.of(
                            "teamTask", "幕墙未生效筛选测试",
                            "dispatchStatus", "未生效",
                            "statusLabel", "未生效")))
            .path("data");
    String curtainWallDispatchId = curtainWallDispatch.path("id").asText();
    postJson(
            "/api/pingan/three-checks/curtain-wall-team-dispatch/records/"
                + curtainWallDispatchId
                + "/submit",
            token,
            Map.of());
    postJson(
            "/api/pingan/three-checks/curtain-wall-team-dispatch/records/"
                + curtainWallDispatchId
                + "/withdraw",
            token,
            Map.of("reason", "测试幕墙未生效筛选"));

    JsonNode teamDispatchList =
        getJson(
                "/api/pingan/three-checks/team-dispatch/records"
                    + "?dateStart=2026-05-19&dateEnd=2026-05-19&status=DRAFT",
                token)
            .path("data")
            .path("items");
    JsonNode curtainWallDispatchList =
        getJson(
                "/api/pingan/three-checks/curtain-wall-team-dispatch/records"
                    + "?dateStart=2026-05-19&dateEnd=2026-05-19&status=DRAFT",
                token)
            .path("data")
            .path("items");

    assertThat(teamDispatchList)
        .anySatisfy(
            row -> {
              assertThat(row.path("id").asText()).isEqualTo(teamDispatchId);
              assertThat(row.path("status").asText()).isEqualTo("WITHDRAWN");
              assertThat(row.path("statusLabel").asText()).isEqualTo("未生效");
            });
    assertThat(curtainWallDispatchList)
        .anySatisfy(
            row -> {
              assertThat(row.path("id").asText()).isEqualTo(curtainWallDispatchId);
              assertThat(row.path("status").asText()).isEqualTo("WITHDRAWN");
              assertThat(row.path("statusLabel").asText()).isEqualTo("未生效");
            });
  }

  @Test
  void keySitesSubmitCreatesOneLinkedRectificationOrderAndExposesMiniWorkflow() throws Exception {
    String token = login("admin", "123456");
    Map<String, Object> request = new HashMap<>();
    request.put("companyId", SOURCE_COMPANY_ID);
    request.put("departmentId", SOURCE_DEPARTMENT_ID);
    request.put("teamId", SOURCE_TEAM_ID);
    request.put("ownerUserId", 2);
    request.put("businessDate", "2026-05-24");
    request.put("sourceRecordId", "wx-key-sites-hazard-001");
    request.put("clientRequestId", "wx-key-sites-hazard-req-001");
    request.put(
        "payload",
        Map.of(
            "siteType", "动火作业区",
            "inspectionDepartment", "安全管理部",
            "responsibleDepartment", "工程管理部",
            "responsiblePerson", "湖贝班长",
            "checkItems",
                List.of(
                    Map.of(
                        "lineId", "key-sites-line-1",
                        "riskType", "火灾",
                        "checkItem", "检查动火隔离及灭火器材",
                        "checkResult", "有隐患",
                        "hazardDescription", "灭火器压力不足",
                        "beforePhoto", "/uploads/key-sites-before.jpg")),
            "statusLabel", "待检查"));

    JsonNode created =
        postJson("/api/mini/pingan/three-checks/key-sites/records", token, request).path("data");
    String id = created.path("id").asText();
    JsonNode submitted =
        postJson(
                "/api/mini/pingan/three-checks/key-sites/records/" + id + "/submit",
                token,
                Map.of())
            .path("data");

    assertThat(submitted.path("status").asText()).isEqualTo("OPENED");
    assertThat(submitted.path("statusLabel").asText()).isEqualTo("已检查");
    assertThat(submitted.path("payload").path("checkItems").get(0).path("rectificationOrderId").asText())
        .isNotBlank();
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from hazard_rectification_order where source_type = 'THREE_CHECK' and source_module_key = 'key-sites' and source_record_id = ? and deleted = 0",
                Integer.class,
                Long.parseLong(id)))
        .isEqualTo(1);

    postJson(
        "/api/mini/pingan/three-checks/key-sites/records/" + id + "/rectification-order",
        token,
        Map.of());
    postJson(
        "/api/mini/pingan/three-checks/key-sites/records/" + id + "/rectification-order",
        token,
        Map.of());
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from hazard_rectification_order where source_type = 'THREE_CHECK' and source_module_key = 'key-sites' and source_record_id = ? and deleted = 0",
                Integer.class,
                Long.parseLong(id)))
        .isEqualTo(1);

    mockMvc
        .perform(
            post("/api/mini/pingan/three-checks/key-sites/records/" + id + "/withdraw")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", "不应撤回检查事实"))))
        .andExpect(status().isBadRequest());

    JsonNode workflow =
        getJson("/api/mini/pingan/three-checks/key-sites/records/" + id + "/workflow", token)
            .path("data");
    assertThat(workflow.path("documentFlow")).isNotEmpty();
  }

  @Test
  void keySitesRejectsHazardWithoutDescription() throws Exception {
    String token = login("admin", "123456");
    Map<String, Object> request = new HashMap<>(
        miniKeySitePayload(
            2L,
            SOURCE_TEAM_ID,
            "2026-05-25",
            "有限空间",
            "wx-key-sites-invalid-001",
            "wx-key-sites-invalid-req-001"));
    request.put(
        "payload",
        Map.of(
            "siteType", "有限空间",
            "checkItems",
                List.of(
                    Map.of(
                        "riskType", "中毒窒息",
                        "checkItem", "检查通风及气体检测",
                        "checkResult", "有隐患")),
            "statusLabel", "待检查"));
    JsonNode created =
        postJson("/api/mini/pingan/three-checks/key-sites/records", token, request).path("data");

    mockMvc
        .perform(
            post(
                    "/api/mini/pingan/three-checks/key-sites/records/"
                        + created.path("id").asText()
                        + "/submit")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void miniProgramGenericRecordsSharePcDataAndSyncControls() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/mini/pingan/three-checks/key-sites/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-05-16",
                    "payload",
                        Map.of(
                            "siteType", "吊装作业区",
                            "responsiblePerson", "湖贝班长",
                            "acceptancePerson", "幕墙安全员",
                            "statusLabel", "待检查"),
                    "sourceChannel", "PC",
                    "sourceRecordId", "wx-key-site-001",
                    "clientRequestId", "wx-req-001"))
            .path("data");

    String id = created.path("id").asText();
    assertThat(created.path("sourceChannel").asText()).isEqualTo("WECHAT_MINI_PROGRAM");
    assertThat(created.path("sourceRecordId").asText()).isEqualTo("wx-key-site-001");
    assertThat(created.path("clientRequestId").asText()).isEqualTo("wx-req-001");
    assertThat(created.path("version").asInt()).isEqualTo(0);

    JsonNode pcList =
        getJson(
                "/api/pingan/three-checks/key-sites/records?dateStart=2026-05-16&dateEnd=2026-05-16",
                token)
            .path("data");
    assertThat(idsIn(pcList.path("items"))).contains(id);

    JsonNode sourceRecordUpsert =
        postJson(
                "/api/mini/pingan/three-checks/key-sites/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-05-16",
                    "payload",
                        Map.of(
                            "siteType", "临边防护区",
                            "responsiblePerson", "湖贝班长",
                            "acceptancePerson", "幕墙安全员",
                            "statusLabel", "待整改"),
                    "sourceRecordId", "wx-key-site-001",
                    "clientRequestId", "wx-req-002",
                    "version", 0))
            .path("data");
    assertThat(sourceRecordUpsert.path("id").asText()).isEqualTo(id);
    assertThat(sourceRecordUpsert.path("payload").path("siteType").asText()).isEqualTo("临边防护区");
    assertThat(sourceRecordUpsert.path("version").asInt()).isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from three_check_record where module_key = 'key-sites' and source_channel = 'WECHAT_MINI_PROGRAM' and source_record_id = 'wx-key-site-001'",
                Integer.class))
        .isEqualTo(1);

    JsonNode firstRequest =
        postJson(
                "/api/mini/pingan/three-checks/key-sites/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-05-17",
                    "payload", Map.of("siteType", "危险品库", "statusLabel", "待检查"),
                    "sourceRecordId", "wx-key-site-002",
                    "clientRequestId", "wx-req-duplicate"))
            .path("data");
    JsonNode repeatedRequest =
        postJson(
                "/api/mini/pingan/three-checks/key-sites/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-05-17",
                    "payload", Map.of("siteType", "这次重复请求不应覆盖第一次", "statusLabel", "待整改"),
                    "sourceRecordId", "wx-key-site-003",
                    "clientRequestId", "wx-req-duplicate"))
            .path("data");
    assertThat(repeatedRequest.path("id").asText()).isEqualTo(firstRequest.path("id").asText());
    assertThat(repeatedRequest.path("payload").path("siteType").asText()).isEqualTo("危险品库");

    mockMvc
        .perform(
            put("/api/pingan/three-checks/key-sites/records/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "companyId", SOURCE_COMPANY_ID,
                            "departmentId", SOURCE_DEPARTMENT_ID,
                            "teamId", SOURCE_TEAM_ID,
                            "ownerUserId", 2,
                            "businessDate", "2026-05-16",
                            "payload", Map.of("siteType", "PC 使用过期版本更新"),
                            "version", 0))))
        .andExpect(status().isConflict());
  }

  @Test
  void miniProgramPreShiftMeetingRecordsSyncToPcGenericWorkbench() throws Exception {
    String token = login("admin", "123456");
    String today = futureBusinessDate();

    JsonNode created =
        postJson(
                "/api/mini/pingan/three-checks/pre-shift-meeting/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", today,
                    "payload",
                        Map.of(
                            "meetingContent", "小程序同步班前会内容",
                            "attendees", List.of("湖贝班长", "幕墙安全员"),
                            "attendeesText", "湖贝班长、幕墙安全员",
                            "statusLabel", "待开会议"),
                    "sourceChannel", "PC",
                    "sourceRecordId", "wx-pre-shift-meeting-001",
                    "clientRequestId", "wx-pre-shift-meeting-req-001"))
            .path("data");

    String id = created.path("id").asText();
    assertThat(created.path("moduleKey").asText()).isEqualTo("pre-shift-meeting");
    assertThat(created.path("sourceChannel").asText()).isEqualTo("WECHAT_MINI_PROGRAM");
    assertThat(created.path("payload").path("meetingContent").asText()).isEqualTo("小程序同步班前会内容");
    assertThat(created.path("payload").path("attendees")).hasSize(2);

    JsonNode pcList =
        getJson(
                "/api/pingan/three-checks/pre-shift-meeting/records"
                    + "?dateStart=" + today + "&dateEnd=" + today + "&sourceChannel=WECHAT_MINI_PROGRAM",
                token)
            .path("data");
    assertThat(idsIn(pcList.path("items"))).contains(id);
    assertThat(pcList.path("items").get(0).path("payload").path("attendeesText").asText())
        .isEqualTo("湖贝班长、幕墙安全员");

    JsonNode upserted =
        postJson(
                "/api/mini/pingan/three-checks/pre-shift-meeting/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", today,
                    "payload",
                        Map.of(
                            "meetingContent", "小程序更新后的班前会内容",
                            "attendees", List.of("湖贝班长"),
                            "attendeesText", "湖贝班长",
                            "statusLabel", "待开会议"),
                    "sourceRecordId", "wx-pre-shift-meeting-001",
                    "clientRequestId", "wx-pre-shift-meeting-req-002",
                    "version", 0))
            .path("data");
    assertThat(upserted.path("id").asText()).isEqualTo(id);
    assertThat(upserted.path("payload").path("meetingContent").asText()).isEqualTo("小程序更新后的班前会内容");
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from three_check_record where module_key = 'pre-shift-meeting' and source_channel = 'WECHAT_MINI_PROGRAM' and source_record_id = 'wx-pre-shift-meeting-001'",
                Integer.class))
        .isEqualTo(1);

    uploadThreeCheckRecordAttachment("pre-shift-meeting", id, token, "IMAGE", "meeting.jpg", "image/jpeg");
    uploadThreeCheckRecordAttachment("pre-shift-meeting", id, token, "VIDEO", "meeting.mp4", "video/mp4");
    JsonNode submitted =
        postJson(
                "/api/pingan/three-checks/pre-shift-meeting/records/" + id + "/submit",
                token,
                Map.of())
            .path("data");
    assertThat(submitted.path("status").asText()).isEqualTo("OPENED");
    assertThat(submitted.path("statusLabel").asText()).isEqualTo("已开会议");

    JsonNode statistics =
        getJson(
                "/api/pingan/three-checks/pre-shift-meeting/records/statistics"
                    + "?dateStart=" + today + "&dateEnd=" + today + "&sourceChannel=WECHAT_MINI_PROGRAM",
                token)
            .path("data");
    assertThat(statistics.path("total").asInt()).isEqualTo(1);
    assertThat(statistics.path("statusCounts").path("OPENED").asInt()).isEqualTo(1);
  }

  @Test
  void miniProgramGenericRecordsUseSharedExecutePermissionAndDataScope() throws Exception {
    long viewOnlyUserId = 9821L;
    long scopedExecuteUserId = 9822L;
    createPermissionFixtureUser(
        viewOnlyUserId,
        9821L,
        "mini_three_check_view_only",
        "小程序三查只读用户",
        "MINI_THREE_CHECK_VIEW_ONLY",
        "PINGAN_KEY_SITES_VIEW");
    createPermissionFixtureUser(
        scopedExecuteUserId,
        9822L,
        "mini_three_check_scoped_execute",
        "小程序三查范围执行用户",
        "MINI_THREE_CHECK_SCOPED_EXECUTE",
        "PINGAN_KEY_SITES_VIEW",
        "PINGAN_KEY_SITES_CREATE",
        "PINGAN_KEY_SITES_SUBMIT");
    jdbcTemplate.update(
        "update sys_role set data_scope = 'ORG_AND_CHILDREN' where role_code = 'MINI_THREE_CHECK_SCOPED_EXECUTE'");

    String viewOnlyToken = login("mini_three_check_view_only", "123456");
    Map<String, Object> viewOnlyCreatePayload =
        miniKeySitePayload(
            viewOnlyUserId,
            SOURCE_TEAM_ID,
            "2026-05-20",
            "小程序权限测试",
            "wx-rbac-view-only",
            "wx-rbac-view-only-req");

    mockMvc
        .perform(
            post("/api/mini/pingan/three-checks/key-sites/records")
                .header("Authorization", "Bearer " + viewOnlyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(viewOnlyCreatePayload)))
        .andExpect(status().isForbidden());

    JsonNode ownedDraft =
        postJson(
                "/api/mini/pingan/three-checks/key-sites/records",
                login("admin", "123456"),
                miniKeySitePayload(
                    viewOnlyUserId,
                    SOURCE_TEAM_ID,
                    "2026-05-21",
                    "小程序提交权限测试",
                    "wx-rbac-owned-draft",
                    "wx-rbac-owned-draft-req"))
            .path("data");
    mockMvc
        .perform(
            post(
                    "/api/mini/pingan/three-checks/key-sites/records/"
                        + ownedDraft.path("id").asText()
                        + "/submit")
                .header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isForbidden());

    String scopedExecuteToken = login("mini_three_check_scoped_execute", "123456");
    mockMvc
        .perform(
            post("/api/mini/pingan/three-checks/key-sites/records")
                .header("Authorization", "Bearer " + scopedExecuteToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        miniKeySitePayload(
                            scopedExecuteUserId,
                            1011002L,
                            "2026-05-22",
                            "小程序越权创建",
                            "wx-rbac-out-of-scope-create",
                            "wx-rbac-out-of-scope-create-req"))))
        .andExpect(status().isForbidden());
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from three_check_record where source_record_id = 'wx-rbac-out-of-scope-create'",
                Integer.class))
        .isZero();

    JsonNode outOfScopeDraft =
        postJson(
                "/api/mini/pingan/three-checks/key-sites/records",
                login("admin", "123456"),
                miniKeySitePayload(
                    scopedExecuteUserId,
                    1011002L,
                    "2026-05-23",
                    "小程序越权提交",
                    "wx-rbac-out-of-scope-submit",
                    "wx-rbac-out-of-scope-submit-req"))
            .path("data");
    mockMvc
        .perform(
            post(
                    "/api/mini/pingan/three-checks/key-sites/records/"
                        + outOfScopeDraft.path("id").asText()
                        + "/submit")
                .header("Authorization", "Bearer " + scopedExecuteToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsUnsupportedGenericModuleKey() throws Exception {
    String token = login("admin", "123456");

    mockMvc
        .perform(
            get("/api/pingan/three-checks/unknown-module/records")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            get("/api/pingan/three-checks/unknown-module/records/1/workflow")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  private String login(String username, String password) throws Exception {
    JsonNode response =
        postJson("/api/auth/login", null, Map.of("username", username, "password", password));
    return response.path("data").path("accessToken").asText();
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
        post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body));
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

  private void uploadThreeCheckRecordAttachment(
      String moduleKey, String id, String token, String fileKind, String fileName, String contentType)
      throws Exception {
    mockMvc
        .perform(
            multipart("/api/pingan/three-checks/" + moduleKey + "/records/" + id + "/attachments")
                .file(
                    new MockMultipartFile(
                        "file", fileName, contentType, "VIDEO".equals(fileKind) ? tinyMp4() : tinyJpeg()))
                .param("fileKind", fileKind)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  private void assertInspectionSubmitRejected(
      String token, String moduleKey, String businessDate, Map<String, Object> item) throws Exception {
    JsonNode created =
        postJson(
                "/api/pingan/three-checks/" + moduleKey + "/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    SOURCE_DEPARTMENT_ID,
                    "teamId",
                    SOURCE_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    businessDate,
                    "payload",
                    Map.of("statusLabel", "待检查", "checkItems", List.of(item))))
            .path("data");

    mockMvc
        .perform(
            post(
                    "/api/pingan/three-checks/"
                        + moduleKey
                        + "/records/"
                        + created.path("id").asText()
                        + "/submit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  private JsonNode deleteJson(String url, String token) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(delete(url).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }

  private JsonNode createGenericRecord(
      String token, String moduleKey, String statusLabel, Map<String, Object> payload)
      throws Exception {
    return createGenericRecord(token, moduleKey, statusLabel, payload, "2026-05-19");
  }

  private JsonNode createGenericRecord(
      String token,
      String moduleKey,
      String statusLabel,
      Map<String, Object> payload,
      String businessDate)
      throws Exception {
    return postJson(
            "/api/pingan/three-checks/" + moduleKey + "/records",
            token,
            Map.of(
                "companyId",
                SOURCE_COMPANY_ID,
                "departmentId",
                SOURCE_DEPARTMENT_ID,
                "teamId",
                SOURCE_TEAM_ID,
                "ownerUserId",
                2,
                "businessDate",
                businessDate,
                "status",
                statusLabel,
                "payload",
                payload))
        .path("data");
  }

  private String futureBusinessDate() {
    return "2036-10-01";
  }

  private Map<String, Object> miniKeySitePayload(
      long ownerUserId,
      long teamId,
      String businessDate,
      String siteType,
      String sourceRecordId,
      String clientRequestId) {
    return Map.of(
        "companyId",
        SOURCE_COMPANY_ID,
        "departmentId",
        SOURCE_DEPARTMENT_ID,
        "teamId",
        teamId,
        "ownerUserId",
        ownerUserId,
        "businessDate",
        businessDate,
        "payload",
        Map.of("siteType", siteType, "statusLabel", "待检查"),
        "sourceRecordId",
        sourceRecordId,
        "clientRequestId",
        clientRequestId);
  }

  private Map<String, Object> preShiftInspectionPayload(String imageCheck) {
    return Map.of(
        "owner",
        "湖贝班长",
        "imageCheck",
        imageCheck,
        "statusLabel",
        "待检查",
        "checkItems",
        List.of(
            Map.of(
                "riskType",
                "机械伤害",
                "checkItem",
                "检查机械设备是否处于良好状态",
                "checkResult",
                "无隐患")));
  }

  private JsonNode quickShotWorkflowAction(String token, String id, String action, int version)
      throws Exception {
    Map<String, Object> payload =
        switch (action) {
          case "ISSUE_RECTIFICATION" ->
              Map.of(
                  "rectificationResponsiblePerson",
                  "湖贝班长",
                  "rectificationDeadline",
                  "2026-05-30",
                  "rectificationRequirement",
                  "按要求整改");
          case "MARK_RECTIFIED" ->
              Map.of(
                  "afterRectificationPhoto",
                  "/uploads/quick-shot-after.jpg",
                  "rectificationDescription",
                  "已完成整改");
          case "REQUEST_ACCEPTANCE" ->
              Map.of(
                  "acceptancePerson",
                  "幕墙安全员",
                  "acceptanceRequestRemark",
                  "申请验收");
          case "ACCEPT" -> Map.of("acceptanceResult", "通过", "acceptanceRemark", "验收合格");
          default -> Map.of("reviewRemark", "复核确认");
        };
    return postJson(
            "/api/pingan/three-checks/quick-shot/records/" + id + "/workflow-actions",
            token,
            Map.of("action", action, "payload", payload, "version", version))
        .path("data");
  }

  private JsonNode hazardOrderAction(
      String token, String id, String action, Map<String, Object> payload) throws Exception {
    int version =
        getJson("/api/pingan/hazard-rectification/orders/" + id, token)
            .path("data")
            .path("version")
            .asInt();
    return postJson(
            "/api/pingan/hazard-rectification/orders/" + id + "/actions",
            token,
            Map.of("action", action, "payload", payload, "version", version))
        .path("data");
  }

  private JsonNode createPointsFlowRecord(
      String token,
      String businessDate,
      long teamId,
      String userName,
      String pointsReason,
      String pointsChange,
      int pointsQuantity)
      throws Exception {
    long departmentId = teamId == 1011003L ? 101110L : SOURCE_DEPARTMENT_ID;
    String createdAt = businessDate + " 08:30:00";
    Map<String, Object> payload = new HashMap<>();
    payload.put("createdAt", createdAt);
    payload.put("user", userName);
    payload.put("pointsReason", pointsReason);
    payload.put("pointsChange", pointsChange);
    payload.put("pointsQuantity", pointsQuantity);
    if ("兑换".equals(pointsChange)) {
      payload.put("vendingMachine", "榜单兑换机");
      payload.put("goods", "矿泉水");
    }
    return postJson(
            "/api/pingan/three-checks/points-flow/records",
            token,
            Map.of(
                "companyId",
                SOURCE_COMPANY_ID,
                "departmentId",
                departmentId,
                "teamId",
                teamId,
                "ownerUserId",
                2,
                "businessDate",
                businessDate,
                "payload",
                payload))
        .path("data");
  }

  private List<String> idsIn(JsonNode nodes) {
    List<String> ids = new ArrayList<>();
    for (JsonNode node : nodes) {
      ids.add(node.path("id").asText());
    }
    return ids;
  }

  private List<String> recordNosIn(JsonNode nodes) {
    List<String> recordNos = new ArrayList<>();
    for (JsonNode node : nodes) {
      recordNos.add(node.path("recordNo").asText());
    }
    return recordNos;
  }

  private List<String> namesIn(JsonNode nodes) {
    List<String> names = new ArrayList<>();
    for (JsonNode node : nodes) {
      names.add(node.path("name").asText());
    }
    return names;
  }

  private List<String> actionsIn(JsonNode nodes) {
    List<String> actions = new ArrayList<>();
    for (JsonNode node : nodes) {
      actions.add(node.path("action").asText());
    }
    return actions;
  }

  private JsonNode firstLogByAction(JsonNode nodes, String action) {
    for (JsonNode node : nodes) {
      if (action.equals(node.path("action").asText())) {
        return node;
      }
    }
    return objectMapper.createObjectNode();
  }

  private JsonNode firstHistoryByField(JsonNode nodes, String fieldKey) {
    for (JsonNode node : nodes) {
      if (fieldKey.equals(node.path("fieldKey").asText())) {
        return node;
      }
    }
    return objectMapper.createObjectNode();
  }

  private JsonNode firstItemById(JsonNode nodes, String id) {
    for (JsonNode node : nodes) {
      if (id.equals(node.path("id").asText())) {
        return node;
      }
    }
    return objectMapper.createObjectNode();
  }

  private int childCount(long rootDispatchRecordId) {
    return jdbcTemplate.queryForObject(
        """
        select count(*)
        from three_check_record
        where deleted = 0
          and root_dispatch_record_id = ?
          and module_key in (
            'pre-shift-meeting',
            'pre-shift-inspection',
            'mid-shift-inspection',
            'post-shift-inspection'
          )
        """,
        Integer.class,
        rootDispatchRecordId);
  }

  private JsonNode childByModule(long rootDispatchRecordId, String moduleKey) throws Exception {
    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            """
            select id, module_key, task_id, root_dispatch_record_id, company_id, department_id,
              team_id, owner_user_id, business_date, status, source_record_id, client_request_id,
              payload_json
            from three_check_record
            where deleted = 0
              and root_dispatch_record_id = ?
              and module_key = ?
            """,
            rootDispatchRecordId,
            moduleKey);
    return objectMapper.valueToTree(row);
  }

  private void deleteExistingTeamCheckTemplate(String stage) {
    jdbcTemplate.update(
        """
        update sys_team_check_template_item
        set deleted = template_id
        where template_id in (
          select id
          from sys_team_check_template
          where company_org_id = ?
            and department_org_id = ?
            and team_org_id = ?
            and inspection_stage = ?
            and deleted = 0
        )
        """,
        SOURCE_COMPANY_ID,
        SOURCE_DEPARTMENT_ID,
        SOURCE_TEAM_ID,
        stage);
    jdbcTemplate.update(
        """
        update sys_team_check_template
        set deleted = id
        where company_org_id = ?
          and department_org_id = ?
          and team_org_id = ?
          and inspection_stage = ?
          and deleted = 0
        """,
        SOURCE_COMPANY_ID,
        SOURCE_DEPARTMENT_ID,
        SOURCE_TEAM_ID,
        stage);
  }

  private void insertRecordForEnterpriseQuery(String recordNo, long ownerUserId, String businessDate) {
    insertRecordForEnterpriseQuery(
        recordNo, "pre-shift-inspection", ownerUserId, SOURCE_TEAM_ID, businessDate);
  }

  private void insertRecordForEnterpriseQuery(
      String recordNo, String moduleKey, long ownerUserId, long teamId, String businessDate) {
    insertRecordForEnterpriseQueryAndReturnId(
        recordNo, moduleKey, ownerUserId, teamId, businessDate, "DRAFT");
  }

  private JsonNode findRecordById(JsonNode items, long id) {
    for (JsonNode item : items) {
      if (String.valueOf(id).equals(item.path("id").asText())) {
        return item;
      }
    }
    throw new AssertionError("record not found: " + id);
  }

  private long insertRecordForEnterpriseQueryAndReturnId(
      String recordNo,
      String moduleKey,
      long ownerUserId,
      long teamId,
      String businessDate,
      String status) {
    long companyId = teamId == 10L ? 8L : SOURCE_COMPANY_ID;
    long departmentId = switch ((int) teamId) {
      case 10 -> 9L;
      case 1011002 -> SOURCE_DEPARTMENT_ID;
      default -> teamId == 1011003L ? 101110L : SOURCE_DEPARTMENT_ID;
    };
    String statusLabel = "OPENED".equals(status) ? "已检查" : "待检查";
    jdbcTemplate.update(
        """
        insert into three_check_record (
          module_key, record_no, company_id, department_id, team_id, owner_user_id,
          business_date, status, payload_json, image_check_status, video_check_status,
          reminder_count, version, created_by, updated_by, source_channel,
          client_request_id, last_synced_at, created_at, updated_at, deleted
        ) values (
          ?, ?, ?, ?, ?, ?, cast(? as date), ?,
          ?,
          '未上传', '未上传', 0, 0, 1, 1, 'PC', ?, current_timestamp,
          current_timestamp, current_timestamp, 0
        )
        """,
        moduleKey,
        recordNo,
        companyId,
        departmentId,
        teamId,
        ownerUserId,
        businessDate,
        status,
        "{\"owner\":\"本人范围用户\",\"statusLabel\":\"" + statusLabel + "\"}",
        "client-" + recordNo);
    Long id =
        jdbcTemplate.queryForObject(
            "select id from three_check_record where record_no = ?",
            Long.class,
            recordNo);
    return id == null ? -1L : id;
  }

  private long insertSubmitReadyOneShiftRecord(
      String recordNo, String moduleKey, long ownerUserId, String businessDate)
      throws Exception {
    Map<String, Object> payload = new HashMap<>();
    payload.put("owner", "提交前组员");
    payload.put("responsiblePerson", "提交前组员");
    payload.put("statusLabel", "待检查");
    if ("pre-shift-meeting".equals(moduleKey)) {
      payload.put("meetingContent", "班长提交班前会");
      payload.put("attendeesText", "湖贝班长、提交前组员");
    } else {
      payload.put(
          "checkItems",
          List.of(
              Map.of(
                  "riskType",
                  "机械伤害",
                  "checkItem",
                  "检查机械设备是否处于良好状态",
                  "checkResult",
                  "无隐患")));
    }
    jdbcTemplate.update(
        """
        insert into three_check_record (
          module_key, record_no, company_id, department_id, team_id, owner_user_id,
          business_date, status, payload_json, image_check_status, video_check_status,
          reminder_count, version, created_by, updated_by, source_channel,
          client_request_id, last_synced_at, created_at, updated_at, deleted
        ) values (
          ?, ?, ?, ?, ?, ?, cast(? as date), 'DRAFT',
          ?,
          '现场照片', '未上传', 0, 0, 1, 1, 'PC', ?, current_timestamp,
          current_timestamp, current_timestamp, 0
        )
        """,
        moduleKey,
        recordNo,
        SOURCE_COMPANY_ID,
        SOURCE_DEPARTMENT_ID,
        SOURCE_TEAM_ID,
        ownerUserId,
        businessDate,
        objectMapper.writeValueAsString(payload),
        "client-" + recordNo);
    Long id =
        jdbcTemplate.queryForObject(
            "select id from three_check_record where record_no = ?",
            Long.class,
            recordNo);
    return id == null ? -1L : id;
  }

  private void setRootDispatchRecordId(long recordId, long rootDispatchRecordId) {
    jdbcTemplate.update(
        "update three_check_record set root_dispatch_record_id = ? where id = ?",
        rootDispatchRecordId,
        recordId);
  }

  private void createThreeCheckRoleUser(
      long userId, String username, String realName, long orgId, String... roleCodes) {
    jdbcTemplate.update(
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (?, ?, '{noop}123456', ?, ?, 'ACTIVE', 0)",
        userId,
        username,
        realName,
        orgId);
    for (String roleCode : roleCodes) {
      jdbcTemplate.update(
          """
          insert into sys_user_role (user_id, role_id)
          select ?, id
          from sys_role
          where role_code = ?
          """,
          userId,
          roleCode);
    }
  }

  private void cleanupThreeCheckAccessFixture(long userId, String username, String recordNoPrefix) {
    jdbcTemplate.update(
        """
        delete from biz_change_history
        where biz_id in (
          select id from three_check_record where record_no like ?
        )
        """,
        recordNoPrefix + "%");
    jdbcTemplate.update(
        """
        delete from biz_status_log
        where biz_id in (
          select id from three_check_record where record_no like ?
        )
        """,
        recordNoPrefix + "%");
    jdbcTemplate.update(
        "delete from three_check_record where record_no like ?",
        recordNoPrefix + "%");
    jdbcTemplate.update("delete from sys_team_member where user_id = ? or username = ?", userId, username);
    jdbcTemplate.update("delete from sys_user_role where user_id = ?", userId);
    jdbcTemplate.update("delete from sys_user where id = ? or username = ?", userId, username);
  }

  private void grantThreeCheckPermissions(long userId, String... permissionCodes) {
    for (String permissionCode : permissionCodes) {
      jdbcTemplate.update(
          """
          insert into sys_role_menu (role_id, menu_id)
          select ur.role_id, m.id
          from sys_user_role ur
          cross join sys_menu m
          where ur.user_id = ?
            and m.permission_code = ?
            and not exists (
              select 1
              from sys_role_menu rm
              where rm.role_id = ur.role_id
                and rm.menu_id = m.id
            )
          """,
          userId,
          permissionCode);
    }
  }

  private void createPermissionFixtureUser(
      long userId,
      long roleId,
      String username,
      String realName,
      String roleCode,
      String... permissionCodes) {
    jdbcTemplate.update("delete from sys_user_role where user_id = ? or role_id = ?", userId, roleId);
    jdbcTemplate.update("delete from sys_role_menu where role_id = ?", roleId);
    jdbcTemplate.update("delete from sys_role where id = ? or role_code = ?", roleId, roleCode);
    jdbcTemplate.update("delete from sys_user where id = ? or username = ?", userId, username);
    jdbcTemplate.update(
        "insert into sys_role (id, role_code, role_name, data_scope) values (?, ?, ?, 'SELF')",
        roleId,
        roleCode,
        realName + "角色");
    jdbcTemplate.update(
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (?, ?, '{noop}123456', ?, ?, 'ACTIVE', 0)",
        userId,
        username,
        realName,
        SOURCE_TEAM_ID);
    jdbcTemplate.update("insert into sys_user_role (user_id, role_id) values (?, ?)", userId, roleId);
    grantThreeCheckPermissions(userId, permissionCodes);
  }

  private Map<String, Object> quickShotPayload(long ownerUserId, String businessDate) {
    return Map.of(
        "companyId",
        SOURCE_COMPANY_ID,
        "departmentId",
        SOURCE_DEPARTMENT_ID,
        "teamId",
        SOURCE_TEAM_ID,
        "ownerUserId",
        ownerUserId,
        "businessDate",
        businessDate,
        "payload",
        Map.of("hazardDescription", "quick shot rbac hazard"));
  }

  private Map<String, Object> genericThreeCheckPayload(
      long ownerUserId, String businessDate, String description) {
    return Map.of(
        "companyId",
        SOURCE_COMPANY_ID,
        "departmentId",
        SOURCE_DEPARTMENT_ID,
        "teamId",
        SOURCE_TEAM_ID,
        "ownerUserId",
        ownerUserId,
        "businessDate",
        businessDate,
        "payload",
        Map.of("workContent", description, "statusLabel", "待检查"));
  }

  private Map<String, Object> inspectionThreeCheckPayload(
      long ownerUserId, String businessDate, String description) {
    return Map.of(
        "companyId",
        SOURCE_COMPANY_ID,
        "departmentId",
        SOURCE_DEPARTMENT_ID,
        "teamId",
        SOURCE_TEAM_ID,
        "ownerUserId",
        ownerUserId,
        "businessDate",
        businessDate,
        "payload",
        Map.of(
            "workContent",
            description,
            "statusLabel",
            "待检查",
            "checkItems",
            List.of(
                Map.of(
                    "riskType",
                    "机械伤害",
                    "checkItem",
                    "检查设备防护",
                "checkResult",
                "无隐患"))));
  }

  private Map<String, Object> inspectionThreeCheckPayloadWithResult(
      long ownerUserId, String businessDate, String description, String checkResult) {
    return Map.of(
        "companyId",
        SOURCE_COMPANY_ID,
        "departmentId",
        SOURCE_DEPARTMENT_ID,
        "teamId",
        SOURCE_TEAM_ID,
        "ownerUserId",
        ownerUserId,
        "businessDate",
        businessDate,
        "payload",
        Map.of(
            "workContent",
            description,
            "statusLabel",
            "待检查",
            "checkItems",
            List.of(
                Map.of(
                    "lineId",
                    "permission-" + ownerUserId + "-" + businessDate,
                    "riskType",
                    "机械伤害",
                    "checkItem",
                    "检查设备防护",
                    "checkResult",
                    checkResult,
                    "hazardDescription",
                    "设备防护缺失"))));
  }

  private Map<String, Object> miniQuickShotPayload(
      long ownerUserId, String businessDate, String sourceRecordId, String clientRequestId) {
    Map<String, Object> payload = new HashMap<>(quickShotPayload(ownerUserId, businessDate));
    payload.put("sourceRecordId", sourceRecordId);
    payload.put("clientRequestId", clientRequestId);
    return payload;
  }

  private void assertForbiddenMutationLeavesRecordUnchanged(
      String token,
      String url,
      Object body,
      long recordId,
      String expectedStatus,
      int expectedReminderCount)
      throws Exception {
    int historyBefore = countHistory(recordId);
    int logsBefore = countStatusLogs(recordId);
    Map<String, Object> rowBefore =
        jdbcTemplate.queryForMap(
            """
            select status, reminder_count, version, updated_by, updated_at
            from three_check_record
            where id = ?
            """,
            recordId);
    mockMvc
        .perform(
            post(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isForbidden());
    Map<String, Object> rowAfter =
        jdbcTemplate.queryForMap(
            """
            select status, reminder_count, version, updated_by, updated_at
            from three_check_record
            where id = ?
            """,
            recordId);
    assertThat(rowAfter).isEqualTo(rowBefore);
    assertThat(rowAfter.get("status")).isEqualTo(expectedStatus);
    assertThat(((Number) rowAfter.get("reminder_count")).intValue()).isEqualTo(expectedReminderCount);
    assertThat(countHistory(recordId)).isEqualTo(historyBefore);
    assertThat(countStatusLogs(recordId)).isEqualTo(logsBefore);
  }

  private int countHistory(long recordId) {
    return jdbcTemplate.queryForObject(
        "select count(*) from biz_change_history where biz_id = ?",
        Integer.class,
        recordId);
  }

  private int countStatusLogs(long recordId) {
    return jdbcTemplate.queryForObject(
        "select count(*) from biz_status_log where biz_id = ?",
        Integer.class,
        recordId);
  }

  private int countAttachmentsByOriginalName(String originalName) {
    return jdbcTemplate.queryForObject(
        "select count(*) from biz_attachment where original_name = ?",
        Integer.class,
        originalName);
  }

  private byte[] tinyJpeg() {
    return new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xd9};
  }

  private byte[] tinyMp4() {
    return new byte[] {0, 0, 0, 12, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
  }

  private void createTeamLeaderMember(
      long teamId, long userId, String username, String memberName) {
    jdbcTemplate.update(
        """
        insert into sys_team_member (team_org_id, user_id, username, member_name, member_role, deleted)
        values (?, ?, ?, ?, 'LEADER', 0)
        """,
        teamId,
        userId,
        username,
        memberName);
  }

  private void createTeamMember(
      long teamId, long userId, String username, String memberName) {
    jdbcTemplate.update(
        """
        insert into sys_team_member (team_org_id, user_id, username, member_name, member_role, deleted)
        values (?, ?, ?, ?, 'MEMBER', 0)
        """,
        teamId,
        userId,
        username,
        memberName);
  }
}
