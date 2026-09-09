const meService = require("../../services/me")

const TITLE_BY_TYPE = {
  dispatch: "派班记录",
  "three-check": "三查记录",
  "hazard-report": "隐患上报",
  rectification: "整改记录",
  learning: "学习记录",
  exam: "考试记录",
  points: "积分明细"
}

function decodeText(value) {
  if (!value) return ""
  try {
    return decodeURIComponent(value)
  } catch (error) {
    return value
  }
}

function normalizeRecord(item = {}) {
  const points = Number(item.pointsDelta || 0)
  return {
    ...item,
    dateText: item.businessDate || "日期未记录",
    subtitle: item.subtitle || "个人业务记录",
    pointsText: item.pointsDelta === null || item.pointsDelta === undefined
      ? ""
      : `${points > 0 ? "+" : ""}${points}`,
    statusTone: ["待整改", "待验收", "待审核", "未通过", "已驳回"].includes(item.statusLabel)
      ? "warning"
      : "success"
  }
}

function detailRoute(record = {}) {
  const id = record.targetId || record.id
  switch (record.type) {
    case "dispatch":
      return `/pages/dispatch/detail/index?id=${id}`
    case "three-check":
      if (record.moduleKey === "pre-shift-meeting") {
        return `/pages/three-check/meeting-detail/index?id=${id}`
      }
      return `/pages/three-check/inspection-detail/index?id=${id}&moduleKey=${record.moduleKey}`
    case "hazard-report":
      return `/pages/hazard/source-detail/index?id=${id}&moduleKey=${record.moduleKey}`
    case "rectification":
      return `/pages/hazard-rectification/order-detail/index?id=${id}`
    case "learning":
      return record.relatedId ? `/pages/training/learning-detail/index?id=${record.relatedId}` : ""
    case "exam":
      return record.relatedId ? `/pages/training/exam-basic/index?id=${record.relatedId}` : ""
    default:
      return ""
  }
}

Page({
  data: {
    type: "",
    title: "我的记录",
    records: [],
    page: 1,
    pageSize: 20,
    total: 0,
    hasMore: false,
    loading: false,
    loadingMore: false,
    errorText: ""
  },

  onLoad(options = {}) {
    const type = options.type || ""
    const title = decodeText(options.title) || TITLE_BY_TYPE[type] || "我的记录"
    this.setData({ type, title })
    return this.refresh()
  },

  async onPullDownRefresh() {
    await this.refresh()
    if (wx.stopPullDownRefresh) wx.stopPullDownRefresh()
  },

  async onReachBottom() {
    await this.loadMore()
  },

  async refresh() {
    if (!this.data.type) return
    this.setData({ loading: true, errorText: "", page: 1 })
    try {
      const result = await meService.listMineRecords(this.data.type, { page: 1, pageSize: this.data.pageSize })
      const records = (result.items || []).map(normalizeRecord)
      const total = Number(result.total || 0)
      this.setData({ records, total, hasMore: records.length < total, loading: false })
    } catch (error) {
      this.setData({
        loading: false,
        errorText: error && error.message ? error.message : "记录加载失败"
      })
    }
  },

  async loadMore() {
    if (this.data.loading || this.data.loadingMore || !this.data.hasMore) return
    const page = this.data.page + 1
    this.setData({ loadingMore: true })
    try {
      const result = await meService.listMineRecords(this.data.type, { page, pageSize: this.data.pageSize })
      const records = this.data.records.concat((result.items || []).map(normalizeRecord))
      const total = Number(result.total || 0)
      this.setData({ page, records, total, hasMore: records.length < total, loadingMore: false })
    } catch (error) {
      this.setData({ loadingMore: false })
      wx.showToast({ title: "加载失败", icon: "none" })
    }
  },

  openRecord(event) {
    const id = String(event.currentTarget.dataset.id || "")
    const record = this.data.records.find(item => String(item.id) === id)
    if (!record) return
    const route = detailRoute(record)
    if (route) {
      wx.navigateTo({ url: route })
    }
  },

  goBack() {
    wx.navigateBack()
  }
})

module.exports = {
  TITLE_BY_TYPE,
  decodeText,
  normalizeRecord,
  detailRoute
}
