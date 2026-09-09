const assert = require("node:assert/strict")
const test = require("node:test")

const servicePath = require.resolve("../services/mini-three-check")
const flowServicePath = require.resolve("../services/flow")

function reload(modulePath) {
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  return require(modulePath)
}

test("dispatch mapper converts team-dispatch records to prototype table rows", () => {
  const mapper = reload("../mappers/dispatch")

  const rows = mapper.mapDispatchRecordsToRows([
    {
      id: "1",
      businessDate: "2026-05-15",
      team: "幕墙组装2班",
      owner: "Demo Admin",
      payload: { workType: "安全员", task: "安全交底与现场巡查" }
    },
    {
      id: "2",
      date: "2026-05-16",
      team: "幕墙机加班组",
      owner: "Demo Leader",
      payload: { jobType: "班长", content: "一班三查任务联动" }
    }
  ])

  assert.deepEqual(rows, [
    { id: "1", date: "2026-05-15", team: "幕墙组装2班", name: "Demo Admin", work: "安全员", task: "安全交底与现场巡查", status: "-", statusLabel: "-", department: "-" },
    { id: "2", date: "2026-05-16", team: "幕墙机加班组", name: "Demo Leader", work: "班长", task: "一班三查任务联动", status: "-", statusLabel: "-", department: "-" }
  ])
})

test("dispatch mapper derives frontend-only record table summary", () => {
  const mapper = reload("../mappers/dispatch")

  const summary = mapper.summarizeDispatchRows([
    { status: "OPENED" },
    { status: "DRAFT" },
    { statusLabel: "已提交" }
  ])

  assert.deepEqual(summary, {
    total: 3,
    submitted: 2,
    draft: 1,
    tasks: 8
  })
})

test("dispatch list page loads team-dispatch records into prototype table rows", async () => {
  const serviceCalls = []
  const RealDate = global.Date
  class FakeDate extends RealDate {
    constructor(...args) {
      if (args.length === 0) {
        super("2026-07-06T08:00:00+08:00")
        return
      }
      super(...args)
    }
    static now() {
      return new RealDate("2026-07-06T08:00:00+08:00").getTime()
    }
  }
  global.Date = FakeDate
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      listRecords(moduleKey, query) {
        serviceCalls.push({ moduleKey, query })
        return Promise.resolve({
          items: [
            {
              id: "100",
              businessDate: "2026-05-15",
              team: "检测中心班",
              owner: "戴军权",
              payload: { workType: "安全管理员", task: "安全交底" }
            }
          ],
          total: 1
        })
      }
    }
  }

  global.wx = {
    showToast() {},
    navigateTo() {}
  }
  global.Page = config => {
    global.__dispatchListPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  try {
    const pagePath = require.resolve("../pages/dispatch/list/index")
    delete require.cache[pagePath]
    delete require.cache[require.resolve("../mappers/dispatch")]
    require("../pages/dispatch/list/index")

    await global.__dispatchListPage.onLoad({ companyKey: "gs" })

    assert.deepEqual(serviceCalls, [
      {
        moduleKey: "team-dispatch",
        query: { dateStart: "2026-07-06", dateEnd: "2026-07-06", page: 1, pageSize: 20 }
      }
    ])
    assert.equal(global.__dispatchListPage.data.dispatchRecordDate, "2026-07-06")
    assert.equal(global.__dispatchListPage.data.dispatchDateStart, "2026-07-06")
    assert.equal(global.__dispatchListPage.data.dispatchDateEnd, "2026-07-06")
    assert.equal(global.__dispatchListPage.data.loading, false)
    assert.equal(global.__dispatchListPage.data.total, 1)
    assert.deepEqual(global.__dispatchListPage.data.dispatchRecordRows, [
      { id: "100", date: "2026-05-15", team: "检测中心班", name: "戴军权", work: "安全管理员", task: "安全交底", status: "-", statusLabel: "待提交", statusClass: "pending", department: "-" }
    ])
    assert.deepEqual(global.__dispatchListPage.data.summary, {
      total: 1,
      submitted: 0,
      draft: 1,
      tasks: 0
    })
  } finally {
    global.Date = RealDate
  }
})

