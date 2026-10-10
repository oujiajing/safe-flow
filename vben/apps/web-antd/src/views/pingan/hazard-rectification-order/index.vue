<script setup lang="ts">
import type { TableColumnsType } from 'ant-design-vue';

import type { PinganHazardRectificationOrderApi } from '#/api/pingan/hazard-rectification-order';
import type { PinganPreShiftMeetingApi } from '#/api/pingan/pre-shift-meeting';
import type { SystemManagementApi } from '#/api/system-management/types';
import type { PinganOrganizationFilterKey } from '#/views/pingan/shared/organization-filter-permission';

import { computed, onMounted, reactive, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { useAccessStore, useUserStore } from '@vben/stores';

import {
  AutoComplete,
  Button,
  DatePicker,
  Drawer,
  Input,
  message,
  Modal,
  Select,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  actionHazardRectificationOrderApi,
  batchDeleteHazardRectificationOrdersApi,
  confirmHazardRectificationSummaryApi,
  createHazardRectificationOrderApi,
  editHazardRectificationSummaryApi,
  getHazardRectificationOrderDetailApi,
  getHazardRectificationOrdersApi,
  reviewHazardRectificationOrderWithAgentApi,
  summarizeHazardRectificationOrderWithAgentApi,
  uploadHazardRectificationOrderAttachmentApi,
} from '#/api/pingan/hazard-rectification-order';
import {
  getPinganOrgTreeApi,
  getPinganUsersApi,
} from '#/api/pingan/pre-shift-meeting';
import { getLatestQuickShotAgentRunApi } from '#/api/pingan/three-check-record';
import { resolveAttachmentPreviewUrl } from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.data';
import {
  canAcceptHazardRectificationOrder,
  canCreateThreeCheckRecord,
  canDeleteThreeCheckRecord,
  canRectifyHazardRectificationOrder,
  canVoidHazardRectificationOrder,
} from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.view-state';
import {
  getVisiblePinganOrganizationFilterKeys,
  isPinganOrganizationFilterLocked,
  resolveScopedPinganOrganizationDefaults,
} from '#/views/pingan/shared/organization-filter-permission';
import StatusFilterActions from '#/views/pingan/shared/StatusFilterActions.vue';

type OrgNode = SystemManagementApi.OrganizationNode;

interface OrgOption {
  children?: OrgOption[];
  label: string;
  orgType?: string;
  value: SystemManagementApi.Id;
}

interface FlowLogDetail {
  key: string;
  label: string;
  value: string;
}

type HazardOrderStatusFilter = NonNullable<
  PinganHazardRectificationOrderApi.OrderListParams['status']
>;

const statusOptions: Array<{ label: string; value: HazardOrderStatusFilter }> =
  [
    { label: '全部', value: 'all' },
    { label: '待派发', value: 'PENDING_ASSIGN' },
    { label: '待整改', value: 'PENDING_RECTIFY' },
    { label: '已整改', value: 'RECTIFIED' },
    { label: '待验收', value: 'PENDING_ACCEPTANCE' },
    { label: '已关闭', value: 'CLOSED' },
    { label: '已作废', value: 'CANCELLED' },
  ];
const sourceTypeOptions = [
  { label: '全部来源', value: 'all' },
  { label: '一班三查', value: 'THREE_CHECK' },
  { label: '手工新增', value: 'MANUAL' },
  { label: '安全检查', value: 'SAFETY_INSPECTION' },
  { label: '随手拍', value: 'QUICK_SHOT' },
];
const sourceModuleLabels: Record<string, string> = {
  manual: '手工新增',
  'mid-shift-inspection': '班中检查',
  'post-shift-inspection': '班后检查',
  'pre-shift-inspection': '班前检查',
  'pre-shift-meeting': '班前会',
  'quick-shot': '随手拍',
  'safety-check': '安全检查',
};

const statusTone: Record<string, string> = {
  CANCELLED: 'default',
  CLOSED: 'green',
  PENDING_ACCEPTANCE: 'blue',
  PENDING_ASSIGN: 'orange',
  PENDING_RECTIFY: 'red',
  RECTIFIED: 'gold',
};

const actionLabels: Record<PinganHazardRectificationOrderApi.Action, string> = {
  ACCEPT: '审核验收',
  CANCEL: '作废',
  ISSUE_RECTIFICATION: '下发整改',
  MARK_RECTIFIED: '提交整改结果',
  REJECT_ACCEPTANCE: '验收驳回',
  REQUEST_ACCEPTANCE: '提交验收',
};
const flowLogPayloadLabels: Record<string, string> = {
  acceptanceDepartmentId: '验收部门',
  acceptanceRequirement: '验收要求',
  acceptanceRemark: '验收说明',
  acceptanceUserId: '验收人',
  afterPhoto: '整改后照片',
  cancelReason: '作废原因',
  rectificationDeadline: '整改期限',
  rectificationDepartmentId: '整改部门',
  rectificationDescription: '整改说明',
  rectificationRequirement: '整改措施',
  rectificationResponsibleUserId: '整改责任人',
  supplementalAttachments: '补充附件',
  rejectReason: '驳回原因',
  remark: '说明',
};

const accessStore = useAccessStore();
const userStore = useUserStore();
const query = reactive<PinganHazardRectificationOrderApi.OrderListParams>({
  page: 1,
  pageSize: 20,
  sourceType: 'all',
  status: 'all',
});
const orgNodes = ref<OrgNode[]>([]);
const orgTreeOptions = ref<OrgOption[]>([]);
const responsibleUsers = ref<PinganPreShiftMeetingApi.UserOption[]>([]);
const loading = ref(false);
const actionSubmitting = ref(false);
const createOpen = ref(false);
const createSubmitting = ref(false);
const rows = ref<PinganHazardRectificationOrderApi.OrderRow[]>([]);
const selectedOrderRowKeys = ref<PinganHazardRectificationOrderApi.Id[]>([]);
const total = ref(0);
const detailOpen = ref(false);
const activeDetailTab = ref('hazard');
const historyActionFilter = ref('all');
const historyOperatorFilter = ref('all');
const historyKeyword = ref('');
const currentOrder = ref<PinganHazardRectificationOrderApi.OrderDetail>();
const agentReviewLoading = ref(false);
const agentRectificationDescriptionLoading = ref(false);
const agentSummaryLoading = ref(false);
const agentSummaryConfirming = ref(false);
const agentSummaryEditing = ref(false);
const agentSummaryEditJson = ref('');
const agentReview =
  ref<PinganHazardRectificationOrderApi.AgentReviewResponse>();
const agentSummary =
  ref<PinganHazardRectificationOrderApi.AgentSummaryResponse>();
const actionOpen = ref(false);
const actionReturnToDetail = ref(false);
const currentAction = ref<PinganHazardRectificationOrderApi.Action>();
const rectifiedPhotoInputRef = ref<HTMLInputElement>();
const supplementalAttachmentInputRef = ref<HTMLInputElement>();
const supplementalAttachmentFiles = ref<File[]>([]);
const actionForm = reactive({
  acceptanceDepartmentId: undefined as
    | PinganHazardRectificationOrderApi.Id
    | undefined,
  acceptanceRequirement: '',
  acceptanceRemark: '',
  acceptanceUserId: undefined as string | undefined,
  acceptanceUserInput: '',
  afterPhoto: '',
  cancelReason: '',
  rectificationDeadline: '',
  rectificationDepartmentId: undefined as
    | PinganHazardRectificationOrderApi.Id
    | undefined,
  rectificationDescription: '',
  rectificationRequirement: '',
  rectificationResponsibleUserId: undefined as string | undefined,
  rectificationResponsibleUserInput: '',
  rejectReason: '',
});
const createForm = reactive({
  businessDate: '',
  companyId: undefined as PinganHazardRectificationOrderApi.Id | undefined,
  departmentId: undefined as PinganHazardRectificationOrderApi.Id | undefined,
  teamId: undefined as PinganHazardRectificationOrderApi.Id | undefined,
});
const createItems = ref<PinganHazardRectificationOrderApi.CreateItemRequest[]>([
  {
    beforePhoto: '',
    checkItem: '',
    hazardDescription: '',
    riskType: '',
  },
]);
const accessCodes = computed(() => accessStore.accessCodes ?? []);
const currentUserRoles = computed(() => userStore.userInfo?.roles ?? []);
const currentUserOrgDefaults = computed(() =>
  resolveScopedPinganOrganizationDefaults(
    userStore.userInfo?.orgId,
    orgNodes.value,
  ),
);
const visibleOrganizationFilterKeys = computed(() =>
  getVisiblePinganOrganizationFilterKeys(currentUserRoles.value),
);
const canCreateOrder = computed(() =>
  canCreateThreeCheckRecord('PinganHazardRectification', accessCodes.value),
);
const canDeleteOrder = computed(() =>
  canDeleteThreeCheckRecord('PinganHazardRectification', accessCodes.value),
);
const canRectifyOrder = computed(() =>
  canRectifyHazardRectificationOrder(accessCodes.value),
);
const canAcceptOrder = computed(() =>
  canAcceptHazardRectificationOrder(accessCodes.value),
);
const canVoidOrder = computed(() =>
  canVoidHazardRectificationOrder(accessCodes.value),
);

const columns: TableColumnsType<PinganHazardRectificationOrderApi.OrderRow> = [
  { dataIndex: 'orderNo', fixed: 'left', title: '工单号', width: 170 },
  { dataIndex: 'status', title: '状态', width: 110 },
  { dataIndex: 'sourceType', title: '来源', width: 110 },
  { dataIndex: 'hazardCount', title: '隐患数', width: 90 },
  { dataIndex: 'sourceRecordNo', title: '来源单据', width: 190 },
  { dataIndex: 'sourceModuleKey', title: '来源模块', width: 170 },
  { dataIndex: 'team', title: '班组', width: 150 },
  { dataIndex: 'businessDate', title: '业务日期', width: 130 },
  { dataIndex: 'rectificationDeadline', title: '整改期限', width: 180 },
  { dataIndex: 'closedAt', title: '闭环时间', width: 180 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 130 },
];

const itemColumns: TableColumnsType<PinganHazardRectificationOrderApi.OrderItem> =
  [
    { dataIndex: 'sourceLineIndex', title: '序号', width: 70 },
    { dataIndex: 'riskType', title: '风险', width: 130 },
    { dataIndex: 'checkItem', title: '检查项', width: 260 },
    { dataIndex: 'aiEnabled', title: '是否启用AI', width: 120 },
    { dataIndex: 'hazardDescription', title: '隐患描述', width: 260 },
    { dataIndex: 'hazardMedia', title: '隐患图片/视频', width: 180 },
    { dataIndex: 'rectificationStatusLabel', title: '整改状态', width: 120 },
    { dataIndex: 'closedAt', title: '闭环时间', width: 170 },
  ];

const availableActions = computed(() => {
  const status = currentOrder.value?.status;
  let actions: string[] = [];
  switch (status) {
    case 'PENDING_ACCEPTANCE': {
      actions = ['ACCEPT', 'CANCEL'];

      break;
    }
    case 'PENDING_ASSIGN': {
      actions = ['ISSUE_RECTIFICATION', 'CANCEL'];

      break;
    }
    case 'PENDING_RECTIFY': {
      actions = ['MARK_RECTIFIED', 'CANCEL'];

      break;
    }
    case 'RECTIFIED': {
      actions = ['REQUEST_ACCEPTANCE', 'CANCEL'];

      break;
    }
    // No default
  }
  return actions.filter((action) =>
    canPerformOrderAction(action as PinganHazardRectificationOrderApi.Action),
  );
});
const primaryHazardItem = computed(() => currentOrder.value?.items?.[0]);
const rectificationSubmitLog = computed(() =>
  currentOrder.value?.flowLogs.find((log) => log.action === 'MARK_RECTIFIED'),
);
const acceptanceSubmitLog = computed(() =>
  currentOrder.value?.flowLogs.find(
    (log) => log.action === 'REQUEST_ACCEPTANCE',
  ),
);
const rectificationSubmitStatusLabel = computed(() => {
  const status = currentOrder.value?.status;
  if (!currentOrder.value?.rectifiedAt) return '待提交';
  if (status === 'RECTIFIED') return '待验收时提交';
  if (status === 'PENDING_ACCEPTANCE') return '已提交验收';
  if (status === 'CLOSED') return '已提交 / 已完成';
  return '已完成';
});
const historyActionOptions = computed(() => {
  const options = new Map<string, string>();
  for (const log of currentOrder.value?.flowLogs ?? [])
    options.set(log.action, log.actionLabel);
  return [
    { label: '全部类型', value: 'all' },
    ...Array.from(options, ([value, label]) => ({ label, value })),
  ];
});
const historyOperatorOptions = computed(() => [
  { label: '全部操作人', value: 'all' },
  ...Array.from(
    new Set(
      (currentOrder.value?.flowLogs ?? [])
        .map((log) => log.operatorName || '系统')
        .filter(Boolean),
    ),
    (value) => ({ label: value, value }),
  ),
]);
const filteredFlowLogs = computed(() => {
  const keyword = historyKeyword.value.trim().toLocaleLowerCase();
  return (currentOrder.value?.flowLogs ?? []).filter((log) => {
    if (
      historyActionFilter.value !== 'all' &&
      log.action !== historyActionFilter.value
    )
      return false;
    if (
      historyOperatorFilter.value !== 'all' &&
      (log.operatorName || '系统') !== historyOperatorFilter.value
    )
      return false;
    if (!keyword) return true;
    return [
      log.actionLabel,
      log.remark,
      log.operatorName,
      log.toStatusLabel,
      historyLogDescription(log),
    ].some((value) => value?.toLocaleLowerCase().includes(keyword));
  });
});
const detailTabs = [
  { key: 'hazard', label: '隐患与证据' },
  { key: 'result', label: '整改结果' },
  { key: 'acceptance', label: '验收结论' },
  { key: 'history', label: '处理记录' },
  { key: 'basic', label: '基础信息' },
];
const detailStatusMeta = computed(() => {
  const status = currentOrder.value?.status;
  const map: Record<string, { icon: string; subtitle: string; tone: string }> =
    {
      CANCELLED: {
        icon: 'lucide:circle-slash-2',
        subtitle: '流程已终止',
        tone: 'neutral',
      },
      CLOSED: {
        icon: 'lucide:badge-check',
        subtitle: '按期完成',
        tone: 'success',
      },
      PENDING_ACCEPTANCE: {
        icon: 'lucide:shield-check',
        subtitle: '等待人工验收',
        tone: 'blue',
      },
      PENDING_ASSIGN: {
        icon: 'lucide:clipboard-list',
        subtitle: '待明确整改任务',
        tone: 'warning',
      },
      PENDING_RECTIFY: {
        icon: 'lucide:hard-hat',
        subtitle: '整改任务执行中',
        tone: 'danger',
      },
      RECTIFIED: {
        icon: 'lucide:image-check',
        subtitle: '整改结果待提交验收',
        tone: 'blue',
      },
    };
  return (
    map[status ?? ''] ?? {
      icon: 'lucide:clipboard-list',
      subtitle: '待明确整改任务',
      tone: 'warning',
    }
  );
});
const actionCardMeta = computed(() => {
  const action = currentAction.value;
  if (action === 'ISSUE_RECTIFICATION') {
    return {
      icon: 'lucide:clipboard-check',
      title: '下发整改',
      badge: '待下发',
      description: '明确责任人、整改期限与验收标准',
      submit: '下发整改',
    };
  }
  if (action === 'MARK_RECTIFIED') {
    return {
      icon: 'lucide:cloud-upload',
      title: '提交整改结果',
      badge: '整改中',
      description: '提交整改事实与现场证据',
      submit: '提交整改结果',
    };
  }
  if (action === 'ACCEPT' || action === 'REJECT_ACCEPTANCE') {
    return {
      icon: 'lucide:shield-check',
      title: '审核验收',
      badge: '待验收',
      description: '核对整改证据并作出人工验收结论',
      submit: action === 'ACCEPT' ? '验收通过' : '退回整改',
    };
  }
  return {
    icon: 'lucide:send',
    title: action ? actionLabels[action] : '工单操作',
    badge: '',
    description: '完成当前流程操作',
    submit: action ? actionLabels[action] : '确定',
  };
});
const hideSourceSequenceAndRiskColumns = computed(() =>
  ['QUICK_SHOT', 'SAFETY_INSPECTION'].includes(
    currentOrder.value?.sourceType ?? '',
  ),
);
const detailItemColumns = computed(() =>
  hideSourceSequenceAndRiskColumns.value
    ? itemColumns.filter((column) => !isHiddenSourceColumn(column))
    : itemColumns,
);

function canPerformOrderAction(
  action: PinganHazardRectificationOrderApi.Action,
) {
  if (
    action === 'ISSUE_RECTIFICATION' ||
    action === 'MARK_RECTIFIED' ||
    action === 'REQUEST_ACCEPTANCE'
  ) {
    return canRectifyOrder.value;
  }
  if (action === 'ACCEPT' || action === 'REJECT_ACCEPTANCE') {
    return canAcceptOrder.value;
  }
  if (action === 'CANCEL') {
    return canVoidOrder.value;
  }
  return false;
}

const flatOrganizations = computed(() => flattenOrganizations(orgNodes.value));
const acceptanceUserOptions = computed(() => {
  const options = responsibleUsers.value.map((user) => ({
    label:
      user.realName === user.username
        ? user.realName
        : `${user.realName}（${user.username}）`,
    value: String(user.id),
  }));
  const selectedId = String(currentOrder.value?.acceptanceUserId ?? '');
  const selectedName = currentOrder.value?.acceptanceUserName;
  if (
    selectedId &&
    selectedName &&
    !options.some((option) => option.value === selectedId)
  ) {
    options.push({ label: selectedName, value: selectedId });
  }
  return options;
});
const editableUserOptions = computed(() =>
  responsibleUsers.value.map((user) => ({
    label:
      user.realName === user.username
        ? user.realName
        : `${user.realName}（${user.username}）`,
    value:
      user.realName === user.username
        ? user.realName
        : `${user.realName}（${user.username}）`,
  })),
);
const companyOptions = computed(() => orgOptionsByType('COMPANY'));
const departmentOptions = computed(() =>
  childOptionsUnderSelectedCompany('DEPARTMENT'),
);
const teamOptions = computed(() => childOptionsUnderSelectedCompany('TEAM'));
const createDepartmentOptions = computed(() =>
  childOptionsUnder(createForm.companyId, undefined, 'DEPARTMENT'),
);
const createTeamOptions = computed(() =>
  childOptionsUnder(createForm.companyId, createForm.departmentId, 'TEAM'),
);
const actionDepartmentOptions = computed(() =>
  childOptionsUnder(currentOrder.value?.companyId, undefined, 'DEPARTMENT'),
);
const rowSelection = computed(() => ({
  selectedRowKeys: selectedOrderRowKeys.value,
  onChange: (keys: Array<PinganHazardRectificationOrderApi.Id>) => {
    selectedOrderRowKeys.value = keys;
  },
}));

async function loadOrders() {
  loading.value = true;
  try {
    const data = await getHazardRectificationOrdersApi(query);
    rows.value = data.items;
    total.value = Number(data.total || 0);
  } finally {
    loading.value = false;
  }
}

function setStatusFilter(status: HazardOrderStatusFilter) {
  query.status = status;
  query.page = 1;
  void loadOrders();
}

function resetFilters() {
  query.status = 'all';
  query.sourceType = 'all';
  query.companyId = currentUserOrgDefaults.value.companyId;
  query.departmentId = currentUserOrgDefaults.value.departmentId;
  query.teamId = currentUserOrgDefaults.value.teamId;
  query.page = 1;
  void loadOrders();
}

function handleBatchDelete() {
  if (!canDeleteOrder.value) {
    return;
  }
  if (selectedOrderRowKeys.value.length === 0) {
    message.warning('请选择要删除的隐患整改工单');
    return;
  }
  Modal.confirm({
    content: `已选择 ${selectedOrderRowKeys.value.length} 条工单，删除后列表将不再显示。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await batchDeleteHazardRectificationOrdersApi(selectedOrderRowKeys.value);
      selectedOrderRowKeys.value = [];
      message.success('批量删除成功');
      await loadOrders();
    },
    title: '确认删除选中的隐患整改工单吗？',
  });
}

async function loadOrgTreeOptions() {
  const nodes = await getPinganOrgTreeApi();
  orgNodes.value = nodes;
  orgTreeOptions.value = normalizeOrgTree(nodes);
  enforceHazardUserOrganizationScope();
}

async function loadResponsibleUsers() {
  responsibleUsers.value = await getPinganUsersApi();
}

async function openDetail(id: string) {
  currentOrder.value = await getHazardRectificationOrderDetailApi(id);
  activeDetailTab.value = 'hazard';
  historyActionFilter.value = 'all';
  historyOperatorFilter.value = 'all';
  historyKeyword.value = '';
  agentReview.value = undefined;
  agentSummary.value = undefined;
  agentSummaryEditJson.value = '';
  detailOpen.value = true;
}

function historyLogDescription(log: PinganHazardRectificationOrderApi.FlowLog) {
  if (log.remark?.trim()) return log.remark.trim();
  const descriptions: Record<string, string> = {
    ACCEPT: '现场确认整改到位，验收通过。',
    ISSUE_RECTIFICATION: '下发整改任务，明确整改要求和期限。',
    MARK_RECTIFIED: '上传整改结果并完成现场整改。',
    REQUEST_ACCEPTANCE: '提交整改结果，申请验收。',
  };
  return (
    descriptions[log.action] ||
    `${log.fromStatusLabel || '开始'} → ${log.toStatusLabel}`
  );
}

function exportHistoryRecords() {
  if (!currentOrder.value) return;
  const csvCell = (value: unknown) =>
    `"${String(value ?? '').replaceAll('"', '""')}"`;
  const rows = filteredFlowLogs.value.map((log) =>
    [
      log.createdAt,
      log.actionLabel,
      historyLogDescription(log),
      log.operatorName || '系统',
      log.toStatusLabel,
    ]
      .map((value) => csvCell(value))
      .join(','),
  );
  const csv = [
    ['时间', '处理内容', '说明', '操作人', '状态']
      .map((value) => csvCell(value))
      .join(','),
    ...rows,
  ].join('\n');
  const url = URL.createObjectURL(
    new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' }),
  );
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `${currentOrder.value.orderNo}_处理记录.csv`;
  document.body.append(anchor);
  anchor.click();
  anchor.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 0);
  message.success('处理记录已导出');
}

async function copyCurrentOrderNo() {
  const orderNo = currentOrder.value?.orderNo;
  if (!orderNo) return;
  try {
    await navigator.clipboard.writeText(orderNo);
  } catch {
    const textarea = document.createElement('textarea');
    textarea.value = orderNo;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.append(textarea);
    textarea.select();
    document.execCommand('copy');
    textarea.remove();
  }
  message.success('工单编号已复制');
}

function completedOnTime() {
  const deadline = currentOrder.value?.rectificationDeadline;
  const completedAt = currentOrder.value?.rectifiedAt;
  if (!deadline || !completedAt) return false;
  return completedAt <= deadline;
}

function internalOrderNumber(orderNo?: string) {
  if (!orderNo) return '-';
  return orderNo.replace(/^HR-/, 'IS-');
}

function resetActionForm() {
  actionForm.acceptanceDepartmentId = undefined;
  actionForm.acceptanceRequirement = '';
  actionForm.acceptanceRemark = '';
  actionForm.acceptanceUserId = undefined;
  actionForm.acceptanceUserInput = '';
  actionForm.afterPhoto = '';
  actionForm.cancelReason = '';
  actionForm.rectificationDeadline = '';
  actionForm.rectificationDepartmentId = undefined;
  actionForm.rectificationDescription = '';
  actionForm.rectificationRequirement = '';
  actionForm.rectificationResponsibleUserId = undefined;
  actionForm.rectificationResponsibleUserInput = '';
  actionForm.rejectReason = '';
  supplementalAttachmentFiles.value = [];
}

async function openAction(action: PinganHazardRectificationOrderApi.Action) {
  if (!canPerformOrderAction(action)) {
    return;
  }
  resetActionForm();
  if (action === 'ISSUE_RECTIFICATION') {
    actionForm.rectificationDepartmentId =
      currentOrder.value?.rectificationDepartmentId ??
      currentOrder.value?.departmentId;
    actionForm.rectificationRequirement = sourceRectificationMeasures();
    actionForm.acceptanceRequirement = numberedListText(
      sourceAcceptanceCriteria(),
    );
    actionForm.rectificationResponsibleUserInput =
      currentOrder.value?.rectificationResponsibleUserName ?? '';
    actionForm.acceptanceUserInput =
      currentOrder.value?.acceptanceUserName ?? '';
  }
  if (
    action === 'ACCEPT' ||
    action === 'REJECT_ACCEPTANCE' ||
    action === 'REQUEST_ACCEPTANCE'
  ) {
    actionForm.acceptanceDepartmentId =
      currentOrder.value?.acceptanceDepartmentId ??
      currentOrder.value?.departmentId;
    actionForm.acceptanceUserId = currentOrder.value?.acceptanceUserId
      ? String(currentOrder.value.acceptanceUserId)
      : undefined;
    if (action === 'ACCEPT' || action === 'REJECT_ACCEPTANCE') {
      actionForm.acceptanceUserInput =
        currentOrder.value?.acceptanceUserName ?? '';
    }
    actionForm.acceptanceRequirement =
      currentOrder.value?.acceptanceRequirement?.trim()
        ? numberedListText(
            splitListText(currentOrder.value.acceptanceRequirement),
          )
        : numberedListText(sourceAcceptanceCriteria());
    if (
      !actionForm.acceptanceRequirement &&
      currentOrder.value?.sourceType === 'QUICK_SHOT' &&
      currentOrder.value.sourceRecordId
    ) {
      try {
        const run = await getLatestQuickShotAgentRunApi(
          String(currentOrder.value.sourceRecordId),
        );
        const acceptedIds = new Set(
          (run?.decisions ?? [])
            .filter((decision) => decision.decision !== 'REJECTED')
            .map((decision) => decision.candidateId),
        );
        const criteria = (run?.assessment?.assessments ?? []).flatMap(
          (item) => {
            if (acceptedIds.size > 0 && !acceptedIds.has(item.candidateId))
              return [];
            const value = item.assessment.acceptanceCriteria;
            return Array.isArray(value)
              ? value
              : (typeof value === 'string'
                ? value.split(/[；;\n]/)
                : []);
          },
        );
        actionForm.acceptanceRequirement = numberedListText(criteria);
      } catch {
        // Agent 历史不可用时保留人工填写入口，不阻塞原验收流程。
      }
    }
  }
  currentAction.value = action;
  actionReturnToDetail.value = detailOpen.value;
  detailOpen.value = false;
  actionOpen.value = true;
}

function closeActionCard() {
  actionOpen.value = false;
  if (actionReturnToDetail.value) detailOpen.value = true;
  actionReturnToDetail.value = false;
}

function triggerRectifiedPhotoUpload() {
  rectifiedPhotoInputRef.value?.click();
}

async function handleRectifiedPhotoChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  if (!file.type.startsWith('image/')) {
    message.error('请选择图片文件');
    input.value = '';
    return;
  }

  if (!currentOrder.value) {
    message.error('请先打开工单详情');
    input.value = '';
    return;
  }
  if (!canRectifyOrder.value) {
    input.value = '';
    return;
  }

  try {
    const attachment = await uploadHazardRectificationOrderAttachmentApi(
      currentOrder.value.id,
      'RECTIFICATION_AFTER_PHOTO',
      file,
    );
    actionForm.afterPhoto = attachment.url;
  } finally {
    input.value = '';
  }
}

function triggerSupplementalAttachmentUpload() {
  supplementalAttachmentInputRef.value?.click();
}

function handleSupplementalAttachmentChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const files = [...(input.files ?? [])];
  input.value = '';
  if (files.length === 0) return;
  const acceptedExtensions = new Set([
    'doc',
    'docx',
    'gif',
    'jpeg',
    'jpg',
    'pdf',
    'png',
    'webp',
  ]);
  const existing = supplementalAttachmentFiles.value;
  const accepted: File[] = [];
  for (const file of files) {
    const extension = file.name.split('.').pop()?.toLocaleLowerCase() ?? '';
    if (!acceptedExtensions.has(extension)) {
      message.error(`${file.name}：仅支持图片、PDF、Word 文件`);
      continue;
    }
    if (file.size > 10 * 1024 * 1024) {
      message.error(`${file.name}：单个文件不能超过 10MB`);
      continue;
    }
    const duplicated = [...existing, ...accepted].some(
      (item) =>
        item.name === file.name &&
        item.size === file.size &&
        item.lastModified === file.lastModified,
    );
    if (!duplicated) accepted.push(file);
  }
  const remaining = Math.max(0, 5 - existing.length);
  if (accepted.length > remaining) {
    message.warning('补充附件最多上传 5 个');
  }
  supplementalAttachmentFiles.value = [
    ...existing,
    ...accepted.slice(0, remaining),
  ];
}

