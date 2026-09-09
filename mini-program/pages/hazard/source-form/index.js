const {
  createHazardRecord,
  submitHazardRecord,
  uploadHazardAttachment
} = require("../../../services/hazard-source-record")
const { getCurrentUser } = require("../../../services/user")
const { getOrgTree } = require("../../../services/organization")
const { createClientRequestId, createClientUpdatedAt, createSourceRecordId } = require("../../../utils/idempotency")
const { appendQuery } = require("../../../utils/module-routes")
const {
  organizationOptionsFor,
  resolvePickerState,
  resolveScopedOrganizationDefaults
} = require("../../../utils/organization-options")

const TITLE_BY_MODULE = {
  "quick-shot": "随手拍",
  "safety-check": "安全检查"
}

function today() {
  return formatLocalDate(new Date())
}

function formatLocalDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

function formatLocalClock(date) {
  const hours = String(date.getHours()).padStart(2, "0")
  const minutes = String(date.getMinutes()).padStart(2, "0")
  return `${hours}:${minutes}`
}

function combineDateTime(dateText, clockText) {
  const date = dateText || today()
  const clock = clockText || formatLocalClock(new Date())
  return `${date} ${clock}:00`
}

function initialClock() {
  return formatLocalClock(new Date())
}

function initialDateTime() {
  const date = new Date()
  const yearMonthDay = formatLocalDate(date)
  const hours = String(date.getHours()).padStart(2, "0")
  const minutes = String(date.getMinutes()).padStart(2, "0")
  const seconds = String(date.getSeconds()).padStart(2, "0")
  return `${yearMonthDay} ${hours}:${minutes}:${seconds}`
}

function defaultCheckDateState() {
  const date = new Date()
  const checkDate = formatLocalDate(date)
  const checkClock = formatLocalClock(date)
  const seconds = String(date.getSeconds()).padStart(2, "0")
  return {
    checkDate,
    checkClock,
    checkTime: `${checkDate} ${checkClock}:${seconds}`
  }
}

function safeDecodeText(value) {
  if (!value) return ""
  try {
    return decodeURIComponent(value)
  } catch (error) {
    return value
  }
}

function pickFirstValue(source, keys) {
  for (const key of keys) {
    if (source && source[key] !== undefined && source[key] !== null && source[key] !== "") {
      return source[key]
    }
  }
  return ""
}

function filenameFromPath(path) {
  if (!path) return ""
  const parts = String(path).split(/[\\/]/)
  return parts[parts.length - 1] || path
}

function normalizeMediaType(file) {
  const explicitType = String(file && (file.fileType || file.type || file.mediaType) ? file.fileType || file.type || file.mediaType : "").toLowerCase()
  if (explicitType.includes("video")) return "video"
  if (explicitType.includes("image")) return "image"
  const path = String(file && (file.tempFilePath || file.path) ? file.tempFilePath || file.path : "").toLowerCase()
  return /\.(mp4|mov|m4v|avi|webm)$/.test(path) ? "video" : "image"
}

function normalizeAttachment(file) {
  const path = file && (file.tempFilePath || file.path)
  if (!path) return null
  const type = normalizeMediaType(file)
  return {
    type,
    path,
    name: filenameFromPath(path)
  }
}

const DEPARTMENT_SCOPED_ROLES = ["DEPARTMENT_MANAGER", "WORKSHOP_DIRECTOR"]
const COMPANY_SCOPED_ROLES = ["COMPANY_LEADER", "ENTERPRISE_LEADER"]
const MEMBER_ROLES = ["CURTAIN_WALL_MEMBER", "TEAM_MEMBER"]
const DEFAULT_CHECK_METHOD_OPTIONS = [
  "四不两直对安全管理情况开展检查",
  "召开现场安全会议",
  "日常检查"
]

function hasAnyRole(user, roleCodes) {
  const roles = user && Array.isArray(user.roles) ? user.roles : []
  return roles.some(role => roleCodes.includes(role))
}

