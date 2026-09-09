package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(
    properties = {
      "pingan.monitor-center.approved-companies=3=Demo Works Company,4=Demo Works Company,24=Demo Company,28=Demo Materials",
      "pingan.monitor-center.curtain-wall-company-ids=3"
    })
class MonitorCenterApiTest {

  private static final String OVERVIEW_PATH =
      "/api/pingan/monitor-center/overview?dateStart=2026-05-30&dateEnd=2026-05-30";

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update(
        """
        delete from hazard_rectification_flow_log
        where order_id in (
          select id from hazard_rectification_order where order_no like 'MC-HRO-%'
        )
        """);
    jdbcTemplate.update(
        """
        delete from hazard_rectification_order_item
        where order_id in (
          select id from hazard_rectification_order where order_no like 'MC-HRO-%'
        )
        """);
    jdbcTemplate.update("delete from hazard_rectification_order where order_no like 'MC-HRO-%'");
    jdbcTemplate.update("delete from three_check_record where record_no like 'MC-%'");
    jdbcTemplate.update(
        "delete from training_safety_learning_content where code like 'MC-LEARN-%' or code like 'MCSIM-LEARN-%'");
    jdbcTemplate.update("delete from special_work_record where project like 'MC-SW-%'");
    jdbcTemplate.update("delete from risk_control_hazard where id between 9101 and 9106");
    jdbcTemplate.update("delete from risk_control_library where id between 9001 and 9003");

    seedThreeCheckRecord("MC-TD-001", "team-dispatch", 3, "2026-05-30", "OPENED");
    seedThreeCheckRecord("MC-TD-002", "curtain-wall-team-dispatch", 3, "2026-05-30", "ARCHIVED");
    seedThreeCheckRecord("MC-TD-003", "team-dispatch", 4, "2026-05-30", "DRAFT");
    seedThreeCheckRecord(
        "MC-TD-YC-002", "team-dispatch", 4, 101109, 1011002, "2026-05-30", "OPENED");
    seedThreeCheckRecord("MC-TD-OUT-DATE", "team-dispatch", 24, "2026-05-29", "OPENED");
    seedThreeCheckRecord("MC-TD-OUT-COMPANY", "team-dispatch", 8, "2026-05-30", "OPENED");

    seedThreeCheckRecord("MC-PRE-001", "pre-shift-meeting", 3, "2026-05-30", "OPENED");
    seedThreeCheckRecord("MC-PRE-002", "pre-shift-meeting", 3, "2026-05-30", "ARCHIVED");
    seedThreeCheckRecord("MC-PRE-003", "pre-shift-meeting", 4, "2026-05-30", "OPENED");
    seedThreeCheckRecord(
        "MC-PRE-YC-002", "pre-shift-meeting", 4, 101109, 1011002, "2026-05-30", "OPENED");
    seedThreeCheckRecord("MC-PRE-OUT-DATE", "pre-shift-meeting", 3, "2026-05-29", "OPENED");
    seedThreeCheckRecord("MC-PSI-001", "pre-shift-inspection", 3, "2026-05-30", "OPENED");
    seedThreeCheckRecord(
        "MC-PSI-YC-002", "pre-shift-inspection", 4, 101109, 1011002, "2026-05-30", "OPENED");
    seedThreeCheckRecord("MC-MSI-001", "mid-shift-inspection", 3, "2026-05-30", "OPENED");
    seedThreeCheckRecord(
        "MC-MSI-YC-002", "mid-shift-inspection", 4, 101109, 1011002, "2026-05-30", "OPENED");
    seedThreeCheckRecord(
        "MC-POST-YC-002", "post-shift-inspection", 4, 101109, 1011002, "2026-05-30", "OPENED");

    seedThreeCheckRecord("MC-HR-001", "hazard-rectification", 3, "2026-05-30", "OPENED");
    seedThreeCheckRecord("MC-HR-002", "hazard-rectification", 3, "2026-05-30", "ARCHIVED");
    seedThreeCheckRecord("MC-HR-003", "hazard-rectification", 3, "2026-05-30", "DRAFT");
    seedThreeCheckRecord("MC-HR-004", "hazard-rectification", 4, "2026-05-30", "WITHDRAWN");
    seedThreeCheckRecord("MC-HR-OUT-COMPANY", "hazard-rectification", 8, "2026-05-30", "OPENED");

    seedHazardRectificationOrder("MC-HRO-001", 3, 101109, 1011001, "2026-05-30", "CLOSED");
    seedHazardRectificationOrder("MC-HRO-002", 3, 101109, 1011001, "2026-05-30", "PENDING_RECTIFY");
    seedHazardRectificationOrder("MC-HRO-003", 3, 101109, 1011001, "2026-05-30", "RECTIFIED");
    seedHazardRectificationOrder("MC-HRO-004", 4, 101109, 1011002, "2026-05-30", "PENDING_ACCEPTANCE");
    seedHazardRectificationOrder("MC-HRO-005", 4, 101109, 1011002, "2026-05-30", "CANCELLED");
    seedHazardRectificationOrder("MC-HRO-OUT-DATE", 3, 101109, 1011001, "2026-05-29", "CLOSED");
    seedHazardRectificationOrder("MC-HRO-OUT-COMPANY", 8, 101109, 1011001, "2026-05-30", "CLOSED");

