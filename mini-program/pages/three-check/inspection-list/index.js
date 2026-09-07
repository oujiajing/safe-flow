const { MODULE_KEY_BY_ID } = require("../../../config/modules")
const {
  getRecord,
  listRecords,
  openRectificationOrder: openRectificationOrderApi,
  remindRecord,
  submitRecord,
  withdrawRecord
} = require("../../../services/mini-three-check")
const { mapInspectionRecordsToRows } = require("../../../mappers/inspection")
const { appendQuery } = require("../../../utils/module-routes")
const { TODAY, loadThreeCheckTabCounts } = require("../../../utils/three-check-stats")
const { refreshThreeCheckActionState, threeCheckActionState } = require("../../../utils/three-check-permissions")

const MODULE_TITLE_BY_ID = {
  "before-check": "班前检查",
  "during-check": "班中检查",
  "after-check": "班后检查"
}

const MODULE_TITLE_BY_KEY = {
  "pre-shift-inspection": "班前检查",
  "mid-shift-inspection": "班中检查",
  "post-shift-inspection": "班后检查"
}

Page({
  data: {
    moduleId: "before-check",
    moduleKey: "pre-shift-inspection",
    moduleTitle: "班前检查",
    companyKey: "gs",
    inspectionMode: "list",
    inspectionTab: "pending",
    inspectionRows: [],
    currentCheckFlow: { pending: false, done: false },
    tabCounts: { pending: 0, done: 0, expired: 0 },
    total: 0,
    loading: false,
    errorText: "",
    actionMessage: "",
    inspectionDateStart: TODAY,
    inspectionDateEnd: TODAY,
    inspectionFilterApplied: false,
    showInspectionFilter: false,
    ...threeCheckActionState()
  },

  async onLoad(options = {}) {
    const moduleId = options.moduleId || "before-check"
    const moduleKey = options.moduleKey || MODULE_KEY_BY_ID[moduleId] || "pre-shift-inspection"
    const moduleTitle = MODULE_TITLE_BY_ID[moduleId] || MODULE_TITLE_BY_KEY[moduleKey] || "班前检查"
    const actionState = await refreshThreeCheckActionState(moduleId)
    this.setData({
      moduleId,
      moduleKey,
      moduleTitle,
      companyKey: options.companyKey || "gs",
      ...actionState
    })
    if (wx.setNavigationBarTitle) {
      wx.setNavigationBarTitle({ title: moduleTitle })
    }
    return this.loadInspections()
  },

  async loadInspections() {
    const status = this.data.inspectionTab === "done" ? "OPENED" : (this.data.inspectionTab === "expired" ? "all" : "DRAFT")
    const query = {
      status,
      page: 1,
      pageSize: 20
    }
    if (this.data.inspectionTab === "expired") {
      query.overdue = true
    }
    if (this.data.inspectionTab === "pending") {
      query.dateStart = TODAY
      query.dateEnd = TODAY
    }
    if (this.data.inspectionFilterApplied) {
      if (this.data.inspectionDateStart) query.dateStart = this.data.inspectionDateStart
      if (this.data.inspectionDateEnd) query.dateEnd = this.data.inspectionDateEnd
    }
    this.setData({ loading: true, errorText: "", actionMessage: "" })
    try {
      const result = await listRecords(this.data.moduleKey, query)
      const tabCounts = await loadThreeCheckTabCounts(this.data.moduleKey)
      const rows = mapInspectionRecordsToRows(result.items || [])
      this.setData({
        inspectionRows: rows,
        currentCheckFlow: {
          pending: this.data.inspectionTab === "pending" && rows.length > 0,
          done: this.data.inspectionTab === "done" && rows.length > 0,
          expired: this.data.inspectionTab === "expired" && rows.length > 0
        },
        tabCounts,
        total: result.total || 0,
        loading: false
      })
    } catch (error) {
      this.setData({
        loading: false,
        currentCheckFlow: { pending: false, done: false },
        errorText: error && error.message ? error.message : "检查记录加载失败"
      })
    }
  },

  async changeInspectionTab(event) {
    this.setData({
      inspectionTab: event.currentTarget.dataset.tab,
      actionMessage: ""
    })
    await this.loadInspections()
  },

  openInspectionFilter() {
    this.setData({ showInspectionFilter: true })
  },

  closeInspectionFilter() {
    this.setData({ showInspectionFilter: false })
  },

  changeInspectionFilterDate(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const value = event.detail && event.detail.value ? event.detail.value : ""
    if (dataset.field === "dateStart") {
      this.setData({ inspectionDateStart: value })
      return
    }
    if (dataset.field === "dateEnd") {
      this.setData({ inspectionDateEnd: value })
    }
  },

  async applyInspectionFilter() {
    this.setData({ showInspectionFilter: false, inspectionFilterApplied: true })
    await this.loadInspections()
  },

  goBack() {
    const pages = typeof getCurrentPages === "function" ? getCurrentPages() : []
    if (pages.length > 1) {
      wx.navigateBack()
      return
    }
    wx.switchTab({ url: "/pages/home/home" })
  },

  openInspectionDetail(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    wx.navigateTo({
      url: appendQuery("/pages/three-check/inspection-detail/index", {
        id: dataset.id,
        moduleId: this.data.moduleId,
        moduleKey: this.data.moduleKey,
        companyKey: this.data.companyKey
      })
    })
  },

  async remindInspection(event = {}) {
    if (!this.data.canRemindAction) return
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const id = dataset.id
    if (!id) return
    try {
      await remindRecord(this.data.moduleKey, id)
      await this.loadInspections()
      this.showActionMessage("已催一下")
    } catch (error) {
      this.showActionMessage(error && error.message ? error.message : "催办失败")
    }
  },

  async openRectificationOrder(event = {}) {
    if (!this.data.canCreateRectificationOrderAction) return
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    if (dataset.canCreateRectificationOrder !== true && dataset.canCreateRectificationOrder !== "true") return
    const id = dataset.id
    if (!id) return
    try {
      const order = await openRectificationOrderApi(this.data.moduleKey, id)
      const orderId = order && order.id ? order.id : ""
      if (!orderId) {
        this.showActionMessage("整改单创建失败")
        return
      }
      wx.navigateTo({
        url: appendQuery("/pages/hazard-rectification/order-detail/index", {
          id: orderId,
          sourceRecordId: id,
          moduleKey: this.data.moduleKey,
          companyKey: this.data.companyKey
        })
      })
    } catch (error) {
      this.showActionMessage(error && error.message ? error.message : "开整改单失败")
    }
  },

  hasCompleteCheckResults(record) {
    const payload = record && record.payload ? record.payload : {}
    const items = Array.isArray(payload.checkItems) ? payload.checkItems : []
    return items.every(item => item.checkResult === "无隐患" || item.checkResult === "有隐患")
  },

  showActionMessage(title) {
    if (wx.showToast) {
      wx.showToast({ title, icon: "none" })
    }
    this.setData({ actionMessage: title })
  },

  async submitInspection(event = {}) {
    if (!this.data.canSubmitAction) return
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const id = dataset.id
    if (!id) return
    try {
      const record = await getRecord(this.data.moduleKey, id)
      if (!this.hasCompleteCheckResults(record)) {
        this.showActionMessage("请填写检查结果")
        return
      }
      await submitRecord(this.data.moduleKey, id)
      if (wx.showToast) {
        wx.showToast({ title: "已提交", icon: "success" })
      }
      await this.loadInspections()
    } catch (error) {
      this.showActionMessage(error && error.message ? error.message : "提交失败")
    }
  },

  async withdrawInspection(event = {}) {
    if (!this.data.canWithdrawAction) return
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const id = dataset.id
    if (!id) return
    try {
      await withdrawRecord(this.data.moduleKey, id, { reason: "小程序撤回" })
      if (wx.showToast) {
        wx.showToast({ title: "已撤回", icon: "success" })
      }
      await this.loadInspections()
    } catch (error) {
      this.showActionMessage(error && error.message ? error.message : "当前状态不可撤回")
    }
  }
})
