const STORAGE_KEY = "analysis.filterContext"

function pad(value) {
  return String(value).padStart(2, "0")
}

function formatDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function parseDate(value) {
  const parts = String(value || "").split("-").map(Number)
  if (parts.length !== 3 || parts.some(Number.isNaN)) return new Date()
  return new Date(parts[0], parts[1] - 1, parts[2])
}

function addDays(date, days) {
  const next = new Date(date.getTime())
  next.setDate(next.getDate() + days)
  return next
}

function createDefaultContext(today = new Date()) {
  const dateEnd = formatDate(today)
  return {
    orgId: "",
    orgName: "全部可见组织",
    rangeLabel: "近7天",
    dateStart: formatDate(addDays(today, -6)),
    dateEnd
  }
}

function normalizeContext(value, today = new Date()) {
  const fallback = createDefaultContext(today)
  const source = value && typeof value === "object" ? value : {}
  let dateStart = source.dateStart || fallback.dateStart
  let dateEnd = source.dateEnd || fallback.dateEnd
  if (parseDate(dateStart) > parseDate(dateEnd)) {
    const swapped = dateStart
    dateStart = dateEnd
    dateEnd = swapped
  }
  return {
    orgId: source.orgId === undefined || source.orgId === null ? "" : source.orgId,
    orgName: source.orgName || fallback.orgName,
    rangeLabel: source.rangeLabel || "自定义",
    dateStart,
    dateEnd
  }
}

function getStoredContext(today = new Date()) {
  if (typeof wx === "undefined" || !wx.getStorageSync) return createDefaultContext(today)
  return normalizeContext(wx.getStorageSync(STORAGE_KEY), today)
}

function setStoredContext(context) {
  const normalized = normalizeContext(context)
  if (typeof wx !== "undefined" && wx.setStorageSync) {
    wx.setStorageSync(STORAGE_KEY, normalized)
  }
  return normalized
}

function contextKey(context) {
  const value = normalizeContext(context)
  return [value.orgId, value.dateStart, value.dateEnd].join("|")
}

function toQuery(context) {
  const value = normalizeContext(context)
  return {
    dateStart: value.dateStart,
    dateEnd: value.dateEnd,
    ...(value.orgId !== "" ? { orgId: value.orgId } : {})
  }
}

function shortDate(value) {
  return String(value || "").slice(5)
}

function rangeText(context) {
  const value = normalizeContext(context)
  return `${value.rangeLabel}  ${shortDate(value.dateStart)} 至 ${shortDate(value.dateEnd)}`
}

module.exports = {
  STORAGE_KEY,
  addDays,
  contextKey,
  createDefaultContext,
  formatDate,
  getStoredContext,
  normalizeContext,
  rangeText,
  setStoredContext,
  shortDate,
  toQuery
}
