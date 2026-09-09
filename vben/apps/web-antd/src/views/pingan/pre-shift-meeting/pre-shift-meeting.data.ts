export type MeetingStatus =
  | 'ACCEPTED'
  | 'APPROVED'
  | 'ARCHIVED'
  | 'DRAFT'
  | 'OPENED'
  | 'PENDING_ACCEPTANCE'
  | 'PENDING_APPROVAL'
  | 'PENDING_RECTIFICATION'
  | 'PENDING_REVIEW'
  | 'REJECTED'
  | 'RECTIFIED'
  | 'REVIEWED'
  | 'WITHDRAWN';
export type OrganizationId = number | string;

export interface OrganizationNode {
  children?: OrganizationNode[];
  companyType?: string;
  id?: OrganizationId;
  key: string;
  orgType?: string;
  title: string;
}

export interface MeetingRow {
  attendees: string;
  canRemind?: boolean;
  canSubmit?: boolean;
  canWithdraw?: boolean;
  company: string;
  date: string;
  department: string;
  id: string;
  imageCheck: string;
  imagePreviewUrl?: string;
  organizationPath: string[];
  owner: string;
  sourceChannel?: 'API_IMPORT' | 'PC' | 'WECHAT_MINI_PROGRAM';
  status: MeetingStatus;
  team: string;
  version?: number;
  videoCheck: string;
  videoPreviewUrl?: string;
}

export interface OrganizationSelectOption {
  label: string;
  orgType?: string;
  path: string[];
  value: OrganizationId;
}

export interface CreateOrganizationDefaults {
  companyId?: OrganizationId;
  departmentId?: OrganizationId;
  teamId?: OrganizationId;
}

export interface MeetingFilters {
  company?: string;
  companyId?: OrganizationId;
  dateRange?: [string, string];
  department?: string;
  departmentId?: OrganizationId;
  organizationKey?: string;
  status?: MeetingStatus | 'all' | 'overdue';
  team?: string;
  teamId?: OrganizationId;
}

export interface OrganizationFilterState {
  company: string;
  companyId?: OrganizationId;
  department: string;
  departmentId?: OrganizationId;
  team: string;
  teamId?: OrganizationId;
}

export const organizationTree: OrganizationNode[] = [
  {
    id: 1,
    key: 'group-guangsheng',
    orgType: 'GROUP',
    title: 'Demo控股集团',
    children: [
      {
        id: 2,
        key: 'group-mining',
        orgType: 'GROUP',
        title: 'Demo Safety Holdings',
        children: [
          {
            id: 3,
            key: 'company-muqian',
            orgType: 'COMPANY',
            title: 'Demo Works Company',
            children: [
              {
                id: 4,
                key: 'company-yuancheng',
                orgType: 'COMPANY',
                title: 'Demo Works Company',
              },
            ],
          },
          {
            id: 11,
            key: 'company-mining-invest',
            orgType: 'COMPANY',
            title: 'Demo矿投',
            children: [
              {
                id: 8,
                key: 'company-meizhou-jiasheng',
                orgType: 'COMPANY',
                title: 'Demo East Site',
              },
              {
                id: 12,
                key: 'company-heyuan-guyun',
                orgType: 'COMPANY',
                title: 'Demo North Site',
              },
              {
                id: 13,
                key: 'company-botai',
                orgType: 'COMPANY',
                title: 'Demo Industrial',
              },
              {
                id: 14,
                key: 'company-chaoan-liyuan',
                orgType: 'COMPANY',
                title: 'Demo Coastal Site',
              },
              {
                id: 15,
                key: 'company-guangdong-metallurgy',
                orgType: 'COMPANY',
                title: 'Demo Metallurgy Company',
              },
              {
                id: 16,
                key: 'company-heshangtian',
                orgType: 'COMPANY',
                title: 'Demo Harvest Site',
              },
            ],
          },
          {
            id: 17,
            key: 'company-south-storage',
            orgType: 'COMPANY',
            title: 'Demo Storage',
            children: [
              {
                id: 18,
                key: 'company-south-storage-transport',
                orgType: 'COMPANY',
                title: 'Demo Transport',
              },
              {
                id: 19,
                key: 'company-foshan',
                orgType: 'COMPANY',
                title: 'Demo South Depot',
              },
              {
                id: 20,
                key: 'company-changzhou-south-storage',
                orgType: 'COMPANY',
                title: 'Demo East Depot',
              },
            ],
          },
          {
            id: 21,
            key: 'company-guangsheng-metallurgy',
            orgType: 'COMPANY',
            title: 'Demo Metallurgy',
            children: [
              {
                id: 22,
                key: 'company-jingfa',
                orgType: 'COMPANY',
                title: 'Demo Development',
              },
            ],
          },
          {
            id: 23,
            key: 'company-yaoling',
            orgType: 'COMPANY',
            title: 'Demo Site',
            children: [
              {
                id: 24,
                key: 'company-ziyuan',
                orgType: 'COMPANY',
                title: 'Demo Company',
              },
            ],
          },
          {
            id: 25,
            key: 'company-yangchun',
            orgType: 'COMPANY',
            title: 'Demo Spring Site',
          },
          {
            id: 26,
            key: 'company-gold',
            orgType: 'COMPANY',
            title: 'Demo Gold Group',
          },
          {
            id: 27,
            key: 'school-metallurgy',
            orgType: 'SCHOOL',
            title: 'Demo Training School',
          },
          {
            id: 28,
            key: 'company-new-material',
            orgType: 'COMPANY',
            title: 'Demo Materials',
            children: [
              {
                id: 29,
                key: 'company-gaoli',
                orgType: 'COMPANY',
                title: '高力公司',
              },
            ],
          },
          {
            id: 30,
            key: 'company-guangsheng-storage',
            orgType: 'COMPANY',
            title: 'Demo Warehouse',
          },
          {
            id: 31,
            key: 'company-jinyue-muqian',
            orgType: 'COMPANY',
            title: 'Demo Works Subsidiary',
          },
          {
            id: 32,
            key: 'company-fanao',
            orgType: 'COMPANY',
            title: 'Demo Pacific Company',
          },
        ],
      },
    ],
  },
];

