const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")
const { registeredPages } = require("./helpers/app-routes")

const root = path.resolve(__dirname, "..")
const read = file => fs.readFileSync(path.join(root, file), "utf8")

test("mine module follows the approved identity and record-card design", () => {
  const app = JSON.parse(read("app.json"))
  const pageConfig = JSON.parse(read("pages/mine/mine.json"))
  const markup = read("pages/mine/mine.wxml")
  const styles = read("pages/mine/mine.wxss")
  const logic = read("pages/mine/mine.js")
  const pages = ["pages/mine-records/index", "pages/settings/index", "pages/profile/index", "pages/help/index", "pages/about/index"]

  for (const page of pages) assert.ok(registeredPages(app).includes(page))
  assert.equal(pageConfig.navigationStyle, "custom")
  assert.equal(pageConfig.usingComponents["green-header"], undefined)
  assert.match(markup, /class="mine-settings-button"/)
  assert.match(markup, /name="setting"/)
  assert.match(markup, /class="identity-avatar"/)
  assert.match(markup, /class="identity-position"/)
  assert.match(markup, /我的工作记录/)
  assert.match(markup, /安全成长/)
  assert.equal((markup.match(/class="record-icon"/g) || []).length, 2)
  assert.match(logic, /\/assets\/mine-icons\/dispatch-record\.webp/)
  assert.match(logic, /\/assets\/mine-icons\/points-detail\.webp/)
  assert.match(logic, /wx\.navigateTo\(\{ url: "\/pages\/settings\/index" \}\)/)
  assert.match(styles, /\.work-grid\s*\{[\s\S]*grid-template-columns:\s*repeat\(2/)
  assert.match(styles, /\.growth-grid\s*\{[\s\S]*grid-template-columns:\s*repeat\(3/)
  assert.match(styles, /\.record-icon\s*\{[\s\S]*width:\s*132rpx;[\s\S]*height:\s*132rpx;/)
  assert.match(styles, /\.work-item\s*\{[\s\S]*gap:\s*2rpx;/)
  assert.match(styles, /\.growth-item\s*\{[\s\S]*gap:\s*2rpx;/)
  assert.doesNotMatch(markup, /section-title-mark/)
  assert.doesNotMatch(styles, /work-item:nth-child/)
  assert.doesNotMatch(styles, /growth-item \+ \.growth-item/)
  assert.doesNotMatch(markup, /个人安全概览/)
  assert.doesNotMatch(markup, /账号与服务/)
  assert.doesNotMatch(markup, /退出登录/)
  assert.doesNotMatch(markup, /<green-header/)
  assert.doesNotMatch(markup, /openAvatarPreview|avatar-preview|从手机相册选择|保存到手机/)
  assert.doesNotMatch(logic, /openAvatarPreview|chooseAvatar|saveAvatarToAlbum|uploadAvatar/)
  assert.doesNotMatch(markup, /我的待办/)
})

test("settings page follows the approved account-and-services design", () => {
  const app = JSON.parse(read("app.json"))
  const pageConfig = JSON.parse(read("pages/settings/index.json"))
  const markup = read("pages/settings/index.wxml")
  const styles = read("pages/settings/index.wxss")
  const logic = read("pages/settings/index.js")

  assert.ok(registeredPages(app).includes("pages/settings/index"))
  assert.equal(pageConfig.navigationStyle, "custom")
  assert.equal(pageConfig.usingComponents["t-icon"], "tdesign-miniprogram/icon/icon")
  assert.match(markup, /class="settings-nav-title">设置/)
  assert.doesNotMatch(markup, /账号与服务/)
  assert.match(markup, /wx:for="\{\{serviceEntries\}\}"/)
  assert.match(markup, /class="settings-status-bar"/)
  assert.match(markup, /name="chevron-left" size="58rpx"/)
  assert.match(markup, /切换账号/)
  assert.match(markup, /退出登录/)
  assert.doesNotMatch(markup, /退出后将清除本机登录信息/)
  assert.match(styles, /\.settings-card,[\s\S]*\.account-action-card\s*\{[\s\S]*border-radius:\s*28rpx/)
  assert.match(styles, /\.settings-row\s*\{[\s\S]*min-height:\s*136rpx/)
  assert.match(styles, /\.settings-nav-title\s*\{[\s\S]*text-align:\s*left/)
  assert.match(styles, /\.settings-nav-title\s*\{[\s\S]*font-size:\s*38rpx/)
  assert.match(styles, /\.account-action-card\s*\{[\s\S]*color:\s*#20262d/)
  assert.match(logic, /\/pages\/profile\/index/)
  assert.match(logic, /\/pages\/help\/index/)
  assert.match(logic, /\/pages\/about\/index/)
  assert.match(logic, /confirmColor:\s*"#e73339"/)
  assert.match(logic, /confirmSwitchAccount/)
  assert.match(logic, /logout\(\)/)
})

test("mine record routes reuse implemented business detail pages", () => {
  global.Page = () => {}
  const { detailRoute } = require("../pages/mine-records/index")

  assert.equal(detailRoute({ type: "dispatch", targetId: "1" }), "/pages/dispatch/detail/index?id=1")
  assert.equal(detailRoute({ type: "three-check", targetId: "2", moduleKey: "pre-shift-meeting" }), "/pages/three-check/meeting-detail/index?id=2")
  assert.equal(detailRoute({ type: "learning", relatedId: "3" }), "/pages/training/learning-detail/index?id=3")
  assert.equal(detailRoute({ type: "points", targetId: "4" }), "")
})

test("profile page follows the approved grouped personal-information design", () => {
  const pageConfig = JSON.parse(read("pages/profile/index.json"))
  const markup = read("pages/profile/index.wxml")
  const styles = read("pages/profile/index.wxss")
  const logic = read("pages/profile/index.js")

  assert.equal(pageConfig.navigationStyle, "custom")
  assert.equal(pageConfig.usingComponents["green-header"], undefined)
  assert.equal(pageConfig.usingComponents["t-icon"], "tdesign-miniprogram/icon/icon")
  assert.match(markup, /class="profile-nav-title">个人资料/)
  assert.match(markup, /class="profile-row profile-avatar-row"/)
  assert.match(markup, /bindtap="openAvatarEditor"/)
  assert.match(markup, /wx:for="\{\{basicRows\}\}"/)
  assert.match(markup, /wx:for="\{\{organizationRows\}\}"/)
  assert.match(markup, /组织与岗位信息由系统管理员统一维护/)
  assert.match(markup, /name="lock-on"/)
  assert.doesNotMatch(markup, /<green-header/)
  assert.doesNotMatch(markup, /class="profile-hero"/)
  assert.match(styles, /\.profile-card\s*\{[\s\S]*border-radius:\s*28rpx/)
  assert.match(styles, /\.profile-avatar\s*\{[\s\S]*width:\s*88rpx;[\s\S]*height:\s*88rpx;[\s\S]*border-radius:\s*24rpx/)
  assert.match(logic, /function groupedRows/)
  assert.match(logic, /openAvatarEditor/)
  assert.match(logic, /wx\.navigateTo\(\{ url: "\/pages\/avatar\/index" \}\)/)
})

test("avatar page follows the supplied dedicated avatar-editor design", () => {
  const app = JSON.parse(read("app.json"))
  const pageConfig = JSON.parse(read("pages/avatar/index.json"))
  const markup = read("pages/avatar/index.wxml")
  const styles = read("pages/avatar/index.wxss")
  const logic = read("pages/avatar/index.js")

  assert.ok(registeredPages(app).includes("pages/avatar/index"))
  assert.equal(pageConfig.navigationStyle, "custom")
  assert.equal(pageConfig.navigationBarTextStyle, "black")
  assert.match(markup, /设置个人头像/)
  assert.match(markup, /class="avatar-preview-panel"/)
  assert.match(markup, /从相册选一张/)
  assert.match(markup, /拍一张照片/)
  assert.match(markup, /设置头像装扮/)
  assert.match(markup, /使用AI头像/)
  assert.match(styles, /\.avatar-preview-panel\s*\{[\s\S]*flex:\s*0 0 750rpx/)
  assert.match(styles, /\.avatar-action-row\s*\{[\s\S]*min-height:\s*112rpx/)
  assert.match(logic, /meService\.uploadAvatar/)
  assert.match(logic, /sourceType:\s*\[sourceType\]/)
})

test("mine module pages use black native status-bar content", () => {
  const pages = [
    "pages/mine/mine.json",
    "pages/mine-records/index.json",
    "pages/profile/index.json",
    "pages/settings/index.json",
    "pages/help/index.json",
    "pages/about/index.json"
  ]

  for (const page of pages) {
    const config = JSON.parse(read(page))
    assert.equal(config.navigationStyle, "custom")
    assert.equal(config.navigationBarTextStyle, "black")
  }
})
