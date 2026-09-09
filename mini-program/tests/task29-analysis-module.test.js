const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")

const { createDefaultContext, normalizeContext, rangeText, toQuery } = require("../utils/analysis-context")
const { buildAnalysisModel } = require("../utils/analysis-model")
const { buildScopeState, selectionContext } = require("../utils/analysis-scope")
const { compactLabelIndexes } = require("../utils/analysis-charts")

function readMini(file) {
  return fs.readFileSync(path.join(__dirname, "..", file), "utf8")
}

test("monitor center service reuses the PC aggregate endpoint and removes empty query values", async () => {
  const requestPath = require.resolve("../utils/request")
  const servicePath = require.resolve("../services/monitor-center")
  const calls = []
  require.cache[requestPath] = {
    id: requestPath,
    filename: requestPath,
    loaded: true,
    exports: { request(options) { calls.push(options); return Promise.resolve({}) } }
  }
  delete require.cache[servicePath]
  const service = require("../services/monitor-center")
  await service.getMonitorCenterOverview({ dateStart: "2026-07-16", dateEnd: "2026-07-22", orgId: "" })
  assert.deepEqual(calls, [{
    url: "/api/pingan/monitor-center/overview",
    method: "GET",
    data: { dateStart: "2026-07-16", dateEnd: "2026-07-22" }
  }])
})

test("analysis context defaults to seven days and normalizes reversed dates", () => {
  const context = createDefaultContext(new Date(2026, 6, 22))
  assert.deepEqual(context, {
    orgId: "",
    orgName: "全部可见组织",
    rangeLabel: "近7天",
    dateStart: "2026-07-16",
    dateEnd: "2026-07-22"
  })
  const normalized = normalizeContext({ dateStart: "2026-07-22", dateEnd: "2026-07-16", rangeLabel: "自定义" }, new Date(2026, 6, 22))
  assert.equal(normalized.dateStart, "2026-07-16")
  assert.equal(normalized.dateEnd, "2026-07-22")
  assert.deepEqual(toQuery(context), { dateStart: "2026-07-16", dateEnd: "2026-07-22" })
  assert.equal(rangeText(context), "近7天  07-16 至 07-22")
})

test("analysis scope separates company department and team options by account boundary", () => {
  const tree = [{
    id: 10,
    name: "一公司",
    orgType: "COMPANY",
    children: [{
      id: 20,
      name: "生产部",
      orgType: "DEPARTMENT",
      children: [
        { id: 30, name: "生产一班", orgType: "TEAM", children: [] },
        { id: 31, name: "生产二班", orgType: "TEAM", children: [] }
      ]
    }]
  }, {
    id: 11,
    name: "二公司",
    orgType: "COMPANY",
    children: []
  }]

  const globalState = buildScopeState(tree, { orgId: "", orgName: "全部可见组织" }, { global: true })
  assert.equal(globalState.showCompany, true)
  assert.equal(globalState.showDepartment, true)
  assert.equal(globalState.showTeam, true)
  assert.deepEqual(globalState.companyOptions.map(item => item.name), ["全部公司", "一公司", "二公司"])
  assert.deepEqual(globalState.departmentOptions.map(item => item.name), ["全部部门", "生产部"])
  assert.deepEqual(globalState.teamOptions.map(item => item.name), ["全部班组", "生产一班", "生产二班"])

  const companyState = buildScopeState(tree, { orgId: 10, orgName: "一公司" }, { global: false, orgId: 10 })
  assert.equal(companyState.showCompany, false)
  assert.equal(companyState.showDepartment, true)
  assert.equal(companyState.showTeam, true)
  assert.deepEqual(companyState.departmentOptions.map(item => item.name), ["全部部门", "生产部"])
  assert.deepEqual(selectionContext(tree, companyState, "department", 1), { orgId: 20, orgName: "生产部" })

  const departmentState = buildScopeState(tree, { orgId: 20, orgName: "生产部" }, { global: false, orgId: 20 })
  assert.equal(departmentState.showCompany, false)
  assert.equal(departmentState.showDepartment, false)
  assert.equal(departmentState.showTeam, true)
  assert.deepEqual(departmentState.teamOptions.map(item => item.name), ["全部班组", "生产一班", "生产二班"])
  assert.deepEqual(selectionContext(tree, departmentState, "team", 2), { orgId: 31, orgName: "生产二班" })

  const teamState = buildScopeState(tree, { orgId: 30, orgName: "生产一班" }, { global: false, orgId: 30 })
  assert.equal(teamState.showCompany, false)
  assert.equal(teamState.showDepartment, false)
  assert.equal(teamState.showTeam, false)
  assert.equal(teamState.fixedOrgName, "生产一班")
})

