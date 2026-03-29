# Share Data (MQTT) — Implementation Plan

**Stato:** Bozza — in attesa di approvazione
**Autore:** Claude Code
**Data:** 2026-03-29
**Versione:** 1.0

---

## 1. Executive Summary

La feature "Share Data" aggiunge la possibilità di sincronizzare bollette e tipologie di spesa tra più istanze dell'app Spese tramite un broker MQTT opzionale. L'utente configura i parametri di connessione (broker, credenziali, gruppo) e da quel momento ogni modifica locale viene propagata alle altre istanze dello stesso gruppo, e viceversa. L'app continua a funzionare normalmente senza MQTT configurato. L'implementazione segue il pattern già validato nell'app "consumo-carburante".

---

## 2. Obiettivo e motivazione

- **Problema che risolve:** Attualmente ogni istanza dell'app Spese è isolata. Se più membri di una famiglia o gruppo usano l'app, i dati non sono condivisi e vanno inseriti manualmente su ogni dispositivo.
- **Metriche di successo:**
  - [ ] Bollette e tipologie create su un dispositivo compaiono sugli altri entro pochi secondi
  - [ ] L'app senza MQTT configurato non mostra regressioni funzionali
  - [ ] Conflitti risolti automaticamente senza perdita di dati recenti (last-write-wins)
- **Legame con obiettivi di prodotto:** Predisporre l'app alla condivisione dati tra istanze (coerente con la scelta architetturale di UUID come PK)

---

## 3. Scope

### Incluso
- Classe `MqttConfig` per gestione configurazione MQTT via SharedPreferences
- Classe `MqttSyncManager` (singleton) per connessione, publish, subscribe e gestione conflitti
- Activity `MqttConfigActivity` con UI Material Design 3 per configurazione broker
- Sincronizzazione bidirezionale di Bolletta e PurchaseType
- Strategia last-write-wins basata su campo `updatedAt`
- Publish con `retain=true`, cancellazione con payload vuoto
- Full sync (publishAll) alla connessione
- Pulsante "Test connessione" nella schermata di configurazione
- Migrazione DB v3→v4 per aggiunta campo `updatedAt`
- Integrazione nelle Activity esistenti (publish dopo insert/update/delete)

### Escluso (out of scope)
- Sincronizzazione in background tramite Service/WorkManager — la sync avviene solo in foreground
- Crittografia end-to-end dei messaggi — ci si affida a TLS per il trasporto
- Risoluzione conflitti interattiva — last-write-wins automatico
- UI per stato sincronizzazione o log messaggi
- Export/import dati via MQTT (si usa solo per sync live)

### Decisioni aperte

Nessuna — tutte le decisioni sono state risolte.

---

## 4. User Stories e criteri di accettazione

### US-001 · Configurazione MQTT
**Priorità:** Must Have

Come utente voglio configurare i parametri del broker MQTT per abilitare la condivisione dati con altre istanze dell'app.

**Criteri di accettazione:**
- [ ] Schermata accessibile dal menu con campi: broker URL, porta, username, password, group ID, toggle TLS, toggle abilitazione
- [ ] Configurazione salvata in SharedPreferences (`mqtt_config`)
- [ ] Validazione impedisce salvataggio con broker URL o group ID vuoti
- [ ] Voce "Condivisione" visibile nel menu overflow di MainActivity

### US-002 · Sincronizzazione automatica delle bollette
**Priorità:** Must Have

Come utente voglio che le bollette inserite, modificate o eliminate vengano automaticamente propagate alle altre istanze del mio gruppo.

**Criteri di accettazione:**
- [ ] Insert pubblica JSON su `sync/{groupId}/bollette/{id}` con retain
- [ ] Update pubblica lo stesso topic con dati aggiornati
- [ ] Delete pubblica payload vuoto con retain
- [ ] Ricezione remota: insert se non esiste, update se `updatedAt` remoto > locale, ignora altrimenti
- [ ] Bolletta con `purchaseTypeId` inesistente scartata con log warning
- [ ] Se una tipologia remota ha lo stesso `name` di una locale (ma UUID diverso), il nome locale viene modificato (es. suffisso) per evitare conflitto UNIQUE

