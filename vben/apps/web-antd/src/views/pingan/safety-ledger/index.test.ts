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

describe('safety ledger page', () => {
  it('renders an internal ledger directory instead of relying on global child menus', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-ledger/index.vue'),
      'utf8',
    );

    expect(page).toContain('台账目录');
    expect(page).toContain('ledger-directory');
    expect(page).toContain('ledgerDirectoryKeyword');
    expect(page).toContain('filterSafetyLedgerNavigation');
    expect(page).toContain('goLedgerEntry');
    expect(page).toContain("router.push({ name: item.routeName })");
    expect(page).toContain('ledgerDirectoryCollapsed');
    expect(page).toContain('is-directory-collapsed');
    expect(page).toContain('ledger-directory__collapse-button');
    expect(page).toContain('ledger-directory__expand-button');
    expect(page).toContain('展开台账目录');
    expect(page).toContain('&gt;');
  });

  it('keeps the table area read-only with blank operation cells', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-ledger/index.vue'),
      'utf8',
    );

    expect(page).toContain("column.dataIndex === 'operation'");
    expect(page).not.toContain('查看详情');
    expect(page).not.toContain('openDetail');
  });

  it('distinguishes synchronized ledgers from self-maintained document ledgers', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-ledger/index.vue'),
      'utf8',
    );

    expect(page).toContain('isDocumentLedger');
    expect(page).toContain('新增台账');
  });

  it('keeps the safety ledger chrome focused on the current child title', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-ledger/index.vue'),
      'utf8',
    );

    expect(page).toContain('<h2>{{ entry.title }}</h2>');
    expect(page).not.toContain('sourceDescription');
    expect(page).not.toContain('sourceTag');
    expect(page).not.toContain('safety-ledger__breadcrumb');
    expect(page).not.toContain('按层级浏览，共');
    expect(page).not.toContain('混合数据源');
    expect(page).not.toContain('台账维护');
    expect(page).not.toContain('同步数据');
  });

  it('supports self-maintained document creation with managed companies and batch delete', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-ledger/index.vue'),
      'utf8',
    );

    expect(page).toContain('getSystemFullOrganizationTreeApi');
    expect(page).toContain('createSafetyLedgerDocumentApi');
    expect(page).toContain('batchDeleteSafetyLedgerDocumentsApi');
    expect(page).toContain('selectedRowKeys');
    expect(page).toContain('批量删除');
    expect(page).toContain('accept=".pdf,.doc,.docx"');
    expect(page).toContain('所属企业');
    expect(page).toContain('handleCreateDocument');
  });

  it('derives ledger organization options from the scoped full organization tree', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-ledger/index.vue'),
      'utf8',
    );

    expect(page).toContain('organizationOptionsFor');
    expect(page).toContain('syncCompanyOptionsFromOrganizationTree');
    expect(page).toContain('syncDepartmentOptionsFromOrganizationTree');
    expect(page).toContain('syncTeamOptionsFromOrganizationTree');
    expect(page).toContain('await loadOrganizationScopeTree();');
    expect(page).not.toContain('getCompanyOptionsApi');
    expect(page).not.toContain('getDepartmentOptionsApi');
    expect(page).not.toContain('getTeamOptionsApi');
  });

  it('uses linked company department and team selects in document ledgers', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-ledger/index.vue'),
      'utf8',
    );

    expect(page).toContain('documentDepartmentOptions');
    expect(page).toContain('documentTeamOptions');
    expect(page).toContain('handleDocumentCompanyChange');
    expect(page).toContain('handleDocumentDepartmentChange');
    expect(page).toContain('loadDocumentDepartmentOptions');
    expect(page).toContain('loadDocumentTeamOptions');
    expect(page).toContain(
      ':disabled="!documentForm.companyId || isLedgerOrganizationFieldLocked(\'department\')"',
    );
    expect(page).toContain(
      ':disabled="!documentDepartmentValue || isLedgerOrganizationFieldLocked(\'team\')"',
    );
    expect(page).toContain('department: documentForm.department.trim()');
    expect(page).toContain('team: documentForm.team.trim()');
    expect(page).not.toContain(
      '<Input v-model:value="documentForm.department" placeholder="请输入部门" />',
    );
    expect(page).not.toContain(
      '<Input v-model:value="documentForm.team" placeholder="请输入班组" />',
    );
  });

  it('scopes document create and edit organization fields by the current user role', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-ledger/index.vue'),
      'utf8',
    );

    expect(page).toContain('applyScopedLedgerDocumentOrganizationDefaults');
    expect(page).toContain('hydrateScopedLedgerDocumentOrganizationLabels');
    expect(page).toContain('enforceScopedLedgerDocumentOrganizationDefaults');
    expect(page).toMatch(
      /async function handleCreateDocument\(\) \{[\s\S]*await enforceScopedLedgerDocumentOrganizationDefaults\(\);[\s\S]*if \(/,
    );
    expect(page).toContain(
      ':disabled="isLedgerOrganizationFieldLocked(\'company\')"',
    );
    expect(page).toContain(
      ':disabled="!documentForm.companyId || isLedgerOrganizationFieldLocked(\'department\')"',
    );
    expect(page).toContain(
      ':disabled="!documentDepartmentValue || isLedgerOrganizationFieldLocked(\'team\')"',
    );
  });

  it('adds edit and delete actions only for self-maintained document rows', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-ledger/index.vue'),
      'utf8',
    );

    expect(page).toContain('updateSafetyLedgerDocumentApi');
    expect(page).toContain('deleteSafetyLedgerDocumentApi');
    expect(page).toContain('openEditDocument');
    expect(page).toContain('handleDeleteDocument');
    expect(page).toContain('修改');
    expect(page).toContain('删除');
    expect(page).toContain("column.dataIndex === 'operation' && isDocumentLedger");
  });

  it('adds a pre-shift meeting report action with media preview and print export', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-ledger/index.vue'),
      'utf8',
    );

    expect(page).toContain('getThreeCheckRecordFlowApi');
    expect(page).toContain('isPreShiftMeetingLedger');
    expect(page).toContain('canOpenMeetingReport');
    expect(page).toContain('openMeetingReport');
    expect(page).toContain('SafetyLedgerMeetingReport');
    expect(page).toContain('meetingReportOpen');
    expect(page).toContain('广晟矿业平安班组班前会议记录表');
    expect(page).toContain('打印/导出 PDF');
    expect(page).toContain('window.print()');
    expect(page).toContain('record.imagePreviewUrl');
    expect(page).toContain('record.videoPreviewUrl');
    expect(page).toContain('openMediaPreview');
    expect(page).toContain('safety-ledger-media-preview--image');
    expect(page).toContain('safety-ledger-media-preview--video');
    expect(page).toContain('mediaPreviewOpen');
    expect(page).toContain('记录表');
  });

  it('supports table configuration per safety ledger entry', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-ledger/index.vue'),
      'utf8',
    );

    expect(page).toContain('SafetyLedgerTableConfigDrawer');
    expect(page).toContain('tableConfigOpen');
    expect(page).toContain('表格配置');
    expect(page).toContain('applySystemTableConfigColumns');
    expect(page).toContain('safetyLedgerTableConfigStorageKey');
    expect(page).toContain('saveTableConfig');
    expect(page).toContain('deleteTableConfig');
    expect(page).toContain(':columns="tableColumns"');
  });

  it('shows filters above the ledger table and keeps the two-column shell', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-ledger/index.vue'),
      'utf8',
    );

    expect(page).toContain('safety-ledger-shell');
    expect(page).toContain('safety-ledger-content');
    expect(page).toContain('renderedFilterFields');
    expect(page).toContain('filterFieldLabel');
    expect(page).toContain('上传时间（起）');
    expect(page).toContain('检查单位');
    expect(page).toContain('受检单位');
    expect(page).toContain('车间');
    expect(page).toContain('状态');
    expect(page).not.toContain('搜索关键字');
    expect(page).toContain('DatePicker');
    expect(page).toContain('placeholder="年/月/日"');
    expect(page).toContain('value-format="YYYY-MM-DD"');
    expect(page).not.toContain('type="date"');
    expect(page).toContain('grid-template-columns: 260px minmax(0, 1fr)');
    expect(page).toContain('grid-template-rows: auto auto minmax(0, 1fr)');
    expect(page).toContain('height: calc(100vh - 112px)');
    expect(page).toContain('overflow-y: scroll');
  });

  it('scopes status filter organization fields by the current user role and organization', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-ledger/index.vue'),
      'utf8',
    );

    expect(page).toContain('useUserStore');
    expect(page).toContain('getSystemFullOrganizationTreeApi');
    expect(page).toContain('getVisiblePinganOrganizationFilterKeys');
    expect(page).toContain('isPinganOrganizationFilterLocked');
    expect(page).toContain('resolveScopedPinganOrganizationDefaults');
    expect(page).toContain('isLedgerFilterFieldVisible');
    expect(page).toContain('isLedgerFilterFieldLocked');
    expect(page).toContain('applyScopedLedgerOrganizationDefaults');
    expect(page).toContain(
      'entry.value.filters.filter((field) => isLedgerFilterFieldVisible(field))',
    );
  });
});
