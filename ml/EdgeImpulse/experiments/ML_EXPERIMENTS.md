# Informe d'Experiments de Machine Learning
### (Plantilla feta per Gemini i omplerta per Martí recollint dades d'EdgeImpulse)

**Autor:** Martí  
**Projecte:** Detector de Postura Ergonòmica (Correcte/Incorrecte)

## 1. Dataset
### Origen de les dades
Les imatges han estat capturades íntegrament amb el meu dispositiu mòbil personal en un entorn real (la meva habitació). S'ha buscat recrear les condicions habituals d'ús de l'aplicació.

### Distribució de les dades
S'ha creat un dataset diversificat per evitar biaixos per vestimenta o accessoris:
- **Total de mostres:** 169 imatges d'entrenament + 32 imatges de test.
- **Classes:**
    - **Correcte (122 imatges):** Inclou variacions de cara i costat, amb diferents vestimentes (dessuadora grisa, verda, sense dessuadora) i l'ús de cascos des de diferents angles (dreta, esquerra, cara).
    - **Incorrecte (47 imatges):** Inclou postures inclinades o mal posicionades amb les mateixes variacions de roba i accessoris que la classe anterior.
- **Set de Test (32 imatges):** 25 correctes i 7 incorrectes per validar el model final.

## 2. Preprocessament
Totes les imatges han seguit el següent flux de preparació abans de l'entrenament:
1. **Reescalat:** Ajust de la mida a **160x160 píxels**.
2. **Normalització de color:** Conversió a **Grayscale** (escala de grisos) per reduir la complexitat computacional i centrar l'aprenentatge en la forma i posició del cos, no en els colors.
3. **Data Augmentation:** Aplicació de girs horitzontals i variacions de brillantor aleatòries per augmentar la robustesa del model davant canvis d'il·luminació.

## 3. Taula d'Experiments
S'han comparat dues arquitectures principals per trobar l'equilibri òptim entre precisió i velocitat en el dispositiu mòbil:

| Mètrica                 | Experiment 1: MobileNetV2 (RGB) | Experiment 2: MobileNetV2 (Grayscale) |
|:------------------------|:--------------------------------|:--------------------------------------|
| **Resolució**           | 96x96 píxels                    | **160x160 píxels**                    |
| **Color**               | RGB (Color)                     | **Grayscale (Grisos)**                |
| **Accuracy (Precisió)** | 88.9%                           | **100.0%**                            |
| **Loss (Error)**        | 0.15                            | **0.01**                              |
| **Temps d'Inferència**  | 6 ms                            | **10 ms**                             |
| **Ús de RAM (Peak)**    | 546.0K                          | **546.6K**                            |

## 4. Anàlisi del Rendiment (On-device Performance)
El model final triat (**Experiment 2**) presenta un rendiment excel·lent per a un entorn mòbil:
- **Latència (Inferència):** 10 ms (processament en CPU).
- **Consell de memòria:** Només 546.6K de RAM, cosa que el fa extremadament lleuger per a qualsevol telèfon Android modern.
- **Mida del model:** Aproximadament 1.6M (format .tflite).

## 5. Conclusions
L'increment de la resolució de 96x96 a 160x160 ha estat clau per assolir una precisió del 100% en el set de validació. Tot i que el temps d'inferència ha pujat lleugerament (de 6ms a 10ms), la millora en l'accuracy justifica el canvi, ja que segueix estant molt per sota dels límits de percepció humana en temps real. El model és capaç de generalitzar correctament tot i els canvis de vestimenta o l'ús de cascos.

## Resultat últim experiment (MobileNetV2 0.35, 160x160 Grayscale):

## 1. Definició del Problema
L'objectiu és classificar la postura de l'usuari en dues categories:
- **Correcte**: Usuari ben assegut davant la càmera.
- **Incorrecte**: Usuari inclinat o en posició no ergonòmica.

## 2. Arquitectura del Model
S'ha utilitzat **MobileNetV2 (Alpha 0.35)** per la seva eficiència en dispositius mòbils.
- **Input**: 160x160 píxels, Grayscale.
- **Optimizer**: Adam (Learning rate: 0.0005).

## 3. Resultats de l'Entrenament
Dades obtingudes després de l'entrenament a Edge Impulse:

###### Mirar imatge Experiment2_Resilts.png a la mateixa carpeta.

| Mètrica      | Valor |
|:-------------|:------|
| **Accuracy** | 100%  |
| **Loss**     | 0.01% |

- **Observacions**: El model ha aconseguit una alta precisió, però cal tenir en compte que el conjunt de dades és limitat i pot no reflectir totes les variacions possibles en la postura dels usuaris. S'han de fer més proves amb més datasets.

