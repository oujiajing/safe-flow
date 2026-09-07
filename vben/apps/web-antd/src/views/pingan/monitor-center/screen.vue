<script setup lang="ts">
import type { EchartsUIType } from '@vben/plugins/echarts';

import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

import { useFullscreen } from '@vueuse/core';
import { DatePicker, Select } from 'ant-design-vue';

import {
  getMonitorCenterOverviewApi,
  type PinganMonitorCenterApi,
} from '#/api/pingan/monitor-center';
import {
  getScreenContentProfileVideosApi,
  type PinganContentProfileApi,
} from '#/api/pingan/content-profile';

interface CompanyChartRow {
  fullName: string;
  shortName: string;
  total: number;
}

type PanelKey = 'dispatch' | 'hazard' | 'risk' | 'threeCheck';
type PanelView = 'chart' | 'detail';

const seriesColors: Record<string, string> = {
  班中检查: '#ff9f0a',
  班前会: '#2f80ed',
  班前检查: '#39c5bb',
  班后检查: '#8b5cf6',
};

const riskColors: Record<string, string> = {
  重大风险: '#ff3b30',
  较大风险: '#ff9f0a',
  一般风险: '#ffd60a',
  低风险: '#2f80ed',
};

const hazardColors = {
  done: '#66c56c', // 已整改 绿色
  open: '#ff9f0a', // 未整改 橙色
  total: '#2f80ed',
};

const screenCompanyOptions: Array<{
  label: string;
  value: PinganMonitorCenterApi.Id | 'all-company';
}> = [
  { label: '全部公司', value: 'all-company' },
  { label: '广晟幕墙', value: 3 },
  { label: '广晟源成', value: 4 },
  { label: '资源公司', value: 24 },
  { label: '广晟新材', value: 28 },
];
const screenCompanyNames = screenCompanyOptions
  .filter((item) => item.value !== 'all-company')
  .map((item) => item.label);

const overview = ref<PinganMonitorCenterApi.Overview>(createEmptyOverview());
const isOverviewLoading = ref(false);
const overviewLoadError = ref('');
const selectedCompanyId = ref<PinganMonitorCenterApi.Id | 'all-company'>(
  'all-company',
);
const selectedDateRange = ref<[string, string]>([
  formatDateValue(new Date()),
  formatDateValue(new Date()),
]);
const screenVideos = ref<PinganContentProfileApi.ContentProfile[]>([]);
const activeVisionVideoIndex = ref(0);
const screenFullscreenRef = ref<HTMLElement>();
const currentDateTime = ref(formatDateTime(new Date()));
const dispatchChartRef = ref<EchartsUIType>();
const threeCheckChartRef = ref<EchartsUIType>();
const riskChartRef = ref<EchartsUIType>();
const hazardPieRef = ref<EchartsUIType>();
const hazardBarRef = ref<EchartsUIType>();
const panelViews = ref<Record<PanelKey, PanelView>>({
  dispatch: 'chart',
  hazard: 'chart',
  risk: 'chart',
  threeCheck: 'chart',
});

const { isFullscreen, toggle: toggleFullscreen } =
  useFullscreen(screenFullscreenRef);
const { renderEcharts: renderDispatchChart } = useEcharts(dispatchChartRef);
const { renderEcharts: renderThreeCheckChart } = useEcharts(threeCheckChartRef);
const { renderEcharts: renderRiskChart } = useEcharts(riskChartRef);
const { renderEcharts: renderHazardPie } = useEcharts(hazardPieRef);
const { renderEcharts: renderHazardBar } = useEcharts(hazardBarRef);

const chartTextColor = '#d9efff';
const companyLabelColor = '#f2f8ff';
const legendTextColor = '#dff3ff';
const gridLineColor = 'rgba(176, 220, 255, 0.5)';
let clockTimer: number | undefined;

