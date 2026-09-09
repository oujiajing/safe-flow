Component({
  data: {
    statusBarHeight: 28,
    navigationBarHeight: 54,
    menuSpaceWidth: 94,
    headerTokens: {
      backIconSize: "52rpx",
      titleColor: "#ffffff"
    }
  },

  properties: {
    title: {
      type: String,
      value: ""
    },
    showBack: {
      type: Boolean,
      value: true
    }
  },

  lifetimes: {
    attached() {
      const systemInfo = wx.getSystemInfoSync ? wx.getSystemInfoSync() : {}
      const menuButton = wx.getMenuButtonBoundingClientRect
        ? wx.getMenuButtonBoundingClientRect()
        : null
      const statusBarHeight = Number(systemInfo.statusBarHeight) || 20
      const windowWidth = Number(systemInfo.windowWidth || systemInfo.screenWidth) || 375
      const hasMenuGeometry = menuButton
        && Number.isFinite(menuButton.top)
        && Number.isFinite(menuButton.height)
      const navigationBarHeight = hasMenuGeometry
        ? Math.max(menuButton.height, (menuButton.top - statusBarHeight) * 2 + menuButton.height)
        : 44
      const menuSpaceWidth = menuButton && Number.isFinite(menuButton.left)
        ? Math.max(88, windowWidth - menuButton.left)
        : 94

      this.setData({
        statusBarHeight,
        navigationBarHeight,
        menuSpaceWidth
      })
    }
  },

  methods: {
    handleBack() {
      this.triggerEvent("back")
    }
  }
})
