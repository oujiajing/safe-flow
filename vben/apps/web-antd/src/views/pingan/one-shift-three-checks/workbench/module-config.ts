import { canShowPinganDataMap } from '#/views/pingan/shared/data-map-permission';
import {
  getVisiblePinganOrganizationFilterKeys,
  isPinganDepartmentScopedRole,
  isPinganMemberRole,
  isPinganOrganizationFilterLocked,
  type PinganOrganizationFilterKey,
} from '#/views/pingan/shared/organization-filter-permission';

export interface ThreeCheckTableColumn {
  dataIndex: string;
  fixed?: 'left' | 'right';
  title: string;
  width: number;
}

export type ThreeCheckCreateMode = 'API' | 'DISABLED' | 'LOCAL_TABLE';
export type ThreeCheckApiMode =
  | 'NONE'
  | 'PRE_SHIFT_MEETING'
  | 'THREE_CHECK_RECORD';
export type ThreeCheckLocalAction = 'REMIND' | 'SUBMIT' | 'WITHDRAW';
export type ThreeCheckLocalStatus = 'ARCHIVED' | 'DRAFT' | 'OPENED' | 'WITHDRAWN';
export interface ThreeCheckFieldOption {
  label: string;
  value: string;
}

export type ThreeCheckStatusBadgeTone =
  | 'blue'
  | 'gray'
  | 'green'
  | 'orange'
  | 'red';
export type ThreeCheckAttachmentColumnKind = 'IMAGE' | 'VIDEO';
export type ThreeCheckDetailActionKey =
  | 'attachment'
  | 'changeHistory'
  | 'documentFlow'
  | 'edit'
  | 'print'
  | 'refresh';

export interface ThreeCheckDetailAction {
  icon: string;
  key: ThreeCheckDetailActionKey;
  label: string;
}

const threeCheckDetailActions: ThreeCheckDetailAction[] = [
  { icon: 'lucide:refresh-cw', key: 'refresh', label: '刷新' },
  { icon: 'lucide:printer', key: 'print', label: '打印' },
  { icon: 'lucide:git-branch', key: 'documentFlow', label: '单据流' },
  { icon: 'lucide:paperclip', key: 'attachment', label: '附件' },
  { icon: 'lucide:history', key: 'changeHistory', label: '变更历史' },
  { icon: 'lucide:square-pen', key: 'edit', label: '修改' },
];

export const oneShiftThreeCheckRouteNames = [
  'PinganTeamDispatch',
  'PinganCurtainWallTeamDispatch',
  'PreShiftMeeting',
  'PinganPreShiftSafetyActivity',
  'PinganPreShiftInspection',
  'PinganMidShiftInspection',
  'PinganPostShiftInspection',
  'PinganKeySites',
] as const;

export type OneShiftThreeCheckRouteName =
  (typeof oneShiftThreeCheckRouteNames)[number];

export type OneShiftOrganizationFilterKey = PinganOrganizationFilterKey;

export function shouldShowOneShiftDataMap(roles: readonly string[] | undefined) {
  return canShowPinganDataMap(roles);
}

export function isOneShiftTeamMemberRole(roles: readonly string[] | undefined) {
  return isPinganMemberRole(roles);
}

export function isOneShiftDepartmentScopedRole(
  roles: readonly string[] | undefined,
) {
  return isPinganDepartmentScopedRole(roles);
}

export function getOneShiftVisibleOrganizationFilterKeys(
  roles: readonly string[] | undefined,
): OneShiftOrganizationFilterKey[] {
  return getVisiblePinganOrganizationFilterKeys(roles);
}

export function isOneShiftOrganizationFieldLocked(
  roles: readonly string[] | undefined,
  field: OneShiftOrganizationFilterKey,
) {
  return isPinganOrganizationFilterLocked(roles, field);
}

export interface ThreeCheckModuleRuntime {
  apiMode: ThreeCheckApiMode;
  apiReady: boolean;
  bizType: string;
  columns: ThreeCheckTableColumn[];
  createMode: ThreeCheckCreateMode;
  isPreShiftMeetingCompatible: boolean;
  moduleKey?: string;
  routeName: string;
  title: string;
}

export type OneShiftThreeCheckModuleConfig = ThreeCheckModuleRuntime;

export interface ThreeCheckDetailFieldGroup {
  columns: ThreeCheckTableColumn[];
  key: string;
  title: string;
}

export function getThreeCheckDetailActions() {
  return [...threeCheckDetailActions];
}

interface ThreeCheckModuleRuntimeInput {
  businessColumns: ThreeCheckTableColumn[];
  routeName: string;
  routeTitle: string;
}

export interface LocalThreeCheckRowInput {
  columns: ThreeCheckTableColumn[];
  now?: Date;
  routeName: string;
  values: Record<string, string | string[]>;
}

export interface ThreeCheckRecordEditableDetail {
  businessDate?: string;
  canSubmit?: boolean;
  companyId?: number | string;
  date?: string;
  departmentId?: number | string;
  id?: string;
  imageCheck?: string;
  owner?: string;
  ownerUserId?: number;
  payload?: Record<string, unknown>;
  rootDispatchRecordId?: number | string;
  status?: ThreeCheckLocalStatus | string;
  statusLabel?: string;
  teamId?: number | string;
  version?: number;
  videoCheck?: string;
}

export interface PreShiftMeetingRecordPayloadInput {
  attendeeNames: string[];
  meetingContent: string;
  statusLabel?: string;
}

export type TeamCheckInspectionStage =
  | 'KEY_SITES'
  | 'MID_SHIFT_INSPECTION'
  | 'POST_SHIFT_INSPECTION'
  | 'PRE_SHIFT_INSPECTION';

