const { listRecords } = require("../../../services/mini-three-check")
const { appendQuery } = require("../../../utils/module-routes")

const MODULE_KEY = "key-sites"
const TABS = ["pending", "rectifying", "accepting", "closed"]

function rectificationState(record) {
  if (record.status !== "OPENED" && record.status !== "ARCHIVED") return "pending"
  const items = record.payload && Array.isArray(record.payload.checkItems) ? record.payload.checkItems : []
  const statuses = items.map(item => item.rectificationStatus).filter(Boolean)
  if (statuses.includes("PENDING_ASSIGN") || statuses.includes("PENDING_RECTIFY")) return "rectifying"
  if (statuses.includes("RECTIFIED") || statuses.includes("PENDING_ACCEPTANCE")) return "accepting"
  if (statuses.length > 0 && statuses.every(status => status === "CLOSED")) return "closed"
  return "closed"
}

function toRow(record) {
  const payload = record.payload || {}
  const state = rectificationState(record)
  const label = {
    pending: "待检查",
    rectifying: "整改中",
    accepting: "待验收",
    closed: "已检查"
  }[state]
  return {
    id: record.id,
    recordNo: record.recordNo,
    siteType: payload.siteType || "未填写场所类型",
    department: payload.inspectionDepartment || record.department || "-",
    responsibleDepartment: payload.responsibleDepartment || "-",
    responsiblePerson: payload.responsiblePerson || record.owner || "-",
    date: record.businessDate || record.date || "-",
    state,
    statusLabel: label
  }
}

function filterOptions(rows, key, allLabel, sortDescending = false) {
  const values = [...new Set(rows.map(item => item[key]).filter(value => value && value !== "-"))]
    .sort((left, right) => String(left).localeCompare(String(right), "zh-CN"))
  if (sortDescending) values.reverse()
  return [allLabel, ...values]
}

Page({
  data: {
    activeTab: "pending",
    keyword: "",
    rows: [],
    visibleRows: [],
    dateOptions: ["全部日期"],
    departmentOptions: ["全部部门"],
    siteTypeOptions: ["全部类型"],
    dateFilterIndex: 0,
    departmentFilterIndex: 0,
    siteTypeFilterIndex: 0,
    dateFilter: "",
    departmentFilter: "",
    siteTypeFilter: "",
    counts: { pending: 0, rectifying: 0, accepting: 0, closed: 0 },
    loading: false,
    errorText: ""
  },

  onLoad() {
    if (wx.setNavigationBarTitle) wx.setNavigationBarTitle({ title: "重点场所" })
  },

  goBack() {
    wx.navigateBack()
  },

  onShow() {
    return this.loadRecords()
  },

  async loadRecords() {
    this.setData({ loading: true, errorText: "" })
    try {
      const result = await listRecords(MODULE_KEY, { status: "all", page: 1, pageSize: 100 })
      const rows = (result.items || []).map(toRow)
      const counts = TABS.reduce((output, tab) => {
        output[tab] = rows.filter(item => item.state === tab).length
        return output
      }, {})
      const dateOptions = filterOptions(rows, "date", "全部日期", true)
      const departmentOptions = filterOptions(rows, "department", "全部部门")
      const siteTypeOptions = filterOptions(rows, "siteType", "全部类型")
      const dateFilterIndex = Math.max(0, dateOptions.indexOf(this.data.dateFilter))
      const departmentFilterIndex = Math.max(0, departmentOptions.indexOf(this.data.departmentFilter))
      const siteTypeFilterIndex = Math.max(0, siteTypeOptions.indexOf(this.data.siteTypeFilter))
      this.setData({
        rows,
        counts,
        dateOptions,
        departmentOptions,
        siteTypeOptions,
        dateFilterIndex,
        departmentFilterIndex,
        siteTypeFilterIndex,
        dateFilter: dateFilterIndex ? dateOptions[dateFilterIndex] : "",
        departmentFilter: departmentFilterIndex ? departmentOptions[departmentFilterIndex] : "",
        siteTypeFilter: siteTypeFilterIndex ? siteTypeOptions[siteTypeFilterIndex] : "",
        loading: false
      })
      this.applyView()
    } catch (error) {
      this.setData({ loading: false, errorText: error && error.message ? error.message : "重点场所加载失败" })
    }
  },

  applyView() {
    const keyword = String(this.data.keyword || "").trim().toLowerCase()
    const visibleRows = this.data.rows.filter(item => {
      if (item.state !== this.data.activeTab) return false
      if (this.data.dateFilter && item.date !== this.data.dateFilter) return false
      if (this.data.departmentFilter && item.department !== this.data.departmentFilter) return false
      if (this.data.siteTypeFilter && item.siteType !== this.data.siteTypeFilter) return false
      if (!keyword) return true
      return [item.siteType, item.department, item.responsibleDepartment, item.responsiblePerson]
        .some(value => String(value || "").toLowerCase().includes(keyword))
    })
    this.setData({ visibleRows })
  },

  changeTab(event) {
    this.setData({ activeTab: event.currentTarget.dataset.tab })
    this.applyView()
  },

  updateKeyword(event) {
    this.setData({ keyword: event.detail.value })
    this.applyView()
  },

  changeDateFilter(event) {
    const dateFilterIndex = Number(event.detail.value)
    this.setData({ dateFilterIndex, dateFilter: dateFilterIndex ? this.data.dateOptions[dateFilterIndex] : "" })
    this.applyView()
  },

  changeDepartmentFilter(event) {
    const departmentFilterIndex = Number(event.detail.value)
    this.setData({ departmentFilterIndex, departmentFilter: departmentFilterIndex ? this.data.departmentOptions[departmentFilterIndex] : "" })
    this.applyView()
  },

  changeSiteTypeFilter(event) {
    const siteTypeFilterIndex = Number(event.detail.value)
    this.setData({ siteTypeFilterIndex, siteTypeFilter: siteTypeFilterIndex ? this.data.siteTypeOptions[siteTypeFilterIndex] : "" })
    this.applyView()
  },

  openForm() {
    wx.navigateTo({ url: "/pages/key-sites/form/index" })
  },

  openDetail(event) {
    wx.navigateTo({ url: appendQuery("/pages/key-sites/detail/index", { id: event.currentTarget.dataset.id }) })
  },

  openInspection(event) {
    wx.navigateTo({ url: appendQuery("/pages/key-sites/inspection/index", { id: event.currentTarget.dataset.id }) })
  }
})

module.exports = { filterOptions, rectificationState, toRow }
