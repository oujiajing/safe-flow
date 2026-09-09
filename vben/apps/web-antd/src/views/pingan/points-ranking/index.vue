<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';
import type { TableColumnsType } from 'ant-design-vue';
import type { CSSProperties } from 'vue';

import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue';

import { VbenScrollbar } from '@vben/common-ui';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import { useUserStore } from '@vben/stores';

import {
  Button,
  DatePicker,
  Input,
  Segmented,
  Select,
  Table,
  Tabs,
  Tree,
} from 'ant-design-vue';

import {
  getPinganCompanyOrgTreeApi,
  getPinganOrgTreeApi,
  type PinganPreShiftMeetingApi,
} from '#/api/pingan/pre-shift-meeting';
import {
  getSafetyPointsRankingChartsApi,
  getSafetyPointsRankingIndividualApi,
  getSafetyPointsRankingOverviewApi,
  getSafetyPointsRankingTeamApi,
  type PinganSafetyPointsRankingApi,
} from '#/api/pingan/points-ranking';
import {
  applyOrganizationFilterCompany,
  applyOrganizationFilterDepartment,
  applyOrganizationFilterFromDataMapSelection,
  applyOrganizationFilterTeam,
  collectExpandableOrganizationKeys,
  filterOrganizationTree,
  getCascadedOrganizationOptions,
  organizationTree,
  type OrganizationId,
  type OrganizationFilterState,
  type OrganizationNode,
} from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.data';
import {
  createDataMapCollapseState,
  createDataMapResizeState,
} from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.view-state';
import { canShowPinganDataMap } from '#/views/pingan/shared/data-map-permission';
import {
  getVisiblePinganOrganizationFilterKeys,
  isPinganOrganizationFilterLocked,
  resolveScopedPinganOrganizationDefaults,
  type PinganOrganizationFilterKey,
} from '#/views/pingan/shared/organization-filter-permission';

interface RankingFilters extends OrganizationFilterState {
  dateEnd?: string;
  dateStart?: string;
}

const userStore = useUserStore();
const filters = reactive<RankingFilters>({
  company: '',
  department: '',
  team: '',
});
const activeTab = ref<'individual' | 'team'>('individual');
const dataMapOrganizationNodes = ref<OrganizationNode[]>(organizationTree);
const expandedOrganizationKeys = ref<string[]>(
  collectExpandableOrganizationKeys(organizationTree),
);
const formOrganizationNodes = ref<OrganizationNode[]>([]);
const selectedOrganizationKeys = ref<string[]>([]);
const teamRankBy = ref<PinganSafetyPointsRankingApi.RankBy>('TEAM');
const treeKeyword = ref('');
const { dataMapToggleLabel, isDataMapCollapsed, toggleDataMap } =
  createDataMapCollapseState();
const {
  beginDataMapResize,
  dataMapWidth,
  endDataMapResize,
  isDataMapResizing,
  updateDataMapResize,
} = createDataMapResizeState();
const loading = ref(false);
const overview = ref<PinganSafetyPointsRankingApi.Overview>({
  currentTotalScore: 0,
  monthlyAddScore: 0,
  monthlyRedeemScore: 0,
  participantCount: 0,
});
const charts = ref<PinganSafetyPointsRankingApi.Charts>({
  sources: [],
  teamTop5: [],
  trend: [],
});
const individualRows = ref<PinganSafetyPointsRankingApi.IndividualRow[]>([]);
const individualTotal = ref(0);
const teamRows = ref<PinganSafetyPointsRankingApi.TeamRow[]>([]);
const teamTotal = ref(0);
const pagination = reactive({ current: 1, pageSize: 10 });

const trendChartRef = ref<EchartsUIType>();
const sourceChartRef = ref<EchartsUIType>();
const topChartRef = ref<EchartsUIType>();
const { renderEcharts: renderTrendChart } = useEcharts(trendChartRef);
const { renderEcharts: renderSourceChart } = useEcharts(sourceChartRef);
const { renderEcharts: renderTopChart } = useEcharts(topChartRef);

