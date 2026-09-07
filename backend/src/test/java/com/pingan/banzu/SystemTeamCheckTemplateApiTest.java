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
import java.util.LinkedHashMap;
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
class SystemTeamCheckTemplateApiTest {

  private static final long COMPANY_ID = 4L;
  private static final long DEPARTMENT_ID = 101109L;
  private static final long TEAM_ID = 1011001L;

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void supportsKeySitesTemplateStage() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

    JsonNode libraryItem =
        postJson(
                "/api/system/team-check-item-templates/library",
                token,
                Map.of(
                    "riskType", "重点场所风险",
                    "checkItem", "检查重点场所安全防护是否完好",
                    "applicableStage", "KEY_SITES",
                    "defaultCheckResult", "无隐患",
                    "requireImage", true,
                    "requireVideo", false,
                    "sortOrder", 10,
                    "status", "ACTIVE"))
            .path("data");

    assertThat(libraryItem.path("applicableStage").asText()).isEqualTo("KEY_SITES");
    assertThat(libraryItem.path("applicableStageLabel").asText()).isEqualTo("重点场所检查");
  }

  @Test
  void managesLibraryItemsAndResolvesMostSpecificTemplate() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

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
                    "sortOrder", 10,
                    "status", "ACTIVE"))
            .path("data");
    assertThat(libraryItem.path("id").asLong()).isPositive();
    assertThat(libraryItem.path("applicableStageLabel").asText()).isEqualTo("班前检查");

    JsonNode libraryList =
        getJson(
                "/api/system/team-check-item-templates/library?stage=PRE_SHIFT_INSPECTION&keyword=机械&page=1&pageSize=20",
                token)
            .path("data");
    assertThat(libraryList.path("total").asInt()).isGreaterThanOrEqualTo(1);

    JsonNode companyTemplate =
        postJson(
                "/api/system/team-check-item-templates/templates",
                token,
                templatePayload(
                    "公司班前检查模板",
                    COMPANY_ID,
                    null,
                    null,
                    "PRE_SHIFT_INSPECTION",
                    "公司级风险",
                    "公司级检查项",
                    libraryItem.path("id").asLong()))
            .path("data");
    assertThat(companyTemplate.path("scope").asText()).isEqualTo("COMPANY");

    JsonNode teamTemplate =
        postJson(
                "/api/system/team-check-item-templates/templates",
                token,
                templatePayload(
                    "幕墙组装1班班前检查模板",
                    COMPANY_ID,
                    DEPARTMENT_ID,
                    TEAM_ID,
                    "PRE_SHIFT_INSPECTION",
                    "机械伤害",
                    "检查机械设备是否处于良好状态",
                    libraryItem.path("id").asLong()))
            .path("data");
    long teamTemplateId = teamTemplate.path("id").asLong();
    assertThat(teamTemplate.path("scope").asText()).isEqualTo("TEAM");
    assertThat(teamTemplate.path("items")).hasSize(1);
    assertThat(teamTemplate.path("items").get(0).path("riskType").asText()).isEqualTo("机械伤害");

    JsonNode exactCompanyTemplates =
        getJson(
                "/api/system/team-check-item-templates/templates?companyOrgId="
                    + COMPANY_ID
                    + "&stage=PRE_SHIFT_INSPECTION&status=all&exactScope=true",
                token)
            .path("data");
    assertThat(exactCompanyTemplates.path("total").asInt()).isEqualTo(1);
    assertThat(exactCompanyTemplates.path("items").get(0).path("id").asLong())
        .isEqualTo(companyTemplate.path("id").asLong());

    JsonNode resolved =
        getJson(
                "/api/system/team-check-item-templates/resolve?companyOrgId="
                    + COMPANY_ID
                    + "&departmentOrgId="
                    + DEPARTMENT_ID
                    + "&teamOrgId="
                    + TEAM_ID
                    + "&stage=PRE_SHIFT_INSPECTION",
                token)
            .path("data");
    assertThat(resolved.path("id").asLong()).isEqualTo(teamTemplateId);
    assertThat(resolved.path("scope").asText()).isEqualTo("TEAM");
    assertThat(resolved.path("items").get(0).path("checkItem").asText())
        .isEqualTo("检查机械设备是否处于良好状态");

    JsonNode resolvedWithParentCompany =
        getJson(
                "/api/system/team-check-item-templates/resolve?companyOrgId=3"
                    + "&departmentOrgId="
                    + DEPARTMENT_ID
                    + "&teamOrgId="
                    + TEAM_ID
                    + "&stage=PRE_SHIFT_INSPECTION",
                token)
            .path("data");
    assertThat(resolvedWithParentCompany.path("id").asLong()).isEqualTo(teamTemplateId);
    assertThat(resolvedWithParentCompany.path("companyOrgId").asLong()).isEqualTo(COMPANY_ID);

    JsonNode updated =
        putJson(
                "/api/system/team-check-item-templates/templates/" + teamTemplateId,
                token,
                templatePayload(
                    "幕墙组装1班班前检查模板-更新",
                    COMPANY_ID,
                    DEPARTMENT_ID,
                    TEAM_ID,
                    "PRE_SHIFT_INSPECTION",
                    "噪音伤害",
                    "检查是否佩戴听力防护用品",
                    null))
            .path("data");
    assertThat(updated.path("items")).hasSize(1);
    assertThat(updated.path("items").get(0).path("riskType").asText()).isEqualTo("噪音伤害");

    patchJson(
        "/api/system/team-check-item-templates/templates/" + teamTemplateId + "/status",
        token,
        Map.of("status", "INACTIVE"));
    JsonNode exactTeamTemplates =
        getJson(
                "/api/system/team-check-item-templates/templates?companyOrgId="
                    + COMPANY_ID
                    + "&departmentOrgId="
                    + DEPARTMENT_ID
                    + "&teamOrgId="
                    + TEAM_ID
                    + "&stage=PRE_SHIFT_INSPECTION&status=all&exactScope=true",
                token)
            .path("data");
    assertThat(exactTeamTemplates.path("total").asInt()).isEqualTo(1);
    assertThat(exactTeamTemplates.path("items").get(0).path("id").asLong()).isEqualTo(teamTemplateId);
    assertThat(exactTeamTemplates.path("items").get(0).path("status").asText()).isEqualTo("INACTIVE");

    JsonNode directOnly =
        getJson(
                "/api/system/team-check-item-templates/resolve?companyOrgId="
                    + COMPANY_ID
                    + "&departmentOrgId="
                    + DEPARTMENT_ID
                    + "&teamOrgId="
                    + TEAM_ID
                    + "&stage=PRE_SHIFT_INSPECTION",
                token)
            .path("data");
    assertThat(directOnly.isNull()).isTrue();

    JsonNode companyResolved =
        getJson(
                "/api/system/team-check-item-templates/resolve?companyOrgId="
                    + COMPANY_ID
                    + "&stage=PRE_SHIFT_INSPECTION",
                token)
            .path("data");
    assertThat(companyResolved.path("scope").asText()).isEqualTo("COMPANY");
    assertThat(companyResolved.path("items").get(0).path("riskType").asText()).isEqualTo("公司级风险");
  }

  @Test
  void allowsPostShiftTemplatesWithoutRiskType() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

    Map<String, Object> libraryPayload = new LinkedHashMap<>();
    libraryPayload.put("checkItem", "检查工具材料是否归位");
    libraryPayload.put("applicableStage", "POST_SHIFT_INSPECTION");
    libraryPayload.put("defaultCheckResult", "无隐患");
    libraryPayload.put("requireImage", false);
    libraryPayload.put("requireVideo", false);
    libraryPayload.put("sortOrder", 30);
    libraryPayload.put("status", "ACTIVE");

    JsonNode libraryItem =
        postJson("/api/system/team-check-item-templates/library", token, libraryPayload).path("data");
    assertThat(libraryItem.path("riskType").asText()).isEmpty();

    Map<String, Object> templateItem = new LinkedHashMap<>();
    templateItem.put("libraryItemId", libraryItem.path("id").asLong());
    templateItem.put("checkItem", "检查工具材料是否归位");
    templateItem.put("defaultCheckResult", "无隐患");
    templateItem.put("requireImage", false);
    templateItem.put("requireVideo", false);
    templateItem.put("sortOrder", 1);

    Map<String, Object> templatePayload = new LinkedHashMap<>();
    templatePayload.put("name", "公司班后检查模板");
    templatePayload.put("companyOrgId", COMPANY_ID);
    templatePayload.put("departmentOrgId", null);
    templatePayload.put("teamOrgId", null);
    templatePayload.put("inspectionStage", "POST_SHIFT_INSPECTION");
    templatePayload.put("status", "ACTIVE");
    templatePayload.put("items", List.of(templateItem));

    JsonNode template =
        postJson("/api/system/team-check-item-templates/templates", token, templatePayload).path("data");
    assertThat(template.path("items").get(0).path("riskType").asText()).isEmpty();
  }

  @Test
  void managesPreShiftMeetingConfirmationTemplates() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

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
    assertThat(libraryItem.path("applicableStageLabel").asText()).isEqualTo("班前会安全确认");

    JsonNode template =
        postJson(
                "/api/system/team-check-item-templates/templates",
                token,
                templatePayload(
                    "幕墙组装1班班前会安全确认模板",
                    COMPANY_ID,
                    DEPARTMENT_ID,
                    TEAM_ID,
                    "PRE_SHIFT_MEETING_CONFIRMATION",
                    "机械伤害",
                    "确认排水构筑物设计与防护能力",
                    libraryItem.path("id").asLong()))
            .path("data");

    assertThat(template.path("inspectionStageLabel").asText()).isEqualTo("班前会安全确认");
    assertThat(template.path("items")).hasSize(1);
    assertThat(template.path("items").get(0).path("riskType").asText()).isEqualTo("机械伤害");
    assertThat(template.path("items").get(0).path("checkItem").asText())
        .isEqualTo("确认排水构筑物设计与防护能力");

    JsonNode resolved =
        getJson(
                "/api/system/team-check-item-templates/resolve?companyOrgId="
                    + COMPANY_ID
                    + "&departmentOrgId="
                    + DEPARTMENT_ID
                    + "&teamOrgId="
                    + TEAM_ID
                    + "&stage=PRE_SHIFT_MEETING_CONFIRMATION",
                token)
            .path("data");
    assertThat(resolved.path("id").asLong()).isEqualTo(template.path("id").asLong());
    assertThat(resolved.path("items").get(0).path("checkItem").asText())
        .isEqualTo("确认排水构筑物设计与防护能力");

    mockMvc
        .perform(
            post("/api/system/team-check-item-templates/library")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "riskType", "",
                            "checkItem", "风险为空的班前会事项",
                            "applicableStage", "PRE_SHIFT_MEETING_CONFIRMATION",
                            "status", "ACTIVE"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deletesTemplatesSoftlyAndBlocksNonAdminUsers() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    JsonNode template =
        postJson(
                "/api/system/team-check-item-templates/templates",
                adminToken,
                templatePayload(
                    "班后检查删除模板",
                    COMPANY_ID,
                    DEPARTMENT_ID,
                    TEAM_ID,
                    "POST_SHIFT_INSPECTION",
                    "物体打击",
                    "检查作业面材料是否清理",
                    null))
            .path("data");
    long templateId = template.path("id").asLong();

    mockMvc
        .perform(delete("/api/system/team-check-item-templates/templates/" + templateId).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());

    assertThat(
            jdbcTemplate.queryForObject(
                "select deleted from sys_team_check_template where id = ?",
                Long.class,
                templateId))
        .isEqualTo(templateId);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from sys_team_check_template_item where template_id = ? and deleted = ?",
                Integer.class,
                templateId,
                templateId))
        .isEqualTo(1);

    String memberToken = login("team_member", "SAFE_TEST_PASSWORD");
    mockMvc
        .perform(
            get("/api/system/team-check-item-templates/templates")
                .header("Authorization", "Bearer " + memberToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void updatesLibraryItemsAndCopiesTemplates() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

    JsonNode libraryItem =
        postJson(
                "/api/system/team-check-item-templates/library",
                token,
                Map.of(
                    "riskType", "高处坠落",
                    "checkItem", "检查临边防护是否牢固",
                    "applicableStage", "MID_SHIFT_INSPECTION",
                    "defaultCheckResult", "无隐患",
                    "requireImage", false,
                    "requireVideo", false,
                    "sortOrder", 20,
                    "status", "ACTIVE"))
            .path("data");

    JsonNode updatedLibraryItem =
        putJson(
                "/api/system/team-check-item-templates/library/" + libraryItem.path("id").asLong(),
                token,
                Map.of(
                    "riskType", "高处坠落",
                    "checkItem", "检查安全带是否正确系挂",
                    "applicableStage", "MID_SHIFT_INSPECTION",
                    "defaultCheckResult", "无隐患",
                    "requireImage", true,
                    "requireVideo", false,
                    "sortOrder", 21,
                    "status", "ACTIVE"))
            .path("data");
    assertThat(updatedLibraryItem.path("checkItem").asText()).isEqualTo("检查安全带是否正确系挂");
    assertThat(updatedLibraryItem.path("requireImage").asBoolean()).isTrue();

    patchJson(
        "/api/system/team-check-item-templates/library/" + libraryItem.path("id").asLong() + "/status",
        token,
        Map.of("status", "INACTIVE"));

    JsonNode sourceTemplate =
        postJson(
                "/api/system/team-check-item-templates/templates",
                token,
                templatePayload(
                    "公司班中检查模板",
                    COMPANY_ID,
                    null,
                    null,
                    "MID_SHIFT_INSPECTION",
                    "高处坠落",
                    "检查安全带是否正确系挂",
                    libraryItem.path("id").asLong()))
            .path("data");
    long sourceTemplateId = sourceTemplate.path("id").asLong();

    JsonNode detail =
        getJson("/api/system/team-check-item-templates/templates/" + sourceTemplateId, token).path("data");
    assertThat(detail.path("id").asLong()).isEqualTo(sourceTemplateId);

    JsonNode copied =
        postJson(
                "/api/system/team-check-item-templates/templates/" + sourceTemplateId + "/copy",
                token,
                Map.of(
                    "name", "幕墙组装1班班中检查模板",
                    "companyOrgId", COMPANY_ID,
                    "departmentOrgId", DEPARTMENT_ID,
                    "teamOrgId", TEAM_ID,
                    "inspectionStage", "MID_SHIFT_INSPECTION",
                    "status", "ACTIVE"))
            .path("data");
    assertThat(copied.path("scope").asText()).isEqualTo("TEAM");
    assertThat(copied.path("items")).hasSize(1);
    assertThat(copied.path("items").get(0).path("checkItem").asText())
        .isEqualTo("检查安全带是否正确系挂");

    mockMvc
        .perform(
            delete("/api/system/team-check-item-templates/library/" + libraryItem.path("id").asLong())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    assertThat(
            jdbcTemplate.queryForObject(
                "select deleted from sys_check_item_library where id = ?",
                Long.class,
                libraryItem.path("id").asLong()))
        .isEqualTo(libraryItem.path("id").asLong());
  }

  private Map<String, Object> templatePayload(
      String name,
      Long companyId,
      Long departmentId,
      Long teamId,
      String stage,
      String riskType,
      String checkItem,
      Long libraryItemId) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("libraryItemId", libraryItemId);
    item.put("riskType", riskType);
    item.put("checkItem", checkItem);
    item.put("defaultCheckResult", "无隐患");
    item.put("requireImage", true);
    item.put("requireVideo", false);
    item.put("sortOrder", 1);

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("name", name);
    payload.put("companyOrgId", companyId);
    payload.put("departmentOrgId", departmentId);
    payload.put("teamOrgId", teamId);
    payload.put("inspectionStage", stage);
    payload.put("status", "ACTIVE");
    payload.put("items", List.of(item));
    return payload;
  }

  private String login(String username, String password) throws Exception {
    JsonNode response = postJson("/api/auth/login", null, Map.of("username", username, "password", password));
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
}

