const pointsService = require("../../../services/safety-points")

function getWx() {
  return wx
}

Page({
  data: {
    loading: false,
    records: [],
    total: 0
  },

  onLoad() {
    return this.loadRecords()
  },

  async loadRecords() {
    this.setData({ loading: true })
    try {
      const result = await pointsService.listPointFlow({ page: 1, pageSize: 20 })
      this.setData({ records: result.items || [], total: result.total || 0 })
    } finally {
      this.setData({ loading: false })
    }
  },

  goBack() {
    getWx().navigateBack()
  }
})
