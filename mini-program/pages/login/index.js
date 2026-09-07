const { loginAndBootstrap } = require("../../utils/auth-flow")

Page({
  data: {
    username: "admin",
    password: "123456",
    submitting: false,
    passwordVisible: false,
    agreementChecked: false
  },

  onUsernameInput(event) {
    this.setData({ username: event.detail.value })
  },

  onPasswordInput(event) {
    this.setData({ password: event.detail.value })
  },

  togglePassword() {
    this.setData({ passwordVisible: !this.data.passwordVisible })
  },

  toggleAgreement() {
    this.setData({ agreementChecked: !this.data.agreementChecked })
  },

  showComingSoon() {
    wx.showToast({ title: "该登录方式暂未开放", icon: "none" })
  },

  async submitLogin() {
    if (this.data.submitting) {
      return
    }

    const username = String(this.data.username || "").trim()
    const password = String(this.data.password || "")

    if (!this.data.agreementChecked) {
      wx.showToast({ title: "请先阅读并同意用户协议和隐私政策", icon: "none" })
      return
    }

    if (!username || !password) {
      wx.showToast({ title: "请输入账号和密码", icon: "none" })
      return
    }

    this.setData({ submitting: true })
    try {
      await loginAndBootstrap({ username, password })
      wx.showToast({ title: "登录成功", icon: "success" })
      wx.switchTab({ url: "/pages/home/home" })
    } catch (error) {
      wx.showToast({ title: error && error.message ? error.message : "登录失败", icon: "none" })
    } finally {
      this.setData({ submitting: false })
    }
  }
})
