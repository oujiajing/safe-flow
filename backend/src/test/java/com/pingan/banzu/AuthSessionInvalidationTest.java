package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class AuthSessionInvalidationTest {

  private static final long USER_ID = 92_001L;
  private static final long ROLE_ID = 92_001L;

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    clean();
    jdbcTemplate.update(
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, auth_version, deleted)"
            + " values (?, 'session_fixture', '{noop}SAFE_TEST_PASSWORD', '会话测试用户', 8, 'ACTIVE', 0, 0)",
        USER_ID);
    jdbcTemplate.update(
        "insert into sys_role (id, role_code, role_name, data_scope, deleted)"
            + " values (?, 'SESSION_FIXTURE_ROLE', '会话测试角色', 'SELF', 0)",
        ROLE_ID);
    jdbcTemplate.update(
        "insert into sys_user_role (user_id, role_id) values (?, ?)", USER_ID, ROLE_ID);
  }

  @AfterEach
  void clean() {
    jdbcTemplate.update("delete from sys_user_role where user_id = ? or role_id = ?", USER_ID, ROLE_ID);
    jdbcTemplate.update("delete from sys_role where id = ?", ROLE_ID);
    jdbcTemplate.update("delete from sys_user where id = ?", USER_ID);
  }

  @Test
  void rejectsExistingTokenAfterAccountFreeze() throws Exception {
    String token = login();
    authenticated(token);
    jdbcTemplate.update("update sys_user set status = 'INACTIVE' where id = ?", USER_ID);
    rejected(token);
  }

  @Test
  void rejectsExistingTokenAfterRoleRevocation() throws Exception {
    String token = login();
    authenticated(token);
    jdbcTemplate.update("delete from sys_user_role where user_id = ?", USER_ID);
    rejected(token);
  }

  @Test
  void logoutInvalidatesThePresentedToken() throws Exception {
    String token = login();
    mockMvc
        .perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    rejected(token);
    Long version =
        jdbcTemplate.queryForObject(
            "select auth_version from sys_user where id = ?", Long.class, USER_ID);
    assertThat(version).isEqualTo(1L);
  }

  private String login() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("username", "session_fixture", "password", "SAFE_TEST_PASSWORD"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).path("data").path("accessToken").asText();
  }

  private void authenticated(String token) throws Exception {
    mockMvc
        .perform(get("/api/auth/codes").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  private void rejected(String token) throws Exception {
    mockMvc
        .perform(get("/api/auth/codes").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }
}

