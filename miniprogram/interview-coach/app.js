/**
 * 小程序入口，初始化全局逻辑。
 */
App({
  globalData: {},

  onLaunch() {},

  onError(error) {
    console.error('[app] uncaught error', error);
  },

  onUnhandledRejection(res) {
    console.error('[app] unhandled rejection', res && (res.reason || res));
  },

  onPageNotFound(res) {
    console.warn('[app] page not found', res);
    wx.reLaunch({ url: '/pages/targets/targets' });
  }
});
