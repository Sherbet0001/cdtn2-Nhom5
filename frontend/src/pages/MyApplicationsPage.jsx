import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle,
  Briefcase,
  CheckCircle2,
  Clock,
  Eye,
  FileText,
  Inbox,
  MapPin,
  RefreshCw,
  Star,
  X,
} from 'lucide-react';
import { formatVnd } from '../components/JobCard';
import ReviewModal from '../components/ReviewModal';
import jobService from '../services/jobService';

const APPLICATION_STATUS_BADGE = {
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

const JOB_STATUS_BADGE = {
  OPEN: { label: 'Đang mở (OPEN)', className: 'bg-emerald-50 text-emerald-700' },
  FILLED: { label: 'Đã đủ người (FILLED)', className: 'bg-blue-50 text-blue-700' },
  COMPLETED: { label: 'Hoàn thành (COMPLETED)', className: 'bg-purple-50 text-purple-700' },
  CLOSED: { label: 'Đã đóng (CLOSED)', className: 'bg-slate-100 text-slate-600' },
};

const MyApplicationsPage = () => {
  const [statusFilter, setStatusFilter] = useState('');
  const [applications, setApplications] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorState, setErrorState] = useState(null);

  const [selectedDetail, setSelectedDetail] = useState(null);
  const [isDetailLoading, setIsDetailLoading] = useState(false);

  const [reviewTarget, setReviewTarget] = useState(null);
  const [toastMessage, setToastMessage] = useState('');

  const loadApplications = useCallback(async (filterVal = statusFilter) => {
    setIsLoading(true);
    setErrorState(null);
    try {
      const response = await jobService.getMyApplications(filterVal);
      setApplications(response.data || []);
    } catch (error) {
      setErrorState({
        message: error.message || 'Không thể tải danh sách hồ sơ đã ứng tuyển.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    loadApplications(statusFilter);
  }, [statusFilter, loadApplications]);

  const handleOpenDetail = async (applicationId) => {
    setIsDetailLoading(true);
    try {
      const res = await jobService.getApplicationById(applicationId);
      setSelectedDetail(res.data);
    } catch (error) {
      setErrorState({
        message: error.message || 'Không thể tải chi tiết hồ sơ ứng tuyển.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsDetailLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            Việc làm đã ứng tuyển & Theo dõi trạng thái (FR-13, FR-14, FR-15)
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Theo dõi trạng thái xét duyệt hồ sơ và đánh giá Nhà tuyển dụng sau khi hoàn thành công việc
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-1.5 rounded-xl border border-slate-200 bg-white p-1.5 shadow-xs">
          {[
            { value: '', label: 'Tất cả' },
            { value: 'PENDING', label: 'Chờ duyệt' },
            { value: 'ACCEPTED', label: 'Đã chấp nhận' },
            { value: 'REJECTED', label: 'Đã từ chối' },
          ].map((tab) => (
            <button
              key={tab.value || 'ALL'}
              type="button"
              onClick={() => setStatusFilter(tab.value)}
              className={`rounded-lg px-3 py-1.5 text-xs font-bold transition ${
                statusFilter === tab.value
                  ? 'bg-red-600 text-white shadow-xs'
                  : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {toastMessage && (
        <div className="mb-5 flex items-center gap-2.5 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-800">
          <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* 1. Loading State */}
      {isLoading && (
        <div className="space-y-4">
          {[1, 2, 3].map((item) => (
            <div key={item} className="h-32 animate-pulse rounded-2xl bg-slate-200/80" />
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
              <h2 className="text-base font-bold">Không thể tải lịch sử ứng tuyển</h2>
              <p className="mt-1 text-sm text-red-800">{errorState.message}</p>
              <p className="mt-2 font-mono text-xs text-red-600">
                RequestId: {errorState.requestId}
              </p>
              <button
                type="button"
                onClick={() => loadApplications(statusFilter)}
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
            {statusFilter
              ? `Không có hồ sơ ứng tuyển nào ở trạng thái ${statusFilter}`
              : 'Bạn chưa ứng tuyển công việc nào'}
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            {statusFilter
              ? 'Hãy thử chuyển sang bộ lọc Tất cả để xem toàn bộ lịch sử ứng tuyển.'
              : 'Khám phá các việc làm part-time đang mở tuyển và nộp hồ sơ ngay.'}
          </p>
          {statusFilter ? (
            <button
              type="button"
              onClick={() => setStatusFilter('')}
              className="mt-4 inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-bold text-slate-700 hover:bg-slate-50"
            >
              Xóa bộ lọc
            </button>
          ) : (
            <Link
              to="/jobs"
              className="mt-4 inline-flex items-center gap-2 rounded-xl bg-red-600 px-5 py-2.5 text-sm font-bold text-white hover:bg-red-700"
            >
              <Briefcase className="h-4 w-4" />
              <span>Tìm việc làm ngay</span>
            </Link>
          )}
        </div>
      )}

      {/* Danh sách hồ sơ ứng tuyển */}
      {!isLoading && !errorState && applications.length > 0 && (
        <div className="space-y-4">
          {applications.map((app) => {
            const appBadge = APPLICATION_STATUS_BADGE[app.status] || {
              label: app.status,
              className: 'bg-slate-100 text-slate-700 border-slate-200',
            };
            const jobBadge = JOB_STATUS_BADGE[app.jobStatus] || {
              label: app.jobStatus || 'OPEN',
              className: 'bg-slate-100 text-slate-700',
            };
            const canReviewEmployer =
              app.status === 'ACCEPTED' &&
              app.jobStatus === 'COMPLETED' &&
              !app.hasReviewed;

            return (
              <div
                key={app.id}
                className="flex flex-col justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center"
              >
                <div className="space-y-1.5">
                  <div className="flex flex-wrap items-center gap-2">
                    <span
                      className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold ${appBadge.className}`}
                    >
                      {appBadge.label}
                    </span>
                    <span
                      className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${jobBadge.className}`}
                    >
                      Tin: {jobBadge.label}
                    </span>
                    <span className="inline-flex items-center gap-1 text-xs text-slate-500">
                      <Clock className="h-3.5 w-3.5" />
                      <span>
                        Nộp lúc:{' '}
                        {app.appliedAt
                          ? new Date(app.appliedAt).toLocaleString('vi-VN')
                          : ''}
                      </span>
                    </span>
                  </div>

                  <Link
                    to={`/jobs/${app.jobId}`}
                    className="block text-base font-bold text-slate-900 hover:text-red-600"
                  >
                    {app.jobTitle}
                  </Link>

                  <div className="flex flex-wrap items-center gap-3 text-xs text-slate-600">
                    {app.companyName && (
                      <span className="font-semibold text-slate-800">
                        Đơn vị: {app.companyName}
                      </span>
                    )}
                    {app.salaryMin && app.salaryMax && (
                      <span>
                        Mức lương: {formatVnd(app.salaryMin)} - {formatVnd(app.salaryMax)} (
                        {app.salaryUnit})
                      </span>
                    )}
                    {app.locationName && (
                      <span className="inline-flex items-center gap-1">
                        <MapPin className="h-3.5 w-3.5 text-slate-400" />
                        <span>{app.locationName}</span>
                      </span>
                    )}
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  {canReviewEmployer && (
                    <button
                      type="button"
                      onClick={() =>
                        setReviewTarget({
                          jobId: app.jobId,
                          jobTitle: app.jobTitle,
                          revieweeId: app.employerId,
                          revieweeName: app.companyName || app.employerName,
                        })
                      }
                      className="inline-flex items-center gap-1.5 rounded-xl bg-amber-500 px-3.5 py-2 text-xs font-bold text-white shadow-xs transition hover:bg-amber-600"
                    >
                      <Star className="h-3.5 w-3.5 fill-white" />
                      <span>Đánh giá NTD (FR-15)</span>
                    </button>
                  )}

                  {app.hasReviewed && (
                    <span className="inline-flex items-center gap-1 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs font-semibold text-emerald-800">
                      <CheckCircle2 className="h-3.5 w-3.5" />
                      <span>Đã đánh giá NTD</span>
                    </span>
                  )}

                  <button
                    type="button"
                    disabled={isDetailLoading}
                    onClick={() => handleOpenDetail(app.id)}
                    className="inline-flex items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                  >
                    <FileText className="h-3.5 w-3.5" />
                    <span>Chi tiết hồ sơ</span>
                  </button>

                  <Link
                    to={`/jobs/${app.jobId}`}
                    className="inline-flex items-center gap-1.5 rounded-xl bg-slate-900 px-3.5 py-2 text-xs font-bold text-white hover:bg-slate-800"
                  >
                    <Eye className="h-3.5 w-3.5" />
                    <span>Xem việc làm</span>
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal Chi tiết Hồ sơ Ứng tuyển (GET /api/v1/applications/{id} - FR-13) */}
      {selectedDetail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs">
          <div className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-6 shadow-xl">
            <div className="flex items-start justify-between gap-3 border-b border-slate-100 pb-4">
              <div>
                <h2 className="text-lg font-bold text-slate-900">
                  Chi tiết hồ sơ ứng tuyển #{selectedDetail.id} (FR-13)
                </h2>
                <p className="mt-1 text-xs text-slate-500">
                  Công việc: <strong className="text-slate-800">{selectedDetail.jobTitle}</strong>
                </p>
              </div>
              <button
                type="button"
                onClick={() => setSelectedDetail(null)}
                className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="mt-4 space-y-3 text-xs text-slate-700">
              <div className="flex items-center justify-between rounded-xl bg-slate-50 p-3">
                <span className="font-semibold text-slate-600">Trạng thái xét duyệt:</span>
                <span className="font-bold text-slate-900">{selectedDetail.status}</span>
              </div>
              <div className="flex items-center justify-between rounded-xl bg-slate-50 p-3">
                <span className="font-semibold text-slate-600">Trạng thái công việc:</span>
                <span className="font-bold text-slate-900">{selectedDetail.jobStatus}</span>
              </div>
              {selectedDetail.coverLetter && (
                <div className="rounded-xl border border-slate-200 p-3.5">
                  <p className="font-bold text-slate-600">Thư giới thiệu (Cover Letter):</p>
                  <p className="mt-1 whitespace-pre-line leading-relaxed">
                    {selectedDetail.coverLetter}
                  </p>
                </div>
              )}
              <div className="text-[11px] text-slate-500">
                Thời gian nộp:{' '}
                {selectedDetail.appliedAt
                  ? new Date(selectedDetail.appliedAt).toLocaleString('vi-VN')
                  : ''}
              </div>
            </div>

            <div className="mt-5 flex justify-end">
              <button
                type="button"
                onClick={() => setSelectedDetail(null)}
                className="rounded-xl bg-slate-900 px-4 py-2 text-xs font-bold text-white hover:bg-slate-800"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Đánh giá Nhà tuyển dụng (FR-15) */}
      <ReviewModal
        isOpen={Boolean(reviewTarget)}
        onClose={() => setReviewTarget(null)}
        jobId={reviewTarget?.jobId}
        jobTitle={reviewTarget?.jobTitle}
        revieweeId={reviewTarget?.revieweeId}
        revieweeName={reviewTarget?.revieweeName}
        revieweeRoleLabel="Nhà tuyển dụng"
        onSuccess={() => {
          setToastMessage('Đã gửi đánh giá cho Nhà tuyển dụng thành công!');
          loadApplications(statusFilter);
        }}
      />
    </div>
  );
};

export default MyApplicationsPage;

