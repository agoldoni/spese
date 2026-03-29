# Anagrafica Tipologia — Piano (Fase 1)

## 1. Obiettivo e motivazione

Sostituire la lista fissa di tipi bolletta (hardcoded in `strings.xml`) con un'entità gestita a DB (`PurchaseType`), permettendo all'utente di creare, modificare ed eliminare tipologie di spesa in autonomia. Questo rende l'app flessibile e personalizzabile senza richiedere aggiornamenti del codice.

## 2. Scope

### Incluso
- Nuova entity Room `PurchaseType` (id UUID, name univoco, description opzionale)
- DAO e query CRUD per `PurchaseType`
- Migrazione DB v2 → v3: creazione tabella + seed dei 4 tipi attuali
- Sostituzione del campo `tipo` (String) in `Bolletta` con `purchaseTypeId` (FK verso `PurchaseType`)
- Aggiornamento dello spinner in `AddBollettaActivity` per caricare i tipi dal DB
- Schermata di gestione anagrafica tipologie (lista + add/edit/delete)
- Vincolo: non si può eliminare un tipo se usato da almeno una bolletta (o soft-delete/avviso)

### Escluso (out of scope)
- Sincronizzazione remota / multi-device
- Ordinamento o categorizzazione gerarchica dei tipi
- Icone o colori associati ai tipi

## 3. User Stories

1. **Come utente** voglio aggiungere un nuovo tipo di spesa personalizzato **per** catalogare bollette con categorie non previste inizialmente.
2. **Come utente** voglio modificare nome e descrizione di un tipo esistente **per** correggere errori o aggiornare la nomenclatura.
3. **Come utente** voglio eliminare un tipo non utilizzato **per** mantenere la lista pulita.
4. **Come utente** voglio selezionare il tipo di spesa da un elenco dinamico quando inserisco una bolletta **per** avere sempre le mie categorie aggiornate.

## 4. Criteri di accettazione

- [ ] La tabella `purchase_types` esiste con colonne `id` (TEXT PK), `name` (TEXT UNIQUE NOT NULL), `description` (TEXT nullable)
- [ ] La migrazione v2→v3 crea la tabella e converte `bollette.tipo` in `bollette.purchaseTypeId` (creando i `PurchaseType` corrispondenti ai tipi esistenti nelle bollette)
- [ ] Lo spinner in AddBolletta mostra i tipi dal DB, non dall'array statico
- [ ] È possibile aggiungere un nuovo tipo con nome univoco
- [ ] È possibile modificare nome e descrizione di un tipo esistente
- [ ] Non è possibile eliminare un tipo associato ad almeno una bolletta (messaggio di errore)
- [ ] È possibile eliminare un tipo non usato
- [ ] L'array `tipi_bolletta` in strings.xml viene rimosso (non più usato)
- [ ] Tutti gli identificatori nel codice Java sono in inglese

## 5. Rischi e dipendenze

| Rischio | Impatto | Mitigazione |
|---|---|---|
| Migrazione dati: mappare il testo `tipo` al UUID del nuovo `PurchaseType` | Alto — dati persi se fallisce | La migrazione crea un `PurchaseType` per ogni valore distinto di `tipo` presente nelle bollette, poi fa UPDATE con subquery per risolvere nome → id |
| Nome tipo duplicato | Basso | Constraint UNIQUE su `name` + validazione UI |

## 6. Stima effort

| Area | Giorni/uomo |
|---|---|
| Entity + DAO + Migrazione DB | 0.5 |
| Refactor Bolletta (tipo → purchaseTypeId) | 0.5 |
| Schermata gestione tipologie (CRUD) | 1 |
| Aggiornamento AddBollettaActivity (spinner da DB) | 0.5 |
| Rinomina identificatori in inglese | 0.5 |
| **Totale** | **3** |

## 7. Milestones

1. **M1 — Modello dati:** Creare `PurchaseType` entity + DAO. Aggiornare `Bolletta` (campo `purchaseTypeId`). Scrivere migrazione v2→v3.
2. **M2 — Spinner dinamico:** Modificare `AddBollettaActivity` per caricare i tipi dal DB invece che da strings.xml. Rimuovere l'array statico.
3. **M3 — Gestione anagrafica:** Activity per lista tipologie con add/edit/delete. Vincolo eliminazione se tipo in uso.
4. **M4 — Rinomina inglese:** Rinominare identificatori Java in inglese in tutti i file toccati.
