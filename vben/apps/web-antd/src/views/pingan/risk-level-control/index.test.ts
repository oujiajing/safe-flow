import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { describe, expect, it } from 'vitest';

import {
  riskControlColumns,
  riskControlTableConfigModule,
} from './risk-level-control.config';

function appSourcePath(path: string) {
  return join(
    process.cwd(),
    process.cwd().endsWith(join('apps', 'web-antd'))
      ? path
      : `apps/web-antd/${path}`,
  );
}

describe('risk-level-control page', () => {
  it('defines the Excel-backed risk hazard columns', () => {
    expect(riskControlColumns.map((column) => column.title)).toEqual([
      '公司',
      '风险点',
      '危险源',
      '风险影响因素',
      '事故类型',
      '事故发生的可能性（L）',
      '人员暴露于危险环境中的频繁程度（E）',
      '发生事故可能造成的后果（C）',
      '风险值（D）',
      '风险等级',
      '关键技术与工程措施',
      '关键人员素养与系统管理措施',
      '关键个体防护与应急管理措施',
      '上级单位责任人',
      '责任部门',
      '责任人/联系方式',
      '可能产生的事故隐患',
      '隐患整治措施',
    ]);
    expect(riskControlTableConfigModule).toBe('risk-level-control');
  });

  it('keeps toolbar actions and table configuration wired on the page', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/risk-level-control/index.vue'),
      'utf8',
    );

    for (const text of [
      '新建',
      '新增隐患',
      '编辑隐患',
      '下载模板',
      '下载数据',
      '上传数据',
      '表格配置',
      'RiskControlTableConfigDrawer',
    ]) {
      expect(page).toContain(text);
    }
  });

  it('uses the one-shift workbench shell without changing risk-control fields', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/risk-level-control/index.vue'),
      'utf8',
    );

    for (const text of [
      "'pingan-shell'",
      'class="data-map"',
      '数据地图',
      '组织层级快速定位',
      'class="workbench__bar"',
      'class="filter-item filter-item--company"',
      'class="table-panel"',
      'class="table-panel__toolbar"',
      'class="meeting-table"',
      '风险库',
      '关键词',
    ]) {
      expect(page).toContain(text);
    }
    expect(page).toContain(':tree-data="filteredOrganizationTree"');
    expect(page).toContain('filteredLibraries');
    expect(page).toContain('getVisiblePinganOrganizationFilterKeys');
    expect(page).toContain('isPinganOrganizationFilterLocked');
    expect(page).toContain('resolveScopedPinganOrganizationDefaults');
    expect(page).toContain('getPinganOrgTreeApi');
    expect(page).toContain('organizationScopeNodes');
    expect(page).toContain('isCompanyFilterVisible');
    expect(page).toContain('isCompanyFilterLocked');
    expect(page).toContain('enforceCompanyScope');
    expect(page).toContain('v-if="isCompanyFilterVisible"');
    expect(page).toContain('riskControlColumns');
    expect(page).not.toContain('<span>日期（起）</span>');
    expect(page).not.toContain('<span>状态</span>');
  });

  it('matches the one-shift table header color and data-map interactions', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/risk-level-control/index.vue'),
      'utf8',
    );

    expect(page).toContain('createDataMapCollapseState');
    expect(page).toContain('createDataMapResizeState');
    expect(page).toContain('canShowPinganDataMap');
    expect(page).toContain('useUserStore');
    expect(page).toContain('v-if="shouldShowDataMap"');
    expect(page).toContain('isDataMapCollapsed');
    expect(page).toContain('dataMapToggleLabel');
    expect(page).toContain('class="data-map__collapse"');
    expect(page).toContain('class="data-map__resize-handle"');
    expect(page).toContain('@pointerdown="handleDataMapResizeStart"');
    expect(page).toContain('pingan-shell--map-collapsed');
    expect(page).toContain('data-map--collapsed');
    expect(page).toMatch(
      /\.meeting-table :deep\(\.ant-table-thead > tr > th\)[\s\S]*color:\s*#475569[\s\S]*font-size:\s*12px[\s\S]*font-weight:\s*700/,
    );
  });

  it('locks risk library and hazard company fields to the current role scope', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/risk-level-control/index.vue'),
      'utf8',
    );

    expect(page).toContain('applyScopedPinganOrganizationDefaults');
    expect(page).toContain('function enforceHazardFormCompanyScope()');
    expect(page).toMatch(
      /async function saveLibrary\(\) \{[\s\S]*enforceHazardFormCompanyScope\(\);/,
    );
    expect(page).toMatch(
      /async function saveHazard\(\) \{[\s\S]*enforceHazardFormCompanyScope\(\);/,
    );
    expect(page).toContain(':disabled="isCompanyFilterLocked"');
  });

  it('supports the formal database risk-hazard-library title', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/risk-level-control/index.vue'),
      'utf8',
    );

    expect(page).toContain("defineProps<{ pageTitle?: string }>()");
    expect(page).toContain("pageTitle: '风险分级管控'");
    expect(page).toContain('{{ props.pageTitle }}');
    expect(page).toContain(':module-title="props.pageTitle"');
  });

  it('reopens and expands the data map when returning from menu navigation', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/risk-level-control/index.vue'),
      'utf8',
    );

    expect(page).toContain('onActivated');
    expect(page).toContain('openDataMap');
    expect(page).toContain('collectExpandableOrganizationKeys');
    expect(page).toMatch(
      /if\s*\(isDataMapCollapsed\.value\)\s*\{[\s\S]*toggleDataMap\(\)/,
    );
    expect(page).toContain(
      'expandedOrganizationKeys.value = collectExpandableOrganizationKeys',
    );
  });
});
