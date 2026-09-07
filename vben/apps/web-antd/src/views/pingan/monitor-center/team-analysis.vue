<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Checkbox, DatePicker, Input, Select, Tooltip } from 'ant-design-vue';

import {
  createEmptyMonitorCenterOverview,
  flattenMonitorCenterScopeOptions,
  getMonitorCenterOverviewApi,
  normalizeMonitorCenterOverview,
  toMonitorCenterNumber,
  type PinganMonitorCenterApi,
} from '#/api/pingan/monitor-center';

const searchKeyword = ref('');
const selectedGroup = ref('guangsheng');
const selectedCompany = ref<PinganMonitorCenterApi.Id | 'all-company'>('all-company');
const selectedTeam = ref<PinganMonitorCenterApi.Id | 'all-team'>('all-team');
const dateRange = ref<[string, string]>(['2026-06-01', '2026-06-01']);
const overview = ref<PinganMonitorCenterApi.Overview>(
  createEmptyMonitorCenterOverview(),
);
const loading = ref(false);
const loadError = ref('');
type DetailTableView = 'detail' | 'summary';
type DetailColumnKey =
  | 'action'
  | 'captain'
  | 'company'
  | 'dispatch'
  | 'hazardTotal'
  | 'learning'
  | 'mid'
  | 'openHazards'
  | 'overall'
  | 'post'
  | 'preCheck'
  | 'preMeeting'
  | 'safety'
  | 'status'
  | 'workshop';

const activeDetailView = ref<DetailTableView>('summary');
const showTableConfig = ref(false);
const currentPage = ref(1);
const pageJump = ref(1);
const pageSize = ref(10);
const visibleDetailColumns = ref<Record<DetailColumnKey, boolean>>({
  action: true,
  captain: true,
  company: true,
  dispatch: true,
  hazardTotal: true,
  learning: true,
  mid: true,
  openHazards: true,
  overall: true,
  post: true,
  preCheck: true,
  preMeeting: true,
  safety: true,
  status: true,
  workshop: true,
});

const groupOptions = [{ label: '广晟矿业集团', value: 'guangsheng' }];
const pageSizeOptions = [
  { label: '10条/页', value: 10 },
  { label: '20条/页', value: 20 },
  { label: '50条/页', value: 50 },
];
const scopeOptions = computed(() => overview.value.scopeOptions ?? []);
const flatScopeOptions = computed(() =>
  flattenMonitorCenterScopeOptions(scopeOptions.value),
);
const companyOptions = computed(() => [
  { label: '全部公司', value: 'all-company' },
  ...flatScopeOptions.value
    .filter((item) => item.orgType?.toUpperCase() === 'COMPANY')
    .map((item) => ({ label: item.name, value: item.id })),
]);
const teamOptions = computed(() => [
  { label: '全部班组', value: 'all-team' },
  ...flatScopeOptions.value
    .filter((item) => item.orgType?.toUpperCase() === 'TEAM')
    .map((item) => ({ label: item.name, value: item.id })),
]);
const teamAnalysis = computed(() => overview.value.teamAnalysis);
const averageCompletionRateTip =
  '平均完成率 = 已完成单元数 / 应完成单元数 × 100%。已完成单元数包括已完成派班、班前会、班前检查、班中检查、班后检查；应完成单元数 = 派班数 × 5。';

const summaryCards = computed(() => [
  {
    icon: 'lucide:users-round',
    label: '班组总数',
    note: '实时统计',
    tone: 'blue',
    unit: '个',
    value: String(toMonitorCenterNumber(teamAnalysis.value.summary.teamCount)),
  },
  {
    icon: 'lucide:shield-check',
    label: '正常班组',
    note: '无未整改隐患和未完成三查',
    tone: 'green',
    unit: '个',
    value: String(toMonitorCenterNumber(teamAnalysis.value.summary.normalTeams)),
  },
  {
    icon: 'lucide:triangle-alert',
    label: '异常班组',
    note: '存在未整改隐患或未完成三查',
    tone: 'amber',
    unit: '个',
    value: String(toMonitorCenterNumber(teamAnalysis.value.summary.abnormalTeams)),
  },
  {
    icon: 'lucide:clipboard-list',
    label: '未完成三查',
    note: '按派班应完成三查项统计',
    tone: 'blue',
    unit: '个',
    value: String(toMonitorCenterNumber(teamAnalysis.value.summary.unfinishedThreeChecks)),
  },
  {
    icon: 'lucide:circle-alert',
    label: '未整改隐患',
    note: '隐患整改模块未闭环',
    tone: 'red',
    unit: '项',
    value: String(toMonitorCenterNumber(teamAnalysis.value.summary.openHazards)),
  },
  {
    icon: 'lucide:pie-chart',
    label: '平均完成率',
    note: '派班和三查综合完成',
    tip: averageCompletionRateTip,
    tone: 'blue',
    unit: '',
    value: teamAnalysis.value.summary.averageCompletionRate || '0.00%',
  },
]);

