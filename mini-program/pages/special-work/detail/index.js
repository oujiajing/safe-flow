const { getSpecialWorkRecord } = require("../../../services/special-work")
const { hasPermission } = require("../../../utils/auth")
const { appendQuery } = require("../../../utils/module-routes")
const { formatDateTime, nextStatus, statusClass, workflowAction } = require("../../../utils/special-work")

function detailView(record) {
  return {
    ...record,
    statusClass: statusClass(record.status),
    applicationTimeLabel: formatDateTime(record.applicationTime),
    implementationStartTimeLabel: formatDateTime(record.implementationStartTime),
    implementationEndTimeLabel: formatDateTime(record.implementationEndTime),
    completionAcceptanceTimeLabel: formatDateTime(record.completionAcceptanceTime)
  }
}

function canProcessStatus(status) {
  const action = workflowAction(status)
  return Boolean(action && hasPermission(action.permission))
}

Page({
  data: { id: "", record: null, canEdit: false, canProcess: false, nextStatus: "", actionLabel: "", loading: false, errorText: "" },

  async onLoad(options = {}) {
    this.setData({ id: options.id || "", canEdit: hasPermission("PINGAN_SPECIAL_WORK_REVIEW") })
    if (wx.setNavigationBarTitle) wx.setNavigationBarTitle({ title: "作业票详情" })
    await this.loadDetail()
  },

  goBack() { wx.navigateBack() },

  async onShow() { if (this.data.id && this.data.record) await this.loadDetail() },

  async loadDetail() {
    if (!this.data.id) return
    this.setData({ loading: true, errorText: "" })
    try {
      const record = detailView(await getSpecialWorkRecord(this.data.id))
      const action = workflowAction(record.status)
      this.setData({ record, canProcess: canProcessStatus(record.status), nextStatus: nextStatus(record.status), actionLabel: action ? action.label : "", loading: false })
    } catch (error) {
      this.setData({ loading: false, errorText: error && error.message ? error.message : "作业票加载失败" })
    }
  },

  openEdit() {
    wx.navigateTo({ url: appendQuery("/pages/special-work/form/index", { id: this.data.id }) })
  },

  openWorkflow() {
    wx.navigateTo({ url: appendQuery("/pages/special-work/workflow/index", { id: this.data.id }) })
  },

  previewImage() {
    const url = this.data.record && this.data.record.image && this.data.record.image.url
    if (url && wx.previewImage) wx.previewImage({ current: url, urls: [url] })
  }
})

module.exports = { canProcessStatus, detailView }
