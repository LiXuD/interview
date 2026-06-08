/**
 * 认证状态管理，处理登录、登出和 401 自动清理。
 */

const storage = require('./storage');
const request = require('./request');
const API = require('./api');

function handleAuthResponse(data) {
  storage.setToken(data.token);
  storage.setUserInfo(data.userId, data.username);
  return data;
}

/**
 * Dev Login：用用户名登录获取 Token。
 */
function devLogin(username) {
  return request.post(API.AUTH_DEV_LOGIN, { username }).then(handleAuthResponse);
}

/**
 * 微信登录：调用 wx.login 获取 code，发送到后端换取 Token。
 */
function wechatLogin() {
  return new Promise((resolve, reject) => {
    wx.login({
      success(loginRes) {
        if (!loginRes.code) {
          reject(new Error('wx.login 获取 code 失败'));
          return;
        }
        request.post(API.AUTH_WECHAT, { code: loginRes.code })
          .then(handleAuthResponse)
          .then(resolve)
          .catch(reject);
      },
      fail(err) {
        reject(new Error('wx.login 调用失败: ' + (err.errMsg || '')));
      }
    });
  });
}

/**
 * 登出：清除本地登录态。
 */
function logout() {
  storage.clearAuth();
}

/**
 * 处理 401 响应：清除登录态并跳转到登录页。
 */
function handleUnauthorized() {
  storage.clearAuth();
  wx.reLaunch({ url: '/pages/login/login' });
}

module.exports = {
  devLogin,
  wechatLogin,
  logout,
  handleUnauthorized,
  isLoggedIn: storage.isLoggedIn
};
