const express = require('express');
const path = require('path');
const { ChatRepository } = require('./model/chatRepository');
const { AiModel } = require('./model/aiModel');
const { RagModel } = require('./model/ragModel');
const { ChatViewModel } = require('./viewmodel/chatViewModel');
const { RagViewModel } = require('./viewmodel/ragViewModel');
const { HealthViewModel } = require('./viewmodel/healthViewModel');
const { mountRoutes } = require('./view/routes');
const {
  OUT_OF_CONTEXT_MESSAGE,
  APP_OVERVIEW_MESSAGE,
  AI_CONNECTIVITY_ERROR_MESSAGE
} = require('./config/constants');
const { getGeneralScopedReply } = require('./utils/text');

async function createApp(config) {
  const app = express();
  app.use(express.json());
  app.use((_req, res, next) => {
    res.setHeader('X-PosturAI-Backend', 'true');
    next();
  });

  const chatRepository = new ChatRepository(path.join(config.backendDir, 'chat.db'));
  await chatRepository.init();

  const ragModel = new RagModel(RagModel.knowledgePathFromBackendDir(config.backendDir));
  const aiModel = new AiModel({
    universityAiUrl: config.universityAiUrl,
    universityAiToken: config.universityAiToken,
    ollamaModel: config.ollamaModel,
    timeoutMs: config.aiTimeoutMs,
    maxRetries: config.aiMaxRetries
  });

  const shared = {
    ragModel,
    aiModel,
    outOfContextMessage: OUT_OF_CONTEXT_MESSAGE,
    appOverviewMessage: APP_OVERVIEW_MESSAGE,
    aiConnectivityErrorMessage: AI_CONNECTIVITY_ERROR_MESSAGE,
    getGeneralScopedReply
  };

  const chatViewModel = new ChatViewModel({
    chatRepository,
    ...shared
  });
  const ragViewModel = new RagViewModel(shared);
  const healthViewModel = new HealthViewModel({
    port: config.port,
    universityAiUrl: config.universityAiUrl,
    ollamaModel: config.ollamaModel,
    ragModel
  });

  mountRoutes(app, { chatViewModel, ragViewModel, healthViewModel });

  return {
    app,
    ragModel
  };
}

module.exports = { createApp };
