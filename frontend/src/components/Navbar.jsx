import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import {
  Briefcase,
  Building2,
  ClipboardList,
  LogOut,
  PlusCircle,
  Search,
  ShieldCheck,
  UserCircle,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import NotificationBell from './NotificationBell';

const ROLE_BADGE_CONFIG = {
  STUDENT: {
    label: 'Sinh viên',
    className: 'bg-blue-100 text-blue-800 border-blue-200',
  },
  EMPLOYER: {
    label: 'Nhà tuyển dụng',
    className: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  },
  ADMIN: {
    label: 'Quản trị viên',
    className: 'bg-purple-100 text-purple-800 border-purple-200',
  },
};

const Navbar = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const roleBadge = user?.role ? ROLE_BADGE_CONFIG[user.role] : null;

  return (
    <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3.5 sm:px-6">
        <div className="flex items-center gap-6">
          <Link to="/jobs" className="flex items-center gap-2.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-red-600 text-white shadow-sm">
              <Briefcase className="h-5 w-5" />
            </div>
            <div>
              <span className="block text-base font-bold tracking-tight text-slate-900">
                VLU Part-time Job
              </span>
              <span className="hidden text-xs font-medium text-slate-500 sm:block">
                Nền tảng việc làm sinh viên Văn Lang
              </span>
            </div>
          </Link>

          <div className="hidden items-center gap-1 md:flex">
            <Link
              to="/jobs"
              className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-semibold transition ${
                location.pathname === '/jobs'
                  ? 'bg-red-50 text-red-700'
                  : 'text-slate-700 hover:bg-slate-100'
              }`}
            >
              <Search className="h-4 w-4" />
              <span>Tìm việc làm</span>
            </Link>

            {isAuthenticated && user?.role === 'STUDENT' && (
              <Link
                to="/applications/mine"
                className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-semibold transition ${
                  location.pathname === '/applications/mine'
                    ? 'bg-red-50 text-red-700'
                    : 'text-slate-700 hover:bg-slate-100'
                }`}
              >
                <ClipboardList className="h-4 w-4" />
                <span>Việc đã ứng tuyển</span>
              </Link>
            )}

            {isAuthenticated && user?.role === 'EMPLOYER' && (
              <>
                <Link
                  to="/employer/jobs"
                  className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-semibold transition ${
                    location.pathname.startsWith('/employer/jobs') &&
                    location.pathname !== '/employer/jobs/new'
                      ? 'bg-red-50 text-red-700'
                      : 'text-slate-700 hover:bg-slate-100'
                  }`}
                >
                  <Building2 className="h-4 w-4" />
                  <span>Quản lý tin đăng</span>
                </Link>
                <Link
                  to="/employer/jobs/new"
                  className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-semibold transition ${
                    location.pathname === '/employer/jobs/new'
                      ? 'bg-red-600 text-white shadow-sm'
                      : 'text-red-600 hover:bg-red-50'
                  }`}
                >
                  <PlusCircle className="h-4 w-4" />
                  <span>Đăng tin mới</span>
                </Link>
              </>
            )}

            {isAuthenticated && user?.role === 'ADMIN' && (
              <Link
                to="/admin"
                className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-semibold transition ${
                  location.pathname.startsWith('/admin')
                    ? 'bg-purple-100 text-purple-800'
                    : 'text-purple-700 hover:bg-purple-50'
                }`}
              >
                <ShieldCheck className="h-4 w-4" />
                <span>Quản trị hệ thống</span>
              </Link>
            )}
          </div>
        </div>

        <nav className="flex items-center gap-2.5">
          {isAuthenticated && user ? (
            <>
              <NotificationBell />

              <Link
                to="/profile"
                className={`inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition ${
                  location.pathname === '/profile'
                    ? 'bg-red-50 text-red-700'
                    : 'text-slate-700 hover:bg-slate-100'
                }`}
              >
                <UserCircle className="h-4 w-4" />
                <span className="max-w-[140px] truncate">{user.fullName}</span>
                {roleBadge && (
                  <span
                    className={`hidden rounded-full border px-2.5 py-0.5 text-xs font-semibold sm:inline-block ${roleBadge.className}`}
                  >
                    {roleBadge.label}
                  </span>
                )}
              </Link>

              <button
                type="button"
                onClick={handleLogout}
                className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:border-red-200 hover:bg-red-50 hover:text-red-700"
              >
                <LogOut className="h-4 w-4" />
                <span className="hidden sm:inline">Đăng xuất</span>
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className={`rounded-lg px-4 py-2 text-sm font-semibold transition ${
                  location.pathname === '/login'
                    ? 'bg-slate-900 text-white'
                    : 'text-slate-700 hover:bg-slate-100'
                }`}
              >
                Đăng nhập
              </Link>
              <Link
                to="/register"
                className={`inline-flex items-center gap-1.5 rounded-lg px-4 py-2 text-sm font-semibold transition ${
                  location.pathname === '/register'
                    ? 'bg-red-600 text-white shadow-sm'
                    : 'border border-red-600 text-red-600 hover:bg-red-50'
                }`}
              >
                <ShieldCheck className="h-4 w-4" />
                <span>Đăng ký</span>
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
};

export default Navbar;
