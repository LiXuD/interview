import { Routes, Route, Navigate, useNavigate } from 'react-router-dom'
import { isAuthenticated, clearToken } from './auth/AuthStore'
import LoginPage from './pages/LoginPage'
import UsageDashboardPage from './pages/UsageDashboardPage'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  if (!isAuthenticated()) return <Navigate to="/login" replace />
  return <>{children}</>
}

function AppLayout() {
  const navigate = useNavigate()

  function handleLogout() {
    clearToken()
    navigate('/login', { replace: true })
  }

  return (
    <ProtectedRoute>
      <div className="app-header">
        <h1><span>AI 面试教练</span> Token 用量管理</h1>
        <div className="user-info">
          <button className="btn-ghost" onClick={handleLogout}>退出登录</button>
        </div>
      </div>
      <div className="app-content">
        <UsageDashboardPage />
      </div>
    </ProtectedRoute>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/*" element={<AppLayout />} />
    </Routes>
  )
}