const filteredDataMapTree = computed(() =>
  filterOrganizationTree(dataMapOrganizationNodes.value, treeKeyword.value),
);
const formOrganizationSource = computed(() =>
  formOrganizationNodes.value.length
    ? formOrganizationNodes.value
    : organizationTree,
);
const companyOptions = computed(() =>
  getCascadedOrganizationOptions(formOrganizationSource.value, undefined, [
    'COMPANY',
  ]),
);
const departmentOptions = computed(() =>
  getCascadedOrganizationOptions(
    formOrganizationSource.value,
    filters.companyId,
    ['DEPARTMENT'],
  ),
);
const teamOptions = computed(() =>
  getCascadedOrganizationOptions(
    formOrganizationSource.value,
    filters.departmentId,
    ['TEAM'],
  ),
);

const rankingParams = computed(() => ({
  ...(filters.companyId ? { companyId: filters.companyId } : {}),
  ...(filters.departmentId ? { departmentId: filters.departmentId } : {}),
  ...(filters.teamId ? { teamId: filters.teamId } : {}),
  ...(filters.dateStart ? { dateStart: filters.dateStart } : {}),
  ...(filters.dateEnd ? { dateEnd: filters.dateEnd } : {}),
}));
const dataMapGridStyle = computed<CSSProperties>(() => ({
  '--data-map-width': `${dataMapWidth.value}px`,
}));
const shouldShowDataMap = computed(() =>
  canShowPinganDataMap(userStore.userInfo?.roles),
);
const currentUserRoles = computed(() => userStore.userInfo?.roles ?? []);
const currentUserOrgDefaults = computed(() =>
  resolveScopedPinganOrganizationDefaults(
    userStore.userInfo?.orgId,
    formOrganizationSource.value,
  ),
);
const visibleOrganizationFilterKeys = computed(() =>
  getVisiblePinganOrganizationFilterKeys(currentUserRoles.value),
);
function isOrganizationFilterVisible(field: PinganOrganizationFilterKey) {
  return visibleOrganizationFilterKeys.value.includes(field);
}
function isOrganizationFieldLocked(field: PinganOrganizationFilterKey) {
  return isPinganOrganizationFilterLocked(currentUserRoles.value, field);
}
const SOURCE_CHART_LIMIT = 6;
const OTHER_SOURCE_NAME = '其他';
const sourceChartColors = [
  '#2563eb',
  '#10b981',
  '#f59e0b',
  '#06b6d4',
  '#8b5cf6',
  '#64748b',
];
const sourceChartSlices = computed(() =>
  [...charts.value.sources]
    .filter((item) => item.value > 0)
    .sort((left, right) => right.value - left.value),
);
const topSourceChartSlices = computed(() => {
  if (sourceChartSlices.value.length <= SOURCE_CHART_LIMIT) {
    return sourceChartSlices.value;
  }
  const visibleSlices = sourceChartSlices.value.slice(0, SOURCE_CHART_LIMIT - 1);
  const otherValue = sourceChartSlices.value
    .slice(SOURCE_CHART_LIMIT - 1)
    .reduce((total, item) => total + item.value, 0);
  return [
    ...visibleSlices,
    {
      name: OTHER_SOURCE_NAME,
      value: otherValue,
    },
  ];
});
const sourceChartTotal = computed(() =>
  sourceChartSlices.value.reduce((total, item) => total + item.value, 0),
);
const sourceChartMaxValue = computed(() =>
  Math.max(...topSourceChartSlices.value.map((item) => item.value), 1),
);

const metricCards = computed(() => [
  {
    label: '当前总积分',
    tone: 'blue',
    value: overview.value.currentTotalScore,
  },
  {
    label: '参与人数',
    tone: 'cyan',
    value: overview.value.participantCount,
  },
  {
    label: '本月加分',
    tone: 'green',
    value: overview.value.monthlyAddScore,
  },
  {
    label: '本月兑换',
    tone: 'amber',
    value: overview.value.monthlyRedeemScore,
  },
]);

const teamRankOptions = [
  { label: '公司', value: 'COMPANY' },
  { label: '部门', value: 'DEPARTMENT' },
  { label: '班组', value: 'TEAM' },
];
const teamRankTitle = computed(
  () =>
    teamRankOptions.find((item) => item.value === teamRankBy.value)?.label ??
    '班组',
);