### US-003 · Sincronizzazione automatica delle tipologie
**Priorità:** Must Have

Come utente voglio che le tipologie di spesa vengano sincronizzate tra istanze.

**Criteri di accettazione:**
- [ ] Stesse regole di publish/subscribe delle bollette
- [ ] Topic: `sync/{groupId}/tipologie/{id}`
- [ ] Tipologie sincronizzate prima delle bollette nel full sync (ordine: tipologie → bollette)

### US-004 · Funzionamento offline
**Priorità:** Must Have

Come utente voglio che l'app funzioni normalmente senza MQTT.

**Criteri di accettazione:**
- [ ] Con MQTT disabilitato, tutte le operazioni CRUD funzionano senza errori
- [ ] Broker irraggiungibile non blocca operazioni locali
- [ ] Riconnessione automatica in `onResume()` se configurato

### US-005 · Test della connessione
**Priorità:** Should Have

Come utente voglio verificare la connessione al broker dalla schermata di configurazione.

**Criteri di accettazione:**
- [ ] Pulsante "Test connessione" tenta connessione e mostra esito (Toast o dialog)
- [ ] Timeout ragionevole (5-10 secondi) con feedback di caricamento

---

## 5. Architettura tecnica

### Componenti coinvolti

```
                        ┌──────────────────┐
                        │   MQTT Broker    │
                        │   (opzionale)    │
                        └────────┬─────────┘
                                 │
                    publish / subscribe (JSON)
                                 │
┌────────────────────────────────┼────────────────────────────────┐
│ App Spese                      │                                │
│                                │                                │
│  ┌─────────────┐    ┌─────────┴──────────┐    ┌─────────────┐  │
│  │ MainActivity │───▶│  MqttSyncManager   │◀───│ MqttConfig  │  │
│  │ AddBolletta  │    │  (singleton)       │    │ (SharedPref)│  │
│  │ PurchaseType │    │                    │    └─────────────┘  │
│  └──────┬───────┘    │ - connect/discon.  │                     │
│         │            │ - publishBolletta  │    ┌─────────────┐  │
│         │            │ - publishPurchType │    │MqttConfig   │  │
│         ▼            │ - publishAll       │    │Activity     │  │
│  ┌─────────────┐     │ - handleIncoming   │    │(UI config)  │  │
│  │  Room DB    │◀───▶│ - conflict resolve │    └─────────────┘  │
│  │  (SQLite)   │     └────────────────────┘                     │
│  │ - bollette  │                                                │
│  │ - purchase_ │                                                │
│  │   types     │                                                │
│  └─────────────┘                                                │
└─────────────────────────────────────────────────────────────────┘
```

### Modifiche al data model

| Tabella/Tipo | Tipo modifica | Dettaglio |
|---|---|---|
| `bollette` | Modifica | Aggiunta colonna `updatedAt INTEGER NOT NULL DEFAULT 0` |
| `purchase_types` | Modifica | Aggiunta colonna `updatedAt INTEGER NOT NULL DEFAULT 0` |
| `Bolletta.java` | Modifica | Campo `updatedAt` con getter/setter, impostato nel costruttore |
| `PurchaseType.java` | Modifica | Campo `updatedAt` con getter/setter, impostato nel costruttore |
| `BollettaDao` | Modifica | Aggiunta query `getById(String id)` |
| `AppDatabase` | Modifica | Versione 3→4, aggiunta `MIGRATION_3_4` |

### Nuove API o endpoint

Non applicabile (app nativa, nessun server HTTP).

**Topic MQTT:**

| Topic | Direzione | Payload | Retain |
|---|---|---|---|
| `sync/{groupId}/tipologie/{id}` | pub/sub | JSON PurchaseType oppure vuoto (delete) | Sì |
| `sync/{groupId}/bollette/{id}` | pub/sub | JSON Bolletta oppure vuoto (delete) | Sì |

