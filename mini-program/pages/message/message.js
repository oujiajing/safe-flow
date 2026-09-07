const notificationService = require("../../services/notification")
const { getNotificationResolvedRoute } = require("../../utils/notification-routes")
const { syncTabBarSelected } = require("../../utils/tab-bar")

const GROUPS = [
  { value: "ACTION", label: "待我处理", unreadCount: 0 },
  { value: "BUSINESS", label: "业务通知", unreadCount: 0 },
  { value: "SYSTEM", label: "系统公告", unreadCount: 0 }
]

const DEFAULT_MODULE_OPTIONS = [
  { value: "", label: "全部模块" },
  { value: "pre-shift-meeting", label: "班前会" },
  { value: "pre-shift-inspection", label: "班前检查" },
  { value: "mid-shift-inspection", label: "班中检查" },
  { value: "post-shift-inspection", label: "班后检查" },
  { value: "hazard-rectification", label: "隐患整改" },
  { value: "special-work", label: "特殊作业" }
]

const MODULE_VISUALS = {
  "pre-shift-meeting": { icon: "chat", tone: "green" },
  "pre-shift-inspection": { icon: "assignment-checked", tone: "amber" },
  "mid-shift-inspection": { icon: "task-checked", tone: "blue" },
  "post-shift-inspection": { icon: "check-circle", tone: "teal" },
  "hazard-rectification": { icon: "shield-error", tone: "green" },
  "special-work": { icon: "tools", tone: "orange" },
  "team-dispatch": { icon: "calendar", tone: "blue" },
  "hazard-source": { icon: "flag", tone: "amber" },
  "safety-exam": { icon: "edit", tone: "green" },
  "safety-learning": { icon: "book", tone: "teal" },
  "risk-control": { icon: "chart", tone: "orange" },
  "key-site": { icon: "location", tone: "blue" },
  "safety-points": { icon: "star", tone: "amber" },
  "safety-ledger": { icon: "file", tone: "teal" },
  "system-account": { icon: "user", tone: "blue" },
  "system-import": { icon: "upload", tone: "teal" },
  "system-announcement": { icon: "notification", tone: "green" }
}

function emptyCounts() {
  return {
    pending: 0,
    unread: 0,
    overdue: 0,
    groupUnreadCounts: { ACTION: 0, BUSINESS: 0, SYSTEM: 0 }
  }
}

function formatTime(value) {
  if (!value) return ""
  const normalized = String(value).replace(" ", "T")
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 16).replace("T", " ")
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = String(date.getHours()).padStart(2, "0")
  const minute = String(date.getMinutes()).padStart(2, "0")
  return `${month}月${day}日 ${hour}：${minute}`
}

function normalizeMessage(message) {
  const visual = MODULE_VISUALS[message.moduleKey] || { icon: "notification", tone: "blue" }
  const cardTitle = conciseTitle(message.title, message.moduleName, message.groupType)
  return {
    ...message,
    read: Boolean(message.read),
    cardTitle,
    timeLabel: formatTime(message.createdAt),
    deadlineLabel: message.deadline ? String(message.deadline).slice(0, 16).replace("T", " ") : "",
    icon: visual.icon,
    tone: visual.tone,
    statusLabel: message.overdue
      ? "已逾期"
      : (message.actionStatus || message.handlingStatus) === "PENDING"
        ? (message.actionLabel || "待处理").replace(/^去/, "待")
        : message.read ? "已读" : "未读"
  }
}

function conciseTitle(title, moduleName, groupType) {
  const text = String(title || "业务消息")
  if (text.includes("被催办")) return "任务被催办，请尽快处理"
  if (moduleName && text.startsWith(moduleName)) {
    const concise = text.slice(moduleName.length).replace(/^[:：\s]+/, "")
    if (
      groupType === "ACTION"
      && /^(?:(?:任务)?待(?:你)?(?:执行|审批|验收|审核|检查|处理)|(?:任务|内容|检查)?已(?:下发|分派|逾期)|即将截止)$/.test(concise)
    ) return ""
    if (concise.length >= 4) return concise
  }
  return text
}

