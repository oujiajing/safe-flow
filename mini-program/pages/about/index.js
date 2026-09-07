Page({
  data: { version: "1.0.0" },

  onLoad() {
    if (wx.getAccountInfoSync) {
      const account = wx.getAccountInfoSync()
      const version = account && account.miniProgram && account.miniProgram.version
      if (version) this.setData({ version })
    }
  },

  goBack() {
    wx.navigateBack()
  }
})
