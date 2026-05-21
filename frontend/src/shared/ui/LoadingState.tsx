import { Center, Loader, Stack, Text } from '@mantine/core';

type LoadingStateProps = {
  label?: string;
};

export function LoadingState({ label = 'Загружаем данные...' }: LoadingStateProps) {
  return (
    <Center py="xl">
      <Stack align="center" gap="sm">
        <Loader color="ocean" />
        <Text c="dimmed" size="sm">
          {label}
        </Text>
      </Stack>
    </Center>
  );
}
