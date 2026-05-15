class HealthViewModel {
  constructor({ port, universityAiUrl, ollamaModel, ragModel, serviceName = 'posturai-backend' }) {
    this.port = port;
    this.universityAiUrl = universityAiUrl;
    this.ollamaModel = ollamaModel;
    this.ragModel = ragModel;
    this.serviceName = serviceName;
  }

  status() {
    return {
      status: 200,
      body: {
        ok: true,
        service: this.serviceName,
        port: this.port,
        aiUrl: this.universityAiUrl,
        model: this.ollamaModel,
        ragIndexCount: this.ragModel.getCount(),
        embeddingsCachePath: this.ragModel.getEmbeddingsCachePath()
      }
    };
  }
}

module.exports = { HealthViewModel };
