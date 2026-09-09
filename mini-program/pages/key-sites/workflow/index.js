const { getRecord, getRecordWorkflow } = require("../../../services/mini-three-check")

const MODULE_KEY = "key-sites"

function linkedStatus(record) {
  const items = record && record.payload && Array.isArray(record.payload.checkItems) ? record.payload.checkItems : []
  const item = items.find(entry => entry.rectificationStatus)
  return item ? item.rectificationStatus : ""
}

function eventBy(workflow, patterns) {
  return workflow.find(item => {
    const text = `${item.action || ""} ${item.actionLabel || ""} ${item.remark || ""}`
    return patterns.some(pattern => text.includes(pattern))
  })
}

function formatWorkflowTime(value) {
  if (!value) return ""
  const normalized = String(value).replace("T", " ")
  const match = normalized.match(/^(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2}:\d{2})/)
  return match ? `${match[1]} ${match[2]}` : normalized
}

function eventMeta(event, fallbackActor, fallbackTime) {
  return {
    actor: (event && event.operatorName) || fallbackActor || "-",
    time: formatWorkflowTime((event && (event.occurredAt || event.createdAt)) || fallbackTime)
  }
}

function buildFlowSteps(record, workflow = []) {
  const payload = record.payload || {}
  const orderStatus = linkedStatus(record)
  const inspected = record.status === "OPENED" || record.status === "ARCHIVED"
  const hasRectification = Boolean(orderStatus)
  const accepting = ["RECTIFIED", "PENDING_ACCEPTANCE", "CLOSED"].includes(orderStatus)
  const closed = orderStatus === "CLOSED"
  const registeredEvent = eventBy(workflow, ["CREATE", "登记", "提交"])
  const inspectedEvent = eventBy(workflow, ["检查", "OPEN", "SUBMIT"])
  const rectifiedEvent = eventBy(workflow, ["整改", "RECTIFY"])
  const acceptedEvent = eventBy(workflow, ["验收", "ACCEPT", "CLOSE"])
  const registered = eventMeta(registeredEvent, payload.responsiblePerson || record.owner, record.createdAt || record.businessDate)
  const checked = eventMeta(inspectedEvent, payload.acceptancePerson || record.owner, record.updatedAt)
  const rectified = eventMeta(rectifiedEvent, payload.responsibleDepartment || payload.responsiblePerson, "")
  const accepted = eventMeta(acceptedEvent, payload.acceptancePerson, "")

  return [
    { key: "registered", index: 1, title: "登记检查", state: "done", actor: registered.actor, time: registered.time, note: `${registered.actor} 提交登记`, noteBox: true },
    { key: "checked", index: 2, title: "隐患确认", state: inspected ? "done" : "pending", actor: checked.actor, time: checked.time, note: inspected ? "现场检查已提交" : "待执行现场检查", noteBox: false },
    { key: "rectifying", index: 3, title: "整改跟进", state: hasRectification && !accepting ? "current" : (accepting ? "done" : "pending"), actor: rectified.actor, time: rectified.time, note: hasRectification ? "已关联统一隐患整改工单" : "无隐患，无需整改", noteBox: false },
    { key: "accepted", index: 4, title: "验收关闭", state: closed ? "done" : "pending", actor: accepted.actor, time: accepted.time, note: closed ? "整改工单已验收关闭" : (accepting ? "等待验收" : "待处理"), noteBox: false }
  ]
}

function currentNode(steps) {
  const current = steps.find(item => item.state === "current")
  if (current) return current.title
  const pending = steps.find(item => item.state === "pending")
  return pending ? pending.title : "已闭环"
}

Page({
  data: { id: "", selectedStep: 0, record: null, flowSteps: [], currentNode: "", loading: false, errorText: "" },

  async onLoad(options = {}) {
    this.setData({ id: options.id || "", selectedStep: Number(options.step) || 0 })
    if (wx.setNavigationBarTitle) wx.setNavigationBarTitle({ title: "单据流" })
    await this.loadWorkflow()
  },

  goBack() {
    wx.navigateBack()
  },

  async loadWorkflow() {
    if (!this.data.id) return
    this.setData({ loading: true, errorText: "" })
    try {
      const [record, response] = await Promise.all([
        getRecord(MODULE_KEY, this.data.id),
        getRecordWorkflow(MODULE_KEY, this.data.id)
      ])
      const flowSteps = buildFlowSteps(record, response.documentFlow || [])
      this.setData({ record, flowSteps, currentNode: currentNode(flowSteps), loading: false })
      if (this.data.selectedStep && wx.pageScrollTo) {
        const scrollToSelected = () => wx.pageScrollTo({ selector: `#flow-step-${this.data.selectedStep}`, duration: 200 })
        if (wx.nextTick) wx.nextTick(scrollToSelected)
        else scrollToSelected()
      }
    } catch (error) {
      this.setData({ loading: false, errorText: error && error.message ? error.message : "单据流加载失败" })
    }
  }
})

module.exports = { buildFlowSteps, currentNode, formatWorkflowTime }
