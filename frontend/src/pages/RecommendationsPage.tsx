import {
  Badge,
  Button,
  Card,
  Group,
  NumberInput,
  SegmentedControl,
  SimpleGrid,
  Stack,
  Text,
} from '@mantine/core';
import { useMemo, useState } from 'react';
import { Link } from 'react-router';

import { useGetRecommendationsTonight } from '../shared/api/generated/recommendations/recommendations';
import { BoardGameImage } from '../shared/ui/BoardGameImage';
import { formatCollectionStatus } from '../shared/ui/collectionStatus';
import { EmptyState } from '../shared/ui/EmptyState';
import { ErrorState } from '../shared/ui/ErrorState';
import { LoadingState } from '../shared/ui/LoadingState';
import { PageHeader } from '../shared/ui/PageHeader';

const limitOptions = [5, 10, 20, 30, 50];

function formatPlayers(minPlayers?: number | null, maxPlayers?: number | null) {
  if (minPlayers && maxPlayers) {
    return `${minPlayers}-${maxPlayers} игроков`;
  }

  if (minPlayers) {
    return `От ${minPlayers} игроков`;
  }

  if (maxPlayers) {
    return `До ${maxPlayers} игроков`;
  }

  return 'Количество игроков неизвестно';
}

function formatPlayTime(game: {
  playingTime?: number | null;
  minPlayTime?: number | null;
  maxPlayTime?: number | null;
}) {
  if (game.playingTime) {
    return `${game.playingTime} мин`;
  }

  if (game.minPlayTime && game.maxPlayTime) {
    return `${game.minPlayTime}-${game.maxPlayTime} мин`;
  }

  if (game.minPlayTime) {
    return `От ${game.minPlayTime} мин`;
  }

  if (game.maxPlayTime) {
    return `До ${game.maxPlayTime} мин`;
  }

  return 'Время партии неизвестно';
}

function humanizeReason(reason: string) {
  const map: Record<string, string> = {
    shelf_of_shame_bonus: 'Игра ещё не сыграна',
    matches_player_count: 'Подходит под выбранное число игроков',
    short_play_time_bonus: 'Подходит под короткую партию',
    in_collection_bonus: 'Уже есть в вашей коллекции',
    recently_added_bonus: 'Недавно добавлена в коллекцию',
    custom_game_bonus: 'Игра создана вручную',
  };

  if (map[reason]) {
    return map[reason];
  }

  const normalized = reason.replaceAll('_', ' ').trim();

  if (!normalized) {
    return 'Другой сигнал рекомендации';
  }

  return normalized[0].toUpperCase() + normalized.slice(1);
}

function toNumberOrEmpty(value: string | number): number | '' {
  return typeof value === 'number' ? value : '';
}

