import { Link, Outlet, useLocation } from 'react-router-dom';

const NAV_ITEMS = [
  { label: 'Dashboard', path: '/admin', icon: '▦' },
  { label: 'Flights', path: '/admin/flights', icon: '✈' },
  { label: 'Reservations', path: '/admin/reservations', icon: '📋' },
  { label: 'Reports', path: '/admin/reports', icon: '📊' },
  { label: 'Complaints', path: '/admin/complaints', icon: '📨' },
  { label: 'Modifications', path: '/admin/modifications', icon: '✏' },
] as const;

export default function AdminLayout() {
  const { pathname } = useLocation();

  return (
    <div className="flex min-h-[calc(100vh-4rem)]">
      <aside className="w-60 border-r border-gray-200 bg-white">
        <div className="border-b border-gray-200 px-4 py-3">
          <p className="text-xs font-semibold uppercase tracking-wider text-gray-400">
            Administration
          </p>
        </div>
        <nav className="flex flex-col gap-1 p-3">
          {NAV_ITEMS.map(({ label, path, icon }) => {
            const isActive = pathname === path || (path !== '/admin' && pathname.startsWith(path));
            return (
              <Link
                key={path}
                to={path}
                className={`flex items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                }`}
              >
                <span className="text-base">{icon}</span>
                {label}
              </Link>
            );
          })}
        </nav>
      </aside>
      <div className="flex-1 overflow-auto bg-gray-50 p-6">
        <Outlet />
      </div>
    </div>
  );
}
