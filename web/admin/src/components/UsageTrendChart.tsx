import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import type { AiUsageDailyPoint } from '../api/adminUsage'
import { formatTokens } from '../utils/format'

interface Props {
  data: AiUsageDailyPoint[]
}

export default function UsageTrendChart({ data }: Props) {
  return (
    <div className="card">
      <h3>每日 Token 用量趋势</h3>
      <ResponsiveContainer width="100%" height={260}>
        <AreaChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="date" tick={{ fontSize: 10 }} />
          <YAxis tickFormatter={formatTokens} tick={{ fontSize: 10 }} />
          <Tooltip formatter={(v: number) => formatTokens(v)} />
          <Area type="monotone" dataKey="totalTokens" stroke="#4f46e5" fill="#eef2ff" name="Total Tokens" strokeWidth={2} />
          <Area type="monotone" dataKey="totalInputTokens" stroke="#059669" fill="#ecfdf5" name="Input" strokeWidth={1.5} />
          <Area type="monotone" dataKey="totalOutputTokens" stroke="#d97706" fill="#fffbeb" name="Output" strokeWidth={1.5} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
