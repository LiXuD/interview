import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import KpiCard from '../KpiCard'

describe('KpiCard', () => {
  it('renders label and value', () => {
    render(<KpiCard label="总用户" value={42} />)
    expect(screen.getByText('总用户')).toBeInTheDocument()
    expect(screen.getByText('42')).toBeInTheDocument()
  })

  it('formats numeric value with locale separators', () => {
    render(<KpiCard label="Token" value={1234567} />)
    expect(screen.getByText('1,234,567')).toBeInTheDocument()
  })

  it('renders string value as-is', () => {
    render(<KpiCard label="成功率" value="95%" />)
    expect(screen.getByText('95%')).toBeInTheDocument()
  })

  it('renders sub text when provided', () => {
    render(<KpiCard label="活跃" value={10} sub="本周期" />)
    expect(screen.getByText('本周期')).toBeInTheDocument()
  })

  it('does not render sub text when omitted', () => {
    const { container } = render(<KpiCard label="活跃" value={10} />)
    expect(container.querySelector('.sub')).toBeNull()
  })
})
