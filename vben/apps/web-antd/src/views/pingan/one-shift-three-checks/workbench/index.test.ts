import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

import {
  createOneShiftThreeCheckDetailEditForm,
  createOneShiftThreeCheckRecordUpdatePayloadFromDetail,
  createPreShiftMeetingSafetyConfirmLine,
  createTeamCheckInspectionLine,
  getOneShiftThreeCheckDetailActions,
  getOneShiftThreeCheckDetailDisplayColumns,
  getOneShiftThreeCheckDetailFieldGroups,
  getPreShiftMeetingSafetyConfirmColumns,
  getThreeCheckStatusFilterOptions,
  getTeamCheckInspectionColumns,
  getOneShiftThreeCheckTableDisplayColumns,
  getOneShiftVisibleOrganizationFilterKeys,
  isOneShiftOrganizationFieldLocked,
  isOneShiftTeamMemberRole,
  oneShiftThreeCheckRouteNames,
  resolveOneShiftThreeCheckRuntime,
  serializeTeamCheckInspectionPayload,
  shouldShowOneShiftDataMap,
  validatePreShiftMeetingSafetyConfirmResults,
  validatePreShiftMeetingMediaChecks,
  validateTeamCheckInspectionResults,
} from './module-config';

const workbenchSource = readFileSync(
  resolve(dirname(fileURLToPath(import.meta.url)), 'index.vue'),
  'utf8',
);
const moduleConfigSource = readFileSync(
  resolve(dirname(fileURLToPath(import.meta.url)), 'module-config.ts'),
  'utf8',
);

function sourceBetween(start: string, end: string) {
  const startIndex = workbenchSource.indexOf(start);
  const endIndex = workbenchSource.indexOf(end, startIndex);
  expect(startIndex).toBeGreaterThanOrEqual(0);
  expect(endIndex).toBeGreaterThan(startIndex);
  return workbenchSource.slice(startIndex, endIndex);
}

