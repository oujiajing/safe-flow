<script setup lang="ts">
import type { TableColumnsType, TableProps } from 'ant-design-vue';

import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useAccessStore, useUserStore } from '@vben/stores';

import {
  Button,
  DatePicker,
  Empty,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Space,
  Table,
} from 'ant-design-vue';

import {
  batchDeleteExamPapersApi,
  batchDeleteExamQuestionBankApi,
  batchDeleteExamResultsApi,
  batchDeleteExamTasksApi,
  confirmExamQuestionBankPdfApi,
  createExamPaperApi,
  createExamQuestionBankApi,
  createExamResultApi,
  createExamTaskApi,
  deleteExamPaperApi,
  deleteExamQuestionBankApi,
  deleteExamResultApi,
  deleteExamTaskApi,
  downloadExamQuestionBankApi,
  downloadExamQuestionBankTemplateApi,
  downloadExamQuestionTemplateApi,
  downloadExamTaskQuestionsApi,
  getExamQuestionBankApi,
  getExamPapersApi,
  getExamResultsApi,
  getExamResultDetailApi,
  getExamTaskDetailApi,
  getExamTaskResultsApi,
  getExamTasksApi,
  previewExamQuestionBankPdfApi,
  reviewExamResultApi,
  updateExamPaperApi,
  updateExamQuestionBankApi,
  uploadExamQuestionBankWorkbookApi,
  uploadExamQuestionWorkbookApi,
} from '#/api/pingan/training-exam';
import type { PinganTrainingExamApi } from '#/api/pingan/training-exam';
import {
  getPinganCompanyOrgTreeApi,
  getPinganOrgTreeApi,
  getPinganUsersApi,
} from '#/api/pingan/pre-shift-meeting';
import DataMapPanel from '#/views/pingan/shared/DataMapPanel.vue';
import StatusFilterActions from '#/views/pingan/shared/StatusFilterActions.vue';
import type { DataMapNode } from '#/views/pingan/shared/data-map-state';
import { canShowPinganDataMap } from '#/views/pingan/shared/data-map-permission';
import {
  applyScopedPinganOrganizationDefaults,
  getVisiblePinganOrganizationFilterKeys,
  isPinganOrganizationFilterLocked,
  resolveScopedPinganOrganizationDefaults,
  type PinganOrganizationFilterKey,
} from '#/views/pingan/shared/organization-filter-permission';

interface Option {
  label: string;
  value: number | string;
}

type Mode = 'results' | 'tasks';
type ViewMode =
  | 'createTask'
  | 'list'
  | 'paperEdit'
  | 'papers'
  | 'questionBankEdit'
  | 'questionBank'
  | 'resultDetail'
  | 'taskDetail';

const route = useRoute();
const accessStore = useAccessStore();
const userStore = useUserStore();

const trainingExamPermissionCodes = {
  legacyManage: 'PINGAN_TRAINING_MANAGE',
  legacyView: 'PINGAN_TRAINING_VIEW',
  resultsCreate: 'PINGAN_TRAINING_EXAM_RESULTS_CREATE',
  resultsDelete: 'PINGAN_TRAINING_EXAM_RESULTS_DELETE',
  tasksCreate: 'PINGAN_TRAINING_EXAM_TASKS_CREATE',
  tasksDelete: 'PINGAN_TRAINING_EXAM_TASKS_DELETE',
  tasksDownload: 'PINGAN_TRAINING_EXAM_TASKS_DOWNLOAD',
} as const;

const taskStatusOptions = [
  { label: '全部', value: 'all' },
  { label: '已生效', value: 'ACTIVE' },
  { label: '未生效', value: 'INACTIVE' },
];

const resultStatusOptions = [
  { label: '全部', value: 'all' },
  { label: '待考试', value: 'PENDING_EXAM' },
  { label: '已考试', value: 'EXAMED' },
];

const questionTypeOptions = [
  { label: '全部', value: 'all' },
  { label: '单选题', value: 'SINGLE_CHOICE' },
  { label: '多选题', value: 'MULTIPLE_CHOICE' },
  { label: '问答题', value: 'SHORT_ANSWER' },
  { label: '案例分析题', value: 'CASE_ANALYSIS' },
];
const childQuestionTypeOptions = questionTypeOptions.filter(
  (item) => item.value !== 'all' && item.value !== 'CASE_ANALYSIS',
);

const taskColumns: TableColumnsType<PinganTrainingExamApi.ExamTask> = [
  { dataIndex: 'company', fixed: 'left', title: '公司', width: 180 },
  { dataIndex: 'department', title: '部门', width: 160 },
  { dataIndex: 'team', title: '班组', width: 150 },
  { dataIndex: 'exam', title: '考试', width: 220 },
  { dataIndex: 'examDate', title: '考试日期', width: 130 },
  { dataIndex: 'durationMinutes', title: '时长', width: 90 },
  { dataIndex: 'statusLabel', title: '状态', width: 120 },
  { dataIndex: 'createdAt', title: '创建时间', width: 180 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 160 },
];

const resultColumns: TableColumnsType<PinganTrainingExamApi.ExamResult> = [
  { dataIndex: 'company', fixed: 'left', title: '公司', width: 180 },
  { dataIndex: 'department', title: '部门', width: 160 },
  { dataIndex: 'examPersonName', title: '考试人员', width: 140 },
  { dataIndex: 'exam', title: '考试', width: 220 },
  { dataIndex: 'score', title: '考分', width: 100 },
  { dataIndex: 'examDate', title: '考试日期', width: 130 },
  { dataIndex: 'statusLabel', title: '状态', width: 120 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 120 },
];

const departmentDetailColumns: TableColumnsType<PinganTrainingExamApi.ExamResult> =
  [
    { dataIndex: 'company', fixed: 'left', title: '公司', width: 180 },
    { dataIndex: 'department', title: '部门', width: 150 },
    { dataIndex: 'examPersonName', title: '考试人员', width: 130 },
    { dataIndex: 'exam', title: '考试', width: 220 },
    { dataIndex: 'examDate', title: '考试日期', width: 130 },
    { dataIndex: 'statusLabel', title: '状态', width: 110 },
    { dataIndex: 'actions', fixed: 'right', title: '操作', width: 100 },
  ];

const questionColumns: TableColumnsType<PinganTrainingExamApi.ExamQuestion> = [
  { dataIndex: 'questionType', title: '题型', width: 120 },
  { dataIndex: 'questionText', title: '考题', width: 260 },
  { dataIndex: 'selectedOption', title: '选项', width: 150 },
  { dataIndex: 'allOptions', title: '全部选项', width: 220 },
  { dataIndex: 'answer', title: '答案', width: 140 },
  { dataIndex: 'answerExplanation', title: '答案解析', width: 220 },
  { dataIndex: 'score', title: '分值', width: 110 },
  { dataIndex: 'actualScore', title: '实际分', width: 110 },
];

const editableQuestionColumns: TableColumnsType<PinganTrainingExamApi.ExamQuestion> =
  [
    ...questionColumns,
    { dataIndex: 'actions', fixed: 'right', title: '操作', width: 90 },
  ];

const paperEditableQuestionColumns: TableColumnsType<PinganTrainingExamApi.ExamQuestion> =
  [
    { align: 'center', dataIndex: 'questionNo', title: '题号', width: 70 },
    { dataIndex: 'questionType', title: '题型', width: 130 },
    { dataIndex: 'questionText', title: '考题', width: 300 },
    { dataIndex: 'selectedOption', title: '选项', width: 150 },
    { dataIndex: 'allOptions', title: '全部选项', width: 300 },
    { dataIndex: 'answer', title: '答案', width: 170 },
    { dataIndex: 'answerExplanation', title: '答案解析', width: 280 },
    { dataIndex: 'score', title: '分值', width: 100 },
    { dataIndex: 'actualScore', title: '实际分', width: 100 },
    { dataIndex: 'actions', fixed: 'right', title: '操作', width: 150 },
  ];

const paperColumns: TableColumnsType<PinganTrainingExamApi.ExamPaper> = [
  { dataIndex: 'company', ellipsis: true, title: '公司', width: 110 },
  { dataIndex: 'department', ellipsis: true, title: '部门', width: 100 },
  { dataIndex: 'team', ellipsis: true, title: '班组', width: 90 },
  { dataIndex: 'paperName', ellipsis: true, title: '试卷名称', width: 140 },
  { dataIndex: 'questionCount', title: '题目数量', width: 78 },
  { dataIndex: 'totalScore', title: '总分', width: 64 },
  { dataIndex: 'description', ellipsis: true, title: '说明', width: 140 },
  { dataIndex: 'updatedAt', title: '更新时间', width: 132 },
  { dataIndex: 'actions', title: '操作', width: 96 },
];

const questionBankColumns: TableColumnsType<PinganTrainingExamApi.ExamQuestionBank> =
  [
    { dataIndex: 'questionTypeLabel', title: '题型', width: 110 },
    { dataIndex: 'questionText', ellipsis: true, title: '考题', width: 330 },
    { dataIndex: 'answer', ellipsis: true, title: '答案', width: 100 },
    {
      dataIndex: 'answerExplanation',
      ellipsis: true,
      title: '答案解析',
      width: 280,
    },
    { dataIndex: 'score', title: '分值', width: 70 },
    { dataIndex: 'actions', title: '操作', width: 100 },
  ];

const questionBankPdfColumns: TableColumnsType<PinganTrainingExamApi.ExamQuestionBankPdfDraft> =
  [
    { dataIndex: 'sourceLabel', title: '来源', width: 110 },
    { dataIndex: 'questionTypeLabel', title: '题型', width: 100 },
    { dataIndex: 'questionText', title: '题目', width: 360 },
    { dataIndex: 'score', title: '分值', width: 90 },
    { dataIndex: 'actions', fixed: 'right', title: '操作', width: 120 },
  ];

const filters = reactive<PinganTrainingExamApi.ListParams>({
  companyId: undefined,
  dateEnd: '',
  dateStart: '',
  departmentId: undefined,
  status: 'all',
});

const taskForm = reactive<PinganTrainingExamApi.ExamTaskPayload>({
  companyId: '',
  departmentId: undefined,
  durationMinutes: 30,
  exam: '',
  examDate: '',
  examPersonUserIds: [],
  questions: [],
  remark: '',
  status: 'ACTIVE',
  teamId: undefined,
});

const questionBankFilters =
  reactive<PinganTrainingExamApi.QuestionBankListParams>({
    companyId: undefined,
    departmentId: undefined,
    keyword: '',
    page: 1,
    pageSize: 20,
    questionType: 'all',
    teamId: undefined,
  });

const questionBankForm =
  reactive<PinganTrainingExamApi.ExamQuestionBankPayload>({
    answerExplanation: '',
    caseMaterial: '',
    children: [],
    companyId: '',
    correctAnswers: ['A'],
    departmentId: undefined,
    options: createChoiceOptions('SINGLE_CHOICE'),
    questionText: '',
    questionType: 'SINGLE_CHOICE',
    referenceAnswer: '',
    score: 5,
    teamId: undefined,
  });

const paperFilters = reactive<PinganTrainingExamApi.PaperListParams>({
  companyId: undefined,
  departmentId: undefined,
  keyword: '',
  page: 1,
  pageSize: 100,
  teamId: undefined,
});

const paperForm = reactive<PinganTrainingExamApi.ExamPaperPayload>({
  companyId: undefined,
  departmentId: undefined,
  description: '',
  paperName: '',
  questions: [],
  teamId: undefined,
});

const resultForm = reactive<PinganTrainingExamApi.ExamResultPayload>({
  examPersonUserId: '',
  score: 0,
  status: 'PENDING_EXAM',
  taskId: '',
});

const organizationNodes = ref<DataMapNode[]>([]);
const organizationOptionNodes = ref<DataMapNode[]>([]);
const selectedOrganizationKeys = ref<string[]>([]);
const companyOptions = ref<Option[]>([]);
const departmentOptions = ref<Option[]>([]);
const users = ref<
  Array<{ id: number | string; realName: string; username: string }>
