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

  async postChat(body) {
    try {
      const question = (body?.question || '').trim();
      if (!question) {
        return { status: 400, body: { error: 'Question is required' } };
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

      return { status: 200, body: { id: messageId, answer } };
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
