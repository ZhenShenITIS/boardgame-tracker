const collectionStatusLabels: Record<string, string> = {
  IN_COLLECTION: 'В коллекции',
  WISH_LIST: 'Вишлист',
  SOLD: 'Продана',
};

export function formatCollectionStatus(status: string | null | undefined) {
  if (!status) {
    return 'Неизвестно';
  }

  return collectionStatusLabels[status] ?? status;
}
