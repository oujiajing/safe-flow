const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")

const threeCheckServicePath = require.resolve("../services/mini-three-check")
const flowServicePath = require.resolve("../services/flow")
const organizationServicePath = require.resolve("../services/organization")

const orgTree = [
  {
    id: 1,
    title: "广晟控股集团",
    orgType: "GROUP",
    children: [
      {
        id: 4,
        title: "广晟源成",
        orgType: "COMPANY",
        children: [
          {
            id: 101109,
            title: "幕墙组装",
            orgType: "DEPARTMENT",
            children: [
              { id: 1011001, title: "幕墙组装1班", orgType: "TEAM", children: [] },
              { id: 1011002, title: "幕墙组装2班", orgType: "TEAM", children: [] }
            ]
          }
        ]
      }
    ]
  }
]

function stubOrganizationTree(nodes = orgTree) {
  require.cache[organizationServicePath] = {
    id: organizationServicePath,
    filename: organizationServicePath,
    loaded: true,
    exports: {
      getOrgTree() {
        return Promise.resolve(nodes)
      }
    }
  }
}

function stubOrganizationFailure() {
  require.cache[organizationServicePath] = {
    id: organizationServicePath,
    filename: organizationServicePath,
    loaded: true,
    exports: {
      getOrgTree() {
        return Promise.reject(new Error("timeout"))
      }
    }
  }
}

function todayString() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, "0")
  const day = String(now.getDate()).padStart(2, "0")
  return `${now.getFullYear()}-${month}-${day}`
}

test("dispatch list routes create and detail to new dispatch pages", () => {
  const navigateCalls = []
  global.wx = {
    navigateTo(options) {
      navigateCalls.push(options)
    }
  }
  global.Page = config => {
    global.__dispatchListPage = {
      ...config,
      data: { ...config.data, companyKey: "gs" }
    }
  }

  const pagePath = require.resolve("../pages/dispatch/list/index")
  delete require.cache[pagePath]
  require("../pages/dispatch/list/index")

  global.__dispatchListPage.openCreate()
  global.__dispatchListPage.openDetail({ currentTarget: { dataset: { id: "d100" } } })

  assert.deepEqual(navigateCalls, [
    { url: "/pages/dispatch/form/index?companyKey=gs" },
    { url: "/pages/dispatch/record-detail/index?id=d100&companyKey=gs" }
  ])
})

