package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.DataFormatter;
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
class RiskLevelControlApiTest {

  private static final long SOURCE_COMPANY_ID = 4L;
  private static final List<String> HEADERS =
      List.of(
          "公司",
          "风险点",
          "危险源",
          "风险影响因素",
          "事故类型",
          "事故发生的可能性（L）",
          "人员暴露于危险环境中的频繁程度（E）",
          "发生事故可能造成的后果（C）",
          "风险值（D）",
          "风险等级",
          "关键技术与工程措施",
          "关键人员素养与系统管理措施",
          "关键个体防护与应急管理措施",
          "上级单位责任人",
          "责任部门",
          "责任人/联系方式",
          "可能产生的事故隐患",
          "隐患整治措施");

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void createsRiskLibraryWithFirstHazardAndMaintainsHazardDetails() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

    JsonNode created =
        postJson(
                "/api/pingan/risk-level-control/libraries",
                Map.of(
                    "name",
                    "Demo Works风险库-单测",
                    "hazard",
                    hazard("吊篮作业", "高处坠落", "高处坠落", "一般风险")),
                token)
            .path("data");

    long libraryId = created.path("id").asLong();
    assertThat(libraryId).isPositive();
    assertThat(created.path("name").asText()).isEqualTo("Demo Works风险库-单测");
    assertThat(created.path("company").asText()).isEqualTo("Demo Works Company");
    assertThat(created.path("hazardCount").asInt()).isEqualTo(1);

    JsonNode appended =
        postJson(
                "/api/pingan/risk-level-control/libraries/" + libraryId + "/hazards",
                hazard("临时用电", "触电", "触电", "较大风险"),
                token)
            .path("data");
    assertThat(appended.path("libraryId").asLong()).isEqualTo(libraryId);

    JsonNode updated =
        putJson(
                "/api/pingan/risk-level-control/libraries/"
                    + libraryId
                    + "/hazards/"
                    + appended.path("id").asLong(),
                hazard("临时用电", "触电", "触电", "重大风险"),
                token)
            .path("data");
    assertThat(updated.path("riskLevel").asText()).isEqualTo("重大风险");

