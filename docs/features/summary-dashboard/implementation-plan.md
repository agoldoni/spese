# Summary Dashboard — Implementation Plan

**Stato:** Bozza — in attesa di approvazione
**Autore:** Claude Code
**Data:** 2026-03-29
**Versione:** 1.0

---

## 1. Executive Summary

La Summary Dashboard è una nuova vista dell'app Spese che mostra i totali delle bollette aggregati per anno o per anno/tipologia. L'utente accede dal menu principale e può scegliere il tipo di raggruppamento tramite un dropdown. Questa feature risponde alla necessità di avere una visione sintetica delle spese senza dover scorrere manualmente l'elenco delle singole bollette. La struttura è progettata per essere estensibile con ulteriori raggruppamenti futuri.

---

## 2. Obiettivo e motivazione

- **Problema che risolve:** L'app mostra solo una lista cronologica delle bollette. L'utente non ha modo di capire a colpo d'occhio quanto spende per anno o per categoria senza fare calcoli manuali.
- **Metriche di successo:**
  - [ ] L'utente riesce a consultare il totale annuale in meno di 3 tap dalla schermata principale
  - [ ] I totali mostrati corrispondono alla somma reale delle bollette inserite
- **Legame con obiettivi di prodotto:** Feature di base per trasformare l'app da semplice registro a strumento di analisi delle spese personali

---

## 3. Scope

### Incluso
- Nuova `SummaryActivity` accessibile dal menu overflow di `MainActivity`
- Dropdown per selezionare la modalità di raggruppamento: "Per anno", "Per anno/tipologia"
- Modalità "Per anno": somma di tutte le bollette raggruppate per anno, ordine decrescente
- Modalità "Per anno/tipologia": secondo dropdown per selezionare la tipologia, totale filtrato per anno
- POJO `YearlySummary` per i risultati delle query aggregate Room
- Adapter dedicato per la RecyclerView dei risultati
- Gestione stato vuoto ("Nessun dato disponibile")

### Escluso (out of scope)
- Grafici o chart — sarà una feature separata in futuro
- Esportazione dati — fuori dal perimetro corrente
- Filtri per intervallo di mesi — complessità non giustificata in questa fase
- Altre combinazioni di raggruppamento — verranno aggiunte incrementalmente

### Decisioni aperte

Nessuna decisione aperta.

---

## 4. User Stories e criteri di accettazione

### US-001 · Riepilogo annuale
**Priorità:** Must Have

Come utente voglio vedere il totale delle mie spese per ogni anno per avere una visione d'insieme dell'andamento annuale.

**Criteri di accettazione:**
- [ ] La lista mostra una riga per ogni anno con bollette, nel formato `Anno | Totale €`
- [ ] Gli anni sono ordinati dal più recente al più vecchio
- [ ] Gli importi sono formattati con separatore decimale virgola e simbolo €

### US-002 · Riepilogo per tipologia
**Priorità:** Must Have

Come utente voglio filtrare il riepilogo per una specifica tipologia di spesa per capire quanto spendo in quella categoria anno per anno.

**Criteri di accettazione:**
- [ ] Selezionando "Per anno/tipologia" appare un secondo dropdown con tutte le tipologie
- [ ] Il secondo dropdown è visibile solo in questa modalità
- [ ] La lista mostra i totali annuali filtrati per la tipologia selezionata
- [ ] Cambiando tipologia la lista si aggiorna immediatamente

### US-003 · Accesso dal menu
**Priorità:** Must Have

Come utente voglio accedere al riepilogo dal menu principale per consultarlo rapidamente.

**Criteri di accettazione:**
- [ ] La voce "Riepilogo" appare nel menu overflow di MainActivity
- [ ] Toccando "Riepilogo" si apre la SummaryActivity
- [ ] La toolbar mostra il titolo "Riepilogo" con freccia back

### US-004 · Stato vuoto
**Priorità:** Should Have

Come utente voglio vedere un messaggio chiaro quando non ci sono dati per il raggruppamento selezionato.

**Criteri di accettazione:**
- [ ] Se non ci sono bollette, viene mostrato "Nessun dato disponibile"
- [ ] Il messaggio appare anche quando si seleziona una tipologia senza bollette associate

---

## 5. Architettura tecnica

### Componenti coinvolti

```
MainActivity (menu) → SummaryActivity
                          ├── Spinner raggruppamento
                          ├── Spinner tipologia (condizionale)
                          ├── RecyclerView + SummaryAdapter
                          └── TextView stato vuoto

SummaryActivity → BollettaDao.getTotalByYear()
                → BollettaDao.getTotalByYearAndType(typeId)
                → PurchaseTypeDao.getAll()

BollettaDao → [query SQL aggregate] → YearlySummary (POJO)
```

### Modifiche al data model

