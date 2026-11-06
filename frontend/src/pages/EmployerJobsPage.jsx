import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle,
  Edit3,
  Eye,
  Inbox,
  PlusCircle,
  RefreshCw,
  Users,
} from 'lucide-react';
import { formatVnd } from '../components/JobCard';
import jobService from '../services/jobService';

const EmployerJobsPage = () => {
  const [jobs, setJobs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorState, setErrorState] = useState(null);

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

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            Quản lý tin tuyển dụng của tôi (FR-17, FR-18, FR-20)
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Theo dõi các tin đã đăng, chỉnh sửa nội dung và duyệt hồ sơ ứng viên
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
          {jobs.map((job) => (
            <div
              key={job.id}
              className="flex flex-col justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center"
            >
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-bold text-slate-700">
                    #{job.id}
                  </span>
                  <span className="rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-bold text-emerald-800">
                    {job.status}
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
          ))}
        </div>
      )}
    </div>
  );
};

export default EmployerJobsPage;

