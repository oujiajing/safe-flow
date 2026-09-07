import { describe, expect, it } from 'vitest';

import accidentSource from './accident-analysis.vue?raw';
import analysisSource from './data-analysis.vue?raw';
import echartsPluginSource from '../../../../../../packages/effects/plugins/src/echarts/echarts.ts?raw';
import source from './index.vue?raw';
import screenSource from './screen.vue?raw';
import teamSource from './team-analysis.vue?raw';

describe('monitor center page', () => {
  it('keeps the monitor center module tabs and defaults to the blue screen', () => {
    expect(source).toContain("const activeModule = ref<MonitorModule>('screen')");
    expect(source).toContain('ScreenDashboard');
    expect(source).toContain('DataAnalysis');
    expect(source).toContain('AccidentAnalysis');
    expect(source).toContain('TeamAnalysis');
    for (const text of [
      '平安班组大屏',
      '平安班组数据分析中心',
      '事故看板',
      '班组分析',
    ]) {
      expect(source).toContain(text);
    }
  });

  it('keeps the original blue large-screen dashboard as its own module', () => {
    for (const text of [
      '广晟矿业平安班组监控中心',
      '平安大视界',
      '风险分级管控',
      '一班三查',
      '隐患排查',
      'AI监控',
      '今日告警',
      'monitor-screen',
      'screen-grid',
      'left-column',
      'center-column',
      'right-column',
    ]) {
      expect(screenSource).toContain(text);
    }
  });

  it('renders the approved large-screen visual treatment', () => {
    for (const text of [
      '广晟幕墙',
      '资源公司',
      '广晟新材',
      '广晟源成',
      '重大风险: \'#ff3b30\'',
      '较大风险: \'#ff9f0a\'',
      '一般风险: \'#ffd60a\'',
      '低风险: \'#2f80ed\'',
      '隐患总数',
      'metric-strip',
      'companyCategoryAxis',
      'sortCompanyRows',
    ]) {
      expect(screenSource).toContain(text);
    }
    expect(screenSource).toContain("xAxis: valueAxis");
    expect(screenSource).toContain("yAxis: companyCategoryAxis");
    expect(screenSource).toContain('已整改 绿色');
    expect(screenSource).toContain('未整改 橙色');
    expect(screenSource).not.toContain('最初的梦想');
  });

  it('uses larger chart labels with softer company-name color', () => {
    for (const text of [
      "const companyLabelColor = '#f2f8ff'",
      "const legendTextColor = '#dff3ff'",
      'fontSize: 13',
      'fontWeight: 700',
      'textStyle: { color: legendTextColor, fontSize: 12, fontWeight: 700 }',
    ]) {
      expect(screenSource).toContain(text);
    }
    expect(screenSource).not.toContain("textStyle: { color: '#cceaff', fontSize: 10 }");
    expect(screenSource).not.toContain("textStyle: { color: '#b9dfff', fontSize: 10 }");
  });

  it('keeps the screen browser-comment refinements visible in source', () => {
    for (const text of [
      '班前会%',
      '班前检查',
      '班中检查%',
      '班后检查%',
      'vision-video-placeholder',
      '暂无宣传视频',
      'hazard-bar-chart--raised',
    ]) {
      expect(screenSource).toContain(text);
    }
    expect(screenSource).not.toContain('alt="平安大视界视频封面"');
    expect(screenSource).not.toContain('班中 <b>{{ summary.threeCheckRates.mid }}</b>');
  });

  it('renders the AI monitor as the approved alert card', () => {
    for (const text of [
      '/pingan-monitor/ai-monitor-drone.png',
      '点击放大',
      'AI智能报警',
      '智能提醒，稳定可靠，为安全多添一份保障。',
      'ai-alert-card',
    ]) {
      expect(screenSource).toContain(text);
    }
    expect(screenSource).not.toContain('/pingan-monitor/ai-monitor-panel.svg');
    expect(screenSource).not.toContain('camera-strip');
    expect(screenSource).not.toContain('未佩戴安全帽');
    expect(screenSource).not.toContain('人员聚集');
  });

  it('adds chart and detail switches to business chart panels', () => {
    for (const text of [
      "type PanelView = 'chart' | 'detail'",
      'panelViews',
      'setPanelView',
      '作业派班视图切换',
      '一班三查视图切换',
      '风险分级管控视图切换',
      '隐患排查视图切换',
      'panel-view-toggle',
      'screen-detail-table-wrap',
      'screen-detail-table',
      '<th>公司</th>',
      '<th>派班完成数</th>',
      '<th>班前会</th>',
      '<th>重大风险</th>',
      '<th>隐患总数</th>',
    ]) {
      expect(screenSource).toContain(text);
    }
    expect(screenSource).not.toContain('AI监控视图切换');
    expect(screenSource).not.toContain('平安大视界视图切换');
    expect(screenSource).not.toContain("panelViews.vision === 'detail'");
  });

  it('renders the vision panel from managed content videos with the approved safety copy only', () => {
    for (const text of [
      'getScreenContentProfileVideosApi',
      'loadScreenVideos',
      'screenVideos',
      'activeVisionVideo',
      'handleVisionVideoEnded',
      'vision-safety-copy',
      '安全，是一切工作的基石，是每个人最基本的保障。',
      '无论是工作还是生活，都要时刻牢记，生命是无价的，它承载着责任、家庭和未来。',
      '因此，安全生产、生活中的每一个细节都不容忽视。严格遵守各项安全制度，确保工作环境的安全无隐患',
    ]) {
      expect(screenSource).toContain(text);
    }
    expect(screenSource).not.toContain('已连续安全生产 128 天');
    expect(screenSource).not.toContain('未整改隐患 {{ summary.hazardOpen }}');
    expect(screenSource).not.toContain('重大风险 {{ riskTotals.major }}');
    expect(screenSource).not.toContain('visionVideoInputRef');
    expect(screenSource).not.toContain('chooseVisionVideo');
    expect(screenSource).not.toContain('handleVisionVideoChange');
    expect(screenSource).not.toContain('accept="video/*"');
    expect(screenSource).not.toContain('上传视频');
    expect(screenSource).toMatch(
      /\.vision-copy\s*{[^}]*box-sizing: border-box;[^}]*margin: 0 10px;/s,
    );
  });

  it('filters the large-screen dashboard by date range and approved companies', () => {
    for (const text of [
      'DatePicker.RangePicker',
      'selectedDateRange',
      'selectedCompanyId',
      'screenCompanyOptions',
      '全部公司',
      '广晟幕墙',
      '广晟源成',
      '资源公司',
      '广晟新材',
      'dateStart',
      'dateEnd',
      "selectedCompanyId.value === 'all-company'",
      "selectedCompanyId.value === 'all-company' ? undefined : selectedCompanyId.value",
      'handleScreenFilterChange',
    ]) {
      expect(screenSource).toContain(text);
    }
    expect(screenSource.indexOf('广晟幕墙')).toBeLessThan(
      screenSource.indexOf('广晟源成'),
    );
    expect(screenSource.indexOf('广晟源成')).toBeLessThan(
      screenSource.indexOf('资源公司'),
    );
    expect(screenSource.indexOf('资源公司')).toBeLessThan(
      screenSource.indexOf('广晟新材'),
    );
  });

  it('hides the duplicated scope name and clock from the left screen filters', () => {
    expect(screenSource).toContain('screen-company-select');
    expect(screenSource).toContain('screen-date-picker');
    expect(screenSource).toContain('<span class="date-range">{{ currentDateTime }}</span>');
    expect(screenSource).not.toContain(
      '<span class="scope-name">{{ selectedScopeName }}</span>',
    );
    expect(screenSource).not.toContain('class="scope-name"');
  });

  it('renders the data analysis center dashboard from the reference screen', () => {
    for (const text of [
      '选择公司',
      '选择班组',
      '图表视图',
      '明细视图',
      '班前未开会',
      '班前未检查',
      '班中未检查',
      '班后未检查',
      '上报隐患数',
      '未解决隐患',
      '重大风险',
      '安全学习',
      '作业派班',
      '隐患整改',
      '一班三查',
      '学习次数',
    ]) {
      expect(analysisSource).toContain(text);
    }
  });

  it('places the data-analysis filter toolbar on the left', () => {
    expect(analysisSource).toMatch(
      /\.monitor-toolbar\s*{[^}]*justify-content: start;/s,
    );
    expect(analysisSource).not.toContain('justify-content: end;');
  });

  it('keeps the aggregate API as the page data source', () => {
    for (const moduleSource of [
      accidentSource,
      analysisSource,
      screenSource,
      teamSource,
    ]) {
      expect(moduleSource).toContain('getMonitorCenterOverviewApi');
      expect(moduleSource).toContain('loadMonitorOverview');
      expect(moduleSource).toContain('overview.value');
      expect(moduleSource).toContain('scopeOptions');
    }
    expect(analysisSource).toContain(
      'orgId: selectedTeam.value ?? selectedCompany.value',
    );
    expect(screenSource).toContain('selectedCompanyId.value');
    expect(accidentSource).toContain('overviewLoadError');
    expect(teamSource).toContain('loadError');
  });

  it('loads team analysis from the real aggregate API on the seeded validation date', () => {
    expect(teamSource).toContain('getMonitorCenterOverviewApi');
    expect(teamSource).toContain('teamAnalysis.value.summary.teamCount');
    expect(teamSource).toContain("['2026-06-01', '2026-06-01']");
    expect(teamSource).not.toContain("['2026-05-29', '2026-05-29']");
  });

  it('uses chart widgets for the dashboard panels', () => {
    for (const text of [
      'EchartsUI',
      'dispatchChartRef',
      'hazardFunnelChartRef',
      'closureChartRef',
      'hazardDonutChartRef',
      'threeChecksChartRef',
      'learningTrendChartRef',
      'metricCards',
      'rankRows',
    ]) {
      expect(analysisSource).toContain(text);
    }
  });

  it('registers chart types used by the monitor center', () => {
    expect(analysisSource).toContain("type: 'funnel'");
    expect(echartsPluginSource).toContain('FunnelChart');
    expect(echartsPluginSource).toContain('FunnelChart,');
  });

  it('switches the data-analysis panels to detail tables', () => {
    for (const text of [
      "type AnalysisPanelKey = 'dispatch' | 'hazard' | 'learning' | 'threeCheck'",
      'analysisPanelViews',
      'setAnalysisPanelView',
      "v-if=\"analysisPanelViews.dispatch === 'chart'\"",
      "v-if=\"analysisPanelViews.hazard === 'chart'\"",
      "v-if=\"analysisPanelViews.threeCheck === 'chart'\"",
      "v-if=\"analysisPanelViews.learning === 'chart'\"",
      'dispatchDetailRows',
      'hazardDetailRows',
      'threeCheckDetailRows',
      'learningTrendRows',
      'detail-table-wrap',
      'detail-table--compact',
      '公司/班组',
      '闭环率',
      '未完成',
      '学习次数',
    ]) {
      expect(analysisSource).toContain(text);
    }
    expect(analysisSource).not.toContain('const activeView = ref');
    expect(analysisSource).not.toContain('v-model:value="activeView"');
  });

  it('uses backend learning aggregates instead of fixed learning samples', () => {
    expect(analysisSource).toContain('summary.value.learningToday');
    expect(analysisSource).toContain('summary.value.learningTotal');
    expect(analysisSource).toContain('overview.value.learningTrend');
    expect(analysisSource).not.toContain("value: '10'");
    expect(analysisSource).not.toContain("value: '124'");
    expect(analysisSource).not.toContain("{ date: '05-24'");
  });

  it('keeps the data-analysis dispatch chart filling its card', () => {
    expect(analysisSource).toContain('height: 100% !important');
    expect(analysisSource).toContain('flex: 1');
    expect(analysisSource).toContain('min-height: 400px');
    expect(analysisSource).toContain('align-items: stretch');
    expect(analysisSource).toContain('align-self: stretch');
  });

  it('anchors the data-analysis checks and learning stats to the card bottom', () => {
    expect(analysisSource).toMatch(
      /\.panel--checks,\s*\.panel--learning\s*{[^}]*display: grid;[^}]*grid-template-rows: auto minmax\(0, 1fr\) auto;/s,
    );
    expect(analysisSource).toMatch(
      /\.checks-chart\s*{[^}]*height: 100% !important;[^}]*min-height: 0;/s,
    );
    expect(analysisSource).toMatch(
      /\.learning-layout\s*{[^}]*height: 100%;[^}]*min-height: 0;/s,
    );
    expect(analysisSource).not.toContain('height: 148px !important');
    expect(analysisSource).not.toContain('height: 112px !important');
  });

  it('aligns the data-analysis hazard card and summary emphasis', () => {
    expect(analysisSource).toMatch(
      /\.panel--hazard\s*{[^}]*display: grid;[^}]*grid-template-rows: auto minmax\(0, 1fr\) auto;/s,
    );
    expect(analysisSource).toMatch(
      /\.hazard-layout\s*{[^}]*grid-template-columns: minmax\(124px, 0\.72fr\) 150px minmax\(256px, 1\.28fr\);[^}]*align-items: stretch;/s,
    );
    expect(analysisSource).toMatch(
      /\.hazard-layout\s*{[^}]*grid-template-rows: minmax\(0, 1fr\) 30px;/s,
    );
    expect(analysisSource).toMatch(
      /\.hazard-footer\s*{[^}]*grid-column: 2 \/ 4;[^}]*margin: 0;/s,
    );
    expect(analysisSource).toMatch(
      /@media \(max-width: 1400px\)\s*{[^}]*\.hazard-footer\s*{[^}]*grid-column: 1;/s,
    );
    expect(analysisSource).toMatch(
      /\.hazard-state-card\s*{[^}]*padding: 16px 12px 10px;/s,
    );
    expect(analysisSource).toMatch(
      /\.hazard-donut-wrap\s*{[^}]*grid-template-columns: 92px minmax\(0, 1fr\);[^}]*gap: 10px;/s,
    );
    expect(analysisSource).toContain('closure-label');
    expect(analysisSource).toMatch(
      /\.summary-up\s*{[^}]*font-size: 28px !important;/s,
    );
  });

  it('fits the data-analysis dashboard into one viewport without page scrolling', () => {
    expect(analysisSource).toContain('height: calc(100vh - 136px)');
    expect(analysisSource).toContain('height: calc(100vh - 350px)');
    expect(analysisSource).toContain('grid-template-rows: 492px minmax(260px, 1fr)');
    expect(analysisSource).toContain('min-height: 285px');
    expect(analysisSource).toContain('height: 100% !important');
    expect(analysisSource).toContain('@media (max-height: 1100px)');
    expect(analysisSource).toContain('grid-template-rows: 430px minmax(0, 1fr)');
    expect(analysisSource).toContain('height: calc(100vh - 320px)');
    expect(analysisSource).toContain('min-height: 360px');
    expect(analysisSource).toContain('.learning-stats strong');
  });

  it('renders the accident analysis dashboard as its own monitor module', () => {
    expect(source).toContain("activeModule === 'accident'");
    for (const text of [
      '风险隐患事故预警看板',
      '可能事故类型雷达图',
      '隐患整改记录（非事故记录）',
      '所属企业风险等级统计表（来自风险管控，非事故伤害）',
      '真实事故趋势（待事故专用接口）',
      '统计情况',
      '可能事故类型统计',
      '风险原因及隐患整改情况',
      '暂无真实事故数据源',
      '待事故专用接口',
      '整改闭环',
    ]) {
      expect(accidentSource).toContain(text);
    }
    expect(accidentSource).toContain('radarChartRef');
    expect(accidentSource).toContain('trendChartRef');
    expect(accidentSource).toContain('injuryTypeChartRef');
    expect(accidentSource).toContain('rectificationChartRef');
  });

  it('labels accident dashboard real module data and unavailable accident facts', () => {
    for (const text of [
      '风险隐患事故预警看板',
      '可能事故类型雷达图',
      '隐患整改记录（非事故记录）',
      '暂无真实事故数据源',
      '待事故专用接口',
      'riskAccidentTypeBars',
      'hazardRecordRows',
      'specialWorkTypeBars',
    ]) {
      expect(accidentSource).toContain(text);
    }
    expect(accidentSource).not.toContain('轻伤TRIFR');
    expect(accidentSource).not.toContain('重伤TRIFR');
    expect(accidentSource).not.toContain('已展示样例视图');
  });

  it('keeps the accident dashboard dense without large blank lower gaps', () => {
    expect(accidentSource).toContain(
      'grid-template-rows: minmax(0, 30fr) minmax(0, 24fr) minmax(0, 46fr)',
    );
    expect(accidentSource).toContain('height: calc(100vh - 212px)');
    expect(accidentSource).toContain(
      'grid-template-rows: repeat(5, minmax(86px, 1fr))',
    );
    expect(accidentSource).toContain('grid-auto-rows: minmax(74px, 1fr)');
    expect(accidentSource).toContain('min-height: 86px');
    expect(accidentSource).toContain('min-height: 88px');
    expect(accidentSource).toContain(
      'grid-template-columns: 150px 150px minmax(140px, 1fr)',
    );
    expect(accidentSource).toContain('min-height: 170px');
    expect(accidentSource).toContain(
      'grid: { bottom: 12, containLabel: true, left: 12, right: 18, top: 26 }',
    );
    expect(accidentSource).toContain(
      'height: min(400px, calc(100% - 42px)) !important',
    );
    expect(accidentSource).toContain('height: calc(100% - 38px) !important');
  });

  it('renders the team analysis dashboard from the reference screen', () => {
    expect(source).toContain("activeModule === 'team'");
    for (const text of [
      '使用公司/班组 点击选择或回车',
      '班组总数',
      '正常班组',
      '异常班组',
      '未完成三查',
      '未整改隐患',
      '平均完成率',
      '班组指标排行',
      '三查完成率TOP5',
      '未整改隐患TOP5',
      '异常项热力图',
      '明细列表',
      '指标汇总',
      '查看明细',
      '表格配置',
      '下载',
      '三查完成率',
      '隐患整体',
      '公司学习次数',
    ]) {
      expect(teamSource).toContain(text);
    }
    for (const removedText of [
      '清除排序',
      '打印模式',
      '数据管理',
      '卡片管理',
      '标准视图',
      'viewOptions',
      'printMode',
      'Switch',
      'refresh-only',
      '更多 <IconifyIcon icon="lucide:chevron-right" />',
    ]) {
      expect(teamSource).not.toContain(removedText);
    }
    expect(teamSource).toContain('team-analysis-page');
    expect(teamSource).toContain('summaryCards');
    expect(teamSource).toContain('teamAnalysis');
    expect(teamSource).not.toContain('excellentTeams');
    expect(teamSource).not.toContain('laggingTeams');
    expect(teamSource).not.toContain('health');
    expect(teamSource).not.toContain('健康度');
    expect(teamSource).not.toContain('已展示样例视图');
    expect(teamSource).not.toContain('幕墙组装1班');
    expect(teamSource).not.toContain('玻璃铝板外案件班');
  });

  it('implements team-analysis table view switching, configuration, and download', () => {
    for (const text of [
      "type DetailTableView = 'detail' | 'summary'",
      "const activeDetailView = ref<DetailTableView>('summary')",
      'showTableConfig',
      'visibleDetailColumns',
      'summaryTableRows',
      'currentPageRows',
      'currentPage',
      'pageSize',
      'totalPages',
      'visiblePageNumbers',
      'setCurrentPage',
      'downloadCurrentTeamTable',
      'downloadCsv',
      "@click=\"activeDetailView = 'summary'\"",
      "@click=\"activeDetailView = 'detail'\"",
      '@click="showTableConfig = true"',
      '@click="downloadCurrentTeamTable"',
      'v-if="activeDetailView === \'summary\'"',
      'v-else class="detail-table"',
      'table-config-drawer',
      'table-config-mask',
      '班组分析表格配置',
      '字段配置',
      '过滤条件',
      '宽度模式',
      '排序方式',
      '启用',
      '保存',
      '关闭',
      'v-model:checked="visibleDetailColumns',
    ]) {
      expect(teamSource).toContain(text);
    }
    expect(teamSource).not.toContain('table-config-panel');
    expect(teamSource).not.toContain('<button type="button">2</button>');
    expect(teamSource).not.toContain('<button type="button">3</button>');
    expect(teamSource).not.toContain('<button type="button">4</button>');
    expect(teamSource).not.toContain('<button class="page-input" type="button">1</button>');
    expect(teamSource).not.toContain('排名');
    expect(teamSource).toContain('序号');
  });

  it('explains the heatmap abnormal rate on hover', () => {
    expect(teamSource).toContain('Tooltip');
    expect(teamSource).toContain('heatmapAbnormalRateTip');
    expect(teamSource).toContain('异常率 = 异常项数量 / 应检查或应完成项数量 × 100%');
    expect(teamSource).toContain('aria-label="查看异常率计算说明"');
  });

  it('explains the average completion rate on hover', () => {
    expect(teamSource).toContain('averageCompletionRateTip');
    expect(teamSource).toContain('平均完成率 = 已完成单元数 / 应完成单元数 × 100%');
    expect(teamSource).toContain('已完成单元数包括已完成派班、班前会、班前检查、班中检查、班后检查');
    expect(teamSource).toContain('aria-label="查看平均完成率计算说明"');
    expect(teamSource).toContain('summary-tip');
  });
});
