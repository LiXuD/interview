/**
 * 登录页：支持微信登录和 Dev Login。
 */
const auth = require('../../utils/auth');
const storage = require('../../utils/storage');

Page({
  data: {
    username: '',
    loading: false,
    error: ''
  },

  onUsernameInput(e) {
    this.setData({ username: e.detail.value, error: '' });
  },

  onWechatLogin() {
    this.setData({ loading: true, error: '' });
    auth.wechatLogin()
      .then(() => {
        this._navigateAfterLogin();
      })
      .catch((err) => {
        this.setData({ error: err.message || '微信登录失败' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  onDevLogin() {
    const username = this.data.username.trim();
    if (!username) return;
    this.setData({ loading: true, error: '' });
    auth.devLogin(username)
      .then(() => {
        this._navigateAfterLogin();
      })
      .catch((err) => {
        this.setData({ error: err.message || '登录失败' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  _navigateAfterLogin() {
    if (storage.hasCompletedOnboarding()) {
      wx.reLaunch({ url: storage.ROUTES.TARGETS });
    } else {
      wx.reLaunch({ url: storage.ROUTES.ONBOARDING });
    }
  }
});
