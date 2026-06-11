/**
 * 统一请求封装。
 * 自动注入 Bearer Token、统一错误处理、401 自动清理登录态。
 */

const storage = require('./storage');
const { BASE_URL, REQUEST_TIMEOUT } = require('./config');

let isRedirectingToLogin = false;

function resetRedirectFlag() {
  isRedirectingToLogin = false;
}

function request(options) {
  return new Promise((resolve, reject) => {
    if (!BASE_URL) {
      reject(new Error('当前环境未配置 API 地址，请联系管理员'));
      return;
    }

    const token = storage.getToken();
    const header = {
      'Content-Type': 'application/json',
      ...options.header
    };
    if (token) {
      header['Authorization'] = 'Bearer ' + token;
    }

    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header,
      timeout: options.timeout || REQUEST_TIMEOUT,
      success(res) {
        if (res.statusCode === 401) {
          storage.clearAuth();
          if (!isRedirectingToLogin) {
            isRedirectingToLogin = true;
            wx.reLaunch({
              url: '/pages/login/login',
              complete: resetRedirectFlag
            });
            setTimeout(resetRedirectFlag, 3000);
          }
          reject(new Error('未登录或登录已过期'));
          return;
        }
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data);
        } else {
          const msg = (res.data && res.data.message) || '请求失败 (' + res.statusCode + ')';
          const err = new Error(msg);
          err.statusCode = res.statusCode;
          err.data = res.data;
          reject(err);
        }
      },
      fail(err) {
        reject(new Error('网络请求失败: ' + (err.errMsg || '')));
      }
    });
  });
}

function get(url, data) {
  return request({ url, method: 'GET', data });
}

function post(url, data) {
  return request({ url, method: 'POST', data });
}

function put(url, data) {
  return request({ url, method: 'PUT', data });
}

function patch(url, data) {
  return request({ url, method: 'PATCH', data });
}

function del(url, data) {
  return request({ url, method: 'DELETE', data });
}

module.exports = {
  request,
  get,
  post,
  put,
  patch,
  del
};
