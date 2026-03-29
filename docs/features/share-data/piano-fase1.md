# Share Data — Piano (Fase 1)

## 1. Obiettivo e motivazione

Permettere a più istanze dell'app Spese di condividere e sincronizzare i dati (bollette e tipologie di spesa) tramite un broker MQTT opzionale. L'utente potrà configurare la connessione a un broker MQTT e, una volta abilitata, ogni modifica locale verrà propagata alle altre istanze dello stesso gruppo, e viceversa. L'app continuerà a funzionare perfettamente anche senza MQTT configurato.

## 2. Scope

### Incluso
- Classe `MqttConfig` per gestione configurazione MQTT via SharedPreferences
- Classe `MqttSyncManager` (singleton) per connessione, publish, subscribe e gestione conflitti
- Activity `MqttConfigActivity` con UI per configurazione broker (URL, porta, username, password, group ID, TLS, toggle abilitazione)
- Sincronizzazione bidirezionale di **Bolletta** e **PurchaseType**
- Strategia last-write-wins basata su campo `updatedAt`
- Publish con `retain=true` per garantire che nuovi subscriber ricevano lo stato corrente
- Cancellazione remota tramite payload vuoto con retain
- Full sync (publishAll) alla connessione
- Pulsante "Test connessione" nella schermata di configurazione
- Aggiunta campo `updatedAt` a Bolletta e PurchaseType (migrazione DB)
- Integrazione nelle Activity esistenti (publish dopo insert/update/delete)

### Escluso (out of scope)
- Sincronizzazione in background tramite Service/WorkManager — la sync avviene solo quando l'app è in foreground
- Crittografia end-to-end dei messaggi — si affida a TLS per la sicurezza del trasporto
- Risoluzione conflitti interattiva — si usa last-write-wins automatico
- UI per visualizzare lo stato della sincronizzazione o log dei messaggi

## 3. User Stories

### US-001 · Configurazione MQTT
Come utente voglio configurare i parametri del broker MQTT per abilitare la condivisione dati con altre istanze dell'app.

### US-002 · Sincronizzazione automatica delle bollette
Come utente voglio che le bollette inserite, modificate o eliminate vengano automaticamente propagate alle altre istanze del mio gruppo, e ricevere le modifiche fatte dagli altri.

### US-003 · Sincronizzazione automatica delle tipologie
Come utente voglio che le tipologie di spesa vengano sincronizzate tra istanze, così che tutte condividano le stesse categorie.

### US-004 · Funzionamento offline
Come utente voglio che l'app funzioni normalmente anche senza MQTT configurato o quando il broker non è raggiungibile.

### US-005 · Test della connessione
Come utente voglio poter verificare la connessione al broker MQTT dalla schermata di configurazione prima di salvare.

## 4. Criteri di accettazione

### US-001
- [ ] Esiste una schermata accessibile dal menu con campi: broker URL, porta, username, password, group ID, toggle TLS, toggle abilitazione
- [ ] La configurazione viene salvata in SharedPreferences
- [ ] La validazione impedisce il salvataggio con campi obbligatori vuoti (broker URL, group ID)

### US-002
- [ ] Inserimento di una bolletta pubblica un messaggio JSON su `sync/{groupId}/bollette/{id}` con retain
- [ ] Modifica pubblica lo stesso messaggio aggiornato
- [ ] Eliminazione pubblica un payload vuoto con retain
- [ ] Ricezione di una bolletta remota la inserisce nel DB locale se non esiste
- [ ] Ricezione di una bolletta con `updatedAt` più recente aggiorna quella locale
- [ ] Ricezione di una bolletta con `updatedAt` meno recente viene ignorata
- [ ] Ricezione di una bolletta con `purchaseTypeId` inesistente viene scartata con log di warning

### US-003
- [ ] Le tipologie seguono lo stesso pattern di publish/subscribe delle bollette
- [ ] Topic: `sync/{groupId}/tipologie/{id}`

### US-004
- [ ] Con MQTT disabilitato, tutte le operazioni CRUD funzionano normalmente
- [ ] Se il broker non è raggiungibile, le operazioni locali non sono bloccate
- [ ] La riconnessione avviene automaticamente in `onResume()` se configurato

### US-005
- [ ] Il pulsante "Test connessione" tenta una connessione e mostra esito positivo/negativo

## 5. Rischi e dipendenze

| Rischio | Probabilità | Impatto | Mitigazione |
|---|---|---|---|
| Conflitto dati con last-write-wins perde modifiche | Media | Medio | Documentare il comportamento; accettabile per app personale/familiare |
| Dipendenza da libreria HiveMQ MQTT client | Bassa | Basso | Libreria matura e stabile (v1.3.3) |
| Migrazione DB (aggiunta `updatedAt`) su dati esistenti | Bassa | Medio | Migrazione Room con default `System.currentTimeMillis()` |
| Pacchetti Netty in conflitto con altre dipendenze | Bassa | Basso | packagingOptions per escludere META-INF duplicati |
| Foreign key: bolletta ricevuta prima della tipologia corrispondente | Media | Basso | Scartare bolletta con FK mancante (log warning), verrà risincronizzata al prossimo publishAll |

## 6. Stima effort

| Area | Giorni/uomo |
|---|---|
| Backend (MqttConfig, MqttSyncManager, migrazione DB) | 2 |
| Frontend (MqttConfigActivity, layout, integrazione Activity) | 1.5 |
| Test manuali (multi-device, offline, conflitti) | 1 |
| Documentazione | 0.5 |
| **Totale** | **5** |

## 7. Milestones

1. **M1 — Migrazione DB:** Aggiungere `updatedAt` a Bolletta e PurchaseType, migrazione Room v3→v4
2. **M2 — MqttConfig:** Classe configurazione con SharedPreferences
3. **M3 — MqttConfigActivity:** UI configurazione con test connessione
4. **M4 — MqttSyncManager:** Singleton con connect/disconnect, publish, subscribe, handler conflitti
5. **M5 — Integrazione Activity:** Chiamate publish dopo ogni CRUD in MainActivity, PurchaseTypeActivity
6. **M6 — Manifest e dipendenze:** Permessi INTERNET/ACCESS_NETWORK_STATE, dipendenze Gradle, packagingOptions
7. **M7 — Test end-to-end:** Verifica su più dispositivi
