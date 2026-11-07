import React, { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  Briefcase,
  Building2,
  Calendar,
  CheckCircle2,
  Clock,
  Coins,
  Edit3,
  Loader2,
  MapPin,
  RefreshCw,
  Send,
  Star,
  Users,
} from 'lucide-react';
import { formatVnd, SALARY_UNIT_LABELS } from '../components/JobCard';
import ReviewModal from '../components/ReviewModal';
import { useAuth } from '../contexts/AuthContext';
import jobService from '../services/jobService';

const APPLICATION_STATUS_BADGE = {
  PENDING: {
    label: 'Chờ duyệt (PENDING)',
    className: 'bg-amber-100 text-amber-800 border-amber-200',
    boxClass: 'border-amber-200 bg-amber-50 text-amber-900',
    desc: 'Hồ sơ của bạn đang chờ Nhà tuyển dụng xem xét.',
  },
  ACCEPTED: {
    label: 'Đã chấp nhận (ACCEPTED)',
    className: 'bg-emerald-100 text-emerald-800 border-emerald-200',
    boxClass: 'border-emerald-200 bg-emerald-50 text-emerald-900',
    desc: 'Chúc mừng! Nhà tuyển dụng đã chấp nhận hồ sơ ứng tuyển của bạn.',
  },
  REJECTED: {
    label: 'Đã từ chối (REJECTED)',
    className: 'bg-red-100 text-red-800 border-red-200',
    boxClass: 'border-red-200 bg-red-50 text-red-900',
    desc: 'Hồ sơ của bạn chưa phù hợp với đợt tuyển dụng này.',
  },
};

