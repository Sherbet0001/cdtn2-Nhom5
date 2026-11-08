import { useEffect, useState } from 'react';
import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { BriefcaseBusiness, LogOut, UserRound } from 'lucide-react';
import { useAuth } from './contexts/AuthContext.jsx';
import LoginPage from './pages/LoginPage.jsx';
import ProfilePage from './pages/ProfilePage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import JobsPage from './pages/JobsPage.jsx';
import JobDetailPage from './pages/JobDetailPage.jsx';
import EmployerJobsPage from './pages/EmployerJobsPage.jsx';
import JobFormPage from './pages/JobFormPage.jsx';
import JobApplicantsPage from './pages/JobApplicantsPage.jsx';
import MyApplicationsPage from './pages/MyApplicationsPage.jsx';
import NotificationsPage from './pages/NotificationsPage.jsx';
import EmployerApplicationsPage from './pages/EmployerApplicationsPage.jsx';
import notificationService from './services/notificationService.js';

function homeForUser(user) {
  return user?.role === 'EMPLOYER' ? '/employer/jobs' : '/jobs';
}

function AppLayout() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (!isAuthenticated || user.role !== 'STUDENT') {
      setUnreadCount(0);
      return;
    }
    const refreshUnreadCount = () => {
      notificationService.getMine().then((data) => setUnreadCount(data.unreadCount))
        .catch(() => setUnreadCount(0));
    };
    refreshUnreadCount();
    window.addEventListener('partjob:notifications-updated', refreshUnreadCount);
    return () => window.removeEventListener('partjob:notifications-updated', refreshUnreadCount);
  }, [isAuthenticated, user?.role, location.pathname]);

  async function handleLogout() {
    let notice = 'Bạn đã đăng xuất.';
    try {
      await logout();
    } catch (error) {
      notice = `Đã xóa phiên đăng nhập trên thiết bị, nhưng máy chủ không phản hồi: ${error.message}`;
    } finally {
      navigate('/login', { replace: true, state: { notice } });
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <nav className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4 sm:px-6">
          <Link className="flex items-center gap-2 font-bold text-indigo-700" to="/">
            <BriefcaseBusiness aria-hidden="true" size={22} />
            <span>PartJob</span>
          </Link>
          {isAuthenticated ? (
            <div className="flex items-center gap-3">
              <Link className="button-quiet hidden sm:inline-flex" to="/jobs">Việc làm</Link>
              {user.role === 'EMPLOYER' && <Link className="button-quiet hidden sm:inline-flex" to="/employer/jobs">Quản lý tin</Link>}
              {user.role === 'EMPLOYER' && <Link className="button-quiet hidden sm:inline-flex" to="/employer/applications">Đơn ứng tuyển</Link>}
              {user.role === 'STUDENT' && <Link className="button-quiet hidden sm:inline-flex" to="/applications/mine">Hồ sơ ứng tuyển</Link>}
              {user.role === 'STUDENT' && <Link className="button-quiet hidden sm:inline-flex" to="/notifications">Thông báo{unreadCount > 0 && <span className="ml-1 rounded-full bg-indigo-100 px-1.5 py-0.5 text-xs text-indigo-800">{unreadCount}</span>}</Link>}
              <Link className="button-quiet hidden sm:inline-flex" to="/profile">Hồ sơ</Link>
              <span className="hidden text-sm text-slate-600 sm:inline">
                <UserRound aria-hidden="true" className="mr-1 inline" size={16} />
                {user.fullName}
              </span>
              <button className="button-secondary" onClick={handleLogout} type="button">
                <LogOut aria-hidden="true" size={16} />
                Đăng xuất
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link className="button-quiet hidden sm:inline-flex" to="/jobs">Việc làm</Link>
              <Link className="button-quiet" to="/login">Đăng nhập</Link>
              <Link className="button-primary" to="/register">Tạo tài khoản</Link>
            </div>
          )}
          <div className="flex basis-full items-center justify-center gap-2 border-t border-slate-100 pt-2 sm:hidden">
            <Link className="button-quiet" to="/jobs">Việc làm</Link>
            {isAuthenticated && user.role === 'EMPLOYER' && <Link className="button-quiet" to="/employer/jobs">Quản lý tin</Link>}
            {isAuthenticated && user.role === 'EMPLOYER' && <Link className="button-quiet" to="/employer/applications">Đơn ứng tuyển</Link>}
            {isAuthenticated && user.role === 'STUDENT' && <Link className="button-quiet" to="/applications/mine">Ứng tuyển</Link>}
            {isAuthenticated && user.role === 'STUDENT' && <Link className="button-quiet" to="/notifications">Thông báo{unreadCount > 0 ? ` (${unreadCount})` : ''}</Link>}
            {isAuthenticated && <Link className="button-quiet" to="/profile">Hồ sơ</Link>}
          </div>
        </nav>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
        <Routes>
          <Route path="/" element={<JobsPage />} />
          <Route path="/jobs" element={<JobsPage />} />
          <Route path="/jobs/:jobId" element={<JobDetailPage />} />
          <Route path="/login" element={isAuthenticated ? <Navigate to={homeForUser(user)} replace /> : <LoginPage />} />
          <Route path="/register" element={isAuthenticated ? <Navigate to={homeForUser(user)} replace /> : <RegisterPage />} />
          <Route path="/profile" element={isAuthenticated ? <ProfilePage /> : <Navigate to="/login" replace />} />
          <Route path="/applications/mine" element={user?.role === 'STUDENT' ? <MyApplicationsPage /> : <Navigate to={isAuthenticated ? '/jobs' : '/login'} replace />} />
          <Route path="/notifications" element={user?.role === 'STUDENT' ? <NotificationsPage /> : <Navigate to={isAuthenticated ? '/jobs' : '/login'} replace />} />
          <Route path="/employer/jobs" element={user?.role === 'EMPLOYER' ? <EmployerJobsPage /> : <Navigate to={isAuthenticated ? '/jobs' : '/login'} replace />} />
          <Route path="/employer/applications" element={user?.role === 'EMPLOYER' ? <EmployerApplicationsPage /> : <Navigate to={isAuthenticated ? '/jobs' : '/login'} replace />} />
          <Route path="/employer/jobs/new" element={user?.role === 'EMPLOYER' ? <JobFormPage /> : <Navigate to={isAuthenticated ? '/jobs' : '/login'} replace />} />
          <Route path="/employer/jobs/:jobId/edit" element={user?.role === 'EMPLOYER' ? <JobFormPage /> : <Navigate to={isAuthenticated ? '/jobs' : '/login'} replace />} />
          <Route path="/employer/jobs/:jobId/applications" element={user?.role === 'EMPLOYER' ? <JobApplicantsPage /> : <Navigate to={isAuthenticated ? '/jobs' : '/login'} replace />} />
          <Route path="*" element={<Navigate to="/jobs" replace />} />
        </Routes>
      </main>
    </div>
  );
}

export default function App() {
  return <AppLayout />;
}
