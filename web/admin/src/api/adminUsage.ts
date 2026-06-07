import { apiFetch } from './http'

export interface AiUsageSummary {
  totalRequests: number
  successfulRequests: number
  failedRequests: number
  estimatedRequests: number
  totalInputTokens: number
  totalOutputTokens: number
  totalCacheCreationTokens: number
  totalCacheReadTokens: number
  totalReasoningTokens: number
  totalTokens: number
}

export interface AiUsageDailyPoint {
  date: string
  totalRequests: number
  successfulRequests: number
  failedRequests: number
  totalInputTokens: number
  totalOutputTokens: number
  totalCacheCreationTokens: number
  totalCacheReadTokens: number
  totalReasoningTokens: number
  totalTokens: number
}

export interface AiUsageBreakdown {
  name: string
  totalRequests: number
  successfulRequests: number
  failedRequests: number
  estimatedRequests: number
  totalInputTokens: number
  totalOutputTokens: number
  totalCacheCreationTokens: number
  totalCacheReadTokens: number
  totalReasoningTokens: number
  totalTokens: number
}

export interface AdminOverview {
  totalUsers: number
  activeUsers: number
  quotaExceededUsers: number
  summary: AiUsageSummary
  daily: AiUsageDailyPoint[]
  topUsers: AiUsageBreakdown[]
  topModels: AiUsageBreakdown[]
  topTasks: AiUsageBreakdown[]
  providers: AiUsageBreakdown[]
}

export interface AdminUserRow {
  userId: string
  username: string
  email: string | null
  role: string
  createdAt: string | null
  monthlyTokenQuota: number | null
  currentMonthTokens: number
  remainingMonthlyTokens: number | null
  quotaExceeded: boolean
  lastUsedAt: string | null
  summary: AiUsageSummary
}

export interface AdminUsersPage {
  page: number
  size: number
  totalElements: number
  totalPages: number
  items: AdminUserRow[]
}

export interface AdminUserDetail {
  userId: string
  username: string
  email: string | null
  role: string
  createdAt: string | null
  monthlyTokenQuota: number | null
  currentMonthTokens: number
  remainingMonthlyTokens: number | null
  quotaExceeded: boolean
  summary: AiUsageSummary
  daily: AiUsageDailyPoint[]
  byTask: AiUsageBreakdown[]
  byModel: AiUsageBreakdown[]
  byProvider: AiUsageBreakdown[]
}

export interface AdminTokenQuota {
  userId: string
  monthlyTokenQuota: number | null
  currentMonthTokens: number
  remainingMonthlyTokens: number | null
  quotaExceeded: boolean
}

export function fetchOverview(params?: { startDate?: string; endDate?: string }): Promise<AdminOverview> {
  const qs = new URLSearchParams()
  if (params?.startDate) qs.set('startDate', params.startDate)
  if (params?.endDate) qs.set('endDate', params.endDate)
  const query = qs.toString()
  return apiFetch(`/api/admin/ai-usage/overview${query ? '?' + query : ''}`)
}

export function fetchUsersPage(params: {
  startDate?: string
  endDate?: string
  keyword?: string
  page?: number
  size?: number
  sort?: string
}): Promise<AdminUsersPage> {
  const qs = new URLSearchParams()
  if (params.startDate) qs.set('startDate', params.startDate)
  if (params.endDate) qs.set('endDate', params.endDate)
  if (params.keyword) qs.set('keyword', params.keyword)
  if (params.page !== undefined) qs.set('page', String(params.page))
  if (params.size !== undefined) qs.set('size', String(params.size))
  if (params.sort) qs.set('sort', params.sort)
  return apiFetch(`/api/admin/ai-usage/users?${qs.toString()}`)
}

export function fetchUserDetail(userId: string, params?: {
  startDate?: string
  endDate?: string
}): Promise<AdminUserDetail> {
  const qs = new URLSearchParams()
  if (params?.startDate) qs.set('startDate', params.startDate)
  if (params?.endDate) qs.set('endDate', params.endDate)
  const query = qs.toString()
  return apiFetch(`/api/admin/ai-usage/users/${userId}${query ? '?' + query : ''}`)
}

export function updateTokenQuota(userId: string, monthlyTokenQuota: number | null): Promise<AdminTokenQuota> {
  return apiFetch(`/api/admin/users/${userId}/token-quota`, {
    method: 'PATCH',
    body: JSON.stringify({ monthlyTokenQuota }),
  })
}
