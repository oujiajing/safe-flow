package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.AttachmentResponse;
import com.pingan.banzu.dto.MineOverviewResponse;
import com.pingan.banzu.dto.MineRecordListItem;
import com.pingan.banzu.service.MineService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/mini/pingan/me")
public class MiniMineController {

  private final MineService service;

  public MiniMineController(MineService service) {
    this.service = service;
  }

  @GetMapping("/overview")
  public ApiResponse<MineOverviewResponse> overview() {
    return ApiResponse.ok(service.overview());
  }

  @PostMapping("/avatar")
  public ApiResponse<AttachmentResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.uploadAvatar(file));
  }

  @GetMapping("/records/{type}")
  public ApiResponse<PageResult<MineRecordListItem>> records(
      @PathVariable String type,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    return ApiResponse.ok(service.records(type, page, pageSize));
  }
}
