const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: {
    targetId: '',
    step: 'input', // 'input' | 'confirm'
    resumeText: '',
    draft: null,
    loading: false,
    error: ''
  },

  onLoad(options) {
    this.setData({ targetId: options.targetId || '' });
  },

  onResumeInput(e) { this.setData({ resumeText: e.detail.value, error: '' }); },
  onSummaryInput(e) {
    const draft = { ...this.data.draft, summary: e.detail.value };
    this.setData({ draft, error: '' });
  },

  onGenerate() {
    const resumeText = this.data.resumeText.trim();
    if (!resumeText) return;
    this.setData({ loading: true, error: '' });
    request.post(API.PROFILE_DRAFT_SUMMARY, { resumeText })
      .then((draft) => {
        this.setData({ draft, step: 'confirm' });
      })
      .catch((err) => {
        this.setData({ error: err.message || '生成摘要失败' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  onConfirm() {
    const { targetId, draft } = this.data;
    if (!draft) return;
    this.setData({ loading: true, error: '' });
    request.post(API.PROFILE_CONFIRM, {
      targetId,
      summary: draft.summary,
      skills: draft.skills || [],
      projects: draft.projects || [],
      experience: draft.experience || []
    })
      .then(() => {
        wx.showToast({ title: '已确认' });
        this._navTimer = setTimeout(() => {
          wx.redirectTo({ url: '/pages/jobbrief/jobbrief?targetId=' + targetId });
        }, 500);
      })
      .catch((err) => {
        this.setData({ error: err.message || '确认失败' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  onUnload() {
    if (this._navTimer) clearTimeout(this._navTimer);
  }
});