Page({
  data: {
    viewMode: "list",
    groups: GROUPS,
    activeGroup: "ACTION",
    moduleOptions: DEFAULT_MODULE_OPTIONS,
    moduleIndex: 0,
    filterDate: "",
    counts: emptyCounts(),
    messages: [],
    activeMessage: null,
    loading: false,
    loadingMore: false,
    errorText: "",
    page: 1,
    pageSize: 20,
    total: 0,
    hasMore: false
  },

  async onShow() {
    syncTabBarSelected(this, "pages/message/message")
    if (this.data.viewMode === "detail" && this.data.activeMessage) return
    await this.refreshPage()
  },

  async onPullDownRefresh() {
    await this.refreshPage()
    if (wx.stopPullDownRefresh) wx.stopPullDownRefresh()
  },

  async onReachBottom() {
    await this.loadMore()
  },

  async refreshPage() {
    this.setData({ loading: true, errorText: "", page: 1 })
    let counts = null
    let pageData = null
    let countsError = null
    let listError = null
    try {
      counts = await notificationService.getUnreadCounts()
    } catch (error) {
      countsError = error
    }
    try {
      pageData = await notificationService.listNotifications(this.query(1))
    } catch (error) {
      listError = error
    }
    if (counts || pageData) {
      pageData = pageData || { items: this.data.messages, total: this.data.total }
      const messages = (pageData.items || []).map(normalizeMessage)
      const normalizedCounts = counts || emptyCounts()
      const dynamicModules = normalizedCounts.visibleCategories
        ? [{ value: "", label: "全部模块" }].concat(
            Object.keys(normalizedCounts.visibleCategories).map(value => ({
              value,
              label: normalizedCounts.visibleCategories[value]
            }))
          )
        : this.data.moduleOptions
      this.setData({
        counts: normalizedCounts,
        groups: GROUPS.map(item => ({
          ...item,
          unreadCount: Number(normalizedCounts.groupUnreadCounts[item.value] || 0)
        })),
        messages,
        moduleOptions: dynamicModules,
        total: Number(pageData.total || 0),
        hasMore: messages.length < Number(pageData.total || 0),
        loading: false,
        errorText: listError ? (listError.message || "消息列表加载失败，请重试") : ""
      })
      if (countsError) wx.showToast({ title: "消息统计加载失败", icon: "none" })
    } else {
      this.setData({
        loading: false,
        errorText: (listError && listError.message) || (countsError && countsError.message) || "消息加载失败"
      })
    }
  },

  async loadMore() {
    if (this.data.loading || this.data.loadingMore || !this.data.hasMore) return
    const page = this.data.page + 1
    this.setData({ loadingMore: true })
    try {
      const pageData = await notificationService.listNotifications(this.query(page))
      const messages = this.data.messages.concat((pageData.items || []).map(normalizeMessage))
      this.setData({
        page,
        messages,
        total: Number(pageData.total || 0),
        hasMore: messages.length < Number(pageData.total || 0),
        loadingMore: false
      })
    } catch (error) {
      this.setData({ loadingMore: false })
      wx.showToast({ title: error && error.message ? error.message : "加载失败", icon: "none" })
    }
  },

  query(page) {
    const moduleOption = this.data.moduleOptions[this.data.moduleIndex] || DEFAULT_MODULE_OPTIONS[0]
    return {
      groupType: this.data.activeGroup,
      moduleKey: moduleOption.value || undefined,
      dateStart: this.data.filterDate || undefined,
      dateEnd: this.data.filterDate || undefined,
      page,
      pageSize: this.data.pageSize
    }
  },

  async changeGroup(event) {
    const group = event.currentTarget.dataset.group
    if (!group || group === this.data.activeGroup) return
    this.setData({ activeGroup: group })
    await this.refreshPage()
  },

  async changeModule(event) {
    this.setData({ moduleIndex: Number(event.detail.value || 0) })
    await this.refreshPage()
  },

  async changeDate(event) {
    this.setData({ filterDate: event.detail.value || "" })
    await this.refreshPage()
  },

  async clearDate() {
    if (!this.data.filterDate) return
    this.setData({ filterDate: "" })
    await this.refreshPage()
  },

  markAllRead() {
    if (!this.data.counts.unread) return
    wx.showModal({
      title: "全部已读",
      content: "将当前分组中的消息全部标记为已读？",
      success: async result => {
        if (!result.confirm) return
        const moduleOption = this.data.moduleOptions[this.data.moduleIndex] || DEFAULT_MODULE_OPTIONS[0]
        try {
          await notificationService.markAllNotificationsRead({
            groupType: this.data.activeGroup,
            moduleKey: moduleOption.value || undefined,
            dateStart: this.data.filterDate || undefined,
            dateEnd: this.data.filterDate || undefined
          })
          await this.refreshPage()
        } catch (error) {
          wx.showToast({ title: error && error.message ? error.message : "操作失败", icon: "none" })
        }
      }
    })
  },

  async openMessage(event) {
    const id = String(event.currentTarget.dataset.id || "")
    const message = this.data.messages.find(item => String(item.id) === id)
    if (!message) return
    if (!message.read) {
      try {
        await notificationService.markNotificationRead(message.id)
      } catch (error) {
        wx.showToast({ title: "已读状态同步失败", icon: "none" })
      }
    }
    const activeMessage = normalizeMessage({ ...message, read: true })
    this.setData({ viewMode: "detail", activeMessage })
  },

  backToList() {
    this.setData({ viewMode: "list", activeMessage: null })
    this.refreshPage()
  },

  handleHeaderBack() {
    if (this.data.viewMode === "detail") {
      this.backToList()
      return
    }
    this.goBack()
  },

  async handleAction(event) {
    const id = String(event.currentTarget.dataset.id || "")
    const message = this.data.activeMessage && String(this.data.activeMessage.id) === id
      ? this.data.activeMessage
      : this.data.messages.find(item => String(item.id) === id)
    if (!message) return
    if (!message.read) {
      try {
        await notificationService.markNotificationRead(message.id)
      } catch (error) {
        // Business navigation remains available if read-state synchronization fails.
      }
    }
    let resolution
    try {
      resolution = await notificationService.resolveNotificationAction(message.id)
    } catch (error) {
      wx.showToast({ title: error && error.message ? error.message : "无法校验当前操作", icon: "none" })
      return
    }
    if (!resolution.actionAvailable) {
      wx.showToast({ title: resolution.actionUnavailableReason || "当前不可操作", icon: "none" })
      await this.refreshPage()
      return
    }
    const route = getNotificationResolvedRoute(resolution.routeKey, resolution.routeParams)
    if (!route) {
      wx.showToast({ title: "该消息暂无可跳转的业务页面", icon: "none" })
      return
    }
    wx.navigateTo({ url: route })
  },

  goBack() {
    const pages = getCurrentPages()
    if (pages.length > 1 && wx.navigateBack) {
      wx.navigateBack()
      return
    }
    wx.reLaunch({ url: "/pages/home/home" })
  }
})
