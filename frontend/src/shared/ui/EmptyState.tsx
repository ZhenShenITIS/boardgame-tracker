import { Paper, Stack, Text, ThemeIcon } from '@mantine/core';
import { Inbox } from 'lucide-react';
import { type ReactNode } from 'react';

type EmptyStateProps = {
  title: string;
  description: string;
  action?: ReactNode;
};

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <Paper p="lg" withBorder radius="md">
      <Stack align="center" ta="center" gap="sm">
        <ThemeIcon size={44} radius="xl" variant="light" color="ocean">
          <Inbox size={22} />
        </ThemeIcon>
        <Text fw={600}>{title}</Text>
        <Text c="dimmed" size="sm">
          {description}
        </Text>
        {action}
      </Stack>
    </Paper>
  );
}
