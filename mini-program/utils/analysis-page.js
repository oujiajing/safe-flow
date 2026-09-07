const auth = require("./auth")
const analysisContext = require("./analysis-context")
const { buildAnalysisModel } = require("./analysis-model")
const monitorCenterService = require("../services/monitor-center")
const { syncTabBarSelected } = require("./tab-bar")
const { drawDonut, drawFunnel, drawLineChart } = require("./analysis-charts")

const PAGE_ROUTES = {
  dispatch: "/pages/analysis/dispatch/index",
  hazard: "/pages/analysis/hazard/index",
  threeCheck: "/pages/analysis/three-check/index",
  learning: "/pages/analysis/learning/index"
}

function formatClock(date = new Date()) {
  return `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`
}

function hasViewPermission() {
  const codes = auth.getPermissionCodes()
  if (codes.length === 0) return true
  return codes.includes("PINGAN_MONITOR_CENTER_ENTRY") && codes.includes("PINGAN_MONITOR_CENTER_VIEW")
}

function resolveContext() {
  const context = analysisContext.getStoredContext()
  const codes = auth.getPermissionCodes()
  const user = auth.getCurrentUser() || {}
  if (context.orgId === "" && codes.length > 0 && !codes.includes("PINGAN_MONITOR_CENTER_GLOBAL_VIEW")) {
    const orgId = user.orgId || user.organizationId || user.teamId
    if (orgId !== undefined && orgId !== null && orgId !== "") {
      context.orgId = orgId
      context.orgName = user.orgName || user.organizationName || user.teamName || "当前组织"
    }
  }
  return context
}

function resolveScopeAccess() {
  const codes = auth.getPermissionCodes()
  const user = auth.getCurrentUser() || {}
  return {
    global: codes.length === 0 || codes.includes("PINGAN_MONITOR_CENTER_GLOBAL_VIEW"),
    orgId: user.orgId || user.organizationId || user.departmentId || user.teamId || ""
  }
}

function renderCharts(page, type) {
  const model = page.data.model || {}
  if (type === "overview") {
    const completionRate = Number(model.threeCheck && model.threeCheck.overallRateValue || 0)
    drawDonut("overviewThreeCheckDonut", [
      { value: completionRate, color: "#287cf0" },
      { value: Math.max(100 - completionRate, 0), color: "#d9dee6" }
    ], { sizeRpx: 230 })
    drawFunnel("overviewHazardFunnel", model.hazard && model.hazard.rectificationStages, {
      compact: true,
      heightRpx: 336
    })
  }
  if (type === "dispatch") {
    drawLineChart("dispatchTrend", model.dispatch && model.dispatch.trend, {
      primaryKey: "finished",
      primaryColor: "#0a9a62",
      secondaryKey: "pending",
      secondaryColor: "#ff7800",
      heightRpx: 190
    })
  }
  if (type === "threeCheck") {
    const colors = { green: "#07965e", blue: "#1677e8", orange: "#ff7100", purple: "#8b5cf6" }
    const rates = model.threeCheck && model.threeCheck.completionRates || []
    rates.forEach((item, index) => {
      const completionRate = Math.min(100, Math.max(0, Number(String(item.value || "0").replace("%", "")) || 0))
      drawDonut(`threeRateDonut${index}`, [
        { value: completionRate, color: colors[item.tone] || "#07965e" },
        { value: Math.max(100 - completionRate, 0), color: "#e2e7ed" }
      ], { sizeRpx: 158 })
    })
  }
  if (type === "hazard") {
    drawDonut("hazardClosureDonut", [
      { value: model.hazard && model.hazard.closed, color: "#07965e" },
      { value: model.hazard && model.hazard.open, color: "#ff7100" },
      { value: model.hazard && model.hazard.cancelled, color: "#c7cbd1" }
    ])
    drawDonut("hazardStatusDonut", (model.hazard && model.hazard.statusRows || []).map(item => ({
      value: item.value,
      color: { green: "#0a9a62", orange: "#ff7800", blue: "#1677ff", purple: "#8b5cf6", gray: "#d7dce2" }[item.tone] || "#ef4444"
    })))
  }
  if (type === "learning") {
    drawLineChart("learningTrend", model.learning && model.learning.trend, { primaryColor: "#1677ff", heightRpx: 286, showYAxisTicks: true })
  }
}

