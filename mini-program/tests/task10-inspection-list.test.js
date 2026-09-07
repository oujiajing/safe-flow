const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")

const servicePath = require.resolve("../services/mini-three-check")
const hazardOrderServicePath = require.resolve("../services/hazard-rectification-order")
const RealDate = global.Date

class FrozenDate extends RealDate {
  constructor(...args) {
    if (args.length === 0) {
      super("2026-07-03T08:00:00+08:00")
      return
    }
    super(...args)
  }

  static now() {
    return new RealDate("2026-07-03T08:00:00+08:00").getTime()
  }
}

global.Date = FrozenDate

function reload(modulePath) {
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  return require(modulePath)
}

test("inspection mapper converts three-check records to prototype check rows", () => {
  const mapper = reload("../mappers/inspection")

  const rows = mapper.mapInspectionRecordsToRows([
    {
      id: "c1",
      businessDate: "2026-06-16",
      departmentName: "幕墙车间",
      teamName: "幕墙组装2班",
      ownerName: "沈光明",
      content: "高处作业安全检查",
      status: "DRAFT"
    },
    {
      id: "c2",
      date: "2026-06-17",
      payload: {
        department: "检测中心",
        team: "检测中心班",
        owner: "戴军权",
        task: "设备点检"
      },
      status: "OPENED"
    }
  ])

  assert.deepEqual(rows, [
    {
      id: "c1",
      dept: "幕墙车间",
      team: "幕墙组装2班",
      owner: "沈光明",
      task: "高处作业安全检查",
      date: "2026-06-16",
      status: "待提交",
      statusClass: "pending"
    },
    {
      id: "c2",
      dept: "检测中心",
      team: "检测中心班",
      owner: "戴军权",
      task: "设备点检",
      date: "2026-06-17",
      status: "已提交",
      statusClass: "done"
    }
  ])
})

test("inspection list page loads selected module records into prototype rows", async () => {
  const serviceCalls = []
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      listRecords(moduleKey, query) {
        serviceCalls.push({ moduleKey, query })
        if (query.pageSize === 1) {
          if (query.overdue === true) return Promise.resolve({ items: [], total: 3 })
          const totals = {
            DRAFT: 9,
            WITHDRAWN: 2,
            OPENED: 4,
            ARCHIVED: 5
          }
          return Promise.resolve({ items: [], total: totals[query.status] || 0 })
        }
        return Promise.resolve({
          items: [
            {
              id: "c100",
              businessDate: "2026-06-16",
              departmentName: "幕墙车间",
              teamName: "幕墙组装2班",
              ownerName: "沈光明",
              content: "高处作业安全检查",
              status: "DRAFT"
            }
          ],
          total: 1
        })
      }
    }
  }

  global.wx = {
    navigateTo() {}
  }
  global.Page = config => {
    global.__inspectionListPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/inspection-list/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../mappers/inspection")]
  require("../pages/three-check/inspection-list/index")

  await global.__inspectionListPage.onLoad({
    moduleId: "during-check",
    moduleKey: "mid-shift-inspection",
    companyKey: "gs"
  })

  assert.equal(serviceCalls[0].moduleKey, "mid-shift-inspection")
  assert.deepEqual(serviceCalls[0].query, { status: "DRAFT", page: 1, pageSize: 20, dateStart: "2026-07-03", dateEnd: "2026-07-03" })
  assert.equal(serviceCalls.some(call => call.query.status === "WITHDRAWN" && call.query.pageSize === 1), true)
  assert.equal(serviceCalls.some(call => call.query.status === "ARCHIVED" && call.query.pageSize === 1), true)
  assert.equal(global.__inspectionListPage.data.moduleId, "during-check")
  assert.equal(global.__inspectionListPage.data.moduleKey, "mid-shift-inspection")
  assert.equal(global.__inspectionListPage.data.companyKey, "gs")
  assert.equal(global.__inspectionListPage.data.inspectionDateStart, "2026-07-03")
  assert.equal(global.__inspectionListPage.data.inspectionDateEnd, "2026-07-03")
  assert.equal(global.__inspectionListPage.data.total, 1)
  assert.deepEqual(global.__inspectionListPage.data.tabCounts, {
    pending: 11,
    done: 9,
    expired: 3
  })
  assert.equal(serviceCalls.some(call => call.query.overdue === true), true)
  assert.deepEqual(global.__inspectionListPage.data.inspectionRows, [
    {
      id: "c100",
      dept: "幕墙车间",
      team: "幕墙组装2班",
      owner: "沈光明",
      task: "高处作业安全检查",
      date: "2026-06-16",
      status: "待提交",
      statusClass: "pending"
    }
  ])
})

