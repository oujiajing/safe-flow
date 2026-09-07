<script setup lang="ts">
import type { TableColumnsType, TableProps } from 'ant-design-vue';
import type { SystemTableConfig } from '#/views/system-management/shared/systemTableConfig';
import type { SystemCrudColumn } from '#/views/system-management/shared/useSystemCrudPage';

import { computed, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import {
  Button,
  DatePicker,
  Empty,
  Input,
  message,
  Modal,
  Select,
  Table,
} from 'ant-design-vue';
import { useRoute, useRouter } from 'vue-router';

import {
  batchDeleteSafetyLedgerDocumentsApi,
  createSafetyLedgerDocumentApi,
  deleteSafetyLedgerDocumentApi,
  getSafetyLedgerRowsApi,
  updateSafetyLedgerDocumentApi,
} from '#/api/pingan/safety-ledger';
import type { PinganSafetyLedgerApi } from '#/api/pingan/safety-ledger';
import { getThreeCheckRecordFlowApi } from '#/api/pingan/three-check-record';
import type { PinganThreeCheckRecordApi } from '#/api/pingan/three-check-record';
import {
  getSystemFullOrganizationTreeApi,
} from '#/api/system-management/options';
import type { SystemManagementApi } from '#/api/system-management/types';
import {
  getVisiblePinganOrganizationFilterKeys,
  isPinganOrganizationFilterLocked,
  resolveScopedPinganOrganizationDefaults,
  type PinganOrganizationFilterKey,
} from '#/views/pingan/shared/organization-filter-permission';
import SafetyLedgerTableConfigDrawer from '#/views/system-management/shared/SystemTableConfigDrawer.vue';
import {
  applySystemTableConfigColumns,
  calculateSystemTableAutoWidth,
  createDefaultSystemTableConfig,
  getSystemTableConfigStorage,
  mergeSystemTableConfig,
} from '#/views/system-management/shared/systemTableConfig';

import {
  filterSafetyLedgerNavigation,
  findSafetyLedgerEntryByRouteName,
  getSafetyLedgerAncestorRouteNames,
  isSafetyLedgerEntry,
  safetyLedgerEntries,
  safetyLedgerSections,
} from './safety-ledger.config';
import type {
  SafetyLedgerColumnConfig,
  SafetyLedgerEntryConfig,
  SafetyLedgerFilterField,
  SafetyLedgerSectionConfig,
} from './safety-ledger.config';

interface LedgerFilters {
  checkUnit: string;
  companyId: PinganSafetyLedgerApi.Id | undefined;
  dateEnd: string;
  dateStart: string;
  departmentId: PinganSafetyLedgerApi.Id | undefined;
  inspectedUnit: string;
  status: string;
  teamId: PinganSafetyLedgerApi.Id | undefined;
  uploadedEnd: string;
  uploadedStart: string;
  workshopId: PinganSafetyLedgerApi.Id | undefined;
}

interface DocumentForm {
  companyId: PinganSafetyLedgerApi.Id | undefined;
  date: string;
  department: string;
  file: File | undefined;
  name: string;
  richText: string;
  team: string;
  type: string;
}

interface SafetyLedgerMeetingReportSection {
  label: string;
  value: string;
}

interface SafetyLedgerMeetingReport {
  accidentType: string;
  company: string;
  meeting: null | PinganThreeCheckRecordApi.RecordRow | undefined;
  participants: string;
  postCheck: string;
  preCheck: string;
  safetyOfficer: string;
  team: string;
  teamLeader: string;
  time: string;
  midCheck: string;
  workContent: string;
  sections: SafetyLedgerMeetingReportSection[];
}

interface SafetyLedgerMediaPreview {
  title: string;
  type: 'image' | 'video';
  url: string;
}

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const filters = reactive<LedgerFilters>({
  checkUnit: '',
  companyId: undefined,
  dateEnd: '',
  dateStart: '',
  departmentId: undefined,
  inspectedUnit: '',
  status: '',
  teamId: undefined,
  uploadedEnd: '',
  uploadedStart: '',
  workshopId: undefined,
});
const ledgerDirectoryKeyword = ref('');
const ledgerDirectoryCollapsed = ref(false);
const expandedLedgerRouteNames = ref<string[]>([]);
const rows = ref<PinganSafetyLedgerApi.SafetyLedgerRow[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);
const loading = ref(false);
const companyOptions = ref<SystemManagementApi.Option[]>([]);
const organizationNodes = ref<SystemManagementApi.OrganizationNode[]>([]);
const companyOptionsLoading = ref(false);
const departmentOptions = ref<SystemManagementApi.Option[]>([]);
const departmentOptionsLoading = ref(false);
const teamOptions = ref<SystemManagementApi.Option[]>([]);
const teamOptionsLoading = ref(false);
const documentDepartmentOptions = ref<SystemManagementApi.Option[]>([]);
const documentDepartmentOptionsLoading = ref(false);
const documentTeamOptions = ref<SystemManagementApi.Option[]>([]);
const documentTeamOptionsLoading = ref(false);
const documentDepartmentValue = ref<SystemManagementApi.Id>();
const documentTeamValue = ref<SystemManagementApi.Id>();
const createDocumentOpen = ref(false);
const tableConfigOpen = ref(false);
const creatingDocument = ref(false);
const deletingDocuments = ref(false);
const editingDocumentId = ref<PinganSafetyLedgerApi.Id>();
const existingDocumentFileName = ref('');
const selectedRowKeys = ref<string[]>([]);
const documentFileInputRef = ref<HTMLInputElement>();
const meetingReportOpen = ref(false);
const meetingReportLoading = ref(false);
const meetingReport = ref<SafetyLedgerMeetingReport>();
const mediaPreviewOpen = ref(false);
const mediaPreview = ref<SafetyLedgerMediaPreview>();
const fallbackEntry = safetyLedgerEntries[0] as SafetyLedgerEntryConfig;
const safetyLedgerTableConfigModule = 'safety-ledger';
const documentForm = reactive<DocumentForm>({
  companyId: undefined,
  date: '',
  department: '',
  file: undefined,
  name: '',
  richText: '',
  team: '',
  type: '',
});

const tableConfig = ref<SystemTableConfig>(
  createDefaultSystemTableConfig(
    safetyLedgerTableConfigModule,
    fallbackEntry.title,
    ledgerColumnsToSystemColumns(fallbackEntry.columns),
    [],
  ),
);

const entry = computed(
  () =>
    findSafetyLedgerEntryByRouteName(String(route.name || '')) ??
    fallbackEntry,
);

const currentAncestorRouteNames = computed(() =>
  getSafetyLedgerAncestorRouteNames(
    safetyLedgerSections,
    String(route.name || ''),
  ),
);

const filteredLedgerSections = computed(() =>
  filterSafetyLedgerNavigation(
    safetyLedgerSections,
    ledgerDirectoryKeyword.value,
  ),
);

const isDocumentLedger = computed(
  () => entry.value.sourceKind === 'DOCUMENT_LEDGER',
);

const isDocumentOrgLedger = computed(
  () => entry.value.headerKey === 'document-org',
);

const isPreShiftMeetingLedger = computed(
  () => entry.value.routeName === 'PinganSafetyLedgerPreShiftMeetingRecord',
);

const currentUserRoles = computed(() => userStore.userInfo?.roles ?? []);
const currentUserOrgDefaults = computed(() =>
  resolveScopedPinganOrganizationDefaults(
    userStore.userInfo?.orgId,
    organizationNodes.value,
  ),
);
const visibleOrganizationFilterKeys = computed(() =>
  getVisiblePinganOrganizationFilterKeys(currentUserRoles.value),
);
const renderedFilterFields = computed(() =>
  entry.value.filters.filter((field) => isLedgerFilterFieldVisible(field)),
);

const companyUnitOptions = computed(() =>
  companyOptions.value.map((option) => ({
    label: option.label,
    value: option.label,
  })),
);

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '待整改', value: '待整改' },
  { label: '整改中', value: '整改中' },
  { label: '已整改', value: '已整改' },
  { label: '已验收', value: '已验收' },
];

