package com.spese;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.spese.db.AppDatabase;
import com.spese.db.Bolletta;
import com.spese.db.BollettaDao;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddBollettaActivity extends AppCompatActivity {

    public static final String EXTRA_BOLLETTA_ID = "bolletta_id";
    public static final String EXTRA_BOLLETTA_TIPO = "bolletta_tipo";
    public static final String EXTRA_BOLLETTA_IMPORTO = "bolletta_importo";
    public static final String EXTRA_BOLLETTA_MESE = "bolletta_mese";
    public static final String EXTRA_BOLLETTA_ANNO = "bolletta_anno";

    private AutoCompleteTextView spinnerTipo;
    private TextInputEditText editImporto;
    private AutoCompleteTextView spinnerMese;
    private TextInputEditText editAnno;
    private TextInputLayout layoutImporto;

    private BollettaDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private long editId = -1;
    private boolean isEdit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bolletta);

        dao = AppDatabase.getInstance(this).bollettaDao();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        spinnerTipo = findViewById(R.id.spinner_tipo);
        editImporto = findViewById(R.id.edit_importo);
        spinnerMese = findViewById(R.id.spinner_mese);
        editAnno = findViewById(R.id.edit_anno);
        layoutImporto = findViewById(R.id.layout_importo);
        MaterialButton btnSalva = findViewById(R.id.btn_salva);

        setupTipoSpinner();
        setupMeseSpinner();

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_BOLLETTA_ID)) {
            isEdit = true;
            editId = intent.getLongExtra(EXTRA_BOLLETTA_ID, -1);
            toolbar.setTitle(R.string.modifica_bolletta);

            spinnerTipo.setText(intent.getStringExtra(EXTRA_BOLLETTA_TIPO), false);
            editImporto.setText(String.valueOf(intent.getDoubleExtra(EXTRA_BOLLETTA_IMPORTO, 0)));

            int mese = intent.getIntExtra(EXTRA_BOLLETTA_MESE, 1);
            String[] mesi = getResources().getStringArray(R.array.mesi);
            if (mese >= 1 && mese <= 12) {
                spinnerMese.setText(mesi[mese - 1], false);
            }
            editAnno.setText(String.valueOf(intent.getIntExtra(EXTRA_BOLLETTA_ANNO, Calendar.getInstance().get(Calendar.YEAR))));
        } else {
            Calendar cal = Calendar.getInstance();
            String[] mesi = getResources().getStringArray(R.array.mesi);
            spinnerMese.setText(mesi[cal.get(Calendar.MONTH)], false);
            editAnno.setText(String.valueOf(cal.get(Calendar.YEAR)));
        }

        btnSalva.setOnClickListener(v -> salva());
    }

    private void setupTipoSpinner() {
        String[] tipi = getResources().getStringArray(R.array.tipi_bolletta);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, tipi);
        spinnerTipo.setAdapter(adapter);
        spinnerTipo.setText(tipi[0], false);
    }

    private void setupMeseSpinner() {
        String[] mesi = getResources().getStringArray(R.array.mesi);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, mesi);
        spinnerMese.setAdapter(adapter);
    }

    private void salva() {
        String importoStr = editImporto.getText() != null ? editImporto.getText().toString().trim() : "";
        String annoStr = editAnno.getText() != null ? editAnno.getText().toString().trim() : "";

        double importo;
        try {
            importo = Double.parseDouble(importoStr.replace(",", "."));
        } catch (NumberFormatException e) {
            layoutImporto.setError(getString(R.string.errore_importo));
            return;
        }

        if (importo <= 0) {
            layoutImporto.setError(getString(R.string.errore_importo_zero));
            return;
        }

        layoutImporto.setError(null);

        importo = Math.round(importo * 100.0) / 100.0;

        String tipo = spinnerTipo.getText().toString();
        int mese = getMeseSelezionato();
        int anno;
        try {
            anno = Integer.parseInt(annoStr);
        } catch (NumberFormatException e) {
            anno = Calendar.getInstance().get(Calendar.YEAR);
        }

        if (isEdit) {
            Bolletta bolletta = new Bolletta(tipo, importo, mese, anno);
            bolletta.setId(editId);
            executor.execute(() -> {
                dao.update(bolletta);
                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            });
        } else {
            Bolletta bolletta = new Bolletta(tipo, importo, mese, anno);
            executor.execute(() -> {
                dao.insert(bolletta);
                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            });
        }
    }

    private int getMeseSelezionato() {
        String meseText = spinnerMese.getText().toString();
        String[] mesi = getResources().getStringArray(R.array.mesi);
        for (int i = 0; i < mesi.length; i++) {
            if (mesi[i].equals(meseText)) return i + 1;
        }
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