const individualColumns: TableColumnsType<PinganSafetyPointsRankingApi.IndividualRow> =
  [
    { dataIndex: 'rank', title: '排名', width: 72 },
    { dataIndex: 'userName', title: '用户', width: 120 },
    { dataIndex: 'company', title: '公司', width: 140 },
    { dataIndex: 'department', title: '部门', width: 120 },
    { dataIndex: 'team', title: '班组', width: 140 },
    { dataIndex: 'currentScore', title: '当前积分', width: 100 },
    { dataIndex: 'addScore', title: '加分', width: 88 },
    { dataIndex: 'deductScore', title: '扣分', width: 88 },
    { dataIndex: 'redeemScore', title: '兑换', width: 88 },
    { dataIndex: 'recordCount', title: '记录数', width: 88 },
    { dataIndex: 'lastBusinessDate', title: '最近日期', width: 120 },
  ];

const teamColumns: TableColumnsType<PinganSafetyPointsRankingApi.TeamRow> = [
  { dataIndex: 'rank', title: '排名', width: 72 },
  { dataIndex: 'rankName', title: '排名对象', width: 180 },
  { dataIndex: 'currentScore', title: '当前积分', width: 100 },
  { dataIndex: 'addScore', title: '加分', width: 88 },
  { dataIndex: 'deductScore', title: '扣分', width: 88 },
  { dataIndex: 'redeemScore', title: '兑换', width: 88 },
  { dataIndex: 'memberCount', title: '参与人数', width: 100 },
  { dataIndex: 'recordCount', title: '记录数', width: 88 },
  { dataIndex: 'lastBusinessDate', title: '最近日期', width: 120 },
];

function normalizeOrgNode(
  node: PinganPreShiftMeetingApi.OrgNode,
): OrganizationNode {
  return {
    children: node.children?.map(normalizeOrgNode),
    companyType: node.companyType,
    id: node.id,
    key: node.key,
    orgType: node.orgType,
    title: node.title,
  };
}

async function loadOrganizations() {
  try {
    const [apiCompanyTree, apiFullTree] = await Promise.all([
      getPinganCompanyOrgTreeApi(),
      getPinganOrgTreeApi(),
    ]);
    const companyTree = apiCompanyTree.map(normalizeOrgNode);
    dataMapOrganizationNodes.value = companyTree;
    expandedOrganizationKeys.value =
      collectExpandableOrganizationKeys(companyTree);
    formOrganizationNodes.value = apiFullTree.map(normalizeOrgNode);
    enforceUserOrganizationScope();
  } catch {
    formOrganizationNodes.value = [];
  }
}

function normalizeSelectId(value: unknown): OrganizationId | undefined {
  if (Array.isArray(value)) {
    return undefined;
  }
  return typeof value === 'number' || typeof value === 'string'
    ? value
    : undefined;
}

function applyScopedOrganizationDefaults(target: RankingFilters) {
  const defaults = currentUserOrgDefaults.value;
  if (isOrganizationFieldLocked('company') && defaults.companyId !== undefined) {
    target.companyId = defaults.companyId;
  }
  if (
    isOrganizationFieldLocked('department') &&
    defaults.departmentId !== undefined
  ) {
    target.departmentId = defaults.departmentId;
  }
  if (isOrganizationFieldLocked('team') && defaults.teamId !== undefined) {
    target.teamId = defaults.teamId;
  }
}

function enforceUserOrganizationScope() {
  if (!formOrganizationSource.value.length) {
    return;
  }
  applyScopedOrganizationDefaults(filters);
}

function onCompanyChange(value: unknown) {
  if (isOrganizationFieldLocked('company')) {
    enforceUserOrganizationScope();
    return;
  }
  selectedOrganizationKeys.value = [];
  applyOrganizationFilterCompany(
    filters,
    formOrganizationSource.value,
    normalizeSelectId(value),
  );
}

function onDepartmentChange(value: unknown) {
  if (isOrganizationFieldLocked('department')) {
    enforceUserOrganizationScope();
    return;
  }
  selectedOrganizationKeys.value = [];
  applyOrganizationFilterDepartment(
    filters,
    formOrganizationSource.value,
    normalizeSelectId(value),
  );
}

