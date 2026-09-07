const assert = require("node:assert/strict")
const test = require("node:test")

function installWx() {
  global.wx = {
    navigateToCalls: [],
    toastCalls: [],
    chosenMedia: "/tmp/after.jpg",
    previewImageCalls: [],
    navigateTo(options) {
      this.navigateToCalls.push(options)
    },
    showToast(options) {
      this.toastCalls.push(options)
    },
    chooseMedia(options) {
      options.success({ tempFiles: [{ tempFilePath: this.chosenMedia }] })
    },
    previewImage(options) {
      this.previewImageCalls.push(options)
    },
    getSystemInfoSync() {
      return { statusBarHeight: 20, windowWidth: 375 }
    },
    getMenuButtonBoundingClientRect() {
      return { top: 26, height: 32, left: 292 }
    },
    switchTab() {},
    navigateBack() {}
  }
  global.getCurrentPages = () => []
}

function capturePage(modulePath) {
  let pageConfig = null
  global.Page = config => {
    pageConfig = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  require(modulePath)
  return pageConfig
}

function mockOrderService(exports) {
  const servicePath = require.resolve("../services/hazard-rectification-order")
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports
  }
}

function mockOrganizationService() {
  const organizationPath = require.resolve("../services/organization")
  require.cache[organizationPath] = {
    id: organizationPath,
    filename: organizationPath,
    loaded: true,
    exports: {
      getOrgTree() {
        return Promise.resolve([
          {
            id: 4,
            title: "广东广晟稀有金属光电新材料有限公司",
            orgType: "COMPANY",
            children: [
              {
                id: 101109,
                title: "安全环保部",
                orgType: "DEPARTMENT",
                children: [
                  { id: 1011001, title: "一班", orgType: "TEAM", children: [] },
                  { id: 1011002, title: "二班", orgType: "TEAM", children: [] }
                ]
              },
              {
                id: 101110,
                title: "生产部",
                orgType: "DEPARTMENT",
                children: [
                  { id: 1011003, title: "三班", orgType: "TEAM", children: [] }
                ]
              }
            ]
          }
        ])
      }
    }
  }
}

function mockThreeCheckService(exports = {}) {
  const servicePath = require.resolve("../services/mini-three-check")
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      openRectificationOrder() {
        return Promise.resolve({ id: "unused" })
      },
      ...exports
    }
  }
}

