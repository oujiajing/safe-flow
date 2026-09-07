package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.ConflictException;
import com.pingan.banzu.common.HazardRectificationOrderStatus;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.common.ThreeCheckBizType;
import com.pingan.banzu.domain.HazardRectificationFlowLog;
import com.pingan.banzu.domain.HazardRectificationOrder;
import com.pingan.banzu.domain.HazardRectificationOrderItem;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.domain.SysUser;
import com.pingan.banzu.domain.ThreeCheckRecord;
import com.pingan.banzu.dto.HazardRectificationFlowLogResponse;
import com.pingan.banzu.dto.HazardRectificationOrderActionRequest;
import com.pingan.banzu.dto.HazardRectificationOrderBatchDeleteRequest;
import com.pingan.banzu.dto.HazardRectificationOrderCreateItemRequest;
import com.pingan.banzu.dto.HazardRectificationOrderCreateRequest;
import com.pingan.banzu.dto.HazardRectificationOrderDetailResponse;
import com.pingan.banzu.dto.HazardRectificationOrderItemResponse;
import com.pingan.banzu.dto.HazardRectificationOrderListItem;
import com.pingan.banzu.dto.HazardRectificationOrderQuery;
import com.pingan.banzu.mapper.HazardRectificationFlowLogMapper;
import com.pingan.banzu.mapper.HazardRectificationOrderItemMapper;
import com.pingan.banzu.mapper.HazardRectificationOrderMapper;
import com.pingan.banzu.mapper.BizAttachmentMapper;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.mapper.SysUserMapper;
import com.pingan.banzu.mapper.ThreeCheckRecordMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HazardRectificationOrderService {

  private static final String SOURCE_THREE_CHECK = "THREE_CHECK";
  private static final String SOURCE_MANUAL = "MANUAL";
  private static final String SOURCE_QUICK_SHOT = "QUICK_SHOT";
  private static final String SOURCE_SAFETY_INSPECTION = "SAFETY_INSPECTION";
  private static final String SOURCE_MODULE_MANUAL = "manual";
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};

  private final HazardRectificationOrderMapper orderMapper;
  private final HazardRectificationOrderConcurrentUpdater orderConcurrentUpdater;
  private final HazardRectificationOrderItemMapper itemMapper;
  private final HazardRectificationFlowLogMapper flowLogMapper;
  private final ObjectMapper objectMapper;
  private final SysOrgMapper orgMapper;
  private final SysUserMapper userMapper;
  private final ThreeCheckRecordMapper recordMapper;
  private final ThreeCheckRecordConcurrentUpdater concurrentUpdater;
  private final BizAttachmentMapper attachmentMapper;
  private final AttachmentUrlResolver attachmentUrlResolver;
  private final HazardPermissionPolicy hazardPermissionPolicy;
  private final NotificationEventPublisher notificationEventPublisher;

  public HazardRectificationOrderService(
      HazardRectificationOrderMapper orderMapper,
      HazardRectificationOrderConcurrentUpdater orderConcurrentUpdater,
      HazardRectificationOrderItemMapper itemMapper,
      HazardRectificationFlowLogMapper flowLogMapper,
      ObjectMapper objectMapper,
      SysOrgMapper orgMapper,
      SysUserMapper userMapper,
      ThreeCheckRecordMapper recordMapper,
      ThreeCheckRecordConcurrentUpdater concurrentUpdater,
      BizAttachmentMapper attachmentMapper,
      AttachmentUrlResolver attachmentUrlResolver,
      HazardPermissionPolicy hazardPermissionPolicy,
      NotificationEventPublisher notificationEventPublisher) {
    this.orderMapper = orderMapper;
    this.orderConcurrentUpdater = orderConcurrentUpdater;
    this.itemMapper = itemMapper;
    this.flowLogMapper = flowLogMapper;
    this.objectMapper = objectMapper;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
    this.recordMapper = recordMapper;
    this.concurrentUpdater = concurrentUpdater;
    this.attachmentMapper = attachmentMapper;
    this.attachmentUrlResolver = attachmentUrlResolver;
    this.hazardPermissionPolicy = hazardPermissionPolicy;
    this.notificationEventPublisher = notificationEventPublisher;
  }

  public PageResult<HazardRectificationOrderListItem> list(HazardRectificationOrderQuery query) {
    assertCanViewHazards();
    int page = Math.max(1, query.page() == null ? 1 : query.page());
    int pageSize = Math.min(100, Math.max(1, query.pageSize() == null ? 20 : query.pageSize()));
    QueryWrapper<HazardRectificationOrder> wrapper = queryWrapper(query);
    long total = orderMapper.selectCount(wrapper);
    List<HazardRectificationOrder> rows =
        orderMapper.selectList(
            queryWrapper(query)
                .orderByDesc("created_at", "id")
                .last("limit " + pageSize + " offset " + ((page - 1) * pageSize)));
    Map<Long, SysOrg> orgs = orgMap();
    return new PageResult<>(rows.stream().map(order -> toListItem(order, orgs)).toList(), total);
  }

  public HazardRectificationOrderDetailResponse detail(Long id) {
    assertCanViewHazards();
    HazardRectificationOrder order = requireOrder(id);
    return toDetail(order);
  }

  public void assertCanUploadAttachment(Long id) {
    hazardPermissionPolicy.assertCanRectifyOrder();
    requireOrder(id);
  }

  @Transactional
  public void batchDelete(HazardRectificationOrderBatchDeleteRequest request) {
    hazardPermissionPolicy.assertCanDeleteOrder();
    CurrentUser user = CurrentUserContext.require();
    List<Long> ids =
        request.ids().stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) {
      throw new BusinessException("请选择要删除的隐患整改工单");
    }
    List<HazardRectificationOrder> orders = ids.stream().map(this::requireOrder).toList();
    orders.forEach(order -> orderConcurrentUpdater.softDelete(order, user.userId()));
  }

  @Transactional
  public HazardRectificationOrderDetailResponse createManual(
      HazardRectificationOrderCreateRequest request) {
    hazardPermissionPolicy.assertCanCreateOrder();
    CurrentUser user = CurrentUserContext.require();
    Long companyId = requireValue(request == null ? null : request.companyId(), "公司");
    Long departmentId = requireValue(request.departmentId(), "部门");
    Long teamId = requireValue(request.teamId(), "班组");
    LocalDate businessDate = requireValue(request.businessDate(), "业务日期");
    List<HazardRectificationOrderCreateItemRequest> items = request.items();
    if (items == null || items.isEmpty()) {
      throw new BusinessException("隐患明细不能为空");
    }

    LocalDateTime now = LocalDateTime.now();
    HazardRectificationOrder order = new HazardRectificationOrder();
    order.orderNo = nextOrderNo();
    order.sourceType = SOURCE_MANUAL;
    order.sourceModuleKey = SOURCE_MODULE_MANUAL;
    order.sourceRecordId = null;
    order.sourceRecordNo = null;
    order.rootDispatchRecordId = null;
    order.companyId = companyId;
    order.departmentId = departmentId;
    order.teamId = teamId;
    order.businessDate = businessDate;
    order.hazardCount = items.size();
    order.status = HazardRectificationOrderStatus.PENDING_ASSIGN;
    order.version = 0;
    order.createdBy = user.userId();
    order.updatedBy = user.userId();
    order.createdAt = now;
    order.updatedAt = now;
    order.deleted = 0;
    orderMapper.insert(order);

    for (int index = 0; index < items.size(); index++) {
      itemMapper.insert(toManualItem(order, items.get(index), index));
    }
    writeFlowLog(
        order.id,
        null,
        order.status,
        "CREATE",
        "手工创建",
        user.userId(),
        "手工创建隐患整改工单",
        null);
    return toDetail(order);
  }

  @Transactional
  public HazardRectificationOrderDetailResponse action(
      Long id, HazardRectificationOrderActionRequest request) {
    CurrentUser user = CurrentUserContext.require();
    HazardRectificationOrder order = requireOrder(id);
    requireVersionMatch(order, request == null ? null : request.version());
    String action = normalizeAction(request == null ? null : request.action());
    assertCanPerformAction(action);
    Map<String, Object> payload =
        request == null || request.payload() == null ? Map.of() : request.payload();
    String from = order.status;
    String to = nextStatus(from, action);

    applyActionPayload(order, action, payload, user);
    order.status = to;
    order.updatedBy = user.userId();
    order.updatedAt = LocalDateTime.now();
    if (HazardRectificationOrderStatus.CLOSED.equals(to)) {
      order.closedAt = order.updatedAt;
    } else if (HazardRectificationOrderStatus.CANCELLED.equals(to)) {
      order.closedAt = null;
    }
    orderConcurrentUpdater.update(order, from);
    syncItemStatus(order);
    String actionLabel = actionLabel(action);
    String remark = actionRemark(action, request == null ? null : request.remark(), payload);
    writeFlowLog(
        order.id,
        from,
        to,
        action,
        actionLabel,
        user.userId(),
        remark,
        writePayload(payload));
    backfillSourceRecord(order, actionLabel, remark);
    notificationEventPublisher.hazardChanged(order, action, user.userId());
    return toDetail(order);
  }

  private void assertCanViewHazards() {
    hazardPermissionPolicy.assertCanView("hazard-rectification");
  }

  private void assertCanPerformAction(String action) {
    switch (action) {
      case "ISSUE_RECTIFICATION", "MARK_RECTIFIED", "REQUEST_ACCEPTANCE" ->
          hazardPermissionPolicy.assertCanRectifyOrder();
      case "ACCEPT", "REJECT_ACCEPTANCE" -> hazardPermissionPolicy.assertCanAcceptOrder();
      case "CANCEL" -> hazardPermissionPolicy.assertCanVoidOrder();
      default -> throw new BusinessException("不支持的操作");
    }
  }

  @Transactional
  public void createOrUpdateFromThreeCheckInspection(Long sourceRecordId, Long operatorId) {
    ThreeCheckRecord record = recordMapper.selectById(sourceRecordId);
    if (record == null || Integer.valueOf(1).equals(record.deleted)) {
      throw new BusinessException("一班三查记录不存在");
    }
    if (!isTeamCheckInspectionModule(record.moduleKey)) {
      return;
    }
    createOrUpdateFromRecord(record, operatorId, SOURCE_THREE_CHECK, "一班三查发现隐患自动生成工单");
  }

  @Transactional
  public HazardRectificationOrderDetailResponse openFromThreeCheckInspection(
      Long sourceRecordId, Long operatorId) {
    ThreeCheckRecord record = recordMapper.selectById(sourceRecordId);
    if (record == null || Integer.valueOf(1).equals(record.deleted)) {
      throw new BusinessException("一班三查记录不存在");
    }
    if (!isTeamCheckInspectionModule(record.moduleKey)) {
      throw new BusinessException("只有班前、班中、班后检查或重点场所检查可以开整改单");
    }
    HazardRectificationOrder order =
        createOrUpdateFromRecord(record, operatorId, SOURCE_THREE_CHECK, "一班三查发现隐患手动开整改单");
    if (order == null) {
      throw new BusinessException("未发现有隐患检查项，不能开整改单");
    }
    return toDetail(order);
  }

  public boolean hasOrderFromThreeCheckInspection(Long sourceRecordId) {
    ThreeCheckRecord record = recordMapper.selectById(sourceRecordId);
    return record != null
        && !Integer.valueOf(1).equals(record.deleted)
        && isTeamCheckInspectionModule(record.moduleKey)
        && existingSourceOrder(SOURCE_THREE_CHECK, record) != null;
  }

  @Transactional
  public void createOrUpdateFromSafetyCheck(Long sourceRecordId, Long operatorId) {
    ThreeCheckRecord record = recordMapper.selectById(sourceRecordId);
    if (record == null || Integer.valueOf(1).equals(record.deleted)) {
      throw new BusinessException("安全检查记录不存在");
    }
    if (!"safety-check".equals(record.moduleKey)) {
      return;
    }
    createOrUpdateFromRecord(record, operatorId, SOURCE_SAFETY_INSPECTION, "安全检查发现隐患自动生成工单");
  }

  @Transactional
  public void createOrUpdateFromQuickShot(Long sourceRecordId, Long operatorId) {
    ThreeCheckRecord record = recordMapper.selectById(sourceRecordId);
    if (record == null || Integer.valueOf(1).equals(record.deleted)) {
      throw new BusinessException("随手拍记录不存在");
    }
    if (!"quick-shot".equals(record.moduleKey)) {
      return;
    }
    Map<String, Object> payload = readPayload(record.payloadJson);
    if (text(payload.get("hazardDescription")) == null) {
      return;
    }
    HazardRectificationOrder order = existingSourceOrder(SOURCE_QUICK_SHOT, record);
    if (order == null) {
      order = createOrder(record, 1, operatorId, SOURCE_QUICK_SHOT);
      itemMapper.insert(toQuickShotItem(order, record, payload));
      writeFlowLog(
          order.id,
          null,
          order.status,
          "CREATE",
          "自动生成",
          operatorId,
          "随手拍审核通过自动生成工单",
          null);
    }
    backfillQuickShotPayload(payload, order, null, null);
    record.payloadJson = writePayload(payload);
    concurrentUpdater.update(record);
  }

  @Transactional
  public void cancelFromQuickShotWithdraw(Long sourceRecordId, Long operatorId, String remark) {
    ThreeCheckRecord record = recordMapper.selectById(sourceRecordId);
    if (record == null || Integer.valueOf(1).equals(record.deleted)) {
      throw new BusinessException("随手拍记录不存在");
    }
    if (!"quick-shot".equals(record.moduleKey)) {
      return;
    }
    HazardRectificationOrder order = existingSourceOrder(SOURCE_QUICK_SHOT, record);
    if (order == null || HazardRectificationOrderStatus.CANCELLED.equals(order.status)) {
      return;
    }
    String fromStatus = order.status;
    String toStatus = requireCancelTransition(fromStatus);
    order.status = toStatus;
    order.updatedBy = operatorId;
    order.updatedAt = LocalDateTime.now();
    orderConcurrentUpdater.update(order, fromStatus);
    syncItemStatus(order);
    writeFlowLog(
        order.id,
        fromStatus,
        toStatus,
        "CANCEL",
        "作废",
        operatorId,
        isBlank(remark) ? "随手拍撤回自动作废" : remark,
        writePayload(Map.of("cancelReason", isBlank(remark) ? "随手拍撤回自动作废" : remark)));
    backfillSourceRecord(order, "作废", isBlank(remark) ? "随手拍撤回自动作废" : remark);
  }

  private HazardRectificationOrder createOrUpdateFromRecord(
      ThreeCheckRecord record, Long operatorId, String sourceType, String createRemark) {
    Map<String, Object> payload = readPayload(record.payloadJson);
    HazardSourceCollection sourceItems = normalizedHazardSourceCollection(payload, sourceType);
    List<HazardSourceLine> hazardLines = hazardLines(record, sourceItems.items());
    if (hazardLines.isEmpty()) {
      return null;
    }

    HazardRectificationOrder order = existingSourceOrder(sourceType, record);
    if (order == null) {
      order = createOrder(record, hazardLines.size(), operatorId, sourceType);
      writeFlowLog(order.id, null, order.status, "CREATE", "自动生成", operatorId, createRemark, null);
    } else if (!HazardRectificationOrderStatus.PENDING_ASSIGN.equals(order.status)) {
      backfillCheckItems(sourceItems.items(), hazardLines, order);
      payload.put(sourceItems.payloadKey(), sourceItems.items());
      record.payloadJson = writePayload(payload);
      concurrentUpdater.update(record);
      return order;
    } else {
      order.hazardCount = hazardLines.size();
      order.updatedBy = operatorId;
      order.updatedAt = LocalDateTime.now();
      orderConcurrentUpdater.update(order, order.status);
      backfillCheckItems(sourceItems.items(), hazardLines, order);
      payload.put(sourceItems.payloadKey(), sourceItems.items());
      record.payloadJson = writePayload(payload);
      concurrentUpdater.update(record);
      return order;
    }

    for (HazardSourceLine hazardLine : hazardLines) {
      itemMapper.insert(toItem(order, record, hazardLine));
    }
    backfillCheckItems(sourceItems.items(), hazardLines, order);
    payload.put(sourceItems.payloadKey(), sourceItems.items());
    record.payloadJson = writePayload(payload);
    concurrentUpdater.update(record);
    return order;
  }

  private HazardRectificationOrder createOrder(
      ThreeCheckRecord record, int hazardCount, Long operatorId, String sourceType) {
    LocalDateTime now = LocalDateTime.now();
    HazardRectificationOrder order = new HazardRectificationOrder();
    order.orderNo = nextOrderNo();
    order.sourceType = sourceType;
    order.sourceModuleKey = record.moduleKey;
    order.sourceRecordId = record.id;
    order.sourceRecordNo = record.recordNo;
    order.rootDispatchRecordId = record.rootDispatchRecordId;
    order.companyId = record.companyId;
    order.departmentId = record.departmentId;
    order.teamId = record.teamId;
    order.businessDate = record.businessDate;
    order.hazardCount = hazardCount;
    order.status = HazardRectificationOrderStatus.PENDING_ASSIGN;
    order.version = 0;
    order.createdBy = operatorId;
    order.updatedBy = operatorId;
    order.createdAt = now;
    order.updatedAt = now;
    order.deleted = 0;
    orderMapper.insert(order);
    return order;
  }

  private HazardRectificationOrderItem toItem(
      HazardRectificationOrder order, ThreeCheckRecord record, HazardSourceLine line) {
    LocalDateTime now = LocalDateTime.now();
    HazardRectificationOrderItem item = new HazardRectificationOrderItem();
    item.orderId = order.id;
    item.sourceLineId = line.sourceLineId();
    item.sourceLineIndex = line.index();
    item.libraryItemId = longValue(line.item().get("libraryItemId"));
    item.riskType = text(line.item().get("riskType"));
    item.checkItem = Objects.requireNonNullElse(text(line.item().get("checkItem")), "");
    item.hazardDescription = firstText(line.item(), "hazardDescription", "rectificationDescription");
    item.beforePhoto =
        sourceMediaValue(
            record,
            line.item(),
            "IMAGE",
            "beforePhoto",
            "beforeRectificationPhoto",
            "rectificationBeforePhoto",
            "photo",
            "image",
            "imageUpload");
    item.beforeVideo = sourceMediaValue(record, line.item(), "VIDEO", "beforeVideo", "video", "videoUpload");
    item.defaultFollowUpPlan = text(line.item().get("defaultFollowUpPlan"));
    item.sourceSnapshotJson = writePayload(line.item());
    item.rectificationStatus = order.status;
    item.sortOrder = integerValue(line.item().get("sortOrder"), line.index());
    item.createdAt = now;
    item.updatedAt = now;
    item.deleted = 0;
    return item;
  }

  private HazardRectificationOrderItem toManualItem(
      HazardRectificationOrder order,
      HazardRectificationOrderCreateItemRequest request,
      int index) {
    String checkItem = text(request == null ? null : request.checkItem());
    if (checkItem == null) {
      throw new BusinessException("检查项不能为空");
    }
    String hazardDescription = text(request.hazardDescription());
    if (hazardDescription == null) {
      throw new BusinessException("隐患描述不能为空");
    }
    LocalDateTime now = LocalDateTime.now();
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("riskType", text(request.riskType()));
    snapshot.put("checkItem", checkItem);
    snapshot.put("hazardDescription", hazardDescription);
    snapshot.put("beforePhoto", text(request.beforePhoto()));
    snapshot.put("beforeVideo", text(request.beforeVideo()));
    snapshot.put("defaultFollowUpPlan", text(request.defaultFollowUpPlan()));

    HazardRectificationOrderItem item = new HazardRectificationOrderItem();
    item.orderId = order.id;
    item.sourceLineId = SOURCE_MODULE_MANUAL + "-" + order.id + "-" + (index + 1);
    item.sourceLineIndex = index;
    item.riskType = text(request.riskType());
    item.checkItem = checkItem;
    item.hazardDescription = hazardDescription;
    item.beforePhoto = text(request.beforePhoto());
    item.beforeVideo = text(request.beforeVideo());
    item.defaultFollowUpPlan = text(request.defaultFollowUpPlan());
    item.sourceSnapshotJson = writePayload(snapshot);
    item.rectificationStatus = order.status;
    item.sortOrder = index;
    item.createdAt = now;
    item.updatedAt = now;
    item.deleted = 0;
    return item;
  }

  private HazardRectificationOrderItem toQuickShotItem(
      HazardRectificationOrder order, ThreeCheckRecord record, Map<String, Object> payload) {
    LocalDateTime now = LocalDateTime.now();
    String hazardDescription = Objects.requireNonNullElse(text(payload.get("hazardDescription")), "随手拍隐患");
    HazardRectificationOrderItem item = new HazardRectificationOrderItem();
    item.orderId = order.id;
    item.sourceLineId = "quick-shot-" + record.id;
    item.sourceLineIndex = 0;
    item.riskType = text(payload.get("riskType"));
    item.checkItem = Objects.requireNonNullElse(firstText(payload, "checkItem", "hazardTitle"), "随手拍隐患");
    item.hazardDescription = hazardDescription;
    String hazardMedia = firstText(payload, "hazardImage", "hazardMedia", "media");
    String attachmentImage = firstSourceAttachmentUrl(record, "IMAGE");
    String attachmentVideo = firstSourceAttachmentUrl(record, "VIDEO");
    item.beforePhoto =
        !isBlank(attachmentImage)
            ? attachmentImage
            : isVideoMedia(hazardMedia)
            ? firstText(payload, "beforePhoto", "photo", "image", "imageUpload")
            : firstText(payload, "beforePhoto", "photo", "image", "imageUpload", "hazardImage");
    item.beforeVideo =
        !isBlank(attachmentVideo)
            ? attachmentVideo
            : isVideoMedia(hazardMedia)
            ? Objects.requireNonNullElse(hazardMedia, firstText(payload, "beforeVideo", "video", "videoUpload"))
            : firstText(payload, "beforeVideo", "video", "videoUpload");
    item.defaultFollowUpPlan = text(payload.get("defaultFollowUpPlan"));
    item.sourceSnapshotJson = writePayload(payload);
    item.rectificationStatus = order.status;
    item.sortOrder = 0;
    item.createdAt = now;
    item.updatedAt = now;
    item.deleted = 0;
    return item;
  }

  private boolean isVideoMedia(String value) {
    if (value == null) {
      return false;
    }
    String lower = value.toLowerCase(Locale.ROOT);
    return lower.endsWith(".mp4")
        || lower.endsWith(".mov")
        || lower.endsWith(".avi")
        || lower.endsWith(".webm")
        || lower.endsWith(".mkv")
        || lower.contains("video/");
  }

  private String sourceMediaValue(
      ThreeCheckRecord record, Map<String, Object> payload, String fileKind, String... keys) {
    String value = firstText(payload, keys);
    String attachmentUrl = firstSourceAttachmentUrl(record, fileKind);
    if (!isBlank(attachmentUrl) && !looksLikeMediaUrl(value)) {
      return attachmentUrl;
    }
    return value;
  }

  private String responseMediaValue(
      HazardRectificationOrder order,
      Map<String, Object> sourceSnapshot,
      String fileKind,
      String itemValue,
      String... snapshotKeys) {
    if (looksLikeMediaUrl(itemValue)) {
      return itemValue;
    }
    String snapshotValue = firstText(sourceSnapshot, snapshotKeys);
    if (looksLikeMediaUrl(snapshotValue) && mediaMatchesKind(snapshotValue, fileKind)) {
      return snapshotValue;
    }
    String attachmentUrl = firstSourceAttachmentUrl(order, fileKind);
    if (!isBlank(attachmentUrl)) {
      return attachmentUrl;
    }
    return itemValue;
  }

  private String firstSourceAttachmentUrl(ThreeCheckRecord record, String fileKind) {
    if (record == null || record.id == null || isBlank(record.moduleKey)) {
      return null;
    }
    BizAttachment attachment =
        attachmentMapper.selectOne(
            new QueryWrapper<BizAttachment>()
                .eq("biz_type", sourceBizType(record.moduleKey))
                .eq("biz_id", record.id)
                .eq("file_kind", fileKind)
                .eq("deleted", 0)
                .orderByDesc("uploaded_at")
                .last("limit 1"));
    return attachment == null ? null : attachmentUrlResolver.url(attachment);
  }

  private String firstSourceAttachmentUrl(HazardRectificationOrder order, String fileKind) {
    if (order == null || order.sourceRecordId == null || isBlank(order.sourceModuleKey)) {
      return null;
    }
    BizAttachment attachment =
        attachmentMapper.selectOne(
            new QueryWrapper<BizAttachment>()
                .eq("biz_type", sourceBizType(order.sourceModuleKey))
                .eq("biz_id", order.sourceRecordId)
                .eq("file_kind", fileKind)
                .eq("deleted", 0)
                .orderByDesc("uploaded_at")
                .last("limit 1"));
    return attachment == null ? null : attachmentUrlResolver.url(attachment);
  }

  private String sourceBizType(String moduleKey) {
    return switch (moduleKey) {
      case "safety-check" -> ThreeCheckBizType.HAZARD_SAFETY_CHECK;
      case "quick-shot" -> ThreeCheckBizType.HAZARD_QUICK_SHOT;
      case "pre-shift-inspection" -> ThreeCheckBizType.THREE_CHECK_PRE_SHIFT_INSPECTION;
      case "mid-shift-inspection" -> ThreeCheckBizType.THREE_CHECK_MID_SHIFT_INSPECTION;
      case "post-shift-inspection" -> ThreeCheckBizType.THREE_CHECK_POST_SHIFT_INSPECTION;
      case "key-sites" -> ThreeCheckBizType.THREE_CHECK_KEY_SITES;
      default -> "THREE_CHECK_" + moduleKey.toUpperCase(Locale.ROOT).replace("-", "_");
    };
  }

  private boolean looksLikeMediaUrl(String value) {
    if (isBlank(value)) {
      return false;
    }
    String lower = value.toLowerCase(Locale.ROOT);
    return lower.startsWith("/")
        || lower.startsWith("http://")
        || lower.startsWith("https://")
        || lower.startsWith("data:");
  }

  private boolean mediaMatchesKind(String value, String fileKind) {
    if (isBlank(value)) {
      return false;
    }
    return "VIDEO".equals(fileKind) ? isVideoMedia(value) : !isVideoMedia(value);
  }

  private void backfillCheckItems(
      List<Map<String, Object>> checkItems, List<HazardSourceLine> hazardLines, HazardRectificationOrder order) {
    Map<Integer, HazardSourceLine> byIndex =
        hazardLines.stream().collect(Collectors.toMap(HazardSourceLine::index, Function.identity()));
    for (int index = 0; index < checkItems.size(); index++) {
      HazardSourceLine line = byIndex.get(index);
      if (line == null) {
        continue;
      }
      Map<String, Object> item = checkItems.get(index);
      item.put("lineId", line.sourceLineId());
      item.put("rectificationOrderId", String.valueOf(order.id));
      item.put("rectificationOrderNo", order.orderNo);
      item.put("rectificationStatus", order.status);
      item.put("rectificationStatusLabel", HazardRectificationOrderStatus.label(order.status));
      if (order.closedAt != null) {
        item.put("rectificationClosedAt", formatDateTime(order.closedAt));
      }
    }
  }

  private List<HazardSourceLine> hazardLines(
      ThreeCheckRecord record, List<Map<String, Object>> checkItems) {
    List<HazardSourceLine> lines = new ArrayList<>();
    for (int index = 0; index < checkItems.size(); index++) {
      Map<String, Object> item = checkItems.get(index);
      if (!"有隐患".equals(text(item.get("checkResult")))) {
        continue;
      }
      String sourceLineId = stableLineId(record, item, index);
      lines.add(new HazardSourceLine(index, sourceLineId, item));
    }
    return lines;
  }

  private String stableLineId(ThreeCheckRecord record, Map<String, Object> item, int index) {
    String lineId = firstText(item, "lineId", "sourceLineId");
    if (lineId != null) {
      return lineId;
    }
    String libraryItemId = text(item.get("libraryItemId"));
    if (libraryItemId != null) {
      return record.moduleKey + "-" + libraryItemId;
    }
    return record.moduleKey + "-" + record.id + "-" + (index + 1);
  }

  private HazardRectificationOrder existingSourceOrder(String sourceType, ThreeCheckRecord record) {
    return orderMapper.selectOne(
        new QueryWrapper<HazardRectificationOrder>()
            .eq("source_type", sourceType)
            .eq("source_module_key", record.moduleKey)
            .eq("source_record_id", record.id)
            .eq("deleted", 0)
            .last("limit 1"));
  }

  private QueryWrapper<HazardRectificationOrder> queryWrapper(HazardRectificationOrderQuery query) {
    QueryWrapper<HazardRectificationOrder> wrapper =
        new QueryWrapper<HazardRectificationOrder>().eq("deleted", 0);
    if (!isBlank(query.sourceType()) && !"all".equalsIgnoreCase(query.sourceType())) {
      wrapper.eq("source_type", query.sourceType().trim());
    }
    if (!isBlank(query.sourceModuleKey())) {
      wrapper.eq("source_module_key", query.sourceModuleKey().trim());
    }
    if (query.rootDispatchRecordId() != null) {
      wrapper.eq("root_dispatch_record_id", query.rootDispatchRecordId());
    }
    if (query.sourceRecordId() != null) {
      wrapper.eq("source_record_id", query.sourceRecordId());
    }
    if (!isBlank(query.status()) && !"all".equalsIgnoreCase(query.status())) {
      wrapper.eq("status", query.status().trim().toUpperCase(Locale.ROOT));
    }
    if (query.companyId() != null) {
      wrapper.eq("company_id", query.companyId());
    }
    if (query.departmentId() != null) {
      wrapper.eq("department_id", query.departmentId());
    }
    if (query.teamId() != null) {
      wrapper.eq("team_id", query.teamId());
    }
    if (query.responsibleUserId() != null) {
      wrapper.eq("rectification_responsible_user_id", query.responsibleUserId());
    }
    if (query.dateStart() != null) {
      wrapper.ge("business_date", query.dateStart());
    }
    if (query.dateEnd() != null) {
      wrapper.le("business_date", query.dateEnd());
    }
    return wrapper;
  }

  private HazardRectificationOrder requireOrder(Long id) {
    HazardRectificationOrder order = orderMapper.selectById(id);
    if (order == null || Integer.valueOf(1).equals(order.deleted)) {
      throw new BusinessException("隐患整改工单不存在");
    }
    return order;
  }

  private HazardRectificationOrderListItem toListItem(
      HazardRectificationOrder order, Map<Long, SysOrg> orgs) {
    return new HazardRectificationOrderListItem(
        String.valueOf(order.id),
        order.orderNo,
        order.sourceType,
        order.sourceModuleKey,
        order.sourceRecordId == null ? null : String.valueOf(order.sourceRecordId),
        order.sourceRecordNo,
        order.rootDispatchRecordId == null ? null : String.valueOf(order.rootDispatchRecordId),
        order.companyId,
        orgName(orgs, order.companyId),
        order.departmentId,
        orgName(orgs, order.departmentId),
        order.teamId,
        orgName(orgs, order.teamId),
        String.valueOf(order.businessDate),
        order.hazardCount == null ? 0 : order.hazardCount,
        order.status,
        HazardRectificationOrderStatus.label(order.status),
        formatDateTime(order.rectificationDeadline),
        formatDateTime(order.issuedAt),
        formatDateTime(order.rectifiedAt),
        formatDateTime(order.closedAt),
        order.version == null ? 0 : order.version);
  }

  private HazardRectificationOrderDetailResponse toDetail(HazardRectificationOrder order) {
    Map<Long, SysOrg> orgs = orgMap();
    List<HazardRectificationOrderItemResponse> items =
        itemMapper
            .selectList(
                new QueryWrapper<HazardRectificationOrderItem>()
                    .eq("order_id", order.id)
            .eq("deleted", 0)
            .orderByAsc("sort_order", "id"))
            .stream()
            .map(item -> toItemResponse(order, item))
            .toList();
    Map<Long, SysUser> users = userMap();
    List<HazardRectificationFlowLogResponse> logs =
        flowLogMapper
            .selectList(
                new QueryWrapper<HazardRectificationFlowLog>()
                    .eq("order_id", order.id)
                    .orderByAsc("created_at", "id"))
            .stream()
            .map(log -> toFlowLogResponse(log, users))
            .toList();
    return new HazardRectificationOrderDetailResponse(
        String.valueOf(order.id),
        order.orderNo,
        order.sourceType,
        order.sourceModuleKey,
        order.sourceRecordId == null ? null : String.valueOf(order.sourceRecordId),
        order.sourceRecordNo,
        order.rootDispatchRecordId == null ? null : String.valueOf(order.rootDispatchRecordId),
        order.companyId,
        orgName(orgs, order.companyId),
        order.departmentId,
        orgName(orgs, order.departmentId),
        order.teamId,
        orgName(orgs, order.teamId),
        String.valueOf(order.businessDate),
        order.hazardCount == null ? 0 : order.hazardCount,
        order.status,
        HazardRectificationOrderStatus.label(order.status),
        order.rectificationDepartmentId,
        order.rectificationResponsibleUserId,
        order.rectificationRequirement,
        formatDateTime(order.rectificationDeadline),
        order.issuedBy,
        formatDateTime(order.issuedAt),
        order.rectifiedBy,
        formatDateTime(order.rectifiedAt),
        order.rectificationDescription,
        order.rectificationAfterPhoto,
        order.acceptanceUserId,
        order.acceptanceDepartmentId,
        formatDateTime(order.acceptanceAt),
        order.acceptanceResult,
        order.acceptanceRemark,
        formatDateTime(order.closedAt),
        order.version == null ? 0 : order.version,
        items,
        logs);
  }

  private HazardRectificationOrderItemResponse toItemResponse(
      HazardRectificationOrder order, HazardRectificationOrderItem item) {
    Map<String, Object> sourceSnapshot = readOptionalPayload(item.sourceSnapshotJson);
    return new HazardRectificationOrderItemResponse(
        String.valueOf(item.id),
        item.sourceLineId,
        item.sourceLineIndex == null ? 0 : item.sourceLineIndex,
        item.libraryItemId == null ? null : String.valueOf(item.libraryItemId),
        item.riskType,
        item.checkItem,
        item.hazardDescription,
        text(sourceSnapshot.get("aiEnabled")),
        responseMediaValue(
            order,
            sourceSnapshot,
            "IMAGE",
            item.beforePhoto,
            "beforePhoto",
            "beforeRectificationPhoto",
            "rectificationBeforePhoto",
            "photo",
            "image",
            "imageUpload",
            "hazardImage"),
        responseMediaValue(
            order,
            sourceSnapshot,
            "VIDEO",
            item.beforeVideo,
            "beforeVideo",
            "video",
            "videoUpload",
            "hazardImage"),
        item.defaultFollowUpPlan,
        item.rectificationStatus,
        HazardRectificationOrderStatus.label(item.rectificationStatus),
        formatDateTime(item.closedAt),
        item.sortOrder == null ? 0 : item.sortOrder,
        sourceSnapshot);
  }

  private HazardRectificationFlowLogResponse toFlowLogResponse(
      HazardRectificationFlowLog log, Map<Long, SysUser> users) {
    SysUser operator = log.operatorId == null ? null : users.get(log.operatorId);
    return new HazardRectificationFlowLogResponse(
        String.valueOf(log.id),
        log.fromStatus,
        log.fromStatus == null ? null : HazardRectificationOrderStatus.label(log.fromStatus),
        log.toStatus,
        HazardRectificationOrderStatus.label(log.toStatus),
        log.action,
        log.actionLabel,
        log.operatorId == null ? null : String.valueOf(log.operatorId),
        operator == null ? null : operator.realName,
        log.remark,
        readOptionalPayload(log.payloadJson),
        formatDateTime(log.createdAt));
  }

  private void writeFlowLog(
      Long orderId,
      String fromStatus,
      String toStatus,
      String action,
      String actionLabel,
      Long operatorId,
      String remark,
      String payloadJson) {
    HazardRectificationFlowLog log = new HazardRectificationFlowLog();
    log.orderId = orderId;
    log.fromStatus = fromStatus;
    log.toStatus = toStatus;
    log.action = action;
    log.actionLabel = actionLabel;
    log.operatorId = operatorId;
    log.remark = remark;
    log.payloadJson = payloadJson;
    log.createdAt = LocalDateTime.now();
    flowLogMapper.insert(log);
  }

  private void applyActionPayload(
      HazardRectificationOrder order, String action, Map<String, Object> payload, CurrentUser user) {
    switch (action) {
      case "ISSUE_RECTIFICATION" -> {
        Long responsibleUserId =
            requiredLong(payload, "rectificationResponsibleUserId", "整改责任人");
        String requirement = requiredText(payload, "rectificationRequirement", "整改要求");
        LocalDateTime deadline = requiredDateTime(payload, "rectificationDeadline", "整改期限");
        Long acceptanceUserId = longValue(payload.get("acceptanceUserId"));
        if (acceptanceUserId != null && acceptanceUserId.equals(responsibleUserId)) {
          throw new BusinessException("整改责任人和验收人不允许同一人");
        }
        order.rectificationResponsibleUserId = responsibleUserId;
        order.rectificationDepartmentId = longValue(payload.get("rectificationDepartmentId"));
        order.rectificationRequirement = requirement;
        order.rectificationDeadline = deadline;
        order.acceptanceUserId = acceptanceUserId;
        order.acceptanceDepartmentId = longValue(payload.get("acceptanceDepartmentId"));
        order.issuedBy = user.userId();
        order.issuedAt = LocalDateTime.now();
      }
      case "MARK_RECTIFIED" -> {
        order.rectificationDescription =
            requiredText(payload, "rectificationDescription", "整改说明");
        order.rectificationAfterPhoto =
            requiredText(payload, "afterPhoto", "整改后照片");
        order.rectifiedBy = user.userId();
        order.rectifiedAt = LocalDateTime.now();
      }
      case "REQUEST_ACCEPTANCE" -> {
        Long acceptanceUserId =
            longValue(payload.get("acceptanceUserId")) == null
                ? order.acceptanceUserId
                : longValue(payload.get("acceptanceUserId"));
        Long acceptanceDepartmentId = longValue(payload.get("acceptanceDepartmentId"));
        if (acceptanceDepartmentId != null) {
          order.acceptanceDepartmentId = acceptanceDepartmentId;
        }
        if (acceptanceUserId == null && order.acceptanceDepartmentId == null) {
          throw new BusinessException("验收人或验收部门不能为空");
        }
        validateAcceptanceUser(order, acceptanceUserId);
        order.acceptanceUserId = acceptanceUserId;
      }
      case "ACCEPT" -> {
        Long selectedAcceptanceUserId = longValue(payload.get("acceptanceUserId"));
        if (selectedAcceptanceUserId == null) {
          validateAcceptanceOperator(order, user.userId());
        } else {
          validateAcceptanceUser(order, selectedAcceptanceUserId);
          order.acceptanceUserId = selectedAcceptanceUserId;
        }
        if (order.acceptanceUserId == null && order.acceptanceDepartmentId == null) {
          throw new BusinessException("验收人或验收部门不能为空");
        }
        order.acceptanceAt = LocalDateTime.now();
        order.acceptanceResult = "PASS";
        order.acceptanceRemark = firstText(payload, "acceptanceRemark", "remark");
      }
      case "REJECT_ACCEPTANCE" -> {
        Long selectedAcceptanceUserId = longValue(payload.get("acceptanceUserId"));
        if (selectedAcceptanceUserId == null) {
          validateAcceptanceOperator(order, user.userId());
        } else {
          validateAcceptanceUser(order, selectedAcceptanceUserId);
          order.acceptanceUserId = selectedAcceptanceUserId;
        }
        if (order.acceptanceUserId == null && order.acceptanceDepartmentId == null) {
          throw new BusinessException("验收人或验收部门不能为空");
        }
        order.acceptanceAt = LocalDateTime.now();
        order.acceptanceResult = "REJECT";
        order.acceptanceRemark = requiredText(payload, "rejectReason", "驳回原因");
        order.closedAt = null;
      }
      case "CANCEL" -> {
        requiredText(payload, "cancelReason", "作废原因");
        order.closedAt = null;
      }
      default -> throw new BusinessException("隐患整改工单动作不支持：" + action);
    }
  }

  private void syncItemStatus(HazardRectificationOrder order) {
    List<HazardRectificationOrderItem> items =
        itemMapper.selectList(
            new QueryWrapper<HazardRectificationOrderItem>()
                .eq("order_id", order.id)
                .eq("deleted", 0));
    for (HazardRectificationOrderItem item : items) {
      item.rectificationStatus = order.status;
      item.closedAt = HazardRectificationOrderStatus.CLOSED.equals(order.status) ? order.closedAt : null;
      item.updatedAt = LocalDateTime.now();
      itemMapper.updateById(item);
    }
  }

  private void backfillSourceRecord(
      HazardRectificationOrder order, String actionLabel, String remark) {
    if (!SOURCE_THREE_CHECK.equals(order.sourceType)
        && !SOURCE_SAFETY_INSPECTION.equals(order.sourceType)
        && !SOURCE_QUICK_SHOT.equals(order.sourceType)) {
      return;
    }
    ThreeCheckRecord record = recordMapper.selectById(order.sourceRecordId);
    if (record == null || Integer.valueOf(1).equals(record.deleted)) {
      return;
    }
    Map<String, Object> payload = readPayload(record.payloadJson);
    if (SOURCE_QUICK_SHOT.equals(order.sourceType)) {
      backfillQuickShotPayload(payload, order, actionLabel, remark);
      record.payloadJson = writePayload(payload);
      concurrentUpdater.update(record);
      return;
    }
    List<HazardSourceCollection> sourceCollections =
        normalizedHazardSourceCollections(payload, order.sourceType);
    boolean changed = false;
    for (HazardSourceCollection sourceCollection : sourceCollections) {
      boolean collectionChanged = false;
      for (Map<String, Object> item : sourceCollection.items()) {
        if (!String.valueOf(order.id).equals(text(item.get("rectificationOrderId")))) {
          continue;
        }
        item.put("rectificationStatus", order.status);
        item.put("rectificationStatusLabel", HazardRectificationOrderStatus.label(order.status));
        if (HazardRectificationOrderStatus.CLOSED.equals(order.status)) {
          item.put("rectificationClosedAt", formatDateTime(order.closedAt));
        } else {
          item.remove("rectificationClosedAt");
        }
        item.put("lastRectificationAction", actionLabel);
        item.put("lastRectificationRemark", remark);
        collectionChanged = true;
      }
      if (collectionChanged) {
        payload.put(sourceCollection.payloadKey(), sourceCollection.items());
        changed = true;
      }
    }
    if (!changed) {
      return;
    }
    record.payloadJson = writePayload(payload);
    concurrentUpdater.update(record);
  }

  private void backfillQuickShotPayload(
      Map<String, Object> payload,
      HazardRectificationOrder order,
      String actionLabel,
      String remark) {
    payload.put("rectificationOrderId", String.valueOf(order.id));
    payload.put("rectificationOrderNo", order.orderNo);
    payload.put("rectificationStatus", order.status);
    payload.put("rectificationStatusLabel", HazardRectificationOrderStatus.label(order.status));
    if (HazardRectificationOrderStatus.CLOSED.equals(order.status)) {
      payload.put("rectificationClosedAt", formatDateTime(order.closedAt));
    } else {
      payload.remove("rectificationClosedAt");
    }
    if (actionLabel != null) {
      payload.put("lastRectificationAction", actionLabel);
    }
    if (remark != null) {
      payload.put("lastRectificationRemark", remark);
    }
  }

  private String normalizeAction(String action) {
    if (isBlank(action)) {
      throw new BusinessException("隐患整改工单动作不能为空");
    }
    String normalized = action.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "ISSUE_RECTIFICATION", "MARK_RECTIFIED", "REQUEST_ACCEPTANCE", "ACCEPT", "REJECT_ACCEPTANCE", "CANCEL" ->
          normalized;
      default -> throw new BusinessException("隐患整改工单动作不支持：" + action);
    };
  }

  private String nextStatus(String currentStatus, String action) {
    return switch (action) {
      case "ISSUE_RECTIFICATION" ->
          requireTransition(
              currentStatus,
              HazardRectificationOrderStatus.PENDING_ASSIGN,
              HazardRectificationOrderStatus.PENDING_RECTIFY,
              action);
      case "MARK_RECTIFIED" ->
          requireTransition(
              currentStatus,
              HazardRectificationOrderStatus.PENDING_RECTIFY,
              HazardRectificationOrderStatus.RECTIFIED,
              action);
      case "REQUEST_ACCEPTANCE" ->
          requireTransition(
              currentStatus,
              HazardRectificationOrderStatus.RECTIFIED,
              HazardRectificationOrderStatus.PENDING_ACCEPTANCE,
              action);
      case "ACCEPT" ->
          requireTransition(
              currentStatus,
              HazardRectificationOrderStatus.PENDING_ACCEPTANCE,
              HazardRectificationOrderStatus.CLOSED,
              action);
      case "REJECT_ACCEPTANCE" ->
          requireTransition(
              currentStatus,
              HazardRectificationOrderStatus.PENDING_ACCEPTANCE,
              HazardRectificationOrderStatus.PENDING_RECTIFY,
              action);
      case "CANCEL" -> requireCancelTransition(currentStatus);
      default -> throw new BusinessException("隐患整改工单动作不支持：" + action);
    };
  }

  private String requireTransition(
      String currentStatus, String expectedStatus, String nextStatus, String action) {
    if (!expectedStatus.equals(currentStatus)) {
      throw new BusinessException(
          "隐患整改工单动作"
              + actionLabel(action)
              + "不适用于当前状态："
              + HazardRectificationOrderStatus.label(currentStatus));
    }
    return nextStatus;
  }

  private String requireCancelTransition(String currentStatus) {
    if (HazardRectificationOrderStatus.PENDING_ASSIGN.equals(currentStatus)
        || HazardRectificationOrderStatus.PENDING_RECTIFY.equals(currentStatus)
        || HazardRectificationOrderStatus.RECTIFIED.equals(currentStatus)
        || HazardRectificationOrderStatus.PENDING_ACCEPTANCE.equals(currentStatus)) {
      return HazardRectificationOrderStatus.CANCELLED;
    }
    throw new BusinessException(
        "隐患整改工单动作作废不适用于当前状态：" + HazardRectificationOrderStatus.label(currentStatus));
  }

  private String actionLabel(String action) {
    return switch (action) {
      case "ISSUE_RECTIFICATION" -> "下发整改";
      case "MARK_RECTIFIED" -> "整改完成";
      case "REQUEST_ACCEPTANCE" -> "提交验收";
      case "ACCEPT" -> "验收通过";
      case "REJECT_ACCEPTANCE" -> "验收驳回";
      case "CANCEL" -> "作废";
      default -> action;
    };
  }

  private String actionRemark(String action, String requestRemark, Map<String, Object> payload) {
    if (!isBlank(requestRemark)) {
      return requestRemark.trim();
    }
    if ("REJECT_ACCEPTANCE".equals(action)) {
      return requiredText(payload, "rejectReason", "驳回原因");
    }
    if ("CANCEL".equals(action)) {
      return requiredText(payload, "cancelReason", "作废原因");
    }
    String payloadRemark = firstText(payload, "acceptanceRemark", "rectificationDescription", "remark");
    return payloadRemark == null ? actionLabel(action) : payloadRemark;
  }

  private void validateAcceptanceUser(HazardRectificationOrder order, Long acceptanceUserId) {
    if (acceptanceUserId == null) {
      return;
    }
    if (acceptanceUserId.equals(order.rectificationResponsibleUserId)) {
      throw new BusinessException("所选验收人不能为整改责任人");
    }
    if (acceptanceUserId.equals(order.issuedBy)) {
      throw new BusinessException("所选验收人不能为下发人");
    }
  }

  private void validateAcceptanceOperator(HazardRectificationOrder order, Long operatorId) {
    if (operatorId.equals(order.rectificationResponsibleUserId)) {
      throw new BusinessException("未填写验收人ID，整改责任人不能验收自己的工单");
    }
    if (operatorId.equals(order.issuedBy)) {
      throw new BusinessException("未填写验收人ID，下发人不能验收自己下发的工单");
    }
    if (order.acceptanceUserId != null && !operatorId.equals(order.acceptanceUserId)) {
      throw new BusinessException("当前用户不是该工单验收人");
    }
  }

  private void requireVersionMatch(HazardRectificationOrder order, Integer requestVersion) {
    if (requestVersion == null) {
      throw new ConflictException("缺少工单版本，请刷新后重试");
    }
    if (!requestVersion.equals(order.version)) {
      throw new ConflictException("工单已被其他端更新，请刷新后重试");
    }
  }

  private String requiredText(Map<String, Object> payload, String key, String label) {
    String value = text(payload.get(key));
    if (value == null) {
      throw new BusinessException(label + "不能为空");
    }
    return value;
  }

  private Long requiredLong(Map<String, Object> payload, String key, String label) {
    Long value = longValue(payload.get(key));
    if (value == null) {
      throw new BusinessException(label + "不能为空");
    }
    return value;
  }

  private <T> T requireValue(T value, String label) {
    if (value == null) {
      throw new BusinessException(label + "不能为空");
    }
    return value;
  }

  private LocalDateTime requiredDateTime(Map<String, Object> payload, String key, String label) {
    String value = requiredText(payload, key, label);
    try {
      return LocalDateTime.parse(value.replace(' ', 'T'));
    } catch (RuntimeException exception) {
      throw new BusinessException(label + "格式不正确");
    }
  }

  private String nextOrderNo() {
    String datePart = java.time.LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    String prefix = "HR-" + datePart + "-";
    long count =
        orderMapper.selectCount(
            new QueryWrapper<HazardRectificationOrder>()
                .likeRight("order_no", prefix)
                .eq("deleted", 0));
    return prefix + String.format("%04d", count + 1);
  }

  private List<Map<String, Object>> normalizedCheckItems(Map<String, Object> payload) {
    return normalizedPayloadItems(payload, "checkItems");
  }

  private HazardSourceCollection normalizedHazardSourceCollection(
      Map<String, Object> payload, String sourceType) {
    List<HazardSourceCollection> collections = normalizedHazardSourceCollections(payload, sourceType);
    if (collections.isEmpty()) {
      return new HazardSourceCollection("checkItems", List.of());
    }
    return collections.get(0);
  }

  private List<HazardSourceCollection> normalizedHazardSourceCollections(
      Map<String, Object> payload, String sourceType) {
    List<HazardSourceCollection> collections = new ArrayList<>();
    List<Map<String, Object>> hazardLines = normalizedPayloadItems(payload, "hazardLines");
    List<Map<String, Object>> checkItems = normalizedCheckItems(payload);
    if (SOURCE_SAFETY_INSPECTION.equals(sourceType) && !hazardLines.isEmpty()) {
      collections.add(new HazardSourceCollection("hazardLines", hazardLines));
      if (!checkItems.isEmpty()) {
        collections.add(new HazardSourceCollection("checkItems", checkItems));
      }
      return collections;
    }
    if (!checkItems.isEmpty()) {
      collections.add(new HazardSourceCollection("checkItems", checkItems));
    }
    if (!hazardLines.isEmpty()) {
      collections.add(new HazardSourceCollection("hazardLines", hazardLines));
    }
    return collections;
  }

  private List<Map<String, Object>> normalizedPayloadItems(Map<String, Object> payload, String payloadKey) {
    Object rawItems = payload.get(payloadKey);
    if (!(rawItems instanceof List<?> items)) {
      return List.of();
    }
    List<Map<String, Object>> normalized = new ArrayList<>();
    for (Object rawItem : items) {
      if (!(rawItem instanceof Map<?, ?> source)) {
        continue;
      }
      Map<String, Object> item = new LinkedHashMap<>();
      source.forEach((key, value) -> item.put(String.valueOf(key), value));
      normalized.add(item);
    }
    return normalized;
  }

  private Map<Long, SysOrg> orgMap() {
    return orgMapper.selectList(new QueryWrapper<SysOrg>().eq("deleted", 0)).stream()
        .collect(Collectors.toMap(org -> org.id, Function.identity(), (left, right) -> left));
  }

  private Map<Long, SysUser> userMap() {
    return userMapper.selectList(new QueryWrapper<SysUser>().eq("deleted", 0)).stream()
        .collect(Collectors.toMap(user -> user.id, Function.identity(), (left, right) -> left));
  }

  private String orgName(Map<Long, SysOrg> orgs, Long id) {
    SysOrg org = id == null ? null : orgs.get(id);
    return org == null ? null : org.orgName;
  }

  private Map<String, Object> readPayload(String payloadJson) {
    if (isBlank(payloadJson)) {
      return new LinkedHashMap<>();
    }
    try {
      return objectMapper.readValue(payloadJson, PAYLOAD_TYPE);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("记录扩展字段解析失败：" + exception.getMessage());
    }
  }

  private Map<String, Object> readOptionalPayload(String payloadJson) {
    return isBlank(payloadJson) ? Map.of() : readPayload(payloadJson);
  }

  private String writePayload(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("记录扩展字段保存失败：" + exception.getMessage());
    }
  }

  private Long longValue(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value == null || String.valueOf(value).isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private Integer integerValue(Object value, int fallback) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value == null || String.valueOf(value).isBlank()) {
      return fallback;
    }
    try {
      return Integer.parseInt(String.valueOf(value));
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }

  private String firstText(Map<String, Object> payload, String... keys) {
    for (String key : keys) {
      String value = text(payload.get(key));
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private String text(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() ? null : text;
  }

  private String formatDateTime(LocalDateTime value) {
    return value == null ? null : value.format(DATE_TIME_FORMATTER);
  }

  private boolean isTeamCheckInspectionModule(String moduleKey) {
    return "pre-shift-inspection".equals(moduleKey)
        || "mid-shift-inspection".equals(moduleKey)
        || "post-shift-inspection".equals(moduleKey)
        || "key-sites".equals(moduleKey);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private record HazardSourceLine(int index, String sourceLineId, Map<String, Object> item) {}

  private record HazardSourceCollection(String payloadKey, List<Map<String, Object>> items) {}
}
