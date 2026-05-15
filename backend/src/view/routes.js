const express = require('express');

function mountRoutes(app, { chatViewModel, ragViewModel, healthViewModel }) {
  const router = express.Router();

  router.post('/chat', async (req, res) => {
    const result = await chatViewModel.postChat(req.body);
    return res.status(result.status).json(result.body);
  });

  router.get('/chat/history', async (_req, res) => {
    const result = await chatViewModel.getHistory();
    return res.status(result.status).json(result.body);
  });

  router.post('/rag/split', async (req, res) => {
    const result = await ragViewModel.split(req.body);
    return res.status(result.status).json(result.body);
  });

  router.post('/rag/load', async (req, res) => {
    const result = await ragViewModel.load(req.body);
    return res.status(result.status).json(result.body);
  });

  router.post('/rag/query', async (req, res) => {
    const result = await ragViewModel.query(req.body);
    return res.status(result.status).json(result.body);
  });

  router.get('/rag/status', (_req, res) => {
    const result = ragViewModel.status();
    return res.status(result.status).json(result.body);
  });

  router.post('/rag/check', async (req, res) => {
    const result = await ragViewModel.check(req.body);
    return res.status(result.status).json(result.body);
  });

  router.get('/health', (_req, res) => {
    const result = healthViewModel.status();
    return res.status(result.status).json(result.body);
  });

  app.use(router);
}

module.exports = { mountRoutes };
