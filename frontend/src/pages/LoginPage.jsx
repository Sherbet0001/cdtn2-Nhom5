import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { BriefcaseBusiness, LoaderCircle } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext.jsx';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const notice = location.state?.notice;

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setIsSubmitting(true);
    try {
      const user = await login({ email: email.trim(), password });
      navigate(user.role === 'EMPLOYER' ? '/employer/jobs' : '/jobs', { replace: true });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="mx-auto grid max-w-4xl overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm md:grid-cols-2">
      <div className="hidden flex-col justify-between bg-indigo-700 p-10 text-white md:flex">
        <BriefcaseBusiness size={30} aria-hidden="true" />
        <div>
          <p className="mb-3 text-sm font-semibold uppercase tracking-widest text-indigo-200">PartJob</p>
          <h1 className="text-3xl font-bold leading-tight">Bắt đầu hành trình việc làm phù hợp với bạn.</h1>
          <p className="mt-4 text-indigo-100">Tạo kết nối đáng tin cậy giữa sinh viên và nhà tuyển dụng.</p>
        </div>
        <p className="text-sm text-indigo-200">Nền tảng việc làm part-time cho sinh viên</p>
      </div>

      <div className="p-6 sm:p-10">
        <p className="text-sm font-semibold text-indigo-700 md:hidden">PartJob</p>
        <h1 className="mt-2 text-2xl font-bold text-slate-900">Đăng nhập</h1>
        <p className="mt-2 text-sm text-slate-600">Đăng nhập để quản lý hồ sơ và tài khoản của bạn.</p>

        {notice && <p className="notice-success mt-5" role="status">{notice}</p>}
        {error && <p className="notice-error mt-5" role="alert">{error}</p>}

        <form className="mt-6 space-y-5" onSubmit={handleSubmit}>
          <div>
            <label className="field-label" htmlFor="email">Email</label>
            <input
              autoComplete="email"
              className="field-input"
              id="email"
              name="email"
              onChange={(event) => setEmail(event.target.value)}
              required
              type="email"
              value={email}
            />
          </div>
          <div>
            <label className="field-label" htmlFor="password">Mật khẩu</label>
            <input
              autoComplete="current-password"
              className="field-input"
              id="password"
              name="password"
              onChange={(event) => setPassword(event.target.value)}
              required
              type="password"
              value={password}
            />
          </div>
          <button className="button-primary w-full" disabled={isSubmitting} type="submit">
            {isSubmitting && <LoaderCircle aria-hidden="true" className="animate-spin" size={18} />}
            {isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          Chưa có tài khoản? <Link className="font-semibold text-indigo-700 hover:underline" to="/register">Đăng ký</Link>
        </p>
      </div>
    </section>
  );
}
