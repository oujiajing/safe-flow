const { getToken } = require("../../utils/auth")
const { LOGIN_PAGE } = require("../../utils/auth-flow")
const env = require("../../config/env")
const meService = require("../../services/me")

function rows(profile = {}) {
  return [
    { label: "姓名", value: profile.realName || "未维护" },
    { label: "登录账号", value: profile.username || "未维护" },
    { label: "员工编码", value: profile.employeeCode || "未维护" },
    { label: "所属公司", value: profile.companyName || "未维护" },
    { label: "所属部门", value: profile.departmentName || "未维护" },
    { label: "所属班组", value: profile.teamName || "未维护" },
    { label: "当前岗位", value: profile.positionName || "未维护" },
    { label: "业务角色", value: (profile.roleNames || []).join("、") || "未配置" }
  ]
}

function groupedRows(profile = {}) {
  const allRows = rows(profile)
  return {
    basicRows: allRows.slice(0, 3).map((item, index) => ({ ...item, emphasis: index === 0 })),
    organizationRows: allRows.slice(3)
  }
}

function resolveAvatarUrl(value) {
  const url = value === undefined || value === null ? "" : String(value).trim()
  if (!url || /^(https?:|wxfile:|data:)/.test(url)) return url
  return `${env.baseUrl}${url.startsWith("/") ? url : `/${url}`}`
}

Page({
  data: {
    statusBarHeight: 20,
    navigationBarHeight: 44,
    loading: false,
    errorText: "",
    profile: {},
    avatarText: "我",
    avatarUrl: "",
    basicRows: [],
    organizationRows: []
  },

  onLoad() {
    this.setupNavigationMetrics()
    if (!getToken()) {
      wx.redirectTo({ url: LOGIN_PAGE })
    }
  },

  onShow() {
    if (getToken()) {
      return this.loadProfile()
    }
  },

  async loadProfile() {
    this.setData({ loading: true, errorText: "" })
    try {
      const overview = await meService.getMineOverview()
      const profile = overview.profile || {}
      const name = profile.realName || profile.username || "我"
      this.setData({
        profile,
        avatarText: String(name).slice(0, 1),
        avatarUrl: resolveAvatarUrl(profile.avatar),
        ...groupedRows(profile),
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false, errorText: error && error.message ? error.message : "资料加载失败" })
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
    this.setData({ statusBarHeight, navigationBarHeight })
  },

  openAvatarEditor() {
    wx.navigateTo({ url: "/pages/avatar/index" })
  },

  goBack() {
    wx.navigateBack()
  }
})

module.exports = { rows, groupedRows, resolveAvatarUrl }
