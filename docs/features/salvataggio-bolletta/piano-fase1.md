# Salvataggio Bolletta — Piano (Fase 1)

**Stato:** In attesa di revisione
**Autore:** agoldoni
**Data:** 2026-03-29

---

## 1. Obiettivo e motivazione

L'app Spese attualmente non ha funzionalità applicative. La prima feature concreta permette all'utente di registrare le bollette domestiche (luce, gas, acqua, rifiuti) con importo e periodo di riferimento, e di visualizzarle in una lista. Questo pone le basi per il tracciamento delle spese ricorrenti e introduce il primo data model e layer di persistenza.

---

## 2. Scope

### Incluso
- Modello dati `Bolletta` con campi: tipo, importo (EUR, 2 decimali), mese, anno
- Persistenza locale (database SQLite tramite Room o accesso diretto)
- Form di inserimento con:
  - Selettore tipo bolletta (luce, gas, rifiuti, acqua)
  - Campo importo numerico con 2 decimali
  - Selettore mese/anno con default al mese corrente
- Lista bollette nella schermata principale con Material Card
- FAB (Floating Action Button) per avviare l'inserimento
- Modifica di una bolletta esistente (tap sulla card per riaprire il form precompilato)
- Eliminazione di una bolletta (swipe o azione nella card, con conferma)
- Filtri, ricerca o ordinamento nella lista
- Export dati o backup
- Grafici o statistiche
- Sincronizzazione cloud

---

## 3. User Stories

**US-001 — Inserimento bolletta**
Come utente voglio inserire una bolletta specificando tipo, importo e periodo per tenere traccia delle mie spese domestiche.

**US-002 — Default mese corrente**
Come utente voglio che il mese/anno sia precompilato con il mese corrente per velocizzare l'inserimento.

**US-003 — Visualizzazione lista bollette**
Come utente voglio vedere le bollette salvate in una lista di card per avere una panoramica delle spese registrate.

**US-004 — Modifica bolletta**
Come utente voglio modificare una bolletta già salvata per correggere errori di inserimento.

**US-005 — Eliminazione bolletta**
Come utente voglio eliminare una bolletta per rimuovere registrazioni errate o duplicate.

---

## 4. Criteri di accettazione

### US-001 — Inserimento bolletta
- [ ] Il form presenta un selettore con le 4 tipologie: luce, gas, rifiuti, acqua
- [ ] Il campo importo accetta solo valori numerici con massimo 2 decimali
- [ ] Il salvataggio persiste i dati in database locale
- [ ] Dopo il salvataggio, l'utente torna alla lista e vede la nuova bolletta

### US-002 — Default mese corrente
- [ ] Il selettore mese/anno è precompilato con mese e anno correnti
- [ ] L'utente può modificare mese e anno prima di salvare

### US-003 — Visualizzazione lista bollette
- [ ] La schermata principale mostra una lista di Material Card
- [ ] Ogni card mostra: tipo bolletta, importo formattato (es. "€ 125,50"), mese/anno
- [ ] La lista è ordinata per data di inserimento (più recenti in alto)
- [ ] Se non ci sono bollette, viene mostrato un messaggio vuoto

### US-004 — Modifica bolletta
- [ ] Tap sulla card apre il form precompilato con i dati della bolletta
- [ ] L'utente può modificare tutti i campi (tipo, importo, mese/anno)
- [ ] Il salvataggio aggiorna il record esistente nel database
- [ ] Dopo il salvataggio, la lista riflette le modifiche

### US-005 — Eliminazione bolletta
- [ ] L'utente può eliminare una bolletta dalla lista (swipe o azione sulla card)
- [ ] Prima dell'eliminazione viene mostrato un dialogo di conferma
- [ ] Dopo la conferma, il record viene rimosso dal database e dalla lista

---

## 5. Rischi e dipendenze

| Rischio | Probabilità | Impatto | Mitigazione |
|---|---|---|---|
| Prima introduzione di un database — scelta architetturale vincolante | Media | Alto | Valutare Room vs SQLite diretto nella Fase 2 |
| Validazione importo con locale italiano (virgola vs punto decimale) | Media | Medio | Usare `InputFilter` e formattazione con `DecimalFormat` |
| RecyclerView + Adapter pattern nuovo nel progetto | Bassa | Basso | Pattern standard Android, ben documentato |

**Dipendenze:**
- Nessuna dipendenza esterna oltre a Room (se scelto) come nuova libreria

---

## 6. Stima effort

**Totale: ~3.5 giorni/uomo**

| Area | Effort | Dettaglio |
|---|---|---|
| BE (model + DB + DAO) | 1.0 gg | Entity, database, DAO con CRUD completo, repository |
| FE (form + lista + card + edit + delete) | 1.5 gg | Layout XML, Activity/Fragment, Adapter, FAB, form riutilizzato per modifica, swipe-to-delete, dialogo conferma |
| Test | 0.75 gg | Test manuale su emulatore, edge case importo, flussi modifica/eliminazione |
| Documentazione | 0.25 gg | Implementation plan, aggiornamento README |

---

## 7. Milestones

| # | Task | Dipende da |
|---|---|---|
| M1 | Definire modello dati `Bolletta` e setup database | — |
| M2 | Creare DAO e repository per operazioni CRUD (insert, getAll, update, delete) | M1 |
| M3 | Layout form inserimento bolletta (`activity_add_bolletta.xml`) | — |
| M4 | Activity di inserimento con validazione e salvataggio | M2, M3 |
| M5 | Layout card bolletta + layout lista (`item_bolletta.xml`) | — |
| M6 | Adapter RecyclerView + integrazione in MainActivity | M2, M5 |
| M7 | FAB in MainActivity per navigare al form | M4, M6 |
| M8 | Modifica bolletta: tap su card riapre il form precompilato | M4, M6 |
| M9 | Eliminazione bolletta: swipe/azione + dialogo di conferma | M6 |
| M10 | Test manuale completo | M8, M9 |
