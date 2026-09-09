const env = require("./config/env")

App({
  globalData: {
    env,
    currentUser: null,
    permissionCodes: []
  }
})
