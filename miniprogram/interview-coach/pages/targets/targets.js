/**
 * 目标岗位列表页。
 */
const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: {
    targets: [],
    loading: true,
    error: ''
  },

  onShow() {
    this.loadTargets();
  },

  loadTargets() {
    this.setData({ loading: true, error: '' });
    request.get(API.TARGETS)
      .then((data) => {
        this.setData({ targets: Array.isArray(data) ? data : [] });
      })
      .catch((err) => {
        this.setData({ error: err.message || '加载失败' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  onCreateTarget() {
    wx.navigateTo({ url: '/pages/target-create/target-create' });
  },

  onTargetTap(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/profile/profile?targetId=' + id });
  },

  goSettings() {
    wx.navigateTo({ url: '/pages/settings/settings' });
  }
});
