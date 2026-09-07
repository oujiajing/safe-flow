const assert = require("node:assert/strict")
const test = require("node:test")
const fs = require("node:fs")

const requestModulePath = require.resolve("../utils/request")
const learningServiceModulePath = require.resolve("../services/safety-learning")

function reload(modulePath) {
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  return require(modulePath)
}

function withRequestStub() {
  const calls = []
  require.cache[requestModulePath] = {
    id: requestModulePath,
    filename: requestModulePath,
    loaded: true,
    exports: {
      request(options) {
        calls.push(options)
        return Promise.resolve({ ok: true, options })
      }
    }
  }
  return calls
}

test("learning service maps mini safety learning and check-in endpoints", async () => {
  const calls = withRequestStub()
  const servicePath = require.resolve("../services/safety-learning")
  delete require.cache[servicePath]
  const learning = require("../services/safety-learning")

  await learning.listLearningContents({ page: 1, pageSize: 10, keyword: "", category: undefined })
  await learning.getLearningContent(12)
  await learning.checkInLearning(12, { clientRequestId: "learn-12-7" })

  assert.deepEqual(calls, [
    { url: "/api/mini/pingan/training/safety-learning/contents", method: "GET", data: { page: 1, pageSize: 10 } },
    { url: "/api/mini/pingan/training/safety-learning/contents/12", method: "GET" },
    {
      url: "/api/mini/pingan/training/safety-learning/contents/12/check-in",
      method: "POST",
      data: { clientRequestId: "learn-12-7" }
    }
  ])
})

test("learning service falls back to PC safety learning endpoints when mini adapter is not deployed", async () => {
  const calls = []
  require.cache[requestModulePath] = {
    id: requestModulePath,
    filename: requestModulePath,
    loaded: true,
    exports: {
      request(options) {
        calls.push(options)
        if (options.url.startsWith("/api/mini/pingan/training/safety-learning/contents")) {
          const error = new Error("not found")
          error.response = { statusCode: 404 }
          return Promise.reject(error)
        }
        return Promise.resolve({ ok: true, options })
      }
    }
  }

  const servicePath = require.resolve("../services/safety-learning")
  delete require.cache[servicePath]
  const learning = require("../services/safety-learning")

  await learning.listLearningContents({ page: 1, pageSize: 10, status: "ACTIVE" })
  await learning.getLearningContent(2)

  assert.deepEqual(calls, [
    {
      url: "/api/mini/pingan/training/safety-learning/contents",
      method: "GET",
      data: { page: 1, pageSize: 10, status: "ACTIVE" }
    },
    {
      url: "/api/pingan/training/safety-learning/contents",
      method: "GET",
      data: { page: 1, pageSize: 10, status: "ACTIVE" }
    },
    { url: "/api/mini/pingan/training/safety-learning/contents/2", method: "GET" },
    { url: "/api/pingan/training/safety-learning/contents/2", method: "GET" }
  ])
})

test("points service maps flow and ranking endpoints used by the mini program", async () => {
  const calls = withRequestStub()
  const servicePath = require.resolve("../services/safety-points")
  delete require.cache[servicePath]
  const points = require("../services/safety-points")

  await points.listPointFlow({ page: 1, ownerUserId: 7 })
  await points.getRankingOverview({ rankBy: "TEAM" })
  await points.listIndividualRanking({ page: 1 })
  await points.listTeamRanking({ page: 1 })

  assert.deepEqual(calls, [
    { url: "/api/pingan/three-checks/points-flow/records", method: "GET", data: { page: 1, ownerUserId: 7 } },
    { url: "/api/pingan/safety-points/ranking/overview", method: "GET", data: { rankBy: "TEAM" } },
    { url: "/api/pingan/safety-points/ranking/individual", method: "GET", data: { page: 1 } },
    { url: "/api/pingan/safety-points/ranking/team", method: "GET", data: { page: 1 } }
  ])
})

test("learning and points pages are registered with green-header based shells", () => {
  const app = JSON.parse(fs.readFileSync(require.resolve("../app.json"), "utf8"))
  const { registeredPages } = require("./helpers/app-routes")
  const pages = registeredPages(app)
  assert.ok(pages.includes("pages/training/learning-list/index"))
  assert.ok(pages.includes("pages/training/learning-detail/index"))
  assert.ok(pages.includes("pages/points/flow/index"))
  assert.ok(pages.includes("pages/points/rank/index"))

  for (const pagePath of [
    "../pages/training/learning-list/index.json",
    "../pages/training/learning-detail/index.json",
    "../pages/points/flow/index.json",
    "../pages/points/rank/index.json"
  ]) {
    const json = fs.readFileSync(require.resolve(pagePath), "utf8")
    assert.match(json, /"navigationStyle":\s*"custom"/)
    assert.match(json, /"green-header":\s*"\/components\/ui\/green-header\/index"/)
  }
})

