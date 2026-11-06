import React, { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  FileText,
  GraduationCap,
  Inbox,
  Loader2,
  Mail,
  Phone,
  RefreshCw,
  UserCheck,
  XCircle,
} from 'lucide-react';
import jobService from '../services/jobService';

const STATUS_BADGE = {
  PENDING: {
    label: 'Chờ duyệt (PENDING)',
    className: 'bg-amber-100 text-amber-800 border-amber-200',
  },
  ACCEPTED: {
    label: 'Đã chấp nhận (ACCEPTED)',
    className: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  },
  REJECTED: {
    label: 'Đã từ chối (REJECTED)',
    className: 'bg-red-100 text-red-800 border-red-200',
  },
};

const JobApplicantsPage = () => {
  const { jobId } = useParams();

  const [job, setJob] = useState(null);
  const [applications, setApplications] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorState, setErrorState] = useState(null);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [actionMessage, setActionMessage] = useState('');
  const [actionError, setActionError] = useState(null);

  const loadJobAndApplicants = useCallback(async () => {
    setIsLoading(true);
    setErrorState(null);
    try {
      const [jobRes, appRes] = await Promise.all([
        jobService.getJobById(jobId),
        jobService.getJobApplications(jobId),
      ]);
      setJob(jobRes.data);
      setApplications(appRes.data || []);
    } catch (error) {
      setErrorState({
        message: error.message || 'Không thể tải danh sách hồ sơ ứng viên.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsLoading(false);
    }
  }, [jobId]);

  useEffect(() => {
    loadJobAndApplicants();
  }, [loadJobAndApplicants]);

  const handleDecision = async (applicationId, targetStatus) => {
    setActionLoadingId(applicationId);
    setActionError(null);
    setActionMessage('');

    try {
      await jobService.updateApplicationStatus(applicationId, targetStatus);
      setActionMessage(
        targetStatus === 'ACCEPTED'
          ? 'Đã chấp nhận hồ sơ ứng viên và gửi thông báo cho sinh viên!'
          : 'Đã từ chối hồ sơ ứng viên và gửi thông báo cho sinh viên.'
      );
      await loadJobAndApplicants();
    } catch (error) {
      setActionError({
        message: error.message || 'Không thể cập nhật trạng thái hồ sơ ứng viên.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setActionLoadingId(null);
    }
  };

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6">
      <div className="mb-4">
        <Link
          to="/employer/jobs"
          className="inline-flex items-center gap-1.5 text-sm font-semibold text-red-600 hover:underline"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Quay lại Quản lý tin tuyển dụng</span>
        </Link>
      </div>

      <div className="mb-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h1 className="text-xl font-bold text-slate-900">
          Danh sách ứng viên ứng tuyển (FR-19, FR-21, FR-22)
        </h1>
        {job && (
          <p className="mt-1 text-sm text-slate-600">
            Tin tuyển dụng: <strong className="text-slate-900">{job.title}</strong> • Chỉ tiêu đã
            nhận:{' '}
            <strong className="text-emerald-700">
              {job.acceptedCount ?? 0} / {job.slots}
            </strong>{' '}
            sinh viên
          </p>
        )}
      </div>

      {actionMessage && (
        <div className="mb-5 flex items-center gap-2.5 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-800">
          <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600" />
          <span>{actionMessage}</span>
        </div>
      )}

      {actionError && (
        <div
          role="alert"
          className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"
        >
          <div className="flex items-start gap-2.5">
            <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
            <div>
              <p className="font-semibold">{actionError.message}</p>
              <p className="mt-1 font-mono text-xs text-red-600">
                RequestId: {actionError.requestId}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* 1. Loading State */}
      {isLoading && (
        <div className="space-y-4">
          {[1, 2].map((item) => (
            <div key={item} className="h-44 animate-pulse rounded-2xl bg-slate-200/80" />
          ))}
        </div>
      )}

      {/* 3. Error State */}
      {!isLoading && errorState && (
        <div
          role="alert"
          className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-900"
        >
          <div className="flex items-start gap-3">
            <AlertCircle className="mt-0.5 h-6 w-6 shrink-0 text-red-600" />
            <div>
              <h2 className="text-base font-bold">Không thể tải danh sách ứng viên</h2>
              <p className="mt-1 text-sm text-red-800">{errorState.message}</p>
              <p className="mt-2 font-mono text-xs text-red-600">
                RequestId: {errorState.requestId}
              </p>
              <button
                type="button"
                onClick={loadJobAndApplicants}
                className="mt-4 inline-flex items-center gap-2 rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700"
              >
                <RefreshCw className="h-4 w-4" />
                <span>Thử lại</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 2. Empty State */}
      {!isLoading && !errorState && applications.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">
          <Inbox className="mx-auto h-10 w-10 text-slate-400" />
          <h2 className="mt-3 text-base font-bold text-slate-900">
            Chưa có sinh viên nào nộp hồ sơ vào tin này
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            Khi có sinh viên nộp đơn, hồ sơ và CV sẽ hiển thị tại đây.
          </p>
        </div>
      )}

      {/* Danh sách hồ sơ ứng viên */}
      {!isLoading && !errorState && applications.length > 0 && (
        <div className="space-y-4">
          {applications.map((app) => {
            const badge = STATUS_BADGE[app.status] || {
              label: app.status,
              className: 'bg-slate-100 text-slate-700 border-slate-200',
            };
            const isProcessing = actionLoadingId === app.id;

            return (
              <div
                key={app.id}
                className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2.5">
                      <h3 className="text-lg font-bold text-slate-900">
                        {app.studentFullName}
                      </h3>
                      <span
                        className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold ${badge.className}`}
                      >
                        {badge.label}
                      </span>
                    </div>

                    <div className="mt-2 flex flex-wrap items-center gap-4 text-xs text-slate-600">
                      <span className="inline-flex items-center gap-1">
                        <Mail className="h-3.5 w-3.5 text-slate-400" />
                        <span>{app.studentEmail}</span>
                      </span>

                      {app.studentPhone && (
                        <span className="inline-flex items-center gap-1">
                          <Phone className="h-3.5 w-3.5 text-slate-400" />
                          <span>{app.studentPhone}</span>
                        </span>
                      )}

                      {app.studentSchool && (
                        <span className="inline-flex items-center gap-1">
                          <GraduationCap className="h-3.5 w-3.5 text-slate-400" />
                          <span>
                            {app.studentSchool}
                            {app.studentMajor ? ` - ${app.studentMajor}` : ''}
                          </span>
                        </span>
                      )}
                    </div>
                  </div>

                  <Link
                    to={`/profiles/${app.studentId}`}
                    className="inline-flex items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                  >
                    <UserCheck className="h-3.5 w-3.5" />
                    <span>Xem trang hồ sơ ứng viên</span>
                  </Link>
                </div>

                {app.studentCvContent && (
                  <div className="mt-4 rounded-xl bg-slate-50 p-4">
                    <h4 className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-slate-500">
                      <FileText className="h-3.5 w-3.5" />
                      <span>Nội dung CV của ứng viên</span>
                    </h4>
                    <p className="mt-1.5 whitespace-pre-line text-xs leading-relaxed text-slate-700">
                      {app.studentCvContent}
                    </p>
                  </div>
                )}

                {app.coverLetter && (
                  <div className="mt-3 rounded-xl border border-slate-100 bg-white p-3.5">
                    <h4 className="text-xs font-bold text-slate-500">
                      Thư giới thiệu (Cover Letter):
                    </h4>
                    <p className="mt-1 whitespace-pre-line text-xs text-slate-700">
                      {app.coverLetter}
                    </p>
                  </div>
                )}

                {app.status === 'PENDING' && (
                  <div className="mt-4 flex items-center justify-end gap-2.5 border-t border-slate-100 pt-4">
                    <button
                      type="button"
                      disabled={isProcessing}
                      onClick={() => handleDecision(app.id, 'REJECTED')}
                      className="inline-flex items-center gap-1.5 rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-xs font-bold text-red-700 transition hover:bg-red-100 disabled:opacity-50"
                    >
                      {isProcessing ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      ) : (
                        <XCircle className="h-3.5 w-3.5" />
                      )}
                      <span>Từ chối (REJECTED)</span>
                    </button>

                    <button
                      type="button"
                      disabled={isProcessing}
                      onClick={() => handleDecision(app.id, 'ACCEPTED')}
                      className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-bold text-white shadow-sm transition hover:bg-emerald-700 disabled:opacity-50"
                    >
                      {isProcessing ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      ) : (
                        <CheckCircle2 className="h-3.5 w-3.5" />
                      )}
                      <span>Chấp nhận (ACCEPTED)</span>
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default JobApplicantsPage;

