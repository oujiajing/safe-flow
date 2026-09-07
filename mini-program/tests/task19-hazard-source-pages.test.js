const assert = require("node:assert/strict")
const test = require("node:test")

function installWx() {
  global.wx = {
    navigateToCalls: [],
    toastCalls: [],
    chooseMediaCalls: [],
    previewImageCalls: [],
    chosenMedia: "/tmp/hazard.jpg",
    navigateTo(options) {
      this.navigateToCalls.push(options)
    },
    showToast(options) {
      this.toastCalls.push(options)
    },
    chooseMedia(options) {
      this.chooseMediaCalls.push(options)
      options.success({
        tempFiles: this.chosenMediaTempFiles || [{ tempFilePath: this.chosenMedia, fileType: "image" }]
      })
    },
    previewImage(options) {
      this.previewImageCalls.push(options)
    },
    setNavigationBarTitle() {},
    navigateBack() {}
  }
}

function capturePage(modulePath) {
  let pageConfig = null
  global.Page = config => {
    pageConfig = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
        Object.keys(patch).forEach(key => {
          if (!key.includes(".")) return
          const parts = key.split(".")
          let target = this.data
          for (let index = 0; index < parts.length - 1; index += 1) {
            target[parts[index]] = target[parts[index]] || {}
            target = target[parts[index]]
          }
          target[parts[parts.length - 1]] = patch[key]
        })
      }
    }
  }
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  require(modulePath)
  return pageConfig
}

function mockSourceService(exports) {
  const servicePath = require.resolve("../services/hazard-source-record")
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
            title: "Demo Works Company",
            orgType: "COMPANY",
            children: [
              {
                id: 101109,
                title: "幕墙组装",
                orgType: "DEPARTMENT",
                children: [
                  { id: 1011001, title: "幕墙组装1班", orgType: "TEAM", children: [] }
                ]
              }
            ]
          }
        ])
      }
    }
  }
}

function mockOrganizationServiceWithTwoTeams() {
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
            title: "Demo Materials Company",
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

function mockOrganizationServiceWithTwoCompanies() {
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
            title: "Demo Materials Company",
            orgType: "COMPANY",
            children: [
              {
                id: 101109,
                title: "安全环保部",
                orgType: "DEPARTMENT",
                children: [
                  { id: 1011001, title: "一班", orgType: "TEAM", children: [] }
                ]
              }
            ]
          },
          {
            id: 5,
            title: "海安加工厂",
            orgType: "COMPANY",
            children: [
              {
                id: 201109,
                title: "生产部",
                orgType: "DEPARTMENT",
                children: [
                  { id: 2011001, title: "甲班", orgType: "TEAM", children: [] },
                  { id: 2011002, title: "乙班", orgType: "TEAM", children: [] }
                ]
              },
              {
                id: 201110,
                title: "质检部",
                orgType: "DEPARTMENT",
                children: [
                  { id: 2011003, title: "质检一班", orgType: "TEAM", children: [] }
                ]
              }
            ]
          }
        ])
      }
    }
  }
}

function mockSafetyCheckOrganizationService() {
  const organizationPath = require.resolve("../services/organization")
  require.cache[organizationPath] = {
    id: organizationPath,
    filename: organizationPath,
    loaded: true,
    exports: {
      getOrgTree() {
        return Promise.resolve([
          {
            id: 1,
            title: "广东省Demo控股集团有限公司",
            orgType: "GROUP",
            children: [
              {
                id: 4,
                title: "Demo Materials Company",
                orgType: "COMPANY",
                children: []
              },
              {
                id: 5,
                title: "Demo Materials Company",
                orgType: "COMPANY",
                children: []
              }
            ]
          }
        ])
      }
    }
  }
}

test("hazard source list loads records and opens create/detail pages", async () => {
  installWx()
  const calls = []
  mockSourceService({
    listHazardRecords(moduleKey, query) {
      calls.push({ type: "list", moduleKey, query })
      return Promise.resolve({
        items: [{ id: "11", status: "DRAFT", payload: { hazardDescription: "通道堆物", location: "一车间" } }],
        total: 1
      })
    }
  })

  const page = capturePage("../pages/hazard/source-list/index")
  await page.onLoad({ moduleKey: "quick-shot", title: "随手拍" })

  assert.equal(page.data.records.length, 1)
  assert.deepEqual(calls[0], {
    type: "list",
    moduleKey: "quick-shot",
    query: { page: 1, pageSize: 20, status: "REVIEWED" }
  })

  await page.changeStatus({ currentTarget: { dataset: { status: "OPENED" } } })
  assert.ok(calls.some(call => call.query.status === "OPENED" && call.query.pageSize === 20))

  page.openCreate()
  page.openDetail({ currentTarget: { dataset: { id: "11" } } })

  assert.deepEqual(global.wx.navigateToCalls, [
    { url: "/pages/hazard/source-form/index?moduleKey=quick-shot&title=%E9%9A%8F%E6%89%8B%E6%8B%8D&companyKey=gs" },
    { url: "/pages/hazard/source-detail/index?id=11&moduleKey=quick-shot&title=%E9%9A%8F%E6%89%8B%E6%8B%8D&companyKey=gs" }
  ])
})

