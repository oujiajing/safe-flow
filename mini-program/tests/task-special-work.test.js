const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")
const { registeredPages } = require("./helpers/app-routes")

function loadPage(relativePath) {
  let pageConfig = null
  global.Page = config => {
    pageConfig = { ...config, data: { ...config.data }, setData(patch) { this.data = { ...this.data, ...patch } } }
  }
  const resolved = require.resolve(relativePath)
  delete require.cache[resolved]
  const helpers = require(relativePath)
  return { pageConfig, helpers }
}

test("special work is a formal permission-aware module with dedicated pages", () => {
  const modules = require("../config/modules")
  const specialWork = modules.getModuleById("special-work")
  const app = JSON.parse(fs.readFileSync(path.join(__dirname, "../app.json"), "utf8"))

  assert.equal(specialWork.enabled, true)
  assert.equal(specialWork.mockOnly, false)
  assert.equal(specialWork.moduleKey, "special-work")
  assert.equal(specialWork.route, "/pages/special-work/list/index")
  assert.deepEqual(specialWork.permissionCodes, ["PINGAN_SPECIAL_WORK_ENTRY", "PINGAN_SPECIAL_WORK_VIEW"])
  assert.deepEqual(registeredPages(app).filter(page => page.startsWith("pages/special-work/")), [
    "pages/special-work/list/index",
    "pages/special-work/form/index",
    "pages/special-work/detail/index",
    "pages/special-work/workflow/index"
  ])
})

test("special work service calls the shared PC backend endpoints", async () => {
  const requestPath = require.resolve("../utils/request")
  const uploadPath = require.resolve("../utils/upload")
  const calls = []
  require.cache[requestPath] = { id: requestPath, filename: requestPath, loaded: true, exports: { request(options) { calls.push(options); return Promise.resolve(options) } } }
  require.cache[uploadPath] = { id: uploadPath, filename: uploadPath, loaded: true, exports: { uploadFile(options) { calls.push(options); return Promise.resolve(options) } } }
  const servicePath = require.resolve("../services/special-work")
  delete require.cache[servicePath]
  const service = require("../services/special-work")

  await service.listSpecialWorkRecords({ status: "IN_PROGRESS" })
  await service.getSpecialWorkRecord("12")
  await service.createSpecialWorkRecord({ project: "炼钢检修" })
  await service.updateSpecialWorkRecord("12", { status: "PENDING_ACCEPTANCE" })
  await service.executeSpecialWorkAction("12", "SUBMIT_ACCEPTANCE", { implementationEndTime: "2026-07-22 18:00:00" })
  await service.uploadSpecialWorkImage("12", "wxfile://work.jpg")

  assert.deepEqual(calls.map(call => [call.method || "UPLOAD", call.url]), [
    ["GET", "/api/pingan/special-work/records"],
    ["GET", "/api/pingan/special-work/records/12"],
    ["POST", "/api/pingan/special-work/records"],
    ["PUT", "/api/pingan/special-work/records/12"],
    ["POST", "/api/pingan/special-work/records/12/actions/SUBMIT_ACCEPTANCE"],
    ["UPLOAD", "/api/pingan/special-work/records/12/image"]
  ])
})

test("special work list and detail map the four real statuses", () => {
  const { helpers: list } = loadPage("../pages/special-work/list/index")
  const { helpers: detail } = loadPage("../pages/special-work/detail/index")
  const row = list.toRow({ id: 1, status: "PENDING_ACCEPTANCE", applicationTime: "2026-07-22T09:15:00", statusLabel: "待验收" })
  const view = detail.detailView({ status: "IN_PROGRESS", implementationStartTime: "2026-07-22T10:00:00" })

  assert.equal(row.statusClass, "accepting")
  assert.equal(row.applicationTimeLabel, "2026-07-22 09:15:00")
  assert.equal(view.statusClass, "working")
  assert.equal(view.implementationStartTimeLabel, "2026-07-22 10:00:00")
})

