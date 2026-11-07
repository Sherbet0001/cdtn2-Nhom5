import React, { useState } from 'react';
import { AlertCircle, Loader2, Send, Star, X } from 'lucide-react';
import reviewService from '../services/reviewService';

const ReviewModal = ({
  isOpen,
  onClose,
  jobId,
  jobTitle,
  revieweeId,
  revieweeName,
  revieweeRoleLabel,
  onSuccess,
}) => {
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);

  if (!isOpen) return null;

  const handleSubmit = async (event) => {
    event.preventDefault();
    setIsSubmitting(true);
    setSubmitError(null);

    try {
      const response = await reviewService.createReview(jobId, {
        revieweeId,
        rating: Number(rating),
        comment: comment.trim(),
      });
      if (onSuccess) {
        onSuccess(response.data);
      }
      onClose();
    } catch (error) {
      setSubmitError({
        message: error.message || 'Không thể gửi đánh giá.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs">
      <div className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-6 shadow-xl">
        <div className="flex items-start justify-between gap-3 border-b border-slate-100 pb-4">
          <div>
            <h2 className="text-lg font-bold text-slate-900">
              Đánh giá {revieweeRoleLabel || 'đối tác'} (FR-15, FR-24)
            </h2>
            <p className="mt-1 text-xs text-slate-600">
              Công việc: <strong className="text-slate-900">{jobTitle}</strong> • Người nhận:{' '}
              <strong className="text-red-600">{revieweeName}</strong>
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {submitError && (
          <div
            role="alert"
            className="mt-4 rounded-xl border border-red-200 bg-red-50 p-3.5 text-xs text-red-800"
          >
            <div className="flex items-start gap-2">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-red-600" />
              <div>
                <p className="font-semibold">{submitError.message}</p>
                <p className="mt-1 font-mono text-[11px] text-red-600">
                  RequestId: {submitError.requestId}
                </p>
              </div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">
              Mức độ hài lòng (1 – 5 sao) *
            </label>
            <div className="flex items-center gap-2">
              {[1, 2, 3, 4, 5].map((starValue) => (
                <button
                  key={starValue}
                  type="button"
                  disabled={isSubmitting}
                  onClick={() => setRating(starValue)}
                  className={`inline-flex items-center gap-1 rounded-xl border px-3.5 py-2 text-sm font-bold transition ${
                    rating >= starValue
                      ? 'border-amber-400 bg-amber-50 text-amber-700'
                      : 'border-slate-200 bg-white text-slate-400 hover:bg-slate-50'
                  }`}
                >
                  <Star
                    className={`h-4 w-4 ${
                      rating >= starValue ? 'fill-amber-400 text-amber-500' : ''
                    }`}
                  />
                  <span>{starValue}</span>
                </button>
              ))}
            </div>
          </div>

          <div>
            <label
              htmlFor="review-comment"
              className="mb-1.5 block text-xs font-bold text-slate-700"
            >
              Nhận xét chi tiết (Tối đa 1000 ký tự)
            </label>
            <textarea
              id="review-comment"
              rows={4}
              maxLength={1000}
              disabled={isSubmitting}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="Chia sẻ trải nghiệm làm việc thực tế, thái độ hợp tác và mức độ hoàn thành công việc..."
              className="w-full rounded-xl border border-slate-300 p-3 text-sm focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20 disabled:bg-slate-100"
            />
          </div>

          <div className="flex items-center justify-end gap-2.5 border-t border-slate-100 pt-4">
            <button
              type="button"
              disabled={isSubmitting}
              onClick={onClose}
              className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="inline-flex items-center gap-1.5 rounded-xl bg-red-600 px-5 py-2 text-xs font-bold text-white shadow-sm transition hover:bg-red-700 disabled:opacity-60"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  <span>Đang gửi...</span>
                </>
              ) : (
                <>
                  <Send className="h-3.5 w-3.5" />
                  <span>Gửi đánh giá ({rating} sao)</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ReviewModal;

