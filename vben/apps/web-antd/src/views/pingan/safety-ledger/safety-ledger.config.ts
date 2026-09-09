export type SafetyLedgerSourceKind =
  | 'DOCUMENT_LEDGER'
  | 'EMPTY'
  | 'POINTS_FLOW'
  | 'POINTS_INDIVIDUAL_RANKING'
  | 'POINTS_TEAM_RANKING'
  | 'SPECIAL_WORK'
  | 'SYSTEM_ACCOUNTS'
  | 'THREE_CHECK_RECORD';

export type SafetyLedgerFilterField =
  | 'checkUnit'
  | 'company'
  | 'dateEnd'
  | 'dateStart'
  | 'department'
  | 'inspectedUnit'
  | 'status'
  | 'team'
  | 'uploadedEnd'
  | 'uploadedStart'
  | 'workshop';

export interface SafetyLedgerColumnConfig {
  dataIndex: string;
  kind?: 'blank' | 'text';
  title: string;
  width: number;
}

export interface SafetyLedgerEntryConfig {
  columns: SafetyLedgerColumnConfig[];
  filters: SafetyLedgerFilterField[];
  headerKey: string;
  moduleKey?: string;
  path: string;
  routeName: string;
  sourceKind: SafetyLedgerSourceKind;
  title: string;
}

export interface SafetyLedgerSectionConfig {
  children: Array<SafetyLedgerEntryConfig | SafetyLedgerSectionConfig>;
  path: string;
  routeName: string;
  title: string;
}

const operationColumn: SafetyLedgerColumnConfig = {
  dataIndex: 'operation',
  kind: 'blank',
  title: '操作',
  width: 120,
};

const documentBasicColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'sequence', title: '序号*', width: 100 },
  { dataIndex: 'name', title: '名称*', width: 220 },
  { dataIndex: 'richText', title: '富文本', width: 180 },
  { dataIndex: 'file', title: '文件*', width: 160 },
  { dataIndex: 'enterprise', title: '所属企业', width: 180 },
  { dataIndex: 'uploadedAt', title: '上传时间', width: 170 },
  operationColumn,
];

const employeeRosterColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'name', title: '姓名*', width: 140 },
  { dataIndex: 'company', title: '所属公司*', width: 180 },
  { dataIndex: 'companyShortName', title: '企业简称', width: 140 },
  { dataIndex: 'department', title: '所属部门', width: 150 },
  { dataIndex: 'team', title: '所属班组', width: 150 },
  { dataIndex: 'receivedPoints', title: '领用积分', width: 110 },
  { dataIndex: 'employeeType', title: '员工类型*', width: 130 },
  { dataIndex: 'post', title: '岗位', width: 130 },
  { dataIndex: 'mobile', title: '手机号', width: 150 },
  { dataIndex: 'status', title: '状态*', width: 110 },
  { dataIndex: 'systemRole', title: '系统角色', width: 160 },
  operationColumn,
];

const documentOrgColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'company', title: '公司', width: 180 },
  { dataIndex: 'department', title: '部门', width: 150 },
  { dataIndex: 'team', title: '班组', width: 150 },
  { dataIndex: 'name', title: '名称*', width: 220 },
  { dataIndex: 'type', title: '类型', width: 140 },
  { dataIndex: 'file', title: '文件', width: 160 },
  { dataIndex: 'date', title: '日期', width: 130 },
  operationColumn,
];

const dispatchColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'expand', title: '展开', width: 90 },
  { dataIndex: 'enterprise', title: '所属企业', width: 180 },
  { dataIndex: 'department', title: '部门', width: 150 },
  { dataIndex: 'date', title: '日期', width: 130 },
  operationColumn,
];

const meetingColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'expand', title: '展开', width: 90 },
  { dataIndex: 'company', title: '公司', width: 180 },
  { dataIndex: 'department', title: '部门', width: 150 },
  { dataIndex: 'team', title: '班组', width: 150 },
  { dataIndex: 'owner', title: '负责人', width: 130 },
  { dataIndex: 'attendees', title: '参会人', width: 150 },
  { dataIndex: 'date', title: '开会日期', width: 130 },
  { dataIndex: 'imageCheck', title: '图片打卡', width: 120 },
  { dataIndex: 'videoCheck', title: '视频打卡', width: 120 },
  { dataIndex: 'status', title: '状态', width: 120 },
  operationColumn,
];

const safetyActivityColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'sequence', title: '序号', width: 90 },
  { dataIndex: 'enterprise', title: '所属企业', width: 180 },
  { dataIndex: 'department', title: '部门', width: 150 },
  { dataIndex: 'team', title: '班组', width: 150 },
  { dataIndex: 'attachment', title: '附件', width: 140 },
  { dataIndex: 'date', title: '日期', width: 130 },
  operationColumn,
];

const preventionControlColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'expand', title: '展开', width: 90 },
  { dataIndex: 'enterprise', title: '所属企业', width: 180 },
  { dataIndex: 'department', title: '部门', width: 150 },
  { dataIndex: 'team', title: '班组', width: 150 },
  { dataIndex: 'date', title: '日期', width: 130 },
  operationColumn,
];

const keySitesColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'expand', title: '展开', width: 90 },
  { dataIndex: 'enterprise', title: '所属企业', width: 180 },
  { dataIndex: 'siteType', title: '场所类型', width: 160 },
  { dataIndex: 'date', title: '日期', width: 130 },
  operationColumn,
];

const individualRankingColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'date', title: '日期', width: 130 },
  { dataIndex: 'company', title: '公司*', width: 180 },
  { dataIndex: 'department', title: '部门/车间', width: 160 },
  { dataIndex: 'name', title: '姓名*', width: 150 },
  { dataIndex: 'points', title: '积分', width: 110 },
  operationColumn,
];

const teamRankingColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'date', title: '日期', width: 130 },
  { dataIndex: 'company', title: '公司*', width: 180 },
  { dataIndex: 'department', title: '车间/部门*', width: 160 },
  { dataIndex: 'name', title: '名称*', width: 170 },
  { dataIndex: 'leader', title: '班组长', width: 130 },
  { dataIndex: 'points', title: '积分', width: 110 },
  operationColumn,
];

const pointsFlowColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'serialNo', title: '流水号', width: 150 },
  { dataIndex: 'createdAt', title: '创建时间', width: 170 },
  { dataIndex: 'company', title: '公司', width: 180 },
  { dataIndex: 'department', title: '部门', width: 150 },
  { dataIndex: 'team', title: '班组', width: 150 },
  { dataIndex: 'user', title: '用户', width: 130 },
  { dataIndex: 'pointsReason', title: '积分变动原因', width: 180 },
  { dataIndex: 'pointsChange', title: '积分变动', width: 120 },
  { dataIndex: 'pointsQuantity', title: '积分数量', width: 120 },
  { dataIndex: 'vendingMachine', title: '贩卖机', width: 150 },
  { dataIndex: 'goods', title: '货品', width: 150 },
];

const inspectionColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'expand', title: '展开', width: 90 },
  { dataIndex: 'checkUnit', title: '检查单位', width: 180 },
  { dataIndex: 'inspectedUnit', title: '受检单位', width: 180 },
  { dataIndex: 'inspectionType', title: '检查类型', width: 150 },
  { dataIndex: 'inspectionDate', title: '检查日期', width: 130 },
  operationColumn,
];

const hiddenDangerChecklistColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'sort', title: '排序', width: 90 },
  { dataIndex: 'sequence', title: '序号', width: 100 },
  { dataIndex: 'time', title: '时间', width: 150 },
  { dataIndex: 'inspectionType', title: '检查类型', width: 150 },
  { dataIndex: 'inspectors', title: '检查人员', width: 150 },
  { dataIndex: 'inspectorCount', title: '检查人数', width: 120 },
  { dataIndex: 'inspectionObject', title: '检查对象', width: 160 },
  { dataIndex: 'description', title: '描述', width: 180 },
  { dataIndex: 'hazardCount', title: '发现隐患问题数量（处）', width: 210 },
  { dataIndex: 'rectificationDeadline', title: '整改时限', width: 140 },
  { dataIndex: 'rectifiedCount', title: '已整改（处）', width: 140 },
  { dataIndex: 'acceptanceStatus', title: '验收情况', width: 140 },
  { dataIndex: 'acceptor', title: '验收人', width: 130 },
];

const specialWorkColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'expand', title: '展开', width: 90 },
  { dataIndex: 'enterprise', title: '所属企业', width: 180 },
  { dataIndex: 'operationType', title: '作业类型', width: 160 },
  { dataIndex: 'date', title: '日期', width: 160 },
  operationColumn,
];

