import { Box, Image } from '@mantine/core';

type BoardGameImageProps = {
  src: string | null | undefined;
  alt: string;
};

export function BoardGameImage({ src, alt }: BoardGameImageProps) {
  if (!src) {
    return null;
  }

  return (
    <Box
      style={{
        aspectRatio: '1 / 1',
        borderRadius: 'var(--mantine-radius-sm)',
        overflow: 'hidden',
        width: '100%',
      }}
    >
      <Image src={src} alt={alt} fit="cover" h="100%" w="100%" />
    </Box>
  );
}
