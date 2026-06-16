import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import TokenCompositionPanel from '../TokenCompositionPanel'
import type { AiUsageBreakdown, AiUsageSummary } from '../../api/adminUsage'

const summary: AiUsageSummary = {
  totalRequests: 8,
  successfulRequests: 7,
  failedRequests: 1,
  estimatedRequests: 0,
  totalInputTokens: 12000,
  totalOutputTokens: 6000,
  totalCacheCreationTokens: 900,
  totalCacheReadTokens: 300,
  totalReasoningTokens: 700,
  totalTokens: 19900,
}

const providers: AiUsageBreakdown[] = [
  {
    name: 'platformDefault',
    totalRequests: 6,
    successfulRequests: 6,
    failedRequests: 0,
    estimatedRequests: 0,
    totalInputTokens: 9000,
    totalOutputTokens: 4500,
    totalCacheCreationTokens: 900,
    totalCacheReadTokens: 300,
    totalReasoningTokens: 700,
    totalTokens: 15400,
  },
  {
    name: 'userOpenAICompatible',
    totalRequests: 2,
    successfulRequests: 1,
    failedRequests: 1,
    estimatedRequests: 0,
    totalInputTokens: 3000,
    totalOutputTokens: 1500,
    totalCacheCreationTokens: 0,
    totalCacheReadTokens: 0,
    totalReasoningTokens: 0,
    totalTokens: 4500,
  },
]

describe('TokenCompositionPanel', () => {
  it('shows visible token composition without relying on chart hover', () => {
    render(<TokenCompositionPanel summary={summary} providers={providers} />)

    expect(screen.getByText('Token 构成')).toBeInTheDocument()
    expect(screen.getByText('Input')).toBeInTheDocument()
    expect(screen.getByText('Output')).toBeInTheDocument()
    expect(screen.getByText('Cache create')).toBeInTheDocument()
    expect(screen.getByText('Cache read')).toBeInTheDocument()
    expect(screen.getByText('Reasoning')).toBeInTheDocument()
    expect(screen.getByText('19.9K')).toBeInTheDocument()
  })

  it('shows provider token totals and request counts', () => {
    render(<TokenCompositionPanel summary={summary} providers={providers} />)

    expect(screen.getByText('Provider 消耗')).toBeInTheDocument()
    expect(screen.getByText('platformDefault')).toBeInTheDocument()
    expect(screen.getByText('userOpenAICompatible')).toBeInTheDocument()
    expect(screen.getByText('6 req')).toBeInTheDocument()
    expect(screen.getByText('2 req')).toBeInTheDocument()
  })
})
