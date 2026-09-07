const env = require("../config/env")
const auth = require("./auth")
const {
  CONFLICT_MESSAGE,
  DOMAIN_WHITELIST_MESSAGE,
  handleHttpError,
  normalizeError,
  normalizeFailMessage,
  unwrapApiResponse
} = require("./api-error")

function getWx() {
  if (typeof wx === "undefined") {
    throw new Error("wx runtime is not available")
  }
  return wx
}

function buildUrl(url) {
  if (/^https?:\/\//.test(url)) {
    return url
  }
  return `${env.baseUrl}${url.startsWith("/") ? url : `/${url}`}`
}

function createHeaders(header = {}) {
  const token = auth.getToken()
  return {
    "Content-Type": "application/json",
    ...header,
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  }
}

function request({ url, method = "GET", data, header = {}, timeout = env.requestTimeout }) {
  const runtime = getWx()

  return new Promise((resolve, reject) => {
    runtime.request({
      url: buildUrl(url),
      method,
      data,
      timeout,
      header: createHeaders(header),
      success(response) {
        if (handleHttpError(response.statusCode, response, reject)) {
          return
        }

        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(normalizeError(response.data && response.data.message ? response.data.message : "请求失败", response))
          return
        }

        const unwrapped = unwrapApiResponse(response, reject)
        if (unwrapped !== undefined) {
          resolve(unwrapped)
        }
      },
      fail(error) {
        reject(normalizeError(normalizeFailMessage(error), error))
      }
    })
  })
}

module.exports = {
  CONFLICT_MESSAGE,
  DOMAIN_WHITELIST_MESSAGE,
  buildUrl,
  createHeaders,
  request
}
