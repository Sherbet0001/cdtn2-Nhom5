import { useEffect, useState } from 'react';
import { ArrowLeft, BriefcaseBusiness, LoaderCircle, Save } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import jobService from '../services/jobService.js';
import profileService from '../services/profileService.js';

const emptyForm = {
  title: '',
  description: '',
  requirements: '',
  benefits: '',
  jobType: 'PART_TIME',
  salaryMin: '',
  salaryMax: '',
  locationId: '',
  expiresAt: '',
};

function toForm(job) {
  return {
    title: job.title || '',
    description: job.description || '',
    requirements: job.requirements || '',
    benefits: job.benefits || '',
    jobType: job.jobType || 'PART_TIME',
    salaryMin: job.salaryMin ?? '',
    salaryMax: job.salaryMax ?? '',
    locationId: job.location?.id ? String(job.location.id) : '',
    expiresAt: job.expiresAt || '',
  };
}

export default function JobFormPage() {
  const { jobId } = useParams();
  const navigate = useNavigate();
  const isEditing = Boolean(jobId);
  const [form, setForm] = useState(emptyForm);
  const [locations, setLocations] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(isEditing);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let active = true;
    Promise.all([
      profileService.getLocations(),
      isEditing ? jobService.getMine() : Promise.resolve(null),
    ]).then(([availableLocations, jobs]) => {
      if (!active) return;
      setLocations(availableLocations);
      if (isEditing) {
        const job = jobs.find((item) => String(item.id) === jobId);
        if (!job) throw new Error('Không tìm thấy tin tuyển dụng của bạn.');
        setForm(toForm(job));
      }
    }).catch((requestError) => {
      if (active) setError(requestError.message);
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, [isEditing, jobId]);

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    setError('');
    setSaving(true);
    const payload = {
      ...form,
      title: form.title.trim(),
      description: form.description.trim(),
      requirements: form.requirements.trim() || null,
      benefits: form.benefits.trim() || null,
      salaryMin: form.salaryMin === '' ? null : Number(form.salaryMin),
      salaryMax: form.salaryMax === '' ? null : Number(form.salaryMax),
      locationId: form.locationId ? Number(form.locationId) : null,
      expiresAt: form.expiresAt || null,
    };
    try {
      if (isEditing) await jobService.update(jobId, payload);
      else await jobService.create(payload);
      navigate('/employer/jobs', { replace: true });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <p className="py-16 text-center text-sm text-slate-500">Đang tải biểu mẫu...</p>;

  return (
    <div className="mx-auto max-w-4xl">
      <Link className="mb-5 inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-indigo-700" to="/employer/jobs"><ArrowLeft size={16} /> Quản lý tin</Link>
      <div className="mb-6 flex items-start gap-3">
        <div className="flex size-11 items-center justify-center rounded-xl bg-indigo-50 text-indigo-700"><BriefcaseBusiness size={21} /></div>
        <div><p className="text-sm font-semibold text-indigo-700">Nhà tuyển dụng</p><h1 className="text-2xl font-extrabold text-slate-900">{isEditing ? 'Chỉnh sửa tin tuyển dụng' : 'Đăng tin tuyển dụng'}</h1><p className="mt-1 text-sm text-slate-600">Thông tin rõ ràng giúp ứng viên hiểu đúng về vị trí công việc.</p></div>
      </div>
      {error && <p className="notice-error mb-5" role="alert">{error}</p>}
      <form className="space-y-7 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-8" onSubmit={submit}>
        <section className="space-y-5">
          <h2 className="border-b border-slate-100 pb-3 font-bold text-slate-900">Thông tin vị trí</h2>
          <div>
            <label className="field-label" htmlFor="title">Tên công việc <span className="text-rose-500">*</span></label>
            <input autoFocus className="field-input" id="title" maxLength={150} name="title" onChange={updateField} placeholder="Ví dụ: Nhân viên phục vụ ca tối" required value={form.title} />
          </div>
          <div className="grid gap-5 sm:grid-cols-2">
            <div><label className="field-label" htmlFor="jobType">Hình thức làm việc <span className="text-rose-500">*</span></label><select className="field-input" id="jobType" name="jobType" onChange={updateField} value={form.jobType}><option value="PART_TIME">Bán thời gian</option><option value="FULL_TIME">Toàn thời gian</option><option value="INTERNSHIP">Thực tập</option><option value="CONTRACT">Hợp đồng</option></select></div>
            <div><label className="field-label" htmlFor="locationId">Khu vực</label><select className="field-input" id="locationId" name="locationId" onChange={updateField} value={form.locationId}><option value="">Linh hoạt / thỏa thuận</option>{locations.map((location) => <option key={location.id} value={location.id}>{location.district}, {location.city}</option>)}</select></div>
          </div>
          <div className="grid gap-5 sm:grid-cols-2">
            <div><label className="field-label" htmlFor="salaryMin">Lương tối thiểu (VNĐ)</label><input className="field-input" id="salaryMin" min="0" name="salaryMin" onChange={updateField} placeholder="Ví dụ: 25000" type="number" value={form.salaryMin} /></div>
            <div><label className="field-label" htmlFor="salaryMax">Lương tối đa (VNĐ)</label><input className="field-input" id="salaryMax" min="0" name="salaryMax" onChange={updateField} placeholder="Ví dụ: 40000" type="number" value={form.salaryMax} /></div>
          </div>
          <div><label className="field-label" htmlFor="expiresAt">Hạn tuyển dụng</label><input className="field-input sm:max-w-xs" id="expiresAt" min={new Date().toISOString().slice(0, 10)} name="expiresAt" onChange={updateField} type="date" value={form.expiresAt} /></div>
        </section>
        <section className="space-y-5">
          <h2 className="border-b border-slate-100 pb-3 font-bold text-slate-900">Mô tả chi tiết</h2>
          <div><label className="field-label" htmlFor="description">Mô tả công việc <span className="text-rose-500">*</span></label><textarea className="field-input min-h-40 resize-y" id="description" maxLength={20000} name="description" onChange={updateField} placeholder="Nhiệm vụ chính, thời gian làm việc, địa điểm..." required value={form.description} /></div>
          <div><label className="field-label" htmlFor="requirements">Yêu cầu ứng viên</label><textarea className="field-input min-h-32 resize-y" id="requirements" maxLength={10000} name="requirements" onChange={updateField} placeholder="Kỹ năng, kinh nghiệm, thời gian có thể làm..." value={form.requirements} /></div>
          <div><label className="field-label" htmlFor="benefits">Quyền lợi</label><textarea className="field-input min-h-32 resize-y" id="benefits" maxLength={10000} name="benefits" onChange={updateField} placeholder="Mức lương, phụ cấp, đào tạo, môi trường làm việc..." value={form.benefits} /></div>
        </section>
        <div className="flex flex-col-reverse justify-end gap-3 border-t border-slate-100 pt-5 sm:flex-row">
          <Link className="button-secondary" to="/employer/jobs">Hủy</Link>
          <button className="button-primary" disabled={saving} type="submit">{saving ? <LoaderCircle aria-hidden="true" className="animate-spin" size={17} /> : <Save aria-hidden="true" size={17} />}{saving ? 'Đang lưu...' : isEditing ? 'Lưu thay đổi' : 'Đăng tin tuyển dụng'}</button>
        </div>
      </form>
    </div>
  );
}
