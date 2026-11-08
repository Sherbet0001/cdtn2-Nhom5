import { useEffect, useState } from 'react';
import { ArrowLeft, CalendarCheck2, Download, Mail, MapPin, Phone, UserRound, XCircle } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import applicationService from '../services/applicationService.js';
import jobService from '../services/jobService.js';
import { downloadFile } from '../utils/downloadFile.js';

function defaultInterviewTime() {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  date.setHours(9, 0, 0, 0);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}

export function ApplicantCard({ application, onDecide, busy }) {
  const [interviewAt, setInterviewAt] = useState(defaultInterviewTime);
  const [interviewLocation, setInterviewLocation] = useState('');
  const [message, setMessage] = useState('');
  const [showInterviewForm, setShowInterviewForm] = useState(false);
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [downloadError, setDownloadError] = useState('');
  const [isDownloadingCv, setIsDownloadingCv] = useState(false);
  const isPending = application.status === 'NEW';

  async function downloadApplicationCv() {
    setDownloadError('');
    setIsDownloadingCv(true);
    try {
      const file = await applicationService.downloadCvFile(application.id);
      downloadFile(file, application.cvFileName);
    } catch (requestError) {
      setDownloadError(requestError.message);
    } finally {
      setIsDownloadingCv(false);
    }
  }

  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex flex-col justify-between gap-4 sm:flex-row">
        <div className="flex gap-3">
          <div className="flex size-11 shrink-0 items-center justify-center rounded-full bg-indigo-50 text-indigo-700"><UserRound size={20} /></div>
          <div>
            <h2 className="font-bold text-slate-900">{application.student?.fullName || 'Ứng viên'}</h2>
            <p className="mt-1 flex items-center gap-1.5 text-sm text-slate-600"><Mail size={14} />{application.student?.email}</p>
            {application.student?.phone && <p className="mt-1 flex items-center gap-1.5 text-sm text-slate-600"><Phone size={14} />{application.student.phone}</p>}
            <p className="mt-1 text-xs text-slate-500">{[application.student?.school, application.student?.major].filter(Boolean).join(' · ')}</p>
            <p className="mt-1 text-xs text-slate-400">Ứng tuyển {new Date(application.appliedAt).toLocaleString('vi-VN')}</p>
          </div>
        </div>
        <span className={`h-fit rounded-full px-3 py-1.5 text-xs font-semibold ${application.status === 'NEW' ? 'bg-amber-50 text-amber-700' : application.status === 'INTERVIEW' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>
          {application.status === 'NEW' ? 'Chờ xem xét' : application.status === 'INTERVIEW' ? 'Đã mời phỏng vấn' : 'Đã từ chối'}
        </span>
      </div>

      {application.student?.bio && <p className="mt-4 text-sm leading-6 text-slate-600">{application.student.bio}</p>}
      {application.coverLetter && <div className="mt-4 rounded-xl bg-indigo-50/60 p-4"><h3 className="text-sm font-bold text-slate-800">Lời nhắn ứng tuyển</h3><p className="mt-2 whitespace-pre-line text-sm leading-6 text-slate-600">{application.coverLetter}</p></div>}
      {application.cvContent && <div className="mt-4 rounded-xl border border-slate-200 p-4">
        <h3 className="text-sm font-bold text-slate-900">CV tại thời điểm ứng tuyển</h3>
        <p className="mt-2 whitespace-pre-line text-sm leading-6 text-slate-600">{application.cvContent}</p>
      </div>}
      {application.cvFileName && <div className="mt-4 rounded-xl border border-indigo-100 bg-indigo-50/50 p-4">
        <h3 className="text-sm font-bold text-slate-900">Tệp CV đính kèm</h3>
        <p className="mt-1 text-sm text-slate-600">{application.cvFileName}</p>
        <button className="button-secondary mt-3" disabled={isDownloadingCv} onClick={downloadApplicationCv} type="button">
          {isDownloadingCv ? 'Đang tải xuống...' : <><Download size={16} /> Tải tệp CV</>}
        </button>
        {downloadError && <p className="notice-error mt-3" role="alert">{downloadError}</p>}
      </div>}

      {application.status === 'INTERVIEW' && (
        <div className="mt-4 rounded-xl bg-emerald-50 p-4 text-sm text-emerald-900">
          <p className="font-semibold">Lịch phỏng vấn đã gửi cho sinh viên</p>
          {application.interviewAt && <p className="mt-1">{new Date(application.interviewAt).toLocaleString('vi-VN')}</p>}
          {application.interviewLocation && <p className="mt-1 inline-flex items-center gap-1"><MapPin size={14} />{application.interviewLocation}</p>}
          {application.employerMessage && <p className="mt-1">{application.employerMessage}</p>}
        </div>
      )}
      {application.status === 'REJECTED' && application.employerMessage && <p className="mt-3 text-sm text-slate-600">Ghi chú: {application.employerMessage}</p>}

      {isPending && (
        <div className="mt-5 border-t border-slate-100 pt-4">
          {!showInterviewForm && !showRejectForm ? (
            <div className="flex flex-wrap gap-2">
              <button className="button-primary" onClick={() => setShowInterviewForm(true)} type="button"><CalendarCheck2 size={16} /> Mời phỏng vấn</button>
              <button className="button-secondary !text-rose-700 hover:!bg-rose-50" onClick={() => setShowRejectForm(true)} type="button"><XCircle size={16} /> Từ chối</button>
            </div>
          ) : showInterviewForm ? (
            <form className="grid gap-4 sm:grid-cols-2" onSubmit={(event) => { event.preventDefault(); onDecide(application.id, { status: 'INTERVIEW', interviewAt, interviewLocation: interviewLocation.trim(), employerMessage: message.trim() || null }); }}>
              <div><label className="field-label" htmlFor={`interview-at-${application.id}`}>Thời gian phỏng vấn</label><input className="field-input" id={`interview-at-${application.id}`} min={defaultInterviewTime()} onChange={(event) => setInterviewAt(event.target.value)} required type="datetime-local" value={interviewAt} /></div>
              <div><label className="field-label" htmlFor={`interview-location-${application.id}`}>Địa điểm / hình thức</label><input className="field-input" id={`interview-location-${application.id}`} onChange={(event) => setInterviewLocation(event.target.value)} placeholder="Địa chỉ hoặc liên kết họp trực tuyến" required value={interviewLocation} /></div>
              <div className="sm:col-span-2"><label className="field-label" htmlFor={`interview-message-${application.id}`}>Lời nhắn (không bắt buộc)</label><textarea className="field-input min-h-20" id={`interview-message-${application.id}`} onChange={(event) => setMessage(event.target.value)} placeholder="Thông tin cần chuẩn bị trước buổi gặp..." value={message} /></div>
              <div className="flex gap-2 sm:col-span-2"><button className="button-primary" disabled={busy} type="submit">{busy ? 'Đang gửi...' : 'Xác nhận và gửi lịch'}</button><button className="button-secondary" onClick={() => setShowInterviewForm(false)} type="button">Hủy</button></div>
            </form>
          ) : (
            <form className="space-y-3" onSubmit={(event) => { event.preventDefault(); onDecide(application.id, { status: 'REJECTED', employerMessage: message.trim() || null }); }}>
              <div><label className="field-label" htmlFor={`reject-message-${application.id}`}>Lời nhắn từ chối (không bắt buộc)</label><textarea className="field-input min-h-20" id={`reject-message-${application.id}`} maxLength={2000} onChange={(event) => setMessage(event.target.value)} placeholder="Viết phản hồi lịch sự cho ứng viên..." value={message} /></div>
              <div className="flex gap-2"><button className="button-secondary !border-rose-200 !text-rose-700 hover:!bg-rose-50" disabled={busy} type="submit">{busy ? 'Đang gửi...' : 'Xác nhận từ chối'}</button><button className="button-secondary" onClick={() => setShowRejectForm(false)} type="button">Hủy</button></div>
            </form>
          )}
        </div>
      )}
    </article>
  );
}

export default function JobApplicantsPage() {
  const { jobId } = useParams();
  const [job, setJob] = useState(null);
  const [applications, setApplications] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    Promise.all([jobService.getMine(), applicationService.getForJob(jobId)])
      .then(([jobs, items]) => {
        const ownedJob = jobs.find((item) => String(item.id) === jobId);
        if (!ownedJob) throw new Error('Không tìm thấy tin tuyển dụng của bạn.');
        setJob(ownedJob);
        setApplications(items);
      })
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoading(false));
  }, [jobId]);

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

  if (loading) return <p className="py-16 text-center text-sm text-slate-500">Đang tải hồ sơ ứng viên...</p>;
  if (error && !job) return <section className="mx-auto max-w-2xl"><p className="notice-error" role="alert">{error}</p><Link className="button-secondary mt-4" to="/employer/jobs"><ArrowLeft size={16} /> Quản lý tin</Link></section>;

  return (
    <div className="mx-auto max-w-4xl space-y-5">
      <Link className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-indigo-700" to="/employer/jobs"><ArrowLeft size={16} /> Quản lý tin</Link>
      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <p className="text-sm font-semibold text-indigo-700">Hồ sơ ứng viên</p>
        <h1 className="mt-1 text-2xl font-extrabold text-slate-900">{job?.title}</h1>
        <p className="mt-2 text-sm text-slate-500">{applications.length} hồ sơ đã ứng tuyển</p>
      </div>
      {error && <p className="notice-error" role="alert">{error}</p>}
      {applications.length ? applications.map((application) => <ApplicantCard key={application.id} application={application} busy={busyId === application.id} onDecide={decide} />)
        : <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-14 text-center"><UserRound aria-hidden="true" className="mx-auto text-slate-300" size={36} /><h2 className="mt-4 font-semibold text-slate-800">Chưa có hồ sơ ứng tuyển</h2><p className="mt-1 text-sm text-slate-500">Hồ sơ ứng viên sẽ hiển thị tại đây khi sinh viên ứng tuyển.</p></div>}
    </div>
  );
}
