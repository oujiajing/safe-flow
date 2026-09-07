const { createSpecialWorkRecord, getSpecialWorkRecord, updateSpecialWorkRecord, uploadSpecialWorkImage } = require("../../../services/special-work")
const { getOrgTree } = require("../../../services/organization")
const { organizationOptionsFor } = require("../../../utils/organization-options")
const { appendQuery } = require("../../../utils/module-routes")
const { dateTimeParts, mergeDateTimePart, nowDateTime, recordPayload } = require("../../../utils/special-work")

const WORK_TYPES = ["动火作业", "受限空间作业", "高处作业", "吊装作业", "临时用电作业", "动土作业", "断路作业", "盲板抽堵作业", "其他"]
const DATE_TIME_FIELDS = ["applicationTime", "implementationStartTime", "implementationEndTime", "completionAcceptanceTime"]

function emptyForm() {
  return recordPayload({ applicationTime: nowDateTime(), workType: WORK_TYPES[0], status: "PENDING_APPROVAL" })
}

function dateTimesFor(form) {
  return DATE_TIME_FIELDS.reduce((memo, field) => {
    memo[field] = dateTimeParts(form[field])
    return memo
  }, {})
}

Page({
  data: {
    id: "", step: 0, steps: ["申请信息", "作业实施", "人员验收"],
    workTypes: WORK_TYPES, workTypeIndex: 0,
    companyOptions: [], companyIndex: 0,
    form: emptyForm(), dateTimes: dateTimesFor(emptyForm()),
    existingImage: null, selectedImagePath: "", saving: false, errorText: ""
  },

  async onLoad(options = {}) {
    const id = options.id || ""
    this.setData({ id })
    if (wx.setNavigationBarTitle) wx.setNavigationBarTitle({ title: id ? "编辑特种作业" : "新建特种作业" })
    await this.loadData()
  },

  goBack() { wx.navigateBack() },

  async loadData() {
    this.setData({ errorText: "" })
    try {
      const [nodes, record] = await Promise.all([getOrgTree(), this.data.id ? getSpecialWorkRecord(this.data.id) : Promise.resolve(null)])
      const companyOptions = organizationOptionsFor(nodes, undefined, ["COMPANY"])
      const form = record ? recordPayload(record) : emptyForm()
      const companyIndex = Math.max(0, companyOptions.findIndex(option => String(option.value) === String(form.companyId)))
      const selectedCompany = companyOptions[companyIndex]
      if (!form.companyId && selectedCompany) form.companyId = selectedCompany.value
      const workTypeIndex = Math.max(0, WORK_TYPES.indexOf(form.workType))
      this.setData({ companyOptions, companyIndex, form, dateTimes: dateTimesFor(form), workTypeIndex, existingImage: record && record.image ? record.image : null })
    } catch (error) {
      this.setData({ errorText: error && error.message ? error.message : "表单数据加载失败" })
    }
  },

  changeCompany(event) {
    const companyIndex = Number(event.detail.value)
    const company = this.data.companyOptions[companyIndex]
    if (company) this.setData({ companyIndex, form: { ...this.data.form, companyId: company.value }, errorText: "" })
  },

  changeWorkType(event) {
    const workTypeIndex = Number(event.detail.value)
    this.setData({ workTypeIndex, form: { ...this.data.form, workType: WORK_TYPES[workTypeIndex] }, errorText: "" })
  },

  updateField(event) {
    const field = event.currentTarget.dataset.field
    this.setData({ form: { ...this.data.form, [field]: event.detail.value }, errorText: "" })
  },

  changeDateTime(event) {
    const { field, part } = event.currentTarget.dataset
    const value = mergeDateTimePart(this.data.form[field], part, event.detail.value)
    const form = { ...this.data.form, [field]: value }
    this.setData({ form, dateTimes: dateTimesFor(form), errorText: "" })
  },

  chooseImage() {
    const apply = path => this.setData({ selectedImagePath: path, errorText: "" })
    if (wx.chooseMedia) {
      wx.chooseMedia({ count: 1, mediaType: ["image"], sourceType: ["album", "camera"], success: result => { const file = (result.tempFiles || [])[0]; if (file) apply(file.tempFilePath || file.path) } })
      return
    }
    if (wx.chooseImage) wx.chooseImage({ count: 1, sourceType: ["album", "camera"], success: result => apply((result.tempFilePaths || [])[0]) })
  },

  previewImage() {
    const url = this.data.selectedImagePath || (this.data.existingImage && this.data.existingImage.url)
    if (url && wx.previewImage) wx.previewImage({ current: url, urls: [url] })
  },

  handleImageTap() {
    if (this.data.selectedImagePath || this.data.existingImage) this.previewImage()
    else this.chooseImage()
  },

  validate() {
    const form = this.data.form
    if (!form.companyId) return "请选择公司"
    if (!String(form.project || "").trim()) return "请填写项目"
    if (!String(form.workType || "").trim()) return "请选择作业类型"
    if (!form.applicationTime) return "请选择作业申请时间"
    return ""
  },

  previousStep() { this.setData({ step: Math.max(0, this.data.step - 1), errorText: "" }) },

  handleBackStep() {
    if (this.data.step === 0) this.goBack()
    else this.previousStep()
  },

  nextStep() {
    const errorText = this.data.step === 0 ? this.validate() : ""
    if (errorText) return this.setData({ errorText })
    this.setData({ step: Math.min(2, this.data.step + 1), errorText: "" })
  },

  async submit() {
    if (this.data.saving) return
    const errorText = this.validate()
    if (errorText) return this.setData({ errorText })
    this.setData({ saving: true, errorText: "" })
    try {
      const payload = recordPayload(this.data.form)
      const record = this.data.id
        ? await updateSpecialWorkRecord(this.data.id, payload)
        : await createSpecialWorkRecord(payload)
      if (this.data.selectedImagePath) await uploadSpecialWorkImage(record.id, this.data.selectedImagePath)
      this.setData({ saving: false })
      if (wx.showToast) wx.showToast({ title: this.data.id ? "作业票已更新" : "申请已提交", icon: "success" })
      wx.redirectTo({ url: appendQuery("/pages/special-work/detail/index", { id: record.id }) })
    } catch (error) {
      this.setData({ saving: false, errorText: error && error.message ? error.message : "保存失败" })
    }
  }
})

module.exports = { DATE_TIME_FIELDS, WORK_TYPES, dateTimesFor, emptyForm }