export const defaultExpandedOrganizationKeys = [
  ...collectExpandableOrganizationKeys(organizationTree),
];

export function collectExpandableOrganizationKeys(
  nodes: OrganizationNode[],
): string[] {
  return nodes.flatMap((node) => [
    ...(node.children?.length ? [node.key] : []),
    ...collectExpandableOrganizationKeys(node.children ?? []),
  ]);
}

export const meetingRows: MeetingRow[] = [
  {
    attendees: '5人',
    company: '演示公司',
    date: '2025-04-23',
    department: 'Demo Works Company',
    id: 'pre-shift-001',
    imageCheck: '现场照片',
    organizationPath: [
      'group-guangsheng',
      'group-mining',
      'company-muqian',
      'company-yuancheng',
    ],
    owner: 'HB_MONITOR',
    status: 'OPENED',
    team: 'Demo Works Company',
    videoCheck: '视频已传',
  },
  {
    attendees: '8人',
    company: 'Demo East Site',
    date: '2025-04-18',
    department: 'Demo East Site',
    id: 'pre-shift-002',
    imageCheck: '待补传',
    organizationPath: [
      'group-guangsheng',
      'group-mining',
      'company-mining-invest',
      'company-meizhou-jiasheng',
    ],
    owner: 'ZY_SUPERVISOR',
    status: 'DRAFT',
    team: 'Demo East Site',
    videoCheck: '未上传',
  },
  {
    attendees: '6人',
    company: 'Demo Spring Site',
    date: '2025-03-29',
    department: '采矿车间',
    id: 'pre-shift-003',
    imageCheck: '现场照片',
    organizationPath: [
      'group-guangsheng',
      'group-mining',
      'company-yangchun',
    ],
    owner: 'CK_MANAGER',
    status: 'ARCHIVED',
    team: '夜班',
    videoCheck: '视频已传',
  },
  {
    attendees: '7人',
    company: 'Demo Transport',
    date: '2025-04-09',
    department: '选矿车间',
    id: 'pre-shift-004',
    imageCheck: '现场照片',
    organizationPath: [
      'group-guangsheng',
      'group-mining',
      'company-south-storage',
      'company-south-storage-transport',
    ],
    owner: 'XK_MONITOR',
    status: 'OPENED',
    team: '中班',
    videoCheck: '视频已传',
  },
  {
    attendees: '9人',
    company: 'Demo Works Company',
    date: '2025-05-06',
    department: 'Demo Works Company',
    id: 'pre-shift-005',
    imageCheck: '现场照片',
    organizationPath: [
      'group-guangsheng',
      'group-mining',
      'company-muqian',
      'company-yuancheng',
    ],
    owner: 'MQ_FOREMAN',
    status: 'OPENED',
    team: 'Demo Works Company',
    videoCheck: '视频已传',
  },
  {
    attendees: '6人',
    company: 'Demo Works Company',
    date: '2025-04-26',
    department: 'Demo Works Company',
    id: 'pre-shift-006',
    imageCheck: '待补传',
    organizationPath: [
      'group-guangsheng',
      'group-mining',
      'company-muqian',
      'company-yuancheng',
    ],
    owner: 'MQ_SAFE',
    status: 'DRAFT',
    team: 'Demo Works Company',
    videoCheck: '未上传',
  },
  {
    attendees: '10人',
    company: 'Demo Works Company',
    date: '2025-03-18',
    department: 'Demo Works Company',
    id: 'pre-shift-007',
    imageCheck: '现场照片',
    organizationPath: [
      'group-guangsheng',
      'group-mining',
      'company-muqian',
      'company-yuancheng',
    ],
    owner: 'LM_MONITOR',
    status: 'ARCHIVED',
    team: 'Demo Works Company',
    videoCheck: '视频已传',
  },
  {
    attendees: '4人',
    company: 'Demo Works Company',
    date: '2025-05-12',
    department: 'Demo Works Company',
    id: 'pre-shift-008',
    imageCheck: '现场照片',
    organizationPath: [
      'group-guangsheng',
      'group-mining',
      'company-muqian',
      'company-yuancheng',
    ],
    owner: 'CL_MANAGER',
    status: 'DRAFT',
    team: 'Demo Works Company',
    videoCheck: '未上传',
  },
  {
    attendees: '8人',
    company: 'Demo Works Company',
    date: '2025-02-27',
    department: 'Demo Works Company',
    id: 'pre-shift-009',
    imageCheck: '现场照片',
    organizationPath: [
      'group-guangsheng',
      'group-mining',
      'company-muqian',
      'company-yuancheng',
    ],
    owner: 'QA_SUPERVISOR',
    status: 'OPENED',
    team: 'Demo Works Company',
    videoCheck: '视频已传',
  },
  {
    attendees: '7人',
    company: 'Demo Works Company',
    date: '2025-04-30',
    department: 'Demo Works Company',
    id: 'pre-shift-010',
    imageCheck: '待补传',
    organizationPath: [
      'group-guangsheng',
      'group-mining',
      'company-muqian',
      'company-yuancheng',
    ],
    owner: 'AQ_MONITOR',
    status: 'ARCHIVED',
    team: 'Demo Works Company',
    videoCheck: '未上传',
  },
];

