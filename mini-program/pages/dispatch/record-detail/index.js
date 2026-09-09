const { getRecord, getRecordWorkflow } = require("../../../services/mini-three-check")
const { getDispatchFlow } = require("../../../services/flow")

const MODULE_KEY = "team-dispatch"
const STAGES = [
  {
    stageKey: "teamDispatch",
    title: "班组派班",
    shortName: "班组派班",
    moduleKeys: ["team-dispatch", "curtain-wall-team-dispatch"],
    icon: "calendar",
    defaultStatus: "不生效",
    fields: [
      ["company", "公司"],
      ["department", "车间"],
      ["team", "班组"],
      ["teamTask", "班组任务"],
      ["dispatchType", "排班类型"],
      ["managerCount", "管理人员人数（手录）"],
      ["dispatchDate", "派班日期"],
      ["dispatchTime", "派班时间"]
    ]
  },
  {
    stageKey: "preShiftMeeting",
    title: "班前会",
    shortName: "班前会",
    moduleKeys: ["pre-shift-meeting"],
    icon: "usergroup",
    defaultStatus: "待开会议",
    fields: [
      ["company", "公司"],
      ["department", "部门"],
      ["team", "班组"],
      ["owner", "负责人"],
      ["attendees", "参会人"],
      ["date", "开会日期"],
      ["imageCheck", "图片打卡"],
      ["videoCheck", "视频打卡"]
    ]
  },
  {
    stageKey: "preShiftInspection",
    title: "班前检查",
    shortName: "班前检查",
    moduleKeys: ["pre-shift-inspection"],
    icon: "file-paste",
    defaultStatus: "待检查",
    fields: [
      ["company", "公司"],
      ["department", "车间"],
      ["team", "班组"],
      ["owner", "负责人"],
      ["date", "检查日期"],
      ["imageCheck", "图片打卡"]
    ]
  },
  {
    stageKey: "midShiftInspection",
    title: "班中检查",
    shortName: "班中检查",
    moduleKeys: ["mid-shift-inspection"],
    icon: "work",
    defaultStatus: "待检查",
    fields: [
      ["company", "公司"],
      ["department", "车间"],
      ["team", "班组"],
      ["owner", "负责人"],
      ["date", "检查日期"],
      ["imageCheck", "图片打卡"],
      ["videoCheck", "视频打卡"]
    ]
  },
  {
    stageKey: "postShiftInspection",
    title: "班后检查",
    shortName: "班后检查",
    moduleKeys: ["post-shift-inspection"],
    icon: "calendar",
    defaultStatus: "待检查",
    fields: [
      ["company", "公司"],
      ["department", "车间"],
      ["team", "班组"],
      ["owner", "负责人"],
      ["date", "检查日期"],
      ["imageCheck", "图片打卡"],
      ["handoverStatus", "交班状态"]
    ]
  }
]

function firstValue(...values) {
  return values.find(value => value !== undefined && value !== null && value !== "") || ""
}

function displayValue(value) {
  return value === undefined || value === null || value === "" || value === "-" ? "未填写" : String(value)
}

function stageByKey(stageKey) {
  return STAGES.find(stage => stage.stageKey === stageKey) || STAGES[0]
}

function moduleForStage(stage, record) {
  return firstValue(record && record.moduleKey, stage.moduleKey, stage.moduleKeys[0])
}

function cacheKey(moduleKey, id) {
  return `${moduleKey}:${id}`
}

function payloadOf(record = {}) {
  return record.payload || {}
}

function recordId(record = {}) {
  return firstValue(record.id, record.recordId)
}

function recordDate(record = {}) {
  const payload = payloadOf(record)
  return firstValue(record.businessDate, record.date, record.dispatchDate, payload.dispatchDate, payload.recordDate)
}

