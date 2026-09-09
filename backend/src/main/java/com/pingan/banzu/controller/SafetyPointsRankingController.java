package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.SafetyPointsIndividualRankingRow;
import com.pingan.banzu.dto.SafetyPointsRankingChartsResponse;
import com.pingan.banzu.dto.SafetyPointsRankingOverviewResponse;
import com.pingan.banzu.dto.SafetyPointsRankingQuery;
import com.pingan.banzu.dto.SafetyPointsTeamRankingRow;
import com.pingan.banzu.service.SafetyPointsRankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pingan/safety-points/ranking")
public class SafetyPointsRankingController {

  private final SafetyPointsRankingService rankingService;

  public SafetyPointsRankingController(SafetyPointsRankingService rankingService) {
    this.rankingService = rankingService;
  }

  @GetMapping("/overview")
  public ApiResponse<SafetyPointsRankingOverviewResponse> overview(
      @ModelAttribute SafetyPointsRankingQuery query) {
    return ApiResponse.ok(rankingService.overview(query));
  }

  @GetMapping("/charts")
  public ApiResponse<SafetyPointsRankingChartsResponse> charts(
      @ModelAttribute SafetyPointsRankingQuery query) {
    return ApiResponse.ok(rankingService.charts(query));
  }

  @GetMapping("/individual")
  public ApiResponse<PageResult<SafetyPointsIndividualRankingRow>> individual(
      @ModelAttribute SafetyPointsRankingQuery query) {
    return ApiResponse.ok(rankingService.individual(query));
  }

  @GetMapping("/team")
  public ApiResponse<PageResult<SafetyPointsTeamRankingRow>> team(
      @ModelAttribute SafetyPointsRankingQuery query) {
    return ApiResponse.ok(rankingService.team(query));
  }
}
