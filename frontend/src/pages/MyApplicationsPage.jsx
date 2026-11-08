import { useEffect, useState } from 'react';
import { Bell, BriefcaseBusiness, CalendarCheck2, Clock3, XCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import applicationService from '../services/applicationService.js';

const statusDetails = {
  NEW: { label: 'Đang được xem xét', className: 'bg-amber-50 text-amber-700', icon: Clock3 },
  INTERVIEW: { label: 'Được mời phỏng vấn', className: 'bg-emerald-50 text-emerald-700', icon: CalendarCheck2 },
  REJECTED: { label: 'Chưa phù hợp lần này', className: 'bg-slate-100 text-slate-600', icon: XCircle },
};

export default function MyApplicationsPage() {
  const [applications, setApplications] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    applicationService.getMine().then(setApplications).catch((requestError) => setError(requestError.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-6">
        <p className="text-sm font-semibold text-indigo-700">Sinh viên</p>
        <h1 className="mt-1 text-2xl font-extrabold text-slate-900">Hồ sơ ứng tuyển của tôi</h1>
        <p className="mt-2 text-sm text-slate-600">Theo dõi tiến độ và thông tin phỏng vấn từ nhà tuyển dụng.</p>
      </div>
      {error && <p className="notice-error mb-5" role="alert">{error}</p>}
      {loading ? <p className="rounded-2xl bg-white p-10 text-center text-sm text-slate-500">Đang tải hồ sơ ứng tuyển...</p>
        : applications.length ? (
          <div className="space-y-4">
            {applications.map((application) => {
              const status = statusDetails[application.status];
              const Icon = status.icon;
              return (
                <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm" key={application.id}>
                  <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
                    <div className="flex gap-3">
                      <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-indigo-50 text-indigo-700"><BriefcaseBusiness size={20} /></div>
                      <div><h2 className="font-bold text-slate-900">{application.jobTitle}</h2><p className="mt-1 text-sm text-slate-500">{application.companyName}</p><p className="mt-1 text-xs text-slate-400">Đã ứng tuyển {new Date(application.appliedAt).toLocaleDateString('vi-VN')}</p></div>
                    </div>
                    <span className={`inline-flex w-fit items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold ${status.className}`}><Icon size={14} />{status.label}</span>
                  </div>
                  {application.status === 'INTERVIEW' && (
                    <div className="mt-4 rounded-xl border border-emerald-100 bg-emerald-50/60 p-4 text-sm">
                      <h3 className="font-bold text-emerald-900">Thông tin phỏng vấn</h3>
                      {application.interviewAt && <p className="mt-2 text-emerald-800">{new Date(application.interviewAt).toLocaleString('vi-VN')}</p>}
                      {application.interviewLocation && <p className="mt-1 text-emerald-800">{application.interviewLocation}</p>}
                      {application.employerMessage && <p className="mt-2 text-emerald-800">{application.employerMessage}</p>}
                    </div>
                  )}
                  {application.status === 'REJECTED' && application.employerMessage && <p className="mt-3 text-sm text-slate-600">{application.employerMessage}</p>}
                  <div className="mt-4 flex flex-wrap gap-3 border-t border-slate-100 pt-4">
                    <Link className="text-sm font-semibold text-indigo-700 hover:underline" to={`/jobs/${application.jobId}`}>Xem tin tuyển dụng</Link>
                    <Link className="inline-flex items-center gap-1 text-sm font-semibold text-slate-600 hover:text-indigo-700" to="/notifications"><Bell size={14} /> Thông báo</Link>
                  </div>
                </article>
              );
            })}
          </div>
        ) : (
          <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-14 text-center">
            <BriefcaseBusiness aria-hidden="true" className="mx-auto text-slate-300" size={36} />
            <h2 className="mt-4 font-semibold text-slate-800">Bạn chưa ứng tuyển công việc nào</h2>
            <p className="mt-1 text-sm text-slate-500">Cập nhật CV trong hồ sơ rồi khám phá các cơ hội phù hợp.</p>
            <Link className="button-primary mt-5" to="/jobs">Tìm việc</Link>
          </div>
        )}
    </div>
  );
}
