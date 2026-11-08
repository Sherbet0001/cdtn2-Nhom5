import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { LoaderCircle } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext.jsx';

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
    role: 'STUDENT',
  });
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setIsSubmitting(true);
    try {
      await register({ ...form, fullName: form.fullName.trim(), email: form.email.trim() });
      navigate('/login', { replace: true, state: { notice: 'Tạo tài khoản thành công. Hãy đăng nhập để tiếp tục.' } });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="mx-auto max-w-xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-10">
      <p className="text-sm font-semibold text-indigo-700">PartJob</p>
      <h1 className="mt-2 text-2xl font-bold text-slate-900">Tạo tài khoản</h1>
      <p className="mt-2 text-sm text-slate-600">Đăng ký với vai trò sinh viên hoặc nhà tuyển dụng.</p>

      {error && <p className="notice-error mt-5" role="alert">{error}</p>}

      <form className="mt-6 space-y-5" onSubmit={handleSubmit}>
        <div>
          <label className="field-label" htmlFor="fullName">Họ và tên</label>
          <input
            autoComplete="name"
            className="field-input"
            id="fullName"
            maxLength={100}
            name="fullName"
            onChange={updateField}
            required
            value={form.fullName}
          />
        </div>
        <div>
          <label className="field-label" htmlFor="registerEmail">Email</label>
          <input
            autoComplete="email"
            className="field-input"
            id="registerEmail"
            name="email"
            onChange={updateField}
            required
            type="email"
            value={form.email}
          />
        </div>
        <div>
          <label className="field-label" htmlFor="registerPassword">Mật khẩu</label>
          <input
            autoComplete="new-password"
            className="field-input"
            id="registerPassword"
            minLength={8}
            name="password"
            onChange={updateField}
            required
            type="password"
            value={form.password}
          />
          <p className="mt-1 text-xs text-slate-500">Tối thiểu 8 ký tự.</p>
        </div>
        <fieldset>
          <legend className="field-label">Bạn đăng ký với vai trò</legend>
          <div className="grid gap-3 sm:grid-cols-2">
            {[
              ['STUDENT', 'Sinh viên'],
              ['EMPLOYER', 'Nhà tuyển dụng'],
            ].map(([value, label]) => (
              <label
                className={`flex cursor-pointer items-center gap-3 rounded-lg border p-3 text-sm ${form.role === value ? 'border-indigo-500 bg-indigo-50 text-indigo-800' : 'border-slate-200 text-slate-700'}`}
                key={value}
              >
                <input
                  checked={form.role === value}
                  className="accent-indigo-600"
                  name="role"
                  onChange={updateField}
                  type="radio"
                  value={value}
                />
                {label}
              </label>
            ))}
          </div>
        </fieldset>
        <button className="button-primary w-full" disabled={isSubmitting} type="submit">
          {isSubmitting && <LoaderCircle aria-hidden="true" className="animate-spin" size={18} />}
          {isSubmitting ? 'Đang tạo tài khoản...' : 'Đăng ký'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-600">
        Đã có tài khoản? <Link className="font-semibold text-indigo-700 hover:underline" to="/login">Đăng nhập</Link>
      </p>
    </section>
  );
}