function createAnalysisPage({ type, tabPage = false }) {
  return {
    data: {
      context: resolveContext(),
      rangeText: "",
      model: {},
      scopeChoices: [],
      scopeTree: [],
      scopeAccess: resolveScopeAccess(),
      updatedAt: "--:--",
      loading: true,
      refreshing: false,
      noPermission: false,
      errorMessage: "",
      detailCards: {}
    },

    onLoad() {
      this._analysisRequestId = 0
      this._lastContextKey = ""
      this.setData({ rangeText: analysisContext.rangeText(this.data.context) })
    },

    onShow() {
      if (tabPage) syncTabBarSelected(this, "pages/analysis/analysis")
      const context = resolveContext()
      const key = analysisContext.contextKey(context)
      if (!this._loadedAt || key !== this._lastContextKey || Date.now() - this._loadedAt > 60000) {
        this.setData({ context, rangeText: analysisContext.rangeText(context) })
        this.loadAnalysis()
      }
    },

    onPullDownRefresh() {
      this.setData({ refreshing: true })
      return this.loadAnalysis(true)
    },

    async loadAnalysis(force = false) {
      if (!hasViewPermission()) {
        this.setData({ loading: false, refreshing: false, noPermission: true, errorMessage: "" })
        if (typeof wx !== "undefined" && wx.stopPullDownRefresh) wx.stopPullDownRefresh()
        return
      }
      const requestId = ++this._analysisRequestId
      const context = this.data.context
      this.setData({ loading: !force && !this._loadedAt, errorMessage: "", noPermission: false })
      try {
        const overview = await monitorCenterService.getMonitorCenterOverview(analysisContext.toQuery(context))
        if (requestId !== this._analysisRequestId) return
        const model = buildAnalysisModel(overview, context)
        const permissionCodes = auth.getPermissionCodes()
        if (permissionCodes.length > 0 && !permissionCodes.includes("PINGAN_MONITOR_CENTER_GLOBAL_VIEW")) {
          model.scopeChoices = model.scopeChoices.filter(item => item.id !== "")
        }
        const selected = model.scopeChoices.find(item => String(item.id) === String(context.orgId))
        if (selected && context.orgName !== selected.name) {
          context.orgName = selected.name
          analysisContext.setStoredContext(context)
        }
        this._loadedAt = Date.now()
        this._lastContextKey = analysisContext.contextKey(context)
        this.setData({
          context,
          rangeText: analysisContext.rangeText(context),
          model,
          scopeChoices: model.scopeChoices,
          scopeTree: model.scopeTree,
          scopeAccess: resolveScopeAccess(),
          updatedAt: formatClock(),
          loading: false,
          refreshing: false,
          errorMessage: ""
        }, () => renderCharts(this, type))
      } catch (error) {
        if (requestId !== this._analysisRequestId) return
        const forbidden = error && error.response && error.response.statusCode === 403
        this.setData({
          loading: false,
          refreshing: false,
          noPermission: forbidden,
          errorMessage: forbidden ? "" : (error && error.message ? error.message : "数据加载失败，请稍后重试")
        })
      } finally {
        if (typeof wx !== "undefined" && wx.stopPullDownRefresh) wx.stopPullDownRefresh()
      }
    },

    handleFilterApply(event) {
      const context = analysisContext.setStoredContext(event.detail)
      this.setData({ context, rangeText: analysisContext.rangeText(context) })
      this.loadAnalysis(true)
    },

    toggleCardDetail(event) {
      const card = event.currentTarget.dataset.card
      if (!card) return
      const detailCards = this.data.detailCards || {}
      const showingDetail = Boolean(detailCards[card])
      this.setData({ [`detailCards.${card}`]: !showingDetail }, () => {
        if (showingDetail) renderCharts(this, type)
      })
    },

    openDetail(event) {
      const route = PAGE_ROUTES[event.currentTarget.dataset.type]
      if (route && typeof wx !== "undefined") wx.navigateTo({ url: route })
    },

    openBusiness(event) {
      const context = this.data.context
      const params = [`dateStart=${encodeURIComponent(context.dateStart)}`, `dateEnd=${encodeURIComponent(context.dateEnd)}`]
      if (context.orgId !== "") params.push(`orgId=${encodeURIComponent(context.orgId)}`)
      const routes = {
        dispatch: "/pages/dispatch/list/index",
        hazard: "/pages/hazard-rectification/order-list/index",
        threeCheck: "/pages/three-check/inspection-list/index?moduleId=before-check&moduleKey=pre-shift-inspection",
        learning: "/pages/training/learning-list/index"
      }
      const base = routes[event.currentTarget.dataset.type]
      if (!base || typeof wx === "undefined") return
      wx.navigateTo({ url: `${base}${base.includes("?") ? "&" : "?"}${params.join("&")}` })
    },

    goBack() {
      if (typeof wx !== "undefined") wx.navigateBack()
    },

    retryLoad() {
      this.loadAnalysis(true)
    }
  }
}

module.exports = { PAGE_ROUTES, createAnalysisPage, formatClock, hasViewPermission, resolveContext, resolveScopeAccess }
