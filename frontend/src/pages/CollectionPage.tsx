import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Modal,
  Pagination,
  Select,
  SimpleGrid,
  Stack,
  Switch,
  Text,
  TextInput,
  Textarea,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useQueryClient } from '@tanstack/react-query';
import { AlertCircle, ArrowRight, CalendarDays, Pencil, Plus, Trash2 } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link } from 'react-router';

import { useDeleteCollectionItemsId, useGetCollectionItems, usePutCollectionItemsId } from '../shared/api/generated/collection-items/collection-items';
import type {
  GetCollectionItems200DataItem,
  GetCollectionItemsStatus,
  PutCollectionItemsIdBodyStatus,
} from '../shared/api/generated/model';
import { usePostCollectionItemsCollectionItemIdPlaySessionsQuick } from '../shared/api/generated/play-sessions/play-sessions';
import { COLLECTION_SUM_IN_RUBLES_MAX } from '../shared/api/numericLimits';
import { BoardGameImage } from '../shared/ui/BoardGameImage';
import { formatCollectionStatus } from '../shared/ui/collectionStatus';
import { EmptyState } from '../shared/ui/EmptyState';
import { ErrorState } from '../shared/ui/ErrorState';
import { LoadingState } from '../shared/ui/LoadingState';
import { PageHeader } from '../shared/ui/PageHeader';

type EditFormState = {
  status: PutCollectionItemsIdBodyStatus;
  datePurchased: string;
  sumInRubles: string;
  comment: string;
};

type EditFieldErrors = {
  sumInRubles?: string;
  comment?: string;
};

const PAGE_LIMIT = 12;

const statusFilterOptions: { label: string; value: string }[] = [
  { label: 'Все статусы', value: 'ALL' },
  { label: formatCollectionStatus('IN_COLLECTION'), value: 'IN_COLLECTION' },
  { label: formatCollectionStatus('WISH_LIST'), value: 'WISH_LIST' },
  { label: formatCollectionStatus('SOLD'), value: 'SOLD' },
];

const statusOptions: { label: string; value: PutCollectionItemsIdBodyStatus }[] = [
  { label: formatCollectionStatus('IN_COLLECTION'), value: 'IN_COLLECTION' },
  { label: formatCollectionStatus('WISH_LIST'), value: 'WISH_LIST' },
  { label: formatCollectionStatus('SOLD'), value: 'SOLD' },
];

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

