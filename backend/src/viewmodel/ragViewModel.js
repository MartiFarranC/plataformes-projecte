class RagViewModel {
  constructor({
    ragModel,
    aiModel,
    outOfContextMessage,
    appOverviewMessage,
    aiConnectivityErrorMessage,
    getGeneralScopedReply
  }) {
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

  async split(body) {
    try {
      const text = (body?.text || '').trim();
      if (!text) {
        return { status: 400, body: { error: 'Text is required' } };
      }

      const chunkSize = Number(body.chunkSize || 500);
      const overlap = Number(body.overlap || 100);
      return { status: 200, body: this.ragModel.split(text, chunkSize, overlap) };
    } catch (error) {
      return { status: 500, body: { error: error.message } };
    }
  }

  async load(body) {
    try {
      const chunkSize = Number(body.chunkSize || 500);
      const overlap = Number(body.overlap || 100);
      return { status: 200, body: await this.ragModel.load(chunkSize, overlap) };
    } catch (error) {
      return { status: 500, body: { error: error.message } };
    }
  }

  async query(body) {
    try {
      const question = (body?.question || '').trim();
      if (!question) {
        return { status: 400, body: { error: 'Question is required' } };
      }

      const topK = Number(body.topK || 5);
      const generalReply = this.getGeneralScopedReply(question, this.appOverviewMessage);
      if (generalReply) {
        return {
          status: 200,
          body: { question, answer: generalReply, context: '', count: this.ragModel.getCount() }
        };
      }

      const { isRelevant, context } = await this.ragModel.getQueryState(question, topK);
      if (!context || !isRelevant) {
        return {
          status: 200,
          body: { question, answer: this.outOfContextMessage, context: '', count: this.ragModel.getCount() }
        };
      }

      const answer = await this.aiModel.ask(this.buildPrompt(question, context));
      return { status: 200, body: { question, answer, context, count: this.ragModel.getCount() } };
    } catch (error) {
      if (this.isAiConnectivityError(error)) {
        return { status: 503, body: { error: this.aiConnectivityErrorMessage } };
      }
      return { status: 500, body: { error: error.message } };
    }
  }

  async check(body) {
    const question = (body?.question || '').trim();
    if (!question) {
      return { status: 400, body: { error: 'Question is required' } };
    }

    const topK = Number(body.topK || 5);
    const { isRelevant, context } = await this.ragModel.getQueryState(question, topK);

    return {
      status: 200,
      body: {
        question,
        isRelevant,
        hasContext: Boolean(context),
        contextPreview: context.slice(0, 400),
        count: this.ragModel.getCount()
      }
    };
  }

  status() {
    return { status: 200, body: this.ragModel.getStatus() };
  }
}

module.exports = { RagViewModel };
