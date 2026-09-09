const { getToken } = require("../../utils/auth")
const { LOGIN_PAGE, logout } = require("../../utils/auth-flow")

const SERVICE_ENTRIES = [
  { route: "/pages/profile/index", label: "个人资料", icon: "user" },
  { route: "/pages/help/index", label: "帮助与反馈", icon: "help-circle" },
  { route: "/pages/about/index", label: "关于平安班组", icon: "info-circle", value: "版本 1.0.0" }
]

Page({
  data: {
    statusBarHeight: 20,
    navigationBarHeight: 44,
    serviceEntries: SERVICE_ENTRIES
  },

  onLoad() {
    this.setupNavigationMetrics()
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

  goBack() {
    wx.navigateBack()
  },

  openService(event) {
    const route = event.currentTarget.dataset.route
    if (!route) return
    if (route === "/pages/profile/index" && !getToken()) {
      wx.navigateTo({ url: LOGIN_PAGE })
      return
    }
    wx.navigateTo({ url: route })
  },

  confirmSwitchAccount() {
    if (!getToken()) {
      wx.navigateTo({ url: LOGIN_PAGE })
      return
    }
    wx.showModal({
      title: "切换账号",
      content: "切换账号将退出当前登录，是否继续？",
      confirmText: "切换",
      confirmColor: "#078c59",
      success(result) {
        if (result.confirm) logout()
      }
    })
  },

  confirmLogout() {
    if (!getToken()) {
      wx.navigateTo({ url: LOGIN_PAGE })
      return
    }
    wx.showModal({
      title: "退出登录",
      content: "确定要退出当前账号吗？",
      confirmText: "退出",
      confirmColor: "#e73339",
      success(result) {
        if (result.confirm) logout()
      }
    })
  }
})

module.exports = { SERVICE_ENTRIES }
