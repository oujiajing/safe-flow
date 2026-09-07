package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class TrainingExamApiTest {

  private static final long SOURCE_COMPANY_ID = 4L;
  private static final long SOURCE_DEPARTMENT_ID = 101105L;
  private static final long SECOND_EXAM_PERSON_USER_ID = 10005L;
  private static final long EXAM_PERSON_USER_ID = 10006L;

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void expandsExamOrganizationScopeAndRestrictsApplicableQuestionBankItems()
      throws Exception {
    String token = login("admin", "123456");
    long teamId =
        jdbcTemplate.queryForObject(
            """
            select team.id
            from sys_user user_account
            join sys_org team on team.id = user_account.org_id
            where user_account.username = 'HB_MONITOR'
              and team.org_type = 'TEAM'
            """,
            Long.class);
    long departmentId =
        jdbcTemplate.queryForObject(
            """
            select department.id
            from sys_org team
            join sys_org department
              on department.org_type = 'DEPARTMENT'
             and team.org_path like concat(department.org_path, '%')
            where team.id = ?
            order by length(department.org_path) desc
            limit 1
            """,
            Long.class,
            teamId);
    long companyId =
        jdbcTemplate.queryForObject(
            """
            select company.id
            from sys_org team
            join sys_org company
              on company.org_type = 'COMPANY'
             and team.org_path like concat(company.org_path, '%')
            where team.id = ?
            order by length(company.org_path) desc
            limit 1
            """,
            Long.class,
            teamId);
    long companyTaskId =
        createAutomaticScopeTask(token, "公司范围自动考试", companyId, null, null);
    long departmentTaskId =
        createAutomaticScopeTask(
            token, "部门范围自动考试", companyId, departmentId, null);
    long teamTaskId =
        createAutomaticScopeTask(
            token, "班组范围自动考试", companyId, departmentId, teamId);

    assertThat(taskResultCount(companyTaskId))
        .isEqualTo(activePersonCountForScope(companyId, "COMPANY"));
    assertThat(taskResultCount(departmentTaskId))
        .isEqualTo(activePersonCountForScope(departmentId, "DEPARTMENT"));
    assertThat(taskResultCount(teamTaskId))
        .isEqualTo(activePersonCountForScope(teamId, "TEAM"));

    JsonNode teamTask =
        getJson("/api/pingan/training/exam-tasks/" + teamTaskId, token).path("data");
    assertThat(teamTask.path("teamId").asLong()).isEqualTo(teamId);
    assertThat(teamTask.path("team").asText()).isNotBlank();

    String marker = "SCOPE-VISIBILITY-" + teamTaskId;
    Map<String, Object> companyQuestion =
        questionBankPayload(
            "SINGLE_CHOICE", marker + "-COMPANY", "", "A、B、C、D", "A", 1, 0);
    companyQuestion.put("companyId", companyId);
    companyQuestion.remove("departmentId");
    postJson("/api/pingan/training/exam-question-bank", companyQuestion, token);

    Map<String, Object> departmentQuestion = new HashMap<>(companyQuestion);
    departmentQuestion.put("questionText", marker + "-DEPARTMENT");
    departmentQuestion.put("departmentId", departmentId);
    postJson("/api/pingan/training/exam-question-bank", departmentQuestion, token);

    Map<String, Object> teamQuestion = new HashMap<>(departmentQuestion);
    teamQuestion.put("questionText", marker + "-TEAM");
    teamQuestion.put("teamId", teamId);
    postJson("/api/pingan/training/exam-question-bank", teamQuestion, token);

    assertThat(
            getJson(
                    "/api/pingan/training/exam-question-bank?applicable=true&companyId="
                        + companyId
                        + "&keyword="
                        + marker,
                    token)
                .path("data")
                .path("total")
                .asInt())
        .isEqualTo(1);
    assertThat(
            getJson(
                    "/api/pingan/training/exam-question-bank?applicable=true&companyId="
                        + companyId
                        + "&departmentId="
                        + departmentId
                        + "&keyword="
                        + marker,
                    token)
                .path("data")
                .path("total")
                .asInt())
        .isEqualTo(2);
    JsonNode teamApplicable =
        getJson(
                "/api/pingan/training/exam-question-bank?applicable=true&companyId="
                    + companyId
                    + "&departmentId="
                    + departmentId
                    + "&teamId="
                    + teamId
                    + "&keyword="
                    + marker,
                token)
            .path("data");
    assertThat(teamApplicable.path("total").asInt()).isEqualTo(3);

    String memberToken = login("HB_MONITOR", "123456");
    JsonNode memberExams =
        getJson("/api/mini/pingan/training/exams?status=PENDING_EXAM", memberToken)
            .path("data");
    assertThat(memberExams.path("items").toString()).contains("班组范围自动考试");
  }

  @Test
  void createsExamTaskWithQuestionsAndGeneratedPersonDetails() throws Exception {
    String token = login("admin", "123456");

    JsonNode task =
        postJson(
                "/api/pingan/training/exam-tasks",
                taskPayload(
                    "安全月考试-单测",
                    "2026-05-22",
                    "ACTIVE",
                    List.of(SECOND_EXAM_PERSON_USER_ID, EXAM_PERSON_USER_ID),
                    questionPayloads()),
                token)
            .path("data");
    long taskId = task.path("id").asLong();
    assertThat(task.path("company").asText()).isEqualTo("广晟源成");
    assertThat(task.path("department").asText()).isEqualTo("安全质量职卫部");
    assertThat(task.path("code").asText()).startsWith("EXAM-TASK-202605-");
    assertThat(task.path("remark").asText()).isEqualTo("安全管理部上半年安全知识考试");
    assertThat(task.path("statusLabel").asText()).isEqualTo("已生效");

    JsonNode taskList =
        getJson(
                "/api/pingan/training/exam-tasks?companyId="
                    + SOURCE_COMPANY_ID
                    + "&departmentId="
                    + SOURCE_DEPARTMENT_ID
                    + "&dateStart=2026-05-22&dateEnd=2026-05-22&status=ACTIVE",
                token)
            .path("data");
    assertThat(taskList.path("total").asInt()).isEqualTo(1);
    assertThat(taskList.path("items").get(0).path("id").asLong()).isEqualTo(taskId);

    JsonNode taskDetail = getJson("/api/pingan/training/exam-tasks/" + taskId, token).path("data");
    assertThat(taskDetail.path("code").asText()).startsWith("EXAM-TASK-202605-");
    assertThat(taskDetail.path("remark").asText()).isEqualTo("安全管理部上半年安全知识考试");
    assertThat(taskDetail.path("questions")).hasSize(2);
    assertThat(taskDetail.path("questions").get(0).path("questionText").asText()).isEqualTo("安全生产的方针是？");
    assertThat(taskDetail.path("results")).hasSize(2);
    assertThat(taskDetail.path("results").get(0).path("code").asText()).startsWith("EXAM-USER-202605-");

    JsonNode detailResults =
        getJson("/api/pingan/training/exam-tasks/" + taskId + "/results", token).path("data");
    assertThat(detailResults.path("total").asInt()).isEqualTo(2);
    long resultId = detailResults.path("items").get(0).path("id").asLong();

    JsonNode resultDetail = getJson("/api/pingan/training/exam-results/" + resultId, token).path("data");
    assertThat(resultDetail.path("code").asText()).startsWith("EXAM-USER-202605-");
    assertThat(resultDetail.path("exam").asText()).isEqualTo("安全月考试-单测");
    assertThat(resultDetail.path("score").asDouble()).isEqualTo(0.0);
    assertThat(resultDetail.path("questions")).hasSize(2);
    assertThat(resultDetail.path("questions").get(1).path("actualScore").asDouble()).isEqualTo(0.0);

    mockMvc
        .perform(delete("/api/pingan/training/exam-tasks/" + taskId).header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());

    for (JsonNode item : detailResults.path("items")) {
      mockMvc
          .perform(
              delete("/api/pingan/training/exam-results/" + item.path("id").asLong())
                  .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk());
    }

    mockMvc
        .perform(delete("/api/pingan/training/exam-tasks/" + taskId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    Integer taskDeleted =
        jdbcTemplate.queryForObject("select deleted from training_exam_task where id = ?", Integer.class, taskId);
    assertThat(taskDeleted).isEqualTo(1);
  }

  @Test
  void managesExamResultsWithFiltersAndBatchDelete() throws Exception {
    String token = login("admin", "123456");
    long firstTaskId =
        postJson(
                "/api/pingan/training/exam-tasks",
                taskPayload(
                    "待考任务-单测",
                    "2026-05-23",
                    "INACTIVE",
                    List.of(EXAM_PERSON_USER_ID),
                    List.of(questionPayload("单选题", "安全帽颜色？", "A", "A、B、C、D", "A", 5, 0))),
                token)
            .path("data")
            .path("id")
            .asLong();
    long secondTaskId =
        postJson(
                "/api/pingan/training/exam-tasks",
                taskPayload(
                    "过滤任务-单测",
                    "2026-05-24",
                    "ACTIVE",
                    List.of(EXAM_PERSON_USER_ID),
                    List.of(questionPayload("判断题", "动火作业无需审批。", "判", "正确、错误", "错误", 5, 5))),
                token)
            .path("data")
            .path("id")
            .asLong();

    assertThat(secondTaskId).isPositive();
    long firstResultId =
        getJson("/api/pingan/training/exam-tasks/" + firstTaskId + "/results", token)
            .path("data")
            .path("items")
            .get(0)
            .path("id")
            .asLong();

    JsonNode list =
        getJson(
                "/api/pingan/training/exam-results?companyId="
                    + SOURCE_COMPANY_ID
                    + "&departmentId="
                    + SOURCE_DEPARTMENT_ID
                    + "&dateStart=2026-05-23&dateEnd=2026-05-23&status=PENDING_EXAM",
                token)
            .path("data");
    assertThat(list.path("total").asInt()).isEqualTo(1);
    assertThat(list.path("items").get(0).path("id").asLong()).isEqualTo(firstResultId);
    assertThat(list.path("items").get(0).path("statusLabel").asText()).isEqualTo("待考试");

    mockMvc
        .perform(
            post("/api/pingan/training/exam-results/batch-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("ids", List.of(firstResultId))))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    Integer deleted =
        jdbcTemplate.queryForObject(
            "select deleted from training_exam_result where id = ?", Integer.class, firstResultId);
    assertThat(deleted).isEqualTo(1);
  }

  @Test
  void derivesExamResultStatusFromScore() throws Exception {
    String token = login("admin", "123456");
    long taskId =
        postJson(
                "/api/pingan/training/exam-tasks",
                taskPayload(
                    "成绩状态派生-单测",
                    "2026-05-25",
                    "ACTIVE",
                    List.of(EXAM_PERSON_USER_ID),
                    List.of(questionPayload("单选题", "零分是否待考试？", "A", "A、B", "A", 5, 0))),
                token)
            .path("data")
            .path("id")
            .asLong();

    JsonNode zeroScoreResult =
        postJson(
                "/api/pingan/training/exam-results",
                resultPayload(taskId, SECOND_EXAM_PERSON_USER_ID, 0, "EXAMED"),
                token)
            .path("data");
    assertThat(zeroScoreResult.path("status").asText()).isEqualTo("PENDING_EXAM");
    assertThat(zeroScoreResult.path("statusLabel").asText()).isEqualTo("待考试");

    JsonNode scoredResult =
        postJson(
                "/api/pingan/training/exam-results",
                resultPayload(taskId, SECOND_EXAM_PERSON_USER_ID, 0.1, "PENDING_EXAM"),
                token)
            .path("data");
    assertThat(scoredResult.path("status").asText()).isEqualTo("EXAMED");
    assertThat(scoredResult.path("statusLabel").asText()).isEqualTo("已考试");
  }

  @Test
  void miniProgramSubmitsAssignedExamWithServerGradingAndIdempotency() throws Exception {
    String adminToken = login("admin", "123456");
    String memberToken = login("team_member", "123456");
    String otherToken = login("team_leader", "123456");
    long taskId =
        postJson(
                "/api/pingan/training/exam-tasks",
                taskPayload(
                    "小程序交卷-单测",
                    "2026-05-26",
                    "ACTIVE",
                    List.of(EXAM_PERSON_USER_ID),
                    questionPayloads()),
                adminToken)
            .path("data")
            .path("id")
            .asLong();

    JsonNode pending =
        getJson("/api/mini/pingan/training/exams?status=PENDING_EXAM", memberToken).path("data");
    assertThat(pending.path("items").findValuesAsText("taskId")).contains(String.valueOf(taskId));

    JsonNode detail =
        getJson("/api/mini/pingan/training/exams/" + taskId, memberToken).path("data");
    assertThat(detail.path("status").asText()).isEqualTo("PENDING_EXAM");
    assertThat(detail.path("score").isNull()).isTrue();
    assertThat(detail.path("durationMinutes").asInt()).isEqualTo(30);
    assertThat(detail.path("remainingSeconds").asInt()).isEqualTo(1800);
    assertThat(detail.path("questions")).hasSize(2);
    assertThat(detail.path("questions").get(0).hasNonNull("correctAnswer")).isFalse();
    long firstQuestionId = detail.path("questions").get(0).path("id").asLong();
    long secondQuestionId = detail.path("questions").get(1).path("id").asLong();

    JsonNode progress =
        putJson(
                "/api/mini/pingan/training/exams/" + taskId + "/progress",
                Map.of(
                    "currentQuestionIndex",
                    1,
                    "remainingSeconds",
                    1700,
                    "answers",
                    List.of(
                        Map.of(
                            "questionId", firstQuestionId, "selectedOption", "A"))),
                memberToken)
            .path("data");
    assertThat(progress.path("currentQuestionIndex").asInt()).isEqualTo(1);
    assertThat(progress.path("remainingSeconds").asInt()).isEqualTo(1700);
    assertThat(progress.path("answeredCount").asInt()).isEqualTo(1);
    assertThat(progress.path("questions").get(0).path("selectedOption").asText())
        .isEqualTo("A");

    mockMvc
        .perform(
            post("/api/mini/pingan/training/exams/" + taskId + "/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "requestId",
                            "mini-submit-incomplete",
                            "answers",
                            List.of(Map.of("questionId", firstQuestionId, "selectedOption", "A")))))
                .header("Authorization", "Bearer " + memberToken))
        .andExpect(status().isBadRequest());

    Map<String, Object> submission =
        Map.of(
            "requestId",
            "mini-submit-success",
            "answers",
            List.of(
                Map.of("questionId", firstQuestionId, "selectedOption", "A"),
                Map.of("questionId", secondQuestionId, "selectedOption", "A")));
    JsonNode submitted =
        postJson(
                "/api/mini/pingan/training/exams/" + taskId + "/submissions",
                submission,
                memberToken)
            .path("data");
    assertThat(submitted.path("status").asText()).isEqualTo("EXAMED");
    assertThat(submitted.path("score").asDouble()).isEqualTo(5.0);
    assertThat(submitted.path("submittedAt").asText()).isNotBlank();
    assertThat(submitted.path("questions").get(0).path("correctAnswer").asText()).isEqualTo("A");
    assertThat(submitted.path("questions").get(1).path("actualScore").asDouble()).isZero();
    assertThat(submitted.path("correctCount").asInt()).isEqualTo(1);
    assertThat(submitted.path("incorrectCount").asInt()).isEqualTo(1);
    assertThat(submitted.path("elapsedSeconds").asInt()).isNotNegative();

    JsonNode replay =
        postJson(
                "/api/mini/pingan/training/exams/" + taskId + "/submissions",
                submission,
                memberToken)
            .path("data");
    assertThat(replay.path("resultId")).isEqualTo(submitted.path("resultId"));
    assertThat(replay.path("score")).isEqualTo(submitted.path("score"));

    mockMvc
        .perform(
            post("/api/mini/pingan/training/exams/" + taskId + "/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "requestId",
                            "mini-submit-duplicate",
                            "answers",
                            submission.get("answers"))))
                .header("Authorization", "Bearer " + memberToken))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            get("/api/mini/pingan/training/exams/" + taskId)
                .header("Authorization", "Bearer " + otherToken))
        .andExpect(status().isForbidden());

    JsonNode pcResults =
        getJson("/api/pingan/training/exam-tasks/" + taskId + "/results", adminToken).path("data");
    assertThat(pcResults.path("items").get(0).path("status").asText()).isEqualTo("EXAMED");
    assertThat(pcResults.path("items").get(0).path("score").asDouble()).isEqualTo(5.0);
  }

  @Test
  void trainingUserOptionsRespectCurrentUserDataScope() throws Exception {
    String token = login("company_leader", "123456");

    JsonNode users = getJson("/api/pingan/users", token).path("data");

    assertThat(ids(users)).doesNotContain(1L);
    assertThat(ids(users)).contains(10001L);
  }

  @Test
  void downloadsAndParsesQuestionWorkbook() throws Exception {
    String token = login("admin", "123456");

    var templateResponse =
        mockMvc
            .perform(
                get("/api/pingan/training/exam-tasks/questions/template")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(templateResponse.getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("training-exam-question-template");
    assertThat(templateResponse.getContentAsByteArray()).isNotEmpty();

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "考题导入.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            questionWorkbook());
    JsonNode importResult =
        objectMapper.readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/training/exam-tasks/questions/import")
                            .file(file)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");
    assertThat(importResult.path("successRows").asInt()).isEqualTo(2);
    assertThat(importResult.path("questions").get(0).path("questionType").asText()).isEqualTo("单选题");
    assertThat(importResult.path("questions").get(1).path("actualScore").asDouble()).isEqualTo(8.0);
  }

  @Test
  void managesQuestionBankAndExportsWorkbooks() throws Exception {
    String token = login("admin", "123456");

    JsonNode created =
        postJson(
                "/api/pingan/training/exam-question-bank",
                questionBankPayload("单选题", "题库消防知识题", "A", "A、B、C、D", "A", 5, 0),
                token)
            .path("data");
    long questionId = created.path("id").asLong();
    assertThat(created.path("company").asText()).isEqualTo("广晟源成");
    assertThat(created.path("department").asText()).isEqualTo("安全质量职卫部");
    assertThat(created.path("questionText").asText()).isEqualTo("题库消防知识题");

    JsonNode list =
        getJson(
                "/api/pingan/training/exam-question-bank?companyId="
                    + SOURCE_COMPANY_ID
                    + "&departmentId="
                    + SOURCE_DEPARTMENT_ID
                    + "&keyword=消防&questionType=单选题",
                token)
            .path("data");
    assertThat(list.path("total").asInt()).isEqualTo(1);
    assertThat(list.path("items").get(0).path("id").asLong()).isEqualTo(questionId);

    JsonNode updated =
        putJson(
                "/api/pingan/training/exam-question-bank/" + questionId,
                questionBankPayload("单选题", "题库消防知识题-更新", "B", "A、B、C、D", "B", 6, 0),
                token)
            .path("data");
    assertThat(updated.path("questionText").asText()).isEqualTo("题库消防知识题-更新");
    assertThat(updated.path("score").asDouble()).isEqualTo(6.0);

    var templateResponse =
        mockMvc
            .perform(
                get("/api/pingan/training/exam-question-bank/template")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(templateResponse.getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("training-exam-question-bank-template");
    assertThat(templateResponse.getContentAsByteArray()).isNotEmpty();

    var exportResponse =
        mockMvc
            .perform(
                get(
                        "/api/pingan/training/exam-question-bank/export?companyId="
                            + SOURCE_COMPANY_ID
                            + "&departmentId="
                            + SOURCE_DEPARTMENT_ID
                            + "&keyword=消防")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(exportResponse.getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("training-exam-question-bank");
    assertThat(exportResponse.getContentAsByteArray()).isNotEmpty();

    mockMvc
        .perform(delete("/api/pingan/training/exam-question-bank/" + questionId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    JsonNode afterDelete =
        getJson(
                "/api/pingan/training/exam-question-bank?companyId="
                    + SOURCE_COMPANY_ID
                    + "&departmentId="
                    + SOURCE_DEPARTMENT_ID
                    + "&keyword=消防",
                token)
            .path("data");
    assertThat(afterDelete.path("total").asInt()).isEqualTo(0);
  }

  @Test
  void managesReusableExamPapersAndBatchDeletesQuestionBankItems() throws Exception {
    String token = login("admin", "123456");
    Long paperTeamId =
        jdbcTemplate.queryForObject(
            """
            select id
            from sys_org
            where org_type = 'TEAM'
              and deleted = 0
            order by id
            limit 1
            """,
            Long.class);
    Long paperDepartmentId =
        jdbcTemplate.queryForObject(
            """
            select department.id
            from sys_org team
            join sys_org department
              on department.org_type = 'DEPARTMENT'
             and team.org_path like concat(department.org_path, '%')
            where team.id = ?
            order by length(department.org_path) desc
            limit 1
            """,
            Long.class,
            paperTeamId);
    Long paperCompanyId =
        jdbcTemplate.queryForObject(
            """
            select company.id
            from sys_org team
            join sys_org company
              on company.org_type = 'COMPANY'
             and team.org_path like concat(company.org_path, '%')
            where team.id = ?
            order by length(company.org_path) desc
            limit 1
            """,
            Long.class,
            paperTeamId);
    Map<String, Object> paperPayload =
        new HashMap<>(
            Map.of(
                "companyId", paperCompanyId,
                "departmentId", paperDepartmentId,
                "teamId", paperTeamId,
                "paperName", "建筑施工安全模拟卷",
                "description", "用于考试任务重复组卷",
                "questions", questionPayloads()));

    JsonNode created =
        postJson("/api/pingan/training/exam-papers", paperPayload, token).path("data");
    long paperId = created.path("id").asLong();
    assertThat(created.path("paperName").asText()).isEqualTo("建筑施工安全模拟卷");
    assertThat(created.path("questionCount").asInt()).isEqualTo(2);
    assertThat(created.path("totalScore").asDouble()).isEqualTo(15.0);
    assertThat(created.path("questions")).hasSize(2);
    assertThat(created.path("teamId").asLong()).isEqualTo(paperTeamId);
    assertThat(created.path("team").asText()).isNotBlank();

    JsonNode listed =
        getJson(
                "/api/pingan/training/exam-papers?companyId="
                    + paperCompanyId
                    + "&departmentId="
                    + paperDepartmentId
                    + "&teamId="
                    + paperTeamId
                    + "&keyword=建筑施工",
                token)
            .path("data");
    assertThat(listed.path("total").asInt()).isEqualTo(1);
    assertThat(listed.path("items").get(0).path("id").asLong()).isEqualTo(paperId);
    JsonNode departmentOnly =
        getJson(
                "/api/pingan/training/exam-papers?companyId="
                    + paperCompanyId
                    + "&departmentId="
                    + paperDepartmentId
                    + "&keyword=建筑施工",
                token)
            .path("data");
    assertThat(departmentOnly.path("total").asInt()).isZero();

    Map<String, Object> updatedPayload = new HashMap<>(paperPayload);
    updatedPayload.put("paperName", "建筑施工安全模拟卷（修订）");
    JsonNode updated =
        putJson("/api/pingan/training/exam-papers/" + paperId, updatedPayload, token)
            .path("data");
    assertThat(updated.path("paperName").asText()).contains("修订");

    Map<String, Object> globalPaperPayload =
        Map.of(
            "paperName", "跨公司共享建筑施工试卷",
            "description", "全局共享，不限定公司和部门",
            "questions", questionPayloads());
    JsonNode globalPaper =
        postJson("/api/pingan/training/exam-papers", globalPaperPayload, token).path("data");
    long globalPaperId = globalPaper.path("id").asLong();
    assertThat(globalPaper.path("companyId").isNull()).isTrue();
    assertThat(globalPaper.path("departmentId").isNull()).isTrue();
    assertThat(globalPaper.path("teamId").isNull()).isTrue();
    assertThat(globalPaper.path("company").asText()).isEqualTo("全局共享");
    assertThat(globalPaper.path("department").asText()).isEqualTo("全部部门");

    JsonNode companyFiltered =
        getJson(
                "/api/pingan/training/exam-papers?companyId="
                    + paperCompanyId
                    + "&departmentId="
                    + paperDepartmentId
                    + "&keyword=跨公司共享",
                token)
            .path("data");
    assertThat(companyFiltered.path("total").asInt()).isEqualTo(1);
    assertThat(companyFiltered.path("items").get(0).path("id").asLong())
        .isEqualTo(globalPaperId);

    Long otherCompanyId =
        jdbcTemplate.queryForObject(
            "select id from sys_org where org_type = 'COMPANY' and deleted = 0 and id <> ? order by id limit 1",
            Long.class,
            paperCompanyId);
    String otherCompanyToken =
        selfScopedPermissionToken(
            8521L,
            8521L,
            "global_paper_cross_company_view",
            "GLOBAL_PAPER_CROSS_COMPANY_VIEW",
            otherCompanyId,
            "PINGAN_TRAINING_EXAM_TASKS_VIEW");
    JsonNode crossCompanyList =
        getJson(
                "/api/pingan/training/exam-papers?keyword=跨公司共享",
                otherCompanyToken)
            .path("data");
    assertThat(crossCompanyList.path("total").asInt()).isEqualTo(1);
    assertThat(crossCompanyList.path("items").get(0).path("company").asText())
        .isEqualTo("全局共享");

    JsonNode firstQuestion =
        postJson(
                "/api/pingan/training/exam-question-bank",
                questionBankPayload("单选题", "批量删除题库题目一", "A", "A、B、C、D", "A", 5, 0),
                token)
            .path("data");
    JsonNode secondQuestion =
        postJson(
                "/api/pingan/training/exam-question-bank",
                questionBankPayload("单选题", "批量删除题库题目二", "A", "A、B、C、D", "A", 5, 0),
                token)
            .path("data");
    mockMvc
        .perform(
            post("/api/pingan/training/exam-question-bank/batch-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "ids",
                            List.of(
                                firstQuestion.path("id").asLong(),
                                secondQuestion.path("id").asLong()))))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/pingan/training/exam-papers/batch-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("ids", List.of(paperId, globalPaperId))))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    Integer deleted =
        jdbcTemplate.queryForObject(
            "select deleted from training_exam_paper where id = ?", Integer.class, paperId);
    assertThat(deleted).isEqualTo(1);
  }

  @Test
  void createsStructuredMultipleChoiceAndCaseAnalysisQuestionBankItems() throws Exception {
    String token = login("admin", "123456");
    Map<String, Object> multiple =
        questionBankPayload(
            "MULTIPLE_CHOICE",
            "进入作业现场前应检查哪些内容？",
            "",
            "A、B、C、D、E",
            "A、B",
            10,
            0);
    JsonNode multipleCreated =
        postJson("/api/pingan/training/exam-question-bank", multiple, token).path("data");
    assertThat(multipleCreated.path("questionType").asText()).isEqualTo("MULTIPLE_CHOICE");
    assertThat(multipleCreated.path("questionTypeLabel").asText()).isEqualTo("多选题");
    assertThat(multipleCreated.path("options").size()).isEqualTo(5);
    assertThat(multipleCreated.path("correctAnswers").size()).isEqualTo(2);
    assertThat(multipleCreated.path("answerExplanation").asText()).isEqualTo("测试答案解析");

    Map<String, Object> childSingle =
        Map.of(
            "questionType", "SINGLE_CHOICE",
            "questionText", "发现气体检测不合格时首先应当？",
            "options",
                List.of(
                    Map.of("key", "A", "content", "停止进入"),
                    Map.of("key", "B", "content", "继续作业"),
                    Map.of("key", "C", "content", "关闭报警"),
                    Map.of("key", "D", "content", "减少监护"),
                    Map.of("key", "E", "content", "等待指令")),
            "correctAnswers", List.of("E"),
            "referenceAnswer", "",
            "answerExplanation", "检测不合格不得进入。",
            "score", 5);
    Map<String, Object> childAnswer = new HashMap<>();
    childAnswer.put("questionType", "SHORT_ANSWER");
    childAnswer.put("questionText", "说明受限空间作业的主要控制措施。");
    childAnswer.put("options", List.of());
    childAnswer.put("correctAnswers", List.of());
    childAnswer.put("referenceAnswer", "审批、检测、通风、监护和应急准备。");
    childAnswer.put("answerExplanation", "按受限空间作业制度评分。");
    childAnswer.put("score", 15);
    Map<String, Object> casePayload = new HashMap<>();
    casePayload.put("companyId", SOURCE_COMPANY_ID);
    casePayload.put("departmentId", SOURCE_DEPARTMENT_ID);
    casePayload.put("questionType", "CASE_ANALYSIS");
    casePayload.put("questionText", "受限空间违章作业案例");
    casePayload.put("caseMaterial", "某班组未完成检测即准备进入受限空间。");
    casePayload.put("options", List.of());
    casePayload.put("correctAnswers", List.of());
    casePayload.put("referenceAnswer", "");
    casePayload.put("answerExplanation", "综合考察风险辨识和控制措施。");
    casePayload.put("score", 0);
    casePayload.put("children", List.of(childSingle, childAnswer));
    JsonNode caseCreated =
        postJson("/api/pingan/training/exam-question-bank", casePayload, token).path("data");
    assertThat(caseCreated.path("questionType").asText()).isEqualTo("CASE_ANALYSIS");
    assertThat(caseCreated.path("children").size()).isEqualTo(2);
    assertThat(caseCreated.path("children").get(0).path("options")).hasSize(5);
    assertThat(caseCreated.path("children").get(0).path("correctAnswers").get(0).asText())
        .isEqualTo("E");
    assertThat(caseCreated.path("score").asDouble()).isEqualTo(20.0);
    assertThat(caseCreated.path("children").get(1).path("referenceAnswer").asText())
        .contains("审批");

    @SuppressWarnings("unchecked")
    Map<String, Object> multipleSnapshot =
        objectMapper.convertValue(multipleCreated, Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> caseSnapshot =
        objectMapper.convertValue(caseCreated, Map.class);
    JsonNode draftTask =
        postJson(
                "/api/pingan/training/exam-tasks",
                taskPayload(
                    "结构化案例题快照",
                    "2026-06-02",
                    "INACTIVE",
                    List.of(EXAM_PERSON_USER_ID),
                    List.of(multipleSnapshot, caseSnapshot)),
                token)
            .path("data");
    long taskId = draftTask.path("id").asLong();
    JsonNode taskDetail =
        getJson("/api/pingan/training/exam-tasks/" + taskId, token).path("data");
    assertThat(taskDetail.path("questions").get(0).path("questionType").asText())
        .isEqualTo("MULTIPLE_CHOICE");
    assertThat(taskDetail.path("questions").get(0).path("options").size()).isEqualTo(5);
    assertThat(taskDetail.path("questions").get(0).path("correctAnswers").size()).isEqualTo(2);
    assertThat(taskDetail.path("questions").get(1).path("questionType").asText())
        .isEqualTo("CASE_ANALYSIS");
    assertThat(taskDetail.path("questions").get(1).path("children").size()).isEqualTo(2);
    assertThat(taskDetail.path("questions").get(1).path("caseMaterial").asText())
        .contains("未完成检测");
    long resultId = taskDetail.path("results").get(0).path("id").asLong();
    JsonNode resultDetail =
        getJson("/api/pingan/training/exam-results/" + resultId, token).path("data");
    assertThat(resultDetail.path("questions").get(0).path("options").size()).isEqualTo(5);
    assertThat(resultDetail.path("questions").get(1).path("children").size()).isEqualTo(2);
    assertThat(resultDetail.path("questions").get(1).path("answerExplanation").asText())
        .contains("风险辨识");

    casePayload.put("questionText", "已修改的题库案例标题");
    putJson(
        "/api/pingan/training/exam-question-bank/" + caseCreated.path("id").asLong(),
        casePayload,
        token);
    JsonNode unchangedSnapshot =
        getJson("/api/pingan/training/exam-tasks/" + taskId, token).path("data");
    assertThat(unchangedSnapshot.path("questions").get(1).path("questionText").asText())
        .isEqualTo("受限空间违章作业案例");

    long activeTaskId =
        postJson(
                "/api/pingan/training/exam-tasks",
                taskPayload(
                    "小程序结构化案例题",
                    "2026-06-03",
                    "ACTIVE",
                    List.of(EXAM_PERSON_USER_ID),
                    List.of(multipleSnapshot, caseSnapshot)),
                token)
            .path("data")
            .path("id")
            .asLong();
    String memberToken = login("team_member", "123456");
    JsonNode miniDetail =
        getJson("/api/mini/pingan/training/exams/" + activeTaskId, memberToken).path("data");
    assertThat(miniDetail.path("questions")).hasSize(3);
    assertThat(miniDetail.path("questions").get(0).path("questionType").asText())
        .isEqualTo("MULTIPLE_CHOICE");
    assertThat(miniDetail.path("questions").get(0).path("questionTypeLabel").asText())
        .isEqualTo("多选题");
    assertThat(miniDetail.path("questions").get(0).path("options")).hasSize(5);
    assertThat(miniDetail.path("questions").get(0).path("correctAnswers")).isEmpty();
    assertThat(miniDetail.path("questions").get(1).path("caseMaterial").asText())
        .contains("未完成检测");
    assertThat(miniDetail.path("questions").get(1).path("questionKey").asText()).contains(":0");
    assertThat(miniDetail.path("questions").get(2).path("questionType").asText())
        .isEqualTo("SHORT_ANSWER");
    assertThat(miniDetail.path("questions").get(2).path("referenceAnswer").isNull()).isTrue();

    List<Map<String, Object>> structuredAnswers = new ArrayList<>();
    structuredAnswers.add(
        Map.of(
            "questionKey",
            miniDetail.path("questions").get(0).path("questionKey").asText(),
            "selectedOption",
            "B,A"));
    structuredAnswers.add(
        Map.of(
            "questionKey",
            miniDetail.path("questions").get(1).path("questionKey").asText(),
            "selectedOption",
            "E"));
    structuredAnswers.add(
        Map.of(
            "questionKey",
            miniDetail.path("questions").get(2).path("questionKey").asText(),
            "selectedOption",
            "应先审批并检测，持续通风，安排监护并做好应急准备。"));
    JsonNode submitted =
        postJson(
                "/api/mini/pingan/training/exams/" + activeTaskId + "/submissions",
                Map.of("requestId", "structured-case-submit", "answers", structuredAnswers),
                memberToken)
            .path("data");
    assertThat(submitted.path("status").asText()).isEqualTo("PENDING_REVIEW");
    assertThat(submitted.path("statusLabel").asText()).isEqualTo("待评分");
    assertThat(submitted.path("score").isNull()).isTrue();
    assertThat(submitted.path("questions").get(0).path("correctAnswers")).hasSize(2);
    assertThat(submitted.path("questions").get(2).path("referenceAnswer").asText())
        .contains("审批");
    JsonNode pendingReviewDetail =
        getJson(
                "/api/pingan/training/exam-results/" + submitted.path("resultId").asLong(),
                token)
            .path("data");
    long caseResultQuestionId =
        pendingReviewDetail.path("questions").get(1).path("id").asLong();
    JsonNode reviewed =
        postJson(
                "/api/pingan/training/exam-results/"
                    + submitted.path("resultId").asLong()
                    + "/review",
                Map.of(
                    "scores",
                    List.of(
                        Map.of(
                            "questionId", caseResultQuestionId,
                            "actualScore", 12))),
                token)
            .path("data");
    assertThat(reviewed.path("status").asText()).isEqualTo("EXAMED");
    assertThat(reviewed.path("score").asDouble()).isEqualTo(22.0);
    mockMvc
        .perform(
            post(
                    "/api/pingan/training/exam-results/"
                        + submitted.path("resultId").asLong()
                        + "/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "scores",
                            List.of(
                                Map.of(
                                    "questionId", caseResultQuestionId,
                                    "actualScore", 12)))))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void confirmsValidatedPdfDraftsAndRejectsFakePdfFiles() throws Exception {
    String token = login("admin", "123456");
    Map<String, Object> first =
        pdfDraft(
            questionBankPayload(
                "SINGLE_CHOICE",
                "PDF确认入库题目一",
                "",
                "A、B、C、D",
                "A",
                1,
                0),
            "单选题 1");
    Map<String, Object> second =
        pdfDraft(
            questionBankPayload(
                "MULTIPLE_CHOICE",
                "PDF确认入库题目二",
                "",
                "A、B、C、D、E",
                "A、C",
                1,
                0),
            "多选题 2");

    JsonNode confirmed =
        postJson(
                "/api/pingan/training/exam-question-bank/pdf/confirm",
                Map.of(
                    "companyId", SOURCE_COMPANY_ID,
                    "departmentId", SOURCE_DEPARTMENT_ID,
                    "questions", List.of(first, second)),
                token)
            .path("data");
    assertThat(confirmed.path("successRows").asInt()).isEqualTo(2);
    assertThat(confirmed.path("questions").get(1).path("correctAnswers").size())
        .isEqualTo(2);

    int beforePaperConfirm =
        jdbcTemplate.queryForObject(
            "select count(*) from training_exam_question_bank where question_text = 'PDF当前试卷题目'",
            Integer.class);
    Map<String, Object> paperDraft =
        pdfDraft(
            questionBankPayload(
                "SINGLE_CHOICE",
                "PDF当前试卷题目",
                "",
                "A、B、C、D",
                "B",
                2,
                0),
            "单选题 1");
    JsonNode paperConfirmed =
        postJson(
                "/api/pingan/training/exam-question-bank/pdf/confirm",
                Map.of("target", "PAPER", "questions", List.of(paperDraft)),
                token)
            .path("data");
    assertThat(paperConfirmed.path("target").asText()).isEqualTo("PAPER");
    assertThat(paperConfirmed.path("successRows").asInt()).isEqualTo(1);
    assertThat(paperConfirmed.path("questions").size()).isZero();
    assertThat(paperConfirmed.path("paperQuestions").get(0).path("answer").asText())
        .isEqualTo("B");
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from training_exam_question_bank where question_text = 'PDF当前试卷题目'",
                Integer.class))
        .isEqualTo(beforePaperConfirm);

    int beforeInvalid =
        jdbcTemplate.queryForObject(
            "select count(*) from training_exam_question_bank where question_text like 'PDF事务回滚题目%'",
            Integer.class);
    Map<String, Object> validBeforeFailure =
        pdfDraft(
            questionBankPayload(
                "SINGLE_CHOICE",
                "PDF事务回滚题目一",
                "",
                "A、B、C、D",
                "A",
                1,
                0),
            "单选题 3");
    Map<String, Object> invalid =
        pdfDraft(
            questionBankPayload(
                "SINGLE_CHOICE",
                "PDF事务回滚题目二",
                "",
                "A、B、C、D",
                "A",
                1,
                0),
            "单选题 4");
    invalid.put("correctAnswers", List.of("E"));
    mockMvc
        .perform(
            post("/api/pingan/training/exam-question-bank/pdf/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "companyId", SOURCE_COMPANY_ID,
                            "departmentId", SOURCE_DEPARTMENT_ID,
                            "questions", List.of(validBeforeFailure, invalid))))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("单选题 4")));
    int afterInvalid =
        jdbcTemplate.queryForObject(
            "select count(*) from training_exam_question_bank where question_text like 'PDF事务回滚题目%'",
            Integer.class);
    assertThat(afterInvalid).isEqualTo(beforeInvalid);

    MockMultipartFile fakePdf =
        new MockMultipartFile(
            "file",
            "fake.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "not-a-real-pdf".getBytes(StandardCharsets.UTF_8));
    mockMvc
        .perform(
            multipart("/api/pingan/training/exam-question-bank/pdf/preview")
                .file(fakePdf)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("PDF 附件仅支持真实的 PDF 文件"));
  }

  @Test
  void acceptsFourOptionMultipleChoiceTasksAndRequiresTwoAnswers() throws Exception {
    String token = login("admin", "123456");
    Map<String, Object> fourOptionMultiple =
        questionBankPayload(
            "MULTIPLE_CHOICE",
            "以下哪些属于考试前检查内容？",
            "",
            "A、B、C、D",
            "A、C",
            10,
            0);
    fourOptionMultiple.remove("companyId");
    fourOptionMultiple.remove("departmentId");

    JsonNode created =
        postJson(
                "/api/pingan/training/exam-tasks",
                taskPayload(
                    "四选项多选题考试",
                    "2026-07-30",
                    "ACTIVE",
                    List.of(EXAM_PERSON_USER_ID),
                    List.of(fourOptionMultiple)),
                token)
            .path("data");
    JsonNode detail =
        getJson("/api/pingan/training/exam-tasks/" + created.path("id").asText(), token).path("data");
    assertThat(detail.path("questions").get(0).path("options")).hasSize(4);
    assertThat(detail.path("questions").get(0).path("correctAnswers")).hasSize(2);

    Map<String, Object> invalid = new HashMap<>(fourOptionMultiple);
    invalid.put("answer", "A");
    invalid.put("correctAnswers", List.of("A"));
    mockMvc
        .perform(
            post("/api/pingan/training/exam-tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        taskPayload(
                            "多选题单答案错误考试",
                            "2026-07-30",
                            "ACTIVE",
                            List.of(EXAM_PERSON_USER_ID),
                            List.of(invalid))))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("多选题至少需要设置两个答案"));
  }

  @Test
  void rejectsInvalidStructuredQuestionBankAnswers() throws Exception {
    String token = login("admin", "123456");
    Map<String, Object> invalid =
        questionBankPayload(
            "SINGLE_CHOICE",
            "错误的单选题",
            "",
            "A、B、C、D",
            "A、B",
            5,
            0);

    mockMvc
        .perform(
            post("/api/pingan/training/exam-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("单选题必须且只能设置一个答案"));
  }

  @Test
  void importsQuestionBankWorkbookAndExportsTaskQuestionSnapshot() throws Exception {
    String token = login("admin", "123456");

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "题库导入.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            questionWorkbook());
    JsonNode importResult =
        objectMapper.readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/training/exam-question-bank/import")
                            .file(file)
                            .param("companyId", String.valueOf(SOURCE_COMPANY_ID))
                            .param("departmentId", String.valueOf(SOURCE_DEPARTMENT_ID))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");
    assertThat(importResult.path("successRows").asInt()).isEqualTo(2);
    assertThat(importResult.path("questions").get(0).path("company").asText()).isEqualTo("广晟源成");
    assertThat(importResult.path("questions").get(1).path("actualScore").asDouble()).isEqualTo(8.0);

    JsonNode bankList =
        getJson("/api/pingan/training/exam-question-bank?keyword=安全生产的方针", token).path("data");
    assertThat(bankList.path("total").asInt()).isGreaterThanOrEqualTo(1);

    long taskId =
        postJson(
                "/api/pingan/training/exam-tasks",
                taskPayload(
                    "题库快照导出-单测",
                    "2026-05-26",
                    "ACTIVE",
                    List.of(EXAM_PERSON_USER_ID),
                    questionPayloads()),
                token)
            .path("data")
            .path("id")
            .asLong();

    var exportResponse =
        mockMvc
            .perform(
                get("/api/pingan/training/exam-tasks/" + taskId + "/questions/export")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(exportResponse.getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("training-exam-task-questions");
    assertThat(exportResponse.getContentAsByteArray()).isNotEmpty();
  }

  @Test
  void enforcesTrainingExamViewAndManagePermissions() throws Exception {
    String noViewToken =
        permissionToken(
            8511L,
            8511L,
            "training_exam_entry_only",
            "TRAINING_EXAM_ENTRY_ONLY",
            "PINGAN_TRAINING_EXAM_TASKS_ENTRY");
    mockMvc
        .perform(get("/api/pingan/training/exam-tasks").header("Authorization", "Bearer " + noViewToken))
        .andExpect(status().isForbidden());

    String viewOnlyToken =
        permissionToken(
            8512L,
            8512L,
            "training_exam_view_only",
            "TRAINING_EXAM_VIEW_ONLY",
            "PINGAN_TRAINING_EXAM_TASKS_VIEW");
    mockMvc
        .perform(get("/api/pingan/training/exam-tasks").header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/pingan/training/exam-results").header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isForbidden());

    String resultViewToken =
        permissionToken(
            8513L,
            8513L,
            "training_exam_result_view_only",
            "TRAINING_EXAM_RESULT_VIEW_ONLY",
            "PINGAN_TRAINING_EXAM_RESULTS_VIEW");
    mockMvc
        .perform(get("/api/pingan/training/exam-results").header("Authorization", "Bearer " + resultViewToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/pingan/training/exam-tasks").header("Authorization", "Bearer " + resultViewToken))
        .andExpect(status().isForbidden());

    String adminToken = login("admin", "123456");
    long taskId =
        postJson(
                "/api/pingan/training/exam-tasks",
                taskPayload(
                    "考试权限测试",
                    "2026-05-31",
                    "ACTIVE",
                    List.of(EXAM_PERSON_USER_ID),
                    List.of(questionPayload("单选题", "权限题？", "A", "A、B", "A", 5, 5))),
                adminToken)
            .path("data")
            .path("id")
            .asLong();

    mockMvc
        .perform(get("/api/pingan/training/exam-tasks/" + taskId).header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/pingan/training/exam-tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        taskPayload(
                            "无管理考试新增",
                            "2026-05-31",
                            "ACTIVE",
                            List.of(EXAM_PERSON_USER_ID),
                            List.of(questionPayload("单选题", "无管理题？", "A", "A、B", "A", 5, 5)))))
                .header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/pingan/training/exam-question-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        questionBankPayload("单选题", "无管理题库新增", "A", "A、B", "A", 5, 0)))
                .header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void selfScopedMemberCanViewExamResultsForOwnCompanyFilter() throws Exception {
    String adminToken = login("admin", "123456");
    Map<String, Object> ownDepartmentTask =
        new HashMap<>(
            taskPayload(
                "成员成绩查看测试",
                "2026-06-01",
                "ACTIVE",
                List.of(EXAM_PERSON_USER_ID),
                List.of(questionPayload("单选题", "成员能否查看成绩？", "A", "A、B", "A", 5, 5))));
    ownDepartmentTask.put("departmentId", 101109L);
    postJson(
        "/api/pingan/training/exam-tasks",
        ownDepartmentTask,
        adminToken);
    String memberToken =
        selfScopedPermissionToken(
            8521L,
            8521L,
            "training_exam_result_member",
            "TRAINING_EXAM_RESULT_MEMBER",
            1011001L,
            "PINGAN_TRAINING_EXAM_RESULTS_VIEW");

    JsonNode result =
        getJson(
                "/api/pingan/training/exam-results?companyId=" + SOURCE_COMPANY_ID + "&status=all",
                memberToken)
            .path("data");

    assertThat(result.path("total").asInt()).isGreaterThanOrEqualTo(1);
  }

  private Map<String, Object> taskPayload(
      String exam, String examDate, String status, List<Long> examPersonUserIds, List<Map<String, Object>> questions) {
    return Map.of(
        "companyId", SOURCE_COMPANY_ID,
        "departmentId", SOURCE_DEPARTMENT_ID,
        "exam", exam,
        "examDate", examDate,
        "status", status,
        "remark", "安全管理部上半年安全知识考试",
        "examPersonUserIds", examPersonUserIds,
        "questions", questions);
  }

  private Map<String, Object> resultPayload(long taskId, long examPersonUserId, double score, String status) {
    return Map.of(
        "taskId", taskId,
        "examPersonUserId", examPersonUserId,
        "score", score,
        "status", status);
  }

  private List<Map<String, Object>> questionPayloads() {
    return List.of(
        questionPayload("单选题", "安全生产的方针是？", "A", "A、B、C、D", "A", 5, 5),
        questionPayload("多选题", "以下哪些属于安全防护用品？", "A、C、D", "A、B、C、D", "A、C、D", 10, 8));
  }

  private Map<String, Object> questionPayload(
      String type, String text, String selectedOption, String allOptions, String answer, double score, double actualScore) {
    return Map.of(
        "questionType", type,
        "questionText", text,
        "selectedOption", selectedOption,
        "allOptions", allOptions,
        "answer", answer,
        "score", score,
        "actualScore", actualScore);
  }

  private Map<String, Object> questionBankPayload(
      String type, String text, String selectedOption, String allOptions, String answer, double score, double actualScore) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("companyId", SOURCE_COMPANY_ID);
    payload.put("departmentId", SOURCE_DEPARTMENT_ID);
    payload.put("questionType", type);
    payload.put("questionText", text);
    payload.put("selectedOption", selectedOption);
    payload.put("allOptions", allOptions);
    payload.put("answer", answer);
    payload.put("score", score);
    payload.put("actualScore", actualScore);
    List<String> keys = java.util.Arrays.stream(allOptions.split("、")).toList();
    payload.put(
        "options",
        keys.stream()
            .map(key -> Map.of("key", key, "content", "选项" + key))
            .toList());
    payload.put(
        "correctAnswers",
        java.util.Arrays.stream(answer.split("、")).toList());
    payload.put("answerExplanation", "测试答案解析");
    payload.put("referenceAnswer", "");
    payload.put("caseMaterial", "");
    payload.put("children", List.of());
    return payload;
  }

  private long createAutomaticScopeTask(
      String token, String exam, long companyId, Long departmentId, Long teamId)
      throws Exception {
    Map<String, Object> payload = new HashMap<>();
    payload.put("companyId", companyId);
    if (departmentId != null) {
      payload.put("departmentId", departmentId);
    }
    if (teamId != null) {
      payload.put("teamId", teamId);
    }
    payload.put("exam", exam);
    payload.put("examDate", "2026-07-29");
    payload.put("status", "ACTIVE");
    payload.put("remark", "组织范围自动生成考试人员");
    payload.put("questions", questionPayloads());
    return postJson("/api/pingan/training/exam-tasks", payload, token)
        .path("data")
        .path("id")
        .asLong();
  }

  private int taskResultCount(long taskId) {
    return jdbcTemplate.queryForObject(
        "select count(*) from training_exam_result where task_id = ? and deleted = 0",
        Integer.class,
        taskId);
  }

  private int activePersonCountForScope(long orgId, String scopeType) {
    return jdbcTemplate.queryForObject(
        """
        select count(*)
        from sys_user user_account
        join sys_org user_org on user_org.id = user_account.org_id
        join sys_org scope_org on scope_org.id = ?
        where user_account.deleted = 0
          and user_account.status = 'ACTIVE'
          and user_org.deleted = 0
          and user_org.org_path like concat(scope_org.org_path, '%')
          and (
            ? <> 'COMPANY'
            or user_org.org_type in ('DEPARTMENT', 'TEAM')
          )
        """,
        Integer.class,
        orgId,
        scopeType);
  }

  private Map<String, Object> pdfDraft(
      Map<String, Object> questionBankPayload, String sourceLabel) {
    Map<String, Object> draft = new HashMap<>(questionBankPayload);
    draft.remove("companyId");
    draft.remove("departmentId");
    draft.remove("selectedOption");
    draft.remove("allOptions");
    draft.remove("answer");
    draft.remove("actualScore");
    draft.put("sourceLabel", sourceLabel);
    draft.put(
        "questionTypeLabel",
        "MULTIPLE_CHOICE".equals(draft.get("questionType")) ? "多选题" : "单选题");
    return draft;
  }

  private byte[] questionWorkbook() throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("考题");
      Row header = sheet.createRow(0);
      List<String> headers = List.of("题型", "考题", "选项", "全部选项", "答案", "分值", "实际分");
      for (int i = 0; i < headers.size(); i++) {
        header.createCell(i).setCellValue(headers.get(i));
      }
      Object[][] rows = {
        {"单选题", "安全生产的方针是？", "A", "A、B、C、D", "A", 5, 5},
        {"多选题", "以下哪些属于安全防护用品？", "A、C、D", "A、B、C、D", "A、C、D", 10, 8},
      };
      for (int r = 0; r < rows.length; r++) {
        Row row = sheet.createRow(r + 1);
        for (int c = 0; c < rows[r].length; c++) {
          Object value = rows[r][c];
          if (value instanceof Number number) {
            row.createCell(c).setCellValue(number.doubleValue());
          } else {
            row.createCell(c).setCellValue(String.valueOf(value));
          }
        }
      }
      workbook.write(output);
      return output.toByteArray();
    }
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

  private String selfScopedPermissionToken(
      long userId,
      long roleId,
      String username,
      String roleCode,
      long orgId,
      String... permissionCodes)
      throws Exception {
    jdbcTemplate.update("delete from sys_user_role where user_id = ? or role_id = ?", userId, roleId);
    jdbcTemplate.update("delete from sys_role_menu where role_id = ?", roleId);
    jdbcTemplate.update("delete from sys_user where id = ? or username = ?", userId, username);
    jdbcTemplate.update("delete from sys_role where id = ? or role_code = ?", roleId, roleCode);
    jdbcTemplate.update(
        "insert into sys_role (id, role_code, role_name, data_scope) values (?, ?, ?, 'SELF')",
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
        orgId);
    jdbcTemplate.update("insert into sys_user_role (user_id, role_id) values (?, ?)", userId, roleId);
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

  private List<Long> ids(JsonNode nodes) {
    List<Long> ids = new java.util.ArrayList<>();
    nodes.forEach(node -> ids.add(node.path("id").asLong()));
    return ids;
  }
}