const JobDetailPage = () => {
  const { id } = useParams();
  const { user, isAuthenticated } = useAuth();

  const [job, setJob] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);

  const [coverLetter, setCoverLetter] = useState('');
  const [isApplying, setIsApplying] = useState(false);
  const [applyError, setApplyError] = useState(null);
  const [applySuccess, setApplySuccess] = useState('');
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);

  const loadJobDetail = useCallback(async () => {
    setIsLoading(true);
    setFetchError(null);
    try {
      const response = await jobService.getJobById(id);
      setJob(response.data);
    } catch (error) {
      setFetchError({
        message: error.message || 'Không thể tải thông tin chi tiết tin tuyển dụng.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadJobDetail();
  }, [loadJobDetail]);

  const handleApplySubmit = async (event) => {
    event.preventDefault();
    setIsApplying(true);
    setApplyError(null);
    setApplySuccess('');

    try {
      await jobService.applyJob(id, { coverLetter: coverLetter.trim() });
      setApplySuccess(
        'Nộp hồ sơ ứng tuyển thành công! Nhà tuyển dụng đã nhận được thông báo mới.'
      );
      setCoverLetter('');
      await loadJobDetail();
    } catch (error) {
      setApplyError({
        message: error.message || 'Không thể nộp hồ sơ ứng tuyển.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsApplying(false);
    }
  };

  if (isLoading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6">
        <div className="animate-pulse space-y-5">
          <div className="h-36 rounded-2xl bg-slate-200/80" />
          <div className="h-64 rounded-2xl bg-slate-200/80" />
        </div>
      </div>
    );
  }

  if (fetchError || !job) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-12">
        <div
          role="alert"
          className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-900"
        >
          <div className="flex items-start gap-3">
            <AlertCircle className="mt-0.5 h-6 w-6 shrink-0 text-red-600" />
            <div>
              <h2 className="text-base font-bold">Không thể tải tin tuyển dụng</h2>
              <p className="mt-1 text-sm text-red-800">{fetchError?.message}</p>
              <p className="mt-2 font-mono text-xs text-red-600">
                RequestId: {fetchError?.requestId}
              </p>
              <div className="mt-4 flex items-center gap-3">
                <button
                  type="button"
                  onClick={loadJobDetail}
                  className="inline-flex items-center gap-2 rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700"
                >
                  <RefreshCw className="h-4 w-4" />
                  <span>Thử lại</span>
                </button>
                <Link
                  to="/jobs"
                  className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-700 hover:underline"
                >
                  <ArrowLeft className="h-4 w-4" />
                  <span>Quay lại danh sách việc làm</span>
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  const isOwnerEmployer =
    isAuthenticated && user?.role === 'EMPLOYER' && user?.id === job.employerId;
  const isStudent = isAuthenticated && user?.role === 'STUDENT';
  const unitLabel = SALARY_UNIT_LABELS[job.salaryUnit] || job.salaryUnit;
  const canStudentReviewEmployer =
    isStudent &&
    job.status === 'COMPLETED' &&
    job.myApplicationStatus === 'ACCEPTED' &&
    !job.hasReviewedEmployer;

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6">
      <div className="mb-4 flex items-center justify-between">
        <Link
          to="/jobs"
          className="inline-flex items-center gap-1.5 text-sm font-semibold text-red-600 hover:underline"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Quay lại danh sách việc làm</span>
        </Link>

        {isOwnerEmployer && (
          <div className="flex items-center gap-2.5">
            <Link
              to={`/employer/jobs/${job.id}/applications`}
              className="inline-flex items-center gap-1.5 rounded-xl bg-slate-900 px-4 py-2 text-xs font-bold text-white hover:bg-slate-800"
            >
              <Users className="h-4 w-4" />
              <span>Danh sách ứng viên ({job.applicantCount ?? 0})</span>
            </Link>
            {job.status !== 'COMPLETED' && job.status !== 'CLOSED' && (
              <Link
                to={`/employer/jobs/${job.id}/edit`}
                className="inline-flex items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-bold text-slate-700 hover:bg-slate-50"
              >
                <Edit3 className="h-4 w-4" />
                <span>Chỉnh sửa tin</span>
              </Link>
            )}
          </div>
        )}
      </div>

      {/* Khối thông tin chính của tin tuyển dụng (FR-10) */}
      <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-red-50 px-3 py-1 text-xs font-semibold text-red-700">
            <Briefcase className="h-3.5 w-3.5" />
            <span>{job.categoryName}</span>
          </span>

          <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-bold text-emerald-800">
            Trạng thái: {job.status}
          </span>
        </div>

        <h1 className="mt-3 text-2xl font-bold text-slate-900">{job.title}</h1>

        <div className="mt-2 flex flex-wrap items-center gap-3 text-sm font-semibold text-slate-700">
          <span className="inline-flex items-center gap-1.5">
            <Building2 className="h-4 w-4 text-slate-400" />
            <span>{job.companyName}</span>
          </span>
          {isAuthenticated && (
            <Link
              to={`/profiles/${job.employerId}`}
              className="text-xs font-semibold text-red-600 hover:underline"
            >
              Xem hồ sơ & Đánh giá của Nhà tuyển dụng
            </Link>
          )}
        </div>

        <div className="mt-6 grid grid-cols-1 gap-4 rounded-2xl bg-slate-50 p-4 text-sm sm:grid-cols-2">
          <div className="flex items-center gap-2.5 font-semibold text-emerald-700">
            <Coins className="h-5 w-5 shrink-0 text-emerald-600" />
            <span>
              Mức lương: {formatVnd(job.salaryMin)} - {formatVnd(job.salaryMax)} / {unitLabel}
            </span>
          </div>

          <div className="flex items-center gap-2.5 text-slate-700">
            <MapPin className="h-5 w-5 shrink-0 text-slate-400" />
            <span>Khu vực: {job.locationName}</span>
          </div>

          <div className="flex items-center gap-2.5 text-slate-700">
            <Clock className="h-5 w-5 shrink-0 text-slate-400" />
            <span>Lịch làm việc: {job.workSchedule}</span>
          </div>

          <div className="flex items-center gap-2.5 text-slate-700">
            <Users className="h-5 w-5 shrink-0 text-slate-400" />
            <span>
              Chỉ tiêu tuyển: {job.acceptedCount ?? 0} / {job.slots} sinh viên
            </span>
          </div>

          <div className="flex items-center gap-2.5 text-slate-700 sm:col-span-2">
            <Calendar className="h-5 w-5 shrink-0 text-slate-400" />
            <span>
              Thời gian làm việc: {job.startDate} đến {job.endDate}
            </span>
          </div>
        </div>

        <div className="mt-6 border-t border-slate-100 pt-6">
          <h2 className="text-base font-bold text-slate-900">Mô tả chi tiết công việc</h2>
          <p className="mt-2 whitespace-pre-line text-sm leading-relaxed text-slate-700">
            {job.description}
          </p>
        </div>
      </div>

      {/* Khối Ứng tuyển & Đánh giá dành cho Sinh viên (FR-12, FR-15) */}
      {isStudent && (
        <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <h2 className="text-lg font-bold text-slate-900">
            Ứng tuyển & Đánh giá công việc (FR-12, FR-15)
          </h2>

          {applySuccess && (
            <div className="mt-4 flex items-center gap-2.5 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-800">
              <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600" />
              <span>{applySuccess}</span>
            </div>
          )}

          {applyError && (
            <div
              role="alert"
              className="mt-4 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"
            >
              <div className="flex items-start gap-2.5">
                <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
                <div className="flex-1">
                  <p className="font-semibold">{applyError.message}</p>
                  <p className="mt-1 font-mono text-xs text-red-600">
                    RequestId: {applyError.requestId}
                  </p>
                </div>
              </div>
            </div>
          )}

          {job.hasApplied ? (
            (() => {
              const appStatus = job.myApplicationStatus || 'PENDING';
              const statusCfg =
                APPLICATION_STATUS_BADGE[appStatus] || APPLICATION_STATUS_BADGE.PENDING;
              return (
                <div className={`mt-4 rounded-xl border p-4 text-sm ${statusCfg.boxClass}`}>
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <span className="font-semibold">
                      Trạng thái hồ sơ ứng tuyển của bạn:
                    </span>
                    <span
                      className={`rounded-full border px-3 py-1 text-xs font-bold ${statusCfg.className}`}
                    >
                      {statusCfg.label}
                    </span>
                  </div>
                  <p className="mt-1.5 text-xs opacity-90">{statusCfg.desc}</p>

                  {canStudentReviewEmployer && (
                    <div className="mt-4 flex items-center justify-between border-t border-emerald-200/80 pt-3">
                      <span className="text-xs font-semibold text-emerald-900">
                        Công việc đã hoàn thành (COMPLETED). Hãy gửi đánh giá cho Nhà tuyển dụng!
                      </span>
                      <button
                        type="button"
                        onClick={() => setIsReviewModalOpen(true)}
                        className="inline-flex items-center gap-1.5 rounded-xl bg-amber-500 px-4 py-2 text-xs font-bold text-white shadow-xs transition hover:bg-amber-600"
                      >
                        <Star className="h-3.5 w-3.5 fill-white" />
                        <span>Đánh giá Nhà tuyển dụng (FR-15)</span>
                      </button>
                    </div>
                  )}

                  {job.hasReviewedEmployer && (
                    <div className="mt-3 flex items-center gap-1.5 border-t border-emerald-200/80 pt-3 text-xs font-bold text-emerald-800">
                      <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                      <span>Bạn đã gửi đánh giá cho Nhà tuyển dụng ở công việc này.</span>
                    </div>
                  )}
                </div>
              );
            })()
          ) : job.status !== 'OPEN' ? (
            <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm font-medium text-amber-800">
              Tin tuyển dụng này hiện không còn ở trạng thái mở nhận hồ sơ (OPEN).
            </div>
          ) : (
            <form onSubmit={handleApplySubmit} className="mt-4 space-y-4">
              <div>
                <label
                  htmlFor="coverLetter"
                  className="mb-1.5 block text-sm font-semibold text-slate-700"
                >
                  Thư giới thiệu ngắn gọn (Cover Letter)
                </label>
                <textarea
                  id="coverLetter"
                  rows={4}
                  disabled={isApplying}
                  value={coverLetter}
                  onChange={(e) => setCoverLetter(e.target.value)}
                  placeholder="Giới thiệu lịch rảnh và kinh nghiệm phù hợp của bạn với vị trí này..."
                  className="w-full rounded-xl border border-slate-300 p-3.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
                />
                <p className="mt-1 text-xs text-slate-500">
                  Hệ thống sẽ đính kèm trực tiếp nội dung CV trong Hồ sơ cá nhân (/profile) của bạn.
                </p>
              </div>

              <button
                type="submit"
                disabled={isApplying}
                className="inline-flex items-center gap-2 rounded-xl bg-red-600 px-6 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-red-700 disabled:opacity-60"
              >
                {isApplying ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span>Đang gửi hồ sơ...</span>
                  </>
                ) : (
                  <>
                    <Send className="h-4 w-4" />
                    <span>Ứng tuyển ngay</span>
                  </>
                )}
              </button>
            </form>
          )}
        </div>
      )}

      {!isAuthenticated && (
        <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-center shadow-sm">
          <p className="text-sm text-slate-600">
            Vui lòng đăng nhập bằng tài khoản Sinh viên để nộp hồ sơ ứng tuyển công việc này.
          </p>
          <Link
            to="/login"
            className="mt-3 inline-flex rounded-xl bg-red-600 px-5 py-2.5 text-sm font-bold text-white hover:bg-red-700"
          >
            Đăng nhập để ứng tuyển
          </Link>
        </div>
      )}

      <ReviewModal
        isOpen={isReviewModalOpen}
        onClose={() => setIsReviewModalOpen(false)}
        jobId={job.id}
        jobTitle={job.title}
        revieweeId={job.employerId}
        revieweeName={job.companyName}
        revieweeRoleLabel="Nhà tuyển dụng"
        onSuccess={() => {
          setApplySuccess('Đã gửi đánh giá cho Nhà tuyển dụng thành công!');
          loadJobDetail();
        }}
      />
    </div>
  );
};

export default JobDetailPage;
