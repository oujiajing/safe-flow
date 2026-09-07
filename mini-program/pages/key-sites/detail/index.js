const { getRecord, getRecordWorkflow, openRectificationOrder, remindRecord } = require("../../../services/mini-three-check")
const { appendQuery } = require("../../../utils/module-routes")
const { mediaKind, mediaUrl, openMediaPreview } = require("../../../utils/media-preview")

const MODULE_KEY = "key-sites"

function workflowRoute(id, step) {
  return appendQuery("/pages/key-sites/workflow/index", { id, step })
}

function detailAttachments(record) {
  return ((record && record.attachments) || []).map((attachment, index) => ({
    ...attachment,
    fileKind: mediaKind(attachment).toUpperCase(),
    name: attachment.originalName || attachment.name || `附件${index + 1}`,
    previewUrl: mediaUrl(attachment)
  })).filter(attachment => attachment.previewUrl)
}

function linkedOrder(record) {
  const items = record && record.payload && Array.isArray(record.payload.checkItems)
    ? record.payload.checkItems
    : []
  const item = items.find(entry => entry.rectificationOrderId)
  return item ? {
    id: item.rectificationOrderId,
    orderNo: item.rectificationOrderNo,
    status: item.rectificationStatusLabel || item.rectificationStatus
  } : null
}

Page({
  data: { id: "", record: null, order: null, workflow: [], viewStatus: "待检查", attachments: [], attachmentCount: 0, previewVideoUrl: "", loading: false, reminding: false, errorText: "" },

  async onLoad(options = {}) {
    this.setData({ id: options.id || "" })
    if (wx.setNavigationBarTitle) wx.setNavigationBarTitle({ title: "单据明细" })
    await this.loadDetail()
  },

  goBack() {
    wx.navigateBack()
  },

  async onShow() {
    if (this.data.id && this.data.record) await this.loadDetail()
  },

  async loadDetail() {
    if (!this.data.id) return
    this.setData({ loading: true, errorText: "" })
    try {
      const [record, workflow] = await Promise.all([
        getRecord(MODULE_KEY, this.data.id),
        getRecordWorkflow(MODULE_KEY, this.data.id)
      ])
      const attachments = detailAttachments(record)
      this.setData({
        record,
        order: linkedOrder(record),
        workflow: workflow.documentFlow || [],
        viewStatus: statusFor(record, linkedOrder(record)),
        attachments,
        attachmentCount: attachments.length,
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false, errorText: error && error.message ? error.message : "详情加载失败" })
    }
  },

  openInspection() {
    wx.navigateTo({ url: appendQuery("/pages/key-sites/inspection/index", { id: this.data.id }) })
  },

  openWorkflow(event = {}) {
    const step = event.currentTarget && event.currentTarget.dataset
      ? event.currentTarget.dataset.step
      : ""
    wx.navigateTo({ url: workflowRoute(this.data.id, step) })
  },

  previewAttachment(event) {
    const index = Number(event.currentTarget.dataset.index)
    const result = openMediaPreview(wx, this.data.attachments, index)
    if (result.videoUrl) this.setData({ previewVideoUrl: result.videoUrl })
  },

  closeVideoPreview() {
    this.setData({ previewVideoUrl: "" })
  },

  noop() {},

  async remind() {
    if (this.data.reminding) return
    this.setData({ reminding: true })
    try {
      const record = await remindRecord(MODULE_KEY, this.data.id)
      this.setData({ record, reminding: false })
      if (wx.showToast) wx.showToast({ title: "催办成功", icon: "success" })
    } catch (error) {
      this.setData({ reminding: false })
      if (wx.showToast) wx.showToast({ title: error.message || "催办失败", icon: "none" })
    }
  },

  async openOrder() {
    try {
      const order = this.data.order || await openRectificationOrder(MODULE_KEY, this.data.id)
      wx.navigateTo({
        url: appendQuery("/pages/hazard-rectification/order-detail/index", {
          id: order.id,
          moduleKey: MODULE_KEY,
          sourceRecordId: this.data.id
        })
      })
    } catch (error) {
      if (wx.showToast) wx.showToast({ title: error.message || "暂无整改单", icon: "none" })
    }
  }
})

function statusFor(record, order) {
  if (record.status !== "OPENED" && record.status !== "ARCHIVED") return "待检查"
  if (!order) return "已检查"
  const status = order.status || ""
  if (status.includes("验收") || status === "RECTIFIED" || status === "PENDING_ACCEPTANCE") return "待验收"
  if (status.includes("关闭") || status.includes("闭环") || status === "CLOSED") return "已闭环"
  return "待整改"
}

module.exports = { detailAttachments, linkedOrder, statusFor, workflowRoute }