function onTeamChange(value: unknown) {
  if (isOrganizationFieldLocked('team')) {
    enforceUserOrganizationScope();
    return;
  }
  selectedOrganizationKeys.value = [];
  applyOrganizationFilterTeam(
    filters,
    formOrganizationSource.value,
    normalizeSelectId(value),
  );
}

function handleDataMapSelect(keys: unknown[]) {
  if (!shouldShowDataMap.value) {
    return;
  }
  selectedOrganizationKeys.value = keys.filter(
    (key): key is string => typeof key === 'string',
  );
  applyOrganizationFilterFromDataMapSelection(
    filters,
    dataMapOrganizationNodes.value,
    formOrganizationSource.value,
    selectedOrganizationKeys.value[0],
  );
  enforceUserOrganizationScope();
  onSearch();
}

function onSearch() {
  pagination.current = 1;
  void loadData();
}

function formatSourcePercent(value: number) {
  if (!sourceChartTotal.value) {
    return '0%';
  }
  return `${Math.round((value / sourceChartTotal.value) * 100)}%`;
}

async function loadData() {
  loading.value = true;
  const params = {
    ...rankingParams.value,
    page: pagination.current,
    pageSize: pagination.pageSize,
  };
  try {
    const [overviewData, chartsData, individualData, teamData] =
      await Promise.all([
        getSafetyPointsRankingOverviewApi(rankingParams.value),
        getSafetyPointsRankingChartsApi({
          ...rankingParams.value,
          rankBy: teamRankBy.value,
        }),
        getSafetyPointsRankingIndividualApi(params),
        getSafetyPointsRankingTeamApi({ ...params, rankBy: teamRankBy.value }),
      ]);
    overview.value = overviewData;
    charts.value = chartsData;
    individualRows.value = individualData.items;
    individualTotal.value = individualData.total;
    teamRows.value = teamData.items;
    teamTotal.value = teamData.total;
    await nextTick();
    renderCharts();
  } finally {
    loading.value = false;
  }
}

function renderCharts() {
  renderTrendChart({
    color: ['#2563eb'],
    grid: { bottom: 28, left: 42, right: 18, top: 30 },
    series: [
      {
        areaStyle: { color: 'rgba(37, 99, 235, 0.12)' },
        data: charts.value.trend.map((item) => item.score),
        smooth: true,
        symbolSize: 7,
        type: 'line',
      },
    ],
    tooltip: { trigger: 'axis' },
    xAxis: {
      axisTick: { show: false },
      data: charts.value.trend.map((item) => item.date.slice(5)),
      type: 'category',
    },
    yAxis: { splitLine: { lineStyle: { color: '#eef2f7' } }, type: 'value' },
  });
  renderSourceChart({
    color: sourceChartColors,
    legend: { show: false },
    series: [
      {
        avoidLabelOverlap: true,
        center: ['50%', '50%'],
        data: topSourceChartSlices.value,
        emphasis: {
          scaleSize: 6,
        },
        label: { show: false },
        labelLine: { show: false },
        radius: ['54%', '74%'],
        type: 'pie',
      },
    ],
    tooltip: {
      confine: true,
      formatter: (params: unknown) => {
        const item = (
          Array.isArray(params) ? params[0] : params
        ) as Record<string, unknown>;
        const percent =
          typeof item.percent === 'number' ? Math.round(item.percent) : 0;
        const value = typeof item.value === 'number' ? item.value : 0;
        return `${String(item.name ?? '')}<br/>积分：${value}<br/>占比：${percent}%`;
      },
      trigger: 'item',
    },
  });
  renderTopChart({
    color: ['#1d4ed8'],
    grid: { bottom: 20, containLabel: true, left: 8, right: 24, top: 18 },
    series: [
      {
        barMaxWidth: 14,
        data: charts.value.teamTop5.map((item) => item.score),
        type: 'bar',
      },
    ],
    tooltip: { trigger: 'axis' },
    xAxis: {
      splitLine: { lineStyle: { color: '#eef2f7' } },
      type: 'value',
    },
    yAxis: {
      axisTick: { show: false },
      data: charts.value.teamTop5.map((item) => item.name),
      type: 'category',
    },
  });
}

