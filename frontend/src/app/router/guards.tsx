import { Navigate, Outlet } from 'react-router';

import { useAuth } from '../../shared/auth';
import { LoadingState } from '../../shared/ui/LoadingState';

export function ProtectedRoute() {
  const { isAuthenticated, status } = useAuth();

  if (status === 'loading') {
    return <LoadingState label="Проверяем сессию..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}

export function PublicOnlyRoute() {
  const { isAuthenticated, status } = useAuth();

  if (status === 'loading') {
    return <LoadingState label="Проверяем сессию..." />;
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