const hazardRectificationColumns: SafetyLedgerColumnConfig[] = [
  { dataIndex: 'expand', title: '展开', width: 90 },
  { dataIndex: 'company', title: '公司', width: 180 },
  { dataIndex: 'department', title: '部门', width: 150 },
  { dataIndex: 'team', title: '班组', width: 150 },
  { dataIndex: 'inspector', title: '检查人', width: 130 },
  { dataIndex: 'responsiblePerson', title: '责任人', width: 130 },
  { dataIndex: 'createdAt', title: '创建时间', width: 170 },
  { dataIndex: 'rectificationCompletedAt', title: '整改完成时间', width: 170 },
  operationColumn,
];

const uploadCompanyFilters: SafetyLedgerFilterField[] = [
  'company',
  'uploadedStart',
  'uploadedEnd',
];

const orgFilters: SafetyLedgerFilterField[] = [
  'company',
  'department',
  'team',
];

const riskControlFilters: SafetyLedgerFilterField[] = [
  'company',
  'department',
  'team',
  'dateStart',
  'dateEnd',
];

const examFilters: SafetyLedgerFilterField[] = [
  'company',
  'department',
  'dateStart',
  'dateEnd',
];

const inspectionFilters: SafetyLedgerFilterField[] = [
  'checkUnit',
  'inspectedUnit',
  'dateStart',
  'dateEnd',
];

const hiddenDangerFilters: SafetyLedgerFilterField[] = [
  'company',
  'uploadedStart',
  'uploadedEnd',
  'status',
];

const specialWorkFilters: SafetyLedgerFilterField[] = [
  'company',
  'dateStart',
  'dateEnd',
];

const hazardRectificationFilters: SafetyLedgerFilterField[] = [
  'company',
  'workshop',
  'team',
  'dateStart',
  'dateEnd',
];

function entry(
  routeName: string,
  path: string,
  title: string,
  headerKey: string,
  columns: SafetyLedgerColumnConfig[],
  filters: SafetyLedgerFilterField[],
  sourceKind: SafetyLedgerSourceKind = 'DOCUMENT_LEDGER',
  moduleKey?: string,
): SafetyLedgerEntryConfig {
  return {
    columns,
    filters,
    headerKey,
    moduleKey,
    path,
    routeName,
    sourceKind,
    title,
  };
}

