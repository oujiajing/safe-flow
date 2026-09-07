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

describe('risk-four-color-map page', () => {
  it('uses the risk-control data-map shell and four-color map workbench actions', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/risk-four-color-map/index.vue'),
      'utf8',
    );

    for (const text of [
      "'pingan-shell'",
      'class="data-map"',
      '数据地图',
      '组织层级快速定位',
      'class="workbench__bar"',
      'class="filter-item filter-item--company"',
      '四色图名称',
      '关键词',
      '新建四色图',
      '上传/替换底图',
      '风险四色图',
      '暂无四色图底图',
    ]) {
      expect(page).toContain(text);
    }
    expect(page).toContain(':tree-data="filteredOrganizationTree"');
    expect(page).toContain('createDataMapCollapseState');
    expect(page).toContain('createDataMapResizeState');
    expect(page).toContain('canShowPinganDataMap');
    expect(page).toContain('useUserStore');
    expect(page).toContain('v-if="shouldShowDataMap"');
    expect(page).toContain('getVisiblePinganOrganizationFilterKeys');
    expect(page).toContain('isPinganOrganizationFilterLocked');
    expect(page).toContain('resolveScopedPinganOrganizationDefaults');
    expect(page).toContain('getPinganOrgTreeApi');
    expect(page).toContain('organizationScopeNodes');
    expect(page).toContain('isCompanyFilterVisible');
    expect(page).toContain('isCompanyFilterLocked');
    expect(page).toContain('enforceCompanyScope');
    expect(page).toContain('v-if="isCompanyFilterVisible"');
    expect(page).toContain('openDataMap');
  });

  it('renders image preview controls without point or region editing', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/risk-four-color-map/index.vue'),
      'utf8',
    );

    for (const text of ['适应屏幕', '放大', '缩小', '还原', '全屏查看']) {
      expect(page).toContain(text);
    }
    expect(page).toContain('risk-map-preview__image');
    expect(page).toContain('previewScale');
    expect(page).not.toContain('新增风险点');
    expect(page).not.toContain('drawPolygon');
  });

  it('locks create and edit company fields to the current role scope', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/risk-four-color-map/index.vue'),
      'utf8',
    );

    expect(page).toContain('applyScopedPinganOrganizationDefaults');
    expect(page).toContain('function enforceFormCompanyScope()');
    expect(page).toMatch(
      /async function saveMap\(\) \{[\s\S]*enforceFormCompanyScope\(\);/,
    );
    expect(page).toContain(':disabled="isCompanyFilterLocked"');
  });

  it('keeps the page fixed and lets the basic information panel collapse', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/risk-four-color-map/index.vue'),
      'utf8',
    );

    expect(page).toContain('isDetailCollapsed');
    expect(page).toContain('risk-map-layout--detail-collapsed');
    expect(page).toContain('基础信息');
    expect(page).toContain('收起');
    expect(page).toContain('展开');
    expect(page).toMatch(/\.risk-four-color-page\s*\{[\s\S]*height:\s*100%[\s\S]*overflow:\s*hidden/);
    expect(page).toMatch(/\.pingan-shell\s*\{[\s\S]*height:\s*calc\(100vh - 112px\)[\s\S]*overflow:\s*hidden/);
    expect(page).toMatch(/\.risk-map-preview__canvas\s*\{[\s\S]*min-height:\s*0/);
  });
});
