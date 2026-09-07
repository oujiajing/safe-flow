const { getRecord, updateRecord, uploadAttachment } = require("../../../services/mini-three-check")
const { mapMeetingRecordToRow } = require("../../../mappers/meeting")
const { createClientRequestId, createClientUpdatedAt, createSourceRecordId } = require("../../../utils/idempotency")
const { refreshThreeCheckActionState } = require("../../../utils/three-check-permissions")

const MODULE_KEY = "pre-shift-meeting"

function resetConfirmStatusForSelection(row) {
  return {
    ...row,
    safetyConfirmItems: (row.safetyConfirmItems || []).map(item => ({
      ...item,
      expanded: true,
      confirmStatus: ""
    }))
  }
}

Page({
  data: {
    id: "",
    companyKey: "gs",
    meetingRecord: null,
    rawRecord: null,
    recordVersion: undefined,
    sourceRecordId: "",
    ownerUserId: 2,
    ownerOptions: [
      { label: "郭文清", value: 2 },
      { label: "周柏锋", value: 3 },
      { label: "程维新", value: 5 }
    ],
    ownerIndex: 0,
    attendeeOptions: [
      { label: "郭文清", value: "郭文清" },
      { label: "周柏锋", value: "周柏锋" },
      { label: "程维新", value: "程维新" }
    ],
    showAttendeePicker: false,
    loading: false,
    saving: false,
    errorText: "",
    operationResult: "",
    activeDetailTab: "basic",
    detailSectionExpanded: false,
    uploadingImageCheck: false,
    uploadingVideoCheck: false,
    confirmStatusDropdownIndex: -1,
    canUpdateAction: true
  },

  async onLoad(options = {}) {
    const actionState = await refreshThreeCheckActionState("meeting")
    this.setData({
      id: options.id || "",
      companyKey: options.companyKey || "gs",
      canUpdateAction: actionState.canUpdateAction,
      sourceRecordId: createSourceRecordId(MODULE_KEY)
    })
    await this.loadMeeting()
  },

  async loadMeeting() {
    if (!this.data.id) return
    this.setData({ loading: true, errorText: "" })
    try {
      const record = await getRecord(MODULE_KEY, this.data.id)
      const row = resetConfirmStatusForSelection(mapMeetingRecordToRow(record))
      const ownerIndex = Math.max(0, this.data.ownerOptions.findIndex(item => item.label === row.owner || String(item.value) === String(record.ownerUserId)))
      this.setData({
        meetingRecord: row,
        rawRecord: record,
        recordVersion: record.version,
        ownerUserId: record.ownerUserId || this.data.ownerOptions[ownerIndex].value,
        ownerIndex,
        loading: false
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorText: error && error.message ? error.message : "班前会详情加载失败"
      })
    }
  },

  chooseMediaFile(fileKind) {
    const isVideo = fileKind === "VIDEO"
    if (wx.chooseMedia) {
      return new Promise(resolve => {
        wx.chooseMedia({
          count: 1,
          mediaType: [isVideo ? "video" : "image"],
          sourceType: ["album", "camera"],
          success: result => {
            const file = (result.tempFiles || []).find(item => item && item.tempFilePath)
            resolve(file ? file.tempFilePath : "")
          },
          fail: () => resolve("")
        })
      })
    }
    const choose = isVideo ? wx.chooseVideo : wx.chooseImage
    if (!choose) {
      this.setData({ operationResult: isVideo ? "当前微信环境不支持选择视频" : "当前微信环境不支持选择图片" })
      return Promise.resolve("")
    }
    return new Promise(resolve => {
      choose({
        count: 1,
        sourceType: ["album", "camera"],
        success: result => resolve(result.tempFilePath || (result.tempFilePaths && result.tempFilePaths[0]) || ""),
        fail: () => resolve("")
      })
    })
  },

  async uploadMeetingCheck(fileKind) {
    if (!this.data.canUpdateAction) return
    if (!this.data.id) return
    const isVideo = fileKind === "VIDEO"
    const filePath = await this.chooseMediaFile(fileKind)
    if (!filePath) return
    this.setData({
      [isVideo ? "uploadingVideoCheck" : "uploadingImageCheck"]: true,
      operationResult: ""
    })
    try {
      await uploadAttachment(MODULE_KEY, this.data.id, fileKind, filePath)
      await this.loadMeeting()
      this.setData({
        [isVideo ? "uploadingVideoCheck" : "uploadingImageCheck"]: false,
        operationResult: isVideo ? "视频打卡已上传" : "图片打卡已上传"
      })
      if (wx.showToast) {
        wx.showToast({ title: isVideo ? "视频打卡已上传" : "图片打卡已上传", icon: "success" })
      }
    } catch (error) {
      this.setData({
        [isVideo ? "uploadingVideoCheck" : "uploadingImageCheck"]: false,
        operationResult: error && error.message ? error.message : (isVideo ? "视频打卡上传失败" : "图片打卡上传失败")
      })
    }
  },

  uploadMeetingImageCheck() {
    return this.uploadMeetingCheck("IMAGE")
  },

  uploadMeetingVideoCheck() {
    return this.uploadMeetingCheck("VIDEO")
  },

  toggleDetailSection() {
    this.setData({ detailSectionExpanded: !this.data.detailSectionExpanded })
  },

  toggleSafetyItem(event) {
    const index = Number(event.currentTarget.dataset.index)
    const meetingRecord = this.data.meetingRecord
    if (!meetingRecord || !Array.isArray(meetingRecord.safetyConfirmItems)) return
    const safetyConfirmItems = meetingRecord.safetyConfirmItems.map((item, itemIndex) => ({
      ...item,
      expanded: itemIndex === index ? !item.expanded : item.expanded
    }))
    this.setData({ meetingRecord: { ...meetingRecord, safetyConfirmItems } })
  },

  toggleConfirmStatusDropdown(event) {
    const index = Number(event.currentTarget.dataset.index)
    this.setData({
      confirmStatusDropdownIndex: this.data.confirmStatusDropdownIndex === index ? -1 : index
    })
  },

  async chooseConfirmStatus(event) {
    const index = Number(event.currentTarget.dataset.index)
    const status = event.currentTarget.dataset.status
    const meetingRecord = this.data.meetingRecord
    if (!meetingRecord || !Array.isArray(meetingRecord.safetyConfirmItems) || !status) return
    const safetyConfirmItems = meetingRecord.safetyConfirmItems.map((item, itemIndex) => ({
      ...item,
      confirmStatus: itemIndex === index ? status : item.confirmStatus
    }))
    this.setData({
      meetingRecord: { ...meetingRecord, safetyConfirmItems },
      confirmStatusDropdownIndex: -1
    })
    await this.saveMeetingDetail()
  },

  changeOwner(event) {
    const index = Number(event.detail.value)
    const owner = this.data.ownerOptions[index]
    if (!owner || !this.data.meetingRecord) return
    this.setData({
      ownerUserId: owner.value,
      ownerIndex: index,
      meetingRecord: { ...this.data.meetingRecord, owner: owner.label },
      operationResult: ""
    })
  },

  toggleAttendeePicker() {
    this.setData({ showAttendeePicker: !this.data.showAttendeePicker })
  },

  changeAttendees(event) {
    const attendees = event.detail.value || []
    const meetingRecord = this.data.meetingRecord
    if (!meetingRecord) return
    this.setData({
      meetingRecord: {
        ...meetingRecord,
        attendees,
        attendeesText: attendees.join("、")
      },
      operationResult: ""
    })
  },

  updateMeetingContent(event) {
    const meetingRecord = this.data.meetingRecord
    if (!meetingRecord) return
    this.setData({
      meetingRecord: { ...meetingRecord, task: event.detail.value },
      operationResult: ""
    })
  },

  buildSavePayload() {
    const record = this.data.rawRecord || {}
    const row = this.data.meetingRecord || {}
    const rawPayload = record.payload || {}
    return {
      companyId: record.companyId || "",
      departmentId: record.departmentId || "",
      teamId: record.teamId || "",
      ownerUserId: this.data.ownerUserId || record.ownerUserId || undefined,
      businessDate: row.date,
      payload: {
        ...rawPayload,
        companyName: row.company,
        departmentName: row.dept,
        teamName: row.team,
        ownerName: row.owner,
        meetingContent: row.task || "",
        attendees: row.attendees || [],
        safetyConfirmItems: (row.safetyConfirmItems || []).map(item => {
          const { expanded, ...rest } = item
          return rest
        })
      },
      version: this.data.recordVersion,
      sourceRecordId: this.data.sourceRecordId,
      clientRequestId: createClientRequestId(),
      clientUpdatedAt: createClientUpdatedAt()
    }
  },

  async saveMeetingDetail() {
    if (!this.data.canUpdateAction) {
      this.setData({ operationResult: "当前账号仅可查看班前会" })
      return
    }
    if (this.data.saving || !this.data.id) return
    this.setData({ saving: true, operationResult: "" })
    try {
      await updateRecord(MODULE_KEY, this.data.id, this.buildSavePayload())
      this.setData({ saving: false, operationResult: "明细已保存" })
      if (wx.showToast) {
        wx.showToast({ title: "明细已保存", icon: "success" })
      }
    } catch (error) {
      this.setData({
        saving: false,
        operationResult: error && error.message ? error.message : "明细保存失败"
      })
    }
  },

  switchDetailTab(event) {
    const tab = event.currentTarget && event.currentTarget.dataset
      ? event.currentTarget.dataset.tab
      : "basic"
    this.setData({ activeDetailTab: tab === "detail" ? "detail" : "basic" })
  },

  goBack() {
    wx.navigateBack()
  }
})
