package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
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
class NotificationCenterApiTest {

  private static final String DEDUP_PREFIX = "notification-api-test-";
  private static final long REMINDER_MEETING_ID = 990054L;
  private static final long GENERATED_MEETING_ID = 990055L;

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void cleanNotifications() {
    jdbcTemplate.update(
        "delete from biz_notification_recipient where notification_id in"
            + " (select id from biz_notification where biz_type = 'PRE_SHIFT_MEETING' and biz_id = ?)",
        REMINDER_MEETING_ID);
    jdbcTemplate.update(
        "delete from biz_notification where biz_type = 'PRE_SHIFT_MEETING' and biz_id = ?",
        REMINDER_MEETING_ID);
    jdbcTemplate.update(
        "delete from biz_remind_record where biz_type = 'PRE_SHIFT_MEETING' and biz_id = ?",
        REMINDER_MEETING_ID);
    jdbcTemplate.update(
        "delete from biz_status_log where biz_type = 'PRE_SHIFT_MEETING' and biz_id = ?",
        REMINDER_MEETING_ID);
    jdbcTemplate.update("delete from three_check_record where id = ?", REMINDER_MEETING_ID);
    jdbcTemplate.update("delete from three_check_record where id = ?", GENERATED_MEETING_ID);
    jdbcTemplate.update(
        "delete from biz_notification_recipient where notification_id in"
            + " (select id from biz_notification where dedup_key like ?)",
        DEDUP_PREFIX + "%");
    jdbcTemplate.update("delete from biz_notification where dedup_key like ?", DEDUP_PREFIX + "%");
  }

  @Test
  void generatedPreShiftMeetingResolvesTeamFromThreeCheckRecord() throws Exception {
    jdbcTemplate.update(
        "insert into three_check_record"
            + " (id, module_key, record_no, company_id, department_id, team_id, owner_user_id, business_date,"
            + " status, reminder_count, version, created_by, updated_by, source_channel, created_at, updated_at, deleted)"
            + " values (?, 'pre-shift-meeting', 'TCR-PSM-NOTIFY-TEST', 5, 6, 7, 1, CURRENT_DATE,"
            + " 'DRAFT', 0, 0, 1, 1, 'PC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
        GENERATED_MEETING_ID);
    long notificationId =
        insertNotification(
            "ACTION", "pre-shift-meeting", "班前会待执行", "OPEN_MEETING",
            "IMPORTANT", null, "generated-meeting", 1L, null, "PENDING");
    jdbcTemplate.update(
        "update biz_notification set biz_type = 'PRE_SHIFT_MEETING', biz_id = ? where id = ?",
        GENERATED_MEETING_ID,
        notificationId);

    JsonNode message =
        getJson("/api/mini/notifications/" + notificationId, login("admin", "SAFE_TEST_PASSWORD")).path("data");
    assertThat(message.path("teamName").asText())
        .isEqualTo(jdbcTemplate.queryForObject("select org_name from sys_org where id = 7", String.class));
    assertThat(message.path("responsibleName").asText()).isEqualTo("系统管理员");
  }

  @Test
  void preShiftMeetingReminderPublishesAnActionMessageToTheOwner() throws Exception {
    jdbcTemplate.update(
        "insert into three_check_record"
            + " (id, module_key, record_no, task_id, company_id, department_id, team_id, owner_user_id,"
            + " business_date, payload_json, image_check_status, video_check_status, status, reminder_count,"
            + " version, created_by, updated_by, source_channel, created_at, updated_at, deleted)"
            + " values (?, 'pre-shift-meeting', 'BQM-NOTIFY-TEST', 1001, 5, 6, 7, 2, CURRENT_DATE,"
            + " '{\"meetingContent\":\"消息测试\"}', '现场照片', '未上传', 'OPENED', 0, 0, 1, 1,"
            + " 'PC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
        REMINDER_MEETING_ID);
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");

    postEmpty(
        "/api/pingan/three-checks/pre-shift-meeting/records/"
            + REMINDER_MEETING_ID
            + "/remind",
        adminToken);

    String ownerToken = login("HB_MONITOR", "SAFE_TEST_PASSWORD");
    JsonNode data =
        getJson(
                "/api/mini/notifications?groupType=ACTION&moduleKey=pre-shift-meeting&unread=true",
                ownerToken)
            .path("data");
    assertThat(data.path("total").asInt()).isEqualTo(1);
    JsonNode message = data.path("items").get(0);
    assertThat(message.path("bizId").asLong()).isEqualTo(REMINDER_MEETING_ID);
    assertThat(message.path("title").asText()).contains("被催办");
    assertThat(message.path("actionKey").asText()).isEqualTo("OPEN_MEETING");
    assertThat(message.path("teamName").asText())
        .isEqualTo(jdbcTemplate.queryForObject("select org_name from sys_org where id = 7", String.class));
    assertThat(message.path("responsibleName").asText())
        .isEqualTo(jdbcTemplate.queryForObject("select real_name from sys_user where id = 2", String.class));
  }

