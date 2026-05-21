import { Badge, Group, Stack, Text, Title } from '@mantine/core';
import { type ReactNode } from 'react';

type PageHeaderProps = {
  title: string;
  description?: string;
  action?: ReactNode;
  badge?: string;
};

export function PageHeader({ title, description, action, badge }: PageHeaderProps) {
  return (
    <Group justify="space-between" align="flex-start" gap="md">
      <Stack gap={4}>
        <Group gap="xs">
          <Title order={2}>{title}</Title>
          {badge ? (
            <Badge variant="light" color="ember">
              {badge}
            </Badge>
          ) : null}
        </Group>
        {description ? (
          <Text c="dimmed" size="sm">
            {description}
          </Text>
        ) : null}
      </Stack>
      {action}
    </Group>
  );
}
