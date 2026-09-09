package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingan.banzu.domain.HazardRectificationOrder;
import com.pingan.banzu.domain.ThreeCheckRecord;
import com.pingan.banzu.mapper.HazardRectificationOrderMapper;
import com.pingan.banzu.mapper.ThreeCheckRecordMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
class ContainerInfrastructureIT {

  private static final Logger log = LoggerFactory.getLogger(ContainerInfrastructureIT.class);
  private static final String MINIO_ACCESS_KEY = "testminio";
  private static final String MINIO_SECRET_KEY = "testminio-secret";
  private static final String MINIO_BUCKET = "safeteam-portfolio-integration";

  @Container
  static final PostgreSQLContainer postgres =
      new PostgreSQLContainer(
              DockerImageName.parse("registry-1.docker.io/pgvector/pgvector:pg16")
                  .asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("demo_safeteam_it")
          .withUsername("pingan")
          .withPassword("pingan");

  @Container
  static final GenericContainer<?> minio =
      new GenericContainer<>(
              DockerImageName.parse(
                  "quay.io/minio/minio@sha256:14cea493d9a34af32f524e538b8346cf79f3321eff8e708c1e2960462bd8936e"))
          .withEnv("MINIO_ROOT_USER", MINIO_ACCESS_KEY)
          .withEnv("MINIO_ROOT_PASSWORD", MINIO_SECRET_KEY)
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/live").forStatusCode(200));

  @DynamicPropertySource
  static void infrastructureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("pingan.storage.provider", () -> "minio");
    registry.add("pingan.storage.endpoint", ContainerInfrastructureIT::minioEndpoint);
    registry.add("pingan.storage.public-endpoint", ContainerInfrastructureIT::minioEndpoint);
    registry.add("pingan.storage.bucket", () -> MINIO_BUCKET);
    registry.add("pingan.storage.access-key", () -> MINIO_ACCESS_KEY);
    registry.add("pingan.storage.secret-key", () -> MINIO_SECRET_KEY);
  }

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired MinioClient minioClient;
  @Autowired HazardRectificationOrderMapper hazardRectificationOrderMapper;
  @Autowired ThreeCheckRecordMapper threeCheckRecordMapper;

