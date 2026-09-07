Component({
  options: {
    multipleSlots: true
  },

  properties: {
    title: {
      type: String,
      value: ""
    },
    icon: {
      type: String,
      value: ""
    },
    type: {
      type: String,
      value: ""
    },
    showAccent: {
      type: Boolean,
      value: false
    },
    extra: {
      type: Boolean,
      value: false
    },
    size: {
      type: String,
      value: "medium"
    }
  },

  data: {
    resolvedIcon: "file-paste",
    sizeClass: "medium",
    iconSize: "22rpx"
  },

  observers: {
    "icon, type, size": function () {
      this.updateHeader()
    }
  },

  lifetimes: {
    attached() {
      this.updateHeader()
    }
  },

  methods: {
    updateHeader() {
      const size = this.normalizeSize(this.data.size)
      this.setData({
        resolvedIcon: this.data.icon || this.resolveIcon(this.data.type),
        sizeClass: size,
        iconSize: this.resolveIconSize(size)
      })
    },

    normalizeSize(size) {
      return ["small", "medium", "large"].indexOf(size) >= 0 ? size : "medium"
    },

    resolveIconSize(size) {
      const sizeMap = {
        small: "20rpx",
        medium: "22rpx",
        large: "24rpx"
      }
      return sizeMap[size] || sizeMap.medium
    },

    resolveIcon(type) {
      const iconMap = {
        order: "file-paste",
        hazard: "shield-error",
        flow: "history",
        dispatch: "send",
        submit: "upload",
        accept: "assignment-checked",
        close: "check-circle",
        cancel: "close-circle"
      }
      return iconMap[type] || "file-paste"
    }
  }
})
