1. Descripción del proyecto
   PostureGuard és una aplicació Android d'avantguarda dissenyada per millorar la salut física de l'usuari mitjançant la monitorització constant de la postura. Utilitzant tècniques de Machine Learning (Computer Vision) i la fusió de dades de sensors inercials, l'aplicació detecta si l'usuari manté una posició ergonòmica mentre utilitza el dispositiu.

En cas de detectar una postura incorrecta o una inclinació excessiva del coll (com l'efecte "text-neck"), el sistema activa una alerta hàptica (vibració) cada 5 segons per conscienciar l'usuari i fomentar la correcció immediata.

2. Arquitectura general de la aplicación

3. L'arquitectura del sistema es basa en un model Modular i Basat en Esdeveniments, seguint les recomanacions de les guies de "Plataformas en Red". S'ha separat la lògica de negoci, el processament de dades i la interfície per garantir l'eficiència energètica i la mantenibilitat.

L'aplicació opera en tres capes principals:

Capa de Percepció: Captura dades en temps real de la càmera (CameraX) i del sensor de vector de rotació.

Capa de Processament: El motor TensorFlow Lite processa els frames per classificar la postura, mentre que la lògica de sensors calcula l'angle d'inclinació absolut.

Capa d'Acció: Gestiona el feedback de l'usuari mitjançant canvis visuals a la UI i el control del motor de vibració del hardware.

3. Módulos principales del sistema
El projecte s'ha dividit en mòduls especialitzats per complir amb els principis de "Separation of Concerns":

ui/MainActivity: Actua com el controlador central (orquestrador). Gestiona el cicle de vida de l'activitat i sincronitza la informació visual.

ml/PoseClassifier: Encapsula el model de Machine Learning. Realitza el preprocessament de la imatge (resize a 160x160, normalització RGB) i executa la inferència amb l'intèrpret de TFLite.

sensors/AngleProvider: Gestiona el Rotation Vector Sensor. Implementa la matriu de rotació per obtenir un angle de 360° estable i sense soroll, evitant el Gimbal Lock.

sensors/VibrationManager: Controla el sistema d'alertes hàptiques. Utilitza un Handler per programar vibracions periòdiques (cada 5s) sense bloquejar el fil d'execució principal (UI thread).

4. Instrucciones para ejecutar la app
   Requisitos previos
Dispositiu físic Android amb API 26 (Android 8.0) o superior (necessari per al motor de vibració i sensors de rotació).

Càmera frontal funcional.

Android Studio Ladybug o superior.

   Pasos de instalación
Clonar el repositori:

Bash
git clone https://github.com/el-teu-usuari/testposturai.git
Importar el projecte:
Obre Android Studio i selecciona la carpeta app/.

Configurar el model:
Assegura't que el fitxer tflite_learn_901615_40.tflite es troba a la carpeta app/src/main/assets/.

Compilar i Executar:
Connecta el dispositiu per USB i prem Run. Accepta els permisos de càmera quan se sol·licitin.

5. Documentación de Machine Learning