export interface TeamCheckInspectionLine {
  [key: string]: any;
  checkItem: string;
  checkResult: string;
  followUpPlan: string;
  libraryItemId?: string;
  lastRectificationAction: string;
  lastRectificationRemark: string;
  rectificationDescription: string;
  rectificationClosedAt: string;
  rectificationOrderId: string;
  rectificationOrderNo: string;
  rectificationStatus: string;
  rectificationStatusLabel: string;
  requireImage: boolean;
  requireVideo: boolean;
  riskType: string;
  sortOrder?: number;
  uploadDescription: string;
}

export interface PreShiftMeetingSafetyConfirmLine {
  [key: string]: any;
  confirmStatus: string;
  libraryItemId?: string;
  riskType: string;
  safetyItem: string;
  sortOrder?: number;
}

const operationColumn: ThreeCheckTableColumn = {
  dataIndex: 'actions',
  fixed: 'right',
  title: '操作',
  width: 300,
};

const oneShiftCompanyColumn: ThreeCheckTableColumn = {
  dataIndex: 'company',
  fixed: 'left',
  title: '公司',
  width: 180,
};

const preShiftMeetingBusinessColumns: ThreeCheckTableColumn[] = [
  oneShiftCompanyColumn,
  { dataIndex: 'department', title: '部门', width: 120 },
  { dataIndex: 'team', title: '班组', width: 160 },
  { dataIndex: 'owner', title: '负责人', width: 130 },
  { dataIndex: 'attendees', title: '参会人', width: 100 },
  { dataIndex: 'date', title: '开会日期', width: 130 },
  { dataIndex: 'imageCheck', title: '图片打卡', width: 120 },
  { dataIndex: 'videoCheck', title: '视频打卡', width: 120 },
  { dataIndex: 'status', title: '状态', width: 120 },
];

const teamDispatchBusinessColumns: ThreeCheckTableColumn[] = [
  oneShiftCompanyColumn,
  { dataIndex: 'department', title: '车间', width: 140 },
  { dataIndex: 'team', title: '班组', width: 160 },
  { dataIndex: 'teamTask', title: '班组任务', width: 160 },
  { dataIndex: 'dispatchType', title: '排班类型', width: 130 },
  { dataIndex: 'managerCount', title: '管理人员人数（手录）', width: 180 },
  { dataIndex: 'dispatchDate', title: '派班日期', width: 130 },
  { dataIndex: 'dispatchStatus', title: '派班状态', width: 120 },
  { dataIndex: 'dispatchTime', title: '派班时间', width: 150 },
];

const preShiftSafetyActivityBusinessColumns: ThreeCheckTableColumn[] = [
  oneShiftCompanyColumn,
  { dataIndex: 'department', title: '车间', width: 140 },
  { dataIndex: 'team', title: '班组', width: 160 },
  { dataIndex: 'workContent', title: '作业内容', width: 180 },
  { dataIndex: 'laborTeamCount', title: '劳务班组人数（手录）', width: 190 },
  { dataIndex: 'equipmentInspection', title: '设备是否点检', width: 140 },
  {
    dataIndex: 'safetyActivityRecordUpload',
    title: '班前安全活动记录表上传',
    width: 220,
  },
  { dataIndex: 'imageUpload', title: '图片上传', width: 120 },
  { dataIndex: 'videoUpload', title: '视频上传', width: 120 },
  { dataIndex: 'date', title: '日期', width: 120 },
  { dataIndex: 'createdAt', title: '创建时间', width: 160 },
  { dataIndex: 'status', title: '状态', width: 120 },
];

const preShiftInspectionBusinessColumns: ThreeCheckTableColumn[] = [
  oneShiftCompanyColumn,
  { dataIndex: 'department', title: '车间', width: 140 },
  { dataIndex: 'team', title: '班组', width: 160 },
  { dataIndex: 'owner', title: '负责人', width: 130 },
  { dataIndex: 'date', title: '检查日期', width: 130 },
  { dataIndex: 'imageCheck', title: '图片打卡', width: 120 },
  { dataIndex: 'status', title: '状态', width: 120 },
];

const midShiftInspectionBusinessColumns: ThreeCheckTableColumn[] = [
  oneShiftCompanyColumn,
  { dataIndex: 'department', title: '车间', width: 140 },
  { dataIndex: 'team', title: '班组', width: 160 },
  { dataIndex: 'owner', title: '负责人', width: 130 },
  { dataIndex: 'date', title: '检查日期', width: 130 },
  { dataIndex: 'imageCheck', title: '图片打卡', width: 120 },
  { dataIndex: 'videoCheck', title: '视频打卡', width: 120 },
  { dataIndex: 'status', title: '状态', width: 120 },
];

const postShiftInspectionBusinessColumns: ThreeCheckTableColumn[] = [
  oneShiftCompanyColumn,
  { dataIndex: 'department', title: '车间', width: 140 },
  { dataIndex: 'team', title: '班组', width: 160 },
  { dataIndex: 'owner', title: '负责人', width: 130 },
  { dataIndex: 'date', title: '检查日期', width: 130 },
  { dataIndex: 'imageCheck', title: '图片打卡', width: 120 },
  { dataIndex: 'handoverStatus', title: '交班状态', width: 120 },
  { dataIndex: 'status', title: '状态', width: 120 },
];

const teamCheckInspectionRouteStages: Record<
  string,
  TeamCheckInspectionStage
> = {
  PinganKeySites: 'KEY_SITES',
  PinganMidShiftInspection: 'MID_SHIFT_INSPECTION',
  PinganPostShiftInspection: 'POST_SHIFT_INSPECTION',
  PinganPreShiftInspection: 'PRE_SHIFT_INSPECTION',
};