test("inspection list page opens detail route and submits or withdraws records inline", async () => {
  const navigateCalls = []
  const serviceCalls = []
  global.wx = {
    navigateTo(options) {
      navigateCalls.push(options)
    },
    showToast() {}
  }
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      listRecords(moduleKey, query) {
        serviceCalls.push({ type: "list", moduleKey, query })
        return Promise.resolve({ items: [], total: 0 })
      },
      getRecord(moduleKey, id) {
        serviceCalls.push({ type: "get", moduleKey, id })
        return Promise.resolve({
          id,
          moduleKey,
          payload: {
            checkItems: [
              { riskType: "机械伤害", checkItem: "检查机械设备", checkResult: "无隐患" }
            ]
          }
        })
      },
      submitRecord(moduleKey, id) {
        serviceCalls.push({ type: "submit", moduleKey, id })
        return Promise.resolve({ id, moduleKey, status: "OPENED" })
      },
      withdrawRecord(moduleKey, id, payload) {
        serviceCalls.push({ type: "withdraw", moduleKey, id, payload })
        return Promise.resolve({ id, moduleKey, status: "WITHDRAWN" })
      }
    }
  }
  global.Page = config => {
    global.__inspectionListPage = {
      ...config,
      data: { ...config.data, moduleId: "before-check", moduleKey: "pre-shift-inspection", companyKey: "gs" },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/inspection-list/index")
  delete require.cache[pagePath]
  require("../pages/three-check/inspection-list/index")

  global.__inspectionListPage.openInspectionDetail({ currentTarget: { dataset: { id: "c100" } } })
  await global.__inspectionListPage.submitInspection({ currentTarget: { dataset: { id: "c100" } } })
  await global.__inspectionListPage.withdrawInspection({ currentTarget: { dataset: { id: "c100" } } })

  assert.deepEqual(navigateCalls, [
    { url: "/pages/three-check/inspection-detail/index?id=c100&moduleId=before-check&moduleKey=pre-shift-inspection&companyKey=gs" }
  ])
  assert.deepEqual(serviceCalls.filter(call => call.type !== "list" || call.query.pageSize === 20), [
    { type: "get", moduleKey: "pre-shift-inspection", id: "c100" },
    { type: "submit", moduleKey: "pre-shift-inspection", id: "c100" },
    { type: "list", moduleKey: "pre-shift-inspection", query: { status: "DRAFT", page: 1, pageSize: 20, dateStart: "2026-07-03", dateEnd: "2026-07-03" } },
    { type: "withdraw", moduleKey: "pre-shift-inspection", id: "c100", payload: { reason: "小程序撤回" } },
    { type: "list", moduleKey: "pre-shift-inspection", query: { status: "DRAFT", page: 1, pageSize: 20, dateStart: "2026-07-03", dateEnd: "2026-07-03" } }
  ])
  assert.equal(serviceCalls.some(call => call.type === "list" && call.query.pageSize === 1), true)
})

test("inspection list page reminds records through mini api", async () => {
  const serviceCalls = []
  let toastTitle = ""
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      listRecords(moduleKey, query) {
        serviceCalls.push({ type: "list", moduleKey, query })
        return Promise.resolve({ items: [], total: 0 })
      },
      remindRecord(moduleKey, id) {
        serviceCalls.push({ type: "remind", moduleKey, id })
        return Promise.resolve({ id, moduleKey, reminderCount: 1 })
      }
    }
  }
  global.wx = {
    showToast(options) {
      toastTitle = options.title
    },
    setNavigationBarTitle() {}
  }
  global.Page = config => {
    global.__inspectionListPage = {
      ...config,
      data: { ...config.data, moduleKey: "mid-shift-inspection" },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/inspection-list/index")
  delete require.cache[pagePath]
  require("../pages/three-check/inspection-list/index")

  await global.__inspectionListPage.remindInspection({ currentTarget: { dataset: { id: "c100" } } })

  assert.equal(toastTitle, "已催一下")
  assert.equal(global.__inspectionListPage.data.actionMessage, "已催一下")
  assert.deepEqual(serviceCalls.filter(call => call.type !== "list" || call.query.pageSize === 20), [
    { type: "remind", moduleKey: "mid-shift-inspection", id: "c100" },
    { type: "list", moduleKey: "mid-shift-inspection", query: { status: "DRAFT", page: 1, pageSize: 20, dateStart: "2026-07-03", dateEnd: "2026-07-03" } }
  ])
})

