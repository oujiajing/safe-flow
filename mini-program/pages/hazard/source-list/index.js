const { listHazardRecords } = require("../../../services/hazard-source-record")
const { getOrgTree } = require("../../../services/organization")
const { appendQuery } = require("../../../utils/module-routes")
const { organizationOptionsFor } = require("../../../utils/organization-options")

const TITLE_BY_MODULE = {
  "quick-shot": "随手拍",
  "safety-check": "安全检查"
}

const DEFAULT_STATUS_BY_MODULE = {
  "quick-shot": "REVIEWED",
  "safety-check": "OPENED"
}

const TAB_STATUS_BY_MODULE = {
  "quick-shot": [
    { title: "待审批", status: "PENDING_REVIEW", showCount: true },
    { title: "通过", status: "REVIEWED", showCount: true },
    { title: "不通过", status: "REJECTED", showCount: true },
    { title: "已闭环", status: "ACCEPTED", showCount: true }
  ],
  "safety-check": [
    { title: "待检查", status: "OPENED", showCount: true },
    { title: "已完成", status: "ARCHIVED", showCount: true }
  ]
}

function safeDecodeText(value) {
  if (!value) return ""
  try {
    return decodeURIComponent(value)
  } catch (error) {
    return value
  }
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

function addDays(date, days) {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

function defaultDateRange() {
  const end = new Date()
  return {
    startDate: formatDate(addDays(end, -7)),
    endDate: formatDate(end)
  }
}

const INITIAL_DATE_RANGE = defaultDateRange()
const DEFAULT_COMPANY_NAME = "广东广晟稀有金属光电新材料有限公司"
const DEFAULT_DEPARTMENT_NAME = "安全环保部"
const DEFAULT_REPORTER_NAME = "李燕发"
const ALL_DEPARTMENT_OPTION = { label: "全部部门", value: "" }
const ALL_TEAM_OPTION = { label: "全部班组", value: "" }

function safeTotal(result) {
  const total = Number(result && result.total)
  return Number.isFinite(total) && total > 0 ? total : 0
}

function firstFilled(...values) {
  const value = values.find(item => item !== undefined && item !== null && String(item).trim() !== "")
  return value === undefined ? "--" : String(value)
}

function joinNames(value, fallback = "--") {
  if (Array.isArray(value)) {
    const text = value.filter(Boolean).join("、")
    return text || fallback
  }
  return firstFilled(value, fallback)
}

function normalizeSafetyStatus(record = {}, payload = {}) {
  if (record.statusLabel || payload.statusLabel) {
    return record.statusLabel || payload.statusLabel
  }
  if (record.status === "ARCHIVED" || record.status === "CLOSED") {
    return "已完成"
  }
  return "待检查"
}

function buildTabLabel(tab, count) {
  return tab.showCount ? `${tab.title} (${count})` : tab.title
}

function normalizeTabs(moduleKey, counts = {}) {
  const baseTabs = TAB_STATUS_BY_MODULE[moduleKey] || TAB_STATUS_BY_MODULE["safety-check"]
  return baseTabs.map(tab => {
    const count = counts[tab.status] || 0
    return {
      ...tab,
      count,
      label: buildTabLabel(tab, count)
    }
  })
}

function optionIndexByValue(options, value) {
  const index = options.findIndex(option => String(option.value) === String(value))
  return index >= 0 ? index : 0
}

function normalizeDisplayRecord(record = {}) {
  const payload = record.payload || {}
  const companyName = payload.companyName || record.fullCompanyName || record.companyFullName || record.companyName || record.company || DEFAULT_COMPANY_NAME
  const inspectedUnitName = firstFilled(payload.inspectedUnitName, payload.inspectedUnit, record.companyName, record.company, companyName)
  const checkType = firstFilled(payload.checkType, payload.manualCheckType, record.checkType)
  return {
    ...record,
    displayCompanyName: String(companyName).length < 10 ? DEFAULT_COMPANY_NAME : companyName,
    displayDepartmentName: payload.departmentName || record.departmentName || DEFAULT_DEPARTMENT_NAME,
    displayTeamName: payload.teamName || record.teamName || record.team || "",
    displayReporterName: payload.reporterName || record.ownerUserName || record.reporterName || DEFAULT_REPORTER_NAME,
    displayDate: record.businessDate || record.recordDate || String(record.createdAt || "").slice(0, 10),
    safetyTitle: inspectedUnitName,
    safetyInspectionUnit: firstFilled(payload.inspectionUnitName, payload.inspectionUnit, record.inspectionUnitName, record.inspectionUnit, companyName),
    safetyCheckType: checkType,
    safetyCheckTime: firstFilled(payload.checkTime, payload.checkDate, record.businessDate, record.recordDate, String(record.createdAt || "").slice(0, 16)),
    safetyCheckMethod: firstFilled(payload.checkMethod, payload.method, record.checkMethod),
    safetyInspectors: joinNames(payload.inspectors || payload.inspectorNames || record.inspectors, firstFilled(payload.creatorName, record.ownerUserName, record.reporterName)),
    safetyAcceptance: firstFilled(payload.acceptancePassed, payload.rectificationAcceptance, payload.acceptanceResult, payload.acceptanceUsers && joinNames(payload.acceptanceUsers)),
    safetyStatusLabel: normalizeSafetyStatus(record, payload)
  }
}

Page({
  data: {
    moduleKey: "quick-shot",
    title: "随手拍",
    companyKey: "gs",
    status: "PENDING_REVIEW",
    tabs: normalizeTabs("quick-shot"),
    records: [],
    total: 0,
    page: 1,
    pageSize: 20,
    filterVisible: false,
    filterApplied: false,
    startDate: INITIAL_DATE_RANGE.startDate,
    endDate: INITIAL_DATE_RANGE.endDate,
    organizationTree: [],
    departmentOptions: [ALL_DEPARTMENT_OPTION],
    teamOptions: [ALL_TEAM_OPTION],
    selectedDepartmentId: "",
    selectedTeamId: "",
    departmentIndex: 0,
    teamIndex: 0,
    currentDepartmentLabel: ALL_DEPARTMENT_OPTION.label,
    currentTeamLabel: ALL_TEAM_OPTION.label,
    loading: false,
    errorText: ""
  },

  async onLoad(options = {}) {
    const moduleKey = options.moduleKey || "quick-shot"
    const title = safeDecodeText(options.title) || TITLE_BY_MODULE[moduleKey] || "隐患记录"
    this.setData({
      moduleKey,
      title,
      companyKey: options.companyKey || "gs",
      status: options.status || DEFAULT_STATUS_BY_MODULE[moduleKey] || "all",
      tabs: normalizeTabs(moduleKey)
    })
    if (wx.setNavigationBarTitle) {
      wx.setNavigationBarTitle({ title })
    }
    await this.loadOrganizationFilters()
    await this.loadRecords()
  },

  async loadOrganizationFilters() {
    try {
      const organizationTree = await getOrgTree()
      const departmentOptions = [ALL_DEPARTMENT_OPTION, ...organizationOptionsFor(organizationTree, undefined, ["DEPARTMENT"])]
      const teamOptions = [ALL_TEAM_OPTION, ...organizationOptionsFor(organizationTree, undefined, ["TEAM"])]
      this.setData({
        organizationTree,
        departmentOptions,
        teamOptions,
        selectedDepartmentId: "",
        selectedTeamId: "",
        departmentIndex: 0,
        teamIndex: 0,
        currentDepartmentLabel: ALL_DEPARTMENT_OPTION.label,
        currentTeamLabel: ALL_TEAM_OPTION.label
      })
    } catch (error) {
      this.setData({
        organizationTree: [],
        departmentOptions: [ALL_DEPARTMENT_OPTION],
        teamOptions: [ALL_TEAM_OPTION],
        selectedDepartmentId: "",
        selectedTeamId: "",
        departmentIndex: 0,
        teamIndex: 0,
        currentDepartmentLabel: ALL_DEPARTMENT_OPTION.label,
        currentTeamLabel: ALL_TEAM_OPTION.label
      })
    }
  },

  applyOrganizationQuery(query) {
    if (this.data.selectedDepartmentId) {
      query.departmentId = this.data.selectedDepartmentId
    }
    if (this.data.selectedTeamId) {
      query.teamId = this.data.selectedTeamId
    }
    return query
  },

  async loadRecords() {
    const query = {
      page: this.data.page,
      pageSize: this.data.pageSize
    }
    if (this.data.status !== "all") {
      query.status = this.data.status
    }
    if (this.data.filterApplied) {
      query.dateStart = this.data.startDate
      query.dateEnd = this.data.endDate
    }
    this.applyOrganizationQuery(query)
    this.setData({ loading: true, errorText: "" })
    try {
      const result = await listHazardRecords(this.data.moduleKey, query)
      const tabs = await this.loadTabCounts().catch(() => this.data.tabs)
      this.setData({
        records: (result.items || []).map(normalizeDisplayRecord),
        total: safeTotal(result),
        tabs,
        loading: false
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorText: error && error.message ? error.message : "记录加载失败"
      })
    }
  },

  buildCountQuery(status) {
    const query = {
      page: 1,
      pageSize: 1,
      status
    }
    if (this.data.filterApplied) {
      query.dateStart = this.data.startDate
      query.dateEnd = this.data.endDate
    }
    return this.applyOrganizationQuery(query)
  },

  async loadTabCounts() {
    const baseTabs = TAB_STATUS_BY_MODULE[this.data.moduleKey] || TAB_STATUS_BY_MODULE["safety-check"]
    const results = await Promise.all(baseTabs.map(tab => listHazardRecords(this.data.moduleKey, this.buildCountQuery(tab.status))))
    const counts = baseTabs.reduce((memo, tab, index) => {
      memo[tab.status] = safeTotal(results[index])
      return memo
    }, {})
    return normalizeTabs(this.data.moduleKey, counts)
  },

  async changeStatus(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    this.setData({ status: dataset.status || "all", page: 1 })
    await this.loadRecords()
  },

  async changeDepartmentFilter(event = {}) {
    const rawIndex = event.detail ? Number(event.detail.value) : 0
    const departmentIndex = Number.isFinite(rawIndex) ? rawIndex : 0
    const department = this.data.departmentOptions[departmentIndex] || ALL_DEPARTMENT_OPTION
    const selectedDepartmentId = department.value || ""
    const teamOptions = [
      ALL_TEAM_OPTION,
      ...organizationOptionsFor(this.data.organizationTree, selectedDepartmentId || undefined, ["TEAM"])
    ]
    this.setData({
      selectedDepartmentId,
      selectedTeamId: "",
      departmentIndex: optionIndexByValue(this.data.departmentOptions, selectedDepartmentId),
      teamIndex: 0,
      teamOptions,
      currentDepartmentLabel: department.label || ALL_DEPARTMENT_OPTION.label,
      currentTeamLabel: ALL_TEAM_OPTION.label,
      page: 1
    })
    await this.loadRecords()
  },

  async changeTeamFilter(event = {}) {
    const rawIndex = event.detail ? Number(event.detail.value) : 0
    const teamIndex = Number.isFinite(rawIndex) ? rawIndex : 0
    const team = this.data.teamOptions[teamIndex] || ALL_TEAM_OPTION
    const selectedTeamId = team.value || ""
    this.setData({
      selectedTeamId,
      teamIndex: optionIndexByValue(this.data.teamOptions, selectedTeamId),
      currentTeamLabel: team.label || ALL_TEAM_OPTION.label,
      page: 1
    })
    await this.loadRecords()
  },

  openFilter() {
    this.setData({ filterVisible: true })
  },

  closeFilter() {
    this.setData({ filterVisible: false })
  },

  changeStartDate(event = {}) {
    this.setData({ startDate: event.detail ? event.detail.value : this.data.startDate })
  },

  changeEndDate(event = {}) {
    this.setData({ endDate: event.detail ? event.detail.value : this.data.endDate })
  },

  async changeInlineStartDate(event = {}) {
    const startDate = event.detail && event.detail.value ? event.detail.value : this.data.startDate
    const endDate = startDate > this.data.endDate ? startDate : this.data.endDate
    this.setData({ startDate, endDate, filterApplied: true, page: 1 })
    await this.loadRecords()
  },

  async changeInlineEndDate(event = {}) {
    const endDate = event.detail && event.detail.value ? event.detail.value : this.data.endDate
    const startDate = endDate < this.data.startDate ? endDate : this.data.startDate
    this.setData({ startDate, endDate, filterApplied: true, page: 1 })
    await this.loadRecords()
  },

  async changeSafetyStartDate(event = {}) {
    await this.changeInlineStartDate(event)
  },

  async changeSafetyEndDate(event = {}) {
    await this.changeInlineEndDate(event)
  },

  async applyFilter() {
    this.setData({ filterApplied: true, filterVisible: false, page: 1 })
    await this.loadRecords()
  },

  openCreate() {
    wx.navigateTo({
      url: appendQuery("/pages/hazard/source-form/index", {
        moduleKey: this.data.moduleKey,
        title: this.data.title,
        companyKey: this.data.companyKey
      })
    })
  },

  openDetail(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    if (!dataset.id) return
    wx.navigateTo({
      url: appendQuery("/pages/hazard/source-detail/index", {
        id: dataset.id,
        moduleKey: this.data.moduleKey,
        title: this.data.title,
        companyKey: this.data.companyKey
      })
    })
  },

  openRectificationOrder(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    if (!dataset.id) return
    wx.navigateTo({
      url: appendQuery("/pages/hazard-rectification/order-detail/index", {
        moduleKey: this.data.moduleKey,
        sourceRecordId: dataset.id
      })
    })
  },

  goBack() {
    const pages = typeof getCurrentPages === "function" ? getCurrentPages() : []
    if (pages.length > 1 && wx.navigateBack) {
      wx.navigateBack()
      return
    }
    if (wx.switchTab) {
      wx.switchTab({ url: "/pages/home/home" })
    }
  }
})
