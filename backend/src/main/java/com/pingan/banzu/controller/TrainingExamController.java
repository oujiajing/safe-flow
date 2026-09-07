package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.TrainingExamBatchDeleteRequest;
import com.pingan.banzu.dto.TrainingExamPaperQuery;
import com.pingan.banzu.dto.TrainingExamPaperRequest;
import com.pingan.banzu.dto.TrainingExamPaperResponse;
import com.pingan.banzu.dto.TrainingExamPdfConfirmRequest;
import com.pingan.banzu.dto.TrainingExamPdfConfirmResult;
import com.pingan.banzu.dto.TrainingExamPdfPreviewResponse;
import com.pingan.banzu.dto.TrainingExamQuestionBankImportResult;
import com.pingan.banzu.dto.TrainingExamQuestionBankQuery;
import com.pingan.banzu.dto.TrainingExamQuestionBankRequest;
import com.pingan.banzu.dto.TrainingExamQuestionBankResponse;
import com.pingan.banzu.dto.TrainingExamQuestionImportResult;
import com.pingan.banzu.dto.TrainingExamResultDetailResponse;
import com.pingan.banzu.dto.TrainingExamResultQuery;
import com.pingan.banzu.dto.TrainingExamResultRequest;
import com.pingan.banzu.dto.TrainingExamResultResponse;
import com.pingan.banzu.dto.TrainingExamReviewRequest;
import com.pingan.banzu.dto.TrainingExamTaskDetailResponse;
import com.pingan.banzu.dto.TrainingExamTaskQuery;
import com.pingan.banzu.dto.TrainingExamTaskRequest;
import com.pingan.banzu.dto.TrainingExamTaskResponse;
import com.pingan.banzu.service.TrainingExamService;
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
@RequestMapping("/api/pingan/training")
public class TrainingExamController {

  private static final MediaType XLSX_MEDIA_TYPE =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final TrainingExamService service;

  public TrainingExamController(TrainingExamService service) {
    this.service = service;
  }

  @GetMapping("/exam-tasks")
  public ApiResponse<PageResult<TrainingExamTaskResponse>> tasks(@ModelAttribute TrainingExamTaskQuery query) {
    return ApiResponse.ok(service.listTasks(query));
  }

  @PostMapping("/exam-tasks")
  public ApiResponse<TrainingExamTaskResponse> createTask(@RequestBody TrainingExamTaskRequest request) {
    return ApiResponse.ok(service.createTask(request));
  }

  @GetMapping("/exam-question-bank")
  public ApiResponse<PageResult<TrainingExamQuestionBankResponse>> questionBank(
      @ModelAttribute TrainingExamQuestionBankQuery query) {
    return ApiResponse.ok(service.listQuestionBank(query));
  }

  @PostMapping("/exam-question-bank")
  public ApiResponse<TrainingExamQuestionBankResponse> createQuestionBank(
      @RequestBody TrainingExamQuestionBankRequest request) {
    return ApiResponse.ok(service.createQuestionBank(request));
  }

  @PutMapping("/exam-question-bank/{id}")
  public ApiResponse<TrainingExamQuestionBankResponse> updateQuestionBank(
      @PathVariable Long id, @RequestBody TrainingExamQuestionBankRequest request) {
    return ApiResponse.ok(service.updateQuestionBank(id, request));
  }

  @DeleteMapping("/exam-question-bank/{id}")
  public ApiResponse<Void> deleteQuestionBank(@PathVariable Long id) {
    service.deleteQuestionBank(id);
    return ApiResponse.ok();
  }

  @PostMapping("/exam-question-bank/batch-delete")
  public ApiResponse<Void> batchDeleteQuestionBank(
      @RequestBody TrainingExamBatchDeleteRequest request) {
    service.batchDeleteQuestionBank(request);
    return ApiResponse.ok();
  }

  @GetMapping("/exam-papers")
  public ApiResponse<PageResult<TrainingExamPaperResponse>> papers(
      @ModelAttribute TrainingExamPaperQuery query) {
    return ApiResponse.ok(service.listPapers(query));
  }

  @PostMapping("/exam-papers")
  public ApiResponse<TrainingExamPaperResponse> createPaper(
      @RequestBody TrainingExamPaperRequest request) {
    return ApiResponse.ok(service.createPaper(request));
  }

  @PutMapping("/exam-papers/{id}")
  public ApiResponse<TrainingExamPaperResponse> updatePaper(
      @PathVariable Long id, @RequestBody TrainingExamPaperRequest request) {
    return ApiResponse.ok(service.updatePaper(id, request));
  }