const documentDepartmentSelectOptions = computed(() =>
  withLegacyOption(documentDepartmentOptions.value, documentForm.department),
);

const documentTeamSelectOptions = computed(() =>
  withLegacyOption(documentTeamOptions.value, documentForm.team),
);

const baseTableColumns = computed<SystemCrudColumn[]>(() =>
  ledgerColumnsToSystemColumns(entry.value.columns),
);

const tableColumns = computed<
  TableColumnsType<PinganSafetyLedgerApi.SafetyLedgerRow>
>(() =>
  applySystemTableConfigColumns(
    baseTableColumns.value,
    tableConfig.value,
    rows.value,
  ) as TableColumnsType<PinganSafetyLedgerApi.SafetyLedgerRow>,
);

const autoWidths = computed(() =>
  Object.fromEntries(
    baseTableColumns.value.map((column) => [
      String(column.dataIndex),
      calculateSystemTableAutoWidth(column, rows.value),
    ]),
  ),
);

const defaultTableConfig = computed(() =>
  createDefaultSystemTableConfig(
    safetyLedgerTableConfigModule,
    entry.value.title,
    baseTableColumns.value,
    [],
  ),
);

function ledgerColumnsToSystemColumns(columns: SafetyLedgerColumnConfig[]) {
  return columns.map((column: SafetyLedgerColumnConfig) => ({
    dataIndex: column.dataIndex,
    title: column.title,
    width: column.width,
  }));
}

const tableScroll = computed(() => ({
  x: tableColumns.value.reduce(
    (sum, column) => sum + Number(column.width ?? 120),
    0,
  ),
}));

const pagination = computed<TableProps['pagination']>(() => ({
  current: page.value,
  pageSize: pageSize.value,
  showSizeChanger: true,
  showTotal: (value) => `共 ${value} 条`,
  total: total.value,
}));

const canBatchDelete = computed(
  () => isDocumentLedger.value && selectedRowKeys.value.length > 0,
);

const documentModalTitle = computed(() =>
  editingDocumentId.value ? '修改台账' : '新增台账',
);

const tableRowSelection = computed<
  TableProps<PinganSafetyLedgerApi.SafetyLedgerRow>['rowSelection']
>(() =>
  isDocumentLedger.value
    ? {
        onChange: (keys) => {
          selectedRowKeys.value = keys.map(String);
        },
        selectedRowKeys: selectedRowKeys.value,
      }
    : undefined,
);

function rowKey(record: PinganSafetyLedgerApi.SafetyLedgerRow) {
  return String(record.id);
}

function safetyLedgerTableConfigStorageKey(path = entry.value.path) {
  return `pingan:safety-ledger-table-config:${path}`;
}

function loadTableConfig() {
  const raw = getSystemTableConfigStorage()?.getItem(
    safetyLedgerTableConfigStorageKey(),
  );
  if (!raw) {
    tableConfig.value = defaultTableConfig.value;
    return;
  }
  try {
    tableConfig.value = mergeSystemTableConfig(
      JSON.parse(raw) as Partial<SystemTableConfig>,
      safetyLedgerTableConfigModule,
      entry.value.title,
      baseTableColumns.value,
      [],
    );
  } catch {
    tableConfig.value = defaultTableConfig.value;
  }
}

function saveTableConfig(config: SystemTableConfig) {
  getSystemTableConfigStorage()?.setItem(
    safetyLedgerTableConfigStorageKey(),
    JSON.stringify(config),
  );
  tableConfig.value = config;
  tableConfigOpen.value = false;
  message.success('表格配置已保存');
}

function deleteTableConfig() {
  getSystemTableConfigStorage()?.removeItem(safetyLedgerTableConfigStorageKey());
  tableConfig.value = defaultTableConfig.value;
  tableConfigOpen.value = false;
  message.success('表格配置已恢复');
}

function filterFieldLabel(field: SafetyLedgerFilterField) {
  const labels: Record<SafetyLedgerFilterField, string> = {
    checkUnit: '检查单位',
    company: '公司',
    dateEnd: '日期（止）',
    dateStart: '日期（起）',
    department: '部门',
    inspectedUnit: '受检单位',
    status: '状态',
    team: '班组',
    uploadedEnd: '上传时间（止）',
    uploadedStart: '上传时间（起）',
    workshop: '车间',
  };
  return labels[field];
}

function isDateFilterField(field: SafetyLedgerFilterField) {
  return (
    field === 'dateStart' ||
    field === 'dateEnd' ||
    field === 'uploadedStart' ||
    field === 'uploadedEnd'
  );
}

function withLegacyOption(
  options: SystemManagementApi.Option[],
  legacyLabel: string,
) {
  const trimmedLabel = legacyLabel.trim();
  if (!trimmedLabel || options.some((option) => option.label === trimmedLabel)) {
    return options;
  }
  return [
    ...options,
    {
      label: trimmedLabel,
      value: `legacy:${trimmedLabel}`,
    },
  ];
}

function findOptionByValue(
  options: SystemManagementApi.Option[],
  value: SystemManagementApi.Id | undefined,
) {
  return options.find((option) => option.value === value);
}

function findOptionByLabel(
  options: SystemManagementApi.Option[],
  label: string,
) {
  return options.find((option) => option.label === label.trim());
}

function findOrganizationNodeById(
  nodes: SystemManagementApi.OrganizationNode[],
  id: SystemManagementApi.Id | undefined,
): SystemManagementApi.OrganizationNode | undefined {
  if (id === undefined) {
    return undefined;
  }
  for (const node of nodes) {
    if (String(node.id) === String(id)) {
      return node;
    }
    const child = findOrganizationNodeById(node.children ?? [], id);
    if (child) {
      return child;
    }
  }
  return undefined;
}

function documentOrganizationLabel(
  value: SystemManagementApi.Id | undefined,
  options: SystemManagementApi.Option[],
) {
  return (
    findOptionByValue(options, value)?.label ??
    findOrganizationNodeById(organizationNodes.value, value)?.title ??
    ''
  );
}

function normalizeDocumentSelectValue(value: unknown) {
  return Array.isArray(value)
    ? undefined
    : (value as SystemManagementApi.Id | undefined);
}

function ledgerOrganizationField(
  field: SafetyLedgerFilterField,
): PinganOrganizationFilterKey | undefined {
  if (field === 'company') {
    return 'company';
  }
  if (field === 'department' || field === 'workshop') {
    return 'department';
  }
  if (field === 'team') {
    return 'team';
  }
  return undefined;
}

function isLedgerFilterFieldVisible(field: SafetyLedgerFilterField) {
  const organizationField = ledgerOrganizationField(field);
  return organizationField
    ? visibleOrganizationFilterKeys.value.includes(organizationField)
    : true;
}

function isLedgerOrganizationFieldLocked(field: PinganOrganizationFilterKey) {
  return isPinganOrganizationFilterLocked(currentUserRoles.value, field);
}

function isLedgerFilterFieldLocked(field: SafetyLedgerFilterField) {
  const organizationField = ledgerOrganizationField(field);
  return organizationField
    ? isLedgerOrganizationFieldLocked(organizationField)
    : false;
}

function isOrganizationId(value: unknown): value is SystemManagementApi.Id {
  return typeof value === 'number' || typeof value === 'string';
}

function findOrganizationNode(
  nodes: SystemManagementApi.OrganizationNode[],
  id?: SystemManagementApi.Id,
): SystemManagementApi.OrganizationNode | undefined {
  if (!isOrganizationId(id)) {
    return undefined;
  }
  for (const node of nodes) {
    if (String(node.id) === String(id)) {
      return node;
    }
    const child = findOrganizationNode(node.children ?? [], id);
    if (child) {
      return child;
    }
  }
  return undefined;
}

