import { useCallback, useEffect, useState } from 'react';
import { Download, FileUp, LoaderCircle, MapPin, Save, UserRound } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext.jsx';
import profileService from '../services/profileService.js';
import { downloadFile } from '../utils/downloadFile.js';

function profileToForm(profile) {
  return {
    fullName: profile.fullName || '',
    phone: profile.phone || '',
    locationId: profile.location?.id ? String(profile.location.id) : '',
    bio: profile.bio || '',
    school: profile.school || '',
    major: profile.major || '',
    cvContent: profile.cvContent || '',
    cvFileName: profile.cvFileName || '',
    companyName: profile.companyName || '',
  };
}

export default function ProfilePage() {
  const { user, syncProfile } = useAuth();
  const [form, setForm] = useState(null);
  const [locations, setLocations] = useState([]);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [selectedCvFile, setSelectedCvFile] = useState(null);
  const [isUploadingCv, setIsUploadingCv] = useState(false);
  const [fileInputKey, setFileInputKey] = useState(0);

  const loadProfile = useCallback(async () => {
    setError('');
    setIsLoading(true);
    try {
      const [profile, availableLocations] = await Promise.all([
        profileService.getMine(),
        profileService.getLocations(),
      ]);
      setForm(profileToForm(profile));
      setLocations(availableLocations);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setNotice('');
    setIsSaving(true);
    try {
      const updatedProfile = await profileService.updateMine({
        ...form,
        fullName: form.fullName.trim(),
        locationId: form.locationId ? Number(form.locationId) : null,
      });
      setForm(profileToForm(updatedProfile));
      syncProfile(updatedProfile);
      setNotice('Hồ sơ đã được cập nhật.');
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setIsSaving(false);
    }
  }

  async function handleCvUpload() {
    if (!selectedCvFile) {
      setError('Vui lòng chọn tệp CV trước khi tải lên.');
      return;
    }
    setError('');
    setNotice('');
    setIsUploadingCv(true);
    try {
      const updatedProfile = await profileService.uploadCvFile(selectedCvFile);
      setForm((current) => ({ ...current, cvFileName: updatedProfile.cvFileName }));
      setSelectedCvFile(null);
      setFileInputKey((current) => current + 1);
      setNotice('Tệp CV đã được tải lên.');
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setIsUploadingCv(false);
    }
  }

  async function handleCvDownload() {
    setError('');
    try {
      const file = await profileService.downloadCvFile();
      downloadFile(file, form.cvFileName);
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  if (isLoading) {
    return (
      <div className="flex min-h-64 items-center justify-center gap-2 text-sm text-slate-600" role="status">
        <LoaderCircle aria-hidden="true" className="animate-spin" size={18} />
        Đang tải hồ sơ...
      </div>
    );
  }

  if (!form) {
    return (
      <section className="mx-auto max-w-2xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <p className="notice-error" role="alert">{error || 'Không thể tải hồ sơ.'}</p>
        <button className="button-secondary mt-4" onClick={loadProfile} type="button">Thử lại</button>
      </section>
    );
  }

  const isStudent = user.role === 'STUDENT';

  return (
    <section className="mx-auto max-w-3xl">
      <div className="mb-6">
        <p className="text-sm font-semibold text-indigo-700">Tài khoản / Hồ sơ</p>
        <h1 className="mt-2 text-2xl font-bold text-slate-900">Hồ sơ cá nhân</h1>
        <p className="mt-2 text-sm text-slate-600">Cập nhật thông tin để sử dụng nền tảng PartJob.</p>
      </div>

      <div className="mb-5 flex items-center gap-3 rounded-xl border border-slate-200 bg-white p-4">
        <span className="flex size-11 items-center justify-center rounded-full bg-indigo-50 text-indigo-700">
          <UserRound aria-hidden="true" />
        </span>
        <div className="min-w-0">
          <p className="truncate font-semibold text-slate-900">{user.email}</p>
          <p className="text-sm text-slate-500">{isStudent ? 'Sinh viên' : 'Nhà tuyển dụng'}</p>
        </div>
      </div>

      {error && <p className="notice-error mb-5" role="alert">{error}</p>}
      {notice && <p className="notice-success mb-5" role="status">{notice}</p>}

      <form className="space-y-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-8" onSubmit={handleSubmit}>
        <div className="grid gap-5 sm:grid-cols-2">
          <div>
            <label className="field-label" htmlFor="profileFullName">Họ và tên</label>
            <input className="field-input" id="profileFullName" maxLength={100} name="fullName" onChange={updateField} required value={form.fullName} />
          </div>
          <div>
            <label className="field-label" htmlFor="phone">Số điện thoại</label>
            <input className="field-input" id="phone" maxLength={20} name="phone" onChange={updateField} value={form.phone} />
          </div>
        </div>

        <div>
          <label className="field-label" htmlFor="locationId">
            <MapPin aria-hidden="true" className="mr-1 inline" size={15} />
            Khu vực
          </label>
          <select className="field-input" id="locationId" name="locationId" onChange={updateField} value={form.locationId}>
            <option value="">Chọn khu vực</option>
            {locations.map((location) => (
              <option key={location.id} value={location.id}>{location.district}, {location.city}</option>
            ))}
          </select>
        </div>

        {isStudent ? (
          <>
            <div className="grid gap-5 sm:grid-cols-2">
              <div>
                <label className="field-label" htmlFor="school">Trường học</label>
                <input className="field-input" id="school" maxLength={100} name="school" onChange={updateField} value={form.school} />
              </div>
              <div>
                <label className="field-label" htmlFor="major">Chuyên ngành</label>
                <input className="field-input" id="major" maxLength={100} name="major" onChange={updateField} value={form.major} />
              </div>
            </div>
            <div>
              <label className="field-label" htmlFor="cvContent">Nội dung CV</label>
              <textarea className="field-input min-h-36 resize-y" id="cvContent" maxLength={20000} name="cvContent" onChange={updateField} placeholder="Kỹ năng, kinh nghiệm và mục tiêu nghề nghiệp..." value={form.cvContent} />
            </div>
            <div className="rounded-xl border border-slate-200 p-4">
              <label className="field-label" htmlFor="cvFile">Tệp CV (PDF, DOC, DOCX; tối đa 5 MB)</label>
              <input
                key={fileInputKey}
                accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                className="field-input"
                id="cvFile"
                onChange={(event) => {
                  setSelectedCvFile(event.target.files?.[0] || null);
                  setError('');
                }}
                type="file"
              />
              {form.cvFileName && <p className="mt-2 text-sm text-slate-600">CV hiện tại: <span className="font-semibold">{form.cvFileName}</span></p>}
              <div className="mt-3 flex flex-wrap gap-2">
                <button className="button-secondary" disabled={!selectedCvFile || isUploadingCv} onClick={handleCvUpload} type="button">
                  {isUploadingCv ? <LoaderCircle aria-hidden="true" className="animate-spin" size={16} /> : <FileUp aria-hidden="true" size={16} />}
                  {isUploadingCv ? 'Đang tải lên...' : 'Tải CV lên'}
                </button>
                {form.cvFileName && <button className="button-secondary" onClick={handleCvDownload} type="button"><Download size={16} /> Tải CV hiện tại</button>}
              </div>
            </div>
          </>
        ) : (
          <div>
            <label className="field-label" htmlFor="companyName">Tên doanh nghiệp / cửa hàng</label>
            <input className="field-input" id="companyName" maxLength={150} name="companyName" onChange={updateField} value={form.companyName} />
          </div>
        )}

        <div>
          <label className="field-label" htmlFor="bio">Giới thiệu</label>
          <textarea className="field-input min-h-28 resize-y" id="bio" maxLength={2000} name="bio" onChange={updateField} value={form.bio} />
        </div>

        <div className="flex justify-end border-t border-slate-100 pt-5">
          <button className="button-primary" disabled={isSaving} type="submit">
            {isSaving ? <LoaderCircle aria-hidden="true" className="animate-spin" size={17} /> : <Save aria-hidden="true" size={17} />}
            {isSaving ? 'Đang lưu...' : 'Lưu hồ sơ'}
          </button>
        </div>
      </form>
    </section>
  );
}