function isNumericId(value) {
  return value !== "" && value !== undefined && value !== null && Number.isFinite(Number(value))
}

function normalizeId(value) {
  return isNumericId(value) ? Number(value) : value
}

function organizationLocksFor(user) {
  if (hasAnyRole(user, MEMBER_ROLES)) {
    return { company: true, department: true, team: true }
  }
  if (hasAnyRole(user, DEPARTMENT_SCOPED_ROLES)) {
    return { company: true, department: true, team: false }
  }
  if (hasAnyRole(user, COMPANY_SCOPED_ROLES)) {
    return { company: true, department: false, team: false }
  }
  return { company: false, department: false, team: false }
}

Page({
  data: {
    moduleKey: "quick-shot",
    title: "随手拍",
    companyKey: "gs",
    currentUser: {},
    organizationNodes: [],
    organizationLocks: {
      company: false,
      department: false,
      team: false
    },
    companyOptions: [],
    departmentOptions: [],
    teamOptions: [],
    companyIndex: 0,
    departmentIndex: 0,
    teamIndex: 0,
    currentCompany: {
      company: "",
      workshop: ""
    },
    currentTeam: "",
    inspectionUnitOptions: [],
    inspectedUnitOptions: [],
    selectedInspectionUnitId: "",
    selectedInspectedUnitId: "",
    currentInspectionUnitName: "",
    currentInspectedUnitName: "",
    inspectionUnitIndex: 0,
    inspectedUnitIndex: 0,
    checkMethodOptions: DEFAULT_CHECK_METHOD_OPTIONS.map(label => ({ label, checked: true })),
    selectedCompanyId: "",
    selectedDepartmentId: "",
    selectedTeamId: "",
    photo: "",
    attachments: [],
    uploadDate: today(),
    checkClock: initialClock(),
    safetyFormTab: "base",
    safetyHazardCards: [{ id: "card-1", index: 1, collapsed: false }],
    submitting: false,
    errorText: "",
    operationResult: "",
    form: {
      hazardDescription: "",
      location: "",
      checkTheme: "",
      checkArea: "",
      checkItem: "",
      inspectedProject: "",
      manualCheckType: "",
      checkDate: today(),
      checkTime: initialDateTime(),
      manualInspectors: "",
      manualInspectedUnitUsers: "",
      checkContent: "",
      remark: "",
      checkMethods: DEFAULT_CHECK_METHOD_OPTIONS,
      rectificationMeasures: "",
      rectificationResponsiblePerson: "",
      rectificationDeadline: "",
      enableAi: "是"
    }
  },

  async onLoad(options = {}) {
    const moduleKey = options.moduleKey || "quick-shot"
    const title = safeDecodeText(options.title) || TITLE_BY_MODULE[moduleKey] || "隐患记录"
    const dateState = defaultCheckDateState()
    this.setData({
      moduleKey,
      title,
      companyKey: options.companyKey || "gs",
      uploadDate: dateState.checkDate,
      checkClock: dateState.checkClock,
      form: {
        ...this.data.form,
        checkDate: dateState.checkDate,
        checkTime: dateState.checkTime
      }
    })
    if (wx.setNavigationBarTitle) {
      wx.setNavigationBarTitle({ title: `新增${title}` })
    }
    await this.loadCurrentUser()
    await this.loadOrganizationOptions()
  },

  async loadCurrentUser() {
    try {
      const currentUser = await getCurrentUser()
      this.setData({ currentUser: currentUser || {} })
    } catch (error) {
      this.setData({ currentUser: {} })
    }
  },

  async loadOrganizationOptions() {
    try {
      const nodes = await getOrgTree()
      const user = this.data.currentUser || {}
      const locks = organizationLocksFor(user)
      const scopedDefaults = resolveScopedOrganizationDefaults(nodes, user.orgId)
      const selected = {
        companyId: locks.company ? scopedDefaults.companyId : pickFirstValue(user, ["companyId", "company_id"]),
        departmentId: locks.department ? scopedDefaults.departmentId : pickFirstValue(user, ["departmentId", "department_id", "deptId"]),
        teamId: locks.team ? scopedDefaults.teamId : pickFirstValue(user, ["teamId", "team_id"])
      }
      const state = resolvePickerState(nodes, selected)
      if (!state) return
      const inspectionUnitOptions = organizationOptionsFor(nodes, undefined, ["GROUP", "COMPANY", "SCHOOL"])
      const inspectedUnitOptions = organizationOptionsFor(nodes, undefined, ["COMPANY", "SCHOOL"])
      const selectedInspectionUnit = inspectionUnitOptions[0]
      const selectedInspectedUnit = inspectedUnitOptions[0]
      const autoSelectTeam = !locks.department && !locks.team
      const firstTeam = state.selectedTeamId || !autoSelectTeam
        ? null
        : (state.teamOptions || []).find(option => option && option.value)
      this.setData({
        organizationNodes: nodes,
        organizationLocks: locks,
        inspectionUnitOptions,
        inspectedUnitOptions,
        selectedInspectionUnitId: selectedInspectionUnit ? selectedInspectionUnit.value : "",
        selectedInspectedUnitId: selectedInspectedUnit ? selectedInspectedUnit.value : "",
        currentInspectionUnitName: selectedInspectionUnit ? selectedInspectionUnit.label : "",
        currentInspectedUnitName: selectedInspectedUnit ? selectedInspectedUnit.label : "",
        inspectionUnitIndex: 0,
        inspectedUnitIndex: 0,
        ...state,
        selectedTeamId: firstTeam ? firstTeam.value : state.selectedTeamId,
        currentTeam: firstTeam ? firstTeam.label : state.currentTeam
      })
    } catch (error) {
      this.setData({ organizationNodes: [] })
    }
  },

  changeInspectionUnit(event = {}) {
    const index = Number(event.detail ? event.detail.value : 0)
    const unit = this.data.inspectionUnitOptions[index]
    if (!unit) return
    this.setData({
      selectedInspectionUnitId: unit.value,
      currentInspectionUnitName: unit.label,
      inspectionUnitIndex: index
    })
  },

  changeInspectedUnit(event = {}) {
    const index = Number(event.detail ? event.detail.value : 0)
    const unit = this.data.inspectedUnitOptions[index]
    if (!unit) return
    this.setData({
      selectedInspectedUnitId: unit.value,
      currentInspectedUnitName: unit.label,
      inspectedUnitIndex: index,
      selectedCompanyId: unit.value
    })
  },

  changeCompany(event = {}) {
    if (this.data.organizationLocks.company) return
    const index = Number(event.detail ? event.detail.value : 0)
    const company = this.data.companyOptions[index]
    if (!company) return
    const departmentOptions = organizationOptionsFor(this.data.organizationNodes, company.value, ["DEPARTMENT"])
    const department = departmentOptions[0]
    const teamOptions = department ? organizationOptionsFor(this.data.organizationNodes, department.value, ["TEAM"]) : []
    this.setData({
      selectedCompanyId: company.value,
      companyIndex: index,
      departmentOptions,
      departmentIndex: 0,
      selectedDepartmentId: department ? department.value : "",
      teamOptions,
      teamIndex: 0,
      selectedTeamId: "",
      currentCompany: {
        company: company.label,
        workshop: department ? department.label : ""
      },
      currentTeam: "",
      errorText: "",
      operationResult: ""
    })
  },

  changeDepartment(event = {}) {
    if (this.data.organizationLocks.department) return
    const index = Number(event.detail ? event.detail.value : 0)
    const department = this.data.departmentOptions[index]
    if (!department) return
    const teamOptions = organizationOptionsFor(this.data.organizationNodes, department.value, ["TEAM"])
    this.setData({
      selectedDepartmentId: department.value,
      departmentIndex: index,
      teamOptions,
      teamIndex: 0,
      selectedTeamId: "",
      currentCompany: {
        ...this.data.currentCompany,
        workshop: department.label
      },
      currentTeam: "",
      errorText: "",
      operationResult: ""
    })
  },

  changeUploadDate(event = {}) {
    const value = event.detail ? event.detail.value : ""
    if (!value) return
    this.setData({
      uploadDate: value,
      errorText: "",
      operationResult: ""
    })
  },

  changeCheckDate(event = {}) {
    const value = event.detail ? event.detail.value : ""
    if (!value) return
    const clock = this.data.checkClock || formatLocalClock(new Date())
    this.setData({
      form: {
        ...this.data.form,
        checkDate: value,
        checkTime: combineDateTime(value, clock)
      },
      errorText: "",
      operationResult: ""
    })
  },

  changeCheckTime(event = {}) {
    const value = event.detail ? event.detail.value : ""
    if (!value) return
    const checkDate = this.data.form.checkDate || today()
    this.setData({
      checkClock: value,
      form: {
        ...this.data.form,
        checkDate,
        checkTime: combineDateTime(checkDate, value)
      },
      errorText: "",
      operationResult: ""
    })
  },

  changeTeam(event = {}) {
    if (this.data.organizationLocks.team) return
    const index = Number(event.detail ? event.detail.value : 0)
    const team = this.data.teamOptions[index]
    if (!team) return
    this.setData({
      selectedTeamId: team.value,
      teamIndex: index,
      currentTeam: team.label,
      errorText: "",
      operationResult: ""
    })
  },

  changeAi(event = {}) {
    const value = event.currentTarget && event.currentTarget.dataset
      ? event.currentTarget.dataset.value
      : ""
    if (!value) return
    this.setData({
      form: {
        ...this.data.form,
        enableAi: value
      }
    })
  },

  changeSafetyFormTab(event = {}) {
    const tab = event.currentTarget && event.currentTarget.dataset
      ? event.currentTarget.dataset.tab
      : ""
    if (!tab) return
    this.setData({ safetyFormTab: tab })
  },

  addSafetyHazardCard() {
    const cards = this.data.safetyHazardCards || []
    const nextIndex = cards.reduce((max, item) => Math.max(max, Number(item.index) || 0), 0) + 1
    this.setData({
      safetyHazardCards: [
        ...cards,
        { id: `card-${nextIndex}`, index: nextIndex, collapsed: false }
      ]
    })
  },

  toggleSafetyHazardCard(event = {}) {
    const id = event.currentTarget && event.currentTarget.dataset
      ? event.currentTarget.dataset.id
      : ""
    if (!id) return
    this.setData({
      safetyHazardCards: (this.data.safetyHazardCards || []).map(item => (
        item.id === id
          ? { ...item, collapsed: !item.collapsed }
          : item
      ))
    })
  },

  deleteSafetyHazardCard(event = {}) {
    const id = event.currentTarget && event.currentTarget.dataset
      ? event.currentTarget.dataset.id
      : ""
    const cards = this.data.safetyHazardCards || []
    const nextCards = id ? cards.filter(item => item.id !== id) : []
    const patch = { safetyHazardCards: nextCards }
    if (nextCards.length === 0) {
      patch.form = {
        ...this.data.form,
        hazardDescription: "",
        rectificationMeasures: "",
        rectificationResponsiblePerson: "",
        rectificationDeadline: ""
      }
    }
    this.setData({
      ...patch
    })
  },

  toggleCheckMethod(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const value = dataset.value
    if (!value) return
    const current = this.data.form.checkMethods || []
    const exists = current.includes(value)
    const nextMethods = exists ? current.filter(item => item !== value) : [...current, value]
    this.setData({
      checkMethodOptions: (this.data.checkMethodOptions || []).map(item => ({
        ...item,
        checked: nextMethods.includes(item.label)
      })),
      form: {
        ...this.data.form,
        checkMethods: nextMethods
      }
    })
  },

  updateField(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const field = dataset.field
    if (!field) return
    this.setData({
      form: {
        ...this.data.form,
        [field]: event.detail ? event.detail.value : ""
      },
      errorText: "",
      operationResult: ""
    })
  },

  choosePhoto(sourceType) {
    const selectedSourceType = sourceType === "camera" || sourceType === "album"
      ? [sourceType]
      : ["album", "camera"]
    if (wx.chooseMedia) {
      wx.chooseMedia({
        count: this.data.moduleKey === "safety-check" ? 9 : 1,
        mediaType: this.data.moduleKey === "safety-check" ? ["image", "video"] : ["image"],
        sourceType: selectedSourceType,
        success: result => {
          const files = (result.tempFiles || []).map(normalizeAttachment).filter(Boolean)
          if (this.data.moduleKey === "safety-check") {
            this.setData({
              attachments: [...this.data.attachments, ...files],
              operationResult: ""
            })
            return
          }
          const file = files[0]
          if (file && file.path) {
            this.setData({ photo: file.path, operationResult: "" })
          }
        }
      })
      return
    }
    if (!wx.chooseImage) return
    wx.chooseImage({
      count: 1,
      sourceType: selectedSourceType,
      success: result => {
        const filePath = result.tempFilePaths && result.tempFilePaths[0]
        if (filePath) {
          if (this.data.moduleKey === "safety-check") {
            this.setData({
              attachments: [
                ...this.data.attachments,
                { type: "image", path: filePath, name: filenameFromPath(filePath) }
              ],
              operationResult: ""
            })
            return
          }
          this.setData({ photo: filePath, operationResult: "" })
        }
      }
    })
  },

  takePhoto() {
    this.choosePhoto("camera")
  },

  uploadPhoto() {
    this.choosePhoto("album")
  },

  previewAttachment(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const attachment = this.data.attachments[Number(dataset.index)]
    if (!attachment || attachment.type !== "image" || !wx.previewImage) return
    const urls = this.data.attachments
      .filter(item => item.type === "image")
      .map(item => item.path)
    wx.previewImage({
      current: attachment.path,
      urls
    })
  },

  previewPhoto() {
    if (!this.data.photo || !wx.previewImage) return
    wx.previewImage({
      current: this.data.photo,
      urls: [this.data.photo]
    })
  },

  buildPayload() {
    const user = this.data.currentUser || {}
    const form = this.data.form
    const common = {
      companyId: this.data.selectedCompanyId || pickFirstValue(user, ["companyId", "company_id"]),
      departmentId: this.data.selectedDepartmentId || pickFirstValue(user, ["departmentId", "department_id", "deptId"]),
      ownerUserId: normalizeId(pickFirstValue(user, ["id", "userId", "user_id"])),
      businessDate: this.data.uploadDate || today(),
      sourceRecordId: createSourceRecordId(this.data.moduleKey),
      clientRequestId: createClientRequestId(),
      clientUpdatedAt: createClientUpdatedAt()
    }

    if (this.data.moduleKey === "safety-check") {
      const inspectedUnit = this.data.inspectedUnitOptions[this.data.inspectedUnitIndex]
      const inspectionUnit = this.data.inspectionUnitOptions[this.data.inspectionUnitIndex]
      return {
        ...common,
        companyId: this.data.selectedInspectedUnitId || common.companyId,
        content: form.checkContent || form.checkTheme || "移动端安全检查",
        payload: {
          inspectionUnitId: this.data.selectedInspectionUnitId,
          inspectionUnitName: inspectionUnit ? inspectionUnit.label : "",
          inspectedUnitId: this.data.selectedInspectedUnitId,
          inspectedUnitName: inspectedUnit ? inspectedUnit.label : "",
          inspectedProject: form.inspectedProject,
          checkTheme: form.checkTheme,
          checkArea: form.checkArea,
          manualCheckType: form.manualCheckType,
          checkDate: form.checkDate || today(),
          checkTime: form.checkTime,
          creatorName: user.realName || user.username || "",
          checkMethods: form.checkMethods,
          checkMethod: (form.checkMethods || []).join("，"),
          manualInspectors: form.manualInspectors,
          manualInspectedUnitUsers: form.manualInspectedUnitUsers,
          checkContent: form.checkContent,
          remark: form.remark,
          checkItems: (this.data.safetyHazardCards || []).map(item => (
            item.collapsed ? null : {
              checkItem: form.checkItem,
              checkResult: "有隐患",
              hazardDescription: form.hazardDescription,
              rectificationMeasures: form.rectificationMeasures,
              rectificationResponsiblePerson: form.rectificationResponsiblePerson,
              rectificationDeadline: form.rectificationDeadline
            }
          )).filter(Boolean)
        }
      }
    }

    return {
      ...common,
      teamId: this.data.selectedTeamId || pickFirstValue(user, ["teamId", "team_id"]),
      content: form.hazardDescription || "移动端随手拍",
      payload: {
        hazardDescription: form.hazardDescription,
        location: form.location,
        photo: this.data.photo,
        rectificationMeasures: form.rectificationMeasures,
        enableAi: form.enableAi
      }
    }
  },

  hasOrganizationSelection() {
    const user = this.data.currentUser || {}
    return Boolean(
      (this.data.selectedCompanyId || pickFirstValue(user, ["companyId", "company_id"])) &&
      (this.data.selectedDepartmentId || pickFirstValue(user, ["departmentId", "department_id", "deptId"])) &&
      (this.data.selectedTeamId || pickFirstValue(user, ["teamId", "team_id"]))
    )
  },

  validateForm() {
    const form = this.data.form
    if (this.data.moduleKey === "safety-check") {
      if (!this.data.selectedInspectionUnitId || !this.data.selectedInspectedUnitId) {
        this.setData({ errorText: "请选择检查单位和受检单位" })
        return false
      }
      if (!form.checkTheme || !(form.checkContent || form.hazardDescription)) {
        this.setData({ errorText: "请填写检查类型和检查内容" })
        return false
      }
      return true
    }
    if (!this.hasOrganizationSelection()) {
      this.setData({ errorText: "请先选择公司、车间、班组" })
      return false
    }
    if (!form.hazardDescription) {
      this.setData({ errorText: "请填写隐患描述" })
      return false
    }
    if (this.data.moduleKey === "safety-check" && (!form.checkTheme || !form.checkArea || !form.checkItem)) {
      this.setData({ errorText: "请填写检查主题、区域和检查项" })
      return false
    }
    return true
  },

  async submitForm() {
    if (this.data.submitting) return
    if (!this.validateForm()) return
    this.setData({ submitting: true, errorText: "", operationResult: "" })
    try {
      const created = await createHazardRecord(this.data.moduleKey, this.buildPayload())
      const id = created && (created.id || created.recordId)
      if (id && this.data.photo) {
        await uploadHazardAttachment(this.data.moduleKey, id, "IMAGE", this.data.photo)
      }
      if (id && this.data.moduleKey === "safety-check") {
        for (const attachment of this.data.attachments) {
          await uploadHazardAttachment(
            this.data.moduleKey,
            id,
            attachment.type === "video" ? "VIDEO" : "IMAGE",
            attachment.path
          )
        }
      }
      if (id && this.data.moduleKey === "safety-check") {
        await submitHazardRecord(this.data.moduleKey, id)
      }
      this.setData({ submitting: false, operationResult: "已提交" })
      if (wx.showToast) {
        wx.showToast({ title: "已提交", icon: "success" })
      }
      if (id && this.data.moduleKey !== "safety-check" && wx.navigateTo) {
        wx.navigateTo({
          url: appendQuery("/pages/hazard/source-detail/index", {
            id,
            moduleKey: this.data.moduleKey,
            title: this.data.title,
            companyKey: this.data.companyKey
          })
        })
      }
    } catch (error) {
      this.setData({
        submitting: false,
        errorText: error && error.message ? error.message : "提交失败"
      })
    }
  },

  saveDraft() {
    if (wx.showToast) {
      wx.showToast({ title: "草稿已保留", icon: "success" })
    }
  },

  nextStep() {
    return this.submitForm()
  },

  goBack() {
    if (wx.navigateBack) {
      wx.navigateBack()
    }
  }
})