function localizeDemoText(value: string): string {
  return value
    .replaceAll('Demo RegionDemo控股集团有限公司', '演示公司集团')
    .replaceAll('Demo控股集团', '演示安全集团')
    .replaceAll('Demo Safety Holdings', '演示安全集团')
    .replaceAll('Demo Works Company', '演示公司')
    .replaceAll('Demo Harbor班长', '班组负责人')
    .replaceAll(/Demo(?: [A-Za-z]+)+/g, '演示单位')
    .replaceAll('Demo矿投', '演示矿业');
}

function localizeOrganizationNodes(nodes: OrganizationNode[]): void {
  nodes.forEach((node) => {
    node.title = localizeDemoText(node.title);
    if (node.children) localizeOrganizationNodes(node.children);
  });
}

localizeOrganizationNodes(organizationTree);
meetingRows.forEach((row) => {
  row.company = localizeDemoText(row.company);
  row.department = localizeDemoText(row.department);
  row.owner = localizeDemoText(row.owner);
  row.team = localizeDemoText(row.team);
  row.attendees = localizeDemoText(row.attendees);
});

export type ThreeCheckTemplateRow = MeetingRow & Record<string, unknown>;

const yuanchengOrganizationPath = [
  'group-guangsheng',
  'group-mining',
  'company-muqian',
  'company-yuancheng',
];

function createTemplateRow(
  values: Partial<ThreeCheckTemplateRow> & {
    date: string;
    id: string;
    status?: MeetingStatus;
  },
): ThreeCheckTemplateRow {
  const { date, id, status: inputStatus, ...restValues } = values;
  const status = inputStatus ?? 'DRAFT';
  return {
    attendees: '',
    canRemind: status === 'OPENED',
    canSubmit: status === 'DRAFT' || status === 'WITHDRAWN',
    canWithdraw: status === 'OPENED',
    company: '演示公司',
    date,
    department: '组装',
    id,
    imageCheck: '未上传',
    organizationPath: yuanchengOrganizationPath,
    owner: 'Demo Harbor班长',
    reminderCount: 0,
    sourceChannel: 'PC',
    status,
    statusLabel: status === 'OPENED' ? '已开会议' : '待开会议',
    team: '组装1班',
    version: 0,
    videoCheck: '未上传',
    ...restValues,
  };
}

