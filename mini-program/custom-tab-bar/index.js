const { TAB_ITEMS, isTabPath } = require("../utils/tab-bar")
const notificationService = require("../services/notification")
const { getToken } = require("../utils/auth")

Component({
  data: {
    selected: TAB_ITEMS[0].pagePath,
    tabItems: TAB_ITEMS
  },

  lifetimes: {
    attached() {
      this.syncSelected()
      this.refreshUnreadBadge()
    }
  },

  pageLifetimes: {
    show() {
      this.syncSelected()
      this.refreshUnreadBadge()
    }
  },

  methods: {
    onTabTap(event) {
      const nextPath = event.currentTarget.dataset.pagePath
      if (!nextPath || nextPath === this.data.selected) {
        return
      }

      this.setData({
        selected: nextPath
      })
      wx.switchTab({
        url: nextPath,
        fail: () => {
          this.syncSelected()
        }
      })
    },

    syncSelected() {
      const pages = getCurrentPages()
      const currentPage = pages[pages.length - 1]
      const currentPath = currentPage ? `/${currentPage.route}` : TAB_ITEMS[0].pagePath
      const matched = isTabPath(currentPath)

      if (matched && currentPath !== this.data.selected) {
        this.setData({
          selected: currentPath
        })
      }
    },

    async refreshUnreadBadge() {
      if (!getToken()) {
        this.setData({ tabItems: TAB_ITEMS.map(item => ({ ...item, badgeCount: 0 })) })
        return
      }
      try {
        const counts = await notificationService.getUnreadCounts()
        const unread = Math.max(0, Number(counts && counts.unread ? counts.unread : 0))
        this.setData({
          tabItems: TAB_ITEMS.map(item => ({
            ...item,
            badgeCount: item.pagePath === "/pages/message/message" ? unread : 0
          }))
        })
      } catch (error) {
        // Navigation remains usable when unread synchronization fails.
      }
    }
  }
})
