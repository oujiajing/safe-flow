<script setup lang="ts">
import type { TableColumnsType } from 'ant-design-vue';
import type { PinganHazardRectificationOrderApi } from '#/api/pingan/hazard-rectification-order';
import type { SystemManagementApi } from '#/api/system-management/types';

import { computed, onMounted, reactive, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { useAccessStore, useUserStore } from '@vben/stores';

import {
  Button,
  DatePicker,
  Drawer,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Table,
  Tag,
  Timeline,
} from 'ant-design-vue';

import {
  actionHazardRectificationOrderApi,
  batchDeleteHazardRectificationOrdersApi,
  createHazardRectificationOrderApi,
  getHazardRectificationOrderDetailApi,
  getHazardRectificationOrdersApi,
  uploadHazardRectificationOrderAttachmentApi,
} from '#/api/pingan/hazard-rectification-order';
import { getPinganOrgTreeApi } from '#/api/pingan/pre-shift-meeting';
import StatusFilterActions from '#/views/pingan/shared/StatusFilterActions.vue';
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
  type PinganOrganizationFilterKey,
} from '#/views/pingan/shared/organization-filter-permission';

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

const statusOptions: Array<{ label: string; value: HazardOrderStatusFilter }> = [
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
  ACCEPT: '验收通过',
  CANCEL: '作废',
  ISSUE_RECTIFICATION: '下发整改',
  MARK_RECTIFIED: '整改完成',
  REJECT_ACCEPTANCE: '验收驳回',
  REQUEST_ACCEPTANCE: '提交验收',
};
const flowLogPayloadLabels: Record<string, string> = {
  acceptanceDepartmentId: '验收部门',
  acceptanceRemark: '验收说明',
  acceptanceUserId: '验收人',
  afterPhoto: '整改后照片',
  cancelReason: '作废原因',
  rectificationDeadline: '整改期限',
  rectificationDepartmentId: '整改部门',
  rectificationDescription: '整改说明',
  rectificationRequirement: '整改要求',
  rectificationResponsibleUserId: '整改责任人',
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
const loading = ref(false);
const actionSubmitting = ref(false);
const createOpen = ref(false);
const createSubmitting = ref(false);
const rows = ref<PinganHazardRectificationOrderApi.OrderRow[]>([]);
const selectedOrderRowKeys = ref<PinganHazardRectificationOrderApi.Id[]>([]);
const total = ref(0);
const detailOpen = ref(false);
const currentOrder = ref<PinganHazardRectificationOrderApi.OrderDetail>();
const actionOpen = ref(false);
const currentAction = ref<PinganHazardRectificationOrderApi.Action>();
const rectifiedPhotoInputRef = ref<HTMLInputElement>();
const actionForm = reactive({
  acceptanceDepartmentId: undefined as
    | PinganHazardRectificationOrderApi.Id
    | undefined,
  acceptanceRemark: '',
  acceptanceUserId: '',
  afterPhoto: '',
  cancelReason: '',
  rectificationDeadline: '',
  rectificationDepartmentId: undefined as
    | PinganHazardRectificationOrderApi.Id
    | undefined,
  rectificationDescription: '',
  rectificationRequirement: '',
  rectificationResponsibleUserId: undefined as number | undefined,
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
  resolveScopedPinganOrganizationDefaults(userStore.userInfo?.orgId, orgNodes.value),
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

const itemColumns: TableColumnsType<PinganHazardRectificationOrderApi.OrderItem> = [
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
  const actions =
    status === 'PENDING_ASSIGN'
      ? ['ISSUE_RECTIFICATION', 'CANCEL']
      : status === 'PENDING_RECTIFY'
        ? ['MARK_RECTIFIED', 'CANCEL']
        : status === 'RECTIFIED'
          ? ['REQUEST_ACCEPTANCE', 'CANCEL']
          : status === 'PENDING_ACCEPTANCE'
            ? ['ACCEPT', 'REJECT_ACCEPTANCE', 'CANCEL']
            : [];
  return actions.filter((action) =>
    canPerformOrderAction(action as PinganHazardRectificationOrderApi.Action),
  );
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

function canPerformOrderAction(action: PinganHazardRectificationOrderApi.Action) {
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
  if (!selectedOrderRowKeys.value.length) {
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

async function openDetail(id: string) {
  currentOrder.value = await getHazardRectificationOrderDetailApi(id);
  detailOpen.value = true;
}

function resetActionForm() {
  actionForm.acceptanceDepartmentId = undefined;
  actionForm.acceptanceRemark = '';
  actionForm.acceptanceUserId = '';
  actionForm.afterPhoto = '';
  actionForm.cancelReason = '';
  actionForm.rectificationDeadline = '';
  actionForm.rectificationDepartmentId = undefined;
  actionForm.rectificationDescription = '';
  actionForm.rectificationRequirement = '';
  actionForm.rectificationResponsibleUserId = undefined;
  actionForm.rejectReason = '';
}

function openAction(action: PinganHazardRectificationOrderApi.Action) {
  if (!canPerformOrderAction(action)) {
    return;
  }
  resetActionForm();
  if (action === 'ISSUE_RECTIFICATION') {
    actionForm.rectificationDepartmentId =
      currentOrder.value?.rectificationDepartmentId ??
      currentOrder.value?.departmentId;
  }
  if (
    action === 'ACCEPT' ||
    action === 'REJECT_ACCEPTANCE' ||
    action === 'REQUEST_ACCEPTANCE'
  ) {
    actionForm.acceptanceDepartmentId =
      currentOrder.value?.acceptanceDepartmentId ??
      currentOrder.value?.departmentId;
    actionForm.acceptanceUserId = String(
      currentOrder.value?.acceptanceUserId ?? '',
    );
  }
  currentAction.value = action;
  actionOpen.value = true;
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

function normalizeOrgTree(nodes: OrgNode[]): OrgOption[] {
  return nodes.map((node) => ({
    children: normalizeOrgTree(node.children ?? []),
    label: node.title,
    orgType: node.orgType,
    value: node.id,
  }));
}

function flattenOrganizations(nodes: OrgNode[]): OrgNode[] {
  return nodes.flatMap((node) => [node, ...flattenOrganizations(node.children ?? [])]);
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

function optionsUnder(parentId: PinganHazardRectificationOrderApi.Id, orgType: 'DEPARTMENT' | 'TEAM') {
  const path = findOrganizationPath(orgNodes.value, parentId);
  const parent = path.at(-1);
  return flattenOrganizations(parent?.children ?? [])
    .filter((node) => node.orgType === orgType)
    .map((node) => ({ label: node.title, value: node.id }));
}

function findOrganizationPath(nodes: OrgNode[], id: PinganHazardRectificationOrderApi.Id): OrgNode[] {
  for (const node of nodes) {
    if (String(node.id) === String(id)) return [node];
    const childPath = findOrganizationPath(node.children ?? [], id);
    if (childPath.length) return [node, ...childPath];
  }
  return [];
}

function isHazardOrganizationFieldLocked(
  field: PinganOrganizationFilterKey,
) {
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
  return record.beforeVideo || record.beforePhoto || '';
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
      companyId: createForm.companyId!,
      departmentId: createForm.departmentId!,
      items: createItems.value.map((item) => ({
        beforePhoto: item.beforePhoto?.trim() || undefined,
        checkItem: item.checkItem.trim(),
        hazardDescription: item.hazardDescription.trim(),
        riskType: item.riskType?.trim() || undefined,
      })),
      teamId: createForm.teamId!,
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
  const rawValue = String(actionForm.acceptanceUserId).trim();
  if (!rawValue || !/^\d+$/.test(rawValue)) {
    return undefined;
  }
  return Number(rawValue);
}

function actionRequiresAcceptanceUser() {
  return (
    currentAction.value === 'ACCEPT' ||
    currentAction.value === 'REJECT_ACCEPTANCE'
  );
}

function actionPayload() {
  switch (currentAction.value) {
    case 'ISSUE_RECTIFICATION':
      return {
        acceptanceUserId: selectedAcceptanceUserId(),
        rectificationDepartmentId: actionForm.rectificationDepartmentId,
        rectificationDeadline: actionForm.rectificationDeadline,
        rectificationRequirement: actionForm.rectificationRequirement,
        rectificationResponsibleUserId: actionForm.rectificationResponsibleUserId,
      };
    case 'MARK_RECTIFIED':
      return {
        afterPhoto: actionForm.afterPhoto,
        rectificationDescription: actionForm.rectificationDescription,
      };
    case 'REQUEST_ACCEPTANCE':
      return {
        acceptanceDepartmentId: actionForm.acceptanceDepartmentId,
        acceptanceRemark: actionForm.acceptanceRemark,
        acceptanceUserId: selectedAcceptanceUserId(),
      };
    case 'ACCEPT':
      return {
        acceptanceRemark: actionForm.acceptanceRemark,
        acceptanceUserId: selectedAcceptanceUserId(),
      };
    case 'REJECT_ACCEPTANCE':
      return {
        acceptanceUserId: selectedAcceptanceUserId(),
        rejectReason: actionForm.rejectReason,
      };
    case 'CANCEL':
      return {
        cancelReason: actionForm.cancelReason,
      };
    default:
      return {};
  }
}

function unknownToText(value: unknown) {
  if (value === undefined || value === null) return '';
  if (typeof value === 'string') return value.trim();
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  return '';
}

function organizationNameById(value: unknown) {
  const id = unknownToText(value);
  if (!id) {
    return '';
  }
  return (
    flatOrganizations.value.find((organization) => String(organization.id) === id)
      ?.title ?? ''
  );
}

function flowLogPayloadValue(key: string, value: unknown) {
  if (['rectificationDepartmentId', 'acceptanceDepartmentId'].includes(key)) {
    return organizationNameById(value) || unknownToText(value);
  }
  return unknownToText(value);
}

function flowLogDetailItems(log: PinganHazardRectificationOrderApi.FlowLog) {
  const payload = log.payload ?? {};
  return Object.entries(flowLogPayloadLabels)
    .map(([key, label]): FlowLogDetail => ({
      key,
      label,
      value: flowLogPayloadValue(key, payload[key]),
    }))
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
    currentAction.value === 'REQUEST_ACCEPTANCE' &&
    !actionForm.acceptanceDepartmentId &&
    !selectedAcceptanceUserId()
  ) {
    message.error('请选择验收部门或填写验收人');
    return;
  }
  if (actionRequiresAcceptanceUser()) {
    const rawAcceptanceUserId = String(actionForm.acceptanceUserId).trim();
    if (!rawAcceptanceUserId) {
      message.error('请填写验收人');
      return;
    }
    if (!selectedAcceptanceUserId()) {
      message.error('验收人必须是数字');
      return;
    }
  }
  if (currentAction.value === 'CANCEL' && !actionForm.cancelReason.trim()) {
    message.error('请填写作废原因');
    return;
  }
  actionSubmitting.value = true;
  try {
    currentOrder.value = await actionHazardRectificationOrderApi(
      currentOrder.value.id,
      {
        action: currentAction.value,
        payload: actionPayload(),
        version: currentOrder.value.version,
      },
    );
    actionOpen.value = false;
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

function onPageChange(page: number, pageSize: number) {
  query.page = page;
  query.pageSize = pageSize;
  void loadOrders();
}

onMounted(() => {
  void loadOrgTreeOptions();
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
          @change="(value) => setCompany(value as PinganHazardRectificationOrderApi.Id | undefined)"
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
          :disabled="!query.companyId || isHazardOrganizationFieldLocked('department')"
          :options="departmentOptions"
          placeholder="请选择部门"
          @change="(value) => setDepartment(value as PinganHazardRectificationOrderApi.Id | undefined)"
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
          :disabled="!query.departmentId || isHazardOrganizationFieldLocked('team')"
          :options="teamOptions"
          placeholder="请选择班组"
          @change="(value) => setTeam(value as PinganHazardRectificationOrderApi.Id | undefined)"
        />
      </label>
      <Button type="primary" :disabled="!canCreateOrder" @click="openCreateDrawer">
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
      @change="(pagination: any) => onPageChange(pagination.current, pagination.pageSize)"
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

    <Drawer
      v-model:open="detailOpen"
      destroy-on-close
      width="920"
      :title="currentOrder?.orderNo || '隐患整改工单'"
    >
      <template v-if="currentOrder">
        <section class="order-detail-summary">
          <div>
            <span>状态</span>
            <Tag :color="statusTone[currentOrder.status] || 'default'">
              {{ currentOrder.statusLabel }}
            </Tag>
          </div>
          <div><span>来源</span>{{ sourceTypeLabel(currentOrder.sourceType) }}</div>
          <div>
            <span>来源模块</span>{{ sourceModuleLabel(currentOrder.sourceModuleKey) }}
          </div>
          <div><span>来源单据</span>{{ currentOrder.sourceRecordNo || '-' }}</div>
          <div><span>班组</span>{{ currentOrder.team }}</div>
          <div><span>隐患数</span>{{ currentOrder.hazardCount }}</div>
          <div><span>整改期限</span>{{ currentOrder.rectificationDeadline || '-' }}</div>
          <div><span>闭环时间</span>{{ currentOrder.closedAt || '-' }}</div>
        </section>

        <section v-if="availableActions.length" class="order-action-bar">
          <Button
            v-for="action in availableActions"
            :key="action"
            size="small"
            type="primary"
            @click="openAction(action as PinganHazardRectificationOrderApi.Action)"
          >
            <IconifyIcon
              :icon="action === 'REJECT_ACCEPTANCE' ? 'lucide:circle-x' : 'lucide:check-circle'"
            />
            {{ actionLabels[action as PinganHazardRectificationOrderApi.Action] }}
          </Button>
        </section>

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
                v-if="itemHazardMediaUrl(record) && itemHazardMediaIsVideo(record)"
                controls
                :src="itemHazardMediaUrl(record)"
                class="order-item-media"
              />
              <img
                v-else-if="itemHazardMediaUrl(record)"
                :src="itemHazardMediaUrl(record)"
                alt="隐患图片/视频"
                class="order-item-media"
              />
              <span v-else>-</span>
            </template>
          </template>
        </Table>

        <section
          v-if="
            currentOrder.rectificationDescription ||
            currentOrder.rectificationAfterPhoto ||
            currentOrder.rectifiedAt
          "
          class="order-result-panel"
        >
          <h3>整改结果</h3>
          <div class="order-result-grid">
            <div>
              <span>整改完成时间</span>
              {{ currentOrder.rectifiedAt || '-' }}
            </div>
            <div>
              <span>整改说明</span>
              {{ currentOrder.rectificationDescription || '-' }}
            </div>
          </div>
          <img
            v-if="currentOrder.rectificationAfterPhoto"
            :src="currentOrder.rectificationAfterPhoto"
            alt="整改后照片"
            class="order-result-photo"
          />
        </section>

        <Timeline class="order-flow">
          <Timeline.Item v-for="log in currentOrder.flowLogs" :key="log.id">
            <strong>{{ log.actionLabel }}</strong>
            <span>{{ log.toStatusLabel }} · {{ log.operatorName || '系统' }} · {{ log.createdAt }}</span>
            <p v-if="log.remark">{{ log.remark }}</p>
            <div
              v-if="flowLogDetailItems(log).length"
              class="flow-log-detail-block"
            >
              <p class="flow-log-details-title">流程明细</p>
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
                    />
                  </dd>
                  <dd v-else>{{ detail.value }}</dd>
                </template>
              </dl>
            </div>
          </Timeline.Item>
        </Timeline>
      </template>
    </Drawer>

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
            @change="(value) => setCreateCompany(value as PinganHazardRectificationOrderApi.Id | undefined)"
          />
          <Select
            v-model:value="createForm.departmentId"
            :disabled="isHazardOrganizationFieldLocked('department')"
            :options="createDepartmentOptions"
            placeholder="请选择部门"
            @change="(value) => setCreateDepartment(value as PinganHazardRectificationOrderApi.Id | undefined)"
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
      :title="currentAction ? actionLabels[currentAction] : '工单动作'"
      @ok="submitAction"
    >
      <div class="action-form">
        <template v-if="currentAction === 'ISSUE_RECTIFICATION'">
          <Select
            v-model:value="actionForm.rectificationDepartmentId"
            class="form-control"
            :options="actionDepartmentOptions"
            placeholder="请选择整改部门"
          />
          <InputNumber
            v-model:value="actionForm.rectificationResponsibleUserId"
            class="form-control"
            placeholder="整改责任人"
          />
          <Input
            v-model:value="actionForm.acceptanceUserId"
            class="form-control acceptance-user-input"
            placeholder="验收人"
          />
          <DatePicker
            v-model:value="actionForm.rectificationDeadline"
            class="form-control"
            format="YYYY-MM-DD HH:mm"
            placeholder="整改期限：2026-06-05 18:30"
            :show-time="{ format: 'HH:mm' }"
            value-format="YYYY-MM-DD HH:mm"
          />
          <Input.TextArea
            v-model:value="actionForm.rectificationRequirement"
            placeholder="整改要求"
          />
        </template>
        <template v-else-if="currentAction === 'MARK_RECTIFIED'">
          <Input.TextArea
            v-model:value="actionForm.rectificationDescription"
            placeholder="整改说明"
          />
          <div class="rectified-photo-upload">
            <input
              ref="rectifiedPhotoInputRef"
              accept="image/*"
              class="rectified-photo-input"
              type="file"
              @change="handleRectifiedPhotoChange"
            />
            <Button type="dashed" @click="triggerRectifiedPhotoUpload">
              <IconifyIcon icon="lucide:image-plus" />
              上传整改后照片
            </Button>
            <img
              v-if="actionForm.afterPhoto"
              :src="actionForm.afterPhoto"
              alt="整改后照片预览"
              class="rectified-photo-preview"
            />
          </div>
        </template>
        <template v-else-if="currentAction === 'REQUEST_ACCEPTANCE'">
          <Select
            v-model:value="actionForm.acceptanceDepartmentId"
            class="form-control"
            :options="actionDepartmentOptions"
            placeholder="请选择验收部门"
          />
          <Input
            v-model:value="actionForm.acceptanceUserId"
            class="form-control acceptance-user-input"
            placeholder="验收人"
          />
          <Input.TextArea
            v-model:value="actionForm.acceptanceRemark"
            placeholder="验收申请备注"
          />
        </template>
        <template v-if="currentAction === 'ACCEPT'">
          <div class="acceptance-action-fields">
            <Input
              v-model:value="actionForm.acceptanceUserId"
              class="form-control acceptance-user-input"
              placeholder="验收人"
            />
            <Input.TextArea
              v-model:value="actionForm.acceptanceRemark"
              placeholder="验收意见"
            />
          </div>
        </template>
        <template v-else-if="currentAction === 'REJECT_ACCEPTANCE'">
          <div class="acceptance-action-fields">
            <Input
              v-model:value="actionForm.acceptanceUserId"
              class="form-control acceptance-user-input"
              placeholder="验收人"
            />
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
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
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
  gap: 8px;
  margin-bottom: 12px;
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

.order-result-grid {
  display: grid;
  grid-template-columns: minmax(0, 180px) minmax(0, 1fr);
  gap: 10px;
}

.order-result-grid > div {
  min-width: 0;
  color: #1f2937;
  word-break: break-word;
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
  word-break: break-word;
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
  gap: 10px;
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
  max-height: 220px;
  object-fit: contain;
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
  justify-content: flex-end;
  gap: 8px;
}
</style>
