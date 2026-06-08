/**
 * 应用配置，集中管理环境相关常量。
 * 根据小程序运行环境自动切换 API 地址。
 */

const ENV_URLS = {
  develop: 'http://localhost:18080',   // 开发环境（本地后端）
  trial: 'https://api.example.com',    // 体验版（替换为实际地址）
  release: 'https://api.example.com'   // 正式版（替换为实际地址）
};

function getBaseUrl() {
  try {
    const { envVersion } = wx.getAccountInfoSync().miniProgram;
    return ENV_URLS[envVersion] || ENV_URLS.develop;
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
