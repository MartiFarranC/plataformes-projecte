const express = require('express');
const { open } = require('sqlite');
const sqlite3 = require('sqlite3');
const dotenv = require('dotenv');
const path = require('path');
const { splitText } = require('./rag/splitter');
const { buildRagIndex, getRagContext, getRagIndexCount, isQueryRelevantToRag, getEmbeddingsCachePath } = require('./rag/store');

dotenv.config();

const app = express();
app.use(express.json());
app.use((_req, res, next) => {
  res.setHeader('X-PosturAI-Backend', 'true');
  next();
});

const PORT = Number(process.env.PORT || 3002);
const UNIVERSITY_AI_URL = process.env.UNIVERSITY_AI_URL;
const UNIVERSITY_AI_TOKEN = process.env.UNIVERSITY_AI_TOKEN;
const OLLAMA_MODEL = process.env.OLLAMA_MODEL || 'llama3.2:3b';
const AI_TIMEOUT_MS = Number(process.env.AI_TIMEOUT_MS || 120000);
const AI_MAX_RETRIES = Number(process.env.AI_MAX_RETRIES || 2);
const OUT_OF_CONTEXT_MESSAGE = 'Perdona però no puc parlar de res que no sigui el meu context.';
const APP_OVERVIEW_MESSAGE = 'Soc l’assistent de PosturAI. Puc ajudar-te amb postura, ergonomia, dolor cervical/lumbar, hàbits saludables a l’escriptori i exercicis relacionats.';

let db;

async function initDb() {
  db = await open({
    filename: './chat.db',
    driver: sqlite3.Database
  });

  await db.exec(`
    CREATE TABLE IF NOT EXISTS chat_messages (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      question TEXT NOT NULL,
      answer TEXT,
      status TEXT NOT NULL,
      created_at TEXT NOT NULL,
      answered_at TEXT
    )
  `);
}

async function askUniversityAI(question) {
  if (!UNIVERSITY_AI_URL) {
    throw new Error('UNIVERSITY_AI_URL not configured');
  }

  const headers = { 'Content-Type': 'application/json' };
  if (UNIVERSITY_AI_TOKEN) {
    headers.Authorization = `Bearer ${UNIVERSITY_AI_TOKEN}`;
  }

  const isOllamaGenerate = UNIVERSITY_AI_URL.includes('/api/generate');
  const payload = isOllamaGenerate
    ? { model: OLLAMA_MODEL, prompt: question, stream: false }
    : { question };

  let response;
  let lastError;
  for (let attempt = 1; attempt <= AI_MAX_RETRIES; attempt += 1) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), AI_TIMEOUT_MS);
    try {
      response = await fetch(UNIVERSITY_AI_URL, {
        method: 'POST',
        headers,
        body: JSON.stringify(payload),
        signal: controller.signal
      });
      lastError = null;
      break;
    } catch (error) {
      if (error.name === 'AbortError') {
        lastError = new Error(`AI timeout after ${AI_TIMEOUT_MS} ms`);
      } else {
        lastError = new Error(`AI connection failed: ${error.message}`);
      }
      if (attempt < AI_MAX_RETRIES) {
        await new Promise((resolve) => setTimeout(resolve, 1000));
      }
    } finally {
      clearTimeout(timeout);
    }
  }

  if (!response) {
    throw lastError || new Error('AI request failed');
  }

  if (!response.ok) {
    const txt = await response.text();
    throw new Error(`AI error ${response.status}: ${txt}`);
  }

  const data = await response.json();
  return data.answer || data.response || JSON.stringify(data);
}