const teamCheckInspectionColumns: ThreeCheckTableColumn[] = [
  { dataIndex: 'riskType', title: '风险', width: 150 },
  { dataIndex: 'checkItem', title: '检查项', width: 240 },
  { dataIndex: 'checkResult', title: '检查结果', width: 150 },
  { dataIndex: 'rectificationDescription', title: '隐患描述', width: 260 },
  { dataIndex: 'followUpPlan', title: '跟进方案', width: 160 },
  { dataIndex: 'uploadDescription', title: '上传说明', width: 260 },
  { dataIndex: 'rectificationOrderNo', title: '整改工单号', width: 180 },
  { dataIndex: 'rectificationStatus', title: '整改情况', width: 170 },
  { dataIndex: 'rectificationClosedAt', title: '闭环时间', width: 170 },
  { dataIndex: 'lastRectificationAction', title: '最近动作', width: 180 },
];

const preShiftMeetingSafetyConfirmColumns: ThreeCheckTableColumn[] = [
  { dataIndex: 'riskType', title: '风险', width: 180 },
  { dataIndex: 'safetyItem', title: '安全注意事项', width: 360 },
  { dataIndex: 'confirmStatus', title: '确认状态', width: 160 },
];

const keySitesBusinessColumns: ThreeCheckTableColumn[] = [
  oneShiftCompanyColumn,
  { dataIndex: 'siteType', title: '场所类型', width: 140 },
  { dataIndex: 'inspectionDepartment', title: '检查部门', width: 150 },
  { dataIndex: 'responsibleDepartment', title: '责任部门', width: 150 },
  { dataIndex: 'date', title: '检查日期', width: 130 },
  { dataIndex: 'responsiblePerson', title: '责任人', width: 130 },
  { dataIndex: 'acceptancePerson', title: '验收人', width: 130 },
  { dataIndex: 'status', title: '状态', width: 120 },
];

const businessColumnsByRouteName: Record<string, ThreeCheckTableColumn[]> = {
  PinganCurtainWallTeamDispatch: teamDispatchBusinessColumns,
  PinganKeySites: keySitesBusinessColumns,
  PinganMidShiftInspection: midShiftInspectionBusinessColumns,
  PinganPostShiftInspection: postShiftInspectionBusinessColumns,
  PinganPreShiftInspection: preShiftInspectionBusinessColumns,
  PinganPreShiftSafetyActivity: preShiftSafetyActivityBusinessColumns,
  PinganTeamDispatch: teamDispatchBusinessColumns,
  PreShiftMeeting: preShiftMeetingBusinessColumns,
};

const threeCheckRecordConfigByRouteName: Record<
  string,
  { bizType: string; moduleKey: string }
> = {
  PreShiftMeeting: {
    bizType: 'PRE_SHIFT_MEETING',
    moduleKey: 'pre-shift-meeting',
  },
  PinganCurtainWallTeamDispatch: {
    bizType: 'THREE_CHECK_CURTAIN_WALL_TEAM_DISPATCH',
    moduleKey: 'curtain-wall-team-dispatch',
  },
  PinganKeySites: {
    bizType: 'THREE_CHECK_KEY_SITES',
    moduleKey: 'key-sites',
  },
  PinganMidShiftInspection: {
    bizType: 'THREE_CHECK_MID_SHIFT_INSPECTION',
    moduleKey: 'mid-shift-inspection',
  },
  PinganPostShiftInspection: {
    bizType: 'THREE_CHECK_POST_SHIFT_INSPECTION',
    moduleKey: 'post-shift-inspection',
  },
  PinganPreShiftInspection: {
    bizType: 'THREE_CHECK_PRE_SHIFT_INSPECTION',
    moduleKey: 'pre-shift-inspection',
  },
  PinganPreShiftSafetyActivity: {
    bizType: 'THREE_CHECK_PRE_SHIFT_SAFETY_ACTIVITY',
    moduleKey: 'pre-shift-safety-activity',
  },
  PinganTeamDispatch: {
    bizType: 'THREE_CHECK_TEAM_DISPATCH',
    moduleKey: 'team-dispatch',
  },
};

const localStatusLabels: Record<ThreeCheckLocalStatus, string> = {
  ARCHIVED: '已归档',
  DRAFT: '待提交',
  OPENED: '已提交',
  WITHDRAWN: '已撤回',
};

const booleanEffectOptions: ThreeCheckFieldOption[] = [
  { label: '已生效', value: '已生效' },
  { label: '未生效', value: '未生效' },
];

const curtainWallDispatchTypeOptions: ThreeCheckFieldOption[] = [
  { label: '今日', value: '今日' },
  { label: '明日', value: '明日' },
];

export const preShiftSafetyActivityWorkContentOptions: ThreeCheckFieldOption[] =
  [
    { label: '吊装作业', value: '吊装作业' },
    { label: '吊篮作业', value: '吊篮作业' },
    { label: '高处作业', value: '高处作业' },
    { label: '动火作业', value: '动火作业' },
    { label: '临时用电', value: '临时用电' },
    { label: '机械作业', value: '机械作业' },
    { label: '脚手架作业', value: '脚手架作业' },
    { label: '维修（收尾工程）', value: '维修（收尾工程）' },
  ];

const meetingStatusOptions: ThreeCheckFieldOption[] = [
  { label: '已开会议', value: '已开会议' },
  { label: '待开会议', value: '待开会议' },
];

const inspectionStatusOptions: ThreeCheckFieldOption[] = [
  { label: '已检查', value: '已检查' },
  { label: '待检查', value: '待检查' },
];

const keySiteStatusOptions: ThreeCheckFieldOption[] = [
  { label: '已检查', value: '已检查' },
  { label: '待检查', value: '待检查' },
];