const threeCheckTemplateRowsByRouteName: Record<string, ThreeCheckTemplateRow[]> =
  {
    PinganCurtainWallTeamDispatch: [
      createTemplateRow({
        date: '2026-05-15',
        department: '门窗组装',
        dispatchDate: '2026-05-15',
        dispatchStatus: '生效',
        dispatchTime: '2026-05-15 07:35',
        dispatchType: '今日',
        id: 'curtain-wall-team-dispatch-001',
        managerCount: '3',
        team: '门窗组装1班',
        teamTask: '单元板装配',
      }),
      createTemplateRow({
        date: '2026-05-15',
        department: '机加',
        dispatchDate: '2026-05-15',
        dispatchStatus: '不生效',
        dispatchTime: '2026-05-15 07:50',
        dispatchType: '明日',
        id: 'curtain-wall-team-dispatch-002',
        managerCount: '2',
        status: 'OPENED',
        team: '机加1班',
        teamTask: '型材切割开料',
      }),
    ],
    PinganKeySites: [
      createTemplateRow({
        acceptancePerson: '安全员',
        date: '2026-05-15',
        id: 'key-sites-001',
        inspectionDepartment: '安全质量职卫部',
        responsibleDepartment: '组装',
        responsiblePerson: 'Demo Harbor班长',
        siteType: '吊装作业区',
        statusLabel: '待检查',
      }),
      createTemplateRow({
        acceptancePerson: '系统管理员',
        date: '2026-05-14',
        id: 'key-sites-002',
        inspectionDepartment: '生产部',
        responsibleDepartment: '资材仓库',
        responsiblePerson: '安全员',
        siteType: '临边防护区',
        status: 'ARCHIVED',
        statusLabel: '已验收',
      }),
    ],
    PinganMidShiftInspection: [
      createTemplateRow({
        date: '2026-05-15',
        id: 'mid-shift-inspection-001',
        imageCheck: '现场照片',
        owner: 'Demo Harbor班长',
        statusLabel: '待检查',
        videoCheck: '视频已传',
      }),
      createTemplateRow({
        date: '2026-05-14',
        department: '资材仓库',
        id: 'mid-shift-inspection-002',
        imageCheck: '待补传',
        owner: '安全员',
        status: 'OPENED',
        statusLabel: '已检查',
        team: '型材卸车备料班',
        videoCheck: '未上传',
      }),
    ],
    PinganPostShiftInspection: [
      createTemplateRow({
        date: '2026-05-15',
        handoverStatus: '待交班',
        id: 'post-shift-inspection-001',
        imageCheck: '现场照片',
        owner: 'Demo Harbor班长',
        statusLabel: '待检查',
      }),
      createTemplateRow({
        date: '2026-05-14',
        department: '门窗机加',
        handoverStatus: '已交班',
        id: 'post-shift-inspection-002',
        imageCheck: '现场照片',
        owner: '门窗班长',
        status: 'OPENED',
        statusLabel: '已检查',
        team: '门窗加工1班',
      }),
    ],
    PinganPreShiftInspection: [
      createTemplateRow({
        date: '2026-05-15',
        id: 'pre-shift-inspection-001',
        imageCheck: '未上传',
        owner: 'Demo Harbor班长',
        statusLabel: '待检查',
      }),
      createTemplateRow({
        date: '2026-05-14',
        department: '机加',
        id: 'pre-shift-inspection-002',
        imageCheck: '现场照片',
        owner: '安全员',
        status: 'OPENED',
        statusLabel: '已检查',
        team: '机加1班',
      }),
    ],
    PinganPreShiftSafetyActivity: [
      createTemplateRow({
        createdAt: '2026-05-15 07:45',
        date: '2026-05-15',
        equipmentInspection: '是',
        id: 'pre-shift-safety-activity-001',
        imageUpload: '未上传',
        laborTeamCount: '12',
        safetyActivityRecordUpload: '待上传',
        statusLabel: '待开会议',
        videoUpload: '未上传',
        workContent: '吊装作业、吊篮作业、高处作业、动火作业、临时用电、机械作业、脚手架作业',
      }),
      createTemplateRow({
        createdAt: '2026-05-14 07:40',
        date: '2026-05-14',
        department: '资材仓库',
        equipmentInspection: '是',
        id: 'pre-shift-safety-activity-002',
        imageUpload: '现场照片',
        laborTeamCount: '8',
        safetyActivityRecordUpload: '已上传',
        status: 'OPENED',
        statusLabel: '已开会议',
        team: '配件仓库班',
        videoUpload: '视频已传',
        workContent: '吊装作业、机械作业',
      }),
    ],
    PinganTeamDispatch: [
      createTemplateRow({
        date: '2026-05-15',
        dispatchDate: '2026-05-15',
        dispatchStatus: '生效',
        dispatchTime: '2026-05-15 07:30',
        dispatchType: '常规派班',
        id: 'team-dispatch-001',
        managerCount: '2',
        teamTask: '组装巡检',
      }),
      createTemplateRow({
        date: '2026-05-14',
        department: '资材仓库',
        dispatchDate: '2026-05-14',
        dispatchStatus: '不生效',
        dispatchTime: '2026-05-14 07:35',
        dispatchType: '常规派班',
        id: 'team-dispatch-002',
        managerCount: '1',
        status: 'OPENED',
        team: '型材卸车备料班',
        teamTask: '型材卸车备料',
      }),
    ],
  };

