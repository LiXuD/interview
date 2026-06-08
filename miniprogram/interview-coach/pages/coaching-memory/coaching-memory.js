const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: { targetId: '', memories: [], loading: true, error: '' },
  onLoad(options) {
    this.setData({ targetId: options.targetId || '' });
    this.loadMemories();
  },

  loadMemories() {
    this.setData({ loading: true });
    request.get(API.COACHING_MEMORIES(this.data.targetId))
      .then((data) => { this.setData({ memories: Array.isArray(data) ? data : [] }); })
      .catch((err) => { this.setData({ error: err.message || '加载失败' }); })
      .finally(() => { this.setData({ loading: false }); });
  },

  onCorrect(e) {
    const id = e.currentTarget.dataset.id;
    const source = e.currentTarget.dataset.source;
    const content = source === 'rejected' ? '标记为无效' : '请描述正确信息';

    if (source === 'rejected') {
      request.patch(API.COACHING_MEMORY_CORRECT(id), { source: 'rejected' })
        .then(() => { wx.showToast({ title: '已标记' }); this.loadMemories(); })
        .catch((err) => { this.setData({ error: err.message || '操作失败' }); });
    } else {
      wx.showModal({
        title: '纠错',
        editable: true,
        placeholderText: '输入正确信息',
        success: (res) => {
          if (res.confirm && res.content) {
            request.patch(API.COACHING_MEMORY_CORRECT(id), { source: 'corrected', correction: res.content })
              .then(() => { wx.showToast({ title: '已纠正' }); this.loadMemories(); })
              .catch((err) => { this.setData({ error: err.message || '操作失败' }); });
          }
        }
      });
    }
  }
});
