import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  CheckCircle2,
  KeyRound,
  Loader2,
  Lock,
  Mail,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

const LoginPage = () => {
  const { login, authBannerMessage, setAuthBannerMessage } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [formData, setFormData] = useState({
    email: location.state?.registeredEmail || '',
    password: '',
  });

  // 3-State UI Pattern: Loading, Error, Empty/Ready
  const [isLoading, setIsLoading] = useState(false);
  const [errorState, setErrorState] = useState(null);
  const [noticeMessage, setNoticeMessage] = useState(location.state?.notice || '');

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errorState) setErrorState(null);
    if (authBannerMessage) setAuthBannerMessage('');
  };

  const handleRoleRedirect = (role) => {
    // Chuyển hướng theo vai trò kèm thông báo ngữ cảnh trên trang Hồ sơ
    if (role === 'STUDENT') {
      navigate('/profile?role=student');
    } else if (role === 'EMPLOYER') {
      navigate('/profile?role=employer');
    } else {
      navigate('/profile?role=admin');
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setErrorState(null);
    setNoticeMessage('');
    setIsLoading(true);

    try {
      const loggedInUser = await login({
        email: formData.email.trim(),
        password: formData.password,
      });
      handleRoleRedirect(loggedInUser.role);
    } catch (error) {
      setErrorState({
        message: error.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại email và mật khẩu.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="mx-auto flex min-h-[calc(100vh-76px)] max-w-md items-center px-4 py-10">
      <div className="w-full rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="mb-6 text-center">
          <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-red-50 text-red-600">
            <KeyRound className="h-6 w-6" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900">Đăng nhập hệ thống</h1>
          <p className="mt-1.5 text-sm text-slate-600">
            Xác thực JWT Stateless (Thời hạn phiên 2 giờ - FR-02, FR-04)
          </p>
        </div>

        {noticeMessage && (
          <div className="mb-5 flex items-center gap-2.5 rounded-xl border border-emerald-200 bg-emerald-50 p-3.5 text-sm font-medium text-emerald-800">
            <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600" />
            <span>{noticeMessage}</span>
          </div>
        )}

        {authBannerMessage && (
          <div className="mb-5 flex items-center gap-2.5 rounded-xl border border-amber-200 bg-amber-50 p-3.5 text-sm font-medium text-amber-900">
            <AlertCircle className="h-5 w-5 shrink-0 text-amber-600" />
            <span>{authBannerMessage}</span>
          </div>
        )}

        {/* Trạng thái Lỗi (Error State) kèm mã requestId và nút Thử lại */}
        {errorState && (
          <div
            role="alert"
            className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"
          >
            <div className="flex items-start gap-2.5">
              <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
              <div className="flex-1">
                <p className="font-semibold">{errorState.message}</p>
                <div className="mt-2 flex items-center justify-between border-t border-red-200/70 pt-2 text-xs text-red-600">
                  <span>RequestId: {errorState.requestId}</span>
                  <button
                    type="button"
                    onClick={() => setErrorState(null)}
                    className="font-semibold underline hover:text-red-800"
                  >
                    Thử lại
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="email" className="mb-1.5 block text-sm font-semibold text-slate-700">
              Email đăng nhập
            </label>
            <div className="relative">
              <Mail className="pointer-events-none absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
              <input
                id="email"
                name="email"
                type="email"
                required
                disabled={isLoading}
                value={formData.email}
                onChange={handleChange}
                placeholder="nhap-email@vanlanguni.vn"
                className="w-full rounded-xl border border-slate-300 py-2.5 pl-10 pr-4 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
              />
            </div>
          </div>

          <div>
            <label
              htmlFor="password"
              className="mb-1.5 block text-sm font-semibold text-slate-700"
            >
              Mật khẩu
            </label>
            <div className="relative">
              <Lock className="pointer-events-none absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
              <input
                id="password"
                name="password"
                type="password"
                required
                disabled={isLoading}
                value={formData.password}
                onChange={handleChange}
                placeholder="••••••••"
                className="w-full rounded-xl border border-slate-300 py-2.5 pl-10 pr-4 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
              />
            </div>
          </div>

          {/* Trạng thái Đang tải (Loading State): Khóa nút tránh nhấn lặp */}
          <button
            type="submit"
            disabled={isLoading}
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-slate-900 px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isLoading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                <span>Đang xác thực...</span>
              </>
            ) : (
              <span>Đăng nhập</span>
            )}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          Chưa có tài khoản?{' '}
          <Link to="/register" className="font-semibold text-red-600 hover:underline">
            Đăng ký tài khoản mới
          </Link>
        </p>
      </div>
    </div>
  );
};

export default LoginPage;
