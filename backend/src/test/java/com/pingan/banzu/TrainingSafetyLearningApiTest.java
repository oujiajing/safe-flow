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
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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
class TrainingSafetyLearningApiTest {

  private static final long SOURCE_COMPANY_ID = 4L;
  private static final List<String> TEMPLATE_HEADERS =
      List.of("公司", "分类", "标题", "内容", "封面图", "视频", "日期", "计时", "编码", "附件", "HTML提取", "草稿", "状态");

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void createsUpdatesFiltersAndDeletesSafetyLearningContent() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

    JsonNode created =
        postJson(
                "/api/pingan/training/safety-learning/contents",
                payload("安全知识", "高处作业安全学习", "2026-05-22", "ACTIVE", ""),
                token)
            .path("data");
    long id = created.path("id").asLong();
    assertThat(created.path("company").asText()).isEqualTo("Demo Works Company");
    assertThat(created.path("code").asText()).startsWith("LEARN-202605-");
    assertThat(created.path("statusLabel").asText()).isEqualTo("激活");

    JsonNode updated =
        putJson(
                "/api/pingan/training/safety-learning/contents/" + id,
                payload("事故案例", "高处作业事故案例", "2026-05-23", "DRAFT", created.path("code").asText()),
                token)
            .path("data");
    assertThat(updated.path("category").asText()).isEqualTo("事故案例");
    assertThat(updated.path("draft").asText()).isEqualTo("是");
    assertThat(updated.path("statusLabel").asText()).isEqualTo("草稿");

    JsonNode filtered =
        getJson(
                "/api/pingan/training/safety-learning/contents?companyId="
                    + SOURCE_COMPANY_ID
                    + "&category=事故案例&keyword=高处作业&dateStart=2026-05-23&dateEnd=2026-05-23&status=DRAFT",
                token)
            .path("data");
    assertThat(filtered.path("total").asInt()).isEqualTo(1);
    assertThat(filtered.path("items").get(0).path("id").asLong()).isEqualTo(id);

