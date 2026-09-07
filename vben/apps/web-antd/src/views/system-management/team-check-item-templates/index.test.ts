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

describe('team check item template page', () => {
  it('uses system-management shared layout and exposes inspection template controls', () => {
    const source = readFileSync(
      appSourcePath(
        'src/views/system-management/team-check-item-templates/index.vue',
      ),
      'utf8',
    );
    const config = readFileSync(
      appSourcePath(
        'src/views/system-management/team-check-item-templates/team-check-item-template.config.ts',
      ),
      'utf8',
    );

    for (const sharedComponent of [
      'SystemDataMap',
      'SystemFilterBar',
      'SystemTableConfigDrawer',
    ]) {
      expect(source).toContain(sharedComponent);
    }

    for (const text of [
      '班组检查项模板',
      '新建模板',
      '新增模板检查项',
      '模板操作',
      '模板状态',
      '新增公共检查项',
      '批量加入',
      '批量删除',
      '复制上级模板',
      '检查项库',
      '上级模板可复制',
      '班前会安全确认',
      '班前检查',
      '班中检查',
      '班后检查',
      '风险类型',
      '安全注意事项',
      '检查项',
      '启用',
      '草稿',
      '停用',
    ]) {
      expect(source + config).toContain(text);
    }

    expect(source).toContain('getTeamCheckTemplateResolveApi');
    expect(source).toContain('getTeamCheckTemplatesApi');
    expect(source).toContain('createTeamCheckTemplateApi');
    expect(source).toContain('getTeamCheckItemLibraryApi');
    expect(source).toContain('deleteTeamCheckItemLibraryApi');
    expect(source).toContain('getPinganOrgTreeApi');
    expect(source).not.toContain('getPinganCompanyOrgTreeApi');
    expect(source).not.toContain('SystemToolbar');
    expect(source).not.toContain('@refresh="refreshTemplates"');
    expect(source).not.toContain('@new="openCreateTemplate"');
    expect(source).toContain('function addTemplateOnlyItem');
    expect(source).toContain('stageScopedLibraryFields');
    expect(source).toContain('postShiftLibraryFields');
    expect(source).toContain('postShiftTemplateItemFields');
    expect(source).toContain('preShiftMeetingLibraryFields');
    expect(source).toContain('preShiftMeetingTemplateItemFields');
    expect(source).toContain("currentStage.value === 'PRE_SHIFT_MEETING_CONFIRMATION'");
    expect(source).toContain("currentStage.value === 'POST_SHIFT_INSPECTION'");
    expect(source).toContain('applicableStage: currentStage.value');
    expect(source).toContain('function deleteSelectedLibraryItems');
    expect(source).toContain('async function addSelectedLibraryItems');
    expect(source).toContain('canAddSelectedLibraryItems');
    expect(source).toContain('async function removeTemplateItem');
    expect(source).toContain('applySystemTableConfigColumns');
    expect(source).toContain('sortRowsBySystemTableConfig');
    expect(source).toContain('function saveTableConfig');
    expect(source).toContain('tableConfigOpen.value = false');
    expect(source).toContain('表格配置已保存');
    expect(source).toContain('模板至少保留一个检查项');
    expect(source).toContain('selectedLibraryRowKeys');
    expect(source).toContain(':row-selection');
    expect(source).toContain('templateItemDrawerOpen');
    expect(source).toContain('function childOptionsUnderSelectedCompany');
    expect(source).toContain('function initializeCompanyLevelExpandedKeys');
    expect(source).toContain('team-check-item-template-page__layout--map-collapsed');
    expect(source).toContain("dataMapCollapsed");
    expect(source).toContain('grid-template-columns: 56px minmax(0, 1fr)');
    expect(source).toContain('exactScope: true');
    expect(source).toContain("status: 'all'");
    expect(source).toContain('const exactTemplate = result.items[0];');
    expect(source).toContain('currentTemplateStatusLabel');
    expect(source).toContain('currentTemplateStatusColor');
    expect(source).toContain("node.orgType === 'COMPANY'");
    expect(source).toContain('async function addLibraryItem');
    expect(source).toContain('await saveTemplate();');
    expect(source).toContain('function loadUpperTemplate');
    expect(source).not.toContain('继承公司模板');
    expect(source).not.toContain('继承部门模板');
    expect(source).not.toContain('draftItems.value = [];');
    expect(source).toContain('team-check-item-template-page');
    expect(source).not.toContain('hero');
  });
});