const localStatusLabelByRouteName: Record<
  string,
  Partial<Record<ThreeCheckLocalStatus, string>>
> = {
  PinganCurtainWallTeamDispatch: {
    DRAFT: '未生效',
    OPENED: '已生效',
    WITHDRAWN: '未生效',
  },
  PinganKeySites: {
    ARCHIVED: '已检查',
    DRAFT: '待检查',
    OPENED: '已检查',
    WITHDRAWN: '待检查',
  },
  PinganMidShiftInspection: {
    DRAFT: '待检查',
    OPENED: '已检查',
    WITHDRAWN: '待检查',
  },
  PinganPostShiftInspection: {
    DRAFT: '待检查',
    OPENED: '已检查',
    WITHDRAWN: '待检查',
  },
  PinganPreShiftInspection: {
    DRAFT: '待检查',
    OPENED: '已检查',
    WITHDRAWN: '待检查',
  },
  PinganPreShiftSafetyActivity: {
    DRAFT: '待开会议',
    OPENED: '已开会议',
    WITHDRAWN: '待开会议',
  },
  PinganTeamDispatch: {
    DRAFT: '未生效',
    OPENED: '已生效',
    WITHDRAWN: '未生效',
  },
  PreShiftMeeting: {
    DRAFT: '待开会议',
    OPENED: '已开会议',
    WITHDRAWN: '待开会议',
  },
};

const localStatusCodeByLabelByRouteName: Record<
  string,
  Record<string, ThreeCheckLocalStatus>
> = Object.fromEntries(
  Object.entries(localStatusLabelByRouteName).map(([routeName, labels]) => [
    routeName,
    Object.fromEntries(
      Object.entries(labels).map(([status, label]) => [
        label,
        status as ThreeCheckLocalStatus,
      ]),
    ),
  ]),
) as Record<string, Record<string, ThreeCheckLocalStatus>>;

const fieldOptionsByRouteName: Record<
  string,
  Record<string, ThreeCheckFieldOption[]>
> = {
  PinganCurtainWallTeamDispatch: {
    dispatchStatus: booleanEffectOptions,
    dispatchType: curtainWallDispatchTypeOptions,
  },
  PinganKeySites: {
    status: keySiteStatusOptions,
  },
  PinganMidShiftInspection: {
    status: inspectionStatusOptions,
  },
  PinganPostShiftInspection: {
    status: inspectionStatusOptions,
  },
  PinganPreShiftInspection: {
    status: inspectionStatusOptions,
  },
  PinganPreShiftSafetyActivity: {
    status: meetingStatusOptions,
    workContent: preShiftSafetyActivityWorkContentOptions,
  },
  PinganTeamDispatch: {
    dispatchStatus: booleanEffectOptions,
  },
  PreShiftMeeting: {
    status: meetingStatusOptions,
  },
};

export const preShiftMeetingBusinessColumnTitles =
  preShiftMeetingBusinessColumns.map((column) => column.title);

export const preShiftMeetingColumnTitles = [
  ...preShiftMeetingBusinessColumnTitles,
  operationColumn.title,
];

function appendOperationColumn(businessColumns: ThreeCheckTableColumn[]) {
  return [...businessColumns, operationColumn];
}

function titlesOf(columns: ThreeCheckTableColumn[]) {
  return columns.map((column) => column.title);
}

function hasPreShiftMeetingHeaders(businessColumns: ThreeCheckTableColumn[]) {
  const titles = titlesOf(businessColumns);
  return (
    titles.length === preShiftMeetingBusinessColumnTitles.length &&
    titles.every(
      (title, index) => title === preShiftMeetingBusinessColumnTitles[index],
    )
  );
}

function defaultLocalDate(now: Date) {
  return now.toISOString().slice(0, 10);
}

function localCapabilities(status: ThreeCheckLocalStatus) {
  return {
    canRemind: status === 'OPENED',
    canSubmit: status === 'DRAFT' || status === 'WITHDRAWN',
    canWithdraw: status === 'OPENED',
  };
}

function normalizeLocalStatusLabel(
  routeName: string,
  status: ThreeCheckLocalStatus,
  statusLabel?: string,
) {
  if (statusLabel) {
    return statusLabel;
  }
  const routeStatusLabel = localStatusLabelByRouteName[routeName]?.[status];
  if (routeStatusLabel) {
    return routeStatusLabel;
  }
  return localStatusLabels[status];
}

export function getThreeCheckFieldOptions(
  routeName: string,
  fieldName: string,
) {
  return fieldOptionsByRouteName[routeName]?.[fieldName] ?? [];
}

export function isThreeCheckMultiSelectField(
  routeName: string,
  fieldName: string,
) {
  return routeName === 'PinganPreShiftSafetyActivity' && fieldName === 'workContent';
}

export function normalizeThreeCheckStatusBoxLabel(label?: string) {
  if (!label) {
    return '';
  }
  const trimmedLabel = label.trim();
  if (trimmedLabel === '不生效') {
    return '未生效';
  }
  if (trimmedLabel === '生效') {
    return '已生效';
  }
  if (trimmedLabel === '带开会议') {
    return '待开会议';
  }
  return trimmedLabel;
}

export function getThreeCheckStatusBadgeTone(
  label?: string,
): ThreeCheckStatusBadgeTone {
  const normalizedLabel = normalizeThreeCheckStatusBoxLabel(label);
  if (['已检查', '已开会议', '已生效', '已验收'].includes(normalizedLabel)) {
    return 'green';
  }
  if (
    ['待检查', '待开会议', '待验收', '未生效'].includes(normalizedLabel)
  ) {
    return 'orange';
  }
  if (normalizedLabel === '待整改') {
    return 'red';
  }
  if (normalizedLabel === '已催办') {
    return 'blue';
  }
  return 'gray';
}