test("inspection list opens rectification order through mini api", async () => {
  const navigateCalls = []
  const serviceCalls = []
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      openRectificationOrder(moduleKey, id) {
        serviceCalls.push({ type: "openOrder", moduleKey, id })
        return Promise.resolve({ id: "9001", orderNo: "HR-9001" })
      }
    }
  }
  global.wx = {
    navigateTo(options) {
      navigateCalls.push(options)
    },
    showToast() {},
    setNavigationBarTitle() {}
  }
  global.Page = config => {
    global.__inspectionListPage = {
      ...config,
      data: { ...config.data, moduleKey: "post-shift-inspection", moduleId: "after-check", companyKey: "gs" },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/inspection-list/index")
  delete require.cache[pagePath]
  require("../pages/three-check/inspection-list/index")

  await global.__inspectionListPage.openRectificationOrder({
    currentTarget: { dataset: { id: "c300", canCreateRectificationOrder: true } }
  })

  assert.deepEqual(serviceCalls, [{ type: "openOrder", moduleKey: "post-shift-inspection", id: "c300" }])
  assert.deepEqual(navigateCalls, [
    { url: "/pages/hazard-rectification/order-detail/index?id=9001&sourceRecordId=c300&moduleKey=post-shift-inspection&companyKey=gs" }
  ])
})

test("inspection list blocks manual rectification order trigger without action and record capability", async () => {
  const serviceCalls = []
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      openRectificationOrder(moduleKey, id) {
        serviceCalls.push({ moduleKey, id })
        return Promise.resolve({ id: "9001" })
      }
    }
  }
  global.wx = {
    navigateTo() {},
    showToast() {},
    setNavigationBarTitle() {}
  }
  global.Page = config => {
    global.__inspectionListPage = {
      ...config,
      data: {
        ...config.data,
        moduleKey: "pre-shift-inspection",
        canCreateRectificationOrderAction: false
      },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/inspection-list/index")
  delete require.cache[pagePath]
  require("../pages/three-check/inspection-list/index")

  await global.__inspectionListPage.openRectificationOrder({
    currentTarget: { dataset: { id: "c301", canCreateRectificationOrder: true } }
  })
  global.__inspectionListPage.data.canCreateRectificationOrderAction = true
  await global.__inspectionListPage.openRectificationOrder({
    currentTarget: { dataset: { id: "c301", canCreateRectificationOrder: false } }
  })

  assert.deepEqual(serviceCalls, [])
})

test("rectification order detail loads through mini source endpoint when source params exist", async () => {
  const serviceCalls = []
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      openRectificationOrder(moduleKey, id) {
        serviceCalls.push({ type: "openOrder", moduleKey, id })
        return Promise.resolve({ id: "9001", orderNo: "HR-9001", statusLabel: "待派发" })
      }
    }
  }
  require.cache[hazardOrderServicePath] = {
    id: hazardOrderServicePath,
    filename: hazardOrderServicePath,
    loaded: true,
    exports: {
      getOrder(id) {
        serviceCalls.push({ type: "getOrder", id })
        return Promise.resolve({ id, orderNo: "HR-PC" })
      }
    }
  }
  global.wx = {
    navigateBack() {}
  }
  global.Page = config => {
    global.__rectificationOrderDetailPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/hazard-rectification/order-detail/index")
  delete require.cache[pagePath]
  require("../pages/hazard-rectification/order-detail/index")

  await global.__rectificationOrderDetailPage.onLoad({
    id: "9001",
    sourceRecordId: "c300",
    moduleKey: "post-shift-inspection"
  })

  assert.deepEqual(serviceCalls, [{ type: "openOrder", moduleKey: "post-shift-inspection", id: "c300" }])
  assert.deepEqual(global.__rectificationOrderDetailPage.data.order, {
    id: "9001",
    orderNo: "HR-9001",
    statusLabel: "待派发"
  })
  assert.equal(global.__rectificationOrderDetailPage.data.id, "9001")
})

