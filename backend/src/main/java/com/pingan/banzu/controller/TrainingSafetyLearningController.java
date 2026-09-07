package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.TrainingSafetyLearningBatchDeleteRequest;
import com.pingan.banzu.dto.TrainingSafetyLearningContentQuery;
import com.pingan.banzu.dto.TrainingSafetyLearningContentRequest;
import com.pingan.banzu.dto.TrainingSafetyLearningContentResponse;
import com.pingan.banzu.dto.TrainingSafetyLearningImportResult;
import com.pingan.banzu.service.TrainingSafetyLearningService;
import com.pingan.banzu.system.service.SystemExcelService.ExcelFile;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
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
@RequestMapping("/api/pingan/training/safety-learning/contents")
public class TrainingSafetyLearningController {

  private static final MediaType XLSX_MEDIA_TYPE =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final TrainingSafetyLearningService service;

  public TrainingSafetyLearningController(TrainingSafetyLearningService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResult<TrainingSafetyLearningContentResponse>> list(
      @ModelAttribute TrainingSafetyLearningContentQuery query) {
    return ApiResponse.ok(service.list(query));
  }

  @PostMapping
  public ApiResponse<TrainingSafetyLearningContentResponse> create(
      @RequestBody TrainingSafetyLearningContentRequest request) {
    return ApiResponse.ok(service.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<TrainingSafetyLearningContentResponse> update(
      @PathVariable Long id, @RequestBody TrainingSafetyLearningContentRequest request) {
    return ApiResponse.ok(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  @PostMapping("/batch-delete")
  public ApiResponse<Void> batchDelete(@RequestBody TrainingSafetyLearningBatchDeleteRequest request) {
    service.batchDelete(request);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/attachment")
  public ApiResponse<TrainingSafetyLearningContentResponse> uploadAttachment(
      @PathVariable Long id, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.uploadAttachment(id, file));
  }

  @DeleteMapping("/{id}/attachment")
  public ApiResponse<TrainingSafetyLearningContentResponse> removeAttachment(@PathVariable Long id) {
    return ApiResponse.ok(service.removeAttachment(id));
  }

  @PostMapping("/{id}/cover-image")
  public ApiResponse<TrainingSafetyLearningContentResponse> uploadCoverImage(
      @PathVariable Long id, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.uploadCoverImage(id, file));
  }

  @DeleteMapping("/{id}/cover-image")
  public ApiResponse<TrainingSafetyLearningContentResponse> removeCoverImage(@PathVariable Long id) {
    return ApiResponse.ok(service.removeCoverImage(id));
  }

  @PostMapping("/{id}/video")
  public ApiResponse<TrainingSafetyLearningContentResponse> uploadVideo(
      @PathVariable Long id, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.uploadVideo(id, file));
  }

  @DeleteMapping("/{id}/video")
  public ApiResponse<TrainingSafetyLearningContentResponse> removeVideo(@PathVariable Long id) {
    return ApiResponse.ok(service.removeVideo(id));
  }

  @GetMapping("/template")
  public ResponseEntity<byte[]> template() {
    return file(service.template());
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(@ModelAttribute TrainingSafetyLearningContentQuery query) {
    return file(service.exportData(query));
  }

  @PostMapping("/import")
  public ApiResponse<TrainingSafetyLearningImportResult> importData(@RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.importData(file));
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
