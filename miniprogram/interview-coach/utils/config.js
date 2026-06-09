/**
 * 应用配置，集中管理环境相关常量。
 * 根据小程序运行环境自动切换 API 地址。
 */

const ENV_URLS = {
  develop: 'http://localhost:18080',   // 开发环境（本地后端）
  // TODO: 部署前必须替换为实际后端地址，否则体验版和正式版无法连接
  trial: '',                           // 体验版（部署时配置）
  release: ''                          // 正式版（部署时配置）
};

function getBaseUrl() {
  try {
    const { envVersion } = wx.getAccountInfoSync().miniProgram;
    const url = ENV_URLS[envVersion];
    if (!url) {
      console.warn('[config] 环境 ' + envVersion + ' 未配置 API 地址，回退到开发环境');
      return ENV_URLS.develop;
    }
    return url;
  } catch (e) {
    return ENV_URLS.develop;
  }
}

const BASE_URL = getBaseUrl();
const REQUEST_TIMEOUT = 15000;

module.exports = {
  BASE_URL,
  REQUEST_TIMEOUT
};
