const auth = require("./auth")

const CONFLICT_MESSAGE = "记录已被其他端更新，请刷新后重试"
const DOMAIN_WHITELIST_MESSAGE = "本地后端域名被拦截，请在微信开发者工具关闭合法域名校验后重试"

function getWx() {
  if (typeof wx === "undefined") {
    throw new Error("wx runtime is not available")
  }
  return wx
}

function normalizeError(message, response) {
  const error = new Error(message)
  error.response = response
  return error
}

function normalizeFailMessage(error, fallback = "网络请求失败") {
  const message = error && error.errMsg ? error.errMsg : fallback
  if (message.includes("url not in domain list")) {
    return DOMAIN_WHITELIST_MESSAGE
  }
  return message
}

function rejectConflict(response, reject) {
  getWx().showModal({ title: "记录已更新", content: CONFLICT_MESSAGE, showCancel: false })
  reject(normalizeError(CONFLICT_MESSAGE, response))
}

function handleHttpError(statusCode, response, reject) {
  const runtime = getWx()

  if (statusCode === 401) {
    auth.clearAuth()
    runtime.navigateTo({ url: "/pages/login/index" })
    reject(normalizeError("登录已失效，请重新登录", response))
    return true
  }

  if (statusCode === 403) {
    runtime.showToast({ title: "无权限操作", icon: "none" })
    reject(normalizeError("无权限操作", response))
    return true
  }

  if (statusCode === 409) {
    rejectConflict(response, reject)
    return true
  }

  return false
}

function unwrapApiResponse(response, reject, fallback = "请求失败") {
  const body = response.data
  const message = body && body.message ? body.message : fallback

  if (body && body.code === 0) {
    return body.data
  }

  if (message.includes("版本冲突")) {
    rejectConflict(response, reject)
    return undefined
  }

  reject(normalizeError(message, response))
  return undefined
}

module.exports = {
  CONFLICT_MESSAGE,
  DOMAIN_WHITELIST_MESSAGE,
  handleHttpError,
  normalizeError,
  normalizeFailMessage,
  unwrapApiResponse
}
