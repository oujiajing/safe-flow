const { executeSpecialWorkAction, getSpecialWorkRecord, STATUS_OPTIONS } = require("../../../services/special-work")
const { dateTimeParts, mergeDateTimePart, nextStatus, nowDateTime, recordPayload, statusClass, WORKFLOW_ACTION_BY_STATUS, workflowAction } = require("../../../utils/special-work")

function actionPayload(status, form) {
  if (status === "PENDING_APPROVAL") {
    return {
      implementationStartTime: form.implementationStartTime,
      safetyDisclosurePerson: form.safetyDisclosurePerson,
      guardian: form.guardian,
      disclosureReceiver: form.disclosureReceiver
    }
  }
  if (status === "IN_PROGRESS") return { implementationEndTime: form.implementationEndTime }
  if (status === "PENDING_ACCEPTANCE") {
    return {
      completionAcceptor: form.completionAcceptor,
      completionAcceptanceTime: form.completionAcceptanceTime
    }
  }
  return {}
}

function flowSteps(status) {
  const currentIndex = STATUS_OPTIONS.findIndex(item => item.value === status)
  return STATUS_OPTIONS.map((item, index) => ({ ...item, index: index + 1, state: index < currentIndex ? "done" : (index === currentIndex ? "current" : "pending") }))
}

function transitionError(status, form) {
  if (status === "PENDING_APPROVAL") {
    if (!form.implementationStartTime) return "请选择作业实施开始时间"
    if (!String(form.safetyDisclosurePerson || "").trim()) return "请填写安全交底人"
    if (!String(form.guardian || "").trim()) return "请填写监护人"
    if (!String(form.disclosureReceiver || "").trim()) return "请填写接受交底人"
  }
  if (status === "IN_PROGRESS" && !form.implementationEndTime) return "请选择作业实施结束时间"
  if (status === "PENDING_ACCEPTANCE") {
    if (!String(form.completionAcceptor || "").trim()) return "请填写完工验收人"
    if (!form.completionAcceptanceTime) return "请选择完工验收时间"
  }
  return ""
}

Page({
  data: { id: "", record: null, form: {}, steps: [], nextStatus: "", actionLabel: "", dateTimes: {}, loading: false, saving: false, errorText: "" },

  async onLoad(options = {}) {
    this.setData({ id: options.id || "" })
    if (wx.setNavigationBarTitle) wx.setNavigationBarTitle({ title: "作业状态处理" })
    await this.loadDetail()
  },

  goBack() { wx.navigateBack() },

  async loadDetail() {
    if (!this.data.id) return
    this.setData({ loading: true, errorText: "" })
    try {
      const record = await getSpecialWorkRecord(this.data.id)
      const form = recordPayload(record)
      const target = nextStatus(record.status)
      if (record.status === "PENDING_APPROVAL" && !form.implementationStartTime) form.implementationStartTime = nowDateTime()
      if (record.status === "IN_PROGRESS" && !form.implementationEndTime) form.implementationEndTime = nowDateTime()
      if (record.status === "PENDING_ACCEPTANCE" && !form.completionAcceptanceTime) form.completionAcceptanceTime = nowDateTime()
      this.setData({ record: { ...record, statusClass: statusClass(record.status) }, form, steps: flowSteps(record.status), nextStatus: target, actionLabel: (workflowAction(record.status) || {}).label || "", dateTimes: this.dateTimes(form), loading: false })
    } catch (error) {
      this.setData({ loading: false, errorText: error && error.message ? error.message : "状态信息加载失败" })
    }
  },

  dateTimes(form) {
    return {
      implementationStartTime: dateTimeParts(form.implementationStartTime),
      implementationEndTime: dateTimeParts(form.implementationEndTime),
      completionAcceptanceTime: dateTimeParts(form.completionAcceptanceTime)
    }
  },

  updateField(event) {
    const field = event.currentTarget.dataset.field
    this.setData({ form: { ...this.data.form, [field]: event.detail.value }, errorText: "" })
  },

  changeDateTime(event) {
    const { field, part } = event.currentTarget.dataset
    const form = { ...this.data.form, [field]: mergeDateTimePart(this.data.form[field], part, event.detail.value) }
    this.setData({ form, dateTimes: this.dateTimes(form), errorText: "" })
  },

  async submitTransition() {
    if (this.data.saving || !this.data.nextStatus) return
    const errorText = transitionError(this.data.record.status, this.data.form)
    if (errorText) return this.setData({ errorText })
    this.setData({ saving: true, errorText: "" })
    try {
      const action = workflowAction(this.data.record.status)
      await executeSpecialWorkAction(this.data.id, action.action, actionPayload(this.data.record.status, this.data.form))
      this.setData({ saving: false })
      if (wx.showToast) wx.showToast({ title: "状态已更新", icon: "success" })
      await this.loadDetail()
    } catch (error) {
      this.setData({ saving: false, errorText: error && error.message ? error.message : "状态更新失败" })
    }
  }
})

module.exports = { ACTION_BY_STATUS: Object.fromEntries(Object.entries(WORKFLOW_ACTION_BY_STATUS).map(([status, item]) => [status, item.action])), ACTION_LABEL_BY_STATUS: Object.fromEntries(Object.entries(WORKFLOW_ACTION_BY_STATUS).map(([status, item]) => [status, item.label])), actionPayload, flowSteps, transitionError }
