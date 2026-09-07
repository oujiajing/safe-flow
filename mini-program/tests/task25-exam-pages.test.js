const assert = require("node:assert/strict")
const test = require("node:test")
const fs = require("node:fs")
const path = require("node:path")

const examServiceModulePath = require.resolve("../services/safety-exam")
const organizationServiceModulePath = require.resolve("../services/organization")
const userServiceModulePath = require.resolve("../services/user")
const requestModulePath = require.resolve("../utils/request")

function readMini(file) {
  return fs.readFileSync(require.resolve(`../${file}`), "utf8")
}

function reload(modulePath) {
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  return require(modulePath)
}

function installExamServiceStub(service) {
  require.cache[examServiceModulePath] = {
    id: examServiceModulePath,
    filename: examServiceModulePath,
    loaded: true,
    exports: service
  }
}

function installOrganizationServiceStub(nodes = []) {
  require.cache[organizationServiceModulePath] = {
    id: organizationServiceModulePath,
    filename: organizationServiceModulePath,
    loaded: true,
    exports: {
      getOrgTree() {
        return Promise.resolve(nodes)
      }
    }
  }
}

function installUserServiceStub(users = []) {
  require.cache[userServiceModulePath] = {
    id: userServiceModulePath,
    filename: userServiceModulePath,
    loaded: true,
    exports: {
      getCurrentUser() {
        return Promise.resolve({})
      },
      listPinganUsers() {
        return Promise.resolve(users)
      }
    }
  }
}

test("safety exam module is promoted to formal mini-program pages", () => {
  const app = JSON.parse(readMini("app.json"))
  const { registeredPages } = require("./helpers/app-routes")
  const pages = registeredPages(app)
  const modules = reload("../config/modules")
  const permissions = reload("../config/permissions")
  const exam = modules.getModuleById("exam")

  assert.ok(pages.includes("pages/training/exam-list/index"))
  assert.ok(pages.includes("pages/training/exam-basic/index"))
  assert.ok(pages.includes("pages/training/exam-detail/index"))
  assert.ok(pages.includes("pages/training/exam-create-basic/index"))
  assert.equal(pages.includes("pages/training/exam-create-detail/index"), false)
  assert.equal(fs.existsSync(path.join(__dirname, "../pages/training/exam-create-detail")), false)
  assert.ok(pages.includes("pages/training/exam-taking/index"))
  assert.equal(exam.enabled, true)
  assert.equal(exam.mockOnly, false)
  assert.equal(exam.route, "/pages/training/exam-list/index")
  assert.deepEqual(exam.permissionCodes, [
    "PINGAN_TRAINING_EXAM_TASKS_ENTRY",
    "PINGAN_TRAINING_EXAM_TASKS_VIEW"
  ])
  assert.deepEqual(permissions.MODULE_PERMISSION_CODES.exam, [
    "PINGAN_TRAINING_EXAM_TASKS_ENTRY",
    "PINGAN_TRAINING_EXAM_TASKS_VIEW"
  ])
})

test("safety exam pages use shared green-header and TDesign icons", () => {
  for (const page of [
    "pages/training/exam-list/index",
    "pages/training/exam-basic/index",
    "pages/training/exam-detail/index",
    "pages/training/exam-create-basic/index"
  ]) {
    const json = readMini(`${page}.json`)
    const wxml = readMini(`${page}.wxml`)

    assert.match(json, /"navigationStyle":\s*"custom"/, page)
    assert.match(json, /"green-header":\s*"\/components\/ui\/green-header\/index"/, page)
    assert.match(json, /"t-icon":\s*"tdesign-miniprogram\/icon\/icon"/, page)
    assert.doesNotMatch(json, /"t-button":\s*"tdesign-miniprogram\/button\/button"/, page)
    assert.match(wxml, /<green-header title="安全考试" bind:back="goBack" \/>/, page)
  }
})

