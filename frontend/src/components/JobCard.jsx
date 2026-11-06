import React from 'react';
import { Link } from 'react-router-dom';
import {
  Briefcase,
  Building2,
  Calendar,
  Clock,
  Coins,
  MapPin,
  Users,
} from 'lucide-react';

export const SALARY_UNIT_LABELS = {
  HOUR: 'giờ',
  DAY: 'ngày',
  MONTH: 'tháng',
  TASK: 'dự án/ca',
};

const STATUS_BADGES = {
  OPEN: { label: 'Đang tuyển', className: 'bg-emerald-100 text-emerald-800 border-emerald-200' },
  FILLED: { label: 'Đã đủ người', className: 'bg-blue-100 text-blue-800 border-blue-200' },
  COMPLETED: { label: 'Đã hoàn thành', className: 'bg-slate-200 text-slate-800 border-slate-300' },
  CLOSED: { label: 'Đã đóng', className: 'bg-amber-100 text-amber-800 border-amber-200' },
  HIDDEN: { label: 'Đã ẩn', className: 'bg-red-100 text-red-800 border-red-200' },
};

export const formatVnd = (amount) => {
  if (amount === null || amount === undefined) return '';
  return Number(amount).toLocaleString('vi-VN') + ' đ';
};

const JobCard = ({ job }) => {
  const unitLabel = SALARY_UNIT_LABELS[job.salaryUnit] || job.salaryUnit;
  const statusBadge = STATUS_BADGES[job.status] || {
    label: job.status,
    className: 'bg-slate-100 text-slate-700 border-slate-200',
  };

  return (
    <article className="flex flex-col justify-between rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-red-300 hover:shadow-md">
      <div>
        <div className="flex flex-wrap items-start justify-between gap-2">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-red-50 px-3 py-1 text-xs font-semibold text-red-700">
            <Briefcase className="h-3.5 w-3.5" />
            <span>{job.categoryName}</span>
          </span>

          <span
            className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold ${statusBadge.className}`}
          >
            {statusBadge.label}
          </span>
        </div>

        <h3 className="mt-3 line-clamp-2 text-lg font-bold text-slate-900">
          <Link to={`/jobs/${job.id}`} className="hover:text-red-600">
            {job.title}
          </Link>
        </h3>

        {job.companyName && (
          <div className="mt-1.5 flex items-center gap-2 text-sm font-medium text-slate-600">
            <Building2 className="h-4 w-4 shrink-0 text-slate-400" />
            <span>{job.companyName}</span>
          </div>
        )}

        <div className="mt-4 space-y-2 text-xs text-slate-600">
          <div className="flex items-center gap-2 font-semibold text-emerald-700">
            <Coins className="h-4 w-4 shrink-0 text-emerald-600" />
            <span>
              {formatVnd(job.salaryMin)} - {formatVnd(job.salaryMax)} / {unitLabel}
            </span>
          </div>

          {job.locationName && (
            <div className="flex items-center gap-2">
              <MapPin className="h-4 w-4 shrink-0 text-slate-400" />
              <span>{job.locationName}</span>
            </div>
          )}

          {job.workSchedule && (
            <div className="flex items-center gap-2">
              <Clock className="h-4 w-4 shrink-0 text-slate-400" />
              <span className="line-clamp-1">{job.workSchedule}</span>
            </div>
          )}

          <div className="flex flex-wrap items-center justify-between gap-2 pt-1 text-slate-500">
            <span className="inline-flex items-center gap-1.5">
              <Users className="h-3.5 w-3.5 text-slate-400" />
              <span>
                Chỉ tiêu: {job.acceptedCount ?? 0}/{job.slots} sinh viên
              </span>
            </span>

            {job.startDate && job.endDate && (
              <span className="inline-flex items-center gap-1">
                <Calendar className="h-3.5 w-3.5 text-slate-400" />
                <span>
                  {job.startDate} → {job.endDate}
                </span>
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="mt-5 flex items-center justify-between border-t border-slate-100 pt-3.5">
        <span className="text-xs text-slate-400">
          {job.applicantCount !== undefined ? `${job.applicantCount} lượt ứng tuyển` : ''}
        </span>
        <Link
          to={`/jobs/${job.id}`}
          className="inline-flex items-center rounded-xl bg-slate-900 px-4 py-2 text-xs font-bold text-white transition hover:bg-red-600"
        >
          Xem chi tiết & Ứng tuyển
        </Link>
      </div>
    </article>
  );
};

export default JobCard;

