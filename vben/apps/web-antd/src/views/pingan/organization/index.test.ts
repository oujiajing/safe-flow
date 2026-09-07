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

describe('organization architecture page', () => {
  it('uses the shared data map and read-only system-maintained overview shell', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).toContain(
      "import DataMapPanel from '#/views/pingan/shared/DataMapPanel.vue'",
    );
    expect(page).toContain('canShowPinganDataMap');
    expect(page).toContain('useUserStore');
    expect(page).toContain('v-if="shouldShowDataMap"');
    expect(page).toContain('getPinganCompanyOrgTreeApi');
    expect(page).toContain('getPinganOrgTreeApi');
    expect(page).toContain('getActiveContentProfileByOrgApi');
    expect(page).toContain('<DataMapPanel');
    expect(page).toContain(':tree-data="organizationNodes"');
    expect(page).toContain('@select="handleDataMapSelect"');
    expect(page).toContain('基础数据由系统管理维护');
    expect(page).toContain('系统管理维护');
    expect(page).toContain('业务视角');
  });

  it('keeps organization architecture read-only instead of duplicating system management CRUD', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    for (const forbidden of [
      '新增人员',
      '新增部门',
      '删除',
      '导入人员',
      '批量导入',
      '调整角色',
    ]) {
      expect(page).not.toContain(forbidden);
    }
  });

  it('exposes the planned business entry routes', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    for (const route of [
      '/pingan/three-checks/pre-shift-meeting',
      '/pingan/hazard-inspection/rectification',
      '/pingan/risk-management/risk-level-control',
      '/pingan/training/safety-learning',
      '/system/personnel-management',
    ]) {
      expect(page).toContain(route);
    }
  });

  it('uses relationship graph nodes as organization navigation controls', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).toContain('resolveOrganizationRelationship');
    expect(page).toContain('relationGraph');
    expect(page).toContain('@click="selectRelationshipNode');
    expect(page).toContain(':aria-pressed="isSelectedRelationshipNode');
    expect(page).toContain('relationGraph.groups');
    expect(page).toContain('relationGraph.chain');
  });

  it('keeps relationship graph node labels text-only without leading icons', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );
    const viewState = readFileSync(
      appSourcePath('src/views/pingan/organization/organization.view-state.ts'),
      'utf8',
    );

    expect(page).not.toContain('relation-node__icon');
    expect(page).not.toContain('relationNodeIcon');
    expect(page).not.toContain(':icon="relationNodeIcon');
    expect(page).not.toContain('chainItem.icon');
    expect(page).not.toContain('group.parent.icon');
    expect(page).not.toContain('child.icon');
    expect(viewState).not.toContain('icon: string');
    expect(viewState).not.toContain('relationNodeIcon');
  });

  it('renders explicit tree connectors between organization graph levels', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).toContain('relation-canvas');
    expect(page).toContain('relation-chain');
    expect(page).toContain('relation-root-connector');
    expect(page).toContain('relation-group-grid');
    expect(page).toContain('relation-node--card::before');
    expect(page).toContain('relation-group-grid::before');
    expect(page).toContain('relation-team-list::before');
  });

  it('keeps large relationship graphs inside their own scrollable canvas', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).toContain('height: clamp(420px, 58vh, 620px)');
    expect(page).toContain('overflow: auto');
    expect(page).toContain('overscroll-behavior: contain');
    expect(page).toContain('--relation-zoom-safe-top');
    expect(page).toContain('--relation-zoom-safe-top: 72px;');
    expect(page).toContain('--relation-canvas-side-space');
    expect(page).toContain('padding: var(--relation-zoom-safe-top) var(--relation-canvas-side-space) 86px');
    expect(page).toContain('.relation-panel {\n  overflow: hidden;');
  });

  it('keeps sibling organization cards on one continuous connector row', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).toContain('relationGraphLayoutStyle');
    expect(page).toContain('--relation-group-count');
    expect(page).toContain('overview-grid--group');
    expect(page).toContain('relation-stage');
    expect(page).toContain('grid-template-columns: repeat(auto-fit, minmax(124px, 1fr))');
    expect(page).toContain('.relation-canvas--group .relation-node--chain');
    expect(page).not.toContain('overflow-x: hidden');
  });

  it('does not show a relationship legend below the organization graph', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).not.toContain('relation-legend');
    expect(page).not.toContain('legend-line');
    expect(page).not.toContain('所属关系');
    expect(page).not.toContain('运行正常');
    expect(page).not.toContain('存在风险');
    expect(page).not.toContain('异常</span>');
  });

  it('keeps the relationship graph root separate from the selected detail organization', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).toContain('relationshipRootId');
    expect(page).toContain('syncRelationshipScope');
    expect(page).toContain("organizationType(node) === 'COMPANY'");
    expect(page).toContain('selectionSource: relationshipSelectionSource(node)');
    expect(page).toContain('syncRelationshipScope: false');
    expect(page).toContain('relationshipSelectionSource');
    expect(page).not.toContain('findNearestPathNodeByType');
    expect(page).not.toContain("organizationType(companyNode.value) === 'COMPANY'");
  });

  it('expands company children in group view without changing the graph root', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).toContain('expandGroupCompanyNode');
    expect(page).toContain("relationGraph.value?.mode === 'GROUP'");
    expect(page).toContain('relationExpandedChildIds.value = [node.id ?? node.key]');
    expect(page).toContain('syncRelationshipScope: false');
  });

  it('uses the safety-management graph visual affordances from the preview', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).toContain('relation-graph-toolbar');
    expect(page).toContain('relation-node__badge');
    expect(page).toContain('collapsedChildCount');
    expect(page).toContain('relationNodeBadgeLabel');
    expect(page).toContain('当前视图仅展示公司层级');
    expect(page).toContain('relation-zoom-tools');
    expect(page).toContain('relation-zoom-tools--top-right');
    expect(page).toContain('一键展开');
    expect(page).toContain('一键折叠');
    expect(page).toContain('放大');
    expect(page).toContain('缩小');
  });

  it('wires one-click relationship graph expand and collapse controls', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).toContain('relationExpandedChildIds');
    expect(page).toContain('relationExpandableNodeIds');
    expect(page).toContain('expandRelationGraph');
    expect(page).toContain('collapseRelationGraph');
    expect(page).toContain('@click="expandRelationGraph"');
    expect(page).toContain('@click="collapseRelationGraph"');
    expect(page).toContain(':disabled="relationExpandableNodeIds.length === 0"');
  });

  it('wires the relationship zoom tools to real graph controls', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).toContain('relationZoom');
    expect(page).toContain('zoomRelationshipGraph');
    expect(page).toContain('fitRelationshipGraph');
    expect(page).toContain('class="relation-zoom-tools relation-zoom-tools--top-right"');
    expect(page).not.toContain('relationZoomPercent');
    expect(page).not.toContain('relation-zoom-tools__value');
    expect(page).toContain('@click="fitRelationshipGraph"');
    expect(page).toContain('@click="zoomRelationshipGraph(1)"');
    expect(page).toContain('@click="zoomRelationshipGraph(-1)"');
    expect(page).toContain(':disabled="relationZoom >= maxRelationZoom"');
    expect(page).toContain(':disabled="relationZoom <= minRelationZoom"');
    expect(page).toContain('transform: `scale(${relationZoom.value})`');
  });

  it('starts from the current user organization before falling back to the root', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).toContain(
      'const initial = findInitialOrganizationNode(fullTree, userStore.userInfo?.orgId);',
    );
    expect(page).toContain('function findInitialOrganizationNode(');
    expect(page).toContain('findOrganizationPathById(nodes, orgId)');
    expect(page).not.toContain("findFirstNodeByType(fullTree, 'TEAM') ?? fullTree[0]");
  });

  it('opens group profile content in a centered modal instead of right drawer navigation', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).toContain("import { findOrganizationProfile } from './organization.profiles'");
    expect(page).toContain('Modal');
    expect(page).toContain('profileModalOpen');
    expect(page).toContain('selectedOrganizationProfile');
    expect(page).toContain('remoteOrganizationProfile');
    expect(page).toContain('loadOrganizationProfile');
    expect(page).toContain('openProfileModal');
    expect(page).toContain('profileButtonLabel');
    expect(page).toContain('集团/公司简介');
    expect(page).toContain('<Modal');
    expect(page).toContain('v-model:open="profileModalOpen"');
    expect(page).toContain('centered');
    expect(page).toContain(':title="profileButtonLabel"');
    expect(page).toContain('v-if="selectedOrganizationProfile"');
    expect(page).toContain(':src="selectedOrganizationProfile.imageUrl"');
    expect(page).toContain('selectedOrganizationProfile.imageAttachment?.url');
    expect(page).toContain('width="min(1280px, calc(100vw - 48px))"');
    expect(page).toContain('profile-modal__title');
    expect(page).toContain('profile-modal__description');
    expect(page).toContain('profile-modal__media');
    expect(page).toContain('selectedOrganizationProfile.title');
    expect(page).not.toContain('profile-modal__facts');
    expect(page).not.toContain('selectedOrganizationProfile.highlights');
    expect(page).not.toContain('<Drawer');
    expect(page).not.toContain('placement="right"');
  });

  it('uses a less dense company graph layout for departments and team lists', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).toContain('.relation-canvas--company .relation-group-grid');
    expect(page).toContain('grid-template-columns: repeat(auto-fit, minmax(190px, 1fr))');
    expect(page).toContain('.relation-canvas--company .relation-team-list');
    expect(page).toContain('grid-template-columns: repeat(auto-fit, minmax(150px, 1fr))');
    expect(page).toContain('align-items: stretch');
  });

  it('hides safety status items for group and shows checks for lower organization levels', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/organization/index.vue'),
      'utf8',
    );

    expect(page).toContain('useAccessStore');
    expect(page).toContain('canViewThreeCheckRecord');
    expect(page).toContain('loadBusinessOverviewPage');
    expect(page).toContain('isGroupSelected');
    expect(page).toContain('preShiftInspectionTotal');
    expect(page).toContain('postShiftInspectionTotal');
    expect(page).toContain(
      "loadBusinessOverviewPage('PinganPreShiftInspection', 'pre-shift-inspection'",
    );
    expect(page).toContain(
      "loadBusinessOverviewPage('PinganPostShiftInspection', 'post-shift-inspection'",
    );
    expect(page).toContain("label: '班前检查'");
    expect(page).toContain("label: '班中检查'");
    expect(page).toContain("label: '班后检查'");
    expect(page).toContain('if (isGroupSelected.value)');
    expect(page).toContain('return [];');
    expect(page).toContain('lowerLevelStatuses');
  });
});