const summary = computed(() => {
  const source = overview.value.summary;
  return {
    ...source,
    dispatchFinished: toNumber(source.dispatchFinished),
    dispatchTotal: toNumber(source.dispatchTotal),
    hazardDone: toNumber(source.hazardDone),
    hazardOpen: toNumber(source.hazardOpen),
    hazardTotal: toNumber(source.hazardTotal),
    riskTotal: toNumber(source.riskTotal),
    teamCount: toNumber(source.teamCount),
  };
});
const dispatchBars = computed(() => {
  const rows = overview.value.dispatchBars.map((item, index) => ({
    ...companyLabel(item.name, index),
    total: toNumber(item.value),
  }));
  return sortCompanyRows(rows);
});
const dispatchDetailRows = computed(() => {
  const rows = overview.value.dispatchBars.map((item, index) => ({
    ...companyLabel(item.name, index),
    total: toNumber(item.value),
  }));
  return sortCompanyRows(rows, 0);
});
const threeCheckSeries = computed(() =>
  overview.value.threeCheckSeries.map((item) => ({
    ...item,
    color: seriesColors[item.name] ?? '#8ec5ff',
    data: item.data.map((value) => toNumber(value)),
  })),
);
const threeCheckRows = computed(() => {
  const rowCount = Math.max(
    ...threeCheckSeries.value.map((item) => item.data.length),
    screenCompanyNames.length,
  );
  const rows = Array.from({ length: rowCount }, (_, index) => {
    const values = Object.fromEntries(
      threeCheckSeries.value.map((item) => [
        item.name,
        toNumber(item.data[index]),
      ]),
    ) as Record<string, number>;
    const total = Object.values(values).reduce((sum, value) => sum + value, 0);
    return {
      ...companyLabel(undefined, index),
      total,
      values,
    };
  });
  return sortCompanyRows(rows);
});
const threeCheckDetailRows = computed(() => {
  const rowCount = Math.max(
    ...threeCheckSeries.value.map((item) => item.data.length),
    screenCompanyNames.length,
  );
  const rows = Array.from({ length: rowCount }, (_, index) => {
    const values = Object.fromEntries(
      threeCheckSeries.value.map((item) => [
        item.name,
        toNumber(item.data[index]),
      ]),
    ) as Record<string, number>;
    const total = Object.values(values).reduce((sum, value) => sum + value, 0);
    return {
      ...companyLabel(undefined, index),
      total,
      values,
    };
  });
  return sortCompanyRows(rows, 0);
});
const riskControlBars = computed(() => {
  const rows = overview.value.riskControlBars.map((item, index) => {
    const major = toNumber(item.major);
    const serious = toNumber(item.serious);
    const normal = toNumber(item.normal);
    const low = toNumber(item.low);
    return {
      ...companyLabel(item.name, index),
      low,
      major,
      normal,
      serious,
      total: major + serious + normal + low,
    };
  });
  return sortCompanyRows(rows);
});
const riskControlDetailRows = computed(() => {
  const rows = overview.value.riskControlBars.map((item, index) => {
    const major = toNumber(item.major);
    const serious = toNumber(item.serious);
    const normal = toNumber(item.normal);
    const low = toNumber(item.low);
    return {
      ...companyLabel(item.name, index),
      low,
      major,
      normal,
      serious,
      total: major + serious + normal + low,
    };
  });
  return sortCompanyRows(rows, 0);
});
const hazardBars = computed(() => {
  const rows = overview.value.hazardBars.map((item, index) => {
    const done = toNumber(item.done);
    const open = toNumber(item.open);
    return {
      ...companyLabel(item.name, index),
      done,
      open,
      total: done + open,
    };
  });
  return sortCompanyRows(rows);
});
const hazardDetailRows = computed(() => {
  const rows = overview.value.hazardBars.map((item, index) => {
    const done = toNumber(item.done);
    const open = toNumber(item.open);
    return {
      ...companyLabel(item.name, index),
      done,
      open,
      total: done + open,
    };
  });
  return sortCompanyRows(rows, 0);
});
const hazardRectificationPie = computed(() => [
  {
    itemStyle: { color: hazardColors.done },
    name: '已整改',
    value: summary.value.hazardDone,
  },
  {
    itemStyle: { color: hazardColors.open },
    name: '未整改',
    value: summary.value.hazardOpen,
  },
]);
const threeCheckTotals = computed(() =>
  Object.fromEntries(
    threeCheckSeries.value.map((item) => [
      item.name,
      item.data.reduce((total, value) => total + value, 0),
    ]),
  ),
);
const riskTotals = computed(() =>
  riskControlBars.value.reduce(
    (total, item) => ({
      low: total.low + item.low,
      major: total.major + item.major,
      normal: total.normal + item.normal,
      serious: total.serious + item.serious,
    }),
    { low: 0, major: 0, normal: 0, serious: 0 },
  ),
);
const activeVisionVideo = computed(() => {
  if (screenVideos.value.length === 0) {
    return undefined;
  }
  return screenVideos.value[
    activeVisionVideoIndex.value % screenVideos.value.length
  ];
});
onMounted(() => {
  void loadMonitorOverview();
  void loadScreenVideos();
  clockTimer = window.setInterval(() => {
    currentDateTime.value = formatDateTime(new Date());
  }, 1000);
});

onBeforeUnmount(() => {
  if (clockTimer) {
    window.clearInterval(clockTimer);
  }
});

async function loadMonitorOverview() {
  isOverviewLoading.value = true;
  overviewLoadError.value = '';
  try {
    const [dateStart, dateEnd] = selectedDateRange.value;
    overview.value = normalizeOverview(
      await getMonitorCenterOverviewApi({
        dateEnd,
        dateStart,
        orgId:
          selectedCompanyId.value === 'all-company'
            ? undefined
            : selectedCompanyId.value,
      }),
    );
  } catch {
    overview.value = createEmptyOverview();
    overviewLoadError.value = '业务模块汇总数据加载失败';
  } finally {
    isOverviewLoading.value = false;
    renderCharts();
  }
}

function createEmptyOverview(): PinganMonitorCenterApi.Overview {
  return {
    companies: [],
    dateEnd: '',
    dateStart: '',
    dispatchBars: [],
    dispatchFinishedBars: [],
    dispatchTrend: [],
    hazardBars: [],
    hazardRecordRows: [],
    hazardRectificationPie: [],
    hazardRectificationOrders: {
      activeOpen: 0,
      cancelled: 0,
      closed: 0,
      total: 0,
    },
    hazardStatusBars: [],
    learningCategoryBars: [],
    learningBars: [],
    learningTrend: [],
    legacyHazardRectificationRecords: {
      total: 0,
    },
    riskAccidentTypeBars: [],
    riskCauseBars: [],
    riskControlBars: [],
    scopeOptions: [],
    specialWorkTypeBars: [],
    summary: {
      dispatchFinished: 0,
      dispatchTotal: 0,
      hazardDone: 0,
      hazardFixedRate: '0.00%',
      hazardOpen: 0,
      hazardTotal: 0,
      learningToday: 0,
      learningTotal: 0,
      riskTotal: 0,
      teamCount: 0,
      threeCheckRates: {
        mid: '0.00%',
        post: '0.00%',
        pre: '0.00%',
        preInspection: '0.00%',
      },
    },
    teamAnalysis: {
      detailRows: [],
      heatmapRows: [],
      metricRankings: {
        openHazardTopTeams: [],
        threeCheckTopTeams: [],
      },
      summary: {
        abnormalTeams: 0,
        averageCompletionRate: '0.00%',
        normalTeams: 0,
        openHazards: 0,
        teamCount: 0,
        unfinishedThreeChecks: 0,
      },
    },
    threeCheckSeries: [],
  };
}