export const safetyLedgerSections: SafetyLedgerSectionConfig[] = [
  {
    path: 'rules-responsibility',
    routeName: 'PinganSafetyLedgerRulesResponsibility',
    title: '安全生产规章制度与责任体系',
    children: [
      entry('PinganSafetyLedgerSafetyRules', 'safety-rules', '安全生产规章制度', 'document-basic', documentBasicColumns, uploadCompanyFilters),
      entry('PinganSafetyLedgerOperatingProcedures', 'operating-procedures', '安全操作规程', 'document-basic', documentBasicColumns, uploadCompanyFilters),
      entry('PinganSafetyLedgerPostDuties', 'post-duties', '岗位安全生产职责', 'document-basic', documentBasicColumns, uploadCompanyFilters),
      entry('PinganSafetyLedgerFullResponsibility', 'full-responsibility', '全员责任制', 'document-basic', documentBasicColumns, uploadCompanyFilters),
    ],
  },
  {
    path: 'organization-certificates',
    routeName: 'PinganSafetyLedgerOrganizationCertificates',
    title: '安全生产组织机构与资格证',
    children: [
      entry('PinganSafetyLedgerEmployeeRoster', 'employee-roster', '员工花名册', 'employee-roster', employeeRosterColumns, orgFilters, 'SYSTEM_ACCOUNTS'),
    ],
  },
  {
    path: 'plans-summary',
    routeName: 'PinganSafetyLedgerPlansSummary',
    title: '安全计划与总结',
    children: [
      entry('PinganSafetyLedgerAnnualPlan', 'annual-plan', '安全生产工作年度计划', 'document-basic', documentBasicColumns, uploadCompanyFilters),
      entry('PinganSafetyLedgerSpecialActivityPlan', 'special-activity-plan', '重点工作和专项活动方案', 'document-basic', documentBasicColumns, uploadCompanyFilters),
    ],
  },
  {
    path: 'risk-control',
    routeName: 'PinganSafetyLedgerRiskControl',
    title: '安全风险分级管控',
    children: [
      entry('PinganSafetyLedgerRiskControlDocument', 'risk-control-document', '安全风险分级管控文档', 'document-org', documentOrgColumns, riskControlFilters),
      entry('PinganSafetyLedgerDispatchRecord', 'dispatch-record', '派班记录表', 'dispatch', dispatchColumns, riskControlFilters, 'THREE_CHECK_RECORD', 'team-dispatch'),
      entry('PinganSafetyLedgerCurtainWallDispatchRecord', 'curtain-wall-dispatch-record', '派班记录表', 'dispatch', dispatchColumns, riskControlFilters, 'THREE_CHECK_RECORD', 'curtain-wall-team-dispatch'),
      entry('PinganSafetyLedgerPreShiftMeetingRecord', 'pre-shift-meeting-record', '班前会议记录表', 'meeting', meetingColumns, riskControlFilters, 'THREE_CHECK_RECORD', 'pre-shift-meeting'),
      entry('PinganSafetyLedgerCurtainWallSafetyActivity', 'curtain-wall-safety-activity', '班前安全活动', 'safety-activity', safetyActivityColumns, riskControlFilters, 'THREE_CHECK_RECORD', 'pre-shift-safety-activity'),
      entry('PinganSafetyLedgerPreventionControl', 'prevention-control', '“双重预防机制”现场管控表', 'prevention-control', preventionControlColumns, riskControlFilters, 'THREE_CHECK_RECORD', 'pre-shift-inspection'),
      entry('PinganSafetyLedgerCurtainWallPreventionControl', 'curtain-wall-prevention-control', '“双重预防机制”现场管控表', 'prevention-control', preventionControlColumns, riskControlFilters, 'THREE_CHECK_RECORD', 'mid-shift-inspection'),
      entry('PinganSafetyLedgerKeySites', 'key-sites', '重点场所', 'key-sites', keySitesColumns, riskControlFilters, 'THREE_CHECK_RECORD', 'key-sites'),
    ],
  },
  {
    path: 'training',
    routeName: 'PinganSafetyLedgerTraining',
    title: '安全生产宣教培训',
    children: [
      {
        path: 'safety-exam',
        routeName: 'PinganSafetyLedgerSafetyExam',
        title: '安全考试',
        children: [
          entry('PinganSafetyLedgerIndividualPointsRanking', 'individual-points-ranking', '个人积分榜单台账', 'individual-ranking', individualRankingColumns, examFilters, 'POINTS_INDIVIDUAL_RANKING'),
          entry('PinganSafetyLedgerTeamPointsRanking', 'team-points-ranking', '班组积分榜单台账', 'team-ranking', teamRankingColumns, examFilters, 'POINTS_TEAM_RANKING'),
          entry('PinganSafetyLedgerIndividualPointsFlow', 'individual-points-flow', '个人积分流水台账', 'points-flow', pointsFlowColumns, examFilters, 'POINTS_FLOW', 'points-flow'),
          entry('PinganSafetyLedgerTeamPointsFlow', 'team-points-flow', '班组积分流水台账', 'points-flow', pointsFlowColumns, examFilters, 'POINTS_FLOW', 'points-flow'),
        ],
      },
    ],
  },
  {
    path: 'hazard-governance',
    routeName: 'PinganSafetyLedgerHazardGovernance',
    title: '安全生产检查与隐患排查治理',
    children: [
      entry('PinganSafetyLedgerReturnToWorkSixOnes', 'return-to-work-six-ones', '复工复产“六个一”', 'document-basic', documentBasicColumns, uploadCompanyFilters),
      entry('PinganSafetyLedgerHazardGovernanceDocument', 'hazard-governance-document', '安全生产检查与隐患排查治理文档', 'document-org', documentOrgColumns, riskControlFilters),
      {
        path: 'safety-inspection',
        routeName: 'PinganSafetyLedgerSafetyInspection',
        title: '安全生产检查',
        children: [
          entry('PinganSafetyLedgerCurtainWallRoutineInspection', 'curtain-wall-routine-inspection', '日周月检', 'inspection', inspectionColumns, inspectionFilters, 'THREE_CHECK_RECORD', 'curtain-wall-routine-check'),
          entry('PinganSafetyLedgerInspectionRectificationRecord', 'inspection-rectification-record', '安全检查及整改记录表', 'inspection', inspectionColumns, inspectionFilters, 'THREE_CHECK_RECORD', 'safety-check'),
          entry('PinganSafetyLedgerSafetyInspectionForm', 'safety-inspection-form', '安全检查表', 'inspection', inspectionColumns, inspectionFilters, 'THREE_CHECK_RECORD', 'safety-check'),
          entry('PinganSafetyLedgerHiddenDangerChecklist', 'hidden-danger-checklist', '生产安全事故隐患排查治理清单', 'hidden-danger-checklist', hiddenDangerChecklistColumns, hiddenDangerFilters, 'THREE_CHECK_RECORD', 'hazard-rectification'),
        ],
      },
      entry('PinganSafetyLedgerSpecialWorkLedger', 'special-work-ledger', '特殊作业台账', 'special-work', specialWorkColumns, specialWorkFilters, 'SPECIAL_WORK'),
      {
        path: 'hazard-rectification',
        routeName: 'PinganSafetyLedgerHazardRectificationLedger',
        title: '隐患整改台账',
        children: [
          entry('PinganSafetyLedgerHazardRectification', 'rectification', '隐患整改', 'hazard-rectification', hazardRectificationColumns, hazardRectificationFilters, 'THREE_CHECK_RECORD', 'hazard-rectification'),
        ],
      },
    ],
  },
];

