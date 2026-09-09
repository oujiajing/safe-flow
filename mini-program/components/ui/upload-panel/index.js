Component({
  properties: {
    photo: {
      type: String,
      value: ""
    },
    emptyText: {
      type: String,
      value: "请上传"
    },
    previewText: {
      type: String,
      value: "1/1"
    }
  },

  methods: {
    upload() {
      this.triggerEvent("upload")
    },
    preview() {
      this.triggerEvent("preview")
    }
  }
})