function formatDate(value?: string | null) {
  if (!value) {
    return 'Неизвестно';
  }

  return value;
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

export function CollectionPage() {
  const queryClient = useQueryClient();

  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [shelfOfShame, setShelfOfShame] = useState(false);
  const [page, setPage] = useState(1);

  const [editingItem, setEditingItem] = useState<GetCollectionItems200DataItem | null>(null);
  const [editForm, setEditForm] = useState<EditFormState>({
    status: 'IN_COLLECTION',
    datePurchased: '',
    sumInRubles: '',
    comment: '',
  });
  const [editFieldErrors, setEditFieldErrors] = useState<EditFieldErrors>({});
  const [editError, setEditError] = useState<string | null>(null);

  const [deletingItem, setDeletingItem] = useState<GetCollectionItems200DataItem | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const collectionQuery = useGetCollectionItems({
    status: statusFilter === 'ALL' ? undefined : (statusFilter as GetCollectionItemsStatus),
    shelfOfShame: shelfOfShame || undefined,
    page,
    limit: PAGE_LIMIT,
  });

  const quickPlayMutation = usePostCollectionItemsCollectionItemIdPlaySessionsQuick();
  const updateMutation = usePutCollectionItemsId();
  const deleteMutation = useDeleteCollectionItemsId();

  const totalPages = collectionQuery.data?.pagination.totalPages ?? 0;
  const items = collectionQuery.data?.data ?? [];

  const isInitialLoading = collectionQuery.isLoading && items.length === 0;
  const isInitialError = collectionQuery.isError && items.length === 0;

  function resetToFirstPage() {
    setPage(1);
  }

  function openEditModal(item: GetCollectionItems200DataItem) {
    setEditingItem(item);
    setEditForm({
      status: item.status,
      datePurchased: item.datePurchased ?? '',
      sumInRubles: item.sumInRubles !== null && item.sumInRubles !== undefined ? String(item.sumInRubles) : '',
      comment: item.comment ?? '',
    });
    setEditFieldErrors({});
    setEditError(null);
  }

  async function invalidateCollectionQueries() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['/collection-items'] }),
      queryClient.invalidateQueries({ queryKey: ['/me/stats'] }),
      queryClient.invalidateQueries({ queryKey: ['/collection-items/shelf-of-shame'] }),
    ]);
  }

  async function handleQuickPlay(item: GetCollectionItems200DataItem) {
    try {
      await quickPlayMutation.mutateAsync({
        collectionItemId: item.id,
        data: {},
      });

      notifications.show({
        title: 'Партия добавлена',
        message: `+1 партия для "${item.boardGame.displayName}"`,
        color: 'teal',
      });

      await invalidateCollectionQueries();
    } catch (error) {
      notifications.show({
        title: 'Не удалось добавить партию',
        message: parseApiError(error, 'Запрос быстрого добавления партии завершился ошибкой.'),
        color: 'red',
      });
    }
  }

  async function handleEditSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!editingItem) {
      return;
    }

    const errors: EditFieldErrors = {};
    const parsedCost = editForm.sumInRubles.trim() ? Number(editForm.sumInRubles) : null;

    if (parsedCost !== null && (!Number.isFinite(parsedCost) || parsedCost < 0 || parsedCost > COLLECTION_SUM_IN_RUBLES_MAX)) {
      errors.sumInRubles = 'Стоимость должна быть от 0 до 99 999 999,99';
    }

    if (editForm.comment.length > 2000) {
      errors.comment = 'Комментарий должен быть не длиннее 2000 символов';
    }

    setEditFieldErrors(errors);
    setEditError(null);

    if (Object.keys(errors).length > 0) {
      return;
    }

    try {
      await updateMutation.mutateAsync({
        id: editingItem.id,
        data: {
          boardGameId: editingItem.boardGame.id,
          status: editForm.status,
          datePurchased: editForm.datePurchased.trim() ? editForm.datePurchased : null,
          sumInRubles: parsedCost,
          comment: editForm.comment.trim() ? editForm.comment.trim() : null,
        },
      });

      notifications.show({
        title: 'Игра в коллекции обновлена',
        message: `"${editingItem.boardGame.displayName}" обновлена.`,
        color: 'teal',
      });

      setEditingItem(null);
      await invalidateCollectionQueries();
    } catch (error) {
      setEditError(parseApiError(error, 'Запрос обновления завершился ошибкой.'));
    }
  }

  async function handleDeleteConfirm() {
    if (!deletingItem) {
      return;
    }

    setDeleteError(null);

    try {
      await deleteMutation.mutateAsync({ id: deletingItem.id });

      notifications.show({
        title: 'Игра удалена из коллекции',
        message: `"${deletingItem.boardGame.displayName}" удалена.`,
        color: 'teal',
      });

      setDeletingItem(null);
      await invalidateCollectionQueries();
    } catch (error) {
      setDeleteError(parseApiError(error, 'Запрос удаления завершился ошибкой.'));
    }
  }

  const isAnyMutating = useMemo(
    () => quickPlayMutation.isPending || updateMutation.isPending || deleteMutation.isPending,
    [quickPlayMutation.isPending, updateMutation.isPending, deleteMutation.isPending],
  );

  if (isInitialLoading) {
    return <LoadingState label="Загружаем коллекцию..." />;
  }

  if (isInitialError) {
    return (
      <ErrorState
        title="Не удалось загрузить коллекцию"
        message="Запрос коллекции завершился ошибкой. Попробуйте ещё раз."
        onRetry={() => {
          void collectionQuery.refetch();
        }}
      />
    );
  }

  return (
    <Stack gap="md">
      <PageHeader
        title="Коллекция"
        description="Управляйте играми, быстрыми партиями и основными изменениями коллекции."
      />

      <Card withBorder radius="md" p="lg">
        <Group align="flex-end" grow>
          <Select
            label="Статус"
            data={statusFilterOptions}
            value={statusFilter}
            onChange={(value) => {
              setStatusFilter(value ?? 'ALL');
              resetToFirstPage();
            }}
          />
          <Switch
            label="Только полка позора"
            checked={shelfOfShame}
            onChange={(event) => {
              setShelfOfShame(event.currentTarget.checked);
              resetToFirstPage();
            }}
          />
        </Group>
      </Card>

      {items.length === 0 ? (
        <EmptyState
          title="По текущим фильтрам коллекция пуста"
          description="Измените фильтр или добавьте игры из каталога."
          action={
            <Button component={Link} to="/boardgames" variant="light" color="ember">
              Открыть поиск игр
            </Button>
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

                <Group gap="xs">
                  <CalendarDays size={14} />
                  <Text size="sm">Куплена: {formatDate(item.datePurchased)}</Text>
                </Group>

                <Text size="sm">Стоимость: {formatCost(item.sumInRubles)}</Text>
                <Text size="sm">Партий: {item.playCount}</Text>
                <Text size="sm" c="dimmed" lineClamp={2}>
                  {item.comment?.trim() ? item.comment : 'Без комментария'}
                </Text>

                <Group mt="auto" gap="xs">
                  <Button
                    component={Link}
                    to={`/collection/${item.id}`}
                    leftSection={<ArrowRight size={14} />}
                    variant="light"
                    color="ocean"
                  >
                    Подробнее
                  </Button>
                  <Button
                    leftSection={<Plus size={14} />}
                    onClick={() => {
                      void handleQuickPlay(item);
                    }}
                    loading={quickPlayMutation.isPending}
                    disabled={isAnyMutating}
                  >
                    +1 партия
                  </Button>
                  <ActionIcon
                    variant="light"
                    color="ocean"
                    aria-label="Редактировать игру в коллекции"
                    onClick={() => openEditModal(item)}
                    disabled={isAnyMutating}
                  >
                    <Pencil size={16} />
                  </ActionIcon>
                  <ActionIcon
                    variant="light"
                    color="red"
                    aria-label="Удалить игру из коллекции"
                    onClick={() => {
                      setDeletingItem(item);
                      setDeleteError(null);
                    }}
                    disabled={isAnyMutating}
                  >
                    <Trash2 size={16} />
                  </ActionIcon>
                </Group>
              </Stack>
            </Card>
          ))}
        </SimpleGrid>
      )}

      {collectionQuery.isError && items.length > 0 ? (
        <Alert color="red" variant="light">
          Не удалось обновить список коллекции.
        </Alert>
      ) : null}

      {totalPages > 1 ? (
        <Group justify="center">
          <Pagination
            total={totalPages}
            value={page}
            onChange={(next) => {
              setPage(next);
            }}
          />
        </Group>
      ) : null}

      <Modal
        opened={Boolean(editingItem)}
        onClose={() => setEditingItem(null)}
        title={editingItem ? `Редактировать: ${editingItem.boardGame.displayName}` : 'Редактировать игру в коллекции'}
        centered
      >
        <form onSubmit={handleEditSubmit}>
          <Stack>
            {editError ? (
              <Alert color="red" variant="light" icon={<AlertCircle size={16} />}>
                {editError}
              </Alert>
            ) : null}

            <Select
              label="Статус"
              data={statusOptions}
              value={editForm.status}
              onChange={(value) =>
                setEditForm((prev) => ({ ...prev, status: (value as PutCollectionItemsIdBodyStatus) ?? 'IN_COLLECTION' }))
              }
              disabled={updateMutation.isPending}
            />

            <TextInput
              type="date"
              label="Дата покупки"
              value={editForm.datePurchased}
              onChange={(event) => {
                const { value } = event.currentTarget;
                setEditForm((prev) => ({ ...prev, datePurchased: value }));
              }}
              disabled={updateMutation.isPending}
            />

            <TextInput
              type="number"
              label="Стоимость в рублях"
              min={0}
              max={COLLECTION_SUM_IN_RUBLES_MAX}
              value={editForm.sumInRubles}
              onChange={(event) => {
                const { value } = event.currentTarget;
                setEditForm((prev) => ({ ...prev, sumInRubles: value }));
              }}
              error={editFieldErrors.sumInRubles}
              disabled={updateMutation.isPending}
            />

            <Textarea
              label="Комментарий"
              value={editForm.comment}
              onChange={(event) => {
                const { value } = event.currentTarget;
                setEditForm((prev) => ({ ...prev, comment: value }));
              }}
              maxLength={2000}
              error={editFieldErrors.comment}
              minRows={3}
              disabled={updateMutation.isPending}
            />

            <Button type="submit" loading={updateMutation.isPending} disabled={updateMutation.isPending}>
              Сохранить
            </Button>
          </Stack>
        </form>
      </Modal>

      <Modal
        opened={Boolean(deletingItem)}
        onClose={() => setDeletingItem(null)}
        title="Удалить игру из коллекции"
        centered
      >
        <Stack>
          {deleteError ? (
            <Alert color="red" variant="light" icon={<AlertCircle size={16} />}>
              {deleteError}
            </Alert>
          ) : null}
          <Text>
            {deletingItem
              ? `Удалить "${deletingItem.boardGame.displayName}" из коллекции?`
              : 'Удалить эту запись из коллекции?'}
          </Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setDeletingItem(null)} disabled={deleteMutation.isPending}>
              Отмена
            </Button>
            <Button color="red" onClick={() => void handleDeleteConfirm()} loading={deleteMutation.isPending}>
              Удалить
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}
