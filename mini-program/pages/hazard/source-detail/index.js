const {
  getHazardRecord,
  openSourceRectificationOrder,
  workflowHazardRecord
} = require("../../../services/hazard-source-record")
const { getOrgTree } = require("../../../services/organization")
const { organizationOptionsFor } = require("../../../utils/organization-options")

const TITLE_BY_MODULE = {
  "quick-shot": "随手拍",
  "safety-check": "安全检查"
}

function safeDecodeText(value) {
  if (!value) return ""
  try {
    return decodeURIComponent(value)
  } catch (error) {
    return value
  }
}

Page({
  data: {
    id: "",
    moduleKey: "quick-shot",
    title: "隐患记录",
    companyKey: "gs",
    record: null,
    inspectionUnitOptions: [],
    inspectedUnitOptions: [],
    selectedInspectionUnitId: "",
    selectedInspectedUnitId: "",
    inspectionUnitIndex: 0,
    inspectedUnitIndex: 0,
    safetyDetailTab: "basic",
    currentHazardDetail: null,
    currentHazardDetailIndex: 0,
    showHazardDetail: false,
    loading: false,
    actioning: false,
    errorText: "",
    rejectReason: "隐患不成立"
  },

  async onLoad(options = {}) {
    const moduleKey = options.moduleKey || "quick-shot"
    const title = safeDecodeText(options.title) || TITLE_BY_MODULE[moduleKey] || "隐患记录"
    this.setData({
      id: options.id || "",
      moduleKey,
      title,
      companyKey: options.companyKey || "gs"
    })
    if (wx.setNavigationBarTitle) {
      wx.setNavigationBarTitle({ title })
    }
    await this.loadRecord()
  },

  async loadRecord() {
    if (!this.data.id) return
    this.setData({ loading: true, errorText: "" })
    try {
      const record = await getHazardRecord(this.data.moduleKey, this.data.id)
      if (this.data.moduleKey === "safety-check") {
        await this.loadSafetyCheckOrganizations(record)
      }
      this.setData({ record, loading: false })
    } catch (error) {
      this.setData({
        loading: false,
        errorText: error && error.message ? error.message : "记录加载失败"
      })
    }
  },

  async loadSafetyCheckOrganizations(record) {
    try {
      const nodes = await getOrgTree()
      const inspectionUnitOptions = organizationOptionsFor(nodes, undefined, ["GROUP", "COMPANY", "SCHOOL"])
      const inspectedUnitOptions = organizationOptionsFor(nodes, undefined, ["COMPANY", "SCHOOL"])
      const payload = record && record.payload ? record.payload : {}
      const inspectionUnitId = payload.inspectionUnitId || record.inspectionUnitId || inspectionUnitOptions[0]?.value || ""
      const inspectedUnitId = payload.inspectedUnitId || record.inspectedUnitId || record.companyId || inspectedUnitOptions[0]?.value || ""
      const inspectionUnitIndex = Math.max(0, inspectionUnitOptions.findIndex(item => String(item.value) === String(inspectionUnitId)))
      const inspectedUnitIndex = Math.max(0, inspectedUnitOptions.findIndex(item => String(item.value) === String(inspectedUnitId)))
      this.setData({
        inspectionUnitOptions,
        inspectedUnitOptions,
        selectedInspectionUnitId: inspectionUnitOptions[inspectionUnitIndex]?.value || "",
        selectedInspectedUnitId: inspectedUnitOptions[inspectedUnitIndex]?.value || "",
        inspectionUnitIndex,
        inspectedUnitIndex
      })
    } catch (error) {
      this.setData({
        inspectionUnitOptions: [],
        inspectedUnitOptions: []
      })
    }
  },

  changeInspectionUnit(event = {}) {
    const index = Number(event.detail ? event.detail.value : 0)
    const unit = this.data.inspectionUnitOptions[index]
    if (!unit) return
    this.setData({
      selectedInspectionUnitId: unit.value,
      inspectionUnitIndex: index
    })
  },

  changeInspectedUnit(event = {}) {
    const index = Number(event.detail ? event.detail.value : 0)
    const unit = this.data.inspectedUnitOptions[index]
    if (!unit) return
    this.setData({
      selectedInspectedUnitId: unit.value,
      inspectedUnitIndex: index
    })
  },

  changeSafetyDetailTab(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const tab = dataset.tab || "hazards"
    this.setData({ safetyDetailTab: tab === "basic" ? "basic" : "hazards" })
  },

  openHazardDetail(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const index = Number(dataset.index || 0)
    const items = this.data.record && this.data.record.payload ? this.data.record.payload.checkItems || [] : []
    const currentHazardDetail = items[index]
    if (!currentHazardDetail) return
    this.setData({
      currentHazardDetail,
      currentHazardDetailIndex: index,
      showHazardDetail: true
    })
  },

  closeHazardDetail() {
    this.setData({
      showHazardDetail: false,
      currentHazardDetail: null
    })
  },

  updateRejectReason(event = {}) {
    this.setData({ rejectReason: event.detail ? event.detail.value : "" })
  },

  approveQuickShot() {
    return this.runWorkflow("APPROVE", {})
  },

  rejectQuickShot(event = {}) {
    const value = event.detail && event.detail.value ? event.detail.value : this.data.rejectReason
    return this.runWorkflow("REJECT", { rejectReason: value || "隐患不成立" })
  },

  async runWorkflow(action, payload) {
    if (!this.data.record || this.data.actioning) return
    this.setData({ actioning: true })
    try {
      const record = await workflowHazardRecord(this.data.moduleKey, this.data.id, {
        action,
        version: this.data.record.version || 0,
        payload
      })
      this.setData({ record, actioning: false })
      if (wx.showToast) {
        wx.showToast({ title: "操作成功", icon: "success" })
      }
    } catch (error) {
      this.setData({ actioning: false })
      if (wx.showToast) {
        wx.showToast({ title: error && error.message ? error.message : "操作失败", icon: "none" })
      }
    }
  },

  async openRectificationOrder() {
    if (!this.data.id) return
    try {
      const order = await openSourceRectificationOrder(this.data.moduleKey, this.data.id)
      if (!order || !order.id) {
        if (wx.showToast) wx.showToast({ title: "整改单打开失败", icon: "none" })
        return
      }
      wx.navigateTo({ url: `/pages/hazard-rectification/order-detail/index?id=${order.id}` })
    } catch (error) {
      if (wx.showToast) {
        wx.showToast({ title: error && error.message ? error.message : "整改单打开失败", icon: "none" })
      }
    }
  },

  previewRecordPhoto() {
    const photo = this.data.record && this.data.record.payload ? this.data.record.payload.photo : ""
    if (!photo || !wx.previewImage) return
    wx.previewImage({
      current: photo,
      urls: [photo]
    })
  },

  goBack() {
    if (wx.navigateBack) {
      wx.navigateBack()
    }
  }
})
