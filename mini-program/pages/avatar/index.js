const { getToken } = require("../../utils/auth")
const { LOGIN_PAGE } = require("../../utils/auth-flow")
const env = require("../../config/env")
const meService = require("../../services/me")

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
    uploading: false,
    avatarUrl: ""
  },

  onLoad() {
    this.setupNavigationMetrics()
    if (!getToken()) {
      wx.redirectTo({ url: LOGIN_PAGE })
    }
  },

  onShow() {
    if (getToken()) return this.loadAvatar()
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

  async loadAvatar() {
    this.setData({ loading: true })
    try {
      const overview = await meService.getMineOverview()
      this.setData({
        avatarUrl: resolveAvatarUrl(overview && overview.profile && overview.profile.avatar),
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false })
      wx.showToast({ title: error && error.message ? error.message : "头像加载失败", icon: "none" })
    }
  },

  chooseFromAlbum() {
    return this.chooseAndUpload("album")
  },

  takePhoto() {
    return this.chooseAndUpload("camera")
  },

  chooseAndUpload(sourceType) {
    if (this.data.uploading) return Promise.resolve()
    return new Promise((resolve, reject) => {
      const success = result => {
        const media = (result.tempFiles || [])[0]
        const path = media ? (media.tempFilePath || media.path) : (result.tempFilePaths || [])[0]
        resolve(path || "")
      }
      const fail = error => {
        if (error && String(error.errMsg || "").includes("cancel")) resolve("")
        else reject(error)
      }
      if (wx.chooseMedia) {
        wx.chooseMedia({
          count: 1,
          mediaType: ["image"],
          sourceType: [sourceType],
          sizeType: ["compressed"],
          success,
          fail
        })
        return
      }
      if (wx.chooseImage) {
        wx.chooseImage({ count: 1, sourceType: [sourceType], sizeType: ["compressed"], success, fail })
        return
      }
      reject(new Error("当前微信版本不支持选择图片"))
    }).then(async filePath => {
      if (!filePath) return
      this.setData({ uploading: true })
      wx.showLoading({ title: "正在更新头像", mask: true })
      try {
        const uploaded = await meService.uploadAvatar(filePath)
        this.setData({ avatarUrl: resolveAvatarUrl(uploaded && uploaded.url ? uploaded.url : filePath) })
        wx.showToast({ title: "头像已更新", icon: "success" })
      } catch (error) {
        wx.showToast({ title: error && error.message ? error.message : "头像更新失败", icon: "none" })
      } finally {
        this.setData({ uploading: false })
        wx.hideLoading()
      }
    }).catch(error => {
      wx.showToast({ title: error && error.errMsg ? error.errMsg : "图片选择失败", icon: "none" })
    })
  },

  openAvatarDecoration() {
    wx.showToast({ title: "头像装扮功能即将开放", icon: "none" })
  },

  openAiAvatar() {
    wx.showToast({ title: "AI头像功能即将开放", icon: "none" })
  },

  goBack() {
    wx.navigateBack()
  }
})

module.exports = { resolveAvatarUrl }
