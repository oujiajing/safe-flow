const TAB_ITEMS = [
  {
    iconPath: "/assets/tab-bar/home.png",
    pagePath: "/pages/home/home",
    selectedIconPath: "/assets/tab-bar/home-active.png",
    text: "首页"
  },
  {
    iconPath: "/assets/tab-bar/analysis.png",
    pagePath: "/pages/analysis/analysis",
    selectedIconPath: "/assets/tab-bar/analysis-active.png",
    text: "数据分析"
  },
  {
    iconPath: "/assets/tab-bar/message.png",
    pagePath: "/pages/message/message",
    selectedIconPath: "/assets/tab-bar/message-active.png",
    text: "消息"
  },
  {
    iconPath: "/assets/tab-bar/mine.png",
    pagePath: "/pages/mine/mine",
    selectedIconPath: "/assets/tab-bar/mine-active.png",
    text: "我的"
  }
]

function normalizePath(route) {
  if (!route) return TAB_ITEMS[0].pagePath
  return route.startsWith("/") ? route : `/${route}`
}

function isTabPath(route) {
  const path = normalizePath(route)
  return TAB_ITEMS.some(item => item.pagePath === path)
}

function syncTabBarSelected(page, route) {
  if (!page || typeof page.getTabBar !== "function") return false
  const selected = normalizePath(route)
  if (!isTabPath(selected)) return false
  const tabBar = page.getTabBar()
  if (!tabBar || typeof tabBar.setData !== "function") return false
  tabBar.setData({ selected })
  return true
}

module.exports = {
  TAB_ITEMS,
  isTabPath,
  normalizePath,
  syncTabBarSelected
}