function normalizeText(text) {
  return (text || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim();
}

function getGeneralScopedReply(question) {
  const q = normalizeText(question);
  if (!q) {
    return null;
  }

  const greetingPatterns = [/^hola\b/, /^bon dia\b/, /^bona tarda\b/, /^bona nit\b/, /^hey\b/, /^ei\b/];
  if (greetingPatterns.some((pattern) => pattern.test(q))) {
    return 'Hola! Soc l’assistent de PosturAI. Si vols, et puc ajudar amb postura, ergonomia o dolor d’esquena/cervicals.';
  }

  const appScopePatterns = [
    /sobre que (pots|puc) parlar/,
    /de que (pots|puc) parlar/,
    /de que va aquesta app/,
    /que fas/,
    /com em pots ajudar/,
    /quina ajuda em pots donar/,
    /que es posturai/
  ];
  if (appScopePatterns.some((pattern) => pattern.test(q))) {
    return APP_OVERVIEW_MESSAGE;
  }

  return null;
}

app.post('/chat', async (req, res) => {
  try {
    const question = (req.body?.question || '').trim();
    if (!question) {
      return res.status(400).json({ error: 'Question is required' });
    }

    const now = new Date().toISOString();
    const insert = await db.run(
      `INSERT INTO chat_messages (question, status, created_at) VALUES (?, 'pending', ?)`,
      question,
      now
    );

    const messageId = insert.lastID;
    const generalReply = getGeneralScopedReply(question);
    if (generalReply) {
      await db.run(
        `UPDATE chat_messages SET answer = ?, status = 'answered', answered_at = ? WHERE id = ?`,
        generalReply,
        new Date().toISOString(),
        messageId
      );
      return res.json({ id: messageId, answer: generalReply });
    }

    const topK = 5;
    const isRelevant = await isQueryRelevantToRag(question, topK);
    const context = await getRagContext(question, topK);

    let answer;
    try {
      if (!context || !isRelevant) {
        answer = OUT_OF_CONTEXT_MESSAGE;
      } else {
        const prompt = `Utilitza NOMÉS la informació del CONTEXT per respondre. No afegeixis coneixement extern ni responguis a preguntes fora del context. Si la pregunta no està relacionada amb el CONTEXT, respon exactament: "${OUT_OF_CONTEXT_MESSAGE}"\n\nCONTEXT:\n${context}\n\nPREGUNTA:\n${question}`;
        answer = await askUniversityAI(prompt);
      }

      await db.run(
        `UPDATE chat_messages SET answer = ?, status = 'answered', answered_at = ? WHERE id = ?`,
        answer,
        new Date().toISOString(),
        messageId
      );
    } catch (aiError) {
      await db.run(
        `UPDATE chat_messages SET answer = ?, status = 'error', answered_at = ? WHERE id = ?`,
        aiError.message,
        new Date().toISOString(),
        messageId
      );
      throw aiError;
    }

    return res.json({ id: messageId, answer });
  } catch (error) {
    const isAiConnectivityError = error.message.startsWith('AI connection failed:') || error.message.startsWith('AI timeout after');
    if (isAiConnectivityError) {
      return res.status(503).json({ error: 'No he pogut connectar amb el servei d’IA. Revisa VPN + túnel SSH.' });
    }
    return res.status(500).json({ error: error.message });
  }
});

app.get('/chat/history', async (_req, res) => {
  const rows = await db.all(
    `SELECT id, question, answer, status, created_at, answered_at
     FROM chat_messages
     ORDER BY id DESC
     LIMIT 100`
  );
  res.json(rows);
});

app.post('/rag/split', async (req, res) => {
  try {
    const text = (req.body?.text || '').trim();
    if (!text) {
      return res.status(400).json({ error: 'Text is required' });
    }

    const chunkSize = Number(req.body.chunkSize || 500);
    const overlap = Number(req.body.overlap || 100);
    const chunks = splitText(text, chunkSize, overlap);

    return res.json({ count: chunks.length, chunkSize, overlap, chunks });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

app.post('/rag/load', async (req, res) => {
  try {
    const chunkSize = Number(req.body.chunkSize || 500);
    const overlap = Number(req.body.overlap || 100);
    const loaded = await buildRagIndex(path.join(__dirname, 'knowledge'), { chunkSize, overlap });

    return res.json({ count: loaded.length, chunkSize, overlap, preview: loaded.slice(0, 20) });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

app.post('/rag/query', async (req, res) => {
  try {
    const question = (req.body?.question || '').trim();
    if (!question) {
      return res.status(400).json({ error: 'Question is required' });
    }

    const topK = Number(req.body.topK || 5);
    const generalReply = getGeneralScopedReply(question);
    if (generalReply) {
      return res.json({
        question,
        answer: generalReply,
        context: '',
        count: getRagIndexCount()
      });
    }

    const isRelevant = await isQueryRelevantToRag(question, topK);
    const context = await getRagContext(question, topK);

    if (!context || !isRelevant) {
      return res.json({
        question,
        answer: OUT_OF_CONTEXT_MESSAGE,
        context: '',
        count: getRagIndexCount()
      });
    }

    const prompt = `Utilitza NOMÉS la informació del CONTEXT per respondre. No afegeixis coneixement extern ni responguis a preguntes fora del context. Si la pregunta no està relacionada amb el CONTEXT, respon exactament: "${OUT_OF_CONTEXT_MESSAGE}"\n\nCONTEXT:\n${context}\n\nPREGUNTA:\n${question}`;

    const answer = await askUniversityAI(prompt);

    return res.json({ question, answer, context, count: getRagIndexCount() });
  } catch (error) {
    const isAiConnectivityError = error.message.startsWith('AI connection failed:') || error.message.startsWith('AI timeout after');
    if (isAiConnectivityError) {
      return res.status(503).json({ error: 'No he pogut connectar amb el servei d’IA. Revisa VPN + túnel SSH.' });
    }
    return res.status(500).json({ error: error.message });
  }
});

app.get('/rag/status', (_req, res) => {
  res.json({
    loaded: getRagIndexCount() > 0,
    count: getRagIndexCount()
  });
});

app.post('/rag/check', async (req, res) => {
  const question = (req.body?.question || '').trim();
  if (!question) {
    return res.status(400).json({ error: 'Question is required' });
  }

  const topK = Number(req.body.topK || 5);
  const isRelevant = await isQueryRelevantToRag(question, topK);
  const context = await getRagContext(question, topK);

  return res.json({
    question,
    isRelevant,
    hasContext: Boolean(context),
    contextPreview: context.slice(0, 400),
    count: getRagIndexCount()
  });
});

app.get('/health', (_req, res) => {
  res.json({
    ok: true,
    service: 'posturai-backend',
    port: PORT,
    aiUrl: UNIVERSITY_AI_URL,
    model: OLLAMA_MODEL,
    ragIndexCount: getRagIndexCount(),
    embeddingsCachePath: getEmbeddingsCachePath()
  });
});

initDb()
  .then(async () => {
    try {
      await buildRagIndex(path.join(__dirname, 'knowledge'), { chunkSize: 500, overlap: 100 });
      console.log(`RAG index loaded at startup with ${getRagIndexCount()} chunks.`);
    } catch (ragError) {
      console.warn(`RAG startup load failed: ${ragError.message}`);
    }

    app.listen(PORT, () => {
      console.log(`Backend listening on http://localhost:${PORT}`);
    });
  })
  .catch((e) => {
    console.error('DB init error:', e);
    process.exit(1);
  });
