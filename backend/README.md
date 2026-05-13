# Backend chat IA (UdL VPN + SSH tunnel + ngrok)

## 1) Start SSH tunnel to UdL

Keep this terminal open:

```bash
ssh -N -L 11434:localhost:11434 tuneluser@spoofing02-gcd.udl.cat
```

Note: We only forward `11434` to avoid collision with backend port.

## 2) Configure backend

```bash
cd backend
copy .env.example .env
npm install
npm start
```

Default backend port is `3002` (configurable with `PORT` in `.env`).

## 3) Test local backend

```powershell
$body = @{ question = "Hola" } | ConvertTo-Json
Invoke-RestMethod -Method POST -Uri "http://localhost:3002/chat" -ContentType "application/json" -Body $body
```

Health check:

```powershell
Invoke-RestMethod -Method GET -Uri "http://localhost:3002/health"
```

## 4) Expose backend with ngrok

```bash
ngrok http 3002
```

## 5) Android app

Set `apiBaseUrl` in `ChatActivity.kt` with the ngrok HTTPS URL.

## Important notes

- If `/chat` returns `fetch failed`, the SSH tunnel is down or VPN is not connected.
- RAG is auto-loaded at startup from `backend/knowledge/` (you can still call `/rag/load` to reload manually).
- If model responses are slow, increase `AI_TIMEOUT_MS` in `.env` (default `120000`).
- If model name is wrong, change `OLLAMA_MODEL` in `.env`.
- You can list models directly on Ollama API:

```powershell
Invoke-RestMethod -Method GET -Uri "http://127.0.0.1:11434/api/tags"
```
