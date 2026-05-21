import { defineConfig } from 'orval';

export default defineConfig({
  boardgameTracker: {
    input: {
      target: '.orval/openapi.dereferenced.json',
    },
    output: {
      target: 'src/shared/api/generated/endpoints.ts',
      schemas: 'src/shared/api/generated/model',
      client: 'react-query',
      httpClient: 'axios',
      mode: 'tags-split',
      clean: true,
      override: {
        mutator: {
          path: './src/shared/api/client/custom-instance.ts',
          name: 'customInstance',
        },
      },
    },
  },
});
