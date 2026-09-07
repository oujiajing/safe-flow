package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class MineApiTest {

  private static final long ADMIN_POINTS_ID = 998801L;
  private static final long OTHER_POINTS_ID = 998802L;
  private static final long ADMIN_DISPATCH_ID = 998803L;
  private static final long OTHER_DISPATCH_ID = 998804L;
  private static final long EXAM_TASK_ID = 998805L;
  private static final long ADMIN_EXAM_ID = 998806L;
  private static final long OTHER_EXAM_ID = 998807L;

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void seedMineRecords() {
    jdbcTemplate.update("delete from biz_attachment where biz_type = 'USER_AVATAR'");
    jdbcTemplate.update(
        "delete from training_exam_result where id in (?, ?)", ADMIN_EXAM_ID, OTHER_EXAM_ID);
    jdbcTemplate.update("delete from training_exam_task where id = ?", EXAM_TASK_ID);
    jdbcTemplate.update(
        "delete from three_check_record where id in (?, ?, ?, ?)",
        ADMIN_POINTS_ID,
        OTHER_POINTS_ID,
        ADMIN_DISPATCH_ID,
        OTHER_DISPATCH_ID);

    insertRecord(
        ADMIN_POINTS_ID,
        "points-flow",
        "PF-MINE-ADMIN",
        1L,
        "{\"pointsReason\":\"安全学习打卡：高处作业\",\"pointsChange\":\"加分\",\"pointsQuantity\":5,\"learningContentId\":7001,\"learningTitle\":\"高处作业\"}");
    insertRecord(
        OTHER_POINTS_ID,
        "points-flow",
        "PF-MINE-OTHER",
        2L,
        "{\"pointsReason\":\"其他用户积分\",\"pointsChange\":\"加分\",\"pointsQuantity\":99}");
    insertRecord(ADMIN_DISPATCH_ID, "team-dispatch", "TD-MINE-ADMIN", 1L, "{}");
    insertRecord(OTHER_DISPATCH_ID, "team-dispatch", "TD-MINE-OTHER", 2L, "{}");

    jdbcTemplate.update(
        "insert into training_exam_task"
            + " (id, code, company_id, department_id, exam, exam_date, status, created_by, updated_by, created_at, updated_at, deleted)"
            + " values (?, 'EXAM-MINE', 5, 6, '安全生产基础考试', CURRENT_DATE, 'INACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
        EXAM_TASK_ID);
    insertExamResult(ADMIN_EXAM_ID, 1L, "系统管理员", 88);
    insertExamResult(OTHER_EXAM_ID, 2L, "韩冰", 100);
  }

  @Test
  void overviewAndRecordsAreStrictlyScopedToCurrentUser() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

    JsonNode overview = getJson("/api/mini/pingan/me/overview", token).path("data");
    assertThat(overview.path("profile").path("username").asText()).isEqualTo("admin");
    assertThat(overview.path("safetySummary").path("points").asInt()).isEqualTo(5);
    assertThat(overview.path("safetySummary").path("completedLearningCount").asInt()).isEqualTo(1);
    assertThat(overview.path("safetySummary").path("passedExamCount").asInt()).isEqualTo(1);

    JsonNode dispatch = getJson("/api/mini/pingan/me/records/dispatch", token).path("data");
    assertThat(dispatch.path("total").asInt()).isEqualTo(1);
    assertThat(dispatch.path("items").get(0).path("id").asLong()).isEqualTo(ADMIN_DISPATCH_ID);

    JsonNode learning = getJson("/api/mini/pingan/me/records/learning", token).path("data");
    assertThat(learning.path("total").asInt()).isEqualTo(1);
    assertThat(learning.path("items").get(0).path("relatedId").asLong()).isEqualTo(7001L);

    JsonNode exams = getJson("/api/mini/pingan/me/records/exam", token).path("data");
    assertThat(exams.path("total").asInt()).isEqualTo(1);
    assertThat(exams.path("items").get(0).path("id").asLong()).isEqualTo(ADMIN_EXAM_ID);
    assertThat(exams.path("items").get(0).path("statusLabel").asText()).isEqualTo("已通过");
  }

  @Test
  void rejectsUnknownRecordType() throws Exception {
    mockMvc
        .perform(
            get("/api/mini/pingan/me/records/unknown")
                .header("Authorization", "Bearer " + login("admin", "SAFE_TEST_PASSWORD")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void uploadsAndReturnsCurrentUserAvatar() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");
    MockMultipartFile file =
        new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

    JsonNode uploaded =
        objectMapper.readTree(
            mockMvc
                .perform(multipart("/api/mini/pingan/me/avatar").file(file)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8))
            .path("data");

    assertThat(uploaded.path("fileKind").asText()).isEqualTo("IMAGE");
    assertThat(uploaded.path("url").asText()).contains("/api/attachments/");
    JsonNode overview = getJson("/api/mini/pingan/me/overview", token).path("data");
    assertThat(overview.path("profile").path("avatar").asText()).isEqualTo(uploaded.path("url").asText());
  }

  private void insertRecord(Long id, String moduleKey, String recordNo, Long ownerUserId, String payload) {
    jdbcTemplate.update(
        "insert into three_check_record"
            + " (id, module_key, record_no, company_id, department_id, team_id, owner_user_id, business_date,"
            + " status, payload_json, reminder_count, version, created_by, updated_by, source_channel, created_at, updated_at, deleted)"
            + " values (?, ?, ?, 5, 6, 7, ?, CURRENT_DATE, 'OPENED', ?, 0, 0, ?, ?, 'MINI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
        id,
        moduleKey,
        recordNo,
        ownerUserId,
        payload,
        ownerUserId,
        ownerUserId);
  }

  private void insertExamResult(Long id, Long userId, String name, int score) {
    jdbcTemplate.update(
        "insert into training_exam_result"
            + " (id, code, task_id, company_id, department_id, exam_person_user_id, exam_person_name, score, exam_date,"
            + " status, created_by, updated_by, created_at, updated_at, deleted)"
            + " values (?, ?, ?, 5, 6, ?, ?, ?, CURRENT_DATE, 'EXAMED', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
        id,
        "EXAM-USER-" + id,
        EXAM_TASK_ID,
        userId,
        name,
        score);
  }

  private String login(String username, String password) throws Exception {
    JsonNode response =
        objectMapper.readTree(
            mockMvc
                .perform(
                    post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    return response.path("data").path("accessToken").asText();
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
}