test("safety exam taking page uses a custom white exam header", () => {
  const json = readMini("pages/training/exam-taking/index.json")
  const wxml = readMini("pages/training/exam-taking/index.wxml")
  const wxss = readMini("pages/training/exam-taking/index.wxss")
  const topbarStyle = wxss.match(/\.exam-taking-topbar\s*\{[\s\S]*?\}/)?.[0] || ""
  const markStyle = wxss.match(/\.exam-taking-mark\s*\{[\s\S]*?\}/)?.[0] || ""
  const markUnderlineStyle = wxss.match(/\.exam-taking-mark::after\s*\{[\s\S]*?\}/)?.[0] || ""
  const countStyle = wxss.match(/\.exam-taking-count\s*\{[\s\S]*?\}/)?.[0] || ""

  assert.match(json, /"navigationStyle":\s*"custom"/)
  assert.match(json, /"navigationBarTextStyle":\s*"black"/)
  assert.match(json, /"navigationBarBackgroundColor":\s*"#ffffff"/)
  assert.doesNotMatch(json, /"green-header":/)
  assert.match(json, /"t-icon":\s*"tdesign-miniprogram\/icon\/icon"/)
  assert.match(wxml, /class="exam-taking-page" style="padding-top: \{\{statusBarHeight\}\}px;"/)
  assert.match(wxml, /<view class="exam-taking-topbar" style="padding-right: \{\{capsuleSafeRight\}\}px;">\s*<view class="exam-taking-back" bindtap="goBack">\s*<t-icon name="chevron-left" size="48rpx" color="#2d3035" \/>\s*<\/view>\s*<view class="exam-taking-timer">\s*<view class="exam-taking-timer-gauge"><\/view>\s*<text>\{\{remainingText\}\}<\/text>\s*<\/view>\s*<view class="exam-taking-spacer"><\/view>\s*<view class="exam-taking-mark \{\{annotationMode \? 'active' : ''\}\} \{\{hasCurrentAnnotation \? 'annotated' : ''\}\}" bindtap="toggleAnnotation">\s*<t-icon name="edit-1" size="46rpx" color="#2d3035" \/>\s*<\/view>\s*<view class="exam-taking-answer-card" bindtap="openAnswerCard">\s*<t-icon name="file-paste" size="46rpx" color="#2d3035" \/>\s*<\/view>\s*<view class="exam-taking-count">\{\{currentQuestionIndex \+ 1\}\}\/\{\{questionTotal\}\}<\/view>\s*<\/view>\s*<view class="exam-taking-progress">/)
  assert.match(wxml, /class="exam-taking-progress-fill" style="width: \{\{progressPercent\}\}%"/)
  assert.doesNotMatch(wxml, /23:36|4G|wifi|battery|status-icon/)
  assert.match(topbarStyle, /height:\s*96rpx/)
  assert.match(topbarStyle, /grid-template-columns:\s*76rpx 90rpx minmax\(0,\s*1fr\) 68rpx 68rpx minmax\(84rpx,\s*auto\)/)
  assert.match(topbarStyle, /padding:\s*0 34rpx/)
  assert.match(wxss, /\.exam-taking-timer\s*\{[\s\S]*font-size:\s*27rpx/)
  assert.match(markStyle, /z-index:\s*0/)
  assert.match(markUnderlineStyle, /opacity:\s*0/)
  assert.match(markUnderlineStyle, /z-index:\s*-1/)
  assert.match(wxss, /\.exam-taking-mark\.active::after,[\s\S]*\.exam-taking-mark\.annotated::after\s*\{[\s\S]*opacity:\s*1/)
  assert.match(countStyle, /white-space:\s*nowrap/)
  assert.match(wxml, /wx:if="\{\{exitConfirmVisible\}\}"[\s\S]*<view class="exam-exit-title">确定要退出考试吗？<\/view>/)
  assert.match(wxml, /<view class="exam-exit-description">退出会自动保存进度<\/view>/)
  assert.match(wxml, /<view class="exam-exit-action confirm" bindtap="confirmExitExam">确定退出<\/view>\s*<view class="exam-exit-action continue" bindtap="cancelExitExam">继续答题<\/view>/)
  assert.match(wxss, /\.exam-exit-title\s*\{[\s\S]*font-size:\s*40rpx[\s\S]*color:\s*#20242a/)
  assert.match(wxss, /\.exam-exit-action\.confirm\s*\{[\s\S]*background:\s*transparent[\s\S]*color:\s*#8b8f94/)
  assert.match(wxss, /\.exam-exit-action\.continue\s*\{[\s\S]*background:\s*#fff0e3[\s\S]*color:\s*#d66700/)
})

test("safety exam list matches the reference toolbar, status tabs, and card fields", () => {
  const wxml = readMini("pages/training/exam-list/index.wxml")
  const wxss = readMini("pages/training/exam-list/index.wxss")
  const examData = reload("../pages/training/exam-data")
  const record = examData.getExamRecords()[0]
  const searchRowStyle = wxss.match(/\.exam-search-row\s*\{[\s\S]*?\}/)?.[0] || ""
  const searchInputStyle = wxss.match(/\.exam-search-input\s*\{[\s\S]*?\}/)?.[0] || ""
  const searchSubmitStyle = wxss.match(/\.exam-search-submit\s*\{[\s\S]*?\}/)?.[0] || ""
  const toolbarStyle = wxss.match(/\.exam-toolbar\s*\{[\s\S]*?\}/)?.[0] || ""
  const createButtonStyle = wxss.match(/\.exam-create-btn\s*\{[\s\S]*?\}/)?.[0] || ""
  const filterButtonStyle = wxss.match(/\.exam-filter-btn\s*\{[\s\S]*?\}/)?.[0] || ""
  const detailButtonStyle = wxss.match(/\.exam-detail-btn\s*\{[\s\S]*?\}/)?.[0] || ""
  const startButtonStyle = wxss.match(/\.exam-start-btn\s*\{[\s\S]*?\}/)?.[0] || ""

  assert.match(wxml, /placeholder="使用编码、考试人员、考试日期 搜索"/)
  assert.match(wxml, /<t-icon[\s\S]*name="search" size="36rpx"/)
  assert.match(wxml, /<view class="exam-search-submit" bindtap="search">搜索<\/view>/)
  assert.doesNotMatch(wxml, /<t-button[\s\S]*exam-create-btn/)
  assert.doesNotMatch(wxml, /<t-button[\s\S]*exam-filter-btn/)
  assert.doesNotMatch(wxml, /<t-button[\s\S]*exam-detail-btn/)
  assert.match(wxml, /<view class="exam-create-btn" bindtap="openCreate">[\s\S]*<t-icon name="add-circle" size="32rpx"[\s\S]*<text class="exam-create-label">创建<\/text>[\s\S]*<\/view>/)
  assert.match(wxml, /<view class="exam-filter-btn" bindtap="openFilter">[\s\S]*<t-icon name="filter" size="38rpx"[\s\S]*<text>筛选<\/text>[\s\S]*<\/view>/)
  assert.match(wxml, /wx:if="\{\{filterVisible\}\}"/)
  assert.match(wxml, /mode="date" value="\{\{filterStartDate\}\}" bindchange="changeFilterStartDate"/)
  assert.match(wxml, /mode="date" value="\{\{filterEndDate\}\}" bindchange="changeFilterEndDate"/)
  assert.match(wxml, /range="\{\{departmentOptions\}\}" range-key="label" value="\{\{departmentIndex\}\}" bindchange="changeDepartmentFilter"/)
  assert.match(wxml, /range="\{\{teamOptions\}\}" range-key="label" value="\{\{teamIndex\}\}" bindchange="changeTeamFilter"/)
  assert.match(wxml, /bindtap="applyFilter"/)
  assert.match(wxml, /bindtap="resetFilter"/)
  assert.match(wxml, /待考试（\{\{statusCounts\.pending\}\}）/)
  assert.match(wxml, /已考试（\{\{statusCounts\.done\}\}）/)
  assert.match(wxml, /class="exam-card-status \{\{item\.statusKey\}\}">\{\{item\.statusLabel\}\}<\/view>/)
  assert.match(wxml, /exam-status-tabs/)
  assert.match(wxml, /exam-card-company/)
  assert.match(wxml, /exam-card-meta/)
  assert.match(wxml, /exam-person-pill/)
  assert.equal(record.company, "广东广晟有色金属光电新材料有限公司")
  assert.equal(record.exam, "2026年防灾减灾知识考试")
  assert.match(wxml, /明细/)
  assert.match(wxml, /<view wx:for="\{\{records\}\}" wx:key="id" class="exam-card" data-id="\{\{item\.id\}\}" bindtap="openBasic">/)
  assert.match(wxml, /catchtap="startExam">\{\{item\.hasProgress \? '继续考试' : '去考试'\}\}<\/view>/)

  assert.match(searchRowStyle, /height:\s*62rpx/)
  assert.match(searchRowStyle, /margin:\s*20rpx 30rpx 14rpx/)
  assert.match(searchRowStyle, /grid-template-columns:\s*46rpx minmax\(0,\s*1fr\) 82rpx/)
  assert.match(searchRowStyle, /padding:\s*0 20rpx/)
  assert.match(searchRowStyle, /border-radius:\s*999rpx/)
  assert.match(searchInputStyle, /height:\s*60rpx/)
  assert.match(searchInputStyle, /font-size:\s*28rpx/)
  assert.match(searchInputStyle, /line-height:\s*60rpx/)
  assert.match(searchSubmitStyle, /height:\s*52rpx/)
  assert.match(searchSubmitStyle, /display:\s*flex/)
  assert.match(searchSubmitStyle, /justify-content:\s*center/)
  assert.match(searchSubmitStyle, /border:\s*1rpx solid #078249/)
  assert.match(searchSubmitStyle, /border-radius:\s*999rpx/)
  assert.match(searchSubmitStyle, /background:\s*#eef8f2/)
  assert.match(searchSubmitStyle, /color:\s*#078249/)
  assert.match(toolbarStyle, /height:\s*68rpx/)
  assert.match(toolbarStyle, /padding:\s*0 30rpx 6rpx/)
  assert.match(createButtonStyle, /min-width:\s*148rpx/)
  assert.match(createButtonStyle, /height:\s*50rpx/)
  assert.match(createButtonStyle, /align-self:\s*center/)
  assert.match(createButtonStyle, /padding:\s*0 22rpx/)
  assert.match(createButtonStyle, /font-size:\s*27rpx/)
  assert.match(wxss, /\.exam-create-label\s*\{[\s\S]*white-space:\s*nowrap/)
  assert.match(filterButtonStyle, /min-width:\s*116rpx/)
  assert.match(filterButtonStyle, /height:\s*50rpx/)
  assert.match(filterButtonStyle, /align-self:\s*center/)
  assert.match(filterButtonStyle, /justify-content:\s*center/)
  assert.match(filterButtonStyle, /font-size:\s*29rpx/)
  assert.match(wxss, /\.exam-status-tabs\s*\{[\s\S]*?width:\s*100%[\s\S]*?height:\s*86rpx[\s\S]*?margin:\s*0;[\s\S]*?border-radius:\s*16rpx[\s\S]*?background:\s*#ffffff/s)
  assert.match(wxss, /\.exam-status-tab-list\s*\{[\s\S]*?grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/s)
  assert.match(wxss, /\.exam-status-tab-list\s*\{[\s\S]*?align-items:\s*center/s)
  assert.match(wxss, /\.exam-status-tab-list\s*\{[\s\S]*?padding:\s*0/s)
  assert.match(wxss, /\.exam-status-tab\s*\{[\s\S]*?text-align:\s*center/s)
  assert.match(wxss, /\.exam-status-tab\.active::after\s*\{[\s\S]*?background:\s*#078249/s)
  assert.match(wxss, /\.exam-card\s*\{[\s\S]*?border-radius:\s*18rpx[\s\S]*?box-shadow:/s)
  assert.match(wxss, /\.exam-card-list\s*\{[\s\S]*?gap:\s*16rpx[\s\S]*?padding:\s*14rpx 30rpx 0/s)
  assert.match(wxss, /\.exam-person-pill\s*\{[\s\S]*?border-radius:\s*999rpx[\s\S]*?background:\s*#eef8f2/s)
  assert.match(detailButtonStyle, /min-width:\s*98rpx/)
  assert.match(detailButtonStyle, /height:\s*46rpx/)
  assert.match(detailButtonStyle, /padding:\s*0 20rpx/)
  assert.match(detailButtonStyle, /font-size:\s*26rpx/)
  assert.match(detailButtonStyle, /line-height:\s*44rpx/)
  assert.match(detailButtonStyle, /border:\s*2rpx solid #078249/)
  assert.match(detailButtonStyle, /color:\s*#078249/)
  assert.match(wxss, /\.exam-card-actions\s*\{[\s\S]*?display:\s*inline-flex[\s\S]*?gap:\s*12rpx/s)
  assert.match(startButtonStyle, /min-width:\s*112rpx/)
  assert.match(startButtonStyle, /height:\s*46rpx/)
  assert.match(startButtonStyle, /border-radius:\s*999rpx/)
  assert.match(startButtonStyle, /background:\s*#078249/)
  assert.match(startButtonStyle, /color:\s*#ffffff/)
  assert.match(wxss, /\.exam-filter-panel\s*\{[\s\S]*?background:\s*#ffffff/s)
  assert.match(wxss, /\.exam-filter-grid\s*\{[\s\S]*?grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/s)
  assert.match(wxss, /\.exam-filter-action\.primary\s*\{[\s\S]*?background:\s*#078249/s)
})

test("safety exam detail toolbar uses back draft and submit actions", () => {
  for (const page of ["pages/training/exam-basic/index", "pages/training/exam-detail/index"]) {
    const wxml = readMini(`${page}.wxml`)
    const wxss = readMini(`${page}.wxss`)
    const backButtonStyle = wxss.match(/\.exam-back-btn\s*\{[\s\S]*?\}/)?.[0] || ""
    const draftButtonStyle = wxss.match(/\.exam-draft-btn\s*\{[\s\S]*?\}/)?.[0] || ""
    const submitButtonStyle = wxss.match(/\.exam-submit-btn\s*\{[\s\S]*?\}/)?.[0] || ""

    assert.doesNotMatch(wxml, /<t-button[\s\S]*exam-back-btn/, page)
    assert.doesNotMatch(wxml, /主单信息/, page)
    assert.match(wxml, /<view class="exam-back-btn" bindtap="goBack">[\s\S]*<t-icon name="chevron-left" size="38rpx"[\s\S]*<text>返回<\/text>[\s\S]*<\/view>/, page)
    assert.match(wxml, /<view class="exam-toolbar-actions">[\s\S]*<view class="exam-draft-btn" bindtap="saveDraft">[\s\S]*<t-icon name="file-paste" size="34rpx"[\s\S]*<text>草稿<\/text>[\s\S]*<\/view>[\s\S]*<view class="exam-submit-btn" bindtap="submitExam">[\s\S]*<t-icon name="send" size="34rpx"[\s\S]*<text>提交<\/text>[\s\S]*<\/view>[\s\S]*<\/view>/, page)
    assert.match(backButtonStyle, /width:\s*126rpx/)
    assert.match(backButtonStyle, /height:\s*60rpx/)
    assert.match(backButtonStyle, /display:\s*inline-flex/)
    assert.match(backButtonStyle, /border-radius:\s*16rpx/)
    assert.match(draftButtonStyle, /width:\s*136rpx/)
    assert.match(draftButtonStyle, /height:\s*60rpx/)
    assert.match(draftButtonStyle, /display:\s*inline-flex/)
    assert.match(draftButtonStyle, /border-radius:\s*16rpx/)
    assert.match(submitButtonStyle, /width:\s*136rpx/)
    assert.match(submitButtonStyle, /height:\s*60rpx/)
    assert.match(submitButtonStyle, /border-radius:\s*16rpx/)
    assert.match(submitButtonStyle, /background:\s*#078249/)
  }
})

test("safety exam basic page matches the reference main-info fields", () => {
  const wxml = readMini("pages/training/exam-basic/index.wxml")
  const wxss = readMini("pages/training/exam-basic/index.wxss")
  const examData = reload("../pages/training/exam-data")
  const record = examData.getExamRecord("KSPC00000072-190")
  const rows = examData.getExamInfoRows(record)

  ;["返回", "草稿", "提交", "基础信息", "明细信息"].forEach(label => {
    assert.match(wxml, new RegExp(label))
  })
  assert.doesNotMatch(wxml, /主单信息/)
  assert.deepEqual(rows.map(item => item.label), ["编码", "公司", "部门", "考试人员", "考试", "考试日期", "状态", "创建时间", "备注"])
  assert.equal(record.code, "KSPC00000072-190")
  assert.equal(record.company, "广东广晟有色金属光电新材料有限公司")
  assert.equal(record.department, "火法厂")
  assert.equal(record.person, "霍永清")
  assert.equal(record.status, "待考试")
  assert.equal(record.createdAt, "2026-05-14 15:35")
  assert.match(wxml, /class="exam-detail-tab \{\{activeDetailTab === 'basic' \? 'active' : ''\}\}" bindtap="openBasic"/)
  assert.match(wxml, /class="exam-detail-tab \{\{activeDetailTab === 'detail' \? 'active' : ''\}\}" bindtap="openDetail"/)
  assert.match(wxml, /wx:if="\{\{activeDetailTab === 'basic'\}\}"/)
  assert.match(wxml, /wx:if="\{\{activeDetailTab === 'detail'\}\}"/)

  assert.match(wxss, /\.exam-detail-toolbar\s*\{[\s\S]*?min-height:\s*104rpx[\s\S]*?padding:\s*20rpx 0 10rpx[\s\S]*?justify-content:\s*space-between/s)
  assert.match(wxss, /\.exam-detail-tabs\s*\{[\s\S]*?border-bottom:\s*1rpx solid #dfe5eb/s)
  assert.match(wxss, /\.exam-detail-tab\.active::after\s*\{[\s\S]*?background:\s*#078249/s)
  assert.match(wxss, /\.exam-info-card\s*\{[\s\S]*?margin-top:\s*18rpx[\s\S]*?border-radius:\s*18rpx/s)
  assert.match(wxss, /\.exam-info-row\s*\{[\s\S]*?grid-template-columns:\s*58rpx 190rpx minmax\(0,\s*1fr\)/s)
  assert.match(wxss, /\.required\s*\{[\s\S]*?color:\s*#ff2d20/s)
})

test("safety exam detail page matches the reference question accordion", () => {
  const wxml = readMini("pages/training/exam-basic/index.wxml")
  const wxss = readMini("pages/training/exam-basic/index.wxss")
  const examData = reload("../pages/training/exam-data")
  const firstQuestion = examData.getExamQuestions("q1")[0]

  assert.match(wxml, /明细信息/)
  assert.match(wxml, /题型/)
  assert.match(wxml, /考题/)
  assert.match(wxml, /选项/)
  assert.equal(firstQuestion.type, "单选题")
  assert.match(firstQuestion.question, /2026年5月12日是我国第18个全国防灾减灾日/)
  assert.deepEqual(firstQuestion.options, [
    "A. 人人讲安全、个个会应急",
    "B. 防范灾害风险、护航高质量发展",
    "C. 减轻灾害风险、守护美好家园",
    "D. 提升基层应急能力、筑牢防灾减灾救灾人民防线"
  ])
  assert.match(wxml, /bindtap="toggleQuestion"/)
  assert.match(wxml, /activeDetailTab === 'detail'/)

  assert.match(wxss, /\.exam-question-list\s*\{[\s\S]*?gap:\s*12rpx[\s\S]*?padding-top:\s*14rpx/s)
  assert.match(wxss, /\.exam-question-card\s*\{[\s\S]*?border-radius:\s*16rpx[\s\S]*?box-shadow:/s)
  assert.match(wxss, /\.exam-question-no\s*\{[\s\S]*?border-radius:\s*50%[\s\S]*?background:\s*#1689ff/s)
  assert.match(wxss, /\.exam-question-row\s*\{[\s\S]*?grid-template-columns:\s*100rpx minmax\(0,\s*1fr\)/s)
  assert.match(wxss, /\.exam-option\s*\{[\s\S]*?border:\s*1rpx solid #e7ebf0[\s\S]*?border-radius:\s*12rpx/s)
})

test("safety exam service maps PC exam task endpoints", async () => {
  const calls = []
  require.cache[requestModulePath] = {
    id: requestModulePath,
    filename: requestModulePath,
    loaded: true,
    exports: {
      request(options) {
        calls.push(options)
        return Promise.resolve({ items: [], total: 0 })
      }
    }
  }

  const servicePath = require.resolve("../services/safety-exam")
  delete require.cache[servicePath]
  const exam = require("../services/safety-exam")

  await exam.listExamTasks({ page: 1, pageSize: 20, status: "ACTIVE", keyword: "", companyId: undefined })
  await exam.getExamTaskDetail(42)
  await exam.saveExamProgress(42, { answers: [], currentQuestionIndex: 1, remainingSeconds: 120 })
  await exam.submitExam(42, { requestId: "req-42", answers: [] })
  await exam.createExamTask({
    companyId: 4,
    departmentId: 101105,
    exam: "小程序创建考试",
    examDate: "2026-07-08",
    durationMinutes: 30,
    status: "ACTIVE",
    remark: "移动端提交",
    examPersonUserIds: [10006],
    questions: []
  })
  await exam.listExamPapers({ companyId: 4, departmentId: 101105, page: 1, pageSize: 200 })
  await exam.listExamQuestionBank({
    applicable: true,
    companyId: 4,
    departmentId: 101105,
    page: 1,
    pageSize: 200
  })

  assert.deepEqual(calls, [
    {
      url: "/api/mini/pingan/training/exams",
      method: "GET",
      data: { page: 1, pageSize: 20, status: "ACTIVE" }
    },
    {
      url: "/api/mini/pingan/training/exams/42",
      method: "GET"
    },
    {
      url: "/api/mini/pingan/training/exams/42/progress",
      method: "PUT",
      data: { answers: [], currentQuestionIndex: 1, remainingSeconds: 120 }
    },
    {
      url: "/api/mini/pingan/training/exams/42/submissions",
      method: "POST",
      data: { requestId: "req-42", answers: [] }
    },
    {
      url: "/api/pingan/training/exam-tasks",
      method: "POST",
      data: {
        companyId: 4,
        departmentId: 101105,
        exam: "小程序创建考试",
        examDate: "2026-07-08",
        durationMinutes: 30,
        status: "ACTIVE",
        remark: "移动端提交",
        examPersonUserIds: [10006],
        questions: []
      }
    },
    {
      url: "/api/pingan/training/exam-papers",
      method: "GET",
      data: { companyId: 4, departmentId: 101105, page: 1, pageSize: 200 }
    },
    {
      url: "/api/pingan/training/exam-question-bank",
      method: "GET",
      data: {
        applicable: true,
        companyId: 4,
        departmentId: 101105,
        page: 1,
        pageSize: 200
      }
    }
  ])
})

test("safety exam create page selects existing papers or question-bank questions", () => {
  const basicWxml = readMini("pages/training/exam-create-basic/index.wxml")
  const basicWxss = readMini("pages/training/exam-create-basic/index.wxss")

  assert.match(basicWxml, /<green-header title="安全考试" bind:back="goBack" \/>/)
  assert.doesNotMatch(basicWxml, /<t-button/)

  assert.match(basicWxml, /<view class="exam-create-back-btn" bindtap="goBack">[\s\S]*返回[\s\S]*<\/view>/)
  assert.match(basicWxml, /<view class="exam-create-draft-btn" bindtap="saveDraft">[\s\S]*草稿[\s\S]*<\/view>/)
  assert.match(basicWxml, /class="exam-create-next-btn" bindtap="goNext">[\s\S]*下一步[\s\S]*<\/view>/)
  assert.match(basicWxml, /class="exam-create-submit-btn[^"]*" bindtap="submitExamTask">[\s\S]*提交[\s\S]*<\/view>/)
  assert.match(basicWxml, /wx:if="\{\{activeCreateTab === 'basic'\}\}"/)
  assert.match(basicWxml, /wx:if="\{\{activeCreateTab === 'detail'\}\}"/)

  ;["公司", "部门", "班组", "考试名称", "考试日期", "备注"].forEach(label => {
    assert.match(basicWxml, new RegExp(label))
  })
  assert.match(basicWxml, /现有试卷/)
  assert.match(basicWxml, /题库选题/)
  assert.match(basicWxml, /考试人员将按所选组织范围自动生成/)
  assert.doesNotMatch(basicWxml, /考试人员<\/view>/)
  assert.doesNotMatch(basicWxml, /新增题目|添加选项|请输入考题|请输入选项内容/)
  assert.match(basicWxml, /placeholder="请输入备注"/)

  assert.match(basicWxss, /\.exam-create-toolbar\s*\{[\s\S]*?height:\s*108rpx/s)
  assert.match(basicWxss, /\.exam-create-back-btn\s*\{[\s\S]*?width:\s*136rpx[\s\S]*?height:\s*60rpx[\s\S]*?border-radius:\s*16rpx/s)
  assert.match(basicWxss, /\.exam-create-draft-btn\s*\{[\s\S]*?width:\s*140rpx[\s\S]*?height:\s*60rpx[\s\S]*?border-radius:\s*16rpx/s)
  assert.match(basicWxss, /\.exam-create-next-btn\s*\{[\s\S]*?width:\s*150rpx[\s\S]*?height:\s*60rpx[\s\S]*?border-radius:\s*16rpx/s)
  assert.match(basicWxss, /\.exam-create-form-card\s*\{[\s\S]*?margin-top:\s*14rpx[\s\S]*?border-radius:\s*18rpx/s)
  assert.match(basicWxss, /\.exam-create-date\s*\{[\s\S]*?border-radius:\s*16rpx/s)
  assert.match(basicWxss, /\.exam-create-textarea\s*\{[\s\S]*?border-radius:\s*16rpx/s)
  assert.match(basicWxss, /\.exam-create-submit-btn\s*\{[\s\S]*?min-width:\s*142rpx[\s\S]*?height:\s*56rpx[\s\S]*?border-radius:\s*16rpx/s)
  assert.match(basicWxss, /\.exam-source-segment\s*\{[\s\S]*?grid-template-columns:\s*1fr 1fr/s)
  assert.match(basicWxss, /\.exam-paper-card,[\s\S]*?\.exam-bank-card\s*\{[\s\S]*?border-radius:\s*8rpx/s)
})

test("safety exam create flow saves basic draft and posts PC exam task payload", async () => {
  const navigationCalls = []
  const storage = {}
  const serviceCalls = []
  const resourceCalls = []
  const paperQuestion = {
    questionType: "SINGLE_CHOICE",
    questionText: "进入现场前必须佩戴什么？",
    selectedOption: "A",
    allOptions: "A. 安全帽\nB. 太阳帽\nC. 耳机\nD. 便帽",
    answer: "A",
    score: 5,
    options: [
      { label: "A", content: "安全帽" },
      { label: "B", content: "太阳帽" },
      { label: "C", content: "耳机" },
      { label: "D", content: "便帽" }
    ],
    correctAnswers: ["A"],
    answerExplanation: "进入现场应正确佩戴安全帽。"
  }
  installExamServiceStub({
    createExamTask(payload) {
      serviceCalls.push(payload)
      return Promise.resolve({ id: 998, code: "EXAM-TASK-MINI-998" })
    },
    listExamPapers(query) {
      resourceCalls.push(["papers", query])
      return Promise.resolve({
        items: [{
          id: 501,
          companyId: 4,
          departmentId: 101105,
          teamId: 101201,
          paperName: "建筑施工安全试卷",
          questionCount: 1,
          totalScore: 5,
          questions: [paperQuestion]
        }],
        total: 1
      })
    },
    listExamQuestionBank(query) {
      resourceCalls.push(["bank", query])
      return Promise.resolve({
        items: [{
          id: 601,
          companyId: 4,
          departmentId: 101105,
          teamId: 101201,
          questionType: "MULTIPLE_CHOICE",
          questionTypeLabel: "多选题",
          questionText: "下列哪些属于个人防护用品？",
          score: 5,
          options: paperQuestion.options,
          correctAnswers: ["A", "B"],
          answerExplanation: "按现场防护要求选择。"
        }],
        total: 1
      })
    }
  })
  installOrganizationServiceStub([
    {
      id: 4,
      title: "广东广晟有色金属光电新材料有限公司",
      orgType: "COMPANY",
      children: [
        {
          id: 101105,
          title: "火法厂",
          orgType: "DEPARTMENT",
          children: [{
            id: 101201,
            title: "冶炼一班",
            orgType: "TEAM",
            children: []
          }]
        }
      ]
    }
  ])

  global.wx = {
    navigateBack() {},
    navigateTo(options) {
      navigationCalls.push(options.url)
    },
    redirectTo(options) {
      navigationCalls.push(options.url)
    },
    showToast() {},
    setStorageSync(key, value) {
      storage[key] = value
    },
    getStorageSync(key) {
      return storage[key]
    },
    removeStorageSync(key) {
      delete storage[key]
    }
  }

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

  let pagePath = require.resolve("../pages/training/exam-list/index")
  delete require.cache[pagePath]
  require("../pages/training/exam-list/index")
  pageConfig.openCreate()
  assert.equal(navigationCalls.pop(), "/pages/training/exam-create-basic/index")

  pagePath = require.resolve("../pages/training/exam-create-basic/index")
  delete require.cache[pagePath]
  require("../pages/training/exam-create-basic/index")
  await pageConfig.onLoad()
  assert.equal(pageConfig.data.companyName, "广东广晟有色金属光电新材料有限公司")
  assert.equal(pageConfig.data.departmentName, "全部部门（公司范围）")
  pageConfig.changeDepartment({ detail: { value: "1" } })
  pageConfig.changeTeam({ detail: { value: "1" } })
  assert.equal(pageConfig.data.departmentName, "火法厂")
  assert.equal(pageConfig.data.teamName, "冶炼一班")
  await pageConfig.changeExamName({ detail: { value: "小程序创建安全考试" } })
  await pageConfig.changeExamDate({ detail: { value: "2026-07-08" } })
  await pageConfig.changeRemark({ detail: { value: "移动端创建" } })
  await pageConfig.goNext()
  assert.equal(navigationCalls.length, 0)
  assert.equal(pageConfig.data.activeCreateTab, "detail")
  assert.equal(storage.safetyExamCreateDraft.exam, "小程序创建安全考试")
  assert.equal(storage.safetyExamCreateDraft.teamId, 101201)
  assert.deepEqual(resourceCalls, [
    ["papers", { companyId: 4, departmentId: 101105, teamId: 101201, page: 1, pageSize: 200 }],
    ["bank", { applicable: true, companyId: 4, departmentId: 101105, teamId: 101201, page: 1, pageSize: 200 }]
  ])
  pageConfig.selectBankMode()
  pageConfig.toggleBankQuestion({ currentTarget: { dataset: { id: 601 } } })
  const bankPayload = pageConfig.buildPayload()
  assert.equal(bankPayload.questions[0].questionType, "MULTIPLE_CHOICE")
  assert.deepEqual(bankPayload.questions[0].correctAnswers, ["A", "B"])
  assert.deepEqual(bankPayload.examPersonUserIds, [])
  pageConfig.selectPaperMode()
  pageConfig.selectPaper({ currentTarget: { dataset: { id: 501 } } })
  await pageConfig.submitExamTask()

  assert.deepEqual(serviceCalls[0], {
    companyId: 4,
    departmentId: 101105,
    teamId: 101201,
    exam: "小程序创建安全考试",
    examDate: "2026-07-08",
    durationMinutes: 30,
    status: "ACTIVE",
    remark: "移动端创建",
    examPersonUserIds: [],
    questions: [
      {
        questionType: "SINGLE_CHOICE",
        questionText: "进入现场前必须佩戴什么？",
        selectedOption: "A",
        allOptions: "A. 安全帽\nB. 太阳帽\nC. 耳机\nD. 便帽",
        answer: "A",
        score: 5,
        actualScore: 0,
        sortOrder: 1,
        options: paperQuestion.options,
        correctAnswers: ["A"],
        referenceAnswer: "",
        answerExplanation: "进入现场应正确佩戴安全帽。",
        caseMaterial: "",
        children: []
      }
    ]
  })
  assert.equal(storage.safetyExamCreateDraft, undefined)
  assert.equal(navigationCalls.pop(), "/pages/training/exam-list/index")
})

test("safety exam create basic cascades backend organization nodes with orgName type and nodes fields", async () => {
  installExamServiceStub({})
  installOrganizationServiceStub([
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
              id: 101105,
              orgName: "火法厂",
              type: "DEPARTMENT",
              nodes: []
            },
            {
              id: 101106,
              orgName: "湿法厂",
              type: "DEPARTMENT",
              nodes: []
            }
          ]
        },
        {
          id: 5,
          orgName: "海安加工厂",
          type: "COMPANY",
          nodes: [
            {
              id: 201105,
              orgName: "生产部",
              type: "DEPARTMENT",
              nodes: [{
                id: 201201,
                orgName: "生产一班",
                type: "TEAM",
                nodes: []
              }]
            }
          ]
        }
      ]
    }
  ])
  const storage = {}
  global.wx = {
    navigateBack() {},
    navigateTo() {},
    showToast() {},
    setStorageSync(key, value) {
      storage[key] = value
    },
    getStorageSync(key) {
      return storage[key]
    }
  }

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

  const pagePath = require.resolve("../pages/training/exam-create-basic/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../utils/organization-options")]
  require("../pages/training/exam-create-basic/index")
  await pageConfig.onLoad()

  assert.deepEqual(pageConfig.data.companyOptions.map(item => item.label), ["广晟源成", "海安加工厂"])
  assert.deepEqual(pageConfig.data.departmentOptions.map(item => item.label), ["全部部门（公司范围）", "火法厂", "湿法厂"])
  assert.equal(pageConfig.data.departmentId, "")
  assert.equal(pageConfig.data.teamId, "")
  assert.equal(pageConfig.buildPayload().departmentId, null)
  assert.equal(pageConfig.buildPayload().teamId, null)

  pageConfig.changeCompany({ detail: { value: "1" } })
  assert.equal(pageConfig.data.companyId, 5)
  assert.equal(pageConfig.data.companyName, "海安加工厂")
  assert.deepEqual(pageConfig.data.departmentOptions.map(item => item.label), ["全部部门（公司范围）", "生产部"])
  assert.equal(pageConfig.data.departmentId, "")

  pageConfig.changeDepartment({ detail: { value: "1" } })
  assert.equal(pageConfig.data.departmentId, 201105)
  assert.deepEqual(pageConfig.data.teamOptions.map(item => item.label), ["全部班组（部门范围）", "生产一班"])
  assert.equal(pageConfig.data.teamId, "")
  assert.equal(pageConfig.buildPayload().departmentId, 201105)
  assert.equal(pageConfig.buildPayload().teamId, null)

  pageConfig.changeTeam({ detail: { value: "1" } })
  assert.equal(pageConfig.data.teamId, 201201)
  assert.equal(pageConfig.buildPayload().teamId, 201201)
})

test("safety exam list loads PC-backed exam task cards and counts both statuses", async () => {
  const serviceCalls = []
  const detailCalls = []
  installExamServiceStub({
    listExamTasks(query) {
      serviceCalls.push(query)
      if (query.status === "PENDING_EXAM") {
        return Promise.resolve({
          items: [
            {
              taskId: 901,
              resultId: 1901,
              code: "EXAM-USER-PC-1901",
              company: "广晟源成公司",
              department: "安全环保部",
              exam: "PC端设置的安全考试任务",
              examDate: "2026-07-06",
              status: "PENDING_EXAM",
              statusLabel: "待考试",
              examPersonName: "霍永清",
              createdAt: "2026-07-06 09:30:00",
              remark: "来自PC端"
            }
          ],
          total: 1
        })
      }
      return Promise.resolve({
        items: [{
          taskId: 901,
          resultId: 1901,
          code: "EXAM-USER-PC-1901",
          company: "广晟源成公司",
          exam: "PC端设置的安全考试任务",
          examDate: "2026-07-06",
          status: "EXAMED",
          statusLabel: "已考试",
          examPersonName: "霍永清"
        }],
        total: 3
      })
    },
    getExamTaskDetail(id) {
      detailCalls.push(id)
      return Promise.resolve({
        taskId: id,
        results: [
          { examPersonName: "霍永清" },
          { examPersonName: "张神红" }
        ]
      })
    }
  })

  const navigations = []
  global.wx = {
    navigateBack() {},
    navigateTo(options) {
      navigations.push(options.url)
    },
    showToast() {}
  }

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

  const pagePath = require.resolve("../pages/training/exam-list/index")
  delete require.cache[pagePath]
  require("../pages/training/exam-list/index")

  await pageConfig.onLoad()

  assert.deepEqual(serviceCalls.slice(0, 2), [
    { page: 1, pageSize: 20, status: "PENDING_EXAM" },
    { page: 1, pageSize: 1, status: "EXAMED" }
  ])
  assert.equal(pageConfig.data.statusCounts.pending, 1)
  assert.equal(pageConfig.data.statusCounts.done, 3)
  assert.deepEqual(detailCalls, [])
  assert.equal(pageConfig.data.records[0].company, "广晟源成公司")
  assert.equal(pageConfig.data.records[0].person, "霍永清")
  assert.equal(pageConfig.data.records[0].exam, "PC端设置的安全考试任务")
  assert.equal(pageConfig.data.records[0].statusKey, "pending")
  assert.equal(pageConfig.data.records[0].id, 901)
  pageConfig.openBasic({ currentTarget: { dataset: { id: pageConfig.data.records[0].id } } })
  pageConfig.startExam({ currentTarget: { dataset: { id: pageConfig.data.records[0].id } } })
  assert.deepEqual(navigations, [
    "/pages/training/exam-basic/index?id=901",
    "/pages/training/exam-taking/index?id=901"
  ])

  serviceCalls.length = 0
  await pageConfig.onLoad({ status: "done" })
  assert.equal(pageConfig.data.activeStatus, "done")
  assert.deepEqual(serviceCalls.slice(0, 2), [
    { page: 1, pageSize: 20, status: "EXAMED" },
    { page: 1, pageSize: 1, status: "PENDING_EXAM" }
  ])
  assert.equal(pageConfig.data.records[0].statusLabel, "已考试")
  assert.equal(pageConfig.data.records[0].statusKey, "done")
})

test("safety exam filter applies date department and team to exam task query", async () => {
  const serviceCalls = []
  installExamServiceStub({
    listExamTasks(query) {
      serviceCalls.push(query)
      return Promise.resolve({
        items: query.status === "PENDING_EXAM" ? [{ id: 901, status: "PENDING_EXAM", exam: "筛选考试" }] : [],
        total: query.status === "PENDING_EXAM" ? 1 : 0
      })
    }
  })
  installOrganizationServiceStub([
    {
      id: 1,
      title: "广晟源成公司",
      orgType: "COMPANY",
      children: [
        {
          id: 11,
          title: "火法厂",
          orgType: "DEPARTMENT",
          children: [
            { id: 111, title: "火法一班", orgType: "TEAM", children: [] }
          ]
        },
        {
          id: 12,
          title: "湿法厂",
          orgType: "DEPARTMENT",
          children: [
            { id: 121, title: "湿法一班", orgType: "TEAM", children: [] }
          ]
        }
      ]
    }
  ])

  global.wx = {
    navigateBack() {},
    navigateTo() {},
    showToast() {}
  }

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

  const pagePath = require.resolve("../pages/training/exam-list/index")
  delete require.cache[pagePath]
  require("../pages/training/exam-list/index")

  await pageConfig.onLoad()
  pageConfig.openFilter()
  assert.equal(pageConfig.data.filterVisible, true)
  pageConfig.openFilter()
  assert.equal(pageConfig.data.filterVisible, false)
  pageConfig.openFilter()
  assert.equal(pageConfig.data.filterVisible, true)
  await pageConfig.changeFilterStartDate({ detail: { value: "2026-07-01" } })
  await pageConfig.changeFilterEndDate({ detail: { value: "2026-07-08" } })
  await pageConfig.changeDepartmentFilter({ detail: { value: 1 } })
  await pageConfig.changeTeamFilter({ detail: { value: 1 } })
  await pageConfig.applyFilter()

  assert.equal(pageConfig.data.filterApplied, true)
  assert.equal(pageConfig.data.filterVisible, false)
  assert.deepEqual(serviceCalls.at(-2), {
    page: 1,
    pageSize: 20,
    status: "PENDING_EXAM",
    dateStart: "2026-07-01",
    dateEnd: "2026-07-08",
    departmentId: 11,
    teamId: 111
  })
  assert.deepEqual(serviceCalls.at(-1), {
    page: 1,
    pageSize: 1,
    status: "EXAMED",
    dateStart: "2026-07-01",
    dateEnd: "2026-07-08",
    departmentId: 11,
    teamId: 111
  })
})

test("safety exam basic and detail pages load PC-backed task detail and questions", async () => {
  installExamServiceStub({
    getExamTaskDetail(id) {
      return Promise.resolve({
        id,
        code: "EXAM-TASK-PC-902",
        company: "广晟源成公司",
        department: "火法厂",
        exam: "PC端防灾减灾考试",
        examDate: "2026-07-06",
        status: "ACTIVE",
        statusLabel: "已生效",
        createdAt: "2026-07-06 10:12:00",
        remark: "PC端详情",
        results: [
          { examPersonName: "真实考试人员" },
          { examPersonName: "第二考试人员" }
        ],
        questions: [
          {
            id: 77,
            questionType: "单选题",
            questionText: "进入生产区域前必须佩戴什么？",
            allOptions: "A. 安全帽\nB. 太阳帽\nC. 耳机\nD. 便帽",
            selectedOption: "A",
            answer: "A",
            score: 10,
            actualScore: 10
          }
        ]
      })
    }
  })

  global.wx = {
    navigateBack() {},
    navigateTo() {},
    showToast() {}
  }

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

  let pagePath = require.resolve("../pages/training/exam-basic/index")
  delete require.cache[pagePath]
  require("../pages/training/exam-basic/index")
  await pageConfig.onLoad({ id: "902" })

  assert.equal(pageConfig.data.record.code, "EXAM-TASK-PC-902")
  assert.equal(pageConfig.data.infoRows.find(item => item.label === "公司").value, "广晟源成公司")
  assert.equal(pageConfig.data.infoRows.find(item => item.label === "考试人员").value, "真实考试人员、第二考试人员")
  assert.equal(pageConfig.data.infoRows.find(item => item.label === "状态").value, "已生效")
  assert.equal(pageConfig.data.activeDetailTab, "basic")
  assert.equal(pageConfig.data.questions[0].question, "进入生产区域前必须佩戴什么？")
  assert.deepEqual(pageConfig.data.questions[0].options, ["A. 安全帽", "B. 太阳帽", "C. 耳机", "D. 便帽"])
  pageConfig.openDetail()
  assert.equal(pageConfig.data.activeDetailTab, "detail")

  pagePath = require.resolve("../pages/training/exam-detail/index")
  delete require.cache[pagePath]
  require("../pages/training/exam-detail/index")
  await pageConfig.onLoad({ id: "902" })

  assert.equal(pageConfig.data.questions[0].question, "进入生产区域前必须佩戴什么？")
  assert.deepEqual(pageConfig.data.questions[0].options, ["A. 安全帽", "B. 太阳帽", "C. 耳机", "D. 便帽"])
  assert.equal(pageConfig.data.questions[0].expanded, true)
})

test("safety exam taking page aligns with reference layout and loads PC-backed questions", async () => {
  const wxml = readMini("pages/training/exam-taking/index.wxml")
  const wxss = readMini("pages/training/exam-taking/index.wxss")
  const bodyStyle = wxss.match(/\.exam-taking-body\s*\{[\s\S]*?\}/)?.[0] || ""
  const typeRowStyle = wxss.match(/\.exam-taking-type-row\s*\{[\s\S]*?\}/)?.[0] || ""
  const typeStyle = wxss.match(/\.exam-taking-type\s*\{[\s\S]*?\}/)?.[0] || ""
  const instructionStyle = wxss.match(/\.exam-taking-instruction\s*\{[\s\S]*?\}/)?.[0] || ""
  const questionStyle = wxss.match(/\.exam-taking-question\s*\{[\s\S]*?\}/)?.[0] || ""
  const optionsStyle = wxss.match(/\.exam-taking-options\s*\{[\s\S]*?\}/)?.[0] || ""
  const optionStyle = wxss.match(/\.exam-taking-option\s*\{[\s\S]*?\}/)?.[0] || ""
  const optionLetterStyle = wxss.match(/\.exam-taking-option-letter\s*\{[\s\S]*?\}/)?.[0] || ""
  const optionTextStyle = wxss.match(/\.exam-taking-option-text\s*\{[\s\S]*?\}/)?.[0] || ""

  assert.match(wxml, /<text class="exam-taking-type">\{\{currentQuestion\.type\}\}<\/text>/)
  assert.match(wxml, /<text class="exam-taking-instruction">\{\{currentQuestion\.instruction\}\}<\/text>/)
  assert.match(wxml, /<view class="exam-taking-question">\{\{currentQuestion\.question\}\}<\/view>/)
  assert.match(wxml, /wx:for="\{\{currentQuestion\.options\}\}"/)
  assert.match(wxml, /class="exam-taking-option \{\{item\.selected \? 'selected' : ''\}\}"/)
  assert.match(wxml, /<text class="exam-taking-option-letter">\{\{item\.letter\}\}<\/text>/)
  assert.match(wxml, /<text class="exam-taking-option-text">\{\{item\.text\}\}<\/text>/)
  assert.match(bodyStyle, /padding:\s*24rpx 34rpx 36rpx/)
  assert.match(typeRowStyle, /gap:\s*18rpx/)
  assert.match(typeStyle, /width:\s*150rpx/)
  assert.match(typeStyle, /height:\s*42rpx/)
  assert.match(instructionStyle, /min-width:\s*0/)
  assert.match(instructionStyle, /font-size:\s*24rpx/)
  assert.match(instructionStyle, /font-weight:\s*500/)
  assert.match(instructionStyle, /white-space:\s*nowrap/)
  assert.match(instructionStyle, /text-overflow:\s*ellipsis/)
  assert.match(questionStyle, /font-size:\s*36rpx/)
  assert.match(questionStyle, /font-weight:\s*500/)
  assert.match(questionStyle, /line-height:\s*60rpx/)
  assert.match(optionsStyle, /gap:\s*34rpx/)
  assert.match(optionsStyle, /margin-top:\s*58rpx/)
  assert.match(optionStyle, /min-height:\s*118rpx/)
  assert.match(optionStyle, /grid-template-columns:\s*60rpx minmax\(0,\s*1fr\)/)
  assert.match(optionStyle, /padding:\s*20rpx 34rpx/)
  assert.match(optionStyle, /border-radius:\s*16rpx/)
  assert.match(optionStyle, /background:\s*#f7f7f7/)
  assert.match(optionStyle, /color:\s*#2d3035/)
  assert.match(optionLetterStyle, /font-size:\s*34rpx/)
  assert.match(optionLetterStyle, /color:\s*#2d3035/)
  assert.match(optionTextStyle, /font-size:\s*34rpx/)
  assert.match(optionTextStyle, /font-weight:\s*600/)
  assert.match(optionTextStyle, /color:\s*#2d3035/)
  assert.match(bodyStyle, /display:\s*flex/)
  assert.match(bodyStyle, /flex-direction:\s*column/)
  assert.match(wxss, /\.exam-taking-nav\s*\{[\s\S]*margin-top:\s*auto/)
  assert.match(wxml, /class="exam-taking-answer-card" bindtap="openAnswerCard"/)
  assert.match(wxml, /wx:if="\{\{currentQuestion\.caseMaterial\}\}" class="exam-taking-case"/)
  assert.match(wxml, /wx:if="\{\{currentQuestion\.isCase\}\}" class="exam-taking-case-children"/)
  assert.match(wxml, /wx:for="\{\{currentQuestion\.children\}\}" wx:for-item="child"/)
  assert.match(wxml, /data-question-id="\{\{child\.id\}\}"/)
  assert.match(wxml, /第\{\{child\.subNo\}\}小题/)
  assert.match(wxml, /class="exam-taking-short-answer"/)
  assert.match(wxml, /bindinput="inputShortAnswer"/)
  assert.match(wxml, /wx:if="\{\{answerCardVisible\}\}" class="answer-card-mask"/)
  assert.match(wxml, /class="answer-card-title">\{\{resultMode \? '考试结果' : '答题卡'\}\}<\/text>/)
  assert.match(wxml, /wx:for="\{\{answerCardGroups\}\}"/)
  assert.match(wxml, /\{\{question\.resultState\}\}/)
  assert.match(wxml, /class="answer-card-submit" bindtap="submitAnswers">提交<\/view>/)
  assert.match(wxml, /resultSummary\.correctCount/)
  assert.match(wxml, /resultSummary\.incorrectCount/)
  assert.match(wxml, /resultSummary\.elapsedText/)
  assert.match(wxml, /wx:if="\{\{currentQuestionAnswered\}\}" class="exam-taking-nav"/)
  assert.match(wxss, /\.answer-card-number\.answered\s*\{[\s\S]*background:\s*#fff3e8/)
  assert.match(wxss, /\.answer-card-number\.correct\s*\{[\s\S]*background:\s*#e9f7ef/)
  assert.match(wxss, /\.answer-card-number\.incorrect\s*\{[\s\S]*background:\s*#fff0e3/)
  assert.match(wxml, /canvas-id="examAnnotationCanvas"/)
  assert.match(wxml, /class="exam-annotation-canvas \{\{annotationMode \? 'active' : ''\}\}"/)
  assert.match(wxml, /width="\{\{annotationCanvasWidth\}\}"/)
  assert.match(wxml, /height="\{\{annotationCanvasHeight\}\}"/)
  assert.match(wxml, /bindtouchstart="startAnnotation"/)
  assert.match(wxml, /catchtouchmove="moveAnnotation"/)
  assert.match(wxml, /class="exam-annotation-action clear" bindtap="clearCurrentAnnotation">清除标注<\/view>/)
  assert.match(wxml, /class="exam-annotation-action done" bindtap="toggleAnnotation">完成<\/view>/)
  assert.match(wxss, /\.exam-annotation-canvas\.active\s*\{[\s\S]*pointer-events:\s*auto/)
  assert.match(wxss, /\.answer-card-grid\s*\{[\s\S]*grid-template-columns:\s*repeat\(5,\s*92rpx\)/)
  assert.match(wxss, /\.answer-card-sheet\s*\{[\s\S]*border-radius:\s*30rpx 30rpx 0 0/)
  assert.match(wxss, /\.answer-card-sheet\s*\{[\s\S]*height:\s*46vh/)
  assert.match(wxss, /\.answer-card-sheet\s*\{[\s\S]*max-height:\s*46vh/)
  assert.match(wxss, /\.exam-taking-case-child\s*\{[\s\S]*border-bottom:\s*2rpx solid #eeeeee/)

  installExamServiceStub({
    getExamTaskDetail(id) {
      return Promise.resolve({
        id,
        code: "EXAM-TASK-PC-902",
        company: "广晟源成公司",
        department: "火法厂",
        exam: "PC端防灾减灾考试",
        examDate: "2026-07-06",
        status: "ACTIVE",
        durationMinutes: 20,
        results: [{ examPersonName: "真实考试人员" }],
        questions: [
          {
            id: 77,
            questionKey: "77",
            questionType: "SINGLE_CHOICE",
            questionTypeLabel: "单选题",
            questionText: "进入生产区域前必须佩戴什么？",
            options: [
              { key: "A", content: "安全帽" },
              { key: "B", content: "太阳帽" },
              { key: "C", content: "耳机" },
              { key: "D", content: "便帽" }
            ]
          },
          {
            id: 78,
            questionKey: "78",
            questionType: "MULTIPLE_CHOICE",
            questionTypeLabel: "多选题",
            questionText: "发现安全隐患后应采取哪些措施（ ）",
            options: [
              { key: "A", content: "立即上报" },
              { key: "B", content: "等待下班后处理" },
              { key: "C", content: "采取临时防护" }
            ]
          }
        ]
      })
    },
    saveExamProgress(id, payload) {
      progressCalls.push({ id, payload })
      return Promise.resolve({})
    },
    submitExam(id, payload) {
      assert.equal(id, "902")
      assert.match(payload.requestId, /^mini-exam-902-/)
      assert.deepEqual(payload.answers, [
        { questionId: 77, questionKey: "77", selectedOption: "B" },
        { questionId: 78, questionKey: "78", selectedOption: "C" }
      ])
      return Promise.resolve({
        score: 5,
        correctCount: 1,
        incorrectCount: 1,
        elapsedSeconds: 95,
        questions: [
          { questionKey: "77", score: 10, actualScore: 10 },
          { questionKey: "78", score: 10, actualScore: 0 }
        ]
      })
    }
  })

  const toasts = []
  const progressCalls = []
  const canvasCalls = []
  const navigationBacks = []
  global.wx = {
    getSystemInfoSync() {
      return { statusBarHeight: 24, windowWidth: 390 }
    },
    getMenuButtonBoundingClientRect() {
      return { left: 300, right: 378, top: 30, bottom: 62 }
    },
    navigateBack() {
      navigationBacks.push(true)
    },
    createCanvasContext(canvasId) {
      canvasCalls.push(["context", canvasId])
      return {
        beginPath() { canvasCalls.push(["beginPath"]) },
        clearRect(...args) { canvasCalls.push(["clearRect", ...args]) },
        draw(reserve) { canvasCalls.push(["draw", reserve]) },
        lineTo(...args) { canvasCalls.push(["lineTo", ...args]) },
        moveTo(...args) { canvasCalls.push(["moveTo", ...args]) },
        setLineCap(value) { canvasCalls.push(["lineCap", value]) },
        setLineJoin(value) { canvasCalls.push(["lineJoin", value]) },
        setLineWidth(value) { canvasCalls.push(["lineWidth", value]) },
        setStrokeStyle(value) { canvasCalls.push(["strokeStyle", value]) },
        stroke() { canvasCalls.push(["stroke"]) }
      }
    },
    showToast(options) {
      toasts.push(options)
    },
    showModal(options) {
      if (options.title === "退出考试") {
        options.success({ confirm: true })
      }
    }
  }

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

  const pagePath = require.resolve("../pages/training/exam-taking/index")
  delete require.cache[pagePath]
  require("../pages/training/exam-taking/index")
  await pageConfig.onLoad({ id: "902" })

  assert.equal(pageConfig.data.statusBarHeight, 24)
  assert.equal(pageConfig.data.capsuleSafeRight, 98)
  assert.equal(pageConfig.data.recordId, "902")
  assert.equal(pageConfig.data.questionTotal, 2)
  assert.equal(pageConfig.data.remainingSeconds, 1200)
  assert.equal(pageConfig.data.remainingText, "20:00")
  pageConfig.updateCountdown(pageConfig.countdownEndAt - 65 * 1000)
  assert.equal(pageConfig.data.remainingSeconds, 65)
  assert.equal(pageConfig.data.remainingText, "01:05")
  assert.equal(pageConfig.data.progressPercent, 50)
  assert.equal(pageConfig.data.currentQuestion.question, "进入生产区域前必须佩戴什么？")
  assert.equal(pageConfig.data.currentQuestion.instruction, "下列每题给出的选项中只有一个选项是正确的")
  assert.deepEqual(pageConfig.data.currentQuestion.options.map(item => [item.letter, item.text]), [
    ["A", "安全帽"],
    ["B", "太阳帽"],
    ["C", "耳机"],
    ["D", "便帽"]
  ])

  pageConfig.toggleAnnotation()
  assert.equal(pageConfig.data.annotationMode, true)
  pageConfig.startAnnotation({ touches: [{ x: 12, y: 24 }] })
  pageConfig.moveAnnotation({ touches: [{ x: 36, y: 48 }] })
  pageConfig.endAnnotation({ changedTouches: [{ x: 60, y: 72 }] })
  assert.equal(pageConfig.data.hasCurrentAnnotation, true)
  assert.equal(pageConfig.annotationPaths["77"].length, 1)
  assert.equal(canvasCalls.some(call => call[0] === "stroke"), true)
  pageConfig.toggleAnnotation()
  pageConfig.goNext()
  assert.equal(pageConfig.data.hasCurrentAnnotation, false)
  pageConfig.goPrev()
  assert.equal(pageConfig.data.hasCurrentAnnotation, true)
  pageConfig.clearCurrentAnnotation()
  assert.equal(pageConfig.data.hasCurrentAnnotation, false)

  pageConfig.selectOption({ currentTarget: { dataset: { index: 1 } } })
  assert.equal(pageConfig.data.currentQuestion.options[1].selected, true)
  assert.equal(pageConfig.data.currentQuestionAnswered, true)
  assert.equal(pageConfig.data.answeredCount, 1)
  pageConfig.goBack()
  assert.equal(pageConfig.data.exitConfirmVisible, true)
  pageConfig.cancelExitExam()
  assert.equal(pageConfig.data.exitConfirmVisible, false)
  assert.equal(navigationBacks.length, 0)
  pageConfig.goBack()
  await pageConfig.confirmExitExam()
  await Promise.resolve()
  assert.equal(pageConfig.data.exitConfirmVisible, false)
  assert.equal(navigationBacks.length, 1)
  assert.equal(progressCalls.length > 0, true)
  pageConfig.openAnswerCard()
  assert.equal(pageConfig.data.answerCardVisible, true)
  assert.deepEqual(pageConfig.data.answerCardGroups.map(group => group.title), ["单项选择题", "多项选择题"])
  assert.equal(pageConfig.data.answerCardGroups[0].questions[0].answered, true)
  assert.equal(pageConfig.data.answerCardGroups[0].questions[0].current, true)

  pageConfig.jumpToQuestion({ currentTarget: { dataset: { index: 1 } } })
  assert.equal(pageConfig.data.answerCardVisible, false)
  assert.equal(pageConfig.data.currentQuestionIndex, 1)
  assert.equal(pageConfig.data.progressPercent, 100)
  assert.equal(pageConfig.data.answerCardGroups[1].questions[0].current, true)
  pageConfig.selectOption({ currentTarget: { dataset: { index: 0 } } })
  pageConfig.selectOption({ currentTarget: { dataset: { index: 2 } } })
  assert.deepEqual(pageConfig.data.answers["78"], [0, 2])
  assert.equal(pageConfig.data.currentQuestion.options[0].selected, true)
  assert.equal(pageConfig.data.currentQuestion.options[2].selected, true)
  pageConfig.selectOption({ currentTarget: { dataset: { index: 0 } } })
  assert.deepEqual(pageConfig.data.answers["78"], [2])
  assert.equal(pageConfig.data.answeredCount, 2)

  pageConfig.openAnswerCard()
  assert.equal(pageConfig.data.answerCardGroups[1].questions[0].answered, true)
  assert.equal(pageConfig.data.answerCardGroups[1].questions[0].current, true)
  pageConfig.closeAnswerCard()
  assert.equal(pageConfig.data.answerCardVisible, false)
  pageConfig.goNext()
  assert.equal(pageConfig.data.answerCardVisible, true)
  await pageConfig.submitAnswers()
  assert.equal(pageConfig.data.answerCardVisible, true)
  assert.equal(pageConfig.data.resultMode, true)
  assert.deepEqual(pageConfig.data.resultSummary, {
    correctCount: 1,
    incorrectCount: 1,
    elapsedText: "1分35秒"
  })
  assert.equal(pageConfig.data.answerCardGroups[0].questions[0].resultState, "correct")
  assert.equal(pageConfig.data.answerCardGroups[1].questions[0].resultState, "incorrect")
  const toastCountBeforeExpiry = toasts.length
  pageConfig.updateCountdown(pageConfig.countdownEndAt)
  assert.equal(pageConfig.data.remainingText, "00:00")
  assert.equal(toasts.pop().title, "考试时间已到")
  pageConfig.updateCountdown(pageConfig.countdownEndAt + 1000)
  assert.equal(toasts.length, toastCountBeforeExpiry)
  pageConfig.onUnload()
  assert.equal(pageConfig.countdownTimer, null)
})

test("safety exam answer-card groups follow the earliest paper question number", async () => {
  installExamServiceStub({
    getExamTaskDetail() {
      return Promise.resolve({
        id: 904,
        durationMinutes: 30,
        questions: [
          {
            id: 101,
            questionKey: "101",
            questionType: "MULTIPLE_CHOICE",
            questionTypeLabel: "多选题",
            questionText: "第一题为多选题",
            options: [{ key: "A", content: "选项A" }, { key: "B", content: "选项B" }]
          },
          {
            id: 102,
            questionKey: "102",
            questionType: "SINGLE_CHOICE",
            questionTypeLabel: "单选题",
            questionText: "第二题为单选题",
            options: [{ key: "A", content: "选项A" }, { key: "B", content: "选项B" }]
          },
          {
            id: 103,
            questionKey: "103",
            questionType: "MULTIPLE_CHOICE",
            questionTypeLabel: "多选题",
            questionText: "第三题仍为多选题",
            options: [{ key: "A", content: "选项A" }, { key: "B", content: "选项B" }]
          }
        ]
      })
    },
    saveExamProgress() {
      return Promise.resolve({})
    }
  })

  global.wx = {
    getSystemInfoSync() {
      return { statusBarHeight: 24, windowWidth: 390 }
    },
    getMenuButtonBoundingClientRect() {
      return { left: 300 }
    },
    showToast() {}
  }
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

  const pagePath = require.resolve("../pages/training/exam-taking/index")
  delete require.cache[pagePath]
  require("../pages/training/exam-taking/index")
  await pageConfig.onLoad({ id: "904" })

  assert.deepEqual(
    pageConfig.data.answerCardGroups.map(group => group.title),
    ["多项选择题", "单项选择题"]
  )
  assert.deepEqual(
    pageConfig.data.answerCardGroups.map(group => group.questions.map(question => question.no)),
    [[1, 3], [2]]
  )
  pageConfig.onUnload()
})

test("safety exam keeps all case children on one page and one answer-card item", async () => {
  let submittedPayload = null
  installExamServiceStub({
    getExamTaskDetail() {
      const base = {
        id: 90,
        caseTitle: "施工现场事故案例",
        caseMaterial: "某项目在施工过程中发生安全事故，请根据材料作答。"
      }
      return Promise.resolve({
        id: 903,
        durationMinutes: 30,
        questions: [
          {
            ...base,
            questionKey: "90:0",
            questionType: "SINGLE_CHOICE",
            questionTypeLabel: "单选题",
            questionText: "事故发生后首先应做什么？",
            options: [{ key: "A", content: "立即报告" }, { key: "B", content: "隐瞒事故" }]
          },
          {
            ...base,
            questionKey: "90:1",
            questionType: "MULTIPLE_CHOICE",
            questionTypeLabel: "多选题",
            questionText: "现场应采取哪些措施？",
            options: [{ key: "A", content: "设置警戒" }, { key: "B", content: "组织救援" }]
          },
          {
            ...base,
            questionKey: "90:2",
            questionType: "SHORT_ANSWER",
            questionTypeLabel: "问答题",
            questionText: "简述后续整改要求。"
          }
        ]
      })
    },
    saveExamProgress() {
      return Promise.resolve({})
    },
    submitExam(id, payload) {
      submittedPayload = payload
      return Promise.resolve({
        correctCount: 2,
        incorrectCount: 1,
        elapsedSeconds: 60,
        questions: [
          { questionKey: "90:0", score: 1, actualScore: 1 },
          { questionKey: "90:1", score: 1, actualScore: 0 },
          { questionKey: "90:2", score: 1, actualScore: 1 }
        ]
      })
    }
  })

  global.wx = {
    getSystemInfoSync() {
      return { statusBarHeight: 24, windowWidth: 390 }
    },
    getMenuButtonBoundingClientRect() {
      return { left: 300 }
    },
    showToast() {}
  }
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

  const pagePath = require.resolve("../pages/training/exam-taking/index")
  delete require.cache[pagePath]
  require("../pages/training/exam-taking/index")
  await pageConfig.onLoad({ id: "903" })

  assert.equal(pageConfig.data.questionTotal, 1)
  assert.equal(pageConfig.data.currentQuestion.type, "案例题")
  assert.equal(pageConfig.data.currentQuestion.instruction, "请阅读案例材料并完成以下子题")
  assert.deepEqual(
    pageConfig.data.currentQuestion.children.map(child => child.type),
    ["单选题", "多选题", "问答题"]
  )
  assert.deepEqual(pageConfig.data.answerCardGroups.map(group => group.title), ["案例题"])
  assert.equal(pageConfig.data.answerCardGroups[0].questions.length, 1)

  pageConfig.selectOption({ currentTarget: { dataset: { questionId: "90:0", index: 0 } } })
  pageConfig.selectOption({ currentTarget: { dataset: { questionId: "90:1", index: 0 } } })
  pageConfig.selectOption({ currentTarget: { dataset: { questionId: "90:1", index: 1 } } })
  pageConfig.inputShortAnswer({
    currentTarget: { dataset: { questionId: "90:2" } },
    detail: { value: "落实事故隐患整改并复查。" }
  })

  assert.equal(pageConfig.data.currentQuestionAnswered, true)
  assert.equal(pageConfig.data.answeredCount, 1)
  assert.equal(pageConfig.data.answerCardGroups[0].questions[0].answered, true)
  await pageConfig.submitAnswers()
  assert.deepEqual(submittedPayload.answers, [
    { questionId: 90, questionKey: "90:0", selectedOption: "A" },
    { questionId: 90, questionKey: "90:1", selectedOption: "A,B" },
    { questionId: 90, questionKey: "90:2", selectedOption: "落实事故隐患整改并复查。" }
  ])
  assert.equal(pageConfig.data.answerCardGroups[0].questions[0].resultState, "incorrect")
  pageConfig.onUnload()
})

test("safety exam page scripts navigate list, basic, and detail views", async () => {
  const navigationCalls = []
  installExamServiceStub({
    listExamTasks(query) {
      return Promise.resolve({
        items: query.status === "ACTIVE" ? [{ id: "KSPC00000072-190", status: "ACTIVE" }] : [],
        total: query.status === "ACTIVE" ? 1 : 0
      })
    },
    getExamTaskDetail(id) {
      return Promise.resolve({
        id,
        code: id,
        company: "广东广晟有色金属光电新材料有限公司",
        department: "火法厂",
        exam: "2026年防灾减灾知识考试",
        examDate: "2026-05-14",
        status: "ACTIVE",
        statusLabel: "已生效",
        createdAt: "2026-05-14 15:35",
        results: [{ examPersonName: "霍永清" }],
        questions: [
          {
            id: "q1",
            questionType: "单选题",
            questionText: "2026年5月12日是我国第18个全国防灾减灾日，主题是（ ）",
            allOptions: "A. 人人讲安全、个个会应急\nB. 防范灾害风险、护航高质量发展"
          },
          {
            id: "q2",
            questionType: "单选题",
            questionText: "发现安全隐患后应首先（ ）",
            allOptions: "A. 立即上报并采取临时防护\nB. 等待下班后处理"
          }
        ]
      })
    }
  })

  global.wx = {
    navigateBack() {},
    navigateTo(options) {
      navigationCalls.push(options.url)
    },
    redirectTo(options) {
      navigationCalls.push(options.url)
    },
    showToast() {}
  }

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

  let pagePath = require.resolve("../pages/training/exam-list/index")
  delete require.cache[pagePath]
  require("../pages/training/exam-list/index")
  pageConfig.openBasic({ currentTarget: { dataset: { id: "KSPC00000072-190" } } })
  assert.equal(navigationCalls.pop(), "/pages/training/exam-basic/index?id=KSPC00000072-190")
  pageConfig.startExam({ currentTarget: { dataset: { id: "KSPC00000072-190" } } })
  assert.equal(navigationCalls.pop(), "/pages/training/exam-taking/index?id=KSPC00000072-190")

  pagePath = require.resolve("../pages/training/exam-basic/index")
  delete require.cache[pagePath]
  require("../pages/training/exam-basic/index")
  await pageConfig.onLoad({ id: "KSPC00000072-190" })
  const navigationCountBeforeDetailTab = navigationCalls.length
  pageConfig.openDetail()
  assert.equal(navigationCalls.length, navigationCountBeforeDetailTab)
  assert.equal(pageConfig.data.activeDetailTab, "detail")
  pageConfig.toggleQuestion({ currentTarget: { dataset: { id: "q2" } } })
  assert.equal(pageConfig.data.questions.find(item => item.id === "q2").expanded, true)
  pageConfig.openBasic()
  assert.equal(pageConfig.data.activeDetailTab, "basic")
})
