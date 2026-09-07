const { getRecord, listRecords, remindRecord, submitRecord, withdrawRecord } = require("../../../services/mini-three-check")
const { mapMeetingRecordsToRows } = require("../../../mappers/meeting")
const { appendQuery } = require("../../../utils/module-routes")
const { TODAY, loadThreeCheckTabCounts } = require("../../../utils/three-check-stats")
const { refreshThreeCheckActionState, threeCheckActionState } = require("../../../utils/three-check-permissions")

const MODULE_KEY = "pre-shift-meeting"

Page({
  data: {
    companyKey: "gs",
    meetingMode: "list",
    meetingListTab: "pending",
    meetingRows: [],
    meetingFlow: { pending: false, done: false },
    tabCounts: { pending: 0, done: 0, expired: 0 },
    total: 0,
    loading: false,
    errorText: "",
    actionMessage: "",
    meetingDateStart: TODAY,
    meetingDateEnd: TODAY,
    meetingFilterApplied: false,
    showMeetingFilter: false,
    ...threeCheckActionState("meeting")
  },

  async onLoad(options = {}) {
    const actionState = await refreshThreeCheckActionState("meeting")
    this.setData({
      companyKey: options.companyKey || "gs",
      ...actionState
    })
    return this.loadMeetings()
  },

  buildMeetingQuery(status) {
    const query = {
      status,
      page: 1,
      pageSize: 20
    }
    if (this.data.meetingListTab === "expired") {
      query.overdue = true
    }
    if (this.data.meetingListTab === "pending") {
      query.dateStart = TODAY
      query.dateEnd = TODAY
    }
    if (this.data.meetingFilterApplied) {
      if (this.data.meetingDateStart) query.dateStart = this.data.meetingDateStart
      if (this.data.meetingDateEnd) query.dateEnd = this.data.meetingDateEnd
    }
    return query
  },

  async loadCurrentTabRecords() {
    if (this.data.meetingListTab === "pending") {
      const [draftResult, withdrawnResult] = await Promise.all([
        listRecords(MODULE_KEY, this.buildMeetingQuery("DRAFT")),
        listRecords(MODULE_KEY, this.buildMeetingQuery("WITHDRAWN"))
      ])
      return {
        items: [
          ...(draftResult.items || []),
          ...(withdrawnResult.items || [])
        ],
        total: (Number(draftResult.total) || 0) + (Number(withdrawnResult.total) || 0)
      }
    }
    const status = this.data.meetingListTab === "done" ? "OPENED" : "all"
    return listRecords(MODULE_KEY, this.buildMeetingQuery(status))
  },

  async loadMeetings() {
    this.setData({ loading: true, errorText: "", actionMessage: "" })
    try {
      const result = await this.loadCurrentTabRecords()
      const tabCounts = await loadThreeCheckTabCounts(MODULE_KEY)
      const rows = mapMeetingRecordsToRows(result.items || [])
      this.setData({
        meetingRows: rows,
        meetingFlow: {
          pending: this.data.meetingListTab === "pending" && rows.length > 0,
          done: this.data.meetingListTab === "done" && rows.length > 0,
          expired: this.data.meetingListTab === "expired" && rows.length > 0
        },
        tabCounts,
        total: result.total || 0,
        loading: false
      })
    } catch (error) {
      this.setData({
        loading: false,
        meetingFlow: { pending: false, done: false },
        errorText: error && error.message ? error.message : "班前会记录加载失败"
      })
    }
  },

  async changeMeetingListTab(event) {
    this.setData({
      meetingListTab: event.currentTarget.dataset.tab,
      actionMessage: ""
    })
    await this.loadMeetings()
  },

  openMeetingFilter() {
    this.setData({ showMeetingFilter: true })
  },

  closeMeetingFilter() {
    this.setData({ showMeetingFilter: false })
  },

  changeMeetingFilterDate(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const value = event.detail && event.detail.value ? event.detail.value : ""
    if (dataset.field === "dateStart") {
      this.setData({ meetingDateStart: value })
      return
    }
    if (dataset.field === "dateEnd") {
      this.setData({ meetingDateEnd: value })
    }
  },

  async applyMeetingFilter() {
    this.setData({ showMeetingFilter: false, meetingFilterApplied: true })
    await this.loadMeetings()
  },

  goBack() {
    const pages = typeof getCurrentPages === "function" ? getCurrentPages() : []
    if (pages.length > 1) {
      wx.navigateBack()
      return
    }
    wx.switchTab({ url: "/pages/home/home" })
  },

  openMeetingDetail(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    wx.navigateTo({
      url: appendQuery("/pages/three-check/meeting-detail/index", {
        id: dataset.id,
        companyKey: this.data.companyKey
      })
    })
  },

  async remindMeeting(event = {}) {
    if (!this.data.canRemindAction) return
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const id = dataset.id
    if (!id) return
    try {
      await remindRecord(MODULE_KEY, id)
      await this.loadMeetings()
      this.showActionMessage("已催一下")
    } catch (error) {
      this.showActionMessage(error && error.message ? error.message : "催办失败")
    }
  },

  hasCompleteSafetyConfirmations(record) {
    const payload = record && record.payload ? record.payload : {}
    const items = Array.isArray(payload.safetyConfirmItems) ? payload.safetyConfirmItems : []
    return items.every(item => item.confirmStatus === "已确认")
  },

  showActionMessage(title) {
    if (wx.showToast) {
      wx.showToast({ title, icon: "none" })
    }
    this.setData({ actionMessage: title })
  },

  async submitMeeting(event = {}) {
    if (!this.data.canSubmitAction) return
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const id = dataset.id
    if (!id) return
    try {
      const record = await getRecord(MODULE_KEY, id)
      if (!this.hasCompleteSafetyConfirmations(record)) {
        this.showActionMessage("请确认全部安全注意事项")
        return
      }
      await submitRecord(MODULE_KEY, id)
      if (wx.showToast) {
        wx.showToast({ title: "已提交", icon: "success" })
      }
      await this.loadMeetings()
    } catch (error) {
      this.showActionMessage(error && error.message ? error.message : "提交失败")
    }
  },

  async withdrawMeeting(event = {}) {
    if (!this.data.canWithdrawAction) return
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const id = dataset.id
    if (!id) return
    try {
      await withdrawRecord(MODULE_KEY, id, { reason: "小程序撤回" })
      if (wx.showToast) {
        wx.showToast({ title: "已撤回", icon: "success" })
      }
      this.setData({ meetingListTab: "pending" })
      await this.loadMeetings()
    } catch (error) {
      this.showActionMessage(error && error.message ? error.message : "当前状态不可撤回")
    }
  }
})
