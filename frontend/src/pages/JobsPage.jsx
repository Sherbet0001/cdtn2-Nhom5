import { useEffect, useState } from 'react';
import { ArrowRight, BriefcaseBusiness, MapPin, Search, Sparkles } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import profileService from '../services/profileService.js';
import jobService from '../services/jobService.js';

const jobTypes = [
  ['', 'Tất cả hình thức'],
  ['PART_TIME', 'Bán thời gian'],
  ['FULL_TIME', 'Toàn thời gian'],
  ['INTERNSHIP', 'Thực tập'],
  ['CONTRACT', 'Hợp đồng'],
];

const typeLabels = Object.fromEntries(jobTypes.slice(1));
const money = new Intl.NumberFormat('vi-VN');

function salaryLabel(job) {
  if (job.salaryMin == null && job.salaryMax == null) return 'Thỏa thuận';
  if (job.salaryMin != null && job.salaryMax != null) {
    return `${money.format(job.salaryMin)} – ${money.format(job.salaryMax)} ₫`;
  }
  return `${job.salaryMin == null ? 'Tối đa' : 'Từ'} ${money.format(job.salaryMin ?? job.salaryMax)} ₫`;
}

function JobCard({ job }) {
  return (
    <Link
      className="group rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-indigo-200 hover:shadow-md"
      to={`/jobs/${job.id}`}
    >
      <div className="flex items-start gap-4">
        <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-indigo-50 text-indigo-700">
          <BriefcaseBusiness aria-hidden="true" size={22} />
        </div>
        <div className="min-w-0 flex-1">
          <h2 className="line-clamp-2 font-bold text-slate-900 group-hover:text-indigo-700">{job.title}</h2>
          <p className="mt-1 truncate text-sm font-medium text-slate-600">{job.companyName}</p>
        </div>
        <ArrowRight aria-hidden="true" className="mt-1 shrink-0 text-slate-300 transition group-hover:translate-x-1 group-hover:text-indigo-600" size={18} />
      </div>
      <div className="mt-5 flex flex-wrap gap-2 text-xs font-medium">
        <span className="rounded-full bg-indigo-50 px-3 py-1.5 text-indigo-700">{typeLabels[job.jobType]}</span>
        <span className="rounded-full bg-emerald-50 px-3 py-1.5 text-emerald-700">{salaryLabel(job)}</span>
      </div>
      <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-4 text-xs text-slate-500">
        <span className="flex min-w-0 items-center gap-1 truncate">
          <MapPin aria-hidden="true" size={14} />
          {job.location ? `${job.location.district}, ${job.location.city}` : 'Linh hoạt'}
        </span>
        <span>{new Date(job.postedAt).toLocaleDateString('vi-VN')}</span>
      </div>
    </Link>
  );
}