  @DeleteMapping("/exam-papers/{id}")
  public ApiResponse<Void> deletePaper(@PathVariable Long id) {
    service.deletePaper(id);
    return ApiResponse.ok();
  }

  @PostMapping("/exam-papers/batch-delete")
  public ApiResponse<Void> batchDeletePapers(
      @RequestBody TrainingExamBatchDeleteRequest request) {
    service.batchDeletePapers(request);
    return ApiResponse.ok();
  }

  @GetMapping("/exam-question-bank/template")
  public ResponseEntity<byte[]> questionBankTemplate() {
    return file(service.questionBankTemplate());
  }

  @PostMapping("/exam-question-bank/import")
  public ApiResponse<TrainingExamQuestionBankImportResult> importQuestionBank(
      @RequestParam Long companyId,
      @RequestParam(required = false) Long departmentId,
      @RequestParam(required = false) Long teamId,
      @org.springframework.web.bind.annotation.RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.importQuestionBank(companyId, departmentId, teamId, file));
  }

  @PostMapping("/exam-question-bank/pdf/preview")
  public ApiResponse<TrainingExamPdfPreviewResponse> previewQuestionBankPdf(
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.previewQuestionBankPdf(file));
  }

  @PostMapping("/exam-question-bank/pdf/confirm")
  public ApiResponse<TrainingExamPdfConfirmResult> confirmQuestionBankPdf(
      @RequestBody TrainingExamPdfConfirmRequest request) {
    return ApiResponse.ok(service.confirmQuestionBankPdf(request));
  }

  @GetMapping("/exam-question-bank/export")
  public ResponseEntity<byte[]> exportQuestionBank(@ModelAttribute TrainingExamQuestionBankQuery query) {
    return file(service.exportQuestionBank(query));
  }

  @GetMapping("/exam-tasks/questions/template")
  public ResponseEntity<byte[]> questionTemplate() {
    return file(service.questionTemplate());
  }

  @PostMapping("/exam-tasks/questions/import")
  public ApiResponse<TrainingExamQuestionImportResult> importQuestions(
      @org.springframework.web.bind.annotation.RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.importQuestions(file));
  }

  @GetMapping("/exam-tasks/{id}/questions/export")
  public ResponseEntity<byte[]> exportTaskQuestions(@PathVariable Long id) {
    return file(service.exportTaskQuestions(id));
  }

  @GetMapping("/exam-tasks/{id}")
  public ApiResponse<TrainingExamTaskDetailResponse> taskDetail(@PathVariable Long id) {
    return ApiResponse.ok(service.taskDetail(id));
  }

  @DeleteMapping("/exam-tasks/{id}")
  public ApiResponse<Void> deleteTask(@PathVariable Long id) {
    service.deleteTask(id);
    return ApiResponse.ok();
  }

  @PostMapping("/exam-tasks/batch-delete")
  public ApiResponse<Void> batchDeleteTasks(@RequestBody TrainingExamBatchDeleteRequest request) {
    service.batchDeleteTasks(request);
    return ApiResponse.ok();
  }

  @GetMapping("/exam-tasks/{id}/results")
  public ApiResponse<PageResult<TrainingExamResultResponse>> taskResults(
      @PathVariable Long id, @ModelAttribute TrainingExamResultQuery query) {
    return ApiResponse.ok(service.listTaskResults(id, query));
  }

  @GetMapping("/exam-results")
  public ApiResponse<PageResult<TrainingExamResultResponse>> results(@ModelAttribute TrainingExamResultQuery query) {
    return ApiResponse.ok(service.listResults(query));
  }

  @PostMapping("/exam-results")
  public ApiResponse<TrainingExamResultResponse> createResult(@RequestBody TrainingExamResultRequest request) {
    return ApiResponse.ok(service.createResult(request));
  }

  @GetMapping("/exam-results/{id}")
  public ApiResponse<TrainingExamResultDetailResponse> resultDetail(@PathVariable Long id) {
    return ApiResponse.ok(service.resultDetail(id));
  }

  @PostMapping("/exam-results/{id}/review")
  public ApiResponse<TrainingExamResultDetailResponse> reviewResult(
      @PathVariable Long id, @RequestBody TrainingExamReviewRequest request) {
    return ApiResponse.ok(service.reviewResult(id, request));
  }

  @DeleteMapping("/exam-results/{id}")
  public ApiResponse<Void> deleteResult(@PathVariable Long id) {
    service.deleteResult(id);
    return ApiResponse.ok();
  }

  @PostMapping("/exam-results/batch-delete")
  public ApiResponse<Void> batchDeleteResults(@RequestBody TrainingExamBatchDeleteRequest request) {
    service.batchDeleteResults(request);
    return ApiResponse.ok();
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
