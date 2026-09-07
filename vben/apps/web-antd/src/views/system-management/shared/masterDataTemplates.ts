import type {
  SystemCrudColumn,
  SystemFieldConfig,
  SystemFieldOption,
} from './useSystemCrudPage';

export type SystemMasterDataModule =
  | 'company'
  | 'department'
  | 'personnel'
  | 'team';

export const MASTER_DATA_TEMPLATE_HEADERS = {
  company: [
    '编码',
    '排序',
    '名称',
    '简称',
    '描述',
    '状态',
    '地址',
    '公司类型',
    '一级',
    '二级',
    '三级',
    '四级',
    '安全管理员',
    '一级上报人',
    '二级上报人',
    '三级上报人',
    'L1上报时间',
    'L2上报时间',
    'L3上报时间',
    '附属文件1',
    '附属文件2',
    '公司介绍',
  ],
  department: [
    '编码',
    '名称',
    '所属公司',
    '类型',
    '子排序',
    '负责人',
    '描述',
    '状态',
    '顶级',
    '集团',
    '一级单位',
    '二级单位',
    '领导级别',
    '公司排序',
  ],
  personnel: [
    '编码',
    '姓名',
    '所属公司',
    '企业简称',
    '所属部门',
    '所属班组',
    '积分',
    '领用积分',
    '员工类型',
    '岗位',
    '手机号',
    '状态',
    '系统角色',
    '部门排序',
    '管理权重',
  ],
  team: [
    '名称',
    '公司',
    '集团',
    '一级单位',
    '二级单位',
    '车间',
    '班组作业',
    '状态',
    '班组长',
    '班组成员',
    '安全员',
    '积分',
    '编码',
    '活跃',
  ],
} as const;

export const MASTER_DATA_TEMPLATE_FILENAMES = {
  company: '公司管理_导入模板.xlsx',
  department: '部门管理_导入模板.xlsx',
  personnel: '人员管理_导入模板.xlsx',
  team: '班组管理_导入模板.xlsx',
} as const;

const TEAM_MANAGEMENT_FORM_HEADERS = [
  '公司',
  '部门',
  '车间',
  '编码',
  '名称',
  '集团',
  '一级单位',
  '二级单位',
  '班组作业',
  '状态',
  '班组长',
  '班组成员',
  '安全员',
  '提交日期',
  '申请人',
] as const;

const PERSONNEL_MANAGEMENT_FORM_HEADERS = [
  '公司',
  '部门',
  '班组',
  '编码',
  '姓名',
  '系统角色',
  '用户名',
  '手机号',
  '员工类型',
  '岗位',
  '上岗证有效期',
  '加入时间',
  '备注',
  '状态',
  '提交日期',
  '申请人',
] as const;

const personnelRoleNames = [
  '部门安全员',
  '企业后台',
  '部门经理',
  '幕墙工程管理中心',
  '后台管理',
  '安全员',
  '公司领导',
  '组员',
  '总集团安环部',
  '项目部',
  '班组长',
  '公司级安环部',
  '项目经理',
  '安全经理',
  '公司领导',
  '项目部',
  '班长',
  '安环部',
  '生产经理',
  '演示角色',
  '其他部门人员',
  '维修员',
  '工长',
] as const;

const personnelPositionNames = [
  '主操',
  '主管',
  '人事行政',
  '党群',
  '公司领导',
  '制单员',
  '副总经理',
  '司机',
  '安全员',
  '安全员且部门经理',
  '安全环保部副部长',
  '安全环保部管理人员',
  '安全环保部部长',
  '安全管理员',
  '安全经理',
  '工程管理中心人员',
  '工长',
  '总监',
  '总经理',
  '操作工',
  '普工',
  '班长',
  '生产经理',
  '电工',
  '经营',
  '统计员',
  '网管',
  '董办',
  '计调员',
  '设计员',
  '财务员',
  '质检员',
  '部长',
  '部门经理',
] as const;

