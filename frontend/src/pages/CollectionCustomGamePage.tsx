import { Alert, Button, Card, Group, Select, SimpleGrid, Stack, Text, TextInput, Textarea } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useQueryClient } from '@tanstack/react-query';
import { AlertCircle } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router';

import { usePostCollectionItemsCustomGame } from '../shared/api/generated/collection-items/collection-items';
import { API_INT32_MAX, COLLECTION_SUM_IN_RUBLES_MAX } from '../shared/api/numericLimits';
import type { PostCollectionItemsCustomGameBodyStatus } from '../shared/api/generated/model';
import { formatCollectionStatus } from '../shared/ui/collectionStatus';
import { PageHeader } from '../shared/ui/PageHeader';

type CustomGameFormState = {
  originalName: string;
  displayName: string;
  minPlayers: string;
  maxPlayers: string;
  playingTime: string;
  minPlayTime: string;
  maxPlayTime: string;
  minAge: string;
  yearPublished: string;
  status: PostCollectionItemsCustomGameBodyStatus;
  datePurchased: string;
  sumInRubles: string;
  comment: string;
  tagsInput: string;
};

type FieldErrors = Partial<Record<keyof CustomGameFormState, string>>;

const statusOptions: { label: string; value: PostCollectionItemsCustomGameBodyStatus }[] = [
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

function asOptionalPositiveInteger(value: string) {
  if (!value.trim()) {
    return null;
  }

  const parsed = Number(value);

  if (!Number.isFinite(parsed) || !Number.isInteger(parsed) || parsed <= 0 || parsed > API_INT32_MAX) {
    return Number.NaN;
  }

  return parsed;
}

function asOptionalNumber(value: string) {
  if (!value.trim()) {
    return null;
  }

  const parsed = Number(value);

  if (!Number.isFinite(parsed)) {
    return Number.NaN;
  }

  return parsed;
}

export function CollectionCustomGamePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const createMutation = usePostCollectionItemsCustomGame();

  const [form, setForm] = useState<CustomGameFormState>({
    originalName: '',
    displayName: '',
    minPlayers: '',
    maxPlayers: '',
    playingTime: '',
    minPlayTime: '',
    maxPlayTime: '',
    minAge: '',
    yearPublished: '',
    status: 'IN_COLLECTION',
    datePurchased: '',
    sumInRubles: '',
    comment: '',
    tagsInput: '',
  });
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [submitError, setSubmitError] = useState<string | null>(null);

  const isSubmitting = createMutation.isPending;

  const parsedTags = useMemo(
    () =>
      form.tagsInput
        .split('\n')
        .map((value) => value.trim())
        .filter(Boolean)
        .map((name) => ({ name })),
    [form.tagsInput],
  );

  function updateField<K extends keyof CustomGameFormState>(key: K, value: CustomGameFormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
    setSubmitError(null);
  }

  function validateForm() {
    const errors: FieldErrors = {};

    if (!form.originalName.trim()) {
      errors.originalName = 'Укажите оригинальное название';
    } else if (form.originalName.trim().length > 500) {
      errors.originalName = 'Оригинальное название должно быть не длиннее 500 символов';
    }

    if (!form.displayName.trim()) {
      errors.displayName = 'Укажите отображаемое название';
    } else if (form.displayName.trim().length > 500) {
      errors.displayName = 'Отображаемое название должно быть не длиннее 500 символов';
    }

    const minPlayers = asOptionalPositiveInteger(form.minPlayers);
    const maxPlayers = asOptionalPositiveInteger(form.maxPlayers);

    if (Number.isNaN(minPlayers)) {
      errors.minPlayers = 'Минимум игроков должен быть положительным целым числом';
    }

    if (Number.isNaN(maxPlayers)) {
      errors.maxPlayers = 'Максимум игроков должен быть положительным целым числом';
    }

    if (!Number.isNaN(minPlayers) && !Number.isNaN(maxPlayers) && minPlayers && maxPlayers && minPlayers > maxPlayers) {
      errors.maxPlayers = 'Максимум игроков должен быть больше или равен минимуму';
    }

    const playingTime = asOptionalPositiveInteger(form.playingTime);
    const minPlayTime = asOptionalPositiveInteger(form.minPlayTime);
    const maxPlayTime = asOptionalPositiveInteger(form.maxPlayTime);
    const minAge = asOptionalPositiveInteger(form.minAge);
    const yearPublished = asOptionalPositiveInteger(form.yearPublished);

    if (Number.isNaN(playingTime)) {
      errors.playingTime = 'Время партии должно быть положительным целым числом';
    }

    if (Number.isNaN(minPlayTime)) {
      errors.minPlayTime = 'Минимальное время партии должно быть положительным целым числом';
    }

    if (Number.isNaN(maxPlayTime)) {
      errors.maxPlayTime = 'Максимальное время партии должно быть положительным целым числом';
    }

    if (!Number.isNaN(minPlayTime) && !Number.isNaN(maxPlayTime) && minPlayTime && maxPlayTime && minPlayTime > maxPlayTime) {
      errors.maxPlayTime = 'Максимальное время должно быть больше или равно минимальному';
    }

    if (Number.isNaN(minAge)) {
      errors.minAge = 'Минимальный возраст должен быть положительным целым числом';
    }

    if (Number.isNaN(yearPublished)) {
      errors.yearPublished = 'Год издания должен быть положительным целым числом';
    }

    const sumInRubles = asOptionalNumber(form.sumInRubles);

    if (
      Number.isNaN(sumInRubles) ||
      (typeof sumInRubles === 'number' && (sumInRubles < 0 || sumInRubles > COLLECTION_SUM_IN_RUBLES_MAX))
    ) {
      errors.sumInRubles = 'Стоимость должна быть от 0 до 99 999 999,99';
    }

    if (form.comment.length > 2000) {
      errors.comment = 'Комментарий должен быть не длиннее 2000 символов';
    }

    setFieldErrors(errors);

    return {
      valid: Object.keys(errors).length === 0,
      minPlayers,
      maxPlayers,
      playingTime,
      minPlayTime,
      maxPlayTime,
      minAge,
      yearPublished,
      sumInRubles,
    };
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const validated = validateForm();

    if (!validated.valid) {
      return;
    }

    setSubmitError(null);

    try {
      const created = await createMutation.mutateAsync({
        data: {
          originalName: form.originalName.trim(),
          displayName: form.displayName.trim(),
          minPlayers: validated.minPlayers,
          maxPlayers: validated.maxPlayers,
          playingTime: validated.playingTime,
          minPlayTime: validated.minPlayTime,
          maxPlayTime: validated.maxPlayTime,
          minAge: validated.minAge,
          yearPublished: validated.yearPublished,
          status: form.status,
          datePurchased: form.datePurchased.trim() ? form.datePurchased : null,
          sumInRubles: validated.sumInRubles,
          comment: form.comment.trim() ? form.comment.trim() : null,
          tags: parsedTags,
        },
      });

      notifications.show({
        title: 'Игра создана',
        message: `"${created.boardGame.displayName}" добавлена в коллекцию.`,
        color: 'teal',
      });

      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['/collection-items'] }),
        queryClient.invalidateQueries({ queryKey: ['/me/stats'] }),
        queryClient.invalidateQueries({ queryKey: ['/collection-items/shelf-of-shame'] }),
        queryClient.invalidateQueries({ queryKey: ['/boardgames'] }),
      ]);

      navigate('/collection', { replace: true });
    } catch (error) {
      setSubmitError(parseApiError(error, 'Не удалось создать игру вручную.'));
    }
  }

  return (
    <Stack gap="md">
      <PageHeader
        title="Создать игру вручную"
        description="Добавьте игру вручную, если её нет в локальном каталоге."
      />

      <Card withBorder radius="md" p="lg">
        <form onSubmit={handleSubmit}>
          <Stack gap="md">
            {submitError ? (
              <Alert color="red" variant="light" icon={<AlertCircle size={16} />}>
                {submitError}
              </Alert>
            ) : null}

            <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
              <TextInput
                label="Оригинальное название"
                value={form.originalName}
                onChange={(event) => updateField('originalName', event.currentTarget.value)}
                error={fieldErrors.originalName}
                required
                maxLength={500}
                disabled={isSubmitting}
              />
              <TextInput
                label="Отображаемое название"
                value={form.displayName}
                onChange={(event) => updateField('displayName', event.currentTarget.value)}
                error={fieldErrors.displayName}
                required
                maxLength={500}
                disabled={isSubmitting}
              />
              <TextInput
                label="Мин. игроков"
                type="number"
                min={1}
                max={API_INT32_MAX}
                value={form.minPlayers}
                onChange={(event) => updateField('minPlayers', event.currentTarget.value)}
                error={fieldErrors.minPlayers}
                disabled={isSubmitting}
              />
              <TextInput
                label="Макс. игроков"
                type="number"
                min={1}
                max={API_INT32_MAX}
                value={form.maxPlayers}
                onChange={(event) => updateField('maxPlayers', event.currentTarget.value)}
                error={fieldErrors.maxPlayers}
                disabled={isSubmitting}
              />
              <TextInput
                label="Время партии (мин)"
                type="number"
                min={1}
                max={API_INT32_MAX}
                value={form.playingTime}
                onChange={(event) => updateField('playingTime', event.currentTarget.value)}
                error={fieldErrors.playingTime}
                disabled={isSubmitting}
              />
              <TextInput
                label="Мин. время партии (мин)"
                type="number"
                min={1}
                max={API_INT32_MAX}
                value={form.minPlayTime}
                onChange={(event) => updateField('minPlayTime', event.currentTarget.value)}
                error={fieldErrors.minPlayTime}
                disabled={isSubmitting}
              />
              <TextInput
                label="Макс. время партии (мин)"
                type="number"
                min={1}
                max={API_INT32_MAX}
                value={form.maxPlayTime}
                onChange={(event) => updateField('maxPlayTime', event.currentTarget.value)}
                error={fieldErrors.maxPlayTime}
                disabled={isSubmitting}
              />
              <TextInput
                label="Мин. возраст"
                type="number"
                min={1}
                max={API_INT32_MAX}
                value={form.minAge}
                onChange={(event) => updateField('minAge', event.currentTarget.value)}
                error={fieldErrors.minAge}
                disabled={isSubmitting}
              />
              <TextInput
                label="Год издания"
                type="number"
                min={1}
                max={API_INT32_MAX}
                value={form.yearPublished}
                onChange={(event) => updateField('yearPublished', event.currentTarget.value)}
                error={fieldErrors.yearPublished}
                disabled={isSubmitting}
              />
              <Select
                label="Статус"
                data={statusOptions}
                value={form.status}
                onChange={(value) => updateField('status', (value as PostCollectionItemsCustomGameBodyStatus) ?? 'IN_COLLECTION')}
                disabled={isSubmitting}
              />
              <TextInput
                type="date"
                label="Дата покупки"
                value={form.datePurchased}
                onChange={(event) => updateField('datePurchased', event.currentTarget.value)}
                disabled={isSubmitting}
              />
              <TextInput
                label="Стоимость в рублях"
                type="number"
                min={0}
                max={COLLECTION_SUM_IN_RUBLES_MAX}
                value={form.sumInRubles}
                onChange={(event) => updateField('sumInRubles', event.currentTarget.value)}
                error={fieldErrors.sumInRubles}
                disabled={isSubmitting}
              />
            </SimpleGrid>

            <Textarea
              label="Комментарий"
              value={form.comment}
              onChange={(event) => updateField('comment', event.currentTarget.value)}
              maxLength={2000}
              error={fieldErrors.comment}
              minRows={3}
              disabled={isSubmitting}
            />

            <Textarea
              label="Теги (по одному в строке)"
              value={form.tagsInput}
              onChange={(event) => updateField('tagsInput', event.currentTarget.value)}
              minRows={3}
              disabled={isSubmitting}
            />
            <Text c="dimmed" size="sm">
              Распознано тегов: {parsedTags.length}
            </Text>

            <Group justify="flex-end">
              <Button variant="default" onClick={() => navigate('/boardgames')} disabled={isSubmitting}>
                Отмена
              </Button>
              <Button type="submit" loading={isSubmitting} disabled={isSubmitting}>
                Создать игру
              </Button>
            </Group>
          </Stack>
        </form>
      </Card>
    </Stack>
  );
}