test("analysis model transforms shared backend facts for all five pages", () => {
  const model = buildAnalysisModel({
    summary: {
      dispatchTotal: 6, dispatchFinished: 4, hazardTotal: 5, hazardDone: 3, hazardOpen: 2,
      learningTotal: 8, learningToday: 2, learningCompletedCount: 6,
      learningExpectedCount: 12, learningCompletionRate: "50.00%", teamCount: 2, personCount: 12,
      threeCheckRates: { pre: "75.00%", preInspection: "75.00%", mid: "50.00%", post: "25.00%" }
    },
    scopeOptions: [{ id: 10, name: "一公司", orgType: "COMPANY", children: [] }],
    dispatchBars: [{ name: "一公司", value: 6 }],
    dispatchScopeBars: [{ name: "一班", value: 4 }, { name: "二班", value: 2 }],
    dispatchFinishedBars: [{ name: "一公司", value: 4 }],
    dispatchTrend: [{ name: "2026-07-22", total: 6, finished: 4 }],
    threeCheckSeries: [
      { name: "班前会", data: [3] }, { name: "班前检查", data: [3] },
      { name: "班中检查", data: [2] }, { name: "班后检查", data: [1] }
    ],
    hazardRectificationOrders: { total: 6, closed: 3, activeOpen: 2, cancelled: 1 },
    hazardStatusBars: [{ name: "已闭环", value: 3 }, { name: "待整改", value: 2 }, { name: "已作废", value: 1 }],
    hazardBars: [{ name: "一公司", done: 3, open: 2 }],
    riskControlBars: [{ name: "一公司", major: 2, serious: 0, normal: 1, low: 0 }],
    learningTrend: [{ name: "2026-07-22", value: 2 }],
    learningBars: [{ name: "一公司", value: 8 }],
    learningCategoryBars: [{ name: "安全学习", value: 8 }],
    teamAnalysis: {
      summary: { unfinishedThreeChecks: 5 },
      detailRows: [{ teamId: 1, name: "一班", preCheck: "75.00%", mid: "50.00%", post: "25.00%", unfinishedThreeChecks: 5 }]
    }
  })
  assert.equal(model.kpis.find(item => item.key === "meeting").value, 1)
  assert.equal(model.kpis.find(item => item.key === "majorRisk").value, 2)
  assert.deepEqual(model.dispatch.trend[0], { date: "07-22", total: 6, finished: 4, pending: 2, height: "100.00%" })
  assert.deepEqual(model.dispatch.scopeRows, [
    { name: "一班", value: 4, width: "82.00%" },
    { name: "二班", value: 2, width: "41.00%" }
  ])
  assert.deepEqual(model.overviewKpis.map(item => item.label), ["班组数", "人员数", "检查次数", "隐患数"])
  assert.equal(model.overviewKpis.find(item => item.key === "person").value, 12)
  assert.equal(model.threeCheck.overallRateValue, 50)
  assert.deepEqual(model.threeCheck.completionRates.map(item => item.label), ["班前会", "班前检查", "班中检查", "班后检查"])
  assert.equal(model.threeCheck.overviewRate, "50%")
  assert.equal(model.threeCheck.overviewRemainingRate, "50%")
  assert.equal(model.learning.overviewCompletionRate, "50%")
  assert.equal(model.learning.completionWidth, "50%")
  assert.equal(model.hazard.statusRows[0].rate, "50.00%")
  assert.deepEqual(model.hazard.closureRows.map(item => [item.name, item.value, item.rate, item.width]), [
    ["已闭环", 3, "50.00%", "100.00%"],
    ["未闭环", 2, "33.33%", "66.67%"],
    ["已作废", 1, "16.67%", "33.33%"]
  ])
  assert.deepEqual(model.hazard.rectificationStages, [
    { key: "total", label: "隐患总数", value: 5 },
    { key: "pending", label: "待整改", value: 2 },
    { key: "rectifying", label: "整改中", value: 0 },
    { key: "rectified", label: "已整改", value: 3 }
  ])
  assert.equal(model.threeCheck.teamRows[0].unfinished, 5)
  assert.equal(model.learning.categories[0].name, "安全学习")
  assert.equal(model.learning.categories[0].icon, "secured-filled")
  assert.equal(model.learning.categories[0].width, "100%")
  assert.equal(model.learning.rankings[0].rankColor, "#e4aa00")
  assert.equal(model.learning.rankings[0].rankTone, "rank-gold")
  assert.equal(model.learning.rankings[0].width, "100%")
})