function removeSupplementalAttachment(index: number) {
  supplementalAttachmentFiles.value.splice(index, 1);
}

async function uploadSupplementalAttachments() {
  if (!currentOrder.value || supplementalAttachmentFiles.value.length === 0) {
    return [];
  }
  const attachments: PinganHazardRectificationOrderApi.Attachment[] = [];
  for (const file of supplementalAttachmentFiles.value) {
    attachments.push(
      await uploadHazardRectificationOrderAttachmentApi(
        currentOrder.value.id,
        'RECTIFICATION_SUPPLEMENT',
        file,
      ),
    );
  }
  return attachments;
}

function normalizeOrgTree(nodes: OrgNode[]): OrgOption[] {
  return nodes.map((node) => ({
    children: normalizeOrgTree(node.children ?? []),
    label: node.title,
    orgType: node.orgType,
    value: node.id,
  }));
}

function flattenOrganizations(nodes: OrgNode[]): OrgNode[] {
  return nodes.flatMap((node) => [
    node,
    ...flattenOrganizations(node.children ?? []),
  ]);
}

function orgOptionsByType(orgType: string) {
  return flatOrganizations.value
    .filter((node) => node.orgType === orgType)
    .map((node) => ({ label: node.title, value: node.id }));
}

function childOptionsUnderSelectedCompany(orgType: 'DEPARTMENT' | 'TEAM') {
  return childOptionsUnder(query.companyId, query.departmentId, orgType);
}

function childOptionsUnder(
  companyId: PinganHazardRectificationOrderApi.Id | undefined,
  departmentId: PinganHazardRectificationOrderApi.Id | undefined,
  orgType: 'DEPARTMENT' | 'TEAM',
) {
  if (orgType === 'TEAM' && departmentId !== undefined) {
    return optionsUnder(departmentId, orgType);
  }
  if (companyId !== undefined) {
    return optionsUnder(companyId, orgType);
  }
  return orgOptionsByType(orgType);
}

function optionsUnder(
  parentId: PinganHazardRectificationOrderApi.Id,
  orgType: 'DEPARTMENT' | 'TEAM',
) {
  const path = findOrganizationPath(orgNodes.value, parentId);
  const parent = path.at(-1);
  return flattenOrganizations(parent?.children ?? [])
    .filter((node) => node.orgType === orgType)
    .map((node) => ({ label: node.title, value: node.id }));
}

function findOrganizationPath(
  nodes: OrgNode[],
  id: PinganHazardRectificationOrderApi.Id,
): OrgNode[] {
  for (const node of nodes) {
    if (String(node.id) === String(id)) return [node];
    const childPath = findOrganizationPath(node.children ?? [], id);
    if (childPath.length > 0) return [node, ...childPath];
  }
  return [];
}

function isHazardOrganizationFieldLocked(field: PinganOrganizationFilterKey) {
  return isPinganOrganizationFilterLocked(currentUserRoles.value, field);
}

function isHazardOrganizationFilterVisible(field: PinganOrganizationFilterKey) {
  return visibleOrganizationFilterKeys.value.includes(field);
}

function applyScopedOrganizationDefaults(target: {
  companyId?: PinganHazardRectificationOrderApi.Id;
  departmentId?: PinganHazardRectificationOrderApi.Id;
  teamId?: PinganHazardRectificationOrderApi.Id;
}) {
  const defaults = currentUserOrgDefaults.value;
  if (isHazardOrganizationFieldLocked('company') && defaults.companyId) {
    target.companyId = defaults.companyId;
  }
  if (isHazardOrganizationFieldLocked('department') && defaults.departmentId) {
    target.departmentId = defaults.departmentId;
  }
  if (isHazardOrganizationFieldLocked('team') && defaults.teamId) {
    target.teamId = defaults.teamId;
  }
}

function enforceHazardUserOrganizationScope() {
  applyScopedOrganizationDefaults(query);
  applyScopedOrganizationDefaults(createForm);
}

function setCompany(value?: PinganHazardRectificationOrderApi.Id) {
  if (isHazardOrganizationFieldLocked('company')) {
    enforceHazardUserOrganizationScope();
    return;
  }
  query.companyId = value;
  query.departmentId = undefined;
  query.teamId = undefined;
}

function setDepartment(value?: PinganHazardRectificationOrderApi.Id) {
  if (isHazardOrganizationFieldLocked('department')) {
    enforceHazardUserOrganizationScope();
    return;
  }
  query.departmentId = value;
  query.teamId = undefined;
}

function setTeam(value?: PinganHazardRectificationOrderApi.Id) {
  if (isHazardOrganizationFieldLocked('team')) {
    enforceHazardUserOrganizationScope();
    return;
  }
  query.teamId = value;
}

function sourceTypeLabel(sourceType?: string) {
  return (
    sourceTypeOptions.find((option) => option.value === sourceType)?.label ??
    sourceType ??
    '-'
  );
}

function sourceModuleLabel(sourceModuleKey?: string) {
  if (!sourceModuleKey) return '-';
  return sourceModuleLabels[sourceModuleKey] ?? sourceModuleKey;
}

function isHiddenSourceColumn(
  column: TableColumnsType<PinganHazardRectificationOrderApi.OrderItem>[number],
) {
  if (!('dataIndex' in column)) return false;
  return (
    column.dataIndex === 'sourceLineIndex' || column.dataIndex === 'riskType'
  );
}

function itemHazardMediaUrl(
  record: Partial<PinganHazardRectificationOrderApi.OrderItem>,
) {
  return resolveAttachmentPreviewUrl(
    record.beforeVideo || record.beforePhoto || '',
  );
}

function itemHazardMediaIsVideo(
  record: Partial<PinganHazardRectificationOrderApi.OrderItem>,
) {
  const media = itemHazardMediaUrl(record).toLowerCase();
  return (
    !!record.beforeVideo ||
    media.endsWith('.mp4') ||
    media.endsWith('.mov') ||
    media.endsWith('.avi') ||
    media.endsWith('.webm') ||
    media.endsWith('.mkv')
  );
}

function setCreateCompany(value?: PinganHazardRectificationOrderApi.Id) {
  if (isHazardOrganizationFieldLocked('company')) {
    enforceHazardUserOrganizationScope();
    return;
  }
  createForm.companyId = value;
  createForm.departmentId = undefined;
  createForm.teamId = undefined;
}

function setCreateDepartment(value?: PinganHazardRectificationOrderApi.Id) {
  if (isHazardOrganizationFieldLocked('department')) {
    enforceHazardUserOrganizationScope();
    return;
  }
  createForm.departmentId = value;
  createForm.teamId = undefined;
}

function resetCreateForm() {
  createForm.businessDate = '';
  createForm.companyId = undefined;
  createForm.departmentId = undefined;
  createForm.teamId = undefined;
  createItems.value = [
    {
      beforePhoto: '',
      checkItem: '',
      hazardDescription: '',
      riskType: '',
    },
  ];
}

function openCreateDrawer() {
  if (!canCreateOrder.value) {
    return;
  }
  resetCreateForm();
  applyScopedOrganizationDefaults(createForm);
  createOpen.value = true;
}

function addCreateItem() {
  createItems.value.push({
    beforePhoto: '',
    checkItem: '',
    hazardDescription: '',
    riskType: '',
  });
}

function removeCreateItem(index: number) {
  if (createItems.value.length === 1) {
    message.error('至少保留一条隐患明细');
    return;
  }
  createItems.value.splice(index, 1);
}

function validateCreateForm() {
  if (!createForm.companyId) {
    message.error('请选择公司');
    return false;
  }
  if (!createForm.departmentId) {
    message.error('请选择部门');
    return false;
  }
  if (!createForm.teamId) {
    message.error('请选择班组');
    return false;
  }
  if (!createForm.businessDate) {
    message.error('请选择业务日期');
    return false;
  }
  const invalidItem = createItems.value.some(
    (item) => !item.checkItem?.trim() || !item.hazardDescription?.trim(),
  );
  if (invalidItem) {
    message.error('请填写检查项和隐患描述');
    return false;
  }
  return true;
}

