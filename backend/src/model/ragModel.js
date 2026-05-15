const path = require('path');
const { splitText } = require('../../rag/splitter');
const {
  buildRagIndex,
  getRagContext,
  getRagIndexCount,
  isQueryRelevantToRag,
  getEmbeddingsCachePath
} = require('../../rag/store');

class RagModel {
  constructor(knowledgeDir) {
    this.knowledgeDir = knowledgeDir;
  }

  split(text, chunkSize = 500, overlap = 100) {
    const chunks = splitText(text, chunkSize, overlap);
    return { count: chunks.length, chunkSize, overlap, chunks };
  }

  async load(chunkSize = 500, overlap = 100) {
    const loaded = await buildRagIndex(this.knowledgeDir, { chunkSize, overlap });
    return { count: loaded.length, chunkSize, overlap, preview: loaded.slice(0, 20) };
  }

  async getQueryState(question, topK = 5) {
    const isRelevant = await isQueryRelevantToRag(question, topK);
    const context = await getRagContext(question, topK);
    return { isRelevant, context };
  }

  getStatus() {
    const count = getRagIndexCount();
    return { loaded: count > 0, count };
  }

  getCount() {
    return getRagIndexCount();
  }

  getEmbeddingsCachePath() {
    return getEmbeddingsCachePath();
  }

  static knowledgePathFromBackendDir(backendDir) {
    return path.join(backendDir, 'knowledge');
  }
}

module.exports = { RagModel };
