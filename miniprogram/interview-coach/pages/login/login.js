/**
 * 登录页：支持微信登录和 Dev Login。
 */
const auth = require('../../utils/auth');
const storage = require('../../utils/storage');
const config = require('../../utils/config');

Page({
  data: {
    username: '',
    loading: false,
    error: '',
    showDevLogin: config.IS_DEVELOPMENT
  },

  onLoad() {
    if (auth.isLoggedIn()) {
      this._navigateAfterLogin();
    }
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
    if (!this.data.showDevLogin) {
      this.setData({ error: '当前环境不支持 Dev Login' });
      return;
    }
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
