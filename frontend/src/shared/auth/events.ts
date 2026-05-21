type AuthSessionClearedListener = () => void;

const listeners = new Set<AuthSessionClearedListener>();

export function subscribeAuthSessionCleared(listener: AuthSessionClearedListener) {
  listeners.add(listener);

  return () => {
    listeners.delete(listener);
  };
}

export function notifyAuthSessionCleared() {
  listeners.forEach((listener) => {
    listener();
  });
}
