package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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
class HazardRectificationMiniApiTest {

  private static final long SOURCE_COMPANY_ID = 4L;
  private static final long SOURCE_DEPARTMENT_ID = 101109L;
  private static final long SOURCE_TEAM_ID = 1011001L;
  private static final String SOURCE_ID_PREFIX = "mini-%-closure-%";
  private static final String MANUAL_ORDER_MARKER_PREFIX = "mini-manual-closure-";
  private static final String MANUAL_ORDER_MARKER_LIKE = MANUAL_ORDER_MARKER_PREFIX + "%";
  private static final long VIEW_ONLY_USER_ID = 9621L;
  private static final long VIEW_ONLY_ROLE_ID = 9621L;
  private static final String VIEW_ONLY_USERNAME = "mini_hazard_view_only";
  private static final String VIEW_ONLY_ROLE_CODE = "MINI_HAZARD_VIEW_ONLY";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void cleanFixtureData() {
    cleanupManualOrders();
    cleanupSourceRecords();
    cleanupPermissionFixtureUser(VIEW_ONLY_USER_ID, VIEW_ONLY_ROLE_ID, VIEW_ONLY_USERNAME, VIEW_ONLY_ROLE_CODE);
  }

  @Test
  void miniQuickShotApprovalCreatesUnifiedRectificationOrderVisibleToPcEndpoint()
      throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");
    JsonNode created =
        postJson(
                "/api/mini/pingan/three-checks/quick-shot/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-07-01",
                    "sourceRecordId", "mini-quick-shot-closure-001",
                    "clientRequestId", "mini-quick-shot-closure-001",
                    "payload",
                        Map.of(
                            "hazardDescription", "移动端随手拍发现安全通道堆物",
                            "location", "一车间安全通道",
                            "photo", "/uploads/quick-shot-before.jpg")))
            .path("data");

    String recordId = created.path("id").asText();
    JsonNode approved =
        postJson(
                "/api/mini/pingan/three-checks/quick-shot/records/" + recordId + "/workflow-actions",
                token,
                Map.of(
                    "action",
                    "APPROVE",
                    "version",
                    created.path("version").asInt(),
                    "payload",
                    Map.of()))
            .path("data");

    assertThat(approved.path("status").asText()).isEqualTo("REVIEWED");

    JsonNode orders =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceType=QUICK_SHOT&sourceRecordId="
                    + recordId,
                token)
            .path("data");

    assertThat(orders.path("total").asInt()).isEqualTo(1);
    JsonNode order = orders.path("items").get(0);
    assertThat(order.path("sourceModuleKey").asText()).isEqualTo("quick-shot");
    assertThat(order.path("status").asText()).isEqualTo("PENDING_ASSIGN");
  }

  @Test
  void miniSafetyCheckSubmitCreatesUnifiedRectificationOrderVisibleToPcEndpoint()
      throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");
    JsonNode created =
        postJson(
                "/api/mini/pingan/three-checks/safety-check/records",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "ownerUserId", 2,
                    "businessDate", "2026-07-01",
                    "sourceRecordId", "mini-safety-check-closure-001",
                    "clientRequestId", "mini-safety-check-closure-001",
                    "payload",
                        Map.of(
                            "checkTheme", "移动端安全检查",
                            "checkArea", "组装线",
                            "checkItems",
                                List.of(
                                    Map.of(
                                        "riskType", "物体打击",
                                        "checkItem", "检查通道堆物",
                                        "checkResult", "有隐患",
                                        "hazardDescription", "通道堆放材料影响通行",
                                        "rectificationMeasures", "清理通道并设置标识",
                                        "rectificationResponsiblePerson", "湖贝班长",
                                        "rectificationDeadline", "2026-07-03")))))
            .path("data");

    String recordId = created.path("id").asText();
    JsonNode submitted =
        postJson(
                "/api/mini/pingan/three-checks/safety-check/records/" + recordId + "/submit",
                token,
                Map.of())
            .path("data");

    assertThat(submitted.path("status").asText()).isEqualTo("OPENED");

    JsonNode orders =
        getJson(
                "/api/pingan/hazard-rectification/orders?sourceType=SAFETY_INSPECTION&sourceRecordId="
                    + recordId,
                token)
            .path("data");

    assertThat(orders.path("total").asInt()).isEqualTo(1);
    JsonNode orderDetail =
        getJson(
                "/api/pingan/hazard-rectification/orders/"
                    + orders.path("items").get(0).path("id").asText(),
                token)
            .path("data");
    assertThat(orderDetail.path("items")).hasSize(1);
    assertThat(orderDetail.path("items").get(0).path("hazardDescription").asText())
        .contains("通道堆放材料");
  }

  @Test
  void miniCanHandleRectificationOrderActionCycleAndUploadAfterPhoto()
      throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");
    JsonNode created =
        postJson(
                "/api/pingan/hazard-rectification/orders",
                token,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "businessDate", "2026-07-01",
                    "items",
                        List.of(
                            Map.of(
                                "riskType", "高处坠落",
                                "checkItem", MANUAL_ORDER_MARKER_PREFIX + "action-cycle-检查登高平台护栏",
                                "hazardDescription", MANUAL_ORDER_MARKER_PREFIX + "action-cycle-护栏缺少警示标识",
                                "beforePhoto", "/uploads/hazard-before.jpg"))))
            .path("data");

    String orderId = created.path("id").asText();
    mockMvc
        .perform(
            post("/api/pingan/hazard-rectification/orders/" + orderId + "/actions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "action",
                            "ISSUE_RECTIFICATION",
                            "payload",
                            Map.of(
                                "rectificationResponsibleUserId",
                                2,
                                "rectificationRequirement",
                                "缺少版本的请求不能执行",
                                "rectificationDeadline",
                                "2026-07-03 18:00:00")))))
        .andExpect(status().isConflict());
    JsonNode unchanged =
        getJson("/api/pingan/hazard-rectification/orders/" + orderId, token).path("data");
    assertThat(unchanged.path("status").asText()).isEqualTo("PENDING_ASSIGN");
    assertThat(unchanged.path("version").asInt()).isEqualTo(created.path("version").asInt());
    assertThat(unchanged.path("flowLogs")).hasSize(1);

    JsonNode issued =
        postJson(
                "/api/pingan/hazard-rectification/orders/" + orderId + "/actions",
                token,
                Map.of(
                    "action", "ISSUE_RECTIFICATION",
                    "version", created.path("version").asInt(),
                    "payload",
                        Map.of(
                            "rectificationResponsibleUserId", 2,
                            "rectificationDepartmentId", SOURCE_DEPARTMENT_ID,
                            "acceptanceUserId", 3,
                            "rectificationRequirement", "补充警示标识并复查",
                            "rectificationDeadline", "2026-07-03 18:00:00")))
            .path("data");
    assertThat(issued.path("status").asText()).isEqualTo("PENDING_RECTIFY");

    mockMvc
        .perform(
            post("/api/pingan/hazard-rectification/orders/" + orderId + "/actions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "action",
                            "CANCEL",
                            "version",
                            created.path("version").asInt(),
                            "payload",
                            Map.of("cancelReason", "过期版本不得作废")))))
        .andExpect(status().isConflict());
    JsonNode afterConflict =
        getJson("/api/pingan/hazard-rectification/orders/" + orderId, token).path("data");
    assertThat(afterConflict.path("status").asText()).isEqualTo("PENDING_RECTIFY");
    assertThat(afterConflict.path("version").asInt()).isEqualTo(issued.path("version").asInt());
    assertThat(afterConflict.path("flowLogs")).hasSize(2);

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "after.jpg",
            "image/jpeg",
            new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xd9});
    JsonNode upload =
        objectMapper.readTree(
                mockMvc
                    .perform(
                        multipart(
                                "/api/pingan/hazard-rectification/orders/"
                                    + orderId
                                    + "/attachments")
                            .file(file)
                            .param("fileKind", "RECTIFICATION_AFTER_PHOTO")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");
    assertThat(upload.path("url").asText()).isNotBlank();

    JsonNode rectified =
        postJson(
                "/api/pingan/hazard-rectification/orders/" + orderId + "/actions",
                token,
                Map.of(
                    "action", "MARK_RECTIFIED",
                    "version", issued.path("version").asInt(),
                    "payload",
                        Map.of(
                            "rectificationDescription", "已完成标识补充",
                            "afterPhoto", upload.path("url").asText())))
            .path("data");
    assertThat(rectified.path("status").asText()).isEqualTo("RECTIFIED");
    assertThat(rectified.path("rectificationAfterPhoto").asText())
        .isEqualTo(upload.path("url").asText());

    JsonNode pendingAcceptance =
        postJson(
                "/api/pingan/hazard-rectification/orders/" + orderId + "/actions",
                token,
                Map.of(
                    "action", "REQUEST_ACCEPTANCE",
                    "version", rectified.path("version").asInt(),
                    "payload", Map.of("acceptanceUserId", 3, "acceptanceRemark", "申请验收")))
            .path("data");
    assertThat(pendingAcceptance.path("status").asText()).isEqualTo("PENDING_ACCEPTANCE");

    JsonNode closed =
        postJson(
                "/api/pingan/hazard-rectification/orders/" + orderId + "/actions",
                token,
                Map.of(
                    "action", "ACCEPT",
                    "version", pendingAcceptance.path("version").asInt(),
                    "payload", Map.of("acceptanceUserId", 3, "acceptanceRemark", "验收通过")))
            .path("data");
    assertThat(closed.path("status").asText()).isEqualTo("CLOSED");
    assertThat(closed.path("flowLogs")).hasSize(5);
  }

  @Test
  void miniHazardActionsRespectPermissionAndDataScope()
      throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    JsonNode created =
        postJson(
                "/api/pingan/hazard-rectification/orders",
                adminToken,
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "teamId", SOURCE_TEAM_ID,
                    "businessDate", "2026-07-01",
                    "items",
                        List.of(
                            Map.of(
                                "riskType", "机械伤害",
                                "checkItem", MANUAL_ORDER_MARKER_PREFIX + "permission-检查设备防护罩",
                                "hazardDescription", MANUAL_ORDER_MARKER_PREFIX + "permission-防护罩固定松动",
                                "beforePhoto", "/uploads/hazard-before-view-only.jpg"))))
            .path("data");
    String orderId = created.path("id").asText();

    createPermissionFixtureUser(
        VIEW_ONLY_USER_ID,
        VIEW_ONLY_ROLE_ID,
        VIEW_ONLY_USERNAME,
        "小程序隐患只读用户",
        VIEW_ONLY_ROLE_CODE,
        "PINGAN_HAZARD_RECTIFICATION_VIEW");

    String viewOnlyToken = login(VIEW_ONLY_USERNAME, "SAFE_TEST_PASSWORD");
    JsonNode detail =
        getJson("/api/pingan/hazard-rectification/orders/" + orderId, viewOnlyToken).path("data");
    assertThat(detail.path("id").asText()).isEqualTo(orderId);

    Map<String, Object> rowBefore = orderRow(orderId);
    int flowLogCountBefore = flowLogCount(orderId);
    mockMvc
        .perform(
            post("/api/pingan/hazard-rectification/orders/" + orderId + "/actions")
                .header("Authorization", "Bearer " + viewOnlyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "action", "ISSUE_RECTIFICATION",
                            "version", created.path("version").asInt(),
                            "payload",
                                Map.of(
                                    "rectificationResponsibleUserId", VIEW_ONLY_USER_ID,
                                    "rectificationRequirement", "只读用户尝试下发整改",
                                    "rectificationDeadline", "2026-07-03 18:00:00")))))
        .andExpect(status().isForbidden());

    assertThat(orderRow(orderId)).isEqualTo(rowBefore);
    assertThat(flowLogCount(orderId)).isEqualTo(flowLogCountBefore);
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
    return objectMapper.readTree(
        mockMvc
            .perform(
                post(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }

  private void cleanupSourceRecords() {
    jdbcTemplate.update(
        """
        delete from hazard_rectification_flow_log
        where order_id in (
          select id
          from hazard_rectification_order
          where source_record_id in (
            select id
            from three_check_record
            where source_record_id like ? or client_request_id like ?
          )
        )
        """,
        SOURCE_ID_PREFIX,
        SOURCE_ID_PREFIX);
    jdbcTemplate.update(
        """
        delete from hazard_rectification_order_item
        where order_id in (
          select id
          from hazard_rectification_order
          where source_record_id in (
            select id
            from three_check_record
            where source_record_id like ? or client_request_id like ?
          )
        )
        """,
        SOURCE_ID_PREFIX,
        SOURCE_ID_PREFIX);
    jdbcTemplate.update(
        """
        delete from hazard_rectification_order
        where source_record_id in (
          select id
          from three_check_record
          where source_record_id like ? or client_request_id like ?
        )
        """,
        SOURCE_ID_PREFIX,
        SOURCE_ID_PREFIX);
    jdbcTemplate.update(
        """
        delete from biz_change_history
        where biz_type in ('THREE_CHECK_RECORD', 'HAZARD_INSPECTION_RECORD', 'QUICK_SHOT')
          and biz_id in (
            select id
            from three_check_record
            where source_record_id like ? or client_request_id like ?
          )
        """,
        SOURCE_ID_PREFIX,
        SOURCE_ID_PREFIX);
    jdbcTemplate.update(
        """
        delete from biz_status_log
        where biz_id in (
          select id
          from three_check_record
          where source_record_id like ? or client_request_id like ?
        )
        """,
        SOURCE_ID_PREFIX,
        SOURCE_ID_PREFIX);
    jdbcTemplate.update(
        "delete from three_check_record where source_record_id like ? or client_request_id like ?",
        SOURCE_ID_PREFIX,
        SOURCE_ID_PREFIX);
  }

  private void cleanupManualOrders() {
    jdbcTemplate.update(
        """
        delete from biz_attachment
        where biz_type = 'HAZARD_RECTIFICATION_ORDER'
          and biz_id in (
            select distinct order_id
            from hazard_rectification_order_item
            where check_item like ? or hazard_description like ?
          )
        """,
        MANUAL_ORDER_MARKER_LIKE,
        MANUAL_ORDER_MARKER_LIKE);
    jdbcTemplate.update(
        """
        delete from hazard_rectification_flow_log
        where order_id in (
          select distinct order_id
          from hazard_rectification_order_item
          where check_item like ? or hazard_description like ?
        )
        """,
        MANUAL_ORDER_MARKER_LIKE,
        MANUAL_ORDER_MARKER_LIKE);
    jdbcTemplate.update(
        """
        delete from hazard_rectification_order
        where id in (
          select distinct order_id
          from hazard_rectification_order_item
          where check_item like ? or hazard_description like ?
        )
        """,
        MANUAL_ORDER_MARKER_LIKE,
        MANUAL_ORDER_MARKER_LIKE);
    jdbcTemplate.update(
        "delete from hazard_rectification_order_item where check_item like ? or hazard_description like ?",
        MANUAL_ORDER_MARKER_LIKE,
        MANUAL_ORDER_MARKER_LIKE);
  }

  private void createPermissionFixtureUser(
      long userId,
      long roleId,
      String username,
      String realName,
      String roleCode,
      String... permissionCodes) {
    cleanupPermissionFixtureUser(userId, roleId, username, roleCode);
    jdbcTemplate.update(
        "insert into sys_role (id, role_code, role_name, data_scope) values (?, ?, ?, 'ORG_AND_CHILDREN')",
        roleId,
        roleCode,
        realName + "角色");
    jdbcTemplate.update(
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (?, ?, '{noop}SAFE_TEST_PASSWORD', ?, ?, 'ACTIVE', 0)",
        userId,
        username,
        realName,
        SOURCE_TEAM_ID);
    jdbcTemplate.update("insert into sys_user_role (user_id, role_id) values (?, ?)", userId, roleId);
    for (String permissionCode : permissionCodes) {
      jdbcTemplate.update(
          """
          insert into sys_role_menu (role_id, menu_id)
          select ?, id
          from sys_menu
          where permission_code = ?
          """,
          roleId,
          permissionCode);
    }
  }

  private void cleanupPermissionFixtureUser(
      long userId, long roleId, String username, String roleCode) {
    jdbcTemplate.update("delete from sys_user_role where user_id = ? or role_id = ?", userId, roleId);
    jdbcTemplate.update("delete from sys_role_menu where role_id = ?", roleId);
    jdbcTemplate.update("delete from sys_role where id = ? or role_code = ?", roleId, roleCode);
    jdbcTemplate.update("delete from sys_user where id = ? or username = ?", userId, username);
  }

  private Map<String, Object> orderRow(String orderId) {
    return jdbcTemplate.queryForMap(
        """
        select status, rectification_responsible_user_id, rectification_requirement,
          rectification_deadline, version, updated_by
        from hazard_rectification_order
        where id = ?
        """,
        Long.parseLong(orderId));
  }

  private int flowLogCount(String orderId) {
    return jdbcTemplate.queryForObject(
        "select count(*) from hazard_rectification_flow_log where order_id = ?",
        Integer.class,
        Long.parseLong(orderId));
  }
}

