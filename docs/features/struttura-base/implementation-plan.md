# Struttura Base — Implementation Plan

**Stato:** Bozza — in attesa di approvazione
**Autore:** agoldoni
**Data:** 2026-03-28
**Versione:** 1.0

---

## 1. Executive Summary

Creazione della struttura base dell'app Android "Spese" in Java con layout XML.
L'app non contiene funzionalità applicative: fornisce uno scaffold pulito con una
MainActivity, un menu opzioni con la voce "Info" e una schermata informativa.
Serve come fondamenta per lo sviluppo futuro.

---

## 2. Obiettivo e motivazione

- **Problema che risolve:** Il progetto non esiste ancora. Serve una base solida e
  coerente su cui costruire le funzionalità successive.
- **Metriche di successo:**
  - [ ] L'app si avvia senza crash su emulatore API 26+
  - [ ] Il menu funziona correttamente e la schermata Info è raggiungibile
- **Legame con obiettivi di prodotto:** Prerequisito per qualsiasi sviluppo futuro dell'app Spese

---

## 3. Scope

### Incluso
- Progetto Android (Java, XML, Gradle)
- MainActivity con toolbar e menu
- Voce di menu "Info"
- InfoActivity con nome app e versione
- Tema Material Design 3

### Escluso (out of scope)
- Funzionalità applicative (gestione spese, database, ecc.) — saranno feature successive
- Test automatici — non c'è logica da testare in questa fase
- Pubblicazione su Play Store — prematura

---

## 4. User Stories e criteri di accettazione

### US-001 · Avvio app
**Priorità:** Must Have

Come utente voglio aprire l'app e vedere una schermata principale per sapere che l'app funziona.

**Criteri di accettazione:**
- [ ] L'app si avvia senza crash su emulatore API 26+
- [ ] La schermata principale mostra il titolo "Spese"
- [ ] La toolbar è visibile con il nome dell'app

### US-002 · Menu con voce Info
**Priorità:** Must Have

Come utente voglio accedere al menu e trovare la voce "Info" per consultare le informazioni sull'app.

**Criteri di accettazione:**
- [ ] Il menu opzioni si apre toccando l'icona (tre puntini)
- [ ] La voce "Info" è presente nel menu
- [ ] Toccando "Info" si apre la schermata informativa

### US-003 · Schermata Info
**Priorità:** Must Have

Come utente voglio visualizzare una schermata Info con nome app e versione per sapere cosa sto usando.

**Criteri di accettazione:**
- [ ] La schermata mostra il nome dell'app
- [ ] La schermata mostra la versione (letta da BuildConfig)
- [ ] È possibile tornare alla schermata principale con il tasto back

---

## 5. Architettura tecnica

### Componenti coinvolti

```
  Launcher → [MainActivity] → menu tap → [InfoActivity]
                  ↑                            ↑
           activity_main.xml            activity_info.xml
```

### Struttura del progetto

```
app/
├── src/main/
│   ├── java/com/spese/
│   │   ├── MainActivity.java
│   │   └── InfoActivity.java
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml
│   │   │   └── activity_info.xml
│   │   ├── menu/
│   │   │   └── main_menu.xml
│   │   └── values/
│   │       ├── strings.xml
│   │       └── themes.xml
│   └── AndroidManifest.xml
├── build.gradle
build.gradle (root)
settings.gradle
gradle.properties
```

### Modifiche al data model

Nessuna — non è previsto alcun database in questa fase.

### Nuove API o endpoint

Nessuna — app standalone senza comunicazione di rete.

---

## 6. Piano di implementazione

| ID | Task | Area | Stima (gg) | Dipende da | Responsabile |
|---|---|---|---|---|---|
| T-01 | Setup progetto Gradle con struttura cartelle | Infra | 0.1 | — | agoldoni |
| T-02 | Configurazione build.gradle (root + app) | Infra | 0.1 | T-01 | agoldoni |
| T-03 | AndroidManifest.xml con dichiarazione Activity | Infra | 0.05 | T-01 | agoldoni |
| T-04 | Tema Material 3 e strings.xml | FE | 0.05 | T-01 | agoldoni |
| T-05 | MainActivity + activity_main.xml | FE | 0.1 | T-02, T-04 | agoldoni |
| T-06 | Menu XML con voce "Info" | FE | 0.05 | T-05 | agoldoni |
| T-07 | InfoActivity + activity_info.xml | FE | 0.1 | T-04 | agoldoni |
| T-08 | Test manuale su emulatore | Test | 0.2 | T-05, T-06, T-07 | agoldoni |

**Stima totale:** 0.75 giorni/uomo
**Breakdown:** FE 0.3gg · Infra 0.25gg · Test 0.2gg

---

## 7. Piano di test

**Strategia generale:** Test manuale su emulatore. Non è prevista automazione
per uno scaffold senza logica di business.

### Test cases critici

| ID | Tipo | Descrizione | Priorità |
|---|---|---|---|
| TC-01 | Manuale | L'app si avvia senza crash | Alta |
| TC-02 | Manuale | Il menu si apre e contiene "Info" | Alta |
| TC-03 | Manuale | Tap su "Info" apre InfoActivity | Alta |
| TC-04 | Manuale | InfoActivity mostra nome e versione app | Alta |
| TC-05 | Manuale | Back da InfoActivity torna a MainActivity | Media |

### Definition of Done

- [ ] L'app compila senza errori e senza warning
- [ ] Tutti i test manuali (TC-01 → TC-05) superati su emulatore API 26+
- [ ] Nessuna stringa hardcoded nel codice (tutto in strings.xml)

---

## 8. Rischi e mitigazioni

| Rischio | Probabilità | Impatto | Mitigazione |
|---|---|---|---|
| Incompatibilità versioni Gradle/AGP/SDK | Bassa | Medio | Usare combinazioni di versioni testate e documentate |

---

## 9. Rollout e feature flag

**Strategia di rilascio:**
- [x] Deploy diretto (direct)

Non sono previsti feature flag né rollback — si tratta dello scaffold iniziale del progetto.

---

## 10. Checklist di approvazione

| Revisione | Responsabile | Stato | Data |
|---|---|---|---|
| Revisione tecnica | agoldoni | :hourglass: In attesa | — |
| Stima approvata | agoldoni | :hourglass: In attesa | — |

---

## Domande aperte

Nessuna domanda aperta — il progetto è sufficientemente definito per procedere.

---

*Documento generato con la skill `claude-code-feature`.*
