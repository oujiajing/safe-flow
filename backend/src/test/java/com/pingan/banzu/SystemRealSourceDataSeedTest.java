package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
@SpringBootTest
class SystemRealSourceDataSeedTest {

  private static final long SOURCE_COMPANY_ID = 4L;
  private static final long CURTAIN_WALL_ASSEMBLY_DEPARTMENT_ID = 101109L;
  private static final long CURTAIN_WALL_ASSEMBLY_TEAM_ID = 1011001L;
  private static final String SOURCE_COMPANY_NAME = "Demo Works Company";

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void seedsRealSourceCompanyDepartmentAndTeamMasterData() {
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from sys_org o
                join sys_company_profile p on p.org_id = o.id and p.deleted = 0
                where o.id = ?
                  and o.org_code = '1011'
                  and o.org_name = ?
                  and o.org_type = 'COMPANY'
                  and o.status = 'ACTIVE'
                  and o.deleted = 0
                  and p.short_name = 'Demo Works Company'
                  and p.company_type = '子公司'
                  and p.level1_name = 'Demo控股集团'
                  and p.level2_name = 'Demo Safety Holdings'
                  and p.level3_name = 'Demo Works Company'
                  and p.level4_name = 'Demo Works Company'
                """,
                Integer.class,
                SOURCE_COMPANY_ID,
                SOURCE_COMPANY_NAME))
        .isEqualTo(1);

    assertCompanyHierarchy(1L, "集团", "Demo控股集团", null, null, null);
    assertCompanyHierarchy(2L, "集团", "Demo控股集团", "Demo Safety Holdings", null, null);
    assertCompanyHierarchy(3L, "分公司", "Demo控股集团", "Demo Safety Holdings", "Demo Works Company", null);
    assertCompanyHierarchy(4L, "子公司", "Demo控股集团", "Demo Safety Holdings", "Demo Works Company", "Demo Works Company");
    assertCompanyHierarchy(11L, "分公司", "Demo控股集团", "Demo Safety Holdings", "Demo矿投", null);
    assertCompanyHierarchy(8L, "子公司", "Demo控股集团", "Demo Safety Holdings", "Demo矿投", "Demo East Site");
    assertCompanyHierarchy(27L, "分公司", "Demo控股集团", "Demo Safety Holdings", "Demo Training School", null);
    assertCompanyTypeCount("分公司", 12);
    assertCompanyTypeCount("子公司", 13);
    assertYuanchengOperatingProfileSyncedToGroup(1L);
    assertYuanchengOperatingProfileSyncedToGroup(2L);

    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from sys_org o
                join sys_department_profile p on p.org_id = o.id and p.deleted = 0
                where p.company_org_id = ?
                  and o.org_type = 'DEPARTMENT'
                  and o.status = 'ACTIVE'
                  and o.deleted = 0
                """,
                Integer.class,
                SOURCE_COMPANY_ID))
        .isEqualTo(15);
    assertThat(orgNames("DEPARTMENT")).contains("海安加工厂", "安全质量职卫部", "幕墙组装", "资材仓库");

    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from sys_org o
                join sys_team_profile p on p.org_id = o.id and p.deleted = 0
                where p.company_org_id = ?
                  and o.org_type = 'TEAM'
                  and o.status = 'ACTIVE'
                  and o.deleted = 0
                """,
                Integer.class,
                SOURCE_COMPANY_ID))
        .isEqualTo(19);
    assertThat(orgNames("TEAM")).contains("幕墙组装1班", "配件仓库班", "门窗加工4班");
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from sys_team_member
                where team_org_id = ?
                  and username = 'member52'
                  and member_role = 'MEMBER'
                  and deleted = 0
                """,
                Integer.class,
                CURTAIN_WALL_ASSEMBLY_TEAM_ID))
        .isEqualTo(1);
  }

  @Test
  void realSourceDataIsSelectableFromSystemOptionsAndPreShiftMeetingForms() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

    JsonNode companies = getJson("/api/system/options/companies", token).path("data");
    assertThat(optionLabels(companies)).contains(SOURCE_COMPANY_NAME);
    assertThat(optionValues(companies, SOURCE_COMPANY_NAME)).contains(SOURCE_COMPANY_ID);

    JsonNode departments =
        getJson("/api/system/options/departments?companyOrgId=" + SOURCE_COMPANY_ID, token).path("data");
    assertThat(optionLabels(departments)).contains("幕墙组装", "资材仓库");
    assertThat(optionValue(departments, "幕墙组装")).isEqualTo(CURTAIN_WALL_ASSEMBLY_DEPARTMENT_ID);

    JsonNode teams =
        getJson("/api/system/options/teams?parentOrgId=" + CURTAIN_WALL_ASSEMBLY_DEPARTMENT_ID, token).path("data");
    assertThat(optionLabels(teams)).contains("幕墙组装1班", "幕墙组装2班");
    assertThat(optionValue(teams, "幕墙组装1班")).isEqualTo(CURTAIN_WALL_ASSEMBLY_TEAM_ID);

    JsonNode tree = getJson("/api/pingan/org/tree", token).path("data");
    assertThat(flattenOrgTitles(tree)).contains(SOURCE_COMPANY_NAME, "幕墙组装", "幕墙组装1班");

    JsonNode created =
        postJson(
                "/api/pingan/three-checks/pre-shift-meeting/records",
                token,
                Map.of(
                    "companyId",
                    SOURCE_COMPANY_ID,
                    "departmentId",
                    CURTAIN_WALL_ASSEMBLY_DEPARTMENT_ID,
                    "teamId",
                    CURTAIN_WALL_ASSEMBLY_TEAM_ID,
                    "ownerUserId",
                    2,
                    "businessDate",
                    "2025-05-21",
                    "payload",
                    Map.of(
                        "meetingContent",
                        "使用Demo Works真实组织数据创建班前会。",
                        "attendees",
                        List.of("Demo Harbor班长"),
                        "attendeesText",
                        "Demo Harbor班长")))
            .path("data");

    assertThat(created.path("company").asText()).isEqualTo(SOURCE_COMPANY_NAME);
    assertThat(created.path("department").asText()).isEqualTo("幕墙组装");
    assertThat(created.path("team").asText()).isEqualTo("幕墙组装1班");
  }

  @Test
  void doesNotSeedMonitorCenterSimulationDataInRealBusinessTables() {
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from three_check_record
                where record_no like 'MCSIM-%'
                  and business_date = CURRENT_DATE
                  and deleted = 0
                """,
                Integer.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from risk_control_library l
                join risk_control_hazard h on h.library_id = l.id and h.deleted = 0
                where l.name like '监控大屏模拟%'
                  and l.deleted = 0
                """,
                Integer.class))
        .isZero();
  }

  @Test
  void productionMigrationsDoNotLeaveMonitorCenterBusinessTestData() {
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from three_check_record
                where record_no like 'MCTEST-%'
                  and deleted = 0
                """,
                Integer.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from risk_control_library l
                join risk_control_hazard h on h.library_id = l.id and h.deleted = 0
                where l.name like '监控中心测试%'
                  and l.deleted = 0
                """,
                Integer.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from training_safety_learning_content
                where code like 'MCTEST-LEARN-%'
                  and status = 'ACTIVE'
                  and deleted = 0
                """,
                Integer.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from special_work_record
                where project like 'MCTEST-SW-%'
                  and deleted = 0
                """,
                Integer.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from hazard_rectification_order
                where order_no = 'MCTEST-HRO-YC-001'
                """,
                Integer.class))
        .isZero();

    assertThat(orgNames("COMPANY")).contains(SOURCE_COMPANY_NAME);
    assertThat(orgNames("DEPARTMENT")).contains("幕墙组装");
    assertThat(orgNames("TEAM")).contains("幕墙组装1班", "幕墙组装2班");
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

  private List<String> orgNames(String orgType) {
    return jdbcTemplate.queryForList(
        "select org_name from sys_org where org_type = ? and deleted = 0 order by sort_order, id",
        String.class,
        orgType);
  }

  private void assertCompanyHierarchy(
      Long orgId, String companyType, String level1Name, String level2Name, String level3Name, String level4Name) {
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from sys_company_profile
                where org_id = ?
                  and company_type = ?
                  and level1_name = ?
                  and coalesce(level2_name, '') = coalesce(?, '')
                  and coalesce(level3_name, '') = coalesce(?, '')
                  and coalesce(level4_name, '') = coalesce(?, '')
                  and deleted = 0
                """,
                Integer.class,
                orgId,
                companyType,
                level1Name,
                level2Name,
                level3Name,
                level4Name))
        .isEqualTo(1);
  }

  private void assertCompanyTypeCount(String companyType, int expectedCount) {
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from sys_company_profile p
                join sys_org o on o.id = p.org_id
                where o.org_path like '/1/2/%'
                  and o.deleted = 0
                  and p.deleted = 0
                  and p.company_type = ?
                """,
                Integer.class,
                companyType))
        .isEqualTo(expectedCount);
  }

  private void assertYuanchengOperatingProfileSyncedToGroup(Long orgId) {
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from sys_company_profile group_profile
                join sys_company_profile source_profile on source_profile.org_id = ?
                where group_profile.org_id = ?
                  and group_profile.address = source_profile.address
                  and group_profile.safety_manager_username = source_profile.safety_manager_username
                  and group_profile.reporter_l1_usernames = source_profile.reporter_l1_usernames
                  and group_profile.reporter_l2_usernames = source_profile.reporter_l2_usernames
                  and group_profile.reporter_l3_usernames = source_profile.reporter_l3_usernames
                  and group_profile.report_l1_time = source_profile.report_l1_time
                  and group_profile.report_l2_time = source_profile.report_l2_time
                  and group_profile.report_l3_time = source_profile.report_l3_time
                  and group_profile.attachment1_url = source_profile.attachment1_url
                  and group_profile.attachment2_url = source_profile.attachment2_url
                  and group_profile.company_intro = source_profile.company_intro
                  and group_profile.deleted = 0
                  and source_profile.deleted = 0
                """,
                Integer.class,
                SOURCE_COMPANY_ID,
                orgId))
        .isEqualTo(1);
  }

  private List<String> optionLabels(JsonNode options) {
    List<String> labels = new ArrayList<>();
    for (JsonNode option : options) {
      labels.add(option.path("label").asText());
    }
    return labels;
  }

  private long optionValue(JsonNode options, String label) {
    for (JsonNode option : options) {
      if (label.equals(option.path("label").asText())) {
        return option.path("value").asLong();
      }
    }
    return -1L;
  }

  private List<Long> optionValues(JsonNode options, String label) {
    List<Long> values = new ArrayList<>();
    for (JsonNode option : options) {
      if (label.equals(option.path("label").asText())) {
        values.add(option.path("value").asLong());
      }
    }
    return values;
  }

  private int valueByName(JsonNode items, String name) {
    return byName(items, name).path("value").asInt();
  }

  private int seriesValue(JsonNode series, String seriesName, String companyName) {
    JsonNode item = byName(series, seriesName);
    JsonNode companies = item.path("companies");
    JsonNode values = item.path("data");
    for (int index = 0; index < companies.size(); index++) {
      if (companyName.equals(companies.get(index).path("name").asText())) {
        return values.get(index).asInt();
      }
    }
    return -1;
  }

  private JsonNode byName(JsonNode items, String name) {
    for (JsonNode item : items) {
      if (name.equals(item.path("name").asText())) {
        return item;
      }
    }
    throw new AssertionError("Missing item named " + name + " in " + items);
  }

  private List<String> flattenOrgTitles(JsonNode nodes) {
    List<String> titles = new ArrayList<>();
    for (JsonNode node : nodes) {
      titles.add(node.path("title").asText());
      titles.addAll(flattenOrgTitles(node.path("children")));
    }
    return titles;
  }
}
