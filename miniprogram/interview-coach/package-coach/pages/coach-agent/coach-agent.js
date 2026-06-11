const request = require('../../../utils/request');
const API = require('../../../utils/api');

Page({
  data: { targetId: '', agent: null, loading: true, error: '' },
  onLoad(options) {
    this.setData({ targetId: options.targetId || '' });
    request.get(API.COACH_AGENT(this.data.targetId))
      .then((agent) => { this.setData({ agent }); })
      .catch((err) => { this.setData({ error: err.message || '加载失败' }); })
      .finally(() => { this.setData({ loading: false }); });
  }
});
