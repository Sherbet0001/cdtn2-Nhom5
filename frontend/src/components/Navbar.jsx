import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Briefcase, LogOut, ShieldCheck, UserCircle } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

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
        <Link to={isAuthenticated ? '/profile' : '/login'} className="flex items-center gap-2.5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-red-600 text-white shadow-sm">
            <Briefcase className="h-5 w-5" />
          </div>
          <div>
            <span className="block text-base font-bold tracking-tight text-slate-900">
              VLU Part-time Job
            </span>
            <span className="block text-xs font-medium text-slate-500">
              Nền tảng việc làm sinh viên Văn Lang
            </span>
          </div>
        </Link>

        <nav className="flex items-center gap-3">
          {isAuthenticated && user ? (
            <>
              <Link
                to="/profile"
                className={`inline-flex items-center gap-2 rounded-lg px-3.5 py-2 text-sm font-medium transition ${
                  location.pathname === '/profile'
                    ? 'bg-red-50 text-red-700'
                    : 'text-slate-700 hover:bg-slate-100'
                }`}
              >
                <UserCircle className="h-4 w-4" />
                <span>{user.fullName}</span>
                {roleBadge && (
                  <span
                    className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold ${roleBadge.className}`}
                  >
                    {roleBadge.label}
                  </span>
                )}
              </Link>

              <button
                type="button"
                onClick={handleLogout}
                className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3.5 py-2 text-sm font-medium text-slate-700 transition hover:border-red-200 hover:bg-red-50 hover:text-red-700"
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
