package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.SpecialWorkBatchDeleteRequest;
import com.pingan.banzu.dto.SpecialWorkActionRequest;
import com.pingan.banzu.dto.SpecialWorkRecordQuery;
import com.pingan.banzu.dto.SpecialWorkRecordRequest;
import com.pingan.banzu.dto.SpecialWorkRecordResponse;
import com.pingan.banzu.service.SpecialWorkRecordService;
import com.pingan.banzu.system.service.SystemExcelService.ExcelFile;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pingan/special-work/records")
public class SpecialWorkRecordController {

  private final SpecialWorkRecordService service;

  public SpecialWorkRecordController(SpecialWorkRecordService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResult<SpecialWorkRecordResponse>> list(@ModelAttribute SpecialWorkRecordQuery query) {
    return ApiResponse.ok(service.list(query));
  }

  @GetMapping("/{id}")
  public ApiResponse<SpecialWorkRecordResponse> get(@PathVariable Long id) {
    return ApiResponse.ok(service.get(id));
  }

  @PostMapping
  public ApiResponse<SpecialWorkRecordResponse> create(@Valid @RequestBody SpecialWorkRecordRequest request) {
    return ApiResponse.ok(service.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<SpecialWorkRecordResponse> update(
      @PathVariable Long id, @Valid @RequestBody SpecialWorkRecordRequest request) {
    return ApiResponse.ok(service.update(id, request));
  }

  @PostMapping("/{id}/actions/{action}")
  public ApiResponse<SpecialWorkRecordResponse> executeAction(
      @PathVariable Long id,
      @PathVariable String action,
      @RequestBody(required = false) SpecialWorkActionRequest request) {
    return ApiResponse.ok(service.executeAction(id, action, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok(null);
  }

  @PostMapping("/batch-delete")
  public ApiResponse<Void> batchDelete(@Valid @RequestBody SpecialWorkBatchDeleteRequest request) {
    service.batchDelete(request);
    return ApiResponse.ok(null);
  }

  @PostMapping("/{id}/image")
  public ApiResponse<SpecialWorkRecordResponse> uploadImage(
      @PathVariable Long id, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.uploadImage(id, file));
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(@ModelAttribute SpecialWorkRecordQuery query) {
    return file(service.exportData(query));
  }

  private ResponseEntity<byte[]> file(ExcelFile file) {
    String encoded = URLEncoder.encode(file.filename(), StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(file.content());
  }
}
