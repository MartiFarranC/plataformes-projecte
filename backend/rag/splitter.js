const fs = require('fs/promises');
const path = require('path');

function normalizeText(text) {
  return text.replace(/\r\n/g, '\n').replace(/\n{3,}/g, '\n\n').trim();
}

function splitText(text, chunkSize = 500, overlap = 100) {
  if (chunkSize <= 0 || overlap < 0) {
    throw new Error('chunkSize must be > 0 and overlap must be >= 0');
  }

  const normalized = normalizeText(text);
  if (!normalized) {
    return [];
  }

  const paragraphs = normalized
    .split(/\n{2,}/g)
    .map((paragraph) => paragraph.trim())
    .filter(Boolean);

  const chunks = [];
  let current = '';

  for (const paragraph of paragraphs) {
    const candidate = current ? `${current}\n\n${paragraph}` : paragraph;

    if (candidate.length <= chunkSize) {
      current = candidate;
      continue;
    }

    if (current) {
      chunks.push(current);
      current = getOverlap(current, overlap);
      current = current ? `${current}\n\n${paragraph}` : paragraph;
      continue;
    }

    const paragraphChunks = splitOversizedParagraph(paragraph, chunkSize, overlap);
    chunks.push(...paragraphChunks.slice(0, -1));
    current = paragraphChunks[paragraphChunks.length - 1];
  }

  if (current) {
    chunks.push(current);
  }

  return chunks;
}

function splitOversizedParagraph(paragraph, chunkSize, overlap) {
  const words = paragraph.split(/\s+/);
  const chunks = [];
  let start = 0;

  while (start < words.length) {
    const end = Math.min(words.length, start + chunkSize);
    chunks.push(words.slice(start, end).join(' '));
    if (end === words.length) {
      break;
    }
    start = Math.max(0, end - overlap);
  }

  return chunks;
}

function getOverlap(text, overlap) {
  if (!text || overlap <= 0) {
    return '';
  }
  const words = text.split(/\s+/);
  return words.slice(Math.max(0, words.length - overlap)).join(' ');
}

async function listKnowledgeFiles(dir) {
  const entries = await fs.readdir(dir, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await listKnowledgeFiles(fullPath)));
    } else if (entry.isFile() && /\.(md|txt|json|html?)$/i.test(entry.name)) {
      files.push(fullPath);
    }
  }

  return files;
}

async function splitKnowledgeDocuments(dir, options = {}) {
  const { chunkSize = 500, overlap = 100 } = options;
  const files = await listKnowledgeFiles(dir);
  const docs = [];

  for (const file of files) {
    const rawText = await fs.readFile(file, 'utf8');
    const chunks = splitText(rawText, chunkSize, overlap);
    const relativePath = path.relative(dir, file);
    chunks.forEach((chunk, index) => {
      docs.push({
        id: `${relativePath}#${index + 1}`,
        source: relativePath,
        text: chunk
      });
    });
  }

  return docs;
}

module.exports = {
  normalizeText,
  splitText,
  splitKnowledgeDocuments,
  listKnowledgeFiles
};
