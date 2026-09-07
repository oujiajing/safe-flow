<script setup lang="ts">
import type { EchartsUIType } from '@vben/plugins/echarts';

import { computed, nextTick, onMounted, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

import { Select } from 'ant-design-vue';

import {
  createEmptyMonitorCenterOverview,
  getMonitorCenterOverviewApi,
  normalizeMonitorCenterOverview,
  toMonitorCenterNumber,
  type PinganMonitorCenterApi,
} from '#/api/pingan/monitor-center';

const selectedGroup = ref<PinganMonitorCenterApi.Id>('guangsheng');
const overview = ref<PinganMonitorCenterApi.Overview>(
  createEmptyMonitorCenterOverview(),
);
const isOverviewLoading = ref(false);
const overviewLoadError = ref('');
const radarChartRef = ref<EchartsUIType>();
const trendChartRef = ref<EchartsUIType>();
const injuryTypeChartRef = ref<EchartsUIType>();
const rectificationChartRef = ref<EchartsUIType>();

const { renderEcharts: renderRadarChart } = useEcharts(radarChartRef);
const { renderEcharts: renderTrendChart } = useEcharts(trendChartRef);
const { renderEcharts: renderInjuryTypeChart } = useEcharts(injuryTypeChartRef);
const { renderEcharts: renderRectificationChart } =
  useEcharts(rectificationChartRef);

const scopeOptions = computed(() => overview.value.scopeOptions ?? []);
const groupOptions = computed(() => {
  const visibleScopes = scopeOptions.value.length
    ? scopeOptions.value
    : overview.value.companies;
  const companyOptions = visibleScopes.map((item) => ({
    label: item.name,
    value: item.id,
  }));
  return companyOptions.length
    ? [{ label: '广晟矿业集团', value: 'guangsheng' }, ...companyOptions]
    : [{ label: '广晟矿业集团', value: 'guangsheng' }];
});
const summary = computed(() => {
  const source = overview.value.summary;
  return {
    ...source,
    dispatchTotal: toMonitorCenterNumber(source.dispatchTotal),
    hazardDone: toMonitorCenterNumber(source.hazardDone),
    hazardOpen: toMonitorCenterNumber(source.hazardOpen),
    hazardTotal: toMonitorCenterNumber(source.hazardTotal),
    learningTotal: toMonitorCenterNumber(source.learningTotal),
    riskTotal: toMonitorCenterNumber(source.riskTotal),
    teamCount: toMonitorCenterNumber(source.teamCount),
  };
});

const defaultAccidentTypes = [
  '物体打击',
  '车辆伤害',
  '机械伤害',
  '起重伤害',
  '触电',
  '灼烫',
  '高处坠落',
  '火灾',
  '淹溺',
  '中毒窒息',
  '冒顶片帮',
  '透水',
  '放炮',
  '瓦斯爆炸',
  '火药爆炸',
  '容器爆炸',
  '其他伤害',
];

const riskTypeRows = computed(() =>
  (overview.value.riskAccidentTypeBars ?? [])
    .map((item) => ({ name: item.name, value: toMonitorCenterNumber(item.value) }))
    .filter((item) => item.name && item.value > 0),
);

const accidentTypes = computed(() => {
  const names = riskTypeRows.value.map((item) => item.name);
  return names.length ? names : defaultAccidentTypes;
});

const riskCompanyRows = computed(() =>
  (overview.value.riskControlBars ?? []).map((item) => {
    const major = toMonitorCenterNumber(item.major);
    const serious = toMonitorCenterNumber(item.serious);
    const normal = toMonitorCenterNumber(item.normal);
    const low = toMonitorCenterNumber(item.low);
    return {
      low,
      major,
      name: item.name,
      normal,
      serious,
      total: major + serious + normal + low,
    };
  }),
);

const accidentRecords = computed(() => overview.value.hazardRecordRows ?? []);

const statCards = computed(() => [
  {
    icon: 'lucide:users',
    label: '班组数',
    source: '来自组织架构',
    suffix: '个',
    tone: 'blue',
    value: String(toMonitorCenterNumber(summary.value.teamCount)),
  },
  {
    icon: 'lucide:clock-3',
    label: '作业派班',
    source: '来自一班三查',
    suffix: '次',
    tone: 'cyan',
    value: String(toMonitorCenterNumber(summary.value.dispatchTotal)),
  },
  {
    icon: 'lucide:timer',
    label: '未闭环隐患',
    source: '来自隐患排查',
    suffix: '条',
    tone: 'blue',
    value: String(toMonitorCenterNumber(summary.value.hazardOpen)),
  },
  {
    icon: 'lucide:users-round',
    label: '重大风险',
    source: '来自风险管控',
    suffix: '项',
    tone: 'green',
    value: String(
      (overview.value.riskControlBars ?? []).reduce(
        (total, item) => total + toMonitorCenterNumber(item.major),
        0,
      ),
    ),
  },
  {
    icon: 'lucide:bandage',
    label: '安全学习',
    source: '来自宣教培训',
    suffix: '次',
    tone: 'orange',
    value: String(toMonitorCenterNumber(summary.value.learningTotal)),
  },
  {
    icon: 'lucide:clipboard-list',
    label: '特殊作业',
    source: '来自特殊作业',
    suffix: '项',
    tone: 'sky',
    value: String(
      (overview.value.specialWorkTypeBars ?? []).reduce(
        (total, item) => total + toMonitorCenterNumber(item.value),
        0,
      ),
    ),
  },
]);

const unavailableCards = [
  { label: '真实事故记录', value: '暂无真实事故数据源' },
  { label: '伤亡等级', value: '待事故专用接口' },
  { label: '损失工时', value: '待工时事实源' },
  { label: 'TRIFR 指标', value: '待事故专用接口' },
];

const reasonRows = computed(() =>
  (overview.value.riskCauseBars ?? [])
    .map((item) => ({ name: item.name, value: toMonitorCenterNumber(item.value) }))
    .filter((item) => item.name && item.value > 0)
    .slice(0, 5),
);

const rectificationItems = computed(() => {
  const done = toMonitorCenterNumber(summary.value.hazardDone);
  const open = toMonitorCenterNumber(summary.value.hazardOpen);
  return [
    { name: '已闭环隐患', tone: 'green-bg', value: done },
    { name: '未闭环隐患', tone: 'red-bg', value: open },
  ];
});

onMounted(async () => {
  await loadMonitorOverview();
  await nextTick();
  renderCharts();
});

watch(selectedGroup, () => {
  void loadMonitorOverview();
});

async function loadMonitorOverview() {
  isOverviewLoading.value = true;
  overviewLoadError.value = '';
  try {
    const orgId =
      selectedGroup.value === 'guangsheng' ? undefined : selectedGroup.value;
    overview.value = normalizeMonitorCenterOverview(
      await getMonitorCenterOverviewApi({ orgId }),
    );
  } catch {
    overview.value = createEmptyMonitorCenterOverview();
    overviewLoadError.value = '事故看板业务汇总数据加载失败，暂无真实模块数据';
  } finally {
    isOverviewLoading.value = false;
    await nextTick();
    renderCharts();
  }
}

function percent(numerator: number, denominator: number) {
  if (denominator <= 0) {
    return '0.00%';
  }
  return `${((numerator / denominator) * 100).toFixed(2)}%`;
}

function renderCharts() {
  const riskTypeValues = accidentTypes.value.map(
    (name) => riskTypeRows.value.find((item) => item.name === name)?.value ?? 0,
  );
  const maxRiskType = Math.max(1, ...riskTypeValues);

  renderRadarChart({
    color: ['#23d3d1'],
    radar: {
      axisName: { color: '#c6dbef', fontSize: 12 },
      indicator: accidentTypes.value.map((name) => ({ max: maxRiskType, name })),
      radius: '70%',
      splitArea: { show: false },
      splitLine: { lineStyle: { color: 'rgba(105, 177, 255, 0.25)' } },
      axisLine: { lineStyle: { color: 'rgba(105, 177, 255, 0.28)' } },
    },
    series: [
      {
        areaStyle: { color: 'rgba(35, 211, 209, 0.35)' },
        data: [{ value: riskTypeValues }],
        lineStyle: { color: '#23d3d1', width: 2 },
        symbol: 'none',
        type: 'radar',
      },
    ],
    tooltip: { trigger: 'item' },
  });

  renderTrendChart({
    color: ['#4d8cff'],
    grid: { bottom: 38, containLabel: true, left: 40, right: 40, top: 42 },
    legend: {
      data: ['真实事故趋势待接入'],
      icon: 'roundRect',
      textStyle: { color: '#cde7ff' },
      top: 4,
    },
    series: [
      {
        barWidth: 22,
        data: [],
        itemStyle: { color: '#4d8cff' },
        label: { color: '#8ce4ff', position: 'top', show: true },
        name: '真实事故趋势待接入',
        type: 'bar',
      },
    ],
    tooltip: { trigger: 'axis' },
    xAxis: {
      axisLabel: { color: '#c8d9ee' },
      axisLine: { lineStyle: { color: '#406c9b' } },
      axisTick: { show: false },
      data: [],
      type: 'category',
    },
    yAxis: [
      {
        axisLabel: { color: '#c8d9ee' },
        max: 1,
        splitLine: { lineStyle: { color: 'rgba(95, 154, 213, 0.18)' } },
        type: 'value',
      },
    ],
  });

  renderInjuryTypeChart({
    color: ['#4f88e8'],
    grid: { bottom: 12, containLabel: true, left: 12, right: 18, top: 26 },
    legend: {
      data: ['风险项'],
      icon: 'roundRect',
      itemGap: 12,
      itemHeight: 8,
      itemWidth: 9,
      textStyle: { color: '#d2e7ff', fontSize: 11 },
      top: 2,
    },
    series: [
      {
        data: riskTypeRows.value.map((item) => item.value),
        label: { color: '#fff', position: 'inside', show: true },
        name: '风险项',
        barMaxWidth: 16,
        type: 'bar',
      },
    ],
    tooltip: { trigger: 'axis' },
    xAxis: {
      axisLabel: { color: '#c8d9ee' },
      axisLine: { lineStyle: { color: '#406c9b' } },
      max: Math.max(4, ...riskTypeRows.value.map((item) => item.value)),
      splitLine: { lineStyle: { color: 'rgba(95, 154, 213, 0.18)' } },
      type: 'value',
    },
    yAxis: {
      axisLabel: { color: '#c8d9ee', fontSize: 11 },
      axisTick: { show: false },
      data: riskTypeRows.value.map((item) => item.name),
      inverse: true,
      type: 'category',
    },
  });

  renderRectificationChart({
    color: ['#80d36d', '#ff4d42'],
    series: [
      {
        data: rectificationItems.value.map((item) => ({
          name: item.name,
          value: item.value,
        })),
        label: { show: false },
        radius: ['58%', '78%'],
        type: 'pie',
      },
    ],
    tooltip: { trigger: 'item' },
  });
}
</script>

<template>
  <section class="accident-dashboard">
    <header class="accident-header">
      <Select
        v-model:value="selectedGroup"
        :options="groupOptions"
        class="group-select"
      />
      <h1>风险隐患事故预警看板</h1>
      <time>
        <span v-if="isOverviewLoading">加载中</span>
        <span v-else-if="overviewLoadError">{{ overviewLoadError }}</span>
        <span v-else>2026年5月29日&nbsp;&nbsp;14:44:30</span>
      </time>
    </header>

    <main class="accident-grid">
      <section class="panel radar-panel">
        <div class="panel-title">
          <IconifyIcon icon="lucide:book-open" />
          <span>可能事故类型雷达图</span>
        </div>
        <EchartsUI ref="radarChartRef" class="radar-chart" />
        <footer>单位：项 <span>来源：风险管控可能事故类型，非真实事故数</span></footer>
      </section>

      <section class="panel detail-panel">
        <div class="panel-title">
          <IconifyIcon icon="lucide:scroll-text" />
          <span>隐患整改记录（非事故记录）</span>
          <div class="record-tabs">
            <button class="active">全部</button>
            <button>安全检查</button>
            <button>隐患整改</button>
            <button>已整改</button>
            <button>更多 ></button>
          </div>
        </div>
        <div class="record-list">
          <article v-for="item in accidentRecords" :key="`${item.date}-${item.type}-${item.detail}`">
            <strong class="level-minor">
              {{ item.sourceModule }}
            </strong>
            <div>
              <h3>{{ item.type }} <span>|</span> {{ item.team }}</h3>
              <p>{{ item.company }}</p>
              <p>{{ item.detail }}</p>
            </div>
            <time>{{ item.date }}</time>
            <em :class="`status-${item.statusTone}`">{{ item.status }}</em>
          </article>
          <div v-if="accidentRecords.length === 0" class="empty-state">
            暂无真实事故数据源，当前无可展示的隐患整改记录
          </div>
        </div>
        <footer class="record-summary">
          已闭环隐患 <b class="green">{{ summary.hazardDone }}</b> 条
          未闭环隐患 <b class="red">{{ summary.hazardOpen }}</b> 条
          真实事故记录 <b class="orange">待事故专用接口</b>
        </footer>
      </section>

      <section class="panel table-panel">
        <div class="panel-title">
          <IconifyIcon icon="lucide:chart-column" />
          <span>所属企业风险等级统计表（来自风险管控，非事故伤害）</span>
        </div>
        <table class="accident-table">
          <thead>
            <tr>
              <th>责任单位</th>
              <th>重大风险</th>
              <th>较大风险</th>
              <th>一般风险</th>
              <th>低风险</th>
              <th>合计</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in riskCompanyRows" :key="row.name">
              <th>{{ row.name }}</th>
              <td>{{ row.major }}</td>
              <td>{{ row.serious }}</td>
              <td>{{ row.normal }}</td>
              <td>{{ row.low }}</td>
              <td>
                <span>{{ row.total }}</span>
                <i :style="{ width: `${Math.max(row.total / Math.max(summary.riskTotal, 1) * 34, 4)}px` }"></i>
              </td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <th>合计</th>
              <td>{{ riskCompanyRows.reduce((total, row) => total + row.major, 0) }}</td>
              <td>{{ riskCompanyRows.reduce((total, row) => total + row.serious, 0) }}</td>
              <td>{{ riskCompanyRows.reduce((total, row) => total + row.normal, 0) }}</td>
              <td>{{ riskCompanyRows.reduce((total, row) => total + row.low, 0) }}</td>
              <td>{{ summary.riskTotal }}</td>
            </tr>
          </tfoot>
        </table>
      </section>

      <section class="panel trend-panel">
        <div class="panel-title">
          <IconifyIcon icon="lucide:chart-no-axes-combined" />
          <span>真实事故趋势（待事故专用接口）</span>
          <small>暂无真实事故数据源</small>
        </div>
        <EchartsUI ref="trendChartRef" class="trend-chart" />
        <div class="empty-state empty-state--overlay">暂无真实事故数据源，不能以风险/隐患样例替代事故趋势</div>
      </section>

      <section class="panel stats-panel">
        <div class="panel-title">
          <span>统计情况</span>
          <small>数据截至：2026-05-29</small>
        </div>
        <div class="stat-grid">
          <article
            v-for="item in statCards"
            :key="item.label"
            :class="`stat-card stat-card--${item.tone}`"
          >
            <IconifyIcon :icon="item.icon" />
            <div>
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <em>{{ item.suffix }}</em>
              <small>{{ item.source }}</small>
            </div>
          </article>
        </div>
        <div class="unavailable-grid">
          <article v-for="item in unavailableCards" :key="item.label">
            <span>{{ item.label }}</span>
            <b>{{ item.value }}</b>
          </article>
        </div>
      </section>

      <section class="panel injury-panel">
        <div class="panel-title">
          <IconifyIcon icon="lucide:book-open" />
          <span>可能事故类型统计</span>
          <small>来源：风险管控，非真实伤害类型</small>
        </div>
        <EchartsUI ref="injuryTypeChartRef" class="injury-type-chart" />
      </section>

      <section class="panel reason-panel">
        <div class="panel-title">
          <IconifyIcon icon="lucide:siren" />
          <span>风险原因及隐患整改情况</span>
          <small>来自风险管控和隐患排查</small>
        </div>
        <div class="reason-layout">
          <div class="rectification-wrap">
            <EchartsUI ref="rectificationChartRef" class="rectification-chart" />
            <div class="rectification-center">
              <span>整改闭环</span>
              <strong>{{ summary.hazardFixedRate }}</strong>
            </div>
          </div>
          <ul class="rectification-legend">
            <li v-for="item in rectificationItems" :key="item.name">
              <i :class="item.tone"></i>{{ item.name }} <b>{{ item.value }} 条</b>
              <span>{{ percent(item.value, summary.hazardTotal) }}</span>
            </li>
          </ul>
          <div class="reason-top">
            <h3>风险原因TOP5（项）</h3>
            <div v-for="item in reasonRows" :key="item.name" class="reason-row">
              <span>{{ item.name }}</span>
              <i><b :style="{ width: `${item.value / Math.max(reasonRows[0]?.value ?? 1, 1) * 100}%` }"></b></i>
              <em>{{ item.value }}</em>
            </div>
            <div v-if="reasonRows.length === 0" class="empty-state">
              暂无风险原因数据
            </div>
          </div>
        </div>
      </section>
    </main>
  </section>
</template>

<style scoped>
.accident-dashboard {
  --panel-bg: linear-gradient(180deg, rgb(4 50 98 / 88%), rgb(1 35 76 / 94%));
  --panel-border: #075f9f;
  --cyan: #34d9ff;
  --text: #d8eaff;

  min-width: 1440px;
  min-height: 812px;
  padding: 10px 6px 8px;
  overflow: hidden;
  color: var(--text);
  background:
    radial-gradient(circle at 50% 0%, rgb(19 97 191 / 42%), transparent 24%),
    linear-gradient(180deg, #04152c 0%, #001225 100%);
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
}

.accident-header {
  position: relative;
  display: grid;
  grid-template-columns: 220px 1fr 260px;
  align-items: center;
  height: 48px;
  margin-bottom: 8px;
  border-bottom: 1px solid #0a69ac;
  background: linear-gradient(90deg, rgb(1 30 69 / 70%), rgb(1 55 114 / 42%), rgb(1 30 69 / 70%));
}

.group-select {
  width: 190px;
  margin-left: 6px;
}

.group-select :deep(.ant-select-selector) {
  height: 34px;
  color: #cfe6ff;
  background: rgb(6 45 88 / 88%) !important;
  border-color: #0c75bc !important;
  border-radius: 3px;
}

.group-select :deep(.ant-select-selection-item) {
  color: #d8eaff;
}

.accident-header h1 {
  position: relative;
  margin: 0 auto;
  padding: 0 68px;
  color: #fff;
  font-size: 33px;
  font-weight: 900;
  text-align: center;
  text-shadow: 0 0 16px rgb(77 167 255 / 84%);
}

.accident-header h1::before,
.accident-header h1::after {
  position: absolute;
  top: 50%;
  width: 120px;
  height: 48px;
  content: "";
  border-top: 2px solid #1c86e8;
  transform: translateY(-50%) skew(38deg);
}

.accident-header h1::before {
  right: 100%;
  border-left: 2px solid #1c86e8;
}

.accident-header h1::after {
  left: 100%;
  border-right: 2px solid #1c86e8;
  transform: translateY(-50%) skew(-38deg);
}

.accident-header time {
  color: #b8c9dc;
  font-size: 16px;
}

.accident-grid {
  display: grid;
  grid-template-areas:
    "radar table stats"
    "records table injury"
    "records trend reason";
  grid-template-columns: 26fr 44fr 30fr;
  grid-template-rows: minmax(0, 30fr) minmax(0, 24fr) minmax(0, 46fr);
  gap: 8px;
  height: calc(100vh - 212px);
  min-height: 700px;
}

.panel {
  position: relative;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  border-radius: 6px;
  box-shadow:
    inset 0 0 22px rgb(16 121 207 / 24%),
    0 0 12px rgb(0 79 152 / 28%);
}

.radar-panel {
  grid-area: radar;
}

.detail-panel {
  grid-area: records;
}

.table-panel {
  grid-area: table;
  display: flex;
  flex-direction: column;
}

.trend-panel {
  grid-area: trend;
}

.stats-panel {
  grid-area: stats;
}

.injury-panel {
  grid-area: injury;
  min-height: 170px;
}

.reason-panel {
  grid-area: reason;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 12px;
  color: #e8f6ff;
  font-size: 17px;
  font-weight: 900;
}

.panel-title svg {
  color: var(--cyan);
  font-size: 20px;
}

.panel-title small {
  margin-left: auto;
  color: #8fa8c6;
  font-size: 12px;
  font-weight: 600;
}

.panel-title span {
  white-space: nowrap;
}

.radar-chart {
  height: calc(100% - 66px);
}

.radar-panel footer {
  display: flex;
  justify-content: space-between;
  padding: 0 22px 10px;
  color: #8fa8c6;
  font-size: 12px;
}

.record-tabs {
  display: flex;
  flex-shrink: 0;
  margin-left: auto;
  border: 1px solid #0b69aa;
}

.record-tabs button {
  height: 28px;
  padding: 0 14px;
  color: #bdd7f0;
  background: rgb(3 39 84 / 72%);
  border: 0;
  border-right: 1px solid #0b69aa;
  font-size: 12px;
}

.record-tabs button:last-child {
  border-right: 0;
}

.record-tabs button.active {
  color: #fff;
  background: #065fa5;
}

.record-list {
  display: grid;
  grid-template-rows: repeat(5, minmax(86px, 1fr));
  gap: 5px;
  height: calc(100% - 72px);
  padding: 4px 12px 30px;
}

.record-list article {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 72px 58px;
  gap: 8px;
  align-items: start;
  min-height: 86px;
  padding: 8px;
  background: rgb(2 54 100 / 66%);
  border-radius: 4px;
  box-shadow: inset 0 0 10px rgb(31 148 255 / 10%);
}

.record-list strong,
.record-list em {
  display: inline-grid;
  height: 24px;
  place-items: center;
  border-radius: 3px;
  font-style: normal;
  font-weight: 800;
}

.level-major {
  color: #ffd8d4;
  background: #b83735;
}

.level-minor {
  color: #fff2d5;
  background: #bd7d24;
}

.record-list h3 {
  margin: 0 0 4px;
  color: #eaf7ff;
  font-size: 14px;
}

.record-list h3 span {
  color: #5487b9;
}

.record-list p {
  margin: 0 0 2px;
  overflow: hidden;
  color: #9eb6cf;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-list time {
  color: #8fa8c6;
  font-size: 12px;
}

.status-red {
  color: #ff7268;
  border: 1px solid #c33b35;
}

.status-amber {
  color: #ffc267;
  border: 1px solid #b87718;
}

.status-green {
  color: #6fe28c;
  border: 1px solid #2a9e55;
}

.record-summary {
  position: absolute;
  right: 12px;
  bottom: 6px;
  left: 12px;
  display: flex;
  justify-content: space-around;
  color: #c7d7ea;
  font-size: 13px;
}

.red {
  color: #ff4d42;
}

.orange {
  color: #ffae31;
}

.green {
  color: #68da7a;
}

.accident-table {
  width: calc(100% - 18px);
  flex: 1;
  margin: 4px 9px 10px;
  color: #e4f1ff;
  border-collapse: collapse;
  font-size: 13px;
}

.accident-table th,
.accident-table td {
  height: 37px;
  padding: 0 8px;
  text-align: center;
  border-bottom: 1px solid rgb(70 132 189 / 25%);
}

.accident-table th:first-child {
  text-align: left;
}

.accident-table thead th,
.accident-table tfoot th,
.accident-table tfoot td {
  color: #d9efff;
  font-weight: 900;
}

.accident-table tbody th {
  color: #bed3e8;
  font-weight: 700;
}

.accident-table tbody td:last-child {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
}

.accident-table td i {
  display: inline-block;
  height: 8px;
  background: #4d8cff;
  border-radius: 1px;
}

.trend-chart {
  height: min(400px, calc(100% - 42px)) !important;
}

.injury-type-chart {
  height: calc(100% - 38px) !important;
  min-height: 0;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-auto-rows: minmax(74px, 1fr);
  gap: 6px;
  height: calc(100% - 114px);
  padding: 2px 10px 10px;
}

.stat-card {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  column-gap: 4px;
  align-items: center;
  min-height: 88px;
  padding: 8px;
  background: linear-gradient(135deg, rgb(9 90 159 / 88%), rgb(4 58 112 / 88%));
  border: 1px solid #0d75bc;
  border-radius: 4px;
}

.stat-card svg {
  display: grid;
  width: 32px;
  height: 32px;
  padding: 7px;
  color: #fff;
  background: #1f73dc;
  border-radius: 50%;
}

.stat-card span {
  display: block;
  color: #d6e8fa;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.stat-card strong {
  margin-right: 4px;
  color: #3fe9ff;
  font-size: 20px;
  font-weight: 900;
  white-space: nowrap;
}

.stat-card em {
  color: #d6e8fa;
  font-style: normal;
  font-size: 12px;
  white-space: nowrap;
}

.stat-card small {
  display: block;
  grid-column: 1 / -1;
  margin-top: 2px;
  overflow: hidden;
  color: #92aec9;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unavailable-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  padding: 0 10px 10px;
}

.unavailable-grid article {
  min-height: 30px;
  padding: 5px 8px;
  background: rgb(44 61 82 / 52%);
  border: 1px dashed rgb(151 176 202 / 36%);
  border-radius: 4px;
}

.unavailable-grid span {
  display: block;
  color: #b8c9dc;
  font-size: 11px;
}

.unavailable-grid b {
  display: block;
  margin-top: 2px;
  overflow: hidden;
  color: #ffc267;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state {
  display: grid;
  min-height: 70px;
  place-items: center;
  padding: 12px;
  color: #ffc267;
  font-size: 13px;
  text-align: center;
  background: rgb(44 61 82 / 42%);
  border: 1px dashed rgb(151 176 202 / 32%);
  border-radius: 4px;
}

.empty-state--overlay {
  position: absolute;
  right: 24px;
  bottom: 18px;
  left: 24px;
  min-height: 42px;
  pointer-events: none;
}

.stat-card--orange strong {
  color: #ffb02f;
}

.stat-card--green strong {
  color: #25e3b5;
}

.reason-layout {
  display: grid;
  grid-template-columns: 150px 150px minmax(140px, 1fr);
  gap: 14px;
  align-items: center;
  height: calc(100% - 40px);
  padding: 4px 18px 14px;
}

.rectification-wrap {
  position: relative;
}

.rectification-chart {
  width: 150px;
  height: 150px;
}

.rectification-center {
  position: absolute;
  inset: 0;
  display: grid;
  place-content: center;
  text-align: center;
}

.rectification-center span {
  color: #cfe2f7;
  font-weight: 900;
}

.rectification-center strong {
  color: #9de172;
  font-size: 30px;
}

.rectification-legend {
  display: grid;
  gap: 12px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.rectification-legend li {
  display: grid;
  grid-template-columns: 12px 44px 46px 1fr;
  gap: 7px;
  align-items: center;
  color: #c7d7ea;
  font-size: 12px;
}

.rectification-legend i {
  width: 10px;
  height: 10px;
}

.green-bg {
  background: #80d36d;
}

.orange-bg {
  background: #ffbd3e;
}

.red-bg {
  background: #ff4d42;
}

.reason-top {
  padding-left: 18px;
  border-left: 1px solid rgb(107 151 199 / 35%);
}

.reason-top h3 {
  margin: 0 0 14px;
  color: #cfe2f7;
  font-size: 14px;
  text-align: center;
  white-space: nowrap;
}

.reason-row {
  display: grid;
  grid-template-columns: 98px minmax(0, 1fr) 28px;
  gap: 8px;
  align-items: center;
  margin-bottom: 11px;
  color: #dceaff;
  font-size: 13px;
}

.reason-row i {
  height: 12px;
  background: rgb(54 93 142 / 68%);
}

.reason-row b {
  display: block;
  height: 100%;
  background: #4d8cff;
}

.reason-row em {
  color: #4d8cff;
  font-style: normal;
}

@media (max-width: 1500px) {
  .accident-dashboard {
    min-width: 1320px;
  }

  .accident-header h1 {
    font-size: 30px;
  }

  .panel-title {
    gap: 6px;
    padding: 0 10px;
    font-size: 16px;
  }

  .record-tabs button {
    padding: 0 9px;
  }

  .stat-card {
    grid-template-columns: 32px minmax(0, 1fr);
    column-gap: 4px;
    padding: 6px;
  }

  .stat-card svg {
    width: 30px;
    height: 30px;
    padding: 6px;
  }

  .stat-card strong {
    font-size: 18px;
  }

  .reason-layout {
    grid-template-columns: 128px 122px minmax(96px, 1fr);
    gap: 10px;
    padding-right: 12px;
    padding-left: 12px;
  }

  .rectification-chart {
    width: 128px;
    height: 128px;
  }

  .rectification-center strong {
    font-size: 26px;
  }

  .rectification-legend li {
    grid-template-columns: 10px 40px 42px 1fr;
    gap: 5px;
  }

  .reason-top {
    padding-left: 12px;
  }

  .reason-top h3 {
    font-size: 14px;
    text-align: left;
  }

  .reason-row {
    grid-template-columns: 82px minmax(0, 1fr) 24px;
    gap: 6px;
    font-size: 12px;
  }
}
</style>
