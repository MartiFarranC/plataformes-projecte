const express = require('express');
const { open } = require('sqlite');
const sqlite3 = require('sqlite3');
const dotenv = require('dotenv');

dotenv.config();

const app = express();
app.use(express.json());

//const PORT = process.env.PORT || 3000;
const PORT = 3002;
const UNIVERSITY_AI_URL = process.env.UNIVERSITY_AI_URL;
const UNIVERSITY_AI_TOKEN = process.env.UNIVERSITY_AI_TOKEN;
const OLLAMA_MODEL = process.env.OLLAMA_MODEL || 'llama3.2:3b';

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

  const response = await fetch(UNIVERSITY_AI_URL, {
    method: 'POST',
    headers,
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    const txt = await response.text();
    throw new Error(`AI error ${response.status}: ${txt}`);
  }

  const data = await response.json();
  return data.answer || data.response || JSON.stringify(data);
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

    let answer;
    try {
      answer = await askUniversityAI(question);
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

app.get('/health', (_req, res) => {
  res.json({ ok: true, port: PORT, aiUrl: UNIVERSITY_AI_URL, model: OLLAMA_MODEL });
});

initDb()
  .then(() => {
    app.listen(PORT, () => {
      console.log(`Backend listening on http://localhost:${PORT}`);
    });
  })
  .catch((e) => {
    console.error('DB init error:', e);
    process.exit(1);
  });
