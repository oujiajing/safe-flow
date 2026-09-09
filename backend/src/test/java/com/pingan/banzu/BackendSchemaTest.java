package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@ActiveProfiles("test")
@SpringBootTest
class BackendSchemaTest {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping requestMappings;

  @Test
  void flywayConsolidatesLegacyPreShiftMeetingDataIntoThreeCheckRecords() {
    Integer meetingCount =
        jdbcTemplate.queryForObject(
            "select count(*) from three_check_record where module_key = 'pre-shift-meeting'"
                + " and source_channel = 'LEGACY_MIGRATION'",
            Integer.class);
    Integer orgCount = jdbcTemplate.queryForObject("select count(*) from sys_org", Integer.class);
    java.util.List<String> roleCodes =
        jdbcTemplate.queryForList("select role_code from sys_role order by id", String.class);

    assertThat(meetingCount).isGreaterThanOrEqualTo(3);
    assertThat(tableExists("pre_shift_meeting")).isFalse();
    assertThat(tableExists("pre_shift_meeting_attendee")).isFalse();
    assertThat(
            jdbcTemplate.queryForObject(
                "select payload_json from three_check_record where module_key = 'pre-shift-meeting'"
                    + " and source_record_id = 'pre-shift-meeting:2001'",
                String.class))
        .contains("\"meetingContent\"", "\"attendees\"");
    assertThat(orgCount).isGreaterThanOrEqualTo(10);
    assertThat(roleCodes)
        .contains(
            "ADMIN",
            "SAFETY_OFFICER",
            "TEAM_LEADER",
            "COMPANY_LEADER",
            "ENTERPRISE_LEADER",
            "DEPARTMENT_MANAGER",
            "WORKSHOP_DIRECTOR",
            "TEAM_MEMBER");
  }

  @Test
  void genericThreeCheckSchemaContainsPreShiftMeetingSyncFields() {
    assertThat(columnsOf("three_check_record"))
        .contains(
            "created_by",
            "updated_by",
            "source_channel",
            "source_record_id",
            "client_request_id",
            "client_updated_at",
            "last_synced_at");

    assertThat(columnsOf("shift_task"))
        .contains("task_type", "source_channel", "source_record_id", "client_request_id");

    assertThat(tableExists("biz_attachment")).isTrue();
    assertThat(columnsOf("biz_attachment"))
        .contains("storage_provider", "bucket_name", "object_key", "etag");
    assertThat(tableExists("biz_status_log")).isTrue();
    assertThat(tableExists("biz_remind_record")).isTrue();
  }

  @Test
  void legacyPreShiftMeetingEndpointsAreNotRegistered() {
    java.util.Set<String> paths =
        requestMappings.getHandlerMethods().keySet().stream()
            .flatMap(mapping -> mapping.getPatternValues().stream())
            .collect(java.util.stream.Collectors.toSet());

    assertThat(paths).noneMatch(path -> path.contains("/pre-shift-meetings"));
    assertThat(paths)
        .contains(
            "/api/pingan/three-checks/{moduleKey}/records",
            "/api/mini/pingan/three-checks/{moduleKey}/records");
  }

  @Test
  void systemManagementSchemaKeepsOrgUserRoleAsSharedCore() {
    assertThat(tableExists("sys_org")).isTrue();
    assertThat(tableExists("sys_user")).isTrue();
    assertThat(tableExists("sys_role")).isTrue();
    assertThat(tableExists("sys_user_role")).isTrue();
    assertThat(tableExists("sys_menu")).isTrue();
    assertThat(tableExists("sys_role_menu")).isTrue();
  }

  @Test
  void examResultSchemaSupportsIdempotentMiniSubmission() {
    assertThat(columnsOf("training_exam_result"))
        .contains(
            "submission_request_id",
            "submitted_at",
            "started_at",
            "current_question_index",
            "remaining_seconds");
    assertThat(columnsOf("training_exam_task")).contains("duration_minutes");
  }

  @Test
  void examQuestionBankSchemaSupportsStructuredCustomQuestions() {
    assertThat(columnsOf("training_exam_question_bank"))
        .contains(
            "options_json",
            "answers_json",
            "answer_explanation",
            "case_material",
            "children_json");
  }

  @Test
  void examPaperSchemaStoresReusableQuestionSnapshots() {
    assertThat(columnsOf("training_exam_paper"))
        .contains(
            "company_id",
            "department_id",
            "team_id",
            "paper_name",
            "description",
            "questions_json",
            "question_count",
            "total_score",
            "deleted");
  }

  @Test
  void examQuestionSnapshotsSupportStructuredQuestions() {
    for (String table : java.util.List.of("training_exam_question", "training_exam_result_question")) {
      assertThat(columnsOf(table))
          .contains(
              "options_json",
              "answers_json",
              "answer_explanation",
              "case_material",
              "children_json");
    }
  }

  @Test
  void userSchemaSupportsImmediateSessionInvalidation() {
    assertThat(columnsOf("sys_user")).contains("auth_version");
  }

  @Test
  void coreRelationsHaveForeignKeysAndPolymorphicPatrolViews() {
    assertThat(tableExists("business_relation_target")).isTrue();
    assertThat(tableExists("core_relation_integrity_issue")).isTrue();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.table_constraints
                where constraint_type = 'FOREIGN KEY'
                """,
                Integer.class))
        .isGreaterThanOrEqualTo(38);
  }

  private java.util.List<String> columnsOf(String tableName) {
    return jdbcTemplate.queryForList(
        """
        select column_name
        from information_schema.columns
        where table_name = ?
        """,
        String.class,
        tableName);
  }

  private boolean tableExists(String tableName) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where table_name = ?
            """,
            Integer.class,
            tableName);
    return count != null && count > 0;
  }
}
