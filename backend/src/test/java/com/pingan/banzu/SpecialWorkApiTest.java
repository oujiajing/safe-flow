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
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
class SpecialWorkApiTest {

  private static final long SOURCE_COMPANY_ID = 4L;

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void servesOnlyCanonicalStatusesOnTheSharedPcMiniList() throws Exception {
    String token = login("admin", "123456");

    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from special_work_record
                where status in ('PENDING', 'APPROVED')
                  and deleted = 0
                """,
                Integer.class))
        .isZero();

    JsonNode list =
        getJson("/api/pingan/special-work/records?status=all&page=1&pageSize=100", token)
            .path("data");
    assertThat(list.path("items"))
        .allSatisfy(
            item ->
                assertThat(item.path("status").asText())
                    .isIn("PENDING_APPROVAL", "IN_PROGRESS", "PENDING_ACCEPTANCE", "COMPLETED"));
  }

  @Test
  void managesSpecialWorkRecordsWithFiltersAndSoftDelete() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/special-work/records",
                specialWorkPayload("动火审批-单测", "2026-05-20 08:30:00", "PENDING_APPROVAL"),
                token)
            .path("data");

    long recordId = created.path("id").asLong();
    assertThat(recordId).isPositive();
    assertThat(created.path("company").asText()).isEqualTo("广晟源成");
    assertThat(created.path("project").asText()).isEqualTo("动火审批-单测");
    assertThat(created.path("statusLabel").asText()).isEqualTo("待审批");

    JsonNode detail = getJson("/api/pingan/special-work/records/" + recordId, token).path("data");
    assertThat(detail.path("id").asLong()).isEqualTo(recordId);
    assertThat(detail.path("workType").asText()).isEqualTo("动火作业");

    JsonNode updated =
        putJson(
                "/api/pingan/special-work/records/" + recordId,
                specialWorkPayload(
                    "动火审批-单测-更新", "2026-05-20 08:30:00", "PENDING_APPROVAL"),
                token)
            .path("data");
    assertThat(updated.path("project").asText()).isEqualTo("动火审批-单测-更新");
    assertThat(updated.path("statusLabel").asText()).isEqualTo("待审批");

    JsonNode started =
        postJson(
                "/api/pingan/special-work/records/" + recordId + "/actions/APPROVE_AND_START",
                Map.of(
                    "implementationStartTime",
                    "2026-05-22 13:00:00",
                    "safetyDisclosurePerson",
                    "安全员",
                    "guardian",
                    "监护人",
                    "disclosureReceiver",
                    "作业人员"),
                token)
            .path("data");
    assertThat(started.path("statusLabel").asText()).isEqualTo("作业中");

    JsonNode list =
        getJson(
                "/api/pingan/special-work/records?companyId="
                    + SOURCE_COMPANY_ID
                    + "&dateStart=2026-05-20&dateEnd=2026-05-20&status=IN_PROGRESS",
                token)
            .path("data");
    assertThat(list.path("total").asInt()).isEqualTo(1);
    assertThat(list.path("items").get(0).path("id").asLong()).isEqualTo(recordId);

    mockMvc
        .perform(
            post("/api/pingan/special-work/records/batch-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("ids", List.of(recordId))))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    Integer deleted =
        jdbcTemplate.queryForObject(
            "select deleted from special_work_record where id = ?", Integer.class, recordId);
    assertThat(deleted).isEqualTo(1);
  }

  @Test
  void uploadsSpecialWorkImageAndExportsTemplateHeaders() throws Exception {
    String token = login("admin", "123456");
    long recordId =
        postJson(
                "/api/pingan/special-work/records",
                specialWorkPayload(
                    "受限空间审批-单测", "2026-05-21 09:00:00", "PENDING_APPROVAL"),
                token)
            .path("data")
            .path("id")
            .asLong();

    JsonNode uploaded =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/special-work/records/" + recordId + "/image")
                            .file(new MockMultipartFile("file", "special-work.png", "image/png", tinyPng()))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");

    assertThat(uploaded.path("image").path("originalName").asText()).isEqualTo("special-work.png");
    assertThat(uploaded.path("image").path("url").asText()).contains("/api/attachments/");

    byte[] exportBytes =
        mockMvc
            .perform(
                get("/api/pingan/special-work/records/export?dateStart=2026-05-21&dateEnd=2026-05-21")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exportBytes))) {
      var sheet = workbook.getSheetAt(0);
      assertThat(sheet.getRow(0).getPhysicalNumberOfCells()).isEqualTo(16);
      assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("公司");
      assertThat(sheet.getRow(0).getCell(3).getStringCellValue()).isEqualTo("作业申请时间");
      assertThat(sheet.getRow(0).getCell(15).getStringCellValue()).isEqualTo("状态");
      assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("受限空间审批-单测");
      assertThat(sheet.getRow(1).getCell(4).getStringCellValue()).contains("special-work.png");
    }
  }

  @Test
  void deletesSingleSpecialWorkRecord() throws Exception {
    String token = login("admin", "123456");
    long recordId =
        postJson(
                "/api/pingan/special-work/records",
                specialWorkPayload(
                    "吊装作业审批-单测", "2026-05-22 10:00:00", "PENDING_APPROVAL"),
                token)
            .path("data")
            .path("id")
            .asLong();

    mockMvc
        .perform(delete("/api/pingan/special-work/records/" + recordId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    JsonNode list = getJson("/api/pingan/special-work/records?dateStart=2026-05-22&dateEnd=2026-05-22", token).path("data");
    assertThat(list.path("total").asInt()).isZero();
  }

  @Test
  void rejectsSpecialWorkListWhenViewPermissionMissing() throws Exception {
    String token =
        loginPermissionFixtureUser(
            91_201L,
            "special_work_entry_only",
            "ROLE_SPECIAL_WORK_ENTRY_ONLY",
            "PINGAN_SPECIAL_WORK_ENTRY");

    mockMvc
        .perform(get("/api/pingan/special-work/records?status=PENDING_APPROVAL").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void allowsSpecialWorkViewButRejectsCreateWhenApplyPermissionMissing() throws Exception {
    String token =
        loginPermissionFixtureUser(
            91_202L,
            "special_work_view_only",
            "ROLE_SPECIAL_WORK_VIEW_ONLY",
            "PINGAN_SPECIAL_WORK_ENTRY",
            "PINGAN_SPECIAL_WORK_VIEW");

    mockMvc
        .perform(get("/api/pingan/special-work/records?status=PENDING_APPROVAL").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/pingan/special-work/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(specialWorkPayload("缺少申请权限特殊作业", "2026-05-23 08:00:00", "PENDING_APPROVAL")))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void allowsSpecialWorkCreateWithApplyPermission() throws Exception {
    String token =
        loginPermissionFixtureUser(
            91_203L,
            "special_work_apply_only",
            "ROLE_SPECIAL_WORK_APPLY_ONLY",
            "PINGAN_SPECIAL_WORK_APPLY");

    String response = mockMvc
        .perform(
            post("/api/pingan/special-work/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(specialWorkPayload("具备申请权限特殊作业", "2026-05-24 08:00:00", "PENDING_APPROVAL")))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString(StandardCharsets.UTF_8);
    long recordId = objectMapper.readTree(response).path("data").path("id").asLong();

    mockMvc
        .perform(
            multipart("/api/pingan/special-work/records/" + recordId + "/image")
                .file(new MockMultipartFile("file", "own-special-work.png", "image/png", tinyPng()))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void rejectsSpecialWorkMaintenanceWhenReviewPermissionMissing() throws Exception {
    String adminToken = login("admin", "123456");
    long recordId =
        postJson(
                "/api/pingan/special-work/records",
                specialWorkPayload("缺少复核权限特殊作业", "2026-05-25 08:00:00", "PENDING_APPROVAL"),
                adminToken)
            .path("data")
            .path("id")
            .asLong();
    String token =
        loginPermissionFixtureUser(
            91_204L,
            "special_work_no_review",
            "ROLE_SPECIAL_WORK_NO_REVIEW",
            "PINGAN_SPECIAL_WORK_ENTRY",
            "PINGAN_SPECIAL_WORK_VIEW",
            "PINGAN_SPECIAL_WORK_APPLY");

    mockMvc
        .perform(
            put("/api/pingan/special-work/records/" + recordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(specialWorkPayload("缺少复核权限特殊作业-更新", "2026-05-25 08:00:00", "IN_PROGRESS")))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post(
                    "/api/pingan/special-work/records/"
                        + recordId
                        + "/actions/APPROVE_AND_START")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "implementationStartTime",
                            "2026-05-25 09:00:00",
                            "safetyDisclosurePerson",
                            "安全员",
                            "guardian",
                            "监护人",
                            "disclosureReceiver",
                            "作业人员")))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(delete("/api/pingan/special-work/records/" + recordId).header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            multipart("/api/pingan/special-work/records/" + recordId + "/image")
                .file(new MockMultipartFile("file", "special-work.png", "image/png", tinyPng()))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void enforcesServerDrivenSpecialWorkTransitionsAndEvidence() throws Exception {
    String token = login("admin", "123456");

    JsonNode rejectedCreate =
        postJsonBadRequest(
            "/api/pingan/special-work/records",
            specialWorkPayload("非法直达完成", "2026-05-26 08:00:00", "COMPLETED"),
            token);
    assertThat(rejectedCreate.path("code").asInt()).isEqualTo(-1);
    assertThat(rejectedCreate.path("message").asText()).contains("只能进入待审批");

    JsonNode created =
        postJson(
                "/api/pingan/special-work/records",
                specialWorkPayload("动作状态机", "2026-05-26 08:00:00", "PENDING_APPROVAL"),
                token)
            .path("data");
    long recordId = created.path("id").asLong();

    JsonNode rejectedUpdate =
        putJsonBadRequest(
            "/api/pingan/special-work/records/" + recordId,
            specialWorkPayload("动作状态机", "2026-05-26 08:00:00", "COMPLETED"),
            token);
    assertThat(rejectedUpdate.path("code").asInt()).isEqualTo(-1);
    assertThat(rejectedUpdate.path("message").asText()).contains("只能通过流程动作");

    JsonNode missingEvidence =
        postJsonBadRequest(
            "/api/pingan/special-work/records/" + recordId + "/actions/APPROVE_AND_START",
            Map.of(),
            token);
    assertThat(missingEvidence.path("code").asInt()).isEqualTo(-1);
    assertThat(missingEvidence.path("message").asText()).contains("开始时间不能为空");

    JsonNode started =
        postJson(
                "/api/pingan/special-work/records/" + recordId + "/actions/APPROVE_AND_START",
                Map.of(
                    "implementationStartTime",
                    "2026-05-26 09:00:00",
                    "safetyDisclosurePerson",
                    "安全员",
                    "guardian",
                    "监护人",
                    "disclosureReceiver",
                    "作业人员"),
                token)
            .path("data");
    assertThat(started.path("status").asText()).isEqualTo("IN_PROGRESS");

    JsonNode illegalRepeat =
        postJsonBadRequest(
            "/api/pingan/special-work/records/" + recordId + "/actions/APPROVE_AND_START",
            Map.of(),
            token);
    assertThat(illegalRepeat.path("code").asInt()).isEqualTo(-1);
    assertThat(illegalRepeat.path("message").asText()).contains("不能执行");

    JsonNode pendingAcceptance =
        postJson(
                "/api/pingan/special-work/records/"
                    + recordId
                    + "/actions/SUBMIT_ACCEPTANCE",
                Map.of("implementationEndTime", "2026-05-26 12:00:00"),
                token)
            .path("data");
    assertThat(pendingAcceptance.path("status").asText()).isEqualTo("PENDING_ACCEPTANCE");

    JsonNode completed =
        postJson(
                "/api/pingan/special-work/records/"
                    + recordId
                    + "/actions/COMPLETE_ACCEPTANCE",
                Map.of(
                    "completionAcceptor",
                    "验收人",
                    "completionAcceptanceTime",
                    "2026-05-26 12:30:00"),
                token)
            .path("data");
    assertThat(completed.path("status").asText()).isEqualTo("COMPLETED");

    JsonNode rejectedCompletedEdit =
        putJsonBadRequest(
            "/api/pingan/special-work/records/" + recordId,
            specialWorkPayload("完成后篡改", "2026-05-26 08:00:00", "COMPLETED"),
            token);
    assertThat(rejectedCompletedEdit.path("code").asInt()).isEqualTo(-1);
    assertThat(rejectedCompletedEdit.path("message").asText()).contains("不能编辑");
  }

  @Test
  void separatesApplicantApproverAndReviewerActions() throws Exception {
    String applicantToken =
        loginPermissionFixtureUser(
            91_205L,
            "special_work_applicant",
            "ROLE_SPECIAL_WORK_APPLICANT",
            "PINGAN_SPECIAL_WORK_APPLY");
    String otherApplicantToken =
        loginPermissionFixtureUser(
            91_206L,
            "special_work_other_applicant",
            "ROLE_SPECIAL_WORK_OTHER_APPLICANT",
            "PINGAN_SPECIAL_WORK_APPLY");
    String approverToken =
        loginPermissionFixtureUser(
            91_207L,
            "special_work_approver",
            "ROLE_SPECIAL_WORK_APPROVER",
            "PINGAN_SPECIAL_WORK_APPROVE");
    String reviewerToken =
        loginPermissionFixtureUser(
            91_208L,
            "special_work_reviewer",
            "ROLE_SPECIAL_WORK_REVIEWER",
            "PINGAN_SPECIAL_WORK_REVIEW");

    long recordId =
        postJson(
                "/api/pingan/special-work/records",
                specialWorkPayload(
                    "分角色状态机", "2026-05-27 08:00:00", "PENDING_APPROVAL"),
                applicantToken)
            .path("data")
            .path("id")
            .asLong();

    JsonNode started =
        postJson(
                "/api/pingan/special-work/records/" + recordId + "/actions/APPROVE_AND_START",
                Map.of(
                    "implementationStartTime",
                    "2026-05-27 09:00:00",
                    "safetyDisclosurePerson",
                    "安全员",
                    "guardian",
                    "监护人",
                    "disclosureReceiver",
                    "作业人员"),
                approverToken)
            .path("data");
    assertThat(started.path("status").asText()).isEqualTo("IN_PROGRESS");

    mockMvc
        .perform(
            post(
                    "/api/pingan/special-work/records/"
                        + recordId
                        + "/actions/SUBMIT_ACCEPTANCE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("implementationEndTime", "2026-05-27 12:00:00")))
                .header("Authorization", "Bearer " + otherApplicantToken))
        .andExpect(status().isForbidden());

    JsonNode pendingAcceptance =
        postJson(
                "/api/pingan/special-work/records/"
                    + recordId
                    + "/actions/SUBMIT_ACCEPTANCE",
                Map.of("implementationEndTime", "2026-05-27 12:00:00"),
                applicantToken)
            .path("data");
    assertThat(pendingAcceptance.path("status").asText()).isEqualTo("PENDING_ACCEPTANCE");

    JsonNode completed =
        postJson(
                "/api/pingan/special-work/records/"
                    + recordId
                    + "/actions/COMPLETE_ACCEPTANCE",
                Map.of(
                    "completionAcceptor",
                    "验收人",
                    "completionAcceptanceTime",
                    "2026-05-27 12:30:00"),
                reviewerToken)
            .path("data");
    assertThat(completed.path("status").asText()).isEqualTo("COMPLETED");
  }

  private Map<String, Object> specialWorkPayload(String project, String applicationTime, String status) {
    return Map.ofEntries(
        Map.entry("companyId", SOURCE_COMPANY_ID),
        Map.entry("project", project),
        Map.entry("workType", "动火作业"),
        Map.entry("applicationTime", applicationTime),
        Map.entry("workContent", "焊接作业"),
        Map.entry("workLocation", "源成车间一层"),
        Map.entry("riskIdentificationResult", "已辨识并落实隔离措施"),
        Map.entry("implementationStartTime", "2026-05-22 13:00:00"),
        Map.entry("implementationEndTime", "2026-05-22 18:00:00"),
        Map.entry("safetyDisclosurePerson", "安全员"),
        Map.entry("guardian", "监护人"),
        Map.entry("disclosureReceiver", "作业人员"),
        Map.entry("completionAcceptor", "验收人"),
        Map.entry("completionAcceptanceTime", "2026-05-22 18:30:00"),
        Map.entry("status", status));
  }

  private String login(String username, String password) throws Exception {
    JsonNode response =
        objectMapper.readTree(
            mockMvc
                .perform(
                    post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    return response.path("data").path("accessToken").asText();
  }

  private String loginPermissionFixtureUser(
      long id, String username, String roleCode, String... permissionCodes) throws Exception {
    jdbcTemplate.update("delete from sys_user_role where user_id = ?", id);
    jdbcTemplate.update("delete from sys_role_menu where role_id = ?", id);
    jdbcTemplate.update("delete from sys_user where id = ? or username = ?", id, username);
    jdbcTemplate.update("delete from sys_role where id = ? or role_code = ?", id, roleCode);
    jdbcTemplate.update(
        "insert into sys_role (id, role_code, role_name, data_scope) values (?, ?, ?, 'ORG_AND_CHILDREN')",
        id,
        roleCode,
        roleCode);
    for (String permissionCode : permissionCodes) {
      jdbcTemplate.update(
          """
          insert into sys_role_menu (role_id, menu_id)
          select ?, id from sys_menu where permission_code = ?
          """,
          id,
          permissionCode);
    }
    jdbcTemplate.update(
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (?, ?, '{noop}123456', ?, 4, 'ACTIVE', 0)",
        id,
        username,
        username);
    jdbcTemplate.update("insert into sys_user_role (user_id, role_id) values (?, ?)", id, id);
    return login(username, "123456");
  }

  private JsonNode getJson(String path, String token) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(get(path).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }

  private JsonNode postJson(String path, Object body, String token) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(
                post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }

  private JsonNode putJson(String path, Object body, String token) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(
                put(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }

  private JsonNode postJsonBadRequest(String path, Object body, String token) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(
                post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }

  private JsonNode putJsonBadRequest(String path, Object body, String token) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(
                put(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }

  private byte[] tinyPng() {
    return java.util.Base64.getDecoder()
        .decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=");
  }
}
