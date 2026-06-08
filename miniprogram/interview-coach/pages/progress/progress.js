const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: { targetId: '', data: null, loading: true, error: '' },
  onLoad(options) {
    this.setData({ targetId: options.targetId || '' });
    request.get(API.PROGRESS, { targetId: this.data.targetId })
      .then((data) => { this.setData({ data }); })
      .catch((err) => { this.setData({ error: err.message || '加载失败' }); })
      .finally(() => { this.setData({ loading: false }); });
  }
});