>([]);
const taskRows = ref<PinganTrainingExamApi.ExamTask[]>([]);
const resultRows = ref<PinganTrainingExamApi.ExamResult[]>([]);
const taskOptions = ref<PinganTrainingExamApi.ExamTask[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const detailLoading = ref(false);
const resultCreateOpen = ref(false);
const questionBankOpen = ref(false);
const questionBankTarget = ref<'paper' | 'task'>('task');
const questionBankLoading = ref(false);
const questionBankSaving = ref(false);
const questionBankPdfConfirming = ref(false);
const questionBankPdfLoading = ref(false);
const questionBankPdfOpen = ref(false);
const pdfImportTarget = ref<'paper' | 'questionBank'>('questionBank');
const questionBankPdfPreview =
  ref<PinganTrainingExamApi.ExamQuestionBankPdfPreview>();
const questionBankRows = ref<PinganTrainingExamApi.ExamQuestionBank[]>([]);
const questionBankTotal = ref(0);
const questionBankEditingId = ref<PinganTrainingExamApi.Id>();
const questionBankPdfEditingIndex = ref<number>();
const questionBankEditorReturnMode = ref<ViewMode>('questionBank');
const questionBankEditorReopenPdf = ref(false);
const questionBankEditorReopenSelector = ref(false);
const selectedQuestionBankRowKeys = ref<string[]>([]);
const paperEditingId = ref<PinganTrainingExamApi.Id>();
const paperLoading = ref(false);
const paperRows = ref<PinganTrainingExamApi.ExamPaper[]>([]);
const paperSaving = ref(false);
const paperTotal = ref(0);
const selectedPaperId = ref<PinganTrainingExamApi.Id>();
const selectedPaperRowKeys = ref<string[]>([]);
const selectedPaperQuestionRowKeys = ref<string[]>([]);
const expandedPaperQuestionRowKeys = ref<string[]>([]);
const selectedTaskQuestionRowKeys = ref<string[]>([]);
const questionFileInput = ref<HTMLInputElement>();
const questionBankFileInput = ref<HTMLInputElement>();
const questionBankPdfFileInput = ref<HTMLInputElement>();
const paperPdfFileInput = ref<HTMLInputElement>();
const viewMode = ref<ViewMode>('list');
const taskDetail = ref<PinganTrainingExamApi.ExamTaskDetail>();
const resultDetail = ref<PinganTrainingExamApi.ExamResultDetail>();
const reviewingResult = ref(false);
const selectedRowKeys = ref<string[]>([]);

const mode = computed<Mode>(() =>
  route.name === 'PinganExamResults' ? 'results' : 'tasks',
);
const isTaskMode = computed(() => mode.value === 'tasks');
const title = computed(() => (isTaskMode.value ? '考试任务' : '考试成绩'));
const createTitle = computed(() =>
  isTaskMode.value ? '新增考试任务' : '新增考试成绩',
);
const questionBankEditorTitle = computed(() =>
  questionBankPdfEditingIndex.value !== undefined
    ? '校正 PDF 解析草稿'
    : questionBankEditingId.value
      ? '编辑题库试题'
      : '新建题库试题',
);
const questionBankEditorReturnLabel = computed(() =>
  questionBankEditorReopenPdf.value
    ? '返回 PDF 预览'
    : questionBankEditorReopenSelector.value
      ? '返回选择题目'
      : '返回题库管理',
);
const statusOptions = computed(() =>
  isTaskMode.value ? taskStatusOptions : resultStatusOptions,
);
const columns = computed(() =>
  isTaskMode.value ? taskColumns : resultColumns,
);
const rows = computed(() =>
  isTaskMode.value ? taskRows.value : resultRows.value,
);
const accessCodes = computed(() => accessStore.accessCodes ?? []);
const shouldShowDataMap = computed(() =>
  canShowPinganDataMap(userStore.userInfo?.roles),
);
const currentUserRoles = computed(() => userStore.userInfo?.roles ?? []);
const currentUserOrgDefaults = computed(() =>
  resolveScopedPinganOrganizationDefaults(
    userStore.userInfo?.orgId,
    organizationOptionNodes.value,
  ),
);
const visibleOrganizationFilterKeys = computed(() =>
  getVisiblePinganOrganizationFilterKeys(currentUserRoles.value),
);
function isOrganizationFilterVisible(field: PinganOrganizationFilterKey) {
  return visibleOrganizationFilterKeys.value.includes(field);
}
function isOrganizationFieldLocked(field: PinganOrganizationFilterKey) {
  return isPinganOrganizationFilterLocked(currentUserRoles.value, field);
}
const canCreateExamTasks = computed(() =>
  hasAnyAccessCode([
    trainingExamPermissionCodes.tasksCreate,
    trainingExamPermissionCodes.legacyManage,
  ]),
);
const canDeleteExamTasks = computed(() =>
  hasAnyAccessCode([
    trainingExamPermissionCodes.tasksDelete,
    trainingExamPermissionCodes.legacyManage,
  ]),
);
const canDownloadExamTasks = computed(() =>
  hasAnyAccessCode([
    trainingExamPermissionCodes.tasksDownload,
    trainingExamPermissionCodes.legacyView,
    trainingExamPermissionCodes.legacyManage,
  ]),
);
const canCreateExamResults = computed(() =>
  hasAnyAccessCode([
    trainingExamPermissionCodes.resultsCreate,
    trainingExamPermissionCodes.legacyManage,
  ]),
);
const canDeleteExamResults = computed(() =>
  hasAnyAccessCode([
    trainingExamPermissionCodes.resultsDelete,
    trainingExamPermissionCodes.legacyManage,
  ]),
);
const canCreateCurrent = computed(() =>
  isTaskMode.value ? canCreateExamTasks.value : canCreateExamResults.value,
);
const canDeleteCurrent = computed(() =>
  isTaskMode.value ? canDeleteExamTasks.value : canDeleteExamResults.value,
);
const canBatchDelete = computed(
  () => selectedRowKeys.value.length > 0 && canDeleteCurrent.value,
);
const showList = computed(() => viewMode.value === 'list');

const userOptions = computed<Option[]>(() =>
  users.value.map((user) => ({
    label: user.realName || user.username,
    value: user.id,
  })),
);

const examTaskOptions = computed<Option[]>(() =>
  taskOptions.value.map((task) => ({
    label: `${task.exam} / ${task.department} / ${task.examDate}`,
    value: task.id,
  })),
);

const paperOptions = computed<Option[]>(() =>
  paperRows.value.filter(paperAppliesToTask).map((paper) => ({
    label: `${paper.paperName}（${paper.company || '全局共享'} / ${paper.questionCount}题 / ${paper.totalScore}分）`,
    value: paper.id,
  })),
);

function paperAppliesToTask(paper: PinganTrainingExamApi.ExamPaper) {
  return (
    (!paper.companyId ||
      String(paper.companyId) === String(taskForm.companyId)) &&
    (!paper.departmentId ||
      String(paper.departmentId) === String(taskForm.departmentId)) &&
    (!paper.teamId || String(paper.teamId) === String(taskForm.teamId))
  );
}

const selectedTaskPreview = computed(() =>
  taskOptions.value.find(
    (task) => String(task.id) === String(resultForm.taskId),
  ),
);

function hasAnyAccessCode(codes: string[]) {
  return codes.some((code) => accessCodes.value.includes(code));
}

const derivedResultStatus = computed<PinganTrainingExamApi.ResultStatus>(() =>
  Number(resultForm.score || 0) > 0 ? 'EXAMED' : 'PENDING_EXAM',
);
const derivedResultStatusLabel = computed(() =>
  derivedResultStatus.value === 'EXAMED' ? '已考试' : '待考试',
);

const totalQuestionScore = computed(() =>
  (resultDetail.value?.questions ?? []).reduce(
    (sum, question) => sum + Number(question.score || 0),
    0,
  ),
);

const totalActualScore = computed(() =>
  (resultDetail.value?.questions ?? []).reduce(
    (sum, question) => sum + Number(question.actualScore || 0),
    0,
  ),
);

function requiresManualReview(question: PinganTrainingExamApi.ExamQuestion) {
  return (
    question.questionType === 'SHORT_ANSWER' ||
    (question.questionType === 'CASE_ANALYSIS' &&
      (question.children ?? []).some(
        (child) => child.questionType === 'SHORT_ANSWER',
      ))
  );
}

function formatSelectedAnswer(question: PinganTrainingExamApi.ExamQuestion) {
  if (question.questionType !== 'CASE_ANALYSIS') {
    return question.selectedOption || '-';
  }
  try {
    const answers = JSON.parse(question.selectedOption || '{}') as Record<
      string,
      string
    >;
    return (question.children ?? [])
      .map(
        (child, index) =>
          `${index + 1}. ${child.questionText}：${answers[String(index)] || '-'}`,
      )
      .join('\n');
  } catch {
    return question.selectedOption || '-';
  }
}

async function submitResultReview() {
  if (!resultDetail.value || resultDetail.value.status !== 'PENDING_REVIEW') {
    return;
  }
  const questions = resultDetail.value.questions.filter(requiresManualReview);
  if (
    questions.some(
      (question) =>
        Number(question.actualScore) < 0 ||
        Number(question.actualScore) > Number(question.score),
    )
  ) {
    message.warning('实际分需在 0 到题目分值之间');
    return;
  }
  reviewingResult.value = true;
  try {
    resultDetail.value = await reviewExamResultApi(resultDetail.value.id, {
      scores: questions.map((question) => ({
        actualScore: Number(question.actualScore),
        questionId: question.id!,
      })),
    });
    message.success('人工评分已完成');
    await loadData();
  } catch {
    message.error('人工评分失败');
  } finally {
    reviewingResult.value = false;
  }
}

const tableRowSelection = computed<TableProps<any>['rowSelection']>(() => ({
  onChange: (keys) => {
    selectedRowKeys.value = keys.map(String);
  },
  selectedRowKeys: selectedRowKeys.value,
}));

const selectedQuestionBankRows = computed(() =>
  questionBankRows.value.filter((row) =>
    selectedQuestionBankRowKeys.value.includes(String(row.id)),
  ),
);

const questionBankRowSelection = computed<TableProps<any>['rowSelection']>(
  () => ({
    onChange: (_keys, selectedRows) => {
      selectedQuestionBankRowKeys.value = selectedRows
        .map((row) => row.id)
        .filter((id) => id !== undefined && id !== null)
        .map(String);
    },
    selectedRowKeys: selectedQuestionBankRowKeys.value,
  }),
);

const paperRowSelection = computed<TableProps<any>['rowSelection']>(() => ({
  onChange: (keys) => {
    selectedPaperRowKeys.value = keys.map(String);
  },
  selectedRowKeys: selectedPaperRowKeys.value,
}));

const paperQuestionRowSelection = computed<TableProps<any>['rowSelection']>(
  () => ({
    onChange: (keys) => {
      selectedPaperQuestionRowKeys.value = keys.map(String);
    },
    selectedRowKeys: selectedPaperQuestionRowKeys.value,
  }),
);

const taskQuestionRowSelection = computed<TableProps<any>['rowSelection']>(
  () => ({
    onChange: (keys) => {
      selectedTaskQuestionRowKeys.value = keys.map(String);
    },
    selectedRowKeys: selectedTaskQuestionRowKeys.value,
  }),
);

function listParams(): PinganTrainingExamApi.ListParams {
  return {
    companyId: filters.companyId || undefined,
    dateEnd: filters.dateEnd || undefined,
    dateStart: filters.dateStart || undefined,
    departmentId: filters.departmentId || undefined,
    page: 1,
    pageSize: 20,
    status: filters.status || 'all',
  };
}

function flattenCompanies(nodes: DataMapNode[]): Option[] {
  return nodes.flatMap((node) => [
    ...(node.orgType === 'COMPANY' && node.id !== undefined
      ? [{ label: node.title, value: node.id }]
      : []),
    ...flattenCompanies(node.children ?? []),
  ]);
}

function flattenDepartments(nodes: DataMapNode[]): Option[] {
  return nodes.flatMap((node) => [
    ...(node.orgType === 'DEPARTMENT' && node.id !== undefined
      ? [{ label: node.title, value: node.id }]
      : []),
    ...flattenDepartments(node.children ?? []),
  ]);
}

function flattenTeams(nodes: DataMapNode[]): Option[] {
  return nodes.flatMap((node) => [
    ...(node.orgType === 'TEAM' && node.id !== undefined
      ? [{ label: node.title, value: node.id }]
      : []),
    ...flattenTeams(node.children ?? []),
  ]);
}

function findNode(
  nodes: DataMapNode[],
  predicate: (node: DataMapNode) => boolean,
): DataMapNode | undefined {
  for (const node of nodes) {
    if (predicate(node)) {
      return node;
    }
    const child = findNode(node.children ?? [], predicate);
    if (child) {
      return child;
    }
  }
}

function findCompanyForNode(
  nodes: DataMapNode[],
  targetKey?: string,
  currentCompany?: DataMapNode,
): DataMapNode | undefined {
  for (const node of nodes) {
    const company = node.orgType === 'COMPANY' ? node : currentCompany;
    if (node.key === targetKey) {
      return node.orgType === 'COMPANY' ? node : company;
    }
    const child = findCompanyForNode(node.children ?? [], targetKey, company);
    if (child) {
      return child;
    }
  }
}

function departmentsForCompany(companyId?: PinganTrainingExamApi.Id): Option[] {
  if (!companyId) {
    return flattenDepartments(organizationOptionNodes.value);
  }
  const company = findNode(
    organizationOptionNodes.value,
    (node) => String(node.id) === String(companyId),
  );
  return company ? flattenDepartments(company.children ?? []) : [];
}

function teamsForDepartment(departmentId?: PinganTrainingExamApi.Id): Option[] {
  if (!departmentId) {
    return [];
  }
  const department = findNode(
    organizationOptionNodes.value,
    (node) => String(node.id) === String(departmentId),
  );
  return department ? flattenTeams(department.children ?? []) : [];
}

function refreshDepartmentOptions() {
  departmentOptions.value = departmentsForCompany(filters.companyId);
}

function applyScopedOrganizationDefaults(target: {
  companyId?: PinganTrainingExamApi.Id;
  departmentId?: PinganTrainingExamApi.Id;
}) {
  const defaults = currentUserOrgDefaults.value;
  if (
    isOrganizationFieldLocked('company') &&
    defaults.companyId !== undefined
  ) {
    target.companyId = defaults.companyId;
  }
  if (
    isOrganizationFieldLocked('department') &&
    defaults.departmentId !== undefined
  ) {
    target.departmentId = defaults.departmentId;
  }
}

function enforceUserOrganizationScope() {
  if (!organizationOptionNodes.value.length) {
    return;
  }
  applyScopedOrganizationDefaults(filters);
  refreshDepartmentOptions();
}

function enforceTaskFormOrganizationScope() {
  Object.assign(
    taskForm,
    applyScopedPinganOrganizationDefaults(
      {
        companyId: taskForm.companyId,
        departmentId: taskForm.departmentId,
      },
      currentUserOrgDefaults.value,
      currentUserRoles.value,
    ),
  );
}

function enforceQuestionBankFormOrganizationScope() {
  Object.assign(
    questionBankForm,
    applyScopedPinganOrganizationDefaults(
      {
        companyId: questionBankForm.companyId,
        departmentId: questionBankForm.departmentId,
      },
      currentUserOrgDefaults.value,
      currentUserRoles.value,
    ),
  );
}

async function loadOrganizations() {
  try {
    organizationNodes.value = await getPinganCompanyOrgTreeApi();
    organizationOptionNodes.value = await getPinganOrgTreeApi();
    companyOptions.value = flattenCompanies(organizationNodes.value);
    refreshDepartmentOptions();
    enforceUserOrganizationScope();
  } catch {
    organizationNodes.value = [];
    organizationOptionNodes.value = [];
    companyOptions.value = [];
    departmentOptions.value = [];
  }
}

async function loadUsers() {
  try {
    users.value = await getPinganUsersApi();
  } catch {
    users.value = [];
  }
}

async function loadTaskOptions() {
  try {
    const result = await getExamTasksApi({
      page: 1,
      pageSize: 200,
      status: 'all',
    });
    taskOptions.value = result.items;
  } catch {
    taskOptions.value = [];
  }
}

function questionBankListParams(): PinganTrainingExamApi.QuestionBankListParams {
  return {
    applicable: questionBankOpen.value || undefined,
    companyId: questionBankFilters.companyId || undefined,
    departmentId: questionBankFilters.departmentId || undefined,
    keyword: questionBankFilters.keyword || undefined,
    page: questionBankFilters.page || 1,
    pageSize: questionBankFilters.pageSize || 20,
    questionType:
      questionBankFilters.questionType === 'all'
        ? undefined
        : questionBankFilters.questionType,
    teamId: questionBankFilters.teamId || undefined,
  };
}

async function loadQuestionBank() {
  questionBankLoading.value = true;
  try {
    const result = await getExamQuestionBankApi(questionBankListParams());
    questionBankRows.value = result.items;
    questionBankTotal.value = result.total;
  } catch {
    questionBankRows.value = [];
    questionBankTotal.value = 0;
    message.error('题库数据加载失败');
  } finally {
    questionBankLoading.value = false;
  }
}

async function loadPapers() {
  paperLoading.value = true;
  try {
    const result = await getExamPapersApi({
      companyId: paperFilters.companyId || undefined,
      departmentId: paperFilters.departmentId || undefined,
      keyword: paperFilters.keyword || undefined,
      page: paperFilters.page || 1,
      pageSize: paperFilters.pageSize || 100,
      teamId: paperFilters.teamId || undefined,
    });
    paperRows.value = result.items;
    paperTotal.value = result.total;
  } catch {
    paperRows.value = [];
    paperTotal.value = 0;
    message.error('试卷数据加载失败');
  } finally {
    paperLoading.value = false;
  }
}

async function resetAndLoadQuestionBank() {
  resetQuestionBankFiltersFromTask();
  await loadQuestionBank();
}

async function loadData() {
  loading.value = true;
  selectedRowKeys.value = [];
  try {
    if (isTaskMode.value) {
      const result = await getExamTasksApi(listParams());
      taskRows.value = result.items;
      total.value = result.total;
    } else {
      const result = await getExamResultsApi(listParams());
      resultRows.value = result.items;
      total.value = result.total;
      await loadTaskOptions();
    }
  } catch {
    taskRows.value = [];
    resultRows.value = [];
    total.value = 0;
    message.error(`${title.value}数据加载失败`);
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  filters.companyId = undefined;
  filters.departmentId = undefined;
  filters.dateStart = '';
  filters.dateEnd = '';
  filters.status = 'all';
  selectedOrganizationKeys.value = [];
  refreshDepartmentOptions();
  enforceUserOrganizationScope();
  void loadData();
}

function handleCompanyChange() {
  if (isOrganizationFieldLocked('company')) {
    enforceUserOrganizationScope();
    return;
  }
  filters.departmentId = undefined;
  selectedOrganizationKeys.value = [];
  refreshDepartmentOptions();
}

function handleDepartmentChange() {
  if (isOrganizationFieldLocked('department')) {
    enforceUserOrganizationScope();
  }
}

function handleDataMapSelect(node?: DataMapNode) {
  if (!shouldShowDataMap.value || !node) {
    return;
  }
  if (
    isOrganizationFieldLocked('company') ||
    isOrganizationFieldLocked('department')
  ) {
    enforceUserOrganizationScope();
    void loadData();
    return;
  }
  const company = findCompanyForNode(organizationNodes.value, node.key);
  filters.companyId = company?.id;
  filters.departmentId = node.orgType === 'DEPARTMENT' ? node.id : undefined;
  selectedOrganizationKeys.value = [node.key];
  refreshDepartmentOptions();
  enforceUserOrganizationScope();
  void loadData();
}

function blankQuestion(index: number): PinganTrainingExamApi.ExamQuestion {
  return {
    actualScore: 0,
    allOptions: '',
    answer: '',
    questionText: '',
    questionType: 'SINGLE_CHOICE',
    score: 0,
    selectedOption: '',
    sortOrder: index,
  };
}

function createChoiceOptions(
  type: PinganTrainingExamApi.QuestionType,
): PinganTrainingExamApi.ExamQuestionOption[] {
  if (type === 'SHORT_ANSWER' || type === 'CASE_ANALYSIS') {
    return [];
  }
  const count = 4;
  return Array.from({ length: count }, (_, index) => ({
    content: '',
    key: String.fromCharCode(65 + index),
  }));
}

function createQuestionBankChild(): PinganTrainingExamApi.ExamQuestionBankChild {
  return {
    answerExplanation: '',
    correctAnswers: ['A'],
    options: createChoiceOptions('SINGLE_CHOICE'),
    questionText: '',
    questionType: 'SINGLE_CHOICE',
    referenceAnswer: '',
    score: 5,
  };
}

function isChoiceQuestionType(type?: string) {
  return type === 'SINGLE_CHOICE' || type === 'MULTIPLE_CHOICE';
}

function parseLegacyPaperOptions(value?: string) {
  const text = String(value || '').trim();
  if (!text) {
    return [];
  }
  const matches = [
    ...text.matchAll(/(?:^|\s)([A-E])[.、．]\s*(.*?)(?=\s+[A-E][.、．]\s*|$)/g),
  ];
  if (matches.length > 0) {
    return matches.map((match) => ({
      content: match[2]?.trim() || '',
      key: match[1] || '',
    }));
  }
  return text
    .split(/\r?\n|[;；|]/)
    .map((content, index) => ({
      content: content.trim(),
      key: String.fromCharCode(65 + index),
    }))
    .filter((option) => option.content);
}

function ensurePaperChildStructure(
  child: PinganTrainingExamApi.ExamQuestionBankChild,
) {
  if (isChoiceQuestionType(child.questionType)) {
    if (!child.options?.length) {
      child.options = createChoiceOptions(child.questionType);
    }
    if (!child.correctAnswers?.length) {
      child.correctAnswers =
        child.questionType === 'MULTIPLE_CHOICE' ? ['A', 'B'] : ['A'];
    }
    child.referenceAnswer = '';
  } else {
    child.options = [];
    child.correctAnswers = [];
  }
}

function syncPaperQuestionLegacyFields(
  question: PinganTrainingExamApi.ExamQuestion,
) {
  if (question.questionType === 'CASE_ANALYSIS') {
    question.score = Number(
      (question.children ?? []).reduce(
        (sum, child) => sum + Number(child.score || 0),
        0,
      ),
    );
    question.allOptions = '';
    question.answer = '';
    question.options = [];
    question.correctAnswers = [];
    return;
  }
  if (isChoiceQuestionType(question.questionType)) {
    question.allOptions = (question.options ?? [])
      .map((option) => `${option.key}. ${option.content}`)
      .join('\n');
    question.answer = (question.correctAnswers ?? []).join(',');
    question.referenceAnswer = '';
    return;
  }
  question.allOptions = '';
  question.answer = question.referenceAnswer || '';
  question.options = [];
  question.correctAnswers = [];
}

function ensurePaperQuestionStructure(
  question: PinganTrainingExamApi.ExamQuestion,
) {
  if (question.questionType === 'CASE_ANALYSIS') {
    question.children = (question.children ?? []).map((child) => {
      ensurePaperChildStructure(child);
      return child;
    });
    syncPaperQuestionLegacyFields(question);
    return question;
  }
  if (isChoiceQuestionType(question.questionType)) {
    if (!question.options?.length) {
      question.options = parseLegacyPaperOptions(question.allOptions);
    }
    if (!question.options.length) {
      question.options = createChoiceOptions(
        question.questionType as PinganTrainingExamApi.QuestionType,
      );
    }
    if (!question.correctAnswers?.length) {
      question.correctAnswers = String(question.answer || '')
        .split(/[,，、\s]+/)
        .map((answer) => answer.trim().toUpperCase())
        .filter(Boolean);
    }
    if (!question.correctAnswers.length) {
      question.correctAnswers =
        question.questionType === 'MULTIPLE_CHOICE' ? ['A', 'B'] : ['A'];
    }
  } else {
    question.referenceAnswer =
      question.referenceAnswer || question.answer || '';
  }
  syncPaperQuestionLegacyFields(question);
  return question;
}

function changePaperQuestionType(question: PinganTrainingExamApi.ExamQuestion) {
  question.questionTypeLabel =
    questionTypeOptions.find((item) => item.value === question.questionType)
      ?.label || question.questionType;
  if (question.questionType === 'CASE_ANALYSIS') {
    question.options = [];
    question.correctAnswers = [];
    question.referenceAnswer = '';
    question.caseMaterial ||= '';
    question.children =
      (question.children?.length ?? 0) > 0
        ? question.children
        : [createQuestionBankChild()];
  } else if (isChoiceQuestionType(question.questionType)) {
    question.children = [];
    question.caseMaterial = '';
    question.referenceAnswer = '';
    question.options = createChoiceOptions(
      question.questionType as PinganTrainingExamApi.QuestionType,
    );
    question.correctAnswers =
      question.questionType === 'MULTIPLE_CHOICE' ? ['A', 'B'] : ['A'];
  } else {
    question.children = [];
    question.caseMaterial = '';
    question.options = [];
    question.correctAnswers = [];
    question.referenceAnswer ||= '';
  }
  syncPaperQuestionLegacyFields(question);
  const rowKey = questionRowKey(question);
  expandedPaperQuestionRowKeys.value =
    expandedPaperQuestionRowKeys.value.filter((key) => key !== rowKey);
}

function changePaperCaseChildType(
  parent: PinganTrainingExamApi.ExamQuestion,
  child: PinganTrainingExamApi.ExamQuestionBankChild,
) {
  child.options = createChoiceOptions(child.questionType);
  child.correctAnswers =
    child.questionType === 'SINGLE_CHOICE'
      ? ['A']
      : child.questionType === 'MULTIPLE_CHOICE'
        ? ['A', 'B']
        : [];
  child.referenceAnswer = '';
  syncPaperQuestionLegacyFields(parent);
}

function paperQuestionRowExpandable(
  question: PinganTrainingExamApi.ExamQuestion,
) {
  return question.questionType === 'CASE_ANALYSIS';
}

function answerOptions(options: PinganTrainingExamApi.ExamQuestionOption[]) {
  return options.map((option) => ({ label: option.key, value: option.key }));
}

function changeQuestionBankType() {
  questionBankForm.options = createChoiceOptions(questionBankForm.questionType);
  questionBankForm.correctAnswers =
    questionBankForm.questionType === 'SINGLE_CHOICE'
      ? ['A']
      : questionBankForm.questionType === 'MULTIPLE_CHOICE'
        ? ['A', 'B']
        : [];
  questionBankForm.referenceAnswer = '';
  questionBankForm.caseMaterial = '';
  questionBankForm.children =
    questionBankForm.questionType === 'CASE_ANALYSIS'
      ? [createQuestionBankChild()]
      : [];
}

function changeQuestionBankChildType(
  child: PinganTrainingExamApi.ExamQuestionBankChild,
) {
  child.options = createChoiceOptions(child.questionType);
  child.correctAnswers =
    child.questionType === 'SINGLE_CHOICE'
      ? ['A']
      : child.questionType === 'MULTIPLE_CHOICE'
        ? ['A', 'B']
        : [];
  child.referenceAnswer = '';
}

function addQuestionBankChild() {
  questionBankForm.children.push(createQuestionBankChild());
}

function removeQuestionBankChild(index: number) {
  if (questionBankForm.children.length === 1) {
    message.warning('案例分析题至少需要一道子题');
    return;
  }
  questionBankForm.children.splice(index, 1);
}

function questionBankValidationMessage(requireOrganization = true) {
  if (requireOrganization && !questionBankForm.companyId) {
    return '请选择题库归属公司';
  }
  if (questionBankForm.teamId && !questionBankForm.departmentId) {
    return '选择班组前请先选择部门';
  }
  if (!questionBankForm.questionText.trim()) {
    return questionBankForm.questionType === 'CASE_ANALYSIS'
      ? '请输入案例标题'
      : '请输入题干';
  }
  const validateItem = (
    item:
      | PinganTrainingExamApi.ExamQuestionBankChild
      | PinganTrainingExamApi.ExamQuestionBankPayload,
    label: string,
    allowFiveOptionSingle = false,
  ) => {
    if (!item.questionText.trim()) return `${label}题干不能为空`;
    if (item.score <= 0) return `${label}分值必须大于0`;
    if (item.questionType === 'SHORT_ANSWER') {
      return item.referenceAnswer.trim() ? '' : `${label}参考答案不能为空`;
    }
    if (
      item.questionType === 'SINGLE_CHOICE' ||
      item.questionType === 'MULTIPLE_CHOICE'
    ) {
      const validOptionCount =
        item.questionType === 'MULTIPLE_CHOICE'
          ? item.options.length === 4 || item.options.length === 5
          : item.options.length === 4 ||
            (allowFiveOptionSingle && item.options.length === 5);
      if (!validOptionCount) {
        return item.questionType === 'MULTIPLE_CHOICE'
          ? `${label}必须完整填写 A-D 四个选项或 A-E 五个选项`
          : allowFiveOptionSingle
            ? `${label}必须完整填写 A-D 四个选项或 A-E 五个选项`
            : `${label}必须完整填写 A-D 四个选项`;
      }
      if (item.options.some((option) => !option.content.trim())) {
        return `${label}选项内容不能为空`;
      }
      const minimum = item.questionType === 'MULTIPLE_CHOICE' ? 2 : 1;
      if (
        item.correctAnswers.length < minimum ||
        (item.questionType === 'SINGLE_CHOICE' &&
          item.correctAnswers.length !== 1)
      ) {
        return item.questionType === 'MULTIPLE_CHOICE'
          ? `${label}至少选择两个答案`
          : `${label}请选择一个答案`;
      }
    }
    return '';
  };
  if (questionBankForm.questionType === 'CASE_ANALYSIS') {
    if (!questionBankForm.caseMaterial.trim()) return '请输入案例材料';
    if (!questionBankForm.children.length) return '案例分析题至少需要一道子题';
    for (let index = 0; index < questionBankForm.children.length; index += 1) {
      const error = validateItem(
        questionBankForm.children[index]!,
        `第${index + 1}道子题`,
        true,
      );
      if (error) return error;
    }
    return '';
  }
  return validateItem(questionBankForm, '');
}

function resetTaskForm() {
  taskForm.companyId =
    filters.companyId || companyOptions.value[0]?.value || '';
  refreshDepartmentOptions();
  taskForm.departmentId = filters.departmentId || undefined;
  taskForm.teamId = undefined;
  taskForm.exam = '';
  taskForm.examDate = '';
  taskForm.durationMinutes = 30;
  taskForm.examPersonUserIds = [];
  taskForm.questions = [blankQuestion(0)];
  taskForm.remark = '';
  taskForm.status = 'ACTIVE';
  selectedPaperId.value = undefined;
  selectedTaskQuestionRowKeys.value = [];
  enforceTaskFormOrganizationScope();
}

function resetQuestionBankFiltersFromTask() {
  const taskCompanyId =
    viewMode.value === 'createTask' ? taskForm.companyId : undefined;
  const taskDepartmentId =
    viewMode.value === 'createTask' ? taskForm.departmentId : undefined;
  questionBankFilters.companyId =
    taskCompanyId || filters.companyId || companyOptions.value[0]?.value;
  questionBankFilters.departmentId =
    taskDepartmentId || filters.departmentId || undefined;
  questionBankFilters.teamId =
    viewMode.value === 'createTask' ? taskForm.teamId : undefined;
  questionBankFilters.keyword = '';
  questionBankFilters.questionType = 'all';
  questionBankFilters.page = 1;
}

function resetQuestionBankManagementFilters() {
  questionBankFilters.companyId = undefined;
  questionBankFilters.departmentId = undefined;
  questionBankFilters.teamId = undefined;
  questionBankFilters.keyword = '';
  questionBankFilters.questionType = 'all';
  questionBankFilters.page = 1;
  applyScopedOrganizationDefaults(questionBankFilters);
}

function resetPaperFilters() {
  paperFilters.companyId = filters.companyId;
  paperFilters.departmentId = filters.departmentId;
  paperFilters.teamId = undefined;
  paperFilters.keyword = '';
  paperFilters.page = 1;
  applyScopedOrganizationDefaults(paperFilters);
}

function resetPaperForm(record?: PinganTrainingExamApi.ExamPaper) {
  paperEditingId.value = record?.id;
  paperForm.companyId = record?.companyId;
  paperForm.departmentId = record?.departmentId;
  paperForm.teamId = record?.teamId;
  paperForm.paperName = record?.paperName || '';
  paperForm.description = record?.description || '';
  paperForm.questions = (record?.questions ?? []).map((question, index) =>
    ensurePaperQuestionStructure({
      ...question,
      children: question.children?.map((child) => ({
        ...child,
        correctAnswers: [...child.correctAnswers],
        options: child.options.map((option) => ({ ...option })),
      })),
      correctAnswers: [...(question.correctAnswers ?? [])],
      options: question.options?.map((option) => ({ ...option })),
      sortOrder: index,
    }),
  );
  resetPaperCaseExpansion();
  selectedPaperQuestionRowKeys.value = [];
}

function resetQuestionBankForm(
  record?: PinganTrainingExamApi.ExamQuestionBank,
) {
  questionBankEditingId.value = record?.id;
  questionBankForm.companyId =
    record?.companyId ||
    questionBankFilters.companyId ||
    taskForm.companyId ||
    companyOptions.value[0]?.value ||
    '';
  questionBankForm.departmentId =
    record?.departmentId ||
    questionBankFilters.departmentId ||
    taskForm.departmentId ||
    undefined;
  questionBankForm.teamId =
    record?.teamId ||
    questionBankFilters.teamId ||
    taskForm.teamId ||
    undefined;
  questionBankForm.questionType =
    (record?.questionType as PinganTrainingExamApi.QuestionType) ||
    'SINGLE_CHOICE';
  questionBankForm.questionText = record?.questionText || '';
  questionBankForm.options =
    record?.options?.map((option) => ({ ...option })) ||
    createChoiceOptions(questionBankForm.questionType);
  questionBankForm.correctAnswers = [...(record?.correctAnswers || ['A'])];
  questionBankForm.referenceAnswer = record?.referenceAnswer || '';
  questionBankForm.answerExplanation = record?.answerExplanation || '';
  questionBankForm.caseMaterial = record?.caseMaterial || '';
  questionBankForm.children =
    record?.children?.map((child) => ({
      ...child,
      correctAnswers: [...child.correctAnswers],
      options: child.options.map((option) => ({ ...option })),
    })) || [];
  questionBankForm.score = Number(record?.score ?? 5);
  enforceQuestionBankFormOrganizationScope();
}

function resetResultForm() {
  resultForm.taskId = taskOptions.value[0]?.id || '';
  resultForm.examPersonUserId = userOptions.value[0]?.value || '';
  resultForm.score = 0;
  resultForm.status = 'PENDING_EXAM';
}

function openCreateTaskView() {
  resetTaskForm();
  viewMode.value = 'createTask';
  paperFilters.companyId = undefined;
  paperFilters.departmentId = undefined;
  paperFilters.teamId = undefined;
  paperFilters.keyword = '';
  void loadPapers();
}

async function openResultCreateModal() {
  await loadTaskOptions();
  resetResultForm();
  resultCreateOpen.value = true;
}

function openCreateEntry() {
  if (isTaskMode.value) {
    openCreateTaskView();
  } else {
    void openResultCreateModal();
  }
}

function addQuestion() {
  taskForm.questions = [
    ...(taskForm.questions ?? []),
    blankQuestion(taskForm.questions?.length ?? 0),
  ];
}

function questionRowKey(record: PinganTrainingExamApi.ExamQuestion) {
  return `${
    record.id ??
    record.sortOrder ??
    `${record.questionType}:${record.questionText}:${record.score}`
  }`;
}

function resetPaperCaseExpansion() {
  expandedPaperQuestionRowKeys.value = [];
}

function scrollPaperCaseEditorIntoView(
  question: PinganTrainingExamApi.ExamQuestion,
) {
  const rowKey = questionRowKey(question);
  void nextTick(() => {
    const tableBody = document.querySelector(
      '.paper-question-table .ant-table-body',
    );
    if (!(tableBody instanceof HTMLElement)) {
      return;
    }
    const expandedRow = Array.from(
      tableBody.querySelectorAll<HTMLElement>('tr.ant-table-expanded-row'),
    ).find(
      (row) =>
        row.previousElementSibling?.getAttribute('data-row-key') === rowKey,
    );
    if (expandedRow) {
      tableBody.scrollTo({
        behavior: 'smooth',
        top: Math.max(0, expandedRow.offsetTop - 8),
      });
    }
  });
}

function handlePaperQuestionExpand(
  expanded: boolean,
  question: PinganTrainingExamApi.ExamQuestion,
) {
  const rowKey = questionRowKey(question);
  expandedPaperQuestionRowKeys.value = expanded
    ? [...new Set([...expandedPaperQuestionRowKeys.value, rowKey])]
    : expandedPaperQuestionRowKeys.value.filter((key) => key !== rowKey);
  if (expanded) {
    scrollPaperCaseEditorIntoView(question);
  }
}

function paperCaseExpanded(question: PinganTrainingExamApi.ExamQuestion) {
  return expandedPaperQuestionRowKeys.value.includes(questionRowKey(question));
}

function togglePaperCaseEditor(question: PinganTrainingExamApi.ExamQuestion) {
  handlePaperQuestionExpand(!paperCaseExpanded(question), question);
}

function normalizeQuestions(questions: PinganTrainingExamApi.ExamQuestion[]) {
  return questions.map((question, index) => ({
    ...question,
    sortOrder: index,
  }));
}

function removeQuestion(
  target: 'paper' | 'task',
  record: PinganTrainingExamApi.ExamQuestion,
) {
  const questions =
    target === 'paper' ? paperForm.questions : (taskForm.questions ?? []);
  const rowKey = questionRowKey(record);
  const nextQuestions = normalizeQuestions(
    questions.filter((question) => {
      return questionRowKey(question) !== rowKey;
    }),
  );
  if (target === 'paper') {
    paperForm.questions = nextQuestions;
    selectedPaperQuestionRowKeys.value =
      selectedPaperQuestionRowKeys.value.filter((key) => key !== rowKey);
  } else {
    taskForm.questions = nextQuestions;
    selectedTaskQuestionRowKeys.value =
      selectedTaskQuestionRowKeys.value.filter((key) => key !== rowKey);
  }
}

function batchRemoveQuestions(target: 'paper' | 'task') {
  const selectedKeys =
    target === 'paper'
      ? selectedPaperQuestionRowKeys.value
      : selectedTaskQuestionRowKeys.value;
  if (!selectedKeys.length) {
    return;
  }
  Modal.confirm({
    content: `确认移除已选 ${selectedKeys.length} 道考题？该操作仅影响当前${target === 'paper' ? '试卷' : '考试任务'}。`,
    okText: '批量删除',
    okType: 'danger',
    title: '批量删除确认',
    onOk() {
      const questions =
        target === 'paper' ? paperForm.questions : (taskForm.questions ?? []);
      const nextQuestions = normalizeQuestions(
        questions.filter(
          (question) => !selectedKeys.includes(questionRowKey(question)),
        ),
      );
      if (target === 'paper') {
        paperForm.questions = nextQuestions;
        selectedPaperQuestionRowKeys.value = [];
      } else {
        taskForm.questions = nextQuestions;
        selectedTaskQuestionRowKeys.value = [];
      }
    },
  });
}

async function saveTask(status: PinganTrainingExamApi.TaskStatus) {
  enforceTaskFormOrganizationScope();
  if (!taskForm.companyId) {
    message.warning('请选择考试公司');
    return;
  }
  if (!taskForm.questions?.length) {
    message.warning('请维护考题');
    return;
  }
  saving.value = true;
  try {
    await createExamTaskApi({ ...taskForm, status });
    message.success(
      status === 'ACTIVE' ? '考试任务创建成功' : '考试任务草稿已保存',
    );
    viewMode.value = 'list';
    await loadData();
  } catch {
    message.error('考试任务保存失败');
  } finally {
    saving.value = false;
  }
}

async function saveResultRecord() {
  saving.value = true;
  try {
    await createExamResultApi({
      ...resultForm,
      status: derivedResultStatus.value,
    });
    message.success('新增考试成绩成功');
    resultCreateOpen.value = false;
    await loadData();
  } catch {
    message.error('新增考试成绩失败');
  } finally {
    saving.value = false;
  }
}

function saveBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  window.URL.revokeObjectURL(url);
}

async function downloadQuestionTemplate() {
  try {
    const blob = await downloadExamQuestionTemplateApi();
    saveBlob(blob, '宣教培训考试考题_导入模板.xlsx');
  } catch {
    message.error('考题模板下载失败');
  }
}

function triggerQuestionUpload() {
  questionFileInput.value?.click();
}

async function handleQuestionWorkbook(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  try {
    const result = await uploadExamQuestionWorkbookApi(file);
    taskForm.questions = result.questions;
    message.success(`成功导入 ${result.successRows} 道考题`);
    if (result.errors.length > 0) {
      message.warning(result.errors[0]);
    }
  } catch {
    message.error('考题导入失败');
  } finally {
    input.value = '';
  }
}

async function openQuestionBankSelector(target: 'paper' | 'task') {
  resetQuestionBankFiltersFromTask();
  if (target === 'paper') {
    questionBankFilters.companyId = paperForm.companyId;
    questionBankFilters.departmentId = paperForm.departmentId;
    questionBankFilters.teamId = undefined;
  }
  questionBankTarget.value = target;
  selectedQuestionBankRowKeys.value = [];
  questionBankOpen.value = true;
  await loadQuestionBank();
}

async function openQuestionBankPage() {
  resetQuestionBankManagementFilters();
  selectedQuestionBankRowKeys.value = [];
  viewMode.value = 'questionBank';
  await loadQuestionBank();
}

async function openPapersPage() {
  resetPaperFilters();
  selectedPaperRowKeys.value = [];
  viewMode.value = 'papers';
  await loadPapers();
}

function openPaperCreate() {
  resetPaperForm();
  viewMode.value = 'paperEdit';
}

function openPaperEdit(record: PinganTrainingExamApi.ExamPaper) {
  resetPaperForm(record);
  viewMode.value = 'paperEdit';
}

function closePaperEditor() {
  viewMode.value = 'papers';
}

async function savePaper() {
  if (!paperForm.companyId && paperForm.departmentId) {
    message.warning('选择部门前请先选择公司');
    return;
  }
  if (!paperForm.departmentId && paperForm.teamId) {
    message.warning('选择班组前请先选择部门');
    return;
  }
  if (!paperForm.paperName.trim()) {
    message.warning('请输入试卷名称');
    return;
  }
  if (!paperForm.questions.length) {
    message.warning('试卷至少需要一道考题');
    return;
  }
  paperForm.questions.forEach(syncPaperQuestionLegacyFields);
  paperSaving.value = true;
  try {
    if (paperEditingId.value) {
      await updateExamPaperApi(paperEditingId.value, paperForm);
      message.success('试卷已更新');
    } else {
      await createExamPaperApi(paperForm);
      message.success('试卷已创建');
    }
    viewMode.value = 'papers';
    await loadPapers();
  } catch (error) {
    message.error(requestErrorMessage(error, '试卷保存失败'));
  } finally {
    paperSaving.value = false;
  }
}

function deletePaper(record: PinganTrainingExamApi.ExamPaper) {
  Modal.confirm({
    content: `确认删除试卷“${record.paperName}”？`,
    okText: '删除',
    okType: 'danger',
    title: '删除确认',
    async onOk() {
      await deleteExamPaperApi(record.id);
      await loadPapers();
    },
  });
}

function batchDeletePapers() {
  if (!selectedPaperRowKeys.value.length) {
    return;
  }
  Modal.confirm({
    content: `确认删除已选 ${selectedPaperRowKeys.value.length} 份试卷？`,
    okText: '批量删除',
    okType: 'danger',
    title: '批量删除确认',
    async onOk() {
      await batchDeleteExamPapersApi(selectedPaperRowKeys.value);
      selectedPaperRowKeys.value = [];
      await loadPapers();
    },
  });
}

function applySelectedPaper() {
  const paper = paperRows.value.find(
    (item) => String(item.id) === String(selectedPaperId.value),
  );
  if (!paper) {
    message.warning('请选择要带入的试卷');
    return;
  }
  if (!paperAppliesToTask(paper)) {
    selectedPaperId.value = undefined;
    message.warning('该试卷不适用于当前考试任务的公司或部门');
    return;
  }
  taskForm.questions = normalizeQuestions(
    paper.questions.map((question) => ({
      ...question,
      children: question.children?.map((child) => ({
        ...child,
        correctAnswers: [...child.correctAnswers],
        options: child.options.map((option) => ({ ...option })),
      })),
      correctAnswers: [...(question.correctAnswers ?? [])],
      id: undefined,
      options: question.options?.map((option) => ({ ...option })),
    })),
  );
  selectedTaskQuestionRowKeys.value = [];
  message.success(
    `已带入试卷“${paper.paperName}”的 ${paper.questionCount} 道考题`,
  );
}

function openQuestionBankCreate() {
  questionBankEditorReturnMode.value = viewMode.value;
  questionBankEditorReopenSelector.value = questionBankOpen.value;
  questionBankEditorReopenPdf.value = false;
  questionBankOpen.value = false;
  questionBankPdfEditingIndex.value = undefined;
  resetQuestionBankForm();
  viewMode.value = 'questionBankEdit';
}

function openQuestionBankEdit(record: PinganTrainingExamApi.ExamQuestionBank) {
  questionBankEditorReturnMode.value = viewMode.value;
  questionBankEditorReopenSelector.value = questionBankOpen.value;
  questionBankEditorReopenPdf.value = false;
  questionBankOpen.value = false;
  questionBankPdfEditingIndex.value = undefined;
  resetQuestionBankForm(record);
  viewMode.value = 'questionBankEdit';
}

function openQuestionBankPdfDraft(
  record: PinganTrainingExamApi.ExamQuestionBankPdfDraft,
) {
  const index =
    questionBankPdfPreview.value?.questions.findIndex(
      (question) => question === record,
    ) ?? -1;
  const draft = questionBankPdfPreview.value?.questions[index];
  if (!draft) {
    return;
  }
  questionBankEditorReturnMode.value = viewMode.value;
  questionBankEditorReopenPdf.value = true;
  questionBankEditorReopenSelector.value = false;
  questionBankPdfOpen.value = false;
  questionBankPdfEditingIndex.value = index;
  resetQuestionBankForm({
    ...draft,
    actualScore: 0,
    allOptions: '',
    answer: '',
    company: '',
    companyId: questionBankFilters.companyId || '',
    createdAt: '',
    department: '',
    departmentId: questionBankFilters.departmentId,
    id: `pdf-draft-${index}`,
    selectedOption: '',
    team: '',
    teamId: questionBankFilters.teamId,
    updatedAt: '',
  });
  questionBankEditingId.value = undefined;
  viewMode.value = 'questionBankEdit';
}

function closeQuestionBankEditor() {
  viewMode.value = questionBankEditorReturnMode.value;
  if (questionBankEditorReopenPdf.value) {
    questionBankPdfOpen.value = true;
  } else if (questionBankEditorReopenSelector.value) {
    questionBankOpen.value = true;
  }
  questionBankPdfEditingIndex.value = undefined;
  questionBankEditorReopenPdf.value = false;
  questionBankEditorReopenSelector.value = false;
}

async function saveQuestionBankRecord() {
  const editingPdfDraft = questionBankPdfEditingIndex.value !== undefined;
  if (!editingPdfDraft) {
    enforceQuestionBankFormOrganizationScope();
  }
  const validationMessage = questionBankValidationMessage(!editingPdfDraft);
  if (validationMessage) {
    message.warning(validationMessage);
    return;
  }
  if (
    questionBankPdfEditingIndex.value !== undefined &&
    questionBankPdfPreview.value
  ) {
    const index = questionBankPdfEditingIndex.value;
    const current = questionBankPdfPreview.value.questions[index];
    if (!current) {
      return;
    }
    questionBankPdfPreview.value.questions[index] = {
      answerExplanation: questionBankForm.answerExplanation,
      caseMaterial: questionBankForm.caseMaterial,
      children: questionBankForm.children.map((child) => ({
        ...child,
        correctAnswers: [...child.correctAnswers],
        options: child.options.map((option) => ({ ...option })),
      })),
      correctAnswers: [...questionBankForm.correctAnswers],
      options: questionBankForm.options.map((option) => ({ ...option })),
      questionText: questionBankForm.questionText,
      questionType: questionBankForm.questionType,
      questionTypeLabel:
        questionTypeOptions.find(
          (option) => option.value === questionBankForm.questionType,
        )?.label || questionBankForm.questionType,
      referenceAnswer: questionBankForm.referenceAnswer,
      score: Number(questionBankForm.score),
      sourceLabel: current.sourceLabel,
    };
    questionBankPdfEditingIndex.value = undefined;
    closeQuestionBankEditor();
    message.success('PDF 解析草稿已更新');
    return;
  }
  questionBankSaving.value = true;
  try {
    if (questionBankEditingId.value) {
      await updateExamQuestionBankApi(
        questionBankEditingId.value,
        questionBankForm,
      );
      message.success('题库试题已更新');
    } else {
      await createExamQuestionBankApi(questionBankForm);
      message.success('题库试题已新建');
    }
    closeQuestionBankEditor();
    await loadQuestionBank();
  } catch {
    message.error('题库试题保存失败');
  } finally {
    questionBankSaving.value = false;
  }
}

function deleteQuestionBank(record: PinganTrainingExamApi.ExamQuestionBank) {
  Modal.confirm({
    content: `确认删除题库试题“${record.questionText}”？`,
    okText: '删除',
    okType: 'danger',
    title: '删除确认',
    async onOk() {
      await deleteExamQuestionBankApi(record.id);
      await loadQuestionBank();
    },
  });
}

function batchDeleteQuestionBank() {
  const ids = selectedQuestionBankRows.value
    .map((row) => row.id)
    .filter((id) => id !== undefined && id !== null);
  if (!ids.length) {
    selectedQuestionBankRowKeys.value = [];
    message.warning('请选择要删除的题库试题');
    return;
  }
  Modal.confirm({
    content: `确认删除已选 ${ids.length} 道题库试题？`,
    okText: '批量删除',
    okType: 'danger',
    title: '批量删除确认',
    async onOk() {
      try {
        await batchDeleteExamQuestionBankApi(ids);
        selectedQuestionBankRowKeys.value = [];
        await loadQuestionBank();
        message.success(`已删除 ${ids.length} 道题库试题`);
      } catch (error) {
        message.error(requestErrorMessage(error, '题库试题批量删除失败'));
        throw error;
      }
    },
  });
}

async function downloadQuestionBankTemplate() {
  try {
    const blob = await downloadExamQuestionBankTemplateApi();
    saveBlob(blob, '宣教培训考试题库_导入模板.xlsx');
  } catch {
    message.error('题库模板下载失败');
  }
}

async function downloadQuestionBank() {
  try {
    const blob = await downloadExamQuestionBankApi(questionBankListParams());
    saveBlob(blob, '宣教培训考试题库.xlsx');
  } catch {
    message.error('题库下载失败');
  }
}

function triggerQuestionBankUpload() {
  questionBankFileInput.value?.click();
}

function triggerQuestionBankPdfUpload() {
  questionBankPdfFileInput.value?.click();
}

function triggerPaperPdfUpload() {
  paperPdfFileInput.value?.click();
}

function requestErrorMessage(error: unknown, fallback: string) {
  const candidate =
    (error as { response?: { data?: { message?: string } } })?.response?.data
      ?.message || (error as { message?: string })?.message;
  return candidate && !candidate.includes('status code') ? candidate : fallback;
}

async function handleQuestionBankWorkbook(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  if (!questionBankFilters.companyId) {
    message.warning('请先选择题库归属公司');
    input.value = '';
    return;
  }
  try {
    const result = await uploadExamQuestionBankWorkbookApi(
      questionBankFilters.companyId,
      questionBankFilters.departmentId,
      file,
      questionBankFilters.teamId,
    );
    message.success(`成功导入 ${result.successRows} 道题库试题`);
    if (result.errors.length > 0) {
      message.warning(result.errors[0]);
    }
    await loadQuestionBank();
  } catch {
    message.error('题库试题导入失败');
  } finally {
    input.value = '';
  }
}

async function handleQuestionBankPdf(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  if (!questionBankFilters.companyId) {
    message.warning('请先选择题库归属公司');
    input.value = '';
    return;
  }
  pdfImportTarget.value = 'questionBank';
  questionBankPdfLoading.value = true;
  try {
    questionBankPdfPreview.value = await previewExamQuestionBankPdfApi(file);
    questionBankPdfOpen.value = true;
    const questionCount = questionBankPdfPreview.value.questions.length;
    message.success(`已解析 ${questionCount} 条题库草稿，请核对后确认入库`);
  } catch (error) {
    message.error(requestErrorMessage(error, 'PDF 试题解析失败'));
  } finally {
    questionBankPdfLoading.value = false;
    input.value = '';
  }
}

async function handlePaperPdf(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  pdfImportTarget.value = 'paper';
  questionBankPdfLoading.value = true;
  try {
    questionBankPdfPreview.value = await previewExamQuestionBankPdfApi(file);
    questionBankPdfOpen.value = true;
    const questionCount = questionBankPdfPreview.value.questions.length;
    message.success(`已解析 ${questionCount} 道试题，请核对后导入当前试卷`);
  } catch (error) {
    message.error(requestErrorMessage(error, 'PDF 试题解析失败'));
  } finally {
    questionBankPdfLoading.value = false;
    input.value = '';
  }
}

function removeQuestionBankPdfDraft(
  record: PinganTrainingExamApi.ExamQuestionBankPdfDraft,
) {
  const index =
    questionBankPdfPreview.value?.questions.findIndex(
      (question) => question === record,
    ) ?? -1;
  if (index >= 0) {
    questionBankPdfPreview.value?.questions.splice(index, 1);
  }
}

async function confirmQuestionBankPdf() {
  if (
    pdfImportTarget.value === 'paper' &&
    questionBankPdfPreview.value?.questions.length
  ) {
    questionBankPdfConfirming.value = true;
    try {
      const result = await confirmExamQuestionBankPdfApi({
        questions: questionBankPdfPreview.value.questions,
        target: 'PAPER',
      });
      paperForm.questions = normalizeQuestions([
        ...paperForm.questions,
        ...result.paperQuestions,
      ]);
      resetPaperCaseExpansion();
      questionBankPdfOpen.value = false;
      questionBankPdfPreview.value = undefined;
      message.success(
        `已将 ${result.successRows} 道试题导入当前试卷，保存试卷后生效`,
      );
    } catch (error) {
      message.error(
        requestErrorMessage(error, 'PDF 试题确认失败，请检查草稿内容'),
      );
    } finally {
      questionBankPdfConfirming.value = false;
    }
    return;
  }
  if (
    !questionBankFilters.companyId ||
    !questionBankPdfPreview.value?.questions.length
  ) {
    message.warning('没有可确认入库的 PDF 试题草稿');
    return;
  }
  questionBankPdfConfirming.value = true;
  try {
    const result = await confirmExamQuestionBankPdfApi({
      companyId: questionBankFilters.companyId,
      departmentId: questionBankFilters.departmentId,
      questions: questionBankPdfPreview.value.questions,
      target: 'QUESTION_BANK',
      teamId: questionBankFilters.teamId,
    });
    message.success(`成功确认导入 ${result.successRows} 条题库试题`);
    questionBankPdfOpen.value = false;
    questionBankPdfPreview.value = undefined;
    await loadQuestionBank();
  } catch (error) {
    message.error(
      requestErrorMessage(error, 'PDF 题库确认入库失败，请检查草稿内容'),
    );
  } finally {
    questionBankPdfConfirming.value = false;
  }
}

function questionBankQuestionToTaskQuestion(
  question: PinganTrainingExamApi.ExamQuestionBank,
): PinganTrainingExamApi.ExamQuestion {
  return {
    actualScore: Number(question.actualScore || 0),
    allOptions: question.allOptions || '',
    answer: question.answer || '',
    answerExplanation: question.answerExplanation || '',
    caseMaterial: question.caseMaterial || '',
    children: question.children.map((child) => ({
      ...child,
      correctAnswers: [...child.correctAnswers],
      options: child.options.map((option) => ({ ...option })),
    })),
    correctAnswers: [...question.correctAnswers],
    options: question.options.map((option) => ({ ...option })),
    questionText: question.questionText,
    questionType: question.questionType,
    questionTypeLabel: question.questionTypeLabel,
    referenceAnswer: question.referenceAnswer || '',
    score: Number(question.score || 0),
    selectedOption: question.selectedOption || '',
    sortOrder: taskForm.questions?.length ?? 0,
  };
}

function questionTypeChinese(question: PinganTrainingExamApi.ExamQuestion) {
  return (
    question.questionTypeLabel ||
    questionTypeOptions.find((option) => option.value === question.questionType)
      ?.label ||
    question.questionType
  );
}

function appendQuestionBankSelection() {
  if (selectedQuestionBankRows.value.length === 0) {
    message.warning('请选择要带入的题库试题');
    return;
  }
  const selectedQuestions = selectedQuestionBankRows.value.map(
    questionBankQuestionToTaskQuestion,
  );
  if (questionBankTarget.value === 'paper') {
    paperForm.questions = normalizeQuestions([
      ...paperForm.questions,
      ...selectedQuestions,
    ]);
    resetPaperCaseExpansion();
  } else {
    taskForm.questions = normalizeQuestions([
      ...(taskForm.questions ?? []),
      ...selectedQuestions,
    ]);
  }
  questionBankOpen.value = false;
  message.success(
    `已带入 ${selectedQuestionBankRows.value.length} 道题库试题到${questionBankTarget.value === 'paper' ? '试卷' : '考试任务'}`,
  );
}

async function downloadTaskQuestions() {
  if (!taskDetail.value?.id) {
    return;
  }
  try {
    const blob = await downloadExamTaskQuestionsApi(taskDetail.value.id);
    saveBlob(blob, `${taskDetail.value.exam || '考试任务'}_试题.xlsx`);
  } catch {
    message.error('考试试题下载失败');
  }
}

function deleteTask(record: PinganTrainingExamApi.ExamTask) {
  Modal.confirm({
    content: `确认删除考试任务“${record.exam}”？`,
    okText: '删除',
    okType: 'danger',
    title: '删除确认',
    async onOk() {
      await deleteExamTaskApi(record.id);
      await loadData();
    },
  });
}

function deleteResult(record: PinganTrainingExamApi.ExamResult) {
  Modal.confirm({
    content: `确认删除“${record.examPersonName}”的考试成绩？`,
    okText: '删除',
    okType: 'danger',
    title: '删除确认',
    async onOk() {
      await deleteExamResultApi(record.id);
      await loadData();
    },
  });
}

function batchDeleteRecords() {
  Modal.confirm({
    content: `确认删除已选 ${selectedRowKeys.value.length} 条${title.value}？`,
    okText: '批量删除',
    okType: 'danger',
    title: '批量删除确认',
    async onOk() {
      if (isTaskMode.value) {
        await batchDeleteExamTasksApi(selectedRowKeys.value);
      } else {
        await batchDeleteExamResultsApi(selectedRowKeys.value);
      }
      await loadData();
    },
  });
}

async function openTaskDetail(record: PinganTrainingExamApi.ExamTask) {
  detailLoading.value = true;
  try {
    const [detail, results] = await Promise.all([
      getExamTaskDetailApi(record.id),
      getExamTaskResultsApi(record.id, {
        page: 1,
        pageSize: 200,
        status: 'all',
      }),
    ]);
    taskDetail.value = { ...detail, results: results.items };
    viewMode.value = 'taskDetail';
  } catch {
    message.error('考试任务明细加载失败');
  } finally {
    detailLoading.value = false;
  }
}

async function openResultDetail(record: PinganTrainingExamApi.ExamResult) {
  detailLoading.value = true;
  try {
    resultDetail.value = await getExamResultDetailApi(record.id);
    viewMode.value = 'resultDetail';
  } catch {
    message.error('人员考试明细加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function goBack() {
  if (viewMode.value === 'resultDetail' && taskDetail.value) {
    viewMode.value = 'taskDetail';
    return;
  }
  viewMode.value = 'list';
}

function statusBoxClass(label?: string) {
  return [
    'status-box',
    label === '已生效' || label === '已考试'
      ? 'status-box--success'
      : 'status-box--warning',
  ];
}

function scoreClass(score?: number) {
  return Number(score || 0) >= 5 ? 'score--ok' : 'score--warn';
}

function formatDisplayDateTime(value?: string) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 19);
}

function asTask(record: Record<string, any>) {
  return record as PinganTrainingExamApi.ExamTask;
}

function asResult(record: Record<string, any>) {
  return record as PinganTrainingExamApi.ExamResult;
}

function asQuestionBank(record: Record<string, any>) {
  return record as PinganTrainingExamApi.ExamQuestionBank;
}

function asExamPaper(record: Record<string, any>) {
  return record as PinganTrainingExamApi.ExamPaper;
}

function asExamQuestion(record: Record<string, any>) {
  return record as PinganTrainingExamApi.ExamQuestion;
}

function asQuestionBankPdfDraft(record: Record<string, any>) {
  return record as PinganTrainingExamApi.ExamQuestionBankPdfDraft;
}

watch(
  () => filters.companyId,
  () => {
    refreshDepartmentOptions();
  },
);

watch([() => taskForm.companyId, () => taskForm.departmentId], () => {
  if (
    selectedPaperId.value &&
    !paperOptions.value.some(
      (option) => String(option.value) === String(selectedPaperId.value),
    )
  ) {
    selectedPaperId.value = undefined;
  }
});

watch(
  () => route.name,
  () => {
    viewMode.value = 'list';
    resetFilters();
  },
);

onMounted(async () => {
  await Promise.all([loadOrganizations(), loadUsers(), loadTaskOptions()]);
  await loadData();
});
</script>

<template>
  <Page auto-content-height content-class="training-exam-page">
    <section class="training-exam-shell">
      <DataMapPanel
        v-if="shouldShowDataMap"
        v-model:selected-keys="selectedOrganizationKeys"
        :tree-data="organizationNodes"
        @select="handleDataMapSelect"
      />

      <section class="training-exam-workbench">
        <template v-if="showList">
          <div class="workbench__bar">
            <div
              v-if="isOrganizationFilterVisible('company')"
              class="filter-item filter-item--company"
            >
              <span>公司</span>
              <Select
                v-model:value="filters.companyId"
                :allow-clear="!isOrganizationFieldLocked('company')"
                :disabled="isOrganizationFieldLocked('company')"
                :options="companyOptions"
                option-filter-prop="label"
                placeholder="点击选择"
                show-search
                @change="handleCompanyChange"
              />
            </div>
            <div
              v-if="isOrganizationFilterVisible('department')"
              class="filter-item"
            >
              <span>部门</span>
              <Select
                v-model:value="filters.departmentId"
                :allow-clear="!isOrganizationFieldLocked('department')"
                :disabled="isOrganizationFieldLocked('department')"
                :options="departmentOptions"
                option-filter-prop="label"
                placeholder="点击选择"
                show-search
                @change="handleDepartmentChange"
              />
            </div>
            <div class="filter-item">
              <span>日期（起）</span>
              <DatePicker
                v-model:value="filters.dateStart"
                placeholder="点击选择"
                value-format="YYYY-MM-DD"
              />
            </div>
            <div class="filter-item">
              <span>日期（止）</span>
              <DatePicker
                v-model:value="filters.dateEnd"
                placeholder="点击选择"
                value-format="YYYY-MM-DD"
              />
            </div>
            <StatusFilterActions
              v-model="filters.status"
              :options="statusOptions"
              @reset="resetFilters"
              @search="loadData"
            />
          </div>

          <div class="table-panel">
            <div class="table-panel__toolbar">
              <div>
                <div class="table-panel__title">{{ title }}</div>
                <div class="table-panel__summary">
                  共 {{ total }} 条记录，按考试日期筛选
                </div>
              </div>
              <div class="table-panel__actions">
                <span class="table-panel__selection">
                  已选 {{ selectedRowKeys.length }} 条
                </span>
                <Button
                  :disabled="!canBatchDelete"
                  v-if="canDeleteCurrent"
                  danger
                  @click="batchDeleteRecords"
                >
                  批量删除
                </Button>
                <Button
                  v-if="isTaskMode && canCreateExamTasks"
                  @click="openPapersPage"
                >
                  <IconifyIcon icon="lucide:files" />
                  试卷管理
                </Button>
                <Button
                  v-if="isTaskMode && canCreateExamTasks"
                  @click="openQuestionBankPage"
                >
                  <IconifyIcon icon="lucide:library" />
                  题库管理
                </Button>
                <Button
                  v-if="canCreateCurrent"
                  type="primary"
                  @click="openCreateEntry"
                >
                  <IconifyIcon icon="lucide:plus" />
                  {{ createTitle }}
                </Button>
              </div>
            </div>

            <Table
              :columns="columns"
              :data-source="rows"
              :loading="loading"
              :pagination="{
                pageSize: 20,
                showSizeChanger: true,
                showTotal: (count) => `共 ${count} 条记录`,
              }"
              :row-selection="tableRowSelection"
              row-key="id"
              size="small"
              bordered
              class="meeting-table"
              :scroll="{
                x: isTaskMode ? 1150 : 1250,
                y: 'calc(100vh - 430px)',
              }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'statusLabel'">
                  <span :class="statusBoxClass(record.statusLabel)">
                    {{ record.statusLabel }}
                  </span>
                </template>
                <template v-else-if="column.dataIndex === 'createdAt'">
                  {{ formatDisplayDateTime(record.createdAt) }}
                </template>
                <template
                  v-else-if="column.dataIndex === 'actions' && isTaskMode"
                >
                  <Space>
                    <Button
                      size="small"
                      type="link"
                      @click="openTaskDetail(asTask(record))"
                    >
                      明细
                    </Button>
                    <Button
                      v-if="canDeleteExamTasks"
                      danger
                      size="small"
                      type="link"
                      @click="deleteTask(asTask(record))"
                    >
                      删除
                    </Button>
                  </Space>
                </template>
                <template v-else-if="column.dataIndex === 'actions'">
                  <Space>
                    <Button
                      size="small"
                      type="link"
                      @click="openResultDetail(asResult(record))"
                    >
                      明细
                    </Button>
                    <Button
                      v-if="canDeleteExamResults"
                      danger
                      size="small"
                      type="link"
                      @click="deleteResult(asResult(record))"
                    >
                      删除
                    </Button>
                  </Space>
                </template>
              </template>
              <template #emptyText>
                <Empty :description="`暂无${title}数据`" />
              </template>
            </Table>
          </div>
        </template>

        <div v-else-if="viewMode === 'papers'" class="detail-panel">
          <div class="detail-panel__head">
            <div class="subpage-heading">
              <Button
                aria-label="返回考试任务"
                class="subpage-back"
                title="返回考试任务"
                type="text"
                @click="goBack"
              >
                <IconifyIcon icon="lucide:arrow-left" />
              </Button>
              <div>
                <h2>试卷管理</h2>
                <div class="table-panel__summary">
                  将题库试题组合为可重复使用的结构化试卷
                </div>
              </div>
            </div>
            <Space>
              <Button
                v-if="canDeleteExamTasks"
                danger
                :disabled="selectedPaperRowKeys.length === 0"
                @click="batchDeletePapers"
              >
                批量删除
              </Button>
              <Button
                v-if="canCreateExamTasks"
                type="primary"
                @click="openPaperCreate"
              >
                <IconifyIcon icon="lucide:plus" />
                新建试卷
              </Button>
            </Space>
          </div>
          <div class="workbench__bar subpage-filter">
            <div class="filter-item filter-item--company">
              <span>公司</span>
              <Select
                v-model:value="paperFilters.companyId"
                allow-clear
                :options="companyOptions"
                option-filter-prop="label"
                placeholder="点击选择"
                show-search
                @change="
                  paperFilters.departmentId = undefined;
                  paperFilters.teamId = undefined;
                "
              />
            </div>
            <div class="filter-item">
              <span>部门</span>
              <Select
                v-model:value="paperFilters.departmentId"
                allow-clear
                :options="departmentsForCompany(paperFilters.companyId)"
                option-filter-prop="label"
                placeholder="点击选择"
                show-search
                @change="paperFilters.teamId = undefined"
              />
            </div>
            <div class="filter-item">
              <span>班组</span>
              <Select
                v-model:value="paperFilters.teamId"
                allow-clear
                :disabled="!paperFilters.departmentId"
                :options="teamsForDepartment(paperFilters.departmentId)"
                option-filter-prop="label"
                placeholder="点击选择"
                show-search
              />
            </div>
            <div class="filter-item filter-item--keyword">
              <span>关键字</span>
              <Input
                v-model:value="paperFilters.keyword"
                placeholder="试卷名称/说明"
              />
            </div>
            <Space class="subpage-filter__actions">
              <Button type="primary" @click="loadPapers">搜索</Button>
              <Button
                @click="
                  resetPaperFilters();
                  loadPapers();
                "
              >
                重置
              </Button>
            </Space>
          </div>
          <section class="section-card section-card--grow">
            <div class="section-title">
              共 {{ paperTotal }} 份试卷，已选
              {{ selectedPaperRowKeys.length }} 份
            </div>
            <Table
              :columns="paperColumns"
              :data-source="paperRows"
              :loading="paperLoading"
              :pagination="{
                pageSize: 20,
                showTotal: (count) => `共 ${count} 条`,
              }"
              :row-selection="paperRowSelection"
              bordered
              class="meeting-table"
              row-key="id"
              size="small"
              table-layout="fixed"
              :scroll="{ y: 'calc(100vh - 410px)' }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'updatedAt'">
                  {{ formatDisplayDateTime(record.updatedAt) }}
                </template>
                <template v-else-if="column.dataIndex === 'actions'">
                  <Space>
                    <Button
                      v-if="canCreateExamTasks"
                      size="small"
                      type="link"
                      @click="openPaperEdit(asExamPaper(record))"
                    >
                      编辑
                    </Button>
                    <Button
                      v-if="canDeleteExamTasks"
                      danger
                      size="small"
                      type="link"
                      @click="deletePaper(asExamPaper(record))"
                    >
                      删除
                    </Button>
                  </Space>
                </template>
              </template>
            </Table>
          </section>
        </div>

        <div v-else-if="viewMode === 'questionBank'" class="detail-panel">
          <div class="detail-panel__head">
            <div class="subpage-heading">
              <Button
                aria-label="返回考试任务"
                class="subpage-back"
                title="返回考试任务"
                type="text"
                @click="goBack"
              >
                <IconifyIcon icon="lucide:arrow-left" />
              </Button>
              <div>
                <h2>题库管理</h2>
                <div class="table-panel__summary">
                  维护结构化试题，支持 Excel 与 PDF 解析导入
                </div>
              </div>
            </div>
          </div>
          <div class="question-bank-panel question-bank-panel--page">
            <div class="question-bank-filter">
              <div class="filter-item filter-item--company">
                <span>公司</span>
                <Select
                  v-model:value="questionBankFilters.companyId"
                  allow-clear
                  :options="companyOptions"
                  option-filter-prop="label"
                  placeholder="点击选择"
                  show-search
                  @change="
                    questionBankFilters.departmentId = undefined;
                    questionBankFilters.teamId = undefined;
                  "
                />
              </div>
              <div class="filter-item">
                <span>部门</span>
                <Select
                  v-model:value="questionBankFilters.departmentId"
                  allow-clear
                  :options="
                    departmentsForCompany(questionBankFilters.companyId)
                  "
                  option-filter-prop="label"
                  placeholder="点击选择"
                  show-search
                  @change="questionBankFilters.teamId = undefined"
                />
              </div>
              <div class="filter-item">
                <span>班组</span>
                <Select
                  v-model:value="questionBankFilters.teamId"
                  allow-clear
                  :disabled="!questionBankFilters.departmentId"
                  :options="
                    teamsForDepartment(questionBankFilters.departmentId)
                  "
                  option-filter-prop="label"
                  placeholder="点击选择"
                  show-search
                />
              </div>
              <div class="filter-item">
                <span>题型</span>
                <Select
                  v-model:value="questionBankFilters.questionType"
                  :options="questionTypeOptions"
                />
              </div>
              <div class="filter-item filter-item--keyword">
                <span>关键字</span>
                <Input
                  v-model:value="questionBankFilters.keyword"
                  placeholder="题干/选项/答案"
                />
              </div>
              <Space>
                <Button type="primary" @click="loadQuestionBank">搜索</Button>
                <Button
                  @click="
                    resetQuestionBankManagementFilters();
                    loadQuestionBank();
                  "
                >
                  重置
                </Button>
              </Space>
            </div>
            <div class="question-bank-toolbar">
              <Space wrap>
                <Button
                  v-if="canCreateExamTasks"
                  type="primary"
                  @click="openQuestionBankCreate"
                >
                  <IconifyIcon icon="lucide:plus" />
                  新建试题
                </Button>
                <Button
                  v-if="canDeleteExamTasks"
                  danger
                  :disabled="selectedQuestionBankRowKeys.length === 0"
                  @click="batchDeleteQuestionBank"
                >
                  批量删除
                </Button>
                <Button
                  v-if="canDownloadExamTasks"
                  @click="downloadQuestionBankTemplate"
                >
                  <IconifyIcon icon="lucide:file-down" />
                  下载模板
                </Button>
                <Button
                  v-if="canCreateExamTasks"
                  @click="triggerQuestionBankUpload"
                >
                  <IconifyIcon icon="lucide:upload" />
                  Excel 导入
                </Button>
                <Button
                  v-if="canCreateExamTasks"
                  :loading="questionBankPdfLoading"
                  @click="triggerQuestionBankPdfUpload"
                >
                  <IconifyIcon icon="lucide:file-search" />
                  PDF 解析
                </Button>
                <Button
                  v-if="canDownloadExamTasks"
                  @click="downloadQuestionBank"
                >
                  <IconifyIcon icon="lucide:download" />
                  下载题库
                </Button>
              </Space>
              <span>
                共 {{ questionBankTotal }} 道题，已选
                {{ selectedQuestionBankRowKeys.length }} 道
              </span>
              <input
                ref="questionBankFileInput"
                accept=".xlsx,.xls"
                class="hidden-input"
                type="file"
                @change="handleQuestionBankWorkbook"
              />
              <input
                ref="questionBankPdfFileInput"
                accept=".pdf,application/pdf"
                class="hidden-input"
                type="file"
                @change="handleQuestionBankPdf"
              />
            </div>
            <Table
              :columns="questionBankColumns"
              :data-source="questionBankRows"
              :loading="questionBankLoading"
              :pagination="{
                pageSize: 20,
                showTotal: (count) => `共 ${count} 条`,
              }"
              row-key="id"
              :row-selection="questionBankRowSelection"
              bordered
              class="meeting-table"
              size="small"
              table-layout="fixed"
              :scroll="{ y: 'calc(100vh - 460px)' }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'questionText'">
                  <span>{{ record.questionText }}</span>
                  <small
                    v-if="record.children?.length"
                    class="case-child-count"
                  >
                    {{ record.children.length }} 道子题
                  </small>
                </template>
                <template v-else-if="column.dataIndex === 'answer'">
                  {{
                    record.questionType === 'SHORT_ANSWER'
                      ? record.referenceAnswer
                      : record.correctAnswers?.join('、') || '-'
                  }}
                </template>
                <template v-else-if="column.dataIndex === 'actions'">
                  <Space>
                    <Button
                      v-if="canCreateExamTasks"
                      size="small"
                      type="link"
                      @click="openQuestionBankEdit(asQuestionBank(record))"
                    >
                      编辑
                    </Button>
                    <Button
                      v-if="canDeleteExamTasks"
                      danger
                      size="small"
                      type="link"
                      @click="deleteQuestionBank(asQuestionBank(record))"
                    >
                      删除
                    </Button>
                  </Space>
                </template>
              </template>
            </Table>
          </div>
        </div>

        <div v-else-if="viewMode === 'createTask'" class="detail-panel">
          <div class="detail-panel__head">
            <h2>新建考试任务</h2>
            <Space>
              <Button @click="goBack">返回</Button>
              <Button
                v-if="canCreateExamTasks"
                :loading="saving"
                @click="saveTask('INACTIVE')"
                >草稿</Button
              >
              <Button
                v-if="canCreateExamTasks"
                :loading="saving"
                type="primary"
                @click="saveTask('ACTIVE')"
              >
                创建
              </Button>
            </Space>
          </div>

          <section class="section-card">
            <div class="section-title">基础信息</div>
            <div class="info-form info-form--six">
              <label>
                <span>公司</span>
                <Select
                  v-model:value="taskForm.companyId"
                  :disabled="isOrganizationFieldLocked('company')"
                  :options="companyOptions"
                  option-filter-prop="label"
                  placeholder="点击选择"
                  show-search
                  @change="
                    taskForm.departmentId = undefined;
                    taskForm.teamId = undefined;
                  "
                />
              </label>
              <label>
                <span>部门（选填）</span>
                <Select
                  v-model:value="taskForm.departmentId"
                  allow-clear
                  :disabled="isOrganizationFieldLocked('department')"
                  :options="departmentsForCompany(taskForm.companyId)"
                  option-filter-prop="label"
                  placeholder="不选则公司全部部门"
                  show-search
                  @change="taskForm.teamId = undefined"
                />
              </label>
              <label>
                <span>班组（选填）</span>
                <Select
                  v-model:value="taskForm.teamId"
                  allow-clear
                  :disabled="!taskForm.departmentId"
                  :options="teamsForDepartment(taskForm.departmentId)"
                  option-filter-prop="label"
                  placeholder="不选则部门全部班组"
                  show-search
                />
              </label>
              <label>
                <span>考试</span>
                <Input
                  v-model:value="taskForm.exam"
                  placeholder="填写考试名称"
                />
              </label>
              <label>
                <span>考试日期</span>
                <DatePicker
                  v-model:value="taskForm.examDate"
                  value-format="YYYY-MM-DD"
                />
              </label>
              <label>
                <span>考试时长（分钟）</span>
                <InputNumber
                  v-model:value="taskForm.durationMinutes"
                  :max="480"
                  :min="1"
                  :precision="0"
                />
              </label>
              <label>
                <span>备注</span>
                <Input
                  v-model:value="taskForm.remark"
                  placeholder="请输入备注"
                />
              </label>
            </div>
            <div class="form-extra-actions">
              考试人员将在创建任务时按所选公司、部门或班组自动生成。
            </div>
          </section>

          <section class="section-card section-card--grow">
            <div class="section-title section-title--with-actions">
              <span>考题</span>
              <Space>
                <Select
                  v-model:value="selectedPaperId"
                  :options="paperOptions"
                  allow-clear
                  class="paper-picker"
                  option-filter-prop="label"
                  placeholder="选择已有试卷"
                  show-search
                />
                <Button
                  v-if="canCreateExamTasks"
                  :disabled="!selectedPaperId"
                  size="small"
                  @click="applySelectedPaper"
                >
                  带入试卷
                </Button>
                <Button
                  v-if="canCreateExamTasks"
                  size="small"
                  @click="openQuestionBankSelector('task')"
                >
                  <IconifyIcon icon="lucide:list-plus" />
                  从题库选择
                </Button>
                <Button
                  v-if="canCreateExamTasks"
                  danger
                  :disabled="selectedTaskQuestionRowKeys.length === 0"
                  size="small"
                  @click="batchRemoveQuestions('task')"
                >
                  批量删除
                </Button>
                <Button
                  v-if="canDownloadExamTasks"
                  size="small"
                  @click="downloadQuestionTemplate"
                >
                  <IconifyIcon icon="lucide:download" />
                  下载模板
                </Button>
                <Button
                  v-if="canCreateExamTasks"
                  size="small"
                  @click="triggerQuestionUpload"
                >
                  <IconifyIcon icon="lucide:upload" />
                  上传数据
                </Button>
                <Button
                  v-if="canCreateExamTasks"
                  size="small"
                  type="primary"
                  @click="addQuestion"
                >
                  <IconifyIcon icon="lucide:plus" />
                  新建
                </Button>
              </Space>
              <input
                ref="questionFileInput"
                accept=".xlsx,.xls"
                class="hidden-input"
                type="file"
                @change="handleQuestionWorkbook"
              />
            </div>
            <Table
              :columns="editableQuestionColumns"
              :data-source="taskForm.questions"
              :pagination="{
                pageSize: 10,
                showTotal: (count) => `共 ${count} 条`,
              }"
              :row-key="questionRowKey"
              :row-selection="taskQuestionRowSelection"
              bordered
              class="meeting-table"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'questionType'">
                  {{ questionTypeChinese(asExamQuestion(record)) }}
                </template>
                <template v-else-if="column.dataIndex === 'questionText'">
                  <Input v-model:value="record.questionText" size="small" />
                  <small
                    v-if="record.children?.length"
                    class="case-child-count"
                  >
                    {{ record.children.length }} 道子题
                  </small>
                </template>
                <Input
                  v-else-if="column.dataIndex === 'selectedOption'"
                  v-model:value="record.selectedOption"
                  size="small"
                />
                <Input
                  v-else-if="column.dataIndex === 'allOptions'"
                  v-model:value="record.allOptions"
                  size="small"
                />
                <Input
                  v-else-if="column.dataIndex === 'answer'"
                  v-model:value="record.answer"
                  size="small"
                />
                <Input
                  v-else-if="column.dataIndex === 'answerExplanation'"
                  v-model:value="record.answerExplanation"
                  size="small"
                />
                <InputNumber
                  v-else-if="column.dataIndex === 'score'"
                  v-model:value="record.score"
                  :max="100"
                  :min="0"
                  :precision="1"
                  size="small"
                />
                <InputNumber
                  v-else-if="column.dataIndex === 'actualScore'"
                  v-model:value="record.actualScore"
                  :max="100"
                  :min="0"
                  :precision="1"
                  size="small"
                />
                <Button
                  v-else-if="column.dataIndex === 'actions'"
                  danger
                  size="small"
                  type="link"
                  @click="removeQuestion('task', asExamQuestion(record))"
                >
                  删除
                </Button>
              </template>
            </Table>
          </section>
        </div>

        <div v-else-if="viewMode === 'taskDetail'" class="detail-panel">
          <div class="detail-panel__head">
            <h2>考试任务明细</h2>
            <Space>
              <Button
                v-if="canDownloadExamTasks"
                @click="downloadTaskQuestions"
              >
                <IconifyIcon icon="lucide:download" />
                下载试题
              </Button>
              <Button @click="goBack">返回</Button>
            </Space>
          </div>
          <section class="section-card">
            <div class="section-title">基础信息</div>
            <div class="info-grid">
              <span>编码</span><b>{{ taskDetail?.code || '-' }}</b>
              <span>公司</span><b>{{ taskDetail?.company || '-' }}</b>
              <span>部门</span><b>{{ taskDetail?.department || '-' }}</b>
              <span>班组</span><b>{{ taskDetail?.team || '-' }}</b>
              <span>考试</span><b>{{ taskDetail?.exam || '-' }}</b>
              <span>考试日期</span><b>{{ taskDetail?.examDate || '-' }}</b>
              <span>考试时长</span
              ><b>{{ taskDetail?.durationMinutes || 30 }} 分钟</b>
              <span>状态</span>
              <b
                ><span :class="statusBoxClass(taskDetail?.statusLabel)">{{
                  taskDetail?.statusLabel || '-'
                }}</span></b
              >
              <span>创建时间</span
              ><b>{{ formatDisplayDateTime(taskDetail?.createdAt) }}</b>
              <span>备注</span><b>{{ taskDetail?.remark || '-' }}</b>
            </div>
          </section>
          <section class="section-card section-card--grow">
            <div class="section-title">考试任务</div>
            <Table
              :columns="departmentDetailColumns"
              :data-source="taskDetail?.results ?? []"
              :loading="detailLoading"
              :pagination="{
                pageSize: 10,
                showTotal: (count) => `共 ${count} 条`,
              }"
              bordered
              class="meeting-table"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'statusLabel'">
                  <span :class="statusBoxClass(record.statusLabel)">
                    {{ record.statusLabel }}
                  </span>
                </template>
                <template v-else-if="column.dataIndex === 'actions'">
                  <Button
                    size="small"
                    type="link"
                    @click="openResultDetail(asResult(record))"
                  >
                    明细
                  </Button>
                </template>
              </template>
            </Table>
          </section>
        </div>

        <div v-else-if="viewMode === 'resultDetail'" class="detail-panel">
          <div class="detail-panel__head">
            <h2>人员考试明细</h2>
            <Space>
              <Button
                v-if="resultDetail?.status === 'PENDING_REVIEW'"
                :loading="reviewingResult"
                type="primary"
                @click="submitResultReview"
              >
                完成人工评分
              </Button>
              <Button @click="goBack">返回</Button>
            </Space>
          </div>
          <section class="section-card">
            <div class="section-title">基础信息</div>
            <div class="info-grid info-grid--person">
              <span>编码</span><b>{{ resultDetail?.code || '-' }}</b>
              <span>公司</span><b>{{ resultDetail?.company || '-' }}</b>
              <span>部门</span><b>{{ resultDetail?.department || '-' }}</b>
              <span>考试人员</span
              ><b>{{ resultDetail?.examPersonName || '-' }}</b> <span>考试</span
              ><b>{{ resultDetail?.exam || '-' }}</b> <span>考分</span
              ><b class="score--warn">{{ resultDetail?.score ?? '-' }} 分</b>
              <span>考试日期</span><b>{{ resultDetail?.examDate || '-' }}</b>
              <span>状态</span>
              <b
                ><span :class="statusBoxClass(resultDetail?.statusLabel)">{{
                  resultDetail?.statusLabel || '-'
                }}</span></b
              >
              <span>创建时间</span
              ><b>{{ formatDisplayDateTime(resultDetail?.createdAt) }}</b>
              <span>备注</span><b>{{ resultDetail?.remark || '-' }}</b>
            </div>
          </section>
          <section class="section-card section-card--grow">
            <div class="section-title">考题</div>
            <Table
              :columns="questionColumns"
              :data-source="resultDetail?.questions ?? []"
              :loading="detailLoading"
              :pagination="{
                pageSize: 10,
                showTotal: (count) => `共 ${count} 条`,
              }"
              bordered
              class="meeting-table"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <span v-if="column.dataIndex === 'questionType'">
                  {{ questionTypeChinese(asExamQuestion(record)) }}
                </span>
                <pre
                  v-else-if="column.dataIndex === 'selectedOption'"
                  class="exam-answer-text"
                  >{{ formatSelectedAnswer(asExamQuestion(record)) }}</pre
                >
                <template v-else-if="column.dataIndex === 'actualScore'">
                  <InputNumber
                    v-if="
                      resultDetail?.status === 'PENDING_REVIEW' &&
                      requiresManualReview(asExamQuestion(record))
                    "
                    v-model:value="record.actualScore"
                    :max="Number(record.score)"
                    :min="0"
                    :precision="1"
                    size="small"
                  />
                  <span v-else :class="scoreClass(record.actualScore)">
                    {{ record.actualScore }}
                  </span>
                </template>
              </template>
            </Table>
            <div class="score-summary">
              <span v-if="resultDetail?.status === 'PENDING_REVIEW'">
                含客观子题的案例已自动计入客观分，请填写该案例最终总得分。
              </span>
              <span>总分：{{ totalQuestionScore }} 分</span>
              <span
                >考分：<b>{{ totalActualScore }} 分</b></span
              >
            </div>
          </section>
        </div>

        <div
          v-if="viewMode === 'paperEdit'"
          class="detail-panel editor-subpage"
        >
          <section class="editor-subpage__body paper-editor-body">
            <div class="training-exam-form paper-form">
              <div class="paper-scope-card">
                <label>
                  <span>适用公司（选填）</span>
                  <Select
                    v-model:value="paperForm.companyId"
                    allow-clear
                    :options="companyOptions"
                    option-filter-prop="label"
                    placeholder="不选则全局共享"
                    show-search
                    @change="
                      paperForm.departmentId = undefined;
                      paperForm.teamId = undefined;
                    "
                  />
                </label>
                <label>
                  <span>适用部门（选填）</span>
                  <Select
                    v-model:value="paperForm.departmentId"
                    allow-clear
                    :disabled="!paperForm.companyId"
                    :options="departmentsForCompany(paperForm.companyId)"
                    option-filter-prop="label"
                    placeholder="不选则公司内共享"
                    show-search
                    @change="paperForm.teamId = undefined"
                  />
                </label>
                <label>
                  <span>适用班组（选填）</span>
                  <Select
                    v-model:value="paperForm.teamId"
                    allow-clear
                    :disabled="!paperForm.departmentId"
                    :options="teamsForDepartment(paperForm.departmentId)"
                    option-filter-prop="label"
                    placeholder="不选则部门内共享"
                    show-search
                  />
                </label>
                <label>
                  <span>试卷名称</span>
                  <Input
                    v-model:value="paperForm.paperName"
                    placeholder="请输入试卷名称"
                  />
                </label>
                <label>
                  <span>说明</span>
                  <Input
                    v-model:value="paperForm.description"
                    placeholder="请输入适用范围或组卷说明"
                  />
                </label>
              </div>
              <div
                class="training-exam-form__wide paper-question-table-region paper-question-card"
              >
                <div class="paper-editor__toolbar">
                  <div class="paper-editor__heading">
                    <Button
                      aria-label="返回试卷管理"
                      class="subpage-back"
                      title="返回试卷管理"
                      type="text"
                      @click="closePaperEditor"
                    >
                      <IconifyIcon icon="lucide:arrow-left" />
                    </Button>
                    <b>试卷考题</b>
                    <span>
                      共 {{ paperForm.questions.length }} 道，合计
                      {{
                        paperForm.questions.reduce(
                          (sum, question) => sum + Number(question.score || 0),
                          0,
                        )
                      }}
                      分
                    </span>
                  </div>
                  <Space>
                    <Button
                      :loading="paperSaving"
                      type="primary"
                      @click="savePaper"
                    >
                      保存试卷
                    </Button>
                    <Button
                      danger
                      :disabled="selectedPaperQuestionRowKeys.length === 0"
                      @click="batchRemoveQuestions('paper')"
                    >
                      批量删除
                    </Button>
                    <Button
                      :loading="
                        questionBankPdfLoading && pdfImportTarget === 'paper'
                      "
                      @click="triggerPaperPdfUpload"
                    >
                      <IconifyIcon icon="lucide:file-search" />
                      PDF 解析导入
                    </Button>
                    <Button
                      type="primary"
                      @click="openQuestionBankSelector('paper')"
                    >
                      <IconifyIcon icon="lucide:list-plus" />
                      从题库选择
                    </Button>
                  </Space>
                  <input
                    ref="paperPdfFileInput"
                    accept=".pdf,application/pdf"
                    class="hidden-input"
                    type="file"
                    @change="handlePaperPdf"
                  />
                </div>
                <Table
                  :columns="paperEditableQuestionColumns"
                  :data-source="paperForm.questions"
                  :expanded-row-keys="expandedPaperQuestionRowKeys"
                  :pagination="false"
                  :row-key="questionRowKey"
                  :row-expandable="paperQuestionRowExpandable"
                  :row-selection="paperQuestionRowSelection"
                  :show-expand-column="false"
                  bordered
                  class="meeting-table paper-question-table"
                  size="small"
                  :scroll="{ x: 1620, y: 'calc(100vh - 430px)' }"
                  @expand="handlePaperQuestionExpand"
                >
                  <template #bodyCell="{ column, index, record }">
                    <span
                      v-if="column.dataIndex === 'questionNo'"
                      class="paper-question-no"
                    >
                      {{ index + 1 }}
                    </span>
                    <Select
                      v-else-if="column.dataIndex === 'questionType'"
                      v-model:value="record.questionType"
                      :options="questionTypeOptions.slice(1)"
                      size="small"
                      @change="changePaperQuestionType(asExamQuestion(record))"
                    />
                    <template v-else-if="column.dataIndex === 'questionText'">
                      <div class="paper-cell-editor">
                        <textarea
                          v-model="record.questionText"
                          class="paper-cell-textarea paper-cell-textarea--fill"
                        ></textarea>
                        <small
                          v-if="record.children?.length"
                          class="case-child-count"
                        >
                          {{ record.children.length }} 道子题
                        </small>
                      </div>
                    </template>
                    <Input
                      v-else-if="column.dataIndex === 'selectedOption'"
                      v-model:value="record.selectedOption"
                      placeholder="选填"
                      size="small"
                    />
                    <div
                      v-else-if="column.dataIndex === 'allOptions'"
                      class="paper-options-editor"
                    >
                      <template
                        v-if="
                          isChoiceQuestionType(record.questionType) &&
                          record.options?.length
                        "
                      >
                        <label
                          v-for="option in record.options"
                          :key="option.key"
                        >
                          <b>{{ option.key }}</b>
                          <Input
                            v-model:value="option.content"
                            size="small"
                            @input="
                              syncPaperQuestionLegacyFields(
                                asExamQuestion(record),
                              )
                            "
                          />
                        </label>
                      </template>
                      <span v-else class="paper-cell-muted">不适用</span>
                    </div>
                    <template v-else-if="column.dataIndex === 'answer'">
                      <Select
                        v-if="isChoiceQuestionType(record.questionType)"
                        v-model:value="record.correctAnswers"
                        :mode="
                          record.questionType === 'MULTIPLE_CHOICE'
                            ? 'multiple'
                            : undefined
                        "
                        :options="answerOptions(record.options || [])"
                        size="small"
                        @change="
                          syncPaperQuestionLegacyFields(asExamQuestion(record))
                        "
                      />
                      <textarea
                        v-else-if="record.questionType === 'SHORT_ANSWER'"
                        v-model="record.referenceAnswer"
                        class="paper-cell-textarea"
                        rows="3"
                        @input="
                          syncPaperQuestionLegacyFields(asExamQuestion(record))
                        "
                      ></textarea>
                      <span v-else class="paper-cell-muted">由子题组成</span>
                    </template>
                    <textarea
                      v-else-if="column.dataIndex === 'answerExplanation'"
                      v-model="record.answerExplanation"
                      class="paper-cell-textarea paper-cell-textarea--fill"
                    ></textarea>
                    <InputNumber
                      v-else-if="column.dataIndex === 'score'"
                      v-model:value="record.score"
                      :disabled="record.questionType === 'CASE_ANALYSIS'"
                      :max="100"
                      :min="0"
                      :precision="1"
                      size="small"
                    />
                    <InputNumber
                      v-else-if="column.dataIndex === 'actualScore'"
                      v-model:value="record.actualScore"
                      :max="100"
                      :min="0"
                      :precision="1"
                      size="small"
                    />
                    <template v-else-if="column.dataIndex === 'actions'">
                      <Button
                        v-if="record.questionType === 'CASE_ANALYSIS'"
                        size="small"
                        type="link"
                        @click="togglePaperCaseEditor(asExamQuestion(record))"
                      >
                        {{
                          paperCaseExpanded(asExamQuestion(record))
                            ? '收起'
                            : '展开'
                        }}
                      </Button>
                      <Button
                        danger
                        size="small"
                        type="link"
                        @click="removeQuestion('paper', asExamQuestion(record))"
                      >
                        删除
                      </Button>
                    </template>
                  </template>
                  <template #expandedRowRender="{ record }">
                    <div
                      v-if="record.questionType === 'CASE_ANALYSIS'"
                      class="paper-case-inline-editor"
                    >
                      <label class="paper-case-inline-editor__material">
                        <span>案例材料</span>
                        <textarea
                          v-model="record.caseMaterial"
                          class="paper-cell-textarea"
                          rows="4"
                        ></textarea>
                      </label>
                      <div
                        v-for="(child, childIndex) in record.children"
                        :key="childIndex"
                        class="paper-case-child-row"
                      >
                        <b>子题 {{ Number(childIndex) + 1 }}</b>
                        <Select
                          v-model:value="child.questionType"
                          :options="childQuestionTypeOptions"
                          size="small"
                          @change="
                            changePaperCaseChildType(
                              asExamQuestion(record),
                              child,
                            )
                          "
                        />
                        <textarea
                          v-model="child.questionText"
                          class="paper-cell-textarea"
                          placeholder="子题题干"
                          rows="2"
                        ></textarea>
                        <div class="paper-case-child-options">
                          <label
                            v-for="option in child.options"
                            :key="option.key"
                          >
                            <b>{{ option.key }}</b>
                            <Input
                              v-model:value="option.content"
                              size="small"
                            />
                          </label>
                          <Select
                            v-if="isChoiceQuestionType(child.questionType)"
                            v-model:value="child.correctAnswers"
                            :mode="
                              child.questionType === 'MULTIPLE_CHOICE'
                                ? 'multiple'
                                : undefined
                            "
                            :options="answerOptions(child.options)"
                            placeholder="正确答案"
                            size="small"
                          />
                          <textarea
                            v-else
                            v-model="child.referenceAnswer"
                            class="paper-cell-textarea"
                            placeholder="参考答案"
                            rows="2"
                          ></textarea>
                        </div>
                        <textarea
                          v-model="child.answerExplanation"
                          class="paper-cell-textarea"
                          placeholder="答案解析"
                          rows="2"
                        ></textarea>
                        <InputNumber
                          v-model:value="child.score"
                          :max="100"
                          :min="0"
                          :precision="1"
                          size="small"
                          @change="
                            syncPaperQuestionLegacyFields(
                              asExamQuestion(record),
                            )
                          "
                        />
                      </div>
                    </div>
                  </template>
                </Table>
              </div>
            </div>
          </section>
        </div>

        <Modal
          v-model:open="questionBankOpen"
          title="选择题库试题"
          width="1180px"
          :footer="null"
        >
          <div class="question-bank-panel">
            <div class="question-bank-filter">
              <div class="filter-item filter-item--company">
                <span>公司</span>
                <Select
                  v-model:value="questionBankFilters.companyId"
                  allow-clear
                  :options="companyOptions"
                  option-filter-prop="label"
                  placeholder="点击选择"
                  show-search
                  @change="
                    questionBankFilters.departmentId = undefined;
                    questionBankFilters.teamId = undefined;
                  "
                />
              </div>
              <div class="filter-item">
                <span>部门</span>
                <Select
                  v-model:value="questionBankFilters.departmentId"
                  allow-clear
                  :options="
                    departmentsForCompany(questionBankFilters.companyId)
                  "
                  option-filter-prop="label"
                  placeholder="点击选择"
                  show-search
                  @change="questionBankFilters.teamId = undefined"
                />
              </div>
              <div class="filter-item">
                <span>班组</span>
                <Select
                  v-model:value="questionBankFilters.teamId"
                  allow-clear
                  :disabled="!questionBankFilters.departmentId"
                  :options="
                    teamsForDepartment(questionBankFilters.departmentId)
                  "
                  option-filter-prop="label"
                  placeholder="点击选择"
                  show-search
                />
              </div>
              <div class="filter-item">
                <span>题型</span>
                <Select
                  v-model:value="questionBankFilters.questionType"
                  :options="questionTypeOptions"
                />
              </div>
              <div class="filter-item filter-item--keyword">
                <span>关键字</span>
                <Input
                  v-model:value="questionBankFilters.keyword"
                  placeholder="题干/选项/答案"
                />
              </div>
              <Space>
                <Button type="primary" @click="loadQuestionBank">搜索</Button>
                <Button @click="resetAndLoadQuestionBank"> 重置 </Button>
              </Space>
            </div>

            <div class="question-bank-toolbar">
              <Space>
                <Button
                  v-if="canCreateExamTasks"
                  type="primary"
                  @click="openQuestionBankCreate"
                >
                  <IconifyIcon icon="lucide:plus" />
                  新建试题
                </Button>
                <Button
                  v-if="canDownloadExamTasks"
                  @click="downloadQuestionBankTemplate"
                >
                  <IconifyIcon icon="lucide:file-down" />
                  下载模板
                </Button>
                <Button
                  v-if="canCreateExamTasks"
                  @click="triggerQuestionBankUpload"
                >
                  <IconifyIcon icon="lucide:upload" />
                  Excel 导入
                </Button>
                <Button
                  v-if="canCreateExamTasks"
                  :loading="questionBankPdfLoading"
                  @click="triggerQuestionBankPdfUpload"
                >
                  <IconifyIcon icon="lucide:file-search" />
                  PDF 解析
                </Button>
                <Button
                  v-if="canDownloadExamTasks"
                  @click="downloadQuestionBank"
                >
                  <IconifyIcon icon="lucide:download" />
                  下载题库
                </Button>
                <Button
                  v-if="canCreateExamTasks"
                  :disabled="selectedQuestionBankRows.length === 0"
                  @click="appendQuestionBankSelection"
                >
                  <IconifyIcon icon="lucide:list-plus" />
                  带入{{ questionBankTarget === 'paper' ? '试卷' : '考试任务' }}
                </Button>
              </Space>
              <span>
                共 {{ questionBankTotal }} 道题，已选
                {{ selectedQuestionBankRowKeys.length }} 道
              </span>
              <input
                ref="questionBankFileInput"
                accept=".xlsx,.xls"
                class="hidden-input"
                type="file"
                @change="handleQuestionBankWorkbook"
              />
              <input
                ref="questionBankPdfFileInput"
                accept=".pdf,application/pdf"
                class="hidden-input"
                type="file"
                @change="handleQuestionBankPdf"
              />
            </div>

            <Table
              :columns="questionBankColumns"
              :data-source="questionBankRows"
              :loading="questionBankLoading"
              :pagination="{
                pageSize: 20,
                showTotal: (count) => `共 ${count} 条`,
              }"
              row-key="id"
              :row-selection="questionBankRowSelection"
              bordered
              class="meeting-table"
              size="small"
              :scroll="{ x: 1350, y: 420 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'questionText'">
                  <span>{{ record.questionText }}</span>
                  <small
                    v-if="record.children?.length"
                    class="case-child-count"
                  >
                    {{ record.children.length }} 道子题
                  </small>
                </template>
                <template v-else-if="column.dataIndex === 'answer'">
                  {{
                    record.questionType === 'SHORT_ANSWER'
                      ? record.referenceAnswer
                      : record.correctAnswers?.join('、') || '-'
                  }}
                </template>
                <template v-else-if="column.dataIndex === 'actions'">
                  <Space>
                    <Button
                      v-if="canCreateExamTasks"
                      size="small"
                      type="link"
                      @click="openQuestionBankEdit(asQuestionBank(record))"
                    >
                      编辑
                    </Button>
                    <Button
                      v-if="canDeleteExamTasks"
                      danger
                      size="small"
                      type="link"
                      @click="deleteQuestionBank(asQuestionBank(record))"
                    >
                      删除
                    </Button>
                  </Space>
                </template>
              </template>
            </Table>
          </div>
        </Modal>

        <Modal
          v-model:open="questionBankPdfOpen"
          :confirm-loading="questionBankPdfConfirming"
          :ok-button-props="{
            disabled: !questionBankPdfPreview?.questions.length,
          }"
          cancel-text="稍后处理"
          :ok-text="
            pdfImportTarget === 'paper' ? '导入当前试卷' : '确认导入题库'
          "
          :title="
            pdfImportTarget === 'paper'
              ? '当前试卷 PDF 解析预览'
              : 'PDF 试题解析预览'
          "
          width="1100px"
          @ok="confirmQuestionBankPdf"
        >
          <div v-if="questionBankPdfPreview" class="pdf-preview">
            <div class="pdf-preview__summary">
              <b>{{ questionBankPdfPreview.filename }}</b>
              <span>{{ questionBankPdfPreview.pageCount }} 页</span>
              <span>
                {{ questionBankPdfPreview.questions.length }} 条解析草稿
              </span>
              <span>
                {{ questionBankPdfPreview.characterCount }} 个文本字符
              </span>
            </div>
            <div
              v-for="warning in questionBankPdfPreview.warnings"
              :key="warning"
              class="pdf-preview__message pdf-preview__message--warning"
            >
              {{ warning }}
            </div>
            <div
              v-for="error in questionBankPdfPreview.errors"
              :key="error"
              class="pdf-preview__message pdf-preview__message--error"
            >
              {{ error }}
            </div>
            <Table
              :columns="questionBankPdfColumns"
              :data-source="questionBankPdfPreview.questions"
              :pagination="{ pageSize: 10 }"
              :row-key="(record) => record.sourceLabel"
              bordered
              size="small"
              :scroll="{ x: 820, y: 480 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'questionText'">
                  <span>{{ record.questionText }}</span>
                  <small
                    v-if="record.children?.length"
                    class="case-child-count"
                  >
                    {{ record.children.length }} 道子题
                  </small>
                </template>
                <template v-else-if="column.dataIndex === 'actions'">
                  <Space>
                    <Button
                      size="small"
                      type="link"
                      @click="
                        openQuestionBankPdfDraft(asQuestionBankPdfDraft(record))
                      "
                    >
                      校正
                    </Button>
                    <Button
                      danger
                      size="small"
                      type="link"
                      @click="
                        removeQuestionBankPdfDraft(
                          asQuestionBankPdfDraft(record),
                        )
                      "
                    >
                      移除
                    </Button>
                  </Space>
                </template>
              </template>
            </Table>
          </div>
        </Modal>

        <div
          v-if="viewMode === 'questionBankEdit'"
          class="detail-panel editor-subpage"
        >
          <div class="detail-panel__head">
            <div class="subpage-heading">
              <Button
                :aria-label="questionBankEditorReturnLabel"
                class="subpage-back"
                :title="questionBankEditorReturnLabel"
                type="text"
                @click="closeQuestionBankEditor"
              >
                <IconifyIcon icon="lucide:arrow-left" />
              </Button>
              <div>
                <h2>{{ questionBankEditorTitle }}</h2>
                <div class="table-panel__summary">
                  维护题型、选项、答案与组织适用范围
                </div>
              </div>
            </div>
            <Space>
              <Button
                :loading="questionBankSaving"
                type="primary"
                @click="saveQuestionBankRecord"
              >
                保存试题
              </Button>
            </Space>
          </div>
          <section class="section-card editor-subpage__body">
            <div class="training-exam-form training-exam-form--question-bank">
              <div class="question-bank-org-row">
                <label>
                  <span>公司</span>
                  <Select
                    v-model:value="questionBankForm.companyId"
                    :disabled="isOrganizationFieldLocked('company')"
                    :options="companyOptions"
                    option-filter-prop="label"
                    placeholder="点击选择"
                    show-search
                    @change="
                      questionBankForm.departmentId = undefined;
                      questionBankForm.teamId = undefined;
                    "
                  />
                </label>
                <label>
                  <span>部门（选填）</span>
                  <Select
                    v-model:value="questionBankForm.departmentId"
                    allow-clear
                    :disabled="isOrganizationFieldLocked('department')"
                    :options="departmentsForCompany(questionBankForm.companyId)"
                    option-filter-prop="label"
                    placeholder="不选则公司共享"
                    show-search
                    @change="questionBankForm.teamId = undefined"
                  />
                </label>
                <label>
                  <span>班组（选填）</span>
                  <Select
                    v-model:value="questionBankForm.teamId"
                    allow-clear
                    :disabled="!questionBankForm.departmentId"
                    :options="teamsForDepartment(questionBankForm.departmentId)"
                    option-filter-prop="label"
                    placeholder="不选则部门共享"
                    show-search
                  />
                </label>
              </div>
              <label>
                <span>题型</span>
                <Select
                  v-model:value="questionBankForm.questionType"
                  :options="
                    questionTypeOptions.filter((item) => item.value !== 'all')
                  "
                  @change="changeQuestionBankType"
                />
              </label>
              <label
                class="question-bank-title-field"
                :class="{
                  'training-exam-form__wide':
                    questionBankForm.questionType !== 'CASE_ANALYSIS',
                }"
              >
                <span>{{
                  questionBankForm.questionType === 'CASE_ANALYSIS'
                    ? '案例标题'
                    : '考题'
                }}</span>
                <Input
                  v-model:value="questionBankForm.questionText"
                  :placeholder="
                    questionBankForm.questionType === 'CASE_ANALYSIS'
                      ? '填写案例标题'
                      : '填写题干'
                  "
                />
              </label>
              <div
                v-if="
                  questionBankForm.questionType === 'SINGLE_CHOICE' ||
                  questionBankForm.questionType === 'MULTIPLE_CHOICE'
                "
                class="training-exam-form__wide question-editor"
              >
                <span class="question-editor__title">
                  {{
                    questionBankForm.questionType === 'SINGLE_CHOICE'
                      ? '选项（固定 A-D）'
                      : '选项（A-D，可兼容 A-E）'
                  }}
                </span>
                <label
                  v-for="option in questionBankForm.options"
                  :key="option.key"
                  class="question-option-row"
                >
                  <b>{{ option.key }}</b>
                  <Input
                    v-model:value="option.content"
                    :placeholder="`请输入选项 ${option.key} 内容`"
                  />
                </label>
                <label class="question-answer-row">
                  <span>正确答案</span>
                  <Select
                    v-model:value="questionBankForm.correctAnswers"
                    :options="answerOptions(questionBankForm.options)"
                    mode="multiple"
                    placeholder="请选择正确答案"
                  />
                </label>
              </div>
              <label
                v-if="questionBankForm.questionType === 'SHORT_ANSWER'"
                class="training-exam-form__wide"
              >
                <span>参考答案</span>
                <textarea
                  v-model="questionBankForm.referenceAnswer"
                  class="question-textarea"
                  placeholder="请输入参考答案"
                ></textarea>
              </label>
              <template
                v-if="questionBankForm.questionType === 'CASE_ANALYSIS'"
              >
                <label class="training-exam-form__wide">
                  <span>案例材料</span>
                  <textarea
                    v-model="questionBankForm.caseMaterial"
                    class="question-textarea"
                    placeholder="请输入案例背景、经过和已知条件"
                  ></textarea>
                </label>
                <div class="training-exam-form__wide case-question-editor">
                  <div class="case-question-editor__head">
                    <b>案例子题（可混合单选、多选和问答）</b>
                    <Button
                      size="small"
                      type="primary"
                      @click="addQuestionBankChild"
                      >添加子题</Button
                    >
                  </div>
                  <div
                    v-for="(child, childIndex) in questionBankForm.children"
                    :key="childIndex"
                    class="case-question-card"
                  >
                    <div class="case-question-card__head">
                      <b>子题 {{ childIndex + 1 }}</b>
                      <Button
                        danger
                        size="small"
                        type="link"
                        @click="removeQuestionBankChild(childIndex)"
                        >删除</Button
                      >
                    </div>
                    <Select
                      v-model:value="child.questionType"
                      :options="childQuestionTypeOptions"
                      @change="changeQuestionBankChildType(child)"
                    />
                    <Input
                      v-model:value="child.questionText"
                      placeholder="请输入子题题干"
                    />
                    <template
                      v-if="
                        child.questionType === 'SINGLE_CHOICE' ||
                        child.questionType === 'MULTIPLE_CHOICE'
                      "
                    >
                      <label
                        v-for="option in child.options"
                        :key="option.key"
                        class="question-option-row"
                      >
                        <b>{{ option.key }}</b>
                        <Input
                          v-model:value="option.content"
                          :placeholder="`请输入选项 ${option.key} 内容`"
                        />
                      </label>
                      <Select
                        v-model:value="child.correctAnswers"
                        :options="answerOptions(child.options)"
                        mode="multiple"
                        placeholder="请选择正确答案"
                      />
                    </template>
                    <textarea
                      v-else
                      v-model="child.referenceAnswer"
                      class="question-textarea"
                      placeholder="请输入参考答案"
                    ></textarea>
                    <Input
                      v-model:value="child.answerExplanation"
                      placeholder="答案解析（选填）"
                    />
                    <label class="case-question-score">
                      <span>分值</span>
                      <InputNumber
                        v-model:value="child.score"
                        :max="100"
                        :min="0.1"
                        :precision="1"
                      />
                    </label>
                  </div>
                </div>
              </template>
              <label v-if="questionBankForm.questionType !== 'CASE_ANALYSIS'">
                <span>分值</span>
                <InputNumber
                  v-model:value="questionBankForm.score"
                  :max="100"
                  :min="0.1"
                  :precision="1"
                />
              </label>
              <label class="training-exam-form__wide">
                <span>答案解析</span>
                <textarea
                  v-model="questionBankForm.answerExplanation"
                  class="question-textarea"
                  placeholder="请输入答案依据或解题说明（选填）"
                ></textarea>
              </label>
            </div>
          </section>
        </div>

        <Modal
          v-model:open="resultCreateOpen"
          :confirm-loading="saving"
          title="新增考试成绩"
          width="720px"
          @ok="saveResultRecord"
        >
          <div class="training-exam-form">
            <label>
              <span>考试任务</span>
              <Select
                v-model:value="resultForm.taskId"
                :options="examTaskOptions"
                option-filter-prop="label"
                placeholder="选择考试任务"
                show-search
              />
            </label>
            <div v-if="selectedTaskPreview" class="selected-task-preview">
              <span>{{ selectedTaskPreview.company }}</span>
              <span>{{ selectedTaskPreview.department }}</span>
              <span>{{ selectedTaskPreview.exam }}</span>
              <span>{{ selectedTaskPreview.examDate }}</span>
            </div>
            <label>
              <span>考试人员</span>
              <Select
                v-model:value="resultForm.examPersonUserId"
                :options="userOptions"
                option-filter-prop="label"
                placeholder="选择考试人员"
                show-search
              />
            </label>
            <label>
              <span>考分</span>
              <InputNumber
                v-model:value="resultForm.score"
                :max="100"
                :min="0"
                :precision="1"
              />
            </label>
            <label>
              <span>状态</span>
              <b>
                <span :class="statusBoxClass(derivedResultStatusLabel)">
                  {{ derivedResultStatusLabel }}
                </span>
              </b>
            </label>
          </div>
        </Modal>
      </section>
    </section>
  </Page>
