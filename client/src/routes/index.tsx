import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AuthenticatedLayout } from '../components/AuthenticatedLayout';
import { Login } from '../features/auth/Login';
import { Dashboard } from '../features/dashboard/Dashboard';
import { OrdersList } from '../features/orders/OrdersList';
import { NewOrder } from '../features/orders/NewOrder';
import { AvailabilityReview } from '../features/orders/AvailabilityReview';
import { SubmittingOrder } from '../features/orders/SubmittingOrder';
import { SubmissionOutcome } from '../features/orders/SubmissionOutcome';
import { OrderDetail } from '../features/orders/OrderDetail';
import { CompanyPage } from '../features/company/Company';
import { Profile } from '../features/profile/Profile';

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <Login />
  },
  {
    path: '/',
    element: <AuthenticatedLayout />,
    children: [
      {
        index: true,
        element: <Navigate to="/dashboard" replace />
      },
      {
        path: 'dashboard',
        element: <Dashboard />
      },
      {
        path: 'orders',
        element: <OrdersList />
      },
      {
        path: 'orders/new',
        element: <NewOrder />
      },
      {
        path: 'orders/review',
        element: <AvailabilityReview />
      },
      {
        path: 'orders/submitting',
        element: <SubmittingOrder />
      },
      {
        path: 'orders/outcome',
        element: <SubmissionOutcome />
      },
      {
        path: 'orders/:orderId',
        element: <OrderDetail />
      },
      {
        path: 'company',
        element: <CompanyPage />
      },
      {
        path: 'profile',
        element: <Profile />
      }
    ]
  },
  {
    path: '*',
    element: <Navigate to="/login" replace />
  }
]);
