package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
class SafetyLedgerDocumentApiTest {

  private static final long SOURCE_COMPANY_ID = 4L;

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void createsListsAndBatchDeletesDocumentLedgersWithManagedCompanyAndWordFile() throws Exception {
    String token = login("admin", "123456");
    MockMultipartFile docx =
        new MockMultipartFile(
            "file",
            "安全规章.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new byte[] {'P', 'K', 0x03, 0x04});

    JsonNode created =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/safety-ledger/documents")
                            .file(docx)
                            .param("ledgerKey", "safety-rules")
                            .param("name", "安全生产规章制度")
                            .param("companyId", String.valueOf(SOURCE_COMPANY_ID))
                            .param("richText", "制度正文")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");

    long id = created.path("id").asLong();
    assertThat(created.path("companyId").asLong()).isEqualTo(SOURCE_COMPANY_ID);
    assertThat(created.path("company").asText()).isEqualTo("广晟源成");
    assertThat(created.path("attachment").path("fileKind").asText()).isEqualTo("DOCUMENT");
    assertThat(created.path("attachment").path("originalName").asText()).isEqualTo("安全规章.docx");

    JsonNode listed =
        getJson(
                "/api/pingan/safety-ledger/documents?ledgerKey=safety-rules&companyId="
                    + SOURCE_COMPANY_ID
                    + "&keyword=规章&page=1&pageSize=20",
                token)
            .path("data");
    assertThat(listed.path("total").asInt()).isGreaterThanOrEqualTo(1);
    assertThat(findById(listed.path("items"), id).path("richText").asText()).isEqualTo("制度正文");

    String uploadedToday = LocalDate.now().toString();
    JsonNode listedByUploadedAt =
        getJson(
                "/api/pingan/safety-ledger/documents?ledgerKey=safety-rules&uploadedStart="
                    + uploadedToday
                    + "&uploadedEnd="
                    + uploadedToday
                    + "&page=1&pageSize=20",
                token)
            .path("data");
    assertThat(findById(listedByUploadedAt.path("items"), id).isMissingNode()).isFalse();

    JsonNode futureUploadedAt =
        getJson(
                "/api/pingan/safety-ledger/documents?ledgerKey=safety-rules&uploadedStart=2999-01-01&uploadedEnd=2999-01-01&page=1&pageSize=20",
                token)
            .path("data");
    assertThat(findById(futureUploadedAt.path("items"), id).isMissingNode()).isTrue();

    postJson("/api/pingan/safety-ledger/documents/batch-delete", token, Map.of("ids", List.of(id)));
    Integer deleted =
        jdbcTemplate.queryForObject("select deleted from safety_ledger_document where id = ?", Integer.class, id);
    assertThat(deleted).isEqualTo(1);
  }

  @Test
  void updatesAndDeletesDocumentLedgerFromOperationColumnActions() throws Exception {
    String token = login("admin", "123456");
    MockMultipartFile docx =
        new MockMultipartFile(
            "file",
            "操作规程.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new byte[] {'P', 'K', 0x03, 0x04});

    JsonNode created =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/safety-ledger/documents")
                            .file(docx)
                            .param("ledgerKey", "operating-procedures")
                            .param("name", "原操作规程")
                            .param("companyId", String.valueOf(SOURCE_COMPANY_ID))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");

    long id = created.path("id").asLong();
    MockMultipartFile pdf =
        new MockMultipartFile("file", "更新规程.pdf", "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

    JsonNode updated =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/safety-ledger/documents/{id}", id)
                            .file(pdf)
                            .param("ledgerKey", "operating-procedures")
                            .param("name", "更新后的操作规程")
                            .param("companyId", String.valueOf(SOURCE_COMPANY_ID))
                            .param("richText", "更新内容")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");

    assertThat(updated.path("id").asLong()).isEqualTo(id);
    assertThat(updated.path("name").asText()).isEqualTo("更新后的操作规程");
    assertThat(updated.path("richText").asText()).isEqualTo("更新内容");
    assertThat(updated.path("attachment").path("originalName").asText()).isEqualTo("更新规程.pdf");

    mockMvc
        .perform(delete("/api/pingan/safety-ledger/documents/{id}", id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    Integer deleted =
        jdbcTemplate.queryForObject("select deleted from safety_ledger_document where id = ?", Integer.class, id);
    assertThat(deleted).isEqualTo(1);
  }

  @Test
  void supportsOrgDocumentHeadersAndRejectsNonManagedCompanyOrUnsupportedFiles() throws Exception {
    String token = login("admin", "123456");
    MockMultipartFile pdf =
        new MockMultipartFile("file", "风险文档.pdf", "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

    JsonNode created =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/safety-ledger/documents")
                            .file(pdf)
                            .param("ledgerKey", "risk-control-document")
                            .param("name", "风险分级管控文档")
                            .param("companyId", String.valueOf(SOURCE_COMPANY_ID))
                            .param("department", "安全部")
                            .param("team", "一班")
                            .param("type", "风险管控")
                            .param("date", "2026-05-29")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");

    assertThat(created.path("company").asText()).isEqualTo("广晟源成");
    assertThat(created.path("department").asText()).isEqualTo("安全部");
    assertThat(created.path("team").asText()).isEqualTo("一班");
    assertThat(created.path("type").asText()).isEqualTo("风险管控");
    assertThat(created.path("date").asText()).isEqualTo("2026-05-29");

    mockMvc
        .perform(
            multipart("/api/pingan/safety-ledger/documents")
                .file(new MockMultipartFile("file", "错误.txt", "text/plain", "bad".getBytes(StandardCharsets.UTF_8)))
                .param("ledgerKey", "safety-rules")
                .param("name", "错误文件")
                .param("companyId", String.valueOf(SOURCE_COMPANY_ID))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            multipart("/api/pingan/safety-ledger/documents")
                .file(pdf)
                .param("ledgerKey", "safety-rules")
                .param("name", "错误公司")
                .param("companyId", "999999")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void enforcesSafetyLedgerViewAndManagePermissions() throws Exception {
    String noViewToken =
        permissionToken(
            8521L,
            8521L,
            "safety_ledger_entry_only",
            "SAFETY_LEDGER_ENTRY_ONLY",
            "PINGAN_LEDGER_ENTRY");
    mockMvc
        .perform(get("/api/pingan/safety-ledger/documents").header("Authorization", "Bearer " + noViewToken))
        .andExpect(status().isForbidden());

    String viewOnlyToken =
        permissionToken(
            8522L,
            8522L,
            "safety_ledger_view_only",
            "SAFETY_LEDGER_VIEW_ONLY",
            "PINGAN_LEDGER_ENTRY",
            "PINGAN_LEDGER_VIEW");
    mockMvc
        .perform(get("/api/pingan/safety-ledger/documents").header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isOk());

    String adminToken = login("admin", "123456");
    MockMultipartFile adminDoc =
        new MockMultipartFile(
            "file",
            "权限台账.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new byte[] {'P', 'K', 0x03, 0x04});
    long id =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/safety-ledger/documents")
                            .file(adminDoc)
                            .param("ledgerKey", "safety-rules")
                            .param("name", "权限台账")
                            .param("companyId", String.valueOf(SOURCE_COMPANY_ID))
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data")
            .path("id")
            .asLong();

    MockMultipartFile viewOnlyDoc =
        new MockMultipartFile(
            "file",
            "无管理台账.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new byte[] {'P', 'K', 0x03, 0x04});
    mockMvc
        .perform(
            multipart("/api/pingan/safety-ledger/documents")
                .file(viewOnlyDoc)
                .param("ledgerKey", "safety-rules")
                .param("name", "无管理台账")
                .param("companyId", String.valueOf(SOURCE_COMPANY_ID))
                .header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/pingan/safety-ledger/documents/batch-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("ids", List.of(id))))
                .header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isForbidden());
  }

  private String login(String username, String password) throws Exception {
    JsonNode response =
        postJson("/api/auth/login", null, Map.of("username", username, "password", password));
    return response.path("data").path("accessToken").asText();
  }

  private String permissionToken(
      long userId, long roleId, String username, String roleCode, String... permissionCodes) throws Exception {
    jdbcTemplate.update("delete from sys_user_role where user_id = ? or role_id = ?", userId, roleId);
    jdbcTemplate.update("delete from sys_role_menu where role_id = ?", roleId);
    jdbcTemplate.update("delete from sys_user where id = ? or username = ?", userId, username);
    jdbcTemplate.update("delete from sys_role where id = ? or role_code = ?", roleId, roleCode);
    jdbcTemplate.update(
        "insert into sys_role (id, role_code, role_name, data_scope) values (?, ?, ?, 'ORG_AND_CHILDREN')",
        roleId,
        roleCode,
        roleCode);
    for (String permissionCode : permissionCodes) {
      jdbcTemplate.update(
          """
          insert into sys_role_menu (role_id, menu_id)
          select ?, id from sys_menu where permission_code = ?
          """,
          roleId,
          permissionCode);
    }
    jdbcTemplate.update(
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (?, ?, '{noop}123456', ?, ?, 'ACTIVE', 0)",
        userId,
        username,
        username,
        SOURCE_COMPANY_ID);
    jdbcTemplate.update("insert into sys_user_role (user_id, role_id) values (?, ?)", userId, roleId);
    return login(username, "123456");
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

  private JsonNode findById(JsonNode items, long id) {
    for (JsonNode item : items) {
      if (item.path("id").asLong() == id) {
        return item;
      }
    }
    return objectMapper.missingNode();
  }
}