export function RecommendationsPage() {
  const [playerCount, setPlayerCount] = useState<number | ''>(2);
  const [maxPlayTimeMinutes, setMaxPlayTimeMinutes] = useState<number | ''>('');
  const [limit, setLimit] = useState<number | ''>(10);
  const [shelfOnly, setShelfOnly] = useState('false');

  const validPlayerCount = typeof playerCount === 'number' && playerCount >= 1;
  const validMaxPlayTime = typeof maxPlayTimeMinutes === 'number' && maxPlayTimeMinutes >= 1;
  const validLimit = typeof limit === 'number' && limit >= 1;

  const queryParams = useMemo(
    () => ({
      playerCount: validPlayerCount ? playerCount : 1,
      maxPlayTimeMinutes: validMaxPlayTime ? maxPlayTimeMinutes : undefined,
      shelfOfShameOnly: shelfOnly === 'true' ? true : undefined,
      limit: validLimit ? limit : undefined,
    }),
    [limit, maxPlayTimeMinutes, playerCount, shelfOnly, validLimit, validMaxPlayTime, validPlayerCount],
  );

  const recommendationsQuery = useGetRecommendationsTonight(queryParams, {
    query: {
      enabled: validPlayerCount,
    },
  });

  const recommendations = recommendationsQuery.data?.data ?? [];

  return (
    <Stack gap="lg">
      <PageHeader
        title="Рекомендации"
        description="Укажите число игроков и получите подходящие игры из коллекции."
      />

      <Card withBorder radius="md" p="lg">
        <Stack gap="md">
          <Group align="flex-end" grow>
            <NumberInput
              label="Игроки"
              value={playerCount}
              min={1}
              clampBehavior="strict"
              allowDecimal={false}
              onChange={(value) => setPlayerCount(toNumberOrEmpty(value))}
              required
            />
            <NumberInput
              label="Макс. время партии (мин)"
              value={maxPlayTimeMinutes}
              min={1}
              clampBehavior="strict"
              allowDecimal={false}
              onChange={(value) => setMaxPlayTimeMinutes(toNumberOrEmpty(value))}
              placeholder="Необязательно"
            />
            <NumberInput
              label="Лимит"
              value={limit}
              min={1}
              max={50}
              clampBehavior="strict"
              allowDecimal={false}
              onChange={(value) => setLimit(toNumberOrEmpty(value))}
            />
          </Group>

          <Group justify="space-between" align="center">
            <Stack gap={4}>
              <Text size="sm" fw={500}>
                Только полка позора
              </Text>
              <SegmentedControl
                size="xs"
                data={[
                  { label: 'Все', value: 'false' },
                  { label: 'Только полка', value: 'true' },
                ]}
                value={shelfOnly}
                onChange={setShelfOnly}
              />
            </Stack>
            <Text size="sm" c="dimmed">
              {validPlayerCount
                ? `Найдено: ${recommendations.length}`
                : 'Укажите корректное число игроков (>= 1)'}
            </Text>
          </Group>
        </Stack>
      </Card>

      {!validPlayerCount ? (
        <EmptyState
          title="Укажите число игроков"
          description="Задайте число игроков, чтобы загрузить рекомендации на вечер."
        />
      ) : null}

      {validPlayerCount && recommendationsQuery.isLoading ? (
        <LoadingState label="Загружаем рекомендации на вечер..." />
      ) : null}

      {validPlayerCount && recommendationsQuery.isError ? (
        <ErrorState
          title="Не удалось загрузить рекомендации"
          message="Не удалось рассчитать рекомендации по выбранным фильтрам."
          onRetry={() => {
            void recommendationsQuery.refetch();
          }}
        />
      ) : null}

      {validPlayerCount && !recommendationsQuery.isLoading && !recommendationsQuery.isError && recommendations.length === 0 ? (
        <EmptyState
          title="Нет рекомендаций по выбранным фильтрам"
          description="Ослабьте ограничения или отключите режим только полки позора."
          action={
            <Button variant="light" onClick={() => setLimit(limitOptions[1])}>
              Сбросить лимит до 10
            </Button>
          }
        />
      ) : null}

      {validPlayerCount && !recommendationsQuery.isLoading && !recommendationsQuery.isError && recommendations.length > 0 ? (
        <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
          {recommendations.map((entry) => (
            <Card withBorder radius="md" p="lg" key={entry.collectionItemId}>
              <Stack gap="sm" h="100%">
                <BoardGameImage src={entry.boardGame.previewUrl ?? entry.boardGame.imageUrl} alt={entry.boardGame.displayName} />

                <Group justify="space-between" align="flex-start">
                  <Stack gap={2}>
                    <Text fw={600} lh={1.2}>
                      {entry.boardGame.displayName}
                    </Text>
                    <Text size="sm" c="dimmed">
                      {formatPlayers(entry.boardGame.minPlayers, entry.boardGame.maxPlayers)} • {formatPlayTime(entry.boardGame)}
                    </Text>
                  </Stack>
                  <Badge color="ocean" variant="light">
                    {formatCollectionStatus(entry.status)}
                  </Badge>
                </Group>

                <Group gap="md">
                  <Text size="sm">Партий: {entry.playCount}</Text>
                  <Text size="sm">Оценка: {entry.score.toFixed(1)}</Text>
                </Group>

                <Stack gap={4}>
                  {entry.reasons.map((reason) => (
                    <Text key={`${entry.collectionItemId}-${reason}`} size="sm" c="dimmed">
                      • {humanizeReason(reason)}
                    </Text>
                  ))}
                </Stack>

                <Group mt="auto">
                  <Button
                    component={Link}
                    to={`/collection/${entry.collectionItemId}`}
                    variant="light"
                    color="ocean"
                  >
                    Открыть запись коллекции
                  </Button>
                </Group>
              </Stack>
            </Card>
          ))}
        </SimpleGrid>
      ) : null}
    </Stack>
  );
}
