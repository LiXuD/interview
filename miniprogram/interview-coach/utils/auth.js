/**
 * 认证状态管理，处理登录、登出。
 * 401 处理由 utils/request.js 统一处理。
 */

const storage = require('./storage');
const request = require('./request');
const API = require('./api');

function handleAuthResponse(data) {
  storage.setToken(data.token);
  storage.setUserInfo(data.userId, data.username);
  return data;
}

function devLogin(username) {
  return request.post(API.AUTH_DEV_LOGIN, { username }).then(handleAuthResponse);
}

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

function logout() {
  storage.clearAuth();
}

module.exports = {
  devLogin,
  wechatLogin,
  logout,
  isLoggedIn: storage.isLoggedIn
};
