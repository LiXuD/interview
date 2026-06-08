/**
 * 统一请求封装。
 * 自动注入 Bearer Token、统一错误处理、401 自动清理登录态。
 */

const storage = require('./storage');
const { BASE_URL, REQUEST_TIMEOUT } = require('./config');

/**
 * 发起带认证的 HTTP 请求。
 *
 * @param {Object} options
 * @param {string} options.url - 接口路径（不含基础地址）
 * @param {string} [options.method='GET'] - HTTP 方法
 * @param {Object} [options.data] - 请求体
 * @param {Object} [options.header] - 额外请求头
 * @returns {Promise<Object>} 响应数据
 */
function request(options) {
  return new Promise((resolve, reject) => {
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
          wx.reLaunch({ url: '/pages/login/login' });
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

/**
 * GET 请求。
 */
function get(url, data) {
  return request({ url, method: 'GET', data });
}

/**
 * POST 请求。
 */
function post(url, data) {
  return request({ url, method: 'POST', data });
}

/**
 * PUT 请求。
 */
function put(url, data) {
  return request({ url, method: 'PUT', data });
}

/**
 * PATCH 请求。
 */
function patch(url, data) {
  return request({ url, method: 'PATCH', data });
}

/**
 * DELETE 请求。
 */
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