    JsonNode hazards =
        getJson("/api/pingan/risk-level-control/libraries/" + libraryId + "/hazards", token)
            .path("data");
    assertThat(hazards.path("total").asInt()).isEqualTo(2);
    assertThat(hazards.path("items").get(1).path("riskPoint").asText()).isEqualTo("临时用电");
    assertThat(hazards.path("items").get(1).path("riskLevel").asText()).isEqualTo("重大风险");
  }

  @Test
  void downloadsTemplateAndExportsLibraryWithConfiguredHeaders() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

    byte[] template =
        mockMvc
            .perform(get("/api/pingan/risk-level-control/template").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    assertThat(readHeaders(template)).isEqualTo(HEADERS);

    JsonNode created =
        postJson(
                "/api/pingan/risk-level-control/libraries",
                Map.of("name", "导出风险库-单测", "hazard", hazard("动火作业", "火灾", "火灾", "重大风险")),
                token)
            .path("data");

    byte[] exported =
        mockMvc
            .perform(
                get("/api/pingan/risk-level-control/libraries/" + created.path("id").asLong() + "/export")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    assertThat(readHeaders(exported)).isEqualTo(HEADERS);
    assertThat(readFirstDataRow(exported).get(1)).isEqualTo("动火作业");
  }

  @Test
  void importsWorkbookAsOneLibraryWithOneHazardPerDataRowAndCompanyDictionarySheet() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");
    byte[] file =
        workbook(
            List.of(
                List.of("Demo Works Company", "有限空间", "缺氧", "通风不足", "中毒窒息", "3", "6", "7", "126", "较大风险"),
                List.of("Demo Works Company", "吊装作业", "物体打击", "吊点失效", "物体打击", "1", "6", "15", "90", "一般风险")));

    JsonNode imported =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        multipart("/api/pingan/risk-level-control/import")
                            .file(new MockMultipartFile("file", "risk.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, file))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");

    long libraryId = imported.path("library").path("id").asLong();
    assertThat(imported.path("successRows").asInt()).isEqualTo(2);
    assertThat(imported.path("library").path("hazardCount").asInt()).isEqualTo(2);

    JsonNode hazards =
        getJson("/api/pingan/risk-level-control/libraries/" + libraryId + "/hazards", token)
            .path("data")
            .path("items");
    assertThat(hazards).hasSize(2);
    assertThat(hazards.get(0).path("riskPoint").asText()).isEqualTo("有限空间");
    assertThat(hazards.get(1).path("riskPoint").asText()).isEqualTo("吊装作业");
  }

  @Test
  void hidesLibrariesOutsideCurrentUsersOrganizationScope() throws Exception {
    String adminToken = login("admin", "SAFE_TEST_PASSWORD");
    String memberToken = login("team_member", "SAFE_TEST_PASSWORD");

    JsonNode created =
        postJson(
                "/api/pingan/risk-level-control/libraries",
                Map.of("name", "权限风险库-单测", "hazard", hazard("焊接作业", "火灾", "火灾", "一般风险")),
                adminToken)
            .path("data");

    JsonNode adminList = getJson("/api/pingan/risk-level-control/libraries?keyword=权限风险库-单测", adminToken).path("data");
    assertThat(adminList.path("total").asInt()).isEqualTo(1);

    JsonNode memberList = getJson("/api/pingan/risk-level-control/libraries?keyword=权限风险库-单测", memberToken).path("data");
    assertThat(memberList.path("total").asInt()).isZero();
    assertThat(
            mockMvc
                .perform(
                    get("/api/pingan/risk-level-control/libraries/" + created.path("id").asLong() + "/hazards")
                        .header("Authorization", "Bearer " + memberToken))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(403);
  }

  @Test
  void rejectsLibraryListWhenRiskViewPermissionMissing() throws Exception {
    String token =
        loginPermissionFixtureUser(
            91_001L, "risk_level_entry_only", "ROLE_RISK_LEVEL_ENTRY_ONLY", "PINGAN_RISK_ENTRY");

    mockMvc
        .perform(get("/api/pingan/risk-level-control/libraries").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void allowsRiskViewButRejectsLibraryCreateWhenManagePermissionMissing() throws Exception {
    String token =
        loginPermissionFixtureUser(
            91_002L,
            "risk_level_view_only",
            "ROLE_RISK_LEVEL_VIEW_ONLY",
            "PINGAN_RISK_ENTRY",
            "PINGAN_RISK_VIEW");

    mockMvc
        .perform(get("/api/pingan/risk-level-control/libraries").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/pingan/risk-level-control/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "name",
                            "缺少管理权限风险库",
                            "hazard",
                            hazard("缺少管理权限风险点", "危险源", "其他", "一般风险"))))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void databaseRiskLibraryPermissionsReuseTheFormalRiskFactSource() throws Exception {
    String viewToken =
        loginPermissionFixtureUser(
            91_003L,
            "database_risk_view",
            "ROLE_DATABASE_RISK_VIEW",
            "PINGAN_DATABASE_ENTRY",
            "PINGAN_DATABASE_VIEW");
    String manageToken =
        loginPermissionFixtureUser(
            91_004L,
            "database_risk_manage",
            "ROLE_DATABASE_RISK_MANAGE",
            "PINGAN_DATABASE_ENTRY",
            "PINGAN_DATABASE_VIEW",
            "PINGAN_DATABASE_MANAGE");

    mockMvc
        .perform(get("/api/pingan/risk-level-control/libraries").header("Authorization", "Bearer " + viewToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/pingan/risk-level-control/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "name",
                            "数据库风险隐患库-单测",
                            "hazard",
                            hazard("数据库风险点", "危险源", "其他", "一般风险"))))
                .header("Authorization", "Bearer " + viewToken))
        .andExpect(status().isForbidden());

    JsonNode created =
        postJson(
                "/api/pingan/risk-level-control/libraries",
                Map.of(
                    "name",
                    "数据库风险隐患库-单测",
                    "hazard",
                    hazard("数据库风险点", "危险源", "其他", "一般风险")),
                manageToken)
            .path("data");
    assertThat(created.path("id").asLong()).isPositive();
    assertThat(created.path("hazardCount").asInt()).isEqualTo(1);
  }

  private Map<String, String> hazard(
      String riskPoint, String dangerSource, String accidentType, String riskLevel) {
    return Map.ofEntries(
        Map.entry("companyId", String.valueOf(SOURCE_COMPANY_ID)),
        Map.entry("company", "Demo Works Company"),
        Map.entry("riskPoint", riskPoint),
        Map.entry("dangerSource", dangerSource),
        Map.entry("riskInfluenceFactors", "现场环境变化"),
        Map.entry("accidentType", accidentType),
        Map.entry("likelihood", "1"),
        Map.entry("exposureFrequency", "6"),
        Map.entry("consequence", "15"),
        Map.entry("riskValue", "90"),
        Map.entry("riskLevel", riskLevel),
        Map.entry("engineeringMeasures", "设置防护和联锁"),
        Map.entry("managementMeasures", "班前交底和旁站监督"),
        Map.entry("emergencyMeasures", "佩戴防护用品并配置应急物资"),
        Map.entry("superiorResponsiblePerson", "上级负责人"),
        Map.entry("responsibleDepartment", "安全部"),
        Map.entry("responsibleContact", "张三/13800000000"),
        Map.entry("possibleHazard", "可能产生的事故隐患"),
        Map.entry("rectificationMeasures", "立即整改并复查"));
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

  private List<String> readHeaders(byte[] content) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
      DataFormatter formatter = new DataFormatter();
      Row row = workbook.getSheetAt(0).getRow(0);
      List<String> headers = new ArrayList<>();
      for (int index = 0; index < row.getLastCellNum(); index++) {
        headers.add(formatter.formatCellValue(row.getCell(index)));
      }
      return headers;
    }
  }

  private List<String> readFirstDataRow(byte[] content) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
      DataFormatter formatter = new DataFormatter();
      Row row = workbook.getSheetAt(0).getRow(1);
      List<String> values = new ArrayList<>();
      for (int index = 0; index < row.getLastCellNum(); index++) {
        values.add(formatter.formatCellValue(row.getCell(index)));
      }
      return values;
    }
  }

  private byte[] workbook(List<List<String>> rows) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var data = workbook.createSheet("Data");
      Row headerRow = data.createRow(0);
      for (int index = 0; index < HEADERS.size(); index++) {
        headerRow.createCell(index).setCellValue(HEADERS.get(index));
      }
      for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
        Row row = data.createRow(rowIndex + 1);
        List<String> values = rows.get(rowIndex);
        for (int colIndex = 0; colIndex < values.size(); colIndex++) {
          row.createCell(colIndex).setCellValue(values.get(colIndex));
        }
      }

      var companies = workbook.createSheet("企业");
      companies.createRow(0).createCell(0).setCellValue("编码");
      companies.getRow(0).createCell(1).setCellValue("名称");
      companies.createRow(1).createCell(0).setCellValue("1011");
      companies.getRow(1).createCell(1).setCellValue("Demo Works Company");
      workbook.write(output);
      return output.toByteArray();
    }
  }
}