function onTableChange(nextPagination: { current?: number; pageSize?: number }) {
  pagination.current = nextPagination.current ?? 1;
  pagination.pageSize = nextPagination.pageSize ?? 10;
  void loadData();
}

function cleanupDataMapResizeListeners() {
  window.removeEventListener('pointermove', handleDataMapResizeMove);
  window.removeEventListener('pointerup', handleDataMapResizeEnd);
  window.removeEventListener('pointercancel', handleDataMapResizeEnd);
  document.body.style.cursor = '';
  document.body.style.userSelect = '';
}

function handleDataMapResizeMove(event: PointerEvent) {
  updateDataMapResize(event.clientX);
}

function handleDataMapResizeEnd() {
  endDataMapResize();
  cleanupDataMapResizeListeners();
}

function handleDataMapResizeStart(event: PointerEvent) {
  event.preventDefault();
  beginDataMapResize(event.clientX);
  document.body.style.cursor = 'col-resize';
  document.body.style.userSelect = 'none';
  window.addEventListener('pointermove', handleDataMapResizeMove);
  window.addEventListener('pointerup', handleDataMapResizeEnd);
  window.addEventListener('pointercancel', handleDataMapResizeEnd);
}

watch(teamRankBy, () => {
  pagination.current = 1;
  void loadData();
});

onMounted(async () => {
  await loadOrganizations();
  enforceUserOrganizationScope();
  void loadData();
});

onBeforeUnmount(() => {
  cleanupDataMapResizeListeners();
});
</script>

