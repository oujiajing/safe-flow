package com.pingan.banzu.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.common.TrainingExamResultStatus;
import com.pingan.banzu.common.TrainingExamQuestionType;
import com.pingan.banzu.common.TrainingExamTaskStatus;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.domain.SysUser;
import com.pingan.banzu.domain.TrainingExamPaper;
import com.pingan.banzu.domain.TrainingExamQuestion;
import com.pingan.banzu.domain.TrainingExamQuestionBank;
import com.pingan.banzu.domain.TrainingExamResult;
import com.pingan.banzu.domain.TrainingExamResultQuestion;
import com.pingan.banzu.domain.TrainingExamTask;
import com.pingan.banzu.dto.MiniExamAnswerRequest;
import com.pingan.banzu.dto.MiniExamQuestionResponse;
import com.pingan.banzu.dto.MiniExamProgressRequest;
import com.pingan.banzu.dto.MiniExamResponse;
import com.pingan.banzu.dto.MiniExamSubmissionRequest;
import com.pingan.banzu.dto.TrainingExamBatchDeleteRequest;
import com.pingan.banzu.dto.TrainingExamPaperQuery;
import com.pingan.banzu.dto.TrainingExamPaperRequest;
import com.pingan.banzu.dto.TrainingExamPaperResponse;
import com.pingan.banzu.dto.TrainingExamPdfConfirmRequest;
import com.pingan.banzu.dto.TrainingExamPdfConfirmResult;
import com.pingan.banzu.dto.TrainingExamPdfPreviewResponse;
import com.pingan.banzu.dto.TrainingExamPdfQuestionDraft;
import com.pingan.banzu.dto.TrainingExamQuestionBankImportResult;
import com.pingan.banzu.dto.TrainingExamQuestionBankChild;
import com.pingan.banzu.dto.TrainingExamQuestionBankQuery;
import com.pingan.banzu.dto.TrainingExamQuestionBankRequest;
import com.pingan.banzu.dto.TrainingExamQuestionBankResponse;
import com.pingan.banzu.dto.TrainingExamQuestionImportResult;
import com.pingan.banzu.dto.TrainingExamQuestionOption;
import com.pingan.banzu.dto.TrainingExamQuestionPayload;
import com.pingan.banzu.dto.TrainingExamQuestionResponse;
import com.pingan.banzu.dto.TrainingExamResultDetailResponse;
import com.pingan.banzu.dto.TrainingExamResultQuery;
import com.pingan.banzu.dto.TrainingExamResultRequest;
import com.pingan.banzu.dto.TrainingExamResultResponse;
import com.pingan.banzu.dto.TrainingExamReviewRequest;
import com.pingan.banzu.dto.TrainingExamReviewScore;
import com.pingan.banzu.dto.TrainingExamTaskDetailResponse;
import com.pingan.banzu.dto.TrainingExamTaskQuery;
import com.pingan.banzu.dto.TrainingExamTaskRequest;
import com.pingan.banzu.dto.TrainingExamTaskResponse;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.mapper.SysUserMapper;
import com.pingan.banzu.mapper.TrainingExamPaperMapper;
import com.pingan.banzu.mapper.TrainingExamQuestionBankMapper;
import com.pingan.banzu.mapper.TrainingExamQuestionMapper;
import com.pingan.banzu.mapper.TrainingExamResultMapper;
import com.pingan.banzu.mapper.TrainingExamResultQuestionMapper;
import com.pingan.banzu.mapper.TrainingExamTaskMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.security.SystemDataScopeService;
import com.pingan.banzu.system.service.SystemExcelService.ExcelFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TrainingExamService {

  private static final List<String> QUESTION_HEADERS = List.of("题型", "考题", "选项", "全部选项", "答案", "分值", "实际分");
  private static final DateTimeFormatter CODE_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

  private final AuditLogService auditLogService;
  private final ObjectMapper objectMapper;
  private final SysOrgMapper orgMapper;
  private final SysUserMapper userMapper;
  private final SystemDataScopeService dataScopeService;
  private final TrainingPermissionPolicy permissionPolicy;
  private final TrainingExamPdfParser pdfParser;
  private final TrainingExamPaperMapper paperMapper;
  private final TrainingExamQuestionBankMapper questionBankMapper;
  private final TrainingExamQuestionMapper questionMapper;
  private final TrainingExamResultMapper resultMapper;
  private final TrainingExamResultQuestionMapper resultQuestionMapper;
  private final TrainingExamTaskMapper taskMapper;
  private final NotificationOutboxService notificationOutboxService;
  private final NotificationService notificationService;

  public TrainingExamService(
      AuditLogService auditLogService,
      ObjectMapper objectMapper,
      SysOrgMapper orgMapper,
      SysUserMapper userMapper,
      SystemDataScopeService dataScopeService,
      TrainingPermissionPolicy permissionPolicy,
      TrainingExamPdfParser pdfParser,
      TrainingExamPaperMapper paperMapper,
      TrainingExamQuestionBankMapper questionBankMapper,
      TrainingExamQuestionMapper questionMapper,
      TrainingExamResultMapper resultMapper,
      TrainingExamResultQuestionMapper resultQuestionMapper,
      TrainingExamTaskMapper taskMapper,
      NotificationOutboxService notificationOutboxService,
      NotificationService notificationService) {
    this.auditLogService = auditLogService;
    this.objectMapper = objectMapper;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
    this.dataScopeService = dataScopeService;
    this.permissionPolicy = permissionPolicy;
    this.pdfParser = pdfParser;
    this.paperMapper = paperMapper;
    this.questionBankMapper = questionBankMapper;
    this.questionMapper = questionMapper;
    this.resultMapper = resultMapper;
    this.resultQuestionMapper = resultQuestionMapper;
    this.taskMapper = taskMapper;
    this.notificationOutboxService = notificationOutboxService;
    this.notificationService = notificationService;
  }

  public PageResult<TrainingExamTaskResponse> listTasks(TrainingExamTaskQuery query) {
    permissionPolicy.assertCanViewExamTasks();
    List<TrainingExamTask> all = taskMapper.selectList(taskWrapper(query));
    int size = query == null || query.pageSize() == null ? 20 : query.pageSize();
    int page = query == null || query.page() == null ? 1 : query.page();
    int from = Math.max(0, (page - 1) * size);
    int to = Math.min(all.size(), from + size);
    if (from > to) {
      from = to;
    }
    return new PageResult<>(all.subList(from, to).stream().map(this::taskResponse).toList(), all.size());
  }

  public PageResult<MiniExamResponse> listMyExams(
      String status, Integer page, Integer pageSize) {
    CurrentUser user = CurrentUserContext.require();
    QueryWrapper<TrainingExamResult> wrapper =
        new QueryWrapper<TrainingExamResult>()
            .eq("exam_person_user_id", user.userId())
            .eq("deleted", 0)
            .orderByDesc("exam_date")
            .orderByDesc("id");
    if (!blank(status)) {
      TrainingExamResultStatus requestedStatus = TrainingExamResultStatus.parse(status);
      if (requestedStatus == TrainingExamResultStatus.EXAMED) {
        wrapper.in(
            "status",
            TrainingExamResultStatus.EXAMED.name(),
            TrainingExamResultStatus.PENDING_REVIEW.name());
      } else {
        wrapper.eq("status", requestedStatus.name());
      }
    }
    List<TrainingExamResult> all =
        resultMapper.selectList(wrapper).stream()
            .filter(
                result ->
                    TrainingExamTaskStatus.ACTIVE.name()
                        .equals(requireAssignedTask(result.taskId).status))
            .toList();
    int size = pageSize == null ? 20 : Math.max(1, Math.min(pageSize, 100));
    int pageNumber = page == null ? 1 : Math.max(1, page);
    int from = Math.min(all.size(), (pageNumber - 1) * size);
    int to = Math.min(all.size(), from + size);
    return new PageResult<>(
        all.subList(from, to).stream().map(result -> miniExam(result, false)).toList(),
        all.size());
  }

  public MiniExamResponse myExam(Long taskId) {
    if (!TrainingExamTaskStatus.ACTIVE.name().equals(requireAssignedTask(taskId).status)) {
      throw new BusinessException("考试任务未生效");
    }
    TrainingExamResult result = requireMyResult(taskId);
    if (TrainingExamResultStatus.PENDING_EXAM.name().equals(result.status)
        && result.startedAt == null) {
      LocalDateTime startedAt = LocalDateTime.now();
      TrainingExamTask task = requireAssignedTask(taskId);
      result.startedAt = startedAt;
      result.currentQuestionIndex = 0;
      result.remainingSeconds = task.durationMinutes * 60;
      result.updatedAt = startedAt;
      resultMapper.updateById(result);
    }
    return miniExam(result, true);
  }

  @Transactional
  public MiniExamResponse saveMyExamProgress(Long taskId, MiniExamProgressRequest request) {
    TrainingExamResult result = requireMyResult(taskId);
    if (!TrainingExamResultStatus.PENDING_EXAM.name().equals(result.status)) {
      return miniExam(result, true);
    }
    TrainingExamTask task = requireAssignedTask(taskId);
    LocalDateTime now = LocalDateTime.now();
    if (result.startedAt == null) {
      result.startedAt = now;
    }
    int currentIndex =
        request == null || request.currentQuestionIndex() == null
            ? (result.currentQuestionIndex == null ? 0 : result.currentQuestionIndex)
            : request.currentQuestionIndex();
    int remainingSeconds =
        request == null || request.remainingSeconds() == null
            ? (result.remainingSeconds == null ? task.durationMinutes * 60 : result.remainingSeconds)
            : request.remainingSeconds();
    result.currentQuestionIndex = Math.max(0, currentIndex);
    result.remainingSeconds = Math.max(0, remainingSeconds);
    result.updatedAt = now;
    resultMapper.updateById(result);
    saveSelectedAnswers(result, request == null ? List.of() : request.answers());
    return miniExam(requireMyResult(taskId), true);
  }

  @Transactional
  public MiniExamResponse submitMyExam(Long taskId, MiniExamSubmissionRequest request) {
    if (request == null || blank(request.requestId())) {
      throw new BusinessException("提交请求 ID 不能为空");
    }
    TrainingExamResult result = requireMyResult(taskId);
    if (!TrainingExamResultStatus.PENDING_EXAM.name().equals(result.status)) {
      if (request.requestId().trim().equals(result.submissionRequestId)) {
        return miniExam(result, true);
      }
      throw new BusinessException("该考试已提交，请勿重复交卷");
    }
    TrainingExamTask task = requireAssignedTask(taskId);
    if (!TrainingExamTaskStatus.ACTIVE.name().equals(task.status)) {
      throw new BusinessException("考试任务未生效");
    }
    List<TrainingExamResultQuestion> questions = resultQuestionEntities(result.id);
    Map<String, String> answers = answerMap(request.answers());
    int expectedAnswerCount = questions.stream().mapToInt(this::miniAnswerCount).sum();
    if (answers.size() != expectedAnswerCount
        || questions.stream().anyMatch(question -> !hasAllMiniAnswers(question, answers))) {
      throw new BusinessException("请完成全部题目后再提交");
    }

    BigDecimal total = BigDecimal.ZERO;
    Map<Long, BigDecimal> actualScores = new HashMap<>();
    Map<Long, String> selectedAnswers = new HashMap<>();
    boolean pendingReview = false;
    for (TrainingExamResultQuestion question : questions) {
      TrainingExamQuestionType type = storedQuestionType(question.questionType);
      BigDecimal actual = BigDecimal.ZERO;
      String selected;
      if (type == TrainingExamQuestionType.CASE_ANALYSIS) {
        List<TrainingExamQuestionBankChild> children = resultQuestionChildren(question);
        Map<String, String> caseAnswers = new java.util.LinkedHashMap<>();
        for (int index = 0; index < children.size(); index++) {
          TrainingExamQuestionBankChild child = children.get(index);
          String answer = answers.get(miniQuestionKey(question.id, index));
          caseAnswers.put(String.valueOf(index), answer);
          TrainingExamQuestionType childType =
              TrainingExamQuestionType.parse(child.questionType());
          if (childType == TrainingExamQuestionType.SHORT_ANSWER) {
            pendingReview = true;
          } else if (normalizeAnswer(String.join(",", child.correctAnswers()))
              .equals(normalizeAnswer(answer))) {
            actual = actual.add(child.score() == null ? BigDecimal.ZERO : child.score());
          }
        }
        selected = json(caseAnswers);
      } else {
        selected = answers.get(miniQuestionKey(question.id, null));
        if (type == TrainingExamQuestionType.SHORT_ANSWER) {
          pendingReview = true;
        } else if (normalizeAnswer(question.answer).equals(normalizeAnswer(selected))) {
          actual = question.score == null ? BigDecimal.ZERO : question.score;
        }
      }
      actualScores.put(question.id, scale(actual));
      selectedAnswers.put(question.id, selected);
      total = total.add(actual);
    }
    LocalDateTime submittedAt = LocalDateTime.now();
    String submittedStatus =
        pendingReview
            ? TrainingExamResultStatus.PENDING_REVIEW.name()
            : TrainingExamResultStatus.EXAMED.name();
    int updated =
        resultMapper.update(
            null,
            new UpdateWrapper<TrainingExamResult>()
                .eq("id", result.id)
                .eq("status", TrainingExamResultStatus.PENDING_EXAM.name())
                .set("score", scale(total))
                .set("status", submittedStatus)
                .set("submission_request_id", request.requestId().trim())
                .set("submitted_at", submittedAt)
                .set(
                    "remaining_seconds",
                    request.remainingSeconds() == null
                        ? result.remainingSeconds
                        : Math.max(0, request.remainingSeconds()))
                .set("updated_by", CurrentUserContext.require().userId())
                .set("updated_at", submittedAt));
    if (updated != 1) {
      TrainingExamResult current = requireMyResult(taskId);
      if (request.requestId().trim().equals(current.submissionRequestId)) {
        return miniExam(current, true);
      }
      throw new BusinessException("该考试已提交，请勿重复交卷");
    }
    for (TrainingExamResultQuestion question : questions) {
      question.selectedOption = selectedAnswers.get(question.id);
      question.actualScore = actualScores.get(question.id);
      question.updatedBy = CurrentUserContext.require().userId();
      question.updatedAt = submittedAt;
      resultQuestionMapper.updateById(question);
    }
    auditLogService.record(
        SystemModule.TRAINING, "TRAINING_EXAM_RESULT", result.id, "SUBMIT", "小程序考试交卷");
    notificationService.markBusinessHandled("TRAINING_EXAM_TASK", task.id, result.examPersonUserId);
    enqueueExamNotification(
        task, result.examPersonUserId, "EXAM_SUBMITTED", result.examPersonUserId, request.requestId().trim());
    return miniExam(requireMyResult(taskId), true);
  }

  @Transactional
  public TrainingExamTaskResponse createTask(TrainingExamTaskRequest request) {
    permissionPolicy.assertCanCreateExamTasks();
    CurrentUser user = CurrentUserContext.require();
    TrainingExamTask task = new TrainingExamTask();
    applyTaskRequest(task, request);
    boolean automaticScope =
        request.examPersonUserIds() == null || request.examPersonUserIds().isEmpty();
    List<Long> personIds =
        automaticScope
            ? examScopePersonIds(task)
            : validatePersonIds(request.examPersonUserIds());
    List<TrainingExamQuestionPayload> questions = validateQuestions(request.questions());
    task.createdBy = user.userId();
    task.updatedBy = user.userId();
    taskMapper.insert(task);
    task.code = code("EXAM-TASK", task.examDate, task.id);
    taskMapper.updateById(task);

    List<TrainingExamQuestion> savedQuestions = new ArrayList<>();
    for (int i = 0; i < questions.size(); i++) {
      TrainingExamQuestion question = taskQuestion(task.id, questions.get(i), i, user.userId());
      questionMapper.insert(question);
      savedQuestions.add(question);
    }

    for (Long personId : personIds) {
      SysUser examPerson = requireUser(personId);
      TrainingExamResult result = new TrainingExamResult();
      result.taskId = task.id;
      if (automaticScope) {
        SysOrg examPersonOrg = requireActiveOrg(examPerson.orgId, "考试人员组织不存在");
        result.companyId = task.companyId;
        result.departmentId =
            requireAncestor(examPersonOrg, "DEPARTMENT", "考试人员所属部门不存在").id;
      } else {
        result.companyId = task.companyId;
        result.departmentId = task.departmentId;
      }
      result.examPersonUserId = examPerson.id;
      result.examPersonName = text(examPerson.realName);
      result.score = BigDecimal.ZERO.setScale(1);
      result.examDate = task.examDate;
      result.status = TrainingExamResultStatus.PENDING_EXAM.name();
      result.remark = task.remark;
      result.createdBy = user.userId();
      result.updatedBy = user.userId();
      resultMapper.insert(result);
      result.code = code("EXAM-USER", task.examDate, result.id);
      resultMapper.updateById(result);
      copyQuestionsToResult(result.id, savedQuestions, user.userId(), true);
      enqueueExamNotification(
          task, examPerson.id, "EXAM_ASSIGNED", user.userId(), String.valueOf(result.id));
    }

    auditLogService.record(SystemModule.TRAINING, "TRAINING_EXAM_TASK", task.id, "CREATE", "新增考试任务");
    return taskResponse(requireTask(task.id));
  }

  public TrainingExamTaskDetailResponse taskDetail(Long id) {
    permissionPolicy.assertCanViewExamTasks();
    TrainingExamTask task = requireTask(id);
    TrainingExamTaskStatus status = TrainingExamTaskStatus.parse(task.status);
    List<TrainingExamResultResponse> results =
        resultMapper.selectList(new QueryWrapper<TrainingExamResult>().eq("task_id", task.id).eq("deleted", 0)
                .orderByAsc("id"))
            .stream()
            .map(this::resultResponse)
            .toList();
    return new TrainingExamTaskDetailResponse(
        task.id,
        task.code,
        task.companyId,
        orgName(task.companyId),
        task.departmentId,
        task.departmentId == null ? "全部部门" : orgName(task.departmentId),
        task.teamId,
        task.teamId == null ? "全部班组" : orgName(task.teamId),
        task.exam,
        task.examDate,
        task.durationMinutes,
        status.name(),
        status.label(),
        task.remark,
        task.createdAt,
        task.updatedAt,
        taskQuestions(task.id),
        results);
  }

  @Transactional
  public void deleteTask(Long id) {
    permissionPolicy.assertCanDeleteExamTasks();
    TrainingExamTask task = requireTask(id);
    assertTaskHasNoResults(task.id);
    softDeleteTasks(List.of(task.id));
    auditLogService.record(SystemModule.TRAINING, "TRAINING_EXAM_TASK", task.id, "DELETE", "删除考试任务");
  }

  @Transactional
  public void batchDeleteTasks(TrainingExamBatchDeleteRequest request) {
    permissionPolicy.assertCanDeleteExamTasks();
    List<Long> ids = safeIds(request);
    ids.forEach(this::requireTask);
    ids.forEach(this::assertTaskHasNoResults);
    softDeleteTasks(ids);
    auditLogService.record(SystemModule.TRAINING, "TRAINING_EXAM_TASK", null, "BATCH_DELETE", "批量删除考试任务");
  }

  public PageResult<TrainingExamResultResponse> listResults(TrainingExamResultQuery query) {
    permissionPolicy.assertCanViewExamResults();
    List<TrainingExamResult> all = resultMapper.selectList(resultWrapper(query));
    int size = query == null || query.pageSize() == null ? 20 : query.pageSize();
    int page = query == null || query.page() == null ? 1 : query.page();
    int from = Math.max(0, (page - 1) * size);
    int to = Math.min(all.size(), from + size);
    if (from > to) {
      from = to;
    }
    return new PageResult<>(all.subList(from, to).stream().map(this::resultResponse).toList(), all.size());
  }

  public PageResult<TrainingExamResultResponse> listTaskResults(Long taskId, TrainingExamResultQuery query) {
    permissionPolicy.assertCanViewExamTasks();
    TrainingExamTask task = requireTask(taskId);
    TrainingExamResultQuery scoped =
        new TrainingExamResultQuery(
            query == null ? null : query.companyId(),
            query == null ? null : query.departmentId(),
            task.id,
            query == null ? null : query.dateStart(),
            query == null ? null : query.dateEnd(),
            query == null ? null : query.status(),
            query == null ? null : query.page(),
            query == null ? null : query.pageSize());
    return listResults(scoped);
  }

  @Transactional
  public TrainingExamResultResponse createResult(TrainingExamResultRequest request) {
    permissionPolicy.assertCanCreateExamResults();
    CurrentUser user = CurrentUserContext.require();
    if (request == null) {
      throw new BusinessException("考试成绩不能为空");
    }
    TrainingExamTask task = requireTask(request.taskId());
    SysUser examPerson = requireUser(request.examPersonUserId());
    validateScore(request.score(), "考分");
    TrainingExamResult result = new TrainingExamResult();
    result.taskId = task.id;
    result.companyId = task.companyId;
    result.departmentId = task.departmentId;
    result.examPersonUserId = examPerson.id;
    result.examPersonName = text(examPerson.realName);
    result.score = scale(request.score());
    result.examDate = task.examDate;
    result.status = resultStatusForScore(result.score);
    result.remark = task.remark;
    result.createdBy = user.userId();
    result.updatedBy = user.userId();
    resultMapper.insert(result);
    result.code = code("EXAM-USER", task.examDate, result.id);
    resultMapper.updateById(result);
    copyQuestionsToResult(result.id, taskQuestionEntities(task.id), user.userId(), false);
    auditLogService.record(SystemModule.TRAINING, "TRAINING_EXAM_RESULT", result.id, "CREATE", "新增考试成绩");
    enqueueExamNotification(
        task, examPerson.id, "EXAM_RESULT_PUBLISHED", user.userId(), String.valueOf(result.id));
    return resultResponse(requireResult(result.id));
  }

  private void enqueueExamNotification(
      TrainingExamTask task,
      Long recipientUserId,
      String eventType,
      Long operatorId,
      String eventSuffix) {
    notificationOutboxService.enqueue(
        new DomainNotificationEvent(
            "exam:" + eventType + ":" + task.id + ":" + recipientUserId + ":" + eventSuffix,
            eventType,
            "TRAINING_EXAM_TASK",
            task.id,
            task.departmentId == null ? task.companyId : task.departmentId,
            List.of(recipientUserId),
            List.of(),
            task.createdBy,
            operatorId,
            task.examDate == null ? null : task.examDate.atTime(LocalTime.MAX),
            Map.of("summary", text(task.exam) + " · " + (task.examDate == null ? "" : task.examDate))));
  }

  public TrainingExamResultDetailResponse resultDetail(Long id) {
    permissionPolicy.assertCanViewExamResults();
    TrainingExamResult result = requireResultForRead(id);
    TrainingExamTask task = requireTaskForRead(result.taskId);
    TrainingExamResultStatus status = TrainingExamResultStatus.parse(result.status);
    return new TrainingExamResultDetailResponse(
        result.id,
        result.code,
        result.taskId,
        result.companyId,
        orgName(result.companyId),
        result.departmentId,
        orgName(result.departmentId),
        result.examPersonUserId,
        result.examPersonName,
        task.exam,
        result.score,
        result.examDate,
        status.name(),
        status.label(),
        result.remark,
        result.createdAt,
        result.updatedAt,
        resultQuestions(result.id));
  }

  @Transactional
  public TrainingExamResultDetailResponse reviewResult(
      Long id, TrainingExamReviewRequest request) {
    permissionPolicy.assertCanCreateExamResults();
    TrainingExamResult result = requireResult(id);
    if (!TrainingExamResultStatus.PENDING_REVIEW.name().equals(result.status)) {
      throw new BusinessException("仅待评分的考试可以人工评分");
    }
    List<TrainingExamResultQuestion> questions = resultQuestionEntities(result.id);
    List<TrainingExamResultQuestion> manualQuestions =
        questions.stream().filter(this::requiresManualReview).toList();
    Map<Long, BigDecimal> scores = reviewScoreMap(request == null ? null : request.scores());
    if (scores.size() != manualQuestions.size()
        || manualQuestions.stream().anyMatch(question -> !scores.containsKey(question.id))) {
      throw new BusinessException("请完成全部问答题评分");
    }
    LocalDateTime reviewedAt = LocalDateTime.now();
    for (TrainingExamResultQuestion question : manualQuestions) {
      BigDecimal score = scores.get(question.id);
      validateScore(score, "实际分");
      BigDecimal max = question.score == null ? BigDecimal.ZERO : question.score;
      if (score.compareTo(max) > 0) {
        throw new BusinessException("实际分不能超过题目分值");
      }
      question.actualScore = scale(score);
    }
    int claimed =
        resultMapper.update(
            null,
            new UpdateWrapper<TrainingExamResult>()
                .eq("id", result.id)
                .eq("status", TrainingExamResultStatus.PENDING_REVIEW.name())
                .set("status", TrainingExamResultStatus.EXAMED.name())
                .set("updated_by", CurrentUserContext.require().userId())
                .set("updated_at", reviewedAt));
    if (claimed != 1) {
      throw new BusinessException("该考试已完成评分，请勿重复评分");
    }
    for (TrainingExamResultQuestion question : manualQuestions) {
      question.updatedBy = CurrentUserContext.require().userId();
      question.updatedAt = reviewedAt;
      resultQuestionMapper.updateById(question);
    }
    BigDecimal total =
        questions.stream()
            .map(question -> question.actualScore == null ? BigDecimal.ZERO : question.actualScore)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    resultMapper.update(
        null,
        new UpdateWrapper<TrainingExamResult>()
            .eq("id", result.id)
            .set("score", scale(total))
            .set("updated_at", reviewedAt));
    auditLogService.record(
        SystemModule.TRAINING, "TRAINING_EXAM_RESULT", result.id, "REVIEW", "人工评分完成");
    return resultDetail(result.id);
  }

  private boolean requiresManualReview(TrainingExamResultQuestion question) {
    TrainingExamQuestionType type = storedQuestionType(question.questionType);
    return type == TrainingExamQuestionType.SHORT_ANSWER
        || (type == TrainingExamQuestionType.CASE_ANALYSIS
            && resultQuestionChildren(question).stream()
                .anyMatch(
                    child ->
                        TrainingExamQuestionType.parse(child.questionType())
                            == TrainingExamQuestionType.SHORT_ANSWER));
  }

  private Map<Long, BigDecimal> reviewScoreMap(List<TrainingExamReviewScore> scores) {
    if (scores == null || scores.isEmpty()) {
      throw new BusinessException("评分不能为空");
    }
    Map<Long, BigDecimal> mapped = new HashMap<>();
    for (TrainingExamReviewScore score : scores) {
      if (score == null || score.questionId() == null || score.actualScore() == null) {
        throw new BusinessException("评分不能为空");
      }
      if (mapped.put(score.questionId(), score.actualScore()) != null) {
        throw new BusinessException("题目评分重复");
      }
    }
    return mapped;
  }

  @Transactional
  public void deleteResult(Long id) {
    permissionPolicy.assertCanDeleteExamResults();
    TrainingExamResult result = requireResult(id);
    softDeleteResults(List.of(result.id));
    auditLogService.record(SystemModule.TRAINING, "TRAINING_EXAM_RESULT", result.id, "DELETE", "删除考试成绩");
  }

  @Transactional
  public void batchDeleteResults(TrainingExamBatchDeleteRequest request) {
    permissionPolicy.assertCanDeleteExamResults();
    List<Long> ids = safeIds(request);
    ids.forEach(this::requireResult);
    softDeleteResults(ids);
    auditLogService.record(SystemModule.TRAINING, "TRAINING_EXAM_RESULT", null, "BATCH_DELETE", "批量删除考试成绩");
  }

  public PageResult<TrainingExamQuestionBankResponse> listQuestionBank(TrainingExamQuestionBankQuery query) {
    permissionPolicy.assertCanViewExamTasks();
    List<TrainingExamQuestionBank> all = questionBankMapper.selectList(questionBankWrapper(query));
    int size = query == null || query.pageSize() == null ? 20 : query.pageSize();
    int page = query == null || query.page() == null ? 1 : query.page();
    int from = Math.max(0, (page - 1) * size);
    int to = Math.min(all.size(), from + size);
    if (from > to) {
      from = to;
    }
    return new PageResult<>(all.subList(from, to).stream().map(this::questionBankResponse).toList(), all.size());
  }

  @Transactional
  public TrainingExamQuestionBankResponse createQuestionBank(TrainingExamQuestionBankRequest request) {
    permissionPolicy.assertCanCreateExamTasks();
    CurrentUser user = CurrentUserContext.require();
    TrainingExamQuestionBank question = new TrainingExamQuestionBank();
    applyQuestionBankRequest(question, request);
    question.createdBy = user.userId();
    question.updatedBy = user.userId();
    questionBankMapper.insert(question);
    auditLogService.record(SystemModule.TRAINING, "TRAINING_EXAM_QUESTION_BANK", question.id, "CREATE", "新增考试题库试题");
    return questionBankResponse(question);
  }

  @Transactional
  public TrainingExamQuestionBankResponse updateQuestionBank(Long id, TrainingExamQuestionBankRequest request) {
    permissionPolicy.assertCanCreateExamTasks();
    CurrentUser user = CurrentUserContext.require();
    TrainingExamQuestionBank question = requireQuestionBank(id);
    applyQuestionBankRequest(question, request);
    question.updatedBy = user.userId();
    question.updatedAt = LocalDateTime.now();
    questionBankMapper.updateById(question);
    auditLogService.record(SystemModule.TRAINING, "TRAINING_EXAM_QUESTION_BANK", question.id, "UPDATE", "编辑考试题库试题");
    return questionBankResponse(question);
  }

  @Transactional
  public void deleteQuestionBank(Long id) {
    permissionPolicy.assertCanDeleteExamTasks();
    TrainingExamQuestionBank question = requireQuestionBank(id);
    questionBankMapper.update(
        null,
        new UpdateWrapper<TrainingExamQuestionBank>()
            .eq("id", question.id)
            .set("deleted", 1)
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
    auditLogService.record(SystemModule.TRAINING, "TRAINING_EXAM_QUESTION_BANK", question.id, "DELETE", "删除考试题库试题");
  }

  @Transactional
  public void batchDeleteQuestionBank(TrainingExamBatchDeleteRequest request) {
    permissionPolicy.assertCanDeleteExamTasks();
    List<Long> ids = safeIds(request);
    ids.forEach(this::requireQuestionBank);
    questionBankMapper.update(
        null,
        new UpdateWrapper<TrainingExamQuestionBank>()
            .in("id", ids)
            .set("deleted", 1)
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
    auditLogService.record(
        SystemModule.TRAINING,
        "TRAINING_EXAM_QUESTION_BANK",
        null,
        "BATCH_DELETE",
        "批量删除考试题库试题");
  }

  public PageResult<TrainingExamPaperResponse> listPapers(TrainingExamPaperQuery query) {
    permissionPolicy.assertCanViewExamTasks();
    List<TrainingExamPaper> all = paperMapper.selectList(paperWrapper(query));
    int size = query == null || query.pageSize() == null ? 20 : query.pageSize();
    int page = query == null || query.page() == null ? 1 : query.page();
    int from = Math.max(0, (page - 1) * size);
    int to = Math.min(all.size(), from + size);
    if (from > to) {
      from = to;
    }
    return new PageResult<>(
        all.subList(from, to).stream().map(this::paperResponse).toList(), all.size());
  }

  @Transactional
  public TrainingExamPaperResponse createPaper(TrainingExamPaperRequest request) {
    permissionPolicy.assertCanCreateExamTasks();
    CurrentUser user = CurrentUserContext.require();
    TrainingExamPaper paper = new TrainingExamPaper();
    applyPaperRequest(paper, request);
    paper.createdBy = user.userId();
    paper.updatedBy = user.userId();
    paperMapper.insert(paper);
    auditLogService.record(
        SystemModule.TRAINING, "TRAINING_EXAM_PAPER", paper.id, "CREATE", "新增考试试卷");
    return paperResponse(paper);
  }

  @Transactional
  public TrainingExamPaperResponse updatePaper(
      Long id, TrainingExamPaperRequest request) {
    permissionPolicy.assertCanCreateExamTasks();
    TrainingExamPaper paper = requirePaper(id);
    applyPaperRequest(paper, request);
    paper.updatedBy = CurrentUserContext.require().userId();
    paper.updatedAt = LocalDateTime.now();
    paperMapper.updateById(paper);
    auditLogService.record(
        SystemModule.TRAINING, "TRAINING_EXAM_PAPER", paper.id, "UPDATE", "编辑考试试卷");
    return paperResponse(paper);
  }

  @Transactional
  public void deletePaper(Long id) {
    permissionPolicy.assertCanDeleteExamTasks();
    TrainingExamPaper paper = requirePaper(id);
    softDeletePapers(List.of(paper.id));
    auditLogService.record(
        SystemModule.TRAINING, "TRAINING_EXAM_PAPER", paper.id, "DELETE", "删除考试试卷");
  }

  @Transactional
  public void batchDeletePapers(TrainingExamBatchDeleteRequest request) {
    permissionPolicy.assertCanDeleteExamTasks();
    List<Long> ids = safeIds(request);
    ids.forEach(this::requirePaper);
    softDeletePapers(ids);
    auditLogService.record(
        SystemModule.TRAINING, "TRAINING_EXAM_PAPER", null, "BATCH_DELETE", "批量删除考试试卷");
  }

  public ExcelFile questionBankTemplate() {
    permissionPolicy.assertCanDownloadExamTasks();
    return questionWorkbook("training-exam-question-bank-template.xlsx", List.of());
  }

  @Transactional
  public TrainingExamQuestionBankImportResult importQuestionBank(
      Long companyId, Long departmentId, Long teamId, MultipartFile file) {
    permissionPolicy.assertCanCreateExamTasks();
    if (file == null || file.isEmpty()) {
      throw new BusinessException("请选择要上传的考题文件");
    }
    CurrentUser user = CurrentUserContext.require();
    ExamOrgScope orgScope = validateQuestionBankOrg(companyId, departmentId, teamId);
    List<String> errors = new ArrayList<>();
    List<TrainingExamQuestionBankResponse> questions = new ArrayList<>();
    DataFormatter formatter = new DataFormatter();
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.getBytes()))) {
      var sheet = workbook.getSheetAt(0);
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null || blank(formatter.formatCellValue(row.getCell(0))) && blank(formatter.formatCellValue(row.getCell(1)))) {
          continue;
        }
        try {
          TrainingExamQuestionPayload normalized =
              validateQuestion(
                  new TrainingExamQuestionPayload(
                      formatter.formatCellValue(row.getCell(0)),
                      formatter.formatCellValue(row.getCell(1)),
                      formatter.formatCellValue(row.getCell(2)),
                      formatter.formatCellValue(row.getCell(3)),
                      formatter.formatCellValue(row.getCell(4)),
                      decimal(formatter.formatCellValue(row.getCell(5)), "分值"),
                      decimal(formatter.formatCellValue(row.getCell(6)), "实际分"),
                      rowIndex),
                  rowIndex);
          TrainingExamQuestionBank question =
              questionBankQuestion(orgScope, normalized, user.userId());
          questionBankMapper.insert(question);
          questions.add(questionBankResponse(question));
        } catch (BusinessException e) {
          errors.add("第" + (rowIndex + 1) + "行：" + e.getMessage());
        }
      }
    } catch (IOException e) {
      throw new BusinessException("考题文件解析失败");
    }
    auditLogService.record(SystemModule.TRAINING, "TRAINING_EXAM_QUESTION_BANK", null, "IMPORT", "导入考试题库试题");
    return new TrainingExamQuestionBankImportResult(questions.size(), errors, questions);
  }

  public TrainingExamPdfPreviewResponse previewQuestionBankPdf(MultipartFile file) {
    permissionPolicy.assertCanCreateExamTasks();
    return pdfParser.parse(file);
  }

  @Transactional
  public TrainingExamPdfConfirmResult confirmQuestionBankPdf(TrainingExamPdfConfirmRequest request) {
    permissionPolicy.assertCanCreateExamTasks();
    if (request == null || request.questions() == null || request.questions().isEmpty()) {
      throw new BusinessException("PDF 解析草稿不能为空");
    }
    String target = pdfConfirmTarget(request.target());
    if ("PAPER".equals(target)) {
      List<TrainingExamQuestionResponse> paperQuestions = new ArrayList<>();
      for (int index = 0; index < request.questions().size(); index++) {
        TrainingExamPdfQuestionDraft draft = requirePdfDraft(request.questions().get(index), index);
        try {
          TrainingExamQuestionPayload normalized = validateQuestion(pdfDraftPayload(draft, index), index);
          paperQuestions.add(questionResponse(null, normalized, index));
        } catch (BusinessException exception) {
          throw pdfDraftException(draft, index, exception);
        }
      }
      return new TrainingExamPdfConfirmResult(
          target,
          paperQuestions.size(),
          List.of(),
          List.of(),
          List.copyOf(paperQuestions));
    }

    ExamOrgScope orgScope =
        validateQuestionBankOrg(
            request.companyId(), request.departmentId(), request.teamId());
    CurrentUser user = CurrentUserContext.require();
    List<TrainingExamQuestionBankResponse> questions = new ArrayList<>();
    for (int index = 0; index < request.questions().size(); index++) {
      TrainingExamPdfQuestionDraft draft = requirePdfDraft(request.questions().get(index), index);
      TrainingExamQuestionBank question = new TrainingExamQuestionBank();
      try {
        applyQuestionBankRequest(
            question,
            new TrainingExamQuestionBankRequest(
                orgScope.company().id,
                orgScope.department() == null ? null : orgScope.department().id,
                orgScope.team() == null ? null : orgScope.team().id,
                draft.questionType(),
                draft.questionText(),
                "",
                "",
                "",
                draft.score(),
                BigDecimal.ZERO,
                draft.options(),
                draft.correctAnswers(),
                draft.referenceAnswer(),
                draft.answerExplanation(),
                draft.caseMaterial(),
                draft.children()));
      } catch (BusinessException exception) {
        throw pdfDraftException(draft, index, exception);
      }
      question.createdBy = user.userId();
      question.updatedBy = user.userId();
      questionBankMapper.insert(question);
      questions.add(questionBankResponse(question));
    }
    auditLogService.record(
        SystemModule.TRAINING,
        "TRAINING_EXAM_QUESTION_BANK",
        null,
        "PDF_IMPORT",
        "确认导入 PDF 考试题库试题");
    return new TrainingExamPdfConfirmResult(
        target, questions.size(), List.of(), List.copyOf(questions), List.of());
  }

  private String pdfConfirmTarget(String target) {
    if (blank(target) || "QUESTION_BANK".equalsIgnoreCase(target)) {
      return "QUESTION_BANK";
    }
    if ("PAPER".equalsIgnoreCase(target)) {
      return "PAPER";
    }
    throw new BusinessException("PDF 确认目标仅支持 QUESTION_BANK 或 PAPER");
  }

  private TrainingExamPdfQuestionDraft requirePdfDraft(
      TrainingExamPdfQuestionDraft draft, int index) {
    if (draft == null) {
      throw new BusinessException("第" + (index + 1) + "条 PDF 草稿不能为空");
    }
    return draft;
  }

  private BusinessException pdfDraftException(
      TrainingExamPdfQuestionDraft draft, int index, BusinessException exception) {
    return new BusinessException(
        "第"
            + (index + 1)
            + "条 PDF 草稿"
            + (blank(draft.sourceLabel()) ? "" : "（" + draft.sourceLabel() + "）")
            + "："
            + exception.getMessage());
  }

  private TrainingExamQuestionPayload pdfDraftPayload(
      TrainingExamPdfQuestionDraft draft, int sortOrder) {
    return new TrainingExamQuestionPayload(
        draft.questionType(),
        draft.questionText(),
        "",
        "",
        "",
        draft.score(),
        BigDecimal.ZERO,
        sortOrder,
        draft.options(),
        draft.correctAnswers(),
        draft.referenceAnswer(),
        draft.answerExplanation(),
        draft.caseMaterial(),
        draft.children());
  }

  public ExcelFile exportQuestionBank(TrainingExamQuestionBankQuery query) {
    permissionPolicy.assertCanDownloadExamTasks();
    List<TrainingExamQuestionResponse> questions =
        questionBankMapper.selectList(questionBankWrapper(query)).stream().map(this::questionResponse).toList();
    return questionWorkbook("training-exam-question-bank.xlsx", questions);
  }

  public ExcelFile exportTaskQuestions(Long taskId) {
    permissionPolicy.assertCanDownloadExamTasks();
    TrainingExamTask task = requireTask(taskId);
    return questionWorkbook("training-exam-task-questions-" + task.id + ".xlsx", taskQuestions(task.id));
  }

  public ExcelFile questionTemplate() {
    permissionPolicy.assertCanDownloadExamTasks();
    return questionWorkbook("training-exam-question-template.xlsx", List.of());
  }

  private ExcelFile questionWorkbook(String filename, List<TrainingExamQuestionResponse> questions) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("考题");
      CellStyle headerStyle = workbook.createCellStyle();
      Font font = workbook.createFont();
      font.setBold(true);
      headerStyle.setFont(font);
      Row header = sheet.createRow(0);
      for (int i = 0; i < QUESTION_HEADERS.size(); i++) {
        Cell cell = header.createCell(i);
        cell.setCellValue(QUESTION_HEADERS.get(i));
        cell.setCellStyle(headerStyle);
        sheet.setColumnWidth(i, i == 1 || i == 3 ? 7000 : 3600);
      }
      if (questions == null || questions.isEmpty()) {
        Row sample = sheet.createRow(1);
        List<String> values = List.of("单选题", "安全生产的方针是？", "A", "A、B、C、D", "A", "5", "0");
        for (int i = 0; i < values.size(); i++) {
          sample.createCell(i).setCellValue(values.get(i));
        }
      } else {
        for (int rowIndex = 0; rowIndex < questions.size(); rowIndex++) {
          TrainingExamQuestionResponse question = questions.get(rowIndex);
          Row row = sheet.createRow(rowIndex + 1);
          String[] values = {
            question.questionType(),
            question.questionText(),
            question.selectedOption(),
            question.allOptions(),
            question.answer(),
            question.score().toPlainString(),
            question.actualScore().toPlainString()
          };
          for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i] == null ? "" : values[i]);
          }
        }
      }
      workbook.write(output);
      return new ExcelFile(filename, output.toByteArray());
    } catch (IOException e) {
      throw new BusinessException("考题文件生成失败");
    }
  }

  public TrainingExamQuestionImportResult importQuestions(MultipartFile file) {
    permissionPolicy.assertCanCreateExamTasks();
    if (file == null || file.isEmpty()) {
      throw new BusinessException("请选择要上传的考题文件");
    }
    List<String> errors = new ArrayList<>();
    List<TrainingExamQuestionResponse> questions = new ArrayList<>();
    DataFormatter formatter = new DataFormatter();
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.getBytes()))) {
      var sheet = workbook.getSheetAt(0);
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null || blank(formatter.formatCellValue(row.getCell(0))) && blank(formatter.formatCellValue(row.getCell(1)))) {
          continue;
        }
        try {
          TrainingExamQuestionPayload payload =
              new TrainingExamQuestionPayload(
                  formatter.formatCellValue(row.getCell(0)),
                  formatter.formatCellValue(row.getCell(1)),
                  formatter.formatCellValue(row.getCell(2)),
                  formatter.formatCellValue(row.getCell(3)),
                  formatter.formatCellValue(row.getCell(4)),
                  decimal(formatter.formatCellValue(row.getCell(5)), "分值"),
                  decimal(formatter.formatCellValue(row.getCell(6)), "实际分"),
                  rowIndex);
          TrainingExamQuestionPayload normalized = validateQuestion(payload, rowIndex);
          questions.add(questionResponse(null, normalized, questions.size()));
        } catch (BusinessException e) {
          errors.add("第" + (rowIndex + 1) + "行：" + e.getMessage());
        }
      }
    } catch (IOException e) {
      throw new BusinessException("考题文件解析失败");
    }
    return new TrainingExamQuestionImportResult(questions.size(), errors, questions);
  }

  private QueryWrapper<TrainingExamTask> taskWrapper(TrainingExamTaskQuery query) {
    TrainingExamTaskQuery safe = query == null ? new TrainingExamTaskQuery(null, null, null, null, null, null, null) : query;
    QueryWrapper<TrainingExamTask> wrapper = new QueryWrapper<TrainingExamTask>().eq("deleted", 0);
    dataScopeService.applyCompanyReadScope(wrapper, "company_id");
    dataScopeService.applyOptionalDepartmentReadScope(wrapper, "department_id");
    if (safe.companyId() != null) {
      dataScopeService.assertCanReadCompany(safe.companyId());
      wrapper.eq("company_id", safe.companyId());
    }
    if (safe.departmentId() != null) {
      dataScopeService.assertCanReadDepartment(safe.departmentId());
      wrapper.eq("department_id", safe.departmentId());
    }
    if (safe.dateStart() != null) {
      wrapper.ge("exam_date", safe.dateStart());
    }
    if (safe.dateEnd() != null) {
      wrapper.le("exam_date", safe.dateEnd());
    }
    if (!blank(safe.status()) && !"all".equalsIgnoreCase(safe.status())) {
      wrapper.eq("status", TrainingExamTaskStatus.parse(safe.status()).name());
    }
    wrapper.orderByDesc("exam_date").orderByDesc("id");
    return wrapper;
  }

  private QueryWrapper<TrainingExamQuestionBank> questionBankWrapper(TrainingExamQuestionBankQuery query) {
    TrainingExamQuestionBankQuery safe =
        query == null
            ? new TrainingExamQuestionBankQuery(
                null, null, null, null, null, null, null, null)
            : query;
    QueryWrapper<TrainingExamQuestionBank> wrapper = new QueryWrapper<TrainingExamQuestionBank>().eq("deleted", 0);
    dataScopeService.applyCompanyReadScope(wrapper, "company_id");
    applyQuestionBankReadScope(wrapper);
    if (safe.companyId() != null) {
      dataScopeService.assertCanReadCompany(safe.companyId());
      wrapper.eq("company_id", safe.companyId());
    }
    if (Boolean.TRUE.equals(safe.applicable()) && safe.companyId() != null) {
      applyApplicableQuestionBankScope(
          wrapper, safe.companyId(), safe.departmentId(), safe.teamId());
    } else {
      if (safe.departmentId() != null) {
        dataScopeService.assertCanReadDepartment(safe.departmentId());
        wrapper.eq("department_id", safe.departmentId());
      }
      if (safe.teamId() != null) {
        dataScopeService.assertCanAccessOrg(safe.teamId());
        wrapper.eq("team_id", safe.teamId());
      }
    }
    if (!blank(safe.questionType()) && !"all".equalsIgnoreCase(safe.questionType())) {
      TrainingExamQuestionType type = TrainingExamQuestionType.parse(safe.questionType());
      wrapper.and(
          nested ->
              nested
                  .eq("question_type", type.name())
                  .or()
                  .eq("question_type", type.label()));
    }
    if (!blank(safe.keyword())) {
      String keyword = text(safe.keyword());
      wrapper.and(
          nested ->
              nested
                  .like("question_text", keyword)
                  .or()
                  .like("selected_option", keyword)
                  .or()
                  .like("all_options", keyword)
                  .or()
                  .like("answer", keyword));
    }
    wrapper.orderByDesc("updated_at").orderByDesc("id");
    return wrapper;
  }

  private QueryWrapper<TrainingExamPaper> paperWrapper(TrainingExamPaperQuery query) {
    TrainingExamPaperQuery safe =
        query == null
            ? new TrainingExamPaperQuery(null, null, null, null, null, null)
            : query;
    QueryWrapper<TrainingExamPaper> wrapper =
        new QueryWrapper<TrainingExamPaper>().eq("deleted", 0);
    dataScopeService.applyOptionalCompanyReadScope(wrapper, "company_id");
    dataScopeService.applyOptionalDepartmentReadScope(wrapper, "department_id");
    applyPaperTeamReadScope(wrapper);
    if (safe.companyId() != null) {
      dataScopeService.assertCanReadCompany(safe.companyId());
      wrapper.and(
          nested ->
              nested.isNull("company_id").or().eq("company_id", safe.companyId()));
    }
    if (safe.departmentId() != null) {
      dataScopeService.assertCanReadDepartment(safe.departmentId());
      wrapper.and(
          nested ->
              nested
                  .isNull("department_id")
                  .or()
                  .eq("department_id", safe.departmentId()));
      if (safe.teamId() == null) {
        wrapper.isNull("team_id");
      }
    }
    if (safe.teamId() != null) {
      if (safe.departmentId() == null) {
        throw new BusinessException("选择班组前请先选择部门");
      }
      dataScopeService.assertCanAccessOrg(safe.teamId());
      SysOrg team = requireOrg(safe.teamId(), "TEAM", "班组不存在");
      if (team.orgPath == null
          || !team.orgPath.contains("/" + safe.departmentId() + "/")) {
        throw new BusinessException("班组不属于所选部门");
      }
      wrapper.and(
          nested -> nested.isNull("team_id").or().eq("team_id", safe.teamId()));
    }
    if (!blank(safe.keyword())) {
      wrapper.and(
          nested ->
              nested
                  .like("paper_name", text(safe.keyword()))
                  .or()
                  .like("description", text(safe.keyword())));
    }
    wrapper.orderByDesc("updated_at").orderByDesc("id");
    return wrapper;
  }

  private void applyPaperTeamReadScope(QueryWrapper<TrainingExamPaper> wrapper) {
    if ("ALL".equals(dataScopeService.currentDataScope())) {
      return;
    }
    List<SysOrg> activeOrgs =
        orgMapper.selectList(new QueryWrapper<SysOrg>().eq("deleted", 0));
    List<Long> accessibleOrgIds = dataScopeService.accessibleOrgIds();
    List<SysOrg> accessibleOrgs =
        activeOrgs.stream()
            .filter(org -> accessibleOrgIds.contains(org.id))
            .toList();
    List<Long> teamIds =
        activeOrgs.stream()
            .filter(org -> "TEAM".equals(org.orgType))
            .filter(
                org ->
                    accessibleOrgs.stream()
                        .anyMatch(accessible -> orgsOverlap(org, accessible)))
            .map(org -> org.id)
            .toList();
    wrapper.and(
        scope -> {
          scope.isNull("team_id");
          if (!teamIds.isEmpty()) {
            scope.or().in("team_id", teamIds);
          }
        });
  }

  private void applyQuestionBankReadScope(
      QueryWrapper<TrainingExamQuestionBank> wrapper) {
    if ("ALL".equals(dataScopeService.currentDataScope())) {
      return;
    }
    List<SysOrg> activeOrgs =
        orgMapper.selectList(new QueryWrapper<SysOrg>().eq("deleted", 0));
    List<Long> accessibleOrgIds = dataScopeService.accessibleOrgIds();
    List<SysOrg> accessibleOrgs =
        activeOrgs.stream()
            .filter(org -> accessibleOrgIds.contains(org.id))
            .toList();
    List<Long> departmentIds =
        activeOrgs.stream()
            .filter(org -> "DEPARTMENT".equals(org.orgType))
            .filter(org -> accessibleOrgs.stream().anyMatch(accessible -> orgsOverlap(org, accessible)))
            .map(org -> org.id)
            .toList();
    List<Long> teamIds =
        activeOrgs.stream()
            .filter(org -> "TEAM".equals(org.orgType))
            .filter(org -> accessibleOrgs.stream().anyMatch(accessible -> orgsOverlap(org, accessible)))
            .map(org -> org.id)
            .toList();
    wrapper.and(
        scope -> {
          scope.isNull("department_id");
          if (!departmentIds.isEmpty()) {
            scope.or(
                departmentScope -> {
                  departmentScope.in("department_id", departmentIds);
                  departmentScope.and(
                      teamScope -> {
                        teamScope.isNull("team_id");
                        if (!teamIds.isEmpty()) {
                          teamScope.or().in("team_id", teamIds);
                        }
                      });
                });
          }
        });
  }

  private void applyApplicableQuestionBankScope(
      QueryWrapper<TrainingExamQuestionBank> wrapper,
      Long companyId,
      Long departmentId,
      Long teamId) {
    ExamOrgScope scope =
        validateExamOrgScope(companyId, departmentId, teamId);
    if (scope.department() == null) {
      wrapper.isNull("department_id").isNull("team_id");
      return;
    }
    wrapper.and(
        visible -> {
          visible.isNull("department_id");
          visible.or(
              departmentVisible -> {
                departmentVisible.eq("department_id", scope.department().id);
                if (scope.team() == null) {
                  departmentVisible.isNull("team_id");
                } else {
                  departmentVisible.and(
                      teamVisible ->
                          teamVisible
                              .isNull("team_id")
                              .or()
                              .eq("team_id", scope.team().id));
                }
              });
        });
  }

  private QueryWrapper<TrainingExamResult> resultWrapper(TrainingExamResultQuery query) {
    TrainingExamResultQuery safe =
        query == null ? new TrainingExamResultQuery(null, null, null, null, null, null, null, null) : query;
    QueryWrapper<TrainingExamResult> wrapper = new QueryWrapper<TrainingExamResult>().eq("deleted", 0);
    dataScopeService.applyCompanyReadScope(wrapper, "company_id");
    dataScopeService.applyDepartmentReadScope(wrapper, "department_id");
    if (safe.companyId() != null) {
      dataScopeService.assertCanReadCompany(safe.companyId());
      wrapper.eq("company_id", safe.companyId());
    }
    if (safe.departmentId() != null) {
      dataScopeService.assertCanReadDepartment(safe.departmentId());
      wrapper.eq("department_id", safe.departmentId());
    }
    if (safe.taskId() != null) {
      wrapper.eq("task_id", safe.taskId());
    }
    if (safe.dateStart() != null) {
      wrapper.ge("exam_date", safe.dateStart());
    }
    if (safe.dateEnd() != null) {
      wrapper.le("exam_date", safe.dateEnd());
    }
    if (!blank(safe.status()) && !"all".equalsIgnoreCase(safe.status())) {
      wrapper.eq("status", TrainingExamResultStatus.parse(safe.status()).name());
    }
    wrapper.orderByDesc("exam_date").orderByDesc("id");
    return wrapper;
  }

  private void applyTaskRequest(TrainingExamTask task, TrainingExamTaskRequest request) {
    if (request == null) {
      throw new BusinessException("考试任务不能为空");
    }
    ExamOrgScope scope =
        validateExamOrgScope(request.companyId(), request.departmentId(), request.teamId());
    if (blank(request.exam())) {
      throw new BusinessException("考试不能为空");
    }
    if (request.examDate() == null) {
      throw new BusinessException("考试日期不能为空");
    }
    task.companyId = scope.company().id;
    task.departmentId = scope.department() == null ? null : scope.department().id;
    task.teamId = scope.team() == null ? null : scope.team().id;
    task.exam = text(request.exam());
    task.examDate = request.examDate();
    int durationMinutes = request.durationMinutes() == null ? 30 : request.durationMinutes();
    if (durationMinutes < 1 || durationMinutes > 480) {
      throw new BusinessException("考试时长应为 1 至 480 分钟");
    }
    task.durationMinutes = durationMinutes;
    task.status = TrainingExamTaskStatus.parse(request.status()).name();
    task.remark = text(request.remark());
  }

  private void applyQuestionBankRequest(TrainingExamQuestionBank question, TrainingExamQuestionBankRequest request) {
    if (request == null) {
      throw new BusinessException("题库试题不能为空");
    }
    ExamOrgScope orgScope =
        validateQuestionBankOrg(
            request.companyId(), request.departmentId(), request.teamId());
    NormalizedBankQuestion normalized =
        normalizeBankQuestion(
            request.questionType(),
            request.questionText(),
            request.options(),
            request.correctAnswers(),
            request.referenceAnswer(),
            request.answerExplanation(),
            request.caseMaterial(),
            request.children(),
            request.allOptions(),
            request.answer(),
            request.score(),
            true,
            false);
    question.companyId = orgScope.company().id;
    question.departmentId =
        orgScope.department() == null ? null : orgScope.department().id;
    question.teamId = orgScope.team() == null ? null : orgScope.team().id;
    question.questionType = normalized.type().name();
    question.questionText = normalized.questionText();
    question.selectedOption = "";
    question.allOptions = legacyOptions(normalized.options());
    question.answer =
        normalized.type() == TrainingExamQuestionType.SHORT_ANSWER
            ? normalized.referenceAnswer()
            : String.join(",", normalized.correctAnswers());
    question.score = normalized.score();
    question.actualScore = BigDecimal.ZERO.setScale(1);
    question.optionsJson = json(normalized.options());
    question.answersJson = json(normalized.correctAnswers());
    question.answerExplanation = normalized.answerExplanation();
    question.caseMaterial = normalized.caseMaterial();
    question.childrenJson = json(normalized.children());
  }

  private void applyPaperRequest(TrainingExamPaper paper, TrainingExamPaperRequest request) {
    if (request == null) {
      throw new BusinessException("考试试卷不能为空");
    }
    ExamOrgScope orgScope =
        validatePaperOrg(
            request.companyId(), request.departmentId(), request.teamId());
    if (blank(request.paperName())) {
      throw new BusinessException("试卷名称不能为空");
    }
    List<TrainingExamQuestionPayload> questions = validateQuestions(request.questions());
    BigDecimal totalScore =
        scale(
            questions.stream()
                .map(TrainingExamQuestionPayload::score)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    paper.companyId =
        orgScope.company() == null ? null : orgScope.company().id;
    paper.departmentId =
        orgScope.department() == null ? null : orgScope.department().id;
    paper.teamId = orgScope.team() == null ? null : orgScope.team().id;
    paper.paperName = text(request.paperName());
    paper.description = text(request.description());
    paper.questionsJson = json(questions);
    paper.questionCount = questions.size();
    paper.totalScore = totalScore;
  }

  private ExamOrgScope validateQuestionBankOrg(
      Long companyId, Long departmentId, Long teamId) {
    return validateExamOrgScope(companyId, departmentId, teamId);
  }

  private ExamOrgScope validateExamOrgScope(
      Long companyId, Long departmentId, Long teamId) {
    SysOrg company = requireOrg(companyId, "COMPANY", "公司不存在");
    SysOrg department =
        departmentId == null
            ? null
            : requireOrg(departmentId, "DEPARTMENT", "部门不存在");
    SysOrg team =
        teamId == null ? null : requireOrg(teamId, "TEAM", "班组不存在");
    if (team != null && department == null) {
      throw new BusinessException("选择班组前请先选择部门");
    }
    if (department != null
        && (department.orgPath == null
            || !department.orgPath.contains("/" + company.id + "/"))) {
      throw new BusinessException("部门不属于所选公司");
    }
    if (team != null
        && (team.orgPath == null
            || !team.orgPath.contains("/" + department.id + "/"))) {
      throw new BusinessException("班组不属于所选部门");
    }
    dataScopeService.assertCanAccessOrg(
        team != null ? team.id : department != null ? department.id : company.id);
    return new ExamOrgScope(company, department, team);
  }

  private ExamOrgScope validatePaperOrg(
      Long companyId, Long departmentId, Long teamId) {
    if (companyId == null && departmentId == null && teamId == null) {
      return new ExamOrgScope(null, null, null);
    }
    if (companyId == null) {
      throw new BusinessException("选择部门前请先选择公司");
    }
    dataScopeService.assertCanAccessOrg(companyId);
    SysOrg company = requireOrg(companyId, "COMPANY", "公司不存在");
    if (departmentId == null) {
      if (teamId != null) {
        throw new BusinessException("选择班组前请先选择部门");
      }
      return new ExamOrgScope(company, null, null);
    }
    dataScopeService.assertCanAccessOrg(departmentId);
    SysOrg department = requireOrg(departmentId, "DEPARTMENT", "部门不存在");
    if (department.orgPath == null || !department.orgPath.contains("/" + company.id + "/")) {
      throw new BusinessException("部门不属于所选公司");
    }
    if (teamId == null) {
      return new ExamOrgScope(company, department, null);
    }
    dataScopeService.assertCanAccessOrg(teamId);
    SysOrg team = requireOrg(teamId, "TEAM", "班组不存在");
    if (team.orgPath == null
        || !team.orgPath.contains("/" + department.id + "/")) {
      throw new BusinessException("班组不属于所选部门");
    }
    return new ExamOrgScope(company, department, team);
  }

  private List<Long> validatePersonIds(List<Long> personIds) {
    List<Long> ids = personIds.stream().distinct().toList();
    ids.forEach(this::requireUser);
    return ids;
  }

  private List<Long> examScopePersonIds(TrainingExamTask task) {
    SysOrg scopeOrg = taskScopeOrg(task);
    List<Long> orgIds =
        orgMapper
            .selectList(
                new QueryWrapper<SysOrg>()
                    .eq("deleted", 0)
                    .likeRight("org_path", scopeOrg.orgPath)
                    .in("org_type", "DEPARTMENT", "TEAM"))
            .stream()
            .map(org -> org.id)
            .toList();
    if (orgIds.isEmpty()) {
      throw new BusinessException("所选考试范围内没有可考试人员");
    }
    List<Long> personIds =
        userMapper
            .selectList(
                new QueryWrapper<SysUser>()
                    .in("org_id", orgIds)
                    .eq("status", "ACTIVE")
                    .eq("deleted", 0)
                    .orderByAsc("id"))
            .stream()
            .map(user -> user.id)
            .toList();
    if (personIds.isEmpty()) {
      throw new BusinessException("所选考试范围内没有可考试人员");
    }
    return personIds;
  }

  private SysOrg taskScopeOrg(TrainingExamTask task) {
    Long orgId =
        task.teamId != null
            ? task.teamId
            : task.departmentId != null ? task.departmentId : task.companyId;
    return requireActiveOrg(orgId, "考试范围组织不存在");
  }

  private List<TrainingExamQuestionPayload> validateQuestions(List<TrainingExamQuestionPayload> questions) {
    if (questions == null || questions.isEmpty()) {
      throw new BusinessException("考题不能为空");
    }
    List<TrainingExamQuestionPayload> normalized = new ArrayList<>();
    for (int i = 0; i < questions.size(); i++) {
      normalized.add(validateQuestion(questions.get(i), i));
    }
    return normalized;
  }

  private TrainingExamQuestionPayload validateQuestion(TrainingExamQuestionPayload question, int index) {
    if (question == null) {
      throw new BusinessException("考题不能为空");
    }
    if (blank(question.questionType())) {
      throw new BusinessException("题型不能为空");
    }
    if (blank(question.questionText())) {
      throw new BusinessException("考题不能为空");
    }
    if (hasStructuredQuestion(question)) {
      NormalizedBankQuestion normalized =
          normalizeBankQuestion(
              question.questionType(),
              question.questionText(),
              question.options(),
              question.correctAnswers(),
              question.referenceAnswer(),
              question.answerExplanation(),
              question.caseMaterial(),
              question.children(),
              question.allOptions(),
              question.answer(),
              question.score(),
              true,
              false);
      BigDecimal actualScore =
          question.actualScore() == null ? BigDecimal.ZERO : question.actualScore();
      validateScore(actualScore, "实际分");
      return new TrainingExamQuestionPayload(
          normalized.type().name(),
          normalized.questionText(),
          text(question.selectedOption()),
          legacyOptions(normalized.options()),
          normalized.type() == TrainingExamQuestionType.SHORT_ANSWER
              ? normalized.referenceAnswer()
              : String.join(",", normalized.correctAnswers()),
          normalized.score(),
          scale(actualScore),
          question.sortOrder() == null ? index : question.sortOrder(),
          normalized.options(),
          normalized.correctAnswers(),
          normalized.referenceAnswer(),
          normalized.answerExplanation(),
          normalized.caseMaterial(),
          normalized.children());
    }
    BigDecimal score = question.score() == null ? BigDecimal.ZERO : question.score();
    BigDecimal actualScore = question.actualScore() == null ? BigDecimal.ZERO : question.actualScore();
    validateScore(score, "分值");
    validateScore(actualScore, "实际分");
    return new TrainingExamQuestionPayload(
        text(question.questionType()),
        text(question.questionText()),
        text(question.selectedOption()),
        text(question.allOptions()),
        text(question.answer()),
        scale(score),
        scale(actualScore),
        question.sortOrder() == null ? index : question.sortOrder());
  }

  private boolean hasStructuredQuestion(TrainingExamQuestionPayload question) {
    return (question.options() != null && !question.options().isEmpty())
        || (question.correctAnswers() != null && !question.correctAnswers().isEmpty())
        || !blank(question.referenceAnswer())
        || !blank(question.answerExplanation())
        || !blank(question.caseMaterial())
        || (question.children() != null && !question.children().isEmpty());
  }

  private TrainingExamTask requireTask(Long id) {
    TrainingExamTask task = taskMapper.selectById(id);
    if (task == null || Integer.valueOf(1).equals(task.deleted)) {
      throw new BusinessException("考试任务不存在");
    }
    dataScopeService.assertCanAccessOrg(
        task.teamId != null
            ? task.teamId
            : task.departmentId != null ? task.departmentId : task.companyId);
    return task;
  }

  private TrainingExamTask requireTaskForRead(Long id) {
    TrainingExamTask task = taskMapper.selectById(id);
    if (task == null || Integer.valueOf(1).equals(task.deleted)) {
      throw new BusinessException("考试任务不存在");
    }
    dataScopeService.assertCanReadCompany(task.companyId);
    if (task.departmentId != null) {
      dataScopeService.assertCanReadDepartment(task.departmentId);
    }
    return task;
  }

  private TrainingExamQuestionBank requireQuestionBank(Long id) {
    TrainingExamQuestionBank question = questionBankMapper.selectById(id);
    if (question == null || Integer.valueOf(1).equals(question.deleted)) {
      throw new BusinessException("题库试题不存在");
    }
    dataScopeService.assertCanAccessOrg(
        question.teamId != null
            ? question.teamId
            : question.departmentId != null
                ? question.departmentId
                : question.companyId);
    return question;
  }

  private TrainingExamPaper requirePaper(Long id) {
    TrainingExamPaper paper = paperMapper.selectById(id);
    if (paper == null || Integer.valueOf(1).equals(paper.deleted)) {
      throw new BusinessException("考试试卷不存在");
    }
    if (paper.companyId == null) {
      CurrentUser currentUser = CurrentUserContext.require();
      if (!currentUser.isAdmin() && !currentUser.userId().equals(paper.createdBy)) {
        throw new SecurityException("仅试卷创建人或管理员可以修改全局共享试卷");
      }
    } else {
      dataScopeService.assertCanAccessOrg(
          paper.teamId != null
              ? paper.teamId
              : paper.departmentId != null
                  ? paper.departmentId
                  : paper.companyId);
    }
    return paper;
  }

  private TrainingExamResult requireResult(Long id) {
    TrainingExamResult result = resultMapper.selectById(id);
    if (result == null || Integer.valueOf(1).equals(result.deleted)) {
      throw new BusinessException("考试成绩不存在");
    }
    dataScopeService.assertCanAccessOrg(result.companyId);
    return result;
  }

  private TrainingExamResult requireResultForRead(Long id) {
    TrainingExamResult result = resultMapper.selectById(id);
    if (result == null || Integer.valueOf(1).equals(result.deleted)) {
      throw new BusinessException("考试成绩不存在");
    }
    dataScopeService.assertCanReadCompany(result.companyId);
    if (result.departmentId != null) {
      dataScopeService.assertCanReadDepartment(result.departmentId);
    }
    return result;
  }

  private void assertTaskHasNoResults(Long taskId) {
    Long count =
        resultMapper.selectCount(new QueryWrapper<TrainingExamResult>().eq("task_id", taskId).eq("deleted", 0));
    if (count != null && count > 0) {
      throw new BusinessException("该考试任务下存在考试成绩，请先删除关联成绩");
    }
  }

  private SysOrg requireOrg(Long id, String orgType, String message) {
    SysOrg org = id == null ? null : orgMapper.selectById(id);
    if (org == null || Integer.valueOf(1).equals(org.deleted) || !orgType.equals(org.orgType)) {
      throw new BusinessException(message);
    }
    return org;
  }

  private SysOrg requireActiveOrg(Long id, String message) {
    SysOrg org = id == null ? null : orgMapper.selectById(id);
    if (org == null || Integer.valueOf(1).equals(org.deleted)) {
      throw new BusinessException(message);
    }
    return org;
  }

  private SysOrg requireAncestor(
      SysOrg org, String orgType, String message) {
    if (org == null || org.orgPath == null) {
      throw new BusinessException(message);
    }
    for (String pathId : org.orgPath.split("/")) {
      if (pathId.isBlank()) {
        continue;
      }
      SysOrg ancestor = orgMapper.selectById(Long.valueOf(pathId));
      if (ancestor != null
          && !Integer.valueOf(1).equals(ancestor.deleted)
          && orgType.equals(ancestor.orgType)) {
        return ancestor;
      }
    }
    throw new BusinessException(message);
  }

  private boolean isDescendantOrSelf(SysOrg org, SysOrg scope) {
    return org != null
        && scope != null
        && org.orgPath != null
        && scope.orgPath != null
        && org.orgPath.startsWith(scope.orgPath);
  }

  private boolean orgsOverlap(SysOrg first, SysOrg second) {
    return isDescendantOrSelf(first, second)
        || isDescendantOrSelf(second, first);
  }

  private SysUser requireUser(Long id) {
    SysUser user = userMapper.selectById(id);
    if (user == null || Integer.valueOf(1).equals(user.deleted) || !"ACTIVE".equals(user.status)) {
      throw new BusinessException("考试人员不存在");
    }
    dataScopeService.assertCanAccessOrg(user.orgId);
    return user;
  }

  private TrainingExamTaskResponse taskResponse(TrainingExamTask task) {
    TrainingExamTaskStatus status = TrainingExamTaskStatus.parse(task.status);
    return new TrainingExamTaskResponse(
        task.id,
        task.code,
        task.companyId,
        orgName(task.companyId),
        task.departmentId,
        task.departmentId == null ? "全部部门" : orgName(task.departmentId),
        task.teamId,
        task.teamId == null ? "全部班组" : orgName(task.teamId),
        task.exam,
        task.examDate,
        task.durationMinutes,
        status.name(),
        status.label(),
        task.remark,
        task.createdAt,
        task.updatedAt);
  }

  private TrainingExamResultResponse resultResponse(TrainingExamResult result) {
    TrainingExamTask task = requireTaskForRead(result.taskId);
    TrainingExamResultStatus status = TrainingExamResultStatus.parse(result.status);
    return new TrainingExamResultResponse(
        result.id,
        result.code,
        result.taskId,
        result.companyId,
        orgName(result.companyId),
        result.departmentId,
        orgName(result.departmentId),
        result.examPersonUserId,
        result.examPersonName,
        task.exam,
        result.score,
        result.examDate,
        status.name(),
        status.label(),
        result.remark,
        result.createdAt,
        result.updatedAt);
  }

  private TrainingExamQuestionBankResponse questionBankResponse(TrainingExamQuestionBank question) {
    TrainingExamQuestionType type = storedQuestionType(question.questionType);
    List<TrainingExamQuestionOption> options =
        readJson(question.optionsJson, new TypeReference<List<TrainingExamQuestionOption>>() {}, parseLegacyOptions(question.allOptions));
    List<String> answers =
        readJson(question.answersJson, new TypeReference<List<String>>() {}, parseLegacyAnswers(question.answer));
    List<TrainingExamQuestionBankChild> children =
        readJson(question.childrenJson, new TypeReference<List<TrainingExamQuestionBankChild>>() {}, List.of());
    return new TrainingExamQuestionBankResponse(
        question.id,
        question.companyId,
        orgName(question.companyId),
        question.departmentId,
        question.departmentId == null ? "全部部门" : orgName(question.departmentId),
        question.teamId,
        question.teamId == null ? "全部班组" : orgName(question.teamId),
        type == null ? question.questionType : type.name(),
        type == null ? question.questionType : type.label(),
        question.questionText,
        question.selectedOption,
        question.allOptions,
        question.answer,
        question.score,
        question.actualScore,
        options,
        answers,
        type == TrainingExamQuestionType.SHORT_ANSWER ? question.answer : "",
        text(question.answerExplanation),
        text(question.caseMaterial),
        children,
        question.createdAt,
        question.updatedAt);
  }

  private TrainingExamPaperResponse paperResponse(TrainingExamPaper paper) {
    List<TrainingExamQuestionPayload> questions =
        readJson(
            paper.questionsJson,
            new TypeReference<List<TrainingExamQuestionPayload>>() {},
            List.of());
    return new TrainingExamPaperResponse(
        paper.id,
        paper.companyId,
        paper.companyId == null ? "全局共享" : orgName(paper.companyId),
        paper.departmentId,
        paper.departmentId == null ? "全部部门" : orgName(paper.departmentId),
        paper.teamId,
        paper.teamId == null ? "全部班组" : orgName(paper.teamId),
        paper.paperName,
        text(paper.description),
        paper.questionCount,
        paper.totalScore,
        questions,
        paper.createdAt,
        paper.updatedAt);
  }

  private TrainingExamQuestionBank questionBankQuestion(
      ExamOrgScope orgScope, TrainingExamQuestionPayload payload, Long userId) {
    TrainingExamQuestionBank question = new TrainingExamQuestionBank();
    question.companyId = orgScope.company().id;
    question.departmentId =
        orgScope.department() == null ? null : orgScope.department().id;
    question.teamId = orgScope.team() == null ? null : orgScope.team().id;
    question.questionType = payload.questionType();
    question.questionText = payload.questionText();
    question.selectedOption = payload.selectedOption();
    question.allOptions = payload.allOptions();
    question.answer = payload.answer();
    question.score = payload.score();
    question.actualScore = payload.actualScore();
    question.optionsJson = json(payload.options());
    question.answersJson = json(payload.correctAnswers());
    question.answerExplanation = text(payload.answerExplanation());
    question.caseMaterial = text(payload.caseMaterial());
    question.childrenJson = json(payload.children());
    question.createdBy = userId;
    question.updatedBy = userId;
    return question;
  }

  private TrainingExamQuestion taskQuestion(
      Long taskId, TrainingExamQuestionPayload payload, int index, Long userId) {
    TrainingExamQuestion question = new TrainingExamQuestion();
    question.taskId = taskId;
    question.questionType = payload.questionType();
    question.questionText = payload.questionText();
    question.selectedOption = payload.selectedOption();
    question.allOptions = payload.allOptions();
    question.answer = payload.answer();
    question.score = payload.score();
    question.actualScore = payload.actualScore();
    question.optionsJson = json(payload.options());
    question.answersJson = json(payload.correctAnswers());
    question.answerExplanation = text(payload.answerExplanation());
    question.caseMaterial = text(payload.caseMaterial());
    question.childrenJson = json(payload.children());
    question.sortOrder = payload.sortOrder() == null ? index : payload.sortOrder();
    question.createdBy = userId;
    question.updatedBy = userId;
    return question;
  }

  private void copyQuestionsToResult(
      Long resultId, List<TrainingExamQuestion> questions, Long userId, boolean pending) {
    for (TrainingExamQuestion question : questions) {
      TrainingExamResultQuestion copy = new TrainingExamResultQuestion();
      copy.resultId = resultId;
      copy.taskQuestionId = question.id;
      copy.questionType = question.questionType;
      copy.questionText = question.questionText;
      copy.selectedOption = pending ? null : question.selectedOption;
      copy.allOptions = question.allOptions;
      copy.answer = question.answer;
      copy.score = question.score;
      copy.actualScore = pending ? BigDecimal.ZERO.setScale(1) : question.actualScore;
      copy.optionsJson = question.optionsJson;
      copy.answersJson = question.answersJson;
      copy.answerExplanation = question.answerExplanation;
      copy.caseMaterial = question.caseMaterial;
      copy.childrenJson = question.childrenJson;
      copy.sortOrder = question.sortOrder;
      copy.createdBy = userId;
      copy.updatedBy = userId;
      resultQuestionMapper.insert(copy);
    }
  }

  private List<TrainingExamQuestion> taskQuestionEntities(Long taskId) {
    return questionMapper.selectList(
        new QueryWrapper<TrainingExamQuestion>()
            .eq("task_id", taskId)
            .eq("deleted", 0)
            .orderByAsc("sort_order")
            .orderByAsc("id"));
  }

  private List<TrainingExamQuestionResponse> taskQuestions(Long taskId) {
    return taskQuestionEntities(taskId).stream().map(this::questionResponse).toList();
  }

  private List<TrainingExamQuestionResponse> resultQuestions(Long resultId) {
    return resultQuestionMapper
        .selectList(
            new QueryWrapper<TrainingExamResultQuestion>()
                .eq("result_id", resultId)
                .eq("deleted", 0)
                .orderByAsc("sort_order")
                .orderByAsc("id"))
        .stream()
        .map(this::questionResponse)
        .toList();
  }

  private List<TrainingExamResultQuestion> resultQuestionEntities(Long resultId) {
    return resultQuestionMapper.selectList(
        new QueryWrapper<TrainingExamResultQuestion>()
            .eq("result_id", resultId)
            .eq("deleted", 0)
            .orderByAsc("sort_order")
            .orderByAsc("id"));
  }

  private TrainingExamResult requireMyResult(Long taskId) {
    if (taskId == null) {
      throw new BusinessException("考试任务不能为空");
    }
    CurrentUser user = CurrentUserContext.require();
    List<TrainingExamResult> results =
        resultMapper.selectList(
            new QueryWrapper<TrainingExamResult>()
                .eq("task_id", taskId)
                .eq("exam_person_user_id", user.userId())
                .eq("deleted", 0)
                .orderByDesc("id"));
    if (results.isEmpty()) {
      throw new SecurityException("无权访问该考试任务");
    }
    return results.get(0);
  }

  private TrainingExamTask requireAssignedTask(Long taskId) {
    TrainingExamTask task = taskMapper.selectById(taskId);
    if (task == null || Integer.valueOf(1).equals(task.deleted)) {
      throw new BusinessException("考试任务不存在");
    }
    return task;
  }

  private MiniExamResponse miniExam(TrainingExamResult result, boolean includeQuestions) {
    TrainingExamTask task = requireAssignedTask(result.taskId);
    TrainingExamResultStatus status = TrainingExamResultStatus.parse(result.status);
    boolean submitted = status != TrainingExamResultStatus.PENDING_EXAM;
    List<MiniExamQuestionResponse> questions =
        includeQuestions
            ? resultQuestionEntities(result.id).stream()
                .flatMap(question -> miniExamQuestions(question, submitted).stream())
                .toList()
            : List.of();
    List<MiniExamQuestionResponse> progressQuestions =
        resultQuestionEntities(result.id).stream()
            .flatMap(question -> miniExamQuestions(question, submitted).stream())
            .toList();
    int answeredCount =
        (int) progressQuestions.stream().filter(question -> !blank(question.selectedOption())).count();
    int correctCount =
        submitted
            ? (int)
                progressQuestions.stream()
                    .filter(
                        question ->
                            question.actualScore() != null
                                && question.score() != null
                                && question.actualScore().compareTo(question.score()) >= 0)
                    .count()
            : 0;
    int incorrectCount =
        submitted
            ? (int)
                progressQuestions.stream()
                    .filter(
                        question ->
                            question.actualScore() != null
                                && question.score() != null
                                && question.actualScore().compareTo(question.score()) < 0)
                    .count()
            : 0;
    int elapsedSeconds =
        result.startedAt == null
            ? 0
            : (int)
                Math.max(
                    0,
                    java.time.Duration.between(
                            result.startedAt,
                            result.submittedAt == null ? LocalDateTime.now() : result.submittedAt)
                        .getSeconds());
    return new MiniExamResponse(
        task.id,
        result.id,
        result.code,
        orgName(result.companyId),
        orgName(result.departmentId),
        result.examPersonName,
        task.exam,
        task.examDate,
        task.durationMinutes,
        result.currentQuestionIndex == null ? 0 : result.currentQuestionIndex,
        result.remainingSeconds == null ? task.durationMinutes * 60 : result.remainingSeconds,
        answeredCount,
        correctCount,
        incorrectCount,
        elapsedSeconds,
        status.name(),
        status.label(),
        status == TrainingExamResultStatus.EXAMED ? result.score : null,
        result.submittedAt,
        questions);
  }

  private List<MiniExamQuestionResponse> miniExamQuestions(
      TrainingExamResultQuestion question, boolean submitted) {
    TrainingExamQuestionType type = storedQuestionType(question.questionType);
    if (type == TrainingExamQuestionType.CASE_ANALYSIS) {
      List<TrainingExamQuestionBankChild> children = resultQuestionChildren(question);
      Map<String, String> selected =
          readJson(
              question.selectedOption,
              new TypeReference<Map<String, String>>() {},
              Map.of());
      List<MiniExamQuestionResponse> responses = new ArrayList<>();
      for (int index = 0; index < children.size(); index++) {
        TrainingExamQuestionBankChild child = children.get(index);
        TrainingExamQuestionType childType = TrainingExamQuestionType.parse(child.questionType());
        List<String> correctAnswers =
            child.correctAnswers() == null ? List.of() : child.correctAnswers();
        String selectedAnswer = selected.get(String.valueOf(index));
        BigDecimal childActualScore = null;
        if (submitted && childType != TrainingExamQuestionType.SHORT_ANSWER) {
          childActualScore =
              normalizeAnswer(String.join(",", correctAnswers))
                      .equals(normalizeAnswer(selectedAnswer))
                  ? child.score()
                  : BigDecimal.ZERO.setScale(1);
        }
        responses.add(
            new MiniExamQuestionResponse(
                question.id,
                miniQuestionKey(question.id, index),
                childType.name(),
                childType.label(),
                child.questionText(),
                legacyOptions(child.options()),
                child.options() == null ? List.of() : child.options(),
                child.score(),
                (question.sortOrder == null ? 0 : question.sortOrder) * 1000 + index,
                question.questionText,
                text(question.caseMaterial),
                selectedAnswer,
                submitted ? String.join(",", correctAnswers) : null,
                submitted ? correctAnswers : List.of(),
                submitted && childType == TrainingExamQuestionType.SHORT_ANSWER
                    ? text(child.referenceAnswer())
                    : null,
                submitted ? text(child.answerExplanation()) : null,
                childActualScore));
      }
      return responses;
    }

    List<TrainingExamQuestionOption> options =
        readJson(
            question.optionsJson,
            new TypeReference<List<TrainingExamQuestionOption>>() {},
            parseLegacyOptions(question.allOptions));
    List<String> correctAnswers =
        readJson(
            question.answersJson,
            new TypeReference<List<String>>() {},
            parseLegacyAnswers(question.answer));
    return List.of(
        new MiniExamQuestionResponse(
            question.id,
            miniQuestionKey(question.id, null),
            type == null ? question.questionType : type.name(),
            type == null ? question.questionType : type.label(),
            question.questionText,
            question.allOptions,
            options,
            question.score,
            question.sortOrder,
            null,
            null,
            question.selectedOption,
            submitted ? question.answer : null,
            submitted ? correctAnswers : List.of(),
            submitted && type == TrainingExamQuestionType.SHORT_ANSWER ? question.answer : null,
            submitted ? text(question.answerExplanation) : null,
            submitted ? question.actualScore : null));
  }

  private List<TrainingExamQuestionBankChild> resultQuestionChildren(
      TrainingExamResultQuestion question) {
    return readJson(
        question.childrenJson,
        new TypeReference<List<TrainingExamQuestionBankChild>>() {},
        List.of());
  }

  private int miniAnswerCount(TrainingExamResultQuestion question) {
    return storedQuestionType(question.questionType) == TrainingExamQuestionType.CASE_ANALYSIS
        ? resultQuestionChildren(question).size()
        : 1;
  }

  private boolean hasAllMiniAnswers(
      TrainingExamResultQuestion question, Map<String, String> answers) {
    if (storedQuestionType(question.questionType) != TrainingExamQuestionType.CASE_ANALYSIS) {
      return answers.containsKey(miniQuestionKey(question.id, null));
    }
    int childCount = resultQuestionChildren(question).size();
    for (int index = 0; index < childCount; index++) {
      if (!answers.containsKey(miniQuestionKey(question.id, index))) {
        return false;
      }
    }
    return true;
  }

  private String miniQuestionKey(Long questionId, Integer childIndex) {
    return childIndex == null ? String.valueOf(questionId) : questionId + ":" + childIndex;
  }

  private Map<String, String> answerMap(List<MiniExamAnswerRequest> answers) {
    return answerMap(answers, false);
  }

  private Map<String, String> answerMap(
      List<MiniExamAnswerRequest> answers, boolean allowEmpty) {
    if (answers == null || answers.isEmpty()) {
      if (allowEmpty) {
        return Map.of();
      }
      throw new BusinessException("答案不能为空");
    }
    Map<String, String> mapped = new HashMap<>();
    for (MiniExamAnswerRequest answer : answers) {
      String key =
          answer == null
              ? null
              : !blank(answer.questionKey())
                  ? answer.questionKey().trim()
                  : answer.questionId() == null ? null : String.valueOf(answer.questionId());
      if (blank(key) || blank(answer.selectedOption())) {
        throw new BusinessException("答案不能为空");
      }
      if (mapped.put(key, answer.selectedOption().trim()) != null) {
        throw new BusinessException("题目答案重复");
      }
    }
    return mapped;
  }

  private void saveSelectedAnswers(
      TrainingExamResult result, List<MiniExamAnswerRequest> answers) {
    Map<String, String> mapped = answerMap(answers, true);
    if (mapped.isEmpty()) {
      return;
    }
    LocalDateTime updatedAt = LocalDateTime.now();
    for (TrainingExamResultQuestion question : resultQuestionEntities(result.id)) {
      if (storedQuestionType(question.questionType) == TrainingExamQuestionType.CASE_ANALYSIS) {
        Map<String, String> selected =
            readJson(
                question.selectedOption,
                new TypeReference<Map<String, String>>() {},
                new java.util.LinkedHashMap<>());
        for (int index = 0; index < resultQuestionChildren(question).size(); index++) {
          String answer = mapped.get(miniQuestionKey(question.id, index));
          if (answer != null) {
            selected.put(String.valueOf(index), answer);
          }
        }
        question.selectedOption = json(selected);
      } else {
        String answer = mapped.get(miniQuestionKey(question.id, null));
        if (answer == null) {
          continue;
        }
        question.selectedOption = answer;
      }
      question.updatedBy = CurrentUserContext.require().userId();
      question.updatedAt = updatedAt;
      resultQuestionMapper.updateById(question);
    }
  }

  private String normalizeAnswer(String value) {
    if (value == null) return "";
    return java.util.Arrays.stream(value.toUpperCase().split("[,，;；|、\\s]+"))
        .map(String::trim)
        .filter(part -> !part.isEmpty())
        .sorted()
        .distinct()
        .collect(java.util.stream.Collectors.joining(","));
  }

  private TrainingExamQuestionResponse questionResponse(TrainingExamQuestion question) {
    TrainingExamQuestionType type = storedQuestionType(question.questionType);
    return new TrainingExamQuestionResponse(
        question.id,
        type == null ? question.questionType : type.name(),
        question.questionText,
        question.selectedOption,
        question.allOptions,
        question.answer,
        question.score,
        question.actualScore,
        question.sortOrder,
        type == null ? question.questionType : type.label(),
        readJson(
            question.optionsJson,
            new TypeReference<List<TrainingExamQuestionOption>>() {},
            parseLegacyOptions(question.allOptions)),
        readJson(
            question.answersJson,
            new TypeReference<List<String>>() {},
            parseLegacyAnswers(question.answer)),
        type == TrainingExamQuestionType.SHORT_ANSWER ? question.answer : "",
        text(question.answerExplanation),
        text(question.caseMaterial),
        readJson(
            question.childrenJson,
            new TypeReference<List<TrainingExamQuestionBankChild>>() {},
            List.of()));
  }

  private TrainingExamQuestionResponse questionResponse(TrainingExamQuestionBank question) {
    TrainingExamQuestionType type = storedQuestionType(question.questionType);
    return new TrainingExamQuestionResponse(
        question.id,
        type == null ? question.questionType : type.name(),
        question.questionText,
        question.selectedOption,
        question.allOptions,
        question.answer,
        question.score,
        question.actualScore,
        0,
        type == null ? question.questionType : type.label(),
        readJson(
            question.optionsJson,
            new TypeReference<List<TrainingExamQuestionOption>>() {},
            parseLegacyOptions(question.allOptions)),
        readJson(
            question.answersJson,
            new TypeReference<List<String>>() {},
            parseLegacyAnswers(question.answer)),
        type == TrainingExamQuestionType.SHORT_ANSWER ? question.answer : "",
        text(question.answerExplanation),
        text(question.caseMaterial),
        readJson(
            question.childrenJson,
            new TypeReference<List<TrainingExamQuestionBankChild>>() {},
            List.of()));
  }

  private TrainingExamQuestionResponse questionResponse(TrainingExamResultQuestion question) {
    TrainingExamQuestionType type = storedQuestionType(question.questionType);
    return new TrainingExamQuestionResponse(
        question.id,
        type == null ? question.questionType : type.name(),
        question.questionText,
        question.selectedOption,
        question.allOptions,
        question.answer,
        question.score,
        question.actualScore,
        question.sortOrder,
        type == null ? question.questionType : type.label(),
        readJson(
            question.optionsJson,
            new TypeReference<List<TrainingExamQuestionOption>>() {},
            parseLegacyOptions(question.allOptions)),
        readJson(
            question.answersJson,
            new TypeReference<List<String>>() {},
            parseLegacyAnswers(question.answer)),
        type == TrainingExamQuestionType.SHORT_ANSWER ? question.answer : "",
        text(question.answerExplanation),
        text(question.caseMaterial),
        readJson(
            question.childrenJson,
            new TypeReference<List<TrainingExamQuestionBankChild>>() {},
            List.of()));
  }

  private TrainingExamQuestionResponse questionResponse(Long id, TrainingExamQuestionPayload payload, int sortOrder) {
    return new TrainingExamQuestionResponse(
        id,
        payload.questionType(),
        payload.questionText(),
        payload.selectedOption(),
        payload.allOptions(),
        payload.answer(),
        payload.score(),
        payload.actualScore(),
        payload.sortOrder() == null ? sortOrder : payload.sortOrder(),
        storedQuestionType(payload.questionType()) == null
            ? payload.questionType()
            : storedQuestionType(payload.questionType()).label(),
        payload.options() == null ? List.of() : payload.options(),
        payload.correctAnswers() == null ? List.of() : payload.correctAnswers(),
        text(payload.referenceAnswer()),
        text(payload.answerExplanation()),
        text(payload.caseMaterial()),
        payload.children() == null ? List.of() : payload.children());
  }

  private void softDeleteTasks(List<Long> ids) {
    taskMapper.update(
        null,
        new UpdateWrapper<TrainingExamTask>()
            .in("id", ids)
            .set("deleted", 1)
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
    questionMapper.update(
        null,
        new UpdateWrapper<TrainingExamQuestion>()
            .in("task_id", ids)
            .set("deleted", 1)
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
  }

  private void softDeleteResults(List<Long> ids) {
    resultMapper.update(
        null,
        new UpdateWrapper<TrainingExamResult>()
            .in("id", ids)
            .set("deleted", 1)
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
    resultQuestionMapper.update(
        null,
        new UpdateWrapper<TrainingExamResultQuestion>()
            .in("result_id", ids)
            .set("deleted", 1)
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
  }

  private void softDeletePapers(List<Long> ids) {
    paperMapper.update(
        null,
        new UpdateWrapper<TrainingExamPaper>()
            .in("id", ids)
            .set("deleted", 1)
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
  }

  private List<Long> safeIds(TrainingExamBatchDeleteRequest request) {
    if (request == null || request.ids() == null || request.ids().isEmpty()) {
      throw new BusinessException("请选择要删除的数据");
    }
    return request.ids().stream().distinct().toList();
  }

  private BigDecimal totalActualScore(List<TrainingExamQuestion> questions) {
    return scale(
        questions.stream()
            .map((question) -> question.actualScore == null ? BigDecimal.ZERO : question.actualScore)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  private String resultStatusForScore(BigDecimal score) {
    BigDecimal safeScore = score == null ? BigDecimal.ZERO : score;
    return safeScore.compareTo(BigDecimal.ZERO) > 0
        ? TrainingExamResultStatus.EXAMED.name()
        : TrainingExamResultStatus.PENDING_EXAM.name();
  }

  private void validateScore(BigDecimal score, String label) {
    if (score == null) {
      throw new BusinessException(label + "不能为空");
    }
    if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
      throw new BusinessException(label + "需在0到100之间");
    }
  }

  private NormalizedBankQuestion normalizeBankQuestion(
      String questionType,
      String questionText,
      List<TrainingExamQuestionOption> requestedOptions,
      List<String> requestedAnswers,
      String referenceAnswer,
      String answerExplanation,
      String caseMaterial,
      List<TrainingExamQuestionBankChild> requestedChildren,
      String legacyOptions,
      String legacyAnswer,
      BigDecimal requestedScore,
      boolean allowCase,
      boolean allowFiveOptionSingle) {
    TrainingExamQuestionType type = TrainingExamQuestionType.parse(questionType);
    if (!allowCase && type == TrainingExamQuestionType.CASE_ANALYSIS) {
      throw new BusinessException("案例分析题的子题不能继续包含案例分析题");
    }
    if (blank(questionText)) {
      throw new BusinessException(type == TrainingExamQuestionType.CASE_ANALYSIS ? "案例标题不能为空" : "考题不能为空");
    }
    List<TrainingExamQuestionOption> options =
        normalizeOptions(
            requestedOptions == null || requestedOptions.isEmpty()
                ? parseLegacyOptions(legacyOptions)
                : requestedOptions);
    List<String> answers =
        normalizeAnswers(
            requestedAnswers == null || requestedAnswers.isEmpty()
                ? parseLegacyAnswers(legacyAnswer)
                : requestedAnswers);
    BigDecimal score = requestedScore == null ? BigDecimal.ZERO : requestedScore;
    validateScore(score, "分值");

    if (type == TrainingExamQuestionType.CASE_ANALYSIS) {
      if (blank(caseMaterial)) {
        throw new BusinessException("案例材料不能为空");
      }
      if (requestedChildren == null || requestedChildren.isEmpty()) {
        throw new BusinessException("案例分析题至少需要一道子题");
      }
      List<TrainingExamQuestionBankChild> children = new ArrayList<>();
      BigDecimal total = BigDecimal.ZERO;
      for (int index = 0; index < requestedChildren.size(); index++) {
        TrainingExamQuestionBankChild child = requestedChildren.get(index);
        if (child == null) {
          throw new BusinessException("第" + (index + 1) + "道案例子题不能为空");
        }
        NormalizedBankQuestion normalized =
            normalizeBankQuestion(
                child.questionType(),
                child.questionText(),
                child.options(),
                child.correctAnswers(),
                child.referenceAnswer(),
                child.answerExplanation(),
                "",
                List.of(),
                "",
                "",
                child.score(),
                false,
                true);
        children.add(toChild(normalized));
        total = total.add(normalized.score());
      }
      validateScore(total, "案例总分");
      return new NormalizedBankQuestion(
          type,
          text(questionText),
          List.of(),
          List.of(),
          "",
          text(answerExplanation),
          text(caseMaterial),
          scale(total),
          children);
    }

    if (requestedChildren != null && !requestedChildren.isEmpty()) {
      throw new BusinessException("只有案例分析题可以包含子题");
    }
    if (score.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException("分值必须大于0");
    }
    if (type == TrainingExamQuestionType.SHORT_ANSWER) {
      String normalizedReference = blank(referenceAnswer) ? text(legacyAnswer) : text(referenceAnswer);
      if (blank(normalizedReference)) {
        throw new BusinessException("问答题参考答案不能为空");
      }
      return new NormalizedBankQuestion(
          type,
          text(questionText),
          List.of(),
          List.of(),
          normalizedReference,
          text(answerExplanation),
          "",
          scale(score),
          List.of());
    }

    boolean fiveOptionSingle =
        type == TrainingExamQuestionType.SINGLE_CHOICE
            && allowFiveOptionSingle
            && options.size() == 5;
    boolean fiveOptionMultiple =
        type == TrainingExamQuestionType.MULTIPLE_CHOICE && options.size() == 5;
    List<String> expectedKeys =
        (type == TrainingExamQuestionType.SINGLE_CHOICE && !fiveOptionSingle)
                || (type == TrainingExamQuestionType.MULTIPLE_CHOICE && !fiveOptionMultiple)
            ? List.of("A", "B", "C", "D")
            : List.of("A", "B", "C", "D", "E");
    if (options.size() != expectedKeys.size()
        || !options.stream().map(TrainingExamQuestionOption::key).toList().equals(expectedKeys)) {
      throw new BusinessException(
          type == TrainingExamQuestionType.SINGLE_CHOICE
              ? allowFiveOptionSingle
                  ? "案例单选子题必须完整填写A-D四个选项或A-E五个选项"
                  : "单选题必须完整填写A、B、C、D四个选项"
              : "多选题必须完整填写A-D四个选项或A-E五个选项");
    }
    if (options.stream().anyMatch(option -> blank(option.content()))) {
      throw new BusinessException("选项内容不能为空");
    }
    if (type == TrainingExamQuestionType.SINGLE_CHOICE && answers.size() != 1) {
      throw new BusinessException("单选题必须且只能设置一个答案");
    }
    if (type == TrainingExamQuestionType.MULTIPLE_CHOICE && answers.size() < 2) {
      throw new BusinessException("多选题至少需要设置两个答案");
    }
    if (!expectedKeys.containsAll(answers)) {
      throw new BusinessException("答案必须属于当前题目的选项");
    }
    return new NormalizedBankQuestion(
        type,
        text(questionText),
        options,
        answers,
        "",
        text(answerExplanation),
        "",
        scale(score),
        List.of());
  }

  private TrainingExamQuestionBankChild toChild(NormalizedBankQuestion question) {
    return new TrainingExamQuestionBankChild(
        question.type().name(),
        question.type().label(),
        question.questionText(),
        question.options(),
        question.correctAnswers(),
        question.referenceAnswer(),
        question.answerExplanation(),
        question.score());
  }

  private List<TrainingExamQuestionOption> normalizeOptions(List<TrainingExamQuestionOption> options) {
    if (options == null) {
      return List.of();
    }
    List<TrainingExamQuestionOption> normalized = new ArrayList<>();
    Set<String> keys = new java.util.HashSet<>();
    for (TrainingExamQuestionOption option : options) {
      if (option == null || blank(option.key())) {
        throw new BusinessException("选项标识不能为空");
      }
      String key = option.key().trim().toUpperCase();
      if (!keys.add(key)) {
        throw new BusinessException("选项标识不能重复");
      }
      normalized.add(new TrainingExamQuestionOption(key, text(option.content())));
    }
    return normalized;
  }

  private List<String> normalizeAnswers(List<String> answers) {
    if (answers == null) {
      return List.of();
    }
    return answers.stream()
        .filter(answer -> !blank(answer))
        .map(answer -> answer.trim().toUpperCase())
        .distinct()
        .sorted()
        .toList();
  }

  private List<TrainingExamQuestionOption> parseLegacyOptions(String value) {
    if (blank(value)) {
      return List.of();
    }
    List<TrainingExamQuestionOption> options = new ArrayList<>();
    String[] parts = value.split("[\\r\\n、,，;；|]+");
    for (String part : parts) {
      String token = part.trim();
      if (token.isEmpty()) {
        continue;
      }
      java.util.regex.Matcher matcher =
          java.util.regex.Pattern.compile("^([A-Ea-e])(?:[.．、:：]\\s*)?(.*)$").matcher(token);
      if (matcher.matches()) {
        String content = matcher.group(2).trim();
        options.add(
            new TrainingExamQuestionOption(
                matcher.group(1).toUpperCase(), content.isEmpty() ? matcher.group(1).toUpperCase() : content));
      }
    }
    return options;
  }

  private List<String> parseLegacyAnswers(String value) {
    if (blank(value)) {
      return List.of();
    }
    return normalizeAnswers(List.of(value.split("[,，;；|、\\s]+")));
  }

  private String legacyOptions(List<TrainingExamQuestionOption> options) {
    return options.stream()
        .map(option -> option.key() + ". " + option.content())
        .collect(java.util.stream.Collectors.joining("\n"));
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value == null ? List.of() : value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("题目结构保存失败");
    }
  }

  private <T> T readJson(String value, TypeReference<T> type, T fallback) {
    if (blank(value)) {
      return fallback;
    }
    try {
      return objectMapper.readValue(value, type);
    } catch (JsonProcessingException exception) {
      return fallback;
    }
  }

  private TrainingExamQuestionType storedQuestionType(String value) {
    try {
      return TrainingExamQuestionType.parse(value);
    } catch (BusinessException ignored) {
      return null;
    }
  }

  private BigDecimal decimal(String value, String label) {
    if (blank(value)) {
      return BigDecimal.ZERO;
    }
    try {
      return scale(new BigDecimal(value.trim()));
    } catch (NumberFormatException e) {
      throw new BusinessException(label + "必须是数字");
    }
  }

  private BigDecimal scale(BigDecimal value) {
    return value.setScale(1, RoundingMode.HALF_UP);
  }

  private String code(String prefix, java.time.LocalDate date, Long id) {
    return prefix + "-" + date.format(CODE_MONTH) + "-" + String.format("%04d", id);
  }

  private String orgName(Long id) {
    SysOrg org = orgMapper.selectById(id);
    return org == null ? "" : org.orgName;
  }

  private String text(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private record OrgPair(SysOrg company, SysOrg department) {}

  private record ExamOrgScope(SysOrg company, SysOrg department, SysOrg team) {}

  private record NormalizedBankQuestion(
      TrainingExamQuestionType type,
      String questionText,
      List<TrainingExamQuestionOption> options,
      List<String> correctAnswers,
      String referenceAnswer,
      String answerExplanation,
      String caseMaterial,
      BigDecimal score,
      List<TrainingExamQuestionBankChild> children) {}
}
