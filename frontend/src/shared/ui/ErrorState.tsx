import { Alert, Button, Group, Stack, Text } from '@mantine/core';
import { AlertCircle, RefreshCw } from 'lucide-react';

type ErrorStateProps = {
  title?: string;
  message?: string;
  onRetry?: () => void;
};

export function ErrorState({
  title = 'Что-то пошло не так',
  message = 'Не удалось загрузить этот раздел. Попробуйте ещё раз.',
  onRetry,
}: ErrorStateProps) {
  return (
    <Alert color="red" icon={<AlertCircle size={16} />} title={title} variant="light">
      <Stack gap="sm">
        <Text size="sm">{message}</Text>
        {onRetry ? (
          <Group>
            <Button leftSection={<RefreshCw size={16} />} variant="outline" color="red" onClick={onRetry}>
              Повторить
            </Button>
          </Group>
        ) : null}
      </Stack>
    </Alert>
  );
}
