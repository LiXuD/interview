/**
 * 本地存储读写集中维护，禁止在页面中直接调用 wx.setStorageSync。
 *
 * 存储内容：
 *   - token: Bearer Token
 *   - userId: 当前用户 ID
 *   - username: 当前用户名
 *
 * 禁止存储：API Key、微信 sessionKey、AI 原始字符串、简历原文副本。
 */

const KEYS = {
  TOKEN: 'ic_token',
  USER_ID: 'ic_user_id',
  USERNAME: 'ic_username'
};

function getToken() {
  return wx.getStorageSync(KEYS.TOKEN) || '';
}

function setToken(token) {
  wx.setStorageSync(KEYS.TOKEN, token);
}

function removeToken() {
  wx.removeStorageSync(KEYS.TOKEN);
}

function getUserInfo() {
  return {
    userId: wx.getStorageSync(KEYS.USER_ID) || '',
    username: wx.getStorageSync(KEYS.USERNAME) || ''
  };
}

function setUserInfo(userId, username) {
  wx.setStorageSync(KEYS.USER_ID, userId);
  wx.setStorageSync(KEYS.USERNAME, username);
}

function removeUserInfo() {
  wx.removeStorageSync(KEYS.USER_ID);
  wx.removeStorageSync(KEYS.USERNAME);
}

function clearAuth() {
  removeToken();
  removeUserInfo();
}

function isLoggedIn() {
  return !!getToken();
}

module.exports = {
  KEYS,
  getToken,
  setToken,
  removeToken,
  getUserInfo,
  setUserInfo,
  removeUserInfo,
  clearAuth,
  isLoggedIn
};
