package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class SystemImportExportApiTest {

  private static final Map<String, List<String>> HEADERS =
      Map.of(
          "company",
              List.of(
                  "编码", "排序", "名称", "简称", "描述", "状态", "地址", "公司类型", "一级", "二级", "三级", "四级",
                  "安全管理员", "一级上报人", "二级上报人", "三级上报人", "L1上报时间", "L2上报时间", "L3上报时间",
                  "附属文件1", "附属文件2", "公司介绍"),
          "department",
              List.of(
                  "编码", "名称", "所属公司", "类型", "子排序", "负责人", "描述", "状态", "顶级", "集团", "一级单位",
                  "二级单位", "领导级别", "公司排序"),
          "team",
              List.of(
                  "名称", "公司", "集团", "一级单位", "二级单位", "车间", "班组作业", "状态", "班组长", "班组成员",
                  "安全员", "积分", "编码", "活跃"),
          "personnel",
              List.of(
                  "编码", "姓名", "所属公司", "企业简称", "所属部门", "所属班组", "积分", "领用积分", "员工类型", "岗位",
                  "手机号", "状态", "系统角色", "部门排序", "管理权重"));

  private static final Map<String, String> TEMPLATE_FILENAMES =
      Map.of(
          "company", "公司管理_导入模板.xlsx",
          "department", "部门管理_导入模板.xlsx",
          "team", "班组管理_导入模板.xlsx",
          "personnel", "人员管理_导入模板.xlsx");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void downloadsTemplatesForAllExcelModulesWithStableFilenames() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

    for (String module : List.of("company", "department", "team", "personnel")) {
      var result =
          mockMvc
              .perform(get("/api/system/" + module + "/template").header("Authorization", "Bearer " + token))
              .andExpect(status().isOk())
              .andReturn();

      assertThat(ContentDisposition.parse(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).getFilename())
          .isEqualTo(TEMPLATE_FILENAMES.get(module));
      assertThat(readHeaders(result.getResponse().getContentAsByteArray())).isEqualTo(HEADERS.get(module));
    }
  }

  @Test
  void exportsCurrentDataWithTemplateHeaders() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

    for (String module : List.of("company", "department", "team", "personnel")) {
      byte[] content =
          mockMvc
              .perform(get("/api/system/" + module + "/export").header("Authorization", "Bearer " + token))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsByteArray();
      assertThat(readHeaders(content)).isEqualTo(HEADERS.get(module));
    }
  }

  @Test
  void rejectsInvalidImportBatchWithRowLevelErrorsAndJobRecord() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");
    byte[] file =
        workbook(
            HEADERS.get("company"),
            List.of(
                List.of("IMP-COMPANY-DUP", "1", "导入重复公司", "", "", "BROKEN", "", "集团", "导入重复公司"),
                List.of("IMP-COMPANY-DUP", "2", "导入重复公司2", "", "", "ACTIVE", "", "集团", "导入重复公司2")));

    JsonNode response =
        objectMapper.readTree(
            mockMvc
                .perform(
                    multipart("/api/system/company/import")
                        .file(new MockMultipartFile("file", "company.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, file))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

    assertThat(response.path("message").asText()).contains("第2行[状态]状态不合法");
    assertThat(response.path("message").asText()).contains("第3行[编码]文件内重复");
    Integer failedJobs =
        jdbcTemplate.queryForObject(
            "select count(*) from sys_import_job where module = 'COMPANY' and status = 'FAILED'",
            Integer.class);
    assertThat(failedJobs).isGreaterThanOrEqualTo(1);
  }

  @Test
  void importsValidCompanyBatchAndRecordsAudit() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");
    byte[] file =
        workbook(
            HEADERS.get("company"),
            List.of(
                List.of(
                    "IMP-COMPANY-OK",
                    "77",
                    "导入成功公司",
                    "导入简称",
                    "导入描述",
                    "ACTIVE",
                    "广州市导入路1号",
                    "集团",
                    "导入成功公司")));

    JsonNode response =
        objectMapper.readTree(
            mockMvc
                .perform(
                    multipart("/api/system/company/import")
                        .file(new MockMultipartFile("file", "company-ok.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, file))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

    assertThat(response.path("data").path("successRows").asInt()).isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from sys_org where org_code = 'IMP-COMPANY-OK' and deleted = 0",
                Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from sys_import_job where module = 'COMPANY' and status = 'SUCCESS' and success_rows = 1",
                Integer.class))
        .isGreaterThanOrEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from sys_access_log where module = 'COMPANY' and action = 'IMPORT'",
                Integer.class))
        .isGreaterThanOrEqualTo(1);
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

  private byte[] workbook(List<String> headers, List<List<String>> rows) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Data");
      Row headerRow = sheet.createRow(0);
      for (int index = 0; index < headers.size(); index++) {
        headerRow.createCell(index).setCellValue(headers.get(index));
      }
      for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
        Row row = sheet.createRow(rowIndex + 1);
        List<String> values = rows.get(rowIndex);
        for (int colIndex = 0; colIndex < values.size(); colIndex++) {
          row.createCell(colIndex).setCellValue(values.get(colIndex));
        }
      }
      workbook.write(output);
      return output.toByteArray();
    }
  }
}

