package com.spese.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PurchaseTypeDao {

    @Insert
    void insert(PurchaseType purchaseType);

    @Update
    void update(PurchaseType purchaseType);

    @Delete
    void delete(PurchaseType purchaseType);

    @Query("SELECT * FROM purchase_types ORDER BY name ASC")
    List<PurchaseType> getAll();

    @Query("SELECT * FROM purchase_types WHERE id = :id")
    PurchaseType getById(String id);

    @Query("SELECT COUNT(*) FROM bollette WHERE purchaseTypeId = :purchaseTypeId")
    int countBolletteByPurchaseType(String purchaseTypeId);

    @Query("SELECT * FROM purchase_types WHERE name = :name LIMIT 1")
    PurchaseType getByName(String name);
}
