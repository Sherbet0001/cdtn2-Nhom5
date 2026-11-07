import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle,
  BarChart3,
  Briefcase,
  CheckCircle2,
  Edit3,
  Eye,
  EyeOff,
  FolderKanban,
  Inbox,
  Loader2,
  Lock,
  MapPin,
  MessageSquareWarning,
  Plus,
  RefreshCw,
  Search,
  ShieldAlert,
  Star,
  Trash2,
  Unlock,
  Users,
  X,
} from 'lucide-react';
import { formatVnd } from '../components/JobCard';
import { useAuth } from '../contexts/AuthContext';
import adminService from '../services/adminService';

const TABS = [
  { id: 'stats', label: 'Thống kê tổng quan (FR-31)', icon: BarChart3 },
  { id: 'users', label: 'Người dùng & Khóa TK (FR-25, 26)', icon: Users },
  { id: 'jobs', label: 'Tin tuyển dụng & Ẩn tin (FR-27)', icon: Briefcase },
  { id: 'catalogs', label: 'Danh mục & Địa điểm (FR-28, 29)', icon: FolderKanban },
  { id: 'reviews', label: 'Kiểm duyệt đánh giá (FR-30)', icon: MessageSquareWarning },
];

const JOB_STATUS_BADGE = {
  OPEN: { label: 'OPEN', className: 'bg-emerald-100 text-emerald-800 border-emerald-200' },
  FILLED: { label: 'FILLED', className: 'bg-blue-100 text-blue-800 border-blue-200' },
  COMPLETED: { label: 'COMPLETED', className: 'bg-purple-100 text-purple-800 border-purple-200' },
  CLOSED: { label: 'CLOSED', className: 'bg-slate-200 text-slate-700 border-slate-300' },
  HIDDEN: { label: 'HIDDEN (Đã ẩn)', className: 'bg-red-100 text-red-800 border-red-200' },
};

