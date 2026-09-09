const {
  actionOrder,
  getOrder,
  uploadOrderAttachment
} = require("../../../services/hazard-rectification-order")
const { openRectificationOrder } = require("../../../services/mini-three-check")

Page({
  data: {
    id: "",
    moduleKey: "",
    order: null,
    sourceRecordId: "",
    loading: false,
    actioning: false,
    afterPhoto: "",
    deadlineDate: "",
    deadlineTime: "",
    errorText: "",
    navStatusBarHeight: 20,
    navBarHeight: 44,
    navHeight: 64,
    navTitleLeft: 104,
    navTitleRight: 104,
    actionForm: {
      rectificationResponsibleUserId: "",
      rectificationDepartmentId: "",
      rectificationRequirement: "",
      rectificationDeadline: "",
      rectificationDescription: "",
      acceptanceUserId: "",
      acceptanceDepartmentId: "",
      acceptanceRemark: "",
      rejectReason: "",
      cancelReason: ""
    }
  },

  async onLoad(options = {}) {
    this.initCustomNavigation()
    this.setData({
      id: options.id || "",
      moduleKey: options.moduleKey || "",
      sourceRecordId: options.sourceRecordId || ""
    })
    await this.loadOrder()
  },

  initCustomNavigation() {
    if (typeof wx === "undefined") return
    const systemInfo = wx.getSystemInfoSync ? wx.getSystemInfoSync() : {}
    const statusBarHeight = systemInfo.statusBarHeight || 20
    const windowWidth = systemInfo.windowWidth || 375
    let menuButton = null
    if (wx.getMenuButtonBoundingClientRect) {
      menuButton = wx.getMenuButtonBoundingClientRect()
    }
    const menuTop = menuButton && menuButton.top ? menuButton.top : statusBarHeight + 4
    const menuHeight = menuButton && menuButton.height ? menuButton.height : 32
    const navBarHeight = (menuTop - statusBarHeight) * 2 + menuHeight
    const menuLeft = menuButton && menuButton.left ? menuButton.left : windowWidth - 96
    const titleRight = Math.max(windowWidth - menuLeft + 10, 104)
    this.setData({
      navStatusBarHeight: statusBarHeight,
      navBarHeight,
      navHeight: statusBarHeight + navBarHeight,
      navTitleLeft: titleRight,
      navTitleRight: titleRight
    })
  },

  async loadOrder() {
    if (!this.data.id && !(this.data.moduleKey && this.data.sourceRecordId)) return
    this.setData({ loading: true, errorText: "" })
    try {
      const order = this.data.moduleKey && this.data.sourceRecordId
        ? await openRectificationOrder(this.data.moduleKey, this.data.sourceRecordId)
        : await getOrder(this.data.id)
      this.setData({ id: order && order.id ? order.id : this.data.id, order: this.normalizeOrder(order), loading: false })
    } catch (error) {
      this.setData({
        loading: false,
        errorText: error && error.message ? error.message : "整改单加载失败"
      })
    }
  },

  updateActionField(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const field = dataset.field
    if (!field) return
    this.setData({
      actionForm: {
        ...this.data.actionForm,
        [field]: event.detail ? event.detail.value : ""
      }
    })
  },

  displayText(value) {
    return value === null || value === undefined ? "" : value
  },

  sourceText(order = {}) {
    if (order.sourceModuleName) return order.sourceModuleName
    const sourceMap = {
      HAZARD: "隐患排查",
      HAZARD_SOURCE: "隐患排查",
      SAFETY_CHECK: "安全检查",
      THREE_CHECK: "一班三查",
      PRE_SHIFT_MEETING: "班前会",
      PRE_SHIFT_CHECK: "班前检查",
      MID_SHIFT_CHECK: "班中检查",
      POST_SHIFT_CHECK: "班后检查",
      SNAPSHOT: "随手拍",
      "safety-check": "安全检查",
      "quick-shot": "随手拍",
      "pre-shift-inspection": "班前检查",
      "mid-shift-inspection": "班中检查",
      "post-shift-inspection": "班后检查"
    }
    return sourceMap[order.sourceModuleKey] || this.displayText(order.sourceModuleKey)
  },

  detailTitle(order = {}) {
    const titleMap = {
      PENDING_ASSIGN: "待派发整改",
      PENDING_RECTIFY: "待整改工单",
      RECTIFIED: "申请验收",
      PENDING_ACCEPTANCE: "待验收工单",
      CLOSED: "已闭环工单",
      CANCELLED: "已作废工单"
    }
    return titleMap[order.status] || "隐患整改工单"
  },

  normalizeOrder(order) {
    if (!order) return order
    const normalized = { ...order }
    const sourceModuleText = this.sourceText(order)
    const detailTitle = order.status ? this.detailTitle(order) : ""
    const teamText = this.displayText(order.team || order.teamName || order.teamText || order.teamLabel || order.teamDisplayName)
    const hazardCountText = this.displayText(order.hazardCount)
    const rectificationRequirementText = this.displayText(order.rectificationRequirement)
    const rectificationDeadlineText = this.displayText(this.formatFlowTime(order.rectificationDeadline))
    if (sourceModuleText) normalized.sourceModuleText = sourceModuleText
    if (detailTitle) normalized.detailTitle = detailTitle
    if (teamText) normalized.teamText = teamText
    if (hazardCountText) normalized.hazardCountText = hazardCountText
    if (rectificationRequirementText) normalized.rectificationRequirementText = rectificationRequirementText
    if (rectificationDeadlineText) normalized.rectificationDeadlineText = rectificationDeadlineText
    if (Array.isArray(order.flowLogs)) {
      normalized.flowLogs = this.normalizeFlowLogs(order.flowLogs, order.status)
    }
    if (order.status === "CLOSED" || order.status === "CANCELLED") {
      normalized.flowDetailSteps = this.buildFlowDetailSteps(normalized)
    }
    return normalized
  },

  textOrDash(value) {
    const text = this.displayText(value)
    return text || "-"
  },

  findFlowLog(flowLogs = [], keywords = []) {
    return flowLogs.find((log = {}) => {
      const text = `${log.actionLabel || ""}${log.action || ""}${log.remark || ""}`
      return keywords.some((keyword) => text.indexOf(keyword) !== -1)
    }) || null
  },

  shortFlowTime(value) {
    const text = this.formatFlowTime(value)
    if (!text) return "-"
    const matched = text.match(/^\d{4}-(\d{2})-(\d{2})\s+(\d{2}:\d{2})/)
    return matched ? `${matched[1]}-${matched[2]} ${matched[3]}` : text
  },

  firstHazardItem(order = {}) {
    return Array.isArray(order.items) && order.items.length > 0 ? order.items[0] : {}
  },

  buildFlowDetailSteps(order = {}) {
    const flowLogs = Array.isArray(order.flowLogs) ? order.flowLogs : []
    const hazard = this.firstHazardItem(order)
    const foundLog = this.findFlowLog(flowLogs, ["发现隐患", "创建工单", "自动生成"]) || flowLogs[0] || {}
    const issueLog = this.findFlowLog(flowLogs, ["下发整改", "派发整改"])
    const rectifyLog = this.findFlowLog(flowLogs, ["提交整改", "已整改", "完成整改"])
    const requestLog = this.findFlowLog(flowLogs, ["申请验收", "待验收"])
    const acceptLog = this.findFlowLog(flowLogs, ["验收通过", "已闭环", "闭环"])
    const cancelLog = this.findFlowLog(flowLogs, ["作废"])
    const afterPhoto = order.rectificationAfterPhoto || order.afterPhoto || ""
    const steps = [
      {
        key: "found",
        icon: "eye",
        title: "发现隐患",
        time: this.shortFlowTime(foundLog.createdAt),
        rows: [
          [
            { label: "检查项", value: this.textOrDash(hazard.checkItem) }
          ],
          [
            { label: "隐患描述", value: this.textOrDash(hazard.hazardDescription) }
          ],
          [
            { label: "整改状态", value: this.textOrDash(hazard.rectificationStatusLabel || order.statusLabel || order.status) }
          ]
        ]
      },
      {
        key: "issue",
        icon: "doc",
        title: "下发整改",
        time: this.shortFlowTime(issueLog && issueLog.createdAt),
        rows: [
          [
            { label: "整改责任人", value: this.textOrDash(order.rectificationResponsibleUserId || order.rectificationResponsiblePerson) },
            { label: "整改部门", value: this.textOrDash(order.rectificationDepartmentId || order.rectificationDepartmentName) }
          ],
          [
            { label: "验收人", value: this.textOrDash(order.acceptanceUserId || order.acceptanceUserName) },
            { label: "整改要求", value: this.textOrDash(order.rectificationRequirement) }
          ],
          [
            { label: "整改日期", value: this.textOrDash((order.rectificationDeadlineText || "").split(" ")[0]) },
            { label: "选择整改时间", value: this.textOrDash((order.rectificationDeadlineText || "").split(" ")[1]) }
          ]
        ]
      },
      {
        key: "rectify",
        icon: "upload",
        title: "提交整改",
        time: this.shortFlowTime(rectifyLog && rectifyLog.createdAt),
        photos: afterPhoto ? [afterPhoto] : [],
        rows: [
          [
            { label: "整改说明", value: this.textOrDash(order.rectificationDescription) }
          ],
          [
            { label: "选择整改后照片", value: afterPhoto ? "是" : "-" },
            { label: "已选择整改后照片", value: afterPhoto ? "1张" : "-" }
          ],
          [
            { label: "整改后照片预览", value: "" }
          ]
        ]
      },
      {
        key: "request",
        icon: "user",
        title: "申请验收",
        time: this.shortFlowTime(requestLog && requestLog.createdAt),
        rows: [
          [
            { label: "验收人", value: this.textOrDash(order.acceptanceUserId || order.acceptanceUserName) },
            { label: "验收部门", value: this.textOrDash(order.acceptanceDepartmentId || order.acceptanceDepartmentName) }
          ],
          [
            { label: "验收申请备注", value: this.textOrDash(order.acceptanceRemark || (requestLog && requestLog.remark)) }
          ]
        ]
      },
      {
        key: "accept",
        icon: "check",
        title: "验收处理",
        time: this.shortFlowTime(acceptLog && acceptLog.createdAt),
        rows: [
          [
            { label: "验收人", value: this.textOrDash(order.acceptanceUserId || order.acceptanceUserName) }
          ],
          [
            { label: "验收意见", value: this.textOrDash(order.acceptanceRemark || (acceptLog && acceptLog.remark)) }
          ],
          [
            { label: "验收通过", value: order.status === "CLOSED" ? "是" : "-" },
            { label: "驳回验收", value: "否" }
          ],
          [
            { label: "驳回原因", value: "-" }
          ]
        ]
      }
    ]
    if (order.status === "CANCELLED" || cancelLog) {
      steps.push({
        key: "cancel",
        icon: "close",
        title: "作废",
        time: this.shortFlowTime(cancelLog && cancelLog.createdAt),
        rows: [
          [
            { label: "作废原因", value: this.textOrDash(order.cancelReason || (cancelLog && cancelLog.remark)) }
          ]
        ]
      })
    }
    return steps
  },

  normalizeFlowLogs(flowLogs = [], status = "") {
    const keywordMap = {
      PENDING_ASSIGN: ["发现隐患", "创建工单", "自动生成", "待下发", "待派发"],
      PENDING_RECTIFY: ["下发整改", "派发整改", "待整改", "确认接收"],
      RECTIFIED: ["提交整改", "已整改", "申请验收"],
      PENDING_ACCEPTANCE: ["申请验收", "待验收", "提交整改"],
      CLOSED: ["验收通过", "已闭环", "闭环"],
      CANCELLED: ["作废"]
    }
    const keywords = keywordMap[status] || []
    let currentIndex = -1
    if (keywords.length > 0) {
      currentIndex = flowLogs.findIndex((log = {}) => {
        const text = `${log.actionLabel || ""}${log.action || ""}${log.remark || ""}`
        return keywords.some((keyword) => text.indexOf(keyword) !== -1)
      })
    }
    if (currentIndex < 0 && status === "PENDING_ASSIGN" && flowLogs.length > 0) {
      currentIndex = 0
    }
    return flowLogs.map((log, index) => ({
      ...log,
      createdAtText: this.formatFlowTime(log && log.createdAt),
      isCurrent: index === currentIndex
    }))
  },

  formatFlowTime(value) {
    if (!value) return ""
    const text = String(value).replace("T", " ")
    const matched = text.match(/^(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})/)
    return matched ? `${matched[1]} ${matched[2]}` : text
  },

  updateDeadlineDate(event = {}) {
    const date = event.detail ? event.detail.value : ""
    const currentTime = this.data.deadlineTime || "18:00"
    this.setDeadline(date, currentTime)
  },

  updateDeadlineTime(event = {}) {
    const time = event.detail ? event.detail.value : ""
    const currentDate = this.data.deadlineDate
    this.setDeadline(currentDate, time)
  },

  setDeadline(date, time) {
    const value = date && time ? `${date} ${time}:00` : ""
    this.setData({
      deadlineDate: date || "",
      deadlineTime: time || "",
      actionForm: {
        ...this.data.actionForm,
        rectificationDeadline: value
      }
    })
  },

  chooseAfterPhoto() {
    if (wx.chooseMedia) {
      wx.chooseMedia({
        count: 1,
        mediaType: ["image"],
        sourceType: ["album", "camera"],
        success: result => {
          const file = (result.tempFiles || []).find(item => item && item.tempFilePath)
          if (file) {
            this.setData({ afterPhoto: file.tempFilePath })
          }
        }
      })
      return
    }
    if (wx.chooseImage) {
      wx.chooseImage({
        count: 1,
        sourceType: ["album", "camera"],
        success: result => {
          const filePath = result.tempFilePaths && result.tempFilePaths[0]
          if (filePath) {
            this.setData({ afterPhoto: filePath })
          }
        }
      })
    }
  },

  previewAfterPhoto() {
    const current = this.data.afterPhoto || (this.data.order && this.data.order.rectificationAfterPhoto)
    if (!current || !wx.previewImage) return
    wx.previewImage({ current, urls: [current] })
  },

  showActionToast(message) {
    if (wx.showToast) {
      wx.showToast({ title: message, icon: "none" })
    }
  },

  optionalNumber(value) {
    return value === "" || value === undefined || value === null ? undefined : Number(value)
  },

  async runOrderAction(action, payload) {
    if (!this.data.order || this.data.actioning) return
    this.setData({ actioning: true })
    try {
      const order = await actionOrder(this.data.id, {
        action,
        version: this.data.order.version || 0,
        payload
      })
      this.setData({ order: this.normalizeOrder(order), actioning: false })
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

  issueRectification() {
    const form = this.data.actionForm
    if (!form.rectificationResponsibleUserId || !form.rectificationRequirement) {
      this.showActionToast("请填写责任人与整改要求")
      return Promise.resolve()
    }
    return this.runOrderAction("ISSUE_RECTIFICATION", {
      rectificationResponsibleUserId: Number(form.rectificationResponsibleUserId),
      rectificationDepartmentId: this.optionalNumber(form.rectificationDepartmentId),
      acceptanceUserId: this.optionalNumber(form.acceptanceUserId),
      acceptanceDepartmentId: this.optionalNumber(form.acceptanceDepartmentId),
      rectificationRequirement: form.rectificationRequirement,
      rectificationDeadline: form.rectificationDeadline
    })
  },

  async markRectified() {
    const form = this.data.actionForm
    let afterPhotoUrl = this.data.order && this.data.order.rectificationAfterPhoto
    if (this.data.afterPhoto) {
      const uploaded = await uploadOrderAttachment(this.data.id, "RECTIFICATION_AFTER_PHOTO", this.data.afterPhoto)
      afterPhotoUrl = uploaded && uploaded.url
    }
    if (!afterPhotoUrl) {
      this.showActionToast("请选择整改后照片")
      return Promise.resolve()
    }
    return this.runOrderAction("MARK_RECTIFIED", {
      rectificationDescription: form.rectificationDescription,
      afterPhoto: afterPhotoUrl
    })
  },

  requestAcceptance() {
    const form = this.data.actionForm
    return this.runOrderAction("REQUEST_ACCEPTANCE", {
      acceptanceUserId: this.optionalNumber(form.acceptanceUserId),
      acceptanceDepartmentId: this.optionalNumber(form.acceptanceDepartmentId),
      acceptanceRemark: form.acceptanceRemark
    })
  },

  acceptOrder() {
    const form = this.data.actionForm
    return this.runOrderAction("ACCEPT", {
      acceptanceUserId: this.optionalNumber(form.acceptanceUserId),
      acceptanceRemark: form.acceptanceRemark
    })
  },

  rejectAcceptance() {
    return this.runOrderAction("REJECT_ACCEPTANCE", {
      rejectReason: this.data.actionForm.rejectReason
    })
  },

  cancelOrder() {
    return this.runOrderAction("CANCEL", {
      cancelReason: this.data.actionForm.cancelReason
    })
  },

  goBack() {
    wx.navigateBack()
  }
})
