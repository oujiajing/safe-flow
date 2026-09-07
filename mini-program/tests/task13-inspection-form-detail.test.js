const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")

const threeCheckServicePath = require.resolve("../services/mini-three-check")

function reload(modulePath) {
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  return require(modulePath)
}

test("inspection list routes detail to editable inspection detail page", () => {
  const navigateCalls = []
  global.wx = {
    navigateTo(options) {
      navigateCalls.push(options)
    }
  }
  global.Page = config => {
    global.__inspectionListPage = {
      ...config,
      data: { ...config.data, moduleId: "before-check", moduleKey: "pre-shift-inspection", companyKey: "gs" }
    }
  }

  const pagePath = require.resolve("../pages/three-check/inspection-list/index")
  delete require.cache[pagePath]
  require("../pages/three-check/inspection-list/index")

  global.__inspectionListPage.openInspectionDetail({ currentTarget: { dataset: { id: "c100" } } })

  assert.deepEqual(navigateCalls, [
    { url: "/pages/three-check/inspection-detail/index?id=c100&moduleId=before-check&moduleKey=pre-shift-inspection&companyKey=gs" }
  ])
})

test("inspection detail page saves edited check results and remarks", async () => {
  const serviceCalls = []
  require.cache[threeCheckServicePath] = {
    id: threeCheckServicePath,
    filename: threeCheckServicePath,
    loaded: true,
    exports: {
      getRecord(moduleKey, id) {
        serviceCalls.push({ type: "get", moduleKey, id })
        return Promise.resolve({
          id,
          moduleKey,
          recordNo: "BQJC0003",
          companyId: 4,
          departmentId: 101109,
          teamId: 1011002,
          ownerUserId: 7,
          businessDate: "2026-06-17",
          companyName: "广东广晟有色金属光电新材料有限公司",
          departmentName: "废水处理厂",
          teamName: "废水处理2班",
          ownerName: "沈光明",
          status: "DRAFT",
          payload: {
            remarks: "原备注",
            checkItems: [
              { riskType: "机械伤害", checkItem: "检查机械设备", checkResult: "" }
            ]
          },
          version: 2
        })
      },
      updateRecord(moduleKey, id, payload) {
        serviceCalls.push({ type: "update", moduleKey, id, payload })
        return Promise.resolve({ ...payload, id, moduleKey, status: "DRAFT", version: 3 })
      }
    }
  }

  global.wx = {
    showToast() {}
  }
  global.Page = config => {
    global.__inspectionDetailPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/inspection-detail/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../mappers/inspection")]
  require("../pages/three-check/inspection-detail/index")

  await global.__inspectionDetailPage.onLoad({
    id: "c305",
    moduleId: "before-check",
    moduleKey: "pre-shift-inspection",
    companyKey: "gs"
  })
  global.__inspectionDetailPage.chooseCheckResult({
    currentTarget: { dataset: { index: 0, value: "有隐患" } }
  })
  global.__inspectionDetailPage.updateRemark({ detail: { value: "已完成现场确认" } })
  await global.__inspectionDetailPage.saveInspectionDetail()

  assert.equal(serviceCalls[1].type, "update")
  assert.equal(serviceCalls[1].moduleKey, "pre-shift-inspection")
  assert.equal(serviceCalls[1].id, "c305")
  assert.equal(serviceCalls[1].payload.businessDate, "2026-06-17")
  assert.equal(serviceCalls[1].payload.version, 2)
  assert.equal(serviceCalls[1].payload.payload.remarks, "已完成现场确认")
  assert.deepEqual(serviceCalls[1].payload.payload.checkItems, [
    { riskType: "机械伤害", checkItem: "检查机械设备", checkResult: "有隐患", defaultCheckResult: "" }
  ])
})

test("inspection detail page loads one selected module record", async () => {
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
          departmentName: "幕墙车间",
          teamName: "幕墙组装2班",
          ownerName: "沈光明",
          content: "高处作业安全检查",
          status: "OPENED"
        })
      }
    }
  }

  global.Page = config => {
    global.__inspectionDetailPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/inspection-detail/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../mappers/inspection")]
  require("../pages/three-check/inspection-detail/index")

  await global.__inspectionDetailPage.onLoad({
    id: "c300",
    moduleId: "before-check",
    moduleKey: "pre-shift-inspection",
    companyKey: "gs"
  })
  global.__inspectionDetailPage.switchDetailTab({ currentTarget: { dataset: { tab: "detail" } } })

  assert.deepEqual(serviceCalls, [{ moduleKey: "pre-shift-inspection", id: "c300" }])
  assert.equal(global.__inspectionDetailPage.data.activeDetailTab, "detail")
  assert.deepEqual(global.__inspectionDetailPage.data.inspectionRecord, {
    id: "c300",
    code: "c300",
    company: "-",
    dept: "幕墙车间",
    team: "幕墙组装2班",
    owner: "沈光明",
    task: "高处作业安全检查",
    date: "2026-06-16",
    imageCheck: "-",
    imageAttachments: [],
    videoCheck: "-",
    handoverStatus: "-",
    status: "已提交",
    statusClass: "done",
    remark: "-",
    checkItems: [],
    isPostShiftInspection: false,
    rawPayload: {},
    version: 0
  })
})

