import { useEffect, useState } from 'react';
import { Bell, BellRing, CalendarCheck2, Check, XCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import notificationService from '../services/notificationService.js';

function notificationIcon(status) {
  if (status === 'INTERVIEW') return CalendarCheck2;
  if (status === 'REJECTED') return XCircle;
  return Bell;
}

export default function NotificationsPage() {
  const [data, setData] = useState({ unreadCount: 0, items: [] });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    notificationService.getMine().then(setData).catch((requestError) => setError(requestError.message))
      .finally(() => setLoading(false));
  }, []);

  async function markRead(item) {
    setError('');
    try {
      const updated = await notificationService.markRead(item.id);
      setData((current) => ({
        unreadCount: Math.max(0, current.unreadCount - (item.read ? 0 : 1)),
        items: current.items.map((entry) => entry.id === item.id ? updated : entry),
      }));
      window.dispatchEvent(new Event('partjob:notifications-updated'));
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-6 flex items-end justify-between gap-3">
        <div><p className="text-sm font-semibold text-indigo-700">Tài khoản sinh viên</p><h1 className="mt-1 text-2xl font-extrabold text-slate-900">Thông báo</h1><p className="mt-2 text-sm text-slate-600">Cập nhật từ nhà tuyển dụng về hồ sơ ứng tuyển của bạn.</p></div>
        {data.unreadCount > 0 && <span className="rounded-full bg-indigo-100 px-3 py-1 text-xs font-bold text-indigo-800">{data.unreadCount} chưa đọc</span>}
      </div>
      {error && <p className="notice-error mb-5" role="alert">{error}</p>}
      {loading ? <p className="rounded-2xl bg-white p-10 text-center text-sm text-slate-500">Đang tải thông báo...</p>
        : data.items.length ? (
          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            {data.items.map((item) => {
              const Icon = notificationIcon(item.status);
              return (
                <article className={`flex gap-4 border-b border-slate-100 p-5 last:border-0 ${item.read ? '' : 'bg-indigo-50/50'}`} key={item.id}>
                  <div className={`flex size-10 shrink-0 items-center justify-center rounded-full ${item.status === 'INTERVIEW' ? 'bg-emerald-100 text-emerald-700' : item.status === 'REJECTED' ? 'bg-slate-100 text-slate-600' : 'bg-indigo-100 text-indigo-700'}`}><Icon size={19} /></div>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center justify-between gap-2"><h2 className="font-bold text-slate-900">{item.title}</h2>{!item.read && <span className="size-2 rounded-full bg-indigo-600" aria-label="Chưa đọc" />}</div>
                    <p className="mt-1 text-sm leading-6 text-slate-600">{item.message}</p>
                    <p className="mt-2 text-xs text-slate-400">{new Date(item.createdAt).toLocaleString('vi-VN')}</p>
                    {item.status === 'INTERVIEW' && <div className="mt-3 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-900"><div className="flex items-center gap-2 font-semibold"><CalendarCheck2 size={16} />Thông tin phỏng vấn</div>{item.interviewAt && <p className="mt-1">{new Date(item.interviewAt).toLocaleString('vi-VN')}</p>}{item.interviewLocation && <p className="mt-1">{item.interviewLocation}</p>}</div>}
                    <div className="mt-3 flex flex-wrap gap-4">
                      <Link className="text-sm font-semibold text-indigo-700 hover:underline" to={`/jobs/${item.jobId}`}>Xem công việc: {item.jobTitle}</Link>
                      {!item.read && <button className="inline-flex items-center gap-1 text-sm font-semibold text-slate-500 hover:text-indigo-700" onClick={() => markRead(item)} type="button"><Check size={15} /> Đánh dấu đã đọc</button>}
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        ) : (
          <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-14 text-center">
            <BellRing aria-hidden="true" className="mx-auto text-slate-300" size={36} />
            <h2 className="mt-4 font-semibold text-slate-800">Chưa có thông báo</h2>
            <p className="mt-1 text-sm text-slate-500">Chúng tôi sẽ cập nhật khi nhà tuyển dụng phản hồi hồ sơ của bạn.</p>
          </div>
        )}
    </div>
  );
}
