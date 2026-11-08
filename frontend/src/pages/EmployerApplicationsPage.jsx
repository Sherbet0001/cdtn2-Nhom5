import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, BriefcaseBusiness, RefreshCw, Search, UserRound } from 'lucide-react';
import { Link } from 'react-router-dom';
import applicationService from '../services/applicationService.js';
import { ApplicantCard } from './JobApplicantsPage.jsx';

const statusLabels = {
  NEW: 'Chờ xem xét',
  INTERVIEW: 'Đã mời phỏng vấn',
  REJECTED: 'Đã từ chối',
};

export default function EmployerApplicationsPage() {
  const [applications, setApplications] = useState([]);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [jobFilter, setJobFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);

  async function loadApplications() {
    setError('');
    setLoading(true);
    try {
      setApplications(await applicationService.getForEmployer());
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadApplications(); }, []);

  const jobs = useMemo(() => {
    const uniqueJobs = new Map();
    applications.forEach((application) => uniqueJobs.set(application.jobId, application.jobTitle));
    return [...uniqueJobs].map(([id, title]) => ({ id, title }));
  }, [applications]);

  const filteredApplications = useMemo(() => {
    const normalizedSearch = search.trim().toLocaleLowerCase('vi');
    return applications.filter((application) => {
      const matchesStatus = statusFilter === 'ALL' || application.status === statusFilter;
      const matchesJob = jobFilter === 'ALL' || String(application.jobId) === jobFilter;
      const matchesSearch = !normalizedSearch || [
        application.student?.fullName,
        application.student?.email,
        application.jobTitle,
      ].some((value) => value?.toLocaleLowerCase('vi').includes(normalizedSearch));
      return matchesStatus && matchesJob && matchesSearch;
    });
  }, [applications, jobFilter, search, statusFilter]);

  async function decide(applicationId, decision) {
    setError('');
    setBusyId(applicationId);
    try {
      const updated = await applicationService.decide(applicationId, decision);
      setApplications((current) => current.map((item) => item.id === applicationId ? updated : item));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusyId(null);
    }
  }

  const pendingCount = applications.filter((application) => application.status === 'NEW').length;

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <Link className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-indigo-700" to="/employer/jobs">
        <ArrowLeft size={16} /> Quản lý tin
      </Link>

      <section className="flex flex-col justify-between gap-5 rounded-3xl bg-gradient-to-r from-slate-900 to-indigo-950 p-6 text-white sm:flex-row sm:items-center sm:p-8">
        <div>
          <p className="text-sm font-semibold text-indigo-200">Khu vực nhà tuyển dụng</p>
          <h1 className="mt-2 text-3xl font-extrabold">Đơn ứng tuyển</h1>
          <p className="mt-2 text-sm text-slate-300">Xem và xử lý hồ sơ ứng tuyển trên tất cả tin tuyển dụng của bạn.</p>
          <div className="mt-5 flex gap-3 text-sm">
            <span className="rounded-lg bg-white/10 px-3 py-2"><strong>{applications.length}</strong> tổng hồ sơ</span>
            <span className="rounded-lg bg-amber-400/15 px-3 py-2 text-amber-200"><strong>{pendingCount}</strong> chờ xem xét</span>
          </div>
        </div>
        <button aria-label="Tải lại đơn ứng tuyển" className="button-primary !bg-white !text-indigo-800 hover:!bg-indigo-50" disabled={loading} onClick={loadApplications} type="button">
          <RefreshCw className={loading ? 'animate-spin' : ''} size={17} /> Tải lại
        </button>
      </section>

      {error && <p className="notice-error" role="alert">{error}</p>}

      <section className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[1fr_220px_200px]">
        <label className="relative block">
          <span className="sr-only">Tìm ứng viên hoặc vị trí</span>
          <Search aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={17} />
          <input className="field-input pl-10" onChange={(event) => setSearch(event.target.value)} placeholder="Tên, email ứng viên hoặc vị trí" value={search} />
        </label>
        <label>
          <span className="sr-only">Lọc theo trạng thái</span>
          <select className="field-input" onChange={(event) => setStatusFilter(event.target.value)} value={statusFilter}>
            <option value="ALL">Tất cả trạng thái</option>
            {Object.entries(statusLabels).map(([status, label]) => <option key={status} value={status}>{label}</option>)}
          </select>
        </label>
        <label>
          <span className="sr-only">Lọc theo tin tuyển dụng</span>
          <select className="field-input" onChange={(event) => setJobFilter(event.target.value)} value={jobFilter}>
            <option value="ALL">Tất cả vị trí</option>
            {jobs.map((job) => <option key={job.id} value={job.id}>{job.title}</option>)}
          </select>
        </label>
      </section>

      {loading ? (
        <p className="py-12 text-center text-sm text-slate-500">Đang tải đơn ứng tuyển...</p>
      ) : filteredApplications.length ? (
        <>
          <p className="text-sm text-slate-500">Hiển thị {filteredApplications.length} / {applications.length} hồ sơ</p>
          <div className="space-y-4">
            {filteredApplications.map((application) => (
              <div className="space-y-2" key={application.id}>
                <Link className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-700 hover:underline" to={`/employer/jobs/${application.jobId}/applications`}>
                  <BriefcaseBusiness size={15} /> {application.jobTitle} · Xem các ứng viên của tin này
                </Link>
                <ApplicantCard application={application} busy={busyId === application.id} onDecide={decide} />
              </div>
            ))}
          </div>
        </>
      ) : (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-14 text-center">
          <UserRound aria-hidden="true" className="mx-auto text-slate-300" size={36} />
          <h2 className="mt-4 font-semibold text-slate-800">{applications.length ? 'Không tìm thấy hồ sơ phù hợp' : 'Chưa có đơn ứng tuyển'}</h2>
          <p className="mt-1 text-sm text-slate-500">{applications.length ? 'Thử thay đổi từ khóa hoặc bộ lọc.' : 'Đơn ứng tuyển từ sinh viên sẽ xuất hiện tại đây.'}</p>
        </div>
      )}
    </div>
  );
}