test("dispatch list page opens the new create dispatch page", () => {
  const navigateCalls = []
  global.wx = {
    navigateTo(options) {
      navigateCalls.push(options)
    }
  }
  global.Page = config => {
    global.__dispatchListPage = {
      ...config,
      data: { ...config.data }
    }
  }

  const pagePath = require.resolve("../pages/dispatch/list/index")
  delete require.cache[pagePath]
  require("../pages/dispatch/list/index")

  global.__dispatchListPage.openLegacyCreate()

  assert.deepEqual(navigateCalls, [{ url: "/pages/dispatch/form/index?companyKey=gs" }])
})

test("dispatch list page opens record flow detail page", () => {
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

  global.__dispatchListPage.openDetail({ currentTarget: { dataset: { id: "d900" } } })

  assert.deepEqual(navigateCalls, [{ url: "/pages/dispatch/record-detail/index?id=d900&companyKey=gs" }])
})

test("dispatch record table page keeps filters, table fields, and frontend summary content", () => {
  const fs = require("node:fs")
  const path = require("node:path")
  const wxml = fs.readFileSync(path.join(__dirname, "../pages/dispatch/list/index.wxml"), "utf8")

  for (const label of ["派班记录表", "车间", "班组", "开始日期", "结束日期", "今日派班", "已提交", "待提交", "三查任务", "日期", "人名", "工种", "任务", "筛选", "创建派班", "明细"]) {
    assert.match(wxml, new RegExp(label))
  }
  assert.match(wxml, /class="dispatch-filter-date"[^>]*bindtap="openDispatchFilter"/)
  assert.match(wxml, /safe-card[^>]*data-id="\{\{item\.id\}\}"[^>]*bindtap="openDetail"[^>]*class="dispatch-reference-card"/)
  assert.match(wxml, /catchtap="openDetail"[\s\S]*>明细<\/view>/)
  assert.match(wxml, /catchtap="openDetail"[\s\S]*>查看链路<\/view>/)
})

