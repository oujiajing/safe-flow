package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.MiniSafetyLearningCheckInRequest;
import com.pingan.banzu.dto.MiniSafetyLearningCheckInResponse;
import com.pingan.banzu.dto.TrainingSafetyLearningContentQuery;
import com.pingan.banzu.dto.TrainingSafetyLearningContentResponse;
import com.pingan.banzu.service.TrainingSafetyLearningService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini/pingan/training/safety-learning/contents")
public class MiniSafetyLearningController {

  private final TrainingSafetyLearningService service;

  public MiniSafetyLearningController(TrainingSafetyLearningService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResult<TrainingSafetyLearningContentResponse>> list(
      @ModelAttribute TrainingSafetyLearningContentQuery query) {
    return ApiResponse.ok(service.list(query));
  }

  @GetMapping("/{id}")
  public ApiResponse<TrainingSafetyLearningContentResponse> detail(@PathVariable Long id) {
    return ApiResponse.ok(service.detail(id));
  }

  @PostMapping("/{id}/check-in")
  public ApiResponse<MiniSafetyLearningCheckInResponse> checkIn(
      @PathVariable Long id, @RequestBody(required = false) MiniSafetyLearningCheckInRequest request) {
    return ApiResponse.ok(service.checkInFromMiniProgram(id, request));
  }
}
