# Summary Dashboard — Piano (Fase 1)

## 1. Obiettivo e motivazione

L'app Spese attualmente mostra solo una lista cronologica delle bollette. Manca una vista sintetica che permetta all'utente di capire **quanto spende** in un determinato periodo. La Summary Dashboard fornisce aggregazioni per anno e per anno/tipologia, dando una visione d'insieme delle spese. La struttura è pensata per essere estensibile con ulteriori raggruppamenti futuri.

## 2. Scope

### Incluso
- Nuova Activity con una vista riepilogativa
- Combo (Spinner/Dropdown) per selezionare la modalità di raggruppamento
- **Modalità "Per anno"**: somma di tutte le bollette raggruppate per anno, ordine decrescente
- **Modalità "Per anno/tipologia"**: come sopra ma filtrata per una specifica tipologia (secondo selettore visibile solo in questa modalità)
- Navigazione dalla MainActivity (voce nel menu overflow)
- Query di aggregazione nel DAO

### Escluso (out of scope)
- Grafici o chart
- Esportazione dati
- Filtri per intervallo di mesi
- Altre combinazioni di raggruppamento (verranno aggiunte in futuro)

## 3. User Stories

1. **Come utente** voglio vedere il totale delle mie spese per ogni anno **per** avere una visione d'insieme dell'andamento annuale.
2. **Come utente** voglio filtrare il riepilogo per una specifica tipologia di spesa **per** capire quanto spendo in quella categoria anno per anno.
3. **Come utente** voglio accedere al riepilogo dal menu principale **per** consultarlo rapidamente senza navigazioni complesse.
4. **Come utente** voglio che gli anni siano ordinati dal più recente al più vecchio **per** vedere subito i dati più rilevanti.

## 4. Criteri di accettazione

- [ ] La voce "Riepilogo" appare nel menu overflow di MainActivity
- [ ] L'activity mostra un dropdown con le opzioni: "Per anno", "Per anno/tipologia"
- [ ] Selezionando "Per anno" viene mostrata una lista con righe `Anno | Totale €`
- [ ] Selezionando "Per anno/tipologia" appare un secondo dropdown con le tipologie disponibili; la lista mostra `Anno | Totale €` filtrato per quella tipologia
- [ ] Gli importi sono formattati con separatore decimale virgola e simbolo €
- [ ] Gli anni sono in ordine decrescente
- [ ] Se non ci sono dati, viene mostrato un messaggio "Nessun dato disponibile"
- [ ] Il secondo dropdown è visibile solo nella modalità "Per anno/tipologia"

## 5. Rischi e dipendenze

| Rischio | Impatto | Mitigazione |
|---------|---------|-------------|
| Performance query su dataset grandi | Basso (uso personale) | Le query aggregate su SQLite sono efficienti; monitorare se necessario |
| Aggiunta futura di nuovi raggruppamenti | Medio | Progettare la UI e il DAO in modo che sia semplice aggiungere nuove modalità |
| Tipologia eliminata con bollette orfane | Nullo | Il FK con RESTRICT impedisce questa situazione |

**Dipendenze:** Nessuna — i dati necessari (anno, importo, tipologia) sono già presenti nella tabella `bollette`.

## 6. Stima effort

| Area | Giorni/uomo |
|------|-------------|
| Backend (DAO + query) | 0.5 |
| Frontend (Activity + layout + adapter) | 1.5 |
| Test manuale | 0.5 |
| **Totale** | **2.5** |

## 7. Milestones

1. Aggiungere le query aggregate in `BollettaDao`
2. Creare il model per i risultati aggregati (POJO per Room)
3. Creare il layout XML della SummaryActivity
4. Implementare la SummaryActivity con logica di switch tra modalità
5. Creare l'adapter per la RecyclerView dei risultati
6. Aggiungere la voce menu in MainActivity e la navigazione
7. Aggiungere le stringhe in `strings.xml`
8. Test manuale su dispositivo
