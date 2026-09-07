function numberValue(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function percentValue(value) {
  if (typeof value === "string" && value.includes("%")) return numberValue(value.replace("%", ""))
  return numberValue(value)
}

function percentText(numerator, denominator) {
  const total = numberValue(denominator)
  if (total <= 0) return "0.00%"
  return `${Math.min(100, Math.max(0, numberValue(numerator) * 100 / total)).toFixed(2)}%`
}

function capPercent(value) {
  return Math.min(100, Math.max(0, percentValue(value)))
}

function sum(rows, key = "value") {
  return (Array.isArray(rows) ? rows : []).reduce((total, item) => total + numberValue(item && item[key]), 0)
}

function indexByName(rows) {
  return (Array.isArray(rows) ? rows : []).reduce((result, item) => {
    if (item && item.name) result[item.name] = item
    return result
  }, {})
}

function seriesTotal(rows, name) {
  const match = (Array.isArray(rows) ? rows : []).find(item => item && item.name === name)
  return sum(match && match.data ? match.data.map(value => ({ value })) : [])
}

function flattenScopeOptions(nodes, depth = 0) {
  return (Array.isArray(nodes) ? nodes : []).flatMap(node => {
    const item = {
      id: node.id,
      name: node.name || "未命名组织",
      label: `${"　".repeat(depth)}${node.name || "未命名组织"}`,
      orgType: node.orgType || ""
    }
    return [item, ...flattenScopeOptions(node.children, depth + 1)]
  })
}

function toneForHazard(name) {
  if (name === "已闭环") return "green"
  if (name === "已作废") return "gray"
  if (name === "待派发") return "blue"
  if (name === "待验收" || name === "已整改") return "purple"
  return "orange"
}

function makeBars(rows, maxValue, key = "value") {
  const max = Math.max(1, numberValue(maxValue))
  return (Array.isArray(rows) ? rows : []).map(item => ({
    ...item,
    width: `${Math.min(100, numberValue(item && item[key]) * 100 / max).toFixed(2)}%`
  }))
}

function buildAnalysisModel(source = {}, context = {}) {
  const summary = source.summary || {}
  const dispatchTotal = numberValue(summary.dispatchTotal)
  const dispatchFinished = numberValue(summary.dispatchFinished)
  const preMeeting = seriesTotal(source.threeCheckSeries, "班前会")
  const preInspection = seriesTotal(source.threeCheckSeries, "班前检查")
  const midInspection = seriesTotal(source.threeCheckSeries, "班中检查")
  const postInspection = seriesTotal(source.threeCheckSeries, "班后检查")
  const hazard = source.hazardRectificationOrders || {}
  const hazardTotal = numberValue(summary.hazardTotal)
  const hazardClosed = numberValue(hazard.closed !== undefined ? hazard.closed : summary.hazardDone)
  const hazardOpen = numberValue(hazard.activeOpen !== undefined ? hazard.activeOpen : summary.hazardOpen)
  const hazardCancelled = numberValue(hazard.cancelled)
  const majorRisk = sum(source.riskControlBars, "major")
  const learningTotal = numberValue(summary.learningTotal)
  const learningToday = numberValue(summary.learningToday)

  const dispatchFinishedByName = indexByName(source.dispatchFinishedBars)
  const dispatchRows = (Array.isArray(source.dispatchBars) ? source.dispatchBars : []).map(item => {
    const total = numberValue(item.value)
    const finished = numberValue(dispatchFinishedByName[item.name] && dispatchFinishedByName[item.name].value)
    const pending = Math.max(total - finished, 0)
    return { name: item.name, total, finished, pending, rate: percentText(finished, total) }
  })
  const dispatchMax = Math.max(1, ...dispatchRows.map(item => item.total))
  dispatchRows.forEach(item => {
    item.finishedWidth = `${(item.finished * 100 / dispatchMax).toFixed(2)}%`
    item.pendingWidth = `${(item.pending * 100 / dispatchMax).toFixed(2)}%`
  })

  const dispatchTrend = (Array.isArray(source.dispatchTrend) ? source.dispatchTrend : []).map(item => ({
    date: String(item.name || "").slice(5),
    total: numberValue(item.total),
    finished: numberValue(item.finished),
    pending: Math.max(numberValue(item.total) - numberValue(item.finished), 0)
  }))
  const dispatchTrendMax = Math.max(1, ...dispatchTrend.map(item => item.total))
  dispatchTrend.forEach(item => {
    item.height = `${Math.max(6, item.total * 100 / dispatchTrendMax).toFixed(2)}%`
  })
  const dispatchScopeRows = (
    Array.isArray(source.dispatchScopeBars)
      ? source.dispatchScopeBars
      : source.dispatchBars
  ).map(item => ({
    name: item.name || "未命名组织",
    value: numberValue(item.value)
  }))
  const dispatchScopeMax = Math.max(1, ...dispatchScopeRows.map(item => item.value))
  dispatchScopeRows.forEach(item => {
    item.width = `${(item.value * 82 / dispatchScopeMax).toFixed(2)}%`
  })

  const hazardStatusTotal = sum(source.hazardStatusBars)
  const hazardStatusRows = (Array.isArray(source.hazardStatusBars) && source.hazardStatusBars.length > 0
    ? source.hazardStatusBars
    : [
        { name: "已闭环", value: hazardClosed },
        { name: "未闭环", value: hazardOpen },
        { name: "已作废", value: hazardCancelled }
      ]).map(item => ({
    name: item.name,
    value: numberValue(item.value),
    rate: percentText(item.value, hazardStatusTotal || hazardTotal + hazardCancelled),
    tone: toneForHazard(item.name)
  }))
  const hazardMax = Math.max(1, ...hazardStatusRows.map(item => item.value))
  makeBars(hazardStatusRows, hazardMax).forEach((item, index) => Object.assign(hazardStatusRows[index], item))
  const hazardClosureTotal = hazardClosed + hazardOpen + hazardCancelled
  const hazardClosureRows = [
    { name: "已闭环", value: hazardClosed, tone: "green" },
    { name: "未闭环", value: hazardOpen, tone: "orange" },
    { name: "已作废", value: hazardCancelled, tone: "gray" }
  ].map(item => ({
    ...item,
    rate: percentText(item.value, hazardClosureTotal)
  }))
  const hazardClosureMax = Math.max(1, ...hazardClosureRows.map(item => item.value))
  makeBars(hazardClosureRows, hazardClosureMax).forEach((item, index) => Object.assign(hazardClosureRows[index], item))
  const hazardStatusByName = indexByName(hazardStatusRows)
  const hazardPending = ["待派发", "待整改", "未闭环"].reduce(
    (total, name) => total + numberValue(hazardStatusByName[name] && hazardStatusByName[name].value),
    0
  )
  const hazardRectifying = ["已整改", "待验收"].reduce(
    (total, name) => total + numberValue(hazardStatusByName[name] && hazardStatusByName[name].value),
    0
  )
  const hazardRectified = numberValue(
    (hazardStatusByName["已闭环"] && hazardStatusByName["已闭环"].value) || hazardClosed
  )
  const hasDetailedHazardStatuses = Array.isArray(source.hazardStatusBars) && source.hazardStatusBars.length > 0
  const rectificationStages = [
    { key: "total", label: "隐患总数", value: hazardTotal },
    { key: "pending", label: "待整改", value: hasDetailedHazardStatuses ? hazardPending : hazardOpen },
    { key: "rectifying", label: "整改中", value: hasDetailedHazardStatuses ? hazardRectifying : 0 },
    { key: "rectified", label: "已整改", value: hazardRectified }
  ]
  const hazardRows = (Array.isArray(source.hazardBars) ? source.hazardBars : []).map(item => {
    const closed = numberValue(item.done)
    const open = numberValue(item.open)
    const total = closed + open
    return { name: item.name, closed, open, total, rate: percentText(closed, total) }
  })
  const hazardOrgMax = Math.max(1, ...hazardRows.map(item => item.total))
  hazardRows.forEach(item => {
    item.closedWidth = `${(item.closed * 100 / hazardOrgMax).toFixed(2)}%`
    item.openWidth = `${(item.open * 100 / hazardOrgMax).toFixed(2)}%`
  })
  const openStatusRows = hazardStatusRows.filter(item => item.name !== "已闭环" && item.name !== "已作废")

  const rates = summary.threeCheckRates || {}
  const checkRates = [
    { key: "pre", label: "班前检查", value: rates.preInspection || "0.00%", tone: "green" },
    { key: "mid", label: "班中检查", value: rates.mid || "0.00%", tone: "blue" },
    { key: "post", label: "班后检查", value: rates.post || "0.00%", tone: "orange" }
  ].map(item => ({ ...item, width: `${capPercent(item.value)}%` }))
  const completionRates = [
    { key: "meeting", label: "班前会", value: rates.pre || "0.00%", tone: "purple" },
    ...checkRates
  ]
  const overallFinished = preInspection + midInspection + postInspection
  const overallExpected = dispatchFinished * 3
  const overallRate = percentText(overallFinished, overallExpected)
  const overallRateValue = capPercent(overallRate)
  const overviewOverallRateValue = Math.round(overallRateValue)
  const teamAnalysis = source.teamAnalysis || {}
  const teamSummary = teamAnalysis.summary || {}
  const teamRows = (Array.isArray(teamAnalysis.detailRows) ? teamAnalysis.detailRows : []).map(item => ({
    id: item.teamId,
    name: item.name,
    company: item.company,
    pre: item.preCheck || "0.00%",
    mid: item.mid || "0.00%",
    post: item.post || "0.00%",
    preWidth: `${capPercent(item.preCheck)}%`,
    midWidth: `${capPercent(item.mid)}%`,
    postWidth: `${capPercent(item.post)}%`,
    unfinished: numberValue(item.unfinishedThreeChecks !== undefined ? item.unfinishedThreeChecks : item.abnormalItems)
  })).slice(0, 10)

  const learningTrend = (Array.isArray(source.learningTrend) ? source.learningTrend : []).map(item => ({
    date: String(item.name || "").slice(5),
    value: numberValue(item.value)
  }))
  const learningCompletedCount = numberValue(
    summary.learningCompletedCount !== undefined ? summary.learningCompletedCount : learningToday
  )
  const learningExpectedCount = numberValue(
    summary.learningExpectedCount !== undefined ? summary.learningExpectedCount : learningTotal
  )
  const learningCompletionRate = summary.learningCompletionRate
    || percentText(learningCompletedCount, learningExpectedCount)
  const learningRankingValues = (Array.isArray(source.learningBars) ? source.learningBars : []).map(item => numberValue(item.value))
  const learningRankings = (Array.isArray(source.learningBars) ? source.learningBars : [])
    .map(item => ({ name: item.name, value: numberValue(item.value) }))
    .sort((left, right) => right.value - left.value)
    .slice(0, 5)
    .map((item, index) => ({
      ...item,
      rank: index + 1,
      width: `${Math.round(item.value * 100 / Math.max(1, ...learningRankingValues))}%`,
      rankColor: ["#e4aa00", "#a9afb7", "#c96d36", "#475569", "#475569"][index],
      rankTone: ["rank-gold", "rank-silver", "rank-bronze", "rank-default", "rank-default"][index]
    }))
  const categoryValues = (Array.isArray(source.learningCategoryBars) ? source.learningCategoryBars : []).map(item => numberValue(item.value))
  const categories = (Array.isArray(source.learningCategoryBars) ? source.learningCategoryBars : [])
    .map((item, index) => ({
      name: item.name,
      value: numberValue(item.value),
      width: `${Math.round(numberValue(item.value) * 100 / Math.max(1, ...categoryValues))}%`,
      tone: ["green", "blue", "orange", "purple", "gray"][index % 5],
      icon: ["secured-filled", "book-open-filled", "certificate-filled", "file-safety-filled", "bookmark-filled"][index % 5]
    }))

  const kpis = [
    { key: "meeting", label: "班前未开会", value: Math.max(dispatchFinished - preMeeting, 0), unit: "个班次", tone: "orange" },
    { key: "pre", label: "班前未检查", value: Math.max(dispatchFinished - preInspection, 0), unit: "个班次", tone: "orange" },
    { key: "mid", label: "班中未检查", value: Math.max(dispatchFinished - midInspection, 0), unit: "个班次", tone: "orange" },
    { key: "post", label: "班后未检查", value: Math.max(dispatchFinished - postInspection, 0), unit: "个班次", tone: "orange" },
    { key: "hazardTotal", label: "上报隐患数", value: hazardTotal, unit: "条", tone: "blue" },
    { key: "hazardOpen", label: "未解决隐患", value: hazardOpen, unit: "条", tone: "red" },
    { key: "majorRisk", label: "当前重大风险", value: majorRisk, unit: "条", tone: "red" },
    { key: "learning", label: "新增学习内容", value: learningToday, unit: "条", tone: "blue" }
  ]

  return {
    context,
    scopeTree: Array.isArray(source.scopeOptions) ? source.scopeOptions : [],
    scopeChoices: [{ id: "", name: "全部可见组织", label: "全部可见组织", orgType: "ALL" }, ...flattenScopeOptions(source.scopeOptions)],
    overviewKpis: [
      { key: "team", label: "班组数", value: numberValue(summary.teamCount) },
      { key: "person", label: "人员数", value: numberValue(summary.personCount) },
      { key: "checks", label: "检查次数", value: overallFinished },
      { key: "hazard", label: "隐患数", value: hazardTotal }
    ],
    kpis,
    dispatch: {
      total: dispatchTotal,
      finished: dispatchFinished,
      pending: Math.max(dispatchTotal - dispatchFinished, 0),
      rate: percentText(dispatchFinished, dispatchTotal),
      rows: dispatchRows,
      trend: dispatchTrend,
      scopeRows: dispatchScopeRows
    },
    hazard: {
      total: hazardTotal,
      closed: hazardClosed,
      open: hazardOpen,
      cancelled: hazardCancelled,
      rate: percentText(hazardClosed, hazardTotal),
      closedDegree: Math.round(hazardClosed * 360 / Math.max(1, hazardTotal + hazardCancelled)),
      openDegree: Math.round(hazardOpen * 360 / Math.max(1, hazardTotal + hazardCancelled)),
      closureRows: hazardClosureRows,
      statusRows: hazardStatusRows,
      rectificationStages,
      openStatusRows,
      rows: hazardRows
    },
    threeCheck: {
      teamCount: numberValue(summary.teamCount),
      preMeetingRate: rates.pre || "0.00%",
      preMeetingWidth: `${capPercent(rates.pre)}%`,
      overallRate,
      overallRateValue,
      overviewRate: `${overviewOverallRateValue}%`,
      overviewRemainingRate: `${100 - overviewOverallRateValue}%`,
      overallRemainingRate: `${(100 - overallRateValue).toFixed(2)}%`,
      unfinished: numberValue(teamSummary.unfinishedThreeChecks !== undefined
        ? teamSummary.unfinishedThreeChecks
        : Math.max(overallExpected - overallFinished, 0)),
      rates: checkRates,
      completionRates,
      teamRows
    },
    learning: {
      periodTotal: learningTotal,
      today: learningToday,
      completionCount: learningCompletedCount,
      expectedCount: learningExpectedCount,
      completionRate: learningCompletionRate,
      overviewCompletionRate: `${Math.round(capPercent(learningCompletionRate))}%`,
      completionWidth: `${capPercent(learningCompletionRate)}%`,
      coveredOrgs: learningRankings.filter(item => item.value > 0).length,
      peak: Math.max(0, ...learningTrend.map(item => item.value)),
      trend: learningTrend,
      rankings: learningRankings,
      categories
    }
  }
}

module.exports = {
  buildAnalysisModel,
  capPercent,
  flattenScopeOptions,
  numberValue,
  percentText,
  percentValue,
  seriesTotal
}
