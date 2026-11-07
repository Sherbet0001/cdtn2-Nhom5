import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle,
  CheckCircle2,
  Edit3,
  Eye,
  Inbox,
  Loader2,
  Lock,
  PlusCircle,
  RefreshCw,
  UserCheck,
  Users,
} from 'lucide-react';
import { formatVnd } from '../components/JobCard';
import jobService from '../services/jobService';

const JOB_STATUS_BADGE = {
  OPEN: {
    label: 'Đang mở (OPEN)',
    className: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  },
  FILLED: {
    label: 'Đã đủ người (FILLED)',
    className: 'bg-blue-100 text-blue-800 border-blue-200',
  },
  COMPLETED: {
    label: 'Hoàn thành (COMPLETED)',
    className: 'bg-purple-100 text-purple-800 border-purple-200',
  },
  CLOSED: {
    label: 'Đã đóng (CLOSED)',
    className: 'bg-slate-200 text-slate-700 border-slate-300',
  },
  HIDDEN: {
    label: 'Bị ẩn (HIDDEN)',
    className: 'bg-red-100 text-red-800 border-red-200',
  },
};

const EmployerJobsPage = () => {
  const [jobs, setJobs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorState, setErrorState] = useState(null);
  const [statusLoadingJobId, setStatusLoadingJobId] = useState(null);
  const [actionToast, setActionToast] = useState('');
  const [actionError, setActionError] = useState(null);

  const loadMyJobs = useCallback(async () => {
    setIsLoading(true);
    setErrorState(null);
    try {
      const response = await jobService.getMyJobs({ page: 0, size: 50 });
      setJobs(response.data?.content || []);
    } catch (error) {
      setErrorState({
        message: error.message || 'Không thể tải danh sách tin tuyển dụng của bạn.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadMyJobs();
  }, [loadMyJobs]);

  const handleStatusChange = async (jobId, targetStatus) => {
    setStatusLoadingJobId(jobId);
    setActionError(null);
    setActionToast('');

    try {
      await jobService.updateJobStatus(jobId, targetStatus);
      setActionToast(
        targetStatus === 'COMPLETED'
          ? `Đã chuyển tin #${jobId} sang COMPLETED và gửi thông báo JOB_COMPLETED cho các sinh viên đã ACCEPTED!`
          : `Đã chuyển trạng thái tin #${jobId} sang ${targetStatus} thành công!`
      );
      await loadMyJobs();
    } catch (error) {
      setActionError({
        message: error.message || 'Không thể đổi trạng thái tin tuyển dụng.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setStatusLoadingJobId(null);
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            Quản lý tin tuyển dụng của tôi (FR-17, FR-18, FR-20, FR-23)
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Theo dõi tin đã đăng, duyệt hồ sơ và đổi trạng thái công việc (OPEN → FILLED → COMPLETED)
          </p>
        </div>

        <Link
          to="/employer/jobs/new"
          className="inline-flex items-center gap-2 rounded-xl bg-red-600 px-5 py-2.5 text-sm font-bold text-white shadow-sm transition hover:bg-red-700"
        >
          <PlusCircle className="h-4 w-4" />
          <span>Đăng tin mới</span>
        </Link>
      </div>

      {actionToast && (
        <div className="mb-5 flex items-center gap-2.5 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-800">
          <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600" />
          <span>{actionToast}</span>
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
          {[1, 2, 3].map((item) => (
            <div key={item} className="h-28 animate-pulse rounded-2xl bg-slate-200/80" />
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
              <h2 className="text-base font-bold">Lỗi tải danh sách tin đã đăng</h2>
              <p className="mt-1 text-sm text-red-800">{errorState.message}</p>
              <p className="mt-2 font-mono text-xs text-red-600">
                RequestId: {errorState.requestId}
              </p>
              <button
                type="button"
                onClick={loadMyJobs}
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
      {!isLoading && !errorState && jobs.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">
          <Inbox className="mx-auto h-10 w-10 text-slate-400" />
          <h2 className="mt-3 text-base font-bold text-slate-900">
            Bạn chưa đăng tin tuyển dụng nào
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            Hãy đăng tin tuyển dụng đầu tiên để tiếp cận sinh viên tìm việc part-time.
          </p>
          <Link
            to="/employer/jobs/new"
            className="mt-4 inline-flex items-center gap-2 rounded-xl bg-red-600 px-5 py-2.5 text-sm font-bold text-white hover:bg-red-700"
          >
            <PlusCircle className="h-4 w-4" />
            <span>Đăng tin tuyển dụng ngay</span>
          </Link>
        </div>
      )}

      {/* Danh sách tin tuyển dụng của NTD */}
      {!isLoading && !errorState && jobs.length > 0 && (
        <div className="space-y-4">
          {jobs.map((job) => {
            const statusBadge = JOB_STATUS_BADGE[job.status] || {
              label: job.status,
              className: 'bg-slate-100 text-slate-700 border-slate-200',
            };
            const isUpdatingStatus = statusLoadingJobId === job.id;

            return (
              <div
                key={job.id}
                className="flex flex-col justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm lg:flex-row lg:items-center"
              >
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-bold text-slate-700">
                      #{job.id}
                    </span>
                    <span
                      className={`rounded-full border px-2.5 py-0.5 text-xs font-bold ${statusBadge.className}`}
                    >
                      {statusBadge.label}
                    </span>
                    <span className="text-xs text-slate-500">{job.categoryName}</span>
                  </div>

                  <h3 className="text-base font-bold text-slate-900">{job.title}</h3>
                  <p className="text-xs text-slate-600">
                    Mức lương: {formatVnd(job.salaryMin)} - {formatVnd(job.salaryMax)} (
                    {job.salaryUnit}) • Khu vực: {job.locationName} • Đã nhận:{' '}
                    <strong>
                      {job.acceptedCount ?? 0}/{job.slots}
                    </strong>{' '}
                    sinh viên
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  {/* Nút đổi trạng thái tin tuyển dụng theo quy tắc Mục 5 (FR-23) */}
                  {job.status === 'OPEN' && (
                    <>
                      <button
                        type="button"
                        disabled={isUpdatingStatus}
                        onClick={() => handleStatusChange(job.id, 'FILLED')}
                        className="inline-flex items-center gap-1.5 rounded-xl bg-blue-600 px-3 py-2 text-xs font-bold text-white transition hover:bg-blue-700 disabled:opacity-50"
                      >
                        {isUpdatingStatus ? (
                          <Loader2 className="h-3.5 w-3.5 animate-spin" />
                        ) : (
                          <UserCheck className="h-3.5 w-3.5" />
                        )}
                        <span>Đủ người (FILLED)</span>
                      </button>

                      <button
                        type="button"
                        disabled={isUpdatingStatus}
                        onClick={() => handleStatusChange(job.id, 'CLOSED')}
                        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-semibold text-slate-700 transition hover:bg-slate-50 disabled:opacity-50"
                      >
                        <Lock className="h-3.5 w-3.5" />
                        <span>Đóng tin (CLOSED)</span>
                      </button>
                    </>
                  )}

                  {job.status === 'FILLED' && (
                    <>
                      <button
                        type="button"
                        disabled={isUpdatingStatus}
                        onClick={() => handleStatusChange(job.id, 'COMPLETED')}
                        className="inline-flex items-center gap-1.5 rounded-xl bg-purple-600 px-3 py-2 text-xs font-bold text-white transition hover:bg-purple-700 disabled:opacity-50"
                      >
                        {isUpdatingStatus ? (
                          <Loader2 className="h-3.5 w-3.5 animate-spin" />
                        ) : (
                          <CheckCircle2 className="h-3.5 w-3.5" />
                        )}
                        <span>Hoàn thành (COMPLETED)</span>
                      </button>

                      <button
                        type="button"
                        disabled={isUpdatingStatus}
                        onClick={() => handleStatusChange(job.id, 'CLOSED')}
                        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-semibold text-slate-700 transition hover:bg-slate-50 disabled:opacity-50"
                      >
                        <Lock className="h-3.5 w-3.5" />
                        <span>Đóng tin (CLOSED)</span>
                      </button>
                    </>
                  )}

                  <Link
                    to={`/employer/jobs/${job.id}/applications`}
                    className="inline-flex items-center gap-1.5 rounded-xl bg-slate-900 px-3.5 py-2 text-xs font-bold text-white hover:bg-slate-800"
                  >
                    <Users className="h-3.5 w-3.5" />
                    <span>Ứng viên ({job.applicantCount ?? 0})</span>
                  </Link>

                  {job.status !== 'COMPLETED' && job.status !== 'CLOSED' && (
                    <Link
                      to={`/employer/jobs/${job.id}/edit`}
                      className="inline-flex items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-xs font-bold text-slate-700 hover:bg-slate-50"
                    >
                      <Edit3 className="h-3.5 w-3.5" />
                      <span>Sửa tin</span>
                    </Link>
                  )}

                  <Link
                    to={`/jobs/${job.id}`}
                    className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100"
                  >
                    <Eye className="h-3.5 w-3.5" />
                    <span>Chi tiết</span>
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default EmployerJobsPage;
