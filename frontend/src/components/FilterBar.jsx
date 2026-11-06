import React from 'react';
import { RotateCcw, Search } from 'lucide-react';

const FilterBar = ({
  filters,
  categories,
  locations,
  isLoading,
  onChange,
  onSubmit,
  onReset,
}) => {
  return (
    <form
      onSubmit={onSubmit}
      className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
    >
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-5">
        {/* Từ khóa Full-text (FR-08) */}
        <div className="lg:col-span-2">
          <label htmlFor="keyword" className="mb-1 block text-xs font-bold text-slate-700">
            Từ khóa công việc (Tiêu đề / Mô tả)
          </label>
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input
              id="keyword"
              name="keyword"
              type="text"
              disabled={isLoading}
              value={filters.keyword}
              onChange={onChange}
              placeholder="Ví dụ: pha chế, trợ giảng, kiểm thử..."
              className="w-full rounded-xl border border-slate-300 py-2 pl-9 pr-3 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
            />
          </div>
        </div>

        {/* Ngành nghề (FR-09) */}
        <div>
          <label htmlFor="categoryId" className="mb-1 block text-xs font-bold text-slate-700">
            Ngành nghề
          </label>
          <select
            id="categoryId"
            name="categoryId"
            disabled={isLoading}
            value={filters.categoryId}
            onChange={onChange}
            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
          >
            <option value="">Tất cả ngành nghề</option>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.name}
              </option>
            ))}
          </select>
        </div>

        {/* Địa điểm (FR-09) */}
        <div>
          <label htmlFor="locationId" className="mb-1 block text-xs font-bold text-slate-700">
            Khu vực làm việc
          </label>
          <select
            id="locationId"
            name="locationId"
            disabled={isLoading}
            value={filters.locationId}
            onChange={onChange}
            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
          >
            <option value="">Tất cả quận/huyện</option>
            {locations.map((loc) => (
              <option key={loc.id} value={loc.id}>
                {loc.district}, {loc.city}
              </option>
            ))}
          </select>
        </div>

        {/* Mức lương tối thiểu + Đơn vị tính lương */}
        <div className="grid grid-cols-2 gap-2">
          <div>
            <label htmlFor="minSalary" className="mb-1 block text-xs font-bold text-slate-700">
              Lương tối thiểu
            </label>
            <input
              id="minSalary"
              name="minSalary"
              type="number"
              min="0"
              step="1000"
              disabled={isLoading}
              value={filters.minSalary}
              onChange={onChange}
              placeholder="25000"
              className="w-full rounded-xl border border-slate-300 px-2.5 py-2 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
            />
          </div>

          <div>
            <label htmlFor="salaryUnit" className="mb-1 block text-xs font-bold text-slate-700">
              Đơn vị lương
            </label>
            <select
              id="salaryUnit"
              name="salaryUnit"
              disabled={isLoading}
              value={filters.salaryUnit}
              onChange={onChange}
              className="w-full rounded-xl border border-slate-300 bg-white px-2 py-2 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
            >
              <option value="">-- Chọn --</option>
              <option value="HOUR">Theo giờ</option>
              <option value="DAY">Theo ngày</option>
              <option value="MONTH">Theo tháng</option>
              <option value="TASK">Theo ca/dự án</option>
            </select>
          </div>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-3">
        <p className="text-xs text-slate-500">
          Lưu ý: Bộ lọc mức lương tối thiểu chỉ áp dụng khi chọn kèm đơn vị tính lương.
        </p>

        <div className="flex items-center gap-2.5">
          <button
            type="button"
            disabled={isLoading}
            onClick={onReset}
            className="inline-flex items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 transition hover:bg-slate-50 disabled:opacity-50"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            <span>Xóa bộ lọc</span>
          </button>

          <button
            type="submit"
            disabled={isLoading}
            className="inline-flex items-center gap-1.5 rounded-xl bg-red-600 px-5 py-2 text-xs font-bold text-white shadow-sm transition hover:bg-red-700 disabled:opacity-50"
          >
            <Search className="h-3.5 w-3.5" />
            <span>Tìm việc làm</span>
          </button>
        </div>
      </div>
    </form>
  );
};

export default FilterBar;

