const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")

const projectRoot = path.resolve(__dirname, "..")
const markup = fs.readFileSync(path.join(projectRoot, "pages/login/index.wxml"), "utf8")
const styles = fs.readFileSync(path.join(projectRoot, "pages/login/index.wxss"), "utf8")
const logoPath = path.join(projectRoot, "assets/brand/guangsheng-mining-login-transparent.webp")
const backgroundPath = path.join(projectRoot, "assets/brand/login-background.webp")

test("login page presents the Guangsheng Mining brand asset", () => {
  assert.match(markup, /guangsheng-mining-login-transparent\.webp/)
  assert.match(markup, /login-background\.webp/)
  assert.match(markup, /Demo Safety Operations，Demo控股旗下/)
  assert.match(markup, /欢迎登录Demo Safety Operations/)
  assert.match(markup, /更多登录方式/)
  assert.match(markup, /用户协议/)
  assert.equal(fs.existsSync(logoPath), true)
  assert.ok(fs.statSync(logoPath).size > 0)
  assert.equal(fs.existsSync(backgroundPath), true)
  assert.ok(fs.statSync(backgroundPath).size > 0)
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


