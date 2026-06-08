const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: { summary: null, byTask: [], byModel: [], loading: true, error: '' },
  onLoad() {
    Promise.all([
      request.get(API.AI_USAGE_SUMMARY),
      request.get(API.AI_USAGE_BY_TASK),
      request.get(API.AI_USAGE_BY_MODEL)
    ])
      .then(([summary, byTask, byModel]) => {
        this.setData({
          summary,
          byTask: Array.isArray(byTask) ? byTask : [],
          byModel: Array.isArray(byModel) ? byModel : []
        });
      })
      .catch((err) => { this.setData({ error: err.message || '加载失败' }); })
      .finally(() => { this.setData({ loading: false }); });
  }
});