export function getThreeCheckTemplateRows(
  routeName: string,
): ThreeCheckTemplateRow[] {
  const rows: ThreeCheckTemplateRow[] =
    routeName === 'PreShiftMeeting'
      ? (meetingRows as ThreeCheckTemplateRow[])
      : (threeCheckTemplateRowsByRouteName[routeName] ?? []);

  return rows.map((row) => ({
    ...row,
    organizationPath: [...row.organizationPath],
  }));
}

export const statusOptions: Array<{
  label: string;
  value: MeetingFilters['status'];
}> = [
  { label: '全部', value: 'all' },
  { label: '待开会议', value: 'DRAFT' },
  { label: '已开会议', value: 'OPENED' },
  { label: '已撤回', value: 'WITHDRAWN' },
  { label: '已归档', value: 'ARCHIVED' },
  { label: '已逾期', value: 'overdue' },
];

export const statusMeta: Record<
  MeetingStatus,
  { color: string; label: string }
> = {
  ACCEPTED: { color: 'success', label: '已验收' },
  APPROVED: { color: 'success', label: '已审批' },
  ARCHIVED: { color: 'default', label: '已归档' },
  DRAFT: { color: 'warning', label: '待开会议' },
  OPENED: { color: 'processing', label: '已开会议' },
  PENDING_ACCEPTANCE: { color: 'warning', label: '待验收' },
  PENDING_APPROVAL: { color: 'warning', label: '待审批' },
  PENDING_RECTIFICATION: { color: 'error', label: '待整改' },
  PENDING_REVIEW: { color: 'warning', label: '待审核' },
  REJECTED: { color: 'error', label: '已驳回' },
  RECTIFIED: { color: 'success', label: '已整改' },
  REVIEWED: { color: 'success', label: '已审核' },
  WITHDRAWN: { color: 'error', label: '已撤回' },
};

export function flattenOrganizationOptions(
  nodes: OrganizationNode[],
  path: string[] = [],
): OrganizationSelectOption[] {
  return nodes.flatMap((node) => {
    const nextPath = [...path, node.title];
    const selfOption =
      isOrganizationId(node.id)
        ? [
            {
              label: node.title,
              orgType: node.orgType,
              path: nextPath,
              value: node.id,
            },
          ]
        : [];

    return [
      ...selfOption,
      ...flattenOrganizationOptions(node.children ?? [], nextPath),
    ];
  });
}

export function getCascadedOrganizationOptions(
  nodes: OrganizationNode[],
  parentId: OrganizationId | undefined,
  orgTypes: string[],
): OrganizationSelectOption[] {
  const normalizedTypes = orgTypes.map((item) => item.toUpperCase());
  const parent =
    isOrganizationId(parentId)
      ? findOrganizationNode(nodes, parentId)
      : undefined;
  const searchRoots = parent ? parent.children ?? [] : nodes;

  return flattenOrganizationOptions(searchRoots).filter(
    (item) =>
      item.orgType &&
      normalizedTypes.includes(item.orgType.toUpperCase()),
  );
}

