package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.RiskControlHazardRequest;
import com.pingan.banzu.dto.RiskControlHazardResponse;
import com.pingan.banzu.dto.RiskControlImportResult;
import com.pingan.banzu.dto.RiskControlLibraryCreateRequest;
import com.pingan.banzu.dto.RiskControlLibraryResponse;
import com.pingan.banzu.service.RiskLevelControlService;
import com.pingan.banzu.system.service.SystemExcelService.ExcelFile;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pingan/risk-level-control")
public class RiskLevelControlController {

  private static final MediaType XLSX_MEDIA_TYPE =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final RiskLevelControlService service;

  public RiskLevelControlController(RiskLevelControlService service) {
    this.service = service;
  }

  @GetMapping("/libraries")
  public ApiResponse<PageResult<RiskControlLibraryResponse>> libraries(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return ApiResponse.ok(service.listLibraries(keyword, page, pageSize));
  }

  @PostMapping("/libraries")
  public ApiResponse<RiskControlLibraryResponse> createLibrary(
      @RequestBody RiskControlLibraryCreateRequest request) {
    return ApiResponse.ok(service.createLibrary(request));
  }

  @GetMapping("/libraries/{libraryId}/hazards")
  public ApiResponse<PageResult<RiskControlHazardResponse>> hazards(
      @PathVariable Long libraryId,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return ApiResponse.ok(service.listHazards(libraryId, page, pageSize));
  }

  @PostMapping("/libraries/{libraryId}/hazards")
  public ApiResponse<RiskControlHazardResponse> createHazard(
      @PathVariable Long libraryId, @RequestBody RiskControlHazardRequest request) {
    return ApiResponse.ok(service.createHazard(libraryId, request));
  }

  @PutMapping("/libraries/{libraryId}/hazards/{hazardId}")
  public ApiResponse<RiskControlHazardResponse> updateHazard(
      @PathVariable Long libraryId,
      @PathVariable Long hazardId,
      @RequestBody RiskControlHazardRequest request) {
    return ApiResponse.ok(service.updateHazard(libraryId, hazardId, request));
  }

  @GetMapping("/template")
  public ResponseEntity<byte[]> template() {
    return file(service.template());
  }

  @GetMapping("/libraries/{libraryId}/export")
  public ResponseEntity<byte[]> export(@PathVariable Long libraryId) {
    return file(service.exportLibrary(libraryId));
  }

  @PostMapping("/import")
  public ApiResponse<RiskControlImportResult> importWorkbook(@RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.importWorkbook(file));
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