function organizationOptionsFor(
  parentId: SystemManagementApi.Id | undefined,
  orgTypes: string[],
): SystemManagementApi.Option[] {
  const normalizedTypes = orgTypes.map((item) => item.toUpperCase());
  const parent = findOrganizationNode(organizationNodes.value, parentId);
  const roots = parent ? parent.children ?? [] : organizationNodes.value;
  const options: SystemManagementApi.Option[] = [];

  function collect(nodes: SystemManagementApi.OrganizationNode[]) {
    for (const node of nodes) {
      if (
        isOrganizationId(node.id) &&
        node.orgType &&
        normalizedTypes.includes(node.orgType.toUpperCase())
      ) {
        options.push({
          label: node.title,
          value: node.id,
        });
      }
      collect(node.children ?? []);
    }
  }

  collect(roots);
  return options;
}

function syncCompanyOptionsFromOrganizationTree() {
  companyOptions.value = organizationOptionsFor(undefined, ['COMPANY']);
}

function syncDepartmentOptionsFromOrganizationTree(
  companyId?: SystemManagementApi.Id,
) {
  departmentOptions.value = organizationOptionsFor(companyId, ['DEPARTMENT']);
}

function syncTeamOptionsFromOrganizationTree(parentOrgId?: SystemManagementApi.Id) {
  teamOptions.value = organizationOptionsFor(parentOrgId, ['TEAM']);
}

function syncDocumentDepartmentOptionsFromOrganizationTree(
  companyId?: SystemManagementApi.Id,
) {
  documentDepartmentOptions.value = organizationOptionsFor(companyId, [
    'DEPARTMENT',
  ]);
}

function syncDocumentTeamOptionsFromOrganizationTree(
  parentOrgId?: SystemManagementApi.Id,
) {
  documentTeamOptions.value = organizationOptionsFor(parentOrgId, ['TEAM']);
}

function applyScopedLedgerOrganizationDefaults() {
  const defaults = currentUserOrgDefaults.value;
  if (
    isLedgerOrganizationFieldLocked('company') &&
    defaults.companyId !== undefined
  ) {
    filters.companyId = defaults.companyId;
  }
  if (
    isLedgerOrganizationFieldLocked('department') &&
    defaults.departmentId !== undefined
  ) {
    filters.departmentId = defaults.departmentId;
    filters.workshopId = defaults.departmentId;
  }
  if (isLedgerOrganizationFieldLocked('team') && defaults.teamId !== undefined) {
    filters.teamId = defaults.teamId;
  }
}

function applyScopedLedgerDocumentOrganizationDefaults() {
  const defaults = currentUserOrgDefaults.value;
  if (
    isLedgerOrganizationFieldLocked('company') &&
    defaults.companyId !== undefined
  ) {
    documentForm.companyId = defaults.companyId;
  }
  if (
    isLedgerOrganizationFieldLocked('department') &&
    defaults.departmentId !== undefined
  ) {
    documentDepartmentValue.value = defaults.departmentId;
  }
  if (
    isLedgerOrganizationFieldLocked('team') &&
    defaults.teamId !== undefined
  ) {
    documentTeamValue.value = defaults.teamId;
  }
}

function hydrateScopedLedgerDocumentOrganizationLabels() {
  const defaults = currentUserOrgDefaults.value;
  if (
    isLedgerOrganizationFieldLocked('department') &&
    defaults.departmentId !== undefined
  ) {
    documentForm.department = documentOrganizationLabel(
      defaults.departmentId,
      documentDepartmentSelectOptions.value,
    );
    documentDepartmentValue.value = defaults.departmentId;
  }
  if (isLedgerOrganizationFieldLocked('team') && defaults.teamId !== undefined) {
    documentForm.team = documentOrganizationLabel(
      defaults.teamId,
      documentTeamSelectOptions.value,
    );
    documentTeamValue.value = defaults.teamId;
  }
}

async function enforceScopedLedgerDocumentOrganizationDefaults() {
  applyScopedLedgerDocumentOrganizationDefaults();
  if (documentForm.companyId) {
    await loadDocumentDepartmentOptions(
      documentForm.companyId as SystemManagementApi.Id,
    );
  }
  if (documentDepartmentValue.value) {
    await loadDocumentTeamOptions(documentDepartmentValue.value);
  }
  hydrateScopedLedgerDocumentOrganizationLabels();
}

function resetLedgerFilters() {
  filters.checkUnit = '';
  filters.companyId = undefined;
  filters.dateEnd = '';
  filters.dateStart = '';
  filters.departmentId = undefined;
  filters.inspectedUnit = '';
  filters.status = '';
  filters.teamId = undefined;
  filters.uploadedEnd = '';
  filters.uploadedStart = '';
  filters.workshopId = undefined;
  applyScopedLedgerOrganizationDefaults();
}

async function loadRows() {
  loading.value = true;
  try {
    const result = await getSafetyLedgerRowsApi({
      checkUnit: filters.checkUnit,
      companyId: filters.companyId,
      dateEnd: filters.dateEnd,
      dateStart: filters.dateStart,
      departmentId: filters.departmentId,
      inspectedUnit: filters.inspectedUnit,
      ledgerKey: entry.value.path,
      moduleKey: entry.value.moduleKey,
      page: page.value,
      pageSize: pageSize.value,
      sourceKind: entry.value.sourceKind,
      status: filters.status,
      teamId: filters.teamId,
      uploadedEnd: filters.uploadedEnd,
      uploadedStart: filters.uploadedStart,
      workshopId: filters.workshopId,
    });
    rows.value = result.items;
    total.value = result.total;
    selectedRowKeys.value = selectedRowKeys.value.filter((key) =>
      result.items.some((item) => String(item.id) === key),
    );
  } finally {
    loading.value = false;
  }
}

function search() {
  page.value = 1;
  void loadRows();
}

function isLedgerSectionExpanded(item: SafetyLedgerSectionConfig) {
  return (
    ledgerDirectoryKeyword.value.trim().length > 0 ||
    expandedLedgerRouteNames.value.includes(item.routeName)
  );
}

function isLedgerSectionCurrent(item: SafetyLedgerSectionConfig) {
  return currentAncestorRouteNames.value.includes(item.routeName);
}

function isLedgerEntryActive(item: SafetyLedgerEntryConfig) {
  return route.name === item.routeName;
}

function toggleLedgerSection(item: SafetyLedgerSectionConfig) {
  const expanded = new Set(expandedLedgerRouteNames.value);
  if (expanded.has(item.routeName)) {
    expanded.delete(item.routeName);
  } else {
    expanded.add(item.routeName);
  }
  expandedLedgerRouteNames.value = [...expanded];
}

function goLedgerEntry(item: SafetyLedgerEntryConfig) {
  if (route.name !== item.routeName) {
    void router.push({ name: item.routeName });
  }
}

function handleTableChange(pager: TableProps['pagination']) {
  if (pager && typeof pager === 'object') {
    page.value = pager.current ?? 1;
    pageSize.value = pager.pageSize ?? 20;
  }
  void loadRows();
}

function displayValue(value: unknown) {
  if (Array.isArray(value)) {
    return value.join('、');
  }
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  return String(value);
}

function displayReportValue(value: unknown, fallback = '无') {
  const text = displayValue(value);
  return text === '-' ? fallback : text;
}

function payloadOf(record?: PinganThreeCheckRecordApi.RecordRow | null) {
  return record?.payload && typeof record.payload === 'object'
    ? record.payload
    : {};
}

function getPayloadText(
  payload: Record<string, unknown>,
  keys: string[],
  fallback = '',
) {
  for (const key of keys) {
    const value = payload[key];
    if (Array.isArray(value) && value.length > 0) {
      return value.map(String).join('、');
    }
    if (value !== null && value !== undefined && value !== '') {
      return String(value);
    }
  }
  return fallback;
}

function stageRecord(
  flow: PinganThreeCheckRecordApi.RecordFlow,
  stageKey: PinganThreeCheckRecordApi.RecordFlowStage['stageKey'],
) {
  return flow.stages.find((stage) => stage.stageKey === stageKey)?.record;
}

function inspectionSummary(record?: PinganThreeCheckRecordApi.RecordRow | null) {
  const extra = payloadOf(record);
  return displayReportValue(
    getPayloadText(extra, [
      'inspectionContent',
      'checkContent',
      'description',
      'hazardDescription',
      'remark',
    ]) ||
      record?.statusLabel ||
      record?.status,
  );
}

