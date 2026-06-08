/**
 * 应用配置，集中管理环境相关常量。
 */

// 后端 API 基础地址
// 开发环境使用本地 IP，生产环境替换为正式域名
const BASE_URL = 'http://localhost:18080';

// 请求超时时间（毫秒）
const REQUEST_TIMEOUT = 15000;

module.exports = {
  BASE_URL,
  REQUEST_TIMEOUT
};