test("legacy learning group and points prototype pages are removed", () => {
  const appJson = fs.readFileSync(require.resolve("../app.json"), "utf8")

  assert.doesNotMatch(appJson, /pages\/group\/group/)
  assert.doesNotMatch(appJson, /pages\/points\/points/)
  assert.equal(fs.existsSync(require.resolve("../pages/training/learning-list/index.json")), true)
  assert.equal(fs.existsSync(require.resolve("../pages/points/flow/index.json")), true)
  assert.equal(fs.existsSync(`${__dirname}/../pages/group/group.js`), false)
  assert.equal(fs.existsSync(`${__dirname}/../pages/points/points.js`), false)
})

test("home permissions hide learning and points entries unless PC permission codes are present", () => {
  const helper = reload("../utils/home-entries")
  const entries = [
    { id: "learning", title: "安全学习" },
    { id: "point-flow", title: "积分流水" },
    { id: "point-rank", title: "积分榜单" }
  ]

  assert.deepEqual(helper.filterHomeEntries(entries, ["PINGAN_TRAINING_ENTRY", "PINGAN_TRAINING_VIEW"]).map(item => item.id), [
    "learning"
  ])
  assert.deepEqual(helper.filterHomeEntries(entries, ["PINGAN_POINTS_ENTRY", "PINGAN_POINTS_VIEW"]).map(item => item.id), [
    "point-flow",
    "point-rank"
  ])
})

