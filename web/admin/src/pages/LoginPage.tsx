import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { setToken } from '../auth/AuthStore'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault()
    if (!username.trim()) return
    setLoading(true)
    setError('')
    try {
      const res = await fetch('/api/auth/dev-login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username.trim() }),
      })
      if (!res.ok) {
        const body = await res.json().catch(() => ({ message: 'Login failed' }))
        throw new Error(body.message)
      }
      const data = await res.json()
      setToken(data.token)
      navigate('/', { replace: true })
    } catch (err: any) {
      setError(err.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={handleLogin}>
        <h1>AI 面试教练</h1>
        <p className="subtitle">Token 用量管理后台</p>
        <div className="field">
          <label>管理员用户名</label>
          <input
            value={username}
            onChange={e => setUsername(e.target.value)}
            placeholder="输入管理员用户名"
            autoFocus
          />
        </div>
        <button className="btn-primary" type="submit" disabled={loading} style={{ width: '100%' }}>
          {loading ? '登录中...' : '登录'}
        </button>
        {error && <div className="error">{error}</div>}
      </form>
    </div>
  )
}
