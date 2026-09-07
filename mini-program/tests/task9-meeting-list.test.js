const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")

const servicePath = require.resolve("../services/mini-three-check")
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

test("meeting mapper converts pre-shift meeting records to prototype task rows", () => {
  const mapper = reload("../mappers/meeting")

  const rows = mapper.mapMeetingRecordsToRows([
    {
      id: "m1",
      meetingNo: "BQH-001",
      meetingDate: "2026-06-16",
      departmentName: "幕墙车间",
      teamName: "幕墙组装2班",
      ownerName: "蔡燕艳",
      status: "DRAFT",
      attendeeCount: 12,
      meetingContent: "班前安全交底"
    },
    {
      id: "m2",
      recordNo: "BQH-002",
      businessDate: "2026-06-17",
      payload: {
        departmentName: "检测中心",
        team: "检测中心班",
        owner: "戴军权",
        task: "设备点检"
      },
      status: "OPENED"
    }
  ])

  assert.deepEqual(rows, [
    {
      id: "m1",
      company: "-",
      dept: "幕墙车间",
      team: "幕墙组装2班",
      owner: "蔡燕艳",
      attendees: [],
      attendeesText: "",
      task: "班前安全交底",
      date: "2026-06-16",
      status: "待提交",
      statusClass: "muted",
      imageCheck: "-",
      videoCheck: "-",
      imageAttachments: [],
      videoAttachments: [],
      safetyConfirmItems: []
    },
    {
      id: "m2",
      company: "-",
      dept: "检测中心",
      team: "检测中心班",
      owner: "戴军权",
      attendees: [],
      attendeesText: "",
      task: "设备点检",
      date: "2026-06-17",
      status: "已提交",
      statusClass: "",
      imageCheck: "-",
      videoCheck: "-",
      imageAttachments: [],
      videoAttachments: [],
      safetyConfirmItems: []
    }
  ])
})