test("dispatch record detail loads dispatch record and flow stages", async () => {
  const serviceCalls = []
  const records = {
    "team-dispatch:d900": {
      id: "d900",
      recordNo: "TCR-TD-20260703-48EF8F64",
      businessDate: "2026-07-03",
      company: "Demo Works Company",
      department: "门窗机加",
      team: "门窗加工2班",
      owner: "系统管理员",
      status: "OPENED",
      imageCheck: "未上传",
      videoCheck: "未上传",
      payload: {
        teamTask: "门窗加工派班",
        dispatchType: "今日",
        managerCount: "1",
        dispatchDate: "2026-07-03",
        dispatchTime: "2026-07-03 17:02:00"
      },
      attachments: []
    },
    "pre-shift-meeting:m1": {
      id: "m1",
      moduleKey: "pre-shift-meeting",
      recordNo: "TCR-PSM-20260703-001",
      businessDate: "2026-07-03",
      company: "Demo Works Company",
      department: "门窗机加",
      team: "门窗加工2班",
      owner: "班组长",
      status: "OPENED",
      statusLabel: "已开会议",
      imageCheck: "图片已传",
      videoCheck: "视频已传",
      payload: { attendees: "张三、李四" },
      attachments: [
        { id: "a1", fileKind: "IMAGE", url: "https://example.test/a.jpg", originalName: "会议照片.jpg" },
        { id: "a2", fileKind: "VIDEO", url: "https://example.test/a.mp4", originalName: "会议视频.mp4" }
      ]
    },
    "mid-shift-inspection:mid1": {
      id: "mid1",
      moduleKey: "mid-shift-inspection",
      recordNo: "TCR-MSI-20260703-001",
      businessDate: "2026-07-03",
      company: "Demo Works Company",
      department: "门窗机加",
      team: "门窗加工2班",
      owner: "安全员",
      status: "OPENED",
      statusLabel: "已检查",
      imageCheck: "图片已传",
      videoCheck: "视频已传",
      payload: {},
      attachments: []
    },
    "post-shift-inspection:post1": {
      id: "post1",
      moduleKey: "post-shift-inspection",
      recordNo: "TCR-POSTSI-20260703-001",
      businessDate: "2026-07-03",
      company: "Demo Works Company",
      department: "门窗机加",
      team: "门窗加工2班",
      owner: "安全员",
      status: "DRAFT",
      statusLabel: "待检查",
      imageCheck: "未上传",
      payload: { handoverStatus: "未交班" },
      attachments: []
    }
  }
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      getRecord(moduleKey, id) {
        serviceCalls.push({ type: "record", moduleKey, id })
        return Promise.resolve(records[`${moduleKey}:${id}`])
      },
      getRecordWorkflow(moduleKey, id) {
        serviceCalls.push({ type: "workflow", moduleKey, id })
        return Promise.resolve({
          documentFlow: [
            { id: `log-${id}`, actionLabel: "创建记录", operatorName: "系统管理员", occurredAt: "2026-07-03 17:02:00", toStatusLabel: "已提交" }
          ],
          changeHistory: []
        })
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
            { stageKey: "teamDispatch", record: { id, recordNo: "TCR-TD-20260703-48EF8F64", status: "OPENED" } },
            { stageKey: "preShiftMeeting", record: { id: "m1", moduleKey: "pre-shift-meeting", recordNo: "TCR-PSM-20260703-001", status: "OPENED", statusLabel: "已开会议", businessDate: "2026-07-03", company: "Demo Works Company", department: "门窗机加", team: "门窗加工2班", owner: "班组长", imageCheck: "图片已传", videoCheck: "视频已传", payload: { attendees: "张三、李四" } } },
            { stageKey: "preShiftInspection", moduleKey: "pre-shift-inspection", record: null },
            { stageKey: "midShiftInspection", record: { id: "mid1", moduleKey: "mid-shift-inspection", recordNo: "TCR-MSI-20260703-001", status: "OPENED", statusLabel: "已检查", businessDate: "2026-07-03", company: "Demo Works Company", department: "门窗机加", team: "门窗加工2班", owner: "安全员", imageCheck: "图片已传", videoCheck: "视频已传" } },
            { stageKey: "postShiftInspection", record: { id: "post1", moduleKey: "post-shift-inspection", recordNo: "TCR-POSTSI-20260703-001", status: "DRAFT", statusLabel: "待检查", businessDate: "2026-07-03", company: "Demo Works Company", department: "门窗机加", team: "门窗加工2班", owner: "安全员", imageCheck: "未上传", payload: { handoverStatus: "未交班" } } }
          ]
        })
      }
    }
  }
  global.wx = {
    navigateBack() {},
    navigateTo() {},
    showToast() {}
  }
  global.Page = config => {
    global.__dispatchRecordDetailPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/dispatch/record-detail/index")
  delete require.cache[pagePath]
  require("../pages/dispatch/record-detail/index")

  await global.__dispatchRecordDetailPage.onLoad({ id: "d900", companyKey: "gs" })

  assert.deepEqual(serviceCalls.slice(0, 3), [
    { type: "record", moduleKey: "team-dispatch", id: "d900" },
    { type: "flow", id: "d900" },
    { type: "workflow", moduleKey: "team-dispatch", id: "d900" }
  ])
  assert.equal(global.__dispatchRecordDetailPage.data.loading, false)
  assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.title, "一班三查全链路洞察")
  assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.nodes.length, 5)
  assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.statusLabel, "生效")
  assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.selectedStageKey, "teamDispatch")
  assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.detailTitle, "班组派班单据明细")
  assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.detailCells.some(item => item.label === "管理人员人数（手录）" && item.value === "1"), true)

  await global.__dispatchRecordDetailPage.selectFlowNode({ currentTarget: { dataset: { stageKey: "preShiftMeeting" } } })
  assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.selectedStageKey, "preShiftMeeting")
  assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.detailTitle, "班前会单据明细")
  for (const label of ["公司", "部门", "班组", "负责人", "参会人", "开会日期", "图片打卡", "视频打卡"]) {
    assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.detailCells.some(item => item.label === label), true)
  }
  assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.logs[0].title, "创建记录")
  assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.attachments.length, 2)
  assert.equal(serviceCalls.some(call => call.type === "record" && call.moduleKey === "pre-shift-meeting" && call.id === "m1"), true)
  assert.equal(serviceCalls.some(call => call.type === "workflow" && call.moduleKey === "pre-shift-meeting" && call.id === "m1"), true)

  await global.__dispatchRecordDetailPage.selectFlowNode({ currentTarget: { dataset: { stageKey: "midShiftInspection" } } })
  assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.detailCells.some(item => item.label === "视频打卡" && item.value === "视频已传"), true)

  await global.__dispatchRecordDetailPage.selectFlowNode({ currentTarget: { dataset: { stageKey: "postShiftInspection" } } })
  assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.detailCells.some(item => item.label === "交班状态" && item.value === "未交班"), true)

  await global.__dispatchRecordDetailPage.selectFlowNode({ currentTarget: { dataset: { stageKey: "preShiftInspection" } } })
  assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.selectedNode.empty, true)
  for (const label of ["节点", "状态", "公司", "车间", "班组", "业务日期", "关联规则"]) {
    assert.equal(global.__dispatchRecordDetailPage.data.dispatchDetail.detailCells.some(item => item.label === label), true)
  }
})

