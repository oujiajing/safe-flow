const assert = require("node:assert/strict")
const test = require("node:test")

const authServicePath = require.resolve("../services/auth")
const userServicePath = require.resolve("../services/user")

function createWxMock() {
  const storage = new Map()
  const calls = { navigateTo: [], redirectTo: [], reLaunch: [], switchTab: [], showToast: [] }
  return {
    storage,
    calls,
    getStorageSync(key) {
      return storage.get(key)
    },
    setStorageSync(key, value) {
      storage.set(key, value)
    },
    removeStorageSync(key) {
      storage.delete(key)
    },
    navigateTo(options) {
      calls.navigateTo.push(options)
    },
    redirectTo(options) {
      calls.redirectTo.push(options)
    },
    reLaunch(options) {
      calls.reLaunch.push(options)
    },
    switchTab(options) {
      calls.switchTab.push(options)
    },
    showToast(options) {
      calls.showToast.push(options)
    }
  }
}

function reset(modulePath) {
  try {
    delete require.cache[require.resolve(modulePath)]
  } catch (error) {
    // no-op
  }
}

function stubServices({ loginResponse, permissionCodes, currentUser }) {
  const calls = []
  require.cache[authServicePath] = {
    id: authServicePath,
    filename: authServicePath,
    loaded: true,
    exports: {
      login(payload) {
        calls.push({ fn: "login", payload })
        return Promise.resolve(loginResponse)
      },
      getPermissionCodes() {
        calls.push({ fn: "getPermissionCodes" })
        return Promise.resolve(permissionCodes)
      }
    }
  }
  require.cache[userServicePath] = {
    id: userServicePath,
    filename: userServicePath,
    loaded: true,
    exports: {
      getCurrentUser() {
        calls.push({ fn: "getCurrentUser" })
        return Promise.resolve(currentUser)
      }
    }
  }
  return calls
}

test("loginAndBootstrap stores token, permission codes, current user, and updates app globalData", async () => {
  global.wx = createWxMock()
  const app = { globalData: {} }
  global.getApp = () => app
  const calls = stubServices({
    loginResponse: { accessToken: "jwt-token", expiresIn: 7200 },
    permissionCodes: ["PINGAN_THREE_CHECK_ENTRY", "PINGAN_THREE_CHECK_VIEW"],
    currentUser: { id: 1, username: "admin", realName: "管理员" }
  })
  reset("../utils/auth")
  reset("../utils/auth-flow")
  const auth = require("../utils/auth")
  const authFlow = require("../utils/auth-flow")

  const result = await authFlow.loginAndBootstrap({ username: "admin", password: "SAFE_TEST_PASSWORD" })

  assert.deepEqual(calls, [
    { fn: "login", payload: { username: "admin", password: "SAFE_TEST_PASSWORD" } },
    { fn: "getPermissionCodes" },
    { fn: "getCurrentUser" }
  ])
  assert.deepEqual(result, {
    token: "jwt-token",
    permissionCodes: ["PINGAN_THREE_CHECK_ENTRY", "PINGAN_THREE_CHECK_VIEW"],
    currentUser: { id: 1, username: "admin", realName: "管理员" }
  })
  assert.equal(auth.getToken(), "jwt-token")
  assert.deepEqual(auth.getPermissionCodes(), ["PINGAN_THREE_CHECK_ENTRY", "PINGAN_THREE_CHECK_VIEW"])
  assert.deepEqual(auth.getCurrentUser(), { id: 1, username: "admin", realName: "管理员" })
  assert.deepEqual(app.globalData.permissionCodes, ["PINGAN_THREE_CHECK_ENTRY", "PINGAN_THREE_CHECK_VIEW"])
  assert.deepEqual(app.globalData.currentUser, { id: 1, username: "admin", realName: "管理员" })
})

test("three-check pages refresh stale permission codes before deriving action buttons", async () => {
  global.wx = createWxMock()
  const app = { globalData: {} }
  global.getApp = () => app
  const calls = stubServices({
    loginResponse: {},
    permissionCodes: [
      "PINGAN_PRE_SHIFT_MEETING_UPDATE",
      "PINGAN_PRE_SHIFT_MEETING_SUBMIT",
      "PINGAN_PRE_SHIFT_MEETING_VOID",
      "PINGAN_PRE_SHIFT_MEETING_REMIND"
    ],
    currentUser: null
  })
  reset("../utils/auth")
  reset("../utils/auth-flow")
  reset("../utils/three-check-permissions")
  const auth = require("../utils/auth")
  auth.setToken("jwt-token")
  auth.setPermissionCodes(["PINGAN_PRE_SHIFT_MEETING_VIEW"])

  const { refreshThreeCheckActionState } = require("../utils/three-check-permissions")
  const state = await refreshThreeCheckActionState("meeting")

  assert.deepEqual(calls, [{ fn: "getPermissionCodes" }])
  assert.deepEqual(auth.getPermissionCodes(), [
    "PINGAN_PRE_SHIFT_MEETING_UPDATE",
    "PINGAN_PRE_SHIFT_MEETING_SUBMIT",
    "PINGAN_PRE_SHIFT_MEETING_VOID",
    "PINGAN_PRE_SHIFT_MEETING_REMIND"
  ])
  assert.equal(state.canUpdateAction, true)
  assert.equal(state.canSubmitAction, true)
  assert.equal(state.canWithdrawAction, true)
  assert.equal(state.canRemindAction, true)
  assert.deepEqual(app.globalData.permissionCodes, auth.getPermissionCodes())
})

test("requireLogin navigates to login when token is missing and keeps page when token exists", () => {
  global.wx = createWxMock()
  reset("../utils/auth")
  reset("../utils/auth-flow")
  const auth = require("../utils/auth")
  const authFlow = require("../utils/auth-flow")

  assert.equal(authFlow.requireLogin(), false)
  assert.deepEqual(global.wx.calls.navigateTo, [{ url: "/pages/login/index" }])

  auth.setToken("jwt-token")
  assert.equal(authFlow.requireLogin(), true)
  assert.deepEqual(global.wx.calls.navigateTo, [{ url: "/pages/login/index" }])
})

test("logout clears auth and redirects to login", () => {
  global.wx = createWxMock()
  reset("../utils/auth")
  reset("../utils/auth-flow")
  const auth = require("../utils/auth")
  const authFlow = require("../utils/auth-flow")

  auth.setToken("jwt-token")
  auth.setPermissionCodes(["PINGAN_THREE_CHECK_VIEW"])
  auth.setCurrentUser({ id: 1 })

  authFlow.logout()

  assert.equal(auth.getToken(), "")
  assert.deepEqual(auth.getPermissionCodes(), [])
  assert.equal(auth.getCurrentUser(), null)
  assert.deepEqual(global.wx.calls.reLaunch, [{ url: "/pages/login/index" }])
})

