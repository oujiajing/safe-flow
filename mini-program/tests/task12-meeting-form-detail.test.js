const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")

const meetingServicePath = require.resolve("../services/mini-three-check")

function cssBlock(css, selector) {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
  const match = css.match(new RegExp(`${escaped}\\s*\\{([^}]*)\\}`))
  assert.ok(match, `Missing CSS block: ${selector}`)
  return match[1]
}

test("meeting list routes detail to editable meeting detail page", () => {
  const navigateCalls = []
  global.wx = {
    navigateTo(options) {
      navigateCalls.push(options)
    }
  }
  global.Page = config => {
    global.__meetingListPage = {
      ...config,
      data: { ...config.data, companyKey: "gs" }
    }
  }

  const pagePath = require.resolve("../pages/three-check/meeting/index")
  delete require.cache[pagePath]
  require("../pages/three-check/meeting/index")

  global.__meetingListPage.openMeetingDetail({ currentTarget: { dataset: { id: "m100" } } })

  assert.deepEqual(navigateCalls, [
    { url: "/pages/three-check/meeting-detail/index?id=m100&companyKey=gs" }
  ])
})

test("meeting detail page saves edited owner attendees remarks and confirmation status", async () => {
  const serviceCalls = []
  require.cache[meetingServicePath] = {
    id: meetingServicePath,
    filename: meetingServicePath,
    loaded: true,
    exports: {
      getRecord(moduleKey, id) {
        serviceCalls.push({ type: "get", moduleKey, id })
        return Promise.resolve({
          id,
          companyId: 4,
          departmentId: 101109,
          teamId: 1011002,
          ownerUserId: 2,
          businessDate: "2026-06-16",
          companyName: "广东广晟有色金属光电新材料有限公司",
          departmentName: "废水处理厂",
          teamName: "废水处理2班",
          ownerName: "郭文清",
          payload: {
            meetingContent: "原备注",
            attendees: ["周柏锋"],
            safetyConfirmItems: [
              { riskType: "机械伤害", safetyItem: "严禁将手伸入设备运转区域", confirmStatus: "" }
            ]
          },
          status: "DRAFT",
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
    global.__meetingDetailPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/meeting-detail/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../mappers/meeting")]
  require("../pages/three-check/meeting-detail/index")

  await global.__meetingDetailPage.onLoad({ id: "m302", companyKey: "gs" })
  global.__meetingDetailPage.changeOwner({ detail: { value: 1 } })
  global.__meetingDetailPage.changeAttendees({ detail: { value: ["周柏锋", "程维新"] } })
  global.__meetingDetailPage.updateMeetingContent({ detail: { value: "班前安全交底已确认" } })
  await global.__meetingDetailPage.chooseConfirmStatus({
    currentTarget: { dataset: { index: 0, status: "已确认" } }
  })
  await global.__meetingDetailPage.saveMeetingDetail()

  assert.equal(serviceCalls[1].type, "update")
  assert.equal(serviceCalls[1].moduleKey, "pre-shift-meeting")
  assert.equal(serviceCalls[1].id, "m302")
  assert.equal(serviceCalls[1].payload.ownerUserId, 3)
  assert.equal(serviceCalls[1].payload.version, 2)
  assert.equal(serviceCalls[1].payload.payload.ownerName, "周柏锋")
  assert.equal(serviceCalls[1].payload.payload.meetingContent, "班前安全交底已确认")
  assert.deepEqual(serviceCalls[1].payload.payload.attendees, ["周柏锋", "程维新"])
  assert.deepEqual(serviceCalls[1].payload.payload.safetyConfirmItems, [
    { riskType: "机械伤害", safetyItem: "严禁将手伸入设备运转区域", confirmStatus: "已确认" }
  ])
})

test("meeting detail page loads one pre-shift meeting", async () => {
  const serviceCalls = []
  require.cache[meetingServicePath] = {
    id: meetingServicePath,
    filename: meetingServicePath,
    loaded: true,
    exports: {
      getRecord(moduleKey, id) {
        serviceCalls.push({ moduleKey, id })
        return Promise.resolve({
          id,
          meetingDate: "2026-06-16",
          companyName: "广东广晟有色金属光电新材料有限公司",
          departmentName: "废水处理厂",
          teamName: "废水处理2班",
          ownerName: "周柏锋",
          payload: {
            meetingContent: "班前安全交底",
            companyName: "广东广晟有色金属光电新材料有限公司",
            departmentName: "废水处理厂",
            teamName: "废水处理2班",
            ownerName: "周柏锋",
            attendees: ["周柏锋", "程维新"],
            safetyConfirmItems: [
              { riskType: "机械伤害", safetyItem: "严禁将手伸入设备运转区域", confirmStatus: "已确认" }
            ]
          },
          status: "OPENED"
        })
      }
    }
  }

  global.Page = config => {
    global.__meetingDetailPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/meeting-detail/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../mappers/meeting")]
  require("../pages/three-check/meeting-detail/index")

  await global.__meetingDetailPage.onLoad({ id: "m300", companyKey: "gs" })
  global.__meetingDetailPage.switchDetailTab({ currentTarget: { dataset: { tab: "detail" } } })

  assert.deepEqual(serviceCalls, [{ moduleKey: "pre-shift-meeting", id: "m300" }])
  assert.equal(global.__meetingDetailPage.data.activeDetailTab, "detail")
  assert.deepEqual(global.__meetingDetailPage.data.meetingRecord, {
    id: "m300",
    company: "广东广晟有色金属光电新材料有限公司",
    dept: "废水处理厂",
    team: "废水处理2班",
    owner: "周柏锋",
    attendees: ["周柏锋", "程维新"],
    attendeesText: "周柏锋、程维新",
    task: "班前安全交底",
    date: "2026-06-16",
    status: "已提交",
    statusClass: "",
    imageCheck: "-",
    videoCheck: "-",
    imageAttachments: [],
    videoAttachments: [],
    safetyConfirmItems: [
      { riskType: "机械伤害", safetyItem: "严禁将手伸入设备运转区域", confirmStatus: "", expanded: true }
    ]
  })
})

test("meeting detail uploads image and video check attachments then reloads record", async () => {
  const serviceCalls = []
  require.cache[meetingServicePath] = {
    id: meetingServicePath,
    filename: meetingServicePath,
    loaded: true,
    exports: {
      getRecord(moduleKey, id) {
        serviceCalls.push({ type: "get", moduleKey, id })
        return Promise.resolve({
          id,
          meetingDate: "2026-06-16",
          companyName: "广晟有色",
          departmentName: "废水处理厂",
          teamName: "废水处理2班",
          ownerName: "周柏锋",
          imageCheck: serviceCalls.some(call => call.type === "upload" && call.fileKind === "IMAGE") ? "现场照片" : "未上传",
          videoCheck: serviceCalls.some(call => call.type === "upload" && call.fileKind === "VIDEO") ? "视频已传" : "未上传",
          attachments: serviceCalls
            .filter(call => call.type === "upload")
            .map((call, index) => ({
              id: `att-${index + 1}`,
              fileKind: call.fileKind,
              originalName: call.fileKind === "IMAGE" ? "meeting.jpg" : "meeting.mp4",
              url: call.filePath
            })),
          payload: {
            meetingContent: "班前安全交底",
            attendees: [],
            safetyConfirmItems: []
          },
          status: "DRAFT"
        })
      },
      uploadAttachment(moduleKey, id, fileKind, filePath) {
        serviceCalls.push({ type: "upload", moduleKey, id, fileKind, filePath })
        return Promise.resolve({ id: `att-${fileKind}`, fileKind, originalName: filePath, url: filePath })
      }
    }
  }
  global.wx = {
    chooseMedia(options) {
      if (options.mediaType.includes("video")) {
        options.success({ tempFiles: [{ tempFilePath: "/tmp/meeting-video.mp4" }] })
      } else {
        options.success({ tempFiles: [{ tempFilePath: "/tmp/meeting-image.jpg" }] })
      }
    },
    showToast() {}
  }
  global.Page = config => {
    global.__meetingDetailPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/meeting-detail/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../mappers/meeting")]
  require("../pages/three-check/meeting-detail/index")

  await global.__meetingDetailPage.onLoad({ id: "m330", companyKey: "gs" })
  await global.__meetingDetailPage.uploadMeetingImageCheck()
  await global.__meetingDetailPage.uploadMeetingVideoCheck()

  assert.deepEqual(serviceCalls.filter(call => call.type === "upload"), [
    { type: "upload", moduleKey: "pre-shift-meeting", id: "m330", fileKind: "IMAGE", filePath: "/tmp/meeting-image.jpg" },
    { type: "upload", moduleKey: "pre-shift-meeting", id: "m330", fileKind: "VIDEO", filePath: "/tmp/meeting-video.mp4" }
  ])
  assert.equal(serviceCalls.filter(call => call.type === "get").length, 3)
  assert.equal(global.__meetingDetailPage.data.meetingRecord.imageCheck, "现场照片")
  assert.equal(global.__meetingDetailPage.data.meetingRecord.videoCheck, "视频已传")
  assert.equal(global.__meetingDetailPage.data.operationResult, "视频打卡已上传")
})

test("meeting detail page shows confirmation status options beside the clicked field", async () => {
  const serviceCalls = []
  require.cache[meetingServicePath] = {
    id: meetingServicePath,
    filename: meetingServicePath,
    loaded: true,
    exports: {
      getRecord() {
        serviceCalls.push({ type: "get" })
        return Promise.resolve({
          id: "m301",
          companyId: 4,
          departmentId: 101109,
          teamId: 1011002,
          ownerUserId: 2,
          meetingDate: "2026-06-16",
          payload: {
            safetyConfirmItems: [
              { riskType: "机械伤害", safetyItem: "严禁将手伸入设备运转区域", confirmStatus: "" }
            ]
          },
          status: "DRAFT",
          version: 1
        })
      },
      updateRecord(moduleKey, id, payload) {
        serviceCalls.push({ type: "update", moduleKey, id, payload })
        return Promise.resolve({ id, version: 2 })
      }
    }
  }

  global.wx = {}
  global.Page = config => {
    global.__meetingDetailPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/meeting-detail/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../mappers/meeting")]
  require("../pages/three-check/meeting-detail/index")

  await global.__meetingDetailPage.onLoad({ id: "m301", companyKey: "gs" })
  global.__meetingDetailPage.toggleConfirmStatusDropdown({
    currentTarget: { dataset: { index: 0 } }
  })
  assert.equal(global.__meetingDetailPage.data.confirmStatusDropdownIndex, 0)

  await global.__meetingDetailPage.chooseConfirmStatus({
    currentTarget: { dataset: { index: 0, status: "已确认" } }
  })

  assert.equal(global.__meetingDetailPage.data.meetingRecord.safetyConfirmItems[0].confirmStatus, "已确认")
  assert.equal(global.__meetingDetailPage.data.confirmStatusDropdownIndex, -1)
})

test("meeting detail auto saves after choosing confirmation status", async () => {
  const serviceCalls = []
  require.cache[meetingServicePath] = {
    id: meetingServicePath,
    filename: meetingServicePath,
    loaded: true,
    exports: {
      getRecord(moduleKey, id) {
        serviceCalls.push({ type: "get", moduleKey, id })
        return Promise.resolve({
          id,
          companyId: 4,
          departmentId: 101109,
          teamId: 1011002,
          ownerUserId: 2,
          businessDate: "2026-06-16",
          companyName: "广东广晟有色金属光电新材料有限公司",
          departmentName: "废水处理厂",
          teamName: "废水处理2班",
          ownerName: "郭文清",
          payload: {
            meetingContent: "班前安全交底",
            attendees: ["周柏锋"],
            safetyConfirmItems: [
              { riskType: "机械伤害", safetyItem: "严禁将手伸入设备运转区域", confirmStatus: "" }
            ]
          },
          status: "DRAFT",
          version: 6
        })
      },
      updateRecord(moduleKey, id, payload) {
        serviceCalls.push({ type: "update", moduleKey, id, payload })
        return Promise.resolve({ id, moduleKey, status: "DRAFT", version: 7 })
      }
    }
  }

  global.wx = {
    showToast() {}
  }
  global.Page = config => {
    global.__meetingDetailPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/meeting-detail/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../mappers/meeting")]
  require("../pages/three-check/meeting-detail/index")

  await global.__meetingDetailPage.onLoad({ id: "m360", companyKey: "gs" })
  await global.__meetingDetailPage.chooseConfirmStatus({
    currentTarget: { dataset: { index: 0, status: "已确认" } }
  })

  assert.equal(global.__meetingDetailPage.data.saving, false)
  assert.equal(global.__meetingDetailPage.data.operationResult, "明细已保存")
  assert.deepEqual(serviceCalls.map(call => call.type), ["get", "update"])
  assert.equal(serviceCalls[1].moduleKey, "pre-shift-meeting")
  assert.equal(serviceCalls[1].id, "m360")
  assert.equal(serviceCalls[1].payload.version, 6)
  assert.deepEqual(serviceCalls[1].payload.payload.safetyConfirmItems, [
    { riskType: "机械伤害", safetyItem: "严禁将手伸入设备运转区域", confirmStatus: "已确认" }
  ])
})

test("meeting detail safety items are expanded by default and can collapse", async () => {
  require.cache[meetingServicePath] = {
    id: meetingServicePath,
    filename: meetingServicePath,
    loaded: true,
    exports: {
      getRecord() {
        return Promise.resolve({
          id: "m331",
          meetingDate: "2026-06-16",
          payload: {
            safetyConfirmItems: [
              { riskType: "机械伤害", safetyItem: "严禁将手伸入设备运转区域", confirmStatus: "" }
            ]
          },
          status: "DRAFT"
        })
      }
    }
  }
  global.wx = {}
  global.Page = config => {
    global.__meetingDetailPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/three-check/meeting-detail/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../mappers/meeting")]
  require("../pages/three-check/meeting-detail/index")

  await global.__meetingDetailPage.onLoad({ id: "m331" })

  assert.equal(global.__meetingDetailPage.data.detailSectionExpanded, false)
  assert.equal(global.__meetingDetailPage.data.meetingRecord.safetyConfirmItems[0].expanded, true)

  global.__meetingDetailPage.toggleDetailSection()
  assert.equal(global.__meetingDetailPage.data.detailSectionExpanded, true)

  global.__meetingDetailPage.toggleSafetyItem({ currentTarget: { dataset: { index: 0 } } })
  assert.equal(global.__meetingDetailPage.data.meetingRecord.safetyConfirmItems[0].expanded, false)

  global.__meetingDetailPage.toggleSafetyItem({ currentTarget: { dataset: { index: 0 } } })
  assert.equal(global.__meetingDetailPage.data.meetingRecord.safetyConfirmItems[0].expanded, true)
})

test("meeting detail renders safety confirmation fields from template snapshot", () => {
  const detailWxml = fs.readFileSync(path.join(__dirname, "../pages/three-check/meeting-detail/index.wxml"), "utf8")

  for (const label of ["风险", "安全注意事项", "确认状态"]) {
    assert.match(detailWxml, new RegExp(label))
  }
  assert.match(detailWxml, /点击选择/)
})

test("meeting detail renders confirm status as an inline dropdown near the field", () => {
  const detailWxml = fs.readFileSync(path.join(__dirname, "../pages/three-check/meeting-detail/index.wxml"), "utf8")
  const detailWxss = fs.readFileSync(path.join(__dirname, "../pages/three-check/meeting-detail/index.wxss"), "utf8")
  const detailJson = JSON.parse(fs.readFileSync(path.join(__dirname, "../pages/three-check/meeting-detail/index.json"), "utf8"))

  assert.equal(detailJson.navigationStyle, "custom")
  assert.equal(detailJson.navigationBarTextStyle, "white")
  assert.equal(detailJson.navigationBarBackgroundColor, "#078249")
  assert.equal(detailJson.usingComponents["t-icon"], "tdesign-miniprogram/icon/icon")
  assert.equal(detailJson.usingComponents["green-header"], "/components/ui/green-header/index")
  assert.match(detailWxml, /<green-header title="班前会" bind:back="goBack" \/>/)
  assert.match(detailWxml, /班前会/)
  assert.doesNotMatch(detailWxml, /meeting-native-capsule/)
  assert.doesNotMatch(detailWxml, /meeting-native-divider/)
  assert.doesNotMatch(detailWxml, /name="more"/)
  assert.doesNotMatch(detailWxml, /name="app"/)
  assert.doesNotMatch(detailWxml, /three-check-statusbar|detail-home-title|detail-module-title/)
  assert.doesNotMatch(detailWxss, /meeting-native-capsule/)
  assert.doesNotMatch(detailWxss, /meeting-native-divider/)
  assert.match(detailWxml, /detail-content-full/)
  assert.match(detailWxml, /bindtap="switchDetailTab"/)
  assert.match(detailWxml, /data-tab="basic"/)
  assert.match(detailWxml, /data-tab="detail"/)
  assert.match(detailWxml, /wx:if="\{\{activeDetailTab === 'basic'\}\}"/)
  assert.match(detailWxml, /bindtap="toggleConfirmStatusDropdown"/)
  assert.match(detailWxml, /meeting-select-dropdown/)
  assert.match(detailWxml, /bindtap="chooseConfirmStatus"/)
  assert.match(detailWxml, /saveMeetingDetail/)
  assert.match(detailWxml, /meetingRecord\.status === '已提交' \? 'done' : ''/)
  assert.match(detailWxss, /\.meeting-detail-status\s*\{[^}]*border-radius:\s*999rpx[^}]*background:\s*#fff1e5[^}]*color:\s*#ea580c/s)
  assert.match(detailWxss, /\.meeting-detail-page \.meeting-detail-status\.done,[\s\S]*?background:\s*#078249 !important;[\s\S]*?color:\s*#ffffff !important;/)
  assert.match(detailWxml, /checkbox-group/)
  assert.match(detailWxss, /\.meeting-row\s*\{[^}]*min-height:\s*64rpx/s)
  assert.doesNotMatch(detailWxml, /9:41|chart-bar|wifi|battery/)
  assert.doesNotMatch(detailWxml, /meeting-confirm-choice active/)
})

test("meeting detail renders uploaded image and video previews", () => {
  const detailWxml = fs.readFileSync(path.join(__dirname, "../pages/three-check/meeting-detail/index.wxml"), "utf8")
  const detailWxss = fs.readFileSync(path.join(__dirname, "../pages/three-check/meeting-detail/index.wxss"), "utf8")

  assert.match(detailWxml, /wx:for="\{\{meetingRecord\.imageAttachments\}\}"/)
  assert.match(detailWxml, /<image[^>]+src="\{\{item\.url\}\}"/)
  assert.match(detailWxml, /mode="aspectFill"/)
  assert.match(detailWxml, /wx:for="\{\{meetingRecord\.videoAttachments\}\}"/)
  assert.match(detailWxml, /<video[^>]+src="\{\{item\.url\}\}"/)
  assert.match(detailWxss, /\.meeting-media-preview-list/)
  assert.match(detailWxss, /\.meeting-media-thumb/)
  assert.match(detailWxss, /\.meeting-media-video/)
})

test("meeting detail renders redesigned basic and detail reference sections", () => {
  const detailWxml = fs.readFileSync(path.join(__dirname, "../pages/three-check/meeting-detail/index.wxml"), "utf8")
  const detailWxss = fs.readFileSync(path.join(__dirname, "../pages/three-check/meeting-detail/index.wxss"), "utf8")
  const detailJson = JSON.parse(fs.readFileSync(path.join(__dirname, "../pages/three-check/meeting-detail/index.json"), "utf8"))

  assert.equal(detailJson.usingComponents["green-header"], "/components/ui/green-header/index")
  assert.match(detailWxml, /<green-header title="班前会" bind:back="goBack" \/>/)
  assert.match(detailWxml, /meeting-detail-action-bar/)
  assert.match(detailWxml, /<t-icon[^>]+name="chevron-left"/)
  assert.match(detailWxml, /<view class="meeting-back-button meeting-action-control" bindtap="goBack">/)
  assert.match(detailWxml, /<view wx:if="\{\{canUpdateAction\}\}" class="meeting-save-button meeting-action-control detail-save-btn/)
  assert.match(detailWxml, /bindtap="saveMeetingDetail"/)
  assert.doesNotMatch(detailWxml, /<button[^>]+class="meeting-(back-button|save-button|upload-box|upload-action)/)
  assert.match(detailWxml, /meeting-detail-tabs/)
  assert.match(detailWxml, /meeting-basic-card/)
  assert.doesNotMatch(detailWxml, /meeting-media-card/)
  assert.doesNotMatch(detailWxml, /meeting-remark-card/)
  assert.match(detailWxml, /<t-icon[^>]+name="image"/)
  assert.match(detailWxml, /<t-icon[^>]+name="camera"/)
  assert.match(detailWxml, /<t-icon[^>]+name="video"/)
  assert.match(detailWxml, /<t-icon[^>]+name="video-camera"/)
  assert.match(detailWxml, /meeting-upload-column/)
  assert.match(detailWxml, /<view class="meeting-upload-box meeting-action-control/)
  assert.match(detailWxml, /<view class="meeting-upload-action meeting-action-control/)
  assert.match(detailWxml, /最多上传9张/)
  assert.match(detailWxml, /最多上传1个/)
  assert.match(detailWxml, /meeting-safety-card/)
  assert.match(detailWxml, /meeting-safety-status-chip/)
  assert.match(detailWxml, /meeting-safety-status-field/)
  assert.match(detailWxml, /<view wx:if="\{\{item\.expanded\}\}" class="meeting-safety-body">/)
  assert.match(detailWxml, /<t-icon[^>]+t-class="meeting-safety-chevron"[^>]+name="\{\{item\.expanded \? 'chevron-up' : 'chevron-down'\}\}"/)
  assert.match(detailWxml, /<t-icon[^>]+t-class="meeting-select-arrow"[^>]+name="\{\{confirmStatusDropdownIndex === index \? 'chevron-up' : 'chevron-down'\}\}"/)
  assert.doesNotMatch(detailWxml, /class="meeting-safety-chevron">[⌃⌄›‹^v]/)
  assert.doesNotMatch(detailWxml, /class="meeting-select-arrow">/)

  for (const label of ["公司", "车间", "班组", "负责人", "参会人", "开会日期", "图片打卡", "视频打卡", "状态", "备注"]) {
    assert.match(detailWxml, new RegExp(label))
  }

  for (const label of ["风险", "安全注意事项", "确认状态"]) {
    assert.match(detailWxml, new RegExp(label))
  }

  const pageCss = cssBlock(detailWxss, ".page")

  assert.match(detailWxss, /\.meeting-basic-card/)
  assert.match(pageCss, /padding:\s*0;/)
  assert.match(pageCss, /max-width:\s*none;/)
  assert.match(pageCss, /overflow-x:\s*hidden;/)
  assert.match(detailWxss, /page\s*\{[^}]*margin:\s*0;[^}]*padding:\s*0;[^}]*overflow-x:\s*hidden;[^}]*background:\s*#f4f6f8;/s)
  assert.match(detailWxss, /\.meeting-detail-action-bar\s*\{[\s\S]*width:\s*calc\(100vw \+ 2rpx\);[\s\S]*max-width:\s*none;[\s\S]*margin-left:\s*calc\(50% - 50vw\);[\s\S]*justify-content:\s*space-between;/)
  assert.match(detailWxss, /\.meeting-back-button\s*\{[\s\S]*justify-content:\s*flex-start;/)
  assert.match(detailWxss, /\.meeting-detail-content\s*\{[\s\S]*padding:\s*0 0 40rpx;/)
  assert.match(detailWxss, /\.meeting-detail-tabs\s*\{[\s\S]*width:\s*calc\(100vw \+ 2rpx\);[\s\S]*max-width:\s*none;[\s\S]*margin-left:\s*calc\(50% - 50vw\);/)
  assert.match(detailWxss, /\.meeting-detail-tabs\s*\{[\s\S]*margin:\s*0 0 24rpx;/)
  assert.match(detailWxss, /\.meeting-detail-tabs\s*\{[\s\S]*padding:\s*0;/)
  assert.match(detailWxss, /\.meeting-basic-panel,\s*[\r\n]+\.meeting-detail-panel\s*\{[\s\S]*padding:\s*0 28rpx;/)
  assert.match(detailWxss, /\.meeting-label\s*\{[\s\S]*color:\s*#0f172a;/)
  assert.match(detailWxss, /\.meeting-label\s*\{[\s\S]*font-weight:\s*700;/)
  assert.match(detailWxss, /\.meeting-save-button\s*\{[\s\S]*width:\s*112rpx;/)
  assert.match(detailWxss, /\.meeting-save-button\s*\{[\s\S]*min-width:\s*112rpx;/)
  assert.match(detailWxss, /\.meeting-save-button\s*\{[\s\S]*max-width:\s*112rpx;/)
  assert.match(detailWxss, /\.meeting-save-button\s*\{[\s\S]*height:\s*56rpx;/)
  assert.match(detailWxss, /\.meeting-save-button\s*\{[\s\S]*display:\s*flex;/)
  assert.match(detailWxss, /\.meeting-save-button\s*\{[\s\S]*margin-left:\s*auto;/)
  assert.match(detailWxss, /\.meeting-save-button\s*\{[\s\S]*flex:\s*0 0 112rpx;/)
  assert.match(detailWxss, /\.meeting-action-control\.is-loading\s*\{[\s\S]*opacity:\s*0\.72;/)
  assert.match(detailWxss, /\.meeting-upload-grid\s*\{[\s\S]*grid-template-columns:\s*150rpx 206rpx;/)
  assert.match(detailWxss, /\.meeting-upload-box\s*\{[\s\S]*color:\s*#8b94a3;/)
  assert.match(detailWxss, /\.meeting-upload-box\s*\{[\s\S]*border-radius:\s*16rpx;/)
  assert.match(detailWxss, /\.meeting-upload-action\s*\{[\s\S]*width:\s*206rpx;/)
  assert.match(detailWxss, /\.meeting-upload-action\s*\{[\s\S]*height:\s*68rpx;/)
  assert.match(detailWxss, /\.meeting-upload-action\s*\{[\s\S]*border-radius:\s*16rpx;/)
  assert.match(detailWxss, /\.meeting-upload-limit\s*\{[\s\S]*color:\s*#8b94a3;/)
  assert.match(detailWxss, /\.meeting-safety-head\s*\{[\s\S]*grid-template-columns:\s*54rpx minmax\(0,\s*1fr\) 48rpx;/)
  assert.match(detailWxss, /\.meeting-safety-head\s*\{[\s\S]*align-items:\s*center;/)
  assert.match(detailWxss, /\.meeting-safety-chevron\s*\{[\s\S]*width:\s*48rpx;/)
  assert.match(detailWxss, /\.meeting-safety-chevron\s*\{[\s\S]*height:\s*48rpx;/)
  assert.match(detailWxss, /\.meeting-safety-chevron\s*\{[\s\S]*display:\s*flex;/)
  assert.match(detailWxss, /\.meeting-safety-chevron\s*\{[\s\S]*align-items:\s*center;/)
  assert.match(detailWxss, /\.meeting-safety-chevron\s*\{[\s\S]*justify-content:\s*center;/)
  assert.match(detailWxss, /\.meeting-safety-status-field\s*\{[\s\S]*min-height:\s*72rpx;/)
  assert.match(detailWxss, /\.meeting-safety-status-field\s*\{[\s\S]*padding:\s*10rpx 0;/)
  assert.match(detailWxss, /\.meeting-select-value\s*\{[\s\S]*height:\s*56rpx;/)
  assert.match(detailWxss, /\.meeting-select-value\s*\{[\s\S]*border-radius:\s*12rpx;/)
  assert.match(detailWxss, /\.meeting-select-arrow\s*\{[\s\S]*width:\s*32rpx;/)
  assert.match(detailWxss, /\.meeting-select-arrow\s*\{[\s\S]*height:\s*32rpx;/)
  assert.match(detailWxss, /\.meeting-select-arrow\s*\{[\s\S]*display:\s*flex;/)
  assert.match(detailWxss, /\.meeting-select-arrow\s*\{[\s\S]*align-items:\s*center;/)
  assert.match(detailWxss, /\.meeting-select-arrow\s*\{[\s\S]*justify-content:\s*center;/)
  assert.match(detailWxml, /<view class="meeting-label required-placeholder-label">备注<\/view>/)
  assert.match(detailWxss, /\.required-placeholder-label::before\s*\{[\s\S]*content:\s*"\*";/)
  assert.match(detailWxss, /\.required-placeholder-label::before\s*\{[\s\S]*color:\s*transparent;/)
  assert.match(detailWxss, /\.meeting-safety-card/)
  assert.match(detailWxss, /page\s*\{[^}]*margin:\s*0;[^}]*padding:\s*0;[^}]*background:\s*#f4f6f8;/s)
  assert.match(detailWxss, /\.meeting-detail-page\s*\{[^}]*background:\s*#f4f6f8/s)
  assert.match(detailWxss, /\.meeting-detail-content\s*\{[^}]*background:\s*#f4f6f8/s)
  assert.doesNotMatch(detailWxml, /three-check-statusbar|meeting-detail-native-bar/)
})
