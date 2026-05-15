const fs = require('fs/promises');
const path = require('path');

async function main() {
  const repoRoot = path.resolve(__dirname, '..', '..');
  const inputPath = path.join(repoRoot, 'backend', 'knowledge', '.rag-embeddings-cache.json');
  const outputDir = path.join(repoRoot, 'backend', 'knowledge');
  const vectorsPath = path.join(outputDir, 'vectors.tsv');
  const metadataPath = path.join(outputDir, 'metadata.tsv');

  const raw = await fs.readFile(inputPath, 'utf8');
  const payload = JSON.parse(raw);
  const documents = Array.isArray(payload.documents) ? payload.documents : [];

  if (!documents.length) {
    throw new Error('No hi ha documents al fitxer de cache.');
  }

  const vectorLines = [];
  const metadataLines = ['id\tsource\ttextHash'];

  for (const doc of documents) {
    const embedding = Array.isArray(doc.embedding) ? doc.embedding : null;
    if (!embedding || !embedding.length) {
      continue;
    }

    vectorLines.push(embedding.join('\t'));
    metadataLines.push([
      doc.id || '',
      doc.source || '',
      doc.textHash || ''
    ].join('\t'));
  }

  await fs.writeFile(vectorsPath, vectorLines.join('\n') + '\n', 'utf8');
  await fs.writeFile(metadataPath, metadataLines.join('\n') + '\n', 'utf8');

  console.log(`Creat: ${vectorsPath}`);
  console.log(`Creat: ${metadataPath}`);
  console.log(`Total vectors: ${vectorLines.length}`);
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
