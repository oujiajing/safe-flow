const env = require("../config/env")
const auth = require("./auth")
const {
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
    ...header,
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  }
}

function parseUploadData(rawData) {
  if (typeof rawData === "string") {
    try {
      return JSON.parse(rawData)
    } catch (error) {
      return { code: -1, message: rawData }
    }
  }
  return rawData
}

function uploadFile({ url, filePath, fileKind, name = "file", formData = {}, header = {}, timeout = env.uploadTimeout }) {
  const runtime = getWx()

  return new Promise((resolve, reject) => {
    runtime.uploadFile({
      url: buildUrl(url),
      filePath,
      name,
      timeout,
      header: createHeaders(header),
      formData: {
        fileKind,
        ...formData
      },
      success(response) {
        const body = parseUploadData(response.data)
        const normalizedResponse = { ...response, data: body }
        if (handleHttpError(response.statusCode, normalizedResponse, reject)) {
          return
        }

        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(normalizeError(body && body.message ? body.message : "上传失败", normalizedResponse))
          return
        }

        const unwrapped = unwrapApiResponse(normalizedResponse, reject, "上传失败")
        if (unwrapped !== undefined) {
          resolve(unwrapped)
        }
      },
      fail(error) {
        reject(normalizeError(normalizeFailMessage(error, "上传失败"), error))
      }
    })
  })
}

module.exports = {
  uploadFile,
  buildUrl,
  createHeaders,
  parseUploadData
}
