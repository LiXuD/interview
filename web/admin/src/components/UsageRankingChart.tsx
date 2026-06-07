import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import type { AiUsageBreakdown } from '../api/adminUsage'
import { formatTokens } from '../utils/format'

interface Props {
  title: string
  data: AiUsageBreakdown[]
}

export default function UsageRankingChart({ title, data }: Props) {
  const chartData = data.slice(0, 8).map(d => ({
    name: d.name.length > 20 ? d.name.slice(0, 20) + '...' : d.name,
    tokens: d.totalTokens,
    requests: d.totalRequests,
  }))

  return (
    <div className="card">
      <h3>{title}</h3>
      <ResponsiveContainer width="100%" height={260}>
        <BarChart data={chartData} layout="vertical">
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis type="number" tickFormatter={formatTokens} tick={{ fontSize: 10 }} />
          <YAxis type="category" dataKey="name" width={110} tick={{ fontSize: 10 }} />
          <Tooltip formatter={(v: number) => formatTokens(v)} />
          <Bar dataKey="tokens" fill="#4f46e5" name="Tokens" radius={[0, 4, 4, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
