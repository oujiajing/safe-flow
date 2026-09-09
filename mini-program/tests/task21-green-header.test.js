const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")

const completedModulePages = [
  "pages/dispatch/list/index",
  "pages/dispatch/form/index",
  "pages/dispatch/detail/index",
  "pages/dispatch/record-detail/index",
  "pages/three-check/meeting/index",
  "pages/three-check/meeting-detail/index",
  "pages/three-check/inspection-list/index",
  "pages/three-check/inspection-detail/index",
  "pages/hazard/source-list/index",
  "pages/hazard/source-form/index",
  "pages/hazard/source-detail/index",
  "pages/hazard-rectification/order-list/index",
  "pages/hazard-rectification/order-detail/index",
  "pages/training/learning-list/index",
  "pages/training/learning-detail/index",
  "pages/training/exam-list/index",
  "pages/training/exam-basic/index",
  "pages/training/exam-detail/index",
  "pages/points/flow/index",
  "pages/points/rank/index",
  "pages/special-work/list/index",
  "pages/special-work/form/index",
  "pages/special-work/detail/index",
  "pages/special-work/workflow/index"
]

const legacyHeaderPattern =
  /meeting-statusbar|three-check-statusbar|dispatch-green-topbar|source-topbar|custom-nav|detail-home-title|detail-module-bar|native-bar|native-back|native-title|native-menu-space/

function readMini(file) {
  return fs.readFileSync(path.join(__dirname, "..", file), "utf8")
}