test("compact analysis charts keep only readable date anchors", () => {
  assert.deepEqual(compactLabelIndexes(7), [0, 3, 6])
  assert.deepEqual(compactLabelIndexes(3), [0, 1, 2])
  assert.deepEqual(compactLabelIndexes(1), [0])
})

test("overview hazard funnel groups workflow statuses without counting cancelled orders", () => {
  const model = buildAnalysisModel({
    summary: {
      hazardTotal: 10,
      hazardDone: 4,
      hazardOpen: 6,
      threeCheckRates: {}
    },
    dispatchBars: [],
    hazardRectificationOrders: { total: 12, closed: 4, activeOpen: 6, cancelled: 2 },
    hazardStatusBars: [
      { name: "待派发", value: 1 },
      { name: "待整改", value: 2 },
      { name: "已整改", value: 1 },
      { name: "待验收", value: 2 },
      { name: "已闭环", value: 4 },
      { name: "已作废", value: 2 }
    ]
  })

  assert.deepEqual(model.hazard.rectificationStages.map(item => item.value), [10, 3, 3, 4])
})

test("overview completion percentages round to whole numbers", () => {
  const model = buildAnalysisModel({
    summary: {
      dispatchFinished: 1,
      learningCompletedCount: 2,
      learningExpectedCount: 3,
      learningCompletionRate: "66.67%",
      threeCheckRates: {}
    },
    dispatchBars: [],
    threeCheckSeries: [
      { name: "班前检查", data: [1] },
      { name: "班中检查", data: [1] },
      { name: "班后检查", data: [0] }
    ]
  })

  assert.equal(model.threeCheck.overviewRate, "67%")
  assert.equal(model.threeCheck.overviewRemainingRate, "33%")
  assert.equal(model.learning.overviewCompletionRate, "67%")
})

test("company scoped dispatch chart does not fall back to a company bar when no department rows exist", () => {
  const model = buildAnalysisModel({
    summary: { threeCheckRates: {} },
    dispatchBars: [{ name: "一公司", value: 3 }],
    dispatchScopeBars: []
  })

  assert.deepEqual(model.dispatch.scopeRows, [])
})