</template>

<style scoped>
:deep(.training-exam-page) {
  height: 100%;
}

.training-exam-shell {
  display: flex;
  gap: 12px;
  height: calc(100vh - 128px);
  min-height: 640px;
}

.training-exam-workbench {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.workbench__bar,
.table-panel,
.detail-panel {
  background: #fff;
  border: 1px solid #d9e4f5;
  border-radius: 6px;
}

.workbench__bar {
  display: flex;
  flex-wrap: nowrap;
  gap: 10px;
  align-items: flex-end;
  padding: 12px 14px;
  margin-bottom: 12px;
  overflow: hidden;
}

.filter-item {
  display: flex;
  flex: 1 1 126px;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.filter-item span,
.info-form label span,
.training-exam-form label span {
  color: #475569;
  font-size: 13px;
}

.workbench__bar .filter-item > span {
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
}

.filter-item :deep(.ant-picker),
.filter-item :deep(.ant-select),
.info-form :deep(.ant-picker),
.info-form :deep(.ant-select),
.info-form :deep(.ant-input),
.training-exam-form :deep(.ant-picker),
.training-exam-form :deep(.ant-select),
.training-exam-form :deep(.ant-input-number),
.training-exam-form :deep(.ant-input) {
  width: 100%;
}

.workbench__bar .filter-item :deep(.ant-picker),
.workbench__bar .filter-item :deep(.ant-select),
.workbench__bar .filter-item :deep(.ant-input) {
  height: 40px;
}

.subpage-filter__actions :deep(.ant-btn) {
  min-width: 64px;
  height: 40px;
}

.workbench__bar .filter-item :deep(.ant-select-single .ant-select-selector),
.workbench__bar .filter-item :deep(.ant-picker) {
  height: 40px;
}

.workbench__bar .filter-item :deep(.ant-select-single .ant-select-selector),
.workbench__bar .filter-item :deep(.ant-select-selection-item),
.workbench__bar .filter-item :deep(.ant-select-selection-placeholder),
.workbench__bar .filter-item :deep(.ant-picker-input) {
  display: flex;
  align-items: center;
}

.workbench__bar .filter-item :deep(.ant-select-selection-item),
.workbench__bar .filter-item :deep(.ant-select-selection-placeholder),
.workbench__bar .filter-item :deep(.ant-picker-input) {
  height: 100%;
}

.workbench__bar .filter-item :deep(.ant-select-selection-item),
.workbench__bar .filter-item :deep(.ant-select-selection-placeholder),
.workbench__bar .filter-item :deep(.ant-picker-input > input) {
  line-height: normal !important;
}

.table-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: 14px;
}

.table-panel__toolbar,
.detail-panel__head {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.table-panel__toolbar {
  margin-bottom: 12px;
}

.table-panel__title,
.detail-panel__head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.subpage-heading {
  display: flex;
  gap: 6px;
  align-items: flex-start;
}

.subpage-back {
  display: inline-flex;
  flex: 0 0 30px;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  margin-left: -6px;
  color: #475569;
  border-radius: 4px;
}

.subpage-back:hover,
.subpage-back:focus-visible {
  color: #1677ff;
  background: #eef6ff;
}

.subpage-back :deep(svg) {
  width: 18px;
  height: 18px;
}

.table-panel__summary,
.table-panel__selection {
  font-size: 13px;
  color: #64748b;
}

.table-panel__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.detail-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  padding: 12px;
}

.section-card {
  overflow: hidden;
  border: 1px solid #dbe7f6;
  border-radius: 6px;
}

.section-card--grow {
  flex: 1;
  min-height: 0;
}

.editor-subpage__body {
  flex: 1;
  min-height: 0;
  padding: 16px;
  overflow: auto;
}

.section-title {
  display: flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  font-size: 14px;
  font-weight: 700;
  color: #1e3a8a;
  background: #eef6ff;
  border-bottom: 1px solid #dbe7f6;
}

.section-title--with-actions {
  justify-content: space-between;
}

.info-form {
  display: grid;
  gap: 12px;
  padding: 14px;
}

.info-form--six {
  grid-template-columns: repeat(6, minmax(120px, 1fr));
}

.info-form label,
.training-exam-form label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-extra-actions {
  padding: 0 14px 14px;
}

.info-grid {
  display: grid;
  grid-template-columns:
    90px minmax(120px, 1fr) 90px minmax(120px, 1fr)
    90px minmax(120px, 1fr) 90px minmax(120px, 1fr);
  gap: 10px 12px;
  padding: 14px;
  font-size: 13px;
}

.info-grid span {
  color: #64748b;
}

.info-grid b {
  min-width: 0;
  overflow: hidden;
  font-weight: 600;
  color: #1f2937;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-grid--person {
  grid-template-columns:
    90px minmax(110px, 1fr) 90px minmax(110px, 1fr)
    90px minmax(110px, 1fr) 90px minmax(110px, 1fr) 90px minmax(110px, 1fr);
}

.hidden-input {
  display: none;
}

.status-box {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 600;
  border-radius: 4px;
}

.status-box--success {
  color: #047857;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
}

.status-box--warning {
  color: #b45309;
  background: #fffbeb;
  border: 1px solid #fde68a;
}

.score--ok {
  font-weight: 700;
  color: #16a34a;
}

.score--warn {
  font-weight: 700;
  color: #ef4444;
}

.score-summary {
  display: flex;
  justify-content: flex-end;
  gap: 36px;
  padding: 12px 18px;
  margin: 12px 14px 0 auto;
  font-size: 15px;
  color: #111827;
  background: #f8fbff;
  border: 1px solid #dbe7f6;
  border-radius: 6px;
}

.score-summary b {
  color: #ef4444;
}

.question-bank-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-bank-panel--page {
  flex: 1;
  min-height: 0;
}

.subpage-filter {
  margin-bottom: 0;
}

.paper-picker {
  width: 260px;
}

.paper-editor__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
}