function recordValue(record = {}, field) {
  const payload = payloadOf(record)
  const directValues = {
    company: firstValue(record.company, record.companyName, payload.company, payload.companyName),
    department: firstValue(record.department, record.departmentName, payload.department, payload.departmentName),
    team: firstValue(record.team, record.teamName, payload.team, payload.teamName),
    owner: firstValue(record.owner, record.ownerName, payload.owner, payload.ownerName, payload.managerName),
    date: recordDate(record),
    teamTask: firstValue(record.teamTask, record.task, record.content, payload.teamTask, payload.task, payload.taskName, payload.content, payload.dispatchTask),
    dispatchType: firstValue(record.dispatchType, payload.dispatchType, payload.scheduleType),
    managerCount: firstValue(record.managerCount, record.managementCount, payload.managerCount, payload.managementCount),
    dispatchDate: firstValue(record.dispatchDate, payload.dispatchDate, recordDate(record)),
    dispatchTime: firstValue(record.dispatchTime, record.createdAt, record.createTime, payload.dispatchTime),
    attendees: firstValue(record.attendees, payload.attendees, payload.attendeesText, payload.attendeeNames),
    imageCheck: firstValue(record.imageCheck, payload.imageCheck, payload.imageUpload),
    videoCheck: firstValue(record.videoCheck, payload.videoCheck, payload.videoUpload),
    handoverStatus: firstValue(record.handoverStatus, payload.handoverStatus),
    status: firstValue(record.statusLabel, payload.statusLabel, record.status, payload.status)
  }
  return displayValue(firstValue(directValues[field], payload[field], record[field]))
}

function statusSource(record = {}) {
  const payload = payloadOf(record)
  return firstValue(record.statusLabel, payload.statusLabel, record.status, payload.status)
}

function businessStatus(stage, record) {
  const source = statusSource(record)
  if (stage.stageKey === "teamDispatch") {
    if (["OPENED", "ARCHIVED", "已提交", "已生效", "生效"].includes(source)) return "生效"
    if (["WITHDRAWN", "已撤回"].includes(source)) return "已撤回"
    return stage.defaultStatus
  }
  if (stage.stageKey === "preShiftMeeting") {
    return ["OPENED", "ARCHIVED", "已开会议", "已提交"].includes(source) ? "已开会议" : stage.defaultStatus
  }
  return ["OPENED", "ARCHIVED", "已检查", "已提交"].includes(source) ? "已检查" : stage.defaultStatus
}

function statusClass(label) {
  if (["生效", "已开会议", "已检查", "已完成"].includes(label)) return "success"
  if (["已撤回", "不生效", "未生效"].includes(label)) return "muted"
  return "warning"
}

function recordNoFor(stage, record, date) {
  const no = firstValue(record && record.recordNo, recordId(record))
  if (no) return String(no)
  const prefix = stage.stageKey === "teamDispatch" ? "TD"
    : stage.stageKey === "preShiftMeeting" ? "PSM"
      : stage.stageKey === "preShiftInspection" ? "PSI"
        : stage.stageKey === "midShiftInspection" ? "MSI"
          : "POSTSI"
  return `TCR-${prefix}-${(date || "0000-00-00").replace(/-/g, "")}-...`
}

function normalizeAttachment(attachment = {}) {
  const kind = String(firstValue(attachment.fileKind, attachment.kind, attachment.type)).toUpperCase()
  const url = firstValue(attachment.url, attachment.previewUrl, attachment.path, attachment.storagePath)
  return {
    ...attachment,
    id: firstValue(attachment.id, url, attachment.originalName),
    kind,
    isImage: kind === "IMAGE" || /\.(jpg|jpeg|png|gif|webp)$/i.test(url),
    isVideo: kind === "VIDEO" || /\.(mp4|mov|m4v|avi|webm)$/i.test(url),
    name: firstValue(attachment.originalName, attachment.name, url, "附件"),
    url,
    sizeText: attachment.fileSize ? `${Math.round(Number(attachment.fileSize) / 1024)}KB` : ""
  }
}

