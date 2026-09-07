import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { describe, expect, it } from 'vitest';

import {
  canAcceptHazardRectificationOrder,
  canDeleteThreeCheckRecord,
  clampDataMapWidth,
  clampThreeCheckTableColumnWidth,
  canExportThreeCheckRecords,
  createDataMapCollapseState,
  createDataMapResizeState,
  createThreeCheckTableColumnResizeState,
  createThreeCheckTableBodyCellClass,
  createThreeCheckTableHeaderCellClass,
  createThreeCheckWorkbenchText,
  dataMapResizeConfig,
  dataMapTreeContinuousScrollConfig,
  threeCheckTableColumnResizeConfig,
  threeCheckTableScrollConfig,
  canCreateThreeCheckRecord,
  canOpenThreeCheckCreateDialog,
  canRectifyHazardRectificationOrder,
  canRemindThreeCheckRecord,
  canReviewQuickShotRecord,
  canSubmitThreeCheckRecord,
  canUploadThreeCheckAttachment,
  canViewThreeCheckRecord,
  canVoidHazardRectificationOrder,
  canWithdrawThreeCheckRecord,
} from './pre-shift-meeting.view-state';
import {
  applyLocalThreeCheckAction,
  createThreeCheckDetailEditForm,
  createHazardInspectionLine,
  createPreShiftMeetingRecordPayload,
  createThreeCheckRecordUpdatePayloadFromDetail,
  createLocalThreeCheckRow,
  createThreeCheckModuleRuntime,
  exportHazardInspectionLinesCsv,
  filterHazardInspectionLines,
  formatThreeCheckPayloadDisplayValue,
  getTeamCheckInspectionColumns,
  getTeamCheckInspectionStage,
  getHazardInspectionViewStorageKey,
  getThreeCheckAttachmentColumnKind,
  getThreeCheckCreateDisplayColumns,
  getThreeCheckDetailActions,
  getThreeCheckDetailDisplayColumns,
  getThreeCheckDetailFieldGroups,
  getThreeCheckTableDisplayColumns,
  getThreeCheckStatusBadgeTone,
  getThreeCheckFieldOptions,
  getThreeCheckFixedCompanyName,
  getThreeCheckOrganizationSelectTypes,
  getThreeCheckOperationPolicy,
  getThreeCheckSpecialFilterFields,
  getThreeCheckStatusFilterOptions,
  isPointsFlowRoute,
  isThreeCheckCreateAttachmentField,
  isThreeCheckDataMapEnabled,
  isThreeCheckDetailEditable,
  isThreeCheckAttachmentColumn,
  isThreeCheckAttachmentMissing,
  isThreeCheckStatusBoxField,
  normalizeHazardInspectionLines,
  normalizeTeamCheckInspectionLines,
  normalizeThreeCheckStatusBoxLabel,
  oneShiftThreeCheckRouteNames,
  preShiftMeetingBusinessColumnTitles,
  preShiftMeetingColumnTitles,
  resolvePreShiftMeetingRuntime,
  serializeHazardInspectionPayload,
  serializeTeamCheckInspectionPayload,
  validateTeamCheckInspectionResults,
  shouldUseThreeCheckListPreviewFallback,
} from './pre-shift-meeting.module';

function appSourcePath(path: string) {
  return join(
    process.cwd(),
    process.cwd().endsWith(join('apps', 'web-antd')) ? path : `apps/web-antd/${path}`,
  );
}

function sourceBetween(source: string, start: string, end: string) {
  const startIndex = source.indexOf(start);
  const endIndex = source.indexOf(end, startIndex);
  expect(startIndex).toBeGreaterThanOrEqual(0);
  expect(endIndex).toBeGreaterThan(startIndex);
  return source.slice(startIndex, endIndex);
}

