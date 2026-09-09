const { getRecord, updateRecord, submitRecord, uploadAttachment } = require("../../../services/mini-three-check")
const { createClientRequestId, createClientUpdatedAt, createSourceRecordId } = require("../../../utils/idempotency")

const MODULE_KEY = "key-sites"

function normalizedItems(record) {
  const payload = record && record.payload ? record.payload : {}
  const items = Array.isArray(payload.checkItems) ? payload.checkItems : []
  return items.map((item, index) => ({ ...item, lineId: item.lineId || `key-sites-${record.id}-${index + 1}`, beforePhoto: item.beforePhoto || "" }))
}

Page({
  data: { id: "", record: null, checkItems: [], completed: 0, saving: false, uploadingIndex: -1, errorText: "" },

  async onLoad(options = {}) {
    this.setData({ id: options.id || "" })
    if (wx.setNavigationBarTitle) wx.setNavigationBarTitle({ title: "执行检查" })
    await this.loadRecord()
  },

  goBack() {
    wx.navigateBack()
  },

  async loadRecord() {
    if (!this.data.id) return
    try {
      const record = await getRecord(MODULE_KEY, this.data.id)
      const checkItems = normalizedItems(record)
      this.setData({ record, checkItems, completed: checkItems.filter(item => item.checkResult).length, errorText: checkItems.length ? "" : "当前组织尚未配置重点场所检查模板" })
    } catch (error) {
      this.setData({ errorText: error && error.message ? error.message : "检查任务加载失败" })
    }
  },

  chooseResult(event) {
    const index = Number(event.currentTarget.dataset.index)
    const value = event.currentTarget.dataset.value
    const checkItems = this.data.checkItems.map((item, itemIndex) => itemIndex === index ? { ...item, checkResult: value, hazardDescription: value === "无隐患" ? "" : item.hazardDescription } : item)
    this.setData({ checkItems, completed: checkItems.filter(item => item.checkResult).length, errorText: "" })
  },

  updateDescription(event) {
    const index = Number(event.currentTarget.dataset.index)
    const checkItems = this.data.checkItems.map((item, itemIndex) => itemIndex === index ? { ...item, hazardDescription: event.detail.value } : item)
    this.setData({ checkItems, errorText: "" })
  },

  chooseImage(index) {
    return new Promise(resolve => {
      if (!wx.chooseMedia) return resolve("")
      wx.chooseMedia({ count: 1, mediaType: ["image"], sourceType: ["camera", "album"], success: result => resolve(result.tempFiles && result.tempFiles[0] ? result.tempFiles[0].tempFilePath : ""), fail: () => resolve("") })
    })
  },

  async uploadPhoto(event) {
    const index = Number(event.currentTarget.dataset.index)
    const filePath = await this.chooseImage(index)
    if (!filePath) return
    this.setData({ uploadingIndex: index })
    try {
      const attachment = await uploadAttachment(MODULE_KEY, this.data.id, "IMAGE", filePath)
      const url = attachment.url || attachment.previewUrl || attachment.path || filePath
      const checkItems = this.data.checkItems.map((item, itemIndex) => itemIndex === index ? { ...item, beforePhoto: url, localPhoto: filePath } : item)
      this.setData({ checkItems, uploadingIndex: -1 })
    } catch (error) {
      this.setData({ uploadingIndex: -1, errorText: error.message || "图片上传失败" })
    }
  },

  validate() {
    if (!this.data.checkItems.length) return "暂无可执行的检查项"
    for (let index = 0; index < this.data.checkItems.length; index += 1) {
      const item = this.data.checkItems[index]
      if (!item.checkResult) return `请选择第 ${index + 1} 项检查结果`
      if (item.checkResult === "有隐患" && !String(item.hazardDescription || "").trim()) return `请填写第 ${index + 1} 项隐患描述`
      if (item.checkResult === "有隐患" && item.requireImage && !item.beforePhoto) return `请上传第 ${index + 1} 项现场图片`
    }
    return ""
  },

  buildPayload() {
    const record = this.data.record
    return {
      companyId: record.companyId, departmentId: record.departmentId, teamId: record.teamId,
      ownerUserId: record.ownerUserId, businessDate: record.businessDate || record.date,
      content: record.content || "重点场所现场检查",
      payload: { ...(record.payload || {}), checkItems: this.data.checkItems.map(({ localPhoto, ...item }) => item) },
      version: record.version,
      sourceRecordId: record.sourceRecordId || createSourceRecordId(MODULE_KEY),
      clientRequestId: createClientRequestId(), clientUpdatedAt: createClientUpdatedAt()
    }
  },

  async submit() {
    if (this.data.saving) return
    const errorText = this.validate()
    if (errorText) return this.setData({ errorText })
    this.setData({ saving: true, errorText: "" })
    try {
      const updated = await updateRecord(MODULE_KEY, this.data.id, this.buildPayload())
      this.setData({ record: updated })
      await submitRecord(MODULE_KEY, this.data.id)
      if (wx.showToast) wx.showToast({ title: "检查已提交", icon: "success" })
      wx.redirectTo({ url: `/pages/key-sites/detail/index?id=${this.data.id}` })
    } catch (error) {
      this.setData({ saving: false, errorText: error && error.message ? error.message : "提交失败" })
    }
  }
})

module.exports = { normalizedItems }
