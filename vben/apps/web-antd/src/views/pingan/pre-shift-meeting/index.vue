<script setup lang="ts">
import type { CSSProperties } from 'vue';
import type { TableColumnsType, TableProps } from 'ant-design-vue';

import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';

import { Page, VbenScrollbar } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useAccessStore, useUserStore } from '@vben/stores';

import {
  Button,
  DatePicker,
  Empty,
  Image,
  Input,
  message,
  Modal,
  Select,
  Space,
  Table,
  Tree,
} from 'ant-design-vue';
import { useRoute } from 'vue-router';

import {
  createPreShiftMeetingApi,
  deletePreShiftMeetingApi,
  deletePreShiftMeetingAttachmentApi,
  getPinganCompanyOrgTreeApi,
  getPinganOrgTreeApi,
  getPinganUsersApi,
  getPreShiftMeetingDetailApi,
  getPreShiftMeetingsApi,
  remindPreShiftMeetingApi,
  submitPreShiftMeetingApi,
  uploadPreShiftMeetingAttachmentApi,
  withdrawPreShiftMeetingApi,
} from '#/api/pingan/pre-shift-meeting';
import type { PinganPreShiftMeetingApi } from '#/api/pingan/pre-shift-meeting';
import {
  batchThreeCheckRecordsApi,
  createThreeCheckRecordApi,
  deleteThreeCheckRecordAttachmentApi,
  deleteThreeCheckRecordApi,
  getHazardInspectionChangeHistoryApi,
  getHazardInspectionDocumentFlowApi,
  getThreeCheckRecordDetailApi,
  getThreeCheckRecordsApi,
  openThreeCheckRectificationOrderApi,
  remindThreeCheckRecordApi,
  submitThreeCheckRecordApi,
  uploadThreeCheckRecordAttachmentApi,
  updateThreeCheckRecordApi,
  workflowActionThreeCheckRecordApi,
  withdrawThreeCheckRecordApi,
} from '#/api/pingan/three-check-record';
import type { PinganThreeCheckRecordApi } from '#/api/pingan/three-check-record';
import { getTeamCheckTemplateResolveApi } from '#/api/system-management/team-check-item-template';

import {
  defaultExpandedOrganizationKeys,
  applyOrganizationFilterCompany,
  applyOrganizationFilterDepartment,
  applyOrganizationFilterFromDataMapSelection,
  applyOrganizationFilterTeam,
  collectExpandableOrganizationKeys,
  createOrganizationFilterParams,
  filterMeetingRows,
  filterOrganizationTree,
  getCascadedOrganizationOptions,
  getOrganizationIdByKey,
  getThreeCheckTemplateRows,
  organizationTree,
  resolveCreateOrganizationDefaults,
  resolveAttachmentPreviewUrl,
  statusMeta,
  statusOptions,
  upsertAttachmentPreview,
} from './pre-shift-meeting.data';
import {
  canDeleteThreeCheckRecord,
  canExportThreeCheckRecords,
  canOpenThreeCheckCreateDialog,
  canRemindThreeCheckRecord,
  canReviewQuickShotRecord,
  canSubmitThreeCheckRecord,
  canUploadThreeCheckAttachment,
  canWithdrawThreeCheckRecord,
  createDataMapCollapseState,
  createDataMapResizeState,
  createThreeCheckTableBodyCellClass,
  createThreeCheckTableColumnResizeState,
  createThreeCheckTableHeaderCellClass,
  createThreeCheckWorkbenchText,
  dataMapTreeContinuousScrollConfig,
  threeCheckTableColumnResizeConfig,
  threeCheckTableScrollConfig,
} from './pre-shift-meeting.view-state';
import { canShowPinganDataMap } from '#/views/pingan/shared/data-map-permission';
import StatusFilterActions from '#/views/pingan/shared/StatusFilterActions.vue';
import {
  getVisiblePinganOrganizationFilterKeys,
  isPinganOrganizationFilterLocked,
  resolveScopedPinganOrganizationDefaults,
  type PinganOrganizationFilterKey,
} from '#/views/pingan/shared/organization-filter-permission';
import {
  createSharedThreeCheckWorkbenchState,
  THREE_CHECK_LIST_AUTO_REFRESH_INTERVAL_MS,
} from '#/views/pingan/shared/three-check-workbench-state';
import {
  applyLocalThreeCheckAction,
  createHazardInspectionLine,
  createThreeCheckDetailEditForm,
  createLocalThreeCheckRow,
  createPreShiftMeetingRecordPayload,
  createThreeCheckRecordUpdatePayloadFromDetail,
  exportHazardInspectionLinesCsv,
  filterHazardInspectionLines,
  formatThreeCheckPayloadDisplayValue,
  getHazardInspectionViewStorageKey,
  getTeamCheckInspectionColumns,
  getTeamCheckInspectionStage,
  getThreeCheckAttachmentColumnKind,
  getThreeCheckCreateDisplayColumns,
  getThreeCheckDetailActions,
  getThreeCheckDetailDisplayColumns,
  getThreeCheckDetailFieldGroups,
  getThreeCheckTableDisplayColumns,
  getThreeCheckStatusBadgeTone,
  getThreeCheckFieldOptions,
  getThreeCheckFixedCompanyName,
  getThreeCheckOperationPolicy,
  getThreeCheckOrganizationSelectTypes,
  getThreeCheckSpecialFilterFields,
  getThreeCheckStatusFilterOptions,
  isPointsFlowRoute,
  isThreeCheckCreateAttachmentField,
  isThreeCheckDataMapEnabled,
  isThreeCheckDetailEditable,
  isThreeCheckAttachmentColumn,
  isThreeCheckAttachmentMissing,
  isThreeCheckMultiSelectField,
  isThreeCheckStatusBoxField,
  isTeamCheckInspectionRoute,
  normalizeHazardInspectionLines,
  normalizeTeamCheckInspectionLines,
  normalizeThreeCheckStatusBoxLabel,
  resolvePreShiftMeetingRuntime,
  serializeHazardInspectionPayload,
  serializeTeamCheckInspectionPayload,
  shouldUseThreeCheckListPreviewFallback,
  validateTeamCheckInspectionResults,
} from './pre-shift-meeting.module';
import type {
  HazardInspectionLine,
  TeamCheckInspectionLine,
  ThreeCheckDetailActionKey,
  ThreeCheckTableColumn,
} from './pre-shift-meeting.module';

import type {
  MeetingFilters,
  MeetingRow,
  OrganizationNode,
  OrganizationId,
} from './pre-shift-meeting.data';

type ThreeCheckTableRow = MeetingRow & Record<string, any>;
interface HazardInspectionDetailColumn {
  dataIndex: string;
  title: string;
  width: number;
}

type WorkbenchStatusFilter = NonNullable<MeetingFilters['status']>;

interface DocumentFlowField {
  kind?: 'IMAGE' | 'STATUS' | 'TEXT' | 'VIDEO';
  label: string;
  value: string;
}

interface DocumentFlowNode {
  date: string;
  fields: DocumentFlowField[];
  id: string;
  owner: string;
  status: string;
  subtitle: string;
  title: string;
  type: 'detail' | 'master';
}

interface DocumentFlowStep {
  date: string;
  description?: string;
  label: string;
  nodeId?: string;
  owner: string;
  state: 'current' | 'done' | 'pending';
}

interface ChangeHistoryGroup {
  action: string;
  attachmentCount: number;
  count: number;
  displayVersion: number;
  fieldCount: number;
  flowCount: number;
  id: string;
  items: PinganThreeCheckRecordApi.ChangeHistoryItem[];
  operator: string;
  time: string;
  version: number;
}

const {
  changeHistoryFilter,
  changeHistoryFilterOptions,
  changeHistoryKeyword,
  changeHistoryLoading,
  changeHistoryOpen,
  changeHistorySelectedGroupId,
  createForm,
  createOpen,
  createSaving,
  currentDetail,
  currentLocalDetail,
  currentThreeCheckDetail,
  dataMapOrganizationNodes,
  detailAttachmentSectionRef,
  detailEditForm,
  detailEditing,
  detailLoading,
  detailOpen,
  detailSaving,
  documentFlowKeyword,
  documentFlowLoading,
  documentFlowOpen,
  documentFlowSelectedNodeId,
  expandedOrganizationKeys,
  filters,
  formOrganizationNodes,
  loading,
  localCreateForm,
  rows,
  selectedOrganizationKeys,
  selectedRowKeys,
  tableBottomRailActive,
  tablePanelRef,
  teamCheckInspectionLines,
  teamCheckInspectionLoading,
  total,
  treeKeyword,
  userOptions,
  videoPreviewOpen,
  videoPreviewUrl,
} = createSharedThreeCheckWorkbenchState<
  ThreeCheckTableRow,
  TeamCheckInspectionLine
>(organizationTree, defaultExpandedOrganizationKeys);

const route = useRoute();
const accessStore = useAccessStore();
const userStore = useUserStore();
const pendingCreateAttachments = reactive<Record<string, File | undefined>>({});
const hazardInspectionLines = ref<HazardInspectionLine[]>([]);
const hazardInspectionLineKeyword = ref('');
const hazardInspectionViewOpen = ref(false);
const hazardInspectionVisibleColumnKeys = ref<string[]>([]);
const teamCheckInspectionResultOptions = [
  { label: '无隐患', value: '无隐患' },
  { label: '有隐患', value: '有隐患' },
  { label: '不适用', value: '不适用' },
];
const safetyCheckResultOptions = [
  { label: '无隐患', value: '无隐患' },
  { label: '有隐患', value: '有隐患' },
];
const safetyCheckAiEnabledOptions = [
  { label: '是', value: '是' },
  { label: '否', value: '否' },
];
const quickShotWorkflowActionOpen = ref(false);
const quickShotWorkflowActionSaving = ref(false);
const quickShotWorkflowActionRecord = ref<Record<string, any>>();
const quickShotWorkflowAction =
  ref<PinganThreeCheckRecordApi.QuickShotWorkflowAction>();
const quickShotWorkflowActionForm = reactive<Record<string, string>>({});
const quickShotWorkflowPhotoUploading = ref(false);
const documentFlowActiveTab = ref<'list' | 'tree'>('tree');
const currentDocumentFlow =
  ref<PinganThreeCheckRecordApi.DocumentFlowResponse>();
const currentChangeHistory =
  ref<PinganThreeCheckRecordApi.ChangeHistoryResponse>();
const { dataMapToggleLabel, isDataMapCollapsed, toggleDataMap } =
  createDataMapCollapseState();
const {
  beginDataMapResize,
  dataMapWidth,
  endDataMapResize,
  isDataMapResizing,
  updateDataMapResize,
} = createDataMapResizeState();
const {
  beginColumnResize,
  columnWidths,
  endColumnResize,
  isColumnResizing,
  updateColumnResize,
} = createThreeCheckTableColumnResizeState();
const activeDetailPanel = ref<'attachment' | 'changeHistory' | 'documentFlow'>();
let listAutoRefreshTimer: number | undefined;

const filteredTree = computed(() =>
  filterOrganizationTree(dataMapOrganizationNodes.value, treeKeyword.value),
);

const formOrganizationSource = computed(() =>
  formOrganizationNodes.value.length ? formOrganizationNodes.value : organizationTree,
);

const companySelectOptions = computed(() =>
  getCascadedOrganizationOptions(formOrganizationSource.value, undefined, [
    'COMPANY',
  ]).map((item) => ({
    label: item.label,
    value: item.value,
  })),
);

const departmentSelectOptions = computed(() =>
  getCascadedOrganizationOptions(
    formOrganizationSource.value,
    createForm.companyId,
    ['DEPARTMENT'],
  ).map((item) => ({
    label: item.label,
    value: item.value,
  })),
);

const teamSelectOptions = computed(() =>
  getCascadedOrganizationOptions(
    formOrganizationSource.value,
    createForm.departmentId,
    ['TEAM'],
  ).map((item) => ({
    label: item.label,
    value: item.value,
  })),
);

const filterCompanySelectOptions = computed(() =>
  getCascadedOrganizationOptions(formOrganizationSource.value, undefined, [
    'COMPANY',
  ]).map((item) => ({
    label: item.label,
    value: item.value,
  })),
);

const filterDepartmentSelectOptions = computed(() =>
  getCascadedOrganizationOptions(
    formOrganizationSource.value,
    filters.companyId,
    ['DEPARTMENT'],
  ).map((item) => ({
    label: item.label,
    value: item.value,
  })),
);

const filterTeamSelectOptions = computed(() =>
  getCascadedOrganizationOptions(
    formOrganizationSource.value,
    filters.departmentId,
    ['TEAM'],
  ).map((item) => ({
    label: item.label,
    value: item.value,
  })),
);

const departmentSelectValue = computed<OrganizationId | undefined>({
  get: () =>
    departmentSelectOptions.value.some(
      (item) => String(item.value) === String(createForm.departmentId),
    )
      ? createForm.departmentId
      : undefined,
  set: (value) => {
    if (isOrganizationId(value)) {
      createForm.departmentId = value;
    }
  },
});

const teamSelectValue = computed<OrganizationId | undefined>({
  get: () =>
    teamSelectOptions.value.some(
      (item) => String(item.value) === String(createForm.teamId),
    )
      ? createForm.teamId
      : undefined,
  set: (value) => {
    if (isOrganizationId(value)) {
      createForm.teamId = value;
    }
  },
});

const ownerSelectOptions = computed(() =>
  userOptions.value.map((item) => ({
    label: item.realName,
    value: item.id,
  })),
);

const attendeeSelectOptions = computed(() =>
  userOptions.value.map((item) => ({
    label: item.realName,
    value: item.realName,
  })),
);

function resolveCurrentOwnerUserId() {
  const currentUserId = userStore.userInfo?.userId;
  const currentUserOption = ownerSelectOptions.value.find(
    (item) => String(item.value) === String(currentUserId),
  );
  return currentUserOption?.value ?? ownerSelectOptions.value[0]?.value ?? 2;
}

const dataMapGridStyle = computed<CSSProperties>(() => ({
  '--data-map-width': `${dataMapWidth.value}px`,
}));

