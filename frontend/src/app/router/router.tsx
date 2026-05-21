import { createBrowserRouter } from 'react-router';

import { BoardgamesPage } from '../../pages/BoardgamesPage';
import { CollectionCustomGamePage } from '../../pages/CollectionCustomGamePage';
import { CollectionItemDetailsPage } from '../../pages/CollectionItemDetailsPage';
import { CollectionPage } from '../../pages/CollectionPage';
import { DashboardPage } from '../../pages/DashboardPage';
import { LoginPage } from '../../pages/LoginPage';
import { RecommendationsPage } from '../../pages/RecommendationsPage';
import { RegisterPage } from '../../pages/RegisterPage';
import { ShelfOfShamePage } from '../../pages/ShelfOfShamePage';
import { AppLayout } from './AppLayout';
import { ProtectedRoute, PublicOnlyRoute } from './guards';

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      {
        element: <ProtectedRoute />,
        children: [
          {
            path: '/',
            element: <DashboardPage />,
          },
          {
            path: '/collection',
            element: <CollectionPage />,
          },
          {
            path: '/collection/custom-game',
            element: <CollectionCustomGamePage />,
          },
          {
            path: '/collection/:id',
            element: <CollectionItemDetailsPage />,
          },
          {
            path: '/boardgames',
            element: <BoardgamesPage />,
          },
          {
            path: '/shelf-of-shame',
            element: <ShelfOfShamePage />,
          },
          {
            path: '/recommendations',
            element: <RecommendationsPage />,
          },
        ],
      },
      {
        element: <PublicOnlyRoute />,
        children: [
          {
            path: '/login',
            element: <LoginPage />,
          },
          {
            path: '/register',
            element: <RegisterPage />,
          },
        ],
      },
    ],
  },
]);
