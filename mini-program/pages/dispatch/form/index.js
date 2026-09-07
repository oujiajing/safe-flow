const { createRecord, submitRecord } = require("../../../services/mini-three-check")
const { getDispatchFlow } = require("../../../services/flow")
const { getOrgTree } = require("../../../services/organization")
const { removeDraft, saveDraft } = require("../../../utils/draft")
const { createClientRequestId, createClientUpdatedAt, createSourceRecordId } = require("../../../utils/idempotency")
const { resolvePickerState } = require("../../../utils/organization-options")

const MODULE_KEY = "team-dispatch"

function todayString() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, "0")
  const day = String(now.getDate()).padStart(2, "0")
  return `${now.getFullYear()}-${month}-${day}`
}

Page({
  data: {
    companyKey: "gs",
    currentCompany: { company: "广晟", workshop: "幕墙车间" },
    currentTeam: "幕墙组装2班",
    organizationNodes: [],
    companyOptions: [],
    departmentOptions: [],
    teamOptions: [],
    selectedCompanyId: "",
    selectedDepartmentId: "",
    selectedTeamId: "",
    companyIndex: 0,
    departmentIndex: 0,
    teamIndex: 0,
    dispatchDate: "",
    dispatchTaskText: "",
    dispatchTaskLength: 0,
    dispatchType: "",
    dispatchTypeOptions: ["今日", "明日"],
    dispatchTypeIndex: 0,
    managementCount: "0",
    dispatchRemark: "",
    dispatchRemarkLength: 0,
    dispatchDraftName: "",
    operationResult: "",
    showDispatchDraft: false,
    submitting: false,
    sourceRecordId: "",
    generatedThreeCheckCount: 0
  },

  onLoad(options = {}) {
    this.setData({
      companyKey: options.companyKey || "gs",
      dispatchDate: todayString(),
      sourceRecordId: createSourceRecordId(MODULE_KEY)
    })
    this.loadOrganizationOptions()
  },

  async loadOrganizationOptions() {
    try {
      const nodes = await getOrgTree()
      const state = resolvePickerState(nodes)
      if (!state) {
        return
      }
      this.setData({
        organizationNodes: nodes,
        ...state
      })
    } catch (error) {
      // Keep prototype defaults if the organization endpoint is temporarily unavailable.
    }
  },

  applyOrganizationSelection(selection) {
    const state = resolvePickerState(this.data.organizationNodes, selection)
    if (state) {
      this.setData(state)
    }
  },

  changeDispatchCompany(event) {
    const index = Number(event.detail.value)
    const company = this.data.companyOptions[index]
    if (!company) return
    this.applyOrganizationSelection({ companyId: company.value })
  },

  changeDispatchDepartment(event) {
    const index = Number(event.detail.value)
    const department = this.data.departmentOptions[index]
    if (!department) return
    this.applyOrganizationSelection({
      companyId: this.data.selectedCompanyId,
      departmentId: department.value
    })
  },

  changeDispatchTeam(event) {
    const index = Number(event.detail.value)
    const team = this.data.teamOptions[index]
    if (!team) return
    this.applyOrganizationSelection({
      companyId: this.data.selectedCompanyId,
      departmentId: this.data.selectedDepartmentId,
      teamId: team.value
    })
  },

  changeDispatchDate(event) {
    this.setData({ dispatchDate: event.detail.value })
  },

  updateDispatchTaskText(event) {
    const value = event.detail.value || ""
    this.setData({ dispatchTaskText: value, dispatchTaskLength: value.length })
  },

  changeDispatchType(event) {
    const index = Number(event.detail.value)
    this.setData({
      dispatchTypeIndex: index,
      dispatchType: this.data.dispatchTypeOptions[index] || "今日"
    })
  },

  updateManagementCount(event) {
    this.setData({ managementCount: event.detail.value })
  },

  updateDispatchRemark(event) {
    const value = event.detail.value || ""
    this.setData({ dispatchRemark: value, dispatchRemarkLength: value.length })
  },

  openDispatchDraft() {
    this.setData({ showDispatchDraft: true })
  },

  closeDispatchDraft() {
    this.setData({ showDispatchDraft: false })
  },

  updateDispatchDraftName(event) {
    this.setData({ dispatchDraftName: event.detail.value })
  },

  saveDispatchDraft() {
    const dispatchDraftName = this.data.dispatchDraftName || "班组派班草稿"
    this.setData({ showDispatchDraft: false, dispatchDraftName })
    saveDraft(MODULE_KEY, this.data.sourceRecordId, {
      companyId: this.data.selectedCompanyId,
      departmentId: this.data.selectedDepartmentId,
      teamId: this.data.selectedTeamId,
      dispatchDate: this.data.dispatchDate,
      dispatchTaskText: this.data.dispatchTaskText || "班组派班",
      dispatchType: this.data.dispatchType || "今日",
      managementCount: this.data.managementCount,
      dispatchRemark: this.data.dispatchRemark,
      dispatchDraftName
    })
    wx.showToast({ title: "草稿已保存", icon: "success" })
  },

  deleteDispatchDraft() {
    this.setData({ showDispatchDraft: false, dispatchDraftName: "" })
    removeDraft(MODULE_KEY, this.data.sourceRecordId)
    wx.showToast({ title: "草稿已删除", icon: "none" })
  },

  goBack() {
    const pages = typeof getCurrentPages === "function" ? getCurrentPages() : []
    if (pages.length > 1) {
      wx.navigateBack()
      return
    }
    wx.switchTab({ url: "/pages/home/home" })
  },

  buildPayload() {
    return {
      businessDate: this.data.dispatchDate,
      companyId: this.data.selectedCompanyId,
      departmentId: this.data.selectedDepartmentId,
      teamId: this.data.selectedTeamId,
      content: "班组派班",
      payload: {
        companyName: this.data.currentCompany.company,
        departmentName: this.data.currentCompany.workshop,
        teamName: this.data.currentTeam,
        task: this.data.dispatchTaskText || "班组派班",
        dispatchType: this.data.dispatchType || "今日",
        managementCount: this.data.managementCount || "0",
        remark: this.data.dispatchRemark
      },
      sourceRecordId: this.data.sourceRecordId,
      clientRequestId: createClientRequestId(),
      clientUpdatedAt: createClientUpdatedAt()
    }
  },

  hasOrganizationSelection() {
    return Boolean(this.data.selectedCompanyId && this.data.selectedDepartmentId && this.data.selectedTeamId)
  },

  async submitDispatch() {
    if (this.data.submitting) return
    if (this.data.selectedCompanyId && this.data.selectedDepartmentId && !this.data.selectedTeamId) {
      this.setData({ submitting: false, operationResult: "请先选择班组" })
      return
    }
    if (!this.hasOrganizationSelection()) {
      this.setData({ submitting: false, operationResult: "请先选择公司、车间、班组" })
      return
    }
    this.setData({ submitting: true, operationResult: "" })
    try {
      const created = await createRecord(MODULE_KEY, this.buildPayload())
      const id = created.id || created.recordId
      await submitRecord(MODULE_KEY, id)
      const generatedThreeCheckCount = await this.loadGeneratedThreeCheckCount(id)
      const operationResult = generatedThreeCheckCount > 0
        ? `派班成功，已生成${generatedThreeCheckCount}个一班三查任务`
        : "派班成功"
      this.setData({
        submitting: false,
        operationResult,
        generatedThreeCheckCount
      })
      removeDraft(MODULE_KEY, this.data.sourceRecordId)
      wx.showToast({ title: "派班成功", icon: "success" })
    } catch (error) {
      this.setData({
        submitting: false,
        operationResult: error && error.message ? error.message : "派班提交失败"
      })
    }
  },

  async loadGeneratedThreeCheckCount(id) {
    try {
      const flow = await getDispatchFlow(id)
      const stages = Array.isArray(flow && flow.stages) ? flow.stages : []
      return stages.filter(stage => stage && stage.stageKey !== "teamDispatch" && stage.record).length
    } catch (error) {
      return 0
    }
  }
})