### Breaking changes

Nessuno. La migrazione DB è additiva. Il formato MQTT è nuovo.

---

## 6. Piano di implementazione

| ID | Task | Area | Stima (gg) | Dipende da | Responsabile |
|---|---|---|---|---|---|
| T-01 | Migrazione DB v3→v4: aggiunta `updatedAt` a `bollette` e `purchase_types` | DB | 0.25 | — | Dev |
| T-02 | Aggiunta campo `updatedAt` a `Bolletta.java` e `PurchaseType.java` (campo, getter/setter, costruttore) | Model | 0.25 | T-01 | Dev |
| T-03 | Aggiunta `getById(String id)` a `BollettaDao` | DB | 0.1 | — | Dev |
| T-04 | Fix `AddBollettaActivity`: preservare `createdAt` originale in edit, impostare `updatedAt` in insert/update | FE | 0.25 | T-02 | Dev |
| T-05 | Impostare `updatedAt` in `PurchaseTypeActivity` su insert/update | FE | 0.1 | T-02 | Dev |
| T-06 | Aggiunta dipendenze Gradle: HiveMQ MQTT client 1.3.3, Gson 2.10.1, packagingOptions | Infra | 0.1 | — | Dev |
| T-07 | Permessi AndroidManifest: INTERNET, ACCESS_NETWORK_STATE | Infra | 0.1 | — | Dev |
| T-08 | `MqttConfig.java`: gestione configurazione via SharedPreferences | BE | 0.25 | — | Dev |
| T-09 | `MqttConfigActivity.java` + layout XML: UI configurazione broker | FE | 0.5 | T-08 | Dev |
| T-10 | Registrazione `MqttConfigActivity` nel Manifest, voce menu, navigazione da `MainActivity` | FE | 0.1 | T-09 | Dev |
| T-11 | `MqttSyncManager.java`: singleton, connect/disconnect, reconnectIfNeeded | BE | 0.5 | T-06, T-08 | Dev |
| T-12 | `MqttSyncManager`: publishBolletta, publishPurchaseType, publishAll, publishDelete | BE | 0.5 | T-11, T-02 | Dev |
| T-13 | `MqttSyncManager`: subscribe, handleIncoming, conflict resolution (last-write-wins), FK check | BE | 0.5 | T-12, T-03 | Dev |
| T-14 | `MqttSyncManager`: listener/callback per refresh UI da messaggi remoti | BE | 0.25 | T-13 | Dev |
| T-15 | Integrazione `MainActivity`: onResume reconnect, publish dopo delete, registrazione listener refresh | FE | 0.25 | T-14 | Dev |
| T-16 | Integrazione `AddBollettaActivity`: publish dopo insert/update | FE | 0.15 | T-12 | Dev |
| T-17 | Integrazione `PurchaseTypeActivity`: publish dopo insert/update/delete | FE | 0.15 | T-12 | Dev |
| T-18 | Stringhe italiane in `strings.xml` per UI MQTT | FE | 0.1 | — | Dev |
| T-19 | Test manuali multi-device: sync, conflitti, offline, riconnessione | Test | 1.0 | T-15, T-16, T-17 | Dev |

**Stima totale:** 5 giorni/uomo
**Breakdown:** BE 2gg · FE 1.5gg · Test 1gg · Doc 0.5gg

---

## 7. Piano di test

**Strategia generale:** Test manuali su due o più dispositivi Android (o emulatori) connessi allo stesso broker MQTT. Nessun framework di test automatico nel progetto.

### Test cases critici

