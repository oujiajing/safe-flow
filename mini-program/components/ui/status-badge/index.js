Component({
  properties: {
    status: {
      type: String,
      value: ""
    },
    label: {
      type: String,
      value: ""
    },
    icon: {
      type: Boolean,
      value: false
    },
    size: {
      type: String,
      value: "medium"
    }
  },

  data: {
    resolvedLabel: "-",
    tone: "default",
    iconName: "time"
  },

  observers: {
    "status, label": function () {
      this.updateBadge()
    }
  },

  lifetimes: {
    attached() {
      this.updateBadge()
    }
  },

  methods: {
    updateBadge() {
      const status = this.data.status || ""
      const labelMap = {
        PENDING_REVIEW: "待审批",
        REVIEWED: "已上报",
        REJECTED: "不通过",
        ACCEPTED: "已关闭"
      }
      const toneMap = {
        PENDING_REVIEW: "pending",
        REVIEWED: "success",
        REJECTED: "danger",
        ACCEPTED: "muted"
      }
      const iconMap = {
        PENDING_REVIEW: "time",
        REVIEWED: "check-circle",
        REJECTED: "close-circle",
        ACCEPTED: "minus-circle"
      }
      this.setData({
        resolvedLabel: this.data.label || labelMap[status] || status || "-",
        tone: toneMap[status] || "default",
        iconName: iconMap[status] || "time"
      })
    }
  }
})