export function getOrganizationIdByKey(
  nodes: OrganizationNode[],
  key: string,
): OrganizationId | undefined {
  for (const node of nodes) {
    if (node.key === key) {
      return node.id;
    }
    const childId = getOrganizationIdByKey(node.children ?? [], key);
    if (isOrganizationId(childId)) {
      return childId;
    }
  }
  return undefined;
}

export function createOrganizationFilterState(): OrganizationFilterState {
  return {
    company: '',
    department: '',
    team: '',
  };
}

export function createOrganizationFilterParams(
  state: OrganizationFilterState,
) {
  return {
    ...(state.company ? { company: state.company } : {}),
    ...(isOrganizationId(state.companyId)
      ? { companyId: state.companyId }
      : {}),
    ...(state.department ? { department: state.department } : {}),
    ...(isOrganizationId(state.departmentId)
      ? { departmentId: state.departmentId }
      : {}),
    ...(state.team ? { team: state.team } : {}),
    ...(isOrganizationId(state.teamId) ? { teamId: state.teamId } : {}),
  };
}

export function applyOrganizationFilterCompany(
  state: OrganizationFilterState,
  nodes: OrganizationNode[],
  companyId?: OrganizationId,
) {
  const option = findOrganizationSelectOption(nodes, companyId);
  state.companyId = option?.value;
  state.company = option?.label ?? '';
  state.departmentId = undefined;
  state.department = '';
  state.teamId = undefined;
  state.team = '';
}

export function applyOrganizationFilterDepartment(
  state: OrganizationFilterState,
  nodes: OrganizationNode[],
  departmentId?: OrganizationId,
) {
  const option = findOrganizationSelectOption(nodes, departmentId);
  state.departmentId = option?.value;
  state.department = option?.label ?? '';
  state.teamId = undefined;
  state.team = '';
}

export function applyOrganizationFilterTeam(
  state: OrganizationFilterState,
  nodes: OrganizationNode[],
  teamId?: OrganizationId,
) {
  const option = findOrganizationSelectOption(nodes, teamId);
  state.teamId = option?.value;
  state.team = option?.label ?? '';
}

export function applyOrganizationFilterFromDataMapSelection(
  state: OrganizationFilterState,
  dataMapNodes: OrganizationNode[],
  optionNodes: OrganizationNode[],
  key?: string,
) {
  const selectedDataMapNode =
    typeof key === 'string'
      ? findOrganizationNodeByKey(dataMapNodes, key)
      : undefined;
  const selectedMatch = isOrganizationId(selectedDataMapNode?.id)
    ? findOrganizationNodeWithAncestors(optionNodes, selectedDataMapNode.id)
    : undefined;

  if (!selectedMatch) {
    clearOrganizationFilter(state);
    return;
  }

  const { ancestors, node } = selectedMatch;
  const companyNode =
    isOrganizationNodeType(node, 'COMPANY')
      ? node
      : findNearestAncestorByType(ancestors, 'COMPANY');
  const departmentNode =
    isOrganizationNodeType(node, 'DEPARTMENT')
      ? node
      : isOrganizationNodeType(node, 'TEAM')
        ? findNearestAncestorByType(ancestors, 'DEPARTMENT')
        : undefined;
  const teamNode = isOrganizationNodeType(node, 'TEAM') ? node : undefined;

  if (!isOrganizationId(companyNode?.id)) {
    clearOrganizationFilter(state);
    return;
  }

  state.companyId = companyNode.id;
  state.company = companyNode.title;
  state.departmentId = isOrganizationId(departmentNode?.id)
    ? departmentNode.id
    : undefined;
  state.department = departmentNode?.title ?? '';
  state.teamId = isOrganizationId(teamNode?.id) ? teamNode.id : undefined;
  state.team = teamNode?.title ?? '';
}