async function submitManualOrder() {
  if (!canCreateOrder.value) return;
  enforceHazardUserOrganizationScope();
  if (!validateCreateForm()) return;
  createSubmitting.value = true;
  try {
    await createHazardRectificationOrderApi({
      businessDate: createForm.businessDate,
      companyId: createForm.companyId as PinganHazardRectificationOrderApi.Id,
      departmentId:
        createForm.departmentId as PinganHazardRectificationOrderApi.Id,
      items: createItems.value.map((item) => ({
        beforePhoto: item.beforePhoto?.trim() || undefined,
        checkItem: item.checkItem.trim(),
        hazardDescription: item.hazardDescription.trim(),
        riskType: item.riskType?.trim() || undefined,
      })),
      teamId: createForm.teamId as PinganHazardRectificationOrderApi.Id,
    });
    createOpen.value = false;
    query.sourceType = 'all';
    message.success('新增工单成功');
    await loadOrders();
  } finally {
    createSubmitting.value = false;
  }
}

function selectedAcceptanceUserId() {
  const editableAcceptanceUser = editableUser(
    actionForm.acceptanceUserInput,
  );
  if (editableAcceptanceUser) return editableAcceptanceUser.id;
  const value = Number(actionForm.acceptanceUserId);
  return Number.isSafeInteger(value) && value > 0 ? value : undefined;
}

function selectedRectificationResponsibleUserId() {
  if (currentAction.value === 'ISSUE_RECTIFICATION') {
    return editableUser(actionForm.rectificationResponsibleUserInput)?.id;
  }
  const value = Number(actionForm.rectificationResponsibleUserId);
  return Number.isSafeInteger(value) && value > 0 ? value : undefined;
}

function editableUser(value?: string) {
  const identity = value?.trim();
  if (!identity) return undefined;
  const normalizedIdentity = identity.toLocaleLowerCase();
  const exactIdentityMatch = responsibleUsers.value.find((user) => {
    const displayName =
      user.realName === user.username
        ? user.realName
        : `${user.realName}（${user.username}）`;
    return (
      String(user.id) === identity ||
      user.username.toLocaleLowerCase() === normalizedIdentity ||
      displayName === identity
    );
  });
  if (exactIdentityMatch) return exactIdentityMatch;
  const realNameMatches = responsibleUsers.value.filter(
    (user) => user.realName === identity,
  );
  return realNameMatches.length === 1 ? realNameMatches[0] : undefined;
}

function editableUserIdentity(value?: string) {
  const identity = value?.trim();
  if (!identity) return undefined;
  return editableUser(identity)?.realName ?? identity;
}

function actionRequiresAcceptanceUser() {
  return (
    currentAction.value === 'ACCEPT' ||
    currentAction.value === 'REJECT_ACCEPTANCE'
  );
}

function hasAcceptanceUserSelection() {
  return Boolean(
    actionForm.acceptanceUserInput.trim() || selectedAcceptanceUserId(),
  );
}

function actionPayload(): Record<string, unknown> {
  switch (currentAction.value) {
    case 'ACCEPT': {
      return {
        acceptanceRequirement: actionForm.acceptanceRequirement,
        acceptanceRemark: actionForm.acceptanceRemark,
        acceptanceUserId: selectedAcceptanceUserId(),
        acceptanceUserName: editableUserIdentity(
          actionForm.acceptanceUserInput,
        ),
      };
    }
    case 'CANCEL': {
      return {
        cancelReason: actionForm.cancelReason,
      };
    }
    case 'ISSUE_RECTIFICATION': {
      return {
        acceptanceRequirement: actionForm.acceptanceRequirement,
        acceptanceUserId: selectedAcceptanceUserId(),
        acceptanceUserName: editableUserIdentity(
          actionForm.acceptanceUserInput,
        ),
        rectificationDepartmentId: actionForm.rectificationDepartmentId,
        rectificationDeadline: selectedRectificationDeadline(),
        rectificationRequirement: actionForm.rectificationRequirement,
        rectificationResponsibleUserId:
          selectedRectificationResponsibleUserId(),
        rectificationResponsibleUserName: editableUserIdentity(
          actionForm.rectificationResponsibleUserInput,
        ),
      };
    }
    case 'MARK_RECTIFIED': {
      return {
        afterPhoto: actionForm.afterPhoto,
        rectificationDescription: actionForm.rectificationDescription,
      };
    }
    case 'REJECT_ACCEPTANCE': {
      return {
        acceptanceUserId: selectedAcceptanceUserId(),
        acceptanceUserName: editableUserIdentity(
          actionForm.acceptanceUserInput,
        ),
        rejectReason: actionForm.rejectReason,
      };
    }
    case 'REQUEST_ACCEPTANCE': {
      return {
        acceptanceDepartmentId: actionForm.acceptanceDepartmentId,
        acceptanceRequirement: actionForm.acceptanceRequirement,
        acceptanceRemark: actionForm.acceptanceRemark,
        acceptanceUserId: selectedAcceptanceUserId(),
      };
    }
    default: {
      return {};
    }
  }
}

function selectedRectificationDeadline() {
  return actionForm.rectificationDeadline.trim().replace('T', ' ');
}

function unknownToText(value: unknown) {
  if (value === undefined || value === null) return '';
  if (typeof value === 'string') return value.trim();
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  return '';
}

function cleanNumberedItem(value: string) {
  return value
    .trim()
    .replace(/^\s*(?:\d+\s*[.、．)]|[-•])\s*/u, '')
    .trim();
}

function compactStrings(values: Array<string | undefined>): string[] {
  return values.filter(
    (value): value is string =>
      typeof value === 'string' && value.length > 0,
  );
}

function splitListText(value?: string) {
  return value
    ? value
        .split(/[；;\n]/)
        .map((item) => cleanNumberedItem(item))
        .filter(Boolean)
    : [];
}

function numberedListText(values: string[]) {
  return [
    ...new Set(values.map((item) => cleanNumberedItem(item)).filter(Boolean)),
  ]
    .map((item, index) => `${index + 1}. ${item}`)
    .join('\n');
}

function organizationNameById(value: unknown) {
  const id = unknownToText(value);
  if (!id) {
    return '';
  }
  return (
    flatOrganizations.value.find(
      (organization) => String(organization.id) === id,
    )?.title ?? ''
  );
}

function userNameById(value: unknown) {
  const id = unknownToText(value);
  if (!id) return '';
  if (String(currentOrder.value?.rectificationResponsibleUserId ?? '') === id) {
    return currentOrder.value?.rectificationResponsibleUserName ?? '';
  }
  if (String(currentOrder.value?.acceptanceUserId ?? '') === id) {
    return currentOrder.value?.acceptanceUserName ?? '';
  }
  return (
    responsibleUsers.value.find((user) => String(user.id) === id)?.realName ??
    ''
  );
}

function flowLogPayloadValue(
  key: string,
  value: unknown,
  payload: Record<string, unknown>,
) {
  if (key === 'supplementalAttachments' && Array.isArray(value)) {
    return value
      .map((item) => {
        if (!item || typeof item !== 'object') return '';
        const attachment = item as Record<string, unknown>;
        return unknownToText(attachment.originalName ?? attachment.name);
      })
      .filter(Boolean)
      .join('、');
  }
  if (['acceptanceDepartmentId', 'rectificationDepartmentId'].includes(key)) {
    return organizationNameById(value) || unknownToText(value);
  }
  if (['acceptanceUserId', 'rectificationResponsibleUserId'].includes(key)) {
    const nameKey =
      key === 'rectificationResponsibleUserId'
        ? 'rectificationResponsibleUserName'
        : 'acceptanceUserName';
    return (
      unknownToText(payload[nameKey]) ||
      userNameById(value) ||
      unknownToText(value)
    );
  }
  if (key === 'afterPhoto') {
    return resolveAttachmentPreviewUrl(
      currentOrder.value?.rectificationAfterPhoto || unknownToText(value),
    );
  }
  return unknownToText(value);
}

function flowLogDetailItems(log: PinganHazardRectificationOrderApi.FlowLog) {
  const payload = log.payload ?? {};
  return Object.entries(flowLogPayloadLabels)
    .filter(([key]) => key !== 'afterPhoto' || log.action === 'MARK_RECTIFIED')
    .map(
      ([key, label]): FlowLogDetail => ({
        key,
        label,
        value: flowLogPayloadValue(key, payload[key], payload),
      }),
    )
    .filter((item) => item.value);
}

function isFlowLogPhotoDetail(item: FlowLogDetail) {
  return item.key === 'afterPhoto';
}

async function submitAction() {
  if (!currentOrder.value || !currentAction.value) return;
  if (!canPerformOrderAction(currentAction.value)) return;
  if (
    currentAction.value === 'ISSUE_RECTIFICATION' &&
    !actionForm.rectificationDepartmentId
  ) {
    message.error('请选择整改部门');
    return;
  }
  if (
    currentAction.value === 'ISSUE_RECTIFICATION' &&
    !actionForm.rectificationResponsibleUserInput.trim()
  ) {
    message.error('请选择或填写整改责任人');
    return;
  }
  if (
    currentAction.value === 'ISSUE_RECTIFICATION' &&
    !actionForm.acceptanceRequirement.trim()
  ) {
    message.error('请填写验收标准');
    return;
  }
  if (
    currentAction.value === 'ISSUE_RECTIFICATION' &&
    !selectedRectificationDeadline()
  ) {
    message.error('请选择完整的整改日期和时间');
    return;
  }
  if (
    currentAction.value === 'REQUEST_ACCEPTANCE' &&
    !actionForm.acceptanceDepartmentId &&
    !selectedAcceptanceUserId()
  ) {
    message.error('请选择验收部门或填写验收人');
    return;
  }
  if (actionRequiresAcceptanceUser() && !hasAcceptanceUserSelection()) {
    message.error('请选择或填写验收人');
    return;
  }
  if (currentAction.value === 'CANCEL' && !actionForm.cancelReason.trim()) {
    message.error('请填写作废原因');
    return;
  }
  actionSubmitting.value = true;
  try {
    const payload = actionPayload();
    if (currentAction.value === 'ISSUE_RECTIFICATION') {
      const attachments = await uploadSupplementalAttachments();
      if (attachments.length > 0) {
        payload.supplementalAttachments = attachments.map((attachment) => ({
          contentType: attachment.contentType,
          fileSize: attachment.fileSize,
          id: attachment.id,
          originalName: attachment.originalName,
          url: attachment.url,
        }));
      }
    }
    currentOrder.value = await actionHazardRectificationOrderApi(
      currentOrder.value.id,
      {
        action: currentAction.value,
        payload,
        version: currentOrder.value.version,
      },
    );
    closeActionCard();
    message.success(
      currentAction.value === 'REJECT_ACCEPTANCE'
        ? '验收已驳回，工单已退回待整改'
        : `${actionLabels[currentAction.value]}成功`,
    );
    await loadOrders();
  } finally {
    actionSubmitting.value = false;
  }
}

function agentAcceptanceCriteria(): string[] {
  const requirement =
    currentOrder.value?.acceptanceRequirement?.trim() ||
    currentOrder.value?.rectificationRequirement?.trim();
  if (requirement) return splitListText(requirement);
  const sourceCriteria = sourceAcceptanceCriteria();
  return sourceCriteria.length > 0
    ? sourceCriteria
    : compactStrings(
        currentOrder.value?.items.map((item) =>
          item.defaultFollowUpPlan?.trim(),
        ) ?? [],
      );
}

function acceptanceConclusionCriteria() {
  const criteria = agentAcceptanceCriteria();
  const rows = criteria.map((label, index) => ({
    detail:
      index === 0
        ? currentOrder.value?.acceptanceRemark || '现场核对结果符合验收要求。'
        : '已按工单要求完成逐项核对。',
    label,
  }));
  const defaults = [
    {
      detail: '现场抽查未发现同类安全隐患。',
      label: '现场抽查结果符合要求',
    },
    {
      detail: '整改前后照片、提交记录等证据完整，符合要求。',
      label: '整改证据完整可追溯',
    },
  ];
  for (const row of defaults) {
    if (rows.length >= 3) break;
    if (!rows.some((item) => item.label === row.label)) rows.push(row);
  }
  return rows.slice(0, 3);
}

function sourceRectificationMeasures() {
  const measures =
    currentOrder.value?.items.flatMap((item) => {
      const snapshot = item.sourceSnapshot?.rectificationMeasures;
      const reviewed = Array.isArray(snapshot)
        ? snapshot.filter((value): value is string => typeof value === 'string')
        : (typeof snapshot === 'string'
          ? snapshot.split(/[；;\n]/)
          : []);
      const reviewedText = reviewed
        .map((value) => value.trim())
        .filter(Boolean);
      if (reviewedText.length > 0) return reviewedText;
      return splitListText(item.defaultFollowUpPlan);
    }) ?? [];
  return numberedListText(measures);
}

function sourceAcceptanceCriteria(): string[] {
  const criteria =
    currentOrder.value?.items.flatMap((item) => {
      const value = item.sourceSnapshot?.acceptanceCriteria;
      if (Array.isArray(value)) {
        return value
          .filter((entry): entry is string => typeof entry === 'string')
          .map((entry) => entry.trim())
          .filter(Boolean);
      }
      if (typeof value === 'string') {
        return value
          .split(/[；;\n]/)
          .map((entry) => entry.trim())
          .filter(Boolean);
      }
      return [];
    }) ?? [];
  return [
    ...new Set(criteria.map((item) => cleanNumberedItem(item)).filter(Boolean)),
  ];
}

function reviewDescription(
  review: PinganHazardRectificationOrderApi.AgentReviewResponse,
) {
  const failed = review.criterionResults.filter(
    (item) => item.result === 'FAIL',
  );
  const uncertain = review.criterionResults.filter((item) =>
    ['NOT_OBSERVABLE', 'UNCERTAIN'].includes(item.result),
  );
  const passed = review.criterionResults.filter(
    (item) => item.result === 'PASS',
  );
  const lines: string[] = [];

  if (failed.length === 0 && uncertain.length === 0 && passed.length > 0) {
    lines.push(
      'AI整改复核结论：整改后图片未识别到与验收要求相冲突的明显可见隐患，建议进入人工验收。',
      `复核依据：${passed.map((item) => item.criterion).join('；')}`,
    );
  } else {
    const conclusion =
      failed.length > 0
        ? '整改后图片仍识别到明确不符合项，建议补充整改后再提交验收。'
        : '整改后图片存在无法确认的项目，建议人工核验后再决定是否验收。';
    lines.push(`AI整改复核结论：${conclusion}`);
    if (failed.length > 0) {
      lines.push('不符合项：');
      failed.forEach((item, index) => {
        lines.push(`${index + 1}. ${item.criterion}；${item.reason}`);
      });
    }
    if (uncertain.length > 0) {
      lines.push('待人工核验项：');
      uncertain.forEach((item, index) => {
        lines.push(`${index + 1}. ${item.criterion}；${item.reason}`);
      });
    }
    if (passed.length > 0) {
      lines.push(
        `图片可见范围内已满足：${passed.map((item) => item.criterion).join('；')}`,
      );
    }
  }

  if (review.missingMaterials.length > 0) {
    lines.push(`需补充材料：${review.missingMaterials.join('；')}`);
  }
  lines.push(
    '人工确认事项：AI仅核验图片中的可见事实；产品标准、证书、检测数据、培训记录及最终验收结论仍需人工确认。',
  );
  return lines.join('\n');
}

async function runAgentRectificationDescription() {
  const order = currentOrder.value;
  if (!order) return;
  if (!actionForm.afterPhoto.trim()) {
    message.warning('请先上传整改后照片');
    return;
  }
  const criteria =
    sourceAcceptanceCriteria().length > 0
      ? sourceAcceptanceCriteria()
      : agentAcceptanceCriteria();
  if (criteria.length === 0) {
    message.warning('缺少来源模块生成的验收要求或整改要求');
    return;
  }
  agentRectificationDescriptionLoading.value = true;
  try {
    const beforePhotos = compactStrings(
      order.items.map((item) => item.beforePhoto),
    );
    const review = await reviewHazardRectificationOrderWithAgentApi(order.id, {
      orderVersion: order.version,
      criteria,
      beforeEvidence: order.items.flatMap((item) =>
        compactStrings([item.hazardDescription, item.beforePhoto]),
      ),
      afterEvidence: [actionForm.afterPhoto],
      rectificationDescription: actionForm.rectificationDescription,
      beforeImageData: await inlineImageData(beforePhotos),
      afterImageData: await inlineImageData([actionForm.afterPhoto]),
    });
    agentReview.value = review;
    actionForm.rectificationDescription = reviewDescription(review);
    message.success('AI 复核内容已带入整改说明，可继续编辑');
  } finally {
    agentRectificationDescriptionLoading.value = false;
  }
}

async function runAgentRectificationReview() {
  const order = currentOrder.value;
  if (!order) return;
  const criteria = agentAcceptanceCriteria();
  if (criteria.length === 0) {
    message.warning('请先补充整改要求或验收标准');
    return;
  }
  agentReviewLoading.value = true;
  try {
    const beforePhotos = compactStrings(
      order.items.map((item) => item.beforePhoto),
    );
    const afterPhotos = compactStrings([order.rectificationAfterPhoto]);
    agentReview.value = await reviewHazardRectificationOrderWithAgentApi(
      order.id,
      {
        orderVersion: order.version,
        criteria,
        beforeEvidence: order.items.flatMap((item) =>
          compactStrings([item.hazardDescription, item.beforePhoto]),
        ),
        afterEvidence: compactStrings([
          order.rectificationDescription,
          order.rectificationAfterPhoto,
        ]),
        rectificationDescription: order.rectificationDescription,
        beforeImageData: await inlineImageData(beforePhotos),
        afterImageData: await inlineImageData(afterPhotos),
      },
    );
  } finally {
    agentReviewLoading.value = false;
  }
}

async function inlineImageData(urls: string[]) {
  const values: string[] = [];
  for (const url of urls.slice(0, 8)) {
    if (url.startsWith('data:image/')) {
      values.push(url);
      continue;
    }
    try {
      const response = await fetch(url, { credentials: 'include' });
      const blob = await response.blob();
      if (
        !response.ok ||
        !blob.type.startsWith('image/') ||
        blob.size > 6_000_000
      )
        continue;
      values.push(
        await new Promise<string>((resolve, reject) => {
          const reader = new FileReader();
          reader.addEventListener(
            'load',
            () => resolve(String(reader.result)),
            { once: true },
          );
          reader.addEventListener('error', () => reject(reader.error), {
            once: true,
          });
          reader.readAsDataURL(blob);
        }),
      );
    } catch {
      // URL remains in textual evidence; failure to inline must not block manual review.
    }
  }
  return values;
}