    seedRiskLibrary(9001, 3, "监控大屏幕墙风险库");
    seedRiskLibrary(9002, 4, "监控大屏Demo Works风险库");
    seedRiskLibrary(9003, 8, "监控大屏白名单外风险库");
    seedRiskHazard(9101, 9001, 3, "监控大屏重大风险", "重大风险", "高处坠落", "防护缺失");
    seedRiskHazard(9102, 9001, 3, "监控大屏较大风险", "较大风险", "起重伤害", "警戒不足");
    seedRiskHazard(9103, 9001, 3, "监控大屏一般风险", "一般风险", "机械伤害", "设备缺陷");
    seedRiskHazard(9104, 9001, 3, "监控大屏低风险", "低风险", "触电", "临电不规范");
    seedRiskHazard(9105, 9002, 4, "监控大屏Demo Works一般风险", "一般风险", "机械伤害", "设备缺陷");
    seedRiskHazard(9106, 9003, 8, "监控大屏白名单外风险", "重大风险", "车辆伤害", "外部数据");

    seedSafetyLearning("MC-LEARN-001", 3, "2026-05-30", "ACTIVE", 0);
    seedSafetyLearning("MC-LEARN-002", 3, "2026-05-30", "ACTIVE", 0);
    seedSafetyLearning("MC-LEARN-003", 4, "2026-05-30", "ACTIVE", 0);
    seedSafetyLearning("MC-LEARN-TREND", 24, "2026-05-29", "ACTIVE", 0);
    seedSafetyLearning("MC-LEARN-DRAFT", 3, "2026-05-30", "DRAFT", 0);
    seedSafetyLearning("MC-LEARN-INACTIVE", 4, "2026-05-30", "INACTIVE", 0);
    seedSafetyLearning("MC-LEARN-DELETED", 3, "2026-05-30", "ACTIVE", 1);
    seedSafetyLearning("MC-LEARN-OUT-COMPANY", 8, "2026-05-30", "ACTIVE", 0);

