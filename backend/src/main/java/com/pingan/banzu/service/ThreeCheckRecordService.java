package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.ConflictException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.common.SourceChannel;
import com.pingan.banzu.common.ThreeCheckBizType;
import com.pingan.banzu.common.ThreeCheckStatus;
import com.pingan.banzu.domain.BizStatusLog;
import com.pingan.banzu.domain.ShiftTask;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.domain.SysUser;
import com.pingan.banzu.domain.ThreeCheckRecord;
import com.pingan.banzu.dto.AttachmentResponse;
import com.pingan.banzu.dto.HazardRectificationOrderDetailResponse;
import com.pingan.banzu.dto.QuickShotWorkflowActionRequest;
import com.pingan.banzu.dto.ThreeCheckFlowResponse;
import com.pingan.banzu.dto.ThreeCheckFlowStageItem;
import com.pingan.banzu.dto.ThreeCheckRecordChangeHistoryResponse;
import com.pingan.banzu.dto.ThreeCheckRecordBatchRequest;
import com.pingan.banzu.dto.ThreeCheckRecordBatchResponse;
import com.pingan.banzu.dto.ThreeCheckRecordDocumentFlowResponse;
import com.pingan.banzu.dto.ThreeCheckRecordDocumentFlowStatusLog;
import com.pingan.banzu.dto.ThreeCheckRecordDetailResponse;
import com.pingan.banzu.dto.ThreeCheckRecordListItem;
import com.pingan.banzu.dto.ThreeCheckRecordQuery;
import com.pingan.banzu.dto.ThreeCheckRecordSqlCriteria;
import com.pingan.banzu.dto.ThreeCheckRecordStatisticsResponse;
import com.pingan.banzu.dto.ThreeCheckRecordUpsertRequest;
import com.pingan.banzu.dto.ThreeCheckRecordWorkflowResponse;
import com.pingan.banzu.dto.ThreeCheckWorkflowItem;
import com.pingan.banzu.dto.WithdrawRequest;
import com.pingan.banzu.mapper.BizStatusLogMapper;
import com.pingan.banzu.mapper.ShiftTaskMapper;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.mapper.SysUserMapper;
import com.pingan.banzu.mapper.ThreeCheckRecordMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.security.SystemDataScopeService;
import com.pingan.banzu.system.dto.SystemTeamCheckTemplateItemResponse;
import com.pingan.banzu.system.dto.SystemTeamCheckTemplateResponse;
import com.pingan.banzu.system.service.SystemTeamCheckTemplateService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ThreeCheckRecordService {

  private static final String PERMISSION_HAZARD_ENTRY = "PINGAN_HAZARD_ENTRY";
  private static final String PERMISSION_HAZARD_VIEW = "PINGAN_HAZARD_VIEW";
  private static final String PERMISSION_HAZARD_REPORT = "PINGAN_HAZARD_REPORT";
  private static final String PERMISSION_HAZARD_RECTIFICATION = "PINGAN_HAZARD_RECTIFICATION";
  private static final String PERMISSION_HAZARD_ACCEPT = "PINGAN_HAZARD_ACCEPT";
  private static final String PERMISSION_HAZARD_CLOSE = "PINGAN_HAZARD_CLOSE";
  private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final Set<String> WORKFLOW_STATUSES =
      Set.of(
          ThreeCheckStatus.DRAFT,
          ThreeCheckStatus.OPENED,
          ThreeCheckStatus.WITHDRAWN,
          ThreeCheckStatus.ARCHIVED,
          ThreeCheckStatus.PENDING_REVIEW,
          ThreeCheckStatus.REVIEWED,
          ThreeCheckStatus.PENDING_RECTIFICATION,
          ThreeCheckStatus.RECTIFIED,
          ThreeCheckStatus.PENDING_ACCEPTANCE,
          ThreeCheckStatus.ACCEPTED,
          ThreeCheckStatus.REJECTED);
  private static final List<FlowStageSpec> FULL_CHAIN_FLOW_STAGES =
      List.of(
          new FlowStageSpec(
              "teamDispatch", "班组派班", List.of("team-dispatch", "curtain-wall-team-dispatch")),
          new FlowStageSpec("preShiftMeeting", "班前会", List.of("pre-shift-meeting")),
          new FlowStageSpec("preShiftInspection", "班前检查", List.of("pre-shift-inspection")),
          new FlowStageSpec("midShiftInspection", "班中检查", List.of("mid-shift-inspection")),
          new FlowStageSpec("postShiftInspection", "班后检查", List.of("post-shift-inspection")));
  private static final List<GeneratedChildSpec> DISPATCH_GENERATED_CHILDREN =
      List.of(
          new GeneratedChildSpec("pre-shift-meeting", "待开会议"),
          new GeneratedChildSpec("pre-shift-inspection", "待检查"),
          new GeneratedChildSpec("mid-shift-inspection", "待检查"),
          new GeneratedChildSpec("post-shift-inspection", "待检查"));
  private static final Set<String> ONE_SHIFT_SUBMIT_OWNER_MODULES =
      Set.of(
          "pre-shift-meeting",
          "pre-shift-inspection",
          "mid-shift-inspection",
          "post-shift-inspection");
  private static final Set<String> RECTIFICATION_ORDER_SOURCE_MODULES =
      Set.of("pre-shift-inspection", "mid-shift-inspection", "post-shift-inspection");
  private static final Set<String> OVERDUE_MODULES = ONE_SHIFT_SUBMIT_OWNER_MODULES;
  private static final Set<String> HAZARD_INSPECTION_MODULES =
      Set.of(
          "safety-check",
          "hazard-rectification",
          "quick-shot",
          "curtain-wall-penalty",
          "curtain-wall-routine-check");
  private static final Map<String, ModuleSpec> MODULES =
      Map.ofEntries(
          Map.entry(
              "team-dispatch",
              new ModuleSpec("team-dispatch", ThreeCheckBizType.THREE_CHECK_TEAM_DISPATCH, "TD")),
          Map.entry(
              "curtain-wall-team-dispatch",
              new ModuleSpec(
                  "curtain-wall-team-dispatch",
                  ThreeCheckBizType.THREE_CHECK_CURTAIN_WALL_TEAM_DISPATCH,
                  "CWTD")),
          Map.entry(
              "pre-shift-meeting",
              new ModuleSpec("pre-shift-meeting", ThreeCheckBizType.PRE_SHIFT_MEETING, "PSM")),
          Map.entry(
              "pre-shift-safety-activity",
              new ModuleSpec(
                  "pre-shift-safety-activity",
                  ThreeCheckBizType.THREE_CHECK_PRE_SHIFT_SAFETY_ACTIVITY,
                  "PSSA")),
          Map.entry(
              "pre-shift-inspection",
              new ModuleSpec(
                  "pre-shift-inspection", ThreeCheckBizType.THREE_CHECK_PRE_SHIFT_INSPECTION, "PSI")),
          Map.entry(
              "mid-shift-inspection",
              new ModuleSpec(
                  "mid-shift-inspection", ThreeCheckBizType.THREE_CHECK_MID_SHIFT_INSPECTION, "MSI")),
          Map.entry(
              "post-shift-inspection",
              new ModuleSpec(
                  "post-shift-inspection",
                  ThreeCheckBizType.THREE_CHECK_POST_SHIFT_INSPECTION,
                  "POSTSI")),
          Map.entry(
              "key-sites",
              new ModuleSpec("key-sites", ThreeCheckBizType.THREE_CHECK_KEY_SITES, "KS")),
          Map.entry(
              "safety-check",
              new ModuleSpec("safety-check", ThreeCheckBizType.HAZARD_SAFETY_CHECK, "SC")),
          Map.entry(
              "hazard-rectification",
              new ModuleSpec(
                  "hazard-rectification", ThreeCheckBizType.HAZARD_RECTIFICATION, "HR")),
          Map.entry(
              "quick-shot",
              new ModuleSpec("quick-shot", ThreeCheckBizType.HAZARD_QUICK_SHOT, "QS")),
          Map.entry(
              "curtain-wall-penalty",
              new ModuleSpec(
                  "curtain-wall-penalty",
                  ThreeCheckBizType.HAZARD_CURTAIN_WALL_PENALTY,
                  "CWP")),
          Map.entry(
              "curtain-wall-routine-check",
              new ModuleSpec(
                  "curtain-wall-routine-check",
                  ThreeCheckBizType.HAZARD_CURTAIN_WALL_ROUTINE_CHECK,
                  "CWRC")),
          Map.entry(
              "points-flow",
              new ModuleSpec("points-flow", ThreeCheckBizType.SAFETY_POINTS_FLOW, "PF")));

  private final BizStatusLogMapper statusLogMapper;
  private final ShiftTaskMapper shiftTaskMapper;
  private final SysOrgMapper orgMapper;
  private final SysUserMapper userMapper;
  private final SystemDataScopeService dataScopeService;
  private final SafetyPointsPermissionPolicy pointsPermissionPolicy;
  private final ThreeCheckRecordMapper recordMapper;
  private final ThreeCheckRecordConcurrentUpdater concurrentUpdater;
  private final ThreeCheckWorkflowSupport workflowSupport;
  private final ThreeCheckAccessPolicyService accessPolicyService;
  private final ThreeCheckPermissionPolicy threeCheckPermissionPolicy;
  private final HazardPermissionPolicy hazardPermissionPolicy;
  private final ThreeCheckRecordAttachmentSupport attachmentSupport;
  private final ThreeCheckRecordHistoryService historyService;
  private final ThreeCheckRecordPayloadCodec payloadCodec;
  private final QuickShotWorkflowService quickShotWorkflowService;
  private final SystemTeamCheckTemplateService teamCheckTemplateService;
  private final HazardRectificationOrderService hazardRectificationOrderService;
  private final NotificationEventPublisher notificationEventPublisher;
  private final TransactionTemplate createTransaction;

  public ThreeCheckRecordService(
      BizStatusLogMapper statusLogMapper,
      ShiftTaskMapper shiftTaskMapper,
      SysOrgMapper orgMapper,
      SysUserMapper userMapper,
      SystemDataScopeService dataScopeService,
      SafetyPointsPermissionPolicy pointsPermissionPolicy,
      ThreeCheckRecordMapper recordMapper,
      ThreeCheckRecordConcurrentUpdater concurrentUpdater,
      ThreeCheckWorkflowSupport workflowSupport,
      ThreeCheckAccessPolicyService accessPolicyService,
      ThreeCheckPermissionPolicy threeCheckPermissionPolicy,
      HazardPermissionPolicy hazardPermissionPolicy,
      ThreeCheckRecordAttachmentSupport attachmentSupport,
      ThreeCheckRecordHistoryService historyService,
      ThreeCheckRecordPayloadCodec payloadCodec,
      QuickShotWorkflowService quickShotWorkflowService,
      SystemTeamCheckTemplateService teamCheckTemplateService,
      HazardRectificationOrderService hazardRectificationOrderService,
      NotificationEventPublisher notificationEventPublisher,
      PlatformTransactionManager transactionManager) {
    this.statusLogMapper = statusLogMapper;
    this.shiftTaskMapper = shiftTaskMapper;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
    this.dataScopeService = dataScopeService;
    this.pointsPermissionPolicy = pointsPermissionPolicy;
    this.recordMapper = recordMapper;
    this.concurrentUpdater = concurrentUpdater;
    this.workflowSupport = workflowSupport;
    this.accessPolicyService = accessPolicyService;
    this.threeCheckPermissionPolicy = threeCheckPermissionPolicy;
    this.hazardPermissionPolicy = hazardPermissionPolicy;
    this.attachmentSupport = attachmentSupport;
    this.historyService = historyService;
    this.payloadCodec = payloadCodec;
    this.quickShotWorkflowService = quickShotWorkflowService;
    this.teamCheckTemplateService = teamCheckTemplateService;
    this.hazardRectificationOrderService = hazardRectificationOrderService;
    this.notificationEventPublisher = notificationEventPublisher;
    this.createTransaction = new TransactionTemplate(transactionManager);
    this.createTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public PageResult<ThreeCheckRecordListItem> list(String moduleKey, ThreeCheckRecordQuery query) {
    ModuleSpec module = requireModule(moduleKey);
    assertCanViewRecords(module);
    CurrentUser user = CurrentUserContext.require();
    ThreeCheckRecordSqlCriteria criteria = sqlCriteria(module, query, user);
    int page = Math.max(1, query.page() == null ? 1 : query.page());
    int pageSize = Math.min(100, Math.max(1, query.pageSize() == null ? 20 : query.pageSize()));
    int offset = (page - 1) * pageSize;
    List<ThreeCheckRecord> records =
        recordMapper.selectEnterprisePage(criteria, pageSize, offset);
    long total = recordMapper.countEnterpriseRecords(criteria);

    Map<Long, SysOrg> orgs = orgMap();
    Map<Long, SysUser> users = userMap();
    List<ThreeCheckRecordListItem> rows =
        records.stream()
            .map(record -> toListItem(module, record, orgs, users))
            .toList();

    return new PageResult<>(rows, total);
  }

  public PageResult<ThreeCheckRecordListItem> listFromMiniProgram(
      String moduleKey, ThreeCheckRecordQuery query) {
    return list(moduleKey, query);
  }

  public ThreeCheckRecordStatisticsResponse statistics(
      String moduleKey, ThreeCheckRecordQuery query) {
    ModuleSpec module = requireModule(moduleKey);
    assertCanViewRecords(module);
    CurrentUser user = CurrentUserContext.require();
    ThreeCheckRecordSqlCriteria criteria = sqlCriteria(module, query, user);

    long total = recordMapper.countEnterpriseRecords(criteria);
    Map<String, Long> statusCounts = statusCounts(recordMapper.countEnterpriseRecordsByStatus(criteria));
    Map<String, Object> attachmentRow = recordMapper.countEnterpriseRecordAttachments(criteria);
    ThreeCheckRecordStatisticsResponse.AttachmentCounts attachmentCounts =
        new ThreeCheckRecordStatisticsResponse.AttachmentCounts(
            longValue(attachmentRow, "image_uploaded"),
            longValue(attachmentRow, "image_missing"),
            longValue(attachmentRow, "video_uploaded"),
            longValue(attachmentRow, "video_missing"));

    return new ThreeCheckRecordStatisticsResponse(
        total,
        statusCounts,
        attachmentCounts,
        dateCounts(recordMapper.countEnterpriseRecordsByDate(criteria)),
        organizationCounts(recordMapper.countEnterpriseRecordsByCompany(criteria)),
        organizationCounts(recordMapper.countEnterpriseRecordsByDepartment(criteria)),
        organizationCounts(recordMapper.countEnterpriseRecordsByTeam(criteria)));
  }

  public ThreeCheckRecordDetailResponse detail(String moduleKey, Long id) {
    ModuleSpec module = requireModule(moduleKey);
    assertCanViewRecords(module);
    return detailForCurrentUser(module, id);
  }

  public ThreeCheckRecordDocumentFlowResponse documentFlow(String moduleKey, Long id) {
    ModuleSpec module = requireHazardInspectionModule(moduleKey);
    ThreeCheckRecordDetailResponse record = detail(moduleKey, id);
    Map<Long, SysUser> users = userMap();
    List<ThreeCheckRecordDocumentFlowStatusLog> statusLogs =
        workflowSupport.statusLogsFor(module.bizType(), id).stream()
            .map(log -> toDocumentFlowStatusLog(module, log, users))
            .toList();
    return new ThreeCheckRecordDocumentFlowResponse(
        record, statusLogs, linkedRectificationOrder(module, record));
  }

  public ThreeCheckRecordChangeHistoryResponse changeHistory(String moduleKey, Long id) {
    ModuleSpec module = requireHazardInspectionModule(moduleKey);
    ThreeCheckRecordDetailResponse record = detail(moduleKey, id);
    return new ThreeCheckRecordChangeHistoryResponse(
        record, historyService.list(module.bizType(), id));
  }

  public ThreeCheckRecordDetailResponse detailFromMiniProgram(String moduleKey, Long id) {
    return detail(moduleKey, id);
  }

  public ThreeCheckRecordWorkflowResponse workflow(String moduleKey, Long id) {
    ModuleSpec module = requireModule(moduleKey);
    assertCanViewRecords(module);
    CurrentUser user = CurrentUserContext.require();
    ThreeCheckRecord record = requireRecord(module, id);
    Map<Long, SysOrg> orgs = orgMap();
    if (!canAccess(record, user, orgs)) {
      throw new SecurityException("无权查看该一班三查记录");
    }

    Map<Long, SysUser> users = userMap();
    List<ThreeCheckWorkflowItem> documentFlow =
        statusLogMapper
            .selectList(
                new QueryWrapper<BizStatusLog>()
                    .eq("biz_type", module.bizType())
                    .eq("biz_id", id)
                    .orderByAsc("created_at", "id"))
            .stream()
            .map(log -> toWorkflowItem(module, log, users))
            .toList();
    List<ThreeCheckWorkflowItem> changeHistory =
        statusLogMapper
            .selectList(
                new QueryWrapper<BizStatusLog>()
                    .eq("biz_type", module.bizType())
                    .eq("biz_id", id)
                    .orderByDesc("created_at", "id"))
            .stream()
            .map(log -> toWorkflowItem(module, log, users))
            .toList();
    return new ThreeCheckRecordWorkflowResponse(documentFlow, changeHistory);
  }

  public ThreeCheckFlowResponse flow(Long rootDispatchRecordId) {
    CurrentUser user = CurrentUserContext.require();
    ThreeCheckRecord root = requireRootDispatchRecord(rootDispatchRecordId);
    if (usesThreeCheckPermissionPolicy(root.moduleKey)) {
      threeCheckPermissionPolicy.assertCanView(root.moduleKey);
    } else {
      throw new BusinessException("一班三查模块不支持：" + root.moduleKey);
    }
    Map<Long, SysOrg> orgs = orgMap();
    if (!canAccess(root, user, orgs)) {
      throw new SecurityException("无权查看该一班三查流程");
    }

    Map<Long, SysUser> users = userMap();
    Map<String, ThreeCheckRecord> stageRecords = fullChainRecords(root).stream()
        .filter(record -> canAccess(record, user, orgs))
        .collect(
            Collectors.toMap(
                record -> record.moduleKey,
                Function.identity(),
                (left, right) -> left.id <= right.id ? left : right,
                LinkedHashMap::new));
    stageRecords.put(root.moduleKey, root);

    List<ThreeCheckFlowStageItem> stages =
        FULL_CHAIN_FLOW_STAGES.stream()
            .map(
                stage -> {
                  ThreeCheckRecord record = pickFlowRecord(stage, root, stageRecords);
                  ModuleSpec module = record == null ? null : requireModule(record.moduleKey);
                  return new ThreeCheckFlowStageItem(
                      stage.key(),
                      stage.label(),
                      record == null ? stage.moduleKeys().get(0) : record.moduleKey,
                      record == null ? null : toListItem(module, record, orgs, users));
                })
            .toList();

    return new ThreeCheckFlowResponse(
        String.valueOf(root.id),
        root.moduleKey,
        String.valueOf(root.businessDate),
        orgName(orgs, root.companyId),
        orgName(orgs, root.departmentId),
        orgName(orgs, root.teamId),
        stages);
  }

  public ThreeCheckRecordDetailResponse create(String moduleKey, ThreeCheckRecordUpsertRequest request) {
    String sourceChannel = workflowSupport.resolveSourceChannel(request.sourceChannel());
    return createIdempotently(requireModule(moduleKey), request, sourceChannel, false);
  }

  public ThreeCheckRecordDetailResponse createFromMiniProgram(
      String moduleKey, ThreeCheckRecordUpsertRequest request) {
    if (isBlank(request.clientRequestId())) {
      throw new BusinessException("小程序端clientRequestId不能为空");
    }
    return createIdempotently(
        requireModule(moduleKey), request, SourceChannel.WECHAT_MINI_PROGRAM, true);
  }

  @Transactional
  public ThreeCheckRecordDetailResponse update(
      String moduleKey, Long id, ThreeCheckRecordUpsertRequest request) {
    return updateExisting(requireModule(moduleKey), id, request, false, null);
  }

  @Transactional
  public ThreeCheckRecordDetailResponse updateFromMiniProgram(
      String moduleKey, Long id, ThreeCheckRecordUpsertRequest request) {
    return updateExisting(
        requireModule(moduleKey), id, request, true, SourceChannel.WECHAT_MINI_PROGRAM);
  }

  @Transactional
  public void delete(String moduleKey, Long id) {
    ModuleSpec module = requireModule(moduleKey);
    if (HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
      hazardPermissionPolicy.assertCanDeleteRecord(module.moduleKey());
    } else if (isPointsFlowModule(module.moduleKey())) {
      pointsPermissionPolicy.assertCanDeleteFlow();
    } else if (usesThreeCheckPermissionPolicy(module.moduleKey())) {
      threeCheckPermissionPolicy.assertCanDelete(module.moduleKey());
    } else {
      throw new BusinessException("一班三查模块不支持：" + module.moduleKey());
    }
    CurrentUser user = CurrentUserContext.require();
    ThreeCheckRecord record = requireRecord(module, id);
    Map<Long, SysOrg> orgs = orgMap();
    if (!canAccess(record, user, orgs)) {
      throw new SecurityException("无权删除该一班三查记录");
    }

    String from = record.status;
    concurrentUpdater.softDelete(record, user.userId());
    workflowSupport.writeStatusLog(
        module.bizType(), id, from, from, "DELETE", user.userId(), "删除一班三查记录");
    recordChangeHistory(
        module,
        record,
        "DELETE",
        "deleted",
        "删除状态",
        "0",
        "1",
        "STATUS",
        user.userId(),
        "删除隐患排查记录");
  }

  public ThreeCheckRecordBatchResponse batch(
      String moduleKey, ThreeCheckRecordBatchRequest request) {
    ModuleSpec module = requireModule(moduleKey);
    String action = normalizeBatchAction(request.action());
    assertBatchPermission(module, action);

    List<ThreeCheckRecordBatchResponse.Result> results = new ArrayList<>();
    for (Long id : request.ids()) {
      try {
        processBatchRecord(module, action, id, request.reason());
        results.add(new ThreeCheckRecordBatchResponse.Result(String.valueOf(id), true, "处理成功"));
      } catch (RuntimeException ex) {
        results.add(
            new ThreeCheckRecordBatchResponse.Result(
                String.valueOf(id), false, failureMessage(ex)));
      }
    }
    int successCount = (int) results.stream().filter(ThreeCheckRecordBatchResponse.Result::success).count();
    return new ThreeCheckRecordBatchResponse(
        results.size(), successCount, results.size() - successCount, results);
  }

  @Transactional
  public ThreeCheckRecordDetailResponse submit(String moduleKey, Long id) {
    ModuleSpec module = requireModule(moduleKey);
    assertCanSubmit(module);
    CurrentUser user = CurrentUserContext.require();
    ThreeCheckRecord record = requireRecord(module, id);
    Map<Long, SysOrg> orgs = orgMap();
    if (!canAccess(record, user, orgs)) {
      throw new SecurityException("无权修改该一班三查记录");
    }
    ensureNotExpiredForSubmit(module, record);
    workflowSupport.requireSubmittable(record.status);
    String from = record.status;
    Map<String, Object> payload = readPayload(record.payloadJson);
    ensureInspectionCheckResultsBeforeSubmit(module, record, payload);
    ensurePreShiftMeetingSafetyConfirmBeforeSubmit(module, record, payload);
    ensurePreShiftMeetingMediaCheckBeforeSubmit(module, record);
    assignOneShiftSubmitOwnerToTeamLeader(module, record, payload, user);
    String submittedStatus = submittedStatus(module);
    record.status = submittedStatus;
    record.payloadJson = writePayload(forceStatusLabel(payload, module, record.status));
    record.submittedBy = user.userId();
    record.submittedAt = LocalDateTime.now();
    touch(record, user.userId());
    concurrentUpdater.update(record);
    workflowSupport.writeStatusLog(
        module.bizType(), id, from, submittedStatus, "SUBMIT", user.userId(), "提交一班三查记录");
    recordChangeHistory(
        module,
        record,
        "SUBMIT",
        "status",
        "状态",
        moduleStatusLabel(module.moduleKey(), from),
        moduleStatusLabel(module.moduleKey(), record.status),
        "STATUS",
        user.userId(),
        "提交检查");
    ensureEffectiveDispatchGeneratedChildren(record, user.userId());
    hazardRectificationOrderService.createOrUpdateFromThreeCheckInspection(id, user.userId());
    hazardRectificationOrderService.createOrUpdateFromSafetyCheck(id, user.userId());
    notificationEventPublisher.threeCheckHandled(module.bizType(), id, record.ownerUserId);
    return detailForCurrentUser(module, id);
  }

  private String submittedStatus(ModuleSpec module) {
    return ThreeCheckStatus.OPENED;
  }

  @Transactional
  public ThreeCheckRecordDetailResponse submitFromMiniProgram(String moduleKey, Long id) {
    return submit(moduleKey, id);
  }

  @Transactional
  public ThreeCheckRecordDetailResponse withdraw(
      String moduleKey, Long id, WithdrawRequest request) {
    ModuleSpec module = requireModule(moduleKey);
    assertWithdrawPermission(module);
    CurrentUser user = CurrentUserContext.require();
    ThreeCheckRecord record = requireRecord(module, id);
    Map<Long, SysOrg> orgs = orgMap();
    if (!canAccess(record, user, orgs)) {
      throw new SecurityException("无权修改该一班三查记录");
    }
    if ("quick-shot".equals(module.moduleKey())) {
      return withdrawQuickShotToPendingReview(module, record, request, user);
    }
    if ("key-sites".equals(module.moduleKey())
        && hazardRectificationOrderService.hasOrderFromThreeCheckInspection(record.id)) {
      throw new BusinessException("重点场所已关联整改工单，不能撤回检查事实");
    }
    ensureWithdrawAllowedByBusinessDate(module, record);
    workflowSupport.requireWithdrawable(record.status);
    String from = record.status;
    record.status = ThreeCheckStatus.WITHDRAWN;
    record.payloadJson = writePayload(forceStatusLabel(readPayload(record.payloadJson), module, record.status));
    record.withdrawnBy = user.userId();
    record.withdrawnAt = LocalDateTime.now();
    record.withdrawReason = request == null ? null : request.reason();
    touch(record, user.userId());
    concurrentUpdater.update(record);
    workflowSupport.writeStatusLog(
        module.bizType(), id, from, ThreeCheckStatus.WITHDRAWN, "WITHDRAW", user.userId(), record.withdrawReason);
    recordChangeHistory(
        module,
        record,
        "WITHDRAW",
        "status",
        "状态",
        moduleStatusLabel(module.moduleKey(), from),
        moduleStatusLabel(module.moduleKey(), record.status),
        "STATUS",
        user.userId(),
        record.withdrawReason);
    if (isDispatchModule(module.moduleKey())) {
      markDispatchChildrenPaused(record, record.withdrawReason, user.userId());
      notificationEventPublisher.teamDispatchChanged(
          "TEAM_DISPATCH_CANCELLED",
          record.id,
          record.recordNo,
          record.businessDate,
          record.teamId,
          record.ownerUserId == null ? List.of() : List.of(record.ownerUserId),
          record.createdBy,
          user.userId(),
          String.valueOf(record.version));
    }
    return detailForCurrentUser(module, id);
  }

  private ThreeCheckRecordDetailResponse withdrawQuickShotToPendingReview(
      ModuleSpec module, ThreeCheckRecord record, WithdrawRequest request, CurrentUser user) {
    String normalizedStatus = normalizeQuickShotLegacyStatus(record.status);
    if (!ThreeCheckStatus.REVIEWED.equals(normalizedStatus)) {
      throw new BusinessException(
          "随手拍撤回不适用于当前状态：" + moduleStatusLabel(module.moduleKey(), record.status));
    }
    String from = record.status;
    record.status = ThreeCheckStatus.PENDING_REVIEW;
    record.payloadJson = writePayload(forceStatusLabel(readPayload(record.payloadJson), module, record.status));
    record.withdrawnBy = user.userId();
    record.withdrawnAt = LocalDateTime.now();
    record.withdrawReason = request == null ? null : request.reason();
    touch(record, user.userId());
    concurrentUpdater.update(record);
    String remark = isBlank(record.withdrawReason) ? "随手拍撤回重审" : record.withdrawReason;
    workflowSupport.writeStatusLog(
        module.bizType(), record.id, from, ThreeCheckStatus.PENDING_REVIEW, "WITHDRAW", user.userId(), remark);
    recordChangeHistory(
        module,
        record,
        "WITHDRAW",
        "status",
        "状态",
        moduleStatusLabel(module.moduleKey(), from),
        moduleStatusLabel(module.moduleKey(), record.status),
        "STATUS",
        user.userId(),
        remark);
    hazardRectificationOrderService.cancelFromQuickShotWithdraw(record.id, user.userId(), "随手拍撤回自动作废");
    return detailForCurrentUser(module, record.id);
  }

  @Transactional
  public ThreeCheckRecordDetailResponse withdrawFromMiniProgram(
      String moduleKey, Long id, WithdrawRequest request) {
    return withdraw(moduleKey, id, request);
  }

  @Transactional
  public ThreeCheckRecordDetailResponse remind(String moduleKey, Long id) {
    ModuleSpec module = requireModule(moduleKey);
    if (usesThreeCheckPermissionPolicy(module.moduleKey())) {
      threeCheckPermissionPolicy.assertCanRemind(module.moduleKey());
    } else if (HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
      hazardPermissionPolicy.assertCanVoidRecord(module.moduleKey());
    } else {
      throw new BusinessException("一班三查模块不支持：" + module.moduleKey());
    }
    CurrentUser user = CurrentUserContext.require();
    ThreeCheckRecord record = requireRecord(module, id);
    Map<Long, SysOrg> orgs = orgMap();
    if (!canAccess(record, user, orgs)) {
      throw new SecurityException("无权修改该一班三查记录");
    }
    ThreeCheckRecordConcurrentUpdater.ReminderIncrement increment =
        concurrentUpdater.incrementReminder(id, user.userId());
    record = increment.record();
    if (!canAccess(record, user, orgs)) {
      throw new SecurityException("无权修改该一班三查记录");
    }
    int beforeCount = increment.beforeCount();
    workflowSupport.writeRemindRecord(
        module.bizType(), id, record.ownerUserId, "请及时处理一班三查记录：" + record.recordNo, user.userId());
    workflowSupport.writeStatusLog(
        module.bizType(), id, record.status, record.status, "REMIND", user.userId(), "催办一班三查记录");
    recordChangeHistory(
        module,
        record,
        "REMIND",
        "reminderCount",
        "催办次数",
        String.valueOf(beforeCount),
        String.valueOf(record.reminderCount),
        "TEXT",
        user.userId(),
        "催办一班三查记录");
    notificationEventPublisher.threeCheckReminded(
        module.moduleKey(),
        module.bizType(),
        record.id,
        record.recordNo,
        record.businessDate,
        record.reminderCount,
        record.ownerUserId,
        user.userId());
    return detailForCurrentUser(module, id);
  }

  @Transactional
  public ThreeCheckRecordDetailResponse remindFromMiniProgram(String moduleKey, Long id) {
    return remind(moduleKey, id);
  }

  @Transactional
  public HazardRectificationOrderDetailResponse openRectificationOrder(String moduleKey, Long id) {
    ModuleSpec module = requireModule(moduleKey);
    boolean oneShiftInspection = RECTIFICATION_ORDER_SOURCE_MODULES.contains(module.moduleKey());
    if (!oneShiftInspection && !"key-sites".equals(module.moduleKey())) {
      throw new BusinessException("只有班前、班中、班后检查或重点场所检查可以开整改单");
    }
    assertCanViewRecords(module);
    if (oneShiftInspection) {
      threeCheckPermissionPolicy.assertCanCreateRectificationOrder();
    }
    CurrentUser user = CurrentUserContext.require();
    ThreeCheckRecord record = requireRecord(module, id);
    Map<Long, SysOrg> orgs = orgMap();
    if (!canAccess(record, user, orgs)) {
      throw new SecurityException("无权查看该一班三查记录");
    }
    return hazardRectificationOrderService.openFromThreeCheckInspection(id, user.userId());
  }

  @Transactional
  public HazardRectificationOrderDetailResponse openRectificationOrderFromMiniProgram(
      String moduleKey, Long id) {
    return openRectificationOrder(moduleKey, id);
  }

  @Transactional
  public ThreeCheckRecordDetailResponse quickShotWorkflowAction(
      String moduleKey, Long id, QuickShotWorkflowActionRequest request) {
    ModuleSpec module = requireModule(moduleKey);
    if (!"quick-shot".equals(module.moduleKey())) {
      throw new BusinessException("随手拍流程动作仅支持随手拍模块");
    }
    quickShotWorkflowService.assertCanExecute(request);
    CurrentUser user = CurrentUserContext.require();
    ThreeCheckRecord record = requireRecord(module, id);
    Map<Long, SysOrg> orgs = orgMap();
    if (!canAccess(record, user, orgs)) {
      throw new SecurityException("无权修改该一班三查记录");
    }
    quickShotWorkflowService.execute(record, request, user);
    return detailForCurrentUser(module, id);
  }

  private HazardRectificationOrderDetailResponse linkedRectificationOrder(
      ModuleSpec module, ThreeCheckRecordDetailResponse record) {
    if (!"quick-shot".equals(module.moduleKey())
        && !"safety-check".equals(module.moduleKey())) {
      return null;
    }
    if (record == null || record.payload() == null) {
      return null;
    }
    Long orderId = linkedRectificationOrderId(record.payload());
    if (orderId == null) {
      return null;
    }
    try {
      return hazardRectificationOrderService.detail(orderId);
    } catch (BusinessException ex) {
      return null;
    }
  }

  private Long linkedRectificationOrderId(Map<String, Object> payload) {
    Long orderId = parseLong(payload.get("rectificationOrderId"));
    if (orderId != null) {
      return orderId;
    }
    Object checkItems = payload.get("checkItems");
    if (!(checkItems instanceof List<?> items)) {
      return null;
    }
    for (Object rawItem : items) {
      if (!(rawItem instanceof Map<?, ?> item)) {
        continue;
      }
      orderId = parseLong(item.get("rectificationOrderId"));
      if (orderId != null) {
        return orderId;
      }
    }
    return null;
  }

  private Long parseLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String text && !text.isBlank()) {
      return Long.parseLong(text);
    }
    return null;
  }

  private String normalizeBatchAction(String action) {
    if (isBlank(action)) {
      throw new BusinessException("批量操作不能为空");
    }
    String normalized = action.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "DELETE", "REMIND", "SUBMIT", "WITHDRAW" -> normalized;
      default -> throw new BusinessException("批量操作不支持：" + action);
    };
  }

  private void assertBatchPermission(ModuleSpec module, String action) {
    switch (action) {
      case "DELETE" -> {
        if (HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
          hazardPermissionPolicy.assertCanDeleteRecord(module.moduleKey());
        } else if (isPointsFlowModule(module.moduleKey())) {
          pointsPermissionPolicy.assertCanDeleteFlow();
        } else if (usesThreeCheckPermissionPolicy(module.moduleKey())) {
          threeCheckPermissionPolicy.assertCanDelete(module.moduleKey());
        } else {
          throw new BusinessException("一班三查模块不支持：" + module.moduleKey());
        }
      }
      case "SUBMIT" -> {
        if (HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
          hazardPermissionPolicy.assertCanSubmitRecord(module.moduleKey());
        } else if (isPointsFlowModule(module.moduleKey())) {
          pointsPermissionPolicy.assertCanCreateFlow();
        } else if (usesThreeCheckPermissionPolicy(module.moduleKey())) {
          threeCheckPermissionPolicy.assertCanSubmit(module.moduleKey());
        } else {
          throw new BusinessException("一班三查模块不支持：" + module.moduleKey());
        }
      }
      case "WITHDRAW" -> assertWithdrawPermission(module);
      case "REMIND" -> {
        if (usesThreeCheckPermissionPolicy(module.moduleKey())) {
          threeCheckPermissionPolicy.assertCanRemind(module.moduleKey());
        } else if (HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
          hazardPermissionPolicy.assertCanVoidRecord(module.moduleKey());
        } else {
          throw new BusinessException("一班三查模块不支持：" + module.moduleKey());
        }
      }
      default -> throw new BusinessException("批量操作不支持：" + action);
    }
  }

  private void assertWithdrawPermission(ModuleSpec module) {
    if ("quick-shot".equals(module.moduleKey())) {
      hazardPermissionPolicy.assertCanVoidRecord(module.moduleKey());
      return;
    }
    if (HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
      hazardPermissionPolicy.assertCanVoidRecord(module.moduleKey());
      return;
    }
    if (isPointsFlowModule(module.moduleKey())) {
      pointsPermissionPolicy.assertCanDeleteFlow();
      return;
    }
    if (usesThreeCheckPermissionPolicy(module.moduleKey())) {
      threeCheckPermissionPolicy.assertCanVoid(module.moduleKey());
      return;
    }
    throw new BusinessException("一班三查模块不支持：" + module.moduleKey());
  }

  private void processBatchRecord(ModuleSpec module, String action, Long id, String reason) {
    switch (action) {
      case "DELETE" -> delete(module.moduleKey(), id);
      case "SUBMIT" -> submit(module.moduleKey(), id);
      case "WITHDRAW" -> withdraw(module.moduleKey(), id, new WithdrawRequest(reason));
      case "REMIND" -> remind(module.moduleKey(), id);
      default -> throw new BusinessException("批量操作不支持：" + action);
    }
  }

  private String failureMessage(RuntimeException ex) {
    return isBlank(ex.getMessage()) ? "处理失败" : ex.getMessage();
  }

  void markAttachmentUploaded(String moduleKey, Long recordId, String fileKind) {
    ModuleSpec module = requireModule(moduleKey);
    assertCanManageAttachment(module, recordId);
    ThreeCheckRecord record = requireRecord(module, recordId);
    attachmentSupport.markUploaded(record, fileKind);
  }

  void markAttachmentDeleted(String moduleKey, Long recordId, String fileKind) {
    ModuleSpec module = requireModule(moduleKey);
    assertCanManageAttachment(module, recordId);
    ThreeCheckRecord record = requireRecord(module, recordId);
    attachmentSupport.markDeleted(record, module.bizType(), fileKind);
  }

  public void assertCanManageAttachmentBeforeUpload(String moduleKey, Long recordId) {
    ModuleSpec module = requireModule(moduleKey);
    assertCanManageAttachment(module, recordId);
  }

  private void assertCanManageAttachment(ModuleSpec module, Long recordId) {
    assertAttachmentManagePermission(module);
    CurrentUser user = CurrentUserContext.require();
    ThreeCheckRecord record = requireRecord(module, recordId);
    if ("quick-shot".equals(module.moduleKey())) {
      if (isQuickShotAttachmentOwner(record, user)) {
        return;
      }
      throw new SecurityException("无权修改该一班三查记录");
    }
    if (!canAccess(record, user, orgMap())) {
      throw new SecurityException("无权修改该一班三查记录");
    }
  }

  private boolean isQuickShotAttachmentOwner(ThreeCheckRecord record, CurrentUser user) {
    return (record.ownerUserId != null && record.ownerUserId.equals(user.userId()))
        || (record.createdBy != null && record.createdBy.equals(user.userId()));
  }

  private void assertAttachmentManagePermission(ModuleSpec module) {
    if (HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
      hazardPermissionPolicy.assertCanManageRecordAttachment(module.moduleKey());
      return;
    }
    if (isPointsFlowModule(module.moduleKey())) {
      pointsPermissionPolicy.assertCanCreateFlow();
      return;
    }
    if (usesThreeCheckPermissionPolicy(module.moduleKey())) {
      threeCheckPermissionPolicy.assertCanUpdate(module.moduleKey());
      return;
    }
    throw new BusinessException("一班三查模块不支持：" + module.moduleKey());
  }

  void recordAttachmentChange(String moduleKey, Long recordId, AttachmentResponse attachment) {
    if (!HAZARD_INSPECTION_MODULES.contains(moduleKey)) {
      return;
    }
    ModuleSpec module = requireModule(moduleKey);
    ThreeCheckRecord record = requireRecord(module, recordId);
    recordChangeHistory(
        module,
        record,
        "ATTACHMENT",
        "attachment",
        "附件",
        null,
        attachment.originalName(),
        attachment.fileKind(),
        CurrentUserContext.require().userId(),
        "上传附件");
  }

  public String bizType(String moduleKey) {
    return requireModule(moduleKey).bizType();
  }

  private ThreeCheckRecordDetailResponse createOrUpsert(
      ModuleSpec module, ThreeCheckRecordUpsertRequest request, String sourceChannel, boolean miniProgram) {
    validateOrganizationRequest(module, request);
    ThreeCheckRecord sameRequest = findByClientRequestId(module, sourceChannel, request.clientRequestId());
    if (sameRequest != null) {
      assertCanCreate(module);
      return detailForCurrentUser(module, sameRequest.id);
    }

    ThreeCheckRecord sameSourceRecord = findBySourceRecordId(module, sourceChannel, request.sourceRecordId());
    if (sameSourceRecord != null) {
      requireVersionMatch(sameSourceRecord, request.version(), false);
      return updateExisting(module, sameSourceRecord.id, request, miniProgram, sourceChannel);
    }

    return createNew(module, request, sourceChannel);
  }

  private ThreeCheckRecordDetailResponse createIdempotently(
      ModuleSpec module,
      ThreeCheckRecordUpsertRequest request,
      String sourceChannel,
      boolean miniProgram) {
    try {
      return Objects.requireNonNull(
          createTransaction.execute(
              status -> createOrUpsert(module, request, sourceChannel, miniProgram)));
    } catch (DataIntegrityViolationException conflict) {
      ThreeCheckRecordDetailResponse recovered =
          createTransaction.execute(
              status -> {
                ThreeCheckRecord existing =
                    findByClientRequestId(module, sourceChannel, request.clientRequestId());
                if (existing == null) {
                  existing =
                      findBySourceRecordId(module, sourceChannel, request.sourceRecordId());
                }
                if (existing == null) {
                  return null;
                }
                assertCanCreate(module);
                return detailForCurrentUser(module, existing.id);
              });
      if (recovered != null) {
        return recovered;
      }
      throw conflict;
    }
  }

  private ThreeCheckRecordDetailResponse createNew(
      ModuleSpec module, ThreeCheckRecordUpsertRequest request, String sourceChannel) {
    assertCanCreate(module);
    CurrentUser user = CurrentUserContext.require();
    Long ownerUserId = request.ownerUserId() == null ? user.userId() : request.ownerUserId();
    Long taskId = request.taskId();
    if (taskId == null && request.teamId() != null) {
      ShiftTask task = new ShiftTask();
      task.taskNo = "TASK-" + request.businessDate().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + shortId();
      task.companyId = request.companyId();
      task.departmentId = request.departmentId();
      task.teamId = request.teamId();
      task.shiftDate = request.businessDate();
      task.shiftName = "早班";
      task.leaderUserId = ownerUserId;
      task.status = "OPEN";
      task.taskType = "ONE_SHIFT_THREE_CHECKS";
      task.sourceChannel = sourceChannel;
      task.sourceRecordId = blankToNull(request.sourceRecordId());
      task.clientRequestId = resolveClientRequestId(request.clientRequestId());
      task.createdAt = LocalDateTime.now();
      task.updatedAt = task.createdAt;
      task.deleted = 0;
      shiftTaskMapper.insert(task);
      taskId = task.id;
    }

    validatePointsFlowRequest(module, request);
    String status = normalizeStatus(module, request.status(), defaultStatus(module));
    LocalDateTime now = LocalDateTime.now();
    ThreeCheckRecord record = new ThreeCheckRecord();
    record.moduleKey = module.moduleKey();
    record.recordNo = recordNo(module, request.businessDate());
    record.taskId = taskId;
    record.rootDispatchRecordId =
        isDispatchModule(module.moduleKey()) ? null : resolveRootDispatchRecordId(module, request, null);
    record.companyId = request.companyId();
    record.departmentId = request.departmentId();
    record.teamId = request.teamId();
    record.ownerUserId = ownerUserId;
    record.businessDate = request.businessDate();
    record.status = status;
    Map<String, Object> payload =
        withStatusLabel(normalizedPayload(module, request.payload()), module, status);
    if (isTeamCheckInspectionModule(module.moduleKey()) && !hasCheckItemSnapshots(payload)) {
      addCheckItemSnapshotIfInspection(payload, module.moduleKey(), record);
    }
    record.payloadJson = writePayload(payload);
    record.imageCheckStatus = "未上传";
    record.videoCheckStatus = "未上传";
    record.reminderCount = 0;
    record.version = 0;
    record.createdBy = user.userId();
    record.updatedBy = user.userId();
    record.sourceChannel = sourceChannel;
    record.sourceRecordId = blankToNull(request.sourceRecordId());
    record.clientRequestId = resolveClientRequestId(request.clientRequestId());
    record.clientUpdatedAt = request.clientUpdatedAt();
    record.lastSyncedAt = now;
    record.createdAt = now;
    record.updatedAt = now;
    record.deleted = 0;
    recordMapper.insert(record);
    if (isDispatchModule(module.moduleKey())) {
      record.rootDispatchRecordId = record.id;
      concurrentUpdater.update(record);
    }
    if ("quick-shot".equals(module.moduleKey())) {
      workflowSupport.writeStatusLog(
          module.bizType(), record.id, null, status, "CREATE", user.userId(), "创建一班三查记录", record.payloadJson);
    } else {
      workflowSupport.writeStatusLog(
          module.bizType(), record.id, null, status, "CREATE", user.userId(), "创建一班三查记录");
    }
    recordChangeHistory(
        module,
        record,
        "CREATE",
        "recordNo",
        "单据编号",
        null,
        record.recordNo,
        "TEXT",
        user.userId(),
        "创建记录");
    ensureEffectiveDispatchGeneratedChildren(record, user.userId());
    if ("quick-shot".equals(module.moduleKey())) {
      notificationEventPublisher.hazardSourceReviewRequested(
          record.id,
          record.recordNo,
          record.businessDate,
          record.teamId == null ? record.departmentId : record.teamId,
          user.userId());
    }
    if (isDispatchModule(module.moduleKey())) {
      notificationEventPublisher.teamDispatchChanged(
          "TEAM_DISPATCH_ASSIGNED",
          record.id,
          record.recordNo,
          record.businessDate,
          record.teamId,
          record.ownerUserId == null ? List.of() : List.of(record.ownerUserId),
          record.createdBy,
          user.userId(),
          String.valueOf(record.version));
    }
    if ("key-sites".equals(module.moduleKey())) {
      notificationEventPublisher.keySiteInspectionAssigned(
          record.id,
          record.recordNo,
          record.businessDate,
          record.teamId == null ? record.departmentId : record.teamId,
          record.ownerUserId,
          record.createdBy,
          user.userId());
    }
    if (isPointsFlowModule(module.moduleKey())) {
      notificationEventPublisher.pointsChanged(
          record.id,
          record.recordNo,
          record.businessDate,
          record.teamId == null ? record.departmentId : record.teamId,
          record.ownerUserId,
          record.createdBy,
          user.userId());
    }
    return detailForCurrentUser(module, record.id);
  }

  private ThreeCheckRecordDetailResponse updateExisting(
      ModuleSpec module,
      Long id,
      ThreeCheckRecordUpsertRequest request,
      boolean miniProgram,
      String forcedSourceChannel) {
    assertCanUpdate(module);
    CurrentUser user = CurrentUserContext.require();
    ThreeCheckRecord record = requireRecord(module, id);
    Map<Long, SysOrg> orgs = orgMap();
    if (!canAccess(record, user, orgs)) {
      throw new SecurityException("无权修改该一班三查记录");
    }
    requireVersionMatch(record, request.version(), true);
    validateOrganizationRequest(module, request);
    ensureHistoricalBusinessDateUnchanged(module, record, request.businessDate());

    ThreeCheckRecord before = historyService.copyOf(record);
    Map<String, Object> beforePayload = readPayload(record.payloadJson);
    validatePointsFlowRequest(module, request);
    String status = normalizeStatus(module, request.status(), record.status);
    record.taskId = request.taskId() == null ? record.taskId : request.taskId();
    record.rootDispatchRecordId = resolveRootDispatchRecordId(module, request, record);
    record.companyId = request.companyId();
    record.departmentId = request.departmentId();
    record.teamId = request.teamId();
    record.ownerUserId = request.ownerUserId() == null ? record.ownerUserId : request.ownerUserId();
    record.businessDate = request.businessDate();
    record.status = status;
    record.payloadJson =
        writePayload(withStatusLabel(normalizedPayload(module, request.payload()), module, status));
    if (forcedSourceChannel != null) {
      record.sourceChannel = forcedSourceChannel;
    } else if (!miniProgram && !isBlank(request.sourceChannel())) {
      record.sourceChannel = workflowSupport.resolveSourceChannel(request.sourceChannel());
    }
    if (!isBlank(request.sourceRecordId())) {
      record.sourceRecordId = request.sourceRecordId().trim();
    }
    if (!isBlank(request.clientRequestId())) {
      record.clientRequestId = request.clientRequestId().trim();
    }
    record.clientUpdatedAt = request.clientUpdatedAt();
    record.lastSyncedAt = LocalDateTime.now();
    touch(record, user.userId());
    concurrentUpdater.update(record);
    workflowSupport.writeStatusLog(
        module.bizType(), record.id, before.status, record.status, "UPDATE", user.userId(), "更新一班三查记录");
    recordUpdateChangeHistory(module, before, beforePayload, record, readPayload(record.payloadJson), user.userId());
    ensureEffectiveDispatchGeneratedChildren(record, user.userId());
    if (isDispatchModule(module.moduleKey())) {
      List<Long> affectedUsers = new ArrayList<>();
      if (before.ownerUserId != null) affectedUsers.add(before.ownerUserId);
      if (record.ownerUserId != null && !affectedUsers.contains(record.ownerUserId)) {
        affectedUsers.add(record.ownerUserId);
      }
      notificationEventPublisher.teamDispatchChanged(
          "TEAM_DISPATCH_CHANGED",
          record.id,
          record.recordNo,
          record.businessDate,
          record.teamId,
          affectedUsers,
          record.createdBy,
          user.userId(),
          String.valueOf(record.version));
    }
    return detailForCurrentUser(module, id);
  }

  private void assertCanViewRecords(ModuleSpec module) {
    if (HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
      hazardPermissionPolicy.assertCanView(module.moduleKey());
      return;
    }
    if (isPointsFlowModule(module.moduleKey())) {
      pointsPermissionPolicy.assertCanViewFlow();
      return;
    }
    if (usesThreeCheckPermissionPolicy(module.moduleKey())) {
      threeCheckPermissionPolicy.assertCanView(module.moduleKey());
      return;
    }
    throw new BusinessException("一班三查模块不支持：" + module.moduleKey());
  }

  private ThreeCheckRecordDetailResponse detailForCurrentUser(ModuleSpec module, Long id) {
    CurrentUser user = CurrentUserContext.require();
    ThreeCheckRecord record = requireRecord(module, id);
    Map<Long, SysOrg> orgs = orgMap();
    if (!canAccess(record, user, orgs)) {
      throw new SecurityException("无权查看该一班三查记录");
    }
    return toDetail(module, record, orgs, userMap());
  }

  private void assertCanCreate(ModuleSpec module) {
    if (HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
      hazardPermissionPolicy.assertCanCreateOrUpdateRecord(module.moduleKey());
      return;
    }
    if (isPointsFlowModule(module.moduleKey())) {
      pointsPermissionPolicy.assertCanCreateFlow();
      return;
    }
    if (usesThreeCheckPermissionPolicy(module.moduleKey())) {
      threeCheckPermissionPolicy.assertCanCreate(module.moduleKey());
      return;
    }
    throw new BusinessException("一班三查模块不支持：" + module.moduleKey());
  }

  private void assertCanUpdate(ModuleSpec module) {
    if (HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
      hazardPermissionPolicy.assertCanCreateOrUpdateRecord(module.moduleKey());
      return;
    }
    if (isPointsFlowModule(module.moduleKey())) {
      pointsPermissionPolicy.assertCanCreateFlow();
      return;
    }
    if (usesThreeCheckPermissionPolicy(module.moduleKey())) {
      threeCheckPermissionPolicy.assertCanUpdate(module.moduleKey());
      return;
    }
    throw new BusinessException("一班三查模块不支持：" + module.moduleKey());
  }

  private void assertCanSubmit(ModuleSpec module) {
    if (HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
      hazardPermissionPolicy.assertCanSubmitRecord(module.moduleKey());
      return;
    }
    if (isPointsFlowModule(module.moduleKey())) {
      pointsPermissionPolicy.assertCanCreateFlow();
      return;
    }
    if (usesThreeCheckPermissionPolicy(module.moduleKey())) {
      threeCheckPermissionPolicy.assertCanSubmit(module.moduleKey());
      return;
    }
    throw new BusinessException("一班三查模块不支持：" + module.moduleKey());
  }

  private ThreeCheckRecord requireRecord(ModuleSpec module, Long id) {
    ThreeCheckRecord record = recordMapper.selectById(id);
    if (record == null
        || Integer.valueOf(1).equals(record.deleted)
        || !module.moduleKey().equals(record.moduleKey)) {
      throw new BusinessException("一班三查记录不存在");
    }
    return record;
  }

  private String recordNo(ModuleSpec module, LocalDate businessDate) {
    String datePart = businessDate.format(DateTimeFormatter.BASIC_ISO_DATE);
    if (isPointsFlowModule(module.moduleKey())) {
      return module.recordPrefix() + "-" + datePart + "-" + shortId();
    }
    return "TCR-" + module.recordPrefix() + "-" + datePart + "-" + shortId();
  }

  private void validatePointsFlowRequest(ModuleSpec module, ThreeCheckRecordUpsertRequest request) {
    if (!isPointsFlowModule(module.moduleKey())) {
      return;
    }
    Map<String, Object> payload = payloadFrom(request.payload());
    requireText(payload, "createdAt", "创建时间");
    requireText(payload, "user", "用户");
    requireText(payload, "pointsReason", "积分变动原因");
    String pointsChange = requireText(payload, "pointsChange", "积分变动");
    if (!Set.of("加分", "扣分", "兑换").contains(pointsChange)) {
      throw new BusinessException("积分变动只能为加分、扣分或兑换");
    }
    int quantity = positiveInteger(payload.get("pointsQuantity"), "积分数量");
    payload.put("pointsQuantity", quantity);
    if ("兑换".equals(pointsChange)) {
      requireText(payload, "vendingMachine", "贩卖机");
      requireText(payload, "goods", "货品");
    }
  }

  private String requireText(Map<String, Object> payload, String key, String label) {
    Object value = payload.get(key);
    if (value == null || String.valueOf(value).trim().isBlank()) {
      throw new BusinessException(label + "不能为空");
    }
    return String.valueOf(value).trim();
  }

  private int positiveInteger(Object value, String label) {
    if (value == null) {
      throw new BusinessException(label + "不能为空");
    }
    try {
      int parsed =
          value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value).trim());
      if (parsed <= 0) {
        throw new NumberFormatException();
      }
      return parsed;
    } catch (NumberFormatException ex) {
      throw new BusinessException(label + "必须为正整数");
    }
  }

  private ThreeCheckRecord requireRootDispatchRecord(Long id) {
    ThreeCheckRecord record = recordMapper.selectById(id);
    if (record == null || Integer.valueOf(1).equals(record.deleted) || !isDispatchModule(record.moduleKey)) {
      throw new BusinessException("一班三查派班流程不存在");
    }
    return record;
  }

  private boolean isDispatchModule(String moduleKey) {
    return "team-dispatch".equals(moduleKey) || "curtain-wall-team-dispatch".equals(moduleKey);
  }

  private boolean usesThreeCheckPermissionPolicy(String moduleKey) {
    return "team-dispatch".equals(moduleKey)
        || "curtain-wall-team-dispatch".equals(moduleKey)
        || "pre-shift-meeting".equals(moduleKey)
        || "pre-shift-inspection".equals(moduleKey)
        || "mid-shift-inspection".equals(moduleKey)
        || "post-shift-inspection".equals(moduleKey)
        || "pre-shift-safety-activity".equals(moduleKey)
        || "key-sites".equals(moduleKey);
  }

  private boolean isPointsFlowModule(String moduleKey) {
    return "points-flow".equals(moduleKey);
  }

  private boolean isFullChainChildModule(String moduleKey) {
    return "pre-shift-meeting".equals(moduleKey)
        || "pre-shift-inspection".equals(moduleKey)
        || "mid-shift-inspection".equals(moduleKey)
        || "post-shift-inspection".equals(moduleKey);
  }

  private Long resolveRootDispatchRecordId(
      ModuleSpec module, ThreeCheckRecordUpsertRequest request, ThreeCheckRecord existing) {
    if (isDispatchModule(module.moduleKey())) {
      return existing == null ? null : existing.id;
    }
    if (!isFullChainChildModule(module.moduleKey())) {
      return null;
    }
    if (request.rootDispatchRecordId() != null) {
      ThreeCheckRecord root = requireRootDispatchRecord(request.rootDispatchRecordId());
      requireRootMatchesRequest(root, request);
      return root.id;
    }
    if (existing != null && existing.rootDispatchRecordId != null) {
      return existing.rootDispatchRecordId;
    }
    return uniqueRootDispatchCandidate(request);
  }

  private void requireRootMatchesRequest(ThreeCheckRecord root, ThreeCheckRecordUpsertRequest request) {
    if (!root.companyId.equals(request.companyId())
        || !root.departmentId.equals(request.departmentId())
        || !root.teamId.equals(request.teamId())
        || !root.businessDate.equals(request.businessDate())) {
      throw new BusinessException("所选派班不属于当前公司、车间、班组和日期");
    }
  }

  private Long uniqueRootDispatchCandidate(ThreeCheckRecordUpsertRequest request) {
    List<ThreeCheckRecord> candidates =
        recordMapper.selectList(
            new QueryWrapper<ThreeCheckRecord>()
                .eq("deleted", 0)
                .in("module_key", "team-dispatch", "curtain-wall-team-dispatch")
                .eq("company_id", request.companyId())
                .eq("department_id", request.departmentId())
                .eq("team_id", request.teamId())
                .eq("business_date", request.businessDate())
                .orderByAsc("id"));
    return candidates.size() == 1 ? candidates.get(0).id : null;
  }

  private List<ThreeCheckRecord> fullChainRecords(ThreeCheckRecord root) {
    return recordMapper.selectList(
        new QueryWrapper<ThreeCheckRecord>()
            .eq("deleted", 0)
            .eq("root_dispatch_record_id", root.id)
            .in(
                "module_key",
                "team-dispatch",
                "curtain-wall-team-dispatch",
                "pre-shift-meeting",
                "pre-shift-inspection",
                "mid-shift-inspection",
                "post-shift-inspection")
            .orderByAsc("id"));
  }

  private void ensureEffectiveDispatchGeneratedChildren(ThreeCheckRecord root, Long operatorId) {
    if (!isDispatchModule(root.moduleKey)
        || !(ThreeCheckStatus.OPENED.equals(root.status) || ThreeCheckStatus.ARCHIVED.equals(root.status))) {
      return;
    }
    ensureDispatchGeneratedChildren(root, operatorId);
  }

  private void ensureDispatchGeneratedChildren(ThreeCheckRecord root, Long operatorId) {
    if (!isDispatchModule(root.moduleKey)) {
      return;
    }
    Long rootId = root.rootDispatchRecordId == null ? root.id : root.rootDispatchRecordId;
    Long childOwnerUserId = accessPolicyService.resolveTeamLeaderUserId(root.teamId);
    if (childOwnerUserId == null) {
      childOwnerUserId = root.ownerUserId;
    }
    for (GeneratedChildSpec childSpec : DISPATCH_GENERATED_CHILDREN) {
      ModuleSpec module = requireModule(childSpec.moduleKey());
      ThreeCheckRecord existing = firstChildRecord(rootId, module.moduleKey());
      if (existing != null) {
        clearDispatchPause(existing, module, operatorId);
        continue;
      }
      ThreeCheckRecord child = new ThreeCheckRecord();
      child.moduleKey = module.moduleKey();
      child.recordNo = recordNo(module, root.businessDate);
      child.taskId = root.taskId;
      child.rootDispatchRecordId = rootId;
      child.companyId = root.companyId;
      child.departmentId = root.departmentId;
      child.teamId = root.teamId;
      child.ownerUserId = childOwnerUserId;
      child.businessDate = root.businessDate;
      child.status = ThreeCheckStatus.DRAFT;
      child.payloadJson = writePayload(autoGeneratedChildPayload(childSpec, root));
      child.imageCheckStatus = "未上传";
      child.videoCheckStatus = "未上传";
      child.reminderCount = 0;
      child.version = 0;
      child.createdBy = operatorId;
      child.updatedBy = operatorId;
      child.sourceChannel = root.sourceChannel;
      child.sourceRecordId = autoDispatchSourceId(rootId, module.moduleKey());
      child.clientRequestId = autoDispatchSourceId(rootId, module.moduleKey());
      child.lastSyncedAt = LocalDateTime.now();
      child.createdAt = child.lastSyncedAt;
      child.updatedAt = child.lastSyncedAt;
      child.deleted = 0;
      recordMapper.insert(child);
      workflowSupport.writeStatusLog(
          module.bizType(), child.id, null, child.status, "AUTO_CREATE", operatorId, "派班生效自动生成任务");
      notificationEventPublisher.threeCheckAssigned(
          module.moduleKey(),
          module.bizType(),
          child.id,
          child.recordNo,
          child.businessDate,
          child.ownerUserId,
          operatorId);
    }
  }

  private Map<String, Object> autoGeneratedChildPayload(GeneratedChildSpec childSpec, ThreeCheckRecord root) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("statusLabel", childSpec.statusLabel());
    payload.put("autoGenerated", true);
    payload.put("sourceDispatchRecordId", String.valueOf(root.rootDispatchRecordId == null ? root.id : root.rootDispatchRecordId));
    payload.put("sourceDispatchRecordNo", root.recordNo);
    payload.put("sourceDispatchModuleKey", root.moduleKey);
    if ("pre-shift-meeting".equals(childSpec.moduleKey())) {
      payload.put("meetingContent", "");
      payload.put("attendeesText", "");
    } else {
      payload.put("owner", "");
    }
    addSafetyConfirmSnapshotIfPreShiftMeeting(payload, childSpec.moduleKey(), root);
    addCheckItemSnapshotIfInspection(payload, childSpec.moduleKey(), root);
    return payload;
  }

  private void addSafetyConfirmSnapshotIfPreShiftMeeting(
      Map<String, Object> payload, String moduleKey, ThreeCheckRecord root) {
    if (!"pre-shift-meeting".equals(moduleKey)) {
      return;
    }
    SystemTeamCheckTemplateResponse template =
        teamCheckTemplateService.resolve(
            root.companyId, root.departmentId, root.teamId, "PRE_SHIFT_MEETING_CONFIRMATION");
    if (template == null || template.items() == null || template.items().isEmpty()) {
      return;
    }
    payload.put(
        "safetyConfirmItems",
        template.items().stream().map(this::safetyConfirmSnapshot).toList());
    payload.put(
        "safetyConfirmTemplateSource",
        Map.of(
            "id", String.valueOf(template.id()),
            "scope", template.scope(),
            "name", template.name(),
            "inspectionStage", template.inspectionStage()));
    payload.put(
        "safetyConfirmTemplateSnapshotAt",
        LocalDateTime.now().format(DISPLAY_DATE_TIME_FORMATTER));
  }

  private Map<String, Object> safetyConfirmSnapshot(SystemTeamCheckTemplateItemResponse item) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("libraryItemId", item.libraryItemId() == null ? null : String.valueOf(item.libraryItemId()));
    snapshot.put("riskType", item.riskType());
    snapshot.put("safetyItem", item.checkItem());
    snapshot.put("confirmStatus", "");
    snapshot.put("sortOrder", item.sortOrder());
    return snapshot;
  }

  private void addCheckItemSnapshotIfInspection(
      Map<String, Object> payload, String moduleKey, ThreeCheckRecord root) {
    if (!isTeamCheckInspectionModule(moduleKey)) {
      return;
    }
    SystemTeamCheckTemplateResponse template =
        teamCheckTemplateService.resolve(root.companyId, root.departmentId, root.teamId, stageForInspectionModule(moduleKey));
    if (template == null || template.items() == null || template.items().isEmpty()) {
      return;
    }
    payload.put(
        "checkItems",
        template.items().stream().map(this::checkItemSnapshot).toList());
    payload.put(
        "checkItemTemplateSource",
        Map.of(
            "id", String.valueOf(template.id()),
            "scope", template.scope(),
            "name", template.name(),
            "inspectionStage", template.inspectionStage()));
    payload.put("checkItemTemplateSnapshotAt", LocalDateTime.now().format(DISPLAY_DATE_TIME_FORMATTER));
  }

  private Map<String, Object> checkItemSnapshot(SystemTeamCheckTemplateItemResponse item) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("libraryItemId", item.libraryItemId() == null ? null : String.valueOf(item.libraryItemId()));
    snapshot.put("riskType", item.riskType());
    snapshot.put("checkItem", item.checkItem());
    snapshot.put("checkResult", "");
    snapshot.put("defaultCheckResult", item.defaultCheckResult());
    snapshot.put("defaultRectificationDescription", item.defaultRectificationDescription());
    snapshot.put("defaultFollowUpPlan", item.defaultFollowUpPlan());
    snapshot.put("requireImage", item.requireImage());
    snapshot.put("requireVideo", item.requireVideo());
    snapshot.put("sortOrder", item.sortOrder());
    return snapshot;
  }

  private void ensureInspectionCheckResultsBeforeSubmit(
      ModuleSpec module, ThreeCheckRecord record, Map<String, Object> payload) {
    if (!isTeamCheckInspectionModule(module.moduleKey())) {
      return;
    }
    if (!hasCheckItemSnapshots(payload)) {
      addCheckItemSnapshotIfInspection(payload, module.moduleKey(), record);
    }
    Object rawItems = payload.get("checkItems");
    if (!(rawItems instanceof List<?> items) || items.isEmpty()) {
      return;
    }
    List<Map<String, Object>> normalizedItems = new ArrayList<>();
    for (int index = 0; index < items.size(); index++) {
      Object rawItem = items.get(index);
      if (!(rawItem instanceof Map<?, ?> source)) {
        throw new BusinessException("检查项数据不完整");
      }
      Map<String, Object> item = new LinkedHashMap<>();
      source.forEach((key, value) -> item.put(String.valueOf(key), value));
      String result = nonBlankText(item.get("checkResult"));
      if (result == null) {
        throw new BusinessException("请填写第 " + (index + 1) + " 项检查结果");
      }
      if (!"无隐患".equals(result) && !"有隐患".equals(result)) {
        throw new BusinessException("第 " + (index + 1) + " 项检查结果只能选择无隐患或有隐患");
      }
      if (!isPostShiftInspectionModule(module.moduleKey()) && nonBlankText(item.get("riskType")) == null) {
        throw new BusinessException("请填写第 " + (index + 1) + " 项风险");
      }
      if (nonBlankText(item.get("checkItem")) == null) {
        throw new BusinessException("请填写第 " + (index + 1) + " 项检查项");
      }
      if ("key-sites".equals(module.moduleKey()) && "有隐患".equals(result)) {
        if (nonBlankText(item.get("hazardDescription")) == null) {
          throw new BusinessException("请填写第 " + (index + 1) + " 项隐患描述");
        }
        if (required(item.get("requireImage"))
            && firstNonBlank(item, "beforePhoto", "photo", "image", "imageUpload") == null) {
          throw new BusinessException("请上传第 " + (index + 1) + " 项整改前图片");
        }
        if (required(item.get("requireVideo"))
            && firstNonBlank(item, "beforeVideo", "video", "videoUpload") == null) {
          throw new BusinessException("请上传第 " + (index + 1) + " 项整改前视频");
        }
      }
      item.put("checkResult", result);
      normalizedItems.add(item);
    }
    payload.put("checkItems", normalizedItems);
  }

  private void ensurePreShiftMeetingSafetyConfirmBeforeSubmit(
      ModuleSpec module, ThreeCheckRecord record, Map<String, Object> payload) {
    if (!"pre-shift-meeting".equals(module.moduleKey())) {
      return;
    }
    if (!hasSafetyConfirmSnapshots(payload)) {
      return;
    }
    Object rawItems = payload.get("safetyConfirmItems");
    if (!(rawItems instanceof List<?> items) || items.isEmpty()) {
      return;
    }
    List<Map<String, Object>> normalizedItems = new ArrayList<>();
    for (int index = 0; index < items.size(); index++) {
      Object rawItem = items.get(index);
      if (!(rawItem instanceof Map<?, ?> source)) {
        throw new BusinessException("安全确认事项数据不完整");
      }
      Map<String, Object> item = new LinkedHashMap<>();
      source.forEach((key, value) -> item.put(String.valueOf(key), value));
      if (nonBlankText(item.get("riskType")) == null) {
        throw new BusinessException("请填写第 " + (index + 1) + " 项风险");
      }
      if (nonBlankText(item.get("safetyItem")) == null) {
        throw new BusinessException("请填写第 " + (index + 1) + " 项安全注意事项");
      }
      String confirmStatus = nonBlankText(item.get("confirmStatus"));
      if (!"已确认".equals(confirmStatus)) {
        throw new BusinessException("请确认第 " + (index + 1) + " 项安全确认事项");
      }
      item.put("confirmStatus", confirmStatus);
      normalizedItems.add(item);
    }
    payload.put("safetyConfirmItems", normalizedItems);
  }

  private void ensurePreShiftMeetingMediaCheckBeforeSubmit(ModuleSpec module, ThreeCheckRecord record) {
    if (!"pre-shift-meeting".equals(module.moduleKey())) {
      return;
    }
    if (isMediaCheckMissing(record.imageCheckStatus) && isMediaCheckMissing(record.videoCheckStatus)) {
      throw new BusinessException("班前会提交前必须完成图片或视频打卡");
    }
  }

  private void assignOneShiftSubmitOwnerToTeamLeader(
      ModuleSpec module, ThreeCheckRecord record, Map<String, Object> payload, CurrentUser user) {
    if (!ONE_SHIFT_SUBMIT_OWNER_MODULES.contains(module.moduleKey())
        || !accessPolicyService.isTeamLeader(user)) {
      return;
    }
    String ownerName =
        user.realName() == null || user.realName().isBlank() ? user.username() : user.realName();
    record.ownerUserId = user.userId();
    payload.put("owner", ownerName);
    if (isTeamCheckInspectionModule(module.moduleKey())) {
      payload.put("responsiblePerson", ownerName);
    }
  }

  private boolean isMediaCheckMissing(String status) {
    return status == null || status.isBlank() || "未上传".equals(status) || "待上传".equals(status);
  }

  private boolean hasSafetyConfirmSnapshots(Map<String, Object> payload) {
    return payload.get("safetyConfirmItems") instanceof List<?> items && !items.isEmpty();
  }

  private boolean hasCheckItemSnapshots(Map<String, Object> payload) {
    return payload.get("checkItems") instanceof List<?> items && !items.isEmpty();
  }

  private String nonBlankText(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isBlank() ? null : text;
  }

  private boolean required(Object value) {
    return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
  }

  private String firstNonBlank(Map<String, Object> values, String... keys) {
    for (String key : keys) {
      String value = nonBlankText(values.get(key));
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private boolean isTeamCheckInspectionModule(String moduleKey) {
    return "pre-shift-inspection".equals(moduleKey)
        || "mid-shift-inspection".equals(moduleKey)
        || "post-shift-inspection".equals(moduleKey)
        || "key-sites".equals(moduleKey);
  }

  private boolean isPostShiftInspectionModule(String moduleKey) {
    return "post-shift-inspection".equals(moduleKey);
  }

  private String stageForInspectionModule(String moduleKey) {
    return switch (moduleKey) {
      case "pre-shift-inspection" -> "PRE_SHIFT_INSPECTION";
      case "mid-shift-inspection" -> "MID_SHIFT_INSPECTION";
      case "post-shift-inspection" -> "POST_SHIFT_INSPECTION";
      case "key-sites" -> "KEY_SITES";
      default -> throw new BusinessException("检查阶段不合法");
    };
  }

  private void markDispatchChildrenPaused(ThreeCheckRecord root, String reason, Long operatorId) {
    Long rootId = root.rootDispatchRecordId == null ? root.id : root.rootDispatchRecordId;
    for (ThreeCheckRecord child : dispatchChildRecords(rootId)) {
      ModuleSpec module = requireModule(child.moduleKey);
      Map<String, Object> payload = readPayload(child.payloadJson);
      payload.put("rootDispatchPaused", true);
      payload.put("rootDispatchPausedAt", LocalDateTime.now().format(DISPLAY_DATE_TIME_FORMATTER));
      payload.put("rootDispatchWithdrawReason", blankToNull(reason));
      child.payloadJson = writePayload(payload);
      touch(child, operatorId);
      concurrentUpdater.update(child);
      workflowSupport.writeStatusLog(
          module.bizType(),
          child.id,
          child.status,
          child.status,
          "ROOT_DISPATCH_WITHDRAWN",
          operatorId,
          reason);
    }
  }

  private void clearDispatchPause(ThreeCheckRecord child, ModuleSpec module, Long operatorId) {
    Map<String, Object> payload = readPayload(child.payloadJson);
    boolean removedPaused = payload.remove("rootDispatchPaused") != null;
    boolean removedPausedAt = payload.remove("rootDispatchPausedAt") != null;
    boolean removedReason = payload.remove("rootDispatchWithdrawReason") != null;
    boolean changed = removedPaused || removedPausedAt || removedReason;
    if (!changed) {
      return;
    }
    child.payloadJson = writePayload(payload);
    touch(child, operatorId);
    concurrentUpdater.update(child);
    workflowSupport.writeStatusLog(
        module.bizType(),
        child.id,
        child.status,
        child.status,
        "ROOT_DISPATCH_RESUMED",
        operatorId,
        "派班重新生效");
  }

  private ThreeCheckRecord firstChildRecord(Long rootDispatchRecordId, String moduleKey) {
    return recordMapper.selectList(
            new QueryWrapper<ThreeCheckRecord>()
                .eq("deleted", 0)
                .eq("root_dispatch_record_id", rootDispatchRecordId)
                .eq("module_key", moduleKey)
                .orderByAsc("id"))
        .stream()
        .findFirst()
        .orElse(null);
  }

  private List<ThreeCheckRecord> dispatchChildRecords(Long rootDispatchRecordId) {
    return recordMapper.selectList(
        new QueryWrapper<ThreeCheckRecord>()
            .eq("deleted", 0)
            .eq("root_dispatch_record_id", rootDispatchRecordId)
            .in(
                "module_key",
                "pre-shift-meeting",
                "pre-shift-inspection",
                "mid-shift-inspection",
                "post-shift-inspection")
            .orderByAsc("id"));
  }

  private String autoDispatchSourceId(Long rootDispatchRecordId, String moduleKey) {
    return "auto-dispatch-" + rootDispatchRecordId + "-" + moduleKey;
  }

  private ThreeCheckRecord pickFlowRecord(
      FlowStageSpec stage, ThreeCheckRecord root, Map<String, ThreeCheckRecord> records) {
    if (stage.moduleKeys().contains(root.moduleKey)) {
      return root;
    }
    for (String moduleKey : stage.moduleKeys()) {
      ThreeCheckRecord record = records.get(moduleKey);
      if (record != null) {
        return record;
      }
    }
    return null;
  }

  private ModuleSpec requireModule(String moduleKey) {
    ModuleSpec module = MODULES.get(moduleKey);
    if (module == null) {
      throw new BusinessException("一班三查模块不支持");
    }
    return module;
  }

  private ModuleSpec requireHazardInspectionModule(String moduleKey) {
    ModuleSpec module = requireModule(moduleKey);
    if (!HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
      throw new BusinessException("隐患排查模块不支持");
    }
    return module;
  }

  private void validateOrganizationRequest(ModuleSpec module, ThreeCheckRecordUpsertRequest request) {
    if (request.teamId() == null && requiresTeam(module)) {
      throw new BusinessException("班组不能为空");
    }
  }

  private boolean requiresTeam(ModuleSpec module) {
    return !"safety-check".equals(module.moduleKey());
  }

  private ThreeCheckRecordDocumentFlowStatusLog toDocumentFlowStatusLog(
      ModuleSpec module, BizStatusLog log, Map<Long, SysUser> users) {
    SysUser operator = log.operatorId == null ? null : users.get(log.operatorId);
    return new ThreeCheckRecordDocumentFlowStatusLog(
        String.valueOf(log.id),
        log.action,
        log.fromStatus,
        statusLogLabel(module, log.fromStatus),
        log.toStatus,
        statusLogLabel(module, log.toStatus),
        log.operatorId == null ? null : String.valueOf(log.operatorId),
        operator == null ? "" : operator.realName,
        log.remark,
        readOptionalPayload(log.payloadJson),
        formatDateTime(log.createdAt));
  }

  private ThreeCheckRecordListItem toListItem(
      ModuleSpec module, ThreeCheckRecord record, Map<Long, SysOrg> orgs, Map<Long, SysUser> users) {
    Map<String, Object> payload = responsePayload(module, record.status, readPayload(record.payloadJson));
    return new ThreeCheckRecordListItem(
        String.valueOf(record.id),
        module.moduleKey(),
        record.recordNo,
        record.taskId == null ? null : String.valueOf(record.taskId),
        record.rootDispatchRecordId == null ? null : String.valueOf(record.rootDispatchRecordId),
        orgName(orgs, record.companyId),
        orgName(orgs, record.departmentId),
        orgName(orgs, record.teamId),
        users.get(record.ownerUserId) == null ? "" : users.get(record.ownerUserId).realName,
        String.valueOf(record.businessDate),
        String.valueOf(record.businessDate),
        record.imageCheckStatus,
        firstAttachmentUrl(module, record.id, "IMAGE"),
        record.videoCheckStatus,
        firstAttachmentUrl(module, record.id, "VIDEO"),
        record.status,
        statusLabel(module, record.status, payload),
        isOverdue(module, record),
        payload,
        canSubmit(module, record),
        canWithdraw(module, record),
        canRemind(module, record.status),
        canCreateRectificationOrder(module, payload),
        record.sourceChannel,
        record.version);
  }

  private ThreeCheckRecordDetailResponse toDetail(
      ModuleSpec module, ThreeCheckRecord record, Map<Long, SysOrg> orgs, Map<Long, SysUser> users) {
    Map<String, Object> payload = responsePayload(module, record.status, readPayload(record.payloadJson));
    List<AttachmentResponse> attachments = attachmentSupport.list(module.bizType(), record.id);
    return new ThreeCheckRecordDetailResponse(
        String.valueOf(record.id),
        module.moduleKey(),
        record.recordNo,
        record.taskId == null ? null : String.valueOf(record.taskId),
        record.rootDispatchRecordId == null ? null : String.valueOf(record.rootDispatchRecordId),
        record.companyId,
        orgName(orgs, record.companyId),
        record.departmentId,
        orgName(orgs, record.departmentId),
        record.teamId,
        orgName(orgs, record.teamId),
        record.ownerUserId,
        users.get(record.ownerUserId) == null ? "" : users.get(record.ownerUserId).realName,
        String.valueOf(record.businessDate),
        String.valueOf(record.businessDate),
        record.imageCheckStatus,
        record.videoCheckStatus,
        record.status,
        statusLabel(module, record.status, payload),
        isOverdue(module, record),
        record.reminderCount,
        canSubmit(module, record),
        canWithdraw(module, record),
        canRemind(module, record.status),
        canCreateRectificationOrder(module, payload),
        payload,
        attachments,
        record.sourceChannel,
        record.sourceRecordId,
        record.clientRequestId,
        formatDateTime(record.clientUpdatedAt),
        formatDateTime(record.lastSyncedAt),
        record.version);
  }

  private ThreeCheckWorkflowItem toWorkflowItem(
      ModuleSpec module, BizStatusLog log, Map<Long, SysUser> users) {
    SysUser operator = users.get(log.operatorId);
    return new ThreeCheckWorkflowItem(
        String.valueOf(log.id),
        log.action,
        workflowActionLabel(log.action),
        log.fromStatus,
        workflowStatusLabel(module, log.fromStatus),
        log.toStatus,
        workflowStatusLabel(module, log.toStatus),
        log.operatorId,
        operator == null ? "" : operator.realName,
        log.remark,
        log.createdAt);
  }

  private String workflowActionLabel(String action) {
    return switch (action) {
      case "CREATE" -> "创建记录";
      case "UPDATE" -> "更新记录";
      case "SUBMIT" -> "提交";
      case "WITHDRAW" -> "撤回";
      case "REMIND" -> "催办";
      default -> action;
    };
  }

  private String workflowStatusLabel(ModuleSpec module, String status) {
    if (isBlank(status)) {
      return null;
    }
    return moduleStatusLabel(module.moduleKey(), status);
  }

  private Map<Long, SysOrg> orgMap() {
    return orgMapper.selectList(new QueryWrapper<SysOrg>().eq("deleted", 0)).stream()
        .collect(Collectors.toMap(org -> org.id, Function.identity()));
  }

  private Map<Long, SysUser> userMap() {
    return userMapper.selectList(new QueryWrapper<SysUser>().eq("deleted", 0)).stream()
        .collect(Collectors.toMap(user -> user.id, Function.identity()));
  }

  private ThreeCheckRecordSqlCriteria sqlCriteria(
      ModuleSpec module, ThreeCheckRecordQuery query, CurrentUser user) {
    List<String> statuses = normalizedQueryStatuses(module, query.status());
    String dataScope = dataScopeService.currentDataScope();
    boolean quickShotTeamMemberSelfOnly = shouldRestrictQuickShotToOwnRecords(module.moduleKey(), user);
    boolean teamMemberSelfScope =
        "SELF".equals(dataScope) && accessPolicyService.isTeamMember(user) && !quickShotTeamMemberSelfOnly;
    Long selfOwnerUserId =
        ("SELF".equals(dataScope) && !teamMemberSelfScope) || quickShotTeamMemberSelfOnly ? user.userId() : null;
    boolean accessDenied = false;
    List<Long> accessOrgIds = null;
    if (!"ALL".equals(dataScope) && !"SELF".equals(dataScope)) {
      accessOrgIds = dataScopeService.accessibleOrgIds();
      accessDenied = accessOrgIds.isEmpty();
    }

    List<Long> filterOrgIds = null;
    boolean filterOrgDenied = false;
    if (query.organizationId() != null) {
      filterOrgIds = descendantOrgIds(query.organizationId());
      filterOrgDenied = filterOrgIds.isEmpty();
    }
    List<Long> visibleTeamIds = null;
    List<Long> visibleOwnerUserIds = null;
    if (teamMemberSelfScope) {
      visibleTeamIds = accessPolicyService.visibleTeamMemberTeamIds(user);
      visibleOwnerUserIds = List.of(user.userId());
    } else if (accessPolicyService.shouldApplyTeamLeaderNarrowing(user, dataScope)) {
      visibleTeamIds = accessPolicyService.visibleTeamIds(user);
      visibleOwnerUserIds = accessPolicyService.visibleOwnerUserIds(user);
    }
    boolean curtainWallOnly = accessPolicyService.shouldApplyCurtainWallNarrowing(user, dataScope);
    Boolean overdue = OVERDUE_MODULES.contains(module.moduleKey()) ? query.overdue() : null;

    return new ThreeCheckRecordSqlCriteria(
        module.moduleKey(),
        statuses,
        query.companyId(),
        query.departmentId(),
        query.teamId(),
        query.rootDispatchRecordId(),
        blankToNull(query.sourceChannel()),
        blankToNull(query.pointsReason()),
        query.dateStart(),
        query.dateEnd(),
        blankToNull(query.company()),
        blankToNull(query.department()),
        blankToNull(query.team()),
        selfOwnerUserId,
        accessOrgIds,
        accessDenied,
        filterOrgIds,
        filterOrgDenied,
        visibleTeamIds,
        visibleOwnerUserIds,
        curtainWallOnly,
        overdue,
        overdue == null ? null : ThreeCheckOverdueSupport.today());
  }

  private boolean isOverdue(ModuleSpec module, ThreeCheckRecord record) {
    return OVERDUE_MODULES.contains(module.moduleKey())
        && ThreeCheckOverdueSupport.isOverdue(record.businessDate, record.status);
  }

  private List<String> normalizedQueryStatuses(ModuleSpec module, String status) {
    if (isBlank(status) || "all".equalsIgnoreCase(status.trim())) {
      return null;
    }
    String normalizedStatus = normalizeStatus(module, status, null);
    if (isDispatchModule(module.moduleKey()) && ThreeCheckStatus.DRAFT.equals(normalizedStatus)) {
      return List.of(ThreeCheckStatus.DRAFT, ThreeCheckStatus.WITHDRAWN);
    }
    if ("quick-shot".equals(module.moduleKey())) {
      return switch (normalizedStatus) {
        case ThreeCheckStatus.PENDING_REVIEW -> List.of(ThreeCheckStatus.PENDING_REVIEW, ThreeCheckStatus.DRAFT, "PENDING_APPROVAL");
        case ThreeCheckStatus.REVIEWED -> List.of(ThreeCheckStatus.REVIEWED, ThreeCheckStatus.OPENED, "APPROVED");
        case ThreeCheckStatus.PENDING_RECTIFICATION ->
            List.of(ThreeCheckStatus.PENDING_RECTIFICATION, ThreeCheckStatus.WITHDRAWN);
        case ThreeCheckStatus.ACCEPTED -> List.of(ThreeCheckStatus.ACCEPTED, ThreeCheckStatus.ARCHIVED);
        case ThreeCheckStatus.REJECTED -> List.of(ThreeCheckStatus.REJECTED);
        default -> List.of(normalizedStatus);
      };
    }
    return List.of(normalizedStatus);
  }

  private List<Long> descendantOrgIds(Long organizationId) {
    SysOrg target = orgMapper.selectById(organizationId);
    if (target == null || Integer.valueOf(1).equals(target.deleted)) {
      return List.of();
    }
    if (target.orgPath == null || target.orgPath.isBlank()) {
      return List.of(organizationId);
    }
    return orgMapper
        .selectList(
            new QueryWrapper<SysOrg>()
                .eq("deleted", 0)
                .likeRight("org_path", target.orgPath)
                .orderByAsc("sort_order", "id"))
        .stream()
        .map(org -> org.id)
        .toList();
  }

  private Map<String, Long> statusCounts(List<Map<String, Object>> rows) {
    Map<String, Long> counts = new LinkedHashMap<>();
    counts.put(ThreeCheckStatus.DRAFT, 0L);
    counts.put(ThreeCheckStatus.OPENED, 0L);
    counts.put(ThreeCheckStatus.WITHDRAWN, 0L);
    counts.put(ThreeCheckStatus.ARCHIVED, 0L);
    counts.put(ThreeCheckStatus.PENDING_REVIEW, 0L);
    counts.put(ThreeCheckStatus.REVIEWED, 0L);
    counts.put(ThreeCheckStatus.PENDING_RECTIFICATION, 0L);
    counts.put(ThreeCheckStatus.RECTIFIED, 0L);
    counts.put(ThreeCheckStatus.PENDING_ACCEPTANCE, 0L);
    counts.put(ThreeCheckStatus.ACCEPTED, 0L);
    counts.put(ThreeCheckStatus.REJECTED, 0L);
    for (Map<String, Object> row : rows) {
      String status = stringValue(row, "status");
      if (!status.isBlank()) {
        counts.put(status, longValue(row, "count"));
      }
    }
    return counts;
  }

  private List<ThreeCheckRecordStatisticsResponse.DateCount> dateCounts(
      List<Map<String, Object>> rows) {
    return rows.stream()
        .map(
            row ->
                new ThreeCheckRecordStatisticsResponse.DateCount(
                    stringValue(row, "bucket_date"), longValue(row, "count")))
        .toList();
  }

  private List<ThreeCheckRecordStatisticsResponse.OrganizationCount> organizationCounts(
      List<Map<String, Object>> rows) {
    return rows.stream()
        .map(
            row ->
                new ThreeCheckRecordStatisticsResponse.OrganizationCount(
                    nullableLongValue(row, "organization_id"),
                    stringValue(row, "organization_name"),
                    stringValue(row, "organization_type"),
                    longValue(row, "count")))
        .toList();
  }

  private long longValue(Map<String, Object> row, String key) {
    Long value = nullableLongValue(row, key);
    return value == null ? 0L : value;
  }

  private Long nullableLongValue(Map<String, Object> row, String key) {
    Object value = mapValue(row, key);
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String text && !text.isBlank()) {
      return Long.parseLong(text);
    }
    return null;
  }

  private String stringValue(Map<String, Object> row, String key) {
    Object value = mapValue(row, key);
    return value == null ? "" : String.valueOf(value);
  }

  private Object mapValue(Map<String, Object> row, String key) {
    if (row == null) {
      return null;
    }
    if (row.containsKey(key)) {
      return row.get(key);
    }
    String lowerKey = key.toLowerCase(Locale.ROOT);
    if (row.containsKey(lowerKey)) {
      return row.get(lowerKey);
    }
    String upperKey = key.toUpperCase(Locale.ROOT);
    if (row.containsKey(upperKey)) {
      return row.get(upperKey);
    }
    String camelKey = snakeToCamel(key);
    if (row.containsKey(camelKey)) {
      return row.get(camelKey);
    }
    Map<String, Object> normalized = new HashMap<>();
    row.forEach((rowKey, value) -> normalized.put(rowKey.toLowerCase(Locale.ROOT), value));
    return normalized.get(lowerKey);
  }

  private String snakeToCamel(String value) {
    StringBuilder builder = new StringBuilder();
    boolean upperNext = false;
    for (char current : value.toCharArray()) {
      if (current == '_') {
        upperNext = true;
      } else if (upperNext) {
        builder.append(Character.toUpperCase(current));
        upperNext = false;
      } else {
        builder.append(current);
      }
    }
    return builder.toString();
  }

  private boolean canAccess(ThreeCheckRecord record, CurrentUser user, Map<Long, SysOrg> orgs) {
    String dataScope = dataScopeService.currentDataScope();
    if (accessPolicyService.shouldApplyCurtainWallNarrowing(user, dataScope)
        && !isCurtainWallVisibleRecord(record)) {
      return false;
    }
    if (shouldRestrictQuickShotToOwnRecords(record.moduleKey, user)) {
      return isOwnQuickShotRecord(record, user);
    }
    boolean orgVisible = canAccessByDataScope(record, user, orgs);
    if (!orgVisible) {
      return false;
    }
    if ("SELF".equals(dataScope)) {
      return true;
    }
    if (accessPolicyService.shouldApplyTeamLeaderNarrowing(user, dataScope)) {
      return accessPolicyService.canAccessAsTeamLeader(record, user);
    }
    return true;
  }

  private boolean shouldRestrictQuickShotToOwnRecords(String moduleKey, CurrentUser user) {
    return "quick-shot".equals(moduleKey)
        && accessPolicyService.isTeamMember(user)
        && !hazardPermissionPolicy.hasQuickShotPrivilegedView();
  }

  private boolean isOwnQuickShotRecord(ThreeCheckRecord record, CurrentUser user) {
    return (record.ownerUserId != null && record.ownerUserId.equals(user.userId()))
        || (record.createdBy != null && record.createdBy.equals(user.userId()));
  }

  private boolean isCurtainWallVisibleRecord(ThreeCheckRecord record) {
    if (accessPolicyService.isCurtainWallModule(record.moduleKey)) {
      return true;
    }
    if (record.rootDispatchRecordId == null) {
      return false;
    }
    ThreeCheckRecord root = recordMapper.selectById(record.rootDispatchRecordId);
    return accessPolicyService.isCurtainWallDispatchRoot(root);
  }

  private boolean canAccessByDataScope(
      ThreeCheckRecord record, CurrentUser user, Map<Long, SysOrg> orgs) {
    if ("SELF".equals(dataScopeService.currentDataScope())) {
      return record.ownerUserId != null && record.ownerUserId.equals(user.userId())
          || accessPolicyService.canAccessAsTeamMember(record, user);
    }
    return workflowSupport.canAccess(
        recordOrgIds(record), user, orgs);
  }

  private List<Long> recordOrgIds(ThreeCheckRecord record) {
    List<Long> orgIds = new ArrayList<>();
    if (record.companyId != null) {
      orgIds.add(record.companyId);
    }
    if (record.departmentId != null) {
      orgIds.add(record.departmentId);
    }
    if (record.teamId != null) {
      orgIds.add(record.teamId);
    }
    return orgIds;
  }

  private boolean canSubmit(ModuleSpec module, ThreeCheckRecord record) {
    String status = record.status;
    if (ONE_SHIFT_SUBMIT_OWNER_MODULES.contains(module.moduleKey()) && isOverdue(module, record)) {
      return false;
    }
    if (HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
      return workflowSupport.canSubmit(status)
          && hazardPermissionPolicy.canSubmitRecord(module.moduleKey());
    }
    if (usesThreeCheckPermissionPolicy(module.moduleKey())) {
      return workflowSupport.canSubmit(status)
          && threeCheckPermissionPolicy.canSubmit(module.moduleKey());
    }
    return false;
  }

  private boolean canWithdraw(ModuleSpec module, ThreeCheckRecord record) {
    String status = record.status;
    if (ONE_SHIFT_SUBMIT_OWNER_MODULES.contains(module.moduleKey())
        && (ThreeCheckStatus.ARCHIVED.equals(status)
            || ThreeCheckOverdueSupport.isPastBusinessDate(record.businessDate))) {
      return false;
    }
    if ("quick-shot".equals(module.moduleKey())) {
      return ThreeCheckStatus.REVIEWED.equals(normalizeQuickShotLegacyStatus(status))
          && hazardPermissionPolicy.canVoidRecord(module.moduleKey());
    }
    if (HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
      return workflowSupport.canWithdraw(status)
          && hazardPermissionPolicy.canVoidRecord(module.moduleKey());
    }
    if (usesThreeCheckPermissionPolicy(module.moduleKey())) {
      return workflowSupport.canWithdraw(status)
          && threeCheckPermissionPolicy.canVoid(module.moduleKey());
    }
    return false;
  }

  private void ensureNotExpiredForSubmit(ModuleSpec module, ThreeCheckRecord record) {
    if (ONE_SHIFT_SUBMIT_OWNER_MODULES.contains(module.moduleKey()) && isOverdue(module, record)) {
      throw new BusinessException("已逾期的一班三查记录不能提交");
    }
  }

  private void ensureWithdrawAllowedByBusinessDate(ModuleSpec module, ThreeCheckRecord record) {
    if (!ONE_SHIFT_SUBMIT_OWNER_MODULES.contains(module.moduleKey())) {
      return;
    }
    if (ThreeCheckStatus.ARCHIVED.equals(record.status)) {
      throw new BusinessException("已归档的一班三查记录不能撤回");
    }
    if (ThreeCheckOverdueSupport.isPastBusinessDate(record.businessDate)) {
      throw new BusinessException("已过业务日期的一班三查记录不能撤回");
    }
  }

  private void ensureHistoricalBusinessDateUnchanged(
      ModuleSpec module, ThreeCheckRecord record, LocalDate requestedBusinessDate) {
    if (ONE_SHIFT_SUBMIT_OWNER_MODULES.contains(module.moduleKey())
        && ThreeCheckOverdueSupport.isPastBusinessDate(record.businessDate)
        && !record.businessDate.equals(requestedBusinessDate)) {
      throw new BusinessException("历史一班三查记录不能修改业务日期");
    }
  }

  private boolean canRemind(ModuleSpec module, String status) {
    if ("quick-shot".equals(module.moduleKey())) {
      return ThreeCheckStatus.REVIEWED.equals(normalizeQuickShotLegacyStatus(status))
          && hazardPermissionPolicy.canVoidRecord(module.moduleKey());
    }
    if (usesThreeCheckPermissionPolicy(module.moduleKey())) {
      return ThreeCheckStatus.OPENED.equals(status)
          && threeCheckPermissionPolicy.canRemind(module.moduleKey());
    }
    return false;
  }

  private boolean canCreateRectificationOrder(ModuleSpec module, Map<String, Object> payload) {
    if (!RECTIFICATION_ORDER_SOURCE_MODULES.contains(module.moduleKey())
        || !threeCheckPermissionPolicy.canCreateRectificationOrder()) {
      return false;
    }
    Object rawItems = payload.get("checkItems");
    if (!(rawItems instanceof List<?> items)) {
      return false;
    }
    return items.stream()
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .anyMatch(item -> "有隐患".equals(String.valueOf(item.get("checkResult"))));
  }

  private String orgName(Map<Long, SysOrg> orgs, Long orgId) {
    SysOrg org = orgs.get(orgId);
    return org == null ? "" : org.orgName;
  }

  private String firstAttachmentUrl(ModuleSpec module, Long recordId, String fileKind) {
    return attachmentSupport.firstUrl(module.bizType(), recordId, fileKind);
  }

  private ThreeCheckRecord findByClientRequestId(
      ModuleSpec module, String sourceChannel, String clientRequestId) {
    if (isBlank(clientRequestId)) {
      return null;
    }
    return recordMapper.selectOne(
        new QueryWrapper<ThreeCheckRecord>()
            .eq("deleted", 0)
            .eq("module_key", module.moduleKey())
            .eq("source_channel", sourceChannel)
            .eq("client_request_id", clientRequestId.trim())
            .last("limit 1"));
  }

  private ThreeCheckRecord findBySourceRecordId(
      ModuleSpec module, String sourceChannel, String sourceRecordId) {
    if (isBlank(sourceRecordId)) {
      return null;
    }
    return recordMapper.selectOne(
        new QueryWrapper<ThreeCheckRecord>()
            .eq("deleted", 0)
            .eq("module_key", module.moduleKey())
            .eq("source_channel", sourceChannel)
            .eq("source_record_id", sourceRecordId.trim())
            .last("limit 1"));
  }

  private void requireVersionMatch(ThreeCheckRecord record, Integer requestVersion, boolean required) {
    if (requestVersion == null) {
      if (required) {
        throw new ConflictException("记录已被其他端更新，请刷新后重试");
      }
      return;
    }
    if (!requestVersion.equals(record.version)) {
      throw new ConflictException("记录已被其他端更新，请刷新后重试");
    }
  }

  private String normalizeStatus(ModuleSpec module, String requestedStatus, String defaultStatus) {
    if (isPointsFlowModule(module.moduleKey())) {
      return ThreeCheckStatus.OPENED;
    }
    if (isBlank(requestedStatus)) {
      return defaultStatus;
    }
    String trimmed = requestedStatus.trim();
    String upper = trimmed.toUpperCase(Locale.ROOT);
    if (WORKFLOW_STATUSES.contains(upper)) {
      return upper;
    }
    String moduleStatus = moduleStatusByLabel(module.moduleKey(), trimmed);
    if (moduleStatus != null) {
      return moduleStatus;
    }
    return switch (trimmed) {
      case "生效", "已生效", "已开会议", "已检查", "待验收" -> ThreeCheckStatus.OPENED;
      case "不生效", "未生效", "待开会议", "待检查" -> ThreeCheckStatus.DRAFT;
      case "待整改" -> ThreeCheckStatus.WITHDRAWN;
      case "已验收", "已归档", "已完成" -> ThreeCheckStatus.ARCHIVED;
      default -> throw new BusinessException("状态不合法：" + trimmed + "（" + module.moduleKey() + "）");
    };
  }

  private String defaultStatus(ModuleSpec module) {
    return "quick-shot".equals(module.moduleKey())
        ? ThreeCheckStatus.PENDING_REVIEW
        : ThreeCheckStatus.DRAFT;
  }

  private String normalizeQuickShotLegacyStatus(String status) {
    return switch (status) {
      case ThreeCheckStatus.DRAFT, "PENDING_APPROVAL" -> ThreeCheckStatus.PENDING_REVIEW;
      case ThreeCheckStatus.OPENED, "APPROVED" -> ThreeCheckStatus.REVIEWED;
      case ThreeCheckStatus.WITHDRAWN -> ThreeCheckStatus.PENDING_RECTIFICATION;
      case ThreeCheckStatus.ARCHIVED -> ThreeCheckStatus.ACCEPTED;
      default -> status;
    };
  }

  private String moduleStatusByLabel(String moduleKey, String label) {
    return switch (moduleKey) {
      case "safety-check" ->
          switch (label) {
            case "待检查" -> ThreeCheckStatus.DRAFT;
            case "已检查" -> ThreeCheckStatus.OPENED;
            default -> null;
          };
      case "hazard-rectification" ->
          switch (label) {
            case "待整改" -> ThreeCheckStatus.DRAFT;
            case "已整改" -> ThreeCheckStatus.OPENED;
            default -> null;
          };
      case "quick-shot" ->
          switch (label) {
            case "待审批", "待审核" -> ThreeCheckStatus.PENDING_REVIEW;
            case "已审批", "已审核", "通过" -> ThreeCheckStatus.REVIEWED;
            case "待整改" -> ThreeCheckStatus.PENDING_RECTIFICATION;
            case "已整改" -> ThreeCheckStatus.RECTIFIED;
            case "待验收" -> ThreeCheckStatus.PENDING_ACCEPTANCE;
            case "已验收", "不通过" -> ThreeCheckStatus.ACCEPTED;
            case "已驳回" -> ThreeCheckStatus.REJECTED;
            default -> null;
          };
      case "curtain-wall-penalty" ->
          switch (label) {
            case "待批准" -> ThreeCheckStatus.DRAFT;
            case "已生效（未读）" -> ThreeCheckStatus.OPENED;
            case "已生效已读" -> ThreeCheckStatus.ARCHIVED;
            default -> null;
          };
      case "curtain-wall-routine-check" ->
          switch (label) {
            case "待检查" -> ThreeCheckStatus.DRAFT;
            case "已检查" -> ThreeCheckStatus.OPENED;
            default -> null;
          };
      default -> null;
    };
  }

  private Map<String, Object> withStatusLabel(
      Map<String, Object> payload, ModuleSpec module, String status) {
    return syncDerivedStatusFields(payload, module, status, false);
  }

  private Map<String, Object> forceStatusLabel(
      Map<String, Object> payload, ModuleSpec module, String status) {
    return syncDerivedStatusFields(payload, module, status, true);
  }

  private Map<String, Object> responsePayload(
      ModuleSpec module, String status, Map<String, Object> payload) {
    Map<String, Object> synced =
        syncDerivedStatusFields(payload, module, status, isDispatchModule(module.moduleKey()));
    if (isRootDispatchPausedChild(module, synced)) {
      synced.put("statusLabel", "派班已撤回/暂停");
    }
    return synced;
  }

  private Map<String, Object> syncDerivedStatusFields(
      Map<String, Object> payload, ModuleSpec module, String status, boolean forceStatusLabel) {
    Map<String, Object> copy = new LinkedHashMap<>(payload);
    String label = moduleStatusLabel(module.moduleKey(), status);
    Object statusLabel = copy.get("statusLabel");
    if (forceStatusLabel
        || "quick-shot".equals(module.moduleKey())
        || !(statusLabel instanceof String value)
        || value.isBlank()) {
      copy.put("statusLabel", label);
    }
    if (isDispatchModule(module.moduleKey())) {
      copy.put("dispatchStatus", label);
    }
    return copy;
  }

  private String statusLabel(ModuleSpec module, String status, Map<String, Object> payload) {
    if (isRootDispatchPausedChild(module, payload)) {
      return "派班已撤回/暂停";
    }
    if ("quick-shot".equals(module.moduleKey())) {
      return moduleStatusLabel(module.moduleKey(), status);
    }
    Object label = payload.get("statusLabel");
    if (label instanceof String value && !value.isBlank()) {
      return value;
    }
    return moduleStatusLabel(module.moduleKey(), status);
  }

  private boolean isRootDispatchPausedChild(ModuleSpec module, Map<String, Object> payload) {
    if (isDispatchModule(module.moduleKey())) {
      return false;
    }
    Object paused = payload.get("rootDispatchPaused");
    return paused instanceof Boolean value && value;
  }

  private String statusLogLabel(ModuleSpec module, String status) {
    if (isBlank(status)) {
      return null;
    }
    return moduleStatusLabel(module.moduleKey(), status);
  }

  private void recordUpdateChangeHistory(
      ModuleSpec module,
      ThreeCheckRecord before,
      Map<String, Object> beforePayload,
      ThreeCheckRecord after,
      Map<String, Object> afterPayload,
      Long operatorId) {
    if (!HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
      return;
    }
    historyService.recordUpdate(
        module.moduleKey(),
        module.bizType(),
        before,
        beforePayload,
        after,
        afterPayload,
        operatorId,
        this::moduleStatusLabel);
  }

  private void recordChangeHistory(
      ModuleSpec module,
      ThreeCheckRecord record,
      String action,
      String fieldKey,
      String fieldLabel,
      String beforeValue,
      String afterValue,
      String valueType,
      Long operatorId,
      String remark) {
    if (!HAZARD_INSPECTION_MODULES.contains(module.moduleKey())) {
      return;
    }
    historyService.record(
        module.moduleKey(),
        module.bizType(),
        record,
        action,
        fieldKey,
        fieldLabel,
        beforeValue,
        afterValue,
        valueType,
        operatorId,
        remark);
  }

  private String moduleStatusLabel(String moduleKey, String status) {
    if (isPointsFlowModule(moduleKey)) {
      return status;
    }
    if ("safety-check".equals(moduleKey)) {
      return switch (status) {
        case ThreeCheckStatus.OPENED, ThreeCheckStatus.ARCHIVED -> "已检查";
        default -> "待检查";
      };
    }
    if ("hazard-rectification".equals(moduleKey)) {
      return switch (status) {
        case ThreeCheckStatus.OPENED, ThreeCheckStatus.ARCHIVED -> "已整改";
        default -> "待整改";
      };
    }
    if ("quick-shot".equals(moduleKey)) {
      return switch (normalizeQuickShotLegacyStatus(status)) {
        case ThreeCheckStatus.REVIEWED -> "已审核";
        case ThreeCheckStatus.PENDING_RECTIFICATION -> "待整改";
        case ThreeCheckStatus.RECTIFIED -> "已整改";
        case ThreeCheckStatus.PENDING_ACCEPTANCE -> "待验收";
        case ThreeCheckStatus.ACCEPTED -> "已验收";
        case ThreeCheckStatus.REJECTED -> "已驳回";
        default -> "待审核";
      };
    }
    if ("curtain-wall-penalty".equals(moduleKey)) {
      return switch (status) {
        case ThreeCheckStatus.OPENED -> "已生效（未读）";
        case ThreeCheckStatus.ARCHIVED -> "已生效已读";
        default -> "待批准";
      };
    }
    if ("curtain-wall-routine-check".equals(moduleKey)) {
      return ThreeCheckStatus.OPENED.equals(status) || ThreeCheckStatus.ARCHIVED.equals(status)
          ? "已检查"
          : "待检查";
    }
    if ("key-sites".equals(moduleKey)) {
      return switch (status) {
        case ThreeCheckStatus.OPENED, ThreeCheckStatus.ARCHIVED -> "已检查";
        default -> "待检查";
      };
    }
    if (moduleKey.endsWith("inspection")) {
      return ThreeCheckStatus.OPENED.equals(status) || ThreeCheckStatus.ARCHIVED.equals(status)
          ? "已检查"
          : "待检查";
    }
    if ("pre-shift-meeting".equals(moduleKey) || "pre-shift-safety-activity".equals(moduleKey)) {
      return ThreeCheckStatus.OPENED.equals(status) || ThreeCheckStatus.ARCHIVED.equals(status)
          ? "已开会议"
          : "待开会议";
    }
    if ("team-dispatch".equals(moduleKey) || "curtain-wall-team-dispatch".equals(moduleKey)) {
      return ThreeCheckStatus.OPENED.equals(status) || ThreeCheckStatus.ARCHIVED.equals(status)
          ? "已生效"
          : "未生效";
    }
    return status;
  }

  private Map<String, Object> payloadFrom(Map<String, Object> payload) {
    return payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
  }

  private Map<String, Object> normalizedPayload(ModuleSpec module, Map<String, Object> payload) {
    Map<String, Object> copy = payloadFrom(payload);
    if (isPointsFlowModule(module.moduleKey())) {
      copy.put("pointsQuantity", positiveInteger(copy.get("pointsQuantity"), "积分数量"));
      Object pointsChange = copy.get("pointsChange");
      if (pointsChange != null && !String.valueOf(pointsChange).isBlank()) {
        copy.put("statusLabel", String.valueOf(pointsChange).trim());
      }
    }
    return copy;
  }

  private Map<String, Object> readPayload(String payloadJson) {
    return payloadCodec.read(payloadJson);
  }

  private Map<String, Object> readOptionalPayload(String payloadJson) {
    return payloadCodec.readOptional(payloadJson);
  }

  private String writePayload(Map<String, Object> payload) {
    return payloadCodec.write(payload);
  }

  private void touch(ThreeCheckRecord record, Long userId) {
    record.updatedBy = userId;
    record.updatedAt = LocalDateTime.now();
  }

  private String resolveClientRequestId(String clientRequestId) {
    String normalized = blankToNull(clientRequestId);
    return normalized == null ? UUID.randomUUID().toString() : normalized;
  }

  private String blankToNull(String value) {
    return isBlank(value) ? null : value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String formatDateTime(LocalDateTime value) {
    return value == null ? null : value.format(DISPLAY_DATE_TIME_FORMATTER);
  }

  private String shortId() {
    return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  private record ModuleSpec(String moduleKey, String bizType, String recordPrefix) {}

  private record FlowStageSpec(String key, String label, List<String> moduleKeys) {}

  private record GeneratedChildSpec(String moduleKey, String statusLabel) {}
}
