Component({
  properties: {
    loading: {
      type: Boolean,
      value: false
    },
    errorText: {
      type: String,
      value: ""
    },
    empty: {
      type: Boolean,
      value: false
    },
    loadingText: {
      type: String,
      value: "正在加载..."
    },
    emptyText: {
      type: String,
      value: "暂无数据"
    }
  },

  methods: {
    retry() {
      this.triggerEvent("retry")
    }
  }
})
