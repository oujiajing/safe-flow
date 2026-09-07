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

describe('special-work page', () => {
  it('uses server workflow actions instead of an editable target status', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/special-work/index.vue'),
      'utf8',
    );
    const workflow = readFileSync(
      appSourcePath('src/views/pingan/special-work/special-work-workflow.ts'),
      'utf8',
    );

    expect(page).toContain('executeSpecialWorkActionApi');
    expect(workflow).toContain('APPROVE_AND_START');
    expect(workflow).toContain('SUBMIT_ACCEPTANCE');
    expect(workflow).toContain('COMPLETE_ACCEPTANCE');
    expect(page).toContain('getSpecialWorkWorkflowAction');
    expect(page).toContain('canExecuteSpecialWorkWorkflowAction');
    expect(page).toMatch(
      /v-if="getSpecialWorkWorkflowAction[\s\S]*:disabled="[\s\S]*!canExecuteSpecialWorkWorkflowAction/,
    );
    expect(page).not.toContain('v-model:value="form.status"');
  });

  it('uses the shared data-map shell while keeping special-work filters and actions', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/special-work/index.vue'),
      'utf8',
    );

    expect(page).toContain("import DataMapPanel from '#/views/pingan/shared/DataMapPanel.vue'");
    expect(page).toContain('canShowPinganDataMap');
    expect(page).toContain('useUserStore');
    expect(page).toContain('v-if="shouldShowDataMap"');
    expect(page).toContain('getPinganOrgTreeApi');
    expect(page).toContain('organizationScopeNodes');
    expect(page).toContain('<DataMapPanel');
    expect(page).toContain(':tree-data="organizationNodes"');
    expect(page).toContain('@select="handleDataMapSelect"');
    expect(page).toContain('filters.companyId = selectedCompanyId');
    expect(page).toContain('getVisiblePinganOrganizationFilterKeys');
    expect(page).toContain('isPinganOrganizationFilterLocked');
    expect(page).toContain('resolveScopedPinganOrganizationDefaults');
    expect(page).toContain('isCompanyFilterVisible');
    expect(page).toContain('isCompanyFilterLocked');
    expect(page).toContain('enforceCompanyScope');
    expect(page).toContain('v-if="isCompanyFilterVisible"');
    expect(page).toContain('special-work-workbench');

    for (const text of [
      '<span>公司</span>',
      '<span>日期（起）</span>',
      '<span>日期（止）</span>',
      '<span>状态</span>',
      '新增特殊作业',
      '下载',
      '批量删除',
    ]) {
      expect(page).toContain(text);
    }
  });

  it('places image upload at the bottom and supports enlarged previews in form and table', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/special-work/index.vue'),
      'utf8',
    );

    expect(page.indexOf('<span>状态</span>')).toBeLessThan(
      page.indexOf('class="special-work-image-field"'),
    );
    expect(page).toContain('formImagePreviewUrl');
    expect(page).toContain('special-work-attachment-card');
    expect(page).toContain('lucide:image-plus');
    expect(page).toContain('图片打卡');
    expect(page).toContain('选择图片');
    expect(page).toContain('class="special-work-form-preview"');
    expect(page).toContain('class="special-work-form-preview__image"');
    expect(page).toContain('class="special-work-table-image"');
    expect(page.match(/:preview="true"/g)).toHaveLength(2);
  });

  it('locks create and edit company fields to the current role scope', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/special-work/index.vue'),
      'utf8',
    );

    expect(page).toContain('applyScopedPinganOrganizationDefaults');
    expect(page).toContain('function enforceFormCompanyScope()');
    expect(page).toMatch(
      /async function saveRecord\(\) \{[\s\S]*enforceFormCompanyScope\(\);/,
    );
    expect(page).toContain(':disabled="isCompanyFilterLocked"');
  });

  it('defines the reusable shared data-map component contract', () => {
    const component = readFileSync(
      appSourcePath('src/views/pingan/shared/DataMapPanel.vue'),
      'utf8',
    );

    for (const text of [
      '数据地图',
      '组织层级快速定位',
      'createDataMapCollapseState',
      'createDataMapResizeState',
      'openAllDataMap',
      'onActivated(openAllDataMap)',
      'watch(() => props.treeData',
      'class="data-map__collapse"',
      'class="data-map__resize-handle"',
      '@pointerdown="handleResizeStart"',
      'data-map--collapsed',
    ]) {
      expect(component).toContain(text);
    }
    expect(component).toMatch(
      /\.data-map--collapsed \.data-map__header\s*\{[\s\S]*justify-content:\s*flex-start/,
    );
  });
});
