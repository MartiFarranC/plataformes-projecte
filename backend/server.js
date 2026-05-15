const path = require('path');
const dotenv = require('dotenv');
const { createApp } = require('./src/app');

dotenv.config();

const PORT = Number(process.env.PORT || 3002);
const UNIVERSITY_AI_URL = process.env.UNIVERSITY_AI_URL;
const UNIVERSITY_AI_TOKEN = process.env.UNIVERSITY_AI_TOKEN;
const OLLAMA_MODEL = process.env.OLLAMA_MODEL || 'llama3.2:3b';
const AI_TIMEOUT_MS = Number(process.env.AI_TIMEOUT_MS || 120000);
const AI_MAX_RETRIES = Number(process.env.AI_MAX_RETRIES || 2);

async function bootstrap() {
  try {
    const { app, ragModel } = await createApp({
      port: PORT,
      backendDir: __dirname,
      universityAiUrl: UNIVERSITY_AI_URL,
      universityAiToken: UNIVERSITY_AI_TOKEN,
      ollamaModel: OLLAMA_MODEL,
      aiTimeoutMs: AI_TIMEOUT_MS,
      aiMaxRetries: AI_MAX_RETRIES
    });

    try {
      await ragModel.load(500, 100);
      console.log(`RAG index loaded at startup with ${ragModel.getCount()} chunks.`);
    } catch (ragError) {
      console.warn(`RAG startup load failed: ${ragError.message}`);
    }

    app.listen(PORT, () => {
      console.log(`Backend listening on http://localhost:${PORT}`);
    });
  } catch (error) {
    console.error('Backend init error:', error);
    process.exit(1);
  }
}

bootstrap();
