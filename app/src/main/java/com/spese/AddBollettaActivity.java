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
import com.spese.db.PurchaseType;
import com.spese.db.PurchaseTypeDao;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddBollettaActivity extends AppCompatActivity {

    public static final String EXTRA_BOLLETTA_ID = "bolletta_id";
    public static final String EXTRA_BOLLETTA_PURCHASE_TYPE_ID = "bolletta_purchase_type_id";
    public static final String EXTRA_BOLLETTA_AMOUNT = "bolletta_amount";
    public static final String EXTRA_BOLLETTA_MONTH = "bolletta_month";
    public static final String EXTRA_BOLLETTA_YEAR = "bolletta_year";
    public static final String EXTRA_BOLLETTA_CREATED_AT = "bolletta_created_at";

    private AutoCompleteTextView spinnerType;
    private TextInputEditText editAmount;
    private AutoCompleteTextView spinnerMonth;
    private TextInputEditText editYear;
    private TextInputLayout layoutAmount;

    private BollettaDao bollettaDao;
    private PurchaseTypeDao purchaseTypeDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private List<PurchaseType> purchaseTypes;
    private String editId = null;
    private long editCreatedAt = 0;
    private boolean isEdit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bolletta);

        AppDatabase db = AppDatabase.getInstance(this);
        bollettaDao = db.bollettaDao();
        purchaseTypeDao = db.purchaseTypeDao();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        spinnerType = findViewById(R.id.spinner_tipo);
        editAmount = findViewById(R.id.edit_importo);
        spinnerMonth = findViewById(R.id.spinner_mese);
        editYear = findViewById(R.id.edit_anno);
        layoutAmount = findViewById(R.id.layout_importo);
        MaterialButton btnSave = findViewById(R.id.btn_salva);

        setupMonthSpinner();

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_BOLLETTA_ID)) {
            isEdit = true;
            editId = intent.getStringExtra(EXTRA_BOLLETTA_ID);
            editCreatedAt = intent.getLongExtra(EXTRA_BOLLETTA_CREATED_AT, System.currentTimeMillis());
            toolbar.setTitle(R.string.modifica_bolletta);

            editAmount.setText(String.valueOf(intent.getDoubleExtra(EXTRA_BOLLETTA_AMOUNT, 0)));

            int month = intent.getIntExtra(EXTRA_BOLLETTA_MONTH, 1);
            String[] months = getResources().getStringArray(R.array.mesi);
            if (month >= 1 && month <= 12) {
                spinnerMonth.setText(months[month - 1], false);
            }
            editYear.setText(String.valueOf(intent.getIntExtra(EXTRA_BOLLETTA_YEAR, Calendar.getInstance().get(Calendar.YEAR))));
        } else {
            Calendar cal = Calendar.getInstance();
            String[] months = getResources().getStringArray(R.array.mesi);
            spinnerMonth.setText(months[cal.get(Calendar.MONTH)], false);
            editYear.setText(String.valueOf(cal.get(Calendar.YEAR)));
        }

        loadPurchaseTypes(intent);

        btnSave.setOnClickListener(v -> save());
    }

    private void loadPurchaseTypes(Intent intent) {
        executor.execute(() -> {
            purchaseTypes = purchaseTypeDao.getAll();
            String[] names = new String[purchaseTypes.size()];
            for (int i = 0; i < purchaseTypes.size(); i++) {
                names[i] = purchaseTypes.get(i).getName();
            }
            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, names);
                spinnerType.setAdapter(adapter);

                if (isEdit && intent.hasExtra(EXTRA_BOLLETTA_PURCHASE_TYPE_ID)) {
                    String purchaseTypeId = intent.getStringExtra(EXTRA_BOLLETTA_PURCHASE_TYPE_ID);
                    for (PurchaseType pt : purchaseTypes) {
                        if (pt.getId().equals(purchaseTypeId)) {
                            spinnerType.setText(pt.getName(), false);
                            break;
                        }
                    }
                } else if (!purchaseTypes.isEmpty()) {
                    spinnerType.setText(names[0], false);
                }
            });
        });
    }

    private void setupMonthSpinner() {
        String[] months = getResources().getStringArray(R.array.mesi);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, months);
        spinnerMonth.setAdapter(adapter);
    }

    private void save() {
        String amountStr = editAmount.getText() != null ? editAmount.getText().toString().trim() : "";
        String yearStr = editYear.getText() != null ? editYear.getText().toString().trim() : "";

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(",", "."));
        } catch (NumberFormatException e) {
            layoutAmount.setError(getString(R.string.errore_importo));
            return;
        }

        if (amount <= 0) {
            layoutAmount.setError(getString(R.string.errore_importo_zero));
            return;
        }

        layoutAmount.setError(null);

        amount = Math.round(amount * 100.0) / 100.0;

        String selectedTypeName = spinnerType.getText().toString();
        String purchaseTypeId = null;
        if (purchaseTypes != null) {
            for (PurchaseType pt : purchaseTypes) {
                if (pt.getName().equals(selectedTypeName)) {
                    purchaseTypeId = pt.getId();
                    break;
                }
            }
        }
        if (purchaseTypeId == null) return;

        int month = getSelectedMonth();
        int year;
        try {
            year = Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
            year = Calendar.getInstance().get(Calendar.YEAR);
        }

        if (isEdit) {
            Bolletta bolletta = new Bolletta(purchaseTypeId, amount, month, year);
            bolletta.setId(editId);
            bolletta.setCreatedAt(editCreatedAt);
            bolletta.setUpdatedAt(System.currentTimeMillis());
            executor.execute(() -> {
                bollettaDao.update(bolletta);
                MqttSyncManager.getInstance(AddBollettaActivity.this).publishBolletta(bolletta);
                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            });
        } else {
            Bolletta bolletta = new Bolletta(purchaseTypeId, amount, month, year);
            executor.execute(() -> {
                bollettaDao.insert(bolletta);
                MqttSyncManager.getInstance(AddBollettaActivity.this).publishBolletta(bolletta);
                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            });
        }
    }

    private int getSelectedMonth() {
        String monthText = spinnerMonth.getText().toString();
        String[] months = getResources().getStringArray(R.array.mesi);
        for (int i = 0; i < months.length; i++) {
            if (months[i].equals(monthText)) return i + 1;
        }
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
