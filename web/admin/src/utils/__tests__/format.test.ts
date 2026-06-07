import { describe, it, expect } from 'vitest'
import { formatTokens } from '../format'

describe('formatTokens', () => {
  it('returns "-" for null', () => {
    expect(formatTokens(null)).toBe('-')
  })

  it('returns "-" for undefined', () => {
    expect(formatTokens(undefined)).toBe('-')
  })

  it('returns raw number for values below 1000', () => {
    expect(formatTokens(0)).toBe('0')
    expect(formatTokens(42)).toBe('42')
    expect(formatTokens(999)).toBe('999')
  })

  it('formats thousands with K suffix', () => {
    expect(formatTokens(1000)).toBe('1.0K')
    expect(formatTokens(1500)).toBe('1.5K')
    expect(formatTokens(99999)).toBe('100.0K')
  })

  it('formats millions with M suffix', () => {
    expect(formatTokens(1000000)).toBe('1.0M')
    expect(formatTokens(2500000)).toBe('2.5M')
  })
})