function normalizeThreeCheckFieldName(fieldName: unknown) {
  if (fieldName && typeof fieldName === 'object' && 'dataIndex' in fieldName) {
    return normalizeThreeCheckFieldName(
      (fieldName as { dataIndex?: unknown }).dataIndex,
    );
  }
  if (Array.isArray(fieldName)) {
    return String(fieldName.at(-1) ?? '');
  }
  return typeof fieldName === 'string' ? fieldName : '';
}

export function isThreeCheckStatusBoxField(fieldName: unknown) {
  const normalizedFieldName = normalizeThreeCheckFieldName(fieldName);
  const title =
    fieldName && typeof fieldName === 'object' && 'title' in fieldName
      ? String((fieldName as { title?: unknown }).title ?? '')
      : '';
  return (
    normalizedFieldName === 'status' ||
    normalizedFieldName === 'dispatchStatus' ||
    title === '状态' ||
    title === '派班状态'
  );
}

export function getThreeCheckAttachmentColumnKind(
  fieldName: unknown,
): ThreeCheckAttachmentColumnKind | undefined {
  const normalizedFieldName = normalizeThreeCheckFieldName(fieldName);
  const title =
    fieldName && typeof fieldName === 'object' && 'title' in fieldName
      ? String((fieldName as { title?: unknown }).title ?? '')
      : '';
  if (
    normalizedFieldName === 'imageCheck' ||
    normalizedFieldName === 'imageUpload' ||
    title === '图片打卡' ||
    title === '图片上传'
  ) {
    return 'IMAGE';
  }
  if (
    normalizedFieldName === 'videoCheck' ||
    normalizedFieldName === 'videoUpload' ||
    title === '视频打卡' ||
    title === '视频上传'
  ) {
    return 'VIDEO';
  }
  return undefined;
}

export function isThreeCheckAttachmentColumn(fieldName: unknown) {
  return Boolean(getThreeCheckAttachmentColumnKind(fieldName));
}

export function isThreeCheckAttachmentMissing(label?: string) {
  const normalizedLabel = (label ?? '').trim();
  return (
    !normalizedLabel ||
    normalizedLabel === '未上传' ||
    normalizedLabel === '待上传'
  );
}

export function validatePreShiftMeetingMediaChecks(record: {
  imageCheck?: string;
  imageUpload?: string;
  videoCheck?: string;
  videoUpload?: string;
}) {
  const imageMissing = isThreeCheckAttachmentMissing(
    record.imageCheck ?? record.imageUpload,
  );
  const videoMissing = isThreeCheckAttachmentMissing(
    record.videoCheck ?? record.videoUpload,
  );
  if (imageMissing && videoMissing) {
    return {
      message: '班前会提交前必须完成图片或视频打卡',
      valid: false,
    };
  }
  return { message: '', valid: true };
}

export function getThreeCheckStatusFilterOptions(routeName: string) {
  const labels = localStatusLabelByRouteName[routeName];
  if (!labels) {
    return [{ label: '全部', value: 'all' }];
  }

  const orderedStatuses: ThreeCheckLocalStatus[] =
    routeName === 'PinganKeySites'
      ? ['DRAFT', 'WITHDRAWN', 'OPENED', 'ARCHIVED']
      : ['OPENED', 'DRAFT'];

  const options = [
    { label: '全部', value: 'all' },
    ...orderedStatuses
      .map((status) => labels[status] && { label: labels[status], value: status })
      .filter(Boolean),
  ] as Array<{ label: string; value: string }>;
  if (
    [
      'PreShiftMeeting',
      'PinganPreShiftInspection',
      'PinganMidShiftInspection',
      'PinganPostShiftInspection',
    ].includes(routeName)
  ) {
    options.push({ label: '已逾期', value: 'overdue' });
  }
  return options;
}

function resolveLocalStatus(
  routeName: string,
  statusLabel?: string,
): ThreeCheckLocalStatus {
  if (!statusLabel) {
    return 'DRAFT';
  }
  const normalizedLabel = normalizeThreeCheckStatusBoxLabel(statusLabel);
  if (
    normalizedLabel === '待检查' ||
    normalizedLabel === '待开会议' ||
    normalizedLabel === '未生效'
  ) {
    return 'DRAFT';
  }
  if (normalizedLabel === '待整改') {
    return 'WITHDRAWN';
  }
  if (
    normalizedLabel === '待验收' ||
    normalizedLabel === '已检查' ||
    normalizedLabel === '已开会议' ||
    normalizedLabel === '已生效'
  ) {
    return 'OPENED';
  }
  if (normalizedLabel === '已验收') {
    return 'ARCHIVED';
  }
  return localStatusCodeByLabelByRouteName[routeName]?.[normalizedLabel] ?? 'DRAFT';
}

export function formatThreeCheckPayloadDisplayValue(value: unknown) {
  if (Array.isArray(value)) {
    return value.map((item) => String(item)).join('、');
  }
  return typeof value === 'string' ? value : '';
}

function displayValue(value?: string | string[]) {
  return formatThreeCheckPayloadDisplayValue(value);
}

function editableDetailValue(
  detail: ThreeCheckRecordEditableDetail,
  column: ThreeCheckTableColumn,
) {
  const payload = detail.payload ?? {};
  if (column.dataIndex === 'attendees') {
    return displayValue(
      (payload.attendeesText || payload.attendees) as string | string[],
    );
  }
  if (column.dataIndex === 'owner') {
    return payload.owner ?? detail.owner ?? '';
  }
  const directValues: Record<string, unknown> = {
    date: detail.date || detail.businessDate,
    imageCheck: detail.imageCheck,
    status: detail.statusLabel,
    videoCheck: detail.videoCheck,
  };
  return directValues[column.dataIndex] ?? payload[column.dataIndex] ?? '';
}

