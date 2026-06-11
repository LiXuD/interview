/**
 * 隐私授权工具函数。
 * 微信要求调用隐私 API 前必须先获得用户授权。
 */

/**
 * 检查并请求隐私授权。
 * @returns {Promise<void>} 已授权则 resolve，需要授权则 reject
 */
function checkPrivacy() {
  return new Promise((resolve, reject) => {
    wx.getPrivacySetting({
      success(res) {
        if (res.needAuthorization) {
          reject(new Error('NEED_PRIVACY_AUTH'));
        } else {
          resolve();
        }
      },
      fail(err) {
        // getPrivacySetting 失败时放行，避免阻塞登录
        console.warn('[privacy] getPrivacySetting failed:', err);
        resolve();
      }
    });
  });
}

/**
 * 弹出隐私授权弹窗。
 * @returns {Promise<boolean>} 用户是否同意
 */
function requestPrivacyAuthorize() {
  return new Promise((resolve, reject) => {
    wx.requirePrivacyAuthorize({
      success: () => resolve(true),
      fail: (err) => {
        if (err.errMsg && err.errMsg.includes('deny')) {
          resolve(false);
        } else {
          reject(err);
        }
      }
    });
  });
}

module.exports = {
  checkPrivacy,
  requestPrivacyAuthorize
};