test("inspection list page applies date range filter to record query", async () => {
  const serviceCalls = []
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      listRecords(moduleKey, query) {
        serviceCalls.push({ moduleKey, query })
        return Promise.resolve({ items: [], total: 0 })
      }
    }
  }
  global.wx = {
    navigateTo() {},
    setNavigationBarTitle() {}
  }
  global.Page = config => {
    global.__inspectionListPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/inspection-list/index")
  delete require.cache[pagePath]
  require("../pages/three-check/inspection-list/index")

  await global.__inspectionListPage.onLoad({
    moduleId: "during-check",
    moduleKey: "mid-shift-inspection",
    companyKey: "gs"
  })
  serviceCalls.length = 0
  global.__inspectionListPage.changeInspectionFilterDate({ currentTarget: { dataset: { field: "dateStart" } }, detail: { value: "2026-06-23" } })
  global.__inspectionListPage.changeInspectionFilterDate({ currentTarget: { dataset: { field: "dateEnd" } }, detail: { value: "2026-06-30" } })
  await global.__inspectionListPage.applyInspectionFilter()

  assert.equal(global.__inspectionListPage.data.showInspectionFilter, false)
  assert.deepEqual(serviceCalls[0], {
    moduleKey: "mid-shift-inspection",
    query: {
      status: "DRAFT",
      page: 1,
      pageSize: 20,
      dateStart: "2026-06-23",
      dateEnd: "2026-06-30"
    }
  })
})

test("inspection list submit asks for check results before submitting", async () => {
  const serviceCalls = []
  const navigateCalls = []
  let toastTitle = ""
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      getRecord(moduleKey, id) {
        serviceCalls.push({ type: "get", moduleKey, id })
        return Promise.resolve({
          id,
          payload: {
            checkItems: [
              { riskType: "机械伤害", checkItem: "检查机械设备", checkResult: "" }
            ]
          }
        })
      },
      submitRecord(moduleKey, id) {
        serviceCalls.push({ type: "submit", moduleKey, id })
        return Promise.resolve({ id })
      }
    }
  }
  global.wx = {
    navigateTo(options) {
      navigateCalls.push(options)
    },
    showToast(options) {
      toastTitle = options.title
    }
  }
  global.Page = config => {
    global.__inspectionListPage = {
      ...config,
      data: { ...config.data, moduleKey: "pre-shift-inspection" },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/inspection-list/index")
  delete require.cache[pagePath]
  require("../pages/three-check/inspection-list/index")

  await global.__inspectionListPage.submitInspection({ currentTarget: { dataset: { id: "c101" } } })

  assert.equal(toastTitle, "请填写检查结果")
  assert.equal(global.__inspectionListPage.data.errorText, "")
  assert.equal(global.__inspectionListPage.data.actionMessage, "请填写检查结果")
  assert.deepEqual(navigateCalls, [])
  assert.deepEqual(serviceCalls, [{ type: "get", moduleKey: "pre-shift-inspection", id: "c101" }])
})

test("inspection list submits directly when no check item template exists", async () => {
  const serviceCalls = []
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      getRecord(moduleKey, id) {
        serviceCalls.push({ type: "get", moduleKey, id })
        return Promise.resolve({ id, payload: { checkItems: [] } })
      },
      submitRecord(moduleKey, id) {
        serviceCalls.push({ type: "submit", moduleKey, id })
        return Promise.resolve({ id })
      },
      listRecords() {
        return Promise.resolve({ items: [], total: 0 })
      }
    }
  }
  global.wx = { showToast() {} }
  global.Page = config => {
    global.__inspectionListPage = {
      ...config,
      data: { ...config.data, moduleKey: "pre-shift-inspection" },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/inspection-list/index")
  delete require.cache[pagePath]
  require("../pages/three-check/inspection-list/index")

  await global.__inspectionListPage.submitInspection({ currentTarget: { dataset: { id: "c102" } } })

  assert.deepEqual(serviceCalls.slice(0, 2), [
    { type: "get", moduleKey: "pre-shift-inspection", id: "c102" },
    { type: "submit", moduleKey: "pre-shift-inspection", id: "c102" }
  ])
})

