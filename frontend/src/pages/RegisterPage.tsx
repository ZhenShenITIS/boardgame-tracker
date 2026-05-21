import { Alert, Button, Card, PasswordInput, Stack, TextInput } from '@mantine/core';
import { AlertCircle } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router';

import { getAuthErrorMessage, useAuth } from '../shared/auth';
import { PageHeader } from '../shared/ui/PageHeader';

const emailPattern = /^\S+@\S+\.\S+$/;
const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,128}$/;

export function RegisterPage() {
  const navigate = useNavigate();
  const { register } = useAuth();

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<{ name?: string; email?: string; password?: string }>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  function validate() {
    const nextErrors: { name?: string; email?: string; password?: string } = {};
    const trimmedName = name.trim();
    const trimmedEmail = email.trim();

    if (!trimmedName) {
      nextErrors.name = 'Укажите имя';
    } else if (trimmedName.length > 100) {
      nextErrors.name = 'Имя должно быть не длиннее 100 символов';
    }

    if (!trimmedEmail) {
      nextErrors.email = 'Укажите email';
    } else if (!emailPattern.test(trimmedEmail)) {
      nextErrors.email = 'Введите корректный email';
    }

    if (!password) {
      nextErrors.password = 'Укажите пароль';
    } else if (password.length < 8) {
      nextErrors.password = 'Пароль должен быть не короче 8 символов';
    } else if (!passwordPattern.test(password)) {
      nextErrors.password = 'Используйте строчные и заглавные буквы, а также хотя бы одну цифру';
    }

    return nextErrors;
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitError(null);

    const validationErrors = validate();
    setErrors(validationErrors);

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    setIsSubmitting(true);

    try {
      await register({ name: name.trim(), email: email.trim(), password });
      navigate('/', { replace: true });
    } catch (error) {
      setSubmitError(getAuthErrorMessage(error, 'Не удалось создать аккаунт с указанными данными.'));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Stack gap="md">
      <PageHeader title="Регистрация" description="Создайте аккаунт Boardgame Tracker." />
      <Card withBorder radius="md" p="lg" maw={420} w="100%" mx="auto">
        <form onSubmit={handleSubmit}>
          <Stack>
            {submitError ? (
              <Alert color="red" icon={<AlertCircle size={16} />} variant="light">
                {submitError}
              </Alert>
            ) : null}
            <TextInput
              label="Имя"
              placeholder="Имя для отображения"
              value={name}
              onChange={(event) => {
                setName(event.currentTarget.value);
                setSubmitError(null);
              }}
              error={errors.name}
              disabled={isSubmitting}
              autoComplete="name"
            />
            <TextInput
              label="Электронная почта"
              placeholder="you@example.com"
              value={email}
              onChange={(event) => {
                setEmail(event.currentTarget.value);
                setSubmitError(null);
              }}
              error={errors.email}
              disabled={isSubmitting}
              autoComplete="email"
            />
            <PasswordInput
              label="Пароль"
              placeholder="Придумайте пароль"
              value={password}
              onChange={(event) => {
                setPassword(event.currentTarget.value);
                setSubmitError(null);
              }}
              error={errors.password}
              disabled={isSubmitting}
              autoComplete="new-password"
            />
            <Button type="submit" loading={isSubmitting} disabled={isSubmitting}>
              Создать аккаунт
            </Button>
          </Stack>
        </form>
      </Card>
    </Stack>
  );
}