function isEditableDetailColumn(column: ThreeCheckTableColumn) {
  return !['actions', 'company', 'department', 'team'].includes(
    column.dataIndex,
  );
}

export function isThreeCheckDetailEditable(
  detail?: Pick<ThreeCheckRecordEditableDetail, 'canSubmit' | 'status'>,
) {
  return Boolean(
    detail &&
      (detail.canSubmit ||
        detail.status === 'DRAFT' ||
        detail.status === 'WITHDRAWN'),
  );
}

export function createThreeCheckDetailEditForm(
  detail: ThreeCheckRecordEditableDetail,
  columns: ThreeCheckTableColumn[],
) {
  return Object.fromEntries(
    columns
      .filter(isEditableDetailColumn)
      .map((column) => [column.dataIndex, editableDetailValue(detail, column)]),
  ) as Record<string, unknown>;
}

export function createThreeCheckRecordUpdatePayloadFromDetail(
  detail: ThreeCheckRecordEditableDetail,
  form: Record<string, unknown>,
) {
  const payload: Record<string, unknown> = { ...(detail.payload ?? {}) };
  for (const [key, value] of Object.entries(form)) {
    if (
      ['actions', 'businessDate', 'company', 'date', 'department', 'team'].includes(
        key,
      )
    ) {
      continue;
    }
    if (key === 'status') {
      payload.statusLabel = String(value || detail.statusLabel || '');
      continue;
    }
    payload[key] = value;
    if (key === 'attendees') {
      payload.attendeesText = displayValue(value as string | string[]);
    }
  }
  payload.statusLabel = String(
    form.status || payload.statusLabel || detail.statusLabel || '',
  );

  const updatePayload: {
    businessDate: string;
    companyId?: number | string;
    departmentId?: number | string;
    ownerUserId?: number;
    payload: Record<string, unknown>;
    rootDispatchRecordId?: number | string;
    status: string;
    teamId?: number | string;
    version?: number;
  } = {
    businessDate: String(
      form.date || form.businessDate || detail.businessDate || detail.date || '',
    ),
    companyId: detail.companyId,
    departmentId: detail.departmentId,
    ownerUserId: detail.ownerUserId,
    payload,
    status: String(payload.statusLabel || detail.status || ''),
    teamId: detail.teamId,
    version: detail.version,
  };
  if (detail.rootDispatchRecordId) {
    updatePayload.rootDispatchRecordId = detail.rootDispatchRecordId;
  }
  return updatePayload;
}

export function createPreShiftMeetingRecordPayload({
  attendeeNames,
  meetingContent,
  statusLabel = '待开会议',
}: PreShiftMeetingRecordPayloadInput) {
  return {
    attendees: attendeeNames,
    attendeesText: attendeeNames.join('、'),
    meetingContent,
    statusLabel,
  };
}

function teamCheckLineText(value: unknown) {
  if (Array.isArray(value)) {
    return value.map((item) => String(item ?? '')).join('、');
  }
  if (value === undefined || value === null) {
    return '';
  }
  return String(value);
}

function teamCheckLineBoolean(value: unknown) {
  if (typeof value === 'boolean') {
    return value;
  }
  return ['1', 'true', 'yes', '是'].includes(
    String(value ?? '').trim().toLowerCase(),
  );
}

function teamCheckUploadDescription(values: Record<string, unknown>) {
  const descriptions = [
    teamCheckLineBoolean(values.requireImage) ? '需上传图片' : '',
    teamCheckLineBoolean(values.requireVideo) ? '需上传视频' : '',
  ].filter(Boolean);
  return (
    teamCheckLineText(values.uploadDescription) ||
    teamCheckLineText(values.uploadRequirement) ||
    descriptions.join('、')
  );
}

export function createPreShiftMeetingSafetyConfirmLine(
  values: Record<string, unknown> = {},
): PreShiftMeetingSafetyConfirmLine {
  const confirmStatus = teamCheckLineText(values.confirmStatus).trim();
  return {
    confirmStatus: confirmStatus === '已确认' ? '已确认' : '',
    libraryItemId: teamCheckLineText(values.libraryItemId) || undefined,
    riskType: teamCheckLineText(values.riskType),
    safetyItem:
      teamCheckLineText(values.safetyItem) ||
      teamCheckLineText(values.checkItem),
    sortOrder:
      values.sortOrder === undefined || values.sortOrder === null
        ? undefined
        : Number(values.sortOrder),
  };
}

export function normalizePreShiftMeetingSafetyConfirmLines(items?: unknown) {
  if (!Array.isArray(items)) {
    return [];
  }
  return items
    .filter(
      (item): item is Record<string, unknown> =>
        Boolean(item) && typeof item === 'object',
    )
    .map((item) => createPreShiftMeetingSafetyConfirmLine(item))
    .filter((item) => item.safetyItem || item.riskType);
}

export function serializePreShiftMeetingSafetyConfirmPayload(
  payload: Record<string, unknown>,
  lines: PreShiftMeetingSafetyConfirmLine[],
) {
  return {
    ...payload,
    safetyConfirmItems: lines.map((line) => ({ ...line })),
  };
}

export function validatePreShiftMeetingSafetyConfirmResults(
  lines: PreShiftMeetingSafetyConfirmLine[],
) {
  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index]!;
    if (!String(line.riskType ?? '').trim()) {
      return {
        message: `请填写第 ${index + 1} 项风险`,
        valid: false,
      };
    }
    if (!String(line.safetyItem ?? '').trim()) {
      return {
        message: `请填写第 ${index + 1} 项安全注意事项`,
        valid: false,
      };
    }
    if (String(line.confirmStatus ?? '').trim() !== '已确认') {
      return {
        message: `请确认第 ${index + 1} 项安全确认事项`,
        valid: false,
      };
    }
  }
  return { message: '', valid: true };
}

