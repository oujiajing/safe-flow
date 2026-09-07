const { getRecord } = require("../../../services/mini-three-check")
const { mapDispatchRecordToRow } = require("../../../mappers/dispatch")

Page({
  data: {
    id: "",
    companyKey: "gs",
    dispatchRecord: null,
    loading: false,
    errorText: ""
  },

  async onLoad(options = {}) {
    this.setData({
      id: options.id || "",
      companyKey: options.companyKey || "gs"
    })
    await this.loadRecord()
  },

  async loadRecord() {
    if (!this.data.id) return
    this.setData({ loading: true, errorText: "" })
    try {
      const record = await getRecord("team-dispatch", this.data.id)
      this.setData({
        dispatchRecord: mapDispatchRecordToRow(record),
        loading: false
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorText: error && error.message ? error.message : "派班详情加载失败"
      })
    }
  },

  goBack() {
    wx.navigateBack()
  }
})
