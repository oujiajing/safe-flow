const { createRecord, uploadAttachment } = require("../../../services/mini-three-check")
const { getCurrentUser } = require("../../../services/user")
const { getOrgTree } = require("../../../services/organization")
const { createClientRequestId, createClientUpdatedAt, createSourceRecordId } = require("../../../utils/idempotency")
const { organizationOptionsFor, resolvePickerState } = require("../../../utils/organization-options")
const { appendQuery } = require("../../../utils/module-routes")
const { openMediaPreview } = require("../../../utils/media-preview")

const MODULE_KEY = "key-sites"
const SITE_TYPES = ["吊装作业区", "动火作业区", "有限空间", "危险品库", "临边防护区", "其他"]
const MAX_MEDIA_COUNT = 9

function mediaPath(file) {
  return file && (file.tempFilePath || file.path) ? (file.tempFilePath || file.path) : ""
}

function mediaName(path, fileKind, index) {
  const cleanPath = String(path || "").split("?")[0]
  const name = cleanPath.split("/").pop()
  return name || `${fileKind === "VIDEO" ? "视频" : "图片"}${index + 1}`
}

function normalizeSelectedMedia(files, fileKind) {
  return (files || []).map((file, index) => {
    const path = mediaPath(file)
    return path ? {
      fileKind,
      name: mediaName(path, fileKind, index),
      path,
      thumbPath: fileKind === "VIDEO" ? (file.thumbTempFilePath || "") : path
    } : null
  }).filter(Boolean)
}

function mergeSelectedMedia(current, selected, limit = MAX_MEDIA_COUNT) {
  const byPath = new Map((current || []).map(item => [item.path, item]))
  for (const item of selected || []) byPath.set(item.path, item)
  return Array.from(byPath.values()).slice(0, limit)
}

function today() {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
}

