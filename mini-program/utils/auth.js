const TOKEN_KEY = "auth.accessToken"
const PERMISSION_CODES_KEY = "auth.permissionCodes"
const USER_INFO_KEY = "auth.currentUser"

function getWx() {
  if (typeof wx === "undefined") {
    throw new Error("wx runtime is not available")
  }
  return wx
}

function getToken() {
  return getWx().getStorageSync(TOKEN_KEY) || ""
}

function setToken(token) {
  getWx().setStorageSync(TOKEN_KEY, token || "")
}

function getPermissionCodes() {
  const codes = getWx().getStorageSync(PERMISSION_CODES_KEY)
  return Array.isArray(codes) ? codes : []
}

function setPermissionCodes(codes) {
  getWx().setStorageSync(PERMISSION_CODES_KEY, Array.isArray(codes) ? codes : [])
}

function hasPermission(code) {
  return getPermissionCodes().includes(code)
}

function getCurrentUser() {
  return getWx().getStorageSync(USER_INFO_KEY) || null
}

function setCurrentUser(user) {
  getWx().setStorageSync(USER_INFO_KEY, user || null)
}

function clearAuth() {
  const runtime = getWx()
  runtime.removeStorageSync(TOKEN_KEY)
  runtime.removeStorageSync(PERMISSION_CODES_KEY)
  runtime.removeStorageSync(USER_INFO_KEY)
}

module.exports = {
  TOKEN_KEY,
  PERMISSION_CODES_KEY,
  USER_INFO_KEY,
  getToken,
  setToken,
  clearAuth,
  getPermissionCodes,
  setPermissionCodes,
  hasPermission,
  getCurrentUser,
  setCurrentUser
}
