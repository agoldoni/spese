# Spese

App Android nativa per la gestione delle spese personali, costruita con Material Design 3.

## Tech Stack

- **Linguaggio:** Java 17
- **Framework:** Android SDK (API 26 - 34)
- **UI:** Material Design 3 (Material Components 1.11.0)
- **Database:** Room (SQLite) con migrazioni
- **Sync:** MQTT opzionale (HiveMQ client 1.3.3)
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
│   ├── MainActivity.java         # Lista bollette
│   ├── AddBollettaActivity.java  # Inserimento/modifica bolletta
│   ├── PurchaseTypeActivity.java # Gestione tipologie di spesa
│   ├── SummaryActivity.java      # Riepilogo per anno/tipologia
│   ├── InfoActivity.java         # Informazioni / versione
│   ├── MqttConfig.java           # Configurazione MQTT (SharedPreferences)
│   ├── MqttSyncManager.java      # Sincronizzazione dati via MQTT
│   ├── MqttConfigActivity.java   # UI configurazione broker MQTT
│   └── db/
│       ├── AppDatabase.java      # Database Room (SQLite) con migrazioni
│       ├── Bolletta.java         # Entita' bolletta
│       ├── BollettaDao.java      # DAO bollette
│       ├── PurchaseType.java     # Entita' tipologia di spesa
│       ├── PurchaseTypeDao.java  # DAO tipologie
│       └── YearlySummary.java    # Modello riepilogo annuale
├── res/
│   ├── layout/                   # Layout XML delle schermate
│   ├── menu/                     # Menu dell'app
│   ├── values/                   # Stringhe, colori, temi
│   └── drawable/                 # Icone e risorse grafiche
└── AndroidManifest.xml
```

## Funzionalita'

### Gestione bollette
- Inserimento, modifica ed eliminazione di bollette (spese)
- Ogni bolletta ha: tipologia, importo, mese e anno
- Lista cronologica con importo formattato in euro

### Tipologie di spesa
- Anagrafica delle categorie di spesa (es. Luce, Gas, Acqua)
- Nome univoco e descrizione opzionale
- Protezione: non e' possibile eliminare una tipologia in uso

### Riepilogo
- Totale spese per anno
- Totale spese per anno e tipologia

### Condivisione dati (MQTT)
- Sincronizzazione bidirezionale tra piu' istanze dell'app tramite broker MQTT
- Completamente opzionale: l'app funziona normalmente senza configurazione
- Configurazione: indirizzo broker, porta, credenziali, ID gruppo, TLS
- Test connessione dalla schermata di configurazione
- Strategia last-write-wins per la risoluzione automatica dei conflitti
- Full sync alla connessione, publish incrementali ad ogni modifica
- Gestione automatica dei conflitti di nome sulle tipologie

### Altro
- Tema Material Design 3 con supporto DayNight
- Schermata info con versione dell'app
- Script di build e installazione multi-dispositivo

## Licenza

Questo progetto e' ad uso personale.
