import {
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Modal,
  NumberInput,
  Select,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Textarea,
} from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { notifications } from '@mantine/notifications';
import { useMutationState, useQueryClient } from '@tanstack/react-query';
import { CalendarDays, Plus, Search, Users } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router';

import { useGetBoardgames } from '../shared/api/generated/boardgames/boardgames';
import { usePostCollectionItems } from '../shared/api/generated/collection-items/collection-items';
import type { GetBoardgames200DataItem, PostCollectionItemsBodyStatus } from '../shared/api/generated/model';
import { BoardGameImage } from '../shared/ui/BoardGameImage';
import { formatCollectionStatus } from '../shared/ui/collectionStatus';
import { EmptyState } from '../shared/ui/EmptyState';
import { ErrorState } from '../shared/ui/ErrorState';
import { LoadingState } from '../shared/ui/LoadingState';
import { PageHeader } from '../shared/ui/PageHeader';

type AddFormState = {
  status: PostCollectionItemsBodyStatus;
  datePurchased: string;
  sumInRubles: string;
  comment: string;
};

const PAGE_LIMIT = 20;

const statusOptions: { label: string; value: PostCollectionItemsBodyStatus }[] = [
  { label: formatCollectionStatus('IN_COLLECTION'), value: 'IN_COLLECTION' },
  { label: formatCollectionStatus('WISH_LIST'), value: 'WISH_LIST' },
  { label: formatCollectionStatus('SOLD'), value: 'SOLD' },
];

