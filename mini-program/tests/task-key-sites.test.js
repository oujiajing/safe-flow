const assert = require("node:assert/strict")
const test = require("node:test")
const fs = require("node:fs")
const path = require("node:path")
const { registeredPages } = require("./helpers/app-routes")

function loadPage(relativePath) {
  let pageConfig = null
  global.Page = config => {
    pageConfig = {
      ...config,
      data: { ...config.data },
      setData(patch) { this.data = { ...this.data, ...patch } }
    }
  }
  const resolved = require.resolve(relativePath)
  delete require.cache[resolved]
  const helpers = require(relativePath)
  return { pageConfig, helpers }
}

test("key sites is a formal mini-program module with dedicated routes", () => {
  const modules = require("../config/modules")
  const keySites = modules.getModuleById("key-place")
  const app = JSON.parse(fs.readFileSync(path.join(__dirname, "../app.json"), "utf8"))

  assert.equal(keySites.enabled, true)
  assert.equal(keySites.mockOnly, false)
  assert.equal(keySites.moduleKey, "key-sites")
  assert.equal(keySites.route, "/pages/key-sites/list/index")
  assert.deepEqual(keySites.permissionCodes, ["PINGAN_KEY_SITES_ENTRY", "PINGAN_KEY_SITES_VIEW"])
  assert.deepEqual(
    registeredPages(app).filter(page => page.startsWith("pages/key-sites/")),
    [
      "pages/key-sites/list/index",
      "pages/key-sites/form/index",
      "pages/key-sites/detail/index",
      "pages/key-sites/inspection/index",
      "pages/key-sites/workflow/index"
    ]
  )
})

test("key sites list derives rectification progress from the linked unified order", () => {
  const { helpers } = loadPage("../pages/key-sites/list/index")

  assert.equal(helpers.rectificationState({ status: "DRAFT", payload: {} }), "pending")
  assert.equal(helpers.rectificationState({ status: "OPENED", payload: { checkItems: [{ rectificationStatus: "PENDING_RECTIFY" }] } }), "rectifying")
  assert.equal(helpers.rectificationState({ status: "OPENED", payload: { checkItems: [{ rectificationStatus: "PENDING_ACCEPTANCE" }] } }), "accepting")
  assert.equal(helpers.rectificationState({ status: "OPENED", payload: { checkItems: [{ rectificationStatus: "CLOSED" }] } }), "closed")
})

test("key sites list filters immediately by date department and site type", () => {
  const { pageConfig, helpers } = loadPage("../pages/key-sites/list/index")
  const rows = [
    { id: "1", state: "pending", date: "2026-07-21", department: "安全部", siteType: "吊装作业区", responsibleDepartment: "幕墙组", responsiblePerson: "甲" },
    { id: "2", state: "pending", date: "2026-07-20", department: "工程部", siteType: "动火作业区", responsibleDepartment: "焊接组", responsiblePerson: "乙" },
    { id: "3", state: "closed", date: "2026-07-21", department: "安全部", siteType: "动火作业区", responsibleDepartment: "焊接组", responsiblePerson: "丙" }
  ]

  assert.deepEqual(helpers.filterOptions(rows, "date", "全部日期", true), ["全部日期", "2026-07-21", "2026-07-20"])
  pageConfig.data = {
    ...pageConfig.data,
    rows,
    dateOptions: ["全部日期", "2026-07-21", "2026-07-20"],
    departmentOptions: ["全部部门", "安全部", "工程部"],
    siteTypeOptions: ["全部类型", "吊装作业区", "动火作业区"]
  }

  pageConfig.changeDateFilter({ detail: { value: "1" } })
  assert.deepEqual(pageConfig.data.visibleRows.map(item => item.id), ["1"])
  pageConfig.changeDepartmentFilter({ detail: { value: "2" } })
  assert.deepEqual(pageConfig.data.visibleRows, [])
  pageConfig.changeDateFilter({ detail: { value: "0" } })
  assert.deepEqual(pageConfig.data.visibleRows.map(item => item.id), ["2"])
  pageConfig.changeSiteTypeFilter({ detail: { value: "1" } })
  assert.deepEqual(pageConfig.data.visibleRows, [])
  pageConfig.changeDepartmentFilter({ detail: { value: "0" } })
  assert.deepEqual(pageConfig.data.visibleRows.map(item => item.id), ["1"])
})