| ID | Tipo | Descrizione | Priorità |
|---|---|---|---|
| TC-01 | Manuale | Salvare configurazione MQTT con campi validi, verificare persistenza dopo restart app | Alta |
| TC-02 | Manuale | Tentare salvataggio con broker URL vuoto, verificare che la validazione blocchi | Alta |
| TC-03 | Manuale | Test connessione con broker valido: esito positivo | Alta |
| TC-04 | Manuale | Test connessione con broker invalido: esito negativo con messaggio chiaro | Alta |
| TC-05 | Manuale | Creare bolletta su device A, verificare comparsa su device B | Alta |
| TC-06 | Manuale | Modificare bolletta su device A, verificare aggiornamento su device B | Alta |
| TC-07 | Manuale | Eliminare bolletta su device A, verificare scomparsa su device B | Alta |
| TC-08 | Manuale | Creare/modificare/eliminare tipologia su device A, verificare su device B | Alta |
| TC-09 | Manuale | Modificare stessa bolletta su entrambi i device quasi contemporaneamente, verificare che vince il più recente | Media |
| TC-10 | Manuale | Disabilitare MQTT, CRUD bollette funziona normalmente | Alta |
| TC-11 | Manuale | Chiudere e riaprire app con MQTT configurato, verificare riconnessione e ricezione dati | Alta |
| TC-12 | Manuale | Creare bolletta su device A offline, abilitare MQTT, verificare che publishAll la invia | Media |
| TC-13 | Manuale | Ricevere bolletta con purchaseTypeId inesistente, verificare che viene scartata senza crash | Media |
| TC-14 | Manuale | Migrazione DB: app con dati esistenti (v3), aggiornare a v4, verificare che `updatedAt` = 0 per record esistenti | Alta |

### Definition of Done per QA

- [ ] Tutti i test cases TC-01 — TC-14 superati
- [ ] App senza MQTT configurato non mostra regressioni
- [ ] Nessun crash o ANR nei log durante sync
- [ ] Build debug e release compilano senza errori

---

## 8. Rischi e mitigazioni

| Rischio | Probabilità | Impatto | Mitigazione |
|---|---|---|---|
| Conflitto last-write-wins perde modifiche quasi simultanee | Media | Medio | Accettabile per app personale/familiare; documentare comportamento |
| PurchaseType con stesso `name` ma UUID diverso: UNIQUE constraint fallisce | Media | Medio | Rinominare la tipologia locale esistente (es. aggiungendo suffisso numerico) prima di inserire quella remota, così da preservare entrambe |
| Migrazione DB su dati esistenti corrompe il database | Bassa | Alto | Migrazione additiva (ALTER TABLE ADD COLUMN), testare su DB con dati reali |
| Conflitti META-INF Netty nel build | Bassa | Basso | packagingOptions exclude come in consumo-carburante |
| Bolletta ricevuta prima della sua tipologia (FK mancante) | Media | Basso | Scartare con log warning; publishAll invia tipologie prima delle bollette; al prossimo reconnect la bolletta arriverà di nuovo |
| Broker MQTT non raggiungibile blocca UI | Bassa | Alto | Tutte le operazioni MQTT su executor dedicato, mai sul main thread; operazioni locali indipendenti da MQTT |

---

## 9. Rollout e feature flag

**Strategia di rilascio:**
- [x] Deploy diretto (direct)
- [ ] Graduale con feature flag
- [ ] Canary release

La feature è intrinsecamente opzionale: MQTT è disabilitato di default. L'utente deve esplicitamente configurare e abilitare la condivisione.

**Piano di rollback:**
1. L'utente può disabilitare MQTT dal toggle nella schermata di configurazione
2. La disabilitazione interrompe immediatamente la connessione e tutte le operazioni di sync
3. I dati locali rimangono intatti — nessuna perdita

---

## 10. Checklist di approvazione

| Revisione | Responsabile | Stato | Data |
|---|---|---|---|
| Revisione tecnica | Tech Lead | ⏳ In attesa | — |
| Revisione prodotto | Product Owner | ⏳ In attesa | — |
| Stima approvata | Engineering Manager | ⏳ In attesa | — |
| Rischi accettati | Tech Lead | ⏳ In attesa | — |
| Data di inizio confermata | Team | ⏳ In attesa | — |

---

## Domande aperte

Nessuna domanda aperta.

---

*Documento generato con la skill `claude-code-feature`.*
