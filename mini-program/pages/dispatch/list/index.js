const { listRecords } = require("../../../services/mini-three-check")
const { mapDispatchRecordsToRows, summarizeDispatchRows } = require("../../../mappers/dispatch")

const ALL_OPTION = "全部"

function todayString() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, "0")
  const day = String(now.getDate()).padStart(2, "0")
  return `${now.getFullYear()}-${month}-${day}`
}

function normalizeTabStatus(row) {
  if (!row) return "pending"
  if (row.status === "OPENED" || row.status === "ARCHIVED" || row.statusLabel === "已提交") return "done"
  if (row.status === "WITHDRAWN" || row.statusLabel === "已撤回") return "withdrawn"
  return "pending"
}

function rowStatusClass(row) {
  const tab = normalizeTabStatus(row)
  if (tab === "done") return "done"
  if (tab === "withdrawn") return "withdrawn"
  return "pending"
}

function rowStatusLabel(row) {
  const tab = normalizeTabStatus(row)
  if (tab === "done") return "已提交"
  if (tab === "withdrawn") return "已撤回"
  return row && row.statusLabel && row.statusLabel !== "-" ? row.statusLabel : "待提交"
}

Page({
  data: {
    companyKey: "gs",
    dispatchRecordDate: todayString(),
    dispatchDateStart: todayString(),
    dispatchDateEnd: todayString(),
    dispatchListTab: "pending",
    tabCounts: {
      pending: 0,
      done: 0,
      withdrawn: 0
    },
    departmentOptions: [ALL_OPTION],
    teamOptions: [ALL_OPTION],
    departmentIndex: 0,
    teamIndex: 0,
    rawDispatchRecordRows: [],
    dispatchRecordRows: [],
    summary: {
      total: 0,
      submitted: 0,
      draft: 0,
      tasks: 0
    },
    total: 0,
    loading: false,
    errorText: "",
    actionMessage: "",
    showDispatchFilter: false
  },

  onLoad(options = {}) {
    const date = options.date || this.data.dispatchRecordDate || todayString()
    this.setData({
      companyKey: options.companyKey || "gs",
      dispatchRecordDate: date,
      dispatchDateStart: date,
      dispatchDateEnd: date
    })
    this.loadRecords()
  },

  async loadRecords() {
    this.setData({ loading: true, errorText: "", actionMessage: "" })
    try {
      const result = await listRecords("team-dispatch", {
        dateStart: this.data.dispatchDateStart,
        dateEnd: this.data.dispatchDateEnd,
        page: 1,
        pageSize: 20
      })
      const rows = mapDispatchRecordsToRows(result.items || []).map(item => ({
        ...item,
        statusClass: rowStatusClass(item),
        statusLabel: rowStatusLabel(item)
      }))
      const summary = summarizeDispatchRows(rows)
      const nextData = {
        rawDispatchRecordRows: rows,
        departmentOptions: this.uniqueOptions(rows, "department"),
        teamOptions: this.uniqueOptions(rows, "team"),
        summary: {
          ...summary,
          total: result.total || summary.total
        },
        total: result.total || rows.length,
        loading: false
      }
      this.setData({
        ...nextData,
        dispatchRecordRows: this.filterRows(rows, nextData),
        tabCounts: this.countTabs(rows, nextData)
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorText: error && error.message ? error.message : "派班记录加载失败"
      })
    }
  },

  changeDispatchListTab(event) {
    const tab = event.currentTarget.dataset.tab || "pending"
    this.setData({
      dispatchListTab: tab,
      dispatchRecordRows: this.filterRows(this.data.rawDispatchRecordRows, { dispatchListTab: tab })
    })
  },

  openDispatchFilter() {
    this.setData({ showDispatchFilter: true })
  },

  closeDispatchFilter() {
    this.setData({ showDispatchFilter: false })
  },

  changeDispatchFilterDate(event) {
    const field = event.currentTarget.dataset.field
    if (field === "dateStart") {
      this.setData({ dispatchDateStart: event.detail.value, dispatchRecordDate: event.detail.value })
      return
    }
    if (field === "dateEnd") {
      this.setData({ dispatchDateEnd: event.detail.value })
    }
  },

  changeDepartment(event) {
    const departmentIndex = Number(event.detail.value)
    this.setData({
      departmentIndex,
      dispatchRecordRows: this.filterRows(this.data.rawDispatchRecordRows, { departmentIndex })
    })
  },

  changeTeam(event) {
    const teamIndex = Number(event.detail.value)
    this.setData({
      teamIndex,
      dispatchRecordRows: this.filterRows(this.data.rawDispatchRecordRows, { teamIndex })
    })
  },

  async applyDispatchFilter() {
    this.setData({ showDispatchFilter: false })
    await this.loadRecords()
  },

  changeDispatchDate(event) {
    this.setData({
      dispatchRecordDate: event.detail.value,
      dispatchDateStart: event.detail.value,
      dispatchDateEnd: event.detail.value
    })
  },

  async queryDispatchRecords() {
    await this.loadRecords()
  },

  goBack() {
    const pages = typeof getCurrentPages === "function" ? getCurrentPages() : []
    if (pages.length > 1) {
      wx.navigateBack()
      return
    }
    wx.switchTab({ url: "/pages/home/home" })
  },

  openCreate() {
    wx.navigateTo({
      url: `/pages/dispatch/form/index?companyKey=${encodeURIComponent(this.data.companyKey)}`
    })
  },

  openDetail(event) {
    const id = event.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/dispatch/record-detail/index?id=${encodeURIComponent(id)}&companyKey=${encodeURIComponent(this.data.companyKey)}`
    })
  },

  openLegacyCreate() {
    this.openCreate()
  },

  openLegacyDetail(event) {
    this.openDetail(event)
  },

  uniqueOptions(rows, key) {
    const values = (Array.isArray(rows) ? rows : [])
      .map(item => item[key])
      .filter(value => value && value !== "-")
    return [ALL_OPTION, ...Array.from(new Set(values))]
  },

  countTabs(rows = this.data.rawDispatchRecordRows, override = {}) {
    const safeRows = this.filterByOptions(rows, override)
    return safeRows.reduce((counts, row) => {
      const tab = normalizeTabStatus(row)
      counts[tab] = (counts[tab] || 0) + 1
      return counts
    }, { pending: 0, done: 0, withdrawn: 0 })
  },

  filterRows(rows = this.data.rawDispatchRecordRows, override = {}) {
    const tab = override.dispatchListTab || this.data.dispatchListTab
    return this.filterByOptions(rows, override).filter(item => normalizeTabStatus(item) === tab)
  },

  filterByOptions(rows = this.data.rawDispatchRecordRows, override = {}) {
    const departmentOptions = override.departmentOptions || this.data.departmentOptions
    const teamOptions = override.teamOptions || this.data.teamOptions
    const departmentIndex = override.departmentIndex !== undefined ? override.departmentIndex : this.data.departmentIndex
    const teamIndex = override.teamIndex !== undefined ? override.teamIndex : this.data.teamIndex
    const department = departmentOptions[departmentIndex] || ALL_OPTION
    const team = teamOptions[teamIndex] || ALL_OPTION
    return (Array.isArray(rows) ? rows : []).filter(item => {
      const departmentMatched = department === ALL_OPTION || item.department === department
      const teamMatched = team === ALL_OPTION || item.team === team
      return departmentMatched && teamMatched
    })
  }
})