test("dispatch record detail renders flow insight sections and components", () => {
  const fs = require("node:fs")
  const path = require("node:path")
  const app = JSON.parse(fs.readFileSync(path.join(__dirname, "../app.json"), "utf8"))
  const { registeredPages } = require("./helpers/app-routes")
  const json = JSON.parse(fs.readFileSync(path.join(__dirname, "../pages/dispatch/record-detail/index.json"), "utf8"))
  const wxml = fs.readFileSync(path.join(__dirname, "../pages/dispatch/record-detail/index.wxml"), "utf8")
  const wxss = fs.readFileSync(path.join(__dirname, "../pages/dispatch/record-detail/index.wxss"), "utf8")

  assert.equal(registeredPages(app).includes("pages/dispatch/record-detail/index"), true)
  assert.equal(json.usingComponents["green-header"], "/components/ui/green-header/index")
  assert.equal(json.usingComponents["t-icon"], "tdesign-miniprogram/icon/icon")
  assert.equal(json.usingComponents["t-button"], "tdesign-miniprogram/button/button")
  for (const label of ["链路洞察", "一班三查全链路洞察", "查看原单", "导出", "更多", "链路完成度", "当前节点", "待处理节点", "责任人", "附件数", "链路节点列表", "真实动作日志", "附件（图片或视频）", "暂无附件", "暂无日志", "暂无单据"]) {
    assert.match(wxml, new RegExp(label))
  }
  assert.match(wxml, /data-stage-key="\{\{item\.stageKey\}\}"[\s\S]*bindtap="selectFlowNode"/)
  assert.match(wxml, /\{\{dispatchDetail\.detailTitle\}\}/)
  assert.match(wxml, /wx:for="\{\{dispatchDetail\.attachments\}\}"/)
  assert.match(wxml, /<image[\s\S]*bindtap="previewAttachment"/)
  assert.match(wxml, /<video[\s\S]*controls/)
  assert.doesNotMatch(wxml, /scroll-x="true"[\s\S]*dispatch-step-card/)
  assert.match(wxss, /\.dispatch-step-track\s*\{[\s\S]*?grid-template-columns:\s*repeat\(5,\s*minmax\(0,\s*1fr\)\);/)
})

