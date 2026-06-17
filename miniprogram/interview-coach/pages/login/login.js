/**
 * 登录页：使用微信登录进入 AI 面试教练闭环。
 */
const auth = require('../../utils/auth');
const storage = require('../../utils/storage');
const privacy = require('../../utils/privacy');

Page({
  data: {
    loading: false,
    error: ''
  },

  onLoad() {
    if (auth.isLoggedIn()) {
      this._navigateAfterLogin();
    }
  },

  onWechatLogin() {
    if (this.data.loading) return;
    this.setData({ loading: true, error: '' });
    privacy.checkPrivacy()
      .then(() => auth.wechatLogin())
      .then(() => {
        this._navigateAfterLogin();
      })
      .catch((err) => {
        if (err.message === 'NEED_PRIVACY_AUTH') {
          return privacy.requestPrivacyAuthorize().then((granted) => {
            if (!granted) {
              this.setData({ error: '需要同意隐私授权才能登录' });
              return;
            }
            return this.onWechatLogin();
          });
        }
        this.setData({ error: err.message || '微信登录失败' });
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
