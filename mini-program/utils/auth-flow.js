const authStorage = require("./auth")
const authService = require("../services/auth")
const userService = require("../services/user")

const LOGIN_PAGE = "/pages/login/index"

function getWx() {
  if (typeof wx === "undefined") {
    throw new Error("wx runtime is not available")
  }
  return wx
}

function getRuntimeApp() {
  if (typeof getApp === "function") {
    return getApp()
  }
  return null
}

function resolveToken(loginResponse) {
  return loginResponse && (loginResponse.accessToken || loginResponse.token || loginResponse.access_token || "")
}

function syncGlobalData(currentUser, permissionCodes) {
  const app = getRuntimeApp()
  if (!app) {
    return
  }
  app.globalData = app.globalData || {}
  app.globalData.currentUser = currentUser
  app.globalData.permissionCodes = permissionCodes
}

async function loginAndBootstrap(credentials) {
  const loginResponse = await authService.login(credentials)
  const token = resolveToken(loginResponse)
  authStorage.setToken(token)

  const permissionCodes = await authService.getPermissionCodes()
  authStorage.setPermissionCodes(permissionCodes)

  const currentUser = await userService.getCurrentUser()
  authStorage.setCurrentUser(currentUser)
  syncGlobalData(currentUser, permissionCodes)

  return {
    token,
    permissionCodes,
    currentUser
  }
}

async function refreshPermissionCodes() {
  const currentCodes = authStorage.getPermissionCodes()
  if (!authStorage.getToken()) {
    return currentCodes
  }
  try {
    const permissionCodes = await authService.getPermissionCodes()
    authStorage.setPermissionCodes(permissionCodes)
    syncGlobalData(authStorage.getCurrentUser(), permissionCodes)
    return permissionCodes
  } catch (error) {
    return currentCodes
  }
}

function requireLogin() {
  if (authStorage.getToken()) {
    return true
  }
  getWx().navigateTo({ url: LOGIN_PAGE })
  return false
}

function logout() {
  authStorage.clearAuth()
  syncGlobalData(null, [])
  getWx().reLaunch({ url: LOGIN_PAGE })
}

module.exports = {
  LOGIN_PAGE,
  loginAndBootstrap,
  refreshPermissionCodes,
  requireLogin,
  logout
}