const workbenchText = computed(() =>
  createThreeCheckWorkbenchText(moduleRuntime.value.title),
);
const moduleRuntime = computed(() =>
  resolvePreShiftMeetingRuntime(
    String(route.name || ''),
    String(route.meta.title || '一班三查'),
  ),
);
const showDataMap = computed(() =>
  isThreeCheckDataMapEnabled(moduleRuntime.value.routeName) &&
  canShowPinganDataMap(userStore.userInfo?.roles),
);
const currentUserRoles = computed(() => userStore.userInfo?.roles ?? []);
const currentUserOrgDefaults = computed(() =>
  resolveScopedPinganOrganizationDefaults(
    userStore.userInfo?.orgId,
    formOrganizationSource.value,
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
const fixedCompanyName = computed(() =>
  getThreeCheckFixedCompanyName(moduleRuntime.value.routeName),
);
const fixedCompanyOption = computed(() =>
  fixedCompanyName.value
    ? companySelectOptions.value.find(
        (item) => item.label === fixedCompanyName.value,
      )
    : undefined,
);
const activeCompanySelectOptions = computed(() =>
  fixedCompanyOption.value
    ? [fixedCompanyOption.value]
    : companySelectOptions.value,
);
const activeFilterCompanySelectOptions = computed(() =>
  fixedCompanyOption.value
    ? [fixedCompanyOption.value]
    : filterCompanySelectOptions.value,
);
const columns = computed<TableColumnsType<ThreeCheckTableRow>>(() =>
  getThreeCheckTableDisplayColumns(
    moduleRuntime.value.routeName,
    moduleRuntime.value.columns,
  ).map((column: ThreeCheckTableColumn) => ({
      ...column,
      ellipsis: column.dataIndex !== 'actions',
      customCell: () => ({
        class: {
          ...createThreeCheckTableBodyCellClass(column),
          'meeting-table__actions-cell': column.dataIndex === 'actions',
          'meeting-table__attachment-cell': isThreeCheckAttachmentColumn(column),
          'meeting-table__status-cell': isThreeCheckStatusBoxField(column),
        },
      }),
      customHeaderCell: () => ({
        class: {
          ...createThreeCheckTableHeaderCellClass(column),
        },
        onMousedown: (event: MouseEvent) =>
          handleColumnResizeStart(event, column),
      }),
      width: columnWidths[column.dataIndex] ?? column.width,
    })),
);
const meetingTableClass = computed(() => [
  'meeting-table',
  {
    'meeting-table--bottom-rail': tableBottomRailActive.value,
    'meeting-table--column-resizing': isColumnResizing.value,
  },
]);
const localCreateColumns = computed(() =>
  getThreeCheckCreateDisplayColumns(
    moduleRuntime.value.routeName,
    moduleRuntime.value.columns,
  ),
);
const localDetailColumns = computed(() =>
  getThreeCheckDetailDisplayColumns(
    moduleRuntime.value.routeName,
    moduleRuntime.value.columns,
  ),
);
const detailFieldGroups = computed(() =>
  getThreeCheckDetailFieldGroups(
    moduleRuntime.value.routeName,
    localDetailColumns.value,
  ),
);
const detailActions = computed(() =>
  getThreeCheckDetailActions(moduleRuntime.value.routeName),
);
const detailAttachmentLabel = computed(() =>
  isQuickShotModule.value ? '隐患图片/视频' : '附件',
);
const wideDetailRouteNames = [
  'PinganQuickShot',
  'PinganCurtainWallPenalty',
  'PinganCurtainWallRoutineCheck',
];
const bottomDetailAttachmentRouteNames = [
  'PinganQuickShot',
  'PinganCurtainWallPenalty',
  'PinganCurtainWallRoutineCheck',
];
const hazardInspectionRouteNames = [
  'PinganSafetyCheck',
  'PinganQuickShot',
  'PinganCurtainWallPenalty',
  'PinganCurtainWallRoutineCheck',
];
const quickShotDocumentFlowStepLabels = [
  '创建记录',
  '待审核',
  '已审核',
];
const quickShotDocumentFlowRejectedStepLabels = [
  '创建记录',
  '待审核',
  '已驳回',
];
const quickShotDocumentFlowDescriptions: Record<string, string> = {
  创建记录: '员工提交随手拍隐患信息',
  待审核: '安全员确认部门、班组和隐患描述，并判断隐患是否成立',
  已审核: '安全员确认隐患存在并补充审核说明',
  已驳回: '安全员确认隐患不成立并记录驳回原因',
};
const quickShotWorkflowFieldGroups: Record<
  PinganThreeCheckRecordApi.QuickShotWorkflowAction,
  Array<{ dataIndex: string; label: string; required?: boolean }>
> = {
  APPROVE: [
    { dataIndex: 'hazardReviewConclusion', label: '隐患判断说明' },
    { dataIndex: 'reviewRemark', label: '审核说明' },
  ],
  REJECT: [
    { dataIndex: 'rejectReason', label: '驳回原因', required: true },
    { dataIndex: 'reviewRemark', label: '审核说明' },
  ],
};
const quickShotWorkflowDepartmentFields = ['rectificationDepartment', 'acceptanceDepartment'];
const quickShotWorkflowDepartmentOptions = computed(() => {
  const record = quickShotWorkflowActionRecord.value;
  const companyId =
    record?.companyId ??
    currentThreeCheckDetail.value?.companyId ??
    fixedCompanyOption.value?.value;
  return getCascadedOrganizationOptions(
    formOrganizationSource.value,
    isOrganizationId(companyId) ? companyId : undefined,
    ['DEPARTMENT'],
  ).map((item) => ({
    label: item.label,
    value: item.label,
  }));
});
const quickShotDocumentFlowFieldGroups: Record<
  string,
  Array<{
    dataIndex: string;
    kind?: DocumentFlowField['kind'];
    label: string;
  }>
> = {
  ACCEPT: [
    { dataIndex: 'acceptedBy', label: '验收人' },
    { dataIndex: 'acceptanceDepartment', label: '验收部门' },
    { dataIndex: 'acceptedAt', label: '验收时间' },
    { dataIndex: 'acceptanceResult', label: '验收结果' },
    { dataIndex: 'acceptanceRemark', label: '验收说明' },
  ],
  APPROVE: [
    { dataIndex: 'reviewedBy', label: '审核安全员' },
    { dataIndex: 'reviewedAt', label: '审核时间' },
    { dataIndex: 'hazardReviewConclusion', label: '隐患判断说明' },
    { dataIndex: 'reviewRemark', label: '审核说明' },
  ],
  CREATE: [
    { dataIndex: 'reporter', label: '上报人' },
    { dataIndex: 'company', label: '公司' },
    { dataIndex: 'department', label: '部门' },
    { dataIndex: 'team', label: '班组' },
    { dataIndex: 'hazardImage', kind: 'IMAGE', label: '隐患图片/视频' },
    { dataIndex: 'hazardDescription', label: '隐患描述' },
    { dataIndex: 'uploadTime', label: '上传时间' },
    { dataIndex: 'aiEnabled', label: '是否启用AI' },
  ],
  ISSUE_RECTIFICATION: [
    { dataIndex: 'rectificationResponsiblePerson', label: '整改责任人' },
    { dataIndex: 'rectificationDepartment', label: '整改部门' },
    { dataIndex: 'rectificationRequirement', label: '整改要求' },
    { dataIndex: 'rectificationDeadline', label: '整改截止时间' },
    { dataIndex: 'rectificationIssuedBy', label: '下发人' },
    { dataIndex: 'rectificationIssuedAt', label: '下发时间' },
  ],
  MARK_RECTIFIED: [
    { dataIndex: 'rectifiedBy', label: '整改完成人' },
    { dataIndex: 'rectifiedAt', label: '整改完成时间' },
    { dataIndex: 'afterRectificationPhoto', kind: 'IMAGE', label: '整改后照片' },
    { dataIndex: 'rectificationDescription', label: '整改说明' },
  ],
  REJECT: [
    { dataIndex: 'rejectedBy', label: '驳回人' },
    { dataIndex: 'rejectedAt', label: '驳回时间' },
    { dataIndex: 'rejectReason', label: '驳回原因' },
    { dataIndex: 'reviewRemark', label: '审核说明' },
  ],
  REQUEST_ACCEPTANCE: [
    { dataIndex: 'acceptanceRequestedBy', label: '提交验收人' },
    { dataIndex: 'acceptanceRequestedAt', label: '提交验收时间' },
    { dataIndex: 'acceptanceRequestRemark', label: '提交验收说明' },
    { dataIndex: 'acceptancePerson', label: '验收人' },
    { dataIndex: 'acceptanceDepartment', label: '验收部门' },
  ],
};
const isQuickShotModule = computed(
  () => moduleRuntime.value.routeName === 'PinganQuickShot',
);
const showBottomDetailAttachmentRow = computed(
  () =>
    bottomDetailAttachmentRouteNames.includes(moduleRuntime.value.routeName) &&
    localDetailColumns.value.some((column) => isDetailAttachmentColumn(column)),
);
const isTeamCheckInspectionDetail = computed(() =>
  isTeamCheckInspectionRoute(moduleRuntime.value.routeName),
);
const teamCheckInspectionColumns = computed(() =>
  getTeamCheckInspectionColumns(moduleRuntime.value.routeName),
);
const teamCheckInspectionTableTitle = computed(() => {
  const titleMap: Record<string, string> = {
    PinganMidShiftInspection: '班中检查项',
    PinganPostShiftInspection: '班后检查项',
    PinganPreShiftInspection: '班前检查项',
  };
  return titleMap[moduleRuntime.value.routeName] ?? '检查项';
});
const showTeamCheckInspectionTable = computed(
  () => isTeamCheckInspectionDetail.value,
);
const hazardDetailRouteNames = [
  'PinganSafetyCheck',
  ...wideDetailRouteNames,
];
const detailModalCentered = computed(() =>
  hazardDetailRouteNames.includes(moduleRuntime.value.routeName) ||
  isTeamCheckInspectionDetail.value,
);
const detailModalWidth = computed(() => {
  if (isTeamCheckInspectionDetail.value) {
    return 'min(1560px, calc(100vw - 24px))';
  }
  if (moduleRuntime.value.routeName === 'PinganSafetyCheck') {
    return 'min(1480px, calc(100vw - 32px))';
  }
  if (wideDetailRouteNames.includes(moduleRuntime.value.routeName)) {
    return 'min(1280px, calc(100vw - 32px))';
  }
  if (isPointsFlowRoute(moduleRuntime.value.routeName)) {
    return 'min(1080px, calc(100vw - 32px))';
  }
  return 760;
});
const detailModalWrapClass = computed(() => {
  if (moduleRuntime.value.routeName === 'PinganSafetyCheck') {
    return 'pingan-detail-modal pingan-detail-modal--safety-check';
  }
  if (isTeamCheckInspectionDetail.value) {
    return 'pingan-detail-modal pingan-detail-modal--team-check';
  }
  if (wideDetailRouteNames.includes(moduleRuntime.value.routeName)) {
    return 'pingan-detail-modal pingan-detail-modal--wide';
  }
  return 'pingan-detail-modal';
});
const isSafetyCheckDetail = computed(
  () => moduleRuntime.value.routeName === 'PinganSafetyCheck',
);
const isHazardInspectionDetail = computed(() =>
  isHazardInspectionRoute(moduleRuntime.value.routeName),
);
const moduleApiReady = computed(() => moduleRuntime.value.apiReady);
const isPreShiftMeetingModule = computed(
  () => moduleRuntime.value.apiMode === 'PRE_SHIFT_MEETING',
);
const usesPreShiftMeetingForm = computed(
  () => moduleRuntime.value.routeName === 'PreShiftMeeting',
);
const isThreeCheckRecordModule = computed(
  () => moduleRuntime.value.apiMode === 'THREE_CHECK_RECORD',
);
const isPointsFlowModule = computed(() =>
  isPointsFlowRoute(moduleRuntime.value.routeName),
);
const specialFilterFields = computed(() =>
  getThreeCheckSpecialFilterFields(moduleRuntime.value.routeName),
);
const showTeamFilter = computed(() => specialFilterFields.value.team);
const showStatusFilter = computed(() => specialFilterFields.value.status);
const showPointsReasonFilter = computed(
  () => specialFilterFields.value.pointsReason,
);
const pointsReasonFilterOptions = computed(() =>
  getThreeCheckFieldOptions(moduleRuntime.value.routeName, 'pointsReason'),
);
const departmentFilterLabel = computed(() =>
  isPointsFlowModule.value ? '部门' : '车间',
);
const operationPolicy = computed(() =>
  getThreeCheckOperationPolicy(moduleRuntime.value.routeName),
);
const threeCheckModuleKey = computed(() => moduleRuntime.value.moduleKey ?? '');
const canUseWorkbench = computed(
  () => moduleRuntime.value.createMode !== 'DISABLED',
);
const canCreateRecord = computed(
  () =>
    canOpenThreeCheckCreateDialog(
      moduleRuntime.value.routeName,
      moduleRuntime.value.createMode,
      accessStore.accessCodes ?? [],
    ),
);
const canUploadAttachment = computed(
  () =>
    moduleApiReady.value &&
    canUploadThreeCheckAttachment(
      moduleRuntime.value.routeName,
      accessStore.accessCodes ?? [],
    ),
);
const canDeleteRecord = computed(() =>
  canDeleteThreeCheckRecord(
    moduleRuntime.value.routeName,
    accessStore.accessCodes ?? [],
  ),
);
const canExportRecords = computed(() =>
  canExportThreeCheckRecords(
    moduleRuntime.value.routeName,
    accessStore.accessCodes ?? [],
  ),
);
const canReviewQuickShotWorkflow = computed(() =>
  canReviewQuickShotRecord(accessStore.accessCodes ?? []),
);
const canSubmitRecord = computed(() =>
  canSubmitThreeCheckRecord(
    moduleRuntime.value.routeName,
    accessStore.accessCodes ?? [],
  ),
);
const canWithdrawRecord = computed(() =>
  canWithdrawThreeCheckRecord(
    accessStore.accessCodes ?? [],
    moduleRuntime.value.routeName,
  ),
);
const canRemindRecord = computed(() =>
  canRemindThreeCheckRecord(
    accessStore.accessCodes ?? [],
    moduleRuntime.value.routeName,
  ),
);
const canCreateRectificationOrder = computed(() =>
  (accessStore.accessCodes ?? []).includes(
    'PINGAN_ONE_SHIFT_THREE_CHECKS_RECTIFICATION_ORDER_CREATE',
  ),
);
const isOneShiftInspectionModule = computed(() =>
  ['mid-shift-inspection', 'post-shift-inspection', 'pre-shift-inspection'].includes(
    threeCheckModuleKey.value,
  ),
);
const hasSelectedRows = computed(() => selectedRowKeys.value.length > 0);
const canBatchDelete = computed(
  () =>
    operationPolicy.value.batchDelete &&
    hasSelectedRows.value &&
    canDeleteRecord.value,
);
const tableRowSelection = computed<TableProps<ThreeCheckTableRow>['rowSelection']>(
  () =>
    operationPolicy.value.batchDelete
      ? {
          getCheckboxProps: () => ({
            disabled: !isThreeCheckRecordModule.value,
          }),
          onChange: (keys) => {
            selectedRowKeys.value = keys.map(String);
          },
          preserveSelectedRowKeys: true,
          selectedRowKeys: selectedRowKeys.value,
        }
      : undefined,
);
const canEditCurrentThreeCheckDetail = computed(() =>
  operationPolicy.value.edit &&
  isThreeCheckDetailEditable(currentThreeCheckDetail.value),
);
const hazardInspectionRequiredFieldKeys: Record<string, string[]> = {
  PinganSafetyCheck: [
    'inspectionUnit',
    'createdBy',
    'inspectedUnit',
    'inspectionMethod',
    'inspectionContent',
    'inspectionType',
    'inspectors',
    'inspectionDate',
    'acceptancePersonnel',
    'inspectionTime',
    'inspectedUnitPersonnel',
  ],
};
const safetyCheckDetailLineColumns: HazardInspectionDetailColumn[] = [
  { dataIndex: 'sequence', title: '序号', width: 64 },
  { dataIndex: 'aiEnabled', title: '是否启用AI', width: 110 },
  { dataIndex: 'hazardLibrary', title: '隐患库选择', width: 160 },
  { dataIndex: 'checkResult', title: '检查结果', width: 120 },
  { dataIndex: 'beforeRectificationPhoto', title: '整改前照片', width: 120 },
  { dataIndex: 'hazardDescription', title: '隐患描述', width: 190 },
  { dataIndex: 'rectificationMeasures', title: '整改建议', width: 190 },
  { dataIndex: 'rectificationOrderNo', title: '关联整改工单', width: 160 },
  { dataIndex: 'rectificationStatusLabel', title: '工单状态', width: 130 },
  { dataIndex: 'rectificationClosedAt', title: '闭环时间', width: 160 },
];
const hazardInspectionLineColumns = computed(() => safetyCheckDetailLineColumns);
const hazardInspectionDefaultVisibleColumnKeys = computed(() =>
  hazardInspectionLineColumns.value.map((column) => column.dataIndex),
);
const hazardInspectionVisibleColumnOptions = computed(() =>
  hazardInspectionLineColumns.value.map((column) => ({
    label: column.title,
    value: column.dataIndex,
  })),
);
const visibleHazardInspectionLineColumns = computed(() => {
  const visibleKeys = hazardInspectionVisibleColumnKeys.value.length
    ? hazardInspectionVisibleColumnKeys.value
    : hazardInspectionDefaultVisibleColumnKeys.value;
  const visibleKeySet = new Set(visibleKeys);
  return hazardInspectionLineColumns.value.filter((column) =>
    visibleKeySet.has(column.dataIndex),
  );
});
const filteredHazardInspectionLines = computed(() =>
  filterHazardInspectionLines(
    hazardInspectionLines.value,
    hazardInspectionLineKeyword.value,
  ),
);
const hazardInspectionLineValidationLabel = computed(() => {
  const lines = filteredHazardInspectionLines.value.length
    ? filteredHazardInspectionLines.value
    : hazardInspectionLines.value;
  return lines.length && lines.every((line) => line.validated)
    ? '已校验'
    : '待校验';
});
const hazardInspectionLineRecordCount = computed(
  () => `${filteredHazardInspectionLines.value.length} 条记录`,
);
const detailDocumentFlowItems = computed(() => {
  const detail = currentThreeCheckDetail.value;
  if (!detail) {
    return [];
  }
  return [
    {
      label: '创建记录',
      meta: detail.businessDate || detail.date || '未记录日期',
      value: `${detail.owner || '系统'} 创建 ${detail.recordNo}`,
    },
    {
      label: '当前状态',
      meta: detail.sourceChannel || 'PC',
      value: detail.statusLabel || getStatusLabel(detail.status),
    },
    {
      label: '处理提醒',
      meta: `催办 ${detail.reminderCount || 0} 次`,
      value: detail.canSubmit ? '待提交处理' : '流程已进入当前节点',
    },
  ];
});
const currentDocumentFlowDetail = computed(
  () => currentDocumentFlow.value?.record ?? currentThreeCheckDetail.value,
);
const currentDocumentFlowStatusLogs = computed(
  () => currentDocumentFlow.value?.statusLogs ?? [],
);
const documentFlowNodes = computed<DocumentFlowNode[]>(() => {
  const detail = currentDocumentFlowDetail.value;
  if (!detail) {
    return [];
  }
  const statusLabel = detail.statusLabel || getStatusLabel(detail.status);
  const owner = documentFlowOwner();
  const date = detail.businessDate || detail.date || '未记录日期';
  const masterNode: DocumentFlowNode = {
    date,
    fields: [
      { label: '单据编号', value: detail.recordNo },
      { label: '模块', value: moduleRuntime.value.title },
      { label: '公司', value: detail.company || '未填写' },
      { label: '部门', value: detail.department || '未填写' },
      { label: '班组', value: detail.team || '未填写' },
      { label: '来源', value: detail.sourceChannel || 'PC' },
      { kind: 'STATUS', label: '状态', value: statusLabel },
    ],
    id: 'master',
    owner,
    status: statusLabel,
    subtitle: `${moduleRuntime.value.title} ${detail.recordNo}`,
    title: `${moduleRuntime.value.title}主单`,
    type: 'master',
  };
  if (isQuickShotModule.value) {
    return [masterNode, ...quickShotDocumentFlowNodes()];
  }
  return [masterNode, documentFlowDetailNode()];
});
const filteredDocumentFlowNodes = computed(() => {
  const keyword = documentFlowKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return documentFlowNodes.value;
  }
  return documentFlowNodes.value.filter((node) =>
    [
      node.title,
      node.subtitle,
      node.owner,
      node.status,
      node.date,
      ...node.fields.flatMap((field) => [field.label, field.value]),
    ]
      .join(' ')
      .toLowerCase()
      .includes(keyword),
  );
});
const documentFlowTreeMasterNode = computed(
  () =>
    filteredDocumentFlowNodes.value.find((node) => node.type === 'master') ??
    documentFlowNodes.value[0],
);
const documentFlowTreeDetailNodes = computed(() =>
  filteredDocumentFlowNodes.value.filter((node) => node.type === 'detail'),
);
const documentFlowListNodes = computed(() => documentFlowTreeDetailNodes.value);
const documentFlowSelectedNode = computed(() => {
  const nodes = filteredDocumentFlowNodes.value.length
    ? filteredDocumentFlowNodes.value
    : documentFlowNodes.value;
  return (
    nodes.find((node) => node.id === documentFlowSelectedNodeId.value) ??
    nodes[0]
  );
});
const documentFlowSelectedNodePosition = computed(() => {
  const selected = documentFlowSelectedNode.value;
  if (!selected || selected.type !== 'detail') {
    return '主单';
  }
  const index = documentFlowListNodes.value.findIndex(
    (node) => node.id === selected.id,
  );
  return `第 ${index >= 0 ? index + 1 : 1} 条 / 共 ${
    documentFlowListNodes.value.length || 1
  } 条`;
});
const documentFlowSummary = computed(() => {
  const detail = currentDocumentFlowDetail.value;
  const status = detail ? detail.statusLabel || getStatusLabel(detail.status) : '';
  return [
    {
      icon: 'lucide:shield-check',
      label: '当前状态',
      tone: 'orange',
      value: status || '未记录',
    },
    {
      icon: 'lucide:map-pinned',
      label: '当前节点',
      tone: 'blue',
      value: documentFlowCurrentStepLabel(status),
    },
    {
      icon: 'lucide:user-round',
      label: '责任人',
      tone: 'default',
      value: documentFlowOwner(),
    },
    {
      icon: 'lucide:list',
      label: '明细数量',
      tone: 'default',
      value: String(Math.max(documentFlowNodes.value.length - 1, 0)),
    },
    {
      icon: 'lucide:paperclip',
      label: '附件',
      tone: 'default',
      value: String(currentDocumentFlowDetail.value?.attachments.length ?? 0),
    },
  ];
});
const documentFlowSteps = computed<DocumentFlowStep[]>(() => {
  const statusLogs = currentDocumentFlowStatusLogs.value;
  if (statusLogs.length) {
    return statusLogs.map((log, index) => ({
      date: formatChangeHistoryTime(log.createdAt),
      description:
        log.remark ||
        quickShotDocumentFlowDescriptions[log.toStatusLabel || ''] ||
        quickShotDocumentFlowDescriptions[documentFlowActionLabel(log.action)] ||
        '',
      label: documentFlowActionLabel(log.action),
      nodeId: documentFlowStepNodeId(log, index),
      owner: log.operatorName || log.operatorId || '-',
      state: index === statusLogs.length - 1 ? 'current' : 'done',
    }));
  }
  const detail = currentDocumentFlowDetail.value;
  const status = detail ? detail.statusLabel || getStatusLabel(detail.status) : '';
  const currentIndex = documentFlowCurrentStepIndex(status);
  const date = detail?.businessDate || detail?.date || '-';
  const owner = documentFlowOwner();
  const stepLabels = isQuickShotModule.value
    ? status === '已驳回'
      ? quickShotDocumentFlowRejectedStepLabels
      : quickShotDocumentFlowStepLabels
    : ['创建记录', '提交检查', '整改处理中', '待验收', '已完成'];
  return stepLabels.map((label, index) => ({
    date: index <= currentIndex ? date : '-',
    description: quickShotDocumentFlowDescriptions[label] || '',
    label,
    nodeId: index === currentIndex ? documentFlowListNodes.value[0]?.id : undefined,
    owner: index <= currentIndex ? owner : '-',
      state:
        index < currentIndex
          ? 'done'
          : index === currentIndex
            ? 'current'
            : 'pending',
    }));
});
const documentFlowSubtitle = computed(() => {
  return `${moduleRuntime.value.title} · 全链路洞察`;
});
const documentFlowAttachments = computed(
  () => currentDocumentFlowDetail.value?.attachments ?? [],
);
const detailPreviewAttachments = computed(() => {
  if (currentThreeCheckDetail.value) {
    return currentThreeCheckDetail.value.attachments;
  }
  if (currentDetail.value) {
    return currentDetail.value.attachments;
  }
  return [];
});

function attachmentFileKindLabel(fileKind?: string) {
  const normalizedKind = String(fileKind ?? '').toUpperCase();
  if (normalizedKind === 'IMAGE') {
    return '图片';
  }
  if (normalizedKind === 'VIDEO') {
    return '视频';
  }
  if (normalizedKind === 'PDF') {
    return 'PDF';
  }
  return '文件';
}

const detailChangeHistoryItems = computed(() => {
  const detail = currentThreeCheckDetail.value;
  if (!detail) {
    return [];
  }
  return [
    { label: '版本号', value: detail.version },
    { label: '客户端更新时间', value: detail.clientUpdatedAt },
    { label: '最后同步时间', value: detail.lastSyncedAt },
    { label: '来源记录ID', value: detail.sourceRecordId },
    { label: '客户端请求ID', value: detail.clientRequestId },
  ].filter(
    (item) =>
      item.value !== undefined && item.value !== null && item.value !== '',
  );
});
const changeHistoryItems = computed(() => currentChangeHistory.value?.items ?? []);
const changeHistoryFieldLabelMap: Record<string, string> = {
  acceptanceDate: '验收日期',
  acceptancePerson: '验收人员',
  acceptanceResult: '整改验收是否通过',
  aiEnabled: '是否启用AI',
  attachment: '附件',
  attachments: '附件',
  businessDate: '检查日期',
  checkPerson: '检查人员',
  checkUnit: '检查单位',
  createdBy: '创建人',
  detailCount: '明细计数',
  hazardDescription: '隐患描述',
  hazardLibrary: '隐患库选择',
  inspectedUnit: '受检单位',
  inspectedUnitPerson: '受检单位人员',
  inspectedUnitPersonManual: '受检单位人员（手录）',
  inspectionContent: '检查内容',
  inspectionDate: '检查日期',
  inspectionMethod: '检查方式',
  inspectionPersonnel: '检查人员',
  inspectionPersonnelManual: '检查人员（手录）',
  inspectionTime: '检查时间',
  inspectionType: '检查类型',
  inspectionTypeManual: '检查类型（手录）',
  inspectionUnit: '检查单位',
  manualInspectionType: '检查类型（手录）',
  owner: '负责人',
  rectificationDeadline: '整改截止日期',
  rectificationMeasures: '整改措施',
  rectificationResponsiblePerson: '整改责任人',
  reporter: '上报人',
  responsiblePerson: '整改责任人',
  status: '状态',
  statusLabel: '状态',
  videoUpload: '视频上传',
};
const filteredChangeHistoryItems = computed(() => {
  const keyword = changeHistoryKeyword.value.trim().toLowerCase();
  return changeHistoryItems.value.filter((item) => {
    const filter = changeHistoryFilter.value;
    const matchesFilter =
      filter === 'ALL' ||
      (filter === 'ATTACHMENT'
        ? item.action === 'ATTACHMENT' || ['IMAGE', 'VIDEO'].includes(item.valueType || '')
        : filter === 'STATUS'
          ? item.fieldKey === 'status' || item.valueType === 'STATUS'
          : filter === 'FLOW'
            ? ['CREATE', 'SUBMIT', 'WITHDRAW', 'REMIND'].includes(item.action)
            : item.action === 'UPDATE');
    if (!matchesFilter) {
      return false;
    }
    if (!keyword) {
      return true;
    }
    return [
      item.action,
      item.fieldKey,
      item.fieldLabel,
      item.beforeValue,
      item.afterValue,
      item.operatorName,
      item.remark,
    ]
      .join(' ')
      .toLowerCase()
      .includes(keyword);
  });
});
const changeHistoryGroups = computed<ChangeHistoryGroup[]>(() => {
  const groupMap = new Map<string, PinganThreeCheckRecordApi.ChangeHistoryItem[]>();
  for (const item of filteredChangeHistoryItems.value) {
    const key = `${item.version}-${item.action}-${item.createdAt}`;
    groupMap.set(key, [...(groupMap.get(key) ?? []), item]);
  }
  const groups = [...groupMap.entries()].map(([id, items]) => {
    const attachmentCount = items.filter(
      (item) =>
        item.action === 'ATTACHMENT' ||
        ['IMAGE', 'VIDEO'].includes(item.valueType || ''),
    ).length;
    const flowCount = items.filter((item) =>
      ['CREATE', 'SUBMIT', 'WITHDRAW', 'REMIND'].includes(item.action),
    ).length;
    return {
      action: items[0]?.action ?? 'UPDATE',
      attachmentCount,
      count: items.length,
      displayVersion: 1,
      fieldCount: Math.max(items.length - attachmentCount - flowCount, 0),
      flowCount,
      id,
      items,
      operator: items[0]?.operatorName || items[0]?.operatorId || '系统',
      time: formatChangeHistoryTime(items[0]?.createdAt),
      version: items[0]?.version ?? 0,
    };
  });
  const sortedVersions = [...new Set(groups.map((group) => group.version))]
    .sort((left, right) => left - right);
  const displayVersionMap = new Map(
    sortedVersions.map((version, index) => [version, index + 1]),
  );
  return groups.map((group) => ({
    ...group,
    displayVersion: displayVersionMap.get(group.version) ?? group.version + 1,
  }));
});
const changeHistorySelectedGroup = computed(
  () =>
    changeHistoryGroups.value.find(
      (group) => group.id === changeHistorySelectedGroupId.value,
    ) ?? changeHistoryGroups.value[0],
);
const selectedChangeHistoryItems = computed(
  () => changeHistorySelectedGroup.value?.items ?? [],
);
const changeHistorySummary = computed(() => {
  const items = changeHistoryItems.value;
  const latest = items[0];
  return [
    {
      icon: 'lucide:history',
      label: '变更次数',
      tone: 'blue',
      value: String(items.length),
    },
    {
      icon: 'lucide:user-round',
      label: '最近操作人',
      tone: 'default',
      value: latest?.operatorName || latest?.operatorId || '未记录',
    },
    {
      icon: 'lucide:clock-3',
      label: '最近变更',
      tone: 'default',
      value: formatChangeHistoryTime(latest?.createdAt),
    },
    {
      icon: 'lucide:paperclip',
      label: '附件变更',
      tone: 'orange',
      value: String(
        items.filter(
          (item) =>
            item.action === 'ATTACHMENT' ||
            ['IMAGE', 'VIDEO'].includes(item.valueType || ''),
        ).length,
      ),
    },
  ];
});
const changeHistorySubtitle = computed(() => {
  return `${moduleRuntime.value.title} · 字段级追溯`;
});
const changeHistoryAttachmentItems = computed(() =>
  filteredChangeHistoryItems.value.filter(
    (item) =>
      item.action === 'ATTACHMENT' || ['IMAGE', 'VIDEO'].includes(item.valueType || ''),
  ),
);
const emptyDescription = computed(() =>
  moduleRuntime.value.createMode === 'DISABLED'
    ? `${workbenchText.value.title}待接入`
    : workbenchText.value.emptyText,
);
const tableDataSourceText = computed(() =>
  isPreShiftMeetingModule.value
    ? '数据来自班前会接口'
    : isThreeCheckRecordModule.value
      ? '数据来自一班三查通用接口'
      : moduleRuntime.value.createMode === 'LOCAL_TABLE'
      ? '当前模块支持表头模板新增'
      : '当前模块待接入',
);
const activeStatusOptions = computed(() => {
  const options = getThreeCheckStatusFilterOptions(moduleRuntime.value.routeName);
  if (options.length > 1) {
    return options as Array<{ label: string; value: WorkbenchStatusFilter }>;
  }
  if (isPreShiftMeetingModule.value) {
    return statusOptions;
  }
  return [{ label: '全部', value: 'all' as WorkbenchStatusFilter }];
});

function activeMeetingFilters(): MeetingFilters {
  return {
    company: filters.company,
    dateRange:
      filters.dateStart || filters.dateEnd
        ? [filters.dateStart, filters.dateEnd]
        : undefined,
    department: filters.department,
    organizationKey: showDataMap.value
      ? selectedOrganizationKeys.value[0]
      : undefined,
    status: filters.status,
    team: filters.team,
  };
}

function setWorkbenchStatusFilter(status: MeetingFilters['status']) {
  filters.status = status ?? 'all';
}

function loadLocalTemplateRows() {
  const templateRows = getThreeCheckTemplateRows(moduleRuntime.value.routeName);
  const filteredRows = filterMeetingRows(templateRows, activeMeetingFilters());
  rows.value = filteredRows;
  total.value = filteredRows.length;
  loading.value = false;
}

function normalizeOrgNode(
  node: PinganPreShiftMeetingApi.OrgNode,
): OrganizationNode {
  return {
    children: node.children?.map(normalizeOrgNode),
    companyType: node.companyType,
    id: node.id,
    key: node.key,
    orgType: node.orgType,
    title: node.title,
  };
}

function normalizeMeetingRow(
  row: PinganPreShiftMeetingApi.MeetingRow,
): ThreeCheckTableRow {
  return {
    attendees: row.attendees,
    canRemind: row.canRemind,
    canSubmit: row.canSubmit,
    canWithdraw: row.canWithdraw,
    company: row.company,
    date: row.date,
    department: row.department,
    id: row.id,
    imageCheck: row.imageCheck,
    imagePreviewUrl: resolveAttachmentPreviewUrl(row.imagePreviewUrl),
    organizationPath: [],
    owner: row.owner,
    overdue: row.overdue,
    sourceChannel: row.sourceChannel,
    status: row.status,
    team: row.team,
    version: row.version,
    videoCheck: row.videoCheck,
    videoPreviewUrl: resolveAttachmentPreviewUrl(row.videoPreviewUrl),
  };
}

function normalizeThreeCheckRecordRow(
  row: PinganThreeCheckRecordApi.RecordRow,
): ThreeCheckTableRow {
  const payload = (row.payload ?? {}) as Record<string, any>;

  return {
    ...payload,
    attendees: formatThreeCheckPayloadDisplayValue(
      payload.attendeesText || payload.attendees,
    ),
    canRemind: row.canRemind,
    canSubmit: row.canSubmit,
    canWithdraw: row.canWithdraw,
    company: row.company,
    date: row.date || row.businessDate,
    department: row.department,
    id: row.id,
    imageCheck: row.imageCheck,
    imagePreviewUrl: resolveAttachmentPreviewUrl(row.imagePreviewUrl),
    moduleKey: row.moduleKey,
    organizationPath: [],
    owner: row.owner || payload.owner || payload.responsiblePerson || '',
    overdue: row.overdue,
    payload,
    recordNo: row.recordNo,
    serialNo: row.recordNo,
    sourceChannel: row.sourceChannel,
    status: row.status,
    statusLabel: row.statusLabel ?? payload.statusLabel,
    team: row.team,
    version: row.version,
    videoCheck: row.videoCheck,
    videoPreviewUrl: resolveAttachmentPreviewUrl(row.videoPreviewUrl),
  };
}

async function loadOrganizations() {
  try {
    const [apiCompanyTree, apiFullTree] = await Promise.all([
      getPinganCompanyOrgTreeApi(),
      getPinganOrgTreeApi(),
    ]);
    const companyTree = apiCompanyTree.map(normalizeOrgNode);
    const fullTree = apiFullTree.map(normalizeOrgNode);
    dataMapOrganizationNodes.value = companyTree;
    expandedOrganizationKeys.value = collectExpandableOrganizationKeys(companyTree);
    formOrganizationNodes.value = fullTree;
    enforceUserOrganizationScope();
  } catch {
    formOrganizationNodes.value = [];
  }
}

async function loadUsers() {
  try {
    userOptions.value = await getPinganUsersApi();
  } catch {
    if (!userOptions.value.length) {
      userOptions.value = [
        { id: 1, orgId: 1, realName: '系统管理员', username: 'admin' },
        { id: 2, orgId: 4, realName: 'Demo Harbor班长', username: 'HB_MONITOR' },
        { id: 3, orgId: 4, realName: '幕墙安全员', username: 'MQ_SAFE' },
        { id: 4, orgId: 8, realName: '制氧主管', username: 'ZY_SUPERVISOR' },
      ];
    }
  }
}

async function loadMeetings() {
  applyFixedCompanyDefaults();
  enforceUserOrganizationScope();
  if (moduleRuntime.value.createMode === 'LOCAL_TABLE') {
    loadLocalTemplateRows();
    return;
  }

  if (!moduleApiReady.value) {
    rows.value = [];
    total.value = 0;
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    const overdue = filters.status === 'overdue';
    const requestStatus = (overdue ? 'all' : filters.status) as Exclude<
      MeetingFilters['status'],
      'overdue'
    >;
    const params = {
      ...createOrganizationFilterParams(filters),
      dateEnd: filters.dateEnd || undefined,
      dateStart: filters.dateStart || undefined,
      organizationId: showDataMap.value && selectedOrganizationKeys.value[0]
        ? getOrganizationIdByKey(
            dataMapOrganizationNodes.value,
            selectedOrganizationKeys.value[0],
          )
        : undefined,
      page: 1,
      pageSize: 20,
      overdue: overdue || undefined,
      pointsReason: showPointsReasonFilter.value
        ? filters.pointsReason || undefined
        : undefined,
      status: requestStatus,
    };
    const result = isThreeCheckRecordModule.value
      ? await getThreeCheckRecordsApi(threeCheckModuleKey.value, params)
      : await getPreShiftMeetingsApi(params);
    rows.value = result.items.map((row) =>
      isThreeCheckRecordModule.value
        ? normalizeThreeCheckRecordRow(
            row as PinganThreeCheckRecordApi.RecordRow,
          )
        : normalizeMeetingRow(row as PinganPreShiftMeetingApi.MeetingRow),
    );
    total.value = result.total;
  } catch {
    message.error(workbenchText.value.dataLoadErrorText);
    rows.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

async function refreshListForExternalChanges() {
  if (document.hidden || loading.value || createSaving.value || detailSaving.value) {
    return;
  }
  await loadMeetings();
}

function stopListAutoRefresh() {
  if (listAutoRefreshTimer === undefined) {
    return;
  }
  window.clearInterval(listAutoRefreshTimer);
  listAutoRefreshTimer = undefined;
}

function startListAutoRefresh() {
  if (listAutoRefreshTimer !== undefined) {
    return;
  }
  listAutoRefreshTimer = window.setInterval(() => {
    void refreshListForExternalChanges();
  }, THREE_CHECK_LIST_AUTO_REFRESH_INTERVAL_MS);
}

function handleDocumentVisibilityChange() {
  if (document.hidden) {
    stopListAutoRefresh();
    return;
  }
  startListAutoRefresh();
  void refreshListForExternalChanges();
}

function resetFilters() {
  filters.company = '';
  filters.companyId = undefined;
  filters.dateEnd = '';
  filters.dateStart = '';
  filters.department = '';
  filters.departmentId = undefined;
  filters.pointsReason = undefined;
  filters.status = 'all';
  filters.team = '';
  filters.teamId = undefined;
  selectedOrganizationKeys.value = [];
  clearSelectedRows();
  applyFixedCompanyDefaults();
  enforceUserOrganizationScope();
  void loadMeetings();
}

function getStatusLabel(status: MeetingRow['status']) {
  return statusMeta[status]?.label ?? status;
}

function getRecordStatusLabel(record: Record<string, any>) {
  if (record.overdue === true) {
    return '已逾期';
  }
  if (typeof record.statusLabel === 'string') {
    return record.statusLabel;
  }
  return getStatusLabel(record.status);
}

function statusBoxClass(label?: string) {
  return [
    'status-box',
    `status-box--${getThreeCheckStatusBadgeTone(label)}`,
  ];
}

function statusBoxLabel(label?: string) {
  return normalizeThreeCheckStatusBoxLabel(label);
}

function isStatusBoxColumn(dataIndex: unknown) {
  return isThreeCheckStatusBoxField(dataIndex);
}

function columnDataIndexFieldName(dataIndex: unknown) {
  if (dataIndex && typeof dataIndex === 'object' && 'dataIndex' in dataIndex) {
    return columnDataIndexFieldName(
      (dataIndex as { dataIndex?: unknown }).dataIndex,
    );
  }
  if (Array.isArray(dataIndex)) {
    return String(dataIndex.at(-1) ?? '');
  }
  return typeof dataIndex === 'string' ? dataIndex : '';
}

function columnStatusBoxLabel(record: Record<string, any>, dataIndex: unknown) {
  const fieldName = columnDataIndexFieldName(dataIndex);
  if (fieldName === 'status') {
    return statusBoxLabel(getRecordStatusLabel(record));
  }
  if (fieldName) {
    return statusBoxLabel(String(record[fieldName] ?? ''));
  }
  return '';
}

function columnStatusBoxClass(record: Record<string, any>, dataIndex: unknown) {
  return statusBoxClass(columnStatusBoxLabel(record, dataIndex));
}

function isAttachmentColumn(column: unknown) {
  return isThreeCheckAttachmentColumn(column);
}

function attachmentKind(column: unknown) {
  return getThreeCheckAttachmentColumnKind(column);
}

function attachmentCellValue(record: Record<string, any>, column: unknown) {
  const fieldName = columnDataIndexFieldName(column);
  return fieldName ? String(record[fieldName] ?? '') : '';
}

function attachmentCellLabel(record: Record<string, any>, column: unknown) {
  return attachmentCellValue(record, column) || '未上传';
}

function isVideoAttachmentValue(value: string) {
  return /\.(?:avi|mkv|mov|mp4|webm)$/i.test(value) || value.includes('video/');
}

function isQuickShotHazardMediaColumn(column: unknown) {
  return (
    moduleRuntime.value.routeName === 'PinganQuickShot' &&
    columnDataIndexFieldName(column) === 'hazardImage'
  );
}

function attachmentRenderKind(record: Record<string, any>, column: unknown) {
  const kind = attachmentKind(column);
  if (!isQuickShotHazardMediaColumn(column)) {
    return kind;
  }
  const fieldValue = attachmentCellValue(record, column);
  if (record.videoPreviewUrl || isVideoAttachmentValue(fieldValue)) {
    return 'VIDEO';
  }
  return kind;
}

function attachmentPreviewFor(record: Record<string, any>, column: unknown) {
  const fieldName = columnDataIndexFieldName(column);
  const fieldValue = attachmentCellValue(record, column);
  if (isQuickShotHazardMediaColumn(column)) {
    if (
      fieldValue &&
      !isThreeCheckAttachmentMissing(fieldValue) &&
      shouldUseDirectAttachmentPreviewValue(fieldValue)
    ) {
      return resolveAttachmentPreviewUrl(fieldValue);
    }
    return resolveAttachmentPreviewUrl(
      record.videoPreviewUrl || record.imagePreviewUrl,
    );
  }
  if (
    fieldValue &&
    !isThreeCheckAttachmentMissing(fieldValue) &&
    shouldResolveAttachmentValue(fieldValue)
  ) {
    return resolveAttachmentPreviewUrl(fieldValue);
  }
  const kind = attachmentKind(column);
  if (!shouldUseThreeCheckListPreviewFallback(fieldName, kind)) {
    return '';
  }
  return resolveAttachmentPreviewUrl(
    kind === 'VIDEO' ? record.videoPreviewUrl : record.imagePreviewUrl,
  );
}

function attachmentPillClass(record: Record<string, any>, column: unknown) {
  return [
    'upload-pill',
    isThreeCheckAttachmentMissing(attachmentCellValue(record, column))
      ? 'upload-pill--missing'
      : 'upload-pill--done',
  ];
}

function detailAttachmentFieldKind(column: ThreeCheckTableColumn) {
  return getThreeCheckAttachmentColumnKind(column);
}

function isDetailAttachmentColumn(column: ThreeCheckTableColumn) {
  return Boolean(detailAttachmentFieldKind(column));
}

function detailAttachmentFieldAccept(column: ThreeCheckTableColumn) {
  return detailAttachmentFieldKind(column) === 'VIDEO' ? 'video/*' : 'image/*';
}

function detailAttachmentFieldUploadText(column: ThreeCheckTableColumn) {
  return detailAttachmentFieldKind(column) === 'VIDEO' ? '上传视频' : '上传图片';
}

function shouldResolveAttachmentValue(value: string) {
  return (
    /^https?:\/\//i.test(value) ||
    value.startsWith('/uploads/') ||
    /^[A-Z_]+\|/.test(value) ||
    /[\\/]/.test(value) ||
    /\.(?:avif|gif|jpe?g|mov|mp4|png|webm|webp)$/i.test(value)
  );
}

function shouldUseDirectAttachmentPreviewValue(value: string) {
  return (
    /^https?:\/\//i.test(value) ||
    value.startsWith('/uploads/') ||
    /^[A-Z_]+\|/.test(value) ||
    /[\\/]/.test(value)
  );
}

function detailAttachmentColumnsByKind(fileKind: 'IMAGE' | 'VIDEO') {
  return localDetailColumns.value.filter(
    (column) => detailAttachmentFieldKind(column) === fileKind,
  );
}

function detailAttachmentForField(column: ThreeCheckTableColumn) {
  const detail = currentThreeCheckDetail.value;
  const fileKind = detailAttachmentFieldKind(column);
  if (!detail || !fileKind) {
    return undefined;
  }
  const fields = detailAttachmentColumnsByKind(fileKind);
  const fieldIndex = Math.max(
    0,
    fields.findIndex((field) => field.dataIndex === column.dataIndex),
  );
  return detail.attachments.filter((attachment) => attachment.fileKind === fileKind)[
    fieldIndex
  ];
}

function detailAttachmentFieldValue(column: ThreeCheckTableColumn) {
  const value = threeCheckDetailValue(column);
  if (!isThreeCheckAttachmentMissing(value) && value !== '未填写') {
    return value;
  }
  return detailAttachmentForField(column)?.originalName ?? value;
}

function detailAttachmentFieldPreview(column: ThreeCheckTableColumn) {
  const value = detailAttachmentFieldValue(column);
  if (value && shouldResolveAttachmentValue(value)) {
    return resolveAttachmentPreviewUrl(value);
  }
  const attachment = detailAttachmentForField(column);
  return attachment ? attachmentPreviewUrl(attachment) : '';
}

function isOrganizationId(value: unknown): value is OrganizationId {
  return typeof value === 'number' || typeof value === 'string';
}

function firstOptionValue(options: Array<{ value: OrganizationId }>) {
  return options[0]?.value;
}

function optionLabel(
  options: Array<{ label: string; value: OrganizationId }>,
  value: OrganizationId | undefined,
) {
  return (
    options.find((item) => String(item.value) === String(value))?.label ?? ''
  );
}

function padDatePart(value: number) {
  return String(value).padStart(2, '0');
}

function formatLocalCreateDate(now = new Date()) {
  return [
    now.getFullYear(),
    padDatePart(now.getMonth() + 1),
    padDatePart(now.getDate()),
  ].join('-');
}

function formatLocalCreateDateTime(now = new Date()) {
  return `${formatLocalCreateDate(now)} ${padDatePart(now.getHours())}:${padDatePart(
    now.getMinutes(),
  )}`;
}

function isLocalDateColumn(column: ThreeCheckTableColumn) {
  return (
    column.dataIndex === 'date' ||
    column.dataIndex.endsWith('Date') ||
    column.title === '日期' ||
    column.title.endsWith('日期')
  );
}

function isLocalDateTimeColumn(column: ThreeCheckTableColumn) {
  return column.dataIndex === 'uploadTime' || column.title.endsWith('时间');
}

function fieldOptions(column: ThreeCheckTableColumn) {
  return getThreeCheckFieldOptions(
    moduleRuntime.value.routeName,
    column.dataIndex,
  );
}

function organizationFieldOptions(column: ThreeCheckTableColumn) {
  const orgTypes = getThreeCheckOrganizationSelectTypes(
    moduleRuntime.value.routeName,
    column.dataIndex,
  );
  if (!orgTypes.length) {
    return [];
  }
  return getCascadedOrganizationOptions(
    formOrganizationSource.value,
    undefined,
    orgTypes,
  ).map((item) => ({
    label: item.label,
    value: item.label,
  }));
}

function localFieldOptions(column: ThreeCheckTableColumn) {
  const organizationOptions = organizationFieldOptions(column);
  return organizationOptions.length ? organizationOptions : fieldOptions(column);
}

function isLocalSelectColumn(column: ThreeCheckTableColumn) {
  return localFieldOptions(column).length > 0;
}

function isEditableThreeCheckDetailColumn(column: ThreeCheckTableColumn) {
  return !['actions', 'company', 'department', 'team'].includes(
    column.dataIndex,
  );
}

function isLongTextDetailColumn(column: ThreeCheckTableColumn) {
  return [
    'hazardDescription',
    'inspectionContent',
    'inspectionType',
    'inspectionTypeManual',
    'remarks',
    'rectificationMeasures',
  ].includes(column.dataIndex);
}

function defaultDetailColumnGroups(columns: ThreeCheckTableColumn[]) {
  return [
    columns.filter((_, index) => index % 2 === 0),
    columns.filter((_, index) => index % 2 === 1),
  ].filter((columnGroup) => columnGroup.length);
}

function isLocalMultiSelectColumn(column: ThreeCheckTableColumn) {
  return isThreeCheckMultiSelectField(
    moduleRuntime.value.routeName,
    column.dataIndex,
  );
}

function syncLocalOrganizationFields() {
  localCreateForm.company = optionLabel(
    activeCompanySelectOptions.value,
    createForm.companyId,
  );
  localCreateForm.department = optionLabel(
    departmentSelectOptions.value,
    createForm.departmentId,
  );
  localCreateForm.team = optionLabel(teamSelectOptions.value, createForm.teamId);
}

function applyFixedCompanyDefaults() {
  const option = fixedCompanyOption.value;
  if (!option) {
    return;
  }
  selectedOrganizationKeys.value = [];
  filters.companyId = option.value;
  filters.company = option.label;
  createForm.companyId = option.value;
  syncDepartmentAndTeam(option.value);
  syncLocalOrganizationFields();
}

function applyScopedOrganizationDefaults(target: {
  companyId?: OrganizationId;
  departmentId?: OrganizationId;
  teamId?: OrganizationId;
}) {
  const defaults = currentUserOrgDefaults.value;
  if (isOrganizationFieldLocked('company') && isOrganizationId(defaults.companyId)) {
    target.companyId = defaults.companyId;
  }
  if (
    isOrganizationFieldLocked('department') &&
    isOrganizationId(defaults.departmentId)
  ) {
    target.departmentId = defaults.departmentId;
  }
  if (isOrganizationFieldLocked('team') && isOrganizationId(defaults.teamId)) {
    target.teamId = defaults.teamId;
  }
}

function enforceUserOrganizationScope() {
  if (!formOrganizationSource.value.length) {
    return;
  }
  applyScopedOrganizationDefaults(filters);
  applyScopedOrganizationDefaults(createForm);
  syncLocalOrganizationFields();
}

function defaultLocalCreateValue(column: ThreeCheckTableColumn, now: Date) {
  const today = formatLocalCreateDate(now);
  const currentTime = formatLocalCreateDateTime(now);
  const ownerName = optionLabel(ownerSelectOptions.value, createForm.ownerUserId);

  switch (column.dataIndex) {
    case 'attendees': {
      return createForm.attendeeNames.join('、');
    }
    case 'createdAt':
    case 'dispatchTime': {
      return currentTime;
    }
    case 'uploadTime': {
      return currentTime;
    }
    case 'createdBy': {
      return ownerName;
    }
    case 'date':
    case 'dispatchDate': {
      return today;
    }
    case 'dispatchStatus': {
      return '已生效';
    }
    case 'dispatchType': {
      return moduleRuntime.value.routeName === 'PinganCurtainWallTeamDispatch'
        ? '今日'
        : '常规派班';
    }
    case 'equipmentInspection': {
      return '是';
    }
    case 'imageCheck':
    case 'hazardImage':
    case 'imageOne':
    case 'imageTwo':
    case 'imageThree':
    case 'imageUpload':
    case 'safetyActivityRecordUpload': {
      return '未上传';
    }
    case 'owner':
    case 'responsiblePerson': {
      return ownerName;
    }
    case 'pointsChange': {
      return '加分';
    }
    case 'pointsQuantity': {
      return '1';
    }
    case 'pointsReason': {
      return undefined;
    }
    case 'serialNo': {
      return '';
    }
    case 'status': {
      if (moduleRuntime.value.routeName === 'PinganQuickShot') {
        return '待审核';
      }
      const options = fieldOptions(column);
      return options.at(-1)?.value ?? '待提交';
    }
    case 'user': {
      return ownerName;
    }
    case 'workContent': {
      return fieldOptions(column)
        .slice(0, 7)
        .map((option) => option.value);
    }
    case 'videoCheck':
    case 'videoUpload': {
      return '未上传';
    }
    case 'updatedAt': {
      return currentTime;
    }
    default: {
      return '';
    }
  }
}

function seedLocalCreateForm(now = new Date()) {
  Object.keys(localCreateForm).forEach((key) => {
    delete localCreateForm[key];
  });
  Object.keys(pendingCreateAttachments).forEach((key) => {
    delete pendingCreateAttachments[key];
  });

  for (const column of localCreateColumns.value) {
    localCreateForm[column.dataIndex] = defaultLocalCreateValue(column, now);
  }
  applyFixedCompanyDefaults();
  syncLocalOrganizationFields();
}

function organizationValuesFor(parentId: OrganizationId, orgTypes: string[]) {
  return getCascadedOrganizationOptions(
    formOrganizationSource.value,
    parentId,
    orgTypes,
  ).map((item) => ({ value: item.value }));
}

function normalizeSelectId(value: unknown) {
  if (isOrganizationId(value)) {
    return value;
  }
  return undefined;
}

function handleFilterCompanyChange(value: unknown) {
  if (isOrganizationFieldLocked('company')) {
    enforceUserOrganizationScope();
    return;
  }
  if (fixedCompanyOption.value) {
    applyFixedCompanyDefaults();
    return;
  }
  applyOrganizationFilterCompany(
    filters,
    formOrganizationSource.value,
    normalizeSelectId(value),
  );
}

function handleFilterDepartmentChange(value: unknown) {
  if (isOrganizationFieldLocked('department')) {
    enforceUserOrganizationScope();
    return;
  }
  applyOrganizationFilterDepartment(
    filters,
    formOrganizationSource.value,
    normalizeSelectId(value),
  );
}

function handleFilterTeamChange(value: unknown) {
  if (isOrganizationFieldLocked('team')) {
    enforceUserOrganizationScope();
    return;
  }
  applyOrganizationFilterTeam(
    filters,
    formOrganizationSource.value,
    normalizeSelectId(value),
  );
}

function handleDataMapSelect(keys: unknown[]) {
  if (!showDataMap.value) {
    return;
  }
  selectedOrganizationKeys.value = keys.filter(
    (key): key is string => typeof key === 'string',
  );
  const selectedKey = selectedOrganizationKeys.value[0];
  applyOrganizationFilterFromDataMapSelection(
    filters,
    dataMapOrganizationNodes.value,
    formOrganizationSource.value,
    selectedKey,
  );
  void loadMeetings();
}

function syncDepartmentAndTeam(companyId: OrganizationId) {
  const departmentId = firstOptionValue(
    organizationValuesFor(companyId, ['DEPARTMENT']),
  ) ?? companyId;
  createForm.departmentId = departmentId;
  createForm.teamId =
    firstOptionValue(organizationValuesFor(departmentId, ['TEAM'])) ??
    departmentId;
}

function handleCompanyChange(value: unknown) {
  const companyId = normalizeSelectId(value);
  if (isOrganizationId(companyId)) {
    syncDepartmentAndTeam(companyId);
    syncLocalOrganizationFields();
  }
}

function handleDepartmentChange(value: unknown) {
  const departmentId = normalizeSelectId(value);
  if (isOrganizationId(departmentId)) {
    createForm.teamId =
      firstOptionValue(organizationValuesFor(departmentId, ['TEAM'])) ??
      departmentId;
    syncLocalOrganizationFields();
  }
}

function handleTeamChange() {
  syncLocalOrganizationFields();
}

function handlePointsFlowUserChange(value: unknown) {
  const option = ownerSelectOptions.value.find(
    (item) => String(item.value) === String(value),
  );
  localCreateForm.user = option?.label ?? '';
}

function isCreateAttachmentField(column: ThreeCheckTableColumn) {
  return isThreeCheckCreateAttachmentField(
    moduleRuntime.value.routeName,
    column.dataIndex,
  );
}

function createAttachmentAccept(column: ThreeCheckTableColumn) {
  if (
    moduleRuntime.value.routeName === 'PinganQuickShot' &&
    column.dataIndex === 'hazardImage'
  ) {
    return 'image/*,video/*';
  }
  return getThreeCheckAttachmentColumnKind(column) === 'VIDEO'
    ? 'video/*'
    : 'image/*';
}

function handleCreateAttachmentChange(
  column: ThreeCheckTableColumn,
  event: Event,
) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  pendingCreateAttachments[column.dataIndex] = file;
  localCreateForm[column.dataIndex] = file.name;
}

async function uploadPendingCreateAttachments(recordId: string) {
  const entries = Object.entries(pendingCreateAttachments).filter(
    (entry): entry is [string, File] => Boolean(entry[1]),
  );
  for (const [fieldName, file] of entries) {
    const fileKind =
      moduleRuntime.value.routeName === 'PinganQuickShot' &&
      fieldName === 'hazardImage'
        ? file.type.startsWith('video/')
          ? 'VIDEO'
          : 'IMAGE'
        : getThreeCheckAttachmentColumnKind(fieldName);
    if (fileKind) {
      await uploadThreeCheckRecordAttachmentApi(
        threeCheckModuleKey.value,
        recordId,
        fileKind,
        file,
      );
    }
  }
}

function validateQuickShotHazardMedia() {
  if (moduleRuntime.value.routeName !== 'PinganQuickShot') {
    return true;
  }
  const uploaded = pendingCreateAttachments.hazardImage;
  const label = String(localCreateForm.hazardImage ?? '').trim();
  if (uploaded || !isThreeCheckAttachmentMissing(label)) {
    return true;
  }
  message.error('请上传隐患图片/视频');
  return false;
}

function openCreateModal() {
  if (!canCreateRecord.value) {
    return;
  }
  const now = new Date();
  createForm.meetingDate = formatLocalCreateDate(now);
  createForm.meetingContent = '';
  const organizationDefaults = resolveCreateOrganizationDefaults(
    filters,
    formOrganizationSource.value,
    userStore.userInfo?.orgId ??
      firstOptionValue(activeCompanySelectOptions.value) ??
      createForm.companyId,
  );
  createForm.companyId = organizationDefaults.companyId ?? createForm.companyId;
  createForm.departmentId =
    organizationDefaults.departmentId ?? createForm.departmentId;
  createForm.teamId = organizationDefaults.teamId ?? createForm.teamId;
  createForm.ownerUserId =
    resolveCurrentOwnerUserId();
  createForm.attendeeNames = attendeeSelectOptions.value.some(
    (item) => item.value === 'Demo Harbor班长',
  )
    ? ['Demo Harbor班长']
    : attendeeSelectOptions.value.slice(0, 1).map((item) => item.value);
  applyFixedCompanyDefaults();
  if (
    moduleRuntime.value.createMode === 'LOCAL_TABLE' ||
    isThreeCheckRecordModule.value
  ) {
    seedLocalCreateForm(now);
  }
  enforceUserOrganizationScope();
  syncLocalOrganizationFields();
  createOpen.value = true;
}

function resolveThreeCheckRecordBusinessDate() {
  if (isPointsFlowModule.value) {
    return String(localCreateForm.createdAt || '').slice(0, 10);
  }
  const rawValue =
    localCreateForm.date ||
    localCreateForm.dispatchDate ||
    localCreateForm.createdAt ||
    createForm.meetingDate;
  return String(rawValue).slice(0, 10);
}

function resolveThreeCheckRecordStatus() {
  if (isPointsFlowModule.value) {
    return String(localCreateForm.pointsChange || '加分');
  }
  return localCreateForm.status || localCreateForm.dispatchStatus || undefined;
}

function createThreeCheckRecordPayload() {
  if (usesPreShiftMeetingForm.value) {
    return createPreShiftMeetingRecordPayload({
      attendeeNames: createForm.attendeeNames,
      meetingContent: createForm.meetingContent,
      statusLabel: String(localCreateForm.status || '待开会议'),
    });
  }

  const payload: Record<string, unknown> = { ...localCreateForm };
  delete payload.company;
  delete payload.department;
  delete payload.serialNo;
  delete payload.team;
  if (isPointsFlowModule.value) {
    payload.pointsQuantity = Number(payload.pointsQuantity);
    payload.statusLabel = String(payload.pointsChange || '加分');
  }
  if (isHazardInspectionDetail.value) {
    return serializeHazardInspectionPayload(
      payload,
      normalizeHazardInspectionLines(payload),
    );
  }
  return payload;
}

function validatePointsFlowFields(values: Record<string, unknown>) {
  if (!isPointsFlowModule.value) {
    return true;
  }
  const requiredFields = [
    ['createdAt', '创建时间'],
    ['user', '用户'],
    ['pointsReason', '积分变动原因'],
    ['pointsChange', '积分变动'],
    ['pointsQuantity', '积分数量'],
  ] as const;
  for (const [field, label] of requiredFields) {
    if (!values[field] || !String(values[field]).trim()) {
      message.error(`请填写${label}`);
      return false;
    }
  }
  const quantity = Number(values.pointsQuantity);
  if (!Number.isInteger(quantity) || quantity <= 0) {
    message.error('积分数量必须为正整数');
    return false;
  }
  if (values.pointsChange === '兑换') {
    for (const [field, label] of [
      ['vendingMachine', '贩卖机'],
      ['goods', '货品'],
    ] as const) {
      if (!values[field] || !String(values[field]).trim()) {
        message.error(`请填写${label}`);
        return false;
      }
    }
  }
  return true;
}

function hazardInspectionEmptyValue() {
  return isHazardInspectionDetail.value ? '' : '未填写';
}

function hazardInspectionRequiredFields() {
  const fieldKeys =
    hazardInspectionRequiredFieldKeys[moduleRuntime.value.routeName] ?? [];
  return fieldKeys.map((dataIndex) => {
    const column =
      localDetailColumns.value.find((item) => item.dataIndex === dataIndex) ??
      moduleRuntime.value.columns.find((item) => item.dataIndex === dataIndex);
    return {
      dataIndex,
      title: column?.title ?? dataIndex,
    };
  });
}

function hazardInspectionRequiredValue(
  dataIndex: string,
  values: Record<string, unknown>,
) {
  if (dataIndex in values) {
    return values[dataIndex];
  }
  if (dataIndex === 'company') {
    return createForm.companyId || currentThreeCheckDetail.value?.companyId;
  }
  if (dataIndex === 'department') {
    return createForm.departmentId || currentThreeCheckDetail.value?.departmentId;
  }
  if (dataIndex === 'status') {
    return values.status || resolveThreeCheckRecordStatus();
  }
  if (dataIndex === 'team') {
    return createForm.teamId || currentThreeCheckDetail.value?.teamId;
  }
  const detail = currentThreeCheckDetail.value;
  if (!detail) {
    return undefined;
  }
  const payload = (detail.payload ?? {}) as Record<string, unknown>;
  const directValues: Record<string, unknown> = {
    company: detail.company,
    createdAt: payload.createdAt,
    date: detail.date || detail.businessDate,
    department: detail.department,
    status: detail.statusLabel || getStatusLabel(detail.status),
    team: detail.team,
    updatedAt: detail.clientUpdatedAt || detail.lastSyncedAt,
  };
  return directValues[dataIndex] ?? payload[dataIndex];
}

function isMissingHazardInspectionRequiredValue(value: unknown) {
  if (Array.isArray(value)) {
    return value.filter(Boolean).length === 0;
  }
  return value === undefined || value === null || String(value).trim() === '';
}

function validateHazardInspectionRequiredFields(values: Record<string, unknown>) {
  if (!isHazardInspectionDetail.value) {
    return true;
  }
  const missingFields = hazardInspectionRequiredFields().filter((field) =>
    isMissingHazardInspectionRequiredValue(
      hazardInspectionRequiredValue(field.dataIndex, values),
    ),
  );
  if (missingFields.length) {
    message.error(`请填写${missingFields[0]?.title || '必填项'}`);
    return false;
  }
  return true;
}

async function handleCreateMeeting() {
  if (!canCreateRecord.value) {
    return;
  }
  enforceUserOrganizationScope();
  const companyId = createForm.companyId;
  const departmentId = createForm.departmentId;
  const teamId = createForm.teamId;
  if (!companyId || !departmentId || !teamId) {
    message.error('请选择公司、部门和班组');
    return;
  }
  if (
    !validateHazardInspectionRequiredFields({
      ...localCreateForm,
      company: companyId,
      department: departmentId,
      team: teamId,
    })
  ) {
    return;
  }
  if (!validatePointsFlowFields(localCreateForm)) {
    return;
  }
  if (!validateQuickShotHazardMedia()) {
    return;
  }
  createSaving.value = true;
  try {
    if (moduleRuntime.value.createMode === 'LOCAL_TABLE') {
      const row = createLocalThreeCheckRow({
        columns: moduleRuntime.value.columns,
        routeName: moduleRuntime.value.routeName,
        values: { ...localCreateForm },
      }) as ThreeCheckTableRow;
      rows.value = [row, ...rows.value];
      total.value = rows.value.length;
      message.success(workbenchText.value.createSuccessText);
      createOpen.value = false;
      return;
    }

    if (isThreeCheckRecordModule.value) {
      syncLocalOrganizationFields();
      const createdRecord = await createThreeCheckRecordApi(threeCheckModuleKey.value, {
        businessDate: resolveThreeCheckRecordBusinessDate(),
        companyId,
        departmentId,
        ownerUserId: createForm.ownerUserId,
        payload: createThreeCheckRecordPayload(),
        status: resolveThreeCheckRecordStatus(),
        teamId,
      });
      await uploadPendingCreateAttachments(createdRecord.id);
      message.success(workbenchText.value.createSuccessText);
      createOpen.value = false;
      await loadMeetings();
      return;
    }

    await createPreShiftMeetingApi({
      attendees: createForm.attendeeNames,
      companyId,
      departmentId,
      meetingContent: createForm.meetingContent,
      meetingDate: createForm.meetingDate,
      ownerUserId: createForm.ownerUserId,
      teamId,
    });
    message.success(workbenchText.value.createSuccessText);
    createOpen.value = false;
    await loadMeetings();
  } finally {
    createSaving.value = false;
  }
}

function seedThreeCheckDetailEditForm() {
  Object.keys(detailEditForm).forEach((key) => {
    delete detailEditForm[key];
  });
  if (!currentThreeCheckDetail.value) {
    return;
  }
  Object.assign(
    detailEditForm,
    createThreeCheckDetailEditForm(
      currentThreeCheckDetail.value,
      localDetailColumns.value,
    ),
  );
  if (isSafetyCheckDetail.value) {
    syncHazardInspectionLinesFromDetail();
    syncHazardInspectionFirstLineToEditForm();
    for (const dataIndex of safetyCheckLineEditableDataIndexes()) {
      detailEditForm[dataIndex] = safetyCheckDetailRawValue(dataIndex);
    }
    syncHazardInspectionFirstLineToEditForm();
  }
  syncTeamCheckInspectionLinesFromDetail();
}

function startThreeCheckDetailEdit() {
  seedThreeCheckDetailEditForm();
  detailEditing.value = true;
}

function cancelThreeCheckDetailEdit() {
  detailEditing.value = false;
  seedThreeCheckDetailEditForm();
}

async function saveThreeCheckDetailEdit() {
  if (!currentThreeCheckDetail.value || !canEditCurrentThreeCheckDetail.value) {
    return;
  }
  if (!validateHazardInspectionRequiredFields(detailEditForm)) {
    return;
  }
  if (!validateTeamCheckInspectionLineResults()) {
    return;
  }
  if (!validatePointsFlowFields(detailEditForm)) {
    return;
  }
  const payload = createThreeCheckRecordUpdatePayloadFromDetail(
    currentThreeCheckDetail.value,
    detailEditForm,
  );
  if (isHazardInspectionDetail.value) {
    syncHazardInspectionFirstLineToEditForm();
    payload.payload = serializeHazardInspectionPayload(
      payload.payload as Record<string, unknown>,
      hazardInspectionLines.value,
    );
  }
  if (isTeamCheckInspectionDetail.value) {
    payload.payload = serializeTeamCheckInspectionPayload(
      payload.payload as Record<string, unknown>,
      teamCheckInspectionLines.value,
    );
  }
  if (isPointsFlowModule.value) {
    const detailPayload = payload.payload as Record<string, unknown>;
    detailPayload.pointsQuantity = Number(detailPayload.pointsQuantity);
    detailPayload.statusLabel = String(detailPayload.pointsChange || '加分');
    payload.status = String(detailPayload.pointsChange || '加分');
    if (detailPayload.createdAt) {
      payload.businessDate = String(detailPayload.createdAt).slice(0, 10);
    }
  }
  if (!payload.companyId || !payload.departmentId || !payload.teamId) {
    message.error('明细缺少组织信息，请刷新后重试');
    return;
  }

  detailSaving.value = true;
  try {
    currentThreeCheckDetail.value = await updateThreeCheckRecordApi(
      threeCheckModuleKey.value,
      currentThreeCheckDetail.value.id,
      payload as PinganThreeCheckRecordApi.RecordPayload,
    );
    detailEditing.value = false;
    seedThreeCheckDetailEditForm();
    message.success('明细已保存');
    await loadMeetings();
  } finally {
    detailSaving.value = false;
  }
}

async function openDetail(record: Record<string, any>) {
  currentDetail.value = undefined;
  currentThreeCheckDetail.value = undefined;
  currentLocalDetail.value = undefined;
  detailEditing.value = false;
  activeDetailPanel.value = undefined;
  documentFlowOpen.value = false;
  documentFlowSelectedNodeId.value = '';
  currentDocumentFlow.value = undefined;
  changeHistoryOpen.value = false;
  changeHistorySelectedGroupId.value = '';
  currentChangeHistory.value = undefined;
  seedThreeCheckDetailEditForm();

  if (!canUseWorkbench.value) {
    return;
  }

  if (!moduleApiReady.value) {
    currentLocalDetail.value = record as ThreeCheckTableRow;
    detailLoading.value = false;
    detailOpen.value = true;
    return;
  }

  const meeting = record as MeetingRow;
  detailOpen.value = true;
  detailLoading.value = true;
  try {
    if (isThreeCheckRecordModule.value) {
      currentThreeCheckDetail.value = await getThreeCheckRecordDetailApi(
        threeCheckModuleKey.value,
        meeting.id,
      );
      seedThreeCheckDetailEditForm();
      await loadTeamCheckInspectionLinesFromDetail();
    } else {
      currentDetail.value = await getPreShiftMeetingDetailApi(meeting.id);
    }
  } finally {
    detailLoading.value = false;
  }
}

async function refreshCurrentDetail() {
  if (currentThreeCheckDetail.value) {
    currentThreeCheckDetail.value = await getThreeCheckRecordDetailApi(
      threeCheckModuleKey.value,
      currentThreeCheckDetail.value.id,
    );
    if (!detailEditing.value) {
      seedThreeCheckDetailEditForm();
    }
    await loadTeamCheckInspectionLinesFromDetail();
    return;
  }
  if (currentDetail.value) {
    currentDetail.value = await getPreShiftMeetingDetailApi(currentDetail.value.id);
  }
}

function detailActionLabel(key: ThreeCheckDetailActionKey) {
  return (
    detailActions.value.find((action) => action.key === key)?.label ?? key
  );
}

function detailActionIcon(key: ThreeCheckDetailActionKey) {
  return detailActions.value.find((action) => action.key === key)?.icon ?? '';
}

function scrollToDetailAttachments() {
  detailAttachmentSectionRef.value?.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
  });
}