test("analysis module registers overview and four native subpages with shared filters", () => {
  const app = JSON.parse(readMini("app.json"))
  const { registeredPages } = require("./helpers/app-routes")
  const scopeFilterMarkup = readMini("components/analysis/scope-filter/index.wxml")
  const pages = [
    "pages/analysis/analysis",
    "pages/analysis/dispatch/index",
    "pages/analysis/hazard/index",
    "pages/analysis/three-check/index",
    "pages/analysis/learning/index"
  ]
  for (const page of pages) {
    assert.ok(registeredPages(app).includes(page), page)
    const json = JSON.parse(readMini(`${page}.json`))
    const wxml = readMini(`${page}.wxml`)
    assert.equal(json.navigationStyle, "custom")
    assert.match(wxml, /<scope-filter/)
    assert.match(wxml, /compact="\{\{true\}\}"/)
    assert.doesNotMatch(wxml, /学习次数|示例数据|mock/i)
  }
  assert.match(readMini("pages/analysis/analysis.wxml"), /data-type="dispatch"/)
  assert.match(readMini("pages/analysis/analysis.wxml"), /tabBarInset="\{\{true\}\}"/)
  assert.match(readMini("pages/analysis/analysis.wxml"), /data-type="hazard"/)
  assert.match(readMini("pages/analysis/analysis.wxml"), /data-type="threeCheck"/)
  assert.match(readMini("pages/analysis/analysis.wxml"), /data-type="learning"/)
  assert.match(scopeFilterMarkup, /src="\/assets\/页面图标\/公司\.svg"/)
  assert.match(scopeFilterMarkup, /data-level="company"/)
  assert.match(scopeFilterMarkup, /data-level="department"/)
  assert.match(scopeFilterMarkup, /data-level="team"/)
  assert.match(scopeFilterMarkup, /class="filter-apply" bindtap="apply">应用<\/button>/)
  assert.doesNotMatch(scopeFilterMarkup, /应用筛选/)
  assert.doesNotMatch(scopeFilterMarkup, />组织范围<\/text>\s*<picker/)
  assert.doesNotMatch(scopeFilterMarkup, /name="building"/)
  const scopeStyles = readMini("components/analysis/scope-filter/index.wxss")
  assert.match(scopeStyles, /\.filter-mask\{position:fixed;z-index:1100/)
  assert.match(scopeStyles, /\.filter-drawer\{position:fixed;z-index:1101/)
  assert.match(scopeStyles, /\.filter-drawer--tabbar\{bottom:104rpx/)
  assert.match(scopeStyles, /\.filter-actions button\{flex:1;/)
})

test("analysis overview and business pages preserve the reference vertical rhythm", () => {
  const sharedStyles = readMini("styles/analysis.wxss")
  const overviewMarkup = readMini("pages/analysis/analysis.wxml")
  const overviewStyles = readMini("pages/analysis/analysis.wxss")
  const dispatchMarkup = readMini("pages/analysis/dispatch/index.wxml")
  const threeCheckMarkup = readMini("pages/analysis/three-check/index.wxml")
  const hazardMarkup = readMini("pages/analysis/hazard/index.wxml")
  const learningMarkup = readMini("pages/analysis/learning/index.wxml")
  const learningStyles = readMini("pages/analysis/learning/index.wxss")

  assert.match(sharedStyles, /height:\s*var\(--analysis-header-height\)/)
  assert.match(sharedStyles, /margin-top:\s*28rpx/)
  assert.match(sharedStyles, /--analysis-header-height:\s*164rpx/)
  assert.match(sharedStyles, /grid-template-columns:\s*repeat\(4/)
  assert.match(overviewMarkup, /overview-kpi-grid/)
  assert.match(overviewMarkup, /<view class="overview-title">班组派班<\/view>/)
  assert.match(overviewMarkup, /<view class="overview-title">一班三查<\/view>/)
  assert.doesNotMatch(overviewMarkup, />派班分析<|>三查完成率</)
  assert.equal((overviewMarkup.match(/overview-summary-card/g) || []).length, 4)
  assert.match(overviewMarkup, /compact="\{\{true\}\}"/)
  assert.match(overviewMarkup, /overview-card-grid/)
  assert.match(overviewMarkup, /model\.dispatch\.scopeRows/)
  assert.match(overviewMarkup, /overview-horizontal-bar/)
  assert.match(overviewMarkup, /overviewThreeCheckDonut/)
  assert.match(overviewMarkup, /overviewHazardFunnel/)
  assert.match(overviewMarkup, /隐患整改/)
  assert.match(overviewMarkup, /overview-funnel-chart/)
  assert.doesNotMatch(overviewMarkup, /overviewHazardTrend|隐患趋势/)
  assert.match(overviewMarkup, /完成人次\/应学人次/)
  assert.doesNotMatch(overviewMarkup, /model\.dispatch\.total|model\.hazard\.total/)
  assert.match(overviewMarkup, /model\.threeCheck\.overviewRate/)
  assert.match(overviewMarkup, /model\.threeCheck\.overviewRemainingRate/)
  assert.match(overviewMarkup, /model\.learning\.overviewCompletionRate/)
  assert.doesNotMatch(overviewMarkup, /model\.learning\.completionRate/)
  assert.match(overviewStyles, /grid-template-columns:\s*repeat\(2/)
  assert.match(overviewStyles, /height:\s*446rpx/)
  assert.match(overviewStyles, /height:\s*910rpx/)
  assert.match(overviewStyles, /\.overview-horizontal-row\s*\{[^}]*grid-template-columns:\s*88rpx minmax\(0,\s*1fr\);/)
  assert.match(overviewStyles, /\.overview-horizontal-label\s*\{[^}]*text-align:\s*left;/)
  assert.match(overviewStyles, /\.overview-horizontal-label\s*\{[^}]*padding-right:\s*4rpx;/)
  assert.match(dispatchMarkup, /dispatch-org-chart/)
  assert.match(dispatchMarkup, /dispatch-trend-card/)
  assert.match(threeCheckMarkup, /three-rate-card/)
  assert.match(threeCheckMarkup, /一班三查完成情况/)
  assert.match(threeCheckMarkup, /model\.threeCheck\.completionRates/)
  assert.match(threeCheckMarkup, /threeRateDonut\{\{index\}\}/)
  assert.match(threeCheckMarkup, /three-detail-card/)
  assert.doesNotMatch(threeCheckMarkup, /premeeting-card|model\.threeCheck\.preMeetingWidth|three-team-chart-row|detailCards\.threeTeams|前置活动|说明：完成率/)
  assert.match(readMini("pages/analysis/three-check/index.wxss"), /\.three-rate-card\s*\{\s*min-height:\s*326rpx/)
  assert.match(hazardMarkup, /hazardClosureDonut/)
  assert.match(hazardMarkup, /hazardStatusDonut/)
  assert.match(hazardMarkup, /hazard-rate-donut/)
  assert.match(hazardMarkup, /hazard-status-list hazard-rate-legend/)
  assert.match(hazardMarkup, /model\.hazard\.closureRows/)
  assert.match(hazardMarkup, /整改状态分布/)
  assert.match(hazardMarkup, /隐患闭环情况/)
  assert.doesNotMatch(hazardMarkup, /hazard-rate-copy|按整改状态分布|各组织隐患闭环情况|detailCards\.hazardOrg|alert-icon/)
  const hazardStyles = readMini("pages/analysis/hazard/index.wxss")
  assert.match(hazardStyles, /min-height:\s*144rpx/)
  assert.match(hazardStyles, /\.hazard-status-row\s*\{[^}]*margin:18rpx 0;[^}]*font-size:22rpx;[^}]*line-height:1\.45;/)
  assert.match(learningMarkup, /learning-kpi-grid/)
  assert.match(learningMarkup, /截至结束日近7天/)
  assert.match(learningMarkup, /组织内容数量 TOP5/)
  assert.match(learningMarkup, /内容分类分布/)
  assert.match(learningStyles, /min-height:\s*437rpx/)
  assert.match(learningStyles, /grid-template-columns:\s*repeat\(2/)
  for (const markup of [overviewMarkup, dispatchMarkup, threeCheckMarkup, hazardMarkup, learningMarkup]) {
    assert.doesNotMatch(markup, /<strong/)
  }
})

test("analysis cards default to charts and expose independent detail actions", () => {
  const pages = {
    dispatch: ["dispatchOrg", "dispatchTrend"],
    hazard: ["hazardClosure", "hazardStatus"],
    "three-check": ["threeRates"],
    learning: ["learningTrend", "learningRanking", "learningCategory"]
  }

  for (const [page, cards] of Object.entries(pages)) {
    const markup = readMini(`pages/analysis/${page}/index.wxml`)
    assert.doesNotMatch(markup, /图表视图|明细视图|switchView|viewMode/)
    for (const card of cards) {
      assert.match(markup, new RegExp(`data-card="${card}"`))
      assert.match(markup, new RegExp(`detailCards\\.${card} \\? '图表' : '明细'`))
    }
  }

  const pageSource = readMini("utils/analysis-page.js")
  const chartSource = readMini("utils/analysis-charts.js")
  assert.match(pageSource, /detailCards:\s*\{\}/)
  assert.match(pageSource, /drawFunnel\("overviewHazardFunnel"/)
  assert.match(pageSource, /drawDonut\(`threeRateDonut\$\{index\}`/)
  assert.match(chartSource, /function drawFunnel\(canvasId, rows/)
  assert.match(chartSource, /const bottomWidth = maxWidth - widthStep \* \(index \+ 1\)/)
  assert.match(pageSource, /toggleCardDetail\(event\)/)
  assert.match(pageSource, /\[`detailCards\.\$\{card\}`\]/)
  assert.doesNotMatch(pageSource, /viewMode:\s*"chart"|switchView\(event\)/)
})