    seedSpecialWork("MC-SW-001", 3, "动火作业", "2026-05-30 08:30:00", "IN_PROGRESS");
    seedSpecialWork("MC-SW-002", 4, "高处作业", "2026-05-30 09:30:00", "PENDING_APPROVAL");
    seedSpecialWork("MC-SW-003", 24, "吊装作业", "2026-05-29 10:30:00", "COMPLETED");
    seedSpecialWork("MC-SW-OUT-COMPANY", 8, "临时用电", "2026-05-30 11:30:00", "IN_PROGRESS");
  }

  @Test
  void usesUnifiedHazardRectificationOrdersForStatisticsAndKeepsLegacyRecordsSeparate()
      throws Exception {
    JsonNode data = getJson(OVERVIEW_PATH, login("admin", "SAFE_TEST_PASSWORD")).path("data");

    JsonNode summary = data.path("summary");
    assertThat(summary.path("hazardTotal").asInt()).isEqualTo(4);
    assertThat(summary.path("hazardDone").asInt()).isEqualTo(1);
    assertThat(summary.path("hazardOpen").asInt()).isEqualTo(3);
    assertThat(summary.path("hazardFixedRate").asText()).isEqualTo("25.00%");

    JsonNode orderStats = data.path("hazardRectificationOrders");
    assertThat(orderStats.path("total").asInt()).isEqualTo(5);
    assertThat(orderStats.path("closed").asInt()).isEqualTo(1);
    assertThat(orderStats.path("activeOpen").asInt()).isEqualTo(3);
    assertThat(orderStats.path("cancelled").asInt()).isEqualTo(1);

    JsonNode legacyStats = data.path("legacyHazardRectificationRecords");
    assertThat(legacyStats.path("total").asInt()).isEqualTo(4);

    assertThat(data.path("hazardBars").get(0).path("done").asInt()).isEqualTo(1);
    assertThat(data.path("hazardBars").get(0).path("open").asInt()).isEqualTo(2);
    assertThat(data.path("hazardBars").get(1).path("open").asInt()).isEqualTo(1);
    assertThat(data.path("teamAnalysis").path("summary").path("openHazards").asInt()).isEqualTo(3);
  }

  @Test
  void returnsOverviewForApprovedFourCompaniesWithBusinessStatistics() throws Exception {
    JsonNode data = getJson(OVERVIEW_PATH, login("admin", "SAFE_TEST_PASSWORD")).path("data");

    assertThat(names(data.path("companies")))
        .containsExactly("Demo Works Company", "Demo Works Company", "Demo Company", "Demo Materials");
    assertThat(ids(data.path("companies"))).containsExactly(3L, 4L, 24L, 28L);
    assertThat(names(data.path("scopeOptions")))
        .contains("Demo Works Company", "Demo Works Company", "Demo Company", "Demo Materials", "幕墙组装", "幕墙组装1班");
    assertThat(data.path("dateStart").asText()).isEqualTo("2026-05-30");
    assertThat(data.path("dateEnd").asText()).isEqualTo("2026-05-30");

    JsonNode summary = data.path("summary");
    assertThat(summary.has("personCount")).isTrue();
    assertThat(summary.path("personCount").asInt()).isGreaterThanOrEqualTo(0);
    assertThat(summary.path("dispatchTotal").asInt()).isEqualTo(4);
    assertThat(summary.path("dispatchFinished").asInt()).isEqualTo(3);
    assertThat(summary.path("riskTotal").asInt()).isEqualTo(5);
    assertThat(summary.path("hazardTotal").asInt()).isEqualTo(4);
    assertThat(summary.path("hazardDone").asInt()).isEqualTo(1);
    assertThat(summary.path("hazardOpen").asInt()).isEqualTo(3);
    assertThat(summary.path("learningTotal").asInt()).isEqualTo(3);
    assertThat(summary.path("learningToday").asInt()).isEqualTo(3);
    assertThat(summary.path("learningCompletedCount").asInt()).isGreaterThanOrEqualTo(0);
    assertThat(summary.path("learningExpectedCount").asInt()).isGreaterThanOrEqualTo(0);
    assertThat(summary.path("learningCompletionRate").asText()).endsWith("%");
    assertThat(summary.path("hazardFixedRate").asText()).isEqualTo("25.00%");
    assertThat(summary.path("threeCheckRates").path("pre").asText()).isEqualTo("100.00%");
    assertThat(summary.path("threeCheckRates").path("preInspection").asText()).isEqualTo("66.67%");
    assertThat(summary.path("threeCheckRates").path("mid").asText()).isEqualTo("66.67%");
    assertThat(summary.path("threeCheckRates").path("post").asText()).isEqualTo("33.33%");

    assertThat(valueByName(data.path("dispatchBars"), "Demo Works Company")).isEqualTo(2);
    assertThat(valueByName(data.path("dispatchBars"), "Demo Works Company")).isEqualTo(2);
    assertThat(valueByName(data.path("dispatchBars"), "Demo Company")).isZero();
    assertThat(data.path("dispatchFinishedBars").get(0).path("value").asInt()).isEqualTo(2);
    assertThat(data.path("dispatchFinishedBars").get(1).path("value").asInt()).isEqualTo(1);
    assertThat(seriesValueByCompanyId(data.path("threeCheckSeries"), "班前会", 3L)).isEqualTo(2);
    assertThat(seriesValueByCompanyId(data.path("threeCheckSeries"), "班前检查", 3L)).isEqualTo(1);
    assertThat(seriesValueByCompanyId(data.path("threeCheckSeries"), "班中检查", 3L)).isEqualTo(1);
    assertThat(seriesValueByCompanyId(data.path("threeCheckSeries"), "班后检查", 3L)).isZero();
    assertThat(seriesValueByCompanyId(data.path("threeCheckSeries"), "班后检查", 4L)).isEqualTo(1);

    JsonNode curtainRisk = byName(data.path("riskControlBars"), "Demo Works Company");
    assertThat(curtainRisk.path("major").asInt()).isEqualTo(1);
    assertThat(curtainRisk.path("serious").asInt()).isEqualTo(1);
    assertThat(curtainRisk.path("normal").asInt()).isEqualTo(1);
    assertThat(curtainRisk.path("low").asInt()).isEqualTo(1);
    assertThat(byName(data.path("riskControlBars"), "Demo Works Company").path("normal").asInt()).isEqualTo(1);

    assertThat(byName(data.path("hazardRectificationPie"), "已整改隐患").path("value").asInt()).isEqualTo(1);
    assertThat(byName(data.path("hazardRectificationPie"), "未整改隐患").path("value").asInt()).isEqualTo(3);
    assertThat(byName(data.path("hazardRectificationPie"), "已作废").path("value").asInt()).isEqualTo(1);
    assertThat(data.path("hazardBars").get(0).path("done").asInt()).isEqualTo(1);
    assertThat(data.path("hazardBars").get(0).path("open").asInt()).isEqualTo(2);
    assertThat(data.path("hazardBars").get(1).path("open").asInt()).isEqualTo(1);
    assertThat(data.path("learningBars").get(0).path("value").asInt()).isEqualTo(2);
    assertThat(data.path("learningBars").get(1).path("value").asInt()).isEqualTo(1);
    assertThat(valueByName(data.path("learningBars"), "Demo Company")).isZero();
    assertThat(valueByName(data.path("learningTrend"), "2026-05-29")).isEqualTo(1);
    assertThat(valueByName(data.path("learningTrend"), "2026-05-30")).isEqualTo(3);
    assertThat(byName(data.path("dispatchTrend"), "2026-05-30").path("total").asInt()).isEqualTo(4);
    assertThat(byName(data.path("dispatchTrend"), "2026-05-30").path("finished").asInt()).isEqualTo(3);
    assertThat(valueByName(data.path("hazardStatusBars"), "已闭环")).isEqualTo(1);
    assertThat(valueByName(data.path("hazardStatusBars"), "待整改")).isEqualTo(1);
    assertThat(valueByName(data.path("hazardStatusBars"), "已整改")).isEqualTo(1);
    assertThat(valueByName(data.path("hazardStatusBars"), "待验收")).isEqualTo(1);
    assertThat(valueByName(data.path("hazardStatusBars"), "已作废")).isEqualTo(1);
    assertThat(valueByName(data.path("learningCategoryBars"), "安全学习")).isEqualTo(3);
    assertThat(valueByName(data.path("riskAccidentTypeBars"), "机械伤害")).isEqualTo(2);
    assertThat(valueByName(data.path("riskAccidentTypeBars"), "高处坠落")).isEqualTo(1);
    assertThat(valueByName(data.path("riskCauseBars"), "设备缺陷")).isEqualTo(2);
    assertThat(valueByName(data.path("specialWorkTypeBars"), "动火作业")).isEqualTo(1);
    assertThat(valueByName(data.path("specialWorkTypeBars"), "高处作业")).isEqualTo(1);
    assertThat(data.path("hazardRecordRows").get(0).path("sourceModule").asText()).isNotBlank();
    assertThat(data.path("hazardRecordRows").toString()).contains("隐患");

    String json = data.toString();
    assertThat(json).doesNotContain("Demo East Site");
    assertThat(json).doesNotContain("MC-TD-OUT-COMPANY");
    assertThat(json).doesNotContain("MC-LEARN-OUT-COMPANY");
    assertThat(json).doesNotContain("MC-SW-OUT-COMPANY");
    assertThat(json).doesNotContain("MCSIM");
    assertThat(json).doesNotContainIgnoringCase("health");
    assertThat(json).doesNotContain("健康度");
  }

  @Test
  void keepsMonitorOverviewWithinTheJdbcQueryBudget() throws Exception {
    Logger jdbcLogger = (Logger) LoggerFactory.getLogger(JdbcTemplate.class);
    Level previousLevel = jdbcLogger.getLevel();
    boolean previousAdditive = jdbcLogger.isAdditive();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    jdbcLogger.addAppender(appender);
    jdbcLogger.setLevel(Level.DEBUG);
    jdbcLogger.setAdditive(false);
    try {
      getJson(OVERVIEW_PATH, login("admin", "SAFE_TEST_PASSWORD"));
    } finally {
      jdbcLogger.detachAppender(appender);
      jdbcLogger.setLevel(previousLevel);
      jdbcLogger.setAdditive(previousAdditive);
      appender.stop();
    }

    long queryCount =
        appender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .filter(message -> message.startsWith("Executing"))
            .filter(message -> message.contains("SQL query"))
            .count();
    assertThat(queryCount).isLessThanOrEqualTo(20);
  }

  @Test
  void treatsAutoGeneratedDraftThreeCheckChildrenAsPendingInsteadOfCompleted()
      throws Exception {
    seedThreeCheckRecord("MC-AUTO-TD-001", "team-dispatch", 24, 101109, 1011001, "2026-05-30", "OPENED");
    linkAutoChild("MC-AUTO-TD-001", "MC-AUTO-PRE-001", "pre-shift-meeting", "DRAFT");
    linkAutoChild("MC-AUTO-TD-001", "MC-AUTO-PSI-001", "pre-shift-inspection", "DRAFT");
    linkAutoChild("MC-AUTO-TD-001", "MC-AUTO-MSI-001", "mid-shift-inspection", "DRAFT");
    linkAutoChild("MC-AUTO-TD-001", "MC-AUTO-POST-001", "post-shift-inspection", "DRAFT");

    JsonNode data = getJson(OVERVIEW_PATH + "&orgId=24", login("admin", "SAFE_TEST_PASSWORD")).path("data");

    assertThat(data.path("summary").path("dispatchTotal").asInt()).isEqualTo(1);
    assertThat(data.path("summary").path("dispatchFinished").asInt()).isEqualTo(1);
    assertThat(data.path("summary").path("threeCheckRates").path("pre").asText()).isEqualTo("0.00%");
    assertThat(data.path("summary").path("threeCheckRates").path("preInspection").asText()).isEqualTo("0.00%");
    assertThat(data.path("summary").path("threeCheckRates").path("mid").asText()).isEqualTo("0.00%");
    assertThat(data.path("summary").path("threeCheckRates").path("post").asText()).isEqualTo("0.00%");

    jdbcTemplate.update("update three_check_record set status = 'OPENED' where record_no = 'MC-AUTO-PRE-001'");
    JsonNode afterMeetingDone =
        getJson(OVERVIEW_PATH + "&orgId=24", login("admin", "SAFE_TEST_PASSWORD")).path("data");
    assertThat(afterMeetingDone.path("summary").path("threeCheckRates").path("pre").asText())
        .isEqualTo("100.00%");
    assertThat(afterMeetingDone.path("summary").path("threeCheckRates").path("preInspection").asText())
        .isEqualTo("0.00%");
  }

  @Test
  void excludesWithdrawnDispatchesFromThreeCheckCompletionRateDenominator()
      throws Exception {
    seedThreeCheckRecord("MC-AUTO-WITHDRAWN-TD", "team-dispatch", 24, 101109, 1011001, "2026-05-30", "WITHDRAWN");
    linkAutoChild("MC-AUTO-WITHDRAWN-TD", "MC-AUTO-WITHDRAWN-PRE", "pre-shift-meeting", "OPENED");

    JsonNode data = getJson(OVERVIEW_PATH + "&orgId=24", login("admin", "SAFE_TEST_PASSWORD")).path("data");

    assertThat(data.path("summary").path("dispatchTotal").asInt()).isEqualTo(1);
    assertThat(data.path("summary").path("dispatchFinished").asInt()).isZero();
    assertThat(data.path("summary").path("threeCheckRates").path("pre").asText()).isEqualTo("0.00%");
  }

  @Test
  void rejectsOverviewWhenUserCannotEnterMonitorCenter() throws Exception {
    mockMvc
        .perform(get(OVERVIEW_PATH).header("Authorization", "Bearer " + login("team_member", "SAFE_TEST_PASSWORD")))
        .andExpect(status().isForbidden());
  }

  @Test
  void curtainWallLeaderMonitorCenterShowsOnlyCurtainWallCompany() throws Exception {
    long userId = 9301L;
    String username = "curtain_wall_monitor_leader";
    jdbcTemplate.update("delete from sys_user_role where user_id = ?", userId);
    jdbcTemplate.update("delete from sys_user where id = ? or username = ?", userId, username);
    jdbcTemplate.update(
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (?, ?, '{noop}SAFE_TEST_PASSWORD', '幕墙看板领导', 1, 'ACTIVE', 0)",
        userId,
        username);
    jdbcTemplate.update(
        """
        insert into sys_user_role (user_id, role_id)
        select ?, id from sys_role where role_code = 'CURTAIN_WALL_LEADER'
        """,
        userId);

    JsonNode data = getJson(OVERVIEW_PATH, login(username, "SAFE_TEST_PASSWORD")).path("data");

    assertThat(names(data.path("companies"))).containsExactly("Demo Works Company");
    assertThat(ids(data.path("companies"))).containsExactly(3L);
    assertThat(names(data.path("dispatchBars"))).containsExactly("Demo Works Company");
    assertThat(names(data.path("dispatchScopeBars"))).isNotEmpty();
    assertThat(data.path("summary").path("dispatchTotal").asInt()).isEqualTo(2);
    assertThat(data.toString()).doesNotContain("Demo Company", "Demo Materials");
  }

  @Test
  void filtersOverviewBySelectedApprovedCompanyButKeepsSwitchOptions() throws Exception {
    JsonNode data =
        getJson(OVERVIEW_PATH + "&orgId=3", login("admin", "SAFE_TEST_PASSWORD")).path("data");

    assertThat(names(data.path("companies")))
        .containsExactly("Demo Works Company", "Demo Works Company", "Demo Company", "Demo Materials");
    assertThat(names(data.path("dispatchBars")))
        .contains("幕墙组装")
        .doesNotContain("Demo Works Company");
    assertThat(valueByName(data.path("dispatchBars"), "幕墙组装")).isEqualTo(2);
    assertThat(names(data.path("dispatchScopeBars")))
        .contains("幕墙组装")
        .doesNotContain("Demo Works Company");
    assertThat(valueByName(data.path("dispatchScopeBars"), "幕墙组装")).isEqualTo(2);
    assertThat(data.path("summary").path("dispatchTotal").asInt()).isEqualTo(2);
    assertThat(data.path("summary").path("dispatchFinished").asInt()).isEqualTo(2);
    assertThat(data.path("summary").path("riskTotal").asInt()).isEqualTo(4);
    assertThat(data.path("summary").path("hazardTotal").asInt()).isEqualTo(3);
    assertThat(data.path("summary").path("learningTotal").asInt()).isEqualTo(2);
    assertThat(valueByName(data.path("learningBars"), "Demo Works Company")).isEqualTo(2);
    assertThat(valueByName(data.path("riskAccidentTypeBars"), "机械伤害")).isEqualTo(1);
    assertThat(valueByName(data.path("specialWorkTypeBars"), "动火作业")).isEqualTo(1);
    assertThat(seriesValue(data.path("threeCheckSeries"), "班前会", "Demo Works Company")).isEqualTo(2);
    assertThat(data.toString()).doesNotContain("Demo Works Company一般风险");
  }

  @Test
  void filtersOverviewBySelectedDataMapDepartmentLevel() throws Exception {
    JsonNode data =
        getJson(OVERVIEW_PATH + "&orgId=101109", login("admin", "SAFE_TEST_PASSWORD")).path("data");

    assertThat(names(data.path("scopeOptions"))).contains("Demo Works Company", "幕墙组装", "幕墙组装1班");
    assertThat(names(data.path("dispatchBars")))
        .containsExactly("幕墙组装1班", "幕墙组装2班");
    assertThat(names(data.path("dispatchScopeBars")))
        .containsExactly("幕墙组装1班", "幕墙组装2班");
    assertThat(data.path("summary").path("dispatchTotal").asInt()).isEqualTo(2);
    assertThat(data.path("summary").path("dispatchFinished").asInt()).isEqualTo(1);
    assertThat(data.path("summary").path("hazardTotal").asInt()).isEqualTo(1);
    assertThat(data.path("summary").path("learningTotal").asInt()).isEqualTo(1);
    assertThat(valueByName(data.path("learningBars"), "幕墙组装")).isEqualTo(1);
    assertThat(valueByName(data.path("riskAccidentTypeBars"), "机械伤害")).isEqualTo(1);
    assertThat(valueByName(data.path("specialWorkTypeBars"), "高处作业")).isEqualTo(1);
    assertThat(seriesValue(data.path("threeCheckSeries"), "班前会", "幕墙组装")).isEqualTo(2);

    JsonNode teamAnalysis = data.path("teamAnalysis");
    assertThat(teamAnalysis.path("summary").path("teamCount").asInt()).isEqualTo(2);
    assertThat(teamAnalysis.path("summary").path("normalTeams").asInt()).isEqualTo(1);
    assertThat(teamAnalysis.path("summary").path("abnormalTeams").asInt()).isEqualTo(1);
    assertThat(teamAnalysis.path("summary").path("unfinishedThreeChecks").asInt()).isZero();
    assertThat(teamAnalysis.path("summary").path("openHazards").asInt()).isEqualTo(1);
    assertThat(teamAnalysis.path("summary").path("averageCompletionRate").asText()).isEqualTo("100.00%");
    assertThat(teamAnalysis.path("metricRankings").path("threeCheckTopTeams").get(0).path("name").asText())
        .isEqualTo("幕墙组装2班");
    assertThat(teamAnalysis.path("metricRankings").path("threeCheckTopTeams").get(0).path("value").asText())
        .isEqualTo("100.00%");
    assertThat(teamAnalysis.path("metricRankings").path("openHazardTopTeams").get(0).path("name").asText())
        .isEqualTo("幕墙组装2班");
    assertThat(teamAnalysis.path("metricRankings").path("openHazardTopTeams").get(0).path("value").asInt())
        .isEqualTo(1);
    assertThat(byName(teamAnalysis.path("heatmapRows"), "幕墙组装1班").path("values").get(0).asInt())
        .isEqualTo(100);
    assertThat(byName(teamAnalysis.path("heatmapRows"), "幕墙组装1班").path("values").get(5).asInt())
        .isZero();
    assertThat(byName(teamAnalysis.path("heatmapRows"), "幕墙组装2班").path("values").get(0).asInt())
        .isZero();
    assertThat(byName(teamAnalysis.path("heatmapRows"), "幕墙组装2班").path("values").get(5).asInt())
        .isEqualTo(100);
    JsonNode firstTeam = byName(teamAnalysis.path("detailRows"), "幕墙组装2班");
    assertThat(firstTeam.path("status").asText()).isEqualTo("异常");
    assertThat(firstTeam.path("dispatchCompletionRate").asText()).isEqualTo("100.00%");
    assertThat(firstTeam.path("threeCheckCompletionRate").asText()).isEqualTo("100.00%");
    assertThat(firstTeam.path("unfinishedThreeChecks").asInt()).isZero();
    assertThat(firstTeam.path("hazardTotal").asInt()).isEqualTo(1);
    assertThat(firstTeam.path("openHazards").asInt()).isEqualTo(1);
    assertThat(firstTeam.path("companyLearningCount").asInt()).isEqualTo(1);
    assertThat(teamAnalysis.toString()).doesNotContainIgnoringCase("health");
  }

  @Test
  void nonAdminOnlySeesApprovedCompaniesWithinDataScope() throws Exception {
    JsonNode data = getJson(OVERVIEW_PATH, login("MQ_SAFE", "SAFE_TEST_PASSWORD")).path("data");

    assertThat(names(data.path("companies"))).containsExactly("Demo Works Company");
    assertThat(ids(data.path("companies"))).containsExactly(4L);
    assertThat(valueByName(data.path("dispatchBars"), "幕墙组装")).isEqualTo(2);
    assertThat(valueByName(data.path("learningBars"), "Demo Works Company")).isEqualTo(1);
    assertThat(valueByName(data.path("riskAccidentTypeBars"), "机械伤害")).isEqualTo(1);
    assertThat(data.path("summary").path("dispatchTotal").asInt()).isEqualTo(2);
    assertThat(data.toString()).doesNotContain("Demo East Site");
  }

  @Test
  void departmentScopedMonitorCenterSeesOwnCompanyAndDepartmentTeamsOnly()
      throws Exception {
    String token =
        loginPermissionFixtureUserWithScopeAndOrg(
            9360L,
            "monitor_department_scope",
            "MONITOR_DEPARTMENT_SCOPE_ROLE",
            "ORG_AND_CHILDREN",
            101109L,
            "PINGAN_MONITOR_CENTER_ENTRY",
            "PINGAN_MONITOR_CENTER_VIEW");

    JsonNode data = getJson(OVERVIEW_PATH, token).path("data");

    assertThat(names(data.path("companies"))).containsExactly("Demo Works Company");
    assertThat(ids(data.path("companies"))).containsExactly(4L);
    assertThat(names(data.path("dispatchBars")))
        .containsExactly("幕墙组装1班", "幕墙组装2班");
    assertThat(names(data.path("dispatchScopeBars")))
        .containsExactly("幕墙组装1班", "幕墙组装2班");
    assertThat(data.path("summary").path("dispatchTotal").asInt()).isEqualTo(2);
    assertThat(data.path("teamAnalysis").path("summary").path("teamCount").asInt()).isEqualTo(2);
    assertThat(names(data.path("teamAnalysis").path("detailRows")))
        .containsExactly("幕墙组装2班", "幕墙组装1班");
    assertThat(names(data.path("scopeOptions")))
        .contains("Demo Works Company", "幕墙组装", "幕墙组装1班", "幕墙组装2班")
        .doesNotContain("Demo Company", "Demo Materials");
    assertThat(data.toString()).doesNotContain("MC-TD-001", "Demo Company", "Demo Materials");
  }

  @Test
  void rejectsOverviewWithoutMonitorCenterPermissionEvenWhenUserCanViewThreeChecks()
      throws Exception {
    String token = loginPermissionFixtureUser(
        9351L,
        "monitor_without_entry",
        "MONITOR_WITHOUT_ENTRY_ROLE",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW");

    mockMvc
        .perform(get(OVERVIEW_PATH).header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void allScopeOverviewWithoutOrgIdRequiresGlobalPermissionButSelectedOrgDoesNot()
      throws Exception {
    String token =
        loginPermissionFixtureUserWithScope(
            9353L,
            "monitor_all_without_global",
            "MONITOR_ALL_WITHOUT_GLOBAL_ROLE",
            "ALL",
            "PINGAN_MONITOR_CENTER_ENTRY",
            "PINGAN_MONITOR_CENTER_VIEW");

    mockMvc
        .perform(get(OVERVIEW_PATH).header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());

    JsonNode selectedOrgData = getJson(OVERVIEW_PATH + "&orgId=4", token).path("data");
    assertThat(names(selectedOrgData.path("dispatchBars"))).contains("幕墙组装");
    assertThat(valueByName(selectedOrgData.path("dispatchBars"), "幕墙组装")).isEqualTo(2);
    assertThat(selectedOrgData.path("summary").path("dispatchTotal").asInt()).isEqualTo(2);
  }

  @Test
  void rejectsSafetyPointsRankingWithoutPointsPermissionEvenWhenUserCanViewThreeChecks()
      throws Exception {
    String token = loginPermissionFixtureUser(
        9352L,
        "points_without_entry",
        "POINTS_WITHOUT_ENTRY_ROLE",
        "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW");

    mockMvc
        .perform(get("/api/pingan/safety-points/ranking/overview").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  private void seedThreeCheckRecord(
      String recordNo, String moduleKey, long companyId, String businessDate, String status) {
    seedThreeCheckRecord(recordNo, moduleKey, companyId, 101109, 1011001, businessDate, status);
  }

  private void seedThreeCheckRecord(
      String recordNo,
      String moduleKey,
      long companyId,
      long departmentId,
      long teamId,
      String businessDate,
      String status) {
    jdbcTemplate.update(
        """
        insert into three_check_record (
          module_key, record_no, company_id, department_id, team_id, owner_user_id,
          business_date, status, payload_json, image_check_status, video_check_status,
          created_by, updated_by, source_channel, client_request_id, deleted
        ) values (?, ?, ?, ?, ?, 2, ?, ?, '{}', '现场照片', '视频已传', 1, 1, 'PC', ?, 0)
        """,
        moduleKey,
        recordNo,
        companyId,
        departmentId,
        teamId,
        businessDate,
        status,
        "client-" + recordNo);
  }

  private void seedHazardRectificationOrder(
      String orderNo,
      long companyId,
      long departmentId,
      long teamId,
      String businessDate,
      String status) {
    jdbcTemplate.update(
        """
        insert into hazard_rectification_order (
          order_no, source_type, source_module_key, source_record_id, source_record_no,
          company_id, department_id, team_id, business_date, hazard_count, status,
          rectification_requirement, rectification_deadline, issued_by, issued_at,
          version, created_by, updated_by, deleted
        ) values (?, 'MANUAL', 'manual', null, ?, ?, ?, ?, ?, 1, ?,
          '监控中心统计口径隐患测试', timestamp '2026-06-02 18:00:00', 1,
          timestamp '2026-05-30 08:00:00', 0, 1, 1, 0)
        """,
        orderNo,
        orderNo,
        companyId,
        departmentId,
        teamId,
        businessDate,
        status);
  }

  private void linkAutoChild(String rootRecordNo, String childRecordNo, String moduleKey, String status) {
    Long rootId =
        jdbcTemplate.queryForObject(
            "select id from three_check_record where record_no = ?", Long.class, rootRecordNo);
    jdbcTemplate.update(
        """
        update three_check_record
        set root_dispatch_record_id = ?
        where record_no = ?
        """,
        rootId,
        rootRecordNo);
    seedThreeCheckRecord(childRecordNo, moduleKey, 24, 101109, 1011001, "2026-05-30", status);
    jdbcTemplate.update(
        """
        update three_check_record
        set root_dispatch_record_id = ?
        where record_no = ?
        """,
        rootId,
        childRecordNo);
  }

  private void seedRiskLibrary(long id, long companyId, String name) {
    jdbcTemplate.update(
        """
        insert into risk_control_library (id, name, company_id, created_by, updated_by, deleted)
        values (?, ?, ?, 1, 1, 0)
        """,
        id,
        name,
        companyId);
  }

  private void seedRiskHazard(
      long id,
      long libraryId,
      long companyId,
      String riskPoint,
      String riskLevel,
      String accidentType,
      String dangerSource) {
    jdbcTemplate.update(
        """
        insert into risk_control_hazard (
          id, library_id, company_id, risk_point, danger_source, risk_level,
          accident_type, risk_influence_factors, created_by, updated_by, deleted
        ) values (?, ?, ?, ?, ?, ?, ?, ?, 1, 1, 0)
        """,
        id,
        libraryId,
        companyId,
        riskPoint,
        dangerSource,
        riskLevel,
        accidentType,
        dangerSource);
  }

  private void seedSpecialWork(
      String project, long companyId, String workType, String applicationTime, String status) {
    jdbcTemplate.update(
        """
        insert into special_work_record (
          company_id, project, work_type, application_time, work_content,
          work_location, risk_identification_result, status, created_by, updated_by, deleted
        ) values (?, ?, ?, ?, '监控中心特殊作业', '监控中心作业点', '已识别作业风险', ?, 1, 1, 0)
        """,
        companyId,
        project,
        workType,
        applicationTime,
        status);
  }

  private void seedSafetyLearning(
      String code, long companyId, String learningDate, String status, int deleted) {
    jdbcTemplate.update(
        """
        insert into training_safety_learning_content (
          company_id, category, title, content, learning_date, duration_text,
          code, draft, status, created_by, updated_by, deleted
        ) values (?, '安全学习', ?, '监控中心安全学习内容', ?, '15分钟', ?, ?, ?, 1, 1, ?)
        """,
        companyId,
        "监控中心安全学习-" + code,
        learningDate,
        code,
        "DRAFT".equals(status) ? "是" : "否",
        status,
        deleted);
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
    return loginPermissionFixtureUserWithScope(
        id, username, roleCode, "ORG_AND_CHILDREN", permissionCodes);
  }

  private String loginPermissionFixtureUserWithScope(
      long id, String username, String roleCode, String dataScope, String... permissionCodes)
      throws Exception {
    return loginPermissionFixtureUserWithScopeAndOrg(
        id, username, roleCode, dataScope, 4L, permissionCodes);
  }

  private String loginPermissionFixtureUserWithScopeAndOrg(
      long id,
      String username,
      String roleCode,
      String dataScope,
      long orgId,
      String... permissionCodes)
      throws Exception {
    jdbcTemplate.update("delete from sys_user_role where user_id = ?", id);
    jdbcTemplate.update("delete from sys_role_menu where role_id = ?", id);
    jdbcTemplate.update("delete from sys_user where id = ? or username = ?", id, username);
    jdbcTemplate.update("delete from sys_role where id = ? or role_code = ?", id, roleCode);
    jdbcTemplate.update(
        "insert into sys_role (id, role_code, role_name, data_scope) values (?, ?, ?, ?)",
        id,
        roleCode,
        roleCode,
        dataScope);
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
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (?, ?, '{noop}SAFE_TEST_PASSWORD', ?, ?, 'ACTIVE', 0)",
        id,
        username,
        username,
        orgId);
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

  private List<String> names(JsonNode items) {
    return items.findValuesAsText("name");
  }

  private List<Long> ids(JsonNode items) {
    List<Long> ids = new ArrayList<>();
    for (JsonNode item : items) {
      ids.add(item.path("id").asLong());
    }
    return ids;
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

  private int seriesValueByCompanyId(JsonNode series, String seriesName, long companyId) {
    JsonNode item = byName(series, seriesName);
    JsonNode companies = item.path("companies");
    JsonNode values = item.path("data");
    for (int index = 0; index < companies.size(); index++) {
      if (companies.get(index).path("id").asLong() == companyId) {
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
}