test("green-header component matches the pending-meeting topbar template", () => {
  const json = JSON.parse(readMini("components/ui/green-header/index.json"))
  const js = readMini("components/ui/green-header/index.js")
  const wxml = readMini("components/ui/green-header/index.wxml")
  const wxss = readMini("components/ui/green-header/index.wxss")

  assert.equal(json.component, true)
  assert.equal(json.styleIsolation, "isolated")
  assert.equal(json.usingComponents["t-icon"], "tdesign-miniprogram/icon/icon")
  assert.match(wxml, /green-header-statusbar/)
  assert.match(wxml, /green-header-bar/)
  assert.match(wxml, /green-header-title/)
  assert.match(wxml, /name="chevron-left"/)
  assert.match(wxml, /size="\{\{headerTokens.backIconSize\}\}"/)
  assert.match(wxml, /color="\{\{headerTokens.titleColor\}\}"/)
  assert.match(wxml, /bindtap="handleBack"/)
  assert.match(wxss, /--green-header-bg:\s*#078249;/)
  assert.match(wxss, /--green-header-title-color:\s*#ffffff;/)
  assert.match(wxss, /--green-header-title-size:\s*34rpx;/)
  assert.match(wxss, /--green-header-title-weight:\s*900;/)
  assert.match(wxss, /--green-header-back-left:\s*28rpx;/)
  assert.match(wxss, /--green-header-menu-width:\s*188rpx;/)
  assert.match(wxss, /\.green-header\s*\{[\s\S]*?width:\s*100%;[\s\S]*?margin-left:\s*0;[\s\S]*?background:\s*#078249;[\s\S]*?overflow:\s*visible;/)
  assert.match(wxss, /\.green-header-statusbar\s*\{[\s\S]*?width:\s*100%;[\s\S]*?height:\s*28px;[\s\S]*?margin-left:\s*0;[\s\S]*?background:\s*#078249;/)
  assert.match(wxss, /\.green-header-bar\s*\{[\s\S]*?width:\s*100%;[\s\S]*?height:\s*54px;[\s\S]*?margin-left:\s*0;[\s\S]*?background:\s*#078249;/)
  assert.match(wxss, /\.green-header-bar\s*\{[\s\S]*?display:\s*grid;[\s\S]*?grid-template-columns:\s*188rpx minmax\(0,\s*1fr\) 188rpx;[\s\S]*?align-items:\s*center;/)
  assert.match(wxml, /green-header-statusbar" style="height: \{\{statusBarHeight\}\}px;"/)
  assert.match(wxml, /height: \{\{navigationBarHeight\}\}px; grid-template-columns: \{\{menuSpaceWidth\}\}px minmax\(0, 1fr\) \{\{menuSpaceWidth\}\}px;/)
  assert.match(js, /getMenuButtonBoundingClientRect/)
  assert.match(js, /Math\.max\(menuButton\.height,\s*\(menuButton\.top - statusBarHeight\) \* 2 \+ menuButton\.height\)/)
  assert.match(js, /Math\.max\(88,\s*windowWidth - menuButton\.left\)/)
  const leftBlock = wxss.match(/\.green-header-left\s*\{[\s\S]*?\}/)[0]
  assert.match(leftBlock, /display:\s*flex;/)
  assert.match(leftBlock, /align-items:\s*center;/)
  assert.match(leftBlock, /width:\s*188rpx;/)
  assert.match(leftBlock, /height:\s*54px;/)
  assert.match(wxss, /\.green-header-title\s*\{[\s\S]*?color:\s*#ffffff;[\s\S]*?font-size:\s*34rpx;[\s\S]*?font-weight:\s*900;[\s\S]*?line-height:\s*48rpx;[\s\S]*?text-align:\s*center;/)
  assert.match(wxss, /\.green-header-title\s*\{[\s\S]*?position:\s*static;[\s\S]*?display:\s*flex;[\s\S]*?align-items:\s*center;[\s\S]*?justify-content:\s*center;/)
  assert.doesNotMatch(wxss.match(/\.green-header-title\s*\{[\s\S]*?\}/)[0], /position:\s*absolute|top:\s*50%|transform:\s*translateY\(-50%\)/)
  assert.doesNotMatch(wxss.match(/\.green-header-back\s*\{[\s\S]*?\}/)[0], /position:\s*absolute|top:\s*50%|transform:\s*translateY\(-50%\)/)
  assert.doesNotMatch(wxss, /\.green-header\s*\{[\s\S]*?overflow-x:\s*hidden;/)
  assert.doesNotMatch(wxss, /calc\(100vw \+/)
  assert.doesNotMatch(wxss, /50vw/)
  assert.doesNotMatch(wxss, /margin-left:\s*calc\(/)
  assert.doesNotMatch(wxss, /\.green-header-statusbar\s*\{[\s\S]*?calc\(100vw \+ 6rpx\)/)
  assert.doesNotMatch(wxss, /\.green-header-bar\s*\{[\s\S]*?calc\(100vw \+ 6rpx\)/)
  assert.doesNotMatch(wxss, /\.green-header-bar\s*\{[\s\S]*?border-bottom:\s*1rpx solid #078249;/)
  assert.doesNotMatch(wxss, /linear-gradient/)
  assert.doesNotMatch(wxss, /180rpx/)
})

test("home header fills the viewport with the reference title row", () => {
  const wxml = readMini("pages/home/home.wxml")
  const wxss = readMini("pages/home/home.wxss")

  assert.match(wxss, /\.top-area\s*\{[\s\S]*?left:\s*0;[\s\S]*?right:\s*0;/)
  assert.match(wxml, /class="home-custom-nav"/)
  assert.match(wxml, /class="home-nav-title"[^>]*>平安班组微应用<\/view>/)
  assert.match(wxml, /top-area-spacer" style="height: calc\(\{\{statusBarHeight\}\}px \+ \{\{navigationBarHeight\}\}px \+ \{\{profileAreaHeightRpx\}\}rpx\);"/)
  assert.doesNotMatch(wxss, /\.top-area\s*\{[\s\S]*?left:\s*-2rpx;/)
})

test("dispatch pages keep the shared header outside padded content", () => {
  const formWxss = readMini("pages/dispatch/form/index.wxss")
  const listWxss = readMini("pages/dispatch/list/index.wxss")

  assert.match(formWxss, /\.dispatch-page\s*\{[\s\S]*?padding:\s*0 !important;[\s\S]*?box-sizing:\s*border-box;/)
  assert.match(formWxss, /\.dispatch-page\s*\{[\s\S]*?overflow-x:\s*visible !important;/)
  assert.match(formWxss, /\.dispatch-hero-card\s*\{[\s\S]*?margin:\s*24rpx 24rpx 0;/)
  assert.match(formWxss, /\.dispatch-form-card\s*\{[\s\S]*?margin:\s*30rpx 24rpx 0;/)
  assert.doesNotMatch(formWxss, /\.dispatch-page\s*\{[\s\S]*?padding:\s*0 24rpx;/)

  assert.match(listWxss, /\.dispatch-report-page\s*\{[\s\S]*?padding:\s*0 0 48rpx;/)
  assert.match(listWxss, /\.dispatch-tabs\s*\{[\s\S]*?width:\s*100%;/)
  assert.match(listWxss, /\.dispatch-filter-row\s*\{[\s\S]*?width:\s*100%;/)
  assert.match(listWxss, /\.dispatch-summary-grid\s*\{[\s\S]*?margin:\s*22rpx 24rpx;/)
  assert.match(listWxss, /\.dispatch-table-scroll\s*\{[\s\S]*?margin:\s*0 24rpx;/)
  assert.doesNotMatch(listWxss, /\.dispatch-report-page\s*\{[\s\S]*?padding:\s*0 24rpx 48rpx;/)
})

test("completed mini-program module pages use the shared green-header", () => {
  for (const page of completedModulePages) {
    const json = JSON.parse(readMini(`${page}.json`))
    const wxml = readMini(`${page}.wxml`)
    assert.equal(json.navigationStyle, "custom", page)
    assert.equal(json.usingComponents["green-header"], "/components/ui/green-header/index", page)
    assert.match(wxml, /<green-header[\s\S]*?bind:back="goBack"/, page)
    assert.doesNotMatch(wxml, legacyHeaderPattern, page)
    const wxss = readMini(`${page}.wxss`)
    assert.doesNotMatch(wxss, legacyHeaderPattern, page)
    assert.doesNotMatch(wxss, /green-header-(title|back|bar|statusbar|menu-space)/, page)
  }
})