async function refreshDetailFromToolbar() {
  detailLoading.value = true;
  try {
    await refreshCurrentDetail();
    await loadMeetings();
    message.success('明细已刷新');
  } finally {
    detailLoading.value = false;
  }
}

function toggleDetailPanel(panel: 'attachment' | 'changeHistory' | 'documentFlow') {
  activeDetailPanel.value =
    activeDetailPanel.value === panel ? undefined : panel;
}

async function loadChangeHistory(showFallbackMessage = false) {
  const detail = currentThreeCheckDetail.value;
  if (!detail || !isHazardInspectionDetail.value) {
    return;
  }
  changeHistoryLoading.value = true;
  try {
    const history = await getHazardInspectionChangeHistoryApi(
      threeCheckModuleKey.value,
      detail.id,
    );
    currentChangeHistory.value = history;
    currentThreeCheckDetail.value = history.record;
    if (!changeHistorySelectedGroup.value) {
      changeHistorySelectedGroupId.value = changeHistoryGroups.value[0]?.id ?? '';
    }
  } catch {
    currentChangeHistory.value = undefined;
    if (showFallbackMessage) {
      message.warning('变更历史加载失败，已显示当前版本信息');
    }
  } finally {
    changeHistoryLoading.value = false;
  }
}

async function openChangeHistory() {
  changeHistoryKeyword.value = '';
  changeHistoryFilter.value = 'ALL';
  changeHistorySelectedGroupId.value = changeHistoryGroups.value[0]?.id ?? '';
  changeHistoryOpen.value = true;
  await loadChangeHistory(true);
  changeHistorySelectedGroupId.value = changeHistoryGroups.value[0]?.id ?? '';
}

function closeChangeHistory() {
  changeHistoryOpen.value = false;
}

function selectChangeHistoryGroup(group: ChangeHistoryGroup) {
  changeHistorySelectedGroupId.value = group.id;
}

async function refreshChangeHistory() {
  await loadChangeHistory(true);
  if (!changeHistorySelectedGroup.value) {
    changeHistorySelectedGroupId.value = changeHistoryGroups.value[0]?.id ?? '';
  }
}

function printChangeHistory() {
  window.print();
}

async function loadDocumentFlow(showFallbackMessage = false) {
  const detail = currentThreeCheckDetail.value;
  if (!detail || !isHazardInspectionDetail.value) {
    return;
  }
  documentFlowLoading.value = true;
  try {
    const flow = await getHazardInspectionDocumentFlowApi(
      threeCheckModuleKey.value,
      detail.id,
    );
    currentDocumentFlow.value = flow;
    currentThreeCheckDetail.value = flow.record;
    if (!documentFlowSelectedNode.value) {
      documentFlowSelectedNodeId.value = documentFlowNodes.value[0]?.id ?? '';
    }
  } catch {
    currentDocumentFlow.value = undefined;
    if (showFallbackMessage) {
      message.warning('单据流加载失败，已显示当前明细数据');
    }
  } finally {
    documentFlowLoading.value = false;
  }
}

async function openDocumentFlow() {
  documentFlowKeyword.value = '';
  documentFlowActiveTab.value = 'tree';
  documentFlowSelectedNodeId.value = documentFlowNodes.value[0]?.id ?? '';
  documentFlowOpen.value = true;
  await loadDocumentFlow(true);
  documentFlowSelectedNodeId.value = documentFlowNodes.value[0]?.id ?? '';
}

function closeDocumentFlow() {
  documentFlowOpen.value = false;
}

function selectDocumentFlowNode(node: DocumentFlowNode) {
  documentFlowSelectedNodeId.value = node.id;
}

function selectDocumentFlowStep(step: DocumentFlowStep) {
  if (step.nodeId) {
    documentFlowSelectedNodeId.value = step.nodeId;
    return;
  }
  const matchedNode = documentFlowListNodes.value.find(
    (node) => node.title.includes(step.label) || node.status === step.label,
  );
  if (matchedNode) {
    documentFlowSelectedNodeId.value = matchedNode.id;
  }
}

function setDocumentFlowActiveTab(tab: 'list' | 'tree') {
  documentFlowActiveTab.value = tab;
  if (
    tab === 'list' &&
    documentFlowSelectedNode.value?.type !== 'detail' &&
    documentFlowListNodes.value[0]
  ) {
    documentFlowSelectedNodeId.value = documentFlowListNodes.value[0].id;
  }
  if (tab === 'tree' && documentFlowTreeMasterNode.value) {
    documentFlowSelectedNodeId.value = documentFlowTreeMasterNode.value.id;
  }
}

async function refreshDocumentFlow() {
  if (isHazardInspectionDetail.value) {
    await loadDocumentFlow(true);
  } else {
    await refreshDetailFromToolbar();
  }
  if (!documentFlowSelectedNode.value) {
    documentFlowSelectedNodeId.value = documentFlowNodes.value[0]?.id ?? '';
  }
}

function exportDocumentFlow() {
  if (!canExportRecords.value) {
    return;
  }
  message.info('单据流导出功能待接入');
}

function printDocumentFlow() {
  window.print();
}

