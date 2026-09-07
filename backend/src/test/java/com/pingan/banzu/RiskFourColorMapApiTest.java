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
class RiskFourColorMapApiTest {

  private static final long SOURCE_COMPANY_ID = 4L;

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void maintainsFourColorMapsWithSoftDeleteAndCompanyScope() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

    JsonNode created =
        postJson(
                "/api/pingan/risk-four-color-maps",
                Map.of("name", "Demo Works厂区四色图", "companyId", SOURCE_COMPANY_ID, "remark", "厂区总图"),
                token)
            .path("data");

    long mapId = created.path("id").asLong();
    assertThat(mapId).isPositive();
    assertThat(created.path("company").asText()).isEqualTo("Demo Works Company");
    assertThat(created.path("remark").asText()).isEqualTo("厂区总图");

    JsonNode updated =
        putJson(
                "/api/pingan/risk-four-color-maps/" + mapId,
                Map.of("name", "Demo Works车间四色图", "companyId", SOURCE_COMPANY_ID, "remark", "车间底图"),
                token)
            .path("data");
    assertThat(updated.path("name").asText()).isEqualTo("Demo Works车间四色图");

    JsonNode list = getJson("/api/pingan/risk-four-color-maps?keyword=Demo Works车间", token).path("data");
    assertThat(list.path("total").asInt()).isEqualTo(1);
    assertThat(list.path("items").get(0).path("id").asLong()).isEqualTo(mapId);

    mockMvc
        .perform(delete("/api/pingan/risk-four-color-maps/" + mapId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    Integer deleted =
        jdbcTemplate.queryForObject(
            "select deleted from risk_four_color_map where id = ?", Integer.class, mapId);
    assertThat(deleted).isEqualTo(1);

    JsonNode afterDelete = getJson("/api/pingan/risk-four-color-maps?keyword=Demo Works车间", token).path("data");
    assertThat(afterDelete.path("total").asInt()).isZero();
  }

  @Test
  void uploadsImageBackgroundAndRejectsNonImageFiles() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");
    long mapId =
        postJson(
                "/api/pingan/risk-four-color-maps",
                Map.of("name", "上传四色图", "companyId", SOURCE_COMPANY_ID, "remark", "上传测试"),
                token)
            .path("data")
            .path("id")
            .asLong();

    JsonNode uploaded =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/risk-four-color-maps/" + mapId + "/background")
                            .file(new MockMultipartFile("file", "four-color.png", "image/png", tinyPng()))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");

    assertThat(uploaded.path("backgroundAttachment").path("url").asText()).contains("/api/attachments/");
    assertThat(uploaded.path("backgroundAttachment").path("contentType").asText()).isEqualTo("image/png");

    mockMvc
        .perform(
            multipart("/api/pingan/risk-four-color-maps/" + mapId + "/background")
                .file(new MockMultipartFile("file", "not-image.txt", "text/plain", "plain".getBytes(StandardCharsets.UTF_8)))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void hidesFourColorMapsOutsideCurrentUsersOrganizationScope() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    String memberToken = login("team_member", "SAFE_TEST_PASSWORD");

    postJson(
            "/api/pingan/risk-four-color-maps",
            Map.of("name", "权限四色图-单测", "companyId", SOURCE_COMPANY_ID, "remark", "权限测试"),
            adminToken)
        .path("data");

    JsonNode adminList = getJson("/api/pingan/risk-four-color-maps?keyword=权限四色图-单测", adminToken).path("data");
    assertThat(adminList.path("total").asInt()).isEqualTo(1);

    JsonNode memberList = getJson("/api/pingan/risk-four-color-maps?keyword=权限四色图-单测", memberToken).path("data");
    assertThat(memberList.path("total").asInt()).isZero();
  }

  @Test
  void rejectsFourColorMapListWhenRiskViewPermissionMissing() throws Exception {
    String token =
        loginPermissionFixtureUser(
            91_101L, "risk_map_entry_only", "ROLE_RISK_MAP_ENTRY_ONLY", "PINGAN_RISK_ENTRY");

    mockMvc
        .perform(get("/api/pingan/risk-four-color-maps").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void allowsFourColorMapViewButRejectsCreateWhenManagePermissionMissing() throws Exception {
    String token =
        loginPermissionFixtureUser(
            91_102L,
            "risk_map_view_only",
            "ROLE_RISK_MAP_VIEW_ONLY",
            "PINGAN_RISK_ENTRY",
            "PINGAN_RISK_VIEW");

    mockMvc
        .perform(get("/api/pingan/risk-four-color-maps").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/pingan/risk-four-color-maps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("name", "缺少管理权限四色图", "companyId", SOURCE_COMPANY_ID, "remark", "权限测试")))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
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
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (?, ?, '{noop}SAFE_TEST_PASSWORD', ?, 4, 'ACTIVE', 0)",
        id,
        username,
        username);
    jdbcTemplate.update("insert into sys_user_role (user_id, role_id) values (?, ?)", id, id);
    return login(username, "SAFE_TEST_PASSWORD");
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

  private byte[] tinyPng() {
    return java.util.Base64.getDecoder()
        .decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=");
  }
}