async function runAgentRectificationSummary() {
  const order = currentOrder.value;
  if (!order) return;
  agentSummaryLoading.value = true;
  try {
    const summary = await summarizeHazardRectificationOrderWithAgentApi(
      order.id,
      {
        orderVersion: order.version,
        hazardDescription: compactStrings(
          order.items.map((item) => item.hazardDescription),
        ).join('；'),
        rectificationDescription: order.rectificationDescription,
        acceptanceResult: order.acceptanceResult,
        acceptanceRemark: order.acceptanceRemark,
        measures: compactStrings(
          order.items.map((item) => item.defaultFollowUpPlan),
        ),
        acceptanceCriteria: agentAcceptanceCriteria(),
        flowFacts: order.flowLogs.map(
          (log) => `${log.actionLabel}：${log.remark || log.toStatusLabel}`,
        ),
        citations: order.items.map((item) => `order.item.${item.id}`),
      },
    );
    agentSummary.value = summary;
    agentSummaryEditJson.value = JSON.stringify(summary, null, 2);
    message.success('AI 总结草稿已生成，可编辑后保存或确认');
  } finally {
    agentSummaryLoading.value = false;
  }
}

async function confirmAgentRectificationSummary() {
  const order = currentOrder.value;
  const summary = agentSummary.value;
  if (!order || !summary) return;
  agentSummaryConfirming.value = true;
  try {
    await confirmHazardRectificationSummaryApi(
      order.id,
      summary.summaryVersion ?? 1,
      order.version,
      JSON.stringify(summary),
    );
    message.success('整改说明总结已确认并留存版本');
  } finally {
    agentSummaryConfirming.value = false;
  }
}

async function editAgentRectificationSummary() {
  const order = currentOrder.value;
  const summary = agentSummary.value;
  if (!order || !summary || !agentSummaryEditJson.value.trim()) return;
  agentSummaryEditing.value = true;
  try {
    await editHazardRectificationSummaryApi(
      order.id,
      summary.summaryVersion ?? 1,
      order.version,
      agentSummaryEditJson.value,
      '人工修订',
    );
    message.success('总结修订已保存');
    agentSummary.value = { ...summary, status: 'REVISED' };
  } finally {
    agentSummaryEditing.value = false;
  }
}

function onPageChange(page: number, pageSize: number) {
  query.page = page;
  query.pageSize = pageSize;
  void loadOrders();
}

onMounted(() => {
  void loadOrgTreeOptions();
  void loadResponsibleUsers();
  void loadOrders();
});
</script>

<template>
  <div class="hazard-order-page">
    <section class="hazard-order-toolbar">
      <label class="toolbar-filter">
        <span>来源类型</span>
        <Select
          v-model:value="query.sourceType"
          :options="sourceTypeOptions"
          @change="loadOrders"
        />
      </label>
      <label
        v-if="isHazardOrganizationFilterVisible('company')"
        class="toolbar-filter"
      >
        <span>公司</span>
        <Select
          v-model:value="query.companyId"
          :allow-clear="!isHazardOrganizationFieldLocked('company')"
          :disabled="isHazardOrganizationFieldLocked('company')"
          :options="companyOptions"
          placeholder="请选择公司"
          @change="
            (value) =>
              setCompany(
                value as PinganHazardRectificationOrderApi.Id | undefined,
              )
          "
        />
      </label>
      <label
        v-if="isHazardOrganizationFilterVisible('department')"
        class="toolbar-filter"
      >
        <span>部门</span>
        <Select
          v-model:value="query.departmentId"
          :allow-clear="!isHazardOrganizationFieldLocked('department')"
          :disabled="
            !query.companyId || isHazardOrganizationFieldLocked('department')
          "
          :options="departmentOptions"
          placeholder="请选择部门"
          @change="
            (value) =>
              setDepartment(
                value as PinganHazardRectificationOrderApi.Id | undefined,
              )
          "
        />
      </label>
      <label
        v-if="isHazardOrganizationFilterVisible('team')"
        class="toolbar-filter"
      >
        <span>班组</span>
        <Select
          v-model:value="query.teamId"
          :allow-clear="!isHazardOrganizationFieldLocked('team')"
          :disabled="
            !query.departmentId || isHazardOrganizationFieldLocked('team')
          "
          :options="teamOptions"
          placeholder="请选择班组"
          @change="
            (value) =>
              setTeam(value as PinganHazardRectificationOrderApi.Id | undefined)
          "
        />
      </label>
      <Button
        type="primary"
        :disabled="!canCreateOrder"
        @click="openCreateDrawer"
      >
        <IconifyIcon icon="lucide:plus" />
        新增工单
      </Button>
      <Button
        danger
        :disabled="selectedOrderRowKeys.length === 0 || !canDeleteOrder"
        @click="handleBatchDelete"
      >
        <IconifyIcon icon="lucide:trash-2" />
        批量删除
      </Button>
      <Button @click="loadOrders">
        <IconifyIcon icon="lucide:refresh-cw" />
        刷新
      </Button>
      <StatusFilterActions
        class="hazard-order-toolbar__status"
        :model-value="query.status"
        :options="statusOptions"
        search-text="查询"
        @reset="resetFilters"
        @search="loadOrders"
        @update:model-value="setStatusFilter"
      />
    </section>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="{ current: query.page, pageSize: query.pageSize, total }"
      :row-selection="rowSelection"
      row-key="id"
      :scroll="{ x: 1500 }"
      size="small"
      @change="
        (pagination: any) =>
          onPageChange(pagination.current, pagination.pageSize)
      "
    >
      <template #bodyCell="{ column, record }">
        <Tag
          v-if="column.dataIndex === 'status'"
          :color="statusTone[record.status] || 'default'"
        >
          {{ record.statusLabel }}
        </Tag>
        <span v-else-if="column.dataIndex === 'sourceType'">
          {{ sourceTypeLabel(record.sourceType) }}
        </span>
        <span v-else-if="column.dataIndex === 'sourceModuleKey'">
          {{ sourceModuleLabel(record.sourceModuleKey) }}
        </span>
        <Button
          v-else-if="column.dataIndex === 'actions'"
          size="small"
          type="link"
          @click="openDetail(record.id)"
        >
          <IconifyIcon icon="lucide:panel-right-open" />
          查看
        </Button>
      </template>
    </Table>

    <Modal
      v-model:open="detailOpen"
      destroy-on-close
      :footer="null"
      :width="1320"
      centered
      class="rectification-detail-drawer"
    >
      <template #title>
        <div class="detail-drawer-title">
          <span>隐患整改详情</span>
          <small>{{ currentOrder?.orderNo || '隐患整改工单' }}</small>
        </div>
      </template>
      <template v-if="currentOrder">
        <section
          class="detail-status-overview"
          :data-tone="detailStatusMeta.tone"
        >
          <div class="detail-status-main">
            <span class="detail-status-icon"><IconifyIcon :icon="detailStatusMeta.icon" /></span>
            <div>
              <strong>{{ currentOrder.statusLabel }}</strong>
              <span>{{ detailStatusMeta.subtitle }}</span>
            </div>
          </div>
          <dl class="detail-status-facts">
            <div>
              <dt>整改责任人</dt>
              <dd>
                <IconifyIcon icon="lucide:user-round" />{{
                  currentOrder.rectificationResponsibleUserName ||
                  userNameById(currentOrder.rectificationResponsibleUserId) ||
                  '-'
                }}
              </dd>
            </div>
            <div>
              <dt>验收人</dt>
              <dd>
                <IconifyIcon icon="lucide:user-check" />{{
                  currentOrder.acceptanceUserName ||
                  userNameById(currentOrder.acceptanceUserId) ||
                  '-'
                }}
              </dd>
            </div>
            <div>
              <dt>整改期限</dt>
              <dd>
                <IconifyIcon icon="lucide:clock-3" />{{
                  currentOrder.rectificationDeadline || '-'
                }}
              </dd>
            </div>
            <div>
              <dt>闭环时间</dt>
              <dd>
                <IconifyIcon icon="lucide:circle-check-big" />{{
                  currentOrder.closedAt || '-'
                }}
              </dd>
            </div>
          </dl>
        </section>

        <div class="detail-command-bar">
          <nav class="detail-tabs" aria-label="隐患整改详情分区">
            <button
              v-for="tab in detailTabs"
              :key="tab.key"
              type="button"
              :class="{ active: activeDetailTab === tab.key }"
              @click="activeDetailTab = tab.key"
            >
              {{ tab.label }}
            </button>
          </nav>

          <section v-if="availableActions.length > 0" class="order-action-bar">
            <Button
              v-for="action in availableActions"
              :key="action"
              :danger="action === 'CANCEL' || action === 'REJECT_ACCEPTANCE'"
              :type="action === 'CANCEL' ? 'default' : 'primary'"
              @click="
                openAction(action as PinganHazardRectificationOrderApi.Action)
              "
            >
              <IconifyIcon
                :icon="
                  action === 'REJECT_ACCEPTANCE'
                    ? 'lucide:undo-2'
                    : action === 'CANCEL'
                      ? 'lucide:circle-slash-2'
                      : 'lucide:arrow-right'
                "
              />
              {{
                actionLabels[
                  action as PinganHazardRectificationOrderApi.Action
                ]
              }}
            </Button>
          </section>
        </div>

        <div v-show="activeDetailTab === 'hazard'" class="detail-tab-panel">
          <section class="evidence-compare-grid">
            <article class="evidence-card evidence-card--before">
              <h3><span></span>整改前</h3>
              <div class="evidence-card__body">
                <video
                  v-if="
                    primaryHazardItem &&
                    itemHazardMediaUrl(primaryHazardItem) &&
                    itemHazardMediaIsVideo(primaryHazardItem)
                  "
                  controls
                  aria-label="整改前隐患视频"
                  :src="itemHazardMediaUrl(primaryHazardItem)"
                  class="evidence-card__media"
                ></video>
                <img
                  v-else-if="
                    primaryHazardItem && itemHazardMediaUrl(primaryHazardItem)
                  "
                  :src="itemHazardMediaUrl(primaryHazardItem)"
                  alt="整改前隐患现场"
                  class="evidence-card__media"
                  height="280"
                  loading="lazy"
                  width="520"
                />
                <div v-else class="evidence-card__empty">
                  <IconifyIcon icon="lucide:image-off" />暂无现场图片
                </div>
                <div class="evidence-card__copy">
                  <strong>{{
                    primaryHazardItem?.checkItem || '隐患现场'
                  }}</strong>
                  <p>
                    {{ primaryHazardItem?.hazardDescription || '暂无隐患描述' }}
                  </p>
                </div>
              </div>
            </article>
            <article class="evidence-card evidence-card--after">
              <h3><span></span>整改后</h3>
              <div class="evidence-card__body">
                <img
                  v-if="currentOrder.rectificationAfterPhoto"
                  :src="
                    resolveAttachmentPreviewUrl(
                      currentOrder.rectificationAfterPhoto,
                    )
                  "
                  alt="整改后照片"
                  class="evidence-card__media"
                  height="280"
                  loading="lazy"
                  width="520"
                />
                <div v-else class="evidence-card__empty">
                  <IconifyIcon icon="lucide:image-off" />等待提交整改证据
                </div>
                <div class="evidence-card__copy">
                  <strong>{{
                    currentOrder.rectifiedAt ? '整改已完成' : '尚未完成整改'
                  }}</strong>
                  <p>
                    {{
                      currentOrder.rectificationDescription ||
                      '整改结果提交后，将在此与问题现场并排对照。'
                    }}
                  </p>
                </div>
              </div>
            </article>
          </section>
          <section class="detail-callout">
            <IconifyIcon icon="lucide:settings-2" /><strong>整改措施</strong>
            <p>
              {{
                currentOrder.rectificationRequirement ||
                sourceRectificationMeasures() ||
                '待下发整改任务后补充'
              }}
            </p>
          </section>
          <section class="ai-assist-strip detail-ai-strip">
            <IconifyIcon icon="lucide:sparkles" />
            <div>
              <strong>AI 辅助意见</strong>
              <p>基于现场证据给出核对提示，仅供人工处置与验收参考。</p>
            </div>
            <Button type="link">展开查看</Button>
          </section>
          <section
            v-if="currentOrder.status === 'CLOSED'"
            class="acceptance-brief"
          >
            <span><IconifyIcon icon="lucide:circle-check-big" /></span>
            <strong>验收通过</strong>
            <dl>
              <div>
                <dt>验收人</dt>
                <dd>
                  {{
                    currentOrder.acceptanceUserName ||
                    userNameById(currentOrder.acceptanceUserId) ||
                    '-'
                  }}
                </dd>
              </div>
              <div>
                <dt>验收时间</dt>
                <dd>
                  {{
                    currentOrder.acceptanceAt || currentOrder.closedAt || '-'
                  }}
                </dd>
              </div>
              <div>
                <dt>验收意见</dt>
                <dd>
                  {{
                    currentOrder.acceptanceRemark ||
                    '整改措施已落实，现场风险已消除。'
                  }}
                </dd>
              </div>
            </dl>
          </section>
          <section
            v-if="currentOrder.flowLogs.length > 0"
            class="detail-section process-preview"
          >
            <div class="detail-section__title">
              <div>
                <h3>处理流程</h3>
                <p>以紧凑记录呈现关键节点，避免纵向时间轴拉长页面。</p>
              </div>
              <Button type="link" @click="activeDetailTab = 'history'">
                查看全部
              </Button>
            </div>
            <div class="process-table">
              <div class="process-table__head">
                <span>时间</span><span>处理内容</span><span>操作人</span><span>状态</span>
              </div>
              <article
                v-for="log in currentOrder.flowLogs.slice(0, 5)"
                :key="log.id"
                class="process-row"
              >
                <time>{{ log.createdAt }}</time>
                <div class="process-row__content">
                  <strong>{{ log.actionLabel }}</strong>
                  <p>
                    {{
                      log.remark ||
                      `${log.fromStatusLabel || '开始'} → ${log.toStatusLabel}`
                    }}
                  </p>
                </div>
                <span>{{ log.operatorName || '系统' }}</span>
                <Tag :color="statusTone[log.toStatus] || 'blue'">
                  {{ log.toStatusLabel }}
                </Tag>
              </article>
            </div>
          </section>
          <section v-if="currentOrder.items.length > 1" class="detail-section">
            <h3>全部隐患项（{{ currentOrder.items.length }}）</h3>
            <Table
              :columns="detailItemColumns"
              :data-source="currentOrder.items"
              :pagination="false"
              row-key="id"
              :scroll="{ x: 1050 }"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'hazardMedia'">
                  <video
                    v-if="
                      itemHazardMediaUrl(record) &&
                      itemHazardMediaIsVideo(record)
                    "
                    controls
                    aria-label="隐患证据视频"
                    :src="itemHazardMediaUrl(record)"
                    class="order-item-media"
                  ></video>
                  <img
                    v-else-if="itemHazardMediaUrl(record)"
                    :src="itemHazardMediaUrl(record)"
                    alt="隐患图片/视频"
                    class="order-item-media"
                    height="96"
                    loading="lazy"
                    width="132"
                  />
                  <span v-else>-</span>
                </template>
              </template>
            </Table>
          </section>
        </div>

        <div
          v-show="activeDetailTab === 'result'"
          class="detail-tab-panel detail-tab-panel--result"
        >
          <section class="result-status-banner">
            <span><IconifyIcon icon="lucide:circle-check-big" /></span>
            <strong>{{
              currentOrder.rectifiedAt ? '整改已完成' : '等待提交整改'
            }}</strong>
            <dl>
              <div>
                <dt>完成时间</dt>
                <dd>{{ currentOrder.rectifiedAt || '-' }}</dd>
              </div>
              <div>
                <dt>整改责任人</dt>
                <dd>
                  {{ currentOrder.rectificationResponsibleUserName || '-' }}
                </dd>
              </div>
              <div>
                <dt>提交状态</dt>
                <dd>
                  <Tag :color="currentOrder.rectifiedAt ? 'green' : 'orange'">
                    {{ rectificationSubmitStatusLabel }}
                  </Tag>
                </dd>
              </div>
            </dl>
          </section>
          <section class="evidence-compare-grid">
            <article class="evidence-card evidence-card--before">
              <h3><span></span>整改前</h3>
              <div class="evidence-card__body">
                <img
                  v-if="
                    primaryHazardItem && itemHazardMediaUrl(primaryHazardItem)
                  "
                  :src="itemHazardMediaUrl(primaryHazardItem)"
                  alt="整改前隐患现场"
                  class="evidence-card__media"
                />
                <div v-else class="evidence-card__empty">
                  <IconifyIcon icon="lucide:image-off" />暂无现场图片
                </div>
                <div class="evidence-card__copy">
                  <p>
                    {{ primaryHazardItem?.hazardDescription || '暂无隐患描述' }}
                  </p>
                  <small>
                    <IconifyIcon icon="lucide:user-round" />
                    上传人：{{
                      rectificationSubmitLog?.operatorName || '系统管理员'
                    }}
                  </small>
                  <small>
                    <IconifyIcon icon="lucide:clock-3" />
                    上传时间：{{
                      currentOrder.issuedAt || currentOrder.businessDate || '-'
                    }}
                  </small>
                </div>
              </div>
            </article>
            <article class="evidence-card evidence-card--after">
              <h3><span></span>整改后</h3>
              <div class="evidence-card__body">
                <img
                  v-if="currentOrder.rectificationAfterPhoto"
                  :src="
                    resolveAttachmentPreviewUrl(
                      currentOrder.rectificationAfterPhoto,
                    )
                  "
                  alt="整改后照片"
                  class="evidence-card__media"
                />
                <div v-else class="evidence-card__empty">
                  <IconifyIcon icon="lucide:image-off" />等待提交整改证据
                </div>
                <div class="evidence-card__copy">
                  <p>
                    {{
                      currentOrder.rectificationDescription || '暂无整改说明'
                    }}
                  </p>
                  <small>
                    <IconifyIcon icon="lucide:user-round" />
                    上传人：{{
                      rectificationSubmitLog?.operatorName ||
                      currentOrder.rectificationResponsibleUserName ||
                      '-'
                    }}
                  </small>
                  <small>
                    <IconifyIcon icon="lucide:clock-3" />
                    上传时间：{{ currentOrder.rectifiedAt || '-' }}
                  </small>
                  <small v-if="currentOrder.rectificationAfterPhoto">
                    <IconifyIcon icon="lucide:paperclip" />
                    附件数量：1 个
                  </small>
                </div>
              </div>
            </article>
          </section>
          <section class="detail-section result-description-card">
            <div class="detail-section__title">
              <div>
                <h3>整改说明</h3>
                <p>集中呈现整改措施、责任归属与实际完成信息</p>
              </div>
              <Tag :color="currentOrder.rectifiedAt ? 'green' : 'orange'">
                {{ currentOrder.rectifiedAt ? '已提交' : '待提交' }}
              </Tag>
            </div>
            <div class="result-layout result-layout--facts">
              <dl class="rectification-detail-list">
                <div class="wide">
                  <dt>整改措施</dt>
                  <dd>
                    {{
                      currentOrder.rectificationRequirement ||
                      sourceRectificationMeasures() ||
                      currentOrder.rectificationDescription ||
                      '-'
                    }}
                  </dd>
                </div>
                <div>
                  <dt>整改责任人</dt>
                  <dd>
                    <IconifyIcon icon="lucide:user-round" />
                    {{ currentOrder.rectificationResponsibleUserName || '-' }}
                  </dd>
                </div>
                <div>
                  <dt>整改部门</dt>
                  <dd>
                    {{
                      organizationNameById(
                        currentOrder.rectificationDepartmentId,
                      ) ||
                      currentOrder.department ||
                      '-'
                    }}
                  </dd>
                </div>
                <div class="wide">
                  <dt>实际完成时间</dt>
                  <dd>
                    <IconifyIcon icon="lucide:clock-3" />
                    {{ currentOrder.rectifiedAt || '-' }}
                  </dd>
                </div>
              </dl>
            </div>
          </section>
          <section class="order-result-panel result-ai-panel">
            <div class="order-agent-panel-head">
              <div class="result-ai-panel__heading">
                <span><IconifyIcon icon="lucide:sparkles" /></span>
                <div>
                  <h3>AI整改复核意见</h3>
                  <p>根据整改证据生成复核建议，最终验收仍由验收人确认。</p>
                </div>
              </div>
              <Button
                v-if="
                  ['RECTIFIED', 'PENDING_ACCEPTANCE'].includes(
                    currentOrder.status,
                  )
                "
                :loading="agentReviewLoading"
                @click="runAgentRectificationReview"
              >
                展开全文
              </Button>
            </div>
            <template v-if="agentReview">
              <Tag>{{ agentReview.overallSuggestion }}</Tag>
              <div
                v-for="item in agentReview.criterionResults"
                :key="item.criterion"
                class="order-agent-item"
              >
                <strong>{{ item.criterion }}</strong><Tag
                  :color="
                    item.result === 'PASS'
                      ? 'green'
                      : item.result === 'FAIL'
                        ? 'red'
                        : 'orange'
                  "
                >
                  {{ item.result }}
                </Tag>
                <p>{{ item.reason }}</p>
              </div>
              <p v-if="agentReview.missingMaterials.length > 0">
                需补充：{{ agentReview.missingMaterials.join('；') }}
              </p>
            </template>
          </section>
          <section class="detail-section submission-record-card">
            <div class="detail-section__title">
              <div>
                <h3>提交记录</h3>
                <p>保留整改结果的提交人、时间与版本信息</p>
              </div>
              <Tag color="blue">V{{ currentOrder.version }}.0</Tag>
            </div>
            <dl>
              <div>
                <dt>提交人</dt>
                <dd>
                  <IconifyIcon icon="lucide:user-round" />
                  {{
                    rectificationSubmitLog?.operatorName ||
                    currentOrder.rectificationResponsibleUserName ||
                    '-'
                  }}
                </dd>
              </div>
              <div>
                <dt>提交时间</dt>
                <dd>
                  <IconifyIcon icon="lucide:clock-3" />
                  {{
                    rectificationSubmitLog?.createdAt ||
                    currentOrder.rectifiedAt ||
                    '-'
                  }}
                </dd>
              </div>
              <div>
                <dt>版本号</dt>
                <dd>V{{ currentOrder.version }}.0</dd>
              </div>
            </dl>
          </section>
        </div>

        <div
          v-show="activeDetailTab === 'acceptance'"
          class="detail-tab-panel detail-tab-panel--acceptance"
        >
          <section
            class="acceptance-summary"
            :class="{ 'is-passed': currentOrder.status === 'CLOSED' }"
          >
            <span><IconifyIcon
                :icon="
                  currentOrder.status === 'CLOSED'
                    ? 'lucide:circle-check-big'
                    : 'lucide:shield-ellipsis'
                "
            /></span>
            <div class="acceptance-summary__copy">
              <strong>{{
                currentOrder.status === 'CLOSED' ? '验收通过' : '等待验收结论'
              }}</strong>
              <p>
                {{
                  currentOrder.acceptanceRemark ||
                  '验收人需依据现场证据和验收要求给出人工结论。'
                }}
              </p>
            </div>
            <dl>
              <div>
                <dt><IconifyIcon icon="lucide:user-round" /> 验收人</dt>
                <dd>
                  {{
                    currentOrder.acceptanceUserName ||
                    userNameById(currentOrder.acceptanceUserId) ||
                    '-'
                  }}
                </dd>
              </div>
              <div>
                <dt><IconifyIcon icon="lucide:clock-3" /> 验收时间</dt>
                <dd>{{ currentOrder.acceptanceAt || '-' }}</dd>
              </div>
            </dl>
          </section>
          <section class="detail-section acceptance-review-card">
            <h3><span></span>验收核对</h3>
            <div class="acceptance-review-table">
              <div class="acceptance-review-table__head">
                <span>序号</span><span>验收项</span><span>核对结果</span><span>说明</span>
              </div>
              <div
                v-for="(criterion, index) in acceptanceConclusionCriteria()"
                :key="criterion.label"
                class="acceptance-review-table__row"
              >
                <span>{{ index + 1 }}</span>
                <strong>{{ criterion.label }}</strong>
                <span class="acceptance-review-result"><IconifyIcon icon="lucide:circle-check-big" />符合</span>
                <p>{{ criterion.detail }}</p>
              </div>
            </div>
          </section>
          <section class="acceptance-info-grid">
            <article class="detail-section acceptance-opinion-card">
              <h3><IconifyIcon icon="lucide:file-text" />验收意见</h3>
              <p>
                {{
                  currentOrder.acceptanceRemark || '验收人尚未填写验收意见。'
                }}
              </p>
            </article>
            <article class="detail-section acceptance-meta-card">
              <h3><IconifyIcon icon="lucide:info" />验收信息</h3>
              <dl>
                <div>
                  <dt>验收部门</dt>
                  <dd>
                    {{
                      organizationNameById(currentOrder.departmentId) ||
                      currentOrder.department ||
                      '-'
                    }}
                  </dd>
                </div>
                <div>
                  <dt>验收人</dt>
                  <dd>{{ currentOrder.acceptanceUserName || '-' }}</dd>
                </div>
                <div>
                  <dt>验收方式</dt>
                  <dd>现场实地验收</dd>
                </div>
                <div>
                  <dt>闭环时间</dt>
                  <dd>{{ currentOrder.closedAt || '-' }}</dd>
                </div>
              </dl>
            </article>
          </section>
          <section class="detail-section acceptance-evidence-card">
            <h3><IconifyIcon icon="lucide:image" />证据引用</h3>
            <div class="acceptance-evidence-grid">
              <article>
                <img
                  v-if="
                    primaryHazardItem && itemHazardMediaUrl(primaryHazardItem)
                  "
                  :src="itemHazardMediaUrl(primaryHazardItem)"
                  alt="整改前证据"
                  height="86"
                  loading="lazy"
                  width="190"
                />
                <div>
                  <strong>整改前</strong>
                  <p>{{ primaryHazardItem?.hazardDescription || '-' }}</p>
                  <time>{{ currentOrder.issuedAt || '-' }}</time>
                </div>
              </article>
              <article>
                <img
                  v-if="currentOrder.rectificationAfterPhoto"
                  :src="
                    resolveAttachmentPreviewUrl(
                      currentOrder.rectificationAfterPhoto,
                    )
                  "
                  alt="整改后证据"
                  height="86"
                  loading="lazy"
                  width="190"
                />
                <div>
                  <strong>整改后</strong>
                  <p>{{ currentOrder.rectificationDescription || '-' }}</p>
                  <time>{{ currentOrder.rectifiedAt || '-' }}</time>
                </div>
              </article>
            </div>
          </section>
          <details
            v-if="currentOrder.status === 'CLOSED'"
            class="order-result-panel acceptance-ai-panel"
          >
            <summary class="order-agent-panel-head">
              <div>
                <h3><IconifyIcon icon="lucide:sparkles" />AI辅助复核</h3>
                <p>仅供参考，不改变人工验收结论。</p>
              </div>
              <IconifyIcon icon="lucide:chevron-down" />
            </summary>
            <Button
              :loading="agentSummaryLoading"
              @click="runAgentRectificationSummary"
            >
              生成复核总结
            </Button>
            <template v-if="agentSummary">
              <Tag>{{ agentSummary.status }}</Tag><Input.TextArea
                v-model:value="agentSummaryEditJson"
                :rows="5"
                placeholder="可编辑总结 JSON；保存后仍需人工确认冻结"
              />
              <div class="inline-actions">
                <Button
                  :loading="agentSummaryEditing"
                  @click="editAgentRectificationSummary"
                >
                  保存修订
