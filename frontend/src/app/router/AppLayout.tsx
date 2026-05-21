import { AppShell, Burger, Button, Group, NavLink, ScrollArea, Stack, Text, Title, UnstyledButton, rem } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import {
  BookOpen,
  LibraryBig,
  LogIn,
  LogOut,
  Sparkles,
  Swords,
  UserPlus,
  Warehouse,
} from 'lucide-react';
import { type ReactNode } from 'react';
import { Link, Outlet, useLocation } from 'react-router';

import { useAuth } from '../../shared/auth';

type NavigationItem = {
  to: string;
  label: string;
  icon: ReactNode;
};

const privateNavigationItems: NavigationItem[] = [
  { to: '/', label: 'Дашборд', icon: <LibraryBig size={16} /> },
  { to: '/collection', label: 'Коллекция', icon: <Warehouse size={16} /> },
  { to: '/boardgames', label: 'Игры', icon: <BookOpen size={16} /> },
  { to: '/shelf-of-shame', label: 'Полка позора', icon: <Swords size={16} /> },
  { to: '/recommendations', label: 'Рекомендации', icon: <Sparkles size={16} /> },
];

const publicNavigationItems: NavigationItem[] = [
  { to: '/login', label: 'Вход', icon: <LogIn size={16} /> },
  { to: '/register', label: 'Регистрация', icon: <UserPlus size={16} /> },
];

function Navigation({ closeMobile, isAuthenticated }: { closeMobile: () => void; isAuthenticated: boolean }) {
  const location = useLocation();
  const items = isAuthenticated ? privateNavigationItems : publicNavigationItems;

  return (
    <Stack gap={4}>
      {items.map((item) => (
        <NavLink
          key={item.to}
          component={Link}
          to={item.to}
          label={item.label}
          leftSection={item.icon}
          active={location.pathname === item.to}
          onClick={closeMobile}
        />
      ))}
    </Stack>
  );
}

export function AppLayout() {
  const [opened, { toggle, close }] = useDisclosure(false);
  const { isAuthenticated, logout, status, user } = useAuth();

  return (
    <AppShell
      header={{ height: 64 }}
      navbar={{
        width: 260,
        breakpoint: 'sm',
        collapsed: { mobile: !opened },
      }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" />
            <UnstyledButton component={Link} to="/">
              <Group gap="xs" wrap="nowrap">
                <LibraryBig size={20} />
                <Title order={4}>Boardgame Tracker</Title>
              </Group>
            </UnstyledButton>
          </Group>
          {isAuthenticated ? (
            <Group gap="sm" wrap="nowrap">
              {user ? (
                <Stack gap={0} align="flex-end" visibleFrom="sm">
                  <Text size="sm" fw={500} lh={1.1}>
                    {user.name}
                  </Text>
                  <Text size="xs" c="dimmed" lh={1.1}>
                    {user.email}
                  </Text>
                </Stack>
              ) : null}
              <Button
                size="xs"
                variant="light"
                color="ember"
                leftSection={<LogOut size={14} />}
                onClick={() => {
                  void logout();
                }}
              >
                Выйти
              </Button>
            </Group>
          ) : (
            <Text c="dimmed" size="sm" visibleFrom="sm">
              {status === 'loading' ? 'Проверка сессии' : 'Авторизация'}
            </Text>
          )}
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="xs">
        <AppShell.Section component={ScrollArea} grow>
          <Navigation closeMobile={close} isAuthenticated={isAuthenticated} />
        </AppShell.Section>
      </AppShell.Navbar>

      <AppShell.Main>
        <Stack gap="lg" maw={rem(1120)} mx="auto">
          <Outlet />
        </Stack>
      </AppShell.Main>
    </AppShell>
  );
}