test("key sites detail resolves one linked rectification order from check item backfill", () => {
  const { helpers } = loadPage("../pages/key-sites/detail/index")
  assert.deepEqual(
    helpers.linkedOrder({ payload: { checkItems: [{ rectificationOrderId: "81", rectificationOrderNo: "HZ-81", rectificationStatusLabel: "待整改" }] } }),
    { id: "81", orderNo: "HZ-81", status: "待整改" }
  )
  assert.equal(helpers.linkedOrder({ payload: { checkItems: [] } }), null)
  assert.equal(helpers.statusFor({ status: "DRAFT" }, null), "待检查")
  assert.equal(helpers.statusFor({ status: "OPENED" }, { status: "PENDING_RECTIFY" }), "待整改")
  assert.equal(helpers.statusFor({ status: "OPENED" }, { status: "PENDING_ACCEPTANCE" }), "待验收")
  assert.equal(helpers.statusFor({ status: "OPENED" }, { status: "CLOSED" }), "已闭环")
  assert.equal(helpers.workflowRoute("13", 3), "/pages/key-sites/workflow/index?id=13&step=3")
})

test("key sites detail always renders all four progress numbers", () => {
  const markup = fs.readFileSync(path.join(__dirname, "../pages/key-sites/detail/index.wxml"), "utf8")
  const progress = markup.match(/<view class="ks-progress"[\s\S]*?<\/view>\s*<view class="ks-card/)[0]

  for (const number of [1, 2, 3, 4]) assert.match(progress, new RegExp(`<text>${number}</text>`))
  assert.doesNotMatch(progress, /<t-icon/)
})

test("key sites create supports multiple image and video attachments", () => {
  const { helpers } = loadPage("../pages/key-sites/form/index")
  const selected = helpers.normalizeSelectedMedia([
    { tempFilePath: "wxfile://first.jpg" },
    { tempFilePath: "wxfile://second.jpg" }
  ], "IMAGE")
  const merged = helpers.mergeSelectedMedia([selected[0]], selected)
  const markup = fs.readFileSync(path.join(__dirname, "../pages/key-sites/form/index.wxml"), "utf8")

  assert.deepEqual(merged.map(item => item.path), ["wxfile://first.jpg", "wxfile://second.jpg"])
  assert.match(markup, /data-kind="IMAGE" bindtap="chooseAttachments"/)
  assert.match(markup, /data-kind="VIDEO" bindtap="chooseAttachments"/)
  assert.match(markup, /wx:for="\{\{attachmentImages\}\}"/)
  assert.match(markup, /wx:for="\{\{attachmentVideos\}\}"/)
})

test("key sites attachments open a mixed full-screen media preview", () => {
  const { buildPreviewSources, openMediaPreview } = require("../utils/media-preview")
  const calls = []
  const media = [
    { fileKind: "IMAGE", url: "https://example.com/site.jpg" },
    { fileKind: "VIDEO", url: "https://example.com/site.mp4" }
  ]
  const sources = buildPreviewSources(media)

  assert.deepEqual(sources, [
    { type: "image", url: "https://example.com/site.jpg" },
    { type: "video", url: "https://example.com/site.mp4" }
  ])
  assert.deepEqual(openMediaPreview({ previewMedia(options) { calls.push(options) } }, media, 1), {
    handled: true,
    videoUrl: ""
  })
  assert.equal(calls[0].current, 1)
  assert.deepEqual(calls[0].sources, sources)

  const formMarkup = fs.readFileSync(path.join(__dirname, "../pages/key-sites/form/index.wxml"), "utf8")
  const detailMarkup = fs.readFileSync(path.join(__dirname, "../pages/key-sites/detail/index.wxml"), "utf8")
  assert.match(formMarkup, /bindtap="previewAttachment"/)
  assert.match(formMarkup, /<video src="\{\{item\.path\}\}"[^>]+class="ks-video-poster"/)
  assert.match(detailMarkup, /class="ks-detail-media-grid"/)
  assert.match(detailMarkup, /<video src="\{\{item\.previewUrl\}\}"/)
  assert.match(detailMarkup, /bindtap="previewAttachment"/)
  assert.match(formMarkup, /direction="90"[^>]*show-fullscreen-btn/)
  assert.match(detailMarkup, /direction="90"[^>]*show-fullscreen-btn/)
})

test("key sites inspection keeps stable source line ids and validates hazard details", () => {
  const { pageConfig, helpers } = loadPage("../pages/key-sites/inspection/index")
  const items = helpers.normalizedItems({ id: "91", payload: { checkItems: [{ checkItem: "检查围挡", requireImage: true }] } })
  assert.equal(items[0].lineId, "key-sites-91-1")

  pageConfig.data.checkItems = [{ checkItem: "检查围挡", checkResult: "有隐患", hazardDescription: "", requireImage: true, beforePhoto: "" }]
  assert.equal(pageConfig.validate(), "请填写第 1 项隐患描述")
  pageConfig.data.checkItems[0].hazardDescription = "围挡松动"
  assert.equal(pageConfig.validate(), "请上传第 1 项现场图片")
  pageConfig.data.checkItems[0].beforePhoto = "/uploads/before.jpg"
  assert.equal(pageConfig.validate(), "")
})

test("key sites workflow presents the four visual stages without duplicating rectification actions", () => {
  const { helpers } = loadPage("../pages/key-sites/workflow/index")
  const steps = helpers.buildFlowSteps({
    status: "OPENED",
    owner: "湖贝班长",
    payload: {
      responsiblePerson: "湖贝班长",
      responsibleDepartment: "幕墙组装",
      acceptancePerson: "幕墙安全员",
      checkItems: [{ rectificationStatus: "PENDING_RECTIFY" }]
    }
  }, [])

  assert.deepEqual(steps.map(item => item.title), ["登记检查", "隐患确认", "整改跟进", "验收关闭"])
  assert.equal(steps[2].state, "current")
  assert.equal(helpers.currentNode(steps), "整改跟进")
  assert.equal(steps[0].noteBox, true)
  assert.deepEqual(steps.slice(1).map(item => item.noteBox), [false, false, false])
  assert.equal(helpers.formatWorkflowTime("2026-07-21T22:13:51.844891"), "2026-07-21 22:13:51")

  const markup = fs.readFileSync(path.join(__dirname, "../pages/key-sites/workflow/index.wxml"), "utf8")
  assert.doesNotMatch(markup, /整改提交|验收通过|不通过退回/)
  assert.match(markup, /id="flow-step-\{\{item\.index\}\}"/)
  assert.match(markup, /<view class="ks-flow-marker"><text>\{\{item\.index\}\}<\/text><\/view>/)
})

test("key sites visual sizing stays aligned with the selected 390px mobile reference", () => {
  const common = fs.readFileSync(path.join(__dirname, "../pages/key-sites/common.wxss"), "utf8")
  const list = fs.readFileSync(path.join(__dirname, "../pages/key-sites/list/index.wxss"), "utf8")
  const form = fs.readFileSync(path.join(__dirname, "../pages/key-sites/form/index.wxml"), "utf8")

  assert.match(common, /\.ks-row\s*\{[^}]*min-height:\s*104rpx/s)
  assert.match(common, /\.ks-primary, \.ks-outline\s*\{[^}]*height:\s*88rpx/s)
  assert.match(list, /\.ks-go\s*\{[^}]*width:\s*100% !important;[^}]*height:\s*84rpx/s)
  assert.match(list, /\.ks-fab\s*\{[^}]*width:\s*168rpx !important;[^}]*height:\s*96rpx;[^}]*right:\s*12rpx/s)
  assert.match(list, /\.ks-filter\s*\{[^}]*display:\s*flex;[^}]*justify-content:\s*space-evenly/s)
  assert.match(list, /\.ks-filter-pill\s*\{[^}]*width:\s*116rpx;[^}]*height:\s*54rpx;[^}]*border:\s*0;[^}]*box-shadow:/s)
  assert.match(list, /\.ks-filter-pill-wide\s*\{[^}]*width:\s*156rpx;/s)
  assert.match(form, />提交登记</)
})

