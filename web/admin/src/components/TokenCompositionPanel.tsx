import type { AiUsageBreakdown, AiUsageSummary } from '../api/adminUsage'
import { formatTokens } from '../utils/format'

interface Props {
  summary: AiUsageSummary
  providers: AiUsageBreakdown[]
}

const tokenParts = [
  { key: 'totalInputTokens', label: 'Input', className: 'part-input' },
  { key: 'totalOutputTokens', label: 'Output', className: 'part-output' },
  { key: 'totalCacheCreationTokens', label: 'Cache create', className: 'part-cache-create' },
  { key: 'totalCacheReadTokens', label: 'Cache read', className: 'part-cache-read' },
  { key: 'totalReasoningTokens', label: 'Reasoning', className: 'part-reasoning' },
] as const

export default function TokenCompositionPanel({ summary, providers }: Props) {
  const total = Math.max(0, summary.totalTokens)
  const visibleProviders = providers.slice(0, 6)

  return (
    <div className="token-panel">
      <section className="card token-composition">
        <div className="card-title-row">
          <h3>Token 构成</h3>
          <strong className="mono">{formatTokens(total)}</strong>
        </div>
        <div className="token-stack" aria-label="Token 构成占比">
          {tokenParts.map(part => {
            const value = summary[part.key]
            const percent = total > 0 ? Math.max(2, (value / total) * 100) : 0
            return (
              <span
                key={part.key}
                className={`token-stack-part ${part.className}`}
                style={{ width: `${percent}%` }}
                title={`${part.label}: ${formatTokens(value)}`}
              />
            )
          })}
        </div>
        <div className="token-part-grid">
          {tokenParts.map(part => (
            <div key={part.key} className="token-part-row">
              <span className={`token-dot ${part.className}`} />
              <span>{part.label}</span>
              <strong className="mono">{formatTokens(summary[part.key])}</strong>
            </div>
          ))}
        </div>
      </section>

      <section className="card provider-panel">
        <div className="card-title-row">
          <h3>Provider 消耗</h3>
          <span>{visibleProviders.length} 个来源</span>
        </div>
        {visibleProviders.length > 0 ? (
          <div className="provider-list">
            {visibleProviders.map(provider => {
              const percent = total > 0 ? (provider.totalTokens / total) * 100 : 0
              return (
                <div className="provider-row" key={provider.name}>
                  <div className="provider-row-main">
                    <span>{provider.name}</span>
                    <strong className="mono">{formatTokens(provider.totalTokens)}</strong>
                  </div>
                  <div className="provider-row-meta">
                    <span>{provider.totalRequests} req</span>
                    <span>{Math.round(percent)}%</span>
                  </div>
                  <div className="provider-meter">
                    <span style={{ width: `${Math.max(2, percent)}%` }} />
                  </div>
                </div>
              )
            })}
          </div>
        ) : (
          <div className="empty-panel">暂无 Provider 用量</div>
        )}
      </section>
    </div>
  )
}
