package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.SafetyLedgerDocumentBatchDeleteRequest;
import com.pingan.banzu.dto.SafetyLedgerDocumentQuery;
import com.pingan.banzu.dto.SafetyLedgerDocumentResponse;
import com.pingan.banzu.service.SafetyLedgerDocumentService;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pingan/safety-ledger/documents")
public class SafetyLedgerDocumentController {

  private final SafetyLedgerDocumentService service;

  public SafetyLedgerDocumentController(SafetyLedgerDocumentService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResult<SafetyLedgerDocumentResponse>> list(@ModelAttribute SafetyLedgerDocumentQuery query) {
    return ApiResponse.ok(service.list(query));
  }

  @PostMapping
  public ApiResponse<SafetyLedgerDocumentResponse> create(
      @RequestParam String ledgerKey,
      @RequestParam String name,
      @RequestParam Long companyId,
      @RequestParam(required = false) String richText,
      @RequestParam(required = false) String department,
      @RequestParam(required = false) String team,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) LocalDate date,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.create(ledgerKey, name, companyId, richText, department, team, type, date, file));
  }

  @PostMapping("/{id}")
  public ApiResponse<SafetyLedgerDocumentResponse> update(
      @PathVariable Long id,
      @RequestParam String ledgerKey,
      @RequestParam String name,
      @RequestParam Long companyId,
      @RequestParam(required = false) String richText,
      @RequestParam(required = false) String department,
      @RequestParam(required = false) String team,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) LocalDate date,
      @RequestParam(value = "file", required = false) MultipartFile file) {
    return ApiResponse.ok(service.update(id, ledgerKey, name, companyId, richText, department, team, type, date, file));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  @PostMapping("/batch-delete")
  public ApiResponse<Void> batchDelete(@RequestBody SafetyLedgerDocumentBatchDeleteRequest request) {
    service.batchDelete(request);
    return ApiResponse.ok();
  }
}
