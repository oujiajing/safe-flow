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
  getThreeCheckRecordDetailApi,
  getThreeCheckRecordFlowApi,
  getThreeCheckRecordsApi,
  getThreeCheckRecordWorkflowApi,
  openThreeCheckRectificationOrderApi,
  remindThreeCheckRecordApi,
  submitThreeCheckRecordApi,
  uploadThreeCheckRecordAttachmentApi,
  updateThreeCheckRecordApi,
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
  createOrganizationFilterState,
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
} from '../../pre-shift-meeting/pre-shift-meeting.data';
import {
  canDeleteThreeCheckRecord,
  canOpenThreeCheckCreateDialog,
  canRemindThreeCheckRecord,
  canSubmitThreeCheckRecord,
  canUploadThreeCheckAttachment,
  canViewThreeCheckRecord,
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
} from '../../pre-shift-meeting/pre-shift-meeting.view-state';
import {
  createOneShiftThreeCheckDetailEditForm as createThreeCheckDetailEditForm,
  createOneShiftThreeCheckRecordUpdatePayloadFromDetail as createThreeCheckRecordUpdatePayloadFromDetail,
  createOneShiftThreeCheckWorkflowPanelState,
  isOneShiftThreeCheckDetailEditable as isThreeCheckDetailEditable,
} from './detail-adapter';
import {
  applyLocalThreeCheckAction,
  createLocalThreeCheckRow,
  createPreShiftMeetingRecordPayload,
  formatThreeCheckPayloadDisplayValue,
  getPreShiftMeetingSafetyConfirmColumns,
  getThreeCheckAttachmentColumnKind,
  getThreeCheckDetailActions,
  getThreeCheckDetailDisplayColumns,
  getThreeCheckDetailFieldGroups,
  getThreeCheckTableDisplayColumns,
  getThreeCheckStatusBadgeTone,
  getThreeCheckFieldOptions,
  getThreeCheckStatusFilterOptions,
  getTeamCheckInspectionColumns,
  getTeamCheckInspectionStage,
  getOneShiftVisibleOrganizationFilterKeys,
  isThreeCheckAttachmentColumn,
  isThreeCheckAttachmentMissing,
  isThreeCheckMultiSelectField,
  isThreeCheckStatusBoxField,
  isOneShiftOrganizationFieldLocked,
  isOneShiftTeamMemberRole,
  isTeamCheckInspectionRequiredColumn,
  isTeamCheckInspectionRoute,
  normalizePreShiftMeetingSafetyConfirmLines,
  normalizeTeamCheckInspectionLines,
  normalizeThreeCheckStatusBoxLabel,
  resolveOneShiftThreeCheckRuntime as resolvePreShiftMeetingRuntime,
  serializePreShiftMeetingSafetyConfirmPayload,
  serializeTeamCheckInspectionPayload,
  shouldShowOneShiftDataMap,
  validatePreShiftMeetingMediaChecks,
  validatePreShiftMeetingSafetyConfirmResults,
  validateTeamCheckInspectionResults,
} from './module-config';
import type { OneShiftOrganizationFilterKey } from './module-config';
import type {
  PreShiftMeetingSafetyConfirmLine,
  TeamCheckInspectionLine,
  ThreeCheckDetailActionKey,
  ThreeCheckTableColumn,
} from './module-config';

import type {
  MeetingFilters,
  MeetingRow,
  OrganizationNode,
  OrganizationId,
} from '../../pre-shift-meeting/pre-shift-meeting.data';
import {
  createSharedThreeCheckWorkbenchState,
  THREE_CHECK_LIST_AUTO_REFRESH_INTERVAL_MS,
} from '#/views/pingan/shared/three-check-workbench-state';
import StatusFilterActions from '#/views/pingan/shared/StatusFilterActions.vue';

type ThreeCheckTableRow = MeetingRow & Record<string, any>;
type WorkbenchStatusFilter = NonNullable<MeetingFilters['status']>;
type DetailAttachment =
  | PinganPreShiftMeetingApi.MeetingDetail['attachments'][number]
  | PinganThreeCheckRecordApi.RecordDetail['attachments'][number];
type OneShiftFlowStageKey =
  | 'keySiteAcceptance'
  | 'keySiteConfirmation'
  | 'keySiteRectification'
  | 'keySiteRegistration'
  | 'midShiftInspection'
  | 'postShiftInspection'
  | 'preShiftSafetyActivity'
  | 'preShiftInspection'
  | 'preShiftMeeting'
  | 'teamDispatch';

interface OneShiftFlowStageConfig {
  icon: string;
  key: OneShiftFlowStageKey;
  label: string;
  moduleKeys: string[];
  shortLabel: string;
}

interface OneShiftFlowLinkContext {
  businessDate: string;
  companyId?: PinganThreeCheckRecordApi.Id;
  currentModuleKey: string;
  currentRecordId: string;
  departmentId?: PinganThreeCheckRecordApi.Id;
  recordNo: string;
  rootDispatchRecordId?: string;
  teamId?: PinganThreeCheckRecordApi.Id;
}

interface DocumentFlowField {
  kind?: 'IMAGE' | 'STATUS' | 'TEXT' | 'VIDEO';
  label: string;
  value: string;
}

interface OneShiftFlowNode {
  current: boolean;
  date: string;
  empty: boolean;
  fields: DocumentFlowField[];
  id: string;
  icon: string;
  moduleKey?: string;
  owner: string;
  record?: PinganThreeCheckRecordApi.RecordRow;
  sequence: number;
  stage: OneShiftFlowStageConfig;
  status: string;
  subtitle: string;
  title: string;
}

interface DocumentFlowStep {
  date: string;
  label: string;
  owner: string;
  recordNo: string;
  state: 'current' | 'done' | 'pending';
  status: string;
  statusTitle: string;
  timeText: string;
}

interface OneShiftChangeHistoryItem {
  action: string;
  afterValue?: string;
  beforeValue?: string;
  createdAt: string;
  fieldKey: string;
  fieldLabel?: string;
  id: string;
  operatorId?: PinganThreeCheckRecordApi.Id | null;
  operatorName?: string;
  remark?: string | null;
  valueType?: 'IMAGE' | 'STATUS' | 'TEXT' | 'VIDEO' | string;
  version: number;
}

interface ChangeHistoryGroup {
  action: string;
  attachmentCount: number;
  count: number;
  displayVersion: number;
  fieldCount: number;
  flowCount: number;
  id: string;
  items: OneShiftChangeHistoryItem[];
  operator: string;
  time: string;
  version: number;
}

const oneShiftFlowStageConfigs: OneShiftFlowStageConfig[] = [
  {
    icon: 'lucide:calendar-clock',
    key: 'teamDispatch',
    label: '班组派班',
    moduleKeys: ['team-dispatch'],
    shortLabel: '派班',
  },
  {
    icon: 'lucide:users-round',
    key: 'preShiftMeeting',
    label: '班前会',
    moduleKeys: ['pre-shift-meeting'],
    shortLabel: '班前会',
  },
  {
    icon: 'lucide:clipboard-check',
    key: 'preShiftInspection',
    label: '班前检查',
    moduleKeys: ['pre-shift-inspection'],
    shortLabel: '班前',
  },
  {
    icon: 'lucide:hard-hat',
    key: 'midShiftInspection',
    label: '班中检查',
    moduleKeys: ['mid-shift-inspection'],
    shortLabel: '班中',
  },
  {
    icon: 'lucide:clipboard-list',
    key: 'postShiftInspection',
    label: '班后检查',
    moduleKeys: ['post-shift-inspection'],
    shortLabel: '班后',
  },
];

const preShiftSafetyActivityFlowStageConfig: OneShiftFlowStageConfig = {
  icon: 'lucide:shield-check',
  key: 'preShiftSafetyActivity',
  label: '班前安全活动',
  moduleKeys: ['pre-shift-safety-activity'],
  shortLabel: '安全活动',
};

const keySiteFlowStageConfigs: OneShiftFlowStageConfig[] = [
  {
    icon: 'lucide:map-pinned',
    key: 'keySiteRegistration',
    label: '登记检查',
    moduleKeys: ['key-sites'],
    shortLabel: '登记',
  },
  {
    icon: 'lucide:scan-search',
    key: 'keySiteConfirmation',
    label: '隐患确认',
    moduleKeys: ['key-sites'],
    shortLabel: '确认',
  },
  {
    icon: 'lucide:wrench',
    key: 'keySiteRectification',
    label: '整改跟进',
    moduleKeys: ['key-sites'],
    shortLabel: '整改',
  },
  {
    icon: 'lucide:badge-check',
    key: 'keySiteAcceptance',
    label: '验收关闭',
    moduleKeys: ['key-sites'],
    shortLabel: '验收',
  },
];

const allDocumentFlowStageConfigs = [
  ...oneShiftFlowStageConfigs,
  preShiftSafetyActivityFlowStageConfig,
  ...keySiteFlowStageConfigs,
];

const oneShiftRouteNameByModuleKey: Record<string, string> = {
  'curtain-wall-team-dispatch': 'PinganCurtainWallTeamDispatch',
  'key-sites': 'PinganKeySites',
  'mid-shift-inspection': 'PinganMidShiftInspection',
  'post-shift-inspection': 'PinganPostShiftInspection',
  'pre-shift-safety-activity': 'PinganPreShiftSafetyActivity',
  'pre-shift-inspection': 'PinganPreShiftInspection',
  'pre-shift-meeting': 'PreShiftMeeting',
  'team-dispatch': 'PinganTeamDispatch',
};

const GUANGSHENG_CURTAIN_WALL_COMPANY_ID = 3;
const GUANGSHENG_CURTAIN_WALL_COMPANY_NAME = '演示公司';
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
const createUploadFiles = reactive<{
  IMAGE: File[];
  VIDEO: File[];
}>({
  IMAGE: [],
  VIDEO: [],
});
const createUploadPreviewUrls = reactive<{
  IMAGE: string[];
  VIDEO: string[];
}>({
  IMAGE: [],
  VIDEO: [],
});

const route = useRoute();
const accessStore = useAccessStore();
const userStore = useUserStore();
const createRootDispatchLoading = ref(false);
const createRootDispatchOptions = ref<Array<{ label: string; value: string }>>([]);
const createRootDispatchRecordId = ref<string>();
const detailWorkflow = ref<PinganThreeCheckRecordApi.RecordWorkflow>();
const detailWorkflowError = ref('');
const detailWorkflowLoading = ref(false);
const teamCheckInspectionResultOptions = [
  { label: '无隐患', value: '无隐患' },
  { label: '有隐患', value: '有隐患' },
];
const safetyConfirmLines = ref<PreShiftMeetingSafetyConfirmLine[]>([]);
const safetyConfirmLoading = ref(false);
const safetyConfirmStatusOptions = [{ label: '已确认', value: '已确认' }];
const documentFlowLogsExpanded = ref(false);
const documentFlowLinkContext = ref<OneShiftFlowLinkContext>();
const documentFlowStageRows = ref(
  createEmptyDocumentFlowStageRows(),
);
const documentFlowDetailCache = reactive<
  Record<string, PinganThreeCheckRecordApi.RecordDetail | undefined>
>({});
const documentFlowWorkflowCache = reactive<
  Record<string, PinganThreeCheckRecordApi.RecordWorkflow | undefined>
>({});
const changeHistoryLogsExpanded = ref(false);
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
const activeDetailPanel = ref<'changeHistory' | 'documentFlow'>();
let listAutoRefreshTimer: number | undefined;

const filteredTree = computed(() =>
  filterOrganizationTree(dataMapOrganizationNodes.value, treeKeyword.value),
);

const allCompanySelectOptions = computed(() =>
  getCascadedOrganizationOptions(formOrganizationNodes.value, undefined, [
    'COMPANY',
  ]).map((item) => ({
    label: item.label,
    value: item.value,
  })),
);

const preShiftSafetyActivityCompanyOption = computed(
  () =>
    allCompanySelectOptions.value.find(
      (item) => String(item.value) === String(GUANGSHENG_CURTAIN_WALL_COMPANY_ID),
    ) ??
    allCompanySelectOptions.value.find(
      (item) => item.label === GUANGSHENG_CURTAIN_WALL_COMPANY_NAME,
    ) ?? {
      label: GUANGSHENG_CURTAIN_WALL_COMPANY_NAME,
      value: GUANGSHENG_CURTAIN_WALL_COMPANY_ID,
    },
);

const companySelectOptions = computed(() =>
  isPreShiftSafetyActivityModule.value
    ? [preShiftSafetyActivityCompanyOption.value]
    : allCompanySelectOptions.value,
);

const departmentSelectOptions = computed(() =>
  getCascadedOrganizationOptions(
    formOrganizationNodes.value,
    createForm.companyId,
    ['DEPARTMENT'],
  ).map((item) => ({
    label: item.label,
    value: item.value,
  })),
);

const teamSelectOptions = computed(() =>
  getCascadedOrganizationOptions(
    formOrganizationNodes.value,
    createForm.departmentId,
    ['TEAM'],
  ).map((item) => ({
    label: item.label,
    value: item.value,
  })),
);

const filterCompanySelectOptions = computed(() =>
  isPreShiftSafetyActivityModule.value
    ? [preShiftSafetyActivityCompanyOption.value]
    : allCompanySelectOptions.value,
);

const filterDepartmentSelectOptions = computed(() =>
  getCascadedOrganizationOptions(
    formOrganizationNodes.value,
    filters.companyId,
    ['DEPARTMENT'],
  ).map((item) => ({
    label: item.label,
    value: item.value,
  })),
);