</Button><Button
                  v-if="
                    agentSummary.status === 'DRAFT' ||
                    agentSummary.status === 'REVISED'
                  "
                  :loading="agentSummaryConfirming"
                  type="primary"
                  @click="confirmAgentRectificationSummary"
                >
                  确认并留存总结
                </Button>
              </div>
              <article
                v-for="section in agentSummary.sections"
                :key="section.key"
                class="order-agent-item"
              >
                <strong>{{ section.title }}</strong>
                <p>{{ section.content }}</p>
                <small>来源：{{ section.sourceRefs.join('、') }}</small>
              </article>
            </template>
          </details>
          <section class="acceptance-closure-bar">
            <span><IconifyIcon icon="lucide:circle-check-big" /></span>
            <strong>闭环状态</strong>
            <b>{{
              currentOrder.status === 'CLOSED' ? '已完成闭环' : '待闭环'
            }}</b>
            <div><small>闭环时间</small>{{ currentOrder.closedAt || '-' }}</div>
            <Button ghost type="primary" @click="activeDetailTab = 'history'">
              查看处理记录
            </Button>
          </section>
        </div>

        <div
          v-show="activeDetailTab === 'history'"
          class="detail-tab-panel detail-tab-panel--history"
        >
          <section class="detail-section history-record-card">
            <div class="history-toolbar">
              <div class="history-toolbar__title">
                <span></span>
                <h3>处理记录</h3>
                <p>
                  共{{ currentOrder.flowLogs.length }}个节点 ·
                  <b>{{
                    currentOrder.status === 'CLOSED'
                      ? '已全部完成'
                      : `当前显示 ${filteredFlowLogs.length} 条`
                  }}</b>
                </p>
              </div>
              <div class="history-toolbar__filters">
                <Select
                  v-model:value="historyActionFilter"
                  aria-label="处理类型筛选"
                  :options="historyActionOptions"
                />
                <Select
                  v-model:value="historyOperatorFilter"
                  aria-label="操作人筛选"
                  :options="historyOperatorOptions"
                />
                <Input.Search
                  v-model:value="historyKeyword"
                  allow-clear
                  aria-label="处理记录关键字搜索"
                  placeholder="请输入关键字搜索"
                />
              </div>
            </div>
            <div class="process-table process-table--history">
              <div class="process-table__head">
                <span>时间</span><span>处理内容</span><span>操作人</span><span>状态</span>
              </div>
              <article
                v-for="log in filteredFlowLogs"
                :key="log.id"
                class="process-row"
                :class="{ 'is-final': log.toStatus === 'CLOSED' }"
              >
                <div class="process-row__time">
                  <time>{{ log.createdAt }}</time>
                  <span class="process-row__marker">
                    <IconifyIcon icon="lucide:check" />
                  </span>
                </div>
                <div class="process-row__content">
                  <strong>{{ log.actionLabel }}</strong>
                  <p>{{ historyLogDescription(log) }}</p>
                  <details v-if="flowLogDetailItems(log).length > 0">
                    <summary>查看详情</summary>
                    <dl class="flow-log-details">
                      <template
                        v-for="detail in flowLogDetailItems(log)"
                        :key="detail.key"
                      >
                        <dt>{{ detail.label }}</dt>
                        <dd v-if="isFlowLogPhotoDetail(detail)">
                          <img
                            :src="detail.value"
                            :alt="detail.label"
                            class="flow-log-photo"
                            height="180"
                            loading="lazy"
                            width="260"
                          />
                        </dd>
                        <dd v-else>{{ detail.value }}</dd>
                      </template>
                    </dl>
                  </details>
                </div>
                <span class="process-row__operator"><IconifyIcon icon="lucide:user-round" />{{
                    log.operatorName || '系统'
                  }}</span>
                <Tag :color="log.toStatus === 'CLOSED' ? 'green' : 'blue'">
                  {{ log.toStatus === 'CLOSED' ? '已关闭' : '已完成' }}
                </Tag>
              </article>
              <div v-if="filteredFlowLogs.length === 0" class="history-empty">
                <IconifyIcon icon="lucide:search-x" />未找到符合条件的处理记录
              </div>
            </div>
          </section>
          <details class="system-log-panel">
            <summary>
              <span><IconifyIcon icon="lucide:file-text" />系统操作日志</span>
              <IconifyIcon icon="lucide:chevron-down" />
            </summary>
            <p>系统已保留状态变更、操作人和字段版本，可用于审计追溯。</p>
          </details>
          <div class="history-export-row">
            <Button type="primary" ghost @click="exportHistoryRecords">
              <IconifyIcon icon="lucide:download" />导出记录
            </Button>
          </div>
        </div>

        <div
          v-show="activeDetailTab === 'basic'"
          class="detail-tab-panel detail-tab-panel--basic"
        >
          <section class="basic-card-grid">
            <article class="basic-info-card">
              <h3><IconifyIcon icon="lucide:file-text" />工单信息</h3>
              <dl>
                <div>
                  <dt>工单编号</dt>
                  <dd>
                    <strong>{{ currentOrder.orderNo }}</strong>
                    <Button type="link" @click="copyCurrentOrderNo">
                      <IconifyIcon icon="lucide:copy" />复制工单编号
                    </Button>
                  </dd>
                </div>
                <div>
                  <dt>当前状态</dt>
                  <dd>
                    <Tag :color="statusTone[currentOrder.status] || 'default'">
                      {{ currentOrder.statusLabel }}
                    </Tag>
                  </dd>
                </div>
                <div>
                  <dt>隐患数量</dt>
                  <dd>{{ currentOrder.hazardCount }}</dd>
                </div>
                <div>
                  <dt>创建时间</dt>
                  <dd>{{ currentOrder.issuedAt || '-' }}</dd>
                </div>
              </dl>
            </article>
            <article class="basic-info-card">
              <h3><IconifyIcon icon="lucide:building-2" />业务归属</h3>
              <dl>
                <div>
                  <dt>项目</dt>
                  <dd>
                    <strong>{{ currentOrder.team || '-' }}</strong>
                  </dd>
                </div>
                <div>
                  <dt>来源</dt>
                  <dd>{{ sourceTypeLabel(currentOrder.sourceType) }}</dd>
                </div>
                <div>
                  <dt>来源模块</dt>
                  <dd>{{ sourceModuleLabel(currentOrder.sourceModuleKey) }}</dd>
                </div>
                <div>
                  <dt>来源单据</dt>
                  <dd>{{ currentOrder.sourceRecordNo || '-' }}</dd>
                </div>
              </dl>
            </article>
            <article class="basic-info-card">
              <h3><IconifyIcon icon="lucide:users-round" />责任与验收</h3>
              <dl>
                <div>
                  <dt>整改部门</dt>
                  <dd>
                    {{
                      organizationNameById(
                        currentOrder.rectificationDepartmentId,
                      ) ||
                      currentOrder.team ||
                      '-'
                    }}
                  </dd>
                </div>
                <div>
                  <dt>整改责任人</dt>
                  <dd class="with-icon">
                    <IconifyIcon icon="lucide:user-round" />
                    {{ currentOrder.rectificationResponsibleUserName || '-' }}
                  </dd>
                </div>
                <div>
                  <dt>验收部门</dt>
                  <dd>
                    {{
                      organizationNameById(
                        currentOrder.acceptanceDepartmentId,
                      ) ||
                      currentOrder.department ||
                      '-'
                    }}
                  </dd>
                </div>
                <div>
                  <dt>验收人</dt>
                  <dd class="with-icon">
                    <IconifyIcon icon="lucide:user-round" />
                    {{ currentOrder.acceptanceUserName || '-' }}
                  </dd>
                </div>
              </dl>
            </article>
            <article class="basic-info-card">
              <h3><IconifyIcon icon="lucide:clock-3" />时效信息</h3>
              <dl>
                <div>
                  <dt>整改期限</dt>
                  <dd>
                    <strong>{{
                      currentOrder.rectificationDeadline || '-'
                    }}</strong>
                    <Tag v-if="currentOrder.rectifiedAt" color="green">
                      {{ completedOnTime() ? '按期完成' : '逾期完成' }}
                    </Tag>
                  </dd>
                </div>
                <div>
                  <dt>实际完成时间</dt>
                  <dd>{{ currentOrder.rectifiedAt || '-' }}</dd>
                </div>
                <div>
                  <dt>提交验收时间</dt>
                  <dd>
                    {{
                      acceptanceSubmitLog?.createdAt ||
                      currentOrder.acceptanceAt ||
                      '-'
                    }}
                  </dd>
                </div>
                <div>
                  <dt>闭环时间</dt>
                  <dd>{{ currentOrder.closedAt || '-' }}</dd>
                </div>
              </dl>
            </article>
          </section>
          <details class="basic-extension-panel" open>
            <summary>
              <span><IconifyIcon icon="lucide:file-text" />扩展信息</span>
              <IconifyIcon icon="lucide:chevron-down" />
            </summary>
            <dl>
              <div>
                <dt>创建人</dt>
                <dd>
                  {{ currentOrder.flowLogs[0]?.operatorName || '系统管理员' }}
                </dd>
              </div>
              <div>
                <dt>更新时间</dt>
                <dd>
                  {{ currentOrder.closedAt || currentOrder.rectifiedAt || '-' }}
                </dd>
              </div>
              <div>
                <dt>数据来源</dt>
                <dd>{{ sourceTypeLabel(currentOrder.sourceType) }}</dd>
              </div>
              <div>
                <dt>内部编号</dt>
                <dd>{{ internalOrderNumber(currentOrder.orderNo) }}</dd>
              </div>
            </dl>
          </details>
        </div>
      </template>
    </Modal>

    <Drawer
      v-model:open="createOpen"
      destroy-on-close
      width="820"
      title="手工新增隐患整改工单"
    >
      <div class="create-form">
        <section class="create-grid">
          <Select
            v-model:value="createForm.companyId"
            :disabled="isHazardOrganizationFieldLocked('company')"
            :options="companyOptions"
            placeholder="请选择公司"
            @change="
              (value) =>
                setCreateCompany(
                  value as PinganHazardRectificationOrderApi.Id | undefined,
                )
            "
          />
          <Select
            v-model:value="createForm.departmentId"
            :disabled="isHazardOrganizationFieldLocked('department')"
            :options="createDepartmentOptions"
            placeholder="请选择部门"
            @change="
              (value) =>
                setCreateDepartment(
                  value as PinganHazardRectificationOrderApi.Id | undefined,
                )
            "
          />
          <Select
            v-model:value="createForm.teamId"
            :disabled="isHazardOrganizationFieldLocked('team')"
            :options="createTeamOptions"
            placeholder="请选择班组"
          />
          <DatePicker
            v-model:value="createForm.businessDate"
            class="form-control"
            placeholder="业务日期"
            value-format="YYYY-MM-DD"
          />
        </section>

        <section class="create-items">
          <div
            v-for="(item, index) in createItems"
            :key="index"
            class="create-item"
          >
            <Input v-model:value="item.riskType" placeholder="风险类型" />
            <Input v-model:value="item.checkItem" placeholder="检查项" />
            <Input.TextArea
              v-model:value="item.hazardDescription"
              placeholder="隐患描述"
            />
            <Input v-model:value="item.beforePhoto" placeholder="整改前照片" />
            <Button danger size="small" @click="removeCreateItem(index)">
              <IconifyIcon icon="lucide:trash-2" />
              删除明细
            </Button>
          </div>
          <Button type="dashed" @click="addCreateItem">
            <IconifyIcon icon="lucide:plus" />
            添加隐患明细
          </Button>
        </section>

        <section class="create-actions">
          <Button @click="createOpen = false">取消</Button>
          <Button
            :loading="createSubmitting"
            type="primary"
            @click="submitManualOrder"
          >
            提交
          </Button>
        </section>
      </div>
    </Drawer>

    <Modal
      v-model:open="actionOpen"
      :confirm-loading="actionSubmitting"
      :width="1180"
      :z-index="1100"
      class="rectification-action-modal"
      wrap-class-name="rectification-action-modal-wrap"
      @cancel="closeActionCard"
    >
      <template #title>
        <div class="action-card-title">
          <span class="action-card-title__icon"><IconifyIcon :icon="actionCardMeta.icon" /></span>
          <div>
            <strong>{{ actionCardMeta.title }}</strong><small>{{ actionCardMeta.description }}</small>
          </div>
          <Tag v-if="actionCardMeta.badge" color="blue">
            {{ actionCardMeta.badge }}
          </Tag>
        </div>
      </template>
      <div class="action-form">
        <template v-if="currentAction === 'ISSUE_RECTIFICATION'">
          <section class="action-hazard-summary">
            <div
              v-if="primaryHazardItem && itemHazardMediaUrl(primaryHazardItem)"
              class="action-hazard-summary__media"
            >
              <img
                :src="itemHazardMediaUrl(primaryHazardItem)"
                alt="隐患现场"
                height="120"
                loading="lazy"
                width="220"
              />
            </div>
            <div>
              <span class="section-kicker">隐患摘要</span><strong>{{
                primaryHazardItem?.hazardDescription ||
                primaryHazardItem?.checkItem ||
                '待整改隐患'
              }}</strong>
              <p>所属班组：{{ currentOrder?.team || '-' }}</p>
            </div>
          </section>
          <section class="action-form-section">
            <h3>责任与时限</h3>
            <div class="action-form-grid">
              <label><span><b>*</b> 整改部门</span><Select
                  v-model:value="actionForm.rectificationDepartmentId"
                  class="form-control"
                  :options="actionDepartmentOptions"
                  placeholder="请选择整改部门"
              /></label>
              <label><span><b>*</b> 整改责任人</span><AutoComplete
                  v-model:value="actionForm.rectificationResponsibleUserInput"
                  allow-clear
                  class="form-control person-combobox"
                  :options="editableUserOptions"
                  placeholder="请选择或输入整改责任人"
                  popup-class-name="rectification-person-dropdown"
              /><small class="person-input-hint">演示模式：可选择建议人员，也可直接输入任意姓名</small></label>
              <label><span><b>*</b> 整改期限</span><input
                  v-model="actionForm.rectificationDeadline"
                  aria-label="选择整改日期和时间"
                  class="form-control deadline-datetime-input"
                  step="300"
                  type="datetime-local"
                /><small class="person-input-hint">请选择完整的整改日期和时间</small></label>
              <label><span>验收人</span><AutoComplete
                  v-model:value="actionForm.acceptanceUserInput"
                  allow-clear
                  class="form-control acceptance-user-input person-combobox"
                  :options="editableUserOptions"
                  placeholder="请选择或输入验收人"
                  popup-class-name="rectification-person-dropdown"
              /><small class="person-input-hint">选填；演示模式下支持直接输入任意姓名</small></label>
            </div>
          </section>
          <section class="action-form-section">
            <label class="field-stack"><span><b>*</b> 整改要求</span><Input.TextArea
                v-model:value="actionForm.rectificationRequirement"
                :maxlength="500"
                :rows="4"
                show-count
                placeholder="整改措施（已自动带入来源模块内容，可编辑修改）"
            /></label>
          </section>
          <section class="action-form-section">
            <label class="field-stack"><span><b>*</b> 验收标准</span><Input.TextArea
                v-model:value="actionForm.acceptanceRequirement"
                :maxlength="500"
                :rows="3"
                show-count
                placeholder="请填写可核验、可量化的验收标准"
            /></label>
          </section>
          <section class="action-form-section">
            <div class="field-stack">