const columnKeys = {
  company: {
    L1上报时间: 'reportL1Time',
    L2上报时间: 'reportL2Time',
    L3上报时间: 'reportL3Time',
    一级: 'level1Name',
    一级上报人: 'reporterL1Usernames',
    三级: 'level3Name',
    三级上报人: 'reporterL3Usernames',
    二级: 'level2Name',
    二级上报人: 'reporterL2Usernames',
    公司介绍: 'companyIntro',
    公司类型: 'companyType',
    四级: 'level4Name',
    地址: 'address',
    名称: 'name',
    安全管理员: 'safetyManagerUsername',
    排序: 'sortOrder',
    描述: 'description',
    状态: 'status',
    简称: 'shortName',
    编码: 'code',
    附属文件1: 'attachment1Url',
    附属文件2: 'attachment2Url',
  },
  department: {
    一级单位: 'level1Unit',
    二级单位: 'level2Unit',
    公司排序: 'companySortOrder',
    名称: 'name',
    子排序: 'childSortOrder',
    所属公司: 'companyName',
    描述: 'description',
    状态: 'status',
    类型: 'departmentType',
    编码: 'code',
    负责人: 'leaderUsername',
    顶级: 'topLevelName',
    领导级别: 'leaderLevel',
    集团: 'groupName',
  },
  personnel: {
    企业简称: 'companyShortName',
    上岗证有效期: 'certificateValidUntil',
    公司: 'companyName',
    加入时间: 'joinDate',
    备注: 'remark',
    员工类型: 'employeeType',
    申请人: 'applicantName',
    姓名: 'name',
    用户名: 'username',
    岗位: 'positionName',
    手机号: 'mobile',
    提交日期: 'submitDate',
    班组: 'teamOrgId',
    所属公司: 'companyName',
    所属班组: 'teamName',
    所属部门: 'departmentName',
    状态: 'status',
    系统角色: 'systemRoleCode',
    管理权重: 'managementWeight',
    积分: 'points',
    编码: 'employeeCode',
    部门: 'departmentOrgId',
    部门排序: 'departmentSortOrder',
    领用积分: 'receivedPoints',
  },
  team: {
    一级单位: 'level1Unit',
    二级单位: 'level2Unit',
    申请人: 'applicantName',
    公司: 'companyName',
    部门: 'workshopOrgId',
    名称: 'name',
    安全员: 'safetyOfficerUsername',
    活跃: 'active',
    状态: 'status',
    班组作业: 'workTypeName',
    班组成员: 'teamMembers',
    班组长: 'leaderUsername',
    提交日期: 'submitDate',
    积分: 'points',
    编码: 'code',
    车间: 'workshopName',
    集团: 'groupName',
  },
} as const;

const formKeys = {
  company: columnKeys.company,
  department: {
    ...columnKeys.department,
    所属公司: 'companyOrgId',
  },
  personnel: {
    ...columnKeys.personnel,
    所属公司: 'companyOrgId',
    所属班组: 'teamOrgId',
    所属部门: 'departmentOrgId',
  },
  team: {
    ...columnKeys.team,
    公司: 'companyOrgId',
    车间: 'workshopOrgId',
  },
} as const;

const teamManagementFormKeys = {
  ...columnKeys.team,
  公司: 'companyName',
  部门: 'workshopOrgId',
  车间: 'workshopName',
} as const;

const personnelManagementFormKeys = {
  ...columnKeys.personnel,
  公司: 'companyName',
  部门: 'departmentOrgId',
  班组: 'teamOrgId',
} as const;

const fixedLeftByModule: Partial<
  Record<SystemMasterDataModule, Set<string>>