async function handleDetailAction(action: ThreeCheckDetailActionKey) {
  switch (action) {
    case 'refresh': {
      await refreshDetailFromToolbar();
      break;
    }
    case 'print': {
      window.print();
      break;
    }
    case 'documentFlow': {
      if (isHazardInspectionDetail.value) {
        await openDocumentFlow();
      } else {
        toggleDetailPanel('documentFlow');
      }
      break;
    }
    case 'attachment': {
      if (isHazardInspectionDetail.value) {
        toggleDetailPanel('attachment');
      } else {
        scrollToDetailAttachments();
      }
      break;
    }
    case 'changeHistory': {
      if (isHazardInspectionDetail.value) {
        await openChangeHistory();
      } else {
        toggleDetailPanel('changeHistory');
      }
      break;
    }
    case 'edit': {
      if (canEditCurrentThreeCheckDetail.value && !detailEditing.value) {
        startThreeCheckDetailEdit();
      }
      break;
    }
  }
}

async function handleSubmit(record: Record<string, any>) {
  if (!moduleApiReady.value) {
    updateLocalRecord(record, 'SUBMIT');
    message.success(workbenchText.value.submitSuccessText);
    return;
  }

  const meeting = record as MeetingRow;
  if (isThreeCheckRecordModule.value) {
    if (
      isTeamCheckInspectionDetail.value &&
      !(await prepareTeamCheckInspectionForSubmit(meeting.id))
    ) {
      return;
    }
    await submitThreeCheckRecordApi(threeCheckModuleKey.value, meeting.id);
  } else {
    await submitPreShiftMeetingApi(meeting.id);
  }
  message.success(workbenchText.value.submitSuccessText);
  await loadMeetings();
}

function quickShotWorkflowActions(record: Record<string, any>) {
  if (!canReviewQuickShotWorkflow.value) {
    return [];
  }
  const status = String(record.status || '');
  const actions: Record<
    string,
    Array<{
      action: PinganThreeCheckRecordApi.QuickShotWorkflowAction;
      label: string;
      type?: 'primary';
    }>
  > = {
    PENDING_REVIEW: [
      { action: 'APPROVE', label: '审核通过', type: 'primary' },
      { action: 'REJECT', label: '驳回' },
    ],
  };
  return actions[status] ?? [];
}

function quickShotWorkflowActionLabel(
  action?: PinganThreeCheckRecordApi.QuickShotWorkflowAction,
) {
  const labels: Record<string, string> = {
    APPROVE: '审核通过',
    REJECT: '驳回',
  };
  return action ? labels[action] || action : '流程处理';
}

function isQuickShotWorkflowDepartmentField(dataIndex: string) {
  return quickShotWorkflowDepartmentFields.includes(dataIndex);
}

function isQuickShotWorkflowDateField(dataIndex: string) {
  return dataIndex === 'rectificationDeadline';
}

function seedQuickShotWorkflowActionForm(
  record: Record<string, any>,
  action: PinganThreeCheckRecordApi.QuickShotWorkflowAction,
) {
  Object.keys(quickShotWorkflowActionForm).forEach((key) => {
    delete quickShotWorkflowActionForm[key];
  });
  const payload = (record.payload ?? {}) as Record<string, unknown>;
  for (const field of quickShotWorkflowFieldGroups[action] ?? []) {
    quickShotWorkflowActionForm[field.dataIndex] = String(
      payload[field.dataIndex] ?? '',
    );
  }
}

function openQuickShotWorkflowAction(
  record: Record<string, any>,
  action: PinganThreeCheckRecordApi.QuickShotWorkflowAction,
) {
  quickShotWorkflowActionRecord.value = record;
  quickShotWorkflowAction.value = action;
  seedQuickShotWorkflowActionForm(record, action);
  quickShotWorkflowActionOpen.value = true;
}

function validateQuickShotWorkflowActionForm() {
  const action = quickShotWorkflowAction.value;
  if (!action) {
    return false;
  }
  for (const field of quickShotWorkflowFieldGroups[action] ?? []) {
    if (
      field.required &&
      !String(quickShotWorkflowActionForm[field.dataIndex] ?? '').trim()
    ) {
      message.error(`请填写${field.label}`);
      return false;
    }
  }
  return true;
}

async function handleQuickShotWorkflowPhotoChange(
  field: { dataIndex: string; label: string },
  event: Event,
) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  const record = quickShotWorkflowActionRecord.value;
  if (!file || !record) {
    return;
  }
  quickShotWorkflowPhotoUploading.value = true;
  try {
    const attachment = await uploadThreeCheckRecordAttachmentApi(
      threeCheckModuleKey.value,
      String(record.id),
      'IMAGE',
      file,
    );
    quickShotWorkflowActionForm[field.dataIndex] =
      attachment.url || attachment.storagePath || attachment.originalName || file.name;
    const refreshed = await getThreeCheckRecordDetailApi(
      threeCheckModuleKey.value,
      String(record.id),
    );
    quickShotWorkflowActionRecord.value = refreshed as unknown as Record<string, any>;
    if (currentThreeCheckDetail.value?.id === String(record.id)) {
      currentThreeCheckDetail.value = refreshed;
    }
    message.success(`${field.label}已上传`);
  } finally {
    quickShotWorkflowPhotoUploading.value = false;
    input.value = '';
  }
}

async function handleQuickShotWorkflowAction() {
  const record = quickShotWorkflowActionRecord.value;
  const action = quickShotWorkflowAction.value;
  if (!record || !action || !validateQuickShotWorkflowActionForm()) {
    return;
  }
  const meeting = record as MeetingRow;
  const payload = Object.fromEntries(
    Object.entries(quickShotWorkflowActionForm).filter(([, value]) =>
      String(value ?? '').trim(),
    ),
  );
  quickShotWorkflowActionSaving.value = true;
  try {
    await workflowActionThreeCheckRecordApi(threeCheckModuleKey.value, meeting.id, {
    action,
    payload,
    version: Number(record.version ?? 0),
  });
    message.success(`${quickShotWorkflowActionLabel(action)}成功`);
    quickShotWorkflowActionOpen.value = false;
    await loadMeetings();
    if (currentThreeCheckDetail.value?.id === meeting.id) {
      await refreshCurrentDetail();
    }
  } finally {
    quickShotWorkflowActionSaving.value = false;
  }
}

async function handleWithdraw(record: Record<string, any>) {
  if (!moduleApiReady.value) {
    updateLocalRecord(record, 'WITHDRAW');
    message.success(workbenchText.value.withdrawSuccessText);
    return;
  }

  const meeting = record as MeetingRow;
  if (isThreeCheckRecordModule.value) {
    await withdrawThreeCheckRecordApi(
      threeCheckModuleKey.value,
      meeting.id,
      'PC端撤回修改',
    );
  } else {
    await withdrawPreShiftMeetingApi(meeting.id, 'PC端撤回修改');
  }
  message.success(workbenchText.value.withdrawSuccessText);
  await loadMeetings();
  if (currentThreeCheckDetail.value?.id === meeting.id) {
    await refreshCurrentDetail();
  }
}

async function handleRemind(record: Record<string, any>) {
  if (!moduleApiReady.value) {
    updateLocalRecord(record, 'REMIND');
    message.success('已发送催办');
    return;
  }

  const meeting = record as MeetingRow;
  if (isThreeCheckRecordModule.value) {
    await remindThreeCheckRecordApi(threeCheckModuleKey.value, meeting.id);
  } else {
    await remindPreShiftMeetingApi(meeting.id);
  }
  message.success('已发送催办');
  await loadMeetings();
}

function clearSelectedRows() {
  selectedRowKeys.value = [];
}

async function runBatchAction(action: PinganThreeCheckRecordApi.BatchAction) {
  if (
    action !== 'DELETE' ||
    !isThreeCheckRecordModule.value ||
    !selectedRowKeys.value.length ||
    !canBatchDelete.value
  ) {
    return;
  }
  const result = await batchThreeCheckRecordsApi(threeCheckModuleKey.value, {
    action,
    ids: [...selectedRowKeys.value],
  });
  if (result.failureCount > 0) {
    message.warning(
      `批量删除完成：成功 ${result.successCount} 条，失败 ${result.failureCount} 条`,
    );
  } else {
    message.success(`批量删除成功 ${result.successCount} 条`);
  }
  clearSelectedRows();
  await loadMeetings();
}

async function handleBatchAction(action: PinganThreeCheckRecordApi.BatchAction) {
  if (!hasSelectedRows.value) {
    return;
  }
  if (action === 'DELETE') {
    Modal.confirm({
      content: `将删除已选 ${selectedRowKeys.value.length} 条记录，删除后列表和统计将不再显示。`,
      okText: '删除',
      okType: 'danger',
      onOk: () => runBatchAction(action),
      title: '确认批量删除？',
    });
    return;
  }
  await runBatchAction(action);
}

