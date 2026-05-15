class ChatViewModel {
  constructor({
    chatRepository,
    ragModel,
    aiModel,
    outOfContextMessage,
    appOverviewMessage,
    aiConnectivityErrorMessage,
    getGeneralScopedReply
  }) {
    this.chatRepository = chatRepository;
    this.ragModel = ragModel;
    this.aiModel = aiModel;
    this.outOfContextMessage = outOfContextMessage;
    this.appOverviewMessage = appOverviewMessage;
    this.aiConnectivityErrorMessage = aiConnectivityErrorMessage;
    this.getGeneralScopedReply = getGeneralScopedReply;
  }

  buildPrompt(question, context) {
    return `Utilitza NOMÉS la informació del CONTEXT per respondre. No afegeixis coneixement extern ni responguis a preguntes fora del context. Si la pregunta no està relacionada amb el CONTEXT, respon exactament: "${this.outOfContextMessage}"\n\nCONTEXT:\n${context}\n\nPREGUNTA:\n${question}`;
  }

  isAiConnectivityError(error) {
    return error.message.startsWith('AI connection failed:') || error.message.startsWith('AI timeout after');
  }

  extractQuestion(body) {
    const directQuestion = typeof body?.question === 'string' ? body.question : '';
    const nestedQuestion = typeof body?.message?.text === 'string' ? body.message.text : '';
    return (directQuestion || nestedQuestion || '').trim();
  }

  validateEnvelope(body) {
    if (!body || typeof body !== 'object') {
      return 'JSON body is required';
    }

    if (body.requestId && typeof body.requestId !== 'string') {
      return 'requestId must be a string';
    }

    if (body.timestamp && typeof body.timestamp !== 'string' && typeof body.timestamp !== 'number') {
      return 'timestamp must be an ISO string or epoch number';
    }

    if (body.client && typeof body.client !== 'object') {
      return 'client must be an object';
    }

    if (body.metadata && typeof body.metadata !== 'object') {
      return 'metadata must be an object';
    }

    if (body.message && typeof body.message !== 'object') {
      return 'message must be an object';
    }

    return null;
  }

  async postChat(body) {
    try {
      const envelopeError = this.validateEnvelope(body);
      if (envelopeError) {
        return { status: 400, body: { error: envelopeError } };
      }

      const question = this.extractQuestion(body);
      if (!question) {
        return { status: 400, body: { error: 'Question is required (question or message.text)' } };
      }

      const messageId = await this.chatRepository.insertPending(question);
      const generalReply = this.getGeneralScopedReply(question, this.appOverviewMessage);

      if (generalReply) {
        await this.chatRepository.markAnswered(messageId, generalReply);
        return { status: 200, body: { id: messageId, answer: generalReply } };
      }

      const topK = 5;
      const { isRelevant, context } = await this.ragModel.getQueryState(question, topK);

      let answer;
      try {
        if (!context || !isRelevant) {
          answer = this.outOfContextMessage;
        } else {
          answer = await this.aiModel.ask(this.buildPrompt(question, context));
        }
        await this.chatRepository.markAnswered(messageId, answer);
      } catch (aiError) {
        await this.chatRepository.markError(messageId, aiError.message);
        throw aiError;
      }

      return {
        status: 200,
        body: {
          id: messageId,
          answer,
          requestId: body?.requestId || null,
          timestamp: new Date().toISOString()
        }
      };
    } catch (error) {
      if (this.isAiConnectivityError(error)) {
        return { status: 503, body: { error: this.aiConnectivityErrorMessage } };
      }
      return { status: 500, body: { error: error.message } };
    }
  }

  async getHistory() {
    const rows = await this.chatRepository.getHistory();
    return { status: 200, body: rows };
  }
}

module.exports = { ChatViewModel };
