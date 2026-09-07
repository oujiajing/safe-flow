package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
class SystemMasterDataApiTest {
  private static final long JS_UNSAFE_COMPANY_ID = 2_055_946_189_550_952_450L;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void managesCompanyMasterDataWithProfileFieldsAndSoftDelete() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/system/companies",
                token,
                Map.ofEntries(
                    Map.entry("code", "TEST-COMPANY"),
                    Map.entry("sortOrder", 33),
                    Map.entry("name", "测试公司"),
                    Map.entry("shortName", "测试简称"),
                    Map.entry("description", "公司描述"),
                    Map.entry("status", "DRAFT"),
                    Map.entry("address", "广州市测试路1号"),
                    Map.entry("companyType", "集团"),
                    Map.entry("level1Name", "测试公司"),
                    Map.entry("safetyManagerUsername", "MQ_SAFE"),
                    Map.entry("reporterL1Usernames", "HB_MONITOR"),
                    Map.entry("reporterL2Usernames", "MQ_SAFE"),
                    Map.entry("reporterL3Usernames", "admin"),
                    Map.entry("reportL1Time", 15),
                    Map.entry("reportL2Time", 30),
                    Map.entry("reportL3Time", 45),
                    Map.entry("attachment1Url", "/files/company-1.pdf"),
                    Map.entry("attachment2Url", "/files/company-2.pdf"),
                    Map.entry("companyIntro", "公司介绍")))
            .path("data");

    Long companyId = created.path("id").asLong();
    assertThat(created.path("code").asText()).isEqualTo("TEST-COMPANY");
    assertThat(created.path("shortName").asText()).isEqualTo("测试简称");
    assertThat(created.path("reportL3Time").asInt()).isEqualTo(45);
    assertThat(created.path("companyIntro").asText()).isEqualTo("公司介绍");

    JsonNode updated =
        putJson(
                "/api/system/companies/" + companyId,
                token,
                Map.ofEntries(
                    Map.entry("code", "TEST-COMPANY"),
                    Map.entry("sortOrder", 34),
                    Map.entry("name", "测试公司更新"),
                    Map.entry("shortName", "测试更新"),
                    Map.entry("description", "更新描述"),
                    Map.entry("status", "ACTIVE"),
                    Map.entry("address", "深圳市测试路2号"),
                    Map.entry("companyType", "集团"),
                    Map.entry("level1Name", "测试公司更新"),
                    Map.entry("safetyManagerUsername", "admin"),
                    Map.entry("reporterL1Usernames", "admin"),
                    Map.entry("reporterL2Usernames", "HB_MONITOR"),
                    Map.entry("reporterL3Usernames", "MQ_SAFE"),
                    Map.entry("reportL1Time", 20),
                    Map.entry("reportL2Time", 40),
                    Map.entry("reportL3Time", 60),
                    Map.entry("attachment1Url", "/files/company-updated-1.pdf"),
                    Map.entry("attachment2Url", "/files/company-updated-2.pdf"),
                    Map.entry("companyIntro", "公司介绍更新")))
            .path("data");
    assertThat(updated.path("name").asText()).isEqualTo("测试公司更新");
    assertThat(updated.path("status").asText()).isEqualTo("ACTIVE");

    JsonNode inactive =
        patchJson("/api/system/companies/" + companyId + "/status", token, Map.of("status", "INACTIVE"))
            .path("data");
    assertThat(inactive.path("status").asText()).isEqualTo("INACTIVE");

    JsonNode list = getJson("/api/system/companies?keyword=测试公司更新&status=INACTIVE", token).path("data");
    assertThat(list.path("total").asInt()).isEqualTo(1);
    assertThat(list.path("items").get(0).path("address").asText()).isEqualTo("深圳市测试路2号");

    mockMvc.perform(delete("/api/system/companies/" + companyId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    assertThat(jdbcTemplate.queryForObject("select deleted from sys_org where id = ?", Integer.class, companyId))
        .isEqualTo(1);
    assertThat(getJson("/api/system/companies?keyword=测试公司更新", token).path("data").path("total").asInt())
        .isZero();
    assertAudit("COMPANY", "SYS_ORG", companyId, "CREATE");
    assertAudit("COMPANY", "SYS_ORG", companyId, "UPDATE");
    assertAudit("COMPANY", "SYS_ORG", companyId, "DELETE");
  }

  @Test
  void managesDepartmentTeamAndPersonnelMasterDataWithExcelFields() throws Exception {
    String token = login("admin", "123456");
    long companyId = 8L;

    JsonNode department =
        postJson(
                "/api/system/departments",
                token,
                Map.ofEntries(
                    Map.entry("code", "TEST-DEPT"),
                    Map.entry("name", "测试部门"),
                    Map.entry("companyOrgId", companyId),
                    Map.entry("departmentType", "WORKSHOP"),
                    Map.entry("childSortOrder", 5),
                    Map.entry("leaderUsername", "ZY_SUPERVISOR"),
                    Map.entry("description", "部门描述"),
                    Map.entry("status", "ACTIVE"),
                    Map.entry("topLevelName", "顶级"),
                    Map.entry("groupName", "集团"),
                    Map.entry("level1Unit", "一级单位"),
                    Map.entry("level2Unit", "二级单位"),
                    Map.entry("leaderLevel", "L2"),
                    Map.entry("companySortOrder", 8)))
            .path("data");
    Long departmentId = department.path("id").asLong();
    assertThat(department.path("companyName").asText()).isEqualTo("梅州嘉晟");
    assertThat(department.path("leaderLevel").asText()).isEqualTo("L2");

    JsonNode team =
        postJson(
                "/api/system/teams",
                token,
                Map.ofEntries(
                    Map.entry("code", "TEST-TEAM"),
                    Map.entry("name", "测试班组"),
                    Map.entry("companyOrgId", companyId),
                    Map.entry("workshopOrgId", departmentId),
                    Map.entry("groupName", "集团"),
                    Map.entry("level1Unit", "一级单位"),
                    Map.entry("level2Unit", "二级单位"),
                    Map.entry("workshopName", "测试车间"),
                    Map.entry("workTypeCode", "MAINTAIN"),
                    Map.entry("workTypeName", "检修作业"),
                    Map.entry("status", "ACTIVE"),
                    Map.entry("leaderUsername", "ZY_SUPERVISOR"),
                    Map.entry("teamMembers", List.of("作业人员甲", "作业人员乙")),
                    Map.entry("safetyOfficerUsername", "MQ_SAFE"),
                    Map.entry("submitDate", "2026-05-17"),
                    Map.entry("applicantName", "管理员"),
                    Map.entry("points", 12),
                    Map.entry("active", true)))
            .path("data");
    Long teamId = team.path("id").asLong();
    assertThat(team.path("workTypeName").asText()).isEqualTo("检修作业");
    assertThat(team.path("teamMembers")).hasSize(2);
    assertThat(team.path("submitDate").asText()).isEqualTo("2026-05-17");
    assertThat(team.path("applicantName").asText()).isEqualTo("管理员");

    JsonNode updatedTeam =
        putJson(
                "/api/system/teams/" + teamId,
                token,
                Map.ofEntries(
                    Map.entry("code", "TEST-TEAM"),
                    Map.entry("name", "测试班组更新"),
                    Map.entry("companyOrgId", companyId),
                    Map.entry("workshopOrgId", departmentId),
                    Map.entry("groupName", "集团更新"),
                    Map.entry("level1Unit", "一级单位更新"),
                    Map.entry("level2Unit", "二级单位更新"),
                    Map.entry("workshopName", "测试车间更新"),
                    Map.entry("workTypeCode", "MAINTAIN"),
                    Map.entry("workTypeName", "检修作业更新"),
                    Map.entry("status", "ACTIVE"),
                    Map.entry("leaderUsername", "ZY_SUPERVISOR"),
                    Map.entry("teamMembers", List.of("作业人员丙")),
                    Map.entry("safetyOfficerUsername", "MQ_SAFE"),
                    Map.entry("submitDate", "2026-05-18"),
                    Map.entry("applicantName", "班组申请人"),
                    Map.entry("points", 18),
                    Map.entry("active", true)))
            .path("data");
    assertThat(updatedTeam.path("name").asText()).isEqualTo("测试班组更新");
    assertThat(updatedTeam.path("submitDate").asText()).isEqualTo("2026-05-18");
    assertThat(updatedTeam.path("applicantName").asText()).isEqualTo("班组申请人");
    assertThat(updatedTeam.path("teamMembers").get(0).asText()).isEqualTo("作业人员丙");

    JsonNode personnel =
        postJson(
                "/api/system/personnel",
                token,
                Map.ofEntries(
                    Map.entry("employeeCode", "TEST-PERSON"),
                    Map.entry("name", "测试人员"),
                    Map.entry("username", "TEST_PERSON_USER"),
                    Map.entry("companyOrgId", companyId),
                    Map.entry("companyShortName", "梅州"),
                    Map.entry("departmentOrgId", departmentId),
                    Map.entry("teamOrgId", teamId),
                    Map.entry("points", 21),
                    Map.entry("receivedPoints", 7),
                    Map.entry("employeeType", "FULL_TIME"),
                    Map.entry("positionName", "安全岗"),
                    Map.entry("mobile", "13900009999"),
                    Map.entry("status", "ACTIVE"),
                    Map.entry("systemRoleCode", "班组长"),
                    Map.entry("remark", "人员备注"),
                    Map.entry("certificateValidUntil", "2026-12-31"),
                    Map.entry("joinDate", "2026-05-18"),
                    Map.entry("submitDate", "2026-05-18"),
                    Map.entry("applicantName", "管理员"),
                    Map.entry("departmentSortOrder", 6),
                    Map.entry("managementWeight", 9)))
            .path("data");
    Long personnelId = personnel.path("id").asLong();
    assertThat(personnel.path("companyName").asText()).isEqualTo("梅州嘉晟");
    assertThat(personnel.path("departmentName").asText()).isEqualTo("测试部门");
    assertThat(personnel.path("teamName").asText()).isEqualTo("测试班组更新");
    assertThat(personnel.path("managementWeight").asInt()).isEqualTo(9);
    assertThat(personnel.path("systemRoleCode").asText()).isEqualTo("班组长");
    assertThat(personnel.path("positionName").asText()).isEqualTo("安全岗");
    assertThat(personnel.path("remark").asText()).isEqualTo("人员备注");
    assertThat(personnel.path("certificateValidUntil").asText()).isEqualTo("2026-12-31");
    assertThat(personnel.path("joinDate").asText()).isEqualTo("2026-05-18");
    assertThat(personnel.path("submitDate").asText()).isEqualTo("2026-05-18");
    assertThat(personnel.path("applicantName").asText()).isEqualTo("管理员");

    JsonNode updatedPersonnel =
        putJson(
                "/api/system/personnel/" + personnelId,
                token,
                Map.ofEntries(
                    Map.entry("employeeCode", "TEST-PERSON"),
                    Map.entry("name", "测试人员更新"),
                    Map.entry("username", "TEST_PERSON_USER"),
                    Map.entry("companyOrgId", companyId),
                    Map.entry("companyShortName", "梅州"),
                    Map.entry("departmentOrgId", departmentId),
                    Map.entry("teamOrgId", teamId),
                    Map.entry("employeeType", "正式"),
                    Map.entry("positionName", "工长"),
                    Map.entry("mobile", "13900008888"),
                    Map.entry("status", "ACTIVE"),
                    Map.entry("systemRoleCode", "工长"),
                    Map.entry("remark", "人员备注更新"),
                    Map.entry("certificateValidUntil", "2027-01-31"),
                    Map.entry("joinDate", "2026-06-01"),
                    Map.entry("submitDate", "2026-06-02"),
                    Map.entry("applicantName", "申请人更新")))
            .path("data");
    assertThat(updatedPersonnel.path("name").asText()).isEqualTo("测试人员更新");
    assertThat(updatedPersonnel.path("systemRoleCode").asText()).isEqualTo("工长");
    assertThat(updatedPersonnel.path("positionName").asText()).isEqualTo("工长");
    assertThat(updatedPersonnel.path("remark").asText()).isEqualTo("人员备注更新");
    assertThat(updatedPersonnel.path("certificateValidUntil").asText()).isEqualTo("2027-01-31");
    assertThat(updatedPersonnel.path("joinDate").asText()).isEqualTo("2026-06-01");
    assertThat(updatedPersonnel.path("submitDate").asText()).isEqualTo("2026-06-02");
    assertThat(updatedPersonnel.path("applicantName").asText()).isEqualTo("申请人更新");

    JsonNode missingPersonnelName =
        objectMapper.readTree(
            mockMvc
                .perform(
                    post("/api/system/personnel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of(
                                    "employeeCode",
                                    "MISSING-NAME",
                                    "companyOrgId",
                                    companyId,
                                    "submitDate",
                                    "2026-05-18",
                                    "applicantName",
                                    "管理员"))))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    assertThat(missingPersonnelName.path("message").asText()).contains("姓名");

    JsonNode missingPersonnelCode =
        objectMapper.readTree(
            mockMvc
                .perform(
                    post("/api/system/personnel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of(
                                    "name",
                                    "缺少员工编码",
                                    "companyOrgId",
                                    companyId,
                                    "submitDate",
                                    "2026-05-18",
                                    "applicantName",
                                    "管理员"))))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    assertThat(missingPersonnelCode.path("message").asText()).contains("员工编码");

    assertThat(getJson("/api/system/departments?companyOrgId=" + companyId + "&keyword=测试", token).path("data").path("total").asInt())
        .isGreaterThanOrEqualTo(1);
    assertThat(getJson("/api/system/teams?companyOrgId=" + companyId + "&keyword=测试", token).path("data").path("total").asInt())
        .isGreaterThanOrEqualTo(1);
    JsonNode listedTeam = getJson("/api/system/teams?companyOrgId=" + companyId + "&keyword=测试班组更新", token).path("data").path("items").get(0);
    assertThat(listedTeam.path("submitDate").asText()).isEqualTo("2026-05-18");
    assertThat(listedTeam.path("applicantName").asText()).isEqualTo("班组申请人");
    assertThat(getJson("/api/system/personnel?companyOrgId=" + companyId + "&keyword=测试人员更新", token).path("data").path("total").asInt())
        .isEqualTo(1);

    assertThat(patchJson("/api/system/departments/" + departmentId + "/status", token, Map.of("status", "INACTIVE")).path("data").path("status").asText())
        .isEqualTo("INACTIVE");
    assertThat(patchJson("/api/system/teams/" + teamId + "/status", token, Map.of("status", "INACTIVE")).path("data").path("status").asText())
        .isEqualTo("INACTIVE");
    assertThat(patchJson("/api/system/personnel/" + personnelId + "/status", token, Map.of("status", "INACTIVE")).path("data").path("status").asText())
        .isEqualTo("INACTIVE");

    mockMvc.perform(delete("/api/system/personnel/" + personnelId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    assertThat(jdbcTemplate.queryForObject("select deleted from sys_user where id = ?", Integer.class, personnelId))
        .isEqualTo(1);
    mockMvc.perform(delete("/api/system/teams/" + teamId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    assertThat(jdbcTemplate.queryForObject("select deleted from sys_org where id = ?", Integer.class, teamId))
        .isEqualTo(1);
    mockMvc.perform(delete("/api/system/departments/" + departmentId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    assertThat(jdbcTemplate.queryForObject("select deleted from sys_org where id = ?", Integer.class, departmentId))
        .isEqualTo(1);
    assertAudit("DEPARTMENT", "SYS_ORG", departmentId, "CREATE");
    assertAudit("DEPARTMENT", "SYS_ORG", departmentId, "UPDATE");
    assertAudit("DEPARTMENT", "SYS_ORG", departmentId, "DELETE");
    assertAudit("TEAM", "SYS_ORG", teamId, "CREATE");
    assertAudit("TEAM", "SYS_ORG", teamId, "UPDATE");
    assertAudit("TEAM", "SYS_ORG", teamId, "DELETE");
    assertAudit("PERSONNEL", "SYS_USER", personnelId, "CREATE");
    assertAudit("PERSONNEL", "SYS_USER", personnelId, "UPDATE");
    assertAudit("PERSONNEL", "SYS_USER", personnelId, "DELETE");
  }

  @Test
  void filtersMasterDataBySelectedOrganizationSubtreeFromDataMap() throws Exception {
    String token = login("admin", "123456");
    long companyId = 8L;

    JsonNode department =
        postJson(
                "/api/system/departments",
                token,
                Map.of(
                    "code",
                    "MAP-DEPT",
                    "name",
                    "地图筛选部门",
                    "companyOrgId",
                    companyId,
                    "departmentType",
                    "WORKSHOP",
                    "status",
                    "ACTIVE"))
            .path("data");
    Long departmentId = department.path("id").asLong();

    JsonNode team =
        postJson(
                "/api/system/teams",
                token,
                Map.of(
                    "code",
                    "MAP-TEAM",
                    "name",
                    "地图筛选班组",
                    "companyOrgId",
                    companyId,
                    "workshopOrgId",
                    departmentId,
                    "workTypeName",
                    "地图作业",
                    "status",
                    "ACTIVE"))
            .path("data");
    Long teamId = team.path("id").asLong();

    JsonNode personnel =
        postJson(
                "/api/system/personnel",
                token,
                Map.of(
                    "employeeCode",
                    "MAP-PERSON",
                    "name",
                    "地图筛选人员",
                    "username",
                    "MAP_PERSON",
                    "companyOrgId",
                    companyId,
                    "departmentOrgId",
                    departmentId,
                    "teamOrgId",
                    teamId,
                    "status",
                    "ACTIVE"))
            .path("data");
    Long personnelId = personnel.path("id").asLong();

    JsonNode companies = getJson("/api/system/companies?organizationId=" + companyId, token).path("data");
    assertThat(ids(companies.path("items"))).contains(companyId);

    JsonNode departments = getJson("/api/system/departments?organizationId=" + companyId, token).path("data");
    assertThat(ids(departments.path("items"))).contains(departmentId);

    JsonNode teams = getJson("/api/system/teams?organizationId=" + companyId, token).path("data");
    assertThat(ids(teams.path("items"))).contains(teamId);

    JsonNode personnelRows = getJson("/api/system/personnel?organizationId=" + companyId, token).path("data");
    assertThat(ids(personnelRows.path("items"))).contains(personnelId);

    assertThat(ids(getJson("/api/system/departments?organizationId=" + teamId, token).path("data").path("items")))
        .doesNotContain(departmentId);
    assertThat(ids(getJson("/api/system/teams?organizationId=" + departmentId, token).path("data").path("items")))
        .contains(teamId);
    assertThat(ids(getJson("/api/system/personnel?organizationId=" + departmentId, token).path("data").path("items")))
        .contains(personnelId);
  }

  @Test
  void companyManagementIncludesGroupNodesWithHierarchyProfile() throws Exception {
    String token = login("admin", "123456");

    JsonNode rows = getJson("/api/system/companies?organizationId=1&pageSize=100", token).path("data").path("items");

    JsonNode holdingGroup = findById(rows, 1L);
    assertThat(holdingGroup.path("name").asText()).isEqualTo("广晟控股集团");
    assertThat(holdingGroup.path("companyType").asText()).isEqualTo("集团");
    assertThat(holdingGroup.path("level1Name").asText()).isEqualTo("广晟控股集团");
    assertThat(holdingGroup.path("level2Name").isNull()).isTrue();

    JsonNode miningGroup = findById(rows, 2L);
    assertThat(miningGroup.path("name").asText()).isEqualTo("广晟矿业集团");
    assertThat(miningGroup.path("companyType").asText()).isEqualTo("集团");
    assertThat(miningGroup.path("level1Name").asText()).isEqualTo("广晟控股集团");
    assertThat(miningGroup.path("level2Name").asText()).isEqualTo("广晟矿业集团");

    JsonNode curtainWall = findById(rows, 3L);
    assertThat(curtainWall.path("name").asText()).isEqualTo("广晟幕墙");
    assertThat(curtainWall.path("companyType").asText()).isEqualTo("分公司");
    assertThat(curtainWall.path("level3Name").asText()).isEqualTo("广晟幕墙");

    assertThat(ids(rows))
        .containsExactlyInAnyOrder(
            1L, 2L, 3L, 4L, 8L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L, 25L,
            26L, 27L, 28L, 29L, 30L, 31L, 32L);
    assertThat(countByCompanyType(rows, "集团")).isEqualTo(2);
    assertThat(countByCompanyType(rows, "分公司")).isEqualTo(12);
    assertThat(countByCompanyType(rows, "子公司")).isEqualTo(13);

    JsonNode metallurgySchool = findById(rows, 27L);
    assertThat(metallurgySchool.path("name").asText()).isEqualTo("冶金技校");
    assertThat(metallurgySchool.path("companyType").asText()).isEqualTo("分公司");
    assertThat(metallurgySchool.path("level3Name").asText()).isEqualTo("冶金技校");
  }

  @Test
  void createsCompanyUnderHierarchyAndKeepsDataMapsAndDeletionConsistent() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/system/companies",
                token,
                Map.ofEntries(
                    Map.entry("code", "HIERARCHY-COMPANY"),
                    Map.entry("sortOrder", 77),
                    Map.entry("name", "层级新增子公司"),
                    Map.entry("shortName", "层级新增"),
                    Map.entry("status", "ACTIVE"),
                    Map.entry("companyType", "子公司"),
                    Map.entry("level1Name", "广晟控股集团"),
                    Map.entry("level2Name", "广晟矿业集团"),
                    Map.entry("level3Name", "广晟幕墙"),
                    Map.entry("level4Name", "层级新增子公司")))
            .path("data");
    Long companyId = created.path("id").asLong();

    JsonNode tree = getJson("/api/pingan/org/company-tree", token).path("data");
    assertThat(ids(findOrg(tree, "广晟幕墙").path("children"))).contains(companyId);

    JsonNode groupRows = getJson("/api/system/companies?organizationId=2&pageSize=100", token).path("data").path("items");
    assertThat(ids(groupRows)).contains(companyId);

    JsonNode parentCompanyRows =
        getJson("/api/system/companies?organizationId=3&pageSize=100", token).path("data").path("items");
    assertThat(ids(parentCompanyRows)).contains(companyId);

    mockMvc.perform(delete("/api/system/companies/" + companyId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    assertThat(jdbcTemplate.queryForObject("select deleted from sys_org where id = ?", Integer.class, companyId))
        .isEqualTo(1);
  }

  @Test
  void serializesUnsafeCompanyIdsAsJsonStringsAndDeletesByStringPathId() throws Exception {
    String token = login("admin", "123456");
    jdbcTemplate.update(
        """
        insert into sys_org (id, parent_id, org_type, org_code, org_name, org_path, sort_order, status, deleted)
        values (?, null, 'GROUP', 'JSON-UNSAFE-ID', '浏览器大整数公司', ?, 0, 'ACTIVE', 0)
        """,
        JS_UNSAFE_COMPANY_ID,
        "/" + JS_UNSAFE_COMPANY_ID + "/");
    jdbcTemplate.update(
        """
        insert into sys_company_profile (
          org_id, company_type, level1_name, created_at, updated_at, deleted
        ) values (?, '集团', '浏览器大整数公司', current_timestamp, current_timestamp, 0)
        """,
        JS_UNSAFE_COMPANY_ID);

    String body =
        mockMvc
            .perform(
                get("/api/system/companies?keyword=浏览器大整数公司")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(body).contains("\"id\":\"" + JS_UNSAFE_COMPANY_ID + "\"");
    assertThat(body).doesNotContain("\"id\":" + JS_UNSAFE_COMPANY_ID);
    assertThat(objectMapper.readTree(body).path("data").path("items").get(0).path("id").isTextual()).isTrue();

    mockMvc
        .perform(
            delete("/api/system/companies/" + JS_UNSAFE_COMPANY_ID)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    assertThat(jdbcTemplate.queryForObject("select deleted from sys_org where id = ?", Integer.class, JS_UNSAFE_COMPANY_ID))
        .isEqualTo(1);
  }

  @Test
  void createsSecondLevelGroupBranchAndSubsidiaryUnderStrictCompanyHierarchy() throws Exception {
    String token = login("admin", "123456");

    JsonNode group =
        postJson(
                "/api/system/companies",
                token,
                Map.of(
                    "code", "STRICT-GROUP",
                    "name", "严格二级集团",
                    "shortName", "严格二级集团",
                    "status", "ACTIVE",
                    "companyType", "集团",
                    "level1Name", "广晟控股集团",
                    "level2Name", "严格二级集团"))
            .path("data");
    Long groupId = group.path("id").asLong();

    JsonNode branch =
        postJson(
                "/api/system/companies",
                token,
                Map.of(
                    "code", "STRICT-BRANCH",
                    "name", "严格分公司",
                    "shortName", "严格分公司",
                    "status", "ACTIVE",
                    "companyType", "分公司",
                    "level1Name", "广晟控股集团",
                    "level2Name", "严格二级集团",
                    "level3Name", "严格分公司"))
            .path("data");
    Long branchId = branch.path("id").asLong();

    JsonNode subsidiary =
        postJson(
                "/api/system/companies",
                token,
                Map.of(
                    "code", "STRICT-SUBSIDIARY",
                    "name", "严格子公司",
                    "shortName", "严格子公司",
                    "status", "ACTIVE",
                    "companyType", "子公司",
                    "level1Name", "广晟控股集团",
                    "level2Name", "严格二级集团",
                    "level3Name", "严格分公司",
                    "level4Name", "严格子公司"))
            .path("data");
    Long subsidiaryId = subsidiary.path("id").asLong();

    assertThat(jdbcTemplate.queryForObject("select parent_id from sys_org where id = ?", Long.class, groupId))
        .isEqualTo(1L);
    assertThat(jdbcTemplate.queryForObject("select parent_id from sys_org where id = ?", Long.class, branchId))
        .isEqualTo(groupId);
    assertThat(jdbcTemplate.queryForObject("select parent_id from sys_org where id = ?", Long.class, subsidiaryId))
        .isEqualTo(branchId);

    JsonNode tree = getJson("/api/pingan/org/company-tree", token).path("data");
    assertThat(ids(findOrg(tree, "严格分公司").path("children"))).contains(subsidiaryId);
    assertThat(ids(getJson("/api/system/companies?organizationId=" + groupId + "&pageSize=100", token).path("data").path("items")))
        .contains(branchId, subsidiaryId);
    assertThat(ids(getJson("/api/system/companies?organizationId=" + branchId + "&pageSize=100", token).path("data").path("items")))
        .contains(subsidiaryId);

    mockMvc.perform(delete("/api/system/companies/" + subsidiaryId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void rejectsSubsidiaryWhenThirdLevelBranchDoesNotExist() throws Exception {
    String token = login("admin", "123456");

    JsonNode response =
        objectMapper.readTree(
            mockMvc
                .perform(
                    post("/api/system/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of(
                                    "code", "MISSING-BRANCH-SUB",
                                    "name", "找不到分公司的子公司",
                                    "status", "ACTIVE",
                                    "companyType", "子公司",
                                    "level1Name", "广晟控股集团",
                                    "level2Name", "广晟矿业集团",
                                    "level3Name", "不存在的分公司",
                                    "level4Name", "找不到分公司的子公司"))))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

    assertThat(response.path("message").asText()).contains("三级分公司");
  }

  @Test
  void rejectsCompanyCreateWhenCompanyTypeIsMissing() throws Exception {
    String token = login("admin", "123456");

    JsonNode response =
        objectMapper.readTree(
            mockMvc
                .perform(
                    post("/api/system/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of(
                                    "code", "MISSING-COMPANY-TYPE",
                                    "name", "缺少公司类型",
                                    "status", "ACTIVE"))))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

    assertThat(response.path("message").asText()).contains("公司类型");
  }

  @Test
  void rejectsSiblingOrganizationAccessForScopedUsers() throws Exception {
    String token = login("HB_MONITOR", "123456");

    mockMvc
        .perform(
            post("/api/system/departments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "code", "SIBLING-DENIED",
                            "name", "兄弟组织部门",
                            "companyOrgId", 8,
                            "departmentType", "WORKSHOP",
                            "status", "ACTIVE"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsDepartmentCreateWhenCompanyOrgIdIsNotCompanyNode() throws Exception {
    String token = login("admin", "123456");
    long existingDepartmentId = 101109L;

    JsonNode response =
        objectMapper.readTree(
            mockMvc
                .perform(
                    post("/api/system/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of(
                                    "code", "BAD-DEPT-PARENT",
                                    "name", "错误归属部门",
                                    "companyOrgId", existingDepartmentId,
                                    "departmentType", "WORKSHOP",
                                    "status", "ACTIVE"))))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

    assertThat(response.path("message").asText()).isEqualTo("所属公司必须是公司或集团");
  }

  @Test
  void rejectsDuplicateCompanyCodeWithBusinessError() throws Exception {
    String token = login("admin", "123456");

    Map<String, Object> payload =
        Map.of(
            "code", "DUP-COMPANY",
            "name", "重复编码公司",
            "companyType", "集团",
            "level1Name", "重复编码公司",
            "status", "ACTIVE");
    postJson("/api/system/companies", token, payload);

    JsonNode response =
        objectMapper.readTree(
            mockMvc
                .perform(
                    post("/api/system/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

    assertThat(response.path("message").asText()).isEqualTo("组织编码已存在");
  }

  @Test
  void adminCompanyListIgnoresDeletedOrganizationFilter() throws Exception {
    String token = login("admin", "123456");

    Long companyId =
        postJson(
                "/api/system/companies",
                token,
                Map.of(
                    "code", "DELETED-FILTER",
                    "name", "已删除筛选公司",
                    "companyType", "集团",
                    "level1Name", "已删除筛选公司",
                    "status", "ACTIVE"))
            .path("data")
            .path("id")
            .asLong();

    mockMvc.perform(delete("/api/system/companies/" + companyId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    JsonNode list = getJson("/api/system/companies?organizationId=" + companyId, token).path("data");

    assertThat(list.path("total").asInt()).isZero();
  }

  @Test
  void rejectsDeletingOrganizationThatStillHasActiveChildren() throws Exception {
    String token = login("admin", "123456");

    Long companyId =
        postJson(
                "/api/system/companies",
                token,
                Map.of(
                    "code", "PARENT-COMPANY",
                    "name", "父级公司",
                    "companyType", "集团",
                    "level1Name", "父级公司",
                    "status", "ACTIVE"))
            .path("data")
            .path("id")
            .asLong();
    postJson(
        "/api/system/departments",
        token,
        Map.of(
            "code", "PARENT-DEPT",
            "name", "父级公司下级部门",
            "companyOrgId", companyId,
            "departmentType", "WORKSHOP",
            "status", "ACTIVE"));

    JsonNode response =
        objectMapper.readTree(
            mockMvc
                .perform(delete("/api/system/companies/" + companyId).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

    assertThat(response.path("message").asText()).isEqualTo("存在下级组织，不能删除");
    assertThat(jdbcTemplate.queryForObject("select deleted from sys_org where id = ?", Integer.class, companyId))
        .isZero();
  }

  @Test
  void rejectsDeletingDepartmentThatStillHasActiveTeams() throws Exception {
    String token = login("admin", "123456");
    long companyId = 8L;

    Long departmentId =
        postJson(
                "/api/system/departments",
                token,
                Map.of(
                    "code", "PARENT-TEAM-DEPT",
                    "name", "有班组的部门",
                    "companyOrgId", companyId,
                    "departmentType", "WORKSHOP",
                    "status", "ACTIVE"))
            .path("data")
            .path("id")
            .asLong();
    postJson(
        "/api/system/teams",
        token,
        Map.of(
            "code", "PARENT-TEAM",
            "name", "部门下级班组",
            "companyOrgId", companyId,
            "workshopOrgId", departmentId,
            "workTypeName", "作业",
            "status", "ACTIVE"));

    JsonNode response =
        objectMapper.readTree(
            mockMvc
                .perform(delete("/api/system/departments/" + departmentId).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

    assertThat(response.path("message").asText()).isEqualTo("存在下级组织，不能删除");
    assertThat(jdbcTemplate.queryForObject("select deleted from sys_org where id = ?", Integer.class, departmentId))
        .isZero();
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

  private JsonNode patchJson(String url, String token, Object body) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(
                patch(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }

  private void assertAudit(String module, String targetType, Long targetId, String action) {
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from sys_access_log
                where module = ?
                  and target_type = ?
                  and target_id = ?
                  and action = ?
                """,
                Integer.class,
                module,
                targetType,
                targetId,
                action))
        .isGreaterThanOrEqualTo(1);
  }

  private List<Long> ids(JsonNode nodes) {
    return objectMapper.convertValue(
        nodes.findValues("id"),
        objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
  }

  private JsonNode findById(JsonNode items, long id) {
    for (JsonNode item : items) {
      if (item.path("id").asLong() == id) {
        return item;
      }
    }
    return objectMapper.missingNode();
  }

  private JsonNode findOrg(JsonNode items, String title) {
    for (JsonNode item : items) {
      if (title.equals(item.path("title").asText())) {
        return item;
      }
      JsonNode child = findOrg(item.path("children"), title);
      if (!child.isMissingNode()) {
        return child;
      }
    }
    return objectMapper.missingNode();
  }

  private long countByCompanyType(JsonNode items, String companyType) {
    long count = 0;
    for (JsonNode item : items) {
      if (companyType.equals(item.path("companyType").asText())) {
        count++;
      }
    }
    return count;
  }
}
