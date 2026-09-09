const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")

const projectRoot = path.resolve(__dirname, "..")
const markup = fs.readFileSync(path.join(projectRoot, "pages/login/index.wxml"), "utf8")
const styles = fs.readFileSync(path.join(projectRoot, "pages/login/index.wxss"), "utf8")
test("login page presents generic portfolio branding without internal assets", () => {
  assert.doesNotMatch(markup, /guangsheng-mining|Demo Safety Operations，Demo控股旗下/)
  assert.doesNotMatch(markup, /欢迎登录Demo Safety Operations/)
  assert.match(markup, /更多登录方式/)
  assert.match(markup, /用户协议/)
  assert.doesNotMatch(markup, /assets\/brand\//)
})

test("login page keeps the account and password workflow accessible", () => {
  assert.match(markup, /bindinput="onUsernameInput"/)
  assert.match(markup, /bindinput="onPasswordInput"/)
  assert.match(markup, /bindconfirm="submitLogin"/)
  assert.match(markup, /bindtap="submitLogin"/)
  assert.match(markup, /bindtap="togglePassword"/)
  assert.match(markup, /bindtap="toggleAgreement"/)
  assert.match(styles, /border-radius: 34rpx/)
  assert.match(styles, /background: #dd0716/)
  assert.match(styles, /\.login-background-image/)
  assert.match(styles, /\.login-button\[disabled\]/)
})