test("inspection detail uploads image check attachment then reloads record", async () => {
  const serviceCalls = []
  require.cache[threeCheckServicePath] = {
    id: threeCheckServicePath,
    filename: threeCheckServicePath,
    loaded: true,
    exports: {
      getRecord(moduleKey, id) {
        serviceCalls.push({ type: "get", moduleKey, id })
        return Promise.resolve({
          id,
          moduleKey,
          businessDate: "2026-06-16",
          companyName: "广晟有色",
          departmentName: "废水处理厂",
          teamName: "废水处理2班",
          ownerName: "周柏锋",
          imageCheck: serviceCalls.some(call => call.type === "upload") ? "现场照片" : "未上传",
          attachments: serviceCalls
            .filter(call => call.type === "upload")
            .map((call, index) => ({
              id: `att-${index + 1}`,
              fileKind: "IMAGE",
              originalName: "inspection.jpg",
              url: call.filePath
            })),
          payload: {
            checkItems: [
              { riskType: "机械伤害", checkItem: "检查机械设备", checkResult: "无隐患" }
            ]
          },
          status: "DRAFT"
        })
      },
      uploadAttachment(moduleKey, id, fileKind, filePath) {
        serviceCalls.push({ type: "upload", moduleKey, id, fileKind, filePath })
        return Promise.resolve({ id: "att-1", fileKind, originalName: "inspection.jpg", url: filePath })
      }
    }
  }
  global.wx = {
    chooseMedia(options) {
      options.success({ tempFiles: [{ tempFilePath: "/tmp/inspection-image.jpg" }] })
    },
    showToast() {}
  }
  global.Page = config => {
    global.__inspectionDetailPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/inspection-detail/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../mappers/inspection")]
  require("../pages/three-check/inspection-detail/index")

  await global.__inspectionDetailPage.onLoad({
    id: "c330",
    moduleKey: "mid-shift-inspection",
    moduleId: "during-check",
    companyKey: "gs"
  })
  await global.__inspectionDetailPage.uploadInspectionImageCheck()

  assert.deepEqual(serviceCalls.filter(call => call.type === "upload"), [
    { type: "upload", moduleKey: "mid-shift-inspection", id: "c330", fileKind: "IMAGE", filePath: "/tmp/inspection-image.jpg" }
  ])
  assert.equal(serviceCalls.filter(call => call.type === "get").length, 2)
  assert.equal(global.__inspectionDetailPage.data.inspectionRecord.imageCheck, "现场照片")
  assert.equal(global.__inspectionDetailPage.data.operationResult, "图片打卡已上传")
})

test("inspection mapper keeps template check items and hides risk for post-shift records", () => {
  const mapper = reload("../mappers/inspection")

  const before = mapper.mapInspectionRecordToRow({
    id: "c301",
    moduleKey: "pre-shift-inspection",
    recordNo: "BQJC0001",
    company: "广东广晟稀有金属光电新材料有限公司",
    department: "废水处理厂",
    team: "废水处理2班",
    owner: "邹文清",
    businessDate: "2026-06-17",
    imageCheck: "未上传",
    status: "DRAFT",
    payload: {
      remarks: "班前重点检查",
      checkItems: [
        {
          riskType: "机械伤害",
          checkItem: "维护保养设备，检查机械设备是否处于良好状态",
          checkResult: "",
          defaultCheckResult: "无隐患"
        }
      ]
    },
    version: 3
  })

  assert.equal(before.code, "BQJC0001")
  assert.equal(before.company, "广东广晟稀有金属光电新材料有限公司")
  assert.equal(before.imageCheck, "未上传")
  assert.equal(before.remark, "班前重点检查")
  assert.equal(before.isPostShiftInspection, false)
  assert.deepEqual(before.checkItems, [
    {
      riskType: "机械伤害",
      checkItem: "维护保养设备，检查机械设备是否处于良好状态",
      checkResult: "",
      defaultCheckResult: "无隐患",
      expanded: true
    }
  ])

  const post = mapper.mapInspectionRecordToRow({
    id: "c302",
    moduleKey: "post-shift-inspection",
    recordNo: "BHH0001",
    businessDate: "2026-06-17",
    payload: {
      checkItems: [
        {
          riskType: "",
          checkItem: "清点工具材料并确认交班状态",
          checkResult: "无隐患"
        }
      ]
    }
  })

  assert.equal(post.isPostShiftInspection, true)
  assert.equal(post.checkItems[0].riskType, "")
  assert.equal(post.checkItems[0].checkItem, "清点工具材料并确认交班状态")
})

test("inspection detail page keeps editable check result controls on the detail entry", async () => {
  const serviceCalls = []
  require.cache[threeCheckServicePath] = {
    id: threeCheckServicePath,
    filename: threeCheckServicePath,
    loaded: true,
    exports: {
      getRecord(moduleKey, id) {
        serviceCalls.push({ type: "get", moduleKey, id })
        return Promise.resolve({
          id,
          moduleKey,
          recordNo: "BQJC0002",
          companyId: 4,
          departmentId: 5,
          teamId: 6,
          ownerUserId: 7,
          businessDate: "2026-06-17",
          status: "DRAFT",
          payload: {
            remarks: "原备注",
            statusLabel: "待检查",
            checkItems: [
              {
                riskType: "机械伤害",
                checkItem: "维护保养设备，检查机械设备是否处于良好状态",
                checkResult: "",
                defaultCheckResult: "无隐患"
              }
            ]
          },
          version: 2
        })
      },
      updateRecord(moduleKey, id, payload) {
        serviceCalls.push({ type: "update", moduleKey, id, payload })
        return Promise.resolve({ ...payload, id, moduleKey, recordNo: "BQJC0002", status: "DRAFT", version: 3 })
      }
    }
  }

  global.wx = {
    showToast() {}
  }
  global.Page = config => {
    global.__inspectionDetailPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/inspection-detail/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../mappers/inspection")]
  require("../pages/three-check/inspection-detail/index")

  await global.__inspectionDetailPage.onLoad({
    id: "c303",
    moduleId: "before-check",
    moduleKey: "pre-shift-inspection",
    companyKey: "gs"
  })

  assert.equal(global.__inspectionDetailPage.data.inspectionRecord.checkItems[0].expanded, true)
  global.__inspectionDetailPage.toggleDetailItem({ currentTarget: { dataset: { index: 0 } } })
  assert.equal(global.__inspectionDetailPage.data.inspectionRecord.checkItems[0].expanded, false)
  global.__inspectionDetailPage.toggleDetailItem({ currentTarget: { dataset: { index: 0 } } })
  global.__inspectionDetailPage.chooseCheckResult({ currentTarget: { dataset: { index: 0, value: "无隐患" } } })
  assert.equal(global.__inspectionDetailPage.data.inspectionRecord.checkItems[0].expanded, true)
  assert.equal(global.__inspectionDetailPage.data.inspectionRecord.checkItems[0].checkResult, "无隐患")
  assert.equal(typeof global.__inspectionDetailPage.saveInspectionDetail, "function")
  assert.equal(typeof global.__inspectionDetailPage.submitInspectionResults, "undefined")
  assert.deepEqual(serviceCalls, [{ type: "get", moduleKey: "pre-shift-inspection", id: "c303" }])
})

test("inspection detail keeps editable check result controls", () => {
  const detailWxml = fs.readFileSync(path.join(__dirname, "../pages/three-check/inspection-detail/index.wxml"), "utf8")
  const detailWxss = fs.readFileSync(path.join(__dirname, "../pages/three-check/inspection-detail/index.wxss"), "utf8")
  const detailJson = JSON.parse(fs.readFileSync(path.join(__dirname, "../pages/three-check/inspection-detail/index.json"), "utf8"))

  assert.equal(detailJson.navigationStyle, "custom")
  assert.equal(detailJson.navigationBarTextStyle, "white")
  assert.equal(detailJson.navigationBarBackgroundColor, "#078249")
  assert.equal(detailJson.usingComponents["safe-card"], "/components/ui/safe-card/index")
  assert.equal(detailJson.usingComponents["state-view"], "/components/ui/state-view/index")
  assert.equal(detailJson.usingComponents["t-icon"], "tdesign-miniprogram/icon/icon")
  assert.equal(detailJson.usingComponents["green-header"], "/components/ui/green-header/index")
  assert.match(detailWxml, /<green-header title="\{\{moduleTitle\}\}" bind:back="goBack" \/>/)
  assert.match(detailWxss, /page\s*\{[^}]*margin:\s*0;[^}]*padding:\s*0;[^}]*overflow-x:\s*hidden;[^}]*background:\s*#f4f6f8;/s)
  assert.match(detailWxss, /\.page\s*\{[^}]*padding:\s*0;[^}]*max-width:\s*none;[^}]*overflow-x:\s*hidden;[^}]*background:\s*#f4f6f8;/s)
  assert.match(detailWxss, /\.inspection-detail-action-bar\s*\{[\s\S]*width:\s*calc\(100vw \+ 2rpx\);[\s\S]*max-width:\s*none;[\s\S]*margin-left:\s*calc\(50% - 50vw\);[\s\S]*justify-content:\s*space-between;/)
  assert.match(detailWxss, /\.inspection-detail-tabs\s*\{[\s\S]*width:\s*calc\(100vw \+ 2rpx\);[\s\S]*max-width:\s*none;[\s\S]*margin-left:\s*calc\(50% - 50vw\);/)
  assert.match(detailWxss, /\.inspection-detail-page\s*\{[^}]*background:\s*#f4f6f8/s)
  assert.match(detailWxss, /\.inspection-detail-content\s*\{[^}]*background:\s*#f4f6f8/s)
  assert.match(detailWxml, /\{\{moduleTitle\}\}/)
  assert.match(detailWxml, /name="chevron-left"/)
  assert.match(detailWxml, /name="\{\{item\.expanded \? 'chevron-up' : 'chevron-down'\}\}"/)
  assert.match(detailWxml, /name="\{\{checkResultDropdownIndex === index \? 'chevron-up' : 'chevron-down'\}\}"/)
  assert.match(detailWxml, /safe-card/)
  assert.match(detailWxml, /state-view[^>]+loadingText="正在加载检查详情"/)
  assert.match(detailWxml, /detail-content-full/)
  assert.match(detailWxml, /bindtap="switchDetailTab"/)
  assert.match(detailWxml, /data-tab="basic"/)
  assert.match(detailWxml, /data-tab="detail"/)
  assert.match(detailWxml, /wx:if="\{\{activeDetailTab === 'basic'\}\}"/)
  assert.match(detailWxml, /wx:elif="\{\{activeDetailTab === 'detail'\}\}"/)
  assert.match(detailWxml, /inspectionRecord\.isPostShiftInspection \? '' : \(item\.riskType \|\| item\.checkItem \|\| '检查项'\)/)
  assert.match(detailWxml, /bindtap="toggleCheckResultDropdown"/)
  assert.match(detailWxml, /inspection-select-dropdown/)
  assert.match(detailWxml, /bindtap="chooseCheckResult"/)
  assert.match(detailWxml, /saveInspectionDetail/)
  assert.match(detailWxml, /uploadInspectionImageCheck/)
  assert.doesNotMatch(detailWxml, /submitInspectionResults/)
  assert.doesNotMatch(detailWxml, /<button[^>]*(saveInspectionDetail|goBack|uploadInspectionImageCheck)/)
  assert.doesNotMatch(detailWxml, /inspection-detail-arrow">\{\{item\.expanded \? '⌄' : '›'\}\}/)
  assert.doesNotMatch(detailWxml, /inspection-select-arrow">\{\{checkResultDropdownIndex === index \? '⌄' : '›'\}\}/)
  assert.match(detailWxss, /\.inspection-field\s*\{[^}]*min-height:\s*84rpx/s)
  assert.match(detailWxss, /\.inspection-upload-box\s*\{[^}]*border-radius:\s*16rpx/s)
  assert.match(detailWxss, /\.inspection-detail-status\s*\{[^}]*border-radius:\s*999rpx[^}]*background:\s*#fff1e5[^}]*color:\s*#ea580c/s)
  assert.match(detailWxss, /\.inspection-detail-page \.inspection-detail-status\.done,[\s\S]*?background:\s*#078249[\s\S]*?color:\s*#ffffff/s)
  assert.match(detailWxml, /data-value="无隐患"/)
  assert.match(detailWxml, /data-value="有隐患"/)
  assert.doesNotMatch(detailWxml, /9:41|chart-bar|wifi|battery/)
  assert.doesNotMatch(detailWxml, /three-check-statusbar|inspection-detail-native-bar|detail-home-title|detail-module-title/)
})

test("inspection list keeps native status bar space without fake system icons", () => {
  const listWxml = fs.readFileSync(path.join(__dirname, "../pages/three-check/inspection-list/index.wxml"), "utf8")
  const listWxss = fs.readFileSync(path.join(__dirname, "../pages/three-check/inspection-list/index.wxss"), "utf8")
  const listJson = JSON.parse(fs.readFileSync(path.join(__dirname, "../pages/three-check/inspection-list/index.json"), "utf8"))

  assert.equal(listJson.navigationStyle, "custom")
  assert.equal(listJson.navigationBarTextStyle, "white")
  assert.equal(listJson.navigationBarBackgroundColor, "#078249")
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
  assert.match(listWxml, /name="filter"/)
  assert.match(listWxml, /safe-card[^>]*data-id="\{\{item\.id\}\}"[^>]*bindtap="openInspectionDetail"[^>]*class="inspection-reference-card"/)
  assert.match(listWxml, /catchtap="openInspectionDetail"[\s\S]*>明细<\/view>/)
  assert.doesNotMatch(listWxml, /9:41|chart-bar|wifi|battery/)
  assert.doesNotMatch(listWxml, /three-check-statusbar|inspection-native-bar/)
})

test("inspection detail renders uploaded image previews", () => {
  const detailWxml = fs.readFileSync(path.join(__dirname, "../pages/three-check/inspection-detail/index.wxml"), "utf8")
  const detailWxss = fs.readFileSync(path.join(__dirname, "../pages/three-check/inspection-detail/index.wxss"), "utf8")

  assert.match(detailWxml, /wx:for="\{\{inspectionRecord\.imageAttachments\}\}"/)
  assert.match(detailWxml, /<image[^>]+src="\{\{item\.url\}\}"/)
  assert.match(detailWxml, /mode="aspectFill"/)
  assert.match(detailWxss, /\.inspection-media-preview-list/)
  assert.match(detailWxss, /\.inspection-media-thumb/)
})
