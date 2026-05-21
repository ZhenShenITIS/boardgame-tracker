import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Modal,
  Pagination,
  Paper,
  Stack,
  Text,
  TextInput,
  Textarea,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useQueryClient } from '@tanstack/react-query';
import { AlertCircle, ArrowLeft, CalendarDays, Pencil, Plus, Trash2 } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router';

import { useGetCollectionItemsId } from '../shared/api/generated/collection-items/collection-items';
import type {
  GetCollectionItemsCollectionItemIdPlaySessions200DataItem,
  PostCollectionItemsCollectionItemIdPlaySessionsBody,
  PutPlaySessionsIdBody,
} from '../shared/api/generated/model';
import {
  useDeletePlaySessionsId,
  useGetCollectionItemsCollectionItemIdPlaySessions,
  usePostCollectionItemsCollectionItemIdPlaySessions,
  usePutPlaySessionsId,
} from '../shared/api/generated/play-sessions/play-sessions';
import { BoardGameImage } from '../shared/ui/BoardGameImage';
import { formatCollectionStatus } from '../shared/ui/collectionStatus';
import { EmptyState } from '../shared/ui/EmptyState';
import { ErrorState } from '../shared/ui/ErrorState';
import { LoadingState } from '../shared/ui/LoadingState';
import { PageHeader } from '../shared/ui/PageHeader';

type SessionFormState = {
  dateStart: string;
  dateEnd: string;
  comment: string;
};

type SessionFieldErrors = {
  dateStart?: string;
  dateEnd?: string;
  comment?: string;
};

const SESSIONS_PAGE_LIMIT = 10;

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