const filterTeamSelectOptions = computed(() =>
  getCascadedOrganizationOptions(
    formOrganizationNodes.value,
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
const currentUserRoles = computed(() => userStore.userInfo?.roles ?? []);
const currentUserOrgDefaults = computed(() =>
  resolveCreateOrganizationDefaults(
    createOrganizationFilterState(),
    formOrganizationNodes.value,
    userStore.userInfo?.orgId,
  ),
);
const isTeamMemberWorkbench = computed(() =>
  isOneShiftTeamMemberRole(currentUserRoles.value),
);
const visibleOrganizationFilterKeys = computed(() =>
  getOneShiftVisibleOrganizationFilterKeys(currentUserRoles.value),
);
function isOrganizationFilterVisible(field: OneShiftOrganizationFilterKey) {
  return visibleOrganizationFilterKeys.value.includes(field);
}
function isOrganizationFieldLocked(field: OneShiftOrganizationFilterKey) {
  return isOneShiftOrganizationFieldLocked(currentUserRoles.value, field);
}
const columns = computed<TableColumnsType<ThreeCheckTableRow>>(() =>
  getThreeCheckTableDisplayColumns(
    moduleRuntime.value.routeName,
    moduleRuntime.value.columns,
    { roles: currentUserRoles.value },
  ).map((column: ThreeCheckTableColumn) => ({
      ...column,
      customCell: () => ({
        class: {
          ...createThreeCheckTableBodyCellClass(column),
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
  moduleRuntime.value.columns.filter(
    (column) =>
      column.dataIndex !== 'actions' &&
      (!shouldShowCreateAttachmentUpload.value ||
        !isCreateAttachmentUploadColumn(column)),
  ),
);
const localDetailColumns = computed(() =>
  getThreeCheckDetailDisplayColumns(
    moduleRuntime.value.routeName,
    moduleRuntime.value.columns,
  ),
);
const localDetailFieldColumns = computed(() =>
  localDetailColumns.value.filter(
    (column) => {
      const fieldName = columnDataIndexFieldName(column.dataIndex);
      return (
        fieldName !== 'dispatchStatus' &&
        fieldName !== 'status' &&
        !isDetailAttachmentColumn(column)
      );
    },
  ),
);
const detailFieldGroups = computed(() =>
  getThreeCheckDetailFieldGroups(
    moduleRuntime.value.routeName,
    localDetailFieldColumns.value,
  ),
);
const detailActions = computed(() => getThreeCheckDetailActions());
const detailModalCentered = computed(() => isThreeCheckRecordModule.value);
const detailModalWidth = computed(() =>
  isTeamCheckInspectionDetail.value || isPreShiftMeetingSafetyConfirmDetail.value
    ? 'min(1560px, calc(100vw - 24px))'
    : isThreeCheckRecordModule.value
      ? 'min(1080px, calc(100vw - 32px))'
      : 760,
);
const detailModalWrapClass = computed(() =>
  isThreeCheckRecordModule.value
    ? 'pingan-detail-modal pingan-detail-modal--one-shift'
    : 'pingan-detail-modal',
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
const threeCheckModuleKey = computed(() => moduleRuntime.value.moduleKey ?? '');
const isPreShiftSafetyActivityModule = computed(
  () => threeCheckModuleKey.value === 'pre-shift-safety-activity',
);
const shouldSelectRootDispatchForCreate = computed(() =>
  [
    'mid-shift-inspection',
    'post-shift-inspection',
    'pre-shift-inspection',
    'pre-shift-meeting',
  ].includes(threeCheckModuleKey.value),
);
const createUploadSupportedModuleKeys = [
  'key-sites',
  'mid-shift-inspection',
  'post-shift-inspection',
  'pre-shift-safety-activity',
  'pre-shift-inspection',
  'pre-shift-meeting',
];
const shouldShowCreateAttachmentUpload = computed(() =>
  createUploadSupportedModuleKeys.includes(threeCheckModuleKey.value),
);
const shouldShowDataMap = computed(() =>
  shouldShowOneShiftDataMap(currentUserRoles.value),
);
const canUseWorkbench = computed(
  () => moduleRuntime.value.createMode !== 'DISABLED',
);
const canCreateRecord = computed(
  () =>
    !isTeamMemberWorkbench.value &&
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
const canSubmitRecord = computed(() =>
  !isTeamMemberWorkbench.value &&
  canSubmitThreeCheckRecord(
    moduleRuntime.value.routeName,
    accessStore.accessCodes ?? [],
  ),
);
const canWithdrawRecord = computed(() =>
  !isTeamMemberWorkbench.value &&
  canWithdrawThreeCheckRecord(
    accessStore.accessCodes ?? [],
    moduleRuntime.value.routeName,
  ),
);
const canRemindRecord = computed(() =>
  !isTeamMemberWorkbench.value &&
  canRemindThreeCheckRecord(
    accessStore.accessCodes ?? [],
    moduleRuntime.value.routeName,
  ),
);
const canDeleteRecord = computed(() =>
  !isTeamMemberWorkbench.value &&
  canDeleteThreeCheckRecord(
    moduleRuntime.value.routeName,
    accessStore.accessCodes ?? [],
  ),
);
const canCreateRectificationOrder = computed(() =>
  !isTeamMemberWorkbench.value &&
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
  () => hasSelectedRows.value && canDeleteRecord.value,
);
const tableRowSelection = computed<TableProps<ThreeCheckTableRow>['rowSelection']>(
  () =>
    isTeamMemberWorkbench.value
      ? undefined
      : {
          getCheckboxProps: () => ({
            disabled: !isThreeCheckRecordModule.value,
          }),
          onChange: (keys) => {
            selectedRowKeys.value = keys.map(String);
          },
          preserveSelectedRowKeys: true,
          selectedRowKeys: selectedRowKeys.value,
        },
);
const canEditCurrentThreeCheckDetail = computed(() =>
  !isTeamMemberWorkbench.value &&
  isThreeCheckDetailEditable(currentThreeCheckDetail.value),
);
const isTeamCheckInspectionDetail = computed(() =>
  isTeamCheckInspectionRoute(moduleRuntime.value.routeName),
);
const isPreShiftMeetingSafetyConfirmDetail = computed(
  () =>
    isThreeCheckRecordModule.value &&
    moduleRuntime.value.routeName === 'PreShiftMeeting',
);
const teamCheckInspectionColumns = computed(() =>
  getTeamCheckInspectionColumns(moduleRuntime.value.routeName),
);
const safetyConfirmColumns = computed(() =>
  getPreShiftMeetingSafetyConfirmColumns(),
);
const teamCheckInspectionTableTitle = computed(() => {
  const titleMap: Record<string, string> = {
    PinganMidShiftInspection: '班中检查项',
    PinganPostShiftInspection: '班后检查项',
    PinganPreShiftInspection: '班前检查项',
  };
  return titleMap[moduleRuntime.value.routeName] ?? '检查项';
});
const fullChainDocumentFlowModuleKeys = [
  'curtain-wall-team-dispatch',
  'key-sites',
  'mid-shift-inspection',
  'post-shift-inspection',
  'pre-shift-inspection',
  'pre-shift-safety-activity',
  'pre-shift-meeting',
  'team-dispatch',
];
const isFullChainDocumentFlowModule = computed(() =>
  fullChainDocumentFlowModuleKeys.includes(
    currentThreeCheckDetail.value?.moduleKey ?? '',
  ),
);
const currentDocumentFlowStage = computed(() =>
  flowStageForModuleKey(currentThreeCheckDetail.value?.moduleKey),
);
const isPreShiftMeetingDocumentFlow = computed(
  () => currentDocumentFlowStage.value?.key === 'preShiftMeeting',
);
const isPreShiftSafetyActivityDocumentFlow = computed(
  () => currentDocumentFlowStage.value?.key === 'preShiftSafetyActivity',
);
const isKeySiteDocumentFlow = computed(
  () => currentThreeCheckDetail.value?.moduleKey === 'key-sites',
);
const isInspectionDocumentFlow = computed(() =>
  ['midShiftInspection', 'postShiftInspection', 'preShiftInspection'].includes(
    currentDocumentFlowStage.value?.key ?? '',
  ),
);
const isStageSpecificDocumentFlow = computed(
  () =>
    isPreShiftMeetingDocumentFlow.value ||
    isInspectionDocumentFlow.value ||
    isPreShiftSafetyActivityDocumentFlow.value ||
    isKeySiteDocumentFlow.value,
);
const dispatchStatusColumn = computed(() =>
  localDetailColumns.value.find(
    (column) => columnDataIndexFieldName(column.dataIndex) === 'dispatchStatus',
  ),
);
const detailTopStatusLabel = computed(() => {
  const detail = currentThreeCheckDetail.value;
  if (!detail) {
    return '';
  }
  if (dispatchStatusColumn.value) {
    return threeCheckDetailValue(dispatchStatusColumn.value);
  }
  return getRecordStatusLabel(detail);
});
const detailTopStatusTitle = computed(() =>
  dispatchStatusColumn.value?.title ?? '状态',
);
const detailWorkflowPanelState = computed(() =>
  createOneShiftThreeCheckWorkflowPanelState(detailWorkflow.value),
);
const detailDocumentFlowItems = computed(
  () => detailWorkflowPanelState.value.documentFlowItems,
);
const detailChangeHistoryItems = computed(
  () => detailWorkflowPanelState.value.changeHistoryItems,
);
const documentFlowStageConfigs = computed(() =>
  isPreShiftSafetyActivityDocumentFlow.value
    ? [preShiftSafetyActivityFlowStageConfig]
    : isKeySiteDocumentFlow.value
      ? keySiteFlowStageConfigs
    : oneShiftFlowStageConfigs,
);
const documentFlowNodes = computed<OneShiftFlowNode[]>(() =>
  documentFlowStageConfigs.value.map((stage, index) =>
    createOneShiftFlowNode(stage, index),
  ),
);
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
      node.record?.recordNo,
      node.record?.company,
      node.record?.department,
      node.record?.team,
      ...node.fields.flatMap((field) => [field.label, field.value]),
    ]
      .join(' ')
      .toLowerCase()
      .includes(keyword),
  );
});
const documentFlowTreeMasterNode = computed(() => filteredDocumentFlowNodes.value[0]);
const documentFlowTreeDetailNodes = computed(() =>
  filteredDocumentFlowNodes.value.slice(1),
);
const documentFlowSelectedNode = computed(() => {
  const nodes = filteredDocumentFlowNodes.value.length
    ? filteredDocumentFlowNodes.value
    : documentFlowNodes.value;
  return (
    nodes.find((node) => node.id === documentFlowSelectedNodeId.value) ??
    nodes[0]
  );
});
const documentFlowSelectedDetail = computed(() => {
  const node = documentFlowSelectedNode.value;
  if (!node?.record) {
    return undefined;
  }
  return documentFlowDetailCache[documentFlowRecordCacheKey(node.record)];
});
const currentDocumentFlowStatusLogs = computed(() => {
  const node = documentFlowSelectedNode.value;
  if (!node?.record) {
    return [];
  }
  return (
    documentFlowWorkflowCache[documentFlowRecordCacheKey(node.record)]
      ?.documentFlow ?? []
  );
});
const visibleDocumentFlowStatusLogs = computed(() =>
  documentFlowLogsExpanded.value
    ? currentDocumentFlowStatusLogs.value
    : currentDocumentFlowStatusLogs.value.slice(0, 3),
);
const canToggleDocumentFlowLogs = computed(
  () => currentDocumentFlowStatusLogs.value.length > 3,
);
const documentFlowCompletionCount = computed(
  () => documentFlowNodes.value.filter(documentFlowIsCompletedStatus).length,
);
const documentFlowPendingCount = computed(
  () => documentFlowNodes.value.length - documentFlowCompletionCount.value,
);
const documentFlowSelectedAttachments = computed(
  () => documentFlowSelectedDetail.value?.attachments ?? [],
);
const keySiteFlowStatusLabel = computed(() => {
  const detail = currentThreeCheckDetail.value;
  return (
    detail?.statusLabel ||
    (detail?.status ? getStatusLabel(detail.status) : undefined) ||
    '待检查'
  );
});
const keySiteCurrentStageKey = computed<OneShiftFlowStageKey>(() => {
  const statusLabel = statusBoxLabel(keySiteFlowStatusLabel.value);
  if (statusLabel === '已验收') {
    return 'keySiteAcceptance';
  }
  if (statusLabel === '待验收') {
    return 'keySiteAcceptance';
  }
  if (statusLabel === '待整改') {
    return 'keySiteRectification';
  }
  return 'keySiteConfirmation';
});
const documentFlowOpenedStageNode = computed(() => {
  if (isKeySiteDocumentFlow.value) {
    return documentFlowNodes.value.find(
      (node) => node.stage.key === keySiteCurrentStageKey.value,
    );
  }
  const currentStage = flowStageForModuleKey(currentThreeCheckDetail.value?.moduleKey);
  return documentFlowNodes.value.find((node) => node.stage.key === currentStage?.key);
});
const documentFlowCurrentStageNode = computed(
  () =>
    documentFlowOpenedStageNode.value ??
    documentFlowNodes.value.find((node) => !documentFlowIsCompletedStatus(node)) ??
    documentFlowNodes.value.find((node) => node.current) ??
    documentFlowSelectedNode.value,
);
const documentFlowModalTitle = computed(() =>
  isKeySiteDocumentFlow.value
    ? '重点场所单据流洞察'
    : isStageSpecificDocumentFlow.value && currentDocumentFlowStage.value
      ? `${currentDocumentFlowStage.value.label}单据流洞察`
      : '一班三查全链路洞察',
);
const documentFlowSidebarTitle = computed(() =>
  isKeySiteDocumentFlow.value
    ? '场所透视树'
    : isStageSpecificDocumentFlow.value
      ? '流程透视树'
      : '订单透视树',
);
const documentFlowSidebarSubtitle = computed(() =>
  isKeySiteDocumentFlow.value
    ? '重点场所 / 检查记录 / 整改验收'
    : isPreShiftSafetyActivityDocumentFlow.value
      ? '演示公司 / 班前安全活动'
      : isStageSpecificDocumentFlow.value
        ? '班组派班 / 班前会 / 三查记录'
        : '作业排程 / 班组派班',
);
const documentFlowDetailTitle = computed(() =>
  documentFlowSelectedNode.value
    ? `${documentFlowSelectedNode.value.stage.label}单据明细`
    : '单据明细',
);
const documentFlowSummary = computed(() => {
  const selected = documentFlowSelectedNode.value;
  const current = documentFlowCurrentStageNode.value;
  const completionPercent = Math.round(
    (documentFlowCompletionCount.value / documentFlowNodes.value.length) * 100,
  );
  return [
    {
      icon: 'lucide:route',
      label: isKeySiteDocumentFlow.value ? '处置完成度' : '链路完成度',
      tone: 'blue',
      value: `${completionPercent}%`,
    },
    {
      icon: 'lucide:map-pinned',
      label: '当前节点',
      tone: 'blue',
      value: current?.stage.label ?? '未记录',
    },
    isKeySiteDocumentFlow.value
      ? {
          icon: 'lucide:badge-check',
          label: '验收状态',
          tone: statusBoxLabel(keySiteFlowStatusLabel.value) === '已验收'
            ? 'green'
            : 'orange',
          value: keySiteFlowStatusLabel.value,
        }
      : isStageSpecificDocumentFlow.value
      ? {
          icon: isPreShiftMeetingDocumentFlow.value
            ? 'lucide:calendar-check'
            : 'lucide:clipboard-check',
          label: isPreShiftSafetyActivityDocumentFlow.value
            ? '活动状态'
            : isPreShiftMeetingDocumentFlow.value
              ? '会议状态'
              : '检查状态',
          tone:
            ['已开会议', '已检查'].includes(
              currentThreeCheckDetail.value?.statusLabel ?? '',
            )
              ? 'green'
              : 'orange',
          value: documentFlowBusinessStatus(
            currentDocumentFlowStage.value ?? oneShiftFlowStageConfigs[0]!,
            currentThreeCheckDetail.value?.statusLabel ||
              (currentThreeCheckDetail.value?.status
                ? getStatusLabel(currentThreeCheckDetail.value.status)
                : undefined),
          ),
        }
      : {
          icon: 'lucide:circle-alert',
          label: '待处理节点',
          tone: 'orange',
          value: String(documentFlowPendingCount.value),
        },
    {
      icon: isKeySiteDocumentFlow.value ? 'lucide:building-2' : 'lucide:user-round',
      label: isKeySiteDocumentFlow.value ? '责任部门' : '责任人',
      tone: 'default',
      value: isKeySiteDocumentFlow.value
        ? String(
            currentThreeCheckDetail.value?.payload?.responsibleDepartment ??
              selected?.record?.payload?.responsibleDepartment ??
              selected?.record?.department ??
              '未记录',
          )
        : selected?.owner || '未记录',
    },
    {
      icon: 'lucide:paperclip',
      label: '附件数',
      tone: 'default',
      value: String(documentFlowSelectedAttachments.value.length),
    },
  ];
});
const documentFlowSteps = computed<DocumentFlowStep[]>(() => {
  const currentNode = documentFlowCurrentStageNode.value;
  return documentFlowNodes.value.map((node) => ({
    date: node.date,
    label: node.stage.label,
    owner: node.owner,
    recordNo: node.record?.recordNo ?? '--',
    state:
      currentNode?.id === node.id
        ? 'current'
        : documentFlowIsCompletedStatus(node)
          ? 'done'
          : 'pending',
    status: node.status,
    statusTitle: documentFlowStageStatusLabel(node.stage),
    timeText: documentFlowStepTime(node),
  }));
});
const documentFlowSubtitle = computed(
  () => {
    const context = documentFlowLinkContext.value;
    const detail = currentThreeCheckDetail.value;
    const team = detail?.team || '未记录班组';
    const date = context?.businessDate || detail?.businessDate || detail?.date || '未记录日期';
    if (isKeySiteDocumentFlow.value) {
      return `重点场所 · ${date} · 当前节点：重点场所检查`;
    }
    if (isStageSpecificDocumentFlow.value && currentDocumentFlowStage.value) {
      return `${team} · ${date} · 当前节点：${currentDocumentFlowStage.value.label}`;
    }
    return `${team} · ${date}`;
  },
);
const documentFlowAttachments = computed(
  () => documentFlowSelectedAttachments.value,
);
const changeHistoryItems = computed<OneShiftChangeHistoryItem[]>(() =>
  (detailWorkflow.value?.changeHistory ?? []).flatMap((item, index, items) => {
    const version = Math.max(items.length - index, 1);
    const fieldItems: OneShiftChangeHistoryItem[] = [
      {
        action: item.action,
        afterValue:
          item.toStatusLabel ||
          item.toStatus ||
          item.actionLabel ||
          item.action,
        beforeValue: item.fromStatusLabel || item.fromStatus || '',
        createdAt: item.occurredAt,
        fieldKey: 'status',
        fieldLabel: '状态',
        id: `${item.id}-status`,
        operatorId: item.operatorId,
        operatorName: item.operatorName,
        remark: item.remark,
        valueType: 'STATUS',
        version,
      },
    ];
    if (item.remark?.trim()) {
      fieldItems.push({
        action: item.action,
        afterValue: item.remark.trim(),
        beforeValue: '',
        createdAt: item.occurredAt,
        fieldKey: 'remark',
        fieldLabel: '备注',
        id: `${item.id}-remark`,
        operatorId: item.operatorId,
        operatorName: item.operatorName,
        remark: item.remark,
        valueType: 'TEXT',
        version,
      });
    }
    return fieldItems;
  }),
);
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
  const groupMap = new Map<string, OneShiftChangeHistoryItem[]>();
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
      operator: items[0]?.operatorName || String(items[0]?.operatorId ?? '系统'),
      time: formatChangeHistoryTime(items[0]?.createdAt),
      version: items[0]?.version ?? 0,
    };
  });
  const sortedVersions = [...new Set(groups.map((group) => group.version))].sort(
    (left, right) => left - right,
  );
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
const visibleChangeHistoryGroups = computed(() =>
  changeHistoryLogsExpanded.value
    ? changeHistoryGroups.value
    : changeHistoryGroups.value.slice(0, 3),
);
const canToggleChangeHistoryLogs = computed(
  () => changeHistoryGroups.value.length > 3,
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
      value: latest?.operatorName || String(latest?.operatorId ?? '未记录'),
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
const changeHistorySubtitle = computed(
  () => `${moduleRuntime.value.title} · 字段级追溯`,
);
const changeHistoryAttachmentItems = computed(() =>
  filteredChangeHistoryItems.value.filter(
    (item) =>
      item.action === 'ATTACHMENT' ||
      ['IMAGE', 'VIDEO'].includes(item.valueType || ''),
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

function createEmptyDocumentFlowStageRows() {
  return Object.fromEntries(
    allDocumentFlowStageConfigs.map((stage) => [stage.key, undefined]),
  ) as Record<
    OneShiftFlowStageKey,
    PinganThreeCheckRecordApi.RecordRow | undefined
  >;
}

function documentFlowRecordCacheKey(
  record: Pick<PinganThreeCheckRecordApi.RecordRow, 'id' | 'moduleKey'>,
) {
  return `${record.moduleKey}:${record.id}`;
}

function flowStageForModuleKey(moduleKey?: string) {
  return allDocumentFlowStageConfigs.find((stage) =>
    stage.moduleKeys.includes(moduleKey ?? ''),
  );
}

function documentFlowStageStatusLabel(stage: OneShiftFlowStageConfig) {
  return stage.key === 'teamDispatch' ? '派班状态' : '状态';
}

function documentFlowBusinessStatus(
  stage: OneShiftFlowStageConfig,
  statusLabel?: string,
) {
  const normalizedLabel = statusBoxLabel(statusLabel);
  if (stage.key === 'keySiteRegistration') {
    return '已登记';
  }
  if (stage.key === 'keySiteConfirmation') {
    return normalizedLabel === '待检查' ? '待确认' : '已确认';
  }
  if (stage.key === 'keySiteRectification') {
    if (['已验收', '待验收'].includes(normalizedLabel)) {
      return '已整改';
    }
    return '待整改';
  }
  if (stage.key === 'keySiteAcceptance') {
    return normalizedLabel === '已验收' ? '已验收' : '待验收';
  }
  if (stage.key === 'teamDispatch') {
    if (['已生效', '生效', 'OPENED'].includes(normalizedLabel)) {
      return '生效';
    }
    return '不生效';
  }
  if (['preShiftMeeting', 'preShiftSafetyActivity'].includes(stage.key)) {
    return normalizedLabel === '已开会议' ? '已开会议' : '待开会议';
  }
  return normalizedLabel === '已检查' ? '已检查' : '待检查';
}

function documentFlowStatusText(status?: string) {
  return status || '待处理';
}

function normalizeDocumentFlowDateTime(value?: string, fallbackDate?: string) {
  const rawValue = value?.trim();
  if (rawValue) {
    const normalizedValue = rawValue.replace('T', ' ');
    const dateTimeMatch = normalizedValue.match(
      /^(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})(?::(\d{2}))?/,
    );
    if (dateTimeMatch) {
      return `${dateTimeMatch[1]} ${dateTimeMatch[2]}:${dateTimeMatch[3] ?? '00'}`;
    }
    const timeMatch = normalizedValue.match(/^(\d{2}:\d{2})(?::(\d{2}))?/);
    if (timeMatch && fallbackDate && fallbackDate !== '--') {
      return `${fallbackDate} ${timeMatch[1]}:${timeMatch[2] ?? '00'}`;
    }
  }
  return fallbackDate && fallbackDate !== '--' ? `${fallbackDate} 00:00:00` : '--';
}

function documentFlowStepTime(node: OneShiftFlowNode) {
  if (node.stage.key !== 'teamDispatch') {
    return node.date;
  }
  const rowRecord = node.record as unknown as Record<string, unknown> | undefined;
  const payload = (node.record?.payload ?? {}) as Record<string, unknown>;
  const dispatchTime = String(rowRecord?.dispatchTime ?? payload.dispatchTime ?? '');
  return normalizeDocumentFlowDateTime(dispatchTime, node.date);
}

function documentFlowRecordStatusSource(
  stage: OneShiftFlowStageConfig,
  row: PinganThreeCheckRecordApi.RecordRow,
  detail?: PinganThreeCheckRecordApi.RecordDetail,
) {
  if (stage.moduleKeys.includes('key-sites')) {
    return keySiteFlowStatusLabel.value;
  }
  if (stage.key === 'teamDispatch') {
    const payload = ((detail ?? row).payload ?? {}) as Record<string, unknown>;
    const detailRecord = detail as unknown as Record<string, unknown> | undefined;
    const rowRecord = row as unknown as Record<string, unknown>;
    return String(
      detailRecord?.dispatchStatus ??
        rowRecord.dispatchStatus ??
        payload.dispatchStatus ??
        detail?.statusLabel ??
        row.statusLabel ??
        getStatusLabel(row.status),
    );
  }
  return detail?.statusLabel || row.statusLabel || getStatusLabel(row.status);
}

function documentFlowIsCompletedStatus(node: OneShiftFlowNode) {
  return [
    '已登记',
    '已确认',
    '已整改',
    '已验收',
    '已开会议',
    '已检查',
    '生效',
  ].includes(node.status);
}

function orderedFlowModuleKeys(stage: OneShiftFlowStageConfig) {
  return stage.moduleKeys;
}

function createDocumentFlowLinkContext(
  detail?: PinganThreeCheckRecordApi.RecordDetail,
): OneShiftFlowLinkContext | undefined {
  const businessDate = detail?.businessDate || detail?.date;
  if (!detail || !businessDate) {
    return undefined;
  }
  return {
    businessDate,
    companyId: detail.companyId,
    currentModuleKey: detail.moduleKey,
    currentRecordId: detail.id,
    departmentId: detail.departmentId,
    recordNo: detail.recordNo,
    rootDispatchRecordId: detail.rootDispatchRecordId,
    teamId: detail.teamId,
  };
}

function detailToFlowRow(
  detail: PinganThreeCheckRecordApi.RecordDetail,
): PinganThreeCheckRecordApi.RecordRow {
  return {
    businessDate: detail.businessDate,
    canCreateRectificationOrder: detail.canCreateRectificationOrder,
    canRemind: detail.canRemind,
    canSubmit: detail.canSubmit,
    canWithdraw: detail.canWithdraw,
    company: detail.company,
    companyId: detail.companyId,
    date: detail.date,
    department: detail.department,
    departmentId: detail.departmentId,
    id: detail.id,
    imageCheck: detail.imageCheck,
    moduleKey: detail.moduleKey,
    owner: detail.owner,
    ownerUserId: detail.ownerUserId,
    overdue: detail.overdue,
    payload: detail.payload,
    recordNo: detail.recordNo,
    rootDispatchRecordId: detail.rootDispatchRecordId,
    sourceChannel: detail.sourceChannel as PinganThreeCheckRecordApi.SourceChannel,
    status: detail.status,
    statusLabel: detail.statusLabel,
    taskId: detail.taskId,
    team: detail.team,
    teamId: detail.teamId,
    version: detail.version,
    videoCheck: detail.videoCheck,
  };
}

function pickFlowStageRecord(
  stage: OneShiftFlowStageConfig,
  items: PinganThreeCheckRecordApi.RecordRow[],
) {
  const current = currentThreeCheckDetail.value;
  if (current && stage.moduleKeys.includes(current.moduleKey)) {
    const matchedCurrent = items.find(
      (item) => item.moduleKey === current.moduleKey && item.id === current.id,
    );
    if (matchedCurrent) {
      return matchedCurrent;
    }
  }
  return items[0];
}

function createOneShiftFlowNode(
  stage: OneShiftFlowStageConfig,
  index: number,
): OneShiftFlowNode {
  const row = documentFlowStageRows.value[stage.key];
  const detail = row
    ? documentFlowDetailCache[documentFlowRecordCacheKey(row)]
    : undefined;
  const current = Boolean(
    row &&
      (isKeySiteDocumentFlow.value
        ? stage.key === keySiteCurrentStageKey.value
        : row.id === currentThreeCheckDetail.value?.id) &&
      row.moduleKey === currentThreeCheckDetail.value?.moduleKey,
  );
  const status = row
    ? documentFlowBusinessStatus(stage, documentFlowRecordStatusSource(stage, row, detail))
    : documentFlowBusinessStatus(stage);
  const date = row?.businessDate || row?.date || '--';
  const owner = detail?.owner || row?.owner || '未分配';

  return {
    current,
    date,
    empty: !row,
    fields: createDocumentFlowNodeFields(stage, row, detail),
    id: `flow-${stage.key}`,
    icon: stage.icon,
    moduleKey: row?.moduleKey,
    owner,
    record: row,
    sequence: index + 1,
    stage,
    status,
    subtitle: keySiteNodeSubtitle(stage, row, owner, date, status),
    title: keySiteNodeTitle(stage, row),
  };
}

function keySiteNodeTitle(
  stage: OneShiftFlowStageConfig,
  row?: PinganThreeCheckRecordApi.RecordRow,
) {
  if (!stage.moduleKeys.includes('key-sites')) {
    return row ? `[${stage.label}] ${row.recordNo}` : `[${stage.label}] --`;
  }
  const payload = (row?.payload ?? {}) as Record<string, unknown>;
  const primary =
    stage.key === 'keySiteRegistration'
      ? payload.siteType
      : stage.key === 'keySiteConfirmation'
        ? payload.riskDescription || payload.siteType
        : stage.key === 'keySiteRectification'
          ? payload.rectificationRequirement || payload.riskDescription
          : payload.acceptanceConclusion || payload.siteType;
  return `[${stage.label}] ${String(primary || row?.recordNo || '--')}`;
}

function keySiteNodeSubtitle(
  stage: OneShiftFlowStageConfig,
  row: PinganThreeCheckRecordApi.RecordRow | undefined,
  owner: string,
  date: string,
  status: string,
) {
  if (!stage.moduleKeys.includes('key-sites')) {
    return row
      ? `${row.recordNo} | ${owner} | ${date}`
      : `${documentFlowStageStatusLabel(stage)}：${status} | ${owner} | ${date}`;
  }
  const payload = (row?.payload ?? {}) as Record<string, unknown>;
  const department = String(
    payload.responsibleDepartment || row?.department || owner || '未记录',
  );
  return row
    ? `状态：${status} | ${department} | ${date}`
    : `状态：${status} | 未分配 | ${date}`;
}

function createDocumentFlowNodeFields(
  stage: OneShiftFlowStageConfig,
  row?: PinganThreeCheckRecordApi.RecordRow,
  detail?: PinganThreeCheckRecordApi.RecordDetail,
): DocumentFlowField[] {
  if (!row) {
    const context = documentFlowLinkContext.value;
    return [
      { label: '节点', value: stage.label },
      {
        label: documentFlowStageStatusLabel(stage),
        kind: 'STATUS',
        value: documentFlowBusinessStatus(stage),
      },
      { label: '公司', value: currentThreeCheckDetail.value?.company || '未记录' },
      { label: '部门', value: currentThreeCheckDetail.value?.department || '未记录' },
      { label: '班组', value: currentThreeCheckDetail.value?.team || '未记录' },
      { label: '业务日期', value: context?.businessDate || '未记录' },
      { label: '关联规则', value: '公司 + 部门 + 班组 + 业务日期' },
    ];
  }

  const columns = documentFlowDetailColumns(row.moduleKey);
  return columns.map((column) => {
    const fieldName = columnDataIndexFieldName(column.dataIndex);
    return {
      kind: documentFlowFieldKind(column),
      label: column.title,
      value:
        fieldName === 'dispatchStatus'
          ? documentFlowBusinessStatus(
              stage,
              documentFlowFieldValue(column, detail, row),
            )
          : fieldName === 'status'
            ? documentFlowBusinessStatus(
                stage,
                detail?.statusLabel || row.statusLabel || getStatusLabel(row.status),
              )
            : documentFlowFieldValue(column, detail, row),
    };
  });
}

function activeMeetingFilters(): MeetingFilters {
  return {
    company: filters.company,
    dateRange:
      filters.dateStart || filters.dateEnd
        ? [filters.dateStart, filters.dateEnd]
        : undefined,
    department: filters.department,
    organizationKey: selectedOrganizationKeys.value[0],
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
    owner: payload.owner || row.owner || payload.responsiblePerson || '',
    overdue: row.overdue,
    payload,
    recordNo: row.recordNo,
    rootDispatchRecordId: row.rootDispatchRecordId,
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
    enforcePreShiftSafetyActivityCompany();
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
        { id: 2, orgId: 4, realName: '班组负责人', username: 'HB_MONITOR' },
        { id: 3, orgId: 4, realName: '安全员', username: 'MQ_SAFE' },
        { id: 4, orgId: 8, realName: '制氧主管', username: 'ZY_SUPERVISOR' },
      ];
    }
  }
}

async function loadMeetings() {
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
      organizationId: selectedOrganizationKeys.value[0]
        ? getOrganizationIdByKey(
            dataMapOrganizationNodes.value,
            selectedOrganizationKeys.value[0],
          )
        : undefined,
      page: 1,
      pageSize: 20,
      overdue: overdue || undefined,
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
  filters.status = 'all';
  filters.team = '';
  filters.teamId = undefined;
  selectedOrganizationKeys.value = [];
  clearSelectedRows();
  enforcePreShiftSafetyActivityCompany();
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

function attachmentPreviewFor(record: Record<string, any>, column: unknown) {
  const previewUrl =
    attachmentKind(column) === 'VIDEO'
      ? record.videoPreviewUrl
      : record.imagePreviewUrl;
  return resolveAttachmentPreviewUrl(previewUrl);
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

function isCreateAttachmentUploadColumn(column: ThreeCheckTableColumn) {
  return ['imageCheck', 'imageUpload', 'videoCheck', 'videoUpload'].includes(
    column.dataIndex,
  );
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

function fieldOptions(column: ThreeCheckTableColumn) {
  return getThreeCheckFieldOptions(
    moduleRuntime.value.routeName,
    column.dataIndex,
  );
}

function localFieldOptions(column: ThreeCheckTableColumn) {
  return fieldOptions(column);
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
    'inspectionContent',
    'meetingContent',
    'remarks',
    'workContent',
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
    companySelectOptions.value,
    createForm.companyId,
  );
  localCreateForm.department = optionLabel(
    departmentSelectOptions.value,
    createForm.departmentId,
  );
  localCreateForm.team = optionLabel(teamSelectOptions.value, createForm.teamId);
}

function rootDispatchOptionLabel(row: PinganThreeCheckRecordApi.RecordRow) {
  const payload = (row.payload ?? {}) as Record<string, unknown>;
  return [
    row.moduleKey === 'curtain-wall-team-dispatch' ? '派班' : '班组派班',
    payload.dispatchTime || payload.dispatchDate || row.businessDate,
    payload.teamTask || payload.dispatchType || row.owner,
  ]
    .filter(Boolean)
    .join(' / ');
}

async function loadCreateRootDispatchCandidates() {
  if (!shouldSelectRootDispatchForCreate.value) {
    createRootDispatchOptions.value = [];
    createRootDispatchRecordId.value = undefined;
    return;
  }
  const dispatchModules = [
    { moduleKey: 'team-dispatch', routeName: 'PinganTeamDispatch' },
    {
      moduleKey: 'curtain-wall-team-dispatch',
      routeName: 'PinganCurtainWallTeamDispatch',
    },
  ].filter((item) =>
    canViewThreeCheckRecord(item.routeName, accessStore.accessCodes ?? []),
  );
  if (dispatchModules.length === 0) {
    createRootDispatchOptions.value = [];
    createRootDispatchRecordId.value = undefined;
    return;
  }
  const businessDate = resolveThreeCheckRecordBusinessDate();
  if (
    !createForm.companyId ||
    !createForm.departmentId ||
    !createForm.teamId ||
    !businessDate
  ) {
    createRootDispatchOptions.value = [];
    createRootDispatchRecordId.value = undefined;
    return;
  }
  createRootDispatchLoading.value = true;
  try {
    const results = await Promise.all(
      dispatchModules.map((item) =>
        getThreeCheckRecordsApi(item.moduleKey, {
          companyId: createForm.companyId,
          dateEnd: businessDate,
          dateStart: businessDate,
          departmentId: createForm.departmentId,
          page: 1,
          pageSize: 50,
          status: 'all',
          teamId: createForm.teamId,
        }),
      ),
    );
    const options = results
      .flatMap((result) => result.items)
      .map((row) => ({
        label: rootDispatchOptionLabel(row),
        value: row.rootDispatchRecordId || row.id,
      }));
    createRootDispatchOptions.value = options;
    if (!options.some((option) => option.value === createRootDispatchRecordId.value)) {
      createRootDispatchRecordId.value =
        options.length === 1 ? options[0]?.value : undefined;
    }
  } finally {
    createRootDispatchLoading.value = false;
  }
}

function handleCreateDispatchContextChange() {
  syncLocalOrganizationFields();
  void loadCreateRootDispatchCandidates();
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
    case 'imageUpload':
    case 'safetyActivityRecordUpload': {
      return '未上传';
    }
    case 'owner':
    case 'responsiblePerson': {
      return ownerName;
    }
    case 'status': {
      const options = localFieldOptions(column);
      return options.at(-1)?.value ?? '待提交';
    }
    case 'workContent': {
      return localFieldOptions(column)
        .slice(0, 7)
        .map((option) => option.value);
    }
    case 'videoCheck':
    case 'videoUpload': {
      return '未上传';
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

  for (const column of localCreateColumns.value) {
    localCreateForm[column.dataIndex] = defaultLocalCreateValue(column, now);
  }
  syncLocalOrganizationFields();
}

function organizationValuesFor(parentId: OrganizationId, orgTypes: string[]) {
  return getCascadedOrganizationOptions(
    formOrganizationNodes.value,
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

function applyScopedOrganizationDefaults(target: {
  companyId?: OrganizationId;
  departmentId?: OrganizationId;
  teamId?: OrganizationId;
}) {
  const defaults = currentUserOrgDefaults.value;
  if (
    isOrganizationFieldLocked('company') &&
    isOrganizationId(defaults.companyId)
  ) {
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
  if (!formOrganizationNodes.value.length) {
    return;
  }
  applyScopedOrganizationDefaults(filters);
  applyScopedOrganizationDefaults(createForm);
  if (!shouldShowDataMap.value) {
    selectedOrganizationKeys.value = [];
  }
  syncLocalOrganizationFields();
}

function handleFilterCompanyChange(value: unknown) {
  if (isOrganizationFieldLocked('company')) {
    enforceUserOrganizationScope();
    return;
  }
  if (isPreShiftSafetyActivityModule.value) {
    enforcePreShiftSafetyActivityCompany();
    return;
  }
  applyOrganizationFilterCompany(
    filters,
    formOrganizationNodes.value,
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
    formOrganizationNodes.value,
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
    formOrganizationNodes.value,
    normalizeSelectId(value),
  );
}

function handleDataMapSelect(keys: unknown[]) {
  selectedOrganizationKeys.value = keys.filter(
    (key): key is string => typeof key === 'string',
  );
  const selectedKey = selectedOrganizationKeys.value[0];
  applyOrganizationFilterFromDataMapSelection(
    filters,
    dataMapOrganizationNodes.value,
    formOrganizationNodes.value,
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

function enforcePreShiftSafetyActivityCompany() {
  if (!isPreShiftSafetyActivityModule.value) {
    return;
  }
  const companyId = preShiftSafetyActivityCompanyOption.value.value;
  filters.companyId = companyId;
  filters.company = preShiftSafetyActivityCompanyOption.value.label;
  createForm.companyId = companyId;
  syncDepartmentAndTeam(companyId);
  syncLocalOrganizationFields();
  selectedOrganizationKeys.value = [];
}

function handleCompanyChange(value: unknown) {
  if (isOrganizationFieldLocked('company')) {
    enforceUserOrganizationScope();
    return;
  }
  if (isPreShiftSafetyActivityModule.value) {
    enforcePreShiftSafetyActivityCompany();
    return;
  }
  const companyId = normalizeSelectId(value);
  if (isOrganizationId(companyId)) {
    syncDepartmentAndTeam(companyId);
    handleCreateDispatchContextChange();
  }
}

function handleDepartmentChange(value: unknown) {
  if (isOrganizationFieldLocked('department')) {
    enforceUserOrganizationScope();
    return;
  }
  const departmentId = normalizeSelectId(value);
  if (isOrganizationId(departmentId)) {
    createForm.teamId =
      firstOptionValue(organizationValuesFor(departmentId, ['TEAM'])) ??
      departmentId;
    handleCreateDispatchContextChange();
  }
}

function handleTeamChange() {
  if (isOrganizationFieldLocked('team')) {
    enforceUserOrganizationScope();
    return;
  }
  handleCreateDispatchContextChange();
}

function revokeCreateAttachmentPreview(fileKind: 'IMAGE' | 'VIDEO') {
  for (const previewUrl of createUploadPreviewUrls[fileKind]) {
    URL.revokeObjectURL(previewUrl);
  }
  createUploadPreviewUrls[fileKind] = [];
}

function resetCreateAttachmentFiles() {
  revokeCreateAttachmentPreview('IMAGE');
  revokeCreateAttachmentPreview('VIDEO');
  createUploadFiles.IMAGE = [];
  createUploadFiles.VIDEO = [];
}

function handleCreateAttachmentChange(
  fileKind: 'IMAGE' | 'VIDEO',
  event: Event,
) {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  if (files.length === 0) {
    return;
  }
  revokeCreateAttachmentPreview(fileKind);
  createUploadFiles[fileKind] = files;
  createUploadPreviewUrls[fileKind] = files.map((file) =>
    URL.createObjectURL(file),
  );
  input.value = '';
}

function removeCreateAttachment(fileKind: 'IMAGE' | 'VIDEO') {
  revokeCreateAttachmentPreview(fileKind);
  createUploadFiles[fileKind] = [];
}

function createAttachmentImagePreviewUrls() {
  return createUploadPreviewUrls.IMAGE;
}

function createAttachmentVideoPreviewUrls() {
  return createUploadPreviewUrls.VIDEO;
}

async function uploadCreateAttachments(recordId: string) {
  if (!shouldShowCreateAttachmentUpload.value) {
    return;
  }
  for (const file of createUploadFiles.IMAGE) {
    await uploadThreeCheckRecordAttachmentApi(
      threeCheckModuleKey.value,
      recordId,
      'IMAGE',
      file,
    );
  }
  for (const file of createUploadFiles.VIDEO) {
    await uploadThreeCheckRecordAttachmentApi(
      threeCheckModuleKey.value,
      recordId,
      'VIDEO',
      file,
    );
  }
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
    formOrganizationNodes.value,
    userStore.userInfo?.orgId ??
      firstOptionValue(companySelectOptions.value) ??
      createForm.companyId,
  );
  createForm.companyId = organizationDefaults.companyId ?? createForm.companyId;
  createForm.departmentId =
    organizationDefaults.departmentId ?? createForm.departmentId;
  createForm.teamId = organizationDefaults.teamId ?? createForm.teamId;
  applyScopedOrganizationDefaults(createForm);
  createForm.ownerUserId =
    resolveCurrentOwnerUserId();
  createForm.attendeeNames = attendeeSelectOptions.value.some(
    (item) => item.value === '班组负责人',
  )
    ? ['班组负责人']
    : attendeeSelectOptions.value.slice(0, 1).map((item) => item.value);
  createRootDispatchRecordId.value = undefined;
  resetCreateAttachmentFiles();
  if (
    moduleRuntime.value.createMode === 'LOCAL_TABLE' ||
    isThreeCheckRecordModule.value
  ) {
    seedLocalCreateForm(now);
  }
  enforcePreShiftSafetyActivityCompany();
  enforceUserOrganizationScope();
  createOpen.value = true;
  void loadCreateRootDispatchCandidates();
}

function resolveThreeCheckRecordBusinessDate() {
  const rawValue =
    localCreateForm.date ||
    localCreateForm.dispatchDate ||
    localCreateForm.createdAt ||
    createForm.meetingDate;
  return String(rawValue).slice(0, 10);
}

function resolveThreeCheckRecordStatus() {
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
  delete payload.team;
  return payload;
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
      if (
        shouldSelectRootDispatchForCreate.value &&
        createRootDispatchOptions.value.length > 1 &&
        !createRootDispatchRecordId.value
      ) {
        message.error('同一班组同一天存在多条派班，请选择归属派班');
        return;
      }
      const createdDetail = await createThreeCheckRecordApi(threeCheckModuleKey.value, {
        businessDate: resolveThreeCheckRecordBusinessDate(),
        companyId,
        departmentId,
        ownerUserId: createForm.ownerUserId,
        payload: createThreeCheckRecordPayload(),
        rootDispatchRecordId: createRootDispatchRecordId.value,
        status: resolveThreeCheckRecordStatus(),
        teamId,
      });
      await uploadCreateAttachments(createdDetail.id);
      message.success(workbenchText.value.createSuccessText);
      createOpen.value = false;
      resetCreateAttachmentFiles();
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
  syncTeamCheckInspectionLinesFromDetail();
  syncSafetyConfirmLinesFromDetail();
}

function startThreeCheckDetailEdit() {
  seedThreeCheckDetailEditForm();
  detailEditing.value = true;
}

function cancelThreeCheckDetailEdit() {
  detailEditing.value = false;
  seedThreeCheckDetailEditForm();
}

function resetDetailWorkflow() {
  detailWorkflow.value = undefined;
  detailWorkflowError.value = '';
  detailWorkflowLoading.value = false;
}

function resetDocumentFlowChain() {
  documentFlowLinkContext.value = undefined;
  documentFlowStageRows.value = createEmptyDocumentFlowStageRows();
  Object.keys(documentFlowDetailCache).forEach((key) => {
    delete documentFlowDetailCache[key];
  });
  Object.keys(documentFlowWorkflowCache).forEach((key) => {
    delete documentFlowWorkflowCache[key];
  });
  documentFlowSelectedNodeId.value = '';
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

function safetyConfirmPayload(detail = currentThreeCheckDetail.value) {
  return (detail?.payload ?? {}) as Record<string, unknown>;
}

function safetyConfirmSnapshotLines(
  detail = currentThreeCheckDetail.value,
) {
  return normalizePreShiftMeetingSafetyConfirmLines(
    safetyConfirmPayload(detail).safetyConfirmItems,
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

async function resolveSafetyConfirmLinesFromTemplate(
  detail: PinganThreeCheckRecordApi.RecordDetail,
) {
  if (!detail.companyId) {
    return [];
  }
  const template = await getTeamCheckTemplateResolveApi({
    companyOrgId: detail.companyId,
    departmentOrgId: detail.departmentId,
    stage: 'PRE_SHIFT_MEETING_CONFIRMATION',
    teamOrgId: detail.teamId,
  });
  return normalizePreShiftMeetingSafetyConfirmLines(template?.items ?? []);
}

async function resolveSafetyConfirmLinesForDetail(
  detail: PinganThreeCheckRecordApi.RecordDetail,
) {
  const snapshotLines = safetyConfirmSnapshotLines(detail);
  if (snapshotLines.length) {
    return snapshotLines;
  }
  return resolveSafetyConfirmLinesFromTemplate(detail);
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

async function loadSafetyConfirmLinesFromDetail() {
  if (
    !isPreShiftMeetingSafetyConfirmDetail.value ||
    !currentThreeCheckDetail.value
  ) {
    safetyConfirmLines.value = [];
    return;
  }
  safetyConfirmLoading.value = true;
  try {
    safetyConfirmLines.value = await resolveSafetyConfirmLinesForDetail(
      currentThreeCheckDetail.value,
    );
  } catch {
    safetyConfirmLines.value = [];
    message.warning('班前会安全确认事项加载失败，请稍后重试');
  } finally {
    safetyConfirmLoading.value = false;
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

function syncSafetyConfirmLinesFromDetail() {
  if (
    !isPreShiftMeetingSafetyConfirmDetail.value ||
    !currentThreeCheckDetail.value
  ) {
    safetyConfirmLines.value = [];
    return;
  }
  const snapshotLines = safetyConfirmSnapshotLines();
  if (snapshotLines.length) {
    safetyConfirmLines.value = snapshotLines;
  }
}

function validateTeamCheckInspectionLineResults() {
  if (!isTeamCheckInspectionDetail.value) {
    return true;
  }
  const result = validateTeamCheckInspectionResults(
    teamCheckInspectionLines.value,
    moduleRuntime.value.routeName,
  );
  if (!result.valid) {
    message.error(result.message || '请填写检查结果');
    return false;
  }
  return true;
}

function validateSafetyConfirmLineResults() {
  if (!isPreShiftMeetingSafetyConfirmDetail.value) {
    return true;
  }
  const result = validatePreShiftMeetingSafetyConfirmResults(
    safetyConfirmLines.value,
  );
  if (!result.valid) {
    message.error(result.message || '请确认安全确认事项');
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

function sameSafetyConfirmLines(
  detail: PinganThreeCheckRecordApi.RecordDetail,
  lines: PreShiftMeetingSafetyConfirmLine[],
) {
  return (
    JSON.stringify(safetyConfirmSnapshotLines(detail)) ===
    JSON.stringify(lines)
  );
}

function createTeamCheckInspectionUpdatePayload(
  detail: PinganThreeCheckRecordApi.RecordDetail,
  lines: TeamCheckInspectionLine[],
) {
  const payload: PinganThreeCheckRecordApi.RecordPayload = {
    businessDate: detail.businessDate,
    companyId: detail.companyId!,
    departmentId: detail.departmentId!,
    ownerUserId: detail.ownerUserId,
    payload: serializeTeamCheckInspectionPayload(
      teamCheckInspectionPayload(detail),
      lines,
    ),
    status: detail.status,
    teamId: detail.teamId!,
    version: detail.version,
  };
  if (detail.rootDispatchRecordId) {
    payload.rootDispatchRecordId = detail.rootDispatchRecordId;
  }
  return payload;
}

function createSafetyConfirmUpdatePayload(
  detail: PinganThreeCheckRecordApi.RecordDetail,
  lines: PreShiftMeetingSafetyConfirmLine[],
) {
  const payload: PinganThreeCheckRecordApi.RecordPayload = {
    businessDate: detail.businessDate,
    companyId: detail.companyId!,
    departmentId: detail.departmentId!,
    ownerUserId: detail.ownerUserId,
    payload: serializePreShiftMeetingSafetyConfirmPayload(
      safetyConfirmPayload(detail),
      lines,
    ),
    status: detail.status,
    teamId: detail.teamId!,
    version: detail.version,
  };
  if (detail.rootDispatchRecordId) {
    payload.rootDispatchRecordId = detail.rootDispatchRecordId;
  }
  return payload;
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
  const result = validateTeamCheckInspectionResults(
    lines,
    moduleRuntime.value.routeName,
  );
  if (!result.valid) {
    message.error(result.message || '请填写检查结果');
    return false;
  }
  if (!sameTeamCheckInspectionLines(detail, lines)) {
    currentThreeCheckDetail.value = await updateThreeCheckRecordApi(
      threeCheckModuleKey.value,
      recordId,
      createTeamCheckInspectionUpdatePayload(detail, lines),
    );
    teamCheckInspectionLines.value = lines;
  }
  return true;
}

async function prepareSafetyConfirmForSubmit(recordId: string) {
  if (!isPreShiftMeetingSafetyConfirmDetail.value) {
    return true;
  }
  const detail = await getThreeCheckRecordDetailApi(
    threeCheckModuleKey.value,
    recordId,
  );
  const lines = await resolveSafetyConfirmLinesForDetail(detail);
  const result = validatePreShiftMeetingSafetyConfirmResults(lines);
  if (!result.valid) {
    message.error(result.message || '请确认安全确认事项');
    return false;
  }
  if (!sameSafetyConfirmLines(detail, lines)) {
    currentThreeCheckDetail.value = await updateThreeCheckRecordApi(
      threeCheckModuleKey.value,
      recordId,
      createSafetyConfirmUpdatePayload(detail, lines),
    );
    safetyConfirmLines.value = lines;
  }
  return true;
}

async function loadCurrentThreeCheckWorkflow() {
  if (!currentThreeCheckDetail.value) {
    resetDetailWorkflow();
    return;
  }
  detailWorkflowLoading.value = true;
  detailWorkflowError.value = '';
  try {
    detailWorkflow.value = await getThreeCheckRecordWorkflowApi(
      threeCheckModuleKey.value,
      currentThreeCheckDetail.value.id,
    );
  } catch {
    detailWorkflow.value = undefined;
    detailWorkflowError.value = '单据流和变更历史加载失败';
  } finally {
    detailWorkflowLoading.value = false;
  }
}

async function loadDocumentFlowFromRootDispatch(
  rootDispatchRecordId: string,
  currentDetail: PinganThreeCheckRecordApi.RecordDetail,
) {
  const flow = await getThreeCheckRecordFlowApi(rootDispatchRecordId);
  const nextRows = createEmptyDocumentFlowStageRows();
  for (const stage of flow.stages) {
    const stageConfig = oneShiftFlowStageConfigs.find(
      (config) => config.key === stage.stageKey,
    );
    if (stageConfig && stage.record) {
      nextRows[stageConfig.key] = stage.record;
    }
  }
  const currentRow = detailToFlowRow(currentDetail);
  const currentStage = flowStageForModuleKey(currentDetail.moduleKey);
  if (currentStage) {
    nextRows[currentStage.key] = currentRow;
    documentFlowDetailCache[documentFlowRecordCacheKey(currentRow)] =
      currentDetail;
  }
  documentFlowStageRows.value = nextRows;
}

async function loadDocumentFlowStageRecords() {
  const currentDetail = currentThreeCheckDetail.value;
  const context = createDocumentFlowLinkContext(currentDetail);
  if (!currentDetail || !context) {
    resetDocumentFlowChain();
    return;
  }

  documentFlowLinkContext.value = context;

  if (context.rootDispatchRecordId && isFullChainDocumentFlowModule.value) {
    await loadDocumentFlowFromRootDispatch(
      context.rootDispatchRecordId,
      currentDetail,
    );
    return;
  }

  const nextRows = createEmptyDocumentFlowStageRows();

  if (isKeySiteDocumentFlow.value) {
    const currentRow = detailToFlowRow(currentDetail);
    for (const stage of keySiteFlowStageConfigs) {
      nextRows[stage.key] = currentRow;
    }
    documentFlowDetailCache[documentFlowRecordCacheKey(currentRow)] =
      currentDetail;
    documentFlowStageRows.value = nextRows;
    return;
  }

  if (isPreShiftSafetyActivityDocumentFlow.value) {
    nextRows.preShiftSafetyActivity = detailToFlowRow(currentDetail);
    documentFlowDetailCache[
      documentFlowRecordCacheKey(nextRows.preShiftSafetyActivity)
    ] = currentDetail;
    documentFlowStageRows.value = nextRows;
    return;
  }

  await Promise.all(
    oneShiftFlowStageConfigs.map(async (stage) => {
      for (const moduleKey of orderedFlowModuleKeys(stage)) {
        if (moduleKey === currentDetail.moduleKey) {
          nextRows[stage.key] = detailToFlowRow(currentDetail);
          documentFlowDetailCache[documentFlowRecordCacheKey(nextRows[stage.key]!)] =
            currentDetail;
          break;
        }

        const result = await getThreeCheckRecordsApi(moduleKey, {
          companyId: context.companyId,
          dateEnd: context.businessDate,
          dateStart: context.businessDate,
          departmentId: context.departmentId,
          page: 1,
          pageSize: 20,
          teamId: context.teamId,
        });
        const picked = pickFlowStageRecord(stage, result.items);
        if (picked) {
          nextRows[stage.key] = picked;
          break;
        }
      }
    }),
  );

  documentFlowStageRows.value = nextRows;
}

async function loadDocumentFlowNodeDetail(node?: OneShiftFlowNode) {
  if (!node?.record) {
    return;
  }
  const cacheKey = documentFlowRecordCacheKey(node.record);
  if (!documentFlowDetailCache[cacheKey]) {
    documentFlowDetailCache[cacheKey] = await getThreeCheckRecordDetailApi(
      node.record.moduleKey,
      node.record.id,
    );
  }
  if (!documentFlowWorkflowCache[cacheKey]) {
    documentFlowWorkflowCache[cacheKey] = await getThreeCheckRecordWorkflowApi(
      node.record.moduleKey,
      node.record.id,
    );
  }
}

function createCurrentThreeCheckDetailUpdatePayload() {
  if (!currentThreeCheckDetail.value) {
    return;
  }
  const payload = createThreeCheckRecordUpdatePayloadFromDetail(
    currentThreeCheckDetail.value,
    detailEditForm,
  );
  if (isTeamCheckInspectionDetail.value) {
    payload.payload = serializeTeamCheckInspectionPayload(
      payload.payload as Record<string, unknown>,
      teamCheckInspectionLines.value,
    );
  }
  if (isPreShiftMeetingSafetyConfirmDetail.value) {
    payload.payload = serializePreShiftMeetingSafetyConfirmPayload(
      payload.payload as Record<string, unknown>,
      safetyConfirmLines.value,
    );
  }
  if (!payload.companyId || !payload.departmentId || !payload.teamId) {
    message.error('明细缺少组织信息，请刷新后重试');
    return;
  }
  return payload as PinganThreeCheckRecordApi.RecordPayload;
}

async function persistPendingDetailEdits() {
  if (
    !currentThreeCheckDetail.value ||
    !detailEditing.value ||
    !canEditCurrentThreeCheckDetail.value
  ) {
    return;
  }
  const payload = createCurrentThreeCheckDetailUpdatePayload();
  if (!payload) {
    throw new Error('Missing organization fields in current detail');
  }
  currentThreeCheckDetail.value = await updateThreeCheckRecordApi(
    threeCheckModuleKey.value,
    currentThreeCheckDetail.value.id,
    payload,
  );
}

async function saveThreeCheckDetailEdit() {
  if (!currentThreeCheckDetail.value || !canEditCurrentThreeCheckDetail.value) {
    return;
  }
  if (!validateTeamCheckInspectionLineResults()) {
    return;
  }
  if (!validateSafetyConfirmLineResults()) {
    return;
  }
  const payload = createCurrentThreeCheckDetailUpdatePayload();
  if (!payload) {
    return;
  }

  detailSaving.value = true;
  try {
    currentThreeCheckDetail.value = await updateThreeCheckRecordApi(
      threeCheckModuleKey.value,
      currentThreeCheckDetail.value.id,
      payload,
    );
    detailEditing.value = false;
    seedThreeCheckDetailEditForm();
    await loadCurrentThreeCheckWorkflow();
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
  changeHistoryOpen.value = false;
  resetDetailWorkflow();
  resetDocumentFlowChain();
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
      await loadSafetyConfirmLinesFromDetail();
      await loadCurrentThreeCheckWorkflow();
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
      await loadTeamCheckInspectionLinesFromDetail();
      await loadSafetyConfirmLinesFromDetail();
    }
    await loadCurrentThreeCheckWorkflow();
    return;
  }
  if (currentDetail.value) {
    currentDetail.value = await getPreShiftMeetingDetailApi(currentDetail.value.id);
  }
}

function detailActionLabel(key: ThreeCheckDetailActionKey) {
  return detailActions.value.find((action) => action.key === key)?.label ?? key;
}

function detailActionIcon(key: ThreeCheckDetailActionKey) {
  return detailActions.value.find((action) => action.key === key)?.icon ?? '';
}

function scrollToDetailAttachments() {
  const attachmentTarget = Array.isArray(detailAttachmentSectionRef.value)
    ? detailAttachmentSectionRef.value[0]
    : detailAttachmentSectionRef.value;
  if (typeof attachmentTarget?.scrollIntoView !== 'function') {
    return;
  }
  attachmentTarget.scrollIntoView({
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

async function loadChangeHistory(showFallbackMessage = false) {
  if (!currentThreeCheckDetail.value) {
    return;
  }
  changeHistoryLoading.value = true;
  try {
    await loadCurrentThreeCheckWorkflow();
    if (detailWorkflowError.value && showFallbackMessage) {
      message.warning('变更历史加载失败，已显示当前版本信息');
    }
    if (!changeHistorySelectedGroup.value) {
      changeHistorySelectedGroupId.value = changeHistoryGroups.value[0]?.id ?? '';
    }
  } catch {
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
  changeHistoryLogsExpanded.value = false;
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

function toggleChangeHistoryLogs() {
  changeHistoryLogsExpanded.value = !changeHistoryLogsExpanded.value;
  if (!changeHistoryLogsExpanded.value) {
    const firstVisibleGroup = visibleChangeHistoryGroups.value[0];
    if (
      changeHistorySelectedGroupId.value &&
      !visibleChangeHistoryGroups.value.some(
        (group) => group.id === changeHistorySelectedGroupId.value,
      )
    ) {
      changeHistorySelectedGroupId.value = firstVisibleGroup?.id ?? '';
    }
  }
}

async function refreshChangeHistory() {
  changeHistoryLogsExpanded.value = false;
  await loadChangeHistory(true);
  if (!changeHistorySelectedGroup.value) {
    changeHistorySelectedGroupId.value = changeHistoryGroups.value[0]?.id ?? '';
  }
}

function printChangeHistory() {
  window.print();
}

async function loadDocumentFlow(showFallbackMessage = false) {
  if (!currentThreeCheckDetail.value) {
    return;
  }
  documentFlowLoading.value = true;
  try {
    await loadDocumentFlowStageRecords();
    if (!documentFlowSelectedNode.value) {
      documentFlowSelectedNodeId.value = documentFlowNodes.value[0]?.id ?? '';
    }
    await loadDocumentFlowNodeDetail(documentFlowSelectedNode.value);
  } catch {
    resetDocumentFlowChain();
    if (showFallbackMessage) {
      message.warning('单据流加载失败，已显示当前明细数据');
    }
  } finally {
    documentFlowLoading.value = false;
  }
}

async function openDocumentFlow() {
  documentFlowKeyword.value = '';
  documentFlowLogsExpanded.value = false;
  const currentStage = flowStageForModuleKey(currentThreeCheckDetail.value?.moduleKey);
  documentFlowSelectedNodeId.value = currentStage
    ? `flow-${currentStage.key}`
    : documentFlowNodes.value[0]?.id ?? '';
  documentFlowOpen.value = true;
  await loadDocumentFlow(true);
  if (!documentFlowSelectedNode.value) {
    documentFlowSelectedNodeId.value = documentFlowNodes.value[0]?.id ?? '';
  }
  await loadDocumentFlowNodeDetail(documentFlowSelectedNode.value);
}

function closeDocumentFlow() {
  documentFlowOpen.value = false;
}

async function selectDocumentFlowNode(node?: OneShiftFlowNode) {
  if (!node) {
    return;
  }
  documentFlowLogsExpanded.value = false;
  documentFlowSelectedNodeId.value = node.id;
  await loadDocumentFlowNodeDetail(node);
}

async function refreshDocumentFlow() {
  documentFlowLogsExpanded.value = false;
  await loadDocumentFlow(true);
  if (!documentFlowSelectedNode.value) {
    documentFlowSelectedNodeId.value = documentFlowNodes.value[0]?.id ?? '';
  }
  await loadDocumentFlowNodeDetail(documentFlowSelectedNode.value);
}

function exportDocumentFlow() {
  message.info('单据流导出功能待接入');
}

function printDocumentFlow() {
  window.print();
}

function toggleDocumentFlowLogs() {
  documentFlowLogsExpanded.value = !documentFlowLogsExpanded.value;
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
      if (isFullChainDocumentFlowModule.value) {
        await openDocumentFlow();
      } else {
        activeDetailPanel.value =
          activeDetailPanel.value === 'documentFlow' ? undefined : 'documentFlow';
        if (activeDetailPanel.value === 'documentFlow') {
          await loadCurrentThreeCheckWorkflow();
        }
      }
      break;
    }
    case 'attachment': {
      scrollToDetailAttachments();
      break;
    }
    case 'changeHistory': {
      await openChangeHistory();
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
  if (moduleRuntime.value.routeName === 'PreShiftMeeting') {
    const mediaResult = validatePreShiftMeetingMediaChecks(record);
    if (!mediaResult.valid) {
      message.error(mediaResult.message);
      return;
    }
  }
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
    if (
      isPreShiftMeetingSafetyConfirmDetail.value &&
      !(await prepareSafetyConfirmForSubmit(meeting.id))
    ) {
      return;
    }
    await submitThreeCheckRecordApi(threeCheckModuleKey.value, meeting.id);
  } else {
    await submitPreShiftMeetingApi(meeting.id);
  }
  message.success(workbenchText.value.submitSuccessText);
  await loadMeetings();
  if (currentThreeCheckDetail.value?.id === meeting.id) {
    await refreshCurrentDetail();
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
  if (currentThreeCheckDetail.value?.id === meeting.id) {
    await refreshCurrentDetail();
  }
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

  Modal.confirm({
    content: `将删除已选 ${selectedRowKeys.value.length} 条记录，删除后列表和统计将不再显示。`,
    okText: '删除',
    okType: 'danger',
    onOk: () => runBatchAction(action),
    title: '确认批量删除？',
  });
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
  try {
    await persistPendingDetailEdits();
    if (currentThreeCheckDetail.value) {
      const attachment = await uploadThreeCheckRecordAttachmentApi(
        threeCheckModuleKey.value,
        currentThreeCheckDetail.value.id,
        fileKind,
        file,
      );
      currentThreeCheckDetail.value = upsertAttachmentPreview(
        currentThreeCheckDetail.value,
        attachment,
      );
    } else if (currentDetail.value) {
      const attachment = await uploadPreShiftMeetingAttachmentApi(
        currentDetail.value.id,
        fileKind,
        file,
      );
      currentDetail.value = upsertAttachmentPreview(
        currentDetail.value,
        attachment,
      );
    }
    message.success(fileKind === 'IMAGE' ? '图片打卡已上传' : '视频打卡已上传');
    await refreshCurrentDetail();
    await loadMeetings();
  } finally {
    input.value = '';
  }
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
    await persistPendingDetailEdits();
    const attachment = await uploadThreeCheckRecordAttachmentApi(
      threeCheckModuleKey.value,
      currentThreeCheckDetail.value.id,
      fileKind,
      file,
    );
    const updatedDetail = upsertAttachmentPreview(
      currentThreeCheckDetail.value,
      attachment,
    );
    const fieldValue =
      attachment.url || attachment.storagePath || attachment.originalName || file.name;
    currentThreeCheckDetail.value = {
      ...updatedDetail,
      payload: {
        ...(updatedDetail.payload ?? {}),
        [column.dataIndex]: fieldValue,
      },
    };
    detailEditForm[column.dataIndex] = fieldValue;
    message.success(
      `${column.title}${fileKind === 'VIDEO' ? '视频' : '图片'}已上传`,
    );
    await loadMeetings();
  } finally {
    input.value = '';
  }
}

async function handleDeleteAttachment(attachment: DetailAttachment) {
  if (!canUploadAttachment.value) {
    return;
  }
  if (!currentDetail.value && !currentThreeCheckDetail.value) {
    return;
  }
  await persistPendingDetailEdits();
  if (currentThreeCheckDetail.value) {
    await deleteThreeCheckRecordAttachmentApi(
      threeCheckModuleKey.value,
      currentThreeCheckDetail.value.id,
      attachment.id,
    );
  } else if (currentDetail.value) {
    await deletePreShiftMeetingAttachmentApi(
      currentDetail.value.id,
      attachment.id,
    );
  }
  message.success('附件已删除');
  await refreshCurrentDetail();
  await loadMeetings();
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
    attachmentUpload: detail.attachments?.length
      ? `${detail.attachments.length}个附件`
      : '未上传',
    company: detail.company,
    date: detail.date || detail.businessDate,
    department: detail.department,
    imageCheck: detail.imageCheck,
    imageUpload: detail.imageCheck,
    owner: payload.owner || detail.owner,
    status: detail.statusLabel || getStatusLabel(detail.status),
    team: detail.team,
    videoCheck: detail.videoCheck,
    videoUpload: detail.videoCheck,
  };
  return directValues[column.dataIndex] ?? payload[column.dataIndex] ?? '未填写';
}

function documentFlowActionLabel(action?: string) {
  const labels: Record<string, string> = {
    CREATE: '创建记录',
    REMIND: '催办',
    SUBMIT: '提交检查',
    UPDATE: '更新记录',
    WITHDRAW: '撤回',
  };
  return action ? labels[action] || action : '流程记录';
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

function changeHistoryFieldLabel(item: OneShiftChangeHistoryItem) {
  const column = localDetailColumns.value.find(
    (column) => column.dataIndex === item.fieldKey,
  );
  if (!item.fieldLabel || item.fieldLabel === item.fieldKey) {
    return column?.title || item.fieldKey;
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

function changeHistoryTypeLabel(item: OneShiftChangeHistoryItem) {
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

function documentFlowDetailColumns(moduleKey?: string) {
  const routeName = oneShiftRouteNameByModuleKey[moduleKey ?? ''];
  if (!routeName) {
    return localDetailColumns.value.filter(
      (column) => {
        const fieldName = columnDataIndexFieldName(column.dataIndex);
        return (
          fieldName !== 'dispatchStatus' &&
          fieldName !== 'status' &&
          !isDetailAttachmentColumn(column)
        );
      },
    );
  }
  const runtime = resolvePreShiftMeetingRuntime(
    routeName,
    oneShiftFlowStageConfigs.find((stage) =>
      stage.moduleKeys.includes(moduleKey ?? ''),
    )?.label ?? routeName,
  );
  return getThreeCheckDetailDisplayColumns(
    runtime.routeName,
    runtime.columns,
  ).filter(
    (column) => {
      const fieldName = columnDataIndexFieldName(column.dataIndex);
      return (
        fieldName !== 'dispatchStatus' &&
        fieldName !== 'status' &&
        !isThreeCheckAttachmentColumn(column)
      );
    },
  );
}

function documentFlowFieldValue(
  column: ThreeCheckTableColumn,
  detail?: PinganThreeCheckRecordApi.RecordDetail,
  row?: PinganThreeCheckRecordApi.RecordRow,
) {
  const record = detail ?? row;
  if (!record) {
    return '未填写';
  }
  const payload = (record.payload ?? {}) as Record<string, any>;
  const directValues: Record<string, unknown> = {
    attachmentUpload: detail?.attachments?.length
      ? `${detail.attachments.length}个附件`
      : '未上传',
    company: record.company,
    date: record.date || record.businessDate,
    department: record.department,
    imageCheck: record.imageCheck,
    imageUpload: record.imageCheck,
    owner: record.owner,
    status: record.statusLabel || getStatusLabel(record.status),
    team: record.team,
    videoCheck: record.videoCheck,
    videoUpload: record.videoCheck,
  };
  return String(directValues[column.dataIndex] ?? payload[column.dataIndex] ?? '未填写');
}

function documentFlowFieldKind(column: ThreeCheckTableColumn): DocumentFlowField['kind'] {
  const attachmentKind = detailAttachmentFieldKind(column);
  if (attachmentKind) {
    return attachmentKind;
  }
  return isStatusBoxColumn(column) ? 'STATUS' : 'TEXT';
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
  resetCreateAttachmentFiles();
});
onMounted(() => {
  window.addEventListener('mousemove', handleWindowPointerMove);
  window.addEventListener('pointermove', handleWindowPointerMove);
  document.addEventListener('visibilitychange', handleDocumentVisibilityChange);
  enforcePreShiftSafetyActivityCompany();
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
    currentLocalDetail.value = undefined;
    detailEditing.value = false;
    activeDetailPanel.value = undefined;
    documentFlowOpen.value = false;
    changeHistoryOpen.value = false;
    resetDetailWorkflow();
    resetDocumentFlowChain();
    seedThreeCheckDetailEditForm();
    detailLoading.value = false;
    detailOpen.value = false;
    createOpen.value = false;
    clearSelectedRows();
    enforcePreShiftSafetyActivityCompany();
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
          'pingan-shell--no-data-map': !shouldShowDataMap,
        },
      ]"
    >
      <aside
        v-if="shouldShowDataMap"
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
              :allow-clear="
                !isPreShiftSafetyActivityModule &&
                !isOrganizationFieldLocked('company')
              "
              :disabled="
                isPreShiftSafetyActivityModule ||
                isOrganizationFieldLocked('company')
              "
              :options="filterCompanySelectOptions"
              class="w-full"
              option-filter-prop="label"
              placeholder="点击选择"
              show-search
              @change="handleFilterCompanyChange"
            />
          </div>
          <div v-if="isOrganizationFilterVisible('department')" class="filter-item">
            <span>车间</span>
            <Select
              v-model:value="filters.departmentId"
              :allow-clear="!isOrganizationFieldLocked('department')"
              :disabled="
                !filters.companyId || isOrganizationFieldLocked('department')
              "
              :options="filterDepartmentSelectOptions"
              class="w-full"
              option-filter-prop="label"
              placeholder="点击选择"
              show-search
              @change="handleFilterDepartmentChange"
            />
          </div>
          <div v-if="isOrganizationFilterVisible('team')" class="filter-item">
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
            :disabled="!canUseWorkbench"
            :model-value="filters.status"
            :options="activeStatusOptions"
            @reset="resetFilters"
            @search="loadMeetings"
            @update:model-value="setWorkbenchStatusFilter"
          />
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
                v-show="isThreeCheckRecordModule && !isTeamMemberWorkbench"
                class="table-panel__selection"
              >
                已选 {{ selectedRowKeys.length }} 条
              </span>
              <Button
                v-show="isThreeCheckRecordModule && !isTeamMemberWorkbench"
                :disabled="!canBatchDelete"
                danger
                @click="handleBatchAction('DELETE')"
              >
                批量删除
              </Button>
              <Space>
                <Button :disabled="!moduleApiReady">下载数据</Button>
                <Button
                  v-if="!isTeamMemberWorkbench"
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
          >
            <template #bodyCell="{ column, record }">
              <template v-if="isAttachmentColumn(column)">
                <div class="attachment-table-cell">
                  <Image
                    v-if="
                      attachmentKind(column) === 'IMAGE' &&
                      attachmentPreviewFor(record, column)
                    "
                    :height="38"
                    :src="attachmentPreviewFor(record, column)"
                    :width="64"
                    class="check-preview-image"
                  />
                  <button
                    v-else-if="
                      attachmentKind(column) === 'VIDEO' &&
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
                    :disabled="!canUseWorkbench"
                    size="small"
                    type="primary"
                    @click="openDetail(record)"
                  >
                    明细
                  </Button>
                  <Button
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
                    v-if="canWithdrawRecord"
                    :disabled="!canUseWorkbench || !record.canWithdraw"
                    size="small"
                    @click="handleWithdraw(record)"
                  >
                    撤回
                  </Button>
                  <Button
                    v-if="canRemindRecord"
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
            :disabled="
              companySelectOptions.length === 0 ||
              isPreShiftSafetyActivityModule ||
              isOrganizationFieldLocked('company')
            "
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
            :disabled="
              departmentSelectOptions.length === 0 ||
              isOrganizationFieldLocked('department')
            "
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
            :disabled="
              teamSelectOptions.length === 0 || isOrganizationFieldLocked('team')
            "
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
            @change="handleCreateDispatchContextChange"
          />
        </label>
        <label v-if="shouldSelectRootDispatchForCreate">
          <span>归属派班</span>
          <Select
            v-model:value="createRootDispatchRecordId"
            :loading="createRootDispatchLoading"
            :options="createRootDispatchOptions"
            allow-clear
            option-filter-prop="label"
            placeholder="选择本班组当日派班"
            show-search
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
        <label v-if="shouldSelectRootDispatchForCreate">
          <span>归属派班</span>
          <Select
            v-model:value="createRootDispatchRecordId"
            :loading="createRootDispatchLoading"
            :options="createRootDispatchOptions"
            allow-clear
            option-filter-prop="label"
            placeholder="选择本班组当日派班"
            show-search
          />
        </label>
        <label v-for="column in localCreateColumns" :key="column.dataIndex">
          <span>{{ column.title }}</span>
          <Select
            v-if="column.dataIndex === 'company'"
            v-model:value="createForm.companyId"
            :disabled="
              companySelectOptions.length === 0 ||
              isPreShiftSafetyActivityModule ||
              isOrganizationFieldLocked('company')
            "
            :options="companySelectOptions"
            option-filter-prop="label"
            placeholder="选择公司"
            show-search
            @change="handleCompanyChange"
          />
          <Select
            v-else-if="column.dataIndex === 'department'"
            v-model:value="departmentSelectValue"
            :disabled="
              departmentSelectOptions.length === 0 ||
              isOrganizationFieldLocked('department')
            "
            :options="departmentSelectOptions"
            option-filter-prop="label"
            placeholder="暂无部门名单"
            show-search
            @change="handleDepartmentChange"
          />
          <Select
            v-else-if="column.dataIndex === 'team'"
            v-model:value="teamSelectValue"
            :disabled="
              teamSelectOptions.length === 0 || isOrganizationFieldLocked('team')
            "
            :options="teamSelectOptions"
            option-filter-prop="label"
            placeholder="暂无班组名单"
            show-search
            @change="handleTeamChange"
          />
          <DatePicker
            v-else-if="isLocalDateColumn(column)"
            v-model:value="localCreateForm[column.dataIndex]"
            value-format="YYYY-MM-DD"
            @change="handleCreateDispatchContextChange"
          />
          <Select
            v-else-if="isLocalSelectColumn(column)"
            v-model:value="localCreateForm[column.dataIndex]"
            :mode="isLocalMultiSelectColumn(column) ? 'multiple' : undefined"
            :options="fieldOptions(column)"
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
      <div
        v-if="shouldShowCreateAttachmentUpload"
        class="create-attachment-upload"
      >
        <div class="create-attachment-upload__title">附件</div>
        <div class="create-attachment-upload__cards">
          <div class="create-attachment-upload__card">
            <div
              :class="[
                'create-attachment-upload__preview',
                {
                  'create-attachment-upload__preview--images':
                    createUploadFiles.IMAGE.length,
                },
              ]"
            >
              <template v-if="createUploadFiles.IMAGE.length">
                <Image
                  v-for="previewUrl in createAttachmentImagePreviewUrls()"
                  :key="previewUrl"
                  :src="previewUrl"
                  class="create-attachment-upload__image"
                />
              </template>
              <IconifyIcon v-else icon="lucide:image-plus" />
            </div>
            <strong>图片打卡</strong>
            <label class="create-attachment-upload__picker">
              {{ createUploadFiles.IMAGE.length ? '重新选择' : '选择图片' }}
              <input
                :disabled="!canUploadAttachment"
                accept="image/*"
                multiple
                type="file"
                @change="handleCreateAttachmentChange('IMAGE', $event)"
              />
            </label>
          </div>
          <div class="create-attachment-upload__card">
            <div
              :class="[
                'create-attachment-upload__preview',
                {
                  'create-attachment-upload__preview--videos':
                    createUploadFiles.VIDEO.length,
                },
              ]"
            >
              <template v-if="createUploadFiles.VIDEO.length">
                <button
                  v-for="previewUrl in createAttachmentVideoPreviewUrls()"
                  :key="previewUrl"
                  class="create-attachment-upload__video"
                  type="button"
                  @click="openVideoPreview(previewUrl)"
                >
                  <video
                    :src="previewUrl"
                    muted
                    playsinline
                    preload="metadata"
                  ></video>
                  <span>点击预览</span>
                </button>
              </template>
              <IconifyIcon v-else icon="lucide:video" />
            </div>
            <strong>视频打卡</strong>
            <label class="create-attachment-upload__picker">
              {{ createUploadFiles.VIDEO.length ? '重新选择' : '选择视频' }}
              <input
                :disabled="!canUploadAttachment"
                accept="video/*"
                multiple
                type="file"
                @change="handleCreateAttachmentChange('VIDEO', $event)"
              />
            </label>
          </div>
        </div>
        <div
          v-if="createUploadFiles.IMAGE.length || createUploadFiles.VIDEO.length"
          class="create-attachment-upload__selected"
        >
          <Button
            v-if="createUploadFiles.IMAGE.length"
            size="small"
            @click="removeCreateAttachment('IMAGE')"
          >
            移除图片
          </Button>
          <Button
            v-if="createUploadFiles.VIDEO.length"
            size="small"
            @click="removeCreateAttachment('VIDEO')"
          >
            移除视频
          </Button>
        </div>
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
                <button
                  v-if="canUploadAttachment"
                  aria-label="删除附件"
                  class="attachment-delete-button"
                  title="删除附件"
                  type="button"
                  @click.stop="handleDeleteAttachment(attachment)"
                >
                  <IconifyIcon icon="lucide:x" />
                </button>
                <div class="attachment-name">{{ attachment.originalName }}</div>
              </div>
            </div>
            <div v-else class="attachment-empty">暂无附件</div>
          </dd>
        </dl>
      </div>
      <div v-else-if="currentThreeCheckDetail" class="meeting-detail">
        <div class="detail-action-bar">
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
              :type="activeDetailPanel === 'documentFlow' ? 'primary' : 'default'"
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
              :type="activeDetailPanel === 'changeHistory' ? 'primary' : 'default'"
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
          v-if="activeDetailPanel === 'documentFlow'"
          class="detail-panel"
        >
          <h3>单据流</h3>
          <div v-if="detailWorkflowLoading" class="detail-panel-empty">
            加载中...
          </div>
          <div v-else-if="detailWorkflowError" class="detail-panel-empty">
            {{ detailWorkflowError }}
          </div>
          <ol v-else-if="detailDocumentFlowItems.length" class="detail-flow-list">
            <li
              v-for="item in detailDocumentFlowItems"
              :key="item.key"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <em>{{ item.meta }}</em>
            </li>
          </ol>
          <div v-else class="detail-panel-empty">暂无单据流</div>
        </section>
        <section
          v-else-if="activeDetailPanel === 'changeHistory'"
          class="detail-panel"
        >
          <h3>变更历史</h3>
          <div v-if="detailWorkflowLoading" class="detail-panel-empty">
            加载中...
          </div>
          <div v-else-if="detailWorkflowError" class="detail-panel-empty">
            {{ detailWorkflowError }}
          </div>
          <dl
            v-else-if="detailChangeHistoryItems.length"
            class="detail-history-list"
          >
            <template
              v-for="item in detailChangeHistoryItems"
              :key="item.key"
            >
              <dt>{{ item.label }}</dt>
              <dd>
                <strong>{{ item.value }}</strong>
                <em>{{ item.meta }}</em>
              </dd>
            </template>
          </dl>
          <div v-else class="detail-panel-empty">暂无变更历史</div>
        </section>

        <div class="detail-meta-strip">
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
          <div>
            <span>{{ detailTopStatusTitle }}</span>
            <strong>
              <Select
                v-if="detailEditing && dispatchStatusColumn"
                v-model:value="detailEditForm[dispatchStatusColumn.dataIndex]"
                :options="localFieldOptions(dispatchStatusColumn)"
                class="detail-meta-status-select"
                size="small"
              />
              <b
                v-else
                class="detail-meta-status"
                :class="statusBoxClass(detailTopStatusLabel)"
              >
                {{ statusBoxLabel(detailTopStatusLabel) }}
              </b>
            </strong>
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
            <h3>{{ group.title }}</h3>
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
                      { 'detail-field--wide': isLongTextDetailColumn(column) },
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
                        v-if="isLocalDateColumn(column)"
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
                          :height="96"
                          :src="detailAttachmentFieldPreview(column)"
                          :width="132"
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
                    { 'detail-field--wide': isLongTextDetailColumn(column) },
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
                      v-if="isLocalDateColumn(column)"
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
                        :height="96"
                        :src="detailAttachmentFieldPreview(column)"
                        :width="132"
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
              v-if="group.key === 'default' && isTeamCheckInspectionDetail"
              class="team-check-items"
            >
              <div class="team-check-items__header">
                <h3>{{ teamCheckInspectionTableTitle }}</h3>
                <span v-if="teamCheckInspectionLoading">加载中...</span>
                <span v-else>{{ teamCheckInspectionLines.length }} 项</span>
              </div>
              <div class="team-check-items__table-wrap">
                <table class="team-check-items__table">
                  <thead>
                    <tr>
                      <th
                        v-for="column in teamCheckInspectionColumns"
                        :key="column.dataIndex"
                        :style="{ width: `${column.width}px` }"
                      >
                        <span>{{ column.title }}</span>
                        <b
                          v-if="
                            isTeamCheckInspectionRequiredColumn(
                              column,
                              moduleRuntime.routeName,
                            )
                          "
                          class="team-check-items__required"
                        >
                          *
                        </b>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="(line, lineIndex) in teamCheckInspectionLines"
                      :key="`${line.libraryItemId || lineIndex}-${line.checkItem}`"
                    >
                      <td
                        v-for="column in teamCheckInspectionColumns"
                        :key="column.dataIndex"
                      >
                        <Select
                          v-if="
                            detailEditing &&
                            column.dataIndex === 'checkResult'
                          "
                          v-model:value="line.checkResult"
                          :options="teamCheckInspectionResultOptions"
                          class="team-check-items__control"
                        />
                        <Input.TextArea
                          v-else-if="
                            detailEditing &&
                            [
                              'followUpPlan',
                              'rectificationDescription',
                              'rectificationStatus',
                              'uploadDescription',
                            ].includes(column.dataIndex)
                          "
                          v-model:value="line[column.dataIndex]"
                          :auto-size="{ maxRows: 4, minRows: 1 }"
                          class="team-check-items__control team-check-items__textarea"
                        />
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
                        暂无检查项，请先在系统管理配置班组检查项模板
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
            <section
              v-if="
                group.key === 'default' && isPreShiftMeetingSafetyConfirmDetail
              "
              class="safety-confirm-items"
            >
              <div class="team-check-items__header">
                <h3>安全确认事项</h3>
                <span v-if="safetyConfirmLoading">加载中...</span>
                <span v-else>{{ safetyConfirmLines.length }} 项</span>
              </div>
              <div class="team-check-items__table-wrap">
                <table class="team-check-items__table safety-confirm-items__table">
                  <thead>
                    <tr>
                      <th
                        v-for="column in safetyConfirmColumns"
                        :key="column.dataIndex"
                        :style="{ width: `${column.width}px` }"
                      >
                        <span>{{ column.title }}</span>
                        <b
                          v-if="
                            ['riskType', 'confirmStatus'].includes(
                              column.dataIndex,
                            )
                          "
                          class="team-check-items__required"
                        >
                          *
                        </b>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="(line, lineIndex) in safetyConfirmLines"
                      :key="`${line.libraryItemId || lineIndex}-${line.safetyItem}`"
                    >
                      <td
                        v-for="column in safetyConfirmColumns"
                        :key="column.dataIndex"
                      >
                        <Select
                          v-if="
                            detailEditing &&
                            column.dataIndex === 'confirmStatus'
                          "
                          v-model:value="line.confirmStatus"
                          :options="safetyConfirmStatusOptions"
                          allow-clear
                          class="team-check-items__control"
                        />
                        <span v-else class="team-check-items__text">
                          {{ line[column.dataIndex] || '' }}
                        </span>
                      </td>
                    </tr>
                    <tr v-if="!safetyConfirmLines.length">
                      <td
                        class="team-check-items__empty"
                        :colspan="safetyConfirmColumns.length"
                      >
                        暂无安全确认事项，请先在系统管理配置班前会安全确认模板
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
            <div
              ref="detailAttachmentSectionRef"
              :class="[
                'detail-attachment-row',
                {
                  'detail-attachment-row--bottom': group.key === 'default',
                },
              ]"
            >
              <div class="detail-attachment-label">附件（图片或视频）</div>
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
                    <Image
                      v-if="attachment.fileKind === 'IMAGE'"
                      :height="96"
                      :src="attachmentPreviewUrl(attachment)"
                      :width="132"
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
                    <button
                      v-if="canUploadAttachment"
                      aria-label="删除附件"
                      class="attachment-delete-button"
                      title="删除附件"
                      type="button"
                      @click.stop="handleDeleteAttachment(attachment)"
                    >
                      <IconifyIcon icon="lucide:x" />
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
            <div class="change-history-sidebar__head">
              <strong>真实动作日志</strong>
              <button
                v-if="canToggleChangeHistoryLogs"
                class="change-history-sidebar__toggle"
                type="button"
                @click="toggleChangeHistoryLogs"
              >
                {{
                  changeHistoryLogsExpanded
                    ? '收起'
                    : `展开全部（${changeHistoryGroups.length}）`
                }}
              </button>
            </div>
            <span class="change-history-sidebar__track"></span>
            <button
              v-for="(group, index) in visibleChangeHistoryGroups"
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
          <strong>{{ documentFlowModalTitle }}</strong>
          <span>{{ documentFlowSubtitle }}</span>
        </div>
      </template>
      <div class="document-flow">
        <div class="document-flow-toolbar">
          <div class="document-flow-toolbar__left">
            <Input
              v-model:value="documentFlowKeyword"
              class="document-flow-search"
              placeholder="搜索单据编号、流程节点、责任人"
            >
              <template #prefix>
                <IconifyIcon icon="lucide:search" />
              </template>
            </Input>
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
            <Button class="document-flow-tool-button" @click="exportDocumentFlow">
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

        <div
          v-if="!isPreShiftSafetyActivityDocumentFlow"
          class="document-flow-process"
        >
          <button
            v-for="(step, index) in documentFlowSteps"
            :key="`${step.label}-${step.recordNo}-${index}`"
            :class="[
              'document-flow-step',
              `document-flow-step--${step.state}`,
              {
                'is-active':
                  documentFlowSelectedNode?.stage.label === step.label,
              },
            ]"
            type="button"
            @click="selectDocumentFlowNode(documentFlowNodes[index])"
          >
            <span class="document-flow-step__line"></span>
              <strong>{{ step.state === 'done' ? '✓' : index + 1 }}</strong>
              <div>
                <b>{{ step.label }}</b>
                <em :class="statusBoxClass(step.status)">
                  {{ documentFlowStatusText(step.status) }}
                </em>
                <small>{{ step.timeText }} · {{ step.owner }}</small>
              </div>
            </button>
        </div>

        <div class="document-flow-body">
          <aside class="document-flow-sidebar">
            <div class="document-flow-sidebar__header">
              <strong>{{ documentFlowSidebarTitle }}</strong>
              <span>{{ documentFlowSidebarSubtitle }}</span>
            </div>
            <div class="document-flow-node-list">
              <button
                v-if="documentFlowTreeMasterNode"
                :class="[
                  'document-flow-node',
                  'document-flow-node--master',
                  {
                    'is-active':
                      documentFlowSelectedNode?.id === documentFlowTreeMasterNode.id,
                  },
                ]"
                type="button"
                @click="selectDocumentFlowNode(documentFlowTreeMasterNode)"
              >
                <span class="document-flow-node__marker"></span>
                <IconifyIcon :icon="documentFlowTreeMasterNode.icon" />
                <span class="document-flow-node__content">
                  <strong>{{ documentFlowTreeMasterNode.title }}</strong>
                  <em>{{ documentFlowTreeMasterNode.subtitle }}</em>
                </span>
                <span :class="statusBoxClass(documentFlowTreeMasterNode.status)">
                  {{ documentFlowTreeMasterNode.status }}
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
                  <IconifyIcon :icon="node.icon" />
                  <span class="document-flow-node__content">
                    <strong>{{ node.title }}</strong>
                    <em>{{ node.subtitle }}</em>
                  </span>
                  <span :class="statusBoxClass(node.status)">
                    {{ node.status }}
                  </span>
                </button>
              </div>
              <div
                v-if="!documentFlowTreeMasterNode && !documentFlowTreeDetailNodes.length"
                class="document-flow-empty"
              >
                暂无匹配单据
              </div>
            </div>
          </aside>

          <section class="document-flow-main">
            <div v-if="documentFlowSelectedNode" class="document-flow-detail">
              <div class="document-flow-detail__header">
                <div>
                  <h3>{{ documentFlowDetailTitle }}</h3>
                  <p>
                    {{ documentFlowSelectedNode.stage.label }}
                    ·
                    {{ documentFlowSelectedNode.record?.recordNo ?? '--' }}
                  </p>
                </div>
                <span :class="statusBoxClass(documentFlowSelectedNode.status)">
                  {{ documentFlowStatusText(documentFlowSelectedNode.status) }}
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
                        {{ field.value }}
                      </span>
                      <span v-else>{{ field.value || '未填写' }}</span>
                    </dd>
                  </div>
                </template>
              </dl>

              <div class="document-flow-lower">
                <div class="document-flow-logs">
                  <div class="document-flow-logs__head">
                    <div class="document-flow-logs__label">真实动作日志</div>
                    <button
                      v-if="canToggleDocumentFlowLogs"
                      class="document-flow-logs__toggle"
                      type="button"
                      @click="toggleDocumentFlowLogs"
                    >
                      {{
                        documentFlowLogsExpanded
                          ? '收起'
                          : `展开全部（${currentDocumentFlowStatusLogs.length}）`
                      }}
                    </button>
                  </div>
                  <ol
                    v-if="visibleDocumentFlowStatusLogs.length"
                    :class="{ 'is-expanded': documentFlowLogsExpanded }"
                  >
                    <li
                      v-for="log in visibleDocumentFlowStatusLogs"
                      :key="log.id"
                    >
                      <span></span>
                      <div>
                        <strong>{{ documentFlowActionLabel(log.action) }}</strong>
                        <em>
                          {{ log.operatorName || '系统' }}
                          ·
                          {{ formatChangeHistoryTime(log.occurredAt) }}
                        </em>
                      </div>
                      <b v-if="log.toStatusLabel || log.toStatus">
                        {{ log.toStatusLabel || log.toStatus }}
                      </b>
                    </li>
                  </ol>
                  <div v-else class="document-flow-empty">暂无单据流</div>
                </div>

                <div class="document-flow-attachments">
                  <div class="document-flow-attachments__label">
                    附件（图片或视频）
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
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </Modal>

    <Modal
      v-model:open="videoPreviewOpen"
      :footer="null"
      centered
      title="视频预览"
      width="min(960px, calc(100vw - 32px))"
      @cancel="closeVideoPreview"
    >
      <video
        v-if="videoPreviewUrl"
        :src="videoPreviewUrl"
        autoplay
        class="video-preview-player"
        controls
        playsinline
      ></video>
      <div class="video-preview-hint">
        <IconifyIcon icon="lucide:maximize-2" />
        可使用播放器全屏按钮横屏观看
      </div>
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

.pingan-shell--no-data-map {
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
  flex: 1 1 126px;
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
  text-align: center;
  vertical-align: middle;
}

.meeting-table :deep(.meeting-table__center-cell) {
  text-align: center;
}

.meeting-table :deep(.meeting-table__attachment-cell),
.meeting-table :deep(.meeting-table__status-cell) {
  text-align: center;
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

.create-attachment-upload {
  display: grid;
  gap: 10px;
  padding-top: 14px;
  margin-top: 14px;
  border-top: 1px solid #e5e7eb;
}

.create-attachment-upload__title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.create-attachment-upload__cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.create-attachment-upload__card {
  display: grid;
  gap: 6px;
  justify-items: center;
  min-width: 0;
  padding: 10px 12px;
  color: #475569;
  text-align: center;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
}

.create-attachment-upload__preview {
  display: grid;
  place-items: center;
  width: 100%;
  height: 158px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #dbe5f1;
  border-radius: 4px;
}

.create-attachment-upload__preview--images {
  grid-template-columns: repeat(auto-fit, minmax(72px, 1fr));
  gap: 5px;
  padding: 5px;
}

.create-attachment-upload__preview--videos {
  grid-template-columns: repeat(auto-fit, minmax(96px, 1fr));
  gap: 5px;
  padding: 5px;
}

.create-attachment-upload__preview svg {
  width: 28px;
  height: 28px;
  color: #2563eb;
}

.create-attachment-upload__image,
.create-attachment-upload__image :deep(.ant-image-img),
.create-attachment-upload__image :deep(img),
.create-attachment-upload__video,
.create-attachment-upload__video video {
  width: 100%;
  height: 158px;
  object-fit: cover;
}

.create-attachment-upload__image {
  overflow: hidden;
  border-radius: 4px;
}

.create-attachment-upload__preview--images .create-attachment-upload__image,
.create-attachment-upload__preview--images
  .create-attachment-upload__image
  :deep(.ant-image-img),
.create-attachment-upload__preview--images
  .create-attachment-upload__image
  :deep(img) {
  width: 100%;
  height: 100%;
  min-height: 48px;
}

.create-attachment-upload__video {
  position: relative;
  padding: 0;
  overflow: hidden;
  color: #fff;
  cursor: pointer;
  background: #0f172a;
  border: 1px solid #dbe5f1;
  border-radius: 4px;
}

.create-attachment-upload__video span {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 4px;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  background: rgb(15 23 42 / 68%);
}

.create-attachment-upload__preview--videos .create-attachment-upload__video,
.create-attachment-upload__preview--videos .create-attachment-upload__video video {
  height: 100%;
  min-height: 48px;
}

.create-attachment-upload__card strong {
  color: #1f2937;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.2;
}

.create-attachment-upload__card span {
  max-width: 100%;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.create-attachment-upload__picker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 76px;
  height: 26px;
  padding: 0 10px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 4px;
}

.create-attachment-upload__picker input {
  display: none;
}

.create-attachment-upload__selected {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.detail-edit-control {
  width: 100%;
}

.detail-edit-textarea {
  min-height: 64px;
  line-height: 1.55;
  resize: vertical;
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

:global(.change-history-modal .ant-modal-header),
:global(.document-flow-modal .ant-modal-header) {
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

:global(.document-flow-modal .ant-modal-body) {
  max-height: calc(100vh - 96px);
  padding: 0;
  overflow: auto;
  background: #f6f8fb;
}

.change-history-title,
.document-flow-title {
  display: flex;
  gap: 18px;
  align-items: center;
}

.change-history-title strong,
.document-flow-title strong {
  color: #0f172a;
  font-size: 22px;
  font-weight: 800;
}

.change-history-title span,
.document-flow-title span {
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
  height: min(630px, calc(100vh - 300px));
  min-height: 480px;
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
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.change-history-sidebar__head {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  padding-bottom: 4px;
  margin-left: -32px;
  background: #fff;
}

.change-history-sidebar__head strong {
  color: #111827;
  font-size: 14px;
  font-weight: 800;
}

.change-history-sidebar__toggle {
  height: 26px;
  padding: 0 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 4px;
}

.change-history-sidebar__track {
  position: absolute;
  top: 76px;
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
  background: #fff;
  border: 1px solid #e5ebf3;
  border-top: 0;
}

.change-history-diff-row__field strong {
  font-weight: 800;
}

.change-history-diff-value {
  display: -webkit-box;
  box-sizing: border-box;
  width: 100%;
  min-height: 40px;
  padding: 10px 14px;
  overflow: hidden;
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

.document-flow-summary-card__value--green {
  padding: 2px 8px;
  color: #15803d !important;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 4px;
}

.document-flow-process {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  padding: 14px 28px;
  background: #fff;
  border: 1px solid #e4ebf5;
  border-radius: 6px;
}

.document-flow-body {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 16px;
  min-height: 540px;
}

.document-flow-sidebar,
.document-flow-main {
  min-width: 0;
  background: #fff;
  border: 1px solid #e4ebf5;
  border-radius: 6px;
}

.document-flow-sidebar__header {
  display: grid;
  gap: 4px;
  padding: 14px 16px;
  border-bottom: 1px solid #e5ebf3;
}

.document-flow-sidebar__header strong {
  color: #111827;
  font-size: 16px;
  font-weight: 800;
}

.document-flow-sidebar__header span {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
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

.document-flow-step {
  position: relative;
  display: grid;
  gap: 8px;
  justify-items: center;
  min-width: 0;
  padding: 0;
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
  color: #fff;
  background: #18a86b;
  border-color: #18a86b;
}

.document-flow-step--current strong {
  color: #fff;
  background: #2563eb;
  border-color: #2563eb;
  box-shadow: 0 0 0 4px rgb(37 99 235 / 12%);
}

.document-flow-step--pending strong {
  color: #94a3b8;
  background: #fff;
  border-style: dashed;
}

.document-flow-step.is-active b {
  color: #1d4ed8;
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-flow-step em.status-box {
  display: inline-flex;
  min-width: 52px;
  height: 24px;
  margin: 0 auto;
}

.document-flow-detail {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.document-flow-detail__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.document-flow-detail__header h3 {
  margin: 0;
  color: #111827;
  font-size: 16px;
  font-weight: 800;
}

.document-flow-detail__header p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.document-flow-detail__header span {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.document-flow-detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 0;
  overflow: hidden;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
}

.document-flow-detail-field {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  min-width: 0;
  margin: 0;
  border-bottom: 1px solid #e5ebf3;
  border-right: 1px solid #e5ebf3;
}

.document-flow-detail-field:nth-child(3n) {
  border-right: 0;
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

.document-flow-lower {
  display: grid;
  grid-template-columns: minmax(360px, 0.9fr) minmax(420px, 1.1fr);
  gap: 14px;
  align-items: stretch;
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
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  min-width: 0;
  padding: 14px;
  margin-top: 0;
  background: #fff;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
}

.document-flow-attachments__label {
  color: #475569;
  font-size: 14px;
  font-weight: 800;
}

.document-flow-attachment-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-start;
  min-width: 0;
}

.document-flow-attachment-strip .attachment-grid {
  display: contents;
}

.document-flow-attachment-strip .attachment-preview,
.document-flow-attachment-strip .detail-attachment-upload-card {
  width: 116px;
}

.document-flow-attachment-strip .attachment-empty {
  min-width: 88px;
  padding-top: 18px;
  margin-top: 0;
}

.document-flow-logs {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  min-width: 0;
  padding: 14px;
  background: #fff;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
}

.document-flow-logs__label {
  color: #475569;
  font-size: 14px;
  font-weight: 800;
}

.document-flow-logs__head {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
}

.document-flow-logs__toggle {
  height: 26px;
  padding: 0 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 4px;
}

.document-flow-logs ol {
  display: grid;
  gap: 10px;
  max-height: 166px;
  padding: 0;
  margin: 0;
  overflow-y: auto;
  list-style: none;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.document-flow-logs ol.is-expanded {
  max-height: 260px;
}

.document-flow-logs li {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-height: 42px;
}

.document-flow-logs li > span {
  width: 8px;
  height: 8px;
  background: #2f7cf6;
  border-radius: 50%;
}

.document-flow-logs strong,
.document-flow-logs em {
  display: block;
}

.document-flow-logs strong {
  color: #111827;
  font-size: 13px;
  font-weight: 800;
}

.document-flow-logs em {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.document-flow-logs b {
  color: #64748b;
  font-size: 12px;
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

.detail-history-list dd {
  display: grid;
  gap: 3px;
}

.detail-history-list dd strong {
  font: inherit;
}

.detail-flow-list em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  text-align: right;
}

.detail-history-list dd em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
}

.detail-history-list {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  gap: 8px 12px;
  padding: 10px;
  margin: 0;
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

.detail-meta-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
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

.detail-meta-status {
  vertical-align: top;
}

.detail-meta-status-select {
  width: 128px;
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

.detail-field-section--default {
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
  width: 132px;
  height: 96px;
  overflow: hidden;
  border: 1px solid #d8e1ea;
  border-radius: 4px;
}

.detail-attachment-field__preview :deep(.ant-image-img),
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

.detail-attachment-field__upload {
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

.detail-attachment-field__upload input {
  display: none;
}

.safety-confirm-items,
.team-check-items {
  display: grid;
  gap: 10px;
  padding-top: 16px;
  margin-top: 16px;
  border-top: 1px solid #edf2f7;
}

.team-check-items__header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.team-check-items__header h3 {
  margin: 0;
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.team-check-items__header span {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.team-check-items__table-wrap {
  overflow: auto;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.team-check-items__table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
  table-layout: fixed;
}

.safety-confirm-items__table {
  min-width: 740px;
}

.team-check-items__table th,
.team-check-items__table td {
  padding: 10px 12px;
  color: #1f2937;
  font-size: 13px;
  line-height: 1.45;
  vertical-align: top;
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
}

.team-check-items__table th {
  color: #475569;
  font-size: 12px;
  font-weight: 800;
  background: #f8fafc;
}

.team-check-items__table th span {
  display: inline-block;
}

.team-check-items__required {
  display: inline-block;
  margin-left: 3px;
  color: #ef4444;
  font-size: 13px;
  font-weight: 800;
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
  color: #1f2937;
  overflow-wrap: anywhere;
}

.team-check-items__control {
  width: 100%;
}

.team-check-items__textarea.ant-input {
  resize: vertical;
}

.team-check-items__empty {
  color: #94a3b8;
  text-align: center;
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

:global(.pingan-detail-modal--one-shift .ant-modal) {
  top: 0;
  padding-bottom: 0;
}

:global(.pingan-detail-modal--one-shift .ant-modal-content) {
  overflow: hidden;
  border-radius: 6px;
}

:global(.pingan-detail-modal--one-shift .ant-modal-header) {
  min-height: 58px;
  padding: 18px 24px 14px;
  margin-bottom: 0;
  border-bottom: 1px solid #e5e7eb;
}

:global(.pingan-detail-modal--one-shift .ant-modal-body) {
  max-height: calc(100vh - 108px);
  padding: 14px 22px 22px;
  overflow: auto;
}

.meeting-detail > dl {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 10px 12px;
  margin: 0;
}

.meeting-detail > dl > dt {
  color: #64748b;
  font-weight: 700;
}

.meeting-detail > dl > dd {
  min-width: 0;
  margin: 0;
  color: #1f2937;
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
  cursor: pointer;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 4px;
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

.attachment-preview {
  position: relative;
  min-width: 0;
}

.attachment-delete-button {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 2;
  display: inline-grid;
  place-items: center;
  width: 24px;
  height: 24px;
  padding: 0;
  color: #fff;
  cursor: pointer;
  background: rgb(15 23 42 / 72%);
  border: 1px solid rgb(255 255 255 / 70%);
  border-radius: 50%;
  box-shadow: 0 2px 8px rgb(15 23 42 / 22%);
}

.attachment-delete-button:hover {
  background: #dc2626;
  border-color: #fecaca;
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
  aspect-ratio: 16 / 9;
  display: block;
  width: 100%;
  max-height: calc(100vh - 190px);
  object-fit: contain;
  background: #000;
  border-radius: 4px;
}

.video-preview-hint {
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
  margin-top: 10px;
  color: #64748b;
  font-size: 13px;
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