const threeCheckTopTeams = computed(
  () => teamAnalysis.value.metricRankings.threeCheckTopTeams,
);
const openHazardTopTeams = computed(
  () => teamAnalysis.value.metricRankings.openHazardTopTeams,
);

const heatmapColumns = [
  '派班',
  '班前会',
  '班前检查',
  '班中检查',
  '班后检查',
  '隐患',
  '学习活跃度',
];
const heatmapRows = computed(() => teamAnalysis.value.heatmapRows);
const teamDetailRows = computed(() => teamAnalysis.value.detailRows);
const detailColumnOptions: Array<{ key: DetailColumnKey; label: string; width: number }> = [
  { key: 'company', label: '公司', width: 160 },
  { key: 'workshop', label: '车间', width: 132 },
  { key: 'captain', label: '班班长', width: 112 },
  { key: 'safety', label: '安全员', width: 112 },
  { key: 'status', label: '班组状态', width: 104 },
  { key: 'dispatch', label: '派班完成率', width: 112 },
  { key: 'preMeeting', label: '班前会', width: 96 },
  { key: 'preCheck', label: '班前检查', width: 104 },
  { key: 'mid', label: '班中检查', width: 104 },
  { key: 'post', label: '班后检查', width: 104 },
  { key: 'overall', label: '三查整体', width: 104 },
  { key: 'hazardTotal', label: '隐患数', width: 92 },
  { key: 'openHazards', label: '未整改', width: 92 },
  { key: 'learning', label: '公司学习次数', width: 124 },
  { key: 'action', label: '操作', width: 88 },
];
const summaryTableRows = computed(() =>
  teamDetailRows.value.map((row) => ({
    dispatchCompletionRate: row.dispatchCompletionRate,
    hazardTotal: row.hazardTotal,
    name: row.name,
    openHazards: row.openHazards,
    rank: row.rank,
    status: row.status,
    studyCount: row.companyLearningCount,
    threeCheckCompletionRate: row.threeCheckCompletionRate,
  })),
);

const heatmapLegend = [
  { label: '正常(0-5%)', tone: 'normal' },
  { label: '较低(5-15%)', tone: 'low' },
  { label: '一般(15-30%)', tone: 'medium' },
  { label: '较高(30-50%)', tone: 'high' },
  { label: '很高(>50%)', tone: 'danger' },
];
const heatmapAbnormalRateTip =
  '异常率 = 异常项数量 / 应检查或应完成项数量 × 100%。派班、三查按未完成项计算；隐患按未整改占隐患总数计算；学习活跃度按未达标项计算。';
const totalRows = computed(() =>
  activeDetailView.value === 'summary'
    ? summaryTableRows.value.length
    : teamDetailRows.value.length,
);
const totalPages = computed(() =>
  Math.max(1, Math.ceil(totalRows.value / pageSize.value)),
);
const visiblePageNumbers = computed(() => {
  const pages = new Set<number>([1, totalPages.value]);
  for (let page = currentPage.value - 1; page <= currentPage.value + 1; page += 1) {
    if (page >= 1 && page <= totalPages.value) {
      pages.add(page);
    }
  }
  return [...pages].sort((left, right) => left - right);
});
const pagedSummaryTableRows = computed(() =>
  currentPageRows(summaryTableRows.value),
);
const pagedTeamDetailRows = computed(() =>
  currentPageRows(teamDetailRows.value),
);

function currentPageRows<T>(rows: T[]) {
  const start = (currentPage.value - 1) * pageSize.value;
  return rows.slice(start, start + pageSize.value);
}

function setCurrentPage(page: number) {
  if (!Number.isFinite(page)) {
    currentPage.value = 1;
    return;
  }
  currentPage.value = Math.min(Math.max(1, Math.trunc(page)), totalPages.value);
}

function goToJumpPage() {
  setCurrentPage(pageJump.value);
}

onMounted(() => {
  void loadMonitorOverview();
});

watch([selectedCompany, selectedTeam, dateRange], () => {
  setCurrentPage(1);
  void loadMonitorOverview();
});

watch([activeDetailView, pageSize], () => {
  setCurrentPage(1);
});

watch(totalPages, () => {
  setCurrentPage(currentPage.value);
});

watch(currentPage, (page) => {
  pageJump.value = page;
});

async function loadMonitorOverview() {
  loading.value = true;
  loadError.value = '';
  try {
    const [dateStart, dateEnd] = dateRange.value;
    const orgId =
      selectedTeam.value !== 'all-team'
        ? selectedTeam.value
        : selectedCompany.value !== 'all-company'
          ? selectedCompany.value
          : undefined;
    overview.value = normalizeMonitorCenterOverview(
      await getMonitorCenterOverviewApi({ dateEnd, dateStart, orgId }),
    );
  } catch {
    overview.value = createEmptyMonitorCenterOverview();
    loadError.value = '班组分析汇总数据加载失败';
  } finally {
    loading.value = false;
  }
}