describe('pre-shift-meeting page', () => {
  it('collapses and expands the data map panel state', () => {
    const state = createDataMapCollapseState();

    expect(state.isDataMapCollapsed.value).toBe(false);
    expect(state.dataMapToggleLabel.value).toBe('收起');

    state.toggleDataMap();

    expect(state.isDataMapCollapsed.value).toBe(true);
    expect(state.dataMapToggleLabel.value).toBe('展开');

    state.toggleDataMap();

    expect(state.isDataMapCollapsed.value).toBe(false);
    expect(state.dataMapToggleLabel.value).toBe('收起');
  });

  it('uses menu-like continuous scrolling for the data map tree', () => {
    expect(dataMapTreeContinuousScrollConfig).toMatchObject({
      itemHeight: 28,
      virtual: false,
    });
  });

  it('hydrates Pingan data maps from the company tree while keeping the full tree for forms', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );

    expect(page).toContain('canShowPinganDataMap');
    expect(page).toContain('useUserStore');
    expect(page).toContain('v-if="showDataMap"');
    expect(page).toContain('getPinganCompanyOrgTreeApi');
    expect(page).toContain('dataMapOrganizationNodes.value = companyTree');
    expect(page).toContain('formOrganizationNodes.value = fullTree');
    expect(page).toContain(
      'expandedOrganizationKeys.value = collectExpandableOrganizationKeys(companyTree)',
    );
  });

  it('keeps the three-check table vertically scrollable by mouse wheel', () => {
    expect(threeCheckTableScrollConfig).toMatchObject({
      x: 1280,
      y: 'calc(100vh - 420px)',
    });
  });

  it('resizes the data map width while respecting its minimum width', () => {
    expect(dataMapResizeConfig).toMatchObject({
      defaultWidth: 280,
      minWidth: 220,
    });
    expect(clampDataMapWidth(160)).toBe(220);
    expect(clampDataMapWidth(360)).toBe(360);

    const resizeState = createDataMapResizeState();

    resizeState.beginDataMapResize(300);
    resizeState.updateDataMapResize(390);

    expect(resizeState.isDataMapResizing.value).toBe(true);
    expect(resizeState.dataMapWidth.value).toBe(370);

    resizeState.updateDataMapResize(120);
    expect(resizeState.dataMapWidth.value).toBe(220);

    resizeState.endDataMapResize();
    expect(resizeState.isDataMapResizing.value).toBe(false);
  });

  it('resizes table columns while respecting the minimum width', () => {
    expect(threeCheckTableColumnResizeConfig).toMatchObject({
      minWidth: 96,
      triggerEdgeWidth: 10,
    });
    expect(clampThreeCheckTableColumnWidth(72)).toBe(96);
    expect(clampThreeCheckTableColumnWidth(150.4)).toBe(150);

    const resizeState = createThreeCheckTableColumnResizeState({
      company: 180,
    });

    resizeState.beginColumnResize('company', 300, 180);
    resizeState.updateColumnResize(345);

    expect(resizeState.isColumnResizing.value).toBe(true);
    expect(resizeState.columnWidths.company).toBe(225);

    resizeState.updateColumnResize(120);
    expect(resizeState.columnWidths.company).toBe(96);

    resizeState.endColumnResize();
    expect(resizeState.isColumnResizing.value).toBe(false);
  });

  it('centers every table header and body cell while keeping fixed column resize handles visible', () => {
    expect(
      createThreeCheckTableHeaderCellClass({
        dataIndex: 'company',
        fixed: 'left',
        title: '公司',
        width: 180,
      }),
    ).toMatchObject({
      'meeting-table__center-cell': true,
      'meeting-table__fixed-left-resizable-header': true,
      'meeting-table__resizable-header': true,
    });

    expect(
      createThreeCheckTableBodyCellClass({
        dataIndex: 'department',
        title: '车间',
        width: 140,
      }),
    ).toMatchObject({
      'meeting-table__center-cell': true,
    });
  });

  it('derives reusable page text from the active three-check module title', () => {
    expect(createThreeCheckWorkbenchText().title).toBe('一班三查');
    expect(createThreeCheckWorkbenchText('班组派班')).toEqual({
      addButtonText: '新增班组派班',
      createSuccessText: '班组派班已创建',
      createTitle: '新增班组派班',
      dataLoadErrorText: '班组派班数据加载失败',
      detailTitle: '班组派班详情',
      emptyText: '暂无班组派班数据',
      formContentPlaceholder: '填写班组派班内容',
      recordButtonText: '班组派班记录表',
      submitSuccessText: '班组派班已提交',
      title: '班组派班',
      withdrawSuccessText: '班组派班已撤回',
    });

    expect(
      resolvePreShiftMeetingRuntime('PreShiftMeeting', '班前会'),
    ).toMatchObject({
      title: '班前会',
    });
  });

  it('connects every one-shift-three-check module including pre-shift meeting to the generic api', () => {
    expect(
      resolvePreShiftMeetingRuntime('PreShiftMeeting', '一班三查'),
    ).toMatchObject({
      apiMode: 'THREE_CHECK_RECORD',
      apiReady: true,
      bizType: 'PRE_SHIFT_MEETING',
      createMode: 'API',
      isPreShiftMeetingCompatible: true,
      moduleKey: 'pre-shift-meeting',
      routeName: 'PreShiftMeeting',
      title: '一班三查',
    });

    expect(
      resolvePreShiftMeetingRuntime('PinganPreShiftInspection', '班前检查'),
    ).toMatchObject({
      apiMode: 'THREE_CHECK_RECORD',
      apiReady: true,
      bizType: 'THREE_CHECK_PRE_SHIFT_INSPECTION',
      createMode: 'API',
      isPreShiftMeetingCompatible: false,
      moduleKey: 'pre-shift-inspection',
      routeName: 'PinganPreShiftInspection',
      title: '班前检查',
    });
  });

  it('declares the eight one-shift-three-check modules handled by the workbench', () => {
    expect(oneShiftThreeCheckRouteNames).toEqual([
      'PinganTeamDispatch',
      'PinganCurtainWallTeamDispatch',
      'PreShiftMeeting',
      'PinganPreShiftSafetyActivity',
      'PinganPreShiftInspection',
      'PinganMidShiftInspection',
      'PinganPostShiftInspection',
      'PinganKeySites',
    ]);
  });

  it('resolves one-shift-three-check table headers from provided templates', () => {
    expect(preShiftMeetingBusinessColumnTitles).toEqual([
      '公司',
      '部门',
      '班组',
      '负责人',
      '参会人',
      '开会日期',
      '图片打卡',
      '视频打卡',
      '状态',
    ]);

    expect(preShiftMeetingColumnTitles).toEqual([
      ...preShiftMeetingBusinessColumnTitles,
      '操作',
    ]);

    expect(
      resolvePreShiftMeetingRuntime('PinganTeamDispatch', '班组派班').columns.map(
        (column) => column.title,
      ),
    ).toEqual([
      '公司',
      '车间',
      '班组',
      '班组任务',
      '排班类型',
      '管理人员人数（手录）',
      '派班日期',
      '派班状态',
      '派班时间',
      '操作',
    ]);

    expect(
      resolvePreShiftMeetingRuntime(
        'PinganPreShiftSafetyActivity',
        '班前安全活动',
      ).columns.map((column) => column.title),
    ).toEqual([
      '公司',
      '车间',
      '班组',
      '作业内容',
      '劳务班组人数（手录）',
      '设备是否点检',
      '班前安全活动记录表上传',
      '图片上传',
      '视频上传',
      '日期',
      '创建时间',
      '状态',
      '操作',
    ]);

    expect(
      resolvePreShiftMeetingRuntime(
        'PinganPreShiftInspection',
        '班前检查',
      ).columns.map((column) => column.title),
    ).toEqual([
      '公司',
      '车间',
      '班组',
      '负责人',
      '检查日期',
      '图片打卡',
      '状态',
      '操作',
    ]);

    expect(
      resolvePreShiftMeetingRuntime(
        'PinganMidShiftInspection',
        '班中检查',
      ).columns.map((column) => column.title),
    ).toEqual([
      '公司',
      '车间',
      '班组',
      '负责人',
      '检查日期',
      '图片打卡',
      '视频打卡',
      '状态',
      '操作',
    ]);

    expect(
      resolvePreShiftMeetingRuntime(
        'PinganPostShiftInspection',
        '班后检查',
      ).columns.map((column) => column.title),
    ).toEqual([
      '公司',
      '车间',
      '班组',
      '负责人',
      '检查日期',
      '图片打卡',
      '交班状态',
      '状态',
      '操作',
    ]);

    expect(
      resolvePreShiftMeetingRuntime('PinganKeySites', '重点场所').columns.map(
        (column) => column.title,
      ),
    ).toEqual([
      '公司',
      '场所类型',
      '检查部门',
      '责任部门',
      '检查日期',
      '责任人',
      '验收人',
      '状态',
      '操作',
    ]);
  });

  it('keeps the one-shift-three-check company column compact', () => {
    for (const routeName of oneShiftThreeCheckRouteNames) {
      const runtime = resolvePreShiftMeetingRuntime(routeName, routeName);
      expect(runtime.columns.find((column) => column.dataIndex === 'company'))
        .toMatchObject({
          title: '公司',
          width: 180,
        });
    }
  });

  it('hides the company column only in one-shift-three-check table display', () => {
    for (const routeName of oneShiftThreeCheckRouteNames) {
      const runtime = resolvePreShiftMeetingRuntime(routeName, routeName);
      expect(
        getThreeCheckTableDisplayColumns(
          runtime.routeName,
          runtime.columns,
        ).some((column) => column.dataIndex === 'company'),
      ).toBe(false);
      expect(
        runtime.columns.some((column) => column.dataIndex === 'company'),
      ).toBe(true);
    }

    const oldHazardRuntime = resolvePreShiftMeetingRuntime(
      'PinganHazardRectification',
      '隐患整改',
    );
    expect(oldHazardRuntime).toMatchObject({
      apiReady: false,
      createMode: 'DISABLED',
      routeName: 'PinganHazardRectification',
      title: '隐患整改',
    });
    expect(oldHazardRuntime.moduleKey).toBeUndefined();
    expect(oldHazardRuntime.bizType).not.toBe('HAZARD_RECTIFICATION');
  });

  it('resolves other Pingan module table headers from provided templates', () => {
    expect(
      resolvePreShiftMeetingRuntime(
        'PinganRiskLevelControl',
        '风险分级管控',
      ).columns.map((column) => column.title),
    ).toEqual(['编码', '操作']);

    expect(
      resolvePreShiftMeetingRuntime('PinganSafetyCheck', '安全检查').columns.map(
        (column) => column.title,
      ),
    ).toEqual([
      '检查单位',
      '受检单位',
      '受检项目',
      '检查类型',
      '检查日期',
      '状态',
      '操作',
    ]);

    expect(
      getThreeCheckDetailDisplayColumns(
        'PinganSafetyCheck',
        resolvePreShiftMeetingRuntime('PinganSafetyCheck', '安全检查').columns,
      ).map((column) => column.title),
    ).toEqual([
      '检查单位',
      '受检单位',
      '检查类型',
      '检查类型（手录）',
      '检查日期',
      '检查时间',
      '创建人',
      '状态',
      '检查方式',
      '检查人员',
      '验收人员',
      '检查人员（手录）',
      '受检单位人员',
      '受检单位人员（手录）',
      '检查内容',
      '备注',
      '整改验收是否通过',
      '验收日期',
      '明细计数',
      '附件',
    ]);

    expect(
      getThreeCheckDetailFieldGroups(
        'PinganSafetyCheck',
        getThreeCheckDetailDisplayColumns(
          'PinganSafetyCheck',
          resolvePreShiftMeetingRuntime('PinganSafetyCheck', '安全检查').columns,
        ),
      ).map((group) => ({
        fields: group.columns.map((column) => column.title),
        title: group.title,
      })),
    ).toEqual([
      {
        title: '检查信息',
        fields: [
          '检查单位',
          '创建人',
          '受检单位人员（手录）',
          '受检单位',
          '检查方式',
          '检查内容',
          '检查类型',
          '检查人员',
          '备注',
          '检查类型（手录）',
          '验收人员',
          '检查日期',
          '检查人员（手录）',
          '检查时间',
          '受检单位人员',
        ],
      },
    ]);

    expect(getThreeCheckDetailActions().map((action) => action.label)).toEqual([
      '刷新',
      '打印',
      '单据流',
      '附件',
      '变更历史',
      '修改',
    ]);
    expect(
      getThreeCheckDetailActions('PinganPointsFlow').map((action) => action.label),
    ).toEqual([]);
    expect(
      getThreeCheckDetailFieldGroups(
        'PinganPointsFlow',
        getThreeCheckDetailDisplayColumns(
          'PinganPointsFlow',
          resolvePreShiftMeetingRuntime('PinganPointsFlow', '积分流水').columns,
        ),
      ).map((group) => group.title),
    ).toEqual(['基本信息']);

    expect(
      resolvePreShiftMeetingRuntime(
        'PinganHazardRectification',
        '隐患整改',
      ).columns.map((column) => column.title),
    ).not.toEqual([
      '公司',
      '部门',
      '班组',
      '检查人',
      '责任人',
      '创建时间',
      '整改完成时间',
      '整改验收结果',
      '状态',
      '操作',
    ]);

    expect(
      resolvePreShiftMeetingRuntime('PinganQuickShot', '随手拍').columns.map(
        (column) => column.title,
      ),
    ).toEqual([
      '公司',
      '部门',
      '班组',
      '上报人',
      '上传时间',
      '状态',
      '是否启用AI',
      '隐患图片/视频',
      '隐患描述',
      '整改措施',
      '操作',
    ]);

    expect(
      resolvePreShiftMeetingRuntime(
        'PinganCurtainWallPenalty',
        '幕墙处罚管理',
      ).columns.map((column) => column.title),
    ).toEqual([
      '公司',
      '责任单位',
      '劳务单位',
      '隐患描述',
      '图片一',
      '图片二',
      '图片三',
      '处罚结论',
      '日期',
      '批准人',
      '检查人',
      '是否已读',
      '状态',
      '操作',
    ]);

    expect(
      resolvePreShiftMeetingRuntime(
        'PinganCurtainWallRoutineCheck',
        '幕墙日周月检',
      ).columns.map((column) => column.title),
    ).toEqual([
      '公司',
      '部门',
      '巡检记录表类型',
      '图片上传',
      '视频上传',
      '日期',
      '状态',
      '操作',
    ]);

    expect(
      resolvePreShiftMeetingRuntime('PinganExamTasks', '考试任务').columns.map(
        (column) => column.title,
      ),
    ).toEqual([
      '公司',
      '部门',
      '考试人员',
      '考试',
      '考分',
      '考试日期',
      '状态',
      '操作',
    ]);

    expect(
      resolvePreShiftMeetingRuntime('PinganExamResults', '考试成绩').columns.map(
        (column) => column.title,
      ),
    ).toEqual([
      '公司',
      '部门',
      '考试',
      '考试日期',
      '状态',
      '创建时间',
      '操作',
    ]);

    expect(
      resolvePreShiftMeetingRuntime('PinganPointsFlow', '积分流水').columns.map(
        (column) => column.title,
      ),
    ).toEqual([
      '流水号',
      '创建时间',
      '公司',
      '部门',
      '班组',
      '用户',
      '积分变动原因',
      '积分变动',
      '积分数量',
      '贩卖机',
      '货品',
      '操作',
    ]);

    expect(resolvePreShiftMeetingRuntime('PinganPointsFlow', '积分流水')).toMatchObject({
      apiMode: 'THREE_CHECK_RECORD',
      apiReady: true,
      bizType: 'SAFETY_POINTS_FLOW',
      createMode: 'API',
      moduleKey: 'points-flow',
      routeName: 'PinganPointsFlow',
      title: '积分流水',
    });
    expect(isPointsFlowRoute('PinganPointsFlow')).toBe(true);
    expect(getThreeCheckFieldOptions('PinganPointsFlow', 'pointsChange')).toEqual([
      { label: '加分', value: '加分' },
      { label: '扣分', value: '扣分' },
      { label: '兑换', value: '兑换' },
    ]);
    expect(getThreeCheckFieldOptions('PinganPointsFlow', 'pointsReason')).toEqual([
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
    ]);
    expect(getThreeCheckStatusFilterOptions('PinganPointsFlow')).toEqual([
      { label: '全部', value: 'all' },
    ]);
    expect(getThreeCheckSpecialFilterFields('PinganPointsFlow')).toEqual({
      company: true,
      dateRange: true,
      department: true,
      pointsReason: true,
      search: true,
      status: false,
      team: false,
    });
    expect(getThreeCheckOperationPolicy('PinganPointsFlow')).toEqual({
      batchDelete: false,
      delete: false,
      edit: true,
      remind: false,
      submit: false,
      view: true,
      withdraw: false,
    });

    expect(
      resolvePreShiftMeetingRuntime(
        'PinganRiskHazardLibrary',
        '风险隐患库',
      ).columns.map((column) => column.title),
    ).toEqual(['编码', '操作']);
  });

  it('declares generic backend api metadata for the seven non-meeting one-shift-three-check modules', () => {
    const genericModules = [
      ['PinganTeamDispatch', '班组派班', 'team-dispatch'],
      [
        'PinganCurtainWallTeamDispatch',
        '幕墙班组派班',
        'curtain-wall-team-dispatch',
      ],
      [
        'PinganPreShiftSafetyActivity',
        '班前安全活动',
        'pre-shift-safety-activity',
      ],
      ['PinganPreShiftInspection', '班前检查', 'pre-shift-inspection'],
      ['PinganMidShiftInspection', '班中检查', 'mid-shift-inspection'],
      ['PinganPostShiftInspection', '班后检查', 'post-shift-inspection'],
      ['PinganKeySites', '重点场所', 'key-sites'],
    ] as const;

    expect(
      genericModules.map(([routeName, title]) =>
        resolvePreShiftMeetingRuntime(routeName, title),
      ),
    ).toEqual(
      genericModules.map(([routeName, title, moduleKey]) =>
        expect.objectContaining({
          apiMode: 'THREE_CHECK_RECORD',
          apiReady: true,
          createMode: 'API',
          isPreShiftMeetingCompatible: false,
          moduleKey,
          routeName,
          title,
        }),
      ),
    );

    expect(
      createThreeCheckModuleRuntime({
        businessColumns: preShiftMeetingBusinessColumnTitles.map((title) => ({
          dataIndex: title,
          title,
          width: 120,
        })),
        routeName: 'PinganSameHeaderExample',
        routeTitle: '同表头示例',
      }),
    ).toMatchObject({
      apiMode: 'NONE',
      apiReady: false,
      createMode: 'DISABLED',
      isPreShiftMeetingCompatible: true,
      routeName: 'PinganSameHeaderExample',
      title: '同表头示例',
    });
  });

  it('connects hazard inspection routes to generic record api metadata', () => {
    const hazardModules: Array<[string, string, string, string]> = [
      [
        'PinganSafetyCheck',
        '安全检查',
        'safety-check',
        'HAZARD_SAFETY_CHECK',
      ],
      [
        'PinganQuickShot',
        '随手拍',
        'quick-shot',
        'HAZARD_QUICK_SHOT',
      ],
      [
        'PinganCurtainWallPenalty',
        '幕墙处罚管理',
        'curtain-wall-penalty',
        'HAZARD_CURTAIN_WALL_PENALTY',
      ],
      [
        'PinganCurtainWallRoutineCheck',
        '幕墙日周月检',
        'curtain-wall-routine-check',
        'HAZARD_CURTAIN_WALL_ROUTINE_CHECK',
      ],
    ];

    expect(
      hazardModules.map(([routeName, title]) =>
        resolvePreShiftMeetingRuntime(routeName, title),
      ),
    ).toEqual(
      hazardModules.map(([routeName, title, moduleKey, bizType]) =>
        expect.objectContaining({
          apiMode: 'THREE_CHECK_RECORD',
          apiReady: true,
          bizType,
          createMode: 'API',
          isPreShiftMeetingCompatible: false,
          moduleKey,
          routeName,
          title,
        }),
      ),
    );
  });

  it('includes hazard inspection required fields in create forms even when hidden from tables', () => {
    const safetyRuntime = resolvePreShiftMeetingRuntime(
      'PinganSafetyCheck',
      '安全检查',
    );
    expect(
      getThreeCheckCreateDisplayColumns(
        safetyRuntime.routeName,
        safetyRuntime.columns,
      ).map((column) => column.title),
    ).toEqual([
      '检查单位',
      '受检单位',
      '受检项目',
      '检查类型',
      '检查日期',
      '创建人',
      '检查方式',
      '检查人员',
      '验收人员',
      '检查时间',
      '受检单位人员',
      '检查内容',
      '状态',
    ]);

    const oldHazardRuntime = resolvePreShiftMeetingRuntime(
      'PinganHazardRectification',
      '隐患整改',
    );
    expect(oldHazardRuntime.apiReady).toBe(false);
    expect(oldHazardRuntime.moduleKey).toBeUndefined();
    expect(
      getThreeCheckCreateDisplayColumns(
        oldHazardRuntime.routeName,
        oldHazardRuntime.columns,
      ).map((column) => column.title),
    ).not.toEqual([
      '公司',
      '部门',
      '班组',
      '检查人',
      '责任人',
      '检查方式',
      '检查类型',
      '创建时间',
      '整改完成时间',
      '整改验收结果',
      '状态',
      '更新时间',
    ]);
  });

  it('keeps unknown modules disabled while known one-shift modules use their own table shape', () => {
    expect(
      resolvePreShiftMeetingRuntime('PinganTeamDispatch', '班组派班'),
    ).toMatchObject({
      apiReady: true,
      createMode: 'API',
      isPreShiftMeetingCompatible: false,
      title: '班组派班',
    });

    expect(
      resolvePreShiftMeetingRuntime('UnknownThreeCheckRoute', '未知模块'),
    ).toMatchObject({
      apiReady: false,
      createMode: 'DISABLED',
      isPreShiftMeetingCompatible: false,
      title: '未知模块',
    });
  });

  it('creates a local row that matches the active module columns', () => {
    const runtime = resolvePreShiftMeetingRuntime('PinganTeamDispatch', '班组派班');

    const row = createLocalThreeCheckRow({
      columns: runtime.columns,
      now: new Date('2026-05-14T08:30:00+08:00'),
      routeName: runtime.routeName,
      values: {
        company: 'Demo Works Company',
        department: '选矿车间',
        dispatchDate: '2026-05-14',
        dispatchStatus: '生效',
        dispatchTime: '2026-05-14 08:30',
        dispatchType: '常规派班',
        managerCount: '3',
        team: '一班',
        teamTask: '尾矿库巡检',
      },
    });

    expect(row).toMatchObject({
      canRemind: false,
      canSubmit: true,
      canWithdraw: false,
      company: 'Demo Works Company',
      date: '2026-05-14',
      department: '选矿车间',
      dispatchDate: '2026-05-14',
      dispatchStatus: '生效',
      dispatchTime: '2026-05-14 08:30',
      dispatchType: '常规派班',
      id: 'local-PinganTeamDispatch-1778718600000',
      managerCount: '3',
      sourceChannel: 'PC',
      status: 'DRAFT',
      team: '一班',
      teamTask: '尾矿库巡检',
    });
    expect(row).not.toHaveProperty('actions');
  });

  it('applies submit, withdraw, and remind locally for template-backed modules', () => {
    const runtime = resolvePreShiftMeetingRuntime(
      'PinganPreShiftInspection',
      '班前检查',
    );
    const row = createLocalThreeCheckRow({
      columns: runtime.columns,
      now: new Date('2026-05-14T08:30:00+08:00'),
      routeName: runtime.routeName,
      values: {
        company: 'Demo Works Company',
        date: '2026-05-14',
        department: '幕墙组装',
        imageCheck: '未上传',
        owner: 'Demo Harbor班长',
        status: '待检查',
        team: '幕墙组装1班',
      },
    });

    expect(row).toMatchObject({
      canRemind: false,
      canSubmit: true,
      canWithdraw: false,
      status: 'DRAFT',
      statusLabel: '待检查',
    });

    const submitted = applyLocalThreeCheckAction(row, 'SUBMIT');
    expect(submitted).toMatchObject({
      canRemind: true,
      canSubmit: false,
      canWithdraw: true,
      status: 'OPENED',
      statusLabel: '已检查',
    });

    const reminded = applyLocalThreeCheckAction(submitted, 'REMIND');
    expect(reminded).toMatchObject({
      reminderCount: 1,
      status: 'OPENED',
      statusLabel: '已催办',
    });

    const withdrawn = applyLocalThreeCheckAction(reminded, 'WITHDRAW');
    expect(withdrawn).toMatchObject({
      canRemind: false,
      canSubmit: true,
      canWithdraw: false,
      status: 'WITHDRAWN',
      statusLabel: '待检查',
    });
  });

  it('withdraws team dispatch from active to inactive labels', () => {
    const runtime = resolvePreShiftMeetingRuntime(
      'PinganTeamDispatch',
      '班组派班',
    );
    const row = createLocalThreeCheckRow({
      columns: runtime.columns,
      now: new Date('2026-05-14T08:30:00+08:00'),
      routeName: runtime.routeName,
      values: {
        company: 'Demo Works Company',
        department: '幕墙组装',
        dispatchDate: '2026-05-14',
        dispatchStatus: '未生效',
        dispatchTime: '2026-05-14 08:30',
        managerCount: '3',
        team: '幕墙组装1班',
        teamTask: '吊装作业派班',
      },
    });

    const submitted = applyLocalThreeCheckAction(row, 'SUBMIT');
    expect(submitted).toMatchObject({
      status: 'OPENED',
      statusLabel: '已生效',
    });

    const withdrawn = applyLocalThreeCheckAction(submitted, 'WITHDRAW');
    expect(withdrawn).toMatchObject({
      canSubmit: true,
      canWithdraw: false,
      status: 'WITHDRAWN',
      statusLabel: '未生效',
    });
  });

  it('builds editable detail forms and update payloads for withdrawn generic records', () => {
    const runtime = resolvePreShiftMeetingRuntime(
      'PinganPreShiftInspection',
      '班前检查',
    );
    const detail = {
      businessDate: '2026-05-18',
      canSubmit: true,
      company: 'Demo Works Company',
      companyId: 4,
      date: '2026-05-18',
      department: '幕墙组装',
      departmentId: 101_109,
      id: '3001',
      imageCheck: '未上传',
      moduleKey: 'pre-shift-inspection',
      owner: 'Demo Harbor班长',
      ownerUserId: 2,
      payload: {
        owner: 'Demo Harbor班长',
        statusLabel: '待检查',
      },
      status: 'WITHDRAWN',
      statusLabel: '待检查',
      team: '幕墙组装1班',
      teamId: 1_011_001,
      version: 3,
      videoCheck: '未上传',
    };

    expect(isThreeCheckDetailEditable(detail)).toBe(true);
    expect(createThreeCheckDetailEditForm(detail, runtime.columns)).toMatchObject({
      date: '2026-05-18',
      imageCheck: '未上传',
      owner: 'Demo Harbor班长',
    });

    expect(
      createThreeCheckRecordUpdatePayloadFromDetail(detail, {
        imageCheck: '现场照片',
        owner: 'Demo Harbor班长',
      }),
    ).toEqual({
      businessDate: '2026-05-18',
      companyId: 4,
      departmentId: 101_109,
      ownerUserId: 2,
      payload: {
        imageCheck: '现场照片',
        owner: 'Demo Harbor班长',
        statusLabel: '待检查',
      },
      status: '待检查',
      teamId: 1_011_001,
      version: 3,
    });
  });

  it('defines select options for local one-shift-three-check fields', () => {
    expect(
      getThreeCheckFieldOptions('PinganTeamDispatch', 'dispatchStatus'),
    ).toEqual([
      { label: '已生效', value: '已生效' },
      { label: '未生效', value: '未生效' },
    ]);

    expect(
      getThreeCheckFieldOptions(
        'PinganCurtainWallTeamDispatch',
        'dispatchType',
      ),
    ).toEqual([
      { label: '今日', value: '今日' },
      { label: '明日', value: '明日' },
    ]);

    expect(
      getThreeCheckFieldOptions('PinganPreShiftSafetyActivity', 'workContent'),
    ).toEqual([
      { label: '吊装作业', value: '吊装作业' },
      { label: '吊篮作业', value: '吊篮作业' },
      { label: '高处作业', value: '高处作业' },
      { label: '动火作业', value: '动火作业' },
      { label: '临时用电', value: '临时用电' },
      { label: '机械作业', value: '机械作业' },
      { label: '脚手架作业', value: '脚手架作业' },
      { label: '维修（收尾工程）', value: '维修（收尾工程）' },
    ]);

    expect(getThreeCheckFieldOptions('PinganKeySites', 'status')).toEqual([
      { label: '已检查', value: '已检查' },
      { label: '待检查', value: '待检查' },
    ]);
  });

  it('wires the three-check detail toolbar to the requested detail actions', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );

    expect(page).toContain('getThreeCheckDetailActions');
    expect(page).toContain("handleDetailAction('refresh')");
    expect(page).toContain("handleDetailAction('print')");
    expect(page).toContain("handleDetailAction('documentFlow')");
    expect(page).toContain('getHazardInspectionDocumentFlowApi');
    expect(page).toContain('getHazardInspectionChangeHistoryApi');
    expect(page).toContain('openDocumentFlow');
    expect(page).toContain('openChangeHistory');
    expect(page).toContain('isHazardInspectionRoute');
    expect(page).toContain('documentFlowOpen');
    expect(page).toContain('changeHistoryOpen');
    expect(page).toContain('documentFlowLoading');
    expect(page).toContain('changeHistoryLoading');
    expect(page).toContain('documentFlowKeyword');
    expect(page).toContain('changeHistoryKeyword');
    expect(page).toContain('documentFlowActiveTab');
    expect(page).toContain('documentFlowSelectedNodeId');
    expect(page).toContain('changeHistorySelectedGroupId');
    expect(page).toContain('currentDocumentFlow');
    expect(page).toContain('currentChangeHistory');
    expect(page).toContain('formatChangeHistoryTime');
    expect(page).toContain('changeHistoryFieldLabel');
    expect(page).toContain('changeHistoryDisplayVersion');
    expect(page).toContain('documentFlowSummary');
    expect(page).toContain('changeHistorySummary');
    expect(page).toContain('documentFlowSteps');
    expect(page).toMatch(/date:\s*formatChangeHistoryTime\(log\.createdAt\)/);
    expect(page).toContain('changeHistoryGroups');
    expect(page).toContain('filteredDocumentFlowNodes');
    expect(page).toContain('documentFlowTreeDetailNodes');
    expect(page).toContain('documentFlowListNodes');
    expect(page).toContain('document-flow-tree-children');
    expect(page).toContain('setDocumentFlowActiveTab');
    expect(page).toMatch(
      /documentFlowSelectedNodeId\.value\s*=\s*documentFlowNodes\.value\[0\]\?\.id/,
    );
    expect(page).toContain('document-flow-modal');
    expect(page).toContain('change-history-modal');
    expect(page).toContain('全链路洞察');
    expect(page).toContain('字段级追溯');
    expect(page).toContain('单据树');
    expect(page).toContain('明细列表');
    expect(page).toContain('查看原单');
    expect(page).toContain('创建记录');
    expect(page).toContain('提交检查');
    expect(page).toContain('整改处理中');
    expect(page).toContain('待验收');
    expect(page).toContain('已完成');
    expect(page).toContain('document-flow-attachments__label');
    expect(page).toContain('字段');
    expect(page).toContain('变更前');
    expect(page).toContain('变更后');
    expect(page).toContain('附件变更');
    expect(page).toContain('change-history-sidebar__track');
    expect(page).toContain('change-history-summary-card__icon');
    expect(page).toContain('change-history-group__top');
    expect(page).toContain('change-history-group__meta');
    expect(page).toContain('change-history-group__stats');
    expect(page).toContain('字段 {{ group.fieldCount }}');
    expect(page).toContain('附件 {{ group.attachmentCount }}');
    expect(page).toContain('流程 {{ group.flowCount }}');
    expect(page).toContain('document-flow-detail-field');
    expect(page).toContain('change-history-diff-table');
    expect(page).toMatch(/formatChangeHistoryTime\(latest\?\.createdAt\)/);
    expect(page).toMatch(/V\{\{\s*changeHistoryDisplayVersion\(group\)/);
    expect(page).toMatch(/changeHistoryFieldLabel\(item\)/);
    expect(page).toMatch(
      /\.change-history-sidebar__track[\s\S]*background:\s*#d8e1ed/,
    );
    expect(page).toMatch(
      /\.change-history-summary-card__icon[\s\S]*border-radius:\s*50%/,
    );
    expect(page).toMatch(
      /\.change-history-group:not\(\.is-active\):not\(\.is-latest\)[\s\S]*background:\s*linear-gradient\(135deg,\s*#9ca3af,\s*#6b7280\)/,
    );
    expect(page).toContain('document-flow-field-image--compact');
    expect(page).toMatch(
      /\.document-flow-field-image--compact[\s\S]*width:\s*96px[\s\S]*height:\s*64px/,
    );
    expect(page).toContain('document-flow-attachment-strip');
    expect(page).toContain("handleDetailAction('attachment')");
    expect(page).toContain("activeDetailPanel === 'attachment'");
    expect(page).toContain('detailPreviewAttachments');
    expect(page).toContain('detail-attachment-panel');
    const attachmentPanel = page.match(
      /<section\s+v-else-if="!isPointsFlowModule && activeDetailPanel === 'attachment'"[\s\S]*?<\/section>/,
    )?.[0];
    expect(attachmentPanel).toContain('detail-attachment-file-list');
    expect(attachmentPanel).toContain('attachmentFileKindLabel(attachment.fileKind)');
    expect(attachmentPanel).toContain('attachment.originalName');
    expect(attachmentPanel).not.toContain('attachmentPreviewUrl');
    expect(attachmentPanel).not.toContain('openVideoPreview');
    expect(page).toContain("handleDetailAction('changeHistory')");
    expect(page).toContain("handleDetailAction('edit')");
    expect(page).toContain('window.print()');
    expect(page).toContain('detailAttachmentSectionRef');
    expect(page).toContain('IconifyIcon');
    expect(page).toContain('detailActionIcon');
    expect(page).toContain('meeting-detail--safety-check');
    expect(page).toContain('const isSafetyCheckDetail = computed');
    expect(page).toContain('v-if="isSafetyCheckDetail" class="safety-check-lines hazard-detail-lines"');
    expect(page).toContain('safetyCheckDetailLineColumns');
    expect(page).toContain(
      "{ dataIndex: 'aiEnabled', title: '是否启用AI'",
    );
    expect(page).toContain('safetyCheckAiEnabledOptions');
    expect(page).toContain("column.dataIndex === 'aiEnabled'");
    expect(
      page.match(/column\.dataIndex === 'aiEnabled'/g) ?? [],
    ).toHaveLength(1);
    expect(page).toContain(':options="safetyCheckAiEnabledOptions"');
    expect(page).toContain("{ label: '是', value: '是' }");
    expect(page).toContain("{ label: '否', value: '否' }");
    expect(page).toContain(
      "{ dataIndex: 'checkResult', title: '检查结果'",
    );
    expect(page).toContain('safetyCheckResultOptions');
    expect(
      page.match(/column\.dataIndex === 'checkResult'/g) ?? [],
    ).toHaveLength(3);
    expect(page).toContain(':options="safetyCheckResultOptions"');
    expect(page).toContain("{ label: '无隐患', value: '无隐患' }");
    expect(page).toContain("{ label: '有隐患', value: '有隐患' }");
    expect(page).toContain(
      "{ dataIndex: 'rectificationOrderNo', title: '关联整改工单'",
    );
    expect(page).toContain(
      "{ dataIndex: 'rectificationStatusLabel', title: '工单状态'",
    );
    expect(page).toContain(
      "{ dataIndex: 'rectificationClosedAt', title: '闭环时间'",
    );
    expect(page).not.toContain(
      'const safetyCheckDetailLineColumns = hazardRectificationDetailColumns',
    );
    expect(page).not.toContain('v-if="isHazardRectificationDetail"');
    expect(page).not.toContain('hazard-detail-summary');
    expect(page).toContain('hazardInspectionRequiredFieldKeys');
    expect(page).toContain('validateHazardInspectionRequiredFields');
    expect(page).toContain("message.error(`请填写${missingFields[0]?.title || '必填项'}`)");
    expect(page).toContain('hazardInspectionEmptyValue');
    expect(page).toContain("isHazardInspectionDetail.value ? '' : '未填写'");
    expect(page).toContain('grid-auto-flow: column;');
    expect(page).toContain('grid-template-rows: repeat(3, minmax(72px, auto));');
    expect(page).toMatch(
      /\.safety-check-lines\s+\.hazard-lines-table\s+(?:th,\s*)?\.safety-check-lines\s+\.hazard-lines-table\s+td[\s\S]*color:\s*#475569/,
    );
    expect(page).toMatch(
      /\.safety-check-lines\s+\.hazard-lines-text[\s\S]*color:\s*#111827/,
    );
    expect(page).toMatch(
      /\.meeting-detail--safety-check\s+\.detail-field\s+dt\s*\{[^}]*color:\s*#475569/,
    );
    expect(page).toMatch(
      /\.meeting-detail--safety-check\s+\.detail-field\s+dd\s*\{[^}]*color:\s*#111827/,
    );
    expect(page).toContain('detail-attachment-row');
    expect(page).toContain('detail-attachment-upload-card');
    expect(page).toContain('handleAnyAttachmentChange');
    expect(page).toContain('detail-attachment-field');
    expect(page).toContain('handleDetailAttachmentFieldChange');
    expect(page).toContain('detailAttachmentFieldAccept');
    expect(page).toContain('detailAttachmentFieldPreview');
    expect(page).toContain('!isDetailAttachmentColumn(column)');
    expect(page).toContain('!isDetailAttachmentColumn(');
    expect(page).toContain('hazard-lines-upload');
    expect(page).not.toContain("font-family: SimSun, '宋体', serif");
    expect(page).toContain('color: #64748b');
    expect(page).toContain('color: #111827');
    expect(page).not.toContain(
      '.meeting-detail--safety-check .detail-action-bar {',
    );
    expect(page).not.toContain(
      '.meeting-detail--safety-check .detail-action-button {',
    );
    expect(page).not.toContain('meeting-detail--hazard-rectification');
    expect(page).not.toContain(
      '.meeting-detail--hazard-rectification .detail-action-bar {',
    );
    expect(page).toContain('.detail-field--status dd');
    expect(page).toContain('.detail-field--remarks');
    expect(page).toContain('defaultDetailColumnGroups');
    expect(page).toContain('detail-field-grid--balanced');
    expect(page).toContain('detail-field-column');
    expect(page).toContain('min-height: 64px');
    expect(page).toContain('.meeting-detail > dl');
    expect(page).not.toContain('.meeting-detail dl {');
    expect(page).toContain('min(1480px, calc(100vw - 32px))');
    expect(page).toContain('isLongTextDetailColumn(column)');
    expect(page).toContain('<Input.TextArea');
    expect(page).toContain('detail-edit-textarea');
    expect(page).toContain("'inspectionType'");
    expect(page).toContain("'inspectionTypeManual'");
    expect(page).toContain('.detail-field--inspectionType');
    expect(page).toContain(':table-layout="');
    expect(page).toContain(':row-selection="tableRowSelection"');
    expect(page).toContain('selectedRowKeys');
    expect(page).toMatch(
      /<span\s+v-show="isThreeCheckRecordModule && operationPolicy\.batchDelete"\s+class="table-panel__selection"/,
    );
    expect(page).toContain('table-panel__actions');
    expect(page).toContain('批量删除');
    expect(page).toContain("handleBatchAction('DELETE')");
    expect(page).not.toContain('<template v-if="isThreeCheckRecordModule">');
    expect(page).not.toContain('清空选择');
    expect(page).not.toContain('批量提交');
    expect(page).not.toContain('批量撤回');
    expect(page).not.toContain('批量催办');
    expect(page).toContain('handleDelete(record)');
    expect(page).toContain('deleteThreeCheckRecordApi');
    expect(page).toContain('batchThreeCheckRecordsApi');
    expect(page).toContain('isPointsFlowRoute(moduleRuntime.value.routeName)');
    expect(page).toContain('filters.pointsReason');
    expect(page).toContain('pointsReasonFilterOptions');
    expect(page).toContain('showPointsReasonFilter');
    expect(page).toContain('showStatusFilter');
    expect(page).toContain('showTeamFilter');
    expect(page).toContain('departmentFilterLabel');
    expect(page).toContain('v-if="!isPointsFlowModule" class="detail-action-bar"');
    expect(page).toContain("!isPointsFlowModule && activeDetailPanel === 'documentFlow'");
    expect(page).toContain("!isPointsFlowModule && activeDetailPanel === 'changeHistory'");
    expect(page).toContain("!isPointsFlowModule && activeDetailPanel === 'attachment'");
    expect(page).toMatch(
      /v-if="showPointsReasonFilter"[\s\S]*积分变动原因[\s\S]*<Select[\s\S]*placeholder="点击选择"/,
    );
    expect(page).toMatch(/v-if="showStatusFilter"[\s\S]*activeStatusOptions/);
    expect(page).toContain('<StatusFilterActions');
    expect(page).toContain(':options="activeStatusOptions"');
    expect(page).toContain('@update:model-value="setWorkbenchStatusFilter"');
    expect(page).not.toContain('status-filter-tabs');
    expect(page).toMatch(
      /v-if="showTeamFilter && isOrganizationFilterVisible\('team'\)"[\s\S]*filterTeamSelectOptions/,
    );
    expect(page).toContain('operationPolicy.delete');
    expect(page).toContain('operationPolicy.submit');
    expect(page).toContain('operationPolicy.withdraw');
    expect(page).toContain('operationPolicy.remind');
    expect(page).toContain('ellipsis: column.dataIndex !==');
    expect(page).toContain('meeting-table__actions-cell');
    expect(page).toContain('text-overflow: ellipsis');
    expect(page).toContain('hazard-lines-table');
    expect(page).toContain('safetyCheckDetailLineColumns');
    expect(page).not.toContain("routeName === 'PinganHazardRectification'");
    expect(page).toMatch(
      /isPointsFlowRoute\(moduleRuntime\.value\.routeName\)[\s\S]*min\(1080px, calc\(100vw - 32px\)\)/,
    );
    expect(page).toContain('wideDetailRouteNames');
    expect(page).toContain('hazardDetailRouteNames');
    expect(page).toContain(':centered="detailModalCentered"');
    expect(page).toContain("'PinganQuickShot'");
    expect(page).toContain("'PinganCurtainWallPenalty'");
    expect(page).toContain("'PinganCurtainWallRoutineCheck'");
    expect(page).toContain('min(1280px, calc(100vw - 32px))');
    expect(page).toContain('pingan-detail-modal--wide');
    expect(page).toContain('hazard-lines-edit-control');
    expect(page).toContain('hazardInspectionLines');
    expect(page).toContain('filteredHazardInspectionLines');
    expect(page).toContain('@click="addHazardInspectionLine"');
    expect(page).toContain('@click="downloadHazardInspectionLines"');
    expect(page).toContain('@click="openHazardInspectionView"');
    expect(page).toContain('v-model:value="hazardInspectionLineKeyword"');
    expect(page).toContain('hazardInspectionLineValidationLabel');
    expect(page).toContain('v-model:open="hazardInspectionViewOpen"');
    expect(page).toContain('v-model:value="hazardInspectionVisibleColumnKeys"');
    expect(page).not.toContain('hazardRectificationDetailColumns');
    expect(page).not.toContain('hazardRectificationSummaryFields');
    expect(page).not.toContain('hazard-summary-edit-control');
    expect(page).not.toContain("field.dataIndex === 'rectificationAcceptanceResult'");
    expect(page).not.toContain('hazard-acceptance-checks');
    expect(page).not.toContain('isHazardRectificationAcceptanceSelected');
  });

  it('keeps quick-shot local workflow actions to review only and renders linked rectification order read-only', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );

    const workflowActions = sourceBetween(
      page,
      'function quickShotWorkflowActions(record: Record<string, any>)',
      'function quickShotWorkflowActionLabel',
    );
    expect(workflowActions).toContain("'APPROVE'");
    expect(workflowActions).toContain("'REJECT'");
    expect(workflowActions).not.toContain('ISSUE_RECTIFICATION');
    expect(workflowActions).not.toContain('MARK_RECTIFIED');
    expect(workflowActions).not.toContain('REQUEST_ACCEPTANCE');
    expect(workflowActions).not.toContain('ACCEPT');

    const workflowFields = sourceBetween(
      page,
      'const quickShotWorkflowFieldGroups',
      'const quickShotDocumentFlowFieldGroups',
    );
    expect(workflowFields).toContain('APPROVE');
    expect(workflowFields).toContain('REJECT');
    expect(workflowFields).not.toContain('ISSUE_RECTIFICATION');
    expect(workflowFields).not.toContain('MARK_RECTIFIED');
    expect(workflowFields).not.toContain('REQUEST_ACCEPTANCE');
    expect(workflowFields).not.toContain('ACCEPT');

    expect(page).toContain('linkedRectificationOrder');
    expect(page).toContain('quickShotLinkedRectificationOrderNode');
    expect(page).toContain('关联隐患整改工单');
    expect(page).toContain('工单流程');
    expect(page).toContain('整改后照片');
    expect(page).toContain('整改说明');
  });

  it('refreshes the record version before persisting hazard line uploads', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );
    const uploadHandler = page.match(
      /async function handleDetailAttachmentFieldChange[\s\S]*?function attachmentPreviewUrl/,
    )?.[0];

    expect(uploadHandler).toContain(
      'const refreshedDetail = await getThreeCheckRecordDetailApi',
    );
    expect(uploadHandler).toContain('version: refreshedDetail.version');
  });

  it('previews hazard line photos only from the line field to avoid cross-field leakage', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );
    const previewHandler = page.match(
      /function hazardInspectionLineImagePreview[\s\S]*?function downloadHazardInspectionLines/,
    )?.[0];

    expect(previewHandler).not.toContain('detail.attachments.filter');
    expect(previewHandler).not.toContain('lineIndex * 2');
  });

  it('defines hazard inspection specific create controls', () => {
    expect(
      getThreeCheckOrganizationSelectTypes(
        'PinganSafetyCheck',
        'inspectionUnit',
      ),
    ).toEqual(['GROUP', 'COMPANY']);
    expect(
      getThreeCheckOrganizationSelectTypes(
        'PinganSafetyCheck',
        'inspectedUnit',
      ),
    ).toEqual(['COMPANY']);
    expect(
      resolvePreShiftMeetingRuntime('PinganHazardRectification', '隐患整改')
        .apiReady,
    ).toBe(false);

    expect(getThreeCheckFieldOptions('PinganQuickShot', 'aiEnabled')).toEqual([
      { label: '是', value: '是' },
      { label: '否', value: '否' },
    ]);
    expect(
      isThreeCheckCreateAttachmentField('PinganQuickShot', 'hazardImage'),
    ).toBe(true);
  });

  it('uses quick shot status and media fields confirmed for hazard intake', () => {
    const quickShotColumns = resolvePreShiftMeetingRuntime(
      'PinganQuickShot',
      '随手拍',
    ).columns.map((column) => column.title);

    expect(quickShotColumns).toContain('上传时间');
    expect(quickShotColumns).toContain('隐患图片/视频');
    expect(quickShotColumns).not.toContain('上传日期');
    expect(quickShotColumns).not.toContain('检查时间');
    expect(quickShotColumns).not.toContain('隐患图片');
    expect(getThreeCheckStatusFilterOptions('PinganQuickShot')).toEqual([
      { label: '全部', value: 'all' },
      { label: '待审核', value: 'PENDING_REVIEW' },
      { label: '已审核', value: 'REVIEWED' },
    ]);
  });

  it('requires quick shot hazard media and renders upload time as a date-time input', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );

    expect(page).toContain('validateQuickShotHazardMedia');
    expect(page).toContain('请上传隐患图片/视频');
    expect(page).toContain("column.dataIndex === 'uploadTime'");
    expect(page).toContain('show-time');
    expect(page).toContain('value-format="YYYY-MM-DD HH:mm"');
    expect(page).not.toContain('附件（图片或视频）');
  });

  it('keeps quick shot review actions while also showing submit withdraw and remind actions', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );
    const actionCell = sourceBetween(
      page,
      '<template v-else-if="column.dataIndex === \'actions\'">',
      '<template #emptyText>',
    );

    expect(actionCell).toContain('quickShotWorkflowActions(record)');
    expect(actionCell).toContain('handleSubmit(record)');
    expect(actionCell).toContain('handleWithdraw(record)');
    expect(actionCell).toContain('handleRemind(record)');
    expect(actionCell).not.toContain('<template v-else>');
  });

  it('previews quick shot hazard images or videos from uploaded attachment urls on the main table', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );
    const previewHandler = sourceBetween(
      page,
      'function attachmentPreviewFor(record: Record<string, any>, column: unknown)',
      'function attachmentPillClass',
    );

    expect(page).toContain('function attachmentRenderKind');
    expect(page).toContain('isQuickShotHazardMediaColumn');
    expect(previewHandler).toContain('record.videoPreviewUrl');
    expect(previewHandler).toContain('record.imagePreviewUrl');
    expect(page).toContain("attachmentRenderKind(record, column) === 'VIDEO'");
  });

  it('labels quick shot detail attachments as hazard image or video', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );

    expect(page).toContain('detailAttachmentLabel');
    expect(page).toContain("isQuickShotModule.value ? '隐患图片/视频' : '附件'");
    expect(page).toContain('{{ detailAttachmentLabel }}');
  });

  it('normalizes legacy hazard inspection payload into editable lines', () => {
    expect(
      normalizeHazardInspectionLines({
        afterRectificationPhoto: '/uploads/after.jpg',
        checkResult: '有隐患',
        beforeRectificationPhoto: '/uploads/before.jpg',
        hazardDescription: '脚手架隐患',
        hazardLibrary: '高处作业',
        rectificationDeadline: '2026-05-30',
        rectificationMeasures: '加固',
        rectificationResponsiblePerson: '张三',
        statusLabel: '待整改',
      }),
    ).toEqual([
      {
        afterRectificationPhoto: '/uploads/after.jpg',
        aiEnabled: '',
        beforeRectificationPhoto: '/uploads/before.jpg',
        checkResult: '有隐患',
        hazardDescription: '脚手架隐患',
        hazardLibrary: '高处作业',
        rectificationDeadline: '2026-05-30',
        rectificationMeasures: '加固',
        rectificationResponsiblePerson: '张三',
        sequence: '1',
        status: '待整改',
        validated: false,
      },
    ]);
  });

  it('creates and serializes hazard inspection lines with first-line mirrors', () => {
    const firstLine = normalizeHazardInspectionLines({
      checkResult: '有隐患',
      hazardDescription: '洞口未防护',
      rectificationMeasures: '安装防护',
      statusLabel: '待整改',
    })[0]!;
    const nextLine = createHazardInspectionLine([firstLine], {
      hazardDescription: '临边无警示',
    });
    expect(nextLine).toMatchObject({
      hazardDescription: '临边无警示',
      sequence: '2',
      status: '待整改',
      validated: false,
    });

    const payload = serializeHazardInspectionPayload(
      { statusLabel: '待整改' },
      [firstLine, nextLine],
    );
    expect(payload.hazardLines).toEqual([firstLine, nextLine]);
    expect(payload.checkResult).toBe('有隐患');
    expect(payload.hazardDescription).toBe('洞口未防护');
    expect(payload.rectificationMeasures).toBe('安装防护');
    expect(payload.status).toBe('待整改');
  });

  it('normalizes team check template items into detail rows and defaults results', () => {
    const lines = normalizeTeamCheckInspectionLines([
      {
        checkItem: '检查机械设备是否处于良好状态',
        defaultCheckResult: '无隐患',
        defaultFollowUpPlan: '持续跟进',
        defaultRectificationDescription: '保持完好',
        requireImage: true,
        requireVideo: false,
        riskType: '机械伤害',
        sortOrder: 1,
      },
      {
        checkItem: '检查临边洞口防护',
        checkResult: '',
        riskType: '高处坠落',
      },
    ]);

    expect(lines).toEqual([
      expect.objectContaining({
        checkItem: '检查机械设备是否处于良好状态',
        checkResult: '无隐患',
        followUpPlan: '持续跟进',
        rectificationDescription: '保持完好',
        riskType: '机械伤害',
        uploadDescription: '需上传图片',
      }),
      expect.objectContaining({
        checkItem: '检查临边洞口防护',
        checkResult: '无隐患',
        riskType: '高处坠落',
      }),
    ]);
    expect(validateTeamCheckInspectionResults(lines)).toEqual({
      message: '',
      valid: true,
    });
    expect(
      validateTeamCheckInspectionResults([
        { ...lines[0]!, checkResult: '   ' },
      ]),
    ).toEqual({
      message: '请填写第 1 项检查结果',
      valid: false,
    });
  });

  it('serializes team check item rows and omits risk column for post-shift inspection', () => {
    const lines = normalizeTeamCheckInspectionLines([
      {
        checkItem: '清点工具材料并确认交班状态',
        checkResult: '无隐患',
        riskType: '',
      },
    ]);
    const payload = serializeTeamCheckInspectionPayload(
      { statusLabel: '待检查' },
      lines,
    );

    expect(payload.checkItems).toEqual(lines);
    expect(getTeamCheckInspectionStage('PinganPreShiftInspection')).toBe(
      'PRE_SHIFT_INSPECTION',
    );
    expect(getTeamCheckInspectionStage('PinganMidShiftInspection')).toBe(
      'MID_SHIFT_INSPECTION',
    );
    expect(getTeamCheckInspectionStage('PinganPostShiftInspection')).toBe(
      'POST_SHIFT_INSPECTION',
    );
    expect(getTeamCheckInspectionStage('PinganKeySites')).toBe('KEY_SITES');
    expect(
      getTeamCheckInspectionColumns('PinganPostShiftInspection').map(
        (column) => column.title,
      ),
    ).toEqual([
      '检查项',
      '检查结果',
      '隐患描述',
      '跟进方案',
      '上传说明',
      '整改情况',
    ]);
  });

  it('preserves local three-check row edits before attachment actions refresh detail', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );
    const uploadHandler = page.slice(
      page.indexOf('async function handleAttachmentChange'),
      page.indexOf('function handleAnyAttachmentChange'),
    );
    const fieldUploadHandler = page.slice(
      page.indexOf('async function handleDetailAttachmentFieldChange'),
      page.indexOf('function attachmentPreviewUrl'),
    );
    const deleteHandler = page.slice(
      page.indexOf('async function handleDeleteAttachment'),
      page.indexOf('async function handleAttachmentChange'),
    );

    expect(page).toContain('async function persistPendingTeamCheckInspectionEdits');
    expect(uploadHandler).toContain('await persistPendingTeamCheckInspectionEdits()');
    expect(fieldUploadHandler).toContain(
      'await persistPendingTeamCheckInspectionEdits()',
    );
    expect(deleteHandler).toContain('await persistPendingTeamCheckInspectionEdits()');
  });

  it('renders top-right delete controls on attachment previews', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );
    const legacyMeetingPreview = page.match(
      /v-for="attachment in currentDetail\.attachments"[\s\S]*?<\/div>\s*<\/div>\s*<div v-else class="attachment-empty">暂无附件<\/div>/,
    )?.[0];
    const threeCheckPreview = page.match(
      /v-for="attachment in currentThreeCheckDetail\.attachments"[\s\S]*?<\/div>\s*<\/div>\s*<div v-else class="attachment-empty">暂无附件<\/div>/,
    )?.[0];

    expect(legacyMeetingPreview).toContain('attachment-delete-button');
    expect(legacyMeetingPreview).toContain('aria-label="删除附件"');
    expect(legacyMeetingPreview).toContain('@click.stop="handleDeleteAttachment(attachment)"');
    expect(threeCheckPreview).toContain('attachment-delete-button');
    expect(threeCheckPreview).toContain('aria-label="删除附件"');
    expect(threeCheckPreview).toContain('@click.stop="handleDeleteAttachment(attachment)"');
    expect(page).toMatch(
      /\.attachment-delete-button\s*\{[\s\S]*position:\s*absolute;[\s\S]*right:\s*8px;[\s\S]*top:\s*8px;/,
    );
  });

  it('places linked team check item table above attachments in inspection detail', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );
    const tableIndex = page.indexOf('class="team-check-items"');
    const attachmentIndex = page.indexOf("'detail-attachment-row'");

    expect(page).toContain('getTeamCheckTemplateResolveApi');
    expect(page).toContain('resolveTeamCheckInspectionLinesFromTemplate');
    expect(page).toContain('teamCheckInspectionSnapshotLines');
    expect(page).toContain('teamCheckInspectionTableTitle');
    expect(page).toContain("PinganPreShiftInspection: '班前检查项'");
    expect(tableIndex).toBeGreaterThan(-1);
    expect(attachmentIndex).toBeGreaterThan(-1);
    expect(tableIndex).toBeLessThan(attachmentIndex);
  });

  it('filters hazard inspection lines, scopes view storage keys, and exports visible CSV columns', () => {
    const lines = [
      createHazardInspectionLine([], {
        hazardDescription: '消防通道堵塞',
        rectificationMeasures: '清理通道',
        rectificationResponsiblePerson: '李四',
        status: '待整改',
      }),
      createHazardInspectionLine([], {
        hazardDescription: '配电箱缺标识',
        rectificationMeasures: '补充标识',
        rectificationResponsiblePerson: '王五',
        sequence: '2',
        status: '已整改',
      }),
    ];

    expect(filterHazardInspectionLines(lines, '王五')).toEqual([lines[1]!]);
    expect(filterHazardInspectionLines(lines, '通道')).toEqual([lines[0]!]);
    expect(getHazardInspectionViewStorageKey('PinganSafetyCheck')).not.toBe(
      getHazardInspectionViewStorageKey('PinganQuickShot'),
    );
    expect(
      exportHazardInspectionLinesCsv(lines, [
        { dataIndex: 'sequence', title: '序号', width: 64 },
        { dataIndex: 'hazardDescription', title: '隐患描述', width: 190 },
        { dataIndex: 'status', title: '状态', width: 130 },
      ]),
    ).toBe('序号,隐患描述,状态\r\n1,消防通道堵塞,待整改\r\n2,配电箱缺标识,已整改');
  });

  it('keeps quick shot and curtain wall media fields in the bottom attachment area', () => {
    const mediaBottomRoutes = [
      {
        mediaTitles: ['隐患图片/视频'],
        routeName: 'PinganQuickShot',
        title: '随手拍',
      },
      {
        mediaTitles: ['图片一', '图片二', '图片三'],
        routeName: 'PinganCurtainWallPenalty',
        title: '幕墙处罚管理',
      },
      {
        mediaTitles: ['图片上传', '视频上传'],
        routeName: 'PinganCurtainWallRoutineCheck',
        title: '幕墙日周月检',
      },
    ];

    for (const { mediaTitles, routeName, title } of mediaBottomRoutes) {
      const columns = getThreeCheckDetailDisplayColumns(
        routeName,
        resolvePreShiftMeetingRuntime(routeName, title).columns,
      );
      const groupedTitles = getThreeCheckDetailFieldGroups(
        routeName,
        columns,
      ).flatMap((group) => group.columns.map((column) => column.title));

      for (const mediaTitle of mediaTitles) {
        expect(groupedTitles).not.toContain(mediaTitle);
      }
    }

    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );
    expect(page).toContain('bottomDetailAttachmentRouteNames');
    expect(page).toContain('showBottomDetailAttachmentRow');
    expect(page).toContain('detail-attachment-row--bottom');
  });

  it('locks create form organization fields and re-applies scope before creation', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );
    const createModal = sourceBetween(
      page,
      'function openCreateModal()',
      'function resolveThreeCheckRecordBusinessDate()',
    );
    const createHandler = sourceBetween(
      page,
      'async function handleCreateMeeting()',
      'function seedThreeCheckDetailEditForm()',
    );

    expect(createModal).toContain('resolveCurrentOwnerUserId()');
    expect(createModal).not.toContain('ownerSelectOptions.value[1]');
    expect(createHandler).toMatch(
      /enforceUserOrganizationScope\(\);[\s\S]*const companyId = createForm\.companyId;/,
    );
    expect(page).toContain(
      ':disabled="companySelectOptions.length === 0 || isOrganizationFieldLocked(\'company\')"',
    );
    expect(page).toContain(
      ':disabled="departmentSelectOptions.length === 0 || isOrganizationFieldLocked(\'department\')"',
    );
    expect(page).toContain(
      ':disabled="teamSelectOptions.length === 0 || isOrganizationFieldLocked(\'team\')"',
    );
    expect(page).toContain(
      ':disabled="activeCompanySelectOptions.length === 0 || isOrganizationFieldLocked(\'company\')"',
    );
  });

  it('auto-refreshes visible compatibility lists so other accounts see submitted changes', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );
    const lifecycleScript = sourceBetween(
      page,
      'onBeforeUnmount(() => {',
      'watch(\n  () => route.name,',
    );

    expect(page).toContain('createSharedThreeCheckWorkbenchState');
    expect(page).toContain('THREE_CHECK_LIST_AUTO_REFRESH_INTERVAL_MS');
    expect(page).toContain('function startListAutoRefresh()');
    expect(page).toContain('function stopListAutoRefresh()');
    expect(page).toContain('function handleDocumentVisibilityChange()');
    expect(lifecycleScript).toContain('startListAutoRefresh();');
    expect(lifecycleScript).toContain('stopListAutoRefresh();');
    expect(lifecycleScript).toContain(
      "document.addEventListener('visibilitychange', handleDocumentVisibilityChange);",
    );
    expect(lifecycleScript).toContain(
      "document.removeEventListener('visibilitychange', handleDocumentVisibilityChange);",
    );
  });

  it('keeps curtain wall only modules pinned to Guangsheng Curtain Wall without data map', () => {
    expect(isThreeCheckDataMapEnabled('PinganSafetyCheck')).toBe(true);
    expect(isThreeCheckDataMapEnabled('PinganCurtainWallPenalty')).toBe(false);
    expect(isThreeCheckDataMapEnabled('PinganCurtainWallRoutineCheck')).toBe(
      false,
    );

    expect(getThreeCheckFixedCompanyName('PinganCurtainWallPenalty')).toBe(
      'Demo Works Company',
    );
    expect(getThreeCheckFixedCompanyName('PinganCurtainWallRoutineCheck')).toBe(
      'Demo Works Company',
    );
    expect(getThreeCheckFixedCompanyName('PinganSafetyCheck')).toBeUndefined();
  });

  it('maps local status labels to filter status codes per module', () => {
    expect(getThreeCheckStatusFilterOptions('PinganPreShiftInspection')).toEqual([
      { label: '全部', value: 'all' },
      { label: '已检查', value: 'OPENED' },
      { label: '待检查', value: 'DRAFT' },
    ]);
    expect(getThreeCheckStatusFilterOptions('PinganKeySites')).toEqual([
      { label: '全部', value: 'all' },
      { label: '已检查', value: 'OPENED' },
      { label: '待检查', value: 'DRAFT' },
    ]);
    expect(getThreeCheckStatusFilterOptions('PinganSafetyCheck')).toEqual([
      { label: '全部', value: 'all' },
      { label: '待检查', value: 'DRAFT' },
      { label: '已检查', value: 'OPENED' },
    ]);
    expect(getThreeCheckStatusFilterOptions('PinganHazardRectification')).toEqual([
      { label: '全部', value: 'all' },
    ]);
    expect(getThreeCheckStatusFilterOptions('PinganQuickShot')).toEqual([
      { label: '全部', value: 'all' },
      { label: '待审核', value: 'PENDING_REVIEW' },
      { label: '已审核', value: 'REVIEWED' },
    ]);
    expect(
      getThreeCheckStatusFilterOptions('PinganCurtainWallPenalty'),
    ).toEqual([
      { label: '全部', value: 'all' },
      { label: '待批准', value: 'DRAFT' },
      { label: '已生效（未读）', value: 'OPENED' },
      { label: '已生效已读', value: 'ARCHIVED' },
    ]);
    expect(
      getThreeCheckStatusFilterOptions('PinganCurtainWallRoutineCheck'),
    ).toEqual([
      { label: '全部', value: 'all' },
      { label: '已检查', value: 'OPENED' },
      { label: '待检查', value: 'DRAFT' },
    ]);
  });

  it('maps one-shift-three-check status boxes to the requested colors', () => {
    expect(isThreeCheckStatusBoxField('status')).toBe(true);
    expect(isThreeCheckStatusBoxField('dispatchStatus')).toBe(true);
    expect(isThreeCheckStatusBoxField(['dispatchStatus'] as never)).toBe(true);
    expect(isThreeCheckStatusBoxField({ title: '派班状态' })).toBe(true);
    expect(isThreeCheckStatusBoxField('company')).toBe(false);

    expect(normalizeThreeCheckStatusBoxLabel('不生效')).toBe('未生效');
    expect(normalizeThreeCheckStatusBoxLabel('带开会议')).toBe('待开会议');

    expect(getThreeCheckStatusBadgeTone('已检查')).toBe('green');
    expect(getThreeCheckStatusBadgeTone('已开会议')).toBe('green');
    expect(getThreeCheckStatusBadgeTone('已审核')).toBe('green');
    expect(getThreeCheckStatusBadgeTone('已整改')).toBe('green');
    expect(getThreeCheckStatusBadgeTone('已验收')).toBe('green');
    expect(getThreeCheckStatusBadgeTone('生效')).toBe('green');
    expect(getThreeCheckStatusBadgeTone('待审核')).toBe('orange');
    expect(getThreeCheckStatusBadgeTone('待检查')).toBe('orange');
    expect(getThreeCheckStatusBadgeTone('待开会议')).toBe('orange');
    expect(getThreeCheckStatusBadgeTone('待验收')).toBe('orange');
    expect(getThreeCheckStatusBadgeTone('未生效')).toBe('orange');
    expect(getThreeCheckStatusBadgeTone('不生效')).toBe('orange');
    expect(getThreeCheckStatusBadgeTone('已驳回')).toBe('red');
    expect(getThreeCheckStatusBadgeTone('待整改')).toBe('red');
  });

  it('wires quick shot review actions and unified rectification document flow fallback', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/pre-shift-meeting/index.vue'),
      'utf8',
    );
    const api = readFileSync(
      appSourcePath('src/api/pingan/three-check-record.ts'),
      'utf8',
    );

    expect(api).toContain('WorkflowAction');
    expect(api).toContain('workflowActionThreeCheckRecordApi');
    expect(api).toContain('payload?: Record<string, unknown>');
    expect(page).toContain('handleQuickShotWorkflowAction');
    expect(page).toContain('quickShotWorkflowActions');
    expect(page).toContain('审核通过');
    expect(page).toContain('驳回');
    expect(page).toContain('quickShotDocumentFlowStepLabels');
    expect(page).toContain("'待审核'");
    expect(page).toContain("'已审核'");
    expect(page).toContain("'已驳回'");
    expect(page).toContain('quickShotWorkflowActionForm');
    expect(page).toContain('quickShotWorkflowFieldGroups');
    expect(page).toContain('quickShotDocumentFlowNodes');
    expect(page).toContain('quickShotLinkedRectificationOrderNode');
    expect(page).toContain('quickShotDocumentFlowSnapshotPayload');
    expect(page).toContain('quickShotDocumentFlowNodeFields');
    expect(page).toContain('selectDocumentFlowStep');
    expect(page).toContain('documentFlowStepNodeId');
    expect(page).toContain('无历史快照，显示当前信息');
    expect(page).toContain("routeName === 'PinganQuickShot'");
    expect(page).toContain("return '待审核'");

    const actionLabels = sourceBetween(
      page,
      'function documentFlowActionLabel(action?: string)',
      'function documentFlowStepNodeId',
    );
    expect(actionLabels).toContain("ISSUE_RECTIFICATION: '下发整改'");
    expect(actionLabels).toContain("MARK_RECTIFIED: '整改完成'");
    expect(actionLabels).toContain("REQUEST_ACCEPTANCE: '提交验收'");
  });

  it('recognizes upload columns and missing upload labels', () => {
    expect(getThreeCheckAttachmentColumnKind('imageCheck')).toBe('IMAGE');
    expect(getThreeCheckAttachmentColumnKind('hazardImage')).toBe('IMAGE');
    expect(getThreeCheckAttachmentColumnKind('imageOne')).toBe('IMAGE');
    expect(getThreeCheckAttachmentColumnKind('imageTwo')).toBe('IMAGE');
    expect(getThreeCheckAttachmentColumnKind('imageThree')).toBe('IMAGE');
    expect(getThreeCheckAttachmentColumnKind('imageUpload')).toBe('IMAGE');
    expect(getThreeCheckAttachmentColumnKind('beforeRectificationPhoto')).toBe(
      'IMAGE',
    );
    expect(getThreeCheckAttachmentColumnKind('afterRectificationPhoto')).toBe(
      'IMAGE',
    );
    expect(getThreeCheckAttachmentColumnKind('videoCheck')).toBe('VIDEO');
    expect(getThreeCheckAttachmentColumnKind('videoUpload')).toBe('VIDEO');
    expect(getThreeCheckAttachmentColumnKind('status')).toBeUndefined();

    expect(isThreeCheckAttachmentColumn({ dataIndex: 'imageUpload' })).toBe(true);
    expect(isThreeCheckAttachmentColumn({ title: '图片一' })).toBe(true);
    expect(isThreeCheckAttachmentColumn({ title: '图片二' })).toBe(true);
    expect(isThreeCheckAttachmentColumn({ title: '图片三' })).toBe(true);
    expect(isThreeCheckAttachmentColumn({ title: '整改前照片' })).toBe(true);
    expect(isThreeCheckAttachmentColumn({ title: '整改后照片' })).toBe(true);
    expect(isThreeCheckAttachmentColumn({ title: '视频上传' })).toBe(true);
    expect(isThreeCheckAttachmentMissing('未上传')).toBe(true);
    expect(isThreeCheckAttachmentMissing('待上传')).toBe(true);
    expect(isThreeCheckAttachmentMissing('现场照片')).toBe(false);

    expect(shouldUseThreeCheckListPreviewFallback('imageOne', 'IMAGE')).toBe(
      true,
    );
    expect(shouldUseThreeCheckListPreviewFallback('imageTwo', 'IMAGE')).toBe(
      false,
    );
    expect(shouldUseThreeCheckListPreviewFallback('imageThree', 'IMAGE')).toBe(
      false,
    );
    expect(shouldUseThreeCheckListPreviewFallback('hazardImage', 'IMAGE')).toBe(
      true,
    );
  });

  it('derives one-shift-three-check actions from backend permission codes', () => {
    expect(
      canCreateThreeCheckRecord('PinganPreShiftInspection', [
        'PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE',
      ]),
    ).toBe(true);
    expect(
      canCreateThreeCheckRecord('PinganTeamDispatch', [
        'PINGAN_TEAM_DISPATCH_CREATE',
      ]),
    ).toBe(true);
    expect(
      canCreateThreeCheckRecord('PinganCurtainWallTeamDispatch', [
        'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_CREATE',
      ]),
    ).toBe(true);
    expect(
      canCreateThreeCheckRecord('PinganPreShiftInspection', [
        'PINGAN_TEAM_DISPATCH_CREATE',
      ]),
    ).toBe(false);
    expect(
      canCreateThreeCheckRecord('PinganTeamDispatch', ['PINGAN_RISK_CONFIRM']),
    ).toBe(false);
    expect(
      canCreateThreeCheckRecord('PinganPreShiftInspection', [
        'PINGAN_THREE_CHECK_EXECUTE',
      ]),
    ).toBe(false);
    expect(
      canCreateThreeCheckRecord('PinganQuickShot', [
        'PINGAN_HAZARD_QUICK_SHOT_REPORT',
      ]),
    ).toBe(true);
    expect(
      canCreateThreeCheckRecord('PinganQuickShot', [
        'PINGAN_HAZARD_REPORT',
      ]),
    ).toBe(true);
    expect(
      canCreateThreeCheckRecord('PinganQuickShot', [
        'PINGAN_THREE_CHECK_EXECUTE',
      ]),
    ).toBe(false);
    expect(
      canCreateThreeCheckRecord('PinganSafetyCheck', [
        'PINGAN_HAZARD_SAFETY_CHECK_CREATE',
      ]),
    ).toBe(true);
    expect(
      canCreateThreeCheckRecord('PinganSafetyCheck', [
        'PINGAN_HAZARD_QUICK_SHOT_REPORT',
      ]),
    ).toBe(false);
    expect(
      canCreateThreeCheckRecord('PinganHazardRectification', [
        'PINGAN_HAZARD_RECTIFICATION_CREATE',
      ]),
    ).toBe(true);
    expect(
      canCreateThreeCheckRecord('PinganHazardRectification', [
        'PINGAN_HAZARD_RECTIFICATION_REPORT',
      ]),
    ).toBe(false);
    expect(
      canSubmitThreeCheckRecord('PinganHazardRectification', [
        'PINGAN_HAZARD_RECTIFICATION_REPORT',
      ]),
    ).toBe(true);
    expect(
      canSubmitThreeCheckRecord('PinganHazardRectification', [
        'PINGAN_HAZARD_RECTIFICATION_CREATE',
      ]),
    ).toBe(false);
    expect(
      canCreateThreeCheckRecord('PinganPointsFlow', [
        'PINGAN_POINTS_ENTRY',
        'PINGAN_POINTS_VIEW',
      ]),
    ).toBe(false);
    expect(
      canCreateThreeCheckRecord('PinganPointsFlow', [
        'PINGAN_POINTS_FLOW_CREATE',
      ]),
    ).toBe(true);
    expect(
      canCreateThreeCheckRecord('PinganPointsFlow', [
        'PINGAN_POINTS_MANAGE',
      ]),
    ).toBe(true);

    expect(
      canOpenThreeCheckCreateDialog('PinganTeamDispatch', 'LOCAL_TABLE', [
        'PINGAN_TEAM_DISPATCH_CREATE',
      ]),
    ).toBe(true);
    expect(
      canOpenThreeCheckCreateDialog('PinganPreShiftInspection', 'API', [
        'PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE',
      ]),
    ).toBe(true);
    expect(
      canOpenThreeCheckCreateDialog('PinganPreShiftInspection', 'API', [
        'PINGAN_TEAM_DISPATCH_CREATE',
      ]),
    ).toBe(false);
    expect(
      canOpenThreeCheckCreateDialog('PinganTeamDispatch', 'DISABLED', [
        'PINGAN_TEAM_DISPATCH_CREATE',
      ]),
    ).toBe(false);
    expect(
      canOpenThreeCheckCreateDialog('PinganPointsFlow', 'API', [
        'PINGAN_POINTS_ENTRY',
        'PINGAN_POINTS_VIEW',
      ]),
    ).toBe(false);
    expect(
      canOpenThreeCheckCreateDialog('PinganPointsFlow', 'API', [
        'PINGAN_POINTS_FLOW_CREATE',
      ]),
    ).toBe(true);

    expect(
      canUploadThreeCheckAttachment('PinganPreShiftInspection', [
        'PINGAN_ONE_SHIFT_THREE_CHECKS_UPDATE',
      ]),
    ).toBe(true);
    expect(
      canUploadThreeCheckAttachment('PinganPreShiftInspection', [
        'PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE',
      ]),
    ).toBe(false);
    expect(
      canUploadThreeCheckAttachment('PinganQuickShot', [
        'PINGAN_HAZARD_QUICK_SHOT_REPORT',
      ]),
    ).toBe(true);
    expect(
      canUploadThreeCheckAttachment('PinganPointsFlow', [
        'PINGAN_POINTS_ENTRY',
        'PINGAN_POINTS_VIEW',
      ]),
    ).toBe(false);
    expect(
      canUploadThreeCheckAttachment('PinganPointsFlow', [
        'PINGAN_POINTS_FLOW_CREATE',
      ]),
    ).toBe(true);
    expect(
      canViewThreeCheckRecord('PinganTeamDispatch', [
        'PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE',
      ]),
    ).toBe(false);
    expect(
      canViewThreeCheckRecord('PinganTeamDispatch', [
        'PINGAN_TEAM_DISPATCH_VIEW',
      ]),
    ).toBe(true);
    expect(
      canViewThreeCheckRecord('PinganCurtainWallTeamDispatch', [
        'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VIEW',
      ]),
    ).toBe(true);
    expect(
      canViewThreeCheckRecord('PinganHazardRectification', [
        'PINGAN_HAZARD_RECTIFICATION_VIEW',
      ]),
    ).toBe(true);
    expect(
      canViewThreeCheckRecord('PinganQuickShot', ['PINGAN_HAZARD_QUICK_SHOT_VIEW']),
    ).toBe(true);

    expect(
      canDeleteThreeCheckRecord('PinganSafetyCheck', [
        'PINGAN_HAZARD_SAFETY_CHECK_DELETE',
      ]),
    ).toBe(true);
    expect(
      canDeleteThreeCheckRecord('PinganSafetyCheck', [
        'PINGAN_HAZARD_SAFETY_CHECK_CREATE',
      ]),
    ).toBe(false);
    expect(
      canDeleteThreeCheckRecord('PinganPointsFlow', [
        'PINGAN_POINTS_FLOW_DELETE',
      ]),
    ).toBe(true);
    expect(
      canDeleteThreeCheckRecord('PinganPointsFlow', [
        'PINGAN_POINTS_FLOW_CREATE',
      ]),
    ).toBe(false);
    expect(
      canExportThreeCheckRecords('PinganPointsFlow', [
        'PINGAN_POINTS_FLOW_DELETE',
      ]),
    ).toBe(false);
    expect(
      canExportThreeCheckRecords('PinganQuickShot', [
        'PINGAN_HAZARD_QUICK_SHOT_EXPORT',
      ]),
    ).toBe(true);
    expect(
      canExportThreeCheckRecords('PinganQuickShot', [
        'PINGAN_HAZARD_QUICK_SHOT_REPORT',
      ]),
    ).toBe(false);
    expect(
      canSubmitThreeCheckRecord('PinganPreShiftInspection', [
        'PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT',
      ]),
    ).toBe(true);
    expect(
      canSubmitThreeCheckRecord('PinganPreShiftInspection', [
        'PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE',
      ]),
    ).toBe(false);
    expect(
      canDeleteThreeCheckRecord('PinganPreShiftInspection', [
        'PINGAN_ONE_SHIFT_THREE_CHECKS_DELETE',
      ]),
    ).toBe(true);
    expect(
      canExportThreeCheckRecords('PinganPreShiftInspection', [
        'PINGAN_ONE_SHIFT_THREE_CHECKS_EXPORT',
      ]),
    ).toBe(true);
    expect(
      canReviewQuickShotRecord(['PINGAN_HAZARD_QUICK_SHOT_REVIEW']),
    ).toBe(true);
    expect(
      canRectifyHazardRectificationOrder([
        'PINGAN_HAZARD_RECTIFICATION_RECTIFY',
      ]),
    ).toBe(true);
    expect(
      canAcceptHazardRectificationOrder([
        'PINGAN_HAZARD_RECTIFICATION_ACCEPT',
      ]),
    ).toBe(true);
    expect(
      canVoidHazardRectificationOrder(['PINGAN_HAZARD_RECTIFICATION_VOID']),
    ).toBe(true);

    expect(
      canWithdrawThreeCheckRecord([
        'PINGAN_ONE_SHIFT_THREE_CHECKS_VOID',
      ], 'PinganPreShiftInspection'),
    ).toBe(true);
    expect(
      canWithdrawThreeCheckRecord(
        ['PINGAN_HAZARD_QUICK_SHOT_VOID'],
        'PinganQuickShot',
      ),
    ).toBe(true);
    expect(
      canWithdrawThreeCheckRecord(
        ['PINGAN_THREE_CHECK_WITHDRAW'],
        'PinganQuickShot',
      ),
    ).toBe(false);
    expect(
      canWithdrawThreeCheckRecord([
        'PINGAN_THREE_CHECK_WITHDRAW',
      ], 'PinganPreShiftInspection'),
    ).toBe(false);
    expect(
      canRemindThreeCheckRecord(
        ['PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND'],
        'PinganPreShiftInspection',
      ),
    ).toBe(true);
    expect(
      canRemindThreeCheckRecord(
        ['PINGAN_THREE_CHECK_REMIND'],
        'PinganPreShiftInspection',
      ),
    ).toBe(false);
  });

  it('maps pre-shift meeting create fields into a generic record payload', () => {
    expect(
      createPreShiftMeetingRecordPayload({
        attendeeNames: ['Demo Harbor班长', '幕墙安全员'],
        meetingContent: '班前风险交底',
        statusLabel: '待开会议',
      }),
    ).toEqual({
      attendees: ['Demo Harbor班长', '幕墙安全员'],
      attendeesText: 'Demo Harbor班长、幕墙安全员',
      meetingContent: '班前风险交底',
      statusLabel: '待开会议',
    });
  });

  it('formats generic payload array values for table display', () => {
    expect(
      formatThreeCheckPayloadDisplayValue(['Demo Harbor班长', '幕墙安全员']),
    ).toBe('Demo Harbor班长、幕墙安全员');
    expect(formatThreeCheckPayloadDisplayValue('Demo Harbor班长')).toBe('Demo Harbor班长');
    expect(formatThreeCheckPayloadDisplayValue(undefined)).toBe('');
  });
});
