# Spese

App Android nativa per la gestione delle spese personali, costruita con Material Design 3.

## Stato del progetto

Il progetto e' attualmente nella fase iniziale (v1.0.0) con la struttura base dell'applicazione: navigazione tra schermate, tema Material Design 3 e script di build/installazione.

## Tech Stack

- **Linguaggio:** Java 17
- **Framework:** Android SDK (API 26 - 34)
- **UI:** Material Design 3 (Material Components 1.11.0)
- **Build system:** Gradle 8.2

## Requisiti

- Android SDK (API 34)
- Java 17 JDK
- ADB (per l'installazione su dispositivo)

## Build

```bash
# Build APK di debug
./build.sh debug

# Build APK di release
./build.sh release

# Pulizia artefatti
./build.sh clean
```

In alternativa, tramite Gradle direttamente:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

L'APK generato si trova in `app/build/outputs/apk/`.

## Installazione su dispositivo

```bash
# Installa l'APK di debug su tutti i dispositivi connessi
./install-all.sh

# Build + installazione
./install-all.sh --build
```

## Struttura del progetto

```
app/src/main/
├── java/com/spese/
│   ├── MainActivity.java       # Schermata principale
│   └── InfoActivity.java       # Schermata informazioni / versione
├── res/
│   ├── layout/                 # Layout XML delle schermate
│   ├── menu/                   # Menu dell'app
│   ├── values/                 # Stringhe, colori, temi
│   └── drawable/               # Icone e risorse grafiche
└── AndroidManifest.xml
```

## Funzionalita' attuali

- Schermata principale con toolbar Material Design 3
- Schermata info con versione dell'app
- Tema Material Design 3 con supporto DayNight
- Script di build e installazione multi-dispositivo

## Licenza

Questo progetto e' ad uso personale.