async function handleDelete(record: Record<string, any>) {
  if (
    !canDeleteRecord.value ||
    (!isThreeCheckRecordModule.value && !record.canDelete)
  ) {
    return;
  }
  const meeting = record as MeetingRow;
  Modal.confirm({
    content: `确认删除记录 ${meeting.id}？删除后列表和统计将不再显示。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      if (isThreeCheckRecordModule.value) {
        await deleteThreeCheckRecordApi(threeCheckModuleKey.value, meeting.id);
      } else {
        await deletePreShiftMeetingApi(meeting.id);
      }
      message.success('记录已删除');
      selectedRowKeys.value = selectedRowKeys.value.filter(
        (key) => key !== String(meeting.id),
      );
      if (currentThreeCheckDetail.value?.id === meeting.id) {
        detailOpen.value = false;
        currentThreeCheckDetail.value = undefined;
      }
      await loadMeetings();
    },
    title: '确认删除？',
  });
}

async function handleOpenRectificationOrder(record: Record<string, any>) {
  if (
    !isOneShiftInspectionModule.value ||
    !canCreateRectificationOrder.value ||
    !record.canCreateRectificationOrder
  ) {
    return;
  }
  const order = await openThreeCheckRectificationOrderApi(
    threeCheckModuleKey.value,
    String(record.id),
  );
  message.success(`整改单 ${order.orderNo || order.id} 已生成`);
  await loadMeetings();
}

function updateLocalRecord(
  record: Record<string, any>,
  action: 'REMIND' | 'SUBMIT' | 'WITHDRAW',
) {
  const updated = applyLocalThreeCheckAction(record, action) as ThreeCheckTableRow;
  rows.value = rows.value.map((item) => (item.id === updated.id ? updated : item));
  if (currentLocalDetail.value?.id === updated.id) {
    currentLocalDetail.value = updated;
  }
}

async function persistPendingTeamCheckInspectionEdits() {
  if (
    !currentThreeCheckDetail.value ||
    !isTeamCheckInspectionDetail.value ||
    !detailEditing.value
  ) {
    return;
  }
  currentThreeCheckDetail.value = await updateThreeCheckRecordApi(
    threeCheckModuleKey.value,
    currentThreeCheckDetail.value.id,
    createTeamCheckInspectionUpdatePayload(
      currentThreeCheckDetail.value,
      teamCheckInspectionLines.value,
    ),
  );
}

async function handleDeleteAttachment(
  attachment:
    | PinganPreShiftMeetingApi.MeetingDetail['attachments'][number]
    | PinganThreeCheckRecordApi.RecordDetail['attachments'][number],
) {
  if (!canUploadAttachment.value) {
    return;
  }
  await persistPendingTeamCheckInspectionEdits();
  if (currentThreeCheckDetail.value) {
    await deleteThreeCheckRecordAttachmentApi(
      threeCheckModuleKey.value,
      currentThreeCheckDetail.value.id,
      attachment.id,
    );
  } else if (currentDetail.value) {
    await deletePreShiftMeetingAttachmentApi(currentDetail.value.id, attachment.id);
  } else {
    return;
  }
  message.success('附件已删除');
  await refreshCurrentDetail();
  await loadMeetings();
}

async function handleAttachmentChange(
  fileKind: 'IMAGE' | 'VIDEO',
  event: Event,
) {
  if (!canUploadAttachment.value) {
    return;
  }
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file || (!currentDetail.value && !currentThreeCheckDetail.value)) {
    return;
  }
  if (currentThreeCheckDetail.value) {
    await persistPendingTeamCheckInspectionEdits();
    const attachment = await uploadThreeCheckRecordAttachmentApi(
      threeCheckModuleKey.value,
      currentThreeCheckDetail.value.id,
      fileKind,
      file,
    );
    currentThreeCheckDetail.value = upsertAttachmentPreview(
      currentThreeCheckDetail.value as any,
      attachment as any,
    ) as PinganThreeCheckRecordApi.RecordDetail;
  } else if (currentDetail.value) {
    const attachment = await uploadPreShiftMeetingAttachmentApi(
      currentDetail.value.id,
      fileKind,
      file,
    );
    currentDetail.value = upsertAttachmentPreview(
      currentDetail.value as any,
      attachment as any,
    ) as PinganPreShiftMeetingApi.MeetingDetail;
  }
  message.success(fileKind === 'IMAGE' ? '图片打卡已上传' : '视频打卡已上传');
  await refreshCurrentDetail();
  await loadMeetings();
  input.value = '';
}

function handleAnyAttachmentChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  const fileKind = file?.type.startsWith('video/') ? 'VIDEO' : 'IMAGE';
  void handleAttachmentChange(fileKind, event);
}

async function handleDetailAttachmentFieldChange(
  column: ThreeCheckTableColumn,
  event: Event,
  line?: HazardInspectionLine,
) {
  const fileKind = detailAttachmentFieldKind(column);
  if (!fileKind || !canUploadAttachment.value || !currentThreeCheckDetail.value) {
    return;
  }

  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }

  try {
    await persistPendingTeamCheckInspectionEdits();
    const attachment = await uploadThreeCheckRecordAttachmentApi(
      threeCheckModuleKey.value,
      currentThreeCheckDetail.value.id,
      fileKind,
      file,
    );
    const updatedDetail = upsertAttachmentPreview(
      currentThreeCheckDetail.value as any,
      attachment as any,
    ) as PinganThreeCheckRecordApi.RecordDetail;
    const fieldValue =
      attachment.url || attachment.storagePath || attachment.originalName || file.name;
    if (line) {
      line[column.dataIndex] = fieldValue;
      if (hazardInspectionLines.value[0] === line) {
        detailEditForm[column.dataIndex] = fieldValue;
      }
      const refreshedDetail = await getThreeCheckRecordDetailApi(
        threeCheckModuleKey.value,
        currentThreeCheckDetail.value.id,
      );
      const nextPayload = serializeHazardInspectionPayload(
        {
          ...(refreshedDetail.payload ?? {}),
          [column.dataIndex]: fieldValue,
        },
        hazardInspectionLines.value,
      );
      currentThreeCheckDetail.value = await updateThreeCheckRecordApi(
        threeCheckModuleKey.value,
        refreshedDetail.id,
        {
          businessDate:
            refreshedDetail.businessDate || refreshedDetail.date,
          companyId: refreshedDetail.companyId,
          departmentId: refreshedDetail.departmentId,
          ownerUserId: refreshedDetail.ownerUserId,
          payload: nextPayload,
          status: String(
            nextPayload.statusLabel ||
              refreshedDetail.statusLabel ||
              refreshedDetail.status,
          ),
          teamId: refreshedDetail.teamId,
          version: refreshedDetail.version,
        } as PinganThreeCheckRecordApi.RecordPayload,
      );
      syncHazardInspectionLinesFromDetail();
    } else {
      const nextPayload: Record<string, unknown> = {
        ...(updatedDetail.payload ?? {}),
        [column.dataIndex]: fieldValue,
      };
      detailEditForm[column.dataIndex] = fieldValue;
      currentThreeCheckDetail.value = {
        ...updatedDetail,
        payload: nextPayload,
      };
    }
    message.success(
      `${column.title}${fileKind === 'VIDEO' ? '视频' : '图片'}已上传`,
    );
    await loadMeetings();
  } finally {
    input.value = '';
  }
}

function attachmentPreviewUrl(
  attachment:
    | PinganPreShiftMeetingApi.MeetingDetail['attachments'][number]
    | PinganThreeCheckRecordApi.RecordDetail['attachments'][number],
) {
  return resolveAttachmentPreviewUrl(attachment.url || attachment.storagePath);
}

function threeCheckDetailValue(column: ThreeCheckTableColumn) {
  const detail = currentThreeCheckDetail.value;
  if (!detail) {
    return '';
  }
  const payload = (detail.payload ?? {}) as Record<string, any>;
  const directValues: Record<string, unknown> = {
    company: detail.company,
    date: detail.date || detail.businessDate,
    department: detail.department,
    attachmentUpload: detail.attachments?.length
      ? `${detail.attachments.length}个附件`
      : '未上传',
    imageCheck: detail.imageCheck,
    owner: detail.owner,
    status: detail.statusLabel || getStatusLabel(detail.status),
    team: detail.team,
    videoCheck: detail.videoCheck,
  };
  return (
    directValues[column.dataIndex] ??
    payload[column.dataIndex] ??
    hazardInspectionEmptyValue()
  );
}

function isHazardInspectionRoute(routeName: string) {
  return hazardInspectionRouteNames.includes(routeName);
}

function documentFlowOwner() {
  const detail = currentDocumentFlowDetail.value;
  if (!detail) {
    return '未填写';
  }
  const payload = (detail.payload ?? {}) as Record<string, unknown>;
  return String(
    payload.rectificationResponsiblePerson ||
      payload.responsiblePerson ||
      payload.inspector ||
      payload.reporter ||
      payload.createdBy ||
      detail.owner ||
      '未填写',
  );
}

function documentFlowActionLabel(action?: string) {
  const labels: Record<string, string> = {
    ACCEPT: '验收通过',
    APPROVE: '审核通过',
    CREATE: '创建记录',
    ISSUE_RECTIFICATION: '下发整改',
    MARK_RECTIFIED: '整改完成',
    REJECT: '驳回',
    REMIND: '催办',
    REQUEST_ACCEPTANCE: '提交验收',
    SUBMIT: '提交检查',
    UPDATE: '更新记录',
    WITHDRAW: '撤回',
  };
  return action ? labels[action] || action : '流程记录';
}

function documentFlowStepNodeId(
  log: PinganThreeCheckRecordApi.DocumentFlowStatusLog,
  index: number,
) {
  return `log-${log.id || index}`;
}

function quickShotDocumentFlowSnapshotPayload(
  log?: PinganThreeCheckRecordApi.DocumentFlowStatusLog,
) {
  const detail = currentDocumentFlowDetail.value;
  const currentPayload = (detail?.payload ?? {}) as Record<string, unknown>;
  const snapshotPayload = (log?.payload ?? {}) as Record<string, unknown>;
  const hasSnapshot = Object.keys(snapshotPayload).length > 0;
  return {
    hasSnapshot,
    payload: hasSnapshot ? snapshotPayload : currentPayload,
  };
}

function quickShotDocumentFlowValue(
  dataIndex: string,
  payload: Record<string, unknown>,
  log?: PinganThreeCheckRecordApi.DocumentFlowStatusLog,
) {
  const detail = currentDocumentFlowDetail.value;
  const directValues: Record<string, unknown> = {
    company: detail?.company,
    department: detail?.department,
    reporter: payload.reporter || payload.createdBy || detail?.owner,
    status: log?.toStatusLabel || detail?.statusLabel || detail?.status || '',
    team: detail?.team,
    uploadTime:
      payload.uploadTime ||
      payload.uploadedAt ||
      payload.createdAt ||
      log?.createdAt ||
      detail?.businessDate,
  };
  const value = directValues[dataIndex] ?? payload[dataIndex];
  if (dataIndex === 'aiEnabled') {
    if (value === true || value === 'true' || value === '是') {
      return '是';
    }
    if (value === false || value === 'false' || value === '否') {
      return '否';
    }
  }
  return value === undefined || value === null || value === ''
    ? '未填写'
    : String(value);
}

function quickShotDocumentFlowNodeFields(
  log?: PinganThreeCheckRecordApi.DocumentFlowStatusLog,
) {
  const action = log?.action || 'CREATE';
  const { hasSnapshot, payload } = quickShotDocumentFlowSnapshotPayload(log);
  const createFieldConfigs = quickShotDocumentFlowFieldGroups.CREATE ?? [];
  const fieldConfigs =
    quickShotDocumentFlowFieldGroups[action] ??
    createFieldConfigs;
  const fields: DocumentFlowField[] = fieldConfigs.map((field) => ({
    kind: ((
      field.kind ??
      (/photo|image|图片|照片/i.test(field.dataIndex) ? 'IMAGE' : 'TEXT')
    ) as DocumentFlowField['kind']),
    label: field.label,
    value: quickShotDocumentFlowValue(field.dataIndex, payload, log),
  }));
  if (log && !hasSnapshot) {
    fields.unshift({
      kind: 'TEXT',
      label: '快照提示',
      value: '无历史快照，显示当前信息',
    });
  }
  fields.push({
    kind: 'STATUS',
    label: '状态',
    value:
      log?.toStatusLabel ||
      currentDocumentFlowDetail.value?.statusLabel ||
      currentDocumentFlowDetail.value?.status ||
      '',
  });
  return fields;
}

function quickShotLinkedRectificationOrderValue(value: unknown) {
  return value === undefined || value === null || value === ''
    ? '未填写'
    : String(value);
}

function quickShotLinkedRectificationOrderFlowText(
  order: NonNullable<PinganThreeCheckRecordApi.DocumentFlowResponse['linkedRectificationOrder']>,
) {
  if (!order.flowLogs?.length) {
    return '暂无工单流程';
  }
  return order.flowLogs
    .map((log) => {
      const action = log.actionLabel || documentFlowActionLabel(log.action);
      const status = log.toStatusLabel || log.toStatus || '未记录状态';
      const time = formatChangeHistoryTime(log.createdAt);
      const remark = log.remark ? `，${log.remark}` : '';
      return `${time} ${action}至${status}${remark}`;
    })
    .join('\n');
}

function quickShotLinkedRectificationOrderNode(): DocumentFlowNode | undefined {
  const order = currentDocumentFlow.value?.linkedRectificationOrder;
  if (!order) {
    return undefined;
  }
  const status = order.statusLabel || order.status || '未记录状态';
  const date =
    order.closedAt ||
    order.rectifiedAt ||
    order.issuedAt ||
    order.businessDate ||
    '未记录日期';
  const owner = quickShotLinkedRectificationOrderValue(
    order.rectificationResponsibleUserId || order.issuedBy,
  );
  return {
    date,
    fields: [
      { label: '整改工单号', value: quickShotLinkedRectificationOrderValue(order.orderNo) },
      { label: '来源单号', value: quickShotLinkedRectificationOrderValue(order.sourceRecordNo) },
      { kind: 'STATUS', label: '工单状态', value: status },
      { label: '隐患数量', value: quickShotLinkedRectificationOrderValue(order.hazardCount) },
      {
        label: '整改责任人',
        value: quickShotLinkedRectificationOrderValue(order.rectificationResponsibleUserId),
      },
      {
        label: '整改部门',
        value: quickShotLinkedRectificationOrderValue(order.rectificationDepartmentId),
      },
      {
        label: '整改要求',
        value: quickShotLinkedRectificationOrderValue(order.rectificationRequirement),
      },
      {
        label: '整改期限',
        value: quickShotLinkedRectificationOrderValue(order.rectificationDeadline),
      },
      {
        label: '整改说明',
        value: quickShotLinkedRectificationOrderValue(order.rectificationDescription),
      },
      {
        kind: 'IMAGE',
        label: '整改后照片',
        value: quickShotLinkedRectificationOrderValue(order.rectificationAfterPhoto),
      },
      {
        label: '验收结果',
        value: quickShotLinkedRectificationOrderValue(order.acceptanceResult),
      },
      {
        kind: 'TEXT',
        label: '工单流程',
        value: quickShotLinkedRectificationOrderFlowText(order),
      },
    ],
    id: 'linked-rectification-order',
    owner,
    status,
    subtitle: `${order.orderNo || '未生成工单号'} | ${status} | ${date}`,
    title: '关联隐患整改工单',
    type: 'detail',
  };
}

function quickShotDocumentFlowNodes(): DocumentFlowNode[] {
  const detail = currentDocumentFlowDetail.value;
  const logs = currentDocumentFlowStatusLogs.value;
  const fallbackDate = detail?.businessDate || detail?.date || '未记录日期';
  if (!logs.length) {
    const statusLabel = detail?.statusLabel || detail?.status || '';
    const nodes: DocumentFlowNode[] = [
      {
        date: fallbackDate,
        fields: quickShotDocumentFlowNodeFields(),
        id: 'detail-1',
        owner: documentFlowOwner(),
        status: statusLabel,
        subtitle: `${statusLabel || '待审核'} | ${documentFlowOwner()} | ${fallbackDate}`,
        title: '[状态] 当前随手拍信息',
        type: 'detail',
      },
    ];
    const linkedOrderNode = quickShotLinkedRectificationOrderNode();
    return linkedOrderNode ? [...nodes, linkedOrderNode] : nodes;
  }
  const nodes = logs.map((log, index): DocumentFlowNode => {
    const label = documentFlowActionLabel(log.action);
    const status = log.toStatusLabel || log.toStatus || '';
    const owner = log.operatorName || log.operatorId || documentFlowOwner();
    const date = formatChangeHistoryTime(log.createdAt);
    return {
      date,
      fields: quickShotDocumentFlowNodeFields(log),
      id: documentFlowStepNodeId(log, index),
      owner,
      status,
      subtitle: `${status || '未记录状态'} | ${owner || '未填写'} | ${date}`,
      title: `[状态] ${label}`,
      type: 'detail',
    };
  });
  const linkedOrderNode = quickShotLinkedRectificationOrderNode();
  return linkedOrderNode ? [...nodes, linkedOrderNode] : nodes;
}

function formatChangeHistoryTime(value?: string) {
  if (!value) {
    return '未记录';
  }
  const match = value.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2}:\d{2})/);
  return match ? `${match[1]} ${match[2]}` : value;
}

function changeHistoryDisplayVersion(group?: ChangeHistoryGroup) {
  return group ? group.displayVersion : 1;
}

function changeHistoryFieldLabel(item: PinganThreeCheckRecordApi.ChangeHistoryItem) {
  const mappedLabel = changeHistoryFieldLabelMap[item.fieldKey];
  if (!item.fieldLabel || item.fieldLabel === item.fieldKey) {
    return mappedLabel || item.fieldKey;
  }
  return item.fieldLabel;
}

function changeHistoryActionLabel(action?: string) {
  const labels: Record<string, string> = {
    ATTACHMENT: '附件变更',
    CREATE: '创建记录',
    REMIND: '催办',
    SUBMIT: '提交检查',
    UPDATE: '更新记录',
    WITHDRAW: '撤回',
  };
  return action ? labels[action] || action : '变更记录';
}

function changeHistoryTypeLabel(item: PinganThreeCheckRecordApi.ChangeHistoryItem) {
  if (item.action === 'ATTACHMENT') {
    return item.valueType === 'VIDEO' ? '视频' : '图片';
  }
  if (item.fieldKey === 'status' || item.valueType === 'STATUS') {
    return '状态';
  }
  return item.action === 'UPDATE' ? '字段' : '流程';
}

function changeHistoryValue(value?: string) {
  return value === undefined || value === null || value === '' ? '未填写' : value;
}

function documentFlowCurrentStepIndex(statusLabel?: string) {
  const normalized = normalizeThreeCheckStatusBoxLabel(statusLabel || '');
  if (isQuickShotModule.value) {
    const quickShotStatus = (() => {
      if (normalized === '通过' || normalized === '已审批') {
        return '已审核';
      }
      if (normalized === '不通过') {
        return '已验收';
      }
      if (normalized === '待审批') {
        return '待审核';
      }
      return normalized;
    })();
    const labels =
      quickShotStatus === '已驳回'
        ? quickShotDocumentFlowRejectedStepLabels
        : quickShotDocumentFlowStepLabels;
    const index = labels.indexOf(quickShotStatus);
    return index >= 0 ? index : 1;
  }
  if (['已完成', '已验收', '通过', '已生效已读', '已开会议'].includes(normalized)) {
    return 4;
  }
  if (['待验收'].includes(normalized)) {
    return 3;
  }
  if (
    [
      '待整改',
      '待处理',
      '已生效（未读）',
      '待审批',
      '待批准',
      '待检查',
    ].includes(normalized)
  ) {
    return 2;
  }
  return 1;
}

function documentFlowCurrentStepLabel(statusLabel?: string) {
  if (isQuickShotModule.value) {
    return quickShotDocumentFlowStepLabels[
      documentFlowCurrentStepIndex(statusLabel)
    ];
  }
  return ['创建记录', '提交检查', '整改处理中', '待验收', '已完成'][
    documentFlowCurrentStepIndex(statusLabel)
  ];
}

function documentFlowDetailColumns() {
  return localDetailColumns.value.filter((column) => !isDetailAttachmentColumn(column));
}

function documentFlowFieldValue(column: ThreeCheckTableColumn) {
  return String(threeCheckDetailValue(column) || hazardInspectionEmptyValue());
}

function documentFlowFieldKind(column: ThreeCheckTableColumn): DocumentFlowField['kind'] {
  const attachmentKind = detailAttachmentFieldKind(column);
  if (attachmentKind) {
    return attachmentKind;
  }
  return isStatusBoxColumn(column) ? 'STATUS' : 'TEXT';
}

function documentFlowDetailNode(): DocumentFlowNode {
  const detail = currentDocumentFlowDetail.value;
  const columns = documentFlowDetailColumns();
  const statusLabel = detail ? detail.statusLabel || getStatusLabel(detail.status) : '';
  const owner = documentFlowOwner();
  const date = detail?.businessDate || detail?.date || '未记录日期';
  const fields = columns.map((column) => {
    const kind = documentFlowFieldKind(column);
    const imageValue =
      kind === 'IMAGE'
          ? detailAttachmentFieldPreview(column)
          : '';
    return {
      kind,
      label: column.title,
      value: imageValue || documentFlowFieldValue(column),
    };
  });
  const descriptionField =
    fields.find((field) => /隐患描述|检查内容|处罚结论|巡检/.test(field.label)) ??
    fields.find((field) => field.kind === 'TEXT');
  return {
    date,
    fields,
    id: 'detail-1',
    owner,
    status: statusLabel,
    subtitle: `${statusLabel} | ${owner} | ${date}`,
    title: `[明细] ${descriptionField?.value || '单据明细'}`,
    type: 'detail',
  };
}

function hazardInspectionDisplayValue(value: unknown) {
  if (Array.isArray(value)) {
    return value.filter(Boolean).join('、') || hazardInspectionEmptyValue();
  }
  return value === undefined || value === null || value === ''
    ? hazardInspectionEmptyValue()
    : String(value);
}

function safetyCheckDetailRawValue(dataIndex: string) {
  const detail = currentThreeCheckDetail.value;
  if (!detail) {
    return '';
  }
  const payload = (detail.payload ?? {}) as Record<string, unknown>;
  const directValues: Record<string, unknown> = {
    company: detail.company,
    date: detail.date || detail.businessDate,
    department: detail.department,
    status: detail.statusLabel || getStatusLabel(detail.status),
    team: detail.team,
    updatedAt: detail.clientUpdatedAt || detail.lastSyncedAt,
  };
  return directValues[dataIndex] ?? payload[dataIndex] ?? '';
}

function isHazardInspectionImageCell(dataIndex: string) {
  return ['afterRectificationPhoto', 'beforeRectificationPhoto'].includes(
    dataIndex,
  );
}

function syncHazardInspectionLinesFromDetail() {
  if (!isHazardInspectionDetail.value || !currentThreeCheckDetail.value) {
    hazardInspectionLines.value = [];
    hazardInspectionLineKeyword.value = '';
    return;
  }
  hazardInspectionLines.value = normalizeHazardInspectionLines(
    currentThreeCheckDetail.value.payload as Record<string, unknown>,
  );
  loadHazardInspectionViewColumns();
}

function teamCheckInspectionPayload(detail = currentThreeCheckDetail.value) {
  return (detail?.payload ?? {}) as Record<string, unknown>;
}

function teamCheckInspectionSnapshotLines(
  detail = currentThreeCheckDetail.value,
) {
  return normalizeTeamCheckInspectionLines(
    teamCheckInspectionPayload(detail).checkItems,
  );
}

async function resolveTeamCheckInspectionLinesFromTemplate(
  detail: PinganThreeCheckRecordApi.RecordDetail,
) {
  const stage = getTeamCheckInspectionStage(moduleRuntime.value.routeName);
  if (!stage || !detail.companyId) {
    return [];
  }
  const template = await getTeamCheckTemplateResolveApi({
    companyOrgId: detail.companyId,
    departmentOrgId: detail.departmentId,
    stage,
    teamOrgId: detail.teamId,
  });
  return normalizeTeamCheckInspectionLines(template?.items ?? []);
}

async function resolveTeamCheckInspectionLinesForDetail(
  detail: PinganThreeCheckRecordApi.RecordDetail,
) {
  const snapshotLines = teamCheckInspectionSnapshotLines(detail);
  if (snapshotLines.length) {
    return snapshotLines;
  }
  return resolveTeamCheckInspectionLinesFromTemplate(detail);
}

async function loadTeamCheckInspectionLinesFromDetail() {
  if (!isTeamCheckInspectionDetail.value || !currentThreeCheckDetail.value) {
    teamCheckInspectionLines.value = [];
    return;
  }
  teamCheckInspectionLoading.value = true;
  try {
    teamCheckInspectionLines.value =
      await resolveTeamCheckInspectionLinesForDetail(
        currentThreeCheckDetail.value,
      );
  } catch {
    teamCheckInspectionLines.value = [];
    message.warning('班组检查项模板加载失败，请稍后重试');
  } finally {
    teamCheckInspectionLoading.value = false;
  }
}

function syncTeamCheckInspectionLinesFromDetail() {
  if (!isTeamCheckInspectionDetail.value || !currentThreeCheckDetail.value) {
    teamCheckInspectionLines.value = [];
    return;
  }
  const snapshotLines = teamCheckInspectionSnapshotLines();
  if (snapshotLines.length) {
    teamCheckInspectionLines.value = snapshotLines;
  }
}

function validateTeamCheckInspectionLineResults() {
  if (!isTeamCheckInspectionDetail.value) {
    return true;
  }
  const result = validateTeamCheckInspectionResults(teamCheckInspectionLines.value);
  if (!result.valid) {
    message.error(result.message || '请填写检查结果');
    return false;
  }
  return true;
}

function sameTeamCheckInspectionLines(
  detail: PinganThreeCheckRecordApi.RecordDetail,
  lines: TeamCheckInspectionLine[],
) {
  return (
    JSON.stringify(teamCheckInspectionSnapshotLines(detail)) ===
    JSON.stringify(lines)
  );
}

function createTeamCheckInspectionUpdatePayload(
  detail: PinganThreeCheckRecordApi.RecordDetail,
  lines: TeamCheckInspectionLine[],
) {
  return {
    businessDate: detail.businessDate,
    companyId: detail.companyId,
    departmentId: detail.departmentId,
    ownerUserId: detail.ownerUserId,
    payload: serializeTeamCheckInspectionPayload(
      teamCheckInspectionPayload(detail),
      lines,
    ),
    status: detail.status,
    teamId: detail.teamId,
    version: detail.version,
  } as PinganThreeCheckRecordApi.RecordPayload;
}

async function prepareTeamCheckInspectionForSubmit(recordId: string) {
  if (!isTeamCheckInspectionDetail.value) {
    return true;
  }
  const detail = await getThreeCheckRecordDetailApi(
    threeCheckModuleKey.value,
    recordId,
  );
  const lines = await resolveTeamCheckInspectionLinesForDetail(detail);
  const result = validateTeamCheckInspectionResults(lines);
  if (!result.valid) {
    message.error(result.message || '请填写检查结果');
    return false;
  }
  if (!sameTeamCheckInspectionLines(detail, lines)) {
    await updateThreeCheckRecordApi(
      threeCheckModuleKey.value,
      recordId,
      createTeamCheckInspectionUpdatePayload(detail, lines),
    );
  }
  return true;
}

function syncHazardInspectionFirstLineToEditForm() {
  const firstLine = hazardInspectionLines.value[0];
  if (!firstLine) {
    return;
  }
  for (const column of hazardInspectionLineColumns.value) {
    if (column.dataIndex !== 'sequence') {
      detailEditForm[column.dataIndex] = firstLine[column.dataIndex];
    }
  }
}

function loadHazardInspectionViewColumns() {
  const key = getHazardInspectionViewStorageKey(moduleRuntime.value.routeName);
  const defaultKeys = [...hazardInspectionDefaultVisibleColumnKeys.value];
  try {
    const parsed = JSON.parse(window.localStorage.getItem(key) || '[]');
    hazardInspectionVisibleColumnKeys.value = Array.isArray(parsed)
      ? parsed.filter((item) => defaultKeys.includes(String(item))).map(String)
      : defaultKeys;
  } catch {
    hazardInspectionVisibleColumnKeys.value = defaultKeys;
  }
  if (!hazardInspectionVisibleColumnKeys.value.length) {
    hazardInspectionVisibleColumnKeys.value = defaultKeys;
  }
}

function saveHazardInspectionViewColumns() {
  const key = getHazardInspectionViewStorageKey(moduleRuntime.value.routeName);
  window.localStorage.setItem(
    key,
    JSON.stringify(hazardInspectionVisibleColumnKeys.value),
  );
  hazardInspectionViewOpen.value = false;
  message.success('自定义视图已保存');
}

function openHazardInspectionView() {
  loadHazardInspectionViewColumns();
  hazardInspectionViewOpen.value = true;
}

function addHazardInspectionLine() {
  if (!canEditCurrentThreeCheckDetail.value) {
    return;
  }
  if (!detailEditing.value) {
    startThreeCheckDetailEdit();
  }
  hazardInspectionLines.value = [
    ...hazardInspectionLines.value,
    createHazardInspectionLine(hazardInspectionLines.value),
  ];
}

function hazardInspectionLineDisplayValue(
  line: HazardInspectionLine,
  dataIndex: string,
) {
  if (dataIndex === 'validated') {
    return line.validated ? '已校验' : '待校验';
  }
  return hazardInspectionDisplayValue(line[dataIndex]);
}

function hazardInspectionLineImagePreview(
  line: HazardInspectionLine,
  dataIndex: string,
) {
  const lineValue = line[dataIndex];
  if (typeof lineValue === 'string' && lineValue && lineValue !== '未上传') {
    return resolveAttachmentPreviewUrl(lineValue);
  }
  return '';
}

function downloadHazardInspectionLines() {
  if (!canExportRecords.value) {
    return;
  }
  const csv = exportHazardInspectionLinesCsv(
    filteredHazardInspectionLines.value,
    visibleHazardInspectionLineColumns.value.map(hazardInspectionDetailColumn),
  );
  const blob = new Blob([`\uFEFF${csv}`], {
    type: 'text/csv;charset=utf-8',
  });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `${
    isSafetyCheckDetail.value ? '安全检查' : '隐患整改'
  }_隐患明细.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

function safetyCheckLineEditableDataIndexes() {
  return Array.from(
    new Set([
      ...safetyCheckDetailLineColumns.map((column) => column.dataIndex),
    ]),
  ).filter((dataIndex) => isEditableSafetyCheckLineField(dataIndex));
}

function isEditableSafetyCheckLineField(dataIndex: string) {
  if (
    isSafetyCheckDetail.value &&
    [
      'rectificationClosedAt',
      'rectificationOrderNo',
      'rectificationStatusLabel',
    ].includes(dataIndex)
  ) {
    return false;
  }
  return ![
    'company',
    'department',
    'sequence',
    'team',
    'updatedAt',
  ].includes(dataIndex);
}

function hazardInspectionDetailColumn(
  column: HazardInspectionDetailColumn,
): ThreeCheckTableColumn {
  return {
    dataIndex: column.dataIndex,
    title: column.title,
    width: column.width,
  };
}

function openVideoPreview(url?: string) {
  const previewUrl = resolveAttachmentPreviewUrl(url);
  if (!previewUrl) {
    return;
  }
  videoPreviewUrl.value = previewUrl;
  videoPreviewOpen.value = true;
}

function closeVideoPreview() {
  videoPreviewOpen.value = false;
  videoPreviewUrl.value = '';
}

function isColumnResizeEdge(event: MouseEvent) {
  const target = event.currentTarget as HTMLElement | null;
  if (!target) {
    return false;
  }
  const { right } = target.getBoundingClientRect();
  const distanceToRightEdge = right - event.clientX;
  return (
    distanceToRightEdge >= 0 &&
    distanceToRightEdge <= threeCheckTableColumnResizeConfig.triggerEdgeWidth
  );
}

function cleanupColumnResizeListeners() {
  window.removeEventListener('pointermove', handleColumnResizeMove);
  window.removeEventListener('pointerup', handleColumnResizeEnd);
  window.removeEventListener('pointercancel', handleColumnResizeEnd);
  document.body.style.cursor = '';
  document.body.style.userSelect = '';
}

function handleColumnResizeMove(event: PointerEvent) {
  updateColumnResize(event.clientX);
}

function handleColumnResizeEnd() {
  endColumnResize();
  cleanupColumnResizeListeners();
}

function handleColumnResizeStart(
  event: MouseEvent,
  column: ThreeCheckTableColumn,
) {
  if (event.button !== 0 || !isColumnResizeEdge(event)) {
    return;
  }
  event.preventDefault();
  event.stopPropagation();
  beginColumnResize(
    column.dataIndex,
    event.clientX,
    columnWidths[column.dataIndex] ?? column.width,
  );
  document.body.style.cursor = 'col-resize';
  document.body.style.userSelect = 'none';
  window.addEventListener('pointermove', handleColumnResizeMove);
  window.addEventListener('pointerup', handleColumnResizeEnd);
  window.addEventListener('pointercancel', handleColumnResizeEnd);
}

function updateTableBottomRailFromPointer(clientX: number, clientY: number) {
  const target = tablePanelRef.value;
  if (!target) {
    tableBottomRailActive.value = false;
    return;
  }
  const scrollBody =
    target.querySelector<HTMLElement>('.ant-table-body') ??
    target.querySelector<HTMLElement>('.ant-table-content') ??
    target;
  if (!scrollBody) {
    tableBottomRailActive.value = false;
    return;
  }
  const panelRect = target.getBoundingClientRect();
  if (
    clientX < panelRect.left ||
    clientX > panelRect.right ||
    clientY < panelRect.top ||
    clientY > panelRect.bottom
  ) {
    tableBottomRailActive.value = false;
    return;
  }
  const { bottom } = scrollBody.getBoundingClientRect();
  tableBottomRailActive.value = clientY >= bottom - 32 && clientY <= bottom + 12;
}

function handleTablePointerMove(event: MouseEvent | PointerEvent) {
  updateTableBottomRailFromPointer(event.clientX, event.clientY);
}

function handleTablePointerLeave() {
  tableBottomRailActive.value = false;
}

function handleWindowPointerMove(event: MouseEvent | PointerEvent) {
  updateTableBottomRailFromPointer(event.clientX, event.clientY);
}

function cleanupDataMapResizeListeners() {
  window.removeEventListener('pointermove', handleDataMapResizeMove);
  window.removeEventListener('pointerup', handleDataMapResizeEnd);
  window.removeEventListener('pointercancel', handleDataMapResizeEnd);
  document.body.style.cursor = '';
  document.body.style.userSelect = '';
}

function handleDataMapResizeMove(event: PointerEvent) {
  updateDataMapResize(event.clientX);
}

function handleDataMapResizeEnd() {
  endDataMapResize();
  cleanupDataMapResizeListeners();
}

function handleDataMapResizeStart(event: PointerEvent) {
  event.preventDefault();
  beginDataMapResize(event.clientX);
  document.body.style.cursor = 'col-resize';
  document.body.style.userSelect = 'none';
  window.addEventListener('pointermove', handleDataMapResizeMove);
  window.addEventListener('pointerup', handleDataMapResizeEnd);
  window.addEventListener('pointercancel', handleDataMapResizeEnd);
}

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', handleWindowPointerMove);
  window.removeEventListener('pointermove', handleWindowPointerMove);
  document.removeEventListener('visibilitychange', handleDocumentVisibilityChange);
  stopListAutoRefresh();
  cleanupColumnResizeListeners();
  cleanupDataMapResizeListeners();
});
onMounted(() => {
  window.addEventListener('mousemove', handleWindowPointerMove);
  window.addEventListener('pointermove', handleWindowPointerMove);
  document.addEventListener('visibilitychange', handleDocumentVisibilityChange);
  void loadOrganizations();
  void loadUsers();
  void loadMeetings();
  startListAutoRefresh();
});
watch(
  () => route.name,
  () => {
    currentDetail.value = undefined;
    currentThreeCheckDetail.value = undefined;
    currentDocumentFlow.value = undefined;
    currentChangeHistory.value = undefined;
    currentLocalDetail.value = undefined;
    clearSelectedRows();
    changeHistoryOpen.value = false;
    detailEditing.value = false;
    seedThreeCheckDetailEditForm();
    detailLoading.value = false;
    detailOpen.value = false;
    createOpen.value = false;
    void loadMeetings();
  },
);
</script>

<template>
  <Page auto-content-height content-class="pingan-page">
    <div
      :style="dataMapGridStyle"
      :class="[
        'pingan-shell',
        {
          'pingan-shell--map-collapsed': isDataMapCollapsed,
          'pingan-shell--no-map': !showDataMap,
        },
      ]"
    >
      <aside
        v-if="showDataMap"
        :class="[
          'data-map',
          {
            'data-map--collapsed': isDataMapCollapsed,
            'data-map--resizing': isDataMapResizing,
          },
        ]"
      >
        <div class="data-map__header">
          <div>
            <div class="data-map__title">数据地图</div>
            <div class="data-map__subtitle">组织层级快速定位</div>
          </div>
          <Button
            :aria-expanded="!isDataMapCollapsed"
            :aria-label="dataMapToggleLabel"
            class="data-map__collapse"
            size="small"
            type="text"
            @click="toggleDataMap"
          >
            {{ dataMapToggleLabel }}
          </Button>
        </div>

        <div v-if="!isDataMapCollapsed" class="data-map__body">
          <Input
            v-model:value="treeKeyword"
            allow-clear
            placeholder="输入关键字搜索"
          />

          <VbenScrollbar
            class="data-map__tree-scroll"
            shadow
            shadow-border
          >
            <Tree
              v-model:expanded-keys="expandedOrganizationKeys"
              v-model:selected-keys="selectedOrganizationKeys"
              :item-height="dataMapTreeContinuousScrollConfig.itemHeight"
              :tree-data="filteredTree"
              :virtual="dataMapTreeContinuousScrollConfig.virtual"
              block-node
              class="data-map__tree"
              @select="handleDataMapSelect"
            />
          </VbenScrollbar>
        </div>

        <button
          v-if="!isDataMapCollapsed"
          aria-label="拖拽调整数据地图宽度"
          class="data-map__resize-handle"
          type="button"
          @pointerdown="handleDataMapResizeStart"
        ></button>
      </aside>

      <section class="workbench">
        <div class="workbench__bar">
          <div
            v-if="isOrganizationFilterVisible('company')"
            class="filter-item filter-item--company"
          >
            <span>公司</span>
            <Select
              v-model:value="filters.companyId"
              :allow-clear="!fixedCompanyName && !isOrganizationFieldLocked('company')"
              :disabled="Boolean(fixedCompanyName) || isOrganizationFieldLocked('company')"
              :options="activeFilterCompanySelectOptions"
              class="w-full"
              option-filter-prop="label"
              placeholder="点击选择"
              show-search
              @change="handleFilterCompanyChange"
            />
          </div>
          <div v-if="isOrganizationFilterVisible('department')" class="filter-item">
            <span>{{ departmentFilterLabel }}</span>
            <Select
              v-model:value="filters.departmentId"
              :allow-clear="!isOrganizationFieldLocked('department')"
              :disabled="!filters.companyId || isOrganizationFieldLocked('department')"
              :options="filterDepartmentSelectOptions"
              class="w-full"
              option-filter-prop="label"
              placeholder="点击选择"
              show-search
              @change="handleFilterDepartmentChange"
            />
          </div>
          <div
            v-if="showTeamFilter && isOrganizationFilterVisible('team')"
            class="filter-item"
          >
            <span>班组</span>
            <Select
              v-model:value="filters.teamId"
              :allow-clear="!isOrganizationFieldLocked('team')"
              :disabled="!filters.departmentId || isOrganizationFieldLocked('team')"
              :options="filterTeamSelectOptions"
              class="w-full"
              option-filter-prop="label"
              placeholder="点击选择"
              show-search
              @change="handleFilterTeamChange"
            />
          </div>
          <div v-if="showPointsReasonFilter" class="filter-item">
            <span>积分变动原因</span>
            <Select
              v-model:value="filters.pointsReason"
              allow-clear
              :options="pointsReasonFilterOptions"
              class="w-full"
              option-filter-prop="label"
              placeholder="点击选择"
              show-search
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
            v-if="showStatusFilter"
            :disabled="!canUseWorkbench"
            :hide-reset="isPointsFlowModule"
            :model-value="filters.status"
            :options="activeStatusOptions"
            @reset="resetFilters"
            @search="loadMeetings"
            @update:model-value="setWorkbenchStatusFilter"
          />
          <Space v-else>
            <Button :disabled="!canUseWorkbench" type="primary" @click="loadMeetings">
              搜索
            </Button>
            <Button
              v-if="!isPointsFlowModule"
              :disabled="!canUseWorkbench"
              @click="resetFilters"
            >
              重置
            </Button>
          </Space>
        </div>

        <div
          ref="tablePanelRef"
          class="table-panel"
          @mousemove="handleTablePointerMove"
          @pointermove="handleTablePointerMove"
          @pointerleave="handleTablePointerLeave"
        >
          <div class="table-panel__toolbar">
            <div>
              <div class="table-panel__title">{{ workbenchText.title }}</div>
              <div class="table-panel__summary">
                共 {{ total }} 条记录，{{ tableDataSourceText }}
              </div>
            </div>
            <div class="table-panel__actions">
              <span
                v-show="isThreeCheckRecordModule && operationPolicy.batchDelete"
                class="table-panel__selection"
              >
                已选 {{ selectedRowKeys.length }} 条
              </span>
              <Button
                v-show="isThreeCheckRecordModule && operationPolicy.batchDelete"
                :disabled="!canBatchDelete"
                danger
                @click="handleBatchAction('DELETE')"
              >
                批量删除
              </Button>
              <Space>
                <Button :disabled="!moduleApiReady || !canExportRecords">
                  下载数据
                </Button>
                <Button
                  :disabled="!canCreateRecord"
                  type="primary"
                  @click="openCreateModal"
                >
                  {{ workbenchText.addButtonText }}
                </Button>
              </Space>
            </div>
          </div>

          <Table
            :columns="columns"
            :data-source="rows"
            :locale="{ emptyText: emptyDescription }"
            :loading="loading"
            :pagination="{
              pageSize: 20,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 条记录`,
            }"
            :row-selection="tableRowSelection"
            row-key="id"
            size="small"
            bordered
            :class="meetingTableClass"
            :scroll="threeCheckTableScrollConfig"
            :table-layout="'fixed'"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="isAttachmentColumn(column)">
                <div class="attachment-table-cell">
                  <Image
                    v-if="
                      attachmentRenderKind(record, column) === 'IMAGE' &&
                      attachmentPreviewFor(record, column)
                    "
                    :height="38"
                    :src="attachmentPreviewFor(record, column)"
                    :width="64"
                    class="check-preview-image"
                  />
                  <button
                    v-else-if="
                      attachmentRenderKind(record, column) === 'VIDEO' &&
                      attachmentPreviewFor(record, column)
                    "
                    class="video-preview-trigger"
                    type="button"
                    @click="openVideoPreview(attachmentPreviewFor(record, column))"
                  >
                    <video
                      :src="attachmentPreviewFor(record, column)"
                      muted
                      playsinline
                      preload="metadata"
                    ></video>
                    <span>{{ attachmentCellLabel(record, column) }}</span>
                  </button>
                  <span v-else :class="attachmentPillClass(record, column)">
                    {{ attachmentCellLabel(record, column) }}
                  </span>
                </div>
              </template>
              <template v-else-if="isStatusBoxColumn(column)">
                <span :class="columnStatusBoxClass(record, column)">
                  {{ columnStatusBoxLabel(record, column) }}
                </span>
              </template>
              <template v-else-if="column.dataIndex === 'actions'">
                <Space :size="6">
                  <Button
                    v-if="operationPolicy.view"
                    :disabled="!canUseWorkbench"
                    size="small"
                    type="primary"
                    @click="openDetail(record)"
                  >
                    明细
                  </Button>
                  <template v-if="isQuickShotModule">
                    <Button
                      v-for="action in quickShotWorkflowActions(record)"
                      :key="action.action"
                      :disabled="!canUseWorkbench"
                      size="small"
                      :type="action.type"
                      @click="
                        openQuickShotWorkflowAction(record, action.action)
                      "
                    >
                      {{ action.label }}
                    </Button>
                  </template>
                  <Button
                    v-if="operationPolicy.submit"
                    :disabled="
                      !canUseWorkbench || !record.canSubmit || !canSubmitRecord
                    "
                    size="small"
                    type="primary"
                    @click="handleSubmit(record)"
                  >
                    提交
                  </Button>
                  <Button
                    v-if="operationPolicy.withdraw && canWithdrawRecord"
                    :disabled="!canUseWorkbench || !record.canWithdraw"
                    size="small"
                    @click="handleWithdraw(record)"
                  >
                    撤回
                  </Button>
                  <Button
                    v-if="operationPolicy.remind && canRemindRecord"
                    :disabled="!canUseWorkbench || !record.canRemind"
                    size="small"
                    @click="handleRemind(record)"
                  >
                    催办
                  </Button>
                  <Button
                    v-if="isOneShiftInspectionModule"
                    :disabled="
                      !canCreateRectificationOrder ||
                      !record.canCreateRectificationOrder
                    "
                    size="small"
                    @click="handleOpenRectificationOrder(record)"
                  >
                    开整改单
                  </Button>
                  <Button
                    v-if="operationPolicy.delete"
                    :disabled="
                      !canDeleteRecord ||
                      (!isThreeCheckRecordModule && !record.canDelete)
                    "
                    danger
                    size="small"
                    @click="handleDelete(record)"
                  >
                    删除
                  </Button>
                </Space>
              </template>
            </template>
            <template #emptyText>
              <Empty :description="emptyDescription" />
            </template>
          </Table>
        </div>
      </section>
    </div>

    <Modal
      v-model:open="createOpen"
      :confirm-loading="createSaving"
      :title="workbenchText.createTitle"
      @ok="handleCreateMeeting"
    >
      <div v-if="usesPreShiftMeetingForm" class="meeting-form">
        <label>
          <span>公司</span>
          <Select
            v-model:value="createForm.companyId"
            :disabled="companySelectOptions.length === 0 || isOrganizationFieldLocked('company')"
            :options="companySelectOptions"
            option-filter-prop="label"
            placeholder="选择公司"
            show-search
            @change="handleCompanyChange"
          />
        </label>
        <label>
          <span>部门</span>
          <Select
            v-model:value="departmentSelectValue"
            :disabled="departmentSelectOptions.length === 0 || isOrganizationFieldLocked('department')"
            :options="departmentSelectOptions"
            option-filter-prop="label"
            placeholder="暂无部门名单"
            show-search
            @change="handleDepartmentChange"
          />
        </label>
        <label>
          <span>班组</span>
          <Select
            v-model:value="teamSelectValue"
            :disabled="teamSelectOptions.length === 0 || isOrganizationFieldLocked('team')"
            :options="teamSelectOptions"
            option-filter-prop="label"
            placeholder="暂无班组名单"
            show-search
            @change="handleTeamChange"
          />
        </label>
        <label>
          <span>负责人</span>
          <Select
            v-model:value="createForm.ownerUserId"
            :options="ownerSelectOptions"
            option-filter-prop="label"
            show-search
          />
        </label>
        <label>
          <span>开会日期</span>
          <DatePicker
            v-model:value="createForm.meetingDate"
            value-format="YYYY-MM-DD"
          />
        </label>
        <label>
          <span>会议内容</span>
          <Input
            v-model:value="createForm.meetingContent"
            :placeholder="workbenchText.formContentPlaceholder"
          />
        </label>
        <label>
          <span>参会人</span>
          <Select
            v-model:value="createForm.attendeeNames"
            :options="attendeeSelectOptions"
            mode="multiple"
            option-filter-prop="label"
            show-search
          />
        </label>
      </div>
      <div v-else class="meeting-form">
        <label v-for="column in localCreateColumns" :key="column.dataIndex">
          <span>{{ column.title }}</span>
          <Select
            v-if="column.dataIndex === 'company'"
            v-model:value="createForm.companyId"
            :disabled="activeCompanySelectOptions.length === 0 || isOrganizationFieldLocked('company')"
            :options="activeCompanySelectOptions"
            option-filter-prop="label"
            placeholder="选择公司"
            show-search
            @change="handleCompanyChange"
          />
          <Select
            v-else-if="column.dataIndex === 'department'"
            v-model:value="departmentSelectValue"
            :disabled="departmentSelectOptions.length === 0 || isOrganizationFieldLocked('department')"
            :options="departmentSelectOptions"
            option-filter-prop="label"
            placeholder="暂无部门名单"
            show-search
            @change="handleDepartmentChange"
          />
          <Select
            v-else-if="column.dataIndex === 'team'"
            v-model:value="teamSelectValue"
            :disabled="teamSelectOptions.length === 0 || isOrganizationFieldLocked('team')"
            :options="teamSelectOptions"
            option-filter-prop="label"
            placeholder="暂无班组名单"
            show-search
            @change="handleTeamChange"
          />
          <Select
            v-else-if="isPointsFlowModule && column.dataIndex === 'user'"
            v-model:value="createForm.ownerUserId"
            :options="ownerSelectOptions"
            option-filter-prop="label"
            placeholder="选择用户"
            show-search
            @change="handlePointsFlowUserChange"
          />
          <DatePicker
            v-else-if="isLocalDateTimeColumn(column)"
            v-model:value="localCreateForm[column.dataIndex]"
            format="YYYY-MM-DD HH:mm"
            show-time
            value-format="YYYY-MM-DD HH:mm"
          />
          <DatePicker
            v-else-if="isLocalDateColumn(column)"
            v-model:value="localCreateForm[column.dataIndex]"
            value-format="YYYY-MM-DD"
          />
          <div
            v-else-if="isCreateAttachmentField(column)"
            class="create-attachment-control"
          >
            <input
              :accept="createAttachmentAccept(column)"
              :disabled="!canUploadAttachment"
              type="file"
              @change="handleCreateAttachmentChange(column, $event)"
            />
            <span>{{ localCreateForm[column.dataIndex] || '未上传' }}</span>
          </div>
          <Select
            v-else-if="isLocalSelectColumn(column)"
            v-model:value="localCreateForm[column.dataIndex]"
            :mode="isLocalMultiSelectColumn(column) ? 'multiple' : undefined"
            :options="localFieldOptions(column)"
            option-filter-prop="label"
            show-search
          />
          <Input
            v-else
            v-model:value="localCreateForm[column.dataIndex]"
            :placeholder="`填写${column.title}`"
          />
        </label>
      </div>
    </Modal>

    <Modal
      v-model:open="quickShotWorkflowActionOpen"
      :confirm-loading="quickShotWorkflowActionSaving"
      :title="quickShotWorkflowActionLabel(quickShotWorkflowAction)"
      width="560px"
      @ok="handleQuickShotWorkflowAction"
    >
      <div class="meeting-form quick-shot-workflow-form">
        <label
          v-for="field in quickShotWorkflowAction
            ? quickShotWorkflowFieldGroups[quickShotWorkflowAction]
            : []"
          :key="field.dataIndex"
        >
          <span>{{ field.label }}<em v-if="field.required">*</em></span>
          <div
            v-if="field.dataIndex === 'afterRectificationPhoto'"
            class="quick-shot-workflow-upload"
          >
            <Image
              v-if="quickShotWorkflowActionForm[field.dataIndex]"
              :src="
                resolveAttachmentPreviewUrl(
                  quickShotWorkflowActionForm[field.dataIndex],
                )
              "
              class="quick-shot-workflow-upload-preview"
            />
            <label class="quick-shot-workflow-upload-card">
              <IconifyIcon icon="lucide:image-plus" />
              <span>
                {{
                  quickShotWorkflowPhotoUploading ? '上传中...' : '上传整改后照片'
                }}
              </span>
              <input
                accept="image/*"
                :disabled="quickShotWorkflowPhotoUploading"
                type="file"
                @change="handleQuickShotWorkflowPhotoChange(field, $event)"
              />
            </label>
            <Input
              v-model:value="quickShotWorkflowActionForm[field.dataIndex]"
              readonly
              :placeholder="`上传${field.label}`"
            />
          </div>
          <Select
            v-else-if="isQuickShotWorkflowDepartmentField(field.dataIndex)"
            v-model:value="quickShotWorkflowActionForm[field.dataIndex]"
            allow-clear
            :options="quickShotWorkflowDepartmentOptions"
            option-filter-prop="label"
            :placeholder="`选择${field.label}`"
            show-search
          />
          <DatePicker
            v-else-if="isQuickShotWorkflowDateField(field.dataIndex)"
            v-model:value="quickShotWorkflowActionForm[field.dataIndex]"
            class="quick-shot-workflow-date-picker"
            :placeholder="`选择${field.label}`"
            value-format="YYYY-MM-DD"
          />
          <Input.TextArea
            v-if="
              field.dataIndex !== 'afterRectificationPhoto' &&
              !isQuickShotWorkflowDepartmentField(field.dataIndex) &&
              !isQuickShotWorkflowDateField(field.dataIndex) &&
              [
                'acceptanceRemark',
                'acceptanceRequestRemark',
                'rectificationDescription',
                'rectificationRequirement',
                'rejectReason',
                'reviewRemark',
              ].includes(field.dataIndex)
            "
            v-model:value="quickShotWorkflowActionForm[field.dataIndex]"
            :auto-size="{ maxRows: 4, minRows: 2 }"
            :placeholder="`填写${field.label}`"
          />
          <Input
            v-else-if="
              field.dataIndex !== 'afterRectificationPhoto' &&
              !isQuickShotWorkflowDepartmentField(field.dataIndex) &&
              !isQuickShotWorkflowDateField(field.dataIndex)
            "
            v-model:value="quickShotWorkflowActionForm[field.dataIndex]"
            :placeholder="`填写${field.label}`"
          />
        </label>
      </div>
    </Modal>

    <Modal
      v-model:open="detailOpen"
      :centered="detailModalCentered"
      :footer="null"
      :title="workbenchText.detailTitle"
      :width="detailModalWidth"
      :wrap-class-name="detailModalWrapClass"
    >
      <div v-if="detailLoading" class="meeting-detail">加载中...</div>
      <div v-else-if="currentDetail" class="meeting-detail">
        <dl>
          <dt>会议编号</dt>
          <dd>{{ currentDetail.meetingNo }}</dd>
          <dt>组织班组</dt>
          <dd>
            {{ currentDetail.company }} / {{ currentDetail.department }} /
            {{ currentDetail.team }}
          </dd>
          <dt>负责人</dt>
          <dd>{{ currentDetail.owner }}</dd>
          <dt>开会日期</dt>
          <dd>{{ currentDetail.meetingDate }}</dd>
          <dt>来源</dt>
          <dd>{{ currentDetail.sourceChannel }}</dd>
          <dt>会议内容</dt>
          <dd>{{ currentDetail.meetingContent || '未填写' }}</dd>
          <dt>参会人</dt>
          <dd>{{ currentDetail.attendees.join('、') || '未填写' }}</dd>
          <dt>状态</dt>
          <dd>
            <span :class="statusBoxClass(getStatusLabel(currentDetail.status))">
              {{ statusBoxLabel(getStatusLabel(currentDetail.status)) }}
            </span>
          </dd>
          <dt>打卡附件</dt>
          <dd>
            <div class="attachment-actions">
              <label>
                上传图片
                <input
                  :disabled="!canUploadAttachment"
                  accept="image/*"
                  type="file"
                  @change="handleAttachmentChange('IMAGE', $event)"
                />
              </label>
              <label>
                上传视频
                <input
                  :disabled="!canUploadAttachment"
                  accept="video/*"
                  type="file"
                  @change="handleAttachmentChange('VIDEO', $event)"
                />
              </label>
            </div>
            <div
              v-if="currentDetail.attachments.length"
              class="attachment-grid"
            >
              <div
                v-for="attachment in currentDetail.attachments"
                :key="attachment.id"
                class="attachment-preview"
              >
                <button
                  aria-label="删除附件"
                  class="attachment-delete-button"
                  type="button"
                  @click.stop="handleDeleteAttachment(attachment)"
                >
                  <IconifyIcon icon="lucide:x" />
                </button>
                <Image
                  v-if="attachment.fileKind === 'IMAGE'"
                  :src="attachmentPreviewUrl(attachment)"
                  class="attachment-image"
                />
                <button
                  v-else
                  class="attachment-video"
                  type="button"
                  @click="openVideoPreview(attachmentPreviewUrl(attachment))"
                >
                  <video
                    :src="attachmentPreviewUrl(attachment)"
                    muted
                    playsinline
                    preload="metadata"
                  ></video>
                  <span>播放视频</span>
                </button>
                <div class="attachment-name">{{ attachment.originalName }}</div>
              </div>
            </div>
            <div v-else class="attachment-empty">暂无附件</div>
          </dd>
        </dl>
      </div>
      <div
        v-else-if="currentThreeCheckDetail"
        :class="[
          'meeting-detail',
          {
            'meeting-detail--safety-check':
              moduleRuntime.routeName === 'PinganSafetyCheck',
          },
        ]"
      >
        <div v-if="!isPointsFlowModule" class="detail-action-bar">
          <Space :size="8" class="detail-action-buttons" wrap>
            <Button
              class="detail-action-button"
              size="small"
              @click="handleDetailAction('refresh')"
            >
              <IconifyIcon
                :icon="detailActionIcon('refresh')"
                class="detail-action-icon"
              />
              {{ detailActionLabel('refresh') }}
            </Button>
            <Button
              class="detail-action-button"
              size="small"
              @click="handleDetailAction('print')"
            >
              <IconifyIcon
                :icon="detailActionIcon('print')"
                class="detail-action-icon"
              />
              {{ detailActionLabel('print') }}
            </Button>
            <Button
              :type="
                activeDetailPanel === 'documentFlow' ? 'primary' : 'default'
              "
              class="detail-action-button"
              size="small"
              @click="handleDetailAction('documentFlow')"
            >
              <IconifyIcon
                :icon="detailActionIcon('documentFlow')"
                class="detail-action-icon"
              />
              {{ detailActionLabel('documentFlow') }}
            </Button>
            <Button
              :type="activeDetailPanel === 'attachment' ? 'primary' : 'default'"
              class="detail-action-button"
              size="small"
              @click="handleDetailAction('attachment')"
            >
              <IconifyIcon
                :icon="detailActionIcon('attachment')"
                class="detail-action-icon"
              />
              {{ detailActionLabel('attachment') }}
            </Button>
            <Button
              :type="
                activeDetailPanel === 'changeHistory' ? 'primary' : 'default'
              "
              class="detail-action-button"
              size="small"
              @click="handleDetailAction('changeHistory')"
            >
              <IconifyIcon
                :icon="detailActionIcon('changeHistory')"
                class="detail-action-icon"
              />
              {{ detailActionLabel('changeHistory') }}
            </Button>
            <Button
              :disabled="!canEditCurrentThreeCheckDetail || detailEditing"
              class="detail-action-button"
              size="small"
              type="primary"
              @click="handleDetailAction('edit')"
            >
              <IconifyIcon
                :icon="detailActionIcon('edit')"
                class="detail-action-icon"
              />
              {{ detailActionLabel('edit') }}
            </Button>
          </Space>
          <Space v-if="detailEditing" :size="8">
            <Button
              :loading="detailSaving"
              size="small"
              type="primary"
              @click="saveThreeCheckDetailEdit"
            >
              保存
            </Button>
            <Button
              :disabled="detailSaving"
              size="small"
              @click="cancelThreeCheckDetailEdit"
            >
              取消
            </Button>
          </Space>
          <span v-else-if="!canEditCurrentThreeCheckDetail">
            当前状态不可修改
          </span>
          <span v-else>撤回后可修改明细字段</span>
        </div>
        <section
          v-if="!isPointsFlowModule && activeDetailPanel === 'documentFlow'"
          class="detail-panel"
        >
          <h3>单据流</h3>
          <ol class="detail-flow-list">
            <li
              v-for="item in detailDocumentFlowItems"
              :key="item.label"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <em>{{ item.meta }}</em>
            </li>
          </ol>
        </section>
        <section
          v-else-if="!isPointsFlowModule && activeDetailPanel === 'changeHistory'"
          class="detail-panel"
        >
          <h3>变更历史</h3>
          <dl v-if="detailChangeHistoryItems.length" class="detail-history-list">
            <template
              v-for="item in detailChangeHistoryItems"
              :key="item.label"
            >
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value }}</dd>
            </template>
          </dl>
          <div v-else class="detail-panel-empty">暂无变更历史</div>
        </section>
        <section
          v-else-if="!isPointsFlowModule && activeDetailPanel === 'attachment'"
          class="detail-panel detail-attachment-panel"
        >
          <h3>{{ detailAttachmentLabel }}</h3>
          <ul
            v-if="detailPreviewAttachments.length"
            class="detail-attachment-file-list"
          >
            <li
              v-for="attachment in detailPreviewAttachments"
              :key="attachment.id"
              class="detail-attachment-file-item"
            >
              <span class="detail-attachment-file-kind">
                {{ attachmentFileKindLabel(attachment.fileKind) }}
              </span>
              <span class="detail-attachment-file-name">
                {{ attachment.originalName }}
              </span>
            </li>
          </ul>
          <div v-else class="detail-panel-empty">暂无附件</div>
        </section>
        <div
          v-else-if="
            moduleRuntime.routeName !== 'PinganSafetyCheck' &&
            !isPointsFlowModule
          "
          class="detail-meta-strip"
        >
          <div>
            <span>记录编号</span>
            <strong>{{ currentThreeCheckDetail.recordNo }}</strong>
          </div>
          <div>
            <span>来源</span>
            <strong>{{ currentThreeCheckDetail.sourceChannel }}</strong>
          </div>
          <div>
            <span>催办次数</span>
            <strong>{{ currentThreeCheckDetail.reminderCount || 0 }}</strong>
          </div>
        </div>

        <div class="detail-field-sections">
          <section
            v-for="group in detailFieldGroups"
            :key="group.key"
            :class="[
              'detail-field-section',
              `detail-field-section--${group.key}`,
            ]"
          >
            <h3>
              <span>{{ group.title }}</span>
              <Button
                v-if="isSafetyCheckDetail && group.key === 'safetyCheckInfo'"
                :disabled="detailEditing || !canEditCurrentThreeCheckDetail"
                size="small"
                @click="handleDetailAction('edit')"
              >
                <IconifyIcon icon="lucide:pencil" />
                编辑
              </Button>
            </h3>
            <div
              v-if="group.key === 'default'"
              class="detail-field-grid detail-field-grid--balanced"
            >
              <dl
                v-for="(columnGroup, columnGroupIndex) in defaultDetailColumnGroups(
                  group.columns,
                )"
                :key="columnGroupIndex"
                class="detail-field-column"
              >
                <template
                  v-for="column in columnGroup"
                  :key="column.dataIndex"
                >
                  <div
                    :class="[
                      'detail-field',
                      `detail-field--${column.dataIndex}`,
                      {
                        'detail-field--wide': [
                          'inspectionType',
                          'inspectionContent',
                          'remarks',
                        ].includes(column.dataIndex),
                      },
                    ]"
                  >
                    <dt>{{ column.title }}</dt>
                    <dd
                      v-if="
                        detailEditing &&
                        isEditableThreeCheckDetailColumn(column) &&
                        !isDetailAttachmentColumn(column)
                      "
                    >
                      <DatePicker
                        v-if="isLocalDateTimeColumn(column)"
                        v-model:value="detailEditForm[column.dataIndex]"
                        class="detail-edit-control"
                        format="YYYY-MM-DD HH:mm"
                        show-time
                        value-format="YYYY-MM-DD HH:mm"
                      />
                      <DatePicker
                        v-else-if="isLocalDateColumn(column)"
                        v-model:value="detailEditForm[column.dataIndex]"
                        class="detail-edit-control"
                        value-format="YYYY-MM-DD"
                      />
                      <Select
                        v-else-if="isLocalSelectColumn(column)"
                        v-model:value="detailEditForm[column.dataIndex]"
                        :mode="
                          isLocalMultiSelectColumn(column)
                            ? 'multiple'
                            : undefined
                        "
                        :options="localFieldOptions(column)"
                        class="detail-edit-control"
                        option-filter-prop="label"
                        show-search
                      />
                      <Input.TextArea
                        v-else-if="isLongTextDetailColumn(column)"
                        v-model:value="detailEditForm[column.dataIndex]"
                        :auto-size="{ maxRows: 6, minRows: 2 }"
                        class="detail-edit-control detail-edit-textarea"
                        :placeholder="`填写${column.title}`"
                      />
                      <Input
                        v-else
                        v-model:value="detailEditForm[column.dataIndex]"
                        class="detail-edit-control"
                        :placeholder="`填写${column.title}`"
                      />
                    </dd>
                    <dd v-else-if="isStatusBoxColumn(column)">
                      <span
                        :class="
                          statusBoxClass(
                            column.dataIndex === 'status'
                              ? getRecordStatusLabel(currentThreeCheckDetail)
                              : threeCheckDetailValue(column),
                          )
                        "
                      >
                        {{
                          statusBoxLabel(
                            column.dataIndex === 'status'
                              ? getRecordStatusLabel(currentThreeCheckDetail)
                              : threeCheckDetailValue(column),
                          )
                        }}
                      </span>
                    </dd>
                    <dd v-else-if="isDetailAttachmentColumn(column)">
                      <div class="detail-attachment-field">
                        <button
                          v-if="
                            detailAttachmentFieldKind(column) === 'VIDEO' &&
                            detailAttachmentFieldPreview(column)
                          "
                          class="detail-attachment-field__video"
                          type="button"
                          @click="
                            openVideoPreview(detailAttachmentFieldPreview(column))
                          "
                        >
                          <video
                            :src="detailAttachmentFieldPreview(column)"
                            muted
                            playsinline
                            preload="metadata"
                          ></video>
                          <span>播放视频</span>
                        </button>
                        <Image
                          v-else-if="detailAttachmentFieldPreview(column)"
                          :src="detailAttachmentFieldPreview(column)"
                          class="detail-attachment-field__preview"
                        />
                        <span v-else class="detail-attachment-field__empty">
                          未上传
                        </span>
                        <label class="detail-attachment-field__upload">
                          {{ detailAttachmentFieldUploadText(column) }}
                          <input
                            :accept="detailAttachmentFieldAccept(column)"
                            :disabled="!canUploadAttachment"
                            type="file"
                            @change="
                              handleDetailAttachmentFieldChange(column, $event)
                            "
                          />
                        </label>
                      </div>
                    </dd>
                    <dd v-else>
                      {{ threeCheckDetailValue(column) }}
                    </dd>
                  </div>
                </template>
              </dl>
            </div>
            <dl v-else class="detail-field-grid">
              <template
                v-for="column in group.columns"
                :key="column.dataIndex"
              >
                <div
                  :class="[
                    'detail-field',
                    `detail-field--${column.dataIndex}`,
                    {
                      'detail-field--wide': [
                        'inspectionType',
                        'inspectionContent',
                        'remarks',
                      ].includes(column.dataIndex),
                    },
                  ]"
                >
                  <dt>{{ column.title }}</dt>
                  <dd
                    v-if="
                      detailEditing &&
                      isEditableThreeCheckDetailColumn(column) &&
                      !isDetailAttachmentColumn(column)
                    "
                  >
                    <DatePicker
                      v-if="isLocalDateTimeColumn(column)"
                      v-model:value="detailEditForm[column.dataIndex]"
                      class="detail-edit-control"
                      format="YYYY-MM-DD HH:mm"
                      show-time
                      value-format="YYYY-MM-DD HH:mm"
                    />
                    <DatePicker
                      v-else-if="isLocalDateColumn(column)"
                      v-model:value="detailEditForm[column.dataIndex]"
                      class="detail-edit-control"
                      value-format="YYYY-MM-DD"
                    />
                    <Select
                      v-else-if="isLocalSelectColumn(column)"
                      v-model:value="detailEditForm[column.dataIndex]"
                      :mode="
                        isLocalMultiSelectColumn(column)
                          ? 'multiple'
                          : undefined
                      "
                      :options="localFieldOptions(column)"
                      class="detail-edit-control"
                      option-filter-prop="label"
                      show-search
                    />
                    <Input.TextArea
                      v-else-if="isLongTextDetailColumn(column)"
                      v-model:value="detailEditForm[column.dataIndex]"
                      :auto-size="{ maxRows: 6, minRows: 2 }"
                      class="detail-edit-control detail-edit-textarea"
                      :placeholder="`填写${column.title}`"
                    />
                    <Input
                      v-else
                      v-model:value="detailEditForm[column.dataIndex]"
                      class="detail-edit-control"
                      :placeholder="`填写${column.title}`"
                    />
                  </dd>
                  <dd v-else-if="isStatusBoxColumn(column)">
                    <span
                      :class="
                        statusBoxClass(
                          column.dataIndex === 'status'
                            ? getRecordStatusLabel(currentThreeCheckDetail)
                            : threeCheckDetailValue(column),
                        )
                      "
                    >
                      {{
                        statusBoxLabel(
                          column.dataIndex === 'status'
                            ? getRecordStatusLabel(currentThreeCheckDetail)
                            : threeCheckDetailValue(column),
                        )
                      }}
                    </span>
                  </dd>
                  <dd v-else-if="isDetailAttachmentColumn(column)">
                    <div class="detail-attachment-field">
                      <button
                        v-if="
                          detailAttachmentFieldKind(column) === 'VIDEO' &&
                          detailAttachmentFieldPreview(column)
                        "
                        class="detail-attachment-field__video"
                        type="button"
                        @click="
                          openVideoPreview(detailAttachmentFieldPreview(column))
                        "
                      >
                        <video
                          :src="detailAttachmentFieldPreview(column)"
                          muted
                          playsinline
                          preload="metadata"
                        ></video>
                        <span>播放视频</span>
                      </button>
                      <Image
                        v-else-if="detailAttachmentFieldPreview(column)"
                        :src="detailAttachmentFieldPreview(column)"
                        class="detail-attachment-field__preview"
                      />
                      <span v-else class="detail-attachment-field__empty">
                        未上传
                      </span>
                      <label class="detail-attachment-field__upload">
                        {{ detailAttachmentFieldUploadText(column) }}
                        <input
                          :accept="detailAttachmentFieldAccept(column)"
                          :disabled="!canUploadAttachment"
                          type="file"
                          @change="
                            handleDetailAttachmentFieldChange(column, $event)
                          "
                        />
                      </label>
                    </div>
                  </dd>
                  <dd v-else>
                    {{ threeCheckDetailValue(column) }}
                  </dd>
                </div>
              </template>
            </dl>
            <section
              v-if="group.key === 'default' && showTeamCheckInspectionTable"
              class="team-check-items"
            >
              <div class="team-check-items__header">
                <h3>{{ teamCheckInspectionTableTitle }}</h3>
                <span v-if="teamCheckInspectionLoading">加载中...</span>
                <span v-else>{{ teamCheckInspectionLines.length }} 项</span>
              </div>
              <div class="team-check-items__table-wrap">
                <table class="team-check-items__table">
                  <colgroup>
                    <col
                      v-for="column in teamCheckInspectionColumns"
                      :key="column.dataIndex"
                      :style="{ width: `${column.width}px` }"
                    />
                  </colgroup>
                  <thead>
                    <tr>
                      <th
                        v-for="column in teamCheckInspectionColumns"
                        :key="column.dataIndex"
                      >
                        {{ column.title }}
                        <span v-if="column.dataIndex === 'checkResult'">*</span>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="(line, lineIndex) in teamCheckInspectionLines"
                      :key="`${line.libraryItemId || line.checkItem}-${lineIndex}`"
                    >
                      <td
                        v-for="column in teamCheckInspectionColumns"
                        :key="column.dataIndex"
                      >
                        <template
                          v-if="
                            detailEditing &&
                            column.dataIndex === 'checkResult'
                          "
                        >
                          <Select
                            v-model:value="line.checkResult"
                            :options="teamCheckInspectionResultOptions"
                            class="team-check-items__control"
                          />
                        </template>
                        <template
                          v-else-if="
                            detailEditing &&
                            [
                              'followUpPlan',
                              'rectificationDescription',
                              'rectificationStatus',
                              'uploadDescription',
                            ].includes(column.dataIndex)
                          "
                        >
                          <Input.TextArea
                            v-model:value="line[column.dataIndex]"
                            :auto-size="{ maxRows: 3, minRows: 1 }"
                            class="team-check-items__control team-check-items__textarea"
                            :placeholder="`填写${column.title}`"
                          />
                        </template>
                        <span v-else class="team-check-items__text">
                          {{ line[column.dataIndex] || '' }}
                        </span>
                      </td>
                    </tr>
                    <tr v-if="!teamCheckInspectionLines.length">
                      <td
                        class="team-check-items__empty"
                        :colspan="teamCheckInspectionColumns.length"
                      >
                        暂无检查项模板
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
            <div
              v-if="
                group.key === 'acceptance' ||
                (group.key === 'default' &&
                  (showBottomDetailAttachmentRow || isTeamCheckInspectionDetail))
              "
              ref="detailAttachmentSectionRef"
              :class="[
                'detail-attachment-row',
                {
                  'detail-attachment-row--bottom':
                    group.key === 'default' && showBottomDetailAttachmentRow,
                },
              ]"
            >
              <div class="detail-attachment-label">{{ detailAttachmentLabel }}</div>
              <div class="detail-attachment-content">
                <div
                  v-if="currentThreeCheckDetail.attachments.length"
                  class="attachment-grid"
                >
                  <div
                    v-for="attachment in currentThreeCheckDetail.attachments"
                    :key="attachment.id"
                    class="attachment-preview"
                  >
                    <button
                      aria-label="删除附件"
                      class="attachment-delete-button"
                      type="button"
                      @click.stop="handleDeleteAttachment(attachment)"
                    >
                      <IconifyIcon icon="lucide:x" />
                    </button>
                    <Image
                      v-if="attachment.fileKind === 'IMAGE'"
                      :src="attachmentPreviewUrl(attachment)"
                      class="attachment-image"
                    />
                    <button
                      v-else
                      class="attachment-video"
                      type="button"
                      @click="openVideoPreview(attachmentPreviewUrl(attachment))"
                    >
                      <video
                        :src="attachmentPreviewUrl(attachment)"
                        muted
                        playsinline
                        preload="metadata"
                      ></video>
                      <span>播放视频</span>
                    </button>
                    <div class="attachment-name">
                      {{ attachment.originalName }}
                    </div>
                  </div>
                </div>
                <div v-else class="attachment-empty">暂无附件</div>
                <label class="detail-attachment-upload-card">
                  <IconifyIcon icon="lucide:plus" />
                  上传附件
                  <input
                    :disabled="!canUploadAttachment"
                    accept="image/*,video/*"
                    type="file"
                    @change="handleAnyAttachmentChange"
                  />
                </label>
              </div>
            </div>
          </section>
        </div>
        <section v-if="isSafetyCheckDetail" class="safety-check-lines hazard-detail-lines">
          <div class="hazard-lines-header">
            <h3>隐患明细</h3>
            <Space :size="8">
              <Button
                :disabled="!canEditCurrentThreeCheckDetail"
                size="small"
                @click="startThreeCheckDetailEdit"
              >
                {{ detailEditing ? '已编辑' : '编辑' }}
              </Button>
              <Button
                :disabled="!canEditCurrentThreeCheckDetail"
                size="small"
                @click="addHazardInspectionLine"
              >
                新建
              </Button>
              <Button size="small">
                <IconifyIcon icon="lucide:chevron-up" />
              </Button>
            </Space>
          </div>
          <div class="hazard-lines-toolbar">
            <Input
              v-model:value="hazardInspectionLineKeyword"
              allow-clear
              class="hazard-lines-search"
              placeholder="使用 点击按钮或回车"
            />
            <Button
              :disabled="!canExportRecords"
              size="small"
              @click="downloadHazardInspectionLines"
            >
              下载数据
            </Button>
            <Button disabled size="small">
              {{ hazardInspectionLineValidationLabel }}
            </Button>
            <Button size="small" @click="openHazardInspectionView">
              自定义视图
            </Button>
          </div>
          <div class="hazard-lines-table-wrap">
            <table class="hazard-lines-table">
              <colgroup>
                <col
                  v-for="column in visibleHazardInspectionLineColumns"
                  :key="column.dataIndex"
                  :style="{ width: `${column.width}px` }"
                />
              </colgroup>
              <thead>
                <tr>
                  <th
                    v-for="column in visibleHazardInspectionLineColumns"
                    :key="column.dataIndex"
                  >
                    {{ column.title }}
                    <span v-if="column.dataIndex !== 'sequence'">*</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="line in filteredHazardInspectionLines"
                  :key="line.sequence"
                >
                  <td
                    v-for="column in visibleHazardInspectionLineColumns"
                    :key="column.dataIndex"
                  >
                    <template
                      v-if="
                        detailEditing &&
                        isEditableSafetyCheckLineField(column.dataIndex) &&
                        !isDetailAttachmentColumn(
                          hazardInspectionDetailColumn(column),
                        )
                      "
                    >
                      <Select
                        v-if="column.dataIndex === 'aiEnabled'"
                        v-model:value="line.aiEnabled"
                        :options="safetyCheckAiEnabledOptions"
                        class="hazard-lines-edit-control"
                      />
                      <Select
                        v-else-if="column.dataIndex === 'checkResult'"
                        v-model:value="line.checkResult"
                        :options="safetyCheckResultOptions"
                        class="hazard-lines-edit-control"
                      />
                      <Select
                        v-else-if="
                          isLocalSelectColumn(
                            hazardInspectionDetailColumn(column),
                          )
                        "
                        v-model:value="line[column.dataIndex]"
                        :options="
                          localFieldOptions(
                            hazardInspectionDetailColumn(column),
                          )
                        "
                        class="hazard-lines-edit-control"
                        option-filter-prop="label"
                        show-search
                      />
                      <Input.TextArea
                        v-else-if="
                          isLongTextDetailColumn(
                            hazardInspectionDetailColumn(column),
                          )
                        "
                        v-model:value="line[column.dataIndex]"
                        :auto-size="{ maxRows: 4, minRows: 2 }"
                        class="hazard-lines-edit-control hazard-lines-edit-textarea"
                        :placeholder="`填写${column.title}`"
                      />
                      <Input
                        v-else
                        v-model:value="line[column.dataIndex]"
                        class="hazard-lines-edit-control"
                        :placeholder="`填写${column.title}`"
                      />
                    </template>
                    <template
                      v-else-if="isHazardInspectionImageCell(column.dataIndex)"
                    >
                      <div class="hazard-lines-media-cell">
                        <Image
                          v-if="
                            hazardInspectionLineImagePreview(line, column.dataIndex)
                          "
                          :src="
                            hazardInspectionLineImagePreview(line, column.dataIndex)
                          "
                          class="hazard-lines-image"
                        />
                        <span v-else class="hazard-lines-empty">未上传</span>
                        <label class="hazard-lines-upload">
                          上传图片
                          <input
                            :accept="
                              detailAttachmentFieldAccept(
                                hazardInspectionDetailColumn(column),
                              )
                            "
                            :disabled="!canUploadAttachment"
                            type="file"
                            @change="
                              handleDetailAttachmentFieldChange(
                                hazardInspectionDetailColumn(column),
                                $event,
                                line,
                              )
                            "
                          />
                        </label>
                      </div>
                    </template>
                    <span
                      v-else-if="column.dataIndex === 'status'"
                      :class="
                        statusBoxClass(
                          hazardInspectionLineDisplayValue(
                            line,
                            column.dataIndex,
                          ),
                        )
                      "
                    >
                      {{
                        statusBoxLabel(
                          hazardInspectionLineDisplayValue(
                            line,
                            column.dataIndex,
                          ),
                        )
                      }}
                    </span>
                    <span v-else class="hazard-lines-text">
                      {{ hazardInspectionLineDisplayValue(line, column.dataIndex) }}
                    </span>
                  </td>
                </tr>
                <tr class="hazard-lines-summary-row">
                  <td>汇总</td>
                  <td :colspan="visibleHazardInspectionLineColumns.length - 1"></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="hazard-lines-footer">
            {{ hazardInspectionLineRecordCount }}
          </div>
        </section>
      </div>
      <div v-else-if="currentLocalDetail" class="meeting-detail">
        <dl>
          <template
            v-for="column in localDetailColumns"
            :key="column.dataIndex"
          >
            <dt>{{ column.title }}</dt>
            <dd v-if="isStatusBoxColumn(column)">
              <span :class="columnStatusBoxClass(currentLocalDetail, column)">
                {{ columnStatusBoxLabel(currentLocalDetail, column) }}
              </span>
            </dd>
            <dd v-else>
              {{ currentLocalDetail[column.dataIndex] || '未填写' }}
            </dd>
          </template>
          <dt>来源</dt>
          <dd>{{ currentLocalDetail.sourceChannel || 'PC' }}</dd>
          <dt>催办次数</dt>
          <dd>{{ currentLocalDetail.reminderCount || 0 }}</dd>
        </dl>
      </div>
    </Modal>

    <Modal
      v-model:open="changeHistoryOpen"
      :footer="null"
      :width="'min(1480px, calc(100vw - 32px))'"
      centered
      wrap-class-name="change-history-modal"
      @cancel="closeChangeHistory"
    >
      <template #title>
        <div class="change-history-title">
          <strong>变更历史</strong>
          <span>{{ changeHistorySubtitle }}</span>
        </div>
      </template>
      <div class="change-history">
        <div class="change-history-toolbar">
          <div class="change-history-toolbar__left">
            <Input
              v-model:value="changeHistoryKeyword"
              class="change-history-search"
              placeholder="搜索字段、变更内容、操作人"
            >
              <template #prefix>
                <IconifyIcon icon="lucide:search" />
              </template>
            </Input>
            <div class="change-history-filter">
              <button
                v-for="item in changeHistoryFilterOptions"
                :key="item.value"
                :class="{ 'is-active': changeHistoryFilter === item.value }"
                type="button"
                @click="changeHistoryFilter = item.value"
              >
                {{ item.label }}
              </button>
            </div>
          </div>
          <div class="change-history-toolbar__right">
            <Button
              class="document-flow-tool-button"
              :loading="changeHistoryLoading"
              @click="refreshChangeHistory"
            >
              <IconifyIcon icon="lucide:refresh-cw" />
              刷新
            </Button>
            <Button class="document-flow-tool-button" @click="printChangeHistory">
              <IconifyIcon icon="lucide:printer" />
              打印
            </Button>
            <Button type="primary" @click="closeChangeHistory">
              <IconifyIcon icon="lucide:eye" />
              查看原单
            </Button>
          </div>
        </div>

        <div class="change-history-summary">
          <div
            v-for="item in changeHistorySummary"
            :key="item.label"
            class="change-history-summary-card"
          >
            <span
              :class="[
                'change-history-summary-card__icon',
                `change-history-summary-card__icon--${item.tone}`,
              ]"
            >
              <IconifyIcon :icon="item.icon" />
            </span>
            <div>
              <span>{{ item.label }}</span>
              <strong :class="`change-history-summary-card__value--${item.tone}`">
                {{ item.value }}
              </strong>
            </div>
          </div>
        </div>

        <div class="change-history-body">
          <aside class="change-history-sidebar">
            <span class="change-history-sidebar__track"></span>
            <button
              v-for="(group, index) in changeHistoryGroups"
              :key="group.id"
              :class="[
                'change-history-group',
                { 'is-active': changeHistorySelectedGroup?.id === group.id },
                { 'is-latest': index === 0 },
              ]"
              type="button"
              @click="selectChangeHistoryGroup(group)"
            >
              <span class="change-history-group__top">
                <span class="change-history-group__version">
                  V{{ changeHistoryDisplayVersion(group) }}
                </span>
                <strong>{{ changeHistoryActionLabel(group.action) }}</strong>
              </span>
              <span class="change-history-group__meta">
                <span>
                  <IconifyIcon icon="lucide:user-round" />
                  {{ group.operator }}
                </span>
                <span>
                  <IconifyIcon icon="lucide:clock-3" />
                  {{ group.time }}
                </span>
              </span>
              <span class="change-history-group__stats">
                <span>
                  <IconifyIcon icon="lucide:file-text" />
                  字段 {{ group.fieldCount }}
                </span>
                <span>
                  <IconifyIcon icon="lucide:paperclip" />
                  附件 {{ group.attachmentCount }}
                </span>
                <span>
                  <IconifyIcon icon="lucide:git-branch" />
                  流程 {{ group.flowCount }}
                </span>
              </span>
            </button>
            <div v-if="!changeHistoryGroups.length" class="change-history-empty">
              暂无变更历史
            </div>
          </aside>

          <section class="change-history-main">
            <div class="change-history-main__header">
              <h3>字段变更明细</h3>
              <span>
                {{
                  changeHistorySelectedGroup
                    ? `V${changeHistoryDisplayVersion(changeHistorySelectedGroup)}`
                    : '当前版本'
                }}
              </span>
            </div>
            <div class="change-history-diff-table">
              <div class="change-history-diff-table__head">
                <span>字段</span>
                <span>变更前</span>
                <span>变更后</span>
                <span>类型</span>
              </div>
              <div
                v-for="item in selectedChangeHistoryItems"
                :key="item.id"
                class="change-history-diff-row"
              >
                <div class="change-history-diff-row__field">
                  <strong>{{ changeHistoryFieldLabel(item) }}</strong>
                </div>
                <div>
                  <span class="change-history-diff-value change-history-diff-value--before">
                    {{ changeHistoryValue(item.beforeValue) }}
                  </span>
                </div>
                <div>
                  <span class="change-history-diff-value change-history-diff-value--after">
                    {{ changeHistoryValue(item.afterValue) }}
                  </span>
                </div>
                <div>
                  <span :class="statusBoxClass(changeHistoryTypeLabel(item))">
                    {{ changeHistoryTypeLabel(item) }}
                  </span>
                </div>
              </div>
              <div
                v-if="!selectedChangeHistoryItems.length"
                class="change-history-empty"
              >
                暂无变更历史
              </div>
            </div>

            <div class="change-history-attachments">
              <div class="change-history-attachments__label">附件变更</div>
              <div class="change-history-attachment-list">
                <div
                  v-for="item in changeHistoryAttachmentItems"
                  :key="item.id"
                  class="change-history-attachment-card"
                >
                  <div class="change-history-attachment-card__thumb">
                    <IconifyIcon
                      :icon="item.valueType === 'VIDEO' ? 'lucide:video' : 'lucide:image'"
                    />
                  </div>
                  <div>
                    <strong>{{ item.afterValue || '附件' }}</strong>
                    <span>{{ item.valueType === 'VIDEO' ? '新增视频' : '新增图片' }}</span>
                  </div>
                </div>
                <div
                  v-if="!changeHistoryAttachmentItems.length"
                  class="change-history-empty"
                >
                  暂无附件变更
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </Modal>

    <Modal
      v-model:open="documentFlowOpen"
      :footer="null"
      :width="'min(1480px, calc(100vw - 32px))'"
      centered
      wrap-class-name="document-flow-modal"
      @cancel="closeDocumentFlow"
    >
      <template #title>
        <div class="document-flow-title">
          <strong>单据流</strong>
          <span>{{ documentFlowSubtitle }}</span>
        </div>
      </template>
      <div class="document-flow">
        <div class="document-flow-toolbar">
          <div class="document-flow-toolbar__left">
            <Input
              v-model:value="documentFlowKeyword"
              class="document-flow-search"
              placeholder="搜索单据编号、隐患描述、责任人"
            >
              <template #prefix>
                <IconifyIcon icon="lucide:search" />
              </template>
            </Input>
            <Button class="document-flow-tool-button">
              <IconifyIcon icon="lucide:funnel" />
              筛选
            </Button>
          </div>
          <div class="document-flow-toolbar__right">
            <Button
              class="document-flow-tool-button"
              :loading="documentFlowLoading"
              @click="refreshDocumentFlow"
            >
              <IconifyIcon icon="lucide:refresh-cw" />
              刷新
            </Button>
            <Button
              class="document-flow-tool-button"
              :disabled="!canExportRecords"
              @click="exportDocumentFlow"
            >
              <IconifyIcon icon="lucide:download" />
              导出
            </Button>
            <Button class="document-flow-tool-button" @click="printDocumentFlow">
              <IconifyIcon icon="lucide:printer" />
              打印
            </Button>
            <Button type="primary" @click="closeDocumentFlow">
              <IconifyIcon icon="lucide:eye" />
              查看原单
            </Button>
          </div>
        </div>

        <div class="document-flow-summary">
          <div
            v-for="item in documentFlowSummary"
            :key="item.label"
            class="document-flow-summary-card"
          >
            <IconifyIcon :icon="item.icon" />
            <div>
              <span>{{ item.label }}</span>
              <strong :class="`document-flow-summary-card__value--${item.tone}`">
                {{ item.value }}
              </strong>
            </div>
          </div>
        </div>

        <div class="document-flow-body">
          <aside class="document-flow-sidebar">
            <div class="document-flow-tabs">
              <button
                :class="{ 'is-active': documentFlowActiveTab === 'tree' }"
                type="button"
                @click="setDocumentFlowActiveTab('tree')"
              >
                单据树
              </button>
              <button
                :class="{ 'is-active': documentFlowActiveTab === 'list' }"
                type="button"
                @click="setDocumentFlowActiveTab('list')"
              >
                明细列表
              </button>
            </div>
            <div
              :class="[
                'document-flow-node-list',
                `document-flow-node-list--${documentFlowActiveTab}`,
              ]"
            >
              <template v-if="documentFlowActiveTab === 'tree'">
                <button
                  v-if="documentFlowTreeMasterNode"
                  :class="[
                    'document-flow-node',
                    'document-flow-node--master',
                    {
                      'is-active':
                        documentFlowSelectedNode?.id ===
                        documentFlowTreeMasterNode.id,
                    },
                  ]"
                  type="button"
                  @click="selectDocumentFlowNode(documentFlowTreeMasterNode)"
                >
                  <span class="document-flow-node__marker"></span>
                  <IconifyIcon icon="lucide:folder-git-2" />
                  <span class="document-flow-node__content">
                    <strong>{{ documentFlowTreeMasterNode.title }}</strong>
                    <em>{{ documentFlowTreeMasterNode.subtitle }}</em>
                  </span>
                  <span :class="statusBoxClass(documentFlowTreeMasterNode.status)">
                    {{ statusBoxLabel(documentFlowTreeMasterNode.status) }}
                  </span>
                </button>
                <div
                  v-if="documentFlowTreeDetailNodes.length"
                  class="document-flow-tree-children"
                >
                  <button
                    v-for="node in documentFlowTreeDetailNodes"
                    :key="node.id"
                    :class="[
                      'document-flow-node',
                      'document-flow-node--detail',
                      {
                        'is-active': documentFlowSelectedNode?.id === node.id,
                      },
                    ]"
                    type="button"
                    @click="selectDocumentFlowNode(node)"
                  >
                    <span class="document-flow-node__marker"></span>
                    <IconifyIcon icon="lucide:file-text" />
                    <span class="document-flow-node__content">
                      <strong>{{ node.title }}</strong>
                      <em>{{ node.subtitle }}</em>
                    </span>
                    <span :class="statusBoxClass(node.status)">
                      {{ statusBoxLabel(node.status) }}
                    </span>
                  </button>
                </div>
              </template>
              <template v-else>
                <button
                  v-for="node in documentFlowListNodes"
                  :key="node.id"
                  :class="[
                    'document-flow-node',
                    'document-flow-node--list-detail',
                    { 'is-active': documentFlowSelectedNode?.id === node.id },
                  ]"
                  type="button"
                  @click="selectDocumentFlowNode(node)"
                >
                  <span class="document-flow-node__marker"></span>
                  <IconifyIcon icon="lucide:list-tree" />
                  <span class="document-flow-node__content">
                    <strong>{{ node.title }}</strong>
                    <em>{{ node.subtitle }}</em>
                  </span>
                  <span :class="statusBoxClass(node.status)">
                    {{ statusBoxLabel(node.status) }}
                  </span>
                </button>
              </template>
              <div
                v-if="
                  documentFlowActiveTab === 'tree'
                    ? !documentFlowTreeMasterNode &&
                      !documentFlowTreeDetailNodes.length
                    : !documentFlowListNodes.length
                "
                class="document-flow-empty"
              >
                暂无匹配单据
              </div>
            </div>
          </aside>

          <section class="document-flow-main">
            <div class="document-flow-timeline">
              <button
                v-for="(step, index) in documentFlowSteps"
                :key="step.label"
                :class="[
                  'document-flow-step',
                  `document-flow-step--${step.state}`,
                ]"
                type="button"
                @click="selectDocumentFlowStep(step)"
              >
                <span class="document-flow-step__line"></span>
                <strong>{{ step.state === 'done' ? '✓' : index + 1 }}</strong>
                <div>
                  <b>{{ step.label }}</b>
                  <em>{{ step.date }}</em>
                  <small>{{ step.owner }}</small>
                  <small v-if="step.description">
                    {{ step.description }}
                  </small>
                </div>
              </button>
            </div>

            <div v-if="documentFlowSelectedNode" class="document-flow-detail">
              <div class="document-flow-detail__header">
                <h3>单据明细</h3>
                <span>
                  {{
                    documentFlowSelectedNodePosition
                  }}
                </span>
              </div>
              <dl class="document-flow-detail-grid">
                <template
                  v-for="field in documentFlowSelectedNode.fields"
                  :key="field.label"
                >
                  <div
                    :class="[
                      'document-flow-detail-field',
                      {
                        'document-flow-detail-field--text':
                          field.kind === 'TEXT',
                      },
                    ]"
                  >
                    <dt>{{ field.label }}</dt>
                    <dd>
                      <Image
                        v-if="
                          field.kind === 'IMAGE' &&
                          !isThreeCheckAttachmentMissing(field.value)
                        "
                        :src="resolveAttachmentPreviewUrl(field.value)"
                        class="document-flow-field-image document-flow-field-image--compact"
                      />
                      <span
                        v-else-if="field.kind === 'STATUS'"
                        :class="statusBoxClass(field.value)"
                      >
                        {{ statusBoxLabel(field.value) }}
                      </span>
                      <span v-else>{{ field.value || '未填写' }}</span>
                    </dd>
                  </div>
                </template>
              </dl>

              <div class="document-flow-attachments">
                <div class="document-flow-attachments__label">
                  {{ detailAttachmentLabel }}
                </div>
                <div class="document-flow-attachment-strip">
                  <div
                    v-if="documentFlowAttachments.length"
                    class="attachment-grid"
                  >
                    <div
                      v-for="attachment in documentFlowAttachments"
                      :key="attachment.id"
                      class="attachment-preview"
                    >
                      <Image
                        v-if="attachment.fileKind === 'IMAGE'"
                        :src="attachmentPreviewUrl(attachment)"
                        class="attachment-image"
                      />
                      <button
                        v-else
                        class="attachment-video"
                        type="button"
                        @click="openVideoPreview(attachmentPreviewUrl(attachment))"
                      >
                        <video
                          :src="attachmentPreviewUrl(attachment)"
                          muted
                          playsinline
                          preload="metadata"
                        ></video>
                        <span>播放视频</span>
                      </button>
                      <div class="attachment-name">
                        {{ attachment.originalName }}
                      </div>
                    </div>
                  </div>
                  <div v-else class="attachment-empty">暂无附件</div>
                  <label class="detail-attachment-upload-card">
                    <IconifyIcon icon="lucide:plus" />
                    上传附件
                    <input
                      :disabled="!canUploadAttachment"
                      accept="image/*,video/*"
                      type="file"
                      @change="handleAnyAttachmentChange"
                    />
                  </label>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </Modal>

    <Modal
      v-model:open="hazardInspectionViewOpen"
      title="自定义视图"
      width="520px"
      @ok="saveHazardInspectionViewColumns"
    >
      <Select
        v-model:value="hazardInspectionVisibleColumnKeys"
        :options="hazardInspectionVisibleColumnOptions"
        class="hazard-view-column-select"
        mode="multiple"
        option-filter-prop="label"
        show-search
      />
    </Modal>

    <Modal
      v-model:open="videoPreviewOpen"
      :footer="null"
      title="视频预览"
      width="720px"
      @cancel="closeVideoPreview"
    >
      <video
        v-if="videoPreviewUrl"
        :src="videoPreviewUrl"
        autoplay
        class="video-preview-player"
        controls
      ></video>
    </Modal>
  </Page>
</template>

<style scoped>
:global(.pingan-page) {
  height: 100%;
  padding: 12px;
  background: #f5f7fb;
}

.pingan-shell {
  display: grid;
  grid-template-columns: minmax(220px, var(--data-map-width, 280px)) minmax(
      0,
      1fr
    );
  gap: 12px;
  height: 100%;
  min-height: 0;
  transition: grid-template-columns 0.2s ease;
}

.pingan-shell--map-collapsed {
  grid-template-columns: 56px minmax(0, 1fr);
}

.pingan-shell--no-map {
  grid-template-columns: minmax(0, 1fr);
}

.data-map,
.workbench,
.table-panel {
  min-width: 0;
  background: #fff;
  border: 1px solid #e6eaf2;
  border-radius: 6px;
}

.data-map {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  overflow: hidden;
  transition:
    padding 0.2s ease,
    border-color 0.2s ease;
}

.data-map__header,
.table-panel__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.data-map__header > div {
  min-width: 0;
}

.data-map__title,
.table-panel__title {
  color: #1f2937;
  font-size: 15px;
  font-weight: 700;
}

.data-map__subtitle,
.table-panel__summary {
  margin-top: 3px;
  color: #7c8798;
  font-size: 12px;
}

.data-map__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.data-map__collapse {
  flex: 0 0 auto;
}

.data-map__tree-scroll {
  flex: 1;
  height: 100%;
  min-height: 0;
  --scroll-shadow: 0 0% 100%;
}

.data-map__tree {
  padding-right: 6px;
}

.data-map__resize-handle {
  position: absolute;
  top: 0;
  right: -7px;
  z-index: 4;
  width: 14px;
  height: 100%;
  padding: 0;
  background: transparent;
  border: 0;
  outline: none;
  cursor: col-resize;
  touch-action: none;
}

.data-map__resize-handle::after {
  position: absolute;
  top: 12px;
  bottom: 12px;
  left: 6px;
  width: 2px;
  content: '';
  background: transparent;
  border-radius: 999px;
  transition:
    background 0.15s ease,
    opacity 0.15s ease;
}

.data-map__resize-handle:focus-visible::after,
.data-map__resize-handle:hover::after,
.data-map--resizing .data-map__resize-handle::after {
  background: rgb(59 130 246 / 65%);
}

.data-map--collapsed {
  align-items: center;
  gap: 8px;
  padding: 12px 8px;
}

.data-map--collapsed .data-map__header {
  flex: 1;
  flex-direction: column;
  justify-content: flex-start;
  width: 100%;
}

.data-map--collapsed .data-map__header > div {
  display: flex;
  justify-content: center;
}

.data-map--collapsed .data-map__title {
  line-height: 1.3;
  letter-spacing: 4px;
  writing-mode: vertical-rl;
}

.data-map--collapsed .data-map__subtitle {
  display: none;
}

.workbench {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.workbench__bar {
  display: flex;
  flex-wrap: nowrap;
  align-items: end;
  gap: 10px;
  padding: 12px;
  overflow: hidden;
  background: #f8fafc;
  border-bottom: 1px solid #e6eaf2;
}

.filter-item {
  display: flex;
  flex: 1 1 126px;
  flex-direction: column;
  justify-content: flex-end;
  min-width: 0;
}

.filter-item span {
  display: block;
  margin-bottom: 5px;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.filter-item :deep(.ant-picker),
.filter-item :deep(.ant-select),
.filter-item :deep(.ant-input),
.filter-item :deep(.ant-input-affix-wrapper) {
  width: 100%;
  height: 40px;
  min-height: 40px;
}

.filter-item :deep(.ant-input-affix-wrapper),
.filter-item :deep(.ant-picker),
.filter-item :deep(.ant-select-single .ant-select-selector) {
  display: flex;
  align-items: center;
}

.filter-item :deep(.ant-select-single .ant-select-selector) {
  height: 40px;
}

.filter-item :deep(.ant-select-single .ant-select-selection-item),
.filter-item :deep(.ant-select-single .ant-select-selection-placeholder) {
  line-height: 38px;
}

.table-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  margin: 12px;
  overflow: hidden;
}

.table-panel__toolbar {
  padding: 12px;
  border-bottom: 1px solid #eef2f7;
}

.table-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

.table-panel__selection {
  color: #475569;
  font-size: 13px;
}

.meeting-table {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.meeting-table :deep(.ant-spin-nested-loading),
.meeting-table :deep(.ant-spin-container) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.meeting-table :deep(.ant-table) {
  flex: 1 1 auto;
  min-height: 0;
  color: #4b5563;
  table-layout: fixed;
}

.meeting-table :deep(.ant-table-container) {
  min-height: 0;
}

.meeting-table :deep(.ant-table-body) {
  overscroll-behavior: contain;
  scrollbar-width: none;
}

.meeting-table--bottom-rail :deep(.ant-table-body) {
  scrollbar-width: thin;
}

.meeting-table :deep(.ant-table-body::-webkit-scrollbar),
.meeting-table :deep(.ant-table-content::-webkit-scrollbar) {
  width: 8px;
  height: 0;
}

.meeting-table--bottom-rail :deep(.ant-table-body::-webkit-scrollbar),
.meeting-table--bottom-rail :deep(.ant-table-content::-webkit-scrollbar) {
  height: 10px;
}

.meeting-table :deep(.ant-table-body::-webkit-scrollbar-thumb),
.meeting-table :deep(.ant-table-content::-webkit-scrollbar-thumb) {
  background: #cbd5e1;
  border-radius: 999px;
}

.meeting-table :deep(.ant-table-bordered .ant-table-container),
.meeting-table :deep(.ant-table-bordered .ant-table-content),
.meeting-table :deep(.ant-table-bordered .ant-table-cell),
.meeting-table :deep(.ant-table-bordered .ant-table-thead > tr > th) {
  border-inline-end: 0 !important;
  border-inline-start: 0 !important;
}

.meeting-table :deep(.ant-table-thead > tr > th) {
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
  background: #f8fafc;
}

.meeting-table :deep(.ant-table-cell) {
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.meeting-table :deep(.meeting-table__center-cell) {
  text-align: center;
}

.meeting-table :deep(.meeting-table__attachment-cell),
.meeting-table :deep(.meeting-table__actions-cell),
.meeting-table :deep(.meeting-table__status-cell) {
  overflow: visible;
  text-align: center;
  text-overflow: clip;
}

.meeting-table :deep(.meeting-table__resizable-header) {
  position: relative;
}

.meeting-table :deep(.meeting-table__resizable-header::after) {
  position: absolute;
  top: 8px;
  right: 0;
  bottom: 8px;
  width: 10px;
  cursor: col-resize;
  content: '';
  border-right: 2px solid transparent;
  transition: border-color 0.15s ease;
}

.meeting-table :deep(.meeting-table__fixed-left-resizable-header::after) {
  right: -5px;
  z-index: 6;
  width: 14px;
}

.meeting-table :deep(.meeting-table__resizable-header:hover::after),
.meeting-table--column-resizing :deep(.meeting-table__resizable-header::after) {
  border-right-color: #93c5fd;
}

.status-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 64px;
  height: 26px;
  padding: 0 9px;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
}

.status-box--green {
  color: #15803d;
  background: #f0fdf4;
  border-color: #86efac;
}

.status-box--orange {
  color: #c2410c;
  background: #fff7ed;
  border-color: #fdba74;
}

.status-box--red {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fca5a5;
}

.status-box--blue {
  color: #1d4ed8;
  background: #eff6ff;
  border-color: #93c5fd;
}

.status-box--gray {
  color: #475569;
  background: #f8fafc;
  border-color: #cbd5e1;
}

.attachment-table-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
}

.upload-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 64px;
  height: 26px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  border: 1px solid;
  border-radius: 8px;
}

.upload-pill--missing {
  color: #c2410c;
  background: #fff7ed;
  border-color: #fdba74;
}

.upload-pill--done {
  color: #15803d;
  background: #f0fdf4;
  border-color: #86efac;
}

.check-thumb {
  display: inline-flex;
  align-items: end;
  justify-content: center;
  width: 56px;
  height: 34px;
  overflow: hidden;
  color: #fff;
  font-size: 10px;
  line-height: 1;
  background:
    linear-gradient(160deg, rgba(15, 118, 110, 0.18), rgba(37, 99, 235, 0.32)),
    linear-gradient(135deg, #5b8f61 0%, #b8a45c 42%, #637a8c 100%);
  border: 1px solid #d8e1ea;
  border-radius: 4px;
}

.check-thumb span {
  width: 100%;
  padding: 3px 0;
  text-align: center;
  background: rgb(15 23 42 / 55%);
}

.check-preview-image {
  width: 56px;
  height: 34px;
  overflow: hidden;
  border: 1px solid #d8e1ea;
  border-radius: 4px;
}

.check-preview-image :deep(img) {
  object-fit: cover;
}

.video-preview-trigger {
  position: relative;
  display: inline-flex;
  align-items: flex-end;
  justify-content: center;
  width: 72px;
  height: 40px;
  padding: 0;
  overflow: hidden;
  color: #fff;
  cursor: pointer;
  background: #111827;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
}

.video-preview-trigger video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.78;
}

.video-preview-trigger span {
  position: relative;
  z-index: 1;
  width: 100%;
  padding: 3px 0;
  font-size: 10px;
  line-height: 1;
  text-align: center;
  background: rgb(15 23 42 / 58%);
}

.meeting-form,
.meeting-detail {
  display: grid;
  gap: 12px;
}

.meeting-form label {
  display: grid;
  gap: 6px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.meeting-form :deep(.ant-picker),
.meeting-form :deep(.ant-select) {
  width: 100%;
}

.quick-shot-workflow-date-picker {
  width: 100%;
}

.quick-shot-workflow-upload {
  display: grid;
  gap: 8px;
}

.quick-shot-workflow-upload-card {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  color: #1d4ed8;
  cursor: pointer;
  background: #eff6ff;
  border: 1px dashed #93c5fd;
  border-radius: 4px;
}

.quick-shot-workflow-upload-card input {
  display: none;
}

.quick-shot-workflow-upload-card svg {
  width: 18px;
  height: 18px;
}

.quick-shot-workflow-upload-preview {
  width: 120px;
  height: 82px;
  overflow: hidden;
  border: 1px solid #d8e1ea;
  border-radius: 4px;
}

.quick-shot-workflow-upload-preview :deep(img) {
  width: 120px;
  height: 82px;
  object-fit: cover;
}

.create-attachment-control {
  display: grid;
  gap: 6px;
}

.create-attachment-control input {
  width: 100%;
  color: #475569;
  font-weight: 500;
}

.create-attachment-control span {
  min-height: 22px;
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
}

:global(.document-flow-modal .ant-modal-content) {
  overflow: hidden;
  border-radius: 6px;
}

:global(.change-history-modal .ant-modal-content) {
  overflow: hidden;
  border-radius: 6px;
  box-shadow: 0 18px 48px rgb(15 23 42 / 22%);
}

:global(.change-history-modal .ant-modal-header) {
  min-height: 66px;
  padding: 22px 24px 16px;
  margin-bottom: 0;
  border-bottom: 1px solid #e5ebf3;
}

:global(.change-history-modal .ant-modal-body) {
  max-height: calc(100vh - 96px);
  padding: 0;
  overflow: auto;
  background: #fbfcff;
}

.change-history-title {
  display: flex;
  gap: 18px;
  align-items: center;
}

.change-history-title strong {
  color: #0f172a;
  font-size: 22px;
  font-weight: 800;
}

.change-history-title span {
  color: #64748b;
  font-size: 14px;
  font-weight: 700;
}

.change-history {
  display: grid;
  gap: 16px;
  padding: 14px 28px 28px;
  background: #fbfcff;
}

.change-history-toolbar,
.change-history-toolbar__left,
.change-history-toolbar__right,
.change-history-filter {
  display: flex;
  gap: 10px;
  align-items: center;
}

.change-history-toolbar {
  justify-content: space-between;
}

.change-history-search {
  width: 320px;
}

.change-history-filter {
  gap: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #dbe3ee;
  border-radius: 6px;
}

.change-history-filter button {
  height: 32px;
  padding: 0 12px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-right: 1px solid #e5ebf3;
}

.change-history-filter button:last-child {
  border-right: 0;
}

.change-history-filter button.is-active {
  color: #2563eb;
  background: #eff6ff;
}

.change-history-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.change-history-summary-card {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  min-height: 94px;
  padding: 18px;
  background: #fff;
  border: 1px solid #dde6f1;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgb(15 23 42 / 5%);
}

.change-history-summary-card__icon {
  display: inline-grid;
  place-items: center;
  width: 46px;
  height: 46px;
  line-height: 1;
  color: #2f7cf6;
  background: #eef5ff;
  border-radius: 50%;
}

.change-history-summary-card__icon svg,
.change-history-summary-card__icon :deep(svg) {
  display: block;
  width: 25px;
  height: 25px;
}

.change-history-summary-card__icon--default {
  color: #16a34a;
  background: #eaf8ed;
}

.change-history-summary-card__icon--orange {
  color: #f97316;
  background: #fff1e6;
}

.change-history-summary-card > div span {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.change-history-summary-card strong {
  display: block;
  margin-top: 4px;
  overflow: hidden;
  color: #111827;
  font-size: 16px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.change-history-summary-card__value--orange {
  color: #ea580c !important;
}

.change-history-summary-card__value--blue {
  color: #1d4ed8 !important;
}

.change-history-body {
  display: grid;
  grid-template-columns: 430px minmax(0, 1fr);
  gap: 14px;
  min-height: 630px;
}

.change-history-sidebar,
.change-history-main {
  min-width: 0;
  background: #fff;
  border: 1px solid #e4ebf5;
  border-radius: 6px;
}

.change-history-sidebar {
  position: relative;
  display: grid;
  align-content: start;
  gap: 18px;
  padding: 18px 16px 18px 52px;
}

.change-history-sidebar__track {
  position: absolute;
  top: 32px;
  bottom: 32px;
  left: 25px;
  width: 2px;
  background: #d8e1ed;
}

.change-history-group {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 134px;
  padding: 18px 20px;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1px solid #dde6f1;
  border-radius: 6px;
  box-shadow: 0 8px 22px rgb(15 23 42 / 5%);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.change-history-group::before {
  position: absolute;
  top: 28px;
  left: -34px;
  width: 12px;
  height: 12px;
  content: '';
  background: #94a3b8;
  border: 3px solid #fff;
  border-radius: 50%;
  box-shadow: 0 0 0 1px #d8e1ed;
}

.change-history-group.is-active,
.change-history-group.is-latest {
  background: linear-gradient(90deg, #eff6ff 0%, #fff 68%);
  border-color: #2f7cf6;
  box-shadow: 0 12px 28px rgb(47 124 246 / 14%);
}

.change-history-group.is-active::before,
.change-history-group.is-latest::before {
  background: #2f7cf6;
  box-shadow: 0 0 0 4px #dbeafe;
}

.change-history-group__top,
.change-history-group__meta,
.change-history-group__stats {
  display: flex;
  min-width: 0;
}

.change-history-group__top {
  gap: 16px;
  align-items: center;
}

.change-history-group__version {
  display: inline-grid;
  place-items: center;
  min-width: 52px;
  height: 28px;
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  background: linear-gradient(135deg, #2f7cf6, #1d4ed8);
  border-radius: 4px;
  box-shadow: 0 6px 14px rgb(47 124 246 / 28%);
}

.change-history-group:not(.is-active):not(.is-latest)
  .change-history-group__version {
  color: #fff;
  background: linear-gradient(135deg, #9ca3af, #6b7280);
  box-shadow: none;
}

.change-history-group__top strong {
  min-width: 0;
  overflow: hidden;
  color: #111827;
  font-size: 16px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.change-history-group.is-active .change-history-group__top strong,
.change-history-group.is-latest .change-history-group__top strong {
  color: #1677ff;
}

.change-history-group__meta {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 168px;
  gap: 18px;
  align-items: center;
}

.change-history-group__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.change-history-group__meta span,
.change-history-group__stats span {
  display: inline-flex;
  min-width: 0;
  gap: 6px;
  align-items: center;
  overflow: hidden;
  color: #4b5563;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.change-history-group__meta svg,
.change-history-group__stats svg {
  flex: 0 0 auto;
  width: 15px;
  height: 15px;
  color: #111827;
  stroke-width: 1.8;
}

.change-history-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
  box-shadow: 0 8px 24px rgb(15 23 42 / 5%);
}

.change-history-main__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid #e5ebf3;
}

.change-history-main__header h3 {
  margin: 0;
  color: #111827;
  font-size: 16px;
  font-weight: 800;
}

.change-history-main__header span {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.change-history-diff-table {
  padding: 14px;
  overflow: auto;
}

.change-history-diff-table__head,
.change-history-diff-row {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) minmax(0, 1fr) 90px;
  min-width: 760px;
}

.change-history-diff-table__head {
  overflow: hidden;
  color: #475569;
  font-size: 13px;
  font-weight: 800;
  background: #fff;
  border: 1px solid #e5ebf3;
  border-bottom-color: #d9e1ec;
  border-radius: 6px 6px 0 0;
}

.change-history-diff-table__head span,
.change-history-diff-row > div {
  min-width: 0;
  padding: 11px 12px;
}

.change-history-diff-row > div {
  display: flex;
  align-items: center;
}

.change-history-diff-row {
  color: #111827;
  font-size: 13px;
  border: 1px solid #e5ebf3;
  border-top: 0;
  background: #fff;
}

.change-history-diff-row__field strong {
  font-weight: 800;
}

.change-history-diff-value {
  display: -webkit-box;
  box-sizing: border-box;
  width: 100%;
  overflow: hidden;
  min-height: 40px;
  padding: 10px 14px;
  line-height: 1.55;
  white-space: pre-wrap;
  border-radius: 6px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.change-history-diff-value--before {
  background: #f6f7f9;
}

.change-history-diff-value--after {
  color: #15803d;
  background: #edf6ff;
}

.change-history-attachments {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 12px;
  padding: 14px 18px 18px;
  border-top: 1px solid #e5ebf3;
}

.change-history-attachments__label {
  color: #475569;
  font-size: 14px;
  font-weight: 800;
}

.change-history-attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  min-width: 0;
}

.change-history-attachment-card {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  width: 220px;
  padding: 8px;
  background: #f8fafc;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
}

.change-history-attachment-card__thumb {
  display: grid;
  place-items: center;
  width: 54px;
  height: 42px;
  color: #2563eb;
  background: #dbeafe;
  border-radius: 4px;
}

.change-history-attachment-card strong,
.change-history-attachment-card span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.change-history-attachment-card strong {
  color: #111827;
  font-size: 13px;
  font-weight: 800;
}

.change-history-attachment-card span {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.change-history-empty {
  padding: 24px;
  color: #94a3b8;
  text-align: center;
}

:global(.document-flow-modal .ant-modal-header) {
  min-height: 66px;
  padding: 22px 24px 16px;
  margin-bottom: 0;
  border-bottom: 1px solid #e5ebf3;
}

:global(.document-flow-modal .ant-modal-body) {
  max-height: calc(100vh - 96px);
  padding: 0;
  overflow: auto;
  background: #f6f8fb;
}

.document-flow-title {
  display: flex;
  gap: 18px;
  align-items: center;
}

.document-flow-title strong {
  color: #0f172a;
  font-size: 22px;
  font-weight: 800;
}

.document-flow-title span {
  color: #64748b;
  font-size: 14px;
  font-weight: 700;
}

.document-flow {
  display: grid;
  gap: 14px;
  padding: 14px 20px 20px;
}

.document-flow-toolbar,
.document-flow-toolbar__left,
.document-flow-toolbar__right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.document-flow-toolbar {
  justify-content: space-between;
}

.document-flow-search {
  width: 310px;
}

.document-flow-tool-button {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.document-flow-summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.document-flow-summary-card {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  min-height: 74px;
  padding: 13px 16px;
  background: #fff;
  border: 1px solid #e4ebf5;
  border-radius: 6px;
}

.document-flow-summary-card > svg {
  width: 36px;
  height: 36px;
  color: #2f7cf6;
}

.document-flow-summary-card span {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.document-flow-summary-card strong {
  display: inline-flex;
  margin-top: 4px;
  color: #111827;
  font-size: 17px;
  font-weight: 800;
}

.document-flow-summary-card__value--orange {
  padding: 2px 8px;
  color: #ea580c !important;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 4px;
}

.document-flow-summary-card__value--blue {
  color: #1d4ed8 !important;
}

.document-flow-body {
  display: grid;
  grid-template-columns: 344px minmax(0, 1fr);
  gap: 16px;
  min-height: 620px;
}

.document-flow-sidebar,
.document-flow-main {
  min-width: 0;
  background: #fff;
  border: 1px solid #e4ebf5;
  border-radius: 6px;
}

.document-flow-tabs {
  display: flex;
  height: 46px;
  border-bottom: 1px solid #e5ebf3;
}

.document-flow-tabs button {
  position: relative;
  flex: 0 0 96px;
  color: #475569;
  font-size: 13px;
  font-weight: 800;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.document-flow-tabs button.is-active {
  color: #2563eb;
}

.document-flow-tabs button.is-active::after {
  position: absolute;
  right: 18px;
  bottom: 0;
  left: 18px;
  height: 3px;
  content: '';
  background: #2563eb;
  border-radius: 3px 3px 0 0;
}

.document-flow-node-list {
  display: grid;
  gap: 10px;
  padding: 18px 14px;
}

.document-flow-tree-children {
  position: relative;
  display: grid;
  gap: 10px;
  padding-left: 26px;
}

.document-flow-tree-children::before {
  position: absolute;
  top: -10px;
  bottom: 32px;
  left: 10px;
  width: 1px;
  content: '';
  background: #dbe5f1;
}

.document-flow-node {
  position: relative;
  display: grid;
  grid-template-columns: 20px 22px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  min-height: 64px;
  padding: 10px;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
}

.document-flow-tree-children .document-flow-node::before {
  position: absolute;
  top: 31px;
  left: -16px;
  width: 16px;
  height: 1px;
  content: '';
  background: #dbe5f1;
}

.document-flow-node--master {
  background: #f8fbff;
}

.document-flow-node--list-detail {
  grid-template-columns: 14px 22px minmax(0, 1fr) auto;
}

.document-flow-node.is-active {
  background: #eff6ff;
  border-color: #2f7cf6;
}

.document-flow-node__marker {
  width: 9px;
  height: 9px;
  margin: auto;
  background: #cbd5e1;
  border-radius: 50%;
}

.document-flow-node.is-active .document-flow-node__marker {
  background: #22c55e;
}

.document-flow-node > svg {
  width: 20px;
  height: 20px;
  color: #2563eb;
}

.document-flow-node__content {
  min-width: 0;
}

.document-flow-node__content strong,
.document-flow-node__content em {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-flow-node__content strong {
  color: #111827;
  font-size: 13px;
  font-weight: 800;
}

.document-flow-node__content em {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.document-flow-empty {
  padding: 32px 0;
  color: #94a3b8;
  text-align: center;
}

.document-flow-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
}

.document-flow-timeline {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(112px, 1fr));
  padding: 18px 34px;
  border-bottom: 1px solid #e5ebf3;
}

.document-flow-step {
  position: relative;
  display: grid;
  gap: 8px;
  justify-items: center;
  padding: 0;
  font: inherit;
  text-align: center;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.document-flow-step__line {
  position: absolute;
  top: 18px;
  right: calc(50% + 22px);
  left: calc(-50% + 22px);
  height: 1px;
  background: #d1d5db;
}

.document-flow-step:first-child .document-flow-step__line {
  display: none;
}

.document-flow-step strong {
  position: relative;
  z-index: 1;
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  color: #64748b;
  background: #fff;
  border: 1px solid #d1d5db;
  border-radius: 50%;
}

.document-flow-step--done strong {
  color: #16a34a;
  border-color: #16a34a;
}

.document-flow-step--current strong {
  color: #fff;
  background: #2563eb;
  border-color: #2563eb;
}

.document-flow-step b,
.document-flow-step em,
.document-flow-step small {
  display: block;
}

.document-flow-step b {
  color: #111827;
  font-size: 13px;
}

.document-flow-step em,
.document-flow-step small {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.document-flow-detail {
  padding: 14px;
}

.document-flow-detail__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.document-flow-detail__header h3 {
  margin: 0;
  color: #111827;
  font-size: 16px;
  font-weight: 800;
}

.document-flow-detail__header span {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.document-flow-detail-grid {
  display: grid;
  grid-template-columns: 1fr;
  margin: 0;
  overflow: hidden;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
}

.document-flow-detail-field {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  min-width: 0;
  border-bottom: 1px solid #e5ebf3;
}

.document-flow-detail-field:last-child {
  border-bottom: 0;
}

.document-flow-detail-field dt,
.document-flow-detail-field dd {
  min-height: 46px;
  padding: 11px 14px;
  margin: 0;
}

.document-flow-detail-field dt {
  color: #64748b;
  font-weight: 800;
  background: #f8fafc;
  border-right: 1px solid #e5ebf3;
}

.document-flow-detail-field dd {
  min-width: 0;
  color: #111827;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.document-flow-detail-field--text {
  align-items: stretch;
}

.document-flow-detail-field--text dd {
  line-height: 1.55;
  white-space: pre-wrap;
}

:global(.document-flow-field-image--compact),
:global(.document-flow-field-image--compact.ant-image-img),
:global(.document-flow-field-image--compact .ant-image-img),
:global(.document-flow-field-image--compact img) {
  width: 96px;
  height: 64px;
  object-fit: cover;
}

:global(.document-flow-field-image--compact) {
  display: inline-block;
  overflow: hidden;
  vertical-align: middle;
  border: 1px solid #d8e1ea;
  border-radius: 4px;
}

.document-flow-attachments {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 14px;
  padding-top: 18px;
  margin-top: 16px;
  border-top: 1px solid #edf2f7;
}

.document-flow-attachments__label {
  color: #475569;
  font-size: 14px;
  font-weight: 800;
}

.document-flow-attachment-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: flex-start;
  min-width: 0;
}

.document-flow-attachment-strip .attachment-grid {
  display: contents;
}

.document-flow-attachment-strip .attachment-preview,
.document-flow-attachment-strip .detail-attachment-upload-card {
  width: 132px;
}

.document-flow-attachment-strip .attachment-empty {
  min-width: 88px;
  padding-top: 38px;
  margin-top: 0;
}

.detail-action-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px;
  color: #475569;
  font-size: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.detail-action-buttons {
  min-width: 0;
}

.detail-action-button {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  height: 32px;
  padding-inline: 12px;
  font-weight: 700;
}

.detail-action-icon {
  width: 15px;
  height: 15px;
}

.detail-action-bar span {
  color: #64748b;
  font-weight: 700;
  line-height: 1.4;
}

.detail-panel {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #dbeafe;
  border-radius: 6px;
}

.detail-panel h3 {
  margin: 0 0 10px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.detail-flow-list {
  display: grid;
  gap: 8px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.detail-flow-list li {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr) 128px;
  gap: 10px;
  align-items: center;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.detail-flow-list span,
.detail-history-list dt {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.detail-flow-list strong,
.detail-history-list dd {
  min-width: 0;
  margin: 0;
  color: #1f2937;
  font-size: 13px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.detail-flow-list em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  text-align: right;
}

.detail-history-list {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  gap: 8px 12px;
  margin: 0;
  padding: 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.detail-panel-empty {
  padding: 10px;
  color: #94a3b8;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.detail-edit-control {
  width: 100%;
}

.detail-edit-textarea {
  min-height: 64px;
  line-height: 1.55;
  resize: vertical;
}

.detail-meta-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.detail-meta-strip > div {
  min-width: 0;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.detail-meta-strip span,
.detail-field dt {
  display: block;
  margin-bottom: 4px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.3;
}

.detail-meta-strip strong,
.detail-field dd {
  min-width: 0;
  margin: 0;
  color: #1f2937;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.detail-field-sections {
  display: grid;
  align-items: stretch;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-field-section {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 14px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.detail-field-section--content,
.detail-field-section--acceptance,
.detail-field-section--default {
  grid-column: 1 / -1;
}

.detail-field-section--default + .detail-field-section--media {
  grid-column: 1 / -1;
}

.detail-field-section h3 {
  margin: 0 0 10px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.2;
}

.detail-field-grid {
  display: grid;
  flex: 1;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
  margin: 0;
}

.detail-field-section--content .detail-field-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.detail-field-grid--balanced {
  align-items: start;
}

.detail-field-column {
  display: grid;
  gap: 10px;
  min-width: 0;
  margin: 0;
}

.detail-field {
  min-width: 0;
  min-height: 58px;
  padding: 9px 10px;
  background: #f8fafc;
  border: 1px solid #edf2f7;
  border-radius: 6px;
}

.detail-field--wide {
  grid-column: 1 / -1;
}

.detail-field-section--content .detail-field {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  align-items: center;
  min-height: 42px;
}

.detail-field-section--content .detail-field dt {
  margin-bottom: 0;
}

.detail-attachment-field {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.detail-attachment-field__preview,
.detail-attachment-field__video {
  flex: 0 0 auto;
  width: 88px;
  height: 58px;
  overflow: hidden;
  border: 1px solid #d8e1ea;
  border-radius: 4px;
}

.detail-attachment-field__preview :deep(img),
.detail-attachment-field__video video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-attachment-field__video {
  position: relative;
  padding: 0;
  cursor: pointer;
  background: #0f172a;
}

.detail-attachment-field__video span {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 3px 4px;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  text-align: center;
  background: rgb(15 23 42 / 68%);
}

.detail-attachment-field__empty {
  color: #94a3b8;
  font-size: 13px;
  font-weight: 500;
}

.detail-attachment-field__upload,
.hazard-lines-upload {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 0 10px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 4px;
}

.detail-attachment-field__upload input,
.hazard-lines-upload input {
  display: none;
}

:global(.pingan-detail-modal--safety-check .ant-modal) {
  top: 0;
  padding-bottom: 0;
}

:global(.pingan-detail-modal--safety-check .ant-modal-content) {
  overflow: hidden;
  border-radius: 6px;
}

:global(.pingan-detail-modal--safety-check .ant-modal-header) {
  min-height: 64px;
  padding: 20px 30px 16px;
  margin-bottom: 0;
  border-bottom: 1px solid #e5ebf3;
}

:global(.pingan-detail-modal--safety-check .ant-modal-title) {
  color: #0b274a;
  font-size: 18px;
  font-weight: 800;
  line-height: 26px;
}

:global(.pingan-detail-modal--safety-check .ant-modal-close) {
  top: 18px;
  right: 24px;
  color: #475569;
}

:global(.pingan-detail-modal--safety-check .ant-modal-body) {
  padding: 12px 28px 24px;
}

:global(.pingan-detail-modal--wide .ant-modal) {
  top: 0;
  padding-bottom: 0;
}

:global(.pingan-detail-modal--wide .ant-modal-content) {
  overflow: hidden;
  border-radius: 6px;
}

:global(.pingan-detail-modal--wide .ant-modal-header) {
  min-height: 58px;
  padding: 18px 24px 14px;
  margin-bottom: 0;
  border-bottom: 1px solid #e5e7eb;
}

:global(.pingan-detail-modal--wide .ant-modal-body) {
  max-height: calc(100vh - 108px);
  padding: 14px 22px 22px;
  overflow: auto;
}

:global(.pingan-detail-modal--team-check .ant-modal) {
  top: 0;
  padding-bottom: 0;
}

:global(.pingan-detail-modal--team-check .ant-modal-content) {
  overflow: hidden;
  border-radius: 6px;
}

:global(.pingan-detail-modal--team-check .ant-modal-header) {
  min-height: 58px;
  padding: 18px 24px 14px;
  margin-bottom: 0;
  border-bottom: 1px solid #e5e7eb;
}

:global(.pingan-detail-modal--team-check .ant-modal-body) {
  max-height: calc(100vh - 108px);
  padding: 14px 22px 22px;
  overflow: auto;
}

.meeting-detail--safety-check {
  gap: 12px;
}

.meeting-detail--safety-check .detail-field-sections {
  grid-template-columns: 1fr;
  gap: 12px;
}

.meeting-detail--safety-check .detail-field-section {
  padding: 0;
  overflow: hidden;
  border-color: #e3eaf3;
  border-radius: 6px;
}

.meeting-detail--safety-check .detail-field-section h3 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 50px;
  padding: 0 18px 0 22px;
  margin: 0;
  color: #111827;
  font-size: 15px;
  font-weight: 800;
  line-height: 20px;
  background: #fff;
  border-bottom: 1px solid #e5ebf3;
}

.meeting-detail--safety-check .detail-field-section h3 span {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-height: 20px;
}

.meeting-detail--safety-check .detail-field-section h3 span::before {
  width: 3px;
  height: 18px;
  margin-right: 9px;
  content: '';
  background: #1677ff;
  border-radius: 2px;
}

.meeting-detail--safety-check .detail-field-section h3 .ant-btn {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  height: 32px;
  padding: 0 14px;
  color: #374151;
  font-weight: 700;
  border-color: #d8e1ea;
  border-radius: 4px;
}

.meeting-detail--safety-check .detail-field-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
  grid-template-rows: repeat(3, minmax(72px, auto));
  grid-auto-flow: column;
  gap: 0;
  padding: 24px 0 26px;
}

.meeting-detail--safety-check .detail-field {
  display: block;
  min-height: 72px;
  padding: 0 20px;
  background: transparent;
  border: 0;
  border-left: 0;
  border-radius: 0;
}

.meeting-detail--safety-check .detail-field:nth-child(n + 4) {
  border-left: 1px solid #e8eef6;
}

.meeting-detail--safety-check .detail-field--inspectionUnit {
  grid-column: 1;
  grid-row: 1;
}

.meeting-detail--safety-check .detail-field--createdBy {
  grid-column: 1;
  grid-row: 2;
}

.meeting-detail--safety-check .detail-field--inspectedUnitPersonnelManual {
  grid-column: 1;
  grid-row: 3;
}

.meeting-detail--safety-check .detail-field--inspectedUnit {
  grid-column: 2;
  grid-row: 1;
}

.meeting-detail--safety-check .detail-field--inspectionMethod {
  grid-column: 2;
  grid-row: 2;
}

.meeting-detail--safety-check .detail-field--inspectionContent {
  grid-column: 2;
  grid-row: 3;
}

.meeting-detail--safety-check .detail-field--inspectionType {
  grid-column: 3;
  grid-row: 1;
}

.meeting-detail--safety-check .detail-field--inspectors {
  grid-column: 3;
  grid-row: 2;
}

.meeting-detail--safety-check .detail-field--remarks {
  grid-column: 3;
  grid-row: 3;
}

.meeting-detail--safety-check .detail-field--inspectionTypeManual {
  grid-column: 4;
  grid-row: 1;
}

.meeting-detail--safety-check .detail-field--acceptancePersonnel {
  grid-column: 4;
  grid-row: 2;
}

.meeting-detail--safety-check .detail-field--inspectionDate {
  grid-column: 5;
  grid-row: 1;
}

.meeting-detail--safety-check .detail-field--inspectorsManual {
  grid-column: 5;
  grid-row: 2;
}

.meeting-detail--safety-check .detail-field--inspectionTime {
  grid-column: 6;
  grid-row: 1;
}

.meeting-detail--safety-check .detail-field--inspectedUnitPersonnel {
  grid-column: 6;
  grid-row: 2;
}

.meeting-detail--safety-check .detail-field--wide {
  align-self: stretch;
}

.meeting-detail--safety-check .detail-field dt {
  margin: 0 0 12px;
  color: #475569;
  font-size: 13px;
  font-weight: 800;
  line-height: 18px;
  white-space: nowrap;
}

.meeting-detail--safety-check .detail-field dd {
  min-height: 24px;
  padding: 0;
  color: #111827;
  font-size: 13px;
  font-weight: 600;
  line-height: 24px;
  border-bottom: 0;
}

.meeting-detail--safety-check .detail-field--inspectionUnit dt::after,
.meeting-detail--safety-check .detail-field--inspectedUnit dt::after,
.meeting-detail--safety-check .detail-field--inspectionType dt::after,
.meeting-detail--safety-check .detail-field--inspectionDate dt::after,
.meeting-detail--safety-check .detail-field--inspectionTime dt::after,
.meeting-detail--safety-check .detail-field--createdBy dt::after,
.meeting-detail--safety-check .detail-field--inspectionMethod dt::after,
.meeting-detail--safety-check .detail-field--inspectors dt::after,
.meeting-detail--safety-check .detail-field--acceptancePersonnel dt::after,
.meeting-detail--safety-check .detail-field--inspectedUnitPersonnel dt::after,
.meeting-detail--safety-check .detail-field--inspectionContent dt::after {
  margin-left: 3px;
  color: #ef4444;
  content: '*';
}

.meeting-detail--safety-check .detail-field--status dd {
  border-bottom: 0;
}

.meeting-detail--safety-check
  .detail-field-section--content
  .detail-field
  dd {
  padding: 6px 13px;
  color: #111827;
  background: #f6f8fa;
  border: 1px solid #e3e9f1;
  border-radius: 4px;
}

.meeting-detail--safety-check
  .detail-field-section--content
  .detail-field--wide
  dd {
  min-height: 52px;
  line-height: 1.55;
  white-space: pre-wrap;
}

.meeting-detail--safety-check
  .detail-field-section--content
  .detail-field--inspectionType
  dd,
.meeting-detail--safety-check
  .detail-field-section--content
  .detail-field--inspectionContent
  dd {
  min-height: 68px;
}

.meeting-detail--safety-check
  .detail-field-section--content
  .detail-field--remarks
  dd {
  min-height: 42px;
  line-height: 1.35;
}

.meeting-detail--safety-check .detail-edit-control {
  min-height: 30px;
}

.team-check-items {
  margin-top: 16px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.team-check-items__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 42px;
  padding: 0 12px;
  background: #fafafa;
  border-bottom: 1px solid #e5e7eb;
}

.team-check-items__header h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  font-weight: 700;
}

.team-check-items__header span {
  color: #6b7280;
  font-size: 12px;
}

.team-check-items__table-wrap {
  overflow-x: auto;
}

.team-check-items__table {
  width: 100%;
  min-width: 1280px;
  table-layout: fixed;
  border-collapse: collapse;
}

.team-check-items__table th,
.team-check-items__table td {
  padding: 10px 12px;
  color: #374151;
  font-size: 13px;
  line-height: 1.45;
  text-align: left;
  vertical-align: top;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.team-check-items__table th {
  color: #4b5563;
  font-weight: 700;
  white-space: nowrap;
  background: #f8fafc;
}

.team-check-items__table th span {
  color: #ef4444;
}

.team-check-items__table th:last-child,
.team-check-items__table td:last-child {
  border-right: 0;
}

.team-check-items__table tbody tr:last-child td {
  border-bottom: 0;
}

.team-check-items__text {
  display: block;
  min-height: 22px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.team-check-items__control {
  width: 100%;
}

.team-check-items__textarea.ant-input {
  min-height: 32px;
}

.team-check-items__empty {
  color: #9ca3af;
  text-align: center !important;
}

.detail-attachment-row {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 12px;
  padding: 0 24px 16px;
}

.detail-attachment-row--bottom {
  padding: 18px 0 0;
  margin-top: 16px;
  border-top: 1px solid #edf2f7;
}

.detail-attachment-label {
  padding-top: 7px;
  color: #475569;
  font-size: 14px;
  font-weight: 800;
  line-height: 20px;
}

.detail-attachment-content {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: flex-start;
  justify-content: flex-start;
  min-width: 0;
  padding-top: 4px;
  border-top: 1px solid #edf2f7;
}

.detail-attachment-content .attachment-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin: 0;
}

.detail-attachment-content .attachment-preview,
.detail-attachment-upload-card {
  width: 132px;
}

.detail-attachment-content .attachment-empty {
  min-width: 88px;
  padding: 38px 0 0;
  color: #94a3b8;
  text-align: left;
}

.detail-attachment-upload-card {
  display: inline-flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  justify-content: center;
  height: 100px;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 4px;
}

.detail-attachment-upload-card svg {
  width: 26px;
  height: 26px;
  color: #64748b;
}

.detail-attachment-upload-card input {
  display: none;
}

.meeting-detail--safety-check .attachment-image,
.meeting-detail--safety-check .attachment-image :deep(img),
.meeting-detail--safety-check .attachment-video {
  height: 86px;
}

.meeting-detail > dl {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 10px 12px;
  margin: 0;
}

.meeting-detail dt {
  color: #64748b;
  font-weight: 700;
}

.meeting-detail dd {
  min-width: 0;
  margin: 0;
  color: #1f2937;
}

.hazard-detail-lines {
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
}

.hazard-lines-table th span {
  color: #ef4444;
}

.hazard-lines-edit-control {
  width: 100%;
  color: #111827;
}

.hazard-lines-edit-control.ant-input,
.hazard-lines-edit-control :deep(.ant-select-selector),
.hazard-lines-edit-control :deep(.ant-input) {
  color: #111827;
  background: #f8fafc;
  border-color: #dbe3ee;
}

.hazard-lines-edit-textarea {
  min-height: 54px;
  color: #111827;
  line-height: 1.45;
  resize: vertical;
  background: #f8fafc;
}

.hazard-lines-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 16px 12px;
}

.hazard-lines-header h3 {
  margin: 0;
  color: #334155;
  font-size: 15px;
  font-weight: 800;
}

.hazard-lines-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px 8px;
}

.hazard-lines-search {
  width: 260px;
}

.hazard-view-column-select {
  width: 100%;
}

.hazard-lines-table-wrap {
  margin: 0 16px;
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-bottom: 0;
}

.hazard-lines-table {
  width: 100%;
  min-width: 1320px;
  table-layout: fixed;
  border-collapse: collapse;
}

.hazard-lines-table th,
.hazard-lines-table td {
  height: 48px;
  padding: 8px 10px;
  color: #475569;
  font-size: 13px;
  font-weight: 500;
  line-height: 20px;
  vertical-align: middle;
  border-right: 1px solid #edf2f7;
  border-bottom: 1px solid #edf2f7;
}

.hazard-lines-table th {
  color: #475569;
  font-weight: 700;
  text-align: left;
  background: #f8fafc;
}

.hazard-lines-table th:last-child,
.hazard-lines-table td:last-child {
  border-right: 0;
}

.hazard-lines-text {
  display: -webkit-box;
  overflow: hidden;
  color: #111827;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.hazard-lines-media-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.hazard-lines-image {
  width: 42px;
  height: 34px;
  overflow: hidden;
  border: 1px solid #d8e1ea;
  border-radius: 3px;
}

.hazard-lines-image :deep(img) {
  width: 42px;
  height: 34px;
  object-fit: cover;
}

.hazard-lines-empty {
  color: #94a3b8;
}

.hazard-lines-summary-row td {
  height: 44px;
  color: #64748b;
  background: #fff;
}

.hazard-lines-footer {
  padding: 10px 18px 14px;
  color: #111827;
  font-size: 12px;
  font-weight: 600;
  text-align: right;
}

.safety-check-lines .hazard-lines-header h3 {
  color: #334155;
}

.safety-check-lines .hazard-lines-table th,
.safety-check-lines .hazard-lines-table td {
  color: #475569;
}

.safety-check-lines .hazard-lines-text,
.safety-check-lines .hazard-lines-footer {
  color: #111827;
}

@media (max-width: 900px) {
  .detail-meta-strip,
  .detail-field-sections,
  .detail-field-grid {
    grid-template-columns: 1fr;
  }

  .detail-flow-list li,
  .detail-history-list {
    grid-template-columns: 1fr;
  }

  .detail-flow-list em {
    text-align: left;
  }

  .detail-attachment-row,
  .detail-field-section--content,
  .detail-field-section--media,
  .detail-field--wide {
    grid-column: auto;
  }

  .detail-attachment-row {
    grid-template-columns: 1fr;
  }

  .meeting-detail--safety-check .detail-field-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: none;
    grid-auto-flow: row;
  }

  .meeting-detail--safety-check .detail-field {
    grid-column: auto;
    grid-row: auto;
  }

  .meeting-detail--safety-check .detail-field:nth-child(n + 4) {
    border-left: 0;
  }

  .meeting-detail--safety-check .detail-field:nth-child(2n) {
    border-left: 1px solid #e8eef6;
  }
}

@media (max-width: 640px) {
  .meeting-detail--safety-check .detail-field-grid {
    grid-template-columns: 1fr;
  }

  .meeting-detail--safety-check .detail-field {
    border-left: 0;
  }
}

@media print {
  :global(body *) {
    visibility: hidden;
  }

  .meeting-detail,
  .meeting-detail * {
    visibility: visible;
  }

  .meeting-detail {
    position: fixed;
    inset: 0;
    display: block;
    padding: 0;
    background: #fff;
  }

  .detail-action-bar,
  .attachment-actions {
    display: none !important;
  }
}

.attachment-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.attachment-actions label {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  padding: 6px 10px;
  color: #1d4ed8;
  font-weight: 700;
  cursor: pointer;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 4px;
}

.attachment-actions label svg {
  width: 15px;
  height: 15px;
}

.attachment-actions input {
  display: none;
}

.attachment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.detail-attachment-card {
  min-height: 146px;
}

.detail-attachment-file-list {
  display: grid;
  gap: 8px;
  padding: 0;
  margin: 10px 0 0;
  list-style: none;
}

.detail-attachment-file-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  min-height: 36px;
  padding: 7px 10px;
  background: #f8fafc;
  border: 1px solid #d8e1ea;
  border-radius: 4px;
}

.detail-attachment-file-kind {
  min-width: 44px;
  padding: 2px 6px;
  color: #1f4f82;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  background: #e6f0fb;
  border-radius: 4px;
}

.detail-attachment-file-name {
  overflow: hidden;
  color: #334155;
  font-size: 13px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-field-section--media .attachment-grid {
  grid-template-columns: repeat(auto-fill, minmax(96px, 112px));
}

.attachment-preview {
  position: relative;
  min-width: 0;
}

.attachment-delete-button {
  position: absolute;
  right: 8px;
  top: 8px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  color: #fff;
  cursor: pointer;
  background: rgb(15 23 42 / 72%);
  border: 0;
  border-radius: 4px;
}

.attachment-delete-button svg {
  width: 14px;
  height: 14px;
}

.attachment-image {
  width: 100%;
  height: 96px;
  overflow: hidden;
  border: 1px solid #d8e1ea;
  border-radius: 4px;
}

.attachment-image :deep(img) {
  width: 100%;
  height: 96px;
  object-fit: cover;
}

.attachment-video {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  width: 100%;
  height: 96px;
  padding: 0;
  overflow: hidden;
  color: #fff;
  cursor: pointer;
  background: #111827;
  border: 1px solid #d8e1ea;
  border-radius: 4px;
}

.attachment-video video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.74;
}

.attachment-video span {
  position: relative;
  z-index: 1;
  width: 100%;
  padding: 6px 0;
  font-size: 12px;
  text-align: center;
  background: rgb(15 23 42 / 62%);
}

.attachment-name {
  margin-top: 5px;
  overflow: hidden;
  color: #475569;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-empty {
  margin-top: 8px;
  color: #94a3b8;
}

.video-preview-player {
  display: block;
  width: 100%;
  max-height: 70vh;
  background: #000;
  border-radius: 4px;
}

@media (max-width: 900px) {
  .workbench__bar {
    flex-wrap: wrap;
    overflow: visible;
  }

  .filter-item {
    flex-basis: calc(50% - 5px);
  }
}
</style>
