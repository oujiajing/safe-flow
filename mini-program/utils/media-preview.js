const env = require("../config/env")

function mediaUrl(item = {}) {
  const value = item.url || item.previewUrl || item.path || item.tempFilePath || item.storagePath || ""
  if (!value || /^(https?:|wxfile:|blob:|file:)/.test(value)) return value
  if (value.startsWith("/")) return `${env.baseUrl}${value}`
  return `${env.assetBaseUrl.replace(/\/+$/, "")}/${value.replace(/^\/+/, "")}`
}

function mediaKind(item = {}) {
  const kind = String(item.fileKind || item.kind || item.type || "").toUpperCase()
  if (kind === "VIDEO" || /\.(mp4|mov|m4v|avi|webm)(\?|$)/i.test(mediaUrl(item))) return "video"
  return "image"
}

function buildPreviewSources(items = []) {
  return items.map(item => ({
    type: mediaKind(item),
    url: mediaUrl(item),
    ...(item.thumbPath || item.poster ? { poster: item.thumbPath || item.poster } : {})
  })).filter(item => item.url)
}

function openMediaPreview(runtime, items, currentIndex = 0) {
  const sources = buildPreviewSources(items)
  if (!sources.length) return { handled: false, videoUrl: "" }
  const index = Math.max(0, Math.min(Number(currentIndex) || 0, sources.length - 1))
  if (runtime.previewMedia) {
    runtime.previewMedia({ current: index, sources })
    return { handled: true, videoUrl: "" }
  }
  const current = sources[index]
  if (current.type === "image" && runtime.previewImage) {
    const urls = sources.filter(item => item.type === "image").map(item => item.url)
    runtime.previewImage({ current: current.url, urls })
    return { handled: true, videoUrl: "" }
  }
  return { handled: false, videoUrl: current.type === "video" ? current.url : "" }
}

module.exports = { buildPreviewSources, mediaKind, mediaUrl, openMediaPreview }
