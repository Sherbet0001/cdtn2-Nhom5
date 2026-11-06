import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  Loader2,
  Save,
} from 'lucide-react';
import jobService from '../services/jobService';
import profileService from '../services/profileService';

const JobFormPage = () => {
  const { id } = useParams();
  const isEditMode = Boolean(id);
  const navigate = useNavigate();

  const [categories, setCategories] = useState([]);
  const [locations, setLocations] = useState([]);
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorState, setErrorState] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    categoryId: '',
    locationId: '',
    salaryMin: '',
    salaryMax: '',
    salaryUnit: 'HOUR',
    workSchedule: '',
    startDate: '',
    endDate: '',
    slots: 1,
  });

  const loadInitialData = useCallback(async () => {
    setIsInitialLoading(true);
    setErrorState(null);
    try {
      const [catRes, locRes] = await Promise.all([
        profileService.getCategories(),
        profileService.getLocations(),
      ]);
      setCategories(catRes.data || []);
      setLocations(locRes.data || []);

      if (isEditMode) {
        const jobRes = await jobService.getJobById(id);
        const job = jobRes.data;
        setFormData({
          title: job.title || '',
          description: job.description || '',
          categoryId: job.categoryId ? String(job.categoryId) : '',
          locationId: job.locationId ? String(job.locationId) : '',
          salaryMin: job.salaryMin !== undefined ? String(job.salaryMin) : '',
          salaryMax: job.salaryMax !== undefined ? String(job.salaryMax) : '',
          salaryUnit: job.salaryUnit || 'HOUR',
          workSchedule: job.workSchedule || '',
          startDate: job.startDate || '',
          endDate: job.endDate || '',
          slots: job.slots || 1,
        });
      }
    } catch (error) {
      setErrorState({
        message: error.message || 'Không thể tải dữ liệu biểu mẫu tin tuyển dụng.',
        errors: error.errors || [],
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsInitialLoading(false);
    }
  }, [id, isEditMode]);

  useEffect(() => {
    loadInitialData();
  }, [loadInitialData]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errorState) setErrorState(null);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setIsSubmitting(true);
    setErrorState(null);
    setSuccessMessage('');

    try {
      const payload = {
        title: formData.title.trim(),
        description: formData.description.trim(),
        categoryId: Number(formData.categoryId),
        locationId: Number(formData.locationId),
        salaryMin: Number(formData.salaryMin),
        salaryMax: Number(formData.salaryMax),
        salaryUnit: formData.salaryUnit,
        workSchedule: formData.workSchedule.trim(),
        startDate: formData.startDate,
        endDate: formData.endDate,
        slots: Number(formData.slots),
      };

      if (isEditMode) {
        await jobService.updateJob(id, payload);
        setSuccessMessage('Cập nhật tin tuyển dụng thành công!');
        setTimeout(() => navigate(`/jobs/${id}`), 800);
      } else {
        const res = await jobService.createJob(payload);
        const createdId = res.data?.jobId || res.data?.id;
        setSuccessMessage('Đăng tin tuyển dụng mới thành công!');
        setTimeout(() => navigate(createdId ? `/jobs/${createdId}` : '/employer/jobs'), 800);
      }
    } catch (error) {
      setErrorState({
        message: error.message || 'Không thể lưu tin tuyển dụng.',
        errors: error.errors || [],
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isInitialLoading) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6">
        <div className="h-96 animate-pulse rounded-2xl bg-slate-200/80" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6">
      <div className="mb-4">
        <Link
          to="/employer/jobs"
          className="inline-flex items-center gap-1.5 text-sm font-semibold text-red-600 hover:underline"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Quay lại Quản lý tin đã đăng</span>
        </Link>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <h1 className="text-2xl font-bold text-slate-900">
          {isEditMode
            ? 'Chỉnh sửa tin tuyển dụng (FR-18)'
            : 'Đăng tin tuyển dụng việc làm mới (FR-17)'}
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Điền đầy đủ tiêu đề, mô tả, mức lương, lịch làm việc và số lượng sinh viên cần tuyển
        </p>

        {errorState && (
          <div
            role="alert"
            className="mt-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"
          >
            <div className="flex items-start gap-2.5">
              <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
              <div className="flex-1">
                <p className="font-semibold">{errorState.message}</p>
                {errorState.errors?.length > 0 && (
                  <ul className="mt-1.5 list-inside list-disc space-y-1 text-xs text-red-700">
                    {errorState.errors.map((err, idx) => (
                      <li key={idx}>{err.message}</li>
                    ))}
                  </ul>
                )}
                <p className="mt-2 font-mono text-xs text-red-600">
                  RequestId: {errorState.requestId}
                </p>
              </div>
            </div>
          </div>
        )}

        {successMessage && (
          <div className="mt-5 flex items-center gap-2.5 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-800">
            <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600" />
            <span>{successMessage}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-6 space-y-5">
          <div>
            <label htmlFor="title" className="mb-1.5 block text-sm font-semibold text-slate-700">
              Tiêu đề công việc *
            </label>
            <input
              id="title"
              name="title"
              type="text"
              required
              disabled={isSubmitting}
              value={formData.title}
              onChange={handleChange}
              placeholder="Ví dụ: Nhân viên phục vụ ca tối tại Cafe Góc Phố"
              className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label
                htmlFor="categoryId"
                className="mb-1.5 block text-sm font-semibold text-slate-700"
              >
                Ngành nghề *
              </label>
              <select
                id="categoryId"
                name="categoryId"
                required
                disabled={isSubmitting}
                value={formData.categoryId}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
              >
                <option value="">-- Chọn ngành nghề --</option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label
                htmlFor="locationId"
                className="mb-1.5 block text-sm font-semibold text-slate-700"
              >
                Khu vực làm việc *
              </label>
              <select
                id="locationId"
                name="locationId"
                required
                disabled={isSubmitting}
                value={formData.locationId}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
              >
                <option value="">-- Chọn quận/huyện --</option>
                {locations.map((loc) => (
                  <option key={loc.id} value={loc.id}>
                    {loc.district}, {loc.city}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div>
              <label
                htmlFor="salaryMin"
                className="mb-1.5 block text-sm font-semibold text-slate-700"
              >
                Lương tối thiểu (VNĐ) *
              </label>
              <input
                id="salaryMin"
                name="salaryMin"
                type="number"
                min="0"
                required
                disabled={isSubmitting}
                value={formData.salaryMin}
                onChange={handleChange}
                placeholder="28000"
                className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
              />
            </div>

            <div>
              <label
                htmlFor="salaryMax"
                className="mb-1.5 block text-sm font-semibold text-slate-700"
              >
                Lương tối đa (VNĐ) *
              </label>
              <input
                id="salaryMax"
                name="salaryMax"
                type="number"
                min="0"
                required
                disabled={isSubmitting}
                value={formData.salaryMax}
                onChange={handleChange}
                placeholder="35000"
                className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
              />
            </div>

            <div>
              <label
                htmlFor="salaryUnit"
                className="mb-1.5 block text-sm font-semibold text-slate-700"
              >
                Đơn vị tính lương *
              </label>
              <select
                id="salaryUnit"
                name="salaryUnit"
                required
                disabled={isSubmitting}
                value={formData.salaryUnit}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
              >
                <option value="HOUR">Theo giờ (HOUR)</option>
                <option value="DAY">Theo ngày (DAY)</option>
                <option value="MONTH">Theo tháng (MONTH)</option>
                <option value="TASK">Theo ca/dự án (TASK)</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div className="sm:col-span-2">
              <label
                htmlFor="workSchedule"
                className="mb-1.5 block text-sm font-semibold text-slate-700"
              >
                Lịch làm việc *
              </label>
              <input
                id="workSchedule"
                name="workSchedule"
                type="text"
                required
                disabled={isSubmitting}
                value={formData.workSchedule}
                onChange={handleChange}
                placeholder="Ví dụ: 18:00 - 22:00 (Thứ 2 - Thứ 6)"
                className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
              />
            </div>

            <div>
              <label htmlFor="slots" className="mb-1.5 block text-sm font-semibold text-slate-700">
                Số lượng tuyển (slots ≥ 1) *
              </label>
              <input
                id="slots"
                name="slots"
                type="number"
                min="1"
                required
                disabled={isSubmitting}
                value={formData.slots}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label
                htmlFor="startDate"
                className="mb-1.5 block text-sm font-semibold text-slate-700"
              >
                Ngày bắt đầu *
              </label>
              <input
                id="startDate"
                name="startDate"
                type="date"
                required
                disabled={isSubmitting}
                value={formData.startDate}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
              />
            </div>

            <div>
              <label
                htmlFor="endDate"
                className="mb-1.5 block text-sm font-semibold text-slate-700"
              >
                Ngày kết thúc *
              </label>
              <input
                id="endDate"
                name="endDate"
                type="date"
                required
                disabled={isSubmitting}
                value={formData.endDate}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
              />
            </div>
          </div>

          <div>
            <label
              htmlFor="description"
              className="mb-1.5 block text-sm font-semibold text-slate-700"
            >
              Mô tả công việc & Yêu cầu ứng viên *
            </label>
            <textarea
              id="description"
              name="description"
              rows={5}
              required
              disabled={isSubmitting}
              value={formData.description}
              onChange={handleChange}
              placeholder="Mô tả công việc cụ thể, yêu cầu kỹ năng và quyền lợi dành cho sinh viên..."
              className="w-full rounded-xl border border-slate-300 p-3.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
            />
          </div>

          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={isSubmitting}
              className="inline-flex items-center gap-2 rounded-xl bg-red-600 px-6 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-red-700 disabled:opacity-60"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>Đang lưu...</span>
                </>
              ) : (
                <>
                  <Save className="h-4 w-4" />
                  <span>{isEditMode ? 'Lưu thay đổi' : 'Đăng tin tuyển dụng'}</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default JobFormPage;

