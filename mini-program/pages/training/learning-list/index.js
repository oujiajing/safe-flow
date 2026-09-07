const learningService = require("../../../services/safety-learning")

function getWx() {
  return wx
}

const CATEGORY_TABS = [
  { label: "应急管理", value: "应急管理" },
  { label: "安全设施", value: "安全设施" },
  { label: "隐患知识", value: "隐患知识" },
  { label: "法律知识", value: "法律知识" },
  { label: "设备操作规程", value: "设备操作规程" }
]

const STATUS_MAP = {
  NOT_STARTED: { label: "未学习", className: "pending" },
  UNLEARNED: { label: "未学习", className: "pending" },
  PENDING: { label: "未学习", className: "pending" },
  IN_PROGRESS: { label: "未学习", className: "pending" },
  LEARNING: { label: "未学习", className: "pending" },
  COMPLETED: { label: "已学习", className: "done" },
  LEARNED: { label: "已学习", className: "done" },
  CHECKED_IN: { label: "已学习", className: "done" },
  FINISHED: { label: "已学习", className: "done" }
}

function pickFirst(record, keys) {
  for (const key of keys) {
    if (record && record[key] !== undefined && record[key] !== null && record[key] !== "") {
      return record[key]
    }
  }
  return ""
}

function normalizeStatus(record) {
  const rawStatus = pickFirst(record, ["learningStatus", "studyStatus", "checkInStatus", "status"])
  const normalized = String(rawStatus || "").toUpperCase()
  if (STATUS_MAP[normalized]) {
    return STATUS_MAP[normalized]
  }
  const label = pickFirst(record, ["learningStatusLabel", "studyStatusLabel", "statusLabel"]) || "未学习"
  if (label.indexOf("已") >= 0) {
    return { label: "已学习", className: "done" }
  }
  return { label: "未学习", className: "pending" }
}

function normalizeLearningContent(record) {
  const status = normalizeStatus(record)
  const categoryLabel = pickFirst(record, ["categoryName", "category", "typeName", "type"]) || "应急管理"
  const points = pickFirst(record, ["pointValue", "points", "score", "credit", "integral"])
  const coverImageAttachment = record.coverImageAttachment || {}
  return {
    ...record,
    categoryLabel,
    coverUrl:
      coverImageAttachment.url ||
      pickFirst(record, ["coverUrl", "coverImage", "cover", "imageUrl", "thumbnailUrl", "thumbUrl", "bannerUrl"]),
    statusLabel: status.label,
    statusClass: status.className,
    pointText: `${points || 0}积分`,
    actionText: "去学习"
  }
}

function buildLearningQuery(data) {
  const query = {
    page: 1,
    pageSize: 20,
    status: "ACTIVE"
  }
  const keyword = String(data.keyword || "").trim()
  if (keyword) {
    query.keyword = keyword
  }
  if (data.activeCategory) {
    query.category = data.activeCategory
  }
  return query
}

Page({
  data: {
    loading: false,
    keyword: "",
    categoryTabs: CATEGORY_TABS,
    activeCategory: "",
    contents: [],
    total: 0
  },

  onLoad() {
    return this.loadContents()
  },

  async loadContents() {
    this.setData({ loading: true })
    try {
      const result = await learningService.listLearningContents(buildLearningQuery(this.data))
      this.setData({
        contents: (result.items || []).map(normalizeLearningContent),
        total: result.total || 0
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  updateKeyword(event) {
    this.setData({ keyword: event.detail.value || "" })
  },

  search() {
    return this.loadContents()
  },

  changeCategory(event) {
    const category = event.currentTarget.dataset.category
    if (!category || category === this.data.activeCategory) {
      return Promise.resolve()
    }
    this.setData({ activeCategory: category })
    return this.loadContents()
  },

  openDetail(event) {
    const id = event.currentTarget.dataset.id
    if (!id) {
      return
    }
    getWx().navigateTo({ url: `/pages/training/learning-detail/index?id=${id}` })
  },

  goBack() {
    getWx().navigateBack()
  }
})