test("all key sites pages use the shared green mini-program header", () => {
  const pages = ["list", "form", "detail", "inspection", "workflow"]

  for (const page of pages) {
    const directory = path.join(__dirname, `../pages/key-sites/${page}`)
    const markup = fs.readFileSync(path.join(directory, "index.wxml"), "utf8")
    const config = JSON.parse(fs.readFileSync(path.join(directory, "index.json"), "utf8"))

    assert.match(markup, /<green-header\s+title=/)
    assert.equal(config.navigationStyle, "custom")
    assert.equal(config.navigationBarBackgroundColor, "#078249")
    assert.equal(config.navigationBarTextStyle, "white")
    assert.equal(config.usingComponents["green-header"], "/components/ui/green-header/index")
  }
})

test("key sites form and detail headers sit outside padded content", () => {
  for (const page of ["form", "detail"]) {
    const directory = path.join(__dirname, `../pages/key-sites/${page}`)
    const markup = fs.readFileSync(path.join(directory, "index.wxml"), "utf8")
    const styles = fs.readFileSync(path.join(directory, "index.wxss"), "utf8")

    assert.match(markup, /<view class="ks-page">\s*<green-header[^>]+\/>\s*<view class="ks-page-content">/s)
    assert.match(styles, /\.ks-page\s*\{[^}]*padding:\s*0 0 168rpx;/s)
    assert.match(styles, /\.ks-page-content\s*\{[^}]*padding:/s)
  }
})

test("key sites workflow uses the full-bleed shared header shell", () => {
  const directory = path.join(__dirname, "../pages/key-sites/workflow")
  const markup = fs.readFileSync(path.join(directory, "index.wxml"), "utf8")
  const styles = fs.readFileSync(path.join(directory, "index.wxss"), "utf8")

  assert.match(markup, /<view class="ks-workflow-page">\s*<green-header[^>]+\/>\s*<view class="ks-workflow-content">/s)
  assert.doesNotMatch(styles, /\.ks-workflow-page\s*\{[^}]*padding:/s)
  assert.match(styles, /\.ks-workflow-content\s*\{[^}]*padding:\s*20rpx 24rpx 60rpx;/s)
})
