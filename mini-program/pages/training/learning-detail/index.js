const learningService = require("../../../services/safety-learning")
const idempotency = require("../../../utils/idempotency")

function getWx() {
  return wx
}

function pickFirst(record, keys) {
  for (const key of keys) {
    if (record && record[key] !== undefined && record[key] !== null && record[key] !== "") {
      return record[key]
    }
  }
  return ""
}

function toNumber(value) {
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : 0
}

function durationTextToSeconds(text) {
  const value = String(text || "")
  const minuteMatch = value.match(/(\d+(?:\.\d+)?)\s*分钟/)
  if (minuteMatch) {
    return Math.round(Number(minuteMatch[1]) * 60)
  }
  const secondMatch = value.match(/(\d+(?:\.\d+)?)\s*秒/)
  if (secondMatch) {
    return Math.round(Number(secondMatch[1]))
  }
  return 0
}

function formatDuration(seconds) {
  const safeSeconds = Math.max(0, Math.round(seconds || 0))
  if (!safeSeconds) {
    return "不限时长"
  }
  if (safeSeconds < 60) {
    return `${safeSeconds}秒`
  }
  const minutes = Math.ceil(safeSeconds / 60)
  return `${minutes}分钟`
}

function requiredWatchSeconds(content) {
  const seconds = toNumber(
    pickFirst(content, ["requiredWatchSeconds", "watchSeconds", "durationSeconds", "videoDurationSeconds"])
  )
  if (seconds) {
    return Math.round(seconds)
  }
  const minutes = toNumber(pickFirst(content, ["requiredWatchMinutes", "durationMinutes", "learningDurationMinutes"]))
  if (minutes) {
    return Math.round(minutes * 60)
  }
  return durationTextToSeconds(pickFirst(content, ["durationText", "learningDurationText"]))
}

function watchState(watchedSeconds, requiredSeconds) {
  const watched = Math.max(0, Math.floor(watchedSeconds || 0))
  const required = Math.max(0, Math.floor(requiredSeconds || 0))
  const canCheckIn = required === 0 || watched >= required
  const progress = required === 0 ? 100 : Math.min(100, Math.round((watched / required) * 100))
  const remaining = Math.max(0, required - watched)
  return {
    canCheckIn,
    remainingWatchText: canCheckIn ? "已完成学习，可进行打卡" : `还需学习${formatDuration(remaining)}即可打卡`,
    requiredWatchText: formatDuration(required),
    watchedSeconds: watched,
    watchedText: formatDuration(watched),
    watchProgress: progress
  }
}

function normalizeDetail(content) {
  if (!content) {
    return null
  }
  const videoAttachment = content.videoAttachment || {}
  const requiredSeconds = requiredWatchSeconds(content)
  return {
    ...content,
    category: content.category || "应急管理",
    coverImageUrl: "",
    durationText: formatDuration(requiredSeconds),
    requiredWatchSeconds: requiredSeconds,
    videoUrl: videoAttachment.url || content.video || content.videoUrl || ""
  }
}

Page({
  data: {
    id: "",
    loading: false,
    checkingIn: false,
    content: null,
    checkIn: null,
    requiredWatchSeconds: 0,
    videoDurationSeconds: 0,
    watchedSeconds: 0,
    watchedText: "0秒",
    requiredWatchText: "不限时长",
    remainingWatchText: "已完成学习，可进行打卡",
    watchProgress: 0,
    canCheckIn: false,
    videoFullscreen: false
  },

  onLoad(options = {}) {
    this.setData({ id: options.id || "" })
    return this.loadDetail()
  },

  async loadDetail() {
    if (!this.data.id) {
      return
    }
    this.setData({ loading: true })
    try {
      const content = normalizeDetail(await learningService.getLearningContent(this.data.id))
      const requiredSeconds = content ? content.requiredWatchSeconds : 0
      this.setData({
        content,
        requiredWatchSeconds: requiredSeconds,
        ...watchState(0, requiredSeconds)
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  handleVideoLoadedMetadata(event) {
    const duration = toNumber(event && event.detail && event.detail.duration)
    if (!duration) {
      return
    }
    const requiredSeconds = this.data.requiredWatchSeconds || Math.round(duration)
    this.setData({
      requiredWatchSeconds: requiredSeconds,
      videoDurationSeconds: Math.round(duration),
      ...watchState(this.data.watchedSeconds, requiredSeconds)
    })
  },

  handleVideoTimeUpdate(event) {
    const detail = (event && event.detail) || {}
    const currentTime = toNumber(detail.currentTime)
    const duration = toNumber(detail.duration)
    const watchedSeconds = Math.max(this.data.watchedSeconds || 0, Math.floor(currentTime))
    const requiredSeconds = this.data.requiredWatchSeconds || Math.round(duration || 0)
    this.setData({
      requiredWatchSeconds: requiredSeconds,
      videoDurationSeconds: duration ? Math.round(duration) : this.data.videoDurationSeconds,
      ...watchState(watchedSeconds, requiredSeconds)
    })
  },

  handleVideoFullscreenChange(event) {
    const detail = (event && event.detail) || {}
    this.setData({ videoFullscreen: Boolean(detail.fullScreen) })
  },

  async checkIn() {
    if (!this.data.id || this.data.checkingIn) {
      return
    }
    if (!this.data.canCheckIn) {
      getWx().showToast({ title: "完成学习后可打卡", icon: "none" })
      return
    }
    this.setData({ checkingIn: true })
    try {
      const checkIn = await learningService.checkInLearning(this.data.id, {
        clientRequestId: idempotency.createClientRequestId("learning"),
        watchedSeconds: this.data.watchedSeconds
      })
      this.setData({ checkIn })
      getWx().showToast({ title: "打卡成功", icon: "success" })
    } finally {
      this.setData({ checkingIn: false })
    }
  },

  goBack() {
    getWx().navigateBack()
  }
})
