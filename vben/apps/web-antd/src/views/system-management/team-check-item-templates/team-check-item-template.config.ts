import type {
  SystemCrudColumn,
  SystemFieldConfig,
  SystemFieldOption,
} from '../shared/useSystemCrudPage';

export const inspectionStageOptions: SystemFieldOption[] = [
  { label: '重点场所检查', value: 'KEY_SITES' },
  { label: '班前会安全确认', value: 'PRE_SHIFT_MEETING_CONFIRMATION' },
  { label: '班前检查', value: 'PRE_SHIFT_INSPECTION' },
  { label: '班中检查', value: 'MID_SHIFT_INSPECTION' },
  { label: '班后检查', value: 'POST_SHIFT_INSPECTION' },
];

export const templateStatusOptions: SystemFieldOption[] = [
  { label: '启用', value: 'ACTIVE' },
  { label: '草稿', value: 'DRAFT' },
  { label: '停用', value: 'INACTIVE' },
];

export const templateColumns: SystemCrudColumn[] = [
  { dataIndex: 'riskType', fixed: 'left', title: '风险类型', width: 150 },
  { dataIndex: 'checkItem', title: '检查项', width: 320 },
  { dataIndex: 'defaultCheckResult', title: '默认结果', width: 110 },
  { dataIndex: 'requireImageLabel', title: '图片要求', width: 100 },
  { dataIndex: 'requireVideoLabel', title: '视频要求', width: 100 },
  { dataIndex: 'sortOrder', title: '排序', width: 90 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 120 },
];

export const libraryColumns: SystemCrudColumn[] = [
  { dataIndex: 'riskType', fixed: 'left', title: '风险类型', width: 150 },
  { dataIndex: 'checkItem', title: '检查项', width: 320 },
  { dataIndex: 'defaultCheckResult', title: '默认结果', width: 110 },
  { dataIndex: 'requireImageLabel', title: '图片要求', width: 100 },
  { dataIndex: 'requireVideoLabel', title: '视频要求', width: 100 },
  { dataIndex: 'statusLabel', title: '状态', width: 90 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 100 },
];

export const safetyConfirmTemplateColumns: SystemCrudColumn[] = [
  { dataIndex: 'riskType', fixed: 'left', title: '风险', width: 150 },
  { dataIndex: 'checkItem', title: '安全注意事项', width: 420 },
  { dataIndex: 'sortOrder', title: '排序', width: 90 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 120 },
];

export const safetyConfirmLibraryColumns: SystemCrudColumn[] = [
  { dataIndex: 'riskType', fixed: 'left', title: '风险', width: 150 },
  { dataIndex: 'checkItem', title: '安全注意事项', width: 420 },
  { dataIndex: 'statusLabel', title: '状态', width: 90 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 100 },
];

export const libraryFields: SystemFieldConfig[] = [
  { label: '风险类型', required: true, valueKey: 'riskType' },
  {
    component: 'textarea',
    label: '检查项',
    required: true,
    valueKey: 'checkItem',
  },
  {
    component: 'select',
    label: '适用阶段',
    options: inspectionStageOptions,
    required: true,
    valueKey: 'applicableStage',
  },
  { defaultValue: '无隐患', label: '默认结果', valueKey: 'defaultCheckResult' },
  { label: '排序', type: 'number', valueKey: 'sortOrder' },
  {
    component: 'select',
    defaultValue: 'ACTIVE',
    label: '状态',
    options: templateStatusOptions,
    valueKey: 'status',
  },
];

export const preShiftMeetingLibraryFields: SystemFieldConfig[] = [
  { label: '风险', required: true, valueKey: 'riskType' },
  {
    component: 'textarea',
    label: '安全注意事项',
    required: true,
    valueKey: 'checkItem',
  },
  { label: '排序', type: 'number', valueKey: 'sortOrder' },
  {
    component: 'select',
    defaultValue: 'ACTIVE',
    label: '状态',
    options: templateStatusOptions,
    valueKey: 'status',
  },
];

export const templateItemFields: SystemFieldConfig[] = [
  { label: '风险类型', required: true, valueKey: 'riskType' },
  {
    component: 'textarea',
    label: '检查项',
    required: true,
    valueKey: 'checkItem',
  },
  { defaultValue: '无隐患', label: '默认结果', valueKey: 'defaultCheckResult' },
  {
    component: 'textarea',
    label: '整改描述',
    valueKey: 'defaultRectificationDescription',
  },
  {
    component: 'textarea',
    label: '跟进计划',
    valueKey: 'defaultFollowUpPlan',
  },
  { label: '排序', type: 'number', valueKey: 'sortOrder' },
];

export const preShiftMeetingTemplateItemFields: SystemFieldConfig[] = [
  { label: '风险', required: true, valueKey: 'riskType' },
  {
    component: 'textarea',
    label: '安全注意事项',
    required: true,
    valueKey: 'checkItem',
  },
  { label: '排序', type: 'number', valueKey: 'sortOrder' },
];

export const statusTone: Record<string, string> = {
  ACTIVE: 'green',
  DRAFT: 'blue',
  INACTIVE: 'red',
};