test("home learning and points shortcuts enter formal green-header pages", () => {
  const navigationCalls = []
  global.wx = {
    getSystemInfoSync() {
      return { statusBarHeight: 20 }
    },
    getMenuButtonBoundingClientRect() {
      return { top: 20, bottom: 52, height: 32 }
    },
    getStorageSync() {
      return undefined
    },
    setStorageSync() {},
    removeStorageSync() {},
    navigateTo(options) {
      navigationCalls.push(options.url)
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

  const homePath = require.resolve("../pages/home/home")
  delete require.cache[homePath]
  require("../pages/home/home")

  const pointsEntry = pageConfig.data.circleActions.find(item => item.id === "point-rank")
  assert.ok(pointsEntry, "home should expose the formal point-rank entry")
  assert.equal(pointsEntry.title, "积分榜单")
  assert.equal(pointsEntry.type, "module")
  assert.equal(pointsEntry.iconPath, "/assets/icons/points_ranking.webp")
  assert.equal(pageConfig.data.circleActions.some(item => item.id === "point-flow"), false)

  pageConfig.openModule({
    currentTarget: {
      dataset: { id: "study", type: "group" }
    }
  })
  pageConfig.openModule({
    currentTarget: {
      dataset: { id: "points", type: "group" }
    }
  })

  assert.deepEqual(navigationCalls, [
    "/pages/training/learning-list/index?companyKey=gs",
    "/pages/points/rank/index?companyKey=gs"
  ])
})

test("learning list uses full-width search and screenshot card structure", () => {
  const wxml = fs.readFileSync(require.resolve("../pages/training/learning-list/index.wxml"), "utf8")
  const wxss = fs.readFileSync(require.resolve("../pages/training/learning-list/index.wxss"), "utf8")
  const json = fs.readFileSync(require.resolve("../pages/training/learning-list/index.json"), "utf8")
  const filterRowStyle = wxss.match(/\.learning-filter-row\s*\{[\s\S]*?\}/)?.[0] || ""
  const searchBoxStyle = wxss.match(/\.learning-search-box\s*\{[\s\S]*?\}/)?.[0] || ""

  ;["应急管理", "安全设施", "隐患知识", "法律知识", "设备操作规程"].forEach(label => {
    assert.match(wxml, new RegExp(label))
  })
  assert.match(wxml, /learning-status-tabs/)
  assert.match(wxml, /learning-status-tab/)
  assert.match(wxml, /bindtap="changeCategory"/)
  assert.match(wxml, /placeholder="搜索课程"/)
  assert.match(wxml, /name="search"/)
  assert.match(wxml, /<t-button[\s\S]*learning-search-submit[\s\S]*bind:tap="search"[\s\S]*搜索/)
  assert.doesNotMatch(wxml, /learning-filter-wrap/)
  assert.doesNotMatch(wxml, /learning-filter-btn/)
  assert.doesNotMatch(wxml, /icon="filter"/)
  assert.doesNotMatch(wxml, /bind:tap="openFilter"/)
  assert.match(wxml, /<t-icon[\s\S]*name="tag"/)
  assert.match(wxml, /<t-icon[\s\S]*name="star-filled"/)
  assert.doesNotMatch(wxml, /name="chevron-right"/)
  assert.doesNotMatch(wxml, /learning-action/)
  assert.doesNotMatch(wxml, /去学习/)
  assert.doesNotMatch(wxml, /学习状态/)
  assert.match(wxml, /learning-cover-image/)
  assert.match(wxml, /learning-status-pill/)
  assert.match(wxml, /learning-point-icon/)

  assert.match(wxss, /\.learning-status-tabs\s*\{[\s\S]*?background:\s*#ffffff[\s\S]*?border-bottom:\s*1rpx solid #e8edf1/s)
  assert.match(wxss, /\.learning-status-tab-list\s*\{[\s\S]*?grid-template-columns:\s*repeat\(5,\s*minmax\(0,\s*1fr\)\)/s)
  assert.doesNotMatch(wxss, /\.learning-status-tab-list\s*\{[\s\S]*?min-width:\s*max-content/s)
  assert.match(wxss, /\.learning-status-tab\.active::after\s*\{[\s\S]*?background:\s*#2f6bff/s)
  assert.match(filterRowStyle, /display:\s*block/)
  assert.match(filterRowStyle, /padding:\s*18rpx 24rpx/)
  assert.doesNotMatch(filterRowStyle, /grid-template-columns:/)
  assert.doesNotMatch(wxss, /\.learning-filter-wrap\s*\{/)
  assert.doesNotMatch(wxss, /\.learning-filter-btn\s*\{/)
  assert.match(searchBoxStyle, /border-radius:\s*999rpx/)
  assert.match(searchBoxStyle, /width:\s*100%/)
  assert.match(wxss, /\.learning-search-box\s*\{[\s\S]*?grid-template-columns:\s*42rpx minmax\(0,\s*1fr\) 112rpx/s)
  assert.match(wxss, /\.learning-search-submit\s*\{[\s\S]*?border-radius:\s*999rpx[\s\S]*?background:\s*#2f6bff[\s\S]*?color:\s*#ffffff/s)
  assert.match(wxss, /\.learning-card\s*\{[\s\S]*?grid-template-columns:\s*196rpx minmax\(0,\s*1fr\)/s)
  assert.match(wxss, /\.learning-card\s*\{[\s\S]*?padding:\s*10rpx 18rpx/s)
  assert.match(wxss, /\.learning-title\s*\{[\s\S]*?font-size:\s*30rpx/s)
  assert.match(wxss, /\.learning-cover\s*\{[\s\S]*?height:\s*160rpx[\s\S]*?align-self:\s*center/s)
  assert.match(wxss, /\.learning-cover-image\s*\{[\s\S]*?height:\s*160rpx/s)
  assert.match(wxss, /\.learning-cover-placeholder\s*\{[\s\S]*?height:\s*160rpx/s)
  assert.match(wxss, /\.learning-status-pill\.pending\s*\{[\s\S]*?background:\s*#fff7e6[\s\S]*?color:\s*#d97706/s)
  assert.match(wxss, /\.learning-status-pill\.done\s*\{[\s\S]*?background:\s*#e7f6ef[\s\S]*?color:\s*#078249/s)
  assert.doesNotMatch(wxss, /\.learning-card-bottom\s*\{/)
  assert.doesNotMatch(wxss, /\.learning-action\s*\{/)

  assert.match(json, /"t-icon":\s*"tdesign-miniprogram\/icon\/icon"/)
  assert.match(json, /"t-button":\s*"tdesign-miniprogram\/button\/button"/)
})

test("learning list queries PC-backed mini service by selected category and normalizes card fields", async () => {
  const serviceCalls = []
  require.cache[learningServiceModulePath] = {
    id: learningServiceModulePath,
    filename: learningServiceModulePath,
    loaded: true,
    exports: {
      listLearningContents(query) {
        serviceCalls.push(query)
        return Promise.resolve({
          items: [
            {
              id: 77,
              title: "2026防灾减灾日安全培训教育",
              category: "班组安全学习",
              coverImageAttachment: {
                url: "https://pc.example.test/training/77.png"
              },
              pointValue: 1,
              learningStatus: "NOT_STARTED"
            },
            {
              id: 78,
              title: "应急工作原则",
              category: "应急管理",
              pointValue: 1,
              learningStatus: "IN_PROGRESS"
            },
            {
              id: 79,
              title: "应急后的恢复工作",
              category: "应急管理",
              pointValue: 1,
              learningStatus: "CHECKED_IN"
            }
          ],
          total: 1
        })
      }
    }
  }

  global.wx = {
    navigateBack() {},
    navigateTo() {}
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

  const pagePath = require.resolve("../pages/training/learning-list/index")
  delete require.cache[pagePath]
  require("../pages/training/learning-list/index")

  await pageConfig.onLoad()

  assert.deepEqual(pageConfig.data.categoryTabs.map(item => item.label), [
    "应急管理",
    "安全设施",
    "隐患知识",
    "法律知识",
    "设备操作规程"
  ])
  assert.deepEqual(serviceCalls[0], { page: 1, pageSize: 20, status: "ACTIVE" })
  assert.equal(pageConfig.data.contents[0].title, "2026防灾减灾日安全培训教育")
  assert.equal(pageConfig.data.contents[0].categoryLabel, "班组安全学习")
  assert.equal(pageConfig.data.contents[0].coverUrl, "https://pc.example.test/training/77.png")
  assert.equal(pageConfig.data.contents[0].statusLabel, "未学习")
  assert.equal(pageConfig.data.contents[0].statusClass, "pending")
  assert.equal(pageConfig.data.contents[0].pointText, "1积分")
  assert.equal(pageConfig.data.contents[1].statusLabel, "未学习")
  assert.equal(pageConfig.data.contents[1].statusClass, "pending")
  assert.equal(pageConfig.data.contents[2].statusLabel, "已学习")
  assert.equal(pageConfig.data.contents[2].statusClass, "done")

  await pageConfig.changeCategory({ currentTarget: { dataset: { category: "隐患知识" } } })
  assert.equal(pageConfig.data.activeCategory, "隐患知识")
  assert.equal(serviceCalls[1].category, "隐患知识")
})

test("learning detail renders PC uploaded safety learning video", async () => {
  const wxml = fs.readFileSync(require.resolve("../pages/training/learning-detail/index.wxml"), "utf8")
  const wxss = fs.readFileSync(require.resolve("../pages/training/learning-detail/index.wxss"), "utf8")
  const json = fs.readFileSync(require.resolve("../pages/training/learning-detail/index.json"), "utf8")

  assert.match(wxml, /learning-video/)
  assert.match(wxml, /<video[\s\S]*id="learningVideo"[\s\S]*show-fullscreen-btn="{{true}}"[\s\S]*direction="90"[\s\S]*show-center-play-btn="{{true}}"[\s\S]*show-play-btn="{{true}}"[\s\S]*enable-progress-gesture="{{true}}"[\s\S]*enable-auto-rotation="{{true}}"[\s\S]*show-screen-lock-button="{{true}}"[\s\S]*bindfullscreenchange="handleVideoFullscreenChange"[\s\S]*bindtimeupdate="handleVideoTimeUpdate"[\s\S]*bindloadedmetadata="handleVideoLoadedMetadata"/)
  assert.doesNotMatch(wxml, /learning-cover-image/)
  assert.doesNotMatch(wxml, /<button/)
  assert.doesNotMatch(wxml, /<t-button/)
  assert.doesNotMatch(wxml, /learning-detail-hero/)
  assert.doesNotMatch(wxml, /checkin-caption/)
  assert.doesNotMatch(wxml, /requirement-divider/)
  assert.doesNotMatch(wxml, /完成学习后可打卡/)
  assert.doesNotMatch(wxml, /video-fullscreen-action/)
  assert.doesNotMatch(wxml, /video-shell/)
  assert.doesNotMatch(wxml, /video-expand-action/)
  assert.doesNotMatch(wxml, /toggleVideoExpanded/)
  assert.doesNotMatch(wxml, /name="{{videoExpanded \? 'fullscreen-exit' : 'fullscreen'}}"/)
  assert.match(wxml, /class="checkin-action {{canCheckIn \? '' : 'disabled'}}"[\s\S]*<t-icon[\s\S]*name="calendar"[\s\S]*<text>学习打卡<\/text>/)
  assert.doesNotMatch(wxss, /\.learning-detail-hero\s*\{/)
  assert.match(wxss, /\.detail-summary-card\s*\{[\s\S]*?margin-top:\s*20rpx[\s\S]*?border-radius:\s*28rpx/s)
  assert.match(wxss, /\.requirement-card\s*\{[\s\S]*?padding:\s*32rpx 30rpx 18rpx/s)
  assert.doesNotMatch(wxss, /\.video-fullscreen-action\s*\{/)
  assert.doesNotMatch(wxss, /\.video-shell/)
  assert.doesNotMatch(wxss, /\.video-expand-action/)
  assert.doesNotMatch(wxss, /\.checkin-caption\s*\{/)
  assert.doesNotMatch(wxss, /\.requirement-divider\s*\{/)
  assert.match(wxss, /\.learning-video\s*\{[\s\S]*?width:\s*100%[\s\S]*?height:\s*320rpx[\s\S]*?border-radius:\s*16rpx/s)
  assert.match(wxss, /\.watch-progress-fill\s*\{[\s\S]*?background:\s*#08a45f/s)
  assert.match(wxss, /\.checkin-action\s*\{[\s\S]*?margin:\s*18rpx auto 0/s)
  assert.match(wxss, /\.checkin-action\.disabled\s*\{[\s\S]*?background:\s*#c9cbd1/s)
  const pageJson = JSON.parse(json)
  assert.equal(pageJson.pageOrientation, "auto")
  assert.equal(pageJson.usingComponents["t-icon"], "tdesign-miniprogram/icon/icon")

  const checkInCalls = []
  const servicePath = require.resolve("../services/safety-learning")
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      getLearningContent(id) {
        return Promise.resolve({
          id,
          title: "Demo Works Company安全学习视频",
          category: "班组安全学习",
          learningDate: "2026-06-01",
          durationMinutes: 6,
          content: "结合事故看板复盘脚手架通道占用导致的绊跌风险。",
          coverImageAttachment: {
            url: "https://pc.example.test/training/cover.png"
          },
          videoAttachment: {
            url: "http://localhost:9000/safeteam-portfolio/training/video.mp4?X-Amz-Signature=test"
          }
        })
      },
      checkInLearning(id, payload) {
        checkInCalls.push({ id, payload })
        return Promise.resolve({ id: "checkin-1", pointsAwarded: 1 })
      }
    }
  }

  const toastCalls = []
  global.wx = {
    navigateBack() {},
    showToast(options) {
      toastCalls.push(options)
    },
    createVideoContext() {
      return {
        requestFullScreen() {}
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

  const pagePath = require.resolve("../pages/training/learning-detail/index")
  delete require.cache[pagePath]
  require("../pages/training/learning-detail/index")

  await pageConfig.onLoad({ id: "88" })

  assert.equal(
    pageConfig.data.content.videoUrl,
    "http://localhost:9000/safeteam-portfolio/training/video.mp4?X-Amz-Signature=test"
  )
  assert.equal(pageConfig.data.content.coverImageUrl, "")
  assert.equal(pageConfig.data.videoExpanded, undefined)
  assert.equal(pageConfig.toggleVideoExpanded, undefined)
  assert.equal(pageConfig.data.videoFullscreen, false)
  assert.equal(pageConfig.data.requiredWatchSeconds, 360)
  assert.equal(pageConfig.data.requiredWatchText, "6分钟")
  assert.equal(pageConfig.data.canCheckIn, false)

  pageConfig.handleVideoFullscreenChange({ detail: { fullScreen: true, direction: "horizontal" } })
  assert.equal(pageConfig.data.videoFullscreen, true)
  pageConfig.handleVideoFullscreenChange({ detail: { fullScreen: false, direction: "vertical" } })
  assert.equal(pageConfig.data.videoFullscreen, false)

  pageConfig.handleVideoTimeUpdate({ detail: { currentTime: 240, duration: 360 } })
  assert.equal(pageConfig.data.watchedSeconds, 240)
  assert.equal(pageConfig.data.watchProgress, 67)
  assert.equal(pageConfig.data.remainingWatchText, "还需学习2分钟即可打卡")
  await pageConfig.checkIn()
  assert.equal(checkInCalls.length, 0)
  assert.deepEqual(toastCalls.at(-1), { title: "完成学习后可打卡", icon: "none" })

  pageConfig.handleVideoTimeUpdate({ detail: { currentTime: 360, duration: 360 } })
  assert.equal(pageConfig.data.canCheckIn, true)
  await pageConfig.checkIn()
  assert.equal(checkInCalls.length, 1)
  assert.equal(pageConfig.data.checkIn.pointsAwarded, 1)
})