export function getPreShiftMeetingSafetyConfirmColumns() {
  return [...preShiftMeetingSafetyConfirmColumns];
}

export function createTeamCheckInspectionLine(
  values: Record<string, unknown> = {},
): TeamCheckInspectionLine {
  return {
    checkItem: teamCheckLineText(values.checkItem),
    checkResult: normalizeTeamCheckInspectionResult(values.checkResult),
    followUpPlan:
      teamCheckLineText(values.followUpPlan) ||
      teamCheckLineText(values.defaultFollowUpPlan),
    libraryItemId: teamCheckLineText(values.libraryItemId) || undefined,
    lastRectificationAction: teamCheckLineText(
      values.lastRectificationAction,
    ),
    lastRectificationRemark: teamCheckLineText(
      values.lastRectificationRemark,
    ),
    rectificationDescription:
      teamCheckLineText(values.rectificationDescription) ||
      teamCheckLineText(values.defaultRectificationDescription),
    rectificationClosedAt: teamCheckLineText(values.rectificationClosedAt),
    rectificationOrderId: teamCheckLineText(values.rectificationOrderId),
    rectificationOrderNo: teamCheckLineText(values.rectificationOrderNo),
    rectificationStatus: teamCheckLineText(values.rectificationStatus),
    rectificationStatusLabel: teamCheckLineText(
      values.rectificationStatusLabel,
    ),
    requireImage: teamCheckLineBoolean(values.requireImage),
    requireVideo: teamCheckLineBoolean(values.requireVideo),
    riskType: teamCheckLineText(values.riskType),
    sortOrder:
      values.sortOrder === undefined || values.sortOrder === null
        ? undefined
        : Number(values.sortOrder),
    uploadDescription: teamCheckUploadDescription(values),
  };
}

export function normalizeTeamCheckInspectionLines(items?: unknown) {
  if (!Array.isArray(items)) {
    return [];
  }
  return items
    .filter(
      (item): item is Record<string, unknown> =>
        Boolean(item) && typeof item === 'object',
    )
    .map((item) => createTeamCheckInspectionLine(item))
    .filter((item) => item.checkItem || item.riskType);
}

export function serializeTeamCheckInspectionPayload(
  payload: Record<string, unknown>,
  lines: TeamCheckInspectionLine[],
) {
  return {
    ...payload,
    checkItems: lines.map((line) => ({ ...line })),
  };
}

export function validateTeamCheckInspectionResults(
  lines: TeamCheckInspectionLine[],
  routeName?: string,
) {
  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index]!;
    if (
      routeName !== 'PinganPostShiftInspection' &&
      !String(line.riskType ?? '').trim()
    ) {
      return {
        message: `请填写第 ${index + 1} 项风险`,
        valid: false,
      };
    }
    if (!String(line.checkItem ?? '').trim()) {
      return {
        message: `请填写第 ${index + 1} 项检查项`,
        valid: false,
      };
    }
    if (!String(line.checkResult ?? '').trim()) {
      return {
        message: `请填写第 ${index + 1} 项检查结果`,
        valid: false,
      };
    }
    if (!['无隐患', '有隐患'].includes(String(line.checkResult))) {
      return {
        message: `第 ${index + 1} 项检查结果只能选择无隐患或有隐患`,
        valid: false,
      };
    }
  }
  return { message: '', valid: true };
}

export function isTeamCheckInspectionRequiredColumn(
  column: ThreeCheckTableColumn,
  routeName: string,
) {
  return (
    column.dataIndex === 'checkItem' ||
    column.dataIndex === 'checkResult' ||
    (column.dataIndex === 'riskType' &&
      getTeamCheckInspectionStage(routeName) !== 'POST_SHIFT_INSPECTION')
  );
}

export function normalizeTeamCheckInspectionResult(value: unknown) {
  const text = teamCheckLineText(value).trim();
  return text === '有隐患' || text === '无隐患' ? text : '';
}

export function normalizeTeamCheckInspectionResultLines(
  lines: TeamCheckInspectionLine[],
) {
  return lines.map((line) => ({
    ...line,
    checkResult: normalizeTeamCheckInspectionResult(line.checkResult),
  }));
}

export function validateTeamCheckInspectionResultOptions(
  lines: TeamCheckInspectionLine[],
) {
  const invalidIndex = lines.findIndex(
    (line) =>
      line.checkResult &&
      !['无隐患', '有隐患'].includes(String(line.checkResult)),
  );
  if (invalidIndex >= 0) {
    return {
      message: `第 ${invalidIndex + 1} 项检查结果只能选择无隐患或有隐患`,
      valid: false,
    };
  }
  return { message: '', valid: true };
}

export function getTeamCheckInspectionStage(routeName: string) {
  return teamCheckInspectionRouteStages[routeName];
}

export function isTeamCheckInspectionRoute(routeName: string) {
  return Boolean(getTeamCheckInspectionStage(routeName));
}

export function getTeamCheckInspectionColumns(routeName: string) {
  return getTeamCheckInspectionStage(routeName) === 'POST_SHIFT_INSPECTION'
    ? teamCheckInspectionColumns.filter(
        (column) => column.dataIndex !== 'riskType',
      )
    : [...teamCheckInspectionColumns];
}