test("quick-shot source list defaults to pending review tabs and renders screenshot actions", async () => {
  installWx()
  const calls = []
  const totals = {
    PENDING_REVIEW: 3,
    REVIEWED: 9,
    REJECTED: 1,
    ACCEPTED: 0
  }
  mockSourceService({
    listHazardRecords(moduleKey, query) {
      calls.push({ type: "list", moduleKey, query })
      if (query.pageSize === 1) {
        return Promise.resolve({ items: [], total: totals[query.status] || 0 })
      }
      return Promise.resolve({
        items: [
          {
            id: "11",
            status: "REVIEWED",
            statusLabel: "待审批",
            payload: {
              hazardDescription: "通道堆物",
              departmentName: "安全环保部",
              teamName: "一班"
            },
            companyName: "Demo Materials Company",
            departmentName: "安全环保部",
            businessDate: "2026-07-01"
          }
        ],
        total: totals[query.status] || 0
      })
    }
  })

  const page = capturePage("../pages/hazard/source-list/index")
  await page.onLoad({ moduleKey: "quick-shot", title: "%E9%9A%8F%E6%89%8B%E6%8B%8D" })

  assert.equal(page.data.status, "REVIEWED")
  assert.equal(page.data.title, "随手拍")
  assert.equal(page.data.total, 9)
  assert.deepEqual(calls[0], {
    type: "list",
    moduleKey: "quick-shot",
    query: { page: 1, pageSize: 20, status: "REVIEWED" }
  })

  page.openRectificationOrder({ currentTarget: { dataset: { id: "11" } } })
  page.openDetail({ currentTarget: { dataset: { id: "11" } } })
  assert.deepEqual(global.wx.navigateToCalls, [
    { url: "/pages/hazard-rectification/order-detail/index?moduleKey=quick-shot&sourceRecordId=11" },
    { url: "/pages/hazard/source-detail/index?id=11&moduleKey=quick-shot&title=%E9%9A%8F%E6%89%8B%E6%8B%8D&companyKey=gs" }
  ])

  assert.deepEqual(page.data.tabs.map(item => item.label), ["待审批 (3)", "通过 (9)", "不通过 (1)", "已闭环 (0)"])
  assert.equal(page.data.tabs.find(item => item.label === "已闭环 (0)").status, "ACCEPTED")
  assert.equal(calls.some(call => call.query.status === "CLOSED"), false)
  assert.equal(page.data.records[0].displayTeamName, "一班")
  const wxml = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-list/index.wxml"), "utf8")
  assert.match(wxml, /开整改单/)
  assert.match(wxml, /<view class="source-record source-record-card quick-shot-record-card"[^>]*data-id="\{\{item\.id\}\}"[^>]*bindtap="openDetail"/)
  assert.doesNotMatch(wxml, /查看明细|查看详情/)
  assert.doesNotMatch(wxml, /[⊕▽▦]/)
  assert.doesNotMatch(wxml, /displayCompanyName/)
  assert.doesNotMatch(wxml, /source-company-icon/)
  assert.doesNotMatch(wxml, /source-record-company/)
  assert.match(wxml, /source-toolbar-inline/)
  assert.match(wxml, /source-date-filter/)
  assert.match(wxml, /name="add-circle"/)
  assert.match(wxml, /name="calendar"/)
  assert.match(wxml, /name="chevron-down"/)
  assert.match(wxml, /bindchange="changeInlineStartDate"/)
  assert.match(wxml, /bindchange="changeInlineEndDate"/)
  assert.match(wxml, /source-org-filter-row/)
  assert.match(wxml, /source-org-filter/)
  assert.match(wxml, /range="\{\{departmentOptions\}\}"/)
  assert.match(wxml, /range="\{\{teamOptions\}\}"/)
  assert.match(wxml, /range-key="label"/)
  assert.match(wxml, /bindchange="changeDepartmentFilter"/)
  assert.match(wxml, /bindchange="changeTeamFilter"/)
  assert.match(wxml, /currentDepartmentLabel/)
  assert.match(wxml, /currentTeamLabel/)
  assert.doesNotMatch(wxml, /quick-shot-filter-card/)
  assert.doesNotMatch(wxml, /quick-shot-filter-row/)
  assert.doesNotMatch(wxml, /quick-shot-filter-icon/)
  assert.doesNotMatch(wxml, /source-filter-panel/)
  assert.doesNotMatch(wxml.match(/<block wx:elif="\{\{records\.length > 0\}\}">[\s\S]*?<\/block>/)[0], /source-action-icon/)
  assert.doesNotMatch(wxml.match(/<block wx:elif="\{\{records\.length > 0\}\}">[\s\S]*?<\/block>/)[0], /status-badge/)
  assert.doesNotMatch(wxml, /source-field-name/)
  assert.match(wxml, /source-record-values/)
  assert.match(wxml, /source-rectify-btn/)

  const wxss = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-list/index.wxss"), "utf8")
  assert.match(wxss.match(/\.source-toolbar-inline\s*\{[\s\S]*?\}/)[0], /height:\s*92rpx/)
  assert.match(wxss.match(/\.source-toolbar-inline\s*\{[\s\S]*?\}/)[0], /display:\s*grid/)
  assert.match(wxss.match(/\.source-toolbar-inline\s*\{[\s\S]*?\}/)[0], /padding:\s*0 44rpx/)
  assert.match(wxss, /\.quick-shot-page \.source-toolbar-inline \.source-primary\s*\{[\s\S]*?color:\s*#ffffff/)
  assert.match(wxss.match(/\.source-date-filter\s*\{[\s\S]*?\}/)[0], /justify-self:\s*end/)
  assert.match(wxss.match(/\.source-date-filter\s*\{[\s\S]*?\}/)[0], /width:\s*max-content/)
  assert.match(wxss.match(/\.quick-shot-page > \.source-tabs\s*\{[\s\S]*?\}/)[0], /justify-content:\s*space-around/)
  assert.match(wxss.match(/\.quick-shot-page > \.source-tabs \.source-tab\s*\{[\s\S]*?\}/)[0], /background:\s*transparent/)
  assert.match(wxss.match(/\.quick-shot-page > \.source-tabs \.source-tab\s*\{[\s\S]*?\}/)[0], /box-shadow:\s*none/)
  assert.match(wxss.match(/\.quick-shot-page > \.source-tabs \.source-tab\.active\s*\{[\s\S]*?\}/)[0], /color:\s*#0b9860/)
  assert.match(wxss.match(/\.quick-shot-page > \.source-tabs \.source-tab\.active::after\s*\{[\s\S]*?\}/)[0], /background:\s*#0b9860/)
  assert.match(wxss.match(/\.source-org-filter-row\s*\{[\s\S]*?\}/)[0], /display:\s*grid/)
  assert.match(wxss.match(/\.source-org-filter-row\s*\{[\s\S]*?\}/)[0], /grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/)
  assert.match(wxss.match(/\.source-org-filter\s*\{[\s\S]*?\}/)[0], /height:\s*72rpx/)
  assert.match(wxss.match(/\.source-org-filter\s*\{[\s\S]*?\}/)[0], /border:\s*1rpx solid #d8dee8/)
})

test("safety-check source list has pending and completed tabs matching screenshot cards", async () => {
  installWx()
  const calls = []
  const totals = {
    OPENED: 23,
    ARCHIVED: 5
  }
  mockSourceService({
    listHazardRecords(moduleKey, query) {
      calls.push({ type: "list", moduleKey, query })
      if (query.pageSize === 1) {
        return Promise.resolve({ items: [], total: totals[query.status] || 0 })
      }
      return Promise.resolve({
        items: [
          {
            id: "41",
            status: query.status,
            statusLabel: query.status === "ARCHIVED" ? "已完成" : "待检查",
            payload: {
              inspectedUnitName: "Demo Works Company",
              inspectionUnitName: "Demo控股集团",
              checkType: "1111",
              checkTime: "2026-07-01 15:25",
              checkMethod: "四不两直对安全管理情况开展检查，召开现场安全会议",
              inspectors: ["系统管理员"],
              acceptancePassed: "--"
            },
            companyName: "Demo Works Company",
            businessDate: "2026-07-01"
          }
        ],
        total: totals[query.status] || 0
      })
    }
  })

  const page = capturePage("../pages/hazard/source-list/index")
  await page.onLoad({ moduleKey: "safety-check", title: "安全检查" })

  assert.equal(page.data.status, "OPENED")
  assert.deepEqual(page.data.tabs.map(item => item.label), ["待检查 (23)", "已完成 (5)"])
  assert.equal(page.data.total, 23)
  assert.deepEqual(calls[0], {
    type: "list",
    moduleKey: "safety-check",
    query: { page: 1, pageSize: 20, status: "OPENED" }
  })
  await page.changeStatus({ currentTarget: { dataset: { status: "ARCHIVED" } } })
  assert.equal(page.data.status, "ARCHIVED")
  assert.equal(page.data.total, 5)
  assert.deepEqual(calls.find(call => call.query.status === "ARCHIVED" && call.query.pageSize === 20), {
    type: "list",
    moduleKey: "safety-check",
    query: { page: 1, pageSize: 20, status: "ARCHIVED" }
  })

  page.openDetail({ currentTarget: { dataset: { id: "41" } } })
  assert.deepEqual(global.wx.navigateToCalls, [
    { url: "/pages/hazard/source-detail/index?id=41&moduleKey=safety-check&title=%E5%AE%89%E5%85%A8%E6%A3%80%E6%9F%A5&companyKey=gs" }
  ])
  assert.equal(page.data.records[0].safetyTitle, "Demo Works Company")
  assert.equal(page.data.records[0].safetyInspectionUnit, "Demo控股集团")
  assert.equal(page.data.records[0].safetyCheckType, "1111")
  assert.equal(page.data.records[0].safetyCheckTime, "2026-07-01 15:25")
  assert.equal(page.data.records[0].safetyCheckMethod, "四不两直对安全管理情况开展检查，召开现场安全会议")
  assert.equal(page.data.records[0].safetyInspectors, "系统管理员")
  assert.equal(page.data.records[0].safetyAcceptance, "--")
  assert.equal(page.data.records[0].safetyStatusLabel, "已完成")

  const wxml = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-list/index.wxml"), "utf8")
  assert.match(wxml, /source-safety-list/)
  assert.match(wxml, /name="add-circle"/)
  assert.match(wxml, /safety-date-filter/)
  assert.match(wxml, /name="calendar"/)
  assert.match(wxml, /name="chevron-down"/)
  assert.match(wxml, /bindchange="changeInlineStartDate"/)
  assert.match(wxml, /bindchange="changeInlineEndDate"/)
  assert.match(wxml, /source-org-filter-row/)
  assert.match(wxml, /bindchange="changeDepartmentFilter"/)
  assert.match(wxml, /bindchange="changeTeamFilter"/)
  assert.doesNotMatch(wxml, /safety-filter-trigger/)
  assert.match(wxml, /safe-card[\s\S]*<view class="source-safety-record safety-check-card"[^>]*data-id="\{\{item\.id\}\}"[^>]*bindtap="openDetail"/)
  assert.match(wxml, /safety-check-card-title[\s\S]*safetyTitle/)
  assert.match(wxml, /safety-check-card-status[\s\S]*safetyStatusLabel/)
  assert.match(wxml, /检查单位/)
  assert.match(wxml, /检查类型/)
  assert.match(wxml, /检查时间/)
  assert.match(wxml, /检查方式/)
  assert.match(wxml, /检查人员/)
  assert.match(wxml, /整改验收/)
  assert.doesNotMatch(wxml, /查看详情/)
  assert.match(wxml, /name="building"/)
  assert.match(wxml, /size="50rpx" color="#0b9860"/)
  assert.match(wxml, /name="time"/)
  assert.match(wxml, /name="assignment"/)
  assert.match(wxml, /name="user"/)
  assert.match(wxml, /name="verified"/)
  assert.doesNotMatch(wxml, /safety-check-bottom-divider/)
  assert.doesNotMatch(wxml, /safety-check-card-link/)
  assert.match(wxml, /source-toolbar-inline/)
  assert.match(wxml, /source-toolbar-safety/)
  assert.match(wxml, /wx:for="\{\{tabs\}\}"/)
  assert.deepEqual(page.data.tabs.map(item => item.title), ["待检查", "已完成"])
  assert.doesNotMatch(wxml.match(/<view class="source-safety-list">[\s\S]*?<\/view>\s*<\/block>/)[0], /openRectificationOrder/)

  const wxss = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-list/index.wxss"), "utf8")
  assert.match(wxss, /\.source-page\.safety-check-page/)
  assert.match(wxss, /\.source-toolbar-safety\s*\{[\s\S]*?height:\s*92rpx/)
  assert.match(wxss, /\.source-toolbar-safety\s*\{[\s\S]*?display:\s*grid/)
  assert.match(wxss, /\.source-toolbar-safety\s*\{[\s\S]*?padding:\s*0 44rpx/)
  assert.match(wxss, /\.source-toolbar-safety\s*\{[\s\S]*?border-bottom:\s*1rpx solid #edf1f5/)
  assert.match(wxss, /\.safety-date-filter\s*\{[\s\S]*?height:\s*60rpx/)
  assert.match(wxss, /\.safety-date-filter\s*\{[\s\S]*?justify-self:\s*end/)
  assert.match(wxss, /\.safety-date-filter\s*\{[\s\S]*?width:\s*max-content/)
  assert.match(wxss, /\.safety-date-filter\s*\{[\s\S]*?max-width:\s*100%/)
  assert.match(wxss, /\.safety-date-filter\s*\{[\s\S]*?padding:\s*0 8rpx 0 20rpx/)
  assert.match(wxss, /\.safety-date-filter\s*\{[\s\S]*?border:\s*1rpx solid #d8dee8/)
  assert.match(wxss, /\.safety-date-icon\s*\{[\s\S]*?width:\s*34rpx/)
  assert.match(wxss, /\.safety-date-arrow\s*\{[\s\S]*?width:\s*30rpx/)
  assert.match(wxss, /\.safety-date-arrow\s*\{[\s\S]*?justify-content:\s*center/)
  const safetyCardStyle = wxss.match(/\.safety-check-card\s*\{[\s\S]*?\}/)[0]
  assert.match(safetyCardStyle, /margin:\s*0/)
  assert.match(safetyCardStyle, /padding:\s*0/)
  assert.match(safetyCardStyle, /background:\s*transparent/)
  assert.match(safetyCardStyle, /box-shadow:\s*none/)
  assert.doesNotMatch(safetyCardStyle, /min-height/)
  assert.match(wxss, /\.safety-check-card-main\s*\{[\s\S]*?grid-template-columns:\s*96rpx minmax\(0,\s*1fr\)/)
  assert.match(wxss, /\.safety-check-unit-icon\s*\{[\s\S]*?width:\s*84rpx/)
  assert.match(wxss, /\.safety-check-unit-icon\s*\{[\s\S]*?height:\s*84rpx/)
  assert.match(wxss, /\.safety-check-row\s*\{[\s\S]*?grid-template-columns:\s*36rpx 150rpx minmax\(0,\s*1fr\)/)
  assert.match(wxss, /\.safety-check-fields\s*\{[\s\S]*?gap:\s*16rpx/)
  assert.match(wxss, /\.safety-check-method-value\s*\{[\s\S]*?white-space:\s*nowrap/)
  assert.match(wxss, /\.safety-check-method-value\s*\{[\s\S]*?overflow:\s*hidden/)
  assert.match(wxss, /\.safety-check-method-value\s*\{[\s\S]*?text-overflow:\s*ellipsis/)
  assert.match(wxss.match(/\.safety-check-page > \.source-tabs\s*\{[\s\S]*?\}/)[0], /height:\s*88rpx/)
  assert.match(wxss.match(/\.safety-check-page > \.source-tabs\s*\{[\s\S]*?\}/)[0], /overflow-x:\s*auto/)
  assert.match(wxss.match(/\.safety-check-page > \.source-tabs\s*\{[\s\S]*?\}/)[0], /justify-content:\s*space-around/)
  assert.match(wxss.match(/\.source-safety-list\s*\{[\s\S]*?\}/)[0], /padding:\s*28rpx 32rpx 36rpx/)
  assert.match(wxss.match(/\.source-org-filter-row\s*\{[\s\S]*?\}/)[0], /padding:\s*18rpx 20rpx 20rpx/)
  const safetyTabStyle = wxss.match(/\.safety-check-page > \.source-tabs \.source-tab\s*\{[\s\S]*?\}/)[0]
  assert.match(safetyTabStyle, /background:\s*transparent/)
  assert.match(safetyTabStyle, /border:\s*0/)
  assert.match(safetyTabStyle, /box-shadow:\s*none/)
  assert.match(safetyTabStyle, /height:\s*72rpx/)
  assert.match(safetyTabStyle, /line-height:\s*72rpx/)
  assert.doesNotMatch(safetyTabStyle, /border-radius/)
  const safetyActiveTabStyle = wxss.match(/\.safety-check-page > \.source-tabs \.source-tab\.active\s*\{[\s\S]*?\}/)[0]
  assert.match(safetyActiveTabStyle, /color:\s*#0b9860/)
  assert.match(safetyActiveTabStyle, /font-weight:\s*900/)
  assert.doesNotMatch(safetyActiveTabStyle, /background:/)
  const safetyActiveLineStyle = wxss.match(/\.safety-check-page > \.source-tabs \.source-tab\.active::after\s*\{[\s\S]*?\}/)[0]
  assert.match(safetyActiveLineStyle, /height:\s*4rpx/)
  assert.match(safetyActiveLineStyle, /background:\s*#0b9860/)
  assert.doesNotMatch(safetyActiveLineStyle, /display:\s*none/)
  assert.doesNotMatch(wxss, /\.safety-filter-trigger\s*\{/)
})

test("safety-check source list applies inline date range immediately", async () => {
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
    mockSourceService({
      listHazardRecords(moduleKey, query) {
        calls.push({ type: "list", moduleKey, query })
        return Promise.resolve({ items: [], total: 0 })
      }
    })

    const page = capturePage("../pages/hazard/source-list/index")
    await page.onLoad({ moduleKey: "safety-check", title: "安全检查" })

    await page.changeSafetyStartDate({ detail: { value: "2026-07-10" } })
    assert.equal(page.data.filterApplied, true)
    assert.equal(page.data.startDate, "2026-07-10")
    assert.equal(page.data.endDate, "2026-07-10")
    assert.deepEqual(calls.find(call => call.query.dateStart === "2026-07-10" && call.query.pageSize === 20), {
      type: "list",
      moduleKey: "safety-check",
      query: {
        page: 1,
        pageSize: 20,
        status: "OPENED",
        dateStart: "2026-07-10",
        dateEnd: "2026-07-10"
      }
    })

    await page.changeSafetyEndDate({ detail: { value: "2026-07-31" } })
    assert.equal(page.data.endDate, "2026-07-31")
    assert.deepEqual(calls.find(call => call.query.dateEnd === "2026-07-31" && call.query.pageSize === 20), {
      type: "list",
      moduleKey: "safety-check",
      query: {
        page: 1,
        pageSize: 20,
        status: "OPENED",
        dateStart: "2026-07-10",
        dateEnd: "2026-07-31"
      }
    })
  } finally {
    global.Date = RealDate
  }
})

test("quick-shot source list applies inline date range immediately", async () => {
  installWx()
  const calls = []
  mockSourceService({
    listHazardRecords(moduleKey, query) {
      calls.push({ type: "list", moduleKey, query })
      return Promise.resolve({ items: [], total: 0 })
    }
  })

  const page = capturePage("../pages/hazard/source-list/index")
  await page.onLoad({ moduleKey: "quick-shot", title: "随手拍" })

  await page.changeInlineStartDate({ detail: { value: "2026-06-24" } })

  assert.equal(page.data.filterApplied, true)
  assert.equal(page.data.startDate, "2026-06-24")
  assert.deepEqual(calls.find(call => call.query.dateStart === "2026-06-24" && call.query.pageSize === 20), {
    type: "list",
    moduleKey: "quick-shot",
    query: {
      page: 1,
      pageSize: 20,
      status: "REVIEWED",
      dateStart: "2026-06-24",
      dateEnd: page.data.endDate
    }
  })

  await page.changeInlineEndDate({ detail: { value: "2026-07-01" } })
  assert.equal(page.data.endDate, "2026-07-01")
  assert.deepEqual(calls.find(call => call.query.dateEnd === "2026-07-01" && call.query.pageSize === 20), {
    type: "list",
    moduleKey: "quick-shot",
    query: {
      page: 1,
      pageSize: 20,
      status: "REVIEWED",
      dateStart: "2026-06-24",
      dateEnd: "2026-07-01"
    }
  })
})

test("safety-check source list filters by department and team immediately", async () => {
  installWx()
  mockOrganizationServiceWithTwoTeams()
  const calls = []
  mockSourceService({
    listHazardRecords(moduleKey, query) {
      calls.push({ type: "list", moduleKey, query })
      return Promise.resolve({ items: [], total: 0 })
    }
  })

  const page = capturePage("../pages/hazard/source-list/index")
  await page.onLoad({ moduleKey: "safety-check", title: "安全检查" })

  assert.deepEqual(page.data.departmentOptions.map(item => item.label), ["全部部门", "安全环保部", "生产部"])
  assert.deepEqual(page.data.teamOptions.map(item => item.label), ["全部班组", "一班", "二班", "三班"])
  assert.equal(page.data.currentDepartmentLabel, "全部部门")
  assert.equal(page.data.currentTeamLabel, "全部班组")

  await page.changeDepartmentFilter({ detail: { value: 1 } })
  assert.equal(page.data.selectedDepartmentId, 101109)
  assert.equal(page.data.selectedTeamId, "")
  assert.deepEqual(page.data.teamOptions.map(item => item.label), ["全部班组", "一班", "二班"])
  assert.deepEqual(calls.find(call => call.query.departmentId === 101109 && call.query.pageSize === 20), {
    type: "list",
    moduleKey: "safety-check",
    query: {
      page: 1,
      pageSize: 20,
      status: "OPENED",
      departmentId: 101109
    }
  })
  assert.ok(calls.some(call => call.query.pageSize === 1 && call.query.departmentId === 101109))

  await page.changeTeamFilter({ detail: { value: 2 } })
  assert.equal(page.data.selectedTeamId, 1011002)
  assert.deepEqual(calls.find(call => call.query.teamId === 1011002 && call.query.pageSize === 20), {
    type: "list",
    moduleKey: "safety-check",
    query: {
      page: 1,
      pageSize: 20,
      status: "OPENED",
      departmentId: 101109,
      teamId: 1011002
    }
  })
})

test("quick-shot source list filters by department and team immediately", async () => {
  installWx()
  mockOrganizationServiceWithTwoTeams()
  const calls = []
  mockSourceService({
    listHazardRecords(moduleKey, query) {
      calls.push({ type: "list", moduleKey, query })
      return Promise.resolve({ items: [], total: 0 })
    }
  })

  const page = capturePage("../pages/hazard/source-list/index")
  await page.onLoad({ moduleKey: "quick-shot", title: "随手拍" })

  await page.changeDepartmentFilter({ detail: { value: 2 } })
  assert.equal(page.data.selectedDepartmentId, 101110)
  assert.deepEqual(page.data.teamOptions.map(item => item.label), ["全部班组", "三班"])
  assert.deepEqual(calls.find(call => call.query.departmentId === 101110 && call.query.pageSize === 20), {
    type: "list",
    moduleKey: "quick-shot",
    query: {
      page: 1,
      pageSize: 20,
      status: "REVIEWED",
      departmentId: 101110
    }
  })

  await page.changeTeamFilter({ detail: { value: 1 } })
  assert.equal(page.data.selectedTeamId, 1011003)
  assert.deepEqual(calls.find(call => call.query.teamId === 1011003 && call.query.pageSize === 20), {
    type: "list",
    moduleKey: "quick-shot",
    query: {
      page: 1,
      pageSize: 20,
      status: "REVIEWED",
      departmentId: 101110,
      teamId: 1011003
    }
  })
})

test("hazard source pages use a unified green topbar", () => {
  for (const page of ["source-list", "source-form", "source-detail"]) {
    const json = JSON.parse(require("node:fs").readFileSync(require.resolve(`../pages/hazard/${page}/index.json`), "utf8"))
    const wxml = require("node:fs").readFileSync(require.resolve(`../pages/hazard/${page}/index.wxml`), "utf8")
    const wxss = require("node:fs").readFileSync(require.resolve(`../pages/hazard/${page}/index.wxss`), "utf8")
    assert.equal(json.usingComponents["green-header"], "/components/ui/green-header/index")
    assert.match(wxml, /<green-header[\s\S]*?bind:back="goBack"/)
    assert.doesNotMatch(wxml, /source-topbar|source-homebar/)
    assert.match(wxss, /^page\s*\{[\s\S]*?margin:\s*0;[\s\S]*?padding:\s*0;/m)
  }
})

test("quick-shot source form locks company and department for department scoped roles and keeps team selectable", async () => {
  installWx()
  mockOrganizationServiceWithTwoTeams()
  const calls = []
  mockSourceService({
    createHazardRecord(moduleKey, payload) {
      calls.push({ type: "create", moduleKey, payload })
      return Promise.resolve({ id: "23" })
    },
    uploadHazardAttachment(moduleKey, id, fileKind, filePath) {
      calls.push({ type: "upload", moduleKey, id, fileKind, filePath })
      return Promise.resolve({ url: "/uploads/hazard.jpg" })
    },
    submitHazardRecord(moduleKey, id) {
      calls.push({ type: "submit", moduleKey, id })
      return Promise.reject(new Error("quick-shot should not call generic submit"))
    }
  })
  const userPath = require.resolve("../services/user")
  require.cache[userPath] = {
    id: userPath,
    filename: userPath,
    loaded: true,
    exports: {
      getCurrentUser() {
        return Promise.resolve({
          userId: "7",
          realName: "陈志斌",
          orgId: 101109,
          roles: ["WORKSHOP_DIRECTOR"]
        })
      }
    }
  }

  const page = capturePage("../pages/hazard/source-form/index")
  await page.onLoad({ moduleKey: "quick-shot" })

  assert.equal(page.data.selectedCompanyId, 4)
  assert.equal(page.data.selectedDepartmentId, 101109)
  assert.equal(page.data.selectedTeamId, "")
  assert.equal(page.data.organizationLocks.company, true)
  assert.equal(page.data.organizationLocks.department, true)
  assert.equal(page.data.organizationLocks.team, false)
  assert.deepEqual(page.data.teamOptions.map(item => item.label), ["一班", "二班"])

  page.changeTeam({ detail: { value: 1 } })
  page.updateField({ currentTarget: { dataset: { field: "hazardDescription" } }, detail: { value: "安全通道堆物" } })
  page.updateField({ currentTarget: { dataset: { field: "rectificationMeasures" } }, detail: { value: "立即清理" } })
  page.choosePhoto()
  await page.submitForm()

  assert.equal(calls[0].payload.companyId, 4)
  assert.equal(calls[0].payload.departmentId, 101109)
  assert.equal(calls[0].payload.teamId, 1011002)
  assert.equal(calls[0].payload.ownerUserId, 7)
  assert.equal(calls[0].payload.payload.rectificationMeasures, "立即清理")
  assert.equal(calls.some(call => call.type === "submit"), false)
})

test("quick-shot source form creates and uploads selected photo without generic submit", async () => {
  installWx()
  mockOrganizationServiceWithTwoCompanies()
  const calls = []
  mockSourceService({
    createHazardRecord(moduleKey, payload) {
      calls.push({ type: "create", moduleKey, payload })
      return Promise.resolve({ id: "21" })
    },
    uploadHazardAttachment(moduleKey, id, fileKind, filePath) {
      calls.push({ type: "upload", moduleKey, id, fileKind, filePath })
      return Promise.resolve({ url: "/uploads/hazard.jpg" })
    },
    submitHazardRecord(moduleKey, id) {
      calls.push({ type: "submit", moduleKey, id })
      return Promise.reject(new Error("quick-shot should not call generic submit"))
    }
  })
  const userPath = require.resolve("../services/user")
  require.cache[userPath] = {
    id: userPath,
    filename: userPath,
    loaded: true,
    exports: {
      getCurrentUser() {
        return Promise.resolve({ id: 7, companyId: 4, departmentId: 101109, teamId: 1011001 })
      }
    }
  }

  const page = capturePage("../pages/hazard/source-form/index")
  await page.onLoad({ moduleKey: "quick-shot" })
  assert.deepEqual(page.data.companyOptions.map(item => item.label), [
    "Demo Materials Company",
    "海安加工厂"
  ])
  page.changeCompany({ detail: { value: 1 } })
  assert.equal(page.data.currentCompany.company, "海安加工厂")
  assert.deepEqual(page.data.departmentOptions.map(item => item.label), ["生产部", "质检部"])
  assert.deepEqual(page.data.teamOptions.map(item => item.label), ["甲班", "乙班"])
  assert.equal(page.data.selectedTeamId, "")
  page.changeDepartment({ detail: { value: 1 } })
  assert.equal(page.data.currentCompany.workshop, "质检部")
  assert.deepEqual(page.data.teamOptions.map(item => item.label), ["质检一班"])
  page.changeTeam({ detail: { value: 0 } })
  page.changeUploadDate({ detail: { value: "2026-07-02" } })
  page.updateField({ currentTarget: { dataset: { field: "hazardDescription" } }, detail: { value: "安全通道堆物" } })
  page.updateField({ currentTarget: { dataset: { field: "location" } }, detail: { value: "一车间" } })
  page.choosePhoto()
  assert.equal(page.data.photo, "/tmp/hazard.jpg")
  assert.deepEqual(global.wx.chooseMediaCalls[0].sourceType, ["album", "camera"])
  page.takePhoto()
  assert.deepEqual(global.wx.chooseMediaCalls[1].sourceType, ["camera"])
  page.uploadPhoto()
  assert.deepEqual(global.wx.chooseMediaCalls[2].sourceType, ["album"])
  page.previewPhoto()
  assert.deepEqual(global.wx.previewImageCalls, [
    { current: "/tmp/hazard.jpg", urls: ["/tmp/hazard.jpg"] }
  ])
  await page.submitForm()

  assert.equal(calls[0].type, "create")
  assert.equal(calls[0].moduleKey, "quick-shot")
  assert.equal(calls[0].payload.companyId, 5)
  assert.equal(calls[0].payload.departmentId, 201110)
  assert.equal(calls[0].payload.teamId, 2011003)
  assert.equal(calls[0].payload.ownerUserId, 7)
  assert.equal(calls[0].payload.businessDate, "2026-07-02")
  assert.equal(calls[0].payload.payload.hazardDescription, "安全通道堆物")
  assert.equal(calls[0].payload.payload.location, "一车间")
  assert.deepEqual(calls.slice(1), [
    { type: "upload", moduleKey: "quick-shot", id: "21", fileKind: "IMAGE", filePath: "/tmp/hazard.jpg" }
  ])
  assert.equal(calls.some(call => call.type === "submit"), false)

  const wxml = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-form/index.wxml"), "utf8")
  assert.match(wxml, /range="\{\{companyOptions\}\}"[\s\S]*bindchange="changeCompany"/)
  assert.match(wxml, /range="\{\{departmentOptions\}\}"[\s\S]*bindchange="changeDepartment"/)
  assert.match(wxml, /picker mode="date"[^>]*bindchange="changeUploadDate"/)
  assert.match(wxml, /upload-panel[\s\S]*bind:upload="uploadPhoto"[\s\S]*bind:preview="previewPhoto"/)
  assert.match(wxml, /bindtap="takePhoto"[\s\S]*手机拍照/)
  assert.match(wxml, /bindtap="uploadPhoto"[\s\S]*上传图片/)
})

test("safety-check source form creates with check item fields and optional photo", async () => {
  installWx()
  mockOrganizationService()
  const calls = []
  mockSourceService({
    createHazardRecord(moduleKey, payload) {
      calls.push({ type: "create", moduleKey, payload })
      return Promise.resolve({ id: "22" })
    },
    uploadHazardAttachment(moduleKey, id, fileKind, filePath) {
      calls.push({ type: "upload", moduleKey, id, fileKind, filePath })
      return Promise.resolve({ url: "/uploads/check.jpg" })
    },
    submitHazardRecord(moduleKey, id) {
      calls.push({ type: "submit", moduleKey, id })
      return Promise.resolve({ id, status: "OPENED" })
    }
  })
  const userPath = require.resolve("../services/user")
  require.cache[userPath] = {
    id: userPath,
    filename: userPath,
    loaded: true,
    exports: {
      getCurrentUser() {
        return Promise.resolve({})
      }
    }
  }

  const page = capturePage("../pages/hazard/source-form/index")
  await page.onLoad({ moduleKey: "safety-check" })
  page.updateField({ currentTarget: { dataset: { field: "checkTheme" } }, detail: { value: "移动端检查" } })
  page.updateField({ currentTarget: { dataset: { field: "checkArea" } }, detail: { value: "组装线" } })
  page.updateField({ currentTarget: { dataset: { field: "checkItem" } }, detail: { value: "检查通道" } })
  page.updateField({ currentTarget: { dataset: { field: "hazardDescription" } }, detail: { value: "通道堆放材料" } })
  page.updateField({ currentTarget: { dataset: { field: "rectificationMeasures" } }, detail: { value: "清理并标识" } })
  page.choosePhoto()
  await page.submitForm()

  assert.equal(calls[0].moduleKey, "safety-check")
  assert.equal(calls[0].payload.payload.checkTheme, "移动端检查")
  assert.equal(calls[0].payload.payload.checkArea, "组装线")
  assert.deepEqual(calls[0].payload.payload.checkItems, [
    {
      checkItem: "检查通道",
      checkResult: "有隐患",
      hazardDescription: "通道堆放材料",
      rectificationMeasures: "清理并标识",
      rectificationResponsiblePerson: "",
      rectificationDeadline: ""
    }
  ])
  assert.equal(calls[1].type, "upload")
  assert.equal(calls[2].type, "submit")
})

test("safety-check create page matches screenshot fields and maps payload", async () => {
  installWx()
  global.wx.chosenMediaTempFiles = [
    { tempFilePath: "/tmp/check-before.jpg", fileType: "image" },
    { tempFilePath: "/tmp/check-video.mp4", fileType: "video" }
  ]
  mockSafetyCheckOrganizationService()
  const calls = []
  mockSourceService({
    createHazardRecord(moduleKey, payload) {
      calls.push({ type: "create", moduleKey, payload })
      return Promise.resolve({ id: "52" })
    },
    uploadHazardAttachment(moduleKey, id, fileKind, filePath) {
      calls.push({ type: "upload", moduleKey, id, fileKind, filePath })
      return Promise.resolve({ url: "/uploads/check.jpg" })
    },
    submitHazardRecord(moduleKey, id) {
      calls.push({ type: "submit", moduleKey, id })
      return Promise.resolve({ id, status: "CLOSED" })
    }
  })
  const userPath = require.resolve("../services/user")
  require.cache[userPath] = {
    id: userPath,
    filename: userPath,
    loaded: true,
    exports: {
      getCurrentUser() {
        return Promise.resolve({ userId: "7", realName: "陈志斌" })
      }
    }
  }

  const page = capturePage("../pages/hazard/source-form/index")
  await page.onLoad({ moduleKey: "safety-check", title: "安全检查" })
  assert.equal(page.data.safetyFormTab, "base")
  page.changeSafetyFormTab({ currentTarget: { dataset: { tab: "hazards" } } })
  assert.equal(page.data.safetyFormTab, "hazards")
  assert.deepEqual(page.data.safetyHazardCards.map(item => item.index), [1])
  page.addSafetyHazardCard()
  assert.deepEqual(page.data.safetyHazardCards.map(item => item.index), [1, 2])
  page.deleteSafetyHazardCard({ currentTarget: { dataset: { id: "card-2" } } })
  assert.deepEqual(page.data.safetyHazardCards.map(item => item.index), [1])
  page.toggleSafetyHazardCard({ currentTarget: { dataset: { id: "card-1" } } })
  assert.equal(page.data.safetyHazardCards[0].collapsed, true)
  page.toggleSafetyHazardCard({ currentTarget: { dataset: { id: "card-1" } } })
  assert.equal(page.data.safetyHazardCards[0].collapsed, false)
  page.changeSafetyFormTab({ currentTarget: { dataset: { tab: "base" } } })
  assert.equal(page.data.safetyFormTab, "base")

  assert.deepEqual(page.data.inspectionUnitOptions.map(item => item.label), [
    "广东省Demo控股集团有限公司",
    "Demo Materials Company",
    "Demo Materials Company"
  ])
  assert.deepEqual(page.data.inspectedUnitOptions.map(item => item.label), [
    "Demo Materials Company",
    "Demo Materials Company"
  ])
  assert.match(page.data.form.checkDate, /^\d{4}-\d{2}-\d{2}$/)
  assert.match(page.data.form.checkTime, /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)

  page.changeCheckDate({ detail: { value: "2026-07-02" } })
  assert.equal(page.data.form.checkDate, "2026-07-02")
  assert.match(page.data.form.checkTime, /^2026-07-02 \d{2}:\d{2}:\d{2}$/)

  page.changeCheckTime({ detail: { value: "09:30" } })
  assert.equal(page.data.checkClock, "09:30")
  assert.equal(page.data.form.checkTime, "2026-07-02 09:30:00")

  page.changeInspectionUnit({ detail: { value: 1 } })
  page.changeInspectedUnit({ detail: { value: 1 } })
  page.updateField({ currentTarget: { dataset: { field: "inspectedProject" } }, detail: { value: "受检项目A" } })
  page.updateField({ currentTarget: { dataset: { field: "checkTheme" } }, detail: { value: "汛期安全检查" } })
  page.updateField({ currentTarget: { dataset: { field: "manualCheckType" } }, detail: { value: "手录检查类型" } })
  page.updateField({ currentTarget: { dataset: { field: "manualInspectors" } }, detail: { value: "手录检查人员" } })
  page.updateField({ currentTarget: { dataset: { field: "manualInspectedUnitUsers" } }, detail: { value: "手录受检人员" } })
  page.updateField({ currentTarget: { dataset: { field: "checkContent" } }, detail: { value: "检查内容" } })
  page.updateField({ currentTarget: { dataset: { field: "remark" } }, detail: { value: "备注" } })
  page.choosePhoto()
  assert.deepEqual(global.wx.chooseMediaCalls[0].mediaType, ["image", "video"])
  assert.deepEqual(page.data.attachments, [
    { type: "image", path: "/tmp/check-before.jpg", name: "check-before.jpg" },
    { type: "video", path: "/tmp/check-video.mp4", name: "check-video.mp4" }
  ])
  page.previewAttachment({ currentTarget: { dataset: { index: 0 } } })
  assert.deepEqual(global.wx.previewImageCalls, [
    { current: "/tmp/check-before.jpg", urls: ["/tmp/check-before.jpg"] }
  ])
  await page.submitForm()

  assert.equal(calls[0].moduleKey, "safety-check")
  assert.equal(calls[0].payload.companyId, 5)
  assert.equal(Object.hasOwn(calls[0].payload, "teamId"), false)
  assert.equal(calls[0].payload.payload.inspectionUnitId, 4)
  assert.equal(calls[0].payload.payload.inspectedUnitId, 5)
  assert.equal(calls[0].payload.payload.inspectedProject, "受检项目A")
  assert.equal(calls[0].payload.payload.manualCheckType, "手录检查类型")
  assert.equal(calls[0].payload.payload.checkDate, "2026-07-02")
  assert.equal(calls[0].payload.payload.checkTime, "2026-07-02 09:30:00")
  assert.deepEqual(calls[0].payload.payload.checkMethods, [
    "四不两直对安全管理情况开展检查",
    "召开现场安全会议",
    "日常检查"
  ])
  assert.equal(calls[0].payload.payload.checkContent, "检查内容")
  assert.deepEqual(calls.slice(1, 3), [
    { type: "upload", moduleKey: "safety-check", id: "52", fileKind: "IMAGE", filePath: "/tmp/check-before.jpg" },
    { type: "upload", moduleKey: "safety-check", id: "52", fileKind: "VIDEO", filePath: "/tmp/check-video.mp4" }
  ])
  assert.equal(calls[3].type, "submit")
  assert.deepEqual(global.wx.navigateToCalls, [])

  const wxml = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-form/index.wxml"), "utf8")
  const safetyBlock = wxml.match(/<block wx:if="\{\{moduleKey === 'safety-check'\}\}">[\s\S]*?<\/block>/)[0]
  assert.match(safetyBlock, /safe-card[\s\S]*safety-form-card/)
  assert.match(safetyBlock, /safety-form-row[\s\S]*检查单位/)
  assert.match(safetyBlock, /safety-form-required[\s\S]*检查单位/)
  assert.match(safetyBlock, /safety-upload-panel/)
  assert.match(safetyBlock, /0\/500/)
  assert.match(wxml, /picker mode="date"[^>]*bindchange="changeCheckDate"/)
  assert.match(wxml, /picker mode="time"[^>]*bindchange="changeCheckTime"/)
  assert.match(wxml, /safety-attachment-preview/)
  assert.match(wxml, /<image[\s\S]*mode="aspectFill"[\s\S]*bindtap="previewAttachment"/)
  assert.match(wxml, /<video[\s\S]*class="safety-video-preview"/)
  ;[
    "检查单位",
    "受检单位",
    "受检项目",
    "检查类型",
    "检查类型（手录）",
    "检查日期",
    "检查时间",
    "创建人",
    "检查方式",
    "检查人员",
    "验收人员",
    "检查人员（手录）",
    "受检单位人员",
    "受检单位人员（手录）",
    "检查内容",
    "备注",
    "上传附件",
    "提交"
  ].forEach(text => assert.match(wxml, new RegExp(text)))
  assert.doesNotMatch(wxml, /下一步/)
})

test("safety-check hazard tab matches screenshot style for detail items", () => {
  const wxml = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-form/index.wxml"), "utf8")
  const wxss = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-form/index.wxss"), "utf8")

  assert.match(wxml, /data-tab="base"[\s\S]*基础信息<\/view>[\s\S]*data-tab="hazards"[\s\S]*隐患明细<\/view>/)
  assert.match(wxml, /safety-hazard-tab/)
  assert.match(wxml, /<view class="safety-hazard-add" bindtap="addSafetyHazardCard"/)
  assert.doesNotMatch(wxml, /<button class="safety-hazard-add"/)
  assert.match(wxml, /wx:for="\{\{safetyHazardCards\}\}"/)
  assert.match(wxml, /class="safety-hazard-index">\{\{item.index\}\}/)
  assert.match(wxml, /safety-hazard-card/)
  assert.match(wxml, /safety-hazard-head/)
  assert.match(wxml, /safety-hazard-add/)
  assert.match(wxml, /safety-hazard-index/)
  assert.match(wxml, /safety-hazard-delete" data-id="\{\{item.id\}\}" bindtap="deleteSafetyHazardCard"/)
  assert.match(wxml, /bindtap="toggleSafetyHazardCard"/)
  assert.match(wxml, /safety-hazard-chevron/)
  assert.match(wxml, /safety-hazard-field/)
  assert.match(wxml, /是否启用AI/)
  assert.match(wxml, /整改前照片/)
  assert.match(wxml, /隐患描述/)
  assert.match(wxml, /整改措施/)
  assert.match(wxml, /整改责任人/)
  assert.match(wxml, /整改截止日期/)
  assert.match(wxss, /\.safety-hazard-tab\s*\{/)
  assert.match(wxss, /\.safety-hazard-card\s*\{/)
  assert.match(wxss, /\.safety-hazard-head\s*\{/)
  assert.match(wxss, /\.safety-hazard-card-collapsed\s*\{/)
  assert.match(wxss.match(/\.safety-hazard-add\s*\{[\s\S]*?\}/)[0], /width:\s*118rpx/)
  assert.match(wxss.match(/\.safety-hazard-add\s*\{[\s\S]*?\}/)[0], /height:\s*54rpx/)
  assert.match(wxss.match(/\.safety-hazard-add\s*\{[\s\S]*?\}/)[0], /margin:\s*0 0 10rpx 0/)
  assert.match(wxss.match(/\.safety-hazard-add text\s*\{[\s\S]*?\}/)[0], /width:\s*24rpx/)
  assert.match(wxss.match(/\.safety-hazard-add text\s*\{[\s\S]*?\}/)[0], /height:\s*24rpx/)
  assert.match(wxss, /\.safety-hazard-field\s*\{/)
  assert.match(wxss.match(/\.safety-hazard-ai-option\.active\s*\{[\s\S]*?\}/)[0], /background:\s*#e7f6ef/)
  assert.match(wxss.match(/\.safety-hazard-ai-option\.active\s*\{[\s\S]*?\}/)[0], /border-color:\s*#13a66b/)
  assert.match(wxss, /\.safety-hazard-required::before\s*\{[\s\S]*?content:\s*"\*"/)
  assert.doesNotMatch(wxss, /\.safety-hazard-required::after\s*\{[\s\S]*?content:\s*"\*"/)
})

test("source detail approves quick-shot and opens rectification order detail", async () => {
  installWx()
  const calls = []
  mockSourceService({
    getHazardRecord(moduleKey, id) {
      calls.push({ type: "get", moduleKey, id })
      return Promise.resolve({ id, version: 3, status: "OPENED", payload: { hazardDescription: "通道堆物" } })
    },
    workflowHazardRecord(moduleKey, id, payload) {
      calls.push({ type: "workflow", moduleKey, id, payload })
      return Promise.resolve({ id, version: 4, status: "REVIEWED", payload: { hazardDescription: "通道堆物" } })
    },
    openSourceRectificationOrder(moduleKey, id) {
      calls.push({ type: "order", moduleKey, id })
      return Promise.resolve({ id: "91" })
    }
  })

  const page = capturePage("../pages/hazard/source-detail/index")
  await page.onLoad({ id: "31", moduleKey: "quick-shot" })
  await page.approveQuickShot()
  await page.openRectificationOrder()

  assert.deepEqual(calls, [
    { type: "get", moduleKey: "quick-shot", id: "31" },
    { type: "workflow", moduleKey: "quick-shot", id: "31", payload: { action: "APPROVE", version: 3, payload: {} } },
    { type: "order", moduleKey: "quick-shot", id: "31" }
  ])
  assert.equal(global.wx.navigateToCalls[0].url, "/pages/hazard-rectification/order-detail/index?id=91")
})

test("quick-shot detail keeps the read-only screenshot style", () => {
  const wxml = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-detail/index.wxml"), "utf8")
  const wxss = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-detail/index.wxss"), "utf8")

  assert.match(wxml, /quick-shot-summary-card/)
  const summaryBlock = wxml.match(/<view class="quick-shot-summary-card">[\s\S]*?<\/view>\s*<\/safe-card>/)[0]
  assert.match(summaryBlock, /quick-shot-summary-badge/)
  assert.match(summaryBlock, /quick-shot-summary-meta-icon/)
  assert.doesNotMatch(summaryBlock, /status-badge/)
  assert.match(wxss, /\.quick-shot-summary-card\s*\{[\s\S]*?grid-template-columns:\s*96rpx minmax\(0,\s*1fr\)/)
  assert.match(wxss, /\.quick-shot-summary-badge\s*\{[\s\S]*?position:\s*absolute/)
  assert.match(wxss, /\.quick-shot-summary-badge\s*\{[\s\S]*?right:\s*0/)
  assert.match(wxss, /\.quick-shot-summary-icon\s*\{[\s\S]*?background:\s*#dcf7e6/)
  assert.match(wxml, /quick-shot-photo-card/)
  assert.match(wxml, /quick-shot-info-card/)
  assert.match(wxml, /quick-shot-hazard-card/)
  assert.match(wxml, /quick-shot-primary-action-wrap[\s\S]*quick-shot-primary-action/)
  assert.match(wxml, /<view class="quick-shot-primary-action[^"]*"[\s\S]*bindtap="openRectificationOrder"/)
  assert.doesNotMatch(wxml, /<button class="quick-shot-primary-action"/)
  assert.match(wxss, /\.quick-shot-primary-action-wrap\s*\{[\s\S]*?width:\s*100%/)
  assert.match(wxss, /\.quick-shot-primary-action-wrap\s*\{[\s\S]*?box-sizing:\s*border-box/)
  assert.match(wxss, /\.quick-shot-primary-action\s*\{[\s\S]*?padding:\s*0/)
  assert.match(wxss, /\.quick-shot-primary-action\s*\{[\s\S]*?background:\s*#13a66b/)
  assert.match(wxss, /\.quick-shot-primary-action\s*\{[\s\S]*?margin:\s*0/)
  assert.match(wxss, /\.quick-shot-primary-action::after\s*\{[\s\S]*?border:\s*0/)
  assert.match(wxml, /编号/)
  assert.match(wxml, /更新时间/)
  assert.doesNotMatch(wxml, /source-back-btn/)
  assert.doesNotMatch(wxml, /source-detail-toolbar/)
  assert.doesNotMatch(wxml, />返回</)
  assert.match(wxml, /打开整改单/)
})

test("quick-shot pages mirror the supplied mobile screenshots", () => {
  const formWxml = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-form/index.wxml"), "utf8")
  const listWxml = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-list/index.wxml"), "utf8")
  const detailWxml = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-detail/index.wxml"), "utf8")
  const formWxss = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-form/index.wxss"), "utf8")
  const safeCardWxss = require("node:fs").readFileSync(require.resolve("../components/ui/safe-card/index.wxss"), "utf8")
  const listWxss = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-list/index.wxss"), "utf8")
  const detailWxss = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-detail/index.wxss"), "utf8")

  const formJson = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-form/index.json"), "utf8")
  const listJson = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-list/index.json"), "utf8")
  const detailJson = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-detail/index.json"), "utf8")

  ;[formJson, listJson, detailJson].forEach(json => {
    assert.match(json, /safe-card/)
    assert.match(json, /state-view/)
    assert.match(json, /state-view/)
  })

  assert.match(formWxml, /quick-shot-form-card/)
  assert.match(formWxml, /upload-panel/)
  assert.match(formWxml, /info-row/)
  assert.match(formWxml, /wx:if="\{\{moduleKey === 'safety-check'\}\}"[\s\S]*create-tabs/)
  assert.match(formWxss, /\.quick-shot-form-card/)
  assert.match(formWxss, /\.quick-shot-textarea/)

  assert.match(listWxml, /state-view/)
  assert.match(listWxml, /source-date-filter/)
  assert.doesNotMatch(listWxml, /quick-shot-filter-trigger/)
  assert.doesNotMatch(listWxml, /quick-shot-filter-card/)
  assert.doesNotMatch(listWxml, /quick-shot-filter-icon/)
  assert.match(listWxml, /quick-shot-record-card/)
  assert.match(listWxml, /<view class="source-record source-record-card quick-shot-record-card"[^>]*data-id="\{\{item\.id\}\}"[^>]*bindtap="openDetail"/)
  assert.match(listWxml, /quick-shot-record-fields/)
  assert.match(listWxml, /quick-shot-record-actions/)
  assert.doesNotMatch(listWxml, /查看明细|查看详情/)
  assert.doesNotMatch(listWxml.match(/<block wx:elif="\{\{records\.length > 0\}\}">[\s\S]*?<\/block>/)[0], /status-badge/)
  assert.doesNotMatch(listWxml.match(/<block wx:elif="\{\{records\.length > 0\}\}">[\s\S]*?<\/block>/)[0], /source-action-icon/)
  assert.doesNotMatch(listWxml, /source-field-name/)
  assert.doesNotMatch(listWxml, /displayCompanyName/)
  assert.match(listWxml, /source-rectify-btn/)
  assert.match(listWxss, /\.quick-shot-record-card/)
  assert.match(listWxss, /\.quick-shot-record-actions/)
  assert.match(listWxss, /\.source-toolbar-inline/)
  assert.match(listWxss, /\.source-date-filter/)
  assert.match(listWxss, /\.quick-shot-page > \.source-tabs\s*\{[\s\S]*?justify-content:\s*space-around/)
  assert.doesNotMatch(listWxss.match(/\.quick-shot-tabs\s*\{[\s\S]*?\}/)[0], /repeat\(4/)

  ;["编号", "公司", "部门", "班组", "上报人", "上传日期", "状态", "创建时间", "是否启用AI", "隐患图片", "隐患描述", "整改措施", "更新时间"].forEach(text => {
    assert.match(detailWxml, new RegExp(text))
  })
  assert.match(detailWxml, /quick-shot-detail/)
  assert.doesNotMatch(detailWxml, /source-detail-toolbar/)
  assert.doesNotMatch(detailWxml, /source-back-btn/)
  assert.match(detailWxml, /quick-shot-photo/)
  assert.match(detailWxss, /\.quick-shot-summary-card/)
  assert.match(detailWxss, /\.quick-shot-primary-action/)
})

test("quick-shot screenshot replica removes debug UI and uses exact visible detail/form layout", () => {
  const formWxml = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-form/index.wxml"), "utf8")
  const listWxml = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-list/index.wxml"), "utf8")
  const detailWxml = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-detail/index.wxml"), "utf8")
  const listJs = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-list/index.js"), "utf8")
  const formWxss = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-form/index.wxss"), "utf8")
  const safeCardWxss = require("node:fs").readFileSync(require.resolve("../components/ui/safe-card/index.wxss"), "utf8")
  const listWxss = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-list/index.wxss"), "utf8")
  const detailWxss = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-detail/index.wxss"), "utf8")

  assert.doesNotMatch(listWxml, /source-debug-mark|v-shot-0701/)
  assert.doesNotMatch(listJs, /v-shot-0701/)

  assert.match(formWxml, /quick-shot-form-actions/)
  assert.match(formWxml, /quick-shot-ai-option/)
  assert.match(formWxml, /quick-shot-textarea-count/)
  const quickShotFormBlock = formWxml.match(/<block wx:else>[\s\S]*?<\/block>/)[0]
  assert.doesNotMatch(quickShotFormBlock, /\srequired(?:=|\s|")/)
  assert.match(formWxss, /--quick-shot-form-font-size:\s*29rpx/)
  assert.equal((formWxml.match(/placeholder-class="quick-shot-textarea-placeholder"/g) || []).length, 2)
  assert.match(formWxss, /\.quick-shot-form\s*\{[\s\S]*?padding:\s*24rpx 20rpx/)
  assert.match(formWxss, /\.quick-shot-form safe-card\s*\{[\s\S]*?--safe-card-min-height:\s*calc\(100vh - 294rpx\)/)
  assert.match(safeCardWxss, /\.safe-card\s*\{[\s\S]*?min-height:\s*var\(--safe-card-min-height,\s*0\)/)
  assert.match(formWxss, /\.quick-shot-textarea-placeholder\s*\{[\s\S]*?font-size:\s*25rpx/)
  assert.match(formWxss, /\.quick-shot-textarea-placeholder\s*\{[\s\S]*?line-height:\s*36rpx/)
  assert.match(formWxss, /\.quick-shot-form-row\s*\{[\s\S]*?font-size:\s*var\(--quick-shot-form-font-size\)/)
  assert.match(formWxss, /\.quick-shot-textarea-count\s*\{[\s\S]*?font-size:\s*var\(--quick-shot-form-font-size\)/)
  assert.match(formWxss, /\.quick-shot-page \.source-form-actions\s*\{[\s\S]*?grid-template-columns:\s*1fr 1fr/)
  assert.match(formWxss, /\.quick-shot-page \.source-form-actions::after\s*\{[\s\S]*?left:\s*50%/)
  assert.match(formWxss, /\.quick-shot-ai-option\.active/)
  assert.match(formWxss, /\.quick-shot-textarea-count/)

  assert.match(listWxml, /quick-shot-record-fields/)
  assert.match(listWxml, /quick-shot-record-actions/)
  assert.match(listWxml, /source-rectify-btn/)
  assert.doesNotMatch(listWxml, /source-detail-btn/)
  assert.doesNotMatch(listWxml, /查看明细|查看详情/)
  assert.doesNotMatch(listWxml.match(/<button class="source-secondary source-rectify-btn"[\s\S]*?<\/button>/)[0], /<text/)
  assert.doesNotMatch(listWxml, /source-record-left|source-record-right/)
  assert.match(listWxss, /\.quick-shot-record-card\s*\{[\s\S]*?min-height:\s*218rpx/)
  assert.match(listWxss, /\.quick-shot-record-fields\s*\{[\s\S]*?gap:\s*18rpx/)
  assert.match(listWxss, /\.quick-shot-record-actions\s*\{[\s\S]*?position:\s*absolute/)
  assert.match(listWxss, /\.quick-shot-record-actions\s*\{[\s\S]*?right:\s*24rpx/)
  assert.match(listWxss, /\.source-secondary\s*\{[\s\S]*?min-width:\s*0/)
  assert.match(listWxss, /\.source-secondary\s*\{[\s\S]*?width:\s*176rpx/)
  assert.match(listWxss, /\.source-rectify-btn\s*\{[\s\S]*?margin-left:\s*0/)
  assert.match(listWxss, /\.source-toolbar\s*\{[\s\S]*?height:\s*120rpx/)

  assert.doesNotMatch(detailWxml, /审批通过|审批不通过/)
  assert.doesNotMatch(detailWxml, />返回</)
  assert.match(detailWxml, /quick-shot-empty-photo/)
  assert.match(detailWxss, /\.quick-shot-detail-content/)
  assert.match(detailWxss, /\.quick-shot-photo-card/)
  assert.doesNotMatch(detailWxml, /source-submit/)
})

test("safety-check detail renders inspection fields, selectable units, and hazard detail modal", async () => {
  installWx()
  mockSafetyCheckOrganizationService()
  mockSourceService({
    getHazardRecord(moduleKey, id) {
      return Promise.resolve({
        id,
        status: "CLOSED",
        statusLabel: "已完成",
        companyId: 5,
        companyName: "Demo Materials Company",
        businessDate: "2026-06-04",
        payload: {
          inspectionUnitId: 1,
          inspectedUnitId: 5,
          checkType: "汛期安全检查，打通消防“生命通道”专项检查",
          manualCheckType: "吸取刘阳、山西煤矿事故教训安全检查",
          checkTime: "2026-06-04 15:12",
          creatorName: "熊万胜",
          checkMethod: "四不两直对安全管理情况开展检查，召开现场安全会议，日常检查",
          inspectors: ["李保云", "熊万胜", "梁楚铭", "徐倩倩"],
          acceptanceUsers: ["巫希晚"],
          inspectedUnitUsers: ["赖中偲", "陈志斌"],
          checkContent: "高力公司车间",
          acceptancePassed: "通过",
          acceptanceDate: "2026-06-12",
          attachments: [
            { name: "IMG20260604105201.jpg", size: "3.95MB" }
          ],
          checkItems: [
            {
              inspectedUnitId: 5,
              enableAi: "否",
              beforePhotos: ["/uploads/before.jpg"],
              hazardDescription: "1.高力公司员工存在不安全行为。2.高力公司安全风险分区图不清。",
              rectificationMeasures: "1.实操培训，规范员工行为。2.辨别风险，更新安全风险分区图。",
              rectificationResponsiblePerson: "陈志斌",
              rectificationDeadline: "2026-06-12",
              afterPhotos: ["/uploads/after1.jpg", "/uploads/after2.jpg"],
              rectificationStatusLabel: "已整改"
            }
          ]
        }
      })
    }
  })

  const page = capturePage("../pages/hazard/source-detail/index")
  await page.onLoad({ id: "51", moduleKey: "safety-check", title: "%E5%AE%89%E5%85%A8%E6%A3%80%E6%9F%A5" })

  assert.equal(page.data.title, "安全检查")
  assert.equal(page.data.safetyDetailTab, "basic")
  assert.deepEqual(page.data.inspectionUnitOptions.map(item => item.label), [
    "广东省Demo控股集团有限公司",
    "Demo Materials Company",
    "Demo Materials Company"
  ])
  assert.deepEqual(page.data.inspectedUnitOptions.map(item => item.label), [
    "Demo Materials Company",
    "Demo Materials Company"
  ])
  assert.equal(page.data.selectedInspectionUnitId, 1)
  assert.equal(page.data.selectedInspectedUnitId, 5)

  page.changeInspectionUnit({ detail: { value: 1 } })
  page.changeInspectedUnit({ detail: { value: 0 } })
  assert.equal(page.data.selectedInspectionUnitId, 4)
  assert.equal(page.data.selectedInspectedUnitId, 4)

  page.changeSafetyDetailTab({ currentTarget: { dataset: { tab: "hazards" } } })
  assert.equal(page.data.safetyDetailTab, "hazards")

  page.openHazardDetail({ currentTarget: { dataset: { index: 0 } } })
  assert.equal(page.data.showHazardDetail, true)
  assert.equal(page.data.currentHazardDetail.rectificationResponsiblePerson, "陈志斌")
  page.closeHazardDetail()
  assert.equal(page.data.showHazardDetail, false)

  const wxml = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-detail/index.wxml"), "utf8")
  assert.match(wxml, /<green-header[\s\S]*?bind:back="goBack"/)
  assert.doesNotMatch(wxml, /source-homebar|source-topbar/)
  assert.match(wxml, /safety-tabs/)
  assert.match(wxml, /safety-readonly-card/)
  assert.match(wxml, /safety-readonly-grid/)
  assert.match(wxml, /safety-readonly-row/)
  assert.match(wxml, /safety-readonly-box/)
  assert.match(wxml, /safety-readonly-chip-list/)
  assert.match(wxml, /safety-readonly-attachments/)
  assert.match(wxml, /safety-readonly-summary/)
  assert.match(wxml, /safety-readonly-hazard-card/)
  assert.match(wxml, /safety-readonly-photo-list/)
  assert.match(wxml, /status-badge/)
  assert.match(wxml, /检查单位/)
  assert.match(wxml, /受检单位/)
  assert.match(wxml, /受检项目/)
  assert.match(wxml, /检查类型（手录）/)
  assert.match(wxml, /检查日期/)
  assert.match(wxml, /检查时间/)
  assert.match(wxml, /创建人/)
  assert.match(wxml, /检查方式/)
  assert.match(wxml, /检查人员/)
  assert.match(wxml, /验收人员/)
  assert.match(wxml, /检查内容/)
  assert.match(wxml, /备注/)
  assert.match(wxml, /整改验收是否通过/)
  assert.match(wxml, /验收日期/)
  assert.match(wxml, /data-tab="hazards"[\s\S]*隐患明细/)
  assert.match(wxml, /safety-readonly-hazard-status/)
  assert.match(wxml, /是否启用AI/)
  assert.match(wxml, /整改前照片/)
  assert.match(wxml, /整改后照片/)
  assert.match(wxml, /整改责任人/)
  assert.doesNotMatch(wxml, /picker mode=/)
  assert.doesNotMatch(wxml, /<input/)
  assert.doesNotMatch(wxml, /textarea/)
  assert.doesNotMatch(wxml, /hazard-filter-row/)
  assert.doesNotMatch(wxml, /hazard-detail-btn/)

  const wxss = require("node:fs").readFileSync(require.resolve("../pages/hazard/source-detail/index.wxss"), "utf8")
  assert.doesNotMatch(wxml, /safety-back-btn/)
  assert.doesNotMatch(wxss, /\.safety-back-btn/)
  assert.match(wxss, /\.safety-readonly-card\s*\{/)
  assert.match(wxss, /\.safety-readonly-grid\s*\{/)
  assert.match(wxss, /\.safety-readonly-row\s*\{/)
  assert.match(wxss, /\.safety-readonly-box\s*\{/)
  assert.match(wxss, /\.safety-readonly-pill\s*\{[\s\S]*?color:\s*#1687ff/)
  assert.match(wxss, /\.safety-readonly-status\s*\{/)
  assert.match(wxss, /\.safety-readonly-status\.done\s*\{[\s\S]*?background:\s*#16a34a/)
  assert.match(wxss, /\.safety-readonly-status\.pending\s*\{[\s\S]*?background:\s*#f59e0b/)
  assert.match(wxss, /\.safety-readonly-hazard-card\s*\{/)
  assert.match(wxss, /\.safety-readonly-photo-list\s*\{/)
})

test("snapshot and safety-check routes point to source list while hazard points to rectification list", () => {
  const { getModuleById } = require("../config/modules")
  const routes = require("../utils/module-routes")

  assert.equal(getModuleById("hazard").route, "/pages/hazard-rectification/order-list/index")
  assert.equal(getModuleById("snapshot").route, "/pages/hazard/source-list/index?moduleKey=quick-shot&title=%E9%9A%8F%E6%89%8B%E6%8B%8D")
  assert.equal(getModuleById("safety-check").route, "/pages/hazard/source-list/index?moduleKey=safety-check&title=%E5%AE%89%E5%85%A8%E6%A3%80%E6%9F%A5")
  assert.equal(routes.getEntryRoute("hazard", "gs"), "/pages/hazard-rectification/order-list/index?companyKey=gs")
  assert.equal(routes.getEntryRoute("snapshot", "gs"), "/pages/hazard/source-list/index?moduleKey=quick-shot&title=%E9%9A%8F%E6%89%8B%E6%8B%8D&companyKey=gs")
  assert.equal(routes.getEntryRoute("safety-check", "gs"), "/pages/hazard/source-list/index?moduleKey=safety-check&title=%E5%AE%89%E5%85%A8%E6%A3%80%E6%9F%A5&companyKey=gs")
})

