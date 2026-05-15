const path = require('path');
const fs = require('fs/promises');
const crypto = require('crypto');
const { splitKnowledgeDocuments } = require('./splitter');

let index = [];
let cacheFilePath = '';
const DEFAULT_MIN_SCORE = 0.2;
const DEFAULT_MIN_MATCH_RATIO = 0.3;
const DEFAULT_MIN_MATCHED_TERMS = 2;
const DEFAULT_SEMANTIC_FALLBACK_MIN_SCORE = 0.35;
const DEFAULT_SEMANTIC_INTENT_MIN_SCORE = 0.22;
const EMBEDDINGS_URL = process.env.EMBEDDINGS_URL || 'http://127.0.0.1:11435/api/embeddings';
const EMBEDDINGS_MODEL = process.env.EMBEDDINGS_MODEL || 'nomic-embed-text';
const EMBEDDINGS_TIMEOUT_MS = Number(process.env.EMBEDDINGS_TIMEOUT_MS || 30000);
const EMBEDDINGS_ENABLED = (process.env.EMBEDDINGS_ENABLED || 'true').toLowerCase() !== 'false';
const EMBEDDINGS_CONCURRENCY = Number(process.env.EMBEDDINGS_CONCURRENCY || 4);
const EMBEDDINGS_CACHE_FILE = process.env.EMBEDDINGS_CACHE_FILE || '.rag-embeddings-cache.json';
const STOP_WORDS = new Set([
  'a', 'al', 'amb', 'de', 'del', 'dels', 'des', 'el', 'els', 'en', 'es', 'i', 'la', 'las', 'les', 'lo',
  'los', 'o', 'on', 'per', 'que', 'qui', 'quin', 'quina', 'quines', 'quins', 'se', 'si', 'te', 'un',
  'una', 'uns', 'unes', 'y'
]);
const DOMAIN_TERMS = new Set([
  'postura', 'posture', 'ergonomia', 'ergonomic', 'esquena', 'back', 'coll', 'neck',
  'espatlles', 'shoulders', 'columna', 'spine', 'text', 'textneck', 'sitting', 'seure',
  'assegut', 'pantalla', 'screen', 'mobile', 'mobil', 'mòbil', 'phone', 'chin', 'core',
  'cadira', 'cervicals', 'dolor', 'lumbars', 'llumbar', 'escriptori'
]);

function normalizeQuery(query) {
  return query
    .toLowerCase()
    .replace(/[\W_]+/g, ' ')
    .trim();
}

function tokenize(text) {
  return text
    .toLowerCase()
    .replace(/[\W_]+/g, ' ')
    .split(/\s+/)
    .filter(Boolean);
}

function extractQueryTerms(query) {
  return tokenize(query).filter((term) => term.length >= 3 && !STOP_WORDS.has(term));
}

function hasDomainIntent(queryTerms) {
  return queryTerms.some((term) => DOMAIN_TERMS.has(term));
}

function scoreDocument(doc, queryTerms) {
  const tokens = tokenize(doc.text);
  const counts = tokens.reduce((acc, token) => {
    acc[token] = (acc[token] || 0) + 1;
    return acc;
  }, {});

  let score = 0;
  for (const term of queryTerms) {
    if (counts[term]) {
      score += counts[term];
    }
  }

  return score;
}

function hashText(text) {
  return crypto.createHash('sha256').update(text || '').digest('hex');
}

function resolveCacheFile(dir) {
  return path.join(dir, EMBEDDINGS_CACHE_FILE);
}

async function loadEmbeddingsCache(cachePath) {
  try {
    const raw = await fs.readFile(cachePath, 'utf8');
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed?.documents)) {
      return new Map();
    }

    return new Map(
      parsed.documents
        .filter((item) => item?.id && item?.textHash && Array.isArray(item?.embedding))
        .map((item) => [`${item.id}:${item.textHash}`, item.embedding])
    );
  } catch (_error) {
    return new Map();
  }
}

async function saveEmbeddingsCache(cachePath, docs) {
  const payload = {
    version: 1,
    generatedAt: new Date().toISOString(),
    documents: docs
      .filter((doc) => Array.isArray(doc.embedding) && doc.embedding.length > 0)
      .map((doc) => ({
        id: doc.id,
        source: doc.source,
        textHash: hashText(doc.text),
        embedding: doc.embedding
      }))
  };

  await fs.writeFile(cachePath, JSON.stringify(payload), 'utf8');
}

function dot(a, b) {
  let total = 0;
  for (let i = 0; i < a.length; i += 1) {
    total += a[i] * b[i];
  }
  return total;
}

function norm(vector) {
  return Math.sqrt(dot(vector, vector));
}

function cosineSimilarity(a, b) {
  if (!Array.isArray(a) || !Array.isArray(b) || !a.length || !b.length || a.length !== b.length) {
    return 0;
  }
  const magnitude = norm(a) * norm(b);
  if (!magnitude) {
    return 0;
  }
  return dot(a, b) / magnitude;
}

async function fetchEmbedding(text) {
  if (!EMBEDDINGS_ENABLED || !text?.trim()) {
    return null;
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), EMBEDDINGS_TIMEOUT_MS);

  try {
    const response = await fetch(EMBEDDINGS_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ model: EMBEDDINGS_MODEL, prompt: text }),
      signal: controller.signal
    });

    if (!response.ok) {
      return null;
    }

    const data = await response.json();
    const vector = data.embedding || data.data?.[0]?.embedding || null;
    if (!Array.isArray(vector) || !vector.length) {
      return null;
    }

    return vector.map((value) => Number(value) || 0);
  } catch (_error) {
    return null;
  } finally {
    clearTimeout(timeout);
  }
}