test("inspection list uses meeting-like card layout with TDesign icons", () => {
  const listWxml = fs.readFileSync(path.join(__dirname, "../pages/three-check/inspection-list/index.wxml"), "utf8")
  const listWxss = fs.readFileSync(path.join(__dirname, "../pages/three-check/inspection-list/index.wxss"), "utf8")
  const listJson = JSON.parse(fs.readFileSync(path.join(__dirname, "../pages/three-check/inspection-list/index.json"), "utf8"))

  assert.equal(listJson.navigationStyle, "custom")
  assert.equal(listJson.navigationBarTextStyle, "white")
  assert.equal(listJson.navigationBarBackgroundColor, "#078249")
  assert.equal(listJson.usingComponents["safe-card"], "/components/ui/safe-card/index")
  assert.equal(listJson.usingComponents["state-view"], "/components/ui/state-view/index")
  assert.equal(listJson.usingComponents["t-icon"], "tdesign-miniprogram/icon/icon")
  assert.equal(listJson.usingComponents["green-header"], "/components/ui/green-header/index")
  assert.match(listWxml, /<green-header title="\{\{moduleTitle\}\}" bind:back="goBack" \/>/)
  assert.match(listWxss, /page\s*\{[^}]*margin:\s*0;[^}]*padding:\s*0;[^}]*overflow-x:\s*hidden;[^}]*background:\s*#f4f6f8;/s)
  assert.match(listWxss, /\.page\s*\{[^}]*padding:\s*0;[^}]*max-width:\s*none;[^}]*overflow-x:\s*hidden;[^}]*background:\s*#f4f6f8;/s)
  assert.match(listWxss, /\.inspection-page\s*\{[^}]*background:\s*#f4f6f8/s)
  assert.match(listWxss, /\.inspection-tabs\s*\{[^}]*max-width:\s*none;/s)
  assert.match(listWxss, /\.inspection-filter-row\s*\{[^}]*max-width:\s*none;/s)
  assert.match(listWxss, /\.inspection-tabs\s*\{[^}]*width:\s*calc\(100vw \+ 2rpx\);[^}]*margin-left:\s*calc\(50% - 50vw\);/s)
  assert.match(listWxss, /\.inspection-filter-row\s*\{[^}]*width:\s*calc\(100vw \+ 2rpx\);[^}]*margin-left:\s*calc\(50% - 50vw\);/s)
  assert.match(listWxss, /\.inspection-task-list\s*\{[^}]*background:\s*#f4f6f8/s)
  assert.match(listWxml, /\{\{moduleTitle\}\}/)
  assert.match(listWxml, /name="filter"/)
  assert.match(listWxml, /safe-card/)
  assert.match(listWxml, /state-view[^>]+loadingText="正在加载检查记录"/)
  assert.match(listWxml, /state-view[^>]+emptyText="没有更多了"/)
  assert.match(listWxml, /catchtap="openInspectionDetail"/)
  assert.match(listWxml, /catchtap="submitInspection"/)
  assert.match(listWxml, /catchtap="withdrawInspection"/)
  assert.match(listWxml, /catchtap="remindInspection"/)
  assert.match(listWxml, /catchtap="openRectificationOrder"/)
  assert.match(listWxss, /\.inspection-status-pill\s*\{[^}]*border-radius:\s*999rpx[^}]*background:\s*#fff1e5[^}]*color:\s*#ea580c/s)
  assert.match(listWxss, /\.inspection-status-pill\.done,[\s\S]*?background:\s*#078249[\s\S]*?color:\s*#ffffff/s)
  assert.doesNotMatch(listWxml, /9:41|chart-bar|wifi|battery/)
  assert.doesNotMatch(listWxml, /filter-simple-top|filter-simple-stem|before-check-icon/)
  assert.doesNotMatch(listWxml, /three-check-statusbar|inspection-native-bar/)
})
