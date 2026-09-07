package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.MiniExamResponse;
import com.pingan.banzu.dto.MiniExamProgressRequest;
import com.pingan.banzu.dto.MiniExamSubmissionRequest;
import com.pingan.banzu.service.TrainingExamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini/pingan/training/exams")
public class MiniTrainingExamController {

  private final TrainingExamService service;

  public MiniTrainingExamController(TrainingExamService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResult<MiniExamResponse>> exams(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return ApiResponse.ok(service.listMyExams(status, page, pageSize));
  }

  @GetMapping("/{taskId}")
  public ApiResponse<MiniExamResponse> exam(@PathVariable Long taskId) {
    return ApiResponse.ok(service.myExam(taskId));
  }

  @PostMapping("/{taskId}/submissions")
  public ApiResponse<MiniExamResponse> submit(
      @PathVariable Long taskId, @RequestBody MiniExamSubmissionRequest request) {
    return ApiResponse.ok(service.submitMyExam(taskId, request));
  }

  @PutMapping("/{taskId}/progress")
  public ApiResponse<MiniExamResponse> saveProgress(
      @PathVariable Long taskId, @RequestBody MiniExamProgressRequest request) {
    return ApiResponse.ok(service.saveMyExamProgress(taskId, request));
  }
}
