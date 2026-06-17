const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: { title: '', jd: '', submitting: false, error: '' },

  onTitleInput(e) { this.setData({ title: e.detail.value, error: '' }); },
  onJdInput(e) { this.setData({ jd: e.detail.value, error: '' }); },

  onSubmit() {
    const { title, jd, submitting } = this.data;
    if (submitting) return;
    if (!title.trim() || !jd.trim()) return;
    this.setData({ submitting: true, error: '' });
    request.post(API.TARGETS, { title: title.trim(), jd: jd.trim() })
      .then((target) => {
        wx.showToast({ title: '创建成功' });
        wx.navigateTo({
          url: '/package-coach/pages/profile/profile?targetId=' + target.id
        });
      })
      .catch((err) => {
        this.setData({ error: err.message || '创建失败' });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  }
});
