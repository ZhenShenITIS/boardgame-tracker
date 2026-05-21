import { Alert, Button, Card, PasswordInput, Stack, TextInput } from '@mantine/core';
import { AlertCircle } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router';

import { getAuthErrorMessage, useAuth } from '../shared/auth';
import { PageHeader } from '../shared/ui/PageHeader';

const emailPattern = /^\S+@\S+\.\S+$/;
const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,128}$/;

export function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  function validate() {
    const nextErrors: { email?: string; password?: string } = {};

    if (!email.trim()) {
      nextErrors.email = 'Укажите email';
    } else if (!emailPattern.test(email.trim())) {
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
      await login({ email: email.trim(), password });
      navigate('/', { replace: true });
    } catch (error) {
      setSubmitError(getAuthErrorMessage(error, 'Не удалось войти с указанными данными.'));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Stack gap="md">
      <PageHeader title="Вход" description="Войдите в аккаунт Boardgame Tracker." />
      <Card withBorder radius="md" p="lg" maw={420} w="100%" mx="auto">
        <form onSubmit={handleSubmit}>
          <Stack>
            {submitError ? (
              <Alert color="red" icon={<AlertCircle size={16} />} variant="light">
                {submitError}
              </Alert>
            ) : null}
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
              placeholder="Введите пароль"
              value={password}
              onChange={(event) => {
                setPassword(event.currentTarget.value);
                setSubmitError(null);
              }}
              error={errors.password}
              disabled={isSubmitting}
              autoComplete="current-password"
            />
            <Button type="submit" loading={isSubmitting} disabled={isSubmitting}>
              Войти
            </Button>
          </Stack>
        </form>
      </Card>
    </Stack>
  );
}