export function isSafetyLedgerEntry(
  item: SafetyLedgerEntryConfig | SafetyLedgerSectionConfig,
): item is SafetyLedgerEntryConfig {
  return 'columns' in item;
}

function flattenSections(
  sections: Array<SafetyLedgerEntryConfig | SafetyLedgerSectionConfig>,
): SafetyLedgerEntryConfig[] {
  return sections.flatMap((section) =>
    isSafetyLedgerEntry(section) ? [section] : flattenSections(section.children),
  );
}

export const safetyLedgerEntries = flattenSections(safetyLedgerSections);

export function findSafetyLedgerEntryByRouteName(routeName: string) {
  return safetyLedgerEntries.find((entry) => entry.routeName === routeName);
}

export function filterSafetyLedgerNavigation(
  sections: SafetyLedgerSectionConfig[],
  keyword: string,
): SafetyLedgerSectionConfig[] {
  const trimmedKeyword = keyword.trim();
  if (!trimmedKeyword) {
    return sections;
  }

  function filterItems(
    items: Array<SafetyLedgerEntryConfig | SafetyLedgerSectionConfig>,
  ): Array<SafetyLedgerEntryConfig | SafetyLedgerSectionConfig> {
    return items.flatMap((item) => {
      if (item.title.includes(trimmedKeyword)) {
        return [item];
      }

      if (isSafetyLedgerEntry(item)) {
        return [];
      }

      const filteredChildren = filterItems(item.children);
      return filteredChildren.length > 0
        ? [{ ...item, children: filteredChildren }]
        : [];
    });
  }

  return filterItems(sections).filter(
    (item): item is SafetyLedgerSectionConfig => !isSafetyLedgerEntry(item),
  );
}

export function getSafetyLedgerAncestorRouteNames(
  sections: SafetyLedgerSectionConfig[],
  routeName: string,
): string[] {
  function walk(
    items: Array<SafetyLedgerEntryConfig | SafetyLedgerSectionConfig>,
    ancestors: string[],
  ): string[] | undefined {
    for (const item of items) {
      if (item.routeName === routeName) {
        return ancestors;
      }

      if (!isSafetyLedgerEntry(item)) {
        const result = walk(item.children, [...ancestors, item.routeName]);
        if (result) {
          return result;
        }
      }
    }
  }

  return walk(sections, []) ?? [];
}

export function getSafetyLedgerTitlePath(
  sections: SafetyLedgerSectionConfig[],
  routeName: string,
): string[] {
  function walk(
    items: Array<SafetyLedgerEntryConfig | SafetyLedgerSectionConfig>,
    titles: string[],
  ): string[] | undefined {
    for (const item of items) {
      const nextTitles = [...titles, item.title];
      if (item.routeName === routeName) {
        return nextTitles;
      }

      if (!isSafetyLedgerEntry(item)) {
        const result = walk(item.children, nextTitles);
        if (result) {
          return result;
        }
      }
    }
  }

  return walk(sections, []) ?? [];
}
