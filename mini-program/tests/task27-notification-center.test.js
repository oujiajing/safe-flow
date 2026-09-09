const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")

const root = path.resolve(__dirname, "..")

function readText(file) {
  return fs.readFileSync(path.join(root, file), "utf8")
}

function readJson(file) {
  return JSON.parse(readText(file))
}

function reload(modulePath) {
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  return require(modulePath)
}

test("notification center is a real API-backed secondary page", () => {
  const config = readJson("pages/message/message.json")
  const script = readText("pages/message/message.js")
  const markup = readText("pages/message/message.wxml")
  const styles = readText("pages/message/message.wxss")

  assert.equal(config.navigationStyle, "custom")
  assert.equal(config.enablePullDownRefresh, true)
  assert.equal(config.usingComponents["green-header"], "/components/ui/green-header/index")
  assert.match(script, /require\("\.\.\/\.\.\/services\/notification"\)/)
  assert.match(script, /notificationService\.listNotifications/)
  assert.match(script, /notificationService\.markAllNotificationsRead/)
  assert.match(script, /notificationService\.resolveNotificationAction/)
  assert.match(script, /visibleCategories/)
  assert.doesNotMatch(script, /Promise\.all\(/)
  assert.doesNotMatch(script, /reminderMessages|keyPlaceReminderMessage|getStorageSync/)
  assert.doesNotMatch(markup, /消息概览|summary-card|summary-grid/)
  assert.match(markup, /show-back="\{\{viewMode === 'detail'\}\}"/)
  assert.match(script, /syncTabBarSelected\(this,\s*"pages\/message\/message"\)/)
  assert.match(styles, /\.message-page\s*\{[\s\S]*padding-bottom:\s*var\(--pingan-tabbar-content-bottom\);/)
  assert.match(script, /待我处理/)
  assert.match(script, /业务通知/)
  assert.match(script, /系统公告/)
  assert.match(markup, /全部已读/)
  assert.doesNotMatch(markup, /只看未读|toggleUnread|unreadOnly/)
  assert.doesNotMatch(script, /toggleUnread|unreadOnly/)
  assert.match(markup, /mode="date"/)
  assert.match(markup, /bindchange="changeDate"/)
  assert.match(markup, /bindtap="clearDate"/)
  assert.match(markup, /\{\{item\.timeLabel\}\}/)
  assert.match(script, /dateStart:\s*this\.data\.filterDate/)
  assert.match(script, /dateEnd:\s*this\.data\.filterDate/)
  assert.match(script, /return `\$\{month\}月\$\{day\}日 \$\{hour\}：\$\{minute\}`/)
  assert.match(markup, /wx:if="\{\{item\.cardTitle\}\}" class="message-title"/)
  assert.match(markup, /item\.teamName/)
  assert.match(markup, /<text>\{\{item\.responsibleName\}\}<\/text>/)
  assert.doesNotMatch(markup, /name="usergroup"|name="user"|name="time"/)
  assert.match(markup, /message-card-head[\s\S]*status-pill[\s\S]*message-time/)
  assert.doesNotMatch(markup, /负责人 \{\{item\.responsibleName\}\}/)
  assert.doesNotMatch(markup, /<view class="message-summary">\{\{item\.summary\}\}<\/view>/)
  assert.match(script, /assignment-checked/)
  assert.match(script, /task-checked/)
  assert.match(script, /check-circle/)
  assert.match(script, /"hazard-rectification":\s*\{\s*icon:\s*"shield-error"/)
  assert.match(script, /"special-work":\s*\{\s*icon:\s*"tools"/)
  assert.doesNotMatch(script, /icon:\s*"(?:shield|fire)"/)
  const integratedSources = [
    "pre-shift-meeting",
    "pre-shift-inspection",
    "mid-shift-inspection",
    "post-shift-inspection",
    "team-dispatch",
    "hazard-source",
    "hazard-rectification",
    "special-work",
    "safety-exam",
    "safety-learning",
    "risk-control",
    "key-site",
    "safety-points",
    "safety-ledger",
    "system-account",
    "system-import",
    "system-announcement"
  ]
  for (const source of integratedSources) {
    assert.match(script, new RegExp(`"${source}"\\s*:\\s*\\{\\s*icon:\\s*"[^"]+"`))
  }
  assert.match(markup, /name="\{\{item\.icon\}\}"/)
  assert.match(markup, /module-icon--\{\{item\.tone\}\}/)
  assert.match(script, /function conciseTitle/)
  assert.match(script, /groupType === "ACTION"/)
  assert.match(script, /任务\)\?待/)
  assert.ok(script.includes("(?:任务|内容|检查)?已(?:下发|分派|逾期)"))
  assert.doesNotMatch(script, /新整改任务已分派|新的验收任务/)
  assert.doesNotMatch(script, /return "新任务已分派"/)
  assert.doesNotMatch(markup, /class="message-action"/)
  assert.match(markup, /class="detail-primary"[\s\S]*bindtap="handleAction"/)
  assert.doesNotMatch(markup, /标为未读|标为已读|不可操作原因|actionUnavailableReason/)
  assert.doesNotMatch(script, /toggleDetailUnread/)
  assert.doesNotMatch(markup, /tab-bar|底部导航/)
  assert.doesNotMatch(styles, /\.summary-card|\.summary-grid|\.summary-item/)
  assert.match(styles, /\.message-card\s*\{[\s\S]*grid-template-columns:/)
  assert.match(styles, /\.status-pill\.danger/)
  assert.match(styles, /\.unread-dot\s*\{[\s\S]*background:\s*#0a9a62/)
  assert.match(styles, /\.message-context\s*\{/)
  assert.match(styles, /\.message-card-icon\s*\{[\s\S]*width:\s*96rpx;[\s\S]*height:\s*96rpx;/)
  assert.match(styles, /\.message-card-head\s*\{[\s\S]*justify-content:\s*space-between;/)
  assert.match(styles, /\.message-card:not\(:last-child\)::after\s*\{[\s\S]*left:\s*130rpx;/)
  assert.doesNotMatch(styles, /\.message-action\s*\{/)
  assert.doesNotMatch(styles, /\.message-main\s*\{[\s\S]*padding-right:/)
  assert.doesNotMatch(styles, /\.detail-secondary/)
})

test("notification service maps list counts and read endpoints", async () => {
  const calls = []
  global.wx = {
    getStorageSync() { return "" },
    request(options) {
      calls.push({ url: options.url, method: options.method, data: options.data })
      options.success({ statusCode: 200, data: { code: 0, data: {} } })
    }
  }
  const service = reload("../services/notification")

  await service.listNotifications({
    groupType: "ACTION",
    moduleKey: undefined,
    unread: undefined,
    handlingStatus: null,
    dateStart: "2026-07-24",
    dateEnd: "2026-07-24",
    page: 1
  })
  await service.getUnreadCounts()
  await service.markNotificationRead("12")
  await service.markNotificationUnread("12")
  await service.resolveNotificationAction("12")
  await service.markAllNotificationsRead({
    groupType: "ACTION",
    moduleKey: "special-work",
    dateStart: "2026-07-24",
    dateEnd: "2026-07-24"
  })

  assert.match(calls[0].url, /\/api\/mini\/notifications$/)
  assert.equal(calls[0].method, "GET")
  assert.deepEqual(calls[0].data, {
    groupType: "ACTION",
    dateStart: "2026-07-24",
    dateEnd: "2026-07-24",
    page: 1
  })
  assert.match(calls[1].url, /\/api\/mini\/notifications\/unread-counts$/)
  assert.match(calls[2].url, /\/api\/mini\/notifications\/12\/read$/)
  assert.match(calls[3].url, /\/api\/mini\/notifications\/12\/unread$/)
  assert.match(calls[4].url, /\/api\/mini\/notifications\/12\/resolve-action$/)
  assert.match(calls[5].url, /\/api\/mini\/notifications\/read-all\?groupType=ACTION&moduleKey=special-work&dateStart=2026-07-24&dateEnd=2026-07-24$/)
})

test("notification business actions only resolve controlled existing routes", () => {
  const { getNotificationBusinessRoute, getNotificationResolvedRoute } = reload("../utils/notification-routes")

  assert.equal(
    getNotificationBusinessRoute({ moduleKey: "pre-shift-meeting", bizId: "10" }),
    "/pages/three-check/meeting-detail/index?id=10"
  )
  assert.equal(
    getNotificationBusinessRoute({ moduleKey: "mid-shift-inspection", bizId: "11" }),
    "/pages/three-check/inspection-detail/index?id=11&moduleId=during-check&moduleKey=mid-shift-inspection"
  )
  assert.equal(
    getNotificationBusinessRoute({ moduleKey: "hazard-rectification", bizId: "12" }),
    "/pages/hazard-rectification/order-detail/index?id=12"
  )
  assert.equal(
    getNotificationBusinessRoute({ moduleKey: "special-work", bizId: "13" }),
    "/pages/special-work/detail/index?id=13"
  )
  assert.equal(getNotificationBusinessRoute({ moduleKey: "unknown", bizId: "14" }), "")
  assert.equal(
    getNotificationResolvedRoute("TEAM_DISPATCH_DETAIL", { id: "15" }),
    "/pages/dispatch/detail/index?id=15"
  )
  assert.equal(
    getNotificationResolvedRoute("SAFETY_EXAM_DETAIL", { id: "16" }),
    "/pages/training/exam-detail/index?id=16"
  )
  assert.equal(getNotificationResolvedRoute("UNTRUSTED_URL", { id: "17" }), "")
})
