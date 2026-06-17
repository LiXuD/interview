const request = require('../../../utils/request');
const API = require('../../../utils/api');

Page({
  data: {
    reportId: '',
    targetId: '',
    report: null,
    loading: true,
    error: ''
  },

  onLoad(options) {
    this.setData({
      reportId: options.id || '',
      targetId: options.targetId || ''
    });
    if (this.data.reportId) {
      this.loadReport();
    } else if (this.data.targetId) {
      this.loadLatestReport(options.type || 'mockInterview');
    }
  },

  loadReport() {
    request.get(API.REPORT_DETAIL(this.data.reportId))
      .then((report) => { this.setData({ report }); })
      .catch((err) => { this.setData({ error: err.message || '加载失败' }); })
      .finally(() => { this.setData({ loading: false }); });
  },

  loadLatestReport(type) {
    request.get(API.REPORTS, { targetId: this.data.targetId })
      .then((reports) => {
        const list = Array.isArray(reports) ? reports : [];
        const match = list.find((r) => r.type === type) || list[0];
        if (match) {
          this.setData({ report: match, reportId: match.id });
        } else {
          this.setData({ error: '暂无报告' });
        }
      })
      .catch((err) => { this.setData({ error: err.message || '加载失败' }); })
      .finally(() => { this.setData({ loading: false }); });
  },

  goBack() {
    wx.reLaunch({ url: '/pages/targets/targets' });
  },

  onShareAppMessage() {
    const report = this.data.report;
    const score = report ? (report.overallScore || report.totalScore || '') : '';
    const title = score ? '我在 AI 面试教练获得了 ' + score + ' 分' : '来看看我的面试测评报告';
    return {
      title,
      path: '/pages/targets/targets'
    };
  }
});