> = {
  company: new Set(['编码', '名称']),
  department: new Set(['编码', '名称']),
  personnel: new Set(['编码', '姓名']),
  team: new Set(['名称']),
};
const hiddenTableHeaders: Partial<Record<SystemMasterDataModule, Set<string>>> = {
  company: new Set(['排序']),
};
const numberFields = new Set([
  'L1上报时间',
  'L2上报时间',
  'L3上报时间',
  '公司',
  '公司排序',
  '子排序',
  '所属公司',
  '所属班组',
  '所属部门',
  '排序',
  '积分',
  '管理权重',
  '车间',
  '部门排序',
  '领用积分',
]);
const dateFields = new Set(['上岗证有效期', '加入时间', '提交日期']);
const textareaFields = new Set(['公司介绍', '备注', '描述']);
const companyTypeOptions: SystemFieldOption[] = [
  { label: '集团', value: '集团' },
  { label: '分公司', value: '分公司' },
  { label: '子公司', value: '子公司' },
];

function textOptions(values: readonly string[]): SystemFieldOption[] {
  return [...new Set(values)].map((value) => ({ label: value, value }));
}

const personnelRoleOptions = textOptions(personnelRoleNames);
const personnelPositionOptions = textOptions(personnelPositionNames);
const employeeTypeOptions: SystemFieldOption[] = [
  { label: '正式', value: '正式' },
  { label: '外包', value: '外包' },
];

const columnWidthByHeader: Record<string, number> = {
  L1上报时间: 112,
  L2上报时间: 112,
  L3上报时间: 112,
  一级上报人: 140,
  三级上报人: 140,
  二级上报人: 140,
  公司介绍: 220,
  安全管理员: 132,
  所属公司: 180,
  所属班组: 150,
  所属部门: 150,
  描述: 220,
  班组成员: 180,
  系统角色: 140,
  附属文件1: 180,
  附属文件2: 180,
};

