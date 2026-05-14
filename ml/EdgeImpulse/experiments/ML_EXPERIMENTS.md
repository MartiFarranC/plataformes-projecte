# ML & RAG Experimentation Report: posturAI

Aquest document detalla el procés de recerca, entrenament i implementació dels models d'Intel·ligència Artificial utilitzats en el projecte `posturAI`, tant per a la classificació de postures en temps real com per a l'assistent de xat intel·ligent.

## 1. Mòdul de Machine Learning (Visió per Computador)

### 1.1 Descripció del Problema

L'objectiu és detectar en temps real si l'usuari manté una postura ergonòmica davant de l'ordinador. El sistema ha de ser capaç d'executar-se en un dispositiu mòbil (Android) amb baixa latència i sense dependre del núvol.

### 1.2 Dataset

Es tracta d'un dataset propi capturat en un entorn real (habitació amb il·luminació natural/artificial) per maximitzar la fiabilitat en el context d'ús final.

- Classes: `Correcte` i `Incorrecte` (inclinació excessiva o mala posició).
- Volum total: `201` imatges.
- Entrenament: `169` imatges (`122` Correctes / `47` Incorrectes).
- Test: `32` imatges (`25` Correctes / `7` Incorrectes).

### 1.3 Preprocessament i Augmentació

- Resizing: les imatges es reescalen a `160x160` píxels.
- Color: conversió a `grayscale` (1 canal) per reduir la càrrega computacional i centrar el model en formes i siluetes.
- Data augmentation: `horizontal flip`, variació de brillantor i `random crop/resize` durant l'entrenament per reduir overfitting.

### 1.4 Models Evaluats i Entrenament

S'ha utilitzat l'arquitectura `MobileNetV2` (`alpha=0.35`) pel seu equilibri entre precisió i velocitat en dispositius ARM.

| Paràmetre | Experiment 1 (Baseline) | Experiment 2 (Final) |
|---|---|---|
| Arquitectura | MobileNetV2 RGB | MobileNetV2 Grayscale |
| Resolució | 96x96 | 160x160 |
| Optimizer | Adam (LR: 0.0005) | Adam (LR: 0.0005 + Fine-tuning) |
| Accuracy (Test) | 88.9% | 100.0% |
| Loss | 0.15 | 0.01 |
| Inferència | 6 ms | 10 ms |
| Pes model | ~1.6 MB | ~1.6 MB |

Procés d'entrenament:

- Fase inicial de `20` epochs.
- Fase de fine-tuning de `10` epochs amb `learning rate = 4.5 x 10^-5`.
- `Dropout = 0.1`.
- Ajust final amb capa `Dense(16)`.

### 1.5 Optimització Mòbil

El model final s'ha exportat a format TensorFlow Lite (`.tflite`). Tot i que no s'ha aplicat una quantització agressiva (`int8`), el pes de `1.6 MB` i la latència de `10 ms` permeten una execució fluida en la majoria de dispositius Android moderns.

## 2. RAG (Retrieval-Augmented Generation)

### 2.1 Descripció i Abordatge

Per a l'assistent de xat, s'ha implementat un sistema RAG per garantir que les respostes del model de llenguatge (LLM) estiguin basades en coneixement expert sobre ergonomia i no només en el seu entrenament base.

### 2.2 Dades de Coneixement

El sistema indexa fitxers de text pla en format `.md`, `.txt` i `.json` situats a `backend/knowledge/`.

Fonts principals:

- `posture_guidelines.md`
- `info.md`
- `app_overview.md`

Nota: els fitxers PDF presents a la carpeta estan reservats per a futures expansions del parser.

### 2.3 Tecnologia i Arquitectura

S'ha optat per una arquitectura lightweight sense bases de dades vectorials externes:

- Cerca: basada en tokens i freqüència de termes (cerca lèxica) en memòria.
- Filtrat: llindars de rellevància (`minScore`, `minMatchRatio`) per reduir soroll.
- Model: backend amb `Llama 3.2:3b` (via Ollama) per generar la resposta final amb context recuperat.

### 2.4 Resultats i Discussió

- Validació: inspecció manual mitjançant endpoints de control (`/rag/check`).
- Seguretat: detecció d'intent de domini; si no hi ha relació o context suficient, es retorna missatge "fora de context" per minimitzar al·lucinacions.

## 3. Estructura del Projecte (ML/RAG)

```text
ml/
└─ EdgeImpulse/
   ├─ scripts/
   │  └─ train_model.py          # Script principal d'entrenament Keras/TF
   └─ models/
      └─ tflite_learn_...tflite  # Model final per a l'app Android

backend/
├─ rag/
│  ├─ splitter.js                # Lògica de fragmentació de documents
│  └─ store.js                   # Motor de cerca i gestió de context
└─ knowledge/                    # Base de coneixement (Markdown/Text)
```

## 4. Conclusions

L'Experiment 2 ha demostrat que la combinació de `MobileNetV2` en escala de grisos amb resolució `160x160` ofereix una precisió del `100%` en el dataset actual, sent una solució molt adequada per a `posturAI`. El sistema RAG, tot i ser simple (cerca lèxica), compleix la funció de restringir el comportament del xat a l'àmbit de la salut postural amb cost computacional baix al servidor.
