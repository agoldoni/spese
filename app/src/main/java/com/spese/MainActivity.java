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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private BollettaAdapter adapter;
    private BollettaDao dao;
    private TextView textListaVuota;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> addEditLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    caricaBollette();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dao = AppDatabase.getInstance(this).bollettaDao();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textListaVuota = findViewById(R.id.text_lista_vuota);

        String[] mesi = getResources().getStringArray(R.array.mesi);
        adapter = new BollettaAdapter(mesi);

        adapter.setOnItemClickListener(this::apriModifica);
        adapter.setOnItemLongClickListener((bolletta, position) -> mostraDialogoEliminazione(bolletta, position));

        RecyclerView recycler = findViewById(R.id.recycler_bollette);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_aggiungi);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddBollettaActivity.class);
            addEditLauncher.launch(intent);
        });

        caricaBollette();
    }

    private void caricaBollette() {
        executor.execute(() -> {
            List<Bolletta> lista = dao.getAll();
            runOnUiThread(() -> {
                adapter.setBollette(lista);
                textListaVuota.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void apriModifica(Bolletta bolletta) {
        Intent intent = new Intent(this, AddBollettaActivity.class);
        intent.putExtra(AddBollettaActivity.EXTRA_BOLLETTA_ID, bolletta.getId());
        intent.putExtra(AddBollettaActivity.EXTRA_BOLLETTA_TIPO, bolletta.getTipo());
        intent.putExtra(AddBollettaActivity.EXTRA_BOLLETTA_IMPORTO, bolletta.getImporto());
        intent.putExtra(AddBollettaActivity.EXTRA_BOLLETTA_MESE, bolletta.getMese());
        intent.putExtra(AddBollettaActivity.EXTRA_BOLLETTA_ANNO, bolletta.getAnno());
        addEditLauncher.launch(intent);
    }

    private void mostraDialogoEliminazione(Bolletta bolletta, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.conferma_eliminazione)
                .setMessage(R.string.messaggio_eliminazione)
                .setNegativeButton(R.string.btn_annulla, null)
                .setPositiveButton(R.string.btn_elimina, (dialog, which) -> {
                    executor.execute(() -> {
                        dao.delete(bolletta);
                        runOnUiThread(() -> {
                            adapter.removeItem(position);
                            textListaVuota.setVisibility(
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
