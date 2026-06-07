import type { AdminUserRow } from '../api/adminUsage'
import { formatTokens } from '../utils/format'

interface Props {
  items: AdminUserRow[]
  page: number
  totalPages: number
  totalElements: number
  onPageChange: (page: number) => void
  onUserClick: (userId: string) => void
}

function formatTime(s: string | null): string {
  if (!s) return '-'
  return s.replace('T', ' ').replace(/Z$/, '').slice(0, 16)
}

export default function UserUsageTable({ items, page, totalPages, totalElements, onPageChange, onUserClick }: Props) {
  return (
    <div className="table-container">
      <table>
        <thead>
          <tr>
            <th>用户名</th>
            <th>角色</th>
            <th>本月 Token</th>
            <th>配额</th>
            <th>剩余</th>
            <th>请求数</th>
            <th>最后使用</th>
          </tr>
        </thead>
        <tbody>
          {items.map(row => (
            <tr key={row.userId} onClick={() => onUserClick(row.userId)} style={{ cursor: 'pointer' }}>
              <td>
                <strong>{row.username}</strong>
                {row.email && <div style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>{row.email}</div>}
              </td>
              <td>
                <span className={`badge ${row.role === 'ADMIN' ? 'badge-info' : 'badge-ok'}`}>
                  {row.role}
                </span>
              </td>
              <td className="mono">{formatTokens(row.currentMonthTokens)}</td>
              <td className="mono">{row.monthlyTokenQuota != null ? formatTokens(row.monthlyTokenQuota) : '不限'}</td>
              <td>
                {row.quotaExceeded ? (
                  <span className="badge badge-danger">超限</span>
                ) : (
                  <span className="mono">{formatTokens(row.remainingMonthlyTokens)}</span>
                )}
              </td>
              <td className="mono">{row.summary.totalRequests}</td>
              <td style={{ fontSize: 12, fontFamily: 'var(--font-mono)', color: 'var(--text-tertiary)' }}>{formatTime(row.lastUsedAt)}</td>
            </tr>
          ))}
          {items.length === 0 && (
            <tr><td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-tertiary)', padding: 40 }}>暂无数据</td></tr>
          )}
        </tbody>
      </table>
      <div className="pagination">
        <span>共 {totalElements} 条</span>
        <div className="pages">
          <button className="btn-ghost" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>上一页</button>
          {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => i).map(i => (
            <button
              key={i}
              className={i === page ? 'active' : ''}
              onClick={() => onPageChange(i)}
            >
              {i + 1}
            </button>
          ))}
          <button className="btn-ghost" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>下一页</button>
        </div>
      </div>
    </div>
  )
}