test("special work form payload exposes only real PC fields", () => {
  const { helpers } = loadPage("../pages/special-work/form/index")
  const form = helpers.emptyForm()
  const expectedFields = [
    "companyId", "project", "workType", "applicationTime", "workContent", "workLocation",
    "riskIdentificationResult", "implementationStartTime", "implementationEndTime",
    "safetyDisclosurePerson", "guardian", "disclosureReceiver", "completionAcceptor",
    "completionAcceptanceTime", "status"
  ]

  assert.deepEqual(Object.keys(form), expectedFields)
  assert.equal(form.status, "PENDING_APPROVAL")
  assert.equal(form.workType, "动火作业")
  assert.equal(helpers.WORK_TYPES.includes("受限空间作业"), true)
})

test("special work workflow validates stage-specific real fields", () => {
  const { helpers } = loadPage("../pages/special-work/workflow/index")
  const base = { implementationStartTime: "", implementationEndTime: "", safetyDisclosurePerson: "", guardian: "", disclosureReceiver: "", completionAcceptor: "", completionAcceptanceTime: "" }

  assert.equal(helpers.transitionError("PENDING_APPROVAL", base), "请选择作业实施开始时间")
  assert.equal(helpers.transitionError("IN_PROGRESS", base), "请选择作业实施结束时间")
  assert.equal(helpers.transitionError("PENDING_ACCEPTANCE", base), "请填写完工验收人")
  assert.equal(helpers.ACTION_BY_STATUS.PENDING_APPROVAL, "APPROVE_AND_START")
  assert.deepEqual(helpers.ACTION_LABEL_BY_STATUS, {
    PENDING_APPROVAL: "审批",
    IN_PROGRESS: "提交验收",
    PENDING_ACCEPTANCE: "验收"
  })
  assert.deepEqual(helpers.actionPayload("IN_PROGRESS", { implementationEndTime: "2026-07-22 18:00:00" }), { implementationEndTime: "2026-07-22 18:00:00" })
  assert.deepEqual(helpers.flowSteps("PENDING_ACCEPTANCE").map(item => item.state), ["done", "done", "current", "pending"])
})

test("special work PC and mini-program expose the same workflow action text", () => {
  const { helpers } = loadPage("../pages/special-work/workflow/index")
  const pcWorkflow = fs.readFileSync(
    path.join(__dirname, "../../vben/apps/web-antd/src/views/pingan/special-work/special-work-workflow.ts"),
    "utf8"
  )

  for (const [status, action] of Object.entries(helpers.ACTION_BY_STATUS)) {
    const label = helpers.ACTION_LABEL_BY_STATUS[status]
    assert.match(pcWorkflow, new RegExp(`case '${status}'[\\s\\S]*action: '${action}'[\\s\\S]*label: '${label}'`))
  }
})

test("special work pages use shared green header and contain no demo fields or statuses", () => {
  const pages = ["list", "form", "detail", "workflow"]
  const combined = pages.map(page => fs.readFileSync(path.join(__dirname, `../pages/special-work/${page}/index.wxml`), "utf8")).join("\n")

  for (const page of pages) {
    const directory = path.join(__dirname, `../pages/special-work/${page}`)
    const markup = fs.readFileSync(path.join(directory, "index.wxml"), "utf8")
    const config = JSON.parse(fs.readFileSync(path.join(directory, "index.json"), "utf8"))
    assert.match(markup, /<green-header\s+title=/)
    assert.equal(config.navigationStyle, "custom")
    assert.equal(config.usingComponents["green-header"], "/components/ui/green-header/index")
  }
  for (const realField of ["风险辨识结果", "安全交底人", "接受交底人", "完工验收时间"]) assert.match(combined, new RegExp(realField))
  assert.doesNotMatch(combined, /待检查|待确认|作业人|安全措施/)
  const detailMarkup = fs.readFileSync(path.join(__dirname, "../pages/special-work/detail/index.wxml"), "utf8")
  assert.match(detailMarkup, /\{\{actionLabel\}\}/)
  assert.doesNotMatch(detailMarkup, />状态处理<\/button>/)
  assert.doesNotMatch(combined, /批准并开始作业|结束作业并提交验收|确认验收完成/)
})
