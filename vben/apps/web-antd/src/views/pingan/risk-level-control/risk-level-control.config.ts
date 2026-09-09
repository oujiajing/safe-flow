import type { SystemCrudColumn } from '#/views/system-management/shared/useSystemCrudPage';

export const riskControlTableConfigModule = 'risk-level-control';

export const riskControlColumns: SystemCrudColumn[] = [
  { dataIndex: 'company', fixed: 'left', title: '公司', width: 220 },
  { dataIndex: 'riskPoint', fixed: 'left', title: '风险点', width: 180 },
  { dataIndex: 'dangerSource', title: '危险源', width: 180 },
  { dataIndex: 'riskInfluenceFactors', title: '风险影响因素', width: 220 },
  { dataIndex: 'accidentType', title: '事故类型', width: 140 },
  { dataIndex: 'likelihood', title: '事故发生的可能性（L）', width: 190 },
  {
    dataIndex: 'exposureFrequency',
    title: '人员暴露于危险环境中的频繁程度（E）',
    width: 260,
  },
  {
    dataIndex: 'consequence',
    title: '发生事故可能造成的后果（C）',
    width: 220,
  },
  { dataIndex: 'riskValue', title: '风险值（D）', width: 130 },
  { dataIndex: 'riskLevel', title: '风险等级', width: 130 },
  { dataIndex: 'engineeringMeasures', title: '关键技术与工程措施', width: 230 },
  {
    dataIndex: 'managementMeasures',
    title: '关键人员素养与系统管理措施',
    width: 260,
  },
  {
    dataIndex: 'emergencyMeasures',
    title: '关键个体防护与应急管理措施',
    width: 260,
  },
  { dataIndex: 'superiorResponsiblePerson', title: '上级单位责任人', width: 170 },
  { dataIndex: 'responsibleDepartment', title: '责任部门', width: 150 },
  { dataIndex: 'responsibleContact', title: '责任人/联系方式', width: 180 },
  { dataIndex: 'possibleHazard', title: '可能产生的事故隐患', width: 240 },
  { dataIndex: 'rectificationMeasures', title: '隐患整治措施', width: 220 },
];

export const riskControlSearchFilters = ['keyword'] as const;
