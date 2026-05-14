# Backend PosturAI

Aquest fitxer resumeix només la part backend.
La guia oficial completa end-to-end és a `../README.md`.

## Quick start

```powershell
cd backend
copy .env.example .env
npm install
npm start
```

Port recomanat per la guia completa: `3002` (via `.env`).

## Túnel SSH (UdL)

```bash
ssh -N -L 3001:localhost:3000 -L 11435:localhost:11434 tuneluser@spoofing02-gcd.udl.cat
```

## Proves locals

Health:

```powershell
Invoke-RestMethod -Method GET -Uri "http://localhost:3002/health"
```

Chat:

```powershell
$body = @{ question = "Hola" } | ConvertTo-Json
Invoke-RestMethod -Method POST -Uri "http://localhost:3002/chat" -ContentType "application/json" -Body $body
```

## Nota important Android

L'app Android no llegeix la URL des de `ChatActivity.kt`.
La llegeix des de `BuildConfig.CHAT_API_BASE_URL` definit a `app/build.gradle.kts`.