test("dispatch form creates and submits simplified team dispatch payload", async () => {
  const serviceCalls = []
  stubOrganizationTree()
  require.cache[threeCheckServicePath] = {
    id: threeCheckServicePath,
    filename: threeCheckServicePath,
    loaded: true,
    exports: {
      createRecord(moduleKey, payload) {
        serviceCalls.push({ type: "create", moduleKey, payload })
        return Promise.resolve({ id: "d200" })
      },
      submitRecord(moduleKey, id) {
        serviceCalls.push({ type: "submit", moduleKey, id })
        return Promise.resolve({ id })
      }
    }
  }
  require.cache[flowServicePath] = {
    id: flowServicePath,
    filename: flowServicePath,
    loaded: true,
    exports: {
      getDispatchFlow(id) {
        serviceCalls.push({ type: "flow", id })
        return Promise.resolve({
          stages: [
            { stageKey: "teamDispatch", record: { id } },
            { stageKey: "preShiftMeeting", record: { id: "m200", moduleKey: "pre-shift-meeting" } },
            { stageKey: "preShiftInspection", record: { id: "i201", moduleKey: "pre-shift-inspection" } },
            { stageKey: "midShiftInspection", record: { id: "i202", moduleKey: "mid-shift-inspection" } },
            { stageKey: "postShiftInspection", record: { id: "i203", moduleKey: "post-shift-inspection" } }
          ]
        })
      }
    }
  }

  global.wx = {
    showToast() {},
    navigateBack() {},
    removeStorageSync() {}
  }
  global.Page = config => {
    global.__dispatchFormPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/dispatch/form/index")
  delete require.cache[pagePath]
  require("../pages/dispatch/form/index")

  await global.__dispatchFormPage.onLoad({ companyKey: "gs" })
  global.__dispatchFormPage.changeDispatchTeam({ detail: { value: "1" } })
  global.__dispatchFormPage.updateDispatchRemark({ detail: { value: "现场加固区域注意交叉作业" } })
  global.__dispatchFormPage.changeDispatchDate({ detail: { value: "2026-06-16" } })
  await global.__dispatchFormPage.submitDispatch()

  assert.equal(serviceCalls[0].type, "create")
  assert.equal(serviceCalls[0].moduleKey, "team-dispatch")
  assert.equal(serviceCalls[0].payload.businessDate, "2026-06-16")
  assert.equal(serviceCalls[0].payload.companyId, 4)
  assert.equal(serviceCalls[0].payload.departmentId, 101109)
  assert.equal(serviceCalls[0].payload.teamId, 1011002)
  assert.equal(serviceCalls[0].payload.content, "班组派班")
  assert.equal(serviceCalls[0].payload.payload.companyName, "广晟源成")
  assert.equal(serviceCalls[0].payload.payload.departmentName, "幕墙组装")
  assert.equal(serviceCalls[0].payload.payload.teamName, "幕墙组装2班")
  assert.equal(serviceCalls[0].payload.payload.task, "班组派班")
  assert.equal(serviceCalls[0].payload.payload.dispatchType, "今日")
  assert.equal(serviceCalls[0].payload.payload.managementCount, "0")
  assert.equal(serviceCalls[0].payload.payload.remark, "现场加固区域注意交叉作业")
  assert.match(serviceCalls[0].payload.sourceRecordId, /^wx-local-team-dispatch-/)
  assert.match(serviceCalls[0].payload.clientRequestId, /^wx-req-/)
  assert.equal(serviceCalls[1].type, "submit")
  assert.deepEqual(serviceCalls[1], { type: "submit", moduleKey: "team-dispatch", id: "d200" })
  assert.deepEqual(serviceCalls[2], { type: "flow", id: "d200" })
  assert.equal(global.__dispatchFormPage.data.submitting, false)
  assert.equal(global.__dispatchFormPage.data.operationResult, "派班成功，已生成4个一班三查任务")
  assert.equal(global.__dispatchFormPage.data.generatedThreeCheckCount, 4)
})

test("dispatch submit creates records that today's one-shift-three-check lists can load", async () => {
  const serviceCalls = []
  const RealDate = global.Date
  class FakeDate extends RealDate {
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
  global.Date = FakeDate
  stubOrganizationTree()
  require.cache[threeCheckServicePath] = {
    id: threeCheckServicePath,
    filename: threeCheckServicePath,
    loaded: true,
    exports: {
      createRecord(moduleKey, payload) {
        serviceCalls.push({ type: "create", moduleKey, payload })
        return Promise.resolve({ id: "d210" })
      },
      submitRecord(moduleKey, id) {
        serviceCalls.push({ type: "submit", moduleKey, id })
        return Promise.resolve({ id, status: "OPENED" })
      },
      listRecords(moduleKey, query) {
        serviceCalls.push({ type: "list", moduleKey, query })
        if (query.pageSize === 1) {
          return Promise.resolve({ items: [], total: 1 })
        }
        if (query.status === "DRAFT" && query.dateStart === "2026-07-03" && query.dateEnd === "2026-07-03") {
          if (moduleKey === "pre-shift-meeting") {
            return Promise.resolve({
              items: [
                {
                  id: "m210",
                  businessDate: "2026-07-03",
                  departmentName: "幕墙组装",
                  teamName: "幕墙组装2班",
                  meetingContent: "派班自动生成班前会",
                  status: "DRAFT"
                }
              ],
              total: 1
            })
          }
          if (moduleKey === "pre-shift-inspection") {
            return Promise.resolve({
              items: [
                {
                  id: "i210",
                  businessDate: "2026-07-03",
                  departmentName: "幕墙组装",
                  teamName: "幕墙组装2班",
                  content: "派班自动生成班前检查",
                  status: "DRAFT"
                }
              ],
              total: 1
            })
          }
        }
        return Promise.resolve({ items: [], total: 0 })
      }
    }
  }
  require.cache[flowServicePath] = {
    id: flowServicePath,
    filename: flowServicePath,
    loaded: true,
    exports: {
      getDispatchFlow(id) {
        serviceCalls.push({ type: "flow", id })
        return Promise.resolve({
          stages: [
            { stageKey: "teamDispatch", record: { id } },
            { stageKey: "preShiftMeeting", record: { id: "m210", moduleKey: "pre-shift-meeting" } },
            { stageKey: "preShiftInspection", record: { id: "i210", moduleKey: "pre-shift-inspection" } },
            { stageKey: "midShiftInspection", record: { id: "i211", moduleKey: "mid-shift-inspection" } },
            { stageKey: "postShiftInspection", record: { id: "i212", moduleKey: "post-shift-inspection" } }
          ]
        })
      }
    }
  }

  try {
    global.wx = {
      showToast() {},
      navigateBack() {},
      setNavigationBarTitle() {}
    }
    global.Page = config => {
      global.__dispatchFormPage = {
        ...config,
        data: { ...config.data },
        setData(patch) {
          this.data = { ...this.data, ...patch }
        }
      }
    }

    delete require.cache[require.resolve("../pages/dispatch/form/index")]
    require("../pages/dispatch/form/index")
    await global.__dispatchFormPage.onLoad({ companyKey: "gs" })
    global.__dispatchFormPage.changeDispatchTeam({ detail: { value: "1" } })
    await global.__dispatchFormPage.submitDispatch()

    global.Page = config => {
      global.__meetingListPage = {
        ...config,
        data: { ...config.data },
        setData(patch) {
          this.data = { ...this.data, ...patch }
        }
      }
    }
    delete require.cache[require.resolve("../utils/three-check-stats")]
    delete require.cache[require.resolve("../pages/three-check/meeting/index")]
    delete require.cache[require.resolve("../mappers/meeting")]
    require("../pages/three-check/meeting/index")
    await global.__meetingListPage.onLoad({ companyKey: "gs" })

    global.Page = config => {
      global.__inspectionListPage = {
        ...config,
        data: { ...config.data },
        setData(patch) {
          this.data = { ...this.data, ...patch }
        }
      }
    }
    delete require.cache[require.resolve("../utils/three-check-stats")]
    delete require.cache[require.resolve("../pages/three-check/inspection-list/index")]
    delete require.cache[require.resolve("../mappers/inspection")]
    require("../pages/three-check/inspection-list/index")
    await global.__inspectionListPage.onLoad({
      moduleId: "before-check",
      moduleKey: "pre-shift-inspection",
      companyKey: "gs"
    })

    assert.equal(serviceCalls[0].payload.businessDate, "2026-07-03")
    assert.deepEqual(serviceCalls.filter(call => call.type === "flow"), [{ type: "flow", id: "d210" }])
    assert.equal(global.__meetingListPage.data.meetingRows[0].id, "m210")
    assert.equal(global.__inspectionListPage.data.inspectionRows[0].id, "i210")
    assert.equal(serviceCalls.some(call => call.type === "list" && call.moduleKey === "pre-shift-meeting" && call.query.status === "DRAFT" && call.query.dateStart === "2026-07-03" && call.query.dateEnd === "2026-07-03"), true)
    assert.equal(serviceCalls.some(call => call.type === "list" && call.moduleKey === "pre-shift-inspection" && call.query.status === "DRAFT" && call.query.dateStart === "2026-07-03" && call.query.dateEnd === "2026-07-03"), true)
  } finally {
    global.Date = RealDate
  }
})

test("dispatch form loads PC organization tree and cascades company department team options", async () => {
  stubOrganizationTree()
  global.wx = {
    showToast() {},
    navigateBack() {}
  }
  global.Page = config => {
    global.__dispatchFormPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/dispatch/form/index")
  delete require.cache[pagePath]
  require("../pages/dispatch/form/index")

  await global.__dispatchFormPage.onLoad({ companyKey: "gs" })

  assert.equal(global.__dispatchFormPage.data.dispatchDate, todayString())
  assert.equal(global.__dispatchFormPage.data.currentCompany.company, "广晟源成")
  assert.equal(global.__dispatchFormPage.data.currentCompany.workshop, "幕墙组装")
  assert.equal(global.__dispatchFormPage.data.currentTeam, "")
  assert.equal(global.__dispatchFormPage.data.selectedTeamId, "")
  assert.deepEqual(global.__dispatchFormPage.data.companyOptions.map(item => item.label), ["广晟源成"])
  assert.deepEqual(global.__dispatchFormPage.data.departmentOptions.map(item => item.label), ["幕墙组装"])
  assert.deepEqual(global.__dispatchFormPage.data.teamOptions.map(item => item.label), ["幕墙组装1班", "幕墙组装2班"])

  global.__dispatchFormPage.changeDispatchTeam({ detail: { value: "1" } })

  assert.equal(global.__dispatchFormPage.data.selectedTeamId, 1011002)
  assert.equal(global.__dispatchFormPage.data.currentTeam, "幕墙组装2班")
})

test("dispatch form cascades backend organization nodes with orgName type and nodes fields", async () => {
  stubOrganizationTree([
    {
      id: 1,
      orgName: "广晟控股集团",
      type: "GROUP",
      nodes: [
        {
          id: 4,
          orgName: "广晟源成",
          type: "COMPANY",
          nodes: [
            {
              id: 101109,
              orgName: "幕墙组装",
              type: "DEPARTMENT",
              nodes: [
                { id: 1011001, orgName: "幕墙组装1班", type: "TEAM", nodes: [] },
                { id: 1011002, orgName: "幕墙组装2班", type: "TEAM", nodes: [] }
              ]
            },
            {
              id: 101110,
              orgName: "安全环保部",
              type: "DEPARTMENT",
              nodes: [
                { id: 1012001, orgName: "安全一班", type: "TEAM", nodes: [] }
              ]
            }
          ]
        },
        {
          id: 5,
          orgName: "海安加工厂",
          type: "COMPANY",
          nodes: [
            {
              id: 201109,
              orgName: "生产部",
              type: "DEPARTMENT",
              nodes: [
                { id: 2011001, orgName: "生产一班", type: "TEAM", nodes: [] }
              ]
            }
          ]
        }
      ]
    }
  ])
  global.wx = {
    showToast() {},
    navigateBack() {}
  }
  global.Page = config => {
    global.__dispatchFormPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/dispatch/form/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../utils/organization-options")]
  require("../pages/dispatch/form/index")

  await global.__dispatchFormPage.onLoad({ companyKey: "gs" })
  assert.deepEqual(global.__dispatchFormPage.data.companyOptions.map(item => item.label), ["广晟源成", "海安加工厂"])

  global.__dispatchFormPage.changeDispatchCompany({ detail: { value: "1" } })
  assert.equal(global.__dispatchFormPage.data.selectedCompanyId, 5)
  assert.equal(global.__dispatchFormPage.data.currentCompany.company, "海安加工厂")
  assert.deepEqual(global.__dispatchFormPage.data.departmentOptions.map(item => item.label), ["生产部"])
  assert.deepEqual(global.__dispatchFormPage.data.teamOptions.map(item => item.label), ["生产一班"])

  global.__dispatchFormPage.changeDispatchTeam({ detail: { value: "0" } })
  assert.equal(global.__dispatchFormPage.data.selectedTeamId, 2011001)
  assert.equal(global.__dispatchFormPage.data.currentTeam, "生产一班")
})

test("dispatch form wxml keeps full redesign fields without merging record table", () => {
  const wxml = fs.readFileSync(path.join(__dirname, "../pages/dispatch/form/index.wxml"), "utf8")
  const json = JSON.parse(fs.readFileSync(path.join(__dirname, "../pages/dispatch/form/index.json"), "utf8"))

  assert.equal(json.usingComponents["t-icon"], "tdesign-miniprogram/icon/icon")
  for (const label of ["班组派班", "安全第一", "预防为主", "管理人员", "作业人员", "公司", "车间", "班组", "班组任务", "排班类型", "管理人员人数（手录）", "派班日期", "备注", "草稿", "提交"]) {
    assert.match(wxml, new RegExp(label))
  }
  assert.doesNotMatch(wxml, /派班记录表/)
  assert.doesNotMatch(wxml, /dispatch-topbar-subtitle/)
  assert.doesNotMatch(wxml, /当前状态/)
  assert.match(wxml, /dispatch-hero-card/)
  assert.match(wxml, /dispatch-control/)
  assert.match(wxml, /dispatch-char-count/)
  assert.match(wxml, /<t-icon/)
  assert.equal((wxml.match(/dispatch-field-icon/g) || []).length >= 8, true)
  assert.match(wxml, /请选择/)
  assert.match(wxml, /请输入班组任务/)
})

test("dispatch form blocks submit when organization tree has not loaded", async () => {
  const serviceCalls = []
  stubOrganizationFailure()
  require.cache[threeCheckServicePath] = {
    id: threeCheckServicePath,
    filename: threeCheckServicePath,
    loaded: true,
    exports: {
      createRecord(moduleKey, payload) {
        serviceCalls.push({ type: "create", moduleKey, payload })
        return Promise.resolve({ id: "d201" })
      },
      submitRecord() {
        serviceCalls.push({ type: "submit" })
        return Promise.resolve({})
      }
    }
  }
  require.cache[flowServicePath] = {
    id: flowServicePath,
    filename: flowServicePath,
    loaded: true,
    exports: {
      getDispatchFlow() {
        serviceCalls.push({ type: "flow" })
        return Promise.resolve({ children: [] })
      }
    }
  }
  global.wx = {
    showToast() {},
    navigateBack() {}
  }
  global.Page = config => {
    global.__dispatchFormPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/dispatch/form/index")
  delete require.cache[pagePath]
  require("../pages/dispatch/form/index")

  await global.__dispatchFormPage.onLoad({ companyKey: "gs" })
  await global.__dispatchFormPage.submitDispatch()

  assert.deepEqual(serviceCalls, [])
  assert.equal(global.__dispatchFormPage.data.submitting, false)
  assert.equal(global.__dispatchFormPage.data.operationResult, "请先选择公司、车间、班组")
})

test("dispatch form blocks submit when team has not been selected", async () => {
  const serviceCalls = []
  stubOrganizationTree()
  require.cache[threeCheckServicePath] = {
    id: threeCheckServicePath,
    filename: threeCheckServicePath,
    loaded: true,
    exports: {
      createRecord(moduleKey, payload) {
        serviceCalls.push({ type: "create", moduleKey, payload })
        return Promise.resolve({ id: "d202" })
      },
      submitRecord() {
        serviceCalls.push({ type: "submit" })
        return Promise.resolve({})
      }
    }
  }
  global.wx = {
    showToast() {},
    navigateBack() {}
  }
  global.Page = config => {
    global.__dispatchFormPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/dispatch/form/index")
  delete require.cache[pagePath]
  require("../pages/dispatch/form/index")

  await global.__dispatchFormPage.onLoad({ companyKey: "gs" })
  await global.__dispatchFormPage.submitDispatch()

  assert.deepEqual(serviceCalls, [])
  assert.equal(global.__dispatchFormPage.data.operationResult, "请先选择班组")
})

test("dispatch detail page loads one team-dispatch record", async () => {
  const serviceCalls = []
  require.cache[threeCheckServicePath] = {
    id: threeCheckServicePath,
    filename: threeCheckServicePath,
    loaded: true,
    exports: {
      getRecord(moduleKey, id) {
        serviceCalls.push({ moduleKey, id })
        return Promise.resolve({
          id,
          businessDate: "2026-06-16",
          team: "幕墙组装2班",
          owner: "蔡燕艳",
          payload: { workType: "安全员", task: "安全交底与现场巡查" }
        })
      }
    }
  }

  global.Page = config => {
    global.__dispatchDetailPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/dispatch/detail/index")
  delete require.cache[pagePath]
  require("../pages/dispatch/detail/index")

  await global.__dispatchDetailPage.onLoad({ id: "d300", companyKey: "gs" })

  assert.deepEqual(serviceCalls, [{ moduleKey: "team-dispatch", id: "d300" }])
  assert.deepEqual(global.__dispatchDetailPage.data.dispatchRecord, {
    id: "d300",
    date: "2026-06-16",
    department: "-",
    team: "幕墙组装2班",
    name: "蔡燕艳",
    work: "安全员",
    task: "安全交底与现场巡查",
    status: "-",
    statusLabel: "-"
  })
})

test("dispatch detail uses screenshot-style full page layout", () => {
  const wxml = fs.readFileSync(path.join(__dirname, "../pages/dispatch/detail/index.wxml"), "utf8")
  const wxss = fs.readFileSync(path.join(__dirname, "../pages/dispatch/detail/index.wxss"), "utf8")
  const json = JSON.parse(fs.readFileSync(path.join(__dirname, "../pages/dispatch/detail/index.json"), "utf8"))

  assert.equal(json.navigationStyle, "custom")
  assert.equal(json.usingComponents["green-header"], "/components/ui/green-header/index")
  assert.match(wxml, /<green-header title="班组派班" bind:back="goBack" \/>/)
  assert.doesNotMatch(wxml, /detail-home-title|detail-module-bar|detail-module-title/)
  assert.match(wxml, /班组派班/)
  assert.match(wxml, /detail-content-full/)
  assert.match(wxss, /\.dispatch-field\s*\{[^}]*min-height:\s*64rpx/s)
  assert.doesNotMatch(wxml, /<input class="dispatch-value/)
})
