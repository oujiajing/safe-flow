package com.pingan.banzu.system.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.system.dto.SystemImportResult;
import com.pingan.banzu.system.service.SystemExcelService;
import com.pingan.banzu.system.service.SystemExcelService.ExcelFile;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/system/{module}")
public class SystemImportExportController {

  private static final MediaType XLSX_MEDIA_TYPE =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final SystemExcelService excelService;

  public SystemImportExportController(SystemExcelService excelService) {
    this.excelService = excelService;
  }

  @GetMapping("/template")
  public ResponseEntity<byte[]> template(@PathVariable String module) {
    return file(excelService.template(module));
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(@PathVariable String module, @RequestParam Map<String, String> params) {
    return file(excelService.exportData(module, params));
  }

  @PostMapping("/import")
  public ApiResponse<SystemImportResult> importData(@PathVariable String module, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(excelService.importData(module, file));
  }

  private ResponseEntity<byte[]> file(ExcelFile file) {
    return ResponseEntity.ok()
        .contentType(XLSX_MEDIA_TYPE)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build().toString())
        .body(file.content());
  }
}
