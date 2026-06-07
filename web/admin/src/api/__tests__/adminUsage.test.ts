import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fetchOverview, fetchUsersPage, fetchUserDetail, updateTokenQuota } from '../adminUsage'

const mockFetch = vi.fn()
vi.stubGlobal('fetch', mockFetch)

vi.mock('../../auth/AuthStore', () => ({
  getToken: () => 'test-token',
  clearToken: vi.fn(),
}))

function mockJsonResponse(data: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(data),
  }
}

beforeEach(() => {
  mockFetch.mockReset()
})

describe('fetchOverview', () => {
  it('calls /api/admin/ai-usage/overview without params', async () => {
    mockFetch.mockResolvedValue(mockJsonResponse({ totalUsers: 0 }))
    await fetchOverview()
    expect(mockFetch).toHaveBeenCalledWith(
      '/api/admin/ai-usage/overview',
      expect.objectContaining({ headers: expect.objectContaining({ Authorization: 'Bearer test-token' }) })
    )
  })

  it('appends startDate and endDate as query params', async () => {
    mockFetch.mockResolvedValue(mockJsonResponse({ totalUsers: 0 }))
    await fetchOverview({ startDate: '2026-01-01', endDate: '2026-06-30' })
    const url = mockFetch.mock.calls[0][0] as string
    expect(url).toContain('startDate=2026-01-01')
    expect(url).toContain('endDate=2026-06-30')
  })
})

describe('fetchUsersPage', () => {
  it('passes all params as query string', async () => {
    mockFetch.mockResolvedValue(mockJsonResponse({ items: [], totalElements: 0 }))
    await fetchUsersPage({ keyword: 'alice', page: 1, size: 10, sort: 'usernameAsc', startDate: '2026-01-01' })
    const url = mockFetch.mock.calls[0][0] as string
    expect(url).toContain('keyword=alice')
    expect(url).toContain('page=1')
    expect(url).toContain('size=10')
    expect(url).toContain('sort=usernameAsc')
    expect(url).toContain('startDate=2026-01-01')
  })
})

describe('fetchUserDetail', () => {
  it('calls /api/admin/ai-usage/users/{id}', async () => {
    mockFetch.mockResolvedValue(mockJsonResponse({ userId: 'abc' }))
    await fetchUserDetail('abc')
    const url = mockFetch.mock.calls[0][0] as string
    expect(url).toContain('/api/admin/ai-usage/users/abc')
  })

  it('appends date params when provided', async () => {
    mockFetch.mockResolvedValue(mockJsonResponse({ userId: 'abc' }))
    await fetchUserDetail('abc', { startDate: '2026-03-01', endDate: '2026-03-31' })
    const url = mockFetch.mock.calls[0][0] as string
    expect(url).toContain('startDate=2026-03-01')
    expect(url).toContain('endDate=2026-03-31')
  })

  it('omits date params when not provided', async () => {
    mockFetch.mockResolvedValue(mockJsonResponse({ userId: 'abc' }))
    await fetchUserDetail('abc')
    const url = mockFetch.mock.calls[0][0] as string
    expect(url).not.toContain('startDate')
    expect(url).not.toContain('endDate')
  })
})

describe('updateTokenQuota', () => {
  it('sends PATCH with body', async () => {
    mockFetch.mockResolvedValue(mockJsonResponse({ monthlyTokenQuota: 50000 }))
    await updateTokenQuota('user-1', 50000)
    expect(mockFetch).toHaveBeenCalledWith(
      '/api/admin/users/user-1/token-quota',
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({ monthlyTokenQuota: 50000 }),
      })
    )
  })

  it('sends null quota for unlimited', async () => {
    mockFetch.mockResolvedValue(mockJsonResponse({ monthlyTokenQuota: null }))
    await updateTokenQuota('user-1', null)
    expect(mockFetch).toHaveBeenCalledWith(
      '/api/admin/users/user-1/token-quota',
      expect.objectContaining({ body: JSON.stringify({ monthlyTokenQuota: null }) })
    )
  })
})
