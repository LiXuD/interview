const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: { providers: [], loading: true, error: '' },
  onLoad() { this.loadProviders(); },

  loadProviders() {
    this.setData({ loading: true });
    request.get(API.AI_PROVIDERS)
      .then((data) => { this.setData({ providers: Array.isArray(data) ? data : [] }); })
      .catch((err) => { this.setData({ error: err.message || '加载失败' }); })
      .finally(() => { this.setData({ loading: false }); });
  },

  onSetDefault(e) {
    const id = e.currentTarget.dataset.id;
    request.post(API.AI_PROVIDER_SET_DEFAULT(id))
      .then(() => { wx.showToast({ title: '已设置' }); this.loadProviders(); })
      .catch((err) => { this.setData({ error: err.message || '设置失败' }); });
  },

  onDelete(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '删除后 API Key 将被清除',
      success: (res) => {
        if (res.confirm) {
          request.del(API.AI_PROVIDER_DELETE(id))
            .then(() => { wx.showToast({ title: '已删除' }); this.loadProviders(); })
            .catch((err) => { this.setData({ error: err.message || '删除失败' }); });
        }
      }
    });
  },

  onCreate() {
    wx.showToast({ title: '即将实现', icon: 'none' });
  }
});
