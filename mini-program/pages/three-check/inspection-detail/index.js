const { getRecord, updateRecord, uploadAttachment } = require("../../../services/mini-three-check")
const { mapInspectionRecordToRow } = require("../../../mappers/inspection")
const { createClientRequestId, createClientUpdatedAt, createSourceRecordId } = require("../../../utils/idempotency")
const { refreshThreeCheckActionState } = require("../../../utils/three-check-permissions")

const MODULE_TITLES = {
  "pre-shift-inspection": "班前检查",
  "mid-shift-inspection": "班中检查",
  "post-shift-inspection": "班后检查"
}

Page({
  data: {
    id: "",
    moduleId: "before-check",
    moduleKey: "pre-shift-inspection",
    moduleTitle: "班前检查",
    companyKey: "gs",
    inspectionRecord: null,
    rawRecord: null,
    sourceRecordId: "",
    loading: false,
    saving: false,
    uploadingImageCheck: false,
    errorText: "",
    activeDetailTab: "basic",
    operationResult: "",
    checkResultDropdownIndex: -1,
    canUpdateAction: true
  },

  async onLoad(options = {}) {
    const moduleKey = options.moduleKey || "pre-shift-inspection"
    const actionState = await refreshThreeCheckActionState(options.moduleId || "before-check")
    this.setData({
      id: options.id || "",
      moduleId: options.moduleId || "before-check",
      moduleKey,
      moduleTitle: MODULE_TITLES[moduleKey] || "检查",
      companyKey: options.companyKey || "gs",
      canUpdateAction: actionState.canUpdateAction,
      sourceRecordId: createSourceRecordId(moduleKey)
    })
    await this.loadRecord()
  },

  async loadRecord() {
    if (!this.data.id) return
    this.setData({ loading: true, errorText: "" })
    try {
      const record = await getRecord(this.data.moduleKey, this.data.id)
      this.setData({
        inspectionRecord: mapInspectionRecordToRow(record),
        rawRecord: record,
        loading: false
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorText: error && error.message ? error.message : "检查详情加载失败"
      })
    }
  },

  chooseImageFile() {
    if (wx.chooseMedia) {
      return new Promise(resolve => {
        wx.chooseMedia({
          count: 1,
          mediaType: ["image"],
          sourceType: ["album", "camera"],
          success: result => {
            const file = (result.tempFiles || []).find(item => item && item.tempFilePath)
            resolve(file ? file.tempFilePath : "")
          },
          fail: () => resolve("")
        })
      })
    }
    if (!wx.chooseImage) {
      this.setData({ operationResult: "当前微信环境不支持选择图片" })
      return Promise.resolve("")
    }
    return new Promise(resolve => {
      wx.chooseImage({
        count: 1,
        sourceType: ["album", "camera"],
        success: result => resolve((result.tempFilePaths && result.tempFilePaths[0]) || ""),
        fail: () => resolve("")
      })
    })
  },

  async uploadInspectionImageCheck() {
    if (!this.data.canUpdateAction) return
    if (!this.data.id) return
    const filePath = await this.chooseImageFile()
    if (!filePath) return
    this.setData({ uploadingImageCheck: true, operationResult: "" })
    try {
      await uploadAttachment(this.data.moduleKey, this.data.id, "IMAGE", filePath)
      await this.loadRecord()
      this.setData({ uploadingImageCheck: false, operationResult: "图片打卡已上传" })
      if (wx.showToast) {
        wx.showToast({ title: "图片打卡已上传", icon: "success" })
      }
    } catch (error) {
      this.setData({
        uploadingImageCheck: false,
        operationResult: error && error.message ? error.message : "图片打卡上传失败"
      })
    }
  },

  toggleDetailItem(event) {
    const index = Number(event.currentTarget.dataset.index)
    const inspectionRecord = this.data.inspectionRecord
    if (!inspectionRecord || !Array.isArray(inspectionRecord.checkItems)) return
    const checkItems = inspectionRecord.checkItems.map((item, itemIndex) => ({
      ...item,
      expanded: itemIndex === index ? !item.expanded : item.expanded
    }))
    this.setData({ inspectionRecord: { ...inspectionRecord, checkItems } })
  },

  toggleCheckResultDropdown(event) {
    const index = Number(event.currentTarget.dataset.index)
    this.setData({
      checkResultDropdownIndex: this.data.checkResultDropdownIndex === index ? -1 : index
    })
  },

  chooseCheckResult(event) {
    const index = Number(event.currentTarget.dataset.index)
    const result = event.currentTarget.dataset.value
    const inspectionRecord = this.data.inspectionRecord
    if (!inspectionRecord || !Array.isArray(inspectionRecord.checkItems)) return
    const checkItems = inspectionRecord.checkItems.map((item, itemIndex) => ({
      ...item,
      checkResult: itemIndex === index ? result : item.checkResult
    }))
    this.setData({
      inspectionRecord: { ...inspectionRecord, checkItems },
      checkResultDropdownIndex: -1,
      operationResult: ""
    })
  },

  updateRemark(event) {
    const inspectionRecord = this.data.inspectionRecord
    if (!inspectionRecord) return
    this.setData({
      inspectionRecord: { ...inspectionRecord, remark: event.detail.value },
      operationResult: ""
    })
  },

  buildSavePayload() {
    const record = this.data.rawRecord || {}
    const row = this.data.inspectionRecord || {}
    const rawPayload = row.rawPayload || record.payload || {}
    return {
      rootRecordId: this.data.id,
      recordDate: row.date,
      companyId: record.companyId || "",
      departmentId: record.departmentId || "",
      teamId: record.teamId || "",
      ownerUserId: record.ownerUserId || "",
      businessDate: row.date,
      content: row.remark && row.remark !== "-" ? row.remark : row.task || "现场安全检查",
      payload: {
        ...rawPayload,
        companyName: row.company,
        departmentName: row.dept,
        teamName: row.team,
        ownerName: row.owner,
        remarks: row.remark === "-" ? "" : row.remark,
        checkItems: (row.checkItems || []).map(item => {
          const { expanded, ...rest } = item
          return rest
        })
      },
      version: row.version || record.version,
      sourceRecordId: this.data.sourceRecordId,
      clientRequestId: createClientRequestId(),
      clientUpdatedAt: createClientUpdatedAt()
    }
  },

  async saveInspectionDetail() {
    if (!this.data.canUpdateAction) {
      this.setData({ operationResult: "当前账号仅可查看检查记录" })
      return
    }
    if (this.data.saving || !this.data.id) return
    this.setData({ saving: true, operationResult: "" })
    try {
      await updateRecord(this.data.moduleKey, this.data.id, this.buildSavePayload())
      this.setData({ saving: false, operationResult: "明细已保存" })
      if (wx.showToast) {
        wx.showToast({ title: "明细已保存", icon: "success" })
      }
    } catch (error) {
      this.setData({
        saving: false,
        operationResult: error && error.message ? error.message : "明细保存失败"
      })
    }
  },

  switchDetailTab(event) {
    const tab = event.currentTarget && event.currentTarget.dataset
      ? event.currentTarget.dataset.tab
      : "basic"
    this.setData({ activeDetailTab: tab === "detail" ? "detail" : "basic" })
  },

  goBack() {
    wx.navigateBack()
  }
})