Page({
  data: {
    currentUser: {}, organizationNodes: [],
    companyOptions: [], departmentOptions: [], teamOptions: [],
    companyIndex: 0, departmentIndex: 0, teamIndex: 0,
    selectedCompanyId: "", selectedDepartmentId: "", selectedTeamId: "",
    currentCompany: { company: "", workshop: "" }, currentTeam: "",
    siteTypes: SITE_TYPES, siteTypeIndex: 0,
    form: {
      siteType: SITE_TYPES[0], inspectionDepartment: "", responsibleDepartment: "",
      responsiblePerson: "", acceptancePerson: "", inspectionDate: today()
    },
    attachmentImages: [], attachmentVideos: [], previewVideoUrl: "",
    saving: false, uploadingAttachments: false, errorText: ""
  },

  async onLoad() {
    if (wx.setNavigationBarTitle) wx.setNavigationBarTitle({ title: "新建重点场所" })
    await Promise.all([this.loadUser(), this.loadOrganizations()])
  },

  goBack() {
    wx.navigateBack()
  },

  async loadUser() {
    try {
      const currentUser = await getCurrentUser()
      const name = currentUser.realName || currentUser.username || ""
      this.setData({ currentUser, form: { ...this.data.form, responsiblePerson: name } })
    } catch (error) {
      this.setData({ currentUser: {} })
    }
  },

  async loadOrganizations() {
    try {
      const nodes = await getOrgTree()
      const state = resolvePickerState(nodes, {})
      if (!state) return
      this.setData({ organizationNodes: nodes, ...state })
    } catch (error) {
      this.setData({ errorText: "组织数据加载失败" })
    }
  },

  changeCompany(event) {
    const index = Number(event.detail.value)
    const company = this.data.companyOptions[index]
    if (!company) return
    const departmentOptions = organizationOptionsFor(this.data.organizationNodes, company.value, ["DEPARTMENT"])
    this.setData({ companyIndex: index, selectedCompanyId: company.value, currentCompany: { company: company.label, workshop: "" }, departmentOptions, departmentIndex: 0, selectedDepartmentId: "", teamOptions: [], selectedTeamId: "", currentTeam: "" })
  },

  changeDepartment(event) {
    const index = Number(event.detail.value)
    const department = this.data.departmentOptions[index]
    if (!department) return
    const teamOptions = organizationOptionsFor(this.data.organizationNodes, department.value, ["TEAM"])
    this.setData({ departmentIndex: index, selectedDepartmentId: department.value, currentCompany: { ...this.data.currentCompany, workshop: department.label }, teamOptions, teamIndex: 0, selectedTeamId: "", currentTeam: "", form: { ...this.data.form, inspectionDepartment: department.label } })
  },

  changeTeam(event) {
    const index = Number(event.detail.value)
    const team = this.data.teamOptions[index]
    if (!team) return
    this.setData({ teamIndex: index, selectedTeamId: team.value, currentTeam: team.label })
  },

  changeSiteType(event) {
    const index = Number(event.detail.value)
    this.setData({ siteTypeIndex: index, form: { ...this.data.form, siteType: SITE_TYPES[index] } })
  },

  changeDate(event) {
    this.setData({ form: { ...this.data.form, inspectionDate: event.detail.value } })
  },

  updateField(event) {
    const field = event.currentTarget.dataset.field
    this.setData({ form: { ...this.data.form, [field]: event.detail.value }, errorText: "" })
  },

  chooseAttachments(event) {
    const fileKind = event.currentTarget.dataset.kind
    const dataKey = fileKind === "VIDEO" ? "attachmentVideos" : "attachmentImages"
    const remaining = MAX_MEDIA_COUNT - this.data[dataKey].length
    if (remaining <= 0) {
      if (wx.showToast) wx.showToast({ title: `最多选择${MAX_MEDIA_COUNT}个文件`, icon: "none" })
      return
    }
    const applySelection = files => {
      const selected = normalizeSelectedMedia(files, fileKind)
      this.setData({ [dataKey]: mergeSelectedMedia(this.data[dataKey], selected), errorText: "" })
    }
    if (wx.chooseMedia) {
      wx.chooseMedia({
        count: remaining,
        mediaType: [fileKind === "VIDEO" ? "video" : "image"],
        sourceType: ["album", "camera"],
        success: result => applySelection(result.tempFiles || [])
      })
      return
    }
    if (fileKind === "IMAGE" && wx.chooseImage) {
      wx.chooseImage({ count: remaining, sourceType: ["album", "camera"], success: result => applySelection((result.tempFilePaths || []).map(path => ({ path }))) })
      return
    }
    if (fileKind === "VIDEO" && wx.chooseVideo) {
      wx.chooseVideo({ sourceType: ["album", "camera"], success: result => applySelection([result]) })
      return
    }
    this.setData({ errorText: "当前微信环境不支持选择该附件" })
  },

  removeAttachment(event) {
    const fileKind = event.currentTarget.dataset.kind
    const index = Number(event.currentTarget.dataset.index)
    const dataKey = fileKind === "VIDEO" ? "attachmentVideos" : "attachmentImages"
    this.setData({ [dataKey]: this.data[dataKey].filter((item, itemIndex) => itemIndex !== index) })
  },

  previewAttachment(event) {
    const fileKind = event.currentTarget.dataset.kind
    const localIndex = Number(event.currentTarget.dataset.index)
    const items = [...this.data.attachmentImages, ...this.data.attachmentVideos]
    const currentIndex = fileKind === "VIDEO" ? this.data.attachmentImages.length + localIndex : localIndex
    const result = openMediaPreview(wx, items, currentIndex)
    if (result.videoUrl) this.setData({ previewVideoUrl: result.videoUrl })
  },

  closeVideoPreview() {
    this.setData({ previewVideoUrl: "" })
  },

  noop() {},

  async uploadSelectedAttachments(recordId) {
    const files = [...this.data.attachmentImages, ...this.data.attachmentVideos]
    if (!files.length) return []
    this.setData({ uploadingAttachments: true })
    const uploaded = []
    try {
      for (const file of files) {
        uploaded.push(await uploadAttachment(MODULE_KEY, recordId, file.fileKind, file.path))
      }
      this.setData({ uploadingAttachments: false })
      return uploaded
    } catch (error) {
      this.setData({ uploadingAttachments: false })
      throw error
    }
  },

  validate() {
    if (!this.data.selectedCompanyId || !this.data.selectedDepartmentId || !this.data.selectedTeamId) return "请选择公司、部门和班组"
    if (!this.data.form.siteType) return "请选择场所类型"
    if (!this.data.form.inspectionDepartment) return "请填写检查部门"
    if (!this.data.form.responsibleDepartment) return "请填写责任部门"
    if (!this.data.form.responsiblePerson) return "请填写责任人"
    if (!this.data.form.acceptancePerson) return "请选择验收人"
    return ""
  },

  async save(event) {
    if (this.data.saving) return
    const errorText = this.validate()
    if (errorText) {
      this.setData({ errorText })
      return
    }
    this.setData({ saving: true, errorText: "" })
    try {
      const user = this.data.currentUser || {}
      const record = await createRecord(MODULE_KEY, {
        companyId: this.data.selectedCompanyId,
        departmentId: this.data.selectedDepartmentId,
        teamId: this.data.selectedTeamId,
        ownerUserId: user.id || user.userId,
        businessDate: this.data.form.inspectionDate,
        content: `${this.data.form.siteType}重点场所检查`,
        payload: { ...this.data.form, statusLabel: "待检查" },
        sourceRecordId: createSourceRecordId(MODULE_KEY),
        clientRequestId: createClientRequestId(),
        clientUpdatedAt: createClientUpdatedAt()
      })
      await this.uploadSelectedAttachments(record.id || record.recordId)
      this.setData({ saving: false })
      if (event.currentTarget.dataset.next === "detail") {
        if (wx.showToast) wx.showToast({ title: "登记已提交", icon: "success" })
        wx.redirectTo({ url: appendQuery("/pages/key-sites/detail/index", { id: record.id }) })
      } else {
        if (wx.showToast) wx.showToast({ title: "草稿已保存", icon: "success" })
        wx.navigateBack()
      }
    } catch (error) {
      this.setData({ saving: false, errorText: error && error.message ? error.message : "保存失败" })
    }
  }
})

module.exports = { MAX_MEDIA_COUNT, SITE_TYPES, mergeSelectedMedia, normalizeSelectedMedia, today }
