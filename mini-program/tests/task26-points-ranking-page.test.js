const assert = require("node:assert/strict")
const fs = require("node:fs")
const test = require("node:test")

const pointsServicePath = require.resolve("../services/safety-points")
const authPath = require.resolve("../utils/auth")

function loadPage({ currentUser, service }) {
  require.cache[pointsServicePath] = {
    id: pointsServicePath,
    filename: pointsServicePath,
    loaded: true,
    exports: service
  }
  require.cache[authPath] = {
    id: authPath,
    filename: authPath,
    loaded: true,
    exports: {
      getCurrentUser() {
        return currentUser
      }
    }
  }

  global.wx = {
    navigateBack() {},
    showToast() {}
  }

  let page = null
  global.Page = config => {
    page = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/points/rank/index")
  delete require.cache[pagePath]
  require("../pages/points/rank/index")
  return page
}

test("ranking adapter maps PC personal and team rows without decorative identities", () => {
  const {
    buildPeriodRange,
    findCurrentIndividual,
    findCurrentTeam,
    formatScore,
    normalizeIndividualRows,
    normalizeTeamRows,
    splitRanking
  } = require("../pages/points/rank/ranking-data")

  assert.deepEqual(buildPeriodRange("current-month", new Date(2026, 6, 13)), {
    dateStart: "2026-07-01",
    dateEnd: "2026-07-31"
  })
  assert.deepEqual(buildPeriodRange("previous-month", new Date(2026, 0, 5)), {
    dateStart: "2025-12-01",
    dateEnd: "2025-12-31"
  })
  assert.deepEqual(buildPeriodRange("all", new Date(2026, 6, 13)), {})
  assert.equal(formatScore(8960), "8,960")
  assert.equal(formatScore(undefined), "0")

  const personal = normalizeIndividualRows([
    { rank: 2, userName: "李娜", team: "检修一班", currentScore: 1165 },
    { rank: 1, userName: "张伟", team: "生产一班", currentScore: 1286 },
    { rank: 4, userName: "赵敏", team: "检修二班", currentScore: 820 }
  ])
  assert.deepEqual(personal.map(item => [item.rank, item.name, item.subtitle, item.scoreText]), [
    [1, "张伟", "生产一班", "1,286"],
    [2, "李娜", "检修一班", "1,165"],
    [4, "赵敏", "检修二班", "820"]
  ])
  assert.deepEqual(splitRanking(personal).podium.map(item => item.rank), [1, 2])
  assert.deepEqual(splitRanking(personal).list.map(item => item.rank), [4])
  assert.equal(findCurrentIndividual(personal, { realName: "赵敏" }).rank, 4)
  assert.equal(findCurrentIndividual(personal, { username: "unknown" }), null)

  const teams = normalizeTeamRows([
    { rank: 2, rankId: "team-2", rankName: "运行二班", memberCount: 16, currentScore: 8420 },
    { rank: 1, rankId: "team-1", rankName: "生产一班", memberCount: 18, currentScore: 8960 },
    { rank: 5, rankId: "team-5", rankName: "生产二班", memberCount: 17, currentScore: 7260 }
  ])
  assert.deepEqual(teams.map(item => [item.rank, item.name, item.subtitle, item.scoreText]), [
    [1, "生产一班", "18人", "8,960"],
    [2, "运行二班", "16人", "8,420"],
    [5, "生产二班", "17人", "7,260"]
  ])
  assert.equal(findCurrentTeam(teams, { teamId: "team-5" }).rank, 5)
  assert.equal(findCurrentTeam(teams, { orgId: "team-1", organizationName: "生产一班" })?.rank, 1)
  assert.equal(findCurrentTeam(teams, { teamName: "运行二班" }).rank, 2)
  assert.equal(findCurrentTeam(teams, { teamName: "未知班组" }), null)
})

test("ranking page loads PC-backed monthly rankings and switches tabs", async () => {
  const calls = []
  const page = loadPage({
    currentUser: { realName: "赵敏", teamId: "team-5", teamName: "生产二班" },
    service: {
      getRankingOverview(query) {
        calls.push(["overview", query])
        return Promise.resolve({ currentTotalScore: 8888, participantCount: 8 })
      },
      listIndividualRanking(query) {
        calls.push(["individual", query])
        return Promise.resolve({
          items: [
            { rank: 1, userName: "张伟", team: "生产一班", currentScore: 1286 },
            { rank: 2, userName: "李娜", team: "检修一班", currentScore: 1165 },
            { rank: 3, userName: "王强", team: "运行二班", currentScore: 1098 },
            { rank: 4, userName: "赵敏", team: "检修二班", currentScore: 820 }
          ],
          total: 4
        })
      },
      listTeamRanking(query) {
        calls.push(["team", query])
        return Promise.resolve({
          items: [
            { rank: 1, rankId: "team-1", rankName: "生产一班", memberCount: 18, currentScore: 8960 },
            { rank: 2, rankId: "team-2", rankName: "运行二班", memberCount: 16, currentScore: 8420 },
            { rank: 3, rankId: "team-3", rankName: "检修一班", memberCount: 14, currentScore: 7985 },
            { rank: 5, rankId: "team-5", rankName: "生产二班", memberCount: 17, currentScore: 7260 }
          ],
          total: 4
        })
      }
    }
  })

  await page.onLoad()

  assert.equal(calls.length, 3)
  assert.equal(calls[0][1].dateStart.endsWith("-01"), true)
  assert.match(calls[0][1].dateEnd, /^\d{4}-\d{2}-\d{2}$/)
  assert.deepEqual(calls[1][1], { ...calls[0][1], page: 1, pageSize: 100 })
  assert.deepEqual(calls[2][1], { ...calls[0][1], page: 1, pageSize: 100, rankBy: "TEAM" })
  assert.deepEqual(page.data.individualPodium.map(item => item.rank), [1, 2, 3])
  assert.deepEqual(page.data.individualList.map(item => item.rank), [4])
  assert.equal(page.data.currentIndividual.name, "赵敏")
  assert.equal(page.data.currentTeam.name, "生产二班")
  assert.deepEqual(page.data.teamFilterLabels, ["全部班组", "生产一班", "检修一班", "运行二班", "检修二班"])
  assert.equal(page.data.loading, false)
  assert.equal(page.data.loadError, "")

  const teamFilterIndex = page.data.teamFilterLabels.indexOf("检修二班")
  page.changeTeamFilter({ detail: { value: teamFilterIndex } })
  assert.equal(page.data.teamFilterLabel, "检修二班")
  assert.deepEqual(page.data.individualPodium, [])
  assert.deepEqual(page.data.individualList.map(item => item.name), ["赵敏"])
  assert.equal(page.data.currentIndividual.name, "赵敏")

  page.switchTab({ currentTarget: { dataset: { tab: "team" } } })
  assert.equal(page.data.activeTab, "team")
  page.switchTab({ currentTarget: { dataset: { tab: "individual" } } })
  assert.equal(page.data.activeTab, "individual")
})

test("ranking page preserves loaded data and exposes retry when refresh fails", async () => {
  let shouldFail = false
  const page = loadPage({
    currentUser: { realName: "张伟" },
    service: {
      getRankingOverview() {
        return shouldFail ? Promise.reject(new Error("offline")) : Promise.resolve({})
      },
      listIndividualRanking() {
        return Promise.resolve({ items: [{ rank: 1, userName: "张伟", team: "生产一班", currentScore: 1286 }] })
      },
      listTeamRanking() {
        return Promise.resolve({ items: [] })
      }
    }
  })

  await page.onLoad()
  shouldFail = true
  await page.loadRanking()

  assert.equal(page.data.individualPodium[0].name, "张伟")
  assert.equal(page.data.loadError, "榜单加载失败，请稍后重试")
  assert.equal(page.data.loading, false)
})

test("ranking page markup and styles reproduce the approved icon-free design", () => {
  const wxml = fs.readFileSync(require.resolve("../pages/points/rank/index.wxml"), "utf8")
  const wxss = fs.readFileSync(require.resolve("../pages/points/rank/index.wxss"), "utf8")

  assert.match(wxml, /class="ranking-tabs"/)
  assert.match(wxml, /data-tab="individual"/)
  assert.match(wxml, /data-tab="team"/)
  assert.match(wxml, /个人榜单/)
  assert.match(wxml, /团队榜单/)
  assert.match(wxml, /class="podium-card"/)
  assert.match(wxml, /bindchange="changeTeamFilter"/)
  assert.match(wxml, /<view class="filter-caret"><\/view>/)
  assert.match(wxml, /podium-slot--second/)
  assert.match(wxml, /podium-slot--first/)
  assert.match(wxml, /podium-slot--third/)
  assert.match(wxml, /我的排名/)
  assert.match(wxml, /我的团队/)
  assert.match(wxml, /class="ranking-list"/)
  assert.match(wxml, /bindtap="retryLoad"/)
  assert.doesNotMatch(wxml, /<image/)
  assert.doesNotMatch(wxml, /t-icon/)
  assert.doesNotMatch(wxml, /avatar|shield|badge|people|person|member-icon/i)

  assert.match(wxss, /#15966b/i)
  assert.match(wxss, /#eaf7f1/i)
  assert.match(wxss, /\.ranking-tabs\s*\{[\s\S]*?border-radius:/)
  assert.match(wxss, /\.podium-grid\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3/)
  assert.match(wxss, /\.ranking-row\s*\{[\s\S]*?grid-template-columns:/)
  assert.match(wxss, /\.ranking-filter--static\s*\{\s*justify-content:\s*flex-start;/)
  assert.match(wxss, /\.filter-caret\s*\{[\s\S]*?position:\s*absolute;[\s\S]*?top:\s*50%;[\s\S]*?border-right:[\s\S]*?border-bottom:[\s\S]*?transform:\s*translateY\(-50%\)\s*rotate\(45deg\);/)
})
