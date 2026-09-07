const { getCurrentUser, getToken } = require("../../utils/auth")
const { LOGIN_PAGE } = require("../../utils/auth-flow")
const { syncTabBarSelected } = require("../../utils/tab-bar")
const env = require("../../config/env")
const meService = require("../../services/me")

const WORK_ENTRIES = [
  { key: "dispatchRecords", type: "dispatch", label: "派班记录", iconPath: "/assets/mine-icons/dispatch-record.webp" },
  { key: "threeCheckRecords", type: "three-check", label: "三查记录", iconPath: "/assets/mine-icons/three-check-record.webp" },
  { key: "hazardReports", type: "hazard-report", label: "隐患上报", iconPath: "/assets/mine-icons/hazard-report.webp" },
  { key: "rectificationRecords", type: "rectification", label: "整改记录", iconPath: "/assets/mine-icons/rectification-record.webp" }
]

const GROWTH_ENTRIES = [
  { key: "learningRecords", type: "learning", label: "学习记录", iconPath: "/assets/mine-icons/learning-record.webp" },
  { key: "examRecords", type: "exam", label: "考试记录", iconPath: "/assets/mine-icons/exam-record.webp" },
  { key: "pointFlow", type: "points", label: "积分明细", iconPath: "/assets/mine-icons/points-detail.webp" }
]

function text(value) {
  return value === undefined || value === null ? "" : String(value).trim()
}

function avatarText(profile = {}) {
  const name = text(profile.realName || profile.username)
  return name ? name.slice(0, 1) : "我"
}

function avatarUrl(value) {
  const url = text(value)
  if (!url || /^(https?:|wxfile:|data:)/.test(url)) return url
  return `${env.baseUrl}${url.startsWith("/") ? url : `/${url}`}`
}

function organizationText(profile = {}) {
  return [profile.companyName, profile.departmentName, profile.teamName]
    .map(text)
    .filter(Boolean)
    .join(" · ") || "组织信息未维护"
}

function cachedProfile(user = {}) {
  return {
    username: text(user.username),
    realName: text(user.realName || user.name || user.username) || "平安班组用户",
    avatar: text(user.avatar),
    positionName: text(user.positionName) || "岗位未维护",
    companyName: text(user.companyName),
    departmentName: text(user.departmentName || user.organizationName),
    teamName: text(user.teamName),
    roleNames: Array.isArray(user.roles) ? user.roles : []
  }
}

function entriesFor(capabilities = {}, entries) {
  return entries.filter(item => capabilities[item.key] !== false)
}

function normalizeOverview(overview = {}, fallback = {}) {
  const profile = { ...fallback, ...(overview.profile || {}) }
  const summary = overview.safetySummary || {}
  const capabilities = overview.capabilities || {}
  return {
    profile,
    avatarText: avatarText(profile),
    avatarUrl: avatarUrl(profile.avatar),
    organizationText: organizationText(profile),
    summary: {
      points: Number(summary.points || 0),
      completedLearningCount: Number(summary.completedLearningCount || 0),
      passedExamCount: Number(summary.passedExamCount || 0)
    },
    workEntries: entriesFor(capabilities, WORK_ENTRIES),
    growthEntries: entriesFor(capabilities, GROWTH_ENTRIES)
  }
}

Page({
  data: {
    navHeight: 64,
    loggedIn: false,
    loading: false,
    errorText: "",
    profile: cachedProfile(),
    avatarText: "我",
    avatarUrl: "",
    organizationText: "登录后查看个人安全档案",
    summary: {
      points: "--",
      completedLearningCount: "--",
      passedExamCount: "--"
    },
    workEntries: WORK_ENTRIES,
    growthEntries: GROWTH_ENTRIES
  },

  onLoad() {
    this.setupNavigationMetrics()
  },

  async onShow() {
    syncTabBarSelected(this, "pages/mine/mine")
    const loggedIn = Boolean(getToken())
    if (!loggedIn) {
      this.setData({
        loggedIn: false,
        loading: false,
        errorText: "",
        profile: { realName: "未登录", positionName: "" },
        avatarText: "我",
        avatarUrl: "",
        organizationText: "登录后查看个人安全档案",
        summary: { points: "--", completedLearningCount: "--", passedExamCount: "--" }
      })
      return
    }

    const fallback = cachedProfile(getCurrentUser() || {})
    const seeded = normalizeOverview({}, fallback)
    this.setData({ loggedIn: true, ...seeded })
    await this.loadOverview(fallback)
  },

  async onPullDownRefresh() {
    if (getToken()) {
      await this.loadOverview(cachedProfile(getCurrentUser() || {}))
    }
    if (wx.stopPullDownRefresh) wx.stopPullDownRefresh()
  },

  async loadOverview(fallback = {}) {
    this.setData({ loading: true, errorText: "" })
    try {
      const overview = await meService.getMineOverview()
      this.setData({ ...normalizeOverview(overview, fallback), loading: false })
    } catch (error) {
      this.setData({
        loading: false,
        errorText: error && error.message ? error.message : "个人信息加载失败"
      })
    }
  },

  setupNavigationMetrics() {
    const systemInfo = wx.getSystemInfoSync ? wx.getSystemInfoSync() : {}
    const menuButton = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null
    const statusBarHeight = Number(systemInfo.statusBarHeight) || 20
    const navigationBarHeight = menuButton
      && Number.isFinite(menuButton.top)
      && Number.isFinite(menuButton.height)
      ? Math.max(menuButton.height, (menuButton.top - statusBarHeight) * 2 + menuButton.height)
      : 44
    this.setData({ navHeight: statusBarHeight + navigationBarHeight })
  },

  openSettings() {
    wx.navigateTo({ url: "/pages/settings/index" })
  },

  openProfile() {
    if (!this.data.loggedIn) {
      wx.navigateTo({ url: LOGIN_PAGE })
      return
    }
    wx.navigateTo({ url: "/pages/profile/index" })
  },

  openMetric(event) {
    const type = event.currentTarget.dataset.type
    const entry = this.data.growthEntries.find(item => item.type === type)
    if (entry) this.openRecordEntry({ currentTarget: { dataset: entry } })
  },

  openRecordEntry(event) {
    if (!this.data.loggedIn) {
      wx.navigateTo({ url: LOGIN_PAGE })
      return
    }
    const { type, label } = event.currentTarget.dataset
    if (!type) return
    wx.navigateTo({
      url: `/pages/mine-records/index?type=${encodeURIComponent(type)}&title=${encodeURIComponent(label || "我的记录")}`
    })
  },

  login() {
    wx.navigateTo({ url: LOGIN_PAGE })
  },

  retry() {
    return this.loadOverview(cachedProfile(getCurrentUser() || {}))
  }
})

module.exports = {
  WORK_ENTRIES,
  GROWTH_ENTRIES,
  avatarText,
  avatarUrl,
  organizationText,
  cachedProfile,
  normalizeOverview
}
