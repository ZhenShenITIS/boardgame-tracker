import { ActionIcon, Card, Grid, Group, Progress, SimpleGrid, Stack, Text, ThemeIcon } from '@mantine/core';
import { ArrowRight, ChartNoAxesColumnIncreasing, CircleDollarSign, Clock3, Search, Sparkles, Swords, Warehouse } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router';

import { useGetMeStats } from '../shared/api/generated/me/me';
import { EmptyState } from '../shared/ui/EmptyState';
import { ErrorState } from '../shared/ui/ErrorState';
import { LoadingState } from '../shared/ui/LoadingState';
import { PageHeader } from '../shared/ui/PageHeader';

function formatRubles(value: number) {
  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 0,
  }).format(value);
}

function StatCard({
  title,
  value,
  hint,
  icon,
}: {
  title: string;
  value: string;
  hint: string;
  icon: ReactNode;
}) {
  return (
    <Card withBorder radius="md" p="lg">
      <Stack gap="xs">
        <Group justify="space-between" align="flex-start">
          <Text size="sm" c="dimmed">
            {title}
          </Text>
          <ThemeIcon variant="light" color="ocean" size={34} radius="md">
            {icon}
          </ThemeIcon>
        </Group>
        <Text fw={700} size="xl" lh={1}>
          {value}
        </Text>
        <Text size="xs" c="dimmed">
          {hint}
        </Text>
      </Stack>
    </Card>
  );
}

function QuickAction({ to, label, icon }: { to: string; label: string; icon: ReactNode }) {
  return (
    <Card withBorder radius="md" p="md" component={Link} to={to}>
      <Group justify="space-between" wrap="nowrap">
        <Group gap="sm" wrap="nowrap">
          <ThemeIcon variant="light" color="ember" radius="md">
            {icon}
          </ThemeIcon>
          <Text fw={500}>{label}</Text>
        </Group>
        <ActionIcon variant="subtle" color="gray" aria-label={label}>
          <ArrowRight size={16} />
        </ActionIcon>
      </Group>
    </Card>
  );
}

export function DashboardPage() {
  const statsQuery = useGetMeStats();

  if (statsQuery.isLoading) {
    return <LoadingState label="Загружаем статистику дашборда..." />;
  }

  if (statsQuery.isError) {
    return (
      <ErrorState
        title="Не удалось загрузить дашборд"
        message="Статистика временно недоступна. Попробуйте ещё раз."
        onRetry={() => {
          void statsQuery.refetch();
        }}
      />
    );
  }

  const stats = statsQuery.data;

  if (!stats || stats.totalItems === 0) {
    return (
      <Stack gap="md">
        <PageHeader
          title="Дашборд"
          description="Следите за прогрессом коллекции, стоимостью полки позора и быстрыми действиями."
        />
        <EmptyState
          title="Ваша коллекция пуста"
          description="Добавьте первую игру, чтобы увидеть персональную статистику."
          action={
            <ActionIcon component={Link} to="/boardgames" variant="light" color="ember" size="lg" aria-label="Найти игры">
              <Search size={18} />
            </ActionIcon>
          }
        />
      </Stack>
    );
  }

  const playedPercent = Math.max(0, Math.min(100, stats.playedPercent));

  return (
    <Stack gap="lg">
      <PageHeader
        title="Дашборд"
        description="Следите за прогрессом коллекции, стоимостью полки позора и быстрыми действиями."
      />

      <SimpleGrid cols={{ base: 1, sm: 2, lg: 5 }} spacing="md">
        <StatCard
          title="Всего игр"
          value={String(stats.totalItems)}
          hint="Все записи коллекции"
          icon={<Warehouse size={18} />}
        />
        <StatCard
          title="Сыгранные игры"
          value={String(stats.playedItems)}
          hint="Есть хотя бы одна партия"
          icon={<ChartNoAxesColumnIncreasing size={18} />}
        />
        <StatCard
          title="Несыгранные игры"
          value={String(stats.unplayedItems)}
          hint="Текущая нагрузка полки"
          icon={<Clock3 size={18} />}
        />
        <StatCard
          title="Стоимость полки"
          value={formatRubles(stats.shelfOfShameCost)}
          hint="Сумма несыгранных игр"
          icon={<CircleDollarSign size={18} />}
        />
        <StatCard
          title="Процент сыгранных"
          value={`${playedPercent.toFixed(0)}%`}
          hint="Доля сыгранных записей"
          icon={<Sparkles size={18} />}
        />
      </SimpleGrid>

      <Grid>
        <Grid.Col span={{ base: 12, md: 7 }}>
          <Card withBorder radius="md" p="lg">
            <Stack gap="sm">
              <Group justify="space-between">
                <Text fw={600}>Прогресс коллекции</Text>
                <Text c="dimmed" size="sm">
                  {playedPercent.toFixed(1)}%
                </Text>
              </Group>
              <Progress value={playedPercent} color="ember" size="lg" radius="xl" />
              <Text size="sm" c="dimmed">
                {stats.playedItems} из {stats.totalItems} игр имеют хотя бы одну записанную партию.
              </Text>
            </Stack>
          </Card>
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 5 }}>
          <Card withBorder radius="md" p="lg">
            <Stack gap="sm">
              <Text fw={600}>Быстрые действия</Text>
              <QuickAction to="/collection" label="Открыть коллекцию" icon={<Warehouse size={16} />} />
              <QuickAction to="/boardgames" label="Найти игру" icon={<Search size={16} />} />
              <QuickAction to="/shelf-of-shame" label="Открыть полку позора" icon={<Swords size={16} />} />
              <QuickAction to="/recommendations" label="Получить рекомендацию" icon={<Sparkles size={16} />} />
            </Stack>
          </Card>
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
