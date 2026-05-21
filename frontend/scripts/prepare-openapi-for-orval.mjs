import SwaggerParser from '@apidevtools/swagger-parser';
import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');
const openApiSource = resolve(frontendRoot, '../src/main/resources/openapi/openapi.yaml');
const outputPath = resolve(frontendRoot, '.orval/openapi.dereferenced.json');

const openApiDocument = await SwaggerParser.dereference(openApiSource, {
  dereference: {
    circular: 'ignore',
  },
});

await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, JSON.stringify(openApiDocument, null, 2));
