import { useCallback, useEffect, useState } from 'react';
import { ArrowRight, BriefcaseBusiness, CirclePlus, Clock3, MapPin, Pencil, Power, RefreshCw, UserRound } from 'lucide-react';
import { Link } from 'react-router-dom';
import jobService from '../services/jobService.js';

const typeLabels = {
  PART_TIME: 'Bán thời gian',
  FULL_TIME: 'Toàn thời gian',
  INTERNSHIP: 'Thực tập',
  CONTRACT: 'Hợp đồng',
};
const money = new Intl.NumberFormat('vi-VN');

function salaryLabel(job) {
  if (job.salaryMin == null && job.salaryMax == null) return 'Thỏa thuận';
  if (job.salaryMin != null && job.salaryMax != null) return `${money.format(job.salaryMin)} – ${money.format(job.salaryMax)} ₫`;
  return `${job.salaryMin == null ? 'Tối đa' : 'Từ'} ${money.format(job.salaryMin ?? job.salaryMax)} ₫`;
}

export default function EmployerJobsPage() {
  const [jobs, setJobs] = useState([]);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);

  const loadJobs = useCallback(async () => {
    setError('');
    setLoading(true);
    try {
      setJobs(await jobService.getMine());
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadJobs(); }, [loadJobs]);

  async function toggleStatus(job) {
    setError('');
    setNotice('');
    setUpdatingId(job.id);
    try {
      const updated = await jobService.updateStatus(job.id, job.status === 'OPEN' ? 'CLOSED' : 'OPEN');
      setJobs((current) => current.map((item) => item.id === job.id ? updated : item));
      setNotice(updated.status === 'OPEN' ? 'Đã mở lại tin tuyển dụng.' : 'Đã đóng tin tuyển dụng.');
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setUpdatingId(null);
    }
  }

  const openCount = jobs.filter((job) => job.status === 'OPEN').length;

  return (
    <div className="space-y-7">
      <section className="flex flex-col justify-between gap-5 rounded-3xl bg-gradient-to-r from-slate-900 to-indigo-950 p-6 text-white sm:flex-row sm:items-center sm:p-9">
        <div>
          <p className="text-sm font-semibold text-indigo-200">Khu vực nhà tuyển dụng</p>
          <h1 className="mt-2 text-3xl font-extrabold">Tin tuyển dụng của bạn</h1>
          <p className="mt-2 text-sm text-slate-300">Quản lý các cơ hội việc làm và tìm ứng viên phù hợp.</p>
          <Link className="mt-3 inline-flex text-sm font-semibold text-indigo-200 hover:text-white" to="/profile">Cập nhật thông tin doanh nghiệp <ArrowRight className="ml-1" size={15} /></Link>
          <div className="mt-5 flex gap-3 text-sm">
            <span className="rounded-lg bg-white/10 px-3 py-2"><strong>{jobs.length}</strong> tổng tin</span>
            <span className="rounded-lg bg-emerald-400/15 px-3 py-2 text-emerald-200"><strong>{openCount}</strong> đang tuyển</span>
          </div>
        </div>
        <Link className="button-primary !bg-white !text-indigo-800 hover:!bg-indigo-50" to="/employer/jobs/new"><CirclePlus size={18} /> Đăng tin mới</Link>
      </section>

      {error && <p className="notice-error" role="alert">{error}</p>}
      {notice && <p className="notice-success" role="status">{notice}</p>}
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
          <div><h2 className="font-bold text-slate-900">Danh sách tin</h2><p className="mt-1 text-xs text-slate-500">Chỉnh sửa nội dung hoặc đóng tin khi đã tuyển đủ.</p></div>
          <button aria-label="Tải lại danh sách" className="button-secondary !p-2" onClick={loadJobs} type="button"><RefreshCw size={16} /></button>
        </div>
        {loading ? (
          <p className="p-10 text-center text-sm text-slate-500">Đang tải tin tuyển dụng...</p>
        ) : jobs.length === 0 ? (
          <div className="px-6 py-14 text-center">
            <BriefcaseBusiness aria-hidden="true" className="mx-auto text-slate-300" size={36} />
            <h3 className="mt-4 font-semibold text-slate-800">Bạn chưa đăng tin tuyển dụng nào</h3>
            <p className="mt-1 text-sm text-slate-500">Tạo tin đầu tiên để bắt đầu tiếp cận ứng viên.</p>
            <Link className="button-primary mt-5" to="/employer/jobs/new"><CirclePlus size={17} /> Đăng tin tuyển dụng</Link>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {jobs.map((job) => (
              <article className="flex flex-col gap-4 p-5 transition hover:bg-slate-50/70 sm:flex-row sm:items-center" key={job.id}>
                <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-indigo-50 text-indigo-700"><BriefcaseBusiness size={20} /></div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="font-bold text-slate-900">{job.title}</h3>
                    <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${job.status === 'OPEN' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>{job.status === 'OPEN' ? 'Đang tuyển' : 'Đã đóng'}</span>
                  </div>
                  <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">
                    <span>{typeLabels[job.jobType]}</span><span>{salaryLabel(job)}</span>
                    <span className="inline-flex items-center gap-1"><MapPin size={13} />{job.location ? `${job.location.district}, ${job.location.city}` : 'Linh hoạt'}</span>
                    <span className="inline-flex items-center gap-1"><Clock3 size={13} />Đăng {new Date(job.postedAt).toLocaleDateString('vi-VN')}</span>
                  </div>
                </div>
                <div className="flex items-center gap-2 sm:justify-end">
                  <Link aria-label={`Hồ sơ ứng viên ${job.title}`} className="button-secondary !px-3 !py-2" to={`/employer/jobs/${job.id}/applications`}><UserRound size={16} /><span className="hidden md:inline">Ứng viên</span></Link>
                  <Link aria-label={`Xem tin ${job.title}`} className="button-secondary !px-3 !py-2" to={`/jobs/${job.id}`}><ArrowRight size={16} /> <span className="hidden md:inline">Xem tin</span></Link>
                  <Link aria-label={`Sửa tin ${job.title}`} className="button-secondary !px-3 !py-2" to={`/employer/jobs/${job.id}/edit`}><Pencil size={16} /> <span className="hidden md:inline">Sửa</span></Link>
                  <button aria-label={job.status === 'OPEN' ? `Đóng tin ${job.title}` : `Mở lại tin ${job.title}`} className="button-secondary !px-3 !py-2" disabled={updatingId === job.id} onClick={() => toggleStatus(job)} type="button"><Power size={16} /><span className="hidden md:inline">{job.status === 'OPEN' ? 'Đóng tin' : 'Mở lại'}</span></button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