export function createThreeCheckModuleRuntime({
  businessColumns,
  routeName,
  routeTitle,
}: ThreeCheckModuleRuntimeInput): ThreeCheckModuleRuntime {
  const isPreShiftMeetingCompatible =
    hasPreShiftMeetingHeaders(businessColumns);
  const threeCheckRecordConfig = threeCheckRecordConfigByRouteName[routeName];

  if (threeCheckRecordConfig) {
    return {
      apiMode: 'THREE_CHECK_RECORD',
      apiReady: true,
      bizType: threeCheckRecordConfig.bizType,
      columns: appendOperationColumn(businessColumns),
      createMode: 'API',
      isPreShiftMeetingCompatible,
      moduleKey: threeCheckRecordConfig.moduleKey,
      routeName,
      title: routeTitle || '一班三查',
    };
  }

  return {
    apiMode: 'NONE',
    apiReady: false,
    bizType: 'UNSUPPORTED',
    columns: appendOperationColumn(businessColumns),
    createMode: 'DISABLED',
    isPreShiftMeetingCompatible,
    routeName,
    title: routeTitle || '一班三查',
  };
}

export function getThreeCheckTableDisplayColumns(
  routeName: string,
  columns: ThreeCheckTableColumn[],
  options: { roles?: readonly string[] } = {},
) {
  if (
    !oneShiftThreeCheckRouteNames.includes(
      routeName as OneShiftThreeCheckRouteName,
    )
  ) {
    return columns;
  }
  return columns.filter(
    (column) =>
      column.dataIndex !== 'company' &&
      (!isOneShiftTeamMemberRole(options.roles) ||
        column.dataIndex !== 'actions'),
  );
}

export function getThreeCheckDetailDisplayColumns(
  _routeName: string,
  columns: ThreeCheckTableColumn[],
) {
  return columns.filter((column) => column.dataIndex !== 'actions');
}

export function getThreeCheckDetailFieldGroups(
  _routeName: string,
  columns: ThreeCheckTableColumn[],
): ThreeCheckDetailFieldGroup[] {
  return [{ columns, key: 'default', title: '明细信息' }];
}

export function createLocalThreeCheckRow({
  columns,
  now = new Date(),
  routeName,
  values,
}: LocalThreeCheckRowInput) {
  const requestedStatusLabel = displayValue(values.status);
  const status = resolveLocalStatus(routeName, requestedStatusLabel);
  const row: Record<string, unknown> = {
    ...localCapabilities(status),
    id: `local-${routeName}-${now.getTime()}`,
    organizationPath: [],
    reminderCount: 0,
    routeName,
    sourceChannel: 'PC',
    status,
    version: 0,
  };

  for (const column of columns) {
    if (column.dataIndex === operationColumn.dataIndex) {
      continue;
    }

    if (column.dataIndex === 'status') {
      row.statusLabel = normalizeLocalStatusLabel(
        routeName,
        status,
        displayValue(values.status),
      );
      continue;
    }

    row[column.dataIndex] = displayValue(values[column.dataIndex]);
  }

  row.company = row.company || values.company || '';
  row.date =
    row.date || values.dispatchDate || values.createdAt || defaultLocalDate(now);
  row.department = row.department || values.department || '';
  row.imageCheck = row.imageCheck || values.imageUpload || '未上传';
  row.owner =
    row.owner ||
    values.responsiblePerson ||
    values.acceptancePerson ||
    values.inspectionDepartment ||
    '';
  row.team = row.team || values.team || '';
  row.videoCheck = row.videoCheck || values.videoUpload || '未上传';

  return row;
}

export function applyLocalThreeCheckAction(
  row: Record<string, any>,
  action: ThreeCheckLocalAction,
  now = new Date(),
): Record<string, any> {
  if (action === 'REMIND') {
    return {
      ...row,
      ...localCapabilities(row.status),
      lastRemindedAt: now.toISOString(),
      reminderCount: Number(row.reminderCount ?? 0) + 1,
      statusLabel: '已催办',
      version: Number(row.version ?? 0) + 1,
    };
  }

  const status: ThreeCheckLocalStatus =
    action === 'SUBMIT' ? 'OPENED' : 'WITHDRAWN';

  return {
    ...row,
    ...localCapabilities(status),
    status,
    statusLabel: normalizeLocalStatusLabel(row.routeName, status),
    version: Number(row.version ?? 0) + 1,
  };
}

export const preShiftMeetingColumns = appendOperationColumn(
  preShiftMeetingBusinessColumns,
);

export function resolvePreShiftMeetingRuntime(
  routeName: string,
  routeTitle: string,
): ThreeCheckModuleRuntime {
  const businessColumns = businessColumnsByRouteName[routeName];
  if (!businessColumns) {
    return {
      apiMode: 'NONE',
      apiReady: false,
      bizType: 'UNSUPPORTED',
      columns: appendOperationColumn(preShiftMeetingBusinessColumns),
      createMode: 'DISABLED',
      isPreShiftMeetingCompatible: false,
      routeName,
      title: routeTitle || '班前会',
    };
  }

  return createThreeCheckModuleRuntime({
    businessColumns,
    routeName,
    routeTitle,
  });
}

export const resolveOneShiftThreeCheckRuntime = resolvePreShiftMeetingRuntime;

export const getOneShiftThreeCheckTableDisplayColumns =
  getThreeCheckTableDisplayColumns;

export const getOneShiftThreeCheckDetailActions =
  getThreeCheckDetailActions;

export const getOneShiftThreeCheckDetailDisplayColumns =
  getThreeCheckDetailDisplayColumns;

export const getOneShiftThreeCheckDetailFieldGroups =
  getThreeCheckDetailFieldGroups;

export const createOneShiftThreeCheckDetailEditForm =
  createThreeCheckDetailEditForm;

export const createOneShiftThreeCheckRecordUpdatePayloadFromDetail =
  createThreeCheckRecordUpdatePayloadFromDetail;