function normalizeOverview(
  source: PinganMonitorCenterApi.Overview,
): PinganMonitorCenterApi.Overview {
  const empty = createEmptyOverview();
  return {
    ...empty,
    ...source,
    companies: source.companies ?? [],
    dispatchBars: source.dispatchBars ?? [],
    dispatchFinishedBars: source.dispatchFinishedBars ?? [],
    hazardBars: source.hazardBars ?? [],
    hazardRecordRows: source.hazardRecordRows ?? [],
    hazardRectificationPie: source.hazardRectificationPie ?? [],
    hazardRectificationOrders: {
      ...empty.hazardRectificationOrders,
      ...(source.hazardRectificationOrders ?? {}),
    },
    learningBars: source.learningBars ?? [],
    learningTrend: source.learningTrend ?? [],
    legacyHazardRectificationRecords: {
      ...empty.legacyHazardRectificationRecords,
      ...(source.legacyHazardRectificationRecords ?? {}),
    },
    riskAccidentTypeBars: source.riskAccidentTypeBars ?? [],
    riskCauseBars: source.riskCauseBars ?? [],
    riskControlBars: source.riskControlBars ?? [],
    scopeOptions: source.scopeOptions ?? [],
    specialWorkTypeBars: source.specialWorkTypeBars ?? [],
    summary: {
      ...empty.summary,
      ...(source.summary ?? {}),
      threeCheckRates: {
        ...empty.summary.threeCheckRates,
        ...(source.summary?.threeCheckRates ?? {}),
      },
    },
    teamAnalysis: {
      ...empty.teamAnalysis,
      ...(source.teamAnalysis ?? {}),
      detailRows: source.teamAnalysis?.detailRows ?? [],
      heatmapRows: source.teamAnalysis?.heatmapRows ?? [],
      metricRankings: {
        ...empty.teamAnalysis.metricRankings,
        ...(source.teamAnalysis?.metricRankings ?? {}),
        openHazardTopTeams:
          source.teamAnalysis?.metricRankings?.openHazardTopTeams ?? [],
        threeCheckTopTeams:
          source.teamAnalysis?.metricRankings?.threeCheckTopTeams ?? [],
      },
      summary: {
        ...empty.teamAnalysis.summary,
        ...(source.teamAnalysis?.summary ?? {}),
      },
    },
    threeCheckSeries: source.threeCheckSeries ?? [],
  };
}

function handleScreenFilterChange() {
  void loadMonitorOverview();
  void loadScreenVideos();
}

function setPanelView(panel: PanelKey, view: PanelView) {
  panelViews.value[panel] = view;
  if (view === 'chart') {
    void nextTick(renderCharts);
  }
}

async function loadScreenVideos() {
  try {
    activeVisionVideoIndex.value = 0;
    screenVideos.value = await getScreenContentProfileVideosApi({
      orgId:
        selectedCompanyId.value === 'all-company' ? undefined : selectedCompanyId.value,
    });
  } catch {
    screenVideos.value = [];
  }
}

function handleVisionVideoEnded() {
  if (screenVideos.value.length <= 1) {
    return;
  }
  activeVisionVideoIndex.value =
    (activeVisionVideoIndex.value + 1) % screenVideos.value.length;
}

function toNumber(value: unknown) {
  const numberValue = Number(value ?? 0);
  return Number.isFinite(numberValue) ? numberValue : 0;
}

function formatDateTime(value: Date) {
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  const hours = String(value.getHours()).padStart(2, '0');
  const minutes = String(value.getMinutes()).padStart(2, '0');
  const seconds = String(value.getSeconds()).padStart(2, '0');
  return `${value.getFullYear()}年${month}月${day}日 ${hours}:${minutes}:${seconds}`;
}