function splitDelimitedText(value: unknown) {
  return String(value ?? '')
    .split(/[,，;；]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function joinDelimitedText(value: unknown) {
  return Array.isArray(value) ? value.join(',') : value;
}

function formatBooleanText(value: unknown) {
  if (value === undefined || value === null || value === '') return undefined;
  return value === false || value === 'false' || value === 0 || value === '0'
    ? 'false'
    : 'true';
}

function normalizeBooleanText(value: unknown) {
  if (value === undefined || value === null || value === '') return undefined;
  return !(value === false || value === 'false' || value === 0 || value === '0');
}

function fieldOptions(
  header: string,
  statusOptions: SystemFieldOption[],
): Pick<SystemFieldConfig, 'component' | 'options'> {
  if (header === '状态') {
    return { component: 'select', options: statusOptions };
  }
  if (header === '活跃') {
    return {
      component: 'select',
      options: [
        { label: '是', value: 'true' },
        { label: '否', value: 'false' },
      ],
    };
  }
  if (header === '公司类型') {
    return { component: 'select', options: companyTypeOptions };
  }
  if (header === '员工类型') {
    return { component: 'select', options: employeeTypeOptions };
  }
  if (header === '系统角色') {
    return { component: 'select', options: personnelRoleOptions };
  }
  if (header === '岗位') {
    return { component: 'select', options: personnelPositionOptions };
  }
  if (textareaFields.has(header)) {
    return { component: 'textarea' };
  }
  return {};
}

function fieldPlaceholder(header: string) {
  if (header === '公司') return '填写公司组织ID；导入时可填公司编码或名称';
  if (header === '所属公司') return '填写公司组织ID；导入时可填公司编码或名称';
  if (header === '所属部门') return '填写部门组织ID；导入时可填部门编码或名称';
  if (header === '所属班组') return '填写班组组织ID；导入时可填班组编码或名称';
  if (header === '部门') return '请选择部门';
  if (header === '班组') return '请选择班组';
  if (header === '车间') return '填写车间组织ID；导入时可填部门编码或名称';
  if (header === '班组成员') return '多个成员用逗号分隔';
  return undefined;
}

function resolveTemplateKey(keys: Record<string, string>, header: string) {
  return keys[header] ?? header;
}

function isRequiredField(module: SystemMasterDataModule, header: string) {
  if (header === '状态') {
    return true;
  }
  if (module === 'company') {
    return ['编码', '名称', '公司类型'].includes(header);
  }
  return (
    header === '编码' ||
    header === '名称' ||
    header === '姓名' ||
    header === '公司' ||
    header === '所属公司' ||
    header === '部门' ||
    header === '提交日期' ||
    header === '申请人'
  );
}

function componentForHeader(
  header: string,
  module: SystemMasterDataModule,
  statusOptions: SystemFieldOption[],
) {
  if (module === 'team' && header === '部门') {
    return { component: 'select' as const };
  }
  if (module === 'personnel' && ['部门', '班组'].includes(header)) {
    return { component: 'select' as const };
  }
  return fieldOptions(header, statusOptions);
}

function readonlyTeamField(header: string) {
  return ['一级单位', '二级单位', '公司', '集团'].includes(header);
}

function createMasterDataField(
  module: SystemMasterDataModule,
  header: string,
  statusOptions: SystemFieldOption[],
  keyOverrides?: Record<string, string>,
): SystemFieldConfig {
  const keys = (keyOverrides ?? formKeys[module]) as Record<string, string>;
  const valueKey = resolveTemplateKey(keys, header);
  const isWorkshopNameField =
    module === 'team' && header === '车间' && valueKey === 'workshopName';
  return {
    ...componentForHeader(header, module, statusOptions),
    formatValue:
      header === '班组成员'
        ? joinDelimitedText
        : header === '活跃'
          ? formatBooleanText
          : undefined,
    label: header,
    normalizeValue:
      header === '班组成员'
        ? splitDelimitedText
        : header === '活跃'
          ? normalizeBooleanText
          : undefined,
    placeholder: isWorkshopNameField ? '请输入车间' : fieldPlaceholder(header),
    readonly: module === 'team' ? readonlyTeamField(header) : undefined,
    required: isRequiredField(module, header),
    type: dateFields.has(header)
      ? ('date' as const)
      : numberFields.has(header) && !isWorkshopNameField
        ? ('number' as const)
        : undefined,
    valueKey,
  };
}

export function getMasterDataColumns(
  module: SystemMasterDataModule,
): SystemCrudColumn[] {
  const keys = columnKeys[module] as Record<string, string>;
  const hiddenHeaders = hiddenTableHeaders[module] ?? new Set<string>();
  const fixedLeft = fixedLeftByModule[module] ?? new Set<string>();
  return MASTER_DATA_TEMPLATE_HEADERS[module]
    .filter((title) => !hiddenHeaders.has(title))
    .map((title) => ({
      dataIndex: resolveTemplateKey(keys, title),
      fixed: fixedLeft.has(title) ? ('left' as const) : undefined,
      title,
      width: columnWidthByHeader[title] ?? (title.length > 4 ? 128 : 112),
    }));
}

export function getMasterDataFields(
  module: SystemMasterDataModule,
  statusOptions: SystemFieldOption[],
): SystemFieldConfig[] {
  return MASTER_DATA_TEMPLATE_HEADERS[module].map((header) =>
    createMasterDataField(module, header, statusOptions),
  );
}

export function getTeamManagementFields(
  statusOptions: SystemFieldOption[],
): SystemFieldConfig[] {
  return TEAM_MANAGEMENT_FORM_HEADERS.map((header) =>
    createMasterDataField('team', header, statusOptions, teamManagementFormKeys),
  );
}

export function getPersonnelManagementFields(
  statusOptions: SystemFieldOption[],
): SystemFieldConfig[] {
  return PERSONNEL_MANAGEMENT_FORM_HEADERS.map((header) => {
    const field = createMasterDataField(
      'personnel',
      header,
      statusOptions,
      personnelManagementFormKeys,
    );
    return {
      ...field,
      readonly: header === '公司' ? true : field.readonly,
      required: ['编码', '姓名', '状态'].includes(header),
    };
  });
}
