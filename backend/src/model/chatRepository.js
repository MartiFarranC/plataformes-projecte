const { open } = require('sqlite');
const sqlite3 = require('sqlite3');

class ChatRepository {
  constructor(filename = './chat.db') {
    this.filename = filename;
    this.db = null;
  }

  async init() {
    this.db = await open({
      filename: this.filename,
      driver: sqlite3.Database
    });

    await this.db.exec(`
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

  async insertPending(question) {
    const now = new Date().toISOString();
    const insert = await this.db.run(
      `INSERT INTO chat_messages (question, status, created_at) VALUES (?, 'pending', ?)`,
      question,
      now
    );
    return insert.lastID;
  }

  async markAnswered(id, answer) {
    await this.db.run(
      `UPDATE chat_messages SET answer = ?, status = 'answered', answered_at = ? WHERE id = ?`,
      answer,
      new Date().toISOString(),
      id
    );
  }

  async markError(id, errorMessage) {
    await this.db.run(
      `UPDATE chat_messages SET answer = ?, status = 'error', answered_at = ? WHERE id = ?`,
      errorMessage,
      new Date().toISOString(),
      id
    );
  }

  async getHistory(limit = 100) {
    return this.db.all(
      `SELECT id, question, answer, status, created_at, answered_at
       FROM chat_messages
       ORDER BY id DESC
       LIMIT ?`,
      limit
    );
  }
}

module.exports = { ChatRepository };