function getApiErrorMessage(error: unknown, fallback: string) {
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

function mergeBoardgames(current: GetBoardgames200DataItem[], incoming: GetBoardgames200DataItem[]) {
  if (current.length === 0) {
    return incoming;
  }

  const seen = new Set(current.map((game) => game.id));
  const next = [...current];

  incoming.forEach((game) => {
    if (!seen.has(game.id)) {
      next.push(game);
      seen.add(game.id);
    }
  });

  return next;
}

function formatPlayers(game: GetBoardgames200DataItem) {
  if (game.minPlayers && game.maxPlayers) {
    return `${game.minPlayers}-${game.maxPlayers} игроков`;
  }

  if (game.minPlayers) {
    return `От ${game.minPlayers} игроков`;
  }

  if (game.maxPlayers) {
    return `До ${game.maxPlayers} игроков`;
  }

  return 'Количество игроков неизвестно';
}

function formatTime(game: GetBoardgames200DataItem) {
  if (game.playingTime) {
    return `${game.playingTime} мин`;
  }

  if (game.minPlayTime && game.maxPlayTime) {
    return `${game.minPlayTime}-${game.maxPlayTime} мин`;
  }

  return 'Время партии неизвестно';
}

export function BoardgamesPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [debouncedSearch] = useDebouncedValue(search, 400);
  const [page, setPage] = useState(1);
  const [items, setItems] = useState<GetBoardgames200DataItem[]>([]);
  const [selectedGame, setSelectedGame] = useState<GetBoardgames200DataItem | null>(null);
  const [addForm, setAddForm] = useState<AddFormState>({
    status: 'IN_COLLECTION',
    datePurchased: '',
    sumInRubles: '',
    comment: '',
  });
  const [addFormError, setAddFormError] = useState<string | null>(null);
  const [addFieldErrors, setAddFieldErrors] = useState<{
    status?: string;
    datePurchased?: string;
    sumInRubles?: string;
    comment?: string;
  }>({});

  const searchQuery = debouncedSearch.trim();

  const boardgamesQuery = useGetBoardgames({
    query: searchQuery || undefined,
    page,
    limit: PAGE_LIMIT,
  });

  const addMutation = usePostCollectionItems();
  const isAdding = addMutation.isPending;

  const statsRefetching = useMutationState({
    filters: { mutationKey: ['postCollectionItems'] },
    select: (mutation) => mutation.state.status === 'pending',
  }).some(Boolean);

  useEffect(() => {
    setPage(1);
    setItems([]);
  }, [searchQuery]);

  useEffect(() => {
    if (!boardgamesQuery.data) {
      return;
    }

    if (page === 1) {
      setItems(boardgamesQuery.data.data);
      return;
    }

    setItems((prev) => mergeBoardgames(prev, boardgamesQuery.data!.data));
  }, [boardgamesQuery.data, page]);

  const hasMore = useMemo(() => {
    const currentPage = boardgamesQuery.data?.pagination.page ?? page;
    const totalPages = boardgamesQuery.data?.pagination.totalPages ?? 0;
    return totalPages > 0 && currentPage < totalPages;
  }, [boardgamesQuery.data?.pagination.page, boardgamesQuery.data?.pagination.totalPages, page]);

  function openAddModal(game: GetBoardgames200DataItem) {
    setSelectedGame(game);
    setAddForm({
      status: 'IN_COLLECTION',
      datePurchased: '',
      sumInRubles: '',
      comment: '',
    });
    setAddFormError(null);
    setAddFieldErrors({});
  }

  function closeAddModal() {
    setSelectedGame(null);
    setAddFormError(null);
    setAddFieldErrors({});
  }

  async function submitAddToCollection(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!selectedGame) {
      return;
    }

    const fieldErrors: {
      status?: string;
      datePurchased?: string;
      sumInRubles?: string;
      comment?: string;
    } = {};

    if (!addForm.status) {
      fieldErrors.status = 'Укажите статус';
    }

    if (addForm.sumInRubles) {
      const value = Number(addForm.sumInRubles);

      if (Number.isNaN(value) || value < 0) {
        fieldErrors.sumInRubles = 'Сумма должна быть больше или равна 0';
      }
    }

    if (addForm.comment.length > 2000) {
      fieldErrors.comment = 'Комментарий должен быть не длиннее 2000 символов';
    }

    setAddFieldErrors(fieldErrors);
    setAddFormError(null);

    if (Object.keys(fieldErrors).length > 0) {
      return;
    }

    try {
      await addMutation.mutateAsync({
        data: {
          boardGameId: selectedGame.id,
          status: addForm.status,
          datePurchased: addForm.datePurchased || null,
          sumInRubles: addForm.sumInRubles ? Number(addForm.sumInRubles) : null,
          comment: addForm.comment || null,
        },
      });

      notifications.show({
        title: 'Добавлено в коллекцию',
        message: `"${selectedGame.displayName}" добавлена в коллекцию.`,
        color: 'teal',
      });

      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['/collection-items'] }),
        queryClient.invalidateQueries({ queryKey: ['/me/stats'] }),
        queryClient.invalidateQueries({ queryKey: ['/collection-items/shelf-of-shame'] }),
      ]);

      closeAddModal();
    } catch (error) {
      setAddFormError(getApiErrorMessage(error, 'Не удалось добавить игру в коллекцию.'));
    }
  }

  const isInitialLoading = boardgamesQuery.isLoading && items.length === 0;
  const isInitialError = boardgamesQuery.isError && items.length === 0;

  if (isInitialLoading) {
    return <LoadingState label="Загружаем локальный каталог игр..." />;
  }

  if (isInitialError) {
    return (
      <ErrorState
        title="Не удалось загрузить игры"
        message="Запрос к локальному каталогу завершился ошибкой. Попробуйте ещё раз."
        onRetry={() => {
          void boardgamesQuery.refetch();
        }}
      />
    );
  }

  return (
    <Stack gap="lg">
      <PageHeader title="Игры" description="Ищите игры в локальном каталоге и добавляйте их в коллекцию." />

      <Card withBorder radius="md" p="lg">
        <TextInput
          leftSection={<Search size={16} />}
          placeholder="Поиск по локальному каталогу"
          value={search}
          onChange={(event) => {
            setSearch(event.currentTarget.value);
          }}
        />
      </Card>

      {items.length === 0 ? (
        <EmptyState
          title="В локальном каталоге ничего не найдено"
          description="Попробуйте другой запрос или создайте игру вручную."
          action={
            <Button component={Link} to="/collection/custom-game" variant="light" color="ember">
              Открыть форму ручного добавления
            </Button>
          }
        />
      ) : (
        <SimpleGrid cols={{ base: 1, sm: 2, xl: 3 }} spacing="md">
          {items.map((game) => (
            <Card withBorder radius="md" key={game.id} p="lg">
              <Stack gap="sm" h="100%">
                <BoardGameImage src={game.previewUrl ?? game.imageUrl} alt={game.displayName} />
                <Group justify="space-between" align="flex-start">
                  <Stack gap={2}>
                    <Text fw={600} lh={1.2}>
                      {game.displayName}
                    </Text>
                    <Text size="sm" c="dimmed">
                      {game.yearPublished ?? 'Год неизвестен'}
                    </Text>
                  </Stack>
                  {game.complexity ? (
                    <Badge variant="light" color="ocean">
                      {game.complexity.toFixed(1)}
                    </Badge>
                  ) : null}
                </Group>
                <Group gap="xs">
                  <Users size={14} />
                  <Text size="sm">{formatPlayers(game)}</Text>
                </Group>
                <Group gap="xs">
                  <CalendarDays size={14} />
                  <Text size="sm">{formatTime(game)}</Text>
                </Group>
                <Button
                  mt="auto"
                  leftSection={<Plus size={16} />}
                  onClick={() => {
                    openAddModal(game);
                  }}
                >
                  Добавить в коллекцию
                </Button>
              </Stack>
            </Card>
          ))}
        </SimpleGrid>
      )}

      {boardgamesQuery.isError && items.length > 0 ? (
        <Alert color="red" variant="light">
          Не удалось загрузить следующую порцию результатов.
        </Alert>
      ) : null}

      {hasMore ? (
        <Group justify="center">
          <Button
            variant="default"
            loading={boardgamesQuery.isFetching}
            disabled={boardgamesQuery.isFetching}
            onClick={() => {
              setPage((prev) => prev + 1);
            }}
          >
            Загрузить ещё
          </Button>
        </Group>
      ) : null}

      <Modal
        opened={Boolean(selectedGame)}
        onClose={closeAddModal}
        title={selectedGame ? `Добавить "${selectedGame.displayName}"` : 'Добавить в коллекцию'}
        centered
      >
        <form onSubmit={submitAddToCollection}>
          <Stack>
            {addFormError ? (
              <Alert color="red" variant="light">
                {addFormError}
              </Alert>
            ) : null}

            <Select
              label="Статус"
              data={statusOptions}
              value={addForm.status}
              onChange={(value) => {
                setAddForm((prev) => ({ ...prev, status: (value as PostCollectionItemsBodyStatus) ?? 'IN_COLLECTION' }));
              }}
              error={addFieldErrors.status}
              disabled={isAdding}
              required
            />

            <TextInput
              type="date"
              label="Дата покупки"
              value={addForm.datePurchased}
              onChange={(event) => {
                const { value } = event.currentTarget;
                setAddForm((prev) => ({ ...prev, datePurchased: value }));
              }}
              error={addFieldErrors.datePurchased}
              disabled={isAdding}
            />

            <NumberInput
              label="Стоимость в рублях"
              min={0}
              value={addForm.sumInRubles}
              onChange={(value) => {
                setAddForm((prev) => ({ ...prev, sumInRubles: value === '' ? '' : String(value ?? '') }));
              }}
              error={addFieldErrors.sumInRubles}
              disabled={isAdding}
            />

            <Textarea
              label="Комментарий"
              value={addForm.comment}
              maxLength={2000}
              onChange={(event) => {
                const { value } = event.currentTarget;
                setAddForm((prev) => ({ ...prev, comment: value }));
              }}
              error={addFieldErrors.comment}
              disabled={isAdding}
              minRows={3}
            />

            <Button type="submit" loading={isAdding || statsRefetching} disabled={isAdding || statsRefetching}>
              Добавить игру
            </Button>
          </Stack>
        </form>
      </Modal>
    </Stack>
  );
}
