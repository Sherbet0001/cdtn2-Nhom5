import React, { useCallback, useEffect, useState } from 'react';
import {
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  Inbox,
  RefreshCw,
  RotateCcw,
} from 'lucide-react';
import FilterBar from '../components/FilterBar';
import JobCard from '../components/JobCard';
import jobService from '../services/jobService';
import profileService from '../services/profileService';

const INITIAL_FILTERS = {
  keyword: '',
  categoryId: '',
  locationId: '',
  minSalary: '',
  salaryUnit: '',
};

const PAGE_SIZE = 9;

const JobListPage = () => {
  const [filters, setFilters] = useState(INITIAL_FILTERS);
  const [appliedFilters, setAppliedFilters] = useState(INITIAL_FILTERS);
  const [categories, setCategories] = useState([]);
  const [locations, setLocations] = useState([]);

  // 3-State UI Pattern: Loading, Empty (chưa có dữ liệu vs không khớp bộ lọc), Error
  const [isLoading, setIsLoading] = useState(true);
  const [errorState, setErrorState] = useState(null);

  const [pageData, setPageData] = useState({
    content: [],
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  });

  useEffect(() => {
    const fetchCatalogs = async () => {
      try {
        const [catRes, locRes] = await Promise.all([
          profileService.getCategories(),
          profileService.getLocations(),
        ]);
        setCategories(catRes.data || []);
        setLocations(locRes.data || []);
      } catch {
        // Danh mục có thể tải lại khi người dùng bấm Retry
      }
    };
    fetchCatalogs();
  }, []);

  const fetchJobs = useCallback(async (currentFilters, targetPage) => {
    setIsLoading(true);
    setErrorState(null);

    try {
      const params = {
        page: targetPage,
        size: PAGE_SIZE,
        sort: 'createdAt,desc',
      };
      if (currentFilters.keyword.trim()) {
        params.keyword = currentFilters.keyword.trim();
      }
      if (currentFilters.categoryId) {
        params.categoryId = Number(currentFilters.categoryId);
      }
      if (currentFilters.locationId) {
        params.locationId = Number(currentFilters.locationId);
      }
      if (currentFilters.minSalary && currentFilters.salaryUnit) {
        params.minSalary = Number(currentFilters.minSalary);
        params.salaryUnit = currentFilters.salaryUnit;
      }

      const response = await jobService.getJobs(params);
      const data = response.data || {
        content: [],
        page: 0,
        size: PAGE_SIZE,
        totalElements: 0,
        totalPages: 0,
      };
      setPageData(data);
    } catch (error) {
      setErrorState({
        message: error.message || 'Không thể tải danh sách việc làm part-time.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchJobs(appliedFilters, 0);
  }, [appliedFilters, fetchJobs]);

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    setFilters((prev) => ({ ...prev, [name]: value }));
  };

  const handleSearchSubmit = (event) => {
    event.preventDefault();
    setAppliedFilters({ ...filters });
  };

  const handleResetFilters = () => {
    setFilters(INITIAL_FILTERS);
    setAppliedFilters(INITIAL_FILTERS);
  };

  const handlePageChange = (newPage) => {
    if (newPage < 0 || newPage >= pageData.totalPages) return;
    fetchJobs(appliedFilters, newPage);
  };

  const hasActiveFilter = Boolean(
    appliedFilters.keyword.trim() ||
      appliedFilters.categoryId ||
      appliedFilters.locationId ||
      (appliedFilters.minSalary && appliedFilters.salaryUnit)
  );

  const startItem =
    pageData.totalElements === 0 ? 0 : pageData.page * pageData.size + 1;
  const endItem =
    pageData.totalElements === 0
      ? 0
      : Math.min((pageData.page + 1) * pageData.size, pageData.totalElements);

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-900">
          Sàn việc làm Part-time cho Sinh viên (FR-07, FR-08, FR-09)
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Tìm kiếm từ khóa Full-text và lọc việc làm đang mở tuyển theo ngành nghề, khu vực và mức lương
        </p>
      </div>

      <FilterBar
        filters={filters}
        categories={categories}
        locations={locations}
        isLoading={isLoading}
        onChange={handleFilterChange}
        onSubmit={handleSearchSubmit}
        onReset={handleResetFilters}
      />

      {/* Dòng đếm số lượng tin đăng chuẩn */}
      <div className="my-5 flex flex-wrap items-center justify-between gap-2 text-sm text-slate-600">
        <span>
          Hiển thị{' '}
          <strong className="text-slate-900">
            {startItem}-{endItem}
          </strong>{' '}
          / <strong className="text-slate-900">{pageData.totalElements}</strong> việc làm
        </span>
      </div>

      {/* 1. TRẠNG THÁI ĐANG TẢI (LOADING SKELETON) */}
      {isLoading && (
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3, 4, 5, 6].map((item) => (
            <div
              key={item}
              className="h-64 animate-pulse rounded-2xl border border-slate-200 bg-slate-200/70 p-5"
            />
          ))}
        </div>
      )}

      {/* 3. TRẠNG THÁI LỖI (ERROR STATE KÈM REQUEST ID & RETRY) */}
      {!isLoading && errorState && (
        <div
          role="alert"
          className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-900 shadow-sm"
        >
          <div className="flex items-start gap-3">
            <AlertCircle className="mt-0.5 h-6 w-6 shrink-0 text-red-600" />
            <div className="flex-1">
              <h2 className="text-base font-bold">Lỗi tải danh sách việc làm</h2>
              <p className="mt-1 text-sm text-red-800">{errorState.message}</p>
              <p className="mt-2 font-mono text-xs text-red-600">
                RequestId: {errorState.requestId}
              </p>
              <button
                type="button"
                onClick={() => fetchJobs(appliedFilters, pageData.page)}
                className="mt-4 inline-flex items-center gap-2 rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-red-700"
              >
                <RefreshCw className="h-4 w-4" />
                <span>Thử lại (Retry)</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 2. TRẠNG THÁI RỖNG (EMPTY STATE): Phân biệt chưa có dữ liệu vs không khớp bộ lọc */}
      {!isLoading && !errorState && pageData.content.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">
          <Inbox className="mx-auto h-10 w-10 text-slate-400" />
          <h2 className="mt-3 text-base font-bold text-slate-900">
            {hasActiveFilter
              ? 'Không tìm thấy việc làm phù hợp với bộ lọc'
              : 'Hiện chưa có tin tuyển dụng nào đang mở'}
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            {hasActiveFilter
              ? 'Hãy thử điều chỉnh từ khóa hoặc xóa bộ lọc để xem toàn bộ công việc đang tuyển.'
              : 'Các nhà tuyển dụng sẽ sớm đăng tải công việc part-time mới.'}
          </p>
          {hasActiveFilter && (
            <button
              type="button"
              onClick={handleResetFilters}
              className="mt-4 inline-flex items-center gap-2 rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-red-700"
            >
              <RotateCcw className="h-4 w-4" />
              <span>Xóa bộ lọc</span>
            </button>
          )}
        </div>
      )}

      {/* Danh sách JobCard */}
      {!isLoading && !errorState && pageData.content.length > 0 && (
        <>
          <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
            {pageData.content.map((job) => (
              <JobCard key={job.id} job={job} />
            ))}
          </div>

          {/* Điều hướng phân trang */}
          {pageData.totalPages > 1 && (
            <div className="mt-8 flex items-center justify-center gap-2">
              <button
                type="button"
                disabled={pageData.page === 0}
                onClick={() => handlePageChange(pageData.page - 1)}
                className="inline-flex items-center gap-1 rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-40"
              >
                <ChevronLeft className="h-4 w-4" />
                <span>Trang trước</span>
              </button>

              <span className="px-3 text-xs font-semibold text-slate-700">
                Trang {pageData.page + 1} / {pageData.totalPages}
              </span>

              <button
                type="button"
                disabled={pageData.page + 1 >= pageData.totalPages}
                onClick={() => handlePageChange(pageData.page + 1)}
                className="inline-flex items-center gap-1 rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-40"
              >
                <span>Trang sau</span>
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default JobListPage;