  @Test
  void listsOnlyCurrentUserNotificationsAndReturnsSummaryCounts() throws Exception {
    long overdueId =
        insertNotification(
            "ACTION", "hazard-rectification", "设备通道隐患待你验收", "ACCEPT",
            "URGENT", LocalDateTime.now().minusHours(2), "overdue", 1L, null, "PENDING");
    insertNotification(
        "BUSINESS", "special-work", "动火作业申请已通过", "VIEW",
        "NORMAL", null, "business", 1L, null, "NONE");
    insertNotification(
        "SYSTEM", "system", "平台维护通知", null,
        "NORMAL", null, "system", 1L, LocalDateTime.now(), "NONE");
    insertNotification(
        "ACTION", "pre-shift-inspection", "班前检查待执行", "OPEN_INSPECTION",
        "IMPORTANT", LocalDateTime.now().plusHours(2), "other-user", 2L, null, "PENDING");

    String token = login("admin", "SAFE_TEST_PASSWORD");
    JsonNode list =
        getJson("/api/mini/notifications?groupType=ACTION&unread=true&page=1&pageSize=20", token)
            .path("data");

    assertThat(list.path("total").asInt()).isEqualTo(1);
    assertThat(list.path("items").get(0).path("id").asLong()).isEqualTo(overdueId);
    assertThat(list.path("items").get(0).path("moduleName").asText()).isEqualTo("隐患整改");
    assertThat(list.path("items").get(0).path("actionLabel").asText()).isEqualTo("去验收");
    assertThat(list.path("items").get(0).path("overdue").asBoolean()).isTrue();

    JsonNode counts = getJson("/api/mini/notifications/unread-counts", token).path("data");
    assertThat(counts.path("pending").asInt()).isEqualTo(1);
    assertThat(counts.path("unread").asInt()).isEqualTo(2);
    assertThat(counts.path("overdue").asInt()).isEqualTo(1);
    assertThat(counts.path("groupUnreadCounts").path("ACTION").asInt()).isEqualTo(1);
    assertThat(counts.path("groupUnreadCounts").path("BUSINESS").asInt()).isEqualTo(1);
  }

  @Test
  void filtersNotificationsByCreatedDate() throws Exception {
    long todayId =
        insertNotification(
            "BUSINESS", "special-work", "今日消息", "VIEW",
            "NORMAL", null, "date-today", 1L, null, "NONE");
    long earlierId =
        insertNotification(
            "BUSINESS", "special-work", "历史消息", "VIEW",
            "NORMAL", null, "date-earlier", 1L, null, "NONE");
    jdbcTemplate.update(
        "update biz_notification set created_at = ? where id = ?",
        LocalDateTime.now().minusDays(2),
        earlierId);

    String date = LocalDate.now().toString();
    JsonNode data =
        getJson(
                "/api/mini/notifications?groupType=BUSINESS&dateStart=" + date + "&dateEnd=" + date,
                login("admin", "SAFE_TEST_PASSWORD"))
            .path("data");

    assertThat(data.path("total").asInt()).isEqualTo(1);
    assertThat(data.path("items").get(0).path("id").asLong()).isEqualTo(todayId);
  }

  @Test
  void supportsReadUnreadAndFilteredReadAllWithoutCrossUserWrites() throws Exception {
    long first =
        insertNotification(
            "ACTION", "pre-shift-inspection", "班前检查待执行", "OPEN_INSPECTION",
            "IMPORTANT", null, "read-state-1", 1L, null, "PENDING");
    long second =
        insertNotification(
            "BUSINESS", "special-work", "特殊作业状态更新", "VIEW",
            "NORMAL", null, "read-state-2", 1L, null, "NONE");
    long otherUser =
        insertNotification(
            "ACTION", "pre-shift-inspection", "其他用户待办", "OPEN_INSPECTION",
            "IMPORTANT", null, "read-state-other", 2L, null, "PENDING");
    String token = login("admin", "SAFE_TEST_PASSWORD");

    postEmpty("/api/mini/notifications/" + first + "/read", token);
    assertThat(getJson("/api/mini/notifications/" + first, token).path("data").path("read").asBoolean())
        .isTrue();
    postEmpty("/api/mini/notifications/" + first + "/unread", token);
    assertThat(getJson("/api/mini/notifications/" + first, token).path("data").path("read").asBoolean())
        .isFalse();

    JsonNode readAll = postEmpty("/api/mini/notifications/read-all?groupType=ACTION", token);
    assertThat(readAll.path("data").asInt()).isEqualTo(1);
    assertThat(getJson("/api/mini/notifications/" + first, token).path("data").path("read").asBoolean())
        .isTrue();
    assertThat(getJson("/api/mini/notifications/" + second, token).path("data").path("read").asBoolean())
        .isFalse();
    assertThat(
            jdbcTemplate.queryForObject(
                "select read_at is null from biz_notification_recipient where notification_id = ? and recipient_user_id = 2",
                Boolean.class,
                otherUser))
        .isTrue();
  }

  private long insertNotification(
      String groupType,
      String moduleKey,
      String title,
      String actionKey,
      String severity,
      LocalDateTime deadline,
      String suffix,
      Long recipientUserId,
      LocalDateTime readAt,
      String handlingStatus) {
    String dedup = DEDUP_PREFIX + suffix;
    LocalDateTime now = LocalDateTime.now();
    jdbcTemplate.update(
        "insert into biz_notification"
            + " (event_type, group_type, module_key, title, summary, biz_type, biz_id, severity, action_key, deadline, dedup_key, triggered_by, created_at, updated_at)"
            + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        "TEST_EVENT",
        groupType,
        moduleKey,
        title,
        "测试消息摘要",
        "TEST_BIZ",
        Math.abs(suffix.hashCode()) + 1L,
        severity,
        actionKey,
        deadline,
        dedup,
        1L,
        now,
        now);
    Long id =
        jdbcTemplate.queryForObject(
            "select id from biz_notification where dedup_key = ?", Long.class, dedup);
    jdbcTemplate.update(
        "insert into biz_notification_recipient"
            + " (notification_id, recipient_user_id, read_at, handling_status, archived, created_at, updated_at)"
            + " values (?, ?, ?, ?, 0, ?, ?)",
        id,
        recipientUserId,
        readAt,
        handlingStatus,
        now,
        now);
    return id;
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

  private JsonNode postEmpty(String path, String token) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(post(path).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }
}

