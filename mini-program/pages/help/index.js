const FAQS = [
  {
    question: "为什么看不到部分工作记录？",
    answer: "个人中心仅展示当前账号参与的业务记录。若记录缺失，请先确认派班、检查或整改任务中的人员信息是否为当前账号。"
  },
  {
    question: "安全积分多久更新？",
    answer: "积分产生后会同步到个人安全概览。下拉刷新页面即可查看最新积分。"
  },
  {
    question: "个人资料如何修改？",
    answer: "姓名、组织、班组和岗位来自系统组织架构，请联系本单位系统管理员维护。"
  }
]

Page({
  data: { faqs: FAQS, expandedIndex: -1 },

  toggleFaq(event) {
    const index = Number(event.currentTarget.dataset.index)
    this.setData({ expandedIndex: this.data.expandedIndex === index ? -1 : index })
  },

  copySupportInfo() {
    wx.setClipboardData({
      data: "平安班组小程序问题反馈",
      success() {
        wx.showToast({ title: "反馈标题已复制", icon: "success" })
      }
    })
  },

  goBack() {
    wx.navigateBack()
  }
})

module.exports = { FAQS }