<span>补充附件 <em>选填</em></span>
              <input
                ref="supplementalAttachmentInputRef"
                accept=".jpg,.jpeg,.png,.gif,.webp,.pdf,.doc,.docx"
                aria-label="上传补充附件"
                class="supplemental-attachment-input"
                multiple
                type="file"
                @change="handleSupplementalAttachmentChange"
              />
              <div class="supplemental-attachment-box">
                <button
                  class="supplemental-attachment-trigger"
                  type="button"
                  @click="triggerSupplementalAttachmentUpload"
                >
                  <IconifyIcon icon="lucide:plus" />
                  <span>上传附件</span>
                </button>
                <div class="supplemental-attachment-content">
                  <p>支持图片、PDF、Word 文件，单个不超过 10MB，最多 5 个。</p>
                  <ul v-if="supplementalAttachmentFiles.length > 0">
                    <li
                      v-for="(file, index) in supplementalAttachmentFiles"
                      :key="`${file.name}-${file.lastModified}`"
                    >
                      <IconifyIcon icon="lucide:paperclip" />
                      <span>{{ file.name }}</span>
                      <small>{{ (file.size / 1024 / 1024).toFixed(2) }}MB</small>
                      <button
                        :aria-label="`移除 ${file.name}`"
                        type="button"
                        @click="removeSupplementalAttachment(index)"
                      >
                        <IconifyIcon icon="lucide:x" />
                      </button>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </section>
        </template>
        <template v-else-if="currentAction === 'MARK_RECTIFIED'">
          <section class="action-evidence-grid">
            <article>
              <h3><span></span>整改前（问题现场）</h3>
              <div class="action-evidence-body">
                <img
                  v-if="
                    primaryHazardItem && itemHazardMediaUrl(primaryHazardItem)
                  "
                  :src="itemHazardMediaUrl(primaryHazardItem)"
                  alt="整改前现场"
                  height="180"
                  loading="lazy"
                  width="520"
                />
                <div v-else class="result-empty">暂无整改前照片</div>
                <p>{{ primaryHazardItem?.hazardDescription || '-' }}</p>
              </div>
            </article>
            <article>
              <h3><span></span>整改后（已完成）</h3>
              <div class="action-evidence-body">
                <input
                  ref="rectifiedPhotoInputRef"
                  accept="image/*"
                  aria-label="上传整改后照片"
                  class="rectified-photo-input"
                  name="rectificationAfterPhoto"
                  type="file"
                  @change="handleRectifiedPhotoChange"
                /><img
                  v-if="actionForm.afterPhoto"
                  :src="resolveAttachmentPreviewUrl(actionForm.afterPhoto)"
                  alt="整改后照片预览"
                  class="rectified-photo-preview"
                  height="180"
                  width="520"
                /><button
                  v-else
                  type="button"
                  class="evidence-upload"
                  @click="triggerRectifiedPhotoUpload"
                >
                  <IconifyIcon icon="lucide:image-plus" /><span>上传整改后照片</span>
</button><Button
                  v-if="actionForm.afterPhoto"
                  type="dashed"
                  @click="triggerRectifiedPhotoUpload"
                >
                  <IconifyIcon icon="lucide:upload" />继续上传
                </Button>
              </div>
            </article>
          </section>
          <section class="action-form-section">
            <label class="field-stack"><span><b>*</b> 整改结果说明</span><Input.TextArea
                v-model:value="actionForm.rectificationDescription"
                :maxlength="500"
                :rows="4"
                show-count
                placeholder="整改说明；可手工填写，也可使用 AI 整改复核后继续编辑"
            /></label>
          </section>
          <section class="ai-assist-strip">
            <IconifyIcon icon="lucide:sparkles" />
            <div>
              <strong>AI 辅助检查</strong>
              <p>仅核对照片中的可见事实，结果只作提交前参考。</p>
            </div>
            <Button
              :loading="agentRectificationDescriptionLoading"
              @click="runAgentRectificationDescription"
            >
              生成辅助说明
            </Button>
          </section>
        </template>
        <template v-else-if="currentAction === 'REQUEST_ACCEPTANCE'">
          <Select
            v-model:value="actionForm.acceptanceDepartmentId"
            class="form-control"
            :options="actionDepartmentOptions"
            placeholder="请选择验收部门"
          />
          <Select
            v-model:value="actionForm.acceptanceUserId"
            allow-clear
            class="form-control acceptance-user-input"
            :options="acceptanceUserOptions"
            option-filter-prop="label"
            option-label-prop="label"
            placeholder="请选择验收人"
            popup-class-name="rectification-person-dropdown"
            show-search
          />
          <Input.TextArea
            v-model:value="actionForm.acceptanceRequirement"
            :rows="4"
            placeholder="验收要求；默认带入来源模块基于整改前图片生成的要求，可编辑"
          />
          <Input.TextArea
            v-model:value="actionForm.acceptanceRemark"
            placeholder="验收申请备注"
          />
        </template>
        <template v-if="currentAction === 'ACCEPT'">
          <section class="action-evidence-grid action-evidence-grid--compact">
            <article>
              <h3><span></span>整改前</h3>
              <div class="action-evidence-body">
                <img
                  v-if="
                    primaryHazardItem && itemHazardMediaUrl(primaryHazardItem)
                  "
                  :src="itemHazardMediaUrl(primaryHazardItem)"
                  alt="整改前现场"
                  height="180"
                  loading="lazy"
                  width="520"
                />
                <p>{{ primaryHazardItem?.hazardDescription || '-' }}</p>
              </div>
            </article>
            <article>
              <h3><span></span>整改后</h3>
              <div class="action-evidence-body">
                <img
                  v-if="currentOrder?.rectificationAfterPhoto"
                  :src="
                    resolveAttachmentPreviewUrl(
                      currentOrder.rectificationAfterPhoto,
                    )
                  "
                  alt="整改后现场"
                  height="180"
                  loading="lazy"
                  width="520"
                />
                <p>{{ currentOrder?.rectificationDescription || '-' }}</p>
              </div>
            </article>
          </section>
          <section class="action-form-section">
            <h3>验收核对 <b>*</b></h3>
            <ul class="acceptance-checklist">
              <li
                v-for="(criterion, index) in agentAcceptanceCriteria()"
                :key="criterion"
              >
                <IconifyIcon icon="lucide:circle-check-big" /><span>{{ index + 1 }}. {{ criterion }}</span><Tag color="green">待确认</Tag>
              </li>
              <li v-if="agentAcceptanceCriteria().length === 0">
                暂无结构化验收条目，请结合整改要求人工核对。
              </li>
            </ul>
          </section>
          <section class="action-form-section">
            <h3>验收结论 <b>*</b></h3>
            <div class="acceptance-decision">
              <button
                type="button"
                @click="currentAction = 'REJECT_ACCEPTANCE'"
              >
                <IconifyIcon icon="lucide:undo-2" />退回整改
</button><button
                type="button"
                class="active"
                @click="currentAction = 'ACCEPT'"
              >
                <IconifyIcon icon="lucide:circle-check-big" />验收通过
              </button>
            </div>
          </section>
          <div class="acceptance-action-fields action-form-grid">
            <div class="acceptance-person-field">
              <AutoComplete
                v-model:value="actionForm.acceptanceUserInput"
                allow-clear
                class="form-control acceptance-user-input person-combobox"
                :options="editableUserOptions"
                placeholder="请选择或输入验收人"
                popup-class-name="rectification-person-dropdown"
              />
              <small class="person-input-hint">演示模式：可选择建议人员，也可直接输入任意姓名</small>
            </div>
            <Input.TextArea
              v-model:value="actionForm.acceptanceRequirement"
              :rows="4"
              placeholder="验收要求；默认带入来源模块基于整改前图片生成的要求，可编辑"
            />
            <Input.TextArea
              v-model:value="actionForm.acceptanceRemark"
              :maxlength="500"
              show-count
              placeholder="验收意见"
            />
          </div>
        </template>
        <template v-else-if="currentAction === 'REJECT_ACCEPTANCE'">
          <section class="action-form-section">
            <h3>验收结论 <b>*</b></h3>
            <div class="acceptance-decision">
              <button
                type="button"
                class="active danger"
                @click="currentAction = 'REJECT_ACCEPTANCE'"
              >
                <IconifyIcon icon="lucide:undo-2" />退回整改
</button><button type="button" @click="currentAction = 'ACCEPT'">
                <IconifyIcon icon="lucide:circle-check-big" />验收通过
              </button>
            </div>
          </section>
          <div class="acceptance-action-fields">
            <div class="acceptance-person-field">
              <AutoComplete
                v-model:value="actionForm.acceptanceUserInput"
                allow-clear
                class="form-control acceptance-user-input person-combobox"
                :options="editableUserOptions"
                placeholder="请选择或输入验收人"
                popup-class-name="rectification-person-dropdown"
              />
              <small class="person-input-hint">演示模式：可选择建议人员，也可直接输入任意姓名</small>
            </div>
            <Input.TextArea
              v-model:value="actionForm.rejectReason"
              placeholder="驳回原因"
            />
          </div>
        </template>
        <template v-else-if="currentAction === 'CANCEL'">
          <Input.TextArea
            v-model:value="actionForm.cancelReason"
            placeholder="作废原因"
          />
        </template>
      </div>
      <template #footer>
        <div class="action-modal-footer">
          <span><IconifyIcon
              icon="lucide:info"
            />提交后将进入下一流程节点并保留操作记录</span>
          <div>
            <Button @click="closeActionCard">取消</Button><Button
              :danger="currentAction === 'REJECT_ACCEPTANCE'"
              type="primary"
              :loading="actionSubmitting"
              @click="submitAction"
            >
              {{ actionCardMeta.submit }}
            </Button>
          </div>
        </div>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.hazard-order-page {
  min-height: 100%;
  padding: 16px;
  background: #f6f8fb;
}