.paper-editor__toolbar > div {
  display: flex;
  gap: 12px;
  align-items: center;
}

.paper-editor__toolbar > div:first-child {
  flex-shrink: 0;
}

.paper-editor__heading {
  gap: 6px !important;
}

.paper-editor__toolbar :deep(.ant-space) {
  flex-wrap: wrap;
  justify-content: flex-end;
  margin-left: auto;
}

.paper-editor__toolbar span {
  font-size: 13px;
  color: #64748b;
}

.paper-editor__toolbar b {
  font-size: 16px;
  color: #172033;
  white-space: nowrap;
}

.question-bank-filter,
.question-bank-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: end;
}

.question-bank-toolbar {
  justify-content: space-between;
  font-size: 13px;
  color: #64748b;
}

.training-exam-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.paper-form {
  grid-template-columns: repeat(3, minmax(0, 1fr)) minmax(0, 1.15fr) minmax(
      0,
      1.35fr
    );
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  width: 100%;
  height: 100%;
}

.paper-scope-card {
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns:
    repeat(3, minmax(0, 1fr)) minmax(0, 1.15fr)
    minmax(0, 1.35fr);
  gap: 10px;
  padding: 14px;
  background: #fff;
  border: 1px solid #dbe7f6;
  border-radius: 6px;
}