const AdminDashboardPage = () => {
  const { user: currentUser } = useAuth();
  const [activeTab, setActiveTab] = useState('stats');
  const [toastMessage, setToastMessage] = useState('');
  const [actionError, setActionError] = useState(null);
  const [processingId, setProcessingId] = useState(null);

  // --- Tab 1: Stats (FR-31) ---
  const [stats, setStats] = useState(null);
  const [isStatsLoading, setIsStatsLoading] = useState(false);
  const [statsError, setStatsError] = useState(null);

  // --- Tab 2: Users (FR-25, FR-26) ---
  const [usersData, setUsersData] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0 });
  const [userFilters, setUserFilters] = useState({ role: '', status: '', keyword: '', page: 0 });
  const [isUsersLoading, setIsUsersLoading] = useState(false);
  const [usersError, setUsersError] = useState(null);

  // --- Tab 3: Jobs (FR-27) ---
  const [jobsData, setJobsData] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0 });
  const [jobFilters, setJobFilters] = useState({ status: '', keyword: '', page: 0 });
  const [isJobsLoading, setIsJobsLoading] = useState(false);
  const [jobsError, setJobsError] = useState(null);

  // --- Tab 4: Catalogs (FR-28, FR-29) ---
  const [categories, setCategories] = useState([]);
  const [locations, setLocations] = useState([]);
  const [isCatalogsLoading, setIsCatalogsLoading] = useState(false);
  const [catalogsError, setCatalogsError] = useState(null);
  const [categoryForm, setCategoryForm] = useState({ id: null, name: '' });
  const [locationForm, setLocationForm] = useState({ id: null, city: '', district: '' });

  // --- Tab 5: Reviews (FR-30) ---
  const [reviewsData, setReviewsData] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0 });
  const [reviewFilters, setReviewFilters] = useState({ status: '', page: 0 });
  const [isReviewsLoading, setIsReviewsLoading] = useState(false);
  const [reviewsError, setReviewsError] = useState(null);

  const clearFeedback = () => {
    setToastMessage('');
    setActionError(null);
  };

  // Load Stats
  const loadStats = useCallback(async () => {
    setIsStatsLoading(true);
    setStatsError(null);
    try {
      const res = await adminService.getStats();
      setStats(res.data);
    } catch (error) {
      setStatsError({
        message: error.message || 'Không thể tải số liệu thống kê hệ thống.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsStatsLoading(false);
    }
  }, []);

  // Load Users
  const loadUsers = useCallback(async (filters = userFilters) => {
    setIsUsersLoading(true);
    setUsersError(null);
    try {
      const params = { page: filters.page, size: 10 };
      if (filters.role) params.role = filters.role;
      if (filters.status) params.status = filters.status;
      if (filters.keyword?.trim()) params.keyword = filters.keyword.trim();
      const res = await adminService.getUsers(params);
      setUsersData(res.data || { content: [], page: 0, totalPages: 0, totalElements: 0 });
    } catch (error) {
      setUsersError({
        message: error.message || 'Không thể tải danh sách tài khoản người dùng.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsUsersLoading(false);
    }
  }, [userFilters]);

  // Load Jobs
  const loadJobs = useCallback(async (filters = jobFilters) => {
    setIsJobsLoading(true);
    setJobsError(null);
    try {
      const params = { page: filters.page, size: 10 };
      if (filters.status) params.status = filters.status;
      if (filters.keyword?.trim()) params.keyword = filters.keyword.trim();
      const res = await adminService.getJobs(params);
      setJobsData(res.data || { content: [], page: 0, totalPages: 0, totalElements: 0 });
    } catch (error) {
      setJobsError({
        message: error.message || 'Không thể tải danh sách tin tuyển dụng.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsJobsLoading(false);
    }
  }, [jobFilters]);

  // Load Catalogs
  const loadCatalogs = useCallback(async () => {
    setIsCatalogsLoading(true);
    setCatalogsError(null);
    try {
      const [catRes, locRes] = await Promise.all([
        adminService.getCategories(),
        adminService.getLocations(),
      ]);
      setCategories(catRes.data || []);
      setLocations(locRes.data || []);
    } catch (error) {
      setCatalogsError({
        message: error.message || 'Không thể tải danh mục ngành nghề và địa điểm.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsCatalogsLoading(false);
    }
  }, []);

  // Load Reviews
  const loadReviews = useCallback(async (filters = reviewFilters) => {
    setIsReviewsLoading(true);
    setReviewsError(null);
    try {
      const params = { page: filters.page, size: 10 };
      if (filters.status) params.status = filters.status;
      const res = await adminService.getReviews(params);
      setReviewsData(res.data || { content: [], page: 0, totalPages: 0, totalElements: 0 });
    } catch (error) {
      setReviewsError({
        message: error.message || 'Không thể tải danh sách đánh giá.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsReviewsLoading(false);
    }
  }, [reviewFilters]);

  useEffect(() => {
    if (activeTab === 'stats') loadStats();
    if (activeTab === 'users') loadUsers(userFilters);
    if (activeTab === 'jobs') loadJobs(jobFilters);
    if (activeTab === 'catalogs') loadCatalogs();
    if (activeTab === 'reviews') loadReviews(reviewFilters);
  }, [activeTab]); // eslint-disable-line react-hooks/exhaustive-deps

  // Actions: Toggle User Status (FR-26)
  const handleToggleUserStatus = async (u) => {
    clearFeedback();
    const targetStatus = u.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    setProcessingId(`user-${u.id}`);
    try {
      await adminService.updateUserStatus(u.id, targetStatus);
      setToastMessage(
        `Đã chuyển trạng thái tài khoản ${u.email} sang ${targetStatus} thành công.`
      );
      loadUsers(userFilters);
    } catch (error) {
      setActionError({
        message: error.message || 'Không thể cập nhật trạng thái tài khoản.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setProcessingId(null);
    }
  };

  // Actions: Hide or Restore Job (FR-27)
  const handleAdminJobAction = async (job, actionStatus) => {
    clearFeedback();
    setProcessingId(`job-${job.id}`);
    try {
      const res = await adminService.updateJobStatus(job.id, actionStatus);
      const updatedStatus = res.data?.status || actionStatus;
      setToastMessage(
        actionStatus === 'HIDDEN'
          ? `Đã ẩn tin tuyển dụng #${job.id} (${job.title}).`
          : `Đã khôi phục tin tuyển dụng #${job.id} về trạng thái ${updatedStatus}.`
      );
      loadJobs(jobFilters);
    } catch (error) {
      setActionError({
        message: error.message || 'Không thể cập nhật trạng thái hiển thị của tin tuyển dụng.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setProcessingId(null);
    }
  };

  // Actions: Category CRUD (FR-28)
  const handleSaveCategory = async (event) => {
    event.preventDefault();
    if (!categoryForm.name.trim()) return;
    clearFeedback();
    setProcessingId('cat-save');
    try {
      if (categoryForm.id) {
        await adminService.updateCategory(categoryForm.id, { name: categoryForm.name.trim() });
        setToastMessage('Đã cập nhật danh mục ngành nghề thành công.');
      } else {
        await adminService.createCategory({ name: categoryForm.name.trim() });
        setToastMessage('Đã thêm mới danh mục ngành nghề thành công.');
      }
      setCategoryForm({ id: null, name: '' });
      loadCatalogs();
    } catch (error) {
      setActionError({
        message: error.message || 'Không thể lưu danh mục ngành nghề.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setProcessingId(null);
    }
  };

  const handleDeleteCategory = async (cat) => {
    clearFeedback();
    setProcessingId(`cat-del-${cat.id}`);
    try {
      await adminService.deleteCategory(cat.id);
      setToastMessage(`Đã xóa danh mục ngành nghề "${cat.name}" thành công.`);
      loadCatalogs();
    } catch (error) {
      setActionError({
        message: error.message || 'Không thể xóa danh mục ngành nghề.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setProcessingId(null);
    }
  };

  // Actions: Location CRUD (FR-29)
  const handleSaveLocation = async (event) => {
    event.preventDefault();
    if (!locationForm.city.trim() || !locationForm.district.trim()) return;
    clearFeedback();
    setProcessingId('loc-save');
    try {
      const payload = {
        city: locationForm.city.trim(),
        district: locationForm.district.trim(),
      };
      if (locationForm.id) {
        await adminService.updateLocation(locationForm.id, payload);
        setToastMessage('Đã cập nhật địa điểm làm việc thành công.');
      } else {
        await adminService.createLocation(payload);
        setToastMessage('Đã thêm mới địa điểm làm việc thành công.');
      }
      setLocationForm({ id: null, city: '', district: '' });
      loadCatalogs();
    } catch (error) {
      setActionError({
        message: error.message || 'Không thể lưu địa điểm làm việc.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setProcessingId(null);
    }
  };

  const handleDeleteLocation = async (loc) => {
    clearFeedback();
    setProcessingId(`loc-del-${loc.id}`);
    try {
      await adminService.deleteLocation(loc.id);
      setToastMessage(`Đã xóa địa điểm "${loc.district}, ${loc.city}" thành công.`);
      loadCatalogs();
    } catch (error) {
      setActionError({
        message: error.message || 'Không thể xóa địa điểm làm việc.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setProcessingId(null);
    }
  };

  // Actions: Toggle Review Status (FR-30)
  const handleToggleReviewStatus = async (rev) => {
    clearFeedback();
    const targetStatus = rev.status === 'VISIBLE' ? 'HIDDEN' : 'VISIBLE';
    setProcessingId(`rev-${rev.id}`);
    try {
      await adminService.updateReviewStatus(rev.id, targetStatus);
      setToastMessage(
        targetStatus === 'HIDDEN'
          ? `Đã ẩn đánh giá #${rev.id} khỏi trang hồ sơ công khai.`
          : `Đã hiển thị lại đánh giá #${rev.id}.`
      );
      loadReviews(reviewFilters);
    } catch (error) {
      setActionError({
        message: error.message || 'Không thể cập nhật trạng thái hiển thị của đánh giá.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      {/* Header */}
      <div className="mb-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <span className="inline-flex items-center gap-1.5 rounded-full bg-purple-100 px-3 py-1 text-xs font-bold text-purple-800">
              <ShieldAlert className="h-3.5 w-3.5" />
              <span>Bảng điều khiển Quản trị viên Hệ thống (Sprint 5: FR-25 → FR-31)</span>
            </span>
            <h1 className="mt-2 text-2xl font-bold text-slate-900">
              Quản trị người dùng, Tin đăng, Danh mục & Kiểm duyệt đánh giá
            </h1>
            <p className="mt-1 text-sm text-slate-600">
              Quản lý toàn diện hoạt động trên nền tảng VLU Part-time Job theo phân quyền RBAC
            </p>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="mt-6 flex flex-wrap gap-2 border-t border-slate-100 pt-4">
          {TABS.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => {
                  clearFeedback();
                  setActiveTab(tab.id);
                }}
                className={`inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-xs font-bold transition ${
                  isActive
                    ? 'bg-slate-900 text-white shadow-sm'
                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                <Icon className="h-4 w-4" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Feedback Banner */}
      {toastMessage && (
        <div className="mb-5 flex items-center justify-between gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-800">
          <div className="flex items-center gap-2.5">
            <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600" />
            <span>{toastMessage}</span>
          </div>
          <button
            type="button"
            onClick={() => setToastMessage('')}
            className="text-emerald-700 hover:text-emerald-900"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {actionError && (
        <div
          role="alert"
          className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-900"
        >
          <div className="flex items-start gap-2.5">
            <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
            <div className="flex-1">
              <p className="font-semibold">{actionError.message}</p>
              <div className="mt-1.5 flex items-center justify-between text-xs text-red-600">
                <span>RequestId: {actionError.requestId}</span>
                <button
                  type="button"
                  onClick={() => setActionError(null)}
                  className="font-semibold underline hover:text-red-800"
                >
                  Đóng
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ==================== TAB 1: THỐNG KÊ TỔNG QUAN (FR-31) ==================== */}
      {activeTab === 'stats' && (
        <section>
          {isStatsLoading && (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {[1, 2, 3, 4].map((n) => (
                <div key={n} className="h-36 animate-pulse rounded-2xl bg-slate-200/80" />
              ))}
            </div>
          )}

          {!isStatsLoading && statsError && (
            <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-900">
              <div className="flex items-start gap-3">
                <AlertCircle className="mt-0.5 h-6 w-6 shrink-0 text-red-600" />
                <div>
                  <h2 className="text-base font-bold">Không thể tải thống kê hệ thống</h2>
                  <p className="mt-1 text-sm text-red-800">{statsError.message}</p>
                  <p className="mt-2 font-mono text-xs text-red-600">
                    RequestId: {statsError.requestId}
                  </p>
                  <button
                    type="button"
                    onClick={loadStats}
                    className="mt-4 inline-flex items-center gap-2 rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700"
                  >
                    <RefreshCw className="h-4 w-4" />
                    <span>Thử lại</span>
                  </button>
                </div>
              </div>
            </div>
          )}

          {!isStatsLoading && !statsError && stats && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold uppercase text-slate-500">
                      Tổng người dùng
                    </span>
                    <Users className="h-5 w-5 text-blue-600" />
                  </div>
                  <p className="mt-3 text-3xl font-extrabold text-slate-900">{stats.totalUsers}</p>
                  <div className="mt-2 flex flex-wrap gap-2 text-xs text-slate-600">
                    <span>SV: <strong>{stats.totalStudents}</strong></span>
                    <span>• NTD: <strong>{stats.totalEmployers}</strong></span>
                    <span>• Khóa: <strong className="text-red-600">{stats.lockedUsers}</strong></span>
                  </div>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold uppercase text-slate-500">
                      Tin tuyển dụng
                    </span>
                    <Briefcase className="h-5 w-5 text-emerald-600" />
                  </div>
                  <p className="mt-3 text-3xl font-extrabold text-slate-900">{stats.totalJobs}</p>
                  <div className="mt-2 flex flex-wrap gap-2 text-xs text-slate-600">
                    <span>OPEN: <strong className="text-emerald-700">{stats.openJobs}</strong></span>
                    <span>• FILLED: <strong>{stats.filledJobs}</strong></span>
                    <span>• DONE: <strong>{stats.completedJobs}</strong></span>
                    <span>• ẨN: <strong className="text-red-600">{stats.hiddenJobs}</strong></span>
                  </div>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold uppercase text-slate-500">
                      Hồ sơ ứng tuyển
                    </span>
                    <FolderKanban className="h-5 w-5 text-amber-600" />
                  </div>
                  <p className="mt-3 text-3xl font-extrabold text-slate-900">
                    {stats.totalApplications}
                  </p>
                  <div className="mt-2 flex flex-wrap gap-2 text-xs text-slate-600">
                    <span>Chờ: <strong>{stats.pendingApplications}</strong></span>
                    <span>• Nhận: <strong className="text-emerald-700">{stats.acceptedApplications}</strong></span>
                    <span>• Từ chối: <strong>{stats.rejectedApplications}</strong></span>
                  </div>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold uppercase text-slate-500">
                      Đánh giá & Danh mục
                    </span>
                    <Star className="h-5 w-5 text-purple-600" />
                  </div>
                  <p className="mt-3 text-3xl font-extrabold text-slate-900">{stats.totalReviews}</p>
                  <div className="mt-2 flex flex-wrap gap-2 text-xs text-slate-600">
                    <span>Hiện: <strong>{stats.visibleReviews}</strong></span>
                    <span>• Ẩn: <strong className="text-red-600">{stats.hiddenReviews}</strong></span>
                    <span>• Ngành: <strong>{stats.totalCategories}</strong></span>
                    <span>• Khu vực: <strong>{stats.totalLocations}</strong></span>
                  </div>
                </div>
              </div>
            </div>
          )}
        </section>
      )}

      {/* ==================== TAB 2: QUẢN LÝ NGƯỜI DÙNG (FR-25, FR-26) ==================== */}
      {activeTab === 'users' && (
        <section className="space-y-4">
          {/* Bộ lọc */}
          <form
            onSubmit={(e) => {
              e.preventDefault();
              const updated = { ...userFilters, page: 0 };
              setUserFilters(updated);
              loadUsers(updated);
            }}
            className="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"
          >
            <div className="relative min-w-[220px] flex-1">
              <Search className="pointer-events-none absolute left-3.5 top-2.5 h-4 w-4 text-slate-400" />
              <input
                type="text"
                value={userFilters.keyword}
                onChange={(e) => setUserFilters((prev) => ({ ...prev, keyword: e.target.value }))}
                placeholder="Tìm theo email, họ tên hoặc công ty..."
                className="w-full rounded-xl border border-slate-300 py-2 pl-9 pr-3 text-xs focus:border-red-600 focus:outline-none"
              />
            </div>

            <select
              value={userFilters.role}
              onChange={(e) => {
                const updated = { ...userFilters, role: e.target.value, page: 0 };
                setUserFilters(updated);
                loadUsers(updated);
              }}
              className="rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-semibold text-slate-700"
            >
              <option value="">Tất cả vai trò</option>
              <option value="STUDENT">Sinh viên (STUDENT)</option>
              <option value="EMPLOYER">Nhà tuyển dụng (EMPLOYER)</option>
              <option value="ADMIN">Quản trị viên (ADMIN)</option>
            </select>

            <select
              value={userFilters.status}
              onChange={(e) => {
                const updated = { ...userFilters, status: e.target.value, page: 0 };
                setUserFilters(updated);
                loadUsers(updated);
              }}
              className="rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-semibold text-slate-700"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động (ACTIVE)</option>
              <option value="LOCKED">Đã khóa (LOCKED)</option>
            </select>

            <button
              type="submit"
              className="rounded-xl bg-slate-900 px-4 py-2 text-xs font-bold text-white hover:bg-slate-800"
            >
              Lọc danh sách
            </button>
          </form>

          {isUsersLoading && (
            <div className="space-y-3">
              {[1, 2, 3].map((n) => (
                <div key={n} className="h-20 animate-pulse rounded-2xl bg-slate-200/80" />
              ))}
            </div>
          )}

          {!isUsersLoading && usersError && (
            <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-900">
              <p className="font-bold">{usersError.message}</p>
              <p className="mt-1 font-mono text-xs text-red-600">RequestId: {usersError.requestId}</p>
              <button
                type="button"
                onClick={() => loadUsers(userFilters)}
                className="mt-3 inline-flex items-center gap-1.5 rounded-xl bg-red-600 px-4 py-2 text-xs font-bold text-white"
              >
                <RefreshCw className="h-3.5 w-3.5" />
                <span>Thử lại</span>
              </button>
            </div>
          )}

          {!isUsersLoading && !usersError && usersData.content.length === 0 && (
            <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">
              <Inbox className="mx-auto h-8 w-8 text-slate-400" />
              <p className="mt-2 text-sm font-bold text-slate-700">
                Không tìm thấy tài khoản người dùng nào khớp bộ lọc
              </p>
            </div>
          )}

          {!isUsersLoading && !usersError && usersData.content.length > 0 && (
            <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="border-b border-slate-200 bg-slate-50 text-slate-600 uppercase">
                    <tr>
                      <th className="px-4 py-3">ID</th>
                      <th className="px-4 py-3">Tài khoản / Họ tên</th>
                      <th className="px-4 py-3">Vai trò</th>
                      <th className="px-4 py-3">Đơn vị / Trường học</th>
                      <th className="px-4 py-3">Trạng thái</th>
                      <th className="px-4 py-3 text-right">Thao tác (FR-26)</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {usersData.content.map((u) => {
                      const isSelf = currentUser?.id === u.id;
                      const isBusy = processingId === `user-${u.id}`;
                      return (
                        <tr key={u.id} className="hover:bg-slate-50/80">
                          <td className="px-4 py-3 font-mono font-bold text-slate-700">#{u.id}</td>
                          <td className="px-4 py-3">
                            <div className="font-bold text-slate-900">{u.fullName}</div>
                            <div className="text-slate-500">{u.email}</div>
                          </td>
                          <td className="px-4 py-3">
                            <span className="rounded-full bg-slate-100 px-2.5 py-0.5 font-bold text-slate-700">
                              {u.role}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-slate-600">
                            {u.companyName || u.school || '—'}
                          </td>
                          <td className="px-4 py-3">
                            <span
                              className={`rounded-full px-2.5 py-0.5 font-bold ${
                                u.status === 'ACTIVE'
                                  ? 'bg-emerald-100 text-emerald-800'
                                  : 'bg-red-100 text-red-800'
                              }`}
                            >
                              {u.status}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-right">
                            <div className="inline-flex items-center gap-2">
                              <Link
                                to={`/profiles/${u.id}`}
                                className="rounded-lg border border-slate-200 px-2.5 py-1.5 font-semibold text-slate-700 hover:bg-slate-100"
                              >
                                Hồ sơ
                              </Link>
                              {!isSelf && (
                                <button
                                  type="button"
                                  disabled={isBusy}
                                  onClick={() => handleToggleUserStatus(u)}
                                  className={`inline-flex items-center gap-1 rounded-lg px-2.5 py-1.5 font-bold text-white transition disabled:opacity-50 ${
                                    u.status === 'ACTIVE'
                                      ? 'bg-red-600 hover:bg-red-700'
                                      : 'bg-emerald-600 hover:bg-emerald-700'
                                  }`}
                                >
                                  {isBusy ? (
                                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                                  ) : u.status === 'ACTIVE' ? (
                                    <Lock className="h-3.5 w-3.5" />
                                  ) : (
                                    <Unlock className="h-3.5 w-3.5" />
                                  )}
                                  <span>{u.status === 'ACTIVE' ? 'Khóa TK' : 'Mở khóa'}</span>
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </section>
      )}

      {/* ==================== TAB 3: QUẢN LÝ TIN TUYỂN DỤNG (FR-27) ==================== */}
      {activeTab === 'jobs' && (
        <section className="space-y-4">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              const updated = { ...jobFilters, page: 0 };
              setJobFilters(updated);
              loadJobs(updated);
            }}
            className="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"
          >
            <div className="relative min-w-[220px] flex-1">
              <Search className="pointer-events-none absolute left-3.5 top-2.5 h-4 w-4 text-slate-400" />
              <input
                type="text"
                value={jobFilters.keyword}
                onChange={(e) => setJobFilters((prev) => ({ ...prev, keyword: e.target.value }))}
                placeholder="Tìm theo tiêu đề hoặc mô tả công việc..."
                className="w-full rounded-xl border border-slate-300 py-2 pl-9 pr-3 text-xs focus:border-red-600 focus:outline-none"
              />
            </div>

            <select
              value={jobFilters.status}
              onChange={(e) => {
                const updated = { ...jobFilters, status: e.target.value, page: 0 };
                setJobFilters(updated);
                loadJobs(updated);
              }}
              className="rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-semibold text-slate-700"
            >
              <option value="">Mọi trạng thái</option>
              <option value="OPEN">Đang mở (OPEN)</option>
              <option value="FILLED">Đã đủ người (FILLED)</option>
              <option value="COMPLETED">Đã hoàn thành (COMPLETED)</option>
              <option value="CLOSED">Đã đóng (CLOSED)</option>
              <option value="HIDDEN">Đã bị ẩn (HIDDEN)</option>
            </select>

            <button
              type="submit"
              className="rounded-xl bg-slate-900 px-4 py-2 text-xs font-bold text-white hover:bg-slate-800"
            >
              Lọc tin đăng
            </button>
          </form>

          {isJobsLoading && (
            <div className="space-y-3">
              {[1, 2, 3].map((n) => (
                <div key={n} className="h-24 animate-pulse rounded-2xl bg-slate-200/80" />
              ))}
            </div>
          )}

          {!isJobsLoading && jobsError && (
            <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-900">
              <p className="font-bold">{jobsError.message}</p>
              <p className="mt-1 font-mono text-xs text-red-600">RequestId: {jobsError.requestId}</p>
              <button
                type="button"
                onClick={() => loadJobs(jobFilters)}
                className="mt-3 inline-flex items-center gap-1.5 rounded-xl bg-red-600 px-4 py-2 text-xs font-bold text-white"
              >
                <RefreshCw className="h-3.5 w-3.5" />
                <span>Thử lại</span>
              </button>
            </div>
          )}

          {!isJobsLoading && !jobsError && jobsData.content.length === 0 && (
            <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">
              <Inbox className="mx-auto h-8 w-8 text-slate-400" />
              <p className="mt-2 text-sm font-bold text-slate-700">
                Không có tin tuyển dụng nào khớp điều kiện lọc
              </p>
            </div>
          )}

          {!isJobsLoading && !jobsError && jobsData.content.length > 0 && (
            <div className="space-y-3">
              {jobsData.content.map((job) => {
                const badge = JOB_STATUS_BADGE[job.status] || {
                  label: job.status,
                  className: 'bg-slate-100 text-slate-700',
                };
                const canHide = job.status === 'OPEN' || job.status === 'FILLED';
                const canRestore = job.status === 'HIDDEN';
                const isBusy = processingId === `job-${job.id}`;

                return (
                  <div
                    key={job.id}
                    className="flex flex-col justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center"
                  >
                    <div className="space-y-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-mono text-xs font-bold text-slate-500">
                          #{job.id}
                        </span>
                        <span
                          className={`rounded-full border px-2.5 py-0.5 text-xs font-bold ${badge.className}`}
                        >
                          {badge.label}
                        </span>
                        <span className="text-xs text-slate-500">
                          Đơn vị: <strong className="text-slate-700">{job.companyName}</strong>
                        </span>
                        <span className="text-xs text-slate-500">
                          • Đã nhận: <strong>{job.acceptedCount}/{job.slots}</strong> ứng viên
                        </span>
                      </div>
                      <Link
                        to={`/jobs/${job.id}`}
                        className="block text-base font-bold text-slate-900 hover:text-red-600"
                      >
                        {job.title}
                      </Link>
                      <p className="text-xs text-slate-600">
                        Ngành: {job.categoryName} • Khu vực: {job.locationName} • Lương:{' '}
                        {formatVnd(job.salaryMin)} - {formatVnd(job.salaryMax)} ({job.salaryUnit})
                      </p>
                    </div>

                    <div className="flex flex-wrap items-center gap-2">
                      {canHide && (
                        <button
                          type="button"
                          disabled={isBusy}
                          onClick={() => handleAdminJobAction(job, 'HIDDEN')}
                          className="inline-flex items-center gap-1.5 rounded-xl bg-red-600 px-3.5 py-2 text-xs font-bold text-white hover:bg-red-700 disabled:opacity-50"
                        >
                          {isBusy ? (
                            <Loader2 className="h-3.5 w-3.5 animate-spin" />
                          ) : (
                            <EyeOff className="h-3.5 w-3.5" />
                          )}
                          <span>Ẩn tin vi phạm (HIDDEN)</span>
                        </button>
                      )}

                      {canRestore && (
                        <button
                          type="button"
                          disabled={isBusy}
                          onClick={() => handleAdminJobAction(job, 'RESTORE')}
                          className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-3.5 py-2 text-xs font-bold text-white hover:bg-emerald-700 disabled:opacity-50"
                        >
                          {isBusy ? (
                            <Loader2 className="h-3.5 w-3.5 animate-spin" />
                          ) : (
                            <Eye className="h-3.5 w-3.5" />
                          )}
                          <span>Khôi phục tin (OPEN/FILLED)</span>
                        </button>
                      )}

                      <Link
                        to={`/jobs/${job.id}`}
                        className="rounded-xl border border-slate-300 px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                      >
                        Chi tiết
                      </Link>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      )}

      {/* ==================== TAB 4: DANH MỤC & ĐỊA ĐIỂM (FR-28, FR-29) ==================== */}
      {activeTab === 'catalogs' && (
        <section className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          {isCatalogsLoading && (
            <>
              <div className="h-80 animate-pulse rounded-2xl bg-slate-200/80" />
              <div className="h-80 animate-pulse rounded-2xl bg-slate-200/80" />
            </>
          )}

          {!isCatalogsLoading && catalogsError && (
            <div
              role="alert"
              className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-900 lg:col-span-2"
            >
              <p className="font-bold">{catalogsError.message}</p>
              <p className="mt-1 font-mono text-xs text-red-600">
                RequestId: {catalogsError.requestId}
              </p>
              <button
                type="button"
                onClick={loadCatalogs}
                className="mt-3 inline-flex items-center gap-1.5 rounded-xl bg-red-600 px-4 py-2 text-xs font-bold text-white"
              >
                <RefreshCw className="h-3.5 w-3.5" />
                <span>Thử lại</span>
              </button>
            </div>
          )}

          {!isCatalogsLoading && !catalogsError && (
            <>
              {/* Cột 1: Danh mục Ngành nghề (FR-28) */}
              <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <h2 className="text-base font-bold text-slate-900">
                  Danh mục Ngành nghề (FR-28)
                </h2>
                <p className="mt-0.5 text-xs text-slate-500">
                  Xóa ngành nghề đang có tin sử dụng sẽ bị chặn với lỗi 409 Conflict
                </p>

                <form onSubmit={handleSaveCategory} className="mt-4 flex gap-2">
                  <input
                    type="text"
                    required
                    value={categoryForm.name}
                    onChange={(e) => setCategoryForm((prev) => ({ ...prev, name: e.target.value }))}
                    placeholder="Tên ngành nghề (vd: Thiết kế đồ họa)..."
                    className="flex-1 rounded-xl border border-slate-300 px-3 py-2 text-xs focus:border-red-600 focus:outline-none"
                  />
                  <button
                    type="submit"
                    disabled={processingId === 'cat-save'}
                    className="inline-flex items-center gap-1 rounded-xl bg-slate-900 px-3.5 py-2 text-xs font-bold text-white hover:bg-slate-800"
                  >
                    <Plus className="h-3.5 w-3.5" />
                    <span>{categoryForm.id ? 'Lưu sửa' : 'Thêm'}</span>
                  </button>
                  {categoryForm.id && (
                    <button
                      type="button"
                      onClick={() => setCategoryForm({ id: null, name: '' })}
                      className="rounded-xl border border-slate-300 px-2.5 py-2 text-xs text-slate-600"
                    >
                      Hủy
                    </button>
                  )}
                </form>

                <ul className="mt-4 divide-y divide-slate-100 text-xs">
                  {categories.map((cat) => (
                    <li key={cat.id} className="flex items-center justify-between py-2.5">
                      <span className="font-semibold text-slate-800">
                        #{cat.id} — {cat.name}
                      </span>
                      <div className="flex items-center gap-1.5">
                        <button
                          type="button"
                          onClick={() => setCategoryForm({ id: cat.id, name: cat.name })}
                          className="rounded-lg border border-slate-200 p-1.5 text-slate-600 hover:bg-slate-100"
                          title="Đổi tên"
                        >
                          <Edit3 className="h-3.5 w-3.5" />
                        </button>
                        <button
                          type="button"
                          disabled={processingId === `cat-del-${cat.id}`}
                          onClick={() => handleDeleteCategory(cat)}
                          className="rounded-lg border border-red-200 p-1.5 text-red-600 hover:bg-red-50"
                          title="Xóa danh mục"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Cột 2: Danh mục Địa điểm (FR-29) */}
              <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <h2 className="text-base font-bold text-slate-900">
                  Danh mục Địa điểm làm việc (FR-29)
                </h2>
                <p className="mt-0.5 text-xs text-slate-500">
                  Xóa địa điểm đang gắn với tin đăng hoặc hồ sơ sẽ trả về 409 Conflict
                </p>

                <form onSubmit={handleSaveLocation} className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-3">
                  <input
                    type="text"
                    required
                    value={locationForm.city}
                    onChange={(e) => setLocationForm((prev) => ({ ...prev, city: e.target.value }))}
                    placeholder="Tỉnh/TP (vd: TP. Hồ Chí Minh)"
                    className="rounded-xl border border-slate-300 px-3 py-2 text-xs focus:border-red-600 focus:outline-none"
                  />
                  <input
                    type="text"
                    required
                    value={locationForm.district}
                    onChange={(e) =>
                      setLocationForm((prev) => ({ ...prev, district: e.target.value }))
                    }
                    placeholder="Quận/Huyện (vd: Quận Phú Nhuận)"
                    className="rounded-xl border border-slate-300 px-3 py-2 text-xs focus:border-red-600 focus:outline-none"
                  />
                  <div className="flex gap-1.5">
                    <button
                      type="submit"
                      disabled={processingId === 'loc-save'}
                      className="flex-1 inline-flex items-center justify-center gap-1 rounded-xl bg-slate-900 px-3 py-2 text-xs font-bold text-white hover:bg-slate-800"
                    >
                      <Plus className="h-3.5 w-3.5" />
                      <span>{locationForm.id ? 'Lưu' : 'Thêm'}</span>
                    </button>
                    {locationForm.id && (
                      <button
                        type="button"
                        onClick={() => setLocationForm({ id: null, city: '', district: '' })}
                        className="rounded-xl border border-slate-300 px-2.5 py-2 text-xs text-slate-600"
                      >
                        Hủy
                      </button>
                    )}
                  </div>
                </form>

                <ul className="mt-4 divide-y divide-slate-100 text-xs">
                  {locations.map((loc) => (
                    <li key={loc.id} className="flex items-center justify-between py-2.5">
                      <span className="inline-flex items-center gap-1.5 font-semibold text-slate-800">
                        <MapPin className="h-3.5 w-3.5 text-slate-400" />
                        <span>
                          #{loc.id} — {loc.district}, {loc.city}
                        </span>
                      </span>
                      <div className="flex items-center gap-1.5">
                        <button
                          type="button"
                          onClick={() =>
                            setLocationForm({
                              id: loc.id,
                              city: loc.city,
                              district: loc.district,
                            })
                          }
                          className="rounded-lg border border-slate-200 p-1.5 text-slate-600 hover:bg-slate-100"
                          title="Sửa địa điểm"
                        >
                          <Edit3 className="h-3.5 w-3.5" />
                        </button>
                        <button
                          type="button"
                          disabled={processingId === `loc-del-${loc.id}`}
                          onClick={() => handleDeleteLocation(loc)}
                          className="rounded-lg border border-red-200 p-1.5 text-red-600 hover:bg-red-50"
                          title="Xóa địa điểm"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
            </>
          )}
        </section>
      )}

      {/* ==================== TAB 5: KIỂM DUYỆT ĐÁNH GIÁ (FR-30) ==================== */}
      {activeTab === 'reviews' && (
        <section className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <div>
              <h2 className="text-sm font-bold text-slate-900">
                Kiểm duyệt Đánh giá hai chiều (FR-30)
              </h2>
              <p className="text-xs text-slate-500">
                Ẩn các đánh giá vi phạm quy chuẩn cộng đồng (chuyển sang HIDDEN sẽ không tính vào điểm trung bình)
              </p>
            </div>

            <select
              value={reviewFilters.status}
              onChange={(e) => {
                const updated = { status: e.target.value, page: 0 };
                setReviewFilters(updated);
                loadReviews(updated);
              }}
              className="rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-semibold text-slate-700"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="VISIBLE">Đang hiển thị (VISIBLE)</option>
              <option value="HIDDEN">Đã ẩn (HIDDEN)</option>
            </select>
          </div>

          {isReviewsLoading && (
            <div className="space-y-3">
              {[1, 2].map((n) => (
                <div key={n} className="h-24 animate-pulse rounded-2xl bg-slate-200/80" />
              ))}
            </div>
          )}

          {!isReviewsLoading && reviewsError && (
            <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-900">
              <p className="font-bold">{reviewsError.message}</p>
              <p className="mt-1 font-mono text-xs text-red-600">
                RequestId: {reviewsError.requestId}
              </p>
              <button
                type="button"
                onClick={() => loadReviews(reviewFilters)}
                className="mt-3 inline-flex items-center gap-1.5 rounded-xl bg-red-600 px-4 py-2 text-xs font-bold text-white"
              >
                <RefreshCw className="h-3.5 w-3.5" />
                <span>Thử lại</span>
              </button>
            </div>
          )}

          {!isReviewsLoading && !reviewsError && reviewsData.content.length === 0 && (
            <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">
              <Inbox className="mx-auto h-8 w-8 text-slate-400" />
              <p className="mt-2 text-sm font-bold text-slate-700">
                Chưa có bản ghi đánh giá nào trong danh sách
              </p>
            </div>
          )}

          {!isReviewsLoading && !reviewsError && reviewsData.content.length > 0 && (
            <div className="space-y-3">
              {reviewsData.content.map((rev) => {
                const isBusy = processingId === `rev-${rev.id}`;
                return (
                  <div
                    key={rev.id}
                    className="flex flex-col justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center"
                  >
                    <div className="space-y-1">
                      <div className="flex flex-wrap items-center gap-2 text-xs">
                        <span className="font-mono font-bold text-slate-500">#{rev.id}</span>
                        <span
                          className={`rounded-full px-2.5 py-0.5 font-bold ${
                            rev.status === 'VISIBLE'
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-red-100 text-red-800'
                          }`}
                        >
                          {rev.status}
                        </span>
                        <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-0.5 font-bold text-amber-800">
                          <Star className="h-3 w-3 fill-amber-500 text-amber-600" />
                          <span>{rev.rating} / 5</span>
                        </span>
                        <span className="text-slate-600">
                          Từ <strong>{rev.reviewerName}</strong> ({rev.reviewerRole}) →{' '}
                          <strong>{rev.revieweeName}</strong>
                        </span>
                      </div>

                      <p className="text-xs text-slate-500">
                        Công việc: <strong className="text-slate-800">{rev.jobTitle}</strong>
                      </p>

                      {rev.comment && (
                        <p className="text-sm text-slate-800">{rev.comment}</p>
                      )}
                    </div>

                    <div>
                      <button
                        type="button"
                        disabled={isBusy}
                        onClick={() => handleToggleReviewStatus(rev)}
                        className={`inline-flex items-center gap-1.5 rounded-xl px-3.5 py-2 text-xs font-bold text-white transition disabled:opacity-50 ${
                          rev.status === 'VISIBLE'
                            ? 'bg-red-600 hover:bg-red-700'
                            : 'bg-emerald-600 hover:bg-emerald-700'
                        }`}
                      >
                        {isBusy ? (
                          <Loader2 className="h-3.5 w-3.5 animate-spin" />
                        ) : rev.status === 'VISIBLE' ? (
                          <EyeOff className="h-3.5 w-3.5" />
                        ) : (
                          <Eye className="h-3.5 w-3.5" />
                        )}
                        <span>
                          {rev.status === 'VISIBLE' ? 'Ẩn đánh giá (HIDDEN)' : 'Khôi phục (VISIBLE)'}
                        </span>
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      )}
    </div>
  );
};

export default AdminDashboardPage;

