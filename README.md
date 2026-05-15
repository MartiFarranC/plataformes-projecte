# PosturAI

Guia oficial (single source of truth) per executar el sistema complet end-to-end:
- App Android amb TensorFlow Lite (classificació de postura on-device)
- Backend Node.js amb RAG + connexió al model IA via VPN/SSH tunnel
- Exposició del backend amb ngrok per al xat des del mòbil

## 1. Estructura del repositori

```text
posturAI/
|- app/              # App Android
|- backend/          # Backend Node.js (chat + RAG)
|- ml/               # Materials de ML (models, experiments, scripts)
|- README.md         # Aquesta guia (font única)
```

## 2. Requisits previs

- Android Studio (Ladybug o superior)
- JDK compatible amb el projecte Android
- Node.js 18+ i npm
- Python 3.10+ (per scripts ML)
- Compte/configuració Firebase per a autenticació (fitxer `app/google-services.json`)
- VPN UdL (si cal accedir al model IA remot)
- `ssh` i `ngrok` instal·lats
- Dispositiu Android físic (recomanat) o emulador

## 3. Part ML (entrenament, tuning i export)

Scripts finals:

- `ml/EdgeImpulse/scripts/train_model.py`: entrena 1 configuració i exporta `.keras` + `.tflite`
- `ml/EdgeImpulse/scripts/train_experiments.py`: executa múltiples models + hiperparàmetres i tria model final
- `ml/EdgeImpulse/scripts/export_rag_embeddings_for_tensorflow.py`: exporta embeddings RAG a `.npz`

### 3.1 Entrenament d'una configuració

```bash
python ml/EdgeImpulse/scripts/train_model.py \
  --data-dir <dataset_root> \
  --img-size 160 \
  --grayscale \
  --epochs 20 \
  --fine-tune-epochs 10
```

### 3.2 Campanya de múltiples models i ajust d'hiperparàmetres

```bash
python ml/EdgeImpulse/scripts/train_experiments.py \
  --data-dir <dataset_root> \
  --epochs 20 \
  --fine-tune-epochs 10
```

Resultats:
- `ml/EdgeImpulse/experiments/experiments_summary.json`
- model final exportat: `ml/EdgeImpulse/models/tflite_learn_901615_40.tflite`

### 3.3 Model per a l'app Android

Copia o mantén el model final a:
- `app/src/main/assets/tflite_learn_901615_40.tflite`

## 4. Configuració Android (inferencia local)

La inferència local de postura es fa amb `PoseClassifier` (TensorFlow Lite) i es mostra a la UI de `MainActivity`.

## 5. Configuració backend (RAG + IA)

Des de l'arrel del projecte:

```powershell
cd backend
copy .env.example .env
npm install
```

Valors rellevants de `backend/.env`:
- `PORT=3002`
- `UNIVERSITY_AI_URL=http://127.0.0.1:11435/api/generate`
- `OLLAMA_MODEL=llama3.2:3b`
- `EMBEDDINGS_URL=http://127.0.0.1:11435/api/embeddings`
- `EMBEDDINGS_MODEL=nomic-embed-text`

## 6. Obrir túnel SSH (IA remota UdL)

Amb VPN activa, obre una terminal separada i deixa-la oberta:

```bash
ssh -N -L 3001:localhost:3000 -L 11435:localhost:11434 tuneluser@spoofing02-gcd.udl.cat
```

## 7. Engegar backend

En una altra terminal:

```powershell
cd backend
npm start
```

Comprovació ràpida:

```powershell
Invoke-RestMethod -Method GET -Uri "http://localhost:3002/health"
```

Test del xat:

```powershell
$body = @{ question = "Consells de postura?" } | ConvertTo-Json
Invoke-RestMethod -Method POST -Uri "http://localhost:3002/chat" -ContentType "application/json" -Body $body
```

## 8. Exposar backend amb ngrok

```bash
ngrok http 3002
```

Copia la URL HTTPS pública (exemple: `https://xxxx.ngrok-free.app`).

## 9. Connectar l'app Android al backend

L'app llegeix la URL base des de `BuildConfig.CHAT_API_BASE_URL`, definit a `app/build.gradle.kts`.

Opció recomanada (sense editar codi):

```bash
./gradlew :app:assembleDebug -PCHAT_API_BASE_URL=https://xxxx.ngrok-free.app
```

Opció alternativa:
- editar `chatApiBaseUrl` a `app/build.gradle.kts`

## 10. Executar l'app Android

1. Obre el projecte a Android Studio.
2. Espera la sync de Gradle.
3. Connecta dispositiu/emulador.
4. Executa `app` (`Run`).
5. Dona permisos de càmera.

Flux funcional esperat:
- Login/Register (Firebase) -> pantalla principal
- `Comencar analisi postural` -> inferència TFLite local + resultat visual
- `Xat IA` -> petició HTTP a `/chat` del backend via ngrok

## 11. Verificació end-to-end (checklist)

1. `health` backend respon a `localhost:3002`
2. `/chat` respon en local
3. ngrok exposa `3002` i l'endpoint remot respon
4. `CHAT_API_BASE_URL` apunta a la URL ngrok actual
5. Des de l'app, el xat rep resposta
6. A `MainActivity`, es veu `Postura correcta/incorrecta` en temps real
7. El model TFLite de `app/assets` coincideix amb el model final exportat

## 12. Troubleshooting

- Error `No he pogut connectar amb la IA`:
- Revisa `CHAT_API_BASE_URL`
- Revisa que ngrok estigui actiu
- Revisa que backend estigui en marxa
- Error backend `503`:
- Revisa VPN + túnel SSH
- Revisa `UNIVERSITY_AI_URL`
- El xat diu `backend incorrecte`:
- URL apuntant a un servei que no és aquest backend
- El model de postura no detecta:
- Verifica permisos de càmera
- Verifica que el `.tflite` existeix a `app/src/main/assets/`

## 13. Referències internes

- Backend detallat: `backend/README.md`
- Documentació completa d'experiments: `ml/EdgeImpulse/experiments/ML_EXPERIMENTS.md`
