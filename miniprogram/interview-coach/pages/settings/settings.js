/**
 * 设置页：账号信息、健康检查、隐私政策、退出登录、删除账号。
 */
const storage = require('../../utils/storage');
const auth = require('../../utils/auth');
const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: {
    username: '',
    userId: '',
    healthStatus: '未检查',
    healthOk: false,
    error: '',
    showDeleteConfirm: false,
    deleteLocalMemory: false
  },

  onLoad() {
    const info = storage.getUserInfo();
    this.setData({
      username: info.username || '未知',
      userId: info.userId || '未知'
    });
    this.checkHealth();
  },

  checkHealth() {
    this.setData({ healthStatus: '检查中...' });
    request.get(API.HEALTH)
      .then((data) => {
        this.setData({
          healthStatus: data && data.status === 'UP' ? '已连接' : '异常',
          healthOk: data && data.status === 'UP'
        });
      })
      .catch(() => {
        this.setData({ healthStatus: '连接失败', healthOk: false });
      });
  },

  goPrivacy() {
    wx.navigateTo({ url: '/pages/privacy/privacy' });
  },

  onLogout() {
    wx.showModal({
      title: '确认退出',
      content: '退出后需要重新登录',
      success: (res) => {
        if (res.confirm) {
          auth.logout();
          wx.reLaunch({ url: '/pages/login/login' });
        }
      }
    });
  },

  onDeleteAccount() {
    wx.showModal({
      title: '删除账号',
      content: '此操作不可撤销。所有远端数据（目标岗位、测评、训练、模拟面试、报告、教练记忆）将被永久删除。',
      confirmColor: '#e74c3c'
    }).then((res) => {
      if (!res.confirm) return;
      this.setData({ showDeleteConfirm: true, deleteLocalMemory: false });
    });
  },

  onDeleteMemoryChange(e) {
    this.setData({ deleteLocalMemory: e.detail.value.length > 0 });
  },

  onCancelDelete() {
    this.setData({ showDeleteConfirm: false });
  },

  onConfirmDelete() {
    this.setData({ showDeleteConfirm: false, error: '' });
    request.del(API.ME)
      .then(() => {
        if (this.data.deleteLocalMemory) {
          try { wx.removeStorageSync('ic_coaching_memory'); } catch (e) { /* ignore */ }
        }
        storage.clearAuth();
        wx.showToast({ title: '账号已删除', icon: 'success' });
        this._navTimer = setTimeout(() => {
          wx.reLaunch({ url: '/pages/login/login' });
        }, 1000);
      })
      .catch((err) => {
        this.setData({ error: err.message || '删除失败' });
      });
  },

  onUnload() {
    if (this._navTimer) clearTimeout(this._navTimer);
  }
});