describe('one-shift-three-checks workbench module config', () => {
  it('adds a backend-derived overdue filter to the four aligned modules', () => {
    for (const routeName of [
      'PreShiftMeeting',
      'PinganPreShiftInspection',
      'PinganMidShiftInspection',
      'PinganPostShiftInspection',
    ]) {
      expect(getThreeCheckStatusFilterOptions(routeName)).toContainEqual({
        label: '已逾期',
        value: 'overdue',
      });
    }
    expect(getThreeCheckStatusFilterOptions('PinganKeySites')).not.toContainEqual({
      label: '已逾期',
      value: 'overdue',
    });
  });

  it('declares only the eight one-shift-three-check modules', () => {
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

  it('keeps hazard inspection routes out of the one-shift runtime', () => {
    const runtime = resolveOneShiftThreeCheckRuntime(
      'PinganHazardRectification',
      '隐患整改',
    );

    expect(runtime).toMatchObject({
      apiReady: false,
      createMode: 'DISABLED',
      routeName: 'PinganHazardRectification',
      title: '隐患整改',
    });
    expect(runtime.moduleKey).toBeUndefined();
  });

  it('keeps one-shift detail fields and update payload behavior intact', () => {
    const runtime = resolveOneShiftThreeCheckRuntime(
      'PinganPreShiftInspection',
      '班前检查',
    );
    const detail = {
      businessDate: '2026-05-18',
      canSubmit: true,
      companyId: 4,
      departmentId: 101_109,
      id: '3001',
      imageCheck: '未上传',
      owner: '湖贝班长',
      ownerUserId: 2,
      payload: {
        owner: '湖贝班长',
        statusLabel: '待检查',
      },
      status: 'DRAFT',
      statusLabel: '待检查',
      teamId: 1_011_001,
      version: 3,
      videoCheck: '未上传',
    };

    expect(
      getOneShiftThreeCheckTableDisplayColumns(
        runtime.routeName,
        runtime.columns,
      ).some((column) => column.dataIndex === 'company'),
    ).toBe(false);
    expect(
      createOneShiftThreeCheckDetailEditForm(detail, runtime.columns),
    ).toMatchObject({
      date: '2026-05-18',
      imageCheck: '未上传',
      owner: '湖贝班长',
      status: '待检查',
    });
    expect(
      createOneShiftThreeCheckRecordUpdatePayloadFromDetail(detail, {
        imageCheck: '现场照片',
        owner: '湖贝班长',
      }),
    ).toEqual({
      businessDate: '2026-05-18',
      companyId: 4,
      departmentId: 101_109,
      ownerUserId: 2,
      payload: {
        imageCheck: '现场照片',
        owner: '湖贝班长',
        statusLabel: '待检查',
      },
      status: '待检查',
      teamId: 1_011_001,
      version: 3,
    });
  });

  it('applies role-based data map and organization filter visibility', () => {
    expect(shouldShowOneShiftDataMap(['ADMIN'])).toBe(true);
    expect(shouldShowOneShiftDataMap(['GROUP_LEADER'])).toBe(true);
    expect(shouldShowOneShiftDataMap(['CURTAIN_WALL_LEADER'])).toBe(true);
    expect(shouldShowOneShiftDataMap(['ENTERPRISE_LEADER'])).toBe(true);
    expect(shouldShowOneShiftDataMap(['COMPANY_LEADER'])).toBe(true);
    expect(shouldShowOneShiftDataMap(['DEPARTMENT_MANAGER'])).toBe(false);
    expect(shouldShowOneShiftDataMap(['WORKSHOP_DIRECTOR'])).toBe(false);
    expect(shouldShowOneShiftDataMap(['TEAM_MEMBER'])).toBe(false);
    expect(shouldShowOneShiftDataMap(['CURTAIN_WALL_MEMBER'])).toBe(false);

    expect(isOneShiftTeamMemberRole(['TEAM_MEMBER'])).toBe(true);
    expect(isOneShiftTeamMemberRole(['CURTAIN_WALL_MEMBER'])).toBe(true);
    expect(getOneShiftVisibleOrganizationFilterKeys(['TEAM_MEMBER'])).toEqual([]);
    expect(getOneShiftVisibleOrganizationFilterKeys(['DEPARTMENT_MANAGER'])).toEqual([
      'company',
      'department',
      'team',
    ]);
    expect(isOneShiftOrganizationFieldLocked(['TEAM_MEMBER'], 'team')).toBe(true);
    expect(
      isOneShiftOrganizationFieldLocked(['DEPARTMENT_MANAGER'], 'department'),
    ).toBe(true);
    expect(isOneShiftOrganizationFieldLocked(['DEPARTMENT_MANAGER'], 'team')).toBe(false);
    expect(isOneShiftOrganizationFieldLocked(['WORKSHOP_DIRECTOR'], 'team')).toBe(false);
    expect(isOneShiftOrganizationFieldLocked(['COMPANY_LEADER'], 'company')).toBe(true);
    expect(isOneShiftOrganizationFieldLocked(['COMPANY_LEADER'], 'department')).toBe(false);
    expect(isOneShiftOrganizationFieldLocked(['ENTERPRISE_LEADER'], 'company')).toBe(true);
    expect(isOneShiftOrganizationFieldLocked(['ENTERPRISE_LEADER'], 'team')).toBe(false);
  });

  it('removes the table operation column for one-shift member roles', () => {
    const runtime = resolveOneShiftThreeCheckRuntime(
      'PinganPreShiftInspection',
      '班前检查',
    );

    expect(
      getOneShiftThreeCheckTableDisplayColumns(
        runtime.routeName,
        runtime.columns,
        { roles: ['TEAM_MEMBER'] },
      ).some((column) => column.dataIndex === 'actions'),
    ).toBe(false);
    expect(
      getOneShiftThreeCheckTableDisplayColumns(
        runtime.routeName,
        runtime.columns,
        { roles: ['DEPARTMENT_MANAGER'] },
      ).some((column) => column.dataIndex === 'actions'),
    ).toBe(true);
  });

  it('keeps pre-shift meeting detail edits in sync with main table display fields', () => {
    const runtime = resolveOneShiftThreeCheckRuntime(
      'PreShiftMeeting',
      '班前会',
    );
    const detail = {
      businessDate: '2026-05-18',
      canSubmit: true,
      companyId: 4,
      departmentId: 101_109,
      id: '3002',
      imageCheck: '未上传',
      owner: '旧负责人',
      ownerUserId: 2,
      payload: {
        attendees: ['旧参会人'],
        attendeesText: '旧参会人',
        owner: '新负责人',
        statusLabel: '待开会议',
      },
      status: 'DRAFT',
      statusLabel: '待开会议',
      teamId: 1_011_001,
      version: 1,
      videoCheck: '未上传',
    };

    expect(
      createOneShiftThreeCheckDetailEditForm(detail, runtime.columns),
    ).toMatchObject({
      attendees: '旧参会人',
      owner: '新负责人',
    });
    expect(
      createOneShiftThreeCheckRecordUpdatePayloadFromDetail(detail, {
        attendees: ['张三', '李四'],
        owner: '新负责人',
        status: '已开会议',
      }),
    ).toMatchObject({
      payload: {
        attendees: ['张三', '李四'],
        attendeesText: '张三、李四',
        owner: '新负责人',
        statusLabel: '已开会议',
      },
      status: '已开会议',
    });
  });

  it('provides the hazard-inspection style detail shell for one-shift records', () => {
    const runtime = resolveOneShiftThreeCheckRuntime(
      'PinganPreShiftSafetyActivity',
      '班前安全活动',
    );
    const detailColumns = getOneShiftThreeCheckDetailDisplayColumns(
      runtime.routeName,
      runtime.columns,
    );
    const fieldGroups = getOneShiftThreeCheckDetailFieldGroups(
      runtime.routeName,
      detailColumns,
    );

    expect(getOneShiftThreeCheckDetailActions().map((action) => action.key)).toEqual([
      'refresh',
      'print',
      'documentFlow',
      'attachment',
      'changeHistory',
      'edit',
    ]);
    expect(detailColumns.some((column) => column.dataIndex === 'actions')).toBe(
      false,
    );
    expect(detailColumns.some((column) => column.dataIndex === 'company')).toBe(
      true,
    );
    expect(fieldGroups).toEqual([
      {
        columns: detailColumns,
        key: 'default',
        title: '明细信息',
      },
    ]);
  });

  it('uses separate start and end date filters in the one-shift workbench toolbar', () => {
    const workbenchToolbar = sourceBetween(
      '<div class="workbench__bar">',
      '<div\n          ref="tablePanelRef"',
    );

    expect(workbenchToolbar).toContain('<span>日期（起）</span>');
    expect(workbenchToolbar).toContain('v-model:value="filters.dateStart"');
    expect(workbenchToolbar).toContain('<span>日期（止）</span>');
    expect(workbenchToolbar).toContain('v-model:value="filters.dateEnd"');
    expect(workbenchToolbar).toContain('placeholder="点击选择"');
    expect(workbenchToolbar).not.toContain('<RangePicker');
    expect(workbenchSource).not.toMatch(/import\s*\{[\s\S]*RangePicker[\s\S]*\}\s*from 'ant-design-vue'/);
  });

  it('renders status, search and reset as one shared inline filter group', () => {
    expect(workbenchSource).toContain('<StatusFilterActions');
    expect(workbenchSource).toContain(':options="activeStatusOptions"');
    expect(workbenchSource).toContain(
      '@update:model-value="setWorkbenchStatusFilter"',
    );
    expect(workbenchSource).not.toContain('one-shift-status-tabs');
  });

  it('adds only delete-oriented batch controls to the real one-shift workbench table', () => {
    const tablePanel = sourceBetween(
      '<div class="table-panel__toolbar">',
      '</Table>',
    );

    expect(workbenchSource).toContain('batchThreeCheckRecordsApi');
    expect(workbenchSource).toContain('deleteThreeCheckRecordApi');
    expect(workbenchSource).toContain('selectedRowKeys');
    expect(workbenchSource).toContain('tableRowSelection');
    expect(tablePanel).toContain(':row-selection="tableRowSelection"');
    expect(tablePanel).toContain('已选 {{ selectedRowKeys.length }} 条');
    expect(tablePanel).toContain('批量删除');
    expect(tablePanel).toContain("handleBatchAction('DELETE')");
    expect(tablePanel).toContain('handleDelete(record)');
    expect(tablePanel).not.toContain('清空选择');
    expect(tablePanel).not.toContain('批量提交');
    expect(tablePanel).not.toContain('批量撤回');
    expect(tablePanel).not.toContain('批量催办');
  });

  it('allows image and video uploads while creating the four linked one-shift records', () => {
    const createModalTemplate = sourceBetween(
      '<Modal\n      v-model:open="createOpen"',
      '<Modal\n      v-model:open="detailOpen"',
    );
    const createHandlerScript = sourceBetween(
      'async function handleCreateMeeting()',
      'function seedThreeCheckDetailEditForm()',
    );
    const createUploadSupportedModules = sourceBetween(
      'const createUploadSupportedModuleKeys = [',
      'const shouldShowCreateAttachmentUpload = computed',
    );
    const localCreateColumnScript = sourceBetween(
      'const localCreateColumns = computed(() =>',
      'const localDetailColumns = computed(() =>',
    );

    expect(workbenchSource).toContain('createUploadSupportedModuleKeys');
    expect(workbenchSource).toContain('createUploadFiles');
    expect(workbenchSource).toContain('isCreateAttachmentUploadColumn');
    expect(workbenchSource).toContain('handleCreateAttachmentChange');
    expect(workbenchSource).toContain('uploadCreateAttachments');
    expect(workbenchSource).toContain('createAttachmentImagePreviewUrls');
    expect(workbenchSource).not.toContain('createAttachmentFileName');
    expect(createModalTemplate).toContain('v-if="shouldShowCreateAttachmentUpload"');
    expect(createModalTemplate).toContain('附件');
    expect(createModalTemplate).not.toContain('新增上传附件');
    expect(createModalTemplate).toContain('图片打卡');
    expect(createModalTemplate).toContain('视频打卡');
    expect(createModalTemplate).toContain('v-if="createUploadFiles.IMAGE.length"');
    expect(createModalTemplate).toContain('v-if="createUploadFiles.VIDEO.length"');
    expect(createModalTemplate).not.toContain('图片上传');
    expect(createModalTemplate).not.toContain('视频上传');
    expect(createModalTemplate).toContain('v-for="previewUrl in createAttachmentImagePreviewUrls()"');
    expect(createModalTemplate).toContain('v-for="previewUrl in createAttachmentVideoPreviewUrls()"');
    expect(createModalTemplate).toContain('openVideoPreview(previewUrl)');
    expect(createModalTemplate).toContain('multiple');
    expect(createModalTemplate).toContain('accept="image/*"');
    expect(createModalTemplate).toContain('accept="video/*"');
    expect(workbenchSource).toContain('class="video-preview-player"');
    expect(workbenchSource).toContain('playsinline');
    expect(workbenchSource).toContain('可使用播放器全屏按钮横屏观看');
    expect(workbenchSource).toContain('aspect-ratio: 16 / 9');
    expect(createHandlerScript).toContain('const createdDetail = await createThreeCheckRecordApi');
    expect(createHandlerScript).toContain('await uploadCreateAttachments(createdDetail.id)');
    expect(workbenchSource).toContain('for (const file of createUploadFiles.IMAGE)');
    expect(workbenchSource).toContain('for (const file of createUploadFiles.VIDEO)');
    expect(localCreateColumnScript).toContain('!isCreateAttachmentUploadColumn(column)');
    expect(createUploadSupportedModules).toContain("'pre-shift-safety-activity'");
    expect(createUploadSupportedModules).toContain("'key-sites'");
    expect(workbenchSource).toContain("'pre-shift-meeting'");
    expect(workbenchSource).toContain("'pre-shift-inspection'");
    expect(workbenchSource).toContain("'mid-shift-inspection'");
    expect(workbenchSource).toContain("'post-shift-inspection'");
  });

  it('re-applies role organization scope before creating one-shift records', () => {
    const createHandlerScript = sourceBetween(
      'async function handleCreateMeeting()',
      'function seedThreeCheckDetailEditForm()',
    );
    const createModalScript = sourceBetween(
      'function openCreateModal()',
      'function resolveThreeCheckRecordBusinessDate()',
    );
    const rootDispatchCandidateScript = sourceBetween(
      'async function loadCreateRootDispatchCandidates()',
      'function handleCreateDispatchContextChange()',
    );

    expect(createHandlerScript).toMatch(
      /enforceUserOrganizationScope\(\);[\s\S]*const companyId = createForm\.companyId;/,
    );
    expect(createModalScript).toContain('resolveCurrentOwnerUserId()');
    expect(createModalScript).not.toContain('ownerSelectOptions.value[1]');
    expect(rootDispatchCandidateScript).toContain('canViewThreeCheckRecord');
    expect(rootDispatchCandidateScript).toContain("routeName: 'PinganTeamDispatch'");
    expect(rootDispatchCandidateScript).toContain(
      "routeName: 'PinganCurtainWallTeamDispatch'",
    );
    expect(rootDispatchCandidateScript).not.toContain(
      "['team-dispatch', 'curtain-wall-team-dispatch'].map",
    );
    expect(workbenchSource).toContain('companySelectOptions.length === 0');
    expect(workbenchSource).toContain("isOrganizationFieldLocked('company')");
    expect(workbenchSource).toContain('departmentSelectOptions.length === 0');
    expect(workbenchSource).toContain("isOrganizationFieldLocked('department')");
    expect(workbenchSource).toContain('teamSelectOptions.length === 0');
    expect(workbenchSource).toContain("isOrganizationFieldLocked('team')");
  });

  it('auto-refreshes visible one-shift lists so other accounts see submitted changes', () => {
    const lifecycleScript = sourceBetween(
      'onBeforeUnmount(() => {',
      'watch(\n  () => route.name,',
    );

    expect(workbenchSource).toContain('createSharedThreeCheckWorkbenchState');
    expect(workbenchSource).toContain(
      'THREE_CHECK_LIST_AUTO_REFRESH_INTERVAL_MS',
    );
    expect(workbenchSource).toContain('function startListAutoRefresh()');
    expect(workbenchSource).toContain('function stopListAutoRefresh()');
    expect(workbenchSource).toContain('function handleDocumentVisibilityChange()');
    expect(lifecycleScript).toContain('startListAutoRefresh();');
    expect(lifecycleScript).toContain('stopListAutoRefresh();');
    expect(lifecycleScript).toContain(
      "document.addEventListener('visibilitychange', handleDocumentVisibilityChange);",
    );
    expect(lifecycleScript).toContain(
      "document.removeEventListener('visibilitychange', handleDocumentVisibilityChange);",
    );
  });

  it('keeps status in the top meta strip and keeps media check-in fields only in the bottom attachments area', () => {
    const metaStripTemplate = sourceBetween(
      '<div class="detail-meta-strip">',
      '<div class="detail-field-sections">',
    );
    const detailGroupsScript = sourceBetween(
      'const localDetailColumns = computed(() =>',
      'const detailActions = computed(() =>',
    );
    const documentFlowDetailScript = sourceBetween(
      'function documentFlowDetailColumns(moduleKey?: string)',
      'function documentFlowFieldValue',
    );
    const bottomAttachmentImageTemplate = sourceBetween(
      'v-for="attachment in currentThreeCheckDetail.attachments"',
      '<div v-else class="attachment-empty">',
    );

    expect(metaStripTemplate).toContain('<span>{{ detailTopStatusTitle }}</span>');
    expect(detailGroupsScript).toContain('localDetailFieldColumns');
    expect(detailGroupsScript).toContain("fieldName !== 'status'");
    expect(detailGroupsScript).toContain("fieldName !== 'dispatchStatus'");
    expect(detailGroupsScript).toContain('!isDetailAttachmentColumn(column)');
    expect(detailGroupsScript).toContain('localDetailFieldColumns.value');
    expect(workbenchSource).toContain('dispatchStatusColumn');
    expect(workbenchSource).toContain('detailTopStatusTitle');
    expect(workbenchSource).toContain('detailTopStatusLabel');
    expect(workbenchSource).toContain('detail-meta-status-select');
    expect(workbenchSource).toContain(
      'v-model:value="detailEditForm[dispatchStatusColumn.dataIndex]"',
    );
    expect(documentFlowDetailScript).toContain('!isDetailAttachmentColumn(column)');
    expect(bottomAttachmentImageTemplate).toContain(':height="96"');
    expect(bottomAttachmentImageTemplate).toContain(':width="132"');
  });

  it('loads real workflow logs for document flow and change history panels', () => {
    expect(workbenchSource).toContain('getThreeCheckRecordWorkflowApi');
    expect(workbenchSource).toContain(
      'createOneShiftThreeCheckWorkflowPanelState',
    );
    expect(workbenchSource).toContain('detailWorkflowLoading');
    expect(workbenchSource).toContain('detailWorkflowError');
    expect(workbenchSource).toContain('暂无单据流');
    expect(workbenchSource).toContain('暂无变更历史');
    expect(workbenchSource).toContain('documentFlowOpen');
    expect(workbenchSource).toContain('getThreeCheckRecordFlowApi');
    expect(workbenchSource).toContain('rootDispatchRecordId');
    expect(workbenchSource).toContain('loadDocumentFlowFromRootDispatch');
    expect(workbenchSource).toContain('changeHistoryOpen');
    expect(workbenchSource).toContain('documentFlowLoading');
    expect(workbenchSource).toContain('changeHistoryLoading');
    expect(workbenchSource).toContain('documentFlowKeyword');
    expect(workbenchSource).toContain('changeHistoryKeyword');
    expect(workbenchSource).toContain('isFullChainDocumentFlowModule');
    expect(workbenchSource).toContain('fullChainDocumentFlowModuleKeys');
    expect(workbenchSource).toContain("'pre-shift-meeting'");
    expect(workbenchSource).toContain("'pre-shift-inspection'");
    expect(workbenchSource).toContain("'mid-shift-inspection'");
    expect(workbenchSource).toContain("'post-shift-inspection'");
    expect(workbenchSource).toContain("'pre-shift-safety-activity'");
    expect(workbenchSource).toContain("'key-sites'");
    expect(workbenchSource).toContain('keySiteFlowStageConfigs');
    expect(workbenchSource).toContain('keySiteRegistration');
    expect(workbenchSource).toContain('keySiteConfirmation');
    expect(workbenchSource).toContain('keySiteRectification');
    expect(workbenchSource).toContain('keySiteAcceptance');
    expect(workbenchSource).toContain('重点场所单据流洞察');
    expect(workbenchSource).toContain('场所透视树');
    expect(workbenchSource).toContain('重点场所 / 检查记录 / 整改验收');
    expect(workbenchSource).toContain('验收状态');
    expect(workbenchSource).toContain('当前节点：重点场所检查');
    expect(workbenchSource).toContain('preShiftSafetyActivityFlowStageConfig');
    expect(workbenchSource).toContain('isPreShiftSafetyActivityDocumentFlow');
    expect(workbenchSource).toContain('v-if="!isPreShiftSafetyActivityDocumentFlow"');
    expect(workbenchSource).toContain('currentDocumentFlowStage');
    expect(workbenchSource).toContain('isInspectionDocumentFlow');
    expect(workbenchSource).toContain('isStageSpecificDocumentFlow');
    expect(workbenchSource).toContain('单据流洞察');
    expect(workbenchSource).toContain('流程透视树');
    expect(workbenchSource).toContain('班组派班 / 班前会 / 三查记录');
    expect(workbenchSource).toContain('会议状态');
    expect(workbenchSource).toContain('检查状态');
    expect(workbenchSource).toContain('活动状态');
    expect(workbenchSource).toContain('广晟幕墙 / 班前安全活动');
    expect(workbenchSource).toContain('GUANGSHENG_CURTAIN_WALL_COMPANY_ID');
    expect(workbenchSource).toContain('shouldShowDataMap');
    expect(workbenchSource).toContain('pingan-shell--no-data-map');
    expect(workbenchSource).toContain('enforcePreShiftSafetyActivityCompany');
    expect(workbenchSource).toContain('当前节点：${currentDocumentFlowStage.value.label}');
    expect(workbenchSource).toContain('documentFlowDetailTitle');
    expect(workbenchSource).toContain('documentFlowSelectedNodeId');
    expect(workbenchSource).toContain('changeHistorySelectedGroupId');
    expect(workbenchSource).toContain('formatChangeHistoryTime');
    expect(workbenchSource).toContain('changeHistoryFieldLabel');
    expect(workbenchSource).toContain('changeHistoryDisplayVersion');
    expect(workbenchSource).toContain('documentFlowSummary');
    expect(workbenchSource).toContain('changeHistorySummary');
    expect(workbenchSource).toContain('documentFlowSteps');
    expect(workbenchSource).toContain('changeHistoryGroups');
    expect(workbenchSource).toContain('filteredDocumentFlowNodes');
    expect(workbenchSource).toContain('documentFlowTreeDetailNodes');
    expect(workbenchSource).toContain('document-flow-tree-children');
    expect(workbenchSource).toMatch(
      /documentFlowSelectedNodeId\.value\s*=\s*documentFlowNodes\.value\[0\]\?\.id/,
    );
    expect(workbenchSource).toContain('document-flow-modal');
    expect(workbenchSource).toContain('change-history-modal');
    expect(workbenchSource).toContain('一班三查全链路洞察');
    expect(workbenchSource).toContain('字段级追溯');
    expect(workbenchSource).toContain('订单透视树');
    expect(workbenchSource).toContain('搜索单据编号、流程节点、责任人');
    expect(workbenchSource).toContain('班组派班');
    expect(workbenchSource).toContain('班前会');
    expect(workbenchSource).toContain('班前检查');
    expect(workbenchSource).toContain('班中检查');
    expect(workbenchSource).toContain('班后检查');
    expect(workbenchSource).toContain('派班状态');
    expect(workbenchSource).toContain('生效');
    expect(workbenchSource).toContain('不生效');
    expect(workbenchSource).toContain('已开会议');
    expect(workbenchSource).toContain('待开会议');
    expect(workbenchSource).toContain('已检查');
    expect(workbenchSource).toContain('待检查');
    expect(workbenchSource).toContain('查看原单');
    expect(workbenchSource).toContain('创建记录');
    expect(workbenchSource).toContain('提交');
    expect(workbenchSource).toContain('附件（图片或视频）');
    expect(workbenchSource).toContain('字段');
    expect(workbenchSource).toContain('变更前');
    expect(workbenchSource).toContain('变更后');
    expect(workbenchSource).toContain('附件变更');
    expect(workbenchSource).toContain('change-history-sidebar__track');
    expect(workbenchSource).toContain('change-history-summary-card__icon');
    expect(workbenchSource).toContain('change-history-group__top');
    expect(workbenchSource).toContain('change-history-group__meta');
    expect(workbenchSource).toContain('change-history-group__stats');
    expect(workbenchSource).toContain('字段 {{ group.fieldCount }}');
    expect(workbenchSource).toContain('附件 {{ group.attachmentCount }}');
    expect(workbenchSource).toContain('流程 {{ group.flowCount }}');
    expect(workbenchSource).toContain('document-flow-detail-field');
    expect(workbenchSource).toContain('document-flow-lower');
    expect(workbenchSource).toContain('documentFlowStepTime');
    expect(workbenchSource).toContain('documentFlowStatusText');
    expect(workbenchSource).toContain('documentFlowLogsExpanded');
    expect(workbenchSource).toContain('visibleDocumentFlowStatusLogs');
    expect(workbenchSource).toContain('canToggleDocumentFlowLogs');
    expect(workbenchSource).toContain('toggleDocumentFlowLogs');
    expect(workbenchSource).toContain('展开全部');
    expect(workbenchSource).toContain('收起');
    expect(workbenchSource).toContain("fieldName !== 'dispatchStatus'");
    expect(workbenchSource).toContain('return `${team} · ${date}`');
    expect(workbenchSource).not.toContain('return `${team} · ${date} · ${recordNo}`');
    expect(workbenchSource).not.toContain('{{ step.recordNo }} · {{ step.date }}');
    expect(workbenchSource).toMatch(
      /\.document-flow-detail-grid[\s\S]*grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/,
    );
    expect(workbenchSource).toMatch(
      /\.document-flow-lower[\s\S]*grid-template-columns:\s*minmax\(360px,\s*0\.9fr\)\s*minmax\(420px,\s*1\.1fr\)/,
    );
    expect(workbenchSource).toContain('createDocumentFlowNodeFields(stage, row, detail)');
    expect(workbenchSource).toContain('documentFlowBusinessStatus');
    expect(workbenchSource).toContain('documentFlowStageStatusLabel');
    expect(workbenchSource).toContain('documentFlowIsCompletedStatus');
    expect(workbenchSource).toContain('change-history-diff-table');
    expect(workbenchSource).toMatch(/formatChangeHistoryTime\(latest\?\.createdAt\)/);
    expect(workbenchSource).toMatch(/V\{\{\s*changeHistoryDisplayVersion\(group\)/);
    expect(workbenchSource).toMatch(/changeHistoryFieldLabel\(item\)/);
    expect(workbenchSource).toMatch(
      /\.change-history-sidebar__track[\s\S]*background:\s*#d8e1ed/,
    );
    expect(workbenchSource).toMatch(
      /\.change-history-summary-card__icon[\s\S]*border-radius:\s*50%/,
    );
    expect(workbenchSource).toMatch(
      /\.change-history-group:not\(\.is-active\):not\(\.is-latest\)[\s\S]*background:\s*linear-gradient\(135deg,\s*#9ca3af,\s*#6b7280\)/,
    );
    expect(workbenchSource).toContain('document-flow-field-image--compact');
    expect(workbenchSource).toMatch(
      /\.document-flow-field-image--compact[\s\S]*width:\s*96px[\s\S]*height:\s*64px/,
    );
    expect(workbenchSource).toContain('document-flow-attachment-strip');
    expect(workbenchSource).toContain('openDocumentFlow');
    expect(workbenchSource).toContain('openChangeHistory');
    expect(workbenchSource).toContain("handleDetailAction('documentFlow')");
    expect(workbenchSource).toContain("handleDetailAction('changeHistory')");
  });

  it('renders linked team check items above attachments in inspection detail modals', () => {
    const detailBodyTemplate = sourceBetween(
      '<div v-else-if="currentThreeCheckDetail" class="meeting-detail">',
      '<div v-else-if="currentLocalDetail" class="meeting-detail">',
    );

    const tableIndex = detailBodyTemplate.indexOf('class="team-check-items"');
    const attachmentIndex = detailBodyTemplate.indexOf("'detail-attachment-row'");

    expect(workbenchSource).toContain('getTeamCheckTemplateResolveApi');
    expect(workbenchSource).toContain('loadTeamCheckInspectionLinesFromDetail');
    expect(workbenchSource).toContain('prepareTeamCheckInspectionForSubmit');
    expect(workbenchSource).toContain('serializeTeamCheckInspectionPayload');
    expect(tableIndex).toBeGreaterThanOrEqual(0);
    expect(attachmentIndex).toBeGreaterThan(tableIndex);
  });

  it('preserves in-progress inspection and safety-confirm edits around attachment actions', () => {
    const refreshCurrentDetailSource = sourceBetween(
      'async function refreshCurrentDetail()',
      'function detailActionLabel',
    );
    const handleAttachmentChangeSource = sourceBetween(
      'async function handleAttachmentChange(',
      'function handleAnyAttachmentChange',
    );
    const handleDetailAttachmentFieldChangeSource = sourceBetween(
      'async function handleDetailAttachmentFieldChange(',
      '</script>',
    );

    expect(workbenchSource).toContain('async function persistPendingDetailEdits()');
    expect(handleAttachmentChangeSource).toContain(
      'await persistPendingDetailEdits();',
    );
    expect(handleDetailAttachmentFieldChangeSource).toContain(
      'await persistPendingDetailEdits();',
    );
    expect(refreshCurrentDetailSource).toContain(`if (!detailEditing.value) {
      seedThreeCheckDetailEditForm();
      await loadTeamCheckInspectionLinesFromDetail();
      await loadSafetyConfirmLinesFromDetail();
    }`);
  });

  it('renders delete buttons on uploaded attachment previews in detail modals', () => {
    expect(workbenchSource).toContain('deletePreShiftMeetingAttachmentApi');
    expect(workbenchSource).toContain('deleteThreeCheckRecordAttachmentApi');
    expect(workbenchSource).toContain('async function handleDeleteAttachment(');
    expect(workbenchSource).toContain('class="attachment-delete-button"');
    expect(workbenchSource).toContain('aria-label="删除附件"');
    expect(workbenchSource).toContain(
      '@click.stop="handleDeleteAttachment(attachment)"',
    );
    expect(workbenchSource).toMatch(
      /\.attachment-preview\s*\{[\s\S]*position:\s*relative/,
    );
  });

  it('maps the three inspection routes to team check templates and omits risk in post-shift columns', () => {
    expect(moduleConfigSource).toContain('PinganPreShiftInspection');
    expect(moduleConfigSource).toContain('PRE_SHIFT_INSPECTION');
    expect(moduleConfigSource).toContain('PinganMidShiftInspection');
    expect(moduleConfigSource).toContain('MID_SHIFT_INSPECTION');
    expect(moduleConfigSource).toContain('PinganPostShiftInspection');
    expect(moduleConfigSource).toContain('POST_SHIFT_INSPECTION');
    expect(moduleConfigSource).toContain("column.dataIndex !== 'riskType'");
    expect(workbenchSource).toContain('getTeamCheckInspectionStage');
    expect(workbenchSource).toContain('getTeamCheckInspectionColumns');
  });

  it('marks team check required columns, limits result options, and leaves optional blanks empty', () => {
    expect(workbenchSource).toContain('isTeamCheckInspectionRequiredColumn');
    expect(workbenchSource).toContain('team-check-items__required');
    expect(workbenchSource).toContain("{ label: '无隐患', value: '无隐患' }");
    expect(workbenchSource).toContain("{ label: '有隐患', value: '有隐患' }");
    expect(workbenchSource).not.toContain("{ label: '不适用', value: '不适用' }");
    expect(workbenchSource).toContain("line[column.dataIndex] || ''");
    expect(workbenchSource).not.toContain("line[column.dataIndex] || '未填写'");

    expect(
      getTeamCheckInspectionColumns('PinganPreShiftInspection')
        .map((column) => column.dataIndex),
    ).toContain('riskType');
    expect(
      getTeamCheckInspectionColumns('PinganPostShiftInspection')
        .map((column) => column.dataIndex),
    ).not.toContain('riskType');
  });

  it('keeps rectification order fields on team check inspection lines', () => {
    const line = createTeamCheckInspectionLine({
      checkItem: '检查临边防护',
      checkResult: '有隐患',
      lastRectificationAction: '验收驳回',
      lastRectificationRemark: '整改照片不清晰',
      rectificationClosedAt: '2026-09-20 18:00:00',
      rectificationOrderId: '1',
      rectificationOrderNo: 'HR-20260604-0001',
      rectificationStatus: 'PENDING_RECTIFY',
      rectificationStatusLabel: '待整改',
      riskType: '高处坠落',
    });

    expect(line.rectificationOrderNo).toBe('HR-20260604-0001');
    expect(line.rectificationStatusLabel).toBe('待整改');
    expect(line.rectificationClosedAt).toBe('2026-09-20 18:00:00');
    expect(line.lastRectificationAction).toBe('验收驳回');

    expect(
      serializeTeamCheckInspectionPayload({}, [line]).checkItems[0],
    ).toMatchObject({
      lastRectificationRemark: '整改照片不清晰',
      rectificationOrderId: '1',
      rectificationOrderNo: 'HR-20260604-0001',
    });
    expect(
      getTeamCheckInspectionColumns('PinganPreShiftInspection').map(
        (column) => column.dataIndex,
      ),
    ).toEqual(
      expect.arrayContaining([
        'lastRectificationAction',
        'rectificationClosedAt',
        'rectificationOrderNo',
      ]),
    );
  });

  it('validates required team check fields before inspection submit', () => {
    expect(
      createTeamCheckInspectionLine({
        checkItem: '检查机械设备',
        defaultCheckResult: '无隐患',
        riskType: '机械伤害',
      }).checkResult,
    ).toBe('');
    expect(
      validateTeamCheckInspectionResults(
        [
          {
            checkItem: '检查机械设备',
            checkResult: '',
            riskType: '机械伤害',
          } as any,
        ],
        'PinganPreShiftInspection',
      ),
    ).toEqual({
      message: '请填写第 1 项检查结果',
      valid: false,
    });
    expect(
      validateTeamCheckInspectionResults(
        [
          createTeamCheckInspectionLine({
            checkItem: '检查机械设备',
            checkResult: '无隐患',
            riskType: '',
          }),
        ],
        'PinganPreShiftInspection',
      ),
    ).toEqual({
      message: '请填写第 1 项风险',
      valid: false,
    });
    expect(
      validateTeamCheckInspectionResults(
        [
          createTeamCheckInspectionLine({
            checkItem: '清点工具',
            checkResult: '有隐患',
            riskType: '',
          }),
        ],
        'PinganPostShiftInspection',
      ),
    ).toEqual({ message: '', valid: true });
    expect(
      validateTeamCheckInspectionResults(
        [
          {
            checkItem: '检查机械设备',
            checkResult: '不适用',
            riskType: '机械伤害',
          } as any,
        ],
        'PinganPreShiftInspection',
      ),
    ).toEqual({
      message: '第 1 项检查结果只能选择无隐患或有隐患',
      valid: false,
    });
  });

  it('renders and validates pre-shift meeting safety confirmation items above attachments', () => {
    const detailBodyTemplate = sourceBetween(
      '<div v-else-if="currentThreeCheckDetail" class="meeting-detail">',
      '<div v-else-if="currentLocalDetail" class="meeting-detail">',
    );
    const safetyTableIndex = detailBodyTemplate.indexOf(
      'class="safety-confirm-items"',
    );
    const attachmentIndex = detailBodyTemplate.indexOf("'detail-attachment-row'");

    expect(workbenchSource).toContain('loadSafetyConfirmLinesFromDetail');
    expect(workbenchSource).toContain('prepareSafetyConfirmForSubmit');
    expect(workbenchSource).toContain('serializePreShiftMeetingSafetyConfirmPayload');
    expect(workbenchSource).toContain('PRE_SHIFT_MEETING_CONFIRMATION');
    expect(workbenchSource).toContain("{ label: '已确认', value: '已确认' }");
    expect(safetyTableIndex).toBeGreaterThanOrEqual(0);
    expect(attachmentIndex).toBeGreaterThan(safetyTableIndex);

    expect(getPreShiftMeetingSafetyConfirmColumns().map((column) => column.dataIndex)).toEqual([
      'riskType',
      'safetyItem',
      'confirmStatus',
    ]);
    expect(
      createPreShiftMeetingSafetyConfirmLine({
        checkItem: '确认排水构筑物设计与防护能力',
        defaultCheckResult: '无隐患',
        riskType: '机械伤害',
      }),
    ).toMatchObject({
      confirmStatus: '',
      riskType: '机械伤害',
      safetyItem: '确认排水构筑物设计与防护能力',
    });
    expect(
      validatePreShiftMeetingSafetyConfirmResults([
        createPreShiftMeetingSafetyConfirmLine({
          confirmStatus: '',
          riskType: '机械伤害',
          safetyItem: '确认排水构筑物设计与防护能力',
        }),
      ]),
    ).toEqual({
      message: '请确认第 1 项安全确认事项',
      valid: false,
    });
    expect(
      validatePreShiftMeetingSafetyConfirmResults([
        createPreShiftMeetingSafetyConfirmLine({
          confirmStatus: '已确认',
          riskType: '机械伤害',
          safetyItem: '确认排水构筑物设计与防护能力',
        }),
      ]),
    ).toEqual({ message: '', valid: true });
  });

  it('requires at least one pre-shift meeting media check before submit', () => {
    expect(workbenchSource).toContain('validatePreShiftMeetingMediaChecks');
    expect(moduleConfigSource).toContain('班前会提交前必须完成图片或视频打卡');
    expect(
      validatePreShiftMeetingMediaChecks({
        imageCheck: '未上传',
        videoCheck: '未上传',
      }),
    ).toEqual({
      message: '班前会提交前必须完成图片或视频打卡',
      valid: false,
    });
    expect(
      validatePreShiftMeetingMediaChecks({
        imageCheck: '未上传',
        videoCheck: '视频已传',
      }),
    ).toEqual({ message: '', valid: true });
    expect(
      validatePreShiftMeetingMediaChecks({
        imageCheck: '现场照片',
        videoCheck: '未上传',
      }),
    ).toEqual({ message: '', valid: true });
    expect(
      validatePreShiftMeetingMediaChecks({
        imageCheck: '现场照片',
        videoCheck: '视频已传',
      }),
    ).toEqual({ message: '', valid: true });
  });
});