function formatDateValue(value: Date) {
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${value.getFullYear()}-${month}-${day}`;
}

function companyLabel(sourceName: string | undefined, index: number) {
  const shortName = screenCompanyNames[index % screenCompanyNames.length] ?? '广晟公司';
  return {
    fullName: shortName,
    originalName: sourceName ?? shortName,
    shortName,
  };
}

function sortCompanyRows<T extends CompanyChartRow>(rows: T[], limit = 4) {
  const sortedRows = [...rows].sort((left, right) => right.total - left.total);
  return limit > 0 ? sortedRows.slice(0, limit) : sortedRows;
}

function dynamicAxisMax(values: number[]) {
  const maxValue = Math.max(...values, 0);
  if (maxValue <= 4) {
    return 4;
  }
  return Math.ceil((maxValue * 1.18) / 2) * 2;
}

function chartGrid(top = 34, bottom = 34, left = 70, right = 28) {
  return {
    bottom,
    containLabel: true,
    left,
    right,
    top,
  };
}

function companyCategoryAxis(names: string[]) {
  return {
    axisLabel: {
      color: companyLabelColor,
      fontSize: 13,
      fontWeight: 700,
      hideOverlap: true,
      interval: 0,
      margin: 14,
      overflow: 'truncate',
      width: 64,
    },
    axisLine: { lineStyle: { color: '#89c9ff' } },
    axisTick: { show: false },
    data: names,
    inverse: true,
    type: 'category',
  } as const;
}

function valueAxis(max?: number) {
  return {
    axisLabel: { color: chartTextColor, fontSize: 11 },
    max,
    splitLine: { lineStyle: { color: gridLineColor } },
    type: 'value',
  } as const;
}

const hideZeroLabel = {
  color: '#d8ecff',
  formatter: (params: { value?: unknown }) =>
    toNumber(params.value) > 0 ? String(params.value) : '',
  position: 'right',
  show: true,
} as const;

function companyTooltip(rows: CompanyChartRow[]) {
  return {
    formatter: (params: unknown) => {
      const items = (Array.isArray(params) ? params : [params]) as Array<{
        dataIndex?: number;
        marker?: string;
        seriesName?: string;
        value?: unknown;
      }>;
      const first = items[0];
      const row = rows[toNumber(first?.dataIndex)];
      const lines = items
        .filter((item) => toNumber(item.value) > 0)
        .map((item) => `${item.marker ?? ''}${item.seriesName ?? ''}: ${item.value}`)
        .join('<br/>');
      return `${row?.fullName ?? ''}<br/>${lines}`;
    },
    trigger: 'axis' as const,
  };
}

function renderCharts() {
  const dispatchRows = dispatchBars.value;
  const threeRows = threeCheckRows.value;
  const riskRows = riskControlBars.value;
  const hazardRows = hazardBars.value;

  renderDispatchChart({
    grid: chartGrid(16),
    series: [
      {
        barWidth: 18,
        data: dispatchRows.map((item) => item.total),
        itemStyle: { borderRadius: [0, 10, 10, 0], color: '#24d2f4' },
        label: hideZeroLabel,
        name: '派班数',
        type: 'bar',
      },
    ],
    tooltip: companyTooltip(dispatchRows),
    xAxis: valueAxis(dynamicAxisMax(dispatchRows.map((item) => item.total))),
    yAxis: companyCategoryAxis(dispatchRows.map((item) => item.shortName)),
  });

  renderThreeCheckChart({
    grid: chartGrid(42),
    legend: {
      data: threeCheckSeries.value.map((item) => item.name),
      icon: 'roundRect',
      itemHeight: 9,
      itemWidth: 12,
      textStyle: { color: legendTextColor, fontSize: 12, fontWeight: 700 },
      top: 0,
    },
    series: threeCheckSeries.value.map((item) => ({
      barWidth: 18,
      data: threeRows.map((row) => row.values[item.name] ?? 0),
      itemStyle: { color: item.color },
      label: hideZeroLabel,
      name: item.name,
      stack: 'three-checks',
      type: 'bar',
    })),
    tooltip: companyTooltip(threeRows),
    xAxis: valueAxis(dynamicAxisMax(threeRows.map((item) => item.total))),
    yAxis: companyCategoryAxis(threeRows.map((item) => item.shortName)),
  });

  renderRiskChart({
    grid: chartGrid(58, 40, 70, 36),
    legend: {
      data: ['重大风险', '较大风险', '一般风险', '低风险'],
      icon: 'roundRect',
      itemHeight: 10,
      itemWidth: 10,
      textStyle: { color: legendTextColor, fontSize: 12, fontWeight: 700 },
      top: 0,
    },
    series: [
      {
        barWidth: 20,
        data: riskRows.map((item) => item.major),
        itemStyle: { color: riskColors.重大风险 },
        label: hideZeroLabel,
        name: '重大风险',
        stack: 'risk',
        type: 'bar',
      },
      {
        barWidth: 20,
        data: riskRows.map((item) => item.serious),
        itemStyle: { color: riskColors.较大风险 },
        label: hideZeroLabel,
        name: '较大风险',
        stack: 'risk',
        type: 'bar',
      },
      {
        barWidth: 20,
        data: riskRows.map((item) => item.normal),
        itemStyle: { color: riskColors.一般风险 },
        label: hideZeroLabel,
        name: '一般风险',
        stack: 'risk',
        type: 'bar',
      },
      {
        barWidth: 20,
        data: riskRows.map((item) => item.low),
        itemStyle: { color: riskColors.低风险 },
        label: hideZeroLabel,
        name: '低风险',
        stack: 'risk',
        type: 'bar',
      },
    ],
    tooltip: companyTooltip(riskRows),
    xAxis: valueAxis(dynamicAxisMax(riskRows.map((item) => item.total))),
    yAxis: companyCategoryAxis(riskRows.map((item) => item.shortName)),
  });

  renderHazardPie({
    legend: {
      left: 'center',
      textStyle: { color: legendTextColor, fontSize: 12, fontWeight: 700 },
      top: 0,
    },
    series: [
      {
        data: hazardRectificationPie.value,
        label: {
          color: '#d9efff',
          formatter: (params: { name?: string; percent?: number; value?: unknown }) =>
            toNumber(params.value) > 0 ? `${params.name}: ${params.percent}%` : '',
        },
        radius: ['48%', '68%'],
        type: 'pie',
      },
    ],
    tooltip: { trigger: 'item' },
  });

  renderHazardBar({
    grid: chartGrid(48, 34, 70, 28),
    legend: {
      data: ['隐患总数', '已整改隐患', '未整改隐患'],
      icon: 'roundRect',
      itemHeight: 10,
      itemWidth: 12,
      textStyle: { color: legendTextColor, fontSize: 12, fontWeight: 700 },
      top: 0,
    },
    series: [
      {
        barWidth: 18,
        data: hazardRows.map((item) => item.total),
        itemStyle: { color: hazardColors.total },
        label: hideZeroLabel,
        name: '隐患总数',
        type: 'bar',
      },
      {
        barWidth: 18,
        data: hazardRows.map((item) => item.done),
        itemStyle: { color: hazardColors.done },
        label: hideZeroLabel,
        name: '已整改隐患',
        stack: 'hazard-status',
        type: 'bar',
      },
      {
        barWidth: 18,
        data: hazardRows.map((item) => item.open),
        itemStyle: { color: hazardColors.open },
        label: hideZeroLabel,
        name: '未整改隐患',
        stack: 'hazard-status',
        type: 'bar',
      },
    ],
    tooltip: companyTooltip(hazardRows),
    xAxis: valueAxis(dynamicAxisMax(hazardRows.map((item) => item.total))),
    yAxis: companyCategoryAxis(hazardRows.map((item) => item.shortName)),
  });
}
</script>

<template>
  <main ref="screenFullscreenRef" class="monitor-screen">
    <div class="screen-bg"></div>
    <header class="screen-header">
      <div class="header-left">
        <span class="weather-chip"><IconifyIcon icon="lucide:cloud" />多云 26°C</span>
        <Select
          v-model:value="selectedCompanyId"
          :options="screenCompanyOptions"
          class="screen-company-select"
          popup-class-name="screen-filter-popup"
          @change="handleScreenFilterChange"
        />
        <DatePicker.RangePicker
          v-model:value="selectedDateRange"
          class="screen-date-picker"
          value-format="YYYY-MM-DD"
          @change="handleScreenFilterChange"
        />
        <span v-if="overviewLoadError" class="data-source-status">
          {{ overviewLoadError }}
        </span>
        <span v-else-if="isOverviewLoading" class="data-source-status">
          正在加载业务模块数据
        </span>
      </div>
      <div class="header-title">
        <h1>广晟矿业平安班组监控中心</h1>
      </div>
      <div class="header-right">
        <span class="date-range">{{ currentDateTime }}</span>
        <button class="fullscreen-button" type="button" @click="toggleFullscreen">
          <IconifyIcon :icon="isFullscreen ? 'lucide:minimize' : 'lucide:maximize'" />
          {{ isFullscreen ? '退出全屏' : '全屏显示' }}
        </button>
      </div>
    </header>

    <section class="screen-grid">
      <aside class="left-column">
        <section class="screen-panel panel--dispatch">
          <div class="panel-title">
            <span><IconifyIcon icon="lucide:book-open-check" />作业派班</span>
            <div class="panel-view-toggle" aria-label="作业派班视图切换">
              <button
                :class="{ active: panelViews.dispatch === 'chart' }"
                type="button"
                @click="setPanelView('dispatch', 'chart')"
              >
                图表
              </button>
              <button
                :class="{ active: panelViews.dispatch === 'detail' }"
                type="button"
                @click="setPanelView('dispatch', 'detail')"
              >
                明细
              </button>
            </div>
          </div>
          <template v-if="panelViews.dispatch === 'chart'">
            <div class="summary-row">
              <div><span>班组数</span><strong>{{ summary.teamCount }}</strong></div>
              <div><span>作业派班数</span><strong>{{ summary.dispatchTotal }}</strong></div>
            </div>
            <h3 class="chart-caption">派班数 Top N</h3>
            <EchartsUI ref="dispatchChartRef" class="chart dispatch-chart" />
            <div class="metric-strip">
              <span>已完成 {{ summary.dispatchFinished }}</span>
              <span>派班总数 {{ summary.dispatchTotal }}</span>
            </div>
          </template>
          <div v-else class="screen-detail-table-wrap">
            <table class="screen-detail-table">
              <thead>
                <tr>
                  <th>公司</th>
                  <th>派班完成数</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in dispatchDetailRows" :key="item.fullName">
                  <td>{{ item.shortName }}</td>
                  <td>{{ item.total }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="screen-panel panel--three-check">
          <div class="panel-title">
            <span><IconifyIcon icon="lucide:clipboard-check" />一班三查</span>
            <div class="panel-view-toggle" aria-label="一班三查视图切换">
              <button
                :class="{ active: panelViews.threeCheck === 'chart' }"
                type="button"
                @click="setPanelView('threeCheck', 'chart')"
              >
                图表
              </button>
              <button
                :class="{ active: panelViews.threeCheck === 'detail' }"
                type="button"
                @click="setPanelView('threeCheck', 'detail')"
              >
                明细
              </button>
            </div>
          </div>
          <template v-if="panelViews.threeCheck === 'chart'">
            <div class="summary-row summary-row--four">
              <div><span>班前会</span><strong>{{ threeCheckTotals['班前会'] ?? 0 }}</strong></div>
              <div><span>班前检查</span><strong>{{ threeCheckTotals['班前检查'] ?? 0 }}</strong></div>
              <div><span>班中检查</span><strong>{{ threeCheckTotals['班中检查'] ?? 0 }}</strong></div>
              <div><span>班后检查</span><strong>{{ threeCheckTotals['班后检查'] ?? 0 }}</strong></div>
            </div>
            <h3 class="chart-caption">检查数 Top N</h3>
            <EchartsUI ref="threeCheckChartRef" class="chart three-check-chart" />
            <div class="metric-strip metric-strip--four">
              <span>班前会% <b>{{ summary.threeCheckRates.pre }}</b></span>
              <span>班前检查 <b>{{ summary.threeCheckRates.preInspection }}</b></span>
              <span>班中检查% <b>{{ summary.threeCheckRates.mid }}</b></span>
              <span>班后检查% <b>{{ summary.threeCheckRates.post }}</b></span>
            </div>
          </template>
          <div v-else class="screen-detail-table-wrap">
            <table class="screen-detail-table">
              <thead>
                <tr>
                  <th>公司</th>
                  <th>班前会</th>
                  <th>班前检查</th>
                  <th>班中检查</th>
                  <th>班后检查</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in threeCheckDetailRows" :key="item.fullName">
                  <td>{{ item.shortName }}</td>
                  <td>{{ item.values['班前会'] ?? 0 }}</td>
                  <td>{{ item.values['班前检查'] ?? 0 }}</td>
                  <td>{{ item.values['班中检查'] ?? 0 }}</td>
                  <td>{{ item.values['班后检查'] ?? 0 }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </aside>

      <section class="center-column">
        <section class="screen-panel panel--vision">
          <div class="panel-title">
            <span><IconifyIcon icon="lucide:bot" />平安大视界</span>
          </div>
            <div class="vision-layout">
              <div class="video-box">
                <video
                  v-if="activeVisionVideo?.videoAttachment?.url"
                  :key="activeVisionVideo.videoAttachment.id"
                  :src="activeVisionVideo.videoAttachment.url"
                  class="vision-video-player"
                  controls
                  @ended="handleVisionVideoEnded"
                ></video>
                <div v-else class="vision-video-placeholder">暂无宣传视频</div>
                <span
                  v-if="activeVisionVideo?.videoAttachment?.originalName"
                  class="vision-video-name"
                >
                  {{ activeVisionVideo.videoTitle || activeVisionVideo.videoAttachment.originalName }}
                </span>
              </div>
            </div>
          <div class="vision-copy vision-safety-copy">
            <p>安全，是一切工作的基石，是每个人最基本的保障。</p>
            <p>无论是工作还是生活，都要时刻牢记，生命是无价的，它承载着责任、家庭和未来。</p>
            <p>因此，安全生产、生活中的每一个细节都不容忽视。严格遵守各项安全制度，确保工作环境的安全无隐患</p>
          </div>
        </section>

        <section class="screen-panel panel--hazard">
          <div class="panel-title">
            <span><IconifyIcon icon="lucide:badge-alert" />隐患排查</span>
            <div class="panel-view-toggle" aria-label="隐患排查视图切换">
              <button
                :class="{ active: panelViews.hazard === 'chart' }"
                type="button"
                @click="setPanelView('hazard', 'chart')"
              >
                图表
              </button>
              <button
                :class="{ active: panelViews.hazard === 'detail' }"
                type="button"
                @click="setPanelView('hazard', 'detail')"
              >
                明细
              </button>
            </div>
          </div>
          <template v-if="panelViews.hazard === 'chart'">
            <div class="hazard-layout">
              <div class="hazard-pie">
                <EchartsUI ref="hazardPieRef" class="chart hazard-pie-chart" />
                <span>隐患总数 {{ summary.hazardTotal }}</span>
              </div>
              <h3 class="chart-caption">隐患数 Top N</h3>
              <EchartsUI ref="hazardBarRef" class="chart hazard-bar-chart hazard-bar-chart--raised" />
            </div>
            <div class="metric-strip metric-strip--four">
              <span>隐患总数 <b>{{ summary.hazardTotal }}</b></span>
              <span>已整改 <b class="green">{{ summary.hazardDone }}</b></span>
              <span>未整改 <b class="orange">{{ summary.hazardOpen }}</b></span>
              <span>整改率 <b>{{ summary.hazardFixedRate }}</b></span>
            </div>
          </template>
          <div v-else class="screen-detail-table-wrap">
            <table class="screen-detail-table">
              <thead>
                <tr>
                  <th>公司</th>
                  <th>隐患总数</th>
                  <th>已整改</th>
                  <th>未整改</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in hazardDetailRows" :key="item.fullName">
                  <td>{{ item.shortName }}</td>
                  <td>{{ item.total }}</td>
                  <td>{{ item.done }}</td>
                  <td>{{ item.open }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </section>

      <aside class="right-column">
        <section class="screen-panel panel--risk">
          <div class="panel-title">
            <span><IconifyIcon icon="lucide:shield-check" />风险分级管控</span>
            <div class="panel-view-toggle" aria-label="风险分级管控视图切换">
              <button
                :class="{ active: panelViews.risk === 'chart' }"
                type="button"
                @click="setPanelView('risk', 'chart')"
              >
                图表
              </button>
              <button
                :class="{ active: panelViews.risk === 'detail' }"
                type="button"
                @click="setPanelView('risk', 'detail')"
              >
                明细
              </button>
            </div>
          </div>
          <template v-if="panelViews.risk === 'chart'">
            <h3 class="chart-caption">风险点数 Top N</h3>
            <EchartsUI ref="riskChartRef" class="chart risk-chart" />
            <div class="metric-strip metric-strip--risk">
              <span class="red">重大风险 <b>{{ riskTotals.major }}</b></span>
              <span class="orange">较大风险 <b>{{ riskTotals.serious }}</b></span>
              <span class="yellow">一般风险 <b>{{ riskTotals.normal }}</b></span>
              <span class="blue">低风险 <b>{{ riskTotals.low }}</b></span>
            </div>
          </template>
          <div v-else class="screen-detail-table-wrap">
            <table class="screen-detail-table">
              <thead>
                <tr>
                  <th>公司</th>
                  <th>重大风险</th>
                  <th>较大风险</th>
                  <th>一般风险</th>
                  <th>低风险</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in riskControlDetailRows" :key="item.fullName">
                  <td>{{ item.shortName }}</td>
                  <td>{{ item.major }}</td>
                  <td>{{ item.serious }}</td>
                  <td>{{ item.normal }}</td>
                  <td>{{ item.low }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="screen-panel panel--ai">
          <div class="panel-title">
            <span><IconifyIcon icon="lucide:scan-eye" />AI监控</span>
            <em>今日告警 <b>2</b></em>
          </div>
          <div class="ai-body">
            <div class="ai-visual">
              <img alt="AI监控无人机与摄像头画面" src="/pingan-monitor/ai-monitor-drone.png" />
              <button type="button" aria-label="放大AI监控画面">点击放大</button>
            </div>
            <div class="ai-alert-card">
              <strong>AI智能报警</strong>
              <span>智能提醒，稳定可靠，为安全多添一份保障。</span>
            </div>
          </div>
        </section>
      </aside>
    </section>
  </main>
</template>

<style scoped>
:global(body:has(.monitor-screen:fullscreen)) {
  overflow: hidden;
}

.monitor-screen {
  position: relative;
  min-width: 1280px;
  min-height: 720px;
  height: calc(100vh - 170px);
  padding: 46px 10px 10px;
  overflow: hidden;
  color: #e8f8ff;
  background:
    radial-gradient(circle at 52% 42%, rgb(23 127 255 / 18%), transparent 34%),
    linear-gradient(135deg, #031329 0%, #062a59 48%, #021024 100%);
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
}

.monitor-screen:fullscreen {
  width: 100vw;
  height: 100vh;
}

.screen-bg,
.screen-bg::before,
.screen-bg::after {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.screen-bg {
  background:
    linear-gradient(90deg, rgb(28 243 255 / 5%) 1px, transparent 1px),
    linear-gradient(rgb(28 243 255 / 4%) 1px, transparent 1px);
  background-size: 74px 74px;
  opacity: 0.32;
}

.screen-bg::before {
  content: "";
  background: linear-gradient(180deg, rgb(60 172 255 / 10%), transparent 42%);
}

.screen-bg::after {
  content: "";
  background: radial-gradient(ellipse at center, transparent 34%, rgb(0 6 28 / 56%) 100%);
}

.screen-header {
  position: absolute;
  z-index: 2;
  top: 0;
  left: 0;
  display: grid;
  width: 100%;
  height: 46px;
  grid-template-columns: 32% 36% 32%;
  align-items: center;
  border-bottom: 1px solid rgb(77 163 221 / 48%);
  background: linear-gradient(90deg, rgb(3 15 35 / 96%), rgb(5 29 65 / 88%), rgb(3 15 35 / 96%));
  box-shadow: 0 8px 22px rgb(0 8 22 / 24%);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 0 12px;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}

.weather-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #f3fbff;
  font-size: 13px;
}

.screen-company-select {
  width: 118px;
}

.screen-date-picker {
  width: 238px;
}

.monitor-screen :deep(.screen-company-select .ant-select-selector),
.monitor-screen :deep(.screen-date-picker.ant-picker) {
  height: 30px;
  color: #dff4ff !important;
  background: rgb(9 34 72 / 70%) !important;
  border-color: rgb(124 181 220 / 42%) !important;
  border-radius: 4px;
  box-shadow: none !important;
}

.monitor-screen :deep(.screen-company-select .ant-select-selection-item),
.monitor-screen :deep(.screen-date-picker .ant-picker-input > input),
.monitor-screen :deep(.screen-date-picker .ant-picker-separator),
.monitor-screen :deep(.screen-date-picker .ant-picker-suffix),
.monitor-screen :deep(.screen-company-select .ant-select-arrow) {
  color: #dff4ff !important;
  font-size: 12px;
  font-weight: 700;
}

.date-range {
  color: #dcecff;
}

.data-source-status {
  max-width: 132px;
  overflow: hidden;
  color: #ffdf7e;
  font-size: 12px;
  text-overflow: ellipsis;
}

.header-title {
  position: relative;
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
}

.header-title h1 {
  position: relative;
  margin: 0;
  padding: 0 30px;
  color: #fff;
  font-size: 25px;
  font-weight: 700;
  letter-spacing: 2px;
  text-shadow: 0 2px 18px rgb(60 164 255 / 46%);
}

.header-title span {
  width: 82px;
  height: 26px;
  border-top: 3px solid #24efff;
  border-bottom: 3px solid rgb(36 239 255 / 65%);
  transform: skew(35deg);
}

.header-title span:last-child {
  transform: skew(-35deg);
}

.header-right {
  justify-content: flex-end;
  font-size: 12px;
}

.header-right button,
.panel-tabs button {
  color: #eaf8ff;
  background: rgb(17 57 129 / 72%);
  border: 1px solid rgb(186 226 255 / 95%);
}

.header-right button {
  height: 30px;
  min-width: 88px;
  border-radius: 4px;
}

.fullscreen-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.screen-grid {
  position: relative;
  z-index: 1;
  display: grid;
  height: 100%;
  grid-template-columns: 23% 54% 23%;
  gap: 8px;
}

.left-column,
.center-column,
.right-column {
  display: grid;
  gap: 8px;
  min-height: 0;
}

.left-column,
.right-column {
  grid-template-rows: 1fr 1fr;
}

.center-column {
  grid-template-rows: 60% 40%;
}

.screen-panel {
  position: relative;
  min-height: 0;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgb(4 30 69 / 94%), rgb(5 48 94 / 88%)),
    radial-gradient(circle at 90% 10%, rgb(61 148 212 / 16%), transparent 34%);
  border: 1px solid rgb(25 153 223 / 72%);
  border-radius: 5px;
  box-shadow:
    inset 0 0 18px rgb(74 149 212 / 18%),
    0 10px 24px rgb(0 8 24 / 22%);
}

.screen-panel::before,
.screen-panel::after {
  position: absolute;
  width: 18px;
  height: 18px;
  content: "";
  border-color: rgb(44 184 255 / 64%);
}

.screen-panel::before {
  top: -2px;
  left: -2px;
  border-top: 2px solid;
  border-left: 2px solid;
}

.screen-panel::after {
  right: -2px;
  bottom: -2px;
  border-right: 2px solid;
  border-bottom: 2px solid;
}

.panel-title {
  display: flex;
  height: 42px;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px 0 12px;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.panel-title > span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.panel-title svg {
  color: #19f6ff;
  font-size: 22px;
}

.panel-tabs {
  display: inline-flex;
  border: 1px solid rgb(196 232 255 / 80%);
}

.panel-tabs button {
  height: 24px;
  padding: 0 10px;
  border: 0;
  font-size: 12px;
}

.panel-tabs button.active {
  color: #09518f;
  background: #d8f7ff;
}

.panel-view-toggle {
  display: inline-flex;
  overflow: hidden;
  border: 1px solid rgb(198 231 255 / 88%);
  border-radius: 0;
}

.panel-view-toggle button {
  height: 26px;
  min-width: 42px;
  padding: 0 8px;
  color: #eaf8ff;
  background: rgb(20 82 182 / 42%);
  border: 0;
  border-left: 1px solid rgb(198 231 255 / 88%);
  font-size: 12px;
  font-weight: 700;
}

.panel-view-toggle button:first-child {
  border-left: 0;
}

.panel-view-toggle button.active {
  color: #08346d;
  background: #f0fbff;
}

.screen-detail-table-wrap {
  height: calc(100% - 42px);
  padding: 0 8px 10px;
  overflow: auto;
}

.screen-detail-table-wrap::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.screen-detail-table-wrap::-webkit-scrollbar-thumb {
  background: rgb(123 196 255 / 46%);
  border-radius: 999px;
}

.screen-detail-table {
  width: 100%;
  min-width: 100%;
  overflow: hidden;
  color: #fff;
  background: rgb(44 111 213 / 76%);
  border-collapse: collapse;
  border-radius: 2px;
  table-layout: fixed;
}

.screen-detail-table thead {
  position: sticky;
  z-index: 1;
  top: 0;
}

.screen-detail-table th,
.screen-detail-table td {
  height: 31px;
  padding: 0 8px;
  overflow: hidden;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.screen-detail-table th {
  color: #f5fbff;
  background: rgb(31 93 203 / 92%);
}

.screen-detail-table td {
  background: rgb(49 114 214 / 70%);
}

.screen-detail-table tbody tr:nth-child(even) td {
  background: rgb(56 124 221 / 72%);
}

.screen-detail-table td:first-child,
.screen-detail-table th:first-child {
  text-align: left;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  padding: 0 34px;
  text-align: center;
}

.summary-row--four {
  display: flex;
  gap: 4px;
  justify-content: space-around;
  padding: 0 12px;
}

.summary-row--four > div {
  display: flex;
  min-width: 54px;
  flex-direction: column;
  align-items: center;
}

.summary-row span {
  display: block;
  margin-bottom: 4px;
  color: #fff;
  font-size: 15px;
}

.summary-row strong {
  display: block;
  color: #22fff5;
  font-size: 22px;
}

.summary-row--four span {
  min-height: 40px;
  font-size: 13px;
}

.chart {
  width: 100%;
}

.chart-caption {
  margin: 4px 20px 0;
  color: #f2f8ff;
  font-size: 13px;
  font-weight: 700;
}

.dispatch-chart {
  height: calc(100% - 182px) !important;
  min-height: 170px;
}

.three-check-chart {
  height: calc(100% - 186px) !important;
  min-height: 170px;
}

.risk-chart {
  height: calc(100% - 118px) !important;
  min-height: 310px;
}

.metric-strip {
  position: absolute;
  right: 10px;
  bottom: 12px;
  left: 10px;
  display: flex;
  min-height: 34px;
  align-items: center;
  justify-content: center;
  gap: 18px;
  padding: 0 12px;
  margin: 0;
  background: rgb(3 21 49 / 44%);
  border: 1px solid rgb(103 166 218 / 26%);
  border-radius: 5px;
  color: #e8fbff;
  font-size: 13px;
  font-weight: 700;
}

.metric-strip span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.metric-strip b {
  color: #24d2f4;
  font-size: 20px;
}

.metric-strip .green,
.green,
.metric-strip b.green {
  color: #66c56c;
}

.metric-strip .orange,
.orange,
.metric-strip b.orange {
  color: #ff9f0a;
}

.metric-strip .red,
.red {
  color: #ff3b30;
}

.metric-strip .yellow,
.yellow {
  color: #ffd60a;
}

.metric-strip .blue,
.blue {
  color: #2f80ed;
}

.metric-strip--four {
  justify-content: space-around;
}

.panel--three-check .metric-strip--four {
  justify-content: space-between;
  gap: 8px;
  padding: 0 10px;
}

.panel--three-check .metric-strip span {
  gap: 3px;
  white-space: nowrap;
}

.panel--three-check .metric-strip b {
  font-size: 16px;
}

.metric-strip--risk {
  justify-content: space-around;
  gap: 10px;
}

.vision-layout {
  display: block;
  height: calc(100% - 142px);
  padding: 8px 10px 0;
}

.video-box {
  position: relative;
  height: 100%;
  min-width: 0;
  overflow: hidden;
  background: rgb(5 18 42 / 48%);
}

.video-box img,
.vision-video-player,
.ai-visual img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.vision-video-placeholder {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
  color: #d8f5ff;
  background:
    linear-gradient(135deg, rgb(5 20 45 / 78%), rgb(8 42 82 / 62%)),
    radial-gradient(circle at 50% 42%, rgb(36 210 244 / 14%), transparent 38%);
  font-size: 28px;
  font-weight: 700;
}

.vision-upload-button {
  position: absolute;
  right: 12px;
  bottom: 12px;
  display: inline-flex;
  height: 30px;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  color: #eaf8ff;
  background: rgb(6 38 101 / 82%);
  border: 1px solid rgb(186 226 255 / 90%);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 700;
}

.vision-video-input {
  display: none;
}

.vision-video-name {
  position: absolute;
  bottom: 48px;
  left: 12px;
  max-width: calc(100% - 24px);
  padding: 4px 8px;
  overflow: hidden;
  color: #dff4ff;
  background: rgb(4 24 70 / 72%);
  border-radius: 3px;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vision-copy {
  display: grid;
  box-sizing: border-box;
  height: 92px;
  align-content: center;
  gap: 7px;
  margin: 0 10px;
  padding: 8px 16px;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
  background: rgb(3 21 49 / 42%);
}

.vision-copy p {
  margin: 0;
  min-width: 0;
  color: #d8efff;
  overflow-wrap: anywhere;
}

.hazard-layout {
  display: grid;
  height: calc(100% - 66px);
  grid-template-columns: 32% 68%;
  grid-template-rows: 20px minmax(0, 1fr);
  gap: 4px 8px;
  padding: 0 8px 46px;
}

.hazard-pie {
  position: relative;
  grid-row: 1 / 3;
  min-width: 0;
}

.hazard-layout .chart-caption {
  margin: 0;
  align-self: end;
}

.hazard-pie-chart,
.hazard-bar-chart {
  height: 100% !important;
}

.hazard-bar-chart--raised {
  min-height: 150px;
}

.hazard-pie span {
  position: absolute;
  bottom: 16px;
  left: 18px;
  color: #d9efff;
  font-size: 12px;
}

.panel--ai {
  display: grid;
  grid-template-rows: 42px 1fr;
}

.panel-title em {
  padding: 4px 9px;
  color: #d9efff;
  background: rgb(102 143 193 / 20%);
  border: 1px solid rgb(137 191 235 / 28%);
  border-radius: 4px;
  font-size: 12px;
  font-style: normal;
}

.panel-title em b {
  display: inline-flex;
  min-width: 18px;
  min-height: 18px;
  align-items: center;
  justify-content: center;
  margin-left: 4px;
  color: #fff;
  background: #ff3b30;
  border-radius: 999px;
}

.ai-body {
  display: grid;
  min-height: 0;
  height: calc(100% - 42px);
  grid-template-rows: minmax(0, 1fr) 82px;
  gap: 10px;
  padding: 0 10px 12px;
}

.ai-visual {
  position: relative;
  min-height: 0;
  margin: 0;
  overflow: hidden;
  background: #07162c;
  border-radius: 7px;
}

.ai-visual button {
  position: absolute;
  top: 0;
  right: 0;
  display: inline-flex;
  height: 34px;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  color: #fff;
  background: rgb(13 50 96 / 88%);
  border: 0;
  border-radius: 0 7px 0 7px;
  font-size: 13px;
  font-weight: 700;
}

.ai-alert-card {
  display: grid;
  align-content: center;
  gap: 12px;
  min-width: 0;
  min-height: 0;
  padding: 0 14px;
  background:
    radial-gradient(circle at 26% 22%, rgb(36 210 244 / 15%), transparent 24%),
    linear-gradient(135deg, #174ea6, #153f91 72%, #12347d);
  border-radius: 7px;
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 8%);
}

.ai-alert-card strong {
  margin: 0;
  color: #fff;
  font-size: 14px;
  line-height: 1.2;
}

.ai-alert-card span {
  color: #e5f3ff;
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 1440px) {
  .monitor-screen {
    min-width: 1180px;
  }

  .header-title h1 {
    padding: 0 18px;
    font-size: 22px;
  }

  .header-title span {
    width: 58px;
  }

  .header-right,
  .header-left {
    gap: 6px;
    font-size: 12px;
  }
}
</style>
