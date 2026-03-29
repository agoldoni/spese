package com.spese;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.spese.db.AppDatabase;
import com.spese.db.PurchaseType;
import com.spese.db.PurchaseTypeDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PurchaseTypeActivity extends AppCompatActivity {

    private PurchaseTypeAdapter adapter;
    private PurchaseTypeDao purchaseTypeDao;
    private TextView textEmptyList;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean dataChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_type);

        purchaseTypeDao = AppDatabase.getInstance(this).purchaseTypeDao();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        textEmptyList = findViewById(R.id.text_lista_vuota);

        adapter = new PurchaseTypeAdapter();
        adapter.setOnItemClickListener(this::showEditDialog);
        adapter.setOnItemLongClickListener(this::showDeleteDialog);

        RecyclerView recycler = findViewById(R.id.recycler_purchase_types);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_aggiungi);
        fab.setOnClickListener(v -> showAddDialog());

        loadData();
    }

    private void loadData() {
        executor.execute(() -> {
            List<PurchaseType> types = purchaseTypeDao.getAll();
            runOnUiThread(() -> {
                adapter.setItems(types);
                textEmptyList.setVisibility(types.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_purchase_type, null);
        EditText editName = dialogView.findViewById(R.id.edit_name);
        EditText editDescription = dialogView.findViewById(R.id.edit_description);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.aggiungi_tipologia)
                .setView(dialogView)
                .setNegativeButton(R.string.btn_annulla, null)
                .setPositiveButton(R.string.btn_salva, (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) return;
                    String description = editDescription.getText().toString().trim();

                    PurchaseType pt = new PurchaseType(name, description.isEmpty() ? null : description);
                    executor.execute(() -> {
                        try {
                            purchaseTypeDao.insert(pt);
                            MqttSyncManager.getInstance(PurchaseTypeActivity.this).publishPurchaseType(pt);
                            dataChanged = true;
                            runOnUiThread(this::loadData);
                        } catch (Exception e) {
                            runOnUiThread(() -> new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.errore)
                                    .setMessage(R.string.errore_nome_duplicato)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show());
                        }
                    });
                })
                .show();
    }

    private void showEditDialog(PurchaseType purchaseType) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_purchase_type, null);
        EditText editName = dialogView.findViewById(R.id.edit_name);
        EditText editDescription = dialogView.findViewById(R.id.edit_description);

        editName.setText(purchaseType.getName());
        editDescription.setText(purchaseType.getDescription());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.modifica_tipologia)
                .setView(dialogView)
                .setNegativeButton(R.string.btn_annulla, null)
                .setPositiveButton(R.string.btn_salva, (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) return;
                    String description = editDescription.getText().toString().trim();

                    purchaseType.setName(name);
                    purchaseType.setDescription(description.isEmpty() ? null : description);
                    purchaseType.setUpdatedAt(System.currentTimeMillis());
                    executor.execute(() -> {
                        try {
                            purchaseTypeDao.update(purchaseType);
                            MqttSyncManager.getInstance(PurchaseTypeActivity.this).publishPurchaseType(purchaseType);
                            dataChanged = true;
                            runOnUiThread(this::loadData);
                        } catch (Exception e) {
                            runOnUiThread(() -> new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.errore)
                                    .setMessage(R.string.errore_nome_duplicato)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show());
                        }
                    });
                })
                .show();
    }

    private void showDeleteDialog(PurchaseType purchaseType) {
        executor.execute(() -> {
            int count = purchaseTypeDao.countBolletteByPurchaseType(purchaseType.getId());
            runOnUiThread(() -> {
                if (count > 0) {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.errore)
                            .setMessage(getString(R.string.errore_tipologia_in_uso, count))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                } else {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.conferma_eliminazione)
                            .setMessage(R.string.messaggio_eliminazione_tipologia)
                            .setNegativeButton(R.string.btn_annulla, null)
                            .setPositiveButton(R.string.btn_elimina, (dialog, which) -> {
                                executor.execute(() -> {
                                    purchaseTypeDao.delete(purchaseType);
                                    MqttSyncManager.getInstance(PurchaseTypeActivity.this).publishDeletePurchaseType(purchaseType.getId());
                                    dataChanged = true;
                                    runOnUiThread(this::loadData);
                                });
                            })
                            .show();
                }
            });
        });
    }

    @Override
    public void finish() {
        if (dataChanged) {
            setResult(RESULT_OK);
        }
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
