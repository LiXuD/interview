import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import UserUsageTable from '../UserUsageTable'
import type { AdminUserRow } from '../../api/adminUsage'

function makeRow(overrides: Partial<AdminUserRow> = {}): AdminUserRow {
  return {
    userId: 'u1',
    username: 'alice',
    email: 'alice@example.com',
    role: 'USER',
    createdAt: '2026-01-15T10:00:00Z',
    monthlyTokenQuota: 100000,
    currentMonthTokens: 50000,
    remainingMonthlyTokens: 50000,
    quotaExceeded: false,
    lastUsedAt: '2026-06-01T12:30:00Z',
    summary: {
      totalRequests: 10,
      successfulRequests: 9,
      failedRequests: 1,
      estimatedRequests: 0,
      totalInputTokens: 30000,
      totalOutputTokens: 20000,
      totalCacheCreationTokens: 0,
      totalCacheReadTokens: 0,
      totalReasoningTokens: 0,
      totalTokens: 50000,
    },
    ...overrides,
  }
}

describe('UserUsageTable', () => {
  it('renders table headers', () => {
    render(
      <UserUsageTable items={[]} page={0} totalPages={0} totalElements={0} onPageChange={vi.fn()} onUserClick={vi.fn()} />
    )
    expect(screen.getByText('用户名')).toBeInTheDocument()
    expect(screen.getByText('角色')).toBeInTheDocument()
    expect(screen.getByText('本月 Token')).toBeInTheDocument()
    expect(screen.getByText('配额')).toBeInTheDocument()
  })

  it('renders user rows with data', () => {
    render(
      <UserUsageTable items={[makeRow()]} page={0} totalPages={1} totalElements={1} onPageChange={vi.fn()} onUserClick={vi.fn()} />
    )
    expect(screen.getByText('alice')).toBeInTheDocument()
    expect(screen.getByText('alice@example.com')).toBeInTheDocument()
    expect(screen.getByText('USER')).toBeInTheDocument()
  })

  it('shows empty state when no items', () => {
    render(
      <UserUsageTable items={[]} page={0} totalPages={0} totalElements={0} onPageChange={vi.fn()} onUserClick={vi.fn()} />
    )
    expect(screen.getByText('暂无数据')).toBeInTheDocument()
  })

  it('shows quota exceeded badge', () => {
    render(
      <UserUsageTable items={[makeRow({ quotaExceeded: true })]} page={0} totalPages={1} totalElements={1} onPageChange={vi.fn()} onUserClick={vi.fn()} />
    )
    expect(screen.getByText('超限')).toBeInTheDocument()
  })

  it('shows unlimited quota when quota is null', () => {
    render(
      <UserUsageTable items={[makeRow({ monthlyTokenQuota: null, remainingMonthlyTokens: null })]} page={0} totalPages={1} totalElements={1} onPageChange={vi.fn()} onUserClick={vi.fn()} />
    )
    expect(screen.getByText('不限')).toBeInTheDocument()
  })

  it('calls onUserClick when row is clicked', async () => {
    const onUserClick = vi.fn()
    const user = userEvent.setup()
    render(
      <UserUsageTable items={[makeRow()]} page={0} totalPages={1} totalElements={1} onPageChange={vi.fn()} onUserClick={onUserClick} />
    )
    await user.click(screen.getByText('alice'))
    expect(onUserClick).toHaveBeenCalledWith('u1')
  })

  it('calls onPageChange on pagination click', async () => {
    const onPageChange = vi.fn()
    const user = userEvent.setup()
    render(
      <UserUsageTable items={[makeRow()]} page={1} totalPages={3} totalElements={60} onPageChange={onPageChange} onUserClick={vi.fn()} />
    )
    await user.click(screen.getByText('上一页'))
    expect(onPageChange).toHaveBeenCalledWith(0)
  })

  it('disables prev button on first page', () => {
    render(
      <UserUsageTable items={[makeRow()]} page={0} totalPages={3} totalElements={60} onPageChange={vi.fn()} onUserClick={vi.fn()} />
    )
    expect(screen.getByText('上一页')).toBeDisabled()
  })

  it('disables next button on last page', () => {
    render(
      <UserUsageTable items={[makeRow()]} page={2} totalPages={3} totalElements={60} onPageChange={vi.fn()} onUserClick={vi.fn()} />
    )
    expect(screen.getByText('下一页')).toBeDisabled()
  })

  it('shows ADMIN badge for admin users', () => {
    render(
      <UserUsageTable items={[makeRow({ role: 'ADMIN' })]} page={0} totalPages={1} totalElements={1} onPageChange={vi.fn()} onUserClick={vi.fn()} />
    )
    expect(screen.getByText('ADMIN')).toBeInTheDocument()
  })
})