function buildMeetingReport(
  flow: PinganThreeCheckRecordApi.RecordFlow,
): SafetyLedgerMeetingReport {
  const dispatch = stageRecord(flow, 'teamDispatch');
  const meeting = stageRecord(flow, 'preShiftMeeting');
  const preInspection = stageRecord(flow, 'preShiftInspection');
  const midInspection = stageRecord(flow, 'midShiftInspection');
  const postInspection = stageRecord(flow, 'postShiftInspection');
  const dispatchPayload = payloadOf(dispatch);
  const meetingPayload = payloadOf(meeting);
  const workContent = displayReportValue(
    getPayloadText(dispatchPayload, [
      'workContent',
      'taskContent',
      'dispatchContent',
    ]) ||
      getPayloadText(meetingPayload, ['meetingContent', 'content', 'remark']),
  );
  const accidentType = displayReportValue(
    getPayloadText(meetingPayload, ['accidentType', 'hazardType']) ||
      getPayloadText(dispatchPayload, ['accidentType', 'hazardType']),
  );
  const participants = displayReportValue(
    getPayloadText(meetingPayload, ['attendeesText', 'attendees', 'participants']),
  );
  const report = {
    accidentType,
    company: displayReportValue(flow.company, ''),
    meeting,
    participants,
    postCheck: inspectionSummary(postInspection),
    preCheck: inspectionSummary(preInspection),
    safetyOfficer: displayReportValue(meeting?.owner ?? dispatch?.owner),
    team: [flow.department, flow.team].filter(Boolean).join(' / '),
    teamLeader: displayReportValue(dispatch?.owner ?? flow.team),
    time: displayReportValue(flow.businessDate, ''),
    midCheck: inspectionSummary(midInspection),
    workContent,
  };
  return {
    ...report,
    sections: [
      { label: '工作内容', value: report.workContent },
      { label: '事故类型', value: report.accidentType },
      { label: '班前检查', value: report.preCheck },
      { label: '班中检查', value: report.midCheck },
      { label: '班后检查', value: report.postCheck },
      { label: '其它', value: '无' },
      { label: '参会人员', value: report.participants },
      { label: '安全员', value: report.safetyOfficer },
      { label: '班长', value: report.teamLeader },
    ],
  };
}

function canOpenMeetingReport(record: Record<string, unknown>) {
  return isPreShiftMeetingLedger.value && Boolean(record.rootDispatchRecordId);
}

async function openMeetingReport(record: Record<string, unknown>) {
  if (!canOpenMeetingReport(record)) {
    return;
  }
  meetingReportLoading.value = true;
  meetingReportOpen.value = true;
  try {
    const flow = await getThreeCheckRecordFlowApi(
      String(record.rootDispatchRecordId),
    );
    meetingReport.value = buildMeetingReport(flow);
  } catch {
    meetingReportOpen.value = false;
    message.error('记录表加载失败');
  } finally {
    meetingReportLoading.value = false;
  }
}

function printMeetingReport() {
  window.print();
}

function openMediaPreview(type: 'image' | 'video', url: unknown) {
  const previewUrl = String(url || '');
  if (!previewUrl) {
    return;
  }
  mediaPreview.value = {
    title: type === 'image' ? '图片打卡预览' : '视频打卡预览',
    type,
    url: previewUrl,
  };
  mediaPreviewOpen.value = true;
}

function isAllowedDocumentFile(file: File) {
  return /\.(doc|docx|pdf)$/i.test(file.name);
}

function resetDocumentForm() {
  editingDocumentId.value = undefined;
  existingDocumentFileName.value = '';
  documentDepartmentOptions.value = [];
  documentDepartmentValue.value = undefined;
  documentTeamOptions.value = [];
  documentTeamValue.value = undefined;
  documentForm.companyId = undefined;
  documentForm.date = '';
  documentForm.department = '';
  documentForm.file = undefined;
  documentForm.name = '';
  documentForm.richText = '';
  documentForm.team = '';
  documentForm.type = '';
  if (documentFileInputRef.value) {
    documentFileInputRef.value.value = '';
  }
  applyScopedLedgerDocumentOrganizationDefaults();
}

async function loadCompanyOptions() {
  await loadOrganizationScopeTree();
  if (companyOptions.value.length > 0 || companyOptionsLoading.value) {
    applyScopedLedgerOrganizationDefaults();
    applyScopedLedgerDocumentOrganizationDefaults();
    return;
  }
  companyOptionsLoading.value = true;
  try {
    syncCompanyOptionsFromOrganizationTree();
    applyScopedLedgerOrganizationDefaults();
    applyScopedLedgerDocumentOrganizationDefaults();
  } catch {
    message.error('公司选项加载失败');
  } finally {
    companyOptionsLoading.value = false;
  }
}

async function loadOrganizationScopeTree() {
  if (organizationNodes.value.length > 0) {
    syncCompanyOptionsFromOrganizationTree();
    applyScopedLedgerDocumentOrganizationDefaults();
    return;
  }
  try {
    organizationNodes.value = await getSystemFullOrganizationTreeApi();
    syncCompanyOptionsFromOrganizationTree();
    applyScopedLedgerOrganizationDefaults();
    applyScopedLedgerDocumentOrganizationDefaults();
  } catch {
    organizationNodes.value = [];
    companyOptions.value = [];
  }
}

async function loadDepartmentOptions(companyId?: SystemManagementApi.Id) {
  await loadOrganizationScopeTree();
  departmentOptionsLoading.value = true;
  try {
    syncDepartmentOptionsFromOrganizationTree(companyId);
  } catch {
    message.error('部门选项加载失败');
  } finally {
    departmentOptionsLoading.value = false;
  }
}

async function loadTeamOptions(parentOrgId?: SystemManagementApi.Id) {
  await loadOrganizationScopeTree();
  teamOptionsLoading.value = true;
  try {
    syncTeamOptionsFromOrganizationTree(parentOrgId);
  } catch {
    message.error('班组选项加载失败');
  } finally {
    teamOptionsLoading.value = false;
  }
}

async function loadDocumentDepartmentOptions(
  companyId?: SystemManagementApi.Id,
) {
  await loadOrganizationScopeTree();
  if (!companyId) {
    documentDepartmentOptions.value = [];
    return;
  }
  documentDepartmentOptionsLoading.value = true;
  try {
    syncDocumentDepartmentOptionsFromOrganizationTree(companyId);
  } catch {
    message.error('部门选项加载失败');
  } finally {
    documentDepartmentOptionsLoading.value = false;
  }
}

async function loadDocumentTeamOptions(parentOrgId?: SystemManagementApi.Id) {
  await loadOrganizationScopeTree();
  if (!parentOrgId) {
    documentTeamOptions.value = [];
    return;
  }
  documentTeamOptionsLoading.value = true;
  try {
    syncDocumentTeamOptionsFromOrganizationTree(parentOrgId);
  } catch {
    message.error('班组选项加载失败');
  } finally {
    documentTeamOptionsLoading.value = false;
  }
}

async function hydrateDocumentOrgOptions() {
  await loadDocumentDepartmentOptions(
    documentForm.companyId as SystemManagementApi.Id,
  );
  applyScopedLedgerDocumentOrganizationDefaults();
  const department = findOptionByLabel(
    documentDepartmentOptions.value,
    documentForm.department,
  ) ?? findOptionByValue(
    documentDepartmentSelectOptions.value,
    documentDepartmentValue.value,
  );
  documentDepartmentValue.value = department?.value;
  if (department) {
    await loadDocumentTeamOptions(department.value);
    applyScopedLedgerDocumentOrganizationDefaults();
    const team = findOptionByLabel(documentTeamOptions.value, documentForm.team) ??
      findOptionByValue(documentTeamSelectOptions.value, documentTeamValue.value);
    documentTeamValue.value = team?.value;
  }
  hydrateScopedLedgerDocumentOrganizationLabels();
}

function handleDocumentCompanyChange() {
  if (isLedgerOrganizationFieldLocked('company')) {
    void enforceScopedLedgerDocumentOrganizationDefaults();
    return;
  }
  documentForm.department = '';
  documentForm.team = '';
  documentDepartmentValue.value = undefined;
  documentTeamValue.value = undefined;
  documentTeamOptions.value = [];
  void loadDocumentDepartmentOptions(
    documentForm.companyId as SystemManagementApi.Id,
  );
}