export function resolveCreateOrganizationDefaults(
  state: OrganizationFilterState,
  nodes: OrganizationNode[],
  fallbackCompanyId?: OrganizationId,
): CreateOrganizationDefaults {
  const directCompany = findTypedOrganizationMatch(
    nodes,
    state.companyId,
    'COMPANY',
  );
  const directDepartment = findTypedOrganizationMatch(
    nodes,
    state.departmentId,
    'DEPARTMENT',
  );
  const directTeam = findTypedOrganizationMatch(nodes, state.teamId, 'TEAM');
  const fallbackMatch = isOrganizationId(fallbackCompanyId)
    ? findOrganizationNodeWithAncestors(nodes, fallbackCompanyId)
    : undefined;
  const fallbackCompanyNode =
    fallbackMatch && isOrganizationNodeType(fallbackMatch.node, 'COMPANY')
      ? fallbackMatch.node
      : findNearestAncestorByType(fallbackMatch?.ancestors ?? [], 'COMPANY');
  const firstCompanyId = firstOptionValue(
    getCascadedOrganizationOptions(nodes, undefined, ['COMPANY']),
  );
  const firstCompany = findTypedOrganizationMatch(
    nodes,
    firstCompanyId,
    'COMPANY',
  );

  const companyNode =
    directCompany?.node ??
    findNearestAncestorByType(directDepartment?.ancestors ?? [], 'COMPANY') ??
    findNearestAncestorByType(directTeam?.ancestors ?? [], 'COMPANY') ??
    fallbackCompanyNode ??
    firstCompany?.node;

  if (!isOrganizationId(companyNode?.id)) {
    return {};
  }

  const departmentNode =
    directDepartment &&
    organizationMatchHasAncestor(directDepartment, companyNode.id)
      ? directDepartment.node
      : directTeam &&
          organizationMatchHasAncestor(directTeam, companyNode.id)
        ? findNearestAncestorByType(directTeam.ancestors, 'DEPARTMENT')
        : undefined;
  const departmentId =
    departmentNode?.id ??
    firstOptionValue(
      getCascadedOrganizationOptions(nodes, companyNode.id, ['DEPARTMENT']),
    ) ??
    companyNode.id;
  const teamNode =
    directTeam &&
    organizationMatchHasAncestor(directTeam, companyNode.id) &&
    organizationMatchHasAncestor(directTeam, departmentId)
      ? directTeam.node
      : fallbackMatch &&
          isOrganizationNodeType(fallbackMatch.node, 'TEAM') &&
          organizationMatchHasAncestor(fallbackMatch, companyNode.id) &&
          organizationMatchHasAncestor(fallbackMatch, departmentId)
        ? fallbackMatch.node
        : undefined;
  const teamId =
    teamNode?.id ??
    firstOptionValue(
      getCascadedOrganizationOptions(nodes, departmentId, ['TEAM']),
    ) ??
    departmentId;

  return {
    companyId: companyNode.id,
    departmentId,
    teamId,
  };
}

function clearOrganizationFilter(state: OrganizationFilterState) {
  state.companyId = undefined;
  state.company = '';
  state.departmentId = undefined;
  state.department = '';
  state.teamId = undefined;
  state.team = '';
}

function findOrganizationNodeByKey(
  nodes: OrganizationNode[],
  key: string,
): OrganizationNode | undefined {
  for (const node of nodes) {
    if (node.key === key) {
      return node;
    }
    const child = findOrganizationNodeByKey(node.children ?? [], key);
    if (child) {
      return child;
    }
  }
  return undefined;
}

interface OrganizationNodeMatch {
  ancestors: OrganizationNode[];
  node: OrganizationNode;
}

function findOrganizationNodeWithAncestors(
  nodes: OrganizationNode[],
  id: OrganizationId,
  ancestors: OrganizationNode[] = [],
): OrganizationNodeMatch | undefined {
  for (const node of nodes) {
    if (isOrganizationId(node.id) && String(node.id) === String(id)) {
      return { ancestors, node };
    }
    const child = findOrganizationNodeWithAncestors(
      node.children ?? [],
      id,
      [...ancestors, node],
    );
    if (child) {
      return child;
    }
  }
  return undefined;
}

function findTypedOrganizationMatch(
  nodes: OrganizationNode[],
  id: OrganizationId | undefined,
  orgType: string,
): OrganizationNodeMatch | undefined {
  const match = isOrganizationId(id)
    ? findOrganizationNodeWithAncestors(nodes, id)
    : undefined;
  return match && isOrganizationNodeType(match.node, orgType)
    ? match
    : undefined;
}

function findNearestAncestorByType(
  ancestors: OrganizationNode[],
  orgType: string,
): OrganizationNode | undefined {
  return [...ancestors]
    .reverse()
    .find((ancestor) => isOrganizationNodeType(ancestor, orgType));
}

function isOrganizationNodeType(node: OrganizationNode | undefined, orgType: string) {
  return node?.orgType?.toUpperCase() === orgType;
}