.paper-editor-body {
  display: flex;
  overflow: hidden;
}

.paper-question-card {
  padding: 0 12px 12px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #dbe7f6;
  border-radius: 6px;
}

.paper-question-table-region {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.paper-question-table {
  flex: 1;
  min-height: 0;
}

.paper-question-table :deep(.ant-spin-nested-loading),
.paper-question-table :deep(.ant-spin-container),
.paper-question-table :deep(.ant-table),
.paper-question-table :deep(.ant-table-container) {
  height: 100%;
  min-height: 0;
}

.paper-question-table :deep(.ant-spin-container),
.paper-question-table :deep(.ant-table),
.paper-question-table :deep(.ant-table-container) {
  display: flex;
  flex-direction: column;
}

.paper-question-table :deep(.ant-table-body) {
  flex: 1;
  min-height: 240px;
}

.paper-question-table :deep(.ant-table-cell) {
  vertical-align: top;
}

.paper-question-no {
  display: block;
  padding-top: 5px;
  font-weight: 700;
  color: #334155;
  text-align: center;
}

.paper-question-table
  :deep(.ant-table-tbody > tr:not(.ant-table-expanded-row) > .ant-table-cell) {
  height: 1px;
}

.paper-question-table :deep(.ant-select),
.paper-question-table :deep(.ant-input-number) {
  width: 100%;
}

.paper-cell-editor {
  display: flex;
  flex-direction: column;
  gap: 4px;
  height: 100%;
}

.paper-cell-textarea {
  width: 100%;
  min-height: 64px;
  padding: 5px 8px;
  color: #1f2937;
  resize: vertical;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  outline: none;
  box-sizing: border-box;
}

.paper-cell-textarea--fill {
  height: 100%;
  min-height: 100%;
  resize: none;
}

.paper-cell-editor .paper-cell-textarea--fill {
  flex: 1;
  min-height: 64px;
}

.paper-cell-textarea:focus {
  border-color: #4096ff;
  box-shadow: 0 0 0 2px rgb(5 145 255 / 10%);
}

.paper-options-editor,
.paper-case-child-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.paper-options-editor label,
.paper-case-child-options label {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 4px;
  align-items: center;
}

.paper-options-editor label b,
.paper-case-child-options label b {
  color: #1d4ed8;
  text-align: center;
}

.paper-cell-muted {
  color: #94a3b8;
}

.paper-case-inline-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: clamp(220px, calc(100vh - 470px), 560px);
  padding: 12px;
  overflow: auto;
  background: #f8fbff;
}

