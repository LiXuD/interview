/**
 * 目标岗位详情页：查看、编辑、删除。
 */
const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: {
    id: '',
    title: '',
    jd: '',
    status: '',
    createdAt: '',
    loading: true,
    error: '',
    editing: false,
    editTitle: '',
    editJd: '',
    saving: false,
    deleting: false,
    showDeleteConfirm: false
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ id: options.id });
      this.loadTarget();
    } else {
      this.setData({ loading: false, error: '缺少目标岗位 ID' });
    }
  },

  loadTarget() {
    this.setData({ loading: true, error: '' });
    return request.get(API.TARGET_DETAIL(this.data.id))
      .then((data) => {
        this.setData({
          title: data.title || '',
          jd: data.jd || '',
          status: data.status || '',
          createdAt: data.createdAt || ''
        });
      })
      .catch((err) => {
        this.setData({ error: err.message || '加载失败' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  onEdit() {
    this.setData({
      editing: true,
      editTitle: this.data.title,
      editJd: this.data.jd
    });
  },

  onCancelEdit() {
    this.setData({ editing: false });
  },

  onEditTitleInput(e) { this.setData({ editTitle: e.detail.value }); },
  onEditJdInput(e) { this.setData({ editJd: e.detail.value }); },

  onSave() {
    const { editTitle, editJd, title, jd } = this.data;
    const newTitle = editTitle.trim();
    const newJd = editJd.trim();
    if (!newTitle || !newJd) return;
    if (newTitle === title && newJd === jd) {
      this.setData({ editing: false });
      return;
    }
    this.setData({ saving: true });
    request.patch(API.TARGET_DETAIL(this.data.id), { title: newTitle, jd: newJd })
      .then(() => {
        this.setData({ title: newTitle, jd: newJd, editing: false });
        wx.showToast({ title: '已保存' });
      })
      .catch((err) => {
        wx.showToast({ title: err.message || '保存失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ saving: false });
      });
  },

  onDelete() {
    this.setData({ showDeleteConfirm: true });
  },

  onCancelDelete() {
    this.setData({ showDeleteConfirm: false });
  },

  onConfirmDelete() {
    if (this.data.deleting) return;
    this.setData({ showDeleteConfirm: false, deleting: true });
    request.del(API.TARGET_DETAIL(this.data.id))
      .then(() => {
        wx.showToast({ title: '已删除' });
        wx.navigateBack();
      })
      .catch((err) => {
        wx.showToast({ title: err.message || '删除失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ deleting: false });
      });
  },

  onGoProfile() {
    wx.navigateTo({ url: '/package-coach/pages/profile/profile?targetId=' + this.data.id });
  },

  onGoJobBrief() {
    wx.navigateTo({ url: '/package-coach/pages/jobbrief/jobbrief?targetId=' + this.data.id });
  },

  onGoAssessment() {
    wx.navigateTo({ url: '/package-coach/pages/assessment/assessment?targetId=' + this.data.id });
  },

  onGoTraining() {
    wx.navigateTo({ url: '/package-coach/pages/training/training?targetId=' + this.data.id });
  },

  onGoMockInterview() {
    wx.navigateTo({ url: '/package-coach/pages/mock-interview/mock-interview?targetId=' + this.data.id });
  },

  onGoReports() {
    wx.navigateTo({ url: '/package-coach/pages/report-detail/report-detail?targetId=' + this.data.id });
  },

  onPullDownRefresh() {
    this.loadTarget().finally(() => wx.stopPullDownRefresh());
  },

  onShareAppMessage() {
    return {
      title: '我在用 AI 面试教练准备「' + (this.data.title || '面试') + '」',
      path: '/pages/target-detail/target-detail?id=' + this.data.id
    };
  }
});
