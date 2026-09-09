const { listSpecialWorkRecords, STATUS_OPTIONS } = require("../../../services/special-work")
const { hasPermission } = require("../../../utils/auth")
const { appendQuery } = require("../../../utils/module-routes")
const { formatDateTime, statusClass } = require("../../../utils/special-work")

const TABS = [{ value: "all", label: "全部" }, ...STATUS_OPTIONS]

function toRow(record) {
  return {
    ...record,
    applicationTimeLabel: formatDateTime(record.applicationTime),
    statusClass: statusClass(record.status)
  }
}

Page({
  data: {
    tabs: TABS,
    activeStatus: "all",
    keyword: "",
    date: "",
    rows: [],
    visibleRows: [],
    counts: {},
    canCreate: false,
    loading: false,
    errorText: ""
  },

  onLoad() {
    if (wx.setNavigationBarTitle) wx.setNavigationBarTitle({ title: "特种作业" })
    this.setData({ canCreate: hasPermission("PINGAN_SPECIAL_WORK_APPLY") })
  },

  goBack() { wx.navigateBack() },

  onShow() { return this.loadRecords() },

  async loadRecords() {
    this.setData({ loading: true, errorText: "" })
    try {
      const result = await listSpecialWorkRecords({ status: "all", page: 1, pageSize: 100 })
      const rows = (result.items || []).map(toRow)
      const counts = TABS.reduce((memo, tab) => {
        memo[tab.value] = tab.value === "all" ? rows.length : rows.filter(row => row.status === tab.value).length
        return memo
      }, {})
      this.setData({ rows, counts, tabs: TABS.map(tab => ({ ...tab, count: counts[tab.value] || 0 })), loading: false })
      this.applyView()
    } catch (error) {
      this.setData({ loading: false, errorText: error && error.message ? error.message : "特种作业加载失败" })
    }
  },

  applyView() {
    const keyword = String(this.data.keyword || "").trim().toLowerCase()
    const visibleRows = this.data.rows.filter(row => {
      if (this.data.activeStatus !== "all" && row.status !== this.data.activeStatus) return false
      if (this.data.date && !String(row.applicationTime || "").startsWith(this.data.date)) return false
      if (!keyword) return true
      return [row.company, row.project, row.workType, row.workContent, row.workLocation]
        .some(value => String(value || "").toLowerCase().includes(keyword))
    })
    this.setData({ visibleRows })
  },

  changeStatus(event) {
    this.setData({ activeStatus: event.currentTarget.dataset.status })
    this.applyView()
  },

  updateKeyword(event) {
    this.setData({ keyword: event.detail.value })
    this.applyView()
  },

  changeDate(event) {
    this.setData({ date: event.detail.value })
    this.applyView()
  },

  clearDate() {
    this.setData({ date: "" })
    this.applyView()
  },

  openForm() { wx.navigateTo({ url: "/pages/special-work/form/index" }) },

  openDetail(event) {
    wx.navigateTo({ url: appendQuery("/pages/special-work/detail/index", { id: event.currentTarget.dataset.id }) })
  }
})

module.exports = { TABS, toRow }
