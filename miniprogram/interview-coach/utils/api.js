/**
 * 接口路径集中维护，禁止在页面中散落 URL。
 */

const API = {
  // 认证
  AUTH_WECHAT: '/api/auth/wechat',
  AUTH_LOGOUT: '/api/auth/logout',

  // 当前用户
  ME: '/api/me',

  // 健康检查
  HEALTH: '/api/health',

  // 目标岗位
  TARGETS: '/api/targets',
  TARGET_DETAIL: (id) => `/api/targets/${id}`,

  // 候选人摘要
  PROFILE_DRAFT_SUMMARY: '/api/profiles/draft-summary',
  PROFILE_CONFIRM: '/api/profiles/confirm',
  PROFILE_CURRENT: '/api/profiles/current',

  // 岗位画像
  JOB_BRIEF: (targetId) => `/api/job-briefs/${targetId}`,
  JOB_BRIEF_GENERATE: '/api/job-briefs/generate',

  // 测评
  ASSESSMENT_START: '/api/assessments/start',
  ASSESSMENT_ANSWER: (id) => `/api/assessments/${id}/answers`,
  ASSESSMENT_FINISH: (id) => `/api/assessments/${id}/finish`,

  // 训练
  TRAINING_PLAN: (targetId) => `/api/training-plans/${targetId}`,
  TRAINING_PLAN_GENERATE: '/api/training-plans/generate',
  TRAINING_TASK_ANSWER: (id) => `/api/training-tasks/${id}/answer`,
  TRAINING_TASK_COMPLETE: (id) => `/api/training-tasks/${id}/complete`,
  ADAPTIVE_SESSION_START: (id) => `/api/training-tasks/${id}/adaptive-sessions/start`,
  ADAPTIVE_SESSION_ANSWER: (id) => `/api/training-sessions/${id}/answers`,

  // 模拟面试
  MOCK_INTERVIEWS_BY_TARGET: (targetId) => `/api/mock-interviews/target/${targetId}`,
  MOCK_INTERVIEW_START: '/api/mock-interviews/start',
  MOCK_INTERVIEW_ANSWER: (id) => `/api/mock-interviews/${id}/answer`,
  MOCK_INTERVIEW_FINISH: (id) => `/api/mock-interviews/${id}/finish`,

  // 报告
  REPORTS: '/api/reports',
  REPORT_DETAIL: (id) => `/api/reports/${id}`,

  // 维度分析
  DIMENSION_ANALYSIS: '/api/dimension-analysis',

  // 进步追踪
  PROGRESS: '/api/progress',

  // 教练 Agent
  COACH_AGENT: (targetId) => `/api/targets/${targetId}/coach-agent`,

  // AI Provider
  AI_PROVIDERS: '/api/ai-providers',
  AI_PROVIDERS_STATUS: '/api/ai-providers/status',
  AI_PROVIDERS_TEST: '/api/ai-providers/test',
  AI_PROVIDER_SET_DEFAULT: (id) => `/api/ai-providers/${id}/default`,
  AI_PROVIDER_DELETE: (id) => `/api/ai-providers/${id}`,

  // AI 用量
  AI_USAGE_SUMMARY: '/api/ai-usage/me/summary',
  AI_USAGE_DAILY: '/api/ai-usage/me/daily',
  AI_USAGE_BY_TASK: '/api/ai-usage/me/by-task',
  AI_USAGE_BY_MODEL: '/api/ai-usage/me/by-model',
  AI_USAGE_BY_PROVIDER: '/api/ai-usage/me/by-provider',

  // 教练记忆
  COACHING_MEMORIES: (targetId) => `/api/coaching-memories/target/${targetId}`,
  COACHING_MEMORY_CORRECT: (id) => `/api/coaching-memories/${id}/corrections`,
  COACHING_MEMORY_IMPORT: '/api/coaching-memories/import'
};

module.exports = API;
