const assert = require("node:assert/strict")
const test = require("node:test")

const authPath = require.resolve("../utils/auth")
const authFlowPath = require.resolve("../utils/auth-flow")

function loadSettings(token = "token") {
  const navigateCalls = []
  const modalCalls = []
  let logoutCalls = 0

  require.cache[authPath] = {
    id: authPath,
    filename: authPath,
    loaded: true,
    exports: { getToken: () => token }
  }
  require.cache[authFlowPath] = {
    id: authFlowPath,
    filename: authFlowPath,
    loaded: true,
    exports: {
      LOGIN_PAGE: "/pages/login/index",
      logout() { logoutCalls += 1 }
    }
  }
  global.wx = {
    navigateTo(options) { navigateCalls.push(options) },
    navigateBack() {},
    showModal(options) {
      modalCalls.push(options)
      options.success({ confirm: true })
    }
  }
  global.Page = config => {
    global.__settingsPage = {
      ...config,
      data: JSON.parse(JSON.stringify(config.data)),
      setData(patch) { this.data = { ...this.data, ...patch } }
    }
  }

  const pagePath = require.resolve("../pages/settings/index")
  delete require.cache[pagePath]
  require("../pages/settings/index")
  return {
    page: global.__settingsPage,
    navigateCalls,
    modalCalls,
    logoutCalls: () => logoutCalls
  }
}

test("settings routes account services and confirms account switching and logout", () => {
  const { page, navigateCalls, modalCalls, logoutCalls } = loadSettings()

  page.openService({ currentTarget: { dataset: { route: "/pages/profile/index" } } })
  page.openService({ currentTarget: { dataset: { route: "/pages/help/index" } } })
  assert.deepEqual(navigateCalls, [
    { url: "/pages/profile/index" },
    { url: "/pages/help/index" }
  ])

  page.confirmSwitchAccount()
  page.confirmLogout()
  assert.deepEqual(modalCalls.map(item => item.title), ["切换账号", "退出登录"])
  assert.equal(logoutCalls(), 2)
})

test("settings redirects protected actions to login without a session", () => {
  const { page, navigateCalls, logoutCalls } = loadSettings("")

  page.openService({ currentTarget: { dataset: { route: "/pages/profile/index" } } })
  page.confirmSwitchAccount()
  page.confirmLogout()

  assert.deepEqual(navigateCalls, [
    { url: "/pages/login/index" },
    { url: "/pages/login/index" },
    { url: "/pages/login/index" }
  ])
  assert.equal(logoutCalls(), 0)
})