  @BeforeEach
  void ensureMinioBucketExists() throws Exception {
    if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(MINIO_BUCKET).build())) {
      minioClient.makeBucket(MakeBucketArgs.builder().bucket(MINIO_BUCKET).build());
    }
  }

  @Test
  void migratesTheRealPostgresSchemaWithoutProductionTestFacts() throws Exception {
    assertThat(
            jdbcTemplate.queryForObject(
                "select version from flyway_schema_history where success = true order by installed_rank desc limit 1",
                String.class))
        .isEqualTo("71");
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from information_schema.table_constraints"
                    + " where constraint_schema = 'public' and constraint_type = 'FOREIGN KEY'",
                Integer.class))
        .isGreaterThanOrEqualTo(38);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from core_relation_integrity_issue",
                Integer.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from three_check_record where lower(record_no) like 'mctest-%'",
                Integer.class))
        .isZero();
    mockMvc
        .perform(get("/actuator/health/readiness"))
        .andExpect(status().isOk())
        .andExpect(
            result ->
                assertThat(result.getResponse().getContentAsString())
                    .isEqualTo("{\"status\":\"UP\"}"));
  }

  @Test
  void permitsRepeatedSoftDeletesWhileKeepingOneActiveBusinessKey() {
    long suffix = System.nanoTime();
    long orgId = 9_000_000_000L + suffix % 100_000;
    long companyId = orgId + 1;
    String learningCode = "IT-LEARN-" + suffix;
    String orderNo = "IT-ORDER-" + suffix;
    long sourceRecordId = 8_000_000_000L + suffix % 100_000;
    long orderId = 0;
    try {
      jdbcTemplate.update(
          """
          insert into sys_org (
            id, parent_id, org_type, org_code, org_name, org_path, status, deleted
          ) values
            (?, null, 'COMPANY', ?, 'integration company', ?, 'ACTIVE', 0),
            (?, ?, 'DEPARTMENT', ?, 'integration department', ?, 'ACTIVE', 0),
            (?, ?, 'TEAM', ?, 'integration team', ?, 'ACTIVE', 0)
          """,
          companyId,
          "IT-COMPANY-" + suffix,
          "/" + companyId,
          companyId + 1,
          companyId,
          "IT-DEPARTMENT-" + suffix,
          "/" + companyId + "/" + (companyId + 1),
          companyId + 2,
          companyId + 1,
          "IT-TEAM-" + suffix,
          "/" + companyId + "/" + (companyId + 1) + "/" + (companyId + 2));
      repeatCreateAndDelete(
          """
          insert into sys_org_content_profile (org_id, title, status, deleted)
          values (?, 'integration profile', 'DRAFT', 0)
          returning id
          """,
          orgId);
      assertSecondActiveInsertFails(
          """
          insert into sys_org_content_profile (org_id, title, status, deleted)
          values (?, 'integration profile', 'DRAFT', 0)
          """,
          orgId);

      repeatCreateAndDelete(
          """
          insert into sys_team_check_template (
            name, company_org_id, department_org_id, team_org_id, inspection_stage, status, deleted
          ) values ('integration template', ?, null, null, 'PRE_SHIFT', 'ACTIVE', 0)
          returning id
          """,
          companyId);
      assertSecondActiveInsertFails(
          """
          insert into sys_team_check_template (
            name, company_org_id, department_org_id, team_org_id, inspection_stage, status, deleted
          ) values ('integration template', ?, null, null, 'PRE_SHIFT', 'ACTIVE', 0)
          """,
          companyId);

      repeatCreateAndDelete(
          """
          insert into training_safety_learning_content (
            company_id, category, title, learning_date, code, status, deleted
          ) values (?, 'IT', 'integration learning', current_date, ?, 'DRAFT', 0)
          returning id
          """,
          companyId,
          learningCode);
      assertSecondActiveInsertFails(
          """
          insert into training_safety_learning_content (
            company_id, category, title, learning_date, code, status, deleted
          ) values (?, 'IT', 'integration learning', current_date, ?, 'DRAFT', 0)
          """,
          companyId,
          learningCode);

      repeatCreateAndDelete(
          """
          insert into hazard_rectification_order (
            order_no, source_type, source_module_key, source_record_id,
            company_id, department_id, team_id, business_date, status, deleted
          ) values (?, 'THREE_CHECK', 'safety-check', ?, ?, ?, ?, current_date, 'PENDING_ISSUE', 0)
          returning id
          """,
          orderNo,
          sourceRecordId,
          companyId,
          companyId + 1,
          companyId + 2);
      assertSecondActiveInsertFails(
          """
          insert into hazard_rectification_order (
            order_no, source_type, source_module_key, source_record_id,
            company_id, department_id, team_id, business_date, status, deleted
          ) values (?, 'THREE_CHECK', 'safety-check', ?, ?, ?, ?, current_date, 'PENDING_ISSUE', 0)
          """,
          orderNo,
          sourceRecordId,
          companyId,
          companyId + 1,
          companyId + 2);

      orderId =
          jdbcTemplate.queryForObject(
              "select id from hazard_rectification_order where order_no = ? and deleted = 0",
              Long.class,
              orderNo);
      repeatCreateAndDelete(
          """
          insert into hazard_rectification_order_item (
            order_id, source_line_id, source_line_index, check_item, rectification_status, deleted
          ) values (?, 'line-1', 1, 'integration item', 'PENDING', 0)
          returning id
          """,
          orderId);
      assertSecondActiveInsertFails(
          """
          insert into hazard_rectification_order_item (
            order_id, source_line_id, source_line_index, check_item, rectification_status, deleted
          ) values (?, 'line-1', 1, 'integration item', 'PENDING', 0)
          """,
          orderId);
    } finally {
      if (orderId > 0) {
        jdbcTemplate.update("delete from hazard_rectification_order_item where order_id = ?", orderId);
      }
      jdbcTemplate.update("delete from hazard_rectification_order where order_no = ?", orderNo);
      jdbcTemplate.update("delete from training_safety_learning_content where code = ?", learningCode);
      jdbcTemplate.update(
          "delete from sys_team_check_template where company_org_id = ?", companyId);
      jdbcTemplate.update("delete from sys_org_content_profile where org_id = ?", orgId);
      jdbcTemplate.update(
          "delete from sys_org where id in (?, ?, ?)",
          companyId + 2,
          companyId + 1,
          companyId);
    }
  }

  @Test
  void completesLoginCreateReadAndAttachmentRoundTripAcrossPostgresAndMinio() throws Exception {
    String token = login();
    String requestId = "it-e2e-" + System.nanoTime();
    long recordId = 0;
    String objectKey = null;
    try {
      JsonNode created =
          postJson(
                  "/api/pingan/three-checks/pre-shift-inspection/records",
                  token,
                  Map.of(
                      "companyId", 4,
                      "departmentId", 101109,
                      "teamId", 1011001,
                      "ownerUserId", 2,
                      "businessDate", LocalDate.now().toString(),
                      "clientRequestId", requestId,
                      "payload", Map.of("statusLabel", "待检查", "source", "container-e2e")))
              .path("data");
      recordId = created.path("id").asLong();

      MockMultipartFile image =
          new MockMultipartFile("file", "e2e.jpg", "image/jpeg", tinyJpeg());
      mockMvc
          .perform(
              multipart(
                      "/api/pingan/three-checks/pre-shift-inspection/records/"
                          + recordId
                          + "/attachments")
                  .file(image)
                  .param("fileKind", "IMAGE")
                  .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk());

      JsonNode detail =
          getJson(
                  "/api/pingan/three-checks/pre-shift-inspection/records/" + recordId,
                  token)
              .path("data");
      assertThat(detail.path("id").asLong()).isEqualTo(recordId);
      assertThat(detail.path("payload").path("source").asText()).isEqualTo("container-e2e");

      objectKey =
          jdbcTemplate.queryForObject(
              "select object_key from biz_attachment where biz_id = ? and original_name = 'e2e.jpg' and deleted = 0",
              String.class,
              recordId);
      assertThat(
              minioClient.statObject(
                  StatObjectArgs.builder().bucket(MINIO_BUCKET).object(objectKey).build()))
          .isNotNull();
    } finally {
      if (objectKey != null) {
        minioClient.removeObject(
            RemoveObjectArgs.builder().bucket(MINIO_BUCKET).object(objectKey).build());
      }
      if (recordId > 0) {
        jdbcTemplate.update(
            "delete from biz_attachment where biz_id = ? and original_name = 'e2e.jpg'",
            recordId);
        jdbcTemplate.update("delete from three_check_record where id = ?", recordId);
      }
    }
  }

  @Test
  void concurrentClientRequestsReturnTheSameSuccessfulRecord() throws Exception {
    String token = login();
    String requestId = "it-concurrency-" + System.nanoTime();
    String distinctRequestId = requestId + "-distinct";
    CyclicBarrier start = new CyclicBarrier(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Callable<JsonNode> create =
          () -> {
            start.await();
            return postJson(
                    "/api/mini/pingan/three-checks/pre-shift-inspection/records",
                    token,
                    miniCreateRequest(requestId))
                .path("data");
          };

      var first = executor.submit(create);
      var second = executor.submit(create);
      List<Long> recordIds =
          List.of(first.get().path("id").asLong(), second.get().path("id").asLong());
      assertThat(recordIds.get(0)).isPositive().isEqualTo(recordIds.get(1));
      assertThat(
              jdbcTemplate.queryForObject(
                  "select count(*) from three_check_record where client_request_id = ?",
                  Integer.class,
                  requestId))
          .isOne();

      long distinctId =
          postJson(
                  "/api/mini/pingan/three-checks/pre-shift-inspection/records",
                  token,
                  miniCreateRequest(distinctRequestId))
              .path("data")
              .path("id")
              .asLong();
      assertThat(distinctId).isPositive().isNotEqualTo(recordIds.get(0));
    } finally {
      executor.shutdownNow();
      jdbcTemplate.update(
          """
          delete from biz_change_history
          where biz_type = 'THREE_CHECK_PRE_SHIFT_INSPECTION'
            and biz_id in (
              select id from three_check_record
              where client_request_id in (?, ?)
            )
          """,
          requestId,
          distinctRequestId);
      jdbcTemplate.update(
          """
          delete from biz_status_log
          where biz_type = 'THREE_CHECK_PRE_SHIFT_INSPECTION'
            and biz_id in (
              select id from three_check_record
              where client_request_id in (?, ?)
            )
          """,
          requestId,
          distinctRequestId);
      jdbcTemplate.update(
          "delete from three_check_record where client_request_id in (?, ?)",
          requestId,
          distinctRequestId);
      jdbcTemplate.update(
          "delete from shift_task where client_request_id in (?, ?)",
          requestId,
          distinctRequestId);
    }
  }

  @Test
  void concurrentUpdatesUseTheStoredVersionAsAnAtomicCondition() throws Exception {
    String recordNo = "IT-CAS-" + System.nanoTime();
    Long recordId =
        jdbcTemplate.queryForObject(
            """
            insert into three_check_record (
              module_key, record_no, company_id, department_id, team_id, owner_user_id,
              business_date, status, payload_json, version, source_channel, deleted
            ) values (
              'pre-shift-inspection', ?, 4, 101109, 1011001, 2,
              current_date, 'DRAFT', '{}', 0, 'PC', 0
            )
            returning id
            """,
            Long.class,
            recordNo);
    CyclicBarrier loaded = new CyclicBarrier(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      var first =
          executor.submit(() -> updateRecordAtBarrier(recordId, "{\"writer\":\"first\"}", loaded));
      var second =
          executor.submit(() -> updateRecordAtBarrier(recordId, "{\"writer\":\"second\"}", loaded));

      assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(0, 1);
      assertThat(
              jdbcTemplate.queryForObject(
                  "select version from three_check_record where id = ?", Integer.class, recordId))
          .isEqualTo(1);
    } finally {
      executor.shutdownNow();
      jdbcTemplate.update("delete from three_check_record where id = ?", recordId);
    }
  }

  @Test
  void concurrentHazardActionsUseVersionAndPredecessorStatusAtomically() throws Exception {
    String orderNo = "IT-ORDER-CAS-" + System.nanoTime();
    Long orderId =
        jdbcTemplate.queryForObject(
            """
            insert into hazard_rectification_order (
              order_no, source_type, source_module_key, company_id, department_id, team_id,
              business_date, hazard_count, status, acceptance_user_id, version, deleted
            ) values (
              ?, 'MANUAL', 'manual', 4, 101109, 1011001,
              current_date, 1, 'PENDING_ACCEPTANCE', 3, 0, 0
            )
            returning id
            """,
            Long.class,
            orderNo);
    CyclicBarrier loaded = new CyclicBarrier(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      var accepted =
          executor.submit(
              () ->
                  updateHazardOrderAtBarrier(
                      orderId, "CLOSED", "PASS", loaded));
      var rejected =
          executor.submit(
              () ->
                  updateHazardOrderAtBarrier(
                      orderId, "PENDING_RECTIFY", "REJECT", loaded));

      assertThat(List.of(accepted.get(), rejected.get())).containsExactlyInAnyOrder(0, 1);
      assertThat(
              jdbcTemplate.queryForObject(
                  "select version from hazard_rectification_order where id = ?",
                  Integer.class,
                  orderId))
          .isEqualTo(1);
    } finally {
      executor.shutdownNow();
      jdbcTemplate.update("delete from hazard_rectification_order where id = ?", orderId);
    }
  }

  @Test
  void concurrentRemindersDoNotLoseAnIncrement() throws Exception {
    String token = login();
    String recordNo = "IT-REMIND-" + System.nanoTime();
    Long recordId =
        jdbcTemplate.queryForObject(
            """
            insert into three_check_record (
              module_key, record_no, company_id, department_id, team_id, owner_user_id,
              business_date, status, payload_json, reminder_count, version, source_channel, deleted
            ) values (
              'pre-shift-inspection', ?, 4, 101109, 1011001, 2,
              current_date, 'DRAFT', '{}', 0, 0, 'PC', 0
            )
            returning id
            """,
            Long.class,
            recordNo);
    CyclicBarrier start = new CyclicBarrier(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Callable<JsonNode> remind =
          () -> {
            start.await();
            return postJson(
                    "/api/pingan/three-checks/pre-shift-inspection/records/"
                        + recordId
                        + "/remind",
                    token,
                    Map.of())
                .path("data");
          };

      var first = executor.submit(remind);
      var second = executor.submit(remind);
      assertThat(
              List.of(
                  first.get().path("reminderCount").asInt(),
                  second.get().path("reminderCount").asInt()))
          .containsExactlyInAnyOrder(1, 2);
      assertThat(
              jdbcTemplate.queryForObject(
                  "select reminder_count from three_check_record where id = ?",
                  Integer.class,
                  recordId))
          .isEqualTo(2);
    } finally {
      executor.shutdownNow();
      jdbcTemplate.update(
          "delete from biz_notification_recipient where notification_id in"
              + " (select id from biz_notification where biz_id = ?)",
          recordId);
      jdbcTemplate.update("delete from biz_notification where biz_id = ?", recordId);
      jdbcTemplate.update("delete from biz_remind_record where biz_id = ?", recordId);
      jdbcTemplate.update("delete from biz_change_history where biz_id = ?", recordId);
      jdbcTemplate.update("delete from biz_status_log where biz_id = ?", recordId);
      jdbcTemplate.update("delete from three_check_record where id = ?", recordId);
    }
  }

  @Test
  void aggregatesAndPagesSafetyPointsInsidePostgresWithBoundedLatency() throws Exception {
    String marker = "PG-PERF-" + System.nanoTime();
    String token = login();
    try {
      jdbcTemplate.update(
          """
          insert into three_check_record (
            module_key, record_no, company_id, department_id, team_id, owner_user_id,
            business_date, status, payload_json, source_channel, deleted
          )
          select
            'points-flow',
            ? || '-' || value,
            4,
            101109,
            case when value % 2 = 0 then 1011001 else 1011002 end,
            2,
            date '2026-07-01' + ((value - 1) % 10),
            'OPENED',
            jsonb_build_object(
              'user', '容量用户-' || ((value - 1) % 200),
              'pointsReason', '容量基线',
              'pointsChange', case when value % 7 = 0 then '扣分' else '加分' end,
              'pointsQuantity', (value % 5) + 1
            )::text,
            'PC',
            0
          from generate_series(1, 5000) value
          """,
          marker);

      String path =
          "/api/pingan/safety-points/ranking/individual"
              + "?dateStart=2026-07-01&dateEnd=2026-07-10"
              + "&keyword=容量基线&page=2&pageSize=10";
      List<Long> elapsedMillis = new ArrayList<>();
      JsonNode page = null;
      for (int run = 0; run < 10; run++) {
        long started = System.nanoTime();
        page = getJson(path, token).path("data");
        elapsedMillis.add(Duration.ofNanos(System.nanoTime() - started).toMillis());
      }
      Collections.sort(elapsedMillis);
      long p95 = elapsedMillis.get(9);
      int responseBytes = objectMapper.writeValueAsBytes(page).length;
      log.info(
          "PERF-001 PostgreSQL points baseline: rows=5000, participants=200, pageSize=10, p95Ms={}, responseBytes={}",
          p95,
          responseBytes);

      assertThat(page.path("total").asInt()).isEqualTo(200);
      assertThat(page.path("items")).hasSize(10);
      assertThat(page.path("items").get(0).path("rank").asInt()).isEqualTo(11);
      assertThat(responseBytes).isLessThan(20_000);
      assertThat(p95).isLessThan(2_000);

      String common =
          "?dateStart=2026-07-01&dateEnd=2026-07-10&keyword=容量基线";
      JsonNode emptyPage =
          getJson(
                  "/api/pingan/safety-points/ranking/individual"
                      + common
                      + "&page=999&pageSize=10",
                  token)
              .path("data");
      JsonNode overview =
          getJson("/api/pingan/safety-points/ranking/overview" + common, token).path("data");
      JsonNode teams =
          getJson(
                  "/api/pingan/safety-points/ranking/team"
                      + common
                      + "&rankBy=TEAM&page=1&pageSize=1",
                  token)
              .path("data");
      JsonNode charts =
          getJson(
                  "/api/pingan/safety-points/ranking/charts" + common + "&rankBy=TEAM",
                  token)
              .path("data");
      assertThat(emptyPage.path("total").asInt()).isEqualTo(200);
      assertThat(emptyPage.path("items")).isEmpty();
      assertThat(overview.path("participantCount").asInt()).isEqualTo(200);
      assertThat(teams.path("total").asInt()).isEqualTo(2);
      assertThat(teams.path("items")).hasSize(1);
      assertThat(charts.path("trend")).isNotEmpty();
      assertThat(charts.path("sources").get(0).path("name").asText()).isEqualTo("容量基线");
      assertThat(charts.path("teamTop5")).hasSize(2);
    } finally {
      jdbcTemplate.update("delete from three_check_record where record_no like ?", marker + "-%");
    }
  }

  private Map<String, Object> miniCreateRequest(String clientRequestId) {
    return Map.of(
        "companyId", 4,
        "departmentId", 101109,
        "teamId", 1011001,
        "ownerUserId", 2,
        "businessDate", LocalDate.now().toString(),
        "clientRequestId", clientRequestId,
        "payload", Map.of("source", "concurrent-container-test"));
  }

  private int updateRecordAtBarrier(Long recordId, String payloadJson, CyclicBarrier loaded)
      throws Exception {
    ThreeCheckRecord record = threeCheckRecordMapper.selectById(recordId);
    int expectedVersion = record.version;
    record.payloadJson = payloadJson;
    record.version = expectedVersion + 1;
    loaded.await();
    return threeCheckRecordMapper.updateByIdAndVersion(record, expectedVersion);
  }

  private int updateHazardOrderAtBarrier(
      Long orderId, String status, String acceptanceResult, CyclicBarrier loaded) throws Exception {
    HazardRectificationOrder order = hazardRectificationOrderMapper.selectById(orderId);
    int expectedVersion = order.version;
    String expectedStatus = order.status;
    order.status = status;
    order.acceptanceResult = acceptanceResult;
    order.version = expectedVersion + 1;
    loaded.await();
    return hazardRectificationOrderMapper.updateByIdAndVersionAndStatus(
        order, expectedVersion, expectedStatus);
  }

  private String login() throws Exception {
    return postJson(
            "/api/auth/login",
            null,
            Map.of("username", "admin", "password", "SAFE_TEST_PASSWORD"))
        .path("data")
        .path("accessToken")
        .asText();
  }

  private void repeatCreateAndDelete(String insertSql, Object... args) {
    for (int round = 0; round < 3; round++) {
      Long id = jdbcTemplate.queryForObject(insertSql, Long.class, args);
      String table = insertSql.substring(insertSql.indexOf("into ") + 5, insertSql.indexOf(" (")).trim();
      jdbcTemplate.update("update " + table + " set deleted = 1 where id = ?", id);
    }
    jdbcTemplate.queryForObject(insertSql, Long.class, args);
  }

  private void assertSecondActiveInsertFails(String insertSql, Object... args) {
    assertThatThrownBy(() -> jdbcTemplate.update(insertSql, args))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private JsonNode postJson(String url, String token, Object body) throws Exception {
    var request =
        post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(body));
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return objectMapper.readTree(
        mockMvc
            .perform(request)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray());
  }

  private JsonNode getJson(String url, String token) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(get(url).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray());
  }

  private static String minioEndpoint() {
    return "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
  }

  private static byte[] tinyJpeg() {
    return new byte[] {
      (byte) 0xff,
      (byte) 0xd8,
      (byte) 0xff,
      (byte) 0xe0,
      0x00,
      0x10,
      0x4a,
      0x46,
      0x49,
      0x46,
      0x00,
      0x01,
      (byte) 0xff,
      (byte) 0xd9
    };
  }
}