function workflowLogs(workflow = {}) {
  return (workflow.documentFlow || []).map(item => ({
    title: firstValue(item.actionLabel, item.actionName, item.action, "动作记录"),
    meta: `${firstValue(item.operatorName, item.operator, "系统管理员")} · ${firstValue(item.occurredAt, item.createdAt, item.time, "")}`,
    state: firstValue(item.toStatusLabel, item.statusLabel, item.toStatus, item.status, "")
  }))
}

function emptyNodeFields(stage, rootRecord) {
  return [
    { label: "节点", value: stage.title },
    { label: "状态", value: businessStatus(stage, {}) },
    { label: "公司", value: recordValue(rootRecord, "company") },
    { label: "车间", value: recordValue(rootRecord, "department") },
    { label: "班组", value: recordValue(rootRecord, "team") },
    { label: "业务日期", value: displayValue(recordDate(rootRecord)) },
    { label: "关联规则", value: "公司 + 部门 + 班组 + 业务日期" }
  ]
}

function nodeFields(stage, record, rootRecord) {
  if (!record) return emptyNodeFields(stage, rootRecord)
  return stage.fields.map(([field, label]) => ({ label, value: recordValue(record, field) }))
}

function buildNodes(rootRecord, flow, selectedStageKey) {
  const stages = Array.isArray(flow && flow.stages) ? flow.stages : []
  const stageMap = stages.reduce((map, stage = {}) => {
    if (stage.stageKey) map[stage.stageKey] = stage
    return map
  }, {})
  return STAGES.map((stage, index) => {
    const flowStage = stageMap[stage.stageKey] || {}
    const record = stage.stageKey === "teamDispatch" ? rootRecord : flowStage.record
    const date = firstValue(recordDate(record || {}), recordDate(rootRecord))
    const owner = recordValue(record || rootRecord, "owner")
    const statusLabel = record ? businessStatus(stage, record) : businessStatus(stage, {})
    return {
      ...stage,
      index: index + 1,
      moduleKey: moduleForStage(stage, record || flowStage),
      record: record || null,
      recordId: recordId(record || {}),
      recordNo: record ? recordNoFor(stage, record, date) : "暂无单据",
      date: displayValue(date),
      owner,
      active: stage.stageKey === selectedStageKey,
      empty: !record,
      statusLabel,
      statusClass: statusClass(statusLabel)
    }
  })
}

function buildViewModel(base, selectedStageKey, detailCache, workflowCache) {
  const nodes = buildNodes(base.rootRecord, base.flow, selectedStageKey)
  const selectedNode = nodes.find(node => node.stageKey === selectedStageKey) || nodes[0]
  const selectedDetail = selectedNode.recordId
    ? detailCache[cacheKey(selectedNode.moduleKey, selectedNode.recordId)] || selectedNode.record
    : null
  const workflow = selectedNode.recordId ? workflowCache[cacheKey(selectedNode.moduleKey, selectedNode.recordId)] || {} : {}
  const attachments = (selectedDetail && Array.isArray(selectedDetail.attachments) ? selectedDetail.attachments : []).map(normalizeAttachment)
  const doneCount = nodes.filter(node => node.statusClass === "success").length
  const pendingCount = nodes.length - doneCount
  const currentNode = nodes.find(node => node.statusClass !== "success") || selectedNode
  const status = businessStatus(STAGES[0], base.rootRecord)
  return {
    title: "一班三查全链路洞察",
    subtitle: `${recordValue(base.rootRecord, "team")} · ${displayValue(recordDate(base.rootRecord))}`,
    statusLabel: status,
    statusClass: statusClass(status),
    progress: `${Math.round((doneCount / nodes.length) * 100)}%`,
    currentNode: currentNode.title,
    pendingCount,
    owner: selectedNode.owner,
    attachmentCount: attachments.length,
    recordNo: displayValue(base.rootRecord.recordNo || recordId(base.rootRecord)),
    nodes,
    selectedStageKey,
    selectedNode,
    detailTitle: `${selectedNode.title}单据明细`,
    detailCells: nodeFields(selectedNode, selectedDetail || selectedNode.record, base.rootRecord),
    logs: workflowLogs(workflow),
    attachments
  }
}