function heatLevel(value: number) {
  if (value > 50) return 'danger';
  if (value >= 30) return 'high';
  if (value >= 15) return 'medium';
  if (value >= 5) return 'low';
  return 'normal';
}

function csvCell(value: unknown) {
  const text = String(value ?? '');
  return /[",\r\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
}

function downloadCsv(filename: string, headers: string[], rows: unknown[][]) {
  const csv = [headers, ...rows]
    .map((row) => row.map((cell) => csvCell(cell)).join(','))
    .join('\r\n');
  const blob = new Blob([`\uFEFF${csv}`], {
    type: 'text/csv;charset=utf-8;',
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

function downloadCurrentTeamTable() {
  if (activeDetailView.value === 'summary') {
    downloadCsv(
      '班组分析_指标汇总.csv',
      [
        '序号',
        '班组',
        '班组状态',
        '派班完成率',
        '三查整体',
        '隐患数',
        '未整改',
        '公司学习次数',
      ],
      summaryTableRows.value.map((row) => [
        row.rank,
        row.name,
        row.status,
        row.dispatchCompletionRate,
        row.threeCheckCompletionRate,
        row.hazardTotal,
        row.openHazards,
        row.studyCount,
      ]),
    );
    return;
  }

  downloadCsv(
    '班组分析_明细列表.csv',
    [
      '序号',
      '班组',
      '公司',
      '车间',
      '班班长',
      '安全员',
      '班组状态',
      '派班完成率',
      '班前会',
      '班前检查',
      '班中检查',
      '班后检查',
      '三查整体',
      '隐患数',
      '未整改',
      '公司学习次数',
    ],
    teamDetailRows.value.map((row) => [
      row.rank,
      row.name,
      row.company,
      row.workshop,
      row.captain,
      row.safety,
      row.status,
      row.dispatchCompletionRate,
      row.preMeeting,
      row.preCheck,
      row.mid,
      row.post,
      row.threeCheckCompletionRate,
      row.hazardTotal,
      row.openHazards,
      row.companyLearningCount,
    ]),
  );
}
</script>

<template>
  <section class="team-analysis-page">
    <header class="filter-bar">
      <Input
        v-model:value="searchKeyword"
        class="search-box"
        placeholder="使用公司/班组 点击选择或回车"
      >
        <template #suffix>
          <IconifyIcon icon="lucide:search" />
        </template>
      </Input>
      <Select
        v-model:value="selectedGroup"
        :options="groupOptions"
        class="filter-select"
      />
      <Select
        v-model:value="selectedCompany"
        :options="companyOptions"
        class="filter-select"
      />
      <Select
        v-model:value="selectedTeam"
        :options="teamOptions"
        class="filter-select"
      />
      <DatePicker.RangePicker
        v-model:value="dateRange"
        class="date-range"
        value-format="YYYY-MM-DD"
      />
      <div class="toolbar-spacer"></div>
      <span v-if="loading || loadError" class="data-status">
        {{ loading ? '加载中' : loadError }}
      </span>
    </header>

    <main class="team-analysis-canvas">
      <section class="summary-strip">
        <article
          v-for="item in summaryCards"
          :key="item.label"
          :class="['summary-card', `summary-card--${item.tone}`]"
        >
          <span class="summary-icon">
            <IconifyIcon :icon="item.icon" />
          </span>
          <div class="summary-copy">
            <h3>
              {{ item.label }}
              <Tooltip v-if="item.tip" :title="item.tip" placement="top">
                <span
                  aria-label="查看平均完成率计算说明"
                  class="summary-tip"
                  role="img"
                  tabindex="0"
                >
                  <IconifyIcon icon="lucide:circle-help" />
                </span>
              </Tooltip>
            </h3>
            <p>
              <strong>{{ item.value }}</strong>
              <span>{{ item.unit }}</span>
            </p>
            <small>{{ item.note }}</small>
          </div>
        </article>
      </section>

      <section class="middle-grid">
        <article class="panel ranking-panel">
          <header class="panel-title">
            <h2>班组指标排行</h2>
          </header>
          <div class="ranking-columns">
            <div class="rank-list rank-list--good">
              <h3>三查完成率TOP5</h3>
              <ol>
                <li v-for="(item, index) in threeCheckTopTeams" :key="item.name">
                  <b>{{ index + 1 }}</b>
                  <span>{{ item.name }}</span>
                  <strong>{{ item.value }}</strong>
                </li>
                <li v-if="threeCheckTopTeams.length === 0" class="empty-rank">暂无数据</li>
              </ol>
            </div>
            <div class="rank-divider"></div>
            <div class="rank-list rank-list--bad">
              <h3>未整改隐患TOP5</h3>
              <ol>
                <li v-for="(item, index) in openHazardTopTeams" :key="item.name">
                  <b>{{ index + 1 }}</b>
                  <span>{{ item.name }}</span>
                  <strong>{{ item.value }}</strong>
                </li>
                <li v-if="openHazardTopTeams.length === 0" class="empty-rank">暂无数据</li>
              </ol>
            </div>
          </div>
        </article>

        <article class="panel heatmap-panel">
          <header class="panel-title">
            <h2>异常项热力图 <span>(异常率 %)</span></h2>
            <Tooltip :title="heatmapAbnormalRateTip" placement="top">
              <span
                aria-label="查看异常率计算说明"
                class="info-tooltip"
                role="img"
                tabindex="0"
              >
                <IconifyIcon class="info-icon" icon="lucide:circle-help" />
              </span>
            </Tooltip>
            <ul class="heatmap-legend">
              <li v-for="item in heatmapLegend" :key="item.label">
                <i :class="`heat-${item.tone}`"></i>
                {{ item.label }}
              </li>
            </ul>
          </header>
          <table class="heatmap-table">
            <thead>
              <tr>
                <th>班组</th>
                <th v-for="column in heatmapColumns" :key="column">{{ column }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in heatmapRows" :key="row.name">
                <th>{{ row.name }}</th>
                <td
                  v-for="(value, index) in row.values"
                  :key="`${row.name}-${index}`"
                  :class="`heat-cell heat-${heatLevel(toMonitorCenterNumber(value))}`"
                >
                  {{ value }}%
                </td>
              </tr>
              <tr v-if="heatmapRows.length === 0">
                <td :colspan="heatmapColumns.length + 1">暂无数据</td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>

      <section class="panel detail-panel">
        <header class="panel-title detail-title">
          <h2>明细列表</h2>
          <div class="detail-actions">
            <button
              :class="activeDetailView === 'summary' ? 'primary-action' : 'plain-action'"
              type="button"
              @click="activeDetailView = 'summary'"
            >
              指标汇总
            </button>
            <button
              :class="activeDetailView === 'detail' ? 'primary-action' : 'plain-action'"
              type="button"
              @click="activeDetailView = 'detail'"
            >
              查看明细
            </button>
            <span class="detail-action-divider"></span>
            <button
              class="plain-action"
              type="button"
              @click="showTableConfig = true"
            >
              <IconifyIcon icon="lucide:settings-2" />
              表格配置
            </button>
            <button class="plain-action" type="button" @click="downloadCurrentTeamTable">
              <IconifyIcon icon="lucide:download" />
              下载
            </button>
          </div>
        </header>
        <div
          v-if="showTableConfig"
          class="table-config-mask"
          @click="showTableConfig = false"
        ></div>
        <aside v-if="showTableConfig" class="table-config-drawer">
          <header class="config-drawer-header">
            <button
              aria-label="关闭表格配置"
              class="drawer-close"
              type="button"
              @click="showTableConfig = false"
            >
              <IconifyIcon icon="lucide:x" />
            </button>
            <h3>班组分析表格配置</h3>
          </header>
          <section class="config-meta-grid">
            <label>
              <span>编码</span>
              <input readonly value="TEAM_ANALYSIS_STANDARD" />
            </label>
            <label>
              <span>名称</span>
              <input readonly value="班组分析默认视图" />
            </label>
            <label class="config-top-toggle">
              <span>置顶</span>
              <Checkbox class="config-switch" />
            </label>
            <label>
              <span>推送角色</span>
              <input disabled placeholder="请选择推送角色" />
            </label>
          </section>
          <nav class="config-tabs" aria-label="表格配置分类">
            <button class="config-tab active" type="button">字段配置</button>
            <button class="config-tab" type="button">过滤条件</button>
          </nav>
          <table class="config-table">
            <thead>
              <tr>
                <th>排序</th>
                <th>字段</th>
                <th>宽度模式</th>
                <th>排序方式</th>
                <th>启用</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in detailColumnOptions" :key="item.key">
                <td><span class="drag-handle">::</span></td>
                <td>{{ item.label }}</td>
                <td>
                  <span class="select-chip">自适应</span>
                  <span class="width-hint">{{ item.width }}px</span>
                </td>
                <td><span class="select-chip">不排序</span></td>
                <td>
                  <Checkbox
                    v-model:checked="visibleDetailColumns[item.key]"
                    class="config-switch"
                  />
                </td>
              </tr>
            </tbody>
          </table>
          <footer class="config-footer">
            <button class="plain-action muted-action" disabled type="button">推送</button>
            <button class="primary-action" type="button" @click="showTableConfig = false">
              保存
            </button>
            <button class="plain-action" type="button" @click="showTableConfig = false">
              关闭
            </button>
            <button class="plain-action danger-action" type="button">删除</button>
          </footer>
        </aside>
        <table v-if="activeDetailView === 'summary'" class="detail-table summary-table">
          <thead>
            <tr>
              <th>序号</th>
              <th>班组</th>
              <th>班组状态</th>
              <th>派班完成率</th>
              <th>三查整体</th>
              <th>隐患数</th>
              <th>未整改</th>
              <th>公司学习次数</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in pagedSummaryTableRows" :key="row.rank">
              <td>{{ row.rank }}</td>
              <td>{{ row.name }}</td>
              <td><span class="status-pill">{{ row.status }}</span></td>
              <td>{{ row.dispatchCompletionRate }}</td>
              <td>{{ row.threeCheckCompletionRate }}</td>
              <td>{{ row.hazardTotal }}</td>
              <td>{{ row.openHazards }}</td>
              <td>{{ row.studyCount }}</td>
            </tr>
            <tr v-if="summaryTableRows.length === 0">
              <td colspan="8">暂无数据</td>
            </tr>
          </tbody>
        </table>
        <table v-else class="detail-table">
          <thead>
            <tr>
              <th rowspan="2">序号</th>
              <th rowspan="2">班组</th>
              <th v-if="visibleDetailColumns.company" rowspan="2">公司</th>
              <th v-if="visibleDetailColumns.workshop" rowspan="2">车间</th>
              <th v-if="visibleDetailColumns.captain || visibleDetailColumns.safety" :colspan="Number(visibleDetailColumns.captain) + Number(visibleDetailColumns.safety)">班组信息</th>
              <th v-if="visibleDetailColumns.status" rowspan="2">班组状态</th>
              <th v-if="visibleDetailColumns.dispatch" rowspan="2">派班完成率</th>
              <th v-if="visibleDetailColumns.preMeeting || visibleDetailColumns.preCheck || visibleDetailColumns.mid || visibleDetailColumns.post || visibleDetailColumns.overall" :colspan="Number(visibleDetailColumns.preMeeting) + Number(visibleDetailColumns.preCheck) + Number(visibleDetailColumns.mid) + Number(visibleDetailColumns.post) + Number(visibleDetailColumns.overall)">三查完成率</th>
              <th v-if="visibleDetailColumns.hazardTotal || visibleDetailColumns.openHazards" :colspan="Number(visibleDetailColumns.hazardTotal) + Number(visibleDetailColumns.openHazards)">隐患整体</th>
              <th v-if="visibleDetailColumns.learning" rowspan="2">公司学习次数</th>
              <th v-if="visibleDetailColumns.action" rowspan="2">操作</th>
            </tr>
            <tr>
              <th v-if="visibleDetailColumns.captain">班班长</th>
              <th v-if="visibleDetailColumns.safety">安全员</th>
              <th v-if="visibleDetailColumns.preMeeting">班前会</th>
              <th v-if="visibleDetailColumns.preCheck">班前检查</th>
              <th v-if="visibleDetailColumns.mid">班中检查</th>
              <th v-if="visibleDetailColumns.post">班后检查</th>
              <th v-if="visibleDetailColumns.overall">整体</th>
              <th v-if="visibleDetailColumns.hazardTotal">隐患数</th>
              <th v-if="visibleDetailColumns.openHazards">未整改</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in pagedTeamDetailRows" :key="row.teamId">
              <td>{{ row.rank }}</td>
              <td>{{ row.name }}</td>
              <td v-if="visibleDetailColumns.company">{{ row.company }}</td>
              <td v-if="visibleDetailColumns.workshop">{{ row.workshop }}</td>
              <td v-if="visibleDetailColumns.captain">{{ row.captain }}</td>
              <td v-if="visibleDetailColumns.safety">{{ row.safety }}</td>
              <td v-if="visibleDetailColumns.status"><span class="status-pill">{{ row.status }}</span></td>
              <td v-if="visibleDetailColumns.dispatch">{{ row.dispatchCompletionRate }}</td>
              <td v-if="visibleDetailColumns.preMeeting">{{ row.preMeeting }}</td>
              <td v-if="visibleDetailColumns.preCheck">{{ row.preCheck }}</td>
              <td v-if="visibleDetailColumns.mid">{{ row.mid }}</td>
              <td v-if="visibleDetailColumns.post">{{ row.post }}</td>
              <td v-if="visibleDetailColumns.overall">{{ row.threeCheckCompletionRate }}</td>
              <td v-if="visibleDetailColumns.hazardTotal">{{ row.hazardTotal }}</td>
              <td v-if="visibleDetailColumns.openHazards">{{ row.openHazards }}</td>
              <td v-if="visibleDetailColumns.learning">{{ row.companyLearningCount }}</td>
              <td v-if="visibleDetailColumns.action"><a href="javascript:void(0)">查看</a></td>
            </tr>
            <tr v-if="teamDetailRows.length === 0">
              <td colspan="17">暂无数据</td>
            </tr>
          </tbody>
        </table>
        <footer class="pager-row">
          <span>共 {{ totalRows }} 条</span>
          <Select
            v-model:value="pageSize"
            :options="pageSizeOptions"
            class="pager-size-select"
            size="small"
          />
          <button
            :disabled="currentPage <= 1"
            class="page-arrow"
            type="button"
            @click="setCurrentPage(currentPage - 1)"
          >
            <IconifyIcon icon="lucide:chevron-left" />
          </button>
          <button
            v-for="page in visiblePageNumbers"
            :key="page"
            :class="{ 'page-current': page === currentPage }"
            type="button"
            @click="setCurrentPage(page)"
          >
            {{ page }}
          </button>
          <button
            :disabled="currentPage >= totalPages"
            class="page-arrow"
            type="button"
            @click="setCurrentPage(currentPage + 1)"
          >
            <IconifyIcon icon="lucide:chevron-right" />
          </button>
          <span>前往</span>
          <input
            v-model.number="pageJump"
            :max="totalPages"
            class="page-input"
            min="1"
            type="number"
            @blur="goToJumpPage"
            @keyup.enter="goToJumpPage"
          />
          <span>页</span>
        </footer>
      </section>
    </main>
  </section>
</template>

<style scoped>
.team-analysis-page {
  min-width: 1500px;
  min-height: calc(100vh - 160px);
  padding: 18px 20px;
  overflow: hidden;
  color: #263449;
  background:
    linear-gradient(180deg, rgb(247 250 255 / 92%), rgb(255 255 255 / 98%)),
    #f4f7fb;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
}

.filter-bar {
  display: grid;
  grid-template-columns: 300px 154px 130px 130px 300px 1fr auto;
  gap: 12px;
  align-items: center;
  margin-bottom: 22px;
}

.team-analysis-page :deep(.ant-input-affix-wrapper),
.team-analysis-page :deep(.ant-select-selector),
.team-analysis-page :deep(.ant-picker) {
  height: 34px;
  color: #4d596a !important;
  background: #fff !important;
  border-color: #d9e2ef !important;
  border-radius: 3px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.team-analysis-page :deep(.ant-input),
.team-analysis-page :deep(.ant-select-selection-item),
.team-analysis-page :deep(.ant-picker-input > input),
.team-analysis-page :deep(.ant-picker-separator),
.team-analysis-page :deep(.ant-picker-suffix),
.team-analysis-page :deep(.ant-select-arrow) {
  color: #4d596a !important;
}

.team-analysis-page :deep(.ant-input) {
  background: #fff !important;
}

.team-analysis-page :deep(.ant-input::placeholder) {
  color: #a4afbf !important;
}

.search-box {
  width: 300px;
}

.plain-action,
.primary-action,
.pager-row button,
.panel-title button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  gap: 6px;
  color: #516073;
  background: #fff;
  border: 1px solid #dce4ef;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
  font-size: 14px;
  white-space: nowrap;
}

.toolbar-spacer {
  min-width: 0;
}

.team-analysis-canvas {
  display: grid;
  gap: 18px;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 18px;
}

.summary-card {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  align-items: center;
  min-height: 120px;
  padding: 20px 22px;
  background: #fff;
  border: 1px solid #e2e8f2;
  border-radius: 4px;
  box-shadow: 0 7px 22px rgb(28 45 70 / 5%);
}

.summary-icon {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  color: #2d84ff;
  background: #eef5ff;
  border-radius: 12px;
  font-size: 36px;
}

.summary-card--green .summary-icon {
  color: #0fae7b;
  background: #e8f8f1;
}

.summary-card--amber .summary-icon {
  color: #f7a310;
  background: #fff4df;
}

.summary-card--red .summary-icon {
  color: #ff4640;
  background: #fff0ed;
}

.summary-copy h3 {
  display: inline-flex;
  align-items: flex-start;
  gap: 4px;
  margin: 0 0 8px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 800;
}

.summary-tip {
  position: relative;
  top: -3px;
  display: inline-flex;
  width: 14px;
  height: 14px;
  align-items: center;
  justify-content: center;
  color: #238bff;
  border-radius: 50%;
  cursor: help;
  font-size: 12px;
}

.summary-tip:focus-visible {
  outline: 2px solid #238bff;
  outline-offset: 2px;
}

.summary-copy p {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin: 0 0 5px;
}

.summary-copy strong {
  color: #1f2d44;
  font-size: 32px;
  font-weight: 500;
  line-height: 1;
}

.summary-card:last-child .summary-copy strong {
  color: #2687ff;
}

.summary-copy span,
.summary-copy small {
  color: #788598;
  font-size: 14px;
}

.summary-copy b {
  margin-left: 8px;
  color: #ff443e;
  font-weight: 500;
}

.summary-copy b.down {
  color: #10b981;
}

.middle-grid {
  display: grid;
  grid-template-columns: 38% minmax(0, 1fr);
  gap: 18px;
}

.panel {
  overflow: hidden;
  background: #fff;
  border: 1px solid #dde6f1;
  border-radius: 4px;
  box-shadow: 0 8px 22px rgb(31 45 61 / 4%);
}

.panel-title {
  display: flex;
  align-items: center;
  min-height: 50px;
  padding: 0 14px;
  border-bottom: 1px solid #e6edf5;
}

.panel-title h2 {
  position: relative;
  margin: 0;
  padding-left: 12px;
  color: #1f3659;
  font-size: 17px;
  font-weight: 900;
}

.panel-title h2::before {
  position: absolute;
  top: 2px;
  bottom: 2px;
  left: 0;
  width: 4px;
  content: "";
  background: #238bff;
}

.panel-title h2 span,
.info-icon {
  color: #8a96a8;
  font-size: 13px;
  font-weight: 600;
}

.info-tooltip {
  display: inline-flex;
  width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  color: #64748b;
  border-radius: 50%;
  cursor: help;
}

.info-tooltip:focus-visible {
  outline: 2px solid #238bff;
  outline-offset: 2px;
}

.panel-title button,
.detail-actions {
  margin-left: auto;
}

.ranking-columns {
  display: grid;
  grid-template-columns: 1fr 1px 1fr;
  gap: 24px;
  padding: 18px 24px 18px;
}

.rank-divider {
  background: #dde5ef;
}

.rank-list h3 {
  margin: 0 0 14px;
  color: #10b981;
  font-size: 15px;
  font-weight: 900;
}

.rank-list--bad h3 {
  color: #ff443e;
}

.rank-list ol {
  display: grid;
  gap: 0;
  padding: 0;
  margin: 0;
  list-style: none;
}

.rank-list li {
  display: grid;
  grid-template-columns: 26px minmax(0, 1fr) 54px;
  gap: 10px;
  align-items: center;
  min-height: 48px;
  border-bottom: 1px solid #e7edf5;
}

.rank-list b {
  color: #10b981;
  font-size: 26px;
  font-weight: 500;
}

.rank-list--bad b {
  color: #ff443e;
}

.rank-list span {
  overflow: hidden;
  color: #2f3d52;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-list strong {
  color: #10b981;
  font-size: 18px;
  text-align: right;
}

.rank-list--bad strong {
  color: #ff443e;
}

.empty-rank {
  display: flex !important;
  justify-content: center;
  color: #8a96a8;
  grid-template-columns: 1fr !important;
}

.heatmap-panel .panel-title {
  gap: 8px;
}

.heatmap-legend {
  display: flex;
  gap: 28px;
  align-items: center;
  padding: 0;
  margin: 0 0 0 auto;
  list-style: none;
  color: #6b7789;
  font-size: 14px;
}

.heatmap-legend li {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  white-space: nowrap;
}

.heatmap-legend i {
  width: 12px;
  height: 12px;
}

.heatmap-table,
.detail-table {
  width: calc(100% - 30px);
  margin: 0 15px 15px;
  border-collapse: collapse;
  table-layout: fixed;
}

.heatmap-table th,
.heatmap-table td,
.detail-table th,
.detail-table td {
  height: 40px;
  padding: 0 10px;
  color: #374155;
  text-align: center;
  border: 1px solid #e1e8f2;
  font-size: 14px;
}

.heatmap-table thead th,
.detail-table thead th {
  color: #4d596a;
  background: #f7f9fc;
  font-weight: 800;
}

.heatmap-table th:first-child {
  width: 150px;
  text-align: left;
}

.heat-normal {
  background: #bfeccb;
}

.heat-low {
  background: #d8efb7;
}

.heat-medium {
  background: #ffd979;
}

.heat-high {
  background: #ffa047;
}

.heat-danger {
  background: #ff7b68;
}

.heat-cell {
  color: #2d3b4f;
}

.detail-panel {
  min-height: 400px;
}

.detail-title {
  border-bottom: 0;
}

.detail-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.detail-action-divider {
  width: 1px;
  height: 22px;
  margin: 0 2px;
  background: #dce4ef;
}

.table-config-mask {
  position: fixed;
  inset: 0;
  z-index: 9998;
  background: rgb(15 23 42 / 20%);
}

.table-config-drawer {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 9999;
  display: flex;
  width: min(760px, 94vw);
  height: 100vh;
  flex-direction: column;
  color: #263449;
  background: #fff;
  box-shadow: -18px 0 36px rgb(22 34 51 / 16%);
}

.config-drawer-header {
  display: grid;
  grid-template-columns: 36px 1fr;
  gap: 10px;
  align-items: center;
  min-height: 58px;
  padding: 0 20px;
  border-bottom: 1px solid #e5ebf3;
}

.drawer-close {
  display: inline-grid;
  width: 32px;
  height: 32px;
  place-items: center;
  color: #5a6677;
  background: #f5f7fa;
  border: 1px solid #dde5ef;
  border-radius: 4px;
}

.config-drawer-header h3 {
  margin: 0;
  color: #1d2a3d;
  font-size: 16px;
  font-weight: 800;
}

.config-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 18px;
  padding: 18px 20px 12px;
  border-bottom: 1px solid #eef2f6;
}

.config-meta-grid label {
  display: grid;
  gap: 8px;
  color: #5c6878;
  font-size: 13px;
  font-weight: 700;
}

.config-meta-grid input {
  height: 34px;
  padding: 0 10px;
  color: #263449;
  background: #f7f9fc;
  border: 1px solid #dce4ef;
  border-radius: 4px;
  outline: none;
}

.config-meta-grid input:disabled {
  color: #98a3b3;
  background: #f3f6fa;
}

.config-top-toggle {
  align-content: start;
}

.config-tabs {
  display: flex;
  gap: 0;
  padding: 10px 20px 0;
  border-bottom: 1px solid #e7edf5;
}

.config-tab {
  position: relative;
  height: 40px;
  padding: 0 20px;
  color: #5a6677;
  background: transparent;
  border: 0;
  font-weight: 800;
}

.config-tab.active {
  color: #238bff;
}

.config-tab.active::after {
  position: absolute;
  right: 18px;
  bottom: -1px;
  left: 18px;
  height: 2px;
  background: #238bff;
  content: "";
}

.config-table {
  width: calc(100% - 40px);
  margin: 16px 20px;
  border-collapse: collapse;
  font-size: 13px;
}

.config-table th,
.config-table td {
  height: 42px;
  padding: 0 12px;
  text-align: left;
  border: 1px solid #e4eaf2;
}

.config-table th {
  color: #516073;
  background: #f7f9fc;
  font-weight: 800;
}

.config-table td {
  color: #2e3b50;
  background: #fff;
}

.drag-handle {
  color: #a4adbb;
  font-family: Consolas, monospace;
  font-weight: 900;
  letter-spacing: 0;
}

.select-chip {
  display: inline-flex;
  min-width: 74px;
  height: 28px;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px;
  color: #3f4c5f;
  background: #fff;
  border: 1px solid #dce4ef;
  border-radius: 3px;
}

.select-chip::after {
  margin-left: 10px;
  color: #94a0b1;
  content: "⌄";
}

.width-hint {
  margin-left: 8px;
  color: #8692a4;
}

.config-switch :deep(.ant-checkbox-inner) {
  width: 34px;
  height: 18px;
  background: #cfd7e3;
  border: 0;
  border-radius: 999px;
}

.config-switch :deep(.ant-checkbox-inner::after) {
  top: 50%;
  left: 4px;
  width: 10px;
  height: 10px;
  background: #fff;
  border: 0;
  border-radius: 50%;
  transform: translateY(-50%);
}

.config-switch :deep(.ant-checkbox-checked .ant-checkbox-inner) {
  background: #238bff;
}

.config-switch :deep(.ant-checkbox-checked .ant-checkbox-inner::after) {
  transform: translate(14px, -50%);
}

.config-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 14px 20px 18px;
  margin-top: auto;
  border-top: 1px solid #e7edf5;
}

.muted-action {
  color: #9aa5b4;
  background: #f4f6f9;
}

.danger-action {
  color: #d74a43;
  border-color: #f0c9c6;
}

.primary-action {
  color: #fff;
  background: #238bff;
  border-color: #238bff;
  box-shadow: 0 6px 14px rgb(35 139 255 / 20%);
}

.detail-table {
  margin-top: 0;
}

.detail-table th {
  height: 36px;
}

.detail-table td {
  height: 42px;
}

.status-pill {
  display: inline-flex;
  min-width: 58px;
  height: 24px;
  align-items: center;
  justify-content: center;
  color: #10b981;
  background: #e7f8ef;
  border-radius: 4px;
  font-weight: 800;
}

.detail-table a {
  color: #238bff;
  font-weight: 700;
}

.pager-row {
  display: flex;
  gap: 14px;
  align-items: center;
  justify-content: flex-end;
  padding: 0 28px 16px;
  color: #4c596d;
}

.pager-row button {
  min-width: 36px;
  padding: 0 12px;
  box-shadow: none;
}

.pager-row button:disabled {
  color: #a5afbd;
  cursor: not-allowed;
  background: #f4f7fb;
}

.pager-size-select {
  width: 98px;
}

.pager-row :deep(.ant-select-selector) {
  height: 34px !important;
  border-color: #dce4ef !important;
  border-radius: 4px !important;
}

.pager-row .page-current {
  color: #238bff;
  background: #eef6ff;
  border-color: #9dccff;
}

.pager-row .page-input {
  width: 52px;
  height: 34px;
  padding: 0 8px;
  color: #516073;
  text-align: center;
  background: #fff;
  border: 1px solid #dce4ef;
  border-radius: 4px;
  outline: none;
}

.pager-row .page-input:focus {
  border-color: #238bff;
  box-shadow: 0 0 0 2px rgb(35 139 255 / 12%);
}
</style>
