import { createBrowserRouter } from 'react-router-dom';
import AppLayout from '@/components/layout/AppLayout';
import AdminLayout from '@/components/layout/AdminLayout';
import ProtectedRoute from '@/components/guards/ProtectedRoute';
import AdminRoute from '@/components/guards/AdminRoute';
import GuestRoute from '@/components/guards/GuestRoute';

import Landing from '@/pages/Landing';
import NotFoundPage from '@/pages/NotFoundPage';
import LoginPage from '@/pages/auth/LoginPage';
import RegisterPage from '@/pages/auth/RegisterPage';
import FlightSearchPage from '@/pages/customer/FlightSearchPage';
import FlightDetailPage from '@/pages/customer/FlightDetailPage';
import BookingsPage from '@/pages/customer/BookingsPage';
import BookingDetailPage from '@/pages/customer/BookingDetailPage';
import ComplaintsPage from '@/pages/customer/ComplaintsPage';
import NotificationsPage from '@/pages/customer/NotificationsPage';
import LoyaltyPage from '@/pages/customer/LoyaltyPage';
import RouteMapPage from '@/pages/customer/RouteMapPage';
import PassengersPage from '@/pages/customer/PassengersPage';
import DashboardPage from '@/pages/admin/DashboardPage';
import FlightManagementPage from '@/pages/admin/FlightManagementPage';
import ReservationsPage from '@/pages/admin/ReservationsPage';
import ReportsPage from '@/pages/admin/ReportsPage';
import ComplaintManagementPage from '@/pages/admin/ComplaintManagementPage';
import ModificationRequestsPage from '@/pages/admin/ModificationRequestsPage';

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <Landing /> },
      { path: '/flights', element: <FlightSearchPage /> },
      { path: '/flights/:flightId', element: <FlightDetailPage /> },
      { path: '/map', element: <RouteMapPage /> },

      {
        element: <GuestRoute />,
        children: [
          { path: '/login', element: <LoginPage /> },
          { path: '/register', element: <RegisterPage /> },
        ],
      },

      {
        element: <ProtectedRoute />,
        children: [
          { path: '/bookings', element: <BookingsPage /> },
          { path: '/bookings/:bookingId', element: <BookingDetailPage /> },
          { path: '/passengers', element: <PassengersPage /> },
          { path: '/complaints', element: <ComplaintsPage /> },
          { path: '/notifications', element: <NotificationsPage /> },
          { path: '/loyalty', element: <LoyaltyPage /> },
        ],
      },

      {
        element: <AdminRoute />,
        path: '/admin',
        children: [
          {
            element: <AdminLayout />,
            children: [
              { index: true, element: <DashboardPage /> },
              { path: 'flights', element: <FlightManagementPage /> },
              { path: 'reservations', element: <ReservationsPage /> },
              { path: 'reports', element: <ReportsPage /> },
              { path: 'complaints', element: <ComplaintManagementPage /> },
              { path: 'modifications', element: <ModificationRequestsPage /> },
            ],
          },
        ],
      },

      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);
