/**
 * 小程序入口，初始化全局逻辑。
 */
const auth = require('./utils/auth');

App({
  globalData: {},

  onLaunch() {
    if (!auth.isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/login' });
    }
  }
});
