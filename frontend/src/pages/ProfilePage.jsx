import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  BookOpen,
  Building2,
  CheckCircle2,
  FileText,
  GraduationCap,
  Inbox,
  Loader2,
  MapPin,
  Phone,
  RefreshCw,
  Save,
  Search,
  Star,
  UserCheck,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import profileService from '../services/profileService';
import reviewService from '../services/reviewService';

const ProfilePage = () => {
  const { user, syncUserProfileName } = useAuth();
  const { userId: paramUserId } = useParams();
  const navigate = useNavigate();

  const isViewingOtherUser = Boolean(paramUserId);

  // 3-State UI Pattern:
  // 1) Loading State (isInitialLoading, isSubmitting)
  // 2) Empty State (isProfileIncomplete)
  // 3) Error State (fetchError, submitError) kèm requestId và nút Thử lại (Retry)
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [fetchError, setFetchError] = useState(null);
  const [submitError, setSubmitError] = useState(null);
  const [successToast, setSuccessToast] = useState('');
  const [lookupUserId, setLookupUserId] = useState('');

  const [profile, setProfile] = useState(null);
  const [locations, setLocations] = useState([]);
  const [reviewsSummary, setReviewsSummary] = useState({
    averageRating: 0,
    totalReviews: 0,
    reviews: [],
  });

  const [formData, setFormData] = useState({
    fullName: '',
    phone: '',
    locationId: '',
    bio: '',
    school: '',
    major: '',
    cvContent: '',
    companyName: '',
  });

  const loadProfileAndLocations = useCallback(async () => {
    setIsInitialLoading(true);
    setFetchError(null);

    try {
      const [profileRes, locationsRes] = await Promise.all([
        isViewingOtherUser
          ? profileService.getUserProfileById(paramUserId)
          : profileService.getMyProfile(),
        profileService.getLocations(),
      ]);

      const fetchedProfile = profileRes.data;
      const fetchedLocations = locationsRes.data || [];

      setProfile(fetchedProfile);
      setLocations(fetchedLocations);

      if (fetchedProfile?.userId) {
        try {
          const revRes = await reviewService.getUserReviews(fetchedProfile.userId);
          if (revRes.data) {
            setReviewsSummary(revRes.data);
          }
        } catch {
          // Giữ mặc định nếu chưa có đánh giá
        }
      }

      if (fetchedProfile) {
        setFormData({
          fullName: fetchedProfile.fullName || '',
          phone: fetchedProfile.phone || '',
          locationId: fetchedProfile.locationId ? String(fetchedProfile.locationId) : '',
          bio: fetchedProfile.bio || '',
          school: fetchedProfile.school || '',
          major: fetchedProfile.major || '',
          cvContent: fetchedProfile.cvContent || '',
          companyName: fetchedProfile.companyName || '',
        });
      }
    } catch (error) {
      setFetchError({
        message: error.message || 'Không thể tải dữ liệu hồ sơ cá nhân từ máy chủ.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsInitialLoading(false);
    }
  }, [isViewingOtherUser, paramUserId]);

  useEffect(() => {
    loadProfileAndLocations();
  }, [loadProfileAndLocations]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (submitError) setSubmitError(null);
    if (successToast) setSuccessToast('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (isViewingOtherUser) return;

    setIsSubmitting(true);
    setSubmitError(null);
    setSuccessToast('');

    try {
      const payload = {
        fullName: formData.fullName.trim(),
        phone: formData.phone.trim(),
        locationId: formData.locationId ? Number(formData.locationId) : null,
        bio: formData.bio.trim(),
        school: formData.school.trim(),
        major: formData.major.trim(),
        cvContent: formData.cvContent.trim(),
        companyName: formData.companyName.trim(),
      };

      const response = await profileService.updateMyProfile(payload);
      const updated = response.data;
      setProfile(updated);
      syncUserProfileName(updated.fullName);
      setSuccessToast('Cập nhật hồ sơ cá nhân thành công!');
    } catch (error) {
      setSubmitError({
        message: error.message || 'Không thể lưu thay đổi hồ sơ cá nhân.',
        errors: error.errors || [],
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCandidateLookup = (event) => {
    event.preventDefault();
    const targetId = lookupUserId.trim();
    if (!targetId) return;
    navigate(`/profiles/${targetId}`);
  };

  const isStudent = (profile?.role || (!isViewingOtherUser && user?.role)) === 'STUDENT';
  const isEmployer = (profile?.role || (!isViewingOtherUser && user?.role)) === 'EMPLOYER';
  const canLookupOtherProfile = user?.role === 'EMPLOYER' || user?.role === 'ADMIN';

  // Kiểm tra trạng thái rỗng (Empty State) khi người dùng mới đăng ký, chưa điền CV / Thông tin đơn vị
  const isProfileIncomplete =
    profile &&
    !profile.phone &&
    !profile.bio &&
    ((isStudent && !profile.cvContent && !profile.school) ||
      (isEmployer && !profile.companyName));

  // 1. TRẠNG THÁI ĐANG TẢI (LOADING STATE - SKELETON SCREEN)
  if (isInitialLoading) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6">
        <div className="animate-pulse space-y-6">
          <div className="h-28 rounded-2xl bg-slate-200/80" />
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
            <div className="h-80 rounded-2xl bg-slate-200/80" />
            <div className="h-96 rounded-2xl bg-slate-200/80 lg:col-span-2" />
          </div>
        </div>
      </div>
    );
  }

  // 3. TRẠNG THÁI LỖI KHI TẢI DỮ LIỆU (ERROR STATE KÈM REQUEST ID VÀ NÚT RETRY)
  if (fetchError) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-12">
        <div
          role="alert"
          className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-900 shadow-sm"
        >
          <div className="flex items-start gap-3">
            <AlertCircle className="mt-0.5 h-6 w-6 shrink-0 text-red-600" />
            <div className="flex-1">
              <h2 className="text-base font-bold">Không thể tải hồ sơ người dùng</h2>
              <p className="mt-1 text-sm text-red-800">{fetchError.message}</p>
              <p className="mt-2 font-mono text-xs text-red-600">
                Mã tra cứu sự cố (RequestId): {fetchError.requestId}
              </p>
              <div className="mt-4 flex flex-wrap items-center gap-3">
                <button
                  type="button"
                  onClick={loadProfileAndLocations}
                  className="inline-flex items-center gap-2 rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-red-700"
                >
                  <RefreshCw className="h-4 w-4" />
                  <span>Thử lại (Retry)</span>
                </button>
                {isViewingOtherUser && (
                  <Link
                    to="/profile"
                    className="inline-flex items-center gap-2 rounded-xl border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-700 transition hover:bg-red-100"
                  >
                    <ArrowLeft className="h-4 w-4" />
                    <span>Quay về hồ sơ của tôi</span>
                  </Link>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6">
      {isViewingOtherUser && (
        <div className="mb-4">
          <Link
            to="/profile"
            className="inline-flex items-center gap-1.5 text-sm font-semibold text-red-600 hover:underline"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Quay về hồ sơ cá nhân của tôi (/profile)</span>
          </Link>
        </div>
      )}

      {/* Header Banner theo vai trò */}
      <div className="mb-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div>
            <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
              <UserCheck className="h-3.5 w-3.5 text-red-600" />
              {isViewingOtherUser
                ? `Hồ sơ ứng viên / người dùng #${profile?.userId}`
                : isStudent
                ? 'Hồ sơ Sinh viên & CV ứng tuyển (FR-05, FR-11)'
                : isEmployer
                ? 'Hồ sơ Nhà tuyển dụng & Đơn vị (FR-05, FR-16)'
                : 'Hồ sơ Quản trị viên Hệ thống (FR-05)'}
            </span>
            <h1 className="mt-2 text-2xl font-bold text-slate-900">{profile?.fullName}</h1>
            <p className="text-sm text-slate-500">{profile?.email}</p>
          </div>

          {profile?.updatedAt && (
            <div className="text-xs text-slate-500">
              Cập nhật lần cuối:{' '}
              <span className="font-medium text-slate-700">
                {new Date(profile.updatedAt).toLocaleString('vi-VN')}
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Thanh tra cứu nhanh hồ sơ ứng viên theo userId dành cho NTD / Admin (GET /api/v1/profiles/{userId}) */}
      {canLookupOtherProfile && !isViewingOtherUser && (
        <form
          onSubmit={handleCandidateLookup}
          className="mb-6 flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between"
        >
          <div>
            <h2 className="text-sm font-bold text-slate-900">
              Tra cứu hồ sơ ứng viên theo User ID (GET /api/v1/profiles/&#123;userId&#125;)
            </h2>
            <p className="text-xs text-slate-500">
              Nhà tuyển dụng chỉ xem được hồ sơ Sinh viên đã ứng tuyển vào tin của mình (ví dụ: User ID 4).
            </p>
          </div>
          <div className="flex items-center gap-2">
            <input
              type="number"
              min="1"
              value={lookupUserId}
              onChange={(e) => setLookupUserId(e.target.value)}
              placeholder="Nhập User ID (vd: 4)"
              className="w-44 rounded-xl border border-slate-300 px-3.5 py-2 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20"
            />
            <button
              type="submit"
              className="inline-flex items-center gap-1.5 rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800"
            >
              <Search className="h-4 w-4" />
              <span>Xem hồ sơ</span>
            </button>
          </div>
        </form>
      )}

      {/* 2. TRẠNG THÁI RỖNG (EMPTY STATE): Khi tài khoản mới đăng ký chưa có thông tin chi tiết / CV */}
      {isProfileIncomplete && (
        <div className="mb-6 rounded-2xl border border-dashed border-amber-300 bg-amber-50/70 p-5 text-amber-900">
          <div className="flex items-start gap-3">
            <Inbox className="mt-0.5 h-6 w-6 shrink-0 text-amber-600" />
            <div>
              <h2 className="text-sm font-bold">
                {isStudent
                  ? 'Hồ sơ chưa có thông tin CV và trường học'
                  : 'Hồ sơ doanh nghiệp/cửa hàng chưa được cập nhật'}
              </h2>
              <p className="mt-1 text-xs text-amber-800">
                {isStudent
                  ? 'Vui lòng bổ sung trường học, chuyên ngành và nội dung CV bên dưới để có thể ứng tuyển việc làm part-time (FR-11, FR-12).'
                  : 'Vui lòng bổ sung tên đơn vị/cửa hàng, địa điểm và giới thiệu để tăng độ tin cậy khi đăng tin tuyển dụng (FR-16).'}
              </p>
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Cột trái: Thẻ tóm tắt trực quan (Chỉ hiển thị trường có dữ liệu thật từ API, không hardcode) */}
        <aside
          className={`space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm ${
            isViewingOtherUser ? 'lg:col-span-3' : ''
          }`}
        >
          <h2 className="text-base font-bold text-slate-900">Tóm tắt hồ sơ hiện tại</h2>

          <div className="space-y-3 text-sm">
            {profile?.phone && (
              <div className="flex items-center gap-2.5 text-slate-700">
                <Phone className="h-4 w-4 shrink-0 text-slate-400" />
                <span>{profile.phone}</span>
              </div>
            )}

            {profile?.locationName && (
              <div className="flex items-center gap-2.5 text-slate-700">
                <MapPin className="h-4 w-4 shrink-0 text-slate-400" />
                <span>{profile.locationName}</span>
              </div>
            )}

            {isStudent && profile?.school && (
              <div className="flex items-center gap-2.5 text-slate-700">
                <GraduationCap className="h-4 w-4 shrink-0 text-slate-400" />
                <span>{profile.school}</span>
              </div>
            )}

            {isStudent && profile?.major && (
              <div className="flex items-center gap-2.5 text-slate-700">
                <BookOpen className="h-4 w-4 shrink-0 text-slate-400" />
                <span>Chuyên ngành: {profile.major}</span>
              </div>
            )}

            {isEmployer && profile?.companyName && (
              <div className="flex items-center gap-2.5 text-slate-700">
                <Building2 className="h-4 w-4 shrink-0 text-slate-400" />
                <span className="font-semibold">{profile.companyName}</span>
              </div>
            )}
          </div>

          {profile?.bio && (
            <div className="border-t border-slate-100 pt-4">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                Giới thiệu
              </h3>
              <p className="mt-1.5 whitespace-pre-line text-sm text-slate-700">{profile.bio}</p>
            </div>
          )}

          {isStudent && (
            <div className="border-t border-slate-100 pt-4">
              <h3 className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-slate-400">
                <FileText className="h-3.5 w-3.5" />
                <span>Nội dung CV (FR-11)</span>
              </h3>
              {profile?.cvContent ? (
                <div className="mt-2 rounded-xl bg-slate-50 p-3 text-xs leading-relaxed text-slate-700 whitespace-pre-line">
                  {profile.cvContent}
                </div>
              ) : (
                <p className="mt-2 text-xs italic text-slate-400">
                  Chưa cập nhật nội dung CV cá nhân.
                </p>
              )}
            </div>
          )}
        </aside>

        {/* Cột phải: Form cập nhật hồ sơ cá nhân PUT /api/v1/profiles/me */}
        {!isViewingOtherUser && (
          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm lg:col-span-2">
            <h2 className="mb-4 text-lg font-bold text-slate-900">
              Cập nhật thông tin hồ sơ
            </h2>

            {/* Thông báo lỗi khi Submit (Error State) */}
            {submitError && (
              <div
                role="alert"
                className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"
              >
                <div className="flex items-start gap-2.5">
                  <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
                  <div className="flex-1">
                    <p className="font-semibold">{submitError.message}</p>
                    {submitError.errors?.length > 0 && (
                      <ul className="mt-1.5 list-inside list-disc space-y-1 text-xs text-red-700">
                        {submitError.errors.map((err, idx) => (
                          <li key={idx}>{err.message}</li>
                        ))}
                      </ul>
                    )}
                    <div className="mt-2 flex items-center justify-between border-t border-red-200/70 pt-2 text-xs text-red-600">
                      <span>RequestId: {submitError.requestId}</span>
                      <button
                        type="button"
                        onClick={() => setSubmitError(null)}
                        className="font-semibold underline hover:text-red-800"
                      >
                        Đóng & Thử lại
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Thông báo cập nhật thành công */}
            {successToast && (
              <div className="mb-5 flex items-center gap-2.5 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-800">
                <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600" />
                <span>{successToast}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label
                    htmlFor="fullName"
                    className="mb-1.5 block text-sm font-semibold text-slate-700"
                  >
                    {isEmployer ? 'Họ và tên người đại diện *' : 'Họ và tên *'}
                  </label>
                  <input
                    id="fullName"
                    name="fullName"
                    type="text"
                    required
                    disabled={isSubmitting}
                    value={formData.fullName}
                    onChange={handleChange}
                    className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
                  />
                </div>

                <div>
                  <label
                    htmlFor="phone"
                    className="mb-1.5 block text-sm font-semibold text-slate-700"
                  >
                    Số điện thoại liên hệ
                  </label>
                  <input
                    id="phone"
                    name="phone"
                    type="tel"
                    disabled={isSubmitting}
                    value={formData.phone}
                    onChange={handleChange}
                    placeholder="Ví dụ: 0914333444"
                    className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
                  />
                </div>
              </div>

              <div>
                <label
                  htmlFor="locationId"
                  className="mb-1.5 block text-sm font-semibold text-slate-700"
                >
                  Khu vực sinh sống / làm việc
                </label>
                <select
                  id="locationId"
                  name="locationId"
                  disabled={isSubmitting}
                  value={formData.locationId}
                  onChange={handleChange}
                  className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
                >
                  <option value="">-- Chọn Quận / Huyện --</option>
                  {locations.map((loc) => (
                    <option key={loc.id} value={loc.id}>
                      {loc.district}, {loc.city}
                    </option>
                  ))}
                </select>
              </div>

              {/* Khối thông tin dành riêng cho Sinh viên (FR-11) */}
              {isStudent && (
                <>
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div>
                      <label
                        htmlFor="school"
                        className="mb-1.5 block text-sm font-semibold text-slate-700"
                      >
                        Trường đại học / cao đẳng
                      </label>
                      <input
                        id="school"
                        name="school"
                        type="text"
                        disabled={isSubmitting}
                        value={formData.school}
                        onChange={handleChange}
                        placeholder="Ví dụ: Đại học Văn Lang"
                        className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="major"
                        className="mb-1.5 block text-sm font-semibold text-slate-700"
                      >
                        Chuyên ngành đào tạo
                      </label>
                      <input
                        id="major"
                        name="major"
                        type="text"
                        disabled={isSubmitting}
                        value={formData.major}
                        onChange={handleChange}
                        placeholder="Ví dụ: Công nghệ phần mềm"
                        className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
                      />
                    </div>
                  </div>

                  <div>
                    <label
                      htmlFor="cvContent"
                      className="mb-1.5 block text-sm font-semibold text-slate-700"
                    >
                      Nội dung CV & Kỹ năng làm việc (FR-11)
                    </label>
                    <textarea
                      id="cvContent"
                      name="cvContent"
                      rows={5}
                      disabled={isSubmitting}
                      value={formData.cvContent}
                      onChange={handleChange}
                      placeholder="Mô tả kỹ năng chuyên môn, kinh nghiệm làm thêm, ngoại ngữ và khung giờ rảnh trong tuần..."
                      className="w-full rounded-xl border border-slate-300 p-3.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
                    />
                  </div>
                </>
              )}

              {/* Khối thông tin dành riêng cho Nhà tuyển dụng (FR-16) */}
              {isEmployer && (
                <div>
                  <label
                    htmlFor="companyName"
                    className="mb-1.5 block text-sm font-semibold text-slate-700"
                  >
                    Tên Doanh nghiệp / Cửa hàng / Đơn vị tuyển dụng (FR-16)
                  </label>
                  <input
                    id="companyName"
                    name="companyName"
                    type="text"
                    disabled={isSubmitting}
                    value={formData.companyName}
                    onChange={handleChange}
                    placeholder="Ví dụ: Chuỗi Cafe Góc Phố"
                    className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
                  />
                </div>
              )}

              <div>
                <label htmlFor="bio" className="mb-1.5 block text-sm font-semibold text-slate-700">
                  {isEmployer ? 'Giới thiệu về đơn vị tuyển dụng' : 'Giới thiệu bản thân'}
                </label>
                <textarea
                  id="bio"
                  name="bio"
                  rows={3}
                  disabled={isSubmitting}
                  value={formData.bio}
                  onChange={handleChange}
                  placeholder={
                    isEmployer
                      ? 'Giới thiệu môi trường làm việc, lĩnh vực kinh doanh của đơn vị...'
                      : 'Giới thiệu ngắn gọn mục tiêu công việc và điểm mạnh của bản thân...'
                  }
                  className="w-full rounded-xl border border-slate-300 p-3.5 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
                />
              </div>

              <div className="flex justify-end pt-2">
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="inline-flex items-center gap-2 rounded-xl bg-red-600 px-6 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      <span>Đang lưu hồ sơ...</span>
                    </>
                  ) : (
                    <>
                      <Save className="h-4 w-4" />
                      <span>Lưu hồ sơ cá nhân</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </section>
        )}
      </div>

      {/* Khối hiển thị Đánh giá VISIBLE & Điểm trung bình (GET /api/v1/users/{userId}/reviews - FR-15, FR-24) */}
      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-100 pb-4">
          <div>
            <h2 className="text-lg font-bold text-slate-900">
              Đánh giá nhận được & Điểm uy tín (FR-15, FR-24)
            </h2>
            <p className="text-xs text-slate-500">
              Chỉ hiển thị các đánh giá hợp lệ (status = VISIBLE) sau khi hoàn thành công việc
            </p>
          </div>

          <div className="flex items-center gap-3 rounded-2xl bg-amber-50 px-4 py-2.5 border border-amber-200">
            <Star className="h-6 w-6 fill-amber-400 text-amber-500" />
            <div>
              <div className="text-base font-bold text-amber-900">
                {reviewsSummary.averageRating ?? 0} / 5.0 sao
              </div>
              <div className="text-xs text-amber-700">
                {reviewsSummary.totalReviews ?? 0} lượt đánh giá
              </div>
            </div>
          </div>
        </div>

        {(!reviewsSummary.reviews || reviewsSummary.reviews.length === 0) ? (
          <div className="py-8 text-center">
            <Inbox className="mx-auto h-8 w-8 text-slate-400" />
            <p className="mt-2 text-sm font-semibold text-slate-700">
              Người dùng này chưa nhận được đánh giá nào
            </p>
          </div>
        ) : (
          <div className="mt-4 space-y-3">
            {reviewsSummary.reviews.map((rev) => (
              <div
                key={rev.id}
                className="rounded-xl border border-slate-100 bg-slate-50/70 p-4"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-slate-900">
                      {rev.reviewerName}
                    </span>
                    <span className="rounded-full bg-slate-200 px-2 py-0.5 text-[10px] font-bold text-slate-700">
                      {rev.reviewerRole}
                    </span>
                    <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-bold text-amber-800">
                      <Star className="h-3 w-3 fill-amber-500 text-amber-600" />
                      <span>{rev.rating} / 5</span>
                    </span>
                  </div>
                  <span className="text-xs text-slate-400">
                    {rev.createdAt
                      ? new Date(rev.createdAt).toLocaleString('vi-VN')
                      : ''}
                  </span>
                </div>

                <p className="mt-1 text-xs font-medium text-slate-500">
                  Công việc: <strong className="text-slate-700">{rev.jobTitle}</strong>
                </p>

                {rev.comment && (
                  <p className="mt-2 whitespace-pre-line text-sm text-slate-800">
                    {rev.comment}
                  </p>
                )}
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
};

export default ProfilePage;
