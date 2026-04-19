# PostureGuard - Monitor de Postura Ergonòmica - Readme maquillat amb Copilot (Purament estètic)

## Descripció del Projecte

PostureGuard és una aplicació Android d'avantguarda dissenyada per millorar la salut física de l'usuari mitjançant la monitorització constant de la postura. Utilitzant tècniques de Machine Learning (Computer Vision) i la fusió de dades de sensors inercials, l'aplicació detecta si l'usuari manté una posició ergonòmica mentre utilitza el dispositiu.

En cas de detectar una postura incorrecta o una inclinació excessiva del coll (com l'efecte "text-neck"), el sistema activa una alerta hàptica (vibració) cada 5 segons per conscienciar l'usuari i fomentar la correcció immediata.

## Característiques Principals

- ** Detecció Intel·ligent**: Classificació de postures mitjançant Machine Learning
- ** Sensors Avançats**: Fusió de dades de càmera i sensors inercials
- ** Feedback Instantani**: Alertes hàptiques per correcció immediata
- ** Eficiència Energètica**: Optimitzat per a ús continu en dispositius mòbils
- ** Precisió Alta**: Model entrenat amb 100% d'accuracy en validació

## Arquitectura General

L'arquitectura del sistema es basa en un model **Modular i Basat en Esdeveniments**, seguint les recomanacions de les guies de "Plataformas en Red". S'ha separat la lògica de negoci, el processament de dades i la interfície per garantir l'eficiència energètica i la mantenibilitat.

### Capes del Sistema

**Capa de Percepció**: Captura dades en temps real de la càmera (CameraX) i del sensor de vector de rotació.

**Capa de Processament**: El motor TensorFlow Lite processa els frames per classificar la postura, mentre que la lògica de sensors calcula l'angle d'inclinació absolut.

**Capa d'Acció**: Gestiona el feedback de l'usuari mitjançant canvis visuals a la UI i el control del motor de vibració del hardware.

##  Mòduls Principals

El projecte s'ha dividit en mòduls especialitzats per complir amb els principis de "Separation of Concerns":

- **ui/MainActivity**: Actua com el controlador central (orquestrador). Gestiona el cicle de vida de l'activitat i sincronitza la informació visual.

- **ml/PoseClassifier**: Encapsula el model de Machine Learning. Realitza el preprocessament de la imatge (resize a 160x160, Grayscale) i executa la inferència amb l'intèrpret de TFLite.

- **sensors/AngleProvider**: Gestiona el Rotation Vector Sensor. Implementa la matriu de rotació per obtenir un angle de 360° estable i sense soroll, evitant el Gimbal Lock.

- **sensors/VibrationManager**: Controla el sistema d'alertes hàptiques. Utilitza un Handler per programar vibracions periòdiques (cada 5s) sense bloquejar el fil d'execució principal (UI thread).

## Tecnologies Utilitzades

- **Android Studio Ladybug**: Entorn de desenvolupament
- **Kotlin**: Llenguatge de programació principal
- **TensorFlow Lite 2.16.1**: Motor de Machine Learning
- **CameraX**: API moderna per a càmera Android
- **Firebase**: Autenticació i base de dades (Auth, Firestore)
- **Edge Impulse**: Plataforma per entrenament de models ML
- **MobileNetV2**: Arquitectura del model neuronal

## Estructura del Projecte

```
TestPosturAI/
├── app/                          # Aplicació principal Android
│   ├── src/main/
│   │   ├── assets/               # Model TFLite
│   │   ├── java/com/example/testposturai/
│   │   │   ├── auth/             # Gestió d'autenticació
│   │   │   ├── ml/               # Classificador de postures
│   │   │   ├── sensors/          # Sensors i vibració
│   │   │   └── ui/               # Interfície d'usuari
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts          # Configuració Gradle
├── ml/                           # Documentació ML
│   └── EdgeImpulse/
│       ├── dataset/              # Conjunt de dades
│       ├── experiments/          # Experiments i resultats
│       └── models/               # Models entrenats
└── gradle/                       # Configuració Gradle
```

## Instruccions d'Execució

### Requisits previs

- Dispositiu físic Android amb API 26 (Android 8.0) o superior
- Càmera frontal funcional
- Android Studio Ladybug o superior
- Connexió a internet per a dependències

### Passos d'instal·lació

1. **Clonar el repositori:**
   ```bash
   git clone https://github.com/el-teu-usuari/testposturai.git
   cd testposturai
   ```

2. **Importar el projecte:**
   - Obre Android Studio
   - Selecciona "Open" i navega fins a la carpeta `TestPosturAI/`
   - Espera que es sincronitzin les dependències Gradle

3. **Configurar el model ML:**
   - Assegura't que el fitxer `tflite_learn_901615_40.tflite` es troba a `app/src/main/assets/`
   - El model ja està inclòs al repositori

4. **Compilar i executar:**
   - Connecta el dispositiu Android per USB
   - Prem el botó "Run" a Android Studio
   - Accepta els permisos de càmera quan se sol·licitin

## Documentació de Machine Learning

### Dataset
Les imatges han estat capturades amb dispositiu mòbil en entorn real:
- **Total de mostres:** 169 imatges d'entrenament + 32 imatges de test
- **Classes:**
  - **Correcte (122 imatges):** Variacions de cara i costat, diferents vestimentes i accessoris
  - **Incorrecte (47 imatges):** Postures inclinades amb mateixes variacions

### Preprocessament
1. **Reescalat:** Ajust a 160x160 píxels
2. **Normalització:** Conversió a Grayscale per reduir complexitat
3. **Data Augmentation:** Girs horitzontals i variacions de brillantor

### Arquitectura del Model
- **Framework:** MobileNetV2 (Alpha 0.35)
- **Input:** 160x160 píxels, Grayscale
- **Optimizer:** Adam (Learning rate: 0.0005)

### Resultats
| Mètrica | Valor |
|:--------|:------|
| **Accuracy** | 100% |
| **Loss** | 0.01% |
| **Temps d'Inferència** | 10 ms |
| **Ús de RAM** | 546.6K |

### Rendiment On-device
- **Latència:** 10 ms (processament en CPU)
- **Mida del model:** ~1.6MB (format .tflite)
- **Compatibilitat:** Qualsevol telèfon Android modern

## Experiments ML

S'han comparat dues arquitectures per trobar l'equilibri òptim:

| Mètrica | MobileNetV2 (RGB) | MobileNetV2 (Grayscale) |
|:--------|:------------------|:-----------------------|
| Resolució | 96x96 píxels | **160x160 píxels** |
| Color | RGB (Color) | **Grayscale** |
| Accuracy | 88.9% | **100.0%** |
| Loss | 0.15 | **0.01** |
| Temps Inferència | 6 ms | **10 ms** |
| Ús RAM | 546.0K | **546.6K** |

## Conclusions

L'increment de resolució ha estat clau per assolir 100% d'accuracy. Tot i l'augment lleuger en temps d'inferència, la millora en precisió justifica el canvi. El model generalitza correctament malgrat canvis de vestimenta o accessoris.

## Llicència

Aquest projecte està sota llicència MIT. Consulta el fitxer LICENSE per a més detalls.

## Autor

**Martí** - Desenvolupador principal

## Agraïments

- Edge Impulse per la plataforma d'entrenament ML
- Google per les APIs de CameraX i TensorFlow Lite
- Comunitat Android per la documentació i suport
- Gemini i Copilot per l'ajuda de cerca d'informació i millora estètica 

## Sistema d'Autenticació

PostureGuard inclou un sistema complet d'autenticació d'usuaris basat en Firebase Authentication i Firestore per gestionar perfils d'usuari.

### Funcionalitats d'Autenticació

- ** Registre d'usuaris**: Creació de comptes amb validació d'email
- ** Inici de sessió**: Autenticació segura amb email i contrasenya
- ** Verificació d'email**: Sistema de verificació obligatòria per seguretat
- ** Gestió de perfil**: Edició d'informació personal (email)
- ** Tancament de sessió**: Logout segur amb neteja d'estat
- ** Verificació automàtica**: Comprovació en temps real de l'estat de verificació

### Flux d'Autenticació

1. **Registre**:
   - L'usuari introdueix email i contrasenya
   - Es crea el compte a Firebase Auth
   - S'envia automàticament un email de verificació
   - Es guarda l'estat "verificacioPendent" a Firestore
   - L'usuari és desconnectat fins a verificar l'email

2. **Verificació d'Email**:
   - L'usuari rep un email amb enllaç de verificació
   - En fer clic, es marca com verificat a Firebase
   - L'app detecta automàticament el canvi d'estat
   - S'actualitza Firestore i es permet l'accés

3. **Login**:
   - Autenticació amb credencials
   - Comprovació d'estat de verificació
   - Accés a la pantalla principal si tot és correcte

4. **Gestió de Perfil**:
   - Possibilitat de canviar l'adreça d'email
   - Requeriment de verificació del nou email
   - Actualització segura amb Firebase Auth

5. **Logout**:
   - Tancament de sessió net
   - Retorn a pantalla d'autenticació
   - Neteja d'estat local

### Seguretat Implementada

- **Verificació d'email obligatòria** abans d'accedir a l'app
- **Protecció contra comptes no verificats**
- **Gestió d'estats** amb Firestore per persistència
- **Validació de sessions** en cada accés
- **Reenviament d'emails** de verificació si cal

### Base de Dades

S'utilitza **Firebase Firestore** per emmagatzemar:
- Estat de verificació d'email (`verificacioPendent`)
- Possibles extensions futures de perfil d'usuari

Aquest sistema garanteix que només usuaris verificats puguin accedir a les funcionalitats de monitoratge postural, mantenint la privacitat i seguretat de les dades.
