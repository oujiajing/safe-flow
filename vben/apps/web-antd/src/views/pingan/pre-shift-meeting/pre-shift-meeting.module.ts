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
export type ThreeCheckLocalStatus =
  | 'ACCEPTED'
  | 'ARCHIVED'
  | 'DRAFT'
  | 'OPENED'
  | 'PENDING_ACCEPTANCE'
  | 'PENDING_RECTIFICATION'
  | 'PENDING_REVIEW'
  | 'REJECTED'
  | 'RECTIFIED'
  | 'REVIEWED'
  | 'WITHDRAWN';
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

export function getThreeCheckDetailActions(routeName?: string) {
  const actions = isPointsFlowRoute(routeName ?? '') ? [] : threeCheckDetailActions;
  return [...actions];
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

export interface ThreeCheckSpecialFilterFields {
  company: boolean;
  dateRange: boolean;
  department: boolean;
  pointsReason: boolean;
  search: boolean;
  status: boolean;
  team: boolean;
}

export interface ThreeCheckOperationPolicy {
  batchDelete: boolean;
  delete: boolean;
  edit: boolean;
  remind: boolean;
  submit: boolean;
  view: boolean;
  withdraw: boolean;
}

export interface ThreeCheckDetailFieldGroup {
  columns: ThreeCheckTableColumn[];
  key: string;
  title: string;
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

export interface HazardInspectionLine {
  [key: string]: any;
  afterRectificationPhoto: string;
  aiEnabled: string;
  beforeRectificationPhoto: string;
  checkResult: string;
  hazardDescription: string;
  hazardLibrary: string;
  rectificationDeadline: string;
  rectificationMeasures: string;
  rectificationResponsiblePerson: string;
  sequence: string;
  status: string;
  validated: boolean;
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
  rectificationDescription: string;
  rectificationStatus: string;
  requireImage: boolean;
  requireVideo: boolean;
  riskType: string;
  sortOrder?: number;
  uploadDescription: string;
}

const hazardInspectionLineFieldKeys = [
  'aiEnabled',
  'checkResult',
  'hazardLibrary',
  'beforeRectificationPhoto',
  'hazardDescription',
  'rectificationMeasures',
  'rectificationResponsiblePerson',
  'rectificationDeadline',
  'afterRectificationPhoto',
  'status',
] as const;

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
  { dataIndex: 'rectificationStatus', title: '整改情况', width: 170 },
];

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

const riskHazardLibraryBusinessColumns: ThreeCheckTableColumn[] = [
  { dataIndex: 'code', fixed: 'left', title: '编码', width: 160 },
];

const safetyCheckBusinessColumns: ThreeCheckTableColumn[] = [
  { dataIndex: 'inspectionUnit', fixed: 'left', title: '检查单位', width: 180 },
  { dataIndex: 'inspectedUnit', title: '受检单位', width: 180 },
  { dataIndex: 'inspectedProject', title: '受检项目', width: 180 },
  { dataIndex: 'inspectionType', title: '检查类型', width: 140 },
  { dataIndex: 'inspectionDate', title: '检查日期', width: 130 },
  { dataIndex: 'status', title: '状态', width: 120 },
];

const safetyCheckDetailColumns: ThreeCheckTableColumn[] = [
  { dataIndex: 'inspectionUnit', title: '检查单位', width: 180 },
  { dataIndex: 'inspectedUnit', title: '受检单位', width: 180 },
  { dataIndex: 'inspectionType', title: '检查类型', width: 140 },
  { dataIndex: 'inspectionTypeManual', title: '检查类型（手录）', width: 160 },
  { dataIndex: 'inspectionDate', title: '检查日期', width: 130 },
  { dataIndex: 'inspectionTime', title: '检查时间', width: 130 },
  { dataIndex: 'createdBy', title: '创建人', width: 130 },
  { dataIndex: 'status', title: '状态', width: 120 },
  { dataIndex: 'inspectionMethod', title: '检查方式', width: 140 },
  { dataIndex: 'inspectors', title: '检查人员', width: 160 },
  { dataIndex: 'acceptancePersonnel', title: '验收人员', width: 160 },
  { dataIndex: 'inspectorsManual', title: '检查人员（手录）', width: 170 },
  { dataIndex: 'inspectedUnitPersonnel', title: '受检单位人员', width: 170 },
  {
    dataIndex: 'inspectedUnitPersonnelManual',
    title: '受检单位人员（手录）',
    width: 190,
  },
  { dataIndex: 'inspectionContent', title: '检查内容', width: 220 },
  { dataIndex: 'remarks', title: '备注', width: 180 },
  {
    dataIndex: 'rectificationAcceptancePassed',
    title: '整改验收是否通过',
    width: 180,
  },
  { dataIndex: 'acceptanceDate', title: '验收日期', width: 130 },
  { dataIndex: 'detailCount', title: '明细计数', width: 110 },
  { dataIndex: 'attachmentUpload', title: '附件', width: 170 },
];

const safetyCheckCreateColumnKeys = [
  'inspectionUnit',
  'inspectedUnit',
  'inspectedProject',
  'inspectionType',
  'inspectionDate',
  'createdBy',
  'inspectionMethod',
  'inspectors',
  'acceptancePersonnel',
  'inspectionTime',
  'inspectedUnitPersonnel',
  'inspectionContent',
  'status',
] as const;

const quickShotBusinessColumns: ThreeCheckTableColumn[] = [
  { dataIndex: 'company', fixed: 'left', title: '公司', width: 220 },
  { dataIndex: 'department', title: '部门', width: 140 },
  { dataIndex: 'team', title: '班组', width: 160 },
  { dataIndex: 'reporter', title: '上报人', width: 130 },
  { dataIndex: 'uploadTime', title: '上传时间', width: 160 },
  { dataIndex: 'status', title: '状态', width: 120 },
  { dataIndex: 'aiEnabled', title: '是否启用AI', width: 130 },
  { dataIndex: 'hazardImage', title: '隐患图片/视频', width: 160 },
  { dataIndex: 'hazardDescription', title: '隐患描述', width: 200 },
  { dataIndex: 'rectificationMeasures', title: '整改措施', width: 200 },
];

const curtainWallPenaltyBusinessColumns: ThreeCheckTableColumn[] = [
  { dataIndex: 'company', fixed: 'left', title: '公司', width: 220 },
  { dataIndex: 'responsibleUnit', title: '责任单位', width: 160 },
  { dataIndex: 'laborUnit', title: '劳务单位', width: 160 },
  { dataIndex: 'hazardDescription', title: '隐患描述', width: 200 },
  { dataIndex: 'imageOne', title: '图片一', width: 120 },
  { dataIndex: 'imageTwo', title: '图片二', width: 120 },
  { dataIndex: 'imageThree', title: '图片三', width: 120 },
  { dataIndex: 'penaltyConclusion', title: '处罚结论', width: 180 },
  { dataIndex: 'date', title: '日期', width: 120 },
  { dataIndex: 'approver', title: '批准人', width: 130 },
  { dataIndex: 'inspector', title: '检查人', width: 130 },
  { dataIndex: 'hasRead', title: '是否已读', width: 120 },
  { dataIndex: 'status', title: '状态', width: 120 },
];

const curtainWallRoutineCheckBusinessColumns: ThreeCheckTableColumn[] = [
  { dataIndex: 'company', fixed: 'left', title: '公司', width: 220 },
  { dataIndex: 'department', title: '部门', width: 140 },
  { dataIndex: 'patrolRecordType', title: '巡检记录表类型', width: 180 },
  { dataIndex: 'imageUpload', title: '图片上传', width: 120 },
  { dataIndex: 'videoUpload', title: '视频上传', width: 120 },
  { dataIndex: 'date', title: '日期', width: 120 },
  { dataIndex: 'status', title: '状态', width: 120 },
];

const examTaskBusinessColumns: ThreeCheckTableColumn[] = [
  { dataIndex: 'company', fixed: 'left', title: '公司', width: 220 },
  { dataIndex: 'department', title: '部门', width: 140 },
  { dataIndex: 'examPerson', title: '考试人员', width: 140 },
  { dataIndex: 'exam', title: '考试', width: 180 },
  { dataIndex: 'examScore', title: '考分', width: 100 },
  { dataIndex: 'examDate', title: '考试日期', width: 130 },
  { dataIndex: 'status', title: '状态', width: 120 },
];

const examArrangementBusinessColumns: ThreeCheckTableColumn[] = [
  { dataIndex: 'company', fixed: 'left', title: '公司', width: 220 },
  { dataIndex: 'department', title: '部门', width: 140 },
  { dataIndex: 'exam', title: '考试', width: 180 },
  { dataIndex: 'examDate', title: '考试日期', width: 130 },
  { dataIndex: 'status', title: '状态', width: 120 },
  { dataIndex: 'createdAt', title: '创建时间', width: 160 },
];

const pointsFlowBusinessColumns: ThreeCheckTableColumn[] = [
  { dataIndex: 'serialNo', fixed: 'left', title: '流水号', width: 180 },
  { dataIndex: 'createdAt', title: '创建时间', width: 160 },
  { dataIndex: 'company', title: '公司', width: 220 },
  { dataIndex: 'department', title: '部门', width: 140 },
  { dataIndex: 'team', title: '班组', width: 160 },
  { dataIndex: 'user', title: '用户', width: 130 },
  { dataIndex: 'pointsReason', title: '积分变动原因', width: 200 },
  { dataIndex: 'pointsChange', title: '积分变动', width: 130 },
  { dataIndex: 'pointsQuantity', title: '积分数量', width: 130 },
  { dataIndex: 'vendingMachine', title: '贩卖机', width: 140 },
  { dataIndex: 'goods', title: '货品', width: 140 },
];

const businessColumnsByRouteName: Record<string, ThreeCheckTableColumn[]> = {
  PinganCurtainWallTeamDispatch: teamDispatchBusinessColumns,
  PinganCurtainWallPenalty: curtainWallPenaltyBusinessColumns,
  PinganCurtainWallRoutineCheck: curtainWallRoutineCheckBusinessColumns,
  PinganExamResults: examArrangementBusinessColumns,
  PinganExamTasks: examTaskBusinessColumns,
  PinganKeySites: keySitesBusinessColumns,
  PinganMidShiftInspection: midShiftInspectionBusinessColumns,
  PinganPointsFlow: pointsFlowBusinessColumns,
  PinganPostShiftInspection: postShiftInspectionBusinessColumns,
  PinganPreShiftInspection: preShiftInspectionBusinessColumns,
  PinganPreShiftSafetyActivity: preShiftSafetyActivityBusinessColumns,
  PinganQuickShot: quickShotBusinessColumns,
  PinganRiskHazardLibrary: riskHazardLibraryBusinessColumns,
  PinganRiskLevelControl: riskHazardLibraryBusinessColumns,
  PinganSafetyCheck: safetyCheckBusinessColumns,
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
  PinganCurtainWallPenalty: {
    bizType: 'HAZARD_CURTAIN_WALL_PENALTY',
    moduleKey: 'curtain-wall-penalty',
  },
  PinganCurtainWallRoutineCheck: {
    bizType: 'HAZARD_CURTAIN_WALL_ROUTINE_CHECK',
    moduleKey: 'curtain-wall-routine-check',
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
  PinganPointsFlow: {
    bizType: 'SAFETY_POINTS_FLOW',
    moduleKey: 'points-flow',
  },
  PinganQuickShot: {
    bizType: 'HAZARD_QUICK_SHOT',
    moduleKey: 'quick-shot',
  },
  PinganSafetyCheck: {
    bizType: 'HAZARD_SAFETY_CHECK',
    moduleKey: 'safety-check',
  },
  PinganTeamDispatch: {
    bizType: 'THREE_CHECK_TEAM_DISPATCH',
    moduleKey: 'team-dispatch',
  },
};

const localStatusLabels: Record<ThreeCheckLocalStatus, string> = {
  ACCEPTED: '已验收',
  ARCHIVED: '已归档',
  DRAFT: '待提交',
  OPENED: '已提交',
  PENDING_ACCEPTANCE: '待验收',
  PENDING_RECTIFICATION: '待整改',
  PENDING_REVIEW: '待审核',
  REJECTED: '已驳回',
  RECTIFIED: '已整改',
  REVIEWED: '已审核',
  WITHDRAWN: '已撤回',
};

const booleanEffectOptions: ThreeCheckFieldOption[] = [
  { label: '已生效', value: '已生效' },
  { label: '未生效', value: '未生效' },
];

const yesNoOptions: ThreeCheckFieldOption[] = [
  { label: '是', value: '是' },
  { label: '否', value: '否' },
];

const pointsChangeOptions: ThreeCheckFieldOption[] = [
  { label: '加分', value: '加分' },
  { label: '扣分', value: '扣分' },
  { label: '兑换', value: '兑换' },
];

const pointsReasonOptions: ThreeCheckFieldOption[] = [
  { label: '学习-应急管理', value: '学习-应急管理' },
  { label: '学习-安全课程', value: '学习-安全课程' },
  { label: '学习-安全设施', value: '学习-安全设施' },
  { label: '学习-法律知识', value: '学习-法律知识' },
  { label: '学习-我要安全', value: '学习-我要安全' },
  { label: '学习-事故案例', value: '学习-事故案例' },
  { label: '学习-设备操作规程', value: '学习-设备操作规程' },
  { label: '考试通过', value: '考试通过' },
  { label: '考试不通过', value: '考试不通过' },
  { label: '随手拍被采纳', value: '随手拍被采纳' },
  { label: '积分兑换', value: '积分兑换' },
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
  PinganCurtainWallPenalty: {
    ARCHIVED: '已生效已读',
    DRAFT: '待批准',
    OPENED: '已生效（未读）',
  },
  PinganCurtainWallRoutineCheck: {
    DRAFT: '待检查',
    OPENED: '已检查',
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
  PinganQuickShot: {
    ACCEPTED: '已验收',
    ARCHIVED: '已验收',
    DRAFT: '待审核',
    OPENED: '已审核',
    PENDING_ACCEPTANCE: '待验收',
    PENDING_RECTIFICATION: '待整改',
    PENDING_REVIEW: '待审核',
    REJECTED: '已驳回',
    RECTIFIED: '已整改',
    REVIEWED: '已审核',
    WITHDRAWN: '待整改',
  },
  PinganSafetyCheck: {
    DRAFT: '待检查',
    OPENED: '已检查',
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
    Object.entries(labels).reduce(
      (result, [status, label]) => {
        if (label && !result[label]) {
          result[label] = status as ThreeCheckLocalStatus;
        }
        return result;
      },
      {} as Record<string, ThreeCheckLocalStatus>,
    ),
  ]),
) as Record<string, Record<string, ThreeCheckLocalStatus>>;

function statusOptionsForRoute(routeName: string): ThreeCheckFieldOption[] {
  const labels = localStatusLabelByRouteName[routeName];
  if (!labels) {
    return [];
  }
  return statusOrderForRoute(routeName)
    .map((status) => labels[status] && { label: labels[status], value: labels[status] })
    .filter(Boolean) as ThreeCheckFieldOption[];
}

const fieldOptionsByRouteName: Record<
  string,
  Record<string, ThreeCheckFieldOption[]>
> = {
  PinganCurtainWallTeamDispatch: {
    dispatchStatus: booleanEffectOptions,
    dispatchType: curtainWallDispatchTypeOptions,
  },
  PinganCurtainWallPenalty: {
    status: statusOptionsForRoute('PinganCurtainWallPenalty'),
  },
  PinganCurtainWallRoutineCheck: {
    status: statusOptionsForRoute('PinganCurtainWallRoutineCheck'),
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
  PinganPointsFlow: {
    pointsChange: pointsChangeOptions,
    pointsReason: pointsReasonOptions,
  },
  PinganPreShiftInspection: {
    status: inspectionStatusOptions,
  },
  PinganPreShiftSafetyActivity: {
    status: meetingStatusOptions,
    workContent: preShiftSafetyActivityWorkContentOptions,
  },
  PinganQuickShot: {
    aiEnabled: yesNoOptions,
    status: statusOptionsForRoute('PinganQuickShot').filter((option) =>
      ['待审核', '已审核'].includes(option.label),
    ),
  },
  PinganSafetyCheck: {
    status: statusOptionsForRoute('PinganSafetyCheck'),
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

export function getThreeCheckOrganizationSelectTypes(
  routeName: string,
  fieldName: string,
) {
  if (routeName === 'PinganSafetyCheck' && fieldName === 'inspectionUnit') {
    return ['GROUP', 'COMPANY'];
  }
  if (routeName === 'PinganSafetyCheck' && fieldName === 'inspectedUnit') {
    return ['COMPANY'];
  }
  return [];
}

const curtainWallOnlyRouteNames = new Set([
  'PinganCurtainWallPenalty',
  'PinganCurtainWallRoutineCheck',
]);

export function isThreeCheckDataMapEnabled(routeName: string) {
  return !curtainWallOnlyRouteNames.has(routeName);
}

export function getThreeCheckFixedCompanyName(routeName: string) {
  return curtainWallOnlyRouteNames.has(routeName) ? 'Demo Works Company' : undefined;
}

export function isPointsFlowRoute(routeName: string) {
  return routeName === 'PinganPointsFlow';
}

export function getThreeCheckSpecialFilterFields(
  routeName: string,
): ThreeCheckSpecialFilterFields {
  if (isPointsFlowRoute(routeName)) {
    return {
      company: true,
      dateRange: true,
      department: true,
      pointsReason: true,
      search: true,
      status: false,
      team: false,
    };
  }
  return {
    company: true,
    dateRange: true,
    department: true,
    pointsReason: false,
    search: true,
    status: true,
    team: true,
  };
}

export function getThreeCheckOperationPolicy(
  routeName: string,
): ThreeCheckOperationPolicy {
  if (isPointsFlowRoute(routeName)) {
    return {
      batchDelete: false,
      delete: false,
      edit: true,
      remind: false,
      submit: false,
      view: true,
      withdraw: false,
    };
  }
  return {
    batchDelete: true,
    delete: true,
    edit: true,
    remind: true,
    submit: true,
    view: true,
    withdraw: true,
  };
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
  if (
    ['已审核', '已检查', '已开会议', '已生效', '已验收', '已整改'].includes(
      normalizedLabel,
    )
  ) {
    return 'green';
  }
  if (normalizedLabel === '加分') {
    return 'green';
  }
  if (['已完成', '通过', '已生效已读'].includes(normalizedLabel)) {
    return 'green';
  }
  if (
    [
      '待检查',
      '待开会议',
      '待处理',
      '待审核',
      '待审批',
      '待批准',
      '待验收',
      '未生效',
    ].includes(normalizedLabel)
  ) {
    return 'orange';
  }
  if (
    normalizedLabel === '待整改' ||
    normalizedLabel === '已驳回' ||
    normalizedLabel === '不通过'
  ) {
    return 'red';
  }
  if (normalizedLabel === '扣分') {
    return 'red';
  }
  if (
    normalizedLabel === '兑换' ||
    normalizedLabel === '已催办' ||
    normalizedLabel === '已生效（未读）'
  ) {
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
    normalizedFieldName === 'hazardImage' ||
    normalizedFieldName === 'imageOne' ||
    normalizedFieldName === 'imageTwo' ||
    normalizedFieldName === 'imageThree' ||
    normalizedFieldName === 'imageUpload' ||
    normalizedFieldName === 'beforeRectificationPhoto' ||
    normalizedFieldName === 'afterRectificationPhoto' ||
    normalizedFieldName === 'attachmentUpload' ||
    title === '附件' ||
    title === '隐患图片' ||
    title === '隐患图片/视频' ||
    title === '整改前照片' ||
    title === '整改后照片' ||
    title === '图片一' ||
    title === '图片二' ||
    title === '图片三' ||
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

const createAttachmentFieldNamesByRouteName: Record<string, string[]> = {
  PinganCurtainWallPenalty: ['imageOne', 'imageTwo', 'imageThree'],
  PinganCurtainWallRoutineCheck: ['imageUpload', 'videoUpload'],
  PinganQuickShot: ['hazardImage'],
};

export function isThreeCheckCreateAttachmentField(
  routeName: string,
  fieldName: unknown,
) {
  const normalizedFieldName = normalizeThreeCheckFieldName(fieldName);
  return Boolean(
    normalizedFieldName &&
      createAttachmentFieldNamesByRouteName[routeName]?.includes(
        normalizedFieldName,
      ) &&
      getThreeCheckAttachmentColumnKind(normalizedFieldName),
  );
}

export function isThreeCheckAttachmentMissing(label?: string) {
  const normalizedLabel = (label ?? '').trim();
  return (
    !normalizedLabel ||
    normalizedLabel === '未上传' ||
    normalizedLabel === '待上传'
  );
}

export function shouldUseThreeCheckListPreviewFallback(
  fieldName: unknown,
  fileKind?: ThreeCheckAttachmentColumnKind,
) {
  const normalizedFieldName = normalizeThreeCheckFieldName(fieldName);
  if (fileKind === 'VIDEO') {
    return true;
  }
  if (fileKind !== 'IMAGE') {
    return false;
  }
  return !['imageTwo', 'imageThree'].includes(normalizedFieldName);
}

export function getThreeCheckStatusFilterOptions(routeName: string) {
  const labels = localStatusLabelByRouteName[routeName];
  if (!labels) {
    return [{ label: '全部', value: 'all' }];
  }

  return [
    { label: '全部', value: 'all' },
    ...statusOrderForRoute(routeName)
      .map((status) => labels[status] && { label: labels[status], value: status })
      .filter(Boolean),
  ] as Array<{ label: string; value: string }>;
}

function statusOrderForRoute(routeName: string): ThreeCheckLocalStatus[] {
  if (
    [
      'PinganCurtainWallPenalty',
      'PinganSafetyCheck',
    ].includes(routeName)
  ) {
    return ['DRAFT', 'WITHDRAWN', 'OPENED', 'ARCHIVED'];
  }
  if (['PinganQuickShot'].includes(routeName)) {
    return ['PENDING_REVIEW', 'REVIEWED'];
  }
  return ['OPENED', 'DRAFT'];
}

function resolveLocalStatus(
  routeName: string,
  statusLabel?: string,
): ThreeCheckLocalStatus {
  if (!statusLabel) {
    return 'DRAFT';
  }
  const normalizedLabel = normalizeThreeCheckStatusBoxLabel(statusLabel);
  if (routeName === 'PinganQuickShot') {
    if (normalizedLabel === '待审核' || normalizedLabel === '待审批') {
      return 'PENDING_REVIEW';
    }
    if (normalizedLabel === '已审核' || normalizedLabel === '已审批' || normalizedLabel === '通过') {
      return 'REVIEWED';
    }
    if (normalizedLabel === '待整改') {
      return 'PENDING_RECTIFICATION';
    }
    if (normalizedLabel === '已整改') {
      return 'RECTIFIED';
    }
    if (normalizedLabel === '待验收') {
      return 'PENDING_ACCEPTANCE';
    }
    if (normalizedLabel === '已验收') {
      return 'ACCEPTED';
    }
    if (normalizedLabel === '已驳回') {
      return 'REJECTED';
    }
  }
  const routeStatus =
    localStatusCodeByLabelByRouteName[routeName]?.[normalizedLabel];
  if (routeStatus) {
    return routeStatus;
  }
  if (
    normalizedLabel === '待检查' ||
    normalizedLabel === '待开会议' ||
    normalizedLabel === '未生效' ||
    normalizedLabel === '待审核' ||
    normalizedLabel === '待审批'
  ) {
    return routeName === 'PinganQuickShot' ? 'PENDING_REVIEW' : 'DRAFT';
  }
  if (normalizedLabel === '待整改') {
    return routeName === 'PinganQuickShot' ? 'PENDING_RECTIFICATION' : 'WITHDRAWN';
  }
  if (
    normalizedLabel === '待验收' ||
    normalizedLabel === '已检查' ||
    normalizedLabel === '已开会议' ||
    normalizedLabel === '已生效'
  ) {
    return routeName === 'PinganQuickShot' && normalizedLabel === '待验收'
      ? 'PENDING_ACCEPTANCE'
      : 'OPENED';
  }
  if (normalizedLabel === '已验收') {
    return routeName === 'PinganQuickShot' ? 'ACCEPTED' : 'ARCHIVED';
  }
  return 'DRAFT';
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
  const directValues: Record<string, unknown> = {
    date: detail.date || detail.businessDate,
    imageCheck: detail.imageCheck,
    owner: detail.owner,
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
  }
  payload.statusLabel = String(
    form.status || payload.statusLabel || detail.statusLabel || '',
  );

  return {
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
}

function hazardLineText(value: unknown) {
  if (Array.isArray(value)) {
    return value.map((item) => String(item ?? '')).join('、');
  }
  if (value === undefined || value === null) {
    return '';
  }
  return String(value);
}

function hazardLineBoolean(value: unknown) {
  if (typeof value === 'boolean') {
    return value;
  }
  return ['1', 'true', 'yes', '已校验', '是'].includes(
    String(value ?? '').trim().toLowerCase(),
  );
}

export function createHazardInspectionLine(
  existingLines: unknown[] = [],
  values: Record<string, unknown> = {},
): HazardInspectionLine {
  const sequence =
    hazardLineText(values.sequence) || String(existingLines.length + 1);
  return {
    afterRectificationPhoto: hazardLineText(values.afterRectificationPhoto),
    aiEnabled: hazardLineText(values.aiEnabled),
    beforeRectificationPhoto: hazardLineText(values.beforeRectificationPhoto),
    checkResult: hazardLineText(values.checkResult),
    hazardDescription: hazardLineText(values.hazardDescription),
    hazardLibrary: hazardLineText(values.hazardLibrary),
    rectificationDeadline: hazardLineText(values.rectificationDeadline),
    rectificationMeasures: hazardLineText(values.rectificationMeasures),
    rectificationResponsiblePerson: hazardLineText(
      values.rectificationResponsiblePerson,
    ),
    sequence,
    status: hazardLineText(values.status || values.statusLabel) || '待整改',
    validated: hazardLineBoolean(values.validated ?? values.isValidated),
  };
}

export function normalizeHazardInspectionLines(payload?: Record<string, unknown>) {
  const source = payload ?? {};
  const lines = Array.isArray(source.hazardLines)
    ? source.hazardLines.filter(
        (line): line is Record<string, unknown> =>
          Boolean(line) && typeof line === 'object',
      )
    : [];
  if (lines.length) {
    return lines.map((line, index) =>
      createHazardInspectionLine(lines.slice(0, index), {
        ...line,
        sequence: hazardLineText(line.sequence) || String(index + 1),
      }),
    );
  }
  return [
    createHazardInspectionLine([], {
      ...Object.fromEntries(
        hazardInspectionLineFieldKeys.map((key) => [key, source[key]]),
      ),
      status: source.status || source.statusLabel,
    }),
  ];
}

export function serializeHazardInspectionPayload(
  payload: Record<string, unknown>,
  lines: HazardInspectionLine[],
) {
  const nextPayload: Record<string, unknown> = {
    ...payload,
    hazardLines: lines,
  };
  const firstLine = lines[0];
  if (firstLine) {
    for (const key of hazardInspectionLineFieldKeys) {
      nextPayload[key] = firstLine[key];
    }
  }
  return nextPayload;
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

export function createTeamCheckInspectionLine(
  values: Record<string, unknown> = {},
): TeamCheckInspectionLine {
  return {
    checkItem: teamCheckLineText(values.checkItem),
    checkResult:
      teamCheckLineText(values.checkResult) ||
      teamCheckLineText(values.defaultCheckResult) ||
      '无隐患',
    followUpPlan:
      teamCheckLineText(values.followUpPlan) ||
      teamCheckLineText(values.defaultFollowUpPlan),
    libraryItemId: teamCheckLineText(values.libraryItemId) || undefined,
    rectificationDescription:
      teamCheckLineText(values.rectificationDescription) ||
      teamCheckLineText(values.defaultRectificationDescription),
    rectificationStatus: teamCheckLineText(values.rectificationStatus),
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
) {
  const missingIndex = lines.findIndex(
    (line) => !String(line.checkResult ?? '').trim(),
  );
  if (missingIndex >= 0) {
    return {
      message: `请填写第 ${missingIndex + 1} 项检查结果`,
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

export function filterHazardInspectionLines(
  lines: HazardInspectionLine[],
  keyword?: string,
) {
  const normalizedKeyword = String(keyword ?? '').trim().toLowerCase();
  if (!normalizedKeyword) {
    return lines;
  }
  return lines.filter((line) =>
    Object.values(line).some((value) =>
      hazardLineText(value).toLowerCase().includes(normalizedKeyword),
    ),
  );
}

export function getHazardInspectionViewStorageKey(routeName: string) {
  return `pingan:${routeName}:hazard-lines-visible-columns`;
}

function escapeCsvCell(value: unknown) {
  const text = hazardLineText(value);
  return /[",\r\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
}

export function exportHazardInspectionLinesCsv(
  lines: HazardInspectionLine[],
  columns: ThreeCheckTableColumn[],
) {
  const header = columns.map((column) => escapeCsvCell(column.title)).join(',');
  const body = lines
    .map((line) =>
      columns.map((column) => escapeCsvCell(line[column.dataIndex as keyof HazardInspectionLine])).join(','),
    )
    .join('\r\n');
  return body ? `${header}\r\n${body}` : header;
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
) {
  if (
    !oneShiftThreeCheckRouteNames.includes(
      routeName as OneShiftThreeCheckRouteName,
    )
  ) {
    return columns;
  }
  return columns.filter((column) => column.dataIndex !== 'company');
}

function pickColumnsByKeys(
  columns: ThreeCheckTableColumn[],
  keys: readonly string[],
) {
  return keys
    .map((dataIndex) => columns.find((column) => column.dataIndex === dataIndex))
    .filter(Boolean) as ThreeCheckTableColumn[];
}

export function getThreeCheckCreateDisplayColumns(
  routeName: string,
  columns: ThreeCheckTableColumn[],
) {
  if (routeName === 'PinganSafetyCheck') {
    return pickColumnsByKeys(
      [...columns, ...safetyCheckDetailColumns],
      safetyCheckCreateColumnKeys,
    );
  }
  return columns.filter(
    (column) =>
      column.dataIndex !== 'actions' &&
      !(isPointsFlowRoute(routeName) && column.dataIndex === 'serialNo'),
  );
}

export function getThreeCheckDetailDisplayColumns(
  routeName: string,
  columns: ThreeCheckTableColumn[],
) {
  if (routeName === 'PinganSafetyCheck') {
    return safetyCheckDetailColumns;
  }
  return columns.filter((column) => column.dataIndex !== 'actions');
}

function pickDetailColumns(
  columns: ThreeCheckTableColumn[],
  dataIndexes: string[],
) {
  return pickColumnsByKeys(columns, dataIndexes);
}

export function getThreeCheckDetailFieldGroups(
  routeName: string,
  columns: ThreeCheckTableColumn[],
): ThreeCheckDetailFieldGroup[] {
  if (routeName !== 'PinganSafetyCheck') {
    const detailColumns = [
      'PinganQuickShot',
      'PinganCurtainWallPenalty',
      'PinganCurtainWallRoutineCheck',
    ].includes(routeName)
      ? columns.filter((column) => !isThreeCheckAttachmentColumn(column))
      : columns;
    return [
      {
        columns: detailColumns,
        key: 'default',
        title: isPointsFlowRoute(routeName) ? '基本信息' : '明细信息',
      },
    ];
  }

  return [
    {
      columns: pickDetailColumns(columns, [
        'inspectionUnit',
        'createdBy',
        'inspectedUnitPersonnelManual',
        'inspectedUnit',
        'inspectionMethod',
        'inspectionContent',
        'inspectionType',
        'inspectors',
        'remarks',
        'inspectionTypeManual',
        'acceptancePersonnel',
        'inspectionDate',
        'inspectorsManual',
        'inspectionTime',
        'inspectedUnitPersonnel',
      ]),
      key: 'safetyCheckInfo',
      title: '检查信息',
    },
  ];
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

