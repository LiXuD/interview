const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: {
    targetId: '',
    brief: null,
    loading: false,
    loadingText: '加载中...',
    error: ''
  },

  onLoad(options) {
    this.setData({ targetId: options.targetId || '' });
    this.loadBrief();
  },

  loadBrief() {
    this.setData({ loading: true, loadingText: '加载中...' });
    request.get(API.JOB_BRIEF(this.data.targetId))
      .then((brief) => {
        this.setData({ brief });
      })
      .catch((err) => {
        // 404 表示尚未生成，不算错误
        if (err.statusCode === 404) {
          this.setData({ brief: null });
        } else {
          this.setData({ error: err.message || '加载失败' });
        }
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  onGenerate() {
    this.setData({ error: '', loading: true, loadingText: 'AI 正在分析岗位...' });
    request.post(API.JOB_BRIEF_GENERATE, { targetId: this.data.targetId })
      .then((brief) => {
        this.setData({ brief });
        wx.showToast({ title: '生成成功' });
      })
      .catch((err) => {
        this.setData({ error: err.message || '生成失败' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  goAssessment() {
    wx.redirectTo({ url: '/pages/assessment/assessment?targetId=' + this.data.targetId });
  }
});