    mockMvc
        .perform(delete("/api/pingan/training/safety-learning/contents/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    Integer deleted =
        jdbcTemplate.queryForObject(
            "select deleted from training_safety_learning_content where id = ?", Integer.class, id);
    assertThat(deleted).isEqualTo(1);
  }

  @Test
  void uploadsPdfAttachmentAndRejectsNonPdf() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");
    long id =
        postJson(
                "/api/pingan/training/safety-learning/contents",
                payload("法律知识", "安全生产法学习", "2026-05-22", "ACTIVE", "LAW-001"),
                token)
            .path("data")
            .path("id")
            .asLong();

    MockMultipartFile pdf =
        new MockMultipartFile("file", "安全学习.pdf", "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));
    JsonNode uploaded =
        objectMapper.readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/training/safety-learning/contents/" + id + "/attachment")
                            .file(pdf)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");
    assertThat(uploaded.path("attachment").path("fileKind").asText()).isEqualTo("PDF");
    assertThat(uploaded.path("attachment").path("originalName").asText()).isEqualTo("安全学习.pdf");
    long attachmentId = uploaded.path("attachment").path("id").asLong();

    JsonNode removed =
        objectMapper.readTree(
                mockMvc
                    .perform(
                        delete("/api/pingan/training/safety-learning/contents/" + id + "/attachment")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");
    assertThat(removed.path("attachment").isMissingNode() || removed.path("attachment").isNull()).isTrue();
    assertThat(removed.path("attachmentText").asText()).isEmpty();
    Long contentAttachmentId =
        jdbcTemplate.queryForObject(
            "select attachment_id from training_safety_learning_content where id = ?", Long.class, id);
    assertThat(contentAttachmentId).isNull();
    Integer attachmentDeleted =
        jdbcTemplate.queryForObject("select deleted from biz_attachment where id = ?", Integer.class, attachmentId);
    assertThat(attachmentDeleted).isEqualTo(1);

    MockMultipartFile image = new MockMultipartFile("file", "错误.png", "image/png", new byte[] {1, 2});
    mockMvc
        .perform(
            multipart("/api/pingan/training/safety-learning/contents/" + id + "/attachment")
                .file(image)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void uploadsAndRemovesCoverImageAndVideo() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");
    long id =
        postJson(
                "/api/pingan/training/safety-learning/contents",
                payload("安全课程", "视频安全学习", "2026-05-22", "ACTIVE", "MEDIA-001"),
                token)
            .path("data")
            .path("id")
            .asLong();

    MockMultipartFile cover =
        new MockMultipartFile("file", "封面.png", "image/png", new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
    JsonNode coverUploaded =
        objectMapper.readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/training/safety-learning/contents/" + id + "/cover-image")
                            .file(cover)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");
    assertThat(coverUploaded.path("coverImageAttachment").path("fileKind").asText()).isEqualTo("IMAGE");
    assertThat(coverUploaded.path("coverImageAttachment").path("originalName").asText()).isEqualTo("封面.png");

    MockMultipartFile video =
        new MockMultipartFile("file", "课程.mp4", "video/mp4", new byte[] {0, 0, 0, 12, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'});
    JsonNode videoUploaded =
        objectMapper.readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/training/safety-learning/contents/" + id + "/video")
                            .file(video)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");
    assertThat(videoUploaded.path("videoAttachment").path("fileKind").asText()).isEqualTo("VIDEO");
    assertThat(videoUploaded.path("videoAttachment").path("originalName").asText()).isEqualTo("课程.mp4");
    long coverAttachmentId = videoUploaded.path("coverImageAttachment").path("id").asLong();
    long videoAttachmentId = videoUploaded.path("videoAttachment").path("id").asLong();

    mockMvc
        .perform(
            multipart("/api/pingan/training/safety-learning/contents/" + id + "/cover-image")
                .file(new MockMultipartFile("file", "错误.txt", "text/plain", "no".getBytes(StandardCharsets.UTF_8)))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            multipart("/api/pingan/training/safety-learning/contents/" + id + "/video")
                .file(new MockMultipartFile("file", "错误.png", "image/png", new byte[] {1}))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());

    JsonNode coverRemoved =
        objectMapper.readTree(
                mockMvc
                    .perform(
                        delete("/api/pingan/training/safety-learning/contents/" + id + "/cover-image")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");
    assertThat(coverRemoved.path("coverImageAttachment").isMissingNode() || coverRemoved.path("coverImageAttachment").isNull())
        .isTrue();
    assertThat(coverRemoved.path("coverImage").asText()).isEmpty();
    Integer coverDeleted =
        jdbcTemplate.queryForObject("select deleted from biz_attachment where id = ?", Integer.class, coverAttachmentId);
    assertThat(coverDeleted).isEqualTo(1);

    JsonNode videoRemoved =
        objectMapper.readTree(
                mockMvc
                    .perform(
                        delete("/api/pingan/training/safety-learning/contents/" + id + "/video")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");
    assertThat(videoRemoved.path("videoAttachment").isMissingNode() || videoRemoved.path("videoAttachment").isNull())
        .isTrue();
    assertThat(videoRemoved.path("video").asText()).isEmpty();
    Integer videoDeleted =
        jdbcTemplate.queryForObject("select deleted from biz_attachment where id = ?", Integer.class, videoAttachmentId);
    assertThat(videoDeleted).isEqualTo(1);
  }

  @Test
  void downloadsTemplateImportsWorkbookAndExportsFilteredRows() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

    var templateResponse =
        mockMvc
            .perform(
                get("/api/pingan/training/safety-learning/contents/template")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(templateResponse.getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains(".xlsx");
    assertThat(headers(templateResponse.getContentAsByteArray())).isEqualTo(TEMPLATE_HEADERS);

    MockMultipartFile workbook =
        new MockMultipartFile(
            "file",
            "学习内容.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            importWorkbook("设备操作规程", "叉车安全学习", "LEARN-IMPORT-001"));
    JsonNode importResult =
        objectMapper.readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/training/safety-learning/contents/import")
                            .file(workbook)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");
    assertThat(importResult.path("successRows").asInt()).isEqualTo(1);
    assertThat(importResult.path("errors")).isEmpty();

    MockMultipartFile duplicate =
        new MockMultipartFile(
            "file",
            "学习内容重复.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            importWorkbook("设备操作规程", "重复编码", "LEARN-IMPORT-001"));
    JsonNode duplicateResult =
        objectMapper.readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/training/safety-learning/contents/import")
                            .file(duplicate)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");
    assertThat(duplicateResult.path("successRows").asInt()).isEqualTo(0);
    assertThat(duplicateResult.path("errors").get(0).asText()).contains("编码已存在");

    var exportResponse =
        mockMvc
            .perform(
                get("/api/pingan/training/safety-learning/contents/export?keyword=LEARN-IMPORT-001")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(headers(exportResponse.getContentAsByteArray())).isEqualTo(TEMPLATE_HEADERS);
    assertThat(exportResponse.getContentAsByteArray()).isNotEmpty();
  }

  @Test
  void batchDeletesSafetyLearningContent() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");
    long first =
        postJson(
                "/api/pingan/training/safety-learning/contents",
                payload("安全设施", "消防设施学习", "2026-05-24", "ACTIVE", "BATCH-001"),
                token)
            .path("data")
            .path("id")
            .asLong();
    long second =
        postJson(
                "/api/pingan/training/safety-learning/contents",
                payload("应急管理", "应急预案学习", "2026-05-24", "INACTIVE", "BATCH-002"),
                token)
            .path("data")
            .path("id")
            .asLong();

    mockMvc
        .perform(
            post("/api/pingan/training/safety-learning/contents/batch-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("ids", List.of(first, second))))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    Integer remaining =
        jdbcTemplate.queryForObject(
            "select count(*) from training_safety_learning_content where id in (?, ?) and deleted = 0",
            Integer.class,
            first,
            second);
    assertThat(remaining).isZero();
  }

  @Test
  void enforcesSafetyLearningViewAndManagePermissions() throws Exception {
    String noViewToken =
        permissionToken(
            8501L,
            8501L,
            "training_learning_entry_only",
            "TRAINING_LEARNING_ENTRY_ONLY",
            "PINGAN_TRAINING_SAFETY_LEARNING_ENTRY");
    mockMvc
        .perform(
            get("/api/pingan/training/safety-learning/contents")
                .header("Authorization", "Bearer " + noViewToken))
        .andExpect(status().isForbidden());

    String viewOnlyToken =
        permissionToken(
            8502L,
            8502L,
            "training_learning_view_only",
            "TRAINING_LEARNING_VIEW_ONLY",
            "PINGAN_TRAINING_SAFETY_LEARNING_VIEW");
    mockMvc
        .perform(
            get("/api/pingan/training/safety-learning/contents")
                .header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isOk());

    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    long contentId =
        postJson(
                "/api/pingan/training/safety-learning/contents",
                payload("权限测试", "安全学习权限测试", "2026-05-30", "ACTIVE", "PERM-LEARN-001"),
                adminToken)
            .path("data")
            .path("id")
            .asLong();

    mockMvc
        .perform(
            post("/api/pingan/training/safety-learning/contents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        payload("权限测试", "无管理新增", "2026-05-30", "ACTIVE", "PERM-LEARN-002")))
                .header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            get("/api/pingan/training/safety-learning/contents/template")
                .header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isForbidden());

    MockMultipartFile pdf =
        new MockMultipartFile("file", "权限.pdf", "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));
    mockMvc
        .perform(
            multipart("/api/pingan/training/safety-learning/contents/" + contentId + "/attachment")
                .file(pdf)
                .header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isForbidden());

    MockMultipartFile workbook =
        new MockMultipartFile(
            "file",
            "权限导入.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            importWorkbook("权限测试", "无管理导入", "PERM-LEARN-IMPORT"));
    mockMvc
        .perform(
            multipart("/api/pingan/training/safety-learning/contents/import")
                .file(workbook)
                .header("Authorization", "Bearer " + viewOnlyToken))
        .andExpect(status().isForbidden());

    String createToken =
        permissionToken(
            8503L,
            8503L,
            "training_learning_creator",
            "TRAINING_LEARNING_CREATOR",
            "PINGAN_TRAINING_SAFETY_LEARNING_CREATE");
    JsonNode createAllowed =
        postJson(
                "/api/pingan/training/safety-learning/contents",
                payload("权限测试", "新增权限允许", "2026-05-30", "ACTIVE", "PERM-LEARN-CREATE"),
                createToken)
            .path("data");
    assertThat(createAllowed.path("title").asText()).isEqualTo("新增权限允许");

    String voidToken =
        permissionToken(
            8504L,
            8504L,
            "training_learning_voider",
            "TRAINING_LEARNING_VOIDER",
            "PINGAN_TRAINING_SAFETY_LEARNING_VOID");
    JsonNode voided =
        putJson(
                "/api/pingan/training/safety-learning/contents/" + createAllowed.path("id").asLong(),
                payload("权限测试", "作废权限允许", "2026-05-30", "INACTIVE", createAllowed.path("code").asText()),
                voidToken)
            .path("data");
    assertThat(voided.path("statusLabel").asText()).isEqualTo("作废");

    String downloadToken =
        permissionToken(
            8505L,
            8505L,
            "training_learning_downloader",
            "TRAINING_LEARNING_DOWNLOADER",
            "PINGAN_TRAINING_SAFETY_LEARNING_DOWNLOAD");
    mockMvc
        .perform(
            get("/api/pingan/training/safety-learning/contents/template")
                .header("Authorization", "Bearer " + downloadToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            get("/api/pingan/training/safety-learning/contents/export")
                .header("Authorization", "Bearer " + downloadToken))
        .andExpect(status().isOk());

    String deleteToken =
        permissionToken(
            8506L,
            8506L,
            "training_learning_deleter",
            "TRAINING_LEARNING_DELETER",
            "PINGAN_TRAINING_SAFETY_LEARNING_DELETE");
    mockMvc
        .perform(
            delete("/api/pingan/training/safety-learning/contents/" + contentId)
                .header("Authorization", "Bearer " + deleteToken))
        .andExpect(status().isOk());
  }

  @Test
  void miniSafetyLearningListRequiresTrainingViewPermission() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    long contentId =
        postJson(
                "/api/pingan/training/safety-learning/contents",
                payload("小程序学习", "小程序安全学习权限课", "2026-07-07", "ACTIVE", "MINI-LEARN-RBAC"),
                adminToken)
            .path("data")
            .path("id")
            .asLong();

    String noTrainingToken =
        permissionToken(
            9711L,
            9711L,
            "mini_learning_no_training",
            "MINI_LEARNING_NO_TRAINING",
            "PINGAN_POINTS_VIEW");
    mockMvc
        .perform(
            get("/api/mini/pingan/training/safety-learning/contents")
                .header("Authorization", "Bearer " + noTrainingToken))
        .andExpect(status().isForbidden());

    String trainingViewerToken =
        permissionToken(
            9712L,
            9712L,
            "mini_learning_viewer",
            "MINI_LEARNING_VIEWER",
            "PINGAN_TRAINING_VIEW");
    JsonNode list =
        getJson(
                "/api/mini/pingan/training/safety-learning/contents?keyword=小程序安全学习权限课",
                trainingViewerToken)
            .path("data");

    assertThat(list.path("total").asInt()).isEqualTo(1);
    assertThat(list.path("items").get(0).path("id").asLong()).isEqualTo(contentId);
  }

  @Test
  void miniSafetyLearningCheckInCreatesPointsFlowVisibleOnPc() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    long contentId =
        postJson(
                "/api/pingan/training/safety-learning/contents",
                payload("小程序学习", "每日安全学习打卡", "2026-07-07", "ACTIVE", "MINI-LEARN-CHECKIN"),
                adminToken)
            .path("data")
            .path("id")
            .asLong();

    String learnerToken =
        selfScopedPermissionToken(
            9721L,
            9721L,
            "mini_learning_checkin_user",
            "MINI_LEARNING_CHECKIN_USER",
            1011001L,
            "PINGAN_TRAINING_VIEW",
            "PINGAN_POINTS_VIEW");

    JsonNode checkedIn =
        postJson(
                "/api/mini/pingan/training/safety-learning/contents/" + contentId + "/check-in",
                Map.of("clientRequestId", "mini-learn-checkin-9721"),
                learnerToken)
            .path("data");

    assertThat(checkedIn.path("learningContentId").asLong()).isEqualTo(contentId);
    assertThat(checkedIn.path("checkInStatus").asText()).isEqualTo("CHECKED_IN");
    assertThat(checkedIn.path("pointsAwarded").asInt()).isEqualTo(1);
    assertThat(checkedIn.path("pointsRecord").path("moduleKey").asText()).isEqualTo("points-flow");
    assertThat(checkedIn.path("pointsRecord").path("payload").path("pointsReason").asText()).contains("每日安全学习打卡");

    JsonNode duplicate =
        postJson(
                "/api/mini/pingan/training/safety-learning/contents/" + contentId + "/check-in",
                Map.of("clientRequestId", "mini-learn-checkin-9721"),
                learnerToken)
            .path("data");
    assertThat(duplicate.path("pointsRecord").path("id").asText())
        .isEqualTo(checkedIn.path("pointsRecord").path("id").asText());

    Integer pointsRecords =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from three_check_record
            where module_key = 'points-flow'
              and owner_user_id = ?
              and source_channel = 'WECHAT_MINI_PROGRAM'
              and source_record_id = ?
              and deleted = 0
            """,
            Integer.class,
            9721L,
            "learning-" + contentId + "-user-9721");
    assertThat(pointsRecords).isEqualTo(1);

    JsonNode pcFlow =
        getJson("/api/pingan/three-checks/points-flow/records?pointsReason=每日安全学习打卡", learnerToken).path("data");
    assertThat(pcFlow.path("total").asInt()).isEqualTo(1);
  }

  @Test
  void selfScopedMemberCanViewSafetyLearningForOwnCompanyFilter() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    postJson(
        "/api/pingan/training/safety-learning/contents",
        payload("成员学习", "成员安全学习查看测试", "2026-06-01", "ACTIVE", "MEMBER-LEARN-VIEW"),
        adminToken);
    String memberToken =
        selfScopedPermissionToken(
            8511L,
            8511L,
            "training_learning_member",
            "TRAINING_LEARNING_MEMBER",
            1011001L,
            "PINGAN_TRAINING_SAFETY_LEARNING_VIEW");

    JsonNode result =
        getJson(
                "/api/pingan/training/safety-learning/contents?companyId="
                    + SOURCE_COMPANY_ID
                    + "&status=all",
                memberToken)
            .path("data");

    assertThat(result.path("total").asInt()).isGreaterThanOrEqualTo(1);
  }

  private Map<String, Object> payload(
      String category, String title, String learningDate, String status, String code) {
    return Map.ofEntries(
        Map.entry("companyId", SOURCE_COMPANY_ID),
        Map.entry("category", category),
        Map.entry("title", title),
        Map.entry("content", "学习内容正文"),
        Map.entry("coverImage", "封面图文本"),
        Map.entry("video", "视频文本"),
        Map.entry("learningDate", learningDate),
        Map.entry("durationText", "30分钟"),
        Map.entry("code", code),
        Map.entry("attachmentText", "附件文本"),
        Map.entry("htmlExtract", "不提取"),
        Map.entry("draft", "DRAFT".equals(status) ? "是" : "否"),
        Map.entry("status", status));
  }

  private byte[] importWorkbook(String category, String title, String code) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Data");
      Row header = sheet.createRow(0);
      for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) {
        header.createCell(i).setCellValue(TEMPLATE_HEADERS.get(i));
      }
      Row row = sheet.createRow(1);
      List<String> values =
          List.of(
              "Demo Works Company",
              category,
              title,
              "导入正文",
              "封面",
              "视频",
              "2026-05-22",
              "20分钟",
              code,
              "导入附件.pdf",
              "不提取",
              "否",
              "激活");
      for (int i = 0; i < values.size(); i++) {
        row.createCell(i).setCellValue(values.get(i));
      }
      workbook.write(output);
      return output.toByteArray();
    }
  }

  private List<String> headers(byte[] content) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(content))) {
      Row row = workbook.getSheetAt(0).getRow(0);
      return TEMPLATE_HEADERS.stream().map(header -> row.getCell(TEMPLATE_HEADERS.indexOf(header)).getStringCellValue()).toList();
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
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (?, ?, '{noop}SAFE_TEST_PASSWORD', ?, ?, 'ACTIVE', 0)",
        userId,
        username,
        username,
        SOURCE_COMPANY_ID);
    jdbcTemplate.update("insert into sys_user_role (user_id, role_id) values (?, ?)", userId, roleId);
    return login(username, "SAFE_TEST_PASSWORD");
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
        "insert into sys_user (id, username, password_hash, real_name, org_id, status, deleted) values (?, ?, '{noop}SAFE_TEST_PASSWORD', ?, ?, 'ACTIVE', 0)",
        userId,
        username,
        username,
        orgId);
    jdbcTemplate.update("insert into sys_user_role (user_id, role_id) values (?, ?)", userId, roleId);
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
}