<template>
  <div
    :class="[
      'points-ranking-page',
      {
        'points-ranking-page--map-collapsed': isDataMapCollapsed,
        'points-ranking-page--no-map': !shouldShowDataMap,
      },
    ]"
    :style="dataMapGridStyle"
  >
    <aside
      v-if="shouldShowDataMap"
      :class="[
        'data-map',
        {
          'data-map--collapsed': isDataMapCollapsed,
          'data-map--resizing': isDataMapResizing,
        },
      ]"
    >
      <div class="data-map__header">
        <div>
          <div class="data-map__title">数据地图</div>
          <div class="data-map__subtitle">组织层级快速定位</div>
        </div>
        <Button
          :aria-expanded="!isDataMapCollapsed"
          :aria-label="dataMapToggleLabel"
          class="data-map__collapse"
          size="small"
          @click="toggleDataMap"
        >
          {{ dataMapToggleLabel }}
        </Button>
      </div>
      <div v-if="!isDataMapCollapsed" class="data-map__body">
        <Input
          v-model:value="treeKeyword"
          allow-clear
          placeholder="输入关键字搜索"
        />
        <VbenScrollbar class="data-map__tree-scroll" shadow shadow-border>
          <Tree
            v-model:expanded-keys="expandedOrganizationKeys"
            v-model:selected-keys="selectedOrganizationKeys"
            :tree-data="filteredDataMapTree"
            block-node
            class="data-map__tree"
            @select="handleDataMapSelect"
          />
        </VbenScrollbar>
      </div>
      <button
        v-if="!isDataMapCollapsed"
        aria-label="拖拽调整数据地图宽度"
        class="data-map__resize-handle"
        type="button"
        @pointerdown="handleDataMapResizeStart"
      />
    </aside>

    <main class="ranking-workbench">
      <section class="ranking-filter-bar">
        <div class="filter-grid">
          <div v-if="isOrganizationFilterVisible('company')" class="filter-item">
            <span>公司</span>
            <Select
              v-model:value="filters.companyId"
              :allow-clear="!isOrganizationFieldLocked('company')"
              class="w-full"
              :disabled="isOrganizationFieldLocked('company')"
              option-filter-prop="label"
              placeholder="点击选择"
              show-search
              :options="companyOptions"
              @change="onCompanyChange"
            />
          </div>
          <div v-if="isOrganizationFilterVisible('department')" class="filter-item">
            <span>部门</span>
            <Select
              v-model:value="filters.departmentId"
              :allow-clear="!isOrganizationFieldLocked('department')"
              class="w-full"
              :disabled="!filters.companyId || isOrganizationFieldLocked('department')"
              option-filter-prop="label"
              placeholder="点击选择"
              show-search
              :options="departmentOptions"
              @change="onDepartmentChange"
            />
          </div>
          <div v-if="isOrganizationFilterVisible('team')" class="filter-item">
            <span>班组</span>
            <Select
              v-model:value="filters.teamId"
              :allow-clear="!isOrganizationFieldLocked('team')"
              class="w-full"
              :disabled="!filters.departmentId || isOrganizationFieldLocked('team')"
              option-filter-prop="label"
              placeholder="点击选择"
              show-search
              :options="teamOptions"
              @change="onTeamChange"
            />
          </div>
          <div class="filter-item">
            <span>日期（起）</span>
            <DatePicker
              v-model:value="filters.dateStart"
              placeholder="点击选择"
              value-format="YYYY-MM-DD"
            />
          </div>
          <div class="filter-item">
            <span>日期（止）</span>
            <DatePicker
              v-model:value="filters.dateEnd"
              placeholder="点击选择"
              value-format="YYYY-MM-DD"
            />
          </div>
          <Button class="search-button" type="primary" @click="onSearch">
            搜索
          </Button>
        </div>
      </section>

      <section class="metric-grid">
        <div
          v-for="item in metricCards"
          :key="item.label"
          class="metric-card"
          :class="`metric-card--${item.tone}`"
        >
          <span class="metric-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="dashboard-grid">
        <div class="main-column">
          <div class="chart-grid">
            <section class="panel trend-panel">
              <header class="panel-header">
                <h3>积分趋势</h3>
                <span>按日累计当前积分</span>
              </header>
              <EchartsUI ref="trendChartRef" class="chart-canvas" />
            </section>
            <section class="panel">
              <header class="panel-header">
                <h3>积分来源占比</h3>
                <span>按积分变动原因统计</span>
              </header>
              <div class="source-chart-layout">
                <div class="source-chart-wrap">
                  <EchartsUI
                    ref="sourceChartRef"
                    class="chart-canvas source-chart-canvas"
                  />
                  <div class="source-chart-summary">
                    <span>来源合计</span>
                    <strong>{{ sourceChartTotal }}</strong>
                  </div>
                </div>
                <div class="source-chart-list">
                  <div
                    v-for="(item, index) in topSourceChartSlices"
                    :key="item.name"
                    class="source-chart-row"
                  >
                    <div class="source-chart-row__top">
                      <span
                        class="source-chart-dot"
                        :style="{
                          background:
                            sourceChartColors[index % sourceChartColors.length],
                        }"
                      />
                      <span class="source-chart-name" :title="item.name">
                        {{ item.name }}
                      </span>
                      <strong>{{ item.value }}</strong>
                      <em>{{ formatSourcePercent(item.value) }}</em>
                    </div>
                    <div class="source-chart-track">
                      <span
                        :style="{
                          background:
                            sourceChartColors[index % sourceChartColors.length],
                          width: `${Math.round(
                            (item.value / sourceChartMaxValue) * 100,
                          )}%`,
                        }"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </section>
            <section class="panel">
              <header class="panel-header">
                <h3>团队 TOP5</h3>
                <span>按{{ teamRankTitle }}排名</span>
              </header>
              <EchartsUI ref="topChartRef" class="chart-canvas" />
            </section>
          </div>

          <section class="panel ranking-panel">
            <div class="ranking-tabs-row">
              <Tabs v-model:active-key="activeTab" class="ranking-tabs">
                <Tabs.TabPane key="individual" tab="个人榜单" />
                <Tabs.TabPane key="team" tab="团队榜单" />
              </Tabs>
              <Segmented
                v-if="activeTab === 'team'"
                v-model:value="teamRankBy"
                :options="teamRankOptions"
              />
            </div>

            <Table
              v-if="activeTab === 'individual'"
              bordered
              :columns="individualColumns"
              :data-source="individualRows"
              :loading="loading"
              :pagination="{
                current: pagination.current,
                pageSize: pagination.pageSize,
                showSizeChanger: true,
                total: individualTotal,
              }"
              row-key="userName"
              :scroll="{ x: 1180 }"
              size="middle"
              @change="onTableChange"
            />
            <Table
              v-else
              bordered
              :columns="teamColumns"
              :data-source="teamRows"
              :loading="loading"
              :pagination="{
                current: pagination.current,
                pageSize: pagination.pageSize,
                showSizeChanger: true,
                total: teamTotal,
              }"
              row-key="rankId"
              :scroll="{ x: 960 }"
              size="middle"
              @change="onTableChange"
            />
          </section>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.points-ranking-page {
  display: grid;
  grid-template-columns: minmax(220px, var(--data-map-width, 280px)) minmax(
      0,
      1fr
    );
  gap: 12px;
  min-height: 100%;
  padding: 16px;
  background: #f5f7fb;
  color: #172033;
  transition: grid-template-columns 0.2s ease;
}

