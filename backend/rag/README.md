# RAG backend

Aquest directori conté l'implementació bàsica del RAG per al backend.

## On posar la informació
Posa els fitxers que vols que el RAG utilitzi a `backend/knowledge/`. Pots usar:
- `.md`
- `.txt`
- `.json`
- `.html`

## Endpoints disponibles

### 1. `POST /rag/split`
Divideix un text en chunks.

Body d'exemple:
```
{
  "text": "Aquesta és una prova ...",
  "chunkSize": 500,
  "overlap": 100
}
```

### 2. `POST /rag/load`
Carrega els fitxers de `backend/knowledge/` i construeix els chunks.

Body d'exemple opcional:
```
{
  "chunkSize": 500,
  "overlap": 100
}
```

### 3. `POST /rag/query`
Fes una pregunta i utilitza el context dels documents carregats per construir el prompt.

Body d'exemple:
```
{
  "question": "Quina és la millor postura per seure?",
  "topK": 5
}
```

## Flux d'ús
1. Afegir informació dins `backend/knowledge/`.
2. Arrancar el servidor.
3. Fer `POST /rag/load`.
4. Fer `POST /rag/query` per obtenir respostes amb context.

## Nota
Aquesta implementació usa una cerca de text simple. En fases següents es pot afegir un vector store amb embeddings externs o una base de dades de vectors.

## Comportament fora de context
`/chat` i `/rag/query` ara validen la rellevància de la pregunta amb el contingut carregat al RAG.  
Si la pregunta no té prou relació amb el context, la resposta serà:
`Perdona però no puc parlar de res que no sigui el meu context.`
