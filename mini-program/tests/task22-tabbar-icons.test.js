const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")

const root = path.resolve(__dirname, "..")

function readJson(file) {
  return JSON.parse(fs.readFileSync(path.join(root, file), "utf8"))
}

function readText(file) {
  return fs.readFileSync(path.join(root, file), "utf8")
}

function reload(modulePath) {
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  return require(modulePath)
}

test("home tab bar renders local icons directly without component slots", () => {
  const appConfig = readJson("app.json")
  const tabBar = reload("../utils/tab-bar")
  const tabBarConfig = readJson("custom-tab-bar/index.json")
  const tabBarScript = readText("custom-tab-bar/index.js")
  const tabBarMarkup = readText("custom-tab-bar/index.wxml")
  const tabBarStyles = readText("custom-tab-bar/index.wxss")
  const appStyles = readText("app.wxss")
  const homeStyles = readText("pages/home/home.wxss")
  const standardTabPageStyles = [
    "pages/analysis/analysis.wxss",
    "pages/message/message.wxss"
  ].map((file) => readText(file))

  assert.equal(appConfig.tabBar.custom, true)
  assert.equal(appConfig.tabBar.color, "#333333")
  assert.equal(appConfig.tabBar.selectedColor, "#1f9d67")
  assert.deepEqual(
    appConfig.tabBar.list.map((item) => [item.pagePath, item.text]),
    [
      ["pages/home/home", "首页"],
      ["pages/analysis/analysis", "数据分析"],
      ["pages/message/message", "消息"],
      ["pages/mine/mine", "我的"]
    ]
  )
  assert.deepEqual(
    appConfig.tabBar.list.map((item) => [item.iconPath, item.selectedIconPath]),
    [
      ["assets/tab-bar/home.png", "assets/tab-bar/home-active.png"],
      ["assets/tab-bar/analysis.png", "assets/tab-bar/analysis-active.png"],
      ["assets/tab-bar/message.png", "assets/tab-bar/message-active.png"],
      ["assets/tab-bar/mine.png", "assets/tab-bar/mine-active.png"]
    ]
  )
  assert.deepEqual(tabBarConfig.usingComponents, {})
  assert.doesNotMatch(tabBarMarkup, /<t-tab-bar|<t-tab-bar-item|slot="icon"/)
  assert.match(tabBarMarkup, /class="pingan-tabbar-shell"/)
  assert.match(tabBarMarkup, /wx:for="\{\{tabItems\}\}"/)
  assert.match(tabBarMarkup, /data-page-path="\{\{item\.pagePath\}\}"/)
  assert.match(tabBarMarkup, /bindtap="onTabTap"/)
  assert.match(tabBarMarkup, /class="pingan-tabbar-icon-wrap"/)
  assert.match(tabBarMarkup, /class="pingan-tabbar-badge"/)
  assert.match(tabBarMarkup, /src="\{\{selected === item\.pagePath \? item\.selectedIconPath : item\.iconPath\}\}"/)
  assert.match(tabBarMarkup, /mode="aspectFit"/)
  assert.match(tabBarMarkup, /<text\s+class="pingan-tabbar-label">\{\{item\.text\}\}<\/text>/)
  assert.doesNotMatch(tabBarMarkup, /pingan-tabbar-bottom-mask/)
  assert.deepEqual(tabBar.TAB_ITEMS.map((item) => item.iconPath), [
    "/assets/tab-bar/home.png",
    "/assets/tab-bar/analysis.png",
    "/assets/tab-bar/message.png",
    "/assets/tab-bar/mine.png"
  ])
  for (const item of tabBar.TAB_ITEMS) {
    assert.equal(fs.existsSync(path.join(root, item.iconPath)), true)
    assert.equal(fs.existsSync(path.join(root, item.selectedIconPath)), true)
  }
  assert.match(tabBarScript, /getUnreadCounts/)
  assert.match(tabBarScript, /onTabTap\(event\)[\s\S]*event\.currentTarget\.dataset\.pagePath/)
  assert.match(tabBarScript, /setData\(\{\s*selected:\s*nextPath\s*\}\)/s)
  assert.match(tabBarStyles, /\.pingan-tabbar-shell\s*\{[\s\S]*position:\s*fixed;[\s\S]*bottom:\s*0;[\s\S]*padding-bottom:\s*env\(safe-area-inset-bottom\);/)
  assert.match(tabBarStyles, /\.pingan-tabbar\s*\{[\s\S]*display:\s*flex;[\s\S]*height:\s*104rpx;/)
  assert.match(tabBarStyles, /\.pingan-tabbar-item\s*\{[\s\S]*display:\s*flex;[\s\S]*align-items:\s*center;[\s\S]*justify-content:\s*center;/)
  assert.match(tabBarStyles, /\.pingan-tabbar-icon\s*\{[\s\S]*width:\s*52rpx;/)
  assert.match(tabBarStyles, /\.pingan-tabbar-icon\s*\{[\s\S]*height:\s*52rpx;/)
  assert.match(tabBarStyles, /\.pingan-tabbar-icon--active\s*\{[\s\S]*opacity:\s*1;/)
  assert.doesNotMatch(tabBarStyles, /filter:/)
  assert.match(tabBarStyles, /\.pingan-tabbar-label\s*\{[\s\S]*display:\s*block;/)
  assert.match(tabBarStyles, /\.pingan-tabbar-label\s*\{[\s\S]*line-height:\s*26rpx;/)
  assert.match(tabBarStyles, /\.pingan-tabbar-label\s*\{[\s\S]*padding-top:\s*8rpx;/)
  assert.match(tabBarStyles, /\.pingan-tabbar-item--active\s*\{[\s\S]*color:\s*#008c62;/)
  assert.match(tabBarStyles, /\.pingan-tabbar-badge\s*\{[\s\S]*display:\s*inline-flex;[\s\S]*align-items:\s*center;[\s\S]*justify-content:\s*center;/)
  assert.match(appStyles, /--pingan-tabbar-content-bottom:\s*calc\(200rpx \+ env\(safe-area-inset-bottom\)\);/)
  assert.match(appStyles, /\.page\s*\{[\s\S]*padding-bottom:\s*var\(--pingan-tabbar-content-bottom\);/)
  assert.match(homeStyles, /padding-bottom:\s*calc\(120rpx \+ env\(safe-area-inset-bottom\)\);/)
  for (const styles of standardTabPageStyles) {
    assert.match(styles, /padding-bottom:\s*var\(--pingan-tabbar-content-bottom\);/)
  }
})

test("tab pages actively sync the custom tab bar selected path", () => {
  const tabBar = reload("../utils/tab-bar")
  const tabPageFiles = [
    "pages/home/home.js",
    "pages/analysis/analysis.js",
    "pages/message/message.js",
    "pages/mine/mine.js"
  ]
  const setDataCalls = []
  const page = {
    getTabBar() {
      return {
        setData(patch) {
          setDataCalls.push(patch)
        }
      }
    }
  }

  assert.equal(tabBar.syncTabBarSelected(page, "pages/analysis/analysis"), true)
  assert.deepEqual(setDataCalls, [{ selected: "/pages/analysis/analysis" }])
  assert.equal(tabBar.syncTabBarSelected(page, "pages/message/message"), true)
  assert.deepEqual(setDataCalls, [
    { selected: "/pages/analysis/analysis" },
    { selected: "/pages/message/message" }
  ])
  assert.equal(tabBar.syncTabBarSelected(page, "pages/points/points"), false)

  for (const file of tabPageFiles) {
    const source = readText(file)
    assert.match(source, /syncTabBarSelected/)
    assert.match(source, /syncTabBarSelected\(this,\s*"pages\//)
  }
})

test("home banners are loaded from remotely served banner assets", () => {
  const homeScript = readText("pages/home/home.js")
  const envScript = readText("config/env.js")
  const env = reload("../config/env")

  assert.match(homeScript, /require\("\.\.\/\.\.\/config\/env"\)/)
  assert.match(homeScript, /assetUrl\("mini-program\/banners\/1\.jpg"\)/)
  assert.match(homeScript, /assetUrl\("mini-program\/banners\/6\.jpg"\)/)
  assert.match(homeScript, /env\.assetBaseUrl/)
  assert.match(envScript, /LAN_HOST/)
  assert.equal(env.currentEnv, "dev")
  assert.equal(env.baseUrl, "http://localhost:8080")
  assert.equal(env.assetBaseUrl, "http://localhost:9000/demo-safeteam")
  assert.doesNotMatch(homeScript, /src:\s*"\/assets\/banners\//)
  assert.doesNotMatch(homeScript, /\/uploads\/\$\{normalizedPath\}/)
})

