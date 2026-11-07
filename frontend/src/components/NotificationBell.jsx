import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  Bell,
  CheckCheck,
  CheckCircle2,
  Inbox,
  Loader2,
  RefreshCw,
} from 'lucide-react';
import notificationService from '../services/notificationService';

const POLLING_INTERVAL_MS = 30000; // 30 giây theo yêu cầu FR-06

const TYPE_BADGE = {
  APPLICATION_NEW: {
    label: 'Ứng viên mới',
    className: 'bg-blue-100 text-blue-800',
  },
  APPLICATION_ACCEPTED: {
    label: 'Hồ sơ được duyệt',
    className: 'bg-emerald-100 text-emerald-800',
  },
  APPLICATION_REJECTED: {
    label: 'Hồ sơ bị từ chối',
    className: 'bg-red-100 text-red-800',
  },
  JOB_COMPLETED: {
    label: 'Hoàn thành công việc',
    className: 'bg-purple-100 text-purple-800',
  },
};

const NotificationBell = () => {
  const navigate = useNavigate();
  const dropdownRef = useRef(null);

  const [isOpen, setIsOpen] = useState(false);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isMarkingAll, setIsMarkingAll] = useState(false);
  const [errorState, setErrorState] = useState(null);

  const fetchUnreadCount = useCallback(async () => {
    try {
      const res = await notificationService.getUnreadCount();
      setUnreadCount(res.data?.unreadCount ?? 0);
    } catch {
      // Giữ nguyên badge hiện tại nếu polling ngầm gặp gián đoạn mạng tạm thời
    }
  }, []);

  const fetchNotifications = useCallback(
    async (onlyUnread = unreadOnly) => {
      setIsLoading(true);
      setErrorState(null);
      try {
        const [listRes, countRes] = await Promise.all([
          notificationService.getNotifications(onlyUnread),
          notificationService.getUnreadCount(),
        ]);
        setNotifications(listRes.data || []);
        setUnreadCount(countRes.data?.unreadCount ?? 0);
      } catch (error) {
        setErrorState({
          message: error.message || 'Không thể tải danh sách thông báo.',
          requestId: error.requestId || 'N/A',
        });
      } finally {
        setIsLoading(false);
      }
    },
    [unreadOnly]
  );

  // FR-06: Polling số thông báo chưa đọc mỗi 30 giây
  useEffect(() => {
    fetchUnreadCount();
    const intervalId = setInterval(() => {
      fetchUnreadCount();
    }, POLLING_INTERVAL_MS);

    return () => clearInterval(intervalId);
  }, [fetchUnreadCount]);

  useEffect(() => {
    if (isOpen) {
      fetchNotifications(unreadOnly);
    }
  }, [isOpen, unreadOnly, fetchNotifications]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleMarkAsRead = async (notif) => {
    try {
      if (!notif.isRead) {
        await notificationService.markAsRead(notif.id);
        setNotifications((prev) =>
          unreadOnly
            ? prev.filter((item) => item.id !== notif.id)
            : prev.map((item) => (item.id === notif.id ? { ...item, isRead: true } : item))
        );
        setUnreadCount((prev) => Math.max(0, prev - 1));
      }
      if (notif.jobId) {
        setIsOpen(false);
        if (notif.type === 'APPLICATION_NEW') {
          navigate(`/employer/jobs/${notif.jobId}/applications`);
        } else {
          navigate(`/jobs/${notif.jobId}`);
        }
      }
    } catch (error) {
      setErrorState({
        message: error.message || 'Không thể đánh dấu đã đọc thông báo.',
        requestId: error.requestId || 'N/A',
      });
    }
  };

  const handleMarkAllAsRead = async () => {
    setIsMarkingAll(true);
    setErrorState(null);
    try {
      await notificationService.markAllAsRead();
      setUnreadCount(0);
      setNotifications((prev) =>
        unreadOnly ? [] : prev.map((item) => ({ ...item, isRead: true }))
      );
    } catch (error) {
      setErrorState({
        message: error.message || 'Không thể đánh dấu tất cả thông báo đã đọc.',
        requestId: error.requestId || 'N/A',
      });
    } finally {
      setIsMarkingAll(false);
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        aria-label="Thông báo hệ thống"
        className="relative inline-flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-700 transition hover:bg-slate-100"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute -right-1.5 -top-1.5 flex h-5 min-w-[20px] items-center justify-center rounded-full bg-red-600 px-1 text-[11px] font-bold text-white shadow-sm">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 sm:w-96 rounded-2xl border border-slate-200 bg-white shadow-xl z-50 overflow-hidden">
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
            <div>
              <h3 className="text-sm font-bold text-slate-900">Thông báo (FR-06)</h3>
              <p className="text-[11px] text-slate-500">
                Tự động cập nhật mỗi 30 giây • Chưa đọc: {unreadCount}
              </p>
            </div>
            {unreadCount > 0 && (
              <button
                type="button"
                disabled={isMarkingAll}
                onClick={handleMarkAllAsRead}
                className="inline-flex items-center gap-1 rounded-lg bg-red-50 px-2.5 py-1 text-xs font-semibold text-red-700 transition hover:bg-red-100 disabled:opacity-50"
              >
                {isMarkingAll ? (
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                ) : (
                  <CheckCheck className="h-3.5 w-3.5" />
                )}
                <span>Đọc tất cả</span>
              </button>
            )}
          </div>

          <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-4 py-2 text-xs">
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                onClick={() => setUnreadOnly(false)}
                className={`rounded-lg px-2.5 py-1 font-semibold transition ${
                  !unreadOnly
                    ? 'bg-slate-900 text-white'
                    : 'text-slate-600 hover:bg-slate-200/70'
                }`}
              >
                Tất cả
              </button>
              <button
                type="button"
                onClick={() => setUnreadOnly(true)}
                className={`rounded-lg px-2.5 py-1 font-semibold transition ${
                  unreadOnly
                    ? 'bg-slate-900 text-white'
                    : 'text-slate-600 hover:bg-slate-200/70'
                }`}
              >
                Chưa đọc ({unreadCount})
              </button>
            </div>

            <button
              type="button"
              onClick={() => fetchNotifications(unreadOnly)}
              className="inline-flex items-center gap-1 text-slate-500 hover:text-slate-800"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span>Làm mới</span>
            </button>
          </div>

          <div className="max-h-96 overflow-y-auto divide-y divide-slate-100">
            {/* 1. Loading State */}
            {isLoading && (
              <div className="space-y-2 p-4">
                {[1, 2, 3].map((item) => (
                  <div
                    key={item}
                    className="h-16 animate-pulse rounded-xl bg-slate-200/70"
                  />
                ))}
              </div>
            )}

            {/* 3. Error State */}
            {!isLoading && errorState && (
              <div className="p-4 text-xs text-red-800 bg-red-50">
                <div className="flex items-start gap-2">
                  <AlertCircle className="h-4 w-4 shrink-0 text-red-600 mt-0.5" />
                  <div>
                    <p className="font-semibold">{errorState.message}</p>
                    <p className="mt-1 font-mono text-[11px] text-red-600">
                      RequestId: {errorState.requestId}
                    </p>
                    <button
                      type="button"
                      onClick={() => fetchNotifications(unreadOnly)}
                      className="mt-2 inline-flex items-center gap-1 rounded-lg bg-red-600 px-2.5 py-1 font-semibold text-white"
                    >
                      <RefreshCw className="h-3 w-3" />
                      <span>Thử lại</span>
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* 2. Empty State */}
            {!isLoading && !errorState && notifications.length === 0 && (
              <div className="p-8 text-center">
                <Inbox className="mx-auto h-8 w-8 text-slate-400" />
                <p className="mt-2 text-xs font-semibold text-slate-700">
                  {unreadOnly
                    ? 'Bạn không có thông báo chưa đọc nào'
                    : 'Chưa có thông báo nào trong hệ thống'}
                </p>
              </div>
            )}

            {/* Danh sách thông báo */}
            {!isLoading &&
              !errorState &&
              notifications.map((notif) => {
                const badge = TYPE_BADGE[notif.type] || {
                  label: notif.type,
                  className: 'bg-slate-100 text-slate-700',
                };
                return (
                  <button
                    key={notif.id}
                    type="button"
                    onClick={() => handleMarkAsRead(notif)}
                    className={`w-full text-left px-4 py-3 transition hover:bg-slate-50 ${
                      !notif.isRead ? 'bg-red-50/40' : 'bg-white'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span
                        className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${badge.className}`}
                      >
                        {badge.label}
                      </span>
                      <span className="text-[11px] text-slate-400">
                        {notif.createdAt
                          ? new Date(notif.createdAt).toLocaleString('vi-VN')
                          : ''}
                      </span>
                    </div>
                    <p
                      className={`mt-1.5 text-xs leading-relaxed ${
                        !notif.isRead
                          ? 'font-semibold text-slate-900'
                          : 'text-slate-600'
                      }`}
                    >
                      {notif.message}
                    </p>
                    <div className="mt-1.5 flex items-center justify-between text-[11px]">
                      {!notif.isRead ? (
                        <span className="font-semibold text-red-600">
                          • Chưa đọc (Bấm để đánh dấu đã đọc)
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-emerald-700">
                          <CheckCircle2 className="h-3 w-3" />
                          <span>Đã đọc</span>
                        </span>
                      )}
                    </div>
                  </button>
                );
              })}
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationBell;