test("rectification order list loads pending orders and opens detail", async () => {
  installWx()
  const calls = []
  mockOrderService({
    listOrders(query) {
      calls.push(query)
      return Promise.resolve({
        items: [
          {
            id: "81",
            orderNo: "HD-81",
            sourceRecordNo: "SRC-81",
            status: "PENDING_RECTIFY",
            statusLabel: "待整改",
            departmentName: "安全环保部",
            teamName: "幕墙组装班",
            sourceModuleName: "安全检查",
            checkMethod: "四不两直对安全管理情况开展检查,召开现场安全会议,日常检查",
            checkType: "隐患排查室",
            issuerName: "陈主管",
            rectificationResponsiblePerson: "李满发",
            businessDate: "2026-05-29"
          }
        ],
        total: 1
      })
    }
  })

  const page = capturePage("../pages/hazard-rectification/order-list/index")
  await page.onLoad()

  assert.deepEqual(calls[0], { page: 1, pageSize: 20, status: "PENDING_RECTIFY" })
  assert.equal(page.data.orders.length, 1)
  assert.equal(page.data.total, 1)
  assert.equal(page.data.navStatusBarHeight, 20)
  assert.equal(page.data.navBarHeight, 44)
  assert.equal(page.data.navHeight, 64)
  assert.equal(page.data.navTitleLeft, 104)
  assert.equal(page.data.navTitleRight, 104)

  const json = require("node:fs").readFileSync(require.resolve("../pages/hazard-rectification/order-list/index.json"), "utf8")
  assert.match(json, /"navigationStyle":\s*"custom"/)
  assert.match(json, /"t-icon":\s*"tdesign-miniprogram\/icon\/icon"/)
  assert.match(json, /"green-header":\s*"\/components\/ui\/green-header\/index"/)

  const wxml = require("node:fs").readFileSync(require.resolve("../pages/hazard-rectification/order-list/index.wxml"), "utf8")
  assert.doesNotMatch(wxml, /order-building-icon/)
  assert.match(wxml, /<green-header title="隐患整改" bind:back="goBack" \/>/)
  assert.doesNotMatch(wxml, /custom-nav|custom-nav-spacer|custom-nav-back|navTitleLeft|navTitleRight/)
  assert.doesNotMatch(wxml, /<t-tabs/)
  assert.match(wxml, /order-toolbar/)
  assert.match(wxml, /order-date-filter/)
  assert.match(wxml, /name="calendar"/)
  assert.match(wxml, /name="chevron-down"/)
  assert.match(wxml, /bindchange="changeStartDate"/)
  assert.match(wxml, /bindchange="changeEndDate"/)
  assert.match(wxml, /order-tab \{\{status === 'all'/)
  assert.match(wxml, /data-status="PENDING_ASSIGN"/)
  assert.match(wxml, /data-status="PENDING_RECTIFY"/)
  assert.match(wxml, /data-status="PENDING_ACCEPTANCE"/)
  assert.match(wxml, /data-status="CLOSED"/)
  assert.match(wxml, /order-org-filter-row/)
  assert.match(wxml, /order-org-filter/)
  assert.match(wxml, /range="\{\{departmentOptions\}\}"/)
  assert.match(wxml, /range="\{\{teamOptions\}\}"/)
  assert.match(wxml, /range-key="label"/)
  assert.match(wxml, /bindchange="changeDepartment"/)
  assert.match(wxml, /bindchange="changeTeam"/)
  assert.match(wxml, /currentDepartmentLabel/)
  assert.match(wxml, /currentTeamLabel/)
  assert.match(wxml, /name="building"/)
  assert.match(wxml, /name="usergroup"/)
  assert.match(wxml, /<t-loading/)
  assert.match(wxml, /<t-empty/)
  assert.match(wxml, /order-status-pill/)
  assert.match(wxml, /<t-icon/)
  assert.match(wxml, /check-circle/)
  assert.match(wxml, /hourglass/)
  assert.match(wxml, /search/)
  assert.match(wxml, /time/)
  assert.match(wxml, /order-status-\{\{item\.status\}\}/)
  assert.doesNotMatch(wxml, /rectification-topbar/)
  assert.match(wxml, /班组/)
  assert.match(wxml, /teamName \|\| item\.team/)
  assert.match(wxml, /检查方式/)
  assert.match(wxml, /检查类型/)
  assert.match(wxml, /rectificationResponsiblePerson/)
  assert.match(wxml, /<view class="order-card"[^>]*data-id="\{\{item\.id\}\}"[^>]*bindtap="openDetail"/)
  assert.doesNotMatch(wxml, /查看明细|查看详情/)
  assert.doesNotMatch(wxml, /order-detail-btn/)
  assert.match(wxml, /日期/)
  assert.doesNotMatch(wxml, /orderNo|sourceRecordNo|工单号|来源单据/)

  const wxss = require("node:fs").readFileSync(require.resolve("../pages/hazard-rectification/order-list/index.wxss"), "utf8")
  assert.match(wxss, /page\s*\{[\s\S]*background:\s*#f4f6f8/)
  assert.match(wxss, /^page\s*\{[\s\S]*?margin:\s*0;[\s\S]*?padding:\s*0;/m)
  assert.match(wxss, /\.page\s*\{[^}]*padding:\s*0;/)
  assert.match(wxss, /\.order-card-title/)
  assert.match(wxss, /\.order-card-body/)
  assert.match(wxss, /\.order-status-pill/)
  assert.match(wxss, /\.order-status-PENDING_ASSIGN/)
  assert.match(wxss, /\.order-status-CLOSED/)
  assert.match(wxss, /font-size:\s*27rpx/)
  assert.match(wxss, /\.order-date-label/)
  assert.match(wxss, /\.order-card-foot/)
  assert.doesNotMatch(wxss.match(/\.order-card-foot\s*\{[\s\S]*?\}/)[0], /border-top/)
  assert.match(wxss, /--td-brand-color:\s*#1f9d67/)
  assert.match(wxss.match(/\.order-toolbar\s*\{[\s\S]*?\}/)[0], /height:\s*92rpx/)
  assert.match(wxss.match(/\.order-toolbar\s*\{[\s\S]*?\}/)[0], /padding:\s*0 20rpx/)
  assert.match(wxss.match(/\.order-toolbar\s*\{[\s\S]*?\}/)[0], /display:\s*grid/)
  assert.match(wxss.match(/\.order-date-filter\s*\{[\s\S]*?\}/)[0], /justify-self:\s*stretch/)
  assert.match(wxss.match(/\.order-date-filter\s*\{[\s\S]*?\}/)[0], /justify-content:\s*center/)
  assert.match(wxss.match(/\.order-date-filter\s*\{[\s\S]*?\}/)[0], /width:\s*100%/)
  assert.match(wxss.match(/\.order-date-filter\s*\{[\s\S]*?\}/)[0], /max-width:\s*100%/)
  assert.match(wxss.match(/\.order-date-filter\s*\{[\s\S]*?\}/)[0], /border:\s*1rpx solid #d8dee8/)
  assert.match(wxss.match(/\.order-org-filter-row\s*\{[\s\S]*?\}/)[0], /display:\s*grid/)
  assert.match(wxss.match(/\.order-org-filter-row\s*\{[\s\S]*?\}/)[0], /grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/)
  assert.match(wxss.match(/\.order-org-filter\s*\{[\s\S]*?\}/)[0], /height:\s*72rpx/)
  assert.match(wxss.match(/\.order-org-filter\s*\{[\s\S]*?\}/)[0], /border:\s*1rpx solid #d8dee8/)
  assert.match(wxss.match(/\.order-org-label\s*\{[\s\S]*?\}/)[0], /color:\s*#253042/)
  assert.match(wxss.match(/\.order-org-value\s*\{[\s\S]*?\}/)[0], /overflow:\s*hidden/)
  assert.match(wxss.match(/\.order-tabs\s*\{[\s\S]*?\}/)[0], /display:\s*flex/)
  assert.match(wxss.match(/\.order-tabs\s*\{[\s\S]*?\}/)[0], /justify-content:\s*space-around/)
  assert.match(wxss.match(/\.order-tabs\s*\{[\s\S]*?\}/)[0], /overflow-x:\s*auto/)
  assert.match(wxss.match(/\.order-card\s*\{[\s\S]*?\}/)[0], /margin:\s*24rpx 20rpx 16rpx/)
  const orderTabStyle = wxss.match(/\.order-tab\s*\{[\s\S]*?\}/)[0]
  assert.match(orderTabStyle, /background:\s*transparent/)
  assert.match(orderTabStyle, /border:\s*0/)
  assert.match(orderTabStyle, /box-shadow:\s*none/)
  assert.doesNotMatch(orderTabStyle, /border-radius/)
  assert.match(wxss.match(/\.order-tab\.active\s*\{[\s\S]*?\}/)[0], /color:\s*#0b9860/)
  assert.match(wxss.match(/\.order-tab\.active\s*\{[\s\S]*?\}/)[0], /font-weight:\s*900/)
  assert.match(wxss.match(/\.order-tab\.active::after\s*\{[\s\S]*?\}/)[0], /height:\s*4rpx/)
  assert.match(wxss.match(/\.order-tab\.active::after\s*\{[\s\S]*?\}/)[0], /background:\s*#0b9860/)
  assert.doesNotMatch(wxss, /\.order-detail-link/)

  page.openDetail({ currentTarget: { dataset: { id: "81" } } })
  assert.deepEqual(global.wx.navigateToCalls, [
    { url: "/pages/hazard-rectification/order-detail/index?id=81" }
  ])

  await page.changeStatus({ currentTarget: { dataset: { status: "all" } } })
  assert.deepEqual(calls[1], { page: 1, pageSize: 20 })

  await page.changeStatus({ detail: { value: "PENDING_ACCEPTANCE" }, currentTarget: { dataset: {} } })
  assert.deepEqual(calls[2], { page: 1, pageSize: 20, status: "PENDING_ACCEPTANCE" })
})

test("rectification order list applies inline date range immediately", async () => {
  const RealDate = global.Date
  class FrozenDate extends RealDate {
    constructor(...args) {
      if (args.length === 0) {
        super("2026-07-07T08:00:00+08:00")
        return
      }
      super(...args)
    }
    static now() {
      return new RealDate("2026-07-07T08:00:00+08:00").getTime()
    }
  }
  global.Date = FrozenDate

  try {
    installWx()
    const calls = []
    mockOrderService({
      listOrders(query) {
        calls.push(query)
        return Promise.resolve({ items: [], total: 0 })
      }
    })

    const page = capturePage("../pages/hazard-rectification/order-list/index")
    await page.onLoad()

    await page.changeStartDate({ detail: { value: "2026-07-10" } })
    assert.equal(page.data.filterApplied, true)
    assert.equal(page.data.startDate, "2026-07-10")
    assert.equal(page.data.endDate, "2026-07-10")
    assert.deepEqual(calls.find(call => call.dateStart === "2026-07-10" && call.pageSize === 20), {
      page: 1,
      pageSize: 20,
      status: "PENDING_RECTIFY",
      dateStart: "2026-07-10",
      dateEnd: "2026-07-10"
    })

    await page.changeEndDate({ detail: { value: "2026-07-31" } })
    assert.equal(page.data.endDate, "2026-07-31")
    assert.deepEqual(calls.find(call => call.dateEnd === "2026-07-31" && call.pageSize === 20), {
      page: 1,
      pageSize: 20,
      status: "PENDING_RECTIFY",
      dateStart: "2026-07-10",
      dateEnd: "2026-07-31"
    })
  } finally {
    global.Date = RealDate
  }
})

test("rectification order list filters by department and team immediately", async () => {
  installWx()
  mockOrganizationService()
  const calls = []
  mockOrderService({
    listOrders(query) {
      calls.push(query)
      return Promise.resolve({ items: [], total: 0 })
    }
  })

  const page = capturePage("../pages/hazard-rectification/order-list/index")
  await page.onLoad()

  assert.deepEqual(page.data.departmentOptions.map(item => item.label), ["全部部门", "安全环保部", "生产部"])
  assert.deepEqual(page.data.teamOptions.map(item => item.label), ["全部班组", "一班", "二班", "三班"])
  assert.equal(page.data.currentDepartmentLabel, "全部部门")
  assert.equal(page.data.currentTeamLabel, "全部班组")

  await page.changeDepartment({ detail: { value: 1 } })
  assert.equal(page.data.selectedDepartmentId, 101109)
  assert.equal(page.data.selectedTeamId, "")
  assert.deepEqual(page.data.teamOptions.map(item => item.label), ["全部班组", "一班", "二班"])
  assert.deepEqual(calls.find(call => call.departmentId === 101109 && call.pageSize === 20), {
    page: 1,
    pageSize: 20,
    status: "PENDING_RECTIFY",
    departmentId: 101109
  })

  await page.changeTeam({ detail: { value: 2 } })
  assert.equal(page.data.selectedTeamId, 1011002)
  assert.deepEqual(calls.find(call => call.teamId === 1011002 && call.pageSize === 20), {
    page: 1,
    pageSize: 20,
    status: "PENDING_RECTIFY",
    departmentId: 101109,
    teamId: 1011002
  })
})

test("rectification order detail uses the unified green custom header", () => {
  const json = require("node:fs").readFileSync(require.resolve("../pages/hazard-rectification/order-detail/index.json"), "utf8")
  const wxml = require("node:fs").readFileSync(require.resolve("../pages/hazard-rectification/order-detail/index.wxml"), "utf8")
  const wxss = require("node:fs").readFileSync(require.resolve("../pages/hazard-rectification/order-detail/index.wxss"), "utf8")

  assert.match(json, /"navigationStyle":\s*"custom"/)
  assert.match(json, /"green-header":\s*"\/components\/ui\/green-header\/index"/)
  assert.match(wxml, /<green-header title="\{\{order.detailTitle \|\| '隐患整改工单'\}\}" bind:back="goBack" \/>/)
  assert.doesNotMatch(wxml, /custom-nav|custom-nav-spacer|custom-nav-back/)
  assert.match(wxss, /^page\s*\{[\s\S]*?margin:\s*0;[\s\S]*?padding:\s*0;/m)
  assert.match(wxss, /\.page\s*\{[\s\S]*?padding:\s*0 0 40rpx;/)
  assert.match(wxss.match(/\.detail-card\s*\{[\s\S]*?\}/)[0], /margin:\s*0 20rpx 18rpx/)
  assert.match(wxss.match(/\.summary-card\s*\{[\s\S]*?\}/)[0], /margin:\s*0 0 18rpx/)
  assert.doesNotMatch(wxss, /custom-nav|custom-nav-spacer|custom-nav-back|linear-gradient\(135deg,\s*#18b678 0%,\s*#07945f 100%\)/)
})

test("rectification detail uploads after photo and marks order rectified", async () => {
  installWx()
  const calls = []
  mockThreeCheckService()
  mockOrderService({
    getOrder(id) {
      calls.push({ type: "get", id })
      return Promise.resolve({
        id,
        version: 2,
        status: "PENDING_RECTIFY",
        orderNo: "HD-91",
        rectificationAfterPhoto: ""
      })
    },
    uploadOrderAttachment(id, fileKind, filePath) {
      calls.push({ type: "upload", id, fileKind, filePath })
      return Promise.resolve({ url: "/uploads/after.jpg" })
    },
    actionOrder(id, payload) {
      calls.push({ type: "action", id, payload })
      return Promise.resolve({ id, version: 3, status: "RECTIFIED" })
    }
  })

  const page = capturePage("../pages/hazard-rectification/order-detail/index")
  await page.onLoad({ id: "91" })
  page.updateActionField({
    currentTarget: { dataset: { field: "rectificationDescription" } },
    detail: { value: "已清理并拍照" }
  })
  page.chooseAfterPhoto()
  await page.markRectified()

  assert.deepEqual(calls, [
    { type: "get", id: "91" },
    { type: "upload", id: "91", fileKind: "RECTIFICATION_AFTER_PHOTO", filePath: "/tmp/after.jpg" },
    {
      type: "action",
      id: "91",
      payload: {
        action: "MARK_RECTIFIED",
        version: 2,
        payload: {
          rectificationDescription: "已清理并拍照",
          afterPhoto: "/uploads/after.jpg"
        }
      }
    }
  ])
  assert.equal(page.data.order.status, "RECTIFIED")
  assert.deepEqual(global.wx.toastCalls, [{ title: "操作成功", icon: "success" }])
})

test("rectification detail uses pickers, Chinese source text, blank null fields, and photo preview", async () => {
  installWx()
  mockThreeCheckService()
  mockOrderService({
    getOrder(id) {
      return Promise.resolve({
        id,
        version: 1,
        status: "PENDING_ASSIGN",
        sourceModuleKey: "SAFETY_CHECK",
        sourceModuleName: null,
        team: null,
        hazardCount: null,
        rectificationRequirement: null,
        rectificationDeadline: null,
        items: [],
        flowLogs: []
      })
    }
  })

  const page = capturePage("../pages/hazard-rectification/order-detail/index")
  await page.onLoad({ id: "93" })

  assert.equal(page.sourceText(page.data.order), "安全检查")
  assert.equal(page.displayText(null), "")
  page.updateDeadlineDate({ detail: { value: "2026-07-09" } })
  page.updateDeadlineTime({ detail: { value: "18:30" } })
  assert.equal(page.data.actionForm.rectificationDeadline, "2026-07-09 18:30:00")

  page.chooseAfterPhoto()
  page.previewAfterPhoto()
  assert.deepEqual(global.wx.previewImageCalls, [
    { current: "/tmp/after.jpg", urls: ["/tmp/after.jpg"] }
  ])

  const wxml = require("node:fs").readFileSync(require.resolve("../pages/hazard-rectification/order-detail/index.wxml"), "utf8")
  assert.match(wxml, /order\.sourceModuleText/)
  assert.match(wxml, /order\.teamText/)
  assert.match(wxml, /mode="date"/)
  assert.match(wxml, /mode="time"/)
  assert.match(wxml, /deadlineDate/)
  assert.match(wxml, /deadlineTime/)
  assert.doesNotMatch(wxml, /\.slice\(/)
  assert.match(wxml, /previewAfterPhoto/)
  assert.match(wxml, /<image/)
  assert.doesNotMatch(wxml, /rectificationRequirement \|\| '-'/)
  assert.doesNotMatch(wxml, /rectificationDeadline \|\| '-'/)
})

test("rectification detail validates required issue and after-photo fields", async () => {
  installWx()
  const calls = []
  mockThreeCheckService()
  mockOrderService({
    getOrder(id) {
      calls.push({ type: "get", id })
      return Promise.resolve({ id, version: 1, status: "PENDING_ASSIGN" })
    },
    actionOrder(id, payload) {
      calls.push({ type: "action", id, payload })
      return Promise.resolve({ id, version: 2, status: "PENDING_RECTIFY" })
    },
    uploadOrderAttachment() {
      calls.push({ type: "upload" })
      return Promise.resolve({ url: "/uploads/after.jpg" })
    }
  })

  const page = capturePage("../pages/hazard-rectification/order-detail/index")
  await page.onLoad({ id: "92" })
  await page.issueRectification()

  page.setData({ order: { id: "92", version: 2, status: "PENDING_RECTIFY" } })
  await page.markRectified()

  assert.deepEqual(calls, [{ type: "get", id: "92" }])
  assert.deepEqual(global.wx.toastCalls, [
    { title: "请填写责任人与整改要求", icon: "none" },
    { title: "请选择整改后照片", icon: "none" }
  ])
})