async function mapWithConcurrency(items, concurrency, mapper) {
  const safeConcurrency = Math.max(1, concurrency);
  const results = new Array(items.length);
  let cursor = 0;

  async function worker() {
    while (cursor < items.length) {
      const current = cursor;
      cursor += 1;
      results[current] = await mapper(items[current], current);
    }
  }

  await Promise.all(Array.from({ length: Math.min(safeConcurrency, items.length) }, () => worker()));
  return results;
}

async function searchRagIndexWithScores(query, topK = 5) {
  if (!query || index.length === 0) {
    return [];
  }

  const normalized = normalizeQuery(query);
  const queryTerms = extractQueryTerms(normalized);
  if (queryTerms.length === 0) {
    return [];
  }

  const queryEmbedding = await fetchEmbedding(normalized);
  if (queryEmbedding) {
    return index
      .filter((doc) => Array.isArray(doc.embedding) && doc.embedding.length === queryEmbedding.length)
      .map((doc) => ({
        doc,
        score: cosineSimilarity(doc.embedding, queryEmbedding)
      }))
      .filter((item) => item.score > 0)
      .sort((a, b) => b.score - a.score || a.doc.id.localeCompare(b.doc.id))
      .slice(0, topK);
  }

  return index
    .map((doc) => ({ doc, score: scoreDocument(doc, queryTerms) }))
    .filter((item) => item.score > 0)
    .sort((a, b) => b.score - a.score || a.doc.id.localeCompare(b.doc.id))
    .slice(0, topK);
}

async function buildRagIndex(dir, options = {}) {
  const loaded = await splitKnowledgeDocuments(dir, options);
  cacheFilePath = resolveCacheFile(dir);
  const cache = await loadEmbeddingsCache(cacheFilePath);

  const docsWithEmbeddings = await mapWithConcurrency(
    loaded,
    EMBEDDINGS_CONCURRENCY,
    async (doc) => {
      const textHash = hashText(doc.text);
      const cacheKey = `${doc.id}:${textHash}`;
      const cachedEmbedding = cache.get(cacheKey);
      if (cachedEmbedding) {
        return {
          ...doc,
          embedding: cachedEmbedding
        };
      }

      return {
        ...doc,
        embedding: await fetchEmbedding(doc.text)
      };
    }
  );

  index = docsWithEmbeddings;
  await saveEmbeddingsCache(cacheFilePath, docsWithEmbeddings);
  return index;
}

async function searchRagIndex(query, topK = 5) {
  const scored = await searchRagIndexWithScores(query, topK);
  return scored.map((item) => item.doc);
}

async function isQueryRelevantToRag(query, topK = 5, options = {}) {
  const normalized = normalizeQuery(query);
  const queryTerms = extractQueryTerms(normalized);
  if (!queryTerms.length) {
    return false;
  }

  const scoredHits = await searchRagIndexWithScores(query, topK);
  if (!scoredHits.length) {
    return false;
  }

  const minScore = Number(options.minScore || DEFAULT_MIN_SCORE);
  const minMatchRatio = Number(options.minMatchRatio || DEFAULT_MIN_MATCH_RATIO);
  const minMatchedTerms = Number(options.minMatchedTerms || DEFAULT_MIN_MATCHED_TERMS);
  const semanticFallbackMinScore = Number(
    options.semanticFallbackMinScore || DEFAULT_SEMANTIC_FALLBACK_MIN_SCORE
  );
  const semanticIntentMinScore = Number(
    options.semanticIntentMinScore || DEFAULT_SEMANTIC_INTENT_MIN_SCORE
  );
  const hasIntent = hasDomainIntent(queryTerms);
  const uniqueTerms = new Set(queryTerms);
  const matchedTerms = new Set();

  for (const item of scoredHits) {
    const docTokens = new Set(tokenize(item.doc.text));
    for (const term of uniqueTerms) {
      if (docTokens.has(term)) {
        matchedTerms.add(term);
      }
    }
  }

  const bestScore = scoredHits[0].score;
  const matchRatio = uniqueTerms.size > 0 ? matchedTerms.size / uniqueTerms.size : 0;
  const looksLikeEmbeddingScore = bestScore >= -1 && bestScore <= 1;

  // If we have embeddings and semantic similarity is clearly good, allow the query.
  // This avoids false negatives caused by strict literal term matching.
  if (looksLikeEmbeddingScore && bestScore >= semanticIntentMinScore) {
    return true;
  }

  if (!hasIntent && looksLikeEmbeddingScore && bestScore >= semanticFallbackMinScore) {
    return true;
  }

  if (!hasIntent) {
    return false;
  }

  return bestScore >= minScore && matchRatio >= minMatchRatio && matchedTerms.size >= minMatchedTerms;
}

async function getRagContext(query, topK = 5, maxTokens = 1500) {
  const hits = await searchRagIndex(query, topK);
  if (!hits.length) {
    return '';
  }

  let context = '';
  for (const hit of hits) {
    const candidate = `${hit.text}\n\n[Source: ${hit.source}]\n\n`;
    if (context.length + candidate.length > maxTokens && context) {
      break;
    }
    context += candidate;
  }

  return context.trim();
}

function getRagIndexCount() {
  return index.length;
}

function getEmbeddingsCachePath() {
  return cacheFilePath;
}

module.exports = {
  buildRagIndex,
  searchRagIndex,
  getRagContext,
  getRagIndexCount,
  isQueryRelevantToRag,
  getEmbeddingsCachePath
};