.paper-case-inline-editor__material {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.paper-case-child-row {
  display: grid;
  grid-template-columns:
    62px 120px minmax(220px, 1.2fr) minmax(240px, 1.4fr)
    minmax(180px, 1fr) 90px;
  gap: 8px;
  align-items: start;
  padding: 10px;
  background: #fff;
  border: 1px solid #dbe7f6;
  border-radius: 4px;
}

.paper-case-child-row > b {
  padding-top: 5px;
  color: #1e3a8a;
  font-size: 13px;
}

.training-exam-form__wide {
  grid-column: 1 / -1;
}

.question-bank-org-row {
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.question-bank-title-field {
  min-width: 0;
}

.question-editor,
.case-question-editor {
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.question-editor__title {
  display: block;
  margin-bottom: 10px;
  color: #374151;
  font-weight: 600;
}

.question-option-row {
  display: grid !important;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.question-option-row b {
  color: #1677ff;
  text-align: center;
}

.question-answer-row {
  display: grid !important;
  grid-template-columns: 80px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.question-textarea {
  min-height: 88px;
  padding: 8px 11px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  outline: none;
  resize: vertical;
}

.question-textarea:focus {
  border-color: #4096ff;
  box-shadow: 0 0 0 2px rgb(5 145 255 / 10%);
}

.case-question-editor__head,
.case-question-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.case-question-card {
  display: grid;
  gap: 10px;
  padding: 14px;
  margin-top: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #fff;
}

.case-question-score {
  display: flex !important;
  gap: 10px;
  align-items: center;
}

.case-child-count {
  display: block;
  margin-top: 4px;
  color: #1677ff;
}

.pdf-preview {
  display: grid;
  gap: 10px;
}

.pdf-preview__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  padding: 12px;
  color: #334155;
  background: #f8fafc;
  border-radius: 8px;
}

.pdf-preview__message {
  padding: 8px 12px;
  border-radius: 6px;
}

.pdf-preview__message--warning {
  color: #854d0e;
  background: #fefce8;
  border: 1px solid #fde68a;
}

.pdf-preview__message--error {
  color: #991b1b;
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.selected-task-preview {
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
  font-size: 13px;
  color: #334155;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.selected-task-preview span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meeting-table :deep(.ant-table-thead > tr > th) {
  color: #1e3a8a;
  background: #eef6ff;
}

.meeting-table :deep(.ant-input-number) {
  width: 100%;
}

.exam-answer-text {
  margin: 0;
  font-family: inherit;
  line-height: 1.6;
  white-space: pre-wrap;
}

@media (max-width: 1200px) {
  .info-form--six {
    grid-template-columns: repeat(3, minmax(160px, 1fr));
  }

  .info-grid,
  .info-grid--person {
    grid-template-columns: 90px minmax(160px, 1fr) 90px minmax(160px, 1fr);
  }
}

@media (max-width: 960px) {
  .training-exam-shell {
    flex-direction: column;
    height: auto;
  }

  .filter-item,
  .filter-item--company {
    flex-basis: calc(50% - 5px);
  }

  .workbench__bar {
    flex-wrap: wrap;
    overflow: visible;
  }

  .info-form--six,
  .training-exam-form,
  .selected-task-preview {
    grid-template-columns: 1fr;
  }
}
</style>