.hazard-order-toolbar {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  align-items: flex-end;
  padding: 12px;
  margin-bottom: 12px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.toolbar-filter {
  display: grid;
  flex: 1 1 126px;
  gap: 5px;
  min-width: 0;
}

.toolbar-filter > span {
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
  color: #334155;
}

.toolbar-filter :deep(.ant-select) {
  width: 100%;
  height: 40px;
}

.toolbar-filter :deep(.ant-select-single .ant-select-selector),
.hazard-order-toolbar :deep(.ant-btn) {
  height: 40px;
}

.toolbar-filter :deep(.ant-select-single .ant-select-selector),
.toolbar-filter :deep(.ant-select-selection-item),
.toolbar-filter :deep(.ant-select-selection-placeholder),
.hazard-order-toolbar :deep(.ant-btn) {
  display: flex;
  align-items: center;
}

.toolbar-filter :deep(.ant-select-selection-item),
.toolbar-filter :deep(.ant-select-selection-placeholder) {
  height: 100%;
  line-height: normal !important;
}

.hazard-order-toolbar :deep(.ant-btn) {
  justify-content: center;
  line-height: 1;
}

.hazard-order-toolbar__status {
  flex-shrink: 1;
  margin-left: auto;
}

.order-detail-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.order-detail-summary > div {
  min-height: 52px;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.order-detail-summary span {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: #64748b;
}

.order-action-bar {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
  min-height: 52px;
}

.order-result-panel {
  display: grid;
  gap: 10px;
  padding: 12px;
  margin-top: 14px;
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
}

.order-result-panel h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.order-agent-panel-head {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
}

.order-agent-panel-head p,
.order-agent-item p {
  margin: 4px 0 0;
  color: #64748b;
}

.order-agent-item {
  padding: 10px 0;
  border-top: 1px solid #e5e7eb;
}

.order-agent-item strong {
  margin-right: 8px;
}

.order-result-grid {
  display: grid;
  grid-template-columns: minmax(0, 180px) minmax(0, 1fr);
  gap: 10px;
}

.order-result-grid > div {
  min-width: 0;
  color: #1f2937;
  overflow-wrap: anywhere;
}

.order-result-grid span {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: #64748b;
}

.order-result-photo {
  width: 100%;
  max-height: 260px;
  object-fit: contain;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.order-item-media {
  width: 132px;
  max-height: 96px;
  object-fit: contain;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.order-flow {
  margin-top: 18px;
}

.order-flow span {
  display: block;
  margin-top: 4px;
  color: #64748b;
}

.flow-log-detail-block {
  margin-top: 8px;
}

.flow-log-details-title {
  margin: 0 0 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.flow-log-details {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 6px 10px;
  padding: 8px 10px;
  margin: 0;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.flow-log-details dt {
  color: #64748b;
}

.flow-log-details dd {
  min-width: 0;
  margin: 0;
  color: #1f2937;
  overflow-wrap: anywhere;
}

.flow-log-photo {
  width: min(260px, 100%);
  max-height: 180px;
  object-fit: contain;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.action-form {
  display: grid;
  gap: 14px;
}

.form-control {
  width: 100%;
}

.rectified-photo-upload {
  display: grid;
  gap: 10px;
}

.acceptance-action-fields {
  display: grid;
  gap: 10px;
}

.rectified-photo-input {
  display: none;
}

.rectified-photo-preview {
  width: 100%;
  height: clamp(220px, 24vw, 280px);
  max-height: none;
  object-fit: contain;
  object-position: center;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.create-form {
  display: grid;
  gap: 14px;
}

.create-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.create-items {
  display: grid;
  gap: 10px;
}

.create-item {
  display: grid;
  grid-template-columns: minmax(100px, 0.8fr) minmax(160px, 1fr);
  gap: 10px;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.create-item :deep(textarea),
.create-item :deep(.ant-input) {
  min-width: 0;
}

.create-item .ant-input-affix-wrapper,
.create-item textarea,
.create-item button {
  grid-column: span 2;
}

.create-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.detail-drawer-title {
  display: flex;
  gap: 18px;
  align-items: baseline;
  color: hsl(var(--foreground));
}

:global(.rectification-detail-drawer .ant-modal-content),
:global(.rectification-action-modal .ant-modal-content) {
  color: #172033;
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  box-shadow: 0 22px 64px rgb(15 40 75 / 14%);
}

:global(.rectification-detail-drawer) {
  width: min(1320px, calc(100vw - 48px)) !important;
}

:global(.rectification-action-modal) {
  width: min(1180px, calc(100vw - 48px)) !important;
}

:global(.rectification-action-modal-wrap) {
  z-index: 2100 !important;
}

:global(.rectification-detail-drawer .ant-modal-header),
:global(.rectification-action-modal .ant-modal-header) {
  padding: 18px 22px 14px;
  margin: 0;
  background: #fff;
  border-bottom: 1px solid #e7edf5;
  border-radius: 12px 12px 0 0;
}

:global(.rectification-detail-drawer .ant-modal-body) {
  max-height: calc(100vh - 118px);
  padding: 0 18px 18px;
  overflow-y: auto;
  overscroll-behavior: contain;
  background: #fff;
}

:global(.rectification-detail-drawer .ant-modal-close),
:global(.rectification-action-modal .ant-modal-close) {
  color: #52627a;
}

.detail-drawer-title span {
  font-size: 19px;
  font-weight: 700;
}

.detail-drawer-title small {
  font-size: 13px;
  font-weight: 500;
  color: hsl(var(--muted-foreground));
}

.detail-status-overview {
  display: flex;
  gap: 28px;
  align-items: center;
  padding: 16px 18px;
  background: #f8fafc;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.detail-status-main {
  display: flex;
  flex: 0 0 220px;
  gap: 12px;
  align-items: center;
  padding-right: 20px;
  border-right: 1px solid #dbe3ef;
}

.detail-status-icon {
  display: flex;
  flex: 0 0 42px;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  font-size: 22px;
  line-height: 1;
  color: #fff;
  background: #1677ff;
  border-radius: 50%;
}

.detail-status-icon svg {
  display: block;
  width: 22px;
  height: 22px;
}

.detail-status-overview[data-tone='success'] .detail-status-icon {
  background: #0aaa62;
}

.detail-status-overview[data-tone='warning'] .detail-status-icon {
  background: #d97706;
}

.detail-status-overview[data-tone='danger'] .detail-status-icon {
  background: #e5484d;
}

.detail-status-overview[data-tone='neutral'] .detail-status-icon {
  background: #64748b;
}

.detail-status-main strong,
.detail-status-main div > span {
  display: block;
}

.detail-status-main strong {
  font-size: 19px;
  color: #0f172a;
}

.detail-status-main div > span {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.detail-status-facts {
  display: grid;
  flex: 1;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 0;
}

.detail-status-facts > div {
  padding: 0 18px;
  border-left: 1px solid #e2e8f0;
}

.detail-status-facts > div:first-child {
  border-left: 0;
}

.detail-status-facts dt {
  margin-bottom: 6px;
  font-size: 12px;
  color: #64748b;
}

.detail-status-facts dd {
  display: flex;
  gap: 7px;
  align-items: center;
  margin: 0;
  font-weight: 600;
  color: #1e293b;
}

.detail-status-facts svg {
  color: #1677ff;
}

.detail-command-bar {
  display: flex;
  gap: 20px;
  align-items: stretch;
  margin-top: 10px;
  border-bottom: 1px solid #e2e8f0;
}

.detail-tabs {
  display: flex;
  flex: 1;
  gap: 28px;
  min-width: 0;
}

.detail-tabs button {
  position: relative;
  min-height: 52px;
  padding: 0 2px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.detail-tabs button.active {
  color: #1677ff;
}

.detail-tabs button.active::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  content: '';
  background: #1677ff;
}

.detail-tabs button:focus-visible {
  outline: 2px solid #1677ff;
  outline-offset: 2px;
}

.detail-tab-panel {
  display: grid;
  gap: 14px;
  padding: 14px 0 6px;
}

.detail-tab-panel--result {
  gap: 10px;
}

.detail-tab-panel--result .detail-section {
  padding: 12px 14px;
}

.detail-tab-panel--result .evidence-card__body {
  min-height: 162px;
  padding: 10px;
}

.detail-tab-panel--result .evidence-card__media {
  height: 138px;
}

.detail-tab-panel--result .rectification-detail-list dt,
.detail-tab-panel--result .rectification-detail-list dd {
  padding-top: 8px;
  padding-bottom: 8px;
}

.detail-tab-panel--result .order-result-panel {
  padding: 10px 14px;
  margin-top: 0;
}

.result-ai-panel__heading {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.result-ai-panel__heading > span {
  display: grid;
  flex: 0 0 30px;
  place-items: center;
  width: 30px;
  height: 30px;
  color: #1677ff;
  background: #eaf3ff;
  border-radius: 7px;
}

.detail-ai-strip {
  background: #f8fbff;
}

.acceptance-brief,
.result-status-banner {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 16px 18px;
  background: linear-gradient(90deg, #f1fcf7, #f8fffb);
  border: 1px solid #bdebd3;
  border-radius: 8px;
}

.acceptance-brief > span,
.result-status-banner > span {
  display: grid;
  flex: 0 0 42px;
  place-items: center;
  width: 42px;
  height: 42px;
  font-size: 23px;
  color: #fff;
  background: #0aaa62;
  border-radius: 50%;
}

.acceptance-brief > strong,
.result-status-banner > strong {
  flex: 0 0 auto;
  font-size: 18px;
  color: #079455;
}

.acceptance-brief dl,
.result-status-banner dl {
  display: grid;
  flex: 1;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 0;
}

.result-status-banner dl {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.acceptance-brief dl > div,
.result-status-banner dl > div {
  padding: 0 20px;
  border-left: 1px solid #cee9dc;
}

.acceptance-brief dt,
.result-status-banner dt {
  margin-bottom: 4px;
  font-size: 12px;
  color: #718096;
}

.acceptance-brief dd,
.result-status-banner dd {
  margin: 0;
  color: #1f365c;
}

.process-preview {
  padding: 14px 0 0;
}

.process-preview .detail-section__title {
  padding: 0 16px;
}

.evidence-compare-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.evidence-card,
.detail-section {
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.evidence-card h3,
.action-evidence-grid h3 {
  display: flex;
  gap: 9px;
  align-items: center;
  padding: 12px 14px;
  margin: 0;
  font-size: 15px;
  color: hsl(var(--foreground));
  border-bottom: 1px solid #e7edf5;
}

.evidence-card h3 {
  color: #172033;
}

.action-evidence-grid h3 {
  color: #172033 !important;
}

.evidence-card h3 span,
.action-evidence-grid h3 span {
  width: 4px;
  height: 18px;
  background: #1677ff;
  border-radius: 4px;
}

.evidence-card--after h3 span,
.action-evidence-grid article:nth-child(2) h3 span {
  background: #0aaa62;
}

.evidence-card__body {
  display: grid;
  grid-template-columns: minmax(180px, 44%) minmax(0, 1fr);
  gap: 16px;
  min-height: 182px;
  padding: 12px;
}

.evidence-card__media {
  width: 100%;
  height: 156px;
  object-fit: cover;
  background: #f1f5f9;
  border-radius: 6px;
}

.evidence-card__empty,
.result-empty {
  display: grid;
  place-items: center;
  min-height: 150px;
  color: #94a3b8;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
}

.evidence-card__copy strong {
  display: block;
  margin: 8px 0;
  color: #1e293b;
}

.evidence-card__copy p {
  margin: 0;
  line-height: 1.7;
  color: #475569;
}

.evidence-card__copy small {
  display: flex;
  gap: 7px;
  align-items: center;
  margin-top: 9px;
  font-size: 12px;
  color: #64748b;
}

.evidence-card__copy small svg {
  color: #5272a3;
}

.detail-callout {
  display: grid;
  grid-template-columns: 24px 90px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
  padding: 12px 14px;
  background: #f8fbff;
  border: 1px solid #d7e7ff;
  border-radius: 8px;
}

.detail-callout svg {
  font-size: 19px;
  color: #1677ff;
}

.detail-callout p {
  margin: 0;
  color: #334155;
  white-space: pre-line;
}

.detail-section {
  padding: 16px;
}

.detail-section > h3,
.detail-section__title h3 {
  margin: 0;
  font-size: 16px;
  color: #172033;
}

.detail-section__title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}

.detail-section__title p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #64748b;
}

.result-layout {
  display: grid;
  grid-template-columns: minmax(260px, 40%) minmax(0, 1fr);
  gap: 18px;
}

.result-layout--facts {
  grid-template-columns: 1fr;
}

.result-description-card .detail-section__title {
  margin-bottom: 10px;
}

.rectification-detail-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
}

.rectification-detail-list > div {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  grid-column: span 1;
  min-width: 0;
  border-top: 1px solid #edf1f6;
}

.rectification-detail-list > div:first-child {
  border-top: 0;
}

.rectification-detail-list > div.wide {
  grid-column: 1 / -1;
}

.rectification-detail-list dt,
.rectification-detail-list dd {
  min-width: 0;
  padding: 11px 14px;
  margin: 0;
}

.rectification-detail-list dt {
  color: #64748b;
  background: #f8fafc;
}

.rectification-detail-list dd {
  display: flex;
  gap: 7px;
  align-items: flex-start;
  line-height: 1.65;
  color: #1e293b;
  white-space: pre-line;
}

.rectification-detail-list dd svg {
  flex: 0 0 auto;
  margin-top: 3px;
  color: #1677ff;
}

.submission-record-card dl {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 0;
  border-top: 1px solid #e2e8f0;
}

.submission-record-card dl > div {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  padding: 12px 18px 0;
  border-left: 1px solid #e2e8f0;
}

.submission-record-card dl > div:first-child {
  padding-left: 0;
  border-left: 0;
}

.submission-record-card dt {
  color: #64748b;
}

.submission-record-card dd {
  display: flex;
  gap: 7px;
  align-items: center;
  min-width: 0;
  margin: 0;
  color: #1e293b;
}

.submission-record-card dd svg {
  color: #1677ff;
}

.result-facts,
.basic-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 24px;
  margin: 0;
}

.result-facts div,
.basic-info-grid div {
  min-width: 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf1f6;
}

.result-facts .wide,
.basic-info-grid .wide {
  grid-column: 1 / -1;
}

.result-facts dt,
.basic-info-grid dt {
  margin-bottom: 5px;
  font-size: 12px;
  color: #64748b;
}

.result-facts dd,
.basic-info-grid dd {
  margin: 0;
  line-height: 1.7;
  color: #1e293b;
  overflow-wrap: anywhere;
  white-space: pre-line;
}

.detail-tab-panel--basic {
  gap: 14px;
  padding-top: 16px;
}

.basic-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.basic-info-card {
  overflow: hidden;
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.basic-info-card h3 {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 15px 18px;
  margin: 0;
  font-size: 17px;
  color: #172033;
  border-bottom: 1px solid #e2e8f0;
}

.basic-info-card h3 svg {
  font-size: 23px;
  color: #1677ff;
}

.basic-info-card dl {
  padding: 0 22px;
  margin: 0;
}

.basic-info-card dl > div {
  display: grid;
  grid-template-columns: 142px minmax(0, 1fr);
  min-height: 63px;
  border-top: 1px solid #e7edf5;
}

.basic-info-card dl > div:first-child {
  border-top: 0;
}

.basic-info-card dt,
.basic-info-card dd {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
  margin: 0;
}

.basic-info-card dt {
  color: #64748b;
}

.basic-info-card dd {
  color: #172033;
  overflow-wrap: anywhere;
}

.basic-info-card dd strong {
  font-weight: 600;
}

.basic-info-card dd .ant-btn {
  display: flex;
  gap: 6px;
  align-items: center;
  padding-right: 0;
  margin-left: auto;
}

.basic-info-card dd.with-icon svg {
  font-size: 18px;
  color: #1677ff;
}

.basic-info-card .ant-tag {
  color: #079455;
  background: #def7e9;
  border-color: transparent;
}

.basic-extension-panel {
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.basic-extension-panel summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  font-weight: 600;
  color: #172033;
  cursor: pointer;
  list-style: none;
}

.basic-extension-panel summary::-webkit-details-marker {
  display: none;
}

.basic-extension-panel summary span {
  display: flex;
  gap: 10px;
  align-items: center;
}

.basic-extension-panel summary span svg {
  font-size: 20px;
  color: #526987;
}

.basic-extension-panel[open] summary > svg {
  transform: rotate(180deg);
}

.basic-extension-panel dl {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding: 0 18px 14px;
  margin: 0;
  border-top: 1px solid #e7edf5;
}

.basic-extension-panel dl > div {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  padding: 13px 18px 0;
  border-left: 1px solid #e2e8f0;
}

.basic-extension-panel dl > div:first-child {
  padding-left: 0;
  border-left: 0;
}

.basic-extension-panel dt {
  color: #64748b;
}

.basic-extension-panel dd {
  min-width: 0;
  margin: 0;
  color: #172033;
}

.acceptance-summary {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 16px 18px;
  background: #f8fafc;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.acceptance-summary > span {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  font-size: 25px;
  color: #1677ff;
  background: #eaf3ff;
  border-radius: 50%;
}

.acceptance-summary.is-passed {
  background: #f2fbf7;
  border-color: #bfead4;
}

.acceptance-summary.is-passed > span {
  color: #0aaa62;
  background: #dcf8e9;
}

.acceptance-summary strong {
  font-size: 18px;
  color: #172033;
}

.acceptance-summary p {
  margin: 3px 0 0;
  color: #475569;
}

.detail-tab-panel--acceptance {
  gap: 8px;
  padding-top: 10px;
  padding-bottom: 0;
}

.acceptance-summary__copy {
  flex: 1;
  min-width: 0;
}

.acceptance-summary dl {
  display: grid;
  flex: 0 0 47%;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
}

.acceptance-summary dl > div {
  padding: 0 24px;
  border-left: 1px solid #cce7d9;
}

.acceptance-summary dt {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-bottom: 4px;
  font-size: 12px;
  color: #64748b;
}

.acceptance-summary dt svg {
  font-size: 17px;
  color: #1677ff;
}

.acceptance-summary dd {
  margin: 0;
  font-weight: 600;
  color: #172033;
}

.detail-tab-panel--acceptance .detail-section {
  padding: 10px 14px;
}

.acceptance-review-card > h3,
.acceptance-evidence-card > h3,
.acceptance-info-grid h3 {
  display: flex;
  gap: 8px;
  align-items: center;
  margin: 0 0 10px;
  font-size: 15px;
  color: #172033;
}

.acceptance-review-card > h3 > span {
  width: 4px;
  height: 18px;
  background: #1677ff;
  border-radius: 3px;
}

.acceptance-evidence-card > h3 svg,
.acceptance-info-grid h3 svg {
  font-size: 18px;
  color: #1677ff;
}

.acceptance-review-table {
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
}

.acceptance-review-table__head,
.acceptance-review-table__row {
  display: grid;
  grid-template-columns: 72px minmax(220px, 1.15fr) 190px minmax(300px, 1.65fr);
  align-items: center;
}

.acceptance-review-table__head {
  font-size: 12px;
  color: #64748b;
  background: #f6f8fb;
}

.acceptance-review-table__head > *,
.acceptance-review-table__row > * {
  min-width: 0;
  padding: 8px 14px;
  margin: 0;
  border-left: 1px solid #e2e8f0;
}

.acceptance-review-table__head > *:first-child,
.acceptance-review-table__row > *:first-child {
  text-align: center;
  border-left: 0;
}

.acceptance-review-table__row {
  color: #334155;
  border-top: 1px solid #e2e8f0;
}

.acceptance-review-result {
  display: flex;
  gap: 7px;
  align-items: center;
  font-weight: 700;
  color: #079455;
}

.acceptance-review-result svg {
  font-size: 18px;
}

.acceptance-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.acceptance-opinion-card > p {
  min-height: 66px;
  padding: 12px 14px;
  margin: 0;
  line-height: 1.7;
  color: #334155;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.acceptance-meta-card dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px 16px;
  margin: 0;
}

.acceptance-meta-card dl > div {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 8px;
}

.acceptance-meta-card dt {
  color: #64748b;
}

.acceptance-meta-card dd {
  margin: 0;
  color: #172033;
}

.acceptance-evidence-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
}

.acceptance-evidence-grid article {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  min-width: 0;
}

.acceptance-evidence-grid article + article {
  padding-left: 22px;
  border-left: 1px solid #e2e8f0;
}

.acceptance-evidence-grid img {
  width: 100%;
  height: 82px;
  object-fit: cover;
  background: #f1f5f9;
  border-radius: 6px;
}

.acceptance-evidence-grid strong {
  color: #172033;
}

.acceptance-evidence-grid p {
  display: -webkit-box;
  margin: 5px 0;
  overflow: hidden;
  -webkit-line-clamp: 2;
  line-height: 1.55;
  color: #475569;
  -webkit-box-orient: vertical;
}

.acceptance-evidence-grid time {
  font-size: 12px;
  color: #64748b;
}

.acceptance-ai-panel {
  padding: 0;
  margin-top: 0;
}

.acceptance-ai-panel summary {
  padding: 10px 14px;
  cursor: pointer;
  list-style: none;
}

.acceptance-ai-panel summary::-webkit-details-marker {
  display: none;
}

.acceptance-ai-panel summary h3 {
  display: flex;
  gap: 8px;
  align-items: center;
}

.acceptance-ai-panel summary h3 svg,
.acceptance-ai-panel summary > svg {
  color: #1677ff;
}

.acceptance-ai-panel[open] {
  padding-bottom: 12px;
}

.acceptance-ai-panel[open] > :not(summary) {
  margin-right: 14px;
  margin-left: 14px;
}

.acceptance-ai-panel[open] summary > svg {
  transform: rotate(180deg);
}

.acceptance-closure-bar {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 10px 14px;
  background: #f8fffb;
  border: 1px solid #bfead4;
  border-radius: 8px;
}

.acceptance-closure-bar > span {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  color: #fff;
  background: #0aaa62;
  border-radius: 50%;
}

.acceptance-closure-bar > strong {
  color: #172033;
}

.acceptance-closure-bar > b {
  padding-right: 24px;
  color: #079455;
  border-right: 1px solid #cce7d9;
}

.acceptance-closure-bar > div {
  display: flex;
  flex: 1;
  gap: 14px;
  color: #334155;
}

.acceptance-closure-bar small {
  color: #64748b;
}

.inline-actions {
  display: flex;
  gap: 8px;
}

.process-table {
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
}

.process-table__head,
.process-row {
  display: grid;
  grid-template-columns: 180px minmax(320px, 1fr) 150px 110px;
  gap: 16px;
  align-items: start;
  padding: 12px 16px;
}

.process-table__head {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: #f6f8fb;
}

.process-row {
  position: relative;
  border-top: 1px solid #edf1f6;
}

.process-row time,
.process-row > span {
  color: #64748b;
}

.process-row__content strong {
  color: #172033;
}

.process-row__content p {
  margin: 3px 0 0;
  color: #64748b;
}

.process-row details {
  margin-top: 8px;
}

.process-row summary {
  color: #1677ff;
  cursor: pointer;
}

.detail-tab-panel--history {
  gap: 14px;
  padding-top: 18px;
}

.history-record-card {
  padding: 16px;
}

.history-toolbar {
  display: flex;
  gap: 20px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.history-toolbar__title {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 300px;
}

.history-toolbar__title > span {
  width: 5px;
  height: 22px;
  background: #1677ff;
  border-radius: 4px;
}

.history-toolbar__title h3 {
  margin: 0;
  font-size: 18px;
  color: #172033;
}

.history-toolbar__title p {
  margin: 0 0 0 8px;
  color: #64748b;
}

.history-toolbar__title b {
  font-weight: 600;
  color: #079455;
}

.history-toolbar__filters {
  display: grid;
  grid-template-columns: 150px 170px 260px;
  gap: 10px;
}

.process-table--history {
  border-color: #dbe3ef;
}

.process-table--history .process-table__head,
.process-table--history .process-row {
  grid-template-columns: 240px minmax(360px, 1fr) 190px 100px;
  gap: 0;
  padding: 0;
}

.process-table--history .process-table__head > span {
  padding: 11px 16px;
}

.process-table--history .process-row {
  align-items: stretch;
  min-height: 76px;
}

.process-table--history .process-row > * {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 13px 16px;
}

.process-table--history .process-row.is-final {
  background: #f2fbf7;
  border-color: #bfead4;
}

.process-row__time {
  position: relative;
  color: #526987;
}

.process-row__marker {
  position: absolute;
  top: 50%;
  right: -13px;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  color: #1677ff;
  background: #ddecff;
  border: 4px solid #fff;
  border-radius: 50%;
  transform: translateY(-50%);
}

.process-row__marker::before,
.process-row__marker::after {
  position: absolute;
  right: 9px;
  width: 1px;
  height: 30px;
  content: '';
  background: #bad5f4;
}

.process-row__marker::before {
  bottom: 21px;
}

.process-row__marker::after {
  top: 21px;
}

.process-table--history .process-row:first-of-type .process-row__marker::before,
.process-table--history .process-row:last-of-type .process-row__marker::after {
  display: none;
}

.process-table--history .process-row.is-final .process-row__marker {
  right: -19px;
  width: 38px;
  height: 38px;
  font-size: 20px;
  color: #fff;
  background: #0aaa62;
  border-color: #d8f5e6;
  box-shadow: 0 0 0 1px #9cdec0;
}

.process-table--history .process-row__content {
  display: grid;
  grid-template-columns: 135px minmax(0, 1fr);
  gap: 8px 18px;
  align-content: center;
  padding-left: 52px;
}

.process-table--history .process-row__content strong {
  font-size: 16px;
}

.process-table--history .process-row.is-final .process-row__content strong {
  color: #079455;
}

.process-table--history .process-row__content p {
  margin: 0;
}

.process-table--history .process-row__content details {
  grid-column: 2;
  margin: 0;
}

.process-row__operator {
  gap: 8px;
  color: #172033 !important;
}

.process-row__operator svg {
  font-size: 17px;
  color: #1677ff;
}

.process-table--history .ant-tag {
  place-self: center start;
  width: fit-content;
}

.process-table--history .process-row:not(.is-final) .ant-tag {
  color: #1677ff;
  background: #eaf3ff;
  border-color: transparent;
}

.process-table--history .process-row.is-final .ant-tag {
  color: #079455;
  background: #def7e9;
  border-color: transparent;
}

:global(
  .rectification-detail-drawer
    .history-toolbar__filters
    .ant-input-affix-wrapper
) {
  color: #172033;
  background: #fff;
  border-color: #dbe3ef;
}

:global(
  .rectification-detail-drawer .history-toolbar__filters .ant-select-selector
) {
  border-color: #dbe3ef !important;
}

:global(
  .rectification-detail-drawer .history-toolbar__filters input::placeholder
) {
  color: #94a3b8;
}

:global(
  .rectification-detail-drawer
    .history-toolbar__filters
    .ant-input-group-addon
    .ant-btn
) {
  color: #1677ff;
  background: #fff;
  border-color: #d9d9d9;
}

.history-empty {
  display: grid;
  gap: 8px;
  place-items: center;
  min-height: 220px;
  color: #94a3b8;
}

.history-empty svg {
  font-size: 26px;
}

.system-log-panel {
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.system-log-panel summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  font-weight: 600;
  color: #172033;
  cursor: pointer;
  list-style: none;
}

.system-log-panel summary::-webkit-details-marker {
  display: none;
}

.system-log-panel summary span {
  display: flex;
  gap: 10px;
  align-items: center;
}

.system-log-panel summary span svg {
  font-size: 20px;
  color: #526987;
}

.system-log-panel[open] summary > svg {
  transform: rotate(180deg);
}

.system-log-panel p {
  padding: 0 18px 14px;
  margin: 0;
  color: #64748b;
}

.history-export-row {
  display: flex;
  justify-content: flex-end;
}

.history-export-row .ant-btn {
  display: flex;
  gap: 7px;
  align-items: center;
}

.action-card-title {
  display: flex;
  gap: 12px;
  align-items: center;
  padding-right: 32px;
}

.action-card-title__icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  font-size: 21px;
  color: #fff;
  background: #1677ff;
  border-radius: 50%;
}

.action-card-title div {
  display: grid;
  gap: 2px;
}

.action-card-title strong {
  font-size: 19px;
  color: #172033 !important;
}

.action-card-title small {
  font-weight: 400;
  color: #64748b !important;
}

.action-card-title .ant-tag {
  margin-left: 8px;
}

.action-hazard-summary {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  align-items: center;
  padding: 14px;
  background: #f7faff;
  border: 1px solid #dfebfb;
  border-radius: 8px;
}

.action-hazard-summary__media img {
  width: 100%;
  height: 120px;
  object-fit: cover;
  border-radius: 6px;
}

.action-hazard-summary strong {
  display: block;
  margin: 8px 0;
  font-size: 16px;
  color: #172033;
}

.action-hazard-summary p {
  margin: 0;
  color: #64748b;
}

.section-kicker {
  font-size: 12px;
  font-weight: 700;
  color: #1677ff;
}

.action-form-section {
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.action-form-section h3 {
  margin: 0 0 12px;
  font-size: 15px;
  color: #172033 !important;
}

.action-form-section b,
.field-stack b,
.action-form-grid label > span b {
  color: #ef4444;
}

.action-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 18px;
}

.action-form-grid label,
.field-stack {
  display: grid;
  gap: 7px;
}

.action-form-grid label > span,
.field-stack > span {
  font-weight: 600;
  color: #172033 !important;
}

.field-stack > span em {
  margin-left: 4px;
  font-size: 12px;
  font-style: normal;
  font-weight: 500;
  color: #8793a8;
}

.person-input-hint {
  margin-top: -2px;
  font-size: 12px;
  color: #8793a8;
}

.acceptance-person-field {
  display: grid;
  gap: 7px;
  align-content: start;
}

:global(.rectification-person-dropdown) {
  z-index: 2200 !important;
}

.deadline-datetime-input {
  width: 100%;
  height: 32px;
  padding: 4px 11px;
  font: inherit;
  color: #172033;
  outline: none;
  color-scheme: light;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.deadline-datetime-input:hover {
  border-color: #4096ff;
}

.deadline-datetime-input:focus {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgb(5 145 255 / 10%);
}

.supplemental-attachment-input {
  display: none;
}

.supplemental-attachment-box {
  display: flex;
  gap: 18px;
  align-items: flex-start;
}

.supplemental-attachment-trigger {
  display: grid;
  flex: 0 0 112px;
  gap: 6px;
  place-content: center;
  min-height: 96px;
  color: #47617f;
  cursor: pointer;
  background: #fbfdff;
  border: 1px dashed #b8c7dc;
  border-radius: 8px;
}

.supplemental-attachment-trigger:hover {
  color: #1677ff;
  background: #f5f9ff;
  border-color: #69a8ff;
}

.supplemental-attachment-trigger svg {
  margin: 0 auto;
  font-size: 24px;
}

.supplemental-attachment-content {
  flex: 1;
  min-width: 0;
}

.supplemental-attachment-content > p {
  margin: 8px 0 10px;
  font-size: 13px;
  color: #718096;
}

.supplemental-attachment-content ul {
  display: grid;
  gap: 6px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.supplemental-attachment-content li {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  gap: 8px;
  align-items: center;
  padding: 7px 9px;
  color: #334155;
  background: #f8fafc;
  border: 1px solid #e5eaf1;
  border-radius: 6px;
}

.supplemental-attachment-content li > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.supplemental-attachment-content li small {
  color: #8793a8;
}

.supplemental-attachment-content li button {
  display: grid;
  padding: 2px;
  color: #8793a8;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.action-evidence-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.action-evidence-grid article {
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.action-evidence-body {
  display: grid;
  gap: 10px;
  padding: 12px;
}

.action-evidence-body img {
  width: 100%;
  height: clamp(220px, 24vw, 280px);
  object-fit: contain;
  object-position: center;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.action-evidence-body p {
  min-height: 42px;
  margin: 0;
  color: #64748b;
}

.evidence-upload {
  display: grid;
  place-items: center;
  height: clamp(220px, 24vw, 280px);
  font-size: 14px;
  color: #1677ff;
  cursor: pointer;
  background: #f8fbff;
  border: 1px dashed #9cc6ff;
  border-radius: 6px;
}

.evidence-upload svg {
  align-self: end;
  font-size: 30px;
}

.evidence-upload span {
  align-self: start;
}

.ai-assist-strip {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 12px 14px;
  background: #f4f8ff;
  border: 1px solid #d7e7ff;
  border-radius: 8px;
}

.ai-assist-strip > svg {
  font-size: 20px;
  color: #1677ff;
}

.ai-assist-strip p {
  margin: 2px 0 0;
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}

.acceptance-checklist {
  padding: 0;
  margin: 0;
  list-style: none;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
}

.acceptance-checklist li {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 11px 12px;
  border-top: 1px solid #edf1f6;
}

.acceptance-checklist li:first-child {
  border-top: 0;
}

.acceptance-checklist svg {
  font-size: 19px;
  color: #0aaa62;
}

.acceptance-decision {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.acceptance-decision button {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  font-weight: 700;
  color: hsl(var(--foreground));
  cursor: pointer;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
}

.acceptance-decision button.active {
  color: #079455;
  background: #f0fdf6;
  border-color: #12b76a;
}

.acceptance-decision button.active.danger {
  color: #dc2626;
  background: #fff6f6;
  border-color: #ef4444;
}

.action-modal-footer {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
}

.action-modal-footer > span {
  display: flex;
  gap: 7px;
  align-items: center;
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}

.action-modal-footer > div {
  display: flex;
  gap: 10px;
}

:global(.rectification-action-modal .ant-modal-body) {
  max-height: calc(100vh - 220px);
  padding: 18px 22px;
  overflow-y: auto;
  overscroll-behavior: contain;
  background: #fff;
}

:global(.rectification-action-modal .ant-modal-footer) {
  padding: 14px 22px;
  margin: 0;
  background: #fff;
  border-top: 1px solid #e7edf5;
  border-radius: 0 0 12px 12px;
}

:global(.rectification-detail-drawer .ant-select-selector),
:global(.rectification-action-modal .ant-select-selector),
:global(.rectification-detail-drawer .ant-input),
:global(.rectification-action-modal .ant-input),
:global(.rectification-action-modal .ant-picker) {
  color: #172033 !important;
  background: #fff !important;
}

:global(.rectification-action-modal .ant-input::placeholder) {
  color: #94a3b8;
}

:global(.dark) .detail-drawer-title span,
:global(.dark) .action-card-title strong,
:global(.dark) .action-form-section h3,
:global(.dark) .action-form-grid label > span,
:global(.dark) .field-stack > span {
  color: #172033;
}

:global(.dark) .detail-drawer-title small,
:global(.dark) .action-card-title small,
:global(.dark) .action-modal-footer > span,
:global(.dark) .ai-assist-strip p {
  color: #64748b;
}

:global(.dark) .action-form-section,
:global(.dark) .action-evidence-grid article {
  border-color: #dbe3ef;
}

:global(.dark) .acceptance-decision button {
  color: #172033;
  background: #fff;
  border-color: #cbd5e1;
}

:global(.dark) .acceptance-decision button.active {
  color: #079455;
  background: #f0fdf6;
  border-color: #12b76a;
}

:global(.dark) .acceptance-decision button.active.danger {
  color: #dc2626;
  background: #fff6f6;
  border-color: #ef4444;
}

@media (max-width: 900px) {
  .detail-status-overview {
    flex-direction: column;
    align-items: flex-start;
  }

  .detail-status-main {
    flex-basis: auto;
    width: 100%;
    padding-right: 0;
    padding-bottom: 12px;
    border-right: 0;
    border-bottom: 1px solid #dbe3ef;
  }

  .detail-status-facts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
    width: 100%;
  }

  .detail-status-facts > div {
    padding: 0;
    border-left: 0;
  }

  .evidence-compare-grid,
  .action-evidence-grid,
  .result-layout {
    grid-template-columns: 1fr;
  }

  .process-table__head {
    display: none;
  }

  .process-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .acceptance-brief,
  .result-status-banner {
    flex-wrap: wrap;
    align-items: flex-start;
  }

  .acceptance-brief dl,
  .result-status-banner dl {
    flex-basis: 100%;
  }

  .acceptance-summary {
    flex-wrap: wrap;
    align-items: flex-start;
  }

  .acceptance-summary dl {
    flex-basis: 100%;
  }

  .acceptance-info-grid,
  .acceptance-evidence-grid {
    grid-template-columns: 1fr;
  }

  .acceptance-evidence-grid article + article {
    padding-top: 10px;
    padding-left: 0;
    border-top: 1px solid #e2e8f0;
    border-left: 0;
  }

  .acceptance-review-table {
    overflow-x: auto;
  }

  .acceptance-review-table__head,
  .acceptance-review-table__row {
    min-width: 850px;
  }

  .history-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .history-toolbar__filters {
    grid-template-columns: repeat(2, minmax(0, 1fr)) minmax(220px, 1.4fr);
  }

  .process-table--history {
    overflow-x: auto;
  }

  .process-table--history .process-table__head {
    display: grid;
  }

  .process-table--history .process-table__head,
  .process-table--history .process-row {
    grid-template-columns: 210px minmax(420px, 1fr) 170px 100px;
    min-width: 980px;
  }

  .basic-card-grid {
    grid-template-columns: 1fr;
  }

  .basic-extension-panel dl {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .basic-extension-panel dl > div:nth-child(3) {
    padding-left: 0;
    border-left: 0;
  }
}

@media (max-width: 640px) {
  .detail-tabs {
    gap: 8px;
    overflow-x: auto;
  }

  .detail-tabs button {
    flex: 0 0 auto;
    padding-right: 8px;
    padding-left: 8px;
  }

  .detail-status-facts,
  .action-form-grid,
  .basic-info-grid,
  .result-facts {
    grid-template-columns: 1fr;
  }

  .evidence-card__body,
  .action-hazard-summary {
    grid-template-columns: 1fr;
  }

  .action-modal-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .action-modal-footer > div {
    justify-content: flex-end;
  }

  .acceptance-brief dl,
  .result-status-banner dl {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .acceptance-brief dl > div,
  .result-status-banner dl > div {
    padding: 0;
    border-left: 0;
  }

  .rectification-detail-list,
  .submission-record-card dl {
    grid-template-columns: 1fr;
  }

  .rectification-detail-list > div {
    grid-template-columns: 112px minmax(0, 1fr);
    grid-column: 1;
  }

  .submission-record-card dl > div {
    padding: 10px 0;
    border-top: 1px solid #e2e8f0;
    border-left: 0;
  }

  .acceptance-summary dl {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .acceptance-summary dl > div {
    padding: 0;
    border-left: 0;
  }

  .acceptance-evidence-grid article {
    grid-template-columns: 1fr;
  }

  .acceptance-evidence-grid img {
    height: 140px;
  }

  .acceptance-meta-card dl {
    grid-template-columns: 1fr;
  }

  .acceptance-closure-bar {
    flex-wrap: wrap;
    align-items: flex-start;
  }

  .acceptance-closure-bar > div {
    flex-basis: 100%;
  }

  .history-toolbar__title {
    flex-wrap: wrap;
    min-width: 0;
  }

  .history-toolbar__filters {
    grid-template-columns: 1fr;
  }

  .process-table--history .process-table__head {
    display: none;
  }

  .process-table--history .process-row {
    grid-template-columns: 1fr;
    min-width: 0;
  }

  .process-table--history .process-row > * {
    padding: 8px 12px;
  }

  .process-table--history .process-row__content {
    grid-template-columns: 1fr;
    padding-left: 12px;
  }

  .process-table--history .process-row__content details {
    grid-column: 1;
  }

  .process-row__marker {
    display: none;
  }

  .basic-info-card dl > div {
    grid-template-columns: 112px minmax(0, 1fr);
  }

  .basic-extension-panel dl {
    grid-template-columns: 1fr;
  }

  .basic-extension-panel dl > div {
    padding: 10px 0;
    border-top: 1px solid #e2e8f0;
    border-left: 0;
  }
}
</style>
