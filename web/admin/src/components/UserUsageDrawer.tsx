import { useQuery } from '@tanstack/react-query'
import { fetchUserDetail, type AdminUserDetail } from '../api/adminUsage'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import QuotaEditor from './QuotaEditor'
import { formatTokens } from '../utils/format'

interface Props {
  userId: string | null
  startDate?: string
  endDate?: string
  onClose: () => void
}

export default function UserUsageDrawer({ userId, startDate, endDate, onClose }: Props) {
  const { data, isLoading, error } = useQuery<AdminUserDetail>({
    queryKey: ['admin-user-detail', userId, startDate, endDate],
    queryFn: () => fetchUserDetail(userId!, { startDate, endDate }),
    enabled: !!userId,
  })

  if (!userId) return null

  return (
    <>
      <div className="drawer-overlay" onClick={onClose} />
      <div className="drawer">
        <div className="drawer-header">
          <h2>{isLoading ? '加载中...' : data?.username || '用户详情'}</h2>
          <button className="btn-ghost" onClick={onClose} style={{ fontSize: 20 }}>&times;</button>
        </div>

        {error && <div style={{ color: 'var(--danger)' }}>加载失败</div>}

        {data && (
          <>
            <div className="drawer-section">
              <h3>基本信息</h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, fontSize: 13 }}>
                <div><span style={{ color: 'var(--text-tertiary)' }}>角色</span> <span className={`badge ${data.role === 'ADMIN' ? 'badge-info' : 'badge-ok'}`}>{data.role}</span></div>
                <div><span style={{ color: 'var(--text-tertiary)' }}>邮箱</span> <span style={{ marginLeft: 4 }}>{data.email || '-'}</span></div>
                <div><span style={{ color: 'var(--text-tertiary)' }}>注册时间</span> <span style={{ marginLeft: 4, fontFamily: 'var(--font-mono)', fontSize: 12 }}>{data.createdAt?.replace('T', ' ').slice(0, 16) || '-'}</span></div>
                <div><span style={{ color: 'var(--text-tertiary)' }}>配额状态</span> {data.quotaExceeded ? <span className="badge badge-danger">超限</span> : <span className="badge badge-ok">正常</span>}</div>
              </div>
            </div>

            <div className="drawer-section">
              <h3>月度配额</h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10, marginBottom: 14 }}>
                <div className="kpi-card">
                  <div className="label">本月用量</div>
                  <div className="value" style={{ fontSize: 18 }}>{formatTokens(data.currentMonthTokens)}</div>
                </div>
                <div className="kpi-card">
                  <div className="label">配额</div>
                  <div className="value" style={{ fontSize: 18 }}>{data.monthlyTokenQuota != null ? formatTokens(data.monthlyTokenQuota) : '不限'}</div>
                </div>
                <div className="kpi-card">
                  <div className="label">剩余</div>
                  <div className="value" style={{ fontSize: 18 }}>{data.remainingMonthlyTokens != null ? formatTokens(data.remainingMonthlyTokens) : '-'}</div>
                </div>
              </div>
              <QuotaEditor userId={data.userId} currentQuota={data.monthlyTokenQuota} />
            </div>

            <div className="drawer-section">
              <h3>用量汇总</h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10, fontSize: 13 }}>
                <div>总请求 <strong className="mono">{data.summary.totalRequests}</strong></div>
                <div>成功 <strong className="mono">{data.summary.successfulRequests}</strong></div>
                <div>失败 <strong className="mono">{data.summary.failedRequests}</strong></div>
                <div>Input <strong className="mono">{formatTokens(data.summary.totalInputTokens)}</strong></div>
                <div>Output <strong className="mono">{formatTokens(data.summary.totalOutputTokens)}</strong></div>
                <div>Total <strong className="mono">{formatTokens(data.summary.totalTokens)}</strong></div>
              </div>
            </div>

            {data.byTask.length > 0 && (
              <div className="drawer-section">
                <h3>按任务分布</h3>
                <ResponsiveContainer width="100%" height={200}>
                  <BarChart data={data.byTask.slice(0, 6)} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis type="number" tickFormatter={formatTokens} tick={{ fontSize: 10 }} />
                    <YAxis type="category" dataKey="name" width={110} tick={{ fontSize: 10 }} />
                    <Tooltip formatter={(v: number) => formatTokens(v)} />
                    <Bar dataKey="totalTokens" fill="#4f46e5" name="Tokens" radius={[0, 4, 4, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}

            {data.byModel.length > 0 && (
              <div className="drawer-section">
                <h3>按模型分布</h3>
                <ResponsiveContainer width="100%" height={200}>
                  <BarChart data={data.byModel.slice(0, 6)} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis type="number" tickFormatter={formatTokens} tick={{ fontSize: 10 }} />
                    <YAxis type="category" dataKey="name" width={110} tick={{ fontSize: 10 }} />
                    <Tooltip formatter={(v: number) => formatTokens(v)} />
                    <Bar dataKey="totalTokens" fill="#059669" name="Tokens" radius={[0, 4, 4, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </>
        )}
      </div>
    </>
  )
}
