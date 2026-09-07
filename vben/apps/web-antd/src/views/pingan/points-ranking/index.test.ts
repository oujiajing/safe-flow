import { describe, expect, it } from 'vitest';

import {
  getSafetyPointsRankingChartsApi,
  getSafetyPointsRankingIndividualApi,
  getSafetyPointsRankingOverviewApi,
  getSafetyPointsRankingTeamApi,
} from '#/api/pingan/points-ranking';

import source from './index.vue?raw';

describe('points ranking page', () => {
  it('renders the dashboard sections from the approved UI', () => {
    expect(source).toContain('公司');
    expect(source).toContain('部门');
    expect(source).toContain('班组');
    expect(source).toContain('数据地图');
    expect(source).toContain('组织层级快速定位');
    expect(source).toContain('拖拽调整数据地图宽度');
    expect(source).toContain('日期（起）');
    expect(source).toContain('日期（止）');
    expect(source).toContain('搜索');
    expect(source).toContain('当前总积分');
    expect(source).toContain('参与人数');
    expect(source).toContain('本月加分');
    expect(source).toContain('本月兑换');
    expect(source).toContain('积分趋势');
    expect(source).toContain('积分来源占比');
    expect(source).toContain('团队 TOP5');
    expect(source).toContain('个人榜单');
    expect(source).toContain('团队榜单');
    expect(source).toContain('<Table');
  });

  it('loads the live organization tree for data map and cascaded filters', () => {
    expect(source).toContain('getPinganCompanyOrgTreeApi');
    expect(source).toContain('getPinganOrgTreeApi');
    expect(source).toContain('formOrganizationSource');
    expect(source).toContain('createDataMapResizeState');
    expect(source).toContain('canShowPinganDataMap');
    expect(source).toContain('useUserStore');
    expect(source).toContain('v-if="shouldShowDataMap"');
    expect(source).toContain('handleDataMapSelect');
    expect(source).toContain('applyOrganizationFilterFromDataMapSelection');
  });

  it('scopes organization filters by the current user role and organization', () => {
    expect(source).toContain('getVisiblePinganOrganizationFilterKeys');
    expect(source).toContain('isPinganOrganizationFilterLocked');
    expect(source).toContain('resolveScopedPinganOrganizationDefaults');
    expect(source).toContain('isOrganizationFilterVisible');
    expect(source).toContain('isOrganizationFieldLocked');
    expect(source).toContain('enforceUserOrganizationScope');
    expect(source).toContain('v-if="isOrganizationFilterVisible(\'company\')"');
    expect(source).toContain('v-if="isOrganizationFilterVisible(\'department\')"');
    expect(source).toContain('v-if="isOrganizationFilterVisible(\'team\')"');
    expect(source).toContain(":disabled=\"isOrganizationFieldLocked('company')\"");
    expect(source).toContain(":disabled=\"!filters.companyId || isOrganizationFieldLocked('department')\"");
    expect(source).toContain(":disabled=\"!filters.departmentId || isOrganizationFieldLocked('team')\"");
  });

  it('uses ranking APIs and passes team rank dimension', () => {
    expect(getSafetyPointsRankingOverviewApi).toBeTypeOf('function');
    expect(getSafetyPointsRankingChartsApi).toBeTypeOf('function');
    expect(getSafetyPointsRankingIndividualApi).toBeTypeOf('function');
    expect(getSafetyPointsRankingTeamApi).toBeTypeOf('function');
    expect(source).toContain("rankBy: teamRankBy.value");
    expect(source).toContain(
      'getSafetyPointsRankingChartsApi({',
    );
    expect(source).toContain("value: 'COMPANY'");
    expect(source).toContain("value: 'DEPARTMENT'");
    expect(source).toContain("value: 'TEAM'");
  });

  it('links the chart analysis to the selected team ranking dimension', () => {
    expect(source).toContain('teamRankTitle');
    expect(source).toContain('rankBy: teamRankBy.value');
    expect(source).toContain('按{{ teamRankTitle }}排名');
  });

  it('renders a compact source chart without crowded outer labels', () => {
    expect(source).toContain('sourceChartSlices');
    expect(source).toContain('charts.value.sources');
    expect(source).toContain('topSourceChartSlices');
    expect(source).toContain('SOURCE_CHART_LIMIT');
    expect(source).toContain('OTHER_SOURCE_NAME');
    expect(source).toContain('label: { show: false }');
    expect(source).toContain('labelLine: { show: false }');
    expect(source).toContain('source-chart-summary');
    expect(source).toContain('sourceChartTotal');
    expect(source).toContain('sourceChartMaxValue');
  });

  it('is read-only and does not include workbench mutation actions', () => {
    expect(source).not.toContain('状态筛选');
    expect(source).not.toContain('人员、班组、原因');
    expect(source).not.toContain('榜单说明');
    expect(source).not.toContain('新增');
    expect(source).not.toContain('编辑');
    expect(source).not.toContain('删除');
    expect(source).not.toContain('提交');
    expect(source).not.toContain('撤回');
    expect(source).not.toContain('催办');
  });
});