function handleDocumentDepartmentChange(value: unknown) {
  if (isLedgerOrganizationFieldLocked('department')) {
    void enforceScopedLedgerDocumentOrganizationDefaults();
    return;
  }
  const normalizedValue = normalizeDocumentSelectValue(value);
  documentDepartmentValue.value = normalizedValue;
  const option = findOptionByValue(
    documentDepartmentSelectOptions.value,
    normalizedValue,
  );
  documentForm.department = option?.label ?? '';
  documentForm.team = '';
  documentTeamValue.value = undefined;
  documentTeamOptions.value = [];
  if (normalizedValue && !String(normalizedValue).startsWith('legacy:')) {
    void loadDocumentTeamOptions(normalizedValue);
  }
}

function handleDocumentTeamChange(value: unknown) {
  if (isLedgerOrganizationFieldLocked('team')) {
    void enforceScopedLedgerDocumentOrganizationDefaults();
    return;
  }
  const normalizedValue = normalizeDocumentSelectValue(value);
  documentTeamValue.value = normalizedValue;
  const option = findOptionByValue(
    documentTeamSelectOptions.value,
    normalizedValue,
  );
  documentForm.team = option?.label ?? '';
}

function openCreateDocument() {
  if (!isDocumentLedger.value) {
    return;
  }
  resetDocumentForm();
  createDocumentOpen.value = true;
  void loadCompanyOptions().then(() =>
    enforceScopedLedgerDocumentOrganizationDefaults(),
  );
}

function openEditDocument(record: Record<string, any>) {
  if (!isDocumentLedger.value) {
    return;
  }
  resetDocumentForm();
  editingDocumentId.value = record.id as PinganSafetyLedgerApi.Id;
  existingDocumentFileName.value = String(record.file || '');
  documentForm.companyId = record.companyId as PinganSafetyLedgerApi.Id;
  documentForm.date = String(record.date || '');
  documentForm.department = String(record.department || '');
  documentForm.name = String(record.name || '');
  documentForm.richText = String(record.richText || '');
  documentForm.team = String(record.team || '');
  documentForm.type = String(record.type || '');
  applyScopedLedgerDocumentOrganizationDefaults();
  createDocumentOpen.value = true;
  void loadCompanyOptions().then(() => hydrateDocumentOrgOptions());
}

function handleDocumentFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    documentForm.file = undefined;
    return;
  }
  if (!isAllowedDocumentFile(file)) {
    message.warning('仅支持上传 PDF、Word 文件');
    input.value = '';
    documentForm.file = undefined;
    return;
  }
  documentForm.file = file;
}

async function handleCreateDocument() {
  await enforceScopedLedgerDocumentOrganizationDefaults();
  if (
    !documentForm.name.trim() ||
    !documentForm.companyId ||
    (!editingDocumentId.value && !documentForm.file)
  ) {
    message.warning('请填写名称、所属企业并上传文件');
    return;
  }

  creatingDocument.value = true;
  try {
    const payload = {
      companyId: documentForm.companyId,
      date: documentForm.date,
      department: documentForm.department.trim(),
      file: documentForm.file,
      ledgerKey: entry.value.path,
      name: documentForm.name.trim(),
      richText: documentForm.richText.trim(),
      team: documentForm.team.trim(),
      type: documentForm.type.trim(),
    };
    if (editingDocumentId.value) {
      await updateSafetyLedgerDocumentApi(editingDocumentId.value, payload);
    } else {
      await createSafetyLedgerDocumentApi(payload);
    }
    message.success(editingDocumentId.value ? '台账已修改' : '台账已新增');
    createDocumentOpen.value = false;
    resetDocumentForm();
    page.value = 1;
    await loadRows();
  } catch {
    message.error(editingDocumentId.value ? '台账修改失败' : '台账新增失败');
  } finally {
    creatingDocument.value = false;
  }
}

