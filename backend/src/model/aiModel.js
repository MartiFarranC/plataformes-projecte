class AiModel {
  constructor({
    universityAiUrl,
    universityAiToken,
    ollamaModel = 'llama3.2:3b',
    timeoutMs = 120000,
    maxRetries = 2
  }) {
    this.universityAiUrl = universityAiUrl;
    this.universityAiToken = universityAiToken;
    this.ollamaModel = ollamaModel;
    this.timeoutMs = timeoutMs;
    this.maxRetries = maxRetries;
  }

  async ask(question) {
    if (!this.universityAiUrl) {
      throw new Error('UNIVERSITY_AI_URL not configured');
    }

    const headers = { 'Content-Type': 'application/json' };
    if (this.universityAiToken) {
      headers.Authorization = `Bearer ${this.universityAiToken}`;
    }

    const isOllamaGenerate = this.universityAiUrl.includes('/api/generate');
    const payload = isOllamaGenerate
      ? { model: this.ollamaModel, prompt: question, stream: false }
      : { question };

    let response;
    let lastError;
    for (let attempt = 1; attempt <= this.maxRetries; attempt += 1) {
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), this.timeoutMs);
      try {
        response = await fetch(this.universityAiUrl, {
          method: 'POST',
          headers,
          body: JSON.stringify(payload),
          signal: controller.signal
        });
        lastError = null;
        break;
      } catch (error) {
        if (error.name === 'AbortError') {
          lastError = new Error(`AI timeout after ${this.timeoutMs} ms`);
        } else {
          lastError = new Error(`AI connection failed: ${error.message}`);
        }
        if (attempt < this.maxRetries) {
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
}

module.exports = { AiModel };
