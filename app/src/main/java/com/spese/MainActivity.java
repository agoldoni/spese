package com.spese;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.List;
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

        loadData();
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
        if (item.getItemId() == R.id.action_info) {
            startActivity(new Intent(this, InfoActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_purchase_types) {
            Intent intent = new Intent(this, PurchaseTypeActivity.class);
            purchaseTypeLauncher.launch(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
