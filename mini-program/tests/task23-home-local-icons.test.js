const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")

const rootDir = path.resolve(__dirname, "..")
const homeJs = fs.readFileSync(path.join(rootDir, "pages/home/home.js"), "utf8")
const homeJson = fs.readFileSync(path.join(rootDir, "pages/home/home.json"), "utf8")
const homeWxml = fs.readFileSync(path.join(rootDir, "pages/home/home.wxml"), "utf8")
const homeWxss = fs.readFileSync(path.join(rootDir, "pages/home/home.wxss"), "utf8")
test("home shortcut grid uses packaged WebP icons without a network dependency", () => {
  const icons = {
    hazard: "hazard_rectification",
    "safety-check": "safety_inspection",
    snapshot: "quick_photo_report",
    learning: "safety_learning",
    exam: "safety_exam",
    "point-rank": "points_ranking",
    "key-place": "key_places",
    "special-work": "special_operation"
  }
  for (const [id, file] of Object.entries(icons)) {
    assert.match(homeJs, new RegExp(`(?:"${id}"|${id}): "\\/assets\\/icons\\/${file}\\.webp"`))
    assert.equal(fs.existsSync(path.join(rootDir, "assets/icons", `${file}.webp`)), true)
  }
  assert.doesNotMatch(homeJs, /assetUrl\("mini-program\/icons\//)
  assert.doesNotMatch(homeJs, /fallbackIconPath|handleHomeIconError/)

  assert.match(homeWxml, /<view wx:if="{{hasModuleEntries}}" class="module-stage">[\s\S]*<swiper class="module-swiper" style="height: {{moduleSwiperHeightRpx}}rpx;"[^>]*bindchange="handleModuleSwiperChange"[^>]*>[\s\S]*wx:for="{{modulePages}}"[\s\S]*<view class="circle-grid {{modulePage\.layoutClass}}">/)
  assert.doesNotMatch(homeWxml, /<swiper class="module-swiper"[^>]*indicator-dots/)
  assert.match(homeWxml, /wx:for="{{modulePage\.actions}}"[\s\S]*<view class="icon-box">[\s\S]*<image class="home-icon-img" src="{{item\.iconPath}}" mode="aspectFit"><\/image>[\s\S]*<\/view>/)
  assert.match(homeWxml, /<view wx:if="{{hasModuleEntries}}" class="module-stage">[\s\S]*<view wx:if="{{modulePages\.length > 1}}" class="module-dots">[\s\S]*class="module-dot {{moduleSwiperCurrent === index \? 'module-dot--active' : ''}}"[\s\S]*wx:for="{{modulePages}}"/)
  assert.match(homeWxml, /wx:if="{{item\.badge && item\.badge !== '0' && item\.badge !== 0}}"/)
  assert.doesNotMatch(homeWxml, /class="circle-icon" style="background:/)

  assert.match(homeJs, /title:\s*"隐患整改"[\s\S]*id:\s*"hazard"/)
  assert.match(homeJs, /title:\s*"安全考试",\s*iconPath:\s*homeIcons\.exam,\s*id:\s*"exam"/)
  assert.doesNotMatch(homeJs, /title:\s*"隐患排查"/)
  assert.doesNotMatch(homeJs, /title:\s*"AI大师"/)
  assert.doesNotMatch(homeJs, /team-members|班组成员/)
  assert.match(homeJs, /title:\s*"积分榜单"[\s\S]*id:\s*"point-rank"/)
  assert.match(homeJs, /primaryModuleIds\s*=\s*\[\s*"hazard",\s*"safety-check",\s*"snapshot",\s*"special-work",\s*"learning",\s*"exam",\s*"point-rank",\s*"key-place"\s*\]/)
  assert.match(homeJs, /secondaryModuleIds\s*=\s*\[\s*\]/)
  assert.doesNotMatch(homeJs, /处罚通知单|punish|08_penalty-notice/)
  assert.match(homeJs, /moduleSwiperCurrent:\s*0/)
  assert.match(homeJs, /handleModuleSwiperChange\(event\)\s*\{[\s\S]*moduleSwiperCurrent:\s*event\.detail\.current/)
  assert.match(homeJs, /const initialHomeLayout = buildHomeLayoutState\(circleActions, prototypeInfoCards\)/)
  assert.match(homeJs, /buildHomeLayoutState\(filteredCircleActions, infoCards\)/)

  assert.doesNotMatch(homeWxss, /\.module-swiper\s*\{[^}]*height:\s*264rpx;/)
  assert.match(homeWxss, /\.module-dots\s*\{[\s\S]*display:\s*flex;[\s\S]*justify-content:\s*center;[\s\S]*align-items:\s*center;/)
  assert.match(homeWxss, /\.module-dots\s*\{[\s\S]*height:\s*16rpx;[\s\S]*margin:\s*-2rpx 0 4rpx;/)
  assert.match(homeWxss, /\.module-dot\s*\{[\s\S]*width:\s*8rpx;[\s\S]*height:\s*8rpx;[\s\S]*border-radius:\s*999rpx;/)
  assert.match(homeWxss, /\.module-dot--active\s*\{[^}]*width:\s*24rpx;[^}]*height:\s*8rpx;[^}]*border-radius:\s*999rpx;[^}]*\}/)
  assert.match(homeWxss, /\.circle-grid\s*\{[\s\S]*display:\s*flex;[\s\S]*flex-wrap:\s*wrap;[\s\S]*justify-content:\s*flex-start;[\s\S]*row-gap:\s*24rpx;/)
  assert.match(homeWxss, /\.circle-grid--4 \.circle-item\s*\{[^}]*width:\s*25%;[^}]*\}/)
  assert.match(homeWxss, /\.circle-grid--5 \.circle-item\s*\{[^}]*width:\s*20%;[^}]*\}/)
  assert.match(homeWxss, /\.module-swiper \.circle-grid\s*\{[\s\S]*margin-top:\s*0;/)
  assert.match(homeWxss, /\.home-page\s*\{[\s\S]*padding-bottom:\s*calc\(120rpx \+ env\(safe-area-inset-bottom\)\);/)
  assert.match(homeWxss, /\.home-body\s*\{[\s\S]*width:\s*100%;/)
  assert.match(homeWxss, /\.module-stage\s*\{[\s\S]*margin-top:\s*24rpx;[\s\S]*padding:\s*28rpx 0 20rpx;/)
  assert.match(homeWxss, /\.banner-carousel\s*\{[\s\S]*height:\s*396rpx;[\s\S]*margin:\s*24rpx 24rpx 16rpx;/)
  assert.match(homeWxml, /<image class="banner-image" src="{{item\.src}}" mode="aspectFit"/)
  assert.match(homeWxss, /\.icon-box\s*\{[\s\S]*width:\s*80rpx;[\s\S]*height:\s*80rpx;/)
  assert.match(homeWxss, /\.circle-item\s*\{[\s\S]*gap:\s*4rpx;/)
  assert.match(homeWxss, /\.home-icon-img\s*\{[\s\S]*width:\s*76rpx;[\s\S]*height:\s*76rpx;/)
  assert.match(homeWxss, /\.circle-badge\s*\{[\s\S]*position:\s*absolute;[\s\S]*top:\s*6rpx;[\s\S]*right:\s*6rpx;[\s\S]*transform:\s*translate\(25%,\s*-25%\);[\s\S]*min-width:\s*22rpx;[\s\S]*height:\s*22rpx;/)
  assert.match(homeWxss, /\.circle-badge\s*\{[\s\S]*padding:\s*0 5rpx;[\s\S]*border:\s*2rpx solid #ffffff;[\s\S]*background:\s*#dc2626;[\s\S]*display:\s*inline-flex;[\s\S]*align-items:\s*center;[\s\S]*justify-content:\s*center;[\s\S]*font-size:\s*15rpx;[\s\S]*line-height:\s*1;/)
})

test("home contains no removed example module entries", () => {
  assert.doesNotMatch(homeJs, /处罚通知单|punish|08_penalty-notice/)
  assert.equal(fs.existsSync(path.join(rootDir, "assets/icons/08_penalty-notice.png")), false)
  assert.equal(fs.existsSync(path.join(rootDir, "assets/home-icons/notice.svg")), false)
})

test("home contains no smart inspection prototype module", () => {
  assert.doesNotMatch(homeJs, /智能巡检|smart-inspection|intelligent_inspection/)
  assert.equal(fs.existsSync(path.join(rootDir, "assets/icons/intelligent_inspection.png")), false)
})

test("home header follows the reference title and profile layout while using real user fields", () => {
  assert.doesNotMatch(homeJson, /"navigationBarTitleText"/)
  assert.match(homeJson, /"navigationBarTextStyle":\s*"black"/)
  assert.match(homeWxml, /class="home-custom-nav"/)
  assert.match(homeWxml, /class="home-nav-title"[^>]*>平安班组微应用<\/view>/)
  assert.match(homeWxml, /<view class="top-area \{\{hasInfoCards \? '' : 'top-area--compact'\}\}" style="height: calc\(\{\{statusBarHeight\}\}px \+ \{\{navigationBarHeight\}\}px \+ \{\{profileAreaHeightRpx\}\}rpx\);">/)
  assert.match(homeWxml, /class="home-custom-nav" style="height: calc\(\{\{statusBarHeight\}\}px \+ \{\{navigationBarHeight\}\}px\); padding-top: \{\{statusBarHeight\}\}px;"/)
  assert.match(homeWxml, /<view class="user-row">/)
  assert.match(homeWxml, /<image class="avatar-logo" src="\/static\/home\/pabzlogo\.webp" mode="aspectFit"><\/image>/)
  assert.match(homeWxml, /<view class="user-name">{{currentProfile\.displayName}}<\/view>/)
  assert.match(homeWxml, /wx:if="{{currentProfile\.organizationNames\.length}}"/)
  assert.match(homeWxml, /wx:for="{{currentProfile\.organizationNames}}"/)
  assert.doesNotMatch(homeWxml, /currentProfile\.positionName/)
  assert.match(homeWxml, /<image class="message-icon-img" src="\/static\/home\/message\.png" mode="aspectFit"><\/image>/)

  assert.match(homeJs, /currentProfile:\s*buildCurrentProfile\(getCurrentUser\(\),\s*companyProfiles\[0\]\)/)
  assert.match(homeJs, /function buildCurrentProfile\(user,\s*fallbackProfile,\s*orgTree = \[\]\)/)
  assert.match(homeJs, /organizationNames:\s*resolveHomeOrganizationNames\(orgTree,\s*user\)/)
  assert.doesNotMatch(homeJs, /fallbackProfile\.department|fallbackProfile\.teams && fallbackProfile\.teams\[0\]/)

  assert.match(homeWxss, /\.avatar\s*\{[\s\S]*border-radius:\s*50%;[\s\S]*background:\s*#ffffff;/)
  assert.match(homeWxss, /\.user-row\s*\{[\s\S]*min-height:\s*88rpx;[\s\S]*padding-right:\s*88rpx;/)
  assert.doesNotMatch(homeWxss, /\.avatar\s*\{[^}]*transform:/)
  assert.match(homeWxss, /\.avatar\s*\{[\s\S]*width:\s*72rpx;[\s\S]*height:\s*72rpx;/)
  assert.match(homeWxss, /\.avatar-logo\s*\{[\s\S]*width:\s*64rpx;[\s\S]*height:\s*64rpx;[\s\S]*border-radius:\s*50%;/)
  assert.match(homeWxss, /\.user-name\s*\{[\s\S]*font-size:\s*34rpx;/)
  assert.match(homeWxss, /\.organization-tags\s*\{[\s\S]*display:\s*flex;[\s\S]*gap:\s*12rpx;/)
  assert.match(homeWxss, /\.organization-tag\s*\{[\s\S]*border-radius:\s*8rpx;[\s\S]*background:\s*#1599df;/)
  assert.match(homeWxss, /\.info-grid\s*\{[\s\S]*margin-top:\s*24rpx;/)
  assert.match(homeWxss, /\.message-btn\s*\{[\s\S]*right:\s*0;[\s\S]*background:\s*#1599df;/)
  assert.match(homeWxss, /\.message-icon-img\s*\{[\s\S]*width:\s*48rpx;[\s\S]*height:\s*48rpx;[\s\S]*display:\s*block;/)
})

test("home page removes the bottom learning list from the homepage", () => {
  assert.doesNotMatch(homeWxml, /home-learning-list/)
  assert.doesNotMatch(homeWxml, /learningList/)
  assert.doesNotMatch(homeJs, /homePriorityLearningList/)
  assert.doesNotMatch(homeJs, /learningList:/)
  assert.doesNotMatch(homeWxss, /\.home-learning-list\s*\{/)
  assert.doesNotMatch(homeWxss, /\.home-learning-card\s*\{/)
})

test("home dispatch cards use TDesign icons and updated subtitles", () => {
  assert.match(homeJson, /"t-icon":\s*"tdesign-miniprogram\/icon\/icon"/)
  assert.match(homeJs, /title:\s*"班组派班",\s*desc:\s*"快速创建今日派班",\s*icon:\s*"assignment-user"/)
  assert.match(homeJs, /title:\s*"派班记录表",\s*desc:\s*"查看班次与任务记录",\s*icon:\s*"table"/)
  assert.match(homeWxml, /<view class="info-icon" style="background: {{item\.color}};">\s*<t-icon name="{{item\.icon}}" color="#ffffff" size="38rpx" \/>\s*<\/view>/)
})

test("home message button uses local icon and exposes a real unread badge", () => {
  assert.match(homeWxml, /<view class="message-btn" bindtap="openMessage" aria-role="button" aria-label="消息中心">\s*<image class="message-icon-img" src="\/static\/home\/message\.png" mode="aspectFit"><\/image>/)
  assert.match(homeWxml, /messageUnreadCount > 0/)
  assert.match(homeWxml, /class="message-unread-badge"/)
  assert.match(homeJs, /notificationService\.getUnreadCounts\(\)/)
  assert.match(homeWxss, /\.message-btn\s*\{[\s\S]*position:\s*absolute;[\s\S]*top:\s*50%;[\s\S]*right:\s*0;[\s\S]*transform:\s*translateY\(-50%\);[\s\S]*width:\s*72rpx;[\s\S]*height:\s*72rpx;[\s\S]*background:\s*#1599df;/)
  assert.doesNotMatch(homeJs, /messageButtonRight/)
  assert.match(homeJs, /navigationBarHeight:\s*44/)
  assert.match(homeJs, /Math\.max\(menuButton\.height,\s*\(menuButton\.top - statusBarHeight\) \* 2 \+ menuButton\.height\)/)
  assert.doesNotMatch(homeJs, /headerRowTop|headerRowHeight/)
  assert.match(homeWxss, /\.message-icon-img\s*\{[\s\S]*width:\s*48rpx;[\s\S]*height:\s*48rpx;[\s\S]*filter:\s*brightness\(0\) invert\(1\);/)
  assert.match(homeWxss, /\.message-unread-badge\s*\{[\s\S]*top:\s*0;[\s\S]*right:\s*-2rpx;[\s\S]*background:\s*#e5393f;/)
  assert.doesNotMatch(homeWxml, /<text>消息<\/text>/)
  assert.doesNotMatch(homeWxml, /name="chat-message"/)
  assert.doesNotMatch(homeWxml, /<view class="message-icon"><\/view>/)
})

test("home three-check action buttons are compact and turn orange when pending", () => {
  assert.match(homeWxml, /class="green-action {{item\.hasPending \? 'green-action--pending' : ''}}"/)
  assert.match(homeWxss, /\.green-grid\s*\{[\s\S]*display:\s*grid;[\s\S]*grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\);[\s\S]*justify-items:\s*center;[\s\S]*padding:\s*0 8rpx;/)
  assert.match(homeWxss, /\.green-action\s*\{[\s\S]*flex:\s*0 0 118rpx;[\s\S]*width:\s*120rpx;[\s\S]*height:\s*66rpx;[\s\S]*background:\s*#21aa5a;/)
  assert.match(homeWxss, /\.green-action--pending\s*\{[\s\S]*background:\s*hsla\(35,\s*96%,\s*50%,\s*0\.90\);/)
})
