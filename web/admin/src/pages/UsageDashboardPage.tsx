import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { fetchOverview, fetchUsersPage, type AdminOverview, type AdminUsersPage } from '../api/adminUsage'
import KpiCard from '../components/KpiCard'
import UsageTrendChart from '../components/UsageTrendChart'
import UsageRankingChart from '../components/UsageRankingChart'
import UserUsageTable from '../components/UserUsageTable'
import UserUsageDrawer from '../components/UserUsageDrawer'
import { formatTokens } from '../utils/format'

export default function UsageDashboardPage() {
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null)
  const [sort, setSort] = useState('totalTokensDesc')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')

  const { data: overview, isLoading: overviewLoading } = useQuery<AdminOverview>({
    queryKey: ['admin-overview', startDate, endDate],
    queryFn: () => fetchOverview({ startDate: startDate || undefined, endDate: endDate || undefined }),
  })

  const { data: usersPage, isLoading: usersLoading } = useQuery<AdminUsersPage>({
    queryKey: ['admin-users', keyword, page, sort, startDate, endDate],
    queryFn: () => fetchUsersPage({ keyword: keyword || undefined, page, size: 20, sort, startDate: startDate || undefined, endDate: endDate || undefined }),
  })

  if (overviewLoading) return <div className="app-content" style={{ textAlign: 'center', color: 'var(--text-tertiary)', paddingTop: 80 }}>加载中...</div>
  if (!overview) return <div className="app-content" style={{ color: 'var(--danger)', paddingTop: 80 }}>加载失败</div>

  return (
    <>
      {/* KPI Cards */}
      <div className="kpi-grid">
        <KpiCard label="总用户" value={overview.totalUsers} />
        <KpiCard label="活跃用户" value={overview.activeUsers} sub="本周期有 AI 调用" />
        <KpiCard label="超限用户" value={overview.quotaExceededUsers} sub="本月平台 AI 超限" />
        <KpiCard label="总请求" value={overview.summary.totalRequests} />
        <KpiCard label="总 Token" value={formatTokens(overview.summary.totalTokens)} />
        <KpiCard label="成功率" value={
          overview.summary.totalRequests > 0
            ? Math.round(overview.summary.successfulRequests / overview.summary.totalRequests * 100) + '%'
            : '-'
        } />
      </div>

      {/* Charts */}
      <div className="chart-section">
        <UsageTrendChart data={overview.daily} />
        <UsageRankingChart title="Top 用户" data={overview.topUsers} />
      </div>
      <div className="chart-section">
        <UsageRankingChart title="Top 模型" data={overview.topModels} />
        <UsageRankingChart title="Top 任务" data={overview.topTasks} />
      </div>

      {/* User Table */}
      <div className="table-container">
        <div className="table-toolbar">
          <input
            placeholder="搜索用户名或邮箱..."
            value={keyword}
            onChange={e => { setKeyword(e.target.value); setPage(0) }}
          />
          <input
            type="date"
            value={startDate}
            onChange={e => { setStartDate(e.target.value); setPage(0) }}
            title="开始日期"
          />
          <input
            type="date"
            value={endDate}
            onChange={e => { setEndDate(e.target.value); setPage(0) }}
            title="结束日期"
          />
          <select value={sort} onChange={e => { setSort(e.target.value); setPage(0) }}>
            <option value="totalTokensDesc">Token 用量降序</option>
            <option value="totalRequestsDesc">请求数降序</option>
            <option value="usernameAsc">用户名升序</option>
            <option value="createdAtDesc">注册时间降序</option>
          </select>
          <button className="btn-ghost" onClick={() => { setKeyword(''); setStartDate(''); setEndDate(''); setPage(0); setSort('totalTokensDesc') }}>重置</button>
        </div>
        {usersLoading ? (
          <div style={{ padding: 32, textAlign: 'center', color: 'var(--text-tertiary)' }}>加载中...</div>
        ) : usersPage ? (
          <UserUsageTable
            items={usersPage.items}
            page={usersPage.page}
            totalPages={usersPage.totalPages}
            totalElements={usersPage.totalElements}
            onPageChange={setPage}
            onUserClick={setSelectedUserId}
          />
        ) : null}
      </div>

      {/* User Detail Drawer */}
      <UserUsageDrawer userId={selectedUserId} onClose={() => setSelectedUserId(null)} />
    </>
  )
}