function parseCollectionItemId(value: string | undefined) {
  if (!value) {
    return null;
  }

  const parsed = Number(value);

  if (!Number.isInteger(parsed) || parsed < 1) {
    return null;
  }

  return parsed;
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return 'Неизвестно';
  }

  const parsed = new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString('ru-RU', {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
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

function toDateTimeLocalValue(value?: string | null) {
  if (!value) {
    return '';
  }

  const parsed = new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    return '';
  }

  const year = parsed.getFullYear();
  const month = String(parsed.getMonth() + 1).padStart(2, '0');
  const day = String(parsed.getDate()).padStart(2, '0');
  const hours = String(parsed.getHours()).padStart(2, '0');
  const minutes = String(parsed.getMinutes()).padStart(2, '0');

  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function parseDateTimeLocal(value: string) {
  const parsed = new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    return null;
  }

  return parsed;
}

function getInitialSessionFormState(): SessionFormState {
  return {
    dateStart: toDateTimeLocalValue(new Date().toISOString()),
    dateEnd: '',
    comment: '',
  };
}

export function CollectionItemDetailsPage() {
  const { id } = useParams();
  const itemId = parseCollectionItemId(id);
  const resolvedItemId = itemId ?? 0;
  const queryClient = useQueryClient();

  const [page, setPage] = useState(1);
  const [creatingSession, setCreatingSession] = useState(false);
  const [editingSession, setEditingSession] = useState<GetCollectionItemsCollectionItemIdPlaySessions200DataItem | null>(null);
  const [sessionForm, setSessionForm] = useState<SessionFormState>(getInitialSessionFormState);
  const [sessionFieldErrors, setSessionFieldErrors] = useState<SessionFieldErrors>({});
  const [sessionFormError, setSessionFormError] = useState<string | null>(null);
  const [deletingSession, setDeletingSession] = useState<GetCollectionItemsCollectionItemIdPlaySessions200DataItem | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const itemQuery = useGetCollectionItemsId(resolvedItemId);
  const sessionsQuery = useGetCollectionItemsCollectionItemIdPlaySessions(resolvedItemId, {
    page,
    limit: SESSIONS_PAGE_LIMIT,
  });

  const createMutation = usePostCollectionItemsCollectionItemIdPlaySessions();
  const updateMutation = usePutPlaySessionsId();
  const deleteMutation = useDeletePlaySessionsId();

  const sessions = sessionsQuery.data?.data ?? [];
  const sessionsTotalPages = sessionsQuery.data?.pagination.totalPages ?? 0;
  const isEditing = Boolean(editingSession);
  const isSessionModalOpen = creatingSession || isEditing;
  const isSessionSubmitting = createMutation.isPending || updateMutation.isPending;

  const isAnyMutating = useMemo(
    () => createMutation.isPending || updateMutation.isPending || deleteMutation.isPending,
    [createMutation.isPending, updateMutation.isPending, deleteMutation.isPending],
  );

  if (!itemId) {
    return (
      <Stack gap="md">
        <ErrorState
          title="Некорректный ID записи коллекции"
          message="Откройте детали из списка коллекции."
        />
        <Group>
          <Button component={Link} to="/collection" leftSection={<ArrowLeft size={14} />} variant="light" color="ember">
            Назад к коллекции
          </Button>
        </Group>
      </Stack>
    );
  }

  if (itemQuery.isLoading) {
    return <LoadingState label="Загружаем запись коллекции..." />;
  }

  if (itemQuery.isError || !itemQuery.data) {
    return (
      <ErrorState
        title="Не удалось загрузить запись коллекции"
        message="Запрос деталей завершился ошибкой. Попробуйте ещё раз."
        onRetry={() => {
          void itemQuery.refetch();
        }}
      />
    );
  }

  const item = itemQuery.data;

  async function invalidateDetailsQueries() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['/collection-items'] }),
      queryClient.invalidateQueries({ queryKey: [`/collection-items/${itemId}`] }),
      queryClient.invalidateQueries({ queryKey: [`/collection-items/${itemId}/play-sessions`] }),
      queryClient.invalidateQueries({ queryKey: ['/me/stats'] }),
      queryClient.invalidateQueries({ queryKey: ['/collection-items/shelf-of-shame'] }),
    ]);
  }

  function openCreateModal() {
    setCreatingSession(true);
    setEditingSession(null);
    setSessionForm(getInitialSessionFormState());
    setSessionFieldErrors({});
    setSessionFormError(null);
  }

  function openEditModal(session: GetCollectionItemsCollectionItemIdPlaySessions200DataItem) {
    setCreatingSession(false);
    setEditingSession(session);
    setSessionForm({
      dateStart: toDateTimeLocalValue(session.dateStart),
      dateEnd: toDateTimeLocalValue(session.dateEnd),
      comment: session.comment ?? '',
    });
    setSessionFieldErrors({});
    setSessionFormError(null);
  }

  function closeSessionModal() {
    setCreatingSession(false);
    setEditingSession(null);
    setSessionFieldErrors({});
    setSessionFormError(null);
  }

  function validateSessionForm() {
    const errors: SessionFieldErrors = {};
    const payload: PostCollectionItemsCollectionItemIdPlaySessionsBody | PutPlaySessionsIdBody = {
      dateStart: '',
      dateEnd: null,
      comment: null,
    };

    const startRaw = sessionForm.dateStart.trim();
    const endRaw = sessionForm.dateEnd.trim();
    const commentRaw = sessionForm.comment.trim();

    if (!startRaw) {
      errors.dateStart = 'Укажите дату начала';
    }

    const parsedStart = startRaw ? parseDateTimeLocal(startRaw) : null;

    if (startRaw && !parsedStart) {
      errors.dateStart = 'Некорректный формат даты начала';
    }

    const parsedEnd = endRaw ? parseDateTimeLocal(endRaw) : null;

    if (endRaw && !parsedEnd) {
      errors.dateEnd = 'Некорректный формат даты окончания';
    }

    if (parsedStart && parsedEnd && parsedEnd.getTime() < parsedStart.getTime()) {
      errors.dateEnd = 'Дата окончания должна быть позже или равна дате начала';
    }

    if (sessionForm.comment.length > 2000) {
      errors.comment = 'Комментарий должен быть не длиннее 2000 символов';
    }

    if (Object.keys(errors).length > 0) {
      return { errors, payload: null };
    }

    payload.dateStart = parsedStart!.toISOString();
    payload.dateEnd = parsedEnd ? parsedEnd.toISOString() : null;
    payload.comment = commentRaw ? commentRaw : null;

    return { errors, payload };
  }

  async function handleSessionSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const { errors, payload } = validateSessionForm();

    setSessionFieldErrors(errors);
    setSessionFormError(null);

    if (!payload) {
      return;
    }

    try {
      if (editingSession) {
        await updateMutation.mutateAsync({
          id: editingSession.id,
          data: payload,
        });

        notifications.show({
          title: 'Партия обновлена',
          message: 'Изменения партии сохранены.',
          color: 'teal',
        });
      } else {
        await createMutation.mutateAsync({
          collectionItemId: item.id,
          data: payload,
        });

        notifications.show({
          title: 'Партия создана',
          message: `Новая партия добавлена для "${item.boardGame.displayName}".`,
          color: 'teal',
        });
      }

      closeSessionModal();
      await invalidateDetailsQueries();
    } catch (error) {
      setSessionFormError(parseApiError(error, 'Не удалось сохранить партию.'));
    }
  }

  async function handleDeleteSession() {
    if (!deletingSession) {
      return;
    }

    setDeleteError(null);

    try {
      await deleteMutation.mutateAsync({ id: deletingSession.id });

      notifications.show({
        title: 'Партия удалена',
        message: `Партия #${deletingSession.id} удалена.`,
        color: 'teal',
      });

      setDeletingSession(null);
      await invalidateDetailsQueries();
    } catch (error) {
      setDeleteError(parseApiError(error, 'Не удалось удалить партию.'));
    }
  }

  return (
    <Stack gap="lg">
      <PageHeader
        title={item.boardGame.displayName}
        description="Детали игры в коллекции и история партий."
        action={
          <Group gap="xs">
            <Button component={Link} to="/collection" leftSection={<ArrowLeft size={14} />} variant="default">
              Назад
            </Button>
            <Button leftSection={<Plus size={14} />} onClick={openCreateModal} disabled={isAnyMutating}>
              Добавить партию
            </Button>
          </Group>
        }
      />

      <Card withBorder radius="md" p="lg">
        <Stack gap="sm">
          <BoardGameImage src={item.boardGame.previewUrl ?? item.boardGame.imageUrl} alt={item.boardGame.displayName} />

          <Group justify="space-between" align="flex-start">
            <Stack gap={2}>
              <Text fw={700} size="lg" lh={1.2}>
                {item.boardGame.displayName}
              </Text>
              <Text size="sm" c="dimmed">
                {item.boardGame.originalName?.trim() ? item.boardGame.originalName : 'Оригинальное название неизвестно'}
              </Text>
              <Text size="sm" c="dimmed">
                Год: {item.boardGame.yearPublished ?? 'Неизвестно'}
              </Text>
            </Stack>
            <Badge variant="light" color="ocean">
              {formatCollectionStatus(item.status)}
            </Badge>
          </Group>

          <Group gap="md" wrap="wrap">
            <Group gap="xs">
              <CalendarDays size={14} />
              <Text size="sm">Куплена: {formatDate(item.datePurchased)}</Text>
            </Group>
            <Text size="sm">Стоимость: {formatCost(item.sumInRubles)}</Text>
            <Text size="sm">Партий: {item.playCount}</Text>
          </Group>

          <Text size="sm" c="dimmed">
            {item.comment?.trim() ? item.comment : 'Без комментария'}
          </Text>
        </Stack>
      </Card>

      <Stack gap="md">
        <Group justify="space-between" align="center">
          <Text fw={600}>Партии</Text>
          {sessionsQuery.isFetching ? (
            <Text size="sm" c="dimmed">
              Обновляем...
            </Text>
          ) : null}
        </Group>

        {sessionsQuery.isLoading && sessions.length === 0 ? <LoadingState label="Загружаем партии..." /> : null}

        {sessionsQuery.isError && sessions.length === 0 ? (
          <ErrorState
            title="Не удалось загрузить партии"
            message="Запрос партий завершился ошибкой. Попробуйте ещё раз."
            onRetry={() => {
              void sessionsQuery.refetch();
            }}
          />
        ) : null}

        {!sessionsQuery.isLoading && !sessionsQuery.isError && sessions.length === 0 ? (
          <EmptyState
            title="Партий пока нет"
            description="Создайте первую партию для этой игры."
            action={
              <Button leftSection={<Plus size={14} />} onClick={openCreateModal}>
                Добавить партию
              </Button>
            }
          />
        ) : null}

        {sessions.length > 0 ? (
          <Stack gap="sm">
            {sessions.map((session) => (
              <Paper withBorder radius="md" p="md" key={session.id}>
                <Stack gap="xs">
                  <Group justify="space-between" align="flex-start">
                    <Stack gap={2}>
                      <Text fw={600}>Партия #{session.id}</Text>
                      <Text size="sm">
                        Начало: {formatDateTime(session.dateStart)}
                      </Text>
                      <Text size="sm">
                        Окончание: {formatDateTime(session.dateEnd)}
                      </Text>
                    </Stack>
                    <Group gap="xs">
                      <ActionIcon
                        variant="light"
                        color="ocean"
                        aria-label="Редактировать партию"
                        onClick={() => openEditModal(session)}
                        disabled={isAnyMutating}
                      >
                        <Pencil size={16} />
                      </ActionIcon>
                      <ActionIcon
                        variant="light"
                        color="red"
                        aria-label="Удалить партию"
                        onClick={() => {
                          setDeletingSession(session);
                          setDeleteError(null);
                        }}
                        disabled={isAnyMutating}
                      >
                        <Trash2 size={16} />
                      </ActionIcon>
                    </Group>
                  </Group>
                  <Text size="sm" c="dimmed">
                    {session.comment?.trim() ? session.comment : 'Без комментария'}
                  </Text>
                </Stack>
              </Paper>
            ))}
          </Stack>
        ) : null}

        {sessionsQuery.isError && sessions.length > 0 ? (
          <Alert color="red" variant="light">
            Не удалось обновить список партий.
          </Alert>
        ) : null}

        {sessionsTotalPages > 1 ? (
          <Group justify="center">
            <Pagination
              total={sessionsTotalPages}
              value={page}
              onChange={(next) => {
                setPage(next);
              }}
            />
          </Group>
        ) : null}
      </Stack>

      <Modal
        opened={isSessionModalOpen}
        onClose={closeSessionModal}
        title={editingSession ? `Редактировать партию #${editingSession.id}` : 'Создать партию'}
        centered
      >
        <form onSubmit={handleSessionSubmit}>
          <Stack>
            {sessionFormError ? (
              <Alert color="red" variant="light" icon={<AlertCircle size={16} />}>
                {sessionFormError}
              </Alert>
            ) : null}

            <TextInput
              label="Дата начала"
              type="datetime-local"
              value={sessionForm.dateStart}
              onChange={(event) => {
                const { value } = event.currentTarget;
                setSessionForm((prev) => ({ ...prev, dateStart: value }));
              }}
              error={sessionFieldErrors.dateStart}
              disabled={isSessionSubmitting}
              required
            />

            <TextInput
              label="Дата окончания"
              type="datetime-local"
              value={sessionForm.dateEnd}
              onChange={(event) => {
                const { value } = event.currentTarget;
                setSessionForm((prev) => ({ ...prev, dateEnd: value }));
              }}
              error={sessionFieldErrors.dateEnd}
              disabled={isSessionSubmitting}
            />

            <Textarea
              label="Комментарий"
              value={sessionForm.comment}
              onChange={(event) => {
                const { value } = event.currentTarget;
                setSessionForm((prev) => ({ ...prev, comment: value }));
              }}
              error={sessionFieldErrors.comment}
              maxLength={2000}
              minRows={3}
              disabled={isSessionSubmitting}
            />

            <Button type="submit" loading={isSessionSubmitting} disabled={isSessionSubmitting}>
              {editingSession ? 'Сохранить' : 'Создать партию'}
            </Button>
          </Stack>
        </form>
      </Modal>

      <Modal
        opened={Boolean(deletingSession)}
        onClose={() => setDeletingSession(null)}
        title="Удалить партию"
        centered
      >
        <Stack>
          {deleteError ? (
            <Alert color="red" variant="light" icon={<AlertCircle size={16} />}>
              {deleteError}
            </Alert>
          ) : null}
          <Text>
            {deletingSession
              ? `Удалить партию #${deletingSession.id}?`
              : 'Удалить эту партию?'}
          </Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setDeletingSession(null)} disabled={deleteMutation.isPending}>
              Отмена
            </Button>
            <Button color="red" loading={deleteMutation.isPending} onClick={() => void handleDeleteSession()}>
              Удалить
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}