.points-ranking-page--map-collapsed {
  grid-template-columns: 56px minmax(0, 1fr);
}

.points-ranking-page--no-map {
  grid-template-columns: minmax(0, 1fr);
}

.data-map,
.ranking-workbench {
  min-width: 0;
}

.data-map {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  padding: 14px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 6px;
}

.data-map__header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.data-map__header > div {
  min-width: 0;
}

.data-map__title {
  color: #1f2937;
  font-size: 15px;
  font-weight: 700;
}

.data-map__subtitle {
  margin-top: 3px;
  color: #7c8798;
  font-size: 12px;
}

.data-map__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  min-height: 320px;
}

.data-map__tree-scroll {
  flex: 1;
  min-height: 0;
  --scroll-shadow: 0 0% 100%;
}

.data-map__tree {
  padding-right: 6px;
}

.data-map__resize-handle {
  position: absolute;
  top: 0;
  right: -7px;
  z-index: 4;
  width: 14px;
  height: 100%;
  padding: 0;
  background: transparent;
  border: 0;
  outline: none;
  cursor: col-resize;
  touch-action: none;
}

.data-map__resize-handle::after {
  position: absolute;
  top: 12px;
  bottom: 12px;
  left: 6px;
  width: 2px;
  content: '';
  background: transparent;
  border-radius: 999px;
  transition:
    background 0.15s ease,
    opacity 0.15s ease;
}

.data-map__resize-handle:focus-visible::after,
.data-map__resize-handle:hover::after,
.data-map--resizing .data-map__resize-handle::after {
  background: rgb(59 130 246 / 65%);
}

.data-map--collapsed {
  align-items: center;
  gap: 8px;
  padding: 12px 8px;
}

.data-map--collapsed .data-map__header {
  flex: 1;
  flex-direction: column;
  justify-content: flex-start;
  width: 100%;
}

.data-map--collapsed .data-map__header > div {
  display: flex;
  justify-content: center;
}

.data-map--collapsed .data-map__title {
  line-height: 1.3;
  letter-spacing: 4px;
  writing-mode: vertical-rl;
}

.data-map--collapsed .data-map__subtitle {
  display: none;
}

.ranking-workbench {
  min-height: 0;
}

.ranking-filter-bar {
  padding: 14px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 6px;
}

.filter-grid {
  display: grid;
  grid-template-columns:
    minmax(150px, 1fr) minmax(150px, 1fr) minmax(150px, 1fr)
    minmax(140px, 0.8fr) minmax(140px, 0.8fr) 86px;
  gap: 12px;
  align-items: end;
}

