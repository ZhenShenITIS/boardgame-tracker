import {
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Pagination,
  SimpleGrid,
  Stack,
  Text,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useQueryClient } from '@tanstack/react-query';
import { AlertCircle, Plus } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router';

import { useGetCollectionItemsShelfOfShame } from '../shared/api/generated/collection-items/collection-items';
import type { GetCollectionItemsShelfOfShame200DataItem } from '../shared/api/generated/model';
import { usePostCollectionItemsCollectionItemIdPlaySessionsQuick } from '../shared/api/generated/play-sessions/play-sessions';
import { BoardGameImage } from '../shared/ui/BoardGameImage';
import { formatCollectionStatus } from '../shared/ui/collectionStatus';
import { EmptyState } from '../shared/ui/EmptyState';
import { ErrorState } from '../shared/ui/ErrorState';
import { LoadingState } from '../shared/ui/LoadingState';
import { PageHeader } from '../shared/ui/PageHeader';

const PAGE_LIMIT = 12;

function parseApiError(error: unknown, fallback: string) {
  if (
    typeof error === 'object' &&
    error &&
    'response' in error &&
    typeof error.response === 'object' &&
    error.response &&
    'data' in error.response
  ) {
    const data = error.response.data as { error?: { message?: string } };

    if (data?.error?.message) {
      return data.error.message;
    }
  }

  return fallback;
}

function formatCost(value?: number | null) {
  if (value === null || value === undefined) {
    return 'Неизвестно';
  }

  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 0,
  }).format(value);
}

function formatDate(value?: string | null) {
  if (!value) {
    return 'Неизвестно';
  }

  return value;
}

export function ShelfOfShamePage() {
  const [page, setPage] = useState(1);
  const [pendingItemId, setPendingItemId] = useState<number | null>(null);
  const queryClient = useQueryClient();

  const shelfQuery = useGetCollectionItemsShelfOfShame({
    page,
    limit: PAGE_LIMIT,
  });
  const quickPlayMutation = usePostCollectionItemsCollectionItemIdPlaySessionsQuick();

  const items = shelfQuery.data?.data ?? [];
  const totalPages = shelfQuery.data?.pagination.totalPages ?? 0;

  const visibleTotalKnownCost = items.reduce((sum, item) => sum + (item.sumInRubles ?? 0), 0);
  const visibleUnknownCostCount = items.filter((item) => item.sumInRubles === null || item.sumInRubles === undefined).length;

  const isInitialLoading = shelfQuery.isLoading && items.length === 0;
  const isInitialError = shelfQuery.isError && items.length === 0;

  async function invalidateShelfQueries() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['/collection-items/shelf-of-shame'] }),
      queryClient.invalidateQueries({ queryKey: ['/collection-items'] }),
      queryClient.invalidateQueries({ queryKey: ['/me/stats'] }),
    ]);
  }

  async function handleQuickPlay(item: GetCollectionItemsShelfOfShame200DataItem) {
    setPendingItemId(item.id);

    try {
      await quickPlayMutation.mutateAsync({
        collectionItemId: item.id,
        data: {},
      });

      notifications.show({
        title: 'Партия добавлена',
        message: `"${item.boardGame.displayName}" больше не считается несыгранной.`,
        color: 'teal',
      });

      await invalidateShelfQueries();
    } catch (error) {
      notifications.show({
        title: 'Не удалось добавить партию',
        message: parseApiError(error, 'Запрос быстрого добавления партии завершился ошибкой.'),
        color: 'red',
      });
    } finally {
      setPendingItemId(null);
    }
  }

  if (isInitialLoading) {
    return <LoadingState label="Загружаем полку позора..." />;
  }

  if (isInitialError) {
    return (
      <ErrorState
        title="Не удалось загрузить полку позора"
        message="Запрос полки позора завершился ошибкой. Попробуйте ещё раз."
        onRetry={() => {
          void shelfQuery.refetch();
        }}
      />
    );
  }

  return (
    <Stack gap="lg">
      <PageHeader
        title="Полка позора"
        description="Игры из коллекции без сыгранных партий. Добавьте партию, чтобы убрать игру из списка."
      />

      <Card withBorder radius="md" p="lg">
        <Group justify="space-between" align="flex-start">
          <Stack gap={2}>
            <Text fw={600}>Стоимость видимых игр</Text>
            <Text size="xl" fw={700}>
              {formatCost(visibleTotalKnownCost)}
            </Text>
            {visibleUnknownCostCount > 0 ? (
              <Text size="sm" c="dimmed">
                Без стоимости: {visibleUnknownCostCount}. Эти игры не учтены в сумме.
              </Text>
            ) : null}
          </Stack>
          <Button component={Link} to="/" variant="light" color="ocean">
            Открыть статистику
          </Button>
        </Group>
      </Card>

      {items.length === 0 ? (
        <EmptyState
          title="Полка позора пуста"
          description="Добавьте новые игры или проверьте полную коллекцию."
          action={
            <Group>
              <Button component={Link} to="/boardgames" variant="light" color="ember">
                Открыть игры
              </Button>
              <Button component={Link} to="/collection" variant="default">
                Открыть коллекцию
              </Button>
            </Group>
          }
        />
      ) : (
        <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
          {items.map((item) => (
            <Card withBorder radius="md" p="lg" key={item.id}>
              <Stack gap="sm" h="100%">
                <BoardGameImage src={item.boardGame.previewUrl ?? item.boardGame.imageUrl} alt={item.boardGame.displayName} />

                <Group justify="space-between" align="flex-start">
                  <Stack gap={2}>
                    <Text fw={600} lh={1.2}>
                      {item.boardGame.displayName}
                    </Text>
                    <Text size="sm" c="dimmed">
                      {item.boardGame.yearPublished ?? 'Год неизвестен'}
                    </Text>
                  </Stack>
                  <Badge variant="light" color="ocean">
                    {formatCollectionStatus(item.status)}
                  </Badge>
                </Group>

                <Text size="sm">Партий: {item.playCount}</Text>
                <Text size="sm">Куплена: {formatDate(item.datePurchased)}</Text>
                <Text size="sm">Стоимость: {formatCost(item.sumInRubles)}</Text>
                <Text size="sm" c="dimmed" lineClamp={2}>
                  {item.comment?.trim() ? item.comment : 'Без комментария'}
                </Text>

                <Group mt="auto">
                  <Button
                    leftSection={<Plus size={14} />}
                    onClick={() => {
                      void handleQuickPlay(item);
                    }}
                    loading={quickPlayMutation.isPending && pendingItemId === item.id}
                    disabled={quickPlayMutation.isPending}
                  >
                    +1 партия
                  </Button>
                </Group>
              </Stack>
            </Card>
          ))}
        </SimpleGrid>
      )}

      {shelfQuery.isError && items.length > 0 ? (
        <Alert color="red" variant="light" icon={<AlertCircle size={16} />}>
          Не удалось обновить список полки позора.
        </Alert>
      ) : null}

      {totalPages > 1 ? (
        <Group justify="center">
          <Pagination total={totalPages} value={page} onChange={setPage} />
        </Group>
      ) : null}
    </Stack>
  );
}
