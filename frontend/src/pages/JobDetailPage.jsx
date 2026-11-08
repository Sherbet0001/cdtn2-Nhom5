import { useEffect, useState } from 'react';
import { ArrowLeft, BriefcaseBusiness, CalendarDays, CheckCircle2, Clock3, FileText, MapPin, Wallet } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import jobService from '../services/jobService.js';
import applicationService from '../services/applicationService.js';
import profileService from '../services/profileService.js';
import { useAuth } from '../contexts/AuthContext.jsx';

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

function DetailSection({ title, children }) {
  if (!children) return null;
  return <section className="border-t border-slate-100 pt-6"><h2 className="text-lg font-bold text-slate-900">{title}</h2><div className="mt-3 whitespace-pre-line text-sm leading-7 text-slate-600">{children}</div></section>;
}

export default function JobDetailPage() {
  const { jobId } = useParams();
  const { isAuthenticated, user } = useAuth();
  const [job, setJob] = useState(null);
  const [profile, setProfile] = useState(null);
  const [application, setApplication] = useState(null);
  const [coverLetter, setCoverLetter] = useState('');
  const [showApplyForm, setShowApplyForm] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    const requests = [jobService.get(jobId)];
    if (user?.role === 'STUDENT') {
      requests.push(profileService.getMine(), applicationService.getMine());
    }
    Promise.all(requests).then(([data, studentProfile, applications]) => {
      if (!active) return;
      setJob(data);
      if (studentProfile) setProfile(studentProfile);
      if (applications) setApplication(applications.find((item) => String(item.jobId) === jobId) || null);
    }).catch((requestError) => {
      if (active) setError(requestError.message);
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, [jobId, user?.role]);

  async function submitApplication(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const submitted = await applicationService.apply(jobId, coverLetter.trim());
      setApplication(submitted);
      setShowApplyForm(false);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <p className="py-16 text-center text-sm text-slate-500">Đang tải chi tiết việc làm...</p>;
  if (error || !job) return <section className="mx-auto max-w-2xl rounded-2xl bg-white p-8 text-center shadow-sm"><p className="notice-error" role="alert">{error || 'Tin tuyển dụng không tồn tại hoặc đã đóng.'}</p><Link className="button-secondary mt-5" to="/jobs"><ArrowLeft size={16} /> Quay lại danh sách</Link></section>;

  return (
    <div className="mx-auto max-w-4xl">
      <Link className="mb-5 inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-indigo-700" to="/jobs"><ArrowLeft size={16} /> Tất cả việc làm</Link>
      <article className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
        <div className="bg-gradient-to-r from-indigo-700 to-violet-700 p-6 text-white sm:p-9">
          <div className="flex flex-col gap-5 sm:flex-row sm:items-start">
            <div className="flex size-14 shrink-0 items-center justify-center rounded-2xl bg-white/15"><BriefcaseBusiness size={26} /></div>
            <div className="min-w-0 flex-1">
              <span className="rounded-full bg-white/15 px-3 py-1 text-xs font-semibold">{typeLabels[job.jobType]}</span>
              <h1 className="mt-4 text-2xl font-extrabold sm:text-3xl">{job.title}</h1>
              <p className="mt-2 text-indigo-100">{job.companyName}</p>
              <div className="mt-5 flex flex-wrap gap-x-5 gap-y-2 text-sm text-indigo-100">
                <span className="inline-flex items-center gap-1.5"><MapPin size={15} />{job.location ? `${job.location.district}, ${job.location.city}` : 'Linh hoạt'}</span>
                <span className="inline-flex items-center gap-1.5"><Wallet size={15} />{salaryLabel(job)}</span>
                <span className="inline-flex items-center gap-1.5"><Clock3 size={15} />Đăng {new Date(job.postedAt).toLocaleDateString('vi-VN')}</span>
              </div>
            </div>
          </div>
        </div>
        <div className="grid gap-8 p-6 sm:p-9 md:grid-cols-[1fr_260px]">
          <div className="space-y-7">
            <DetailSection title="Mô tả công việc">{job.description}</DetailSection>
            <DetailSection title="Yêu cầu ứng viên">{job.requirements}</DetailSection>
            <DetailSection title="Quyền lợi">{job.benefits}</DetailSection>
          </div>
          <aside className="h-fit rounded-2xl bg-slate-50 p-5">
            <h2 className="font-bold text-slate-900">Thông tin tuyển dụng</h2>
            <dl className="mt-4 space-y-4 text-sm">
              <div><dt className="text-slate-500">Hình thức</dt><dd className="mt-1 font-semibold text-slate-800">{typeLabels[job.jobType]}</dd></div>
              <div><dt className="text-slate-500">Mức lương</dt><dd className="mt-1 font-semibold text-emerald-700">{salaryLabel(job)}</dd></div>
              <div><dt className="text-slate-500">Khu vực</dt><dd className="mt-1 font-semibold text-slate-800">{job.location ? `${job.location.district}, ${job.location.city}` : 'Linh hoạt'}</dd></div>
              {job.expiresAt && <div><dt className="text-slate-500">Hạn ứng tuyển</dt><dd className="mt-1 inline-flex items-center gap-1.5 font-semibold text-slate-800"><CalendarDays size={15} />{new Date(`${job.expiresAt}T00:00:00`).toLocaleDateString('vi-VN')}</dd></div>}
            </dl>
            {user?.role === 'STUDENT' && (
              <div className="mt-5 border-t border-slate-200 pt-5">
                {application ? (
                  <div className={`rounded-xl p-4 text-sm ${application.status === 'INTERVIEW' ? 'bg-emerald-50 text-emerald-900' : application.status === 'REJECTED' ? 'bg-slate-100 text-slate-700' : 'bg-amber-50 text-amber-900'}`}>
                    <p className="font-bold">{application.status === 'INTERVIEW' ? 'Bạn được mời phỏng vấn' : application.status === 'REJECTED' ? 'Hồ sơ chưa phù hợp lần này' : 'Bạn đã ứng tuyển'}</p>
                    {application.status === 'INTERVIEW' && <><p className="mt-2">{application.interviewAt && new Date(application.interviewAt).toLocaleString('vi-VN')}</p><p className="mt-1">{application.interviewLocation}</p></>}
                    <Link className="mt-3 inline-block font-semibold underline" to="/applications/mine">Theo dõi hồ sơ ứng tuyển</Link>
                  </div>
                ) : !isAuthenticated ? (
                  <Link className="button-primary w-full" to="/login">Đăng nhập để ứng tuyển</Link>
                ) : !profile?.cvContent?.trim() && !profile?.cvFileName ? (
                  <div className="space-y-3">
                    <p className="text-sm text-slate-600">Cập nhật CV trong hồ sơ trước khi ứng tuyển.</p>
                    <Link className="button-primary w-full" to="/profile"><FileText size={16} /> Cập nhật CV</Link>
                  </div>
                ) : !showApplyForm ? (
                  <button className="button-primary w-full" onClick={() => setShowApplyForm(true)} type="button"><FileText size={16} /> Ứng tuyển ngay</button>
                ) : (
                  <form className="space-y-3" onSubmit={submitApplication}>
                    <label className="field-label" htmlFor="coverLetter">Lời nhắn cho nhà tuyển dụng (không bắt buộc)</label>
                    <textarea className="field-input min-h-28 resize-y" id="coverLetter" maxLength={3000} onChange={(event) => setCoverLetter(event.target.value)} placeholder="Giới thiệu ngắn về bản thân và lý do bạn phù hợp..." value={coverLetter} />
                    <p className="text-xs leading-5 text-slate-500">CV dạng văn bản và tệp CV hiện tại (nếu có) sẽ được gửi kèm hồ sơ.</p>
                    <button className="button-primary w-full" disabled={submitting} type="submit">{submitting ? 'Đang gửi hồ sơ...' : 'Gửi hồ sơ ứng tuyển'}</button>
                    <button className="button-secondary w-full" onClick={() => setShowApplyForm(false)} type="button">Hủy</button>
                  </form>
                )}
              </div>
            )}
            {user?.role === 'EMPLOYER' && <Link className="button-secondary mt-5 w-full" to={`/employer/jobs/${job.id}/applications`}>Xem hồ sơ ứng viên</Link>}
            <div className="mt-6 rounded-xl border border-indigo-100 bg-white p-3 text-xs leading-5 text-slate-600">
              <CheckCircle2 className="mb-1 text-indigo-600" size={17} />
              Tin được đăng bởi {job.companyName}. Hãy chuẩn bị hồ sơ phù hợp trước khi liên hệ ứng tuyển.
            </div>
          </aside>
        </div>
      </article>
    </div>
  );
}