function handleDeleteDocument(record: Record<string, any>) {
  Modal.confirm({
    content: `确认删除台账「${displayValue(record.name)}」？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteSafetyLedgerDocumentApi(record.id as PinganSafetyLedgerApi.Id);
      selectedRowKeys.value = selectedRowKeys.value.filter(
        (key) => key !== String(record.id),
      );
      message.success('台账已删除');
      await loadRows();
    },
    title: '确认删除？',
  });
}

function handleBatchDeleteDocuments() {
  if (!canBatchDelete.value) {
    return;
  }
  Modal.confirm({
    content: `将删除已选 ${selectedRowKeys.value.length} 条台账记录。`,
    okText: '批量删除',
    okType: 'danger',
    onOk: async () => {
      deletingDocuments.value = true;
      try {
        await batchDeleteSafetyLedgerDocumentsApi([...selectedRowKeys.value]);
        selectedRowKeys.value = [];
        message.success('批量删除成功');
        await loadRows();
      } catch {
        message.error('批量删除失败');
      } finally {
        deletingDocuments.value = false;
      }
    },
    title: '确认批量删除？',
  });
}

function displayFileName(record: Record<string, any>) {
  return displayValue(record.file);
}

watch(
  currentAncestorRouteNames,
  (routeNames) => {
    expandedLedgerRouteNames.value = [
      ...new Set([...expandedLedgerRouteNames.value, ...routeNames]),
    ];
  },
  { immediate: true },
);

watch(
  () => route.name,
  () => {
    page.value = 1;
    selectedRowKeys.value = [];
    createDocumentOpen.value = false;
    tableConfigOpen.value = false;
    resetLedgerFilters();
    resetDocumentForm();
    loadTableConfig();
    void loadRows();
  },
  { immediate: true },
);

watch(
  isDocumentLedger,
  (enabled) => {
    if (enabled) {
      void loadCompanyOptions();
    } else {
      selectedRowKeys.value = [];
    }
  },
  { immediate: true },
);

watch(
  renderedFilterFields,
  (fields) => {
    if (
      fields.includes('company') ||
      fields.includes('checkUnit') ||
      fields.includes('inspectedUnit')
    ) {
      void loadCompanyOptions();
    }
    if (fields.includes('department') || fields.includes('workshop')) {
      void loadDepartmentOptions(filters.companyId as SystemManagementApi.Id);
    }
    if (fields.includes('team')) {
      void loadTeamOptions(
        (filters.departmentId ?? filters.workshopId) as SystemManagementApi.Id,
      );
    }
  },
  { immediate: true },
);

watch(
  () => filters.companyId,
  (companyId) => {
    if (isLedgerOrganizationFieldLocked('company')) {
      applyScopedLedgerOrganizationDefaults();
      companyId = filters.companyId;
    }
    if (isLedgerOrganizationFieldLocked('department')) {
      applyScopedLedgerOrganizationDefaults();
      void loadDepartmentOptions(companyId as SystemManagementApi.Id);
      return;
    }
    filters.departmentId = undefined;
    filters.workshopId = undefined;
    filters.teamId = undefined;
    teamOptions.value = [];
    if (
      renderedFilterFields.value.includes('department') ||
      renderedFilterFields.value.includes('workshop')
    ) {
      void loadDepartmentOptions(companyId as SystemManagementApi.Id);
    }
  },
);

watch(
  () => filters.departmentId,
  (departmentId) => {
    if (isLedgerOrganizationFieldLocked('department')) {
      applyScopedLedgerOrganizationDefaults();
      departmentId = filters.departmentId;
    }
    filters.teamId = undefined;
    if (renderedFilterFields.value.includes('team') && departmentId) {
      void loadTeamOptions(departmentId as SystemManagementApi.Id);
    }
  },
);

watch(
  () => filters.workshopId,
  (workshopId) => {
    if (isLedgerOrganizationFieldLocked('department')) {
      applyScopedLedgerOrganizationDefaults();
      workshopId = filters.workshopId;
    }
    filters.teamId = undefined;
    if (renderedFilterFields.value.includes('team') && workshopId) {
      void loadTeamOptions(workshopId as SystemManagementApi.Id);
    }
  },
);
</script>

<template>
  <Page auto-content-height>
    <div
      class="safety-ledger-shell"
      :class="{ 'is-directory-collapsed': ledgerDirectoryCollapsed }"
    >
      <aside
        class="ledger-directory"
        :class="{ 'is-collapsed': ledgerDirectoryCollapsed }"
      >
        <template v-if="!ledgerDirectoryCollapsed">
          <div class="ledger-directory__header">
            <div>
              <h3>台账目录</h3>
            </div>
            <Button
              class="ledger-directory__collapse-button"
              size="small"
              type="text"
              @click="ledgerDirectoryCollapsed = true"
            >
              收起
            </Button>
          </div>
          <Input
            v-model:value="ledgerDirectoryKeyword"
            allow-clear
            class="ledger-directory__search"
            placeholder="搜索台账"
          />

          <div class="ledger-directory__list">
            <Empty
              v-if="filteredLedgerSections.length === 0"
              description="未找到匹配台账"
              :image="Empty.PRESENTED_IMAGE_SIMPLE"
            />
            <div
              v-for="section in filteredLedgerSections"
              :key="section.routeName"
              class="ledger-directory__section"
            >
              <button
                class="ledger-directory__group"
                :class="{ 'is-current': isLedgerSectionCurrent(section) }"
                type="button"
                @click="toggleLedgerSection(section)"
              >
                <span
                  class="ledger-directory__caret"
                  :class="{ 'is-open': isLedgerSectionExpanded(section) }"
                >
                  &gt;
                </span>
                <span class="ledger-directory__title" :title="section.title">
                  {{ section.title }}
                </span>
              </button>

              <div
                v-show="isLedgerSectionExpanded(section)"
                class="ledger-directory__children"
              >
                <template
                  v-for="item in section.children"
                  :key="item.routeName"
                >
                  <button
                    v-if="isSafetyLedgerEntry(item)"
                    class="ledger-directory__entry"
                    :class="{ 'is-active': isLedgerEntryActive(item) }"
                    type="button"
                    @click="goLedgerEntry(item)"
                  >
                    <span :title="item.title">{{ item.title }}</span>
                  </button>
                  <div v-else class="ledger-directory__subsection">
                    <button
                      class="ledger-directory__group ledger-directory__group--sub"
                      :class="{ 'is-current': isLedgerSectionCurrent(item) }"
                      type="button"
                      @click="toggleLedgerSection(item)"
                    >
                      <span
                        class="ledger-directory__caret"
                        :class="{ 'is-open': isLedgerSectionExpanded(item) }"
                      >
                        &gt;
                      </span>
                      <span class="ledger-directory__title" :title="item.title">
                        {{ item.title }}
                      </span>
                    </button>
                    <div
                      v-show="isLedgerSectionExpanded(item)"
                      class="ledger-directory__children ledger-directory__children--sub"
                    >
                      <template
                        v-for="child in item.children"
                        :key="child.routeName"
                      >
                        <button
                          v-if="isSafetyLedgerEntry(child)"
                          class="ledger-directory__entry ledger-directory__entry--sub"
                          :class="{ 'is-active': isLedgerEntryActive(child) }"
                          type="button"
                          @click="goLedgerEntry(child)"
                        >
                          <span :title="child.title">{{ child.title }}</span>
                        </button>
                      </template>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </template>
        <button
          v-else
          class="ledger-directory__expand-button"
          title="展开台账目录"
          type="button"
          @click="ledgerDirectoryCollapsed = false"
        >
          &gt;
        </button>
      </aside>

      <main class="safety-ledger-content">
        <div class="safety-ledger">
          <header class="safety-ledger__header">
            <div>
              <h2>{{ entry.title }}</h2>
            </div>
          </header>

          <section class="safety-ledger__filters">
            <label
              v-for="field in renderedFilterFields"
              :key="field"
              class="safety-ledger-filter-field"
            >
              <span>{{ filterFieldLabel(field) }}</span>
              <Select
                v-if="field === 'company'"
                v-model:value="filters.companyId"
                :allow-clear="!isLedgerFilterFieldLocked(field)"
                :disabled="isLedgerFilterFieldLocked(field)"
                :loading="companyOptionsLoading"
                :options="companyOptions"
                class="safety-ledger-filter-field__control"
                placeholder="请选择公司"
                show-search
              />
              <Select
                v-else-if="field === 'department'"
                v-model:value="filters.departmentId"
                :allow-clear="!isLedgerFilterFieldLocked(field)"
                :disabled="isLedgerFilterFieldLocked(field)"
                :loading="departmentOptionsLoading"
                :options="departmentOptions"
                class="safety-ledger-filter-field__control"
                placeholder="请选择部门"
                show-search
              />
              <Select
                v-else-if="field === 'workshop'"
                v-model:value="filters.workshopId"
                :allow-clear="!isLedgerFilterFieldLocked(field)"
                :disabled="isLedgerFilterFieldLocked(field)"
                :loading="departmentOptionsLoading"
                :options="departmentOptions"
                class="safety-ledger-filter-field__control"
                placeholder="请选择车间"
                show-search
              />
              <Select
                v-else-if="field === 'team'"
                v-model:value="filters.teamId"
                :allow-clear="!isLedgerFilterFieldLocked(field)"
                :disabled="isLedgerFilterFieldLocked(field)"
                :loading="teamOptionsLoading"
                :options="teamOptions"
                class="safety-ledger-filter-field__control"
                placeholder="请选择班组"
                show-search
              />
              <Select
                v-else-if="field === 'checkUnit'"
                v-model:value="filters.checkUnit"
                :loading="companyOptionsLoading"
                :options="companyUnitOptions"
                allow-clear
                class="safety-ledger-filter-field__control"
                placeholder="请选择检查单位"
                show-search
              />
              <Select
                v-else-if="field === 'inspectedUnit'"
                v-model:value="filters.inspectedUnit"
                :loading="companyOptionsLoading"
                :options="companyUnitOptions"
                allow-clear
                class="safety-ledger-filter-field__control"
                placeholder="请选择受检单位"
                show-search
              />
              <Select
                v-else-if="field === 'status'"
                v-model:value="filters.status"
                :options="statusOptions"
                allow-clear
                class="safety-ledger-filter-field__control"
                placeholder="请选择状态"
              />
              <DatePicker
                v-else-if="field === 'dateStart'"
                v-model:value="filters.dateStart"
                class="safety-ledger__date"
                placeholder="年/月/日"
                value-format="YYYY-MM-DD"
              />
              <DatePicker
                v-else-if="field === 'dateEnd'"
                v-model:value="filters.dateEnd"
                class="safety-ledger__date"
                placeholder="年/月/日"
                value-format="YYYY-MM-DD"
              />
              <DatePicker
                v-else-if="field === 'uploadedStart'"
                v-model:value="filters.uploadedStart"
                class="safety-ledger__date"
                placeholder="年/月/日"
                value-format="YYYY-MM-DD"
              />
              <DatePicker
                v-else-if="field === 'uploadedEnd'"
                v-model:value="filters.uploadedEnd"
                class="safety-ledger__date"
                placeholder="年/月/日"
                value-format="YYYY-MM-DD"
              />
              <DatePicker
                v-else-if="isDateFilterField(field)"
                class="safety-ledger__date"
                placeholder="年/月/日"
                value-format="YYYY-MM-DD"
              />
            </label>
            <Button
              class="safety-ledger__filter-button"
              type="primary"
              @click="search"
            >
              搜索
            </Button>
            <Button
              class="safety-ledger__filter-button"
              @click="tableConfigOpen = true"
            >
              表格配置
            </Button>
            <div v-if="isDocumentLedger" class="safety-ledger__actions">
              <span v-if="selectedRowKeys.length" class="safety-ledger__selected">
                已选 {{ selectedRowKeys.length }} 条
              </span>
              <Button type="primary" @click="openCreateDocument">新增台账</Button>
              <Button
                danger
                :disabled="!canBatchDelete"
                :loading="deletingDocuments"
                @click="handleBatchDeleteDocuments"
              >
                批量删除
              </Button>
            </div>
          </section>

          <Table
            :columns="tableColumns"
            :data-source="rows"
            :loading="loading"
            :pagination="pagination"
            :row-key="rowKey"
            :row-selection="tableRowSelection"
            :scroll="tableScroll"
            class="safety-ledger__table"
            size="middle"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <div
                v-if="column.dataIndex === 'operation' && isDocumentLedger"
                class="document-row-actions"
              >
                <Button size="small" type="link" @click="openEditDocument(record)">
                  修改
                </Button>
                <Button danger size="small" type="link" @click="handleDeleteDocument(record)">
                  删除
                </Button>
              </div>
              <Button
                v-else-if="column.dataIndex === 'operation' && canOpenMeetingReport(record)"
                size="small"
                type="link"
                @click="openMeetingReport(record)"
              >
                记录表
              </Button>
              <span v-else-if="column.dataIndex === 'operation'" />
              <span
                v-else-if="column.dataIndex === 'imageCheck' && record.imagePreviewUrl"
                class="safety-ledger-media-cell"
              >
                <button
                  class="safety-ledger-media-preview safety-ledger-media-preview--image"
                  type="button"
                  @click="openMediaPreview('image', record.imagePreviewUrl)"
                >
                  <img :alt="displayValue(record.imageCheck)" :src="String(record.imagePreviewUrl)" />
                </button>
              </span>
              <span
                v-else-if="column.dataIndex === 'videoCheck' && record.videoPreviewUrl"
                class="safety-ledger-media-cell"
              >
                <button
                  class="safety-ledger-media-preview safety-ledger-media-preview--video"
                  type="button"
                  @click="openMediaPreview('video', record.videoPreviewUrl)"
                >
                  <video :src="String(record.videoPreviewUrl)" muted preload="metadata" />
                </button>
              </span>
              <a
                v-else-if="column.dataIndex === 'file' && record.fileUrl"
                :href="String(record.fileUrl)"
                rel="noreferrer"
                target="_blank"
              >
                {{ displayFileName(record) }}
              </a>
              <span v-else>{{ displayValue(record[column.dataIndex as string]) }}</span>
            </template>
            <template #emptyText>
              <Empty description="暂无台账数据" />
            </template>
          </Table>
        </div>
      </main>
    </div>

    <Modal
      v-model:open="createDocumentOpen"
      :confirm-loading="creatingDocument"
      destroy-on-close
      :title="documentModalTitle"
      width="640px"
      @cancel="resetDocumentForm"
      @ok="handleCreateDocument"
    >
      <div class="document-form">
        <label class="document-form__field">
          <span>名称*</span>
          <Input v-model:value="documentForm.name" placeholder="请输入名称" />
        </label>
        <label class="document-form__field">
          <span>所属企业*</span>
          <Select
            v-model:value="documentForm.companyId"
            :disabled="isLedgerOrganizationFieldLocked('company')"
            :loading="companyOptionsLoading"
            :options="companyOptions"
            allow-clear
            placeholder="请选择公司管理中的公司"
            show-search
            @change="handleDocumentCompanyChange"
          />
        </label>
        <label class="document-form__field">
          <span>富文本</span>
          <Input.TextArea
            v-model:value="documentForm.richText"
            :rows="3"
            placeholder="请输入补充说明"
          />
        </label>
        <template v-if="isDocumentOrgLedger">
          <label class="document-form__field">
            <span>部门</span>
            <Select
              v-model:value="documentDepartmentValue"
              :disabled="!documentForm.companyId || isLedgerOrganizationFieldLocked('department')"
              :loading="documentDepartmentOptionsLoading"
              :options="documentDepartmentSelectOptions"
              allow-clear
              placeholder="请选择部门"
              show-search
              @change="handleDocumentDepartmentChange"
            />
          </label>
          <label class="document-form__field">
            <span>班组</span>
            <Select
              v-model:value="documentTeamValue"
              :disabled="!documentDepartmentValue || isLedgerOrganizationFieldLocked('team')"
              :loading="documentTeamOptionsLoading"
              :options="documentTeamSelectOptions"
              allow-clear
              placeholder="请选择班组"
              show-search
              @change="handleDocumentTeamChange"
            />
          </label>
          <label class="document-form__field">
            <span>类型</span>
            <Input v-model:value="documentForm.type" placeholder="请输入类型" />
          </label>
          <label class="document-form__field">
            <span>日期</span>
            <DatePicker
              v-model:value="documentForm.date"
              class="safety-ledger__date"
              placeholder="年/月/日"
              value-format="YYYY-MM-DD"
            />
          </label>
        </template>
        <label class="document-form__field document-form__field--file">
          <span>{{ editingDocumentId ? '文件' : '文件*' }}</span>
          <div class="document-form__file-row">
            <Button @click="documentFileInputRef?.click()">选择文件</Button>
            <span class="document-form__file-name">
              {{ documentForm.file?.name || existingDocumentFileName || '支持 PDF、Word 文件' }}
            </span>
          </div>
          <input
            ref="documentFileInputRef"
            accept=".pdf,.doc,.docx"
            class="document-form__file-input"
            type="file"
            @change="handleDocumentFileChange"
          />
        </label>
      </div>
    </Modal>

    <Modal
      v-model:open="meetingReportOpen"
      :confirm-loading="meetingReportLoading"
      destroy-on-close
      title="班前会议记录表"
      width="900px"
    >
      <div v-if="meetingReport" class="meeting-report-print-area">
        <div class="meeting-report">
          <header class="meeting-report__header">
            <div class="meeting-report__logo">Demo Safety Operations</div>
            <div>
              <h3>Demo Safety Operations平安班组班前会议记录表</h3>
              <p>
                单位：{{ meetingReport.company || '-' }}
                <span>班组：{{ meetingReport.team || '-' }}</span>
                <span>时间：{{ meetingReport.time || '-' }}</span>
              </p>
            </div>
          </header>
          <table class="meeting-report__table">
            <tbody>
              <tr>
                <th>单位</th>
                <td>{{ meetingReport.company || '-' }}</td>
                <th>班组</th>
                <td>{{ meetingReport.team || '-' }}</td>
                <th>时间</th>
                <td>{{ meetingReport.time || '-' }}</td>
              </tr>
              <tr
                v-for="section in meetingReport.sections"
                :key="section.label"
              >
                <th>{{ section.label }}</th>
                <td colspan="5">{{ section.value }}</td>
              </tr>
              <tr>
                <th>图片打卡</th>
                <td colspan="2">
                  <button
                    v-if="meetingReport.meeting?.imagePreviewUrl"
                    class="safety-ledger-media-preview safety-ledger-media-preview--image"
                    type="button"
                    @click="openMediaPreview('image', meetingReport.meeting.imagePreviewUrl)"
                  >
                    <img
                      :alt="displayValue(meetingReport.meeting?.imageCheck)"
                      :src="meetingReport.meeting.imagePreviewUrl"
                    />
                  </button>
                  <span v-else>{{ displayValue(meetingReport.meeting?.imageCheck) }}</span>
                </td>
                <th>视频打卡</th>
                <td colspan="2">
                  <button
                    v-if="meetingReport.meeting?.videoPreviewUrl"
                    class="safety-ledger-media-preview safety-ledger-media-preview--video"
                    type="button"
                    @click="openMediaPreview('video', meetingReport.meeting.videoPreviewUrl)"
                  >
                    <video :src="meetingReport.meeting.videoPreviewUrl" muted preload="metadata" />
                  </button>
                  <span v-else>{{ displayValue(meetingReport.meeting?.videoCheck) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <Empty v-else description="记录表加载中" />
      <template #footer>
        <Button @click="meetingReportOpen = false">关闭</Button>
        <Button
          :disabled="!meetingReport"
          :loading="meetingReportLoading"
          type="primary"
          @click="printMeetingReport"
        >
          打印/导出 PDF
        </Button>
      </template>
    </Modal>

    <Modal
      v-model:open="mediaPreviewOpen"
      destroy-on-close
      :footer="null"
      :title="mediaPreview?.title"
      width="760px"
    >
      <div v-if="mediaPreview" class="safety-ledger-media-modal">
        <img
          v-if="mediaPreview.type === 'image'"
          alt="图片打卡预览"
          :src="mediaPreview.url"
        />
        <video
          v-else
          controls
          :src="mediaPreview.url"
        />
      </div>
    </Modal>

    <SafetyLedgerTableConfigDrawer
      v-model:open="tableConfigOpen"
      :auto-widths="autoWidths"
      :config="tableConfig"
      :module-title="entry.title"
      @delete="deleteTableConfig"
      @push="saveTableConfig"
      @save="saveTableConfig"
    />
  </Page>
</template>

<style scoped>
.safety-ledger-shell {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
  min-width: 0;
}

.safety-ledger-shell.is-directory-collapsed {
  grid-template-columns: 48px minmax(0, 1fr);
}

.ledger-directory {
  position: sticky;
  top: 12px;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 12px;
  height: calc(100vh - 112px);
  max-height: calc(100vh - 112px);
  min-width: 0;
  padding: 12px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.ledger-directory.is-collapsed {
  grid-template-rows: minmax(0, 1fr);
  align-items: start;
  justify-items: center;
  padding: 8px 6px;
}

.ledger-directory__header {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  justify-content: space-between;
}

.ledger-directory__header h3 {
  margin: 0;
  color: #111827;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.3;
}

.ledger-directory__collapse-button {
  flex: 0 0 auto;
  color: #64748b;
  font-weight: 700;
}

.ledger-directory__expand-button {
  display: grid;
  width: 30px;
  height: 30px;
  padding: 0;
  place-items: center;
  color: #0958d9;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.2;
  cursor: pointer;
  background: #e6f4ff;
  border: 1px solid #bae0ff;
  border-radius: 6px;
}

.ledger-directory__expand-button:hover {
  background: #d6ecff;
}

.ledger-directory__search {
  width: 100%;
}

.ledger-directory__list {
  position: relative;
  display: grid;
  gap: 6px;
  align-content: start;
  min-height: 0;
  padding-right: 12px;
  overflow-x: hidden;
  overflow-y: scroll;
  overscroll-behavior: contain;
  background:
    linear-gradient(to left, #eef2f7 0 8px, transparent 8px) right top /
    8px 100% no-repeat;
  scrollbar-color: #cbd5e1 #f1f5f9;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
}

.ledger-directory__list::-webkit-scrollbar {
  width: 8px;
}

.ledger-directory__list::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 999px;
}

.ledger-directory__list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border: 2px solid #f1f5f9;
  border-radius: 999px;
}

.ledger-directory__list::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

.ledger-directory__section,
.ledger-directory__subsection {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.ledger-directory__group,
.ledger-directory__entry {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
  min-width: 0;
  height: 32px;
  padding: 0 8px;
  color: #334155;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 6px;
}

.ledger-directory__group {
  color: #0f172a;
  font-weight: 800;
}

.ledger-directory__group--sub {
  height: 30px;
  padding-left: 14px;
  color: #475569;
  font-size: 13px;
}

.ledger-directory__entry {
  padding-left: 26px;
  font-size: 13px;
  font-weight: 600;
}

.ledger-directory__entry--sub {
  padding-left: 40px;
}

.ledger-directory__group:hover,
.ledger-directory__entry:hover {
  color: #0958d9;
  background: #f1f5f9;
}

.ledger-directory__group.is-current {
  color: #0958d9;
}

.ledger-directory__entry.is-active {
  color: #0958d9;
  background: #e6f4ff;
}

.ledger-directory__caret {
  flex: 0 0 auto;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1;
  transition: transform 0.18s ease;
}

.ledger-directory__caret.is-open {
  transform: rotate(90deg);
}

.ledger-directory__title,
.ledger-directory__entry span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ledger-directory__children {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding-left: 8px;
  margin-left: 10px;
  border-left: 1px solid #e2e8f0;
}

.ledger-directory__children--sub {
  gap: 2px;
  padding-left: 6px;
  margin-left: 16px;
}

.safety-ledger-content {
  min-width: 0;
}

.safety-ledger {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.safety-ledger__header {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  padding: 2px 2px 0;
}

.safety-ledger__header h2 {
  margin: 4px 0 0;
  color: #111827;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.3;
}

.safety-ledger__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  padding: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.safety-ledger__actions {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-left: auto;
}

.safety-ledger__selected {
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.safety-ledger-filter-field {
  display: grid;
  gap: 4px;
  min-width: 150px;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.safety-ledger-filter-field__control {
  width: 170px;
}

.safety-ledger__filter-button {
  align-self: end;
}

.safety-ledger__date {
  width: 150px;
  height: 32px;
  padding: 4px 10px;
  color: #1f2937;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  outline: none;
}

.safety-ledger__date:focus {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgb(5 145 255 / 10%);
}

.safety-ledger__table {
  min-width: 0;
}

:deep(.safety-ledger__table .ant-table) {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

:deep(.safety-ledger__table .ant-table-thead > tr > th) {
  color: #475569;
  font-weight: 800;
  background: #f8fafc;
}

.document-row-actions {
  display: flex;
  gap: 4px;
  align-items: center;
  min-width: 0;
}

.safety-ledger-media-cell {
  display: flex;
  gap: 6px;
  align-items: center;
  min-width: 0;
}

.safety-ledger-media-preview {
  display: inline-grid;
  flex: 0 0 auto;
  width: 46px;
  height: 34px;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  background: #f8fafc;
  border: 1px solid #dbeafe;
  border-radius: 4px;
}

.safety-ledger-media-preview img,
.safety-ledger-media-preview video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.safety-ledger-media-preview--video {
  background: #0f172a;
}

.safety-ledger-media-modal {
  display: grid;
  place-items: center;
  min-height: 240px;
  background: #0f172a;
  border-radius: 6px;
}

.safety-ledger-media-modal img,
.safety-ledger-media-modal video {
  display: block;
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

.document-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.document-form__field {
  display: grid;
  gap: 6px;
  min-width: 0;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.document-form__field:nth-child(3),
.document-form__field--file {
  grid-column: 1 / -1;
}

.document-form__file-row {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.document-form__file-name {
  min-width: 0;
  overflow: hidden;
  color: #64748b;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-form__file-input {
  display: none;
}

.meeting-report-print-area {
  background: #fff;
}

.meeting-report {
  padding: 18px;
  color: #111827;
  background: #fff;
}

.meeting-report__header {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  margin-bottom: 14px;
}

.meeting-report__logo {
  display: grid;
  height: 58px;
  place-items: center;
  color: #0f172a;
  font-size: 16px;
  font-weight: 800;
  border: 1px solid #111827;
}

.meeting-report__header h3 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  line-height: 1.4;
  text-align: center;
}

.meeting-report__header p {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  justify-content: center;
  margin: 8px 0 0;
  color: #374151;
  font-size: 13px;
}

.meeting-report__table {
  width: 100%;
  border-collapse: collapse;
}

.meeting-report__table th,
.meeting-report__table td {
  min-height: 42px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.6;
  vertical-align: top;
  border: 1px solid #1f2937;
}

.meeting-report__table th {
  width: 110px;
  font-weight: 800;
  text-align: center;
  background: #f8fafc;
}

@media (max-width: 1100px) {
  .safety-ledger-shell {
    grid-template-columns: minmax(0, 1fr);
  }

  .ledger-directory {
    position: static;
    height: 320px;
    max-height: 320px;
  }
}

@media (max-width: 720px) {
  .document-form {
    grid-template-columns: minmax(0, 1fr);
  }

  .safety-ledger__actions {
    width: 100%;
    margin-left: 0;
  }
}

@media print {
  :global(body *) {
    visibility: hidden !important;
  }

  :global(.meeting-report-print-area),
  :global(.meeting-report-print-area *) {
    visibility: visible !important;
  }

  :global(.meeting-report-print-area) {
    position: fixed;
    inset: 0;
    width: 100%;
    padding: 0;
    background: #fff;
  }

  .meeting-report {
    padding: 0;
  }

  .meeting-report__table th,
  .meeting-report__table td {
    border-color: #000;
  }
}
</style>


