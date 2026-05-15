# RAG backend

Aquest directori conte la implementacio del RAG per al backend.

## On posar la informacio
Posa els fitxers que vols que el RAG utilitzi a `backend/knowledge/`. Pots usar:
- `.md`
- `.txt`
- `.json`
- `.html`

## Endpoints disponibles

### 1. `POST /rag/split`
Divideix un text en chunks.

Body d'exemple:
```json
{
  "text": "Aquesta es una prova ...",
  "chunkSize": 500,
  "overlap": 100
}
```

### 2. `POST /rag/load`
Carrega els fitxers de `backend/knowledge/`, construeix chunks i calcula embeddings.

Body d'exemple opcional:
```json
{
  "chunkSize": 500,
  "overlap": 100
}
```

### 3. `POST /rag/query`
Fes una pregunta i utilitza el context dels documents carregats per construir el prompt.

Body d'exemple:
```json
{
  "question": "Quina es la millor postura per seure?",
  "topK": 5
}
```

## Flux d'us
1. Afegir informacio dins `backend/knowledge/`.
2. Arrancar el servidor.
3. Fer `POST /rag/load`.
4. Fer `POST /rag/query` per obtenir respostes amb context.

## Nota tecnica
- Recuperacio principal: embeddings + similitud cosinus.
- Fallback: cerca lexica simple si el servei d'embeddings no esta disponible.
- Configuracio a `.env`:
  - `EMBEDDINGS_CACHE_FILE`
  - `EMBEDDINGS_ENABLED`
  - `EMBEDDINGS_URL`
  - `EMBEDDINGS_MODEL`
  - `EMBEDDINGS_TIMEOUT_MS`
  - `EMBEDDINGS_CONCURRENCY`

- Persistencia:
  - Els embeddings es guarden en un fitxer JSON dins `backend/knowledge/`.
  - Per defecte: `backend/knowledge/.rag-embeddings-cache.json`.
  - En cada `POST /rag/load`, es reutilitzen embeddings existents i nomes es recalculen chunks nous o modificats.

## Comportament fora de context
`/chat` i `/rag/query` validen la rellevancia de la pregunta amb el contingut carregat al RAG.
Si la pregunta no te prou relacio amb el context, la resposta sera:
`Perdona pero no puc parlar de res que no sigui el meu context.`