Page({
  data: {
    id: "",
    companyKey: "gs",
    loading: false,
    errorText: "",
    dispatchDetail: null
  },

  async onLoad(options = {}) {
    this.detailBase = null
    this.detailCache = {}
    this.workflowCache = {}
    this.setData({
      id: options.id || "",
      companyKey: options.companyKey || "gs"
    })
    await this.loadDetail()
  },

  async loadDetail() {
    if (!this.data.id) return
    this.setData({ loading: true, errorText: "" })
    try {
      const rootRecord = await getRecord(MODULE_KEY, this.data.id)
      let flow = null
      try {
        flow = await getDispatchFlow(this.data.id)
      } catch (error) {
        flow = { stages: [] }
      }
      const rootKey = cacheKey(MODULE_KEY, this.data.id)
      this.detailBase = { rootRecord, flow }
      this.detailCache[rootKey] = rootRecord
      await this.loadNodeData("teamDispatch")
      this.setData({ loading: false })
    } catch (error) {
      this.setData({
        loading: false,
        errorText: error && error.message ? error.message : "链路详情加载失败"
      })
    }
  },

  async loadNodeData(stageKey) {
    if (!this.detailBase) return
    const selectedStageKey = stageKey || "teamDispatch"
    const initialModel = buildViewModel(this.detailBase, selectedStageKey, this.detailCache, this.workflowCache)
    this.setData({ dispatchDetail: initialModel })
    const node = initialModel.selectedNode
    if (!node || node.empty || !node.recordId) return
    const key = cacheKey(node.moduleKey, node.recordId)
    if (!this.detailCache[key]) {
      this.detailCache[key] = await getRecord(node.moduleKey, node.recordId)
    }
    if (!this.workflowCache[key]) {
      try {
        this.workflowCache[key] = await getRecordWorkflow(node.moduleKey, node.recordId)
      } catch (error) {
        this.workflowCache[key] = { documentFlow: [] }
      }
    }
    this.setData({
      dispatchDetail: buildViewModel(this.detailBase, selectedStageKey, this.detailCache, this.workflowCache)
    })
  },

  async selectFlowNode(event) {
    const stageKey = event.currentTarget.dataset.stageKey || "teamDispatch"
    await this.loadNodeData(stageKey)
  },

  previewAttachment(event) {
    const index = Number(event.currentTarget.dataset.index)
    const attachments = (this.data.dispatchDetail && this.data.dispatchDetail.attachments) || []
    const current = attachments[index]
    if (!current || !current.url || !current.isImage || !wx.previewImage) return
    wx.previewImage({
      current: current.url,
      urls: attachments.filter(item => item.isImage && item.url).map(item => item.url)
    })
  },

  goBack() {
    const pages = typeof getCurrentPages === "function" ? getCurrentPages() : []
    if (pages.length > 1) {
      wx.navigateBack()
      return
    }
    wx.navigateTo({ url: `/pages/dispatch/list/index?companyKey=${encodeURIComponent(this.data.companyKey)}` })
  },

  openOriginal() {
    if (!this.data.id) return
    wx.navigateTo({
      url: `/pages/dispatch/detail/index?id=${encodeURIComponent(this.data.id)}&companyKey=${encodeURIComponent(this.data.companyKey)}`
    })
  },

  exportDetail() {
    wx.showToast({ title: "导出能力待接入", icon: "none" })
  },

  showMoreActions() {
    wx.showToast({ title: "更多操作待接入", icon: "none" })
  }
})