.filter-item {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.filter-item span {
  margin-bottom: 6px;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.filter-item :deep(.ant-picker),
.filter-item :deep(.ant-select),
.filter-item :deep(.ant-input),
.filter-item :deep(.ant-input-affix-wrapper) {
  width: 100%;
  height: 38px;
  min-height: 38px;
}

.filter-item :deep(.ant-input-affix-wrapper),
.filter-item :deep(.ant-picker),
.filter-item :deep(.ant-select-single .ant-select-selector) {
  display: flex;
  align-items: center;
}

.filter-item :deep(.ant-select-single .ant-select-selector) {
  height: 38px;
}

.filter-item :deep(.ant-select-single .ant-select-selection-item),
.filter-item :deep(.ant-select-single .ant-select-selection-placeholder) {
  line-height: 36px;
}

.search-button {
  height: 38px;
  min-width: 72px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.metric-card {
  position: relative;
  min-height: 86px;
  padding: 16px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 6px;
}

.metric-card::after {
  position: absolute;
  right: 14px;
  bottom: 14px;
  width: 42px;
  height: 42px;
  content: '';
  border-radius: 50%;
  opacity: 0.14;
}

.metric-label {
  display: block;
  margin-bottom: 10px;
  color: #667085;
  font-size: 13px;
}

.metric-card strong {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.metric-card--blue strong {
  color: #2563eb;
}

.metric-card--blue::after {
  background: #2563eb;
}

.metric-card--cyan strong {
  color: #0891b2;
}

.metric-card--cyan::after {
  background: #0891b2;
}

.metric-card--green strong {
  color: #059669;
}

.metric-card--green::after {
  background: #059669;
}

.metric-card--amber strong {
  color: #d97706;
}

.metric-card--amber::after {
  background: #d97706;
}

.dashboard-grid {
  margin-top: 12px;
}

.main-column {
  min-width: 0;
}

.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.9fr) minmax(0, 0.9fr);
  gap: 12px;
}

.panel {
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 6px;
}

.panel-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 14px 16px 0;
}

.panel-header h3 {
  margin: 0;
  color: #172033;
  font-size: 15px;
  font-weight: 700;
}

.panel-header span {
  color: #8a95a6;
  font-size: 12px;
}

.chart-canvas {
  width: 100%;
  height: 260px;
}

.source-chart-layout {
  display: grid;
  grid-template-columns: minmax(116px, 0.72fr) minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  height: 260px;
  padding: 0 14px 10px 10px;
}

.source-chart-wrap {
  position: relative;
  min-width: 0;
}

.source-chart-canvas {
  height: 228px;
}

.source-chart-summary {
  position: absolute;
  top: 50%;
  left: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 72px;
  pointer-events: none;
  transform: translate(-50%, -50%);
}

.source-chart-summary span {
  color: #8a95a6;
  font-size: 11px;
  line-height: 1.2;
}

.source-chart-summary strong {
  margin-top: 3px;
  color: #172033;
  font-size: 20px;
  line-height: 1;
}

.source-chart-list {
  display: flex;
  flex-direction: column;
  gap: 9px;
  min-width: 0;
}

.source-chart-row {
  min-width: 0;
}

.source-chart-row__top {
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr) auto auto;
  gap: 6px;
  align-items: center;
  min-width: 0;
  color: #667085;
  font-size: 12px;
}

.source-chart-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.source-chart-name {
  min-width: 0;
  overflow: hidden;
  color: #344054;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-chart-row__top strong {
  color: #172033;
  font-size: 12px;
}

.source-chart-row__top em {
  min-width: 34px;
  color: #98a2b3;
  font-style: normal;
  text-align: right;
}

.source-chart-track {
  height: 4px;
  margin-top: 5px;
  overflow: hidden;
  background: #eef2f7;
  border-radius: 999px;
}

.source-chart-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.ranking-panel {
  margin-top: 12px;
  padding: 0 12px 12px;
  min-height: 380px;
}

.ranking-tabs-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 56px;
}

.ranking-tabs {
  flex: 1;
}

.ranking-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}

.ranking-panel :deep(.ant-table-thead > tr > th) {
  color: #475467;
  font-weight: 600;
  background: #f8fafc;
}

.ranking-panel :deep(.ant-table-cell) {
  white-space: nowrap;
}

.ranking-panel :deep(.ant-table-wrapper) {
  min-height: 306px;
}

@media (max-width: 960px) {
  .points-ranking-page,
  .points-ranking-page--map-collapsed {
    grid-template-columns: 1fr;
  }

  .data-map--collapsed {
    align-items: stretch;
  }

  .data-map--collapsed .data-map__header {
    flex-direction: row;
  }

  .data-map--collapsed .data-map__title {
    letter-spacing: 0;
    writing-mode: horizontal-tb;
  }

  .filter-grid,
  .metric-grid,
  .chart-grid {
    grid-template-columns: 1fr 1fr;
  }

  .search-button {
    width: 100%;
  }

  .trend-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 640px) {
  .points-ranking-page {
    padding: 10px;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .metric-grid,
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .ranking-tabs-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
    padding: 10px 0;
  }
}
</style>