test("meeting list page loads pre-shift meetings into prototype list rows", async () => {
  const serviceCalls = []
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      listRecords(moduleKey, query) {
        serviceCalls.push({ moduleKey, query })
        if (query.pageSize === 1) {
          if (query.overdue === true) return Promise.resolve({ items: [], total: 4 })
          const totals = {
            DRAFT: 9,
            WITHDRAWN: 2,
            OPENED: 4,
            ARCHIVED: 5
          }
          return Promise.resolve({ items: [], total: totals[query.status] || 0 })
        }
        if (query.status === "WITHDRAWN") {
          return Promise.resolve({
            items: [
              {
                id: "m101",
                meetingDate: "2026-07-03",
                departmentName: "幕墙车间",
                teamName: "幕墙组装2班",
                ownerName: "蔡燕艳",
                meetingContent: "已撤回班前会",
                status: "WITHDRAWN"
              }
            ],
            total: 1
          })
        }
        return Promise.resolve({
          items: [
            {
              id: "m100",
              meetingDate: "2026-07-03",
              departmentName: "幕墙车间",
              teamName: "幕墙组装2班",
              ownerName: "蔡燕艳",
              meetingContent: "班前安全交底",
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
    global.__meetingListPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/meeting/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../mappers/meeting")]
  require("../pages/three-check/meeting/index")

  await global.__meetingListPage.onLoad({ companyKey: "gs" })

  assert.deepEqual(serviceCalls[0], { moduleKey: "pre-shift-meeting", query: { status: "DRAFT", page: 1, pageSize: 20, dateStart: "2026-07-03", dateEnd: "2026-07-03" } })
  assert.deepEqual(serviceCalls[1], { moduleKey: "pre-shift-meeting", query: { status: "WITHDRAWN", page: 1, pageSize: 20, dateStart: "2026-07-03", dateEnd: "2026-07-03" } })
  assert.equal(serviceCalls.some(call => call.query.status === "WITHDRAWN" && call.query.pageSize === 1), true)
  assert.equal(serviceCalls.some(call => call.query.status === "ARCHIVED" && call.query.pageSize === 1), true)
  assert.equal(global.__meetingListPage.data.companyKey, "gs")
  assert.equal(global.__meetingListPage.data.meetingDateStart, "2026-07-03")
  assert.equal(global.__meetingListPage.data.meetingDateEnd, "2026-07-03")
  assert.equal(global.__meetingListPage.data.loading, false)
  assert.equal(global.__meetingListPage.data.total, 2)
  assert.deepEqual(global.__meetingListPage.data.tabCounts, {
    pending: 11,
    done: 9,
    expired: 4
  })
  assert.equal(serviceCalls.some(call => call.query.overdue === true), true)
  assert.deepEqual(global.__meetingListPage.data.meetingRows, [
    {
      id: "m100",
      company: "-",
      dept: "幕墙车间",
      team: "幕墙组装2班",
      owner: "蔡燕艳",
      attendees: [],
      attendeesText: "",
      task: "班前安全交底",
      date: "2026-07-03",
      status: "待提交",
      statusClass: "muted",
      imageCheck: "-",
      videoCheck: "-",
      imageAttachments: [],
      videoAttachments: [],
      safetyConfirmItems: []
    },
    {
      id: "m101",
      company: "-",
      dept: "幕墙车间",
      team: "幕墙组装2班",
      owner: "蔡燕艳",
      attendees: [],
      attendeesText: "",
      task: "已撤回班前会",
      date: "2026-07-03",
      status: "待提交",
      statusClass: "muted",
      imageCheck: "-",
      videoCheck: "-",
      imageAttachments: [],
      videoAttachments: [],
      safetyConfirmItems: []
    }
  ])
})

test("meeting list page opens detail route and submits or withdraws records inline", async () => {
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
          payload: {
            safetyConfirmItems: [
              { riskType: "机械伤害", safetyItem: "严禁将手伸入设备运转区域", confirmStatus: "已确认" }
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
    global.__meetingListPage = {
      ...config,
      data: { ...config.data, companyKey: "gs" },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/meeting/index")
  delete require.cache[pagePath]
  require("../pages/three-check/meeting/index")

  global.__meetingListPage.openMeetingDetail({ currentTarget: { dataset: { id: "m100" } } })
  await global.__meetingListPage.submitMeeting({ currentTarget: { dataset: { id: "m100" } } })
  global.__meetingListPage.setData({ meetingListTab: "done" })
  await global.__meetingListPage.withdrawMeeting({ currentTarget: { dataset: { id: "m100" } } })

  assert.deepEqual(navigateCalls, [
    { url: "/pages/three-check/meeting-detail/index?id=m100&companyKey=gs" }
  ])
  assert.deepEqual(serviceCalls.filter(call => call.type !== "list" || call.query.pageSize === 20), [
    { type: "get", moduleKey: "pre-shift-meeting", id: "m100" },
    { type: "submit", moduleKey: "pre-shift-meeting", id: "m100" },
    { type: "list", moduleKey: "pre-shift-meeting", query: { status: "DRAFT", page: 1, pageSize: 20, dateStart: "2026-07-03", dateEnd: "2026-07-03" } },
    { type: "list", moduleKey: "pre-shift-meeting", query: { status: "WITHDRAWN", page: 1, pageSize: 20, dateStart: "2026-07-03", dateEnd: "2026-07-03" } },
    { type: "withdraw", moduleKey: "pre-shift-meeting", id: "m100", payload: { reason: "小程序撤回" } },
    { type: "list", moduleKey: "pre-shift-meeting", query: { status: "DRAFT", page: 1, pageSize: 20, dateStart: "2026-07-03", dateEnd: "2026-07-03" } },
    { type: "list", moduleKey: "pre-shift-meeting", query: { status: "WITHDRAWN", page: 1, pageSize: 20, dateStart: "2026-07-03", dateEnd: "2026-07-03" } }
  ])
  assert.equal(global.__meetingListPage.data.meetingListTab, "pending")
  assert.equal(serviceCalls.some(call => call.type === "list" && call.query.pageSize === 1), true)
})

test("meeting list page reminds records through mini api", async () => {
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
        return Promise.resolve({ id, moduleKey, reminderCount: 2 })
      }
    }
  }
  global.wx = {
    showToast(options) {
      toastTitle = options.title
    }
  }
  global.Page = config => {
    global.__meetingListPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/meeting/index")
  delete require.cache[pagePath]
  require("../pages/three-check/meeting/index")

  await global.__meetingListPage.remindMeeting({ currentTarget: { dataset: { id: "m100" } } })

  assert.equal(toastTitle, "已催一下")
  assert.equal(global.__meetingListPage.data.actionMessage, "已催一下")
  assert.deepEqual(serviceCalls.filter(call => call.type !== "list" || call.query.pageSize === 20), [
    { type: "remind", moduleKey: "pre-shift-meeting", id: "m100" },
    { type: "list", moduleKey: "pre-shift-meeting", query: { status: "DRAFT", page: 1, pageSize: 20, dateStart: "2026-07-03", dateEnd: "2026-07-03" } },
    { type: "list", moduleKey: "pre-shift-meeting", query: { status: "WITHDRAWN", page: 1, pageSize: 20, dateStart: "2026-07-03", dateEnd: "2026-07-03" } }
  ])
})

test("meeting list page applies date range filter to record query", async () => {
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
    navigateTo() {}
  }
  global.Page = config => {
    global.__meetingListPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/meeting/index")
  delete require.cache[pagePath]
  require("../pages/three-check/meeting/index")

  global.__meetingListPage.changeMeetingFilterDate({ currentTarget: { dataset: { field: "dateStart" } }, detail: { value: "2026-06-23" } })
  global.__meetingListPage.changeMeetingFilterDate({ currentTarget: { dataset: { field: "dateEnd" } }, detail: { value: "2026-06-30" } })
  await global.__meetingListPage.applyMeetingFilter()

  assert.equal(global.__meetingListPage.data.showMeetingFilter, false)
  assert.deepEqual(serviceCalls[0], {
    moduleKey: "pre-shift-meeting",
    query: {
      status: "DRAFT",
      page: 1,
      pageSize: 20,
      dateStart: "2026-06-23",
      dateEnd: "2026-06-30"
    }
  })
  assert.deepEqual(serviceCalls[1], {
    moduleKey: "pre-shift-meeting",
    query: {
      status: "WITHDRAWN",
      page: 1,
      pageSize: 20,
      dateStart: "2026-06-23",
      dateEnd: "2026-06-30"
    }
  })
})

test("meeting list submit asks for safety confirmations before submitting", async () => {
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
            safetyConfirmItems: [
              { riskType: "机械伤害", safetyItem: "严禁将手伸入设备运转区域", confirmStatus: "" }
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
    global.__meetingListPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/meeting/index")
  delete require.cache[pagePath]
  require("../pages/three-check/meeting/index")

  await global.__meetingListPage.submitMeeting({ currentTarget: { dataset: { id: "m101" } } })

  assert.equal(toastTitle, "请确认全部安全注意事项")
  assert.equal(global.__meetingListPage.data.errorText, "")
  assert.equal(global.__meetingListPage.data.actionMessage, "请确认全部安全注意事项")
  assert.deepEqual(navigateCalls, [])
  assert.deepEqual(serviceCalls, [{ type: "get", moduleKey: "pre-shift-meeting", id: "m101" }])
})

test("meeting list submits directly when no safety confirmation template exists", async () => {
  const serviceCalls = []
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      getRecord(moduleKey, id) {
        serviceCalls.push({ type: "get", moduleKey, id })
        return Promise.resolve({ id, payload: { safetyConfirmItems: [] } })
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
    global.__meetingListPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/meeting/index")
  delete require.cache[pagePath]
  require("../pages/three-check/meeting/index")

  await global.__meetingListPage.submitMeeting({ currentTarget: { dataset: { id: "m102" } } })

  assert.deepEqual(serviceCalls.slice(0, 2), [
    { type: "get", moduleKey: "pre-shift-meeting", id: "m102" },
    { type: "submit", moduleKey: "pre-shift-meeting", id: "m102" }
  ])
})

test("meeting list renders redesigned one-shift-three-check card shell without changing actions", () => {
  const pageWxml = fs.readFileSync(path.join(__dirname, "../pages/three-check/meeting/index.wxml"), "utf8")
  const pageWxss = fs.readFileSync(path.join(__dirname, "../pages/three-check/meeting/index.wxss"), "utf8")
  const pageJson = JSON.parse(fs.readFileSync(path.join(__dirname, "../pages/three-check/meeting/index.json"), "utf8"))

  assert.equal(pageJson.usingComponents["safe-card"], "/components/ui/safe-card/index")
  assert.equal(pageJson.usingComponents["state-view"], "/components/ui/state-view/index")
  assert.equal(pageJson.usingComponents["t-icon"], "tdesign-miniprogram/icon/icon")
  assert.equal(pageJson.usingComponents["green-header"], "/components/ui/green-header/index")
  assert.equal(pageJson.navigationBarTextStyle, "white")
  assert.equal(pageJson.navigationBarBackgroundColor, "#078249")
  assert.match(pageWxml, /<green-header title="班前会" bind:back="goBack" \/>/)
  assert.doesNotMatch(pageWxml, /meeting-statusbar|meeting-native-bar/)
  assert.match(pageWxss, /page\s*\{[^}]*margin:\s*0;[^}]*padding:\s*0;[^}]*overflow-x:\s*hidden;[^}]*background:\s*#f4f6f8;/s)
  assert.match(pageWxss, /\.page\s*\{[^}]*padding:\s*0;[^}]*max-width:\s*none;[^}]*overflow-x:\s*hidden;[^}]*background:\s*#f4f6f8;/s)
  assert.match(pageWxss, /\.meeting-page\s*\{[^}]*background:\s*#f4f6f8/s)
  assert.match(pageWxss, /\.meeting-tabs\s*\{[^}]*max-width:\s*none;/s)
  assert.match(pageWxss, /\.meeting-filter-row\s*\{[^}]*max-width:\s*none;/s)
  assert.match(pageWxss, /\.meeting-tabs\s*\{[^}]*width:\s*calc\(100vw \+ 2rpx\);[^}]*margin-left:\s*calc\(50% - 50vw\);/s)
  assert.match(pageWxss, /\.meeting-filter-row\s*\{[^}]*width:\s*calc\(100vw \+ 2rpx\);[^}]*margin-left:\s*calc\(50% - 50vw\);/s)
  assert.match(pageWxss, /\.meeting-task-list\s*\{[^}]*background:\s*#f4f6f8/s)
  assert.doesNotMatch(pageWxml, /meeting-status-time/)
  assert.doesNotMatch(pageWxml, /<t-icon[^>]+name="chart-bar"/)
  assert.doesNotMatch(pageWxml, /<t-icon[^>]+name="wifi"/)
  assert.doesNotMatch(pageWxml, /<t-icon[^>]+name="battery"/)
  assert.doesNotMatch(pageWxml, /meeting-native-capsule/)
  assert.match(pageWxml, /<view class="meeting-filter-trigger" bindtap="openMeetingFilter">/)
  assert.doesNotMatch(pageWxml, /<button class="meeting-filter-trigger"/)
  assert.match(pageWxml, /<t-icon[^>]+name="filter"/)
  assert.match(pageWxml, /safe-card[^>]*data-id="\{\{item\.id\}\}"[^>]*bindtap="openMeetingDetail"[^>]*class="meeting-reference-card"/)
  assert.match(pageWxml, /meeting-field-row meeting-field-row--date/)
  assert.match(pageWxml, /meeting-field-row--date[\s\S]*<t-icon[^>]+name="calendar"/)
  assert.match(pageWxml, /meeting-field-row meeting-field-row--team/)
  assert.match(pageWxml, /meeting-field-row--team[\s\S]*<t-icon[^>]+name="building"/)
  assert.match(pageWxml, /meeting-field-row meeting-field-row--owner/)
  assert.match(pageWxml, /meeting-field-row--owner[\s\S]*<t-icon[^>]+name="user"/)
  assert.match(pageWxml, /meeting-field-row meeting-field-row--content/)
  assert.match(pageWxml, /meeting-field-row--content[\s\S]*<t-icon[^>]+name="file-paste"/)
  assert.match(pageWxml, /meeting-status-pill/)
  assert.match(pageWxml, /item\.status === '已提交' \? 'done' : ''/)
  assert.match(pageWxss, /\.meeting-status-pill\.done,[\s\S]*?border-radius:\s*999rpx;[\s\S]*?background:\s*#078249;[\s\S]*?color:\s*#ffffff;/)
  assert.match(pageWxml, /state-view[\s\S]*loadingText="正在加载班前会"/)
  assert.match(pageWxml, /state-view[\s\S]*emptyText="没有更多了"/)

  for (const action of ["openMeetingDetail", "submitMeeting", "withdrawMeeting", "remindMeeting"]) {
    assert.match(pageWxml, new RegExp(`catchtap="${action}"`))
  }
  assert.match(pageWxml, /catchtap="openMeetingDetail"[\s\S]*>明细<\/view>/)

  assert.doesNotMatch(pageWxss, /\.meeting-status\s*\{[^}]*display:\s*none/s)
  assert.doesNotMatch(pageWxss, /\.meeting-status-(time|icons|icon|battery)/)
  assert.doesNotMatch(pageWxss, /\.meeting-filter-icon::/)
  assert.doesNotMatch(pageWxss, /\.meeting-field-row--(?:date|team|owner|content) \.meeting-field-icon/)
  assert.match(pageWxss, /\.meeting-action-btn\s*\{[\s\S]*border-radius:\s*16rpx;/)
  assert.match(pageWxss, /\.meeting-tabs\s*\{[\s\S]*padding:\s*0;[\s\S]*grid-template-columns:\s*repeat\(3,\s*1fr\);/)
  assert.match(pageWxss, /\.meeting-filter-row\s*\{[\s\S]*padding:\s*0 0 0 36rpx;[\s\S]*justify-content:\s*flex-start;/)
  assert.match(pageWxss, /\.meeting-filter-trigger\s*\{[\s\S]*margin:\s*0;[\s\S]*padding:\s*0;[\s\S]*position:\s*relative;[\s\S]*left:\s*0;/)
  assert.match(pageWxss, /\.meeting-task-list\s*\{[\s\S]*padding:\s*22rpx 0 0;/)
})
