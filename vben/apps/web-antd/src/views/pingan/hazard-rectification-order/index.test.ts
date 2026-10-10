import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { describe, expect, it } from 'vitest';

function appSourcePath(path: string) {
  return join(
    process.cwd(),
    process.cwd().endsWith(join('apps', 'web-antd'))
      ? path
      : `apps/web-antd/${path}`,
  );
}

describe('hazard rectification order page', () => {
  it('loads organization tree options for filters and manual creation without adding personnel selectors', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('orgTreeOptions');
    expect(page).toContain('loadOrgTreeOptions');
    expect(page).toContain('getPinganOrgTreeApi');
    expect(page).not.toContain('getSystemOrganizationTreeApi');
    expect(page).toContain('normalizeOrgTree');
    expect(page).toContain('请选择公司');
    expect(page).toContain('请选择部门');
    expect(page).toContain('请选择班组');
    expect(page).not.toContain('loadUserOptions');
  });

  it('uses the full organization tree so manual creation can load departments under a selected company', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('const nodes = await getPinganOrgTreeApi()');
    expect(page).toContain(
      "childOptionsUnder(createForm.companyId, undefined, 'DEPARTMENT')",
    );
    expect(page).toContain(
      "childOptionsUnder(createForm.companyId, createForm.departmentId, 'TEAM')",
    );
  });

  it('provides a manual create drawer without adding personnel selectors', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('createHazardRectificationOrderApi');
    expect(page).toContain('createOpen');
    expect(page).toContain('createForm');
    expect(page).toContain('createItems');
    expect(page).toContain('新增工单');
    expect(page).toContain('手工新增隐患整改工单');
    expect(page).toContain('业务日期');
    expect(page).toContain('风险类型');
    expect(page).toContain('检查项');
    expect(page).toContain('隐患描述');
    expect(page).toContain('整改前照片');
    expect(page).toContain('添加隐患明细');
    expect(page).toContain('submitManualOrder');
    expect(page).not.toContain('createResponsibleUserSelector');
  });

  it('allows filtering unified orders by manual, three-check, and safety-inspection sources', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('sourceTypeOptions');
    expect(page).toContain('一班三查');
    expect(page).toContain('手工新增');
    expect(page).toContain('安全检查');
    expect(page).toContain('随手拍');
    expect(page).toContain('SAFETY_INSPECTION');
    expect(page).not.toContain('SAFETY_CHECK');
    expect(page).toContain('QUICK_SHOT');
    expect(page).toContain('sourceTypeLabel');
  });

  it('renders status, query and reset as one shared inline filter group', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );
    expect(page).toContain('<StatusFilterActions');
    expect(page).toContain(':options="statusOptions"');
    expect(page).toContain('search-text="查询"');
    expect(page).toContain('@update:model-value="setStatusFilter"');
    expect(page).not.toContain('hazard-status-tabs');
    expect(page).not.toContain('v-model:value="query.sourceRecordId"');
    expect(page).not.toContain('来源记录ID');
    expect(page).toContain('<span>来源类型</span>');
    expect(page).toContain('class="toolbar-filter"');
    expect(
      page.indexOf('class="hazard-order-toolbar__status"'),
    ).toBeGreaterThan(page.indexOf('lucide:refresh-cw'));
    expect(page).toMatch(
      /\.hazard-order-toolbar\s*\{[\s\S]*flex-wrap: nowrap;[\s\S]*align-items: flex-end;[\s\S]*overflow: hidden;/,
    );
    expect(page).toMatch(
      /\.toolbar-filter :deep\(\.ant-select-selection-item\),[\s\S]*align-items: center;/,
    );
    expect(page).toMatch(
      /\.hazard-order-toolbar :deep\(\.ant-btn\)[\s\S]*justify-content: center;[\s\S]*line-height: 1;/,
    );
  });

  it('renders source module keys as Chinese labels in the order list', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('sourceModuleLabel');
    expect(page).toContain("'pre-shift-inspection': '班前检查'");
    expect(page).toContain("'mid-shift-inspection': '班中检查'");
    expect(page).toContain("'post-shift-inspection': '班后检查'");
    expect(page).toContain("'safety-check': '安全检查'");
    expect(page).toContain("'quick-shot': '随手拍'");
    expect(page).toContain("column.dataIndex === 'sourceModuleKey'");
    expect(page).toContain('sourceModuleLabel(record.sourceModuleKey)');
  });

  it('uses a minute-precision date time picker for issuing rectification deadlines', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('DatePicker');
    expect(page).toContain('<DatePicker');
    expect(page).toContain('TimePicker');
    expect(page).toContain('actionForm.rectificationDeadlineDate');
    expect(page).toContain('actionForm.rectificationDeadlineTime');
    expect(page).toContain('placeholder="选择日期"');
    expect(page).toContain('placeholder="选择时间"');
    expect(page).toContain(':minute-step="5"');
    expect(page).toContain("rectificationDeadlineTime: '18:00'");
    expect(page).toContain('function selectedRectificationDeadline()');
    expect(page).toContain("message.error('请选择完整的整改日期和时间')");
  });

  it('uploads rectification completion photos through the order attachment api instead of data urls', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('rectifiedPhotoInputRef');
    expect(page).toContain('handleRectifiedPhotoChange');
    expect(page).toContain('uploadHazardRectificationOrderAttachmentApi');
    expect(page).toContain('RECTIFICATION_AFTER_PHOTO');
    expect(page).toContain('accept="image/*"');
    expect(page).toContain('上传整改后照片');
    expect(page).toContain('class="rectified-photo-preview"');
    expect(page).toContain(
      ':src="resolveAttachmentPreviewUrl(actionForm.afterPhoto)"',
    );
    const uploadHandler = page.match(
      /async function handleRectifiedPhotoChange[\s\S]*?function normalizeOrgTree/,
    )?.[0];
    expect(uploadHandler).toBeTruthy();
    expect(uploadHandler).not.toContain('FileReader');
    expect(page).not.toContain('data:image/png;base64');
    expect(page).not.toContain('placeholder="整改后照片URL"');
  });

  it('shows action details and rectification after photo in order detail', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('order-result-panel');
    expect(page).toContain('整改结果');
    expect(page).toContain('currentOrder.rectificationDescription');
    expect(page).toContain('currentOrder.rectificationAfterPhoto');
    expect(page).toContain('查看详情');
    expect(page).toContain('flowLogDetailItems(log)');
    expect(page).toContain('isFlowLogPhotoDetail');
    expect(page).toContain('flow-log-photo');
    expect(page).toContain(
      "key !== 'afterPhoto' || log.action === 'MARK_RECTIFIED'",
    );
  });

  it('groups the redesigned detail into five focused tabs and a compact process table', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain("{ key: 'hazard', label: '隐患与证据' }");
    expect(page).toContain("{ key: 'result', label: '整改结果' }");
    expect(page).toContain("{ key: 'acceptance', label: '验收结论' }");
    expect(page).toContain("{ key: 'history', label: '处理记录' }");
    expect(page).toContain("{ key: 'basic', label: '基础信息' }");
    expect(page).toContain('class="detail-status-overview"');
    expect(page).toContain('class="evidence-compare-grid"');
    expect(page).toContain('class="process-table"');
    expect(page).toContain('class="rectification-detail-drawer"');
    expect(page).toContain(':width="1320"');
    expect(page).toContain('centered');
    expect(page).toContain('class="acceptance-brief"');
    expect(page).toContain('rectificationSubmitLog');
    expect(page).toContain('rectificationSubmitStatusLabel');
    expect(page).toContain('class="rectification-detail-list"');
    expect(page).toContain('class="detail-section submission-record-card"');
    expect(page).toContain('附件数量：1 个');
    expect(page).toContain('function acceptanceConclusionCriteria()');
    expect(page).toContain('class="detail-section acceptance-review-card"');
    expect(page).toContain('class="acceptance-info-grid"');
    expect(page).toContain('class="detail-section acceptance-evidence-card"');
    expect(page).toContain('class="order-result-panel acceptance-ai-panel"');
    expect(page).toContain('class="acceptance-closure-bar"');
    expect(page).toContain("activeDetailTab = 'history'");
    expect(page).toContain('const filteredFlowLogs = computed');
    expect(page).toContain('class="history-toolbar"');
    expect(page).toContain('class="process-table process-table--history"');
    expect(page).toContain('class="process-row__marker"');
    expect(page).toContain('class="system-log-panel"');
    expect(page).toContain('@click="exportHistoryRecords"');
    expect(page).toContain('_处理记录.csv');
    expect(page).toContain('class="basic-card-grid"');
    expect(page).toContain('class="basic-info-card"');
    expect(page).toContain('class="basic-extension-panel"');
    expect(page).toContain('@click="copyCurrentOrderNo"');
    expect(page).toContain('function completedOnTime()');
    expect(page).toContain('function internalOrderNumber');
    expect(page).toContain('acceptanceSubmitLog');
    expect(page).not.toContain('<Timeline');
  });

  it('keeps redesigned evidence, filters and modal scrolling accessible', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('aria-label="处理类型筛选"');
    expect(page).toContain('aria-label="操作人筛选"');
    expect(page).toContain('aria-label="处理记录关键字搜索"');
    expect(page).toContain('aria-label="上传整改后照片"');
    expect(page).toContain('aria-label="整改前隐患视频"');
    expect(page).toContain('height="280"');
    expect(page).toContain('width="520"');
    expect(page).toContain('overscroll-behavior: contain');
  });

  it('renders task-focused cards for issue, rectification result and acceptance review', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain("title: '下发整改'");
    expect(page).toContain("title: '提交整改结果'");
    expect(page).toContain("title: '审核验收'");
    expect(page).toContain('class="action-hazard-summary"');
    expect(page).toContain('class="action-evidence-grid"');
    expect(page).toContain('class="acceptance-decision"');
    expect(page).toContain(':z-index="1100"');
    expect(page).toContain(':width="1180"');
    expect(page).toContain('placeholder="请填写可核验、可量化的验收标准"');
    expect(page).toContain("message.error('请填写验收标准')");
    expect(page).toContain('提交后将进入下一流程节点并保留操作记录');
  });

  it('allows selecting an acceptance user when accepting or rejecting acceptance', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toMatch(
      /case 'ACCEPT':\s+\{\s+return \{[\s\S]*acceptanceUserId: selectedAcceptanceUserId\(\)/,
    );
    expect(page).toMatch(
      /case 'REJECT_ACCEPTANCE':\s+\{\s+return \{[\s\S]*acceptanceUserId: selectedAcceptanceUserId\(\)/,
    );
    expect(page).toContain('function selectedAcceptanceUserId()');
    expect(page).toContain('function actionRequiresAcceptanceUser()');
    expect(page).toContain('请选择验收人');
    expect(page).not.toContain('验收人必须是数字');
    expect(page).toContain('String(currentOrder.value.acceptanceUserId)');
    expect(page).toContain('const acceptanceUserOptions = computed');
    expect(page).toContain('value: String(user.id)');
    expect(page).toContain(
      'options.push({ label: selectedName, value: selectedId })',
    );
    expect(page).toContain('acceptance-action-fields');
    expect(page).toContain('v-if="currentAction === \'ACCEPT\'"');
    expect(page).toContain(
      'v-else-if="currentAction === \'REJECT_ACCEPTANCE\'"',
    );
    expect(page).toContain('acceptance-user-input');
    expect(page).toContain('placeholder="请选择验收人"');
    expect(page).toMatch(
      /<Select\s+[\s\S]*v-model:value="actionForm\.acceptanceUserId"[\s\S]*:options="acceptanceUserOptions"[\s\S]*show-search/,
    );
    expect(page).toContain('placeholder="验收意见"');
    expect(page).toContain('placeholder="驳回原因"');
  });

  it('selects rectification and acceptance departments from the current company departments', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('const actionDepartmentOptions = computed');
    expect(page).toContain(
      "childOptionsUnder(currentOrder.value?.companyId, undefined, 'DEPARTMENT')",
    );
    expect(page).toContain('rectificationDepartmentId');
    expect(page).toContain('acceptanceDepartmentId');
    expect(page).toContain('placeholder="请选择整改部门"');
    expect(page).toContain('placeholder="请选择验收部门"');
    expect(page).toMatch(
      /case 'ISSUE_RECTIFICATION':\s+\{\s+return \{[\s\S]*rectificationDepartmentId: actionForm\.rectificationDepartmentId[\s\S]*rectificationResponsibleUserId/,
    );
    expect(page).toMatch(
      /case 'REQUEST_ACCEPTANCE':\s+\{\s+return \{[\s\S]*acceptanceDepartmentId: actionForm\.acceptanceDepartmentId[\s\S]*acceptanceUserId/,
    );
  });

  it('supports selecting a suggested user or entering any demo name', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('getPinganUsersApi');
    expect(page).toContain('loadResponsibleUsers');
    expect(page).toContain('editableUserOptions');
    expect(page).toContain('function editableUserIdentity');
    expect(page).toContain('function selectedRectificationResponsibleUserId()');
    expect(page).toMatch(
      /rectificationResponsibleUserId:\s*selectedRectificationResponsibleUserId\(\)/,
    );
    expect(page).toMatch(
      /<AutoComplete\s+[\s\S]*v-model:value="actionForm\.rectificationResponsibleUserInput"[\s\S]*:options="editableUserOptions"/,
    );
    expect(page).toContain('v-model:value="actionForm.acceptanceUserInput"');
    expect(page).toContain('placeholder="请选择或输入整改责任人"');
    expect(page).toContain('placeholder="请选择或输入验收人"');
    expect(page).toContain('演示模式：可选择建议人员，也可直接输入任意姓名');
    expect(page).toContain('演示模式下支持直接输入任意姓名');
    expect(page).toContain("message.error('请选择或填写整改责任人')");
    expect(page).not.toMatch(
      /<InputNumber\s+[\s\S]*v-model:value="actionForm\.rectificationResponsibleUserId"/,
    );
  });

  it('uses person and department labels without exposing ID wording in flow details or action forms', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain("acceptanceDepartmentId: '验收部门'");
    expect(page).toContain("acceptanceUserId: '验收人'");
    expect(page).toContain("rectificationDepartmentId: '整改部门'");
    expect(page).toContain("rectificationResponsibleUserId: '整改责任人'");
    expect(page).toContain('placeholder="请选择或输入整改责任人"');
    expect(page).toContain('placeholder="请选择验收人"');
    expect(page).toContain("message.error('请选择验收人')");
    expect(page).not.toContain('验收人必须是数字');
    expect(page).not.toContain('整改责任人ID');
    expect(page).not.toContain('验收人ID');
    expect(page).not.toContain('整改部门ID');
    expect(page).not.toContain('验收部门ID');
  });

  it('adds optional supplemental attachments to the dispatch action', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('补充附件');
    expect(page).toContain('supplementalAttachmentFiles');
    expect(page).toContain('multiple');
    expect(page).toContain('accept=".jpg,.jpeg,.png,.gif,.webp,.pdf,.doc,.docx"');
    expect(page).toContain('单个不超过 10MB，最多 5 个');
    expect(page).toContain("'RECTIFICATION_SUPPLEMENT'");
    expect(page).toContain('payload.supplementalAttachments');
  });

  it('renders rectification and acceptance department names in flow log details instead of raw ids', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('function organizationNameById');
    expect(page).toContain('function flowLogPayloadValue');
    expect(page).toMatch(
      /if \(\[\s*'acceptanceDepartmentId',\s*'rectificationDepartmentId',?\s*\]\.includes\(key\)\)/,
    );
    expect(page).toContain(
      'organizationNameById(value) || unknownToText(value)',
    );
    expect(page).toMatch(
      /value:\s*flowLogPayloadValue\(key,\s*payload\[key\],\s*payload\)/,
    );
    expect(page).toContain('function userNameById');
    expect(page).toContain('unknownToText(payload[nameKey])');
  });

  it('supports cancelling active orders with a cancel reason', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain("{ label: '已作废', value: 'CANCELLED' }");
    expect(page).toContain("CANCELLED: 'default'");
    expect(page).toContain("CANCEL: '作废'");
    expect(page).toContain("case 'PENDING_ASSIGN':");
    expect(page).toContain("['ISSUE_RECTIFICATION', 'CANCEL']");
    expect(page).toContain("case 'PENDING_RECTIFY':");
    expect(page).toContain("['MARK_RECTIFIED', 'CANCEL']");
    expect(page).toContain("case 'RECTIFIED':");
    expect(page).toContain("['REQUEST_ACCEPTANCE', 'CANCEL']");
    expect(page).toContain("case 'PENDING_ACCEPTANCE':");
    expect(page).toContain("['ACCEPT', 'CANCEL']");
    expect(page).toContain('canPerformOrderAction');
    expect(page).toContain('cancelReason');
    expect(page).toContain("case 'CANCEL':");
    expect(page).toContain('placeholder="作废原因"');
    expect(page).toContain('请填写作废原因');
  });

  it('hides sequence and risk columns only for quick-shot and safety-check detail items', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('const detailItemColumns = computed');
    expect(page).toContain('hideSourceSequenceAndRiskColumns');
    expect(page).toContain("['QUICK_SHOT', 'SAFETY_INSPECTION'].includes");
    expect(page).toContain("column.dataIndex === 'sourceLineIndex'");
    expect(page).toContain("column.dataIndex === 'riskType'");
    expect(page).toContain("{ dataIndex: 'checkItem', title: '检查项'");
    expect(page).toContain("{ dataIndex: 'aiEnabled', title: '是否启用AI'");
    expect(page).toContain(':columns="detailItemColumns"');
  });

  it('shows source hazard image or video in the order item table', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain(
      "{ dataIndex: 'hazardMedia', title: '隐患图片/视频'",
    );
    expect(page).toContain('itemHazardMediaUrl(record)');
    expect(page).toContain('itemHazardMediaIsVideo(record)');
    expect(page).toContain('<video');
    expect(page).toContain('class="order-item-media"');
    expect(page).toContain('record.beforePhoto');
    expect(page).toContain('record.beforeVideo');
    expect(page).toContain('resolveAttachmentPreviewUrl');
  });

  it('supports selecting and batch deleting hazard rectification orders', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('batchDeleteHazardRectificationOrdersApi');
    expect(page).toContain('selectedOrderRowKeys');
    expect(page).toContain('rowSelection');
    expect(page).toContain(':row-selection="rowSelection"');
    expect(page).toContain('批量删除');
    expect(page).toContain('handleBatchDelete');
    expect(page).toContain('确认删除选中的隐患整改工单吗？');
  });

  it('gates create, delete, and workflow actions with hazard submodule permissions', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('useAccessStore');
    expect(page).toContain('canCreateOrder');
    expect(page).toContain('canDeleteOrder');
    expect(page).toContain('canRectifyOrder');
    expect(page).toContain('canAcceptOrder');
    expect(page).toContain('canVoidOrder');
    expect(page).toContain(':disabled="!canCreateOrder"');
    expect(page).toContain(
      ':disabled="selectedOrderRowKeys.length === 0 || !canDeleteOrder"',
    );
    expect(page).toContain('actions.filter((action) =>');
  });

  it('locks manual quick-shot organization fields to the current user role scope', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('useUserStore');
    expect(page).toContain('currentUserRoles');
    expect(page).toContain('currentUserOrgDefaults');
    expect(page).toContain('applyScopedOrganizationDefaults(createForm)');
    expect(page).toContain("isHazardOrganizationFieldLocked('company')");
    expect(page).toContain("isHazardOrganizationFieldLocked('department')");
    expect(page).toContain("isHazardOrganizationFieldLocked('team')");
    expect(page).toContain('function enforceHazardUserOrganizationScope()');
    expect(page).toContain('getVisiblePinganOrganizationFilterKeys');
    expect(page).toContain('isPinganOrganizationFilterLocked');
    expect(page).toContain('resolveScopedPinganOrganizationDefaults');
    expect(page).toMatch(
      /async function submitManualOrder\(\) \{[\s\S]*enforceHazardUserOrganizationScope\(\);[\s\S]*if \(!validateCreateForm\(\)\) return;/,
    );
  });

  it('scopes the list organization filters by the current user role and organization', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).toContain('isHazardOrganizationFilterVisible');
    expect(page).toContain('applyScopedOrganizationDefaults(query)');
    expect(page).toContain(
      'v-if="isHazardOrganizationFilterVisible(\'company\')"',
    );
    expect(page).toContain(
      'v-if="isHazardOrganizationFilterVisible(\'department\')"',
    );
    expect(page).toContain(
      'v-if="isHazardOrganizationFilterVisible(\'team\')"',
    );
    expect(page).toContain(
      ':disabled="isHazardOrganizationFieldLocked(\'company\')"',
    );
    expect(page).toMatch(
      /:disabled="\s*!query\.companyId \|\|\s*isHazardOrganizationFieldLocked\('department'\)\s*"/,
    );
    expect(page).toMatch(
      /:disabled="\s*!query\.departmentId \|\|\s*isHazardOrganizationFieldLocked\('team'\)\s*"/,
    );
  });

  it('does not expose a legacy hazard rectification entry', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/hazard-rectification-order/index.vue'),
      'utf8',
    );

    expect(page).not.toContain('查看旧隐患整改记录');
    expect(page).not.toContain(
      '/pingan/hazard-inspection/rectification/legacy',
    );
  });
});