| Tabella/Tipo | Tipo modifica | Dettaglio |
|---|---|---|
| `YearlySummary` | Nuovo (POJO, non entity) | Classe con campi `year` (int) e `total` (double) per ricevere i risultati delle query aggregate Room |

Nessuna modifica allo schema del database. Nessuna migrazione necessaria.

### Nuove API o endpoint

Non applicabile (app nativa Android senza backend).

---

## 6. Piano di implementazione

| ID | Task | Area | Stima (gg) | Dipende da | Responsabile |
|---|---|---|---|---|---|
| T-01 | Creare `YearlySummary.java` — POJO con campi `year` e `total` | BE | 0.1 | — | Dev |
| T-02 | Aggiungere query aggregate in `BollettaDao` (`getTotalByYear`, `getTotalByYearAndType`) | BE | 0.2 | T-01 | Dev |
| T-03 | Aggiungere stringhe in `strings.xml` (titolo, opzioni dropdown, stato vuoto) | FE | 0.1 | — | Dev |
| T-04 | Creare layout `activity_summary.xml` (toolbar, 2 spinner, RecyclerView, testo vuoto) | FE | 0.3 | T-03 | Dev |
| T-05 | Creare layout `item_summary.xml` (riga anno + totale, stile MaterialCardView) | FE | 0.1 | — | Dev |
| T-06 | Creare `SummaryAdapter.java` (RecyclerView.Adapter per `YearlySummary`) | FE | 0.3 | T-05 | Dev |
| T-07 | Creare `SummaryActivity.java` (logica spinner, caricamento dati, switch modalità) | FE | 0.8 | T-02, T-04, T-06 | Dev |
| T-08 | Aggiungere voce "Riepilogo" in `main_menu.xml` e gestire click in `MainActivity` | FE | 0.1 | T-07 | Dev |
| T-09 | Registrare `SummaryActivity` in `AndroidManifest.xml` | FE | 0.05 | T-07 | Dev |
| T-10 | Test manuale su dispositivo | Test | 0.5 | T-08, T-09 | Dev |

**Stima totale:** 2.5 giorni/uomo
**Breakdown:** BE 0.3gg · FE 1.7gg · Test 0.5gg

---

## 7. Piano di test

**Strategia generale:** Test manuale su dispositivo/emulatore (nessun framework di test automatico configurato nel progetto).

### Test cases critici

| ID | Tipo | Descrizione | Priorità |
|---|---|---|---|
| TC-01 | Manuale | Aprire Riepilogo senza bollette → messaggio "Nessun dato disponibile" | Alta |
| TC-02 | Manuale | Inserire bollette su 3 anni diversi → verificare somme corrette per anno | Alta |
| TC-03 | Manuale | Modalità "Per anno" → verificare ordine anni decrescente | Alta |
| TC-04 | Manuale | Switch a "Per anno/tipologia" → verificare apparizione dropdown tipologia | Alta |
| TC-05 | Manuale | Selezionare tipologia con bollette → verificare totali filtrati corretti | Alta |
| TC-06 | Manuale | Selezionare tipologia senza bollette → messaggio vuoto | Media |
| TC-07 | Manuale | Verificare formattazione importi (virgola decimale, simbolo €) | Media |
| TC-08 | Manuale | Navigazione back dalla toolbar → ritorno a MainActivity | Bassa |

### Definition of Done

- [ ] Tutti i test manuali TC-01 — TC-08 superati
- [ ] Nessun crash o ANR durante l'uso
- [ ] Layout corretto su schermi diversi (phone portrait)
- [ ] I totali corrispondono alla somma reale delle bollette nel database

---

## 8. Rischi e mitigazioni

| Rischio | Probabilità | Impatto | Mitigazione |
|---|---|---|---|
| Errori di arrotondamento `double` nelle somme aggregate | Bassa | Basso | SQLite `SUM()` su `REAL` è preciso per 2 decimali su importi realistici; formattazione con `DecimalFormat("#,##0.00")` come nel resto dell'app |
| Dropdown tipologia non aggiornato dopo modifica tipologie | Bassa | Basso | SummaryActivity ricarica le tipologie in `onResume()` oppure all'apertura |

---

## 9. Rollout e feature flag

**Strategia di rilascio:**
- [x] Deploy diretto (direct)

Non sono necessari feature flag. La feature è autocontenuta e non impatta le funzionalità esistenti. Il rollback consiste nel rimuovere la voce menu.

---

## 10. Checklist di approvazione

| Revisione | Responsabile | Stato | Data |
|---|---|---|---|
| Revisione tecnica | Dev | ⏳ In attesa | — |
| Revisione prodotto | Utente | ⏳ In attesa | — |
| Stima approvata | Dev | ⏳ In attesa | — |

---

## Domande aperte

Nessuna domanda aperta. La feature è ben definita e tutti i dati necessari sono già presenti nel database.

---

*Documento generato con la skill `claude-code-feature`.*
