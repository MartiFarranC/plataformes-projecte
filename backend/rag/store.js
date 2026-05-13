const path = require('path');
const { splitKnowledgeDocuments } = require('./splitter');

let index = [];
const DEFAULT_MIN_SCORE = 2;
const DEFAULT_MIN_MATCH_RATIO = 0.3;
const DEFAULT_MIN_MATCHED_TERMS = 2;
const STOP_WORDS = new Set([
  'a', 'al', 'amb', 'de', 'del', 'dels', 'des', 'el', 'els', 'en', 'es', 'i', 'la', 'las', 'les', 'lo',
  'los', 'o', 'on', 'per', 'que', 'qui', 'quin', 'quina', 'quines', 'quins', 'se', 'si', 'te', 'un',
  'una', 'uns', 'unes', 'y'
]);
const DOMAIN_TERMS = new Set([
  'postura', 'posture', 'ergonomia', 'ergonomic', 'esquena', 'back', 'coll', 'neck',
  'espatlles', 'shoulders', 'columna', 'spine', 'text', 'textneck', 'sitting', 'seure',
  'assegut', 'pantalla', 'screen', 'mobile', 'mòbil', 'phone', 'chin', 'core'
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

function searchRagIndexWithScores(query, topK = 5) {
  if (!query || index.length === 0) {
    return [];
  }

  const normalized = normalizeQuery(query);
  const queryTerms = extractQueryTerms(normalized);
  if (queryTerms.length === 0) {
    return [];
  }

  return index
    .map((doc) => ({
      doc,
      score: scoreDocument(doc, queryTerms)
    }))
    .filter((item) => item.score > 0)
    .sort((a, b) => b.score - a.score || a.doc.id.localeCompare(b.doc.id))
    .slice(0, topK);
}

async function buildRagIndex(dir, options = {}) {
  const loaded = await splitKnowledgeDocuments(dir, options);
  index = loaded;
  return index;
}

function searchRagIndex(query, topK = 5) {
  return searchRagIndexWithScores(query, topK).map((item) => item.doc);
}

function isQueryRelevantToRag(query, topK = 5, options = {}) {
  const normalized = normalizeQuery(query);
  const queryTerms = extractQueryTerms(normalized);
  if (!queryTerms.length || !hasDomainIntent(queryTerms)) {
    return false;
  }

  const scoredHits = searchRagIndexWithScores(query, topK);
  if (!scoredHits.length) {
    return false;
  }

  const minScore = Number(options.minScore || DEFAULT_MIN_SCORE);
  const minMatchRatio = Number(options.minMatchRatio || DEFAULT_MIN_MATCH_RATIO);
  const minMatchedTerms = Number(options.minMatchedTerms || DEFAULT_MIN_MATCHED_TERMS);
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

  return bestScore >= minScore && matchRatio >= minMatchRatio && matchedTerms.size >= minMatchedTerms;
}

function getRagContext(query, topK = 5, maxTokens = 1500) {
  const hits = searchRagIndex(query, topK);
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

module.exports = {
  buildRagIndex,
  searchRagIndex,
  getRagContext,
  getRagIndexCount,
  isQueryRelevantToRag
};
