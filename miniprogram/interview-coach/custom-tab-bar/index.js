Component({
  data: {
    selected: 0,
    color: "#999999",
    selectedColor: "#1a1a2e",
    list: [
      {
        pagePath: "/pages/targets/targets",
        text: "目标",
        iconPath: "",
        selectedIconPath: ""
      },
      {
        pagePath: "/pages/settings/settings",
        text: "设置",
        iconPath: "",
        selectedIconPath: ""
      }
    ]
  },

  methods: {
    switchTab(e) {
      const data = e.currentTarget.dataset;
      const url = data.path;
      wx.switchTab({ url });
    }
  }
});
