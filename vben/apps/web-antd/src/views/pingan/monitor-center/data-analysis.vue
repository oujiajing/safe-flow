<script setup lang="ts">
import type { EchartsUIType } from '@vben/plugins/echarts';

import { computed, nextTick, onMounted, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

import { DatePicker, Segmented, Select } from 'ant-design-vue';

import {
  createEmptyMonitorCenterOverview,
  flattenMonitorCenterScopeOptions,
  getMonitorCenterOverviewApi,
  normalizeMonitorCenterOverview,
  toMonitorCenterNumber,
  type PinganMonitorCenterApi,
} from '#/api/pingan/monitor-center';

interface NamedValue {
  name: string;
  value: number;
}

interface ThreeCheckBar {
  mid: number;
  name: string;
  post: number;
  pre: number;
}

type AnalysisPanelKey = 'dispatch' | 'hazard' | 'learning' | 'threeCheck';
type PanelView = 'chart' | 'detail';

const selectedCompany = ref<PinganMonitorCenterApi.Id>();
const selectedTeam = ref<PinganMonitorCenterApi.Id>();
const dateRange = ref<[string, string]>(['2026-05-29', '2026-05-30']);
const overview = ref<PinganMonitorCenterApi.Overview>(
  createEmptyMonitorCenterOverview(),
);
const loading = ref(false);
const loadError = ref('');

const dispatchChartRef = ref<EchartsUIType>();
const hazardFunnelChartRef = ref<EchartsUIType>();
const closureChartRef = ref<EchartsUIType>();
const hazardDonutChartRef = ref<EchartsUIType>();
const threeChecksChartRef = ref<EchartsUIType>();
const learningTrendChartRef = ref<EchartsUIType>();

const { renderEcharts: renderDispatchChart } = useEcharts(dispatchChartRef);
const { renderEcharts: renderHazardFunnelChart } = useEcharts(
  hazardFunnelChartRef,
);
const { renderEcharts: renderClosureChart } = useEcharts(closureChartRef);
const { renderEcharts: renderHazardDonutChart } =
  useEcharts(hazardDonutChartRef);
const { renderEcharts: renderThreeChecksChart } =
  useEcharts(threeChecksChartRef);
const { renderEcharts: renderLearningTrendChart } = useEcharts(
  learningTrendChartRef,
);

const viewOptions: Array<{ label: string; value: PanelView }> = [
  { label: '图表视图', value: 'chart' },
  { label: '明细视图', value: 'detail' },
];
const analysisPanelViews = ref<Record<AnalysisPanelKey, PanelView>>({
  dispatch: 'chart',
  hazard: 'chart',
  learning: 'chart',
  threeCheck: 'chart',
});

const scopeOptions = computed(() => overview.value.scopeOptions ?? []);
const flatScopeOptions = computed(() =>
  flattenMonitorCenterScopeOptions(scopeOptions.value),
);
const companyOptions = computed(() => {
  return flatScopeOptions.value
    .filter((item) => item.orgType?.toUpperCase() === 'COMPANY')
    .map((item) => ({ label: item.name, value: item.id }));
});
const teamOptions = computed(() => {
  return flatScopeOptions.value
    .filter((item) => item.orgType?.toUpperCase() === 'TEAM')
    .map((item) => ({ label: item.name, value: item.id }));
});

const summary = computed(() => {
  const source =
    overview.value.summary ?? createEmptyMonitorCenterOverview().summary;
  return {
    dispatchFinished: toNumber(source.dispatchFinished),
    dispatchTotal: toNumber(source.dispatchTotal),
    hazardDone: toNumber(source.hazardDone),
    hazardFixedRate: source.hazardFixedRate,
    hazardOpen: toNumber(source.hazardOpen),
    hazardTotal: toNumber(source.hazardTotal),
    learningToday: toNumber(source.learningToday),
    learningTotal: toNumber(source.learningTotal),
    riskTotal: toNumber(source.riskTotal),
    teamCount: toNumber(source.teamCount),
    threeCheckRates: source.threeCheckRates,
  };
});

const dispatchRows = computed<NamedValue[]>(() => {
  const rows = (overview.value.dispatchBars ?? []).map((item) => ({
    name: item.name,
    value: toNumber(item.value),
  }));
  return rows.slice(0, 5);
});

const threeCheckRows = computed<ThreeCheckBar[]>(() => {
  const series = overview.value.threeCheckSeries ?? [];
  const primarySeries = series[0];
  const dimensions =
    primarySeries && primarySeries.companies.length > 0
      ? primarySeries.companies
      : (overview.value.companies ?? []);
  const rows = dimensions.map((dimension, index) => ({
    mid: seriesValue(series, '班中检查', index),
    name: dimension.name,
    post: seriesValue(series, '班后检查', index),
    pre: seriesValue(series, '班前检查', index),
  }));

  return rows.slice(0, 4);
});

const hazardDone = computed(() => summary.value.hazardDone);
const hazardOpen = computed(() => summary.value.hazardOpen);
const hazardTotal = computed(() => summary.value.hazardTotal);
const closureRate = computed(() => percent(hazardDone.value, hazardTotal.value));
const dispatchTotal = computed(() => summary.value.dispatchTotal);
const dispatchCompletionRate = computed(() =>
  percent(toNumber(summary.value.dispatchFinished), dispatchTotal.value),
);
const preMeetingTotal = computed(() =>
  seriesTotal(overview.value.threeCheckSeries, '班前会'),
);
const preInspectionTotal = computed(() =>
  seriesTotal(overview.value.threeCheckSeries, '班前检查'),
);
const midInspectionTotal = computed(() =>
  seriesTotal(overview.value.threeCheckSeries, '班中检查'),
);
const postInspectionTotal = computed(() =>
  seriesTotal(overview.value.threeCheckSeries, '班后检查'),
);
const majorRiskTotal = computed(() =>
  (overview.value.riskControlBars ?? []).reduce(
    (total, item) => total + toNumber(item.major),
    0,
  ),
);
const threeCheckStats = computed(() => {
  const rows = threeCheckRows.value;
  const pre = rows.reduce((total, item) => total + item.pre, 0);
  const mid = rows.reduce((total, item) => total + item.mid, 0);
  const post = rows.reduce((total, item) => total + item.post, 0);
  const planned = dispatchTotal.value * 3;
  return [
    { label: '班组总数', suffix: '个', value: String(summary.value.teamCount) },
    {
      label: '班前检查率',
      suffix: '',
      value: summary.value.threeCheckRates.preInspection,
    },
    { label: '班中检查率', suffix: '', value: summary.value.threeCheckRates.mid },
    { label: '班后检查率', suffix: '', value: summary.value.threeCheckRates.post },
    { label: '整体完成率', suffix: '', value: percent(pre + mid + post, planned) },
  ];
});

const metricCards = computed(() => [
  {
    accent: 'blue',
    change: '来自一班三查',
    icon: 'lucide:users-round',
    label: '班前未开会',
    trend: 'flat',
    unit: '个班组',
    value: Math.max(dispatchTotal.value - preMeetingTotal.value, 0),
  },
  {
    accent: 'teal',
    change: '来自一班三查',
    icon: 'lucide:clipboard-check',
    label: '班前未检查',
    trend: 'flat',
    unit: '个班组',
    value: Math.max(dispatchTotal.value - preInspectionTotal.value, 0),
  },
  {
    accent: 'amber',
    change: '来自一班三查',
    icon: 'lucide:search',
    label: '班中未检查',
    trend: 'flat',
    unit: '个班组',
    value: Math.max(dispatchTotal.value - midInspectionTotal.value, 0),
  },
  {
    accent: 'purple',
    change: '来自一班三查',
    icon: 'lucide:clipboard-list',
    label: '班后未检查',
    trend: 'flat',
    unit: '个班组',
    value: Math.max(dispatchTotal.value - postInspectionTotal.value, 0),
  },
  {
    accent: 'sky',
    change: '来自隐患排查',
    icon: 'lucide:bell',
    label: '上报隐患数',
    trend: 'flat',
    unit: '条',
    value: hazardTotal.value,
  },
  {
    accent: 'orange',
    change: '来自隐患排查',
    icon: 'lucide:wrench',
    label: '未解决隐患',
    trend: 'flat',
    unit: '条',
    value: hazardOpen.value,
  },
  {
    accent: 'red',
    change: '来自风险管控',
    icon: 'lucide:triangle-alert',
    label: '重大风险',
    trend: 'flat',
    unit: '条',
    value: majorRiskTotal.value,
  },
  {
    accent: 'green',
    change: '来自宣教培训',
    icon: 'lucide:graduation-cap',
    label: '安全学习',
    trend: 'flat',
    unit: '次',
    value: summary.value.learningToday,
  },
]);

const dispatchStats = computed(() => [
  { label: '合计派班数', suffix: '个班组', value: String(dispatchTotal.value) },
  {
    label: '已完成',
    modifier: 'up',
    suffix: '个班组',
    value: String(summary.value.dispatchFinished),
  },
  { label: '完成率', suffix: '', value: dispatchCompletionRate.value },
]);

const dispatchDetailRows = computed(() =>
  dispatchRows.value.map((item) => {
    const finished = seriesBarValue(overview.value.dispatchFinishedBars, item.name);
    return {
      finished,
      name: item.name,
      pending: Math.max(item.value - finished, 0),
      rate: percent(finished, item.value),
      total: item.value,
      yesterday: '实时',
    };
  }),
);

const hazardDetailRows = computed(() => {
  const source = overview.value.hazardBars ?? [];
  const rows = source.map((item) => ({
    done: toNumber(item.done),
    name: item.name,
    open: toNumber(item.open),
  }));
  return rows.map((item) => {
    const total = item.done + item.open;
    return {
      ...item,
      closureRate: percent(item.done, total),
      majorRisk: riskMajorValue(item.name),
      total,
    };
  });
});

const threeCheckDetailRows = computed(() =>
  threeCheckRows.value.map((item) => {
    const total = item.pre + item.mid + item.post;
    const planned = seriesBarValue(overview.value.dispatchBars, item.name) * 3;
    return {
      ...item,
      completeRate: percent(total, planned),
      total,
      unfinished: Math.max(planned - total, 0),
    };
  }),
);

const learningTrendRows = computed(() =>
  (overview.value.learningTrend ?? []).map((item, index, rows) => {
    const times = toNumber(item.value);
    const previous = index > 0 ? toNumber(rows[index - 1]?.value) : 0;
    const delta = times - previous;
    return {
      date: formatTrendDate(item.name),
      times,
      trend:
        index === 0
          ? '首日'
          : delta === 0
            ? '较昨日 持平'
            : `较昨日 ${delta > 0 ? '↑' : '↓'} ${Math.abs(delta)}`,
    };
  }),
);

const learningDelta = computed(() => {
  const rows = overview.value.learningTrend ?? [];
  const today = toNumber(rows.at(-1)?.value ?? summary.value.learningToday);
  const yesterday = toNumber(rows.at(-2)?.value);
  return today - yesterday;
});

const learningStats = computed(() => [
  {
    label: '今日学习次数',
    suffix: '次',
    value: String(summary.value.learningToday),
  },
  {
    label: '较昨日',
    modifier: learningDelta.value < 0 ? 'down' : 'up',
    suffix: String(Math.abs(learningDelta.value)),
    value: learningDelta.value < 0 ? '↓' : learningDelta.value > 0 ? '↑' : '持平',
  },
  {
    label: '累计学习次数',
    suffix: '次',
    value: String(summary.value.learningTotal),
  },
]);

const rankRows = computed(() =>
  (overview.value.learningBars ?? [])
    .map((item) => ({ name: item.name, value: toNumber(item.value) }))
    .sort((left, right) => right.value - left.value)
    .slice(0, 5)
    .map((item, index) => ({
      ...item,
      className:
        index === 0
          ? 'rank-medal rank-medal--gold'
          : index === 1
            ? 'rank-medal rank-medal--silver'
            : index === 2
              ? 'rank-medal rank-medal--bronze'
              : '',
      rank: index + 1,
    })),
);

watch([selectedCompany, selectedTeam, dateRange], () => {
  void loadMonitorOverview();
});

onMounted(() => {
  void loadMonitorOverview();
});

async function loadMonitorOverview() {
  loading.value = true;
  loadError.value = '';
  try {
    const [dateStart, dateEnd] = dateRange.value;
    overview.value = normalizeMonitorCenterOverview(
      await getMonitorCenterOverviewApi({
        dateEnd,
        dateStart,
        orgId: selectedTeam.value ?? selectedCompany.value,
      }),
    );
  } catch {
    overview.value = createEmptyMonitorCenterOverview();
    loadError.value = '数据加载失败，暂无业务汇总数据';
  } finally {
    loading.value = false;
    await nextTick();
    renderCharts();
  }
}

function toNumber(value: unknown) {
  return toMonitorCenterNumber(value);
}

function percent(numerator: number, denominator: number) {
  if (denominator <= 0) {
    return '0.00%';
  }
  return `${((numerator / denominator) * 100).toFixed(2)}%`;
}

function seriesValue(
  series: PinganMonitorCenterApi.SeriesItem[],
  name: string,
  index: number,
) {
  return toNumber(series.find((item) => item.name === name)?.data?.[index]);
}

function seriesTotal(
  series: PinganMonitorCenterApi.SeriesItem[] = [],
  name: string,
) {
  return (series.find((item) => item.name === name)?.data ?? []).reduce(
    (total: number, value) => total + toNumber(value),
    0,
  );
}

function seriesBarValue(
  rows: PinganMonitorCenterApi.BarItem[] = [],
  name: string,
) {
  return toNumber(rows.find((item) => item.name === name)?.value);
}

function riskMajorValue(name: string) {
  return toNumber(
    (overview.value.riskControlBars ?? []).find((item) => item.name === name)
      ?.major,
  );
}

function formatTrendDate(date: string) {
  return date.length >= 10 ? date.slice(5) : date;
}

async function setAnalysisPanelView(panel: AnalysisPanelKey, view: PanelView) {
  analysisPanelViews.value[panel] = view;
  if (view === 'chart') {
    await nextTick();
    renderCharts();
  }
}

function renderCharts() {
  if (analysisPanelViews.value.dispatch === 'chart') {
    renderDispatchChart({
      color: ['#2f74f8'],
      grid: { bottom: 26, containLabel: true, left: 12, right: 26, top: 24 },
      series: [
        {
          barWidth: 10,
          data: dispatchRows.value.map((item) => item.value),
          label: { color: '#25385f', position: 'right', show: true },
          type: 'bar',
        },
      ],
      tooltip: { trigger: 'axis' },
      xAxis: {
        axisLine: { lineStyle: { color: '#d7dfeb' } },
        max: Math.max(6, ...dispatchRows.value.map((item) => item.value)),
        splitLine: { lineStyle: { color: '#edf1f7' }, show: true },
        type: 'value',
      },
      yAxis: {
        axisTick: { show: false },
        data: dispatchRows.value.map((item) => item.name),
        inverse: true,
        type: 'category',
      },
    });
  }

  if (analysisPanelViews.value.hazard === 'chart') {
    renderHazardFunnelChart({
      color: ['#3478f6', '#15c7b7', '#ffad27', '#6dc043'],
      series: [
        {
          data: [
            { name: `隐患总数 ${hazardTotal.value}`, value: 100 },
            { name: `待整改 ${hazardOpen.value}`, value: 74 },
            { name: '整改中 0', value: 50 },
            { name: `已整改 ${hazardDone.value}`, value: 32 },
          ],
          gap: 2,
          height: '92%',
          label: {
            color: '#fff',
            fontSize: 14,
            fontWeight: 700,
            formatter: '{b}',
            position: 'inside',
          },
          left: '4%',
          maxSize: '96%',
          minSize: '42%',
          sort: 'descending',
          top: '4%',
          type: 'funnel',
          width: '92%',
        },
      ],
      tooltip: { show: false },
    });

    renderClosureChart({
      color: ['#22bfae', '#e9edf3'],
      series: [
        {
          data: [
            { name: '闭环率', value: hazardDone.value },
            { name: '未闭环', value: hazardOpen.value },
          ],
          label: { show: false },
          radius: ['72%', '86%'],
          type: 'pie',
        },
      ],
      tooltip: { show: false },
    });

    renderHazardDonutChart({
      color: ['#31c48d', '#ffad27', '#f7cf43', '#ff4d4f'],
      legend: { show: false },
      series: [
        {
          data: [
            { name: '已整改隐患', value: hazardDone.value },
            { name: '待整改隐患', value: hazardOpen.value },
            { name: '整改中隐患', value: 0 },
            { name: '未整改隐患', value: 0 },
          ],
          label: { show: false },
          radius: ['46%', '72%'],
          type: 'pie',
        },
      ],
      tooltip: { trigger: 'item' },
    });
  }

  if (analysisPanelViews.value.threeCheck === 'chart') {
    renderThreeChecksChart({
      color: ['#3478f6', '#2fc8bd', '#ffb23a', '#a9adb7'],
      grid: { bottom: 28, containLabel: true, left: 8, right: 12, top: 34 },
      legend: {
        data: ['班前检查', '班中检查', '班后检查', '未完成项'],
        itemHeight: 10,
        itemWidth: 10,
        top: 0,
      },
      series: [
        {
          barWidth: 42,
          data: threeCheckRows.value.map((item) => item.pre),
          label: { color: '#fff', position: 'inside', show: true },
          name: '班前检查',
          stack: 'checks',
          type: 'bar',
        },
        {
          data: threeCheckRows.value.map((item) => item.mid),
          label: { color: '#fff', position: 'inside', show: true },
          name: '班中检查',
          stack: 'checks',
          type: 'bar',
        },
        {
          data: threeCheckRows.value.map((item) => item.post),
          label: { color: '#fff', position: 'inside', show: true },
          name: '班后检查',
          stack: 'checks',
          type: 'bar',
        },
        {
          data: threeCheckRows.value.map((item) => {
            const planned =
              seriesBarValue(overview.value.dispatchBars, item.name) * 3;
            return Math.max(planned - item.pre - item.mid - item.post, 0);
          }),
          name: '未完成项',
          stack: 'checks',
          type: 'bar',
        },
      ],
      tooltip: { trigger: 'axis' },
      xAxis: {
        axisTick: { show: false },
        data: threeCheckRows.value.map((item) => item.name),
        type: 'category',
      },
      yAxis: {
        max: Math.max(
          4,
          ...threeCheckRows.value.map((item) => {
            const planned =
              seriesBarValue(overview.value.dispatchBars, item.name) * 3;
            return Math.max(planned, item.pre + item.mid + item.post);
          }),
        ),
        splitLine: { lineStyle: { color: '#edf1f7' } },
        type: 'value',
      },
    });
  }

  if (analysisPanelViews.value.learning === 'chart') {
    renderLearningTrendChart({
      color: ['#3478f6'],
      grid: { bottom: 26, containLabel: true, left: 8, right: 20, top: 28 },
      series: [
        {
          areaStyle: { color: 'rgba(52, 120, 246, 0.12)' },
          data: learningTrendRows.value.map((item) => item.times),
          label: {
            color: '#172033',
            formatter: '{c}',
            position: 'top',
            show: true,
          },
          lineStyle: { width: 2 },
          smooth: true,
          symbolSize: 8,
          type: 'line',
        },
      ],
      tooltip: { trigger: 'axis' },
      xAxis: {
        axisTick: { show: false },
        data: learningTrendRows.value.map((item) => item.date),
        type: 'category',
      },
      yAxis: {
        max: Math.max(4, ...learningTrendRows.value.map((item) => item.times)),
        splitLine: { lineStyle: { color: '#edf1f7' } },
        type: 'value',
      },
    });
  }
}
</script>

<template>
  <div class="monitor-center-page">
    <header class="monitor-toolbar">
      <Select
        v-model:value="selectedCompany"
        allow-clear
        :options="companyOptions"
        placeholder="选择公司"
      />
      <Select
        v-model:value="selectedTeam"
        allow-clear
        :options="teamOptions"
        placeholder="选择班组"
      />
      <DatePicker.RangePicker
        v-model:value="dateRange"
        value-format="YYYY-MM-DD"
      />
    </header>

    <main class="analysis-canvas">
      <div v-if="loadError || loading" class="data-status">
        {{ loading ? '正在加载业务汇总数据' : loadError }}
      </div>

      <section class="metric-strip">
        <article
          v-for="item in metricCards"
          :key="item.label"
          :class="['metric-card', `metric-card--${item.accent}`]"
        >
          <span class="metric-icon">
            <IconifyIcon :icon="item.icon" />
          </span>
          <div class="metric-copy">
            <h3>{{ item.label }}</h3>
            <div class="metric-value">
              <strong>{{ item.value }}</strong>
              <span>{{ item.unit }}</span>
            </div>
            <p :class="`trend trend--${item.trend}`">{{ item.change }}</p>
          </div>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="panel panel--dispatch">
          <header class="panel-header">
            <div>
              <h2>
                <IconifyIcon icon="lucide:briefcase-business" />
                作业派班
              </h2>
              <span>按公司统计</span>
            </div>
            <Segmented
              :value="analysisPanelViews.dispatch"
              :options="viewOptions"
              size="small"
              @change="
                (value) => setAnalysisPanelView('dispatch', value as PanelView)
              "
            />
          </header>
          <div v-if="analysisPanelViews.dispatch === 'chart'" class="dispatch-layout">
            <div class="chart-title">派班数（个）</div>
            <EchartsUI ref="dispatchChartRef" class="dispatch-chart" />
            <aside class="dispatch-summary">
              <div
                v-for="item in dispatchStats"
                :key="item.label"
                class="summary-row"
              >
                <span>{{ item.label }}</span>
                <strong :class="item.modifier ? `summary-${item.modifier}` : ''">
                  {{ item.value }}
                  <em>{{ item.suffix }}</em>
                </strong>
              </div>
              <div class="summary-progress"><span /></div>
            </aside>
          </div>
          <div v-else class="detail-table-wrap detail-table-wrap--dispatch">
            <table class="detail-table">
              <thead>
                <tr>
                  <th>公司/班组</th>
                  <th>派班数</th>
                  <th>已完成</th>
                  <th>待完成</th>
                  <th>完成率</th>
                  <th>来源</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in dispatchDetailRows" :key="item.name">
                  <td>{{ item.name }}</td>
                  <td>{{ item.total }}</td>
                  <td>{{ item.finished }}</td>
                  <td>{{ item.pending }}</td>
                  <td><b>{{ item.rate }}</b></td>
                  <td>{{ item.yesterday }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="panel panel--hazard">
          <header class="panel-header">
            <div>
              <h2>
                <IconifyIcon icon="lucide:route" />
                隐患整改
              </h2>
              <span>隐患整改闭环情况</span>
            </div>
            <Segmented
              :value="analysisPanelViews.hazard"
              :options="viewOptions"
              size="small"
              @change="
                (value) => setAnalysisPanelView('hazard', value as PanelView)
              "
            />
          </header>
          <div v-if="analysisPanelViews.hazard === 'chart'" class="hazard-layout">
            <div class="hazard-funnel-chart">
              <div class="funnel-layer funnel-layer--total">
                <span>隐患总数</span>
                <strong>{{ hazardTotal }}</strong>
              </div>
              <div class="funnel-layer funnel-layer--pending">
                <span>待整改</span>
                <strong>{{ hazardOpen }}</strong>
              </div>
              <div class="funnel-layer funnel-layer--working">
                <span>整改中</span>
                <strong>0</strong>
              </div>
              <div class="funnel-layer funnel-layer--done">
                <span>已整改</span>
                <strong>{{ hazardDone }}</strong>
              </div>
              <EchartsUI
                ref="hazardFunnelChartRef"
                aria-hidden="true"
                class="hazard-funnel-echarts"
              />
            </div>
            <div class="closure-card">
              <span class="closure-label">闭环率</span>
              <EchartsUI ref="closureChartRef" class="closure-chart" />
              <div class="closure-center">
                <strong>{{ closureRate }}</strong>
              </div>
              <p>来自隐患排查数据</p>
            </div>
            <div class="hazard-state-card">
              <h3>隐患状态分布</h3>
              <div class="hazard-donut-wrap">
                <EchartsUI
                  ref="hazardDonutChartRef"
                  class="hazard-donut-chart"
                />
                <ul>
                  <li>
                    <i class="dot dot--green"></i>
                    已整改隐患 <b>{{ hazardDone }} ({{ closureRate }})</b>
                  </li>
                  <li>
                    <i class="dot dot--amber"></i>
                    待整改隐患 <b>{{ hazardOpen }} ({{ percent(hazardOpen, hazardTotal) }})</b>
                  </li>
                  <li><i class="dot dot--yellow"></i>整改中隐患 <b>0 (0.00%)</b></li>
                  <li><i class="dot dot--red"></i>未整改隐患 <b>0 (0.00%)</b></li>
                </ul>
              </div>
            </div>
            <footer class="hazard-footer">
              隐患总数 <b>{{ hazardTotal }}</b> 条，未解决隐患
              <b>{{ hazardOpen }}</b> 条，重大风险 <b class="danger">0</b> 条
            </footer>
          </div>
          <div v-else class="detail-table-wrap detail-table-wrap--hazard">
            <table class="detail-table">
              <thead>
                <tr>
                  <th>公司/班组</th>
                  <th>隐患总数</th>
                  <th>已整改</th>
                  <th>待整改</th>
                  <th>闭环率</th>
                  <th>重大风险</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in hazardDetailRows" :key="item.name">
                  <td>{{ item.name }}</td>
                  <td>{{ item.total }}</td>
                  <td>{{ item.done }}</td>
                  <td>{{ item.open }}</td>
                  <td><b>{{ item.closureRate }}</b></td>
                  <td>{{ item.majorRisk }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="panel panel--checks">
          <header class="panel-header">
            <div>
              <h2>
                <IconifyIcon icon="lucide:users-round" />
                一班三查
              </h2>
              <span>各班组完成情况</span>
            </div>
            <Segmented
              :value="analysisPanelViews.threeCheck"
              :options="viewOptions"
              size="small"
              @change="
                (value) => setAnalysisPanelView('threeCheck', value as PanelView)
              "
            />
          </header>
          <EchartsUI
            v-if="analysisPanelViews.threeCheck === 'chart'"
            ref="threeChecksChartRef"
            class="checks-chart"
          />
          <footer v-if="analysisPanelViews.threeCheck === 'chart'" class="checks-stats">
            <div v-for="item in threeCheckStats" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}<em>{{ item.suffix }}</em></strong>
            </div>
          </footer>
          <div v-else class="detail-table-wrap detail-table-wrap--compact">
            <table class="detail-table detail-table--compact">
              <thead>
                <tr>
                  <th>班组名称</th>
                  <th>班前</th>
                  <th>班中</th>
                  <th>班后</th>
                  <th>未完成</th>
                  <th>完成率</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in threeCheckDetailRows" :key="item.name">
                  <td>{{ item.name }}</td>
                  <td>{{ item.pre }}</td>
                  <td>{{ item.mid }}</td>
                  <td>{{ item.post }}</td>
                  <td>{{ item.unfinished }}</td>
                  <td><b>{{ item.completeRate }}</b></td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="panel panel--learning">
          <header class="panel-header">
            <div>
              <h2>
                <IconifyIcon icon="lucide:chart-no-axes-combined" />
                学习次数
              </h2>
              <span>学习趋势与排名</span>
            </div>
            <Segmented
              :value="analysisPanelViews.learning"
              :options="viewOptions"
              size="small"
              @change="
                (value) => setAnalysisPanelView('learning', value as PanelView)
              "
            />
          </header>
          <div v-if="analysisPanelViews.learning === 'chart'" class="learning-layout">
            <div class="learning-trend">
              <h3>学习次数趋势（近7日）</h3>
              <EchartsUI
                ref="learningTrendChartRef"
                class="learning-trend-chart"
              />
            </div>
            <aside class="learning-rank">
              <h3>学习次数TOP5（近7日）</h3>
              <table>
                <thead>
                  <tr>
                    <th>序号</th>
                    <th>公司/班组</th>
                    <th>学习次数（次）</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in rankRows" :key="item.name">
                    <td>
                      <span :class="item.className || 'rank-number'">
                        {{ item.rank }}
                      </span>
                    </td>
                    <td>{{ item.name }}</td>
                    <td>{{ item.value }}</td>
                  </tr>
                </tbody>
              </table>
            </aside>
          </div>
          <div v-else class="learning-detail">
            <table class="detail-table detail-table--compact">
              <thead>
                <tr>
                  <th>日期</th>
                  <th>学习次数</th>
                  <th>变化</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in learningTrendRows" :key="item.date">
                  <td>{{ item.date }}</td>
                  <td><b>{{ item.times }}</b></td>
                  <td>{{ item.trend }}</td>
                </tr>
              </tbody>
            </table>
            <table class="detail-table detail-table--compact">
              <thead>
                <tr>
                  <th>序号</th>
                  <th>公司/班组</th>
                  <th>学习次数</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in rankRows" :key="item.name">
                  <td>{{ item.rank }}</td>
                  <td>{{ item.name }}</td>
                  <td><b>{{ item.value }}</b></td>
                </tr>
              </tbody>
            </table>
          </div>
          <footer v-if="analysisPanelViews.learning === 'chart'" class="learning-stats">
            <div
              v-for="item in learningStats"
              :key="item.label"
              :class="item.modifier ? `learning-stat--${item.modifier}` : ''"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}<em>{{ item.suffix }}</em></strong>
            </div>
          </footer>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
.monitor-center-page {
  height: calc(100vh - 136px);
  min-height: 100%;
  overflow: hidden;
  color: #172033;
  background: #f3f6fb;
}

.monitor-toolbar {
  display: grid;
  grid-template-columns: 180px 170px 222px;
  gap: 14px;
  align-items: center;
  justify-content: start;
  min-height: 58px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e8eef7;
  box-shadow: 0 1px 4px rgb(15 23 42 / 3%);
}

.view-tabs,
.panel-header :deep(.ant-segmented) {
  background: #fff;
}

.view-tabs :deep(.ant-segmented-item-selected),
.panel-header :deep(.ant-segmented-item-selected) {
  color: #fff;
  background: #1476f2;
}

.view-tabs :deep(.ant-segmented-item),
.panel-header :deep(.ant-segmented-item) {
  color: #26364f;
}

.view-tabs :deep(.ant-segmented-item-selected),
.panel-header :deep(.ant-segmented-item-selected) {
  color: #fff;
}

.monitor-toolbar :deep(.ant-select-selector),
.monitor-toolbar :deep(.ant-picker) {
  height: 34px;
  color: #1f2d44;
  background: #fff;
  border-color: #dbe3ef;
  border-radius: 4px;
}

.monitor-toolbar :deep(.ant-picker-input > input),
.monitor-toolbar :deep(.ant-select-selection-item),
.monitor-toolbar :deep(.ant-select-selection-placeholder) {
  color: #1f2d44;
}

.analysis-canvas {
  position: relative;
  height: calc(100% - 58px);
  padding: 14px 16px 16px;
  overflow: hidden;
}

.data-status {
  position: absolute;
  top: 16px;
  right: 18px;
  z-index: 2;
  padding: 4px 9px;
  color: #8a5a00;
  background: #fff7df;
  border: 1px solid #ffe1a8;
  border-radius: 4px;
  font-size: 12px;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(8, minmax(128px, 1fr));
  gap: 10px;
}

.metric-card {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 112px;
  padding: 16px 14px;
  background:
    radial-gradient(circle at 84% 16%, rgb(255 255 255 / 70%), transparent 30%),
    #fff;
  border: 1px solid #edf2f8;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgb(37 74 130 / 6%);
}

.metric-icon {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  font-size: 27px;
  border-radius: 50%;
}

.metric-copy {
  min-width: 0;
}

.metric-copy h3 {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 800;
}

.metric-value {
  display: flex;
  gap: 6px;
  align-items: baseline;
}

.metric-value strong {
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
}

.metric-value span {
  color: #607089;
  font-size: 12px;
}

.trend {
  margin: 8px 0 0;
  color: #4d5b73;
  font-size: 12px;
}

.trend--up {
  color: #f04438;
}

.trend--down {
  color: #149b6f;
}

.metric-card--blue .metric-icon {
  color: #1476f2;
  background: #dcecff;
}

.metric-card--blue .metric-value strong {
  color: #1476f2;
}

.metric-card--teal .metric-icon {
  color: #13b8aa;
  background: #d9f5f2;
}

.metric-card--teal .metric-value strong {
  color: #11a99c;
}

.metric-card--amber .metric-icon {
  color: #e79a11;
  background: #fff1cf;
}

.metric-card--amber .metric-value strong,
.metric-card--orange .metric-value strong {
  color: #ff6a1a;
}

.metric-card--purple .metric-icon {
  color: #8c2be8;
  background: #efe1ff;
}

.metric-card--purple .metric-value strong {
  color: #3774f4;
}

.metric-card--sky .metric-icon {
  color: #1579ee;
  background: #dbeafe;
}

.metric-card--sky .metric-value strong {
  color: #1d75f0;
}

.metric-card--orange .metric-icon {
  color: #ff6a1a;
  background: #ffe4d2;
}

.metric-card--red .metric-icon {
  color: #f04438;
  background: #ffe0e2;
}

.metric-card--red .metric-value strong {
  color: #f20f17;
}

.metric-card--green .metric-icon {
  color: #19b970;
  background: #d9f5e7;
}

.metric-card--green .metric-value strong {
  color: #08aa63;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1fr);
  grid-template-rows: 492px minmax(260px, 1fr);
  gap: 10px;
  height: calc(100vh - 350px);
  margin-top: 10px;
}

.panel {
  min-width: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #edf2f8;
  border-radius: 8px;
  box-shadow: 0 12px 34px rgb(37 74 130 / 5%);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 50px;
  padding: 10px 22px 4px;
}

.panel-header h2 {
  display: flex;
  gap: 8px;
  align-items: center;
  margin: 0;
  color: #071a3b;
  font-size: 18px;
  font-weight: 900;
}

.panel-header h2 svg {
  color: #1275f7;
}

.panel-header span {
  margin-left: 8px;
  color: #7a8798;
  font-size: 12px;
}

.detail-table-wrap {
  height: calc(100% - 50px);
  padding: 10px 18px 14px;
  overflow: hidden;
}

.detail-table-wrap--dispatch {
  padding-right: 22px;
  padding-left: 22px;
}

.detail-table-wrap--compact {
  padding-top: 8px;
}

.detail-table {
  width: 100%;
  color: #25324a;
  border-collapse: collapse;
  font-size: 13px;
  table-layout: fixed;
}

.detail-table th {
  height: 36px;
  color: #24324a;
  font-weight: 900;
  text-align: left;
  background: #f3f6fb;
}

.detail-table th,
.detail-table td {
  padding: 8px 10px;
  overflow: hidden;
  border-bottom: 1px solid #edf1f7;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-table td:not(:first-child),
.detail-table th:not(:first-child) {
  text-align: center;
}

.detail-table b {
  color: #1476f2;
  font-weight: 900;
}

.detail-table--compact {
  font-size: 12px;
}

.detail-table--compact th {
  height: 30px;
}

.detail-table--compact th,
.detail-table--compact td {
  padding: 5px 8px;
}

.learning-detail {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: 14px;
  height: calc(100% - 50px);
  padding: 8px 18px 12px;
  overflow: hidden;
}

.panel--dispatch {
  min-height: 310px;
}

.panel--dispatch {
  display: flex;
  flex-direction: column;
}

.dispatch-layout {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 138px;
  gap: 8px;
  align-items: stretch;
  flex: 1;
  min-height: 400px;
  padding: 8px 22px 18px;
}

.chart-title {
  position: absolute;
  top: 8px;
  left: 330px;
  color: #465774;
  font-size: 12px;
}

.dispatch-chart {
  width: 100%;
  height: 100% !important;
  min-height: 400px;
}

.dispatch-summary {
  display: grid;
  min-height: 400px;
  align-content: center;
  align-self: stretch;
  padding: 8px 0 0 12px;
  border-left: 1px solid #edf1f7;
}

.summary-row {
  padding: 10px 0;
  border-bottom: 1px solid #edf1f7;
}

.summary-row span,
.checks-stats span,
.learning-stats span {
  display: block;
  color: #2e3c55;
  font-size: 13px;
}

.summary-row strong {
  display: block;
  margin-top: 6px;
  color: #1476f2;
  font-size: 28px;
  line-height: 1;
}

.summary-row em,
.checks-stats em,
.learning-stats em {
  margin-left: 4px;
  color: inherit;
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
}

.summary-up {
  color: #f04438 !important;
  font-size: 28px !important;
}

.summary-progress {
  height: 8px;
  margin-top: 12px;
  overflow: hidden;
  background: #edf1f7;
  border-radius: 999px;
}

.summary-progress span {
  display: block;
  width: 75%;
  height: 100%;
  background: #1a75f2;
  border-radius: inherit;
}

.hazard-layout {
  display: grid;
  grid-template-columns: minmax(124px, 0.72fr) 150px minmax(256px, 1.28fr);
  grid-template-rows: minmax(0, 1fr) 30px;
  column-gap: 14px;
  row-gap: 8px;
  align-items: stretch;
  height: 100%;
  min-height: 0;
  padding: 12px 22px 8px;
}

.panel--hazard {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-height: 310px;
}

.hazard-funnel-chart {
  position: relative;
  display: grid;
  gap: 2px;
  height: 100%;
  min-height: 218px;
  align-content: center;
  justify-items: center;
  overflow: hidden;
}

.funnel-layer {
  display: grid;
  height: 51px;
  place-items: center;
  color: #fff;
  clip-path: polygon(0 0, 100% 0, 88% 100%, 12% 100%);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.2;
}

.funnel-layer span {
  opacity: 0.95;
}

.funnel-layer strong {
  font-size: 18px;
}

.funnel-layer--total {
  width: 92%;
  background: linear-gradient(180deg, #4a8bff, #2f6fed);
}

.funnel-layer--pending {
  width: 76%;
  background: linear-gradient(180deg, #18c8bd, #0fb2a9);
}

.funnel-layer--working {
  width: 60%;
  background: linear-gradient(180deg, #ffbd3d, #f4a21c);
}

.funnel-layer--done {
  width: 44%;
  background: linear-gradient(180deg, #75ce41, #5db934);
}

.hazard-funnel-echarts {
  position: absolute;
  width: 1px;
  height: 1px;
  pointer-events: none;
  opacity: 0;
}

.closure-card {
  position: relative;
  display: grid;
  height: 100%;
  min-height: 198px;
  place-items: center;
  background: #f8fafc;
  border-radius: 4px;
}

.closure-chart {
  width: 128px;
  height: 128px;
}

.closure-center {
  position: absolute;
  top: 50%;
  left: 50%;
  display: grid;
  place-items: center;
  transform: translate(-50%, -50%);
}

.closure-label {
  position: absolute;
  top: calc(50% - 54px);
  left: 50%;
  color: #43516b;
  font-size: 12px;
  transform: translateX(-50%);
}

.closure-center strong {
  color: #18b881;
  font-size: 20px;
}

.closure-card p {
  margin: 0 0 4px;
  color: #6a7588;
  font-size: 16px;
}

.closure-card b,
.danger {
  color: #f04438;
}

.hazard-state-card {
  display: grid;
  height: 100%;
  min-height: 198px;
  align-content: center;
  padding: 16px 12px 10px;
  background: #f8fafc;
  border-radius: 4px;
}

.hazard-state-card h3,
.learning-layout h3 {
  margin: 0;
  color: #0e1e3a;
  font-size: 15px;
  font-weight: 900;
}

.hazard-donut-wrap {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  margin-top: 6px;
}

.hazard-donut-chart {
  height: 108px;
}

.hazard-state-card ul {
  display: grid;
  gap: 12px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.hazard-state-card li {
  display: grid;
  grid-template-columns: 8px minmax(58px, 1fr) auto;
  gap: 6px;
  align-items: center;
  color: #29364c;
  font-size: 12px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

.dot--green {
  background: #31c48d;
}

.dot--amber {
  background: #ffad27;
}

.dot--yellow {
  background: #f7cf43;
}

.dot--red {
  background: #ff4d4f;
}

.hazard-footer {
  grid-column: 2 / 4;
  height: 30px;
  margin: 0;
  color: #2c3b55;
  font-size: 14px;
  line-height: 30px;
  text-align: center;
  background: #f8fafc;
  border-radius: 4px;
}

.hazard-footer b {
  margin: 0 4px;
  color: #1476f2;
}

.panel--checks,
.panel--learning {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-height: 285px;
}

.checks-chart {
  width: 100%;
  height: 100% !important;
  min-height: 0;
  padding: 0 18px;
}

.checks-stats,
.learning-stats {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0;
  margin: 2px 20px 10px;
  padding: 10px 0;
  align-self: end;
  background: #f8fafc;
  border-radius: 4px;
}

.checks-stats div,
.learning-stats div {
  text-align: center;
  border-right: 1px solid #dce3ee;
}

.checks-stats div:last-child,
.learning-stats div:last-child {
  border-right: 0;
}

.checks-stats strong,
.learning-stats strong {
  display: block;
  margin-top: 6px;
  color: #1476f2;
  font-size: 22px;
  line-height: 1;
}

.learning-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(260px, 0.95fr);
  gap: 22px;
  align-items: stretch;
  height: 100%;
  min-height: 0;
  padding: 10px 22px 0;
}

.learning-trend {
  display: flex;
  min-height: 0;
  flex-direction: column;
}

.learning-rank {
  display: flex;
  min-height: 0;
  flex-direction: column;
  padding-left: 22px;
  border-left: 1px solid #dce3ee;
}

.learning-trend-chart {
  flex: 1;
  height: 100% !important;
  min-height: 0;
}

.learning-rank table {
  width: 100%;
  flex: 1;
  height: 100%;
  margin-top: 10px;
  border-collapse: collapse;
  font-size: 13px;
  table-layout: fixed;
}

.learning-rank th {
  padding: 8px 10px;
  color: #344154;
  font-weight: 800;
  text-align: left;
  background: #f3f6fb;
}

.learning-rank td {
  padding: 9px 10px;
  border-bottom: 1px solid #edf1f7;
}

.rank-medal,
.rank-number {
  display: inline-grid;
  width: 20px;
  height: 20px;
  place-items: center;
  font-weight: 800;
}

.rank-medal {
  color: #fff;
  border-radius: 50%;
}

.rank-medal--gold {
  background: #ffb020;
}

.rank-medal--silver {
  background: #3da1ff;
}

.rank-medal--bronze {
  background: #d9822b;
}

.learning-stats {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.learning-stat--down strong {
  color: #14a574;
}

@media (max-height: 1100px) {
  .monitor-toolbar {
    min-height: 50px;
    padding: 8px 16px;
  }

  .analysis-canvas {
    height: calc(100% - 50px);
    padding: 10px 16px 12px;
  }

  .metric-card {
    grid-template-columns: 46px minmax(0, 1fr);
    min-height: 94px;
    padding: 12px;
  }

  .metric-icon {
    width: 42px;
    height: 42px;
    font-size: 23px;
  }

  .metric-value strong {
    font-size: 26px;
  }

  .trend {
    margin-top: 6px;
  }

  .dashboard-grid {
    grid-template-rows: 430px minmax(0, 1fr);
    gap: 8px;
    height: calc(100vh - 320px);
    margin-top: 8px;
  }

  .panel-header {
    min-height: 46px;
    padding: 8px 18px 3px;
  }

  .dispatch-layout {
    min-height: 360px;
    padding: 6px 18px 14px;
  }

  .dispatch-chart,
  .dispatch-summary {
    min-height: 360px;
  }

  .panel--checks,
  .panel--learning {
    min-height: 0;
  }

  .checks-stats,
  .learning-stats {
    margin: 0 18px 8px;
    padding: 6px 0;
  }

  .learning-layout {
    gap: 14px;
    padding: 6px 18px 0;
  }

  .learning-detail {
    height: calc(100% - 46px);
    padding: 6px 18px 10px;
  }

  .learning-layout h3 {
    font-size: 13px;
    line-height: 1.2;
  }

  .learning-rank table {
    margin-top: 6px;
    font-size: 11px;
    line-height: 1.1;
  }

  .learning-rank th,
  .learning-rank td {
    padding: 4px 6px;
  }

  .learning-stats strong {
    margin-top: 4px;
    font-size: 20px;
  }

  .detail-table-wrap {
    height: calc(100% - 46px);
    padding-top: 8px;
    padding-bottom: 10px;
  }

  .detail-table {
    font-size: 12px;
  }

  .detail-table th {
    height: 30px;
  }

  .detail-table th,
  .detail-table td {
    padding: 5px 8px;
  }

  .rank-medal,
  .rank-number {
    width: 18px;
    height: 18px;
  }
}

@media (max-width: 1400px) {
  .hazard-footer {
    grid-column: 1;
  }

  .metric-strip {
    grid-template-columns: repeat(4, minmax(150px, 1fr));
  }

  .dashboard-grid,
  .hazard-layout,
  .learning-layout {
    grid-template-columns: 1fr;
  }

  .dispatch-layout,
  .dispatch-chart,
  .dispatch-summary {
    min-height: 300px;
  }

  .learning-rank {
    padding-left: 0;
    border-left: 0;
  }
}

@media (max-width: 960px) {
  .monitor-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .monitor-toolbar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-strip,
  .checks-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .analysis-canvas {
    padding: 10px;
  }

  .monitor-toolbar,
  .metric-strip,
  .dispatch-layout,
  .checks-stats,
  .learning-stats {
    grid-template-columns: 1fr;
  }
}
</style>
