package com.spese;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.spese.db.AppDatabase;
import com.spese.db.BollettaDao;
import com.spese.db.PurchaseType;
import com.spese.db.PurchaseTypeDao;
import com.spese.db.YearlySummary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SummaryActivity extends AppCompatActivity {

    private static final int MODE_PER_ANNO = 0;
    private static final int MODE_PER_ANNO_TIPOLOGIA = 1;

    private BollettaDao bollettaDao;
    private PurchaseTypeDao purchaseTypeDao;
    private SummaryAdapter adapter;
    private TextView textVuoto;
    private Spinner spinnerModalita;
    private Spinner spinnerTipologia;

    private final List<PurchaseType> purchaseTypes = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        AppDatabase db = AppDatabase.getInstance(this);
        bollettaDao = db.bollettaDao();
        purchaseTypeDao = db.purchaseTypeDao();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        textVuoto = findViewById(R.id.text_vuoto);
        adapter = new SummaryAdapter();

        RecyclerView recycler = findViewById(R.id.recycler_summary);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        spinnerModalita = findViewById(R.id.spinner_modalita);
        spinnerTipologia = findViewById(R.id.spinner_tipologia);

        spinnerModalita.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onModeChanged(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerTipologia.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerModalita.getSelectedItemPosition() == MODE_PER_ANNO_TIPOLOGIA) {
                    loadByYearAndType(purchaseTypes.get(position).getId());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadPurchaseTypes();
    }

    private void loadPurchaseTypes() {
        executor.execute(() -> {
            List<PurchaseType> types = purchaseTypeDao.getAll();
            runOnUiThread(() -> {
                purchaseTypes.clear();
                purchaseTypes.addAll(types);

                List<String> names = new ArrayList<>();
                for (PurchaseType pt : types) {
                    names.add(pt.getName());
                }
                ArrayAdapter<String> typesAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, names);
                typesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTipologia.setAdapter(typesAdapter);

                loadByYear();
            });
        });
    }

    private void onModeChanged(int mode) {
        if (mode == MODE_PER_ANNO) {
            spinnerTipologia.setVisibility(View.GONE);
            loadByYear();
        } else if (mode == MODE_PER_ANNO_TIPOLOGIA) {
            spinnerTipologia.setVisibility(View.VISIBLE);
            if (!purchaseTypes.isEmpty()) {
                int selectedIndex = spinnerTipologia.getSelectedItemPosition();
                if (selectedIndex < 0) selectedIndex = 0;
                loadByYearAndType(purchaseTypes.get(selectedIndex).getId());
            } else {
                showData(new ArrayList<>());
            }
        }
    }

    private void loadByYear() {
        executor.execute(() -> {
            List<YearlySummary> data = bollettaDao.getTotalByYear();
            runOnUiThread(() -> showData(data));
        });
    }

    private void loadByYearAndType(String typeId) {
        executor.execute(() -> {
            List<YearlySummary> data = bollettaDao.getTotalByYearAndType(typeId);
            runOnUiThread(() -> showData(data));
        });
    }

    private void showData(List<YearlySummary> data) {
        adapter.setItems(data);
        textVuoto.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