function organizationMatchHasAncestor(
  match: OrganizationNodeMatch,
  ancestorId: OrganizationId,
) {
  return match.ancestors.some(
    (ancestor) =>
      isOrganizationId(ancestor.id) &&
      String(ancestor.id) === String(ancestorId),
  );
}

function firstOptionValue(options: OrganizationSelectOption[]) {
  return options[0]?.value;
}

function findOrganizationNode(
  nodes: OrganizationNode[],
  id: OrganizationId,
): OrganizationNode | undefined {
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

function findOrganizationSelectOption(
  nodes: OrganizationNode[],
  id?: OrganizationId,
): OrganizationSelectOption | undefined {
  if (!isOrganizationId(id)) {
    return undefined;
  }
  return flattenOrganizationOptions(nodes).find(
    (option) => String(option.value) === String(id),
  );
}

function isOrganizationId(value: unknown): value is OrganizationId {
  return typeof value === 'number' || typeof value === 'string';
}

export interface AttachmentPreviewItem {
  contentType?: string;
  fileKind: string;
  fileSize?: number;
  id: string;
  originalName?: string;
  storagePath?: string;
  url?: string;
}

export interface AttachmentPreviewDetail<
  TAttachment extends AttachmentPreviewItem = AttachmentPreviewItem,
> {
  attachments: TAttachment[];
  imageCheck: string;
  videoCheck: string;
}

function normalizeStorageDescriptor(value: string) {
  const parts = value.split('|');
  if (parts.length >= 3 && /^[A-Z_]+$/.test(parts[0] ?? '')) {
    return parts[2] ?? value;
  }
  return value;
}

export function resolveAttachmentPreviewUrl(value?: string) {
  if (!value) {
    return '';
  }
  const normalizedValue = normalizeStorageDescriptor(value);
  if (
    /^https?:\/\//i.test(normalizedValue) ||
    normalizedValue.startsWith('/api/attachments/') ||
    normalizedValue.startsWith('/uploads/')
  ) {
    return normalizedValue;
  }
  return `/uploads/${normalizedValue.replace(/^\/+/, '')}`;
}

export function upsertAttachmentPreview<
  TDetail extends AttachmentPreviewDetail,
>(detail: TDetail, attachment: AttachmentPreviewItem): TDetail {
  const previewUrl = resolveAttachmentPreviewUrl(
    attachment.url || attachment.storagePath,
  );
  const nextAttachment = {
    ...attachment,
    url: previewUrl,
  };
  const attachments = detail.attachments.some(
    (item) => item.id === attachment.id,
  )
    ? detail.attachments.map((item) =>
        item.id === attachment.id ? nextAttachment : item,
      )
    : [...detail.attachments, nextAttachment];

  return {
    ...detail,
    attachments,
    imageCheck:
      attachment.fileKind === 'IMAGE' ? '现场照片' : detail.imageCheck,
    videoCheck:
      attachment.fileKind === 'VIDEO' ? '视频已传' : detail.videoCheck,
  };
}

function includesKeyword(value: string, keyword?: string) {
  return !keyword || value.toLowerCase().includes(keyword.toLowerCase());
}

export function filterMeetingRows(
  rows: MeetingRow[],
  filters: MeetingFilters,
) {
  return rows.filter((row) => {
    const [dateStart, dateEnd] = filters.dateRange ?? [];
    const status = filters.status ?? 'all';

    return (
      includesKeyword(row.company, filters.company) &&
      includesKeyword(row.department, filters.department) &&
      includesKeyword(row.team, filters.team) &&
      (status === 'all' || row.status === status) &&
      (!dateStart || row.date >= dateStart) &&
      (!dateEnd || row.date <= dateEnd) &&
      (!filters.organizationKey ||
        row.organizationPath.includes(filters.organizationKey))
    );
  });
}

export function filterOrganizationTree(
  nodes: OrganizationNode[],
  keyword: string,
): OrganizationNode[] {
  const trimmedKeyword = keyword.trim().toLowerCase();
  if (!trimmedKeyword) {
    return nodes;
  }

  return nodes
    .map((node) => {
      const children = node.children
        ? filterOrganizationTree(node.children, keyword)
        : [];
      const selfMatched = node.title.toLowerCase().includes(trimmedKeyword);

      if (!selfMatched && children.length === 0) {
        return null;
      }

      return {
        ...node,
        children,
      };
    })
    .filter(Boolean) as OrganizationNode[];
}
