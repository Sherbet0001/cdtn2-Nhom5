import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  Building2,
  CheckCircle2,
  GraduationCap,
  Loader2,
  Lock,
  Mail,
  User,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

const RegisterPage = () => {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: 'STUDENT',
  });

  // 3-State UI Pattern: Loading, Error, Success/Empty
  const [isLoading, setIsLoading] = useState(false);
  const [errorState, setErrorState] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errorState) {
      setErrorState(null);
    }
  };

  const handleRoleSelect = (selectedRole) => {
    setFormData((prev) => ({ ...prev, role: selectedRole }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setErrorState(null);
    setSuccessMessage('');

    if (formData.password !== formData.confirmPassword) {
      setErrorState({
        message: 'Mật khẩu xác nhận không khớp với mật khẩu đã nhập.',
        requestId: 'CLIENT_VALIDATION',
      });
      return;
    }

    setIsLoading(true);
    try {
      await register({
        fullName: formData.fullName.trim(),
        email: formData.email.trim(),
        password: formData.password,
        role: formData.role,
      });

      setSuccessMessage('Đăng ký tài khoản thành công! Đang chuyển hướng đến trang đăng nhập...');
      setTimeout(() => {
        navigate('/login', {
          state: {
            registeredEmail: formData.email.trim(),
            notice: 'Tài khoản của bạn đã được tạo thành công. Vui lòng đăng nhập!',
          },
        });
      }, 1000);
    } catch (error) {
      setErrorState({
        message: error.message || 'Đăng ký tài khoản thất bại, vui lòng kiểm tra lại thông tin.',
        errors: error.errors || [],
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="mx-auto flex min-h-[calc(100vh-76px)] max-w-xl items-center px-4 py-10">
      <div className="w-full rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-slate-900">Đăng ký tài khoản mới</h1>
          <p className="mt-1.5 text-sm text-slate-600">
            Chọn vai trò phù hợp để kết nối việc làm part-time nhanh chóng (FR-01)
          </p>
        </div>

        {/* Trạng thái Lỗi (Error State) kèm requestId và nút Thử lại */}
        {errorState && (
          <div
            role="alert"
            className="mb-6 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"
          >
            <div className="flex items-start gap-2.5">
              <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
              <div className="flex-1">
                <p className="font-semibold">{errorState.message}</p>
                {errorState.errors?.length > 0 && (
                  <ul className="mt-1.5 list-inside list-disc space-y-1 text-xs text-red-700">
                    {errorState.errors.map((err, idx) => (
                      <li key={idx}>{err.message}</li>
                    ))}
                  </ul>
                )}
                <div className="mt-2 flex items-center justify-between border-t border-red-200/70 pt-2 text-xs text-red-600">
                  <span>Mã tra cứu log (RequestId): {errorState.requestId}</span>
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

        {/* Trạng thái Thành công */}
        {successMessage && (
          <div className="mb-6 flex items-center gap-2.5 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-800">
            <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600" />
            <span>{successMessage}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Chọn vai trò RBAC */}
          <div>
            <label className="mb-2 block text-sm font-semibold text-slate-700">
              Bạn đăng ký với vai trò nào?
            </label>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                disabled={isLoading}
                onClick={() => handleRoleSelect('STUDENT')}
                className={`flex flex-col items-center gap-1.5 rounded-xl border p-3.5 text-center transition ${
                  formData.role === 'STUDENT'
                    ? 'border-red-600 bg-red-50/70 text-red-900 ring-1 ring-red-600'
                    : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                }`}
              >
                <GraduationCap
                  className={`h-6 w-6 ${
                    formData.role === 'STUDENT' ? 'text-red-600' : 'text-slate-500'
                  }`}
                />
                <span className="text-sm font-bold">Sinh viên (STUDENT)</span>
                <span className="text-xs text-slate-500">Tìm việc làm thêm & nộp CV</span>
              </button>

              <button
                type="button"
                disabled={isLoading}
                onClick={() => handleRoleSelect('EMPLOYER')}
                className={`flex flex-col items-center gap-1.5 rounded-xl border p-3.5 text-center transition ${
                  formData.role === 'EMPLOYER'
                    ? 'border-emerald-600 bg-emerald-50/70 text-emerald-900 ring-1 ring-emerald-600'
                    : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                }`}
              >
                <Building2
                  className={`h-6 w-6 ${
                    formData.role === 'EMPLOYER' ? 'text-emerald-600' : 'text-slate-500'
                  }`}
                />
                <span className="text-sm font-bold">Nhà tuyển dụng (EMPLOYER)</span>
                <span className="text-xs text-slate-500">Đăng tin & tuyển sinh viên</span>
              </button>
            </div>
          </div>

          <div>
            <label htmlFor="fullName" className="mb-1.5 block text-sm font-semibold text-slate-700">
              {formData.role === 'EMPLOYER' ? 'Họ và tên người đại diện' : 'Họ và tên sinh viên'}
            </label>
            <div className="relative">
              <User className="pointer-events-none absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
              <input
                id="fullName"
                name="fullName"
                type="text"
                required
                disabled={isLoading}
                value={formData.fullName}
                onChange={handleChange}
                placeholder="Nhập đầy đủ họ và tên..."
                className="w-full rounded-xl border border-slate-300 py-2.5 pl-10 pr-4 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
              />
            </div>
          </div>

          <div>
            <label htmlFor="email" className="mb-1.5 block text-sm font-semibold text-slate-700">
              Địa chỉ Email
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
                placeholder="Ví dụ: sinhvien@vanlanguni.vn"
                className="w-full rounded-xl border border-slate-300 py-2.5 pl-10 pr-4 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label
                htmlFor="password"
                className="mb-1.5 block text-sm font-semibold text-slate-700"
              >
                Mật khẩu (tối thiểu 8 ký tự)
              </label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
                <input
                  id="password"
                  name="password"
                  type="password"
                  required
                  minLength={8}
                  disabled={isLoading}
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="••••••••"
                  className="w-full rounded-xl border border-slate-300 py-2.5 pl-10 pr-4 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
                />
              </div>
            </div>

            <div>
              <label
                htmlFor="confirmPassword"
                className="mb-1.5 block text-sm font-semibold text-slate-700"
              >
                Xác nhận mật khẩu
              </label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type="password"
                  required
                  minLength={8}
                  disabled={isLoading}
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  placeholder="••••••••"
                  className="w-full rounded-xl border border-slate-300 py-2.5 pl-10 pr-4 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
                />
              </div>
            </div>
          </div>

          {/* Trạng thái Đang tải (Loading State): Khóa nút submit để tránh double-submit */}
          <button
            type="submit"
            disabled={isLoading}
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-red-600 px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isLoading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                <span>Đang xử lý đăng ký...</span>
              </>
            ) : (
              <span>Hoàn tất đăng ký tài khoản</span>
            )}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          Đã có tài khoản trên hệ thống?{' '}
          <Link to="/login" className="font-semibold text-red-600 hover:underline">
            Đăng nhập ngay
          </Link>
        </p>
      </div>
    </div>
  );
};

export default RegisterPage;
