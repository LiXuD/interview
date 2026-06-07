import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateTokenQuota } from '../api/adminUsage'
import { formatTokens } from '../utils/format'

interface Props {
  userId: string
  currentQuota: number | null
  onUpdated?: () => void
}

export default function QuotaEditor({ userId, currentQuota, onUpdated }: Props) {
  const [value, setValue] = useState(currentQuota != null ? String(currentQuota) : '')
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (quota: number | null) => updateTokenQuota(userId, quota),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] })
      queryClient.invalidateQueries({ queryKey: ['admin-overview'] })
      queryClient.invalidateQueries({ queryKey: ['admin-user-detail', userId] })
      onUpdated?.()
    },
  })

  function handleSave() {
    const trimmed = value.trim()
    if (trimmed === '') {
      mutation.mutate(null)
    } else {
      const num = parseInt(trimmed, 10)
      if (!isNaN(num) && num >= 0) {
        mutation.mutate(num)
      }
    }
  }

  return (
    <div className="quota-editor">
      <input
        type="number"
        min="0"
        value={value}
        onChange={e => setValue(e.target.value)}
        placeholder="不限制"
        style={{ width: 140 }}
      />
      <button className="btn-primary" onClick={handleSave} disabled={mutation.isPending}>
        {mutation.isPending ? '...' : '保存'}
      </button>
      <button className="btn-ghost" onClick={() => mutation.mutate(null)} disabled={mutation.isPending}>
        不限制
      </button>
      {currentQuota != null && (
        <span className="mono" style={{ fontSize: 12, color: 'var(--text-tertiary)' }}>
          当前: {formatTokens(currentQuota)}
        </span>
      )}
      {mutation.isError && <span style={{ fontSize: 12, color: 'var(--danger)' }}>更新失败</span>}
    </div>
  )
}
