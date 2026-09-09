const { request } = require("../utils/request")

const BASE_PATH = "/api/mini/notifications"

function compactQuery(query = {}) {
  return Object.keys(query).reduce((result, key) => {
    const value = query[key]
    if (value !== undefined && value !== null && value !== "") {
      result[key] = value
    }
    return result
  }, {})
}

function listNotifications(query = {}) {
  return request({ url: BASE_PATH, method: "GET", data: compactQuery(query) })
}

function getUnreadCounts() {
  return request({ url: `${BASE_PATH}/unread-counts`, method: "GET" })
}

function getNotification(id) {
  return request({ url: `${BASE_PATH}/${id}`, method: "GET" })
}

function markNotificationRead(id) {
  return request({ url: `${BASE_PATH}/${id}/read`, method: "POST" })
}

function markNotificationUnread(id) {
  return request({ url: `${BASE_PATH}/${id}/unread`, method: "POST" })
}

function markAllNotificationsRead(query = {}) {
  const compacted = compactQuery(query)
  const params = Object.keys(compacted)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(compacted[key])}`)
    .join("&")
  return request({ url: `${BASE_PATH}/read-all${params ? `?${params}` : ""}`, method: "POST" })
}

function resolveNotificationAction(id) {
  return request({ url: `${BASE_PATH}/${id}/resolve-action`, method: "POST" })
}

module.exports = {
  BASE_PATH,
  compactQuery,
  listNotifications,
  getUnreadCounts,
  getNotification,
  markNotificationRead,
  markNotificationUnread,
  markAllNotificationsRead,
  resolveNotificationAction
}
