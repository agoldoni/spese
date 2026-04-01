package com.spese;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.spese.db.AppDatabase;
import com.spese.db.Bolletta;
import com.spese.db.BollettaDao;
import com.spese.db.PurchaseType;
import com.spese.db.PurchaseTypeDao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private BollettaAdapter adapter;
    private BollettaDao bollettaDao;
    private PurchaseTypeDao purchaseTypeDao;
    private TextView textEmptyList;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> addEditLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadData();
                }
            });

    private final ActivityResultLauncher<Intent> purchaseTypeLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadData();
                }
            });

    private final ActivityResultLauncher<Intent> importLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null
                        && result.getData().getData() != null) {
                    importData(result.getData().getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppDatabase db = AppDatabase.getInstance(this);
        bollettaDao = db.bollettaDao();
        purchaseTypeDao = db.purchaseTypeDao();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textEmptyList = findViewById(R.id.text_lista_vuota);

        String[] months = getResources().getStringArray(R.array.mesi);
        adapter = new BollettaAdapter(months);

        adapter.setOnItemClickListener(this::openEdit);
        adapter.setOnItemLongClickListener((bolletta, position) -> showDeleteDialog(bolletta, position));

        RecyclerView recycler = findViewById(R.id.recycler_bollette);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_aggiungi);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddBollettaActivity.class);
            addEditLauncher.launch(intent);
        });

        MqttSyncManager.getInstance(this).setOnSyncDataReceivedListener(this::loadData);

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MqttSyncManager.getInstance(this).reconnectIfNeeded();
    }

    private void loadData() {
        executor.execute(() -> {
            List<PurchaseType> types = purchaseTypeDao.getAll();
            List<Bolletta> bills = bollettaDao.getAll();
            runOnUiThread(() -> {
                adapter.setPurchaseTypes(types);
                adapter.setBollette(bills);
                textEmptyList.setVisibility(bills.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void openEdit(Bolletta bolletta) {
        Intent intent = new Intent(this, AddBollettaActivity.class);
        intent.putExtra(AddBollettaActivity.EXTRA_BOLLETTA_ID, bolletta.getId());
        intent.putExtra(AddBollettaActivity.EXTRA_BOLLETTA_PURCHASE_TYPE_ID, bolletta.getPurchaseTypeId());
        intent.putExtra(AddBollettaActivity.EXTRA_BOLLETTA_AMOUNT, bolletta.getAmount());
        intent.putExtra(AddBollettaActivity.EXTRA_BOLLETTA_MONTH, bolletta.getMonth());
        intent.putExtra(AddBollettaActivity.EXTRA_BOLLETTA_YEAR, bolletta.getYear());
        intent.putExtra(AddBollettaActivity.EXTRA_BOLLETTA_CREATED_AT, bolletta.getCreatedAt());
        addEditLauncher.launch(intent);
    }

    private void showDeleteDialog(Bolletta bolletta, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.conferma_eliminazione)
                .setMessage(R.string.messaggio_eliminazione)
                .setNegativeButton(R.string.btn_annulla, null)
                .setPositiveButton(R.string.btn_elimina, (dialog, which) -> {
                    executor.execute(() -> {
                        bollettaDao.delete(bolletta);
                        MqttSyncManager.getInstance(MainActivity.this).publishDeleteBolletta(bolletta.getId());
                        runOnUiThread(() -> {
                            adapter.removeItem(position);
                            textEmptyList.setVisibility(
                                    adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                        });
                    });
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_esporta) {
            shareData();
            return true;
        }
        if (item.getItemId() == R.id.action_importa) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*");
            importLauncher.launch(intent);
            return true;
        }
        if (item.getItemId() == R.id.action_condivisione) {
            startActivity(new Intent(this, MqttConfigActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_info) {
            startActivity(new Intent(this, InfoActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_purchase_types) {
            Intent intent = new Intent(this, PurchaseTypeActivity.class);
            purchaseTypeLauncher.launch(intent);
            return true;
        }
        if (item.getItemId() == R.id.action_riepilogo) {
            startActivity(new Intent(this, SummaryActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareData() {
        executor.execute(() -> {
            List<Bolletta> bollette = bollettaDao.getAll();
            List<PurchaseType> types = purchaseTypeDao.getAll();

            if (bollette.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.nessuna_bolletta_da_esportare, Toast.LENGTH_SHORT).show());
                return;
            }

            Map<String, String> typeNames = new HashMap<>();
            for (PurchaseType t : types) {
                typeNames.put(t.getId(), t.getName());
            }

            String[] mesi = getResources().getStringArray(R.array.mesi);
            String exportDate = new SimpleDateFormat("yyyyMMdd", Locale.ITALY).format(new Date());
            String fileName = "spese_" + exportDate + ".csv";
            File csvFile = new File(getCacheDir(), fileName);

            try (FileWriter writer = new FileWriter(csvFile)) {
                writer.write("Id,Tipologia,Importo,Mese,Anno\n");

                for (Bolletta b : bollette) {
                    String tipologia = typeNames.getOrDefault(b.getPurchaseTypeId(), "");
                    String meseNome = (b.getMonth() >= 1 && b.getMonth() <= 12)
                            ? mesi[b.getMonth() - 1] : String.valueOf(b.getMonth());

                    writer.write(String.format(Locale.US, "%s,%s,%.2f,%s,%d\n",
                            b.getId(),
                            tipologia.replace(",", " "),
                            b.getAmount(),
                            meseNome,
                            b.getYear()));
                }

                runOnUiThread(() -> {
                    Uri uri = FileProvider.getUriForFile(this,
                            getApplicationContext().getPackageName() + ".fileprovider",
                            csvFile);

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/csv");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent,
                            getString(R.string.condividi_bollette)));
                });

            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.errore_esportazione, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void importData(Uri uri) {
        executor.execute(() -> {
            int importati = 0;
            int duplicati = 0;
            int errori = 0;
            Map<String, String> typeCache = new HashMap<>();

            try (InputStream is = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                String header = reader.readLine();
                if (header == null || !header.startsWith("Id,Tipologia,")) {
                    runOnUiThread(() -> Toast.makeText(this,
                            R.string.file_non_valido, Toast.LENGTH_LONG).show());
                    return;
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    try {
                        String[] fields = line.split(",", -1);
                        if (fields.length < 5) {
                            errori++;
                            continue;
                        }

                        String id = fields[0].trim();
                        String tipologiaNome = fields[1].trim();
                        double importo = Double.parseDouble(fields[2].trim());
                        String meseStr = fields[3].trim();
                        int anno = Integer.parseInt(fields[4].trim());

                        if (bollettaDao.getById(id) != null) {
                            duplicati++;
                            continue;
                        }

                        // Risolvi mese (nome o numero)
                        int mese = parseMese(meseStr);
                        if (mese < 1 || mese > 12) {
                            errori++;
                            continue;
                        }

                        // Cerca o crea tipologia
                        String typeId = typeCache.get(tipologiaNome);
                        if (typeId == null) {
                            PurchaseType type = purchaseTypeDao.getByName(tipologiaNome);
                            if (type == null) {
                                type = new PurchaseType(tipologiaNome, null);
                                purchaseTypeDao.insert(type);
                            }
                            typeId = type.getId();
                            typeCache.put(tipologiaNome, typeId);
                        }

                        Bolletta b = new Bolletta(typeId, importo, mese, anno);
                        b.setId(id);
                        bollettaDao.insert(b);
                        importati++;

                    } catch (NumberFormatException e) {
                        errori++;
                    }
                }

            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this,
                        R.string.errore_importazione, Toast.LENGTH_LONG).show());
                return;
            }

            final int finalImportati = importati;
            final int finalDuplicati = duplicati;
            final int finalErrori = errori;
            runOnUiThread(() -> {
                Toast.makeText(this,
                        getString(R.string.importazione_completata,
                                finalImportati, finalDuplicati, finalErrori),
                        Toast.LENGTH_LONG).show();
                loadData();
            });
        });
    }

    private int parseMese(String meseStr) {
        String[] mesi = getResources().getStringArray(R.array.mesi);
        for (int i = 0; i < mesi.length; i++) {
            if (mesi[i].equalsIgnoreCase(meseStr)) {
                return i + 1;
            }
        }
        try {
            return Integer.parseInt(meseStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