export default function JobsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get('q') || '');
  const [locationId, setLocationId] = useState(searchParams.get('locationId') || '');
  const [jobType, setJobType] = useState(searchParams.get('jobType') || '');
  const [locations, setLocations] = useState([]);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    profileService.getLocations().then(setLocations).catch((requestError) => setError(requestError.message));
  }, []);

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    setError('');
    jobService.search({
      q: searchParams.get('q') || undefined,
      locationId: searchParams.get('locationId') || undefined,
      jobType: searchParams.get('jobType') || undefined,
      page: Number(searchParams.get('page') || 0),
      size: 8,
    }).then((data) => {
      if (active) setResult(data);
    }).catch((requestError) => {
      if (active) setError(requestError.message);
    }).finally(() => {
      if (active) setIsLoading(false);
    });
    return () => { active = false; };
  }, [searchParams]);

  function submitSearch(event) {
    event.preventDefault();
    const next = new URLSearchParams();
    if (keyword.trim()) next.set('q', keyword.trim());
    if (locationId) next.set('locationId', locationId);
    if (jobType) next.set('jobType', jobType);
    setSearchParams(next);
  }

  const page = result?.number ?? 0;
  const totalPages = result?.totalPages ?? 0;
  const firstResult = result?.totalElements ? page * result.size + 1 : 0;
  const lastResult = Math.min((page + 1) * (result?.size ?? 8), result?.totalElements ?? 0);

  return (
    <div className="space-y-10">
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-indigo-700 via-indigo-700 to-violet-800 px-6 py-10 text-white shadow-lg sm:px-10 sm:py-14">
        <div className="absolute -right-14 -top-20 size-72 rounded-full border-[36px] border-white/5" />
        <div className="relative max-w-2xl">
          <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1.5 text-xs font-semibold text-indigo-100">
            <Sparkles aria-hidden="true" size={14} /> Cơ hội mới mỗi ngày
          </span>
          <h1 className="mt-5 text-3xl font-extrabold leading-tight sm:text-5xl">Tìm công việc phù hợp với lịch trình của bạn</h1>
          <p className="mt-4 max-w-xl text-sm leading-6 text-indigo-100 sm:text-base">
            Khám phá việc làm bán thời gian, thực tập và nhiều cơ hội dành cho sinh viên.
          </p>
        </div>
        <form className="relative mt-8 grid gap-3 rounded-2xl bg-white p-3 shadow-xl md:grid-cols-[1.4fr_1fr_0.9fr_auto]" onSubmit={submitSearch}>
          <label className="flex items-center gap-3 px-2">
            <Search aria-hidden="true" className="shrink-0 text-slate-400" size={19} />
            <input aria-label="Từ khóa" className="w-full border-0 py-2 text-sm text-slate-900 outline-none placeholder:text-slate-400" onChange={(event) => setKeyword(event.target.value)} placeholder="Chức danh, kỹ năng, công ty..." value={keyword} />
          </label>
          <label className="flex items-center gap-2 border-t border-slate-100 px-2 pt-2 md:border-l md:border-t-0 md:pl-4 md:pt-0">
            <MapPin aria-hidden="true" className="shrink-0 text-slate-400" size={17} />
            <select aria-label="Khu vực" className="w-full border-0 bg-transparent py-2 text-sm text-slate-700 outline-none" onChange={(event) => setLocationId(event.target.value)} value={locationId}>
              <option value="">Mọi khu vực</option>
              {locations.map((location) => <option key={location.id} value={location.id}>{location.district}, {location.city}</option>)}
            </select>
          </label>
          <select aria-label="Hình thức làm việc" className="border-t border-slate-100 bg-transparent px-2 pt-2 text-sm text-slate-700 outline-none md:border-l md:border-t-0 md:pl-4 md:pt-0" onChange={(event) => setJobType(event.target.value)} value={jobType}>
            {jobTypes.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
          <button className="button-primary !rounded-xl !px-6" type="submit"><Search aria-hidden="true" size={17} /> Tìm việc</button>
        </form>
      </section>

      <section>
        <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
          <div>
            <p className="text-sm font-semibold text-indigo-700">Cơ hội dành cho bạn</p>
            <h2 className="mt-1 text-2xl font-bold text-slate-900">Việc làm mới nhất</h2>
          </div>
          <p className="text-sm text-slate-500">
            {result ? `${firstResult}–${lastResult} / ${result.totalElements} việc làm đang tuyển` : 'Đang tải tin tuyển dụng...'}
          </p>
        </div>

        {error && <p className="notice-error mb-5" role="alert">{error}</p>}
        {isLoading ? (
          <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center text-sm text-slate-500">Đang tìm việc phù hợp...</div>
        ) : result?.content?.length ? (
          <>
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {result.content.map((job) => <JobCard key={job.id} job={job} />)}
            </div>
            {totalPages > 1 && (
              <div className="mt-7 flex items-center justify-center gap-4">
                <button className="button-secondary" disabled={page === 0} onClick={() => setSearchParams((current) => { const next = new URLSearchParams(current); next.set('page', String(page - 1)); return next; })} type="button">Trang trước</button>
                <span className="text-sm text-slate-600">Trang {page + 1} / {totalPages}</span>
                <button className="button-secondary" disabled={page + 1 >= totalPages} onClick={() => setSearchParams((current) => { const next = new URLSearchParams(current); next.set('page', String(page + 1)); return next; })} type="button">Trang sau</button>
              </div>
            )}
          </>
        ) : (
          <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-14 text-center">
            <BriefcaseBusiness aria-hidden="true" className="mx-auto text-slate-300" size={34} />
            <h3 className="mt-4 font-semibold text-slate-800">Chưa tìm thấy việc làm phù hợp</h3>
            <p className="mt-1 text-sm text-slate-500">Thử đổi từ khóa hoặc bộ lọc để xem thêm cơ hội.</p>
          </div>
        )}
      </section>
    </div>
  );
}
