package com.spese.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Bolletta.class, PurchaseType.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract BollettaDao bollettaDao();
    public abstract PurchaseTypeDao purchaseTypeDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE bollette_new ("
                    + "id TEXT NOT NULL PRIMARY KEY, "
                    + "tipo TEXT, "
                    + "importo REAL NOT NULL, "
                    + "mese INTEGER NOT NULL, "
                    + "anno INTEGER NOT NULL, "
                    + "creatoIl INTEGER NOT NULL)");

            database.execSQL("INSERT INTO bollette_new (id, tipo, importo, mese, anno, creatoIl) "
                    + "SELECT CAST(id AS TEXT), tipo, importo, mese, anno, creatoIl FROM bollette");

            database.execSQL("DROP TABLE bollette");
            database.execSQL("ALTER TABLE bollette_new RENAME TO bollette");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Crea tabella purchase_types
            database.execSQL("CREATE TABLE purchase_types ("
                    + "id TEXT NOT NULL PRIMARY KEY, "
                    + "name TEXT NOT NULL, "
                    + "description TEXT)");
            database.execSQL("CREATE UNIQUE INDEX index_purchase_types_name ON purchase_types (name)");

            // Crea un PurchaseType per ogni valore distinto di tipo presente nelle bollette
            database.execSQL("INSERT INTO purchase_types (id, name, description) "
                    + "SELECT lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' "
                    + "|| substr(hex(randomblob(2)),2) || '-' || substr('89ab', abs(random()) % 4 + 1, 1) "
                    + "|| substr(hex(randomblob(2)),2) || '-' || hex(randomblob(6))), "
                    + "tipo, NULL FROM bollette WHERE tipo IS NOT NULL GROUP BY tipo");

            // Ricrea bollette con le nuove colonne (rinominate in inglese)
            database.execSQL("CREATE TABLE bollette_new ("
                    + "id TEXT NOT NULL PRIMARY KEY, "
                    + "purchaseTypeId TEXT NOT NULL, "
                    + "amount REAL NOT NULL, "
                    + "month INTEGER NOT NULL, "
                    + "year INTEGER NOT NULL, "
                    + "createdAt INTEGER NOT NULL, "
                    + "FOREIGN KEY (purchaseTypeId) REFERENCES purchase_types(id) ON DELETE RESTRICT)");
            database.execSQL("CREATE INDEX index_bollette_purchaseTypeId ON bollette_new (purchaseTypeId)");

            // Migra i dati risolvendo tipo -> purchaseTypeId
            database.execSQL("INSERT INTO bollette_new (id, purchaseTypeId, amount, month, year, createdAt) "
                    + "SELECT b.id, pt.id, b.importo, b.mese, b.anno, b.creatoIl "
                    + "FROM bollette b INNER JOIN purchase_types pt ON b.tipo = pt.name");

            database.execSQL("DROP TABLE bollette");
            database.execSQL("ALTER TABLE bollette_new RENAME TO bollette");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "spese.db"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build();
                }
            }
        }
        return instance;
    }
}
